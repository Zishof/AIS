package ais.database.model.crm;

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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Aktivitas/tindak lanjut terjadwal pada satu {@link CrmLead} — mis. telepon, meeting, email,
 * tugas follow-up. Bentuk mengikuti pola {@code ais.database.model.spmi.TindakLanjutTemuanSPMI}
 * (target date + status + PIC), dengan PIC memakai FK terstruktur ke {@link Tbmuser} (bukan teks
 * bebas) supaya bisa dipakai untuk daftar "aktivitas jatuh tempo/terlambat milik saya".
 *
 * <p>Tabel {@code public.crm_activity} dibuat otomatis oleh {@code hbm2ddl=update} saat restart.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_activity")
public class CrmActivity extends GeneralValueObject {

	private static final long serialVersionUID = 3120260815007L;

	public static final String JENIS_TELEPON = "TELEPON";
	public static final String JENIS_MEETING = "MEETING";
	public static final String JENIS_EMAIL = "EMAIL";
	public static final String JENIS_TUGAS = "TUGAS";
	public static final String JENIS_LAINNYA = "LAINNYA";

	public static final Map<String, String> JENIS_DATA = new LinkedHashMap<String, String>();
	static {
		JENIS_DATA.put(JENIS_TELEPON, "Telepon");
		JENIS_DATA.put(JENIS_MEETING, "Meeting");
		JENIS_DATA.put(JENIS_EMAIL, "Email");
		JENIS_DATA.put(JENIS_TUGAS, "Tugas");
		JENIS_DATA.put(JENIS_LAINNYA, "Lainnya");
	}

	public static final String STATUS_BELUM_DIMULAI = "BELUM_DIMULAI";
	public static final String STATUS_SELESAI = "SELESAI";

	private Long id;
	private CrmLead lead;
	private String jenis;
	private String catatan;
	private Date targetDate;
	private Date tanggalSelesai;
	private String status;
	private Tbmuser picUser;
	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public CrmActivity() {
	}

	public CrmActivity(CrmLead lead) {
		this.lead = lead;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lead", nullable = false)
	public CrmLead getLead() {
		lead = check(lead);
		return lead;
	}

	public void setLead(CrmLead lead) {
		this.lead = lead;
	}

	@Column(name = "jenis", nullable = true, length = 32)
	public String getJenis() {
		return jenis == null || jenis.trim().isEmpty() ? JENIS_LAINNYA : jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	@Column(name = "catatan", nullable = true, columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "target_date", nullable = true)
	public Date getTargetDate() {
		return targetDate;
	}

	public void setTargetDate(Date targetDate) {
		this.targetDate = targetDate;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_selesai", nullable = true)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	@Column(name = "status", nullable = true, length = 32)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_BELUM_DIMULAI : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pic_user", nullable = true)
	public Tbmuser getPicUser() {
		picUser = check(picUser);
		return picUser;
	}

	public void setPicUser(Tbmuser picUser) {
		this.picUser = picUser;
	}

	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	public String toString() {
		return id + "-" + getJenis();
	}
}
