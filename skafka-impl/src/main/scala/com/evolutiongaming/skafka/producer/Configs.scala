package com.evolutiongaming.skafka.producer

import com.evolutiongaming.config.ConfigHelper._
import com.evolutiongaming.nel.Nel
import com.typesafe.config.{Config, ConfigException}

import scala.concurrent.duration._

/**
  * Check [[http://kafka.apache.org/documentation/#producerconfigs]]
  *
  * @param bootstrapServers - should be in the form of "host1:port1","host2:port2,..."
  * @param clientId         - An id string to pass to the server when making requests
  * @param acks             - possible values [all, -1, 0, 1]
  * @param compressionType  = possible values [none, gzip, snappy, lz4]
  */
case class Configs(
  bootstrapServers: Nel[String] = Nel("localhost:9092"),
  clientId: String = "",
  acks: String = "1",
  bufferMemory: Long = 33554432L,
  compressionType: String = "none",
  batchSize: Int = 16384,
  connectionsMaxIdle: FiniteDuration = 9.minutes,
  linger: FiniteDuration = 0.millis,
  maxBlock: FiniteDuration = 1.minute,
  maxRequestSize: Int = 1048576,
  receiveBufferBytes: Int = 32768,
  requestTimeout: FiniteDuration = 30.seconds,
  sendBufferBytes: Int = 131072,
  enableIdempotence: Boolean = false,
  maxInFlightRequestsPerConnection: Int = 5,
  metadataMaxAge: FiniteDuration = 5.minutes,
  reconnectBackoffMax: FiniteDuration = 1.second,
  reconnectBackoff: FiniteDuration = 50.millis,
  retryBackoff: FiniteDuration = 100.millis)

object Configs {

  lazy val Default: Configs = Configs()

  def apply(config: Config): Configs = {

    def get[T: FromConf](path: String, paths: String*) = {
      config.getOpt[T](path, paths: _*)
    }

    def getDuration(path: String, pathMs: => String) = {
      val value = try get[FiniteDuration](path) catch { case _: ConfigException => None }
      value orElse get[Long](pathMs).map { _.millis }
    }

    Configs(
      bootstrapServers = get[Nel[String]](
        "bootstrap-servers",
        "bootstrap.servers") getOrElse Default.bootstrapServers,
      clientId = get[String](
        "client-id",
        "client.id") getOrElse Default.clientId,
      acks = get[String]("acks") getOrElse Default.acks,
      bufferMemory = get[Long](
        "buffer-memory",
        "buffer.memory") getOrElse Default.bufferMemory,
      compressionType = get[String](
        "compression-type",
        "compression.type") getOrElse Default.compressionType,
      batchSize = get[Int](
        "batch-size",
        "batch.size") getOrElse Default.batchSize,
      connectionsMaxIdle = getDuration(
        "connections-max-idle",
        "connections.max.idle.ms") getOrElse Default.connectionsMaxIdle,
      linger = getDuration(
        "linger",
        "linger.ms") getOrElse Default.linger,
      maxBlock = getDuration(
        "max-block",
        "max.block.ms") getOrElse Default.maxBlock,
      maxRequestSize = get[Int](
        "max-request-size",
        "max.request.size") getOrElse Default.maxRequestSize,
      receiveBufferBytes = get[Int](
        "receive-buffer-bytes",
        "receive.buffer.bytes") getOrElse Default.receiveBufferBytes,
      requestTimeout = getDuration(
        "request-timeout",
        "request.timeout.ms") getOrElse Default.requestTimeout,
      sendBufferBytes = get[Int](
        "send-buffer-bytes",
        "send.buffer.bytes") getOrElse Default.sendBufferBytes,
      enableIdempotence = get[Boolean](
        "enable-idempotence",
        "enable.idempotence") getOrElse Default.enableIdempotence,
      maxInFlightRequestsPerConnection = get[Int](
        "max-in-flight-requests-per-connection",
        "max.in.flight.requests.per.connection") getOrElse Default.maxInFlightRequestsPerConnection,
      metadataMaxAge = getDuration(
        "metadata-max-age",
        "metadata.max.age.ms") getOrElse Default.metadataMaxAge,
      reconnectBackoffMax = getDuration(
        "reconnect-backoff-max",
        "reconnect.backoff.max.ms") getOrElse Default.reconnectBackoffMax,
      reconnectBackoff = getDuration(
        "reconnect-backoff",
        "reconnect.backoff.ms") getOrElse Default.reconnectBackoff,
      retryBackoff = getDuration(
        "retry-backoff",
        "retry.backoff.ms") getOrElse Default.retryBackoff)
  }

  implicit def nelFromConf[T](implicit fromConf: FromConf[List[T]]): FromConf[Nel[T]] = {
    FromConf { case (config, path) =>
      val list = fromConf(config, path)
      Nel.unsafe(list)
    }
  }
}
