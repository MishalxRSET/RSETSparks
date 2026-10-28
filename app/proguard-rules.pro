# RSETSparks R8 rules. Firebase, Compose, Coil and Kotlin coroutines ship their own consumer rules;
# only the gaps are covered here.

# Keep line numbers so Play Console / Crashlytics stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Credential Manager loads its Play Services provider reflectively.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
