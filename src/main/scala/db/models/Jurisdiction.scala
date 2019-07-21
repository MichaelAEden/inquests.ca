package db.models

case class Jurisdiction(
  abbreviation: String,
  name: String,
  isFederal: Boolean // TODO: Consider changing to jurisdiction type attribute.
)
