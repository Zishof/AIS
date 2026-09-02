# Koreksi doc 91: dua kesalahan dalam satu temuan

Batch lanjutan sesudah doc 91, dan isinya membatalkan dua pernyataan di dokumen itu.

---

## 1. `^/web` bukan pohon terpisah — itu working copy yang sama

Doc 91 §5 menyatakan repositori punya pohon `web/` sejajar dengan `src/`, dan pohon itu
"tidak ada di working copy ini". **Salah.**

```
$ svn info --show-item url webapp   ->  svn://38.47.178.34/ais/web
$ svn info --show-item url java     ->  svn://38.47.178.34/ais/src
$ svn info --show-item url docs/pos ->  svn://38.47.178.34/ais/docs/pos
```

Nama direktori lokal memang tidak sama dengan jalur repositorinya:

| Lokal | Repositori |
|---|---|
| `ais/src/main/webapp` | `^/web` |
| `ais/src/main/java` | `^/src` |
| `ais/src/main/docs/pos` | `^/docs/pos` |

Jadi ketika `svn log -v` menyebut `M /web/report/Transkrip_Akademik.jrxml`, yang dimaksud
adalah berkas di `src/main/webapp/report/` — direktori yang sedang dibuka, bukan pohon lain.

Kesalahan ini lahir dari membaca jalur repositori seolah jalur lokal. `svn log` selalu
berbicara dalam jalur repositori; satu-satunya cara mengetahui pemetaannya adalah bertanya
kepada `svn info --show-item url`.

## 2. Sumbernya tidak "ditemukan kembali" — itu berkas lain

Doc 91 §3 menyatakan sumber `Transkrip_Akademik` dan `Transkrip_Akademik_subreport0`
ditemukan di `^/web`, sehingga temuan "sumber hilang" batal untuk keduanya. **Juga salah.**

Yang yatim sebenarnya:

```
report/format1/Transkrip_Akademik.jasper
report/surat/Transkrip_Akademik_subreport0.jasper
```

— di **subdirektori**. Yang saya periksa ke repositori adalah
`report/Transkrip_Akademik.jrxml` di tingkat atas: berkas berbeda, di direktori berbeda,
kebetulan bernama sama.

Diperiksa ulang dengan jalur penuh, tidak satu pun dari keempat berkas itu punya `.jrxml`
di jalurnya sendiri.

Ini kekeliruan yang sama yang berulang kali diperingatkan di dokumen-dokumen sebelumnya —
**mencocokkan berdasar nama, bukan jalur** — dan kali ini saya sendiri yang melakukannya,
di dokumen yang justru merayakan ketelitian memeriksa.

## 3. Gambaran yang benar

Tiga belas `.jasper` tanpa `.jrxml` di jalurnya sendiri:

| Keadaan | Jumlah | Arti |
|---|---|---|
| tidak dirujuk siapa pun | 7 | berkas mati |
| ada sumber **bernama sama** di direktori lain | 4 | kemungkinan salinan; dapat dibangkitkan ulang dari sumber itu |
| dirujuk, tanpa sumber senama di mana pun | **2** | inilah yang benar-benar tanpa sumber |

Dua yang terakhir: `report/Daftar_Hadir_guru_Semua_Hari.jasper` (dirujuk 1 berkas) dan
`report/format1/lembar_monitoring_perkuliahanISO.jasper` (1 berkas).

Doc 91 menyebut angka ini **empat**. Yang benar **dua** — dan keduanya jauh lebih kecil
dampaknya daripada `laporan_dosen_pembina_matakuliah` (17 rujukan) yang sempat masuk daftar
itu, padahal sumber bernama sama ada di direktori lain.

Kolom "ada sumber bernama sama di direktori lain" sendiri masih perlu dibuktikan satu per
satu: sumber senama belum tentu sumber yang benar. Itu pekerjaan yang belum dikerjakan, dan
disebut apa adanya di sini alih-alih dihitung sebagai selesai.

## 4. Kenapa dokumen ini ada

Doc 91 ditulis dengan yakin, memakai bukti (`svn log`, `svn info`) yang benar-benar
dijalankan, dan menyimpulkan hal yang salah dari keduanya. Bukti yang sahih tidak menjamin
kesimpulan yang sahih bila pertanyaannya salah — "apakah ada berkas bernama ini?" bukan
pertanyaan yang sama dengan "apakah ada berkas di jalur ini?".

Doc 91 tidak dihapus; ia diberi penunjuk ke dokumen ini. Menghapusnya akan menghilangkan
jejak bahwa kesimpulan yang salah pun terlihat meyakinkan saat ditulis.
