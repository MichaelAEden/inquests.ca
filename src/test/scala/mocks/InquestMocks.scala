package mocks

import slick.jdbc.JdbcBackend.Database

import db.models.{CreateInquest, Inquest, UpdateInquest}
import db.spec.{InquestRepository, SlickInquestRepository}

import scala.concurrent.{ExecutionContext, Future}

// TODO: Mockito
trait InquestMocks {

	def testRepository(implicit ec: ExecutionContext): InquestRepository = {
		val db = Database.forConfig("slick.mysql.local")
		new SlickInquestRepository(db)
	}

	class FailingRepository extends InquestRepository {

		override def all(): Future[Seq[Inquest]] = Future.failed(new Exception("BOOM!"))
		override def byId(id: Int): Future[Option[Inquest]] = Future.failed(new Exception("BOOM!"))
		override def create(createInquest: CreateInquest): Future[Inquest] = Future.failed(new Exception("BOOM!"))
		override def update(id: Int, updateInquest: UpdateInquest): Future[Inquest] = Future.failed(new Exception("BOOM!"))

	}

}
