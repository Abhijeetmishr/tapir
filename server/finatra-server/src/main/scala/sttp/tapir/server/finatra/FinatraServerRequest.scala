package sttp.tapir.server.finatra

import java.net.URI

import com.twitter.finagle.http.Request
import sttp.model.Method
import sttp.tapir.model.{ConnectionInfo, ServerRequest}

class FinatraServerRequest(request: Request) extends ServerRequest {
  override def method: Method = Method(request.method.toString)
  override def protocol: String = request.version.toString
  override def uri: URI = new URI(request.uri)
  override def connectionInfo: ConnectionInfo = ConnectionInfo(None, Some(request.remoteSocketAddress), None)
  override def headers: Seq[(String, String)] = request.headerMap.toList
  override def header(name: String): Option[String] = request.headerMap.get(name)
  override def underlying: Any = request
}
