const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');

const postRoutes = require('./routes/post');
const userRoutes = require('./routes/user');
const authRoutes = require('./routes/auth');

const app = express();

// ミドルウェア設定
app.use(cors());
app.use(bodyParser.json());

// ルーティング設定
app.use('/api/posts', postRoutes);
app.use('/api/users', userRoutes);
app.use('/api/auth', authRoutes);

// サーバー起動
const PORT = 3000;
app.listen(PORT, () => {
  console.log(`✅ Server running at http://localhost:${PORT}`);
});
