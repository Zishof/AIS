package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
 * Entity Hibernate untuk tabel {@code public.report_log} &mdash; catatan log generik
 * berupa teks bebas ({@link #getKeterangan()}) beserta {@link #getTahun()} pencatatan.
 *
 * <p><b>Catatan keamanan/privasi.</b> Satu-satunya pemanggil yang diketahui di repo ini
 * adalah endpoint REST publik {@code MahasiswaResource#reportLog(String)}
 * ({@code GET /mahasiswa/report_log/{url}}), yang menulis baris baru berisi parameter URL
 * apa adanya ke {@link #getKeterangan()} <b>tanpa pemeriksaan autentikasi apa pun</b>.
 * Siapa pun (termasuk pengguna anonim) dapat menyisipkan teks bebas ke tabel ini melalui
 * endpoint tersebut. Javadoc {@code MahasiswaResource} sudah menilai risiko ini secara
 * eksplisit dan sengaja <b>tidak</b> mengubahnya pada audit keamanan 1 September 2026:
 * dampaknya dinilai rendah karena tidak ada transaksi finansial maupun kebocoran data
 * pribadi yang terjadi lewat jalur ini (hanya penulisan log, bukan pembacaan data). Kelas
 * ini sendiri tidak menyimpan data sensitif secara struktural (tidak ada kolom identitas
 * pengguna/PII); risiko yang ada murni pada sisi endpoint (spam/log injection, bukan
 * pembocoran data).
 *
 * <p>Diakses lewat sesi Hibernate native (bukan DAO generik) di pemanggil di atas.
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
@Table(schema = "public", name = "report_log")

public class ReportLog extends GeneralValueObject {

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

	/** @return isi log ({@link #getKeterangan()}) apa adanya, dipakai untuk debugging/log. */
	public String toString() {
		return keterangan;
	}

	private String keterangan;
	private Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);

	/** Konstruktor kosong yang diperlukan Hibernate; {@link #tahun} default ke tahun berjalan. */
	public ReportLog() {
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
	 * @return isi log berupa teks bebas, boleh {@code null}; lihat catatan keamanan pada
	 *     javadoc kelas soal sumber isian yang tidak terautentikasi.
	 */
	@Column(name = "keterangan", columnDefinition = "text", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan isi log berupa teks bebas. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return tahun pencatatan baris log ini; default tahun berjalan saat instance dibuat. */
	public Integer getTahun() {
		return tahun;
	}

	/** @param tahun tahun pencatatan baris log ini. */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

}
