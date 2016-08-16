Interface Repository Version 2.0 Migration
====
***

**Summery**

* Tomcat deploy new ROOT.war
* sql execute migration script
* custom cli - `doit 2` about 1.75 minutes
* custom cli - `doit 3` about 25 minutes
* custom cli - `doit 4` about 2.25 minutes
* custom cli - `doit 5` about 6 minutes
* sql execute remove reg 63 script

**Detailed**

Migrating from version 1.x to 2.0 requires an update to the database schema as well as several manual steps. The Interface Repository CLI and administrator access to the database are required in order to perform the data migration.

Before starting the migration process, ensure that you have created a backup of your MySQL database. Then, do the following:

1. Tomcat, stop and unload existing servlet.
2. Backup the current database.
3. Copy [War File](https://asset.opendof.org/artifact/org.opendof.tools-interface-repository/interface-repository-web/2.0/wars/interface-repository-web-2.0.war) to say `...\base\ROOT.war` (see step 4 for definition of base).  This is already in my share base if you want.
4. Tomcat, deploy then stop the new `ROOT.war` 
5. Unzip the custom tool to some `base` (or use `\\cadams\share\interfaceRepository`)
6. edit `...\base\bin\doit.bat` fix it up to point to Gavin's environment.
7. edit `...\base\bin\interface-repository.properties` fix it up to point to Gavin's environment.
8. Migrate the MySQL database schema. Execute `/interface-repository/migration/2.0/v1tov2migration.sql` [tarball containing v1tov2migration.sql](https://asset.opendof.org/artifact/org.opendof.tools-interface-repository/interface-repository/2.0/dists/tools-interface-repository-interface-repository-2.0.noarch.tar.gz) or `...\base\scripts\v1tov2migration.sql` using your preferred MySQL client in order to migrate the schema to version 2.0.
9. Tomcat start the ROOT deployment.
10. Using the CLI, create any subrepos required for tracking identifiers for existing interfaces. For example, OpenDOF might require a subrepo for registry 1, which in turn would contain subrepos for interfaces of length 1, 2 and 4 bytes. See CLI documentation for the correct command usage. `...\base\bin\doit 2`
11. Using the CLI, set the group for each interface to one of the default groups created for version 2.0. The 'anonymous' group allows public read access to the interface. The private group signifies that only the submitter may read the interface. `...\base\bin\doit 3`
12. Using the CLI, synchronize the new database tables for tracking allocated identifiers with existing interfaces. To do this, execute the following command: `irmanage interface sync`. `...\base\bin\doit 4`
13. Using the CLI, set the submitter for each interface as the user that should have rights to edit the interface. If using the default JIRA authentication, the user's email should correspond with the JIRA user account. See CLI documentation for the correct command usage. `...\base\bin\doit 5`
14. delete the existing registry 3 entry via sql `delete63.sql`

