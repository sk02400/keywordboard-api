const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { Pool } = require('pg');
const axios = require('axios');
const cheerio = require('cheerio');

const authRoutes = require('./routes/auth');
const rankingRoutes = require('./routes/ranking');

const app = express();
const port = 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use('/auth', authRoutes);
app.use('/', rankingRoutes);

// PostgreSQL接続設定
const pool = new Pool({
  user: 'board_db_2jat_user',
  host: 'dpg-d0tr1pu3jp1c73etd9bg-a',
  database: 'board_db_2jat',
  password: 'NhmaBRj4tteZjcsTojr5pLCxtatIQwNU',
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
      res.json({ success: false, token: '', message: 'IDまたはパスワードが違います' });
    }
  } catch (err) {
    console.error('DBエラー:', err);
    res.status(500).json({ success: false, message: 'サーバーエラー' });
  }
});

// メールアドレス存在確認
app.post('/check-email', async (req, res) => {
  const { email } = req.body;
  try {
    const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
    res.json({ exists: result.rows.length > 0 });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Googleログイン後ユーザー登録
app.post('/auth/google-register', async (req, res) => {
  const { email, userId, userName, password } = req.body;

  if (!email || !userId || !userName || !password) {
    return res.status(400).json({ success: false, message: '全ての項目を入力してください' });
  }

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

// 掲示板作成・取得
app.post('/boards', async (req, res) => {
  const { board_code, user_id } = req.body;

  if (!board_code) {
    return res.status(400).json({ error: 'board_code is required' });
  }

  try {
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

    const existing = await pool.query(
      'SELECT board_id FROM boards WHERE board_name = $1',
      [board_code]
    );

    let boardId;

    if (existing.rows.length > 0) {
      boardId = existing.rows[0].board_id;

      await pool.query(`
        INSERT INTO boards_access (board_id, access_count_day, access_count_week, access_count_month)
        VALUES ($1, 1, 1, 1)
        ON CONFLICT (board_id) DO UPDATE SET
          access_count_day = boards_access.access_count_day + 1,
          access_count_week = boards_access.access_count_week + 1,
          access_count_month = boards_access.access_count_month + 1
      `, [boardId]);

      return res.json({
        board_id: boardId,
        page_title: pageTitle,
        board_name: board_code,
        is_link: isLink
      });
    }

    const insertResult = await pool.query(`
      INSERT INTO boards (board_name, is_link, page_title)
      VALUES ($1, $2, $3)
      RETURNING board_id, page_title
    `, [board_code, isLink, pageTitle]);

    boardId = insertResult.rows[0].board_id;
    pageTitle = insertResult.rows[0].page_title;

    await pool.query(`
      INSERT INTO boards_access (board_id, access_count_day, access_count_week, access_count_month)
      VALUES ($1, 1, 1, 1)
    `, [boardId]);

    res.json({
      board_id: boardId,
      page_title: pageTitle,
      board_name: board_code,
      is_link: isLink
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Database error' });
  }
});

// 掲示板取得
app.get('/boards/:id', async (req, res) => {
  const { id } = req.params;
  try {
    const result = await pool.query('SELECT * FROM boards WHERE board_id = $1', [id]);
    if (result.rows.length === 0) {
      res.status(404).json({ error: 'Board not found' });
    } else {
      res.json(result.rows[0]);
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 投稿作成
app.post('/boards/:boardId/posts', async (req, res) => {
  const boardId = req.params.boardId;
  const { post_name, user_id, content, created_at } = req.body;

  if (!post_name || !content || !created_at) {
    return res.status(400).json({ success: false, message: '必要な情報が不足しています' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const result = await client.query(
      'SELECT post_number FROM posts WHERE board_id = $1 FOR UPDATE',
      [boardId]
    );

    const maxPostNumber = result.rows.reduce((max, row) => {
      return row.post_number > max ? row.post_number : max;
    }, 0);

    const nextPostNumber = maxPostNumber + 1;

    await client.query(
      `INSERT INTO posts (board_id, post_name, user_id, content, created_at, post_number)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [boardId, post_name, user_id, content, created_at, nextPostNumber]
    );

    await client.query('COMMIT');
    res.status(201).json({ success: true, message: '投稿成功', post_number: nextPostNumber });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('投稿エラー:', err);
    res.status(500).json({ success: false, message: '投稿に失敗しました' });
  } finally {
    client.release();
  }
});

// 投稿一覧取得
app.get('/posts/:boardId', async (req, res) => {
  const boardId = req.params.boardId;
  const limit = parseInt(req.query.limit || 50);
  const offset = parseInt(req.query.offset || 0);

  try {
    const result = await pool.query(
      `SELECT id, board_id, post_name, content, created_at, post_number
       FROM posts
       WHERE board_id = $1
       ORDER BY post_number ASC
       LIMIT $2 OFFSET $3`,
      [boardId, limit, offset]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('投稿取得エラー:', err);
    res.status(500).json({ success: false, message: '投稿一覧取得に失敗しました' });
  }
});

// ブックマーク関連
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

app.get('/bookmarks/:userId', async (req, res) => {
  const { userId } = req.params;
  try {
    const result = await pool.query(
      `
      SELECT
        b.board_id,
        bd.board_name,
        bd.page_title,
        bd.is_link
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
    console.error('ブックマーク一覧取得エラー:', err);
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

    if (existing.rows.length > 0) {
      return res.json({ room_id: existing.rows[0].room_id });
    }

    const result = await pool.query(
      `INSERT INTO chat_rooms (user1_id, user2_id)
       VALUES ($1, $2)
       RETURNING room_id`,
      [user1_id, user2_id]
    );

    res.json({ room_id: result.rows[0].room_id });
  } catch (err) {
    console.error('チャットルーム作成エラー:', err);
    res.status(500).json({ error: 'サーバーエラー' });
  }
});

// サーバー起動
app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
