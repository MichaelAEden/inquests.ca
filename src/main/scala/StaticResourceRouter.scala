import akka.http.scaladsl.server.{Directives, Route}

trait StaticResourceRouter {

  def getIndex: Route
  def getResource: Route

}

class LocalStaticResourceRouter extends StaticResourceRouter with Directives {

  // TODO: define common method for reading environment variables.
  private lazy val buildPath = sys.env.getOrElse(
    "REACT_BUILD_PATH",
    throw new Exception("Missing environment variable: 'REACT_BUILD_PATH'")
  )
  private lazy val buildStaticPath = buildPath + "static"

  override def getIndex: Route = getFromFile(buildPath + "index.html")
  override def getResource: Route = getFromDirectory(buildStaticPath)

}

class S3StaticResourceRouter extends StaticResourceRouter with Directives {

  override def getIndex: Route = ??? // TODO
  override def getResource: Route = ??? // TODO

}

object StaticResourceRouter {

  def apply(): StaticResourceRouter = sys.env.get("ENV") match {
    case Some("dev") => new LocalStaticResourceRouter
    case Some("prod") => new S3StaticResourceRouter
    case _ => throw new Exception
  }

}
