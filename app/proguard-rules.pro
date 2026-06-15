# NextLib FFmpeg software decoders: the renderer/decoder classes bridge to native
# code via JNI, so R8 must not rename or strip them (otherwise Dolby/MP2 audio breaks
# silently in release builds).
-keep class io.github.anilbeesetti.nextlib.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.footballxtream.data.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.footballxtream.data.**$$serializer { *; }
-keepclassmembers class com.footballxtream.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
