package com.passulo

import kantan.csv.CsvConfiguration
import kantan.csv.generic.*
import kantan.csv.ops.*

import java.io.File

case class ResultListEntry(passId: String, csvId: String, memberNumber: String, firstName: String, lastName: String)
object ResultListEntry {
  def from(passId: String, passInfo: PassInfo): ResultListEntry =
    apply(passId = passId, csvId = passInfo.id, memberNumber = passInfo.number, firstName = passInfo.firstName, lastName = passInfo.lastName)

  def write(results: Seq[ResultListEntry], fileName: String): Unit = {
    val config = CsvConfiguration.rfc.withHeader("Pass ID", "ID", "Member Number", "First Name", "Last Name")
    new File(fileName).writeCsv(results, config)
  }
}
