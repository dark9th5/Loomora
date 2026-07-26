# Proguard rules for Loomora App

# Room Database
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
-keep class * extends androidx.room.RoomDatabase

# Media3 ExoPlayer
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }

# Hilt DI
-keep class * extends javax.inject.Provider
