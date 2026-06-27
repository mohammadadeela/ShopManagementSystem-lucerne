@echo off
cd /d "%~dp0\.."
echo WARNING: database\lucerne_demo_final.sql DROPS and recreates lucerne_demo.
set /p CONFIRM=Type IMPORT-DEMO to continue: 
if /I not "%CONFIRM%"=="IMPORT-DEMO" (echo Cancelled.& exit /b 1)
mysql -u root -p < database\lucerne_demo_final.sql
