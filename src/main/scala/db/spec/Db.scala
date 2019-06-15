package db.spec

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait Db {

  val db: JdbcProfile#Backend#Database
  val config: DatabaseConfig[JdbcProfile]

}
