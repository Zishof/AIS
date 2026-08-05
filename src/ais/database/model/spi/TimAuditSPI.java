package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * <h2>TimAuditSPI &mdash; Keanggotaan Tim Audit pada Satu Penugasan</h2>
 *
 * <p>
 * Kelas penghubung (relasi many-to-many terstruktur) antara satu {@link PenugasanAuditSPI} dan
 * para pengguna ({@link Tbmuser}) yang ditugaskan sebagai auditor pada penugasan tersebut. Satu
 * baris di sini berarti "pengguna X ditugaskan pada penugasan audit Y dengan peran Z". Kelas ini
 * SENGAJA menggantikan pendekatan kolom teks bebas "Nama Auditor" (yang sempat dipakai di draf
 * awal desain modul ini, meniru pola {@code auditorNama} pada modul SPMI) karena kolom teks bebas
 * tidak bisa direkap/dianalisis secara terstruktur (mis. "berapa kali staf X pernah bertugas
 * sebagai auditor tahun ini", kebutuhan umum untuk menjaga rotasi &amp; independensi auditor), dan
 * rawan salah ketik/tidak konsisten antar penugasan.
 * </p>
 *
 * <h3>Dua peran dalam satu tim: Ketua dan Anggota</h3>
 * <p>
 * {@link #getPeranTim()} membedakan {@link #KETUA_TIM} (memimpin &amp; bertanggung jawab penuh atas
 * penugasan, biasanya satu orang per penugasan) dari {@link #ANGGOTA_TIM} (anggota pendukung,
 * bisa lebih dari satu). Pembedaan ini penting bagi pelaporan SPI ke pimpinan (mis. daftar "siapa
 * saja yang pernah menjadi Ketua Tim" sebagai indikator kematangan/kompetensi staf) dan bagi
 * tanggung jawab dokumen (Ketua Tim yang lazimnya menandatangani laporan hasil audit).
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tim_audit_spi")
public class TimAuditSPI extends GeneralValueObject {

	public static final String KETUA_TIM = "KETUA_TIM";
	public static final String ANGGOTA_TIM = "ANGGOTA_TIM";

	public static final Map<String, String> PERAN_TIM_DATA = new LinkedHashMap<String, String>();
	static {
		PERAN_TIM_DATA.put(KETUA_TIM, "Ketua Tim");
		PERAN_TIM_DATA.put(ANGGOTA_TIM, "Anggota Tim");
	}

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return (anggota == null ? "-" : anggota.getUserNama()) + " (" + getPeranTimLabel() + ")";
	}

	private PenugasanAuditSPI penugasanAuditSPI;
	private Tbmuser anggota;
	private String peranTim;
	private Boolean aktif;

	public TimAuditSPI() {
	}

	public TimAuditSPI(PenugasanAuditSPI penugasanAuditSPI) {
		this.penugasanAuditSPI = penugasanAuditSPI;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penugasan_audit_spi", nullable = false)
	public PenugasanAuditSPI getPenugasanAuditSPI() {
		penugasanAuditSPI = check(penugasanAuditSPI);
		return penugasanAuditSPI;
	}

	public void setPenugasanAuditSPI(PenugasanAuditSPI penugasanAuditSPI) {
		this.penugasanAuditSPI = penugasanAuditSPI;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota", nullable = false)
	public Tbmuser getAnggota() {
		anggota = check(anggota);
		return anggota;
	}

	public void setAnggota(Tbmuser anggota) {
		this.anggota = anggota;
	}

	@Column(name = "peran_tim", nullable = false, length = 20)
	public String getPeranTim() {
		return peranTim == null ? ANGGOTA_TIM : peranTim;
	}

	public void setPeranTim(String peranTim) {
		this.peranTim = peranTim;
	}

	/** Label bahasa manusia dari {@link #getPeranTim()}, dipakai langsung oleh tampilan. */
	@Transient
	public String getPeranTimLabel() {
		String label = PERAN_TIM_DATA.get(getPeranTim());
		return label == null ? getPeranTim() : label;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
