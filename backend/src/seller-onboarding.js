export async function registerSellerOnboarding({ app, pool, requireRole, audit }) {
  const clean = (value, max = 500) => String(value ?? '').trim().slice(0, max);
  const normalizePan = (value) => clean(value, 10).toUpperCase();
  const normalizeGstin = (value) => clean(value, 15).toUpperCase();
  const normalizeHsn = (value) => clean(value, 20).toUpperCase();
  const validPan = (value) => /^[A-Z]{5}\d{4}[A-Z]$/.test(value);
  const validGstin = (value) => value === '' || /^[0-9A-Z]{15}$/.test(value);
  const validIfsc = (value) => value === '' || /^[A-Z]{4}0[A-Z0-9]{6}$/.test(value);
  const validStatus = new Set(['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED']);
  const validGstStatus = new Set(['REGISTERED', 'NOT_REGISTERED']);

  app.get('/v1/seller/onboarding', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const result = await pool.query(`SELECT seller_id,phone,business_name,business_type,business_category,business_email,pan,gstin,gst_status,account_holder_name,bank_account_last4,ifsc,payout_preference,pickup_address,pickup_city,pickup_state,pickup_postal_code,return_window_days,shipping_model,onboarding_status,submitted_at,reviewed_at,rejection_reason,verified,payout_account_ready FROM seller_profiles WHERE seller_id=$1`, [request.user.sub]);
    if (!result.rowCount) return reply.code(404).send({ error: 'SELLER_PROFILE_REQUIRED' });
    const row = result.rows[0];
    return { ...row, bank_account_last4: row.bank_account_last4 ? `••••${row.bank_account_last4}` : '' };
  });

  app.put('/v1/seller/onboarding', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const b = request.body || {};
    const pan = normalizePan(b.pan);
    const gstStatus = clean(b.gstStatus, 20).toUpperCase() || (clean(b.gst, 15) ? 'REGISTERED' : 'NOT_REGISTERED');
    const gstin = normalizeGstin(b.gst);
    const ifsc = clean(b.ifsc, 11).toUpperCase();
    const account = clean(b.bankAccountNumber, 34).replace(/\D/g, '');
    const returnDays = Number(b.returnWindowDays);
    const productName = clean(b.productName, 180);
    const productCategory = clean(b.productCategory || b.businessCategory, 100);
    const productDescription = clean(b.productDescription, 1000);
    const productHsn = normalizeHsn(b.productHsn);
    const productPrice = Number(b.productPricePaise);
    const productStock = Number(b.productStockQuantity);
    const productGstRate = Number(b.productGstRate);

    if (!validGstStatus.has(gstStatus)) return reply.code(400).send({ error: 'INVALID_GST_STATUS' });
    if (gstStatus === 'REGISTERED' && !validGstin(gstin) || gstStatus === 'REGISTERED' && !gstin) return reply.code(400).send({ error: 'GSTIN_REQUIRED_FOR_REGISTERED_SELLER' });
    if (gstStatus === 'NOT_REGISTERED' && gstin) return reply.code(400).send({ error: 'GSTIN_NOT_ALLOWED_FOR_NON_REGISTERED_STATUS' });
    if (!clean(b.businessName, 160) || !clean(b.businessType, 80) || !clean(b.businessCategory, 100) || !validPan(pan) || !validGstin(gstin) || !validIfsc(ifsc) || (account && (account.length < 6 || account.length > 34)) || !Number.isInteger(returnDays) || returnDays < 0 || returnDays > 90 || !clean(b.pickupCity, 80) || !clean(b.pickupState, 80) || !/^\d{6}$/.test(clean(b.pickupPostalCode, 6))) return reply.code(400).send({ error: 'INVALID_ONBOARDING_DATA' });

    const hasProduct = Boolean(productName || b.productPricePaise !== undefined || productDescription);
    if (hasProduct && (!productName || !productCategory || !productDescription || !productHsn || !Number.isInteger(productPrice) || productPrice <= 0 || !Number.isInteger(productStock) || productStock < 0 || !Number.isFinite(productGstRate) || productGstRate < 0 || productGstRate > 100)) return reply.code(400).send({ error: 'INVALID_PRODUCT_LISTING' });

    const current = await pool.query('SELECT onboarding_status FROM seller_profiles WHERE seller_id=$1', [request.user.sub]);
    if (!current.rowCount) return reply.code(404).send({ error: 'SELLER_PROFILE_REQUIRED' });
    if (current.rows[0].onboarding_status === 'APPROVED') return reply.code(409).send({ error: 'ONBOARDING_ALREADY_APPROVED' });

    const submit = Boolean(b.submit);
    const status = submit ? 'SUBMITTED' : (current.rows[0].onboarding_status === 'REJECTED' ? 'DRAFT' : current.rows[0].onboarding_status || 'DRAFT');
    const result = await pool.query(`UPDATE seller_profiles SET business_name=$1,business_type=$2,business_category=$3,business_email=$4,pan=$5,gstin=$6,gst_status=$7,account_holder_name=$8,bank_account_last4=$9,ifsc=$10,payout_preference=$11,pickup_address=$12,pickup_city=$13,pickup_state=$14,pickup_postal_code=$15,return_window_days=$16,shipping_model=$17,onboarding_status=$18,submitted_at=CASE WHEN $19 THEN now() ELSE submitted_at END,rejection_reason=CASE WHEN $19 THEN NULL ELSE rejection_reason END WHERE seller_id=$20 RETURNING seller_id,business_name,business_type,business_category,business_email,pan,gstin,gst_status,account_holder_name,bank_account_last4,ifsc,payout_preference,pickup_address,pickup_city,pickup_state,pickup_postal_code,return_window_days,shipping_model,onboarding_status,submitted_at,rejection_reason,verified,payout_account_ready`, [clean(b.businessName,160),clean(b.businessType,80),clean(b.businessCategory,100),clean(b.businessEmail,200).toLowerCase() || null,pan,gstStatus === 'REGISTERED' ? gstin : null,gstStatus,clean(b.accountHolderName,160) || null,account ? account.slice(-4) : null,ifsc || null,clean(b.payoutPreference,60),clean(b.pickupAddress,500),clean(b.pickupCity,80),clean(b.pickupState,80),clean(b.pickupPostalCode,6),returnDays,clean(b.shippingModel,100),status,submit,request.user.sub]);

    let product = null;
    if (hasProduct) {
      const seller = result.rows[0];
      const canPublish = Boolean(seller.verified && seller.payout_account_ready);
      const productResult = await pool.query(`INSERT INTO products(seller_id,seller_name,name,category,price_paise,description,stock_quantity,is_published,hsn_code,gst_rate,gst_rate_source) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,'PENDING_REVIEW') RETURNING id,seller_id,seller_name,name,category,price_paise,description,stock_quantity,is_published,hsn_code,gst_rate,gst_rate_source`, [request.user.sub,clean(b.displayName || seller.business_name,160),productName,productCategory,productPrice,productDescription,productStock,canPublish && submit,productHsn,productGstRate]);
      product = productResult.rows[0];
    }

    await audit(pool, request.user, 'SELLER_PROFILE', request.user.sub, submit ? 'ONBOARDING_SUBMITTED' : 'ONBOARDING_SAVED', { status, gstStatus, productId: product?.id || null });
    const row = result.rows[0];
    row.bank_account_last4 = row.bank_account_last4 ? `••••${row.bank_account_last4}` : '';
    return { onboarding: row, product };
  });

  app.get('/v1/admin/sellers/onboarding', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const status = clean(request.query?.status, 20).toUpperCase();
    const params = status && validStatus.has(status) ? [status] : [];
    const where = params.length ? 'WHERE sp.onboarding_status=$1' : '';
    const rows = (await pool.query(`SELECT sp.seller_id,u.display_name,u.email,sp.phone,sp.business_name,sp.business_type,sp.business_category,sp.business_email,sp.pan,sp.gstin,sp.gst_status,sp.account_holder_name,sp.bank_account_last4,sp.ifsc,sp.payout_preference,sp.pickup_address,sp.pickup_city,sp.pickup_state,sp.pickup_postal_code,sp.return_window_days,sp.shipping_model,sp.onboarding_status,sp.submitted_at,sp.reviewed_at,sp.rejection_reason,sp.verified,sp.payout_account_ready FROM seller_profiles sp JOIN users u ON u.id=sp.seller_id ${where} ORDER BY sp.submitted_at DESC NULLS LAST,sp.created_at DESC`, params)).rows;
    return { sellers: rows.map((row) => ({ ...row, bank_account_last4: row.bank_account_last4 ? `••••${row.bank_account_last4}` : '' })) };
  });

  app.post('/v1/admin/sellers/:sellerId/onboarding/review', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    if (!pool) return reply.code(503).send({ error: 'DATABASE_NOT_CONFIGURED' });
    const decision = clean(request.body?.decision, 20).toUpperCase();
    const reason = clean(request.body?.reason, 500);
    if (!['APPROVE', 'REJECT'].includes(decision)) return reply.code(400).send({ error: 'INVALID_REVIEW_DECISION' });
    const seller = await pool.query('SELECT seller_id,onboarding_status FROM seller_profiles WHERE seller_id=$1', [request.params.sellerId]);
    if (!seller.rowCount) return reply.code(404).send({ error: 'SELLER_NOT_FOUND' });
    if (decision === 'REJECT' && !reason) return reply.code(400).send({ error: 'REJECTION_REASON_REQUIRED' });
    const approved = decision === 'APPROVE';
    const result = await pool.query(`UPDATE seller_profiles SET onboarding_status=$1,verified=$2,verified_at=CASE WHEN $2 THEN now() ELSE NULL END,verified_by=CASE WHEN $2 THEN $3 ELSE NULL END,reviewed_at=now(),rejection_reason=CASE WHEN $2 THEN NULL ELSE $4 END,payout_account_ready=CASE WHEN $2 AND payout_account_ready THEN true ELSE payout_account_ready END WHERE seller_id=$5 RETURNING seller_id,onboarding_status,verified,payout_account_ready,verified_at,reviewed_at,rejection_reason`, [approved ? 'APPROVED' : 'REJECTED',approved,request.user.sub,reason || null,request.params.sellerId]);
    await audit(pool, request.user, 'SELLER_PROFILE', request.params.sellerId, approved ? 'ONBOARDING_APPROVED' : 'ONBOARDING_REJECTED', { reason: reason || null });
    return { onboarding: result.rows[0] };
  });
}
