"""Flask application factory."""

from flask import Flask

from .config import get_config


def create_app(config_name=None):
    app = Flask(__name__)
    app.config.from_object(get_config(config_name))

    from .routes import bp

    app.register_blueprint(bp)

    @app.errorhandler(404)
    def not_found(_error):
        return {"error": "not found"}, 404

    return app
