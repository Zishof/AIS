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

import ais.database.model.GeneralValueObject;

/**
 * Komentar / pesan pada sebuah {@link Ticket} — thread interaksi antara developer↔pengguna maupun
 * pengguna↔pengguna. {@link #internal} = true menandai catatan yang hanya boleh dilihat pengelola
 * (developer/admin), bukan pengaju. Lampiran pesan memakai {@code LampiranLain} (jenis
 * "TICKET_KOMENTAR", ref = id komentar). Tabel {@code public.ticket_komentar} dibuat otomatis
 * ({@code hbm2ddl=update}).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ticket_komentar")
public class TicketKomentar extends GeneralValueObject {

	private static final long serialVersionUID = 3120250724003L;

	private Long id;
	private Ticket ticket;
	private String isi;
	private String userId;
	private String nama;
	private String tipePengguna;
	private Boolean internal;
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public TicketKomentar() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "ticket", nullable = true)
	public Ticket getTicket() {
		return ticket;
	}

	public void setTicket(Ticket ticket) {
		this.ticket = ticket;
	}

	@Column(name = "isi", nullable = true, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	public void setIsi(String isi) {
		this.isi = isi;
	}

	@Column(name = "user_id", nullable = true, length = 128)
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "tipe_pengguna", nullable = true, length = 64)
	public String getTipePengguna() {
		return tipePengguna;
	}

	public void setTipePengguna(String tipePengguna) {
		this.tipePengguna = tipePengguna;
	}

	@Column(name = "internal", nullable = true)
	public Boolean getInternal() {
		return internal == null ? false : internal;
	}

	public void setInternal(Boolean internal) {
		this.internal = internal;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = true)
	public Date getTanggal() {
		return tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
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
		return id + "-" + (nama == null ? "" : nama);
	}
}
