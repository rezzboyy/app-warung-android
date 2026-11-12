package com.warung.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceTest {

    @Test
    fun testResourceSuccess() {
        val data = "Test Data"
        val resource: Resource<String> = Resource.Success(data)

        assertTrue(resource is Resource.Success)
        assertEquals(data, (resource as Resource.Success).data)
    }

    @Test
    fun testResourceError() {
        val errorMessage = "Something went wrong"
        val resource: Resource<String> = Resource.Error(errorMessage)

        assertTrue(resource is Resource.Error)
        assertEquals(errorMessage, (resource as Resource.Error).message)
    }

    @Test
    fun testResourceLoading() {
        val resource: Resource<String> = Resource.Loading
        assertTrue(resource is Resource.Loading)
    }
}
