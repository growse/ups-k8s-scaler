package com.growse.k8s.upsEventHandler.upsClient

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class UPSStateTest :
    FunSpec({
      test("UPS State can parse valid state") {
        val input = "OL"
        Client.UPSStates.parse(input) shouldBe Client.UPSStates.OnLine
      }

      test("UPS State can parse valid state with extra bits") {
        val input = "OL SOMETHING ELSE"
        Client.UPSStates.parse(input) shouldBe Client.UPSStates.OnLine
      }
    })
