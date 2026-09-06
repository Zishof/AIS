# 120 — UAT cmnmedika: paritas data tuntas, sisa hanya paritas tampilan

**Tanggal:** 2026-09-06
**Sifat:** catatan hasil verifikasi. Kode terkait sudah ter-commit (r85439, r85511, r85814).
**Pendahulu:** [116](116-uat-cmnmedika-impor-dbf-rekonsiliasi.md),
[117](117-uat-banding-tenant-vs-dbf.md), [118](118-solusi-paritas-data-impor-legacy.md),
[119](119-pembentukan-dokumen-riwayat-legacy.md).

---

## Verdikt

**Paritas DATA: tuntas** sejauh yang dapat dibuktikan dari berkas DBF.
**Paritas TAMPILAN: belum** — 48 layar belum diadu dengan `inventory.exe`.

---

## 1. Perbandingan dengan DBF — seluruhnya setara

Dibandingkan **kunci dan nilai tiap medan**, bukan sekadar jumlah baris:

| bagian | DBF | tenant | hasil |
|---|---:|---:|---|
| supplier | 101 | 101 | SETARA (nama, alamat, telp, termin) |
| customer | 333 | 333 | SETARA (+ diskon) |
| salesperson | 3 | 3 | SETARA |
| produk | 626 | 626 | SETARA (+ harga, stok minimum) |
| harga_beli | 2.512 | 2.512 | SETARA |
| harga_jual | 11.685 | 11.685 | SETARA |
| opname | 1.771 | 1.771 | SETARA (stok sistem + fisik) |
| piutang | 108 | 108 | SETARA (nilai + status) |
| hutang | 21 | 21 | SETARA |
| akun | 11 | 11 | SETARA |
| **dokumen pembelian** | 5.578 | 5.578 | **SETARA — baris dan total** |
| **dokumen penjualan** | 9.850 | 9.850 | **SETARA — baris dan total** |

**Kesetiaan mutasi: 626/626 produk (100%), selisih 0,00** pada pengadaan maupun penjualan.

### Pemecahan sisa selisih saldo

| komponen | MASUK | KELUAR | produk |
|---|---:|---:|---:|
| **D−C cacat impor** | **0,00** | **0,00** | **0** |
| C−B penolakan aturan importir | 0,00 | 0,00 | 0 |
| B−A ketidakkonsistenan legacy sendiri | −102,00 | −113,80 | 71 |
| tanpa selisih apa pun | | | **449** |

Hanya D−C yang dapat diperbaiki lewat kode, dan nilainya nol. B−A adalah `STOK.DBF` yang tidak
sama dengan berkas transaksinya sendiri — batas paritas yang tak dapat ditembus tanpa sumber data
lain.

---

## 2. Uji kesetaraan: LULUS penuh

```
TOTAL  LULUS=207  GAGAL=0  galat-SQL=0  tanpa-verdikt=0  berkas=29
```

**`tanpa-verdikt=0` yang membuat angka ini bermakna.** Berkas yang tidak menyatakan verdikt juga
tidak bisa GAGAL; tanpa kolom itu, "GAGAL=0" tidak membuktikan apa pun. Perubahan pada importir
(kunci idempotensi, 4 jenis impor baru, pembentukan dokumen, status tagihan, penautan piutang)
tidak merusak satu pun penjaga lama.

---

## 3. Lapisan API: seluruh aksi cocok

Diverifikasi pada instans yang **dibangun ulang dari nol**, bukan yang ditambal manual:

| aksi | DBF | API | |
|---|---:|---:|---|
| `si_supplier_list` | 101 | 101 | COCOK |
| `si_customer_list` | 333 | 333 | COCOK |
| `si_sales_list` | 3 | 3 | COCOK |
| `si_receivable_list` | 108 | 108 | COCOK |
| `si_payable_list` | 21 | 21 | COCOK |
| `si_inventory_balance` (saldo layar) | 626 produk | 626 | **100%, selisih 0,00** |

Startup bersih: 0 kegagalan otentikasi, 0 `AnnotationException`, 0 schema hilang.

### Mengapa lapisan ini harus diuji terpisah

Perbandingan tingkat basis data menyatakan piutang SETARA (108 vs 108, nilai cocok) — **dan
layarnya tetap kosong.** Sebabnya `WHERE d.status = 'AKTIF'`, sementara impor menulis
`LUNAS`/`BELUM`. Cacat semacam ini tidak terlihat dari isi tabel; hanya terlihat dengan
menjalankan aksi yang benar-benar dipakai layar.

---

## 4. Cacat pada alat ukur — pola yang berulang

Beberapa "selisih" terbesar sepanjang verifikasi ini ternyata **cacat pada pembandingnya**, bukan
pada datanya. Semuanya menghasilkan angka yang terlihat meyakinkan:

| gejala | sebab sebenarnya |
|---|---|
| 79 dari 100 produk berselisih saldo | `akhir` adalah saldo BERJALAN; pembanding hanya menjumlahkan mutasi periode |
| "seluruh produk" padahal 100 dari 626 | `page_size` dibatasi server di 100 |
| `si_receivable_list` = 50 | mengirim `limit` ke aksi yang menuntut `page_size` — yang terukur ukuran halaman |
| 129 tagihan berselisih `status` | pembanding masih mengharapkan `LUNAS`/`BELUM` sesudah importir diperbaiki |
| saldo 802.296 vs 7.497 unit | `STOK.DBF` menghitung SEJAK OPNAME, bukan sepanjang masa |

Pelajarannya: setiap kali pembanding melaporkan selisih besar, kemungkinan pertama yang harus
diperiksa adalah pembandingnya sendiri.

---

## 5. Insiden sumber daya, dan sebabnya

Uji kesetaraan gagal dua kali dengan gejala membingungkan: 19 dari 29 berkas "tanpa verdikt",
lalu JVM `malloc failed`. Dua dugaan awal — salinan bayangan (VSS) dan build sesi lain — **keduanya
keliru**; keduanya sempat saya sebut sebelum diperiksa.

Sebab sebenarnya, terukur:

```
pagefile.sys  28,7 GB dialokasikan, hanya 1,3 GB terpakai
disk bebas    0,24 GB   (dari 23 GB beberapa jam sebelumnya)
```

Berkas paging membesar saat memori tertekan dan tidak menyusut sendiri. Sesudah reboot:
`pagefile.sys` 4,0 GB, disk bebas **27,93 GB**, RAM bebas 50,3 GB — dan uji langsung LULUS penuh.

**Sumbangan sendiri:** log Tomcat membengkak **1,28 GB dalam satu berkas**, dan scratchpad sesi
menumpuk **1,39 GB / 355 ribu berkas** dari klaster sekali-pakai yang tidak pernah dibersihkan.
Skrip pembangun instans kini membatasi lognya.

### Nyaris mematikan klaster produksi

Saat membersihkan klaster sekali-pakai, penyaring proses memakai pola **negatif** ("bukan
uat-inventory, bukan PostgreSQL\16\data") dan meloloskan PID 6864 — PostgreSQL di port 5432,
klaster nyata mesin ini. Ia selamat **hanya karena** prosesnya milik layanan sistem sehingga
`Stop-Process` ditolak, dan `-ErrorAction SilentlyContinue` menelan galatnya.

Pelajarannya: pilih proses secara **positif** (yang memang dibuat sendiri, dengan penanda yang
ditanam sendiri), jangan menyaring secara negatif — pola negatif memperlakukan segala yang tak
dikenali sebagai milik sendiri.

---

## 6. Membangun ulang instans UAT

`bangun-instans-uat.ps1` (di `docs/tenant-inventory-sales/`) merekam empat penyesuaian wajibnya
berikut alasannya, agar tidak bergantung pada ingatan:

1. **port 18xxx** — hidup berdampingan dengan instans 8080.
2. **cfg Hibernate → 55600** — paling berbahaya bila terlewat: `HibernateUtil` mengembalikan
   factory ZKPlus **sebelum** `DbCredentialOverride` sempat jalan, sehingga `hibernate.cfg.xml`
   dipakai apa adanya — dan isinya menunjuk `127.0.0.1:5432`, klaster nyata.
3. **`META-INF/context.xml` → 55600** — datasource JNDI tidak dijangkau `DbCredentialOverride`.
4. **`autoDeploy="false"`** — tiap penyuntingan berkas memicu deploy ulang 240 detik; selama itu
   konektornya turun dan semua permintaan dijawab 404.

Plus tambalan lingkungan: mendaftarkan `GrupItemBiayaSekolah` (entitas baru sesi lain yang belum
masuk `hibernate.cfg.xml`; tanpa itu 102 `AnnotationException`).

Dua jebakan ikut terekam: `-replace` PowerShell tidak peka huruf sehingga merusak kapitalisasi
`redirectPort` (atribut XML peka huruf), dan kode keluar robocopy `1`/`2`/`3` adalah **sukses** —
baru `>= 8` yang galat.

---

## 7. Yang tersisa

1. **48 layar diadu dengan `inventory.exe`** — paritas tampilan. Otomasi tangkapan layar tidak
   mungkin di sesi ini (doc 110); jalurnya uji integrasi Flutter atau manual (doc 111).
2. Keputusan yang belum diambil: `HARGAASLI`/`DISCOUNT`/`DISCOUNT2` pada BELI.DBF dan
   `produk.stok_legacy` — medan legacy tanpa rumah pada model tenant.
3. Sesudah UAT 100%: manual pengguna + diagram, lalu varian `sales-inventory` →
   `https://ebisnis.id/ebisnis`.

## Alat verifikasi (`C:\opt\uat-inventory`, di luar SVN)

| berkas | menjawab |
|---|---|
| `banding-dbf.py` | master medan demi medan |
| `banding-mutasi.py` | kesetiaan impor vs baris yang diterima |
| `banding-pecah.py` | pemecahan selisih saldo per komponen |
| `banding-dokumen.py` | jumlah dokumen, baris, dan nilai total per faktur |
| `banding-sisa.py` | piutang, hutang, akun, opname, harga |
| `banding-stok-sumber.py` | konsistensi internal legacy (batas paritas) |
| `banding-api.py` | aksi daftar yang menyuplai layar |
| `banding-saldo-api.py` | kolom saldo layar stok vs turunan DBF |
| `analisis-gagal.py` | menjelaskan penolakan dari DBF-nya sendiri |
