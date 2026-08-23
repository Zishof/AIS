# 15 — Kunci sesi dan login luring (Lapis 1)

Kasir melapor: layar Masuk menjawab *"Server belum dapat dihubungi"* dan toko tidak
dapat berjualan sama sekali. Dokumen ini mencatat sebabnya, perbaikan yang sudah
dikerjakan, dan batas yang sengaja belum dilewati.

---

## 1. Sebabnya: token dibuang klien, padahal server masih menerimanya

| Sisi | Aturan SEBELUM perbaikan |
|---|---|
| Server | token perangkat berlaku **30 hari** — `PosDeviceAuthApi.MASA_BERLAKU_HARI = 30` |
| Klien | token **dihapus** setelah **60 menit** aplikasi tidak dipakai — `PengaturanSesiLokal.defaultTimeoutMenit`, dieksekusi di gerbang awal `main.dart` / `bootstrap.dart` |

Komentar di `PengaturanSesiLokal` mengaku alasannya: *"Server belum memberi kontrak
pasti masa berlaku token"*. Kontraknya sebenarnya ada, 30 hari. Akibat aturan klien
itu, toko yang membuka aplikasi keesokan pagi **wajib** login daring — dan bila
server belum hidup, tidak ada jalan lain sama sekali, walaupun katalog, antrean
transaksi, dan seluruh jalur luring lain sudah siap di perangkat.

Perhatikan juga letaknya: pemeriksaan kedaluwarsa hanya berjalan **saat aplikasi
dimulai**, bukan selama aplikasi berjalan. Jadi aturan lama itu **tidak** mengunci
mesin kasir yang menyala seharian; satu-satunya efek nyatanya adalah kegagalan pagi
hari di atas.

---

## 2. Yang dikerjakan

### 2.1 Batas waktu lokal MENGUNCI, tidak mengeluarkan

Gerbang awal tidak lagi memanggil `hapusToken()` saat batas waktu lokal terlampaui.
Yang dinyalakan adalah penanda `_terkunci`, dan layar pertama menjadi
`LayarKunciScreen`. Token perangkat **dipertahankan**.

Token sekarang hanya dibuang oleh tiga hal:

1. pengguna menekan **Keluar Akun**;
2. server menjawab **HTTP 401** (token dicabut/kedaluwarsa/tidak dikenal) — penjaga
   baru di `ApiClient.aksi`;
3. logout dari layar Kasir/Konfigurasi (jalurnya sama, `ApiClient.hapusToken`).

`hapusToken()` kini membuang **satu paket**: token, catatan aktif, dan bukti sandi
luring. Identitas yang tidak lagi dipakai di perangkat ini tidak boleh menyisakan
jalan masuk.

### 2.2 Membuka kunci: coba server dulu, lalu bukti lokal

`LayarKunciScreen` menempuh urutan ini:

| Keadaan | Yang terjadi |
|---|---|
| Server terjangkau, sandi benar | verifikasi sungguhan; token **diperbarui** (masa berlaku kembali penuh) dan bukti sandi lokal disegarkan |
| Server terjangkau, sandi salah / akun ditolak | pesan server ditampilkan apa adanya — penolakan bisnis **tidak pernah** dialihkan ke jalur luring |
| Server tidak terjangkau (`ApiException.offline`), bukti lokal cocok | kunci terbuka dalam mode luring |
| Server tidak terjangkau, bukti lokal tidak cocok | ditolak, dengan penjelasan bahwa sandi baru dari server belum dapat diperiksa |
| Server tidak terjangkau, belum ada bukti lokal | ditolak, dengan penjelasan bahwa buka kunci pertama memang harus daring |

### 2.3 Bukti sandi lokal — `VerifikatorSandiLokal` (`packages/core_auth`)

Paket `core_auth` sebelumnya masih kerangka kosong (`Calculator`); sekarang berisi
verifikator ini.

* **Yang disimpan**: nama pengguna, garam acak 16 bita, jumlah iterasi, dan hasil
  **PBKDF2-HMAC-SHA256** 32 bita (100.000 iterasi, dijalankan di isolate lewat
  `compute` supaya layar tidak membeku).
* **Yang TIDAK disimpan**: kata sandi, dan bentuk apa pun yang dapat dikembalikan
  menjadi kata sandi. Ada uji khusus yang menyapu seluruh kunci preferensi dan gagal
  bila kata sandi ujinya muncul di salah satunya.
* Perbandingan memakai **waktu tetap** supaya lama proses tidak membocorkan berapa
  bita yang sudah cocok.
* Bukti **hanya dibuat sesudah server menerima login** — jadi jalur luring tidak
  pernah lebih longgar daripada keputusan server, dan akun yang belum pernah masuk di
  perangkat itu tidak akan pernah bisa masuk luring.
* PBKDF2 ditulis sendiri di atas `crypto` (HMAC-SHA256) — tanpa dependensi baru;
  `crypto` sudah dipakai `core_update` di repo yang sama.

---

## 3. Risiko yang diambil, dan yang membatasinya

**Pencabutan akses menjadi terlambat.** Bila sebuah akun dinonaktifkan atau kata
sandinya diganti ketika perangkat sedang luring, perangkat itu masih dapat dibuka
dengan sandi lama sampai ia tersambung lagi. Ini konsekuensi yang tidak dapat
dihilangkan oleh mekanisme luring mana pun — hanya dapat dipersempit.

Yang sudah membatasinya sekarang:

* bukti lokal disegarkan setiap kali buka kunci berhasil **daring**, sehingga sandi
  baru langsung menggantikan yang lama pada kesempatan tersambung pertama;
* begitu ada satu permintaan yang dijawab **401**, seluruh sesi perangkat dibuang;
* `VerifikatorSandiLokal.terakhirDaring()` sudah dicatat, siap dipakai membatasi umur
  mode luring (Lapis 3) tanpa mengubah struktur apa pun.

**Perbandingan yang jujur:** token perangkat memang sudah lama tersimpan apa adanya
di `SharedPreferences`. Berkas itu sejak awal harus diperlakukan sebagai rahasia
perangkat; bukti sandi berbasis PBKDF2 tidak menaikkan kelas rahasianya. Yang
bertambah semata-mata lag pencabutan di atas.

---

## 4. Yang SENGAJA belum dikerjakan

| Belum ada | Alasan |
|---|---|
| Kunci saat aplikasi sedang berjalan (idle lock) | Perilaku lama pun hanya memeriksa saat aplikasi dimulai. Menambah kunci di tengah sesi adalah perubahan alur kerja kasir, bukan perbaikan cacat — perlu diputuskan terpisah. |
| Batas umur mode luring (mis. wajib daring tiap 7 hari) | Lapis 3. Datanya (`terakhirDaring`) sudah dicatat, tinggal gerbangnya. |
| Snapshot `konfigurasi` untuk mengisi `Sesi` saat luring | Lapis 2. Tanpa ini, perangkat yang dibuka luring memakai `Sesi` seadanya sampai server terjangkau — nama toko, pajak, cara bayar, dan `aksesMenu` baru terisi setelah `konfigurasi` berhasil. |
| Menyimpan bukti di secure storage OS | Peningkatan yang wajar, tetapi menambah dependensi platform; belum dibutuhkan untuk menutup cacat ini. |

---

## 5. Hasil uji

| Uji | Hasil |
|---|---|
| `packages/core_auth` — 8 uji `VerifikatorSandiLokal` | **LULUS**: sandi benar cocok; sandi salah, sandi kosong, dan pengguna lain ditolak; nama pengguna tidak peka huruf besar/kecil; kata sandi tidak muncul di kunci preferensi mana pun; garam acak membuat dua penyimpanan sandi sama menghasilkan hash berbeda; `hapus()` menutup jalur luring sepenuhnya |
| `apps/ebisnis/test/kunci_sesi_kontrak_test.dart` (baru) | **LULUS**: gerbang awal tidak lagi memanggil `hapusToken` di jalur kedaluwarsa lokal (dijaga pada `main.dart` DAN `bootstrap.dart`), 401 membuang sesi, bukti sandi disimpan sesudah token diterima, penolakan server tidak dialihkan ke jalur luring |
| `apps/ebisnis` — seluruh suite | **LULUS**, 270 uji |
| `flutter analyze` | `core_auth` bersih; `apps/ebisnis` tidak bertambah satu pun temuan pada berkas yang disentuh |

**Yang belum diuji di perangkat sungguhan:** alur ujung-ke-ujung mematikan server lalu
membuka kunci di mesin kasir. Uji di atas mengunci logikanya, bukan pengalamannya.

---

## 6. Berkas yang disentuh

| Berkas | Perubahan |
|---|---|
| `packages/core_auth/lib/src/verifikator_sandi_lokal.dart` | **baru** — PBKDF2 + penyimpanan bukti |
| `packages/core_auth/lib/core_auth.dart`, `pubspec.yaml`, `test/core_auth_test.dart` | isi paket menggantikan kerangka `Calculator`; dependensi `crypto` + `shared_preferences` |
| `apps/ebisnis/lib/screens/layar_kunci_screen.dart` | **baru** — layar kunci |
| `apps/ebisnis/lib/main.dart`, `lib/bootstrap.dart` | kedaluwarsa lokal → `_terkunci`, rute ke layar kunci |
| `apps/ebisnis/lib/api_client.dart` | `hapusToken` membuang bukti luring; 401 membuang sesi |
| `apps/ebisnis/lib/screens/login_screen.dart` | simpan bukti sandi sesudah login diterima server |
| `apps/ebisnis/test/kunci_sesi_kontrak_test.dart` | **baru** — kontrak keputusan di atas |
