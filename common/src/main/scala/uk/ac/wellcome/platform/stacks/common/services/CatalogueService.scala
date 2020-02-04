package uk.ac.wellcome.platform.stacks.common.services

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.stream.ActorMaterializer
import uk.ac.wellcome.platform.stacks.common.models.{StacksItem, _}

import scala.concurrent.Future

class CatalogueService(val baseUri: Uri = Uri(
  "https://api.wellcomecollection.org/catalogue/v2"
))(
  implicit
    val system: ActorSystem,
    val mat: ActorMaterializer
) extends AkkaClientService
    with AkkaClientServiceGet {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._
  import CatalogueService._

  protected def getIdentifier(
                               identifiers: List[IdentifiersStub]
                             ): Option[SierraItemIdentifier] = identifiers filter (
    _.identifierType.id == "sierra-identifier"
    ) match {
    case List(IdentifiersStub(_, value)) =>
      //TODO: This can fail!
      Some(SierraItemIdentifier(value.toLong))
    case _ => None
  }

  protected def getLocations(
                              locations: List[LocationStub]
                            ): List[StacksLocation] = locations collect {
    case location@LocationStub(_, _, "PhysicalLocation") =>
      StacksLocation(
        location.locationType.id,
        location.locationType.label
      )
  }

  protected def getStacksItems(
                                itemStubs: List[ItemStub]
                              ): List[StacksItemWithOutStatus] = itemStubs map {
    case ItemStub(id, identifiers, locations) =>
      (
        CatalogueItemIdentifier(id),
        getIdentifier(identifiers),
        getLocations(locations)
      )
  } map {
    case (catId, Some(sierraId), List(location)) =>
      StacksItemWithOutStatus(
        StacksItemIdentifier(catId, sierraId),
        location
      )
  }

  // See https://developers.wellcomecollection.org/catalogue/v2/works/getwork
  def getStacksWork(workId: StacksWorkIdentifier): Future[StacksWork[StacksItemWithOutStatus]] =
    for {
      workStub <- get[WorkStub](
        path = Path(s"works/${workId.value}"),
        params = Map(
          ("include","items,identifiers")
        )
      )

      items = getStacksItems(workStub.items)

    } yield StacksWork(workStub.id, items)

  // See https://developers.wellcomecollection.org/catalogue/v2/works/getworks
  def getStacksItem(identifier: Identifier[_]): Future[Option[StacksItem]] =
    for {
      searchStub <- get[SearchStub](
        path = Path("works"),
        params = Map(
          ("include", "items,identifiers"),
          ("query", identifier.value.toString)
        )
      )

      items = searchStub.results
        .map(_.items)
        .flatMap(getStacksItems)

    } yield items match {
      case List(item) => Some(item)
      case _ => None
    }
}

object CatalogueService {
  case class TypeStub(
                       id: String,
                       label: String
                     )

  case class LocationStub(
                           locationType: TypeStub,
                           label: Option[String],
                           `type`: String
                         )

  case class IdentifiersStub(
                              identifierType: TypeStub,
                              value: String
                            )

  case class ItemStub(
                       id: String,
                       identifiers: List[IdentifiersStub],
                       locations: List[LocationStub]
                     )

  case class WorkStub(
                       id: String,
                       items: List[ItemStub]
                     )

  case class SearchStub(
                         totalResults: Int,
                         results: List[WorkStub]
                       )
}