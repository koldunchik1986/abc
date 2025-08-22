@echo off
REM Финальная сборка ABClient Release с полным обходом JDK image проблем
set JAVA_HOME=C:\Program Files\java\jdk-17
set PATH=C:\Program Files\java\jdk-17\bin;%PATH%

REM Максимальные настройки для обхода JDK image трансформации
set GRADLE_OPTS=-Dorg.gradle.java.home="C:\Program Files\java\jdk-17"
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.useAndroidJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.enableJdkToolchain=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.experimental.enableJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.enableDexingArtifactTransform=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.experimental.enableJdkDesugar=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.java.installations.auto-detect=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.java.installations.auto-download=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.configuration-cache=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.daemon=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.parallel=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dkotlin.compiler.execution.strategy=in-process
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=512m"

echo Финальная сборка ABClient Release
echo JAVA_HOME: %JAVA_HOME%
echo Отключены все JDK image функции

REM Останавливаем все демоны для чистого старта
gradlew.bat --stop 2>nul
timeout /t 2 /nobreak >nul

REM Запускаем сборку с отключенными демонами и кешем
gradlew.bat --no-daemon --no-build-cache --no-configuration-cache --offline --stacktrace assembleRelease