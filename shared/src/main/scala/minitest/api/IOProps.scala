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

import cats.effect.implicits._
import cats.effect.{ Effect, IO, Timer }
import cats.implicits._
import cats.{ Parallel, ~> }
import minitest.api.IOProps.Kl

case class IOProps[F[_], AF[_], G, L](
    globalBracket: Kl[F, G, ?] ~> F,
    localBracket: Kl[F, L, ?] ~> Kl[F, G, ?],
    properties: List[IOSpec[F, L, Unit]])(
    implicit
    F: Effect[F],
    P: Parallel[F, AF],
    T: Timer[F])
    extends AbstractIOProps {

  def compile: IO[List[Event]] =
    globalBracket { g =>
      properties
        .parTraverse { spec =>
          localBracket { l =>
            spec.compile(l)
          }.apply(g)
        }
    }.toIO

}

trait AbstractIOProps {

  def compile: IO[List[Event]]

}

object IOProps {

  type Kl[F[_], A, B] = A => F[B]

}
