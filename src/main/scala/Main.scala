import clients.firebase.FirebaseClient
import db.spec.{Db, SlickInquestRepository}
import service.router.AppRouter
import service.Server
import utils.Utils

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging

// TODO: use scalafmt.
object Main extends App with Utils with StrictLogging {

  val host = "0.0.0.0"
  val port = getEnvWithDefault("PORT", "9000").toInt

  implicit val system: ActorSystem = ActorSystem(name = "inquests-ca")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val firebaseClient: FirebaseClient = FirebaseClient(dispatcher)
  val databaseConfig = Db.getConfig
  val repository = new SlickInquestRepository(databaseConfig)
  val router = new AppRouter(repository, firebaseClient)
  val server = new Server(router, host, port)

  val binding = server.bind()

  binding.onComplete {
    case Success(_) => logger.info("Successfully initialized server!")
    case Failure(error) => logger.error(s"Failed to initialize server: ${error.getMessage}")
  }

  Await.result(binding, 10.seconds)
}
