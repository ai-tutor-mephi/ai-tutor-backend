package com.VLmb.ai_tutor_backend.feature.dialog.application.flow;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.CreateDialogResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.application.DialogService;
import com.VLmb.ai_tutor_backend.feature.file.application.DialogFileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Profile("sync")
@RequiredArgsConstructor
public class DialogFlowSyncService implements DialogFlowService {

    private final DialogService dialogService;

    @Override
    public CompletableFuture<CreateDialogResponse> createDialogWithFiles(User user, MultipartFile[] files) {
        try {
            return CompletableFuture.completedFuture(dialogService.createDialogWithFiles(user, files));
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Override
    public CompletableFuture<List<DialogFileResponse>> addFilesToDialog(Long dialogId, User user, MultipartFile[] files) {
        try {
            return CompletableFuture.completedFuture(dialogService.addFilesToDialog(dialogId, user, files));
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    @Override
    public CompletableFuture<SendMessageResponse> sendQuestion(SendMessageRequest request, User user, Long dialogId) {
        try {
            return CompletableFuture.completedFuture(dialogService.sendQuestion(request, user, dialogId));
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
