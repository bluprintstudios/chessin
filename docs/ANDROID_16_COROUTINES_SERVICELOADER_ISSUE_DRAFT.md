# Android 16 Instrumentation Failure: `runTest` Cannot Find Its ServiceLoader Handler

**Status:** Submitted to [Kotlin/kotlinx.coroutines#4782](https://github.com/Kotlin/kotlinx.coroutines/issues/4782) on October 17, 2026.

## Summary

`kotlinx.coroutines.test.runTest` fails before executing its test body on a physical Android 16 device when used from an instrumentation test. The same minimal test passes on an Android 17 emulator with the same project, APKs, and dependency versions.

## Minimal reproduction

```kotlin
@RunWith(AndroidJUnit4::class)
class CoroutinesRunTestReproductionTest {
    @Test
    fun runTestInitializesItsExceptionCollector() = runTest {
        assertTrue(true)
    }
}
```

## Environment

- Android Gradle Plugin: 9.4.1
- Kotlin: 2.2.10
- kotlinx-coroutines-android and kotlinx-coroutines-test: 1.10.2
- Compose BOM: 2026.02.01
- Instrumentation runner: `androidx.test.runner.AndroidJUnitRunner`
- Physical device: OPPO CPH2691, Android 16 (API 36)
- Comparison device: `Medium_Phone_API_37.0` emulator, Android 17 (API 37, x86_64)

## Actual result on Android 16

The minimal test fails before its assertion:

```text
java.lang.IllegalStateException: Exception handler was not found via a ServiceLoader
    at kotlinx.coroutines.internal.CoroutineExceptionHandlerImplKt.ensurePlatformExceptionHandlerLoaded(CoroutineExceptionHandlerImpl.kt:25)
    at kotlinx.coroutines.test.TestScopeImpl.enter(TestScope.kt:230)
    at kotlinx.coroutines.test.TestBuildersKt__TestBuildersKt.runTest-8Mi8wO0(TestBuilders.kt:308)
```

## Expected result

The test body should execute, as it does on the Android 17 emulator.

## Classloader evidence

The test APK contains the service entry for:

```text
kotlinx.coroutines.test.internal.ExceptionCollectorAsService
```

The target app APK contains the service entry for:

```text
kotlinx.coroutines.android.AndroidExceptionPreHandler
```

On Android 16, a direct diagnostic using `CoroutineExceptionHandler::class.java.classLoader` reported only the target app APK resource and only `AndroidExceptionPreHandler`. It did not expose `ExceptionCollectorAsService` from the instrumentation APK. `runTest` passes `ExceptionCollector` to `ensurePlatformExceptionHandlerLoaded`, which then rejects it because that collector is absent from the resolved provider list.

## Commands run

Run the reproduction against one device at a time:

```text
gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pro.chess_review.CoroutinesRunTestReproductionTest
```

For Android 16, this command failed on the physical device. With `ANDROID_SERIAL=emulator-5554` set for the Gradle invocation, it passed on the Android 17 emulator.

## Experiments already ruled out

- Upgrading Kotlin from 2.2.10 to 2.2.20 and coroutines from 1.10.2 to 1.11.0 did not resolve the Android 16 failure.
- A test-only packaging experiment made the test provider visible to the target loader, proving the classloader cause, but it disrupted Compose hierarchy attachment. It was removed rather than retained as a workaround.

No production code or runtime dependencies were changed by this investigation.

## Temporary test-runner containment

`CoachScreenTest` is annotated with `@SdkSuppress(excludedSdks = [36])`. This skips only
the affected platform from running the three Compose instrumentation tests, avoiding three
failures that happen before test code executes. The tests remain required on other API levels;
the Android 17 emulator is the current verification environment. Remove the suppression and
re-run the affected-device suite when kotlinx.coroutines resolves issue 4782 or Android's
instrumentation classloader behavior changes.
