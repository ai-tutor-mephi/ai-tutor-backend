package com.VLmb.ai_tutor_backend.feature.rag.application;

import com.VLmb.ai_tutor_backend.feature.rag.infra.RagRestClient;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagFileRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RagCommunicationService {

    private final MessageRepository messageRepository;
    private final RagRestClient ragRestClient;

    public SendMessageResponse sendQuestionToRag(Long dialogId, Message question) {
        List<DialogMessageResponse> dialogMessages = new ArrayList<>();
        for (Message message: messageRepository.findByDialogId(dialogId)) {
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
        for (Message message : messageRepository.findByDialogId(dialogId)) {
            dialogMessages.add(new DialogMessageResponse(message.getContent(), message.getRole()));
        }

        RagQueryRequest request = new RagQueryRequest(
                dialogId.toString(),
                dialogMessages,
                question.getContent()
        );

        return ragRestClient.sendMessageAsync(request).orTimeout(10, TimeUnit.SECONDS);
    }

    public void loadFileToRag(Long dialogId, List<RagFileRequest> files) {
        RagLoadFilesRequest request = new RagLoadFilesRequest(
                files,
                dialogId.toString()
        );

        ragRestClient.loadFile(request);
    }

}
