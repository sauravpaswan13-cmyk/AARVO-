export async function registerExternalDelivery({ app, pool, requireRole, audit }) {
  const clean=(v,max=500)=>String(v??'').trim().slice(0,max);
  const providerName=clean(process.env.DELIVERY_PARTNER_NAME||'Third-party Delivery',120);
  const webhookUrl=clean(process.env.DELIVERY_PARTNER_WEBHOOK_URL||'',2000);
  const webhookSecret=clean(process.env.DELIVERY_PARTNER_WEBHOOK_SECRET||'',500);

  app.get('/v1/admin/delivery/config',{preHandler:requireRole('ADMIN')},async(_request,reply)=>{
    return {configured:Boolean(webhookUrl&&webhookSecret),provider:providerName,mode:'EXTERNAL_PARTNER_NO_RIDER_APP'};
  });

  app.post('/v1/admin/delivery/dispatch',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    if(!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    if(!webhookUrl||!webhookSecret) return reply.code(503).send({error:'DELIVERY_PARTNER_NOT_CONFIGURED'});
    const orderId=clean(request.body?.orderId,100);
    if(!orderId) return reply.code(400).send({error:'ORDER_ID_REQUIRED'});
    const order=await pool.query(`SELECT o.id,o.status,o.total_paise,o.address_json,
      COALESCE((SELECT sp.business_name FROM seller_profiles sp WHERE sp.seller_id=(SELECT ol.seller_id FROM order_lines ol WHERE ol.order_id=o.id LIMIT 1)),'Seller') AS seller_name
      FROM orders o WHERE o.id=$1`,[orderId]);
    if(!order.rowCount) return reply.code(404).send({error:'ORDER_NOT_FOUND'});
    if(!['PACKED','SHIPPED','READY_FOR_PICKUP'].includes(String(order.rows[0].status).toUpperCase()))
      return reply.code(409).send({error:'ORDER_NOT_READY_FOR_DELIVERY',status:order.rows[0].status});
    const existing=await pool.query(`SELECT id,status,provider_delivery_id,tracking_url FROM delivery_partner_dispatches WHERE order_id=$1 ORDER BY created_at DESC LIMIT 1`,[orderId]);
    if(existing.rowCount && !['FAILED','CANCELLED','DELIVERED'].includes(existing.rows[0].status))
      return reply.code(409).send({error:'DELIVERY_ALREADY_DISPATCHED',dispatch:existing.rows[0]});
    const payload={
      event:'DELIVERY_CREATE',
      orderId,
      orderStatus:order.rows[0].status,
      amountPaise:Number(order.rows[0].total_paise||0),
      sellerName:order.rows[0].seller_name,
      pickupAddress:request.body?.pickupAddress||null,
      deliveryAddress:order.rows[0].address_json,
      callbackRequired:true
    };
    let response;
    try{
      response=await fetch(webhookUrl,{method:'POST',headers:{'content-type':'application/json','x-aarvo-delivery-secret':webhookSecret},body:JSON.stringify(payload)});
    }catch(error){
      return reply.code(502).send({error:'DELIVERY_PARTNER_UNREACHABLE'});
    }
    const raw=await response.text();
    if(!response.ok) return reply.code(502).send({error:'DELIVERY_PARTNER_REJECTED',providerStatus:response.status});
    let provider={}; try{provider=JSON.parse(raw||'{}')}catch{}
    const row=await pool.query(`INSERT INTO delivery_partner_dispatches(order_id,provider,status,provider_delivery_id,tracking_url,last_response)
      VALUES($1,$2,'ASSIGNED',$3,$4,$5::jsonb) RETURNING *`,
      [orderId,providerName,clean(provider.deliveryId||provider.id,200),clean(provider.trackingUrl||provider.tracking_url,1000),JSON.stringify(provider)]);
    await pool.query(`UPDATE orders SET tracking_json=$1,updated_at=now() WHERE id=$2`,
      [JSON.stringify({carrier:providerName,status:'ASSIGNED',trackingUrl:provider.trackingUrl||provider.tracking_url||null}),orderId]);
    await audit(pool,request.user,'DELIVERY',row.rows[0].id,'EXTERNAL_DISPATCHED',{orderId,provider:providerName});
    return reply.code(201).send(row.rows[0]);
  });

  app.get('/v1/admin/delivery/:orderId',{preHandler:requireRole('ADMIN')},async(request,reply)=>{
    if(!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    const r=await pool.query('SELECT * FROM delivery_partner_dispatches WHERE order_id=$1 ORDER BY created_at DESC',[request.params.orderId]);
    return r.rows;
  });

  app.post('/v1/webhooks/delivery-partner',async(request,reply)=>{
    if(!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    if(!webhookSecret || clean(request.headers['x-aarvo-delivery-secret'],500)!==webhookSecret)
      return reply.code(401).send({error:'INVALID_DELIVERY_WEBHOOK_SECRET'});
    const b=request.body||{};
    const orderId=clean(b.orderId||b.order_id,100);
    const status=clean(b.status,40).toUpperCase();
    if(!orderId||!status) return reply.code(400).send({error:'ORDER_ID_AND_STATUS_REQUIRED'});
    const allowed=new Set(['ASSIGNED','ACCEPTED','PICKED_UP','OUT_FOR_DELIVERY','DELIVERED','FAILED','CANCELLED']);
    if(!allowed.has(status)) return reply.code(400).send({error:'INVALID_DELIVERY_STATUS'});
    const client=await pool.connect();
    try{
      await client.query('BEGIN');
      const d=await client.query('SELECT id FROM delivery_partner_dispatches WHERE order_id=$1 ORDER BY created_at DESC LIMIT 1 FOR UPDATE',[orderId]);
      if(!d.rowCount){await client.query('ROLLBACK');return reply.code(404).send({error:'DELIVERY_DISPATCH_NOT_FOUND'});}
      await client.query(`UPDATE delivery_partner_dispatches SET status=$1,provider_delivery_id=COALESCE($2,provider_delivery_id),tracking_url=COALESCE($3,tracking_url),rider_name=COALESCE($4,rider_name),rider_phone=COALESCE($5,rider_phone),last_response=$6::jsonb,updated_at=now() WHERE id=$7`,
        [status,clean(b.deliveryId||b.delivery_id,200)||null,clean(b.trackingUrl||b.tracking_url,1000)||null,clean(b.riderName||b.rider_name,200)||null,clean(b.riderPhone||b.rider_phone,50)||null,JSON.stringify(b),d.rows[0].id]);
      const map={ASSIGNED:'PACKED',ACCEPTED:'PACKED',PICKED_UP:'OUT_FOR_DELIVERY',OUT_FOR_DELIVERY:'OUT_FOR_DELIVERY',DELIVERED:'DELIVERED'};
      const os=map[status];
      if(os) await client.query('UPDATE orders SET status=$1,tracking_json=$2,updated_at=now() WHERE id=$3',[os,JSON.stringify({carrier:providerName,status,trackingUrl:b.trackingUrl||b.tracking_url||null,riderName:b.riderName||b.rider_name||null,updatedAt:new Date().toISOString()}),orderId]);
      await client.query('INSERT INTO delivery_partner_events(order_id,status,payload) VALUES($1,$2,$3::jsonb)',[orderId,status,JSON.stringify(b)]);
      await client.query('COMMIT');
      return {ok:true,status};
    }catch(e){await client.query('ROLLBACK');throw e}finally{client.release()}
  });
}
