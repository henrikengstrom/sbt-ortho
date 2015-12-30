package ortho

import java.io.File

import ortho.OrthoPlugin._
import sbt.IO

import scala.io.Source

object StyleAligner {
  type Dict = Map[String, String]

  final var dictionary: Dict = Map.empty[String, String]

  final val TargetFileName = "target"

  final val DotCharacter = "."
  final val QuoteCharacter = "\""
  final val CommentSymbol = "//"
  final val StarCharacter = "*"
  final val CommentStartSymbol = "/**"
  final val CommentEndSymbol = "*/"

  final val NonWordRegex = "([ˆ.,_#$!?*]+)".r

  /**
   * Loads the content of the dictionary specified.
   * TODO: no reload needed if dictionary file is not changed between runs
   */
  def initialize(fileName: String) = {
    val mmap = scala.collection.mutable.Map.empty[String, String]
    Source.fromInputStream(this.getClass.getResourceAsStream(fileName)).getLines foreach { line =>
      val commaPos = line.indexOf(",")
      if (commaPos > 0) mmap.put(line.substring(0, commaPos), line.substring(commaPos + 1, line.length))
    }
    dictionary = mmap.toMap
  }

  def convert(line: String, fileType: String, lineNumber: Int): LineResult = fileType match {
    case `ScalaFile` | `JavaFile`    => convertSource(line, lineNumber)
    case `TextFile` | `MarkdownFile` => convertText(line, lineNumber)
  }

  private def convertSource(line: String, ln: Int): LineResult = {
    var lineResult = LineResult(original = line, updated = line, words = Seq.empty[WordPair], lineNumber = ln)

    // Handle words inside of quotes
    if (lineResult.updated.indexOf(QuoteCharacter) > 0) {
      val fromIndex = lineResult.updated.indexOf(QuoteCharacter)
      val toIndex = lineResult.updated.indexOf(QuoteCharacter, fromIndex + 1)
      val startLine = lineResult.updated.substring(0, fromIndex + 1)
      val endLine = lineResult.updated.substring(toIndex, lineResult.updated.length)
      val result = convertText(lineResult.updated.substring(fromIndex + 1, toIndex), ln)
      lineResult = lineResult.copy(updated = startLine + result.updated + endLine, words = result.words)
    }

    // Handle words in line comments
    if (lineResult.updated.indexOf(CommentSymbol) > 0) {
      val fromIndex = lineResult.updated.indexOf(CommentSymbol)
      val startLine = lineResult.updated.substring(0, fromIndex + 2)
      val result = convertText(lineResult.updated.substring(fromIndex + 2, lineResult.updated.length), ln)
      lineResult = lineResult.copy(updated = startLine + result.updated, words = lineResult.words ++ result.words)
    }

    // Handle words in multiline comments
    if (lineResult.updated.indexOf(StarCharacter) > -1 && lineResult.updated.indexOf(StarCharacter) < 2) {
      val result = convertText(lineResult.updated, ln)
      lineResult = lineResult.copy(updated = result.updated, words = lineResult.words ++ result.words)
    }

    lineResult
  }

  private def convertText(line: String, ln: Int): LineResult = {
    var updatedWords = Seq.empty[WordPair]
    val words: Seq[String] =
      line.split(" ") map { word =>
        val trimmedWord = word.trim
        if (trimmedWord == `CommentStartSymbol` || trimmedWord == `StarCharacter` || trimmedWord == `CommentSymbol` || trimmedWord == `CommentEndSymbol`)
          word
        else
          NonWordRegex findFirstIn word match {
            case Some(_) =>
              val (washed, left, right) =
                try {
                  wash(word)
                } catch {
                  case e: Exception =>
                    // nothing to do but to ignore and continue with next word
                    (word, "", "")
                }
              val result = check(washed)
              if (result != washed) updatedWords = updatedWords :+ WordPair(washed, result)
              left + result + right
            case None =>
              val result = check(word)
              if (result != word) updatedWords = updatedWords :+ WordPair(word, result)
              result
          }
      }

    LineResult(original = line, updated = words.mkString(" "), words = updatedWords, lineNumber = ln)
  }

  private def check(word: String): String =
    if (word.isEmpty) {
      word
    } else {
      if (word.head.isUpper) {
        val result = dictionary.getOrElse(word.toLowerCase, word)
        // if both first and last chars in word is upper case we assume that the whole word should be upper cased
        if (word.last.isUpper) result.toUpperCase
        else result.head.toUpper + result.tail
      } else
        dictionary.getOrElse(word, word)
    }

}