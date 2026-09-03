package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h2>Grup Item Biaya Sekolah — kepala pengelompok tampilan untuk item biaya sekolah</h2>
 *
 * <p>Entity master kecil yang memetakan tabel <code>sekolah.grup_item_biaya_sekolah</code>. Perannya
 * <b>murni pengelompokan tampilan</b>: ia tidak menyimpan tarif, tidak menyimpan periode, tidak
 * menyimpan akun jurnal, dan tidak pernah ikut dalam perhitungan nominal apa pun. Satu-satunya
 * fungsinya adalah memberi <i>judul kelompok</i> pada daftar tagihan yang dilihat siswa/orang tua,
 * sehingga puluhan baris item biaya tidak tampil sebagai satu daftar datar yang panjang.</p>
 *
 * <h3>Relasi ke {@link ItemBiayaSekolah} (TERVERIFIKASI dari kode)</h3>
 * <p>Arah kepemilikan relasi ada di <b>sisi item</b>, bukan di sini:</p>
 * <ul>
 *   <li>{@code ItemBiayaSekolah} mendeklarasikan {@code @ManyToOne} bernama
 *       {@code grupItemBiayaSekolah} dengan {@code @JoinColumn(name = "grup_item_biaya_sekolah_id")}
 *       — kolom kunci asing fisiknya berada di tabel <code>sekolah.item_biaya_sekolah</code>.</li>
 *   <li>Kelas ini hanya menyediakan sisi terbalik {@link #getItemBiayaSekolahs()}, yaitu
 *       {@code @OneToMany(mappedBy = "grupItemBiayaSekolah")} — <b>tidak memiliki</b> relasi
 *       (Hibernate mengabaikan perubahan pada koleksi ini saat menulis).</li>
 * </ul>
 * <p>Kardinalitasnya karena itu: <b>satu grup dipakai banyak item; satu item berada pada paling
 * banyak satu grup</b> (kolom FK boleh {@code NULL} = "Tanpa Grup"). Keanggotaan grup TIDAK dikelola
 * dari layar grup ini, melainkan dari form Item Biaya: {@code ItemBiayaSekolahAction} menyediakan
 * combo "Grup Item Biaya" yang diisi lewat {@code Common.insertComboDanSemua(...)} dengan pilihan
 * tambahan "Tanpa Grup". Combo tersebut disaring ketat dengan {@code Restrictions.eq("sekolah", s)}
 * — grup dari sekolah lain <b>tidak</b> bisa dipilih di sana (contoh POSITIF penyaringan tenant).</p>
 *
 * <h3>Siapa yang benar-benar memakai grup ini (rantai billing HIDUP PENUH)</h3>
 * <ol>
 *   <li><b>{@code ais.action.master.sekolah.helper.PembayaranOnline}</b> — layar pembayaran online
 *       siswa. Daftar tagihan diurut ulang memakai {@link #getLabelTampilan()} (huruf kecil; tagihan
 *       tanpa grup diberi kunci berisi satu karakter U+FFFF sehingga selalu jatuh paling akhir), lalu setiap
 *       kali id grup berganti disisipkan satu baris kepala {@code MyGroupConfig} berlatar gelap
 *       berisi label grup.</li>
 *   <li><b>{@code ais.action.servlet.api.TagihanApiGrupUtil#putGrup}</b> — kontrak JSON pengelompokan
 *       tagihan untuk seluruh API sekolah. Dipanggil dari {@code TagihanSiswa} (6 titik) dan
 *       {@code PsbCalonApi} (1 titik); mengisi {@code grup_id}, {@code grup_key} ("item:&lt;id&gt;"),
 *       {@code grup_kode}, {@code grup_nama}, {@code grup_ta}, serta {@code grup_item_biaya_aktif}.
 *       Bila grup tidak ada <i>atau tidak aktif</i>, API jatuh ke pengelompokan lama berbasis
 *       {@link PengaturanBiaya}.</li>
 *   <li><b>{@code ais.action.master.sekolah.ItemBiayaSekolahAction}</b> — combo pemilih grup pada form
 *       item, plus label "Grup: &lt;label&gt;" pada daftar item.</li>
 *   <li><b>{@code ais.action.master.sekolah.GrupItemBiayaSekolahAction}</b> — layar CRUD grup itu
 *       sendiri (turunan {@code GenericCrudAction}).</li>
 * </ol>
 *
 * <h3>Peringatan: gerbang {@code aktif} TIDAK konsisten antar pemakai</h3>
 * <p>{@code TagihanApiGrupUtil} menolak grup yang {@code getAktif()}-nya {@code false} dan mundur ke
 * pengelompokan {@code PengaturanBiaya}. {@code PembayaranOnline} <b>tidak</b> melakukan pengecekan
 * itu sama sekali — baik comparator pengurut maupun penyisip baris kepala hanya memeriksa
 * {@code grupItem != null &amp;&amp; grupItem.getId() != null}. Akibatnya grup yang sudah dinonaktifkan
 * tetap tampil sebagai judul kelompok di layar pembayaran online, sementara respons API untuk data
 * yang sama sudah tidak lagi mengenal grup tersebut. Menonaktifkan grup karena itu bukan cara yang
 * andal untuk menyembunyikannya dari siswa.</p>
 *
 * <h3>Peringatan: pengurutan per-grup bertabrakan dengan {@code break} kronologis</h3>
 * <p>Perulangan render di {@code PembayaranOnline} mewarisi logika lama (r74892) untuk periode
 * "Bulanan": begitu ditemukan tagihan dengan {@code tahunbulan} melewati
 * {@code PengaturanBiaya.getBulanSampai()}, perulangan di-{@code break}. Logika itu benar selama
 * daftar tersusun kronologis. Sejak pengurutan per-grup ditambahkan (r83433) daftar disusun
 * berdasarkan label grup lebih dahulu, sehingga {@code break} tersebut dapat memutus perulangan di
 * tengah — <b>seluruh grup yang urutan labelnya berada sesudahnya tidak pernah dirender</b>. Efeknya
 * pada layar finansial: sebagian tagihan diam-diam tidak muncul, dan karenanya tidak bisa dibayar.
 * Ini catatan bagi pembaca kode; perbaikannya ada di sisi {@code PembayaranOnline}, bukan di entity
 * ini.</p>
 *
 * <h3>Catatan hak akses (rantai KEUANGAN — hasil audit)</h3>
 * <ul>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Berkas
 *       <code>/WEB-INF/z/x/y/pages/master/sekolah/grup_item_biaya_sekolah.zul</code> tidak memiliki
 *       entri menu sendiri di repo; satu-satunya pintu masuknya adalah tab kedua
 *       ("Grup Item Biaya Sekolah") pada <code>item_biaya_sekolah.zul</code> lewat
 *       {@code &lt;include&gt;}. Karena {@code CommonPrivilages.checkPrevilages(...)} membaca
 *       {@code Common.getCurrentMenu()} — menu halaman yang sedang aktif, yaitu menu "Item Biaya
 *       Sekolah" — hak Ubah/Hapus atas <i>item biaya</i> otomatis juga berlaku sebagai hak
 *       Ubah/Hapus atas <i>grup</i>. Tidak ada hak terpisah yang bisa diberikan admin. Varian ini
 *       relatif jinak (kedua layar sama-sama konfigurasi billing sekolah), tetapi mekanismenya sama
 *       persis dengan instance-instance lain yang sudah terdaftar dalam audit menyeluruh.</li>
 *   <li><b>Fail-open cakupan tenant pada daftar.</b> {@code GrupItemBiayaSekolahAction.initCriteria}
 *       hanya memasang penyaring yang berasal dari kotak pencarian (nama/kode, yayasan, sekolah,
 *       "hanya grup aktif") dan <b>tidak</b> membatasi hasil ke yayasan/sekolah pengguna yang login.
 *       Pengguna sekolah A yang bisa membuka tab ini melihat, mengubah, menonaktifkan, dan menghapus
 *       grup milik seluruh sekolah dalam satu instalasi. Perilaku ini identik dengan layar induknya
 *       ({@code ItemBiayaSekolahAction.initCriteria} juga tanpa batas tenant bawaan), jadi ini pola
 *       arsitektur layar master sekolah, bukan kekhususan grup.</li>
 *   <li><b>Sisi POSITIF yang sudah diverifikasi.</b> Tombol Tambah bergerbang {@code CREATE};
 *       {@code edit}/{@code delete} diisi dari {@code UPDATE}/{@code DELETE} dan diteruskan ke
 *       {@code Common.copyEditDeleteButtons} (yang benar-benar menyembunyikan tombol saat flag
 *       {@code false}); checkbox "Aktif" di baris daftar memakai {@code setDisabled(!edit)}; tombol
 *       Unduh/Unggah hanya dipasang bila {@code add.isVisible() &amp;&amp; edit &amp;&amp; delete}.
 *       Tidak ditemukan tombol tanpa gerbang di layar ini.</li>
 *   <li><b>Tidak terkena bug penciutan {@code TreeSet}.</b> Kelas ini tidak meng-override
 *       {@code getNomorUrut()} dan koleksi anggotanya adalah {@link HashSet}, bukan {@code TreeSet};
 *       {@link GeneralValueObject#compareTo(GeneralValueObject)} tidak pernah dipakai untuk menyimpan
 *       grup. Pola batch 55/59 TIDAK berlaku di sini.</li>
 *   <li><b>Keunikan kode hanya ditegakkan aplikasi.</b> {@code onSave()} pada Action menolak kode
 *       yang sudah dipakai pada sekolah yang sama, tetapi tidak ada <i>unique constraint</i> di
 *       basis data — penulisan lewat unggahan massal, SQL mentah, atau dua sesi bersamaan tetap bisa
 *       menghasilkan kode kembar.</li>
 * </ul>
 *
 * <h3>Catatan teknis: mengapa {@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code id}
 * dideklarasikan ulang</h3>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun propertinya. Deklarasi ulang
 * keempat anggota di bawah karena itu <b>keharusan teknis, bukan duplikasi yang lupa dibersihkan</b>:
 * tanpa deklarasi ulang, kolom {@code oleh}, {@code oleh_id}, dan {@code tanggal_dirubah} tidak akan
 * pernah dipetakan dan jejak audit hilang. Berbeda dengan beberapa entity lain di paket ini,
 * {@link #getKeterangan()} di sini <b>sudah</b> dipetakan dengan benar ({@code @Column}) sehingga
 * isian keterangan tidak hilang antar-request.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; kunci utama:</b> {@link #getId()}, {@link #setId(Long)}.</li>
 *   <li><b>Atribut bisnis:</b> {@link #getKode()}, {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #getAktif()} beserta setter-nya.</li>
 *   <li><b>Cakupan tenant:</b> {@link #getYayasan()}, {@link #getSekolah()} beserta setter-nya.</li>
 *   <li><b>Relasi anggota (read-only):</b> {@link #getItemBiayaSekolahs()},
 *       {@link #setItemBiayaSekolahs(Set)}.</li>
 *   <li><b>Tampilan:</b> {@link #getLabelTampilan()}, {@link #toString()}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}
 *       beserta setter-nya, dan kait daur hidup {@code onUpdate()}.</li>
 * </ul>
 *
 * <p><b>Riwayat:</b> entity ini relatif BARU — ditambahkan pada r83421 (2 Sep 2026) bersama
 * {@code GrupItemBiayaSekolahAction}, kolom FK di {@code ItemBiayaSekolah}, dan pendaftarannya di
 * {@code hibernate.cfg.xml}; {@code @Transient} pada {@link #getLabelTampilan()} menyusul pada
 * r83444. Tabelnya dibuat oleh {@code hbm2ddl.auto=update}, bukan skrip migrasi tersendiri. Jadi ini
 * bukan peninggalan generator {@code hbm2java} seperti mayoritas entity di paket ini.</p>
 *
 * @see ItemBiayaSekolah
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "grup_item_biaya_sekolah", schema = "sekolah")
public class GrupItemBiayaSekolah extends GeneralValueObject {

	/** Versi serialisasi. Nilainya sengaja berdekatan dengan {@code GrupItemBiayaSekolahAction} (…034 vs …035) karena keduanya lahir pada commit yang sama. */
	private static final long serialVersionUID = 4800716294061911034L;
	/** Kunci utama, dibangkitkan basis data. Dideklarasikan ulang karena {@link GeneralValueObject} tidak dipetakan Hibernate. */
	private Long id;
	/** Kode singkat grup, tampil sebagai awalan pada {@link #getLabelTampilan()}. Unik per sekolah (ditegakkan aplikasi saja). */
	private String kode;
	/** Nama grup, mis. "Biaya KBM Pondok". Bagian kedua dari label tampilan. */
	private String nama;
	/** Catatan bebas; ikut ditampilkan sebagai deskripsi pada combo pemilih grup di form Item Biaya. */
	private String keterangan;
	/** Penanda aktif. Dihormati {@code TagihanApiGrupUtil}, TIDAK dihormati {@code PembayaranOnline} (lihat Javadoc kelas). */
	private Boolean aktif;
	/** Yayasan pemilik grup; lapis atas cakupan tenant. */
	private Yayasan yayasan;
	/** Sekolah pemilik grup; item hanya boleh memakai grup dari sekolah yang sama. */
	private Sekolah sekolah;
	/** Sisi terbalik relasi (read-only). Anggota sesungguhnya ditentukan kolom FK pada {@link ItemBiayaSekolah}. */
	private Set<ItemBiayaSekolah> itemBiayaSekolahs = new HashSet<ItemBiayaSekolah>();
	/** Nama pengguna terakhir yang mengubah baris. Diisi {@code AuditTimestampInterceptor}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris. Diisi {@code AuditTimestampInterceptor}. */
	private String olehId;
	/** Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat, lalu diperbarui pada setiap {@code UPDATE}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kunci utama grup.
	 *
	 * <p>Dipakai sebagai penanda batas kelompok di {@code PembayaranOnline} (baris kepala baru
	 * disisipkan hanya ketika id berubah) dan sebagai nilai {@code grup_id}/{@code grup_key} pada
	 * respons API {@code TagihanApiGrupUtil}. Kolom ditandai {@code insertable = false} karena
	 * nilainya dibangkitkan sekuens basis data.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/**
	 * Menetapkan kunci utama. Hanya dipakai Hibernate saat memuat/menyimpan baris.
	 *
	 * @param id kunci utama dari basis data
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Kode singkat grup, sudah di-{@code trim} dan tidak pernah {@code null}.
	 *
	 * <p>Karena Hibernate memakai <i>property access</i> pada entity ini (anotasi {@code @Id}
	 * dipasang di getter), nilai yang dikembalikan getter inilah yang benar-benar ditulis ke kolom —
	 * artinya kode selalu tersimpan dalam bentuk sudah terpangkas, dan kode kosong tersimpan sebagai
	 * string kosong, bukan {@code NULL}. Wajib diisi ({@code nullable = false}); {@code onSave()}
	 * pada Action menolak kode kosong dan kode kembar pada sekolah yang sama.</p>
	 *
	 * @return kode grup terpangkas, atau string kosong bila belum diisi
	 */
	@Column(name = "kode", nullable = false)
	public String getKode() { return kode == null ? "" : kode.trim(); }
	/**
	 * Menetapkan kode grup. Nilai disimpan apa adanya; pemangkasan dilakukan di getter.
	 *
	 * @param kode kode grup dari form (boleh {@code null})
	 */
	public void setKode(String kode) { this.kode = kode; }

	/**
	 * Nama grup, sudah di-{@code trim} dan tidak pernah {@code null}.
	 *
	 * <p>Meng-override {@link GeneralValueObject#getNama()}, sehingga ikut menjadi kunci urut pada
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}. Karena getter ini tidak pernah
	 * mengembalikan {@code null}, cabang {@code getKeterangan()} pada {@code compareTo} induk tidak
	 * akan pernah tercapai untuk kelas ini.</p>
	 *
	 * @return nama grup terpangkas, atau string kosong bila belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() { return nama == null ? "" : nama.trim(); }
	/**
	 * Menetapkan nama grup. Nilai disimpan apa adanya; pemangkasan dilakukan di getter.
	 *
	 * @param nama nama grup dari form (boleh {@code null})
	 */
	public void setNama(String nama) { this.nama = nama; }

	/**
	 * Keterangan bebas, sudah di-{@code trim} dan tidak pernah {@code null}.
	 *
	 * <p>Ditampilkan sebagai baris deskripsi pada combo "Grup Item Biaya" di form Item Biaya
	 * ({@code Common.insertComboDanSemua(..., "keterangan", ...)}). Berbeda dengan beberapa entity
	 * lain di paket ini yang mewarisi {@code getKeterangan()} dari {@link GeneralValueObject} tanpa
	 * pemetaan — dan karenanya kehilangan isian keterangan tiap request — properti ini dideklarasikan
	 * ulang lengkap dengan {@code @Column} sehingga tersimpan dengan benar.</p>
	 *
	 * @return keterangan terpangkas, atau string kosong bila belum diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() { return keterangan == null ? "" : keterangan.trim(); }
	/**
	 * Menetapkan keterangan grup. Nilai disimpan apa adanya; pemangkasan dilakukan di getter.
	 *
	 * @param keterangan catatan bebas dari form (boleh {@code null})
	 */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Penanda grup aktif; {@code null} diperlakukan sebagai {@code TRUE}.
	 *
	 * <p>Karena Hibernate memakai <i>property access</i>, nilai hasil pemulihan {@code null → TRUE}
	 * inilah yang ditulis ke kolom saat {@code INSERT} — baris baru yang dibuat lewat layar CRUD
	 * (yang memang tidak pernah memanggil {@link #setAktif(Boolean)} saat menyimpan) tetap tersimpan
	 * sebagai {@code true}. Baris yang masuk lewat SQL mentah/migrasi bisa saja tetap {@code NULL} di
	 * basis data; penyaring pencarian pada Action sudah mengantisipasinya dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}.</p>
	 *
	 * <p><b>Penting:</b> nilai ini dihormati {@code TagihanApiGrupUtil} (grup nonaktif membuat API
	 * mundur ke pengelompokan {@link PengaturanBiaya}) tetapi TIDAK dihormati {@code PembayaranOnline},
	 * sehingga grup nonaktif masih tampil sebagai judul kelompok di layar pembayaran online.</p>
	 *
	 * @return {@code TRUE} bila aktif atau belum pernah diisi, {@code FALSE} bila sengaja dinonaktifkan
	 */
	@Column(name = "aktif")
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	/**
	 * Menetapkan status aktif grup.
	 *
	 * <p>Satu-satunya penulis di layar CRUD adalah checkbox "Aktif" pada baris daftar
	 * ({@code GrupItemBiayaSekolahAction.GrupRenderer}), yang langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(grup)} — perubahan tersimpan seketika tanpa tombol Simpan.
	 * Checkbox tersebut dinonaktifkan bila pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param aktif status baru; {@code null} akan dibaca kembali sebagai {@code TRUE}
	 */
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	/**
	 * Yayasan pemilik grup — lapis atas cakupan tenant, wajib diisi di form.
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dahulu untuk meresolusi proxy lazy
	 * (relasi ini {@code FetchType.LAZY}), sehingga getter tetap aman dipakai setelah session
	 * Hibernate ditutup. Hasil resolusi disimpan kembali ke field agar panggilan berikutnya murah.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() { yayasan = check(yayasan); return yayasan; }
	/**
	 * Menetapkan yayasan pemilik grup.
	 *
	 * <p>Objek tanpa id (hasil pilihan combo "Semua"/placeholder) dinormalkan menjadi {@code null}
	 * agar tidak memicu {@code TransientObjectException} melalui {@code CascadeType.PERSIST}.</p>
	 *
	 * @param yayasan yayasan terpilih; {@code null} atau objek ber-id {@code null} disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) { this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan; }

	/**
	 * Sekolah pemilik grup — lapis bawah cakupan tenant, wajib diisi di form.
	 *
	 * <p>Menjadi acuan dua aturan penting: (1) combo "Grup Item Biaya" pada form Item Biaya hanya
	 * menampilkan grup dengan sekolah yang sama dengan item, dan (2) pemeriksaan kode kembar di
	 * {@code onSave()} dilakukan per sekolah. Seperti {@link #getYayasan()}, memanggil
	 * {@link GeneralValueObject#check(Object)} untuk meresolusi proxy lazy.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() { sekolah = check(sekolah); return sekolah; }
	/**
	 * Menetapkan sekolah pemilik grup.
	 *
	 * <p>Objek tanpa id dinormalkan menjadi {@code null}, dengan alasan yang sama seperti
	 * {@link #setYayasan(Yayasan)}. Perlu diingat bahwa mengubah sekolah pada grup yang sudah dipakai
	 * TIDAK memutus keanggotaan item-item lamanya — kolom FK ada di sisi item dan tidak ikut
	 * disesuaikan, sehingga item dapat berakhir menunjuk grup milik sekolah lain.</p>
	 *
	 * @param sekolah sekolah terpilih; {@code null} atau objek ber-id {@code null} disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) { this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah; }

	/**
	 * Daftar item biaya yang tergabung dalam grup ini — <b>sisi terbalik relasi, read-only</b>.
	 *
	 * <p>Dipetakan {@code mappedBy = "grupItemBiayaSekolah"}, artinya pemilik relasi adalah
	 * {@link ItemBiayaSekolah#getGrupItemBiayaSekolah()} dan kolom kunci asingnya
	 * ({@code grup_item_biaya_sekolah_id}) berada di tabel item. Menambah/menghapus elemen pada
	 * {@link Set} yang dikembalikan <b>tidak</b> akan mengubah basis data; keanggotaan hanya berubah
	 * bila {@code ItemBiayaSekolah.setGrupItemBiayaSekolah(...)} dipanggil (yaitu dari form Item
	 * Biaya).</p>
	 *
	 * <p>Bersifat {@code LAZY} dan tanpa {@code cascade}: menghapus grup tidak menghapus item, dan
	 * karena kunci asing tetap menunjuk baris yang dihapus, penghapusan grup yang masih dipakai akan
	 * ditolak basis data. Perlu dicatat bahwa layar daftar TIDAK memakai koleksi ini untuk menampilkan
	 * kolom "Jumlah Item" — ia menjalankan {@code Projections.rowCount()} tersendiri per baris (satu
	 * query tambahan per baris; pola N+1 yang disengaja demi menghindari pemuatan koleksi penuh).</p>
	 *
	 * @return himpunan item anggota; tidak pernah {@code null}, tetapi bisa kosong
	 */
	@OneToMany(mappedBy = "grupItemBiayaSekolah", fetch = FetchType.LAZY)
	public Set<ItemBiayaSekolah> getItemBiayaSekolahs() { return itemBiayaSekolahs; }
	/**
	 * Menetapkan koleksi anggota. Umumnya hanya dipanggil Hibernate saat memuat entity.
	 *
	 * <p>Argumen {@code null} diganti {@link HashSet} kosong sehingga {@link #getItemBiayaSekolahs()}
	 * tidak pernah mengembalikan {@code null} dan pemanggil bebas melakukan iterasi tanpa penjagaan.
	 * Karena relasi ini tidak dimiliki di sisi ini, memanggil setter ini tidak menulis apa pun ke
	 * basis data.</p>
	 *
	 * @param itemBiayaSekolahs himpunan anggota baru; {@code null} diperlakukan sebagai himpunan kosong
	 */
	public void setItemBiayaSekolahs(Set<ItemBiayaSekolah> itemBiayaSekolahs) {
		this.itemBiayaSekolahs = itemBiayaSekolahs == null
				? new HashSet<ItemBiayaSekolah>() : itemBiayaSekolahs;
	}

	/**
	 * Label siap tampil berbentuk <code>"KODE - Nama"</code>, atau hanya <code>"Nama"</code> bila kode kosong.
	 *
	 * <p>Inilah teks yang dilihat siswa/orang tua sebagai judul kelompok tagihan. Dipakai di tiga
	 * tempat: baris kepala {@code MyGroupConfig} pada {@code PembayaranOnline}, kunci pengurutan
	 * daftar tagihan di layar yang sama (dibandingkan dalam huruf kecil), dan field {@code grup_ta}
	 * pada respons {@code TagihanApiGrupUtil}. Juga menjadi isi {@link #toString()}, sehingga label
	 * yang sama muncul pada combo pemilih grup dan pada label "Grup: …" di daftar item biaya.</p>
	 *
	 * <p>Anotasi {@code @Transient} <b>wajib</b> dan sengaja ditambahkan menyusul (r83444): tanpa itu
	 * Hibernate — yang memakai <i>property access</i> pada entity ini — akan menganggap
	 * {@code labelTampilan} sebagai properti persisten dan mencoba membuat kolomnya lewat
	 * {@code hbm2ddl.auto=update}. Nilai selalu dihitung ulang, tidak pernah disimpan.</p>
	 *
	 * @return label gabungan kode dan nama; tidak pernah {@code null}
	 */
	@javax.persistence.Transient
	public String getLabelTampilan() {
		return (getKode().isEmpty() ? "" : getKode() + " - ") + getNama();
	}

	/**
	 * Representasi teks entity, sama persis dengan {@link #getLabelTampilan()}.
	 *
	 * <p>Meng-override {@link GeneralValueObject#toString()} (yang mengembalikan {@code getNama()}
	 * saja) supaya kode ikut tampil. Dipakai oleh komponen ZK yang merender objek entity secara
	 * langsung, mis. combo pemilih grup pada form Item Biaya.</p>
	 *
	 * @return label gabungan kode dan nama; tidak pernah {@code null}
	 */
	public String toString() { return getLabelTampilan(); }

	/**
	 * Nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan {@code @MappedSuperclass}
	 * sehingga propertinya tidak dipetakan Hibernate. Tanpa kolom {@code Column} eksplisit, Hibernate
	 * memakai nama properti sebagai nama kolom ({@code oleh}).</p>
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat interceptor
	 */
	public String getOleh() { return oleh; }
	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Nilai {@code null} atau string kosong <b>diabaikan</b> (jejak lama dipertahankan) — perilaku
	 * yang sengaja dipakai seluruh entity AIS agar jejak audit tidak terhapus oleh proses latar
	 * belakang yang tidak punya konteks pengguna. Pemanggil normalnya adalah
	 * {@code AuditTimestampInterceptor.ubah(...)} dari kait {@code onUpdate()}.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) { if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh; }
	/**
	 * Id pengguna yang terakhir mengubah baris ini. Pasangan numerik/identitas dari {@link #getOleh()}.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menetapkan id pengguna pengubah terakhir; mengabaikan nilai kosong dengan alasan yang sama
	 * seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) { if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId; }

	/**
	 * Cap waktu perubahan terakhir baris ini.
	 *
	 * <p>Diinisialisasi ke waktu server ({@code ais.ui.util.WaktuUtil.getDate()}) sejak objek dibuat,
	 * sehingga baris baru pun langsung punya nilai. Nama properti memakai garis bawah mengikuti nama
	 * kolom warisan ({@code tanggal_dirubah}) dan dipertahankan karena {@code AuditTimestampInterceptor}
	 * serta {@link GeneralValueObject} mengaksesnya lewat nama tersebut.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang dibuat lewat konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/**
	 * Menetapkan cap waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}, nilai {@code null} DITERIMA apa adanya di sini.
	 * Pemanggil normalnya {@code AuditTimestampInterceptor.ubah(...)}.</p>
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	/**
	 * Kait JPA yang berjalan tepat sebelum setiap {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang memperbarui
	 * {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan {@link #setOlehId(String)}
	 * dengan identitas pengguna yang sedang aktif — kecuali bila {@code AuditTrailHelper} menilai
	 * tidak ada perubahan bisnis nyata, sehingga cap waktu tidak bergerak tanpa alasan.</p>
	 *
	 * <p><b>Catatan:</b> hanya ada kait {@code @PreUpdate}, tidak ada {@code @PrePersist} — jejak
	 * {@code oleh}/{@code olehId} pada baris yang <i>baru dibuat</i> karena itu tetap {@code null}
	 * sampai baris tersebut diubah untuk pertama kalinya. Riwayat penuh perubahan tetap terekam
	 * Envers lewat {@code @Audited} pada kelas ini.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
