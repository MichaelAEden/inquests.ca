import akka.http.scaladsl.server.{Directives, Route}

trait Router {

  def route: Route

}

class InquestRouter(inquestRepository: InquestRepository) extends Router with Directives {

  private val staticResourceRouter = StaticResourceRouter()
  private val apiRouter = new ApiRouter(inquestRepository)

  override def route: Route = staticResourceRouter.route ~ apiRouter.route

}
