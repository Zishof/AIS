package ais.database.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Model data untuk pilihan paket per jurusan mhs baru. Tipe ini membawa state yang dipertukarkan
 * oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta
 * relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru},
 * {@code Paket paket}; pemetaan persistence: tabel {@code public.pilihan_paket_per_jurusan_mhs_baru};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()},
 * {@code getJurusanSekolahMahasiswaBaru()}, {@code getPaket()}); mutasi data ({@code setOlehId()}, {@code
 * setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()}, {@code
 * setJurusanSekolahMahasiswaBaru()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pilihan_paket_per_jurusan_mhs_baru")
public class PilihanPaketPerJurusanMhsBaru extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6643595824435487694L;

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
		getPaket();
		getJurusanSekolahMahasiswaBaru();
		return jurusanSekolahMahasiswaBaru + "_" + paket;
	}

	private JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru;
	private Paket paket;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan_sekolah")
	public JurusanSekolahMahasiswaBaru getJurusanSekolahMahasiswaBaru() {
		jurusanSekolahMahasiswaBaru = check(jurusanSekolahMahasiswaBaru);
		return jurusanSekolahMahasiswaBaru;
	}

	public void setJurusanSekolahMahasiswaBaru(JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru) {
		this.jurusanSekolahMahasiswaBaru = jurusanSekolahMahasiswaBaru;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket")
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	public void setPaket(Paket paket) {
		this.paket = paket;
	}

}
