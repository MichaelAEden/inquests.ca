package utils

import com.google.api.core.ApiFuture

import java.util.concurrent.Executor
import scala.concurrent.{Future, Promise}
import scala.util.Try

object FutureConverters {

  implicit class ApiFutureConverter[T](af: ApiFuture[T]) {

    def asScala(implicit e: Executor): Future[T] = {
      val p = Promise[T]()
      af.addListener(() => p.complete(Try(af.get())), e)
      p.future
    }

  }

}
