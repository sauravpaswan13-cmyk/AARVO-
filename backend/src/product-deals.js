export async function registerProductDeals({ app, pool, requireRole, audit }) {
  if (!pool) return;
  await pool.query(`CREATE TABLE IF NOT EXISTS aarvo_product_deals (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    seller_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    discount_percent INTEGER NOT NULL CHECK (discount_percent BETWEEN 1 AND 80),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    starts_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ends_at TIMESTAMPTZ NOT NULL,
    seller_note TEXT NOT NULL DEFAULT '',
    admin_note TEXT NOT NULL DEFAULT '',
    reviewed_by TEXT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at)
  )`);
  await pool.query(`CREATE INDEX IF NOT EXISTS aarvo_product_deals_live_idx ON aarvo_product_deals(status,starts_at,ends_at)`);
  await pool.query(`CREATE INDEX IF NOT EXISTS aarvo_product_deals_seller_idx ON aarvo_product_deals(seller_id,created_at DESC)`);
  app.get('/v1/deals', async (_request, reply) => {
    const result = await pool.query(`SELECT d.id,d.product_id AS "productId",p.name,p.category,p.description,
      p.price_paise AS "originalPricePaise",
      FLOOR(p.price_paise * (100-d.discount_percent) / 100)::bigint AS "dealPricePaise",
      d.discount_percent AS "discountPercent",d.starts_at AS "startsAt",d.ends_at AS "endsAt",
      p.stock_quantity AS "stockQuantity"
      FROM aarvo_product_deals d JOIN products p ON p.id=d.product_id
      WHERE d.status='APPROVED' AND d.starts_at<=now() AND d.ends_at>now()
      AND p.is_published=true AND p.stock_quantity>0
      ORDER BY d.discount_percent DESC,d.ends_at ASC LIMIT 100`);
    return result.rows;
  });
  app.get('/v1/seller/deals', { preHandler: requireRole('SELLER') }, async (request) => {
    return (await pool.query(`SELECT d.id,d.product_id AS "productId",p.name,p.price_paise AS "originalPricePaise",
      d.discount_percent AS "discountPercent",d.status,d.starts_at AS "startsAt",d.ends_at AS "endsAt",
      d.seller_note AS "sellerNote",d.admin_note AS "adminNote",d.created_at AS "createdAt"
      FROM aarvo_product_deals d JOIN products p ON p.id=d.product_id
      WHERE d.seller_id=$1 ORDER BY d.created_at DESC LIMIT 100`,[request.user.sub])).rows;
  });
  app.post('/v1/seller/deals', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    const b=request.body||{}, productId=Number(b.productId), discount=Number(b.discountPercent);
    const endsAt=new Date(b.endsAt);
    const note=String(b.sellerNote||'').trim().slice(0,500);
    if(!Number.isSafeInteger(productId)||productId<=0||!Number.isInteger(discount)||discount<1||discount>80||!Number.isFinite(endsAt.getTime())||endsAt<=new Date()||endsAt>new Date(Date.now()+90*86400000))
      return reply.code(400).send({error:'INVALID_DEAL_REQUEST',message:'Choose your product, a 1–80% discount, and an end date within 90 days.'});
    const product=await pool.query('SELECT id,seller_id,is_published,price_paise,stock_quantity FROM products WHERE id=$1',[productId]);
    if(!product.rowCount||String(product.rows[0].seller_id)!==String(request.user.sub)) return reply.code(404).send({error:'SELLER_PRODUCT_NOT_FOUND'});
    if(!product.rows[0].is_published||Number(product.rows[0].stock_quantity)<1) return reply.code(409).send({error:'PRODUCT_NOT_ELIGIBLE'});
    const active=await pool.query(`SELECT id FROM aarvo_product_deals WHERE product_id=$1 AND status IN ('PENDING','APPROVED') AND ends_at>now() LIMIT 1`,[productId]);
    if(active.rowCount) return reply.code(409).send({error:'DEAL_ALREADY_PENDING_OR_ACTIVE'});
    const result=await pool.query(`INSERT INTO aarvo_product_deals(product_id,seller_id,discount_percent,ends_at,seller_note)
      VALUES($1,$2,$3,$4,$5) RETURNING id,product_id AS "productId",discount_percent AS "discountPercent",status,ends_at AS "endsAt",created_at AS "createdAt"`,
      [productId,request.user.sub,discount,endsAt.toISOString(),note]);
    await audit(pool,request.user,'PRODUCT_DEAL',result.rows[0].id,'SUBMITTED',{productId,discountPercent:discount});
    return reply.code(201).send(result.rows[0]);
  });
  app.post('/v1/admin/deals', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const b=request.body||{}, productId=Number(b.productId), discount=Number(b.discountPercent), endsAt=new Date(b.endsAt);
    const note=String(b.adminNote||'').trim().slice(0,500);
    if(!Number.isSafeInteger(productId)||productId<=0||!Number.isInteger(discount)||discount<1||discount>80||!Number.isFinite(endsAt.getTime())||endsAt<=new Date()||endsAt>new Date(Date.now()+90*86400000))
      return reply.code(400).send({error:'INVALID_DEAL_REQUEST'});
    const product=await pool.query('SELECT id,seller_id,is_published,stock_quantity FROM products WHERE id=$1',[productId]);
    if(!product.rowCount) return reply.code(404).send({error:'PRODUCT_NOT_FOUND'});
    if(!product.rows[0].is_published||Number(product.rows[0].stock_quantity)<1) return reply.code(409).send({error:'PRODUCT_NOT_ELIGIBLE'});
    const conflict=await pool.query(`SELECT id FROM aarvo_product_deals WHERE product_id=$1 AND status IN ('PENDING','APPROVED') AND ends_at>now() LIMIT 1`,[productId]);
    if(conflict.rowCount) return reply.code(409).send({error:'DEAL_ALREADY_PENDING_OR_ACTIVE'});
    const result=await pool.query(`INSERT INTO aarvo_product_deals(product_id,seller_id,discount_percent,status,ends_at,admin_note,reviewed_by,reviewed_at)
      VALUES($1,$2,$3,'APPROVED',$4,$5,$6,now()) RETURNING id,product_id AS "productId",status,discount_percent AS "discountPercent",ends_at AS "endsAt"`,
      [productId,product.rows[0].seller_id,discount,endsAt.toISOString(),note,request.user.sub]);
    await audit(pool,request.user,'PRODUCT_DEAL',result.rows[0].id,'ADMIN_CREATED',{productId,discountPercent:discount});
    return reply.code(201).send(result.rows[0]);
  });
  app.get('/v1/admin/deals', { preHandler: requireRole('ADMIN') }, async () => {
    return (await pool.query(`SELECT d.id,d.product_id AS "productId",d.seller_id AS "sellerId",
      p.name,p.category,p.price_paise AS "originalPricePaise",d.discount_percent AS "discountPercent",
      FLOOR(p.price_paise * (100-d.discount_percent) / 100)::bigint AS "dealPricePaise",
      d.status,d.starts_at AS "startsAt",d.ends_at AS "endsAt",d.seller_note AS "sellerNote",
      d.admin_note AS "adminNote",d.created_at AS "createdAt",d.reviewed_at AS "reviewedAt"
      FROM aarvo_product_deals d JOIN products p ON p.id=d.product_id
      ORDER BY CASE d.status WHEN 'PENDING' THEN 0 ELSE 1 END,d.created_at DESC LIMIT 200`)).rows;
  });
  app.post('/v1/admin/deals/:id/review', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const decision=String(request.body?.decision||'').toUpperCase();
    const note=String(request.body?.adminNote||'').trim().slice(0,500);
    if(!['APPROVE','REJECT'].includes(decision)) return reply.code(400).send({error:'INVALID_DEAL_DECISION'});
    const client=await pool.connect();
    try {
      await client.query('BEGIN');
      const current=await client.query(`SELECT d.*,p.is_published,p.stock_quantity FROM aarvo_product_deals d JOIN products p ON p.id=d.product_id WHERE d.id=$1 FOR UPDATE OF d,p`,[request.params.id]);
      if(!current.rowCount) { await client.query('ROLLBACK'); return reply.code(404).send({error:'DEAL_NOT_FOUND'}); }
      const deal=current.rows[0];
      if(deal.status!=='PENDING') { await client.query('ROLLBACK'); return reply.code(409).send({error:'DEAL_ALREADY_REVIEWED'}); }
      if(decision==='APPROVE' && (!deal.is_published||Number(deal.stock_quantity)<1||new Date(deal.ends_at)<=new Date())) {
        await client.query('ROLLBACK'); return reply.code(409).send({error:'PRODUCT_OR_DEAL_NOT_ELIGIBLE'});
      }
      if(decision==='APPROVE') {
        const conflict=await client.query(`SELECT id FROM aarvo_product_deals WHERE product_id=$1 AND status='APPROVED' AND starts_at<=now() AND ends_at>now() AND id<>$2 LIMIT 1`,[deal.product_id,deal.id]);
        if(conflict.rowCount){await client.query('ROLLBACK');return reply.code(409).send({error:'ANOTHER_ACTIVE_DEAL_EXISTS'});}
      }
      const status=decision==='APPROVE'?'APPROVED':'REJECTED';
      const updated=await client.query(`UPDATE aarvo_product_deals SET status=$1,admin_note=$2,reviewed_by=$3,reviewed_at=now() WHERE id=$4
        RETURNING id,product_id AS "productId",status,discount_percent AS "discountPercent",ends_at AS "endsAt"`,[status,note,request.user.sub,request.params.id]);
      await audit(client,request.user,'PRODUCT_DEAL',request.params.id,status,{productId:deal.product_id,discountPercent:deal.discount_percent});
      await client.query('COMMIT');
      return updated.rows[0];
    } catch(error) { await client.query('ROLLBACK'); throw error; }
    finally { client.release(); }
  });
}
