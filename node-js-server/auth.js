// routes/auth.js
const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const db = require('../db'); // knex設定


// Googleユーザー新規登録
router.post('/google-register', async (req, res) => {
  const { userId, userName, password, email } = req.body;

  if (!userId || !userName || !password || !email) {
    return res.status(400).json({ message: '全項目必須です' });
  }

  try {
    const existing = await db('users').where({ user_id: userId }).orWhere({ email }).first();
    if (existing) {
      return res.status(409).json({ message: '既に登録済みです' });
    }

    const hashed = await bcrypt.hash(password, 10);
    const newUser = {
      user_id: userId,
      user_name: userName,
      password: hashed,
      email,
    };

    await db('users').insert(newUser);
    res.status(201).json({ message: '登録完了', user: newUser });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: '登録失敗' });
  }
});

router.post('/google-register', async (req, res) => {
    const { email, userId, userName, password } = req.body;

    if (!email || !userId || !userName || !password) {
        return res.status(400).json({ success: false, message: "全ての項目を入力してください" });
    }

    try {
        // 重複確認
        const existingUser = await pool.query('SELECT * FROM users WHERE email = $1 OR user_id = $2', [email, userId]);
        if (existingUser.rows.length > 0) {
            return res.status(409).json({ success: false, message: "すでに登録されています" });
        }

        const hashedPassword = await bcrypt.hash(password, 10);

        await pool.query(
            'INSERT INTO users (email, user_id, user_name, password) VALUES ($1, $2, $3, $4)',
            [email, userId, userName, hashedPassword]
        );

        res.json({ success: true, message: "登録成功" });
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, message: "サーバーエラー" });
    }
});

module.exports = router;
