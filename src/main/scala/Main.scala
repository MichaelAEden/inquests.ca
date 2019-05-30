import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App {

	val host = "0.0.0.0"
	val port = sys.env.getOrElse("PORT", "9000").toInt

	implicit val system: ActorSystem = ActorSystem(name = "inquests-ca")
	implicit val materializer: ActorMaterializer = ActorMaterializer()
	import system.dispatcher

	val inquestRepository = new InMemoryInquestRepository(Seq(
		Inquest("1", "Queen vs CBC", "some inquest"),
		Inquest("2", "Superman vs Batman", "some inquest"),
	))
	val router = new InquestRouter(inquestRepository)
	val server = new Server(router, host, port)

	val binding = server.bind()

	binding.onComplete {
		case Success(_) => println("Success!")
		case Failure(error) => println(s"Failed: ${error.getMessage}")
	}

	Await.result(binding, 10.seconds)
}
