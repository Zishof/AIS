package ais.database.model.crm;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
import ais.database.model.Tbmuser;

/**
 * <h3>CrmLead — entitas inti modul CRM (Lead &amp; Peluang dalam satu tabel)</h3>
 *
 * <p>Mengikuti model CRM Odoo: Lead dan Peluang (Opportunity) adalah satu jenis data yang sama,
 * dibedakan oleh {@link #tipe} — Lead adalah prospek mentah sebelum dikualifikasi, Peluang adalah
 * prospek yang sudah dikonfirmasi layak ditindaklanjuti lewat {@link CrmStage} pipeline.</p>
 *
 * <p><b>Generik lintas domain.</b> {@link #pipelineType} menentukan konteks penggunaan (admisi
 * calon mahasiswa/siswa, kemitraan/vendor, donasi alumni, dst — dikonfigurasi admin, lihat
 * {@link CrmPipelineType}). {@link #jenisEntitasTerkait}/{@link #entitasTerkaitId} adalah pranala
 * polimorfik OPSIONAL ke record asal (mis. CalonMahasiswa/CalonSiswa) — memakai pola field string
 * (bukan FK keras), sama seperti {@code ais.database.model.ticket.Ticket#pengajuUserId}/
 * {@code pengajuTipe}, supaya satu entity ini bisa dipakai lintas domain tanpa FK ke banyak tabel
 * berbeda.</p>
 *
 * <p>Tabel {@code public.crm_lead} dibuat otomatis oleh {@code hbm2ddl=update} saat restart.
 * Di-audit Envers untuk riwayat/recovery.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_lead")
public class CrmLead extends GeneralValueObject {

	private static final long serialVersionUID = 3120260815006L;

	/** Tipe data: prospek mentah, atau sudah dikualifikasi jadi peluang. */
	public static final String TIPE_LEAD = "LEAD";
	public static final String TIPE_PELUANG = "PELUANG";

	/** Status menang/kalah pipeline. */
	public static final String STATUS_OPEN = "OPEN";
	public static final String STATUS_WON = "WON";
	public static final String STATUS_LOST = "LOST";

	/** Jenis entitas terkait yang umum dipakai — bebas diisi nilai lain (LAINNYA) oleh UI. */
	public static final String ENTITAS_ADMISI_MAHASISWA = "ADMISI_MAHASISWA";
	public static final String ENTITAS_ADMISI_SISWA = "ADMISI_SISWA";
	public static final String ENTITAS_MITRA_VENDOR = "MITRA_VENDOR";
	public static final String ENTITAS_ALUMNI_DONATUR = "ALUMNI_DONATUR";
	public static final String ENTITAS_LAINNYA = "LAINNYA";

	private Long id;
	private String tipe;
	private CrmPipelineType pipelineType;
	private CrmStage stage;

	private String judul;
	private String kontakNama;
	private String kontakEmail;
	private String kontakTelepon;
	private String kontakInstansi;

	private String jenisEntitasTerkait;
	private Long entitasTerkaitId;

	private String sumber;

	private CrmSalesTeam salesTeam;
	private Tbmuser ditugaskanUser;

	private BigDecimal nilaiEstimasi;
	private Integer probabilitas;

	private Date tanggalTutupDiharapkan;
	private String statusMenangKalah;
	private CrmLostReason lostReason;
	private String catatanKalah;

	private Date tanggalDibuat = ais.ui.util.WaktuUtil.getDate();
	private Date tanggalDikonversiPeluang;
	private Date tanggalDitutup;

	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public CrmLead() {
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

	@Column(name = "tipe", nullable = false, length = 32)
	public String getTipe() {
		return tipe == null || tipe.trim().isEmpty() ? TIPE_LEAD : tipe;
	}

	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pipeline_type", nullable = false)
	public CrmPipelineType getPipelineType() {
		return pipelineType;
	}

	public void setPipelineType(CrmPipelineType pipelineType) {
		this.pipelineType = pipelineType;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "stage", nullable = true)
	public CrmStage getStage() {
		return stage;
	}

	public void setStage(CrmStage stage) {
		this.stage = stage;
	}

	@Column(name = "judul", nullable = false, columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	public void setJudul(String judul) {
		this.judul = judul;
	}

	/** Alias agar kompatibel dengan getNama() milik GeneralValueObject (dipakai combo/label umum). */
	public String getNama() {
		return judul;
	}

	@Column(name = "kontak_nama", nullable = true, length = 255)
	public String getKontakNama() {
		return kontakNama;
	}

	public void setKontakNama(String kontakNama) {
		this.kontakNama = kontakNama;
	}

	@Column(name = "kontak_email", nullable = true, length = 255)
	public String getKontakEmail() {
		return kontakEmail;
	}

	public void setKontakEmail(String kontakEmail) {
		this.kontakEmail = kontakEmail;
	}

	@Column(name = "kontak_telepon", nullable = true, length = 64)
	public String getKontakTelepon() {
		return kontakTelepon;
	}

	public void setKontakTelepon(String kontakTelepon) {
		this.kontakTelepon = kontakTelepon;
	}

	@Column(name = "kontak_instansi", nullable = true, length = 255)
	public String getKontakInstansi() {
		return kontakInstansi;
	}

	public void setKontakInstansi(String kontakInstansi) {
		this.kontakInstansi = kontakInstansi;
	}

	@Column(name = "jenis_entitas_terkait", nullable = true, length = 64)
	public String getJenisEntitasTerkait() {
		return jenisEntitasTerkait;
	}

	public void setJenisEntitasTerkait(String jenisEntitasTerkait) {
		this.jenisEntitasTerkait = jenisEntitasTerkait;
	}

	@Column(name = "entitas_terkait_id", nullable = true)
	public Long getEntitasTerkaitId() {
		return entitasTerkaitId;
	}

	public void setEntitasTerkaitId(Long entitasTerkaitId) {
		this.entitasTerkaitId = entitasTerkaitId;
	}

	@Column(name = "sumber", nullable = true, length = 255)
	public String getSumber() {
		return sumber;
	}

	public void setSumber(String sumber) {
		this.sumber = sumber;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_team", nullable = true)
	public CrmSalesTeam getSalesTeam() {
		return salesTeam;
	}

	public void setSalesTeam(CrmSalesTeam salesTeam) {
		this.salesTeam = salesTeam;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ditugaskan_user", nullable = true)
	public Tbmuser getDitugaskanUser() {
		return ditugaskanUser;
	}

	public void setDitugaskanUser(Tbmuser ditugaskanUser) {
		this.ditugaskanUser = ditugaskanUser;
	}

	@Column(name = "nilai_estimasi", nullable = true)
	public BigDecimal getNilaiEstimasi() {
		return nilaiEstimasi;
	}

	public void setNilaiEstimasi(BigDecimal nilaiEstimasi) {
		this.nilaiEstimasi = nilaiEstimasi;
	}

	@Column(name = "probabilitas", nullable = true)
	public Integer getProbabilitas() {
		return probabilitas == null ? 0 : probabilitas;
	}

	public void setProbabilitas(Integer probabilitas) {
		this.probabilitas = probabilitas;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_tutup_diharapkan", nullable = true)
	public Date getTanggalTutupDiharapkan() {
		return tanggalTutupDiharapkan;
	}

	public void setTanggalTutupDiharapkan(Date tanggalTutupDiharapkan) {
		this.tanggalTutupDiharapkan = tanggalTutupDiharapkan;
	}

	@Column(name = "status_menang_kalah", nullable = true, length = 16)
	public String getStatusMenangKalah() {
		return statusMenangKalah == null || statusMenangKalah.trim().isEmpty() ? STATUS_OPEN : statusMenangKalah;
	}

	public void setStatusMenangKalah(String statusMenangKalah) {
		this.statusMenangKalah = statusMenangKalah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lost_reason", nullable = true)
	public CrmLostReason getLostReason() {
		return lostReason;
	}

	public void setLostReason(CrmLostReason lostReason) {
		this.lostReason = lostReason;
	}

	@Column(name = "catatan_kalah", nullable = true, columnDefinition = "text")
	public String getCatatanKalah() {
		return catatanKalah;
	}

	public void setCatatanKalah(String catatanKalah) {
		this.catatanKalah = catatanKalah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dibuat", nullable = true)
	public Date getTanggalDibuat() {
		return tanggalDibuat;
	}

	public void setTanggalDibuat(Date tanggalDibuat) {
		this.tanggalDibuat = tanggalDibuat;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dikonversi_peluang", nullable = true)
	public Date getTanggalDikonversiPeluang() {
		return tanggalDikonversiPeluang;
	}

	public void setTanggalDikonversiPeluang(Date tanggalDikonversiPeluang) {
		this.tanggalDikonversiPeluang = tanggalDikonversiPeluang;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditutup", nullable = true)
	public Date getTanggalDitutup() {
		return tanggalDitutup;
	}

	public void setTanggalDitutup(Date tanggalDitutup) {
		this.tanggalDitutup = tanggalDitutup;
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
		return id + "-" + judul;
	}
}
