import akka.http.scaladsl.server.Route

trait Router {

  def route: Route

}

class InquestRouter(inquestRepository: InquestRepository) extends Router with InquestDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  // TODO: remove hardcoded path.
  private val buildPath = "/Users/michael/Documents/Projects/InquesterFrontend/build/"
  private val buildStaticPath = buildPath + "static"

  // TODO: break up.
  override def route: Route = pathEndOrSingleSlash {
    getFromFile(buildPath + "index.html")
  } ~ pathPrefix("static") {
    get {
      getFromDirectory(buildStaticPath)
    }
  } ~ pathPrefix("inquests") {
    pathEndOrSingleSlash {
      get {
        handleWithGeneric(inquestRepository.all()) { inquests =>
          complete(inquests)
        }
      } ~ post {
        entity(as[CreateInquest]) { createInquest =>
          validateWith(CreateInquestValidator)(createInquest) {
            handleWithGeneric(inquestRepository.create(createInquest)) { inquest =>
              complete(inquest)
            }
          }
        }
      }
    } ~ path(Segment) { id: String =>
      put {
        entity(as[UpdateInquest]) { updateInquest =>
          validateWith(UpdateInquestValidator)(updateInquest) {
            handle(inquestRepository.update(id, updateInquest)) {
              case InquestRepository.InquestNotFound(_) =>
                ApiError.inquestNotFound(id)
              case _ =>
                ApiError.generic
            } { inquest =>
              complete(inquest)
            }
          }
        }
      }
    }
  }
}
