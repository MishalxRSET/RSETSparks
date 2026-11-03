package com.example

import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers
import com.example.data.CampusEmail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainLogicTest {

  private val validProfile = UserProfile(
    name = "Ananya",
    bio = "Filter coffee and robotics",
    interests = listOf("Robotics"),
    department = "B.Tech Computer Science",
    year = 2027,
    age = 20,
    gender = "WOMAN",
    showMe = "MEN"
  )

  private fun student(gender: String, showMe: String) =
    FirestoreMappers.userFromMap("u_$gender", FirestoreMappers.userToMap("u_$gender", validProfile.copy(gender = gender, showMe = showMe), null, 0, true))

  @Test
  fun `gender and show me are required, extras must be known values`() {
    assertNotNull(UserProfile.validate(validProfile.copy(gender = "")))
    assertNotNull(UserProfile.validate(validProfile.copy(gender = "BI")))
    assertNotNull(UserProfile.validate(validProfile.copy(showMe = "")))
    assertNull(UserProfile.validate(validProfile.copy(orientation = "BISEXUAL", lookingFor = "FRIENDS", pronouns = "she/her")))
    assertNotNull(UserProfile.validate(validProfile.copy(orientation = "SOMETHING")))
    assertNotNull(UserProfile.validate(validProfile.copy(lookingFor = "MARRIAGE")))
    assertNotNull(UserProfile.validate(validProfile.copy(pronouns = "x".repeat(UserProfile.MAX_PRONOUNS_LENGTH + 1))))
  }

  @Test
  fun `decks only pair students who each fit the other's show me`() {
    val womanSeekingMen = student("WOMAN", "MEN")
    val manSeekingWomen = student("MAN", "WOMEN")
    val manSeekingMen = student("MAN", "MEN")
    val transManSeekingWomen = student("TRANS_MAN", "WOMEN")
    val nonBinarySeekingEveryone = student("NON_BINARY", "EVERYONE")
    val womanSeekingEveryone = student("WOMAN", "EVERYONE")

    assertTrue(UserProfile.mutualMatch(womanSeekingMen, manSeekingWomen))
    assertFalse(UserProfile.mutualMatch(womanSeekingMen, manSeekingMen)) // he isn't looking for women
    assertTrue(UserProfile.mutualMatch(womanSeekingMen, transManSeekingWomen)) // trans men count as men
    assertFalse(UserProfile.mutualMatch(womanSeekingMen, nonBinarySeekingEveryone)) // she chose men only
    assertTrue(UserProfile.mutualMatch(womanSeekingEveryone, nonBinarySeekingEveryone))
    // Legacy profiles without gender never appear in anyone's deck.
    assertFalse(UserProfile.mutualMatch(womanSeekingEveryone, student("", "EVERYONE")))
  }

  @Test
  fun `only rajagiri emails are accepted`() {
    assertTrue(CampusEmail.isRajagiri("u2504040@rajagiri.edu.in"))
    assertTrue(CampusEmail.isRajagiri("  Someone@RAJAGIRI.EDU.IN "))
    assertFalse(CampusEmail.isRajagiri("@rajagiri.edu.in"))
    assertFalse(CampusEmail.isRajagiri("student@gmail.com"))
    assertFalse(CampusEmail.isRajagiri("student@rajagiri.edu.in.evil.com"))
    assertFalse(CampusEmail.isRajagiri(null))
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
    assertEquals("WOMAN", user.gender)
    assertEquals("MEN", user.showMe)

    val extras = FirestoreMappers.userFromMap(
      "uid1",
      FirestoreMappers.userToMap("uid1", validProfile.copy(orientation = "QUEER", lookingFor = "FRIENDS", pronouns = " she/they "), null, 0, true)
    )
    assertEquals("Woman · she/they · Queer", extras.identityLine())
    assertEquals("Friends", extras.lookingForLabel())
  }

  @Test
  fun `officer badge only shows when an admin enables it`() {
    val base = FirestoreMappers.userToMap("admin", validProfile, null, 0, visible = false)
    assertFalse(FirestoreMappers.userFromMap("admin", base).isAdmin)
    assertTrue(FirestoreMappers.userFromMap("admin", base + ("showBadges" to true)).isAdmin)
    // The signed-in admin always sees their own admin tools, badge or not.
    assertTrue(FirestoreMappers.userFromMap("admin", base, isAdmin = true).isAdmin)
  }

  @Test
  fun `moderation flags read the same whether stored as booleans or numbers`() {
    // The iOS SDK can hand booleans over as NSNumber; suspended/visible must not fall back to defaults.
    val base = FirestoreMappers.userToMap("u", validProfile, null, 0, visible = true)
    val fromNumbers = FirestoreMappers.userFromMap("u", base + mapOf("suspended" to 1L, "visible" to 0L, "verified" to 0))
    assertTrue(fromNumbers.isSuspended)
    assertFalse(fromNumbers.isVisibleInSparks)
    assertFalse(fromNumbers.isVerified)
  }

  @Test
  fun `timestamps go through the platform converter`() {
    val msg = FirestoreMappers.messageFromMap("m1", mapOf("senderId" to "a", "text" to "hi", "createdAt" to "ts"), false) { 42L }
    assertEquals(42L, msg.timestamp)
    val pending = FirestoreMappers.messageFromMap("m2", mapOf("senderId" to "a", "text" to "hi"), true) { null }
    assertTrue(pending.isPending)
    assertTrue(pending.timestamp > 0)
  }
}
