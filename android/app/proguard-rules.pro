# Keep LiteRT / TFLite GPU delegate classes
-keep class org.tensorflow.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn org.tensorflow.**

# Room
-keep class androidx.room.** { *; }
