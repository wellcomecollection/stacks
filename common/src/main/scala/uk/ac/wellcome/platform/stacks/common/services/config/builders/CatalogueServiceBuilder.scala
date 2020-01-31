package uk.ac.wellcome.platform.stacks.common.services.config.builders

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import uk.ac.wellcome.platform.stacks.common.config.TypesafeBuilder
import uk.ac.wellcome.platform.stacks.common.services.CatalogueService
import uk.ac.wellcome.platform.stacks.common.services.config.models.CatalogueServiceConfig
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._


class CatalogueServiceBuilder()(
  implicit
    val system: ActorSystem,
    val mat: ActorMaterializer,
) extends TypesafeBuilder[CatalogueService, CatalogueServiceConfig]{

  def buildConfig(config: Config): CatalogueServiceConfig =
    CatalogueServiceConfig(config.get("catalogue.api.baseUrl"))

  def buildT(config: CatalogueServiceConfig): CatalogueService =
    new CatalogueService(config.baseUrl)
}
