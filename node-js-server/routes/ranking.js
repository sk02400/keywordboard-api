const express = require('express');
const router = express.Router();
const pool = require('../db');
const axios = require('axios');
const cheerio = require('cheerio');
const { getFaviconUrl } = require('../utils/favicon');

router.get('/boards/ranking/day', async (req, res) => {
  try {
    const { rows } = await pool.query(`
      SELECT b.board_id, b.board_name, b.page_title, b.is_link
      FROM boards b
      ORDER BY b.updated_at DESC
      LIMIT 10
    `);

    const result = [];

    for (const board of rows) {
      const postResult = await pool.query(
        'SELECT board_id, post_name, content, created_at FROM posts WHERE board_id = $1 ORDER BY created_at DESC LIMIT 2',
        [board.board_id]
      );

      const posts = postResult.rows;

      let faviconUrl = null;
      if (board.is_link && board.page_title) {
        faviconUrl = getFaviconUrl(board.page_title);
      }

      result.push({
        board_id: board.board_id,
        page_title: board.page_title,
        board_name: board.board_name,
        is_link: board.is_link,
        favicon_url: faviconUrl,
        posts: posts,
      });
    }

    res.json(result);
  } catch (err) {
    console.error('ランキング取得エラー:', err);
    res.status(500).json({ error: 'ランキング取得失敗' });
  }
});

module.exports = router;
