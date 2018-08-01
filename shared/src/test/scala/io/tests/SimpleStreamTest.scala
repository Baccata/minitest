package io.tests

import cats.effect.IO
import cats.implicits._
import minitest.SimpleStreamTestSuite

import scala.concurrent.ExecutionContext.Implicits.global

import fs2._

object SimpleStreamTest extends SimpleStreamTestSuite[IO] {

  simpleTest("stream pipo!")(
    Stream(1, 2, 3, 4, 5).covary[IO].to(Sink.showLinesStdOut).take(1))

}
