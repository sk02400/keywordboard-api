const express = require('express');
const router = express.Router();
const bcrypt = require('bcrypt');
const pool = require('../db');

const SALT_ROUNDS = 10;

router.post('/register_after_google', async (req, res) => {
  const { email, userId, password, displayName } = req.body;
  if (!email || !userId || !password || !displayName) {
    return res.status(400).json({ success: false, message: '必須項目が不足しています。' });
  }
  try {
    const userCheck = await pool.query(
      'SELECT * FROM users WHERE user_id = $1 OR email = $2',
      [userId, email]
    );
    if (userCheck.rows.length > 0) {
      return res.status(409).json({ success: false, message: 'ユーザーIDまたはメールアドレスは既に使用されています。' });
    }
    const hashedPassword = await bcrypt.hash(password, SALT_ROUNDS);
    await pool.query(
      `INSERT INTO users (user_id, email, password, display_name)
       VALUES ($1, $2, $3, $4)`,
      [userId, email, hashedPassword, displayName]
    );
    res.json({ success: true, message: 'ユーザー登録に成功しました。' });
  } catch (error) {
    console.error('register_after_google error:', error);
    res.status(500).json({ success: false, message: 'サーバーエラーが発生しました。' });
  }
});

module.exports = router;
