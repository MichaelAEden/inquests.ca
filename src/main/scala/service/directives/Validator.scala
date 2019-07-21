package service.directives

import service.models.{ApiError, CreateInquest, UpdateInquest}

trait Validator[T] {

  def validate(t: T): Option[ApiError]

}

