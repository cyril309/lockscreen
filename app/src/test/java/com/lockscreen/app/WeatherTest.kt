package com.lockscreen.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTest {
    @Test
    fun mapsClearAndCloudyConditions() {
        assertEquals("Clear", describeWeatherCode(0))
        assertEquals("Partly cloudy", describeWeatherCode(2))
        assertEquals("Cloudy", describeWeatherCode(3))
    }

    @Test
    fun mapsPrecipitationFogAndStorms() {
        assertEquals("Fog", describeWeatherCode(45))
        assertEquals("Rain", describeWeatherCode(61))
        assertEquals("Snow", describeWeatherCode(73))
        assertEquals("Showers", describeWeatherCode(81))
        assertEquals("Storm", describeWeatherCode(95))
    }

    @Test
    fun returnsEmptyLabelForUnknownCode() {
        assertEquals("", describeWeatherCode(-1))
        assertEquals("", describeWeatherCode(100))
    }
}
