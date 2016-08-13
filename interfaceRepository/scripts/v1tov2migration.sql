USE `interface_repository`;
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;

drop table if exists `registry`;
drop table if exists `reserved`;

DROP TABLE IF EXISTS `holes`;
CREATE TABLE `holes` (
  `holesPk` int(11) NOT NULL AUTO_INCREMENT,
  `subrepoFk` int(11) NOT NULL,
  `min` bigint(20) NOT NULL,
  `max` bigint(20) NOT NULL,
  PRIMARY KEY (`holesPk`),
  KEY `holes_subrepoFk_Idx` (`subrepoFk`),
  CONSTRAINT `holes_subrepoFk_Cst` FOREIGN KEY (`subrepoFk`) REFERENCES `subrepo` (`subrepoPk`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sub-Repository tree node';

DROP TABLE IF EXISTS `interface_new`;
CREATE TABLE `interface_new` (
  `ifacePk` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'interface primary autoincrementing key of interfaces',
  `iid` text CHARACTER SET latin1 NOT NULL COMMENT 'The globally unique Interface ID', 
  `xml` mediumtext CHARACTER SET utf8mb4 COMMENT 'The interface source XML',
  `version` varchar(128) CHARACTER SET utf8mb4 NOT NULL COMMENT 'interface version',
  `repotype` varchar(184) CHARACTER SET utf8mb4 NOT NULL COMMENT 'The repository type (i.e. opendof, allseen)',
  `submitterFk` bigint(10) unsigned NOT NULL COMMENT 'The submitters Fk',
  `groupFk` bigint(10) unsigned DEFAULT NULL COMMENT 'The group this interface belongs to',
  `creationDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Time of allocation',
  `modifiedDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT 'The last time of modification',
  `published` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Boolean flag indicating if the interface is working or published',
  PRIMARY KEY (`ifacePk`),
  UNIQUE KEY `interface_iid_Uk` (`iid`(191),`version`),
  KEY `interface_repotype_Idx` (`repotype`),
  KEY `interface_submitterFk-Idx` (`submitterFk`),
  KEY `interface_groupFk_Idx` (`groupFk`),
  CONSTRAINT `interface_groupFk_Cst` FOREIGN KEY (`groupFk`) REFERENCES `submitter_new` (`submitterPk`),
  CONSTRAINT `interface_submitterFk_Cst` FOREIGN KEY (`submitterFk`) REFERENCES `submitter_new` (`submitterPk`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The interface xml containment table';

INSERT INTO interface_new (ifacePk, iid, xml, version, repotype, submitterFk, creationDate, modifiedDate, published) 
select interface.ifacePk, interface.iid, interface.xml, interface.version, interface.repotype, interface.submitterFk, interface.creationDate, interface.modifiedDate, interface.published
FROM interface;

DROP TABLE IF EXISTS `submitter_new`;
CREATE TABLE `submitter_new` (
  `submitterPk` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'The submitters primary auto incrementing key',
  `name` text(320) CHARACTER SET utf8mb4 NOT NULL COMMENT 'The name of the submitter',
  `email` text(1016) CHARACTER SET utf8mb4 NOT NULL COMMENT 'submitters email ',
  `description` text(2048) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'Brief information about the submitter',
  `joinedDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Time and date the submitter joined',
  `isGroup` tinyint(1) NOT NULL COMMENT 'Does this submitter represent a group',
  PRIMARY KEY (`submitterPk`),
  UNIQUE KEY `submitter_email_Uk` (`email`(191)),
  KEY `submitter_name_Idx` (`name`(191))
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The owner table';

INSERT INTO submitter_new (submitterPk, name, email,description, joinedDate, isGroup) 
select submitter.submitterPk, submitter.name, submitter.email, submitter.description, submitter.joinedDate, submitter.isGroup 
FROM submitter;

DROP TABLE IF EXISTS `submittergroup_new`;
CREATE TABLE `submittergroup_new` (
  `groupFk` bigint(10) unsigned NOT NULL COMMENT 'submitter.submitterPk representing this group',
  `managerFk` bigint(10) unsigned NOT NULL COMMENT 'submitter.submitterPk who is the manager of  the group',
  `memberFk` bigint(10) unsigned NOT NULL COMMENT 'submitter.submitterPk of the group members',
  KEY `submittergroup_groupFk_Idx` (`groupFk`),
  KEY `submittergroup_managerFk_Idx` (`managerFk`),
  KEY `submittergroup_memberFk_Idx` (`memberFk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The group table';

INSERT INTO submittergroup_new (groupFk, managerFk, memberFk) 
select submittergroup.groupFk, submittergroup.managerFk, submittergroup.memberFk 
FROM submittergroup;

DROP TABLE IF EXISTS `subrepo`;
CREATE TABLE `subrepo` (
  `subrepoPk` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Nodes primary key',
  `repotype` text(512) NOT NULL COMMENT 'the repository type (i.e. opendof | allseen)',
  `name` text(320) NOT NULL  COMMENT 'the nodes name',
  `label` text(320) NOT NULL  COMMENT 'the nodes label',
  `depth` int(11) NOT NULL  COMMENT 'the depth of the node from root',
  `parentPid` int(11) DEFAULT NULL  COMMENT 'the parent nodes primary id',
  `groupFk` bigint(20) DEFAULT NULL COMMENT 'The group this node is controlled by',
  PRIMARY KEY (`subrepoPk`),
  UNIQUE KEY `subrepo_rowUni` (`repotype`(191),`parentPid`,`depth`,`name`(191),`label`(191)),
  KEY `subrepo_label` (`label`(191)),
  KEY `subrepo_parentPid_Idx` (`parentPid`),
  KEY `subrepo_groupFk_Idx` (`groupFk`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sub-Repository tree node';

DROP TABLE IF EXISTS `subrepoparent`;
CREATE TABLE `subrepoparent` (
  `parentFk` int(11) DEFAULT NULL COMMENT 'parent subrepo.subrepoPk of the child nodes',
  `childFk` int(11) NOT NULL COMMENT 'subrepo.subrepoPk child nodes',
  `depth` int(11) NOT NULL COMMENT 'the depth of the child from the root node',
  KEY `subrepoparent_parentFk_Idx` (`parentFk`),
  KEY `subrepoparent_childFk_Idx` (`childFk`),
  CONSTRAINT `subrepo_childFk_Cst` FOREIGN KEY (`childFk`) REFERENCES `subrepo` (`subrepoPk`),
  CONSTRAINT `subrepo_sparentFk_Cst` FOREIGN KEY (`parentFk`) REFERENCES `subrepo` (`subrepoPk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sub-Repository tree parent table';

drop table submittergroup;
rename table submittergroup_new to submittergroup;
drop table submitter;
rename table submitter_new to submitter;
drop table interface;
rename table interface_new to interface;

insert into submitter (name, email, description, joinedDate, isGroup) values ('cli-admin', 'cli-admin', 'Root user CLI Privledges', now(), false);

COMMIT;

SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;
SET AUTOCOMMIT = 1;
