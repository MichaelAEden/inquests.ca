import akka.http.scaladsl.server.{Directives, Route}

trait StaticResourceRouter {

  def getIndex: Route
  def getResource: Route

}

class LocalStaticResourceRouter extends StaticResourceRouter with Directives with EnvReader {

  private lazy val buildPath = getEnv("REACT_BUILD_PATH")
  private lazy val buildStaticPath = buildPath + "static"

  // TODO: error handling.
  override def getIndex: Route = getFromFile(buildPath + "index.html")
  override def getResource: Route = getFromDirectory(buildStaticPath)

}

class S3StaticResourceRouter extends StaticResourceRouter with Directives {

  override def getIndex: Route = ??? // TODO
  override def getResource: Route = ??? // TODO

}

object StaticResourceRouter extends EnvReader {

  def apply(): StaticResourceRouter = getEnvWithDefault("ENV", "dev") match {
    case "dev" => new LocalStaticResourceRouter
    case "prod" => new S3StaticResourceRouter
    case env => throw new Exception("Invalid environment: '$env'.")
  }

}
