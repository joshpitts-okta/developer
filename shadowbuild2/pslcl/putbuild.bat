@echo off
if [%1]==[] goto error
if [%2]==[] goto error

set coreSrcBase=%1\platform\core-java
set coreDestBase=%2\platform\core-java

echo on
copy %1\antbuild.bat %2
copy %1\pom.xml %2

copy %1\testing-framework\pom.xml %2\testing-framework
copy %1\testing-framework\antbuild.bat %2\testing-framework

copy %1\testing-framework\platform\pom.xml %2\testing-framework\platform
copy %1\testing-framework\platform\antbuild.bat %2\testing-framework\platform

copy %1\testing-framework\platform\dtf-aws-resource\pom.xml %2\testing-framework\platform\dtf-aws-resource
copy %1\testing-framework\platform\dtf-core\pom.xml %2\testing-framework\platform\dtf-core
copy %1\testing-framework\platform\dtf-ivy-artifact\pom.xml %2\testing-framework\platform\dtf-ivy-artifact
copy %1\testing-framework\platform\dtf-runner\pom.xml %2\testing-framework\platform\dtf-runner

echo off
goto exit

:error
echo.
echo Typically this file is not executed stand alone, see ...\shadowbuild2\getbuild.bat
echo.
echo usage: getbuild ^<src-base^> ^<dest-base^>
echo where:
echo.   src-base is the base path of the shadow build i.e. \wsp\developer\shadowbuild2
echo.   dest-base is the base path the sasg modules i.e. \wsp
echo.

:exit
echo.

