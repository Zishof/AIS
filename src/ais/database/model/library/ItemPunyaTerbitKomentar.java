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



/**
 * Entitas <b>komentar publik</b> (tabel {@code library.item_punya_terbit_punya_terbit_komentar})
 * atas satu baris {@link ItemPunyaTerbit} (konten terbitan sebuah {@link Item}), diisi lewat
 * endpoint publik {@code PerpustakaanResource#tambahKomentarItemterbit} tanpa memerlukan login —
 * dipakai fitur "kirim komentar" pada halaman publikasi item yang dipublikasikan lewat REST API
 * perpustakaan (lihat juga {@code PerpustakaanResource#daftarItemTerbitKomentar} dan
 * {@code #daftarItemTerbitKomentarSemua} untuk pembacaannya).
 *
 * <h2>Bidang {@link #nama} menampung identitas pengirim, bukan isi komentar</h2>
 * <p>Meski nama kelasnya "komentar" dan kolom {@link #nama} bertipe {@code text} (tidak dibatasi
 * panjang seperti nama pada umumnya), penelusuran ke pemanggil satu-satunya
 * ({@code PerpustakaanResource#tambahKomentarItemterbit}) menunjukkan {@link #nama} diisi dari
 * parameter posisional kedua ({@code [1]=nama}) — yakni <b>nama pengirim komentar</b>, sejajar
 * dengan {@link #kontak} ({@code [2]}) dan {@link #email} ({@code [3]}) yang juga identitas
 * pengirim. Endpoint publik tersebut <b>tidak menerima parameter isi/teks komentar apa pun</b> —
 * hanya identitas pengirim (nama, kontak, email) yang direkam, {@link #alamat} selalu diset string
 * kosong secara hardcode oleh pemanggilnya. Dengan kata lain, tabel ini pada implementasinya saat
 * ini berfungsi sebagai <b>log pengunjung/pemberi komentar per publikasi item</b> (siapa yang
 * "berkomentar" dan kapan, dihitung lewat {@code daftarItemTerbitJumlahKomentar}), bukan penyimpan
 * teks komentar itu sendiri — tidak ada kolom terpisah untuk isi pesan.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek. Untuk baris yang dibuat lewat endpoint publik
 * di atas, {@link #getTanggal_dirubah()} adalah satu-satunya penanda waktu pengiriman komentar
 * (dipakai {@code PerpustakaanResource} untuk mengurutkan "terbaru dulu").</p>
 *
 * @see ItemPunyaTerbit
 * @see Item
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "item_punya_terbit_punya_terbit_komentar")



public class ItemPunyaTerbitKomentar extends GeneralValueObject {

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
	 * Mengembalikan id pengguna yang terakhir mengubah baris komentar ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris komentar ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan, agar bidang audit bayangan ini tidak pernah
	 * ditimpa kosong oleh jalur salin/klon objek.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris komentar ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris komentar ini.
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
	 * Mengembalikan stempel waktu perubahan terakhir baris komentar ini — untuk baris yang dibuat
	 * lewat endpoint publik {@code tambahKomentarItemterbit}, sekaligus berfungsi sebagai waktu
	 * pengiriman komentar karena tidak ada kolom waktu kirim terpisah.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: nama pengirim komentar apa adanya (bidang mentah, bukan hasil
	 * {@link #getNama()} yang dipangkas), dipakai label bawaan komponen ZK dan penelusuran log.
	 *
	 * @return nama pengirim komentar apa adanya; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nama pengirim komentar (identitas, bukan isi pesan — lihat catatan pada javadoc kelas). */
	private String nama;
	/** Alamat pengirim komentar; selalu diset string kosong oleh satu-satunya pemanggil endpoint publik yang mengisi kelas ini. */
	private String alamat;
	/** Nomor kontak pengirim komentar. */
	private String kontak;
	/** Alamat surel pengirim komentar. */
	private String email;
	/** Baris {@link ItemPunyaTerbit} (publikasi item) yang menjadi objek komentar ini. */
	private ItemPunyaTerbit itemPunyaTerbit;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public ItemPunyaTerbitKomentar() {
	}

	/**
	 * Mengembalikan kunci utama baris komentar ini.
	 *
	 * @return id baris komentar, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris komentar ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama pengirim komentar setelah dipangkas spasi tepinya. Lihat catatan pada
	 * javadoc kelas: nilai ini adalah identitas pengirim, bukan isi pesan komentar.
	 *
	 * @return nama pengirim komentar yang sudah dipangkas; wajib diisi menurut
	 *         {@code @Column(nullable = false)}
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama pengirim komentar.
	 *
	 * @param nama nama pengirim
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan alamat pengirim komentar.
	 *
	 * @return alamat; boleh {@code null}, dan dalam praktik saat ini selalu string kosong (lihat
	 *         catatan pada bidang {@link #alamat})
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menyetel alamat pengirim komentar.
	 *
	 * @param alamat alamat; boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan nomor kontak pengirim komentar.
	 *
	 * @return nomor kontak; boleh {@code null}
	 */
	public String getKontak() {
		return kontak;
	}

	/**
	 * Menyetel nomor kontak pengirim komentar.
	 *
	 * @param kontak nomor kontak; boleh {@code null}
	 */
	public void setKontak(String kontak) {
		this.kontak = kontak;
	}

	/**
	 * Mengembalikan alamat surel pengirim komentar.
	 *
	 * @return alamat surel; boleh {@code null}
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Menyetel alamat surel pengirim komentar.
	 *
	 * @param email alamat surel; boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengembalikan baris {@link ItemPunyaTerbit} (publikasi item) yang menjadi objek komentar ini.
	 * Dimuat dengan {@link FetchMode#SELECT}.
	 *
	 * @return publikasi item terkait, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_punya_terbit", nullable = true)
	public ItemPunyaTerbit getItemPunyaTerbit() {
		return itemPunyaTerbit;
	}

	/**
	 * Menyetel baris {@link ItemPunyaTerbit} yang menjadi objek komentar ini.
	 *
	 * @param itemPunyaTerbit publikasi item terkait; boleh {@code null}
	 */
	public void setItemPunyaTerbit(ItemPunyaTerbit itemPunyaTerbit) {
		this.itemPunyaTerbit = itemPunyaTerbit;
	}

}
