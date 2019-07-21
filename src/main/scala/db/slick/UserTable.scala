package db.slick

import db.spec.Db
import db.models.User

import slick.lifted.ProvenShape

trait UserTable { this: Db =>

  import config.profile.api._

  class Users(tag: Tag) extends Table[User](tag, "user") {

    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def firebaseUid: Rep[String] = column[String]("firebase_uid", O.Unique)

    def * : ProvenShape[User] = (id.?, firebaseUid) <> (User.tupled, User.unapply)

  }

  val users = TableQuery[Users]

}