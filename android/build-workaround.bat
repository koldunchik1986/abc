@echo off
echo Попытка обхода JDK Image проблемы через системные переменные...

REM Устанавливаем переменные для отключения JDK модулей
set JAVA_TOOL_OPTIONS=-Djdk.module.main=
set _JAVA_OPTIONS=-Djdk.module.path=

REM Настройка GraalVM
set JAVA_HOME=C:\Program Files\java\jdk-17
set PATH=C:\Program Files\java\jdk-17\bin;%PATH%

REM Простые Gradle опции
set GRADLE_OPTS=-Xmx4096m -XX:MaxMetaspaceSize=512m
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.java.home="C:\Program Files\java\jdk-17"
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.daemon=false

echo Сборка с обходом JDK modules...
echo JAVA_HOME: %JAVA_HOME%

cd /d c:\Users\User\nlk\abclient\android

REM Пробуем сборку только с assembleRelease
gradlew.bat --no-daemon assembleRelease