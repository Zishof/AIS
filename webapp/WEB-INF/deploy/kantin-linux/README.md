# Installer Linux e-Kantin

Direktori ini membangun satu berkas installer mandiri `ais-kantin-linux-<arsitektur>.run`.
Installer tersebut sudah berisi:

- Eclipse Temurin JRE 8;
- Apache Tomcat 9;
- WAR AIS yang diberikan saat build;
- service `systemd` dan alat konfigurasi ulang.

Server tujuan tidak memerlukan Java, Tomcat, Git, atau akses internet. Pengguna hanya
memasukkan host, port, username, dan password PostgreSQL utama serta streaming melalui
terminal. Nama context dan database ditetapkan sebagai berikut agar cocok dengan mekanisme
`AppStartupListener`:

- context aplikasi: `kantin`;
- database utama: `kantin`;
- database streaming: `streaming_kantin`.

> AIS masih merupakan aplikasi monolitik. Paket ini menyediakan runtime/deployment khusus
> e-Kantin; akses menu lain tetap dikendalikan oleh role `Kantin` di aplikasi.

## Membuat installer

Jalankan di Linux/WSL pada mesin build yang memiliki internet:

```bash
chmod +x build-installer.sh
./build-installer.sh --war /path/ke/ais.war
```

Hasil default berada di `dist/ais-kantin-linux-x64.run`. Arsitektur ARM64 dapat dibuat dengan:

```bash
./build-installer.sh --war /path/ke/ais.war --arch aarch64
```

Versi bawaan saat ini adalah Tomcat `9.0.120` dan JRE Temurin 8 terbaru dari API resmi
Adoptium. Builder memverifikasi SHA-512 Tomcat dari Apache dan SHA-256 JRE dari metadata
Adoptium. Versi/URL dapat dipin melalui environment variable yang didokumentasikan oleh
`./build-installer.sh --help`.

## Instalasi pada server Linux

```bash
chmod +x ais-kantin-linux-x64.run
sudo ./ais-kantin-linux-x64.run
```

Nilai yang diminta:

1. host dan port database utama;
2. username dan password database utama;
3. host dan port database streaming;
4. username dan password database streaming.

Password tidak ditampilkan di layar. Installer melakukan pemeriksaan koneksi TCP sebagai
peringatan, menginstal ke `/opt/ais-kantin`, menulis konfigurasi ke
`/opt/.g/.h/kantin.txt`, lalu membuat dan menjalankan service `ais-kantin.service`.

Alamat aplikasi setelah aktif:

```text
http://ALAMAT-SERVER:8080/kantin/
```

Port HTTP dapat diubah tanpa prompt saat instalasi:

```bash
sudo AIS_HTTP_PORT=8181 ./ais-kantin-linux-x64.run
```

## Operasional

```bash
sudo systemctl status ais-kantin
sudo journalctl -u ais-kantin -f
sudo systemctl restart ais-kantin
sudo /usr/local/sbin/ais-kantin-configure
```

Konfigurasi ulang menghentikan service, menulis kredensial baru, menghapus direktori hasil
ekstraksi WAR (bukan WAR sumber), lalu menyalakan service agar placeholder Hibernate dibuat
ulang dari WAR yang bersih.

## Upgrade dan rollback

Menjalankan installer versi baru membuat release bertimestamp baru dan mengarahkan symlink
`/opt/ais-kantin/current` ke release tersebut. Release lama tidak langsung dihapus sehingga
rollback dapat dilakukan dengan mengubah symlink `current`, lalu restart service.

## Prasyarat server

- Linux x86_64 atau aarch64 dengan `systemd`;
- user `root` saat instalasi;
- PostgreSQL utama dan streaming sudah dibuat dan dapat diakses;
- kredensial memiliki hak yang diperlukan oleh AIS untuk migrasi Hibernate.

