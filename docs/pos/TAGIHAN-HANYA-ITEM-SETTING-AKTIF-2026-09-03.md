# Tagihan Hanya Menampilkan Item yang Masih Diatur

## Hasil

Daftar tagihan siswa dan calon siswa sekarang hanya memuat pasangan
`PengaturanBiaya + ItemBiayaSekolah` yang masih tercantum pada Setting Biaya
(`PengaturanBiayaAction`).

Jika suatu item pernah membentuk `NominalBiaya`/`Tagihan`, kemudian item tersebut
dilepas dari Setting Biaya, materialisasi lama tetap disimpan untuk integritas
riwayat tetapi tidak lagi ditampilkan sebagai tagihan aktif dan tidak dapat dipilih
untuk pembayaran.

## Akar masalah

Query lama hanya memeriksa bahwa `Tagihan` aktif dan master
`ItemBiayaSekolah` aktif. Query belum mengorelasikan kembali pasangan paket dan
item terhadap tabel konfigurasi `sekolah.pengaturan_biaya_item_biaya`. Karena
itu tagihan lama tetap lolos setelah relasi item pada Setting Biaya dihapus.

## Perbaikan

`TagihanUtil.batasiPadaItemYangMasihDiatur(...)` menambahkan syarat `EXISTS`
terhadap konfigurasi terkini dengan dua kunci sekaligus:

- `pengaturan_biaya_id` harus sama dengan paket tagihan; dan
- `item_biaya_sekolah_id` harus sama dengan item tagihan.

Korelasi memakai placeholder resmi Hibernate 3 `{alias}`, bukan nama alias SQL
keras, sehingga tetap benar saat susunan join query berubah.

Filter dipasang sebelum sistem menentukan apakah perlu membangkitkan ulang
tagihan. Dengan demikian tagihan lama yang sudah tidak dikonfigurasi tidak dapat
menghalangi pembentukan item lain yang masih aktif.

Jalur yang memakai mesin bersama ini mencakup Pembayaran Online ZK, UI baru,
kasir pembayaran siswa, wizard, API TagihanSiswa/PsbCalonApi, dan laporan yang
memanggil `TagihanUtil` atau `TagihanUtilCalonSiswa`.

## Batas perubahan

- Data `Tagihan`, `NominalBiaya`, dan riwayat pembayaran lama tidak dihapus.
- Riwayat pembayaran yang sudah terjadi tetap dapat diaudit.
- Tidak ada perubahan skema database.
- Menambahkan kembali item yang sama pada paket akan membuat pasangan tersebut
  kembali memenuhi konfigurasi aktif.

## Verifikasi

Kompilasi kedua jalur dengan Java 7 berhasil:

```text
JAVAC_EXIT=0
CLASS_COUNT=19234
```

Setelah koreksi placeholder alias, kedua berkas dikompilasi ulang secara
terisolasi terhadap hasil kompilasi penuh: `JAVAC_EXIT=0`, `CLASS_COUNT=13`.

Skenario UAT:

1. Buka satu Setting Biaya yang semula memiliki item SPP, Dana Asrama, dan Dana
   Kuliah.
2. Lepas item SPP lalu simpan.
3. Buka Pembayaran Siswa/Pembayaran Online untuk siswa yang sebelumnya sudah
   memiliki tagihan SPP tetapi belum membayarnya.
4. Pastikan SPP tidak tampil, sedangkan Dana Asrama dan Dana Kuliah tetap tampil.
5. Pastikan riwayat pembayaran lama tetap tersedia pada bagian riwayat.
6. Ulangi pada calon siswa dan endpoint API tagihan.
