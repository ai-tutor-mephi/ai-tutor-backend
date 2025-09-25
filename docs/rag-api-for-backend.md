### `localhost:8000/rag/load-files`

- Тело:
```json
  {
  "content": [
              {
              "fileId": "идентификатор_документа", 
              "fileName": "...", 
              "text": "..."
              }
  ],
  "dialogId": "идентификатор_диалога"
}
```

- Ответ (успех): 200 OK
 
- Ошибки: 500, 400

### `localhost:8000/rag/user-question

- Тело:
```json
  {
  "dialogId": "идентификатор_диалога",
  "dialogMessages": [
           {
           "message": "...", 
           "role": "..."
           }
  ],
  "question": "..."
}
```

- Ответ:
  ```json
  {
  "answer": "..."
  }
  ```

- Ошибки: 
