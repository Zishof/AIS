# Include runtime, dan lima cacat alat dalam satu batch

Batch lanjutan sesudah doc 89.

---

## 1. Yang sudah tertutup, dan yang belum

Include **statis** JSP (`<%@ include file="..." %>`) sudah aman: Jasper menyelesaikannya saat
penerjemahan, dan sapuan doc 83 atas 10.374 berkas bersih. Berkas yang hilang di situ
membuat terjemahannya gagal.

Yang tidak pernah diperiksa siapa pun adalah include **runtime**:

| Bentuk | Jumlah | Dimuat saat |
|---|---|---|
| `<jsp:include page="..."/>` | 8.608 | halaman dijalankan |
| `<include src="..."/>` (ZUL) | 213 | komponennya dirender |

Keduanya baru dicari ketika halamannya dibuka. Tujuan yang hilang tidak muncul di gerbang
mana pun — hanya sebagai halaman galat, atau lebih buruk: bagian halaman yang diam-diam
kosong.

[alat/include-tujuan-hilang.py](alat/include-tujuan-hilang.py) memeriksa keduanya.

```
tujuan diperiksa  : 8790
dilewati (dinamis): 42
tujuan hilang     : 1
```

## 2. Kanalnya sehat — satu temuan dari 8.790

`WEB-INF/z/x/y/pascasarjana/main.zul` dan `main_mhs.zul` memuat

```xml
<include src="tampilan_pengumuman_akademis.zul"/>
```

secara relatif, sedangkan berkas itu tidak ada di direktori `pascasarjana/`. Yang ada dua
salinan di tempat lain — `common/` (3.084 byte) dan `pages/main/` (884 byte).

Kedua halaman itu **hidup**: `apply=`-nya menunjuk `MainAction` dan `Report`, dua-duanya ada.
Berbeda dari `pascasarjana.zul` dan `mhs_pasca.zul` di direktori yang sama, yang kelasnya
memang sudah lenyap (doc 88). Jadi panel pengumuman pada dua halaman aktif ini gagal dimuat.

**Tidak diperbaiki.** Kedua kandidatnya berbeda isi, tidak ada halaman lain yang
meng-include berkas itu sehingga tidak ada konvensi untuk disalin, dan `pages/main/main.zul`
— tetangga salinan yang lebih kecil — justru tidak memakainya sama sekali. Memilih salah
satu berarti menebak, dan tebakan yang salah akan menampilkan panel pengumuman yang keliru
**sambil tampak bekerja**. Itu jenis kesalahan yang tidak akan pernah dilaporkan siapa pun.

## 3. Dari tiga temuan pertama, dua adalah cacat alat ini sendiri

Jalan pertama melaporkan tiga. Dua di antaranya salah, dan keduanya salah karena
pemeriksanya tidak membaca sumbernya seperti runtime membacanya.

**Jalur absolut ZUL diselesaikan terhadap akar yang salah.**
`<include src="/pascasarjana/blank_pasca.zul"/>` dipetakan ke `webapp/pascasarjana/...`,
padahal ZK menyelesaikannya terhadap akar ZUL: `webapp/WEB-INF/z/x/y/pascasarjana/...`.
Berkasnya ada; alatnya yang salah alamat. Kini kedua akar dicoba, karena berkas statis
(`/img`, `/css`) memang tinggal di akar webapp.

**Include di dalam komentar ikut dihitung.**
`organisasi.jsp` memuat empat tab; yang keempat — `_tab_organisasi_pegawai.jsp` — memang
tidak ada, tetapi baris include-nya berada di dalam `<%-- --%>`. Kode yang dikomentari bukan
kode. Komentar JSP dan HTML kini dibuang sebelum dipindai.

Sesudah keduanya diperbaiki, tiga temuan menjadi satu.

## 4. Lima cacat alat dalam satu batch

Menghitung yang sebelumnya (doc 85 dan 86), pemeriksa-pemeriksa ini sudah menyumbang lima
cacatnya sendiri: log UTF-16 yang tidak terbaca, stderr yang terpotong di lebar konsol,
koma PowerShell yang memecah berkas argumen, akar jalur ZUL yang salah, dan komentar yang
ikut dipindai.

Semuanya punya bentuk yang sama: **alatnya melaporkan sesuatu yang terdengar masuk akal.**
Tidak satu pun ketahuan dari keluarannya sendiri — semuanya ketahuan karena tiap temuan
diperiksa ke sumbernya sebelum disentuh.

Angka yang layak diingat dari batch ini: dari tiga temuan pertama, **dua adalah kesalahan
alatnya**. Bila daftar itu dikerjakan tanpa diperiksa, dua berkas yang sehat akan disunting
tanpa alasan — dan satu-satunya temuan yang nyata tetap tidak tersentuh.
