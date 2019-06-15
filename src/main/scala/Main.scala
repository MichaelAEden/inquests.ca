import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import slick.jdbc.JdbcBackend.Database

import db.spec.SlickInquestRepository
import service.router.AppRouter
import service.Server
import utils.EnvReader

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// TODO: use scalafmt.
object Main extends App with EnvReader {

  val host = "0.0.0.0"
  val port = getEnvWithDefault("PORT", "9000").toInt

  implicit val system: ActorSystem = ActorSystem(name = "inquests-ca")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val db = Database.forConfig("slick.mysql.local")
  val repository = new SlickInquestRepository(db)
  val router = new AppRouter(repository)
  val server = new Server(router, host, port)

  val binding = server.bind()

  binding.onComplete {
    case Success(_) => println("Success!")
    case Failure(error) => println(s"Failed: ${error.getMessage}")
  }

  Await.result(binding, 10.seconds)
}
