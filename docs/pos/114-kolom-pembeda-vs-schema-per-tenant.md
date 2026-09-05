# 114 — Kolom pembeda (`pendaftar_id`) vs schema-per-tenant

**Tanggal:** 2026-09-06
**Sifat:** analisis penimbangan. Tidak ada kode yang diubah.
**Pemicu:** "kalau misalnya nambah kolom di semua tabel terkait bagaimana? Misalnya untuk modul
akunting, jika ada tenant baru maka `akunting.akun` ditambah kolom yang berelasi dengan pendaftar,
begitu juga semua tabel lain? Apakah ini lebih berat?"
**Pendamping:** [113](113-kompatibilitas-per-tenant-modul-lain.md).

---

## Jawaban ringkas

**Tidak lebih berat — biayanya terbalik.** Untuk modul ber-ORM seperti Apotik, pendekatan kolom
pembeda **jauh lebih ringan** daripada schema-per-tenant; untuk modul ber-JDBC seperti
SalesInventory dan Kantin, ia **lebih berat**. Dan sifat biayanya berbeda: schema-per-tenant dibayar
**per modul secara bertahap**, kolom pembeda dibayar **global di muka**.

Ada satu syarat yang menentukan, dan repo ini sudah memenuhinya: titik cegat tunggalnya sudah ada
dan sudah terpasang.

---

## Mengapa ini justru menjawab kendala Apotik

Dokumen 113 menyimpulkan Apotik terhalang karena entitasnya memaku `@Table(schema = "sirs")` —
patokan yang dibaca sekali saat SessionFactory dibangun dan tidak bisa diubah per-permintaan.

Kolom pembeda **tidak menyentuh patokan itu sama sekali.** Tabelnya tetap `sirs.item_medis`; yang
berubah hanya barisnya bertambah satu kolom, dan kuerinya bertambah satu syarat. Hibernate 3.6.10
punya alatnya, dan sudah terverifikasi ada di jar-nya:

```
org/hibernate/annotations/Filter.class
org/hibernate/annotations/FilterDef.class
org/hibernate/annotations/FilterDefs.class
```

```java
@FilterDef(name = "tenant", parameters = @ParamDef(name = "pid", type = "long"))
@Filter(name = "tenant", condition = "pendaftar_id = :pid")
@Table(schema = "sirs", name = "item_medis")
public class ItemMedis { ... }
```

Lalu sekali per-session: `session.enableFilter("tenant").setParameter("pid", pid)`.

Seluruh HQL dan Criteria langsung tersaring **tanpa menyentuh kodenya**. Inilah yang membuat 176
panggilan ORM Apotik berubah dari penghalang terbesar menjadi bukan masalah.

> Catatan versi: `MultiTenantConnectionProvider` dan `CurrentTenantIdentifierResolver` baru ada di
> Hibernate **4**. Di 3.6.10 tidak ada dukungan multi-tenant bawaan — `@Filter` adalah satu-satunya
> jalan ORM, dan ia memang cukup untuk model kolom pembeda (tidak untuk schema-per-tenant).

---

## Skalanya

Entitas ber-`@Table(schema=...)`: **1.569**.

| schema | entitas | `@UniqueConstraint` |
|---|---:|---:|
| public | 838 | 38 |
| sirs | 124 | 3 |
| koperasi | 103 | 17 |
| library | 86 | 0 |
| sekolah | 82 | 2 |
| asset | 63 | 0 |
| employ | 51 | 0 |
| rab | 47 | 0 |
| payroll | 43 | 0 |
| **akunting** | **41** | **3** |
| surat | 33 | 1 |
| lain-lain | 58 | 0 |
| **TOTAL** | **1.569** | **64** |

**Akunting sendiri kecil: 41 entitas, 3 batasan unik.** Bila hanya akunting yang dikerjakan,
lingkupnya sangat tertangani.

### Titik sentuh basis data di seluruh repo

Dari 3.359 berkas yang menyentuh basis data:

| kategori | jumlah | tersaring otomatis? |
|---|---:|---|
| HQL + Criteria | **9.266** | **ya** |
| `session.get`/`load` per-id | 1.738 | **tidak** — filter tidak berlaku pada pencarian per-id |
| SQL native (`createSQLQuery`, `prepareStatement`) | 2.464 | **tidak** — melewati Hibernate |
| tulis (`save`/`update`/`merge`/…) | 5.158 | kolomnya harus diisi |

**68,8% pembacaan aman otomatis.** Sisanya 4.202 titik harus ditangani.

---

## Yang membuatnya layak: titik cegat tunggal sudah terpasang

Angka 4.202 dan 5.158 di atas terdengar mustahil — tetapi keduanya sebagian besar bisa ditutup di
**satu berkas**, karena repo ini sudah punya interceptor yang aktif:

```java
// ais/database/hibernate/AuditTimestampInterceptor.java
public class AuditTimestampInterceptor extends EmptyInterceptor {
    public boolean onLoad(...)        // sudah ada
    public boolean onSave(...)        // sudah ada
    public boolean onFlushDirty(...)  // sudah ada
}
```

Dan ia dipasang pada **SessionFactory**, bukan per-session:

```java
// HibernateUtil.java:295,299,334
sf = cfgUtama.setInterceptor(AuditTimestampInterceptor.instance).buildSessionFactory();
```

Artinya seluruh 2.537 titik pembukaan session mewarisinya. Maka:

- **`onSave` mengisi `pendaftar_id` otomatis** → menutup 5.158 titik tulis di satu tempat.
- **`onLoad` memeriksa `pendaftar_id` cocok dengan tenant aktif, dan melempar bila tidak** →
  mengubah 1.738 pencarian per-id dari **kebocoran diam** menjadi **kegagalan bersuara**, juga di
  satu tempat.

Ini keunggulan struktural yang nyata, dan tidak dimiliki schema-per-tenant: di sana tidak ada satu
tempat pun yang bisa menangkap cabang yang lupa ditulis. Bug §21 (saldo awal kartu stok) lolos
justru karena tidak ada penangkap semacam itu.

> **Syarat:** interceptornya singleton tingkat-factory, jadi dipakai bersama semua thread. Identitas
> tenant **wajib** datang dari `ThreadLocal`/`TenantContext`, tidak boleh disimpan sebagai medan
> interceptor. Menyimpannya di sana akan membuat permintaan bersamaan saling menimpa tenant.

Yang **tidak** bisa ditutup interceptor: **2.464 titik SQL native**. Keduanya melewati Hibernate
sepenuhnya. Inilah sisa pekerjaan tangan yang sebenarnya.

---

## Risiko yang berbeda sifatnya, bukan hanya besarnya

Ini pertimbangan terpenting, dan bukan soal biaya.

| | bila ada yang terlewat |
|---|---|
| **schema-per-tenant** | kueri mengenai schema **instalasi bersama** — datanya salah, tetapi bukan milik penyewa lain |
| **kolom pembeda** | kueri mengembalikan **baris penyewa lain** — kebocoran lintas-penyewa sungguhan |

Pada schema-per-tenant, cabang yang lupa memberi angka yang salah dari instalasi bersama; itulah
persis bentuk bug §21 (900 vs 40). Buruk, tapi bukan data pelanggan lain.

Pada kolom pembeda, filter yang lupa memberi **data penyewa lain dalam bentuk yang sepenuhnya masuk
akal** — tabel sama, kolom sama, tampak wajar. Tidak ada yang tampak keliru sampai ada pelanggan
yang melihat data pelanggan lain.

Dengan 2.464 titik SQL native yang tidak terlindungi, tiap satu di antaranya adalah calon kebocoran
seperti itu. Interceptor tidak menjangkaunya.

### Penutup lubang terakhir: Row-Level Security

PostgreSQL ≥ 9.5 bisa memaksakan syarat tenant **di dalam basis data**, sehingga 2.464 titik SQL
native ikut terlindungi tanpa disunting satu per satu. Ini satu-satunya cara menutup lubang itu
secara menyeluruh.

Dua hal yang harus diperiksa lebih dulu, dan **belum**:

1. **Versi PostgreSQL produksi.** Katalog migrasi ditulis bergaya 9.3 (`TenantSchemaMigrasiSelfTest`
   menolak `jsonb`, `ON CONFLICT`, `CREATE INDEX IF NOT EXISTS`). Bila servernya memang 9.3, RLS
   **tidak ada** dan opsi ini gugur.
2. **Interaksinya dengan c3p0.** RLS butuh penyetelan per-transaksi (`SET LOCAL`). Koneksi c3p0
   dipakai bergantian dan **membawa state** — ini jebakan yang sama persis yang membuat
   `SET search_path` ditolak sejak awal pemindahan ini. Harus `SET LOCAL` di dalam transaksi, tidak
   boleh `SET` biasa.

---

## Jebakan khusus akunting

Pertanyaannya memakai `akunting.akun` sebagai contoh, dan justru di sanalah ada masalah yang tidak
muncul pada modul lain: **katalog tenant sudah punya inti akuntansinya sendiri.**

```
CREATE TABLE {S}.akun
CREATE TABLE {S}.jurnal
CREATE TABLE {S}.jurnal_detail
CREATE TABLE {S}.periode_akuntansi
CREATE TABLE {S}.posting_log
```

Jadi penyewa `cmnmedika` **sudah** punya bagan akun dan buku jurnalnya di `cmnmedika.akun` dan
`cmnmedika.jurnal`. Bila kemudian `akunting.akun` diberi kolom `pendaftar_id`, maka penyewa yang
sama punya **dua rumah untuk konsep yang sama** — dan pertanyaan "buku mana yang benar" tidak punya
jawaban.

Itu persis bentuk masalah yang dihindari sepanjang pemindahan ini: dua kebenaran yang boleh
berselisih. Alasan yang sama membuat `ekstrak-dbf.py` menolak menulis langsung ke basis data dan
hanya menyiapkan muatan bagi importir yang sudah teruji.

**Maka keputusan akuntansi harus diambil sekali, bukan per modul.** Pilihannya:

- **(a)** Akuntansi tetap se-tenant di `{S}.jurnal`; Apotik dibuat memposting ke sana. Konsisten
  dengan yang sudah dibangun dan sudah teruji, tetapi Apotik butuh jalur posting baru.
- **(b)** Akuntansi memakai kolom pembeda di `akunting.*`; `{S}.akun`/`{S}.jurnal` **dipensiunkan
  sebelum dipakai produksi**. Masih mungkin **sekarang** — `cmnmedika` belum jalan — tetapi tidak
  lagi setelah ada data nyata di dalamnya.

Pilihan (b) punya jendela waktu yang akan tertutup sendiri. Itu membuatnya mendesak untuk diputuskan
walau tidak mendesak untuk dikerjakan.

---

## Perbandingan akhir

| | schema-per-tenant | kolom pembeda |
|---|---|---|
| modul ber-ORM (Apotik, Hotel, Elearning) | sangat mahal | **hampir gratis** |
| modul ber-JDBC (SalesInventory, Kantin) | **sudah selesai / murah** | mahal — 2.464 titik disunting tangan |
| sifat biaya | per modul, bertahap | global, sekali di muka |
| batasan unik | tidak berubah | **64 harus jadi gabungan** `(pendaftar_id, …)` |
| kunci asing | terjamin dalam satu schema | tidak menjamin se-penyewa — butuh FK gabungan |
| data lama | tidak disentuh | **wajib diisi mundur** di seluruh 1.569 tabel |
| bila terlewat | data instalasi bersama | **data penyewa lain** |
| titik cegat | tidak ada | **sudah ada dan terpasang** |
| cadang/pulih per penyewa | mudah (`pg_dump -n`) | sulit — barisnya bercampur |

---

## Rekomendasi

**Jangan mengubah arah untuk `cmnmedika` sekarang.** Inventory & Sales sudah selesai dengan
schema-per-tenant, sudah terbukti (201 verdikt, 0 gagal), dan sudah berpenjaga
(`audit-local-first.py` 0 pelanggaran). Membongkarnya berarti membuang pekerjaan yang sudah terbukti
demi pekerjaan yang belum.

**Untuk modul berikutnya, kolom pembeda memang pilihan yang lebih baik** — terutama Apotik, karena
ia justru menghapus penghalang yang membuat Apotik mahal. Syaratnya tiga:

1. `onSave`/`onLoad` di `AuditTimestampInterceptor` dipakai sebagai penegak, dengan tenant dari
   `ThreadLocal`.
2. Versi PostgreSQL produksi dipastikan; bila ≥ 9.5, RLS dipakai untuk menutup 2.464 titik SQL
   native, dengan `SET LOCAL` di dalam transaksi.
3. Keputusan akuntansi (a) atau (b) diambil **sebelum** `cmnmedika` menyimpan data nyata.

**Yang harus dihindari: mencampur keduanya tanpa keputusan sadar.** Dua model isolasi berarti dua
model kegagalan, dan penjaga yang dibangun untuk satu model tidak melihat kebocoran model lainnya.

---

## Batas analisis ini

- Angka titik sentuh dihitung dengan pencocokan pola atas teks sumber; sebutan di dalam komentar
  belum dipisahkan, jadi ia **perkiraan**, bukan pembilangan tepat.
- Belum ada yang dijalankan: tak satu pun `@Filter` dipasang, dan perilaku filter pada asosiasi malas
  di Hibernate 3.6 belum diuji pada repo ini.
- Versi PostgreSQL produksi **belum diketahui**, padahal ia menentukan tersedia-tidaknya RLS.
- 64 `@UniqueConstraint` dihitung dari anotasi; batasan unik yang dibuat lewat DDL di luar anotasi
  belum terhitung, jadi angka itu **batas bawah**.
