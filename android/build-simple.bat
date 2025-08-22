@echo off
REM Simple ANL Release build with GraalVM JDK 21
set JAVA_HOME=C:\Program Files\graalvm-jdk-21
set PATH=C:\Program Files\graalvm-jdk-21\bin;%PATH%

REM GraalVM JDK 21 compatibility settings
set GRADLE_OPTS=-Dorg.gradle.java.home="C:\Program Files\graalvm-jdk-21"
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.useAndroidJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.enableJdkToolchain=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.experimental.enableJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=512m"
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.daemon=false

echo Сборка ANL Release (GraalVM JDK 21)
echo JAVA_HOME: %JAVA_HOME%

REM Run without clean to avoid file locking issues
gradlew.bat --no-daemon --no-build-cache --offline assembleRelease