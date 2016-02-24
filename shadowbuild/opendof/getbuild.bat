@echo off
set cleanbuild="true"
set base="\wso\"
copy %base%antbuild.bat 
copy %base%pom.xml 

#copy %base%core-java\pom.xml core-java
#copy %base%core-java\antbuild.bat core-java
#
#copy %base%core-java\dof-domain-management\pom.xml core-java\dof-domain-management
#copy %base%core-java\dof-domain-management\antbuild.bat core-java\dof-domain-management
#
#copy %base%core-java\dof-domain-management\dof-domain-io\pom.xml core-java\dof-domain-management\dof-domain-io
#copy %base%core-java\dof-domain-management\dof-domain-management\pom.xml core-java\dof-domain-management\dof-domain-management
#
#copy %base%core-java\dof-domain-management\dof-domain-management-command-line\pom.xml core-java\dof-domain-management\dof-domain-management-command-line
#copy %base%core-java\dof-domain-management\dof-domain-management-command-line\antbuild.bat core-java\dof-domain-management\dof-domain-management-command-line
#copy %base%core-java\dof-domain-management\dof-domain-management-command-line\dof-domain-management-command-line\pom.xml core-java\dof-domain-management\dof-domain-management-command-line\dof-domain-management-command-line
#copy %base%core-java\dof-domain-management\dof-domain-management-command-line\dof-domain-management-command-line-cred-gen\pom.xml core-java\dof-domain-management\dof-domain-management-command-line\dof-domain-management-command-line-cred-gen
#
#copy %base%core-java\dof-domain-management\dof-domain-storage-javadb\pom.xml core-java\dof-domain-management\dof-domain-storage-javadb
#copy %base%core-java\dof-domain-management\dof-domain-storage-javadb\antbuild.bat core-java\dof-domain-management\dof-domain-storage-javadb
#copy %base%core-java\dof-domain-management\dof-domain-storage-javadb\dof-domain-storage-javadb\pom.xml core-java\dof-domain-management\dof-domain-storage-javadb\dof-domain-storage-javadb
#copy %base%core-java\dof-domain-management\dof-domain-storage-javadb\dof-domain-storage-javadb-installer\pom.xml core-java\dof-domain-management\dof-domain-storage-javadb\dof-domain-storage-javadb-installer
#copy %base%core-java\dof-domain-management\dof-domain-storage-javadb\dof-domain-storage-javadb-routines\pom.xml core-java\dof-domain-management\dof-domain-storage-javadb\dof-domain-storage-javadb-routines
#
#copy %base%core-java\dof-inet\pom.xml core-java\dof-inet
#
#copy %base%core-java\dof-listeners\pom.xml core-java\dof-listeners
#copy %base%core-java\dof-listeners\antbuild.bat core-java\dof-listeners
#copy %base%core-java\dof-listeners\dof-connection-reconnecting-listener\pom.xml core-java\dof-listeners\dof-connection-reconnecting-listener
#copy %base%core-java\dof-listeners\dof-server-restarting-listener\pom.xml core-java\dof-listeners\dof-server-restarting-listener
#copy %base%core-java\dof-listeners\dof-slf4j-log-listener\pom.xml core-java\dof-listeners\dof-slf4j-log-listener
#
#copy %base%datatransfer\pom.xml datatransfer
#copy %base%datatransfer\antbuild.bat datatransfer
#copy %base%datatransfer\java\dof-data-transfer-common\pom.xml datatransfer\java\dof-data-transfer-common
#copy %base%datatransfer\java\dof-data-transfer-manager\pom.xml datatransfer\java\dof-data-transfer-manager
#copy %base%datatransfer\java\dof-data-transfer-sink\pom.xml datatransfer\java\dof-data-transfer-sink
#copy %base%datatransfer\java\dof-data-transfer-snapshot\pom.xml datatransfer\java\dof-data-transfer-snapshot
#copy %base%datatransfer\java\dof-data-transfer-source\pom.xml datatransfer\java\dof-data-transfer-source
#
#copy %base%tools-domain\pom.xml tools-domain
#copy %base%tools-domain\antbuild.bat tools-domain
#copy %base%tools-domain\dof-javadb-as\pom.xml tools-domain\dof-javadb-as
#copy %base%tools-domain\dof-json-as\pom.xml tools-domain\dof-json-as
#		   
copy %base%tools-interface-repository\pom.xml tools-interface-repository
copy %base%tools-interface-repository\antbuild.bat tools-interface-repository
rem copy %base%tools-interface-repository\interface-repository\pom.xml tools-interface-reposistory\interface-repository
copy %base%tools-interface-repository\interface-repository-allseen\pom.xml tools-interface-repository\interface-repository-allseen
copy %base%tools-interface-repository\interface-repository-cli\pom.xml tools-interface-repository\interface-repository-cli
copy %base%tools-interface-repository\interface-repository-core\pom.xml tools-interface-repository\interface-repository-core
copy %base%tools-interface-repository\interface-repository-data-accessor\pom.xml tools-interface-repository\interface-repository-data-accessor
copy %base%tools-interface-repository\interface-repository-mysql\pom.xml tools-interface-repository\interface-repository-mysql
copy %base%tools-interface-repository\interface-repository-opendof\pom.xml tools-interface-repository\interface-repository-opendof
copy %base%tools-interface-repository\interface-repository-servlet\pom.xml tools-interface-repository\interface-repository-servlet
copy %base%tools-interface-repository\interface-repository-web\pom.xml tools-interface-repository\interface-repository-web
