# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization is turned off by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).
-dontoptimize
-dontpreverify
# Note that if you want to enable optimization, you cannot just
# include optimization flags in your own project configuration file;
# instead you will need to point to the
# "proguard-android-optimize.txt" file instead of this one from your
# project.properties file.

# SourceFile and LineNumberTable are required for useful stack traces
-keepattributes *Annotation*,SourceFile,LineNumberTable

-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

# Keep ActionProviders for Cast
# See menu_startup.xml
-keep class android.support.v4.view.ActionProvider
-keepnames class android.support.v4.view.ActionProvider
-keepclassmembers class android.support.v4.view.ActionProvider
-keep class android.support.v7.app.MediaRouteActionProvider
-keepnames class android.support.v7.app.MediaRouteActionProvider
-keepclassmembers class android.support.v7.app.MediaRouteActionProvider

# Allow obfuscation of android.support.v7.internal.view.menu.**
# to avoid problem on Samsung 4.2.2 devices with appcompat v21
# see https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.internal.view.menu.**,android.support.** {*;}
-keep interface !android.support.v7.internal.view.menu.**,android.support.** {*;}

-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }

# Config for Google Play Services: http://developer.android.com/google/play-services/setup.html#Setup
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @ccom.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn com.google.android.gms.**

# Guava ProGuard (Sugar ORM depends on Guava)
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# Dagger depends on ErrorProne, but they are not used in runtime; Ignore
-dontwarn com.google.errorprone.annotations.*

# Unity3D
-keep class bitter.jnibridge.** { *; }
-keep class com.unity3d.** { *; }
-keep class org.fmod.** { *; }

# Santa Snap
-keep class com.google.android.apps.santatracker.santasnap.** { *; }