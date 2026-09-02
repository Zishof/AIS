# Kanal ZUL tanpa gerbang

Batch lanjutan sesudah doc 87.

---

## 1. Kanal keempat yang tidak pernah diperiksa

Empat gerbang sebelumnya menutup `.java` (yang berubah dan seluruhnya),
terjemahan JSP, dan scriptlet JSP. Yang tersisa: **1.557 berkas `.zul`**.

Halaman ZK memuat kelas pengendalinya lewat atribut `apply="..."` dan `use="..."`.
Kelas itu baru dicari ZK **ketika halamannya dibuka**. Kalau kelasnya berpindah paket atau
dihapus, tidak ada satu pun langkah sebelum itu yang menyadarinya — persis lubang yang sama
dengan scriptlet JSP di doc 84, pada kanal yang berbeda.

## 2. Alatnya memeriksa terhadap SUMBER, bukan pohon kelas

[alat/zul-rujukan-kelas.py](alat/zul-rujukan-kelas.py) menarik tiap rujukan `ais.*` dari
berkas `.zul` lalu memastikan berkas `.java`-nya ada.

Memeriksa terhadap **berkas sumber**, bukan pohon kelas hasil kompilasi, adalah keputusan
sadar. Doc 87 menunjukkan pohon kelas di repositori ini basi dalam hitungan menit dan
menghasilkan hantu; berkas `.java` selalu mutakhir. Pemeriksaan ini karena itu tidak punya
kelemahan tersebut sama sekali.

```
berkas .zul       : 1557
rujukan ais.* unik: 1452
menggantung       : 15
```

## 3. Dua diperbaiki (r83259)

| Halaman | Ditunjuk | Sebenarnya |
|---|---|---|
| `pages/master/jenis_biaya.zul` | `ais.action.master.JenisBiayaAction` | `…master.sirs.JenisBiayaAction` |
| `pages/master/sirs/status_pegawai.zul` | `ais.action.master.sirs.StatusPegawaiAction` | `…master.StatusPegawaiAction` |

Keduanya berpindah paket **ke arah yang berlawanan** — satu masuk ke `sirs`, satu keluar
darinya. Sebelum menyunting, kedua nama dipastikan **unik di kedua pohon sumber** (`java/`
dan `src/`), sehingga tidak ada kemungkinan menunjuk kelas bernama sama yang sebenarnya
berbeda. Itu pemeriksaan yang perlu: nama seperti `JenisBiayaAction` justru jenis nama yang
lazim dipakai ulang di modul berbeda.

## 4. Tiga belas halaman yang kelasnya lenyap

Sisanya menunjuk kelas yang tidak ada di mana pun. Untuk menajamkannya, tiap halaman
diperiksa apakah namanya masih disebut berkas lain:

| Keadaan | Jumlah | Halaman |
|---|---|---|
| **masih dirujuk** | 8 | `backup_log`, `jadwal_perkuliahan_paralel`, `jenis_pertemuan`, `perkuliahan`, `mahasiswa_konversi`, `mahasiswa_update_nilai_konversi`, `pendaftaranPMDK`, `pascasarjana` |
| yatim | 5 | `blank_pmb`, `detil_tampilan_pengumuman_akademis`, `reply_pengumuman_akademis`, `pendaftaran_sidang_mahasiswa`, `mhs_pasca` |

Delapan yang pertama lebih penting: masih ada jalan menuju ke sana, jadi membukanya
menghasilkan halaman galat. Lima yang yatim hanya beban mati.

**Batas pengukuran ini perlu disebut**: "masih dirujuk" dihitung dari kemunculan nama
berkasnya di berkas lain, bukan dari analisis tautan yang sesungguhnya. `perkuliahan`
mencatat 1.196 kemunculan — jelas karena katanya lazim, bukan karena ada 1.196 tautan.
Angkanya penunjuk arah, bukan bukti.

Memperbaikinya menuntut keputusan yang sama dengan tiga halaman generator CRUD di doc 86:
apakah modulnya memang sudah dibongkar dan halamannya ikut dibuang, atau kelasnya harus
dihidupkan kembali. Itu milik pemilik sistem.

## 5. Lima gerbang sekarang

| Alat | Kanal | Diperiksa terhadap |
|---|---|---|
| `kompilasi-berubah.sh` | `.java` yang berubah | sumber |
| `kompilasi-penuh.sh` | seluruh `.java` | sumber |
| `jsp-terjemah.ps1` | sintaks JSP | — |
| `jsp-scriptlet.ps1` | Java di dalam JSP | pohon kelas (rawan basi) |
| `zul-rujukan-kelas.py` | rujukan kelas di ZUL | **sumber** |

Hanya satu yang bergantung pada pohon kelas, dan itulah satu-satunya yang pernah
menghasilkan hantu. Pola yang layak diingat saat menambah pemeriksa berikutnya: **periksa
terhadap sesuatu yang tidak bisa basi.**
