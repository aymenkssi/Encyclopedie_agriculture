# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class org.agripedia.**$$serializer { *; }
-keepclassmembers class org.agripedia.** { *** Companion; }
-keepclasseswithmembers class org.agripedia.** { kotlinx.serialization.KSerializer serializer(...); }
