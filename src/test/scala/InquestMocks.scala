import scala.concurrent.Future

trait InquestMocks {

	class FailingRepository extends InquestRepository {

		override def all(): Future[Seq[Inquest]] = Future.failed(new Exception("Something happened"))
		override def byId(id: String): Future[Option[Inquest]] = Future.failed(new Exception("Something happened"))

	}

}
