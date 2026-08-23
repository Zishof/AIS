# 20 — Proses Transitori

Jalan **keluar** dari rekening transitori. Dipindahkan dari layar ZK
`ais.action.master.akunting.ProsesTransitoriAction`.

| Bagian | Tempatnya |
|---|---|
| API | `ais/action/servlet/api/ProsesTransitoriApiHelper.java` |
| Kunci menu | `proses_transitori` (fail-closed, hak per-aksi) |
| Layar | `apps/ebisnis/lib/screens/proses_transitori_screen.dart` |
| Perbaikan menyertai | `ProsesTransferApiHelper.proses_transfer_tandai` |

> Catatan penomoran: berkas ini bernomor 20 dan berbagi nomor dengan
> `20-ikhtisar-kantin-multi-toko.md` milik sesi lain. Isinya berbeda dan keduanya berlaku.

---

## 1. Letaknya pada alur

```
Dokumen Keuangan → DPC → Proses Transfer → disetujui
                                   ├── ditandai Transfer   → langsung ke penerima
                                   └── ditandai Transitori → mampir di rekening transitori
                                                              ↓
                                                        Proses Transitori  ← modul ini
                                                              ↓ disetujui
                                                        siap diposting
```

`PostingProsesTransitoriAction` menuntut `prosesTransitori.disetujuiOleh` tidak null. Jadi
tanpa modul ini, setiap baris Keuangan yang ditandai Transitori **tersangkut** — dananya
sudah keluar dari kas/bank tetapi tidak pernah keluar dari rekening transitori, dan tidak
pernah terjurnal.

`pengadaan_transitori_*` yang sudah ada **tidak menutupi** kebutuhan itu; komentarnya
menyatakan sendiri: *"Hanya transitori milik pembayaran pengadaan POS yang ditampilkan.
Transitori dari modul lain … bukan urusan layar ini."*

---

## 2. Cacat yang ditemukan di modul kemarin

Menandai satu baris DPC sebagai **Transitori** di layar ZK tidak hanya menyetel bendera pada
`DaftarPengajuanTransfer`: ia **membuat satu baris `akunting.transitori`** dan menautkannya
(`dpt.transitoriData`), lalu **menghapusnya** lagi bila centangnya dilepas. Baris itulah
yang menjadi kandidat modul ini.

`proses_transfer_tandai` yang saya tulis kemarin **hanya menyetel benderanya**. Akibatnya
baris yang ditandai Transitori dari POS tidak pernah punya catatan transitori dan tidak
akan pernah muncul di mana pun — persis jenis kegagalan diam yang berulang kali muncul di
sesi ini.

Sekarang `selaraskanCatatanTransitori()` menangani keduanya, dan dipanggil dari **empat**
jalur pelepasan (batal setuju, hapus proses transfer, lepas satu baris, dan simpan yang
melepas baris tak terpilih), bukan hanya dari penandaan.

Catatan yang **sudah masuk satu batch** tidak dihapus: dananya sudah diproses keluar, dan
menghapusnya memutus riwayat batch itu.

---

## 3. Penjaga yang tidak ada di layar ZK

Catatan transitori hanya boleh dimasukkan ke batch bila **proses transfernya sudah
direalisasikan**. Memindahkan dana keluar dari rekening transitori sebelum dananya masuk ke
sana tidak punya arti, dan jurnalnya akan mengkredit akun transitori yang saldonya belum
pernah bertambah.

Yang penting: **kandidat yang belum siap tetap DITAMPILKAN beserta alasannya**, tidak
disembunyikan —

> *Proses transfernya sudah disetujui tetapi belum direalisasikan; dananya belum masuk
> rekening transitori.*

Menyembunyikannya akan membuat pengguna mencari sesuatu yang tidak akan pernah muncul.
Menyembunyikan **atas permintaan** tetap boleh: itulah gunanya chip "Hanya yang siap".
Barisnya juga tidak dapat dicentang, dan server menolaknya lagi saat menyimpan dengan
menyebut kodenya satu per satu.

Diperiksa juga: cara pembayaran yang **belum memetakan Akun Transitori** ditandai peringatan
pada detailnya — tanpa akun itu barisnya tidak akan terjurnal. Sambungan langsung ke
[17-master-data-keuangan.md](17-master-data-keuangan.md).

---

## 4. Dua jebakan skema yang tercatat

**Kolom ganda.** `akunting.transitori` punya **dua** kolom bernama mirip:
`daftar_pengajuan_transfer` (peninggalan, selalu kosong) dan
`daftar_pengajuan_transfer_id` — dan entitasnya memetakan ke yang **kedua**
(`@JoinColumn(name = "daftar_pengajuan_transfer_id", unique = true)`). SQL native pertama
saya memakai kolom yang salah, dan harness-nya menyatakan modulnya rusak padahal kodenya
benar di jalur Hibernate. Empat join di helper ini dibetulkan.

**`Transitori.getTransfer()` mengembalikan `true` tanpa syarat** — logika aslinya
dikomentari di entitasnya:

```java
public Boolean getTransfer() {
    transfer = true;
    return transfer;
//  return transfer == null ? false : transfer;
}
```

Karena Hibernate menyimpan nilai getter, kolom `transfer` selalu `true`. Kriteria
`transfer = true` pada `PostingProsesTransitoriAction` karena itu **selalu terpenuhi dan
bukan gerbang yang sesungguhnya** — gerbangnya adalah persetujuan batch. Ini keluarga
jebakan yang sama dengan `DaftarPengajuanTransfer.getTransfer()` di
[18-proses-transfer.md](18-proses-transfer.md) dan `UangMuka.getStatus()` di
[07-temuan-dan-jebakan.md](07-temuan-dan-jebakan.md): **jangan menyimpulkan isi kolom dari
nama atau dari setter-nya.**

---

## 5. Dua tahap, dan apa yang dikunci

| Status | Isi batch | Boleh |
|---|---|---|
| **Draft** | boleh disunting, catatan boleh dilepas | ubah, hapus, setujui |
| **Disetujui** | terkunci | batal setuju |

Batch **kosong tidak boleh disetujui**. Persetujuan **tidak boleh dibatalkan** bila
catatannya sudah dijurnal — postingnya harus dibatalkan lebih dulu dari Draft Jurnal, supaya
buku besar dan dokumennya tidak berbeda pendapat. Menghapus batch **melepaskan** catatannya,
tidak ikut menghapusnya: dananya masih ada di rekening transitori dan tetap perlu jalan
keluar.

---

## 6. Hasil uji

`TesProsesTransitori` membangun rantainya sungguhan lewat API: Uang Muka → DPC → Proses
Transfer → disetujui → satu baris ditandai **Transitori** dan satunya **Transfer** →
direalisasikan → catatannya dikumpulkan ke satu batch → disetujui.

| Uji | Hasil |
|---|---|
| `TesProsesTransitori` | **39 lulus, 0 gagal**, sisa data uji 0 |
| `flutter analyze` | 0 error |
| `flutter test` | **252 lulus** (sebelumnya 247) |

Cakupannya: kelahiran & penghapusan catatan transitori saat ditandai/dilepas, kandidat yang
belum siap beserta alasannya, penolakan simpan selama transfernya belum cair, validasi,
penjumlahan nilai, lepas & pasang ulang, penolakan menyetujui batch kosong, penguncian
setelah disetujui, syarat mesin posting, pembatalan, daftar, dan dasbor.
