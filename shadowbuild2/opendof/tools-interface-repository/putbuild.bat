@echo off
if [%1]==[] goto error
if [%2]==[] goto error

@echo on
copy %1\pom.xml %2
copy %1\antbuild.bat %2

copy %1\interface-repository-allseen\pom.xml %2\interface-repository-allseen
copy %1\interface-repository-cli\pom.xml %2\interface-repository-cli
copy %1\interface-repository-core\pom.xml %2\interface-repository-core
copy %1\interface-repository-data-accessor\pom.xml %2\interface-repository-data-accessor
copy %1\interface-repository-mysql\pom.xml %2\interface-repository-mysql
copy %1\interface-repository-opendof\pom.xml %2\interface-repository-opendof
copy %1\interface-repository-servlet\pom.xml %2\interface-repository-servlet
copy %1\interface-repository-web\pom.xml %2\interface-repository-web

echo off
goto exit

:error
echo.
echo Typically this file is not executed stand alone, see ...\shadowbuild2\putbuild.bat
echo.
echo usage: putbuild ^<src-base^> ^<dest-base^>
echo where:
echo.   src-base is the base path of the shadow build i.e. \wsp\developer\shadowbuild2\opendof\tools-interface-repository
echo.   dest-base is the base path the opendof modules i.e. \wso\opendof\tools-interface-repository
echo.

:exit
echo.
