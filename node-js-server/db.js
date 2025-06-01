const { Pool } = require('pg');

const pool = new Pool({
  user: 'board_db_2jat_user',
  host: 'dpg-d0tr1pu3jp1c73etd9bg-a',
  database: 'board_db_2jat',
  password: 'NhmaBRj4tteZjcsTojr5pLCxtatIQwNU',
  port: 5432,
});

module.exports = pool;
