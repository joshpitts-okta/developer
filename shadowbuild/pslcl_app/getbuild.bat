@echo on
set cleanbuild="true"
set base="\wsp\"
copy %base%antbuild.bat 
copy %base%pom.xml 

copy %base%eclipse\pom.xml eclipse

copy %base%testing-framework\platform\pom.xml testing-framework
copy %base%testing-framework\platform\antbuild.bat testing-framework

copy %base%testing-framework\platform\dtf-aws-attr\pom.xml testing-framework\platform\dtf-aws-attr
copy %base%testing-framework\platform\dtf-aws-resource\pom.xml testing-framework\platform\dtf-aws-resource
copy %base%testing-framework\platform\dtf-core\pom.xml testing-framework\platform\dtf-core
copy %base%testing-framework\platform\dtf-exec\pom.xml testing-framework\platform\dtf-exec
copy %base%testing-framework\platform\dtf-ivy-artifact\pom.xml testing-framework\platform\dtf-ivy-artifact
copy %base%testing-framework\platform\dtf-runner\pom.xml testing-framework\platform\dtf-runner
