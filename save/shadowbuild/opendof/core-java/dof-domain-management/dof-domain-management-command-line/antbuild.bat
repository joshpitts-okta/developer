@echo off
setlocal
if "[%1]"=="[]" goto first
set cleanbuild="true"

:first
cd dof-domain-management-command-line
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto cred
echo "dof-domain-management-command-line build failed"
goto exit

:cred
echo "dof-domain-management-command-line build ok"
cd ..\dof-domain-management-command-line-cred-gen
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto ok
echo "dof-domain-management-command-line-cred-gen build failed"
goto exit

:ok
echo "dof-domain-management-command-line-cred-gen build ok"
cd ..

:exit
endlocal