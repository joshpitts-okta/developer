@echo off
if [%1]==[] goto error
if %1==home goto home

:work
set srcBase=\wsp\developer\shadowbuild2
set opendofDestBase=\wso
set sasgDestBase=\wsp
goto doit

:home
set srcBase=\wsp\developer\shadowbuild2
set opendofDestBase=\wsp\opendof
set sasgDestBase=\wsp\sasg

:doit
echo on
copy %srcBase%\opendof\antbuild.bat %opendofDestbase%
copy %srcBase%\opendof\pom.xml %opendofDestbase%
copy %srcBase%\pslcl\antbuild.bat %sasgDestbase%
copy %srcBase%\pslcl\pom.xml %sasgDestbase%
echo off

echo %srcBase%\opendof\putbuild.bat %srcBase%\opendof %opendofDestbase%
call %srcBase%\opendof\putbuild.bat %srcBase%\opendof %opendofDestbase%

echo %srcBase%\pslcl\putbuild.bat %srcBase%\pslcl %sasgDestbase%
call %srcBase%\pslcl\putbuild.bat %srcBase%\pslcl %sasgDestbase%
goto exit

:error
echo.
echo.
echo usage: putbuild ^<home ^| work^>
echo.

:exit
echo.

