package service.directives

import service.models.ApiError

trait Validator[T] {

  def validate(t: T): Option[ApiError]

}

