package db.models

case class Jurisdiction(
  id: String,
  name: String,
  isFederal: Boolean // TODO: Consider changing to jurisdiction type attribute.
)
