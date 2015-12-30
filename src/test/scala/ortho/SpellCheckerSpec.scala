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
      val result = SpellChecker.convert("serious seriuos sreiuos", "", 1)
      result.updated should equal("serious serious serious")
      result.words.size should be(2)
    }

    "spell check UK ENG to US ENG" in {
      val result = SpellChecker.convert("colour", "", 1)
      result.updated should equal("color")
      result.words.size should be(1)
    }

    "fix misspelled code" in {
      val result = SpellChecker.convert("* Analyze thsi and look for colors please.", "", 1)
      result.updated should equal("* Analyze this and look for colors please.")
      result.words.size should be(1)
    }
  }
}