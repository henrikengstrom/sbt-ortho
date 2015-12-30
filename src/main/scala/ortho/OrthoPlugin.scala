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

  final val ScalaFile = "scala"
  final val JavaFile = "java"
  final val TextFile = "txt"
  final val MarkdownFile = "md"
  final val UnknownFile = "unknown"

  final val TargetFileName = "target"
  final val DotCharacter = "."

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
    val styleReport = traverseFiles(directory = baseDirectory.value, rewriteFiles = false)(StyleAligner.convert)
    log.info("")
    log.info("ORTHO STYLE ALIGNMENT REPORT FOR PROJECT FILES")
    if (styleReport.isEmpty)
      log.info("Found no deviations.\n\n")
    else {
      styleReport foreach { log.info(_) }
    }
    log.info("")

    SpellChecker.initialize(trainingFile.value)
    val spellingReport = traverseFiles(directory = baseDirectory.value, rewriteFiles = false)(SpellChecker.convert)
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
    val x = traverseFiles(directory = baseDirectory.value, rewriteFiles = true)(StyleAligner.convert)
  }

  def spellCheckTask() = Def.task {
    SpellChecker.initialize(trainingFile.value)
    val x = traverseFiles(directory = baseDirectory.value, rewriteFiles = true)(SpellChecker.convert)
  }

  private def traverseFiles(directory: File, rewriteFiles: Boolean)(func: (String, String, Int) => LineResult): Seq[String] = {

    def getAllFiles(file: File, files: Seq[FileInfo]): Seq[FileInfo] = file match {
      case f if f.isDirectory =>
        if (f.getName.contains(DotCharacter) || f.getName.contains(TargetFileName)) files
        else f.listFiles flatMap { x => getAllFiles(x, files) }
      case f =>
        val fileInfo =
          f.getName.substring(f.getName.lastIndexOf(DotCharacter) + 1) match {
            case `ScalaFile`    => FileInfo(f, ScalaFile)
            case `JavaFile`     => FileInfo(f, JavaFile)
            case `TextFile`     => FileInfo(f, TextFile)
            case `MarkdownFile` => FileInfo(f, MarkdownFile)
            case _              => FileInfo(f, UnknownFile)
          }

        // Only add recognized files to result
        if (fileInfo.fileType != UnknownFile)
          files :+ fileInfo
        else
          files
    }

    def traverseFile(file: File, fileType: String): Seq[LineResult] = {
      var lineNumber = 0
      IO.readLines(file) map { line =>
        lineNumber += 1
        func(line, fileType, lineNumber)
      }
    }

    val filesInfo = getAllFiles(directory, Seq.empty[FileInfo])
    var reports = Seq.empty[String]

    filesInfo foreach { fi =>
      val lines: Seq[LineResult] = traverseFile(fi.file, fi.fileType)
      if (rewriteFiles) {
        IO.writeLines(fi.file, lines map { line => line.updated })
      } else {
        if (lines.nonEmpty) {
          var localReports = Seq.empty[String]
          lines filter { lr => lr.words.nonEmpty } foreach { lineReport =>
            localReports = localReports :+ lineReport.format
          }

          if (localReports.nonEmpty) {
            reports = reports :+ s"FILE: ${fi.file.getAbsolutePath}"
            reports = reports ++ localReports
          }
        }
      }
    }

    reports
  }

  /**
   * Remove any non-letters from the "words" and return the cleaned words together with its non-letter characters.
   * E.g. Passing in ("**BOLD*") will return ("BOLD", "**", "*")
   */
  def wash(s: String, left: String = "", right: String = ""): (String, String, String) = (s.head.isLetter, s.last.isLetter) match {
    case (true, true)  => (s, left, right) //right now we don't handle non-letter chars inside of words
    case (false, true) => wash(s.tail, left + s.head, right)
    case (true, false) => wash(s.substring(0, s.length - 1), left, right + s.last)
    case (false, false) =>
      val sMod = s.tail
      wash(sMod.substring(0, sMod.length - 1), left + s.head, right + s.last)
  }

  case class LineResult(original: String, updated: String, words: Seq[WordPair], lineNumber: Int) {
    def format: String = s"#$lineNumber : " + words.mkString(", ")
  }

  case class WordPair(original: String, updated: String) {
    override def toString: String = s"$original/$updated"
  }
}

