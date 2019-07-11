package service.router

import akka.http.scaladsl.server.Route

trait Router {

  def route: Route
  def sealedRoute: Route = Route.seal(route)

}
