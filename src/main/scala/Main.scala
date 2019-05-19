import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App {

	val host = "0.0.0.0"
	val port = 9000

	implicit val system: ActorSystem = ActorSystem(name = "inquests-ca")
	implicit val materializer: ActorMaterializer = ActorMaterializer()
	import system.dispatcher

	def route = path("hello") {
		get {
			complete("Hello, world!")
		}
	}

	val binding = Http().bindAndHandle(route, host, port)

	binding.onComplete {
		case Success(_) => println("Success!")
		case Failure(error) => println(s"Failed: ${error.getMessage}")
	}

	Await.result(binding, 10.seconds)
}