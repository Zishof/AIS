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
	 * @return nama calon mahasiswa yang menempati baris ruang ini, dipakai untuk debugging/log.
	 * <b>Catatan:</b> memanggil langsung {@code biodataCalonMahasiswa.getNama()} tanpa null-check
	 * &mdash; melempar {@link NullPointerException} bila {@link #getBiodataCalonMahasiswa()}
	 * belum pernah dipanggil/diisi (field lazy belum dimuat).
	 */
	public String toString() {
		return biodataCalonMahasiswa.getNama();
	}

	private RuangPMB ruangPMB;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private String kodeUnik;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public RuangPaketPMB() {
	}

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return ruang PMB yang ditempati baris ini; relasi lazy, dimuat via
	 * {@link GeneralValueObject#check(Object)} saat pertama diakses.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_pmb")
	public RuangPMB getRuangPMB() {
		ruangPMB = check(ruangPMB);
		return ruangPMB;
	}

	/** @param ruangPMB ruang PMB yang ditempati baris ini. */
	public void setRuangPMB(RuangPMB ruangPMB) {
		this.ruangPMB = ruangPMB;
	}

	/**
	 * @return calon mahasiswa yang menempati baris ruang ini; relasi lazy, dimuat via
	 * {@link GeneralValueObject#check(Object)} saat pertama diakses.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_mahasiswa")
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/** @param biodataCalonMahasiswa calon mahasiswa yang menempati baris ruang ini. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return kode unik baris ini, dihitung ulang setiap dibaca (getter destruktif, menulis ke
	 * field {@link #kodeUnik}): jika {@link #getRuangPMB()} atau {@link #getBiodataCalonMahasiswa()}
	 * belum terisi, mengembalikan nilai lama tanpa perhitungan; bila paket ruang dan paket calon
	 * mahasiswa cocok (atau salah satunya {@code null}), kode dibentuk dari
	 * {@code biodataCalonMahasiswa.getId() + "_"}, selain itu string kosong. Kolom dipetakan
	 * {@code unique = true} sehingga kombinasi id calon mahasiswa yang sama dapat memicu
	 * {@code ConstraintViolationException} bila dua baris ruang menghasilkan kode identik.
	 */
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

	/** @param kodeUnik kode unik baris ini; biasanya diturunkan otomatis oleh {@link #getKodeUnik()}. */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
