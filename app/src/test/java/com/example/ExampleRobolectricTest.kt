package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `app name and legal links are configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("RSETSparks", context.getString(R.string.app_name))
    assertTrue(context.getString(R.string.privacy_policy_url).startsWith("https://"))
    assertTrue(context.getString(R.string.terms_url).startsWith("https://"))
    assertTrue(context.getString(R.string.account_deletion_url).startsWith("https://"))
  }
}
