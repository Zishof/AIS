package ais.database.model.sekolah;

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




import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detail_kelompok_kegiatan_kesiswaan")



public class DetailKelompokKegiatanKesiswaan extends GeneralValueObject {
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

	private KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan;
	private String nama;
	private Integer nomorUrut;
	private Boolean aktif;
	private Boolean bisaDipilihSiswa;

	private Set<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans = new TreeSet<JabatanKegiatanKesiswaan>();

	@ManyToMany(targetEntity = JabatanKegiatanKesiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_jabatan_kegiatan_kesiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "jabatan_kegiatan_kesiswaan"))
	public Set<JabatanKegiatanKesiswaan> getJabatanKegiatanKesiswaans() {
		return jabatanKegiatanKesiswaans;
	}

	public void setJabatanKegiatanKesiswaans(Set<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans) {
		this.jabatanKegiatanKesiswaans = jabatanKegiatanKesiswaans;
	}

	private Set<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans = new TreeSet<SkalaKegiatanKesiswaan>();

	@ManyToMany(targetEntity = SkalaKegiatanKesiswaan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_skala_kegiatan_kesiswaan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "skala_kegiatan_kesiswaan"))
	public Set<SkalaKegiatanKesiswaan> getSkalaKegiatanKesiswaans() {
		return skalaKegiatanKesiswaans;
	}

	public void setSkalaKegiatanKesiswaans(Set<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans) {
		this.skalaKegiatanKesiswaans = skalaKegiatanKesiswaans;
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

	public void setKelompokKegiatanKesiswaan(KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan) {
		this.kelompokKegiatanKesiswaan = kelompokKegiatanKesiswaan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kesiswaan", nullable = true)
	public KelompokKegiatanKesiswaan getKelompokKegiatanKesiswaan() {
		kelompokKegiatanKesiswaan = check(kelompokKegiatanKesiswaan);
		return kelompokKegiatanKesiswaan;
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

	public Boolean getBisaDipilihSiswa() {
		if (kelompokKegiatanKesiswaan != null && !kelompokKegiatanKesiswaan.getBisaDipilihSiswa()) {
			bisaDipilihSiswa = false;
		}
		return bisaDipilihSiswa == null ? true : bisaDipilihSiswa;
	}

	public void setBisaDipilihSiswa(Boolean bisaDipilihSiswa) {
		this.bisaDipilihSiswa = bisaDipilihSiswa;
	}

}
