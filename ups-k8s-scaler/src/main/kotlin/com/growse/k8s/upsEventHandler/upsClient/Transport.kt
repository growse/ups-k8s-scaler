package com.growse.k8s.upsEventHandler.upsClient

/**
 * A network transport that can be given to a [Client] for communicating with a remote NUT instance
 */
interface Transport : java.io.Closeable {
  fun writeLine(line: String)

  fun readLine(): String

  val isConnected: Boolean

  fun connect()

  class TimeoutException : Exception()
}
