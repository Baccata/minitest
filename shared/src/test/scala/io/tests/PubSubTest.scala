package io.tests

import cats.effect.IO
import fs2.async.mutable.Topic
import fs2.{ Sink, Stream, async }
import minitest.StreamTestSuite
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object PubSubTest
    extends StreamTestSuite[IO, Topic[IO, Int], (Int, Stream[IO, Int])] {

  def sharedTopicStream: Stream[IO, Topic[IO, Int]] =
    Stream.eval(async.topic[IO, Int](0))

  def addPublisher(topic: Topic[IO, Int]): Stream[IO, Unit] =
    Stream.iterate(1)(_ + 1).covary[IO].repeat.to(topic.publish)

  def addSubscribe(id: Int, topic: Topic[IO, Int]): Stream[IO, Int] =
    topic
      .subscribe(10)
      .filter(_ % id == 0)
      .take(100)

  override def globalBracket[A](
      withG: Topic[IO, Int] => Stream[IO, A]): Stream[IO, A] =
    sharedTopicStream.flatMap { topic =>
      withG(topic).concurrently(addPublisher(topic))
    }

  override def localBracket[A](
      withL: ((Int, Stream[IO, Int])) => Stream[IO, A])(
      global: Topic[IO, Int]): Stream[IO, A] = {
    val newId = 3
    withL(newId -> addSubscribe(newId, global))
  }

  test("pipo") {
    case (id, stream) => stream.filter(_ % id == 0).last.to(Sink.showLinesStdOut)
  }

}
