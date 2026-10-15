import { randomUUID } from 'node:crypto';
import { S3Client, PutObjectCommand, HeadObjectCommand, GetObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

const BUCKET = process.env.AARVO_KYC_BUCKET || 'aarvo-seller-kyc-private';
const MAX_BYTES = 5 * 1024 * 1024;
const ALLOWED = new Map([
  ['application/pdf', '.pdf'],
  ['image/jpeg', '.jpg'],
  ['image/png', '.png']
]);
const clean = (v, n=200) => String(v ?? '').trim().slice(0,n);
const fail = (reply, code, error) => reply.code(code).send({ error });

function storageClient() {
  const endpoint = process.env.AWS_ENDPOINT_URL_S3;
  const region = process.env.AWS_REGION;
  const accessKeyId = process.env.AWS_ACCESS_KEY_ID;
  const secretAccessKey = process.env.AWS_SECRET_ACCESS_KEY;
  if (!endpoint || !region || !accessKeyId || !secretAccessKey) return null;
  return new S3Client({ region, endpoint, forcePathStyle: true, credentials: { accessKeyId, secretAccessKey } });
}

export async function registerSellerKycDocuments({ app, pool, requireRole, audit }) {
  if (pool) {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS seller_kyc_documents (
        id UUID PRIMARY KEY,
        seller_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        document_type TEXT NOT NULL CHECK (document_type IN ('PAN','ADDRESS_PROOF','BUSINESS_PROOF','OTHER')),
        original_filename TEXT NOT NULL,
        content_type TEXT NOT NULL,
        size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
        object_key TEXT NOT NULL UNIQUE,
        status TEXT NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','ACCEPTED','REJECTED')),
        review_note TEXT,
        uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
        reviewed_at TIMESTAMPTZ,
        reviewed_by TEXT REFERENCES users(id)
      );
      CREATE INDEX IF NOT EXISTS seller_kyc_documents_seller_idx ON seller_kyc_documents(seller_id, uploaded_at DESC);
    `);
  }

  app.post('/v1/seller/kyc-documents/presign', { preHandler: requireRole('SELLER'), config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
    if (!pool) return fail(reply, 503, 'DATABASE_NOT_CONFIGURED');
    const s3 = storageClient();
    if (!s3) return fail(reply, 503, 'PRIVATE_DOCUMENT_STORAGE_NOT_CONFIGURED');
    const b = request.body || {};
    const filename = clean(b.filename, 180).replace(/[\\/]/g, '_');
    const contentType = clean(b.contentType, 80).toLowerCase();
    const documentType = clean(b.documentType, 40).toUpperCase();
    const sizeBytes = Number(b.sizeBytes);
    if (!filename || !ALLOWED.has(contentType) || !Number.isSafeInteger(sizeBytes) || sizeBytes < 1 || sizeBytes > MAX_BYTES ||
        !['PAN','ADDRESS_PROOF','BUSINESS_PROOF','OTHER'].includes(documentType)) return fail(reply, 400, 'INVALID_DOCUMENT');
    const id = randomUUID();
    const ext = ALLOWED.get(contentType);
    const key = `seller-kyc/${request.user.sub}/${id}${ext}`;
    const command = new PutObjectCommand({ Bucket: BUCKET, Key: key, ContentType: contentType, Metadata: { sellerid: String(request.user.sub), documentid: id } });
    const uploadUrl = await getSignedUrl(s3, command, { expiresIn: 300 });
    return { uploadUrl, documentId: id, objectKey: key, expiresInSeconds: 300, requiredHeaders: { 'Content-Type': contentType, 'x-amz-meta-sellerid': String(request.user.sub), 'x-amz-meta-documentid': id } };
  });

  app.post('/v1/seller/kyc-documents/confirm', { preHandler: requireRole('SELLER'), config: { rateLimit: { max: 10, timeWindow: '1 minute' } } }, async (request, reply) => {
    if (!pool) return fail(reply, 503, 'DATABASE_NOT_CONFIGURED');
    const s3 = storageClient();
    if (!s3) return fail(reply, 503, 'PRIVATE_DOCUMENT_STORAGE_NOT_CONFIGURED');
    const b = request.body || {};
    const id = clean(b.documentId, 40);
    const key = clean(b.objectKey, 400);
    const filename = clean(b.filename, 180).replace(/[\\/]/g, '_');
    const contentType = clean(b.contentType, 80).toLowerCase();
    const documentType = clean(b.documentType, 40).toUpperCase();
    if (!/^[0-9a-f-]{36}$/i.test(id) || key !== `seller-kyc/${request.user.sub}/${id}${ALLOWED.get(contentType)||''}` ||
        !filename || !ALLOWED.has(contentType) || !['PAN','ADDRESS_PROOF','BUSINESS_PROOF','OTHER'].includes(documentType)) return fail(reply, 400, 'INVALID_DOCUMENT_CONFIRMATION');
    const head = await s3.send(new HeadObjectCommand({ Bucket: BUCKET, Key: key }));
    const sizeBytes = Number(head.ContentLength || 0);
    if (sizeBytes < 1 || sizeBytes > MAX_BYTES || String(head.ContentType || '').toLowerCase() !== contentType ||
        head.Metadata?.sellerid !== String(request.user.sub) || head.Metadata?.documentid !== id) return fail(reply, 400, 'DOCUMENT_VALIDATION_FAILED');
    const result = await pool.query(
      'INSERT INTO seller_kyc_documents(id,seller_id,document_type,original_filename,content_type,size_bytes,object_key) VALUES($1,$2,$3,$4,$5,$6,$7) ON CONFLICT(id) DO UPDATE SET original_filename=EXCLUDED.original_filename RETURNING id,document_type,original_filename,content_type,size_bytes,status,uploaded_at',
      [id, request.user.sub, documentType, filename, contentType, sizeBytes, key]
    );
    await audit(pool, request.user, 'SELLER_KYC_DOCUMENT', id, 'DOCUMENT_UPLOADED', { documentType, contentType, sizeBytes });
    return { document: result.rows[0] };
  });

  app.post('/v1/seller/kyc-documents/submit', { preHandler: requireRole('SELLER'), config: { rateLimit: { max: 5, timeWindow: '1 minute' } } }, async (request, reply) => {
    if (!pool) return fail(reply, 503, 'DATABASE_NOT_CONFIGURED');
    const result = await pool.query(
      "SELECT id FROM seller_kyc_documents WHERE seller_id=$1 ORDER BY uploaded_at DESC",
      [request.user.sub]
    );
    if (!result.rowCount) return fail(reply, 400, 'NO_KYC_DOCUMENTS_UPLOADED');
    await audit(pool, request.user, 'SELLER_KYC_DOCUMENTS', request.user.sub, 'DOCUMENTS_SUBMITTED_FOR_REVIEW', {
      documentCount: result.rowCount,
      documentIds: result.rows.map(row => row.id)
    });
    return { status: 'SUBMITTED', documentCount: result.rowCount, message: 'KYC documents submitted for admin review.' };
  });

  app.get('/v1/seller/kyc-documents', { preHandler: requireRole('SELLER') }, async (request, reply) => {
    if (!pool) return fail(reply, 503, 'DATABASE_NOT_CONFIGURED');
    const result = await pool.query('SELECT id,document_type,original_filename,content_type,size_bytes,status,review_note,uploaded_at,reviewed_at FROM seller_kyc_documents WHERE seller_id=$1 ORDER BY uploaded_at DESC', [request.user.sub]);
    return { documents: result.rows };
  });

  app.get('/v1/admin/sellers/:sellerId/kyc-documents', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    if (!pool) return fail(reply, 503, 'DATABASE_NOT_CONFIGURED');
    const result = await pool.query('SELECT id,document_type,original_filename,content_type,size_bytes,status,review_note,uploaded_at,reviewed_at FROM seller_kyc_documents WHERE seller_id=$1 ORDER BY uploaded_at DESC', [request.params.sellerId]);
    return { documents: result.rows };
  });

  app.post('/v1/admin/sellers/:sellerId/kyc-documents/:documentId/review', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    if (!pool) return fail(reply, 503, 'DATABASE_NOT_CONFIGURED');
    const status = clean(request.body?.status, 20).toUpperCase();
    const note = clean(request.body?.note, 1000);
    if (!['ACCEPTED','REJECTED'].includes(status) || (status === 'REJECTED' && !note)) return fail(reply, 400, 'INVALID_DOCUMENT_REVIEW');
    const result = await pool.query('UPDATE seller_kyc_documents SET status=$1,review_note=NULLIF($2,\'\'),reviewed_at=now(),reviewed_by=$3 WHERE id=$4 AND seller_id=$5 RETURNING id,status,review_note,reviewed_at', [status, note, request.user.sub, request.params.documentId, request.params.sellerId]);
    if (!result.rowCount) return fail(reply, 404, 'DOCUMENT_NOT_FOUND');
    await audit(pool, request.user, 'SELLER_KYC_DOCUMENT', request.params.documentId, 'DOCUMENT_REVIEWED', { status });
    return { document: result.rows[0] };
  });

  app.get('/v1/admin/sellers/:sellerId/kyc-documents/:documentId/download', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    if (!pool) return fail(reply, 503, 'DATABASE_NOT_CONFIGURED');
    const s3 = storageClient();
    if (!s3) return fail(reply, 503, 'PRIVATE_DOCUMENT_STORAGE_NOT_CONFIGURED');
    const result = await pool.query('SELECT object_key,original_filename,content_type FROM seller_kyc_documents WHERE id=$1 AND seller_id=$2', [request.params.documentId, request.params.sellerId]);
    if (!result.rowCount) return fail(reply, 404, 'DOCUMENT_NOT_FOUND');
    const d = result.rows[0];
    const url = await getSignedUrl(s3, new GetObjectCommand({ Bucket: BUCKET, Key: d.object_key, ResponseContentType: d.content_type, ResponseContentDisposition: `attachment; filename="${d.original_filename.replace(/["\\\\]/g,'_')}"` }), { expiresIn: 180 });
    return { url, expiresInSeconds: 180 };
  });
}
