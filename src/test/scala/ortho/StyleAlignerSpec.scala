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
      StyleAligner.convert(multipleLinesSection, OrthoPlugin.ScalaFile, 1).updated should equal(multipleLinesSection)

      val oneLinerOriginal = """val analyse: String = "Analyse this @@ Colour!""""
      val oneLinerResult = """val analyse: String = "Analyze this @@ Color!""""
      StyleAligner.convert(oneLinerOriginal, OrthoPlugin.ScalaFile, 1).updated should equal(oneLinerResult)

      val printlnOriginal = """println("analyse these colours please")"""
      val printlnResult = """println("analyze these colors please")"""
      StyleAligner.convert(printlnOriginal, OrthoPlugin.ScalaFile, 1).updated should equal(printlnResult)

      val commentOriginal = """val colour = "blue analyse"  // this COLOUR is quite nice"""
      val commentResult = """val colour = "blue analyze"  // this COLOR is quite nice"""
      StyleAligner.convert(commentOriginal, OrthoPlugin.ScalaFile, 1).updated should equal(commentResult)

      val multiLineCommentOriginal = " * Analyse this colour description."
      val multiLineCommentResult = " * Analyze this color description."
      StyleAligner.convert(multiLineCommentOriginal, OrthoPlugin.ScalaFile, 1).updated should equal(multiLineCommentResult)
    }

    "should replace all words in Markdown files" in {
      StyleAligner.convert("_Analyse this colour_ *now* please", OrthoPlugin.MarkdownFile, 1).updated should equal("_Analyze this color_ *now* please")
    }

    "should replace all words in text files" in {
      StyleAligner.convert("_Analyse this COLOUR_ *now* please", OrthoPlugin.TextFile, 1).updated should equal("_Analyze this COLOR_ *now* please")
    }
  }
}
