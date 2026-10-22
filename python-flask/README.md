# Python Flask QuickStart

A minimal, production-ready starting point for a Flask 3 JSON API.

## Features

- Application factory (`create_app`) with a blueprint
- Environment-based config (`development`, `testing`, `production`) via `APP_ENV`
- Sample in-memory CRUD API for `/api/items`
- JSON 404 handler and `/health` endpoint
- pytest test suite
- Dockerfile running under gunicorn as a non-root user

## Project layout

```
python-flask/
├── app/
│   ├── __init__.py      # create_app() factory
│   ├── config.py        # config classes
│   └── routes.py        # blueprint with endpoints
├── tests/               # pytest suite
├── wsgi.py              # WSGI entry point
├── requirements.txt
├── requirements-dev.txt
└── Dockerfile
```

## Getting started

Requires Python 3.10+.

```bash
cd python-flask
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements-dev.txt
cp .env.example .env             # optional

flask --app wsgi run --debug
```

The app is served at http://127.0.0.1:5000.

## Endpoints

| Method | Path                    | Description         |
| ------ | ----------------------- | ------------------- |
| GET    | `/`                     | Hello message       |
| GET    | `/health`               | Health check        |
| GET    | `/api/items`            | List items          |
| POST   | `/api/items`            | Create `{"name"}`   |
| GET    | `/api/items/<id>`       | Get an item         |
| DELETE | `/api/items/<id>`       | Delete an item      |

```bash
curl -X POST localhost:5000/api/items -H 'Content-Type: application/json' -d '{"name":"widget"}'
curl localhost:5000/api/items
```

## Tests

```bash
pytest
```

## Docker

```bash
docker build -t python-flask-quickstart .
docker run -p 8000:8000 -e SECRET_KEY=change-me python-flask-quickstart
```

## Configuration

| Variable     | Default                   | Notes                                     |
| ------------ | ------------------------- | ----------------------------------------- |
| `APP_ENV`    | `development`             | `development`, `testing`, or `production` |
| `APP_NAME`   | `python-flask-quickstart` | Shown in the `/` response                 |
| `SECRET_KEY` | `dev-secret-change-me`    | **Set this in production**                |
