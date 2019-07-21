package db.slick

import db.spec.Db
import db.models.User

import slick.lifted.ProvenShape

trait UserTable { this: Db =>

  import config.profile.api._

  class Users(tag: Tag) extends Table[User](tag, "user") {

    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def firebaseUid: Rep[String] = column[String]("firebase_uid", O.Unique)
    def name: Rep[String] = column[String]("name")
    def email: Rep[String] = column[String]("email", O.Unique)
    def jurisdiction: Rep[String] = column[String]("jurisdiction")
    def role: Rep[String] = column[String]("role")

    def * : ProvenShape[User] = (
      id.?,
      firebaseUid,
      name,
      email,
      jurisdiction,
      role
    ) <> (User.tupled, User.unapply)

  }

  val users = TableQuery[Users]

}
