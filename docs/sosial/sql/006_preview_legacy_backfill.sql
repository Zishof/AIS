-- Read-only preview. Ganti nilai tenant hanya di sesi psql; jangan commit tenant produksi ke file.
WITH parameter AS (SELECT :'tenant_key'::varchar tenant_key),
donor_preview AS (
 SELECT d.id,d.kode,d.nama,d.email,d.telp,
  CASE WHEN nullif(btrim(d.nama),'') IS NULL THEN 'REJECT_NAMA_KOSONG'
       WHEN i.id IS NOT NULL THEN 'ALREADY_MAPPED' ELSE 'READY' END disposition
 FROM donatur d CROSS JOIN parameter p
 LEFT JOIN social_donor_identity i ON i.tenant_key=p.tenant_key AND i.donatur_id=d.id
), program_preview AS (
 SELECT x.id,x.kode,x.nama,x.donaturs,
  CASE WHEN nullif(btrim(x.nama),'') IS NULL THEN 'REJECT_NAMA_KOSONG'
       WHEN e.id IS NOT NULL THEN 'ALREADY_MAPPED' ELSE 'READY_REQUIRES_DEFAULT_FUND' END disposition
 FROM program_donatur x CROSS JOIN parameter p
 LEFT JOIN social_program_extension e ON e.program_id=x.id
)
SELECT 'DONOR' entity,disposition,count(*) rows FROM donor_preview GROUP BY disposition
UNION ALL SELECT 'PROGRAM',disposition,count(*) FROM program_preview GROUP BY disposition
ORDER BY entity,disposition;

-- Detail CSV donor legacy perlu keputusan manual/normalisasi; tidak dimutasi oleh preview ini.
SELECT id,kode,nama,donaturs FROM program_donatur
WHERE nullif(btrim(donaturs),'') IS NOT NULL ORDER BY id;
