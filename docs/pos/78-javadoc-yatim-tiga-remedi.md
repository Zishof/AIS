# JavaDoc yatim: ternyata ada remedi ketiga

Batch lanjutan sesudah doc 77. Tiga belas blok JavaDoc yatim diselesaikan.

Doc 75 menyebut dua remedi — GABUNG dan PINDAH — dan menolak menyapunya otomatis karena
keduanya tampak sama. Mengerjakannya satu per satu memunculkan remedi **ketiga** yang tidak
terlihat dari statistik: **HAPUS**, untuk blok yang isinya keliru atau semata duplikat.

---

## 1. Dikembalikan ke tuannya (7 blok)

Tujuh blok adalah uraian lengkap yang tergeser, dan pada **setiap** kasus metode yang
seharusnya dijelaskannya berdiri tanpa dokumentasi sama sekali:

| Blok | Tuannya |
|---|---|
| "Menjumlahkan total tagihan, diskon, dan cashback" | `hitungTotalDiskonCashback` |
| "Checkout final POS Kantin" (49 baris, ber-`@param`) | `bayar` |
| "Hasil parse payload split-pembayaran" | `resolveSplitPembayaran` |
| "Menyelesaikan sesi ujian: menghitung nilai akhir" | `onSelesai` |
| "Masukkan reimbursement yang telah DISETUJUI ke DPC" | `simpanReimbursement` |
| "Telusuri kode INDUK pengadaan (PR) dari sebuah PO" | `cariKodePermintaanInduk` |
| "Nama berkas pada folder cadangan datar yang UNIK" | `berkasCadanganUnik` |

Commit: r83064 (tiga pertama), r83077 (empat berikutnya, tersapu berpesan kosong).

## 2. Remedi ketiga: HAPUS — boilerplate yang menyebut induk KELIRU (4 blok)

Empat kelas renderer bersarang di `sirs/` membawa dua blok boilerplate bertumpuk yang
menjelaskan kelas yang sama tetapi menyebut kelas induk **berbeda**:

| Berkas | Blok yatim menyebut | Induk sebenarnya |
|---|---|---|
| `AlatMedisAction` | `LayananAlatMedisAction` | `BiayaAlatMedisPerKelasAction` |
| `AlatMedisTempatTidurAction` | `LayananAlatMedisAction` | `BiayaAlatMedisPerKelasAction` |
| `PembayaranAction` | `ReturDetailAction` | `PembayaranDetailAction` |
| `TindakanAction` | `LayananTindakanAction` | `BiayaTindakanPerKelasAction` |

Kolom "induk sebenarnya" tidak diambil dari blok mana pun — dicari sendiri dengan menelusuri
deklarasi `class` terdekat di atasnya, lalu dicocokkan. Hasilnya sama persis dengan yang
disebut blok KEDUA, yang memang menempel ke deklarasinya.

Jadi blok atas bukan dokumentasi yang tergeser: ia **menyatakan hubungan kelas yang tidak
ada**. Menggabungkannya dengan blok yang benar — remedi yang paling "aman" secara naluri —
justru akan mengukuhkan hubungan palsu itu ke dalam dokumentasi yang terbaca. Yang keliru
dihapus, yang benar tidak disentuh. Commit r83070.

## 3. Remedi ketiga, bentuk kedua: duplikat dan boilerplate kosong (2 blok)

- `CommonUiFactoryHelper` — blok yatim **identik kata demi kata** dengan blok yang menempel
  di bawahnya. Murni duplikat.
- `TampilStudiMahasiswaHelper` — blok yatim boilerplate generik ("Renderer lokal untuk
  layar/komponen ..."), sedangkan yang menempel adalah uraian spesifik tulisan tangan
  tentang baris grid KRS. Yang generik tidak menambah apa pun.

Keduanya dihapus. Commit r83077.

## 4. Tebakan alat memang perlu dibaca ulang

`javadoc-cari-tuan.py` menebak `simpanUangMuka` untuk blok reimbursement di
`DaftarPengajuanTransfer`. Teks bloknya jelas menyebut reimbursement, dan
`simpanReimbursement` memang ada di berkas yang sama serta tak berdokumen.

Satu tebakan meleset dari tiga belas. Itu bukan alasan membuang alatnya — tanpa alat itu
ketiga belas kasus harus dicari dengan membaca berkas satu per satu — melainkan
pengingat bahwa keluarannya hipotesis, persis seperti ditulis doc 75. Yang menangkap
kekeliruan ini bukan alat lain, melainkan membaca kalimat pertama bloknya.

## 5. Sisa

| | doc 75 | sekarang |
|---|---|---|
| pasangan yatim | 175 | **162** |
| berkas terdampak | 91 | **83** |
| dugaan PINDAH | 25 | **20** |

Sisanya sebagian besar berdugaan GABUNG, dan blok-bloknya tidak menyebut nama tuannya
sehingga alat tidak dapat menebak — harus dibaca. Tidak ada jalan pintas yang aman untuk
itu, dan batch ini menunjukkan alasannya: dari tiga belas kasus, tiga remedi berbeda
terpakai, dan empat di antaranya justru menuntut penghapusan.

## 6. Verifikasi

`javac -source 1.7 -target 1.7 -encoding UTF-8` atas seluruh berkas yang disunting:
**EXIT=0**. Penempatan hasil pemindahan dipastikan di HEAD lewat `svn cat -r HEAD`.
Akhir baris CRLF dipertahankan di semua berkas.
