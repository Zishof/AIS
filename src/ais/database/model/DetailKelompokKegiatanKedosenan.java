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

import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.detail_kelompok_kegiatan_kedosenan}
 * pada modul kegiatan kedosenan (kinerja/aktivitas dosen, mis. untuk perhitungan
 * beban kerja/angka kredit). Merepresentasikan satu butir detail/rincian di
 * bawah satu {@link KelompokKegiatanKedosenan} (kelompok kegiatan induk) —
 * mis. satu jenis kegiatan spesifik dalam kelompok "Pendidikan &amp; Pengajaran".
 *
 * <p>
 * Berelasi many-to-many ke {@link JabatanKegiatanKedosenan} (jabatan fungsional
 * dosen yang boleh memilih detail ini, lewat tabel penghubung
 * {@code detail_kelompok_has_jabatan_kegiatan_kedosenan}) dan {@link
 * SkalaKegiatanKedosenan} (skala/tingkat kegiatan, lewat tabel penghubung
 * {@code detail_kelompok_has_skala_kegiatan_kedosenan}). Flag {@code
 * bisaDipilihDosen} otomatis bernilai {@code false} bila kelompok induknya
 * ({@link KelompokKegiatanKedosenan}) sudah tidak bisa dipilih dosen — lihat
 * {@link #getBisaDipilihDosen()}. Diaudit lewat Hibernate Envers
 * ({@code @Audited}).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detail_kelompok_kegiatan_kedosenan")

public class DetailKelompokKegiatanKedosenan extends GeneralValueObject {
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

	private KelompokKegiatanKedosenan kelompokKegiatanKedosenan;
	private String nama;
	private Integer nomorUrut;
	private Boolean aktif;
	private Boolean bisaDipilihDosen;

	private Set<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans = new TreeSet<JabatanKegiatanKedosenan>();

	@ManyToMany(targetEntity = JabatanKegiatanKedosenan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_jabatan_kegiatan_kedosenan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "jabatan_kegiatan_kedosenan"))
	public Set<JabatanKegiatanKedosenan> getJabatanKegiatanKedosenans() {
		return jabatanKegiatanKedosenans;
	}

	public void setJabatanKegiatanKedosenans(Set<JabatanKegiatanKedosenan> jabatanKegiatanKedosenans) {
		this.jabatanKegiatanKedosenans = jabatanKegiatanKedosenans;
	}

	private Set<SkalaKegiatanKedosenan> skalaKegiatanKedosenans = new TreeSet<SkalaKegiatanKedosenan>();

	@ManyToMany(targetEntity = SkalaKegiatanKedosenan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@JoinTable(name = "detail_kelompok_has_skala_kegiatan_kedosenan", joinColumns = @JoinColumn(name = "detail_kelompok"), inverseJoinColumns = @JoinColumn(name = "skala_kegiatan_kedosenan"))
	public Set<SkalaKegiatanKedosenan> getSkalaKegiatanKedosenans() {
		return skalaKegiatanKedosenans;
	}

	public void setSkalaKegiatanKedosenans(Set<SkalaKegiatanKedosenan> skalaKegiatanKedosenans) {
		this.skalaKegiatanKedosenans = skalaKegiatanKedosenans;
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

	public void setKelompokKegiatanKedosenan(KelompokKegiatanKedosenan kelompokKegiatanKedosenan) {
		this.kelompokKegiatanKedosenan = kelompokKegiatanKedosenan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kedosenan", nullable = true)
	public KelompokKegiatanKedosenan getKelompokKegiatanKedosenan() {
		kelompokKegiatanKedosenan = check(kelompokKegiatanKedosenan);
		return kelompokKegiatanKedosenan;
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

	/** Default {@code true}, tapi dipaksa {@code false} bila kelompok induk ({@link #getKelompokKegiatanKedosenan()}) juga tidak bisa dipilih dosen. */
	public Boolean getBisaDipilihDosen() {
		if (kelompokKegiatanKedosenan != null && !kelompokKegiatanKedosenan.getBisaDipilihDosen()) {
			bisaDipilihDosen = false;
		}
		return bisaDipilihDosen == null ? true : bisaDipilihDosen;
	}

	public void setBisaDipilihDosen(Boolean bisaDipilihDosen) {
		this.bisaDipilihDosen = bisaDipilihDosen;
	}

}
