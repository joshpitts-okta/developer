@echo off
if "[%1]"=="[]" goto error
if /I %1==deploy goto deploy
if /I %1==install goto install
goto error

:deploy
:install
call mvn %1 clean 
cd third-parties
call mvn %1 clean 
cd 	dependency-management
call mvn %1 clean 

cd ..\..
goto exit

:error
echo "buildParents [install | deploy]"

:exit
