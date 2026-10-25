package edu.utesa.directorio

import edu.utesa.directorio.data.DirectoryApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class ApiTest {
    @Test fun mapsCompanyAndUsesGet() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""[{"id":1,"name":"Ana","email":"ana@example.com","company":{"name":"Empresa"}}]"""))
            val users = DirectoryApi.create(server.url("/").toString()).users()
            assertEquals("Empresa", users.single().company.name)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/users", request.path)
        } finally { server.shutdown() }
    }
    @Test fun filtersPostsByUserId() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setBody("""[{"userId":7,"id":1,"title":"Título","body":"Texto"}]"""))
            val posts = DirectoryApi.create(server.url("/").toString()).posts(7)
            assertEquals(7, posts.single().userId)
            assertEquals("/posts?userId=7", server.takeRequest().path)
        } finally { server.shutdown() }
    }
    @Test fun exposesHttpError() = runBlocking {
        val server = MockWebServer(); server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(500))
            try { DirectoryApi.create(server.url("/").toString()).users(); fail("Debe fallar") }
            catch (e: HttpException) { assertEquals(500, e.code()) }
        } finally { server.shutdown() }
    }
}
