@echo off
setlocal

echo.
echo ===================================================
echo              Iniciando os testes!
echo ===================================================
echo.

:: Executa os testes unitários do app
:: clean : Limpa o projeto
:: testDebugUnitTest : Executa os testes unitários
:: -PshowTests : Mostra os testes

call gradlew --stop
call gradlew clean testDebugUnitTest -PshowTests --no-daemon
pause