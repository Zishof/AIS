# Inisiatif Javadoc Menyeluruh AIS

Dimulai 1 Sep 2026. Tujuan: setiap class dan method di `ais/src/main/src` (WC utama
`svn://38.47.178.34/ais/src`, path lokal `C:\opt\AIS\ais\src\main\src`) punya Javadoc
yang benar, rinci, dan enak dibaca — baik oleh manusia maupun oleh sesi AI lain yang
membaca ulang kode ini di kemudian hari.

## Skala

Diukur langsung 1 Sep 2026 (skrip regex method+javadoc di seluruh pohon sumber):

- 7.401 file `.java`
- kira-kira 127.082 deklarasi method (public/protected/private, termasuk konstruktor
  dan method di nested class)
- hanya kira-kira 14.062 (~11%) yang punya blok `/** ... */` tepat sebelum
  deklarasinya
- kira-kira 1.589 file sudah punya Javadoc **template generik** untuk nested/local
  class hasil audit otomatis sebelumnya (contoh gaya: *"Kontrak yang tampak dari
  deklarasi ini meliputi..."*) — ini bukan penjelasan spesifik per class, jadi
  dihitung "ada tapi dangkal", bukan "sudah selesai". Jangan hapus, tapi perkaya bila
  menyentuh file itu.
- kira-kira 2.373 file punya marker `auto-audit(empty-catch)` — ini dari inisiatif
  LAIN (audit blok catch kosong), TIDAK berkaitan dengan Javadoc. Jangan tertukar.

Kesimpulan: ini bukan pekerjaan yang selesai dalam satu atau beberapa sesi. Perkiraan
realistis: puluhan sampai ratusan sesi kerja, dikerjakan bertahap per file/package.

## Strategi: class referensi + link

Codebase ini punya banyak pola "class induk generic + banyak subclass tipis yang
cuma memanggil `super(...)`" (contoh: `GenericRevisiHelper<T>` dasar bagi 50+ class
`Revisi*Helper`). Menulis dokumentasi identik berulang-ulang di setiap subclass boros
dan mudah basi (kalau perilaku induk berubah, ratusan Javadoc subclass ikut salah).

Aturan main:

1. **Class/method induk** (dipakai berulang lewat pewarisan, composition, atau
   dipanggil dari banyak tempat dengan pola sama) → Javadoc SANGAT detail dan rinci
   di situ: apa tujuannya, bagaimana alurnya, efek samping (DB/session/UI), parameter,
   return, exception, contoh pemakaian, jebakan yang pernah ditemukan. Target
   minimal ±500 kata untuk class-level Javadoc pada class semacam ini.
2. **Subclass/pemanggil tipis** → Javadoc singkat yang menjelaskan KEKHASannya saja
   (entity apa, field pencarian apa, kustomisasi apa) + tag `@see` atau `{@link}`
   yang mengarah eksplisit ke class/method induk. Di IDE atau Javadoc HTML hasil
   generate, link ini bisa diklik langsung ke referensi lengkapnya.
3. Method sepele yang truly generik di banyak tempat dengan bentuk identik (getter/
   setter Hibernate entity biasa, dsb.) boleh Javadoc singkat 1-2 baris + `@see`
   ke penjelasan pola getter/setter standar (akan dibuat referensinya saat giliran
   paket `ais.database.model` dikerjakan).
4. Bahasa Javadoc: Bahasa Indonesia, konsisten dengan gaya yang sudah dipakai di
   repo ini (istilah domain: posting, jurnal, revisi, dsb — lihat dok `pos/` untuk
   kosakata akunting/posting).
5. JANGAN mengurangi atau menghapus Javadoc yang sudah ada — hanya menambah/
   memperkaya. Kalau Javadoc lama sudah salah/basi (mendokumentasikan cacat yang
   sudah diperbaiki), mutakhirkan isinya, jangan dihapus begitu saja.

## Alur kerja per file (wajib ikuti aturan repo SVN)

1. `svn status` + `svn log -l` dulu pada file/folder target — sesi paralel sering
   sedang mengerjakan area yang sama. File yang berstatus `M` oleh sesi lain jangan
   disentuh.
2. Baca & pahami kode (bukan cuma tebak dari nama method).
3. Tulis/lengkapi Javadoc.
4. Cek EOL CRLF (`grep -cU`) — file Java di repo ini murni CRLF.
5. Kompilasi verifikasi: `javac -source 1.7 -target 1.7 -encoding UTF-8 -nowarn -cp
   'C:\opt\AIS\ais\src\main\webapp\WEB-INF\lib\*' -sourcepath . -d <scratch>` dari
   akar WC src.
6. `svn commit` PER BERKAS, pesan lewat file `-F` (bukan `-m`), SEGERA setelah lulus
   kompilasi — jangan ditunda, WC ini dipakai bersama banyak sesi paralel.
7. Mirror ke `C:\opt\AIS\ais\src\main\java` (WC kedua dari path repo yang sama):
   copy file → `svn update` file itu di mirror → `cmp` untuk verifikasi identik byte.
8. Update `PROGRESS.md` di folder ini.

## Status

Lihat [PROGRESS.md](PROGRESS.md) untuk daftar file/package yang sudah dikerjakan.
