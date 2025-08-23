@echo off
REM Simple ANL Release build with Oracle JDK 17
set JAVA_HOME=C:\Program Files\java\jdk-17
set PATH=C:\Program Files\java\jdk-17\bin;%PATH%

REM Oracle JDK 17 compatibility settings
set GRADLE_OPTS=-Dorg.gradle.java.home="C:\Program Files\java\jdk-17"
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.useAndroidJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.enableJdkToolchain=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dandroid.experimental.enableJdkImage=false
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.jvmargs="-Xmx6144m -XX:MaxMetaspaceSize=512m"
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.daemon=false

echo Сборка ANL Release (Oracle JDK 17)
echo JAVA_HOME: %JAVA_HOME%

REM Run without clean to avoid file locking issues
gradlew.bat --no-daemon --no-build-cache --offline assembleRelease