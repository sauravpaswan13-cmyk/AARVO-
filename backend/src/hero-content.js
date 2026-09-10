export async function registerHeroContent({ app, pool, requireRole }) {
  if (!pool) return;

  await pool.query(`
    CREATE TABLE IF NOT EXISTS home_hero_slides (
      id BIGSERIAL PRIMARY KEY,
      image_url TEXT NOT NULL,
      title TEXT NOT NULL DEFAULT '',
      subtitle TEXT NOT NULL DEFAULT '',
      cta_label TEXT NOT NULL DEFAULT '',
      cta_target TEXT NOT NULL DEFAULT '',
      sort_order INTEGER NOT NULL DEFAULT 0,
      is_active BOOLEAN NOT NULL DEFAULT true,
      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    )
  `);

  const count = await pool.query('SELECT COUNT(*)::int AS count FROM home_hero_slides');
  if (Number(count.rows[0]?.count || 0) === 0) {
    const seed = [
      ['https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=1400&q=85', 'AARVO Premium Picks', 'Discover products worth bringing home.', 'Shop now', ''],
      ['https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=1400&q=85', 'New Season Style', 'Bold looks. Premium feel.', 'Explore fashion', 'Fashion'],
      ['https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1400&q=85', 'Smart Shopping', 'Great products, simple checkout.', 'Shop deals', '']
    ];
    for (let i = 0; i < seed.length; i++) {
      await pool.query(`INSERT INTO home_hero_slides(image_url,title,subtitle,cta_label,cta_target,sort_order,is_active) VALUES($1,$2,$3,$4,$5,$6,true)`, [...seed[i], i]);
    }
  }

  app.get('/v1/home/hero', async (_request, reply) => {
    const result = await pool.query(`SELECT id,image_url,title,subtitle,cta_label,cta_target,sort_order FROM home_hero_slides WHERE is_active=true ORDER BY sort_order ASC,id ASC`);
    return result.rows;
  });

  app.get('/v1/admin/home/hero', { preHandler: requireRole('ADMIN') }, async () => {
    const result = await pool.query(`SELECT id,image_url,title,subtitle,cta_label,cta_target,sort_order,is_active,created_at,updated_at FROM home_hero_slides ORDER BY sort_order ASC,id ASC`);
    return result.rows;
  });

  app.post('/v1/admin/home/hero', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const body = request.body || {};
    const imageUrl = String(body.imageUrl || '').trim();
    if (!/^https?:\/\//i.test(imageUrl)) return reply.code(400).send({ error: 'INVALID_IMAGE_URL' });
    const result = await pool.query(`INSERT INTO home_hero_slides(image_url,title,subtitle,cta_label,cta_target,sort_order,is_active) VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING *`, [imageUrl, String(body.title || '').trim(), String(body.subtitle || '').trim(), String(body.ctaLabel || '').trim(), String(body.ctaTarget || '').trim(), Number.isInteger(Number(body.sortOrder)) ? Number(body.sortOrder) : 0, body.isActive !== false]);
    return result.rows[0];
  });

  app.put('/v1/admin/home/hero/:id', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const body = request.body || {};
    const imageUrl = String(body.imageUrl || '').trim();
    if (!/^https?:\/\//i.test(imageUrl)) return reply.code(400).send({ error: 'INVALID_IMAGE_URL' });
    const result = await pool.query(`UPDATE home_hero_slides SET image_url=$1,title=$2,subtitle=$3,cta_label=$4,cta_target=$5,sort_order=$6,is_active=$7,updated_at=now() WHERE id=$8 RETURNING *`, [imageUrl, String(body.title || '').trim(), String(body.subtitle || '').trim(), String(body.ctaLabel || '').trim(), String(body.ctaTarget || '').trim(), Number.isInteger(Number(body.sortOrder)) ? Number(body.sortOrder) : 0, body.isActive !== false, request.params.id]);
    if (!result.rowCount) return reply.code(404).send({ error: 'HERO_NOT_FOUND' });
    return result.rows[0];
  });

  app.delete('/v1/admin/home/hero/:id', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const result = await pool.query('DELETE FROM home_hero_slides WHERE id=$1 RETURNING id', [request.params.id]);
    if (!result.rowCount) return reply.code(404).send({ error: 'HERO_NOT_FOUND' });
    return { deleted: true, id: result.rows[0].id };
  });
}
