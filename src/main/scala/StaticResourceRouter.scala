import akka.http.scaladsl.server.{Directives, Route}

trait StaticResourceRouter {

  def getIndex: Route
  def getResource: Route

}

class LocalStaticResourceRouter extends StaticResourceRouter with Directives {

  // TODO: remove hardcoded paths
  private val buildPath = "/Users/michael/Documents/Projects/InquesterFrontend/build/"
  private val buildStaticPath = buildPath + "static"

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
