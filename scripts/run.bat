@echo off
cd /d "%~dp0\.."
where java >nul 2>nul || (echo Java 21 is required.& exit /b 1)
where mvn >nul 2>nul || (echo Apache Maven 3.9+ is required.& exit /b 1)
findstr /x "db.password=CHANGE_ME" config\database.properties >nul 2>nul && (
  echo Edit config\database.properties and set your MySQL password first.
  exit /b 1
)
mvn clean javafx:run
