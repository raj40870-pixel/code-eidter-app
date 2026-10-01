# Code Editor (TermCode IDE) - ProGuard & R8 Obfuscation Rules

# Strip line numbers and original source file names for maximum anti-reverse engineering
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep native JNI methods (required for Terminal PTY execution)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Room Database & Entities
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# ViewModels & ViewBinding
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>();
}
-keep class * implements androidx.viewbinding.ViewBinding { *; }

# Keep data models used by Moshi/JSON
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class com.squareup.moshi.** { *; }

# Keep AppUpdateManager model fields for update JSON serialization
-keepclassmembers class com.example.util.AppUpdateManager { *; }
