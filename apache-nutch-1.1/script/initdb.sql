SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `watchlist` DEFAULT CHARACTER SET latin1 ;
USE `watchlist` ;

-- -----------------------------------------------------
-- Table `watchlist`.`source`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `watchlist`.`source` (
  `id` BIGINT NOT NULL ,
  `name` VARCHAR(45) NULL ,
  `baseurl` VARCHAR(100) NULL ,
  `description` TEXT NULL ,
  `updatetime` DATETIME NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `name` (`name` ASC) ,
  INDEX `baseurl` (`baseurl` ASC) )
ENGINE = InnoDB
COMMENT = 'The Table for Brands';


-- -----------------------------------------------------
-- Table `watchlist`.`product`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `watchlist`.`product` (
  `id` BIGINT NOT NULL ,
  `source_id` BIGINT NOT NULL ,
  `external_id` BIGINT NOT NULL ,
  `primary_catogory` BIGINT NULL ,
  `secondary_catogory` BIGINT NULL ,
  `product_title` VARCHAR(150) NULL ,
  `imgURL` VARCHAR(250) NULL ,
  `latest_price` FLOAT UNSIGNED NULL ,
  `description` TEXT NULL ,
  `update_date` DATETIME NULL ,
  `meta_info` TEXT NULL ,
  `color` VARCHAR(50) NULL ,
  `size` VARCHAR(50) NULL ,
  INDEX `product_source` (`source_id` ASC) ,
  PRIMARY KEY (`id`) ,
  INDEX `product_title` (`product_title` ASC) ,
  INDEX `color` (`color` ASC) ,
  INDEX `external_id` (`external_id` ASC) ,
  CONSTRAINT `product_source`
    FOREIGN KEY (`source_id` )
    REFERENCES `watchlist`.`source` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `watchlist`.`record`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `watchlist`.`record` (
  `id` BIGINT NOT NULL ,
  `source_id` BIGINT NOT NULL ,
  `product_id` BIGINT NOT NULL ,
  `product_title` VARCHAR(150) NULL ,
  `price` FLOAT UNSIGNED NULL ,
  `update_date` DATETIME NULL ,
  PRIMARY KEY (`id`) ,
  INDEX `record_source` (`source_id` ASC) ,
  INDEX `product_title` (`product_title` ASC) ,
  INDEX `record_product` (`product_id` ASC) ,
  CONSTRAINT `record_source`
    FOREIGN KEY (`source_id` )
    REFERENCES `watchlist`.`source` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `record_product`
    FOREIGN KEY (`product_id` )
    REFERENCES `watchlist`.`product` (`id` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
COMMENT = 'The records';


-- -----------------------------------------------------
-- Table `watchlist`.`object`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `watchlist`.`object` (
  `id` BIGINT NOT NULL AUTO_INCREMENT ,
  `orig_table` VARCHAR(50) NULL ,
  PRIMARY KEY (`id`) )
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

