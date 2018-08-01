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

package minitest.runner

import cats.effect.IO
import minitest.api._
import org.scalajs.testinterface.TestUtils
import sbt.testing.{ Event => BaseEvent, Task => BaseTask, _ }

import scala.concurrent.ExecutionContext
import scala.util.Try

final class IOTask(task: TaskDef, cl: ClassLoader) extends BaseTask {
  implicit val ec: ExecutionContext = DefaultExecutionContext

  def tags(): Array[String] = Array.empty
  def taskDef(): TaskDef    = task

  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[BaseTask] = {

    val suiteIO = loadSuite(task.fullyQualifiedName(), cl).fold(IO.unit) {
      suite =>
        loggers.foreach(
          _.info(Console.GREEN + task.fullyQualifiedName() + Console.RESET))

        suite.properties.compile(event =>
          IO {
            loggers.foreach(_.info(event.result.formatted(event.name)))
            eventHandler.handle(sbtEvent(event))
        })
    }

    suiteIO.map(_ => Array.empty[BaseTask]).unsafeRunSync()
  }

  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger],
      continuation: Array[BaseTask] => Unit): Unit =
    continuation(execute(eventHandler, loggers))

  def loadSuite(
      name: String,
      loader: ClassLoader): Option[AbstractIOTestSuite] = {
    Try(TestUtils.loadModule(name, loader)).toOption
      .collect { case ref: AbstractIOTestSuite => ref }
  }

  def sbtEvent(event: Event): BaseEvent = new BaseEvent {
    import event._

    def fullyQualifiedName(): String =
      task.fullyQualifiedName()

    def throwable(): OptionalThrowable =
      result match {
        case Result.Exception(source, _) =>
          new OptionalThrowable(source)
        case Result.Failure(_, Some(source), _) =>
          new OptionalThrowable(source)
        case _ =>
          new OptionalThrowable()
      }

    def status(): Status =
      result match {
        case Result.Exception(_, _) =>
          Status.Error
        case Result.Failure(_, _, _) =>
          Status.Failure
        case Result.Success(_) =>
          Status.Success
        case Result.Ignored(_, _) =>
          Status.Ignored
        case Result.Canceled(_, _) =>
          Status.Canceled
      }

    def selector(): Selector = {
      task.selectors().head
    }

    def fingerprint(): Fingerprint =
      task.fingerprint()

    def duration(): Long =
      event.duration.toMillis
  }

}
