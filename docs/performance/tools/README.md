# Alat perawatan berkas bantuan

Skrip di sini merawat isi `src/main/webapp/WEB-INF/bantuan/` — 2.667 berkas panduan
dan tanya jawab yang dilayani servlet `ais.action.servlet.Bantuan` lewat
`/bantuan?key=<kunci>`.

Semua skrip:

* **idempoten** — aman dijalankan berkali-kali; yang sudah beres akan dilewati;
* **memverifikasi sebelum menulis** — menolak menyimpan bila ada teks yang hilang
  atau tag menjadi tidak berpasangan;
* **butuh Python 3** dan tidak punya dependensi di luar pustaka bawaan;
* **menemukan sendiri letak berkas bantuan** dari posisi skrip ini, jadi berjalan
  sama di Windows maupun di server Linux. Bila salinan kerja berada di tempat
  lain, timpa dengan variabel lingkungan `AIS_WEBINF`.

```bash
python docs/performance/tools/gen_pusat_panduan.py
```

---

## Kapan menjalankan yang mana

### Menambah panduan modul baru

Bila ada berkas `bantuan/<kunci>.html` baru, jalankan **berurutan**:

| Urutan | Skrip | Kegunaan |
|--------|-------|----------|
| 1 | `format_v2.py` | Rombak panduan ke format baru: ringkasan diangkat ke atas, daftar isi, kartu berwarna, tanya jawab jadi akordeon, bahasa baku disederhanakan |
| 2 | `format_qa_v2.py` | Rombak berkas `<kunci>_qa.html` jadi akordeon berpencarian |
| 3 | `ringkas_gaya.py` | Angkat gaya inline berulang menjadi kelas CSS (menghemat ±25% ukuran) |
| 4 | `aria_lencana.py` | Beri `aria-hidden` pada lencana nomor agar pembaca layar tidak membaca "1Tujuan Halaman" |
| 5 | `gen_pusat_panduan.py` | **Bangun ulang `panduan.html`** — indeks yang menaut seluruh panduan |

Urutan penting: `ringkas_gaya.py` hanya bekerja pada berkas yang sudah berformat
baru, dan `gen_pusat_panduan.py` membaca judul dari hasil akhir.

Langkah 1–4 mendukung `--uji` untuk menjalankan tanpa menulis apa pun.

> **Langkah 5 tidak boleh dilewatkan.** `panduan.html` adalah berkas statis;
> panduan baru tidak akan muncul di Pusat Panduan sampai skrip ini dijalankan ulang.

### Menambah layar ZUL baru

`kolom_aksi.py` menyempitkan kolom aksi (menu kebab "...") menjadi 56 px pada
seluruh `WEB-INF/**/*.zul`. Banyak layar mendeklarasikannya `width="10%"`, yang di
layar lebar berarti ±190 px ruang kosong untuk satu tombol kecil.

Aturannya konservatif: hanya kolom berlabel kosong di ujung blok `<columns>`, dan
hanya bila lebarnya persentase / tidak ada / px lebih dari 56. Kolom yang sudah
sempit dibiarkan, dan tidak ada kolom yang disembunyikan — jadi tidak mungkin ada
tombol yang hilang. Mendukung `--uji`.

Angka 56 px sengaja sama dengan `GridKolomHelper.LEBAR_KOLOM_AKSI` agar tidak ada
dua sumber kebenaran.

### Menautkan panduan peran ke halaman terkait

`tautan_terkait.py` menambahkan blok "Panduan terkait" di akhir panduan halaman
yang relevan (mis. `krs.html` → Panduan Pengisian KRS). Petanya ada di dalam
skrip; tambahkan entri di sana bila ada panduan peran baru.

### Memeriksa hasil lewat server yang berjalan

`sapu_bantuan.py` meminta SETIAP kunci bantuan dari servlet yang hidup lalu
memeriksa kode HTTP, keberadaan pembungkus servlet, keseimbangan tag, dan gejala
isi tidak terbaca.

```bash
AIS_BANTUAN_URL='http://localhost:8080/ais/bantuan?key=' \
  python docs/performance/tools/sapu_bantuan.py
```

Butuh aplikasi yang sedang berjalan. Bawaannya menunjuk `localhost:8080/ais`.

---

## `gen_bantuan.py` — perlakuan khusus

Skrip ini **menulis ulang tujuh panduan peran** (`panduan_petugas_perpustakaan.html`
dan seterusnya) dari isi yang tertanam di dalam skrip itu sendiri.

Artinya: **suntingan langsung pada ketujuh berkas HTML itu akan hilang** bila skrip
dijalankan lagi. Bila panduannya perlu diubah, ubah teks di dalam skrip lalu
jalankan — jangan menyunting berkas hasilnya.

Skrip ini **tidak lagi menulis `panduan.html`**. Indeks itu kini dibangun
`gen_pusat_panduan.py` yang menaut seluruh 1.337 panduan; blok penyusun indeks
lama dipertahankan di dalam berkas hanya sebagai riwayat dan hasilnya tidak dipakai.

---

## Yang sengaja tidak disentuh

| Berkas | Alasan |
|--------|--------|
| `hasil_spmi.html` | Panduan tulisan tangan berstruktur khusus (terbungkus `<div>`). Penjaga keseimbangan tag di `format_v2.py` menolaknya secara otomatis. |
| `_qa_umum.html` | Tanya jawab umum bersama, sudah berbentuk akordeon dan dipakai servlet pada mode `?mode=qa`. |

---

## Catatan penting soal skrip

Penyaring pencarian pada halaman panduan ditulis sebagai atribut `oninput`, **bukan**
tag `<script>`. ZK memasang isi panduan lewat `innerHTML`, dan skrip yang disisipkan
dengan cara itu tidak pernah dijalankan peramban — kotak pencarian akan diam tanpa
pesan kesalahan. Bila menambah interaksi baru pada berkas bantuan, ikuti pola yang
sama.
