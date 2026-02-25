package com.VLmb.ai_tutor_backend.feature.rag.application;

import com.VLmb.ai_tutor_backend.feature.rag.infra.RagRestClient;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogMessagesDto;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.MessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;
import com.VLmb.ai_tutor_backend.feature.dialog.infra.MessageRepository;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.FileInf;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.LoadFileToRagDto;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.MessageRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagCommunicationService {

    private final MessageRepository messageRepository;
    private final RagRestClient ragRestClient;

    public MessageResponse sendQuestionToRag(Long dialogId, Message question) {
        List<DialogMessagesDto> dialogMessages = new ArrayList<>();
        for (Message message: messageRepository.findByDialogId(dialogId)) {
            dialogMessages.add(new DialogMessagesDto(message.getContent(), message.getRole()));
        }

        return ragRestClient.sendMessage(new MessageRequestDto(
                dialogId.toString(),
                dialogMessages,
                question.getContent()
        ));
    }

    public void loadFileToRag(Long dialogId, List<FileInf> files) {
        LoadFileToRagDto request = new LoadFileToRagDto(
                files,
                dialogId.toString()
        );

        ragRestClient.loadFile(request);
    }

}
