package com.passulo

import com.passulo.cli.{AppleCACommand, CertificateCommand, CreateCommand, KeypairCommand, RegisterCommand, SignCommand, TemplateCommand}
import picocli.AutoComplete.GenerateCompletion
import picocli.CommandLine
import picocli.CommandLine.Help.Ansi
import picocli.CommandLine.{Command, ITypeConverter}

@Command(
  name = "PassCreator",
  subcommands = Array(
    classOf[KeypairCommand],
    classOf[SignCommand],
    classOf[RegisterCommand],
    classOf[CertificateCommand],
    classOf[AppleCACommand],
    classOf[TemplateCommand],
    classOf[CreateCommand],
    classOf[GenerateCompletion]
  ),
  mixinStandardHelpOptions = true,
  version = Array(
    "@|bold,blue PassCreator v1|@",
    "@|underline https://github.com/passulo/PassCreator|@",
    "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
    "OS: ${os.name} ${os.version} ${os.arch}"
  ),
  description = Array("Creates Membership Passes for Passulo. Check out https://www.passulo.com")
)
class PassCreator {}

object PassCreator {
  def main(args: Array[String]): Unit = {
    new CommandLine(new PassCreator()).execute(args*)
    ()
  }
}

object StdOutText {
  def error(text: String): String    = Ansi.AUTO.string(s"@|bold,red $text|@")
  def success(text: String): String  = Ansi.AUTO.string(s"@|bold,green $text|@")
  def headline(text: String): String = Ansi.AUTO.string(s"@|bold,underline $text|@")
  def code(text: String): String     = Ansi.AUTO.string(s"@|cyan $text|@")
}

class OptionalParameter extends ITypeConverter[scala.Option[String]] {
  override def convert(value: String): scala.Option[String] = Option(value)
}
