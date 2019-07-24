package db.spec

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import clients.firebase.FirebaseUser
import db.models.User
import db.slick.UserTable
import db.spec.UserRepository.UserNotFound
import service.models.{CreateUser, UpdateUser}

import scala.concurrent.{ExecutionContext, Future}

trait UserRepository {

  def byId(id: Int): Future[User]
  def byFirebaseUid(uid: String): Future[User]
  def byEmail(email: String): Future[User]
  def create(createUser: CreateUser, firebaseUser: FirebaseUser): Future[User]
  def update(id: Int, updateUser: UpdateUser): Future[User]

}

object UserRepository {

  final case class UserNotFound() extends Exception(s"User not found.")

}

class SlickUserRepository(databaseConfig: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
  extends UserRepository with Db with UserTable {

  override val config = databaseConfig

  import config.profile.api._

  // TODO: compile queries.
  override def byId(id: Int): Future[User] = {
    val q = users.filter(_.id === id).take(1)
    db.run(q.result).map(_.headOption.getOrElse(throw UserNotFound()))
  }

  override def byFirebaseUid(uid: String): Future[User] = {
    val q = users.filter(_.firebaseUid === uid).take(1)
    db.run(q.result).map(_.headOption.getOrElse(throw UserNotFound()))
  }

  override def byEmail(email: String): Future[User] = {
    val q = users.filter(_.email === email).take(1)
    db.run(q.result).map(_.headOption.getOrElse(throw UserNotFound()))
  }

  override def create(createUser: CreateUser, firebaseUser: FirebaseUser): Future[User] = {
    val user = createUser.toUser(firebaseUser)
    val q = (
      users returning users.map(_.id) into ((_, id) => user.copy(id = Some(id)))
      ) += user
    db.run(q)
  }

  override def update(id: Int, updateUser: UpdateUser): Future[User] = {
    for {
      user <- byId(id)
      newUser = updateUser.toUser(user)

      q = users
        .filter(_.id === id)
        .map(user => (user.name, user.jurisdictionId, user.role))
        .update((newUser.name, newUser.jurisdictionId, newUser.role))

      _ <- db.run(q)
    } yield newUser
  }

}
