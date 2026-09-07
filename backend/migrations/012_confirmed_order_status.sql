-- Keep the marketplace lifecycle aligned with the buyer/seller workflow:
-- successful payment should move an order into CONFIRMED before seller fulfillment.

CREATE OR REPLACE FUNCTION aarvo_normalize_paid_order_status()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.status = 'PAID' THEN
    NEW.status := 'CONFIRMED';
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_orders_paid_to_confirmed ON orders;
CREATE TRIGGER trg_orders_paid_to_confirmed
BEFORE INSERT OR UPDATE OF status ON orders
FOR EACH ROW
EXECUTE FUNCTION aarvo_normalize_paid_order_status();

UPDATE orders
SET status = 'CONFIRMED'
WHERE status = 'PAID';
