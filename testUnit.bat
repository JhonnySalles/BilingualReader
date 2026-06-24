@echo off
setlocal

echo.
echo ===================================================
echo              Iniciando os testes!
echo ===================================================
echo.

call gradlew clean testDebugUnitTest -PshowTests