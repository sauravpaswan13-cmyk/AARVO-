export async function registerOfferCompletion({ app, pool, requireRole, audit }) {
  if (!pool) return;
  await pool.query(`CREATE TABLE IF NOT EXISTS marketplace_offers (
    id BIGSERIAL PRIMARY KEY,
    code TEXT UNIQUE,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    discount_type TEXT NOT NULL CHECK (discount_type IN ('FLAT_PAISE','PERCENT')),
    discount_value INTEGER NOT NULL CHECK (discount_value > 0),
    min_order_paise BIGINT NOT NULL DEFAULT 0 CHECK (min_order_paise >= 0),
    max_discount_paise BIGINT,
    active BOOLEAN NOT NULL DEFAULT true,
    starts_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
  )`);
  app.get('/v1/offers', { preHandler: requireRole('BUYER') }, async (_request, reply) => {
    return (await pool.query(`SELECT id,code,title,description,discount_type,discount_value,min_order_paise,max_discount_paise,starts_at,ends_at
      FROM marketplace_offers WHERE active=true AND starts_at<=now() AND (ends_at IS NULL OR ends_at>now())
      ORDER BY created_at DESC LIMIT 50`)).rows;
  });
  app.post('/v1/offers/validate', { preHandler: requireRole('BUYER') }, async (request, reply) => {
    const code=String(request.body?.code||'').trim().toUpperCase();
    const subtotal=Number(request.body?.subtotalPaise||0);
    if(!code || !Number.isSafeInteger(subtotal) || subtotal<0) return reply.code(400).send({error:'INVALID_OFFER_REQUEST'});
    const r=await pool.query(`SELECT id,code,title,discount_type,discount_value,min_order_paise,max_discount_paise
      FROM marketplace_offers WHERE code=$1 AND active=true AND starts_at<=now() AND (ends_at IS NULL OR ends_at>now())`,[code]);
    if(!r.rowCount) return reply.code(404).send({error:'OFFER_NOT_FOUND'});
    const o=r.rows[0];
    if(subtotal<Number(o.min_order_paise)) return reply.code(409).send({error:'MIN_ORDER_NOT_MET',minOrderPaise:Number(o.min_order_paise)});
    let discount=o.discount_type==='PERCENT'?Math.floor(subtotal*Number(o.discount_value)/100):Number(o.discount_value);
    if(o.max_discount_paise!=null) discount=Math.min(discount,Number(o.max_discount_paise));
    discount=Math.max(0,Math.min(discount,subtotal));
    await audit(pool,request.user,'OFFER',o.id,'VALIDATED',{code,subtotalPaise:subtotal,discountPaise:discount});
    return {valid:true,code,title:o.title,discountPaise:discount,finalSubtotalPaise:subtotal-discount};
  });
  app.post('/v1/admin/offers', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const b=request.body||{}, code=String(b.code||'').trim().toUpperCase(), title=String(b.title||'').trim().slice(0,120);
    const description=String(b.description||'').trim().slice(0,500), type=String(b.discountType||'').trim().toUpperCase();
    const value=Number(b.discountValue), min=Number(b.minOrderPaise||0), max=b.maxDiscountPaise==null?null:Number(b.maxDiscountPaise);
    if(!/^[A-Z0-9_-]{3,40}$/.test(code)||!title||!['FLAT_PAISE','PERCENT'].includes(type)||!Number.isSafeInteger(value)||value<=0||!Number.isSafeInteger(min)||min<0||(max!=null&&(!Number.isSafeInteger(max)||max<=0))) return reply.code(400).send({error:'INVALID_OFFER'});
    if(type==='PERCENT'&&value>100) return reply.code(400).send({error:'INVALID_PERCENT'});
    try {
      const r=await pool.query(`INSERT INTO marketplace_offers(code,title,description,discount_type,discount_value,min_order_paise,max_discount_paise,ends_at)
        VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING *`,[code,title,description,type,value,min,max,b.endsAt||null]);
      await audit(pool,request.user,'OFFER',r.rows[0].id,'CREATED',{code});
      return reply.code(201).send(r.rows[0]);
    } catch(e){ if(e.code==='23505') return reply.code(409).send({error:'OFFER_CODE_EXISTS'}); throw e; }
  });
}