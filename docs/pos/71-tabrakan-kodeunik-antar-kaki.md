# Tabrakan `kodeUnik` Antar-Kaki Jurnal: Mekanisme Terbukti, Satu Keluarga Terpapar

Tanggal: 31 Agustus 2026, pada HEAD r78682. **Tidak ada perubahan kode pada revisi ini** —
dokumen ini melaporkan temuan beserta buktinya dan menyerahkan keputusan perbaikan, dengan
alasan yang dijelaskan di §5. Lanjutan sapuan
[70-audit-dokumen-berkaki-ganda.md](70-audit-dokumen-berkaki-ganda.md).

## 1. Mekanismenya

`CommonAkunting.saveTransaksi` memiliki penjaga duplikat: sebelum menulis, ia mencari
`GrupTransaksi` yang `kodeUnik`-nya sama. **Bila ketemu, jurnal baru TIDAK ditulis** — grup
lama hanya dicap ulang dengan posting history yang baru:

```java
GrupTransaksi apakahSudahada = ... .add(Restrictions.eq("kodeUnik", grupTransaksi.getKodeUnik())) ...
if (apakahSudahada != null) {
    apakahSudahada.setPostingHistory(postingHistory);   // <- hanya dicap ulang
    Common.refreshUpdate(session, apakahSudahada);
} else { ... tulis jurnal baru ... }
```

Kuncinya disusun `GrupTransaksi.ambilUnik()` dari **kelas + id dokumen + `ref`** saja.
Kolom `jenis` TIDAK ikut — dan memang tidak bisa ikut, sebab `getJenis()` adalah getter
turunan yang membaca `postingHistory.getJenis()`.

Konsekuensinya: **dua kaki jurnal pada dokumen yang sama, yang sama-sama menulis dengan
`ref` null, berbagi satu kunci.** Kaki kedua tidak pernah masuk buku besar.

## 2. Bukti empiris (harness `TesTabrakanKodeUnik`)

Diukur langsung terhadap `saveTransaksi` di DB UAT — bukan disimpulkan dari pembacaan kode.
Fixture murah memakai `JenisKasKecil` (salah satu entitas dalam `ambilUnik`), jendela
1–10 Juni 2092, prefiks `UATKU-`:

| Skenario | Hasil terukur |
|---|---|
| Panggilan 1 (kaki A, 100.000, ref null) | 1 grup jurnal terbentuk |
| Panggilan 2 (kaki B, 25.000, ref null, dokumen SAMA) | **tetap 1 grup** — tidak ada jurnal baru |
| Nilai di buku besar sesudah dua panggilan | **tetap 100.000** — nilai kaki B tidak pernah tercatat |
| Cap grup lama | **berpindah** ke posting history kaki B |
| Kolom `jenis` grup | ikut berpindah ke jenis riwayat kaki B |
| **KONTROL**: dua panggilan dokumen sama dengan `ref` BERBEDA | 2 grup terpisah terbentuk — `ref` memang pembedanya |

**LULUS 7, GAGAL 0.**

## 3. Siapa yang terpapar, siapa yang aman

| Dokumen ber-kaki ganda | Ada di `ambilUnik`? | `ref` per kaki | Status |
|---|---|---|---|
| **`Tagihan` (siswa)** — piutang, denda, diskon, dibayar dimuka | **ya** | **keempatnya null** (semua memakai overload `saveTransaksi` tanpa ref; keluarga siswa tidak memakai satu pun konstanta `REF_`) | **TERPAPAR** |
| `Pertangungjawaban` — utama, pajak, pengembalian | ya | null / `'pajak'` / `'pengembalian'` | aman |
| `PertangungjawabanKasBesar` | ya | idem (kaki pajak & pengembalian belum dipakai) | aman |
| `CicilanPembayaran` — utama, dibayar dimuka | ya | null / `'dimuka'` | aman |
| `LogPembayaran` — administrasi, payment gateway | **tidak** | keduanya null | aman **secara kebetulan** — lihat §4 |

Pola pengaman yang sudah dipakai rumah ini justru ada di LPJ: kaki utama ber-ref null,
kaki lain diberi ref sendiri. Keluarga siswa tidak pernah melakukannya.

## 4. Peringatan ke depan: `LogPembayaran` aman hanya karena absen dari kunci

Karena `logPembayaran` tidak ada di `ambilUnik`, `kodeUnik`-nya null sehingga pencarian
duplikat tidak pernah menemukan apa pun dan setiap kaki menulis jurnalnya sendiri. Itu
sebabnya cacat pembatalan silang di dok 70 §2.1 bisa terjadi: kedua kaki memang benar-benar
punya jurnal. **Menambahkan `logPembayaran` ke `ambilUnik` kelak — yang tampak seperti
kerapian — akan langsung menciptakan tabrakan pada dua kaki itu.** Bila suatu saat
ditambahkan, kedua kaki wajib diberi `ref` berbeda pada saat yang sama.

## 5. Kenapa tidak diperbaiki sepihak di sini

Dua jalan perbaikan, keduanya menyentuh semantik data akuntansi:

1. **Beri `ref` khas pada tiga kaki siswa** (mengikuti pola LPJ). Terverifikasi tidak
   merusak apa pun yang sekarang ada: SQL pembatalan keempat kaki menyaring `jenis`, bukan
   `ref`; penghitung closing baris-baris itu memanggil `hitungClosing(..., ref = null, ...)`
   dan `restriksiRefClosing(null)` mengembalikan `1=1` alias tanpa filter; tidak ada satu
   pun kueri lain yang menyaring jurnal ber-`tagihan` berdasarkan `ref`.
2. **Masukkan pembeda kaki ke dalam kunci** — perubahan mekanisme sentral yang menyentuh
   seluruh 47 layar dan setiap mesin posting.

Yang menahan saya bukan pilihan desainnya, melainkan **verifikasi**: modul sekolah nyaris
tidak berdata di UAT — `akunting.grup_transaksi` tidak punya satu pun baris ber-`tagihan`,
dan tidak ada satu tagihan pun yang memenuhi kriteria mesin piutang (rantai
`item_biaya_sekolah.akun_piutang_id` + `pengaturan_biaya` + `jenis_biaya_sekolah` tidak
lengkap). Uji ujung-ke-ujung "posting piutang lalu denda pada tagihan yang sama" karena itu
tidak dapat dijalankan tanpa mengarang rantai master baru di basis data uji. Mengubah cara
penulisan jurnal sebuah modul yang tidak bisa saya jalankan sampai tuntas bukan langkah yang
pantas diambil sepihak.

**Rekomendasi:** ambil jalan (1) — beri `ref` khas pada kaki denda, diskon, dan dibayar
dimuka (kaki piutang tetap null sebagai kaki utama, persis pola LPJ) — lalu jalankan ulang
`TesTabrakanKodeUnik` ditambah satu skenario siswa di lingkungan yang datanya lengkap.
Sampai itu dikerjakan, gejala yang harus dicurigai di lapangan: **satu tagihan yang denda
atau diskonnya "sudah diposting" tetapi jurnalnya tidak ada di buku besar, dan jurnal
piutangnya berubah label menjadi jenis denda/diskon.**
