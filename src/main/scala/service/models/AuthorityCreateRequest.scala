package service.models

import db.models.Authority

case class AuthorityCreateRequest(title: String, description: String) {

  def toAuthority: Authority =
    Authority(None, title, description)

}

object AuthorityCreateRequestValidator extends Validator[AuthorityCreateRequest] {

  def validate(createAuthority: AuthorityCreateRequest): Option[ApiError] = {
    if (createAuthority.title.isEmpty)
      Some(ApiError.invalidAuthorityTitle(""))
    else
      None
  }

}
