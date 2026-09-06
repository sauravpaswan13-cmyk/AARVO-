import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import crypto from 'crypto';
import { Pool } from 'pg';

const app = express();
app.use(cors());
app.use(express.json());

const pool = new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false });

const PORT = Number(process.env.PORT || 10000);
const ADMIN_EMAIL = String(process.env.ADMIN_EMAIL || '').trim().toLowerCase();

function otpHash(code) {
  return crypto.createHash('sha256').update(String(code)).digest('hex');
}

function generateId() {
  return crypto.randomUUID();
}

app.get('/healthz', async (_req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ service: 'aarvo-api', status: 'ok', database: true, auth: true, payments: false, webhooks: false });
  } catch (error) {
    res.status(503).json({ service: 'aarvo-api', status: 'error', database: false, error: error.message });
  }
});

app.get('/v1/products', async (req, res) => {
  try {
    const q = String(req.query.q || '').trim();
    const category = String(req.query.category || '').trim();
    const values = [];
    const conditions = [];
    if (q) {
      values.push(`%${q}%`);
      conditions.push(`(name ILIKE $${values.length} OR description ILIKE $${values.length})`);
    }
    if (category && category.toLowerCase() !== 'all') {
      values.push(category);
      conditions.push(`category = $${values.length}`);
    }
    const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
    const result = await pool.query(`SELECT * FROM products ${where} ORDER BY created_at DESC LIMIT 100`, values);
    res.json(result.rows);
  } catch (error) {
    console.error('[products]', error);
    res.status(500).json({ error: 'Unable to load products.' });
  }
});

// Phone verification challenge creation.
app.post('/v1/auth/phone/challenge', async (req, res) => {
  try {
    const userId = String(req.body.user_id || '').trim();
    const phone = String(req.body.phone || '').trim();
    const code = String(req.body.code || Math.floor(100000 + Math.random() * 900000));
    if (!userId || !/^\d{10}$/.test(phone) || !/^\d{6}$/.test(code)) {
      return res.status(400).json({ error: 'Invalid user_id, phone, or verification code.' });
    }
    const id = generateId();
    await pool.query(
      `INSERT INTO phone_verification_challenges(id,user_id,phone,code_hash,expires_at)
       VALUES($1,$2,$3,$4,now()+interval '10 minutes')`,
      [id, userId, phone, otpHash(code)]
    );
    res.status(201).json({ id, expires_in_seconds: 600, code: process.env.NODE_ENV === 'production' ? undefined : code });
  } catch (error) {
    console.error('[phone/challenge]', error);
    res.status(500).json({ error: 'Unable to create verification challenge.' });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`aarvo-api listening on ${PORT}`);
  if (ADMIN_EMAIL) console.log(`admin configured: ${ADMIN_EMAIL}`);
});
