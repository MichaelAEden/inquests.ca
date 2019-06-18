package db.spec

import slick.lifted.ProvenShape
import slick.dbio.DBIOAction

import db.models.Inquest

import scala.concurrent.Future

// An Inquest table with 3 columns: id, title, description
trait InquestTable { this: Db =>

  import config.profile.api._

  class Inquests(tag: Tag) extends Table[Inquest](tag, "inquest") {

    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def title: Rep[String] = column[String]("title")
    def description: Rep[String] = column[String]("description")

    def * : ProvenShape[Inquest] = (id.?, title, description) <> (Inquest.tupled, Inquest.unapply)

  }

  val inquests = TableQuery[Inquests]

  // TODO: create Table trait with these methods, or move them somewhere more appropriate.
  // Note these functions would only be used for integration tests.
  def init(initialInquests: Seq[Inquest] = Seq.empty): Future[Unit] = {
    db.run(DBIOAction.seq(
      inquests.schema.create,
      inquests ++= initialInquests
    ))
  }
  def drop(): Future[Unit] = db.run(DBIOAction.seq(inquests.schema.drop))

}
