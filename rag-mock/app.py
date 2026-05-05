from __future__ import annotations

import os
import time
from typing import Any, Dict, List, Optional

from fastapi import FastAPI
from pydantic import BaseModel
from dotenv import load_dotenv
load_dotenv()

class RagFileRequest(BaseModel):
    fileId: Optional[str] = None
    fileName: Optional[str] = None
    text: Optional[str] = None


class RagLoadFilesRequest(BaseModel):
    content: Optional[List[RagFileRequest]] = None
    dialogId: Optional[str] = None


class RagQueryMessage(BaseModel):
    message: Optional[str] = None
    role: Optional[str] = None


class RagQueryRequest(BaseModel):
    dialogId: Optional[str] = None
    dialogMessages: Optional[List[RagQueryMessage]] = None
    question: Optional[str] = None


class RagAnswerResponse(BaseModel):
    answer: str


def _delay_seconds(endpoint_name: str) -> float:
    specific_env_var = f"RAG_MOCK_DELAY_SECONDS_{endpoint_name.upper()}"
    delay_str = os.getenv(specific_env_var, os.getenv("RAG_MOCK_DELAY_SECONDS", "10"))

    try:
        return float(delay_str)
    except ValueError:
        return 10.0


def _sleep_if_needed(endpoint_name: str) -> None:
    delay = _delay_seconds(endpoint_name)
    if delay > 0:
        time.sleep(delay)


app = FastAPI(title="RAG Mock", version="0.1.0")


@app.post("/load")
def load_files(payload: RagLoadFilesRequest) -> Dict[str, Any]:
    _sleep_if_needed("load")
    return {"status": "ok", "dialogId": payload.dialogId, "files": len(payload.content or [])}


@app.post("/query", response_model=RagAnswerResponse)
def query(payload: RagQueryRequest) -> RagAnswerResponse:
    _sleep_if_needed("query")
    answer = os.getenv("RAG_MOCK_ANSWER", "Mocked answer")
    return RagAnswerResponse(answer=answer)


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}