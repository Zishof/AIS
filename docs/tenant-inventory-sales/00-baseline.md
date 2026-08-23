# 00 — Baseline P0 multi-tenant Inventory & Sales

Tanggal: 2026-08-23. Deliverable **FASE P0 §8.1**.

## 1. Lingkungan terverifikasi

| | |
|---|---|
| JDK | Eclipse Adoptium 8.0.502.7 (`javac 1.8.0_502`) |
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-8.0.502.7-hotspot\` |
| Ant | terpasang, `ANT_HOME=C:\opt\apache-ant-1.10.15` |
| **Maven** | **TIDAK terpasang** — tidak ada di PATH git-bash maupun PowerShell |

## 2. Tidak ada build siap pakai — dan baseline lama tidak sahih

**`mvn -o compile` tidak dapat dijalankan**: perintahnya tidak ada
(`command not found`, EXIT=127). Hanya repo lokal `~/.m2` yang tertinggal.

**Lebih penting: pom-nya memang tidak mengompilasi apa pun.**

```xml
<sourceDirectory>src/main/empty-maven-source</sourceDirectory>
<warSourceDirectory>src/main/webapp</warSourceDirectory>
```

`pom.xml` hanya membungkus WAR. Karena itu catatan
`docs/pendaftaran-tenant/00-baseline.md` — *"Build kanonik `mvn -o compile`,
hasil baseline EXIT=0"* — **benar secara hampa**: nol berkas Java dikompilasi,
sehingga hasilnya EXIT=0 apa pun keadaan kodenya. Itu tidak dapat dipakai
membedakan regresi.

**Ant pun tidak cocok apa adanya.** `ant/build.xml` memakai `basedir=".."`
dan mencari `web/` serta `resources/` di `C:\opt\AIS\ais`; pohon ini berbentuk
Maven (`src/main/webapp`). Keduanya tidak ada.

## 3. Baseline yang dipakai: javac langsung

Satu-satunya jalur kompilasi yang bekerja di mesin ini:

```bash
cd /c/opt/AIS/ais/src/main
CP=""; for j in webapp/WEB-INF/lib/*.jar; do CP="$CP;$(cygpath -w $j)"; done
find java -name "*.java" > srclist.txt
javac -J-Xmx3g -nowarn -proc:none -encoding UTF-8 -source 1.7 -target 1.7 \
      -cp "$CP" -sourcepath java -d <keluaran> @srclist.txt
```

> `-sourcepath java` wajib eksplisit. Tanpa itu javac ikut membaca cermin
> `src/`, sehingga berkas yang belum disalin ke sana memberi lulus palsu.

### Hasil baseline

| | |
|---|---|
| Berkas sumber | **7.058** (`java/`; cermin `src/` juga 7.058) |
| Jar classpath | 184 (`webapp/WEB-INF/lib`) |
| Kelas dihasilkan | **40.182** (251 MB) |
| **Error** | **0** |
| Peringatan | 0 (hanya 4 catatan deprecation/unchecked) |
| Exit | **0** |

**Setiap error kompilasi sesudah ini adalah regresi.**

## 4. Baseline uji

Tidak ada JUnit: **0** berkas memakai `org.junit`, tidak ada jar junit, tidak
ada surefire. Yang ada **80 kelas `*SelfTest`**, semuanya ber-`main()` dan
berdiri sendiri (melempar bila gagal, mencetak `... OK` lalu `System.exit(0)`).
Tidak satu pun dipanggil dari kode produksi.

| Hasil | Jumlah | Arti |
|---|---|---|
| **LULUS** | **43** | berjalan tanpa DB |
| **GAGAL** | **23** | **menolak jalan dengan sengaja** — menuntut clone DB atau variabel lingkungan (`"Test wajib diarahkan ke clone"`, `"Environment wajib: AIS_JU..."`). Penjaga agar uji tidak menyentuh produksi. **Bukan regresi.** |
| **TIMEOUT** | 14 | menunggu sambungan basis data (batas 12 detik) |

> Catatan runner: `NewUiNativeJspResolverSelfTest.java` berada di direktori
> `menu/test/` tetapi mendeklarasikan `package ais.common.newui.menu;`. Nama
> kelas yang diturunkan dari path meleset; dijalankan dengan nama paket yang
> benar hasilnya **LULUS**. Satu-satunya berkas dengan ketidakcocokan ini.

## 5. Topologi working copy — perlu diketahui sebelum commit

`C:\opt\AIS` **bukan** working copy. Yang berstatus WC hanya subpohonnya, dan
tata letak lokalnya berbeda dari tata letak repo:

| Lokal | SVN | Revisi saat baseline |
|---|---|---|
| `src/main/java` | `^/src` | 78126 |
| `src/main/src` | `^/src` | 78126 |
| `src/main/webapp` | `^/web` | 78126 |
| `src/main/docs` | `^/docs` | 78126 |
| `ais/ant` | `^/ant` | 77756 |
| **`ais/docs`** | **`^/docs`** | **77741** |

> **Catatan.** Dua WC menunjuk `^/docs` yang sama; `ais/docs` tertinggal 385
> revisi. **Bahayanya bukan commit diam-diam** — SVN menolak commit dari WC
> kedaluwarsa dengan galat *out of date*, jadi gagalnya berisik. Yang benar-benar
> dapat mengembalikan isi lama adalah `svn update` lalu konflik diselesaikan
> memihak sisi usang, atau berkas lama disalin menimpa. Saat diperiksa `ais/docs`
> bersih dan belum punya `pos/`, jadi tidak ada bahaya aktif — ia hanya
> membingungkan soal WC mana yang dipakai. Yang benar **`src/main/docs`**.
>
> Yang justru pantas ditindak: dua checkout staging punya suntingan menggantung
> seribu revisi lebih di belakang — `AIS-svn-spmi-commit` (r77110, 2 kotor) dan
> `AIS-svn-spmi-zul-commit` (r77110, 1 kotor). Sembilan lainnya bersih.
>
> `java/` dan `src/` dua-duanya memetakan ke `^/src`. Prosedurnya: commit dari
> SATU sisi, `svn update` sisi lain, lalu verifikasi md5.
