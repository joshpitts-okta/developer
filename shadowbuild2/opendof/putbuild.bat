@echo off
if [%1]==[] goto error
if [%2]==[] goto error

set coreSrcBase=%1\core-java
set coreDestBase=%2\core-java

set datatransferSrcBase=%1\datatransfer
set datatransferDestBase=%2\datatransfer

set toolsdomainSrcBase=%1\tools-domain
set toolsdomainDestBase=%2\tools-domain

set irSrcBase=%1\tools-interface-repository
set irDestBase=%2\tools-interface-repository

echo on
copy %1\antbuild.bat %2
copy %1\pom.xml %2
echo off

::call %coreSrcBase%\getbuild.bat %coreSrcBase%  %coreDestBase%
::call %datatransferSrcBase%\getbuild.bat %datatransferSrcBase%  %datatransferDestBase%
::call %toolsdomainSrcBase%\getbuild.bat %toolsdomainSrcBase%  %toolsdomainDestBase%
echo %irSrcBase%\putbuild.bat %irSrcBase%  %irDestBase%
call %irSrcBase%\putbuild.bat %irSrcBase%  %irDestBase%

goto exit

:error
echo.
echo Typically this file is not executed stand alone, see ...\shadowbuild2\putbuild.bat
echo.
echo usage: putbuild ^<src-base^> ^<dest-base^>
echo where:
echo.   src-base is the base path the shadow build i.e. \wsp\developer\shadowbuild2\opendof
echo.   dest-base is the base path of the opendof modules i.e. \wso
echo.

:exit
echo.

