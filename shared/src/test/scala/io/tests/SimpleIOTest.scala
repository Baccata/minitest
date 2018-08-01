package io.tests

import cats.Parallel
import cats.effect.{ Effect, IO, Timer }
import minitest.SimpleIOTestSuite

object SimpleIOTest extends SimpleIOTestSuite[IO, IO.Par] {

  implicit def ec = scala.concurrent.ExecutionContext.Implicits.global

  def F = Effect[IO]
  def T = Timer[IO]
  def P = Parallel[IO, IO.Par]

  simpleTest("pipo")(IO { println("hello pipo!") })

  simpleTest("lino")(IO { println("hello lino!") })

}
