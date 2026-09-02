# Analisis Cerdas Tagihan Daftar Ulang

## Tujuan

Dokumen ini menjelaskan mesin analisis tagihan yang digunakan oleh seluruh keluarga
DaftarUlangMahasiswa*Action. Pada saat dokumen ini ditulis, keluarga tersebut terdiri dari
DaftarUlangMahasiswaLamaAction untuk mahasiswa yang sudah mempunyai NIM dan
DaftarUlangMahasiswaBaruAction untuk calon mahasiswa/mahasiswa baru. Keduanya mewarisi
AbstractDaftarUlangMahasiswaAction, tetapi tetap mempunyai query audit sendiri karena objek
orang, atribut PMB, dan sebagian jalur pembentukan tagihannya berbeda.

Tujuan fitur bukan hanya memberi tahu bahwa tagihan kosong. Fitur harus menjawab empat
pertanyaan end user: apa yang sedang terjadi, mengapa sistem mengambil keputusan tersebut, apa
dampaknya terhadap pembayaran, dan tindakan apa yang aman dilakukan. Penjelasan teknis lengkap
tetap tersedia untuk admin di bagian bawah popup, tetapi jawaban operasional ditempatkan paling
atas agar pengguna tidak perlu menafsirkan tabel query.

## Arsitektur

Audit teknis tetap berada di masing-masing action. Audit tersebut menguji SettingBiaya dengan
dua arah. Pertama, kriteria produksi dimasukkan berurutan sehingga tahap pertama yang membuat
jumlah kandidat menjadi nol dapat ditemukan. Kedua, setiap kriteria dilewati satu per satu. Jika
melewati satu kriteria membuat kandidat muncul kembali, kriteria itu terbukti sebagai penghambat,
bukan sekadar dugaan. Pola yang sama dilanjutkan pada template DetailBiaya.

Hasil audit dinormalisasi ke DaftarUlangTagihanAnalisisHelper.Data. Helper ini tidak membuka
session Hibernate, tidak mengubah database, tidak membuat tagihan, dan tidak bergantung pada
komponen ZK. Ia hanya menerima fakta dan mengklasifikasikannya. Karena itu aturan keputusan
mahasiswa lama dan mahasiswa baru selalu sama. Action hanya bertanggung jawab menyediakan data
yang benar dan merender tabel teknis spesifiknya.

Fakta utama yang diberikan kepada helper adalah identitas, status akademik, jenis pembayaran,
semester, jumlah Setting umum dan khusus, jumlah Item Biaya aktif, jumlah konfigurasi bulanan,
mode sumber, jumlah template akhir, hasil query produksi, jumlah Kegiatan, jumlah
CicilanPembayaran, jumlah baris layar, nominal bruto sumber layar, dan total pembayaran
committed.

## Sumber Nominal dan Pembayaran

nominalTagihanTampil dihitung dari koleksi sumber yang dipakai layar. Untuk
PengaturanPembayaranBulanan, nilai berasal dari getNominal(). Untuk DetailBiaya, nilai
modifikasi getNilaiBiayaBaru() dipakai bila tersedia; jika tidak, nilai berasal dari
getNilaiBiaya(). Angka ini adalah nominal bruto sumber layar, bukan jaminan nilai settlement
akhir. Diskon, denda, pengecualian, atau alokasi pembayaran dapat mengubah hasil akhir.

nilaiDibayarCommitted wajib berasal dari agregat CicilanPembayaran yang sudah tersimpan di
database untuk orang, jenis kegiatan, dan semester yang sama. Nilai pada textbox yang baru
diketik tidak boleh dianggap pembayaran. Pemisahan ini penting: analisis tidak boleh menyatakan
tagihan lunas hanya karena operator sedang mengetik nominal yang belum disimpan.

Selisih nominal sumber dan pembayaran disebut "estimasi sisa". Kata estimasi disengaja karena
popup merupakan alat diagnosis, bukan dokumen settlement. Bila pembayaran lebih besar dari
nominal sumber, helper tidak menyimpulkan kelebihan bayar secara mutlak. Ia mengarahkan admin
untuk memeriksa History, alokasi lintas item, perubahan nominal setelah transaksi, atau transaksi
berlebih.

## Urutan Keputusan

Urutan keputusan bersifat kontrak. Jangan memindahkan pemeriksaan hilir ke atas pemeriksaan hulu
tanpa memahami akibatnya.

1. SETTING_TIDAK_COCOK: tidak ada Setting umum maupun khusus yang lolos. Item dan tagihan
   mustahil terbentuk, sehingga kegagalan layar hanyalah akibat.
2. ITEM_BIAYA_KOSONG: Setting cocok, tetapi tidak ada Item Biaya aktif yang berlaku pada
   semester tersebut.
3. BILLING_BELUM_DIBUAT: semua kandidat memakai gunakanBiayaDefault=false, Item Biaya ada,
   tetapi tidak ada konfigurasi billing/bulanan dan layar masih kosong. Solusi diarahkan ke
   Setting Biaya > Action > Buat Billing.
4. TEMPLATE_TIDAK_COCOK: sumber dan item ada, tetapi tidak ada DetailBiaya yang lolos
   kriteria produksi. Pengguna diminta membaca baris PENYEBAB TERBUKTI.
5. NOMINAL_NOL: baris layar ada, tetapi jumlah nominalnya nol.
6. PEMBAYARAN_MELEBIHI_TAGIHAN: pembayaran committed lebih besar daripada nominal sumber.
   Ini perlu pemeriksaan, bukan penghapusan transaksi otomatis.
7. LUNAS_MASIH_TAMPIL: baris masih terlihat dan pembayaran sudah menutup nominal. Biasanya
   cukup memeriksa History dan memuat ulang layar.
8. PEMBAYARAN_SEBAGIAN: tagihan valid, ada pembayaran, dan masih ada sisa. Ini kondisi normal.
9. BELUM_DIBAYAR: tagihan valid dan belum mempunyai cicilan committed. Ini juga kondisi normal.
10. TAGIHAN_TERBAYAR_TIDAK_TAMPIL: layar kosong tetapi transaksi sudah ada. Tagihan lunas
    memang dapat disembunyikan, sehingga History menjadi sumber verifikasi utama.
11. LAYAR_BELUM_SINKRON: query produksi menemukan sumber, tetapi layar kosong dan belum ada
    transaksi. Pengguna cukup memeriksa filter, Refresh, dan Proses Tagihan; konfigurasi tidak
    perlu dibuat ulang.
12. QUERY_PRODUKSI_KOSONG: template ada, tetapi query produksi tidak menghasilkan baris.
13. BELUM_TERGENERASI: sumber hulu tersedia, tetapi belum ada baris maupun transaksi. Pengguna
    diarahkan melakukan Refresh lalu Proses Tagihan.

Prioritas ini mencegah pesan yang salah. Contohnya, layar kosong dengan cicilan committed tidak
boleh langsung diberi pesan "buat Setting Biaya". Demikian pula pembayaran sebagian tidak boleh
dianggap error hanya karena status tagihan belum lunas.

## Mode Default dan Billing

Setiap kandidat Setting dihitung berdasarkan getGunakanBiayaDefault(). Mode default berarti
nominal utama berasal dari konfigurasi Item Biaya dan tagihan bersifat sekali tagih untuk
semester, walaupun pembayaran masih boleh dilakukan bertahap. Mode Billing berarti jadwal
bulanan/angsuran harus tersedia. Jika kandidat bercampur antara default dan Billing, popup
memberi catatan bahwa konfigurasi mungkin tumpang tindih dan selector produksi akan menentukan
prioritas. Catatan ini bukan bukti error, tetapi admin perlu memastikan tumpang tindih tersebut
memang disengaja.

Setting khusus mahasiswa selalu diperhitungkan. Jumlah kandidat umum dan khusus ditampilkan
terpisah agar pengguna memahami sumber konfigurasi yang terpilih. Data mahasiswa atau data PMB
tidak boleh diubah hanya agar cocok dengan Setting. Jika data orang sudah benar, solusi yang aman
adalah membuat varian Setting Biaya yang sesuai.

## Contoh Pembayaran Sebagian

Untuk mahasiswa dengan tagihan SPP Rp10.000, satu cicilan committed Rp2.000, satu baris layar,
satu Setting cocok, satu Item Biaya aktif, dan satu template akhir, hasil yang benar adalah
PEMBAYARAN_SEBAGIAN. Popup menjelaskan bahwa tagihan valid, pembayaran sudah dikenali, estimasi
sisa Rp8.000, dan tagihan tetap tampil karena belum lunas. Solusinya adalah melanjutkan
pembayaran sesuai sisa atau tahap jatuh tempo. Tidak perlu membuat Setting baru dan tidak perlu
mengubah status mahasiswa.

## Tingkat Keyakinan

Diagnosis diberi tingkat keyakinan. Keyakinan tinggi dipakai ketika terdapat baris layar dan
angka transaksi yang dapat dibandingkan, atau ketika eliminasi konfigurasi membuktikan bahwa
Setting/Item tidak tersedia. Keyakinan sedang dipakai saat sumber hulu ada tetapi layar kosong,
karena keadaan tersebut masih dapat dipengaruhi status lunas, filter layar, parameter dinamis,
atau proses pembentukan yang belum dijalankan. Tingkat keyakinan membantu end user membedakan
kesimpulan langsung dari keadaan yang memerlukan konfirmasi.

## Panduan Perawatan

Perubahan aturan yang berlaku untuk kedua halaman harus ditambahkan ke
DaftarUlangTagihanAnalisisHelper, bukan disalin ke dua popup. Query khusus entitas tetap berada
di action masing-masing. Bila action keluarga baru ditambahkan, action tersebut harus mengisi
kontrak Data, memakai hitungNominalTagihanTampil, memanggil hitungModeSetting, lalu
menempatkan htmlRingkasan sebelum bukti teknis.

Setiap kondisi baru harus mempunyai kode stabil, judul nonteknis, penjelasan apa yang terjadi,
dampak, langkah aman, dan test. Jalankan DaftarUlangTagihanAnalisisHelperSelfTest dengan
assertion aktif. Minimal pertahankan test untuk pembayaran sebagian, Setting tidak cocok,
Billing belum dibuat, dan transaksi yang membuat tagihan tidak tampil. Setelah itu kompilasi
kedua action dengan JDK 8 karena aplikasi masih memakai API dan dependensi lama.

Popup tidak boleh menyembunyikan tabel teknis. Ringkasan membantu operator, sedangkan tabel
berurutan dan uji "Jika dilewati" dibutuhkan admin untuk membuktikan penyebab. Susunan yang benar
adalah identitas, ringkasan keputusan, langkah, bukti utama, catatan, lalu bukti teknis lengkap.
Dengan urutan ini satu popup melayani end user dan teknisi tanpa mengorbankan keterlacakan.
