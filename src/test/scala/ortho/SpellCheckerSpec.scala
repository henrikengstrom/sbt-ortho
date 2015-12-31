package ortho

import org.scalatest.{ Matchers, WordSpecLike }

class SpellCheckerSpec extends WordSpecLike with Matchers {
  "Spell Check" should {
    SpellChecker.initialize("/US-training.txt")

    "create occurrence map" in {
      SpellChecker.occurrenceMap should not be empty
    }

    "split word into pairs" in {
      SpellChecker.wordSplitter("test") should equal(Seq(("", "test"), ("t", "est"), ("te", "st"), ("tes", "t"), ("test", "")))
    }

    "create deleted character list" in {
      SpellChecker.deletes(SpellChecker.wordSplitter("test")) should equal(Seq("est", "tst", "tet", "tes"))
    }

    "create transposed character list" in {
      SpellChecker.transposes(SpellChecker.wordSplitter("test")) should equal(Seq("etst", "tset", "tets"))
    }

    "create replaced character list" in {
      SpellChecker.replaces(SpellChecker.wordSplitter("tt")) should equal(Seq("at", "bt", "ct", "dt", "et", "ft", "gt", "ht", "it", "jt", "kt", "lt", "mt", "nt", "ot", "pt", "qt", "rt", "st", "tt", "ut", "vt", "wt", "xt", "yt", "zt", "ta", "tb", "tc", "td", "te", "tf", "tg", "th", "ti", "tj", "tk", "tl", "tm", "tn", "to", "tp", "tq", "tr", "ts", "tt", "tu", "tv", "tw", "tx", "ty", "tz"))
    }

    "create inserted character list" in {
      SpellChecker.inserts(SpellChecker.wordSplitter("tt")) should equal(Seq("att", "btt", "ctt", "dtt", "ett", "ftt", "gtt", "htt", "itt", "jtt", "ktt", "ltt", "mtt", "ntt", "ott", "ptt", "qtt", "rtt", "stt", "ttt", "utt", "vtt", "wtt", "xtt", "ytt", "ztt", "tat", "tbt", "tct", "tdt", "tet", "tft", "tgt", "tht", "tit", "tjt", "tkt", "tlt", "tmt", "tnt", "tot", "tpt", "tqt", "trt", "tst", "ttt", "tut", "tvt", "twt", "txt", "tyt", "tzt", "tta", "ttb", "ttc", "ttd", "tte", "ttf", "ttg", "tth", "tti", "ttj", "ttk", "ttl", "ttm", "ttn", "tto", "ttp", "ttq", "ttr", "tts", "ttt", "ttu", "ttv", "ttw", "ttx", "tty", "ttz"))
    }

    "fix misspelled words that exist in dictionary" in {
      val result = SpellChecker.inspect("serious seriuos sreiuos", "txt", 1)
      result.updated should equal("serious serious serious")
      result.words.size should be(2)
    }

    "spell check UK ENG to US ENG" in {
      val result = SpellChecker.inspect("colour", "txt", 1)
      result.updated should equal("color")
      result.words.size should be(1)
    }

    "find misspelled words in comments" in {
      val result1 = SpellChecker.inspect("* Analyze thsi and look for colros please.", "scala", 1)
      result1.updated should equal("* Analyze this and look for colors please.")
      result1.words.size should be(2)
    }

    "find misspelled code" in {
      val result = SpellChecker.inspect("""val analyseThis = "Some fulfill their golas"""", "scala", 1)
      result.updated should equal("""val analyseThis = "Some fulfill their goals"""")
      result.words.size should be(1)
    }

    "ignore text chunks with non-letters" in {
      val result = SpellChecker.inspect("""[pattern group exclude example](../../../test/scala/docs/ConfigurationDocsSpec.scala) { #pattern-group-exclude }""", "txt", 1)
      result.updated should equal("""[pattern group exclude example](../../../test/scala/docs/ConfigurationDocsSpec.scala) { #pattern-group-exclude }""")
      result.words should be(empty)
    }
  }
}