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

import java.util.regex.Matcher

import cats.effect.Sync
import cats.implicits._
import cats.{Eq, Show}

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.control.NonFatal

trait IOAsserts[F[_]] {

  def assert(condition: => Boolean, hint: String)(
      implicit
      F: Sync[F],
      pos: SourceLocation): F[Unit] =
    F.delay(condition)
      .flatMap[Unit] {
        if (_) F.unit
        else F.raiseError(new AssertionException(hint, pos))
      }
      .handleErrorWith {
        case NonFatal(ex) => F.raiseError(new UnexpectedException(ex, pos))
        case e            => F.raiseError(e)
      }

  def assert(condition: => Boolean)(
      implicit F: Sync[F],
      pos: SourceLocation): F[Unit] =
    assert(condition, "assertion failed!")

  def assertEquals[T: Eq: Show](expected: T)(
      callback: => T)(implicit F: Sync[F], pos: SourceLocation): F[Unit] =
    assertResult(expected, "received {0} != expected {1}")(callback)

  def assertResult[T: Eq: Show](expected: T, hint: String)(callback: => T)(
      implicit
      F: Sync[F],
      pos: SourceLocation): F[Unit] =
    F.delay {
        val rs = callback
        (rs, rs === expected)
      }
      .flatMap[Unit] {
        case (_, true) => F.unit
        case (rs, false) =>
          val msg = Asserts.format(hint, rs.show, expected.show)
          F.raiseError(new AssertionException(msg, pos))
      }
      .handleErrorWith {
        case NonFatal(ex) => F.raiseError(new UnexpectedException(ex, pos))
        case e            => F.raiseError(e)
      }

  def intercept[E <: Throwable](cb: F[Unit])(
      implicit F: Sync[F],
      E: ClassTag[E],
      pos: SourceLocation): F[Unit] =
    cb.flatMap[Unit] { _ =>
        val name = E.runtimeClass.getName
        val msg  = s"expected a $name to be thrown"
        F.raiseError(new AssertionException(msg, pos))
      }
      .handleErrorWith {
        case NonFatal(ex) if E.runtimeClass.isInstance(ex) => F.unit
      }

  def cancel(implicit F: Sync[F], pos: SourceLocation): F[Unit] =
    F.raiseError(new CanceledException(None, Some(pos)))

  def cancel(reason: String)(implicit F: Sync[F], pos: SourceLocation): Unit =
    F.raiseError(new CanceledException(Some(reason), Some(pos)))

  def ignore(implicit F: Sync[F], pos: SourceLocation): F[Unit] =
    F.raiseError(new IgnoredException(None, Some(pos)))

  def ignore(reason: String)(implicit F: Sync[F], pos: SourceLocation): Unit =
    F.raiseError(new IgnoredException(Some(reason), Some(pos)))

  def fail(implicit F: Sync[F], pos: SourceLocation): Unit =
    F.raiseError(new AssertionException("failed", pos))

  def fail(reason: String)(implicit F: Sync[F], pos: SourceLocation): Unit =
    F.raiseError(new AssertionException(reason, pos))
}

object IOAsserts {
  def format(tpl: String, values: String*): String = {
    @tailrec
    def loop(index: Int, acc: String): String =
      if (index >= values.length) acc
      else {
        val value = String.valueOf(values(index))
        val newStr =
          acc.replaceAll(s"[{]$index[}]", Matcher.quoteReplacement(value))
        loop(index + 1, newStr)
      }

    loop(0, tpl)
  }
}
