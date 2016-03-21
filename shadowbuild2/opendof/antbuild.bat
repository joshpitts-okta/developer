@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto first

:nuke
rd /s \Users\cadams\.ivy2\cache
rd /s \Users\cadams\.ivy2\local

:first
cd core-java
call antbuild %1
if "%errorlevel%"=="0" goto ir
echo "core-java build failed"
goto exit

:ir
cd \wso\tools-interface-repository
echo "core-java build ok"
call antbuild %1
if "%errorlevel%"=="0" goto dt
echo "tools-interface-repository build failed"
goto exit

:dt
cd \wso\datatransfer
echo "tools-interface-repository build ok"
call antbuild %1
if "%errorlevel%"=="0" goto ok
echo "datatransfer build failed"
goto exit

:ok
echo "datatransfer build ok"

:exit