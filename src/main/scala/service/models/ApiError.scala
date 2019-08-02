package service.models

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

final case class ApiError private (statusCode: StatusCode, message: String)

object ApiError {

  private def apply(statusCode: StatusCode, message: String): ApiError = new ApiError(statusCode, message)

  val generic: ApiError = new ApiError(StatusCodes.InternalServerError, "Unknown error.")

  def invalidAuthorityTitle(title: String): ApiError =
    new ApiError(StatusCodes.BadRequest, s"Invalid authority title: '$title'.")

  def authorityNotFound(id: Int): ApiError =
    new ApiError(StatusCodes.NotFound, s"Authority with id: '$id' not found.")

}
