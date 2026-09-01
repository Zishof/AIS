# Progres Javadoc Menyeluruh

Format tiap baris: `- [status] path/File.java — catatan singkat (revisi svn, tanggal)`

Status: `[referensi]` = class induk/pola sudah didokumentasikan sangat detail (jadi
target link dari class lain), `[tautan]` = subclass/pemanggil tipis sudah ditautkan
ke referensi, `[lengkap]` = file berdiri sendiri sudah didokumentasikan penuh tanpa
perlu referensi eksternal, `[sebagian]` = baru sebagian method, `[belum]` = belum
disentuh (default untuk semua file yang tidak disebut di sini).

## ais/action/master/helper/

- (diisi setelah GenericRevisiHelper.java selesai — lihat catatan sesi 1 Sep 2026 di
  bawah)

## Catatan sesi

### 1 Sep 2026 (sesi awal inisiatif)

- Diukur skala penuh codebase (lihat README.md).
- Dibuat tracker ini + memory `ais-inisiatif-javadoc-menyeluruh`.
- Target pertama dipilih: `ais/action/master/helper/GenericRevisiHelper.java`
  (6.319 baris, dasar 50 subclass `Revisi*Helper`) — leverage tertinggi yang
  ditemukan sejauh ini untuk pola referensi+link.
