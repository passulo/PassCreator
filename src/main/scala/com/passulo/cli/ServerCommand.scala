package com.passulo.cli

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{`Content-Disposition`, ContentDispositionTypes}
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.passulo.*
import com.passulo.util.{CryptoHelper, NanoID}
import de.brendamour.jpasskit.signing.PKSigningInformation
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.generic.auto.*
import picocli.CommandLine.{Command, Option}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.io.File
import java.security.PrivateKey
import java.util.concurrent.Callable
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
@Command(
  name = "server",
  mixinStandardHelpOptions = true,
  description = Array("Starts the PassCreator in server mode, to continuously create passes.")
)
class ServerCommand extends Callable[Int] {

  @Option(names = Array("-p", "--port"), description = Array("The port the server uses."))
  var port: Int = 8080

  def call(): Int = {

    println("Loading configuration from 'passulo.conf'…")
    val config: Config = ConfigSource.resources("passulo.conf").loadOrThrow[Config]

    println("Loading private key…")
    val privateKey: PrivateKey = CryptoHelper.loadPKCS8EncodedPEM(new File(config.keys.privateKey))

    val signingInformation = loadSigningInfo(config.keys)

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      // Create routes
      val routes = path("create") {
        pathEndOrSingleSlash {
          post {
            entity(as[PassInfo]) { member =>
              val passId      = NanoID.create()
              val pass        = Passulo.createPass(member, passId, signingInformation, privateKey, config)
              val contentType = ContentType.parse("application/vnd.apple.pkpass").getOrElse(ContentTypes.`application/octet-stream`)
              respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"${member.filename}.pkpass"))) {
                complete(HttpEntity.Chunked.fromData(contentType, Source.single(ByteString(pass))))
              }
            }
          }
        }
      }

      // Start server
      startHttpServer(routes, port)(context.system)

      Behaviors.empty
    }

    println(StdOutText.success(s"Starting server..."))

    val _ = ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
    0
  }

  private def startHttpServer(routes: Route, port: Int)(implicit system: ActorSystem[?]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http()
      .newServerAt("0.0.0.0", port)
      .bind(routes)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println(StdOutText.success(s"Server online at http://${address.getHostString}:${address.getPort}/"))
      case Failure(ex) =>
        println(StdOutText.error("Failed to bind HTTP endpoint, terminating system"))
        println(ex)
        system.terminate()
    }
  }

  private def loadSigningInfo(keys: Keys): PKSigningInformation = {
    println("Loading Apple Developer certificate from keystore…")
    val (key, cert) = CryptoHelper.privateKeyAndCertFromKeystore(keys.keystore, keys.password).get

    println("Loading Apple WWDR CA…")
    val appleCert = CryptoHelper.certificateFromFile(keys.appleCaCert).get

    new PKSigningInformation(cert, key, appleCert)
  }
}
