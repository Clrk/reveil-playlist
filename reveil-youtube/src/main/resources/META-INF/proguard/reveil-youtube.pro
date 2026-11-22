# NewPipeExtractor parses YouTube with reflection-free code but runs YouTube's JS through Rhino.
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn javax.script.**
-dontwarn java.beans.**
-dontwarn jdk.dynalink.**
-dontwarn org.jspecify.**
-dontwarn com.google.re2j.**
-keep class com.grack.nanojson.** { *; }
-keep class org.jsoup.** { *; }
# protobuf-lite reads message fields by reflection.
-keep class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }
