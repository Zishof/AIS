package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity Hibernate untuk tabel {@code public.jenis_toefl} &mdash; master <b>jenis
 * tes TOEFL/TOAFL</b> (mis. ITP, iBT, TOAFL) beserta rumus perhitungan skor totalnya.
 * Direferensikan dari {@code NilaiToeflToaflMahasiswa#getJenisToefl()}.
 *
 * <p><b>{@link #getRumus()} dievaluasi sebagai ekspresi matematis saat runtime.</b> Nilainya
 * dipakai langsung sebagai input {@code ExpressionBuilder} (exp4j) di
 * {@code NilaiToeflToaflMahasiswa#getTotal()}, dengan variabel {@code skor1}..{@code skor4}
 * yang di-bind ke nilai skor per bagian tes mahasiswa. Karena rumus adalah teks bebas yang
 * dapat diubah lewat CRUD master ({@code ais.action.master.JenisToeflAction}), hanya
 * operator dengan hak tulis ke master data ini yang dapat mengubah cara skor dihitung untuk
 * seluruh mahasiswa yang memakai jenis tes tersebut &mdash; bukan celah yang dapat dieksploitasi
 * pengguna biasa, tetapi perubahan rumus keliru berdampak luas (menghitung ulang semua
 * skor total secara diam-diam pada pemanggilan berikutnya, tanpa audit terpisah untuk
 * hasil perhitungan lama vs baru).
 *
 * <p>Bila kolom {@code rumus} belum diisi, {@link #getRumus()} mengembalikan formula bawaan
 * rata-rata empat skor: {@code (skor1 + skor2 + skor3 + skor4) / 4.0}.
 *
 * <p>Dikelola lewat CRUD master data sederhana di {@code ais.action.master.JenisToeflAction}.
 * Diturunkan dari {@link GeneralValueObject}; {@code id}, {@code oleh}, {@code olehId}, dan
 * {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas induk adalah POJO
 * abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis,
 * bukan duplikasi keliru.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_toefl")

public class JenisToefl extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
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
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah
	 * di-set ke waktu saat ini pada deklarasi field dan di-refresh otomatis oleh
	 * {@link #onUpdate()} pada setiap update; setter ini jarang perlu dipanggil langsung.
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

	/** @return representasi ringkas berupa {@code id-nama}, dipakai untuk debugging/log. */
	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String rumus;
	private String keterangan;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public JenisToefl() {
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

	/** @return nama jenis tes TOEFL/TOAFL (wajib diisi), sudah di-trim. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jenis tes TOEFL/TOAFL. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan tambahan untuk jenis tes ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan tambahan untuk jenis tes ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return rumus perhitungan skor total, sudah di-trim; formula bawaan
	 *     {@code " (skor1 + skor2 + skor3 + skor4) / 4.0 "} bila kolom masih {@code null}
	 *     (belum pernah diisi). Lihat catatan kelas soal evaluasi ekspresi saat runtime.
	 */
	@Column(name = "rumus", columnDefinition = "text", nullable = true)
	public String getRumus() {
		return rumus == null ? " (skor1 + skor2 + skor3 + skor4) / 4.0 " : rumus.trim();
	}

	/** @param rumus rumus perhitungan skor total baru, dalam sintaks ekspresi exp4j. */
	public void setRumus(String rumus) {
		this.rumus = rumus;
	}

}
