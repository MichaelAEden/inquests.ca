package db.slick

import slick.lifted.ProvenShape

import db.models.Authority
import db.spec.Db

trait AuthorityTable { this: Db =>

  import config.profile.api._

  class Authorities(tag: Tag) extends Table[Authority](tag, "authority") {

    def id: Rep[Int] = column[Int]("authority_id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title")
    def description: Rep[String] = column[String]("description")

    def * : ProvenShape[Authority] = (id.?, title, description) <> (Authority.tupled, Authority.unapply)

  }

  val authorities = TableQuery[Authorities]

}
