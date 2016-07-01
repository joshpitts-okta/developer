@echo off
setlocal
if "[%1]"=="[]" goto first
set cleanbuild="true"
	  
:first
cd dof-connection-reconnecting-listener
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto server
echo "dof-connection-reconnecting-listener build failed"
goto exit

:server
echo "dof-connection-reconnecting-listener build ok"
cd ..\dof-server-restarting-listener
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto slf
echo "dof-server-restarting-listener build failed"
goto exit

:slf
echo "dof-server-restarting-listener build ok"
cd ..\dof-slf4j-log-listener
IF "%cleanbuild%"=="" (call ant -q dist-src dist publish) ELSE (call ant -q clean)
if "%errorlevel%"=="0" goto ok
echo "dof-slf4j-log-listener build failed"
goto exit

:ok
echo "dof-slf4j-log-listener build ok"
cd ..

:exit