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

import minitest.api.Utils.discard
import sbt.testing.{ Runner => BaseRunner, Task => BaseTask, _ }

final class IORunner(
    val args: Array[String],
    val remoteArgs: Array[String],
    classLoader: ClassLoader)
    extends BaseRunner {

  def done(): String = ""

  def tasks(list: Array[TaskDef]): Array[BaseTask] = {
    list.map(t => new IOTask(t, classLoader))
  }

  def receiveMessage(msg: String): Option[String] = {
    discard[String](msg)
    None
  }

  def serializeTask(task: BaseTask, serializer: TaskDef => String): String =
    serializer(task.taskDef())

  def deserializeTask(task: String, deserializer: String => TaskDef): BaseTask =
    new IOTask(deserializer(task), classLoader)
}
