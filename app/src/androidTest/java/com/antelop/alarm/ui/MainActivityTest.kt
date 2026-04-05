package com.antelop.alarm.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsBlockingScreenWhenNotDefaultSmsApp() {
        composeRule.onNodeWithText("先把它设为默认短信应用").assertIsDisplayed()
    }
}
