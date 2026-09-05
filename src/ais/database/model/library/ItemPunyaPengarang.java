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

/**
 * Entitas relasi <b>banyak-ke-banyak</b> (tabel {@code library.item_punya_pengarang}) yang
 * menautkan satu {@link Item} pustaka ke satu {@link Pengarang}: satu baris di sini mewakili satu
 * pasangan item-pengarang, sehingga satu item dapat memiliki berapa pun baris (multi-pengarang) dan
 * satu pengarang dapat muncul pada berapa pun item. <b>Tidak ada kolom urutan/ordinal</b> pada
 * tabel ini — semua pengarang yang tertaut sebuah item berkedudukan setara, tidak ada pembedaan
 * "pengarang utama" vs "kontributor". Dikelola sepenuhnya lewat
 * {@code ais.action.master.library.helper.ItemPunyaPengarangHelper}: pengguna dapat menambah baris
 * dengan memilih {@link Pengarang} yang sudah ada lewat picker banyak-pilih, atau membuat
 * {@link Pengarang} baru langsung dari layar item. Baris langsung disimpan ke basis data bila
 * {@link Item} induk sudah persisten; bila item masih baru (belum punya id), baris hanya ditahan di
 * grid UI sampai proses simpan item induk yang menuntaskan penyimpanannya.
 *
 * <p>Bidang {@link #itemTemporary} menautkan baris ini ke {@link ItemTemporary} (salinan
 * sementara/draf sebuah item, dipakai alur revisi/impor) sebagai alternatif dari {@link #item}
 * definitif — mengikuti pola yang sama pada entitas relasi item lain di modul ini (mis.
 * {@link ItemPunyaKategoriItem}).</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Item
 * @see Pengarang
 * @see ItemTemporary
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "library", name = "item_punya_pengarang")
public class ItemPunyaPengarang extends GeneralValueObject {

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
	 * Mengembalikan id pengguna yang terakhir mengubah baris relasi ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris relasi ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan, agar bidang audit bayangan ini tidak pernah
	 * ditimpa kosong oleh jalur salin/klon objek.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris relasi ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris relasi ini.
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Mengembalikan stempel waktu perubahan terakhir baris relasi ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat gabungan item dan pengarang, dipakai label bawaan komponen ZK dan
	 * penelusuran log. Memanggil {@code toString()} kedua relasi apa adanya (dapat memicu resolusi
	 * lazy pada proxy Hibernate yang belum diinisialisasi).
	 *
	 * @return {@code "<item> - <pengarang>"}; bagian yang {@code null} tampil sebagai teks
	 *         {@code "null"} bawaan concatenation Java
	 */
	public String toString() {
		return item + " - " + pengarang;
	}

	/** Item pustaka pada pasangan relasi ini; alternatif dari {@link #itemTemporary} untuk item yang sudah definitif. */
	private Item item;
	/** Salinan sementara/draf item (bila relasi dibuat dari alur revisi/impor yang belum menunjuk item definitif); alternatif dari {@link #item}. */
	private ItemTemporary itemTemporary;
	/** Pengarang pada pasangan relasi ini. */
	private Pengarang pengarang;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public ItemPunyaPengarang() {
	}

	/**
	 * Mengembalikan kunci utama baris relasi ini.
	 *
	 * @return id baris relasi, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris relasi ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan item pustaka pada pasangan relasi ini. Proxy lazy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return item terkait, atau {@code null} bila baris ini menunjuk {@link #itemTemporary} saja
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel item pustaka pada pasangan relasi ini.
	 *
	 * @param item item terkait; boleh {@code null}
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan pengarang pada pasangan relasi ini. Dimuat dengan
	 * {@link FetchMode#SELECT} (query {@code SELECT} terpisah, bukan {@code JOIN}) sehingga tidak
	 * ikut memperbesar hasil join saat memuat daftar item.
	 *
	 * @return pengarang terkait, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengarang", nullable = true)
	public Pengarang getPengarang() {
		return pengarang;
	}

	/**
	 * Menyetel pengarang pada pasangan relasi ini.
	 *
	 * @param pengarang pengarang terkait; boleh {@code null}
	 */
	public void setPengarang(Pengarang pengarang) {
		this.pengarang = pengarang;
	}

	/**
	 * Mengembalikan salinan sementara/draf item terkait, alternatif dari {@link #getItem()} untuk
	 * baris yang dibuat sebelum item definitif tersedia. Dimuat dengan {@link FetchMode#SELECT}.
	 *
	 * @return draf item terkait, atau {@code null} bila baris ini menunjuk {@link #item} definitif
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_temporary", nullable = true)
	public ItemTemporary getItemTemporary() {
		return itemTemporary;
	}

	/**
	 * Menyetel draf item terkait.
	 *
	 * @param itemTemporary draf item; boleh {@code null}
	 */
	public void setItemTemporary(ItemTemporary itemTemporary) {
		this.itemTemporary = itemTemporary;
	}

}
