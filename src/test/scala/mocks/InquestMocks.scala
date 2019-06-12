package mocks

import db.models.{CreateInquest, Inquest, UpdateInquest}
import db.spec.InquestRepository

import scala.concurrent.Future

trait InquestMocks {

	class FailingRepository extends InquestRepository {

		override def all(): Future[Seq[Inquest]] = Future.failed(new Exception("BOOM!"))
		override def byId(id: Int): Future[Option[Inquest]] = Future.failed(new Exception("BOOM!"))
		override def create(createInquest: CreateInquest): Future[Inquest] = Future.failed(new Exception("BOOM!"))
		override def update(id: Int, updateInquest: UpdateInquest): Future[Inquest] = Future.failed(new Exception("BOOM!"))

	}

}
