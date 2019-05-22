trait Validator[T] {

  def validate(t: T): Option[ApiError]

}

object CreateInquestValidator extends Validator[CreateInquest] {

  def validate(createInquest: CreateInquest): Option[ApiError] = {
    if (createInquest.title.isEmpty) Some(ApiError.invalidInquestTitle)
    else None
  }

}
