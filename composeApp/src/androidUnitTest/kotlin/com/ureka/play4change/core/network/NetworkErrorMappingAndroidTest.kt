package com.ureka.play4change.core.network

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkErrorMappingAndroidTest {

    @Test
    fun `IOException maps to NoConnection`() {
        val e = IOException("Connection refused")
        assertEquals(NetworkError.NoConnection, e.toNetworkError())
    }
}
