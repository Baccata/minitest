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

import cats.effect.Effect
import minitest.api._

import scala.concurrent.ExecutionContext

abstract class IOTestSuite[F[_], Global, Local](implicit F: Effect[F])
    extends AbstractIOTestSuite[F]
    with IOAsserts[F] {

  def setupSuite: F[Global]
  def tearDownSuite(g: Global): F[Unit]
  def setup(g: Global): F[Local]
  def tearDown(env: Local): F[Unit]

  implicit def ec: ExecutionContext

  def test(name: String)(f: Local => F[Unit]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      propertiesSeq = propertiesSeq :+
        IOSpec.create[F, Local](name, env => f(env))
    }

  lazy val properties: IOProps[F, _, _] =
    synchronized {
      if (!isInitialized) isInitialized = true
      IOProps(setup, tearDown, setupSuite, tearDownSuite, propertiesSeq)
    }

  private[this] var propertiesSeq = Seq.empty[IOSpec[F, Local, Unit]]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )
}
