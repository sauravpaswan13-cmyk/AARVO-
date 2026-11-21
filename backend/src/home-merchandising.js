const DEFAULT_MERCHANDISING_SECTIONS = [
  {
    sectionKey: 'still_looking',
    title: 'Still looking for these?',
    subtitle: 'Based on items you viewed, saved & carted',
    badgeText: '',
    enabled: true,
    maxItems: 3,
    sortOrder: 0,
    pinnedProductIds: [],
  },
  {
    sectionKey: 'brands_spotlight',
    title: 'Brands in Spotlight',
    subtitle: 'Featured storefronts & top offers',
    badgeText: 'SPOTLIGHT',
    enabled: true,
    maxItems: 6,
    sortOrder: 1,
    pinnedProductIds: [],
  },
  {
    sectionKey: 'top_value_deals',
    title: 'Top Value Deals',
    subtitle: 'Budget-friendly & best-value picks',
    badgeText: 'VALUE PICKS',
    enabled: true,
    maxItems: 3,
    sortOrder: 2,
    pinnedProductIds: [],
  },
  {
    sectionKey: 'suggested_for_you',
    title: 'Suggested For You',
    subtitle: 'Curated picks from your favorite categories',
    badgeText: 'FOR YOU',
    enabled: true,
    maxItems: 3,
    sortOrder: 3,
    pinnedProductIds: [],
  },
  {
    sectionKey: 'more_products',
    title: 'More products for you',
    subtitle: 'Explore verified AARVO marketplace listings',
    badgeText: 'DISCOVER',
    enabled: true,
    maxItems: 8,
    sortOrder: 4,
    pinnedProductIds: [],
  },
];

const ALLOWED_KEYS = new Set(DEFAULT_MERCHANDISING_SECTIONS.map((s) => s.sectionKey));

export async function registerHomeMerchandising({ app, pool, requireRole, audit }) {
  if (!pool) return;

  await pool.query(`
    CREATE TABLE IF NOT EXISTS aarvo_home_merchandising (
      section_key TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      subtitle TEXT NOT NULL DEFAULT '',
      badge_text TEXT NOT NULL DEFAULT '',
      enabled BOOLEAN NOT NULL DEFAULT true,
      max_items INTEGER NOT NULL DEFAULT 3 CHECK (max_items BETWEEN 2 AND 16),
      sort_order INTEGER NOT NULL DEFAULT 0,
      pinned_product_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
      updated_by TEXT,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    )
  `);

  for (const seed of DEFAULT_MERCHANDISING_SECTIONS) {
    await pool.query(
      `INSERT INTO aarvo_home_merchandising
        (section_key, title, subtitle, badge_text, enabled, max_items, sort_order, pinned_product_ids)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8::jsonb)
       ON CONFLICT (section_key) DO NOTHING`,
      [
        seed.sectionKey,
        seed.title,
        seed.subtitle,
        seed.badgeText,
        seed.enabled,
        seed.maxItems,
        seed.sortOrder,
        JSON.stringify(seed.pinnedProductIds),
      ]
    );
  }

  // Public Home Merchandising Feed: returns ordered sections + live hydrated Admin-pinned products
  app.get('/v1/home/merchandising', async (_request, reply) => {
    reply.header('Cache-Control', 'no-store, max-age=0');
    const sectionsResult = await pool.query(`
      SELECT section_key, title, subtitle, badge_text, enabled, max_items, sort_order, pinned_product_ids, updated_at
      FROM aarvo_home_merchandising
      ORDER BY sort_order ASC, section_key ASC
    `);

    const allPinnedIds = [];
    for (const row of sectionsResult.rows) {
      const ids = Array.isArray(row.pinned_product_ids) ? row.pinned_product_ids : [];
      for (const id of ids) {
        const numId = Number(id);
        if (Number.isInteger(numId) && numId > 0) allPinnedIds.push(numId);
      }
    }

    const productsById = new Map();
    if (allPinnedIds.length > 0) {
      const productsResult = await pool.query(
        `SELECT p.id, p.seller_id, p.seller_name, p.name, p.category,
                COALESCE(FLOOR(p.price_paise * (100 - d.discount_percent) / 100), p.price_paise)::bigint AS price_paise,
                COALESCE(p.mrp_price_paise, p.price_paise) AS original_price_paise,
                p.rating, p.description, p.stock_quantity, p.is_published, p.image_url,
                p.cod_enabled, p.online_payment_enabled
         FROM products p
         LEFT JOIN LATERAL (
           SELECT discount_percent FROM aarvo_product_deals
           WHERE product_id = p.id AND status = 'APPROVED' AND starts_at <= now() AND ends_at > now()
           ORDER BY discount_percent DESC LIMIT 1
         ) d ON true
         WHERE p.id = ANY($1::bigint[]) AND p.is_published = true AND p.stock_quantity > 0`,
        [Array.from(new Set(allPinnedIds))]
      );
      for (const p of productsResult.rows) {
        productsById.set(String(p.id), p);
      }
    }

    return sectionsResult.rows.map((row) => {
      const ids = (Array.isArray(row.pinned_product_ids) ? row.pinned_product_ids : []).map((id) =>
        String(id)
      );
      const rawBadge = String(row.badge_text || '').trim();
      const safeBadgeText =
        row.section_key === 'still_looking' || rawBadge.toLowerCase() === 'pink'
          ? ''
          : rawBadge;
      return {
        id: row.section_key,
        title: row.title,
        subtitle: row.subtitle,
        badgeText: safeBadgeText,
        enabled: Boolean(row.enabled),
        maxItems: Number(row.max_items || 3),
        sortOrder: Number(row.sort_order || 0),
        pinnedProductIds: ids,
        pinnedProducts: ids.map((id) => productsById.get(id)).filter(Boolean),
        updatedAt: row.updated_at,
      };
    });
  });

  // Admin: Read all merchandising slots + published catalog summary for quick pinning
  app.get('/v1/admin/home/merchandising', { preHandler: requireRole('ADMIN') }, async () => {
    const sections = (
      await pool.query(`
        SELECT section_key, title, subtitle, badge_text, enabled, max_items, sort_order, pinned_product_ids, updated_by, updated_at
        FROM aarvo_home_merchandising
        ORDER BY sort_order ASC, section_key ASC
      `)
    ).rows;

    const products = (
      await pool.query(`
        SELECT p.id, p.name, p.category, p.seller_name, p.price_paise,
               COALESCE(p.mrp_price_paise, p.price_paise) AS original_price_paise,
               p.stock_quantity, p.image_url
        FROM products p
        WHERE p.is_published = true AND p.stock_quantity > 0
        ORDER BY p.created_at DESC
        LIMIT 200
      `)
    ).rows;

    return {
      sections: sections.map((row) => ({
        id: row.section_key,
        title: row.title,
        subtitle: row.subtitle,
        badgeText:
          row.section_key === 'still_looking' ||
          String(row.badge_text || '').trim().toLowerCase() === 'pink'
            ? ''
            : row.badge_text,
        enabled: Boolean(row.enabled),
        maxItems: Number(row.max_items || 3),
        sortOrder: Number(row.sort_order || 0),
        pinnedProductIds: (Array.isArray(row.pinned_product_ids) ? row.pinned_product_ids : []).map(
          String
        ),
        updatedBy: row.updated_by,
        updatedAt: row.updated_at,
      })),
      products,
    };
  });

  // Admin: Update a specific merchandising slot & pin products managed by Admin
  app.put('/v1/admin/home/merchandising/:key', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const sectionKey = String(request.params.key || '').trim();
    if (!ALLOWED_KEYS.has(sectionKey)) {
      return reply.code(400).send({ error: 'INVALID_MERCHANDISING_SECTION' });
    }
    const body = request.body || {};
    const title = String(body.title || '').trim();
    if (!title || title.length > 80) {
      return reply.code(400).send({ error: 'INVALID_SECTION_TITLE' });
    }
    const subtitle = String(body.subtitle || '').trim().slice(0, 140);
    const rawBadgeText = String(body.badgeText || '').trim().slice(0, 28);
    const badgeText =
      sectionKey === 'still_looking' || rawBadgeText.toLowerCase() === 'pink' ? '' : rawBadgeText;
    const enabled = body.enabled !== false;
    const maxItemsRaw = Number(body.maxItems ?? 3);
    const maxItems = Number.isInteger(maxItemsRaw) ? Math.min(16, Math.max(2, maxItemsRaw)) : 3;
    const sortOrder = Number.isInteger(Number(body.sortOrder)) ? Number(body.sortOrder) : 0;

    const rawIds = Array.isArray(body.pinnedProductIds) ? body.pinnedProductIds : [];
    const pinnedProductIds = Array.from(
      new Set(
        rawIds
          .map((id) => String(id).trim())
          .filter((id) => /^\d+$/.test(id))
      )
    ).slice(0, 16);

    const updated = await pool.query(
      `UPDATE aarvo_home_merchandising
       SET title = $1,
           subtitle = $2,
           badge_text = $3,
           enabled = $4,
           max_items = $5,
           sort_order = $6,
           pinned_product_ids = $7::jsonb,
           updated_by = $8,
           updated_at = now()
       WHERE section_key = $9
       RETURNING section_key, title, subtitle, badge_text, enabled, max_items, sort_order, pinned_product_ids, updated_by, updated_at`,
      [
        title,
        subtitle,
        badgeText,
        enabled,
        maxItems,
        sortOrder,
        JSON.stringify(pinnedProductIds),
        String(request.user?.sub || request.user?.email || 'ADMIN'),
        sectionKey,
      ]
    );

    if (audit) {
      await audit(pool, request.user, 'HOME_MERCHANDISING', sectionKey, 'MERCHANDISING_UPDATED', {
        enabled,
        maxItems,
        sortOrder,
        pinnedCount: pinnedProductIds.length,
      });
    }

    const row = updated.rows[0];
    return {
      id: row.section_key,
      title: row.title,
      subtitle: row.subtitle,
      badgeText: row.badge_text,
      enabled: Boolean(row.enabled),
      maxItems: Number(row.max_items),
      sortOrder: Number(row.sort_order),
      pinnedProductIds: (Array.isArray(row.pinned_product_ids) ? row.pinned_product_ids : []).map(
        String
      ),
      updatedBy: row.updated_by,
      updatedAt: row.updated_at,
    };
  });
}
