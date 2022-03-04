package com.passulo.util

import io.circe.Json

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.Path
object Http {

  val client: HttpClient = HttpClient.newHttpClient

  def get(url: String): HttpResponse[String] = {
    val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
    client.send(request, BodyHandlers.ofString())
  }

  def download(url: String, path: Path): HttpResponse[Path] = {
    val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
    client.send(request, BodyHandlers.ofFile(path))
  }

  def post(url: String, body: Json): HttpResponse[String] = {
    val request = HttpRequest
      .newBuilder(URI.create(url))
      .header("Content-Type", "application/json")
      .POST(BodyPublishers.ofString(body.toString()))
      .build()
    client.send(request, BodyHandlers.ofString())
  }
}
