package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagFileRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagRestClientTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RagRestClientImpl ragRestClient;
    private RagQueryRequest ragQueryRequest;
    private RagLoadFilesRequest ragLoadFilesRequest;

    @BeforeEach
    void setUp() {
        ragRestClient = new RagRestClientImpl(restClient, new ObjectMapper());

        ragQueryRequest = new RagQueryRequest(
                "42",
                List.of(new DialogMessageResponse("Hi", Message.MessageRole.USER)),
                "How are you?"
        );

        ragLoadFilesRequest = new RagLoadFilesRequest(
                List.of(new RagFileRequest("file-1", "notes.txt", "lesson text")),
                "42"
        );
    }

    @Test
    void shouldSendMessage() {
        SendMessageResponse expected = new SendMessageResponse("I am fine");
        stubPostChain("/query");
        when(responseSpec.body(SendMessageResponse.class)).thenReturn(expected);

        SendMessageResponse actual = ragRestClient.sendMessage(ragQueryRequest);

        assertSame(expected, actual);
        verify(requestBodySpec).body(ragQueryRequest);
        verify(responseSpec).body(SendMessageResponse.class);
    }

    @Test
    void shouldLoadFile() {
        stubPostChain("/load");
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        ragRestClient.loadFile(ragLoadFilesRequest);

        verify(requestBodySpec).body(ragLoadFilesRequest);
        verify(responseSpec).toBodilessEntity();
    }

    @Test
    void shouldPropagateErrorFromSendMessage() {
        RuntimeException expected = new RuntimeException("upstream failure");
        stubPostChain("/query");
        when(responseSpec.body(SendMessageResponse.class)).thenThrow(expected);

        RuntimeException actual = assertThrows(RuntimeException.class, () -> ragRestClient.sendMessage(ragQueryRequest));

        assertSame(expected, actual);
    }

    private void stubPostChain(String uri) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(uri)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }
}
