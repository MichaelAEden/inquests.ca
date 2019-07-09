package service.router

import akka.http.scaladsl.server.{Directives, Route}

import clients.firebase.FirebaseClient
import db.spec.InquestRepository

class AppRouter(inquestRepository: InquestRepository, fbClient: FirebaseClient)
  extends Router with Directives {

  private val staticResourceRouter = StaticResourceRouter()
  private val inquestRouter = new InquestRouter(inquestRepository, fbClient)

  override def route: Route = Route.seal {
    staticResourceRouter.route ~ inquestRouter.route
  }

}
