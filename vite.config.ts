import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import type {IncomingMessage, ServerResponse} from 'node:http';
import {defineConfig} from 'vite';

function extractGeoComponent(
  components: Array<{long_name?: string; types?: string[]}>,
  types: string[]
): string {
  for (const t of types) {
    const match = components.find((c) => Array.isArray(c.types) && c.types.includes(t));
    if (match?.long_name) return String(match.long_name).trim();
  }
  return '';
}

function devMapsGeocodeMiddleware() {
  return {
    name: 'aarvo-maps-geocode-dev-proxy',
    configureServer(server: {
      middlewares: {
        use: (
          path: string,
          handler: (req: IncomingMessage, res: ServerResponse) => Promise<void>
        ) => void;
      };
    }) {
      server.middlewares.use('/api/maps/geocode', async (req, res) => {
        const apiKey = String(
          process.env.GOOGLE_MAPS_API_KEY ||
            process.env.MAPS_API_KEY ||
            process.env.VITE_GOOGLE_MAPS_API_KEY ||
            ''
        ).trim();

        const sendJson = (code: number, payload: Record<string, unknown>) => {
          res.statusCode = code;
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify(payload));
        };

        if (!apiKey) {
          sendJson(503, {error: 'MAPS_NOT_CONFIGURED'});
          return;
        }

        const reqUrl = new URL(req.url || '/', 'http://localhost');
        const lat = reqUrl.searchParams.get('lat');
        const lng = reqUrl.searchParams.get('lng');
        const address = reqUrl.searchParams.get('address')?.trim() || '';

        const target = new URL('https://maps.googleapis.com/maps/api/geocode/json');
        target.searchParams.set('key', apiKey);
        target.searchParams.set('region', 'in');
        target.searchParams.set('language', 'en');

        if (lat && lng) {
          target.searchParams.set('latlng', `${lat},${lng}`);
        } else if (address) {
          target.searchParams.set('address', address.slice(0, 240));
        } else {
          sendJson(400, {error: 'MISSING_LOCATION_QUERY'});
          return;
        }

        try {
          const upstream = await fetch(target.toString());
          const data = (await upstream.json().catch(() => ({}))) as {
            status?: string;
            error_message?: string;
            results?: Array<{
              formatted_address?: string;
              address_components?: Array<{long_name?: string; types?: string[]}>;
              geometry?: {location?: {lat?: number; lng?: number}};
            }>;
          };

          if (
            upstream.status === 429 ||
            data.status === 'OVER_QUERY_LIMIT' ||
            data.status === 'OVER_DAILY_LIMIT'
          ) {
            sendJson(429, {
              error: 'RESOURCE_EXHAUSTED',
              message: data.error_message || 'Google Maps Geocoding quota exceeded.',
            });
            return;
          }

          const first = Array.isArray(data.results) ? data.results[0] : undefined;
          if (!first) {
            sendJson(200, {
              found: false,
              formattedAddress: '',
              line1: '',
              line2: '',
              city: '',
              state: '',
              postalCode: '',
              lat: null,
              lng: null,
            });
            return;
          }

          const comps = Array.isArray(first.address_components) ? first.address_components : [];
          const premise = extractGeoComponent(comps, ['premise', 'subpremise']);
          const streetNum = extractGeoComponent(comps, ['street_number']);
          const route = extractGeoComponent(comps, ['route']);
          const sublocality = extractGeoComponent(comps, [
            'sublocality_level_2',
            'sublocality_level_1',
            'sublocality',
            'neighborhood',
          ]);
          const city = extractGeoComponent(comps, [
            'locality',
            'administrative_area_level_2',
            'postal_town',
          ]);
          const state = extractGeoComponent(comps, ['administrative_area_level_1']);
          const postalCode = extractGeoComponent(comps, ['postal_code'])
            .replace(/\D/g, '')
            .slice(0, 6);
          const formattedAddress = String(first.formatted_address || '').trim();
          const line1 =
            [premise, streetNum, route].filter(Boolean).join(' ').trim() ||
            sublocality ||
            formattedAddress.split(',').slice(0, 2).join(', ').trim();

          sendJson(200, {
            found: true,
            formattedAddress,
            line1,
            line2: sublocality && !line1.includes(sublocality) ? sublocality : '',
            city,
            state,
            postalCode,
            lat: first.geometry?.location?.lat ?? null,
            lng: first.geometry?.location?.lng ?? null,
          });
        } catch (err) {
          sendJson(502, {
            error: 'GEOCODE_UPSTREAM_ERROR',
            message: err instanceof Error ? err.message : 'Geocoding failed',
          });
        }
      });
    },
  };
}

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss(), devMapsGeocodeMiddleware()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modify—file watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
