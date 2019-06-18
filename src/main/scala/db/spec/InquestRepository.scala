package db.spec

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import db.models.{CreateInquest, Inquest, UpdateInquest}
import db.spec.InquestRepository.InquestNotFound

import scala.concurrent.{ExecutionContext, Future}

trait InquestRepository {

  def all(): Future[Seq[Inquest]]
  def byId(id: Int): Future[Option[Inquest]]
  def create(createInquest: CreateInquest): Future[Inquest]
  def update(id: Int, updateInquest: UpdateInquest): Future[Inquest]

}

object InquestRepository {

  final case class InquestNotFound(id: Int) extends Exception(s"Inquest with id $id not found.")

}

class SlickInquestRepository(databaseConfig: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
  extends InquestRepository with Db with InquestTable {

  override val db = databaseConfig.db
  override val config = databaseConfig

  import config.profile.api._

  override def all(): Future[Seq[Inquest]] = db.run(inquests.result)

  override def byId(id: Int): Future[Option[Inquest]] = {
    val q = inquests.filter(_.id === id).take(1)
    db.run(q.result).map(_.headOption)
  }

  override def create(createInquest: CreateInquest): Future[Inquest] = {
    val inquest = createInquest.toInquest()
    val q = (
      inquests returning inquests.map(_.id) into ((_, id) => inquest.copy(id = Some(id)))
    ) += inquest
    db.run(q)
  }

  override def update(id: Int, updateInquest: UpdateInquest): Future[Inquest] = {
    // TODO: reduce two db queries to one.
    for {
      result <- byId(id)
      inquest = result.getOrElse(throw InquestNotFound(id))
      newInquest = updateInquest.toInquest(inquest)

      q = inquests
        .filter(_.id === id)
        .map(inquest => (inquest.title, inquest.description))
        .update((newInquest.title, newInquest.description))

      // TODO: ensure db query ran successfully.
      _ <- db.run(q)
    } yield newInquest
  }

}
