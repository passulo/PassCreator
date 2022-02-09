import de.brendamour.jpasskit.PKPass
import de.brendamour.jpasskit.signing.{PKFileBasedSigningUtil, PKPassTemplateFolder, PKSigningInformation}
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.file.{Files, Path}
object Passkit4S {

  def createSignedAndZippedPkPassArchive(pass: PKPass, templateFolder: String, pkSigningInformation: PKSigningInformation): Array[Byte] = {
    val passTemplate  = new PKPassTemplateFolder(templateFolder)
    val pkSigningUtil = new PKFileBasedSigningUtil
    pkSigningUtil.createSignedAndZippedPkPassArchive(pass, passTemplate, pkSigningInformation)
  }

  def createTempDirFromTemplate(templatePath: String): Option[Path] =
    getFolder(templatePath).map { template =>
      val tempDir = Files.createTempDirectory(s"passkit-${System.currentTimeMillis}")
      FileUtils.copyDirectory(template, tempDir.toFile)
      tempDir
    }

  def getFolder(path: String): Option[File] = {
    val file = new File(getClass.getClassLoader.getResource(path).getPath)
    if (file.exists())
      Some(file)
    else
      None
  }
}
