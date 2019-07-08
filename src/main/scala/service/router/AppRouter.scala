package service.router

import akka.http.scaladsl.server.{Directives, Route}

import db.spec.InquestRepository

import scala.concurrent.ExecutionContextExecutor

class AppRouter(inquestRepository: InquestRepository)(implicit ece: ExecutionContextExecutor)
  extends Router with Directives {

  private val staticResourceRouter = StaticResourceRouter()
  private val inquestRouter = new InquestRouter(inquestRepository)

  override def route: Route = staticResourceRouter.route ~ inquestRouter.route

}
