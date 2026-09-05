package ais.database.model.library;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;



/**
 * Entitas relasi (tabel {@code library.item_punya_pemeriksa}) yang menautkan langsung sebuah
 * {@link Item} pustaka (karya ilmiah/koleksi yang perlu ditinjau sebelum "terbit") ke seorang
 * <b>pemeriksa</b> ({@link Tbmuser} — user yang ditugaskan meninjau/mereview), berikut satu
 * {@link #getStatus()} alur review per pasangan item-pemeriksa. Penugasan di sini berada pada
 * tingkat <b>item</b>, <b>berbeda</b> dari {@link PenerbitPunyaPemeriksa} yang menugaskan pemeriksa
 * pada tingkat {@link Penerbit} (tanpa status alur, hanya flag aktif dan domain penelitian
 * opsional). Dikelola sepenuhnya lewat
 * {@code ais.action.master.library.helper.ItemPunyaPemeriksaHelper}: pemeriksa ditambahkan lewat
 * dialog banyak-pilih yang mengecualikan pemeriksa yang sudah ada pada grid item yang sama; baris
 * langsung tersimpan bila {@link Item} induk sudah persisten, atau ditahan di memori saja bila item
 * belum tersimpan (menunggu form induk disimpan).
 *
 * <h2>Status alur review</h2>
 * <p>Empat nilai status yang didukung ({@link #BELUM_DIPERIKSA}, {@link #REVISI},
 * {@link #DITOLAK}, {@link #DISETUJUI}) adalah konstanta string bebas — bidang {@link #status}
 * sendiri hanyalah {@code String} polos tanpa validasi enum atau {@code @Column} yang membatasi
 * nilainya ke keempat konstanta ini; helper UI-lah yang (bila ada) bertanggung jawab menjaga
 * konsistensi nilainya. {@link #getStatus()} mengembalikan {@link #BELUM_DIPERIKSA} sebagai bawaan
 * ketika bidang masih {@code null}, sehingga baris baru yang belum sempat direview tampil sebagai
 * "Belum Diperiksa" tanpa perlu inisialisasi eksplisit di konstruktor.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Item
 * @see PenerbitPunyaPemeriksa
 * @see Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "item_punya_pemeriksa")



public class ItemPunyaPemeriksa extends GeneralValueObject {

	/** Status alur review bawaan untuk baris yang belum pernah direview; dikembalikan {@link #getStatus()} saat bidang {@link #status} masih {@code null}. */
	public static final String BELUM_DIPERIKSA = "Belum Diperiksa";
	/** Status alur review: pemeriksa meminta revisi kepada penyusun item. */
	public static final String REVISI = "Revisi";
	/** Status alur review: item ditolak pemeriksa. */
	public static final String DITOLAK = "Ditolak";
	/** Status alur review: item disetujui pemeriksa. */
	public static final String DISETUJUI = "Disetujui";

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
	 * Representasi teks singkat gabungan item dan pemeriksa, dipakai label bawaan komponen ZK dan
	 * penelusuran log. Memanggil {@code toString()} kedua relasi apa adanya (dapat memicu resolusi
	 * lazy pada proxy Hibernate yang belum diinisialisasi).
	 *
	 * @return {@code "<item> - <pemeriksa>"}; bagian yang {@code null} tampil sebagai teks
	 *         {@code "null"} bawaan concatenation Java
	 */
	public String toString() {
		return item + " - " + pemeriksa;
	}

	/** Item pustaka yang menjadi objek review pada baris ini. */
	private Item item;
	/** Pengguna yang ditugaskan sebagai pemeriksa/reviewer untuk item terkait. */
	private Tbmuser pemeriksa;
	/** Status alur review saat ini; lihat {@link #getStatus()} untuk bawaan saat kosong. */
	private String status;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public ItemPunyaPemeriksa() {
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
	 * Mengembalikan item pustaka yang menjadi objek review pada baris ini. Proxy lazy diresolusi
	 * lebih dulu lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return item terkait, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel item pustaka yang menjadi objek review pada baris ini.
	 *
	 * @param item item terkait; boleh {@code null}
	 */
	public void setItem(Item item) {
		this.item = item;
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
	 * @param tbmuser pengguna pemeriksa; boleh {@code null}
	 */
	public void setPemeriksa(Tbmuser tbmuser) {
		this.pemeriksa = tbmuser;
	}

	/**
	 * Mengembalikan status alur review baris ini, mengisi bidang {@link #status} dengan
	 * {@link #BELUM_DIPERIKSA} (dan menyimpan hasilnya ke bidang, bukan sekadar nilai balik) bila
	 * masih {@code null}. Getter ini sedikit destruktif: pemanggilan pertama pada baris yang belum
	 * pernah direview mengubah bidang di memori dari {@code null} menjadi teks bawaan tersebut.
	 *
	 * @return status alur review saat ini; salah satu dari {@link #BELUM_DIPERIKSA},
	 *         {@link #REVISI}, {@link #DITOLAK}, {@link #DISETUJUI} dalam pemakaian normal, namun
	 *         tidak ditegakkan enum apa pun sehingga nilai bebas lain juga dapat tersimpan bila
	 *         disetel langsung lewat {@link #setStatus(String)}
	 */
	public String getStatus() {
		if(status == null){
			status = ItemPunyaPemeriksa.BELUM_DIPERIKSA;
		}
		return status;
	}

	/**
	 * Menyetel status alur review baris ini secara bebas (tidak divalidasi terhadap konstanta
	 * {@link #BELUM_DIPERIKSA}/{@link #REVISI}/{@link #DITOLAK}/{@link #DISETUJUI}).
	 *
	 * @param status status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

}
