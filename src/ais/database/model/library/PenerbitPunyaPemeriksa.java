package ais.database.model.library;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;



/**
 * Entitas relasi (tabel {@code library.penerbit_punya_pemeriksa}) yang menugaskan seorang
 * <b>pemeriksa</b> ({@link Tbmuser} — user yang ditugaskan meninjau/mereview) pada sebuah
 * {@link Penerbit}, masing-masing dapat dikaitkan ke satu {@link DomainPenelitian} (domain/bidang
 * penelitian tempat pemeriksa itu berkompeten) dan punya status {@link #getAktif()} per baris.
 * Penugasan pemeriksa di sini berada pada tingkat <b>penerbit</b>, <b>bukan</b> per item —
 * bandingkan dengan {@link ItemPunyaPemeriksa} yang menautkan pemeriksa langsung ke {@link Item}
 * untuk alur review satu karya/koleksi tertentu (dengan status {@code BELUM_DIPERIKSA}/
 * {@code REVISI}/{@code DITOLAK}/{@code DISETUJUI}, tidak ada di kelas ini). Dikelola sepenuhnya
 * lewat {@code ais.action.master.library.helper.PenerbitPunyaPemeriksaHelper}: pemeriksa dipilih
 * lewat dialog banyak-pilih yang mengecualikan pemeriksa yang sudah ada pada grid penerbit yang
 * sama, domain penelitian dan status aktif tiap baris dapat diubah langsung dari kombo/checkbox
 * pada grid (tersimpan seketika bila baris sudah persisten).
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Penerbit
 * @see ItemPunyaPemeriksa
 * @see DomainPenelitian
 * @see Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "penerbit_punya_pemeriksa")



public class PenerbitPunyaPemeriksa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan, agar bidang audit bayangan ini tidak pernah
	 * ditimpa kosong oleh jalur salin/klon objek.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris penugasan ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Interceptor-lah
	 * yang mengisi {@link #oleh}, {@link #olehId}, dan {@link #getTanggal_dirubah()} dari konteks
	 * pengguna aktif. Method sengaja {@code protected} dan tidak boleh dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor},
	 * bukan oleh form.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris penugasan ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat gabungan penerbit dan pemeriksa, dipakai label bawaan komponen ZK
	 * dan penelusuran log. Memanggil {@code toString()} kedua relasi apa adanya (dapat memicu
	 * resolusi lazy pada proxy Hibernate yang belum diinisialisasi).
	 *
	 * @return {@code "<penerbit> - <pemeriksa>"}; bagian yang {@code null} tampil sebagai teks
	 *         {@code "null"} bawaan concatenation Java
	 */
	public String toString() {
		return penerbit + " - " + pemeriksa;
	}

	/** Penerbit yang mendapat penugasan pemeriksa ini. */
	private Penerbit penerbit;
	/** Pengguna yang ditugaskan sebagai pemeriksa/reviewer. */
	private Tbmuser pemeriksa;
	/** Domain/bidang penelitian tempat pemeriksa ini berkompeten pada penerbit terkait; boleh kosong. */
	private DomainPenelitian domainPenelitian;
	/** Catatan bebas tentang penugasan ini. */
	private String keterangan;
	/** Status aktif penugasan; bawaan {@code false} (berbeda dari {@link Pengarang#getAktif()} yang bawaan {@code true}). */
	private Boolean aktif = false;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public PenerbitPunyaPemeriksa() {
	}

	/**
	 * Mengembalikan kunci utama baris penugasan ini.
	 *
	 * @return id baris penugasan, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris penugasan ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas tentang penugasan ini.
	 *
	 * @return catatan bebas; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang penugasan ini.
	 *
	 * @param keterangan teks catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif penugasan pemeriksa ini.
	 *
	 * @return status aktif; {@code false} bila bidang belum pernah disetel ({@code null}
	 *         diperlakukan sebagai tidak aktif, berbeda dari {@link Pengarang#getAktif()} yang
	 *         memperlakukan {@code null} sebagai aktif)
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = false;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif penugasan pemeriksa ini.
	 *
	 * @param aktif status aktif baru; {@code null} akan diperlakukan sebagai tidak aktif oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penerbit yang mendapat penugasan pemeriksa ini. Dimuat dengan
	 * {@link FetchMode#SELECT}.
	 *
	 * @return penerbit terkait, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerbit", nullable = true)
	public Penerbit getPenerbit() {
		return penerbit;
	}

	/**
	 * Menyetel penerbit yang mendapat penugasan pemeriksa ini.
	 *
	 * @param penerbit penerbit terkait; boleh {@code null}
	 */
	public void setPenerbit(Penerbit penerbit) {
		this.penerbit = penerbit;
	}

	/**
	 * Mengembalikan pengguna yang ditugaskan sebagai pemeriksa/reviewer pada baris ini. Dimuat
	 * dengan {@link FetchMode#SELECT}.
	 *
	 * @return pengguna pemeriksa, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemeriksa", nullable = true)
	public Tbmuser getPemeriksa() {
		return pemeriksa;
	}

	/**
	 * Menyetel pengguna yang ditugaskan sebagai pemeriksa/reviewer pada baris ini.
	 *
	 * @param pemeriksa pengguna pemeriksa; boleh {@code null}
	 */
	public void setPemeriksa(Tbmuser pemeriksa) {
		this.pemeriksa = pemeriksa;
	}

	/**
	 * Mengembalikan domain/bidang penelitian tempat pemeriksa pada baris ini berkompeten. Dimuat
	 * dengan {@link FetchMode#SELECT}.
	 *
	 * @return domain penelitian terkait, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "domain_penelitian", nullable = true)
	public DomainPenelitian getDomainPenelitian() {
		return domainPenelitian;
	}

	/**
	 * Menyetel domain/bidang penelitian tempat pemeriksa pada baris ini berkompeten.
	 *
	 * @param domainPenelitian domain penelitian baru; boleh {@code null}
	 */
	public void setDomainPenelitian(DomainPenelitian domainPenelitian) {
		this.domainPenelitian = domainPenelitian;
	}

}
