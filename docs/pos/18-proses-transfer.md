# 18 — Proses Transfer (pencairan DPC)

Mata rantai **terakhir** alur Keuangan. Dipindahkan dari layar ZK
`ais.action.master.akunting.ProsesTransferAction`.

| Bagian | Tempatnya |
|---|---|
| API | `ais/action/servlet/api/ProsesTransferApiHelper.java` |
| Kunci menu | `proses_transfer` (fail-closed, hak per-aksi) |
| Layar | `apps/ebisnis/lib/screens/proses_transfer_screen.dart` |
| Tambalan ZK | `ProsesTransferAction.reload()` — penyaring kategori |

---

## 1. Kenapa modul ini yang berikutnya

Delapan modul Keuangan yang sudah dipindahkan — Uang Muka, LPJ, Kas Besar, LPJ Kas Besar,
Kas Kecil, Penggantian Kas Kecil, Dana Talangan, dan Reimbursement Pegawai — semuanya
bermuara di `DaftarPengajuanTransfer` (DPC) **lalu berhenti**. Pencairannya hanya ada di
layar ZK.

Itu bukan sekadar ketidaknyamanan. Mesin posting menuntut
`daftarPengajuanTransfer.prosesTransfer` tidak null (lihat
`PostingUangMukaAction.kriteriaPostingStatic` dan
`PostingPenggantianKasKecilAction`), sehingga **dokumen yang lahir di POS tidak akan pernah
dapat dijurnal** tanpa seseorang membuka ZK. Modul ini menutup rantainya.

Di basis data UAT, `akunting.proses_transfer` **kosong sama sekali** — tahap pencairan
belum pernah dijalankan di sana.

---

## 2. Empat tahap, dan apa yang dikunci tiap tahap

| Status | Isi dokumen | Tanda per baris | Yang boleh |
|---|---|---|---|
| **Draft** | boleh disunting, baris boleh dilepas | belum boleh diisi | ubah, hapus, setujui |
| **Disetujui** | terkunci | **wajib** diisi | realisasikan, batal setuju |
| **Terealisasi** | terkunci | terkunci | batal realisasi |

Penomorannya memakai `NomorSuratAlurKeuangan.DPC` berikut aturan resetnya, sama dengan
`ProsesTransferAction.generateCode`.

**Membatalkan persetujuan MELEPASKAN barisnya.** Sama dengan layar ZK: tanpa itu, baris DPC
"nyangkut" selamanya di status sudah diajukan dan tidak dapat diproses transfer lain. Hanya
berlaku selama dananya belum cair.

**Membatalkan realisasi hanya boleh oleh pelaksananya.** Aturan ZK dipertahankan — yang
mencatat pencairan dana yang tahu apakah dananya benar-benar batal cair. Admin tetap dapat
menembusnya.

---

## 3. Transfer vs Transitori: penentu akun kredit, bukan label

Tiap baris DPC yang menempel ditandai **Transfer** atau **Transitori**, dan keduanya saling
meniadakan. Tanda itulah yang dibaca mesin posting:

| Tanda | Akun kredit jurnalnya |
|---|---|
| `transitori = true` | `caraPembayaranTransfer.akunTransitori` |
| selain itu | `caraPembayaranTransfer.akun` |

Karena itu **realisasi ditahan** selama masih ada baris yang belum bertanda:

> *N baris belum ditandai Transfer atau Transitori. Tanda itu yang menentukan akun kredit
> jurnalnya, jadi tanpa tanda dokumen sumbernya tidak akan terjurnal.*

Persetujuan juga ditolak bila **Cara Pembayaran Transfer** belum dipilih — tanpa itu tidak
ada akun kredit sama sekali. Dan daftar cara pembayaran menandai yang akunnya belum
dipetakan (`akunLengkap: false`), sambungan langsung ke
[17-master-data-keuangan.md](17-master-data-keuangan.md).

---

## 4. Penyaring kategori: dihitung, bukan daftar putih

Penyaring kategori di layar ZK berpola **daftar putih per kolom sumber**: tiap kotak centang
menambahkan satu `isNotNull(<kolom>)`, dan kolom yang belum punya cabangnya membuat barisnya
**tidak pernah tampil di kombinasi penyaring mana pun**.

Cacat itu sudah pernah terjadi pada Reimbursement Pegawai (ada komentar `FIX:` di sana), dan
**masih berlaku** untuk dua kolom yang dibuat modul-modul terbaru:

- `dana_talangan`
- `pertangungjawaban_kas_besar`

Keduanya adalah muara modul yang saya bangun sendiri, jadi keduanya ditambal:

**Di POS** — kategori **dihitung dari barisnya** lewat satu ekspresi `CASE` yang dibangun
dari tabel `KATEGORI`, dan baris yang tidak cocok dengan kategori mana pun jatuh ke kategori
**`lainnya`**. Layar ini karena itu secara struktural tidak mungkin menyembunyikan satu
baris pun; menambah sumber baru cukup menambah satu baris di tabel itu. Nama kolomnya tidak
pernah datang dari permintaan — hanya kunci kategori yang diterima, lewat daftar putih.

**Di ZK** — kotak "Uang Muka" kini mencakup `danaTalangan`, dan kotak "LPJ" mencakup
`pertangungjawabanKasBesar`. Pola satu-kotak-dua-kolom itu sudah dipakai "Kas Kecil"
(`jenisKasKecil` + `penggantianKasKecil`), jadi tidak perlu mengubah UI maupun keempat
pemanggil `reload()`.

---

## 5. Jebakan: penanda yang tidak bisa di-null-kan

`DaftarPengajuanTransfer.getTransfer()` dan `getTransitori()` adalah **getter terhitung**:

```java
public Boolean getTransfer() {
    if (getProsesTransfer() != null && getProsesTransfer().getRealisasikanOleh() != null
            && !getTransitori()) {
        transfer = true;                 // menyetel dirinya sendiri
    }
    return transfer == null ? false : transfer;   // TIDAK PERNAH null
}
```

Hibernate menyimpan nilai **getter**-nya, jadi `setTransfer(null)` — yang dipakai layar ZK
di jalur pembatalannya — tidak pernah menghasilkan `NULL` di basis data; hasilnya `false`.
Harness saya sempat gagal karena mengharapkan `NULL`; **kodenya benar, harapan sayalah yang
salah**. Di helper ini penandaan dikosongkan dengan `Boolean.FALSE` terang-terangan supaya
kodenya menyatakan apa yang sungguh tersimpan; hasil akhirnya identik dengan ZK.

Pelajaran umumnya sama dengan yang sudah tercatat untuk `UangMuka.getStatus()` dan
`getWorkspace()` di [07-temuan-dan-jebakan.md](07-temuan-dan-jebakan.md): **pada basis kode
ini, getter bisa berisi aturan bisnis — jangan menyimpulkan isi kolom dari setter-nya.**

---

## 6. Hasil uji

`TesProsesTransfer` membangun rantainya **sungguhan lewat API**, bukan memalsukan baris:
Uang Muka dibuat → disetujui → diajukan ke DPC → ditarik ke satu Proses Transfer →
disetujui → ditandai → direalisasikan. Baru setelah transfernya cair, Dana Talangan atasnya
dapat dibuat — dan barisnya dipakai membuktikan kategori `dana_talangan` benar-benar
terlihat.

| Uji | Hasil |
|---|---|
| `TesProsesTransfer` | **55 lulus, 0 gagal**, sisa data uji 0 |
| `flutter analyze` | 0 error |
| `flutter test` | **246 lulus** (sebelumnya 241) |

Cakupannya: opsi & ketiga kategori yang bermasalah, kandidat berikut penyaring kategori
(termasuk kategori asing yang ditolak, bukan disisipkan ke SQL), validasi simpan, penjumlahan
nilai, menambah/melepas baris, penjaga urutan tahap (realisasi sebelum disetujui, penandaan
sebelum disetujui), penguncian setelah disetujui dan setelah realisasi, penahanan realisasi
selama ada baris belum bertanda, pembatalan berurutan berikut pembebasan barisnya, daftar,
dasbor, dan hapus.

---

## 7. Catatan lingkungan

Ditemukan **5 baris `akunting.daftar_pengajuan_transfer` sisa uji sesi lain**
(`UAT-PJK-PPh2`, kode `PAJAK-4/6/8/10/12`, dibuat 21–22 Agustus 2026 oleh
`PengadaanPosApiHelper`). Kolom `pajak`-nya NULL padahal namanya menyebut pajak, sehingga di
layar ZK baris-baris itu tidak tampil di penyaring mana pun. Di layar POS mereka muncul di
kategori **Lainnya** — contoh nyata mengapa jaring pengaman itu perlu. Baris-baris itu
**tidak saya hapus**: bukan data uji saya.
