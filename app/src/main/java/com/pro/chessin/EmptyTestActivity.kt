package com.pro.chessin

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Empty Activity for instrumented Compose component tests.
 * This Activity has no content, allowing tests to call setContent via ComposeTestRule.
 */
class EmptyTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No setContent here - tests will call it via ComposeTestRule
    }
}
