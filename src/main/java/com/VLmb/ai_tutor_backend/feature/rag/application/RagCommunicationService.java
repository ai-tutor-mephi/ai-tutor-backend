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

@Service
@RequiredArgsConstructor
public class RagCommunicationService {

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

    public RagQuizResponse generateQuiz(Long dialogId, Integer questionsCount) {
        List<DialogMessageResponse> dialogMessages = messageRepository.findByDialogIdOrderByCreatedAt(dialogId)
                .stream()
                .map(message -> new DialogMessageResponse(message.getContent(), message.getRole()))
                .toList();

        return ragRestClient.generateQuiz(questionsCount, new RagQuizRequest(
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

}
