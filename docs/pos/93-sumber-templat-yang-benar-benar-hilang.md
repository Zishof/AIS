# Enam templat laporan yang benar-benar tanpa sumber

Batch lanjutan sesudah doc 92, menuntaskan satu hal yang dokumen itu sebut belum dibuktikan.

---

## 1. Yang ditinggalkan doc 92

Doc 92 membagi tiga belas `.jasper` tanpa `.jrxml` menjadi tiga kelompok, dan menyebut satu
kolomnya masih perlu dibuktikan:

> Kolom "ada sumber bernama sama di direktori lain" sendiri masih perlu dibuktikan satu per
> satu: sumber senama belum tentu sumber yang benar.

Empat berkas ada di kolom itu. Batch ini membuktikannya.

## 2. Semuanya punya kembaran bernama sama — dan kembarannya berbeda

| `.jasper` tanpa sumber | Kembaran bernama sama yang punya `.jrxml` |
|---|---|
| `report/format1/Transkrip_Akademik` | `report/Transkrip_Akademik`, `report/surat/Transkrip_Akademik` |
| `report/laporan_data_pasien_periode` | `report/sirs/laporan_data_pasien_periode` |
| `report/laporan_dosen_pembina_matakuliah` | `report/format1/laporan_dosen_pembina_matakuliah` |
| `report/surat/Transkrip_Akademik_subreport0` | `report/Transkrip_Akademik_subreport0` |

Perbandingan byte langsung memberi "berbeda" untuk kelimanya — tetapi **itu bukan bukti**.
Berkas `.jasper` menyimpan cap waktu dan info kompilator, sehingga mengompilasi ulang sumber
yang sama pun menghasilkan byte yang berbeda. Byte berbeda tidak berarti rancangan berbeda.

Yang menjawabnya: membandingkan **identifier yang tertanam** di dalamnya — nama field,
parameter, variabel.

```
format1/Transkrip_Akademik          vs Transkrip_Akademik            irisan 324/649 (50%)
laporan_data_pasien_periode         vs sirs/laporan_data_pasien_...  irisan 317/571 (56%)
laporan_dosen_pembina_matakuliah    vs format1/laporan_dosen_...     irisan 386/499 (77%)
surat/Transkrip_Akademik_subreport0 vs Transkrip_Akademik_subrep...  irisan 298/441 (68%)
```

Lima puluh sampai tujuh puluh tujuh persen: **berkerabat, tetapi bukan salinan.** Keduanya
varian laporan yang sama untuk keperluan berbeda — `format1/` tampaknya format satu
institusi, `sirs/` untuk rumah sakit. Memakai `.jrxml` yang satu sebagai sumber yang lain
akan mengubah laporannya, diam-diam, dan hanya ketahuan dari hasil cetaknya.

Metodenya dijadikan alat: [alat/jasper-sidik-rancangan.py](alat/jasper-sidik-rancangan.py).
Ia menyingkirkan yang jelas bukan pasangan; ia **tidak** membuktikan yang mana pasangannya.

## 3. Gambaran akhir: enam, bukan dua, bukan empat

| Doc | Menyebut | Sebabnya |
|---|---|---|
| 91 | 4 | mencocokkan nama lintas direktori, dan mengira `^/web` pohon lain |
| 92 | 2 | jalur penuh dipakai, tetapi kembaran senama diandaikan bisa jadi sumber |
| **93** | **6** | kembaran senama dibuktikan **bukan** sumbernya |

Enam templat yang dipakai dan tidak punya sumber di mana pun:

| Templat | Disebut di |
|---|---|
| `report/format1/Transkrip_Akademik.jasper` | 30 berkas |
| `report/laporan_dosen_pembina_matakuliah.jasper` | 17 berkas |
| `report/surat/Transkrip_Akademik_subreport0.jasper` | 15 berkas |
| `report/laporan_data_pasien_periode.jasper` | 3 berkas |
| `report/Daftar_Hadir_guru_Semua_Hari.jasper` | 1 berkas |
| `report/format1/lembar_monitoring_perkuliahanISO.jasper` | 1 berkas |

**Batas angka rujukan itu**: dihitung dari kemunculan NAMA berkas, sedangkan beberapa nama
dipakai oleh lebih dari satu templat di direktori berbeda. Rujukan "Transkrip_Akademik" bisa
saja menunjuk salinan tingkat atas yang punya sumber, bukan yang di `format1/`. Angkanya
menunjukkan nama itu banyak dipakai — bukan bahwa berkas inilah yang dipakai.

## 4. Artinya apa, dan tidak artinya apa

Keenamnya **bekerja hari ini**. `.jasper` adalah bentuk terkompilasi yang siap dipakai, dan
tidak ada yang rusak.

Yang hilang adalah kemampuan mengubahnya: tata letak, kolom, kop surat, apa pun. Dan bila
suatu saat JasperReports dinaikkan versinya sampai format lama tidak terbaca, keenam laporan
itu tidak dapat dikompilasi ulang — harus dibangun dari nol dengan menebak dari hasil
cetaknya.

Itu risiko yang tidak mendesak dan tidak dapat diperbaiki dengan menyunting berkas. Yang
bisa dilakukan sekarang hanyalah mencatatnya, dan tiga dokumen berturut-turut diperlukan
untuk sampai pada angka yang benar.

## 5. Tiga kali salah pada satu pertanyaan

Pertanyaannya sederhana: berkas mana yang kehilangan sumbernya. Jawabannya salah dua kali
sebelum benar.

| Kesalahan | Penyebabnya |
|---|---|
| menganggap `^/web` pohon terpisah | membaca jalur repositori sebagai jalur lokal |
| mencocokkan berkas lintas direktori | membandingkan nama, bukan jalur |
| mengira kembaran senama bisa jadi sumber | mengandaikan tanpa menguji |

Ketiganya lolos karena terdengar masuk akal dan didukung perintah yang benar-benar
dijalankan. Yang menghentikan masing-masing bukan kehati-hatian yang lebih besar, melainkan
satu pertanyaan lanjutan yang kebetulan diajukan: *jalur repositorinya apa?*, *ini berkas
yang sama atau bernama sama?*, *apakah isinya memang sama?*
