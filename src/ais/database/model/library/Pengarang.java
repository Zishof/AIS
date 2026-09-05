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
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;

/**
 * Entitas master <b>pengarang</b> (tabel {@code library.pengarang}) — data induk penulis buku/karya
 * ilmiah yang dapat dikaitkan ke banyak {@link Item} pustaka lewat entitas relasi
 * {@link ItemPunyaPengarang} (satu baris {@code item_punya_pengarang} per pasangan item-pengarang).
 * Hubungan item↔pengarang adalah <b>banyak-ke-banyak murni tanpa urutan</b>: satu item dapat memiliki
 * berapa pun baris {@link ItemPunyaPengarang}, satu pengarang dapat muncul di berapa pun item, dan
 * tidak ada kolom urutan/ordinal atau penanda "pengarang utama" vs "kontributor" — semua pengarang
 * yang tertaut pada sebuah item berkedudukan setara (lihat pemakaiannya di
 * {@code ais.action.master.library.helper.ItemPunyaPengarangHelper}, yang menyimpan tiap pengarang
 * terpilih sebagai baris {@link ItemPunyaPengarang} lepas tanpa indeks posisi).
 *
 * <h2>Tiga sumber identitas pengarang</h2>
 * <p>Baris pengarang dapat mewakili tiga jenis entitas berbeda, ditentukan dari isi
 * {@link #getMahasiswa()} dan {@link #getUserIdPengarang()}:</p>
 * <ul>
 * <li><b>Pengarang eksternal murni</b> — {@link #mahasiswa} dan {@link #userIdPengarang} sama-sama
 * kosong; {@link #getKode()}/{@link #getNama()} memakai nilai yang diketik manual pada layar
 * {@code PengarangAction} (mis. penulis buku dari luar institusi).</li>
 * <li><b>Pengarang = mahasiswa institusi</b> — {@link #mahasiswa} terisi (mis. penulis skripsi/karya
 * ilmiah mahasiswa); {@link #getNama()} dan {@link #getKode()} lalu mengambil alih nilai dari
 * {@link Mahasiswa#getNama()}/{@link Mahasiswa#getNim()} setiap kali dipanggil.</li>
 * <li><b>Pengarang = pengguna sistem</b> — {@link #userIdPengarang} terisi (mis. dosen/staf yang
 * juga pengguna AIS); {@link #getNama()} lalu mengambil alih dari {@link Tbmuser#getUserNama()}.</li>
 * </ul>
 * <p>Prioritas bila kedua relasi terisi sekaligus adalah {@link #mahasiswa} (dicek lebih dulu pada
 * {@link #getNama()} maupun {@link #getKode()}).</p>
 *
 * <h2>Getter dengan efek samping (pola berulang di modul ini)</h2>
 * <p>{@link #getNama()} dan {@link #getKode()} bukan getter murni: keduanya <b>menulis balik</b>
 * bidang {@link #nama}/{@link #kode} pada setiap pemanggilan bila entitas tertaut mahasiswa/pengguna,
 * sehingga nilai yang dipersisten oleh {@code dynamicUpdate} Hibernate bisa berubah hanya karena
 * getter dibaca (mis. saat merender label UI) sebelum flush — konsisten dengan pola "getter
 * destruktif" yang berulang di banyak entitas modul ini. Lihat catatan pada masing-masing method
 * untuk detail dan satu anomali salin-tempel yang ditemukan pada {@link #getKode()}.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see ItemPunyaPengarang
 * @see Item
 * @see Mahasiswa
 * @see Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "pengarang")



public class Pengarang extends GeneralValueObject {

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
	 * Mengembalikan id pengguna yang terakhir mengubah data pengarang ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah data pengarang ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan, agar bidang audit bayangan ini tidak pernah
	 * ditimpa kosong oleh jalur salin/klon objek.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah data pengarang ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah data pengarang ini.
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
	 * Mengembalikan stempel waktu perubahan terakhir baris pengarang ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: nama pengarang seperti dikembalikan {@link #nama} (bidang mentah,
	 * bukan hasil {@link #getNama()}), dipakai label bawaan komponen ZK dan penelusuran log.
	 *
	 * @return nama pengarang apa adanya; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Kode/identitas pendek pengarang; lihat catatan anomali pada {@link #getKode()}. */
	private String revisi;
	/** Kode/identitas pendek pengarang (NIM bila mahasiswa, tidak diisi otomatis bila pengguna sistem — lihat {@link #getKode()}). */
	private String kode;
	/** Nama pengarang; diganti dinamis oleh {@link #getNama()} bila tertaut {@link #mahasiswa}/{@link #userIdPengarang}. */
	private String nama;
	/** Catatan bebas tentang pengarang. */
	private String keterangan;
	/** Pengguna sistem (dosen/staf) yang diwakili baris pengarang ini, bila pengarang adalah pengguna AIS. */
	private Tbmuser userIdPengarang;
	/** Mahasiswa yang diwakili baris pengarang ini, bila pengarang adalah mahasiswa institusi. */
	private Mahasiswa mahasiswa;
	/** Status aktif pengarang; bawaan {@code true}, tidak ditegakkan mekanisme apa pun di kelas ini sendiri. */
	private Boolean aktif = true;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public Pengarang() {
	}

	/**
	 * Mengembalikan kunci utama baris pengarang ini.
	 *
	 * @return id pengarang, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris pengarang ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama pengarang, dengan tiga efek samping berurutan setiap kali dipanggil:
	 * (1) bila {@link #nama} kosong disetel jadi string kosong, (2) bila lebih dari 254 karakter
	 * dipotong jadi 253 karakter, lalu (3) <b>ditulis ulang</b> dari {@link Mahasiswa#getNama()} bila
	 * {@link #getMahasiswa()} tidak kosong, atau dari {@link Tbmuser#getUserNama()} bila
	 * {@link #getUserIdPengarang()} tidak kosong (mahasiswa diprioritaskan bila keduanya terisi).
	 * Karena langkah (3) dijalankan setelah pemotongan panjang di langkah (2), nama hasil override
	 * dari mahasiswa/pengguna tidak ikut dipotong 254 karakter. Nilai balik akhirnya selalu dipangkas
	 * spasi tepi ({@code trim()}).
	 *
	 * <p><b>Getter destruktif:</b> bidang {@link #nama} benar-benar ditimpa oleh pemanggilan ini,
	 * bukan sekadar dihitung untuk nilai balik — bila dipanggil sebelum flush Hibernate (mis. saat
	 * merender label UI), nilai baru ini pula yang akan dipersisten oleh {@code dynamicUpdate}.</p>
	 *
	 * @return nama pengarang yang sudah dipangkas spasi tepi; {@code null} hanya bila bidang mentah
	 *         null dan tidak ada override dari mahasiswa/pengguna (kondisi yang secara praktis tidak
	 *         tercapai karena langkah (1) selalu mengisi string kosong lebih dulu)
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama == null) {
			nama = "";
		}
		if (nama.length() > 254) {
			nama = nama.substring(0, 253);
		}

		if (getMahasiswa() != null) {
			nama = getMahasiswa().getNama();
		} else if (getUserIdPengarang() != null) {
			nama = getUserIdPengarang().getUserNama();
		}

		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama pengarang secara manual. Untuk pengarang yang tertaut {@link #mahasiswa} atau
	 * {@link #userIdPengarang}, nilai yang disetel di sini akan langsung ditimpa lagi pada
	 * pemanggilan {@link #getNama()} berikutnya.
	 *
	 * @param nama nama pengarang
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas tentang pengarang, dengan efek samping serupa {@link #getNama()}:
	 * bidang {@link #keterangan} yang {@code null} disetel jadi string kosong, dan yang lebih dari
	 * 254 karakter dipotong jadi 253 karakter, sebelum dikembalikan (tanpa {@code trim()}, berbeda
	 * dari {@link #getNama()}).
	 *
	 * @return catatan bebas tentang pengarang; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}
		if (keterangan.length() > 254) {
			keterangan = keterangan.substring(0, 253);
		}
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang pengarang.
	 *
	 * @param keterangan teks catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan pengguna sistem (dosen/staf) yang diwakili baris pengarang ini, atau
	 * {@code null} bila pengarang bukan pengguna AIS. Proxy lazy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return pengguna sistem terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id_pengarang", nullable = true)
	public Tbmuser getUserIdPengarang() {
		userIdPengarang = check(userIdPengarang);
		return userIdPengarang;
	}

	/**
	 * Menautkan baris pengarang ini ke pengguna sistem {@code userIdPengarang}. Menyetel nilai bukan
	 * {@code null} membuat {@link #getNama()} mengambil alih nama dari
	 * {@link Tbmuser#getUserNama()} pada pemanggilan berikutnya (kecuali {@link #mahasiswa} juga
	 * terisi, yang diprioritaskan).
	 *
	 * @param userIdPengarang pengguna sistem yang diwakili; {@code null} untuk melepas tautan
	 */
	public void setUserIdPengarang(Tbmuser userIdPengarang) {
		this.userIdPengarang = userIdPengarang;
	}

	/**
	 * Mengembalikan status aktif pengarang.
	 *
	 * @return {@code true} bila aktif atau bila bidang belum pernah disetel ({@code null} dianggap
	 *         aktif); {@code false} hanya bila eksplisit disetel {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif pengarang.
	 *
	 * @param aktif status aktif baru; {@code null} akan diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kode/identitas pendek pengarang. Bila {@link #getMahasiswa()} tidak kosong,
	 * bidang {@link #kode} ditimpa lebih dulu dengan {@link Mahasiswa#getNim()} sebelum
	 * dikembalikan (getter destruktif, sama seperti {@link #getNama()}).
	 *
	 * <p><b>Anomali salin-tempel:</b> pada cabang {@link #getUserIdPengarang()} tidak kosong,
	 * baris kode ini keliru menulis {@code nama = getUserIdPengarang().getUserId()} — menimpa bidang
	 * {@link #nama} (bukan {@link #kode}) dengan id pengguna. Akibatnya untuk pengarang yang
	 * tertaut pengguna sistem (dan tidak tertaut mahasiswa), {@link #kode} tidak pernah diisi
	 * otomatis dari sini (tetap memakai nilai manual/lama), sementara {@link #nama} sempat ditimpa
	 * id pengguna sesaat sebelum panggilan {@link #getNama()} berikutnya menimpanya kembali dengan
	 * {@link Tbmuser#getUserNama()}. Dalam alur normal (label UI selalu memanggil {@link #getNama()}
	 * setelahnya) dampaknya tidak terlihat pengguna, tetapi bila hanya {@link #getKode()} yang
	 * dipanggil sebelum flush, nilai {@link #nama} yang tersimpan bisa jadi id pengguna, bukan nama.
	 * Diperlakukan sebagai perilaku aktual, bukan diperbaiki di sini.</p>
	 *
	 * @return kode pengarang saat ini (NIM mahasiswa bila tertaut mahasiswa; nilai manual/lama untuk
	 *         kasus lain, termasuk pengarang yang tertaut pengguna sistem)
	 */
	public String getKode() {
		if (getMahasiswa() != null) {
			kode = getMahasiswa().getNim();
		} else if (getUserIdPengarang() != null) {
			nama = getUserIdPengarang().getUserId();
		}
		return kode;
	}

	/**
	 * Menyetel kode/identitas pendek pengarang secara manual.
	 *
	 * @param kode kode pengarang
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan bidang {@code revisi}. Tidak dipetakan ke kolom basis data manapun ({@code
	 * @Column} tidak ada) dan tidak ditemukan pemanggil {@link #setRevisi(String)}/{@link
	 * #getRevisi()} di luar kelas ini pada penelusuran kode saat ini — kemungkinan bidang yatim
	 * warisan dari revisi awal generator hbm2java.
	 *
	 * @return nilai {@code revisi} di memori; selalu {@code null} kecuali disetel manual dalam sesi
	 *         yang sama
	 */
	public String getRevisi() {
		return revisi;
	}

	/**
	 * Menyetel bidang {@code revisi} (lihat catatan pada {@link #getRevisi()} soal statusnya).
	 *
	 * @param revisi nilai baru
	 */
	public void setRevisi(String revisi) {
		this.revisi = revisi;
	}

	/**
	 * Mengembalikan mahasiswa yang diwakili baris pengarang ini, atau {@code null} bila pengarang
	 * bukan mahasiswa institusi. Proxy lazy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return mahasiswa terkait, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menautkan baris pengarang ini ke mahasiswa {@code mahasiswa}. Menyetel nilai bukan
	 * {@code null} membuat {@link #getNama()} dan {@link #getKode()} mengambil alih nama/NIM dari
	 * mahasiswa tersebut pada pemanggilan berikutnya, diprioritaskan di atas
	 * {@link #userIdPengarang}.
	 *
	 * @param mahasiswa mahasiswa yang diwakili; {@code null} untuk melepas tautan
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

}
