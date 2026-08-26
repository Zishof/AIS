# Pengamanan Pembayaran Kedaluwarsa Bankaltimtara

Tanggal: 26 Agustus 2026

## Masalah

Callback pembayaran otomatis Bankaltimtara masih dapat melewati validasi kedaluwarsa. Jalur publik sebelumnya mengaktifkan penanda pemeriksaan ulang dan validasi membandingkan waktu transaksi dari request bank, bukan waktu server ketika callback diterima. Selain itu, proses posting belum selalu berhenti setelah respons validasi berubah menjadi gagal.

Grid Virtual Account dan log Host-to-Host juga menyisakan ruang kosong sehingga data tidak menggunakan lebar layar secara efektif.

## Perbaikan

- Callback pembayaran otomatis menilai kedaluwarsa menggunakan waktu server.
- Pembayaran otomatis yang sudah kedaluwarsa ditolak dengan arahan menggunakan tombol **Cek Pembayaran**.
- Reversal dan pemeriksaan status tidak diperlakukan sebagai penerimaan pembayaran baru.
- Posting pembayaran hanya berjalan apabila seluruh validasi masih menghasilkan `errorCode=00`.
- Tombol **Cek Pembayaran** tetap menggunakan jalur rekonsiliasi manual terautentikasi dan tetap dapat memproses pembayaran yang ditemukan di bank.
- Grid Virtual Account menggunakan seluruh lebar yang tersedia, setiap kolom diberi lebar proporsional, dan kolom tombol diberi label **Aksi**.
- Grid log Host-to-Host menggunakan seluruh lebar yang tersedia; baris spacer kosong di atas dan bawah tabel dihapus.

## Verifikasi

- Kedua pohon sumber Java (`src/main/java` dan `src/main/src`) identik (SHA-256 sama).
- Kedua berkas ZUL berhasil diparsing sebagai XML.
- Kompilasi penuh 7.163 sumber berhasil menggunakan Ant dan pustaka deployment aktual; hasil `BUILD SUCCESSFUL` dengan target kompatibel Java 1.7.
- Pemeriksaan statis memastikan callback publik memanggil `doProcess(..., chekLagi=false, ...)`, sedangkan rekonsiliasi manual tetap melalui `checkPakaiqris`, `checkPakaivaAtauQris`, dan `prosesHasilCekVaBankaltimtara`.
- Uji runtime terhadap callback bank sungguhan tetap harus dilakukan setelah kode dideploy ke Tomcat/staging.
- Uji callback otomatis: VA belum kedaluwarsa diterima; VA kedaluwarsa ditolak tanpa posting.
- Uji manual: tombol **Cek Pembayaran** masih dapat merekonsiliasi pembayaran bank atas VA kedaluwarsa.
