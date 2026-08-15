package io.github.razziell.freebuddynext.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import io.github.razziell.freebuddynext.protocol.MbbCommand
import io.github.razziell.freebuddynext.protocol.MbbStreamParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("MissingPermission")
class RfcommClient {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = MbbStreamParser()
    private val _commands = MutableSharedFlow<MbbCommand>(extraBufferCapacity = 32)
    val commands: SharedFlow<MbbCommand> = _commands

    /** Emitted when an established connection dies on its own (EOF or I/O error). */
    private val _connectionLost = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val connectionLost: SharedFlow<Unit> = _connectionLost

    @Volatile private var socket: BluetoothSocket? = null
    private var reader: Job? = null

    // Commands are sent from independent coroutines (fast ANC taps + delayed
    // read-backs); interleaved OutputStream writes would corrupt MBB frames.
    private val writeMutex = Mutex()

    suspend fun connect(device: BluetoothDevice, serviceUuid: UUID) = withContext(Dispatchers.IO) {
        close()
        val candidate = device.createRfcommSocketToServiceRecord(serviceUuid)
        socket = candidate
        try {
            try {
                withTimeout(CONNECT_TIMEOUT_MS) { blockingConnect(candidate) }
            } catch (timeout: TimeoutCancellationException) {
                throw IOException("Connection timed out", timeout)
            }
            reader = scope.launch { readLoop(candidate) }
        } catch (error: Throwable) {
            runCatching { candidate.close() }
            if (socket === candidate) socket = null
            throw error
        }
    }

    /**
     * BluetoothSocket.connect() is blocking and does not react to coroutine
     * cancellation by itself. Closing the socket from invokeOnCancellation
     * forces the blocked call to abort, so both explicit cancellation and
     * withTimeout above interrupt the attempt for real.
     */
    private suspend fun blockingConnect(candidate: BluetoothSocket) =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { runCatching { candidate.close() } }
            try {
                candidate.connect()
                if (continuation.isActive) continuation.resume(Unit)
            } catch (error: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }

    suspend fun send(command: MbbCommand) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val active = socket ?: throw IOException("RFCOMM is not connected")
            active.outputStream.write(command.toPayload())
            active.outputStream.flush()
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        val activeReader = reader
        reader = null
        val active = socket
        socket = null
        runCatching { active?.close() }
        if (activeReader != null && activeReader != kotlinx.coroutines.currentCoroutineContext()[Job]) {
            activeReader.cancelAndJoin()
        }
        parser.clear()
    }

    private suspend fun readLoop(active: BluetoothSocket) {
        val buffer = ByteArray(1024)
        try {
            while (true) {
                val count = active.inputStream.read(buffer)
                if (count < 0) break
                parser.append(buffer.copyOf(count)).forEach { _commands.emit(it) }
            }
        } catch (_: CancellationException) {
        } catch (_: IOException) {
            // Expected when the remote side drops the link; handled below.
        } finally {
            // socket !== active means close()/reconnect already took ownership.
            val diedOnItsOwn = socket === active
            if (diedOnItsOwn) socket = null
            runCatching { active.close() }
            if (diedOnItsOwn) _connectionLost.tryEmit(Unit)
        }
    }
}
