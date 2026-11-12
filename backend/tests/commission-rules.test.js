import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { commissionBpsForCategory, commissionPaise, commissionRatePercent, COMMISSION_RANGE } from '../src/commission-rules.js';

const launcher = fs.readFileSync(new URL('../src/launcher.js', import.meta.url), 'utf8');

test('AARVO applies a flat 3% commission to every product category', () => {
  for (const category of ['MOBILE', 'ELECTRONICS', 'FASHION', 'HOME & KITCHEN', 'BEAUTY', 'GENERAL MERCHANDISE', 'unlisted-category', '', null]) {
    assert.equal(commissionBpsForCategory(category), 300);
    assert.equal(commissionRatePercent(category), 3);
  }
  assert.deepEqual(COMMISSION_RANGE, { minPercent: 3, maxPercent: 3 });
  assert.equal(commissionPaise(10000, 'MOBILE'), 300);
  assert.equal(commissionPaise(9999, 'unlisted-category'), 299);
});

test('seller commission is deducted from seller proceeds, not added twice to buyer checkout', () => {
  assert.ok(launcher.includes('total=subtotal+deliveryFee,orderId=randomUUID()'));
  assert.ok(!launcher.includes('total=subtotal+deliveryFee+platformFee'));
  assert.ok(launcher.includes('sellerAmount=lineTotal-commissionPaise(lineTotal,p.category)'));
});
