-- Read-only investigation. Run against the affected database; no records are changed.
-- Different dispense IDs/quantities may represent legitimate partial dispensing.
-- Confirm physical handover and stock history before correcting any invoice.
SELECT i.invoice_number, i.total, i.amount_paid, i.balance_due, i.status,
       ii.id AS invoice_item_id, ii.description, ii.quantity AS billed_quantity,
       ii.unit_price, ii.line_total, ii.reference_type, ii.reference_id,
       di.id AS dispense_record_id, di.prescription_item_id,
       di.quantity AS dispensed_quantity, di.dispensed_date, di.dispensed_by,
       pi.quantity AS prescribed_quantity
FROM invoices i
JOIN invoice_items ii ON ii.invoice_id = i.id
LEFT JOIN dispensed_items di
  ON ii.reference_type = 'DISPENSED_ITEM' AND ii.reference_id = di.id
LEFT JOIN prescription_items pi ON pi.id = di.prescription_item_id
WHERE i.invoice_number = 'INV-2026-9D19DFB4'
ORDER BY ii.id;

-- One source dispense record billed multiple times is a definite duplicate reference.
SELECT ii.reference_id AS dispense_record_id, COUNT(*) AS charge_count,
       SUM(ii.line_total) AS total_charged
FROM invoice_items ii
WHERE ii.reference_type = 'DISPENSED_ITEM'
GROUP BY ii.reference_id
HAVING COUNT(*) > 1;

-- More units recorded as dispensed than prescribed needs investigation.
SELECT pi.id AS prescription_item_id, pi.quantity AS prescribed_quantity,
       SUM(di.quantity) AS recorded_dispensed_quantity
FROM prescription_items pi
JOIN dispensed_items di ON di.prescription_item_id = pi.id
GROUP BY pi.id, pi.quantity
HAVING SUM(di.quantity) > pi.quantity;
