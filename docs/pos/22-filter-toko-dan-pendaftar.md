# 22 — Filter toko lintas-toko & pembatasan per pendaftar

Dua lapis yang bekerja bersama:

1. **Izin** — peran mana yang boleh melihat lebih dari satu toko;
2. **Batas** — sejauh mana "lebih dari satu toko" itu, bila akunnya milik pendaftar
   (tenant) tertentu.

## Lapis 1 — izin "Boleh melihat seluruh toko"

`ais.database.model.Tbmrole` mendapat kolom `boleh_lihat_semua_toko` (bawaan `FALSE`).
Kotak centangnya di `TbmroleAction` (baris 2452). Bawaan `FALSE` disengaja: izin melihat
data toko lain harus dinyalakan sadar, bukan diwarisi diam-diam oleh setiap peran lama.

Daftar toko di layar peran dirapikan menjadi dua kolom di dalam `Div` bergulir
(`max-height:240px`) dengan `row.setValign("top")`. Tanpa `valign=top`, baris dengan
banyak toko membuat labelnya melayang di tengah dan sejajaran kolom rusak.

Klien menanyakan izin ini lewat aksi **`toko_filter_list`**. Nilainya **berasal dari
server**, tidak disimpulkan di klien — `Sesi.bolehSemuaToko` tidak pernah ditebak dari
peran atau dari ada-tidaknya `Pedagang`.

## Lapis 2 — batas per pendaftar

`ais.database.model.Tbmuser` mendapat relasi `pendaftar` (`@JoinColumn(name="pendaftar")`).
Aturannya:

| `tbmuser.getPendaftar()` | Cakupan |
|---|---|
| `null` | seluruh toko aktif |
| terisi | hanya toko milik pendaftar itu |

Ini **bukan** pengganti lapis 1: pengguna tetap harus punya izin lintas toko dulu;
pendaftar hanya menyempitkan hasilnya.

### Jalan buntu yang sempat ditempuh

Percobaan pertama menurunkan pendaftar dari `akun_manajemen.userid`. Itu **salah**, dan
sempat masuk ke rancangan sebelum ketahuan: `akun_manajemen` membangkitkan kredensialnya
sendiri berpola `mgr-<nama>`, yang tidak pernah sama dengan `Tbmuser.userid`. Cocokannya
selalu kosong, jadi setiap pengguna akan terlihat "tidak terikat pendaftar" — yaitu
**boleh melihat semuanya**. Diganti dengan relasi langsung `Tbmuser.pendaftar`.

### Layar pengelolaan

`ais.action.master.PendaftarAction` mendapat dua tab:

- **Toko** (`gambarDaftarToko`, baris 949) — daftar toko milik pendaftar;
- **Pengguna** (`gambarPenggunaPendaftar`, baris 996) — menautkan/melepas `Tbmuser`.

Tanpa layar ini, kolom `Tbmuser.pendaftar` hanya bisa diisi lewat SQL langsung.

## Penegakan di API

Helper di `ais.action.servlet.PosApi`:

| Helper | Guna |
|---|---|
| `bolehLihatSemuaToko(Tbmuser)` | membaca izin dari peran |
| `pendaftarIdPengguna(Tbmuser)` | id pendaftar, atau `null` |
| `kondisiToko(alias, pendaftarId)` | potongan `WHERE` untuk mode seluruh toko |
| `batasPendaftar(alias, pendaftarId)` | tambahan batas tenant |
| `isiParamToko(ps, posisi, tokoId)` | mengisi parameter toko dgn aman |

Gerbangnya: `tokoId == null && tbmuser.getPedagang() != null && !bolehLihatSemuaToko(...)`
→ ditolak. Artinya klien **tidak bisa** memperluas cakupannya sendiri hanya dengan
menghilangkan `tokoId` dari payload.

### Pola SQL saringan opsional

```sql
COALESCE(?::bigint, COALESCE(x.toko, -1)) = COALESCE(x.toko, -1)
```

Dua hal yang dipecahkan sekaligus:

1. **Nomor parameter tidak bergeser.** Saringan yang dirakit dengan menyambung string
   membuat nomor `?` berubah-ubah tergantung filter mana yang aktif; itu sumber salah-bind
   yang sulit dilacak. Di sini parameternya selalu ada.
2. **Baris ber-`toko IS NULL` tidak hilang.** Bentuk naifnya, `COALESCE(?, toko) = toko`,
   bernilai UNKNOWN saat `toko` NULL — dan baris itu lenyap dari hasil tanpa jejak. Pada
   `koperasi.produk` di UAT, **85% baris** ber-`toko` NULL. Uji terkait naik dari 3/5 ke
   5/5 setelah `COALESCE(...,-1)` di kedua sisi.

Jebakan pendamping: helper pengisi parameter sempat mengubah `null` menjadi string
`"null"`. Diperbaiki dengan `ps.setNull(idx, Types.OTHER)` eksplisit.

## Sisi klien

- `Sesi`: `bolehSemuaToko`, `tokoFilter` (null = Semua Toko), `daftarTokoFilter`,
  `namaTokoFilter`.
- `AppShell`: kotak toko di kiri atas merangkap combo filter (`_pilihFilterToko`,
  `muatDaftarTokoFilter`).
- `ApiClient._ikutFilterToko(namaAksi)` menyisipkan `tokoId` **hanya** untuk aksi
  berawalan `dashboard_` dan `laporan_`.

Batasan pada awalan itu disengaja: aksi kasir beroperasi pada satu toko, dan menyuntik
toko lain ke sana berarti **mencatat transaksi ke toko yang salah**. Konsekuensinya, aksi
non-laporan yang tetap butuh toko harus ditangani terpisah — itulah yang dibahas
[23](23-toko-pada-payload.md), dan celah itu memang sempat terlewat.

## Agregasi lintas toko dalam satu pendaftar

Dasbor dan laporan menjumlahkan seluruh toko milik satu pendaftar bila pengguna berizin
lintas toko dan tidak memilih toko tertentu.

Enam tempat perlu penjagaan `session.get(Toko.class, null)` — pemanggilan itu **melempar**,
bukan mengembalikan `null`. Label ekspor yang sempat berbunyi `tokonull` diganti menjadi
`semua`.

## Uji & yang belum terbukti

Diverifikasi dengan data yang disemai untuk dua pendaftar, seluruhnya di dalam transaksi
yang di-ROLLBACK: 4/4 lulus — pengguna pendaftar A tidak pernah melihat toko pendaftar B,
dan penjumlahannya benar per pendaftar.

**Belum terbukti di lapangan:** jalur "terikat pendaftar" dengan data nyata. UAT belum
punya `Tbmuser` yang benar-benar ditautkan ke pendaftar, jadi yang teruji baru logikanya,
bukan datanya.
