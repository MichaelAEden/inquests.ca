package db.slick

import slick.lifted.ProvenShape

import db.models.Inquest
import db.spec.Db

trait InquestTable { this: Db =>

  import config.profile.api._

  class Inquests(tag: Tag) extends Table[Inquest](tag, "inquest") {

    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title")
    def description: Rep[String] = column[String]("description")

    def * : ProvenShape[Inquest] = (id.?, title, description) <> (Inquest.tupled, Inquest.unapply)

  }

  val inquests = TableQuery[Inquests]

}
