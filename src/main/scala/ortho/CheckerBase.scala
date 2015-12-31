package ortho

import java.io.File

import ortho.OrthoPlugin.FileInfo
import sbt.IO

/**
 * Base trait for checker related functionality.
 * Contains common features for all checkers.
 */
trait CheckerBase {
  final val ScalaFile = "scala"
  final val JavaFile = "java"
  final val TextFile = "txt"
  final val MarkdownFile = "md"
  final val UnknownFile = "unknown"

  final val TargetFileName = "target"
  final val DotCharacter = "."

  final val QuoteCharacter = "\""
  final val CommentSymbol = "//"
  final val StarCharacter = "*"
  final val CommentStartSymbol = "/**"
  final val CommentEndSymbol = "*/"

  final val NonWordRegex = """([ˆ©@/<>{}()`'$+-=\\\[\\\].,_#$!?*|:;"]+)""".r

  def traverseFiles(directory: File, rewriteFiles: Boolean)(func: (String, String, Int) => LineResult): Seq[String] = {

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

  def inspectSource(line: String, ln: Int, checkFunction: String => String): LineResult = {
    var lineResult = LineResult(original = line, updated = line, words = Seq.empty[WordPair], lineNumber = ln)

    // Handle words inside of quotes
    if (lineResult.updated.indexOf(QuoteCharacter) > 0) {
      val fromIndex = lineResult.updated.indexOf(QuoteCharacter)
      val toIndex = lineResult.updated.indexOf(QuoteCharacter, fromIndex + 1)
      val startLine = lineResult.updated.substring(0, fromIndex + 1)
      val endLine = lineResult.updated.substring(toIndex, lineResult.updated.length)
      val result = inspectText(lineResult.updated.substring(fromIndex + 1, toIndex), ln, checkFunction)
      lineResult = lineResult.copy(updated = startLine + result.updated + endLine, words = result.words)
    }

    // Handle words in line comments
    if (lineResult.updated.indexOf(CommentSymbol) > 0) {
      val fromIndex = lineResult.updated.indexOf(CommentSymbol)
      val startLine = lineResult.updated.substring(0, fromIndex + 2)
      val result = inspectText(lineResult.updated.substring(fromIndex + 2, lineResult.updated.length), ln, checkFunction)
      lineResult = lineResult.copy(updated = startLine + result.updated, words = lineResult.words ++ result.words)
    }

    // Handle words in multiline comments
    if (lineResult.updated.indexOf(StarCharacter) > -1 && lineResult.updated.indexOf(StarCharacter) < 2) {
      val result = inspectText(lineResult.updated, ln, checkFunction)
      lineResult = lineResult.copy(updated = result.updated, words = lineResult.words ++ result.words)
    }

    lineResult
  }

  def inspectText(line: String, ln: Int, checkFunction: String => String): LineResult = {
    var updatedWords = Seq.empty[WordPair]
    val words: Seq[String] =
      line.split(" ") map { word =>
        val trimmedWord = word.trim
        if (trimmedWord == `CommentStartSymbol` || trimmedWord == `StarCharacter` || trimmedWord == `CommentSymbol` || trimmedWord == `CommentEndSymbol`)
          word
        else if (isWord(word)) {
          val result = checkFunction(word)
          if (result != word) updatedWords = updatedWords :+ WordPair(word, result)
          result
        } else {
          // Try to remove any surrounding non-word characters
          val (washed, left, right) =
            try {
              wash(word)
            } catch {
              case e: Exception =>
                // nothing to do but to ignore and continue with next word
                (word, "", "")
            }

          // Check if it is a proper word after the washing, if not just give up
          if (isWord(washed)) {
            val result = checkFunction(washed)
            if (result != washed) updatedWords = updatedWords :+ WordPair(washed, result)
            left + result + right
          } else
            word
        }
      }

    LineResult(original = line, updated = words.mkString(" "), words = updatedWords, lineNumber = ln)
  }

  private def isWord(chars: String): Boolean = NonWordRegex findFirstIn chars isEmpty

}

case class LineResult(original: String, updated: String, words: Seq[WordPair], lineNumber: Int) {
  def format: String = s"#$lineNumber : " + words.mkString(", ")
}

case class WordPair(original: String, updated: String) {
  override def toString: String = s"$original/$updated"
}
