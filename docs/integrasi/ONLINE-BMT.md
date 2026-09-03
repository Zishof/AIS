# Integrasi Pembayaran Online BMT

## Ringkasan implementasi produksi

Callback yang harus didaftarkan pada sistem BMT untuk instalasi ini adalah:

```text
POST https://ecampus.staialbahjah.ac.id/albahjah/OnlineBmt
Content-Type: application/json; charset=utf-8
```

`https://ecampus.staialbahjah.ac.id/albahjah/` adalah context root aplikasi, sedangkan `/OnlineBmt` adalah mapping servlet pada `WEB-INF/web.xml`. BMT memanggil alamat tersebut dari server BMT menuju eCampus. Alamat ini bukan URL yang dipanggil aplikasi mobile/desktop untuk membuat invoice dan bukan halaman yang dibuka pengguna. Pengguna membuat invoice dari UI/API eCampus; sesudah itu BMT menggunakan callback untuk menanyakan invoice, memberi konfirmasi pembayaran, atau memeriksa status transaksi.

Endpoint hanya menerima `POST`. Reverse proxy harus meneruskan body JSON apa adanya, tidak mengubah encoding Base64, dan tidak mengalihkan `POST` menjadi `GET`. TLS wajib berakhir pada sertifikat publik yang valid. Berdasarkan konfirmasi tim BMT tanggal 3 September 2026, request BMT akan berasal dari `103.157.96.112` atau `103.27.207.203`. Kedua alamat tersebut adalah **IP sumber server BMT yang perlu di-allowlist pada firewall/WAF/reverse proxy eCampus**, bukan alamat server eCampus dan bukan URL callback. Pembatasan sebaiknya dilakukan sebelum request mencapai Tomcat. Jangan mempercayai `X-Forwarded-For` langsung di servlet kecuali reverse proxy yang menulis header itu telah ditetapkan sebagai trusted proxy, sebab header dari internet dapat dipalsukan. API key, HMAC, timestamp, dan nonce tetap wajib karena allowlist IP bukan pengganti autentikasi pesan. Health check sebaiknya tidak mengirim transaksi tiruan; `GET` pada endpoint akan menghasilkan kegagalan protokol `405` di dalam body JSON.

## Tujuan dan ruang lingkup

Integrasi Online BMT menambahkan kanal pembayaran untuk tagihan mahasiswa, calon mahasiswa, siswa, calon siswa, dan top-up deposit anggota koperasi. Kontrak teknis yang menjadi acuan berasal dari `ecampus_api.zip`, sedangkan urutan proses dan field bisnis dikonfirmasi oleh `ALUR_API_BMT_ZISHOFT_ALBAHJAH.xlsx` pada sheet `Bayar via Teller & Mobile BMT`. Workbook adalah diagram alur tingkat bisnis; setiap field di dalamnya tetap dikirim sebagai JSON plaintext di dalam envelope terenkripsi yang ditentukan sample code ZIP. Berbeda dari Smartlink yang menyediakan API untuk membuat payment link, kontrak Online BMT adalah kontrak inbound: eCampus menerbitkan invoice lokal, kemudian sistem BMT memanggil endpoint eCampus untuk `INQUIRY`, `PAYMENT`, atau `CHECK_STATUS_PAYMENT`. Perbedaan arah komunikasi ini penting. Nomor invoice yang tampil setelah pengguna menekan tombol Online BMT bukan bukti pembayaran dan tidak boleh langsung mengubah cicilan, status akademik, ataupun saldo deposit.

Seluruh jalur menggunakan `VirtualAccountBank` sebagai catatan invoice bersama. Hal ini membuat posting pembayaran tetap melewati mesin domain lama yang sudah menangani rincian tagihan, top-up, histori, dan deduplikasi. Implementasi tidak menulis saldo dengan SQL langsung. Untuk tagihan perguruan tinggi, callback meneruskan posting ke `PembayaranGatewayHelper.prosesRincianVA`; untuk sekolah ke `VirtualAccountBank.bayarSiswa`; dan untuk invoice top-up murni ke `VirtualAccountBank.bayarTopup`. Dengan demikian perilaku Online BMT konsisten dengan kanal yang sudah stabil dan tidak menciptakan versi perhitungan baru.

## Aktivasi berlapis dan default OFF

Online BMT bersifat fail-closed. Instalasi baru maupun instalasi lama tetap OFF sampai administrator mengaktifkannya secara eksplisit. Sakelar global adalah `aktifkan_pembayaran_via_online_bmt`. Perguruan tinggi juga memerlukan `aktifkan_pembayaran_via_online_bmt_pt_<ID_PT>`. Keduanya memiliki default `tidak_aktif`. Sekolah memiliki kolom `aktfkan_pembayaran_via_online_bmt`, dan kanal pembayaran memiliki kolom dengan nama yang sama. Getter kedua model menormalkan nilai `null` menjadi `false`; ini mencegah data lama tanpa nilai eksplisit tiba-tiba mengaktifkan kanal setelah deployment.

Aturan efektifnya adalah sebagai berikut. Invoice mahasiswa atau calon mahasiswa memerlukan sakelar global dan sakelar PT. Invoice siswa atau calon siswa memerlukan sakelar global, sakelar sekolah, serta sakelar kanal jika sekolah/tagihan memakai `KanalPembayaran`. Top-up anggota koperasi memerlukan sakelar global dan sakelar `KanalPembayaran`. Pemeriksaan dilakukan saat opsi ditampilkan, saat invoice dibuat, dan sekali lagi ketika callback diterima. Pemeriksaan berulang ini disengaja: daftar kanal dari klien tidak dipercaya, dan invoice lama tidak boleh tetap dapat dibayar apabila kanal telah dinonaktifkan karena insiden operasional.

Konfigurasi tambahan adalah `online_bmt_prefix_invoice` dengan default `BMT`, `online_bmt_biaya_administrasi` dengan default `0.0`, `online_bmt_api_key`, `online_bmt_encryption_key`, dan `online_bmt_hmac_key`. Secret wajib diisi melalui administrasi konfigurasi berotorisasi atau mekanisme secret server, tidak boleh ditulis ke source control, dokumentasi, log, maupun aplikasi mobile. Tim BMT mengizinkan credential pada sample code dipakai sementara untuk SIT. Izin ini berarti nilainya boleh dimasukkan ke konfigurasi **lingkungan SIT saja**, bukan disalin menjadi konstanta/default Java. Setelah SIT, credential perlu diganti untuk UAT/produksi dan disepakati pada kedua sisi. `online_bmt_request_time_tolerance` dapat mengatur toleransi timestamp dalam detik; nilai efektif dibatasi antara 30 dan 3600 detik, dengan default 300 detik. Berdasarkan konfirmasi tertulis BMT, `DATA` pada setiap respons sukses wajib selalu dienkripsi; perilaku ini dikunci di endpoint dan tidak dapat dimatikan oleh konfigurasi.

Konfigurasi lengkap yang perlu diperiksa sebelum aktivasi:

| Kunci/kolom | Default | Fungsi |
| --- | --- | --- |
| `aktifkan_pembayaran_via_online_bmt` | `tidak_aktif` | Gerbang global untuk seluruh aplikasi. |
| `aktifkan_pembayaran_via_online_bmt_pt_<ID_PT>` | `tidak_aktif` | Gerbang tenant perguruan tinggi. |
| `sekolah.sekolah.aktfkan_pembayaran_via_online_bmt` | `false` | Gerbang per sekolah. |
| `sekolah.kanal_pembayaran.aktfkan_pembayaran_via_online_bmt` | `false` | Gerbang per kanal sekolah/koperasi. |
| `online_bmt_prefix_invoice` | `BMT` | Prefix nomor invoice lokal, dibersihkan menjadi maksimal delapan karakter alfanumerik. |
| `online_bmt_biaya_administrasi` | `0.0` | Biaya admin yang menjadi bagian nominal inquiry dan payment. |
| `online_bmt_kode_mitra` | kosong | `KD_MITRA_BMT` pada inquiry. |
| `online_bmt_nama_mitra` | kosong | `NM_MITRA_BMT` pada inquiry. |
| `online_bmt_kode_merchant` | kosong | `KD_MERCHANT` pada inquiry. |
| `online_bmt_nama_merchant` | kosong | `NM_MERCHANT` pada inquiry. |
| `online_bmt_api_key` | kosong | Autentikasi envelope dari BMT. |
| `online_bmt_encryption_key` | kosong | Material pembentuk key AES-256. |
| `online_bmt_hmac_key` | kosong | Material pembentuk key HMAC-SHA256. |
| `online_bmt_request_time_tolerance` | `300` | Selisih waktu request terhadap server, dalam detik. |
| `online_bmt_enkripsi_response` | legacy/diabaikan | Dipertahankan agar data instalasi lama kompatibel. Respons sukses selalu terenkripsi dan layar konfigurasi tidak lagi menawarkan sakelar ini. |

### Konfigurasi berjenjang Sekolah dan Kanal Pembayaran

Sepuluh nilai operasional di atas juga tersedia sebagai kolom nullable pada `sekolah.sekolah` dan `sekolah.kanal_pembayaran`: `online_bmt_prefix_invoice`, `online_bmt_biaya_administrasi`, `online_bmt_kode_mitra`, `online_bmt_nama_mitra`, `online_bmt_kode_merchant`, `online_bmt_nama_merchant`, `online_bmt_api_key`, `online_bmt_encryption_key`, `online_bmt_hmac_key`, dan `online_bmt_request_time_tolerance`. Kolom yang sama ditambahkan pada tabel audit Envers. Seluruh `ALTER TABLE` dijalankan saat startup melalui native SQL pada Hibernate `Session` dan transaksi Hibernate, bukan melalui skrip JDBC lepas. DDL menggunakan `ADD COLUMN IF NOT EXISTS`, sehingga deployment ulang tidak menghapus atau menimpa nilai yang sudah ada.

Urutan resolusi selalu **Kanal Pembayaran, kemudian Sekolah, kemudian global**. Contohnya, kanal A dapat mempunyai prefix dan biaya admin sendiri, tetapi tetap mewarisi identitas merchant dan credential dari sekolah. Sekolah dapat mempunyai identitas merchant sendiri, tetapi mewarisi toleransi timestamp global. Bila kanal tidak terhubung ke sekolah, seperti sebagian cara pembayaran koperasi, parent kanal langsung menjadi konfigurasi global. Konfigurasi perguruan tinggi tetap memakai global karena model PT hanya memiliki sakelar per tenant dan tidak mempunyai kumpulan credential sendiri.

Arti nilai kosong harus dipertahankan ketika memelihara kode:

| Jenis nilai | Nilai pada Sekolah/Kanal | Makna |
| --- | --- | --- |
| String, termasuk prefix dan identitas merchant | `NULL`, kosong, atau hanya spasi | Warisi parent. |
| Biaya administrasi | `NULL` | Warisi parent. |
| Biaya administrasi | `0` | Override sah: kanal ini tanpa biaya admin. Jangan dianggap kosong. |
| Toleransi timestamp | `NULL` | Warisi parent. |
| Toleransi timestamp | `30` sampai `3600` | Override sah dalam detik. |
| Sakelar aktif | `NULL` atau `false` | OFF. Sakelar tidak diwariskan demi keamanan. |

Tiga secret adalah **satu paket atomik**, bukan tiga field yang diwariskan secara independen. Jika `online_bmt_api_key`, `online_bmt_encryption_key`, dan `online_bmt_hmac_key` semuanya kosong, scope tersebut mewarisi ketiganya dari parent. Jika ingin override, ketiganya wajib diisi. Kombinasi parsial ditolak oleh form dan dianggap konfigurasi tidak siap oleh callback. Aturan ini mencegah keadaan berbahaya ketika API key kanal dipasangkan dengan AES key sekolah dan HMAC key global. Input secret memakai tipe password pada UI, tetapi nilai tersimpan tetap dimuat ke komponen agar penyimpanan perubahan field lain tidak mengosongkan secret secara tidak sengaja.

Form `SekolahAction` menampilkan override sekolah pada tab Data Payment Gateway. Form `KanalPembayaranAction` menampilkan override kanal pada bagian Online BMT. Label “kosong = ikut ...” adalah perilaku domain, bukan sekadar petunjuk layar. Saat menyimpan, prefix wajib terdiri dari satu sampai delapan huruf/angka, biaya admin tidak boleh negatif, toleransi harus berupa bilangan bulat 30-3600, dan paket credential harus lengkap atau seluruhnya kosong. Sakelar sekolah dan kanal tetap default OFF meskipun konfigurasi parent lengkap.

Resolver bersama berada di `OnlineBmtUtil`. Pembuatan invoice siswa, top-up siswa, koperasi, dan kantin memakai resolver yang sama untuk prefix dan biaya administrasi. Nilai biaya efektif disimpan pada `VirtualAccountBank`, sehingga nominal yang dilihat pengguna, hasil inquiry, dan validasi payment memakai angka identik. Jangan menghitung ulang biaya admin di callback dari konfigurasi terbaru: perubahan konfigurasi setelah invoice dibuat tidak boleh mengubah nominal invoice lama. Identitas merchant, credential, dan toleransi adalah kebijakan callback saat ini dan karena itu di-resolve ulang ketika callback diterima.

Status **aktif** dan **siap transaksi** dibedakan. Aktif berarti sakelar global serta tenant telah ON; siap transaksi berarti tiga credential dan empat identitas mitra/merchant pada konfigurasi efektif juga lengkap. UI, action, serta API pembuat invoice hanya menawarkan Online BMT ketika statusnya siap transaksi. `prepareInvoice` melakukan pemeriksaan terakhir agar jalur yang terlewat tidak dapat menerbitkan invoice yang nantinya selalu ditolak saat inquiry. Callback tetap membedakan kegagalan sakelar dari konfigurasi tidak lengkap dan menyebut nama field yang belum tersedia tanpa pernah menampilkan nilai secret.

Khusus invoice mahasiswa, calon mahasiswa, dan perguruan tinggi, konfigurasi efektif berasal dari konfigurasi **global**; sakelar `aktifkan_pembayaran_via_online_bmt_pt_<ID_PT>` hanya menjadi gerbang ON/OFF tenant. Nilai pada `Sekolah` atau `KanalPembayaran` tidak menjadi fallback ke arah invoice mahasiswa. Override Sekolah/Kanal hanya berlaku bagi invoice yang memang memiliki relasi siswa, calon siswa, sekolah, atau kanal tersebut. Karena itu pengujian invoice mahasiswa wajib mengisi `online_bmt_kode_mitra`, `online_bmt_nama_mitra`, `online_bmt_kode_merchant`, dan `online_bmt_nama_merchant` pada Pengaturan Konfigurasi global.

Pemilihan credential callback berlangsung dua tahap karena isi `NO_INVOICE` berada di dalam `DATA` terenkripsi. Pertama, `API_KEY` pada envelope luar dipakai untuk mencari kandidat profil global, sekolah, atau kanal melalui Hibernate. Servlet mencoba memverifikasi HMAC dan membuka payload hanya dengan kandidat yang lengkap. Kedua, setelah `NO_INVOICE` diketahui dan invoice ditemukan, sistem menentukan sekolah serta kanal pemilik invoice, menghitung konfigurasi efektifnya, lalu mencocokkan **API key, encryption key, dan HMAC key sekaligus** dengan kandidat yang membuka payload. Request baru diterima jika kedua tahap cocok. Akibatnya credential sekolah A tidak dapat dipakai untuk membaca atau membayar invoice sekolah B, sekalipun nomor invoice B diketahui. Respons sukses dienkripsi kembali menggunakan profil efektif pemilik invoice yang sama.

Relasi `siswa`, `calonSiswa`, `anggotaKoperasi`, `kanalPembayaran`, dan sekolah di bawahnya dihidrasi sebelum session pencarian `VirtualAccountBank` ditutup. Langkah ini bukan optimasi tampilan. Tanpa hidrasi tersebut, resolver callback dapat menemui proxy detached, kehilangan informasi tenant, lalu secara keliru jatuh ke konfigurasi global. Setiap refactor lookup invoice wajib mempertahankan kontrak hidrasi ini.

Empat identitas mitra/merchant diwajibkan bersama-sama. `INQUIRY` ditolak dengan kode `503` bila salah satunya kosong. Keputusan fail-closed ini mencegah respons yang secara teknis sukses tetapi tidak dapat dipetakan oleh BMT. Konfigurasi secret juga diwajibkan sebelum payload diproses. Jangan mengaktifkan sakelar global terlebih dahulu sambil membiarkan konfigurasi lain kosong, karena hasil yang diharapkan pada kondisi tersebut memang penolakan layanan.

Matriks aktivasi efektif:

| Pemilik invoice | Syarat efektif |
| --- | --- |
| Mahasiswa/calon mahasiswa | Global ON dan PT terkait ON. |
| Siswa/calon siswa tanpa kanal eksplisit | Global ON dan sekolah ON. |
| Siswa/calon siswa dengan kanal | Global ON, sekolah ON, kanal ON. |
| Anggota koperasi/topup POS | Global ON dan kanal pada cara pembayaran ON. |

Sakelar diperiksa saat daftar metode dibentuk, saat tombol ditampilkan, saat invoice dibuat, dan saat callback diproses. Karena itu menonaktifkan kanal saat insiden akan menghentikan invoice baru sekaligus menolak pembayaran invoice lama melalui callback sampai kanal diaktifkan kembali.

## Penerbitan invoice

Semua generator menerima marker parameter `online_bmt=true`. Marker ini ditangani oleh `OnlineBmtUtil.prepareInvoice`. Method tersebut membuat nomor lokal berawalan prefix yang telah dibersihkan menjadi huruf/angka, menetapkan bank `Online BMT`, channel `ONLINE_BMT`, mengosongkan link, serta menyimpan metadata audit tanpa secret. Generator tetap menghitung total, biaya administrasi, daftar cicilan, item biaya, pemilik, semester, tahun akademik, dan waktu kedaluwarsa dengan algoritma yang sama seperti bank online lain.

Keterangan invoice menyertakan marker `online_bmt:true`. Marker menjadi bagian dari kunci pencarian invoice aktif, sehingga permintaan Online BMT tidak menggunakan ulang invoice Smartlink atau bank online generik yang kebetulan memiliki rincian tagihan sama. Sebaliknya, menekan tombol Online BMT berulang untuk rincian sama dapat menggunakan kembali invoice BMT yang masih aktif, mencegah banyak nomor pembayaran yang tidak perlu.

## Endpoint dan kriptografi

Endpoint merchant adalah `POST /OnlineBmt`. Body luar wajib berupa JSON dengan `API_KEY` dan `DATA`. `DATA` mengikuti format kontrak: JSON plaintext dienkripsi AES-256-CBC dengan IV acak 16 byte; key AES adalah raw SHA-256 dari encryption key. Payload internal berbentuk `v1.<base64-iv>.<base64-ciphertext>`. HMAC-SHA256 dihitung atas payload tersebut menggunakan raw SHA-256 dari HMAC key, lalu hasil akhir adalah Base64 dari `payload.<base64-hmac>`.

Sesuai contoh PHP dari BMT, kesalahan validasi dan kesalahan bisnis tetap dikirim dengan HTTP status `200`. Keberhasilan atau kegagalan wajib dibaca dari field `STATUS` dan `KODE_STATUS`, bukan dari HTTP status. Kesalahan internal tak terduga tetap dapat menghasilkan HTTP `500` agar perangkat pemantauan dan mekanisme retry infrastruktur dapat mengenalinya. Pola ini sengaja dipertahankan demi kompatibilitas dengan pihak BMT yang mungkin hanya memproses body untuk respons HTTP 2xx.

Validasi dilakukan sebelum logika keuangan: metode HTTP harus POST, fitur global harus aktif, API key harus memilih setidaknya satu profil credential lengkap, Base64 dan versi envelope harus valid, HMAC harus cocok, IV harus 16 byte, ciphertext harus dapat didekripsi, profil pembuka payload harus sama dengan profil efektif pemilik invoice, timestamp harus berada dalam toleransi tenant, nonce wajib ada dan belum pernah digunakan, serta jenis request harus dikenal. Pesan galat tidak membocorkan secret, scope kandidat yang hampir cocok, atau plaintext sensitif.

### Pemetaan alur workbook ke endpoint

| Alur pada workbook | Pemanggil | Data bisnis | Keputusan eCampus |
| --- | --- | --- | --- |
| Inquiry tagihan | Server BMT | `NO_INVOICE` | Mengembalikan sembilan field identitas dan nominal invoice bila invoice BMT valid, belum kedaluwarsa, dan tenant aktif. |
| Pembayaran teller/mobile | Server BMT | `NO_INVOICE`, `NOMINAL`, `NO_TRANSAKSI_BMT`, `CHANNEL_BMT` | Memvalidasi nominal dan idempotensi, lalu membukukan pembayaran melalui mesin domain kanonik. |
| Pembayaran melalui VA BMT | Bank/payment gateway memberi notifikasi ke BMT, lalu server BMT memanggil eCampus | Empat field yang sama dengan PAYMENT | eCampus tidak membuat VA bank eksternal dan tidak menerima callback langsung dari bank; callback tepercaya tetap datang dari BMT sebagai `PAYMENT`. |
| Check status setelah timeout | Server BMT | Empat field yang sama dengan PAYMENT, nonce baru | Membaca ledger dan bukti pembayaran eCampus; request check bukan bukti pembayaran baru. |

Pembagian tanggung jawab ini harus dipertahankan saat SIT/UAT. Pengguna memasukkan nomor invoice eCampus pada teller atau aplikasi BMT. Bila pengguna memilih Indomaret, Alfamart, bank umum, atau channel VA lain, pembuatan nomor VA dan penerimaan callback bank merupakan tanggung jawab BMT. eCampus hanya menerima notifikasi akhir yang sudah dinormalisasi oleh BMT. Dengan demikian eCampus tidak perlu mengetahui credential bank di belakang BMT, sementara BMT tidak boleh meminta eCampus menganggap inquiry atau check status sebagai pembayaran.

`INQUIRY` mencari `NO_INVOICE`, memastikan invoice memang milik Online BMT, memeriksa sakelar tenant, status lunas, dan kedaluwarsa. Respons mengembalikan identitas pemilik ringkas dan nominal yang harus dibayar, yaitu total invoice ditambah biaya administrasi. `PAYMENT` menambahkan validasi `NOMINAL`, `NO_TRANSAKSI_BMT`, dan `CHANNEL_BMT`. Channel yang diterima dibatasi pada daftar dari kontrak: `TELLER`, `MOBILE_NASABAH`, `MOBILE_PETUGAS`, `MOBILE_AGEN`, dan `VIRTUAL_ACCOUNT`. `CHECK_STATUS_PAYMENT` membaca ledger server; ia tidak menebak keberhasilan hanya dari data yang dikirim klien.

### Bentuk request sebelum enkripsi

Contoh berikut menunjukkan plaintext yang harus dienkripsi menjadi field `DATA`. Nilai `TIMESTAMP` adalah Unix epoch detik, bukan milidetik. `NONCE` harus acak, maksimal 200 karakter, dan tidak boleh digunakan ulang untuk request apa pun.

```json
{
  "JENIS_REQUEST": "INQUIRY",
  "TIMESTAMP": 1788326400,
  "NONCE": "uuid-acak-untuk-inquiry",
  "NO_INVOICE": "BMT1234567890"
}
```

```json
{
  "JENIS_REQUEST": "PAYMENT",
  "TIMESTAMP": 1788326460,
  "NONCE": "uuid-acak-untuk-payment",
  "NO_INVOICE": "BMT1234567890",
  "NOMINAL": 102500,
  "NO_TRANSAKSI_BMT": "TRX-BMT-UNIK-0001",
  "CHANNEL_BMT": "VIRTUAL_ACCOUNT"
}
```

```json
{
  "JENIS_REQUEST": "CHECK_STATUS_PAYMENT",
  "TIMESTAMP": 1788326520,
  "NONCE": "uuid-acak-untuk-check-status",
  "NO_INVOICE": "BMT1234567890",
  "NOMINAL": 102500,
  "NO_TRANSAKSI_BMT": "TRX-BMT-UNIK-0001",
  "CHANNEL_BMT": "VIRTUAL_ACCOUNT"
}
```

Setelah enkripsi, request HTTP luar hanya berbentuk berikut. Placeholder tidak boleh dikirim secara literal:

```json
{
  "API_KEY": "<secret produksi yang disepakati>",
  "DATA": "<base64 dari v1.base64(iv).base64(ciphertext).base64(hmac)>"
}
```

Urutan kriptografi yang harus sama persis pada kedua pihak adalah: serialisasi plaintext menjadi JSON UTF-8; hitung key AES dari `SHA-256(ENCRYPTION_KEY)` dalam bentuk raw 32 byte; buat IV acak 16 byte; enkripsi menggunakan AES-256-CBC dan padding PKCS#7/PKCS#5; bentuk `v1.<base64 IV>.<base64 ciphertext>`; hitung HMAC-SHA256 atas string payload itu dengan key raw `SHA-256(HMAC_KEY)`; tambahkan `<base64 HMAC>` sebagai bagian keempat; lalu Base64-kan seluruh string empat bagian. HMAC diverifikasi sebelum decrypt untuk menghindari pemrosesan ciphertext yang telah dimodifikasi.

### Bentuk response

Respons luar selalu memiliki empat field:

```json
{
  "STATUS": true,
  "KODE_STATUS": "00",
  "KETERANGAN": "Request berhasil.",
  "DATA": "<payload response terenkripsi>"
}
```

`DATA` pada setiap respons sukses selalu dienkripsi dengan algoritma yang sama. Sesuai workbook, sample `api.php`, dan konfirmasi WA BMT, plaintext hasil decrypt inquiry berisi tepat sembilan field: `NO_INVOICE`, `NAMA`, `TGL`, `DESKRIPSI`, `NOMINAL`, `KD_MITRA_BMT`, `NM_MITRA_BMT`, `KD_MERCHANT`, dan `NM_MERCHANT`. Jangan menambahkan `STATUS_TRANSAKSI` pada hasil inquiry karena status itu hanya menjadi kontrak respons `PAYMENT` dan `CHECK_STATUS_PAYMENT`. Plaintext hasil decrypt payment/check berisi `STATUS_TRANSAKSI` dan `DESKRIPSI_STATUS`.

Ada ketidakkonsistenan pada contoh ZIP: komentar `config.php` menyebut enkripsi respons dapat dimatikan dan default contoh `false`, tetapi `api.php` tetap memanggil `encryptData()` pada semua respons sukses. Konfirmasi WA BMT menyelesaikan ketidakkonsistenan itu: parameter `DATA` pada respons wajib dienkripsi. Karena itu implementasi eCampus selalu mengenkripsi respons sukses. Kunci konfigurasi lama tetap dikenal untuk kompatibilitas data, tetapi nilainya sengaja diabaikan dan tidak ditampilkan lagi sebagai sakelar admin.

Kesalahan autentikasi, validasi, invoice, atau bisnis memakai `STATUS=false` dan `DATA={}`. Sesuai contoh BMT, kesalahan protokol tersebut tetap dikirim melalui HTTP 200; integrator wajib membaca `STATUS` dan `KODE_STATUS`. Hanya exception internal yang tidak tertangani yang memakai HTTP 500. Kode umum meliputi `400` untuk payload/field/channel tidak valid, `401` untuk API key, `408` untuk timestamp kedaluwarsa, `409` untuk nonce atau nomor transaksi yang konflik, `01` untuk kegagalan bisnis invoice/transaksi, dan `503` untuk kanal atau konfigurasi yang belum siap.

Lookup invoice tidak mensyaratkan `BankHost` tertentu. Sebagian generator bank-online lama masih mengisi relasi `BankHost` karena menggunakan struktur bersama Smartlink, sedangkan callback Online BMT datang tanpa objek host internal eCampus. Lookup tetap memakai nomor invoice persis dan sesudah ditemukan wajib melewati validasi `bank = Online BMT` serta sakelar tenant. Dengan urutan ini invoice BMT lama maupun baru tetap dapat ditemukan tanpa memperluas akses ke invoice kanal lain.

## Idempotensi dan konsistensi

Tabel `online_bmt_nonce` melindungi dari replay payload. Nonce memiliki primary key dan hanya dapat dipakai sekali, termasuk bila request pertama gagal pada validasi bisnis. Tabel `online_bmt_request_guard` menjadi ledger nomor transaksi BMT. Unique index pada `no_transaksi_bmt` memungkinkan `ON CONFLICT` bekerja tanpa race. Index `(no_invoice, status, id DESC)` mempercepat pemeriksaan status invoice. Startup memakai DDL `IF NOT EXISTS`, sehingga aman pada deployment berulang dan beberapa node aplikasi. Seluruh `ALTER TABLE` milik migrasi Online BMT dijalankan melalui native SQL pada Hibernate `Session` dan transaksi Hibernate tersendiri; hal ini juga memastikan instalasi lama mendapat kolom `channel_bmt` tanpa bergantung pada utilitas koneksi SQL di luar lifecycle persistence aplikasi.

Saat `PAYMENT` tiba, server mengambil PostgreSQL session advisory lock berdasarkan nomor transaksi BMT. Lock dibuat pada tingkat session, bukan transaction, karena proses harus melewati dua batas transaksi: menyimpan ledger lebih dahulu, lalu memanggil mesin pembayaran kanonik yang membuka transaksinya sendiri. Jika nomor transaksi sudah sukses untuk invoice, nominal, dan channel yang sama serta bukti domain masih tersedia, retry mengembalikan hasil sukses tanpa posting kedua. Jika nomor transaksi yang sama dipakai untuk invoice, nominal, atau channel berbeda, request ditolak. `CHANNEL_BMT` disimpan di ledger, bukan sekadar divalidasi saat request masuk, agar audit dapat membedakan pembayaran teller, mobile nasabah, mobile petugas, mobile agen, dan virtual account secara deterministik. Ledger lama yang dibuat sebelum kolom channel tersedia boleh mengikat channel tepat satu kali di bawah advisory lock, hanya bila invoice dan nominal cocok dan request telah lolos autentikasi lengkap. Setelah terikat, channel tidak dapat diganti.

Ledger `PROCESSING` di-commit sebelum posting finansial dimulai. Urutan ini merupakan perlindungan terhadap kegagalan proses di tengah jalan. Contohnya, mesin pembayaran sudah berhasil membuat `Pembayaran` atau `Deposit`, tetapi JVM berhenti sebelum callback sempat mengubah ledger menjadi `SUCCESS`. Pada retry dengan nonce baru, server menemukan ledger `PROCESSING`, mengecek bukti lunas dari invoice, lalu memulihkan ledger menjadi `SUCCESS` tanpa menjalankan posting kedua. Bila mesin posting melempar exception, server juga memeriksa ulang invoice: jika bukti pembayaran ternyata sudah terbentuk, hasil dipulihkan sebagai sukses; jika belum, ledger ditandai `FAILED` dan dapat dicoba ulang secara aman. Ledger `FAILED` tidak dipulihkan hanya karena invoice kemudian lunas, sebab pembayaran tersebut mungkin berasal dari transaksi atau kanal lain. Ledger `SUCCESS` yang kehilangan bukti domain juga tidak dibalas sukses; status diubah menjadi `FAILED` dengan kode rekonsiliasi agar operator memeriksa histori, bukan menutupi inkonsistensi. Advisory lock baru dilepas setelah seluruh pemeriksaan tersebut selesai, sehingga dua node aplikasi tidak dapat memproses nomor transaksi yang sama secara bersamaan.

Tabel nonce dan ledger sengaja dipisahkan. `online_bmt_nonce` mempunyai primary key sendiri dan merupakan satu-satunya penjaga keunikan nonce. Index nonce lama pada ledger dihapus saat startup dengan `DROP INDEX IF EXISTS` karena redundan dan hanya menambah biaya tulis. Ledger tetap mempunyai unique index `no_transaksi_bmt`, serta index baca `(no_invoice, status, id DESC)` untuk diagnosis dan pemeriksaan status.

Nominal dibandingkan memakai `BigDecimal` dengan toleransi satu sen terhadap `total + biayaAdmin`. BMT tidak boleh mengirim pembayaran parsial untuk satu invoice kecuali invoice yang diterbitkan sejak awal memang bernilai parsial sesuai pilihan pengguna. Invoice kedaluwarsa atau invoice yang telah dibayar oleh transaksi lain ditolak. Aplikasi desktop dan mobile tidak menambah saldo secara lokal, tidak mengantre posting finansial di outbox, dan tidak menampilkan sukses sebelum callback server terverifikasi.

### Urutan callback dan keputusan sistem

Pada `INQUIRY`, servlet memvalidasi envelope, menyimpan nonce, mencari `VirtualAccountBank` berdasarkan nomor persis tanpa mewajibkan `BankHost`, memverifikasi marker bank `Online BMT`, memeriksa gerbang tenant, tanggal kedaluwarsa, status belum lunas, identitas merchant, lalu mengembalikan nominal server. BMT harus memakai nominal dari inquiry sebagai sumber kebenaran dan tidak menghitung biaya admin sendiri. Invoice lunas ditolak agar teller/aplikasi BMT tidak menawarkan pembayaran kedua untuk tagihan yang sudah selesai.

Pada `PAYMENT`, servlet memvalidasi field dan mengambil advisory lock berdasarkan `NO_TRANSAKSI_BMT`. Di bawah lock, pasangan nomor transaksi, invoice, nominal, dan channel dibandingkan dengan ledger. Jika transaksi yang sama sudah `SUCCESS`, respons sukses idempoten dikembalikan. Jika invoice sudah lunas oleh transaksi berbeda, request ditolak. Jika baru, ledger `PROCESSING` di-commit terlebih dahulu, kemudian mesin posting kanonik dijalankan. Setelah bukti pembayaran terbentuk, ledger menjadi `SUCCESS`. Bila posting gagal tanpa bukti pembayaran, ledger menjadi `FAILED`. Bila proses terputus sesudah bukti terbentuk tetapi sebelum ledger sukses, retry dengan nonce baru dan nomor transaksi sama akan mendeteksi invoice lunas lalu memulihkan ledger tanpa posting kedua.

Pada `CHECK_STATUS_PAYMENT`, server tidak menganggap request BMT sebagai bukti bayar. Status `00` hanya diberikan bila ledger untuk nomor transaksi, invoice, nominal, dan channel yang sama berstatus `SUCCESS` sekaligus invoice mempunyai bukti lunas pada domain eCampus. Ada satu mekanisme pemulihan terkendali: bila ledger masih `PROCESSING`, keempat nilai tersebut identik, dan bukti lunas sudah ada, pemeriksaan status menaikkan ledger menjadi `SUCCESS` di bawah advisory lock yang sama dengan payment. Invoice dibaca **setelah** advisory lock diperoleh. Urutan ini penting: bila PAYMENT sedang berjalan, CHECK_STATUS menunggu commit-nya lalu membaca bukti terbaru, bukan memakai object invoice lama yang terambil sebelum menunggu lock. Kondisi `FAILED` tidak dipulihkan otomatis oleh check status karena membutuhkan audit atau retry payment kanonik. Bila data tidak cocok, hasilnya tetap belum terkonfirmasi. BMT harus membuat nonce baru pada setiap pemeriksaan status; memakai ulang nonce lama akan ditolak sebagai replay.

Pemetaan posting berdasarkan pemilik invoice:

| Kondisi invoice | Mesin posting |
| --- | --- |
| `topup > 0` dan tidak mempunyai rincian cicilan | `VirtualAccountBank.bayarTopup` untuk membentuk mutasi deposit. |
| Mempunyai `siswa` atau `calonSiswa` | `VirtualAccountBank.bayarSiswa` untuk pembayaran sekolah dan/atau topup siswa. |
| Selain dua kondisi di atas | `PembayaranGatewayHelper.prosesRincianVA` untuk mahasiswa/calon mahasiswa. |

`DepositAction` perlu dipahami secara khusus. Tombol Topup pada action tersebut adalah pencatatan topup langsung oleh operator berhak dan bukan transaksi Online BMT. Topup daring siswa dibuat melalui mode topup pada `PembayaranOnline`; topup mahasiswa, anggota koperasi, dan aplikasi klien dibuat melalui API/generator invoice. Pada semua jalur daring, saldo baru berubah setelah `PAYMENT` callback lolos verifikasi dan mesin posting selesai. Menambahkan panggilan simpan deposit langsung ke tombol pembuatan invoice akan menyebabkan kredit sebelum uang diterima dan dilarang oleh desain ini.

## Integrasi antarmuka dan API

Tombol Online BMT tersedia pada keluarga `DaftarUlangMahasiswa*Action`, wizard pembayaran, checkout web baru, `PembayaranOnline` sekolah, serta halaman top-up anggota koperasi apabila seluruh sakelar yang relevan aktif. API `tagihan_mahasiswa` dan `tagihan_siswa` menambahkan `Online BMT` pada `bank_va_mobile` secara deduplikasi. API pembuatan VA menerima nilai bank `Online BMT`, tetapi selalu memvalidasi sakelar tenant kembali. API top-up memiliki `topupCaraBayar`; aplikasi mobile mengambil daftar ini dari server dan tidak lagi mengasumsikan Smartlink sebagai satu-satunya kanal. Halaman web top-up juga memakai generator `DownloadTagihanAnggotaKoperasiBankOnline`, sehingga tidak memiliki algoritma posting atau penomoran BMT tersendiri.

API anggota/POS mengembalikan field `gateway` pada setiap pilihan. Nilai lama adalah `smartlink`; nilai Online BMT adalah `online_bmt`. eBisnis dan eCanteen meneruskan field tersebut ketika meminta invoice. Operasi ini sengaja online-only karena nomor invoice harus dibuat dan divalidasi server pada saat itu. Jika jaringan gagal, UI harus menyampaikan bahwa invoice belum dibuat; tidak boleh membuat referensi lokal atau menaikkan saldo sementara.

Cakupan implementasi yang harus tetap dipertahankan ketika ada refactor:

| Area | Jalur Online BMT |
| --- | --- |
| Daftar ulang mahasiswa baru/calon | `DaftarUlangMahasiswaBaruAction`; `DaftarUlangCalonMahasiswaAction` mewarisi action tersebut. |
| Daftar ulang mahasiswa lama | `DaftarUlangMahasiswaLamaAction`. |
| Wizard pembayaran mahasiswa | `WizardPembayaranMhsHelper` dan katalog gateway bersama. |
| Pembayaran/topup siswa | `PembayaranOnline` dan `DownloadTagihanSiswaBankOnline`. |
| Tagihan mahasiswa mobile/API | `TagihanMahasiswa`. |
| Tagihan siswa mobile/API | `TagihanSiswa`. |
| Topup mahasiswa/siswa/koperasi | `TopupHelper` dan route `topupCaraBayar`. |
| POS/kantin/member | `KantinMemberApi`, `KantinHelper`, serta halaman topup web. |
| Desktop eBisnis/eCanteen | Memakai field `gateway` dari server; tidak menyimpan saldo lokal. |
| Mobile AIS | Mengambil metode melalui `topupCaraBayar`, mengirim nama bank, lalu menampilkan link atau nomor VA dari server. |

Nama metode yang dikirim API untuk BMT adalah `Online BMT`; identifier internal pilihan gateway adalah `online_bmt`. Server menerima keduanya pada endpoint topup yang relevan, tetapi klien sebaiknya meneruskan nilai yang diberikan server, bukan membuat daftar hard-coded. Dengan cara ini default OFF dan perubahan kanal tenant langsung tercermin tanpa rilis ulang aplikasi.

## Prosedur rollout dan diagnosis

Sebelum aktivasi, deploy schema dan aplikasi terlebih dahulu, isi tiga secret produksi, tetapkan prefix dan biaya admin, lalu aktifkan global. Aktifkan satu tenant uji dan satu kanal uji. Buat invoice kecil, lakukan `INQUIRY`, lakukan `PAYMENT`, ulangi callback dengan nonce baru tetapi nomor transaksi sama, dan pastikan hanya satu pembayaran/deposit tercatat. Uji pula nonce yang sama, nominal salah, HMAC salah, timestamp lama, invoice kedaluwarsa, channel tidak dikenal, dan sakelar tenant OFF. Setelah seluruh hasil sesuai, kanal dapat diaktifkan bertahap untuk tenant lain.

Urutan rollout yang disarankan:

1. Deploy aplikasi yang memuat servlet, kolom sakelar, tabel nonce, dan ledger. Jangan aktifkan kanal.
2. Pastikan startup berhasil membuat `online_bmt_nonce` dan `online_bmt_request_guard` beserta index transaksi/invoice.
3. Untuk SIT, masukkan credential sample yang disetujui BMT ke konfigurasi server SIT; jangan masukkan ke source/default. Isi pula identitas mitra/merchant yang disepakati. Sebelum UAT/produksi, rotasi menjadi credential lingkungan tujuan.
4. Sinkronkan jam server eCampus dan BMT melalui NTP; selisih lebih dari toleransi akan ditolak.
5. Daftarkan callback `https://ecampus.staialbahjah.ac.id/albahjah/OnlineBmt` pada BMT.
6. Verifikasi dengan test vector bahwa response `DATA` dapat didekripsi BMT. Enkripsi response bersifat wajib dan tidak memiliki sakelar OFF.
7. Aktifkan global, satu PT/sekolah uji, dan satu kanal uji saja.
8. Jalankan pengujian positif dan negatif, lalu cocokkan invoice, ledger, pembayaran, cicilan, dan deposit.
9. Aktifkan tenant lain bertahap sambil memantau error dan transaksi `PROCESSING/FAILED`.

Checklist pengujian minimum:

| Skenario | Hasil yang diharapkan |
| --- | --- |
| Inquiry invoice valid | Identitas, nama, tanggal, deskripsi, dan nominal persis dikembalikan. |
| Inquiry invoice sudah lunas | Ditolak dengan kode bisnis `01`; tidak ditawarkan untuk pembayaran ulang. |
| API key/HMAC salah | Ditolak tanpa decrypt/posting finansial. |
| Timestamp lebih tua dari toleransi | Kode `408`. |
| Nonce digunakan ulang | Kode `409`, tidak ada posting kedua. |
| Nominal kurang/lebih | Ditolak, invoice tetap belum lunas. |
| Channel di luar whitelist | Kode `400`. |
| Tenant atau kanal OFF | Ditolak walaupun invoice pernah dibuat. |
| Payment valid | Tepat satu dokumen pembayaran/deposit dan ledger `SUCCESS`. |
| Retry transaction ID sama, nonce baru | Sukses idempoten; jumlah dokumen tetap satu. |
| Transaction ID sama untuk invoice/nominal/channel lain | Ditolak sebagai konflik. |
| Check status sebelum payment | `STATUS_TRANSAKSI=01`. |
| Check status sesudah payment | `STATUS_TRANSAKSI=00`. |
| Topup | Saldo tidak berubah saat invoice dibuat; berubah sekali sesudah callback payment. |

Query read-only berikut dapat membantu diagnosis. Sesuaikan nomor transaksi/invoice, dan jangan menjalankan `UPDATE/DELETE` manual sebagai jalan pintas:

```sql
SELECT no_transaksi_bmt, no_invoice, nominal, channel_bmt, status,
       response_code, response_message, created_at, updated_at
FROM public.online_bmt_request_guard
WHERE no_transaksi_bmt = '<NO_TRANSAKSI_BMT>'
   OR no_invoice = '<NO_INVOICE>'
ORDER BY id DESC;

SELECT nonce, request_type, created_at
FROM public.online_bmt_nonce
WHERE nonce = '<NONCE>';
```

Jika tombol tidak muncul, periksa sakelar global, sakelar PT atau sekolah, sakelar kanal, serta filter `JenisKegiatan.namaBankPembayaran` yang harus kosong atau mengandung `;online_bmt;`. Jika invoice tidak dapat dibuat, periksa tagihan terpilih, jadwal pembayaran, waktu kedaluwarsa, dan log generator `VirtualAccountBank`. Jika inquiry ditolak, pastikan nomor invoice berasal dari kanal Online BMT dan tenant masih aktif. Jika payment ditolak, periksa nominal total plus admin, channel, timestamp, nonce, dan pasangan transaksi-invoice pada ledger. Jika BMT melaporkan sukses tetapi saldo belum berubah, cari `NO_TRANSAKSI_BMT` di `online_bmt_request_guard`, lalu audit error posting kanonik. Jangan memperbaiki kondisi tersebut dengan insert saldo manual sebelum memastikan callback dan ledger, karena tindakan itu berisiko menggandakan saldo ketika callback dicoba ulang.

### Data yang perlu dikirim kepada BMT untuk memulai SIT

Sesudah aplikasi dan migrasi terpasang, operator eCampus perlu mengaktifkan gerbang global serta **hanya satu** PT/sekolah/kanal uji, lalu membuat invoice Online BMT kecil yang belum lunas dan belum kedaluwarsa. Nomor yang dikirim kepada BMT harus berasal dari invoice tersebut, bukan nomor pembayaran lama, bukan NIM, dan bukan placeholder dokumentasi. Tim BMT meminta contoh invoice agar mereka dapat menjalankan simulasi `INQUIRY`; setelah invoice dibuat, kirim `NO_INVOICE`, nominal yang diharapkan, nama pemilik yang boleh diverifikasi, dan batas waktu berlaku melalui kanal koordinasi SIT. Jangan mengirim API key, encryption key, atau HMAC key ke grup umum.

Di sisi infrastruktur, allowlist `103.157.96.112` dan `103.27.207.203` untuk akses `POST /albahjah/OnlineBmt`. Bila aplikasi berada di belakang CDN atau load balancer, aturan dibuat pada lapisan publik yang pertama menerima koneksi. Catat alamat sumber yang benar-benar terlihat pada access log saat uji pertama. Jika yang terlihat adalah alamat proxy internal, jangan menambah filter servlet berbasis `remoteAddr` secara tergesa-gesa; pastikan rantai trusted proxy dan aturan WAF lebih dahulu. Setelah request pertama masuk, cocokkan waktu server, hasil validasi HMAC, nonce, jenis request, dan nomor invoice. Respons sukses harus berbentuk envelope JSON dengan `STATUS=true`, `KODE_STATUS=00`, serta `DATA` string terenkripsi; BMT kemudian mendekripsi `DATA` untuk membaca payload bisnis.

Urutan SIT minimum adalah: `INQUIRY` invoice aktif; `PAYMENT` dengan nominal dan channel benar; `CHECK_STATUS_PAYMENT` menggunakan nonce baru; retry `PAYMENT` memakai nomor transaksi sama tetapi nonce baru; lalu pengujian negatif untuk HMAC salah, nonce berulang, nominal berbeda, invoice lunas, dan channel tidak dikenal. Setelah payment positif, verifikasi bukan hanya respons API, tetapi juga satu dokumen pembayaran/cicilan atau satu mutasi deposit, status invoice lunas, dan tepat satu ledger `SUCCESS`. Dengan pemeriksaan berlapis ini, status sukses tidak hanya berarti endpoint menjawab, melainkan uang telah dibukukan oleh mesin domain yang sama dengan kanal pembayaran lain.

Interpretasi status ledger: `PROCESSING` berarti request sudah diterima dan posting belum dipastikan selesai; lakukan `CHECK_STATUS_PAYMENT` atau retry `PAYMENT` dengan nonce baru dan nomor transaksi sama. `SUCCESS` berarti ledger dan bukti domain telah berhasil dipastikan. `FAILED` berarti posting tidak menghasilkan bukti lunas; penyebab teknis tersimpan secara ringkas pada `response_message` dan detail exception berada pada audit error aplikasi. Jangan mengubah `FAILED` menjadi `SUCCESS` melalui SQL. Retry kanonik diperlukan agar seluruh relasi cicilan, jurnal, status, dan saldo tetap konsisten.

Rollback operasional dilakukan dengan mematikan sakelar global. Ini segera menyembunyikan kanal dari daftar baru dan membuat callback ditolak tanpa menghapus invoice atau histori. Data nonce/ledger tidak boleh dihapus saat rollback karena masih dibutuhkan untuk mencegah replay ketika kanal dinyalakan kembali. Setelah akar masalah selesai, aktifkan kembali tenant uji terlebih dahulu dan lakukan check status terhadap transaksi yang sempat menggantung.

Untuk rotasi secret, koordinasikan waktu cutover dengan BMT. Karena implementasi hanya mempunyai satu set key aktif, perubahan sepihak akan membuat request gagal. Matikan kanal, tunggu request in-flight selesai, ubah tiga secret pada kedua sisi, lakukan test vector terenkripsi, lalu aktifkan tenant uji. Secret tidak boleh dicetak pada log. Payload audit yang disimpan hanya metadata transaksi yang diperlukan untuk penelusuran dan tidak boleh memuat key.

Prinsip pemeliharaan utama adalah satu sumber kebenaran: generator hanya membuat invoice, servlet hanya memverifikasi dan mengorkestrasi, sedangkan mutasi keuangan tetap dilakukan mesin pembayaran domain. Setiap kanal atau aplikasi baru harus menggunakan tiga lapisan yang sama, bukan menyalin algoritma posting atau mempercayai status dari klien.
