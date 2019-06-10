package service.router

import akka.http.scaladsl.server.{Directives, Route}

import db.spec.InquestRepository

class AppRouter(inquestRepository: InquestRepository) extends Router with Directives {

  private val staticResourceRouter = StaticResourceRouter()
  private val inquestRouter = new InquestRouter(inquestRepository)

  override def route: Route = staticResourceRouter.route ~ pathPrefix("api") {
    inquestRouter.route
  }

}
