package ais.database.model.crm;

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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Tahap (kolom Kanban) pada satu {@link CrmPipelineType} — mis. "Baru", "Kualifikasi",
 * "Penawaran", "Negosiasi", "Menang", "Kalah". {@link #isWon}/{@link #isLost} menandai tahap
 * penutup pipeline (dipakai UI Kanban untuk memicu popup alasan kalah / tanggal penutupan).
 *
 * <p>Mengikuti pola {@code ais.database.model.ticket.TicketKategori}. Tabel {@code public.crm_stage}
 * dibuat otomatis oleh {@code hbm2ddl=update} saat restart.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_stage")
public class CrmStage extends GeneralValueObject {

	private static final long serialVersionUID = 3120260815002L;

	/** Primary key baris {@code crm_stage}. */
	private Long id;
	/** Jenis pipeline pemilik tahap ini; setiap {@link CrmPipelineType} punya kumpulan tahap sendiri. */
	private CrmPipelineType pipelineType;
	/** Nama tahap yang ditampilkan sebagai judul kolom Kanban. */
	private String nama;
	/** Nomor urut tampil kolom Kanban dari kiri ke kanan; lihat {@link #getNomorUrut()} untuk default-nya. */
	private Integer nomorUrut;
	/** Probabilitas menang bawaan yang disarankan untuk lead yang masuk tahap ini. */
	private Integer probabilitasDefault;
	/** Penanda tahap penutup "menang"; lihat {@link #getIsWon()} untuk default-nya. */
	private Boolean isWon;
	/** Penanda tahap penutup "kalah"; lihat {@link #getIsLost()} untuk default-nya. */
	private Boolean isLost;
	/** Kode warna kolom Kanban (mis. kode heksa) untuk pembeda visual antar tahap. */
	private String warna;
	/** Flag aktif/nonaktif (soft delete); lihat {@link #getAktif()} untuk default-nya. */
	private Boolean aktif;
	/** Nama pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOlehId(String)}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diperbarui otomatis lewat {@link #onUpdate()} saat update. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public CrmStage() {
	}

	/**
	 * Mengembalikan primary key baris {@code crm_stage}.
	 *
	 * @return primary key, atau {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan jenis pipeline pemilik tahap ini, setelah diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}. Kolom database {@code nullable = false} — setiap
	 * tahap wajib terkait satu jenis pipeline; tidak ada tahap "global" lintas pipeline.
	 *
	 * @return jenis pipeline pemilik tahap ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pipeline_type", nullable = false)
	public CrmPipelineType getPipelineType() {
		pipelineType = check(pipelineType);
		return pipelineType;
	}

	/**
	 * Menyetel jenis pipeline pemilik tahap ini.
	 *
	 * @param pipelineType jenis pipeline baru
	 */
	public void setPipelineType(CrmPipelineType pipelineType) {
		this.pipelineType = pipelineType;
	}

	/**
	 * Mengembalikan nama tahap, dipangkas spasi tepi ({@code trim()}) — berbeda dari
	 * {@link CrmPipelineType#getNama()}/{@link CrmSalesTeam#getNama()} yang juga men-trim, method
	 * ini konsisten menerapkan aturan yang sama pada seluruh entity lookup CRM.
	 *
	 * @return nama tahap yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	/**
	 * Menyetel nama tahap. Nilai disimpan apa adanya (tanpa {@code trim()} saat setter dipanggil);
	 * pemangkasan spasi baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama tahap baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan nomor urut tampil kolom Kanban ini dari kiri ke kanan. Nilai {@code null}
	 * dinormalkan menjadi {@code 0}.
	 *
	 * @return nomor urut, tidak pernah {@code null}
	 */
	@Column(name = "nomor_urut", nullable = true)
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan probabilitas menang bawaan yang disarankan untuk lead yang masuk tahap ini.
	 * Nilai {@code null} dinormalkan menjadi {@code 0}. Nilai ini murni bawaan/saran — menyalinnya
	 * ke {@link CrmLead#getProbabilitas()} saat lead dipindah ke tahap ini (bila dilakukan) adalah
	 * tanggung jawab kode pemanggil, bukan otomatis oleh entity.
	 *
	 * @return probabilitas default, tidak pernah {@code null}
	 */
	@Column(name = "probabilitas_default", nullable = true)
	public Integer getProbabilitasDefault() {
		return probabilitasDefault == null ? 0 : probabilitasDefault;
	}

	/**
	 * Menyetel probabilitas menang bawaan.
	 *
	 * @param probabilitasDefault probabilitas default baru
	 */
	public void setProbabilitasDefault(Integer probabilitasDefault) {
		this.probabilitasDefault = probabilitasDefault;
	}

	/**
	 * Mengembalikan penanda apakah tahap ini adalah tahap penutup "menang". Nilai {@code null}
	 * dianggap {@code false} — tahap baru tanpa flag eksplisit dianggap BUKAN tahap penutup menang.
	 * Dipakai UI Kanban untuk memicu pengisian {@link CrmLead#getTanggalDitutup()} saat lead
	 * dipindahkan ke tahap ini.
	 *
	 * @return {@code true} bila tahap penutup menang, {@code false} bila bukan (default)
	 */
	@Column(name = "is_won", nullable = true)
	public Boolean getIsWon() {
		return isWon == null ? false : isWon;
	}

	/**
	 * Menyetel penanda tahap penutup "menang". Tidak ada validasi yang mencegah satu tahap
	 * bernilai {@link #getIsWon()} dan {@link #getIsLost()} sekaligus {@code true} — menjaga
	 * eksklusivitas keduanya adalah tanggung jawab UI konfigurasi pipeline.
	 *
	 * @param isWon penanda menang baru
	 */
	public void setIsWon(Boolean isWon) {
		this.isWon = isWon;
	}

	/**
	 * Mengembalikan penanda apakah tahap ini adalah tahap penutup "kalah". Nilai {@code null}
	 * dianggap {@code false}. Dipakai UI Kanban untuk memicu popup alasan kalah
	 * ({@link CrmLead#getLostReason()}/{@link CrmLead#getCatatanKalah()}) saat lead dipindahkan ke
	 * tahap ini.
	 *
	 * @return {@code true} bila tahap penutup kalah, {@code false} bila bukan (default)
	 */
	@Column(name = "is_lost", nullable = true)
	public Boolean getIsLost() {
		return isLost == null ? false : isLost;
	}

	/**
	 * Menyetel penanda tahap penutup "kalah".
	 *
	 * @param isLost penanda kalah baru
	 */
	public void setIsLost(Boolean isLost) {
		this.isLost = isLost;
	}

	/**
	 * Mengembalikan kode warna kolom Kanban (mis. kode heksa) untuk pembeda visual antar tahap.
	 * Tidak divalidasi formatnya di level entity.
	 *
	 * @return kode warna, atau {@code null} bila memakai warna bawaan UI
	 */
	@Column(name = "warna", nullable = true, length = 32)
	public String getWarna() {
		return warna;
	}

	/**
	 * Menyetel kode warna kolom Kanban.
	 *
	 * @param warna kode warna baru
	 */
	public void setWarna(String warna) {
		this.warna = warna;
	}

	/**
	 * Mengembalikan status aktif/nonaktif (soft delete). Nilai {@code null} dianggap {@code true}.
	 *
	 * @return {@code true} bila aktif (default), {@code false} bila dinonaktifkan
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah entity ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
	 * atau string kosong/spasi diabaikan diam-diam (lihat {@link GeneralValueObject#setOleh(String)}).
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah entity ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOleh(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilainya akan ditimpa otomatis
	 * oleh {@link #onUpdate()} pada jalur update Hibernate.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Hook siklus hidup Hibernate {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat
	 * sebelum statement {@code UPDATE} dieksekusi untuk baris ini, mendelegasikan pembaruan stempel
	 * waktu ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi teks ringkas untuk logging/debugging: {@code "<id>-<nama>"}.
	 *
	 * @return representasi teks entity ini
	 */
	public String toString() {
		return id + "-" + nama;
	}
}
