# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.waqf.awrad.**$$serializer { *; }
-keepclassmembers class com.waqf.awrad.** {
    *** Companion;
}
-keepclasseswithmembers class com.waqf.awrad.** {
    kotlinx.serialization.KSerializer serializer(...);
}
