package com.growse.k8s.upsEventHandler.upsClient

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import mu.KotlinLogging

/**
 * Implementation of [Transport] that can connect to a remote NUT instance over a TCP socket
 *
 * @property host Hostname of the instance to connect to
 * @property port TCP Port to connect on
 */
class SocketTransport(private val host: String, private val port: UShort) : Transport {
  private val logger = KotlinLogging.logger {}
  private var socket = Socket()
  private var reader: BufferedReader? = null
  private var writer: OutputStreamWriter? = null

  override fun connect() {
    logger.info { "Connecting to $host:$port" }
    socket = Socket(host, port.toInt()).apply { soTimeout = 1000 }
    reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    writer = OutputStreamWriter(socket.getOutputStream())
    logger.info { "Socket connected to $host:$port : $isConnected" }
  }

  override fun writeLine(line: String) {
    writer?.run {
      write(line + if (line.endsWith("\n")) "" else "\n")
      flush()
    }
  }

  override fun readLine(): String {
    return reader?.readLine() ?: ""
  }

  override val isConnected: Boolean
    get() = socket.isConnected

  override fun close() {
    reader?.close()
    writer?.close()
    socket.close()
  }
}
