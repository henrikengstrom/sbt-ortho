package ortho

import ortho.OrthoPlugin.{ FileInfo, LineResult, WordPair }

import scala.collection.mutable.{ Map => MMap }
import scala.io.Source
import scala.util.matching.Regex
import scala.util.matching.Regex.MatchIterator

/**
 * Simple spell checker. Based on the ideas in Peter Norvig's blog post here: http://norvig.com/spell-correct.html
 * Inspiration for Scala implementation: http://theyougen.blogspot.com/2009/12/peter-norvigs-spelling-corrector-in.html
 */
object SpellChecker {
  // Adjust the alphabet after language being spell checked - right now it expects US English
  val alphabet = ('a' to 'z').toSeq
  val wordsRegex: Regex = ("[%s]+" format alphabet.mkString).r

  type OccurrenceMap = Map[String, Int]
  type WordPairSeq = Seq[(String, String)]

  final val NonWordRegex = """([ˆ.,_#$!?*|"]+)""".r

  var occurrenceMap: OccurrenceMap = Map.empty[String, Int]

  /**
   * Extracts all words, based on regex, from text into an iterator.
   */
  def words(text: String): MatchIterator = wordsRegex findAllIn text

  /**
   * Counts all occurrences of words in an iterator
   */
  def countOccurrences(words: MatchIterator): OccurrenceMap = {
    val mmap = MMap.empty[String, Int]
    while (words.hasNext) {
      val word = words.next().toLowerCase
      mmap.update(word, mmap.getOrElse(word, 0) + 1)
    }
    mmap.toMap
  }

  /**
   * Creates a list of words with missing characters.
   * Passing in Seq(("",test), (t,est), (te,st), (tes,t), (test,"") will generate result:
   * Seq("est", "tst", "tet", "tes")
   */
  def deletes(pairs: WordPairSeq): Seq[String] =
    for {
      (p1, p2) <- pairs
      if p2.length > 0
    } yield p1 + p2.substring(1)

  /**
   * Creates a list of transposed words.
   * Passing in Seq(("",test), (t,est), (te,st), (tes,t), (test,"") will generate result:
   * Seq("etst", "tset", "tets")
   */
  def transposes(pairs: WordPairSeq): Seq[String] =
    for {
      (p1, p2) <- pairs
      if p2.length > 1
    } yield p1 + p2(1) + p2(0) + p2.substring(2)

  /**
   * Creates a list of words with replaced characters.
   * Passing in Seq(("",test), (t,est), (te,st), (tes,t), (test,"") will generate result:
   * Vector("aest", "best", "cest", "dest", "eest" ... tesw", "tesx", "tesy", "tesz")
   *
   * TODO Performance: Use character vicinity to narrow down the number of results.
   * E.g. 'a' should only be substituted with 'q', 'w', 's', 'z' instead of 'a' through 'z'
   */
  def replaces(pairs: WordPairSeq): Seq[String] =
    for {
      (p1, p2) <- pairs
      p3 <- alphabet if p2.length > 0
    } yield p1 + p3 + p2.substring(1)

  /**
   * Creates a list of words with inserted characters.
   * Passing in Seq(("",test), (t,est), (te,st), (tes,t), (test,"") will generate result:
   * Vector("aest", "best", "cest", "dest", "eest" ... tesw", "tesx", "tesy", "tesz")
   *
   * TODO Performance: Use character vicinity to narrow down the number of results.
   * E.g. 'a' should only be inserted with 'q', 'w', 's', 'z' instead of 'a' through 'z'
   */
  def inserts(pairs: WordPairSeq): Seq[String] =
    for {
      (p1, p2) <- pairs
      p3 <- alphabet
    } yield p1 + p3 + p2

  /**
   * Method to split word in combination pairs one letter at a time, e.g. "test" will result in Vector(("",test), (t,est), (te,st), (tes,t), (test,""))
   */
  def wordSplitter(word: String): WordPairSeq =
    for {
      i <- 0 to word.length
    } yield (word take i, word drop i)

  /**
   * Creates a list of all variations.
   */
  def spellingVariations(pairs: WordPairSeq): Seq[String] = deletes(pairs) ++ transposes(pairs) ++ replaces(pairs) ++ inserts(pairs)

  /**
   * Returns a list of counts for words that are available in the occurrence map.
   */
  def existingWords(words: Seq[String]): Seq[String] =
    for {
      word <- words
      _ <- occurrenceMap.get(word)
    } yield word

  /**
   * Creates a list of one off words based on the word parameter.
   */
  def oneOffVariations(word: String): Seq[String] = spellingVariations(wordSplitter(word))

  /**
   * Creates a list of two off words based on the word parameter.
   */
  def twoOffVariations(word: String): Seq[String] =
    for {
      oneOffSeq <- oneOffVariations(word)
      twoOffSeq <- oneOffVariations(oneOffSeq)
      // existingTwoOffWords <- occurrenceMap.get(twoOffSeq) // performance optimization, only use real words for the two off words
    } yield twoOffSeq

  def selectNonEmpty[T](candidates: Seq[T], others: => Seq[T]): Seq[T] = if (candidates.isEmpty) others else candidates

  /**
   * Check for word candidates for 'word'. If it exists untouched in the occurrence map we use that.
   * If not we check the one offs and should it not be present there we try the two offs.
   * Note that the last check uses lazy evaluation to not trigger unnecessary CPU cycles.
   */
  def candidates(word: String): Seq[String] = selectNonEmpty(existingWords(List(word)), selectNonEmpty(existingWords(oneOffVariations(word)), existingWords(twoOffVariations(word))))

  /**
   * If there are more than one candidate we select the one with the highest occurrence count.
   */
  def check(word: String): String =
    if (word.head.isUpper) {
      val candidate = candidates(word.toLowerCase).foldLeft((-1, word.toLowerCase))((max, w) => if (occurrenceMap(w) > max._1) (occurrenceMap(w), w) else max)._2
      if (word.last.isUpper) candidate.toUpperCase
      else candidate.head.toUpper + candidate.tail
    } else
      candidates(word).foldLeft((-1, word))((max, w) => if (occurrenceMap(w) > max._1) (occurrenceMap(w), w) else max)._2

  // TODO (performance): no reload needed if training file is not changed between runs
  def initialize(fileName: String) = {
    // Note that Source.fromInputStream has a performance bug but it's only been fixed in 2.11:
    // http://stackoverflow.com/questions/5221524/idiomatic-way-to-convert-an-inputstream-to-a-string-in-scala
    occurrenceMap = countOccurrences(words(Source.fromInputStream(this.getClass.getResourceAsStream(fileName)).getLines().mkString("\n")))
  }

  def checkFiles(filesInfo: Seq[FileInfo], rewriteFiles: Boolean): Seq[String] = {
    Seq.empty[String]
  }

  def convert(line: String, fileType: String, ln: Int): LineResult = {
    var updatedWords = Seq.empty[WordPair]
    val updatedLine = (line.split(" ") map { word: String =>
      if (word.length < 2 || !word.head.isLetter) word
      else {
        val (washed, left, right) =
          try {
            OrthoPlugin.wash(word)
          } catch {
            case e: Exception =>
              // nothing to do but to ignore and continue with next word
              (word, "", "")
          }

        val result = check(washed)
        if (result != washed) updatedWords = updatedWords :+ WordPair(washed, result)
        left + result + right
      }

    }).mkString(" ")

    LineResult(original = line, updated = updatedLine, words = updatedWords, lineNumber = ln)
  }
}