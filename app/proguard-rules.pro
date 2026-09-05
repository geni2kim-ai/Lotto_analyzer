# ====================================================================
# Lotto Insight ProGuard / R8 Protection & Keep Rules
# ====================================================================

# 1. Jetpack Compose Rules
-keepclassmembers class * extends androidx.compose.ui.node.LayoutNode {
    *** *;
}
-dontwarn androidx.compose.**

# 2. Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# 3. Retrofit2 & OkHttp Rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# 4. Gson Keep Rules (Data Transfer Objects & Models)
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    private void readObjectNoData();
}
-keep class com.example.lottoinsight.core.model.** { *; }
-keep class com.example.lottoinsight.core.network.model.** { *; }
-keep class com.example.lottoinsight.core.database.entity.** { *; }

# 5. Kotlin Coroutines Rules
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    *** *;
}
-dontwarn kotlinx.coroutines.**
