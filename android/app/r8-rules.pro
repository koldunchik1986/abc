# R8 оптимизации для ускорения сборки
# Этот файл содержит специфичные для R8 правила

# Включить все оптимизации R8
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively

# Оптимизация строк и констант
-optimizations !code/simplification/string,!code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Разрешить удаление неиспользуемых параметров
-optimizations !method/removal/parameter

# Отключить verbose логи для ускорения
-verbose

# Использовать параллельную обработку
-dontpreverify

# Оптимизации для Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
}

# Удаление debug кода в release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}