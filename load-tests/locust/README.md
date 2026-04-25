# Locust Load Tests

## Install
```bash
pip install -r requirements.txt
```

## Run (local)
```bash
locust -f locustfile.py --host http://localhost:8080
```

## Run (headless)
```bash
locust -f locustfile.py --host http://localhost:8080 \
  --headless -u 50 -r 5 -t 5m
```

## Environment variables
- `LOCUST_USERNAME` (default: test-user)
- `LOCUST_EMAIL` (default: test-user@example.com)
- `LOCUST_PASSWORD` (default: password1234)
- `LOCUST_DIALOG_QUESTION` (default: "Расскажи о себе")
- `LOCUST_FILES_DIR` (default: ./files)
- `LOCUST_DIALOG_TITLE` (default: "locust-dialog")
