package com.growse.k8s.upsEventHandler.upsClient

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A client for a remote NUT instance that knows how to list available UPS devices, and then monitor their status
 *
 * @property transport A [Transport] instance to handle the underlying network communication
 */

class Client(private val transport: Transport, private val callbackMap: Map<UPSStates, suspend () -> Unit>) {
    private val logger = KotlinLogging.logger {}
    private val monitorDelay: Duration = 2.seconds

    /**
     * Sends a command to the UPS, and reads the response back.
     *
     * @param command the command to send
     * @return a parsed [UPSResponse] of what came back
     */
    private fun sendCommand(command: String): UPSResponse {
        transport.writeLine(command)
        return (
            transport.run {
                readLine().let {
                    if (it.startsWith("BEGIN ")) {
                        val responseLines = mutableListOf(it)
                        while (!responseLines.last().startsWith("END ")) {
                            try {
                                responseLines.add(readLine())
                            } catch (e: Transport.TimeoutException) {
                                logger.error(e) { "Timeout from socket" }
                                return@let listOf("TIMEOUT")
                            }
                        }
                        responseLines
                    } else {
                        listOf(it)
                    }
                }
            }
        ).run(this::parseResponseLines)
    }

    private val errorPrefix = "ERR "
    private val varPrefix = "VAR "
    private val timeoutString = "TIMEOUT"
    private val beginPrefix = "BEGIN "
    private val endPrefix = "END "

    /**
     * Converts a list of lines received from the transport into a [UPSResponse]. A single response can be spread across
     * multiple lines, or not, as the case may be.
     *
     * @param responseLines a list of lines to parse
     * @return a [UPSResponse] that represents the response back from the UPS
     */
    private fun parseResponseLines(responseLines: List<String>): UPSResponse {
        logger.debug("Parsing UPS response: $responseLines")
        return if (responseLines.isEmpty()) {
            UPSResponse.NoResponse
        } else if (responseLines.size == 1 && responseLines.first().startsWith(errorPrefix)) {
            UPSResponse.Error(responseLines.first().substring(errorPrefix.length))
        } else if (responseLines.size == 1 && responseLines.first() == timeoutString) {
            UPSResponse.Timeout
        } else if (responseLines.size == 1 && responseLines.first().startsWith(varPrefix) && responseLines.first()
                .split(" ", limit = 4).size == varPrefix.length
        ) {
            responseLines.first().split(" ", limit = 4)
                .let { UPSResponse.UPSVariable(it[2], it[3].removeSurrounding("\"")) }
        } else if (responseLines.first().startsWith(beginPrefix) &&
            responseLines.last()
                .startsWith(endPrefix) && responseLines.first().substring(beginPrefix.length) ==
            responseLines.last()
                .substring(endPrefix.length)
        ) {
            when (responseLines.first().substring(beginPrefix.length)) {
                "LIST UPS" -> {
                    UPSResponse.UPSList(
                        responseLines.subList(1, responseLines.size - 1)
                            .map { it.split(" ", limit = 3) }.filter { it.first() == "UPS" }
                            .map { UPS(it[1], if (it.size == 3) it[2].removeSurrounding("\"") else "") },
                    )
                }

                else -> {
                    UPSResponse.UnKnownResponse
                }
            }
        } else {
            UPSResponse.UnKnownResponse
        }
    }

    /**
     * Polls a UPS for its status. If it changes, do something
     *
     * @param ups the UPS instance to poll the status of
     */
    private suspend fun monitorUps(ups: UPS) {
        logger.info { "Monitoring UPS: $ups" }
        var previousState: UPSStates? = null
        var job: Job? = null
        while (transport.isConnected) {
            delay(monitorDelay)
            logger.debug("Checking UPS status")
            when (val upsStateValue = getUPSStatus(ups.name)) {
                is UPSResponse.UPSVariable -> {
                    when (val upsState = UPSStates.parse(upsStateValue.value)) {
                        UPSStates.OnLine, UPSStates.OnBattery, UPSStates.LowBattery -> {
                            if (previousState == null || previousState != upsState) {
                                previousState = upsState
                                logger.info { "UPS ${ups.name} changed state to ${upsStateValue.value}" }
                                job?.run {
                                    if (this.isActive) {
                                        logger.warn("Cancelling currently running scale task")
                                        cancelAndJoin()
                                    }
                                }
                                job =
                                    CoroutineScope(Dispatchers.Default).launch {
                                        callbackMap[upsState]?.invoke()
                                    }
                            }
                        }

                        else -> {
                            logger.error("Unable to parse ups status: ${upsStateValue.value}")
                        }
                    }
                }

                else -> {
                    logger.error("Unexpected response from UPS status call: $upsStateValue")
                }
            }
        }
        job?.join()
    }

    /**
     * Connects the underlying transport, grabs the list of UPS's from the remote instance and starts monitoring one of
     * them
     */
    suspend fun connect(): Result<Job> {
        transport.connect()
        if (transport.isConnected) {
            logger.info { "Connected to UPS" }
            return when (val upsListResponse = listUps()) {
                is UPSResponse.UPSList -> {
                    logger.info { "There are ${upsListResponse.upsList.size} UPS devices available" }
                    if (upsListResponse.upsList.isNotEmpty()) {
                        Result.success(coroutineScope { launch { monitorUps(upsListResponse.upsList.first()) } })
                    } else {
                        Result.failure(NoUPSFoundException())
                    }
                }

                else -> {
                    Result.failure(UnexpectedResultException(upsListResponse))
                }
            }
        } else {
            return Result.failure(TransportNotConnectedException())
        }
    }

    private fun listUps() = sendCommand("LIST UPS")

    private fun getUPSStatus(name: String) = sendCommand("GET VAR ${name.filter { !it.isWhitespace() }} ups.status")

    /**
     * All the things that can come back from a UPS that we might care about
     */
    sealed class UPSResponse {
        data class UPSList(val upsList: List<UPS>) : UPSResponse()

        data class Error(val name: String) : UPSResponse()

        data class UPSVariable(val name: String, val value: String) : UPSResponse()

        object Timeout : UPSResponse()

        object NoResponse : UPSResponse()

        object UnKnownResponse : UPSResponse()
    }

    /**
     * A UPS!
     *
     * @property name the internal name that NUT uses to identify this UPS
     * @property description the description of what this is that NUT provides
     */
    data class UPS(val name: String, val description: String)

    /**
     * The various statuses that a UPS can be in that we care about
     *
     * @property statusCode the code used by NUT to represent the status
     */
    enum class UPSStates(val statusCode: String) {
        OnLine("OL"),
        OnBattery("OB"),
        LowBattery("LB"),
        ;

        companion object {
            /**
             * Converts a NUT-represented status into an enum instance
             *
             * @param arg the NUT-represented status to parse
             * @return An instance of [UPSStates], or null if the input couldn't be parsed
             */
            fun parse(arg: String): UPSStates? = values().firstOrNull { it.statusCode == arg.split(" ")[0] }
        }
    }

    class NoUPSFoundException : Throwable()

    class TransportNotConnectedException : Throwable()

    class UnexpectedResultException(val upsResponse: UPSResponse) : Throwable() {
        override val message: String
            get() = "Unexpected response from UPSD $upsResponse"
    }
}
