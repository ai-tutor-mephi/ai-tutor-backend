package com.VLmb.ai_tutor_backend.service;

import com.VLmb.ai_tutor_backend.client.RagRestClient;
import com.VLmb.ai_tutor_backend.dto.*;
import com.VLmb.ai_tutor_backend.entity.FileMetadata;
import com.VLmb.ai_tutor_backend.entity.Message;
import com.VLmb.ai_tutor_backend.repository.DialogRepository;
import com.VLmb.ai_tutor_backend.repository.FileMetadataRepository;
import com.VLmb.ai_tutor_backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.jni.FileInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagCommunicationService {

    private final DialogRepository dialogRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final MessageRepository messageRepository;
    private final RagRestClient ragRestClient;
    private final PdfParsingService pdfParsingService;

    public MessageResponse sendQuestionToRag(Long dialogId, Message question) {
        List<DialogMessagesDto> dialogMessages = new ArrayList<>();
        for (Message message: messageRepository.findByDialogId(dialogId)) {
            dialogMessages.add(new DialogMessagesDto(message.getContent(), message.getRole()));
        }

        return ragRestClient.sendMessage(new MessageRequestDto(
                dialogId,
                dialogMessages,
                question.getContent()
        ));
    }

    public void loadFileToRag(Long dialogId, List<FileInf> files) {
        LoadFileToRagDto request = new LoadFileToRagDto(
                files,
                dialogId
        );

        ragRestClient.loadFile(request);
    }

}
