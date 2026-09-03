# Edit nominal tagihan melalui popup dan snapshot JSON

Tanggal: 2026-09-03  
Status: selesai di kode, menunggu deploy server untuk UAT pengguna  
Revisi SVN terkait: r83913 (model, pemanggil, dan prioritas snapshot), r83925 (popup final dan proteksi mode wizard)

## Hasil

Nominal item biaya yang `nilaiBisaDiubah = true` tidak lagi diedit langsung di sel tabel. Sel hanya menampilkan angka dan ikon edit. Ikon membuka popup yang memuat:

- kode dan nama item biaya;
- jenis kegiatan;
- NIM/no. registrasi dan nama mahasiswa/calon mahasiswa;
- semester dan tahun akademik;
- sumber tagihan reguler atau bulanan;
- uraian serta nominal yang sedang berlaku;
- nominal baru dan catatan/alasan perubahan yang wajib diisi.

Tombol edit hanya dibuat bila seluruh syarat berikut terpenuhi:

1. item biaya mengizinkan perubahan nominal saat pembayaran;
2. login bukan mahasiswa, calon mahasiswa, siswa, atau calon siswa;
3. pengguna mempunyai hak `UPDATE/EDIT` pada menu pembayaran tersebut.

Validasi hak dan flag item dijalankan kembali ketika data disimpan, tidak hanya saat tombol dirender.

## Persistensi

Entity/table `Kegiatan` mendapat kolom teks JSON baru:

```text
nominal_tagihan_kunci_json
```

JSON memakai kunci stabil `detailBiaya:<id>` atau `bulanan:<id>`. Bagian `nilai` menyimpan snapshot final terbaru; bagian `riwayat` menyimpan nominal sebelumnya, nominal baru, alasan, pelaku, waktu epoch, item biaya, dan id sumber tagihan.

Snapshot menjadi sumber nominal dengan prioritas tertinggi. Setelah tersimpan, perubahan nilai master, kalkulasi ulang diskon, modifikasi mahasiswa, atau denda dinamis tidak menimpa/menambahkan nominal tersebut kembali. `DetailKegiatan.biaya` dan `biayaTemporary` tetap ikut diperbarui untuk kompatibilitas jalur lama.

Penyimpanan dilakukan dalam satu transaksi dengan lock pada baris `Kegiatan`, sehingga dua petugas tidak saling menghilangkan isi JSON saat mengubah item berbeda pada kegiatan yang sama.

## Cakupan layar

- Pembayaran mahasiswa lama (`DaftarUlangMahasiswaLamaAction`)
- Pembayaran calon/mahasiswa baru (`DaftarUlangMahasiswaBaruAction`)
- Mode wizard ringkas melalui `ProsesBayarCheckboxRenderer`

Pada mode wizard, ikon edit diberi penanda khusus agar tidak ikut disembunyikan oleh pembersih tombol aksi. Nilai pengurang juga dideduplikasi ketika baris dirender ulang.

## Verifikasi yang sudah dilakukan

- Maven legacy compile: **BUILD SUCCESS**.
- Self-check JSON: snapshot Rp1.500.000 dapat dibaca kembali; kunci `detailBiaya:71`, alasan, dan riwayat audit terbentuk.
- Kedua mirror SVN `src/main/java` dan `src/main/src` identik untuk file yang diubah.
- Pencarian regresi memastikan editor inline `MyDoublebox(jml)`/`MyDoubleboxMin(-jml)` lama sudah tidak ada pada sel nominal.

## UAT setelah deploy

1. Deploy/restart server agar Hibernate membuat kolom baru.
2. Masuk sebagai petugas dengan hak EDIT; pastikan ikon hanya muncul pada item yang boleh diubah.
3. Ubah nominal dan pastikan alasan kosong ditolak.
4. Simpan, muat ulang halaman, lalu jalankan hitung ulang; nominal harus tetap sama.
5. Ubah nilai master item biaya; snapshot pada kegiatan yang sudah diedit tidak boleh berubah.
6. Masuk sebagai mahasiswa/calon atau petugas tanpa EDIT; ikon tidak boleh tampil.
7. Uji item pengurang dan mode wizard; angka tidak boleh terhitung dua kali.

Rollback aplikasi dapat dilakukan dengan mengembalikan revisi kode. Kolom JSON aman dibiarkan karena nullable dan pembaca lama mengabaikannya.
