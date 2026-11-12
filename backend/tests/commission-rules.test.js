import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { commissionBpsForCategory, commissionPaise, commissionRatePercent, COMMISSION_RANGE } from '../src/commission-rules.js';

const launcher = fs.readFileSync(new URL('../src/launcher.js', import.meta.url), 'utf8');

test('AARVO applies category-wise, price-wise commission rates with 0% below ₹1,000', () => {
  const bands = [
    [99999, 0],
    [100000, 1],
    [199999, 1],
    [200000, 2],
    [499999, 2],
    [500000, 3],
    [999999, 3],
    [1000000, 4],
    [2499999, 4],
    [2500000, 5]
  ];
  for (const [price, band] of bands) {
    const expectedMobile = [0, 200, 300, 400, 500, 600][band];
    assert.equal(commissionBpsForCategory('MOBILE', price), expectedMobile);
  }
  assert.equal(commissionBpsForCategory('ELECTRONICS', 100000), 300);
  assert.equal(commissionBpsForCategory('FASHION', 100000), 400);
  assert.equal(commissionBpsForCategory('HOME & KITCHEN', 100000), 300);
  assert.equal(commissionBpsForCategory('BEAUTY', 100000), 400);
  assert.equal(commissionBpsForCategory('GENERAL MERCHANDISE', 100000), 300);
  assert.equal(commissionBpsForCategory('unlisted-category', 100000), 300);
  assert.equal(commissionRatePercent('MOBILE', 100000), 2);
  assert.deepEqual(COMMISSION_RANGE, { minPercent: 0, maxPercent: 8 });
  assert.equal(commissionPaise(100000, 'MOBILE', 100000), 2000);
  assert.equal(commissionPaise(199999, 'MOBILE', 99999), 0);
});

test('the rate band uses unit product price while commission applies to the full order line', () => {
  assert.equal(commissionPaise(200000, 'MOBILE', 100000), 4000);
  assert.equal(commissionPaise(200000, 'FASHION', 100000), 8000);
});

test('seller commission is deducted from seller proceeds, not added twice to buyer checkout', () => {
  assert.ok(launcher.includes('total=subtotal+deliveryFee,orderId=randomUUID()'));
  assert.ok(!launcher.includes('total=subtotal+deliveryFee+platformFee'));
  assert.ok(launcher.includes('sellerAmount=lineTotal-commissionPaise(lineTotal,p.category,Number(p.price_paise))'));
});
