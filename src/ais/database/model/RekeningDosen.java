package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Entity Hibernate untuk tabel {@code public.rekening_dosen} &mdash; nomor rekening bank
 * seorang {@link Dosen} untuk keperluan pembayaran (mis. honor/tunjangan), berelasi ke
 * master {@link Bank}.
 *
 * <p><b>Catatan keamanan/privasi.</b> {@link #getNoRekening()} disimpan sebagai teks polos
 * (plaintext), tidak dienkripsi maupun dimasking di level entity ini. Gerbang akses layar
 * ({@code ais.action.master.RekeningDosenAction}) hanya berupa pemeriksaan privilese modul
 * generik ({@code CommonPrivilages.checkPrevilages(CommonPrivilages.READ)}); pencarian data
 * ({@code initCriteria()}) di action itu <b>tidak menyaring baris berdasarkan satuan kerja,
 * fakultas, atau kepemilikan dosen yang login</b> &mdash; hanya filter opsional per bank/dosen
 * dari kombobox pencarian. Akibatnya siapa pun yang memiliki hak baca pada menu ini bisa
 * melihat nomor rekening bank <b>seluruh dosen di institusi</b>, bukan hanya dosen di unit
 * kerjanya. Lihat juga {@link Dosen} untuk kemungkinan makna satuan kerja pada domain ini.
 *
 * <p>Diakses lewat DAO generik {@code ais.database.dao.RekeningDosenDaoImpl}. Diturunkan dari
 * {@link GeneralValueObject}; {@code id}, {@code oleh}, {@code olehId}, dan
 * {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas induk adalah POJO
 * abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis,
 * bukan duplikasi keliru.
 *
 * @see Bank
 * @see Dosen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "rekening_dosen")

public class RekeningDosen extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -5130925150455694214L;
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

	/** @return nomor rekening ({@link #getNoRekening()}) apa adanya, dipakai untuk debugging/log. */
	public String toString() {
		return noRekening;
	}

	private String noRekening;
	private Bank bank;
	private Dosen dosen;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public RekeningDosen() {
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

	/** @param noRekening nomor rekening bank dosen, disimpan sebagai teks polos (tidak dienkripsi). */
	public void setNoRekening(String noRekening) {
		this.noRekening = noRekening;
	}

	/**
	 * @return nomor rekening bank dosen apa adanya (teks polos, tidak dienkripsi/dimasking);
	 *     lihat catatan keamanan pada javadoc kelas soal gerbang akses yang tidak menyaring
	 *     berdasarkan satuan kerja/kepemilikan.
	 */
	@Column(name = "no_rekening", length = 150)
	public String getNoRekening() {
		return noRekening;
	}

	/** @param bank bank tujuan rekening ini. */
	public void setBank(Bank bank) {
		this.bank = bank;
	}

	/** @return bank tujuan rekening ini (wajib diisi). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bank", nullable = false)
	public Bank getBank() {
		return bank;
	}

	/** @param dosen dosen pemilik rekening ini. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/** @return dosen pemilik rekening ini (wajib diisi). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return dosen;
	}

}
