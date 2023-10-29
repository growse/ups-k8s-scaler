package com.growse.k8s.upsEventHandler.upsClient

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import java.util.LinkedList
import java.util.Queue
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
internal class UPSClientTest {
    @Test
    fun `given a LIST UPS command, when the response is incomplete, an UnexpectedResultException is returned`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse = listOf("BEGIN LIST UPS", "UPS test \"Dummy\""),
                    getStatusVarResponse = "NONSENSE",
                )
            val result = Client(transport, mapOf()).connect()
            assertTrue { result.isFailure }
            assertIs<Client.UnexpectedResultException>(result.exceptionOrNull())
            assertIs<Client.UPSResponse.Timeout>(
                result.exceptionOrNull()?.let { (it as Client.UnexpectedResultException).upsResponse },
            )
        }

    @Test
    fun `given a LIST UPS command, when the response is not a list, an UnexpectedResultException is returned`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse = listOf("PARP", "NONSENSE"),
                    getStatusVarResponse = "NONSENSE",
                )
            val result = Client(transport, mapOf()).connect()
            assertTrue { result.isFailure }
            assertIs<Client.UnexpectedResultException>(result.exceptionOrNull())
        }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a LIST UPS command, when the response is a list but with no valid UPS entries in it, then a NoUpsFoundException is returned`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse = listOf("BEGIN LIST UPS", "NONSENSE", "END LIST UPS"),
                    getStatusVarResponse = "NONSENSE",
                )
            val result = Client(transport, mapOf()).connect()
            assertTrue { result.isFailure }
            assertIs<Client.NoUPSFoundException>(result.exceptionOrNull())
        }

    @Test
    fun `given a LIST UPS command, when the response is an empty list, then a NoUpsFoundException is returned`() =
        runTest {
            val transport = PretendTransport(getStatusVarResponse = "NONSENSE")
            val result = Client(transport, mapOf()).connect()
            assertTrue { result.isFailure }
            assertIs<Client.NoUPSFoundException>(result.exceptionOrNull())
        }

    @Test
    fun `given a UPS that's online, when the monitor is started, then the online callback is called`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse =
                        listOf(
                            "BEGIN LIST UPS",
                            "UPS test \"dummy\"",
                            "END LIST UPS",
                        ),
                    upsName = "test",
                    getStatusVarResponse = "VAR ups ups.status \"OL\"",
                )
            var onlineToggle = false
            var onbatteryToggle = false
            var lowBatteryToggle = false
            val result =
                Client(
                    transport,
                    mapOf(
                        Client.UPSStates.OnLine to { onlineToggle = true },
                        Client.UPSStates.OnBattery to { onbatteryToggle = true },
                        Client.UPSStates.LowBattery to { lowBatteryToggle = true },
                    ),
                ).connect()
            assertTrue { result.isSuccess }
            result.getOrNull()?.join()
            assertTrue { onlineToggle }
            assertFalse { onbatteryToggle }
            assertFalse { lowBatteryToggle }
        }

    @Test
    fun `given a UPS that's onbattery, when the monitor is started, then the onbattery callback is called`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse =
                        listOf(
                            "BEGIN LIST UPS",
                            "UPS test \"dummy\"",
                            "END LIST UPS",
                        ),
                    upsName = "test",
                    getStatusVarResponse = "VAR ups ups.status \"OB\"",
                )
            var onlineToggle = false
            var onbatteryToggle = false
            var lowBatteryToggle = false
            val result =
                Client(
                    transport,
                    mapOf(
                        Client.UPSStates.OnLine to { onlineToggle = true },
                        Client.UPSStates.OnBattery to { onbatteryToggle = true },
                        Client.UPSStates.LowBattery to { lowBatteryToggle = true },
                    ),
                ).connect()
            assertTrue { result.isSuccess }
            result.getOrNull()?.join()
            assertFalse { onlineToggle }
            assertTrue { onbatteryToggle }
            assertFalse { lowBatteryToggle }
        }

    @Test
    fun `given a UPS that's lowbattery, when the monitor is started, then the lowbattery callback is called`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse =
                        listOf(
                            "BEGIN LIST UPS",
                            "UPS test \"dummy\"",
                            "END LIST UPS",
                        ),
                    upsName = "test",
                    getStatusVarResponse = "VAR ups ups.status \"LB\"",
                )
            var onlineToggle = false
            var onbatteryToggle = false
            var lowBatteryToggle = false
            val result =
                Client(
                    transport,
                    mapOf(
                        Client.UPSStates.OnLine to { onlineToggle = true },
                        Client.UPSStates.OnBattery to { onbatteryToggle = true },
                        Client.UPSStates.LowBattery to { lowBatteryToggle = true },
                    ),
                ).connect()
            assertTrue { result.isSuccess }
            result.getOrNull()?.join()
            assertFalse { onlineToggle }
            assertFalse { onbatteryToggle }
            assertTrue { lowBatteryToggle }
        }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `given a UPS that's online, when the monitor is started and the status returns trailing characters, then the online callback is called`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse =
                        listOf(
                            "BEGIN LIST UPS",
                            "UPS test \"dummy\"",
                            "END LIST UPS",
                        ),
                    upsName = "test",
                    getStatusVarResponse = "VAR ups ups.status \"OL CHRG\"",
                )
            var onlineToggle = false
            var onbatteryToggle = false
            var lowBatteryToggle = false
            val result =
                Client(
                    transport,
                    mapOf(
                        Client.UPSStates.OnLine to { onlineToggle = true },
                        Client.UPSStates.OnBattery to { onbatteryToggle = true },
                        Client.UPSStates.LowBattery to { lowBatteryToggle = true },
                    ),
                ).connect()
            assertTrue { result.isSuccess }
            result.getOrNull()?.join()
            assertTrue { onlineToggle }
            assertFalse { onbatteryToggle }
            assertFalse { lowBatteryToggle }
        }

    @Test
    fun `given a UPS that's online, when the monitor is started and the status returns nonsense, then no callback is called`() =
        runTest {
            val transport =
                PretendTransport(
                    listUpsCommandResponse =
                        listOf(
                            "BEGIN LIST UPS",
                            "UPS test \"dummy\"",
                            "END LIST UPS",
                        ),
                    upsName = "test",
                    getStatusVarResponse = "NONSENSE",
                )
            var onlineToggle = false
            var onbatteryToggle = false
            var lowBatteryToggle = false
            val result =
                Client(
                    transport,
                    mapOf(
                        Client.UPSStates.OnLine to { onlineToggle = true },
                        Client.UPSStates.OnBattery to { onbatteryToggle = true },
                        Client.UPSStates.LowBattery to { lowBatteryToggle = true },
                    ),
                ).connect()
            assertTrue { result.isSuccess }
            result.getOrNull()?.join()
            assertFalse { onlineToggle }
            assertFalse { onbatteryToggle }
            assertFalse { lowBatteryToggle }
        }

    class PretendTransport(
        private val listUpsCommandResponse: List<String> =
            listOf(
                "BEGIN LIST UPS",
                "END LIST UPS",
            ),
        private val upsName: String = "",
        private val getStatusVarResponse: String,
    ) : Transport {
        private var connected = false
        private val responses: Queue<String> = LinkedList()

        override fun writeLine(line: String) {
            when (line.trim()) {
                "LIST UPS" -> {
                    responses.run {
                        clear()
                        addAll(listUpsCommandResponse)
                    }
                }

                "GET VAR $upsName ups.status" -> {
                    responses.run {
                        clear()
                        add(getStatusVarResponse)
                    }
                    // Disconnect so the client exits the loop
                    connected = false
                }

                else -> {}
            }
        }

        override fun readLine(): String {
            try {
                return responses.remove()
            } catch (_: Throwable) {
                throw Transport.TimeoutException()
            }
        }

        override val isConnected: Boolean
            get() = connected

        override fun connect() {
            connected = true
        }

        override fun close() {
            connected = false
        }
    }
}
