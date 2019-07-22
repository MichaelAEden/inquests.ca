package db.models

import db.models.Action._

case class Role(title: String, actions: Seq[Action])

object Role {

  val User: Role = Role("user", Seq.empty)

  val Editor: Role = Role("editor", Seq(
    EditAuthority
  ))

  val Admin: Role = Role("admin", Seq(
    EditAuthority,
    ManageUsers
  ))

  def getRole(title: String): Role = {
    Seq(User, Editor, Admin)
      .find(_.title == title)
      .getOrElse(throw new Exception(s"No such role: $title."))
  }

}

case class Action(action: String, realm: String)

object Action {

  val EditAuthority = Action("editAuthority", "access to edit authorities")
  val ManageUsers = Action("manageUsers", "access to manage users")

}
