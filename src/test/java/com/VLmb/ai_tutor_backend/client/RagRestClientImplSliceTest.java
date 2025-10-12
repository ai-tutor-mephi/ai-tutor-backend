//package com.VLmb.ai_tutor_backend.client;
//
//import com.VLmb.ai_tutor_backend.client.config.RagProperties;
//import com.VLmb.ai_tutor_backend.client.config.RagRestClientConfig;
//import com.VLmb.ai_tutor_backend.dto.MessageResponse;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.client.MockRestServiceServer;
//
//import java.time.Duration;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.client.ExpectedCount.once;
//import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
//import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
//
//@Disabled("временно отключено, не участвует в прогоне тестов")
//@RestClientTest(RagRestClientImpl.class)
//@Import(RagRestClientConfig.class)
//public class RagRestClientImplSliceTest {
//
//    @Autowired
//    private MockRestServiceServer server;
//
//    @Autowired
//    private RagRestClient client;
//
//    @MockitoBean
//    private RagProperties ragProperties;
//
//    @Test
//    void current_whenServerReturns200_shouldReturnCorrectResponse() throws Exception {
//        String baseUrl = "http://fake-rag-api.com";
//        when(ragProperties.baseUrl()).thenReturn(baseUrl);
//        when(ragProperties.connectTimeout()).thenReturn(Duration.ofMillis(1000));
//        when(ragProperties.responseTimeout()).thenReturn(Duration.ofMillis(50000));
//        when(ragProperties.logPayloads()).thenReturn(true);
//
//        String expectedJsonResponse = """
//            {
//                "message": "This is a response from RAG"
//            }
//            """;
//
//        server.expect(
//                        once(),
//                        requestTo(baseUrl + "/v1/current"))
//                .andExpect(method(HttpMethod.POST))
//                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
//                .andExpect(jsonPath("$.message").value("Hello RAG"))
//                .andRespond(
//                        withSuccess(expectedJsonResponse, MediaType.APPLICATION_JSON));
//
//
//        MessageResponse response = client.current("Hello RAG");
//
//
//        assertNotNull(response);
//        assertEquals("This is a response from RAG", response.answer());
//
//        server.verify();
//    }
//}
