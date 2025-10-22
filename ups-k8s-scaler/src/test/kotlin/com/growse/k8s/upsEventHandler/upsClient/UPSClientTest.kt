package com.growse.k8s.upsEventHandler.upsClient

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.LinkedList
import java.util.Queue

internal class UPSClientTest :
    FunSpec({
      test(
          "given a LIST UPS command, when the response is incomplete, an UnexpectedResultException is returned") {
            val transport =
                PretendTransport(
                    listUpsCommandResponse = listOf("BEGIN LIST UPS", "UPS test \"Dummy\""),
                    getStatusVarResponse = "NONSENSE",
                )
            val result = Client(transport, mapOf()).connect()
            result.shouldBeFailure()
            val exception = result.exceptionOrNull()
            exception.shouldBeInstanceOf<Client.UnexpectedResultException>()
            (exception as Client.UnexpectedResultException)
                .upsResponse
                .shouldBeInstanceOf<Client.UPSResponse.Timeout>()
          }

      test(
          "given a LIST UPS command, when the response is not a list, an UnexpectedResultException is returned") {
            val transport =
                PretendTransport(
                    listUpsCommandResponse = listOf("PARP", "NONSENSE"),
                    getStatusVarResponse = "NONSENSE",
                )
            val result = Client(transport, mapOf()).connect()
            result.shouldBeFailure()
            result.exceptionOrNull().shouldBeInstanceOf<Client.UnexpectedResultException>()
          }

      test(
          "given a LIST UPS command, when the response is a list but with no valid UPS entries in it, then a NoUpsFoundException is returned") {
            val transport =
                PretendTransport(
                    listUpsCommandResponse = listOf("BEGIN LIST UPS", "NONSENSE", "END LIST UPS"),
                    getStatusVarResponse = "NONSENSE",
                )
            val result = Client(transport, mapOf()).connect()
            result.shouldBeFailure()
            result.exceptionOrNull().shouldBeInstanceOf<Client.NoUPSFoundException>()
          }

      test(
          "given a LIST UPS command, when the response is an empty list, then a NoUpsFoundException is returned") {
            val transport = PretendTransport(getStatusVarResponse = "NONSENSE")
            val result = Client(transport, mapOf()).connect()
            result.shouldBeFailure()
            result.exceptionOrNull().shouldBeInstanceOf<Client.NoUPSFoundException>()
          }

      test(
          "given a UPS that's online, when the monitor is started, then the online callback is called") {
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
                    )
                    .connect()
            result.shouldBeSuccess()
            shouldNotThrowAny { result.getOrNull()?.join() }
            onlineToggle.shouldBeTrue()
            onbatteryToggle.shouldBeFalse()
            lowBatteryToggle.shouldBeFalse()
          }

      test(
          "given a UPS that's onbattery, when the monitor is started, then the onbattery callback is called") {
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
                    )
                    .connect()
            result.shouldBeSuccess()
            shouldNotThrowAny { result.getOrNull()?.join() }
            onlineToggle.shouldBeFalse()
            onbatteryToggle.shouldBeTrue()
            lowBatteryToggle.shouldBeFalse()
          }

      test(
          "given a UPS that's lowbattery, when the monitor is started, then the lowbattery callback is called") {
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
                    )
                    .connect()
            result.shouldBeSuccess()
            shouldNotThrowAny { result.getOrNull()?.join() }
            onlineToggle.shouldBeFalse()
            onbatteryToggle.shouldBeFalse()
            lowBatteryToggle.shouldBeTrue()
          }

      test(
          "given a UPS that's online, when the monitor is started and the status returns trailing characters, then the online callback is called") {
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
                    )
                    .connect()
            result.shouldBeSuccess()
            shouldNotThrowAny { result.getOrNull()?.join() }
            onlineToggle.shouldBeTrue()
            onbatteryToggle.shouldBeFalse()
            lowBatteryToggle.shouldBeFalse()
          }

      test(
          "given a UPS that's online, when the monitor is started and the status returns nonsense, then no callback is called") {
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
                    )
                    .connect()
            result.shouldBeSuccess()
            shouldNotThrowAny { result.getOrNull()?.join() }
            onlineToggle.shouldBeFalse()
            onbatteryToggle.shouldBeFalse()
            lowBatteryToggle.shouldBeFalse()
          }
    }) {
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
