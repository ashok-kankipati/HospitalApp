"""PythonAnywhere: add the project directory to sys.path in the Web tab WSGI file."""
import os

os.environ['CHAT_HTTPS'] = '1'
from app import app as application
