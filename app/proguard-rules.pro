# Room generated implementations are looked up reflectively by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Compose runtime metadata used by the tooling/preview surface.
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}

# Kotlin coroutines internals referenced via reflection by the debug agent.
-dontwarn kotlinx.coroutines.**
