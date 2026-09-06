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
 * Model data untuk paket jurusan pmb. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Paket paket}, {@code Jurusan jurusan}, {@code Boolean
 * pilihan1}, {@code Boolean pilihan2}; pemetaan persistence: tabel {@code paket_has_jurusan};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()},
 * {@code getPaket()}, {@code getJurusan()}); mutasi data ({@code setOlehId()}, {@code setOleh()}, {@code
 * onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()}, {@code setPaket()}); operasi domain lain ({@code
 * toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "paket_has_jurusan")
public class PaketJurusanPmb extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6142801292147884065L;

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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	/** Menandai baris berubah; dipanggil otomatis oleh Hibernate sebelum setiap update. */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * @return representasi ringkas berupa {@code paket_jurusan}, dipakai untuk debugging/log.
	 * Memuat ulang {@link #getPaket()} dan {@link #getJurusan()} terlebih dahulu agar relasi lazy
	 * dimuat sebelum digabung ke string.
	 */
	public String toString() {
		paket = getPaket();
		jurusan = getJurusan();
		return paket + "_" + jurusan;
	}

	private Paket paket;
	private Jurusan jurusan;
	private Boolean pilihan1;
	private Boolean pilihan2;
	private Boolean pilihan3;
	private Boolean pilihan4;
	private Boolean pilihan5;
	private String kelamin;
	private Integer kuota;
	private Boolean kuotaBerlakuPerGelombang;

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
	 * @return {@link Paket} pendaftaran yang dikaitkan dengan {@link #getJurusan()} pada baris
	 * ini; relasi lazy, dimuat via {@link GeneralValueObject#check(Object)} saat pertama diakses.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket")
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	/** @param paket paket pendaftaran yang dikaitkan dengan {@link #getJurusan()}. */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	/**
	 * @return {@link Jurusan} (program studi) yang tersedia untuk {@link #getPaket()} ini; relasi
	 * lazy, dimuat via {@link GeneralValueObject#check(Object)} saat pertama diakses.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/** @param jurusan program studi yang tersedia untuk {@link #getPaket()}. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/** @return apakah jurusan ini boleh dipilih sebagai pilihan ke-1; default {@code true} bila belum diisi. */
	public Boolean getPilihan1() {
		return pilihan1 == null ? true : pilihan1;
	}

	/** @param pilihan1 apakah jurusan ini boleh dipilih sebagai pilihan ke-1. */
	public void setPilihan1(Boolean pilihan1) {
		this.pilihan1 = pilihan1;
	}

	/** @return apakah jurusan ini boleh dipilih sebagai pilihan ke-2; default {@code true} bila belum diisi. */
	public Boolean getPilihan2() {
		return pilihan2 == null ? true : pilihan2;
	}

	/** @param pilihan2 apakah jurusan ini boleh dipilih sebagai pilihan ke-2. */
	public void setPilihan2(Boolean pilihan2) {
		this.pilihan2 = pilihan2;
	}

	/** @return apakah jurusan ini boleh dipilih sebagai pilihan ke-3; default {@code true} bila belum diisi. */
	public Boolean getPilihan3() {
		return pilihan3 == null ? true : pilihan3;
	}

	/** @param pilihan3 apakah jurusan ini boleh dipilih sebagai pilihan ke-3. */
	public void setPilihan3(Boolean pilihan3) {
		this.pilihan3 = pilihan3;
	}

	/** @return apakah jurusan ini boleh dipilih sebagai pilihan ke-4; default {@code true} bila belum diisi. */
	public Boolean getPilihan4() {
		return pilihan4 == null ? true : pilihan4;
	}

	/** @param pilihan4 apakah jurusan ini boleh dipilih sebagai pilihan ke-4. */
	public void setPilihan4(Boolean pilihan4) {
		this.pilihan4 = pilihan4;
	}

	/** @return apakah jurusan ini boleh dipilih sebagai pilihan ke-5; default {@code true} bila belum diisi. */
	public Boolean getPilihan5() {
		return pilihan5 == null ? true : pilihan5;
	}

	/** @param pilihan5 apakah jurusan ini boleh dipilih sebagai pilihan ke-5. */
	public void setPilihan5(Boolean pilihan5) {
		this.pilihan5 = pilihan5;
	}

	/** @return batasan jenis kelamin pendaftar untuk kombinasi paket/jurusan ini; default {@code "Semua"} bila kosong. */
	public String getKelamin() {
		return kelamin == null || kelamin.trim().isEmpty() ? "Semua" : kelamin;
	}

	/** @param kelamin batasan jenis kelamin pendaftar untuk kombinasi paket/jurusan ini. */
	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	/** @return kuota pendaftar untuk kombinasi paket/jurusan ini; default {@code 10000} (efektif tanpa batas) bila belum diisi. */
	public Integer getKuota() {
		return kuota == null ? 10000 : kuota;
	}

	/** @param kuota kuota pendaftar untuk kombinasi paket/jurusan ini. */
	public void setKuota(Integer kuota) {
		this.kuota = kuota;
	}

	/** @return apakah {@link #getKuota()} dihitung per gelombang pendaftaran (bukan kumulatif lintas gelombang); default {@code true}. */
	public Boolean getKuotaBerlakuPerGelombang() {
		return kuotaBerlakuPerGelombang == null ? true : kuotaBerlakuPerGelombang;
	}

	/** @param kuotaBerlakuPerGelombang apakah {@link #getKuota()} dihitung per gelombang pendaftaran. */
	public void setKuotaBerlakuPerGelombang(Boolean kuotaBerlakuPerGelombang) {
		this.kuotaBerlakuPerGelombang = kuotaBerlakuPerGelombang;
	}

}
