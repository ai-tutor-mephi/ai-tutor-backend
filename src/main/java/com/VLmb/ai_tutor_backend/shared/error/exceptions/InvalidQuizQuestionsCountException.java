package com.VLmb.ai_tutor_backend.shared.error.exceptions;

public class InvalidQuizQuestionsCountException extends RuntimeException {

    public InvalidQuizQuestionsCountException(int min, int max) {
        super(String.format("Количество вопросов в квизе должно быть от %d до %d.", min, max));
    }
}
