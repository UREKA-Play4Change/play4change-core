package com.ureka.play4change.features.home.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingNameTest {

    @Test
    fun `given full display name then returns only first name`() {
        assertEquals("Radesh", greetingName("Radesh Govind"))
    }

    @Test
    fun `given single name then returns that name capitalised`() {
        assertEquals("Alice", greetingName("alice"))
    }

    @Test
    fun `given email address then returns local-part first word only`() {
        assertEquals("Radesh", greetingName("radesh.govind@gmail.com"))
    }

    @Test
    fun `given email with digits in local-part then returns first word only`() {
        assertEquals("Radesh", greetingName("radesh.govind123@x.com"))
    }

    @Test
    fun `given blank name then returns there`() {
        assertEquals("there", greetingName(""))
    }

    @Test
    fun `given name with leading spaces then returns first meaningful word`() {
        assertEquals("Alice", greetingName("  Alice Bob"))
    }
}
