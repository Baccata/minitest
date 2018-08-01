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

package minitest

import cats.effect.{ ConcurrentEffect, Timer }

import scala.concurrent.ExecutionContext

import fs2._

abstract class NaiveIOTestSuite[F[_]: ConcurrentEffect: Timer, Global, Local]
    extends IOTestSuite[F, Global, Local] {

  def setupSuite: F[Global]
  def tearDownSuite(g: Global): F[Unit]
  def setup(g: Global): F[Local]
  def tearDown(env: Local): F[Unit]

  override final def globalBracket[A](withG: Global => F[A]): F[A] =
    F.bracket(setupSuite)(withG)(tearDownSuite)

  override final def localBracket[A](withL: Local => F[A]): Global => F[A] =
    global => F.bracket(setup(global))(withL)(tearDown)
}
abstract class NaiveStreamTestSuite[F[_]: ConcurrentEffect, Global, Local](
    implicit ec: ExecutionContext)
    extends StreamTestSuite[F, Global, Local] {

  def setupSuite: F[Global]
  def tearDownSuite(g: Global): F[Unit]
  def setup(g: Global): F[Local]
  def tearDown(env: Local): F[Unit]

  override def globalBracket[A](withG: Global => Stream[F, A]): Stream[F, A] =
    for {
      global <- Stream.eval(setupSuite)
      a      <- withG(global)
      _      <- Stream.eval(tearDownSuite(global))
    } yield a

  override def localBracket[A](
      withL: Local => Stream[F, A]): Global => Stream[F, A] =
    global =>
      for {
        local <- Stream.eval(setup(global))
        a     <- withL(local)
        _     <- Stream.eval(tearDown(local))
      } yield a
}
