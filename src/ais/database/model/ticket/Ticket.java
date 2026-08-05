package ais.database.model.ticket;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * <h3>Ticket — entitas utama modul Ticketing Management</h3>
 *
 * <p>Tiket mencatat monitoring pengembangan &amp; implementasi sistem: progress develop, kendala,
 * permintaan modul baru, dan interaksi (pesan) antara developer↔pengguna maupun pengguna↔pengguna.
 * Bersifat GENERAL — pengaju bisa pegawai/dosen/guru/siswa/mahasiswa/calon/orang tua/vendor/umum
 * (disimpan via {@link #pengajuUserId}/{@link #pengajuTipe}, plus email/HP untuk pengaju eksternal).</p>
 *
 * <p><b>Workflow.</b> {@code extends DataSop} → tiket diajukan lewat SOP/disposisi yang sudah
 * berjalan; {@link #disposisiSop} menautkan tiket ke alur persetujuannya.</p>
 *
 * <p><b>Scoping.</b> {@link #satuanKerja} + {@link #hakAksesTarget} (CSV roleId) dipakai membatasi
 * visibilitas tiket sesuai posisi pengguna; admin/developer melihat semua.</p>
 *
 * <p>Tabel {@code public.ticket} dibuat otomatis oleh {@code hbm2ddl=update} saat restart. Nama
 * kolom snake_case, semua FK nullable agar aman. Di-audit Envers untuk riwayat/recovery.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ticket")
public class Ticket extends DataSop {

	private static final long serialVersionUID = 3120250724002L;

	/** Status siklus hidup tiket. */
	public static final String STATUS_BARU = "Baru";
	public static final String STATUS_DITINJAU = "Ditinjau";
	public static final String STATUS_DIPROSES = "Diproses";
	public static final String STATUS_MENUNGGU_RESPON = "Menunggu Respon";
	public static final String STATUS_SELESAI = "Selesai";
	public static final String STATUS_DITUTUP = "Ditutup";
	public static final String STATUS_DITOLAK = "Ditolak";

	/** Tipe/jenis tiket. */
	public static final String TIPE_KENDALA = "Kendala / Bug";
	public static final String TIPE_PERMINTAAN = "Permintaan Modul Baru";
	public static final String TIPE_PROGRESS = "Progress Development";
	public static final String TIPE_INTERAKSI = "Pertanyaan / Interaksi";
	public static final String TIPE_LAINNYA = "Lainnya";

	/** Prioritas. */
	public static final String PRIORITAS_RENDAH = "Rendah";
	public static final String PRIORITAS_SEDANG = "Sedang";
	public static final String PRIORITAS_TINGGI = "Tinggi";
	public static final String PRIORITAS_KRITIS = "Kritis";

	private Long id;
	private String nomorTiket;
	private String judul;
	private String deskripsi;
	private TicketKategori ticketKategori;
	private String tipe;
	private String prioritas;
	private String status;
	private String modul;
	private Integer progress;

	// Pengaju (general lintas jenis pengguna)
	private String pengajuUserId;
	private String pengajuNama;
	private String pengajuTipe;
	private String pengajuEmail;
	private String pengajuHp;

	// Scoping posisi pengguna
	private SatuanKerja satuanKerja;
	private String hakAksesTarget;

	// Penanganan
	private String ditugaskanKeUserId;
	private String ditugaskanKeNama;

	// Workflow SOP
	private DisposisiSop disposisiSop;

	// Waktu
	private Date tanggalDibuat = ais.ui.util.WaktuUtil.getDate();
	private Date tanggalTarget;
	private Date tanggalSelesai;

	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public Ticket() {
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

	@Column(name = "nomor_tiket", nullable = true, length = 64)
	public String getNomorTiket() {
		return nomorTiket;
	}

	public void setNomorTiket(String nomorTiket) {
		this.nomorTiket = nomorTiket;
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

	@Column(name = "deskripsi", nullable = true, columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi;
	}

	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "ticket_kategori", nullable = true)
	public TicketKategori getTicketKategori() {
		return ticketKategori;
	}

	public void setTicketKategori(TicketKategori ticketKategori) {
		this.ticketKategori = ticketKategori;
	}

	@Column(name = "tipe", nullable = true, length = 64)
	public String getTipe() {
		return tipe;
	}

	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	@Column(name = "prioritas", nullable = true, length = 32)
	public String getPrioritas() {
		return prioritas == null || prioritas.trim().isEmpty() ? PRIORITAS_SEDANG : prioritas;
	}

	public void setPrioritas(String prioritas) {
		this.prioritas = prioritas;
	}

	@Column(name = "status", nullable = true, length = 32)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_BARU : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "modul", nullable = true, length = 255)
	public String getModul() {
		return modul;
	}

	public void setModul(String modul) {
		this.modul = modul;
	}

	@Column(name = "progress", nullable = true)
	public Integer getProgress() {
		return progress == null ? 0 : progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}

	@Column(name = "pengaju_user_id", nullable = true, length = 128)
	public String getPengajuUserId() {
		return pengajuUserId;
	}

	public void setPengajuUserId(String pengajuUserId) {
		this.pengajuUserId = pengajuUserId;
	}

	@Column(name = "pengaju_nama", nullable = true, length = 255)
	public String getPengajuNama() {
		return pengajuNama;
	}

	public void setPengajuNama(String pengajuNama) {
		this.pengajuNama = pengajuNama;
	}

	@Column(name = "pengaju_tipe", nullable = true, length = 64)
	public String getPengajuTipe() {
		return pengajuTipe;
	}

	public void setPengajuTipe(String pengajuTipe) {
		this.pengajuTipe = pengajuTipe;
	}

	@Column(name = "pengaju_email", nullable = true, length = 255)
	public String getPengajuEmail() {
		return pengajuEmail;
	}

	public void setPengajuEmail(String pengajuEmail) {
		this.pengajuEmail = pengajuEmail;
	}

	@Column(name = "pengaju_hp", nullable = true, length = 64)
	public String getPengajuHp() {
		return pengajuHp;
	}

	public void setPengajuHp(String pengajuHp) {
		this.pengajuHp = pengajuHp;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@Column(name = "hak_akses_target", nullable = true, columnDefinition = "text")
	public String getHakAksesTarget() {
		return hakAksesTarget;
	}

	public void setHakAksesTarget(String hakAksesTarget) {
		this.hakAksesTarget = hakAksesTarget;
	}

	@Column(name = "ditugaskan_ke_user_id", nullable = true, length = 128)
	public String getDitugaskanKeUserId() {
		return ditugaskanKeUserId;
	}

	public void setDitugaskanKeUserId(String ditugaskanKeUserId) {
		this.ditugaskanKeUserId = ditugaskanKeUserId;
	}

	@Column(name = "ditugaskan_ke_nama", nullable = true, length = 255)
	public String getDitugaskanKeNama() {
		return ditugaskanKeNama;
	}

	public void setDitugaskanKeNama(String ditugaskanKeNama) {
		this.ditugaskanKeNama = ditugaskanKeNama;
	}

	@Override
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		return disposisiSop;
	}

	@Override
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		this.disposisiSop = disposisiSop;
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
	@Column(name = "tanggal_target", nullable = true)
	public Date getTanggalTarget() {
		return tanggalTarget;
	}

	public void setTanggalTarget(Date tanggalTarget) {
		this.tanggalTarget = tanggalTarget;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_selesai", nullable = true)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
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
