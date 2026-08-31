package ais.database.model;

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

import org.hibernate.envers.Audited;

/**
 * Model data untuk ruang paket pmb. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code RuangPMB ruangPMB}, {@code BiodataCalonMahasiswa
 * biodataCalonMahasiswa}, {@code String kodeUnik}; pemetaan persistence: tabel {@code public.ruang_paket_pmb};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()},
 * {@code getRuangPMB()}, {@code getBiodataCalonMahasiswa()}); mutasi data ({@code setOlehId()}, {@code
 * setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()}, {@code setRuangPMB()}); operasi
 * domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ruang_paket_pmb")
public class RuangPaketPMB extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8522391894818139048L;

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
		return biodataCalonMahasiswa.getNama();
	}

	private RuangPMB ruangPMB;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private String kodeUnik;

	public RuangPaketPMB() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_pmb")
	public RuangPMB getRuangPMB() {
		ruangPMB = check(ruangPMB);
		return ruangPMB;
	}

	public void setRuangPMB(RuangPMB ruangPMB) {
		this.ruangPMB = ruangPMB;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_mahasiswa")
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	@Column(name = "kode_unik", unique = true)
	public String getKodeUnik() {

		ruangPMB = getRuangPMB();
		biodataCalonMahasiswa = getBiodataCalonMahasiswa();

		if (ruangPMB == null || biodataCalonMahasiswa == null) return kodeUnik;
		kodeUnik = ruangPMB.getPaket() == null || biodataCalonMahasiswa.getPaket() == null
				|| ruangPMB.getPaket().getId().equals(biodataCalonMahasiswa.getPaket().getId())
						? biodataCalonMahasiswa.getId() + "_"
						: "";
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
