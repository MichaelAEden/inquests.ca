package db.models

import db.models.Action._

case class Role(title: String, actions: Seq[Action])

object Role {

  val RoleUser: Role = Role("user", Seq.empty)

  val RoleEditor: Role = Role("editor", Seq(
    ActionEditAuthority
  ))

  val RoleAdmin: Role = Role("admin", Seq(
    ActionEditAuthority,
    ActionManageUsers
  ))

  def getRole(title: String): Role = {
    Seq(RoleUser, RoleEditor, RoleAdmin)
      .find(_.title == title)
      .getOrElse(throw new Exception(s"No such role: $title."))
  }

}

case class Action(action: String)

object Action {

  val ActionEditAuthority = Action("editAuthority")
  val ActionManageUsers = Action("manageUsers")

}
