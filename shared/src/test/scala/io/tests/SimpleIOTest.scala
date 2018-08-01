package io.tests

import cats.effect.IO
import cats.implicits._
import minitest.SimpleIOTestSuite

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object SimpleIOTest extends SimpleIOTestSuite[IO] {

  simpleTest("pipo")(
    T.sleep(5000.milliseconds) *> IO { println("hello pipo!") })

  simpleTest("lino")(T.sleep(5000.milliseconds) *> assertEquals(2)(1))

}
