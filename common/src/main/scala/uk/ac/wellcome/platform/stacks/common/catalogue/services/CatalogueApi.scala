package uk.ac.wellcome.platform.stacks.common.catalogue.services

import io.circe.optics.JsonPath._
import io.circe.parser.parse
import scalaj.http.Http
import uk.ac.wellcome.json.JsonUtil._


object CatalogueApi {
  private case class IdentifierType(id: String, label: String, `type`: String)
  private case class Identifier(identifierType: IdentifierType, value: String, `type`: String)
  private case class Item(id: Option[String], identifiers: List[Identifier])

  val rootUrl = "https://api.wellcomecollection.org/catalogue/v2/works"

  def getItemNumber(id: String): Option[String] = {
    val jsonString = Http(s"$rootUrl?include=items,identifiers&query=$id")
      .header("Accept", "application/json")
      .asString
      .body

    (parse(jsonString) map { json =>
      val items =
        root.results(0).items.each.as[Item].getAll(json)

      val identifier = items.find(_.id.contains(id)) flatMap (item => item.identifiers.find(id => id.identifierType.id == "sierra-identifier" ))
      identifier map (_.value)
    } toOption).flatten
  }
}
