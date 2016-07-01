@echo off
setlocal
if "[%1]"=="[]" goto first
set cleanbuild="true"
	  
:first
cd dof-domain-storage-javadb
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto installer
echo "dof-domain-storage-javadb build failed"
goto exit

:installer
echo "dof-domain-storage-javadb build ok"
cd ..\dof-domain-storage-javadb-installer
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto routines
echo "dof-domain-storage-javadb-installer build failed"
goto exit

:routines
echo "dof-domain-storage-javadb-installer build ok"
cd ..\dof-domain-storage-javadb-routines
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto ok
echo "dof-domain-storage-javadb-routines build failed"
goto exit

:ok
echo "dof-domain-storage-javadb-routines build ok"
cd ..

:exit
endlocal