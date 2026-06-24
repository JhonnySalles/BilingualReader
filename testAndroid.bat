@echo off
setlocal

echo.
echo ===================================================
echo      Iniciando o aplicativo e realizando teste!
echo ===================================================
echo.

:: Executa os testes de interface de usuário (UI) do app
:: connectedDebugAndroidTest : Executa todos os testes de interface de usuário (UI)
:: connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.fenix.bilingualreader.view.ui.library.manga.MangaLibraryFragmentTest : Executa apenas os testes da classe MangaLibraryFragmentTest
:: connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.fenix.bilingualreader.view.ui.library.book.BookLibraryFragmentTest : Executa apenas os testes da classe BookLibraryFragmentTest

call gradlew connectedDebugAndroidTest
pause