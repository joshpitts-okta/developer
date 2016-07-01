@echo off
rem machine, person, cleanup, deploy
if "[%1]"=="[]" goto error
if /I %1==machine goto machine
if /I %1==person goto person
if /I %1==cleanup goto cleanup
if /I %1==deploy goto deploy
goto error

:machine
set cmdln=bind aws -c C:\wsp\developer\tests\config\dtf\runnerAws.properties -a C:\wsp\developer\tests\config\dtf\generatorAws.properties -m
goto execute

:person
set cmdln=bind aws -c C:\wsp\developer\tests\config\dtf\runnerAws.properties -a C:\wsp\developer\tests\config\dtf\generatorAws.properties -p
goto execute

:deploy
set cmdln=bind aws -c C:\wsp\developer\tests\config\dtf\runnerAws.properties -a C:\wsp\developer\tests\config\dtf\generatorAws.properties -gm
goto execute


:cleanup
set cmdln=bind aws -c C:\wsp\developer\tests\config\dtf\runnerAws.properties -a C:\wsp\developer\tests\config\dtf\generatorAws.properties -e
goto execute

:execute
set prjb=c:\wsp
set dtfb=%prjb%\testing-framework\platform
set devb=%prjb%\developer
set testsb=%devb%\tests
set tests=%testsb%\target\classes
set helpers=%devb%\helpers\target\classes

set classpath=%tests%;%helpers%
set classpath=%classpath%;%dtfb%\dtf-core\target\classes
set classpath=%classpath%;%dtfb%\dtf-aws-attr\target\classes
set classpath=%classpath%;%dtfb%\dtf-aws-resource\target\classes
set classpath=%classpath%;%dtfb%\dtf-runner\target\classes
set classpath=%classpath%;%testsb%\config
set classpath=%classpath%;%testsb%\lib\*

rem echo classpath=%classpath%

rem java com.pslcl.chad.tests.dtf.DtfCliApp bind aws -c C:\wsp\developer\tests\config\dtf\runnerAws.properties -a C:\wsp\developer\tests\config\dtf\generatorAws.properties -p
java com.pslcl.chad.tests.dtf.DtfCliApp %cmdln%
goto exit

:error
echo doit machine
echo doit deploy
echo doit person
echo doit cleanup

:exit

