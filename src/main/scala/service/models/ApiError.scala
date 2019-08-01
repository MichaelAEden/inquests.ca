package service.models

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

final case class ApiError private (statusCode: StatusCode, message: String)

object ApiError {

  private def apply(statusCode: StatusCode, message: String): ApiError = new ApiError(statusCode, message)

  // 4xx
  val userNotFound: ApiError = new ApiError(StatusCodes.NotFound, s"User not found.")
  val userEmptyName: ApiError = new ApiError(StatusCodes.BadRequest, s"User name cannot be empty.")
  val userInvalidRole: ApiError = new ApiError(StatusCodes.BadRequest, s"Invalid user role.")

  val inquestNotFound: ApiError = new ApiError(StatusCodes.NotFound, s"Inquest not found.")
  val inquestEmptyTitle: ApiError = new ApiError(StatusCodes.BadRequest, s"Inquest title cannot be empty.")

  // 5xx
  val generic: ApiError = new ApiError(StatusCodes.InternalServerError, "Unknown error.")

}
