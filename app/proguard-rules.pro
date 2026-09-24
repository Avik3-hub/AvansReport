# Apache POI uses a small amount of reflection while opening Office templates.
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-dontwarn org.bouncycastle.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.apache.logging.log4j.**
