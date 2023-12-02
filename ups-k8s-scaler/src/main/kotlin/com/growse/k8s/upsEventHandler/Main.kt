package com.growse.k8s.upsEventHandler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.growse.k8s.upsEventHandler.k8s.ScaleDirection
import com.growse.k8s.upsEventHandler.k8s.checkK8sConnectivity
import com.growse.k8s.upsEventHandler.k8s.scaleK8sResources
import com.growse.k8s.upsEventHandler.upsClient.Client
import com.growse.k8s.upsEventHandler.upsClient.SocketTransport
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.util.Config
import kotlin.system.exitProcess
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

const val DEFAULT_UPSD_PORT = 3493

class Main : CliktCommand(name = "ups-k8s-scaler") {
  private val scaleDownImmediatelyOnPowerLoss: Boolean by
      option(
              help = "Scale down immediately when the UPS switches to OnBattery",
              envvar = "SCALE_DOWN_IMMEDIATELY_ON_POWER_LOSS",
          )
          .flag()
  private val scaleDownImmediately: Boolean by
      option(
              help = "Don't connect to upsd, just scale everything down and quit",
              envvar = "SCALE_DOWN_IMMEDIATELY",
          )
          .flag()
  private val scaleUpImmediately: Boolean by
      option(
              help = "Don't connect to upsd, just scale everything up and quit",
              envvar = "SCALE_UP_IMMEDIATELY",
          )
          .flag()
  private val upsdHostname: String by
      option(
              "-H",
              "--hostname",
              help = "Hostname of the remote upsd instance to connect to",
              envvar = "UPSD_HOSTNAME",
          )
          .default("localhost")
  private val upsdPort: Int by
      option(
              "-p",
              "--port",
              help = "Port of the remote upsd instance to connect to",
              envvar = "UPSD_PORT",
          )
          .int()
          .default(DEFAULT_UPSD_PORT)
  private val dryRun: Boolean by option(help = "Dry run scaling actions", envvar = "DRY_RUN").flag()
  private val debug: Boolean by option(help = "Enable debug logging", envvar = "DEBUG_LOG").flag()

  override fun run() {
    runBlocking {
      launch {
        if (debug) {
          System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug")
        }
        logger.debug { "Debug logging enabled" }
        if (dryRun) {
          logger.warn { "Dry run mode enabled" }
        }
        try {
          Configuration.setDefaultApiClient(Config.defaultClient())
        } catch (e: Exception) {
          logger.error(e) { "Unable to init kubeconfig" }
          exitProcess(1)
        }
        if (scaleDownImmediately) {
          scaleK8sResources(ScaleDirection.DOWN, dryRun)
        } else if (scaleUpImmediately) {
          scaleK8sResources(ScaleDirection.UP, dryRun)
        } else {
          checkK8sConnectivity()
              .onFailure {
                logger.error(it) { "Unable to connect to k8s" }
                throw ProgramResult(1)
              }
              .onSuccess { logger.info { "Successfully connected to k8s" } }
          SocketTransport(upsdHostname, upsdPort.toUShort()).use {
            Client(
                    it,
                    mapOf(
                        Client.UPSStates.OnLine to { scaleK8sResources(ScaleDirection.UP, dryRun) },
                        (if (scaleDownImmediatelyOnPowerLoss) {
                          Client.UPSStates.OnBattery
                        } else {
                          Client.UPSStates.LowBattery
                        }) to { scaleK8sResources(ScaleDirection.DOWN, dryRun) },
                    ),
                )
                .connect()
                .onSuccess { job -> job.also { logger.info { "Monitor job started" } }.join() }
                .onFailure { throwable ->
                  logger.error(throwable.cause) { "Unable to monitor UPS" }
                }
          }
        }
      }
    }
  }
}

fun main(args: Array<String>): Unit = Main().main(args)
