# 22 — Closing (Penutupan Periode)

Penutupan periode akuntansi. Dipindahkan dari layar ZK
`ais.action.master.akunting.ClosingAction`.

| Bagian | Tempatnya |
|---|---|
| API | `ais/action/servlet/api/ClosingApiHelper.java` |
| Kunci menu | `closing` (fail-closed, hak per-aksi) |
| Layar | `apps/ebisnis/lib/screens/closing_screen.dart` |
| Harness | `TesClosing` — **31 lulus, 0 gagal**, sisa data uji 0 |

---

## 1. Kenapa modul ini yang berikutnya

Closing-lah yang **mengunci buku**, dan POS sudah lama terkena akibatnya tanpa bisa
melakukannya:

- Setiap mesin pembatalan posting yang saya tulis menolak baris yang sudah masuk closing —
  `delete akunting.grup_transaksi where <kolom> = id and closing is null`.
- Dasbor Draft Jurnal menampilkan kolom **Closing** per modul, jadi angkanya sudah terlihat
  dari POS.

Artinya pengguna POS sudah membaca angkanya dan sudah dibatasi olehnya, tetapi **menutup
periode hanya bisa dari layar ZK**.

---

## 2. Cara kerjanya

Satu closing adalah **tanggal batas** berikut namanya. Seluruh `akunting.grup_transaksi`
yang tanggal transaksinya pada atau sebelum tanggal itu ditautkan padanya.

**Urutan penautannya penting.** Penautan dihitung ulang dari closing **terbaru ke terlama**;
karena tiap langkah menimpa penautan sebelumnya untuk rentangnya sendiri, tiap jurnal
berakhir pada closing **paling awal** yang mencakupnya — itulah periode yang sebenarnya
menutupnya. Membalik urutannya akan menaruh semua jurnal lama pada closing terbaru.
Harness menguji ini secara langsung: tutup Maret dulu, lalu tutup Januari, dan jurnal
Januari **pindah** ke closing Januari sementara Februari–Maret tetap di closing Maret.

---

## 3. Dua penjaga yang dibawa dari ZK

**Tanggal wajib unik.** Dua closing bertanggal sama membuat penautan jurnalnya tidak
menentu.

**Tidak boleh ada jurnal yang tidak balance** pada atau sebelum tanggal itu. Menutup
periode yang memuat jurnal timpang berarti **mengunci kesalahan** sehingga tidak dapat
diperbaiki lagi. Kode jurnalnya disebut dalam pesannya, bukan sekadar "ada jurnal tidak
balance".

---

## 4. Tiga tambahan yang tidak ada di layar ZK

**Kesiapan diperiksa sebelum Simpan ditekan.** Aksi `closing_periksa` menghitung berapa
jurnal yang akan tertaut dan apakah ada yang timpang, **tanpa menyimpan apa pun**. Tombol
Simpan dimatikan bila sudah jelas akan ditolak. Penolakan yang baru muncul sesudah menekan
tombol membuat orang mengira dirinya salah tekan, bukan salah tanggal.

**Closing yang dikunci tidak dapat diubah maupun dihapus.** Kolom `dikunci` sudah ada di
entitasnya tetapi tidak pernah dipakai; di sini ia menjadi penjaga yang sesungguhnya.

**Menghapus closing melepaskan jurnalnya** (`closing = NULL`) alih-alih meninggalkannya
menunjuk baris yang sudah tiada, lalu menautkan ulang ke closing lain yang masih berlaku.
Pesannya menyebut berapa yang dilepas dan berapa yang ditautkan ulang, dan konfirmasinya
menyatakan akibatnya apa adanya: *"periode itu terbuka kembali dan postingnya dapat
dibatalkan lagi."*

---

## 5. Jebakan: getter yang menulis balik fieldnya

`Closing.getDikunci()` bukan getter biasa:

```java
public Tbmuser getDikunci() {
    dikunci = check(dikunci);   // <-- MENULIS saat dibaca
    return dikunci;
}
```

Saat Hibernate memanggilnya di tengah flush, penulisan itu mengubah konteks persistensi
yang sedang ditelusuri, dan `session.update(closing)` meledak dengan
**`java.util.ConcurrentModificationException`**. Ini bukan dugaan — harness pertama gagal
di sana, dan pesannya kosong (`e.getMessage()` null) sehingga baru terbaca setelah `teknis`
ikut dicetak.

Karena itu kunci/buka **sengaja memakai satu `UPDATE` langsung**, tidak lewat entitasnya.
Kolomnya hanya FK ke `tbmuser`, jadi SQL menyatakan maksudnya dengan tepat tanpa menyentuh
getter itu sama sekali.

Ini keluarga jebakan yang sama dengan `DaftarPengajuanTransfer.getTransfer()`
([18](18-proses-transfer.md)), `Transitori.getTransfer()` ([20](20-proses-transitori.md)),
dan `UangMuka.getStatus()` ([07](07-temuan-dan-jebakan.md)) — tetapi yang ini lebih tajam:
bukan hanya nilainya yang mengejutkan, **membacanya saja sudah punya efek samping.**

---

## 6. Hasil uji

| Uji | Hasil |
|---|---|
| `TesClosing` | **31 lulus, 0 gagal**, sisa data uji 0 |
| `flutter analyze` | 0 error |
| `flutter test` | **284 lulus** (sebelumnya 279) |

Harness membuat empat jurnal uji pada empat bulan berbeda — tiga seimbang, satu sengaja
timpang — lalu menguji: penolakan closing yang memuat jurnal timpang (menyebut kodenya),
penolakan tanggal kembar, penautan ulang saat closing lebih awal dibuat belakangan, isi
satu closing berikut totalnya, kunci/buka, penolakan ubah & hapus saat terkunci, serta
pelepasan jurnal saat closing dihapus.

---

## 7. Yang tidak dikerjakan di sini

Layar ZK punya tombol **ekspor Excel** dan **unggah** pada halaman Closing. Keduanya belum
dipindahkan; menutup periode tidak bergantung padanya.
