@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto first

:nuke
rd /s \Users\cadams\.ivy2\cache\org.opendof.tools-domain
rd /s \Users\cadams\.ivy2\local\org.opendof.tools-domain

:first
cd java
cd dof-javadb-as
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto json
echo "dof-javadb-as build failed"
goto exit

:json
echo "dof-javadb-as build ok"
cd ..\dof-json-as
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto ok
echo "dof-json-as build failed"
goto exit

:ok
cd ..\..\..
echo "dof-data-transfer-source build ok"

:exit
endlocal