package io.tests

import cats.Parallel
import cats.effect.{ Effect, IO, Timer }
import minitest.SimpleIOTestSuite
import scala.concurrent.duration._
import cats.implicits._

object SimpleIOTest extends SimpleIOTestSuite[IO, IO.Par] {

  implicit def ec = scala.concurrent.ExecutionContext.Implicits.global

  def F = Effect[IO]
  def T = Timer[IO]
  def P = Parallel[IO, IO.Par]

  simpleTest("pipo")(
    T.sleep(2000.milliseconds) *> IO { println("hello pipo!") })

  simpleTest("lino")(assertEquals(2)(1))

}
