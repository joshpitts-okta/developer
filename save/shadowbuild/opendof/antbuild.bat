@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto first

:nuke
:: let sub builds nuke their own
::rd /s \Users\cadams\.ivy2\cache\org.opendof.core-java
::rd /s \Users\cadams\.ivy2\cache\org.opendof.datatransfer-java
::rd /s \Users\cadams\.ivy2\cache\org.opendof.dof-javadb-as
::rd /s \Users\cadams\.ivy2\cache\org.opendof.dof-json-as
::rd /s \Users\cadams\.ivy2\cache\org.tools-interface-repository
::rd /s \Users\cadams\.ivy2\local\org.opendof.core-java
::rd /s \Users\cadams\.ivy2\local\org.opendof.core-java-internal
::rd /s \Users\cadams\.ivy2\local\org.opendof.datatransfer-java
::rd /s \Users\cadams\.ivy2\local\org.opendof.tools-interface-repository

:first
cd core-java
call antbuild %1
if "%errorlevel%"=="0" goto domain
echo "core-java build failed"
goto exit

:domain
cd \wso\tools-domain
echo "core-java build ok"
goto ir
call antbuild %1
if "%errorlevel%"=="0" goto ir
echo "tools-domain build failed"
goto exit

:ir
cd \wso\tools-interface-repository
echo "tools-domain build skipped"
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
endlocal
