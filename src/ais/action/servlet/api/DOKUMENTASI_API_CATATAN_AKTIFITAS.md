# Dokumentasi API Mobile — Modul Catatan & Aktifitas Harian Siswa

**Sistem:** AIS — Enterprise Education (eCampus / eSchool / ePesantren)
**Tanggal:** 2026-06-22
**Berkas terkait:** `Api.java`, `api/ApiRouteRegistry.java`, `api/CatatanApi.java`, `api/AktifitasHarianSiswaApi.java`

---

## 1. Ikhtisar

Dokumen ini menjelaskan kumpulan endpoint JSON yang disiapkan untuk **aplikasi mobile** guna
mengakses dua kelompok fitur:

1. **Modul Catatan** — fitur pencatatan peristiwa yang menempel pada sebuah subjek. Pada sisi web,
   modul ini diwakili kelas-kelas berpola nama `*Catatan*Action`, antara lain `CatatanSiswaAction`,
   `CatatanGuruAction`, `CatatanKelasSiswaAction`, `CatatanMahasiswaAction`, `CatatanPegawaiAction`,
   dan `CatatanAdministrasiAction`. Implementasi referensi API difokuskan pada **Catatan Siswa**
   karena memuat seluruh karakteristik modul: relasi subjek, jenis catatan, **parameter tambahan
   dinamis**, dan **lampiran**.
2. **Modul Aktifitas Harian Siswa** (`AktiftasHarianSiswa`) — pencatatan kegiatan harian siswa
   beserta dua kanal komunikasi: **pesan pembina** (guru → siswa/orang tua) dan **pesan orang tua**
   (orang tua → sekolah). Di web diwakili `DaftarAktifitasHarianSiswaAction`,
   `DashboardAktifitasHarianSiswaAction`, dan `CatatanOrangTuaAktiftasHarianAction`.

Seluruh endpoint dipanggil melalui satu servlet `Api` (umumnya dipetakan ke `/Api`) dengan metode
`GET`/`POST` dan body JSON. Servlet membaca field `action`, mencari rute yang cocok pada
`ApiRouteRegistry`, lalu mengeksekusi helper terkait. Penambahan endmodul baru cukup dilakukan di
`ApiRouteRegistry.registerCatatan(...)`.

---

## 2. Arsitektur & Alur Permintaan

```
Aplikasi Mobile
      │  HTTP POST /Api   body: { "action": "...", "token": "...", ... }
      ▼
  Api.doGet/doPost ──► Api.ambil()
      │  - ApiAccessGuard.check (rate limit/guard)
      │  - baca field "action"
      ▼
  ApiRouteRegistry (Map<action, ApiRoute>)
      │  route.execute(req, json, perguruanTinggi)
      ▼
  CatatanApi / AktifitasHarianSiswaApi  ──►  Hibernate (baca/tulis)
      ▼
  JSONObject { "status": "...", "description": "...", data/list/... }
```

Identitas pemanggil diperoleh dari **token** via `ApiUtil.currentUser(json, req)` yang
mengembalikan `Tbmuser`. Dari `Tbmuser` diketahui peran: `getSiswa()`, `getGuru()`, `getPegawai()`,
`getMahasiswa()`, `getDosen()`.

---

## 3. Konvensi Respons

| status | arti |
|--------|------|
| `00`   | sukses |
| `97`   | token tidak sesuai / parameter wajib tidak valid |
| `99`   | tidak ada data / entitas tidak ditemukan |
| `500`  | kesalahan internal server |

Muatan data berada pada `data` (objek tunggal) atau `list`/`kelompok` (array). Tanggal memakai
format `yyyy-MM-dd`.

---

## 4. Endpoint Modul Catatan Siswa

Semua endpoint mewajibkan `token`. Subjek `siswa` opsional; bila kosong dan akun login adalah
siswa/orang tua, dipakai siswa milik akun.

### 4.1 `catatan_siswa_jenis` — daftar jenis catatan
Mengembalikan pilihan jenis catatan (dropdown). 
**Request:** `{ "action":"catatan_siswa_jenis", "token":"...", "q":"(opsional)" }`
**Response:** `{ "status":"00", "list":[ {"id":1,"nama":"Pelanggaran","keterangan":"..."}, ... ] }`

### 4.2 `catatan_siswa_parameter` — definisi parameter tambahan (PENTING untuk mobile)
Mengembalikan struktur form dinamis untuk satu jenis catatan. Aplikasi mobile **wajib** memanggil
ini setelah pengguna memilih jenis, lalu merender form berdasarkan metadata.
**Request:** `{ "action":"catatan_siswa_parameter", "token":"...", "jenis":1 }`
**Response:**
```json
{
  "status":"00",
  "kelompok":[
    {
      "id":10, "nama":"Identitas Kejadian",
      "parameter":[
        {
          "key":"10->55", "parameterId":55,
          "label":"Lokasi Kejadian", "keterangan":"Isi lokasi",
          "tipe":"text", "pilihan":"", "nilaiDefault":"",
          "wajib":true, "lampiranWajib":false
        },
        { "key":"10->56", "label":"Foto Bukti", "tipe":"file", "lampiranWajib":true, ... }
      ]
    }
  ]
}
```
Kunci `key` berbentuk `"{kelompokId}->{parameterId}"` dan **harus dikirim balik** saat menyimpan
nilai parameter.

### 4.3 `catatan_siswa_daftar` — daftar catatan (READ)
**Request:** `{ "action":"catatan_siswa_daftar", "token":"...", "siswa":99, "jenis":1, "mulai":"2026-01-01", "sampai":"2026-06-30" }`
**Response:** `list` berisi `{id, nama, keterangan, waktu, oleh, jenis:{id,nama}, parameterTambahanInds}`.
Default rentang: 6 bulan terakhir s.d. hari ini. Akun siswa/orang tua otomatis terkunci ke siswanya.

### 4.4 `catatan_siswa_detail` — rincian + parameter + lampiran
**Request:** `{ "action":"catatan_siswa_detail", "token":"...", "id":123 }`
**Response (`data`):** seluruh field + `siswa:{id,nama,nis}` + `parameterTambahan` (mentah) +
`parameterTambahanInds` (ringkasan) + `lampiran:[ {key,label,nama,url} ]`. URL lampiran berbentuk
`/AmbilLampiran?download=1&ref={idCatatan}&clazz={namaKelas}&jenis={key}`.

### 4.5 `catatan_siswa_simpan` — buat/perbarui (CREATE/UPDATE)
**Request:**
```json
{
  "action":"catatan_siswa_simpan", "token":"...",
  "id":"(opsional; ada=update, tidak=create)",
  "siswa":99, "jenis":1, "nama":"Terlambat", "keterangan":"Datang pukul 08.10",
  "waktu":"2026-06-20", "tahunAjaran":"2025/2026", "semester":1,
  "parameterTambahan":"(string nilai parameter)", "parameterTambahanInds":"(ringkasan)"
}
```
**Response:** `{ "status":"00", "id":123, "description":"Catatan berhasil disimpan" }`. Nilai parameter
tambahan dibangun klien dari definisi `catatan_siswa_parameter` (kunci `"{kelompokId}->{parameterId}"`).

### 4.6 `catatan_siswa_hapus` — hapus (DELETE)
**Request:** `{ "action":"catatan_siswa_hapus", "token":"...", "id":123 }`
**Response:** `{ "status":"00", "description":"Catatan berhasil dihapus" }`.

---

## 5. Endpoint Modul Aktifitas Harian Siswa

### 5.1 `aktifitas_harian_siswa_daftar` — daftar kegiatan harian (orang tua/guru)
**Request:** `{ "action":"aktifitas_harian_siswa_daftar", "token":"...", "siswa":99, "mulai":"2026-06-01", "sampai":"2026-06-30" }`
**Response:** `list` berisi `{id, tanggal, nama, keterangan, aktifitas, materi, pesanPembina,
pesanOrangTua, adaPesanPembina, adaPesanOrangTua}`. Default 30 hari terakhir. Akun siswa/orang tua
otomatis memakai siswanya.

### 5.2 `aktifitas_harian_siswa_detail`
**Request:** `{ "action":"aktifitas_harian_siswa_detail", "token":"...", "id":77 }`
**Response (`data`):** seluruh field + `siswa:{id,nama,nis}`.

### 5.3 `aktifitas_harian_siswa_simpan` — GURU mencatat kegiatan anak didik
**Request:**
```json
{
  "action":"aktifitas_harian_siswa_simpan", "token":"...",
  "id":"(opsional)", "siswa":99, "tanggal":"2026-06-20",
  "nama":"Tahfidz", "keterangan":"Hafalan QS Al-Mulk", "aktifitas":"Setoran",
  "materi":"QS 67:1-10", "pesanPembina":"Bagus, lanjutkan"
}
```
**Response:** `{ "status":"00", "id":77, "description":"Aktifitas harian disimpan" }`.

### 5.4 `aktifitas_harian_siswa_pesan_orang_tua` — ORANG TUA mengisi catatan/tanggapan
**Request:** `{ "action":"aktifitas_harian_siswa_pesan_orang_tua", "token":"...", "id":77, "pesan":"Terima kasih, akan dibimbing di rumah" }`
**Response:** `{ "status":"00", "description":"Pesan orang tua tersimpan" }`. Akun siswa/orang tua
hanya boleh menanggapi aktivitas milik siswanya sendiri (cek kepemilikan).

### 5.5 `aktifitas_harian_siswa_pesan_pembina` — GURU/PEMBINA mengisi pesan
**Request:** `{ "action":"aktifitas_harian_siswa_pesan_pembina", "token":"...", "id":77, "pesan":"Mohon dampingi belajar di rumah" }`
**Response:** `{ "status":"00", "description":"Pesan pembina tersimpan" }`.

---

## 6. Cara Guru & Orang Tua Memasukkan Catatan (ringkasan alur)

**Guru/Pembina:**
1. `aktifitas_harian_siswa_simpan` untuk mencatat kegiatan harian anak didik (sekaligus
   `pesanPembina` bila ingin).
2. Atau `aktifitas_harian_siswa_pesan_pembina` untuk menambah/memperbarui pesan pada entri yang sudah ada.
3. Untuk catatan formal (pelanggaran/prestasi/konseling), gunakan rangkaian Catatan Siswa:
   `catatan_siswa_jenis` → `catatan_siswa_parameter` → `catatan_siswa_simpan`.

**Orang Tua/Siswa:**
1. `aktifitas_harian_siswa_daftar` untuk melihat kegiatan anak.
2. `aktifitas_harian_siswa_pesan_orang_tua` untuk membalas/menanggapi per hari.
3. `catatan_siswa_daftar`/`catatan_siswa_detail` untuk melihat catatan resmi beserta lampiran.

---

## 7. Keamanan

- Semua endpoint memvalidasi token. Tanpa token valid → `97`.
- Akun siswa/orang tua otomatis terkunci ke `Tbmuser.getSiswa()` pada daftar/penyimpanan pesan,
  mencegah akses data siswa lain.
- Pembatasan peran lanjutan (mis. guru hanya boleh mencatat siswa di kelasnya, atau hanya wali
  kelas yang boleh menghapus) dapat ditambahkan pada masing-masing method sesuai kebijakan sekolah;
  titik penyisipannya sudah disiapkan (validasi setelah resolusi `Tbmuser`).
- URL unduh lampiran sebaiknya diakses pada konteks sesi yang sama; bila diperlukan, tambahkan
  token/otorisasi unduh pada servlet `AmbilLampiran`.

---

## 8. Perluasan ke Tipe Catatan Lain

Modul `CatatanGuru`, `CatatanKelasSiswa`, `CatatanMahasiswa`, `CatatanPegawai`, dan
`CatatanAdministrasi` memiliki struktur model yang identik dengan `CatatanSiswa` (subjek + jenis +
`parameterTambahan` + kelompok parameter + `LampiranLain`), hanya berbeda kelas entitas dan kolom
subjek. Untuk menambahkannya:

1. Salin pola enam method `CatatanApi` (jenis/parameter/daftar/detail/simpan/hapus), ganti tipe
   entitas `CatatanSiswa`→`CatatanGuru` (dst.), `JenisCatatanSiswa`→`JenisCatatanGuru`,
   `ParameterTambahanCatatanSiswa`→`ParameterTambahanCatatanGuru`,
   `KelompokParameterTambahanCatatanSiswa`→`KelompokParameterTambahanCatatanGuru`, dan kolom subjek
   (`siswa`→`guru`/`pegawai`/`mahasiswa`/`kelasSiswa`).
2. Daftarkan action baru di `ApiRouteRegistry.registerCatatan(...)` dengan prefiks sesuai
   (mis. `catatan_guru_jenis`, `catatan_guru_simpan`, dst.).
3. Kunci parameter (`"{kelompokId}->{parameterId}"`) dan mekanisme lampiran tetap sama, sehingga
   kompatibel dengan tampilan web.

---

## 9. Catatan Implementasi & Batasan

- Implementasi mengikuti gaya paket `ais.action.servlet.api`: tanpa lambda (kompatibel Java 1.7),
  gagal-aman (selalu mengembalikan JSON), query baca via `HibernateUtil.currentSession()`, tulis via
  `Common.refreshSaveOrUpdate(...)` / hapus via `Common.refreshDelete(...)`.
- **Unggah berkas lampiran** tidak dilakukan melalui endpoint JSON ini (perlu transfer biner/
  multipart). Pola anjuran: simpan catatan → peroleh `id` → unggah berkas ke endpoint berkas khusus
  dengan `ref={id}` dan `jenis={key}`. Endpoint detail/daftar lalu menampilkan lampiran tersebut.
- Verifikasi kompilasi: `javac --release 8` EXIT=0 untuk `CatatanApi`, `AktifitasHarianSiswaApi`,
  dan `ApiRouteRegistry`.
