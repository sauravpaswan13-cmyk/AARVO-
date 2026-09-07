const normalizePhone = (value) => {
  const digits = String(value || '').replace(/\D/g, '');
  const normalized = digits.startsWith('91') && digits.length === 12 ? digits.slice(2) : digits;
  return /^[6-9]\d{9}$/.test(normalized) ? normalized : null;
};

const validImageUrl = (value) => {
  try {
    const raw = String(value || '').trim();
    const url = new URL(raw);
    return ['http:', 'https:'].includes(url.protocol) && raw.length <= 2000;
  } catch {
    return false;
  }
};

const normalizeAddress = (input) => {
  const a = input || {};
  const phone = normalizePhone(a.phone);
  const postalCode = String(a.postalCode ?? a.postal_code ?? '').trim();
  if (!String(a.fullName || '').trim() || !phone || !String(a.line1 || '').trim() || !String(a.city || '').trim() || !String(a.state || '').trim() || !/^\d{6}$/.test(postalCode)) return null;
  return {
    label: String(a.label || 'Home').trim().slice(0, 40),
    fullName: String(a.fullName).trim().slice(0, 120),
    phone,
    line1: String(a.line1).trim().slice(0, 250),
    line2: String(a.line2 || '').trim().slice(0, 250),
    city: String(a.city).trim().slice(0, 100),
    state: String(a.state).trim().slice(0, 100),
    postalCode,
    country: String(a.country || 'IN').trim().toUpperCase().slice(0, 3)
  };
};

export async function registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit }) {
  app.get('/v1/products/:id/images', async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const product = await pool.query('SELECT id FROM products WHERE id=$1 AND is_published=true', [request.params.id]);
    if (!product.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
    return (await pool.query('SELECT id,image_url,alt_text,sort_order,is_primary FROM product_images WHERE product_id=$1 ORDER BY sort_order ASC,id ASC', [request.params.id])).rows;
  });

  app.post('/v1/seller/products/:id/images', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const imageUrl = String(request.body?.imageUrl || '').trim();
    const altText = String(request.body?.altText || '').trim().slice(0, 250);
    const sortOrder = Number(request.body?.sortOrder ?? 0);
    const isPrimary = request.body?.isPrimary !== false;
    if (!validImageUrl(imageUrl) || !Number.isInteger(sortOrder) || sortOrder < 0 || sortOrder > 99) return reply.code(400).send({ error: 'INVALID_PRODUCT_IMAGE' });
    const product = await pool.query('SELECT id FROM products WHERE id=$1 AND seller_id=$2', [request.params.id, request.user.sub]);
    if (!product.rowCount) return reply.code(404).send({ error: 'PRODUCT_NOT_FOUND' });
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      if (isPrimary) await client.query('UPDATE product_images SET is_primary=false WHERE product_id=$1', [request.params.id]);
      const result = await client.query('INSERT INTO product_images(product_id,seller_id,image_url,alt_text,sort_order,is_primary) VALUES($1,$2,$3,$4,$5,$6) RETURNING id,image_url,alt_text,sort_order,is_primary', [request.params.id, request.user.sub, imageUrl, altText, sortOrder, isPrimary]);
      if (isPrimary) await client.query('UPDATE products SET image_url=$1,updated_at=now() WHERE id=$2', [imageUrl, request.params.id]);
      await audit(client, request.user, 'PRODUCT', request.params.id, 'IMAGE_ADDED', { imageId: result.rows[0].id });
      await client.query('COMMIT');
      return reply.code(201).send(result.rows[0]);
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally { client.release(); }
  });

  app.delete('/v1/seller/products/:id/images/:imageId', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const result = await client.query('DELETE FROM product_images WHERE id=$1 AND product_id=$2 AND seller_id=$3 RETURNING id,is_primary', [request.params.imageId, request.params.id, request.user.sub]);
      if (!result.rowCount) { await client.query('ROLLBACK'); return reply.code(404).send({ error: 'PRODUCT_IMAGE_NOT_FOUND' }); }
      if (result.rows[0].is_primary) {
        const replacement = await client.query('SELECT id,image_url FROM product_images WHERE product_id=$1 ORDER BY sort_order ASC,id ASC LIMIT 1', [request.params.id]);
        if (replacement.rowCount) {
          await client.query('UPDATE product_images SET is_primary=true WHERE id=$1', [replacement.rows[0].id]);
          await client.query('UPDATE products SET image_url=$1,updated_at=now() WHERE id=$2 AND seller_id=$3', [replacement.rows[0].image_url, request.params.id, request.user.sub]);
        } else {
          await client.query('UPDATE products SET image_url=NULL,updated_at=now() WHERE id=$1 AND seller_id=$2', [request.params.id, request.user.sub]);
        }
      }
      await audit(client, request.user, 'PRODUCT', request.params.id, 'IMAGE_DELETED', { imageId: Number(request.params.imageId) });
      await client.query('COMMIT');
      return { deleted: true, imageId: Number(request.params.imageId) };
    } catch (error) { await client.query('ROLLBACK'); throw error; } finally { client.release(); }
  });

  app.get('/v1/addresses', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    return (await pool.query('SELECT id,label,full_name,phone,line1,line2,city,state,postal_code,country,is_default,created_at,updated_at FROM buyer_addresses WHERE buyer_id=$1 ORDER BY is_default DESC,updated_at DESC', [request.user.sub])).rows;
  });

  app.post('/v1/addresses', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const address = normalizeAddress(request.body);
    if (!address) return reply.code(400).send({ error: 'INVALID_ADDRESS' });
    const makeDefault = request.body?.isDefault === true;
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const existing = await client.query('SELECT COUNT(*)::int AS count FROM buyer_addresses WHERE buyer_id=$1', [request.user.sub]);
      const isDefault = makeDefault || Number(existing.rows[0].count) === 0;
      if (isDefault) await client.query('UPDATE buyer_addresses SET is_default=false,updated_at=now() WHERE buyer_id=$1', [request.user.sub]);
      const result = await client.query('INSERT INTO buyer_addresses(id,buyer_id,label,full_name,phone,line1,line2,city,state,postal_code,country,is_default) VALUES(gen_random_uuid(),$1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11) RETURNING *', [request.user.sub,address.label,address.fullName,address.phone,address.line1,address.line2,address.city,address.state,address.postalCode,address.country,isDefault]);
      await audit(client, request.user, 'ADDRESS', result.rows[0].id, 'CREATED');
      await client.query('COMMIT');
      return reply.code(201).send(result.rows[0]);
    } catch (error) { await client.query('ROLLBACK'); throw error; } finally { client.release(); }
  });

  app.put('/v1/addresses/:id', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const address = normalizeAddress(request.body);
    if (!address) return reply.code(400).send({ error: 'INVALID_ADDRESS' });
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const current = await client.query('SELECT is_default FROM buyer_addresses WHERE id=$1 AND buyer_id=$2 FOR UPDATE', [request.params.id, request.user.sub]);
      if (!current.rowCount) { await client.query('ROLLBACK'); return reply.code(404).send({ error: 'ADDRESS_NOT_FOUND' }); }
      const requestedDefault = request.body?.isDefault === true;
      const isDefault = requestedDefault || current.rows[0].is_default;
      if (isDefault) await client.query('UPDATE buyer_addresses SET is_default=false,updated_at=now() WHERE buyer_id=$1 AND id<>$2', [request.user.sub, request.params.id]);
      const result = await client.query('UPDATE buyer_addresses SET label=$1,full_name=$2,phone=$3,line1=$4,line2=$5,city=$6,state=$7,postal_code=$8,country=$9,is_default=$10,updated_at=now() WHERE id=$11 AND buyer_id=$12 RETURNING *', [address.label,address.fullName,address.phone,address.line1,address.line2,address.city,address.state,address.postalCode,address.country,isDefault,request.params.id,request.user.sub]);
      await audit(client, request.user, 'ADDRESS', request.params.id, 'UPDATED');
      await client.query('COMMIT');
      return result.rows[0];
    } catch (error) { await client.query('ROLLBACK'); throw error; } finally { client.release(); }
  });

  app.delete('/v1/addresses/:id', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const result = await client.query('DELETE FROM buyer_addresses WHERE id=$1 AND buyer_id=$2 RETURNING is_default', [request.params.id, request.user.sub]);
      if (!result.rowCount) { await client.query('ROLLBACK'); return reply.code(404).send({ error: 'ADDRESS_NOT_FOUND' }); }
      if (result.rows[0].is_default) await client.query('UPDATE buyer_addresses SET is_default=true,updated_at=now() WHERE id=(SELECT id FROM buyer_addresses WHERE buyer_id=$1 ORDER BY updated_at DESC LIMIT 1)', [request.user.sub]);
      await client.query('COMMIT');
      return { deleted: true, addressId: request.params.id };
    } catch (error) { await client.query('ROLLBACK'); throw error; } finally { client.release(); }
  });

  app.post('/v1/addresses/:id/default', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const result = await client.query('SELECT id FROM buyer_addresses WHERE id=$1 AND buyer_id=$2 FOR UPDATE', [request.params.id, request.user.sub]);
      if (!result.rowCount) { await client.query('ROLLBACK'); return reply.code(404).send({ error: 'ADDRESS_NOT_FOUND' }); }
      await client.query('UPDATE buyer_addresses SET is_default=false,updated_at=now() WHERE buyer_id=$1', [request.user.sub]);
      const updated = await client.query('UPDATE buyer_addresses SET is_default=true,updated_at=now() WHERE id=$1 AND buyer_id=$2 RETURNING *', [request.params.id, request.user.sub]);
      await client.query('COMMIT');
      return updated.rows[0];
    } catch (error) { await client.query('ROLLBACK'); throw error; } finally { client.release(); }
  });
}
