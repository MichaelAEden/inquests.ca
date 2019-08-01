package service.models

trait Validator[T] {

  def validate(t: T): Option[ApiError]

}
