-- Read-only tenant-level financial invariant report.
WITH paid AS (
 SELECT tenant_key,coalesce(sum(request_amount),0)::numeric(19,2) settled
 FROM pembayaran_donasi WHERE payment_status='PAID' GROUP BY tenant_key
), returned AS (
 SELECT tenant_key,coalesce(sum(amount),0)::numeric(19,2) returned
 FROM social_correction_event WHERE status='POSTED' AND correction_type IN ('REFUND','REVERSAL') GROUP BY tenant_key
), allocated AS (
 SELECT tenant_key,coalesce(sum(amount),0)::numeric(19,2) allocated
 FROM alokasi_donasi WHERE status='POSTED' GROUP BY tenant_key
), distributed AS (
 SELECT tenant_key,coalesce(sum(amount),0)::numeric(19,2) distributed
 FROM detail_penyaluran_donasi WHERE status='POSTED' GROUP BY tenant_key
), tenants AS (
 SELECT tenant_key FROM paid UNION SELECT tenant_key FROM returned UNION SELECT tenant_key FROM allocated UNION SELECT tenant_key FROM distributed
)
SELECT t.tenant_key,coalesce(p.settled,0) settled,coalesce(r.returned,0) returned,
 coalesce(a.allocated,0) allocated,coalesce(d.distributed,0) distributed,
 coalesce(p.settled,0)-coalesce(r.returned,0)-coalesce(a.allocated,0) unallocated_available,
 coalesce(a.allocated,0)-coalesce(d.distributed,0) allocation_available,
 CASE WHEN coalesce(r.returned,0)>coalesce(p.settled,0)
        OR coalesce(a.allocated,0)>coalesce(p.settled,0)-coalesce(r.returned,0)
        OR coalesce(d.distributed,0)>coalesce(a.allocated,0)
      THEN 'EXCEPTION' ELSE 'PASS' END invariant_status
FROM tenants t LEFT JOIN paid p USING(tenant_key) LEFT JOIN returned r USING(tenant_key)
LEFT JOIN allocated a USING(tenant_key) LEFT JOIN distributed d USING(tenant_key)
ORDER BY t.tenant_key;

-- Detail over-allocation / over-distribution.
SELECT 'TRANSACTION_OVER_ALLOCATED' issue,t.tenant_key,t.transaction_number reference,t.gross_donation_amount expected,sum(a.amount) actual
FROM transaksi_donasi t JOIN alokasi_donasi a ON a.transaction_id=t.id AND a.status='POSTED'
GROUP BY t.id,t.tenant_key,t.transaction_number,t.gross_donation_amount HAVING sum(a.amount)>t.gross_donation_amount
UNION ALL
SELECT 'ALLOCATION_OVER_DISTRIBUTED',a.tenant_key,a.id::text,a.amount,sum(d.amount)
FROM alokasi_donasi a JOIN detail_penyaluran_donasi d ON d.source_allocation_id=a.id AND d.status='POSTED'
GROUP BY a.id,a.tenant_key,a.amount HAVING sum(d.amount)>a.amount;
