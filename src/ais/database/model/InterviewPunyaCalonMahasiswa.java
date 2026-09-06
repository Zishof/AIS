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
 * Model data untuk interview punya calon mahasiswa. Tipe ini membawa state yang dipertukarkan oleh
 * lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi
 * yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code InterviewCalonMahasiswa interviewCalonMahasiswa}, {@code
 * BiodataCalonMahasiswa biodataCalonMahasiswa}, {@code Date mulai}, {@code Date sampai}; pemetaan persistence:
 * tabel {@code public.interview_punya_calon_mahasiswa}; pembacaan/pencarian ({@code getOlehId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getId()}, {@code getInterviewCalonMahasiswa()}, {@code
 * getBiodataCalonMahasiswa()}); mutasi data ({@code setOlehId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setId()}, {@code setInterviewCalonMahasiswa()}); operasi domain lain ({@code
 * toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
@Table(schema = "public", name = "interview_punya_calon_mahasiswa")
public class InterviewPunyaCalonMahasiswa extends GeneralValueObject {

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
	 * @return nama calon mahasiswa yang dijadwalkan interview pada baris ini, dipakai untuk
	 * debugging/log. <b>Catatan:</b> memanggil langsung {@code biodataCalonMahasiswa.getNama()}
	 * tanpa null-check &mdash; melempar {@link NullPointerException} bila
	 * {@link #getBiodataCalonMahasiswa()} belum pernah dipanggil/diisi (field lazy belum dimuat).
	 */
	public String toString() {
		return biodataCalonMahasiswa.getNama();
	}

	private InterviewCalonMahasiswa interviewCalonMahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	private Date mulai;
	private Date sampai;
	private Boolean siap;
	private String keterangan;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public InterviewPunyaCalonMahasiswa() {
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
	 * @return sesi {@link InterviewCalonMahasiswa} induk (jadwal interview massal/kelompok) yang
	 * menaungi baris peserta ini; relasi lazy, dimuat via
	 * {@link GeneralValueObject#check(Object)} saat pertama diakses. {@link #getMulai()} dan
	 * {@link #getSampai()} jatuh kembali ke jadwal induk ini bila tidak di-override per baris.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "interview_calon_mahasiswa")
	public InterviewCalonMahasiswa getInterviewCalonMahasiswa() {
		interviewCalonMahasiswa = check(interviewCalonMahasiswa);
		return interviewCalonMahasiswa;
	}

	/** @param interviewCalonMahasiswa sesi interview induk yang menaungi baris peserta ini. */
	public void setInterviewCalonMahasiswa(InterviewCalonMahasiswa interviewCalonMahasiswa) {
		this.interviewCalonMahasiswa = interviewCalonMahasiswa;
	}

	/**
	 * @return calon mahasiswa peserta interview pada baris ini; relasi lazy (kolom
	 * {@code unique = true}, satu calon mahasiswa hanya boleh punya satu baris peserta), dimuat
	 * via {@link GeneralValueObject#check(Object)} saat pertama diakses.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_mahasiswa", unique = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/** @param biodataCalonMahasiswa calon mahasiswa peserta interview pada baris ini. */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return waktu mulai interview untuk peserta ini. Bila field lokal {@link #mulai} belum
	 * di-set (override per peserta), jatuh kembali ke {@code getInterviewCalonMahasiswa().getMulai()}
	 * (jadwal sesi induk di {@link InterviewCalonMahasiswa}), atau {@code null} bila sesi induk
	 * juga belum ada.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		return mulai != null ? mulai
				: (getInterviewCalonMahasiswa() == null ? null : getInterviewCalonMahasiswa().getMulai());
	}

	/** @param mulai waktu mulai interview khusus untuk peserta ini (override jadwal sesi induk). */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * @return waktu selesai interview untuk peserta ini, dengan fallback yang sama ke sesi induk
	 * {@link InterviewCalonMahasiswa} seperti {@link #getMulai()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSampai() {
		return sampai != null ? sampai
				: (getInterviewCalonMahasiswa() == null ? null : getInterviewCalonMahasiswa().getSampai());
	}

	/** @param sampai waktu selesai interview khusus untuk peserta ini (override jadwal sesi induk). */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/** @return keterangan/catatan hasil interview untuk peserta ini; string kosong (bukan {@code null}) bila belum diisi. */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/** @param keterangan keterangan/catatan hasil interview untuk peserta ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return apakah peserta sudah dinyatakan siap/lulus interview; default {@code false} bila belum diisi. */
	public Boolean getSiap() {
		return siap == null ? false : siap;
	}

	/** @param siap apakah peserta sudah dinyatakan siap/lulus interview. */
	public void setSiap(Boolean siap) {
		this.siap = siap;
	}

}
