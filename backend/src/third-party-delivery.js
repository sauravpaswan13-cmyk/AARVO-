import { createHmac, timingSafeEqual } from 'node:crypto';

const clean=(v,max=500)=>String(v??'').trim().slice(0,max);
const enabled=()=>String(process.env.THIRD_PARTY_DELIVERY_ENABLED||'false').toLowerCase()==='true';
const provider=()=>clean(process.env.THIRD_PARTY_DELIVERY_PROVIDER||'shiprocket',80).toLowerCase();
const baseUrl=()=>clean(process.env.THIRD_PARTY_DELIVERY_BASE_URL||'https://apiv2.shiprocket.in',500).replace(/\/$/,'');
const token=()=>String(process.env.THIRD_PARTY_DELIVERY_AUTH_TOKEN||'');
const pickupLocation=()=>clean(process.env.SHIPROCKET_PICKUP_LOCATION||'',120);
const webhookSecret=()=>String(process.env.THIRD_PARTY_DELIVERY_WEBHOOK_SECRET||'');

const shiprocketPath={
  create:'/v1/external/orders/create/adhoc',
  awb:'/v1/external/courier/assign/awb',
  pickup:'/v1/external/courier/generate/pickup',
  track:'/v1/external/courier/track/awb/'
};

async function providerRequest(path,payload){
  if(!enabled()||!token()||!baseUrl()) throw Object.assign(new Error('DELIVERY_PROVIDER_NOT_CONFIGURED'),{code:'DELIVERY_PROVIDER_NOT_CONFIGURED'});
  const r=await fetch(baseUrl()+path,{method:'POST',headers:{'content-type':'application/json',authorization:'Bearer '+token()},body:JSON.stringify(payload)});
  const t=await r.text(); let data={}; try{data=t?JSON.parse(t):{}}catch{data={raw:t}}
  if(!r.ok) throw Object.assign(new Error('DELIVERY_PROVIDER_REQUEST_FAILED'),{code:'DELIVERY_PROVIDER_REQUEST_FAILED',status:r.status,data});
  return data;
}

function verifySignature(raw,signature){
  const secret=webhookSecret(); if(!secret)return false;
  const supplied=clean(signature,512).replace(/^sha256=/,''); if(!supplied)return false;
  const expected=createHmac('sha256',secret).update(raw).digest('hex');
  try{return timingSafeEqual(Buffer.from(expected),Buffer.from(supplied))}catch{return false}
}

function splitName(name){
  const parts=clean(name,120).split(/\s+/).filter(Boolean);
  return {first:parts.shift()||'Customer',last:parts.join(' ')};
}

function orderDate(){
  const d=new Date();
  const p=n=>String(n).padStart(2,'0');
  return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate())+' '+p(d.getHours())+':'+p(d.getMinutes());
}

function statusFromShiprocket(data){
  const s=String(data?.tracking_data?.shipment_status||data?.status||'').toLowerCase();
  if(s.includes('deliver'))return 'DELIVERED';
  if(s.includes('out for'))return 'OUT_FOR_DELIVERY';
  if(s.includes('pick'))return 'OUT_FOR_DELIVERY';
  if(s.includes('cancel'))return 'CANCELLED';
  if(s.includes('rto')||s.includes('return'))return 'RTO';
  return 'PROCESSING';
}

export async function createThirdPartyShipmentForOrder({pool,audit,orderId,actor=null}) {
  if(!pool) throw Object.assign(new Error('DATABASE_NOT_CONFIGURED'),{code:'DATABASE_NOT_CONFIGURED',status:503});
  if(!enabled()) throw Object.assign(new Error('THIRD_PARTY_DELIVERY_DISABLED'),{code:'THIRD_PARTY_DELIVERY_DISABLED',status:503});
  if(provider()!=='shiprocket') throw Object.assign(new Error('UNSUPPORTED_DELIVERY_PROVIDER'),{code:'UNSUPPORTED_DELIVERY_PROVIDER',status:400});
  if(!token()||!pickupLocation()) throw Object.assign(new Error('SHIPROCKET_NOT_CONFIGURED'),{code:'SHIPROCKET_NOT_CONFIGURED',status:503});

  const q=await pool.query(`SELECT o.id,o.status,o.address_json,o.total_paise,o.subtotal_paise,o.delivery_fee_paise,o.payment_status,
      u.email AS buyer_email,
      COALESCE((SELECT json_agg(json_build_object('productId',ol.product_id,'quantity',ol.quantity,'lineTotalPaise',ol.line_total_paise)) FROM order_lines ol WHERE ol.order_id=o.id),'[]'::json) AS lines
      FROM orders o LEFT JOIN users u ON u.id=o.buyer_id WHERE o.id=$1`,[orderId]);
  if(!q.rowCount)throw Object.assign(new Error('ORDER_NOT_FOUND'),{code:'ORDER_NOT_FOUND',status:404});
  const o=q.rows[0];
  if(!['CONFIRMED','PACKED','PROCESSING','PAID'].includes(String(o.status)))throw Object.assign(new Error('ORDER_NOT_READY_FOR_DELIVERY'),{code:'ORDER_NOT_READY_FOR_DELIVERY',status:409});
  const a=o.address_json||{};
  if(!a.fullName||!a.phone||!a.line1||!a.city||!a.state||!a.postalCode)throw Object.assign(new Error('ORDER_ADDRESS_INCOMPLETE'),{code:'ORDER_ADDRESS_INCOMPLETE',status:409});

  const existing=await pool.query('SELECT * FROM third_party_delivery_shipments WHERE order_id=$1 ORDER BY created_at DESC LIMIT 1',[o.id]);
  if(existing.rowCount&&!['FAILED','CANCELLED'].includes(String(existing.rows[0].status)))throw Object.assign(new Error('DELIVERY_ALREADY_CREATED'),{code:'DELIVERY_ALREADY_CREATED',status:409,shipment:existing.rows[0]});

  const lines=Array.isArray(o.lines)?o.lines:[];
  const productIds=lines.map(x=>Number(x.productId)).filter(Number.isFinite);
  const products=productIds.length?await pool.query('SELECT id,name,price_paise FROM products WHERE id=ANY($1::bigint[])',[productIds]):{rows:[]};
  const byId=new Map(products.rows.map(p=>[Number(p.id),p]));
  const items=lines.map(x=>{const p=byId.get(Number(x.productId));return {name:clean(p?.name||('AARVO Product '+x.productId),120),sku:clean(String(x.productId),80),units:Number(x.quantity)||1,selling_price:(Number(p?.price_paise||x.lineTotalPaise||0)/100).toFixed(2),discount:0,tax:0,hsn:''}});
  if(!items.length)throw Object.assign(new Error('DELIVERY_ITEMS_MISSING'),{code:'DELIVERY_ITEMS_MISSING',status:409});

  const customer=splitName(a.fullName);
  const total=Number(o.total_paise||0)/100;
  const subtotal=Number(o.subtotal_paise||0)/100;
  const shipping=Number(o.delivery_fee_paise||0)/100;
  const payload={
    order_id:String(o.id).replace(/-/g,'').slice(0,50),
    order_date:orderDate(),
    pickup_location:pickupLocation(),
    billing_customer_name:customer.first,
    billing_last_name:customer.last,
    billing_address:clean(a.line1,250),
    billing_address_2:clean(a.line2,250),
    billing_city:clean(a.city,30),
    billing_pincode:Number(a.postalCode),
    billing_state:clean(a.state,100),
    billing_country:'India',
    billing_email:clean(q.rows[0].buyer_email||'no-reply@aarvo.in',150),
    billing_phone:Number(a.phone),
    shipping_is_billing:true,
    order_items:items,
    payment_method:String(o.payment_status||'').toUpperCase()==='COD'?'COD':'Prepaid',
    shipping_charges:shipping,
    giftwrap_charges:0,
    transaction_charges:0,
    total_discount:0,
    sub_total:subtotal,
    length:Number(process.env.SHIPROCKET_PACKAGE_LENGTH_CM||10),
    breadth:Number(process.env.SHIPROCKET_PACKAGE_BREADTH_CM||10),
    height:Number(process.env.SHIPROCKET_PACKAGE_HEIGHT_CM||10),
    weight:Number(process.env.SHIPROCKET_PACKAGE_WEIGHT_KG||0.5)
  };

  const created=await providerRequest(shiprocketPath.create,payload);
  const srOrderId=created.order_id||created.orderId||created.id;
  const srShipmentId=created.shipment_id||created.shipmentId||created.response?.shipment_id;
  if(!srOrderId||!srShipmentId)throw Object.assign(new Error('SHIPROCKET_INVALID_CREATE_RESPONSE'),{code:'SHIPROCKET_INVALID_CREATE_RESPONSE',status:502,data:created});

  const awb=await providerRequest(shiprocketPath.awb,{shipment_id:Number(srShipmentId)});
  const awbData=awb?.response?.data||awb?.data||awb?.response||awb;
  const awbCode=clean(awbData?.awb_code||awbData?.awbCode||'',120);
  const courier=clean(awbData?.courier_name||awbData?.courierName||'',120);
  if(!awbCode)throw Object.assign(new Error('SHIPROCKET_AWB_ASSIGNMENT_FAILED'),{code:'SHIPROCKET_AWB_ASSIGNMENT_FAILED',status:502,data:awb});

  const pickup=await providerRequest(shiprocketPath.pickup,{shipment_id:[Number(srShipmentId)]});
  const trackingUrl=awbCode?'https://shiprocket.co/tracking/'+encodeURIComponent(awbCode):'';
  const payloadStored={create:created,awb,pickup};

  const row=await pool.query(`INSERT INTO third_party_delivery_shipments(order_id,provider,provider_order_id,provider_shipment_id,awb,courier_name,status,tracking_url,provider_payload)
      VALUES($1,'shiprocket',$2,$3,$4,$5,'OUT_FOR_PICKUP',$6,$7)
      RETURNING *`,[o.id,String(srOrderId),String(srShipmentId),awbCode,courier,trackingUrl,JSON.stringify(payloadStored)]);
  await pool.query('UPDATE orders SET tracking_json=$1,updated_at=now() WHERE id=$2',[JSON.stringify({status:'OUT_FOR_PICKUP',carrier:courier||'Shiprocket',provider:'shiprocket',providerOrderId:String(srOrderId),shipmentId:String(srShipmentId),awb:awbCode,trackingUrl,updatedAt:new Date().toISOString()}),o.id]);
  await audit(pool,actor,'ORDER',o.id,'THIRD_PARTY_DELIVERY_CREATED',{provider:'shiprocket',providerOrderId:String(srOrderId),shipmentId:String(srShipmentId),awb:awbCode,courier});
  return row.rows[0];
}

export async function registerThirdPartyDelivery({app,pool,requireRole,audit}){
  app.get('/v1/delivery/provider-status',{preHandler:requireRole('ADMIN')},async()=>({
    enabled:enabled(),provider:provider(),configured:Boolean(token()&&baseUrl()&&(!['shiprocket'].includes(provider())||pickupLocation())),
    mode:'THIRD_PARTY_ONLY',pickupLocation:provider()==='shiprocket'?pickupLocation()||null:null
  }));

  app.post('/v1/admin/orders/:id/third-party-delivery',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    try { return reply.code(201).send(await createThirdPartyShipmentForOrder({pool,audit,orderId:request.params.id,actor:request.user})); }
    catch(error) {
      if(error.code==='DELIVERY_ALREADY_CREATED') return reply.code(409).send({error:error.code,shipment:error.shipment});
      if(error.status) return reply.code(error.status).send({error:error.code,providerResponse:error.data});
      throw error;
    }
  });

  app.post('/v1/webhooks/delivery/:provider',async(request,reply)=>{
    if(!pool)return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    if(String(request.params.provider).toLowerCase()!==provider())return reply.code(404).send({error:'DELIVERY_PROVIDER_NOT_FOUND'});
    const raw=typeof request.rawBody==='string'?request.rawBody:JSON.stringify(request.body||{});
    if(webhookSecret()&&!verifySignature(raw,request.headers['x-aarvo-delivery-signature']))return reply.code(401).send({error:'INVALID_DELIVERY_WEBHOOK_SIGNATURE'});
    const data=request.body||{};
    const shipmentId=clean(data.shipment_id||data.shipmentId||data.awb||data.awb_code||data.id||data?.tracking_data?.shipment_id,200);
    const awb=clean(data.awb||data.awb_code||data?.tracking_data?.shipment_track?.[0]?.awb_code,120);
    const next=statusFromShiprocket(data);
    const result=await pool.query(`UPDATE third_party_delivery_shipments SET status=$1,awb=COALESCE(NULLIF($2,''),awb),tracking_url=COALESCE(NULLIF($3,''),tracking_url),provider_payload=$4,updated_at=now()
      WHERE provider='shiprocket' AND (provider_shipment_id=$5 OR awb=$6) RETURNING order_id,awb,courier_name,tracking_url`,[next,awb,awb?'https://shiprocket.co/tracking/'+encodeURIComponent(awb):'',JSON.stringify(data),shipmentId,awb]);
    if(!result.rowCount)return {received:true,matched:false};
    await pool.query('UPDATE orders SET status=$1,tracking_json=$2,updated_at=now() WHERE id=$3',[next==='RTO'?'CANCELLED':next,JSON.stringify({status:next,carrier:result.rows[0].courier_name||'Shiprocket',provider:'shiprocket',awb:result.rows[0].awb,trackingUrl:result.rows[0].tracking_url,updatedAt:new Date().toISOString()}),result.rows[0].order_id]);
    return {received:true,matched:true};
  });
}
