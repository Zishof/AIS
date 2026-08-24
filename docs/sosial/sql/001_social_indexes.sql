-- Jalankan setelah hbm2ddl membuat tabel baru. Review nama schema/tabel pada target.
CREATE INDEX IF NOT EXISTS idx_social_tx_tenant_status_created ON public.transaksi_donasi (tenant_key,status,created_at);
CREATE INDEX IF NOT EXISTS idx_social_payment_tenant_status_expiry ON public.pembayaran_donasi (tenant_key,payment_status,expiry_at);
CREATE INDEX IF NOT EXISTS idx_social_payment_reconcile ON public.pembayaran_donasi (tenant_key,reconciliation_status,paid_at);
CREATE INDEX IF NOT EXISTS idx_sosial_channel_tenant_active ON public.sosial_channel (tenant_key,aktif,provider);
CREATE INDEX IF NOT EXISTS idx_social_fund_channel ON public.jenis_dana_sosial (tenant_key,sosial_channel_id);
CREATE INDEX IF NOT EXISTS idx_social_tx_channel ON public.transaksi_donasi (tenant_key,sosial_channel_id,created_at);
CREATE INDEX IF NOT EXISTS idx_social_payment_channel ON public.pembayaran_donasi (tenant_key,sosial_channel_id,issued_at);
CREATE INDEX IF NOT EXISTS idx_social_program_public ON public.social_program_extension (tenant_key,public_status,published_at);
CREATE INDEX IF NOT EXISTS idx_social_policy_effective ON public.kebijakan_perhitungan_zakat (tenant_key,jenis_zakat_id,status,effective_from,effective_until);
CREATE INDEX IF NOT EXISTS idx_social_allocation_transaction ON public.alokasi_donasi (transaction_id,status);
CREATE INDEX IF NOT EXISTS idx_social_distribution_allocation ON public.detail_penyaluran_donasi (source_allocation_id,status);
CREATE INDEX IF NOT EXISTS idx_social_receipt_transaction ON public.bukti_setor_sosial (transaction_id,status);
CREATE INDEX IF NOT EXISTS idx_social_reconciliation_exception ON public.social_payment_reconciliation (tenant_key,status,settlement_date);
