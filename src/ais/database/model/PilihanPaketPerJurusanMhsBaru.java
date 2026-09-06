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
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku. Nilai kosong/blank diabaikan (fail-safe agar audit shadow tidak
	 * tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah di-set ke
	 * waktu saat ini pada deklarasi field dan di-refresh otomatis oleh {@link #onUpdate()} pada
	 * setiap update; setter ini jarang perlu dipanggil langsung.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code jurusanSekolahMahasiswaBaru_paket}, dipakai untuk
	 * debugging/log. Memanggil {@link #getPaket()} dan {@link #getJurusanSekolahMahasiswaBaru()}
	 * terlebih dahulu agar relasi lazy dimuat sebelum digabung ke string.
	 */
	public String toString() {
		getPaket();
		getJurusanSekolahMahasiswaBaru();
		return jurusanSekolahMahasiswaBaru + "_" + paket;
	}

	private JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru;
	private Paket paket;

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	/** @param id id baris. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return jurusan sekolah asal (jenjang mahasiswa baru) yang dipilih pada baris ini; relasi
	 * lazy, dimuat via {@link GeneralValueObject#check(Object)} saat pertama diakses.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan_sekolah")
	public JurusanSekolahMahasiswaBaru getJurusanSekolahMahasiswaBaru() {
		jurusanSekolahMahasiswaBaru = check(jurusanSekolahMahasiswaBaru);
		return jurusanSekolahMahasiswaBaru;
	}

	/** @param jurusanSekolahMahasiswaBaru jurusan sekolah asal yang dipilih pada baris ini. */
	public void setJurusanSekolahMahasiswaBaru(JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru) {
		this.jurusanSekolahMahasiswaBaru = jurusanSekolahMahasiswaBaru;
	}

	/**
	 * @return {@link Paket} pendaftaran yang dipasangkan dengan
	 * {@link #getJurusanSekolahMahasiswaBaru()} pada baris pilihan ini; relasi lazy, dimuat via
	 * {@link GeneralValueObject#check(Object)} saat pertama diakses.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket")
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	/** @param paket paket pendaftaran yang dipasangkan dengan jurusan sekolah asal ini. */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

}
