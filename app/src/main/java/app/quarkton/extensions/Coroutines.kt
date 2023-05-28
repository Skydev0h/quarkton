package app.quarkton.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

suspend fun Collection<Job>.joinAllIgnoringAll(): Unit =
    forEach { try { it.join() } catch (_: Throwable) {} }

suspend fun Collection<Job>.joinAllIgnoringCancel(): Unit =
    forEach { try { it.join() } catch (_: CancellationException) {} }