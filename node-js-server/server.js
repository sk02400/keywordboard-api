const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { Pool } = require('pg');
const http = require('http');
const WebSocket = require('ws');
const authRoutes = require('./routes/auth');
const axios = require('axios');
const cheerio = require('cheerio');

const app = express();
const port = 3000;

app.use(cors());
app.use(bodyParser.json());
app.use('/auth', authRoutes);
app.use(express.json());
const rankingRoutes = require('./routes/ranking');
app.use('/', rankingRoutes);

const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'boarddb',
  password: 'postgres',
  port: 5432,
});

// ログイン
app.post('/login', async (req, res) => {
  const { user_id, password } = req.body;
  try {
    const result = await pool.query(
      'SELECT * FROM users WHERE user_id = $1 AND password = $2',
      [user_id, password]
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


// ユーザーが存在するか確認
app.post('/check-email', async (req, res) => {
  const { email } = req.body;
  try {
    const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
    res.json({ exists: result.rows.length > 0 });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ユーザー新規登録（Googleログイン後）
app.post('/auth/google-register', async (req, res) => {
  const { email, userId, userName, password } = req.body;

  if (!email || !userId || !userName || !password) {
    return res.status(400).json({ success: false, message: '全ての項目を入力してください' });
  }

  // 例: PostgreSQL を使ってユーザー登録（ユーザーが既に存在するかチェックなど）
  try {
    const existingUser = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
    if (existingUser.rows.length > 0) {
      return res.status(400).json({ success: false, message: 'このメールアドレスは既に登録されています' });
    }

    await pool.query(
      'INSERT INTO users (email, user_id, user_name, password) VALUES ($1, $2, $3, $4)',
      [email, userId, userName, password]
    );

    res.json({ success: true, message: '登録成功' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
  }
});

app.post('/boards', async (req, res) => {
  const { board_code, user_id } = req.body;

  if (!board_code) {
    return res.status(400).json({ error: 'board_code is required' });
  }

  try {
    // URL 判定
    const urlPattern = /^https?:\/\/[\w\-._~:\/?#[\]@!$&'()*+,;=%.]+$/i;
    const isLink = urlPattern.test(board_code);
    let pageTitle = board_code;

    if (isLink) {
      try {
        const response = await axios.get(board_code);
        const $ = cheerio.load(response.data);
        const title = $('title').text().trim();
        if (title) {
          pageTitle = title;
        }
      } catch (err) {
        console.warn('タイトル取得失敗:', err.message);
      }
    }

    // 既存掲示板のチェック
    const existing = await pool.query(
      'SELECT board_id FROM boards WHERE board_name = $1',
      [board_code]
    );

    let boardId;

    if (existing.rows.length > 0) {
      boardId = existing.rows[0].board_id;

      // アクセス数をインクリメント
      await pool.query(`
        INSERT INTO boards_access (board_id, access_count_day, access_count_week, access_count_month)
        VALUES ($1, 1, 1, 1)
        ON CONFLICT (board_id) DO UPDATE SET
          access_count_day = boards_access.access_count_day + 1,
          access_count_week = boards_access.access_count_week + 1,
          access_count_month = boards_access.access_count_month + 1
      `, [boardId]);

      return res.json({ board_id: boardId, board_code });
    }

    // 新規掲示板の登録
    const insertResult = await pool.query(`
      INSERT INTO boards (board_name, is_link, page_title)
      VALUES ($1, $2, $3)
      RETURNING board_id
    `, [board_code, isLink, pageTitle]);

    boardId = insertResult.rows[0].board_id;

    // boards_access に初期レコード（初回アクセス扱い）
    await pool.query(`
      INSERT INTO boards_access (board_id, access_count_day, access_count_week, access_count_month)
      VALUES ($1, 1, 1, 1)
    `, [boardId]);

    res.json({ board_id: boardId, board_code });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Database error' });
  }
});



// 掲示板取得
app.get('/boards/:id', async (req, res) => {
  const { id } = req.params;
  try {
    const result = await pool.query('SELECT * FROM boards WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      res.status(404).json({ error: 'Board not found' });
    } else {
      res.json(result.rows[0]);
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 投稿処理
app.post('/boards/:boardId/posts', async (req, res) => {
  const boardId = req.params.boardId;
  const { post_name, user_id, content, created_at } = req.body;

  if (!post_name || !content || !created_at) {
    return res.status(400).json({ success: false, message: '必要な情報が不足しています' });
  }

  try {
    await pool.query(
      'INSERT INTO posts (board_id, post_name, user_id, content, created_at) VALUES ($1, $2, $3, $4, $5)',
      [boardId, post_name, user_id, content, created_at]
    );
    res.status(201).json({ success: true, message: '投稿成功' });
  } catch (err) {
    console.error('投稿エラー:', err);
    res.status(500).json({ success: false, message: '投稿に失敗しました' });
  }
});

// 投稿一覧取得処理
app.get('/posts/:boardId', async (req, res) => {
  const boardId = req.params.boardId;

  try {
    const result = await pool.query(
      'SELECT id, board_id, post_name, content, created_at FROM posts WHERE board_id = $1 ORDER BY id DESC',
      [boardId]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('投稿取得エラー:', err);
    res.status(500).json({ success: false, message: '投稿一覧取得に失敗しました' });
  }
});

// ブックマーク取得
app.get('/bookmark/status/:userId/:boardId', async (req, res) => {
  const { userId, boardId } = req.params;
  try {
    const result = await pool.query(
      'SELECT * FROM bookmark WHERE user_id = $1 AND board_id = $2',
      [userId, boardId]
    );
    res.json({ bookmarked: result.rows.length > 0 }); // ← JSONオブジェクト形式で返す
  } catch (err) {
    console.error('ブックマーク取得エラー:', err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
  }
});

// ブックマーク登録
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

// ブックマーク一覧（掲示板名付き）取得
app.get('/bookmarks/:userId', async (req, res) => {
  const { userId } = req.params;
  try {
    const result = await pool.query(
      `
      SELECT 
        b.board_id, 
        bd.board_name,
        bd.page_title
      FROM 
        bookmark b
      JOIN 
        boards bd ON b.board_id = bd.board_id
      WHERE 
        b.user_id = $1
      ORDER BY 
        b.created_at DESC
      `,
      [userId]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('ブックマーク取得エラー:', err);
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

    res.json({ room_id: roomId, partner_id: user2_id, partner_name: user2_id, last_message: null });
  } catch (err) {
    console.error('チャットルーム作成エラー:', err);
    res.status(500).json({ success: false, message: 'チャットルーム作成に失敗しました' });
  }
});

// ユーザー存在チェック
app.get('/users/:userId/exists', async (req, res) => {
  const { userId } = req.params;
  try {
    const result = await pool.query('SELECT 1 FROM users WHERE user_id=$1', [userId]);
    res.json({ exists: result.rows.length > 0 });
  } catch (err) {
    console.error('ユーザー存在チェックエラー:', err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
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
       LEFT JOIN users u ON u.user_id = CASE WHEN c.user1_id=$1 THEN c.user2_id ELSE c.user1_id END
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

// ----- WebSocketサーバー設定 -----
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
        created_at: new Date().toISOString(),
      });

      const clients = roomSockets.get(roomId) || [];
      clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(payload);
        }
      });
    } catch (err) {
      console.error('WebSocketエラー:', err);
    }
  });

  ws.on('close', () => {
    const clients = roomSockets.get(roomId) || [];
    roomSockets.set(roomId, clients.filter(client => client !== ws));
  });

  ws.on('error', (err) => {
    console.error('WebSocket接続エラー:', err);
  });
});

app.post('/auth/google', async (req, res) => {
  const { email } = req.body;

  if (!email) {
    return res.status(400).json({ success: false, message: 'Email is required' });
  }

  try {
    const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
    if (result.rows.length > 0) {
      const user = result.rows[0];
      res.json({
        success: true,
        userId: user.userid,
        userName: user.username
      });
    } else {
      res.status(404).json({ success: false, message: 'User not registered' });
    }
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

server.listen(port, () => {
  console.log(`🚀 サーバー起動 http://localhost:${port}`);
});
