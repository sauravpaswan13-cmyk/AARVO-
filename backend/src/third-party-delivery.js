import { createHmac, timingSafeEqual } from 'node:crypto';

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
  const phone=clean(seller.phone||'',20).replace(/\D/g,'');
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

export async function createThirdPartyShipmentForOrder({pool,audit,orderId,actor=null}) {
  if(!pool) throw Object.assign(new Error('DATABASE_NOT_CONFIGURED'),{code:'DATABASE_NOT_CONFIGURED',status:503});
  if(!enabled()) throw Object.assign(new Error('THIRD_PARTY_DELIVERY_DISABLED'),{code:'THIRD_PARTY_DELIVERY_DISABLED',status:503});
  if(provider()!=='shiprocket') throw Object.assign(new Error('UNSUPPORTED_DELIVERY_PROVIDER'),{code:'UNSUPPORTED_DELIVERY_PROVIDER',status:400});
  if((!staticToken()&&(!apiEmail()||!apiPassword()))||!baseUrl()) throw Object.assign(new Error('SHIPROCKET_NOT_CONFIGURED'),{code:'SHIPROCKET_NOT_CONFIGURED',status:503});

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
  const sellerIds=[...groups.keys()];
  const sellerResult=await pool.query(`SELECT sp.seller_id,sp.phone,COALESCE(NULLIF(sp.business_name,''),u.display_name,'AARVO Seller') AS business_name,COALESCE(NULLIF(sp.business_email,''),u.email) AS business_email,sp.pickup_address,sp.pickup_city,sp.pickup_state,sp.pickup_postal_code FROM seller_profiles sp LEFT JOIN users u ON u.id=sp.seller_id WHERE sp.seller_id=ANY($1::uuid[])`,[sellerIds]);
  const sellerMap=new Map(sellerResult.rows.map(s=>[String(s.seller_id),s]));
  const customer=splitName(a.fullName);
  const shipping=Number(o.delivery_fee_paise||0)/100;
  const createdRows=[];
  let sellerIndex=0;
  for(const [sellerId,sellerLines] of groups){
    const seller=sellerMap.get(sellerId);
    if(!seller)throw Object.assign(new Error('SELLER_PROFILE_REQUIRED_FOR_SHIPPING'),{code:'SELLER_PROFILE_REQUIRED_FOR_SHIPPING',status:409,data:{sellerId}});
    const pickup=await ensureSellerPickup(seller);
    const items=sellerLines.map(({line,product})=>({
      name:clean(product.name||('AARVO Product '+line.productId),120),sku:clean(String(line.productId),80),
      units:Number(line.quantity)||1,selling_price:(Number(product.price_paise||0)/100).toFixed(2),discount:0,tax:0,hsn:''
    }));
    const subtotalPaise=sellerLines.reduce((sum,{line})=>sum+Number(line.lineTotalPaise||0),0);
    const sellerOrderId=('AARVO'+String(o.id).replace(/-/g,'')+'S'+String(sellerId).replace(/-/g,'')).slice(0,50);
    const payload={
      order_id:sellerOrderId,order_date:orderDate(),pickup_location:pickup,
      billing_customer_name:customer.first,billing_last_name:customer.last,
      billing_address:clean(a.line1,250),billing_address_2:clean(a.line2,250),
      billing_city:clean(a.city,30),billing_pincode:Number(a.postalCode),billing_state:clean(a.state,100),billing_country:'India',
      billing_email:clean(q.rows[0].buyer_email||'no-reply@aarvo.in',150),billing_phone:Number(String(a.phone).replace(/\D/g,'')),
      shipping_is_billing:true,order_items:items,
      payment_method:String(o.payment_status||'').toUpperCase()==='COD'?'COD':'Prepaid',
      shipping_charges:sellerIndex===0?shipping:0,giftwrap_charges:0,transaction_charges:0,total_discount:0,
      sub_total:subtotalPaise/100,
      length:Number(process.env.SHIPROCKET_PACKAGE_LENGTH_CM||10),breadth:Number(process.env.SHIPROCKET_PACKAGE_BREADTH_CM||10),
      height:Number(process.env.SHIPROCKET_PACKAGE_HEIGHT_CM||10),weight:Number(process.env.SHIPROCKET_PACKAGE_WEIGHT_KG||0.5)
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
    const row=await pool.query(`INSERT INTO third_party_delivery_shipments(order_id,provider,provider_order_id,provider_shipment_id,awb,courier_name,status,tracking_url,provider_payload)
      VALUES($1,'shiprocket',$2,$3,$4,$5,'OUT_FOR_PICKUP',$6,$7) RETURNING *`,
      [o.id,String(srOrderId),String(srShipmentId),awbCode,courier,trackingUrl,JSON.stringify(stored)]);
    createdRows.push(row.rows[0]);
    await audit(pool,actor,'ORDER',o.id,'THIRD_PARTY_DELIVERY_CREATED',{provider:'shiprocket',providerOrderId:String(srOrderId),shipmentId:String(srShipmentId),awb:awbCode,courier,sellerId,pickupLocation:pickup});
    sellerIndex++;
  }
  const tracking=createdRows.map(row=>({status:row.status,carrier:row.courier_name||'Shiprocket',provider:'shiprocket',providerOrderId:row.provider_order_id,shipmentId:row.provider_shipment_id,awb:row.awb,trackingUrl:row.tracking_url,updatedAt:new Date().toISOString()}));
  await pool.query('UPDATE orders SET tracking_json=$1,updated_at=now() WHERE id=$2',[JSON.stringify({status:'OUT_FOR_PICKUP',provider:'shiprocket',shipments:tracking,updatedAt:new Date().toISOString()}),o.id]);
  return createdRows.length===1?createdRows[0]:{order_id:o.id,provider:'shiprocket',shipments:createdRows};
}

export async function registerThirdPartyDelivery({app,pool,requireRole,audit}){
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
      WHERE provider='shiprocket' AND (provider_shipment_id=$5 OR awb=$6) RETURNING order_id,awb,courier_name,tracking_url`,
      [next,awb,awb?'https://shiprocket.co/tracking/'+encodeURIComponent(awb):'',JSON.stringify(data),shipmentId,awb]);
    if(!result.rowCount)return {received:true,matched:false};
    for(const row of result.rows){
      const rows=await pool.query('SELECT status,provider,provider_order_id,provider_shipment_id,awb,courier_name,tracking_url,updated_at FROM third_party_delivery_shipments WHERE order_id=$1 ORDER BY created_at',[row.order_id]);
      await pool.query('UPDATE orders SET tracking_json=$1,updated_at=now() WHERE id=$2',
        [JSON.stringify({status:next==='RTO'?'CANCELLED':next,provider:'shiprocket',shipments:rows.rows,updatedAt:new Date().toISOString()}),row.order_id]);
    }
    return {received:true,matched:true};
  });
}
