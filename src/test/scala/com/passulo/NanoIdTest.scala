package com.passulo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class NanoIdTest extends AnyWordSpec with Matchers {
  "NanoID" should {
    "return something" in {
      val res = NanoID.create(8)
      println(res)
      res.length shouldBe 8
    }

    "return unique enough results" in {
      //  1 000 000 takes ~4s
      // 10 000 000 takes ~40s
      val ids = List.fill(1000000)(NanoID.create(8))

      ids.size shouldBe ids.distinct.size
    }

  }
}
