package ais.database.model;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Baris <b>penyempit (scoping)</b> untuk {@link JenisKegiatan} (entity billing pusat) — mendefinisikan
 * kombinasi fakultas/jurusan tertentu yang memakai satu {@link JenisKegiatan}, sekaligus menghubungkan
 * kombinasi itu ke daftar komponen biaya ({@link DetailBiaya}) lewat tabel pivot
 * {@code kegiatan_has_biaya}. Dengan kata lain: {@link JenisKegiatan} mendefinisikan JENIS kegiatan
 * (billing) secara umum, sedangkan baris {@code JenisKegiatanDetail} ini mengatur RINCIAN per
 * fakultas/jurusan — komponen biaya apa saja yang berlaku untuk kombinasi itu.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_kegiatan_detail")

public class JenisKegiatanDetail extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -3081613612931036389L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas "{fakultas}_{jurusan}_{jenisKegiatan}", dipakai untuk keperluan
	 *         log/debug.
	 */
	public String toString() {
		return fakultas + "_" + jurusan + "_" + jenisKegiatan;
	}

	private Fakultas fakultas;
	private Jurusan jurusan;

	private JenisKegiatan jenisKegiatan;

	private Set<DetailBiaya> detailBiayas = new HashSet<DetailBiaya>();

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public JenisKegiatanDetail() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return fakultas yang dicakup baris ini, atau {@code null} bila berlaku lintas fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return this.fakultas;
	}

	/**
	 * @param fakultas fakultas yang dicakup baris ini.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return jurusan yang dicakup baris ini, atau {@code null} bila berlaku lintas jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return this.jurusan;
	}

	/**
	 * @param jurusan jurusan yang dicakup baris ini.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @param detailBiayas himpunan komponen biaya yang berlaku untuk kombinasi fakultas/jurusan ini.
	 */
	public void setDetailBiayas(Set<DetailBiaya> detailBiayas) {
		this.detailBiayas = detailBiayas;
	}

	/**
	 * @return himpunan komponen biaya ({@link DetailBiaya}) yang berlaku untuk kombinasi
	 *         fakultas/jurusan ini, lewat tabel pivot {@code kegiatan_has_biaya}.
	 */
	@ManyToMany(targetEntity = DetailBiaya.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "kegiatan_has_biaya", joinColumns = @JoinColumn(name = "kegiatan"), inverseJoinColumns = @JoinColumn(name = "biaya"))
	public Set<DetailBiaya> getDetailBiayas() {
		return detailBiayas;
	}

	/**
	 * @param jenisKegiatan {@link JenisKegiatan} induk (entity billing pusat) yang dirinci baris ini.
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * @return {@link JenisKegiatan} induk (entity billing pusat) yang dirinci baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		return jenisKegiatan;
	}

}
