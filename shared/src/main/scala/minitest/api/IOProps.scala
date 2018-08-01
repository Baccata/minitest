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

import cats.effect.{ ConcurrentEffect, Fiber, IO, Timer }
import cats.implicits._
import cats.effect.implicits._
import cats.~>
import minitest.api.IOProps.Kl
import minitest.api.StreamProps.Kls

import scala.concurrent.ExecutionContext
//import minitest.api.StreamProps.Kls

import fs2.Stream

sealed trait AbstractIOProps {
  def compile: IO[List[Event]]
}

case class IOProps[F[_]: ConcurrentEffect: Timer, G, L](
    globalBracket: Kl[F, G, ?] ~> F,
    localBracket: Kl[F, L, ?] ~> Kl[F, G, ?],
    properties: List[IOSpec[F, L, Unit]])
    extends AbstractIOProps {

  def compile: IO[List[Event]] =
    globalBracket { g =>
      properties
        .traverse[F, Fiber[F, Event]] { spec =>
          localBracket { l =>
            spec.compile(l)
          }.apply(g).start
        }
        .flatMap(_.traverse(fiber => fiber.join))
    }.toIO

}

object IOProps {
  type Kl[F[_], A, B] = A => F[B]
}

case class StreamProps[F[_]: ConcurrentEffect, G, L](
    globalBracket: Kls[F, G, ?] ~> Stream[F, ?],
    localBracket: Kls[F, L, ?] ~> Kls[F, G, ?],
    properties: List[StreamSpec[F, L, Unit]])(implicit ec: ExecutionContext)
    extends AbstractIOProps {

  private implicit val T: Timer[F] = Timer.derive[F]

  def compile: IO[List[Event]] = {
    globalBracket { g =>
      Stream
        .emits(properties)
        .map { spec =>
          localBracket { l =>
            spec.compile(l)
          }.apply(g)
        }
        .join(10)
    }.compile.toList.toIO
  }

}

object StreamProps {
  type Kls[F[_], A, B] = A => Stream[F, B]
}
