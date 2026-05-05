from __future__ import annotations

import os
import random
from pathlib import Path

from locust import HttpUser, task, between


BASE_DIR = Path(__file__).resolve().parent
FILES_DIR = Path(os.getenv("LOCUST_FILES_DIR", BASE_DIR / "files"))
USERNAME = os.getenv("LOCUST_USERNAME", "test-user")
EMAIL = os.getenv("LOCUST_EMAIL", "test-user@example.com")
PASSWORD = os.getenv("LOCUST_PASSWORD", "password1234")
DIALOG_QUESTION = os.getenv("LOCUST_DIALOG_QUESTION", "Расскажи о себе")
DIALOG_TITLE = os.getenv("LOCUST_DIALOG_TITLE", "locust-dialog")


class DialogUser(HttpUser):
    wait_time = between(1, 2)

    def on_start(self):
        self.token = None
        self.dialog_id = None
        self._register_and_login()

    def _register_and_login(self):

        current_username = f"{USERNAME}-{random.randint(1000, 999999)}"
        current_email = f"{EMAIL.split('@')[0]}+{random.randint(1000, 999999)}@{EMAIL.split('@')[1]}"

        # register
        self.client.post(
            "/api/auth/register",
            json={
                "userName": current_username,
                "email": current_email,
                "password": PASSWORD,
            },
            name="auth_register",
        )

        # login (используем те же сгенерированные данные!)
        response = self.client.post(
            "/api/auth/login",
            json={"userName": current_username, "password": PASSWORD},
            name="auth_login",
        )
        if response.status_code == 200:
            self.token = response.json().get("access_token") or response.json().get("accessToken")

    def _auth_headers(self):
        if not self.token:
            return {}
        return {"Authorization": f"Bearer {self.token}"}

    def _pick_file(self):
        if not FILES_DIR.exists():
            return None
        files = [p for p in FILES_DIR.iterdir() if p.is_file()]
        return random.choice(files) if files else None

    @task(2)
    def create_dialog_with_file(self):
        file_path = self._pick_file()
        if not file_path:
            return
        with file_path.open("rb") as f:
            files = {"files": (file_path.name, f, "application/octet-stream")}
            response = self.client.post(
                "/api/dialogs/with-files",
                files=files,
                headers=self._auth_headers(),
                name="dialogs_with_files",
            )
            if response.status_code == 201:
                body = response.json()
                self.dialog_id = body.get("dialogId")

    @task(2)
    def add_files_to_dialog(self):
        if not self.dialog_id:
            return
        file_path = self._pick_file()
        if not file_path:
            return
        with file_path.open("rb") as f:
            files = {"files": (file_path.name, f, "application/octet-stream")}
            self.client.post(
                f"/api/dialogs/{self.dialog_id}/files",
                files=files,
                headers=self._auth_headers(),
                name="dialogs_add_files",
            )

    @task(4)
    def send_question(self):
        if not self.dialog_id:
            return
        self.client.post(
            f"/api/dialogs/{self.dialog_id}/send-question",
            json={"question": DIALOG_QUESTION},
            headers=self._auth_headers(),
            name="dialogs_send_question",
        )
