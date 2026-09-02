# 93 — "Berapa banyak yang sebenarnya kamu lihat?"

Tanggal: 2026-09-02

Dok. 92 ditutup dengan satu pertanyaan yang ditujukan ke seluruh isi
`docs/pos/alat/`:

> Setiap alat di direktori ini sekarang layak ditanyai satu hal yang sama:
> **berapa banyak yang sebenarnya kamu lihat?**

Batch ini menanyakannya ke semua, satu per satu.

## 1. Hasil auditnya

| Alat | Cakupan | Keadaan |
|---|---|---|
| `kontrak-payload-pesanan.py` | 1 halaman JSP | disengaja — namanya sendiri menyebut halamannya |
| `aturan-stok-tiga-nilai.py` | 1 berkas Java | disengaja — menjaga satu aturan di `KantinHelper` |
| `cek-sintaks-jsp.py` | berkas dari argumen | tak ada cakupan tersembunyi |
| `banding-payload-jsp.py` | dua berkas dari argumen | idem |
| `gerbang-peran-tanpa-katalog.py` | 39 terresolusi / 27 tidak | **sudah mencetak cakupannya** (dok. 90) |
| `audit-sakelar-tanpa-pembaca.py` | 1.167 sakelar, 1 wilayah | sudah mencetak hitungannya |
| `field-tanpa-pembaca.py` | 2 pengirim (gerbang) / 295 (`--luas`) | diperbaiki dok. 92 |
| `payload-tanpa-pembaca.py` | **234 dari 274 berkas Dart** | **diperbaiki di sini** |

Dua alat yang memindai satu berkas saja ternyata baik-baik saja: keduanya
memang *kontrak atas satu berkas*, dan itu tertulis di judulnya. Cakupan sempit
bukan cacat; cakupan sempit yang tidak dinyatakan-lah cacatnya.

## 2. Yang ditemukan: satu aplikasi utuh tak pernah terlihat

`payload-tanpa-pembaca.py` menjaga arah klien→server. Akar korpusnya:

```python
AKAR_KLIEN_DART = os.path.join(AKAR_REPO_POS, 'apps', 'ebisnis', 'lib')
```

Satu aplikasi. Repositorinya berisi **dua**:

* `apps/ebisnis` — dipindai
* `apps/ecanteen` — **tidak**, padahal 12 berkasnya memanggil `aksi(...)`
* `packages/core_*` — sembilan paket bersama, **tidak** dipindai

Yang membuatnya lebih menusuk: dua alat sibling di direktori yang sama —
`field-tanpa-pembaca.py` dan `pintu-darurat-tanpa-kunci.py` — sudah lama
memindai `apps` + `packages` penuh. Hanya berkas ini yang tertinggal, dan tidak
ada satu baris pun yang menyebutkannya.

Sesudah dilebarkan: **234 → 274 berkas**, 238 → 240 kunci. Dua kunci baru itu
**keduanya dibaca server**. Jadi tidak ada cacat yang selama ini bersembunyi —
tetapi selama berbulan-bulan sebuah aplikasi penuh berada di luar penglihatan
penjaganya, dan tidak ada cara untuk mengetahuinya.

## 3. Satu batas yang diperiksa dan ternyata benar

Korpus JSP alat itu hanya `modul/kantin`. Di luar kantin ada 11 JSP yang juga
memanggil `fetchDataAPI`. Diperiksa:

```javascript
const res = await fetch('<%=Common.ROOT%>/Data', {
    body: JSON.stringify({ sql: sql, action: "sql", tanpaLogin: "true" })
});
```

Semuanya `_statistik_*` di modul akademik, memanggil endpoint SQL generik
`/Data`, bukan PosApi. Ketiga kuncinya (`sql`, `action`, `tanpaLogin`) sudah ada
di daftar `DIIZINKAN`. Pengecualiannya benar — sekarang alasannya tercetak
setiap kali alat dijalankan, bukan tersimpan di kepala orang yang pernah
memeriksanya.

## 4. Cakupan kini dicetak, bukan disimpulkan

```
CAKUPAN  berkas Dart dipindai    : 274
         JSP dipindai            : modul/kantin saja -- JSP lain
                                   memanggil /Data action=sql, bukan PosApi
```

Itu perubahan yang paling penting di batch ini. Angka di dalam docstring akan
usang tanpa memberi tanda — dok. 90 adalah buktinya. Angka yang dihitung ulang
setiap kali alat berjalan tidak bisa berbohong tentang lari-nya sendiri.

## 5. Yang dipelajari

**Tiga batch berturut-turut menemukan kelas kesalahan yang sama pada alat yang
berbeda.** Dok. 90: angka cakupan yang salah. Dok. 92: cakupan yang tak pernah
disebutkan. Dok. 93: korpus yang lebih sempit daripada sibling-nya tanpa alasan.
Ketiganya lolos dari perhatian karena alatnya **hijau** — dan hijau terasa
seperti jawaban, padahal ia cuma pertanyaan yang dijawab sebagian.

**Ketidakkonsistenan antar-alat adalah petunjuk yang murah.** Ketiga alat ini
memindai repositori klien yang sama untuk tujuan yang berbeda. Begitu
akar-akarnya dibandingkan berdampingan, yang ganjil langsung terlihat dalam
hitungan detik. Membandingkan alat satu sama lain ternyata lebih cepat daripada
memeriksa tiap alat sendiri-sendiri.

**Pemeriksaan ini sudah selesai.** Kedelapan alat kini punya cakupan yang
dinyatakan atau tercetak. Pertanyaan dok. 92 tidak perlu diulang lagi untuk
alat-alat ini — hanya untuk yang berikutnya dibuat.
