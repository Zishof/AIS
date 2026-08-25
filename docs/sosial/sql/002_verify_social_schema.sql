-- Read-only. PASS bila query pertama tidak mengembalikan baris.
WITH expected(table_name) AS (VALUES
 ('donatur'),('gelombang_donatur'),('kategori_program_donatur'),('penyaluran_donasi'),('program_donatur'),
 ('social_tenant_setting'),('sosial_channel'),('social_donor_identity'),('jenis_dana_sosial'),('jenis_zakat'),
 ('kebijakan_perhitungan_zakat'),('perhitungan_zakat'),('social_program_extension'),('transaksi_donasi'),
 ('alokasi_donasi'),('pembayaran_donasi'),('perkembangan_program_sosial'),('kategori_penerima_manfaat'),
 ('detail_penyaluran_donasi'),('bukti_setor_sosial'),('social_payment_reconciliation'),
 ('social_correction_event'),('social_prayer_message')
)
SELECT 'MISSING_TABLE' AS issue,e.table_name
FROM expected e LEFT JOIN information_schema.tables t
 ON t.table_schema='public' AND t.table_name=e.table_name
WHERE t.table_name IS NULL ORDER BY e.table_name;

-- Semua 18 entitas baru mewarisi kolom SocialRecord berikut.
WITH entities(table_name) AS (VALUES
 ('social_tenant_setting'),('sosial_channel'),('social_donor_identity'),('jenis_dana_sosial'),('jenis_zakat'),
 ('kebijakan_perhitungan_zakat'),('perhitungan_zakat'),('social_program_extension'),('transaksi_donasi'),
 ('alokasi_donasi'),('pembayaran_donasi'),('perkembangan_program_sosial'),('kategori_penerima_manfaat'),
 ('detail_penyaluran_donasi'),('bukti_setor_sosial'),('social_payment_reconciliation'),
 ('social_correction_event'),('social_prayer_message')
), required(column_name) AS (VALUES ('id'),('tenant_key'),('status'),('created_at'),('updated_at'),('created_by'),('updated_by'))
SELECT 'MISSING_BASE_COLUMN' AS issue,e.table_name,r.column_name
FROM entities e CROSS JOIN required r
LEFT JOIN information_schema.columns c ON c.table_schema='public' AND c.table_name=e.table_name AND c.column_name=r.column_name
WHERE c.column_name IS NULL ORDER BY e.table_name,r.column_name;

-- Precision uang wajib numeric(19,2).
WITH money(table_name,column_name) AS (VALUES
 ('transaksi_donasi','gross_donation_amount'),('transaksi_donasi','platform_contribution'),('transaksi_donasi','gateway_fee'),('transaksi_donasi','total_payable'),
 ('pembayaran_donasi','request_amount'),('pembayaran_donasi','fee'),('pembayaran_donasi','total'),
 ('alokasi_donasi','amount'),('detail_penyaluran_donasi','amount'),('social_correction_event','amount')
)
SELECT 'INVALID_MONEY_COLUMN' AS issue,m.table_name,m.column_name,c.data_type,c.numeric_precision,c.numeric_scale
FROM money m LEFT JOIN information_schema.columns c ON c.table_schema='public' AND c.table_name=m.table_name AND c.column_name=m.column_name
WHERE c.column_name IS NULL OR c.data_type<>'numeric' OR c.numeric_precision<>19 OR c.numeric_scale<>2
ORDER BY m.table_name,m.column_name;
