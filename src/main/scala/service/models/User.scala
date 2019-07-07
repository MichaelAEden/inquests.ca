package service.models

import com.google.firebase.auth.{FirebaseAuth, UserRecord}

import scala.collection.JavaConverters._

// Wrapper around Firebase user which provides additional helper methods.
case class User(firebaseUid: String) {

  def isAdmin: Boolean = {
    getUserRecord
      .getCustomClaims
      .asScala
      .contains("admin")
  }

  // TODO: use async Firebase call.
  private def getUserRecord: UserRecord = {
    FirebaseAuth.getInstance.getUser(firebaseUid)
  }

}
