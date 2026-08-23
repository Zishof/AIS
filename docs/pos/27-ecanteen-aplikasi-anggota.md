# 27 — eCanteen: aplikasi anggota kantin

Aplikasi member/loyalitas untuk kantin, kafe, dan outlet — Android + Windows Desktop,
Flutter, di `apps/ecanteen` pada repositori yang sama (`Zishof/zishof-platform`).

Acuan perilakunya **bukan** rancangan baru, melainkan halaman JSP yang sudah berjalan:
`src/main/webapp/WEB-INF/baru/modul/kantin/member/landing_page.jsp`. Seluruh logika
diskon, keranjang, dan checkout diturunkan dari sana, bukan dikira-kira.

## Struktur

| Berkas | Isi |
|---|---|
| `lib/app_variant.dart`, `lib/app_config.dart` | varian & konfigurasi build |
| `lib/services/server_config.dart`, `api_client.dart`, `sesi.dart` | koneksi & sesi |
| `lib/services/keranjang.dart`, `diskon_engine.dart`, `checkout_service.dart` | mesin belanja |
| `lib/models/aturan_diskon.dart`, `keranjang_item.dart` | model |
| `lib/screens/` (10 layar) | login, beranda, keranjang, pesanan, transaksi, topup, dashboard, bayar QR, pindai meja, pengaturan server |
| `lib/widgets/` | app_shell, navigasi, format, panel_galat |
| `installer/ecanteen.iss`, `ecanteen_petra.iss` | Inno Setup per varian |
| `tool/buat_ikon.py`, `tool/build_rilis.ps1` | ikon & pipeline rilis |

API-nya lewat `/Api` (`ApiRouteRegistry`, aksi berawalan `kantin_`), bukan `/PosApi`.

## Varian

`ECANTEEN_VARIANT` — `umum` dan `petra`.

Varian `petra` bernama **"Direktorat Pengembangan Usaha Sosial"**, server bawaan
`kantinpcu.ecampus.id`, context path `petra`.

**Nama paket Android untuk semua varian tetap `com.ecanteen.zishof`** — itu paket yang
sudah terdaftar di Google Play (`android/app/build.gradle`, `namespace`). Varian dibedakan
lewat flavor dan `--dart-define`, **bukan** lewat applicationId; mengubah applicationId
berarti aplikasi yang berbeda di mata Play Store dan pengguna lama kehilangan jalur
pembaruan.

Folder instalasi Desktop dibedakan per varian supaya POS Desktop dan aplikasi anggota
tidak saling menimpa saat dipasang di mesin yang sama.

## Tampilan

Login dan tampilan setelah login dibuat mengikuti POS Desktop (layout dan tema menu
Kasir/POS), termasuk tata letak dua kolom pada login varian Petra beserta footer hak cipta
dan lencana keamanan.

## Uji

`test/diskon_engine_test.dart` — 17 uji pada mesin diskon. Ini bagian yang paling pantas
diuji: aturan diskon menentukan uang yang ditagih, dan salah sedikit langsung terasa
pelanggan.

## Yang sengaja belum dikerjakan

- **Topup native.** Masih memakai jembatan web (`mobile_auth.jsp`, dengan parameter `next`
  yang dibatasi daftar putih: `topup`, `va`, `notifikasi`). Membatasi `next` mencegah
  halaman itu dipakai sebagai pengalih terbuka ke alamat sembarang.
- **Penandatanganan rilis APK.** Atas keputusan pengelola, rilisnya masih varian debug.
  APK debug tidak boleh naik ke Play Store; penandatanganan rilis harus disiapkan lebih
  dulu.

## Rilis

| Tag | Isi |
|---|---|
| `ecanteen-v1.0.0` | eCanteen v1.0.0 — aplikasi member kantin |
| `petra-2026.08.20` | eKantin Petra — POS Desktop + Aplikasi Anggota |

Jebakan pipeline yang sempat terjadi: aplikasi Windows dibangun ulang tetapi **ISCC belum
dijalankan**, sehingga `.exe` installer-nya tidak berubah. Terdeteksi karena hash berkas
dibandingkan sebelum unggah. Bandingkan hash setiap kali, jangan andalkan urutan langkah.
