import { randomUUID } from 'node:crypto';
import { S3Client, PutObjectCommand, HeadObjectCommand, HeadBucketCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

const BUCKET = process.env.AARVO_PRODUCT_IMAGES_BUCKET;
const PUBLIC_BASE_URL = String(process.env.AARVO_PRODUCT_IMAGES_PUBLIC_BASE_URL || '').trim().replace(/\/$/, '');
const MAX_BYTES = 8 * 1024 * 1024;
const TYPES = new Map([['image/jpeg','.jpg'],['image/png','.png'],['image/webp','.webp']]);
const clean = (v,n=200) => String(v ?? '').trim().slice(0,n);
const fail = (reply, status, error) => reply.code(status).send({error});
function publicStorageClient() {
  const endpoint=process.env.AARVO_PRODUCT_IMAGES_S3_ENDPOINT;
  const region=process.env.AARVO_PRODUCT_IMAGES_S3_REGION || process.env.AWS_REGION;
  const accessKeyId=process.env.AARVO_PRODUCT_IMAGES_ACCESS_KEY_ID;
  const secretAccessKey=process.env.AARVO_PRODUCT_IMAGES_SECRET_ACCESS_KEY;
  if (!BUCKET || !PUBLIC_BASE_URL || !endpoint || !region || !accessKeyId || !secretAccessKey) return null;
  return new S3Client({region,endpoint,forcePathStyle:true,credentials:{accessKeyId,secretAccessKey}});
}

export async function registerProductImageUploads({app,pool,requireRole,audit}) {
  const client=publicStorageClient();
  let reachable=false;
  if(client) {
    try { await client.send(new HeadBucketCommand({Bucket:BUCKET})); reachable=true; }
    catch(error) { app.log.error({productImageStorageConfigured:true,productImageStorageReachable:false,errorName:error?.name,statusCode:error?.$metadata?.httpStatusCode},'AARVO public product image storage connectivity check failed'); }
  }
  app.log.info({productImageStorageConfigured:Boolean(client),productImageStorageReachable:reachable,publicBaseUrlConfigured:Boolean(PUBLIC_BASE_URL),bucketConfigured:Boolean(BUCKET)},'AARVO public product image storage readiness');
  app.post('/v1/seller/products/:id/images/upload-ticket',{preHandler:requireRole('SELLER'),config:{rateLimit:{max:20,timeWindow:'1 minute'}}},async(request,reply)=>{
    if(!pool) return fail(reply,503,'DATABASE_NOT_CONFIGURED');
    const s3=publicStorageClient();
    if(!s3) return fail(reply,503,'PUBLIC_PRODUCT_IMAGE_STORAGE_NOT_CONFIGURED');
    const product=await pool.query('SELECT id FROM products WHERE id=$1 AND seller_id=$2',[request.params.id,request.user.sub]);
    if(!product.rowCount) return fail(reply,404,'PRODUCT_NOT_FOUND');
    const count=await pool.query('SELECT count(*)::int AS count FROM product_images WHERE product_id=$1',[request.params.id]);
    if(Number(count.rows[0].count)>=5) return fail(reply,409,'PRODUCT_IMAGE_LIMIT_REACHED');
    const b=request.body||{}, contentType=clean(b.contentType,80).toLowerCase(), sizeBytes=Number(b.sizeBytes);
    if(!TYPES.has(contentType)||!Number.isSafeInteger(sizeBytes)||sizeBytes<1||sizeBytes>MAX_BYTES) return fail(reply,400,'INVALID_PRODUCT_IMAGE');
    const imageId=randomUUID();
    const key='product-images/'+String(request.user.sub)+'/'+String(request.params.id)+'/'+imageId+TYPES.get(contentType);
    const command=new PutObjectCommand({Bucket:BUCKET,Key:key,ContentType:contentType,Metadata:{sellerid:String(request.user.sub),productid:String(request.params.id),imageid:imageId}});
    const uploadUrl=await getSignedUrl(s3,command,{expiresIn:300});
    return {uploadUrl,objectKey:key,imageId,expiresInSeconds:300,requiredHeaders:{'Content-Type':contentType,'x-amz-meta-sellerid':String(request.user.sub),'x-amz-meta-productid':String(request.params.id),'x-amz-meta-imageid':imageId}};
  });
  app.post('/v1/seller/products/:id/images/confirm-upload',{preHandler:requireRole('SELLER'),config:{rateLimit:{max:20,timeWindow:'1 minute'}}},async(request,reply)=>{
    if(!pool) return fail(reply,503,'DATABASE_NOT_CONFIGURED');
    const s3=publicStorageClient();
    if(!s3) return fail(reply,503,'PUBLIC_PRODUCT_IMAGE_STORAGE_NOT_CONFIGURED');
    const b=request.body||{}, key=clean(b.objectKey,500), imageId=clean(b.imageId,40), altText=clean(b.altText,250);
    const expectedPrefix='product-images/'+String(request.user.sub)+'/'+String(request.params.id)+'/';
    if(!key.startsWith(expectedPrefix)||!new RegExp('^'+expectedPrefix.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')+'[0-9a-f-]{36}\\.(jpg|png|webp)$','i').test(key)||!/^[0-9a-f-]{36}$/i.test(imageId)||!key.includes(imageId)) return fail(reply,400,'INVALID_PRODUCT_IMAGE_CONFIRMATION');
    const product=await pool.query('SELECT id,name FROM products WHERE id=$1 AND seller_id=$2',[request.params.id,request.user.sub]);
    if(!product.rowCount) return fail(reply,404,'PRODUCT_NOT_FOUND');
    const head=await s3.send(new HeadObjectCommand({Bucket:BUCKET,Key:key}));
    const contentType=String(head.ContentType||'').toLowerCase(), size=Number(head.ContentLength||0);
    if(!TYPES.has(contentType)||size<1||size>MAX_BYTES||head.Metadata?.sellerid!==String(request.user.sub)||head.Metadata?.productid!==String(request.params.id)||head.Metadata?.imageid!==imageId) return fail(reply,400,'PRODUCT_IMAGE_VALIDATION_FAILED');
    const publicUrl=PUBLIC_BASE_URL+'/'+key.split('/').map(encodeURIComponent).join('/');
    const db=await pool.connect();
    try {
      await db.query('BEGIN');
      await db.query('SELECT id FROM products WHERE id=$1 AND seller_id=$2 FOR UPDATE',[request.params.id,request.user.sub]);
      const count=await db.query('SELECT count(*)::int AS count FROM product_images WHERE product_id=$1',[request.params.id]);
      if(Number(count.rows[0].count)>=5) { await db.query('ROLLBACK'); return fail(reply,409,'PRODUCT_IMAGE_LIMIT_REACHED'); }
      const isPrimary=Number(count.rows[0].count)===0;
      const inserted=await db.query('INSERT INTO product_images(product_id,seller_id,image_url,alt_text,sort_order,is_primary) VALUES($1,$2,$3,$4,$5,$6) RETURNING id,image_url,alt_text,sort_order,is_primary',[request.params.id,request.user.sub,publicUrl,altText||product.rows[0].name,Number(b.sortOrder)||0,isPrimary]);
      if(isPrimary) await db.query('UPDATE products SET image_url=$1,updated_at=now() WHERE id=$2 AND seller_id=$3',[publicUrl,request.params.id,request.user.sub]);
      await audit(db,request.user,'PRODUCT',request.params.id,'IMAGE_UPLOADED',{imageId:inserted.rows[0].id,objectKey:key,sizeBytes:size,contentType});
      await db.query('COMMIT');
      return reply.code(201).send({image:inserted.rows[0],publicUrl});
    } catch(error) { await db.query('ROLLBACK'); throw error; }
    finally { db.release(); }
  });
}
