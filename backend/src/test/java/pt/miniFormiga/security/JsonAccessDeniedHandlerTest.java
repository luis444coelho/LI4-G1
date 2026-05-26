package pt.miniFormiga.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonAccessDeniedHandlerTest {

    @Test
    void handleEscreveRespostaJsonComPath() throws Exception {
        JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("sem permissao"));

        assertEquals(403, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertTrue(response.getContentAsString().contains("\"code\":\"ACCESS_DENIED\""));
        assertTrue(response.getContentAsString().contains("\"path\":\"/api/v1/admin\""));
    }
}
