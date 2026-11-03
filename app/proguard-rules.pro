# SQLCipher uses JNI; keep its classes.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
# Room generated code
-keep class * extends androidx.room.RoomDatabase
-dontwarn org.slf4j.**
