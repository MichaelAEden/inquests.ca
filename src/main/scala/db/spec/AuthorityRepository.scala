package db.spec

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import db.models.Authority
import db.slick.AuthorityTable
import db.spec.AuthorityRepository.AuthorityNotFound
import service.models.AuthorityCreateRequest

import scala.concurrent.{ExecutionContext, Future}

trait AuthorityRepository {

  def all(): Future[Seq[Authority]]
  def byId(id: Int): Future[Option[Authority]]
  def create(createAuthority: AuthorityCreateRequest): Future[Authority]
  def delete(id: Int): Future[Unit]

}

object AuthorityRepository {

  final case class AuthorityNotFound(id: Int) extends Exception(s"Authority with id $id not found.")

}

class SlickAuthorityRepository(databaseConfig: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
  extends AuthorityRepository with Db with AuthorityTable {

  override val config = databaseConfig

  import config.profile.api._

  override def all(): Future[Seq[Authority]] = db.run(authorities.result)

  override def byId(id: Int): Future[Option[Authority]] = {
    val q = authorities.filter(_.id === id).take(1)
    db.run(q.result).map(_.headOption)
  }

  override def create(createAuthority: AuthorityCreateRequest): Future[Authority] = {
    val authority = createAuthority.toAuthority
    val q = (
      authorities returning authorities.map(_.id) into ((_, id) => authority.copy(id = Some(id)))
    ) += authority
    db.run(q)
  }

  // TODO: return deleted authority.
  override def delete(id: Int): Future[Unit] = {
    val q = authorities.filter(_.id === id).take(1)
    db.run(q.delete).map { affectedRows =>
      if (affectedRows == 0) throw AuthorityNotFound(id)
      else ()
    }
  }

}
