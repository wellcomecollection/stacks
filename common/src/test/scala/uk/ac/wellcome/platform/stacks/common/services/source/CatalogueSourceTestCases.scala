package uk.ac.wellcome.platform.stacks.common.services.source

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.stacks.common.models.StacksWorkIdentifier
import uk.ac.wellcome.platform.stacks.common.services.source.CatalogueSource.{IdentifiersStub, ItemStub, TypeStub, WorkStub}

trait CatalogueSourceTestCases[CatalogueSourceImpl <: CatalogueSource] extends FunSpec with Matchers with ScalaFutures {
  def withCatalogueSource[R](testWith: TestWith[CatalogueSourceImpl, R]): R

  describe("behaves as a CatalogueSource") {
    it("gets an individual work") {
      withCatalogueSource { catalogueSource =>
        val future = catalogueSource.getWorkStub(id = StacksWorkIdentifier("ayzrznsz"))

        val expectedWork = WorkStub(
          id = "ayzrznsz",
          items = List(
            ItemStub(
              id = Some("q2knsrhh"),
              identifiers = Some(
                List(
                  IdentifiersStub(
                    identifierType = TypeStub(id = "sierra-system-number", label = "Sierra system number"),
                    value = "i10938370"
                  ),
                  IdentifiersStub(
                    identifierType = TypeStub(id = "sierra-identifier", label = "Sierra identifier"),
                    value = "1093837"
                  )
                )
              )
            )
          )
        )

        whenReady(future) { _ shouldBe expectedWork }
      }
    }

    it("handles a work without any items") {
      withCatalogueSource { catalogueSource =>
        val future = catalogueSource.getWorkStub(id = StacksWorkIdentifier("a2284uhb"))

        val expectedWork = WorkStub(id = "a2284uhb", items = List.empty)

        whenReady(future) { _ shouldBe expectedWork }
      }
    }

    it("handles a work where an item is not identified") {
      withCatalogueSource { catalogueSource =>
        val future = catalogueSource.getWorkStub(id = StacksWorkIdentifier("a227dajt"))

        val expectedWork = WorkStub(
          id = "a227dajt",
          items = List(
            ItemStub(id = None, identifiers = None)
          )
        )

        whenReady(future) { _ shouldBe expectedWork }
      }
    }
  }
}
