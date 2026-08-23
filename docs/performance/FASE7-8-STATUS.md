# Fase 7-8 — Status (2026-08-19)

## Sudah selesai (sebagian besar dikerjakan sesi paralel)
| Item | Status |
|---|---|
| P0: skrip `.sh` / dump `.sql` / `.bak` dapat diunduh via HTTP | **DITUTUP** — `security-constraint` + `auth-constraint` kosong (deny-all) di `web.xml` |
| JasperServlet produksi | **SELESAI** — `development=false`, `mappedfile=false`, `maxLoadedJsps=500`, `jspIdleTimeout=1800` |

`maxLoadedJsps=500` penting: dari 10.252 JSP, Tomcat kini hanya menahan 500 kelas JSP
sekaligus dan melepas yang menganggur >30 menit — menekan Metaspace dan mempercepat redeploy.

## TINDAK LANJUT PEMILIK SISTEM (tidak bisa dikerjakan dari sini)
1. **ROTASI password SVN** yang terlanjur tertulis plaintext di `update.sh`,
   `update-web-auto.sh`, dan `commit.sh`. Akses web sudah ditutup, TETAPI nilainya
   masih ada di berkas dan di RIWAYAT SVN.
2. Pindahkan skrip deployment ke luar direktori `webapp/`.
3. Pertimbangkan pembersihan riwayat SVN sebagai pekerjaan keamanan terpisah.

## Belum dikerjakan — butuh keputusan + smoke test

### 1. `servlet_.jar` (80 KB) — REKOMENDASI: BIARKAN untuk sekarang

Terverifikasi: 83 entri, **seluruhnya `javax/servlet/**`, nol kelas lain** — Servlet+JSP API
yang secara prinsip wajib disediakan container, bukan dibundel di `WEB-INF/lib`.

**Riwayat penting (info pemilik, 19-08-2026):** JAR ini pernah dihapus dan **build Ant jadi
error**. Penyebabnya jelas dan bukan kesalahan penghapusan itu sendiri: classpath kompilasi
Ant mengambil `javax.servlet` DARI `WEB-INF/lib`. Begitu JAR-nya hilang, kompilasi kehilangan
Servlet API (`package javax.servlet does not exist`).

**Penilaian ulang biaya-manfaat:**
- Manfaat: hemat 80 KB + sedikit pengurangan beban scan JAR/TLD. KECIL.
- Risiko runtime saat ini: RENDAH — Tomcat 9 sudah menyaring `javax.servlet.*` dari
  `WEB-INF/lib` (WebappClassLoader), jadi tidak menimbulkan LinkageError pada versi ini.
- Biaya: menyentuh skrip build yang sudah terbukti rapuh untuk perubahan ini.

**Kesimpulan: JANGAN dihapus sekarang.** Tidak sepadan. Kerjakan HANYA bila suatu saat
build Ant memang sedang dirapikan, atau bila naik ke container yang tidak menyaring
`javax.servlet` dari WEB-INF/lib.

**Bila nanti dikerjakan**, urutannya WAJIB begini (kebalikan dari percobaan lalu):
1. LEBIH DULU tambahkan Servlet API container ke classpath KOMPILASI di `build.xml`:
   ```xml
   <path id="compile.classpath">
       <fileset dir="${webapp}/WEB-INF/lib" includes="*.jar"/>
       <!-- Servlet/JSP API disediakan Tomcat: dipakai saat COMPILE, TIDAK ikut ke WAR -->
       <fileset dir="${catalina.home}/lib" includes="servlet-api.jar,jsp-api.jar,el-api.jar"/>
   </path>
   ```
   (`${catalina.home}` = folder Tomcat; ketiga jar itu standar ada di `lib/` Tomcat 9.)
2. Pastikan `<war>`/`<lib>` TIDAK menyertakan jar dari `${catalina.home}/lib`.
3. Verifikasi `ant compile` hijau.
4. BARU `svn delete WEB-INF/lib/servlet_.jar`, lalu startup Tomcat + smoke test.

### 2. Kandidat exclude WAR (±345 MB, risiko rendah)
`help/` 197 MB, `WEB-INF/website` 128 MB, `WEB-INF/baru/tbu_penawaran` 20 MB,
`WEB-INF/lib-zk9-ce` 6,5 MB, `WEB-INF/sapto` 1,5 MB, `report/**.bak`.

BELUM dikerjakan karena proyek TIDAK punya `build.xml`/`pom.xml`: WAR tidak dibangun dari
skrip build, melainkan `webapp/` disalin langsung ke Tomcat oleh `update-web-auto.sh`.
Jadi "exclude dari WAR" harus diwujudkan sebagai daftar pengecualian di skrip deployment
itu — perubahan pada proses rilis, bukan pada kode. Perlu persetujuan pemilik.

### 3. Konsolidasi 8.989 JSP scaffolding (Fase 7 inti)
Belum disentuh. Dampak terbesar ada pada Metaspace & waktu deploy, tetapi menyentuh routing
URL sehingga butuh parity test per modul. `maxLoadedJsps=500` sudah meredam gejalanya.
