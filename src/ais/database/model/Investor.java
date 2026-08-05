package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

/**
 * <b>Investor</b> -- pemilik modal pada satu atau lebih toko/brand milik satu {@link Pendaftar}
 * ebisnis.id. Satu Pendaftar boleh punya banyak Investor; satu Investor bisa memegang persentase
 * kepemilikan di lebih dari satu toko/brand -- disimpan sbg {@code kepemilikanJson} (array
 * {@code [{tokoId, persentase}]}) drpd tabel relasi terpisah, supaya iterasi pertama ini tetap
 * ringkas (lihat catatan di {@code PendaftarDashboardHelper}; normalisasi ke tabel sendiri bisa
 * menyusul kalau kebutuhan laporan bagi-hasil sudah lebih jelas).
 *
 * <p>Login memakai kredensial plaintext ({@code userid}/{@code pass}) -- KONSISTEN dgn skema
 * {@code ais.database.model.inventory.Pedagang} yang sudah dipakai seluruh ekosistem POS (akun
 * dibuat oleh pemilik bisnis/Pendaftar, bukan pendaftaran mandiri publik spt {@link Pendaftar}
 * sendiri yang pakai hash PBKDF2 -- beda tingkat kepercayaan, lihat JavaDoc
 * {@code PendaftarPublicHelper}). Dipilih supaya QR-login bisa langsung meng-encode
 * {@code userid:pass} tanpa mekanisme token terpisah.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "investor")
public class Investor extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String nama;
	private String email;
	private String telp;
	private Pendaftar pendaftar;
	private String userid;
	private String pass;
	private String kepemilikanJson;
	private Boolean aktif;
	private Date dibuatPada;

	public Investor() {
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

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() {
		return telp;
	}

	public void setTelp(String telp) {
		this.telp = telp;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	@Column(name = "userid", unique = true, nullable = false, length = 100)
	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	@Column(name = "pass", nullable = false, length = 100)
	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	/** JSON array teks: {@code [{"tokoId":1,"persentase":30.0}, ...]} -- lihat JavaDoc kelas. */
	@Column(name = "kepemilikan_json", nullable = true, columnDefinition = "text")
	public String getKepemilikanJson() {
		return kepemilikanJson;
	}

	public void setKepemilikanJson(String kepemilikanJson) {
		this.kepemilikanJson = kepemilikanJson;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "dibuat_pada")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}

	public String toString() {
		return id + "-" + nama;
	}
}
