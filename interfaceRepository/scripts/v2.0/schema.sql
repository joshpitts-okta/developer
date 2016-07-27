CREATE DATABASE  IF NOT EXISTS `interface_repository` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `interface_repository`;
-- MySQL dump 10.13  Distrib 5.6.23, for Win64 (x86_64)
--
-- Host: localhost    Database: interface_repository
-- ------------------------------------------------------
-- Server version	5.6.25-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `holes`
--

DROP TABLE IF EXISTS `holes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `holes` (
  `holesPk` int(11) NOT NULL AUTO_INCREMENT,
  `subrepoFk` int(11) NOT NULL,
  `min` bigint(20) NOT NULL,
  `max` bigint(20) NOT NULL,
  PRIMARY KEY (`holesPk`),
  KEY `holes_subrepoFk_Idx` (`subrepoFk`),
  CONSTRAINT `holes_subrepoFk_Cst` FOREIGN KEY (`subrepoFk`) REFERENCES `subrepo` (`subrepoPk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sub-Repository tree node';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `interface`
--

DROP TABLE IF EXISTS `interface`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `interface` (
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
  CONSTRAINT `interface_groupFk_Cst` FOREIGN KEY (`groupFk`) REFERENCES `submitter` (`submitterPk`),
  CONSTRAINT `interface_submitterFk_Cst` FOREIGN KEY (`submitterFk`) REFERENCES `submitter` (`submitterPk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The interface xml containment table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submitter`
--

DROP TABLE IF EXISTS `submitter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `submitter` (
  `submitterPk` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'The submitters primary auto incrementing key',
  `name` text CHARACTER SET utf8mb4 NOT NULL COMMENT 'The name of the submitter',
  `email` text CHARACTER SET utf8mb4 NOT NULL COMMENT 'submitters email ',
  `description` text CHARACTER SET utf8mb4 COMMENT 'Brief information about the submitter',
  `joinedDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Time and date the submitter joined',
  `isGroup` tinyint(1) NOT NULL COMMENT 'Does this submitter represent a group',
  PRIMARY KEY (`submitterPk`),
  UNIQUE KEY `submitter_email_Uk` (`email`(191)),
  KEY `submitter_name_Idx` (`name`(191))
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The owner table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submittergroup`
--

DROP TABLE IF EXISTS `submittergroup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `submittergroup` (
  `groupFk` bigint(10) unsigned NOT NULL COMMENT 'submitter.submitterPk representing this group',
  `managerFk` bigint(10) unsigned NOT NULL COMMENT 'submitter.submitterPk who is the manager of  the group',
  `memberFk` bigint(10) unsigned NOT NULL COMMENT 'submitter.submitterPk of the group members',
  KEY `submittergroup_groupFk_Idx` (`groupFk`),
  KEY `submittergroup_managerFk_Idx` (`managerFk`),
  KEY `submittergroup_memberFk_Idx` (`memberFk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The group table';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subrepo`
--

DROP TABLE IF EXISTS `subrepo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subrepo` (
  `subrepoPk` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Nodes primary key',
  `repotype` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'the repository type (i.e. opendof | allseen)',
  `name` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'the nodes name',
  `label` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'the nodes label',
  `depth` int(11) NOT NULL COMMENT 'the depth of the node from root',
  `parentPid` int(11) DEFAULT NULL COMMENT 'the parent nodes primary id',
  `groupFk` bigint(20) DEFAULT NULL COMMENT 'The group this node is controlled by',
  PRIMARY KEY (`subrepoPk`),
  UNIQUE KEY `subrepo_rowUni` (`repotype`(191),`parentPid`,`depth`,`name`(191),`label`(191)),
  KEY `subrepo_label` (`label`(191)),
  KEY `subrepo_parentPid_Idx` (`parentPid`),
  KEY `subrepo_groupFk_Idx` (`groupFk`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sub-Repository tree node';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subrepoparent`
--

DROP TABLE IF EXISTS `subrepoparent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subrepoparent` (
  `parentFk` int(11) DEFAULT NULL COMMENT 'parent subrepo.subrepoPk of the child nodes',
  `childFk` int(11) NOT NULL COMMENT 'subrepo.subrepoPk child nodes',
  `depth` int(11) NOT NULL COMMENT 'the depth of the child from the root node',
  KEY `subrepoparent_parentFk_Idx` (`parentFk`),
  KEY `subrepoparent_childFk_Idx` (`childFk`),
  CONSTRAINT `subrepo_childFk_Cst` FOREIGN KEY (`childFk`) REFERENCES `subrepo` (`subrepoPk`),
  CONSTRAINT `subrepo_sparentFk_Cst` FOREIGN KEY (`parentFk`) REFERENCES `subrepo` (`subrepoPk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sub-Repository tree parent table';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-07-27 11:42:49
