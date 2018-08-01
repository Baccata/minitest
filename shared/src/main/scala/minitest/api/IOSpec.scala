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

import cats.FlatMap
import cats.effect.{ Sync, Timer }
import cats.syntax.all._

import fs2.Stream

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS, _ }

case class IOSpec[F[_], I, O](name: String, f: I => F[Result[O]])
    extends (I => F[Result[O]]) {

  override def apply(v1: I): F[Result[O]] = f(v1)

  def compile(i: I)(
      implicit F: FlatMap[F],
      T: Timer[F]
  ): F[Event] =
    for {
      start <- T.clockRealTime(MILLISECONDS)
      res   <- apply(i)
      end   <- T.clockRealTime(MILLISECONDS)
    } yield Event(name, (end - start).millis, res.asInstanceOf[Result[Unit]])
}

object IOSpec {
  def create[F[_], Env](name: String, cb: Env => F[Unit])(
      implicit F: Sync[F]): IOSpec[F, Env, Unit] =
    IOSpec(name, { env =>
      cb(env)
        .map(u => Result.success(u))
        .handleError(ex => Result.from(ex))
    })

}

case class StreamSpec[F[_], I, O](name: String, f: I => Stream[F, Result[O]])
    extends (I => Stream[F, Result[O]]) {

  override def apply(v1: I): Stream[F, Result[O]] = f(v1)

  def compile(i: I)(implicit T: Timer[F]): Stream[F, Event] =
    for {
      start <- Stream.eval(T.clockRealTime(MILLISECONDS))
      res   <- apply(i)
      end   <- Stream.eval(T.clockRealTime(MILLISECONDS))
    } yield Event(name, (end - start).millis, res.asInstanceOf[Result[Unit]])

}

object StreamSpec {
  def create[F[_], Env](
      name: String,
      cb: Env => Stream[F, Unit]): StreamSpec[F, Env, Unit] =
    StreamSpec(name, { env =>
      cb(env)
        .map(u => Result.success(u))
        .handleErrorWith(ex => Stream.emit(Result.from(ex)))
    })

}

case class Event(name: String, duration: FiniteDuration, result: Result[Unit])
