package com.growse.k8s.upsEventHandler.upsClient

import java.io.BufferedReader
import java.io.EOFException
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketTimeoutException
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
  private var connected = false

  override fun connect() {
    logger.info { "Connecting to $host:$port" }
    socket = Socket(host, port.toInt()).apply { soTimeout = 1000 }
    reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    writer = OutputStreamWriter(socket.getOutputStream())
    connected = true
    logger.info { "Socket connected to $host:$port : $isConnected" }
  }

  override fun writeLine(line: String) {
    try {
      writer?.run {
        write(line + if (line.endsWith("\n")) "" else "\n")
        flush()
      }
    } catch (e: IOException) {
      connected = false
      throw e
    }
  }

  override fun readLine(): String {
    return try {
      reader?.readLine() ?: throw EOFException("Remote end closed the connection")
    } catch (e: SocketTimeoutException) {
      throw Transport.TimeoutException()
    } catch (e: IOException) {
      connected = false
      throw e
    }
  }

  override val isConnected: Boolean
    get() = connected

  override fun close() {
    connected = false
    reader?.close()
    writer?.close()
    socket.close()
  }
}
