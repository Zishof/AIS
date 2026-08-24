# 46 — Angka yang tidak dapat diketuk harus mengatakannya

Laporan produksi, referensi **API-MT6LWJLG** (`https://ebisnis.id/ebisnis/Api_eBisnis`,
HTTP 200, `action=draft_jurnal_rincian`):

```
Permintaan : {"action":"draft_jurnal_rincian","nama":"Posting HPP","status":"draft",
              "mulai":"2026-02-22","sampai":"2026-08-25","limit":200}
Respons    : status 91 — "Posting HPP" diposting per periode, bukan per dokumen,
             sehingga tidak memiliki daftar dokumen yang dapat dirinci.
```

**Penolakannya benar.** Itu memang keputusan yang sudah dicatat di
[18](18-mesin-posting-lengkap.md) bagian 4: Posting HPP diposting per periode, bukan per
dokumen, sehingga tidak ada daftar yang jujur bisa ditampilkan.

Yang salah adalah **layar itu menawarkan ketukannya sejak awal.**

---

## 1. Setengah kontrak yang tidak pernah dipasang

`DraftJurnalApiHelper` sudah mengirim dua bendera kemampuan per baris, dengan komentar
yang menyatakan maksudnya:

```java
// Bendera kemampuan: klien hanya menawarkan tombol yang benar-benar ada mesinnya,
// sehingga tidak ada tombol yang ujungnya menolak.
j.put("bisaRincian", DraftJurnalRingkasanUtil.punyaRincian(b.getNama()));
j.put("bisaPosting", modulPosting(b.getNama()) != null);
```

Layarnya menghormati `bisaPosting` (`draft_jurnal_screen.dart`:307) — dan **tidak pernah
membaca `bisaRincian`**. Angkanya digarisbawahi dan dibungkus `InkWell` semata-mata
karena nilainya lebih dari nol:

```dart
decoration: n > 0 ? TextDecoration.underline : null,
...
if (n == 0) return teks;
return InkWell(onTap: () => _bukaRincian(baris, status), ...);
```

Garis bawah pada angka adalah **janji**: "ketuk saya, ada dokumen di baliknya." Untuk
Posting HPP janji itu tidak bisa ditepati, dan pengguna baru mengetahuinya setelah
mengetuk, menunggu jaringan, lalu menerima lembar merah.

Bentuk cacatnya sama persis dengan yang baru diperbaiki di
[45](45-penyaring-dasbor-dan-layani-semua.md): **server mengirim sesuatu, klien tidak
membacanya, dan tidak ada yang mengeluh.** Kali ini arahnya terbalik — di dokumen 45
klien yang mengirim dan server yang tidak membaca.

---

## 2. Bendera saja tidak cukup — alasannya ikut dikirim

Memadamkan garis bawahnya saja akan menghasilkan satu baris yang diam tanpa sebab: dari
kursi pengguna, angka yang tidak bereaksi terbaca sebagai **kerusakan**, bukan sebagai
sifat baris itu. Yang membuat orang berhenti bertanya adalah **sebabnya**.

Karena itu kalimatnya dijadikan satu sumber di
`DraftJurnalRingkasanUtil.alasanTanpaRincian(nama)`:

| Pemakai | Sebelumnya | Sekarang |
|---|---|---|
| Ringkasan dasbor | hanya `bisaRincian: false` | + `alasanTanpaRincian` |
| Penolakan `draft_jurnal_rincian` | kalimat ditulis di tempat | `alasanTanpaRincian(nama)` |
| Dialog di layar | — | kalimat dari server, apa adanya |

`punyaRincian()` kini turunan dari method itu (`alasanTanpaRincian(nama) == null`),
sehingga **tidak mungkin** ada baris yang ditandai "tidak bisa dirinci" tanpa membawa
alasannya — keduanya berasal dari satu percabangan yang sama.

Pelajaran yang sama dengan dokumen 45 bagian 1.2: dua tempat yang wajib berbunyi sama
harus secara harfiah memakai string yang sama. Kalau tidak, salah satunya akan berubah
lebih dulu dan pengguna membaca dua penjelasan berbeda untuk satu hal.

---

## 3. Perilaku layar sesudahnya

| Keadaan | Tampilan angka | Ketukan |
|---|---|---|
| `n == 0` | redup, tanpa garis bawah | tidak ada |
| `bisaRincian` (bawaan) | berwarna, **bergaris bawah** | membuka daftar dokumen |
| `bisaRincian == false` | berwarna, **tanpa** garis bawah | dialog berisi alasannya, **tanpa memanggil server** |

Ketukannya sengaja tidak dimatikan sama sekali. Angka yang mencolok tetap mengundang
disentuh; lebih baik sentuhan itu menjelaskan diri daripada tidak terjadi apa-apa.

### 3.1 Bendera yang absen dianggap "bisa"

```dart
final bisaRincian = baris['bisaRincian'] != false;
```

Sengaja `!= false`, bukan `== true`. Peladen versi lama belum mengirim bendera ini, dan
memadamkan **seluruh** rincian di seluruh dasbor karena benderanya absen jauh lebih
merugikan daripada sesekali menawarkan ketukan yang ditolak. Sisi ketatnya tetap dijaga
server: permintaan yang tetap datang akan ditolak dengan kalimat yang sama.

---

## 4. Berkas yang berubah

| Berkas | Perubahan |
|---|---|
| `ais/action/master/akunting/util/DraftJurnalRingkasanUtil.java` | `alasanTanpaRincian(nama)` sebagai satu sumber; `punyaRincian` jadi turunannya |
| `ais/action/servlet/api/DraftJurnalApiHelper.java` | ringkasan membawa `alasanTanpaRincian`; penolakan memakai kalimat yang sama |
| `apps/ebisnis/lib/screens/draft_jurnal_screen.dart` | `_angkaSel` membaca `bisaRincian`; `_terangkanTanpaRincian` |
| `apps/ebisnis/test/draft_jurnal_kontrak_test.dart` | uji baru mengunci ketiganya |

---

## 5. Hasil uji

### 5.1 `TesRincianTanpaDokumen` — 8 dari 8 lulus (hanya-baca)

Menyapu **35 baris** dasbor pada rentang bawaan:

| # | Yang diperiksa | Hasil |
|---|---|---|
| 1 | Ringkasan terambil | `status=00`, 35 baris |
| 2 | Ada baris bertanda tanpa rincian | 1 — **Posting HPP** |
| 3 | Setiap baris tanpa rincian membawa ALASANNYA | tidak ada yang kosong |
| 4 | Baris yang bisa dirinci tidak membawa alasan | tidak ada yang berlebih |
| 5 | Baris pada laporan API-MT6LWJLG membawa alasannya | ya |
| 6 | Permintaan rinciannya tetap ditolak sopan | `status=91` |
| 7 | **Kalimat ringkasan IDENTIK dengan pesan penolakan** | sama persis |
| 8 | Baris yang punya rincian tidak ikut tertolak | `Fix Aset (Jurnal Saat BAST)` → `status=00`, 4 dokumen |

Periksaan **7** adalah inti bagian 2: kedua kalimat dibandingkan huruf demi huruf, bukan
sekadar "dua-duanya tidak kosong". Periksaan **8** menjaga arah sebaliknya — penjaga
yang terlalu lebar akan memadamkan rincian yang sah, dan itu jauh lebih merugikan
daripada cacat yang diperbaiki di sini.

### 5.2 `draft_jurnal_kontrak_test.dart` — 5 dari 5 lulus

Satu uji baru mengunci bentuk kode layarnya: bendera dibaca, garis bawah hanya untuk
angka yang benar-benar dapat dibuka, ketukan pada baris tanpa rincian tidak memanggil
server, dan bendera yang absen dianggap "bisa" (bukan "tidak bisa").

### 5.3 Yang BELUM diuji

- **Layarnya sendiri.** Yang dikunci adalah bentuk kodenya (pola source-contract yang
  sama dengan uji-uji kontrak lain), bukan render widget-nya. `ApiClient` singleton
  belum injectable untuk widget test.
- **Rentang tanggal dari laporan** (`2026-02-22` s.d. `2026-08-25`) dipakai apa adanya
  pada pemanggilan rincian, tetapi angka Posting HPP di basis UAT tidak sama dengan
  produksi. Yang diuji adalah perilaku penjaganya, bukan angkanya.

---

## 6. Yang perlu diperiksa lain kali

Dua dokumen berturut-turut menemukan cacat berbentuk sama: **satu sisi mengirim
informasi, sisi lain tidak membacanya, dan tidak ada yang gagal dengan berisik.**

Bendera kemampuan sangat rawan pada bentuk ini, karena mengabaikannya tidak pernah
menghasilkan galat kompilasi maupun uji merah — hanya layar yang menjanjikan lebih dari
yang bisa ditepati. Setiap `j.put("bisa...", ...)` di sisi server layak ditelusuri
sampai ke tempat klien membacanya; bila tidak ketemu, itu bukan bendera, itu bahan yang
terbuang.
