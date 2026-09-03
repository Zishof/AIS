# Akun Master Aset kini menang atas Kelompok — dan getter yang harus dipecah dulu

Doc 103 menahan pekerjaan ini sambil menunggu keputusan pemilik. Keputusannya sudah turun:

> Jika Akun Pembelian (milik aset) ada, ambil itu; kalau tidak, akun pada Master Aset; terakhir
> baru dari Kelompok.

Urutan lama persis kebalikannya. Dokumen ini mencatat pelaksanaannya, satu cacat yang ditemukan
di jalan, dan satu risiko data yang harus diperiksa pemilik sebelum ini dipakai.

---

## 1. Urutan baru

Tiga sumber memang ada di `MasterAsset`, jadi keputusannya jatuh persis ke ketiganya:

1. `akun_*_str` — pemetaan milik aset sendiri, bila sudah memuat akun
2. `akun_*` — kolom FK **warisan** pada aset, bila terisi
3. Kelompok Aset

Ketiganya dipusatkan di satu penolong `pilihAkun(...)`, dipakai oleh `akunTransaksiEfektif()`,
`akunPenyusutanEfektif()`, dan `akunBiayaPenyusutanEfektif()` — supaya ketiga bidang tidak bisa
menyimpang satu sama lain.

## 2. Kenapa getternya harus dipecah lebih dulu

Membalik urutan di dalam getter yang sudah ada akan **merusak data**, bukan sekadar mengubah
perilaku.

`@Id` pada entitas ini dipasang di **getter**, jadi Hibernate memakai akses properti dan
memanggil `getAkunTransaksi()` juga ketika memeriksa perubahan. Getter lama mengembalikan nilai
milik Kelompok Aset — dan nilai itu ikut **tertulis permanen** ke kolom aset pada flush
berikutnya.

Selama kelompok yang menang, akibatnya tersamar: nilai tersalin sama dengan nilai yang dipakai.
Setelah urutannya dibalik, salinan beku itu justru **menang**, dan asetnya berhenti mengikuti
kelompoknya tanpa ada yang menyentuhnya.

Karena itu:

- tiga getter ber-`@Column` sekarang mengembalikan **nilai tersimpan apa adanya** — tanpa
  kelompok, tanpa warisan, dan tanpa menugaskan ulang fieldnya;
- pemilihan akun efektif pindah ke tiga method yang sengaja **bukan** getter JavaBean, sehingga
  Hibernate tidak memetakannya.

## 3. Dua tempat sudah menulis aturan ini — getternya yang mengalahkannya

`PostingHppKantinAction:328` dan `AkunKantinUtil:111` sudah begini sejak awal:

```java
if (ma.getAkunTransaksi() != null && !ma.getAkunTransaksi().trim().isEmpty()) { ...aset... }
if (akunPersediaan == null && kelompok != null) { ...kelompok... }
```

Aset dulu, kelompok cadangan — persis aturan yang baru diputuskan. Tetapi karena getternya
mengembalikan nilai kelompok setiap kali kelompoknya terisi, cabang pertama **selalu** berisi
nilai kelompok dan cabang cadangannya menjadi kode mati. Memurnikan getternya membuat kedua
tempat ini bekerja sesuai yang sudah lama tertulis, tanpa satu baris pun berubah di sana.

Tiga berkas posting aset tidak punya cadangan sama sekali dan bergantung penuh pada getter, jadi
**22 pemanggilan** diarahkan ke penyelesai baru: `PostingPengadaanAction` (8),
`PostingPenyusutanAssetAction` (8), `PostingSaldoAwalMasterAssetDetailAction` (6). Diperiksa
lebih dulu bahwa ketiga berkas itu tidak memuat satu pun penerima `KelompokAsset`.

`MasterAssetAction` tidak tersentuh: ketiga pemanggilannya ternyata atas `kelompokAsset`.

## 4. Cacat salin-tempel yang ikut hilang

Getter lama `getAkunPenyusutan()` memeriksa `getAkunBiayaPenyusutanA()` pada penjaganya tetapi
memakai `getAkunPenyusutanA()` di isinya. Akibatnya, tergantung data: akun akumulasi penyusutan
warisan terabaikan diam-diam, atau `NullPointerException` yang tertelan `catch` sehingga bidang
itu tetap kosong. Sekarang tiap bidang memakai akun warisannya sendiri.

Pemeriksaan "sudah terisi" juga tidak lagi memakai `contains("key")` — penanda heuristik yang
menganggap array berisi akun tanpa `key` sebagai kosong — melainkan menguraikan JSON-nya dan
mencari entri berakun.

## 5. Yang HARUS diperiksa pemilik sebelum dipakai

Tulis-balik pada bagian 2 sudah berjalan selama ini. Artinya sebagian baris `master_asset`
kemungkinan **sudah membawa salinan** nilai kelompoknya. Di bawah urutan baru, salinan itu
menang, dan aset-aset tersebut akan membeku di akun lamanya ketika kelompoknya diubah.

Kueri deteksinya:

```sql
SELECT count(*) FROM asset.master_asset m
JOIN asset.kelompok_asset k ON k.id = m.kelompok_asset
WHERE coalesce(m.akun_transaksi_str,'[]') <> '[]'
  AND m.akun_transaksi_str = k.akun_transaksi_str;
```

Baris yang nilainya **identik byte** dengan kelompoknya hampir pasti salinan mesin, bukan
ketikan orang. Bila jumlahnya besar, baris-baris itu perlu dikosongkan bersamaan dengan
perubahan ini. Kolom `akun_penyusutan_str` dan `akun_biaya_penyusutan_str` perlu diperiksa
dengan cara yang sama.

## 6. Yang diverifikasi, dan yang tidak

**Diverifikasi:** kompilasi kode keluar 0, nol galat, 19.299 kelas; keenam berkas yang
terpengaruh ikut dikompilasi termasuk dua pemakai jalur kantin; isi HEAD dicek ulang (tiga
penyelesai ada, getter tidak lagi mengambil dari kelompok, 22 pemanggilan beralih).

**Tidak diverifikasi:** tidak ada pengujian terhadap basis data atau aplikasi berjalan. Perilaku
tulis-balik Hibernate disimpulkan dari letak `@Id` dan strategi akses properti, bukan diamati.
Angka posting sesudah perubahan belum pernah dibandingkan dengan sebelumnya pada data nyata —
dan itulah yang paling layak diuji lebih dulu di lingkungan uji.
