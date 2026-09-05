# AARVO Security Policy

## Supported security-sensitive areas

AARVO treats authentication, authorization, payment verification, seller access, stock integrity, order state changes and webhook authenticity as security-sensitive.

## Reporting a vulnerability

Do not publish credentials, tokens, personal data or exploitable proof-of-concept payloads in a public issue. Contact the repository owner privately with:

- affected component or route;
- impact and expected security boundary;
- minimal reproduction details;
- suggested mitigation, if known.

## Secrets

Never commit production API keys, Razorpay secrets, webhook secrets, database credentials, JWT secrets or Android signing keys. Use the deployment/build secret store.

## Payment integrity

Payment success reported by the Android client is not sufficient to fulfill an order. Server-side verification and gateway reconciliation are required before an order is considered paid.

## Disclosure

After a fix is deployed, document the affected component and remediation without exposing customer data or secret material.
