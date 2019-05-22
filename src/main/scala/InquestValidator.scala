object InquestValidator {

  def validate(createInquest: CreateInquest): Option[ApiError] = {
    if (createInquest.title.isEmpty) Some(ApiError.invalidInquestTitle)
    else None
  }

}
