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

import minitest.api._

abstract class NaiveIOTestSuite[F[_], AF[_], Global, Local]
    extends IOTestSuite[F, AF, Global, Local]
    with IOAsserts[F] {

  def setupSuite: F[Global]
  def tearDownSuite(g: Global): F[Unit]
  def setup(g: Global): F[Local]
  def tearDown(env: Local): F[Unit]

  override final def globalBracket[A](withG: Global => F[A]): F[A] =
    F.bracket(setupSuite)(withG)(tearDownSuite)

  override final def localBracket[A](withL: Local => F[A]): Global => F[A] =
    global => F.bracket(setup(global))(withL)(tearDown)
}
