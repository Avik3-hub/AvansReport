# Apache POI uses a small amount of reflection while opening Office templates.
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-dontwarn org.bouncycastle.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.apache.logging.log4j.**

# POI also contains optional desktop/SVG renderers. Android has no AWT and
# AvansReport never calls those rendering paths; it only edits DOCX/XLSX data.
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.batik.**
-dontwarn org.w3c.dom.svg.**
