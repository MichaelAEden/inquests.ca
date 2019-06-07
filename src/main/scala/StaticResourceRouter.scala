import akka.http.scaladsl.server.{Directives, Route}

// This router is only used in the dev environment. In production, requests for static resources are
// handled by Nginx.
class DevStaticResourceRouter extends Router with Directives with EnvReader {

  private lazy val buildPath = getEnv("REACT_BUILD_PATH")
  private lazy val buildStaticPath = buildPath + "static"

  // TODO: error handling.
  override def route: Route = pathEndOrSingleSlash {
    getFromFile(buildPath + "index.html")
  } ~ pathPrefix("static") {
    getFromDirectory(buildStaticPath)
  }

}

class ProdStaticResourceRouter extends Router with Directives {

  override def route: Route = reject

}

object StaticResourceRouter extends EnvReader {

  def apply(): Router = getEnvWithDefault("ENV", "dev") match {
    case "dev" => new DevStaticResourceRouter
    case "prod" => new ProdStaticResourceRouter
    case env => throw new Exception("Invalid environment: '$env'.")
  }

}
