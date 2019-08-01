package service.router

import akka.http.scaladsl.server.{Directives, Route}

import clients.firebase.FirebaseClient
import db.spec.{InquestRepository, UserRepository}

class AppRouter(
  inquestRepository: InquestRepository,
  userRepository: UserRepository,
  firebaseClient: FirebaseClient
)
  extends Router with Directives {

  private val staticResourceRouter = StaticResourceRouter()
  private val inquestRouter = new InquestRouter(inquestRepository, userRepository, firebaseClient)
  private val userRouter = new UserRouter(userRepository, firebaseClient)

  // TODO: implement custom exception, rejection handlers.
  override def route: Route = Route.seal {
    staticResourceRouter.route ~ inquestRouter.route ~ userRouter.route
  }

}
