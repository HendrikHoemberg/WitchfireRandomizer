package dev.hendrikhoemberg.witchfirerandomizer.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private Logger filterLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        filterLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        filterLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        filterLogger.detachAppender(listAppender);
    }

    @Test
    void testDynamicRequestIsLoggedWithDetails() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wiki");
        request.setQueryString("category=WEAPON");
        request.setRemoteAddr("203.0.113.195");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, new MockFilterChain());

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        ILoggingEvent event = logs.get(0);
        assertEquals(Level.INFO, event.getLevel());
        String msg = event.getFormattedMessage();
        assertTrue(msg.contains("GET /wiki?category=WEAPON"));
        assertTrue(msg.contains("200"));
        assertTrue(msg.contains("203.0.113.195"));
        assertTrue(msg.matches(".*\\(\\d+ms\\).*"));
    }

    @Test
    void testStaticAssetsAreNotLogged() throws ServletException, IOException {
        String[] staticPaths = {
                "/images/items/b-acute-ailment-bead.webp",
                "/images/favicon.png",
                "/css/main.css",
                "/favicon.ico",
                "/robots.txt"
        };

        for (String path : staticPaths) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
        }

        assertTrue(listAppender.list.isEmpty(), "Expected no logs for static asset requests");
    }

    @Test
    void test4xxStatusIsLoggedAtWarnLevel() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wiki/item/non-existent");
        request.setRemoteAddr("198.51.100.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(404);
        filter.doFilter(request, response, chain);

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        ILoggingEvent event = logs.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("404"));
        assertTrue(event.getFormattedMessage().contains("/wiki/item/non-existent"));
    }

    @Test
    void test5xxStatusIsLoggedAtErrorLevel() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/randomizer/reroll");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(500);
        filter.doFilter(request, response, chain);

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        ILoggingEvent event = logs.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("500"));
        assertTrue(event.getFormattedMessage().contains("/randomizer/reroll"));
    }
}
