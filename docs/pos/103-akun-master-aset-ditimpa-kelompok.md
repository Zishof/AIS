# Akun Master Aset ditimpa Kelompok Aset — dan kenapa layar natifnya ditahan

Sisa terakhir pemanggilan `*.zul` dari POS Desktop/Android adalah dua formulir aset di
`posting_akun_perbaikan.dart`. Menyiapkan penggantinya membuka satu perilaku yang harus
diputuskan pemilik produk lebih dulu.

---

## 1. Bentuk datanya ternyata sederhana

Kolom akun pada aset disimpan sebagai teks bernama `*_str` dengan nilai bawaan `"[]"`, dan
sempat terlihat seperti "bahasa formula". Bukan. Isinya daftar pasangan biasa:

```json
[ { "key": 8412…, "akun": 1234, "satuanKerja": 56 } ]
```

Artinya satu akun boleh berbeda per Satuan Kerja. Editornya di ZK memang grid dua kolom
(Akun, Satuan Kerja) dengan tombol tambah/hapus baris. Itu sepenuhnya bisa dibuat natif.

Bidangnya: `akun_transaksi_str` (Akun Pembelian), `akun_penyusutan_str` (Akumulasi
Penyusutan), `akun_biaya_penyusutan_str` (Akun Biaya), dan khusus Kelompok Aset
`akun_beban_pokok_penjualan_str` (HPP).

## 2. Yang menghentikan layar Master Aset

`MasterAsset.getAkunTransaksi()` — dan sama persis di `getAkunPenyusutan()` serta
`getAkunBiayaPenyusutan()` — dimulai begini:

```java
if (getKelompokAsset() != null && getKelompokAsset().getAkunTransaksi() != null
        && !getKelompokAsset().getAkunTransaksi().equals(Pertangungjawaban.DEFAULT_FORMULA)) {
    akunTransaksi = getKelompokAsset().getAkunTransaksi();
} else {
    akunTransaksi = …nilai milik aset sendiri…;
}
```

**Nilai Kelompok Aset menimpa nilai Master Aset.** Akun milik aset hanya terpakai bila
kelompoknya kosong.

Entri di aplikasi berbunyi "Master Aset / Persediaan — Ubah Akun Pembelian/Persediaan pada
master aset barang". Pada data yang kelompoknya sudah terisi — dan itu keadaan yang normal —
mengubahnya **tidak berpengaruh apa pun**. Membuat layar natif yang meniru perilaku itu berarti
memindahkan kendali yang diam-diam tidak bekerja dari web ke aplikasi, lalu menyebutnya
perbaikan.

Ini bukan bug yang boleh saya perbaiki sendiri: mana yang seharusnya menang, kelompok atau aset,
adalah keputusan akuntansi, bukan keputusan teknis. Tiga jawaban sama masuk akalnya —
kelompok menang (seperti sekarang), aset menang bila diisi, atau nilai aset diperlihatkan
sebagai "warisan dari kelompok" dan medannya dinonaktifkan sampai sengaja dilepas.

## 3. Bahaya kedua: getter yang menulis saat dibaca

Getter itu **menugaskan ulang fieldnya sendiri** (`akunTransaksi = …`) sebagai efek samping
pembacaan, bukan sekadar mengembalikan nilai. Kolomnya dipetakan lewat akses properti, jadi
Hibernate memanggil getter ini juga ketika memeriksa perubahan. Akibat yang mungkin: sekadar
**membaca** satu Master Aset dapat membuat entitasnya tampak berubah, lalu nilai kelompok
tersalin permanen ke baris asetnya sendiri saat flush.

Saya menyebutnya kemungkinan, bukan kepastian — perilakunya bergantung pada kapan snapshot
Hibernate diambil, dan itu tidak bisa saya buktikan tanpa menjalankan aplikasinya. Tetapi
konsekuensinya cukup berat (konfigurasi akuntansi berubah tanpa ada yang menyentuhnya) sehingga
API baca-tulis apa pun untuk aset harus diuji terhadap hal ini lebih dulu, bukan sesudahnya.

## 4. Yang dikerjakan dan yang ditahan

- **Ditahan:** layar natif Master Aset. Menunggu jawaban bagian 2.
- **Jalan terus:** Kelompok Aset. Di sana tidak ada persoalan penimpaan — kelompok justru
  sumber yang berwenang — sehingga layar natifnya bisa dibuat tanpa menebak apa pun.

Sampai keduanya selesai, `posting_akun_perbaikan.dart` masih memanggil
`master_asset.zul` dan `kelompok_asset.zul`. Keduanya `LaunchMode.externalApplication`,
jadi pengguna keluar ke browser sistem.

## 5. Pertanyaan untuk pemilik

> Ketika Kelompok Aset dan Master Aset sama-sama punya Akun Pembelian, mana yang harus dipakai
> saat posting — dan apakah pengguna boleh menimpanya di tingkat aset?

Sebelum itu terjawab, tombol "Master Aset / Persediaan" sebaiknya tetap membuka layar web yang
sudah ada daripada diganti layar natif yang memberi kesan mengubah sesuatu padahal tidak.
