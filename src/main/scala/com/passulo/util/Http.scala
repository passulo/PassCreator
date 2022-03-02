package com.passulo.util

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.concurrent.Future
import scala.jdk.FutureConverters.*
object Http {

  val client: HttpClient = HttpClient.newHttpClient

  def get(url: String): Future[HttpResponse[String]] = {
    val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
    client.sendAsync(request, BodyHandlers.ofString()).asScala
  }

  def post(url: String, body: String): Future[HttpResponse[String]] = {
    val request = HttpRequest.newBuilder(URI.create(url)).POST(BodyPublishers.ofString(body)).build()
    client.sendAsync(request, BodyHandlers.ofString()).asScala
  }
}
