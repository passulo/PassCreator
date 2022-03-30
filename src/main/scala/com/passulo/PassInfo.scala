package com.passulo

import cats.implicits.{catsStdInstancesForEither, toTraverseOps}
import com.passulo.PassInfo.NoneIfEmpty
import kantan.csv.generic.*
import kantan.csv.java8.*
import kantan.csv.ops.toCsvInputOps
import kantan.csv.{rfc, ReadResult}

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate

case class PassInfo(
    id: String,
    firstName: String,
    middleName: String,
    lastName: String,
    gender: String,
    number: String,
    status: String,
    role: String,
    company: String,
    email: String,
    telephone: String,
    validUntil: Option[LocalDate],
    memberSince: Option[LocalDate],
    template: String
) {

  def templateFolder = if (template.isBlank) "default" else template

  def fullName: String =
    (pronoun, NoneIfEmpty(firstName), NoneIfEmpty(middleName), NoneIfEmpty(lastName)) match {
      case (_, Some(f), Some(m), Some(l)) => s"$f $m $l"
      case (_, Some(f), None, Some(l))    => s"$f $l"
      case (Some(p), None, None, Some(l)) => s"$p $l"
      case (None, None, None, Some(l))    => s"$l"
      case _                              => "<no name>"
    }

  def pronoun: Option[String] =
    gender match {
      case "m" => Some("Mr.")
      case "f" => Some("Ms.")
      case "d" => Some("Mx.")
      case _   => None
    }

  def filename: String = java.net.URLEncoder.encode(s"$number-$fullName", StandardCharsets.UTF_8)
}

object PassInfo {
  def NoneIfEmpty(string: String): Option[String] = if (string.isBlank) None else Some(string)

  def readFromCsv(file: File): ReadResult[Seq[PassInfo]] =
    file.asCsvReader[PassInfo](rfc.withHeader).toSeq.sequence

}
