import re
import click
from werkzeug.security import generate_password_hash
from .db import get_db


def register_commands(app):
    @app.cli.command('create-admin')
    @click.option('--username', prompt=True)
    @click.password_option()
    def create_admin(username, password):
        """Create a dedicated administrator from the trusted server console."""
        if not re.fullmatch(r'[A-Za-z0-9_]{3,24}', username):
            raise click.ClickException('Use 3–24 letters, numbers, or underscores.')
        if not 8 <= len(password) <= 128:
            raise click.ClickException('Password must contain 8–128 characters.')
        db = get_db()
        if db.execute('SELECT 1 FROM users WHERE username=?', (username,)).fetchone():
            raise click.ClickException('Username already exists. Choose a new dedicated admin username.')
        with db:
            db.execute('INSERT INTO users(username,password,is_admin) VALUES (?,?,1)',
                       (username, generate_password_hash(password)))
        click.echo('Admin created. Choose Administrator sign-in on the login page.')
