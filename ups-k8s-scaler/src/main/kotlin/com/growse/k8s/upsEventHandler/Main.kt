package com.growse.k8s.upsEventHandler

import com.growse.k8s.upsEventHandler.k8s.ScaleDirection
import com.growse.k8s.upsEventHandler.k8s.scaleK8sResources
import com.growse.k8s.upsEventHandler.upsClient.Client
import com.growse.k8s.upsEventHandler.upsClient.SocketTransport
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.util.Config
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun main(args: Array<String>): Unit = coroutineScope {
    launch {
        val parser = ArgParser("ups-k8s-scaler")
        val scaleDownImmediatelyOnPowerLoss by parser.option(
            ArgType.Boolean,
            "scale-down-immediately",
            description = "Scale down immediately on power loss"
        ).default(false)
        val hostname by parser.option(
            ArgType.String,
            "hostname",
            "H",
            "Hostname of the remote upsd instance to connect to"
        ).default("localhost")
        val port by parser.option(ArgType.Int, "port", "p", "Port of the remote upsd instance to connect to")
            .default(3493)

        val dryRun by parser.option(ArgType.Boolean, "dry-run", null, "Dry run scaling actions").default(false)
        val debug by parser.option(ArgType.Boolean, "debug", "d", "Enable debug logging").default(false)

        parser.parse(args)

        if (debug) {
            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug");
        }
        logger.debug { "Debug logging enabled" }
        if (dryRun) {
            logger.warn { "Dry run mode enabled" }
        }

        Configuration.setDefaultApiClient(Config.defaultClient())

        SocketTransport(hostname, port.toUShort()).use {
            Client(
                it,
                mapOf(
                    Client.UPSStates.OnLine to { scaleK8sResources(ScaleDirection.UP, dryRun) },
                    (if (scaleDownImmediatelyOnPowerLoss) Client.UPSStates.OnBattery else Client.UPSStates.LowBattery) to {
                        scaleK8sResources(ScaleDirection.DOWN, dryRun)
                    }
                )
            ).connect()
        }
    }
}