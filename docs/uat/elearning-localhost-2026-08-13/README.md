# UAT Lokal New UI e-Learning — 13 Agustus 2026

Status keseluruhan: **BELUM LULUS** (`21 PASS`, `1 FAIL`).

Pengujian dijalankan pada aplikasi yang benar-benar aktif di `http://localhost:9090/ais`, menggunakan PostgreSQL database `ais` dan sesi pengguna aplikasi aktif. Kredensial tidak dicatat di repository.

## Lingkungan uji

| Komponen | Nilai |
|---|---|
| Sistem operasi | Windows 11 amd64 |
| Java | Temurin JDK 8 `1.8.0_502` |
| Tomcat | Apache Tomcat `9.0.82` |
| PostgreSQL | PostgreSQL `16.4`, `localhost:5432`, database `ais` |
| Context aplikasi | `/ais` |
| New UI | `/ais/new` |
| Tanggal uji | 2026-08-13 (Asia/Jakarta) |

Konfigurasi koneksi lokal disimpan di luar repository. Password database maupun password pengguna aplikasi tidak disertakan dalam dokumen atau artefak Git.

## Ringkasan hasil

| Area | Hasil | Keterangan |
|---|---:|---|
| Bootstrap Tomcat dan koneksi database | PASS | Aplikasi selesai bootstrap dan halaman utama HTTP 200 |
| Login aplikasi | PASS | Sesi pengguna aplikasi aktif berhasil dibentuk |
| Shell New UI | PASS | HTTP 200, HTML New UI ter-render |
| Dashboard e-Learning | PASS | Dashboard frame ter-render |
| Tujuh kartu fungsi e-Learning | PASS | Semua kartu ditemukan dan seluruh target memberi HTTP 200 |
| Linimasa dan pencarian | PASS | UI linimasa, pencarian, filter lanjutan, rentang waktu tersedia |
| Independensi dari ZUL pada HTML linimasa | PASS | Tidak ditemukan referensi `.zul` pada HTML linimasa |
| API feed linimasa yang dipanggil UI | **FAIL** | UI memanggil `/api`, tetapi servlet aktif berada di `/Api` |
| API legacy yang benar | PASS | `POST /Api` mengembalikan JSON `status=00` |

## Matriks UAT

| ID | Skenario | Hasil | Bukti |
|---|---|---:|---|
| AUTH-01 | Login menggunakan akun aplikasi aktif dari database lokal | PASS | Sesi terbentuk; `/new` tidak kembali ke form login |
| SHELL-01 | Buka `/ais/new` | PASS | HTTP 200; respons 425.523 byte dan marker New UI tersedia |
| EL-01 | Buka dashboard e-Learning frame | PASS | HTTP 200 dan judul `Dashboard e-Learning` tersedia |
| EL-FN-01 | Kartu Linimasa Pembelajaran tersedia | PASS | Kartu ditemukan |
| EL-FN-02 | Kartu Materi & Media tersedia | PASS | Kartu ditemukan |
| EL-FN-03 | Kartu Tugas & Penilaian tersedia | PASS | Kartu ditemukan |
| EL-FN-04 | Kartu Ujian & Monitoring tersedia | PASS | Kartu ditemukan |
| EL-FN-05 | Kartu Diskusi & Kolaborasi tersedia | PASS | Kartu ditemukan |
| EL-FN-06 | Kartu Kalender & Presensi tersedia | PASS | Kartu ditemukan |
| EL-FN-07 | Kartu Gradebook & Analitik tersedia | PASS | Kartu ditemukan |
| EL-TL-01 | Render linimasa dan pencarian lokal | PASS | `eltSearch` tersedia pada HTML |
| EL-TL-02 | Render filter lanjutan dan rentang waktu | PASS | `eltAdvancedOpen` dan `eltMonths` tersedia |
| EL-TL-03 | Linimasa tidak mengarah ke ZUL | PASS | Tidak ditemukan `.zul` pada HTML |
| EL-ROUTE-01 | Buka Linimasa Pembelajaran | PASS | HTTP 200, 96.202 byte |
| EL-ROUTE-02 | Buka Materi & Media | PASS | HTTP 200, 46.748 byte |
| EL-ROUTE-03 | Buka Tugas & Penilaian | PASS | HTTP 200, 96.204 byte |
| EL-ROUTE-04 | Buka Ujian & Monitoring | PASS | HTTP 200, 96.194 byte |
| EL-ROUTE-05 | Buka Diskusi & Kolaborasi | PASS | HTTP 200, 46.786 byte |
| EL-ROUTE-06 | Buka Kalender & Presensi | PASS | HTTP 200, 96.221 byte |
| EL-ROUTE-07 | Buka Gradebook & Analitik | PASS | HTTP 200, 43.600 byte |
| EL-API-01 | Muat feed melalui endpoint yang dipanggil UI (`POST /api`) | **FAIL** | HTTP 200 tetapi `Content-Type: text/html`; respons adalah halaman awal, bukan JSON |
| EL-API-02 | Muat feed melalui servlet existing (`POST /Api`) | PASS | JSON `status=00`, `description=Pengambilan data berhasil`, `totalSize=0` |

## Temuan blocker

`WEB-INF/new/_shared/dashboard/elearning_timeline.jsp` membentuk URL API dengan:

```text
request.getContextPath() + "/api"
```

Sedangkan mapping servlet existing pada `WEB-INF/web.xml` adalah:

```text
/Api
```

Pada Tomcat, kedua URL tersebut tidak ekuivalen. Dampaknya, tampilan linimasa berhasil dirender tetapi permintaan datanya tidak menerima JSON. Acceptance UAT e-Learning belum dapat dinyatakan lulus sebelum ketidaksesuaian route ini diperbaiki dan diuji ulang.

Data lokal saat pengujian juga sangat terbatas: `perkuliahan=0`, `pertemuan=2`, dan `tugas_pertemuan=0`. Karena itu respons API yang benar menghasilkan `totalSize=0`; pengujian visual kartu feed dengan data nyata masih memerlukan seed/data kelas lokal.

## Screenshot

Screenshot otomatis belum tersedia karena tidak ada browser Chrome/in-app browser yang terhubung ke runner Codex saat UAT. Aplikasi dan sesi telah diuji melalui HTTP nyata, tetapi gambar tidak dibuat dari mockup atau sumber palsu. Lihat [catatan screenshot](screenshots/README.md).

## Keputusan UAT

**BELUM LULUS.** Lakukan perbaikan route `/api` versus `/Api`, sediakan data kelas lokal representatif, hubungkan Chrome ke Codex, lalu jalankan regression UAT dan tambahkan screenshot desktop/mobile.

