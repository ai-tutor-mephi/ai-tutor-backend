//package com.VLmb.ai_tutor_backend.client;
//
//import com.VLmb.ai_tutor_backend.dto.MessageRequest;
//import com.VLmb.ai_tutor_backend.dto.MessageResponse;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.MediaType;
//import org.springframework.web.client.RestClient;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@Disabled("временно отключено, не участвует в прогоне тестов")
//@ExtendWith(MockitoExtension.class)
//class RagRestClientImplUnitTest {
//
//    @Mock
//    private RestClient mockRestClient;
//
//    @InjectMocks
//    private RagRestClientImpl ragRestClient;
//
//    @Captor
//    private ArgumentCaptor<MessageRequest> requestCaptor;
//
//    // ... внутри класса RagRestClientImplTest ...
//
//    @Test
//    void current_whenCalledWithMessage_shouldBuildRequestAndReturnResponse() {
//
//        String inputMessage = "Tell me about Mockito";
//        MessageResponse expectedResponse = new MessageResponse("Mockito is a mocking framework!");
//
//
//        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
//        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
//        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
//
//        when(mockRestClient.post()).thenReturn(requestBodyUriSpec);
//        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
//        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
//        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
//        when(requestBodySpec.body(any(MessageRequest.class))).thenReturn(requestBodySpec);
//        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
//        when(responseSpec.body(MessageResponse.class)).thenReturn(expectedResponse);
//
//        MessageResponse actualResponse = ragRestClient.current(inputMessage);
//
//        assertNotNull(actualResponse);
//        assertEquals(expectedResponse, actualResponse);
//
//        verify(mockRestClient).post();
//        verify(requestBodyUriSpec).uri("/v1/current");
//        verify(requestBodySpec).body(requestCaptor.capture());
//
//        MessageRequest capturedRequest = requestCaptor.getValue();
//        assertEquals(inputMessage, capturedRequest.question());
//    }
//}