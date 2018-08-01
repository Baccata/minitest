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

abstract class SimpleIOTestSuite[F[_]: ConcurrentEffect: Timer]
    extends NaiveIOTestSuite[F, Unit, Unit] {

  override def setupSuite: F[Unit]             = F.unit
  override def tearDownSuite(g: Unit): F[Unit] = F.unit
  override def setup(g: Unit): F[Unit]         = F.unit
  override def tearDown(env: Unit): F[Unit]    = F.unit

  def simpleTest(name: String)(f: => F[Unit]): Unit = test(name)(_ => f)

}
