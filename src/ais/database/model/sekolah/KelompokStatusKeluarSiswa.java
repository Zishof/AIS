package ais.database.model.sekolah;

// Dibuat mengikuti pola KelompokStatusKeluarMahasiswa, namun untuk Siswa (tabel berbeda).

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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;

/**
 * <h3>Kelompok Status Keluar Siswa</h3>
 *
 * <p>Entitas ini merupakan padanan {@code KelompokStatusKeluarMahasiswa} untuk sisi Siswa.
 * Fungsinya sebagai <b>wadah/kelompok</b> untuk memberi status keluar (mis. Lulus, Dikeluarkan,
 * Pindah) kepada banyak siswa sekaligus dalam satu tahun akademik &amp; semester, lengkap dengan
 * tanggal lulus/keluar dan keterangan. Siswa dikaitkan ke kelompok melalui kolom
 * {@code kelompok_status_keluar_siswa} pada tabel {@code sekolah.siswa} (relasi
 * {@code Siswa.getKelompokStatusKeluarSiswa()}).</p>
 *
 * <p><b>Perbedaan dengan versi Mahasiswa.</b> Skema tabel berada di schema {@code sekolah}
 * (nama tabel {@code kelompok_status_keluar_siswa}) dan jenis statusnya mengacu ke
 * {@link StatusKeluarSiswa} (bukan {@code StatusKeluar} milik Mahasiswa). Entitas ini sengaja
 * <b>tidak</b> di-{@code @Audited} agar tidak memerlukan tabel _aud tambahan (aplikasi memakai
 * {@code hbm2ddl.auto=none}); relasi dari {@code Siswa} (yang ter-audit) ke entitas ini ditandai
 * {@code @NotAudited} supaya envers tidak mempersoalkannya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "sekolah", name = "kelompok_status_keluar_siswa")
public class KelompokStatusKeluarSiswa extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439809L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
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

	public String toString() {
		statusKeluar = getStatusKeluar();
		return id + "-" + statusKeluar;
	}

	private String nama;
	private String keterangan;
	private StatusKeluarSiswa statusKeluar;
	private Date tanggalLulus;
	private String tahunAkademik;
	private String semester;

	public KelompokStatusKeluarSiswa() {
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

	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_keluar_siswa", nullable = false)
	public StatusKeluarSiswa getStatusKeluar() {
		statusKeluar = check(statusKeluar);
		return statusKeluar;
	}

	public void setStatusKeluar(StatusKeluarSiswa statusKeluar) {
		this.statusKeluar = statusKeluar;
	}

	@Column(name = "tahun_akademik", nullable = false)
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	@Column(name = "semester", nullable = false)
	public String getSemester() {
		return semester == null ? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP) : semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalLulus() {
		return tanggalLulus;
	}

	public void setTanggalLulus(Date tanggalLulus) {
		this.tanggalLulus = tanggalLulus;
	}

}
