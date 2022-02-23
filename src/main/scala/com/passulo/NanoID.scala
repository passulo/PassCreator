package com.passulo

import java.security.SecureRandom

/** Approximation of https://github.com/ai/nanoid
 * See https://zelark.github.io/nano-id-cc/
 *
 * Test runs: Length = 7:
 * 1. 20 duplicates in 10 000 000
 * 1. 13 duplicates in 10 000 000
 *
 * Test runs: Length = 8
 * 1. no duplicates in 10 000 000
 * 2. no duplicates in 10 000 000
 * 3. no duplicates in 10 000 000
 *
 * */
object NanoID {

  private val alphabet  = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray
  private val mask: Int = (2 << Math.floor(Math.log(alphabet.length - 1) / Math.log(2)).toInt) - 1

  def create(length: Int = 8): String = {
    val rand  = new SecureRandom()
    val bytes = new Array[Byte](length)
    rand.nextBytes(bytes)

    (0 until length)
      .map(position => bytes(position) & mask)
      .map(index => alphabet(index % alphabet.length))
      .mkString("")
  }
}
