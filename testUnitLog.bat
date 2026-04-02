@echo off
setlocal

echo.
echo ===================================================
echo              Iniciando os testes!
echo ===================================================
echo.

call gradlew --stop
call gradlew clean testDebugUnitTest -PshowTests > test_unit.txt 2>&1