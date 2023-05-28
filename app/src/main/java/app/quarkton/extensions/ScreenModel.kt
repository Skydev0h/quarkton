package app.quarkton.extensions

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import kotlinx.coroutines.CoroutineScope

public val ScreenModel.crs: CoroutineScope get() = coroutineScope