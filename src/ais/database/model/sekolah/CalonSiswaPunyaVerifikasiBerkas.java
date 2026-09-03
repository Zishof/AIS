package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity Hibernate yang memetakan tabel {@code sekolah.calon_siswa_punya_verifikasi_berkas} pada
 * modul <b>PSB/PPDB</b> (penerimaan siswa baru).
 *
 * <h2>Jangan tertukar dengan {@code CalonSiswaPunyaVerifikasiParameter}</h2>
 * <p>Nama kedua kelas ini hanya berbeda pada kata terakhir, tetapi keduanya adalah <b>entity yang
 * sepenuhnya berbeda</b> — tabel berbeda, master berbeda, layar berbeda, dan bahkan helper
 * pengelola berbeda. Ringkasnya:</p>
 * <table border="1">
 * <caption>Perbandingan dua entity verifikasi PSB</caption>
 * <tr><th>&nbsp;</th><th>kelas ini ({@code …VerifikasiBerkas})</th>
 *     <th>{@code CalonSiswaPunyaVerifikasiParameter}</th></tr>
 * <tr><td>Tabel</td><td>{@code sekolah.calon_siswa_punya_verifikasi_berkas}</td>
 *     <td>{@code public.calon_siswa_punya_verifikasi_parameter}</td></tr>
 * <tr><td>Master</td><td>{@link VerifikasiKelengkapanCalonSiswa} — <b>daftar dokumen wajib</b>
 *     (Akte Kelahiran, Kartu Keluarga, Ijazah, Pas Foto, …) yang dilekatkan ke satu
 *     {@link GelombangPendaftaranPsb}</td>
 *     <td>{@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa} — kategori prestasi,
 *     dengan nilai "Tingkat" dari {@link ParameterVerifikasiCalonSiswa}</td></tr>
 * <tr><td>Kardinalitas</td><td><b>tepat satu baris</b> per pasangan (calon siswa, dokumen wajib);
 *     dibuat otomatis dan tidak pernah dihapus dari UI</td>
 *     <td>N baris bebas per (calon siswa, kategori); ditambah/dihapus manual</td></tr>
 * <tr><td>Isi tambahan</td><td>tidak ada — hanya {@code verified} + {@code keterangan}</td>
 *     <td>ada kolom {@code nama} teks bebas + FK "Tingkat"</td></tr>
 * <tr><td>Pengelola</td><td>{@code ais.action.master.sekolah.psb.VerifikasiPSBHelper}</td>
 *     <td>{@code ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper}</td></tr>
 * </table>
 *
 * <h2>Domain (terverifikasi dari kode, bukan dari nama kelas)</h2>
 * <p>Ini adalah <b>entity transaksi murni</b>, bukan master, dan namanya untuk sekali ini
 * <i>tepat</i>: satu baris = <b>status verifikasi satu jenis berkas wajib untuk satu calon
 * siswa</b>. Perannya dalam rantai PSB:</p>
 * <pre>
 * GelombangPendaftaranPsb
 *   └─ (@ManyToMany) VerifikasiKelengkapanCalonSiswa   ← MASTER: "berkas apa saja yang wajib"
 *                     └─ kelas ini (per CalonSiswa)    ← TRANSAKSI: "sudah diverifikasi belum"
 *                          └─ LampiranLain            ← BERKAS FISIK hasil unggahan
 * </pre>
 * <p>Panitia PSB mendefinisikan daftar berkas wajib per gelombang lewat layar master
 * {@code VerifikasiKelengkapanCalonSiswaAction}; saat panel verifikasi seorang calon siswa
 * dirender, satu baris entity ini <b>dibuat dan langsung di-INSERT</b> untuk setiap butir master
 * yang {@code aktif} dan belum punya pasangan (lihat "Pembuatan otomatis" di bawah).</p>
 *
 * <h2>Berkas fisik tidak disimpan di sini</h2>
 * <p>Entity ini <b>tidak menyimpan blob berkas</b>; ia hanya menjadi <b>jangkar identitas</b>.
 * Berkas hidup di {@code ais.database.model.file.LampiranLain} (basis data STREAMING terpisah),
 * ditemukan dengan pasangan kunci:</p>
 * <ul>
 * <li>{@code lampiran_lain.ref} = {@link #getId()} baris ini, dan</li>
 * <li>{@code lampiran_lain.jenis} = <b>nama kelas berkualifikasi penuh</b>
 * ({@code CalonSiswaPunyaVerifikasiBerkas.class.getName()}), dengan label {@code "Berkas"}.</li>
 * </ul>
 * <p><b>Konsekuensi non-obvious yang sama seperti pada {@code CalonSiswaPunyaVerifikasiParameter}:
 * nama kelas ini adalah <i>nilai data</i> di dalam basis data.</b> Mengganti nama kelas atau
 * memindahkannya ke paket lain akan <b>memutus tautan seluruh berkas PPDB yang sudah terunggah</b>
 * — bukan sekadar refactor kompilasi. Ada 8+ titik yang mengeja string ini
 * ({@code VerifikasiPSBHelper}, {@code CalonSiswaAction}, {@code CommonReportPsb},
 * {@code VerifikasiPSBHtmlHelper}, {@link VerifikasiKelengkapanCalonSiswa#checkBerkas(CalonSiswa)}).</p>
 *
 * <p>Perhatikan bahwa lampiran yang tergantung pada butir <b>master</b>
 * ({@code VerifikasiKelengkapanCalonSiswa.class.getName()}, label {@code "Lampiran"}) adalah hal
 * yang <i>berbeda</i>: itu contoh/formulir kosong yang disediakan panitia untuk diunduh calon
 * siswa, bukan berkas milik calon siswa.</p>
 *
 * <h2>Siapa yang membaca dan menulis baris ini</h2>
 * <ol>
 * <li>{@code ais.action.master.sekolah.psb.VerifikasiPSBHelper} — pengelola utama. Dipanggil dari
 * layar petugas {@code CalonSiswaAction} <b>dan</b> dari 12 varian formulir pendaftaran mandiri
 * ({@code PPDB1}…{@code PPDB3}, {@code PPDB_Alumni}, {@code PPDB_Simple}…{@code PPDB_Simple8}).</li>
 * <li>{@code CalonSiswaAction} — selain panel di atas, ada dasbor ringkasan yang menghitung
 * "n belum upload / n belum verifikasi" dan satu rutin ekspor berkas ke folder.</li>
 * <li>{@code ais.action.master.sekolah.psb.CommonReportPsb} — menyusun blok "Verifikasi
 * Kelengkapan Berkas" pada cetakan biodata/kartu ujian.</li>
 * <li>{@link VerifikasiKelengkapanCalonSiswa#checkBerkas(CalonSiswa)} dan dua saudaranya
 * {@code ambilPesanGagalVerifikasiSebelumUjian}/{@code …SebelumInterview} — <b>gerbang bisnis</b>
 * yang memblokir cetak kartu ujian, ikut ujian, dan wawancara bila ada berkas wajib yang belum
 * ber-{@code verified = true}.</li>
 * <li>{@code ais.common.VerifikasiPSBHtmlHelper} — jalur JSP (bukan ZK) untuk halaman
 * {@code /WEB-INF/baru/modul/ppdb/_sukses_login.jsp}. Lihat peringatan keamanan di bawah.</li>
 * </ol>
 *
 * <h2>Pembuatan otomatis — baris "hantu" sebagai efek samping operasi baca</h2>
 * <p>Seluruh <b>enam</b> pembaca di atas memakai pola yang sama: cari baris pasangan; bila tidak
 * ada, <b>buat baru lalu simpan seketika</b> ({@code Common.refreshSaveOrUpdate(...)} atau
 * {@code session.save(...)} dengan transaksi eksplisit). Artinya operasi yang secara semantik hanya
 * <i>membaca</i> — membuka panel, mencetak biodata, bahkan memeriksa kelayakan ujian — <b>menulis
 * ke basis data</b> dan menghasilkan revisi Envers. Konsekuensi praktis:</p>
 * <ul>
 * <li>Tabel ini tumbuh sebesar (jumlah calon siswa × jumlah berkas wajib aktif) tanpa ada satu pun
 * tindakan pengguna yang eksplisit.</li>
 * <li>Tidak ada <i>unique constraint</i> pada pasangan {@code (calon_siswa,
 * verifikasi_kelengkapan_calon_siswa)}; semua pencarian memakai {@code setMaxResults(1)}, yang
 * berarti duplikat <b>tidak akan pernah terdeteksi</b> — hanya baris pertama yang terpakai, dan
 * berkas yang tertaut ke baris duplikat lainnya menjadi yatim secara senyap. Race dua tab/dua
 * petugas yang membuka panel calon siswa yang sama pada saat bersamaan cukup untuk memicunya.</li>
 * <li>Menonaktifkan lalu mengaktifkan kembali satu butir master tidak membuat baris baru (baris
 * lama masih ditemukan), jadi status verifikasi lama "hidup kembali" apa adanya.</li>
 * </ul>
 *
 * <h2>Broken access control &amp; akses berkas — HASIL VERIFIKASI</h2>
 *
 * <h3>1. Layar pengelola: nol {@code checkPrevilages} (instance ke-5 keluarga PSB)</h3>
 * <p>{@code VerifikasiPSBHelper} <b>tidak memuat satu pun panggilan
 * {@code CommonPrivilages.checkPrevilages(...)}</b> (terverifikasi: nol kemunculan di seluruh
 * berkas). Satu-satunya gerbang adalah tiga pemeriksaan <i>peran kasar</i>:
 * {@code tbmuser != null &amp;&amp; tbmuser.getCalonSiswa() == null &amp;&amp; tbmuser.getSiswa() == null}
 * — yaitu "ada yang login, dan dia bukan calon siswa/siswa". Siapa pun yang lolos itu memperoleh
 * checkbox <b>Sesuai</b> dan textbox <b>Keterangan</b> yang aktif. Akibatnya pengguna yang
 * <b>hanya berhak READ</b> pada menu Calon Siswa dapat:</p>
 * <ul>
 * <li><b>mencentang/membatalkan {@code verified}</b> — keputusan yang menentukan apakah seorang
 * anak boleh mencetak kartu ujian, ikut ujian, dan ikut wawancara;</li>
 * <li>mengubah {@code keterangan} (alasan penolakan berkas);</li>
 * <li>mengunggah/mengganti berkas selama baris belum berstatus terverifikasi;</li>
 * </ul>
 * <p>dan semuanya <b>langsung tersimpan</b> lewat {@code Common.refreshUpdate(...)} /
 * {@code Common.refreshSaveOrUpdate(...)} di dalam listener {@code onClick}/{@code onChange},
 * tanpa menunggu tombol "Simpan" formulir induk.</p>
 * <p>Kontrasnya tajam dan berulang: layar <b>master</b>-nya
 * ({@code VerifikasiKelengkapanCalonSiswaAction}) menghitung {@code CREATE}/{@code UPDATE}/
 * {@code DELETE} dengan benar, dan {@code CalonSiswaAction} juga menghitung {@code edit =
 * checkPrevilages(UPDATE)} untuk tombol status kasar — tetapi flag itu <b>tidak pernah
 * diteruskan</b> ke {@code VerifikasiPSBHelper.tampilkanVerifikasi(...)}. Ini pola yang persis
 * sama dengan {@code CalonSiswaPunyaVerifikasiParameter} dan menjadikan rantai verifikasi PSB
 * <b>bocor di kedua sisinya</b> (berkas maupun parameter).</p>
 * <p>Ditambah <i>fail-open</i> cakupan tenant di {@code CalonSiswaAction.initCriteria()} (klausa
 * sekolah/yayasan menjadi {@code Restrictions.sqlRestriction("1=1")} bila combo "Semua" dipilih),
 * hak baca pada satu sekolah cukup untuk menyentuh berkas calon siswa seluruh instalasi.</p>
 *
 * <h3>2. Berkas dapat diunduh TANPA LOGIN (IDOR pra-otentikasi) — PALING SERIUS</h3>
 * <p>Jalur JSP {@code ais.common.VerifikasiPSBHtmlHelper.getDaftarVerifikasi(...)} dipanggil dari
 * {@code /WEB-INF/baru/modul/ppdb/_sukses_login.jsp}. Halaman itu memilih calon siswa dengan
 * urutan berikut:</p>
 * <pre>
 * String idParam = request.getParameter("id");
 * if (idParam != null &amp;&amp; !idParam.trim().isEmpty()) { casisId = Long.parseLong(idParam.trim()); }
 * else if (tbmuser != null &amp;&amp; tbmuser.getCalonSiswa() != null) { casisId = tbmuser.getCalonSiswa().getId(); }
 * </pre>
 * <p><b>Parameter {@code id} dari URL menang atas sesi, dan tidak ada satu pun pemeriksaan
 * kepemilikan maupun login.</b> Halaman ini dijangkau lewat dispatcher {@code /ppdb}
 * ({@code ais.action.servlet.Ppdb} → {@code /WEB-INF/baru/ppdb.jsp}), yang pada cabang
 * {@code hanya_tampil_jsp=true} langsung melakukan {@code &lt;jsp:include&gt;} berkas yang diminta
 * <b>tanpa memeriksa sesi sama sekali</b> — mekanisme yang identik dengan endpoint
 * {@code _wawancara_service} yang sudah dikonfirmasi pra-otentikasi pada batch 50. Sehingga:</p>
 * <pre>
 * /ppdb?hanya_tampil_jsp=true&amp;p=ppdb&amp;s=_sukses_login&amp;id=&lt;angka&gt;
 * </pre>
 * <p>menghasilkan, untuk <b>calon siswa mana pun lintas sekolah/yayasan/instalasi</b>, tanpa
 * kredensial apa pun:</p>
 * <ul>
 * <li>profil calon siswa (nama, NIS bila diterima, kelas, wali kelas, foto);</li>
 * <li>daftar berkas wajib berikut status {@code verified} dan {@code keterangan} dari entity ini;</li>
 * <li>dan yang terparah — <b>tautan unduh langsung ke berkas fisiknya</b>. Halaman merender
 * {@code &lt;a href='" + urlDownload + "' target='_blank'&gt;…Unduh&lt;/a&gt;} dari
 * {@code LampiranLain.createLinkUri()}, dan {@code createLinkUri()} <b>menyalin berkas ke direktori
 * media publik</b> ({@code CommonMedia.getMediaDirectory()}) dengan nama turunan hasil enkripsi id,
 * lalu menyajikannya sebagai berkas statis <b>tanpa pemeriksaan otorisasi apa pun</b>. Nama berkas
 * memang tidak mudah ditebak, tetapi halaman ini <b>menghadiahkannya secara cuma-cuma</b> kepada
 * siapa pun yang mengiterasi {@code id}.</li>
 * </ul>
 * <p>Isi berkas ini adalah <b>PII anak di bawah umur berkategori paling sensitif</b>: akte
 * kelahiran, kartu keluarga, KTP orang tua, ijazah, kartu keluarga sejahtera, dan pada instalasi
 * tertentu surat keterangan rumah sakit untuk anak berkebutuhan khusus. Iterasi {@code id} berurutan
 * memungkinkan <b>pengunduhan massal seluruh basis data dokumen PPDB</b>.</p>
 * <p><b>Sekaligus primitif TULIS pra-otentikasi:</b> {@code getDaftarVerifikasi(...)} memakai pola
 * "buat bila belum ada" yang sama seperti pembaca lain — ia membuka transaksi dan meng-INSERT baris
 * entity ini. Jadi permintaan HTTP anonim di atas juga <b>menulis ke basis data</b> dan mencetak
 * revisi Envers atas nama "tidak ada siapa-siapa".</p>
 * <p>Temuan ini <b>memperkuat</b> {@code task_1f9c66d3} (dispatcher JSP anonim {@code /ppdb}) dan
 * {@code task_4ca32776} (kebocoran PII PSB) yang sudah ada; ia menambahkan endpoint pra-otentikasi
 * konkret ke-2 pada {@code /ppdb} setelah {@code _wawancara_service}, kali ini dengan dampak
 * <b>eksfiltrasi dokumen</b>, bukan sekadar metadata.</p>
 *
 * <h3>3. Verifikasi POSITIF — pengunggah tidak dapat memverifikasi dirinya sendiri</h3>
 * <p>Pada cabang ZK, akun yang tertaut ke {@code CalonSiswa} atau {@code Siswa} dirender read-only
 * (hanya label "Telah Sesuai"/"Belum Diverifikasi"), sehingga pendaftar tidak bisa mencentang
 * berkasnya sendiri. Tidak ada pula tombol mutasi massal pada jalur ini — setiap perubahan berlaku
 * satu baris.</p>
 *
 * <h2>Pola arsitektur repo — hasil pemeriksaan pada file ini</h2>
 * <ul>
 * <li><b>Getter penulis-balik {@code check()}:</b> <b>ADA, dua tempat</b> —
 * {@link #getCalonSiswa()} dan {@link #getVerifikasiKelengkapanCalonSiswa()} menugaskan ulang hasil
 * {@code check(...)} ke fieldnya. Ini <b>memoisasi resolusi proxy lazy</b>, bukan pola destruktif:
 * {@link GeneralValueObject#check(Object)} mengembalikan argumennya apa adanya bila keempat sumber
 * resolusinya gagal, sehingga tidak pernah memusnahkan nilai.</li>
 * <li><b>Getter yang "membalik kontrak" penyimpanan:</b> <b>ADA, dua tempat</b> —
 * {@link #getKeterangan()} mengubah {@code null} menjadi {@code ""} sekaligus mem-{@code trim}, dan
 * {@link #getVerified()} mengubah {@code null} menjadi {@code false}. Karena entity ini memakai
 * <i>property access</i> (anotasi {@code @Id} ada pada getter), nilai <b>hasil getter</b> itulah
 * yang dibaca Hibernate saat INSERT dan dirty-check — bukan isi field. Baris warisan ber-{@code
 * NULL} karena itu akan "sembuh sendiri" menjadi {@code ''}/{@code false} pada flush berikutnya,
 * disertai satu <b>revisi Envers palsu</b> yang tidak berasal dari tindakan pengguna mana pun.</li>
 * <li><b>Getter destruktif yang mengosongkan data (pola {@code KelasSiswaPSB.getNama()}):</b>
 * <b>TIDAK ADA</b>.</li>
 * <li><b>{@code getNomorUrut()} non-null yang meruntuhkan {@code TreeSet} (pola batch 55):</b>
 * <b>TIDAK ADA di entity ini</b> — kelas ini tidak meng-override {@code getNomorUrut()},
 * {@code getNama()}, maupun {@code getNim()}. Perlu dicatat sebagai <i>risiko laten</i>: karena
 * ketiga kunci urut pertama {@link GeneralValueObject#compareTo(GeneralValueObject)} selalu
 * {@code null} di sini, perbandingan selalu jatuh ke kunci terakhir {@code keterangan} — yang
 * berkat {@link #getKeterangan()} tidak pernah {@code null} dan hampir selalu {@code ""}. Dua baris
 * mana pun karenanya {@code compareTo}-nya {@code 0}. Untungnya <b>tidak satu pun pemanggil
 * memasukkan entity ini ke {@code TreeSet}/{@code TreeMap}</b> (yang di-{@code TreeSet}-kan adalah
 * master {@link VerifikasiKelengkapanCalonSiswa}), jadi bug penciutan tidak terpicu. Jangan pernah
 * menambahkannya. Pada sisi master, penciutan <i>bisa</i> terjadi tetapi hanya bila dua butir
 * berkas wajib diberi {@code nama} yang persis sama.</li>
 * <li><b>Filter tenant di dalam entity:</b> <b>TIDAK ADA</b> — dan memang tidak diharapkan ada;
 * penyaringan sepenuhnya tanggung jawab pemanggil (lihat catatan fail-open di atas).</li>
 * <li><b>Pewarisan hak lewat menu induk:</b> <b>TIDAK ADA</b> dalam bentuk klasiknya — panel ini
 * bukan layar {@code .zul} tersendiri melainkan grid yang disisipkan langsung ke formulir Calon
 * Siswa/PPDB, sehingga tidak ada entri menu untuk diwarisi. Efeknya justru lebih buruk: tidak ada
 * hak yang perlu diwarisi karena <b>tidak ada gerbang hak sama sekali</b>.</li>
 * </ul>
 *
 * <h2>Kuirk dan catatan lain</h2>
 * <ul>
 * <li><b>{@code serialVersionUID} kembar.</b> Nilai {@code 2463821577548439808L} pada kelas ini
 * <b>identik</b> dengan milik {@link VerifikasiKelengkapanCalonSiswa}. Berbeda dari temuan klon
 * batch 53, di sini <b>bukan</b> pertanda entity klon: kedua kelas memetakan tabel yang berbeda dan
 * memang berpasangan master–transaksi. Ini murni artefak salin-tempel dan tidak berbahaya, tetapi
 * membuat pencarian berbasis {@code serialVersionUID} menyesatkan.</li>
 * <li><b>Kolom join {@code nullable = true} untuk dua-duanya.</b> Baris yatim (tanpa calon siswa
 * atau tanpa butir master) secara skema sah, dan memang <i>dibuat sengaja</i> untuk sesaat pada
 * alur pendaftaran baru (lihat "Penautan tertunda" di {@code VerifikasiPSBHelper}). Karena itu
 * pembaca yang memanggil {@code getVerifikasiKelengkapanCalonSiswa().getNama()} tanpa penjaga null
 * — mis. {@code CommonReportPsb} — berpotensi {@code NullPointerException} bila baris yatim
 * sempat tersimpan (proses pendaftaran ditinggalkan sebelum selesai).</li>
 * <li><b>Method {@code VerifikasiPSBHelper.tampilkanGrid(...)} yang mengelola entity ini punya
 * cabang penulisan sendiri yang berbeda</b> (memakai {@code session.save(...)} dengan transaksi
 * eksplisit, bukan {@code Common.refreshSaveOrUpdate}); satu-satunya pemanggilnya adalah
 * {@code TampilanPengumumanAkademisAction} — layar pengumuman hasil seleksi. Cabang itu juga
 * merender checkbox verifikasi bila {@code readonly == false}.</li>
 * <li><b>Tidak ada {@code @PrePersist}.</b> Atribusi {@code oleh}/{@code olehId} baru terisi pada
 * {@code UPDATE} pertama. Karena baris dibuat otomatis oleh mesin (bukan oleh manusia), justru itu
 * yang benar secara semantik — pencatatan "siapa yang memverifikasi" memang baru bermakna saat
 * checkbox ditekan.</li>
 * </ul>
 *
 * @see VerifikasiKelengkapanCalonSiswa master daftar berkas wajib per gelombang
 * @see CalonSiswa pemilik berkas
 * @see GelombangPendaftaranPsb sumber daftar berkas wajib
 * @see CalonSiswaPunyaVerifikasiParameter saudara yang sering tertukar (verifikasi prestasi)
 * @see ais.database.model.GeneralValueObject induk POJO (bukan {@code @MappedSuperclass})
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "calon_siswa_punya_verifikasi_berkas")
public class CalonSiswaPunyaVerifikasiBerkas extends GeneralValueObject {

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Nilainya <b>identik</b> dengan {@link VerifikasiKelengkapanCalonSiswa} — artefak
	 * salin-tempel, bukan penanda klon (lihat catatan pada Javadoc kelas). Tidak dipakai untuk
	 * apa pun selain kontrak {@link java.io.Serializable}.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama; nilainya dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()} lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, jadi tidak perlu (dan tidak boleh)
	 * disetel dari kode layar.</p>
	 *
	 * <p><b>Tidak dipetakan Hibernate sebagai kolom sendiri</b> dalam arti dideklarasikan ulang di
	 * kelas ini: {@link GeneralValueObject} adalah POJO abstrak biasa, <b>bukan</b> {@code @Entity}
	 * maupun {@code @MappedSuperclass}, sehingga properti induknya tidak diwarisi ke pemetaan.
	 * Pengulangan deklarasi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code id} di sini
	 * adalah <b>keharusan teknis</b>, bukan duplikasi yang keliru.</p>
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Setter penjaga:</b> argumen {@code null}, kosong, atau berisi spasi saja
	 * <b>diabaikan diam-diam</b> — nilai lama dipertahankan. Pola ini dipakai konsisten di seluruh
	 * entity repo agar atribusi audit tidak pernah terhapus oleh binding formulir yang mengirim
	 * string kosong.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Setter penjaga</b> dengan semantik yang sama seperti {@link #setOlehId(String)}:
	 * {@code null}/kosong/hanya spasi diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Untuk baris entity ini nilainya berarti <b>"siapa yang terakhir menyentuh status
	 * verifikasi berkas"</b> — informasi yang penting secara akuntabilitas, karena {@code verified}
	 * menentukan kelayakan seorang calon siswa mengikuti ujian dan wawancara.</p>
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila baris belum pernah di-{@code UPDATE}
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} beserta deklarasi field {@code tanggal_dirubah} (keduanya berbagi
	 * satu baris sumber; gaya asli berkas dipertahankan apa adanya).
	 *
	 * <p><b>{@code onUpdate()}</b> dipanggil Hibernate tepat sebelum pernyataan {@code UPDATE}
	 * dieksekusi, dan mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)} dan
	 * {@link #setTanggal_dirubah(Date)} dari pengguna yang sedang login.</p>
	 *
	 * <p><b>Hanya berlaku untuk UPDATE.</b> Tidak ada pasangan {@code @PrePersist}, sehingga pada
	 * INSERT pertama {@code oleh}/{@code olehId} tetap {@code null} dan {@code tanggal_dirubah}
	 * mengandalkan nilai awal field ({@code ais.ui.util.WaktuUtil.getDate()}, dievaluasi saat
	 * object dikonstruksi). Karena baris entity ini <b>dibuat oleh mesin</b> (lihat "Pembuatan
	 * otomatis" pada Javadoc kelas), baris baru memang tidak beratribusi siapa pun sampai ada
	 * petugas yang benar-benar mencentang/mengubah keterangannya.</p>
	 *
	 * <p><b>Interaksi dengan getter penormal.</b> {@link #getKeterangan()} dan
	 * {@link #getVerified()} mengubah {@code null} menjadi {@code ""}/{@code false}. Pada baris
	 * warisan ber-{@code NULL}, dirty-check Hibernate karena itu melihat perubahan dan memicu
	 * {@code UPDATE} — dan dengan demikian hook ini — pada operasi yang secara semantik hanya
	 * membaca (mis. sekadar membuka panel verifikasi). Revisi Envers yang timbul <b>tidak
	 * mencerminkan tindakan pengguna mana pun</b>.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi maupun penjaga {@code null}.
	 *
	 * <p>Normalnya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan
	 * dari kode layar.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Nilai awalnya adalah waktu konstruksi object, bukan
	 * waktu INSERT — perbedaan yang biasanya tidak terasa karena baris ini disimpan segera setelah
	 * dibuat.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object hasil konstruksi
	 *         normal
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Calon siswa pemilik berkas. Lihat {@link #getCalonSiswa()}. */
	private CalonSiswa calonSiswa;
	/** Butir master "berkas apa" yang diwakili baris ini. Lihat {@link #getVerifikasiKelengkapanCalonSiswa()}. */
	private VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa;
	/** Status verifikasi berkas oleh panitia. Lihat {@link #getVerified()}. */
	private Boolean verified;
	/** Catatan petugas atas berkas ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Object hasil konstruksi belum lengkap: pemanggil <b>wajib</b> mengisi
	 * {@link #setCalonSiswa(CalonSiswa)} dan
	 * {@link #setVerifikasiKelengkapanCalonSiswa(VerifikasiKelengkapanCalonSiswa)} sebelum
	 * menyimpan, karena pasangan itulah yang menjadi kunci logis baris.</p>
	 */
	public CalonSiswaPunyaVerifikasiBerkas() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p><b>Perannya melampaui sekadar kunci basis data:</b> nilai ini dipakai sebagai
	 * {@code lampiran_lain.ref} untuk menemukan berkas fisik yang terunggah (lihat Javadoc kelas).
	 * Pada alur pendaftaran baru id masih {@code null} saat berkas diunggah, sehingga
	 * {@code VerifikasiPSBHelper} memakai "acuan sementara" ({@code Common.refSementara()}) dan
	 * menautkannya belakangan lewat {@code tautkanLampiranTertunda(...)} setelah id terbit.</p>
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}); kolomnya {@code insertable = false} sehingga
	 * nilai yang disetel manual lewat {@link #setId(Long)} tidak akan ikut dalam pernyataan
	 * {@code INSERT}.</p>
	 *
	 * @return id baris; {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Tanpa validasi.
	 *
	 * <p>Praktis hanya dipakai Hibernate setelah {@code INSERT}; kode aplikasi tidak perlu
	 * memanggilnya.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan calon siswa pemilik berkas ini.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link GeneralValueObject#check(Object)} dan
	 * <b>menugaskan ulang hasilnya ke field</b>. Ini memoisasi resolusi proxy lazy (kolomnya
	 * {@code FetchType.LAZY}) agar entity tetap dapat dibaca setelah sesi Hibernate-nya ditutup —
	 * pola yang sangat sering dipakai berkas ini karena {@code VerifikasiPSBHelper} membuka dan
	 * menutup sesi berkali-kali dalam satu render. Bukan pola destruktif: {@code check()}
	 * mengembalikan argumennya apa adanya bila resolusi gagal.</p>
	 *
	 * <p>Kolom {@code calon_siswa} sengaja {@code nullable = true} karena pada alur pendaftaran
	 * baru baris ini sempat dibentuk sebelum calon siswanya punya id; {@code simpanVerifikasi()}
	 * mengisinya setelah calon siswa tersimpan.</p>
	 *
	 * @return calon siswa pemilik; {@code null} pada baris yatim (pendaftaran yang belum selesai
	 *         atau data impor)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel calon siswa pemilik berkas. Tanpa validasi maupun penjaga {@code null}.
	 *
	 * <p>Dipanggil dari <b>enam</b> titik yang membuat baris otomatis
	 * ({@code VerifikasiPSBHelper} dua kali, {@code CalonSiswaAction} dua kali,
	 * {@code CommonReportPsb}, {@code VerifikasiKelengkapanCalonSiswa.checkBerkas},
	 * {@code VerifikasiPSBHtmlHelper}), serta dari {@code simpanVerifikasi()} untuk mengisi acuan
	 * yang tertunda pada alur pendaftaran baru.</p>
	 *
	 * <p><b>Peringatan:</b> tidak ada validasi bahwa calon siswa berada pada gelombang yang sama
	 * dengan butir master di {@link #getVerifikasiKelengkapanCalonSiswa()}. Konsistensi itu hanya
	 * dijaga oleh alur pemanggil.</p>
	 *
	 * @param calonSiswa calon siswa pemilik berkas
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan butir master yang menyatakan <b>berkas apa</b> yang diwakili baris ini
	 * (mis. "Akte Kelahiran", "Kartu Keluarga", "Ijazah").
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getCalonSiswa()} — memoisasi hasil
	 * {@link GeneralValueObject#check(Object)} ke field untuk meresolusi proxy lazy.</p>
	 *
	 * <p>Nama yang ditampilkan ke pengguna diambil dari
	 * {@code getVerifikasiKelengkapanCalonSiswa().getNama()}. Beberapa pemanggil (mis.
	 * {@code CommonReportPsb}) memanggil rantai itu <b>tanpa penjaga null</b> padahal kolom
	 * joinnya {@code nullable = true}; satu baris yatim cukup untuk melempar
	 * {@code NullPointerException} dan merusak cetakan.</p>
	 *
	 * @return butir master berkas wajib; {@code null} pada baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "verifikasi_kelengkapan_calon_siswa", nullable = true)
	public VerifikasiKelengkapanCalonSiswa getVerifikasiKelengkapanCalonSiswa() {
		verifikasiKelengkapanCalonSiswa = check(verifikasiKelengkapanCalonSiswa);
		return verifikasiKelengkapanCalonSiswa;
	}

	/**
	 * Menyetel butir master berkas wajib yang diwakili baris ini. Tanpa validasi.
	 *
	 * <p>Bersama {@link #setCalonSiswa(CalonSiswa)} membentuk kunci logis baris. Karena
	 * <b>tidak ada unique constraint</b> pada pasangan itu dan semua pencarian memakai
	 * {@code setMaxResults(1)}, duplikat yang terlanjur tercipta tidak akan pernah terdeteksi —
	 * berkas yang tertaut ke baris duplikat menjadi yatim secara senyap.</p>
	 *
	 * @param verifikasiKelengkapanCalonSiswa butir master berkas wajib
	 */
	public void setVerifikasiKelengkapanCalonSiswa(VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa) {
		this.verifikasiKelengkapanCalonSiswa = verifikasiKelengkapanCalonSiswa;
	}

	/**
	 * Mengembalikan catatan petugas atas berkas ini — biasanya alasan mengapa sebuah berkas
	 * dianggap belum sesuai ("scan buram", "bukan akte asli", …).
	 *
	 * <p><b>Getter penormal:</b> {@code null} dikembalikan sebagai {@code ""} dan nilainya
	 * di-{@code trim}. Karena entity ini memakai <i>property access</i>, nilai <b>hasil getter</b>
	 * inilah yang dibaca Hibernate saat INSERT dan dirty-check — sehingga baris warisan ber-{@code
	 * NULL} akan "sembuh sendiri" menjadi {@code ''} pada flush berikutnya, disertai revisi Envers
	 * yang tidak berasal dari tindakan pengguna. Efeknya juga terasa pada
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}: karena tidak pernah {@code null},
	 * cabang {@code keterangan} <b>selalu</b> menjadi kunci urut yang terpakai untuk entity ini.</p>
	 *
	 * <p>Pada UI, textbox keterangan <b>dinonaktifkan begitu checkbox "Sesuai" dicentang</b> —
	 * catatan hanya relevan untuk berkas yang ditolak.</p>
	 *
	 * @return catatan petugas yang sudah di-{@code trim}; {@code ""} bila kosong. Tidak pernah
	 *         {@code null}.
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menyetel catatan petugas atas berkas ini. Tanpa validasi maupun {@code trim}
	 * (pemangkasan dilakukan {@link #getKeterangan()} saat pembacaan).
	 *
	 * <p>Dipanggil dari listener {@code onChange} textbox keterangan, yang <b>langsung menyimpan
	 * seketika</b> lewat {@code Common.refreshUpdate(...)}/{@code Common.refreshSaveOrUpdate(...)}
	 * tanpa menunggu tombol "Simpan" formulir induk — dan <b>tanpa pemeriksaan hak akses apa pun</b>
	 * (lihat bagian broken access control pada Javadoc kelas).</p>
	 *
	 * @param keterangan catatan petugas; {@code null} diperbolehkan dan akan dibaca sebagai
	 *                   {@code ""}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status verifikasi berkas ini oleh panitia PSB.
	 *
	 * <p><b>Ini field paling berdampak di seluruh entity.</b> Nilainya bukan sekadar penanda
	 * tampilan — ia menjadi <b>gerbang bisnis</b> pada setidaknya tiga tempat:</p>
	 * <ul>
	 * <li>{@link VerifikasiKelengkapanCalonSiswa#checkBerkas(CalonSiswa)} — memblokir
	 * <b>cetak kartu ujian</b> bila gelombangnya menyalakan
	 * {@code cetakKartuUjianHarusVerifikasiBerkas};</li>
	 * <li>{@code VerifikasiKelengkapanCalonSiswa.ambilPesanGagalVerifikasiSebelumUjian(...)} —
	 * memblokir <b>ikut ujian</b> untuk butir master ber-{@code wajibVerifikasiSebelumUjian};</li>
	 * <li>{@code …ambilPesanGagalVerifikasiSebelumInterview(...)} — memblokir <b>wawancara</b>
	 * untuk butir ber-{@code wajibVerifikasiSebelumInterview}.</li>
	 * </ul>
	 * <p>Selain itu ia mengendalikan apakah tombol unggah berkas masih aktif (berkas yang sudah
	 * diverifikasi dikunci), dan tampil di cetakan biodata sebagai "Telah sesuai" /
	 * "Belum Diverifikasi".</p>
	 *
	 * <p><b>Getter penormal:</b> {@code null} dikembalikan sebagai {@code false} ("belum
	 * diverifikasi"), sehingga <i>fail-closed</i> — arah yang benar untuk sebuah gerbang. Sama
	 * seperti {@link #getKeterangan()}, normalisasi ini ikut tertulis kembali ke basis data pada
	 * flush berikutnya karena entity memakai <i>property access</i>.</p>
	 *
	 * @return {@code true} bila berkas sudah dinyatakan sesuai oleh petugas; {@code false} bila
	 *         belum atau bila kolomnya {@code NULL}. Tidak pernah {@code null}.
	 */
	public Boolean getVerified() {
		return verified == null ? false : verified;
	}

	/**
	 * Menyetel status verifikasi berkas. Tanpa validasi maupun pemeriksaan hak akses.
	 *
	 * <p><b>Titik panggil satu-satunya adalah listener {@code onClick} checkbox "Sesuai" di
	 * {@code VerifikasiPSBHelper}</b> (dan salinan nilainya di {@code simpanVerifikasi()}), yang
	 * menyimpan perubahan <b>seketika</b>. Karena helper itu <b>nol
	 * {@code CommonPrivilages.checkPrevilages(...)}</b>, setiap pengguna yang dapat membuka layar
	 * detail calon siswa — termasuk yang hanya berhak READ — dapat meluluskan atau menggagalkan
	 * verifikasi berkas calon siswa mana pun. Perubahannya tetap terekam Envers
	 * ({@code @Audited}) dan beratribusi benar lewat {@link #onUpdate()}, sehingga ini murni cacat
	 * <b>otorisasi</b>, bukan cacat auditabilitas.</p>
	 *
	 * <p>Sisi baiknya: akun yang tertaut ke {@code CalonSiswa}/{@code Siswa} dirender read-only,
	 * jadi pendaftar tidak dapat memverifikasi berkasnya sendiri.</p>
	 *
	 * @param verified status verifikasi baru; {@code null} diperbolehkan dan akan dibaca sebagai
	 *                 {@code false}
	 */
	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

}
