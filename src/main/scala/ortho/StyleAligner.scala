package ortho

import scala.io.Source

object StyleAligner extends CheckerBase {
  type Dict = Map[String, String]

  final var dictionary: Dict = Map.empty[String, String]

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

  def inspect(line: String, fileType: String, lineNumber: Int): LineResult = fileType match {
    case `ScalaFile` | `JavaFile`    => inspectSource(line, lineNumber, check)
    case `TextFile` | `MarkdownFile` => inspectText(line, lineNumber, check)
  }

  private def check(word: String): String =
    if (word.isEmpty) {
      word
    } else {
      if (word.head.isUpper) {
        val result = dictionary.getOrElse(word.toLowerCase, word)
        // try to preserve case
        if (result == word) word
        else if (word.last.isUpper) result.toUpperCase
        else result.head.toUpper + result.tail
      } else
        dictionary.getOrElse(word, word)
    }
}