/*
 * Copyright (c) 2014-2017 by its authors. Some rights reserved.
 * See the project homepage at: https://github.com/monix/minitest
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package minitest.api

import cats.effect.{ Effect, IO }
import cats.effect.implicits._
import cats.implicits._

import scala.concurrent.ExecutionContext

case class IOProps[F[_], G, L](
    setup: G => F[L],
    tearDown: L => F[Unit],
    setupSuite: F[G],
    tearDownSuite: G => F[Unit],
    properties: Seq[IOSpec[F, L, Unit]])(
    implicit
    F: Effect[F],
    ec: ExecutionContext) {

  private def compile(global: G)(
      spec: IOSpec[F, L, Unit]): IOSpec[F, Unit, Unit] =
    IOSpec.create[F, Unit](spec.name, { _ =>
      for {
        local <- setup(global)
        _     <- spec.f(local)
        _     <- tearDown(local)
      } yield ()
    })

  def runSuite(runSpec: IOSpec[IO, Unit, Unit] => IO[Unit]): IO[Unit] =
    setupSuite.toIO.bracket { g =>
      properties.toList
        .map(compile(g))
        .map(_.asIO)
        .parTraverse(runSpec)
        .map(_ => ())
    }(tearDownSuite andThen F.toIO)

}
