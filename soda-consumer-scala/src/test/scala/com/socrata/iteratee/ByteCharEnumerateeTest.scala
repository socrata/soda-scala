package com.socrata.iteratee

import scala.io.Codec

import java.nio.charset.CodingErrorAction
import java.nio.ByteBuffer

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.prop.PropertyChecks
import org.scalacheck.{Gen, Arbitrary}
import collection.mutable

class ByteCharEnumerateeTest extends WordSpec with MustMatchers with PropertyChecks {
  def codec = Codec("utf-8").onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE)

  def decodeBytes(bytes: Array[Byte]) = codec.decoder.decode(ByteBuffer.wrap(bytes)).toString

  val BytesAndSplits = for {
    bytes <- Arbitrary.arbitrary[Array[Byte]]
    splitCount <- Gen.choose(0, bytes.length)
    splits <- Gen.listOfN(splitCount, Gen.choose(0, bytes.length))
  } yield (new mutable.WrappedArray.ofByte(bytes), splits)

  "ByteCharEnumeratee" should {
    "produce an empty string if fed nothing at all" in {
      new ByteCharEnumeratee(codec, new StringIteratee).endOfInput() must equal ("")
    }

    "produce the same characters as just decoding the whole thing" in {
      forAll(BytesAndSplits) { case (bytesSeq, unsortedSplits) =>
        val bytes = bytesSeq.toArray
        whenever(unsortedSplits.forall { i => 0 <= i && i <= bytes.length }) {
          val splits = unsortedSplits.sorted
          val chunks =
            if(splits.isEmpty) Seq(bytes)
            else if(splits.lengthCompare(1) == 0) Seq(bytes.slice(0, splits.head), bytes.slice(splits.head, bytes.length))
            else (0 +: splits :+ bytes.length).sliding(2).map { case Seq(a, b) => bytes.slice(a, b) }

          val result = chunks.foldLeft(new ByteCharEnumeratee(codec, new StringIteratee) : ByteIteratee[String]) { (iteratee, chunk) =>
            iteratee.process(chunk).left.get
          }.endOfInput()
          result must equal (decodeBytes(bytes))
        }
      }
    }

    "raise an error if endOfInput is called multiple times" in {
      val it = new ByteCharEnumeratee(codec, new StringIteratee)
      it.endOfInput()
      an [IllegalStateException] must be thrownBy { it.endOfInput() }
    }

    "raise an error if process is called multiple times" in {
      val it = new ByteCharEnumeratee(codec, new StringIteratee)
      it.process(Array[Byte](1,2,3))
      an [IllegalStateException] must be thrownBy { it.process(Array[Byte](1,2,3)) }
    }
  }
}
