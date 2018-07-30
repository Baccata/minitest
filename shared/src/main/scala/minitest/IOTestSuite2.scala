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

import cats.data.State
import cats.effect.Effect
import minitest.api._
import IOProps2._
import cats.~>

import scala.concurrent.ExecutionContext

abstract class IOTestSuite2[F[_], Global, Local](implicit F: Effect[F])
    extends AbstractIOTestSuite[F]
    with IOAsserts[F] {

  def globalBracket[A](withG: Global => F[A]): F[A]
  def localBracket[A](withL: Local => F[A]): State[Global, F[A]]

  implicit def ec: ExecutionContext

  def test(name: String)(f: Local => F[Unit]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      propertiesSeq = propertiesSeq :+
        IOSpec.create[F, Local](name, env => f(env))
    }

  lazy val properties2: IOProps2[F, _, _] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val gb: Kl[F, Global, ?] ~> F = new (Kl[F, Global, ?] ~> F) {
        override def apply[A](fa: Global => F[A]): F[A] = globalBracket(fa)
      }

      val lb: Kl[F, Local, ?] ~> St[F, Global, ?] =
        new (Kl[F, Local, ?] ~> St[F, Global, ?]) {
          override def apply[A](fa: Local => F[A]): State[Global, F[A]] =
            localBracket(fa)
        }

      IOProps2[F, Global, Local](gb, lb, propertiesSeq.toList)
    }

  private[this] var propertiesSeq = Seq.empty[IOSpec[F, Local, Unit]]
  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )
}
