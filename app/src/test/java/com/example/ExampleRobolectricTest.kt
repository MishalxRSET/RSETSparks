package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoHelper
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RSETSparks", appName)
  }

  @Test
  fun `verify aes encryption roundtrip`() {
    val plainMessage = "Free for canteen chai?"
    val convId = "student_alex_student_ananya"
    val enc = CryptoHelper.encrypt(plainMessage, convId)
    assertTrue(enc.cipherTextBase64.isNotBlank())
    val decrypted = CryptoHelper.decrypt(enc.cipherTextBase64, enc.ivBase64, convId)
    assertEquals(plainMessage, decrypted)
  }

  @Test
  fun `verify admin account defaults to Mohammed Mishal M with badges disabled by default`() {
    val adminEmail = "u2504040@rajagiri.edu.in"
    val profile = com.example.data.model.UserProfile(
      id = "admin_uid",
      email = adminEmail,
      name = "",
      showBadges = false
    )
    val entity = profile.toUserEntity()
    assertEquals("Mohammed Mishal M", entity.name)
    assertEquals(false, entity.showBadges)
    assertEquals(true, entity.isAdmin)
    assertEquals("B.Tech Computer Science", entity.getEffectiveDepartment())
  }

  @Test
  fun `verify admin account only displays officer title and badge when showBadges is turned on`() {
    val adminEmail = "u2504040@rajagiri.edu.in"
    val profileDisabled = com.example.data.model.UserProfile(
      id = "admin_uid",
      email = adminEmail,
      name = "Mohammed Mishal M",
      department = "Campus Safety Directorate",
      showBadges = false
    )
    val entityDisabled = profileDisabled.toUserEntity()
    assertEquals(false, entityDisabled.showBadges)
    assertEquals("B.Tech Computer Science", entityDisabled.getEffectiveDepartment())

    val profileEnabled = profileDisabled.copy(showBadges = true)
    val entityEnabled = profileEnabled.toUserEntity()
    assertEquals(true, entityEnabled.showBadges)
    assertEquals("Campus Safety Directorate", entityEnabled.getEffectiveDepartment())
  }
}
