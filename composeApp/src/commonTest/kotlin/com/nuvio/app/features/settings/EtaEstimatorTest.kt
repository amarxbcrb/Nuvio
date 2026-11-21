package com.nuvio.app.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EtaEstimatorTest {
    @Test
    fun nothingBeforeOneAndAHalfSeconds() {
        val eta = EtaEstimator()
        assertNull(eta.update(done = 10.0, total = 100.0, elapsedMs = 500))
        assertNull(eta.update(done = 20.0, total = 100.0, elapsedMs = 1_499))
    }

    @Test
    fun constantRateGivesExactRemainingTime() {
        val eta = EtaEstimator()
        eta.update(10.0, 100.0, 1_000)
        // 10 items per second throughout: 50 items left = 5 s.
        assertEquals(5_000L, eta.update(50.0, 100.0, 5_000))
    }

    @Test
    fun rateIsSmoothedWithPointTwoWeight() {
        val eta = EtaEstimator()
        eta.update(10.0, 100.0, 1_000) // 0.01 items/ms
        // New instant rate 0.02 -> smoothed 0.2*0.02 + 0.8*0.01 = 0.012; 70 left -> 5833 ms.
        assertEquals(5_833L, eta.update(30.0, 100.0, 2_000))
    }
}
