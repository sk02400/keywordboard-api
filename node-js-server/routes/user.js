const express = require('express');
const router = express.Router();
const db = require('../db'); // pg-pool で接続されたインスタンス

// ユーザー存在確認（メール）
router.get('/exists', async (req, res) => {
    const email = req.query.email;
    const result = await db.query('SELECT user_id, name FROM users WHERE email = $1', [email]);
    if (result.rowCount > 0) {
        res.json({ exists: true, userId: result.rows[0].user_id, name: result.rows[0].name });
    } else {
        res.json({ exists: false });
    }
});

// ユーザー登録
router.post('/register', async (req, res) => {
    const { userId, password, name, email } = req.body;
    try {
        await db.query(
            'INSERT INTO users (user_id, password, name, email) VALUES ($1, $2, $3, $4)',
            [userId, password, name, email]
        );
        res.json({ success: true });
    } catch (e) {
        console.error(e);
        res.json({ success: false, message: '登録エラー' });
    }
});

module.exports = router;
