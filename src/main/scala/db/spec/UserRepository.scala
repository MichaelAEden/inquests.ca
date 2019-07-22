package db.spec

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import clients.firebase.FirebaseUser
import db.models.User
import db.slick.UserTable
import db.spec.UserRepository.UserNotFound
import service.models.CreateUser

import scala.concurrent.{ExecutionContext, Future}

trait UserRepository {

  def byEmail(email: String): Future[User]
  def create(createUser: CreateUser, firebaseUser: FirebaseUser): Future[User]

}

object UserRepository {

  final case class UserNotFound(email: String) extends Exception(s"User with email $email not found.")

}

class SlickUserRepository(databaseConfig: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
  extends UserRepository with Db with UserTable {

  override val config = databaseConfig

  import config.profile.api._

  // TODO: compile queries.
  override def byEmail(email: String): Future[User] = {
    val q = users.filter(_.email === email).take(1)
    db.run(q.result).map(_.headOption.getOrElse(throw UserNotFound(email)))
  }

  override def create(createUser: CreateUser, firebaseUser: FirebaseUser): Future[User] = {
    val user = createUser.toUser(firebaseUser)
    val q = (
      users returning users.map(_.id) into ((_, id) => user.copy(id = Some(id)))
      ) += user
    db.run(q)
  }

}
