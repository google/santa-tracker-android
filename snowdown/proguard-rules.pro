# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/macd/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Called by native code
-dontobfuscate
-keep class com.google.fpl.** { *; }
-keep class com.google.android.gms.games.** { *; }
-keep class com.google.android.gms.nearby.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.vr.cardboard.** { *; }
-keep class com.google.vrtoolkit.cardboard.** { *; }
-keep class org.libsdl.app.** { *; }
-keepclassmembers class org.libsdl.app.SDLActivity {
    public static <methods>;
}

