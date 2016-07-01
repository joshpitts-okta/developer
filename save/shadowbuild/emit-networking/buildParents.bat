@echo off
if "[%1]"=="[]" goto error
if /I %1==deploy goto deploy
if /I %1==install goto install
goto error

:deploy
:install
call mvn %1 pom.xml 
call mvn %1 -fthird-parties\pom.xml 
call mvn %1 -fthird-parties\bundled-projects\pom.xml 
call mvn %1 -fthird-parties\bundled-projects\dependency-management\pom.xml 
rem call mvn %1 -fthird-parties\bundled-projects\dependency-management\internal-release-set\pom.xml 
rem call mvn %1 -fthird-parties\bundled-projects\dependency-management\pre-release-set\pom.xml 
rem call mvn %1 -fthird-parties\bundled-projects\dependency-management\release-set\pom.xml 
rem call mvn %1 -fthird-parties\bundled-projects\dependency-management\stage-release-set\pom.xml 
rem call mvn %1 -fthird-parties\bundled-projects\dependency-management\testing-release-set\pom.xml 

:error
echo "buildTree [install | deploy]"

:exit
