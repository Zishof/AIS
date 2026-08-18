package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Hibernate;
import org.hibernate.envers.Audited;

@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detail_kelompok_kegiatan_kemahasiswaan")

public class DetailKelompokKegiatanKemahasiswaan extends GeneralValueObject {
	private static final long serialVersionUID = -7050166125892447098L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return id + "-" + nama;
	}

	private KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan;
	private String nama;
	private Integer nomorUrut;
	private Boolean aktif;
	private Boolean bisaDipilihMahasiswa;

	private Set<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans = new TreeSet<JabatanKegiatanKemahasiswaan>();

	@ManyToMany(targetEntity = JabatanKegiatanKemahasiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_jabatan_kegiatan_kemahasiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "jabatan_kegiatan_kemahasiswaan"))
	public Set<JabatanKegiatanKemahasiswaan> getJabatanKegiatanKemahasiswaans() {
		return jabatanKegiatanKemahasiswaans;
	}

	public void setJabatanKegiatanKemahasiswaans(Set<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans) {
		this.jabatanKegiatanKemahasiswaans = jabatanKegiatanKemahasiswaans;
	}

	private Set<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans = new TreeSet<SkalaKegiatanKemahasiswaan>();

	@ManyToMany(targetEntity = SkalaKegiatanKemahasiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_skala_kegiatan_kemahasiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "skala_kegiatan_kemahasiswaan"))
	public Set<SkalaKegiatanKemahasiswaan> getSkalaKegiatanKemahasiswaans() {
		return skalaKegiatanKemahasiswaans;
	}

	public void setSkalaKegiatanKemahasiswaans(Set<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans) {
		this.skalaKegiatanKemahasiswaans = skalaKegiatanKemahasiswaans;
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

	@Column(columnDefinition = "text")
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public void setKelompokKegiatanKemahasiswaan(KelompokKegiatanKemahasiswaan kelompokKegiatanKemahasiswaan) {
		this.kelompokKegiatanKemahasiswaan = kelompokKegiatanKemahasiswaan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kemahasiswaan", nullable = true)
	public KelompokKegiatanKemahasiswaan getKelompokKegiatanKemahasiswaan() {
		kelompokKegiatanKemahasiswaan = check(kelompokKegiatanKemahasiswaan);
		return kelompokKegiatanKemahasiswaan;
	}

	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public Boolean getBisaDipilihMahasiswa() {
		// FIX LazyInitializationException: kelompokKegiatanKemahasiswaan bisa berupa
		// proxy Hibernate lazy yang belum di-initialize (mis. diakses dari thread
		// report/background setelah sesi tertutup, lihat ManajemenProperty.safeToString
		// utk pola serupa). Jangan panggil getter proxy sebelum cek isInitialized,
		// karena itu akan memicu load & melempar LazyInitializationException.
		if (kelompokKegiatanKemahasiswaan != null && Hibernate.isInitialized(kelompokKegiatanKemahasiswaan)
				&& !kelompokKegiatanKemahasiswaan.getBisaDipilihMahasiswa()) {
			bisaDipilihMahasiswa = false;
		}
		return bisaDipilihMahasiswa == null ? true : bisaDipilihMahasiswa;
	}

	public void setBisaDipilihMahasiswa(Boolean bisaDipilihMahasiswa) {
		this.bisaDipilihMahasiswa = bisaDipilihMahasiswa;
	}

}
