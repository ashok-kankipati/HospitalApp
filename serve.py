"""
Production entry point for the Household Expense Tracker.

This is the script NSSM should point to. It uses waitress (a
production-grade WSGI server for Windows) instead of Flask's
built-in dev server.
"""

from waitress import serve
from app import app

HOST = '0.0.0.0'
PORT = 8085

if __name__ == '__main__':
    print(f'Starting Household Expense Tracker on http://{HOST}:{PORT}')
    serve(app, host=HOST, port=PORT)
