# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep crash-relevant attributes for stack traces and Gson generic types.
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-renamesourcefileattribute SourceFile

# Gson hits these via reflection during JSON import/export. Keep their
# fields so member names match the on-disk JSON.
-keep class com.example.wardrobe.json_parser.WardrobeImport { *; }
-keep class com.example.wardrobe.json_parser.**Json { *; }

# Gson TypeToken anonymous subclasses (used by Converters.kt for List<String?>).
-keep class * extends com.google.gson.reflect.TypeToken
