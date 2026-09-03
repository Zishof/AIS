# Koreksi doc 97: kesebelas laporan itu bekerja

Batch lanjutan sesudah doc 99, dan isinya membatalkan temuan utama doc 97.

---

## 1. Klaim yang salah

Doc 97 melaporkan sebelas entri katalog laporan akuntansi — Buku Besar, Neraca Lajur, Trial
Balance, Jurnal Harian, dan Neraca/Laba Rugi/Arus Kas komparatif — "terdaftar tetapi belum
ada", dan menyimpulkan semuanya jatuh ke pesan "Laporan ini sedang disiapkan".

**Kesebelasnya bekerja.** Entri katalognya berakhir begini:

```java
k.items.add(item("lk_keu12", "Neraca / Laba Rugi / Arus Kas — 12 Bulan",
        "Kolom 12 bulan berjalan (pilih jenis di combo). Resmi dari jurnal akuntansi.",
        false, false, false, launchZk("keu12")));
                              ^^^^^^^^^^^^^^^^^
```

dan `launchZk` bukan penanda, melainkan URL:

```java
private static String launchZk(String lap) {
    return Common.ROOT + "/pages/master/kantin/laporan_keuangan.zul?lap=" + lap;
}
```

Jadi kesebelas entri itu **tautan** ke layar ZK, bukan kunci yang menunggu cabang dispatch.
Halaman tujuannya ada (`pages/master/kantin/laporan_keuangan.zul`, 87 baris), menyebut
parameter `lap` sebelas kali, dan memuat kode-kodenya apa adanya: `keu12`, `keu2th`,
`neracalajur`, `trial`, `bukubesar`.

## 2. Kenapa pemeriksanya keliru

Alatnya menjawab satu pertanyaan — "apakah ada cabang `if ("kunci".equals(r))` untuk kunci
ini?" — lalu jawabannya diperlakukan sebagai jawaban atas pertanyaan lain: "apakah laporan
ini ada?".

Katalog itu memakai **dua mekanisme**. Sebagian besar entri dilaksanakan di dalam
`LaporanKantinUtil` lewat rantai dispatch; keluarga `lk_*` dibuka sebagai halaman ZK lewat
URL. Mengukur yang pertama lalu menyimpulkan tentang yang kedua menghasilkan sebelas temuan
palsu — dan keseragamannya (**seluruh** keluarga `lk_`, tanpa kecuali) seharusnya sudah
menjadi tanda: cacat jarang rapi, mekanisme yang berbeda selalu rapi.

Alatnya kini mengenali entri berupa tautan dan melewatinya:

```
entri berupa tautan ZK (dilewati): 11
kunci tanpa pelaksana : 0
```

**Nol.** Katalog laporan itu utuh.

## 3. Kenapa kekeliruan ini lebih berbahaya daripada kelihatannya

Dua koreksi sebelumnya (doc 92, doc 95) membatalkan temuan yang **melebih-lebihkan
kerusakan** — melaporkan sesuatu hilang padahal ada. Yang ini sama bentuknya tetapi
akibatnya berbeda: doc 97 memberi tahu pembacanya bahwa sebelas laporan akuntansi **belum
dibuat**.

Pembaca yang mempercayainya punya satu langkah lanjutan yang wajar: membuatkannya. Yaitu
membangun ulang Buku Besar, Neraca Lajur, dan Trial Balance yang sudah ada dan sudah dipakai
— pekerjaan besar yang hasilnya duplikat, dan kemungkinan besar berbeda angkanya dari yang
lama.

Temuan palsu yang mengatakan "ini rusak" membuang waktu. Temuan palsu yang mengatakan "ini
belum ada" mengundang orang membuat yang kedua.

## 4. Yang tetap berlaku dari doc 97

Dua bagiannya tidak terpengaruh dan tetap sah:

- **Penjagaan `closing` pada 24 mesin posting: 24 dari 24 aman** — diperiksa langsung di
  sumbernya, bukan lewat katalog.
- **Keseimbangan debet/kredit di `CommonAkunting.saveTransaksi`** sudah digarap sesi lain di
  doc 72; tidak diduplikasi.

Bagian 2, 3, dan 5 doc 97 dibatalkan dokumen ini.

## 5. Tiga koreksi, satu bentuk

| Doc | Temuan yang dibatalkan | Pertanyaan yang membatalkannya |
|---|---|---|
| 92 | `^/web` pohon terpisah; sumber transkrip hilang | jalur repositorinya apa? ini berkas sama atau bernama sama? |
| 95 | 974 butir menu tanpa induk | kolom kedua itu `parent` atau `root`? |
| **100** | 11 laporan akuntansi belum ada | kolom terakhir entri itu apa? |

Ketiganya lolos karena pengukurannya benar dan hasilnya konsisten. Yang salah pertanyaannya —
dan pertanyaan yang salah tidak pernah mengumumkan dirinya lewat hasil yang berantakan. Ia
justru menghasilkan angka yang rapi.
