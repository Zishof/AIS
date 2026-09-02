# Templat laporan, dan pohon `web/` yang tidak ada di working copy

Batch lanjutan sesudah doc 90.

---

## 1. Kanal kedelapan: subreport

Templat JasperReports memuat subreport lewat ekspresi bernama literal:

```xml
<subreportExpression><![CDATA[$P{SUBREPORT_DIR} + "akunting/jurnal.jasper"]]></subreportExpression>
```

Nama itu baru dicari **ketika laporannya dicetak** — subreport yang hilang menggagalkan
pencetakan di tengah jalan, sesudah pengguna menekan Cetak dan menunggu.

[alat/subreport-hilang.py](alat/subreport-hilang.py) memeriksanya:

```
rujukan subreport : 122
dilewati (dinamis): 10
hilang            : 0
```

Kanalnya sehat.

## 2. Dua alarm palsu yang dihentikan sebelum dilaporkan

**Lima `.jrxml` tanpa `.jasper`.** Terlihat seperti templat yang belum dikompilasi, jadi
laporannya gagal. **Bukan.** `Report.java` memanggil
`JasperCompileManager.compileReportToFile` dan mengompilasi `.jrxml` menjadi `.jasper` saat
dijalankan bila belum ada — lengkap dengan penanganan kompilasi paralel. Berkas `.jasper`
yang belum ada menyembuhkan dirinya sendiri pada pencetakan pertama.

Alatnya karena itu menganggap sebuah rujukan sahih bila `.jasper` **atau** `.jrxml`-nya ada.

**Rujukan bernama `.jasper` saja.** Jalan pertama alat ini melaporkan satu subreport hilang
bernama persis `.jasper`. Ekspresi aslinya:

```
$P{SUBREPORT_DIR} + $P{subreport} + ".jasper"
```

Namanya datang dari parameter saat berjalan; `".jasper"` cuma akhiran. Regexnya menangkap
akhiran itu sebagai nama berkas — cacat alat yang keenam dalam rangkaian batch ini. Kini
ekspresi yang memuat parameter selain `SUBREPORT_DIR` dilewati sebagai dinamis.

> **KOREKSI (doc 92): bagian 3 dan 5 di bawah SALAH.** `^/web` bukan pohon terpisah —
> itu URL repositori dari `src/main/webapp`, direktori yang sedang dibuka. Dan sumber yang
> dikira "ditemukan kembali" adalah berkas LAIN yang kebetulan bernama sama di direktori
> berbeda. Angka yang benar: dua, bukan empat. Lihat
> [92-koreksi-topologi-dan-cocok-nama.md](92-koreksi-topologi-dan-cocok-nama.md).

## 3. Temuan yang hampir salah dilaporkan: "sumber transkrip hilang"

Membandingkan `.jasper` dengan `.jrxml` di dalam `webapp/report/` memberi **13 berkas
`.jasper` tanpa sumber**. Dua di antaranya paling mengkhawatirkan: `Transkrip_Akademik`
(dirujuk 30 berkas) dan `Transkrip_Akademik_subreport0` (15). Transkrip akademik tanpa
sumber berarti tata letaknya tidak dapat diubah siapa pun lagi.

Sebelum melaporkannya, riwayat SVN diperiksa — dan jawabannya membatalkan temuan itu:

```
M /web/report/Transkrip_Akademik.jrxml
```

Sumbernya ada, di `^/web/report/`. **Bukan** `^/src/webapp/report/` tempat working copy ini
bekerja. Repositori punya pohon `web/` sejajar dengan `src/`, dan pohon itu memuat sumber
templat laporan.

Setelah dicek satu per satu ke `^/web`, gambarannya menjadi:

| Keadaan | Jumlah |
|---|---|
| sumbernya ada di `^/web` | 2 (termasuk kedua transkrip) |
| tidak dipakai dan tanpa sumber — berkas mati | 7 |
| **dipakai, tanpa sumber di mana pun** | **4** |

## 4. Empat templat yang benar-benar tanpa sumber

| Templat | Dirujuk |
|---|---|
| `laporan_dosen_pembina_matakuliah.jasper` | 17 berkas |
| `laporan_data_pasien_periode.jasper` | 3 berkas |
| `Daftar_Hadir_guru_Semua_Hari.jasper` | 1 berkas |
| `lembar_monitoring_perkuliahanISO.jasper` | 1 berkas |

Keempatnya bekerja hari ini — `.jasper` adalah bentuk terkompilasi yang siap pakai. Yang
tidak bisa dilakukan: mengubah tata letaknya, dan mengompilasi ulang bila versi
JasperReports naik dan format lamanya tidak lagi terbaca. Memulihkannya menuntut membangun
ulang templatnya dari nol, mengikuti keluaran yang sekarang.

Tidak ada yang bisa diperbaiki dengan menyunting berkas; ini catatan risiko, bukan cacat.

## 5. Topologi yang perlu diketahui sesi berikutnya

Working copy di mesin ini adalah `^/src`, dan `docs/pos` selama ini menganggapnya satu-satunya
pohon. Ternyata tidak: **`^/web` berisi sumber templat laporan yang tidak ada di `^/src`**.

Akibatnya langsung terasa pada pemeriksaan mana pun yang menyimpulkan "berkas X tidak ada":
kesimpulan itu hanya berlaku untuk pohon yang diperiksa. Doc 88 sudah memakai kebiasaan
memeriksa `java/` **dan** `src/` sebelum menyatakan sebuah kelas hilang; kebiasaan yang sama
sekarang berlaku untuk berkas laporan, dengan `^/web` sebagai pohon ketiga.
