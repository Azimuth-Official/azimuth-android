# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class day.azimuth.observer.data.remote.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
