-keep class com.nfc.multilauncher.data.model.** { *; }
-keep class com.nfc.multilauncher.data.nfc.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class androidx.room.** { *; }
