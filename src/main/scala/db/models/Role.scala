package db.models

import db.models.Action._

case class Role(name: String, actions: Seq[Action])

object Role {

  val User: String = "user"
  val Editor: String = "editor"
  val Admin: String = "admin"

  private val roles = Seq[Role](
    Role(User, Seq.empty),
    Role(Editor, Seq(EditAuthority)),
    Role(Admin, Seq(EditAuthority, ManageUsers))
  )

  def isRoleValid(role: String): Boolean =
    roles.exists(_.name == role)

  def canRolePerformAction(role: String, action: Action): Boolean =
    roles
      .find(_.name == role)
      .getOrElse(throw new Exception(s"No such role $role."))
      .actions
      .contains(action)
}

case class Action(action: String, realm: String)

object Action {

  val EditAuthority = Action("editAuthority", "access to edit authorities")
  val ManageUsers = Action("manageUsers", "access to manage users")

}
