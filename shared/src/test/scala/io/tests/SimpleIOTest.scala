package io.tests

import cats.Parallel
import cats.effect.{ Effect, IO, Timer }
import minitest.SimpleIOTestSuite

object SimpleIOTest extends SimpleIOTestSuite[IO, IO.Par] {

  implicit def ec = scala.concurrent.ExecutionContext.Implicits.global

  def F = Effect[IO]
  def T = Timer[IO]
  def P = Parallel[IO, IO.Par]

  simpleTest("pipo")(IO { Thread.sleep(1000); println("hello pipo!") })

  simpleTest("lino")(IO { Thread.sleep(500); println("hello lino!") })

}
