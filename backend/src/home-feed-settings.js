const DEFAULT_CONFIG = {
  sections: [
    { id: 'stillLooking', label: 'Still Looking for These?', enabled: true },
    { id: 'suggested', label: 'Suggested For You', enabled: true },
    { id: 'topDeals', label: 'Top Value Deals', enabled: true },
    { id: 'brands', label: 'Brands in Spotlight', enabled: true },
    { id: 'lowestLive', label: 'Lowest Price on Live', enabled: true },
    { id: 'moreProducts', label: 'More Products for You', enabled: true },
    { id: 'sponsored', label: 'Sponsored Products', enabled: false }
  ]
};

function normalizeConfig(value) {
  const incoming = Array.isArray(value?.sections) ? value.sections : [];
  const byId = new Map(incoming.map((item) => [String(item?.id || ''), item]));
  const sections = DEFAULT_CONFIG.sections.map((base) => {
    const item = byId.get(base.id);
    return { ...base, enabled: typeof item?.enabled === 'boolean' ? item.enabled : base.enabled };
  });
  const order = incoming.map((item) => String(item?.id || '')).filter((id) => sections.some((section) => section.id === id));
  const ordered = [...new Set(order)].map((id) => sections.find((section) => section.id === id));
  for (const section of sections) if (!ordered.includes(section)) ordered.push(section);
  return { sections: ordered };
}

export async function registerHomeFeedSettings({ app, pool, requireRole, audit }) {
  if (!pool) return;
  await pool.query(`CREATE TABLE IF NOT EXISTS aarvo_home_feed_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    config JSONB NOT NULL,
    updated_by TEXT REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
  )`);
  await pool.query(
    'INSERT INTO aarvo_home_feed_settings(id,config) VALUES(1,$1::jsonb) ON CONFLICT(id) DO NOTHING',
    [JSON.stringify(DEFAULT_CONFIG)]
  );
  app.get('/v1/home-feed/settings', async () => {
    const result = await pool.query('SELECT config,updated_at AS "updatedAt" FROM aarvo_home_feed_settings WHERE id=1');
    return normalizeConfig(result.rows[0]?.config || DEFAULT_CONFIG);
  });
  app.get('/v1/admin/home-feed/settings', { preHandler: requireRole('ADMIN') }, async () => {
    const result = await pool.query('SELECT config,updated_at AS "updatedAt" FROM aarvo_home_feed_settings WHERE id=1');
    return normalizeConfig(result.rows[0]?.config || DEFAULT_CONFIG);
  });
  app.put('/v1/admin/home-feed/settings', { preHandler: requireRole('ADMIN') }, async (request, reply) => {
    const raw = request.body?.config;
    if (!raw || !Array.isArray(raw.sections) || raw.sections.length !== DEFAULT_CONFIG.sections.length) {
      return reply.code(400).send({ error: 'INVALID_HOME_FEED_SETTINGS' });
    }
    const ids = raw.sections.map((item) => String(item?.id || ''));
    if (new Set(ids).size !== DEFAULT_CONFIG.sections.length || DEFAULT_CONFIG.sections.some((item) => !ids.includes(item.id)) ||
        raw.sections.some((item) => typeof item.enabled !== 'boolean')) {
      return reply.code(400).send({ error: 'INVALID_HOME_FEED_SETTINGS' });
    }
    const config = normalizeConfig(raw);
    await pool.query(
      'INSERT INTO aarvo_home_feed_settings(id,config,updated_by,updated_at) VALUES(1,$1::jsonb,$2,now()) ON CONFLICT(id) DO UPDATE SET config=EXCLUDED.config,updated_by=EXCLUDED.updated_by,updated_at=now()',
      [JSON.stringify(config), request.user.sub]
    );
    await audit(pool, request.user, 'HOME_FEED', 1, 'SETTINGS_UPDATED', { sections: config.sections });
    return { saved: true, config };
  });
}
