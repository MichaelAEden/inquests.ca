package db.spec

import slick.lifted.{ProvenShape, Rep, Tag}
import slick.jdbc.MySQLProfile.api._

import db.models.Inquest

// An Inquest table with 3 columns: id, title, description
trait InquestTable { this: Db =>

  class Inquests(tag: Tag) extends Table[Inquest](tag, "inquest") {

    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title")
    def description: Rep[String] = column[String]("description")

    def * : ProvenShape[Inquest] = (id.?, title, description) <> (Inquest.tupled, Inquest.unapply)

  }

  val inquests = TableQuery[Inquests]

}
