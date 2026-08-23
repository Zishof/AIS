# 05 — Uang Muka "Diambil dari Permintaan Pengadaan (PR)"

## 1. Keadaan sebelumnya

Penanda "Diambil dari PR" pada formulir Uang Muka hanya berupa boolean. Subjudulnya
sendiri mengakuinya: *"Rincian PR-nya dipilih di layar ZK; di sini cukup penandanya."*

Akibatnya dua kolom yang menjadi inti fitur ini di `UangMukaAction` selalu kosong:

| Kolom | Isi seharusnya |
|---|---|
| `permintaanpengadaanmasterassets` | id baris PR terpilih, dipisah koma |
| `angarans` | id anggaran milik PR induknya, dipisah koma |

Nilai pengajuan tidak pernah terisi dari PR, dan baris PR tidak pernah tertaut ke uang
mukanya.

## 2. Pemilih baris PR

Aksi baru **`uang_muka_cari_pr`** menyajikan PR beserta barisnya, dengan penyaring yang
sama seperti pemilih ZK (`AmbilDataPermintaanPengadaanMasterAssetBanyak`):

- PR aktif (`aktif` null atau true)
- belum ditutup (`tutup` null atau false)
- **sudah disetujui** (`disetujui_oleh IS NOT NULL`)
- opsional disaring satuan kerja dan kata kunci kode/keterangan

Tiap baris membawa `bolehPilih`. Baris yang barangnya **sudah diterima penuh**
(`jumlahdatang >= jumlah`) tidak dapat dipilih — aturan yang sama dengan ZK, yang
menampilkannya sebagai label biasa tanpa kotak centang.

Baris yang sudah tertaut ke uang muka **lain** tetap dapat dipilih, sama seperti ZK.
Bedanya, kode uang muka itu ikut dikirim lewat `uangMukaKode` sehingga layar dapat
memperingatkan dengan warna merah bahwa memilihnya akan **memindahkan** tautannya —
sesuatu yang di ZK terjadi diam-diam.

## 3. Perhitungan nilai

Sama dengan `UangMukaAction`:

```
nilai = Σ (jumlah × hargaBeli) atas seluruh baris PR terpilih
```

Di ZK, kolom Nilai terisi otomatis begitu PR dipilih. Layar Desktop/Android menyalinnya;
server memakai total PR bila `nilai` tidak dikirim atau dokumennya baru.

## 4. Aturan formulir

Disamakan dengan ZK:

| Field | Mode PR |
|---|---|
| Satuan Kerja | **wajib** |
| Anggaran | tidak diminta (PR-nya sudah memotong anggaran) |
| Akun | disembunyikan — di ZK baris akun hanya muncul saat `tanpaAnggaran` **dan** bukan dari PR |
| Baris PR | **wajib**, dengan pesan penolakan yang sama persis dengan ZK |

## 5. Kolom workspace dan akun tetap terisi

Meski API tidak mengisinya, kolom `workspace` dan `akun` pada dokumen tetap terisi
mengikuti PR. Penyebabnya `UangMuka.getWorkspace()` adalah getter **terhitung**: bila
dokumen berbasis PR dan `angarans` terisi, ia mengembalikan anggaran milik PR itu, lalu
`getAkun()` menurunkan akunnya dari anggaran tersebut. Karena Hibernate memetakan lewat
properti, keduanya ikut tersimpan.

Pemotongan gandanya dicegah di tempat lain — lihat
[03-penggunaan-anggaran.md](03-penggunaan-anggaran.md) §4.

## 6. Tautan balik

Baris PR terpilih ditautkan ke dokumen (`assetDetail.setUangMuka(uangMuka)`) di dalam
transaksi yang sama dengan penyimpanan dokumennya.

Dua hal **ditambahkan di luar ZK** karena ketiadaannya meninggalkan data menggantung:

1. Baris yang **dibuang** dari formulir saat menyunting ikut dilepas tautannya. Tanpa
   ini, baris yang dihapus dari daftar tetap tercatat sebagai milik dokumen itu selamanya.
2. **Penghapusan dokumen** melepas tautannya lebih dulu — FK `uang_muka` pada baris PR
   tidak ber-`ON DELETE CASCADE`, sehingga tanpa itu penghapusan ditolak basis data.

## 7. Hasil uji

Harness `TesUangMukaDariPr` — **23 lulus, 0 gagal**, sisa data uji 0. Mencakup: hanya PR
disetujui yang tampil, baris terkunci tidak bisa dipilih, penolakan saat penanda menyala
tanpa baris, nilai 2 × 150.000 = 300.000 lalu 550.000 dan 250.000 saat baris berubah,
kedua kolom teks tersimpan, tautan terpasang dan terlepas, daftar membawa id baris untuk
memuat ulang formulir, serta penghapusan yang berhasil.
