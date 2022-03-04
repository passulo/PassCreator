package com.passulo.cli

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.passulo.*
import com.passulo.cli.CreateCommand.{loadSigningInfo, validatePublicKey}
import com.passulo.util.{CryptoHelper, FileOperations}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.generic.auto.*
import picocli.CommandLine.{Command, Option}
import pureconfig.ConfigSource
import pureconfig.generic.auto.*

import java.util.concurrent.Callable
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}
@Command(
  name = "server",
  mixinStandardHelpOptions = true,
  description = Array("Starts the PassCreator in server mode, to continuously create passes.")
)
class ServerCommand extends Callable[Int] {

  @Option(names = Array("-p", "--port"), description = Array("The port the server uses."))
  var port: Int = 8080

  @Option(names = Array("-n", "--dry-run"), description = Array("Don't actually register the passes at the server."))
  var dryRun: Boolean = false

  def call(): Int = {

    val rootBehavior = for {
      configFile         <- FileOperations.loadFile("passulo.conf")
      config             <- ConfigSource.file(configFile).load[Config].toOption.toRight(ConfigurationError("Failed to load configuration."))
      privateKeyFile     <- FileOperations.loadFile(config.keys.privateKey)
      privateKey         <- Try(CryptoHelper.loadPKCS8EncodedPEM(privateKeyFile)).toOption.toRight(KeyError("Failed to parse private key."))
      signingInformation <- loadSigningInfo(config.keys)
      publicKeyFile      <- FileOperations.loadFile(config.keys.publicKey)
      _                  <- validatePublicKey(config, publicKeyFile, dryRun)
    } yield Behaviors.setup[Nothing] { context =>
      val routes = path("create") {
        pathEndOrSingleSlash {
          post {
            entity(as[PassInfo]) { member =>
              val passId      = Passulo.createAndRegisterId(privateKey, config)
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

    rootBehavior match {
      case Left(value) => println(StdOutText.error(s"Error setting up: ${value.message}"))
      case Right(value) =>
        println(StdOutText.success(s"Starting server..."))
        ActorSystem[Nothing](value, "AkkaHttpServer")
    }

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
}
