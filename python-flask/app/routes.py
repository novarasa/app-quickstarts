"""HTTP routes for the quickstart app."""

from flask import Blueprint, current_app, jsonify, request

bp = Blueprint("main", __name__)

# In-memory store to keep the quickstart dependency-free.
_items = {}
_next_id = 1


@bp.get("/")
def index():
    return jsonify(message=f"Hello from {current_app.config['APP_NAME']}!")


@bp.get("/health")
def health():
    return jsonify(status="ok")


@bp.get("/api/items")
def list_items():
    return jsonify(list(_items.values()))


@bp.post("/api/items")
def create_item():
    global _next_id
    data = request.get_json(silent=True) or {}
    name = data.get("name")
    if not isinstance(name, str) or not name.strip():
        return jsonify(error="'name' is required"), 400
    item = {"id": _next_id, "name": name.strip()}
    _items[_next_id] = item
    _next_id += 1
    return jsonify(item), 201


@bp.get("/api/items/<int:item_id>")
def get_item(item_id):
    item = _items.get(item_id)
    if item is None:
        return jsonify(error="not found"), 404
    return jsonify(item)


@bp.delete("/api/items/<int:item_id>")
def delete_item(item_id):
    if _items.pop(item_id, None) is None:
        return jsonify(error="not found"), 404
    return "", 204


def reset_store():
    """Clear the in-memory store (used by tests)."""
    global _next_id
    _items.clear()
    _next_id = 1
