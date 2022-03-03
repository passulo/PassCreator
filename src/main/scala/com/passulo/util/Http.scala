package com.passulo.util

import io.circe.Json

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
object Http {

  val client: HttpClient = HttpClient.newHttpClient

  def get(url: String): HttpResponse[String] = {
    val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
    client.send(request, BodyHandlers.ofString())
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
