package ais.database.model.file;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
import java.util.Date;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.Common;

/**
 * Entity <b>penyimpan berkas lampiran serba-guna</b> untuk SELURUH modul AIS (tabel
 * {@code public.lampiran_lain}). Hampir setiap berkas yang diunggah pengguna di sistem ini —
 * KTP, Kartu Keluarga, akte, ijazah, foto rumah pendaftar, bukti pembayaran, gambar tanda
 * tangan (TTD) dosen/guru/pegawai, kop surat, stempel, logo, background halaman login,
 * berkas template JasperReports ({@code .jrxml}), skripsi, silabus, SAP, sampai berkas
 * rekonsiliasi host-to-host bank — bermuara ke SATU tabel ini.
 *
 * <h3>Kenapa satu tabel untuk semua</h3>
 * <p>AIS tidak membuat tabel lampiran per modul. Sebagai gantinya dipakai pola
 * <b>polymorphic association tanpa foreign key</b>: satu baris {@code LampiranLain}
 * mengaitkan diri ke "pemilik"-nya lewat sepasang kolom biasa:</p>
 * <ul>
 *   <li>{@link #getRef() ref} — {@code Long} berisi <b>nilai primary key baris pemilik</b>
 *       (mis. {@code Mahasiswa.id}, {@code CatatanSiswa.id}, {@code Peraturan.id},
 *       {@code Jurusan.id}). Kolomnya <b>bukan</b> foreign key dan tidak punya constraint
 *       apa pun ke tabel mana pun.</li>
 *   <li>{@link #getJenis() jenis} — {@code String} penanda "lampiran yang mana". Nilainya
 *       <b>tidak seragam</b>: kadang label manusiawi dari konstanta di kelas ini
 *       (mis. {@link #TTD_DOSEN "TTD Dosen"}, {@link #KOP_SEKOLAH "KOP Sekolah"}), kadang
 *       <b>nama kelas Java pemilik secara fully-qualified</b>
 *       (mis. {@code Peraturan.class.getName()}, seperti dipakai
 *       {@code GolonganAction}/{@code KenaikanPangkatHelper}), kadang label bebas yang
 *       ditulis langsung oleh layar pemanggil.</li>
 * </ul>
 * <p><b>Konsekuensi penting:</b> karena {@code ref} hanyalah angka polos, nilai {@code ref}
 * dari tabel pemilik yang berbeda <b>bertabrakan di ruang angka yang sama</b>
 * ({@code Peraturan#5} dan {@code Mahasiswa#5} sama-sama {@code ref = 5}). Satu-satunya
 * pembeda adalah string {@code jenis}. Tidak ada constraint basis data, tidak ada
 * discriminator kelas, dan tidak ada validasi apa pun yang menjamin bahwa pasangan
 * ({@code ref}, {@code jenis}) yang diminta memang milik entitas yang dimaksud pemanggil.</p>
 *
 * <h3>Jalur pencarian: {@code ambil(...)}</h3>
 * <p>Seluruh pembacaan lampiran melewati {@link #ambil(Long, String)} dan saudaranya, yang
 * mendelegasikan ke {@code FileFotoLain.ambil(usingId, ref, jenis, jumlahCoba, clazz, refresh,
 * kondisiTambahan)}. Ringkas cara kerjanya (lihat {@link ais.database.model.file.FileFotoLain}):</p>
 * <ol>
 *   <li>Cek <b>cache berkas di disk</b> lebih dulu ({@code ambilLokasi}) — hasil query
 *       sebelumnya disimpan sebagai JSON per kunci {@code clazz + ref + jenis}. Cache ini
 *       <b>global per server, bukan per pengguna</b>.</li>
 *   <li>Bila cache kosong/basi/{@code refresh=true}, jalankan Criteria:
 *       {@code Restrictions.eq("jenis", jenis)} (kecocokan <b>persis</b>, case-sensitive)
 *       DAN {@code Restrictions.eq("ref", ref)}, diurutkan {@code Order.desc("id")} dengan
 *       {@code setMaxResults(1)} — jadi yang dikembalikan adalah baris <b>terbaru</b>
 *       untuk pasangan tersebut.</li>
 *   <li>Bila {@code usingId=true}, semantiknya <b>berubah total</b>: filter {@code jenis}
 *       DIMATIKAN dan pencocokan berpindah ke {@code Restrictions.idEq(ref)}, yaitu primary
 *       key baris {@code lampiran_lain} itu sendiri.</li>
 * </ol>
 * <p><b>Tidak ada satu pun pemeriksaan kepemilikan, peran, tenant, atau hak akses di level
 * entity ini maupun di kelas induk.</b> {@code ambil()} adalah query murni; yang boleh
 * dilihat siapa sepenuhnya menjadi tanggung jawab pemanggil.</p>
 *
 * <h3>Implikasi keamanan — WAJIB dibaca sebelum menyentuh kelas ini</h3>
 * <p>Kelas ini adalah entity yang mendasari temuan keamanan terbesar inisiatif ini. Rujukan:
 * dokumen {@code src/ais/action/servlet/SECURITY_FINDING_AmbilLampiran_IDOR.md} (status
 * TERBUKA) dan task eskalasi <b>{@code task_b82b25d2}</b>.</p>
 * <ul>
 *   <li>Servlet {@code ais.action.servlet.AmbilLampiran} dipetakan ke DUA url-pattern:
 *       {@code /AmbilLampiran} dan alias {@code /al} (lihat {@code web.xml}). Servlet itu
 *       menerima {@code ref}, {@code clazz}, {@code jenis}, {@code usingId}, {@code jurusan},
 *       {@code download} <b>langsung dari query string tanpa enkripsi/tanda tangan</b>
 *       sebagai fallback bila token terenkripsi {@code d} tidak ada, dan <b>default
 *       {@code clazz} adalah kelas ini</b>.</li>
 *   <li>Dokumen temuan tersebut <b>sudah usang</b> pada bagian prasyarat akses: ia menyatakan
 *       {@code /al} digerbangi {@code IS_AUTHENTICATED_REMEMBERED}. Sejak <b>19-08-2026</b>
 *       {@code applicationContext-security.xml} menyetel {@code /al} dan {@code /al/**} ke
 *       <b>{@code IS_AUTHENTICATED_ANONYMOUSLY}</b> — publik, tanpa login sama sekali.
 *       Komentar risiko di berkas konfigurasi itu sendiri mengakui hal ini dan mitigasinya
 *       belum dikerjakan.</li>
 *   <li>Kolom {@code id} berstrategi {@link javax.persistence.GenerationType#IDENTITY} sehingga
 *       <b>berurutan dan mudah ditebak</b>, begitu pula {@code ref} yang menyalin PK baris
 *       pemilik. Kombinasi ini + {@code usingId=true} (yang mematikan filter {@code jenis})
 *       membuat seluruh isi tabel dapat dienumerasi hanya dengan menaikkan satu angka.</li>
 *   <li>{@link #getUrl()} dan {@code createLinkUri()} <b>menyalin isi berkas ke direktori media
 *       publik</b> dan dapat menghasilkan URL statis {@code /f<prefix>/<Kelas>/<id>/<nama>}
 *       yang melewati servlet sepenuhnya — sehingga penambalan di level servlet saja tidak
 *       menutup jalur ini.</li>
 * </ul>
 * <p>Jangan menambah pemanggil baru yang membangun URL {@code /AmbilLampiran?ref=..&amp;clazz=..}
 * secara manual; pakai {@code FileFotoLain.ambilLinkLampiranLain(...)} yang menghasilkan token
 * {@code d} terenkripsi.</p>
 *
 * <h3>Bentuk penyimpanan isi berkas</h3>
 * <p>Ada tiga kemungkinan, diperiksa berurutan oleh jalur unduh:</p>
 * <ol>
 *   <li><b>Google Drive</b> — {@link #getGdrive() gdrive} berisi file-id Drive; bila terisi,
 *       {@link #getFoto()} sengaja mengembalikan {@code null}.</li>
 *   <li><b>Blob di basis data</b> — kolom {@code foto} bertipe PostgreSQL Large Object
 *       ({@code oid}); wajib dibaca/ditulis di dalam transaksi non-autocommit (lihat catatan
 *       panjang di {@code FileFotoLain.createFileFotoLain}). Kolom ini {@code @NotAudited}
 *       agar Envers tidak menyalin isi berkas ke tabel revisi.</li>
 *   <li><b>Berkas fisik / link luar</b> — {@link #getLokasiFisik() lokasiFisik} atau
 *       {@link #getLink() link} untuk dokumen yang hanya berupa tautan.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Konstanta {@code jenis}</b> (mayoritas isi berkas ini) — nilai baku untuk kolom
 *       {@code jenis}, dikelompokkan per modul: identitas/TTD, kop &amp; stempel, aset
 *       branding (logo/background/favicon), PMB/PSB, kartu anggota perpustakaan, tombol
 *       payment gateway, dan template {@code .jrxml}.</li>
 *   <li><b>Konstanta {@code ref} sintetis</b> ({@code Long} bernilai <b>negatif</b>) — untuk
 *       lampiran yang tidak punya baris pemilik, mis. {@link #ID_SKIN}, {@link #FAVICON},
 *       {@link #LOGO_DEPAN}. Angka negatif dipakai agar tidak bertabrakan dengan PK asli;
 *       lihat juga {@code Common.refSementara()}.</li>
 *   <li><b>Properti terpetakan</b> — {@code id}, {@code nama}, {@code keterangan},
 *       {@code deskripsi}, {@code link}, {@code ref}, {@code jenis}, {@code foto},
 *       {@code gdrive}, {@code gdriveUsername}, {@code lokasiSimpan}, {@code lokasiFisik},
 *       {@code copyDari}, plus properti audit {@code oleh}/{@code olehId}/{@code
 *       tanggal_dirubah}.</li>
 *   <li><b>Query statis</b> — {@link #ambil(Long, String)} dan varian, serta
 *       {@link #resetLokasi(Boolean, Long, String)} untuk membuang cache disk.</li>
 *   <li><b>Pabrik komponen ZK</b> — tujuh overload
 *       {@link #createDownloadUploadFileLain(org.zkoss.zk.ui.Component, Long, String, String, Boolean, org.zkoss.zk.ui.event.EventListener)}
 *       yang merakit tombol Download/Ganti/Hapus pada layar.</li>
 *   <li><b>Implementasi kontrak induk</b> — {@link #ambilRef()}, {@link #ambilJenis()},
 *       {@link #ambilLink()}, {@link #ambilClazz()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious (mudah salah paham)</h3>
 * <ul>
 *   <li><b>Deklarasi ulang properti warisan BUKAN bug.</b> {@link ais.database.model.GeneralValueObject},
 *       {@code FileFoto}, dan {@code FileFotoLain} semuanya POJO abstrak biasa — tidak ada
 *       {@code @Entity} maupun {@code @MappedSuperclass}. Hibernate tidak memetakan properti
 *       kelas induk, sehingga {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah},
 *       {@code nama}, {@code keterangan}, {@code foto}, {@code lokasiSimpan} <b>harus</b>
 *       dideklarasikan ulang di sini agar tersimpan. Lihat
 *       {@link ais.database.model.GeneralValueObject}.</li>
 *   <li><b>Beberapa getter menulis balik ke field.</b> {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #getDeskripsi()}, {@link #getJenis()},
 *       {@link #getLink()}, {@link #getGdrive()}, {@link #getGdriveUsername()} semuanya
 *       memodifikasi field instance saat dibaca. Karena entity ini memakai <i>property
 *       access</i>, getter-getter itulah yang dipanggil Hibernate saat dirty-check, sehingga
 *       sekadar <b>membaca</b> objek terkelola bisa menghasilkan {@code UPDATE} nyata ke
 *       basis data. Yang paling berdampak: {@link #getNama()} menempelkan awalan
 *       {@code "<id>_"} ke nama berkas.</li>
 *   <li><b>{@code copyDari} adalah delegasi, bukan sekadar jejak duplikasi.</b> Bila terisi,
 *       hampir semua getter membaca nilai dari baris sumber dan <b>menimpa</b> nilai lokal.</li>
 *   <li>{@link #setOleh(String)} dan {@link #setOlehId(String)} <b>menolak diam-diam</b> nilai
 *       kosong/{@code null} — memanggilnya dengan nilai kosong tidak menghapus isi lama.</li>
 * </ul>
 *
 * <p><b>Catatan generator:</b> komentar asli "Bank generated by hbm2java" adalah sisa
 * salin-tempel dari {@code ais.database.model.Bank} (Apr 2010) dan tidak menggambarkan
 * kelas ini; komentar tersebut diganti oleh dokumentasi ini.</p>
 *
 * @see ais.database.model.file.FileFotoLain
 * @see ais.database.model.file.FileFoto
 * @see ais.database.model.GeneralValueObject
 * @see ais.action.servlet.AmbilLampiran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "lampiran_lain")
public class LampiranLain extends FileFotoLain {
	/**
	 * Penanda {@code jenis} untuk bitstream (berkas isi) artikel pada modul repositori jurnal.
	 *
	 * <p><b>Kuirk:</b> konstanta ini <b>tidak dirujuk dari mana pun</b> di seluruh codebase —
	 * satu-satunya kemunculannya adalah deklarasi ini sendiri. Sisa rencana fitur repositori
	 * jurnal yang tidak jadi dipakai; dibiarkan apa adanya karena menghapusnya tidak
	 * memberi manfaat dan berisiko memutus integrasi luar yang mungkin memakai nilai
	 * literalnya.</p>
	 */
	public static final String JURNAL_REPO_BITSTREAM = "JURNAL_REPO_BITSTREAM";

	/**
	 * Lokasi berkas hasil unggahan di sistem berkas server, dideklarasikan ulang di sini
	 * karena {@code FileFoto} bukan {@code @MappedSuperclass} (lihat catatan pada Javadoc
	 * kelas).
	 */
	private String lokasiSimpan;

	/**
	 * Mengembalikan lokasi simpan berkas apa adanya, tanpa normalisasi.
	 *
	 * @return path berkas di server, atau {@code null} bila belum pernah diisi
	 */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/**
	 * Menyetel lokasi simpan berkas.
	 *
	 * @param lokasiSimpan path berkas di server
	 */
	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	/*
	 * ================================================================================
	 * KONSTANTA NILAI KOLOM "jenis"
	 * --------------------------------------------------------------------------------
	 * Seluruh konstanta String di bawah ini adalah nilai baku untuk kolom `jenis`, yaitu
	 * separuh dari kunci pencarian (ref, jenis) yang dipakai ambil(). Kecocokannya PERSIS
	 * (Restrictions.eq, case-sensitive), sehingga NILAI TEKSNYA adalah kontrak data:
	 * mengubah salah satu string di bawah akan memutus tautan ke seluruh baris lama di
	 * tabel lampiran_lain yang sudah tersimpan dengan nilai lama.
	 *
	 * Perlu diingat: `jenis` TIDAK selalu berasal dari konstanta di kelas ini. Sebagian
	 * pemanggil mengisi `jenis` dengan nama kelas Java pemilik secara fully-qualified
	 * (mis. Peraturan.class.getName() di GolonganAction/KenaikanPangkatHelper), sebagian
	 * lain menuliskan label bebas langsung di layarnya.
	 * ================================================================================
	 */

	/** Lampiran bukti/berkas pendukung fitur absensi daring. */
	public static final String ABSEN_ONLINE = "Absen Online";
	/** Berkas naskah lengkap skripsi/tugas akhir mahasiswa. */
	public static final String SKRIPSI = "Skripsi";
	/** Berkas halaman sampul skripsi, terpisah dari naskahnya. */
	public static final String COVER_SKRIPSI = "Cover Skripsi";
	/** Gambar kop surat tingkat jurusan/program studi. */
	public static final String KOP_JURUSAN = "KOP Jurusan";
	/** Gambar stempel tingkat jurusan/program studi. */
	public static final String STEMPEL_JURUSAN = "Stempel Jurusan";
	/** Berkas informasi/profil jurusan yang ditampilkan di halaman publik. */
	public static final String INFO_JURUSAN = "Info Jurusan";
	/**
	 * Gambar tanda tangan dosen.
	 *
	 * <p><b>Sensitif.</b> Berkas ber-{@code jenis} ini dipakai untuk mengesahkan dokumen
	 * resmi (berita acara nilai, SK, laporan kinerja — lihat
	 * {@code webapp/report/form_realisasi_kinerja_dosen.jrxml}). Karena {@code /al}
	 * publik sejak 19-08-2026, gambar tanda tangan dapat diunduh siapa pun yang menebak
	 * {@code ref}; lihat {@code task_b82b25d2}.</p>
	 */
	public static final String TTD_DOSEN = "TTD Dosen";
	/** Gambar tanda tangan mahasiswa. Sensitif, lihat catatan pada {@link #TTD_DOSEN}. */
	public static final String TTD_MAHASISWA = "TTD Mahasiswa";
	/** Gambar tanda tangan guru. Sensitif, lihat catatan pada {@link #TTD_DOSEN}. */
	public static final String TTD_GURU = "TTD Guru";
	/** Gambar tanda tangan siswa. Sensitif, lihat catatan pada {@link #TTD_DOSEN}. */
	public static final String TTD_SISWA = "TTD Siswa";
	/** Gambar tanda tangan pegawai. Sensitif, lihat catatan pada {@link #TTD_DOSEN}. */
	public static final String TTD_PEGAWAI = "TTD Pegawai";
	/** Gambar kop surat (bagian atas) tingkat sekolah. */
	public static final String KOP_SEKOLAH = "KOP Sekolah";
	/** Gambar stempel tingkat sekolah. */
	public static final String STEMPEL_SEKOLAH = "Stempel Sekolah";
	/** Gambar kop bagian bawah (footer) surat tingkat sekolah. */
	public static final String KOP_BAWAH_SEKOLAH = "KOP Bawah Sekolah";

	/*
	 * Aset branding PPDB (Penerimaan Peserta Didik Baru -- jalur SEKOLAH). Prefiks:
	 * KOP_ = kop surat atas, FOOTER_ = kop bawah, BG_ = gambar latar halaman.
	 */
	/** Kop surat formulir PPDB tingkat sekolah. */
	public static final String KOP_PPDB_SEKOLAH = "KOP PPDB Sekolah";
	/** Kop surat formulir PPDB tingkat yayasan (menaungi banyak sekolah). */
	public static final String KOP_PPDB_YAYASAN = "KOP PPDB Yayasan";

	/** Gambar latar halaman PPDB, khusus satu gelombang pendaftaran. */
	public static final String BG_PPDB_GELOMBANG = "BG PPDB Gelombang";
	/** Gambar latar halaman PPDB tingkat sekolah. */
	public static final String BG_PPDB_SEKOLAH = "BG PPDB Sekolah";
	/** Gambar latar halaman PPDB tingkat yayasan. */
	public static final String BG_PPDB_YAYASAN = "BG PPDB Yayasan";

	/** Gambar latar halaman umum sekolah (di luar konteks PPDB). */
	public static final String BG_SEKOLAH = "BG Sekolah";
	/** Gambar latar halaman umum yayasan (di luar konteks PPDB). */
	public static final String BG_YAYASAN = "BG Yayasan";

	/** Kop bawah (footer) formulir PPDB tingkat sekolah. */
	public static final String FOOTER_PPDB_SEKOLAH = "FOOTER PPDB Sekolah";
	/** Kop bawah (footer) formulir PPDB tingkat yayasan. */
	public static final String FOOTER_PPDB_YAYASAN = "FOOTER PPDB Yayasan";

	/*
	 * Aset branding PMB (Penerimaan Mahasiswa Baru -- jalur PERGURUAN TINGGI). Sejajar
	 * dengan blok PPDB di atas; "PT" = perguruan tinggi.
	 */
	/** Gambar latar halaman PMB, khusus satu gelombang pendaftaran. */
	public static final String BG_PMB_GELOMBANG = "BG PMB Gelombang";
	/** Kop surat formulir PMB tingkat perguruan tinggi. */
	public static final String KOP_PMB_PT = "KOP PMB PT";
	/** Gambar latar halaman PMB tingkat perguruan tinggi. */
	public static final String BG_PMB_PT = "BG PMB PT";
	/** Gambar latar halaman umum perguruan tinggi (di luar konteks PMB). */
	public static final String BG_PT = "BG PT";
	/** Kop bawah (footer) formulir PMB tingkat perguruan tinggi. */
	public static final String FOOTER_PMB_PT = "FOOTER PMB PT";

	/** Logo sekolah untuk kop dokumen dan tampilan aplikasi. */
	public static final String LOGO_SEKOLAH = "Logo Sekolah";
	/** Gambar latar tampilan aplikasi untuk pengguna sekolah. */
	public static final String BACKGROUND_SEKOLAH = "Background Sekolah";
	/** Gambar latar khusus halaman login sekolah. */
	public static final String BACKGROUND_LOGIN_SEKOLAH = "Background Login Sekolah";
	/** Kop surat tingkat yayasan. */
	public static final String KOP_YAYASAN = "KOP Yayasan";
	/** Gambar stempel tingkat yayasan. */
	public static final String STEMPEL_YAYASAN = "Stempel Yayasan";
	/** Logo yayasan. */
	public static final String LOGO_YAYASAN = "Logo Yayasan";
	/** Gambar latar tampilan aplikasi untuk pengguna yayasan. */
	public static final String BACKGROUND_YAYASAN = "Background Yayasan";

	/**
	 * Gambar tanda tangan yang dicetak di kolom KIRI blok tanda tangan formulir.
	 * Sensitif, lihat catatan pada {@link #TTD_DOSEN}.
	 */
	public static final String TTD_FORMULIR_KIRI = "TTD Formulir Kiri";
	/**
	 * Gambar tanda tangan yang dicetak di kolom KANAN blok tanda tangan formulir.
	 * Sensitif, lihat catatan pada {@link #TTD_DOSEN}.
	 */
	public static final String TTD_FORMULIR_KANAN = "TTD Formulir Kanan";

	/** Kop surat tingkat perguruan tinggi. */
	public static final String KOP_PT = "KOP PT";
	/** Gambar stempel tingkat perguruan tinggi. */
	public static final String STEMPEL_PT = "Stempel PT";
	/** Kop surat tingkat satuan kerja (unit di bawah perguruan tinggi/yayasan). */
	public static final String KOP_SATKER = "KOP SATKER";
	/** Kop surat pada formulir cetak satu gelombang pendaftaran PMB. */
	public static final String KOP_GELOMBANG_PMB = "KOP Formulir Gelombang Pendaftaran";
	/** Ikon kecil satu gelombang pendaftaran PMB (dipakai di daftar gelombang). */
	public static final String ICON_GELOMBANG_PMB = "Icon Formulir Gelombang Pendaftaran";
	/** Ikon kecil jurusan/program studi. */
	public static final String ICON_JURUSAN = "Icon Jurusan";
	/** Kop surat pada formulir cetak satu gelombang pendaftaran PSB (jalur sekolah). */
	public static final String KOP_GELOMBANG_PSB = "KOP Formulir Gelombang Pendaftaran PSB";
	/** Kop bawah (footer) surat tingkat perguruan tinggi. */
	public static final String KOP_BAWAH_PT = "KOP Bawah PT";
	/** Kop bawah (footer) surat tingkat satuan kerja. */
	public static final String KOP_BAWAH_SATKER = "KOP Bawah Satker";
	/** Logo perguruan tinggi. */
	public static final String LOGO_PT = "Logo PT";
	/** Gambar latar tampilan aplikasi untuk pengguna perguruan tinggi. */
	public static final String BACKGROUND_PT = "Background PT";
	/** Gambar latar khusus halaman login perguruan tinggi. */
	public static final String BACKGROUND_LOGIN_PT = "Background Login PT";
	/** Banner utama (lebar, versi desktop) beranda perguruan tinggi. */
	public static final String BANNER_UTAMA_PT = "Banner Utama PT";
	/** Banner beranda perguruan tinggi versi tampilan mobile. */
	public static final String BANNER_MOBILE_PT = "Banner Mobile PT";

	/** Ikon yang mendampingi satu baris pengumuman. */
	public static final String ICON_PENGUMUMAN = "Icon Pengumuman";

	/** Kop surat tingkat fakultas. */
	public static final String KOP_FAKULTAS = "KOP Fakultas";
	/** Gambar stempel tingkat fakultas. */
	public static final String STEMPEL_FAKULTAS = "Stempel Fakultas";
	/** Kop bawah (footer) surat tingkat fakultas. */
	public static final String FOOT_FAKULTAS = "FOOT Fakultas";

	/** Gambar pada baris master aset (katalog jenis aset inventaris). */
	public static final String GAMBAR_MASTER_ASSET = "Gambar Master Asset";
	/** Gambar pada baris aset inventaris yang konkret. */
	public static final String GAMBAR_ASSET = "Gambar Asset";

	/** Berkas isi buku (modul perpustakaan / bahan ajar digital). */
	public static final String BUKU = "Buku";
	/** Gambar sampul buku, terpisah dari berkas isinya. */
	public static final String COVER_BUKU = "Cover Buku";

	/** Pindaian surat tugas mengajar dosen di perguruan tinggi lain. */
	public static final String SK_PT_LAIN = "Surat Tugas Mengajar di PT Lain";
	/** Pindaian surat keputusan tugas belajar (studi lanjut) pegawai/dosen. */
	public static final String SK_TUGAS_BELAJAR = "Surat Keputusan Tugas Belajar";

	/** Berkas hasil rekonsiliasi pembayaran host-to-host dengan bank/payment gateway. */
	public static final String REKONSILIASI_HOST_TO_HOST = "Rekonsiliasi Host To Host";

	/*
	 * ------------------------------------------------------------------------
	 * Template JasperReports (berkas .jrxml) yang diunggah administrator.
	 * ------------------------------------------------------------------------
	 * Pola ini membuat tata letak cetak DAPAT DIGANTI TANPA DEPLOY: berkas .jrxml
	 * disimpan sebagai lampiran, lalu dibaca kembali oleh mesin laporan saat mencetak.
	 *
	 * PERHATIAN KEAMANAN: .jrxml adalah template yang dapat memuat EKSPRESI JAVA dan
	 * kueri SQL yang dijalankan mesin JasperReports. Kemampuan mengunggah berkas
	 * ber-`jenis` ini setara dengan kemampuan menjalankan kode di server, sehingga layar
	 * yang mengunggahnya wajib dibatasi ke administrator. Perhatikan juga bahwa
	 * setupDownloadButtonAction() di FileFotoLain memperlakukan berkas berakhiran .jrxml
	 * / .xml secara khusus: diunduh langsung lewat Filedownload.save(), bukan dipratinjau.
	 *
	 * Kuirk penamaan yang dipertahankan apa adanya (nilainya sudah tersimpan di basis
	 * data pelanggan, jadi TIDAK boleh dirapikan):
	 *   - beberapa nilai memakai kata "from" padahal maksudnya "form";
	 *   - FILE_JRXML_LAYOUT_JENIS_CATATAN_SISWA bernilai "File jrxml jenis catatan"
	 *     (tanpa kata "siswa"), sedangkan
	 *     FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_ADMINISTRASI juga bernilai
	 *     "File jrxml jenis from catatan" -- keduanya lebih pendek dari nama konstantanya;
	 *   - FILE_JRXML_LAYOUT_JENIS_PENGAJUAN_MHS bernilai
	 *     "File jrxml jenis pengaduan mahasiswa" (pengaduan, bukan pengajuan).
	 */
	/** Template cetak surat keluar. */
	public static final String FILE_JRXML_LAYOUT_SURAT = "File jrxml layout surat";
	/** Template cetak lembar disposisi surat. */
	public static final String FILE_JRXML_LAYOUT_DISPOSISI = "File jrxml layout disposisi";
	/** Template cetak disposisi pada alur SOP. */
	public static final String FILE_JRXML_LAYOUT_DISPOSISI_ALUR_SOP = "File jrxml layout disposisi alur sop";
	/** Template cetak disposisi untuk surat masuk. */
	public static final String FILE_JRXML_LAYOUT_DISPOSISI_MASUK = "File jrxml layout disposisi masuk";
	/** Template cetak sertifikat. */
	public static final String FILE_JRXML_LAYOUT_SERTIFIKAT = "File jrxml sertifikat";
	/** Template cetak catatan guru. */
	public static final String FILE_JRXML_LAYOUT_JENIS_CATATAN_GURU = "File jrxml jenis catatan guru";
	/** Template cetak formulir kegiatan. */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORMULIR_KEGIATAN = "File jrxml jenis formulir kegiatan";
	/** Template cetak catatan pegawai. */
	public static final String FILE_JRXML_LAYOUT_JENIS_CATATAN_PEGAWAI = "File jrxml jenis catatan pegawai";

	/** Template cetak laporan akuntansi. */
	public static final String FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI = "File jrxml jenis laporan akuntansi";

	/** Template cetak catatan kelas siswa. */
	public static final String FILE_JRXML_LAYOUT_JENIS_CATATAN_KELAS_SISWA = "File jrxml jenis catatan kelas siswa";
	/**
	 * Template cetak catatan siswa. Perhatikan nilainya {@code "File jrxml jenis catatan"}
	 * (tanpa kata "siswa") — tidak seragam dengan nama konstantanya.
	 */
	public static final String FILE_JRXML_LAYOUT_JENIS_CATATAN_SISWA = "File jrxml jenis catatan";
	/** Template cetak catatan mahasiswa. */
	public static final String FILE_JRXML_LAYOUT_JENIS_CATATAN_MAHASISWA = "File jrxml jenis catatan mahasiswa";
	/** Template cetak catatan administrasi. */
	public static final String FILE_JRXML_LAYOUT_JENIS_CATATAN_ADMINISTRASI = "File jrxml jenis catatan administrasi";
	/** Template cetak <i>formulir</i> catatan guru (salah ketik "from" dipertahankan). */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_GURU = "File jrxml jenis from catatan guru";
	/** Template cetak <i>formulir</i> catatan pegawai (salah ketik "from" dipertahankan). */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_PEGAWAI = "File jrxml jenis from catatan pegawai";

	/** Template cetak <i>formulir</i> kegiatan (salah ketik "from" dipertahankan). */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_FORMULIR_KEGIATAN = "File jrxml jenis from formulir kegiatan";

	/** Template cetak <i>formulir</i> SK guru (salah ketik "from" dipertahankan). */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_SK_GURU = "File jrxml jenis from sk guru";
	/** Template cetak <i>formulir</i> catatan kelas siswa (salah ketik "from" dipertahankan). */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_KELAS_SISWA = "File jrxml jenis from catatan kelas siswa";
	/** Template cetak <i>formulir</i> catatan siswa (salah ketik "from" dipertahankan). */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_SISWA = "File jrxml jenis from catatan siswa";
	/** Template cetak <i>formulir</i> catatan mahasiswa (salah ketik "from" dipertahankan). */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_MAHASISWA = "File jrxml jenis from catatan mahasiswa";
	/**
	 * Template cetak <i>formulir</i> catatan administrasi. Nilainya
	 * {@code "File jrxml jenis from catatan"} — lebih pendek dari nama konstantanya dan
	 * mudah tertukar dengan konstanta catatan siswa.
	 */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_ADMINISTRASI = "File jrxml jenis from catatan";
	/** Template cetak dokumen perbaikan (sarana/prasarana). */
	public static final String FILE_JRXML_LAYOUT_JENIS_PERBAIKAN = "File jrxml jenis perbaikan";
	/** Template cetak formulir perbaikan. */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORM_PERBAIKAN = "File jrxml jenis form perbaikan";
	/** Template cetak dokumen pengajuan. */
	public static final String FILE_JRXML_LAYOUT_JENIS_PENGAJUAN = "File jrxml jenis pengajuan";

	/** Template cetak pengajuan transaksi pegawai (mis. reimburse/honor). */
	public static final String FILE_JRXML_LAYOUT_JENIS_PENGAJUAN_TRANSAKSI_PEGAWAI = "File jrxml jenis pengajuan transaksi pegawai";

	/** Template cetak dokumen tipe produk koperasi. */
	public static final String FILE_JRXML_LAYOUT_TIPE_PRODUK_KOPERASI = "File jrxml tipe produk koperasi";

	/** Template cetak dokumen pengaduan. */
	public static final String FILE_JRXML_LAYOUT_JENIS_PENGADUAN = "File jrxml jenis pengaduan";
	/**
	 * Template cetak pengajuan mahasiswa. Nilainya justru berbunyi
	 * {@code "File jrxml jenis pengaduan mahasiswa"} (pengaduan, bukan pengajuan) —
	 * ketidakcocokan nama/nilai yang dipertahankan demi kompatibilitas data lama.
	 */
	public static final String FILE_JRXML_LAYOUT_JENIS_PENGAJUAN_MHS = "File jrxml jenis pengaduan mahasiswa";
	/** Template cetak surat cuti dan izin. */
	public static final String FILE_JRXML_LAYOUT_JENIS_CUTI_DAN_IZIN = "File jrxml jenis cuti dan izin";
	/** Template cetak rapor. */
	public static final String FILE_JRXML_LAYOUT_JENIS_RAPOR = "File jrxml jenis rapor";
	/** Template cetak lembar nilai. */
	public static final String FILE_JRXML_LAYOUT_JENIS_NILAI = "File jrxml jenis nilai";

	/** Template cetak slip/format gaji. */
	public static final String FILE_JRXML_LAYOUT_JENIS_FORMAT_GAJI = "File jrxml jenis format gaji";

	/** Template cetak jadwal. */
	public static final String FILE_JRXML_LAYOUT_JENIS_JADWAL = "File jrxml jenis jadwal";
	/** Berkas Excel berisi data surat untuk diimpor massal. */
	public static final String FILE_XLS_DATA_SURAT = "File data surat";
	/** Gambar item/barang (inventaris, produk koperasi, koleksi perpustakaan). */
	public static final String ITEM = "Item";
	/** Gambar/berkas bagan alur registrasi PMB yang ditampilkan di halaman publik. */
	public static final String ALUR_REGISTRASI_PMB = "Alur Registrasi PMB";
	/** Gambar/berkas bagan alur registrasi PSB yang ditampilkan di halaman publik. */
	public static final String ALUR_REGISTRASI_PSB = "Alur Registrasi PSB";
	/**
	 * Berkas paket tema tampilan (arsip {@code .zip}) untuk seluruh instansi.
	 *
	 * <p>Dipasangkan dengan {@link #ID_SKIN} sebagai {@code ref}. Perhatikan bahwa
	 * {@code AmbilLampiran} memperlakukan {@code ref == }{@link #ID_SKIN} sebagai kasus
	 * khusus dan justru melayani berkas {@code /opt/<contextPath>.zip} dari sistem
	 * berkas server, bukan baris basis data.</p>
	 */
	public static final String SKIN = "SKIN";
	/** Gambar pratinjau paket tema tampilan; dipasangkan dengan {@link #ID_SKIN_PREVIEW}. */
	public static final String SKIN_PREVIEW = "SKIN_PREVIEW";

	/** Dokumen silabus satu mata kuliah. */
	public static final String SILABUS = "Susunan silabus perkuliahan";
	/** Dokumen SAP (Satuan Acara Perkuliahan) satu mata kuliah. */
	public static final String SAP = "Susunan SAP perkuliahan";
	/** Dokumen Laporan Hasil Perkuliahan. */
	public static final String LHP = "Laporan Hasil Perkuliahan";

	/** Dokumen susunan rencana pembelajaran. */
	public static final String PEMBELAJARAN = "Susunan Pembelajaran";

	/** Lampiran pada catatan konsultasi/bimbingan mahasiswa. */
	public static final String CATATAN_KONSULTASI = "Catatan Konsultasi";
	/** Lampiran pada catatan pelaksanaan satu pertemuan perkuliahan. */
	public static final String CATATAN_PERKULIAHAN = "Catatan Perkuliahan";
	/** Berkas tugas mandiri yang diunggah pada satu pertemuan perkuliahan. */
	public static final String TUGAS_MANDIRI_PERKULIAHAN = "Tugas Mandiri Perkuliahan";
	/** Lampiran pada forum diskusi perkuliahan. */
	public static final String DISKUSI = "Diskusi";
	/** Pindaian sertifikat sertifikasi dosen. */
	public static final String SERTIFIKASI_DOSEN = "Sertifikasi Dosen";
	/** Pindaian sertifikat sertifikasi pegawai. */
	public static final String SERTIFIKASI_PEGAWAI = "Sertifikasi Pegawai";
	/** Bukti (surat dokter/surat izin) ketidakhadiran pada perkuliahan. */
	public static final String IZIN_TIDAK_MASUK = "Izin Tidak Masuk Perkuliahan";
	/** Berkas tugas kelompok yang diunggah pada satu pertemuan perkuliahan. */
	public static final String TUGAS_KELOMPOK_PERKULIAHAN = "Tugas Kelompok Perkuliahan";

	/*
	 * ================================================================================
	 * KONSTANTA NILAI KOLOM "ref" (ACUAN SINTETIS)
	 * --------------------------------------------------------------------------------
	 * Konstanta Long di bawah ini BUKAN jenis, melainkan nilai `ref` tetap untuk lampiran
	 * yang TIDAK punya baris pemilik (aset milik instansi secara keseluruhan: skin, logo
	 * beranda, favicon, latar kartu anggota, gambar tombol pembayaran, dst).
	 *
	 * Nilainya sengaja NEGATIF agar tidak pernah bertabrakan dengan primary key baris
	 * pemilik yang selalu positif -- prinsip yang sama dipakai Common.refSementara() untuk
	 * acuan sementara saat berkas diunggah sebelum baris pemiliknya tersimpan (lihat
	 * catatan pada FileFotoLain.createFileFotoLain tentang bug lama yang memakai acuan
	 * POSITIF sehingga berkas baru menimpa lampiran milik data lain).
	 *
	 * PENGECUALIAN: ID_ALUR_REGISTRASI_PMB bernilai 0L, satu-satunya yang tidak negatif.
	 * Nilai 0 aman karena kolom id ber-strategi IDENTITY tidak pernah menghasilkan 0.
	 *
	 * Konstanta berakhiran _STR adalah pasangan `jenis` untuk acuan Long di atasnya.
	 * ================================================================================
	 */
	/** Acuan {@code ref} untuk gambar bagan alur registrasi PMB milik instansi. */
	public static final Long ID_ALUR_REGISTRASI_PMB = 0L;
	/** Acuan {@code ref} untuk gambar bagan alur registrasi PSB milik instansi. */
	public static final Long ID_ALUR_REGISTRASI_PSB = -4L;
	/** Acuan {@code ref} untuk paket tema tampilan; berpasangan dengan {@link #SKIN}. */
	public static final Long ID_SKIN = -1L;
	/** Acuan {@code ref} untuk gambar pratinjau tema; berpasangan dengan {@link #SKIN_PREVIEW}. */
	public static final Long ID_SKIN_PREVIEW = -2L;


	/** Acuan {@code ref} untuk berkas panduan penggunaan modul perpustakaan. */
	public static final Long ID_PANDUAN_PUSTAKA = -20031L;

	/*
	 * Aset cetak KARTU ANGGOTA PERPUSTAKAAN. Pola yang sama diulang untuk tiap peran
	 * (anggota umum, mahasiswa, pegawai, siswa, alumni): sepasang acuan `ref` untuk
	 * tanda tangan + stempel, sepasang lagi untuk latar depan/belakang kartu.
	 */
	/** Acuan {@code ref} gambar tanda tangan pada kartu anggota perpustakaan (umum). */
	public static final Long TANDA_TANGAN_KARTU_ANGGOTA_PERPUSTAKAAN = -3L;
	/** Acuan {@code ref} gambar stempel pada kartu anggota perpustakaan (umum). */
	public static final Long STEMPEL_KARTU_ANGGOTA_PERPUSTAKAAN = -4L;
	/** Acuan {@code ref} gambar latar sisi DEPAN kartu anggota perpustakaan (umum). */
	public static final Long BG_1_KARTU_ANGGOTA_PERPUSTAKAAN = -5L;
	/** Acuan {@code ref} gambar latar sisi BELAKANG kartu anggota perpustakaan (umum). */
	public static final Long BG_2_KARTU_ANGGOTA_PERPUSTAKAAN = -6L;
	/**
	 * {@code jenis} latar depan kartu anggota perpustakaan. Nilainya
	 * {@code "Background Depan"} — <b>sama persis</b> dengan
	 * {@link #BACKGROUND_DEPAN_PESANTREN_STR}; keduanya hanya dibedakan oleh {@code ref}.
	 */
	public static final String BG_1_KARTU_ANGGOTA_PERPUSTAKAAN_STR = "Background Depan";
	/** {@code jenis} latar belakang kartu anggota perpustakaan (umum). */
	public static final String BG_2_KARTU_ANGGOTA_PERPUSTAKAAN_STR = "Background Belakang";
	/** {@code jenis} gambar tanda tangan pada kartu anggota perpustakaan (umum). */
	public static final String TTD_KARTU_ANGGOTA_PERPUSTAKAAN_STR = "Ttd Kartu Anggota Perpustakaan";
	/** {@code jenis} gambar stempel pada kartu anggota perpustakaan (umum). */
	public static final String STEMPEL_KARTU_ANGGOTA_PERPUSTAKAAN_STR = "Stempel Kartu Anggota Perpustakaan";

	/** Acuan {@code ref} untuk logo yang dicetak pada kartu perpustakaan. */
	public static final Long LOGO = -35L;
	/** Acuan {@code ref} gambar tanda tangan pada kartu perpustakaan MAHASISWA. */
	public static final Long TANDA_TANGAN_KARTU_MAHASISWA_PERPUSTAKAAN = -30L;
	/** Acuan {@code ref} gambar stempel pada kartu perpustakaan MAHASISWA. */
	public static final Long STEMPEL_KARTU_MAHASISWA_PERPUSTAKAAN = -40L;
	/** Acuan {@code ref} gambar latar sisi DEPAN kartu perpustakaan MAHASISWA. */
	public static final Long BG_1_KARTU_MAHASISWA_PERPUSTAKAAN = -50L;
	/** Acuan {@code ref} gambar latar sisi BELAKANG kartu perpustakaan MAHASISWA. */
	public static final Long BG_2_KARTU_MAHASISWA_PERPUSTAKAAN = -60L;
	/** {@code jenis} latar depan kartu perpustakaan mahasiswa. */
	public static final String BG_1_KARTU_MAHASISWA_PERPUSTAKAAN_STR = "Background Depan Kartu Mahasiswa";
	/** {@code jenis} latar belakang kartu perpustakaan mahasiswa. */
	public static final String BG_2_KARTU_MAHASISWA_PERPUSTAKAAN_STR = "Background Belakang Kartu Mahasiswa";
	/** {@code jenis} gambar tanda tangan pada kartu perpustakaan mahasiswa. */
	public static final String TTD_KARTU_MAHASISWA_PERPUSTAKAAN_STR = "Ttd Kartu Mahasiswa";
	/** {@code jenis} logo pada kartu perpustakaan; berpasangan dengan {@link #LOGO}. */
	public static final String LOGO_STR = "Logo";
	/** {@code jenis} gambar stempel pada kartu perpustakaan mahasiswa. */
	public static final String STEMPEL_KARTU_MAHASISWA_PERPUSTAKAAN_STR = "Stempel Kartu Mahasiswa";

	/** Acuan {@code ref} gambar tanda tangan pada kartu perpustakaan PEGAWAI. */
	public static final Long TANDA_TANGAN_KARTU_PEGAWAI_PERPUSTAKAAN = -308L;
	/** Acuan {@code ref} gambar stempel pada kartu perpustakaan PEGAWAI. */
	public static final Long STEMPEL_KARTU_PEGAWAI_PERPUSTAKAAN = -408L;
	/** Acuan {@code ref} gambar latar sisi DEPAN kartu perpustakaan PEGAWAI. */
	public static final Long BG_1_KARTU_PEGAWAI_PERPUSTAKAAN = -508L;
	/** Acuan {@code ref} gambar latar sisi BELAKANG kartu perpustakaan PEGAWAI. */
	public static final Long BG_2_KARTU_PEGAWAI_PERPUSTAKAAN = -608L;
	/** {@code jenis} latar depan kartu perpustakaan pegawai. */
	public static final String BG_1_KARTU_PEGAWAI_PERPUSTAKAAN_STR = "Background Depan Kartu Pegawai";
	/** {@code jenis} latar belakang kartu perpustakaan pegawai. */
	public static final String BG_2_KARTU_PEGAWAI_PERPUSTAKAAN_STR = "Background Belakang Kartu Pegawai";
	/** {@code jenis} gambar tanda tangan pada kartu perpustakaan pegawai. */
	public static final String TTD_KARTU_PEGAWAI_PERPUSTAKAAN_STR = "Ttd Kartu Pegawai";
	/** {@code jenis} gambar stempel pada kartu perpustakaan pegawai. */
	public static final String STEMPEL_KARTU_PEGAWAI_PERPUSTAKAAN_STR = "Stempel Kartu Pegawai";

	/** Acuan {@code ref} gambar tanda tangan pada kartu perpustakaan SISWA. */
	public static final Long TANDA_TANGAN_KARTU_SISWA_PERPUSTAKAAN = -301L;
	/** Acuan {@code ref} gambar stempel pada kartu perpustakaan SISWA. */
	public static final Long STEMPEL_KARTU_SISWA_PERPUSTAKAAN = -401L;
	/** Acuan {@code ref} gambar latar sisi DEPAN kartu perpustakaan SISWA. */
	public static final Long BG_1_KARTU_SISWA_PERPUSTAKAAN = -501L;
	/** Acuan {@code ref} gambar latar sisi BELAKANG kartu perpustakaan SISWA. */
	public static final Long BG_2_KARTU_SISWA_PERPUSTAKAAN = -601L;
	/** {@code jenis} latar depan kartu perpustakaan siswa. */
	public static final String BG_1_KARTU_SISWA_PERPUSTAKAAN_STR = "Background Depan Kartu Siswa";
	/** {@code jenis} latar belakang kartu perpustakaan siswa. */
	public static final String BG_2_KARTU_SISWA_PERPUSTAKAAN_STR = "Background Belakang Kartu Siswa";
	/** {@code jenis} gambar tanda tangan pada kartu perpustakaan siswa. */
	public static final String TTD_KARTU_SISWA_PERPUSTAKAAN_STR = "Ttd Kartu Siswa";
	/** {@code jenis} gambar stempel pada kartu perpustakaan siswa. */
	public static final String STEMPEL_KARTU_SISWA_PERPUSTAKAAN_STR = "Stempel Kartu Siswa";

	/*
	 * Aset BERANDA/HALAMAN PUBLIK (logo, banner, latar, favicon) per varian portal:
	 * umum, pesantren/sekolah, PMB, PSB, alumni, dashboard. Semuanya berpasangan
	 * <acuan Long> + <konstanta _STR sebagai jenis>.
	 *
	 * PERHATIAN: karena aset-aset ini memang dipasang di halaman publik, justru
	 * merekalah alasan yang dipakai untuk membuka /al ke IS_AUTHENTICATED_ANONYMOUSLY
	 * pada 19-08-2026 -- padahal pintu yang sama juga melayani lampiran pribadi
	 * (KTP, KK, ijazah, tanda tangan). Lihat Javadoc kelas dan task_b82b25d2.
	 */
	/** Acuan {@code ref} gambar latar beranda portal pesantren/sekolah. */
	public static final Long BACKGROUND_DEPAN_PESANTREN = -500011L;
	/** Acuan {@code ref} banner utama beranda. */
	public static final Long BANNER_DEPAN = -300011L;
	/** Acuan {@code ref} gambar latar beranda varian I. */
	public static final Long BACKGROUND_DEPAN_1 = -30001L;
	/** Acuan {@code ref} banner beranda varian I. */
	public static final Long BANNER_1 = -31001L;
	/** Acuan {@code ref} gambar latar beranda varian II. */
	public static final Long BACKGROUND_DEPAN_2 = -30002L;
	/** Acuan {@code ref} logo beranda. */
	public static final Long LOGO_DEPAN = -30003L;
	/** Acuan {@code ref} logo beranda portal pesantren/sekolah. */
	public static final Long LOGO_DEPAN_PESANTREN = -50005L;
	/** Acuan {@code ref} logo beranda portal PMB. */
	public static final Long LOGO_DEPAN_PMB = -300031L;
	/** Acuan {@code ref} banner beranda portal PMB. */
	public static final Long BANNER_DEPAN_PMB = -300041L;
	/** Acuan {@code ref} logo beranda portal PSB. */
	public static final Long LOGO_DEPAN_PSB = -700031L;
	/** Acuan {@code ref} banner beranda portal PSB. */
	public static final Long BANNER_DEPAN_PSB = -700041L;
	/** Acuan {@code ref} logo beranda portal alumni. */
	public static final Long LOGO_DEPAN_ALUMNI = -300032L;
	/** Acuan {@code ref} logo pada halaman dashboard. */
	public static final Long LOGO_DEPAN_DASHBOARD = -300062L;
	/** Acuan {@code ref} banner beranda portal alumni. */
	public static final Long BANNER_DEPAN_ALUMNI = -300042L;
	/** Acuan {@code ref} banner pada halaman dashboard. */
	public static final Long BANNER_DEPAN_DASHBOARD = -300072L;
	/** Acuan {@code ref} logo yang dicetak pada label harga (price tag) modul koperasi. */
public static final Long LOGO_PRICE_TAG = -300083L;
	/** Acuan {@code ref} berkas favicon situs. */
	public static final Long FAVICON = -30004L;

	/**
	 * {@code jenis} latar beranda portal pesantren/sekolah. Nilainya
	 * {@code "Background Depan"} — <b>bertabrakan</b> dengan
	 * {@link #BG_1_KARTU_ANGGOTA_PERPUSTAKAAN_STR}. Karena kunci pencarian adalah pasangan
	 * ({@code ref}, {@code jenis}), keduanya tetap terpisah selama {@code ref} berbeda;
	 * tetapi query yang mencari berdasarkan {@code jenis} saja akan mencampur keduanya.
	 */
	public static final String BACKGROUND_DEPAN_PESANTREN_STR = "Background Depan";

	/** {@code jenis} gambar latar beranda varian I. */
	public static final String BACKGROUND_DEPAN_1_STR = "Background Depan I";
	/** {@code jenis} gambar latar beranda varian II. */
	public static final String BACKGROUND_DEPAN_2_STR = "Background Depan II";
	/** {@code jenis} banner beranda varian I. */
	public static final String BANNER_1_STR = "Banner";

	/** {@code jenis} banner utama beranda. */
	public static final String BANNER_DEPAN_STR = "Banner Utama";
	/** {@code jenis} logo beranda. */
	public static final String LOGO_DEPAN_STR = "Logo Depan";
	/** {@code jenis} logo beranda portal PMB. */
	public static final String LOGO_DEPAN_PMB_STR = "Logo Depan PMB";
	/** {@code jenis} banner beranda portal PMB. */
	public static final String BANNER_DEPAN_PMB_STR = "Banner Depan PMB";
	/**
	 * {@code jenis} logo beranda portal pesantren. Perhatikan nilainya berbunyi
	 * {@code "Logo Depan Sekolah"} — nama konstanta memakai istilah "pesantren",
	 * nilainya memakai "sekolah".
	 */
	public static final String LOGO_DEPAN_PESANTREN_STR = "Logo Depan Sekolah";
	/** {@code jenis} logo beranda portal PSB. */
	public static final String LOGO_DEPAN_PSB_STR = "Logo Depan PSB";
	/** {@code jenis} banner beranda portal PSB. */
	public static final String BANNER_DEPAN_PSB_STR = "Banner Depan PSB";
	/** {@code jenis} logo beranda portal alumni. */
	public static final String LOGO_DEPAN_ALUMNI_STR = "Logo Depan Alumni";
	/** {@code jenis} banner beranda portal alumni. */
	public static final String BANNER_DEPAN_ALUMNI_STR = "Banner Depan Alumni";
	/** {@code jenis} logo pada halaman dashboard. */
	public static final String LOGO_DEPAN_DASHBOARD_STR = "Logo Depan Dashboard";
	/** {@code jenis} banner pada halaman dashboard. */
	public static final String BANNER_DEPAN_DASHBOARD_STR = "Banner Depan Dashboard";
	/** {@code jenis} logo pada label harga (price tag) modul koperasi. */
public static final String LOGO_PRICE_TAG_STR = "Logo Price Tag";
	/** {@code jenis} berkas favicon situs. */
	public static final String FAVICON_STR = "Favicon";

	/** Acuan {@code ref} gambar tanda tangan pada kartu perpustakaan ALUMNI. */
	public static final Long TANDA_TANGAN_KARTU_ALUMNI_PERPUSTAKAAN = -31L;
	/** Acuan {@code ref} gambar stempel pada kartu perpustakaan ALUMNI. */
	public static final Long STEMPEL_KARTU_ALUMNI_PERPUSTAKAAN = -41L;
	/** Acuan {@code ref} gambar latar sisi DEPAN kartu perpustakaan ALUMNI. */
	public static final Long BG_1_KARTU_ALUMNI_PERPUSTAKAAN = -51L;
	/** Acuan {@code ref} gambar latar sisi BELAKANG kartu perpustakaan ALUMNI. */
	public static final Long BG_2_KARTU_ALUMNI_PERPUSTAKAAN = -61L;
	/** {@code jenis} latar depan kartu perpustakaan alumni. */
	public static final String BG_1_KARTU_ALUMNI_PERPUSTAKAAN_STR = "Background Depan Kartu Alumni";
	/** {@code jenis} latar belakang kartu perpustakaan alumni. */
	public static final String BG_2_KARTU_ALUMNI_PERPUSTAKAAN_STR = "Background Belakang Kartu Alumni";
	/** {@code jenis} gambar tanda tangan pada kartu perpustakaan alumni. */
	public static final String TTD_KARTU_ALUMNI_PERPUSTAKAAN_STR = "Ttd Kartu Alumni";
	/** {@code jenis} gambar stempel pada kartu perpustakaan alumni. */
	public static final String STEMPEL_KARTU_ALUMNI_PERPUSTAKAAN_STR = "Stempel Kartu Alumni";

	/*
	 * Gambar latar TOMBOL pembayaran per kanal payment gateway. Perhatikan: SELURUH
	 * konstanta _STR di blok ini bernilai sama persis ("Background Tombol"). Jadi yang
	 * benar-benar membedakan kanal HANYALAH nilai `ref` -- ini contoh paling jelas bahwa
	 * `jenis` sendirian tidak cukup sebagai kunci, dan bahwa kekeliruan pada `ref` akan
	 * menampilkan gambar tombol bank yang salah tanpa error apa pun.
	 */
	/** Acuan {@code ref} gambar latar tombol pembayaran via Faspay. */
	public static final Long BG_TOMBOL_PEMBAYARAN_VIA_FASPAY = -500L;
	/** {@code jenis} gambar latar tombol pembayaran via Faspay. */
	public static final String BG_TOMBOL_PEMBAYARAN_VIA_FASPAY_STR = "Background Tombol";

	/** Acuan {@code ref} gambar latar tombol pembayaran via BNI. */
	public static final Long BG_TOMBOL_PEMBAYARAN_VIA_BNI = -501L;
	/** {@code jenis} gambar latar tombol pembayaran via BNI (nilainya sama dengan kanal lain). */
	public static final String BG_TOMBOL_PEMBAYARAN_VIA_BNI_STR = "Background Tombol";

	/** Acuan {@code ref} gambar latar tombol pembayaran via BSI. */
	public static final Long BG_TOMBOL_PEMBAYARAN_VIA_BSI = -516L;
	/** {@code jenis} gambar latar tombol pembayaran via BSI (nilainya sama dengan kanal lain). */
	public static final String BG_TOMBOL_PEMBAYARAN_VIA_BSI_STR = "Background Tombol";

	/** Acuan {@code ref} gambar latar tombol pembayaran via BRI. */
	public static final Long BG_TOMBOL_PEMBAYARAN_VIA_BRI = -507L;
	/** {@code jenis} gambar latar tombol pembayaran via BRI (nilainya sama dengan kanal lain). */
	public static final String BG_TOMBOL_PEMBAYARAN_VIA_BRI_STR = "Background Tombol";

	/** Acuan {@code ref} gambar latar tombol pembayaran via CIMB. */
	public static final Long BG_TOMBOL_PEMBAYARAN_VIA_CIMB = -502L;
	/** {@code jenis} gambar latar tombol pembayaran via CIMB (nilainya sama dengan kanal lain). */
	public static final String BG_TOMBOL_PEMBAYARAN_VIA_CIMB_STR = "Background Tombol";

	/** Acuan {@code ref} gambar latar tombol pembayaran via Jatelindo. */
	public static final Long BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO = -503L;
	/** {@code jenis} gambar latar tombol pembayaran via Jatelindo (nilainya sama dengan kanal lain). */
	public static final String BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO_STR = "Background Tombol";

	/**
	 * Versi serialisasi Java. Nilainya dipatok agar objek yang sudah ter-serialisasi
	 * (mis. di session ZK) tetap dapat dibaca setelah kelas ini dimodifikasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Pindaian bukti surat penugasan. */
	public static final String BUKTI_PENUGASAN = "Bukti Penugasan";
	/** Pindaian bukti dokumen pendukung yang sifatnya umum. */
	public static final String BUKTI_DOKUMEN = "Bukti Dokumen";

	/**
	 * Primary key baris lampiran ini ({@code IDENTITY}, berurutan). Dideklarasikan ulang di
	 * sini karena kelas induk bukan {@code @MappedSuperclass}.
	 *
	 * <p><b>Catatan keamanan:</b> karena berurutan dan dapat ditebak, nilai ini menjadi
	 * kunci enumerasi bila dipakai sebagai {@code ref} dengan {@code usingId=true}.
	 * Lihat {@link #ambil(Boolean, Long, String)}.</p>
	 */
	private Long id;
	/** Nama tampil pengguna yang terakhir mengubah baris ini (field audit). */
	private String oleh;
	/** Identitas (user id) pengguna yang terakhir mengubah baris ini (field audit). */
	private String olehId;

	/**
	 * Identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return user id pembuat/pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak diam-diam nilai kosong.</b> Bila {@code olehId} {@code null} atau hanya
	 * berisi spasi, method langsung {@code return} tanpa mengubah apa pun — nilai lama
	 * TETAP dipertahankan. Jadi memanggil {@code setOlehId("")} bukan cara menghapus jejak
	 * audit, dan pemanggil tidak boleh mengandalkan method ini untuk mengosongkan field.</p>
	 *
	 * @param olehId user id pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampil pengguna pengubah terakhir.
	 *
	 * <p>Berperilaku sama dengan {@link #setOlehId(String)}: nilai {@code null}/kosong
	 * diabaikan diam-diam sehingga isi lama tidak tertimpa. Nilainya diisi oleh
	 * {@code FileFotoLain.createFileFotoLain} lewat {@code getNamaOleh(Tbmuser)}, yang
	 * memilih nama mahasiswa/dosen/pegawai bila ada dan jatuh ke {@code "external_update"}
	 * bila tidak ada pengguna login.</p>
	 *
	 * @param oleh nama tampil pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA yang dijalankan tepat sebelum {@code UPDATE} baris ini, meneruskan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} agar stempel waktu perubahan diperbarui
	 * secara terpusat.
	 *
	 * <p><b>Perhatikan tata letak barisnya:</b> deklarasi field {@code tanggal_dirubah}
	 * ditulis pada BARIS YANG SAMA dengan method ini (pola yang dipakai konsisten di seluruh
	 * entity AIS, hasil penyisipan otomatis). Jangan menyisipkan Javadoc atau baris baru di
	 * antara keduanya tanpa memisahkan deklarasinya terlebih dahulu.</p>
	 *
	 * <p>Field {@code tanggal_dirubah} diinisialisasi ke waktu sekarang lewat
	 * {@code WaktuUtil.getDate()} sehingga objek baru selalu punya stempel waktu meski
	 * pemanggil lupa menyetelnya.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan (tidak pernah {@code null} untuk objek yang baru dibuat)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini, yaitu nama berkasnya.
	 *
	 * <p><b>Sengaja membaca field {@code nama} langsung, bukan {@link #getNama()}.</b>
	 * Bedanya nyata: {@code getNama()} punya efek samping (menempelkan awalan {@code
	 * "<id>_"}, mengisi nilai dari {@code copyDari}, atau memberi label untuk dokumen
	 * berupa link), sedangkan method ini murni. Karena {@code toString()} dipanggil di
	 * banyak tempat tak terduga — logging, debugger, komponen ZK — membaca field langsung
	 * mencegah efek samping tak sengaja. Konsekuensinya method ini dapat mengembalikan
	 * {@code null}.</p>
	 *
	 * @return nama berkas apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Nama berkas (termasuk ekstensi). Lihat {@link #getNama()} soal awalan {@code "<id>_"}. */
	private String nama;
	/** Keterangan singkat lampiran, biasanya label kolom pada layar unggah. */
	private String keterangan;
	/** URL dokumen eksternal, dipakai bila lampiran hanya berupa tautan tanpa berkas. */
	private String link;
	/** Deskripsi panjang lampiran (kolom {@code text}). */
	private String deskripsi;
	/**
	 * Acuan ke baris pemilik: separuh kunci pencarian bersama {@link #jenis}.
	 *
	 * <p>Berisi primary key baris pemilik (bukan foreign key, tanpa constraint), atau salah
	 * satu konstanta acuan sintetis negatif di kelas ini untuk aset milik instansi. Lihat
	 * Javadoc kelas untuk konsekuensi keamanannya.</p>
	 */
	private Long ref;
	/** Isi berkas sebagai PostgreSQL Large Object ({@code oid}). Lihat {@link #getFoto()}. */
	private Blob foto;
	/**
	 * Penanda "lampiran yang mana": separuh kunci pencarian bersama {@link #ref}.
	 * Nilainya bisa konstanta di kelas ini, nama kelas Java pemilik, atau label bebas.
	 */
	private String jenis;
	/** File-id Google Drive bila isi berkas disimpan di Drive, bukan di basis data. */
	private String gdrive;
	/** Akun Google yang dipakai saat mengunggah ke Drive. */
	private String gdriveUsername;

	/**
	 * Baris lampiran SUMBER yang isinya dipinjam baris ini.
	 *
	 * <p>Bukan sekadar jejak: bila terisi, hampir seluruh getter di kelas ini membaca nilai
	 * dari baris sumber dan <b>menimpa</b> nilai lokal. Lihat {@link #getCopyDari()}.</p>
	 */
	private LampiranLain copyDari;

	/**
	 * Konstruktor kosong wajib Hibernate.
	 *
	 * <p>Dipakai juga oleh {@code FileFotoLain.createFileFotoLain} dan
	 * {@code FileFotoLain.performDelete} lewat {@code clazz.newInstance()}, sehingga
	 * konstruktor ini <b>tidak boleh</b> diberi parameter atau dihapus.</p>
	 */
	public LampiranLain() {
	}

	/**
	 * Primary key baris lampiran ini.
	 *
	 * <p>Dihasilkan basis data dengan strategi {@code IDENTITY} sehingga <b>berurutan</b>;
	 * {@code insertable = false} berarti nilai yang disetel manual lewat {@link #setId(Long)}
	 * TIDAK ikut dikirim saat {@code INSERT} — nilainya selalu ditentukan basis data.</p>
	 *
	 * <p><b>Implikasi keamanan.</b> Sifat berurutan inilah yang membuat enumerasi mungkin:
	 * {@code /al?usingId=true&ref=<N>} memetakan {@code N} langsung ke primary key ini
	 * (lihat {@link #ambil(Boolean, Long, String)}). Lihat pula Javadoc kelas dan
	 * {@code task_b82b25d2}.</p>
	 *
	 * @return primary key, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Umumnya hanya dipanggil Hibernate saat menghidrasi entity atau
	 * oleh {@code Common.convertToObject} saat merekonstruksi objek dari cache JSON.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama berkas lampiran — <b>getter dengan efek samping, bukan pembaca murni</b>.
	 *
	 * <p>Tiga perilaku menulis-balik terjadi berurutan sebelum nilai dikembalikan:</p>
	 * <ol>
	 *   <li>Bila {@link #getCopyDari() copyDari} terisi, field {@code nama} <b>ditimpa</b>
	 *       dengan nama baris sumber (dibaca dari field langsung, bukan lewat getter, agar
	 *       tidak memicu penambahan awalan berantai pada baris sumber).</li>
	 *   <li>Bila {@code nama} kosong tetapi {@link #getLink()} tidak kosong, {@code nama}
	 *       diisi label baku {@code "Data / dokumen berupa link"} — penanda bahwa lampiran
	 *       ini hanya tautan, tanpa berkas fisik.</li>
	 *   <li>Bila {@code nama} sudah ada dan {@link #getId()} sudah ada tetapi nama belum
	 *       berawalan {@code "<id>_"}, awalan itu <b>ditempelkan permanen</b> ke field.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi yang mudah mengejutkan.</b> Ini adalah properti terpetakan dengan
	 * <i>property access</i>, sehingga Hibernate memanggil method ini saat dirty-check.
	 * Untuk objek yang masih terkelola dalam satu session, sekadar <b>membaca</b> nama
	 * lampiran dapat menghasilkan {@code UPDATE} nyata ke kolom {@code nama} — dan karena
	 * kelas ini {@code @Audited}, satu revisi Envers ikut tercatat. Awalan {@code "<id>_"}
	 * itu sendiri disengaja: ia dipakai sebagai penanda kepemilikan berkas fisik
	 * (lihat {@code FileFoto.ambilFile()} yang menolak berkas milik baris lain), dan
	 * {@code FileFotoLain.createDownloadUpload} membuang kembali awalan tersebut saat
	 * menampilkan label tombol.</p>
	 *
	 * @return nama berkas yang sudah dinormalisasi dan di-{@code trim}, atau {@code null}
	 *         bila memang belum pernah diisi dan tidak ada link
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		} else if ((nama == null || nama.trim().isEmpty()) && !getLink().isEmpty()) {
			nama = "Data / dokumen berupa link";
		}

		if (nama != null && getId() != null && !nama.startsWith(getId() + "_")) {
			nama = getId() + "_" + nama;
		}

		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama berkas apa adanya, tanpa normalisasi.
	 *
	 * <p>Awalan {@code "<id>_"} TIDAK ditambahkan di sini — penambahan itu baru terjadi saat
	 * {@link #getNama()} dipanggil.</p>
	 *
	 * @param nama nama berkas beserta ekstensinya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan singkat lampiran (label kolom pada layar unggah, mis. "Ijazah", "KTP").
	 *
	 * <p><b>Menulis balik.</b> Bila {@link #getCopyDari() copyDari} terisi, field
	 * {@code keterangan} ditimpa dengan keterangan baris sumber sebelum dikembalikan.</p>
	 *
	 * <p><b>Membalik kontrak kelas induk:</b> {@link ais.database.model.GeneralValueObject}
	 * menjanjikan keterangan non-{@code null}, sedangkan kolom di sini {@code nullable = true}
	 * dan method ini memang dapat mengembalikan {@code null}. Pemanggil wajib menjaga
	 * kemungkinan {@code null}. Nilai keterangan ikut dipakai sebagai nama berkas tujuan pada
	 * jalur unduh {@code .jrxml}/{@code .xml}
	 * ({@code FileFotoLain.setupDownloadButtonAction} memanggil
	 * {@code Filedownload.save(file, getKeterangan())}).</p>
	 *
	 * @return keterangan lampiran, dapat {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (copyDari != null) {
			keterangan = copyDari.getKeterangan();
		}
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan singkat lampiran.
	 *
	 * @param keterangan keterangan lampiran
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

//	public void setFoto(Blob foto) {
//		this.foto = foto;
//	}

	// Di dalam class ais.database.model.file.LampiranLain

	// 1. Ubah parameter dari Blob ke Object sementara waktu
	/**
	 * Menyetel isi berkas sebagai {@link Blob}.
	 *
	 * <p>Pemanggil utama adalah {@code FileFotoLain.createFileFotoLain}, yang membungkus isi
	 * berkas dengan {@code Hibernate.createBlob(byte[])}. Kolom targetnya PostgreSQL Large
	 * Object ({@code oid}) sehingga penyimpanannya <b>wajib</b> berada di dalam transaksi
	 * non-autocommit — lihat catatan panjang pada {@code createFileFotoLain} tentang koneksi
	 * khusus yang diambil dari {@code ConnectionProvider} untuk keperluan ini.</p>
	 *
	 * <p>Blok yang dikomentari di dalam badan method adalah sisa perkakas penelusuran
	 * (mencetak stack trace pemanggil) yang dipakai saat mengusut dari mana isi berkas
	 * disetel; sengaja dipertahankan sebagai jejak investigasi, tidak aktif.</p>
	 *
	 * @param foto isi berkas; boleh {@code null} bila isi disimpan di Google Drive
	 */
	public void setFoto(Blob foto) {

//		try {
//			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//			for (StackTraceElement element : stackTrace) {
//				// Filter hanya class dari package 'ais.' dan bukan class ini sendiri
//				if (element.getClassName().startsWith("ais.")
//						&& !element.getClassName().equals(this.getClass().getName())) {
//					System.err.println(">>> PEMANGGIL DITEMUKAN DI: " + element.getClassName() + "."
//							+ element.getMethodName() + " (Baris: " + element.getLineNumber() + ")");
//					// Kita print semua stack 'ais' agar terlihat alurnya
//					// break; // Hapus break jika ingin melihat urutan lengkap
//				}
//			}
//		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/LampiranLain.java:430");
//			// TODO: handle exception
//		}

		this.foto = foto;
	}

	/**
	 * Isi berkas sebagai {@link Blob}, dengan dua aturan pemilihan sumber.
	 *
	 * <p><b>Pertama</b>, bila field {@link #gdrive} terisi maka method mengembalikan
	 * {@code null} secara sengaja: isinya ada di Google Drive, bukan di basis data, sehingga
	 * mengembalikan blob lama justru menyesatkan. Perhatikan pemeriksaan ini memakai
	 * <b>field langsung</b> ({@code gdrive}), bukan {@link #getGdrive()} — jadi nilai
	 * warisan dari {@code copyDari} maupun nilai yang tersimpan di berkas samping
	 * (lihat {@link #getGdrive()}) TIDAK dipertimbangkan di sini. Akibatnya baris yang
	 * file-id Drive-nya hanya ada di berkas samping tetap mengembalikan blob lokal.</p>
	 *
	 * <p><b>Kedua</b>, bila blob lokal kosong, isi dipinjam dari
	 * {@link #getCopyDari() copyDari} — lagi-lagi lewat field langsung
	 * ({@code copyDari.foto}) sehingga rantai pinjam-meminjam hanya sedalam satu tingkat
	 * dan tidak menimpa apa pun.</p>
	 *
	 * <p>Ditandai {@code @NotAudited} supaya Envers tidak menyalin isi berkas ke tabel
	 * revisi setiap kali baris berubah. Untuk membaca isinya dengan aman gunakan
	 * {@code FileFotoLain.ambilIsiBlob(...)}, karena Large Object hanya boleh dibaca di
	 * dalam transaksi.</p>
	 *
	 * @return isi berkas, atau {@code null} bila disimpan di Drive/tidak ada
	 */
	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (foto != null ? foto : (copyDari == null ? null : copyDari.foto));
	}

	/**
	 * Menyetel acuan ke baris pemilik lampiran.
	 *
	 * <p>Isilah dengan primary key baris pemilik (mis. {@code mahasiswa.getId()}) atau salah
	 * satu konstanta acuan sintetis negatif di kelas ini. <b>Jangan</b> mengisi dengan angka
	 * positif acak sebagai acuan sementara — gunakan {@code Common.refSementara()} yang
	 * dijamin negatif; acuan positif acak pernah menyebabkan berkas baru menimpa lampiran
	 * milik data lain (lihat catatan pada {@code FileFotoLain.createFileFotoLain}).</p>
	 *
	 * @param ref acuan baris pemilik
	 */
	public void setRef(Long ref) {
		this.ref = ref;
	}

	/**
	 * Acuan ke baris pemilik lampiran — separuh kunci pencarian bersama
	 * {@link #getJenis() jenis}.
	 *
	 * <p>Kolom biasa, <b>bukan foreign key</b>: tidak ada constraint apa pun yang menjamin
	 * baris pemilik masih ada, apalagi bahwa ia bertipe yang diharapkan pemanggil. Nilai
	 * {@code ref} dari tabel pemilik berbeda bertabrakan di ruang angka yang sama dan hanya
	 * dibedakan oleh {@code jenis}. Pembaca murni, tanpa efek samping.</p>
	 *
	 * @return acuan baris pemilik, atau {@code null}
	 */
	@Column(name = "ref", nullable = true)
	public Long getRef() {
		return ref;
	}

	/**
	 * Penanda "lampiran yang mana" — separuh kunci pencarian bersama {@link #getRef() ref}.
	 *
	 * <p><b>Menulis balik, tetapi hanya sebagai fallback:</b> berbeda dari
	 * {@link #getKeterangan()} atau {@link #getDeskripsi()} yang selalu ditimpa nilai
	 * {@code copyDari}, di sini nilai dari baris sumber baru diambil bila {@code jenis}
	 * lokal masih kosong. Nilai lokal yang sudah ada dipertahankan — memang harus begitu,
	 * karena {@code jenis} adalah bagian kunci: menimpanya akan memindahkan baris ini ke
	 * kunci pencarian yang lain.</p>
	 *
	 * <p>Perhatikan bahwa pencocokannya di {@code FileFotoLain.ambil(...)} memakai
	 * {@code Restrictions.eq} — <b>persis, case-sensitive, tanpa {@code trim}</b>. Spasi
	 * berlebih atau beda huruf besar-kecil membuat lampiran "hilang" tanpa pesan error.</p>
	 *
	 * @return nilai {@code jenis}, dapat {@code null}
	 */
	public String getJenis() {
		if (jenis == null || jenis.trim().isEmpty()) {
			if (copyDari != null) {
				jenis = copyDari.getJenis();
			}
		}
		return jenis;
	}

	/**
	 * Menyetel penanda {@code jenis}.
	 *
	 * <p>Gunakan salah satu konstanta di kelas ini bila tersedia, atau
	 * {@code NamaKelasPemilik.class.getName()} untuk mengikuti pola modul kepegawaian.
	 * Mengubah nilai ini pada baris yang sudah tersimpan sama dengan memindahkannya ke kunci
	 * pencarian lain sehingga layar asalnya tidak lagi menemukannya.</p>
	 *
	 * @param jenis penanda jenis lampiran
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Deskripsi panjang lampiran (kolom {@code text}).
	 *
	 * <p><b>Menulis balik:</b> bila {@link #getCopyDari() copyDari} terisi, field
	 * {@code deskripsi} selalu ditimpa dengan deskripsi baris sumber — nilai lokal yang
	 * sudah diisi pengguna akan hilang begitu getter ini dipanggil.</p>
	 *
	 * @return deskripsi lampiran, dapat {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getDeskripsi() {
		if (copyDari != null) {
			deskripsi = copyDari.getDeskripsi();
		}
		return deskripsi;
	}

	/**
	 * Menyetel deskripsi panjang lampiran.
	 *
	 * @param deskripsi deskripsi lampiran
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Baris lampiran SUMBER yang isinya dipinjam baris ini — satu-satunya relasi objek pada
	 * entity ini, dan ke tabel yang sama (self-reference).
	 *
	 * <p><b>Bukan sekadar jejak duplikasi.</b> Bila terisi, hampir seluruh getter di kelas
	 * ini membaca dari baris sumber: {@link #getNama()}, {@link #getKeterangan()},
	 * {@link #getDeskripsi()}, {@link #getLink()}, {@link #getGdrive()},
	 * {@link #getGdriveUsername()} <b>menimpa</b> field lokal dengan nilai sumber, sementara
	 * {@link #getJenis()} dan {@link #getFoto()} hanya memakainya sebagai cadangan. Jadi
	 * mengisi {@code copyDari} membuat baris ini menjadi semacam alias hidup dari baris
	 * sumber, bukan salinan mati.</p>
	 *
	 * <p>Pemetaannya sengaja longgar: {@code @NotFound(IGNORE)} membuat baris sumber yang
	 * sudah dihapus tidak melempar {@code EntityNotFoundException} melainkan menjadi
	 * {@code null}, dan {@code @Fetch(SELECT)} menghindari join yang memaksa Hibernate
	 * memuat blob sumber. Cascade dibatasi ke {@code PERSIST}/{@code MERGE} — <b>tidak</b>
	 * termasuk {@code REMOVE}, sehingga menghapus baris ini tidak ikut menghapus sumbernya.</p>
	 *
	 * <p>Nilainya diisi secara reflektif oleh {@code FileFotoLain.createFileFotoLain} lewat
	 * {@code clazz.getMethod("setCopyDari", clazz)}; kegagalan pencarian method itu ditelan
	 * diam-diam, jadi entity lampiran yang tidak punya {@code setCopyDari} tetap berfungsi
	 * tanpa peringatan.</p>
	 *
	 * @return baris lampiran sumber, atau {@code null} bila baris ini berdiri sendiri
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public LampiranLain getCopyDari() {
		return copyDari;
	}

	/**
	 * Menyetel baris lampiran sumber.
	 *
	 * <p>Perhatikan efeknya: setelah dipanggil, sebagian besar getter kelas ini akan
	 * menimpa field lokal dengan nilai baris sumber. Jangan memakainya sekadar untuk
	 * mencatat asal-usul data.</p>
	 *
	 * @param copyDari baris lampiran sumber; {@code null} untuk melepas keterkaitan
	 */
	public void setCopyDari(LampiranLain copyDari) {
		this.copyDari = copyDari;
	}

	/**
	 * URL dokumen eksternal untuk lampiran yang berupa tautan, bukan berkas terunggah —
	 * <b>getter dengan efek samping</b>.
	 *
	 * <p>Dua penulisan-balik terjadi sebelum nilai dikembalikan:</p>
	 * <ol>
	 *   <li>Bila {@link #getCopyDari() copyDari} terisi, field {@code link} <b>ditimpa</b>
	 *       dengan link baris sumber.</li>
	 *   <li>Bila {@code link} kosong tetapi field {@code nama} diawali {@code "http"}
	 *       (tanpa memedulikan huruf besar-kecil), {@code nama} <b>disalin</b> menjadi
	 *       {@code link}. Ini menangani data lama yang menyimpan URL di kolom nama berkas.
	 *       Perhatikan pemeriksaannya memakai field {@code nama} langsung, bukan
	 *       {@link #getNama()}, sehingga awalan {@code "<id>_"} yang mungkin sudah
	 *       ditempelkan getter nama TIDAK menghalangi deteksi ini pada pemanggilan pertama —
	 *       tetapi bila {@link #getNama()} sempat dipanggil lebih dulu, nama sudah berawalan
	 *       {@code "<id>_"} dan pemeriksaan {@code startsWith("http")} tidak lagi cocok.
	 *       Urutan pemanggilan getter jadi menentukan hasil.</li>
	 * </ol>
	 *
	 * <p><b>Kontrak nilai balik berbeda dari getter lain di kelas ini:</b> method ini tidak
	 * pernah mengembalikan {@code null}, melainkan string kosong. {@link #getNama()}
	 * bergantung pada sifat ini ({@code !getLink().isEmpty()}).</p>
	 *
	 * @return URL dokumen yang sudah di-{@code trim}, atau string kosong bila tidak ada
	 */
	@Column(columnDefinition = "text")
	public String getLink() {
		if (copyDari != null) {
			link = copyDari.getLink();
		}

		if (link == null || link.trim().isEmpty()) {
			if (nama != null && nama.trim().toLowerCase().startsWith("http")) {
				link = nama;
			}
		}

		return link == null ? "" : link.trim();
	}

	/**
	 * Menyetel URL dokumen eksternal.
	 *
	 * @param link URL dokumen; {@code null} berarti lampiran memakai berkas, bukan tautan
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * File-id Google Drive tempat isi berkas disimpan — <b>getter dengan efek samping DAN
	 * pembacaan berkas samping</b>.
	 *
	 * <p>Alurnya tiga langkah:</p>
	 * <ol>
	 *   <li>Bila {@link #getCopyDari() copyDari} terisi, field {@code gdrive} <b>ditimpa</b>
	 *       dengan nilai baris sumber (field langsung, bukan getter).</li>
	 *   <li>Bila nilai lokal kosong, dicoba dibaca dari <b>berkas samping</b> lewat
	 *       {@code retreive("gdrive")} — mekanisme cache berkas milik
	 *       {@link ais.database.model.GeneralValueObject} yang menyimpan nilai per
	 *       kelas+id+sufiks di sistem berkas server, di luar kolom basis data.</li>
	 *   <li>Nilai berkas samping dipakai bila tidak kosong; selain itu dikembalikan nilai
	 *       kolom.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi.</b> Nilai yang dikembalikan bisa berbeda dari isi kolom basis data,
	 * dan berbeda antar-server bila berkas samping tidak ikut disalin. Hasilnya menentukan
	 * perilaku besar: {@link #getFoto()} menolak mengembalikan blob bila field {@code gdrive}
	 * terisi, dan {@code createLinkUri()} langsung mengembalikan URL Google Drive tanpa
	 * menyentuh berkas lokal. Pemanggil yang membandingkan "apakah berkas ada di Drive"
	 * harus konsisten memilih getter ini atau field langsung — keduanya tidak selalu
	 * sepakat.</p>
	 *
	 * @return file-id Google Drive, atau {@code null}/kosong bila isi berkas ada di basis data
	 */
	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	/**
	 * Menyetel file-id Google Drive, sekaligus <b>menulis berkas ke disk</b>.
	 *
	 * <p>Setter ini tidak murni: untuk nilai non-kosong ia memanggil {@code put(gdrive,
	 * "gdrive")} yang menulis satu berkas samping di sistem berkas server (mekanisme
	 * {@link ais.database.model.GeneralValueObject}) sebelum menyetel field. Karena Hibernate
	 * memanggil setter properti terpetakan saat menghidrasi entity, <b>memuat</b> baris
	 * lampiran pun ikut memicu penulisan berkas — kecuali saat startup aplikasi, yang
	 * dijaga oleh {@code AppStartupListener.isStartupInProgress()} di dalam {@code put(...)}
	 * (penjaga itu ditambahkan setelah insiden startup macet karena ribuan tulis-berkas).</p>
	 *
	 * <p>Nilai {@code null}/kosong TIDAK menulis berkas samping, tetapi TETAP disetel ke
	 * field. Akibatnya berkas samping lama tidak ikut terhapus, sehingga
	 * {@link #getGdrive()} masih dapat memunculkan file-id lama meski kolom sudah
	 * dikosongkan.</p>
	 *
	 * @param gdrive file-id Google Drive; {@code null}/kosong berarti isi berkas di basis data
	 */
	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	/**
	 * Akun Google yang dipakai saat mengunggah berkas ke Drive.
	 *
	 * <p><b>Menulis balik:</b> bila {@link #getCopyDari() copyDari} terisi, field ini ditimpa
	 * dengan nilai baris sumber. Berbeda dari {@link #getGdrive()}, tidak ada pembacaan
	 * berkas samping di sini.</p>
	 *
	 * @return nama akun Google pengunggah, atau {@code null}
	 */
	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	/**
	 * Menyetel akun Google pengunggah.
	 *
	 * @param gdriveUsername nama akun Google
	 */
	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	/**
	 * Membuang cache lokasi lampiran di disk untuk satu kunci ({@code ref}, {@code jenis}),
	 * sehingga pemanggilan {@code ambil(...)} berikutnya kembali menembak basis data.
	 *
	 * <p><b>Kenapa perlu.</b> {@code FileFotoLain.ambil(...)} tidak selalu menembak basis
	 * data: hasil query sebelumnya disimpan sebagai JSON pada sebuah berkas di sistem berkas
	 * server, dengan kunci gabungan {@code clazz + ref + jenis} (ditambah sisipan
	 * {@code "_id"} bila {@code usingId} bernilai {@code true}). Selama berkas itu ada, isi
	 * lampiran yang berubah — atau yang baru saja dihapus — masih dilaporkan versi lamanya.
	 * Karena itu method ini WAJIB dipanggil setelah setiap unggah/ganti/hapus lampiran;
	 * {@code FileFotoLain.hapusAtauUpdate}, {@code performDelete}, dan
	 * {@code delete()} sudah memanggilnya sendiri.</p>
	 *
	 * <p><b>Efek samping:</b> menulis berkas cache menjadi string kosong (bukan menghapusnya).
	 * Kegagalan I/O ditelan diam-diam oleh {@code FileFotoLain.tulisLokasi}.</p>
	 *
	 * <p><b>Catatan cakupan:</b> cache ini <b>global per server, bukan per pengguna</b> —
	 * ia menyimpan hasil query, bukan hak akses. Membuang atau mengisinya tidak memengaruhi
	 * siapa yang boleh melihat berkas.</p>
	 *
	 * @param usingId {@code true} bila kunci cache yang dibuang adalah kunci varian
	 *                "cari berdasarkan primary key lampiran"; {@code false} untuk kunci
	 *                normal berbasis acuan pemilik
	 * @param ref     acuan baris pemilik (atau primary key lampiran bila {@code usingId})
	 * @param jenis   penanda jenis lampiran
	 */
	public static void resetLokasi(Boolean usingId, Long ref, String jenis) {
		FileFotoLain.resetLokasi(usingId, ref, jenis, LampiranLain.class);
	}

	/**
	 * Mengambil SATU baris lampiran, dengan pilihan mode pencocokan acuan.
	 *
	 * <h4>Cara pencocokan bekerja (delegasi ke {@code FileFotoLain.ambil})</h4>
	 * <p>Untuk kelas ini nama field acuan yang terdaftar di {@code FileFotoLain.RELASI_MAP}
	 * adalah {@code "ref"}, dan karena nama itu bukan {@code "id"}/{@code "tbmuser"} serta
	 * nama kelas ini tidak berawalan {@code "Foto"} maupun berakhiran {@code "FileContent"},
	 * penyaringan berdasarkan {@code jenis} <b>aktif</b>. Kriteria yang dibangun:</p>
	 * <ul>
	 *   <li>{@code usingId == false} (perilaku normal) &rarr;
	 *       {@code Restrictions.eq("jenis", jenis)} <b>DAN</b>
	 *       {@code Restrictions.eq("ref", ref)}; kedua-duanya kecocokan <b>persis</b>
	 *       (case-sensitive, tanpa {@code trim}).</li>
	 *   <li>{@code usingId == true} &rarr; <b>penyaringan {@code jenis} DIMATIKAN</b>
	 *       (diganti {@code sqlRestriction("true")}) dan pencocokan berpindah ke
	 *       {@code Restrictions.idEq(ref)}, yaitu <b>primary key baris
	 *       {@code lampiran_lain} itu sendiri</b> — bukan lagi acuan ke baris pemilik.</li>
	 * </ul>
	 * <p>Hasil diurutkan {@code Order.desc("id")} dengan {@code setMaxResults(1)}, jadi yang
	 * dikembalikan adalah baris <b>terbaru</b> untuk kunci tersebut; unggahan lama dengan
	 * kunci sama tetap tersimpan di tabel tetapi tidak pernah terpilih lewat jalur ini.
	 * Hasilnya lalu disimpan ke cache berkas (lihat {@link #resetLokasi(Boolean, Long, String)}),
	 * dan ketiadaan hasil dicatat sebagai {@code "0"} agar pencarian berikutnya tidak
	 * mengulang query.</p>
	 *
	 * <h4>TIDAK ADA VALIDASI KEPEMILIKAN DI SINI</h4>
	 * <p>Method ini — dan seluruh rantai {@code FileFotoLain.ambil(...)} di baliknya —
	 * adalah <b>query murni</b>. Tidak ada pemeriksaan pengguna login, peran, tenant
	 * (yayasan/sekolah/kampus), maupun keterkaitan antara pemanggil dan baris pemilik.
	 * Siapa yang boleh melihat berkas sepenuhnya menjadi tanggung jawab pemanggil.</p>
	 *
	 * <p><b>Mengapa itu penting.</b> Servlet {@code ais.action.servlet.AmbilLampiran}
	 * (dipetakan ke {@code /AmbilLampiran} dan alias publik {@code /al}) memanggil rantai
	 * ini dengan {@code ref}, {@code jenis}, {@code clazz}, dan {@code usingId} yang diambil
	 * <b>mentah dari query string</b> bila token terenkripsi {@code d} tidak dikirim, dan
	 * servlet itu tidak memiliki satu pun pemeriksaan hak akses. Kombinasi
	 * {@code usingId = true} + primary key {@code IDENTITY} yang berurutan berarti seluruh
	 * isi tabel lampiran dapat ditelusuri hanya dengan menaikkan satu angka — tanpa perlu
	 * menebak {@code jenis} sama sekali, karena mode itu mematikan penyaringnya. Lihat
	 * dokumen {@code src/ais/action/servlet/SECURITY_FINDING_AmbilLampiran_IDOR.md} dan
	 * {@code task_b82b25d2}. Jangan menambah pemanggil baru yang meneruskan {@code ref}
	 * dari parameter klien tanpa memeriksa kepemilikan lebih dulu.</p>
	 *
	 * @param usingId {@code true} untuk mencocokkan {@code ref} ke primary key baris
	 *                lampiran (dan mengabaikan {@code jenis}); {@code false} untuk
	 *                pencocokan normal ke acuan pemilik
	 * @param ref     acuan baris pemilik, atau primary key lampiran bila {@code usingId}
	 * @param jenis   penanda jenis lampiran; diabaikan bila {@code usingId} bernilai
	 *                {@code true}
	 * @return baris lampiran terbaru yang cocok, atau {@code null} bila tidak ada
	 *         (juga {@code null} bila terjadi kegagalan query — pengecualiannya ditelan dan
	 *         dicatat ke audit error, bukan dilempar ke pemanggil)
	 */
	public static LampiranLain ambil(Boolean usingId, Long ref, String jenis) {
		// TODO Auto-generated method stub
		return (LampiranLain) FileFotoLain.ambil(usingId, ref, jenis, LampiranLain.class);
	}

	/**
	 * Kelas konkret baris ini, dipakai kelas induk untuk membangun query dan HQL secara
	 * generik.
	 *
	 * <p>Diperlukan karena {@code FileFotoLain} menangani banyak subkelas lampiran sekaligus
	 * dan tidak dapat mengandalkan {@code getClass()} — objek yang dibaca Hibernate bisa
	 * berupa proxy. Nilainya menjadi kunci pencarian nama field acuan di
	 * {@code FileFotoLain.RELASI_MAP} ({@code LampiranLain} &rarr; {@code "ref"}) dan
	 * dipakai {@code hapusAtauUpdate} untuk menyusun nama entity pada HQL.</p>
	 *
	 * @return selalu {@code LampiranLain.class}
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		return LampiranLain.class;
	}

	/**
	 * Mengambil satu baris lampiran berdasarkan pasangan ({@code ref}, {@code jenis}),
	 * dengan kendali atas penggunaan cache.
	 *
	 * <p>Setara {@link #ambil(Boolean, Long, String)} dengan {@code usingId = false},
	 * ditambah parameter {@code refresh}. Bila {@code refresh} bernilai {@code true}, cache
	 * berkas di disk <b>dilewati</b> dan query langsung ditembakkan ke basis data (hasilnya
	 * tetap ditulis ulang ke cache). Pakai ini setelah operasi tulis yang belum sempat
	 * memanggil {@link #resetLokasi(Boolean, Long, String)}, atau ketika data terbaru
	 * mutlak dibutuhkan.</p>
	 *
	 * <p>Peringatan keamanan pada {@link #ambil(Boolean, Long, String)} berlaku sama persis
	 * di sini: tidak ada pemeriksaan kepemilikan apa pun.</p>
	 *
	 * @param ref     acuan baris pemilik
	 * @param jenis   penanda jenis lampiran (kecocokan persis, case-sensitive)
	 * @param refresh {@code true} untuk melewati cache berkas dan memaksa query basis data
	 * @return baris lampiran terbaru yang cocok, atau {@code null}
	 */
	public static LampiranLain ambil(Long ref, String jenis, boolean refresh) {
		return (LampiranLain) FileFotoLain.ambil(ref, jenis, LampiranLain.class, refresh);
	}

	/**
	 * Bentuk paling sering dipakai: mengambil satu baris lampiran berdasarkan pasangan
	 * ({@code ref}, {@code jenis}), memanfaatkan cache berkas bila tersedia.
	 *
	 * <p>Setara {@link #ambil(Long, String, boolean)} dengan {@code refresh = false}.
	 * Karena memakai cache, hasilnya bisa <b>basi</b> bila ada penulisan yang tidak
	 * memanggil {@link #resetLokasi(Boolean, Long, String)}; bila baris tidak ditemukan,
	 * ketiadaannya juga ikut di-cache sehingga lampiran yang baru diunggah bisa tampak
	 * belum ada sampai cache dibersihkan.</p>
	 *
	 * <p>Contoh pemakaian tipikal — mencari lampiran milik satu baris master dengan
	 * {@code jenis} berupa nama kelas Java pemiliknya:
	 * {@code LampiranLain.ambil(komponen.getId(), KomponenDataProdukKursus.class.getName())}.</p>
	 *
	 * <p>Peringatan keamanan pada {@link #ambil(Boolean, Long, String)} berlaku sama persis
	 * di sini: tidak ada pemeriksaan kepemilikan apa pun.</p>
	 *
	 * @param ref   acuan baris pemilik
	 * @param jenis penanda jenis lampiran (kecocokan persis, case-sensitive)
	 * @return baris lampiran terbaru yang cocok, atau {@code null}
	 */
	public static LampiranLain ambil(Long ref, String jenis) {
		return (LampiranLain) FileFotoLain.ambil(ref, jenis, LampiranLain.class);
	}

	/**
	 * Resolusi {@code jenis} efektif untuk lampiran milik {@code ownerClass}, dengan
	 * namespace nama kelas pemilik untuk mencegah tabrakan lintas-entitas pada tabel
	 * {@code lampiran_lain} bersama (lihat catatan keamanan sub-temuan jenis-namespace
	 * collision, task_b82b25d2, SECURITY_FINDING_AmbilLampiran_IDOR.md).
	 *
	 * <p>Dipakai oleh keluarga {@code ParameterTambahan*Listener}, yang sebelumnya
	 * membangun {@code jenis} tanpa penanda kelas pemilik (mis. hanya
	 * {@code kelompokId + "->" + parameterId}) sehingga dua entitas berbeda dengan
	 * pasangan id yang kebetulan sama akan saling mengambil lampiran satu sama lain.</p>
	 *
	 * <p><b>Migrasi tanpa penulisan ulang basis data:</b> dicoba dulu bentuk baru
	 * ({@code ownerClass.getName() + "#" + jenisMentah}); bila tidak ada baris yang
	 * cocok, dicoba {@code jenisMentah} apa adanya untuk tetap menemukan baris lama
	 * (pra-perbaikan) milik {@code ref} ini. Bila baris lama ditemukan, string lama itu
	 * dikembalikan sehingga sisa render/upload pada request ini tetap konsisten dengan
	 * baris yang sudah ada; bila tidak ada baris sama sekali, bentuk baru (ber-namespace)
	 * dikembalikan agar unggahan berikutnya sudah aman sejak awal.</p>
	 *
	 * @param ownerClass kelas entitas pemilik lampiran (mis. {@code CatatanPegawai.class})
	 * @param ref        acuan baris pemilik
	 * @param jenisMentah penanda jenis tanpa namespace, seperti yang dibangun pemanggil
	 * @return {@code jenisMentah} bila baris lama ditemukan, selain itu bentuk ber-namespace
	 */
	public static String resolveJenisParameterTambahan(Class<?> ownerClass, Long ref, String jenisMentah) {
		if (ref == null || jenisMentah == null) {
			return jenisMentah;
		}
		String jenisBaru = ownerClass.getName() + "#" + jenisMentah;
		if (LampiranLain.ambil(ref, jenisBaru) != null) {
			return jenisBaru;
		}
		if (LampiranLain.ambil(ref, jenisMentah) != null) {
			return jenisMentah;
		}
		return jenisBaru;
	}

	/**
	 * Mengambil kunci jawaban parameter tambahan dari jenis lampiran efektif.
	 *
	 * <p>Nilai parameter pada kolom {@code parameterTambahanInds*} tetap memakai format lama
	 * {@code kelompokId->parameterId}, sedangkan jenis lampiran baru diberi namespace
	 * {@code nama.kelas#kelompokId->parameterId}. Keduanya sengaja tidak disamakan: namespace
	 * diperlukan untuk mencegah tabrakan lampiran lintas entitas, tetapi tidak boleh mengubah
	 * kontrak data jawaban yang sudah tersimpan. Method ini menjadi satu-satunya tempat untuk
	 * menurunkan jenis lampiran kembali menjadi kunci jawaban yang kompatibel.</p>
	 *
	 * @param jenis jenis lampiran mentah atau yang sudah ber-namespace
	 * @return kunci jawaban tanpa namespace; {@code null} bila masukan {@code null}
	 */
	public static String kunciNilaiParameterTambahan(String jenis) {
		if (jenis == null) {
			return null;
		}
		int pemisah = jenis.lastIndexOf('#');
		return pemisah < 0 ? jenis : jenis.substring(pemisah + 1);
	}

	/*
	 * ================================================================================
	 * PABRIK KOMPONEN ZK: createDownloadUploadFileLain(...)
	 * --------------------------------------------------------------------------------
	 * Delapan overload yang seluruhnya bermuara ke satu bentuk terlengkap (15 parameter),
	 * yang selanjutnya mendelegasikan ke FileFotoLain.createDownloadUpload(...) dengan
	 * clazz = LampiranLain.class.
	 *
	 * Kegunaannya: merakit satu blok kendali lampiran di layar ZK -- tombol Download,
	 * tombol Upload/Ganti, tombol Hapus (dengan dialog konfirmasi), opsional pratinjau dan
	 * tombol simpan-ke-Google-Drive. Method ini MEMBACA basis data (memanggil
	 * FileFotoLain.ambil untuk tahu apakah lampirannya sudah ada) dan komponen yang
	 * dirakitnya DAPAT MENULIS/MENGHAPUS baris lampiran saat diklik pengguna.
	 *
	 * CATATAN HAK AKSES: tidak ada satu pun overload di sini yang memeriksa hak akses.
	 * Tombol Hapus selalu ikut dirakit ketika tampilUpload bernilai true, apa pun peran
	 * penggunanya. Pembatasan siapa boleh mengunggah/menghapus HARUS dikerjakan layar
	 * pemanggil (mis. dengan tidak memanggil overload ini, atau meneruskan
	 * tampilUpload = false untuk pengguna yang hanya boleh melihat).
	 *
	 * Kuirk penamaan: parameter ukuran unggah maksimum bernama "cutomUkuranUpload"
	 * (seharusnya "custom"); salah ketiknya konsisten di seluruh overload dan di kelas
	 * induk, jadi dipertahankan agar tidak memecah pemanggil.
	 * ================================================================================
	 */

	/**
	 * Bentuk paling ringkas: merakit blok unduh/unggah lampiran dengan seluruh opsi tampilan
	 * pada nilai bakunya (bukan hanya ikon, kombo jurusan disembunyikan, tombol unggah
	 * ditampilkan, tanpa batas ukuran khusus).
	 *
	 * @param row           komponen ZK induk tempat blok tombol ditempelkan
	 * @param myref         acuan baris pemilik; bila {@code null} diganti
	 *                      {@code Common.refSementara()} (acuan sementara negatif) supaya
	 *                      berkas dapat diunggah sebelum baris pemiliknya tersimpan
	 * @param jenis         penanda jenis lampiran
	 * @param keterangan    label yang muncul pada tombol dan dialog
	 * @param harusPdf      {@code true} untuk membatasi unggahan hanya berkas PDF
	 * @param eventListener callback yang dipanggil setelah unggahan berhasil; boleh
	 *                      {@code null}
	 */
	public static void createDownloadUploadFileLain(Component row, Long myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener) {
		Long ref = myref == null ? Common.refSementara() : myref;
		LampiranLain.createDownloadUploadFileLain(row, ref, jenis, keterangan, harusPdf, eventListener, null, false,
				false, false, true, null);
	}

	/**
	 * Seperti
	 * {@link #createDownloadUploadFileLain(Component, Long, String, String, Boolean, EventListener)},
	 * ditambah batas ukuran unggah khusus.
	 *
	 * @param row               komponen ZK induk
	 * @param myref             acuan baris pemilik; {@code null} diganti acuan sementara
	 * @param jenis             penanda jenis lampiran
	 * @param keterangan        label tombol/dialog
	 * @param harusPdf          {@code true} untuk membatasi unggahan hanya PDF
	 * @param eventListener     callback setelah unggahan berhasil; boleh {@code null}
	 * @param cutomUkuranUpload batas ukuran unggah khusus (satuan mengikuti
	 *                          {@code AmbilDataLampiranFileLain}); {@code null} memakai
	 *                          batas baku sistem
	 */
	public static void createDownloadUploadFileLain(Component row, Long myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Integer cutomUkuranUpload) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUploadFileLain(row, ref, jenis, keterangan, harusPdf, eventListener, null, cutomUkuranUpload);
	}

	/**
	 * Seperti overload berbatas ukuran, ditambah penampung hasil {@code lampiranLains}.
	 *
	 * @param row               komponen ZK induk
	 * @param myref             acuan baris pemilik; {@code null} diganti acuan sementara
	 * @param jenis             penanda jenis lampiran
	 * @param keterangan        label tombol/dialog
	 * @param harusPdf          {@code true} untuk membatasi unggahan hanya PDF
	 * @param eventListener     callback setelah unggahan berhasil; boleh {@code null}
	 * @param lampiranLains     map yang <b>diisi</b> oleh method ini bila lampiran ditemukan,
	 *                          dengan {@code jenis} sebagai kunci; berguna agar layar dapat
	 *                          memeriksa kelengkapan berkas tanpa query ulang. Boleh
	 *                          {@code null}
	 * @param cutomUkuranUpload batas ukuran unggah khusus; {@code null} memakai batas baku
	 */
	public static void createDownloadUploadFileLain(Component row, Long myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Map<String, LampiranLain> lampiranLains,
			Integer cutomUkuranUpload) {
		Long ref = myref == null ? Common.refSementara() : myref;
		LampiranLain.createDownloadUploadFileLain(row, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				false, false, false, true, cutomUkuranUpload);
	}

	/**
	 * Seperti bentuk paling ringkas, ditambah penampung hasil {@code lampiranLains}.
	 *
	 * @param row           komponen ZK induk
	 * @param myref         acuan baris pemilik; {@code null} diganti acuan sementara
	 * @param jenis         penanda jenis lampiran
	 * @param keterangan    label tombol/dialog
	 * @param harusPdf      {@code true} untuk membatasi unggahan hanya PDF
	 * @param eventListener callback setelah unggahan berhasil; boleh {@code null}
	 * @param lampiranLains map yang diisi hasil pencarian, berkunci {@code jenis}; boleh
	 *                      {@code null}
	 */
	public static void createDownloadUploadFileLain(Component row, Long myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Map<String, LampiranLain> lampiranLains) {
		Long ref = myref == null ? Common.refSementara() : myref;
		LampiranLain.createDownloadUploadFileLain(row, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				false, false, false, true, null);
	}

	/**
	 * Bentuk dengan kendali tampilan lengkap, tanpa batas ukuran khusus.
	 *
	 * <p><b>Perhatikan:</b> mulai overload ini {@code myref} <b>tidak lagi</b> diganti
	 * dengan acuan sementara di kelas ini — nilai {@code null} diteruskan apa adanya dan
	 * baru ditangani {@code FileFotoLain.createDownloadUpload}. Perbedaan kecil ini mudah
	 * terlewat saat berpindah antar-overload.</p>
	 *
	 * @param row                komponen ZK induk
	 * @param myref              acuan baris pemilik; boleh {@code null}
	 * @param jenis              penanda jenis lampiran
	 * @param keterangan         label tombol/dialog
	 * @param harusPdf           {@code true} untuk membatasi unggahan hanya PDF
	 * @param eventListener      callback setelah unggahan berhasil; boleh {@code null}
	 * @param lampiranLains      map yang diisi hasil pencarian; boleh {@code null}
	 * @param tidakTampilJurusan {@code true} menyembunyikan kombo Jurusan. Kombo itu hanya
	 *                           muncul bila konfigurasi
	 *                           {@code upload_file_di_konfigurasi_tiap_jurusan_bisa_beda}
	 *                           bernilai {@code "Y"}
	 * @param hanyaIcon          {@code true} merender tombol sebagai ikon tanpa teks
	 * @param usingId            {@code true} membuat pencarian lampiran memakai primary key
	 *                           baris lampiran, bukan acuan pemilik — sekaligus mematikan
	 *                           penyaringan {@code jenis}; lihat
	 *                           {@link #ambil(Boolean, Long, String)}
	 * @param tampilUpload       {@code true} menampilkan tombol Upload/Ganti dan Hapus;
	 *                           {@code false} membuat blok ini hanya-baca (dan menyembunyikan
	 *                           seluruh baris bila lampiran belum ada)
	 */
	public static void createDownloadUploadFileLain(Component row, Long myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Map<String, LampiranLain> lampiranLains,
			Boolean tidakTampilJurusan, Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload) {
		LampiranLain.createDownloadUploadFileLain(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, null);
	}

	/**
	 * Bentuk dengan kendali tampilan lengkap plus batas ukuran unggah khusus; tata letak
	 * dipaksa horizontal dan pratinjau di layar utama diizinkan.
	 *
	 * @param row                komponen ZK induk
	 * @param myref              acuan baris pemilik; boleh {@code null}
	 * @param jenis              penanda jenis lampiran
	 * @param keterangan         label tombol/dialog
	 * @param harusPdf           {@code true} untuk membatasi unggahan hanya PDF
	 * @param eventListener      callback setelah unggahan berhasil; boleh {@code null}
	 * @param lampiranLains      map yang diisi hasil pencarian; boleh {@code null}
	 * @param tidakTampilJurusan {@code true} menyembunyikan kombo Jurusan
	 * @param hanyaIcon          {@code true} merender tombol sebagai ikon tanpa teks
	 * @param usingId            {@code true} mencari berdasarkan primary key lampiran
	 * @param tampilUpload       {@code true} menampilkan tombol Upload/Ganti dan Hapus
	 * @param cutomUkuranUpload  batas ukuran unggah khusus; {@code null} memakai batas baku
	 */
	public static void createDownloadUploadFileLain(Component row, Long myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Map<String, LampiranLain> lampiranLains,
			Boolean tidakTampilJurusan, Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload,
			Integer cutomUkuranUpload) {
		LampiranLain.createDownloadUploadFileLain(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, false, true);

	}

	/**
	 * Bentuk lengkap dengan kendali arah tata letak dan pratinjau, tanpa wadah pratinjau
	 * terpisah.
	 *
	 * @param row                       komponen ZK induk
	 * @param myref                     acuan baris pemilik; boleh {@code null}
	 * @param jenis                     penanda jenis lampiran
	 * @param keterangan                label tombol/dialog
	 * @param harusPdf                  {@code true} untuk membatasi unggahan hanya PDF
	 * @param eventListener             callback setelah unggahan berhasil; boleh {@code null}
	 * @param lampiranLains             map yang diisi hasil pencarian; boleh {@code null}
	 * @param tidakTampilJurusan        {@code true} menyembunyikan kombo Jurusan
	 * @param hanyaIcon                 {@code true} merender tombol sebagai ikon tanpa teks
	 * @param usingId                   {@code true} mencari berdasarkan primary key lampiran
	 * @param tampilUpload              {@code true} menampilkan tombol Upload/Ganti dan Hapus
	 * @param cutomUkuranUpload         batas ukuran unggah khusus; boleh {@code null}
	 * @param vertical                  {@code true} menyusun tombol secara vertikal
	 *                                  ({@code Vbox}); diabaikan bila {@code hanyaIcon}
	 *                                  bernilai {@code true}
	 * @param janganPreviewDiLayarUtama {@code true} menekan pratinjau berkas langsung di
	 *                                  layar utama
	 */
	public static void createDownloadUploadFileLain(Component row, Long myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Map<String, LampiranLain> lampiranLains,
			Boolean tidakTampilJurusan, Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload,
			Integer cutomUkuranUpload, Boolean vertical, Boolean janganPreviewDiLayarUtama) {
		Component parentPreview = null;
		LampiranLain.createDownloadUploadFileLain(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, parentPreview);
	}

	/**
	 * Bentuk TERLENGKAP — seluruh overload lain bermuara ke sini, yang meneruskan ke
	 * {@code FileFotoLain.createDownloadUpload(...)} dengan {@code clazz =
	 * LampiranLain.class}.
	 *
	 * <p><b>Yang dikerjakan di balik layar</b> (lihat
	 * {@code FileFotoLain.createDownloadUpload}):</p>
	 * <ol>
	 *   <li>Merakit {@code Vbox} berisi wadah tombol dan wadah pratinjau.</li>
	 *   <li>Memanggil {@code FileFotoLain.ambil(usingId, ref, jenis, clazz, refresh)} —
	 *       artinya method ini <b>menyentuh basis data</b> (atau cache berkasnya) setiap
	 *       kali dipanggil, termasuk saat sekadar merender ulang satu baris grid.</li>
	 *   <li>Bila lampiran ada: memasang aksi unduh, menyalin nama berkas ke label tombol
	 *       (dengan membuang awalan {@code "<id>_"}), dan — bila kebijakan Drive
	 *       mengizinkan — menawarkan tombol simpan-ke-Google-Drive.</li>
	 *   <li>Bila {@code tampilUpload} bernilai {@code true}: memasang tombol Upload/Ganti
	 *       yang membuka dialog {@code AmbilDataLampiranFileLain}, serta tombol Hapus dengan
	 *       dialog konfirmasi yang memanggil {@code performDelete(...)}. Penghapusan bersifat
	 *       <i>soft delete</i>: acuan {@code ref} baris lampiran diubah menjadi penanda
	 *       {@code -111111119}, jadi berkasnya tidak benar-benar hilang dari tabel —
	 *       hanya lepas dari pemiliknya.</li>
	 * </ol>
	 *
	 * <p><b>Tidak ada pemeriksaan hak akses di jalur ini.</b> Kehadiran tombol Hapus
	 * ditentukan semata oleh {@code tampilUpload}, bukan oleh peran pengguna.</p>
	 *
	 * @param row                       komponen ZK induk tempat blok tombol ditempelkan
	 * @param myref                     acuan baris pemilik; {@code null} akan diganti acuan
	 *                                  sementara negatif oleh kelas induk
	 * @param jenis                     penanda jenis lampiran
	 * @param keterangan                label tombol/dialog, juga dipakai sebagai nama berkas
	 *                                  tujuan pada unduhan {@code .jrxml}/{@code .xml}
	 * @param harusPdf                  {@code true} untuk membatasi unggahan hanya PDF
	 * @param eventListener             callback setelah unggahan berhasil; boleh {@code null}
	 * @param lampiranLains             map yang diisi hasil pencarian, berkunci {@code jenis};
	 *                                  boleh {@code null}
	 * @param tidakTampilJurusan        {@code true} menyembunyikan kombo Jurusan
	 * @param hanyaIcon                 {@code true} merender tombol sebagai ikon tanpa teks
	 * @param usingId                   {@code true} mencari berdasarkan primary key baris
	 *                                  lampiran dan mengabaikan {@code jenis}
	 * @param tampilUpload              {@code true} menampilkan tombol Upload/Ganti dan Hapus
	 * @param cutomUkuranUpload         batas ukuran unggah khusus; boleh {@code null}
	 * @param vertical                  {@code true} menyusun tombol secara vertikal
	 * @param janganPreviewDiLayarUtama {@code true} menekan pratinjau berkas di layar utama
	 * @param parentPreview             wadah pratinjau milik pemanggil; bila {@code null}
	 *                                  sebuah {@code Vbox} baru dibuat di dalam blok ini
	 */
	public static void createDownloadUploadFileLain(Component row, final Long myref, final String jenis,
			final String keterangan, final Boolean harusPdf, final EventListener eventListener,
			final Map<String, LampiranLain> lampiranLains, final Boolean tidakTampilJurusan, final Boolean hanyaIcon,
			final Boolean usingId, final Boolean tampilUpload, final Integer cutomUkuranUpload, final Boolean vertical,
			final Boolean janganPreviewDiLayarUtama, final Component parentPreview) {
		FileFotoLain.createDownloadUpload(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, parentPreview, LampiranLain.class);
	}

	/**
	 * Implementasi kontrak induk: acuan baris pemilik lampiran ini.
	 *
	 * <p>Dipakai kelas induk untuk membangun URL lampiran
	 * ({@code FileFotoLain.ambilLinkLampiranLain}) dan untuk membuang cache lokasi saat
	 * baris dihapus ({@code FileFotoLain.delete()}). Membaca field langsung, bukan
	 * {@link #getRef()}, sehingga bebas efek samping — meski dalam kasus ini keduanya
	 * setara karena {@code getRef()} juga pembaca murni.</p>
	 *
	 * @return acuan baris pemilik, atau {@code null}
	 */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return ref;
	}

	/**
	 * Implementasi kontrak induk: URL dokumen eksternal baris ini.
	 *
	 * <p>Meneruskan ke {@link #getLink()}, jadi <b>ikut membawa efek sampingnya</b>
	 * (menimpa nilai dari {@code copyDari}, menyalin {@code nama} yang berupa URL ke
	 * kolom {@code link}) dan mengembalikan string kosong, bukan {@code null}, bila tidak
	 * ada tautan.</p>
	 *
	 * @return URL dokumen, atau string kosong
	 */
	@Override
	public String ambilLink() {
		return getLink();
	}

	/**
	 * URL siap-pakai hasil {@code createLinkUri()}, di-cache pada instance.
	 * Tidak terpetakan ke kolom — lihat {@link #getUrl()}.
	 */
	private String url;
	/** Lokasi berkas fisik di server bila isinya tidak disimpan sebagai blob. */
	private String lokasiFisik;

	/**
	 * Implementasi kontrak induk: penanda jenis lampiran ini.
	 *
	 * <p>Sengaja membaca field {@code jenis} langsung, bukan {@link #getJenis()}, sehingga
	 * <b>tidak</b> mengambil nilai cadangan dari {@code copyDari}. Akibatnya method ini
	 * dapat mengembalikan {@code null} untuk baris yang jenisnya hanya diwarisi dari baris
	 * sumber, padahal {@code getJenis()} akan mengembalikan nilai. Perbedaan halus ini
	 * berpengaruh: {@code FileFotoLain.ambilLinkLampiranLain} memakai {@code getJenis()}
	 * untuk menyusun token URL, sedangkan pemanggil lain yang memakai {@code ambilJenis()}
	 * bisa mendapat {@code null}.</p>
	 *
	 * @return penanda jenis apa adanya, dapat {@code null}
	 */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	/**
	 * URL siap-pakai untuk mengakses berkas lampiran ini — <b>bukan properti basis data dan
	 * bukan operasi murni</b>.
	 *
	 * <p>Ditandai {@code @Transient} sehingga tidak disimpan; nilainya dibangun sekali per
	 * instance (lazy) lalu di-cache di field {@link #url}. Pembangunannya lewat
	 * {@code FileFotoLain.createLinkUri()}, yang berperilaku sebagai berikut:</p>
	 * <ol>
	 *   <li>Bila {@link #getGdrive()} terisi, dikembalikan URL Google Drive.</li>
	 *   <li>Selain itu, berkas fisik baris ini <b>DISALIN ke direktori media</b>
	 *       ({@code CommonMedia.getMediaDirectory()}) dengan nama hasil enkripsi
	 *       {@code <id> + "LampiranLain"} yang tanda bacanya dibuang. Nama itu memang tidak
	 *       terbaca manusia, tetapi <b>deterministik</b> — bukan rahasia yang berubah
	 *       tiap permintaan.</li>
	 *   <li>Terakhir dipanggil {@code ambilLinkLampiranLain(...)} yang, bila berkas dengan
	 *       nama itu ada di direktori media, mengembalikan <b>URL STATIS</b> berbentuk
	 *       {@code /f<prefix>/<NamaKelas>/<id>/<namaBerkas>} — bukan URL {@code /al?d=...}.</li>
	 * </ol>
	 *
	 * <p><b>Implikasi keamanan (penting untuk {@code task_b82b25d2}).</b> Karena berkas
	 * disalin ke direktori media dan disajikan sebagai berkas statis, permintaan ke URL
	 * tersebut <b>tidak melewati servlet {@code AmbilLampiran} sama sekali</b>. Segala
	 * mitigasi yang dipasang di level servlet — pemeriksaan kepemilikan, daftar putih
	 * {@code jenis}, pembatasan token — tidak berlaku pada jalur ini. Menutup IDOR di
	 * servlet saja tidak cukup selama getter ini masih menyalin berkas ke area publik.</p>
	 *
	 * <p><b>Penanganan galat:</b> pengecualian ditelan, dicetak ke {@code stderr}, dan
	 * dicatat ke {@code ErrorAuditUtil}; method mengembalikan {@code null} tanpa melempar.
	 * Bila {@link #getId()} masih {@code null} (baris belum tersimpan), URL tidak dibangun
	 * dan hasilnya {@code null}.</p>
	 *
	 * @return URL berkas, atau {@code null} bila belum tersimpan atau pembangunan URL gagal
	 */
	@Transient
	public String getUrl() {
		try {
			if (url == null && getId() != null) {
				url = createLinkUri();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/LampiranLain.java:646");
		}
		return url;
	}

	/**
	 * Menyetel (atau menimpa) URL cache pada instance ini.
	 *
	 * <p>Berguna untuk memaksa URL tertentu tanpa menyalin berkas ke direktori media, atau
	 * untuk mengosongkan cache dengan menyetel {@code null} agar {@link #getUrl()}
	 * membangunnya ulang. Tidak menyentuh basis data.</p>
	 *
	 * @param url URL yang dipakai, atau {@code null} untuk memaksa pembangunan ulang
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Lokasi berkas fisik di server untuk lampiran yang isinya tidak disimpan sebagai blob.
	 *
	 * <p><b>Menormalkan string kosong menjadi {@code null}</b>, sehingga pemanggil cukup
	 * memeriksa {@code != null} tanpa perlu ikut memeriksa {@code isEmpty()}. Perhatikan
	 * pemeriksaannya memakai {@code isEmpty()}, bukan {@code trim().isEmpty()} — nilai yang
	 * hanya berisi spasi tetap dianggap ada. Tidak ada efek samping; nilai field tidak
	 * diubah.</p>
	 *
	 * @return path berkas fisik, atau {@code null} bila kosong/belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getLokasiFisik() {
		return lokasiFisik == null || lokasiFisik.isEmpty() ? null : lokasiFisik;
	}

	/**
	 * Menyetel lokasi berkas fisik di server.
	 *
	 * @param lokasiFisik path berkas fisik
	 */
	public void setLokasiFisik(String lokasiFisik) {
		this.lokasiFisik = lokasiFisik;
	}
}
