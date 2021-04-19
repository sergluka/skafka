package com.evolutiongaming.skafka.consumer

import java.lang.{Long => LongJ}

import cats.Applicative
import cats.data.{NonEmptyList, NonEmptySet => Nes}
import cats.effect.IO
import cats.syntax.all._
import com.evolutiongaming.skafka.consumer.DataPoints._
import com.evolutiongaming.skafka.consumer.RebalanceCallback.implicits._
import com.evolutiongaming.skafka.consumer.RebalanceListener1SyntaxSpec._
import com.evolutiongaming.skafka.{Topic, TopicPartition}
import org.apache.kafka.common
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.util.Try
class RebalanceListener1SyntaxSpec extends AnyFreeSpec with Matchers {

  "type inference to the max" in {
    val consumer = new ExplodingConsumer {
      override def position(partition: common.TopicPartition): LongJ = 0L
    }
    val tfListener = new TfRebalanceListener1[IO]

    RebalanceCallback
      .run(tfListener.onPartitionsAssigned(partitions.s), consumer) mustBe Try(())

    RebalanceCallback
      .run(tfListener.onPartitionsRevoked(partitions.s), consumer) mustBe Try(())

    RebalanceCallback
      .run(tfListener.onPartitionsLost(partitions.s), consumer) mustBe Try(())
  }

}

object RebalanceListener1SyntaxSpec {
  // TODO: add complex example show casing better type inference with RebalanceCallback.api[F]
  class TfRebalanceListener1[F[_]: Applicative] extends RebalanceListener1WithConsumer[F] {

    def someF: F[Unit]          = ().pure[F]
    def someF2(a: Any): F[Unit] = a.pure[F] *> ().pure[F]
    def someFO: F[Option[Unit]] = ().some.pure[F]

    def onPartitionsAssigned(partitions: Nes[TopicPartition]) =
      for {
        _ <- someF.lift
        _ <- someFO.lift
      } yield ()

    def onPartitionsRevoked(partitions: Nes[TopicPartition]) = {
      groupByTopic(partitions) traverse_ {
        case (_, partitions) =>
          for {
            _ <- someF.lift
            partitionsOffsets <- partitions.toNonEmptyList traverse { partition =>
              // fails to compile with `RebalanceCallback.position` variant at
              // _ <- someF2(partitionsOffsets).lift
              // expected type RebalanceCallback[Nothing,?] but found RebalanceCallback[F,Unit]
              consumer.position(partition) map (partition -> _)
            }
            _ <- someF2(partitionsOffsets).lift
          } yield ()
      }
    }

    def onPartitionsLost(partitions: Nes[TopicPartition]) = consumer.empty

    def groupByTopic(
      topicPartitions: Nes[TopicPartition]
    ): NonEmptyList[(Topic, Nes[TopicPartition])] =
      topicPartitions.groupBy(_.topic).toNel

  }
}
