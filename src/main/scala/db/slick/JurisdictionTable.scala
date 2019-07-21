package db.slick

import db.spec.Db
import db.models.Jurisdiction

import slick.lifted.ProvenShape

trait JurisdictionTable { this: Db =>

  import config.profile.api._

  class Jurisdictions(tag: Tag) extends Table[Jurisdiction](tag, "jurisdiction") {

    def abbreviation: Rep[String] = column[String]("abbreviation", O.PrimaryKey)
    def name: Rep[String] = column[String]("name", O.Unique)

    def * : ProvenShape[Jurisdiction] = (
      abbreviation,
      name,
    ) <> (Jurisdiction.tupled, Jurisdiction.unapply)

  }

  val jurisdictions = TableQuery[Jurisdictions]

}
