# NextLib FFmpeg software decoders: the renderer/decoder classes bridge to native
# code via JNI, so R8 must not rename or strip them (otherwise Dolby/MP2 audio breaks
# silently in release builds).
-keep class io.github.anilbeesetti.nextlib.** { *; }

# kotlinx.serialization — keep generated serializers for EVERY @Serializable class, in any package.
# The previous rules only covered com.footballxtream.data.** (the DTOs), but the models in
# com.footballxtream.model.** (ChannelGroup, ChannelFolder, LiveChannel, Quality…) are serialized to
# the on-disk cache too; without these, R8 could break cache read/write only in release builds.
-keepattributes *Annotation*, InnerClasses, RuntimeVisibleAnnotations, AnnotationDefault
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclasseswithmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
