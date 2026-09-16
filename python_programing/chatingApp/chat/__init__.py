import os
import secrets
import time
from pathlib import Path
from flask import Flask, jsonify, request, session
from werkzeug.exceptions import HTTPException
from .db import close_db, init_db, get_db


def create_app(config=None):
    app = Flask(__name__, instance_relative_config=True)
    Path(app.instance_path).mkdir(parents=True, exist_ok=True)
    secret = os.environ.get('SECRET_KEY')
    if not secret:
        secret_path = Path(app.instance_path) / 'secret.key'
        try:
            with secret_path.open('x') as file:
                file.write(secrets.token_hex(32))
        except FileExistsError:
            pass
        secret = secret_path.read_text().strip()
    app.config.update(SECRET_KEY=secret, DATABASE=str(Path(app.instance_path) / 'chat.sqlite3'),
                      MAX_CONTENT_LENGTH=5 * 1024 * 1024 + 16384, SESSION_COOKIE_HTTPONLY=True,
                      SESSION_COOKIE_SAMESITE='Lax', SESSION_COOKIE_NAME='gather_session',
                      SESSION_COOKIE_SECURE=os.environ.get('CHAT_HTTPS') == '1')
    if config:
        app.config.update(config)
    from .tab_sessions import TabSessionInterface
    app.session_interface = TabSessionInterface()
    app.teardown_appcontext(close_db)

    @app.before_request
    def csrf_check():
        db = get_db()
        with db:
            db.execute('DELETE FROM messages WHERE seen_at <= ?', (time.time() - 300,))
        if request.method in ('POST', 'PUT', 'DELETE', 'PATCH'):
            expected = session.get('csrf')
            if not expected or not secrets.compare_digest(expected, request.headers.get('X-CSRF-Token', '')):
                return jsonify(code='csrf_expired', error='Your security token needs updating. Please try again.'), 403

    @app.after_request
    def headers(response):
        response.headers['X-Content-Type-Options'] = 'nosniff'
        response.headers['X-Frame-Options'] = 'DENY'
        response.headers['Referrer-Policy'] = 'same-origin'
        response.headers['Content-Security-Policy'] = "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"
        if request.path.startswith('/api/') or request.path == '/':
            response.headers['Cache-Control'] = 'no-store'
        return response

    @app.errorhandler(HTTPException)
    def http_error(error):
        return jsonify(error=error.description), error.code

    from .routes import bp
    app.register_blueprint(bp)
    from .photos import bp as photos_bp
    app.register_blueprint(photos_bp)
    from .calls import bp as calls_bp
    app.register_blueprint(calls_bp)
    from .admin import register_commands
    register_commands(app)
    with app.app_context():
        init_db()
    return app
