"""Authenticated photo storage, with bounded decoding and metadata removal."""
import io
import warnings
from flask import Blueprint, Response, abort, jsonify, request, session
from PIL import Image, ImageOps, UnidentifiedImageError
from .db import get_db
from .routes import login_required, rate_limit, room_access

bp = Blueprint('photos', __name__)


def normalize_photo(upload):
    raw = upload.read(5 * 1024 * 1024 + 1)
    if not raw or len(raw) > 5 * 1024 * 1024:
        abort(400, 'Photo must be between 1 byte and 5 MB.')
    try:
        with warnings.catch_warnings():
            warnings.simplefilter('error', Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(raw)) as source:
                if source.format not in ('JPEG', 'PNG', 'WEBP'):
                    abort(400, 'Choose a JPEG, PNG, or WebP photo.')
                if source.width * source.height > 24_000_000:
                    abort(400, 'Photo resolution is too large. Choose a smaller photo.')
                source.load()
                oriented = ImageOps.exif_transpose(source)
                oriented.thumbnail((1600, 1600))
                rgba = oriented.convert('RGBA')
                clean = Image.new('RGB', rgba.size, 'white')
                clean.paste(rgba, mask=rgba.getchannel('A'))
                output = io.BytesIO()
                clean.save(output, format='JPEG', quality=82)
                return output.getvalue()
    except (UnidentifiedImageError, OSError, ValueError, Image.DecompressionBombWarning, Image.DecompressionBombError):
        abort(400, 'Unable to read this photo. Choose a valid JPEG, PNG, or WebP image.')


@bp.post('/api/rooms/<int:room_id>/photos')
@login_required
def upload_photo(room_id):
    room_access(room_id)
    rate_limit(f"send:{session['user_id']}", 30, 60)
    upload = request.files.get('photo')
    if upload is None:
        abort(400, 'Choose a photo first.')
    caption = request.form.get('caption', '').strip()
    if len(caption) > 2000:
        abort(400, 'Caption must be at most 2000 characters.')
    image = normalize_photo(upload)
    db = get_db()
    reply_id = request.form.get('reply_to_id')
    if reply_id:
        try:
            reply_id = int(reply_id)
        except ValueError:
            abort(400, 'Invalid reply message.')
        if not db.execute('SELECT 1 FROM messages WHERE id=? AND room_id=?', (reply_id, room_id)).fetchone():
            abort(400, 'The message you are replying to is no longer available.')
    with db:
        db.execute('UPDATE message_sequence SET value=value+1 WHERE id=1')
        message_id = db.execute('SELECT value FROM message_sequence WHERE id=1').fetchone()[0]
        db.execute('INSERT INTO messages(id,room_id,user_id,body,reply_to_id) VALUES (?,?,?,?,?)',
                   (message_id, room_id, session['user_id'], caption or 'Photo', reply_id or None))
        db.execute('INSERT INTO photo_messages VALUES (?,?)', (message_id, image))
    return jsonify(id=message_id), 201


@bp.get('/api/rooms/<int:room_id>/messages/<int:message_id>/photo')
@login_required
def get_photo(room_id, message_id):
    room_access(room_id)
    row = get_db().execute('''SELECT p.image FROM photo_messages p JOIN messages m ON m.id=p.message_id
        WHERE m.room_id=? AND m.id=?''', (room_id, message_id)).fetchone()
    if not row:
        abort(404, 'Photo is no longer available.')
    return Response(row['image'], mimetype='image/jpeg')
