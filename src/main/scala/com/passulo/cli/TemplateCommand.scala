package com.passulo.cli

import com.passulo.StdOutText
import picocli.CommandLine.Command

import java.util.concurrent.Callable

@Command(
  name = "template",
  mixinStandardHelpOptions = true,
  description = Array("Creates a template-folder.")
)
class TemplateCommand extends Callable[Int] {

  def call(): Int = {
    // TODO
    println(StdOutText.error("Not implemented yet."))
    0
  }
}
