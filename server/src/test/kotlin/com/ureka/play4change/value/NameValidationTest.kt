package com.ureka.play4change.value

import com.ureka.play4change.result.Result
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NameValidationTest {

    @Test
    fun `given null name when create called then returns Failure`() {
        val result = Name.create(null)
        assertTrue(result is Result.Failure, "Expected Failure for null input")
    }

    @Test
    fun `given blank name when create called then returns Failure`() {
        val result = Name.create("   ")
        assertTrue(result is Result.Failure, "Expected Failure for blank name")
    }

    @Test
    fun `given name shorter than 2 characters when create called then returns Failure`() {
        val result = Name.create("A")
        assertTrue(result is Result.Failure, "Expected Failure for single-character name")
    }

    @Test
    fun `given name longer than 100 characters when create called then returns Failure`() {
        val longName = "A".repeat(101)
        val result = Name.create(longName)
        assertTrue(result is Result.Failure, "Expected Failure for name exceeding 100 characters")
    }

    @Test
    fun `given name containing control characters when create called then returns Failure`() {
        val result = Name.create("Alice\u0000Bob")
        assertTrue(result is Result.Failure, "Expected Failure for name with control character")
    }

    @Test
    fun `given valid name when create called then returns Success with correct value`() {
        val result = Name.create("Alice")
        assertTrue(result is Result.Success, "Expected Success for valid name")
        assertTrue((result as Result.Success).data.value == "Alice")
    }

    @Test
    fun `given name of exactly 2 characters when create called then returns Success`() {
        val result = Name.create("Al")
        assertTrue(result is Result.Success, "Expected Success for 2-character name")
    }

    @Test
    fun `given name of exactly 100 characters when create called then returns Success`() {
        val name = "A".repeat(100)
        val result = Name.create(name)
        assertTrue(result is Result.Success, "Expected Success for 100-character name")
    }
}
