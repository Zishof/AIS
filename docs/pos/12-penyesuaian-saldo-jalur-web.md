# 12 — Penyesuaian Saldo: padanan web dari tombol POS

Opname saldo voucher/deposit anggota sudah ada di POS Desktop/Android. Dokumen ini mencatat
padanannya di **aplikasi web (JSP)**, memakai mesin yang sama persis.

| | |
|---|---|
| Mesin (dipakai kedua kanal) | `ais/action/servlet/api/PenyesuaianSaldoHelper.java` |
| Jalur web | `ais/action/servlet/Data.java` (dispatch tiga aksi), `webapp/WEB-INF/baru/modul/kantin/member/_manajemen_topup.jsp` |
| Aksi | `penyesuaian_saldo_cek`, `penyesuaian_saldo_simpan`, `penyesuaian_saldo_list` |
| Revisi | r77880 |

---

## 1. Kenapa ditumpangkan pada halaman Manajemen Saldo, bukan halaman baru

Halaman JSP dijangkau lewat baris menu di basis data. Halaman baru berarti tidak bisa dibuka
siapa pun sampai baris menunya ditambahkan. Selain itu petugas yang membetulkan saldo adalah
petugas yang sama dengan yang mengisi topup, dan keduanya perlu melihat riwayat mutasi yang
sama sebelum memutuskan — jadi tombolnya diletakkan di **Manajemen Saldo (Deposit)**, di
samping "Tambah Topup", di dalam blok `bolehEntryTopup` yang sudah ada.

## 2. Mesinnya satu

`Data.java` menyalurkan ketiga aksi ke `PenyesuaianSaldoHelper` — kelas yang sama yang dipakai
PosApi. Termasuk di dalamnya:

- gerbang hak akses `Tbmrole.bolehEntryTopup`;
- **pembacaan ulang saldo sistem di server** saat menyimpan, sehingga angka yang sempat basi di
  layar tidak pernah tertulis sebagai koreksi;
- penulisan **satu mutasi koreksi** senilai selisih, dalam satu transaksi.

Kalau aturannya ditulis ulang di JSP, dua kanal akan berbeda persis pada hal yang paling perlu
konsisten: siapa yang boleh membetulkan saldo orang lain.

## 3. Bentuk layarnya

Modal berisi: penjelasan bahwa saldo **tidak ditimpa** (yang dibuat satu mutasi koreksi),
pencarian member, kartu saldo sistem, isian saldo seharusnya, selisih yang dihitung otomatis,
alasan wajib, dan tabel sepuluh penyesuaian terakhir.

Dua hal teknis yang layak diingat:

- **Nama member dan alasan dilolosi** (`escHtmlPenyesuaian`) sebelum masuk `innerHTML`. Satu
  tanda kutip pada nama sudah cukup merusak markup daftar hasil pencarian.
- **Pemilihan member memakai event listener**, bukan atribut `onclick` inline — atribut inline
  patah begitu nama mengandung tanda kutip.

## 4. Hasil uji (basis data UAT lokal)

Harness memanggil helper lewat ketiga aksi, seperti yang dilakukan `Data.java`:

| Yang diuji | Hasil |
|---|---|
| `penyesuaian_saldo_cek` membaca saldo sistem | LULUS |
| Peran tanpa `bolehEntryTopup` ditolak | LULUS — dengan kalimat hak akses yang sebenarnya |
| Penolakan tidak menulis data | LULUS — jumlah baris tidak berubah |
| Peran berhak dapat menyimpan | LULUS — *"Saldo ... disesuaikan dari 0 menjadi 25.000 (selisih 25.000)"* |
| Saldo terbaca = saldo seharusnya | LULUS — 25.000 = 25.000 |
| Riwayat memuat penyesuaian baru | LULUS |

Sintaks JavaScript JSP diperiksa dengan `node --check` setelah ekspresi JSP disubstitusi.

**Catatan pembersihan**: skrip uji sempat gagal menghapus data ujinya karena menghapus baris
`deposit` lebih dulu, padahal `koperasi.penyesuaian_saldo_anggota` menunjuk ke sana lewat
foreign key. Urutan yang benar: hapus baris penyesuaian dulu, baru deposit-nya. Sisa data uji
sudah dibersihkan dan diverifikasi nol.

## 5. Catatan lingkungan

Pada level log **DEBUG**, flush di `PenyesuaianSaldoHelper.simpan` memicu
`ConcurrentModificationException` di dalam pencetak debug Hibernate
(`org.hibernate.pretty.Printer`). Pada level produksi (`log4j.rootLogger=warn`) jalur itu tidak
dijalankan dan uji di level itu lolos — jadi bukan penghambat, tetapi jangan kaget bila muncul
saat seseorang menyalakan debug Hibernate.
