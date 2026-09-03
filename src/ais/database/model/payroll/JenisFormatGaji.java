package ais.database.model.payroll;

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
import ais.database.model.rab.SatuanKerja;

/**
 * Katalog <b>varian cetak</b> untuk laporan rekap pembayaran gaji &mdash; satu baris tabel
 * {@code payroll.jenis_format_gaji} adalah satu "Format Laporan" yang dapat dipilih operator
 * sebelum mencetak rekap penggajian.
 *
 * <h2>Apa yang entity ini BUKAN (verifikasi eksplisit)</h2>
 * <p>Nama kelas mengesankan bahwa ini adalah katalog <i>jenis</i> dari
 * {@code ais.database.model.payroll.FormatItemGaji} (entity jangkar tenant rantai penggajian).
 * <b>Hipotesis itu SALAH dan sudah diverifikasi dari kode.</b> Fakta yang bisa diperiksa:</p>
 * <ul>
 *   <li>{@code FormatItemGaji} <b>tidak memiliki satu pun</b> referensi ke kelas ini &mdash; tidak
 *   ada field {@code jenisFormatGaji}, tidak ada import, tidak ada penyebutan di Javadoc-nya.</li>
 *   <li>Kelas ini juga <b>tidak memiliki</b> field bertipe {@code FormatItemGaji} maupun
 *   {@code ItemGaji}. Tidak ada FK di kedua arah, dan tidak ada tabel jembatan di antaranya.</li>
 *   <li>Kedua entity hanya "bertemu" secara kebetulan pada satu berkas laporan
 *   ({@code LaporanRekapPembayaranBerdasarkanFormatNilai}) yang membaca keduanya untuk keperluan
 *   yang berbeda: {@code FormatItemGaji} lewat {@code Pegawai.ambilBank(...)} untuk menentukan
 *   rekening, sedangkan kelas ini untuk memilih berkas layout cetak dan mode pengelompokan.</li>
 * </ul>
 * <p>Kemiripan yang ada murni kosmetik: keduanya {@code extends GeneralValueObject}, keduanya
 * memakai {@code satuanKerja} nullable sebagai sumbu tenant, dan keduanya berbagi
 * {@code serialVersionUID} yang sama persis ({@code 2463821577548439808L}) &mdash; jejak khas
 * hbm2java/salin-tempel, bukan tanda kekerabatan domain. Perbedaan isi sangat tajam:
 * {@code FormatItemGaji} membawa dimensi kepegawaian ({@code cabang}, {@code departemen},
 * {@code levelJabatan}) dan menjadi jangkar bagi rencana/realisasi gaji, sedangkan kelas ini
 * hanya membawa tujuh bendera penyajian laporan dan tidak pernah menjadi induk data transaksi
 * apa pun.</p>
 *
 * <h2>Peran sesungguhnya</h2>
 * <p>Baris katalog ini adalah pasangan dari sebuah <b>berkas template JasperReports</b>
 * ({@code .jrxml}/{@code .jasper}) yang diunggah operator. Template itu <i>tidak</i> disimpan di
 * kolom kelas ini; ia dititipkan ke {@code ais.database.model.file.LampiranLain} dengan
 * {@code ref} = {@link #getId()} dan {@code jenis} =
 * {@code LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORMAT_GAJI}. Layar master juga mengizinkan
 * lampiran gambar/latar pendukung dengan pola {@code jenis} berawalan
 * {@code "Jenis_Format_Gaji_"} + barcode acak. Karena itu baris tanpa lampiran jrxml akan ditolak
 * saat dicetak dengan pesan "File laporan format gaji belum diupload".</p>
 * <p>Selain menunjuk berkas layout, baris ini menentukan <b>mode agregasi</b> yang dipakai mesin
 * laporan sebelum data diserahkan ke Jasper. Tujuh bendera boolean di kelas ini dibaca
 * {@code LaporanRekapPembayaranBerdasarkanFormatNilai} sebagai rantai
 * {@code if / else if} sehingga <b>saling meniadakan</b> walau UI mengizinkan semuanya dicentang
 * sekaligus. Urutan presedensinya: {@link #getPerKeluarga()} &rarr; {@link #getPerBank()} &rarr;
 * gabungan {@link #getPerSatker()}/{@link #getPerSatkerFakultas()}/
 * {@link #getPerSatkerJurusan()}/{@link #getPerSatkerSekolah()}. Bendera terakhir yang berdiri
 * sendiri, {@link #getQueryManual()}, bekerja pada tingkat yang berbeda: ketika bernilai
 * {@code true} seluruh pipeline pengambilan dan agregasi data bawaan <b>dilewati</b> dan template
 * jrxml diasumsikan membawa {@code queryString} sendiri, hanya menerima parameter
 * {@code bulan}/{@code tahun}/{@code satuan_kerja_id}/{@code caraPembayaranGaji}.</p>
 *
 * <h2>Permukaan pemanggil (grep menyeluruh)</h2>
 * <p>Hanya ada dua konsumen Java di seluruh repo, ditambah satu layar ZK dan sepasang scaffold
 * New UI:</p>
 * <ul>
 *   <li>{@code ais.action.master.payroll.JenisFormatGajiAction} &mdash; CRUD master ZK
 *   (layar {@code z/x/y/pages/master/payroll/jenis_format_nilai.zul}; perhatikan nama berkas
 *   memakai "nilai", bukan "gaji"). Menyediakan pencarian, grid dengan tujuh checkbox in-line,
 *   unggah lampiran, cetak daftar, serta impor Excel lewat {@code Common.uploadData(...)}.</li>
 *   <li>{@code ais.action.report.format1.payroll.LaporanRekapPembayaranBerdasarkanFormatNilai}
 *   &mdash; satu-satunya pembaca bendera; mengisi combo "Format Laporan *" lewat
 *   {@code Common.insertCombo(..., JenisFormatGaji.class, ...)} yang menyaring
 *   {@code aktif = true}.</li>
 *   <li>{@code WEB-INF/new/payroll/uiux/jenis_format_gaji.jsp} dan
 *   {@code .../services/jenis_format_gaji_service.jsp} &mdash; scaffold New UI yang mendaftarkan
 *   {@code nuiServiceEntities = {"JenisFormatGaji", "LampiranLain"}}, sehingga
 *   {@code dispatcher.jsp} meng-auto-register entity ini ke Generic CRUD v2.</li>
 * </ul>
 *
 * <h2>Cakupan tenant &mdash; dua sumbu yang berbeda</h2>
 * <p>{@link #getSatuanKerja()} boleh {@code null}; baris ber-{@code null} berarti "format
 * bersama/global" yang tampil untuk semua unit kerja. Jalur ZK menegakkan hal itu dengan
 * {@code satuanKerja IS NULL OR satuanKerja IN (unit milik pengguna)}. <b>Namun penyaring itu
 * fail-open</b>: bila himpunan unit kerja pengguna kosong
 * ({@code SekolahUtil.ambilSatuanKerjas()} mengembalikan set kosong) restriksi berubah menjadi
 * {@code Restrictions.sqlRestriction("1=1")} &mdash; seluruh baris SEMUA tenant tampil dan bisa
 * disunting. Pola persis yang sama dipakai pada combo laporan <i>dan</i> pada query data
 * penggajiannya sendiri, jadi kondisi fail-open yang sama juga membocorkan rekap gaji seluruh
 * tenant, bukan hanya daftar katalog ini.</p>
 * <p>Pada Generic CRUD v2 perilakunya justru terbalik dan lebih ketat:
 * {@code GenericCrudAutoEntityAdapter.scopeBindings()} memuat {@code "satuanKerja"} di
 * whitelist-nya (sama seperti {@code FormatItemGaji}), sehingga {@code applyScope()} memasang
 * {@code Restrictions.eq("satuanKerja", unitPengguna)}. Karena {@code eq} tidak pernah cocok
 * dengan {@code NULL}, konsekuensinya: <b>seluruh format global tak terlihat dan tak bisa
 * disunting dari New UI oleh pengguna non-admin</b>, dan {@code validateObjectScope()} akan
 * menolaknya dengan {@code OBJECT_OUTSIDE_SCOPE}. Ini bukan kebocoran, melainkan celah
 * fungsional: dua permukaan CRUD atas tabel yang sama memperlihatkan himpunan baris yang
 * berbeda.</p>
 *
 * <h2>Hal non-obvious lain</h2>
 * <ul>
 *   <li>Kelas ini {@code extends} {@link GeneralValueObject}, yang <b>bukan</b> {@code @Entity}
 *   maupun {@code @MappedSuperclass} &mdash; hanya POJO abstrak. Hibernate tidak memetakan
 *   properti induknya, sehingga deklarasi ulang {@code id}, {@code kode}, {@code nama},
 *   {@code keterangan}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di sini adalah
 *   <b>keharusan teknis</b>, bukan bug atau duplikasi yang perlu "dibersihkan".</li>
 *   <li>Hanya {@code id}, {@code nama}, {@code keterangan}, dan {@code satuan_kerja} yang punya
 *   anotasi pemetaan eksplisit. {@code kode}, {@code aktif}, dan ketujuh bendera mengandalkan
 *   penamaan kolom bawaan Hibernate (tanpa {@code NamingStrategy} khusus di
 *   {@code hibernate.cfg.xml}), jadi nama kolomnya mengikuti nama properti apa adanya.</li>
 *   <li>{@link #toString()} di-override menjadi {@code id + "-" + nama}, berbeda dari format
 *   {@code "kode - nama"} milik {@link GeneralValueObject}. Nilai ini yang muncul pada log,
 *   dropdown ZK, dan jejak Envers.</li>
 *   <li>Entity {@code @Audited} (Envers) dengan {@code dynamicInsert}/{@code dynamicUpdate},
 *   sehingga setiap perubahan bendera dari grid tercatat di tabel revisi. Layar master
 *   menampilkannya lewat {@code RevisiHelper.createNewRevisi(...)}.</li>
 *   <li>Checkbox filter {@code searchaktif} ("Tampilkan hanya yang aktif") ada di ZUL dan
 *   default tercentang, tetapi {@code initCriteria()} <b>tidak pernah membacanya</b> &mdash;
 *   baris non-aktif tetap muncul di layar master. Laporan tetap aman karena combo-nya menyaring
 *   {@code aktif = true} sendiri.</li>
 *   <li><b>Bug salin-tempel di layar master</b> (bukan di kelas ini): listener {@code onCheck}
 *   untuk checkbox "Query Manual" memanggil {@code setPerSatkerSekolah(...)}, bukan
 *   {@link #setQueryManual(Boolean)}. Akibatnya {@link #getQueryManual()} tidak pernah dapat
 *   diaktifkan dari layar (hanya lewat impor Excel), sementara mencentangnya diam-diam mengubah
 *   mode pengelompokan menjadi per-sekolah.</li>
 *   <li>Ketujuh checkbox grid hanya digerbangi lewat {@code setDisabled(!edit)} berdasarkan
 *   {@code CommonPrivilages.UPDATE}; listener {@code onCheck} yang menulis ke database tidak
 *   memeriksa ulang hak akses di sisi server. Ini varian yang lebih ringan dari pola
 *   "checkbox grid tanpa gerbang" &mdash; gerbangnya ada, tetapi hanya di lapisan render.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #setId(Long)}, {@link #getOleh()},
 *   {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, {@code onUpdate()},
 *   {@link #toString()}.</li>
 *   <li><b>Deskripsi katalog:</b> {@link #getKode()}, {@link #setKode(String)},
 *   {@link #getNama()}, {@link #setNama(String)}, {@link #getKeterangan()},
 *   {@link #setKeterangan(String)}, {@link #getAktif()}, {@link #setAktif(Boolean)}.</li>
 *   <li><b>Cakupan tenant:</b> {@link #getSatuanKerja()}, {@link #setSatuanKerja(SatuanKerja)}.</li>
 *   <li><b>Bendera mode penyajian laporan:</b> {@link #getPerKeluarga()},
 *   {@link #setPerKeluarga(Boolean)}, {@link #getPerBank()}, {@link #setPerBank(Boolean)},
 *   {@link #getPerSatker()}, {@link #setPerSatker(Boolean)}, {@link #getPerSatkerFakultas()},
 *   {@link #setPerSatkerFakultas(Boolean)}, {@link #getPerSatkerJurusan()},
 *   {@link #setPerSatkerJurusan(Boolean)}, {@link #getPerSatkerSekolah()},
 *   {@link #setPerSatkerSekolah(Boolean)}, {@link #getQueryManual()},
 *   {@link #setQueryManual(Boolean)}.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.rab.SatuanKerja
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "jenis_format_gaji")
public class JenisFormatGaji extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code IDENTITY}. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * ID pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor} lewat {@code onUpdate()}, atau oleh
	 * {@code GenericCrudAutoEntityAdapter.applyContextDefaults()} pada jalur New UI.</p>
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi &mdash; nilai lama dipertahankan tanpa pesan kesalahan. Jadi field ini tidak
	 * pernah bisa "dikosongkan kembali" lewat setter.</p>
	 *
	 * @param olehId ID pengguna; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga nama lama tidak pernah terhapus oleh penyimpanan berikutnya.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} agar
	 * {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} terisi otomatis dari
	 * konteks pengguna aktif tepat sebelum baris di-{@code UPDATE}.
	 *
	 * <p>Implementasi wajib dari {@code abstract} milik {@link GeneralValueObject}. Jangan
	 * dipanggil manual &mdash; ia dipicu penyedia persistence, bukan kode aplikasi. Callback ini
	 * <b>tidak</b> berjalan pada {@code INSERT}, sehingga baris baru mengandalkan nilai awal
	 * {@link #tanggal_dirubah} di bawah. Karena setiap centang checkbox di grid master memanggil
	 * {@code Common.refreshSaveOrUpdate(...)}, callback ini praktis berjalan sekali per klik
	 * checkbox.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) agar baris baru tetap punya cap waktu meski
	 * {@code @PreUpdate} belum pernah berjalan; lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis lewat {@code onUpdate()}; setel manual hanya untuk keperluan
	 * impor atau perbaikan data.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return cap waktu perubahan; tidak pernah {@code null} untuk object yang baru dibuat di JVM
	 *         ini karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris katalog dalam format {@code "<id>-<nama>"}.
	 *
	 * <p><b>Non-obvious:</b> berbeda dari {@link GeneralValueObject#toString()} yang memakai
	 * {@code "kode - nama"}. Method ini membaca field {@code nama} secara langsung (tanpa
	 * {@code trim()} milik {@link #getNama()}), sehingga spasi berlebih ikut tercetak. Nilai ini
	 * dipakai pada log, tooltip, dan label revisi Envers.</p>
	 *
	 * @return gabungan id dan nama; kedua bagian bisa berupa teks {@code "null"} untuk object
	 *         yang belum tersimpan atau belum diberi nama
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode singkat katalog. Tidak pernah diisi/ditampilkan layar ZK; hanya dipakai
	 * {@code GenericCrudAutoEntityAdapter.getNaturalKeyProperties()} sebagai kunci alami impor.
	 * Lihat {@link #getKode()}.
	 */
	private String kode;

	/** Nama format laporan; kolom {@code nama} NOT NULL. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas; kolom {@code keterangan} nullable. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Unit kerja pemilik format ini &mdash; satu-satunya sumbu tenant di entity ini. Boleh
	 * {@code null} yang berarti "format bersama/global". Lihat {@link #getSatuanKerja()} dan
	 * pembahasan cakupan tenant pada Javadoc kelas.
	 */
	private SatuanKerja satuanKerja;
	/** Bendera aktif; {@code null} diartikan aktif. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Mode rekap per bank penerima. Lihat {@link #getPerBank()}. */
	private Boolean perBank;
	/** Mode rekap per unit kerja pegawai. Lihat {@link #getPerSatker()}. */
	private Boolean perSatker;
	/** Mode rincian per anggota keluarga (premi asuransi). Lihat {@link #getPerKeluarga()}. */
	private Boolean perKeluarga;
	/** Varian {@code perSatker} yang mengambil unit kerja dari penugasan fakultas. Lihat {@link #getPerSatkerFakultas()}. */
	private Boolean perSatkerFakultas;
	/** Varian {@code perSatker} yang mengambil unit kerja dari penugasan jurusan/prodi. Lihat {@link #getPerSatkerJurusan()}. */
	private Boolean perSatkerJurusan;
	/** Varian {@code perSatker} yang mengambil unit kerja dari penugasan sekolah. Lihat {@link #getPerSatkerSekolah()}. */
	private Boolean perSatkerSekolah;
	/** Bendera "template membawa query sendiri". Lihat {@link #getQueryManual()}. */
	private Boolean queryManual;

	/**
	 * Constructor kosong yang dibutuhkan Hibernate dan dipakai layar master saat menekan
	 * "Tambah" ({@code JenisFormatGajiAction.onAdd()}). Seluruh bendera dibiarkan {@code null};
	 * getter masing-masing yang menentukan nilai efektifnya.
	 */
	public JenisFormatGaji() {
	}

	/**
	 * Kunci utama baris katalog.
	 *
	 * <p>Selain sebagai identitas, nilai ini dipakai sebagai {@code ref} pada
	 * {@code LampiranLain} untuk mengambil berkas template jrxml dan gambar pendukung
	 * ({@code LampiranLain.ambil(id, FILE_JRXML_LAYOUT_JENIS_FORMAT_GAJI)}). Karena itu
	 * memindahkan/menyalin baris tanpa memindahkan lampirannya akan membuat cetak laporan gagal
	 * dengan pesan "File laporan format gaji belum diupload".</p>
	 *
	 * @return ID baris, atau {@code null} untuk object yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate saat memuat/menyimpan; jangan disetel manual dari
	 * kode aplikasi.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode singkat katalog.
	 *
	 * <p><b>Non-obvious:</b> mengembalikan string kosong (bukan {@code null}) bila kolomnya
	 * kosong &mdash; berbeda dari {@link #getNama()} yang tetap mengembalikan {@code null}.
	 * Dalam praktiknya kolom ini selalu kosong karena tidak ada satu pun layar yang mengisinya;
	 * satu-satunya pembaca adalah
	 * {@code GenericCrudAutoEntityAdapter.getNaturalKeyProperties()} yang memakai
	 * {@code kode}+{@code nama} untuk deduplikasi impor.</p>
	 *
	 * @return kode yang sudah di-{@code trim()}, atau string kosong bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode singkat katalog.
	 *
	 * @param kode kode baru; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama format laporan, sebagaimana muncul pada combo "Format Laporan *" di layar cetak dan
	 * pada kolom pertama grid master.
	 *
	 * <p>Kolom {@code text} NOT NULL. Layar master menolak simpan bila nama kosong
	 * ({@code JenisFormatGajiAction.onSave()}), tetapi jalur impor Excel dan Generic CRUD v2
	 * tidak melalui validasi ZK itu sehingga tetap bergantung pada constraint database.</p>
	 *
	 * @return nama yang sudah di-{@code trim()}, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama format laporan.
	 *
	 * @param nama nama baru; nilai kosong akan lolos di sini dan baru ditolak oleh validasi
	 *             layar/constraint database
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas format laporan.
	 *
	 * <p>Selain ditampilkan di grid, teks ini dipakai sebagai <i>deskripsi</i> item combo pada
	 * layar cetak ({@code Common.insertCombo(formatGaji, "nama", "keterangan", ...)}), jadi ia
	 * ikut terlihat operator saat memilih format.</p>
	 *
	 * @return keterangan apa adanya (tanpa {@code trim()}), atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Bendera aktif.
	 *
	 * <p><b>Non-obvious:</b> {@code null} diartikan <b>aktif</b>. Baris warisan yang belum pernah
	 * disentuh layar baru otomatis dianggap aktif dan langsung muncul di combo layar cetak
	 * (yang menyaring {@code Restrictions.eq("aktif", true)} &mdash; catatan: filter itu
	 * membandingkan kolom SQL, sehingga baris ber-{@code NULL} justru <b>tidak</b> lolos di sana
	 * meskipun getter ini mengatakan aktif; perbedaan ini nyata dan mudah membingungkan).</p>
	 * <p>Kolom ini juga menjadi dasar <i>soft delete</i> Generic CRUD v2:
	 * {@code GenericCrudAutoEntityAdapter.delete()} hanya menyetel {@code aktif = false}.</p>
	 *
	 * @return {@code true} bila aktif atau bila kolomnya {@code null}; {@code false} hanya bila
	 *         disetel eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel bendera aktif.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" di grid master, yang langsung
	 * menyimpan lewat {@code Common.refreshSaveOrUpdate(...)} tanpa tombol Simpan. Checkbox itu
	 * hanya di-{@code setDisabled(!edit)} berdasarkan {@code CommonPrivilages.UPDATE}; tidak ada
	 * pemeriksaan hak akses ulang di sisi listener.</p>
	 *
	 * @param aktif nilai baru; {@code null} akan dibaca kembali sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Unit kerja pemilik format laporan ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code satuan_kerja}. Getter memanggil
	 * {@link GeneralValueObject#check(Object)} lalu <b>menulis balik</b> hasilnya ke field.
	 * Write-back ini <b>tidak destruktif</b>: {@code check()} hanya meresolusi proxy lazy yang
	 * sudah detached dan, bila gagal, mengembalikan argumennya apa adanya &mdash; ia tidak pernah
	 * mengubah {@code non-null} menjadi {@code null} dan tidak pernah menunjuk baris lain.
	 * Instance yang dikembalikan bisa berbeda object dari yang tersimpan sebelumnya (kanonik dari
	 * {@code EntityIdentityMap} atau hasil reload), tetapi ID-nya sama. Karena itu getter ini
	 * <b>aman</b> dipanggil dari laporan/render, berbeda dengan pola getter cakupan tenant
	 * destruktif yang ditemukan di beberapa entity keuangan lain.</p>
	 * <p>Nilai {@code null} bermakna "format bersama/global". Konsekuensi penyaringannya berbeda
	 * antara jalur ZK (global terlihat semua orang, dan fail-open bila daftar unit kerja pengguna
	 * kosong) dan jalur Generic CRUD v2 (global justru tidak terlihat sama sekali bagi non-admin
	 * karena pembatasnya memakai {@code eq}); lihat Javadoc kelas.</p>
	 *
	 * @return unit kerja pemilik yang sudah teresolusi, atau {@code null} untuk format global
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja")
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja pemilik format.
	 *
	 * <p>Diisi dari bandbox "Satuan Kerja" pada dialog tambah/ubah
	 * ({@code JenisFormatGajiAction.onSave()}). Nilai yang dipilih diambil apa adanya dari
	 * atribut komponen tanpa verifikasi bahwa unit tersebut termasuk cakupan pengguna, sehingga
	 * pemindahan format ke tenant lain bergantung sepenuhnya pada isi bandbox. Properti ini juga
	 * termasuk daftar kolom yang bisa ditulis lewat impor Excel
	 * ({@code Common.uploadData(this, JenisFormatGaji.class, contents)}).</p>
	 *
	 * @param satuanKerja unit kerja pemilik; {@code null} menjadikan format ini global
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mode rekap <b>per unit kerja</b>: mesin laporan menambahkan baris subtotal setiap kali unit
	 * kerja pegawai berganti, memakai {@code Pegawai.getSatuanKerja()} sebagai kunci grup.
	 *
	 * <p>Dievaluasi pada cabang ketiga rantai {@code if/else if} laporan, yaitu hanya bila
	 * {@link #getPerKeluarga()} dan {@link #getPerBank()} keduanya {@code false}. Bendera ini
	 * berbagi cabang yang sama dengan {@link #getPerSatkerFakultas()},
	 * {@link #getPerSatkerJurusan()}, dan {@link #getPerSatkerSekolah()} &mdash; keempatnya
	 * di-OR untuk masuk cabang, lalu ketiga varian penugasan diperiksa berurutan untuk
	 * menentukan unit kerja mana yang dipakai.</p>
	 *
	 * @return {@code true} bila mode ini aktif; {@code null} diartikan {@code false}
	 */
	public Boolean getPerSatker() {
		return perSatker == null ? false : perSatker;
	}

	/**
	 * Menyetel mode rekap per unit kerja.
	 *
	 * <p>Dipanggil listener {@code onCheck} checkbox "Per-Satker" di grid master, yang menyimpan
	 * langsung ke database tanpa tombol Simpan.</p>
	 *
	 * @param perSatker nilai baru; {@code null} dibaca kembali sebagai {@code false}
	 */
	public void setPerSatker(Boolean perSatker) {
		this.perSatker = perSatker;
	}

	/**
	 * Varian mode per unit kerja yang mengambil unit dari <b>penugasan fakultas</b> pegawai
	 * ({@code Pegawai.getTendikFakultas().getSatuanKerja()}) bila tersedia; bila tidak, jatuh
	 * kembali ke {@code Pegawai.getSatuanKerja()}.
	 *
	 * <p>Diperiksa pada urutan kedua di dalam cabang per-unit-kerja, setelah
	 * {@link #getPerSatkerJurusan()} dan sebelum {@link #getPerSatkerSekolah()}. Bila lebih dari
	 * satu varian dicentang, urutan itulah yang menentukan pemenangnya &mdash; UI tidak
	 * mencegahnya.</p>
	 *
	 * @return {@code true} bila varian ini aktif; {@code null} diartikan {@code false}
	 */
	public Boolean getPerSatkerFakultas() {
		return perSatkerFakultas == null ? false : perSatkerFakultas;
	}

	/**
	 * Menyetel varian mode per unit kerja berbasis penugasan fakultas.
	 *
	 * <p>Dipanggil listener {@code onCheck} checkbox "Per-Fakultas" di grid master.</p>
	 *
	 * @param perSatkerFakultas nilai baru; {@code null} dibaca kembali sebagai {@code false}
	 */
	public void setPerSatkerFakultas(Boolean perSatkerFakultas) {
		this.perSatkerFakultas = perSatkerFakultas;
	}

	/**
	 * Varian mode per unit kerja yang mengambil unit dari <b>penugasan jurusan/program studi</b>
	 * pegawai ({@code Pegawai.getTendikJurusan().getSatuanKerja()}).
	 *
	 * <p>Diperiksa <b>paling awal</b> di dalam cabang per-unit-kerja, sehingga varian ini
	 * mengalahkan {@link #getPerSatkerFakultas()} dan {@link #getPerSatkerSekolah()} bila
	 * sama-sama dicentang dan pegawai memang punya penugasan jurusan. Label layarnya
	 * "Per-Prodi".</p>
	 *
	 * @return {@code true} bila varian ini aktif; {@code null} diartikan {@code false}
	 */
	public Boolean getPerSatkerJurusan() {
		return perSatkerJurusan == null ? false : perSatkerJurusan;
	}

	/**
	 * Menyetel varian mode per unit kerja berbasis penugasan jurusan/prodi.
	 *
	 * <p>Dipanggil listener {@code onCheck} checkbox "Per-Prodi" di grid master.</p>
	 *
	 * @param perSatkerJurusan nilai baru; {@code null} dibaca kembali sebagai {@code false}
	 */
	public void setPerSatkerJurusan(Boolean perSatkerJurusan) {
		this.perSatkerJurusan = perSatkerJurusan;
	}

	/**
	 * Varian mode per unit kerja yang mengambil unit dari <b>penugasan sekolah</b> pegawai
	 * ({@code Pegawai.getTendikSekolah().getSatuanKerja()}).
	 *
	 * <p>Diperiksa terakhir di dalam cabang per-unit-kerja.</p>
	 *
	 * @return {@code true} bila varian ini aktif; {@code null} diartikan {@code false}
	 */
	public Boolean getPerSatkerSekolah() {
		return perSatkerSekolah == null ? false : perSatkerSekolah;
	}

	/**
	 * Menyetel varian mode per unit kerja berbasis penugasan sekolah.
	 *
	 * <p><b>Perhatian &mdash; efek samping tak terduga dari layar master:</b> setter ini
	 * dipanggil dari <b>dua</b> listener berbeda. Selain checkbox "Per-Sekolah" yang benar,
	 * listener {@code onCheck} checkbox <b>"Query Manual"</b> juga memanggil method ini alih-alih
	 * {@link #setQueryManual(Boolean)} (bug salin-tempel di
	 * {@code JenisFormatGajiAction}). Akibatnya mencentang "Query Manual" di layar diam-diam
	 * mengubah mode pengelompokan menjadi per-sekolah, sementara {@link #getQueryManual()}
	 * sendiri tidak pernah berubah.</p>
	 *
	 * @param perSatkerSekolah nilai baru; {@code null} dibaca kembali sebagai {@code false}
	 */
	public void setPerSatkerSekolah(Boolean perSatkerSekolah) {
		this.perSatkerSekolah = perSatkerSekolah;
	}

	/**
	 * Bendera "template membawa query sendiri".
	 *
	 * <p>Berbeda dari enam bendera lain yang hanya memilih mode pengelompokan, bendera ini
	 * mengubah <b>seluruh pipeline</b> laporan. Ketika {@code true},
	 * {@code LaporanRekapPembayaranBerdasarkanFormatNilai} melewati sepenuhnya query
	 * {@code PembayaranGajiPunyaPegawai}, penyusunan kolom item gaji, maupun agregasi subtotal;
	 * berkas {@code .jrxml} yang terunggah diasumsikan memuat {@code queryString} sendiri dan
	 * hanya menerima parameter {@code bulan}, {@code tahun}, {@code satuan_kerja_id}, dan
	 * {@code caraPembayaranGaji}. Konsekuensinya: penyaringan tenant sepenuhnya bergantung pada
	 * apakah penulis template memakai parameter {@code satuan_kerja_id} &mdash; tidak ada
	 * pembatas apa pun yang ditegakkan kode Java pada mode ini.</p>
	 * <p><b>Kuirk:</b> karena bug listener yang dijelaskan pada
	 * {@link #setPerSatkerSekolah(Boolean)}, bendera ini <b>tidak dapat diaktifkan dari layar
	 * master</b>. Satu-satunya jalur penulisan yang berfungsi adalah impor Excel
	 * ({@code Common.uploadData(...)}, kolom {@code queryManual} termasuk dalam daftar
	 * {@code contents}) dan jalur Generic CRUD v2.</p>
	 *
	 * @return {@code true} bila template membawa query sendiri; {@code null} diartikan
	 *         {@code false}
	 */
	public Boolean getQueryManual() {
		return queryManual == null ? false : queryManual;
	}

	/**
	 * Menyetel bendera "template membawa query sendiri".
	 *
	 * <p><b>Tidak ada pemanggil yang berfungsi dari layar ZK</b> (lihat
	 * {@link #getQueryManual()}); pemanggil nyata hanya jalur impor data dan Generic CRUD v2.</p>
	 *
	 * @param queryManual nilai baru; {@code null} dibaca kembali sebagai {@code false}
	 */
	public void setQueryManual(Boolean queryManual) {
		this.queryManual = queryManual;
	}

	/**
	 * Mode rekap <b>per bank penerima</b>: mesin laporan mengelompokkan slip berdasarkan rekening
	 * bank pegawai yang berlaku untuk format item gaji terkait
	 * ({@code Pegawai.ambilBank(formatItemGaji)}), dan menambahkan baris subtotal setiap kali
	 * banknya berganti serta satu baris total di akhir.
	 *
	 * <p>Dievaluasi pada cabang kedua rantai {@code if/else if}, yaitu hanya bila
	 * {@link #getPerKeluarga()} bernilai {@code false}. Bila aktif, keempat bendera
	 * {@code perSatker*} diabaikan sepenuhnya.</p>
	 *
	 * @return {@code true} bila mode ini aktif; {@code null} diartikan {@code false}
	 */
	public Boolean getPerBank() {
		return perBank == null ? false : perBank;
	}

	/**
	 * Menyetel mode rekap per bank penerima.
	 *
	 * <p>Dipanggil listener {@code onCheck} checkbox "Per-Bank" di grid master.</p>
	 *
	 * @param perBank nilai baru; {@code null} dibaca kembali sebagai {@code false}
	 */
	public void setPerBank(Boolean perBank) {
		this.perBank = perBank;
	}

	/**
	 * Mode rincian <b>per anggota keluarga</b>: alih-alih satu baris per pegawai, laporan
	 * memecah menjadi satu baris per anggota {@code Keluarga} berstatus aktif dan menyertakan
	 * perhitungan premi asuransi (premi pegawai sendiri ditambah premi seluruh anggota
	 * keluarganya).
	 *
	 * <p>Ini adalah cabang <b>pertama</b> dan berprioritas tertinggi pada rantai
	 * {@code if/else if} laporan: bila aktif, seluruh bendera lain ({@link #getPerBank()} dan
	 * keempat {@code perSatker*}) tidak pernah dievaluasi. Perlu dicatat bahwa pegawai tanpa
	 * anggota keluarga aktif akan <b>hilang sama sekali</b> dari hasil cetak pada mode ini,
	 * karena barisnya hanya dibangkitkan di dalam perulangan anggota keluarga.</p>
	 *
	 * @return {@code true} bila mode ini aktif; {@code null} diartikan {@code false}
	 */
	public Boolean getPerKeluarga() {
		return perKeluarga == null ? false : perKeluarga;
	}

	/**
	 * Menyetel mode rincian per anggota keluarga.
	 *
	 * <p>Dipanggil listener {@code onCheck} checkbox "Per-Keluarga" di grid master.</p>
	 *
	 * @param perKeluarga nilai baru; {@code null} dibaca kembali sebagai {@code false}
	 */
	public void setPerKeluarga(Boolean perKeluarga) {
		this.perKeluarga = perKeluarga;
	}

}
