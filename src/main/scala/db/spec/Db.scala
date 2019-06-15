package db.spec

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import utils.Utils

trait Db {

  val db: JdbcProfile#Backend#Database
  val config: DatabaseConfig[JdbcProfile]

}

object Db extends Utils {

  def getConfig: DatabaseConfig[JdbcProfile] = {
    val configPath = getEnvWithDefault("ENV", "dev") match {
      case "dev" => "slick.mysql.local"
      case "prod" => "slick.mysql.prod"
      case env => throw new Exception(s"Invalid environment: '$env'.")
    }

    DatabaseConfig.forConfig(configPath)
  }

}
