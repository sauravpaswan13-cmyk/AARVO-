export async function registerRiderDelivery({ app, pool, requireRole, audit }) {
  const clean = (v, max=500) => String(v ?? '').trim().slice(0,max);
  app.get('/v1/rider/deliveries', { preHandler: requireRole('RIDER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    return (await pool.query('SELECT da.id,da.order_id,da.status,da.pickup_note,da.delivery_note,da.assigned_at,da.accepted_at,da.picked_up_at,da.delivered_at,o.status AS order_status,o.address_json,o.total_paise FROM delivery_assignments da JOIN orders o ON o.id=da.order_id WHERE da.rider_id=$1 ORDER BY da.updated_at DESC LIMIT 100',[request.user.sub])).rows;
  });
  app.post('/v1/rider/deliveries/:id/status', { preHandler: requireRole('RIDER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    const next=clean(request.body?.status,30).toUpperCase();
    if(!new Set(['ACCEPTED','PICKED_UP','OUT_FOR_DELIVERY','DELIVERED']).has(next)) return reply.code(400).send({error:'INVALID_DELIVERY_STATUS'});
    const client=await pool.connect();
    try{
      await client.query('BEGIN');
      const current=await client.query('SELECT status,order_id FROM delivery_assignments WHERE id=$1 AND rider_id=$2 FOR UPDATE',[request.params.id,request.user.sub]);
      if(!current.rowCount){await client.query('ROLLBACK');return reply.code(404).send({error:'DELIVERY_NOT_FOUND'});}
      const allowed={ASSIGNED:'ACCEPTED',ACCEPTED:'PICKED_UP',PICKED_UP:'OUT_FOR_DELIVERY',OUT_FOR_DELIVERY:'DELIVERED'};
      if(allowed[current.rows[0].status]!==next){await client.query('ROLLBACK');return reply.code(409).send({error:'INVALID_DELIVERY_TRANSITION'});}
      const result=await client.query("UPDATE delivery_assignments SET status=$1,accepted_at=CASE WHEN $1='ACCEPTED' THEN COALESCE(accepted_at,now()) ELSE accepted_at END,picked_up_at=CASE WHEN $1='PICKED_UP' THEN COALESCE(picked_up_at,now()) ELSE picked_up_at END,delivered_at=CASE WHEN $1='DELIVERED' THEN COALESCE(delivered_at,now()) ELSE delivered_at END,updated_at=now() WHERE id=$2 AND rider_id=$3 RETURNING *",[next,request.params.id,request.user.sub]);
      const orderStatus=next==='ACCEPTED'?'PACKED':next==='PICKED_UP'?'OUT_FOR_DELIVERY':next==='OUT_FOR_DELIVERY'?'OUT_FOR_DELIVERY':'DELIVERED';
      await client.query('UPDATE orders SET status=$1,tracking_json=$2,updated_at=now() WHERE id=$3',[orderStatus,JSON.stringify({status:orderStatus,carrier:'AARVO Delivery',note:'Updated by assigned rider',updatedAt:new Date().toISOString()}),current.rows[0].order_id]);
      await client.query('INSERT INTO delivery_events(order_id,status,carrier,note,actor_id) VALUES($1,$2,$3,$4,$5)',[current.rows[0].order_id,orderStatus,'AARVO Delivery','Rider updated delivery status',request.user.sub]);
      await audit(client,request.user,'DELIVERY',request.params.id,'STATUS_UPDATED',{status:next,orderStatus});
      await client.query('COMMIT');
      return result.rows[0];
    }catch(e){await client.query('ROLLBACK');throw e}finally{client.release()}
  });
  app.post('/v1/admin/deliveries/assign', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({error:'DATABASE_NOT_CONFIGURED'});
    const orderId=clean(request.body?.orderId,100), riderId=clean(request.body?.riderId,100);
    if(!orderId||!riderId) return reply.code(400).send({error:'ORDER_AND_RIDER_REQUIRED'});
    const rider=await pool.query("SELECT id FROM users WHERE id=$1 AND role='RIDER'",[riderId]);
    if(!rider.rowCount) return reply.code(404).send({error:'RIDER_NOT_FOUND'});
    const order=await pool.query('SELECT id,status FROM orders WHERE id=$1',[orderId]);
    if(!order.rowCount) return reply.code(404).send({error:'ORDER_NOT_FOUND'});
    const client=await pool.connect();
    try{
      await client.query('BEGIN');
      const existing=await client.query("SELECT id FROM delivery_assignments WHERE order_id=$1 AND status IN ('ASSIGNED','ACCEPTED','PICKED_UP','OUT_FOR_DELIVERY') FOR UPDATE",[orderId]);
      if(existing.rowCount){await client.query('ROLLBACK');return reply.code(409).send({error:'ORDER_ALREADY_ASSIGNED'});}
      const row=await client.query("INSERT INTO delivery_assignments(order_id,rider_id,status) VALUES($1,$2,'ASSIGNED') RETURNING *",[orderId,riderId]);
      await client.query('INSERT INTO rider_notifications(rider_id,order_id,title,body) VALUES($1,$2,$3,$4)',[riderId,orderId,'New AARVO delivery','A new order has been assigned to you.']);
      await audit(client,request.user,'DELIVERY',row.rows[0].id,'ASSIGNED',{orderId,riderId});
      await client.query('COMMIT');
      return reply.code(201).send(row.rows[0]);
    }catch(e){await client.query('ROLLBACK');throw e}finally{client.release()}
  });
}
