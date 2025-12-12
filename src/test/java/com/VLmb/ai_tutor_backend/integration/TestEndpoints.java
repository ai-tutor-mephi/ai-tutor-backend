package com.VLmb.ai_tutor_backend.integration;

/**
 * Shared endpoint paths used across integration tests to avoid scattering literals.
 */
public final class TestEndpoints {
    private TestEndpoints() {}

    public static final String AUTH_REGISTER = "/api/auth/register";
    public static final String AUTH_LOGIN = "/api/auth/login";
    public static final String AUTH_REFRESH = "/api/auth/refresh";

    public static final String USER_CHANGE_USERNAME = "/api/user/change-username";

    public static final String DIALOGS_WITH_FILES = "/api/dialogs/with-files";
    public static final String DIALOGS = "/api/dialogs";
    public static final String DIALOG_FILES = "/api/dialogs/%d/files";
    public static final String DIALOG_CHANGE_TITLE = "/api/dialogs/%d/change-title";
    public static final String DIALOG_DELETE = "/api/dialogs/%d";
    public static final String DIALOG_SEND_QUESTION = "/api/dialogs/%d/send-question";

    public static final String RAG_LOAD = "/load";
    public static final String RAG_QUERY = "/query";
}
