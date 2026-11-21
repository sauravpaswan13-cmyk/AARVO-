function extractComponent(components, types) {
  if (!Array.isArray(components)) return '';
  for (const t of types) {
    const match = components.find((c) => Array.isArray(c.types) && c.types.includes(t));
    if (match?.long_name) return String(match.long_name).trim();
  }
  return '';
}

export function parseGoogleGeocodeResult(result) {
  if (!result || typeof result !== 'object') return null;
  const comps = Array.isArray(result.address_components) ? result.address_components : [];
  const streetNum = extractComponent(comps, ['street_number']);
  const premise = extractComponent(comps, ['premise', 'subpremise']);
  const route = extractComponent(comps, ['route']);
  const sublocality = extractComponent(comps, [
    'sublocality_level_2',
    'sublocality_level_1',
    'sublocality',
    'neighborhood',
  ]);
  const city =
    extractComponent(comps, ['locality', 'administrative_area_level_2', 'postal_town']) || '';
  const state = extractComponent(comps, ['administrative_area_level_1']) || '';
  const postalCode = extractComponent(comps, ['postal_code']).replace(/\D/g, '').slice(0, 6);

  const line1Parts = [premise, streetNum, route].filter(Boolean);
  const formattedAddress = String(result.formatted_address || '').trim();
  const line1 =
    line1Parts.join(' ').trim() ||
    sublocality ||
    formattedAddress.split(',').slice(0, 2).join(', ').trim();

  const loc = result.geometry?.location || {};
  return {
    formattedAddress,
    line1,
    line2: sublocality && !line1.includes(sublocality) ? sublocality : '',
    city,
    state,
    postalCode,
    lat: typeof loc.lat === 'number' ? loc.lat : null,
    lng: typeof loc.lng === 'number' ? loc.lng : null,
  };
}

export async function registerGoogleMapsRoutes({ app }) {
  if (!app) return;

  app.get('/v1/maps/geocode', async (request, reply) => {
    const apiKey = String(
      process.env.GOOGLE_MAPS_API_KEY ||
        process.env.MAPS_API_KEY ||
        process.env.VITE_GOOGLE_MAPS_API_KEY ||
        ''
    ).trim();

    if (!apiKey) {
      return reply.code(503).send({ error: 'MAPS_NOT_CONFIGURED' });
    }

    const latRaw = request.query?.lat;
    const lngRaw = request.query?.lng;
    const addressRaw = String(request.query?.address || '').trim();

    const url = new URL('https://maps.googleapis.com/maps/api/geocode/json');
    url.searchParams.set('key', apiKey);
    url.searchParams.set('region', 'in');
    url.searchParams.set('language', 'en');

    if (latRaw !== undefined && lngRaw !== undefined && latRaw !== '' && lngRaw !== '') {
      const lat = Number(latRaw);
      const lng = Number(lngRaw);
      if (!Number.isFinite(lat) || !Number.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
        return reply.code(400).send({ error: 'INVALID_COORDINATES' });
      }
      url.searchParams.set('latlng', `${lat},${lng}`);
    } else if (addressRaw) {
      url.searchParams.set('address', addressRaw.slice(0, 240));
    } else {
      return reply.code(400).send({ error: 'MISSING_LOCATION_QUERY' });
    }

    const upstream = await fetch(url.toString(), { headers: { Accept: 'application/json' } });
    const data = await upstream.json().catch(() => ({}));

    if (
      upstream.status === 429 ||
      data?.status === 'OVER_QUERY_LIMIT' ||
      data?.status === 'OVER_DAILY_LIMIT'
    ) {
      return reply.code(429).send({
        error: 'RESOURCE_EXHAUSTED',
        message: data?.error_message || 'Google Maps Geocoding quota exceeded.',
      });
    }

    if (!upstream.ok || (data?.status && data.status !== 'OK' && data.status !== 'ZERO_RESULTS')) {
      return reply.code(502).send({
        error: 'GEOCODE_UPSTREAM_ERROR',
        status: data?.status || upstream.status,
      });
    }

    const first = Array.isArray(data?.results) ? data.results[0] : null;
    if (!first) {
      return { found: false, formattedAddress: '', line1: '', line2: '', city: '', state: '', postalCode: '', lat: null, lng: null };
    }

    return {
      found: true,
      ...parseGoogleGeocodeResult(first),
    };
  });
}
