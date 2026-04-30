package com.financialhub.financialhubapi.web;

import com.financialhub.financialhubapi.infrastructure.web.RequestLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestLoggingFilter filter = new RequestLoggingFilter(objectMapper);

    @Test
    void logsInfoForSuccessfulRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.setQueryString("page=1");
        request.addHeader("User-Agent", "test-agent");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void logsWarnForClientError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(400);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void logsErrorForServerError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/summary");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void usesXForwardedForOverRemoteAddr() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void handlesNullQueryString() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/assets/123");
        request.setQueryString(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(204);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void stillLogsWhenChainThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/broken");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        FilterChain chain = mock(FilterChain.class);
        doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException ignored) {
        }

        verify(chain).doFilter(request, response);
    }
}
