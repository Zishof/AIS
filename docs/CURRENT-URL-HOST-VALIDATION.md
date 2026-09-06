# Validasi Host untuk Common.CURRENT_URL

`Common.CURRENT_URL` dan `Common.CURRENT_URL_SIMPLE` (`ais/common/Common.java`) adalah
variabel statis tingkat proses yang menyimpan skema+host+port (dan, untuk `CURRENT_URL`,
context path) aplikasi. Nilainya disusun dari header `Host` permintaan HTTP yang sedang
berjalan oleh delapan servlet: `Main`, `Index`, `Login`, `Baru`, `Dashboard`, `Mobile`,
`New`, dan `FilterJSP` (`FilterJSP` hanya menulis `CURRENT_URL_SIMPLE`). Karena keduanya
statis dan bukan `ThreadLocal`, nilai yang ditulis satu permintaan tetap berlaku untuk
permintaan lain yang lewat sesudahnya sampai ada permintaan berikutnya yang menimpanya
lagi — termasuk permintaan ke `/index` dan `/login`, yang dapat diakses tanpa login.

Nilai ini pernah ikut dipakai menyusun `callbackUrl` yang dikirim ke gateway pembayaran
virtual account (Finpay) dan URL berkas laporan yang dikembalikan ke klien API. Kode baru
yang perlu URL absolut dari sebuah permintaan **wajib** memakai
`Common.getRequestHostWithProtocol(request)` atau `ApiHelperSupport.absoluteUrl(request, uri)`
(keduanya menyusun URL dari objek permintaan yang sedang aktif), **bukan** membaca
`Common.CURRENT_URL`/`CURRENT_URL_SIMPLE` langsung — membaca variabel global itu untuk
sesuatu yang dikirim ke pihak ketiga (URL callback, tautan berkas) dapat mencerminkan host
permintaan lain, bukan permintaan yang sedang diproses.

## Gerbang validasi host

Sebelum menimpa `CURRENT_URL`/`CURRENT_URL_SIMPLE`, kedelapan servlet di atas memanggil
`Common.sanitizedRequestHostForCurrentUrl(request)`. Bila hasilnya `null`, penulisan
dilewati dan nilai lama dipertahankan. Method ini menggabungkan dua pemeriksaan:

1. **Format host** (`Common.isValidRequestHostFormat(host, port)`) — menolak host kosong
   atau yang tidak cocok pola `[A-Za-z0-9.-]+|[0-9a-fA-F:]+` (nama domain/hostname biasa,
   atau alamat IPv6 telanjang), serta porta di luar rentang 1-65535. Pola yang sama dipakai
   `Repository.publicOrigin()` untuk portal Repository publik.
2. **Allowlist host** (`Common.isHostAllowedForCurrentUrl(host)`) — opsional, lihat di
   bawah.

Kedua pemeriksaan ini hanya menyaring bentuk/keanggotaan host; ini bukan pengganti daftar
proxy tepercaya di level container (lihat catatan `X-Forwarded-Proto` di bawah).

## Konfigurasi: `current_url_host_allowlist`

Kunci `Konfigurasi` (tabel konfigurasi, dibaca lewat `Common.getKonfigurasi`) berisi daftar
nama host yang diizinkan menimpa `CURRENT_URL`/`CURRENT_URL_SIMPLE`, dipisah koma, tanpa
membedakan huruf besar/kecil:

```
current_url_host_allowlist = kampus-a.ac.id,kampus-b.ac.id,portal.kampus-a.ac.id
```

- **Kosong atau belum disetel (default)**: seluruh host yang lolos validasi format di atas
  diizinkan — perilaku instalasi yang sudah berjalan tidak berubah tanpa tindakan admin.
- **Disetel**: hanya host yang persis cocok (setelah `trim()`, tanpa wildcard) dengan salah
  satu entri daftar yang diizinkan menimpa nilai global; permintaan dengan `Host` header
  lain (mis. domain penyerang, atau typo/scan otomatis) tidak lagi mengubah
  `CURRENT_URL`/`CURRENT_URL_SIMPLE` untuk permintaan lain.
- **Kegagalan membaca konfigurasi** (mis. basis data belum siap saat startup) meloloskan
  host apa pun yang lolos validasi format — kegagalan baca tidak boleh mengunci seluruh
  aplikasi.

Aktifkan konfigurasi ini pada instalasi yang hanya melayani satu atau beberapa hostname
tetap (kasus paling umum). Instalasi yang sengaja melayani banyak hostname dinamis
(reseller multi-domain, dsb.) sebaiknya tetap membiarkan kunci ini kosong dan mengandalkan
validasi format saja.

## Terkait: `wajib_https`

Kunci `wajib_https` (dibaca `CommonCurrentSessionHelper.isSecure(request)`) memaksa skema
`https://` pada `CURRENT_URL`/`CURRENT_URL_SIMPLE` terlepas dari `request.isSecure()`.
Method ini **sengaja tidak membaca header `X-Forwarded-Proto` langsung** — itu tanggung
jawab valve/proxy tepercaya di level container (mis. Tomcat `RemoteIpValve` dengan
`internalProxies`/`trustedProxies` disetel ke alamat proxy yang sah). Membacanya langsung
di kode aplikasi tanpa validasi asal permintaan akan membuka celah baru: klien mana pun
dapat memalsukan header itu untuk membuat aplikasi mengira koneksinya aman padahal tidak.
Bila `isSecure()` salah pada instalasi yang benar-benar berada di belakang proxy tepercaya,
perbaikannya ada di konfigurasi container/valve, bukan di kode aplikasi.

## Referensi kode

- `ais/common/Common.java` — `CURRENT_URL`/`CURRENT_URL_SIMPLE`,
  `isValidRequestHostFormat`, `isHostAllowedForCurrentUrl`,
  `sanitizedRequestHostForCurrentUrl`, `getRequestHostWithProtocol`.
- `ais/common/CommonCurrentSessionHelper.java` — `isSecure`.
- `ais/action/servlet/Repository.java` — `publicOrigin` (pola validasi host acuan).
- `ais/action/servlet/api/ApiHelperSupport.java` — `absoluteUrl` (pola URL absolut acuan
  untuk endpoint API yang membawa `HttpServletRequest`).
