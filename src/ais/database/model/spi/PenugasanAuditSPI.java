package ais.database.model.spi;

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

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * <h2>PenugasanAuditSPI &mdash; Surat Tugas/Pelaksanaan Satu Kegiatan Audit</h2>
 *
 * <p>
 * Kelas ini merepresentasikan SATU pelaksanaan audit nyata di lapangan &mdash; dokumen yang di
 * dunia audit internal setara dengan "Surat Tugas" atau "Lembar Kerja Audit". Satu baris di sini
 * adalah satu kali SPI benar-benar terjun mengaudit satu unit kerja ({@link SatuanKerja}) dengan
 * satu jenis audit tertentu ({@link JenisAuditSPI}), lengkap dengan tim yang ditugaskan
 * ({@link TimAuditSPI}) dan temuan-temuan yang dihasilkan ({@link TemuanAuditSPI}). Kelas ini
 * adalah muara dari seluruh data yang dibangun pada Bagian A (checklist) dan Bagian B (rencana
 * berbasis risiko) &mdash; lihat {@link #getRencanaAuditTahunanSPI()} untuk tautan opsional ke
 * rencana yang mendasarinya.
 * </p>
 *
 * <h3>Mengapa {@code extends DataSop}: alur persetujuan otomatis lewat mesin SOP</h3>
 * <p>
 * Kelas ini SENGAJA dibuat turunan {@link DataSop} (bukan entity biasa) agar penugasan audit
 * otomatis mendapat alur persetujuan berjenjang lewat mesin SOP/Disposisi yang SUDAH ADA dan
 * terbukti di produksi &mdash; sama seperti {@code ais.database.model.spmi.HasilSPMI} pada modul
 * Audit Mutu Internal akademik. Dengan begitu, TIDAK PERLU menulis kode routing/persetujuan baru
 * sama sekali: cukup {@link ais.action.master.spi.PenugasanAuditSPIAction} mengimplementasikan
 * antarmuka {@code ais.ui.util.FormSop}, dan mesin SOP yang sudah ada akan memanggilnya secara
 * refleksi setiap kali dokumen ini perlu ditampilkan/disetujui di sepanjang alur berjenjang yang
 * dikonfigurasi admin SOP.
 * </p>
 *
 * <h3>Prinsip Three Lines Model: alur persetujuan HARUS independen dari unit yang diaudit</h3>
 * <p>
 * Berbeda dari kebanyakan dokumen lain di aplikasi ini yang alur persetujuannya mengikuti hierarki
 * struktural organisasi biasa (mis. atasan langsung), praktik terbaik audit internal (IIA's Three
 * Lines Model) mensyaratkan SPI sebagai "lini pertahanan ketiga" yang independen dari struktur yang
 * diaudit &mdash; sehingga alur persetujuan penugasan audit semestinya diarahkan ke Yayasan/Senat/
 * Dewan Pengawas, BUKAN ke Rektor/Kepala Sekolah unit yang justru sedang diperiksa. Kelas ini
 * sendiri tidak memaksakan rute tertentu secara terprogram &mdash; rute persetujuan sepenuhnya
 * berupa KONFIGURASI DATA pada layar admin SOP ({@code Sop}/{@code AlurSop}), bukan kode. Ini
 * sengaja dibuat fleksibel karena struktur pengawasan riil berbeda-beda antar lembaga (ada yang
 * punya Dewan Pengawas formal, ada yang cukup Ketua Yayasan), namun operator WAJIB diingatkan saat
 * konfigurasi awal untuk TIDAK mengarahkan alur ke posisi yang justru diaudit.
 * </p>
 *
 * <h3>Auditee terstruktur ({@link SatuanKerja}), tim auditor terstruktur ({@link TimAuditSPI})</h3>
 * <p>
 * Field {@link #getSatuanKerja()} (bukan kolom teks bebas) menjamin auditee SELALU merujuk unit
 * organisasi yang benar-benar ada di data resmi &mdash; mencegah salah ketik nama unit yang lazim
 * terjadi bila dipakai kolom teks bebas, sekaligus otomatis memungkinkan laporan/dasbor
 * direkapitulasi per unit dengan akurat. Demikian pula, siapa saja yang bertugas dalam satu
 * penugasan TIDAK disimpan sebagai satu nama teks tunggal, melainkan lewat relasi many-to-many ke
 * {@link Tbmuser} lewat tabel {@link TimAuditSPI} &mdash; sehingga satu penugasan bisa punya banyak
 * anggota tim dengan peran berbeda (Ketua Tim, Anggota), dan riwayat siapa saja yang pernah menjadi
 * auditor tetap tercatat secara terstruktur untuk keperluan rotasi/independensi auditor di kemudian
 * hari.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "penugasan_audit_spi")
public class PenugasanAuditSPI extends DataSop {

	/**
	 * Kode status awal &mdash; penugasan baru dibuat dan sedang menunggu proses persetujuan
	 * berjenjang lewat mesin SOP/Disposisi. Lihat {@link #getStatus()} untuk penjelasan lengkap
	 * bagaimana status ini dihitung ulang dinamis dari {@link #getDisposisiSop()}.
	 */
	public static final String PENGAJUAN = "Pengajuan";
	/**
	 * Kode status "sudah disetujui" &mdash; dicapai otomatis begitu {@link #getDisetujuiOleh()}
	 * mengembalikan nilai bukan {@code null} (baik lewat alur mesin SOP normal maupun lewat
	 * {@link #getTanggalPersetujuanManual()} untuk kasus persetujuan manual/migrasi data lama).
	 */
	public static final String DISETUJU = "Disetujui";
	/**
	 * Kode status "ditolak" &mdash; dicapai otomatis begitu {@link #getDisposisiSop()} menunjukkan
	 * alur disposisi telah berakhir di titik penolakan (lihat {@link #getStatus()}).
	 */
	public static final String DITOLAK = "Ditolak";

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field ini adalah SHADOW dari jejak
	 * audit yang sesungguhnya sudah lengkap tercatat oleh Hibernate Envers ({@code @Audited} pada
	 * kelas ini) &mdash; keberadaannya BUKAN duplikasi yang keliru, melainkan KEHARUSAN TEKNIS:
	 * Envers menyimpan riwayat revisi lengkap di tabel `_aud` yang mahal/berat untuk sekadar
	 * menampilkan "terakhir diubah oleh siapa" pada satu baris di layar daftar, sehingga pasangan
	 * {@code oleh}/{@code olehId} ini menyediakan cara murah &amp; cepat membaca informasi tersebut
	 * tanpa query terpisah ke tabel Envers. CATATAN: field ini berbeda konsep dari
	 * {@link #getDibuatOleh()}/{@link #getDisetujuiOleh()} &mdash; pasangan {@code oleh}/{@code olehId}
	 * mencatat SIAPA TERAKHIR MENGEDIT baris ini (bisa siapa saja dengan hak ubah), sedangkan
	 * {@link #getDibuatOleh()}/{@link #getDisetujuiOleh()} mencatat identitas PENGAJU dan PENYETUJU
	 * dalam alur SOP yang sesungguhnya (diturunkan dari {@link #getDisposisiSop()}).
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi (mis. data lama sebelum field ini ditambahkan).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini. SENGAJA mengabaikan nilai kosong/blank (tidak
	 * menimpa nilai yang sudah ada) agar proses simpan yang tidak membawa konteks pengguna (mis.
	 * batch/import) tidak menghapus jejak siapa yang terakhir benar-benar mengubah data secara
	 * manual.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna yang mengubah baris ini. Sama seperti {@link #setOlehId(String)},
	 * nilai kosong/blank sengaja diabaikan supaya tidak menimpa jejak yang sudah tercatat.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama (bukan ID) pengguna yang terakhir mengubah baris ini. Lihat
	 * {@link #getOlehId()} untuk penjelasan lengkap kenapa pasangan field ini sengaja ada
	 * berdampingan dengan riwayat Envers dan berbeda konsep dari {@link #getDibuatOleh()}/
	 * {@link #getDisetujuiOleh()}.
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang dipanggil otomatis oleh Hibernate tepat sebelum setiap
	 * operasi UPDATE dieksekusi ke database. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menyegarkan
	 * {@link #getTanggal_dirubah()} ke waktu saat ini &mdash; sehingga kode aplikasi TIDAK PERLU
	 * mengingat untuk memanggil {@code setTanggal_dirubah(new Date())} secara manual pada setiap
	 * titik yang mengubah entity ini.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi manual waktu terakhir baris ini diubah. Dalam praktiknya field ini disegarkan
	 * otomatis lewat {@link #onUpdate()} pada setiap UPDATE, sehingga setter ini terutama dipakai
	 * saat entity pertama kali dibuat atau saat memuat ulang data historis.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil waktu terakhir baris ini diubah, diinisialisasi sejak objek dibuat ke waktu saat
	 * itu ({@link ais.ui.util.WaktuUtil#getDate()}) dan disegarkan otomatis oleh {@link #onUpdate()}
	 * setiap kali terjadi UPDATE ke database.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris penugasan ini (format {@code "<id>-<nama>"}) untuk log/debug.
	 *
	 * @return string gabungan ID dan nama surat tugas.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private DisposisiSop disposisiSop;
	private RencanaAuditTahunanSPI rencanaAuditTahunanSPI;
	private JenisAuditSPI jenisAuditSPI;
	private SatuanKerja satuanKerja;
	private String nama;
	private String keterangan;
	private Date tanggalMulai;
	private Date tanggalSelesai;
	private Boolean aktif;
	private String status;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Date tanggalPersetujuanManual;

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public PenugasanAuditSPI() {
	}

	/**
	 * ID primer baris ini, di-generate otomatis oleh database (strategi {@code IDENTITY}) saat
	 * baris pertama kali disimpan. Anotasi {@code insertable = false} berarti kolom ini TIDAK
	 * pernah dikirim eksplisit dalam perintah INSERT &mdash; nilainya sepenuhnya diserahkan ke
	 * mekanisme auto-increment database.
	 *
	 * @return ID unik baris ini, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris ini secara manual, terutama saat membangun objek referensi ringan untuk
	 * relasi {@code JoinColumn} tanpa memuat seluruh baris dari database.
	 *
	 * @param id ID baris yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama/judul surat tugas ini (mis. "Audit Keuangan Semester I - Fakultas Teknik"). Nilai
	 * dikembalikan sudah di-{@code trim()}, dan BERBEDA dari kebanyakan field teks lain di
	 * aplikasi ini yang memakai {@code null} sebagai fallback, di sini fallback-nya adalah string
	 * KOSONG ({@code ""}) &mdash; menghindari {@code NullPointerException} pada kode tampilan yang
	 * langsung memanggil method String (mis. {@code .length()}) tanpa null-check, karena field ini
	 * lazim langsung dirender sebagai judul jendela/baris tabel.
	 *
	 * @return nama surat tugas yang sudah dipangkas spasinya; string kosong (BUKAN {@code null})
	 *         bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? "" : this.nama.trim();
	}

	/**
	 * Mengisi nama/judul surat tugas ini.
	 *
	 * @param nama nama baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tambahan mengenai penugasan ini, mis. konteks/latar belakang mengapa
	 * penugasan ini dibuat di luar catatan formal lain.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk penugasan ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif/nonaktif baris ini; nilai {@code null} SENGAJA diperlakukan sebagai
	 * {@code true} (aktif) demi kompatibilitas data lama &mdash; konvensi baku entity di aplikasi
	 * ini. BERBEDA dari {@link #getStatus()} (status alur persetujuan Pengajuan/Disetujui/
	 * Ditolak): field ini murni menandai apakah baris penugasan (apapun status persetujuannya)
	 * masih ditampilkan/berlaku, bukan bagian dari alur SOP.
	 *
	 * @return {@code true} bila penugasan ini aktif (termasuk saat nilai tersimpan {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif penugasan ini. Menonaktifkan (bukan menghapus) menjaga
	 * integritas referensial baris {@link TimAuditSPI}/{@link TemuanAuditSPI} yang sudah pernah
	 * mengacu ke sini.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Simpul disposisi mesin SOP generik yang menjadi mesin penggerak alur persetujuan
	 * berjenjang untuk penugasan ini &mdash; lihat javadoc kelas bagian "Mengapa extends DataSop".
	 * Seluruh field turunan status persetujuan pada kelas ini ({@link #getStatus()},
	 * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()}, {@link #getDibuatOleh()})
	 * pada akhirnya membaca nilai dari objek inilah, BUKAN dari kolom tersimpan langsung di baris
	 * ini &mdash; menjadikan {@code DisposisiSop} sebagai satu-satunya sumber kebenaran (source of
	 * truth) status alur persetujuan.
	 *
	 * @return simpul disposisi SOP terkait, atau {@code null} bila penugasan ini belum pernah
	 *         diajukan ke alur persetujuan (mis. baru dibuat sebagai draf).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Mengaitkan penugasan ini ke satu simpul disposisi SOP. Menolak (mengabaikan diam-diam)
	 * argumen {@code null} atau yang belum memiliki ID tersimpan, mencegah tautan wajib mesin SOP
	 * hilang akibat dipanggil dengan objek yang belum sempat di-persist. CATATAN IMPLEMENTASI:
	 * ekspresi ternary pada baris terakhir method ini SECARA LOGIS TIDAK PERNAH mengevaluasi
	 * cabang {@code true}-nya &mdash; kondisi {@code (disposisiSop == null || disposisiSop.getId() == null)}
	 * di dalam ternary sudah dijamin {@code false} pada titik itu (karena bila salah satunya benar,
	 * method sudah RETURN lebih dulu di baris {@code if} sebelumnya). Baris ini setara dengan
	 * {@code this.disposisiSop = disposisiSop;} sederhana; kode ternary tambahan tampaknya sisa
	 * refactor/salin-tempel yang tidak dibersihkan, BUKAN bug fungsional (hasil akhirnya tetap
	 * benar), namun berpotensi membingungkan pembaca kode berikutnya.
	 *
	 * @param disposisiSop simpul disposisi SOP baru; diabaikan bila {@code null} atau belum
	 *        memiliki ID (belum tersimpan).
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Tautan OPSIONAL ke baris rencana kerja tahunan (PKPT) yang mendasari penugasan ini, bila
	 * penugasan ini adalah realisasi dari rencana {@link RencanaAuditTahunanSPI#REGULER} yang
	 * sudah disusun sebelumnya &mdash; lihat javadoc kelas. Boleh {@code null} untuk penugasan
	 * yang muncul di luar PKPT (mis. audit dadakan yang belum sempat direncanakan formal).
	 *
	 * @return rencana tahunan yang mendasari penugasan ini, atau {@code null} bila tidak ada/tidak
	 *         diketahui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "rencana_audit_tahunan_spi", nullable = true)
	public RencanaAuditTahunanSPI getRencanaAuditTahunanSPI() {
		rencanaAuditTahunanSPI = check(rencanaAuditTahunanSPI);
		return rencanaAuditTahunanSPI;
	}

	/**
	 * Mengaitkan penugasan ini ke satu baris rencana tahunan (PKPT).
	 *
	 * @param rencanaAuditTahunanSPI rencana tahunan baru; boleh {@code null}.
	 */
	public void setRencanaAuditTahunanSPI(RencanaAuditTahunanSPI rencanaAuditTahunanSPI) {
		this.rencanaAuditTahunanSPI = rencanaAuditTahunanSPI;
	}

	/**
	 * Jenis/kategori audit yang dilaksanakan pada penugasan ini. Relasi wajib
	 * ({@code nullable = false}): setiap pelaksanaan audit HARUS menyatakan jenis auditnya, karena
	 * inilah yang menentukan checklist mana ({@link ChecklistAuditSPI}) yang relevan dirender saat
	 * pemeriksaan berlangsung.
	 *
	 * @return jenis audit yang dilaksanakan pada penugasan ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_audit_spi", nullable = false)
	public JenisAuditSPI getJenisAuditSPI() {
		jenisAuditSPI = check(jenisAuditSPI);
		return jenisAuditSPI;
	}

	/**
	 * Mengisi jenis audit yang dilaksanakan pada penugasan ini.
	 *
	 * @param jenisAuditSPI jenis audit baru.
	 */
	public void setJenisAuditSPI(JenisAuditSPI jenisAuditSPI) {
		this.jenisAuditSPI = jenisAuditSPI;
	}

	/**
	 * Unit kerja (auditee) yang diaudit pada penugasan ini &mdash; lihat javadoc kelas bagian
	 * "Auditee terstruktur" untuk alasan lengkap memakai relasi {@link SatuanKerja}, bukan kolom
	 * teks bebas. Relasi wajib ({@code nullable = false}).
	 *
	 * @return unit kerja yang diaudit pada penugasan ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengaitkan penugasan ini ke satu unit kerja auditee.
	 *
	 * @param satuanKerja unit kerja baru yang diaudit.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Tanggal mulai pelaksanaan audit di lapangan. Default nilai saat ini
	 * ({@link WaktuUtil#getDate()}) bila belum diisi &mdash; asumsi praktis bahwa penugasan yang
	 * baru dibuat lazimnya dimaksudkan mulai berjalan sejak hari itu juga, kecuali dinyatakan lain.
	 *
	 * @return tanggal mulai pelaksanaan; hari ini bila nilai tersimpan {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		return tanggalMulai == null ? WaktuUtil.getDate() : tanggalMulai;
	}

	/**
	 * Mengisi tanggal mulai pelaksanaan audit ini.
	 *
	 * @param tanggalMulai tanggal mulai baru.
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Tanggal selesai pelaksanaan audit di lapangan. BERBEDA dari {@link #getTanggalMulai()},
	 * field ini TIDAK memiliki nilai default &mdash; tetap {@code null} sampai audit benar-benar
	 * dinyatakan tuntas, sehingga kekosongannya bisa dipakai sebagai penanda "audit masih
	 * berjalan" pada laporan/dasbor.
	 *
	 * @return tanggal selesai pelaksanaan, atau {@code null} bila audit belum/tidak dinyatakan
	 *         selesai.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Mengisi tanggal selesai pelaksanaan audit ini.
	 *
	 * @param tanggalSelesai tanggal selesai baru.
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengisi field {@code dibuatOleh} secara langsung/mentah, TANPA melalui logika sinkronisasi
	 * dari {@link #getDisposisiSop()} yang dilakukan {@link #getDibuatOleh()}. Dipakai terutama
	 * saat memuat data lama (migrasi) yang dibuat sebelum penugasan ini terintegrasi ke mesin SOP,
	 * atau saat unit test/seed data ({@code SpiSampleDataHelper}) perlu mengisi nilai ini secara
	 * eksplisit tanpa harus membangun rangkaian {@link DisposisiSop} yang lengkap.
	 *
	 * @param dibuatOleh pengguna pembuat yang akan diisi mentah ke field.
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengambil pengguna yang MENGAJUKAN/membuat penugasan ini &mdash; sebuah GETTER DESTRUKTIF
	 * (memiliki efek samping menulis ke field {@code this.dibuatOleh}) yang setiap kali dipanggil
	 * mencoba MENYINKRONKAN ulang nilainya dari {@link #getDisposisiSop()}, sumber kebenaran
	 * sesungguhnya untuk siapa pengaju alur SOP ini (lihat javadoc kelas bagian "Mengapa extends
	 * DataSop"). Bila {@link #getDisposisiSop()} memiliki simpul awal
	 * ({@code DisposisiSop.getDisposisiStart()}) dengan pengaju yang tercatat, nilai TERSEBUT
	 * yang dipakai &mdash; MENIMPA apapun yang sebelumnya tersimpan di field {@code dibuatOleh},
	 * termasuk yang diisi manual lewat {@link #setDibuatOleh(Tbmuser)}. Sebaliknya, bila disposisi
	 * belum ada/belum memiliki simpul awal, nilai field yang sudah ada (hasil {@link #check(Object)}
	 * atau pengisian manual sebelumnya) tetap dipertahankan sebagai fallback &mdash; getter ini
	 * TIDAK PERNAH menghapus nilai fallback yang valid hanya karena disposisi belum siap.
	 * <p>
	 * Seluruh proses sinkronisasi ini dibungkus blok {@code try/catch} generik yang MENELAN semua
	 * jenis {@code Exception} (bukan hanya {@code LazyInitializationException} yang disebutkan
	 * di komentar kode) dan hanya mencatatnya ke {@link ais.common.ErrorAuditUtil} tanpa
	 * melempar ulang. Ini SENGAJA dilakukan karena {@link #getDisposisiSop()} bisa jadi merujuk
	 * instance proxy Hibernate "canonical/shared" (dipakai ulang lintas request oleh
	 * {@code AuditTimestampInterceptor}) yang sesi Hibernate aslinya sudah tertutup pada saat
	 * getter ini dipanggil dari request/thread lain &mdash; tanpa penanganan ini, satu baris
	 * penugasan dengan proxy "basi" semacam ini akan membuat SELURUH getter ini (dan turunannya:
	 * {@link #getStatus()}, {@link #getDisetujuiOleh()}) melempar
	 * {@code LazyInitializationException} yang merusak seluruh tampilan daftar penugasan, padahal
	 * nilai fallback yang sudah tersimpan di kolom database sebenarnya cukup memadai untuk
	 * ditampilkan. Trade-off dari pendekatan fail-silent ini: kegagalan sinkronisasi (apapun
	 * penyebabnya, tidak hanya lazy-loading) akan senyap dari sudut pandang pemanggil &mdash;
	 * hanya bisa diketahui lewat log {@code ErrorAuditUtil}, bukan lewat exception yang terlihat
	 * di lapisan tampilan.
	 *
	 * @return pengguna pengaju penugasan ini, disinkronkan dari disposisi SOP bila tersedia,
	 *         atau nilai fallback yang tersimpan bila disposisi belum ada/belum siap.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getDibuatOleh-lazy");
		}
		return dibuatOleh;
	}

	/**
	 * Mengisi field {@code disetujuiOleh} secara langsung/mentah, TANPA melalui logika
	 * sinkronisasi-dari-disposisi yang dilakukan {@link #getDisetujuiOleh()}. Lihat javadoc
	 * {@link #getDisetujuiOleh()} untuk penjelasan lengkap kenapa nilai yang diisi lewat setter
	 * ini bisa saja DITIMPA lagi pada pemanggilan getter berikutnya bila
	 * {@link #getDisposisiSop()} sudah memiliki simpul persetujuan yang tercatat.
	 *
	 * @param disetujuiOleh pengguna penyetuju yang akan diisi mentah ke field.
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengambil pengguna yang MENYETUJUI penugasan ini &mdash; sebuah GETTER DESTRUKTIF (efek
	 * samping menulis ke field {@code this.disetujuiOleh} DAN {@code this.tanggalPersetujuan})
	 * yang menjadi INTI logika penentuan status persetujuan pada kelas ini
	 * ({@link #getStatus()} bergantung penuh pada hasil method ini bernilai bukan {@code null}).
	 * <p>
	 * Alur logikanya, dieksekusi berurutan setiap kali method ini dipanggil:
	 * </p>
	 * <ol>
	 *   <li>{@code disetujuiOleh = check(disetujuiOleh)} &mdash; membaca nilai fallback yang
	 *       sudah tersimpan di field (lihat {@link #check(Object)} pada superclass).</li>
	 *   <li>Di dalam blok {@code try}: bila {@link #getDisposisiSop()} memiliki simpul persetujuan
	 *       ({@code getDisposisiSetuju()}) dengan pengaju yang tercatat, nilai TERSEBUT MENIMPA
	 *       {@code disetujuiOleh} &mdash; disposisi SOP adalah sumber kebenaran, mengalahkan
	 *       apapun yang tersimpan mentah di kolom database.</li>
	 *   <li>SEBALIKNYA, bila disposisi ADA tapi simpul persetujuannya BELUM ada/BELUM punya
	 *       pengaju, {@code disetujuiOleh} secara EKSPLISIT di-null-kan &mdash; ini penting: bukan
	 *       hanya "tidak diisi", melainkan AKTIF MENGOSONGKAN nilai lama sekalipun sebelumnya
	 *       pernah terisi (mis. skenario status berubah dari Disetujui kembali ke Pengajuan/Ditolak
	 *       di sisi mesin SOP, field lokal ini WAJIB ikut kosong, bukan menampilkan approval basi).</li>
	 *   <li>Blok {@code try/catch} ini MENELAN semua {@code Exception} (bukan hanya
	 *       {@code LazyInitializationException}) dan hanya mencatat ke
	 *       {@link ais.common.ErrorAuditUtil} tanpa melempar ulang &mdash; lihat javadoc
	 *       {@link #getDibuatOleh()} untuk alasan lengkap pola fail-silent ini pada seluruh getter
	 *       turunan {@link #getDisposisiSop()} di kelas ini.</li>
	 *   <li>Setelah blok {@code try/catch}, {@code disetujuiOleh} di-{@code check()} SEKALI LAGI.</li>
	 *   <li>TERAKHIR, efek samping kedua: bila {@link #getTanggalPersetujuanManual()} terisi DAN
	 *       {@code disetujuiOleh} akhirnya bukan {@code null}, field {@code tanggalPersetujuan}
	 *       (bukan hanya dikembalikan, tapi DITULIS) disetel ke tanggal persetujuan manual tersebut
	 *       &mdash; jalur ini melayani skenario "persetujuan dicatat manual di luar mesin SOP"
	 *       (mis. migrasi data lama, atau persetujuan lisan/offline yang dicatat belakangan oleh
	 *       admin) TANPA perlu membangun rangkaian {@link DisposisiSop} yang lengkap, asalkan
	 *       {@code disetujuiOleh} tetap harus terisi dari salah satu jalur di atas.</li>
	 * </ol>
	 * <p>
	 * KONSEKUENSI PENTING bagi pemanggil: method ini BUKAN getter murni/idempoten tanpa efek
	 * samping &mdash; memanggilnya bisa mengubah state internal objek ({@code tanggalPersetujuan})
	 * sebagai side-effect. Kode yang membaca {@link #getTanggalPersetujuan()} SETELAH memanggil
	 * method ini akan melihat nilai yang sudah disinkronkan ulang, sedangkan membacanya SEBELUM
	 * memanggil method ini (mis. lewat serialisasi/refleksi yang mengakses field langsung) bisa
	 * melihat nilai lama yang belum disegarkan. Pola getter-dengan-efek-samping semacam ini adalah
	 * arsitektur berulang yang dipakai di banyak entity berbasis {@code DataSop} pada aplikasi ini
	 * (bukan bug spesifik kelas ini), namun tetap layak diwaspadai saat menelusuri urutan
	 * pemanggilan getter mana yang harus dieksekusi lebih dulu.
	 * </p>
	 *
	 * @return pengguna penyetuju penugasan ini, disinkronkan dari disposisi SOP bila tersedia
	 *         (termasuk di-null-kan aktif bila disposisi ada namun belum disetujui), atau nilai
	 *         fallback (termasuk hasil persetujuan manual) bila disposisi belum tersedia.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				disetujuiOleh = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getDisetujuiOleh-lazy");
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		return disetujuiOleh;
	}

	/**
	 * Mengisi field {@code tanggalPersetujuan} secara langsung/mentah, TANPA melalui logika
	 * sinkronisasi yang dilakukan {@link #getTanggalPersetujuan()}. Nilai yang diisi di sini bisa
	 * saja ditimpa lagi pada pemanggilan getter berikutnya bila {@link #getDisposisiSop()} sudah
	 * memiliki simpul persetujuan yang tercatat &mdash; lihat javadoc {@link #getTanggalPersetujuan()}.
	 *
	 * @param tanggalPersetujuan tanggal persetujuan yang akan diisi mentah ke field.
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengambil tanggal/waktu persetujuan penugasan ini &mdash; GETTER DESTRUKTIF (efek samping
	 * menulis ke field {@code this.tanggalPersetujuan}) dengan pola identik seperti
	 * {@link #getDisetujuiOleh()}: bila {@link #getDisposisiSop()} memiliki simpul persetujuan
	 * dengan pengaju tercatat, waktu disposisi tersebut ({@code getDisposisiSetuju().getWaktu()})
	 * MENIMPA nilai field; bila disposisi ada tapi simpul persetujuannya belum lengkap, field
	 * secara AKTIF di-null-kan (bukan sekadar dibiarkan); seluruh proses dibungkus try/catch yang
	 * menelan semua {@code Exception} dan hanya mencatat ke {@link ais.common.ErrorAuditUtil}
	 * (lihat javadoc {@link #getDibuatOleh()} untuk alasan lengkap pola fail-silent ini). CATATAN:
	 * method ini TIDAK menerapkan logika persetujuan manual seperti pada
	 * {@link #getDisetujuiOleh()} &mdash; nilai dari {@link #getTanggalPersetujuanManual()} hanya
	 * ditulis ke field ini sebagai efek samping SAAT {@link #getDisetujuiOleh()} dipanggil, bukan
	 * di dalam method ini sendiri; memanggil {@link #getTanggalPersetujuan()} TANPA pernah
	 * memanggil {@link #getDisetujuiOleh()} lebih dulu tidak akan merefleksikan tanggal
	 * persetujuan manual tersebut.
	 *
	 * @return waktu persetujuan penugasan ini, disinkronkan dari disposisi SOP bila tersedia
	 *         (termasuk di-null-kan aktif bila disposisi ada namun belum disetujui), atau nilai
	 *         fallback yang tersimpan bila disposisi belum tersedia.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Mengisi tanggal pembuatan penugasan ini secara langsung/mentah.
	 *
	 * @param tanggalPembuatan tanggal pembuatan baru.
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Tanggal penugasan ini pertama kali dibuat/diajukan. BERBEDA dari
	 * {@link #getTanggalMulai()}/{@link #getTanggal_dirubah()}, field ini TIDAK disinkronkan dari
	 * {@link #getDisposisiSop()} &mdash; murni nilai kolom database dengan fallback
	 * {@code new Date()} (waktu SAAT getter dipanggil, BUKAN waktu pembuatan objek) bila belum
	 * pernah diisi.
	 *
	 * @return tanggal pembuatan penugasan; waktu saat ini bila nilai tersimpan {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Mengambil status alur persetujuan penugasan ini &mdash; salah satu dari {@link #PENGAJUAN}/
	 * {@link #DISETUJU}/{@link #DITOLAK}. Method ini adalah GETTER DESTRUKTIF paling sentral pada
	 * kelas ini: field {@code this.status} DITULIS ULANG sebagai efek samping pemanggilan,
	 * menjadikannya representasi turunan (derived) dari {@link #getDisposisiSop()} dan
	 * {@link #getDisetujuiOleh()}, BUKAN nilai independen yang sepenuhnya dikendalikan oleh
	 * {@link #setStatus(String)}. Urutan evaluasinya:
	 * </p>
	 * <ol>
	 *   <li>Bila {@link #getDisetujuiOleh()} (yang sendiri juga getter destruktif, lihat
	 *       javadocnya) mengembalikan bukan {@code null}, status DIPAKSA menjadi
	 *       {@link #DISETUJU} &mdash; MENIMPA apapun nilai {@code status} sebelumnya, termasuk
	 *       {@link #DITOLAK} yang mungkin disetel manual lewat {@link #setStatus(String)}.</li>
	 *   <li>SEBALIKNYA, bila field {@code status} SEBELUMNYA (sebelum langkah 1 di atas) bernilai
	 *       {@link #DISETUJU} namun {@link #getDisetujuiOleh()} sudah kembali {@code null} (berarti
	 *       persetujuan sudah dicabut/dibatalkan di sisi disposisi), status dikembalikan ke
	 *       {@link #PENGAJUAN} &mdash; mencegah baris ini terus tampil "Disetujui" padahal
	 *       penyetujunya sudah hilang.</li>
	 *   <li>Di dalam blok {@code try/catch} yang menelan semua {@code Exception} (pola fail-silent
	 *       yang sama seperti {@link #getDibuatOleh()}/{@link #getDisetujuiOleh()}): bila
	 *       {@link #getDisposisiSop()} menunjukkan alur disposisi telah BERAKHIR
	 *       ({@code getDisposisiEnd()} tidak {@code null}) DAN titik akhir alur tersebut memang
	 *       menandakan penolakan ({@code getAlurSop().getPenolakanAdaDiSini()}), status DIPAKSA
	 *       menjadi {@link #DITOLAK} &mdash; langkah ini dievaluasi SETELAH langkah 1/2, sehingga
	 *       penolakan bisa MENIMPA status {@link #DISETUJU} yang barusan disetel bila kedua kondisi
	 *       (disetujui DAN alur berakhir sebagai tolakan) entah bagaimana sama-sama terpenuhi
	 *       &mdash; dalam praktik normal kedua kondisi ini seharusnya saling eksklusif (satu alur
	 *       SOP hanya berakhir pada satu titik: disetujui ATAU ditolak), tapi urutan evaluasi kode
	 *       ini membuat "ditolak" menang bila keduanya entah bagaimana terjadi bersamaan.</li>
	 *   <li>Nilai akhir: {@link #PENGAJUAN} bila {@code status} kosong/{@code null} setelah semua
	 *       langkah di atas, selain itu nilai {@code status} yang sudah disinkronkan.</li>
	 * </ol>
	 * <p>
	 * Untuk pengguna kelas ini (mis. kode tampilan/laporan): method ini SELALU aman dipanggil
	 * kapan saja untuk mendapat status TERKINI yang konsisten dengan keadaan disposisi SOP
	 * sesungguhnya, tanpa perlu memanggil getter lain lebih dulu secara manual &mdash; namun
	 * sebagai konsekuensinya, nilai {@code status} yang tersimpan mentah di kolom database TIDAK
	 * BOLEH dianggap sebagai sumber kebenaran final tanpa melalui getter ini, karena bisa saja
	 * "basi" dibanding keadaan disposisi terkini.
	 *
	 * @return kode status terkini penugasan ini, selalu salah satu dari {@link #PENGAJUAN}/
	 *         {@link #DISETUJU}/{@link #DITOLAK}.
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			disposisiSop = getDisposisiSop();
			if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
					&& disposisiSop.getDisposisiEnd().getAlurSop() != null
					&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
				status = DITOLAK;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/spi/PenugasanAuditSPI.java:getStatus-lazy");
		}

		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Mengisi status penugasan ini secara manual. PENTING: menyetel status ke {@link #DITOLAK}
	 * lewat method ini memiliki EFEK SAMPING TAMBAHAN yang disengaja &mdash; otomatis memanggil
	 * {@link #setDisetujuiOleh(Tbmuser)} dengan {@code null} dan {@link #setTanggalPersetujuan(Date)}
	 * dengan {@code null}, mengosongkan jejak persetujuan yang mungkin sudah ada sebelumnya. Ini
	 * konsisten dengan invarian yang dijaga {@link #getStatus()}: status {@link #DISETUJU} SELALU
	 * berkorespondensi dengan {@link #getDisetujuiOleh()} yang terisi, sehingga menyetel status
	 * penolakan sambil membiarkan {@code disetujuiOleh} lama tetap terisi akan menciptakan keadaan
	 * data yang tidak konsisten (baris "ditolak" tapi tetap tampil punya penyetuju). CATATAN:
	 * nilai yang diisi lewat setter ini (untuk status selain {@link #DITOLAK}) tetaplah tunduk
	 * pada logika penimpaan otomatis di {@link #getStatus()} pada pemanggilan berikutnya &mdash;
	 * mis. menyetel status ke {@link #PENGAJUAN} secara manual TIDAK akan bertahan bila
	 * {@link #getDisposisiSop()} ternyata sudah punya persetujuan tercatat, karena {@link #getStatus()}
	 * akan langsung menimpanya kembali ke {@link #DISETUJU} pada pembacaan berikutnya.
	 *
	 * @param status kode status baru; menyetel {@link #DITOLAK} otomatis mengosongkan penyetuju
	 *        dan tanggal persetujuan.
	 */
	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	/**
	 * Tanggal persetujuan yang dicatat MANUAL (di luar mesin SOP/Disposisi) &mdash; jalur
	 * alternatif untuk skenario persetujuan yang terjadi secara lisan/offline atau untuk migrasi
	 * data lama, TANPA perlu membangun rangkaian {@link DisposisiSop} yang lengkap. Lihat javadoc
	 * {@link #getDisetujuiOleh()} bagian langkah terakhir untuk bagaimana field ini akhirnya
	 * mempengaruhi {@link #getTanggalPersetujuan()}.
	 *
	 * @return tanggal persetujuan manual, atau {@code null} bila penugasan ini disetujui (atau
	 *         belum disetujui) sepenuhnya lewat mesin SOP normal.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Mengisi tanggal persetujuan manual untuk penugasan ini.
	 *
	 * @param tanggalPersetujuanManual tanggal persetujuan manual baru.
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}

}
