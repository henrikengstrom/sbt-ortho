package ortho

import org.scalatest.{ Matchers, WordSpecLike }

class StyleAlignerSpec extends WordSpecLike with Matchers {
  "The style aligner" should {
    "only replace string words in Scala files" in {
      StyleAligner.initialize("/UK-US.txt")

      val multipleLinesSection =
        """
          |val colour = new Colour(colour: String)
          |val analyse = new Analyse(analyseOrder: Long)
        """.stripMargin
      StyleAligner.inspect(multipleLinesSection, "scala", 1).updated should equal(multipleLinesSection)

      val oneLinerOriginal = """val analyse: String = "Analyse this @@ Colour!""""
      val oneLinerResult = """val analyse: String = "Analyze this @@ Color!""""
      StyleAligner.inspect(oneLinerOriginal, "scala", 1).updated should equal(oneLinerResult)

      val printlnOriginal = """println("analyse these colours please")"""
      val printlnResult = """println("analyze these colors please")"""
      StyleAligner.inspect(printlnOriginal, "scala", 1).updated should equal(printlnResult)

      val commentOriginal = """val colour = "blue analyse"  // this COLOUR is quite nice"""
      val commentResult = """val colour = "blue analyze"  // this COLOR is quite nice"""
      StyleAligner.inspect(commentOriginal, "scala", 1).updated should equal(commentResult)

      val multiLineCommentOriginal = " * Analyse this colour description."
      val multiLineCommentResult = " * Analyze this color description."
      StyleAligner.inspect(multiLineCommentOriginal, "scala", 1).updated should equal(multiLineCommentResult)
    }

    "should replace all words in Markdown files" in {
      StyleAligner.inspect("_Analyse this colour_ *now* please", "md", 1).updated should equal("_Analyze this color_ *now* please")
    }

    "should replace all words in text files" in {
      StyleAligner.inspect("_Analyse this COLOUR_ *now* please", "txt", 1).updated should equal("_Analyze this COLOR_ *now* please")
    }

    "should not create upper case for names" in {
      StyleAligner.inspect("StatsD", "txt", 0).updated should equal("StatsD")
    }
  }
}
