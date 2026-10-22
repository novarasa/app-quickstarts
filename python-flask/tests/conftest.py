import pytest

from app import create_app
from app.routes import reset_store


@pytest.fixture
def client():
    reset_store()
    app = create_app("testing")
    with app.test_client() as client:
        yield client
