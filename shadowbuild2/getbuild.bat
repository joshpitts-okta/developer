@echo off
if [%1]==[] goto error
if %1==home goto home

:work
set destbase=\wsp\developer\shadowbuild2
set opendofsrcbase=\wso
set sasgsrcbase=\wsp
goto doit

:home
set destbase=\wsp\developer\shadowbuild2
set opendofsrcbase=\wsp
set sasgsrcbase=\wsp

:doit
echo opendof\getbuild.bat %opendofsrcbase% %destbase%\opendof
call opendof\getbuild.bat %opendofsrcbase% %destbase%\opendof

echo pslcl\getbuild.bat %sasgsrcbase% %destbase%\pslcl
call pslcl\getbuild.bat %sasgsrcbase% %destbase%\pslcl
goto exit

:error
echo.
echo.
echo usage: getbuild ^<home ^| work^>
echo.

:exit
echo.

