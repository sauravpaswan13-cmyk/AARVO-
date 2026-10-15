# AARVO Shiprocket multi-seller delivery

AARVO uses Shiprocket as a third-party courier provider; it does not operate a Rider App.

## Current implementation path

- Admin triggers delivery for an order in PACKED/eligible state.
- Backend groups order lines by the product's seller.
- Each seller must have a saved pickup phone, email (business email or seller account email), address, city, state, and six-digit PIN in Seller Central.
- The backend checks Shiprocket pickup addresses and creates a seller-specific pickup location when missing.
- The order is split into one Shiprocket shipment per seller, each with its own AWB and tracking record.
- AARVO stores each shipment and updates the order tracking JSON as Shiprocket webhook updates arrive.

## Required Render environment variables

Set secrets directly in Render; never commit them or send them in chat:

- `THIRD_PARTY_DELIVERY_ENABLED=true` only after validating the account and integration.
- `THIRD_PARTY_DELIVERY_PROVIDER=shiprocket`
- `THIRD_PARTY_DELIVERY_BASE_URL=https://apiv2.shiprocket.in`
- `SHIPROCKET_API_EMAIL` and `SHIPROCKET_API_PASSWORD` for the API user created in Shiprocket Settings → API.
- `THIRD_PARTY_DELIVERY_WEBHOOK_SECRET` only when a matching signature-verification setup is configured.
- `THIRD_PARTY_DELIVERY_WEBHOOK_URL=https://aarvo-api.onrender.com/v1/webhooks/delivery/shiprocket`
- Package dimensions and weight defaults; replace defaults with realistic per-package values before production.

The legacy `SHIPROCKET_PICKUP_LOCATION` is not used as a substitute for a missing seller address. The backend must not ship a seller's parcel from another seller's or AARVO's address.

## Safe activation checklist

1. Confirm the Shiprocket API user can access Orders, Pickup Addresses, AWB/Courier assignment, and pickup generation.
2. Confirm the Shiprocket account is KYC-complete and usable for actual courier bookings.
3. Confirm seller pickup addresses and contact numbers are collected and saved correctly.
4. Test API authentication and pickup-address listing/creation with a test seller address.
5. Place a controlled test order, validate each seller gets a separate shipment/AWB, and confirm pickup booking and tracking updates.
6. Only then enable delivery in production.

Do not run real shipments just to test credentials; Shiprocket shipment creation can create billable/operational courier bookings. Do not expose API credentials in screenshots, GitHub, or chat.


**Shiprocket webhook security:** set `THIRD_PARTY_DELIVERY_WEBHOOK_SECRET` to the same security token entered in Shiprocket Settings → API → Webhooks. Shiprocket sends it in the `x-api-key` header; AARVO validates that header using a constant-time comparison. Do not use an HMAC signature header for this integration.
