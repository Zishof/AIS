# CRUD ZKoss dan Menu Modul Sosial

## Menu otomatis

`MenuHelper.ensureSosialMenus()` dipanggil dari `AppStartupListener` dan bersifat idempoten. Seeder mempertahankan induk `Modul Sosial` pada branch `600000008`, membuat menu berikut, menghubungkannya ke role Administrator `am`, dan memastikan `read/create/update/delete/approve/reject = 1`.

| ID | Menu | URL |
|---:|---|---|
| 73329301 | Zakat | `/pages/master/sosial/zakat_workspace.zul` |
| 73329302 | Infaq | `/pages/master/sosial/infaq_workspace.zul` |
| 73329303 | Shodaqoh | `/pages/master/sosial/shodaqoh_workspace.zul` |
| 73329304 | Donasi | `/pages/master/sosial/donasi_workspace.zul` |
| 73329305 | SosialChannel | `/pages/master/sosial/sosial_channel.zul` |

Seeder juga memberikan akses penuh Administrator untuk menu sosial lama `33294–33298`.

## Struktur halaman

- Tab pertama: dashboard ringkas per kategori/channel.
- Tab kedua: CRUD ZKoss dengan pencarian, paging, privilege tombol, tambah, ubah, dan hapus.
- Transaksi finansial selain status `DRAFT` tidak menyediakan tombol ubah/hapus. Koreksi record posted harus memakai correction/reversal workflow.
- Form `SosialChannel` dapat memetakan channel ke master Zakat, Infaq, Shodaqoh, dan Donasi. Master `JenisDanaSosial` dibuat idempoten ketika pemetaan dipilih.
- Password Smartlink dan callback secret tidak pernah ditampilkan kembali; nilai kosong saat edit mempertahankan secret lama.

## Database lokal yang diverifikasi

Menu dan privilege telah diterapkan pada database `ais` localhost untuk role `am`. Verifikasi menghasilkan sebelas menu sosial dengan `job_has_menu` tersedia, seluruh enam privilege bernilai `1`, dan tidak ditemukan privilege duplikat.

Setelah deployment, logout/login ulang atau bersihkan cache menu sesi agar menu baru langsung tampil.
