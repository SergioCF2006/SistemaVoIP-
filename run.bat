@echo off
echo ========================================================
echo   Compilando y Ejecutando Sistema VoIP (Proyecto SO)
echo ========================================================

if not exist bin mkdir bin

echo Compilando archivos Java para Java 17 (--release 17)...
javac --release 17 -encoding UTF-8 -d bin src\com\voip\model\*.java src\com\voip\registry\*.java src\com\voip\core\*.java src\com\voip\sip\*.java src\com\voip\rtp\*.java src\com\voip\gui\*.java src\com\voip\Main.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Error al compilar el código Java.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Iniciando Sistema VoIP...
java -cp bin com.voip.Main
