import InquestRepository.InquestNotFound

import scala.concurrent.{ExecutionContext, Future}

trait InquestRepository {

  def all(): Future[Seq[Inquest]]
  def byId(id: String): Future[Option[Inquest]]
  def create(createInquest: CreateInquest): Future[Inquest]
  def update(id: String, updateInquest: UpdateInquest): Future[Inquest]

}

object InquestRepository {

  final case class InquestNotFound(id: String) extends Exception(s"Inquest with id $id not found.")

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

  override def update(id: String, updateInquest: UpdateInquest): Future[Inquest] = {
    val i = inquests.indexWhere(_.id == id)

    if (i == -1)
      Future.failed(InquestNotFound(id))
    else {
      val foundInquest = inquests(i)
      val newInquest = updateHelper(foundInquest, updateInquest)
      inquests = inquests.updated(i, newInquest)
      Future.successful(newInquest)
    }
  }

  private def updateHelper(inquest: Inquest, updateInquest: UpdateInquest): Inquest = {
    inquest.copy(
      title = updateInquest.title.getOrElse(inquest.title),
      description = updateInquest.description.getOrElse(inquest.description)
    )
  }

}