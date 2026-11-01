import { timingSafeEqual } from 'node:crypto';

const clean=(v,max=500)=>String(v??'').trim().slice(0,max);
const enabled=()=>String(process.env.THIRD_PARTY_DELIVERY_ENABLED||'false').toLowerCase()==='true';
const provider=()=>clean(process.env.THIRD_PARTY_DELIVERY_PROVIDER||'shiprocket',80).toLowerCase();
const baseUrl=()=>clean(process.env.THIRD_PARTY_DELIVERY_BASE_URL||'https://apiv2.shiprocket.in',500).replace(/\/$/,'');
const staticToken=()=>String(process.env.THIRD_PARTY_DELIVERY_AUTH_TOKEN||'');
const apiEmail=()=>clean(process.env.SHIPROCKET_API_EMAIL||'',254);
const apiPassword=()=>String(process.env.SHIPROCKET_API_PASSWORD||'');
const pickupLocation=()=>clean(process.env.SHIPROCKET_PICKUP_LOCATION||'',120);
const webhookSecret=()=>String(process.env.THIRD_PARTY_DELIVERY_WEBHOOK_SECRET||'');
let cachedToken='';
let cachedTokenExpiresAt=0;
let loginInFlight=null;

const shiprocketPath={
  login:'/v1/external/auth/login',
  create:'/v1/external/orders/create/adhoc',
  awb:'/v1/external/courier/assign/awb',
  pickup:'/v1/external/courier/generate/pickup',
  listPickups:'/v1/external/settings/company/pickup',
  addPickup:'/v1/external/settings/company/addpickup',
  track:'/v1/external/courier/track/awb/'
};

async function readResponse(r){
  const t=await r.text(); let data={}; try{data=t?JSON.parse(t):{}}catch{data={raw:t}}
  return data;
}

async function loginShiprocket(force=false){
  if(!apiEmail()||!apiPassword()){
    if(staticToken()) return staticToken();
    throw Object.assign(new Error('SHIPROCKET_CREDENTIALS_NOT_CONFIGURED'),{code:'SHIPROCKET_CREDENTIALS_NOT_CONFIGURED',status:503});
  }
  if(!force&&cachedToken&&Date.now()<cachedTokenExpiresAt)return cachedToken;
  if(loginInFlight)return loginInFlight;
  loginInFlight=(async()=>{
    const r=await fetch(baseUrl()+shiprocketPath.login,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({email:apiEmail(),password:apiPassword()})});
    const data=await readResponse(r);
    const fresh=String(data?.token||data?.data?.token||'');
    if(!r.ok||!fresh)throw Object.assign(new Error('SHIPROCKET_LOGIN_FAILED'),{code:'SHIPROCKET_LOGIN_FAILED',status:r.ok?502:r.status,data:{message:data?.message||data?.errors||'Shiprocket authentication failed'}});
    cachedToken=fresh;
    cachedTokenExpiresAt=Date.now()+9*24*60*60*1000;
    return cachedToken;
  })();
  try{return await loginInFlight;}finally{loginInFlight=null;}
}

async function providerRequest(path,payload,method='POST'){
  if(!enabled()||!baseUrl()) throw Object.assign(new Error('DELIVERY_PROVIDER_NOT_CONFIGURED'),{code:'DELIVERY_PROVIDER_NOT_CONFIGURED',status:503});
  let authToken=await loginShiprocket();
  const send=token=>fetch(baseUrl()+path,{method,headers:{'content-type':'application/json',authorization:'Bearer '+token},...(payload===undefined?{}:{body:JSON.stringify(payload)})});
  let r=await send(authToken); let data=await readResponse(r);
  if(r.status===401&&apiEmail()&&apiPassword()){
    authToken=await loginShiprocket(true); r=await send(authToken); data=await readResponse(r);
  }
  if(!r.ok) throw Object.assign(new Error('DELIVERY_PROVIDER_REQUEST_FAILED'),{code:'DELIVERY_PROVIDER_REQUEST_FAILED',status:r.status,data:{endpoint:path,message:data?.message||data?.errors||data?.raw||'Shiprocket request failed'}});
  return data;
}

function verifyWebhookToken(supplied){
  const expected=webhookSecret();
  const received=String(supplied||'').trim();
  if(!expected||!received)return false;
  const a=Buffer.from(expected,'utf8'),b=Buffer.from(received,'utf8');
  return a.length===b.length&&timingSafeEqual(a,b);
}
function splitName(name){
  const parts=clean(name,120).split(/\s+/).filter(Boolean);
  return {first:parts.shift()||'Customer',last:parts.join(' ')};
}
function orderDate(){
  const d=new Date(); const p=n=>String(n).padStart(2,'0');
  return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate())+' '+p(d.getHours())+':'+p(d.getMinutes());
}
function statusFromShiprocket(data){
  const s=String(data?.tracking_data?.shipment_status||data?.status||data?.current_status||'').toLowerCase();
  if(s.includes('deliver'))return 'DELIVERED';
  if(s.includes('out for'))return 'OUT_FOR_DELIVERY';
  if(s.includes('pick'))return 'OUT_FOR_PICKUP';
  if(s.includes('cancel'))return 'CANCELLED';
  if(s.includes('rto')||s.includes('return'))return 'RTO';
  if(s.includes('transit'))return 'IN_TRANSIT';
  return 'PROCESSING';
}
function pickupNickname(sellerId){
  return ('AARVO'+String(sellerId).replace(/[^a-z0-9]/gi,'')).slice(0,36);
}
function pickupRows(data){
  const candidates=[data?.data?.shipping_address,data?.shipping_address,data?.data?.pickup_locations,data?.pickup_locations,data?.data,data];
  return candidates.find(Array.isArray)||[];
}
async function ensureSellerPickup(seller){
  const name=clean(seller.business_name||seller.name||'AARVO Seller',80);
  const email=clean(seller.business_email||'',254);
  const phone=clean(seller.pickup_phone||seller.phone||'',20).replace(/\D/g,'');
  const address=clean(seller.pickup_address||'',80);
  const city=clean(seller.pickup_city||'',80);
  const state=clean(seller.pickup_state||'',80);
  const pin=clean(seller.pickup_postal_code||'',6);
  if(!email||!phone||phone.length<10||phone.length>13||!address||address.length<3||!city||!state||!/^\d{6}$/.test(pin)){
    throw Object.assign(new Error('SELLER_PICKUP_ADDRESS_INCOMPLETE'),{code:'SELLER_PICKUP_ADDRESS_INCOMPLETE',status:409,data:{sellerId:seller.seller_id,required:['seller account/business email','pickup phone','pickup address','city','state','6-digit PIN']}});
  }
  const nickname=pickupNickname(seller.seller_id);
  const listed=await providerRequest(shiprocketPath.listPickups,undefined,'GET');
  const existing=pickupRows(listed).find(x=>String(x?.pickup_location||x?.pickupLocation||x?.name||'').toLowerCase()===nickname.toLowerCase());
  if(existing)return nickname;
  const added=await providerRequest(shiprocketPath.addPickup,{
    pickup_location:nickname,name:clean(name,80),email,phone:Number(phone),
    address,address_2:clean(seller.pickup_address_2||'',80),city,state,country:'India',pin_code:Number(pin)
  });
  if(added?.status===0||added?.status===false)throw Object.assign(new Error('SHIPROCKET_SELLER_PICKUP_CREATE_FAILED'),{code:'SHIPROCKET_SELLER_PICKUP_CREATE_FAILED',status:502,data:{sellerId:seller.seller_id,message:added?.message||'Shiprocket rejected seller pickup location'}});
  return nickname;
}

export async function createThirdPartyShipmentForOrder({pool,audit,orderId,actor=null,sellerId=null}) {
  if(!pool) throw Object.assign(new Error('DATABASE_NOT_CONFIGURED'),{code:'DATABASE_NOT_CONFIGURED',status:503});
  if(!enabled()) throw Object.assign(new Error('THIRD_PARTY_DELIVERY_DISABLED'),{code:'THIRD_PARTY_DELIVERY_DISABLED',status:503});
  if(provider()!=='shiprocket') throw Object.assign(new Error('UNSUPPORTED_DELIVERY_PROVIDER'),{code:'UNSUPPORTED_DELIVERY_PROVIDER',status:400});
  if((!staticToken()&&(!apiEmail()||!apiPassword()))||!baseUrl()) throw Object.assign(new Error('SHIPROCKET_NOT_CONFIGURED'),{code:'SHIPROCKET_NOT_CONFIGURED',status:503});

  const q=await pool.query(`SELECT o.id,o.status,o.address_json,o.total_paise,o.subtotal_paise,o.delivery_fee_paise,o.payment_status,
      u.email AS buyer_email,
      COALESCE((SELECT json_agg(json_build_object('productId',ol.product_id,'quantity',ol.quantity,'unitPricePaise',ol.unit_price_paise,'lineTotalPaise',(ol.unit_price_paise * ol.quantity))) FROM order_lines ol WHERE ol.order_id=o.id),'[]'::json) AS lines
      FROM orders o LEFT JOIN users u ON u.id=o.buyer_id WHERE o.id=$1`,[orderId]);
  if(!q.rowCount)throw Object.assign(new Error('ORDER_NOT_FOUND'),{code:'ORDER_NOT_FOUND',status:404});
  const o=q.rows[0];
  if(!['CONFIRMED','PACKED','PROCESSING','PAID'].includes(String(o.status)))throw Object.assign(new Error('ORDER_NOT_READY_FOR_DELIVERY'),{code:'ORDER_NOT_READY_FOR_DELIVERY',status:409});
  const a=o.address_json||{};
  if(!a.fullName||!a.phone||!a.line1||!a.city||!a.state||!a.postalCode)throw Object.assign(new Error('ORDER_ADDRESS_INCOMPLETE'),{code:'ORDER_ADDRESS_INCOMPLETE',status:409});
  const lines=Array.isArray(o.lines)?o.lines:[];
  const productIds=lines.map(x=>Number(x.productId)).filter(Number.isFinite);
  const products=productIds.length?await pool.query('SELECT id,seller_id,name,price_paise FROM products WHERE id=ANY($1::bigint[])',[productIds]):{rows:[]};
  const byId=new Map(products.rows.map(p=>[Number(p.id),p]));
  const groups=new Map();
  for(const line of lines){
    const product=byId.get(Number(line.productId));
    if(!product)throw Object.assign(new Error('DELIVERY_PRODUCT_NOT_FOUND'),{code:'DELIVERY_PRODUCT_NOT_FOUND',status:409});
    const sellerId=String(product.seller_id||'');
    if(!sellerId)throw Object.assign(new Error('DELIVERY_SELLER_NOT_FOUND'),{code:'DELIVERY_SELLER_NOT_FOUND',status:409});
    if(!groups.has(sellerId))groups.set(sellerId,[]);
    groups.get(sellerId).push({line,product});
  }
  if(!groups.size)throw Object.assign(new Error('DELIVERY_ITEMS_MISSING'),{code:'DELIVERY_ITEMS_MISSING',status:409});
  const packed=await pool.query("SELECT DISTINCT actor_id::text AS seller_id FROM delivery_events WHERE order_id=$1 AND status='PACKED' AND actor_id IS NOT NULL",[o.id]);
  const packedSellerIds=new Set(packed.rows.map(x=>String(x.seller_id)));
  for(const id of [...groups.keys()]) if(!packedSellerIds.has(id)||(sellerId&&String(sellerId)!==id)) groups.delete(id);
  if(!groups.size)throw Object.assign(new Error('NO_SELLER_PACKED_FOR_DELIVERY'),{code:'NO_SELLER_PACKED_FOR_DELIVERY',status:409});
  const sellerIds=[...groups.keys()];
  const sellerResult=await pool.query(`SELECT sp.seller_id,COALESCE(NULLIF(sp.pickup_phone,''),sp.phone) AS phone,COALESCE(NULLIF(sp.business_name,''),u.display_name,'AARVO Seller') AS business_name,COALESCE(NULLIF(sp.business_email,''),u.email) AS business_email,sp.pickup_address,sp.pickup_city,sp.pickup_state,sp.pickup_postal_code FROM seller_profiles sp LEFT JOIN users u ON u.id=sp.seller_id WHERE sp.seller_id=ANY($1::text[])`,[sellerIds]);
  const sellerMap=new Map(sellerResult.rows.map(s=>[String(s.seller_id),s]));
  const customer=splitName(a.fullName);
  const shipping=Number(o.delivery_fee_paise||0)/100;
  const createdRows=[];
  let sellerIndex=0;
  for(const [sellerKey,sellerLines] of groups){
    const sellerId=sellerKey;
    const currentSellerIndex=sellerIndex++;
    const existing=await pool.query('SELECT * FROM third_party_delivery_shipments WHERE order_id=$1 AND seller_id=$2 ORDER BY created_at DESC LIMIT 1',[o.id,sellerId]);
    if(existing.rowCount&&!['FAILED','CANCELLED'].includes(String(existing.rows[0].status))){createdRows.push(existing.rows[0]);continue;}
    const seller=sellerMap.get(sellerId);
    if(!seller)throw Object.assign(new Error('SELLER_PROFILE_REQUIRED_FOR_SHIPPING'),{code:'SELLER_PROFILE_REQUIRED_FOR_SHIPPING',status:409,data:{sellerId}});
    const pickup=await ensureSellerPickup(seller);
    const items=sellerLines.map(({line,product})=>({
      name:clean(product.name||('AARVO Product '+line.productId),120),sku:clean(String(line.productId),80),
      units:Number(line.quantity)||1,selling_price:(Number(line.unitPricePaise||0)/100).toFixed(2),discount:0,tax:0,hsn:''
    }));
    const subtotalPaise=sellerLines.reduce((sum,{line})=>sum+Number(line.lineTotalPaise||0),0);
    const sellerOrderId=('AARVO'+String(o.id).replace(/-/g,'')+'S'+String(sellerId).replace(/-/g,'')).slice(0,50);
    const packageLength=Number(process.env.SHIPROCKET_PACKAGE_LENGTH_CM||0);
    const packageBreadth=Number(process.env.SHIPROCKET_PACKAGE_BREADTH_CM||0);
    const packageHeight=Number(process.env.SHIPROCKET_PACKAGE_HEIGHT_CM||0);
    const packageWeight=Number(process.env.SHIPROCKET_PACKAGE_WEIGHT_KG||0);
    if(![packageLength,packageBreadth,packageHeight,packageWeight].every(v=>Number.isFinite(v)&&v>0))
      throw Object.assign(new Error('SHIPROCKET_PACKAGE_DIMENSIONS_REQUIRED'),{code:'SHIPROCKET_PACKAGE_DIMENSIONS_REQUIRED',status:409,data:{required:['SHIPROCKET_PACKAGE_LENGTH_CM','SHIPROCKET_PACKAGE_BREADTH_CM','SHIPROCKET_PACKAGE_HEIGHT_CM','SHIPROCKET_PACKAGE_WEIGHT_KG']}});
    const payload={
      order_id:sellerOrderId,order_date:orderDate(),pickup_location:pickup,
      billing_customer_name:customer.first,billing_last_name:customer.last,
      billing_address:clean(a.line1,250),billing_address_2:clean(a.line2,250),
      billing_city:clean(a.city,30),billing_pincode:Number(a.postalCode),billing_state:clean(a.state,100),billing_country:'India',
      billing_email:clean(q.rows[0].buyer_email||'no-reply@aarvo.in',150),billing_phone:Number(String(a.phone).replace(/\D/g,'')),
      shipping_is_billing:true,order_items:items,
      payment_method:String(o.payment_status||'').toUpperCase()==='COD'?'COD':'Prepaid',
      shipping_charges:currentSellerIndex===0?shipping:0,giftwrap_charges:0,transaction_charges:0,total_discount:0,
      sub_total:subtotalPaise/100,
      length:packageLength,breadth:packageBreadth,height:packageHeight,weight:packageWeight
    };
    const created=await providerRequest(shiprocketPath.create,payload);
    const srOrderId=created.order_id||created.orderId||created.id;
    const srShipmentId=created.shipment_id||created.shipmentId||created.response?.shipment_id;
    if(!srOrderId||!srShipmentId)throw Object.assign(new Error('SHIPROCKET_INVALID_CREATE_RESPONSE'),{code:'SHIPROCKET_INVALID_CREATE_RESPONSE',status:502,data:{sellerId,response:created}});
    const awb=await providerRequest(shiprocketPath.awb,{shipment_id:Number(srShipmentId)});
    const awbData=awb?.response?.data||awb?.data||awb?.response||awb;
    const awbCode=clean(awbData?.awb_code||awbData?.awbCode||'',120);
    const courier=clean(awbData?.courier_name||awbData?.courierName||'',120);
    if(!awbCode)throw Object.assign(new Error('SHIPROCKET_AWB_ASSIGNMENT_FAILED'),{code:'SHIPROCKET_AWB_ASSIGNMENT_FAILED',status:502,data:{sellerId,response:awb}});
    const pickupResponse=await providerRequest(shiprocketPath.pickup,{shipment_id:[Number(srShipmentId)]});
    const trackingUrl='https://shiprocket.co/tracking/'+encodeURIComponent(awbCode);
    const stored={create:created,awb,pickup:pickupResponse,sellerId,pickupLocation:pickup};
    const row=await pool.query(`INSERT INTO third_party_delivery_shipments(order_id,seller_id,provider,provider_order_id,provider_shipment_id,awb,courier_name,status,tracking_url,provider_payload)
      VALUES($1,$2,'shiprocket',$3,$4,$5,$6,'OUT_FOR_PICKUP',$7,$8) RETURNING *`,
      [o.id,sellerId,String(srOrderId),String(srShipmentId),awbCode,courier,trackingUrl,JSON.stringify(stored)]);
    createdRows.push(row.rows[0]);
    // Manifest generation is required by Shiprocket after AWB + pickup. Persist
    // the shipment first so a manifest API failure can never cause a duplicate order.
    let manifest=null;
    try {
      manifest=await providerRequest('/v1/external/manifests/generate',{shipment_id:[Number(srShipmentId)]});
    } catch (error) {
      manifest={error:error.code||'SHIPROCKET_MANIFEST_FAILED',message:String(error.message||'Manifest generation failed').slice(0,200)};
    }
    const storedPayload={...stored,manifest};
    const saved=await pool.query('UPDATE third_party_delivery_shipments SET provider_payload=$1,updated_at=now() WHERE id=$2 RETURNING *',[JSON.stringify(storedPayload),row.rows[0].id]);
    createdRows[createdRows.length-1]=saved.rows[0];
    await audit(pool,actor,'ORDER',o.id,'THIRD_PARTY_DELIVERY_CREATED',{provider:'shiprocket',providerOrderId:String(srOrderId),shipmentId:String(srShipmentId),awb:awbCode,courier,sellerId,pickupLocation:pickup,manifestGenerated:!manifest?.error});
  }
  const tracking=createdRows.map(row=>({status:row.status,carrier:row.courier_name||'Shiprocket',provider:'shiprocket',providerOrderId:row.provider_order_id,shipmentId:row.provider_shipment_id,awb:row.awb,trackingUrl:row.tracking_url,updatedAt:new Date().toISOString()}));
  await pool.query('UPDATE orders SET tracking_json=$1,updated_at=now() WHERE id=$2',[JSON.stringify({status:'OUT_FOR_PICKUP',provider:'shiprocket',shipments:tracking,updatedAt:new Date().toISOString()}),o.id]);
  return createdRows.length===1?createdRows[0]:{order_id:o.id,provider:'shiprocket',shipments:createdRows};
}

export async function enqueueSellerShipment({pool,orderId,sellerId}) {
  if(!pool||!orderId||!sellerId) return {queued:false};
  const result=await pool.query("INSERT INTO third_party_delivery_jobs(order_id,seller_id,status) VALUES($1,$2,'PENDING') ON CONFLICT(order_id,seller_id) DO UPDATE SET status=CASE WHEN third_party_delivery_jobs.status IN ('DONE','PROCESSING') THEN third_party_delivery_jobs.status ELSE 'PENDING' END,next_attempt_at=LEAST(third_party_delivery_jobs.next_attempt_at,now()),updated_at=now() RETURNING id,status",[orderId,sellerId]);
  return {queued:true,job:result.rows[0]};
}
let deliveryWorkerTimer=null;
async function processDeliveryQueue(pool,audit,logger) {
  if(!enabled()||provider()!=='shiprocket'||!pool)return;
  const client=await pool.connect(); let job;
  try {
    await client.query('BEGIN');
    await client.query("UPDATE third_party_delivery_jobs SET status='RETRY',next_attempt_at=now(),locked_at=NULL,updated_at=now(),last_error=COALESCE(last_error,'WORKER_LEASE_EXPIRED') WHERE status='PROCESSING' AND locked_at < now()-interval '30 minutes'");
    const claimed=await client.query("SELECT id,order_id,seller_id,attempts FROM third_party_delivery_jobs WHERE status IN ('PENDING','RETRY') AND next_attempt_at<=now() ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1");
    if(!claimed.rowCount){await client.query('COMMIT');return;}
    job=claimed.rows[0];
    await client.query("UPDATE third_party_delivery_jobs SET status='PROCESSING',locked_at=now(),updated_at=now() WHERE id=$1",[job.id]);
    await client.query('COMMIT');
  } catch(error) { await client.query('ROLLBACK').catch(()=>{}); throw error; } finally { client.release(); }
  try {
    await createThirdPartyShipmentForOrder({pool,audit,orderId:job.order_id,sellerId:job.seller_id,actor:{sub:'SYSTEM',role:'SYSTEM'}});
    await pool.query("UPDATE third_party_delivery_jobs SET status='DONE',last_error=NULL,locked_at=NULL,updated_at=now() WHERE id=$1",[job.id]);
  } catch(error) {
    const attempts=Number(job.attempts||0)+1; const terminal=attempts>=8; const delay=Math.min(21600,Math.pow(2,attempts)*60);
    await pool.query("UPDATE third_party_delivery_jobs SET status=$2,attempts=$3,next_attempt_at=now()+($4 * interval '1 second'),last_error=$5,locked_at=NULL,updated_at=now() WHERE id=$1",[job.id,terminal?'FAILED':'RETRY',attempts,delay,String(error.code||error.message||'DELIVERY_FAILED').slice(0,160)]);
    logger.warn({orderId:job.order_id,sellerId:job.seller_id,attempts,error:error.code||error.message},'AARVO Shiprocket job deferred');
  }
}
export async function registerThirdPartyDelivery({app,pool,requireRole,audit}){
  if(pool&&!deliveryWorkerTimer){deliveryWorkerTimer=setInterval(()=>processDeliveryQueue(pool,audit,app.log).catch(error=>app.log.error({err:error},'Shiprocket queue worker failed')),5000);deliveryWorkerTimer.unref();}
  // Safe diagnostic: authenticates with Shiprocket and reads pickup locations only.
  // It never creates an order, shipment, AWB, or pickup booking.
  app.post('/v1/admin/delivery/shiprocket/validate',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    if(provider()!=='shiprocket')return reply.code(400).send({ok:false,error:'UNSUPPORTED_DELIVERY_PROVIDER'});
    if(!apiEmail()||!apiPassword()||!baseUrl())return reply.code(503).send({ok:false,error:'SHIPROCKET_CREDENTIALS_NOT_CONFIGURED'});
    try{
      const token=await loginShiprocket(true);
      const response=await fetch(baseUrl()+shiprocketPath.listPickups,{method:'GET',headers:{authorization:'Bearer '+token,'content-type':'application/json'}});
      const data=await readResponse(response);
      if(!response.ok)return reply.code(502).send({ok:false,authenticated:true,error:'SHIPROCKET_PICKUP_LIST_FAILED',providerStatus:response.status,message:clean(data?.message||data?.errors||'Pickup address listing failed',240)});
      const pickups=pickupRows(data);
      return {ok:true,authenticated:true,pickupListAccessible:true,pickupCount:pickups.length,pickupNames:pickups.map(x=>clean(x?.pickup_location||x?.pickupLocation||x?.name||'',80)).filter(Boolean).slice(0,30),shipmentCreated:false,awbAssigned:false,pickupBooked:false};
    }catch(error){
      request.log.warn({code:error.code||'SHIPROCKET_VALIDATION_FAILED',status:error.status||0},'Shiprocket safe validation failed');
      return reply.code(error.status&&error.status<500?error.status:502).send({ok:false,authenticated:false,error:error.code||'SHIPROCKET_VALIDATION_FAILED',message:clean(error.data?.message||error.message||'Shiprocket validation failed',240),shipmentCreated:false,awbAssigned:false,pickupBooked:false});
    }
  });
  app.get('/v1/admin/delivery/jobs',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    if(!pool)return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    const status=clean(request.query?.status||'',20).toUpperCase();
    if(status&&!['PENDING','PROCESSING','RETRY','DONE','FAILED'].includes(status))return reply.code(400).send({error:'INVALID_JOB_STATUS'});
    const result=await pool.query("SELECT id,order_id,seller_id,status,attempts,next_attempt_at,last_error,created_at,updated_at FROM third_party_delivery_jobs WHERE ($1='' OR status=$1) ORDER BY updated_at DESC LIMIT 200",[status]);
    return {jobs:result.rows};
  });
  app.post('/v1/admin/delivery/jobs/:id/retry',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    if(!pool)return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    const result=await pool.query("UPDATE third_party_delivery_jobs SET status='RETRY',attempts=0,next_attempt_at=now(),locked_at=NULL,last_error=NULL,updated_at=now() WHERE id=$1 AND status IN ('FAILED','RETRY') RETURNING id,order_id,seller_id,status,attempts,next_attempt_at",[request.params.id]);
    if(!result.rowCount)return reply.code(404).send({error:'RETRYABLE_DELIVERY_JOB_NOT_FOUND'});
    return {queued:true,job:result.rows[0]};
  });
  app.get('/v1/delivery/provider-status',{preHandler:requireRole('ADMIN')},async()=>({
    enabled:enabled(),provider:provider(),configured:Boolean((staticToken()||(apiEmail()&&apiPassword()))&&baseUrl()),
    mode:'THIRD_PARTY_ONLY',pickupLocation:pickupLocation()||'seller-specific'
  }));
  app.post('/v1/admin/orders/:id/third-party-delivery',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    try { return reply.code(201).send(await createThirdPartyShipmentForOrder({pool,audit,orderId:request.params.id,actor:request.user})); }
    catch(error) {
      if(error.code==='DELIVERY_ALREADY_CREATED') return reply.code(409).send({error:error.code,shipment:error.shipment});
      if(error.status) return reply.code(error.status).send({error:error.code,providerResponse:error.data});
      throw error;
    }
  });
  const handleDeliveryWebhook=async(request,reply,routeProvider)=>{
    if(!pool)return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    if(String(routeProvider||'').toLowerCase()!==provider())return reply.code(404).send({error:'DELIVERY_PROVIDER_NOT_FOUND'});
    if(!webhookSecret()||!verifyWebhookToken(request.headers['x-api-key']))return reply.code(401).send({error:'INVALID_DELIVERY_WEBHOOK_SIGNATURE'});
    const data=request.body||{};
    const shipmentId=clean(data.shipment_id||data.shipmentId||data.awb||data.awb_code||data.id||data?.tracking_data?.shipment_id,200);
    const awb=clean(data.awb||data.awb_code||data?.tracking_data?.shipment_track?.[0]?.awb_code,120);
    const next=statusFromShiprocket(data);
    const result=await pool.query(`UPDATE third_party_delivery_shipments SET status=$1,awb=COALESCE(NULLIF($2,''),awb),tracking_url=COALESCE(NULLIF($3,''),tracking_url),provider_payload=$4,updated_at=now()
      WHERE provider='shiprocket' AND (provider_shipment_id=$5 OR awb=$6) RETURNING order_id,awb,courier_name,tracking_url`,
      [next,awb,awb?'https://shiprocket.co/tracking/'+encodeURIComponent(awb):'',JSON.stringify(data),shipmentId,awb]);
    if(!result.rowCount)return {received:true,matched:false};
    for(const row of result.rows){
      const rows=await pool.query('SELECT status,provider,provider_order_id,provider_shipment_id,awb,courier_name,tracking_url,updated_at FROM third_party_delivery_shipments WHERE order_id=$1 ORDER BY created_at',[row.order_id]);
      await pool.query('UPDATE orders SET tracking_json=$1,updated_at=now() WHERE id=$2',
        [JSON.stringify({status:next==='RTO'?'CANCELLED':next,provider:'shiprocket',shipments:rows.rows,updatedAt:new Date().toISOString()}),row.order_id]);
    }
    return {received:true,matched:true};
  };
  // Shiprocket rejects webhook URLs containing its brand keywords. Keep the
  // provider-specific route for existing integrations and expose this neutral URL.
  app.post('/v1/webhooks/delivery/events',(request,reply)=>handleDeliveryWebhook(request,reply,provider()));
  app.post('/v1/webhooks/delivery/:provider',(request,reply)=>handleDeliveryWebhook(request,reply,request.params.provider));
}
