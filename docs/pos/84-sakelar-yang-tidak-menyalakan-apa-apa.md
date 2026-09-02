# 84 — Sakelar yang tidak menyalakan apa pun, dan penjaga yang sengaja tidak dibuat

Tanggal: 2026-09-02

Dok. 77 mencatat satu sakelar konfigurasi yang dijanjikan JavaDoc tetapi tidak
pernah dibaca kode mana pun. Pertanyaan yang wajar sesudahnya: **ada berapa lagi?**

Jawaban singkatnya: wilayah POS/kantin **bersih**, dan di luar itu ada 218
kandidat — tetapi angka 218 itu **tidak boleh dijadikan vonis**, dan bagian
terpenting dokumen ini adalah alasannya.

## 1. Kelas cacatnya

Admin membuka layar Konfigurasi, melihat "Aktifkan video conference menggunakan
Jitsi", membaliknya, menyimpan. Tidak terjadi apa-apa. Tidak ada galat, tidak ada
log, tidak ada yang mengeluh — karena tidak ada kode yang membaca kunci itu.

Ini bentuk yang sama dengan dok. 45, 46, 75, 76, 80: **satu sisi menyiapkan,
sisi lain tidak memakai, dan tidak ada yang gagal dengan berisik.** Bedanya, di
sini yang tertipu bukan kode lain, melainkan orang yang memakai sistemnya.

## 2. Empat yang sudah ditelusuri sampai tuntas

| Kunci | Label di layar | Temuan |
|---|---|---|
| `audit_listener_aktif` | "Aktifkan AuditListener untuk mencatat create, edit…" | `AuditListener.java` **2.224 baris**, tidak pernah membaca kunci ini. Listener-nya selalu menyala; sakelarnya hiasan. |
| `aktifkan_video_conference` | "Aktifkan video conference menggunakan Jitsi" | Hanya muncul di `KonfigurasiNewAction`. |
| `api_response_selalu_json` | "Selalu kembalikan response API dalam format JSON" | Hanya muncul di `KonfigurasiNewAction`. |
| `daftar_s1` | "Aktifkan Daftar Mahasiswa Baru" | Konstanta `Konfigurasi.DAFTAR_S1` ada, layarnya merender sakelarnya, tak ada yang membaca. `DAFTAR_S2`, `DAFTAR_S3`, `PMB_HOME_INFO`, `PMB_HOME_SEKRETARIAT` bahkan tidak dirujuk di mana pun. |

Diverifikasi dengan menyisir seluruh `C:\opt\AIS`, bukan hanya pohon sumber:
setiap kecocokan lain ternyata `KonfigurasiNewAction.class` (hasil kompilasi
sumber yang sama) atau riwayat Eclipse. Tidak ada pembaca independen.

Seluruhnya berada di modul eCampus/akademik, bukan POS.

## 3. Wilayah POS: bersih

Dari 1.167 sakelar yang ditawarkan sepuluh layar Konfigurasi, **nol** yang
menyangkut kantin/POS/koperasi tanpa pembaca. Gerbang `kantin_pos_cegah_oversell`
yang dulu bermasalah kini dibaca di empat tempat, dan bentuknya dijaga
`alat/aturan-stok-tiga-nilai.py`.

Penyaring wilayahnya sendiri sempat salah: versi substring menjaring
`init_index_hindari_include_postgresql_lama`, karena `include_postgresql` memuat
`_pos`. Penyaring yang dipakai untuk **mempersempit** tuduhan malah melebarkannya.
Sekarang dicocokkan per token antar-garis-bawah, dan dibuktikan dua arah:
menangkap `kantin_pos_cegah_oversell`, menolak yang postgresql tadi.

## 4. Mengapa penjaganya TIDAK dibuat

Naluri pertama adalah memasang gerbang yang gagal bila muncul sakelar tanpa
pembaca. Naluri itu salah di sini, dan menuliskan sebabnya lebih berharga
daripada alatnya.

**Konfigurasi juga dibaca dengan kunci non-literal.** Di basis kode ini ada 198
bentuk argumen berbeda yang bukan literal pada `bolehKonfigurasi(...)` /
`getKonfigurasi(...)`: `key` (69×), `kunci` (15×), `nama` (10×), dan seterusnya.
Sakelar mana pun bisa saja dibaca lewat salah satu jalur itu. Pemindaian literal
dapat mengatakan *"tidak ada pembaca literal"*; ia **tidak dapat** mengatakan
*"tidak ada pembaca"*.

**Angkanya sendiri goyah.** Dua rumusan yang sama-sama masuk akal memberi hasil
berbeda: pencocokan substring 218, pencocokan himpunan literal 261. Selisih 43
itu adalah ukuran kerapuhan pengukurannya, bukan detail teknis.

Gerbang yang menuduh 218 hal tanpa dapat membuktikan satu pun akan ditolak pada
pemakaian pertama — dan penjaga yang tidak dipercaya sama saja dengan tidak ada
(dok. 83 §3.1). Maka yang dibuat adalah `alat/audit-sakelar-tanpa-pembaca.py`:
**melapor, tidak memvonis**, selalu keluar dengan kode 0, dengan batasnya
tertulis di kepala berkas supaya tidak ada yang memasangnya sebagai gerbang
tanpa membaca dulu.

## 5. Positif palsu yang sudah ditutup

* **Kode yang dikomentari.** `aktifkan_item_penilaian_uas` sempat dituduh padahal
  barisnya diawali `//` sehingga tidak pernah dirender. Komentar dibuang lebih
  dulu (23 kandidat gugur).
* **Pembaca yang memakai nama konstanta.** `Konfigurasi.X` kini dihitung sebagai
  pembaca bagi kunci bernilai X.
* **Berkas layar yang merender sekaligus membaca.** Yang dikecualikan dari korpus
  hanya potongan di dalam panggilan `createRow`, bukan seluruh berkasnya.
* **Korpus terlalu sempit.** Diperluas dari `.java`/`.jsp` ke `.js`, `.xml`,
  `.properties`, dan repositori Flutter — 51 juta karakter tambahan. Angkanya
  **tidak berubah**, dan justru itu yang membuat 218 sisanya layak dipercaya
  sebagai kandidat.

## 6. Untuk pemilik modul eCampus

Daftar lengkap: `python docs/pos/alat/audit-sakelar-tanpa-pembaca.py --semua`.
Kelompok terbesar: `elearning` (27), `hibernate` (22), `dashboard` (20),
`dms` (17), `pmb` (14).

Tiap kandidat menuntut satu keputusan yang tidak dapat diambil dari kode:
**apakah sakelarnya seharusnya bekerja, atau seharusnya dihapus dari layar?**
Keduanya perbaikan yang sah; membiarkannya apa adanya berarti terus menawarkan
kendali yang tidak mengendalikan apa pun.

## 7. Yang dipelajari

**Tidak semua temuan layak menjadi penjaga.** Rangkaian dokumen ini biasanya
berakhir dengan gerbang baru. Kali ini pengukurannya sendiri terbukti tidak cukup
kuat, dan memaksakan gerbang di atasnya hanya akan menghasilkan tuduhan yang tak
dapat dibuktikan. Menyatakan batas sebuah alat adalah bagian dari alat itu.

**Penyaring yang mempersempit bisa melebarkan.** `_pos` cocok dengan
`postgresql`. Kesalahan itu terjadi pada langkah yang justru dimaksudkan untuk
menaikkan ketepatan.
