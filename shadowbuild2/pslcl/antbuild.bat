@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto first

:nuke
::let sub build nuke their own
::rd /s \Users\cadams\.ivy2\cache
::rd /s \Users\cadams\.ivy2\local

:first
cd testing-framework
call antbuild %1
if "%errorlevel%"=="0" goto ok
echo "testing-framework build failed"
goto exit

:ok
echo "testing-framework build ok"

:exit
endlocal