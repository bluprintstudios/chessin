# Stockfish 19 Android distribution

The owner selected the open-source Stockfish route on October 15, 2026. Chessin' bundles Stockfish as a separate UCI process. The application source license is a separate decision; this note records the licenses and build provenance for the bundled engine and evaluation network.

## Components

| Component | Version / license | Source |
| --- | --- | --- |
| Stockfish engine | 19, GNU GPL version 3 or later | [Pinned source commit](https://github.com/official-stockfish/Stockfish/tree/edb0d9db6731067ec50ce619ff372b463bc4dd5d), tag `sf_19` |
| NNUE evaluation network | `nn-1a298aa575a0.nnue`, CC0-1.0 | [Official Stockfish networks repository](https://github.com/official-stockfish/networks/blob/master/nn-1a298aa575a0.nnue) |

The license texts are bundled at `app/src/main/assets/licenses/stockfish/COPYING.txt` and `CC0-1.0.txt`, and are shown in Settings → Open-source licenses. The NNUE network is embedded in each engine executable; no separate network file is needed at runtime.

## Source and build provenance

- Stockfish source archive: <https://github.com/official-stockfish/Stockfish/archive/refs/tags/sf_19.zip>
- SHA-256 of that source archive: `024a509d7af387218bd59c559473a71a1d6b411885010543891495119a671119`
- Stockfish source commit: `edb0d9db6731067ec50ce619ff372b463bc4dd5d`
- NNUE file SHA-256: `1a298aa575a085434d29027978dc36867fe9c5bcea9376654b7a8eba1e52dfc2` (the 12-character prefix is also encoded in the file name and checked by Stockfish's `scripts/net.sh`)
- Build tools used: Android NDK `28.2.13676358`, GNU Make `4.4.1`, Windows Git Bash
- Android API level used by Stockfish's NDK compiler wrappers: 29
- Stockfish build targets: `ARCH=armv8` and `ARCH=armv7-neon`, `COMP=ndk`, optimized `build` target
- The ARMv7 build adds `-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384`. NDK 28 produces 16 KB aligned AArch64 load segments by default.

From the pinned source tree, the build commands were:

```sh
make -j4 build ARCH=armv8 COMP=ndk
make -j4 build ARCH=armv7-neon COMP=ndk \
  'ENV_LDFLAGS=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384'
```

The upstream `make build` target downloads the NNUE file named in `src/evaluate.h` and validates it against the hash prefix before embedding it.

## Shipped engine binaries

Both outputs are packaged as `libstockfish.so` so Android installs and extracts them to `ApplicationInfo.nativeLibraryDir`; the app starts the PIE executable with `ProcessBuilder` and communicates over UCI. `app/build.gradle.kts` enables legacy native-library extraction and prevents the Android Gradle Plugin from stripping the engine.

| Android ABI | Source build target | Size (bytes) | SHA-256 |
| --- | --- | ---: | --- |
| `arm64-v8a` | `armv8` | 106,512,520 | `ded1ae2b00e0a1864fcf60961c003b80726a482ab13210825511a225f566104a` |
| `armeabi-v7a` | `armv7-neon` | 104,564,932 | `b1ce5557b06e18b49389157d513f739431a6d45d86fb4de5736ad97bcb45e2bc` |

On October 19, 2026, the repository copies of both binaries were re-hashed locally and matched the values in this table.

Static inspection with NDK 28's `llvm-readelf` showed `0x4000` alignment for every `LOAD` segment in both ELF outputs. The executables depend only on Android system libraries `libc.so`, `libm.so`, and `libdl.so`.

## Remaining release checks

- Install and launch both ABI variants on Android devices; confirm `nativeLibraryDir` extraction, process permissions, UCI startup, analysis, cancellation, and shutdown.
- Inspect the final APK/AAB to confirm both ABI payloads, the license texts, and the unmodified engine hashes are present. Confirm Play's per-ABI delivery size and current 16 KB page-size requirements.
- Review source-availability and Android installation-information requirements for the exact Play distribution. No legal approval is claimed here.
