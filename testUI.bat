@echo off
setlocal

echo.
echo ===================================================
echo              Iniciando os testes de UI!
echo ===================================================
echo.

:: Executa os testes de UI do app
:: connectedAndroidTest : Executa os testes de UI
:: -Pandroid.testInstrumentationRunnerArguments.class : Classe do teste a ser executado

call gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.fenix.bilingualreader.view.ui.menu.ConfigLibrariesFragmentTest
pause