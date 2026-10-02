@echo off
setlocal enabledelayedexpansion

title Booking Concurrency Test

echo.
echo ============================================================
echo             BOOKING CONCURRENCY TEST
echo ============================================================
echo.

REM ------------------------------------------------------------
REM Check whether k6 is installed
REM ------------------------------------------------------------

where k6 >nul 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: k6 is not installed or not available in PATH.
    echo.
    echo Please install k6 and try again.
    echo.
    pause
    exit /b 1
)

REM ------------------------------------------------------------
REM Configuration
REM ------------------------------------------------------------

set /p SHOW_ID=Enter Show ID: 

if "%SHOW_ID%"=="" (
    echo.
    echo ERROR: Show ID cannot be empty.
    echo.
    pause
    exit /b 1
)

set BASE_URL=https://reservation-concurrency-app.onrender.com

echo.
set /p BASE_URL_INPUT=Enter Base URL [https://reservation-concurrency-app.onrender.com]: 

if not "%BASE_URL_INPUT%"=="" (
    set BASE_URL=%BASE_URL_INPUT%
)

echo.
set /p VUS=Enter number of concurrent users [100]: 

if "%VUS%"=="" (
    set VUS=100
)

echo.
set /p SEATS=Enter seats to test (comma-separated) [A2]: 

if "%SEATS%"=="" (
    set SEATS=A2
)

echo.
echo ============================================================
echo TEST CONFIGURATION
echo ============================================================
echo.
echo Base URL          : %BASE_URL%
echo Show ID           : %SHOW_ID%
echo Seats             : %SEATS%
echo Concurrent Users  : %VUS%
echo.
echo ============================================================
echo.
echo Starting test...
echo.

REM ------------------------------------------------------------
REM Run k6
REM ------------------------------------------------------------

k6 run ^
  -e BASE_URL=%BASE_URL% ^
  -e SHOW_ID=%SHOW_ID% ^
  -e VUS=%VUS% ^
    -e SEATS="%SEATS%" ^
  booking-concurrency.js

echo.
echo ============================================================
echo Test execution completed.
echo ============================================================
echo.

pause