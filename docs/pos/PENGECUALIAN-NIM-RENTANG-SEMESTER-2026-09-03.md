# Pengecualian NIM berdasarkan rentang semester

Tanggal: 3 September 2026

## Ringkasan

Form **Setting Biaya** kini menyembunyikan daftar pengecualian NIM secara bawaan. Isian baru tampil setelah pengguna mencentang **Terdapat pengecualian NIM**. Bila nilai tersimpan kosong, checkbox tidak terpilih.

Pengecualian berlaku pada seluruh jalur pembentukan tagihan mahasiswa dan calon mahasiswa, termasuk fallback setting umum, tagihan bulanan/angsuran, daftar ulang, serta penyusun tampilan tagihan.

## Format yang didukung

- Format lama, berlaku untuk semua semester: `NIM,NIM,NIM`
- Format rentang, berlaku inklusif dari semester mulai sampai semester selesai: `NIM:SMT_MULAI:SMT_SAMPAI;NIM:SMT_MULAI:SMT_SAMPAI`
- Kedua format boleh dicampur. Pemisah koma, titik koma, spasi, tab, dan baris baru tetap didukung.

Contoh:

```text
20240001,20240002;20241001:3:5;20241002:8:10
```

Pada contoh tersebut:

- `20240001` dan `20240002` dikecualikan pada semua semester.
- `20241001` hanya dikecualikan pada semester 3, 4, dan 5.
- `20241002` hanya dikecualikan pada semester 8, 9, dan 10.

## Validasi

Penyimpanan ditolak bila:

- format rentang tidak berisi tepat tiga bagian;
- semester mulai atau selesai bukan angka;
- semester mulai kurang dari 1; atau
- semester mulai lebih besar dari semester selesai.

Bila checkbox tidak dipilih, nilai pengecualian disimpan kosong. Bila checkbox dipilih tetapi isi tetap kosong, checkbox dikembalikan menjadi tidak terpilih dan nilai tetap kosong.

## Perilaku jalur tagihan

Sistem memakai penanda internal untuk membedakan dua kondisi:

1. tidak ada setting/tagihan yang cocok sehingga sistem boleh mencoba fallback; dan
2. NIM sengaja dikecualikan sehingga sistem tidak boleh membentuk ulang tagihan dari fallback atau jalur bulanan.

Pemeriksaan pengecualian dilakukan sebelum guard mode angsuran agar NIM yang dikecualikan tidak muncul kembali sebagai tagihan bulanan.

## Database dan deployment

Kolom `setting_biaya.pengecualian_mahasiswa` dan kolom auditnya bertipe `text`. Inisialisasi skema aplikasi menambahkan kolom secara aman bila belum tersedia.

Perubahan ini berada di server aplikasi web; POS Desktop tidak perlu dibangun ulang. Server perlu dibangun dan dideploy agar UI, parser, dan seluruh jalur tagihan memakai aturan baru.

## Verifikasi

Self-test `SettingBiayaPengecualianSemesterSelfTest` memeriksa 13 skenario: kompatibilitas format lama, batas rentang inklusif, semester di luar rentang, format campuran, spasi di sekitar titik dua, dan penolakan format tidak valid. Seluruh skenario lulus.

Kompilasi terarah terhadap seluruh file Java yang berubah berhasil. Kompilasi Maven penuh pada working copy SVN masih terhalang dua error lama yang tidak terkait di `ais/common/Common.java` (konversi `Junction` ke `Disjunction`).

## Referensi perubahan

- SVN kode: r83977, r83978, dan r83979.
- GitHub kode: `aeaf48b6` (`feat(billing): scope NIM exclusions by semester`).
