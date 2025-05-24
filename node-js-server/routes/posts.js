const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/:boardId', async (req, res) => {
    const { boardId } = req.params;
    const result = await db.query(
        'SELECT userName, content, createdAt FROM posts WHERE board_id = $1 ORDER BY createdAt DESC',
        [boardId]
    );
    res.json(result.rows);
});

router.post('/:boardId', async (req, res) => {
    const { boardId } = req.params;
    const { userName, content, createdAt } = req.body;
    await db.query(
        'INSERT INTO posts (board_id, userName, content, createdAt) VALUES ($1, $2, $3, $4)',
        [boardId, userName, content, createdAt]
    );
    res.sendStatus(201);
});

module.exports = router;
