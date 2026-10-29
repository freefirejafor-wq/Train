package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BangladeshRailwayCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RailPulse BD", appName)
    }

    @Test
    fun `calculate live telemetry for Suborno Express`() {
        val suborno = BangladeshRailwayCatalog.allTrains.first { it.trainNumber == "701" }
        val telemetry = BangladeshRailwayCatalog.calculateLiveTelemetry(suborno)
        assertEquals("701", telemetry.train.trainNumber)
        assertTrue(telemetry.totalDistanceKm > 300)
        assertTrue(telemetry.expectedNextArrivalFormatted.isNotBlank())
    }
}
