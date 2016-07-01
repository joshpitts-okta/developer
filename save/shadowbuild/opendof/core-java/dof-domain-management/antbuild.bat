@echo off
setlocal
if "[%1]"=="[]" goto first
set cleanbuild="true"

:first
cd dof-domain-management
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto io
echo "dof-domain-management build failed"
goto exit

:io
echo "dof-domain-io build ok"
cd ..\dof-domain-io
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto cl
echo "dof-domain-io build failed"
goto exit

:cl
echo "dof-domain-management build ok"
cd ..\dof-domain-management-command-line
call antbuild %1
if "%errorlevel%"=="0" goto javadb
echo "dof-domain-management-command-line build failed"
goto exit

:javadb
echo "dof-domain-management-command-line build ok"
cd ..\dof-domain-storage-javadb
call antbuild %1
if "%errorlevel%"=="0" goto ok
echo "dof-domain-storage-javadb build failed"
goto exit

:ok
echo "dof-domain-storage-javadb build ok"
cd ..

:exit
endlocal