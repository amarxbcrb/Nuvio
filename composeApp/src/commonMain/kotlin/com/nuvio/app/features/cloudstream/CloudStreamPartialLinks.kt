package com.nuvio.app.features.cloudstream

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Put in the coroutine context of a link search to hear about the links of an extension as they
 * arrive, before the extension has finished. Nothing is cut short: the search still waits as
 * long as before, the links are only shown earlier.
 */
class CloudStreamPartialLinks(
    val onLinks: (List<CloudStreamPlaybackSource>) -> Unit,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CloudStreamPartialLinks>
}
