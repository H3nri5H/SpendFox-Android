# Keep release builds small and avoid leaking debug log calls if any are added later.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
