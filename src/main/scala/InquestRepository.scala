import scala.concurrent.{ExecutionContext, Future}

trait InquestRepository {

  def all(): Future[Seq[Inquest]]
  def byId(id: String): Future[Option[Inquest]]

}

// TODO: use DB
class InMemoryInquestRepository(initialInquests: Seq[Inquest] = Seq.empty)
                               (implicit ec: ExecutionContext) extends InquestRepository {

  private val inquests = initialInquests.toVector

  override def all(): Future[Seq[Inquest]] = Future.successful(inquests)
  override def byId(id: String): Future[Option[Inquest]] = Future.successful(inquests.find(_.id == id))

}