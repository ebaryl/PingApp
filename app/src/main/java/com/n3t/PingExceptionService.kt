package com.n3t

import java.io.IOException
import java.net.UnknownHostException

class PingExceptionService {
    sealed class PingException(message: String) : Exception(message) {
        class NetworkUnavailable : PingException("Network is unavailable. Please check your internet connection.")
        class InvalidIPAddress : PingException("The provided IP address is invalid.")
        class UnknownHost(host: String) : PingException("Unable to resolve host: $host")
        class CommandExecutionError : PingException("Error executing ping command.")
        class Timeout : PingException("Ping operation timed out.")
        class PermissionDenied : PingException("Permission denied. The app might not have the necessary permissions.")
        class PacketLoss : PingException("All packets were lost during transmission.")
        class UnknownError(originalMessage: String) : PingException("An unknown error occurred: $originalMessage")
    }

    fun handleException(e: Exception): PingException {
        return when (e) {
            is SecurityException -> PingException.PermissionDenied()
            is UnknownHostException -> PingException.UnknownHost(e.message ?: "unknown")
            is IOException -> {
                when {
                    e.message?.contains("Permission denied") == true -> PingException.PermissionDenied()
                    e.message?.contains("No route to host") == true -> PingException.NetworkUnavailable()
                    else -> PingException.CommandExecutionError()
                }
            }
            is InterruptedException -> PingException.Timeout()
            else -> PingException.UnknownError(e.message ?: "No additional information")
        }
    }

    fun isNetworkError(exception: PingException): Boolean {
        return exception is PingException.NetworkUnavailable ||
                exception is PingException.UnknownHost ||
                exception is PingException.PacketLoss
    }

    fun isConfigurationError(exception: PingException): Boolean {
        return exception is PingException.InvalidIPAddress ||
                exception is PingException.PermissionDenied
    }
}