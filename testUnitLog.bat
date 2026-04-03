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
:: > test_unit.txt : Redireciona a saída para o arquivo test_unit.txt
:: 2>&1 : Redireciona a saída de erro para a saída padrão

call gradlew --stop
call gradlew clean testDebugUnitTest -PshowTests > test_unit.txt 2>&1