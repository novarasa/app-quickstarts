"""Configuration classes selected via the APP_ENV environment variable."""

import os


class Config:
    SECRET_KEY = os.environ.get("SECRET_KEY", "dev-secret-change-me")
    APP_NAME = os.environ.get("APP_NAME", "python-flask-quickstart")
    DEBUG = False
    TESTING = False


class DevelopmentConfig(Config):
    DEBUG = True


class TestingConfig(Config):
    TESTING = True


class ProductionConfig(Config):
    pass


CONFIGS = {
    "development": DevelopmentConfig,
    "testing": TestingConfig,
    "production": ProductionConfig,
}


def get_config(name=None):
    name = name or os.environ.get("APP_ENV", "development")
    return CONFIGS.get(name, DevelopmentConfig)
