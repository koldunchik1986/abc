package com.koldunchik1986.ANL.core.filter

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import com.koldunchik1986.ANL.data.model.UserProfile

/**
 * Тесты для PostFilter системы
 */
class HttpFilterTest {

    @Mock
    private lateinit var userProfile: UserProfile
    
    @Mock
    private lateinit var jsFilters: JavaScriptFilters
    
    @Mock
    private lateinit var phpFilters: PhpFilters
    
    @Mock
    private lateinit var htmlFilters: HtmlFilters
    
    private lateinit var httpFilter: HttpFilter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        httpFilter = HttpFilter(userProfile, jsFilters, phpFilters, htmlFilters)
    }

    @Test
    fun `test JavaScript filter processing`() = runBlocking {
        // Arrange
        val jsContent = "function test() { return 'test'; }"
        val jsBytes = jsContent.toByteArray(charset("windows-1251"))
        val modifiedJsContent = "// ABClient modified\n$jsContent"
        val modifiedJsBytes = modifiedJsContent.toByteArray(charset("windows-1251"))
        
        `when`(jsFilters.processMapJs(jsBytes)).thenReturn(modifiedJsBytes)
        
        val request = Request.Builder()
            .url("http://www.neverlands.ru/js/map.js")
            .build()
            
        val originalResponse = createMockResponse(request, jsBytes, "text/javascript")
        
        // Mock the chain
        val chain = mock(okhttp3.Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(originalResponse)
        
        // Act
        val filteredResponse = httpFilter.intercept(chain)
        
        // Assert
        val responseBody = filteredResponse.body
        assertNotNull(responseBody)
        
        val responseContent = responseBody!!.string()
        assertTrue("Response should contain ABClient modification", 
                  responseContent.contains("ABClient modified"))
    }

    @Test
    fun `test PHP filter processing`() = runBlocking {
        // Arrange
        val phpContent = "<html><body>Test main.php content</body></html>"
        val phpBytes = phpContent.toByteArray(charset("windows-1251"))
        val modifiedPhpContent = "<html><body>Modified Test main.php content</body></html>"
        val modifiedPhpBytes = modifiedPhpContent.toByteArray(charset("windows-1251"))
        
        `when`(phpFilters.processMainPhp(anyString(), eq(phpBytes))).thenReturn(modifiedPhpBytes)
        
        val request = Request.Builder()
            .url("http://www.neverlands.ru/main.php?get_id=1")
            .build()
            
        val originalResponse = createMockResponse(request, phpBytes, "text/html")
        
        val chain = mock(okhttp3.Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(originalResponse)
        
        // Act
        val filteredResponse = httpFilter.intercept(chain)
        
        // Assert
        val responseBody = filteredResponse.body
        assertNotNull(responseBody)
        
        val responseContent = responseBody!!.string()
        assertTrue("Response should be modified", 
                  responseContent.contains("Modified"))
    }

    @Test
    fun `test non-neverlands URL bypass`() = runBlocking {
        // Arrange
        val content = "External content"
        val contentBytes = content.toByteArray()
        
        val request = Request.Builder()
            .url("https://google.com/")
            .build()
            
        val originalResponse = createMockResponse(request, contentBytes, "text/html")
        
        val chain = mock(okhttp3.Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(originalResponse)
        
        // Act
        val filteredResponse = httpFilter.intercept(chain)
        
        // Assert
        val responseBody = filteredResponse.body
        assertNotNull(responseBody)
        
        val responseContent = responseBody!!.string()
        assertEquals("External content should not be modified", content, responseContent)
        
        // Verify filters were not called
        verifyNoInteractions(jsFilters)
        verifyNoInteractions(phpFilters)
        verifyNoInteractions(htmlFilters)
    }

    @Test
    fun `test HTML filter processing`() = runBlocking {
        // Arrange
        val htmlContent = "<!DOCTYPE html><html><body>Test</body></html>"
        val htmlBytes = htmlContent.toByteArray(charset("windows-1251"))
        val modifiedHtmlBytes = htmlContent.replace("<!DOCTYPE html>", "")
            .toByteArray(charset("windows-1251"))
        
        `when`(htmlFilters.processPlayerInfo(htmlBytes)).thenReturn(modifiedHtmlBytes)
        
        val request = Request.Builder()
            .url("http://www.neverlands.ru/pinfo.cgi?Player")
            .build()
            
        val originalResponse = createMockResponse(request, htmlBytes, "text/html")
        
        val chain = mock(okhttp3.Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(originalResponse)
        
        // Act
        val filteredResponse = httpFilter.intercept(chain)
        
        // Assert
        val responseBody = filteredResponse.body
        assertNotNull(responseBody)
        
        val responseContent = responseBody!!.string()
        assertFalse("DOCTYPE should be removed", 
                   responseContent.contains("<!DOCTYPE html>"))
    }

    @Test
    fun `test trading filter when enabled`() = runBlocking {
        // Arrange
        val tradeContent = """<font color=#cc0000>Купить вещь у TestPlayer за 100NV?</font>"""
        val tradeBytes = tradeContent.toByteArray(charset("windows-1251"))
        val modifiedTradeBytes = "Modified trade content".toByteArray(charset("windows-1251"))
        
        `when`(userProfile.torgActive).thenReturn(true)
        `when`(phpFilters.processTradePhp(tradeBytes)).thenReturn(modifiedTradeBytes)
        
        val request = Request.Builder()
            .url("http://www.neverlands.ru/gameplay/trade.php")
            .build()
            
        val originalResponse = createMockResponse(request, tradeBytes, "text/html")
        
        val chain = mock(okhttp3.Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(originalResponse)
        
        // Act
        val filteredResponse = httpFilter.intercept(chain)
        
        // Assert
        verify(phpFilters).processTradePhp(tradeBytes)
    }

    @Test
    fun `test trading filter when disabled`() = runBlocking {
        // Arrange
        val tradeContent = """<font color=#cc0000>Купить вещь у TestPlayer за 100NV?</font>"""
        val tradeBytes = tradeContent.toByteArray(charset("windows-1251"))
        
        `when`(userProfile.torgActive).thenReturn(false)
        
        val request = Request.Builder()
            .url("http://www.neverlands.ru/gameplay/trade.php")
            .build()
            
        val originalResponse = createMockResponse(request, tradeBytes, "text/html")
        
        val chain = mock(okhttp3.Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(originalResponse)
        
        // Act
        val filteredResponse = httpFilter.intercept(chain)
        
        // Assert
        verify(phpFilters, never()).processTradePhp(any())
        
        val responseContent = filteredResponse.body!!.string()
        assertEquals("Content should not be modified when trading disabled", 
                    tradeContent, responseContent)
    }

    @Test
    fun `test error handling in filter`() = runBlocking {
        // Arrange
        val content = "Test content"
        val contentBytes = content.toByteArray()
        
        val request = Request.Builder()
            .url("http://www.neverlands.ru/main.php")
            .build()
            
        val originalResponse = createMockResponse(request, contentBytes, "text/html")
        
        `when`(phpFilters.processMainPhp(anyString(), any())).thenThrow(RuntimeException("Test error"))
        
        val chain = mock(okhttp3.Interceptor.Chain::class.java)
        `when`(chain.request()).thenReturn(request)
        `when`(chain.proceed(request)).thenReturn(originalResponse)
        
        // Act
        val filteredResponse = httpFilter.intercept(chain)
        
        // Assert - should not throw exception and return original response
        assertNotNull(filteredResponse)
        val responseContent = filteredResponse.body!!.string()
        assertEquals("Should return original content on error", content, responseContent)
    }

    /**
     * Создает мок ответа для тестов
     */
    private fun createMockResponse(
        request: Request, 
        content: ByteArray, 
        contentType: String
    ): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(content.toResponseBody(contentType.toMediaType()))
            .build()
    }
}