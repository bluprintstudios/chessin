#include <jni.h>
#include <string>
#include <cstring>
#include <sstream>
#include <android/log.h>
#include <pthread.h>
#include <mutex>
#include <condition_variable>
#include <queue>
#include <thread>
#include <chrono>
#include <memory>
#include <vector>
#include <filesystem>

#include "uci.h"
#include "engine.h"
#include "attacks.h"
#include "position.h"
#include "tune.h"

#define LOG_TAG "StockfishEngine"

static JavaVM* g_javaVM = nullptr;
static jclass g_engineBridgeClass = nullptr;
static jmethodID g_onEngineOutputMethod = nullptr;

static std::mutex g_commandQueueMutex;
static std::condition_variable g_commandQueueCV;
static std::queue<std::string> g_commandQueue;
static volatile bool g_engineRunning = false;
static pthread_t g_engineThread;

static std::unique_ptr<Stockfish::Engine> g_engine;

void log_message(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, fmt, args);
    va_end(args);
}

// Safely call the Java static callback NativeBridge.onEngineOutput(String)
static void call_java_onEngineOutput(const std::string& msg) {
    if (!g_javaVM || !g_engineBridgeClass || !g_onEngineOutputMethod) return;

    JNIEnv* env = nullptr;
    bool attached = false;
    if (g_javaVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (g_javaVM->AttachCurrentThread(&env, nullptr) == JNI_OK) {
            attached = true;
        } else {
            log_message("Failed to attach thread for Java callback");
            return;
        }
    }

    jstring jmsg = env->NewStringUTF(msg.c_str());
    if (jmsg) {
        env->CallStaticVoidMethod(g_engineBridgeClass, g_onEngineOutputMethod, jmsg);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->DeleteLocalRef(jmsg);
    }

    // Note: We intentionally do NOT call DetachCurrentThread() here.
    // Search threads in Stockfish persist for the lifetime of the Engine and emit
    // info/depth lines throughout the search. Repeatedly attaching/detaching causes:
    // 1. Performance overhead (JNIEnv allocation for every callback)
    // 2. Potential race conditions where DetachCurrentThread() interferes with
    //    the coroutine dispatch chain (tryEmit -> resume -> dispatch -> unpark)
    //    that happens synchronously within CallStaticVoidMethod above.
    // The JVM will automatically detach threads when they exit (when the
    // ThreadPool is cleared and threads are joined in the Engine destructor).
    // UNUSED: if (attached) { g_javaVM->DetachCurrentThread(); }
}

// Parse a "position" command into fen and moves vector. Supports "startpos" and "fen <6-fields>" plus optional "moves ..."
static void parse_position_command(const std::string& cmd, std::string& outFen, std::vector<std::string>& outMoves) {
    outFen.clear();
    outMoves.clear();

    std::istringstream iss(cmd);
    std::string token;
    // consume "position"
    iss >> token;
    if (!(iss >> token)) return;

    if (token == "startpos") {
        outFen = Stockfish::StartFEN;
    } else if (token == "fen") {
        // FEN has 6 space-separated fields
        std::string fenPart;
        std::string fenFields[6];
        for (int i = 0; i < 6; ++i) {
            if (!(iss >> fenFields[i])) return; // malformed fen
            if (i) fenPart += ' ';
            fenPart += fenFields[i];
        }
        outFen = fenPart;
    } else {
        // Unknown, bail
        return;
    }

    // Check if there's a "moves" token next
    if (iss >> token) {
        if (token == "moves") {
            while (iss >> token) {
                outMoves.push_back(token);
            }
        }
    }
}

// Parse a basic subset of "go" commands. Supports: "go depth N", "go movetime N", "go nodes N", and plain "go" (infinite)
static Stockfish::Search::LimitsType parse_go_command(const std::string& cmd) {
    Stockfish::Search::LimitsType limits;
    std::istringstream iss(cmd);
    std::string token;
    iss >> token; // consume "go"
    while (iss >> token) {
        if (token == "depth") {
            int d = 0;
            if (iss >> d) limits.depth = d;
        } else if (token == "movetime") {
            int ms = 0;
            if (iss >> ms) {
                limits.movetime = Stockfish::TimePoint(ms);
            }
        } else if (token == "nodes") {
            Stockfish::u64 n = 0;
            if (iss >> n) limits.nodes = n;
        } else if (token == "infinite") {
            limits.infinite = 1;
        } else if (token == "mate") {
            int m = 0; if (iss >> m) limits.mate = m;
        }
    }
    return limits;
}

void* engineThreadMain(void* arg) {
    log_message("Engine thread started");

    // Attach this native thread to the JVM to allow easier callback usage
    JNIEnv* env = nullptr;
    if (g_javaVM && g_javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK) {
        log_message("Failed to attach engine thread to JVM");
        return nullptr;
    }

    log_message("Engine thread attached to JVM");

    // Initialize Stockfish global static data (required before engine creation)
    Stockfish::Attacks::init();
    Stockfish::Position::init();
    log_message("Stockfish global static data initialized");

    // Instantiate Stockfish engine
    try {
        g_engine = std::make_unique<Stockfish::Engine>();
    } catch (const std::exception& ex) {
        log_message("Failed to create Stockfish engine: %s", ex.what());
        if (g_javaVM) g_javaVM->DetachCurrentThread();
        return nullptr;
    }

    // Initialize engine with default settings
    try {
        g_engine->resize_threads();
        g_engine->set_tt_size(16); // 16 MB hash table (default)
        // Note: Tune::init is only needed for fishtest tuning, skip for normal use
        log_message("Engine initialized with threads and hash table");
    } catch (const std::exception& ex) {
        log_message("Failed to initialize engine: %s", ex.what());
        // Continue anyway - some initialization might be optional
    }

    // Load NNUE network if path was provided
    std::string* nnuePathPtr = static_cast<std::string*>(arg);
    std::string nnuePath = nnuePathPtr ? *nnuePathPtr : "";
    delete nnuePathPtr; // Clean up the allocated string
    if (!nnuePath.empty()) {
        try {
            g_engine->load_network(nnuePath);
            log_message("NNUE network loaded from: %s", nnuePath.c_str());
        } catch (const std::exception& ex) {
            log_message("Failed to load NNUE network: %s", ex.what());
            // Continue without NNUE - engine will use classical evaluation
        }
    }

    // Register callbacks to forward Stockfish output directly to Java
    // These must all be set before go() can work properly
    g_engine->set_on_update_no_moves([](const Stockfish::Search::InfoShort& info) {
        // Short info without PV - not commonly used but required for proper initialization
        std::string scoreStr = Stockfish::UCIEngine::format_score(info.score);
        char buf[128];
        snprintf(buf, sizeof(buf), "info depth %d score %s", info.depth, scoreStr.c_str());
        call_java_onEngineOutput(std::string(buf));
    });

    g_engine->set_on_update_full([](const Stockfish::Search::InfoFull& info) {
        // Format a sane UCI-like info string
        std::string scoreStr = Stockfish::UCIEngine::format_score(info.score);
        std::string pv = std::string(info.pv);
        char buf[512];
        snprintf(buf, sizeof(buf), "info depth %d seldepth %d multipv %zu score %s time %zu nodes %zu nps %zu pv %s",
                 info.depth,
                 info.selDepth,
                 info.multiPV,
                 scoreStr.c_str(),
                 (size_t)info.timeMs,
                 (size_t)info.nodes,
                 (size_t)info.nps,
                 pv.c_str());
        call_java_onEngineOutput(std::string(buf));
    });

    g_engine->set_on_iter([](const Stockfish::Search::InfoIteration& info) {
        char buf[256];
        std::string currmove = std::string(info.currmove);
        snprintf(buf, sizeof(buf), "info currmove %s currmovenumber %zu depth %d",
                 currmove.c_str(), (size_t)info.currmovenumber, info.depth);
        call_java_onEngineOutput(std::string(buf));
    });

    g_engine->set_on_bestmove([](std::string_view bestmove, std::string_view ponder) {
        std::string bm = std::string(bestmove);
        std::string p = std::string(ponder);
        std::string out = "bestmove " + bm;
        if (!p.empty()) out += std::string(" ponder ") + p;
        call_java_onEngineOutput(out);
    });

    g_engine->set_on_start([]() {
        // Called when search starts - can be used for logging
        log_message("Search started");
    });

    g_engine->set_on_verify_network([](std::string_view msg) {
        call_java_onEngineOutput(std::string("info string ") + std::string(msg));
    });

    // Send a simple UCI greeting
    call_java_onEngineOutput("id name Stockfish");
    call_java_onEngineOutput("id author Stockfish Developers");
    call_java_onEngineOutput("uciok");

    // Main command loop: dispatch UCI commands into the Engine API
    while (g_engineRunning) {
        std::unique_lock<std::mutex> lock(g_commandQueueMutex);
        bool hasCommand = g_commandQueueCV.wait_for(
            lock,
            std::chrono::milliseconds(100),
            []() { return !g_commandQueue.empty() || !g_engineRunning; }
        );

        if (!g_engineRunning) break;
        if (!hasCommand || g_commandQueue.empty()) continue;

        std::string command = g_commandQueue.front();
        g_commandQueue.pop();
        lock.unlock();

        log_message("Engine processing command: %s", command.c_str());

        // Very small subset of UCI commands handled here
        if (command == "uci") {
            call_java_onEngineOutput("uciok");
        } else if (command == "isready") {
            call_java_onEngineOutput("readyok");
        } else if (command.rfind("position", 0) == 0) {
            std::string fen;
            std::vector<std::string> moves;
            parse_position_command(command, fen, moves);
            if (!fen.empty()) {
                auto err = g_engine->set_position(fen, moves);
                if (err.has_value()) {
                    call_java_onEngineOutput("info string failed to set position");
                } else {
                    call_java_onEngineOutput(std::string("info string position set: ") + fen);
                }
            }
        } else if (command.rfind("go", 0) == 0) {
            Stockfish::Search::LimitsType limits = parse_go_command(command);
            // Non-blocking call to start searching
            try {
                log_message("About to call g_engine->go() with depth=%d", limits.depth);
                g_engine->go(limits);
                log_message("g_engine->go() returned successfully");
            } catch (const std::exception& ex) {
                log_message("CRITICAL: Exception while starting search: %s", ex.what());
                call_java_onEngineOutput(std::string("info string exception starting search: ") + ex.what());
            } catch (...) {
                log_message("CRITICAL: Unknown exception while starting search");
                call_java_onEngineOutput(std::string("info string unknown exception starting search"));
            }
        } else if (command == "stop") {
            try {
                g_engine->stop();
            } catch (...) {
                log_message("Exception while stopping search");
            }
        } else if (command == "quit") {
            log_message("Quit command received");
            g_engine->stop();
            g_engineRunning = false;
            break;
        } else if (command.rfind("setoption", 0) == 0) {
            // naive handling: forward to engine options map when possible
            // For now, respond acknowledging the option
            call_java_onEngineOutput(std::string("info string setoption received: ") + command.substr(9));
        } else {
            // Unknown command, echo it for debugging
            call_java_onEngineOutput(std::string("info string unknown command: ") + command);
        }
    }

    // Wait for any running search to finish and destroy engine
    if (g_engine) {
        try {
            g_engine->stop();
            g_engine->wait_for_search_finished();
        } catch (...) {}
        g_engine.reset();
    }

    log_message("Engine thread exiting");

    if (g_javaVM) g_javaVM->DetachCurrentThread();

    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_pro_chessin_engine_NativeBridge_engineStart(
        JNIEnv* env,
        jclass clazz,
        jstring nnuePath) {
    log_message("engineStart() called");
    if (g_engineRunning) {
        log_message("Engine already running");
        return;
    }

    // Clear any leftover commands from previous session to prevent race conditions
    {
        std::unique_lock<std::mutex> lock(g_commandQueueMutex);
        while (!g_commandQueue.empty()) {
            log_message("Clearing leftover command from queue: %s", g_commandQueue.front().c_str());
            g_commandQueue.pop();
        }
    }

    const char* nativeNnuePath = nullptr;
    std::string* nnueStrPtr = nullptr;
    if (nnuePath) {
        nativeNnuePath = env->GetStringUTFChars(nnuePath, nullptr);
        if (nativeNnuePath) {
            nnueStrPtr = new std::string(nativeNnuePath);
            env->ReleaseStringUTFChars(nnuePath, nativeNnuePath);
        }
    }

    g_engineRunning = true;

    if (pthread_create(&g_engineThread, nullptr, engineThreadMain, nnueStrPtr) != 0) {
        log_message("Failed to create engine thread");
        g_engineRunning = false;
        delete nnueStrPtr;
        return;
    }

    log_message("Engine started successfully (thread created)");
}

extern "C" JNIEXPORT void JNICALL
Java_com_pro_chessin_engine_NativeBridge_engineSendCommand(
        JNIEnv* env,
        jclass clazz,
        jstring command) {
    if (!g_engineRunning) {
        log_message("Engine not running, ignoring command");
        return;
    }

    const char* nativeCommand = env->GetStringUTFChars(command, nullptr);
    if (!nativeCommand) return;

    {
        std::unique_lock<std::mutex> lock(g_commandQueueMutex);
        g_commandQueue.push(std::string(nativeCommand));
        g_commandQueueCV.notify_one();
    }

    env->ReleaseStringUTFChars(command, nativeCommand);
}

extern "C" JNIEXPORT void JNICALL
Java_com_pro_chessin_engine_NativeBridge_engineStop(
        JNIEnv* env,
        jclass clazz) {
    log_message("engineStop() called");
    if (!g_engineRunning) {
        log_message("Engine not running");
        return;
    }

    {
        std::unique_lock<std::mutex> lock(g_commandQueueMutex);
        g_commandQueue.push("quit");
        g_engineRunning = false;
        g_commandQueueCV.notify_one();
    }

    pthread_join(g_engineThread, nullptr);

    log_message("Engine stopped");
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    log_message("JNI_OnLoad called");
    g_javaVM = vm;

    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        log_message("Failed to get JNI environment");
        return JNI_VERSION_1_6;
    }

    jclass localEngineBridgeClass = env->FindClass("com/pro/chessin/engine/NativeBridge");
    if (localEngineBridgeClass == nullptr) {
        log_message("Failed to find NativeBridge class");
        return JNI_VERSION_1_6;
    }

    g_engineBridgeClass = (jclass)env->NewGlobalRef(localEngineBridgeClass);
    env->DeleteLocalRef(localEngineBridgeClass);

    if (g_engineBridgeClass == nullptr) {
        log_message("Failed to create global ref to NativeBridge class");
        return JNI_VERSION_1_6;
    }

    g_onEngineOutputMethod = env->GetStaticMethodID(
            g_engineBridgeClass,
            "onEngineOutput",
            "(Ljava/lang/String;)V");

    if (g_onEngineOutputMethod == nullptr) {
        log_message("Failed to find onEngineOutput method");
        env->DeleteGlobalRef(g_engineBridgeClass);
        return JNI_VERSION_1_6;
    }

    log_message("JNI initialized successfully");
    return JNI_VERSION_1_6;
}
