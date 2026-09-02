# Sapuan hak akses: peladen menolak, layar tidak tahu

Pola cacat yang sama ditemukan di sepuluh modul: **peladen menegakkan hak, tetapi
tidak pernah memberitahukannya kepada klien.** Akibatnya tombol tampil untuk semua
orang dan penolakan baru terasa sesudah ditekan.

Pada modul yang **local-first** akibatnya lebih buruk daripada sekadar kejutan:
perintahnya sudah masuk antrean dan layar menjawab *"tersimpan, akan dikirim
otomatis"* — pengguna diberi tahu pekerjaannya aman padahal justru akan ditolak.

Yang dikerjakan di seluruh sapuan ini **bukan gerbang** — gerbang sebenarnya tetap
pemeriksaan di peladen. Yang ditambahkan hanyalah *memberitahukan* hak itu supaya
layar berhenti menawarkan tombol yang sudah pasti gagal.

## Modul yang ditutup

| Modul | Pemeriksaan | Bentuk yang dipakai |
|---|---:|---|
| Pengadaan (6 tahap) | 33 | `hak` per tahap pada balasan daftar, ditempel terpusat di `proses()` |
| Siklus Akuntansi (Saldo Awal, Penyesuaian, Tutup Buku) | 14 | `hak` pada daftar; Tutup Buku pada **pratinjau draft** |
| Jurnal Umum | 6 | empat wewenang terpisah: create/approve/reject/delete |
| Apotik | 6 | `hak` **bersarang per menu** — satu layar, tiga formulir, tiga kunci |
| Grup Produk | 4 | `hak` hanya bila daftarnya sendiri berhasil |
| Kantin (Produk) | 3 | `hak` pada balasan `katalog` |
| Posting Kantin Lanjutan | 3 | `hak` pada **draf**, bukan pada aksi terapkan |
| Draft Jurnal | 3 | `bolehPosting` **per baris** — belasan modul, kunci berbeda-beda |
| Hotel / MitraInap | 24 | ditangkap sekali di `muatDaftarHotel` untuk sembilan layar |
| Inventory & Sales | — | plumbing sudah ada; hanya pemakaiannya yang dilengkapi |

## Keputusan yang berulang, dan alasannya

**Belum dimuat berarti BOLEH, bukan padam.** Setiap penjaga memeriksa dulu apakah
haknya sudah tiba. Memadamkan tombol karena haknya belum sempat dimuat akan
mengunci pengguna yang sebenarnya berhak — lalu menyala sendiri sesaat kemudian,
gejala yang mahal dilacak justru karena sembuh sendiri.

**Hak hanya diperbarui dari emisi SERVER.** `daftarCacheDulu` memancarkan snapshot
cache lebih dulu dan cache tidak membawa hak; menimpanya dengan peta kosong
memadamkan tombol tanpa alasan setiap kali layar dibuka.

**Wewenang tidak digabung.** Memposting bukan turunan dari menyimpan draf;
membalik dokumen yang sudah posting bukan pekerjaan yang sama dengan
menerbitkannya; menerima tagihan mengubah dokumen yang sudah ada, bukan membuat.
Menggabungkannya akan memberi wewenang yang tidak pernah diberikan admin.

**Jalur massal ikut digerbangi.** "Posting Semua Draf" dan "Posting Semua yang
Siap" memakai wewenang yang sama dengan versi satu barisnya. Memadamkan yang satu
saja meninggalkan pintu terbuka; uji kontraknya menghitung kemunculan penjaganya.

**Membaca tetap terbuka.** Di layar Produk, baris tetap dapat diketuk walau
pengguna tidak berhak mengubah — formulir yang sama dipakai melihat rincian.
Ada uji yang khusus menolak perubahan yang mengunci ketukan itu.

## Dua yang ternyata BUKAN celah

- **TransferDpc.** `bolehAjukan` memakai hak `approve` pada kunci modulnya, dan
  kelima layar Keuangan **sudah** menggerbangi tombol "Ajukan ke proses transfer"
  dengan `_boleh('approve')`. Tidak ada yang perlu diubah.
- **ElearningApiUtil.** Dua pemeriksaannya bukan fitur Elearning sama sekali,
  melainkan penjaga generik untuk dua entitas POS pada endpoint web.

## Satu ketidakkonsistenan yang sengaja dibiarkan, tetapi dicatat

Entitas **Penyedia/Supplier** dijaga oleh **kunci menu yang berbeda tergantung
kanal**:

- jalur POS (`penyedia_simpan`, `penyedia_hapus` → `KantinHelper`) memakai kunci
  **`kulakan`** — masuk akal, karena master penyedia disunting dari alur Kulakan;
- endpoint web generik memakai kunci **`penyedia`**.

Akibatnya admin yang mematikan `penyedia` untuk sebuah peran **tidak** menutup
penyuntingan penyedia dari POS, dan sebaliknya. Ini dibiarkan karena keduanya
memang dua antarmuka dengan alur berbeda — tetapi divergensinya tidak terlihat
dari grid `TbmroleAction`, jadi dicatat di sini sebelum ada yang "memperbaikinya"
tanpa tahu alasannya.

## Pelajaran metode: empat kali angka audit meleset

Empat kali dalam satu sapuan, hitungan awal salah karena pencariannya mengandaikan
**bentuk yang diharapkan**, bukan bentuk yang ada:

| Yang dilaporkan | Yang sebenarnya | Sebabnya |
|---|---|---|
| "hanya 1 layar memakai hak" | 15 layar | `_boleh()` punya dua bentuk (1 dan 2 argumen) |
| Inventory & Sales: 35 pemeriksaan perlu kerja peladen | peladen tidak perlu diubah | modul itu memakai `si_actor_context`, bukan `put("hak")` |
| Hotel: 1 pemeriksaan | 24 pemeriksaan | metodenya bernama `boleh()`, bukan `bolehAksi*` |
| Elearning: 2 pemeriksaan modul Elearning | penjaga generik entitas POS | nama berkas menyesatkan |

**Ukur bentuk yang ada, jangan cari bentuk yang diharapkan.** Bila sebuah modul
terbaca "nol gerbang", kemungkinan besar penamaannya berbeda — periksa dulu
sebelum membangun apa pun di atas angka itu.

## Penjaga palsu yang ikut ketahuan

`pengadaan_hak_akses_test.dart` sempat memuat jalur lintas-repo yang rusak menjadi
`C:\opt\AIS<BEL>is\...` — akibat patch yang memakai string Python biasa, tempat
`\a` berarti BEL (0x07), bukan *backslash-a*. Karena uji lintas-repo memakai pola
`if (!f.existsSync()) return;` supaya CI Flutter yang berdiri sendiri tidak gagal,
ujinya **selalu pulang tanpa memeriksa apa pun**: hijau, tetapi hampa.

Perbaikannya dibuktikan dengan **uji negatif** — berkas peladen dirusak sementara,
ujinya jatuh, lalu berkasnya dikembalikan dan diverifikasi identik. Lulusnya uji
saja tidak cukup untuk mempercayai sebuah penjaga.

Bangun jalur Windows di skrip patch dengan `chr(92)`, jangan literal `\a`.
