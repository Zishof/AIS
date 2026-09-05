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




import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

/**
 * Entitas <b>rentang keberlangganan/kepemilikan terbitan berkala</b> (tabel {@code library.serial})
 * untuk sebuah {@link Item} bertipe jurnal/majalah/publikasi berseri. {@link Item} sendiri mewakili
 * record bibliografis judul terbitan berkala itu (mis. dengan ISSN, sejajar dengan konsep "titles"
 * yang dipakai laporan operasional {@code LibraryOperationsApi} — "record bibliografis aktif, bukan
 * jumlah eksemplar"); satu judul dapat memiliki berapa pun baris {@code Serial} (relasi
 * banyak-ke-satu lewat {@link #getItem()}), masing-masing mewakili satu <b>periode</b>
 * keberlangganan/kepemilikan berkelanjutan (mis. satu volume/tahun langganan) dengan
 * {@link #getPeriode()} sebagai keterangan bebas frekuensi terbit (mis. "Bulanan", "Triwulan") dan
 * {@link #getMulai()}/{@link #getSelesai()} sebagai rentang tanggal berlakunya periode tersebut.
 *
 * <p>Dipakai laporan ringkas operasional lewat query native
 * {@code select count(id) from library.serial where mulai<=current_date and (selesai is null or
 * selesai>=current_date)} ({@code LibraryOperationsApi#overview}, dilaporkan sebagai
 * {@code activeSerials}) — perhatikan perbandingan tanggalnya <b>benar/wajar</b>
 * ({@code mulai} sudah lewat dan {@code selesai} belum tiba atau tidak ada batas), berbeda dari
 * kondisi terbalik pada {@link ItemPunyaTerbit#getAktif()} di kelas tetangga.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Item
 * @see ItemPunyaTerbit
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "serial")



public class Serial extends GeneralValueObject {

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
	 * Mengembalikan id pengguna yang terakhir mengubah baris periode terbitan berkala ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris periode terbitan berkala ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan, agar bidang audit bayangan ini tidak pernah
	 * ditimpa kosong oleh jalur salin/klon objek.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris periode terbitan berkala ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris periode terbitan berkala ini.
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
	 * Mengembalikan stempel waktu perubahan terakhir baris periode terbitan berkala ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: catatan bebas periode ini apa adanya, dipakai label bawaan
	 * komponen ZK dan penelusuran log.
	 *
	 * @return catatan bebas ({@link #keterangan}); dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/** Catatan bebas tentang periode terbitan berkala ini (mis. cakupan volume/nomor yang dimiliki). */
	private String keterangan;
	/** Tanggal mulai berlaku periode; bawaan waktu pembuatan objek. */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal berakhir berlaku periode; boleh kosong (masih berlangganan/belum ada batas akhir). */
	private Date selesai;
	/** Keterangan bebas frekuensi terbit periode ini (mis. "Bulanan", "Triwulan"). */
	private String periode;
	/** Item pustaka (judul terbitan berkala) yang periode ini termasuk di dalamnya. */
	private Item item;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public Serial() {
	}

	/**
	 * Mengembalikan kunci utama baris periode terbitan berkala ini.
	 *
	 * @return id baris periode, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris periode terbitan berkala ini. Hanya untuk kebutuhan Hibernate dan
	 * penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas tentang periode terbitan berkala ini.
	 *
	 * @return catatan bebas; boleh {@code null}
	 */
	@Column(name = "keterangan", columnDefinition = "text", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang periode terbitan berkala ini.
	 *
	 * @param keterangan teks catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal mulai berlaku periode ini. Dipakai {@code LibraryOperationsApi#overview}
	 * (lewat query native langsung ke kolom {@code mulai}) untuk menghitung jumlah periode yang
	 * sedang aktif.
	 *
	 * @return tanggal mulai; hanya bagian tanggal yang dipersisten ({@code @Temporal(DATE)})
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Menyetel tanggal mulai berlaku periode ini.
	 *
	 * @param mulai tanggal mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal berakhir berlaku periode ini.
	 *
	 * @return tanggal berakhir; boleh {@code null} (belum ada batas akhir/masih berlangsung); hanya
	 *         bagian tanggal yang dipersisten ({@code @Temporal(DATE)})
	 */
	@Temporal(TemporalType.DATE)
	public Date getSelesai() {
		return selesai;
	}

	/**
	 * Menyetel tanggal berakhir berlaku periode ini.
	 *
	 * @param selesai tanggal berakhir baru; boleh {@code null}
	 */
	public void setSelesai(Date selesai) {
		this.selesai = selesai;
	}

	/**
	 * Mengembalikan keterangan bebas frekuensi terbit periode ini.
	 *
	 * @return keterangan periode; boleh {@code null}
	 */
	public String getPeriode() {
		return periode;
	}

	/**
	 * Menyetel keterangan bebas frekuensi terbit periode ini.
	 *
	 * @param periode keterangan periode baru; boleh {@code null}
	 */
	public void setPeriode(String periode) {
		this.periode = periode;
	}

	/**
	 * Mengembalikan item pustaka (judul terbitan berkala) yang periode ini termasuk di dalamnya.
	 * Proxy lazy diresolusi lebih dulu lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return item judul terbitan berkala terkait, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel item pustaka (judul terbitan berkala) yang periode ini termasuk di dalamnya.
	 *
	 * @param item item judul terbitan berkala; boleh {@code null}
	 */
	public void setItem(Item item) {
		this.item = item;
	}

}
