# ProGuard rules for MaryTTS and dependencies

# Keep all SQL-related classes that MaryTTS might need
-keep class java.sql.** { *; }
-keep class javax.sql.** { *; }

# Keep Derby database classes
-keep class org.apache.derby.** { *; }

# Keep logging classes that libraries might use
-keep class org.apache.log4j.** { *; }
-keep class org.apache.commons.logging.** { *; }
-keep class org.slf4j.** { *; }

# Keep JMX classes for logging configuration
-keep class javax.management.** { *; }

# Keep MaryTTS classes
-keep class marytts.** { *; }
-keep class de.dfki.mary.** { *; }

# Keep audio-related classes
-keep class javax.sound.** { *; }

# Don't warn about missing references that we're keeping
-dontwarn java.sql.**
-dontwarn javax.sql.**
-dontwarn org.apache.derby.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.slf4j.**
-dontwarn javax.management.**

# Don't warn about missing annotation classes - these are optional
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn com.google.errorprone.annotations.**

# Don't warn about optional framework dependencies
-dontwarn javax.servlet.**
-dontwarn junit.**
-dontwarn org.junit.**
-dontwarn org.apache.tools.ant.**
-dontwarn org.apache.bsf.**
-dontwarn org.apache.commons.cli.**
-dontwarn jline.**
-dontwarn org.osgi.**
-dontwarn com.lmax.disruptor.**
-dontwarn org.jctools.**
-dontwarn com.fasterxml.jackson.**
-dontwarn javax.mail.**
-dontwarn org.conscrypt.**

# Don't warn about optional XML processing dependencies
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**

# Don't warn about optional testing frameworks
-dontwarn org.hamcrest.**
-dontwarn org.mockito.**

# Keep our Derby logging disable method
-keep class com.jholachhapdevs.pdfjuggler.feature.tts.MaryTTSService {
    public static java.io.PrintStream disableDerbyLogging();
}

# General rules for reflection and serialization
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Allow build to proceed despite warnings for missing optional dependencies
# These are typically for frameworks/libraries not used at runtime
-ignorewarnings
