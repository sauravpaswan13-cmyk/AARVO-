import { URL } from 'node:url';

const AI_API_KEY = String(process.env.AI_API_KEY || '').trim();
const AI_BASE_URL = String(process.env.AI_BASE_URL || 'https://api.openai.com/v1').replace(/\/$/, '');
const AI_MODEL = String(process.env.AI_MODEL || 'gpt-4.1-mini').trim();

const money = (paise) => `₹${(Number(paise || 0) / 100).toLocaleString('en-IN')}`;
const tokens = (text) => String(text || '').toLowerCase().replace(/[^a-z0-9₹ ]/g, ' ').split(/\s+/).filter(Boolean);
const extractBudget = (text) => { const m = String(text || '').replace(/,/g, '').match(/(?:₹|rs\.?|inr)?\s*(\d{3,7})/i); return m ? Number(m[1]) * 100 : null; };

export const aiConfigured = () => Boolean(AI_API_KEY);

async function provider(messages, json = false) {
  if (!AI_API_KEY) return null;
  const response = await fetch(`${AI_BASE_URL}/chat/completions`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${AI_API_KEY}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ model: AI_MODEL, messages, temperature: 0.2, ...(json ? { response_format: { type: 'json_object' } } : {}) })
  });
  if (!response.ok) throw new Error(`AI_PROVIDER_${response.status}`);
  const body = await response.json();
  return body?.choices?.[0]?.message?.content || '';
}

export async function assistant({ message, products = [], user = null }) {
  const text = String(message || '').trim();
  if (!text) return { reply: 'Batayein, AARVO par aap kya kharidna chahte hain?', products: [] };
  if (AI_API_KEY) {
    const compact = products.slice(0, 30).map(p => ({ id: p.id, name: p.name, category: p.category, price: money(p.price_paise), rating: p.rating, stock: p.stock_quantity }));
    const content = await provider([
      { role: 'system', content: 'You are AARVO AI Shopping Assistant. Help users discover products, compare choices, explain reviews, deals and order/support guidance. Never invent prices, stock, orders, payments, refunds or delivery status. Use only supplied product data for product facts. Critical purchase/payment/refund decisions remain server authoritative. Respond in concise Hinglish unless the user writes English.' },
      { role: 'user', content: JSON.stringify({ message: text, products: compact, signedIn: Boolean(user) }) }
    ]);
    return { reply: content, products: [] };
  }
  const budget = extractBudget(text);
  const ts = tokens(text).filter(t => !['mujhe','chahiye','acha','accha','best','ke','andar','under','mein','me','ka','ki','koi','dikhao','show'].includes(t));
  const scored = products.map(p => {
    const hay = `${p.name} ${p.category} ${p.description}`.toLowerCase();
    let score = 0;
    for (const t of ts) if (hay.includes(t)) score += t.length > 2 ? 3 : 1;
    if (budget && Number(p.price_paise) <= budget) score += 2;
    if (Number(p.stock_quantity) > 0) score += 1;
    score += Number(p.rating || 0) / 5;
    return { p, score };
  }).filter(x => x.score > 1).sort((a,b) => b.score-a.score).slice(0, 6).map(x => x.p);
  if (scored.length) return { reply: `${scored.length} relevant option${scored.length > 1 ? 's' : ''} mili hain. Price, rating aur availability dekhkar choose kijiye.`, products: scored };
  return { reply: 'Mujhe exact match nahi mila. Product ka naam/category ya budget thoda aur clear karke batayein.', products: [] };
}

export async function smartSearch({ query, products = [] }) {
  const text = String(query || '').trim();
  if (AI_API_KEY) {
    const content = await provider([
      { role: 'system', content: 'Convert natural-language shopping queries into JSON with fields: normalizedQuery, category, maxPricePaise, minRating, intent. Do not invent product facts.' },
      { role: 'user', content: text }
    ], true);
    try { return JSON.parse(content); } catch { /* deterministic fallback */ }
  }
  return { normalizedQuery: text, category: '', maxPricePaise: extractBudget(text), minRating: /5\s*star|five star|top rated/i.test(text) ? 4.5 : 0, intent: 'SEARCH' };
}

export async function summarizeReviews({ product, reviews = [] }) {
  if (!reviews.length) return { summary: 'Abhi reviews available nahi hain.', pros: [], cons: [] };
  if (AI_API_KEY) {
    const content = await provider([
      { role: 'system', content: 'Summarize e-commerce reviews faithfully. Return JSON: summary, pros array, cons array, sentiment. Do not invent claims.' },
      { role: 'user', content: JSON.stringify({ product: { name: product?.name, rating: product?.rating }, reviews: reviews.slice(0,100).map(r => ({ rating:r.rating, text:r.review_text })) }) }
    ], true);
    try { return JSON.parse(content); } catch { /* deterministic fallback */ }
  }
  const avg = reviews.reduce((s,r) => s + Number(r.rating || 0), 0) / reviews.length;
  const positive = reviews.filter(r => Number(r.rating) >= 4).length;
  const negative = reviews.filter(r => Number(r.rating) <= 2).length;
  return { summary: `${reviews.length} reviews ka average ${avg.toFixed(1)}/5 hai; ${positive} positive aur ${negative} low-rating reviews mile.`, pros: positive ? ['Overall customer sentiment is positive'] : [], cons: negative ? ['Kuch customers ne low rating di hai'] : [], sentiment: avg >= 4 ? 'POSITIVE' : avg >= 3 ? 'MIXED' : 'NEGATIVE' };
}

export async function compareProducts(products) {
  if (AI_API_KEY) {
    const content = await provider([
      { role: 'system', content: 'Compare supplied products only. Return JSON with summary and rows. Never invent specifications.' },
      { role: 'user', content: JSON.stringify(products.map(p => ({ id:p.id,name:p.name,category:p.category,price:money(p.price_paise),rating:p.rating,stock:p.stock_quantity,description:p.description }))) }
    ], true);
    try { return JSON.parse(content); } catch { /* fallback */ }
  }
  return { summary: 'Price, rating aur available stock ke basis par comparison.', rows: products.map(p => ({ id:p.id, name:p.name, price:money(p.price_paise), rating:p.rating, stock:p.stock_quantity })) };
}

export async function sellerListing(input) {
  const clean = String(input?.name || '').trim();
  if (AI_API_KEY) {
    const content = await provider([
      { role: 'system', content: 'Create marketplace listing assistance. Return JSON: title, description, category, keywords array. Never fabricate technical specifications; mark missing details as needing seller input.' },
      { role: 'user', content: JSON.stringify(input) }
    ], true);
    try { return JSON.parse(content); } catch { /* fallback */ }
  }
  return { title: clean, description: String(input?.description || '').trim(), category: String(input?.category || 'Other').trim(), keywords: tokens(`${clean} ${input?.category || ''}`).slice(0,10) };
}

export function smartDealHints(products = []) {
  return products.filter(p => Number(p.stock_quantity) > 0).sort((a,b) => Number(b.rating||0)-Number(a.rating||0)).slice(0,8).map(p => ({ id:p.id, name:p.name, pricePaise:Number(p.price_paise), rating:Number(p.rating||0) }));
}

export function normalizeAiBaseUrl(value) {
  try { const u = new URL(value); return u.protocol === 'https:' ? u.toString().replace(/\/$/,'') : ''; } catch { return ''; }
}
