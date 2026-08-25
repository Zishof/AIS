-- Envers AIS: schema new_audit, suffix __audit. PASS bila tidak ada MISSING_AUDIT_TABLE/COLUMN.
WITH base(table_name) AS (VALUES
 ('donatur'),('gelombang_donatur'),('kategori_program_donatur'),('penyaluran_donasi'),('program_donatur'),
 ('social_tenant_setting'),('sosial_channel'),('social_donor_identity'),('jenis_dana_sosial'),('jenis_zakat'),
 ('kebijakan_perhitungan_zakat'),('perhitungan_zakat'),('social_program_extension'),('transaksi_donasi'),
 ('alokasi_donasi'),('pembayaran_donasi'),('perkembangan_program_sosial'),('kategori_penerima_manfaat'),
 ('detail_penyaluran_donasi'),('bukti_setor_sosial'),('social_payment_reconciliation'),
 ('social_correction_event'),('social_prayer_message')
)
SELECT 'MISSING_AUDIT_TABLE' issue,b.table_name,b.table_name||'__audit' expected
FROM base b LEFT JOIN information_schema.tables t ON t.table_schema='new_audit' AND t.table_name=b.table_name||'__audit'
WHERE t.table_name IS NULL ORDER BY b.table_name;

-- Kolom basis yang tidak ada pada audit. rev/revtype hanya ada di audit sehingga tidak ikut dibandingkan.
WITH base_tables(table_name) AS (VALUES
 ('social_tenant_setting'),('sosial_channel'),('social_donor_identity'),('jenis_dana_sosial'),('jenis_zakat'),
 ('kebijakan_perhitungan_zakat'),('perhitungan_zakat'),('social_program_extension'),('transaksi_donasi'),
 ('alokasi_donasi'),('pembayaran_donasi'),('perkembangan_program_sosial'),('kategori_penerima_manfaat'),
 ('detail_penyaluran_donasi'),('bukti_setor_sosial'),('social_payment_reconciliation'),
 ('social_correction_event'),('social_prayer_message')
)
SELECT 'MISSING_AUDIT_COLUMN' issue,b.table_name,c.column_name
FROM base_tables b JOIN information_schema.columns c ON c.table_schema='public' AND c.table_name=b.table_name
LEFT JOIN information_schema.columns a ON a.table_schema='new_audit' AND a.table_name=b.table_name||'__audit' AND a.column_name=c.column_name
WHERE a.column_name IS NULL ORDER BY b.table_name,c.ordinal_position;
