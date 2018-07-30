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

import cats.effect.{ Effect, IO, Sync }
import cats.syntax.all._

import scala.util.control.NonFatal

case class IOSpec[F[_], I, O](name: String, f: I => F[Result[O]])
    extends (I => F[Result[O]]) {

  override def apply(v1: I): F[Result[O]] = f(v1)

  def asIO(implicit F: Effect[F]): IOSpec[IO, I, O] =
    IOSpec(name, f andThen F.toIO)
}

object IOSpec {
  def create[F[_], Env](name: String, cb: Env => F[Unit])(
      implicit F: Sync[F]): IOSpec[F, Env, Unit] =
    IOSpec(name, { env =>
      cb(env)
        .map(u => Result.success(u))
        .recoverWith {
          case NonFatal(ex) => F.pure(Result.from(ex))
        }
    })

}
