@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto first

:nuke
rd /s \Users\cadams\.ivy2\cache\org.opendof.datatransfer-java
rd /s \Users\cadams\.ivy2\local\org.opendof.datatransfer-java

:first
cd java
cd dof-data-transfer-common
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto manager
echo "dof-data-transfer-common build failed"
goto exit

:manager
echo "dof-data-transfer-common build ok"
cd ..\dof-data-transfer-manager
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto sink
echo "dof-data-transfer-manager build failed"
goto exit

:sink
echo "dof-data-transfer-manager build ok"
cd ..\dof-data-transfer-sink
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto snap
echo "dof-data-transfer-sink build failed"
goto exit

:snap
echo "dof-data-transfer-sink build ok"
cd ..\dof-data-transfer-snapshot
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto source
echo "dof-data-transfer-snapshot build failed"
goto exit

:source
echo "dof-data-transfer-snapshot build ok"
cd ..\dof-data-transfer-source
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto ok
echo "dof-data-transfer-source build failed"
goto exit

:ok
cd ..\..\..
echo "dof-data-transfer-source build ok"

:exit
endlocal