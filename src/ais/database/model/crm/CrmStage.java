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

	private Long id;
	private CrmPipelineType pipelineType;
	private String nama;
	private Integer nomorUrut;
	private Integer probabilitasDefault;
	private Boolean isWon;
	private Boolean isLost;
	private String warna;
	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public CrmStage() {
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
	@JoinColumn(name = "pipeline_type", nullable = false)
	public CrmPipelineType getPipelineType() {
		pipelineType = check(pipelineType);
		return pipelineType;
	}

	public void setPipelineType(CrmPipelineType pipelineType) {
		this.pipelineType = pipelineType;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "nomor_urut", nullable = true)
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	@Column(name = "probabilitas_default", nullable = true)
	public Integer getProbabilitasDefault() {
		return probabilitasDefault == null ? 0 : probabilitasDefault;
	}

	public void setProbabilitasDefault(Integer probabilitasDefault) {
		this.probabilitasDefault = probabilitasDefault;
	}

	@Column(name = "is_won", nullable = true)
	public Boolean getIsWon() {
		return isWon == null ? false : isWon;
	}

	public void setIsWon(Boolean isWon) {
		this.isWon = isWon;
	}

	@Column(name = "is_lost", nullable = true)
	public Boolean getIsLost() {
		return isLost == null ? false : isLost;
	}

	public void setIsLost(Boolean isLost) {
		this.isLost = isLost;
	}

	@Column(name = "warna", nullable = true, length = 32)
	public String getWarna() {
		return warna;
	}

	public void setWarna(String warna) {
		this.warna = warna;
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
		return id + "-" + nama;
	}
}
