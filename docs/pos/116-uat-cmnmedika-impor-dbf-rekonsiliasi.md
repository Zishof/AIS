# 116 — UAT cmnmedika: instans terisolasi, impor DBF, dan rekonsiliasinya

**Tanggal:** 2026-09-06
**Sifat:** catatan pelaksanaan UAT. Tidak ada kode produksi yang diubah.
**Keputusan yang berlaku:** (a) akuntansi se-tenant — lihat [114](114-kolom-pembeda-vs-schema-per-tenant.md).
**Verdikt UAT saat ini: BELUM 100%.** Alasannya di bagian terakhir.

---

## 1. Instans UAT terisolasi

Instalasi Tomcat bersama **tidak disentuh**. Alasannya terukur: dari 4.009 kelas hasil kompilasi
sumber terkini, **2.936 berbeda** dari yang ter-deploy dan hanya **41** milik pekerjaan tenant.
Sisa 2.895 adalah pekerjaan sesi lain (`action/master/helper` 587, `action/master` 551,
`ais/common` 221, `database/model` 159, `action/report` 137, `sekolah`, `surat`). Menyalinnya akan
menerbitkan semua itu.

| | |
|---|---|
| `CATALINA_BASE` | `C:\opt\uat-inventory\tomcat-uat` — seluruh tulisan di sini |
| `CATALINA_HOME` | instalasi bersama, **hanya dibaca** |
| port | 18005 / 18080 / 18443 / 18009 |
| webapp | salinan sendiri, 70.289 berkas (identik dengan aslinya) |
| basis data | `127.0.0.1:55600/ais_uat` |

### Empat penghalang start-up, dan sebabnya

1. **`GrupItemBiayaSekolah` tak terdaftar.** Sesi lain menambah entitas ini di modul sekolah
   berikut rujukannya dari `ItemBiayaSekolah`, tetapi belum mendaftarkannya di `hibernate.cfg.xml`.
   102 `AnnotationException`, SessionFactory gagal. Ditambal satu baris `<mapping>` di salinan UAT.
   *Bila kelas-kelas itu disalin ke webapp bersama, instalasi bersama yang mati.*

2. **Datasource JNDI ke klaster lain.** `META-INF/context.xml` menunjuk
   `jdbc:postgresql://localhost:5433/ais` sebagai `root` dengan sandi `${AIS_DB_PASSWORD}` yang
   tak pernah terisi. `DbCredentialOverride` **tidak menjangkau sumber daya JNDI** — ia hanya
   menambal objek `Configuration` Hibernate.

3. **`DbCredentialOverride` dilewati untuk factory utama.** Ini yang paling berbahaya:

   ```java
   // HibernateUtil.ambilFactoryZkplusAtauBangunSendiri -- MODE LAMA (default)
   Object sf = zkplus.getMethod("getSessionFactory").invoke(null);
   if (sf instanceof SessionFactory) return (SessionFactory) sf;   // kembali di sini
   ...
   DbCredentialOverride.terapkan(cfgUtama, "utama");               // tak pernah tercapai
   ```

   ZKPlus membangun factory-nya langsung dari `hibernate.cfg.xml`, yang isi **aktifnya**
   menunjuk `127.0.0.1:5432/ais` sebagai `root` — **klaster PostgreSQL nyata yang hidup di mesin
   ini**. Yang menyelamatkan hanya sandinya tidak cocok. Bila cocok, UAT akan berjalan di atas
   basis data sungguhan tanpa satu pun tanda.

   Diperbaiki dengan membetulkan cfg-nya (`hibernate.cfg.xml`, `.streaming.`, `.ojs.`, `.radius.`),
   **bukan** dengan `-Dais.zk.factory_interceptor=true` — opsi itu mengubah semantik interceptor,
   dan UAT justru harus membuktikan paritas.

4. **Schema `new_audit` tidak ada.** Envers memakai `default_schema=new_audit`, tetapi
   `UatSchemaExport` hanya membuat 16 schema yang muncul di anotasi `@Table` — schema audit tidak
   termasuk. Akibatnya `initData` gagal **2.241 kali**, sehingga `InitDataHelper:487` tak pernah
   tercapai dan `aktifkan_akun_demo` tetap `false`. Setelah `CREATE SCHEMA new_audit`, Hibernate
   menyusun sendiri **1.569 tabel audit** (`hbm2ddl.auto=update`).

   **Pelajaran alat:** `UatSchemaExport` melaporkan "0 galat, 1.727 tabel" dan laporan itu benar
   untuk yang ia *coba* buat — dan bisu tentang yang tak pernah ia coba. Laporan sukses yang
   hanya mencakup lingkupnya sendiri mudah dibaca sebagai jaminan kelengkapan.

---

## 2. Tiga pengguna

| pengguna | peran | menu | sandi |
|---|---|---|---|
| `muklis` | `OWNER` | 16/16 | `muklis123` |
| `sales` | `SALES_KELILING` | 9/16 | `sales123` |
| `demo` | `ADMIN_TENANT` | 16/16 | `demo123` |

Kunci izin **diturunkan dari `EbisnisMenuKatalog`**, bukan disalin, agar tidak melenceng saat ada
menu baru.

### Akun `demo` digerbangi produk, bukan konfigurasi UAT

```java
// Tbmuser.getAktif()
if (!ConstantValues.aktifkan_akun_demo) {
    if (userId.trim().equalsIgnoreCase("demo")) aktif = false;   // apa pun isi kolomnya
}
```

Sandi yang dipaksakan kode untuk akun ini (`SwWVmlNrTAA=`) mendekripsi menjadi **persis `demo123`**
— jadi `demo`/`demo123` memang akun bawaan produk. Benderanya dibaca dari basis data
(`InitDataHelper:487`), jadi diaktifkan lewat sakelar resminya: satu baris `public.konfigurasi`
(`nama='aktifkan_akun_demo'`, `nilai='aktif'`), bukan tambalan kelas.

Jangkauannya diperiksa lebih dulu: 8 tempat lain yang membaca bendera ini **semuanya bersyarat
`userId == "demo"`** (mengosongkan `jurusan`, `fakultas`, `yayasan`, `sekolah`, `satuanKerja` —
akun demo sengaja tak terikat unit akademik). `muklis` dan `sales` tak tersentuh.

### Sandi: enkripsi TIDAK diubah

Sandi AIS ter-enkripsi DES dua-arah, bukan hash; `checkLogin` **mendekripsi** lalu membandingkan.
Alat semai memakai `DesEncrypter("AIS_UIN")` yang sama dan memverifikasi dengan cara yang sama —
mendekripsi kembali nilai tersimpan. Ketiganya cocok. Tak satu berkas enkripsi disentuh.

---

## 3. Endpoint dan toko

Dua hal yang menghentikan impor, keduanya bukan bug:

**`si_*` hidup di `/Api_eBisnis`, bukan `/PosApi`.** `ApiEBisnis extends PosApi` dan menerima token
Bearer yang sama, tetapi hanya ia yang memanggil `SalesInventoryApiDispatcher`. Lewat `/PosApi`,
seluruh aksi `si_*` dijawab `"Aksi tidak dikenal"` — gagal yang **terlihat seperti aksinya belum
ada**.

**Toko aktif wajib.** Importir menolak sales/produk tanpa toko: barang dan sales selalu melekat
pada satu toko. `ctx.tokoId` tetap null karena resolvernya mengambil toko dari `koperasi.pedagang`
— entitas instalasi **bersama**; mengikat penyewa ke sana melanggar prinsip yang dijaga sepanjang
pemindahan ini. Dipakai jalur yang memang disediakan kode untuk admin:

```java
Long tokoId = ctx.admin && !request.isNull("toko_id") ? request.getLong("toko_id") : ctx.tokoId;
```

`si_actor_context` mempercepat diagnosis ini: ia menjawab langsung `tokoId=None, actorType=ADMIN`,
jauh lebih murah daripada menyalakan ulang Tomcat 280 detik untuk menebak soal cache.

---

## 4. Rekonsiliasi impor — setiap baris dipertanggungjawabkan

346 bongkah, **160.741 baris dibuat**, **8.605 ditolak**. Penolakan itu **tidak** dianggap detail;
ia dihitung ulang dari DBF-nya sendiri (`analisis-gagal.py`).

| jenis | DBF | dibuat | diperbarui | dilewati | gagal | jumlah |
|---|---:|---:|---:|---:|---:|---:|
| supplier | 101 | 101 | | | | 101 ✓ |
| customer | 334 | 333 | 1 | | | 334 ✓ |
| sales | 3 | 3 | | | | 3 ✓ |
| produk | 626 | 626 | | 0 | 0 | 626 ✓ |
| harga_beli | 3.176 | 2.512 | | 8 | 656 | 3.176 ✓ |
| harga_jual | 11.861 | 11.685 | | 36 | 140 | 11.861 ✓ |
| pembelian | 60.269 | 51.980 | | 562 | 7.727 | 60.269 ✓ |
| penjualan | 94.072 | 93.938 | | 51 | 82 | 94.071 +1 |

### Sebab penolakan, dihitung dari DBF

| jenis | gagal | rincian dari berkas aslinya |
|---|---:|---|
| pembelian | 7.727 | qty≤0 **2.923** + produk yatim **4.208** + keduanya **596** = 7.727 ✓ |
| harga_beli | 656 | produk yatim **656**, supplier yatim 0 ✓ |
| harga_jual | 140 | produk yatim **56** + customer yatim **83** + keduanya **1** = 140 ✓ |
| penjualan | 82 | qty≤0 1 + produk yatim 80 + keduanya 2 = 83 (selisih 1, lihat bawah) |

Empat-empatnya cocok tepat. "Produk yatim" berarti `KODEBRG` tidak ada di `STOK.DBF`; "customer
yatim" berarti `KODECUST` tidak ada di `CUSTOMER.DBF`. Ini kualitas data legacy, bukan cacat
importir.

**Selisih 1 baris pada penjualan** sudah dilacak: `JUAL.DBF` memuat satu rekaman **kosong
sepenuhnya** (seluruh medan blank). Ekstraktor melewatinya karena kuncinya kosong. Itu padding,
bukan data.

**`customer` 333 dari 334** karena ada satu kode ganda; aturan "isi bila kosong" importir
menanganinya sebagai `diperbarui`, bukan kehilangan.

---

## 5. Yang membuat UAT BELUM 100%

### 5.1 Transaksi legacy menjadi mutasi stok, bukan dokumen

Ini **rancangan yang disengaja dan terdokumentasi** di `SalesInventoryDbfImportTenant`:

> "Kode supplier/customer/sales dan nomor batch tetap disimpan pada `keterangan`: teksnya tidak
> dapat menjadi relasi, tetapi membuangnya berarti kehilangan satu-satunya petunjuk asal-usul
> baris itu."

Baris BELI/JUAL legacy adalah item baris **tanpa header dokumen**; menjadikannya faktur berarti
mengarang struktur yang tidak ada di sumbernya. Akibatnya pada basis data tenant:

```
mutasi_stok             145.918   (= 51.980 PENGADAAN + 93.938 PENJUALAN, cocok tepat)
pembelian                     0     pembelian_detail          0
faktur_penjualan              0     faktur_penjualan_detail   0
produk_batch                  0
```

Asal-usulnya utuh — `nomor_dokumen` memuat nomor faktur, `keterangan` memuat kode mitra dan batch:

```
620355        Migrasi BELI.DBF; supplier=051; batch=; ED=
2407-000211   Migrasi JUAL.DBF; customer=00011; sales=01; batch=
```

**Konsekuensi paritas yang belum diselesaikan:** layar yang mendaftar **dokumen** pembelian atau
penjualan akan kosong di UI baru, sementara `inventory.exe` menampilkan puluhan ribu baris. Kartu
stok akan cocok; daftar faktur tidak. Ini harus dibandingkan layar demi layar sebelum ada klaim
apa pun.

### 5.2 Batch dan kedaluwarsa belum menjadi relasi

`produk_batch` kosong walau `BELI.DBF` memuat `NOBATCH` dan `TGLEXP`; keduanya tersimpan sebagai
teks di `keterangan`. Untuk distributor farmasi, penelusuran batch adalah fungsi inti — ini perlu
keputusan tersendiri, bukan diasumsikan memadai.

### 5.3 Medan legacy tanpa rumah

Dilaporkan importir: `produk.stok_legacy`. Ditambah keputusan terbuka yang sudah tercatat lebih
dulu: `HARGAASLI` / `DISCOUNT` / `DISCOUNT2` pada `BELI.DBF` — diskon pembelian per baris kehilangan
jejaknya.

### 5.4 Belum ada satu pun layar yang dibandingkan

48 layar pada panduan belum diadu dengan `inventory.exe`. Sampai itu dilakukan, **tidak ada dasar
untuk menyatakan paritas fungsional maupun data.**

---

## Berkas kerja (di luar SVN, `C:\opt\uat-inventory`)

| berkas | guna |
|---|---|
| `ekstrak-dbf.py` | DBF → muatan JSON (346 bongkah) |
| `jalankan-impor.py` | pelari impor lewat `/Api_eBisnis`, dapat diulang |
| `analisis-gagal.py` | menjelaskan 8.605 penolakan dari DBF-nya sendiri |
| `betulkan-cfg.py` | mengarahkan cfg Hibernate salinan UAT ke klaster 55600 |
| `src/.../UatProvision.java` | pendaftar, tenant, peran, pengguna, toko, gudang |
| `src/.../UatSchemaExport.java` | schema bersama dari entitas Hibernate |

## Langkah berikutnya

1. Bandingkan 48 layar dengan `inventory.exe` — fungsional **dan** data.
2. Putuskan sikap atas §5.1 (dokumen vs mutasi) dan §5.2 (batch).
3. Baru setelah itu: manual pengguna, diagram, dan varian `sales-inventory`.
