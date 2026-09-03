# ALTER FK Detail Kegiatan ke Pembayaran Bulanan

## Hasil

Disiapkan migrasi idempotent untuk mengubah foreign key:

`public.detail_kegiatan.pengaturan_pembayaran_bulanan`

menjadi `ON DELETE CASCADE` terhadap:

`public.pengaturan_pembayaran_bulanan.id`.

Perubahan ini menyelesaikan error PostgreSQL berikut ketika susunan rencana
pembayaran bulanan diubah dan baris lama dihapus:

```text
update or delete on table "pengaturan_pembayaran_bulanan" violates foreign key
constraint "fk59bea98af381abdb" on table "detail_kegiatan"
```

## Keputusan integritas data

- Hanya FK dari `detail_kegiatan` yang diubah menjadi `CASCADE`.
- FK milik `cicilan_pembayaran`, `bukti_pembayaran`, dan tabel detail gateway
  pembayaran tidak diubah. Baris rencana bulanan yang sudah dipakai transaksi
  tetap dilindungi oleh relasi-relasi tersebut.
- `SET NULL` tidak dipakai karena `DetailKegiatan.getKodeUnik()` membentuk
  identitas tagihan bulanan dari ID `PengaturanPembayaranBulanan`. Melepas FK
  akan mengubah identitasnya menjadi tagihan item biasa dan berpotensi membuat
  tagihan lama tetap aktif atau bertabrakan dengan kunci unik lain.
- Migrasi mencari constraint berdasarkan tabel dan kolom. Karena itu migrasi
  tetap bekerja walaupun nama FK hasil generate Hibernate berbeda antar basis
  data.

## Berkas

- Migrasi: `webapp/sql/migrasi_fk_detail_kegiatan_pembayaran_bulanan_cascade_20260903.sql`
- Rollback: `webapp/sql/rollback_fk_detail_kegiatan_pembayaran_bulanan_cascade_20260903.sql`

## Cara menjalankan

Jalankan dengan akun database yang mempunyai hak `ALTER TABLE`:

```bash
psql -X -v ON_ERROR_STOP=1 -d NAMA_DATABASE \
  -f webapp/sql/migrasi_fk_detail_kegiatan_pembayaran_bulanan_cascade_20260903.sql
```

Migrasi memakai transaksi. Bila satu langkah gagal, perubahan FK tidak
diterapkan sebagian.

## Verifikasi

Query verifikasi tersedia di bagian akhir berkas migrasi. Hasil yang benar:

- tepat satu FK pada kolom `pengaturan_pembayaran_bulanan`;
- `delete_action` bernilai `CASCADE`;
- definisi menunjuk `public.pengaturan_pembayaran_bulanan(id)`.

Sesudah itu ulangi simpan/perubahan rencana angsuran yang sebelumnya gagal.
Jika masih gagal, baca nama tabel dan constraint terbaru pada exception; itu
berarti ada relasi transaksi lain yang sengaja tetap melindungi data pembayaran,
bukan migrasi ini gagal.

## Rollback

```bash
psql -X -v ON_ERROR_STOP=1 -d NAMA_DATABASE \
  -f webapp/sql/rollback_fk_detail_kegiatan_pembayaran_bulanan_cascade_20260903.sql
```

Rollback hanya mengembalikan aturan FK menjadi `NO ACTION`. Ia tidak
mengembalikan data anak yang telah terhapus oleh operasi delete saat `CASCADE`
aktif; pemulihan data harus menggunakan backup atau tabel audit.
