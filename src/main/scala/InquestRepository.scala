import scala.concurrent.{ExecutionContext, Future}

trait InquestRepository {

  def all(): Future[Seq[Inquest]]
  def byId(id: String): Future[Option[Inquest]]
  def create(createInquest: CreateInquest): Future[Inquest]

}

// TODO: use DB
class InMemoryInquestRepository(initialInquests: Seq[Inquest] = Seq.empty)
                               (implicit ec: ExecutionContext) extends InquestRepository {

  private var inquests = initialInquests.toVector

  override def all(): Future[Seq[Inquest]] = Future.successful(inquests)

  override def byId(id: String): Future[Option[Inquest]] = Future.successful(inquests.find(_.id == id))

  override def create(createInquest: CreateInquest): Future[Inquest] = {
    val inquest = Inquest(
      (inquests.length + 1).toString,
      createInquest.title,
      createInquest.description
    )
    inquests = inquests :+ inquest
    Future.successful(inquest)
  }

}