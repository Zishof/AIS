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
 * <b>AkunManajemen</b> -- akun pengguna modul Manajemen (SDM/Payroll/Logistik/Surat
 * Menyurat/Workflow/Akunting/Finance/Aset & Inventaris/Produksi/Ekspedisi/Pelacakan
 * Kendaraan/Antar Jemput/Audit & Pengawasan Internal, dst) milik satu {@link Pendaftar}.
 * {@code jabatan} sekadar label bebas tahap ini (mis. "HRD", "Finance") -- pemetaan ke
 * modul/hak-akses spesifik menyusul saat konten tiap modul Manajemen dibangun.
 *
 * <p>Login plaintext, konsisten dgn {@link Investor} dan {@code inventory.Pedagang} -- lihat
 * JavaDoc {@link Investor} utk alasan lengkap.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "akun_manajemen")
public class AkunManajemen extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String nama;
	private String jabatan;
	private Pendaftar pendaftar;
	private String userid;
	private String pass;
	private Boolean aktif;
	private Date dibuatPada;

	public AkunManajemen() {
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

	@Column(name = "jabatan", nullable = true, length = 100)
	public String getJabatan() {
		return jabatan;
	}

	public void setJabatan(String jabatan) {
		this.jabatan = jabatan;
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
