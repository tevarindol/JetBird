-allowaccessmodification
-dontobfuscate
-dontpreverify # not useful for Android

-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }
