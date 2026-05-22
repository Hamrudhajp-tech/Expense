@echo off
title Expense Tracker - Java Project
color 0A

echo.
echo  ==========================================
echo    EXPENSE TRACKER WITH ALERTS
echo    Java Project - No Maven Required!
echo  ==========================================
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found!
    echo Please install Java 17+ from: https://adoptium.net
    echo After installing, restart this .bat file.
    pause
    exit /b 1
)

:: Compile if .class not found or .java is newer
if not exist "ExpenseTracker.class" goto compile
for /f %%i in ('dir /b /o:d ExpenseTracker.java ExpenseTracker.class 2^>nul') do set NEWER=%%i
if "%NEWER%"=="ExpenseTracker.java" goto compile
goto run

:compile
echo [INFO] Compiling...
javac ExpenseTracker.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed! Check ExpenseTracker.java
    pause
    exit /b 1
)
echo [OK] Compiled successfully!
echo.

:run
java ExpenseTracker

echo.
echo  Application closed.
pause
