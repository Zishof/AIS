# Termin berpajak yang tak pernah ditandai, dan JavaDoc yang tidak menempel

Batch lanjutan sesudah doc 74. Dua perbaikan masuk, satu temuan dilaporkan tanpa
diperbaiki massal — dan alasan **mengapa tidak** adalah bagian terpenting dokumen ini.

---

## 1. Termin berpajak tidak pernah ditandai terposting — diperbaiki

Temuan nomor 11 di [07-temuan-dan-jebakan.md](07-temuan-dan-jebakan.md), tercatat sejak
lama dan masih hidup. Diverifikasi ulang di kode sebelum disentuh.

`PostingPemesananPekerjaanAction.onPostingSemua` bercabang dua:

| Cabang | Jurnal ditulis | Dokumen ditandai |
|---|---|---|
| `jenisPajakBarang != null && getAkun() != null` | ya | **tidak** |
| selain itu (tanpa pajak) | ya | ya |

Cabang berpajak menutup transaksinya lalu langsung ke `} else {`; `setPostingHistory`
hanya ada di cabang bawah. Akibatnya termin berpajak jurnalnya tersimpan tetapi
dokumennya **tetap tampil sebagai draft selamanya**. Pengguna menekan posting berulang
kali, dan penjaga anti-jurnal-ganda (`GrupTransaksi` dgn `ref` = kunci termin sudah ada)
menolaknya diam-diam tanpa satu pun pesan.

Jalur API sudah benar sejak awal: menandai **kedua** cabang setelah jurnalnya
benar-benar tersimpan. Cabang berpajak di layar ZK kini mengikuti, dengan komentar yang
menyebut sebabnya supaya tidak terlepas lagi saat direfaktor.

Perilaku cabang tanpa pajak sengaja **tidak** disentuh, termasuk kejanggalannya sendiri
(penanda dipasang di luar `if (akunDebet != null && akunKredit != null)`, jadi dokumen
tetap ditandai walau tak ada jurnal ditulis). Mengubahnya adalah keputusan tersendiri
dengan risiko sendiri; menumpangkannya pada perbaikan ini akan mengaburkan keduanya.

## 2. JavaDoc `roleBolehUbahHarga` tidak menempel ke apa pun — diperbaiki

`Toko.java` memuat blok JavaDoc yang menjelaskan `roleBolehUbahHarga` — format CSV
berpembatas koma, hubungan OR dengan `userBolehUbahHarga` — berdiri tepat di atas blok
JavaDoc lain milik `otomatisBayarSetelahJam24`.

Dua blok JavaDoc beruntun membuat yang atas **tidak menempel ke deklarasi mana pun**:
javadoc dan hover IDE hanya membaca blok terakhir sebelum sebuah deklarasi. Jadi
penjelasan itu tidak pernah terlihat siapa pun, sementara `getRoleBolehUbahHarga()`
tampak tidak berdokumen. Bloknya dipindahkan ke atas `@Column role_boleh_ubah_harga`;
isinya tidak diubah sama sekali.

---

## 3. Temuan: 175 blok JavaDoc yatim — dilaporkan, TIDAK disapu otomatis

Kasus `Toko.java` bukan kasus tunggal. Sapuan seluruh basis kode:

| | |
|---|---|
| pasangan JavaDoc yatim | **175** |
| berkas terdampak | **91** |
| berdugaan perlu **dipindah** (bukan digabung) | **25** |

Alatnya ada di [alat/javadoc-yatim.py](alat/javadoc-yatim.py) — melaporkan saja, tidak
menyunting.

### Mengapa tidak disapu otomatis

Godaannya jelas: 175 tempat, polanya seragam, tinggal gabungkan tiap pasangan menjadi
satu blok. **Itu justru merusak.** Ada dua remedi yang berbeda, dan tampilannya sama:

- **GABUNG** — kedua blok membicarakan anggota yang sama (blok atas biasanya uraian lama
  atau lanjutannya). Menyatukannya benar.
- **PINDAH** — blok atas membicarakan anggota **lain** yang dideklarasikan di tempat lain
  pada berkas yang sama. Menggabungnya akan **menempelkan dokumentasi ke anggota yang
  salah** — dokumentasi yang keliru lebih berbahaya daripada dokumentasi yang hilang,
  karena yang hilang masih terasa hilang sedangkan yang keliru dipercaya.

`Toko.roleBolehUbahHarga` justru kasus PINDAH. Seandainya sapuan otomatis dijalankan,
penjelasan daftar role akan menempel pada pengaturan "bayar otomatis lewat jam 24" —
dua hal yang sama sekali tak berhubungan, dan pembacanya tidak punya cara tahu.

Karena itu alat ini hanya melaporkan, lengkap dengan dugaan PINDAH beserta nama anggota
yang disebut blok yatimnya, supaya penilaian tetap di tangan manusia. Contoh keluarannya:

```
KantinHelper.java:2061  menempel ke cekProdukKadaluarsa, tetapi menyebut IzinkanJualMinusStok
HibernateUtil.java:574  menempel ke transaksiSedangAktif, tetapi menyebut SessionUsable
Pajak.java:814          menempel ke cariKodeTagihanAtauBast, tetapi menyebut PermintaanPengadaanMasterAsset
```

Dugaan itu **dugaan**, bukan vonis: alatnya hanya mencocokkan nama anggota yang disebut,
dan sebuah blok bisa menyebut anggota lain sekadar sebagai rujukan silang.

## 4. Temuan doc 07 yang diperiksa dan ternyata sudah tertutup

Supaya tidak ada yang menyelidikinya ulang:

- **§2 Batal posting melaporkan sukses tanpa melakukan apa pun** — sudah diperbaiki;
  seluruh `batalkanPostingSemua` kini memakai `currentNativeSession()` dgn transaksi
  eksplisit per dokumen.
- **§10 `PostingHistory.getNama()` meledak bila pengguna null** — **sengaja** tidak
  diubah. Di produksi pengguna selalu ada dari sesi POS. Yang berlaku hanyalah aturannya:
  tiap harness dan pemanggil baru wajib memberi `Tbmuser` nyata.

## 5. Verifikasi

`javac -source 1.7 -target 1.7 -encoding UTF-8 -sourcepath . -cp webapp/WEB-INF/lib/*`
atas kedua berkas yang disunting: **EXIT=0**. Akhir baris CRLF dipertahankan (lihat
catatan penyapu di doc 74 §5 — menulis ulang berkas dgn LF membuat seluruh berkas tampak
berubah dan menenggelamkan diff yang sebenarnya).

## 6. Commit

| Revisi | Isi | Pesan |
|---|---|---|
| r82996 | butir 1, termin berpajak | **kosong** -- tersapu sesi paralel |
| r83001 | butir 2, JavaDoc Toko | utuh |

Penyapu yang dicatat di doc 74 §5 masih berjalan. Dari dua commit batch ini, satu
tersapu dan satu sempat lolos — jaraknya memang serapat itu.
