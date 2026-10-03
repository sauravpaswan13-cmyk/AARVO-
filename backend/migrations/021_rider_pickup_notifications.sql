-- Notify the assigned rider whenever an order becomes ready for pickup.
-- This is database-triggered so it works regardless of which seller/admin API
-- changes the order status.

CREATE OR REPLACE FUNCTION notify_rider_when_order_ready()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.status IS DISTINCT FROM OLD.status
     AND UPPER(COALESCE(NEW.status, '')) IN (
       'READY_FOR_PICKUP',
       'READY',
       'PACKED'
     ) THEN
    INSERT INTO rider_notifications (rider_id, order_id, title, body)
    SELECT da.rider_id,
           NEW.id,
           'Order ready for pickup',
           'AARVO order ' || LEFT(NEW.id::text, 12) || ' is ready. Please pick up the order from the seller.'
    FROM delivery_assignments da
    WHERE da.order_id = NEW.id
      AND da.status IN ('ASSIGNED', 'ACCEPTED')
      AND NOT EXISTS (
        SELECT 1
        FROM rider_notifications rn
        WHERE rn.rider_id = da.rider_id
          AND rn.order_id = NEW.id
          AND rn.title = 'Order ready for pickup'
          AND rn.created_at > now() - interval '24 hours'
      );
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_orders_rider_pickup_notification ON orders;

CREATE TRIGGER trg_orders_rider_pickup_notification
AFTER UPDATE OF status ON orders
FOR EACH ROW
EXECUTE FUNCTION notify_rider_when_order_ready();
