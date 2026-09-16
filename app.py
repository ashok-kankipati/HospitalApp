"""
Household Expense Tracker
--------------------------
A small Flask web app to record daily household purchases and
view the total amount spent for any month.

Data is stored locally in a SQLite file (expenses.db) next to this
script, so it persists across restarts of the Windows service.
"""

from flask import Flask, Response, flash, render_template, request, redirect, session, url_for
from functools import wraps
from werkzeug.security import check_password_hash, generate_password_hash
from itsdangerous import BadSignature, SignatureExpired, URLSafeTimedSerializer
import csv
import io
import smtplib
import secrets
import sqlite3
import time
from datetime import datetime
from email.message import EmailMessage
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, 'expenses.db')

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('EXPENSES_SECRET_KEY', 'change-this-secret-key')

CATEGORIES = ['Groceries', 'Utilities', 'Rent', 'Transport',
              'Medical', 'Education', 'Entertainment', 'Other']
SECTIONS = ['Home', 'Personal']


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def token_serializer():
    return URLSafeTimedSerializer(app.config['SECRET_KEY'], salt='account-email')


def send_account_email(recipient, subject, body):
    """Send mail using SMTP settings supplied through environment variables."""
    host = os.environ.get('EXPENSES_SMTP_HOST')
    sender = os.environ.get('EXPENSES_EMAIL_FROM')
    if not host or not sender:
        app.logger.error('Email is not configured: set EXPENSES_SMTP_HOST and EXPENSES_EMAIL_FROM')
        return False
    message = EmailMessage()
    message['From'], message['To'], message['Subject'] = sender, recipient, subject
    message.set_content(body)
    port = int(os.environ.get('EXPENSES_SMTP_PORT', '587'))
    username = os.environ.get('EXPENSES_SMTP_USERNAME')
    password = os.environ.get('EXPENSES_SMTP_PASSWORD')
    try:
        with smtplib.SMTP(host, port, timeout=15) as smtp:
            if os.environ.get('EXPENSES_SMTP_TLS', 'true').lower() in ('1', 'true', 'yes'):
                smtp.starttls()
            if username:
                smtp.login(username, password or '')
            smtp.send_message(message)
        return True
    except (OSError, smtplib.SMTPException):
        app.logger.exception('Could not send account email')
        return False


def send_verification_email(user_id, username, email):
    token = token_serializer().dumps({'purpose': 'verify', 'user_id': user_id, 'email': email})
    link = url_for('verify_email', token=token, _external=True)
    return send_account_email(email, 'Verify your Household Expenses email',
                              f'Hello {username},\n\nVerify your email address:\n{link}\n\n'
                              'This link expires in 24 hours.')


def generate_recovery_key():
    """Return a BitLocker-style 48-digit recovery key in eight groups."""
    return '-'.join(f'{secrets.randbelow(1_000_000):06d}' for _ in range(8))


def normalize_recovery_key(value):
    return ''.join(character for character in value if character.isdigit())


def parse_month(value):
    """Return a valid YYYY-MM value, falling back to the current month."""
    try:
        return datetime.strptime(value, '%Y-%m').strftime('%Y-%m')
    except (TypeError, ValueError):
        return datetime.now().strftime('%Y-%m')


def shift_month(year_month, offset):
    month_date = datetime.strptime(year_month, '%Y-%m')
    month_index = month_date.year * 12 + month_date.month - 1 + offset
    return f'{month_index // 12:04d}-{month_index % 12 + 1:02d}'


def month_options(conn, selected_month, user_id):
    """Build a newest-first list from the earliest expense to this month."""
    earliest = conn.execute(
        "SELECT MIN(substr(date, 1, 7)) AS month FROM expenses WHERE user_id = ?",
        (user_id,)
    ).fetchone()['month']
    current = datetime.now().strftime('%Y-%m')
    first = parse_month(earliest) if earliest else current
    last = max(current, selected_month)
    first = min(first, selected_month)

    options = []
    cursor = last
    while cursor >= first:
        options.append({
            'value': cursor,
            'label': datetime.strptime(cursor, '%Y-%m').strftime('%B %Y'),
        })
        cursor = shift_month(cursor, -1)
    return options


def init_db():
    conn = get_db()
    conn.execute('''
        CREATE TABLE IF NOT EXISTS expenses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date TEXT NOT NULL,
            description TEXT NOT NULL,
            category TEXT,
            amount REAL NOT NULL
        )
    ''')
    # add `section` column if it doesn't exist (migrate older DBs)
    cols = [r['name'] for r in conn.execute("PRAGMA table_info(expenses)").fetchall()]
    if 'section' not in cols:
        conn.execute("ALTER TABLE expenses ADD COLUMN section TEXT DEFAULT 'Home'")
    if 'user_id' not in cols:
        conn.execute("ALTER TABLE expenses ADD COLUMN user_id INTEGER")
    conn.execute('''CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL COLLATE NOCASE UNIQUE,
        password_hash TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    )''')
    user_cols = [r['name'] for r in conn.execute("PRAGMA table_info(users)").fetchall()]
    if 'email' not in user_cols:
        conn.execute('ALTER TABLE users ADD COLUMN email TEXT COLLATE NOCASE')
    if 'email_verified' not in user_cols:
        conn.execute('ALTER TABLE users ADD COLUMN email_verified INTEGER NOT NULL DEFAULT 0')
    if 'reset_nonce' not in user_cols:
        conn.execute('ALTER TABLE users ADD COLUMN reset_nonce TEXT')
    if 'reset_otp_hash' not in user_cols:
        conn.execute('ALTER TABLE users ADD COLUMN reset_otp_hash TEXT')
    if 'reset_otp_expires' not in user_cols:
        conn.execute('ALTER TABLE users ADD COLUMN reset_otp_expires INTEGER')
    if 'reset_otp_attempts' not in user_cols:
        conn.execute('ALTER TABLE users ADD COLUMN reset_otp_attempts INTEGER NOT NULL DEFAULT 0')
    if 'recovery_key_hash' not in user_cols:
        conn.execute('ALTER TABLE users ADD COLUMN recovery_key_hash TEXT')
    # A household may intentionally share one recovery inbox across accounts.
    conn.execute('DROP INDEX IF EXISTS users_email_unique')
    conn.commit()
    conn.close()


init_db()


def login_required(view):
    @wraps(view)
    def wrapped_view(*args, **kwargs):
        if 'user_id' not in session:
            return redirect(url_for('login', next=request.full_path.rstrip('?')))
        return view(*args, **kwargs)
    return wrapped_view


@app.context_processor
def inject_current_user():
    return {'current_username': session.get('username')}


@app.route('/register', methods=['GET', 'POST'])
def register():
    if session.get('user_id'):
        return redirect(url_for('index'))
    username = request.form.get('username', '').strip()
    if request.method == 'POST':
        password = request.form.get('password', '')
        if len(username) < 3:
            flash('Username must contain at least 3 characters.', 'danger')
        elif len(password) < 8:
            flash('Password must contain at least 8 characters.', 'danger')
        elif password != request.form.get('confirm_password', ''):
            flash('Passwords do not match.', 'danger')
        else:
            conn = get_db()
            recovery_key = generate_recovery_key()
            try:
                cursor = conn.execute('INSERT INTO users (username, password_hash, recovery_key_hash) '
                                      'VALUES (?, ?, ?)',
                                      (username, generate_password_hash(password),
                                       generate_password_hash(normalize_recovery_key(recovery_key))))
                user_id = cursor.lastrowid
                if conn.execute('SELECT COUNT(*) AS count FROM users').fetchone()['count'] == 1:
                    conn.execute('UPDATE expenses SET user_id = ? WHERE user_id IS NULL', (user_id,))
                conn.commit()
            except sqlite3.IntegrityError:
                conn.close()
                flash('That username is already registered.', 'danger')
                return render_template('register.html', username=username)
            conn.close()
            session.clear()
            session.update(user_id=user_id, username=username)
            return render_template('recovery_key.html', recovery_key=recovery_key, is_new_account=True)
    return render_template('register.html', username=username)


@app.route('/login', methods=['GET', 'POST'])
def login():
    if session.get('user_id'):
        return redirect(url_for('index'))
    if request.method == 'POST':
        username = request.form.get('username', '').strip()
        conn = get_db()
        user = conn.execute('SELECT * FROM users WHERE username = ?', (username,)).fetchone()
        conn.close()
        if user and check_password_hash(user['password_hash'], request.form.get('password', '')):
            session.clear()
            session.update(user_id=user['id'], username=user['username'])
            next_url = request.args.get('next', '')
            if not next_url.startswith('/') or next_url.startswith('//'):
                next_url = url_for('index')
            return redirect(next_url)
        flash('Invalid username or password.', 'danger')
    return render_template('login.html')


@app.route('/forgot-password', methods=['GET', 'POST'])
def forgot_password():
    if request.method == 'POST':
        supplied_key = normalize_recovery_key(request.form.get('recovery_key', ''))
        conn = get_db()
        users = conn.execute('SELECT id, username, recovery_key_hash FROM users '
                             'WHERE recovery_key_hash IS NOT NULL').fetchall()
        conn.close()
        matched_user = next((user for user in users if len(supplied_key) == 48
                             and check_password_hash(user['recovery_key_hash'], supplied_key)), None)
        if matched_user:
            session.clear()
            session['password_reset_user_id'] = matched_user['id']
            session['password_reset_username'] = matched_user['username']
            session['password_reset_expires'] = int(time.time()) + 600
            flash(f"Recovery key accepted. Reset the password for {matched_user['username']}.", 'success')
            return redirect(url_for('reset_password'))
        flash('That recovery key is invalid.', 'danger')
    return render_template('forgot_password.html')


@app.route('/verify-reset-otp', methods=['GET', 'POST'])
def verify_reset_otp():
    email = session.get('recovery_email', '')
    if request.method == 'POST':
        email = request.form.get('email', '').strip().lower()
        username = request.form.get('username', '').strip()
        otp = request.form.get('otp', '').strip()
        conn = get_db()
        user = conn.execute('SELECT id, reset_otp_hash, reset_otp_expires, reset_otp_attempts '
                            'FROM users WHERE email = ? AND username = ? AND email_verified = 1',
                            (email, username)).fetchone()
        valid = (user and user['reset_otp_hash'] and user['reset_otp_expires']
                 and user['reset_otp_expires'] >= int(time.time())
                 and user['reset_otp_attempts'] < 5 and len(otp) == 6 and otp.isdigit()
                 and check_password_hash(user['reset_otp_hash'], otp))
        if valid:
            conn.execute('UPDATE users SET reset_otp_hash = NULL, reset_otp_expires = NULL WHERE id = ?',
                         (user['id'],))
            conn.commit()
            conn.close()
            session.pop('recovery_email', None)
            session['password_reset_user_id'] = user['id']
            session['password_reset_expires'] = int(time.time()) + 600
            return redirect(url_for('reset_password'))
        if user and user['reset_otp_hash']:
            attempts = user['reset_otp_attempts'] + 1
            conn.execute('UPDATE users SET reset_otp_attempts = ?, reset_otp_hash = CASE WHEN ? >= 5 '
                         'THEN NULL ELSE reset_otp_hash END WHERE id = ?', (attempts, attempts, user['id']))
            conn.commit()
        conn.close()
        flash('The OTP is invalid, expired, or has exceeded five attempts.', 'danger')
    return render_template('verify_reset_otp.html', email=email)


@app.route('/reset-password', methods=['GET', 'POST'])
def reset_password():
    user_id = session.get('password_reset_user_id')
    if not user_id or session.get('password_reset_expires', 0) < int(time.time()):
        session.pop('password_reset_user_id', None)
        session.pop('password_reset_expires', None)
        flash('Enter your recovery key before resetting your password.', 'danger')
        return redirect(url_for('forgot_password'))
    if request.method == 'POST':
        password = request.form.get('password', '')
        if len(password) < 8:
            flash('Password must contain at least 8 characters.', 'danger')
        elif password != request.form.get('confirm_password', ''):
            flash('Passwords do not match.', 'danger')
        else:
            conn = get_db()
            conn.execute('UPDATE users SET password_hash = ?, reset_nonce = NULL, reset_otp_hash = NULL '
                         'WHERE id = ?', (generate_password_hash(password), user_id))
            conn.commit()
            conn.close()
            session.clear()
            flash('Your password has been reset. You can now sign in.', 'success')
            return redirect(url_for('login'))
    return render_template('reset_password.html', username=session.get('password_reset_username'))


@app.route('/account-email', methods=['GET', 'POST'])
@login_required
def account_email():
    conn = get_db()
    user = conn.execute('SELECT recovery_key_hash FROM users WHERE id = ?',
                        (session['user_id'],)).fetchone()
    if request.method == 'POST':
        recovery_key = generate_recovery_key()
        conn.execute('UPDATE users SET recovery_key_hash = ? WHERE id = ?',
                     (generate_password_hash(normalize_recovery_key(recovery_key)), session['user_id']))
        conn.commit()
        conn.close()
        return render_template('recovery_key.html', recovery_key=recovery_key, is_new_account=False)
    conn.close()
    return render_template('account_email.html', user=user)


@app.route('/verify-email/<token>')
def verify_email(token):
    try:
        data = token_serializer().loads(token, max_age=86400)
        if data.get('purpose') != 'verify':
            raise BadSignature('Wrong token purpose')
    except SignatureExpired:
        flash('That verification link has expired. Sign in to request another.', 'danger')
        return redirect(url_for('login'))
    except BadSignature:
        flash('That verification link is invalid.', 'danger')
        return redirect(url_for('login'))
    conn = get_db()
    result = conn.execute('UPDATE users SET email_verified = 1 WHERE id = ? AND email = ?',
                          (data['user_id'], data['email']))
    conn.commit()
    conn.close()
    flash('Email verified. You can now recover your username or password by email.' if result.rowcount
          else 'This email address is no longer attached to that account.',
          'success' if result.rowcount else 'danger')
    return redirect(url_for('index') if session.get('user_id') else url_for('login'))


@app.route('/logout', methods=['POST'])
def logout():
    session.clear()
    flash('You have been signed out.', 'success')
    return redirect(url_for('login'))


@app.route('/')
@login_required
def index():
    today = datetime.now().strftime('%Y-%m-%d')
    conn = get_db()

    todays_expenses = conn.execute(
        'SELECT * FROM expenses WHERE date = ? AND user_id = ? ORDER BY id DESC', (today, session['user_id'])
    ).fetchall()

    year_month = datetime.now().strftime('%Y-%m')
    month_total = conn.execute(
        "SELECT COALESCE(SUM(amount), 0) AS total FROM expenses WHERE date LIKE ? AND user_id = ?",
        (year_month + '%', session['user_id'])
    ).fetchone()['total']

    conn.close()

    return render_template(
        'index.html',
        today=today,
        categories=CATEGORIES,
        todays_expenses=todays_expenses,
        month_total=month_total,
        current_month=datetime.now().strftime('%B %Y'),
        current_year=datetime.now().strftime('%Y'),
        current_month_num=datetime.now().strftime('%m'),
    )


@app.route('/add', methods=['POST'])
@login_required
def add_expense():
    date = request.form.get('date') or datetime.now().strftime('%Y-%m-%d')
    description = request.form.get('description', '').strip()
    category = request.form.get('category', 'Other')
    section = request.form.get('section', 'Home')
    amount_raw = request.form.get('amount', '0').strip()

    try:
        amount = float(amount_raw)
    except ValueError:
        amount = 0.0

    if description and amount > 0:
        conn = get_db()
        conn.execute(
            'INSERT INTO expenses (date, description, category, amount, section, user_id) VALUES (?, ?, ?, ?, ?, ?)',
            (date, description, category, amount, section, session['user_id'])
        )
        conn.commit()
        conn.close()

    return redirect(url_for('index'))


@app.route('/delete/<int:expense_id>', methods=['POST'])
@login_required
def delete_expense(expense_id):
    conn = get_db()
    conn.execute('DELETE FROM expenses WHERE id = ? AND user_id = ?', (expense_id, session['user_id']))
    conn.commit()
    conn.close()
    return redirect(request.referrer or url_for('index'))


@app.route('/edit/<int:expense_id>', methods=['GET', 'POST'])
@login_required
def edit_expense(expense_id):
    conn = get_db()
    if request.method == 'POST':
        date = request.form.get('date') or datetime.now().strftime('%Y-%m-%d')
        description = request.form.get('description', '').strip()
        category = request.form.get('category', 'Other')
        section = request.form.get('section', 'Home')
        amount_raw = request.form.get('amount', '0').strip()
        try:
            amount = float(amount_raw)
        except ValueError:
            amount = 0.0

        if description and amount > 0:
            conn.execute(
                'UPDATE expenses SET date = ?, description = ?, category = ?, amount = ?, section = ? WHERE id = ? AND user_id = ?',
                (date, description, category, amount, section, expense_id, session['user_id'])
            )
            conn.commit()
        conn.close()
        return redirect(request.referrer or url_for('index'))

    expense = conn.execute('SELECT * FROM expenses WHERE id = ? AND user_id = ?', (expense_id, session['user_id'])).fetchone()
    conn.close()
    if not expense:
        return redirect(url_for('index'))

    return render_template('edit.html', expense=expense, categories=CATEGORIES, sections=SECTIONS)


@app.route('/month')
@login_required
def month_view():
    # `period` is the new selector. Keep year/month support for old bookmarks.
    requested_period = request.args.get('period')
    if not requested_period:
        year = request.args.get('year', datetime.now().strftime('%Y'))
        month = request.args.get('month', datetime.now().strftime('%m')).zfill(2)
        requested_period = f'{year}-{month}'
    year_month = parse_month(requested_period)
    year, month = year_month.split('-')

    conn = get_db()

    expenses = conn.execute(
        'SELECT * FROM expenses WHERE date LIKE ? AND user_id = ? ORDER BY date ASC, id ASC',
        (year_month + '%', session['user_id'])
    ).fetchall()

    total = conn.execute(
        "SELECT COALESCE(SUM(amount), 0) AS total FROM expenses WHERE date LIKE ? AND user_id = ?",
        (year_month + '%', session['user_id'])
    ).fetchone()['total']

    category_rows = conn.execute(
        "SELECT category, COALESCE(SUM(amount), 0) AS subtotal "
        "FROM expenses WHERE date LIKE ? AND user_id = ? GROUP BY category ORDER BY subtotal DESC",
        (year_month + '%', session['user_id'])
    ).fetchall()

    section_rows = conn.execute(
        "SELECT section, COALESCE(SUM(amount), 0) AS subtotal "
        "FROM expenses WHERE date LIKE ? AND user_id = ? GROUP BY section ORDER BY subtotal DESC",
        (year_month + '%', session['user_id'])
    ).fetchall()

    available_months = month_options(conn, year_month, session['user_id'])

    conn.close()

    try:
        month_label = datetime.strptime(year_month, '%Y-%m').strftime('%B %Y')
    except ValueError:
        month_label = year_month

    return render_template(
        'month.html',
        expenses=expenses,
        total=total,
        category_rows=category_rows,
        section_totals={
            (row['section'] or 'Home'): row['subtotal']
            for row in section_rows
        },
        year=year,
        month=month,
        selected_month=year_month,
        available_months=available_months,
        previous_month=shift_month(year_month, -1),
        next_month=shift_month(year_month, 1),
        can_go_next=year_month < datetime.now().strftime('%Y-%m'),
        month_label=month_label,
    )


@app.route('/export')
@login_required
def export_expenses():
    """Export one month's expenses as a Google Sheets-compatible CSV file."""
    year = request.args.get('year', datetime.now().strftime('%Y'))
    month = request.args.get('month', datetime.now().strftime('%m')).zfill(2)
    year_month = f'{year}-{month}'

    conn = get_db()
    expenses = conn.execute(
        'SELECT date, description, category, section, amount '
        'FROM expenses WHERE date LIKE ? AND user_id = ? ORDER BY date ASC, id ASC',
        (year_month + '%', session['user_id'])
    ).fetchall()
    conn.close()

    output = io.StringIO(newline='')
    writer = csv.writer(output)
    writer.writerow(['Date', 'Description', 'Category', 'Section', 'Amount'])
    for expense in expenses:
        writer.writerow([
            expense['date'], expense['description'], expense['category'],
            expense['section'] or 'Home', f"{expense['amount']:.2f}",
        ])

    filename = f'household-expenses-{year_month}.csv'
    return Response(
        '\ufeff' + output.getvalue(),
        mimetype='text/csv; charset=utf-8',
        headers={'Content-Disposition': f'attachment; filename="{filename}"'},
    )


if __name__ == '__main__':
    # Local dev server only. For the Windows service, serve.py (waitress) is used instead.
    app.run(debug=True, host='0.0.0.0', port=8085)
