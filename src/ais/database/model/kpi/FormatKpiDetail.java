package ais.database.model.kpi;

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
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * Entitas JPA/Hibernate untuk tabel {@code public.format_kpi_detail} — baris "header" penugasan
 * satu {@link FormatKpi} (template KPI) ke satu {@code Pegawai}, efektif sejak
 * {@link #getTanggalEfektif()} tertentu.
 *
 * <p><b>Peran dalam model KPI:</b> kelas ini adalah simpul penghubung antara template
 * organisasi ({@link FormatKpi}, yang di-scope ke jurusan/fakultas/yayasan/sekolah/satuan kerja)
 * dan pegawai perorangan. Satu {@link FormatKpi} dapat memiliki banyak
 * {@code FormatKpiDetail} — satu per pegawai (dan berpotensi lebih dari satu per pegawai bila
 * template pernah berganti/diperbarui dengan tanggal efektif berbeda). Baris-baris item KPI
 * konkret untuk penugasan ini disimpan di {@link ItemKpi} melalui kolom
 * {@code format_kpi_detail}. Lihat javadoc {@link ItemKpi} untuk diagram alur relasi lengkap
 * ({@code FormatKpi -> FormatKpiDetail -> ItemKpi -> Kpi}).</p>
 *
 * <p>Field {@link #kunci} (relasi ke {@code Tbmuser}) menandakan siapa yang MENGUNCI baris
 * penugasan ini (mis. mencegah pegawai lain mengubah data setelah dikunci oleh atasan/HRD), dan
 * {@link #nilai} menyimpan skor/nilai akhir untuk keseluruhan penugasan ini (berbeda dari nilai
 * per-item yang disimpan di masing-masing {@link ItemKpi}).</p>
 *
 * <p><b>Pola arsitektur berulang yang perlu diwaspadai:</b></p>
 * <ul>
 *   <li><b>Field relasi yang di-"check()" (shadow re-resolve):</b> {@link #getFormatKpi()},
 *   {@link #getPegawai()}, {@link #getKunci()} — lihat
 *   {@link ais.database.model.GeneralValueObject#check(Object)}; KEHARUSAN TEKNIS, bukan bug.</li>
 *   <li><b>Flag {@code aktif} satu-arah:</b> {@link #getAktif()} men-default {@code null} ke
 *   {@code true} tanpa menuliskannya kembali ke field — konsisten dengan {@link Kpi},
 *   {@link ItemKpi}, {@link FormatKpi}.</li>
 *   <li><b>Field bayangan audit:</b> {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   dideklarasikan ulang lokal — KEHARUSAN TEKNIS untuk Hibernate Envers.</li>
 * </ul>
 *
 * @see FormatKpi
 * @see ItemKpi
 * @see Kpi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "format_kpi_detail")
public class FormatKpiDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi untuk kompatibilitas {@link java.io.Serializable}. Nilai identik dengan
	 * entitas-entitas lain dalam paket {@code kpi} — peninggalan hasil generate hbm2java.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer (identity, auto-increment) baris {@code format_kpi_detail}. */
	private Long id;

	/**
	 * Nama/username pengguna yang melakukan perubahan terakhir pada baris ini. Field bayangan
	 * audit, diisi oleh interceptor Hibernate — lihat catatan kelas.
	 */
	private String oleh;

	/**
	 * Id/identifier pengguna yang melakukan perubahan terakhir pada baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh mekanisme audit yang sama.
	 */
	private String olehId;

	/**
	 * Mengembalikan id/identifier pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Menolak (no-op) bila argumen {@code null} atau
	 * kosong/spasi saja, sehingga nilai lama tetap dipertahankan.
	 *
	 * @param olehId id pengguna baru; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi string untuk debugging/log: keterangan penugasan ini.
	 *
	 * @return nilai field {@code keterangan} apa adanya (tidak lewat {@link #getKeterangan()})
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengisi nama/username pengguna yang melakukan perubahan terakhir. Menolak (no-op) bila
	 * argumen {@code null} atau kosong/spasi saja.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/username pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat
	 * sebelum operasi UPDATE dieksekusi, mendelegasikan pencatatan stempel waktu perubahan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu saat ini pada saat objek dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir secara eksplisit. Biasanya dipanggil oleh
	 * mekanisme audit ({@link #onUpdate()}), bukan oleh kode aplikasi.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir pada baris ini.
	 *
	 * @return tanggal/waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Pegawai yang menjadi target penugasan template KPI ini. */
	private Pegawai pegawai;
	/** Template/format KPI yang ditugaskan ke pegawai ini. */
	private FormatKpi formatKpi;
	/** Tanggal mulai berlakunya penugasan template KPI ini bagi pegawai tersebut. */
	private Date tanggalEfektif;
	/** Keterangan/catatan bebas untuk penugasan ini. */
	private String keterangan;
	/** Pengguna (Tbmuser) yang mengunci baris penugasan ini, mis. mencegah perubahan lebih lanjut setelah difinalisasi. */
	private Tbmuser kunci;
	/** Penanda aktif/tidak; default {@code true} bila belum diisi — lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Membandingkan urutan tampil dua {@link GeneralValueObject} dengan mencoba berurutan:
	 * tanggal efektif, nomor urut, NIM, nama, lalu keterangan — memakai kriteria pertama yang
	 * tersedia (tidak null) pada KEDUA sisi perbandingan.
	 *
	 * <p>Perbandingan tanggal efektif melakukan pemaksaan tipe eksplisit
	 * {@code ((FormatKpiDetail) arg0)} — akan melempar {@link ClassCastException} bila
	 * {@code arg0} bukan instance {@code FormatKpiDetail}, namun exception tersebut ditelan oleh
	 * blok percobaan-tangkap di bawah (dicatat ke {@link ais.common.ErrorAuditUtil}) sehingga
	 * pengurutan tidak melempar exception ke pemanggil; hasil fallback adalah 0.</p>
	 *
	 * @param arg0 objek pembanding
	 * @return hasil {@code compareTo} kriteria pertama yang cocok, atau 0 bila tidak ada kriteria
	 *         yang bisa dibandingkan atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getTanggalEfektif() != null && ((FormatKpiDetail) arg0).getTanggalEfektif() != null) {
				return getTanggalEfektif().compareTo(((FormatKpiDetail) arg0).getTanggalEfektif());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kpi/FormatKpiDetail.java:101");

		}

		return 0;
	}

	/** Konstruktor tanpa argumen, dipakai Hibernate untuk membentuk instance via reflection. */
	public FormatKpiDetail() {
	}

	/**
	 * Mengembalikan kunci primer baris {@code format_kpi_detail}. Kolom identity
	 * ({@code insertable = false}) — nilainya dibuat oleh basis data saat INSERT.
	 *
	 * @return id baris ini, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id secara manual. Jarang dipakai aplikasi karena kolom bersifat
	 * {@code insertable = false}.
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan/catatan bebas untuk penugasan ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/catatan bebas untuk penugasan ini.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi template/format KPI yang ditugaskan ke pegawai ini.
	 *
	 * @param formatKpi format KPI baru
	 */
	public void setFormatKpi(FormatKpi formatKpi) {
		this.formatKpi = formatKpi;
	}

	/**
	 * Mengembalikan template/format KPI yang ditugaskan ke pegawai ini. Field di-refresh lewat
	 * {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return format KPI untuk penugasan ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_kpi", nullable = false)
	public FormatKpi getFormatKpi() {
		formatKpi = check(formatKpi);
		return formatKpi;
	}

	/**
	 * Mengembalikan pegawai yang menjadi target penugasan template KPI ini. Field di-refresh
	 * lewat {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc
	 * kelas.
	 *
	 * @return pegawai target penugasan ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Mengisi pegawai yang menjadi target penugasan template KPI ini.
	 *
	 * @param pegawai pegawai baru
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan status aktif penugasan ini.
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; nilai field bila sudah eksplisit di-set
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif penugasan ini.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal mulai berlakunya penugasan template KPI ini.
	 *
	 * @return tanggal efektif; bila belum pernah diisi, mengembalikan tanggal SAAT method ini
	 *         dipanggil ({@link ais.ui.util.WaktuUtil#getDate()}) — bukan {@code null} dan bukan
	 *         tanggal tetap, sehingga nilai fallback ini BERBEDA setiap kali dipanggil pada
	 *         hari yang berbeda selama field tetap kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalEfektif() {
		return tanggalEfektif == null ? WaktuUtil.getDate() : tanggalEfektif;
	}

	/**
	 * Mengisi tanggal mulai berlakunya penugasan template KPI ini.
	 *
	 * @param tanggalEfektif tanggal efektif baru
	 */
	public void setTanggalEfektif(Date tanggalEfektif) {
		this.tanggalEfektif = tanggalEfektif;
	}

	/**
	 * Mengembalikan pengguna (Tbmuser) yang mengunci baris penugasan ini. Field di-refresh
	 * lewat {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc
	 * kelas. Keberadaan nilai di sini (bukan {@code null}) menandakan penugasan telah dikunci
	 * dan (tergantung logika pemanggil di luar kelas ini) mungkin tidak lagi dapat diubah.
	 *
	 * @return pengguna yang mengunci baris ini, atau {@code null} bila belum dikunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kunci")
	public Tbmuser getKunci() {
		kunci = check(kunci);
		return kunci;
	}

	/**
	 * Mengisi pengguna yang mengunci baris penugasan ini.
	 *
	 * @param kunci pengguna pengunci baru, atau {@code null} untuk membuka kunci
	 */
	public void setKunci(Tbmuser kunci) {
		this.kunci = kunci;
	}

	/** Skor/nilai akhir untuk keseluruhan penugasan ini, berbeda dari nilai per-item yang disimpan di masing-masing {@link ItemKpi}. */
	private Double nilai;

	/**
	 * Mengembalikan skor/nilai akhir untuk keseluruhan penugasan ini.
	 *
	 * @return nilai akhir, default {@code 0.0} bila belum diisi
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Mengisi skor/nilai akhir untuk keseluruhan penugasan ini.
	 *
	 * @param nilai nilai akhir baru
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}
}
