@echo off
REM Set JAVA_HOME to GraalVM to avoid JDK image transformation issues
set JAVA_HOME=C:\Program Files\graalvm-jdk-21
set PATH=C:\Program Files\graalvm-jdk-21\bin;%PATH%

REM Disable Android JDK image features completely
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.java.home="C:\Program Files\graalvm-jdk-21"
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.useAndroidJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.enableJdkToolchain=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.experimental.enableJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.enableDexingArtifactTransform=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.experimental.enableJdkDesugar=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.java.installations.auto-detect=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.java.installations.auto-download=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.configuration-cache=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.jvmargs="-Xmx8192m -XX:MaxMetaspaceSize=1024m -Djdk.lang.Process.launchMechanism=vfork"

echo Сборка ABClient Release с GraalVM JDK 21
echo JAVA_HOME: %JAVA_HOME%
echo GRADLE_OPTS: %GRADLE_OPTS%

REM Полная очистка build директории с отключением кеша
echo Полная очистка build и cache директорий...
if exist app\build (
    echo Остановка Gradle daemon...
    gradlew.bat --stop 2>nul
    timeout /t 3 /nobreak >nul
    
    echo Удаление build директории...
    rmdir /s /q app\build 2>nul
    if exist app\build (
        echo Принудительное удаление с taskkill...
        taskkill /f /im java.exe 2>nul
        timeout /t 2 /nobreak >nul
        rmdir /s /q app\build 2>nul
    )
)

REM Очищаем Gradle кеш
echo Очистка Gradle cache...
rmdir /s /q "%USERPROFILE%\.gradle\caches\transforms-3" 2>nul
rmdir /s /q "%USERPROFILE%\.gradle\caches\build-cache-1" 2>nul

echo Запуск сборки без кеша...
gradlew.bat --no-daemon --no-configuration-cache --no-build-cache --stacktrace --init-script init-disable-jdk-image.gradle -Pandroid.useAndroidJdkImage=false -Pandroid.enableJdkToolchain=false -Pandroid.experimental.enableJdkImage=false assembleRelease