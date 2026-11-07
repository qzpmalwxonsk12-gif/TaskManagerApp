# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in app/build.gradle.kts (proguardFiles).

# Keep Room generated code
-keep class androidx.room.** { *; }

# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
