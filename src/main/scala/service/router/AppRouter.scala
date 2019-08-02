package service.router

import akka.http.scaladsl.server.{Directives, Route}

import clients.firebase.FirebaseClient
import db.spec.AuthorityRepository

class AppRouter(authorityRepository: AuthorityRepository, fbClient: FirebaseClient)
  extends Router with Directives {

  private val staticResourceRouter = StaticResourceRouter()
  private val authorityRouter = new AuthorityRouter(authorityRepository, fbClient)

  // TODO: implement custom exception, rejection handlers.
  override def route: Route = Route.seal {
    staticResourceRouter.route ~ authorityRouter.route
  }

}
