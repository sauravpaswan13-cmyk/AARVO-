import { createHmac, timingSafeEqual } from 'node:crypto';

const clean = (v, max=500) => String(v ?? '').trim().slice(0,max);
const enabled = () => String(process.env.THIRD_PARTY_DELIVERY_ENABLED || 'false').toLowerCase() === 'true';
const provider = () => clean(process.env.THIRD_PARTY_DELIVERY_PROVIDER || '',80).toLowerCase();
const baseUrl = () => clean(process.env.THIRD_PARTY_DELIVERY_BASE_URL || '',500).replace(/\\/$/,'');
const createPath = () => clean(process.env.THIRD_PARTY_DELIVERY_CREATE_PATH || '/shipments',500);
const token = () => String(process.env.THIRD_PARTY_DELIVERY_AUTH_TOKEN || '');
const webhookSecret = () => String(process.env.THIRD_PARTY_DELIVERY_WEBHOOK_SECRET || '');

function verifySignature(rawBody, signature) {
  const secret=webhookSecret();
  if(!secret) return false;
  const supplied=clean(signature,512).replace(/^sha256=/,'');
  if(!supplied) return false;
  const expected=createHmac('sha256',secret).update(rawBody).digest('hex');
  try { return timingSafeEqual(Buffer.from(expected),Buffer.from(supplied)); } catch { return false; }
}

async function providerRequest(path, payload) {
  if(!enabled() || !baseUrl() || !token()) throw Object.assign(new Error('DELIVERY_PROVIDER_NOT_CONFIGURED'),{code:'DELIVERY_PROVIDER_NOT_CONFIGURED'});
  const response=await fetch(baseUrl()+path,{
    method:'POST',
    headers:{'content-type':'application/json','authorization':'Bearer '+token()},
    body:JSON.stringify(payload)
  });
  const text=await response.text();
  let data={}; try { data=text ? JSON.parse(text) : {}; } catch { data={raw:text}; }
  if(!response.ok) throw Object.assign(new Error('DELIVERY_PROVIDER_REQUEST_FAILED'),{code:'DELIVERY_PROVIDER_REQUEST_FAILED',status:response.status,data});
  return data;
}

export async function registerThirdPartyDelivery({app,pool,requireRole,audit}) {
  app.get('/v1/delivery/provider-status',{preHandler:requireRole('ADMIN')},async()=>{
    return {enabled:enabled(),provider:provider()||null,configured:Boolean(baseUrl()&&token()),mode:'THIRD_PARTY_ONLY'};
  });

  app.post('/v1/admin/orders/:id/third-party-delivery',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    if(!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    if(!enabled()) return reply.code(503).send({error:'THIRD_PARTY_DELIVERY_DISABLED'});
    const order=await pool.query(`SELECT o.id,o.status,o.address_json,o.total_paise,o.payment_status,
      COALESCE((SELECT json_agg(json_build_object('productId',ol.product_id,'quantity',ol.quantity,'sellerId',ol.seller_id,'lineTotalPaise',ol.line_total_paise)) FROM order_lines ol WHERE ol.order_id=o.id),'[]'::json) AS lines
      FROM orders o WHERE o.id=$1`,[request.params.id]);
    if(!order.rowCount) return reply.code(404).send({error:'ORDER_NOT_FOUND'});
    if(!['CONFIRMED','PACKED','PROCESSING'].includes(String(order.rows[0].status))) return reply.code(409).send({error:'ORDER_NOT_READY_FOR_DELIVERY'});
    const existing=await pool.query('SELECT id,provider,provider_shipment_id,status,tracking_url FROM third_party_delivery_shipments WHERE order_id=$1 ORDER BY created_at DESC LIMIT 1',[request.params.id]);
    if(existing.rowCount && !['FAILED','CANCELLED'].includes(existing.rows[0].status)) return reply.code(409).send({error:'DELIVERY_ALREADY_CREATED',shipment:existing.rows[0]});
    const payload={orderId:String(order.rows[0].id),amountPaise:Number(order.rows[0].total_paise),paymentStatus:order.rows[0].payment_status,address:order.rows[0].address_json,items:order.rows[0].lines,callbackUrl:clean(process.env.THIRD_PARTY_DELIVERY_WEBHOOK_URL||'',1000)};
    const data=await providerRequest(createPath(),payload);
    const shipmentId=clean(data.id||data.shipmentId||data.awb||data.awbNumber||'',200);
    if(!shipmentId) return reply.code(502).send({error:'DELIVERY_PROVIDER_INVALID_RESPONSE'});
    const trackingUrl=clean(data.trackingUrl||data.tracking_url||'',1000);
    const status=clean(data.status||'ASSIGNED',40).toUpperCase();
    const row=await pool.query(`INSERT INTO third_party_delivery_shipments(order_id,provider,provider_shipment_id,status,tracking_url,provider_payload)
      VALUES($1,$2,$3,$4,$5,$6) RETURNING *`,[order.rows[0].id,provider(),shipmentId,status,trackingUrl,JSON.stringify(data)]);
    await pool.query('UPDATE orders SET tracking_json=$1,updated_at=now() WHERE id=$2',[JSON.stringify({status:status,carrier:provider(),shipmentId:shipmentId,trackingUrl:trackingUrl,updatedAt:new Date().toISOString()}),order.rows[0].id]);
    await audit(pool,request.user,'ORDER',order.rows[0].id,'THIRD_PARTY_DELIVERY_CREATED',{provider:provider(),shipmentId});
    return reply.code(201).send(row.rows[0]);
  });

  app.post('/v1/webhooks/delivery/:provider',async(request,reply)=>{
    if(!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    const raw=typeof request.rawBody==='string'?request.rawBody:JSON.stringify(request.body||{});
    if(!verifySignature(raw,request.headers['x-aarvo-delivery-signature'])) return reply.code(401).send({error:'INVALID_DELIVERY_WEBHOOK_SIGNATURE'});
    const data=request.body||{};
    const shipmentId=clean(data.shipmentId||data.shipment_id||data.awb||data.awbNumber||data.id||'',200);
    const next=clean(data.status||'',40).toUpperCase();
    if(!shipmentId||!next) return reply.code(400).send({error:'INVALID_DELIVERY_WEBHOOK'});
    const result=await pool.query('UPDATE third_party_delivery_shipments SET status=$1,tracking_url=COALESCE(NULLIF($2,\'\'),tracking_url),provider_payload=$3,updated_at=now() WHERE provider=$4 AND provider_shipment_id=$5 RETURNING order_id,status,tracking_url',[String(request.params.provider).toLowerCase()===provider()?next:next,clean(data.trackingUrl||data.tracking_url||'',1000),JSON.stringify(data),provider(),shipmentId]);
    if(!result.rowCount) return {received:true,matched:false};
    const mapped={DELIVERED:'DELIVERED',OUT_FOR_DELIVERY:'OUT_FOR_DELIVERY',PICKED_UP:'OUT_FOR_DELIVERY',CANCELLED:'CANCELLED',FAILED:'CANCELLED',ASSIGNED:'PROCESSING',ACCEPTED:'PROCESSING',PACKED:'PACKED'};
    const orderStatus=mapped[next]||'PROCESSING';
    await pool.query('UPDATE orders SET status=$1,tracking_json=$2,updated_at=now() WHERE id=$3',[orderStatus,JSON.stringify({status:orderStatus,carrier:provider(),shipmentId,trackingUrl:result.rows[0].tracking_url,updatedAt:new Date().toISOString()}),result.rows[0].order_id]);
    return {received:true,matched:true};
  });
}
