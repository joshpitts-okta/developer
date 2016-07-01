@echo off
setlocal
if "[%1]"=="[]" goto nuke
set cleanbuild="true"
goto first

:nuke
rd /s \Users\cadams\.ivy2\cache\org.opendof.core-java
rd /s \Users\cadams\.ivy2\cache\org.opendof.core-java-internal
rd /s \Users\cadams\.ivy2\local\org.opendof.core-java
rd /s \Users\cadams\.ivy2\local\org.opendof.core-java-internal
	  
:first
cd dof-oal
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto inet
echo "dof-oal build failed"
goto exit

:inet
echo "dof-oal build ok"
cd ..\dof-inet
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto domain
echo "dof-inet build failed"
goto exit

:domain
echo "dof-inet build ok"
cd ../dof-domain-management
call antbuild %1
if "%errorlevel%"=="0" goto listeners
echo "dof-domain-management group build failed"
goto exit

:listeners
echo "dof-domain-management group build ok"
cd ../dof-listeners
call antbuild %1
if "%errorlevel%"=="0" goto ok
echo "dof-listeners group build failed"
goto exit

:ok
cd ..\..
echo "dof-listeners group build ok"
goto exit

:exit
endlocal