package com.VLmb.ai_tutor_backend.feature.rag.application;

import com.VLmb.ai_tutor_backend.feature.rag.infra.RagRestClient;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagFileRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RagCommunicationService {

    public static final int QUERY_TIMEOUT = 30;
    private final Integer MESSAGE_COUNT = 20;

    private final MessageRepository messageRepository;
    private final RagRestClient ragRestClient;

    public SendMessageResponse sendQuestionToRag(Long dialogId, Message question) {
        List<DialogMessageResponse> dialogMessages = new ArrayList<>();
        for (Message message: messageRepository.findByDialogIdOrderByCreatedAtDesc(
                dialogId,
                PageRequest.of(0, MESSAGE_COUNT)
                )) {
            dialogMessages.add(new DialogMessageResponse(message.getContent(), message.getRole()));
        }

        return ragRestClient.sendMessage(new RagQueryRequest(
                dialogId.toString(),
                dialogMessages,
                question.getContent()
        ));
    }

    public CompletableFuture<SendMessageResponse> sendQuestionToRagAsync(Long dialogId, Message question) {
        List<DialogMessageResponse> dialogMessages = new ArrayList<>();
        for (Message message: messageRepository.findByDialogIdOrderByCreatedAtDesc(
                dialogId,
                PageRequest.of(0, MESSAGE_COUNT)
        )) {
            dialogMessages.add(new DialogMessageResponse(message.getContent(), message.getRole()));
        }

        RagQueryRequest request = new RagQueryRequest(
                dialogId.toString(),
                dialogMessages,
                question.getContent()
        );

        return ragRestClient.sendMessageAsync(request).orTimeout(QUERY_TIMEOUT, TimeUnit.SECONDS);
    }

    public RagQuizResponse generateQuiz(Long dialogId) {
        List<DialogMessageResponse> dialogMessages = messageRepository.findByDialogIdOrderByCreatedAt(dialogId)
                .stream()
                .map(message -> new DialogMessageResponse(message.getContent(), message.getRole()))
                .toList();

        return ragRestClient.generateQuiz(new RagQuizRequest(
                dialogId.toString(),
                dialogMessages
        ));
    }

    public CompletableFuture<RagQuizResponse> generateQuizAsync(Long dialogId) {
        List<DialogMessageResponse> dialogMessages = messageRepository.findByDialogIdOrderByCreatedAt(dialogId)
                .stream()
                .map(message -> new DialogMessageResponse(message.getContent(), message.getRole()))
                .toList();

        return ragRestClient.generateQuizAsync(new RagQuizRequest(
                dialogId.toString(),
                dialogMessages
        ));
    }

    public void loadFileToRag(Long dialogId, List<RagFileRequest> files) {
        RagLoadFilesRequest request = new RagLoadFilesRequest(
                files,
                dialogId.toString()
        );

        ragRestClient.loadFile(request);
    }

    public CompletableFuture<Void> loadFileToRagAsync(Long dialogId, List<RagFileRequest> files) {
        RagLoadFilesRequest request = new RagLoadFilesRequest(
                files,
                dialogId.toString()
        );

        return ragRestClient.loadFileAsync(request);
    }

}
