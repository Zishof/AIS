# Audit Menyeluruh Tombol Posting Layar ZK (40 Layar) + Penutupan Celah Hub

Tanggal: 31 Agustus 2026, pada HEAD r78659. Kode masuk SVN **r78660**
(`PostingPembayaranTerminAction`, mirror `java/` selaras) dan **r78661**
(`transaksi_vendor.zul`, WC web `^/web`). Melengkapi audit per-modul yang tersebar di
dok [54](54-posting-pengembalian-uang-muka.md) §2a,
[56](56-posting-perjanjian-kerjasama.md) §1a, [57 vendor](57-posting-pembayaran-vendor.md) §3,
[57 payroll](57-posting-payroll-pegawai-penggajian.md) §2a, [58](58-posting-saldo-awal-kas-kecil.md) §3,
dan [59](59-posting-kantin-dasbor.md).

## 1. Kenapa sapuan ini

Audit sebelumnya hanya menyentuh ~10 layar yang kebetulan dilewati kampanye mesin
posting (dok 53–59). Karena dari 10 layar itu DUA ternyata cacat — satu menulis jurnal
Dr X / Cr X yang saling meniadakan (r78539), satu lagi punya tombol batal massal yang
mati total karena salah tipe entitas (r78552) — sisa ±30 layar yang tak pernah
diperiksa adalah risiko yang tidak boleh dibiarkan hanya karena modulnya "sudah lama
jalan". Sapuan ini memeriksa **seluruh 40 layar `Posting*Action`** dengan daftar
periksa yang sama.

## 2. Lima kelas cacat yang dicari (dan cara memeriksanya secara mekanis)

| # | Kelas cacat | Preseden | Cara periksa |
|---|---|---|---|
| 1 | Jurnal Dr X / Cr X — sisi kredit diisi variabel akun debet | r78539 | `grep "akunsKredits\.add(akunDebet\|akunKredit\.add(akunDebet"` |
| 2 | SQL batal kehilangan `AND` antar-filter | r78539 | `grep "\"' [a-z_]*="` (pola kutip-spasi-kolom=) |
| 3 | Jenis `PostingHistory` salah tempel antar-jalur | r78551 | bandingkan konstanta `JENIS_*` per berkas; >1 = ditinjau |
| 4 | Dokumen tidak pernah dicap `setPostingHistory*` | r78552 | berkas ber-`onPostingSemua` dengan cap < 2 jalur |
| 5 | Tipe entitas `List<T>` ≠ entitas `initCriteria` | r78552 | entitas di DALAM badan `initCriteria` vs tipe penampungnya |

Catatan penting untuk pemeriksa berikutnya: pola grep naif pada kelas 4 dan 5 penuh
positif palsu. Kelas 4 harus memakai `setPostingHistory[A-Za-z]*(` — banyak modul
memakai setter varian (`setPostingHistoryPengembalian`, `...Dimuka`, `...Denda`,
`...Diskon`, `...PaymentGateway`, `...Kembali`). Kelas 5 harus mengambil
`createCriteria` dari DALAM badan `initCriteria`; `createCriteria` pertama pada berkas
biasanya milik renderer (mencari nomor bukti `GrupTransaksi`) dan menghasilkan 30-an
laporan palsu.

## 3. Hasil: BERSIH pada 40 layar

Kelas 1, 2, 3, dan 4: nihil temuan (satu-satunya kecocokan kelas 1 adalah teks Javadoc
dok 54 yang justru menjelaskan cacat yang sudah diperbaiki). Kelas 5 memunculkan tiga
kandidat, semuanya terbukti sah setelah ditinjau:

- `PostingProsesTransferAction` dan `PostingProsesTransitoriAction` — `List<Long>` atas
  kriteria entitas: sah, keduanya memakai `setProjection(Projections.property("id"))`.
- `PostingPembayaranTerminAction` — `List<PembayaranTerminMasterAsset>` menampung hasil
  `initCriteria` yang memuat `...Detail`. **Tidak pernah meledak**: daftarnya hanya
  diserahkan ke `SimpleListModel` tanpa iterasi ber-cast (erasure), dan renderer-nya
  meng-cast ke `...Detail` dengan benar. Tetap **dibetulkan di r78660** karena satu
  `for`-each yang ditambahkan di situ kelak akan langsung ClassCastException — persis
  cacat r78552 yang menewaskan tombol batal massal Penggajian.

Dengan ini seluruh layar posting ZK sudah teraudit, bukan hanya yang tersentuh kampanye.

## 4. Penutupan celah hub ZK (dok 63 §4)

- **DIKERJAKAN (r78661):** dua tab pembayaran yang tertinggal — `posting_pembayaran.zul`
  (Pembayaran Tagihan Vendor) dan `posting_pembayaran_dp.zul` (Pembayaran DP Vendor) —
  kini di-include pada hub Transaksi Vendor. Layar dan mesinnya sudah lama ada dan
  sekeluarga dengan Termin Vendor yang memang sudah masuk. Tab dan tabpanel ditaruh di
  UJUNG daftar: ZK memasangkan keduanya secara POSISIONAL, jadi menyisipkan di tengah
  akan menggeser pasangan yang sudah ada. Jumlah tab = tabpanel = 8.
- **SENGAJA TIDAK DIKERJAKAN:** menyalakan tab `visible="false"`. Dok 63 mengusulkan
  menyalakan Pertanggungjawaban Kas Besar, tetapi `svn blame` menunjukkan tab itu DAN
  tab DP Pekerjaan Vendor (yang tidak disebut dok 63) disembunyikan **bersama dalam
  satu commit r74892**, 1 Juli 2026. Dua tab disembunyikan sekaligus berarti keputusan
  produk, bukan kelalaian — menyalakannya sepihak akan memunculkan kembali layar yang
  seseorang sengaja tutup. Diserahkan ke pemilik produk; bila memang ingin dinyalakan,
  cukup hapus `visible="false"` pada tab dan tabpanel-nya di
  `uang_muka_dan_kas_kecil.zul` dan `transaksi_vendor.zul`.

## 5. Yang masih terbuka (bukan pekerjaan teknis)

| Butir | Keadaan |
|---|---|
| Dua tab tersembunyi (r74892) | menunggu keputusan produk, lihat §4 |
| Butir **E** dok 61 — Inventory Sales (`NotaSalesKas` dkk.) | menunggu keputusan LINGKUP: tetap buku terpisah dengan konsolidasi manual, atau dibuatkan jembatan posting ke buku besar. Bukan cacat; keluarga NotaSales memang memakai kas & jurnal mini sendiri |
| Kaki KEMBALI modal penyertaan | kodenya sudah ada di HEAD (r78651: baris dasbor "Pengembalian Modal Penyertaan", cap `postingHistoryKembali`, mesin + pembatalan bersaring jenis) dan harness sesi paralel sudah memuat skenarionya; tabel §3/§4 dok 68 masih menyebut "belum" karena ditulis sebelum kodenya masuk — sesi paralel sedang menyelesaikannya, tidak disentuh dari sini |
