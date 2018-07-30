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

import cats.data.State
import cats.effect.implicits._
import cats.effect.{ Effect, IO }
import cats.implicits._
import cats.~>
import minitest.api.IOProps2.{ Kl, St }

import scala.concurrent.ExecutionContext

case class IOProps2[F[_], G, L](
    globalBracket: Kl[F, G, ?] ~> F,
    localBracket: Kl[F, L, ?] ~> St[F, G, ?],
    properties: List[IOSpec[F, L, Unit]])(
    implicit
    F: Effect[F],
    ec: ExecutionContext) {

  def compile(runSpec: IOSpec[IO, Unit, Unit] => IO[Unit]): IO[Unit] =
    globalBracket[Unit] { g =>
      val io = properties
        .traverse[State[G, ?], IO[Unit]] { spec =>
          localBracket { l =>
            spec.apply(l)
          }.map { fRes =>
            runSpec(IOSpec[IO, Unit, Unit](spec.name, _ => fRes.toIO))
          }
        }
        .runA(g)
        .value
        .parTraverse[IO, IO.Par, Unit](identity)
        .map(_ => ())
      F.liftIO(io)
    }.toIO

}

object IOProps2 {

  type Kl[F[_], A, B] = A => F[B]
  type St[F[_], S, A] = State[S, F[A]]

}
