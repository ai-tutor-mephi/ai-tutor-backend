package com.VLmb.ai_tutor_backend.controller;

import com.VLmb.ai_tutor_backend.dto.DialogInfo;
import com.VLmb.ai_tutor_backend.dto.DialogResponse;
import com.VLmb.ai_tutor_backend.dto.FileResponse;
import com.VLmb.ai_tutor_backend.entity.User;
import com.VLmb.ai_tutor_backend.repository.UserRepository;
import com.VLmb.ai_tutor_backend.service.CustomUserDetails;
import com.VLmb.ai_tutor_backend.service.DialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/dialogs")
@RequiredArgsConstructor
public class DialogController {

    private final DialogService dialogService;
    private final UserRepository userRepository;

    @PostMapping(path = "/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DialogResponse> createDialogWithFile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) throws IOException {

        User user = userRepository.findByUserName(principal.getUsername()).orElseThrow();
        DialogResponse response = dialogService.createDialogWithFile(user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/{dialogId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> addFileToDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) throws IOException {

        User user = userRepository.findByUserName(principal.getUsername()).orElseThrow();
        FileResponse response = dialogService.addFileToDialog(dialogId, user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DialogInfo>> getAllDialogs(@AuthenticationPrincipal UserDetails principal) {

        User currentUser = userRepository.findByUserName(principal.getUsername()).orElseThrow();

        List<DialogInfo> dialogs = dialogService.getAllDialogsForUser(currentUser);
        return ResponseEntity.ok(dialogs);
    }

    @DeleteMapping("/{dialogId}")
    public ResponseEntity<Void> deleteDialog(
            @PathVariable Long dialogId,
            @AuthenticationPrincipal UserDetails principal) {

        User currentUser = userRepository.findByUserName(principal.getUsername()).orElseThrow();

        try {
            dialogService.deleteDialog(dialogId, currentUser);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

}
