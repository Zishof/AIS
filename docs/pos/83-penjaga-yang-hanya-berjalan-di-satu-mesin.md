# 83 — Penjaga yang hanya berjalan di satu mesin

Tanggal: 2026-09-02

Dok. 82 ditutup dengan satu pertanyaan:

> Kalau repositori ini di-checkout besok di mesin lain, apakah angka ini masih
> dapat dihasilkan ulang?

Pertanyaan itu lalu diterapkan pada alat-alat di `docs/pos/alat/` sendiri.
Jawabannya **tidak**.

## 1. Temuan: tujuh dari delapan alat menanam jalur mesin ini

Setiap alat membuka berkas lewat konstanta seperti

```python
JSP = r'C:\opt\AIS\ais\src\main\webapp\WEB-INF\baru\modul\kantin\...'
```

Di checkout mana pun selain milik mesin ini, alat-alat itu berhenti di baris
pertama. Alat yang seluruh gunanya adalah menjaga "diklaim tetapi tidak
tersambung" ternyata **hanya tersambung ke satu mesin** — bentuk yang sama untuk
ketiga kalinya, kali ini pada alatnya sendiri, bukan pada kode yang dijaganya.

## 2. Jalurnya diturunkan, tidak ditulis

`alat/akar_repo.py` menampung penentuan letak di SATU tempat:

* Akar AIS diturunkan dari letak berkas itu sendiri (`__file__` naik empat
  tingkat), jadi selalu benar di checkout mana pun.
* Akar POS (Flutter) tidak dapat diturunkan — repositori terpisah — jadi dicari
  berurutan: `POS_REPO`, lalu beberapa letak lazim, lalu **menyerah dengan pesan
  yang menyebutkan cara menentukannya**, bukan diam-diam memindai nol berkas.

Menyalin logika ini ke tiap alat berarti mengulang persis masalah dok. 80 dan 81
(satu perubahan, banyak titik). Harganya satu baris bootstrap `sys.path` per alat
supaya masing-masing tetap dapat dijalankan langsung.

Lima alat diperbarui. Dua alat (`javadoc-yatim.py`, `javadoc-cari-tuan.py`) milik
sesi lain dan sengaja **tidak** disentuh.

## 3. Ujinya menemukan dua cacat yang lebih serius dari cacat aslinya

Jalur absolut hanya membuat alat **gagal berisik** di mesin lain. Dua hal
berikut membuatnya **berbohong dengan tenang** — jauh lebih buruk, dan keduanya
baru terlihat karena ujinya benar-benar dijalankan di jalur lain, bukan dibaca.

### 3.1 Pohon tidak lengkap → tuduhan palsu, tanpa satu pun tanda

Checkout uji yang pertama hanya berisi dua berkas Java. `field-tanpa-pembaca.py`
melaporkan:

```
== Yatim BARU (belum ada di daftar utang) ==
   - memerlukanPersetujuanLimit
```

Field itu **tidak** yatim; ia dibaca `PosApi.java`, yang kebetulan tidak ikut
tersalin. Korpusnya 7,4 juta karakter — "besar", tetapi tidak lengkap. Alat itu
menuduh dengan penuh percaya diri.

Tuduhan palsu adalah cara tercepat membuat sebuah penjaga berhenti dipercaya,
dan penjaga yang tidak dipercaya sama saja dengan tidak ada.

`akar_repo.pastikan_lengkap()` karena itu memeriksa kelengkapan dengan
**menyebut berkas yang memang harus ada** — bukan mengukur besar korpus, yang
sudah terbukti bukan pembeda. Ia juga mencetak akar yang dipakainya, sehingga
`POS_REPO` yang salah ketik langsung terlihat.

### 3.2 Berkas gagal dibaca → korpus menyusut diam-diam

Di jalur yang dalam, tiga JSP bernama panjang melewati batas 260 karakter
Windows dan alatnya berhenti dengan `FileNotFoundError` mentah.

Godaannya adalah membungkus pembacaan dengan `try/except` lalu melanjutkan. Itu
justru cacat yang dijaga alat ini: korpus pembaca menyusut tanpa suara, dan
field yang pembacanya ada di berkas yang gagal dibaca akan dituduh yatim —
persis §3.1, tetapi lebih sulit dilihat.

`akar_repo.baca()` mencatat tiap kegagalan; `pastikan_terbaca()` menolak
melaporkan angka apa pun selama masih ada yang belum terbaca, dan menyebutkan
berkasnya, sebabnya, serta jalan keluarnya.

## 4. Bukti, bukan klaim

Penjaga yang belum pernah terbukti menyala tidak layak dipercaya. Keduanya
dibuktikan dengan kontrol negatif yang nyata:

| Keadaan | Yang diharapkan | Hasil |
|---|---|---|
| Pohon asli, cwd tak berkaitan | keempat alat lulus | rc=0 semua |
| Pohon tanpa `PosApi.java` | menolak, menyebut berkasnya | `Tidak ketemu: PosApi.java` |
| Jalur > 260 karakter | melaporkan gagal-baca | 3 berkas disebut + jalan keluar |
| Checkout lengkap di jalur lain | angka yang **sama** | rc=0 semua, angka identik |

Baris terakhir itulah jawaban atas pertanyaan dok. 82:

```
kontrak-payload-pesanan    SELURUH KONTRAK TERPENUHI (21 periksaan)
aturan-stok-tiga-nilai     BENTUK ATURAN UTUH (9 periksaan)
field-tanpa-pembaca        BERSIH
payload-tanpa-pembaca      BERSIH
```

Dijalankan dari akar yang sama sekali berbeda, dengan direktori kerja yang tidak
berkaitan dengan keduanya.

## 5. Yang dipelajari

**Uji yang dijalankan menemukan hal yang tidak akan ditemukan dengan dibaca.**
Cacat yang dicari adalah jalur absolut. Yang ditemukan adalah dua cara alat itu
menghasilkan angka salah tanpa memberi tanda. Membaca kode tidak akan
memperlihatkan keduanya — keduanya baru muncul setelah alatnya dipaksa berjalan
di tempat yang tidak dirancang untuknya.

**Alat pemeriksa perlu diperiksa dengan standar yang sama seperti kode yang
diperiksanya.** Ini kali ketiga hal itu terbukti: dok. 77 (JavaDoc menjanjikan
gerbang yang tak pernah dibaca), dok. 82 (gerbang kompilasi melapor BERSIH tanpa
mengompilasi apa pun), dan sekarang §3.1 dan §3.2.

**Diam adalah kegagalan yang paling mahal.** Setiap perbaikan di sini bentuknya
sama: mengubah "salah tanpa suara" menjadi "berhenti sambil menjelaskan".

## 6. Yang masih terbuka

* `src/test` (18 UAT Java) tetap di luar SVN — dok. 82 §4, keputusan pengguna.
* Tiga harness bersandar-DB belum pernah berjalan (kredensial UAT ditolak).
* `hanya_perubahan` masih utang sadar di `payload-tanpa-pembaca.py`; membayarnya
  menuntut keputusan "apa artinya tidak berubah" (dok. 79).
