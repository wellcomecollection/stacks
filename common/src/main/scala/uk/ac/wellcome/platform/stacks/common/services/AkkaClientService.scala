package uk.ac.wellcome.platform.stacks.common.services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpMethods, HttpRequest, HttpResponse, RequestEntity, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}

trait AkkaClientService {
  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  protected val defaultBaseUri: Uri
  protected val maybeBaseUri: Option[Uri]
  protected val baseUri = maybeBaseUri.getOrElse(defaultBaseUri)
}

trait AkkaClientServiceGet extends AkkaClientService {
  protected def get[Out](path: String, params: String = "", headers: List[HttpHeader] = Nil)(
    implicit um: Unmarshaller[HttpResponse, Out]
  ): Future[Out] = {

    val paramString = if(!params.isEmpty) s"?${params}" else ""
    val uri = s"${baseUri}/${path}${paramString}"

    for {
      response <- Http().singleRequest(
        HttpRequest(uri = uri, headers = headers)
      )
      t <- Unmarshal(response).to[Out]
    } yield t
  }
}

trait AkkaClientServicePost extends AkkaClientService {
  protected def post[In, Out](path: String, body: In, params: String = "", headers: List[HttpHeader] = Nil)(
    implicit
      um: Unmarshaller[HttpResponse, Out],
      m: Marshaller[In, RequestEntity]
  ): Future[Option[Out]] = {

    val paramString = if(!params.isEmpty) s"?${params}" else ""
    val uri = s"${baseUri}/${path}${paramString}"

    for {
      entity <- Marshal(body).to[RequestEntity]
      response <- Http().singleRequest(
        HttpRequest(HttpMethods.POST, uri = uri, headers = headers, entity = entity)
      )

      t <- response.entity match {
        case foo if foo.isKnownEmpty() => Future.successful(None)
        case _ => Unmarshal(response).to[Out].map(Some(_))
      }
    } yield t
  }
}

trait AkkaClientTokenExchange extends AkkaClientService {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  case class AccessToken(access_token: String)

  val tokenPath: String

  protected def getToken(credentials: BasicHttpCredentials) = {
    val authHeader = Authorization(
      credentials
    )

    val tokenRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = f"$baseUri/$tokenPath",
      headers = List(authHeader),
    )

    for {
      response <- Http().singleRequest(tokenRequest)
      token <- Unmarshal(response).to[AccessToken]
    } yield OAuth2BearerToken(token.access_token)
  }
}