# 26 — Jurnal Penyusutan: pola dua fase dipakai sejak awal

Penyusutan adalah beban bulanan yang berulang: tiap detail aset melahirkan satu baris
`PenyusutanAsset` per bulan, dan tiap baris itu perlu jurnal — debet **beban penyusutan**,
kredit **akumulasi penyusutan**. Barisnya sudah lama tampil di Draft Jurnal; tombolnya mati.

Tujuh belas dari 35 baris punya mesin posting sebelum ini; sekarang **delapan belas**.

---

## 1. Pelajaran dokumen 25 terpakai langsung

Mesin ini memanggil `AssetUtil.ambilDataAkun` **dua kali per dokumen** — sekali untuk akun
biaya, sekali untuk akun akumulasi. Utilitas itu **membuka dan menutup sesi Hibernate
sendiri**, sehingga sesi yang dipegang pemanggil menjadi basi.

> **Dikoreksi 24 Agustus 2026.** Yang menutup sesi adalah `AssetUtil.ambilDataAkun`
> **sendiri**, bukan `ConstantValues.ambil` yang dipanggilnya. Dibuktikan langsung: setelah
> `ambilDataAkun`, `session.isOpen()` berubah dari `true` menjadi `false`; setelah
> `ConstantValues.ambil` sesi tetap terbuka. Perbedaannya penting — memperlakukan keduanya
> sama menghasilkan temuan palsu, dan itu sempat terjadi pada satu pemindaian di sini.


Pada modul BAST hal ini baru ketahuan dari `error_log` setelah separuh dokumen gagal tanpa
pesan. Di sini polanya dipakai sejak baris pertama ditulis:

| Fase | Yang boleh dilakukan |
|---|---|
| **1** | menelusuri entitas, menyusun keterangan, memanggil `ambilDataAkun`. Keluar hanya **id dan angka**. |
| **2** | sesi diambil ULANG, seluruh entitas dimuat kembali (penyusutan, riwayat, kedua akun, satuan kerja), baru transaksinya dibuka. |

Satu detail kecil yang mudah terlewat: **keterangan jurnalnya disusun SEBELUM akunnya dicari.**
Kalimatnya membaca banyak field lazy dari detail aset (barcode, nama, umur ekonomis,
keterangan) — dan sesudah `ambilDataAkun` berjalan, membaca field lazy dari entitas itu dapat
gagal karena sesinya sudah ditutup.

Harness menyediakan **dua** dokumen yang dapat diposting justru untuk ini: dengan satu dokumen,
kesalahan pengambilan sesi tidak akan pernah terlihat.

---

## 2. Kriteria dasbor lebih longgar daripada mesinnya

`initCriteria` mesin memakai tiga **INNER JOIN**:

```java
.createAlias("assetDetail", "assetDetail")
.createAlias("assetDetail.asset", "asset")
.createAlias("asset.masterAsset", "masterAsset")
```

`DraftJurnalRingkasanUtil.kriteriaPenyusutan` tidak punya satu pun. Ketiga join itu menentukan
isi hasil, bukan sekadar penamaan: akun biaya dan akun akumulasi diambil dari **master aset**,
jadi baris penyusutan yang rantai asetnya putus memang tidak dapat dijurnal oleh siapa pun.

Akibatnya baris seperti itu **terhitung sebagai draft tetapi tidak akan pernah dapat diposting**
— keluarga cacat yang sama dengan "tagihan di bawah PO non-termin" pada dokumen 24.

Seperti di sana, keadaan itu **tidak** diperbaiki dengan menyamakan kriterianya secara diam-diam:
memilih salah satu sisi mengubah dokumen mana yang terjurnal. Yang dilakukan adalah
memastikan penggunanya **diberi tahu** — kalimat "N dokumen lain dilewati" dari dokumen 23
menyebutkan selisihnya, dan harness menguncinya sebagai kasus uji.

---

## 3. Kunci haknya berdiri sendiri

Penyusutan tidak punya layar POS. Memakai kunci pengadaan akan keliru — yang dijurnal beban
penyusutan bulanan, bukan dokumen pengadaan. Jadi ditambahkan kunci baru:

```java
DAFTAR.add(new Entri(MODUL_POS, "posting_penyusutan", "Akuntansi: Posting Penyusutan Aset", …));
```

sekeluarga dengan `posting_hpp`, `posting_penjualan`, `posting_kulakan`, dan
`posting_penyesuaian` — semuanya kunci posting tanpa layar CRUD sendiri. Didaftarkan juga di
**kedua** tempat daftar fail-closed, sehingga tidak muncul untuk peran POS biasa dan hanya
terbuka secara bawaan untuk peran akuntansi; admin tetap dapat menyalakannya per peran.

Memberi kunci sendiri bukan formalitas: hak memposting beban penyusutan pantas dapat diberikan
terpisah dari hak posting lain.

---

## 4. Jurnalnya

| Sisi | Akun | Sumber |
|---|---|---|
| Debet | beban penyusutan | `masterAsset.akunBiayaPenyusutan` |
| Kredit | akumulasi penyusutan | `masterAsset.akunPenyusutan` |

Keduanya disaring menurut satuan kerja detail asetnya. Nilai yang tidak positif **menukar kedua
sisinya**, sama seperti layar — penyusutan negatif adalah koreksi, dan menulisnya di sisi yang
sama akan membalik arah saldo. Tanggal jurnalnya `perTanggal`, bukan tanggal posting.

Kedua akun itu disimpan sebagai **JSON di kolom `*_str`**, bukan di kolom bigint bernama mirip —
`akun_penyusutan_str` dan `akun_biaya_penyusutan_str`. Kolom bigint `akun_penyusutan` ada juga
dan dipetakan getter lain. Menulis ke kolom yang salah membuat akunnya tampak terisi padahal
tidak terbaca; itu sempat terjadi saat menyiapkan fixture BAST.

Berlaku pula tiga perbedaan yang sama dengan dokumen 23–25: riwayat posting dibuat setelah
dipastikan ada yang perlu dikerjakan, penanda ditulis lewat SQL, dan baris
`akunting.transaksi` dihapus lebih dulu saat pembatalan.

Satu hal yang **lebih keras** daripada modul sebelumnya: sesudah penandanya dipasang, yang
dilakukan bukan `evict` satu entitas melainkan **`session.clear()`** — seluruh konteks
persistensi dikosongkan. Dengan empat getter yang menulis balik, sekadar membaca satu baris
sudah membuatnya kotor, dan fase 1 serta fase 2 dapat berakhir memegang dua instance berbeda
untuk baris yang sama. Mengeluarkan salah satunya menyisakan yang lain untuk di-flush saat
sesi ditutup. Jurnal yang sudah closing tidak
pernah ikut terhapus. Pembatalan tidak menyaring `ref` — modul inilah satu-satunya yang menulis
jurnal bertaut `penyusutan_asset`.

---

## 5. Membaca satu baris memindahkan tanggalnya

Temuan paling mahal dari modul ini, dan ia **sudah ada sebelum port POS** — layar ZK memanggil
getter yang sama.

`PenyusutanAsset` punya **empat** getter yang menulis balik fieldnya saat dibaca: `getNama`,
`getNilaiBuku`, `getKodeUnik`, dan `getPerTanggal`. Yang terakhir bukan sekadar menulis ulang
nilai yang sama:

```java
public Date getPerTanggal() {
    if (getTahunKe() != null && assetDetail.getTanggalBeli() != null) {
        calendar.setTime(assetDetail.getTanggalBeli());
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + getTahunKe());
        perTanggal = calendar.getTime();          // <-- ditulis balik
    }
    return perTanggal;
}
```

dan `AssetDetail.getTanggalBeli()` diakhiri:

```java
return tanggalBeli == null ? ais.ui.util.WaktuUtil.getDate() : tanggalBeli;   // HARI INI
```

Gabungannya: untuk baris penyusutan yang **tanggal beli asetnya kosong**, sekadar membacanya
menghitung ulang `perTanggal` menjadi **hari ini + tahunKe bulan**, lalu Hibernate menyimpannya
saat flush.

Ini terlihat dalam pengujian: dua baris uji bertanggal `2013-07-08` berubah sendiri menjadi
`2026-09-24` sesudah diposting — bergeser lebih dari tiga belas tahun. Barisnya keluar dari
periode yang sedang dikerjakan, jurnalnya pun bertanggal masa depan, dan pembatalan pada
periode aslinya tidak menemukan apa pun.

Yang menyesatkan: **posting melaporkan sukses**, jurnalnya memang ada, penandanya memang
terpasang. Yang hilang hanya periodenya.

Tidak diperbaiki dari sini. `perTanggal` tampaknya memang dirancang selalu turunan dari tanggal
beli, dan mengubah salah satu getter itu akan menggeser tanggal jurnal seluruh sistem — termasuk
yang sudah terposting lewat layar ZK.

### Diperiksa di produksi, 24 Agustus 2026

```sql
SELECT count(*) FROM asset.penyusutan_asset p
  JOIN asset.asset_detail d ON d.id = p.asset_detail
 WHERE d.tanggalbeli IS NULL;
-- 0
```

**Nol.** Tidak ada satu pun baris penyusutan yang detail asetnya tanpa tanggal beli tersimpan,
jadi jalur "jatuh ke HARI INI" itu tidak pernah terpicu di produksi. Skenario terburuknya —
tanggal melompat bertahun-tahun — **tidak terjadi**.

Yang belum tertutup oleh angka itu: `getTanggalBeli()` tidak hanya jatuh ke hari ini bila kosong,
ia juga **menimpa** nilai tersimpan bila rantai pengadaannya menyediakan tanggal lain —
dari LPJ uang muka, atau dari tanggal pembuatan penerimaan barang. Bila tanggal turunan itu
berbeda dari yang tersimpan, `perTanggal` tetap bergeser, hanya lebih halus.

Pendeteksi gejalanya tidak bergantung pada rantai mana pun: baris penyusutan yang bertanggal
**sesudah** posting yang menerbitkannya.

```sql
SELECT count(*) FROM asset.penyusutan_asset p
  JOIN akunting.posting_history ph ON ph.id = p.posting_history
 WHERE date(p.pertanggal) > date(ph.tanggal);
```

Dijalankan pada hari yang sama: **0** juga.

### Kesimpulan

Kedua pemeriksaan nol. Di produksi, tanggal penyusutan **tidak pernah tergeser** — baik oleh
jalur "jatuh ke hari ini" maupun oleh penimpaan dari rantai pengadaan. Masuk akal: baris
penyusutan dibangkitkan oleh `PenyusutanAssetAction` dengan rumus yang sama, jadi nilai
tersimpan dan nilai hitung ulangnya memang identik sejak awal.

Sisa celah yang tidak tertutup kedua query itu tinggal satu dan sempit: pergeseran **mundur**
(tanggal turunan lebih awal daripada yang tersimpan) tidak akan terlihat oleh query kedua, dan
hanya mungkin bila tanggal di rantai pengadaannya diubah SESUDAH baris penyusutannya dibuat.
Tidak dikejar lebih jauh.

Yang tetap berlaku terlepas dari itu: keempat getter tersebut **membuat entitasnya kotor setiap
kali dibaca**, meski nilainya tidak berubah. Itulah sebabnya mesin posting di sini memakai
`session.clear()`, bukan `evict` satu objek. Alasan itu tidak gugur oleh hasil nol ini.

Harness mengunci keadaan ini: fixture-nya mengisi tanggal beli tepat sebulan sebelum tanggal
uji, dan salah satu langkahnya membandingkan tanggal di basis data sesudah posting.

---

## 6. Uji

`TesPostingPenyusutan` — **22 lulus, 0 gagal**. Dokumen uji bertanggal **2013-07-08**, dan harness memeriksa sendiri
bahwa tanggal itu kosong sebelum memakainya — bukan sekadar berasumsi. Tiga baris penyusutan
dibuat sengaja:

| Baris | Diharapkan |
|---|---|
| aset A, master berakun | terposting |
| aset B, master berakun | terposting, dan penanda A tidak tertimpa |
| aset tanpa master | terhitung sebagai draft, tetapi dilewati dan **disebut** |

Lalu pembatalannya harus mengembalikan keadaan seperti semula, tanpa meninggalkan baris
`akunting.transaksi` yatim. Ditambah dua penjaga: baris penyusutan milik UAT tidak berubah
statusnya, dan memposting rentang tanpa dokumen tidak meninggalkan riwayat posting kosong.

---

## 7. Sempat ada DUA implementasi — kini satu

Saat modul ini dikerjakan, sesi lain mengerjakan baris yang sama secara paralel dan
pekerjaannya mendarat lebih dulu. Repositori sempat memuat dua-duanya, dengan DUA cabang
`"Jurnal Penyusutan"` pada `modulPosting` dan `jalankanPosting` — yang pertama menang, yang
kedua kode mati.

Keduanya diuji dengan data yang sama:

| | Dokumen terposting | Catatan |
|---|---|---|
| `postingSemuaPenyusutan` | **1 dari 2** | dokumen PERTAMA tiap batch gagal `Session is closed!` |
| `postingSemua` | **2 dari 2** | pembatalannya juga mengembalikan keduanya |

Penyebab kegagalannya persis yang dijelaskan di bagian 1: sesi Hibernate dipegang melintasi
`AssetUtil.ambilDataAkun`. Kriteria jalur yang dihapus juga tidak punya ketiga INNER JOIN,
sehingga ia ikut mengambil baris yang rantai asetnya putus — baris yang akun jurnalnya memang
tidak dapat ditemukan.

**Diputuskan 24 Agustus 2026:** implementasi dua fase yang dipertahankan, jalur satunya dihapus
seluruhnya — 173 baris, mencakup `kriteriaPostingPenyusutanStatic`, `postingSemuaPenyusutan`,
dan `batalkanPostingSemuaPenyusutan`, ditambah dua cabang di `DraftJurnalApiHelper` dan kunci
`penyusutan_aset` yang hanya dipakai di sana. Sebelum menghapus dipastikan lebih dulu bahwa
ketiga metode itu **tidak dipanggil dari mana pun selain kedua cabang tersebut** — layar ZK
memakai jalurnya sendiri (`onPostingSemua`) dan tidak tersentuh.

Yang tersisa: satu kunci (`posting_penyusutan`), satu cabang, satu mesin.

---

## 8. Yang masih terbuka

Delapan belas dari 35 baris kini punya mesin posting; tiga baris lain memang bukan posting
massal. Sisa **14 baris**:

| Baris | Jumlah |
|---|---|
| Mahasiswa | 7 |
| Siswa | 6 |
| Gaji | 1 |

Dan seperti tiga dokumen sebelumnya: **jurnal yang dihasilkan belum pernah diperiksa akuntan.**
Yang terbukti adalah akunnya sesuai rantai yang disalin dari layar dan angkanya dihitung dengan
rumus yang sama — bukan bahwa perlakuan akuntansinya tepat.
