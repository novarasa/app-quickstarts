def test_index(client):
    resp = client.get("/")
    assert resp.status_code == 200
    assert "Hello" in resp.get_json()["message"]


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.get_json() == {"status": "ok"}


def test_item_crud(client):
    assert client.get("/api/items").get_json() == []

    resp = client.post("/api/items", json={"name": "widget"})
    assert resp.status_code == 201
    assert resp.get_json() == {"id": 1, "name": "widget"}

    assert client.get("/api/items/1").get_json() == {"id": 1, "name": "widget"}
    assert len(client.get("/api/items").get_json()) == 1

    assert client.delete("/api/items/1").status_code == 204
    assert client.get("/api/items/1").status_code == 404


def test_create_item_requires_name(client):
    assert client.post("/api/items", json={}).status_code == 400
    assert client.post("/api/items", json={"name": "  "}).status_code == 400


def test_unknown_route_returns_json_404(client):
    resp = client.get("/nope")
    assert resp.status_code == 404
    assert resp.get_json() == {"error": "not found"}
