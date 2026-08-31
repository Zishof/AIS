# Pembuat panduan ilustrasi laporan keuangan

Menghasilkan `../70-panduan-laporan-keuangan-an-nahl.pdf`.

Panduan itu memakai **ilustrasi tata letak**, bukan tangkapan layar. Alasannya bukan sekadar
selera: aplikasi Flutter di sesi Remote Desktop hanya menerima klik sintetis bila klien RDP
benar-benar terhubung dan menampilkan desktop, sehingga penelusuran otomatis untuk memotret tiap
laporan tidak dapat diandalkan. Ilustrasi juga punya kelebihan yang tidak dimiliki tangkapan
layar: ia tetap sah ketika data contohnya kosong, dan tidak membocorkan data nyata.

**Nama kolom tiap tabel diambil apa adanya dari definisi laporan di server**
(`LaporanKantinUtil` / `LaporanKatalogData`), bukan dikarang. Kalau kolom sebuah laporan berubah
di server, perbarui pula maketnya di sini supaya panduan tidak menyesatkan.

## Menjalankan

```
python susun_panduan.py
```

Butuh PyMuPDF (`import fitz`). Berkas PDF ditulis ke folder induk.

- `ilustrasi.py` — mesin gambar: sampul, judul bagian, teks, kotak catatan, dan `maket()`
  yang menggambar satu layar laporan (bilah menu, jalur menu, panel filter, tabel).
- `susun_panduan.py` — isi panduannya: tujuh kebutuhan keuangan beserta kolom dan angka contoh.

## Jebakan yang sudah ditemukan

`insert_textbox` PyMuPDF mengembalikan nilai negatif dan **tidak menggambar apa pun** bila kotak
teksnya lebih pendek dari kebutuhan fontnya. Pada tinggi sel 12,5pt, ukuran font di atas 6,6pt
membuat seluruh tabel keluar kosong tanpa pesan galat. Karena itu tinggi baris dan ukuran font
pada `maket()` terikat satu sama lain — jangan naikkan salah satunya sendirian.
