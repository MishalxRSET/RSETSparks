package com.example

import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers
import com.example.security.AuthService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicTest {

  private val validProfile = UserProfile(
    name = "Ananya",
    bio = "Filter coffee and robotics",
    interests = listOf("Robotics"),
    department = "B.Tech Computer Science",
    year = 2027,
    age = 20
  )

  @Test
  fun `only rajagiri emails are accepted`() {
    assertTrue(AuthService.isRajagiriEmail("u2504040@rajagiri.edu.in"))
    assertTrue(AuthService.isRajagiriEmail("  Someone@RAJAGIRI.EDU.IN "))
    assertFalse(AuthService.isRajagiriEmail("@rajagiri.edu.in"))
    assertFalse(AuthService.isRajagiriEmail("student@gmail.com"))
    assertFalse(AuthService.isRajagiriEmail("student@rajagiri.edu.in.evil.com"))
    assertFalse(AuthService.isRajagiriEmail(null))
  }

  @Test
  fun `profiles under 18 are rejected`() {
    assertNull(UserProfile.validate(validProfile))
    assertNotNull(UserProfile.validate(validProfile.copy(age = 17)))
    assertNull(UserProfile.validate(validProfile.copy(age = 18)))
  }

  @Test
  fun `profiles need a name and at least one interest`() {
    assertNotNull(UserProfile.validate(validProfile.copy(name = " ")))
    assertNotNull(UserProfile.validate(validProfile.copy(interests = emptyList())))
    assertNotNull(UserProfile.validate(validProfile.copy(bio = "x".repeat(UserProfile.MAX_BIO_LENGTH + 1))))
  }

  @Test
  fun `pair ids are order independent`() {
    assertEquals(FirestoreMappers.pairId("alice", "bob"), FirestoreMappers.pairId("bob", "alice"))
    assertEquals("alice_bob", FirestoreMappers.pairId("bob", "alice"))
    assertEquals("bob_alice", FirestoreMappers.swipeId("bob", "alice"))
  }

  @Test
  fun `user documents round trip`() {
    val map = FirestoreMappers.userToMap("uid1", validProfile, "https://photo", colorIndex = 3, visible = true)
    val user = FirestoreMappers.userFromMap("uid1", map)
    assertEquals("Ananya", user.name)
    assertEquals(20, user.age)
    assertEquals("Robotics", user.interests)
    assertEquals("https://photo", user.photoUri)
    assertEquals(3, user.avatarColorIndex)
    assertTrue(user.isVisibleInSparks)
    assertFalse(user.isSuspended)
    assertFalse(user.showBadges)
  }

  @Test
  fun `officer badge only shows when an admin enables it`() {
    val base = FirestoreMappers.userToMap("admin", validProfile, null, 0, visible = false)
    assertFalse(FirestoreMappers.userFromMap("admin", base).isAdmin)
    assertTrue(FirestoreMappers.userFromMap("admin", base + ("showBadges" to true)).isAdmin)
    // The signed-in admin always sees their own admin tools, badge or not.
    assertTrue(FirestoreMappers.userFromMap("admin", base, isAdmin = true).isAdmin)
  }
}
