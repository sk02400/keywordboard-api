const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { Pool } = require('pg');
const http = require('http');
const WebSocket = require('ws');
const authRoutes = require('./routes/auth');

const app = express();
const port = 3000;

app.use(cors());
app.use(bodyParser.json());
app.use('/auth', authRoutes);

const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'boarddb',
  password: 'postgres',
  port: 5432,
});

// ----- ルーティング -----

// ログイン
app.post('/login', async (req, res) => {
  const { id, password } = req.body;
  try {
    const result = await pool.query(
      'SELECT * FROM users WHERE id = $1 AND password = $2',
      [id, password]
    );
    if (result.rows.length > 0) {
      res.json({ success: true, token: 'dummy_token', message: 'ログイン成功' });
    } else {
      res.json({ success: false, token: 'dummy_token', message: 'IDまたはパスワードが違います' });
    }
  } catch (err) {
    console.error('DBエラー:', err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
  }
});

// 投稿作成
app.post('/boards/:boardId/posts', async (req, res) => {
  const boardId = req.params.boardId;
  const { post_name, user_id, content, timestamp } = req.body;
  if (!post_name || !content || !timestamp) {
    return res.status(400).json({ success: false, message: '必要な情報が不足しています' });
  }
  try {
    await pool.query(
      'INSERT INTO posts (board_id, post_name, user_id, content, timestamp) VALUES ($1, $2, $3, $4, $5)',
      [boardId, post_name, user_id, content, timestamp]
    );
    res.status(201).json({ success: true, message: '投稿成功' });
  } catch (err) {
    console.error('投稿エラー:', err);
    res.status(500).json({ success: false, message: '投稿に失敗しました' });
  }
});

// 掲示板作成
app.post('/boards', async (req, res) => {
  const { board_name } = req.body;
  if (!board_name) {
    return res.status(400).json({ error: 'board_name is required' });
  }
  try {
    const existing = await pool.query(
      'SELECT id FROM boards WHERE board_name = $1',
      [board_name]
    );
    if (existing.rows.length > 0) {
      return res.json({ board_id: existing.rows[0].id, board_name });
    }
    const result = await pool.query(
      'INSERT INTO boards (board_name) VALUES ($1) RETURNING id',
      [board_name]
    );
    res.json({ board_id: result.rows[0].id, board_name });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Database error' });
  }
});

// 投稿一覧取得
app.get('/posts/:boardId', async (req, res) => {
  const boardId = req.params.boardId;
  try {
    const result = await pool.query(
      'SELECT id, board_id, post_name, content, timestamp FROM posts WHERE board_id = $1 ORDER BY id DESC',
      [boardId]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('投稿取得エラー:', err);
    res.status(500).json({ success: false, message: '投稿一覧取得に失敗しました' });
  }
});

// ブックマーク状態取得
app.get('/bookmark/status/:userId/:boardId', async (req, res) => {
  const { userId, boardId } = req.params;
  try {
    const result = await pool.query(
      'SELECT * FROM bookmark WHERE user_id = $1 AND board_id = $2',
      [userId, boardId]
    );
    res.json({ bookmarked: result.rows.length > 0 });
  } catch (err) {
    console.error('ブックマーク取得エラー:', err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
  }
});

// ブックマーク追加
app.post('/bookmark/:userId/:boardId', async (req, res) => {
  const { userId, boardId } = req.params;
  try {
    await pool.query(
      'INSERT INTO bookmark (user_id, board_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
      [userId, boardId]
    );
    res.json({ success: true, message: 'ブックマーク追加成功' });
  } catch (err) {
    console.error('ブックマーク追加エラー:', err);
    res.status(500).json({ success: false, message: 'ブックマーク追加に失敗しました' });
  }
});

// ブックマーク解除
app.post('/unbookmark/:userId/:boardId', async (req, res) => {
  const { userId, boardId } = req.params;
  try {
    await pool.query(
      'DELETE FROM bookmark WHERE user_id = $1 AND board_id = $2',
      [userId, boardId]
    );
    res.json({ success: true, message: 'ブックマーク解除成功' });
  } catch (err) {
    console.error('ブックマーク解除エラー:', err);
    res.status(500).json({ success: false, message: 'ブックマーク解除に失敗しました' });
  }
});

// ユーザーのブックマーク一覧取得
app.get('/bookmarks/:userId', async (req, res) => {
  const { userId } = req.params;
  try {
    const result = await pool.query(
      `SELECT b.board_id, bd.board_name
       FROM bookmark b
       JOIN boards bd ON b.board_id = bd.id
       WHERE b.user_id = $1
       ORDER BY b.created_at DESC`,
      [userId]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('ブックマーク取得エラー:', err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
  }
});

// ユーザー存在チェック
app.get('/users/:userId/exists', async (req, res) => {
  const { userId } = req.params;
  try {
    const result = await pool.query('SELECT 1 FROM users WHERE id=$1', [userId]);
    res.json({ exists: result.rows.length > 0 });
  } catch (err) {
    console.error('ユーザー存在チェックエラー:', err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
  }
});

// チャットルーム作成
app.post('/chats', async (req, res) => {
  const { user1_id, user2_id } = req.body;
  if (user1_id === user2_id) return res.status(400).json({ error: '同じユーザーとは作成できません' });

  try {
    const existing = await pool.query(
      `SELECT room_id FROM chat_rooms
       WHERE (user1_id=$1 AND user2_id=$2) OR (user1_id=$2 AND user2_id=$1)`,
      [user1_id, user2_id]
    );

    let roomId;
    if (existing.rows.length > 0) {
      roomId = existing.rows[0].room_id;
    } else {
      const insert = await pool.query(
        `INSERT INTO chat_rooms (user1_id, user2_id) VALUES ($1,$2) RETURNING room_id`,
        [user1_id, user2_id]
      );
      roomId = insert.rows[0].room_id;
    }

    res.json({ room_id: roomId, partner_id: user2_id, partner_name: user2_id });
  } catch (err) {
    console.error('チャットルーム作成エラー:', err);
    res.status(500).json({ success: false, message: 'チャットルーム作成に失敗しました' });
  }
});

// DM一覧取得
app.get('/chats/:userId', async (req, res) => {
  const { userId } = req.params;
  try {
    const result = await pool.query(
      `SELECT c.room_id,
              CASE WHEN c.user1_id=$1 THEN c.user2_id ELSE c.user1_id END AS partner_id,
              COALESCE(u.user_name, '') AS partner_name,
              m.content AS last_message
       FROM chat_rooms c
       LEFT JOIN LATERAL (
         SELECT content FROM messages m
         WHERE m.room_id = c.room_id
         ORDER BY m.created_at DESC LIMIT 1
       ) m ON true
       LEFT JOIN users u ON u.id = CASE WHEN c.user1_id=$1 THEN c.user2_id ELSE c.user1_id END
       WHERE c.user1_id=$1 OR c.user2_id=$1
       ORDER BY c.room_id DESC`,
      [userId]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('DM一覧取得エラー:', err);
    res.status(500).json({ success: false, message: 'DM一覧取得に失敗しました' });
  }
});

// メッセージ一覧取得
app.get('/messages/:roomId', async (req, res) => {
  const roomId = req.params.roomId;
  try {
    const messages = await pool.query(
      'SELECT sender_id, sender_name, content, created_at FROM messages WHERE room_id = $1 ORDER BY created_at',
      [roomId]
    );
    res.json(messages.rows);
  } catch (err) {
    console.error('メッセージ取得エラー:', err);
    res.status(500).json({ success: false, message: 'メッセージ取得に失敗しました' });
  }
});

// メッセージ送信（HTTP）
app.post('/messages', async (req, res) => {
  const { room_id, sender_id, sender_name, content } = req.body;
  try {
    await pool.query(
      'INSERT INTO messages (room_id, sender_id, sender_name, content) VALUES ($1, $2, $3, $4)',
      [room_id, sender_id, sender_name, content]
    );
    res.sendStatus(200);
  } catch (err) {
    console.error('メッセージ送信エラー:', err);
    res.status(500).json({ success: false, message: 'メッセージ送信に失敗しました' });
  }
});

// ----- WebSocket サーバー実装 -----
const server = http.createServer(app);
const wss = new WebSocket.Server({ noServer: true });

const roomSockets = new Map();

server.on('upgrade', (req, socket, head) => {
  const match = req.url.match(/^\/ws\/(.+)$/);
  if (!match) {
    socket.destroy();
    return;
  }
  const roomId = match[1];
  wss.handleUpgrade(req, socket, head, (ws) => {
    wss.emit('connection', ws, req, roomId);
  });
});

wss.on('connection', (ws, req, roomId) => {
  if (!roomSockets.has(roomId)) {
    roomSockets.set(roomId, []);
  }
  roomSockets.get(roomId).push(ws);

  ws.on('message', async (data) => {
    try {
      const { sender_id, sender_name, content } = JSON.parse(data);
      await pool.query(
        'INSERT INTO messages (room_id, sender_id, sender_name, content) VALUES ($1, $2, $3, $4)',
        [roomId, sender_id, sender_name, content]
      );

      const payload = JSON.stringify({
        room_id: roomId,
        sender_id,
        sender_name,
        content,
        created_at: new Date().toISOString()
      });

      const clients = roomSockets.get(roomId) || [];
      clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(payload);
        }
      });
    } catch (err) {
      console.error('WebSocket メッセージエラー:', err);
    }
  });

  ws.on('close', () => {
    const clients = roomSockets.get(roomId) || [];
    roomSockets.set(roomId, clients.filter(c => c !== ws));
  });
});

// Google認証用
const { OAuth2Client } = require('google-auth-library');
const client = new OAuth2Client('407964159782-csb5sa23om98fndtjnacv2vlcig6qct4.apps.googleusercontent.com'); // クライアントIDは適宜修正してください

// Googleログイントークン
