import scala.util.{Failure, Success, Try}

trait EnvReader {

	def getEnv(name: String): String = {
		sys.env.getOrElse(name, throw new Exception(s"Could not read environment variable: '$name'."))
	}

	def getEnvWithDefault(name: String, default: => String): String = {
		sys.env.getOrElse(name, default)
	}

}
