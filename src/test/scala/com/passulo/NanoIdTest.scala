package com.passulo

import com.passulo.util.NanoID
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class NanoIdTest extends AnyWordSpec with Matchers {
  "NanoID" should {
    "return something" in {
      NanoID.create(0).length shouldBe 0
      NanoID.create(1).length shouldBe 1
      NanoID.create(8).length shouldBe 8
      NanoID.create(20).length shouldBe 20
      NanoID.create(200).length shouldBe 200
    }

    "return unique enough results" in {
      // add zeros as needed
      //  1 000 000 takes ~4s
      // 10 000 000 takes ~40s
      val ids = List.fill(100000)(NanoID.create(8))

      ids.size shouldBe ids.distinct.size
    }

  }
}
