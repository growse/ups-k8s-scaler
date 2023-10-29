package com.growse.k8s.upsEventHandler.upsClient

import kotlin.test.Test
import kotlin.test.assertEquals

internal class UPSStateTest {
    @Test
    fun `UPS State can parse valid state`() {
        val input = "OL"
        assertEquals(Client.UPSStates.OnLine, Client.UPSStates.parse(input))
    }

    @Test
    fun `UPS State can parse valid state with extra bits`() {
        val input = "OL SOMETHING ELSE"
        assertEquals(Client.UPSStates.OnLine, Client.UPSStates.parse(input))
    }
}
