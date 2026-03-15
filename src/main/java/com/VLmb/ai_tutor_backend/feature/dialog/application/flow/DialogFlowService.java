package com.VLmb.ai_tutor_backend.feature.dialog.application.flow;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.file.application.DialogFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DialogFlowService {

    CompletableFuture<CreateDialogResponse> createDialogWithFiles(User user, MultipartFile[] files);

    CompletableFuture<List<DialogFileResponse>> addFilesToDialog(Long dialogId, User user, MultipartFile[] files);

    CompletableFuture<SendMessageResponse> sendQuestion(SendMessageRequest request, User user, Long dialogId);
}
