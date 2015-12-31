package ortho

import java.io.File

import sbt.Keys._
import sbt._
import sbt.AutoPlugin

object OrthoPlugin extends AutoPlugin {
  object OrthoKeys {
    lazy val orthoReport = taskKey[Unit]("Creates a spelling and dialect report")
    lazy val alignStyles = taskKey[Unit]("Streamlines English spelled differently into one style (default is from UK to US English)")
    lazy val spellCheck = taskKey[Unit]("Fixes any spelling mistakes in the project files")
    lazy val trainingFile = settingKey[String]("Training file for spell checker. Should contain 50000 words as a minimum. Default is US English training.")
    lazy val dictionaryFile = settingKey[String]("Dictionary file for English style checker. Default is UK to US.")
  }

  import OrthoKeys._

  type Dict = Map[String, String]

  final var dictionary: Dict = Map.empty[String, String]

  case class FileInfo(file: File, fileType: String)

  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    orthoReport <<= reportTask(),
    alignStyles <<= alignStylesTask(),
    spellCheck <<= spellCheckTask(),
    trainingFile := "/US-training.txt",
    dictionaryFile := "/UK-US.txt"
  )

  def reportTask() = Def.task {
    val log = streams.value.log

    StyleAligner.initialize(dictionaryFile.value)
    val styleReport = StyleAligner.traverseFiles(directory = baseDirectory.value, rewriteFiles = false)(StyleAligner.inspect)
    log.info("")
    log.info("ORTHO STYLE ALIGNMENT REPORT FOR PROJECT FILES")
    if (styleReport.isEmpty)
      log.info("Found no deviations.\n\n")
    else {
      styleReport foreach { log.info(_) }
    }
    log.info("")
    log.info("Now checking spelling in project which can be time consuming. Hold on...")
    log.info("")

    SpellChecker.initialize(trainingFile.value)
    val spellingReport = SpellChecker.traverseFiles(directory = baseDirectory.value, rewriteFiles = false)(SpellChecker.inspect)
    log.info("ORTHO SPELLING REPORT FOR PROJECT FILES")
    if (spellingReport.isEmpty)
      log.info("Found no incorrect spelling.\n\n")
    else {
      spellingReport foreach { log.info(_) }
    }
    log.info("")
  }

  def alignStylesTask() = Def.task {
    StyleAligner.initialize(dictionaryFile.value)
    val x = StyleAligner.traverseFiles(directory = baseDirectory.value, rewriteFiles = true)(StyleAligner.inspect)
  }

  def spellCheckTask() = Def.task {
    SpellChecker.initialize(trainingFile.value)
    val x = SpellChecker.traverseFiles(directory = baseDirectory.value, rewriteFiles = true)(SpellChecker.inspect)
  }
}

