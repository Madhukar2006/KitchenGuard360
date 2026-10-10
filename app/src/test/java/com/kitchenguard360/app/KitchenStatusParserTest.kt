package com.kitchenguard360.app

import com.kitchenguard360.app.data.model.BluetoothUiState
import com.kitchenguard360.app.data.model.KitchenStatusParser
import com.kitchenguard360.app.data.model.SafetyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KitchenStatusParserTest {

    @Test
    fun testNormalJsonParsing() {
        val json = """{"device":"KitchenGuard360","state":"NORMAL","mq6_raw":76,"mq2_raw":568,"flame_detected":false,"temperature_c":27.5,"humidity_percent":63.7,"mq6_warning":1800,"mq6_danger":2600,"mq2_warning":1800,"mq2_danger":2600,"temperature_warning":35.0,"temperature_danger":50.0,"uptime_seconds":64}"""
        val parsed = KitchenStatusParser.parseJson(json)

        assertNotNull(parsed)
        assertEquals("KitchenGuard360", parsed?.device)
        assertEquals("NORMAL", parsed?.state)
        assertEquals(76, parsed?.mq6Raw)
        assertEquals(568, parsed?.mq2Raw)
        assertEquals(false, parsed?.flameDetected)
        assertEquals(27.5, parsed?.temperatureC!!, 0.01)
        assertEquals(63.7, parsed.humidityPercent!!, 0.01)
        assertEquals(1800, parsed.mq6Warning)
        assertEquals(2600, parsed.mq6Danger)
        assertEquals(1800, parsed.mq2Warning)
        assertEquals(2600, parsed.mq2Danger)
        assertEquals(35.0, parsed.temperatureWarning!!, 0.01)
        assertEquals(50.0, parsed.temperatureDanger!!, 0.01)
        assertEquals(64L, parsed.uptimeSeconds)
    }

    @Test
    fun testWarningJsonParsing() {
        val json = """{"device":"KitchenGuard360","state":"WARNING","mq6_raw":1900,"mq2_raw":1200,"flame_detected":false,"temperature_c":36.2,"humidity_percent":55.0,"mq6_warning":1800,"mq6_danger":2600,"mq2_warning":1800,"mq2_danger":2600,"temperature_warning":35.0,"temperature_danger":50.0,"uptime_seconds":120}"""
        val parsed = KitchenStatusParser.parseJson(json)

        assertNotNull(parsed)
        assertEquals("WARNING", parsed?.state)
        assertEquals(1900, parsed?.mq6Raw)
    }

    @Test
    fun testDangerJsonParsing() {
        val json = """{"device":"KitchenGuard360","state":"DANGER","mq6_raw":2700,"mq2_raw":2800,"flame_detected":true,"temperature_c":52.1,"humidity_percent":40.0,"mq6_warning":1800,"mq6_danger":2600,"mq2_warning":1800,"mq2_danger":2600,"temperature_warning":35.0,"temperature_danger":50.0,"uptime_seconds":180}"""
        val parsed = KitchenStatusParser.parseJson(json)

        assertNotNull(parsed)
        assertEquals("DANGER", parsed?.state)
        assertTrue(parsed?.flameDetected == true)
    }

    @Test
    fun testSensorFaultJsonParsing() {
        val json = """{"device":"KitchenGuard360","state":"SENSOR_FAULT","mq6_raw":0,"mq2_raw":0,"flame_detected":false,"temperature_c":null,"humidity_percent":null,"mq6_warning":1800,"mq6_danger":2600,"mq2_warning":1800,"mq2_danger":2600,"temperature_warning":35.0,"temperature_danger":50.0,"uptime_seconds":200}"""
        val parsed = KitchenStatusParser.parseJson(json)

        assertNotNull(parsed)
        assertEquals("SENSOR_FAULT", parsed?.state)
        assertNull(parsed?.temperatureC)
        assertNull(parsed?.humidityPercent)
    }

    @Test
    fun testNullTemperature() {
        val json = """{"device":"KitchenGuard360","state":"NORMAL","mq6_raw":80,"mq2_raw":100,"flame_detected":false,"temperature_c":null,"humidity_percent":50.0}"""
        val parsed = KitchenStatusParser.parseJson(json)

        assertNotNull(parsed)
        assertNull(parsed?.temperatureC)
        assertEquals(50.0, parsed?.humidityPercent!!, 0.01)
    }

    @Test
    fun testNullHumidity() {
        val json = """{"device":"KitchenGuard360","state":"NORMAL","mq6_raw":80,"mq2_raw":100,"flame_detected":false,"temperature_c":25.0,"humidity_percent":null}"""
        val parsed = KitchenStatusParser.parseJson(json)

        assertNotNull(parsed)
        assertEquals(25.0, parsed?.temperatureC!!, 0.01)
        assertNull(parsed.humidityPercent)
    }

    @Test
    fun testMalformedJson() {
        val json = """{"device":"KitchenGuard360", "state":"NORMAL", mq6_raw: invalid json..."""
        val parsed = KitchenStatusParser.parseJson(json)

        assertNull(parsed)
    }

    @Test
    fun testBlankLine() {
        val parsed = KitchenStatusParser.parseJson("    \n\t  ")
        assertNull(parsed)
    }

    @Test
    fun testUnrelatedBluetoothText() {
        val rawText = "AT+NAME=HC-05 or Other BT Module Output"
        val parsed = KitchenStatusParser.parseJson(rawText)

        assertNull(parsed)
    }

    @Test
    fun testPartialMultipleNewlinePackets() {
        val chunk1 = """{"device":"KitchenGuard360","state":"WARMUP","mq6_raw":50,"mq2_raw":60,"flame_detected":false,"temperature_c":22.0,"humidity_percent":45.0}"""
        val parsed = KitchenStatusParser.parseJson(chunk1)

        assertNotNull(parsed)
        assertEquals("WARMUP", parsed?.state)
    }

    @Test
    fun testStateAndColorMapping() {
        fun mapStateToSafetyLevel(stateStr: String?): SafetyLevel = when (stateStr?.uppercase()) {
            "NORMAL" -> SafetyLevel.SAFE
            "WARNING" -> SafetyLevel.WARNING
            "DANGER" -> SafetyLevel.DANGER
            "WARMUP" -> SafetyLevel.WARNING
            "SENSOR_FAULT" -> SafetyLevel.SENSOR_FAULT
            else -> SafetyLevel.OFFLINE
        }

        assertEquals(SafetyLevel.SAFE, mapStateToSafetyLevel("NORMAL"))
        assertEquals(SafetyLevel.WARNING, mapStateToSafetyLevel("WARNING"))
        assertEquals(SafetyLevel.DANGER, mapStateToSafetyLevel("DANGER"))
        assertEquals(SafetyLevel.WARNING, mapStateToSafetyLevel("WARMUP"))
        assertEquals(SafetyLevel.SENSOR_FAULT, mapStateToSafetyLevel("SENSOR_FAULT"))
        assertEquals(SafetyLevel.OFFLINE, mapStateToSafetyLevel(null))
        assertEquals(SafetyLevel.OFFLINE, mapStateToSafetyLevel("UNKNOWN"))
    }
}
