# Laporan Masalah: Halaman ZK Tampil Seolah Tanpa CSS

**Status:** TAMPILAN PULIH — **sebab pastinya BELUM dikonfirmasi**
**Tanggal laporan:** 20 Agustus 2026 (diperbarui pukul 06:10)
**Aplikasi:** AIS / eCampus (eCampus, eSchool, ePesantren)

> **BACA BAGIAN 0 LEBIH DAHULU.** Gejalanya sudah hilang, tetapi tiga perubahan berangkat
> bersamaan sehingga tidak ada satu pun yang terbukti sebagai penyebab. Dokumen ini tetap
> relevan karena penyebabnya masih terbuka dan bisa kambuh.

---

## 0. Perkembangan Terakhir (WAJIB DIBACA)

### 0.1 Apa yang terjadi

Setelah deploy + restart pada 20-08-2026 sekitar pukul 06:07, halaman `/ecampus`
(`demo.ecampus.id/ecampus/main`) tampil **normal sepenuhnya**: tema breeze aktif, tombol
modul bergaya, kartu dasbor, donut chart, panel pengumuman — semuanya benar.

### 0.2 Mengapa sebabnya belum dapat dipastikan

Tiga perubahan berangkat dalam **satu** siklus deploy, sehingga kontribusi masing-masing
tidak dapat dipisahkan dari satu pengamatan:

1. `web.xml` — `<distributable />` dihapus (r77760)
2. `zk.xml` — dikembalikan ke r77670; `DesktopCounterListener` dilepas
3. restart Tomcat itu sendiri

Siapa pun yang melanjutkan: **jangan tulis "sudah diperbaiki oleh X"** tanpa uji terkendali.

### 0.3 Yang belum dikonfirmasi

Instance yang dulu tampil mentah adalah **`/akfarsam`** (`ecampus.stiksam.ac.id`). Tangkapan
layar yang menunjukkan pemulihan berasal dari **`/ecampus`** (`demo.ecampus.id`) — instance
yang sejak awal memang sehat. Jadi pemulihan `/akfarsam` **belum terbukti**.

### 0.4 Hipotesis terkuat saat ini (belum diuji)

`DesktopCounterListener` (ditambahkan r77749/r77750 pada 04:25) memanggil
`desktop.getSession().setAttribute(...)` pada **setiap** pembuatan desktop ZK. Pada context
yang ditandai `distributable`, penulisan ke sesi memicu Tomcat memeriksa ulang atribut sesi —
termasuk objek `javax.zkoss.zk.ui.Session` yang tidak `Serializable`.

Hipotesis ini menjelaskan hal yang sebelumnya janggal: **tag `<distributable/>` sudah lama ada,
tetapi errornya baru muncul.** Pemicunya bukan tag itu, melainkan listener baru yang mulai
menulis ke sesi.

Cocok dengan seluruh pengamatan:

| Kondisi | Hasil teramati |
|---|---|
| tag ADA + listener ADA | error muncul |
| tag TIDAK ADA + listener ADA | error hilang |
| tag ADA + listener ADA (dipasang ulang) | error muncul lagi |
| tag TIDAK ADA + listener TIDAK ADA | error hilang |

**Kombinasi yang belum pernah diuji: tag ADA + listener TIDAK ADA.** Itu satu-satunya uji
yang memisahkan kedua faktor. Jalankan di lingkungan uji, bukan produksi.

---

## 1. Ringkasan Masalah

Halaman utama aplikasi (`/main` → forward ke `WEB-INF/z/x/y/pages/main/index.zul`) tampil
**mentah tanpa styling**: teks polos, tanpa chrome widget ZK, tanpa tema. Isi halaman
sebenarnya ADA dan terbaca (logo, header, baris modul, tab "Home", daftar pengumuman),
tetapi seluruh tampilannya seperti HTML tanpa CSS.

Yang perlu ditegaskan: **ini bukan halaman kosong dan bukan error 500.** Server tetap
mengirim isi halaman; yang hilang adalah lapisan tampilannya.

---

## 2. Lingkungan

| Komponen | Versi / nilai |
|---|---|
| Framework UI | ZK **5.0.13 PE** (`<!-- ZK 5.0.13 PE 2013100810 Evaluation Only -->`) |
| Tema | breeze (`org.zkoss.theme.preferred=breeze`) |
| Servlet container | Apache Tomcat 9.x, konektor **AJP** di belakang proxy |
| ORM | Hibernate 3.6 + Envers, c3p0 |
| Database | PostgreSQL |
| Security | Spring Security (filter chain) |
| Compiler | `-source 8 -target 8`, gaya kode Java 1.6 (tanpa lambda/stream/diamond) |
| Multi-tenant | beberapa context dalam beberapa instance Tomcat: `/ecampus` (AJP 8009), `/akfarsam` (AJP 6009), `/unikaltar` (AJP 7009) |

Instance yang bergejala: **`/akfarsam`** (`https://ecampus.stiksam.ac.id/akfarsam/main`).
Instance pembanding yang tampilannya normal: **`/ecampus`** (`https://demo.ecampus.id/ecampus/`).

---

## 3. Gejala Persis

Dari tangkapan layar `/akfarsam/main` setelah login sebagai admin:

- Logo, nama institusi, dan baris info (`Akses: 0, Login: 0 TA: 2025/2026, ...`) tampil sebagai teks polos.
- Baris modul (`Menu`, `e-Learning`, `Prestasi`, `Pustaka`, ...) tampil sebagai teks berjajar tanpa gaya tombol.
- Tab `Home` terbentuk, tetapi tanpa styling tab.
- Panel pengumuman terbentuk lengkap dengan pengelompokan (`PRIORITAS UTAMA (1)`, `WISUDA (3)`, `KKN (1)`) — jadi renderer sisi server berjalan.
- Di sisi kanan muncul **tiga pasang panah splitter Borderlayout telanjang** (▲▲▲) yang normalnya tersembunyi oleh CSS ZK.

Kemunculan panah splitter telanjang itu adalah petunjuk kuat bahwa **CSS milik ZK sendiri**
(`zk.wcs`) tidak diterapkan, bukan sekadar CSS aplikasi yang hilang.

---

## 4. Bukti yang Sudah Dikumpulkan

### 4.1 Halaman pembanding yang SEHAT (`/ecampus`) — sumber HTML

Pada instance yang tampilannya normal, `<head>` memuat:

```html
<link rel="stylesheet" type="text/css" href="/ecampus/zi/web/758e99ee/_zkiju-breeze/zul/css/zk.wcs"/>
<link rel="stylesheet" type="text/css" href="/ecampus/zi/web/758e99ee/zss/css/sssaf.css.dsp"/>
<script type="text/javascript" src="/ecampus/zi/web/758e99ee/js/zk.wpd" charset="UTF-8"></script>
<script type="text/javascript" src="/ecampus/zi/web/758e99ee/js/zul.lang.wpd" charset="UTF-8"></script>
<!-- ZK 5.0.13 PE 2013100810 Evaluation Only -->
...
<script src='/ecampus/zi/web/758e99ee/js/zul.breeze.wpd'></script>
```

dan blok `zkmx([0,'z4JQ_',{dt:'z_y49',cu:'/ecampus',uu:'/ecampus/zi',ru:'/WEB-INF/z/x/y/pages/main/index.zul'}...` berisi pohon komponen **lengkap** sampai footer.

Kesimpulan dari sini: pada instance sehat, `update-uri` = `/zi`, tema breeze termuat, dan
render sisi server selesai tanpa exception.

### 4.2 Konfigurasi yang SUDAH diverifikasi benar

Semua ini sudah dicek dan **bukan** penyebabnya:

| Yang dicek | Hasil |
|---|---|
| `breeze.jar` di `WEB-INF/lib` | **ADA** (bersama `zul.jar`, `zcommon.jar`, `zweb.jar`, `zhtml.jar`, `zkplus.jar`) |
| `update-uri` pada servlet `zkLoader` di `web.xml` | `/zi` |
| Mapping servlet `auEngine` (`DHtmlUpdateServlet`) | `/zi/*` — **cocok** dengan `update-uri` |
| Mapping `zkLoader` (`DHtmlLayoutServlet`) | `*.zul`, `*.zhtml` |
| ThemeProvider custom di `zk.xml` | **tidak ada** — tema hanya lewat `library-property` |
| `org.zkoss.theme.preferred` | `breeze` |
| `<ui-factory-class>` | `org.zkoss.zk.ui.http.SerializableUiFactory` |
| `max-upload-size` | `102400` (tidak berubah dalam 24 jam) |

### 4.3 Perubahan CSS aplikasi — sudah diperiksa, bukan penyebab

`web/css/css_utama.css` berubah 122 baris dalam 24 jam (r77647, r77687, r77688, r77691,
r77694, r77703). Seluruh perubahannya bersifat **menambah aturan** untuk kelas spesifik:

- lebar 100% untuk `.ais-crud-filter-grid`, `.fgrid`, `.gridHeader`, `.dms-*`
- kontras teks header dialog PPDB (`.pmb-zk-window .headerHbox`)
- lebar tetap kolom aksi (`.ais-action-column`, 56px)

Tidak ada aturan yang menghapus, menimpa, atau menyembunyikan styling global. **Tidak
menjelaskan gejala "seolah tanpa CSS".**

---

## 5. Exception yang Menyertai (konteks, mungkin terkait, mungkin tidak)

### 5.1 `IllegalArgumentException: setAttribute: Non-serializable attribute [javax.zkoss.zk.ui.Session]`

```
at org.apache.catalina.session.StandardSession.setAttribute(StandardSession.java:1074)
at org.zkoss.zk.ui.http.SessionAgent.put(SessionAgent.java:30)
at org.zkoss.zk.ui.http.SimpleSessionCache.put(SimpleSessionCache.java:36)
at org.zkoss.zk.ui.sys.SessionsCtrl.newSession(SessionsCtrl.java:158)
at org.zkoss.zk.ui.http.WebManager.getSession(WebManager.java:404)
at org.zkoss.zk.ui.http.DHtmlLayoutServlet.doGet(DHtmlLayoutServlet.java:130)
...
at ais.action.servlet.Main.forwardToPage(Main.java:214)
```

**Status: SUDAH DIPECAHKAN.** Disebabkan tag `<distributable/>` di `web.xml`. Tomcat hanya
menolak atribut non-`Serializable` bila context ditandai distributable, sedangkan objek sesi
ZK 5.0.13 (`javax.zkoss.zk.ui.Session`) tidak `Serializable`.

Dibuktikan lewat **uji bolak-balik**:

| Revisi | Aksi | Hasil |
|---|---|---|
| r77757 | tag dihapus | error **hilang** |
| r77759 | tag dipasang lagi | error **muncul lagi** (20-08 05:51:34, `/ecampus`) |
| r77760 | tag dihapus lagi | perbaikan final |

### 5.2 `HibernateException: createCriteria is not valid without active transaction`

```
at org.hibernate.context.ThreadLocalSessionContext$TransactionProtectionWrapper.invoke(...:341)
at com.sun.proxy.$Proxy87.createCriteria(Unknown Source)
at ais.common.Common.checkKonfigurasiBigIcon(Common.java:2148)
at ais.action.maintenance.MainAction.initData(MainAction.java:3266)
at ais.action.maintenance.MainAction.doAfterCompose(MainAction.java:2443)
at org.zkoss.zk.ui.impl.UiEngineImpl.execCreateChild0(...)
...
```

**Status: perbaikan sudah ada di repo (r77758), BELUM ter-deploy** (butuh rebuild WAR).

Sebab: `hibernate.cfg.xml` memakai `current_session_context_class=thread`, sehingga
`getCurrentSession()` mengembalikan proxy `ThreadLocalSessionContext$TransactionProtectionWrapper`
yang menolak semua method selain whitelist kecil (`isOpen`, `getTransaction`,
`beginTransaction`, `close`) selama tidak ada transaksi aktif.

`HibernateUtil.currentSession()` dulu hanya memeriksa "session terbuka" lewat
`isSessionUsable()`, sehingga menyerahkan session yang dipastikan melempar pada pemakaian
pertama. Perbaikan r77758 membuka transaksi bila belum aktif (pola Open Session In View),
dan jatuh ke `currentNativeSession()` bila gagal.

**Relevansi terhadap masalah tampilan:** bila `doAfterCompose` melempar di tengah jalan, ZK
berhenti membangun sisa pohon komponen. Ini BISA menghasilkan halaman setengah jadi. Namun
belum terbukti bahwa inilah penyebab hilangnya CSS.

---

## 6. Linimasa Perubahan 24 Jam Terakhir

Perbandingan HEAD terhadap baseline 19-08-2026 pukul 05:00 (4.214 berkas berubah):

| Kategori | Jumlah | Catatan |
|---|---|---|
| `.html` di `WEB-INF/bantuan/` | 1.339 | berkas bantuan, tidak menyentuh render |
| `.zul` halaman CRUD | 928 | `pages/master/...`, `maintenance/...` |
| Java modul lain | ~125 | koperasi, epsbed, payroll, laporan |
| Konfigurasi | 3 | `zk.xml`, `web.xml`, `css/css_utama.css` |

**Berkas jalur render halaman utama yang TIDAK berubah sama sekali:**

- `ais/action/maintenance/MainAction.java`
- `ais/common/Common.java`
- `ais/ui/util/MyGrid.java`
- `web/WEB-INF/z/x/y/pages/main/index.zul`

**Kelas UI ZK yang berubah:** `UIHelper.java`, `MyGrid.java`, `GridKolomHelper.java`,
`MyColumnConfig.java`, `PembayaranDashboardHtmlUtil.java`

**Listener/hook ZK yang berubah:** `DesktopCounterListener.java` (BARU, r77749),
`BantuanGlobalHook.java`, `AppStartupListener.java`, `ParameterTambahanPsbListener.java`

**Perubahan semantik `zk.xml`:** hanya SATU — penambahan
`<listener-class>ais.common.DesktopCounterListener</listener-class>` (r77750, 20-08 04:25:41).
Sudah **dikembalikan** ke keadaan kemarin (r77670) atas permintaan pemilik sistem.

---

## 7. Yang Sudah Dikesampingkan (beserta alasannya)

| Dugaan | Alasan dikesampingkan |
|---|---|
| `breeze.jar` hilang | JAR ada di `WEB-INF/lib` |
| `update-uri` salah | `/zi` di `web.xml` cocok dengan mapping `auEngine` `/zi/*` |
| ThemeProvider custom gagal | tidak ada ThemeProvider custom di `zk.xml` |
| `css_utama.css` merusak tampilan | seluruh perubahannya aditif untuk kelas spesifik |
| Regresi kode di jalur render | `MainAction`, `Common`, `MyGrid`, `index.zul` tidak berubah |
| `<distributable/>` | sudah dihapus; menyelesaikan error sesi, TIDAK menyelesaikan gejala tampilan |
| `DesktopCounterListener` | sudah dilepas dari `zk.xml`; hanya menyimpan `Integer` (Serializable) ke atribut sesi |

### 7.1 Hasil perbandingan langsung dengan SVN kode kemarin

Perbandingan ini dilakukan pada 20 Agustus 2026 terhadap revisi terakhir tanggal
19 Agustus 2026, yaitu **r77713** (19-08-2026 23:57:13), sampai **HEAD r77762**.
Working copy memang terdiri dari dua root terpisah: `src/main/java` (`^/src`) dan
`src/main/webapp` (`^/web`).

| Berkas/komponen jalur render | Perbedaan r77713 → r77762 | Kesimpulan |
|---|---|---|
| `WEB-INF/zk.xml` | **Tidak ada perbedaan akhir** | Listener `DesktopCounterListener` sempat ditambahkan pada r77750, tetapi dikembalikan pada r77762. Isi HEAD kembali sama dengan kode kemarin. |
| `WEB-INF/web.xml` | `<distributable/>` dihapus | Ini menyelesaikan penolakan atribut sesi ZK non-serializable. Mapping `zkLoader`, `auEngine`, dan `update-uri` tidak berubah. |
| `css/css_utama.css` | **Tidak berubah** | Revisi terakhir tetap r77703, masih tanggal 19 Agustus. Tidak ada perubahan CSS pada tanggal 20 Agustus. |
| `pages/main/index.zul` | **Tidak berubah** | Revisi terakhir r77446 (15 Agustus). Struktur halaman utama sama dengan kode kemarin. |
| JAR inti ZK/tema (`zk`, `zul`, `zcommon`, `zweb`, `zhtml`, `zkplus`, `breeze`) | **Tidak berubah** | Tidak ada penggantian library atau tema antara kode kemarin dan HEAD. |
| `MainAction.java`, `Common.java`, `MyGrid.java`, `UIHelper.java`, `GridKolomHelper.java`, `MyColumnConfig.java` | **Tidak berubah** | Tidak ada regresi tanggal 20 Agustus pada kelas-kelas pembentuk halaman utama. |
| `HibernateUtil.java` | Berubah pada r77758 | Memastikan transaksi aktif sebelum `createCriteria`; perbaikan ini mencegah render berhenti di `MainAction.initData`. |
| `DesktopCounterListener.java` | Berkas baru tetap ada, tetapi tidak terdaftar | Karena sudah dilepas dari `zk.xml` pada r77762, kelas ini tidak dijalankan oleh ZK. |
| `hibernate.cfg.xml` | `hbm2ddl.auto` berubah `update` → `none` pada HEAD | Berkaitan dengan pembaruan skema saat startup, bukan pemuatan `zk.wcs`/tema. Working copy Java lokal masih tertinggal untuk berkas ini. |

Jumlah perubahan keseluruhan sejak r77713 memang besar (**58 berkas Java** dan
**2.274 berkas web**), tetapi setelah disaring ke jalur pemuatan resource ZK, satu-satunya
perbedaan akhir yang relevan hanyalah `web.xml`, tepatnya penghapusan
`<distributable/>`. Tidak ada perubahan pada CSS, tema, JAR ZK, `index.zul`, mapping
resource `/zi`, atau kelas utama yang dapat menjelaskan hilangnya seluruh styling.

**Kesimpulan perbandingan SVN:** gejala pada tangkapan layar bukan regresi kode CSS/tema
dibandingkan kode kemarin. Bukti lebih kuat mengarah ke **artefak/deployment atau pengiriman
resource per-instance**: WAR `/akfarsam` tidak sama/tertinggal, cache resource ZK/Tomcat
tidak sinkron, atau request `/akfarsam/zi/.../zk.wcs` gagal melalui proxy.

Catatan working copy saat audit:

- `WEB-INF/zk.xml` lokal sudah pada r77762 dan `WEB-INF/web.xml` pada r77761.
- `HibernateUtil.java` lokal sudah memuat perbaikan r77758.
- `hibernate.cfg.xml` lokal masih r77758 dan ditandai tertinggal dari HEAD r77762
  (`hbm2ddl.auto=update` lokal versus `none` di HEAD).
- Ada berkas tidak terkontrol `WEB-INF/zk.xml.all`; berkas ini bukan konfigurasi aktif,
  tetapi sebaiknya tidak ikut terkemas ke WAR untuk menghindari kebingungan audit.

---

## 8. Yang BELUM Diperiksa — Langkah Diagnostik Prioritas

Ini yang paling menentukan dan **belum dilakukan**:

### 8.1 Apakah `<link ... zk.wcs>` ADA di sumber halaman yang rusak?

Buka `https://ecampus.stiksam.ac.id/akfarsam/main`, View Source, cari `zk.wcs`.

- **ADA** → berkasnya gagal diambil browser. Lanjut ke 8.2.
- **TIDAK ADA** → ZK berhenti sebelum menulis header resource. Kuat mengarah ke exception
  di `doAfterCompose` (bagian 5.2). Deploy r77758 lalu uji ulang.

### 8.2 Status HTTP resource ZK

DevTools → Network → reload. Periksa request ke `/akfarsam/zi/...`:

- 404 → masalah mapping/deployment pada context itu
- 500 → periksa `catalina.out` pada detik yang sama
- 200 tapi isi kosong/terpotong → curiga proxy/AJP atau kompresi

### 8.3 Bandingkan panjang `zkmx(` antara halaman sehat dan rusak

Bila blok `zkmx(` pada halaman rusak jauh lebih pendek dan berhenti di tengah (tidak sampai
footer), itu bukti langsung render terpotong di tengah `doAfterCompose`.

### 8.4 Periksa `compress` dan proxy

`zkLoader` punya `<init-param>compress = true`. Bila proxy di depan Tomcat (AJP)
mengubah/menggandakan header encoding, berkas `.wcs`/`.wpd` bisa sampai rusak ke browser
sementara HTML tetap utuh. **Belum diperiksa sama sekali.**

---

## 9. Pertanyaan Terbuka

1. Mengapa `/ecampus` sehat sedangkan `/akfarsam` tidak, padahal WAR-nya dari kompilasi yang sama?
   Apa yang berbeda pada level deployment/proxy/Tomcat antar kedua instance?
2. Apakah instance `/akfarsam` pernah berhasil menampilkan halaman bertema sejak restart terakhir?
   (Bila tidak pernah, cakupan pencarian menyempit ke deployment, bukan ke kode.)
3. Apakah `/akfarsam` memakai `<Context>` atau `Manager` khusus di
   `conf/Catalina/<host>/akfarsam.xml` yang berbeda dari `/ecampus`?
4. Apakah berkas hasil kompilasi ZK (`~/.zk` atau direktori temp Tomcat) perlu dibersihkan
   setelah redeploy? ZK menyimpan cache resource ber-hash (`758e99ee` pada contoh sehat);
   hash yang tidak sinkron antara HTML dan berkas di disk akan menghasilkan 404.

---

## 10. Perintah Siap Pakai

Cek resource ZK yang gagal, dari log:

```bash
grep -nE "zi/web|\.wcs|\.wpd" /opt/tomcat/logs/catalina.out | tail -30
```

Cek kedua exception sekaligus:

```bash
grep -cE "Non-serializable attribute|is not valid without active transaction" /opt/tomcat/logs/catalina.out
```

Pastikan `<distributable/>` sudah tidak ada di seluruh context:

```bash
grep -rn "^[[:space:]]*<distributable" /opt/tomcat*/webapps/*/WEB-INF/web.xml
```

Bandingkan `web.xml` antara instance sehat dan bergejala:

```bash
diff /opt/tomcat/webapps/ecampus/WEB-INF/web.xml /opt/tomcat/webapps/akfarsam/WEB-INF/web.xml
```

---

## 11. Batasan Kerja (WAJIB dipatuhi siapa pun yang melanjutkan)

Aturan dari pemilik sistem:

1. Kelompokkan error yang penyebabnya sama; jangan memperbaiki berulang.
2. Cari akar masalah dari stack trace, bukan hanya baris terakhir.
3. Pertahankan seluruh fungsi lama. Jangan mematikan logic, jangan hanya memberi komentar.
4. `openSession()` / `currentNativeSession()` **wajib** ditutup di `finally` dengan
   clear/disconnect/close. `currentSession()` **jangan** ditutup manual.
5. Kompatibel Java 1.7, gaya Java 1.6: **tanpa** lambda, try-with-resources, diamond
   operator, Stream API, atau fitur Java 8+.
6. Berkas ZUL/JSP/CSS diletakkan sesuai struktur folder `webapp` yang benar.
7. Terapkan perbaikan langsung ke berkas yang sudah ada (edit in-place). **Jangan** membuat
   berkas ZIP sebagai hasil kerja.
8. Sertakan laporan singkat isi perubahan beserta alasannya.

### Catatan struktur repositori

- Dua working copy SVN terpisah: `src/main/java` dan `src/main/webapp`
- Setiap perubahan di `src/main/java/<path>` **wajib** disalin ke `src/main/src/<path>`
- Cron di server meng-commit otomatis tiap beberapa menit, jadi pohon kerja **harus selalu
  bisa dikompilasi**
- Perintah kompilasi:

```bash
javac -encoding UTF-8 -source 8 -target 8 -nowarn -implicit:none -d <scratch> -sourcepath src/main/java -cp "build/classes;src/main/webapp/WEB-INF/lib/*" <files>
```

---

## 12. Ringkasan untuk Pembaca yang Terburu-buru

Halaman ZK tampil tanpa CSS pada instance `/akfarsam`. Konfigurasi ZK (tema, JAR,
`update-uri`, mapping servlet) sudah diverifikasi **benar**. CSS aplikasi sudah diverifikasi
**tidak merusak**. Kode jalur render halaman utama **tidak berubah** dalam 24 jam.

Gejalanya **sudah hilang** setelah deploy 20-08 pukul 06:07, tetapi tiga perubahan berangkat
bersamaan (hapus `<distributable/>`, lepas `DesktopCounterListener`, restart) sehingga
**tidak ada satu pun yang terbukti** sebagai penyebab. Pemulihan `/akfarsam` sendiri belum
dikonfirmasi — tangkapan layar pemulihan berasal dari `/ecampus`.

**Hipotesis terkuat (lihat bagian 0.4):** interaksi antara `DesktopCounterListener` yang
menulis ke sesi pada setiap desktop init dengan context yang ditandai `distributable`.
Ini menjelaskan mengapa tag lama menghasilkan error baru.

**Tiga hal yang masih terbuka:**

1. Konfirmasi apakah `/akfarsam` benar-benar sudah pulih.
2. Uji terkendali kombinasi **tag ADA + listener TIDAK ADA** (satu-satunya yang belum dicoba).
3. Deploy r77758 — perbaikan `createCriteria is not valid without active transaction` masih
   menunggu rebuild WAR; selama belum, error itu bisa muncul lagi setelah restart berikutnya.

**Bila gejala tampilan kambuh:** buka View Source pada halaman yang rusak, pastikan apakah
`<link ... zk.wcs>` ada, lalu periksa status HTTP request ke `/<context>/zi/...` di DevTools
Network. Dua data itu langsung memisahkan "masalah pengiriman resource" dari "render
terpotong di server".
