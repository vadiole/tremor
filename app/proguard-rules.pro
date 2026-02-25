# Self-contained R8 config — no default proguard-android-optimize.txt

-optimizationpasses 5
-overloadaggressively
-allowaccessmodification
-repackageclasses ""
-renamesourcefileattribute ""

# Strip all attributes — no reflection, no serialization, no annotation processing

# Kotlin null-check intrinsics
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
  public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
  public static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
  public static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
  public static void checkNotNull(java.lang.Object);
  public static void checkNotNull(java.lang.Object, java.lang.String);
  public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
  public static void checkNotNullParameter(java.lang.Object, java.lang.String);
  public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
  public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
  public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
}

# Strip all Log calls
-assumenosideeffects class android.util.Log {
  public static int v(...);
  public static int d(...);
  public static int i(...);
  public static int w(...);
  public static int e(...);
  public static int wtf(...);
}

-dontwarn kotlin.**
-dontnote kotlin.**
