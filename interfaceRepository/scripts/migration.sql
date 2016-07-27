CREATE DATABASE IF NOT EXISTS `interface_repository`

USE `interface_repository`;

DROP TABLE IF EXISTS `interface`;
CREATE TABLE `interface` (
  `ifacePk` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'interface primary autoincrementing key of interfaces',
  `iid` varchar(255) CHARACTER SET utf8 NOT NULL COMMENT 'The globally unique Interface ID',
  `xml` text CHARACTER SET utf8 COMMENT 'The interface source XML',
  `version` varchar(64) CHARACTER SET utf8 NOT NULL COMMENT 'interface version',
  `repotype` varchar(128) CHARACTER SET utf8 NOT NULL COMMENT 'The repository type (i.e. opendof, allseen)',
  `submitterFk` bigint(10) unsigned NOT NULL COMMENT 'The creators Fk',
  `groupFk` bigint(10) unsigned DEFAULT NULL,
  `creationDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Time of first entry',
  `modifiedDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT 'The last time the xml column was modified',
  `published` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Boolean flag indicating if the interface is working or published',
  PRIMARY KEY (`ifacePk`),
  UNIQUE KEY `iid_index` (`iid`,`version`),
  KEY `creator-fk` (`submitterFk`),
  KEY `interface_groupFk` (`groupFk`),
  CONSTRAINT `interface_groupFk` FOREIGN KEY (`groupFk`) REFERENCES `submitter` (`submitterPk`),
  CONSTRAINT `interface_submitterFk` FOREIGN KEY (`submitterFk`) REFERENCES `submitter` (`submitterPk`)
) ENGINE=InnoDB AUTO_INCREMENT=98 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='The interface xml containment table';

DROP TABLE IF EXISTS `registry`;
DROP TABLE IF EXISTS `reserved`;

DROP TABLE IF EXISTS `submitter`;
CREATE TABLE `submitter` (
  `submitterPk` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'The submitters primary auto incrementing key',
  `name` varchar(80) CHARACTER SET utf8 NOT NULL COMMENT 'The name of the submitter',
  `email` varchar(254) CHARACTER SET utf8 NOT NULL COMMENT 'submitters email ',
  `description` varchar(256) CHARACTER SET utf8 DEFAULT NULL COMMENT 'Brief information about the submitter',
  `joinedDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Time and date the submitter joined',
  `isGroup` tinyint(1) NOT NULL COMMENT 'Does this submitter represent a group',
  PRIMARY KEY (`submitterPk`),
  UNIQUE KEY `creator_index` (`name`),
  UNIQUE KEY `email_index` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='The owner table';

insert into `interface_repository`.`submitter`(`name`,`email`,`description`,`joinedDate`,`isGroup`) values ('OpenDOF Admin','admin@opendof.org','OpenDOF Manager, general inquiries only','2016-05-26 12:22:14',0);
insert into `interface_repository`.`submitter`(`name`,`email`,`description`,`joinedDate`,`isGroup`) values ('cli-admin','cli-admin','Root user CLI Privledges','2016-05-26 12:22:14',0);
insert into `interface_repository`.`submitter`(`name`,`email`,`description`,`joinedDate`,`isGroup`) values ('anonymous','anonymous','Anonymous users group','2016-05-26 12:22:14',1);
insert into `interface_repository`.`submitter`(`name`,`email`,`description`,`joinedDate`,`isGroup`) values ('user','user','Authenticated users group','2016-05-26 12:22:14',1);
insert into `interface_repository`.`submitter`(`name`,`email`,`description`,`joinedDate`,`isGroup`) values ('private','private','Private interface','2016-05-26 12:22:14',1);
insert into `interface_repository`.`submitter`(`name`,`email`,`description`,`joinedDate`,`isGroup`) values ('opendof-allocator','opendof-allocator','DOF IID 1 and 2 byte size allocation group','2016-05-26 12:22:14',1);

DROP TABLE IF EXISTS `submittergroup`;
CREATE TABLE `submittergroup` (
  `groupFk` bigint(10) unsigned NOT NULL COMMENT 'submitter.submitterPk representing this group',
  `managerFk` bigint(11) unsigned DEFAULT NULL COMMENT 'submitter.submitterPk who is the manager of  the group',
  `memberFk` bigint(10) unsigned NOT NULL COMMENT 'submitters.submitterPk of the group members',
  KEY `group-creator-fk` (`managerFk`),
  KEY `group-groupid-fk` (`groupFk`),
  KEY `members-fk` (`memberFk`),
  CONSTRAINT `groupGroupidFk` FOREIGN KEY (`groupFk`) REFERENCES `submitter` (`submitterPk`),
  CONSTRAINT `groupManagerFk` FOREIGN KEY (`managerFk`) REFERENCES `submitter` (`submitterPk`),
  CONSTRAINT `groupMemberFk` FOREIGN KEY (`memberFk`) REFERENCES `submitter` (`submitterPk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='The group creator table';

DROP TABLE IF EXISTS `subrepo`;
CREATE TABLE `subrepo` (
  `subrepoPk` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Nodes primary key',
  `repotype` varchar(128) NOT NULL,
  `name` varchar(64) CHARACTER SET utf8 NOT NULL,
  `label` varchar(64) CHARACTER SET utf8 NOT NULL,
  `depth` int(11) NOT NULL,
  `parentPid` int(11) DEFAULT NULL,
  `groupFk` bigint(20) DEFAULT NULL COMMENT 'The group this node is controlled by',
  PRIMARY KEY (`subrepoPk`),
  UNIQUE KEY `subrepo_rowUni` (`repotype`,`parentPid`,`depth`,`name`,`label`)
) ENGINE=InnoDB AUTO_INCREMENT=172 DEFAULT CHARSET=utf8 COMMENT='Sub-Repository tree node';

DROP TABLE IF EXISTS `subrepoparent`;
CREATE TABLE `subrepoparent` (
  `parentFk` int(11) DEFAULT NULL,
  `childFk` int(11) NOT NULL,
  `depth` int(11) NOT NULL,
  KEY `subrepoparent_parentFk` (`parentFk`),
  KEY `subrepoparent_childFk` (`childFk`),
  CONSTRAINT `subrepo_subrepoPk_child` FOREIGN KEY (`childFk`) REFERENCES `subrepo` (`subrepoPk`),
  CONSTRAINT `subrepo_subrepoPk_parent` FOREIGN KEY (`parentFk`) REFERENCES `subrepo` (`subrepoPk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Sub-Repository tree parent table';

DROP TABLE IF EXISTS `holes`;
CREATE TABLE `holes` (
  `holesPk` int(11) NOT NULL AUTO_INCREMENT,
  `subrepoFk` int(11) NOT NULL,
  `min` bigint(20) NOT NULL,
  `max` bigint(20) NOT NULL,
  PRIMARY KEY (`holesPk`),
  KEY `holes_subrepoFkIdx` (`subrepoFk`),
  CONSTRAINT `subrepo_subrepoPk_subrepoFk` FOREIGN KEY (`subrepoFk`) REFERENCES `subrepo` (`subrepoPk`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8 COMMENT='Sub-Repository tree node';
