package db.spec

import slick.jdbc.JdbcProfile

trait Db {

  val db: JdbcProfile#Backend#Database

}
