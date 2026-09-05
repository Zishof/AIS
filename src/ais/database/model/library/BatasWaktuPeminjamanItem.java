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

import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;



/**
 * Entity <b>aturan batas waktu dan kuota peminjaman</b> koleksi perpustakaan (tabel
 * {@code library.batas_waktu_peminjaman_item}). Satu baris menyatakan satu aturan yang berlaku
 * bagi sekelompok anggota: berapa hari sebuah koleksi boleh dipinjam, berapa banyak eksemplar
 * yang boleh dipegang serentak, dan berapa kali peminjaman boleh diperpanjang.
 *
 * <h3>Aturan ini benar-benar ditegakkan</h3>
 * <p>Berbeda dari sejumlah tabel referensi lain di modul {@code library} yang hanya bersifat
 * deskriptif, isi tabel ini <b>dibaca dan dipakai</b> oleh tiga jalur di
 * {@code ais.action.master.library.util.LibraryUtil}:</p>
 * <ul>
 *   <li>{@code getJumlahHariBatas(anggota, perpustakaan)} mengambil {@link #getJumlahHari()} dan
 *       menyalinnya ke {@link PeminjamanPengadaanItem#getJumlahHariBatas()}, yang kemudian
 *       menjadi dasar tenggat pada
 *       {@link PeminjamanPengadaanItemDetail#getBatasWaktupengembalian()};</li>
 *   <li>{@code getJumlahMaksimalPeminjaman(peminjaman)} mengambil
 *       {@link #getJumlahMaksimalItemYangDipinjam()}, dan {@code getKuota(...)} membandingkannya
 *       dengan cacah pinjaman berjalan; {@code helper/PeminjamanPengadaanItemPunyaItemHelper}
 *       <b>menolak</b> penambahan eksemplar bila kuota terlampaui;</li>
 *   <li>{@code getJumlahMaksimalPerpanjanganPeminjaman(peminjaman)} mengambil
 *       {@link #getJumlahMaksimalPerpanjanganPeminjaman()}, yang dipakai {@code LibraryMemberApi}
 *       dan helper pengembalian untuk <b>menolak</b> perpanjangan berikutnya.</li>
 * </ul>
 * <p>Karena aturan disalin ke dokumen peminjaman pada saat transaksi dibuat, mengubah baris di
 * sini <b>tidak</b> memengaruhi peminjaman yang sudah berjalan &mdash; perilaku yang memang
 * diinginkan agar tenggat yang sudah diberitahukan kepada anggota tidak berubah sepihak.</p>
 *
 * <h3>Cara satu aturan dipilih</h3>
 * <p>Pencocokan dilakukan lewat empat dimensi profil anggota &mdash; {@link #getJenisAnggota()},
 * {@link #getTipeAnggota()}, {@link #getFakultas()}, {@link #getJurusan()} &mdash; ditambah
 * {@link #getPerpustakaan() perpustakaan} dan {@link #getMulaiBerlaku() tanggal mulai berlaku}.
 * Keempat dimensi profil bersifat <b>nullable sebagai wildcard</b>: kriteria yang dipakai
 * berbentuk {@code (kolom = nilaiAnggota ATAU kolom IS NULL)}, sehingga baris yang mengosongkan
 * fakultas berlaku untuk seluruh fakultas. Perpustakaan tidak demikian &mdash; ia dicocokkan
 * dengan {@code =} sehingga setiap perpustakaan wajib punya aturannya sendiri.</p>
 * <p>Dimensi kelima, {@link #getBerlakuUntukSemester() berlakuUntukSemester}, hanya dipakai untuk
 * mahasiswa: kriterianya {@code berlakuUntukSemester <= semesterBerjalan}. Untuk anggota yang
 * bukan mahasiswa, kriterianya {@code berlakuUntukSemester IS NULL}. Bila pencarian putaran
 * pertama tidak menemukan apa pun, {@code LibraryUtil} mengulanginya dengan syarat
 * {@code berlakuUntukSemester IS NULL} sebagai cadangan. Hasil diurutkan menurun berdasarkan
 * {@code mulaiBerlaku} dan hanya satu baris teratas yang dipakai.</p>
 *
 * <p><b>Bila tidak ada aturan yang cocok, hasilnya nol &mdash; bukan nilai baku kelas ini.</b>
 * Ini perbedaan penting yang mudah luput. Getter pada kelas ini menormalkan {@code null} ke nilai
 * baku yang wajar ({@code 7} hari, {@code 10} eksemplar, {@code 1} perpanjangan), tetapi
 * {@code LibraryUtil} <b>tidak pernah memuat entity ini</b>; ia memakai
 * {@code Projections.property(...)} yang membaca kolom mentah dari basis data. Akibatnya kolom
 * yang bernilai {@code NULL} di basis data, dan juga keadaan "tidak ada baris yang cocok",
 * sama-sama menghasilkan {@code 0} pada pemanggil &mdash; bukan {@code 7}, {@code 10}, atau
 * {@code 1}. Nilai baku pada getter hanya berlaku bagi formulir ZK yang memuat entity secara
 * utuh. Konsekuensi nyatanya: perpustakaan yang belum mengisi tabel ini memberikan tenggat nol
 * hari (setiap pinjaman langsung terlambat) dan kuota nol eksemplar, secara senyap.</p>
 *
 * <p><b>{@link #getDefaultItem() defaultItem} adalah penanda tidur.</b> Field ini tidak pernah
 * dibaca oleh kriteria pencarian mana pun di {@code LibraryUtil}, dan
 * {@code BatasWaktuPeminjamanItemAction} pun tidak menyediakan kendali untuk mengubahnya.
 * Nilainya terinisialisasi {@code true} pada objek baru lalu tersimpan, tetapi tidak memengaruhi
 * apa pun. Pola yang sama terdapat pada {@link DendaKeterlambatanItem#getDefaultItem()}.</p>
 *
 * <p><b>Efek samping.</b> Sebagian getter menormalkan {@code null} dan menulis hasilnya balik ke
 * field; lihat catatan pada masing-masing. Persistence, transaksi, otorisasi, dan pemuatan relasi
 * lazy tetap menjadi tanggung jawab DAO/service dengan session aktif; jangan menaruh query
 * duplikat pada model.</p>
 *
 * @see DendaKeterlambatanItem
 * @see PeminjamanPengadaanItem
 * @see PeminjamanPengadaanItemDetail
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "batas_waktu_peminjaman_item")



public class BatasWaktuPeminjamanItem extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan di seluruh entity modul
	 * {@code library} karena kelas-kelas ini dibangkitkan dari template yang sama; jangan
	 * diubah agar sesi ZK/HTTP yang sudah terserialisasi tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (surrogate key) baris ini, dibangkitkan oleh database. */
	private Long id;
	/** Nama pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** ID pengguna aplikasi yang terakhir mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {return olehId;}

	/**
	 * Menyetel ID pengguna pengubah terakhir. Bersifat <b>no-op bila nilai baru kosong atau
	 * hanya berisi spasi</b> agar jejak audit lama tidak tertimpa oleh pemanggil tanpa konteks
	 * pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna pengubah terakhir; no-op bila nilai baru kosong/blank.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/blank.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna aplikasi yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}. Dipanggil Hibernate tepat sebelum {@code UPDATE},
	 * lalu mendelegasikan pengisian trio field audit kepada
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat dan
	 * diperbarui oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah cap waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks aturan untuk combobox, listbox, dan log.
	 *
	 * <p><b>Hanya menampilkan perpustakaan.</b> Seluruh dimensi yang justru membedakan satu
	 * aturan dari aturan lain &mdash; jenis anggota, tipe anggota, fakultas, jurusan, semester,
	 * tanggal mulai berlaku &mdash; tidak ikut tampil. Akibatnya semua aturan milik satu
	 * perpustakaan terlihat identik di daftar dan tidak dapat dibedakan tanpa membuka
	 * rinciannya. Membaca lewat field langsung sehingga tidak memicu inisialisasi proxy.</p>
	 *
	 * @return nama perpustakaan dirangkai dengan {@code ""}; dapat berupa string {@code "null"}.
	 */
	public String toString() {
		return perpustakaan + "";
	}

	/** Tanggal aturan mulai berlaku; dipakai untuk memilih aturan terbaru yang sudah berlaku. */
	private Date mulaiBerlaku = ais.ui.util.WaktuUtil.getDate();
	/** Durasi pinjam dasar dalam hari kerja. */
	private Integer jumlahHari = 7;
	/** Ambang semester minimal agar aturan berlaku bagi mahasiswa; {@code null}/0 berarti tanpa syarat. */
	private Integer berlakuUntukSemester;
	/** Kuota eksemplar yang boleh dipegang serentak seorang anggota. */
	private Integer jumlahMaksimalItemYangDipinjam = 10;
	/** Batas berapa kali satu peminjaman boleh diperpanjang. */
	private Integer jumlahMaksimalPerpanjanganPeminjaman = 1;
	/** Perpustakaan tempat aturan ini berlaku; dicocokkan dengan {@code =}, bukan wildcard. */
	private Perpustakaan perpustakaan;
	/** Jenis anggota sasaran; {@code null} berarti berlaku untuk semua jenis. */
	private JenisAnggota jenisAnggota;
	/** Tipe anggota sasaran; {@code null} berarti berlaku untuk semua tipe. */
	private TipeAnggota tipeAnggota;
	/** Fakultas sasaran; {@code null} berarti berlaku untuk semua fakultas. */
	private Fakultas fakultas;
	/** Jurusan sasaran; {@code null} berarti berlaku untuk semua jurusan. */
	private Jurusan jurusan;
	/** Catatan bebas mengenai aturan ini. */
	private String keterangan;
	/** Penanda tidur: tersimpan namun tidak pernah dibaca kriteria pencarian mana pun. */
	private Boolean defaultItem = true;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate/ZK untuk instansiasi via refleksi.
	 * Nilai baku yang sudah terpasang &mdash; berlaku mulai hari ini, 7 hari, 10 eksemplar,
	 * 1 perpanjangan &mdash; hanya berlaku bagi objek yang dibuat lewat konstruktor ini,
	 * bukan bagi baris yang dimuat dari basis data maupun bagi pemanggil
	 * {@code LibraryUtil} yang membaca kolom mentah.
	 */
	public BatasWaktuPeminjamanItem() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return ID baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate setelah {@code INSERT}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas mengenai aturan ini.
	 *
	 * <p>Karena {@link #toString()} tidak menampilkan dimensi pembeda apa pun, keterangan yang
	 * bermakna di sini adalah satu-satunya cara praktis membedakan aturan-aturan sejenis pada
	 * layar daftar.</p>
	 *
	 * @return keterangan bebas, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas mengenai aturan ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda {@code defaultItem}, dinormalkan ke {@code false} bila belum diisi.
	 *
	 * <p><b>Penanda ini tidur.</b> Tidak ada satu pun kriteria pencarian aturan di
	 * {@code LibraryUtil} yang menyaring berdasarkan nilai ini, dan
	 * {@code BatasWaktuPeminjamanItemAction} tidak menyediakan kendali untuk mengubahnya.
	 * Nilainya terinisialisasi {@code true} pada objek baru lalu ikut tersimpan, tetapi tidak
	 * memengaruhi perilaku apa pun. Bila kelak dipakai untuk menandai "aturan bawaan", perlu
	 * diingat bahwa data yang sudah ada praktis seluruhnya bernilai {@code true}. Pola yang sama
	 * terdapat pada {@link DendaKeterlambatanItem#getDefaultItem()}.</p>
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return penanda {@code defaultItem}; tidak pernah {@code null}.
	 */
	public Boolean getDefaultItem() {
		if (defaultItem == null) {
			defaultItem = false;
		}
		return defaultItem;
	}

	/**
	 * Menyetel penanda {@code defaultItem}. Lihat catatan pada {@link #getDefaultItem()}:
	 * nilai ini tersimpan namun tidak memengaruhi perilaku apa pun.
	 *
	 * @param defaultItem penanda baru.
	 */
	public void setDefaultItem(Boolean defaultItem) {
		this.defaultItem = defaultItem;
	}

	/**
	 * Mengembalikan tanggal aturan mulai berlaku.
	 *
	 * <p>Kriteria pencarian menyaring dengan {@code mulaiberlaku <= CURRENT_DATE} lalu
	 * mengurutkan menurun pada kolom ini dan mengambil satu baris teratas. Artinya
	 * <b>aturan berlaku terbaru yang menang</b>, dan aturan dengan tanggal di masa depan tidak
	 * ikut terpilih sampai tanggalnya tiba &mdash; mekanisme yang memungkinkan perubahan
	 * kebijakan dijadwalkan lebih awal.</p>
	 *
	 * <p>Dipetakan {@link TemporalType#DATE} sehingga hanya komponen tanggal yang tersimpan.
	 * Getter tidak menormalkan {@code null}; baris tanpa tanggal mulai tidak akan pernah lolos
	 * kriteria pencarian dan karenanya menjadi aturan mati.</p>
	 *
	 * @return tanggal mulai berlaku, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulaiBerlaku() {
		return mulaiBerlaku;
	}

	/**
	 * Menyetel tanggal aturan mulai berlaku.
	 *
	 * @param mulaiBerlaku tanggal mulai berlaku.
	 */
	public void setMulaiBerlaku(Date mulaiBerlaku) {
		this.mulaiBerlaku = mulaiBerlaku;
	}

	/**
	 * Mengembalikan durasi pinjam dasar dalam hari kerja, dinormalkan ke {@code 7} bila belum
	 * diisi.
	 *
	 * <p>Nilai inilah yang disalin {@code LibraryUtil.getJumlahHariBatas(...)} ke
	 * {@link PeminjamanPengadaanItem#getJumlahHariBatas()} pada saat peminjaman dibuat, lalu
	 * dikalikan {@code (jumlahPerpanjangan + 1)} untuk memperoleh tenggat efektif.</p>
	 *
	 * <p><b>Nilai baku 7 tidak sampai ke pemanggil.</b> {@code LibraryUtil} membaca kolomnya
	 * lewat {@code Projections.property("jumlahHari")}, bukan dengan memuat entity ini, sehingga
	 * normalisasi pada getter dilewati sama sekali. Kolom {@code NULL} di basis data
	 * menghasilkan {@code 0} pada pemanggil &mdash; yang berarti tanpa tenggat dan setiap
	 * pinjaman langsung terhitung terlambat. Nilai baku {@code 7} hanya terlihat pada formulir
	 * ZK yang memuat entity utuh.</p>
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return durasi pinjam dasar dalam hari kerja; tidak pernah {@code null}.
	 */
	@Column(name = "jumlah_hari", nullable = true)
	public Integer getJumlahHari() {
		if (jumlahHari == null) {
			jumlahHari = 7;
		}
		return jumlahHari;
	}

	/**
	 * Menyetel durasi pinjam dasar dalam hari kerja.
	 *
	 * <p><b>Catatan integritas:</b> setter tidak menolak nilai nol maupun negatif. Nol berarti
	 * seluruh pinjaman terhitung terlambat sejak hari pertama; nilai negatif membuat tenggat
	 * jatuh sebelum tanggal peminjaman.</p>
	 *
	 * @param jumlahHari durasi pinjam dasar dalam hari kerja.
	 */
	public void setJumlahHari(Integer jumlahHari) {
		this.jumlahHari = jumlahHari;
	}

	/**
	 * Mengembalikan perpustakaan tempat aturan ini berlaku, dengan <b>pengisian otomatis</b>
	 * dari sesi bila belum diisi.
	 *
	 * <p>Alur getter ini: (1) bila field masih {@code null}, ambil perpustakaan aktif melalui
	 * {@link Common#getCurrentPerpustakaan()}; (2) jalankan {@code check(...)} milik
	 * {@link GeneralValueObject}. Hasilnya ditulis balik ke field, sehingga getter ini mengubah
	 * state objek (getter destruktif ringan).</p>
	 *
	 * <p><b>Perpustakaan bukan wildcard.</b> Berbeda dari keempat dimensi profil anggota,
	 * kriteria pencarian mencocokkan kolom ini dengan {@code =}. Aturan yang perpustakaannya
	 * kosong karenanya tidak akan pernah terpilih untuk perpustakaan mana pun, dan setiap
	 * perpustakaan wajib memiliki barisnya sendiri.</p>
	 *
	 * @return perpustakaan tempat aturan berlaku; dapat {@code null} bila sesi juga tidak
	 *         memilikinya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perpustakaan", nullable = true)
	public Perpustakaan getPerpustakaan() {
		if (perpustakaan == null) {
			perpustakaan = Common.getCurrentPerpustakaan();
		}
		perpustakaan = check(perpustakaan);
		return perpustakaan;
	}

	/**
	 * Menyetel perpustakaan tempat aturan ini berlaku.
	 *
	 * @param perpustakaan perpustakaan sasaran.
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	/**
	 * Mengembalikan batas berapa kali satu peminjaman boleh diperpanjang, dinormalkan ke
	 * {@code 1} bila belum diisi.
	 *
	 * <p>Nilai ini dibaca {@code LibraryUtil.getJumlahMaksimalPerpanjanganPeminjaman(...)} lalu
	 * disalin ke {@link PeminjamanPengadaanItemDetail#getJumlahMaxPerpanjangan()}.
	 * <b>Penegakannya nyata:</b> {@code LibraryMemberApi} membandingkan cacah perpanjangan
	 * terpakai dengan batas ini dan menolak perpanjangan berikutnya bila sudah tercapai.</p>
	 *
	 * <p>Sama seperti {@link #getJumlahHari()}, nilai baku {@code 1} tidak sampai ke pemanggil
	 * karena {@code LibraryUtil} membaca kolom mentah lewat proyeksi; kolom {@code NULL}
	 * menghasilkan {@code 0}, yang berarti perpanjangan tidak diizinkan sama sekali.</p>
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return batas perpanjangan; tidak pernah {@code null}.
	 */
	public Integer getJumlahMaksimalPerpanjanganPeminjaman() {
		if (jumlahMaksimalPerpanjanganPeminjaman == null) {
			jumlahMaksimalPerpanjanganPeminjaman = 1;
		}
		return jumlahMaksimalPerpanjanganPeminjaman;
	}

	/**
	 * Menyetel batas berapa kali satu peminjaman boleh diperpanjang.
	 *
	 * @param jumlahMaksimalPerpanjanganPeminjaman batas perpanjangan.
	 */
	public void setJumlahMaksimalPerpanjanganPeminjaman(
			Integer jumlahMaksimalPerpanjanganPeminjaman) {
		this.jumlahMaksimalPerpanjanganPeminjaman = jumlahMaksimalPerpanjanganPeminjaman;
	}

	/**
	 * Mengembalikan kuota eksemplar yang boleh dipegang serentak seorang anggota, dinormalkan
	 * ke {@code 10} bila belum diisi.
	 *
	 * <p>Nilai ini dibaca {@code LibraryUtil.getJumlahMaksimalPeminjaman(...)} dan
	 * {@code PerpustakaanResource}, lalu disalin ke
	 * {@link PeminjamanPengadaanItem#getJumlahMaksimalPeminjaman()}. <b>Penegakannya nyata:</b>
	 * {@code LibraryUtil.getKuota(...)} menghitung selisih antara cacah baris peminjaman dan
	 * cacah baris pengembalian milik anggota pada perpustakaan tersebut, dan
	 * {@code helper/PeminjamanPengadaanItemPunyaItemHelper} menolak penambahan eksemplar bila
	 * kuota terlampaui.</p>
	 *
	 * <p>Sama seperti {@link #getJumlahHari()}, nilai baku {@code 10} tidak sampai ke pemanggil
	 * karena pembacaan dilakukan lewat proyeksi kolom mentah; kolom {@code NULL} menghasilkan
	 * {@code 0}, yang berarti anggota tidak boleh meminjam apa pun.</p>
	 *
	 * <p>Normalisasi ditulis balik ke field, sehingga getter ini mengubah state objek.</p>
	 *
	 * @return kuota eksemplar serentak; tidak pernah {@code null}.
	 */
	public Integer getJumlahMaksimalItemYangDipinjam() {
		if (jumlahMaksimalItemYangDipinjam == null) {
			jumlahMaksimalItemYangDipinjam = 10;
		}
		return jumlahMaksimalItemYangDipinjam;
	}

	/**
	 * Menyetel kuota eksemplar yang boleh dipegang serentak seorang anggota.
	 *
	 * @param jumlahMaksimalItemYangDipinjam kuota eksemplar serentak.
	 */
	public void setJumlahMaksimalItemYangDipinjam(
			Integer jumlahMaksimalItemYangDipinjam) {
		this.jumlahMaksimalItemYangDipinjam = jumlahMaksimalItemYangDipinjam;
	}

	/**
	 * Mengembalikan ambang semester minimal agar aturan berlaku bagi mahasiswa, dengan
	 * <b>{@code 0} diperlakukan sama dengan {@code null}</b>.
	 *
	 * <p>Getter memetakan nilai nol menjadi {@code null} agar formulir menampilkan kolom ini
	 * sebagai "tanpa syarat semester" alih-alih angka nol yang membingungkan. Normalisasi
	 * <b>tidak</b> ditulis balik ke field, jadi getter ini tidak mengubah state.</p>
	 *
	 * <p><b>Kriteria pencarian tidak ikut memakai aturan ini.</b> {@code LibraryUtil} membaca
	 * kolom mentah lewat {@code Restrictions.le("berlakuUntukSemester", semesterBerjalan)} untuk
	 * mahasiswa dan {@code Restrictions.isNull("berlakuUntukSemester")} untuk non-mahasiswa.
	 * Akibatnya baris yang menyimpan angka {@code 0} <em>tetap</em> lolos kriteria mahasiswa
	 * (karena {@code 0 <= semesterBerjalan}) namun <em>gagal</em> kriteria non-mahasiswa (karena
	 * kolomnya bukan {@code NULL}) &mdash; padahal lewat formulir kedua keadaan itu tampak
	 * sama-sama "tanpa syarat". Simpanlah {@code null}, bukan {@code 0}, bila memang tidak ingin
	 * membatasi semester.</p>
	 *
	 * @return ambang semester minimal, atau {@code null} bila tanpa syarat semester (termasuk
	 *         bila nilai tersimpannya {@code 0}).
	 */
	public Integer getBerlakuUntukSemester() {
		return berlakuUntukSemester == null || berlakuUntukSemester.equals(0) ? null
				: berlakuUntukSemester;
	}

	/**
	 * Menyetel ambang semester minimal agar aturan berlaku bagi mahasiswa.
	 *
	 * <p>Lihat catatan pada {@link #getBerlakuUntukSemester()}: menyetel {@code 0} tidak sama
	 * dengan menyetel {@code null} sejauh menyangkut kriteria pencarian, meski keduanya terbaca
	 * sama lewat getter.</p>
	 *
	 * @param berlakuUntukSemester ambang semester minimal; {@code null} berarti tanpa syarat.
	 */
	public void setBerlakuUntukSemester(Integer berlakuUntukSemester) {
		this.berlakuUntukSemester = berlakuUntukSemester;
	}

	/**
	 * Mengembalikan jenis anggota sasaran aturan ini.
	 *
	 * <p>Bernilai {@code null} berarti aturan berlaku untuk semua jenis anggota: kriteria
	 * pencarian berbentuk {@code (jenisAnggota = milikAnggota ATAU jenisAnggota IS NULL)}.
	 * Relasi dipetakan {@link FetchMode#SELECT} sehingga aman dibaca dari renderer.</p>
	 *
	 * @return jenis anggota sasaran, atau {@code null} untuk semua jenis.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_anggota", nullable = true)
	public JenisAnggota getJenisAnggota() {
		return jenisAnggota;
	}

	/**
	 * Menyetel jenis anggota sasaran aturan ini.
	 *
	 * @param jenisAnggota jenis anggota sasaran; {@code null} berarti semua jenis.
	 */
	public void setJenisAnggota(JenisAnggota jenisAnggota) {
		this.jenisAnggota = jenisAnggota;
	}

	/**
	 * Mengembalikan tipe anggota sasaran aturan ini.
	 *
	 * <p>Bernilai {@code null} berarti aturan berlaku untuk semua tipe anggota, dengan pola
	 * kriteria wildcard yang sama seperti {@link #getJenisAnggota()}.</p>
	 *
	 * @return tipe anggota sasaran, atau {@code null} untuk semua tipe.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "tipe_anggota", nullable = true)
	public TipeAnggota getTipeAnggota() {
		return tipeAnggota;
	}

	/**
	 * Menyetel tipe anggota sasaran aturan ini.
	 *
	 * @param tipeAnggota tipe anggota sasaran; {@code null} berarti semua tipe.
	 */
	public void setTipeAnggota(TipeAnggota tipeAnggota) {
		this.tipeAnggota = tipeAnggota;
	}

	/**
	 * Mengembalikan fakultas sasaran aturan ini.
	 *
	 * <p>Fakultas anggota tidak disimpan pada {@link Anggota} melainkan diturunkan
	 * {@code LibraryUtil} dari mahasiswa (lewat jurusannya) atau dari dosen yang tertaut.
	 * Bernilai {@code null} berarti aturan berlaku untuk semua fakultas.</p>
	 *
	 * @return fakultas sasaran, atau {@code null} untuk semua fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * Menyetel fakultas sasaran aturan ini.
	 *
	 * @param fakultas fakultas sasaran; {@code null} berarti semua fakultas.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan jurusan sasaran aturan ini.
	 *
	 * <p>Seperti {@link #getFakultas()}, jurusan anggota diturunkan {@code LibraryUtil} dari
	 * mahasiswa atau dosen yang tertaut. Bernilai {@code null} berarti aturan berlaku untuk
	 * semua jurusan.</p>
	 *
	 * @return jurusan sasaran, atau {@code null} untuk semua jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menyetel jurusan sasaran aturan ini.
	 *
	 * @param jurusan jurusan sasaran; {@code null} berarti semua jurusan.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

}
