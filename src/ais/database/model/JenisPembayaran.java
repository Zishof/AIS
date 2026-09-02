package ais.database.model;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity <b>MASTER cara/jenis pembayaran</b> (tabel {@code public.jenis_pembayaran}).
 *
 * <p>Satu baris mewakili satu <b>cara uang diterima atau dikeluarkan</b> oleh institusi:
 * "Tunai", "Transfer BNI", "Virtual Account BRI", "Potong Tabungan", "QRIS", dan sejenisnya.
 * Perannya adalah <b>jembatan antara transaksi keuangan operasional dan jurnal akunting</b>:
 * setiap baris menunjuk satu {@link ais.database.model.akunting.Akun Akun} kas/bank
 * ({@link #getAkun()}) yang nanti dipakai mesin posting sebagai lawan jurnal saat penerimaan
 * dibukukan. Karena itulah menu aplikasinya tidak berlabel "Jenis Pembayaran" melainkan
 * <b>"Akun Pembayaran"</b> ({@code MenuSnapshotData} id menu 917 &rarr;
 * {@code /pages/master/jenis_pembayaran.zul}), meskipun judul jendela tambah/ubah dan judul
 * halaman UI baru tetap "Tambah/Ubah Jenis Pembayaran" ({@code JenisPembayaranAction:342},
 * {@code WEB-INF/new/root/uiux/jenis_pembayaran.jsp}).</p>
 *
 * <h2>Jangan tertukar dengan kelas bernama mirip</h2>
 * <ul>
 *   <li>{@code ais.database.model.sirs.JenisPembayaranMedis} &mdash; master terpisah untuk modul
 *       rumah sakit (SIRS), dikelola {@code ais.action.master.sirs.JenisPembayaranAction}. Kelas
 *       <b>ini</b> tidak dipakai modul SIRS sama sekali.</li>
 *   <li>{@code ais.database.model.asset.JenisPembayaranBarang} &mdash; master cara bayar pengadaan
 *       barang/asset (DP, termin, lunas), tabel dan layar berbeda.</li>
 *   <li>{@code ais.database.model.akunting.CaraPembayaranTransfer} &mdash; salinan struktur yang
 *       hampir identik (termasuk nama konstanta {@code DEFAULT_JENIS_PEMBAYARAN} dan seed "Tunai"),
 *       tetapi tabel dan pemakainya berbeda.</li>
 *   <li>{@code ais.database.model.JenisTabungan} &mdash; master jenis tabungan; di sini ia hanya
 *       muncul sebagai <i>relasi opsional</i> ({@link #getJenisTabungan()}), bukan sinonim.</li>
 * </ul>
 *
 * <h2>Pemakai utama</h2>
 * <p>Kelas ini disebut di sekitar <b>141 berkas Java</b>. Entity yang menyimpannya sebagai kolom:</p>
 * <ul>
 *   <li>{@link ais.database.model.CicilanPembayaran} &mdash; <b>pemakai terpenting</b>: setiap
 *       penerimaan/cicilan mahasiswa mencatat cara bayarnya. Perlu diperhatikan: di
 *       {@code CicilanPembayaran} ada <b>dua</b> properti bertipe kelas ini
 *       ({@code jenisPembayaran} dan {@code jenisTabungan}) &mdash; keduanya {@code JenisPembayaran},
 *       bukan {@link JenisTabungan}.</li>
 *   <li>{@link ais.database.model.CicilanPembayaranGagal} &mdash; arsip cicilan gagal/batal.</li>
 *   <li>{@link ais.database.model.Deposit} &mdash; setoran/deposit mahasiswa.</li>
 *   <li>{@link ais.database.model.PengeluaranMahasiswa} &mdash; sisi pengeluaran/refund.</li>
 *   <li>{@link ais.database.model.BuktiPembayaran} &mdash; bukti bayar yang diunggah/dicetak.</li>
 *   <li>{@link ais.database.model.BankHost} &mdash; pemetaan kanal host-to-host bank.</li>
 *   <li>{@link ais.database.model.ItemBiaya} &mdash; item biaya/tagihan.</li>
 * </ul>
 * <p>Pemakai non-entity yang berat: seluruh keluarga {@code Posting*Action} keuangan, servlet
 * callback payment gateway ({@code Bniresponse}, {@code Briresponse}, {@code Bsiresponse},
 * {@code FasPayResponse}, {@code JatelindoCallback}), unggahan virtual account,
 * {@code DaftarUlangMahasiswaBaru/LamaAction}, serta dasbor dan laporan keuangan.</p>
 *
 * <h2>Dua "nilai bawaan" yang berbeda &mdash; sumber kebingungan</h2>
 * <p>Ada <b>dua</b> pegangan statis ke baris "Tunai", diisi oleh jalur berbeda dan dengan kriteria
 * berbeda. Keduanya <b>tidak</b> dijamin menunjuk baris yang sama:</p>
 * <table border="1">
 *   <tr><th></th><th>{@link #DEFAULT_JENIS_PEMBAYARAN}</th><th>{@code ConstantValues.TUNAI}</th></tr>
 *   <tr><td>Diisi oleh</td><td>{@link #reloadDefault()} (kelas ini)</td>
 *       <td>{@code InitDataHelper} (sekitar baris 753)</td></tr>
 *   <tr><td>Kriteria cari</td><td>{@code defaultPembayaran = true}</td>
 *       <td>{@code nama = "Tunai"} <b>dan</b> {@code akun = ConstantValues.KAS}</td></tr>
 *   <tr><td>Bila tak ketemu</td><td>membuat baris baru: nama "Tunai", deskripsi "Bayar Tunai",
 *       {@code defaultPembayaran = true}, <b>akun dibiarkan {@code null}</b></td>
 *       <td>membuat baris baru: nama "Tunai", deskripsi "Bayar Tunai", {@code akun = KAS},
 *       <b>{@code defaultPembayaran} dibiarkan {@code null}</b></td></tr>
 * </table>
 * <p><b>Konsekuensi yang perlu diwaspadai pada database baru:</b> karena baris seed
 * {@code InitDataHelper} tidak ber-{@code defaultPembayaran = true}, {@link #reloadDefault()}
 * tidak akan menemukannya dan <b>membuat baris "Tunai" kedua</b> &mdash; kali ini tanpa
 * {@code akun}. Baris tanpa akun itu berbahaya bagi mesin posting, yang mengambil akun lawan dari
 * {@link #getAkun()}. Urutan pemanggilan {@code InitDataHelper} vs
 * {@code InitData.reloadDefaults()} menentukan baris mana yang akhirnya jadi "default" di layar.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit</b> (deklarasi ulang dari induk, lihat catatan di bawah):
 *       {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #getDeskripsi()}, {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Pemetaan akunting</b>: {@link #getAkun()} &mdash; akun kas/bank lawan jurnal.</li>
 *   <li><b>Relasi opsional pelengkap</b>: {@link #getBank()} (bank penampung),
 *       {@link #getJenisTabungan()} (bila pembayaran memotong tabungan),
 *       {@link #getSatuanKerja()} (kepemilikan unit / filter multi-unit).</li>
 *   <li><b>Flag</b>: {@link #getAktif()}, {@link #getDefaultPembayaran()}.</li>
 *   <li><b>Utilitas statis</b>: {@link #reloadDefault()},
 *       {@link #ambilJenisPembayaranBerdasarkanKodeAkun(Session, String)} &mdash; keduanya
 *       <b>menulis ke database</b> bila data belum ada.</li>
 * </ul>
 *
 * <h2>Hal non-obvious</h2>
 * <ul>
 *   <li><b>Getter menulis balik ke field.</b> Keempat getter relasi ({@link #getAkun()},
 *       {@link #getBank()}, {@link #getSatuanKerja()}, {@link #getJenisTabungan()}) memakai pola
 *       {@code x = check(x)} milik {@link GeneralValueObject} &mdash; hasil resolusi proxy lazy
 *       <b>ditugaskan kembali</b> ke field. Jadi memanggil getter mengubah state object (dan bisa
 *       memicu query database), bukan sekadar membaca.</li>
 *   <li><b>Getter mengembalikan nilai substitusi.</b> {@link #getNama()} mengembalikan nilai
 *       ter-{@code trim()}, {@link #getAktif()} mengembalikan {@code true} bila field {@code null},
 *       {@link #getDefaultPembayaran()} mengembalikan {@code false} bila field {@code null}.
 *       Karena kelas ini memakai <b>property access</b> (anotasi ada di getter), nilai substitusi
 *       itulah yang dibaca Hibernate saat dirty-check &mdash; sehingga baris dengan kolom
 *       {@code aktif} NULL dapat ikut ter-{@code UPDATE} menjadi {@code true} pada flush pertama
 *       walau pengguna tidak menyentuh apa pun.</li>
 *   <li><b>Tidak ada getter destruktif</b> (yang mengosongkan koleksi/field setelah dibaca) di
 *       kelas ini &mdash; sudah diverifikasi baris per baris.</li>
 *   <li><b>{@link #reloadDefault()} menutup session Hibernate milik thread pemanggil.</b> Ini satu-
 *       satunya method di kelas ini yang menyentuh siklus hidup session; lihat peringatan rinci di
 *       Javadoc method tersebut.</li>
 *   <li><b>Penamaan kolom.</b> {@code MyNamingStrategy} adalah turunan
 *       {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya, tanpa konversi
 *       camelCase &rarr; snake_case). Properti tanpa {@code @Column} karena itu memetakan kolom
 *       bernama persis seperti propertinya: {@code kode}, {@code deskripsi}, {@code aktif}, dan
 *       &mdash; perhatikan &mdash; {@code defaultPembayaran} (huruf besar di tengah, bukan
 *       {@code default_pembayaran}).</li>
 *   <li><b>{@code keterangan} praktis mati.</b> Kolom dan pasangan getter/setter-nya ada, tetapi
 *       layar {@code JenisPembayaranAction} tidak pernah mengisi maupun menampilkannya (yang
 *       memakai {@code keterangan} adalah kelas SIRS bernama mirip). Deskripsi bebas yang benar-
 *       benar dipakai adalah {@link #getDeskripsi()}.</li>
 * </ul>
 *
 * <h2>Catatan tentang {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti yang dideklarasikan di
 * sana. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 * <b>sengaja dideklarasikan ulang</b> di kelas ini. Itu <b>keharusan teknis, bukan duplikasi yang
 * perlu diperbaiki</b>.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.CicilanPembayaran
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.JenisTabungan
 * @see ais.action.master.JenisPembayaranAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_pembayaran")
public class JenisPembayaran extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan {@code hbm2java} dan <b>tidak boleh diubah</b>:
	 * instance kelas ini ikut tersimpan di HttpSession/desktop ZK, sehingga mengubahnya membuat
	 * session lama gagal dideserialisasi setelah redeploy.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (identity {@code jenis_pembayaran.id}). Deklarasi ulang dari induk; lihat Javadoc kelas. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit). Deklarasi ulang dari induk. */
	private String oleh;
	/** Id/NIK pengguna terakhir yang mengubah baris ini (jejak audit). Deklarasi ulang dari induk. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah, <b>hanya bila nilainya berarti</b>.
	 *
	 * <p>Nilai {@code null} atau string yang seluruhnya spasi <b>diabaikan secara diam-diam</b>
	 * (method langsung {@code return} tanpa menyentuh field). Ini disengaja: jejak audit yang sudah
	 * ada tidak boleh terhapus oleh proses yang kebetulan tidak punya konteks pengguna (job
	 * terjadwal, callback bank, import batch).</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah, <b>hanya bila nilainya berarti</b>.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: {@code null}/kosong diabaikan diam-diam agar
	 * jejak audit lama tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari konteks pengguna aktif dan
	 * memperbarui {@link #setTanggal_dirubah(Date)}. Tidak pernah dipanggil manual dari kode
	 * aplikasi, dan <b>tidak</b> berjalan pada {@code INSERT} (hanya {@code UPDATE}).</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke jam server saat object dibuat
	 * ({@code WaktuUtil.getDate()}), lalu diperbarui kait {@link #onUpdate()} pada setiap
	 * {@code UPDATE}. Deklarasi ulang dari induk; lihat Javadoc kelas.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan
	 * oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * <p>Tanpa {@code @Column}, sehingga jatuh ke penamaan default {@code MyNamingStrategy} &mdash;
	 * kolom {@code tanggal_dirubah} apa adanya.</p>
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null} untuk object baru, karena field
	 *         diinisialisasi saat konstruksi)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: <b>hanya {@link #getNama()}</b>.
	 *
	 * <p>Dipakai luas oleh komponen ZK (isi {@code Combobox}/{@code Listbox} cara bayar) dan oleh
	 * perakitan keterangan jurnal. Membaca field {@code nama} langsung, jadi <b>tidak</b>
	 * ter-{@code trim} seperti {@link #getNama()}.</p>
	 *
	 * <p><b>Hati-hati:</b> mengembalikan {@code null} (bukan string kosong) bila nama belum diisi;
	 * beberapa pemakai merangkainya dengan {@code +} sehingga bisa memunculkan teks "null" di layar
	 * &mdash; pola ini terlihat mis. di {@code DaftarUlangMahasiswaLamaAction} yang merangkai
	 * {@code getJenisTabungan()} langsung ke dalam string.</p>
	 *
	 * @return nama cara pembayaran apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode ringkas cara pembayaran (kolom {@code kode}); dipakai sebagai kunci tampilan dan urutan grid. */
	private String kode;

	/** Nama cara pembayaran, mis. "Tunai", "Transfer BNI" (kolom {@code nama}, wajib). */
	private String nama;
	/** Keterangan bebas (kolom {@code keterangan}). Praktis tidak dipakai layar kelas ini; lihat Javadoc kelas. */
	private String keterangan;
	/** Akun kas/bank lawan jurnal saat penerimaan dibukukan (FK {@code akun}). */
	private Akun akun;
	/** Deskripsi bebas yang ditampilkan di grid dan form (kolom {@code deskripsi}). */
	private String deskripsi;
	/** Penanda "cara bayar bawaan"; sumber pencarian {@link #DEFAULT_JENIS_PEMBAYARAN} (kolom {@code defaultPembayaran}). */
	private Boolean defaultPembayaran;
	/** Penanda aktif; {@code null} diperlakukan sebagai aktif (kolom {@code aktif}). */
	private Boolean aktif;
	/** Unit/satuan kerja pemilik baris; dipakai memfilter daftar per unit (FK {@code satuan_kerja}). */
	private SatuanKerja satuanKerja;
	/** Bila terisi, pembayaran ini memotong saldo tabungan berjenis tersebut (FK {@code jenis_tabungan}). */
	private JenisTabungan jenisTabungan;
	/** Bank penampung untuk cara bayar transfer/VA (FK {@code bank}). */
	private Bank bank;

	/**
	 * Cache statis baris "cara bayar bawaan" ({@code defaultPembayaran = true}), diisi
	 * {@link #reloadDefault()}.
	 *
	 * <p><b>Peringatan:</b> field ini {@code public} dan {@code mutable}, dibagi seluruh aplikasi
	 * (bukan per pengguna/per tenant), dan <b>bisa {@code null}</b> sebelum {@link #reloadDefault()}
	 * pertama berjalan. Pemakai karena itu selalu memeriksa {@code null} lebih dulu, mis.
	 * {@code CicilanPembayaran.getJenisPembayaran()} (sekitar baris 867) dan
	 * {@code Deposit} (sekitar baris 194) yang memakainya sebagai nilai jatuhan.</p>
	 *
	 * <p>Object di dalamnya berasal dari session Hibernate yang <b>sudah ditutup</b> oleh
	 * {@link #reloadDefault()} &mdash; jadi ia <i>detached</i>. Membaca relasi lazy-nya
	 * (mis. {@link #getAkun()}) baru berhasil berkat mekanisme {@code check()} milik
	 * {@link GeneralValueObject}.</p>
	 *
	 * @see #reloadDefault()
	 * @see ais.common.ConstantValues#TUNAI
	 */
	public static JenisPembayaran DEFAULT_JENIS_PEMBAYARAN = null;

	/**
	 * Memuat ulang cache {@link #DEFAULT_JENIS_PEMBAYARAN}, dan <b>membuat baris "Tunai" di database
	 * bila belum ada</b>.
	 *
	 * <p>Alur:</p>
	 * <ol>
	 *   <li>Mengambil session lewat {@code HibernateUtil.currentNativeSession()} (session bersama
	 *       satu thread).</li>
	 *   <li>Mencari satu baris dengan {@code defaultPembayaran = true}.</li>
	 *   <li>Bila tidak ada, <b>membuat dan menyimpan</b> baris baru: nama "Tunai", deskripsi
	 *       "Bayar Tunai", {@code defaultPembayaran = true} &mdash; dalam transaksi tersendiri
	 *       ({@code begin}/{@code commit}). Perhatikan: {@link #setAkun(Akun)} <b>tidak</b>
	 *       dipanggil, sehingga baris hasil auto-seed ini <b>tidak punya akun lawan jurnal</b>.</li>
	 *   <li>Menutup session: {@code disconnect()} + {@code close()} + {@code HibernateUtil.closeSession()}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang harus disadari &mdash; session thread ikut ditutup.</b> Karena
	 * {@code currentNativeSession()} mengembalikan session ThreadLocal yang <b>dibagi</b> sepanjang
	 * thread, langkah penutupan di atas membatalkan session milik request yang sedang berjalan.
	 * Itulah sebabnya semua pemanggil di layar membungkusnya dalam
	 * {@code Common.createDefaultTimer(...)} sehingga eksekusinya tertunda ke event ZK berikutnya,
	 * setelah simpan/hapus selesai &mdash; lihat {@code JenisPembayaranAction} baris 251, 302, dan
	 * 518. Memanggil method ini <b>di tengah</b> unit kerja yang belum di-{@code flush} berisiko
	 * kehilangan perubahan.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> saat boot aplikasi lewat {@code InitData} (blok
	 * {@code reloadDefaults}, baris ~715), setiap kali baris master disimpan/dihapus atau checkbox
	 * "default" diubah di {@code JenisPembayaranAction}, serta secara defensif oleh
	 * {@code DaftarUlangMahasiswaBaruAction}/{@code DaftarUlangMahasiswaLamaAction} bila cache masih
	 * {@code null} saat mengisi combo cara bayar.</p>
	 *
	 * <p><b>Tidak aman dari sisi konkurensi:</b> menulis field statis publik tanpa sinkronisasi;
	 * dua thread yang menjalankannya bersamaan bisa saling menimpa, dan pada database kosong bisa
	 * menciptakan lebih dari satu baris "Tunai".</p>
	 *
	 * @see #DEFAULT_JENIS_PEMBAYARAN
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		DEFAULT_JENIS_PEMBAYARAN = (JenisPembayaran) session.createCriteria(JenisPembayaran.class)
				.add(Restrictions.eq("defaultPembayaran", true)).setMaxResults(1).uniqueResult();

		if (DEFAULT_JENIS_PEMBAYARAN == null) {
			DEFAULT_JENIS_PEMBAYARAN = new JenisPembayaran();
			DEFAULT_JENIS_PEMBAYARAN.setNama("Tunai");
			DEFAULT_JENIS_PEMBAYARAN.setDeskripsi("Bayar Tunai");
			DEFAULT_JENIS_PEMBAYARAN.setDefaultPembayaran(true);
			session.getTransaction().begin();
			session.save(DEFAULT_JENIS_PEMBAYARAN);
			session.getTransaction().commit();
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Semua field dibiarkan {@code null} kecuali {@link #tanggal_dirubah} yang langsung diisi jam
	 * server oleh inisialisasi field.</p>
	 */
	public JenisPembayaran() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * (strategi {@code IDENTITY}). Bernilai {@code null} untuk object yang belum pernah disimpan
	 * &mdash; layar memakai pemeriksaan {@code getId() == null} untuk membedakan "tambah" dari
	 * "ubah".</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas cara pembayaran.
	 *
	 * <p>Tanpa {@code @Column}, jadi memetakan kolom {@code kode} apa adanya. Nilainya dipakai
	 * sebagai kunci tampilan revisi/audit di grid dan sebagai kolom pengurut daftar
	 * ({@code Order.asc("kode")}), serta divalidasi keunikannya oleh {@code checkKode()} di layar
	 * master &mdash; keunikan itu <b>tidak</b> ditegakkan constraint di kelas ini.</p>
	 *
	 * <p>Berbeda dari {@link #getNama()}, nilai di sini dikembalikan apa adanya (tidak
	 * ter-{@code trim}); pemanggil di layar melakukan {@code trim()} sendiri.</p>
	 *
	 * @return kode cara pembayaran, bisa {@code null}
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode ringkas cara pembayaran.
	 *
	 * <p>Selain dari form master, juga diisi otomatis dengan kode akun oleh
	 * {@link #ambilJenisPembayaranBerdasarkanKodeAkun(Session, String)} saat baris dibuat
	 * on-the-fly.</p>
	 *
	 * @param kode kode cara pembayaran
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama cara pembayaran, <b>sudah ter-{@code trim()}</b>.
	 *
	 * <p><b>Non-obvious:</b> karena kelas ini memakai property access, nilai ter-{@code trim} inilah
	 * yang dibaca Hibernate saat dirty-check. Baris lama yang kolom {@code nama}-nya mengandung
	 * spasi di ujung karena itu bisa ikut ter-{@code UPDATE} menjadi versi terpangkas pada flush
	 * berikutnya, tanpa ada yang menyuntingnya. Field-nya sendiri tidak diubah (bukan tulis-balik
	 * ke field), hanya nilai kembaliannya yang berbeda dari isi field.</p>
	 *
	 * @return nama cara pembayaran tanpa spasi tepi, atau {@code null} bila field {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama cara pembayaran. Nilai disimpan apa adanya (tanpa {@code trim}); pemangkasan baru
	 * terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama cara pembayaran
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas.
	 *
	 * <p><b>Praktis tidak dipakai:</b> layar master {@code ais.action.master.JenisPembayaranAction}
	 * tidak pernah mengisi maupun menampilkan properti ini &mdash; yang memakai {@code keterangan}
	 * adalah kelas SIRS bernama mirip ({@code ais.database.model.sirs.JenisPembayaranMedis}).
	 * Deskripsi bebas yang benar-benar dipakai adalah {@link #getDeskripsi()}.</p>
	 *
	 * @return keterangan, hampir selalu {@code null} pada data nyata
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan akun kas/bank lawan jurnal untuk cara pembayaran ini.
	 *
	 * <p>Ini properti <b>terpenting</b> kelas ini: mesin posting keuangan memakainya sebagai akun
	 * debit saat penerimaan dibukukan (mis. {@code PostingCicilanMahasiswaAction},
	 * {@code PostingDepositAction}). Baris tanpa akun akan membuat posting gagal atau ditolak
	 * dengan pesan "akun belum diisi"; layar master karena itu memvalidasi akun wajib diisi
	 * ({@code JenisPembayaranAction:463}).</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(akun)} milik {@link GeneralValueObject} lalu
	 * <b>menugaskan hasilnya kembali ke field</b>. Untuk object <i>detached</i> (mis. isi
	 * {@link #DEFAULT_JENIS_PEMBAYARAN} atau {@code ConstantValues.TUNAI}) ini bisa memicu query
	 * database untuk memulihkan proxy lazy. Jadi getter ini <b>tidak murni</b>.</p>
	 *
	 * @return akun kas/bank, atau {@code null} bila belum dipetakan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Mengisi akun kas/bank lawan jurnal.
	 *
	 * <p>Karena relasi ber-{@code cascade PERSIST/MERGE}, menyimpan {@code JenisPembayaran} dengan
	 * akun baru yang belum tersimpan akan ikut menyimpan akun tersebut.</p>
	 *
	 * @param akun akun kas/bank
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan deskripsi cara pembayaran.
	 *
	 * <p>Inilah teks bebas yang benar-benar dipakai layar: ditampilkan sebagai kolom grid dan
	 * disunting lewat {@code Textbox} lima baris di form tambah/ubah. Auto-seed mengisinya dengan
	 * "Bayar Tunai" ({@link #reloadDefault()}) atau "Pembayaran via &lt;nama akun&gt;"
	 * ({@link #ambilJenisPembayaranBerdasarkanKodeAkun(Session, String)}).</p>
	 *
	 * <p>Tanpa {@code @Column}, jadi memetakan kolom {@code deskripsi} apa adanya.</p>
	 *
	 * @return deskripsi, bisa {@code null}
	 */
	public String getDeskripsi() {
		return deskripsi;
	}

	/**
	 * Mengisi deskripsi cara pembayaran.
	 *
	 * @param deskripsi teks deskripsi
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengembalikan apakah baris ini adalah <b>cara bayar bawaan</b>, dengan {@code null}
	 * diperlakukan sebagai {@code false}.
	 *
	 * <p>Hanya baris ber-{@code true} yang dipungut {@link #reloadDefault()} ke
	 * {@link #DEFAULT_JENIS_PEMBAYARAN}. Query pencariannya memakai {@code Restrictions.eq(...,
	 * true)} pada <b>kolom</b>, sehingga baris berkolom NULL tidak ikut terpilih walau getter ini
	 * menyamakan NULL dengan {@code false} &mdash; kebetulan konsisten di sini.</p>
	 *
	 * <p><b>Non-obvious:</b> nilai substitusi {@code false} inilah yang dibaca Hibernate saat
	 * dirty-check (property access), sehingga baris berkolom {@code defaultPembayaran} NULL dapat
	 * ikut ter-{@code UPDATE} menjadi {@code false} pada flush berikutnya. Tidak ada penegakan
	 * "hanya satu baris boleh default": mencentang baris kedua tidak mematikan centang baris
	 * pertama, dan {@link #reloadDefault()} sekadar mengambil yang pertama ditemukan
	 * ({@code setMaxResults(1)}) tanpa urutan yang ditentukan.</p>
	 *
	 * @return {@code true} bila baris ini cara bayar bawaan; tidak pernah {@code null}
	 */
	public Boolean getDefaultPembayaran() {
		return defaultPembayaran == null ? false : defaultPembayaran;
	}

	/**
	 * Menandai/melepas status "cara bayar bawaan".
	 *
	 * <p>Dipanggil dari checkbox di grid master; pemanggilnya menyimpan baris lalu menjadwalkan
	 * {@link #reloadDefault()} lewat timer agar cache statis ikut segar
	 * ({@code JenisPembayaranAction:243}).</p>
	 *
	 * @param defaultPembayaran {@code true} untuk menjadikan baris ini bawaan
	 */
	public void setDefaultPembayaran(Boolean defaultPembayaran) {
		this.defaultPembayaran = defaultPembayaran;
	}

	/**
	 * Mencari cara pembayaran yang terikat pada suatu <b>kode akun</b>, dan
	 * <b>membuatnya bila belum ada</b>.
	 *
	 * <p>Alur:</p>
	 * <ol>
	 *   <li>Nilai jatuhan diset ke {@code ConstantValues.TUNAI} lebih dulu.</li>
	 *   <li>Bila {@code kodeAkun} kosong (string kosong), langsung mengembalikan jatuhan itu.</li>
	 *   <li>Mencari {@link ais.database.model.akunting.Akun} berkode {@code kodeAkun}. Bila akun
	 *       tidak ditemukan, tetap mengembalikan jatuhan.</li>
	 *   <li>Mencari {@code JenisPembayaran} yang menunjuk akun itu <b>dan</b> masih aktif
	 *       ({@code aktif IS NULL OR aktif = true} &mdash; sengaja menerima NULL agar sejalan dengan
	 *       {@link #getAktif()}).</li>
	 *   <li><b>Bila tidak ada, membuat baris master baru</b> di database dalam transaksi tersendiri:
	 *       akun diset, {@code kode} dan {@code nama} disalin dari akun, deskripsi diisi
	 *       "Pembayaran via &lt;nama akun&gt;". Baris baru ini <b>tidak</b> diberi
	 *       {@code satuanKerja}, {@code bank}, maupun {@code aktif}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping penting:</b> method ini <b>menulis ke database</b> dan meng-{@code commit}
	 * transaksi pada session yang diberikan pemanggil &mdash; artinya perubahan lain yang masih
	 * menggantung di session yang sama ikut ter-{@code commit}. Method <b>tidak</b> menutup session
	 * (berbeda dari {@link #reloadDefault()}); pemanggillah yang bertanggung jawab.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> hampir seluruhnya dari jalur otomatis tanpa layar, yaitu servlet
	 * callback payment gateway ({@code Bniresponse}, {@code Briresponse}, {@code Bsiresponse},
	 * {@code FasPayResponse}, {@code JatelindoCallback}) dan posting biaya administrasi /
	 * biaya payment gateway ({@code PostingBiayaAdministrasiPembayaranMahasiswaAction},
	 * {@code PostingBiayaPaymentGatewayPembayaranMahasiswaAction}). Karena itu data master ini bisa
	 * bertambah sendiri akibat transaksi bank, bukan hanya lewat menu.</p>
	 *
	 * <p><b>Perhatikan:</b> {@code kodeAkun} diperiksa dengan {@code isEmpty()} saja, sehingga
	 * argumen {@code null} melempar {@code NullPointerException}, dan string berisi spasi lolos ke
	 * query. Nilai jatuhan {@code ConstantValues.TUNAI} sendiri bisa {@code null} bila
	 * {@code InitDataHelper} belum sempat mengisinya (mis. akun "KAS DAN SETARA KAS" tidak ada),
	 * sehingga method ini bisa mengembalikan {@code null}.</p>
	 *
	 * @param session session Hibernate aktif milik pemanggil; dipakai untuk query <b>dan</b> untuk
	 *                {@code begin}/{@code save}/{@code commit} bila baris harus dibuat
	 * @param kodeAkun kode akun kas/bank yang dicari; string kosong berarti "pakai tunai"
	 * @return cara pembayaran yang cocok, baris baru yang barusan dibuat, atau
	 *         {@code ConstantValues.TUNAI} sebagai jatuhan (yang sendiri bisa {@code null})
	 */
	public static JenisPembayaran ambilJenisPembayaranBerdasarkanKodeAkun(Session session, String kodeAkun) {
		JenisPembayaran jenisPembayaran = ConstantValues.TUNAI;
		if (!kodeAkun.isEmpty()) {
			Akun akun = (Akun) session.createCriteria(Akun.class).add(Restrictions.eq("kode", kodeAkun))
					.setMaxResults(1).uniqueResult();
			if (akun != null) {
				jenisPembayaran = (JenisPembayaran) session.createCriteria(JenisPembayaran.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("akun", akun)).setMaxResults(1).uniqueResult();
				if (jenisPembayaran == null) {
					jenisPembayaran = new JenisPembayaran();
					jenisPembayaran.setAkun(akun);
					jenisPembayaran.setKode(akun.getKode());
					jenisPembayaran.setNama(akun.getNama());
					jenisPembayaran.setDeskripsi("Pembayaran via " + akun.getNama());
					session.getTransaction().begin();
					session.save(jenisPembayaran);
					session.getTransaction().commit();
				}
			}
		}

		return jenisPembayaran;
	}

	/**
	 * Mengembalikan status aktif, dengan {@code null} diperlakukan sebagai <b>aktif</b>.
	 *
	 * <p>Konvensi "NULL = aktif" ini konsisten dengan filter yang dipakai di seluruh aplikasi:
	 * {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))}
	 * &mdash; lihat {@code JenisPembayaranAction.initCriteria(boolean)} dan
	 * {@link #ambilJenisPembayaranBerdasarkanKodeAkun(Session, String)}. Baris nonaktif tetap ada
	 * di database (tidak dihapus) supaya transaksi lama yang menunjuknya tidak putus.</p>
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #getDefaultPembayaran()}, nilai substitusi
	 * {@code true} inilah yang dibaca Hibernate saat dirty-check (property access), sehingga baris
	 * berkolom {@code aktif} NULL dapat ikut ter-{@code UPDATE} menjadi {@code true} pada flush
	 * berikutnya walau tidak ada yang menyuntingnya.</p>
	 *
	 * @return {@code true} bila baris masih boleh dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" di grid master, yang langsung menyimpan perubahan
	 * ({@code JenisPembayaranAction:230}).</p>
	 *
	 * @param aktif {@code true}/{@code null} berarti aktif, {@code false} berarti nonaktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan bank penampung untuk cara pembayaran ini.
	 *
	 * <p>Relevan untuk cara bayar transfer/virtual account: namanya ditampilkan di kolom grid master
	 * dan dipakai kode kanal bank untuk mencocokkan kiriman host-to-host. Kosong ({@code null})
	 * untuk cara bayar tunai.</p>
	 *
	 * <p><b>Efek samping:</b> {@code check(bank)} dengan tulis-balik ke field &mdash; bisa memicu
	 * query untuk memulihkan proxy lazy pada object detached. Getter ini <b>tidak murni</b>.</p>
	 *
	 * @return bank penampung, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank", nullable = true)
	public Bank getBank() {
		bank = check(bank);
		return bank;
	}

	/**
	 * Mengisi bank penampung.
	 *
	 * @param bank bank penampung, atau {@code null} untuk cara bayar non-bank
	 */
	public void setBank(Bank bank) {
		this.bank = bank;
	}

	/**
	 * Mengembalikan satuan kerja (unit) pemilik baris master ini.
	 *
	 * <p>Dipakai untuk memfilter daftar cara pembayaran per unit pada instalasi multi-unit:
	 * {@code JenisPembayaranAction.initCriteria(boolean)} menerima baris yang
	 * {@code satuanKerja IS NULL} (berlaku untuk semua unit) atau yang satuan kerjanya termasuk
	 * dalam himpunan unit milik pengguna.</p>
	 *
	 * <p><b>Efek samping:</b> {@code check(satuanKerja)} dengan tulis-balik ke field &mdash;
	 * getter ini <b>tidak murni</b>.</p>
	 *
	 * @return satuan kerja pemilik, atau {@code null} bila berlaku untuk semua unit
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja pemilik baris master ini.
	 *
	 * @param satuanKerja satuan kerja pemilik, atau {@code null} agar berlaku untuk semua unit
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan jenis tabungan sumber dana, bila cara pembayaran ini <b>memotong tabungan</b>.
	 *
	 * <p>Label di form master menjelaskan maksudnya secara harfiah: "Jika mengambil dari tabungan,
	 * pilih jenis tabungan", dengan pilihan kosong berbunyi "=Bukan Tabungan=". Jadi
	 * {@code null} = pembayaran biasa (tunai/transfer), terisi = saldo tabungan mahasiswa
	 * didebit. {@code DaftarUlangMahasiswaBaru/LamaAction} memeriksa properti ini untuk mengalihkan
	 * alur pembayaran ke pemotongan tabungan.</p>
	 *
	 * <p><b>Jangan tertukar:</b> {@code CicilanPembayaran} juga punya properti bernama
	 * {@code jenisTabungan}, tetapi bertipe {@code JenisPembayaran} &mdash; bukan
	 * {@link JenisTabungan} seperti di sini.</p>
	 *
	 * <p><b>Efek samping:</b> {@code check(jenisTabungan)} dengan tulis-balik ke field &mdash;
	 * getter ini <b>tidak murni</b>.</p>
	 *
	 * @return jenis tabungan sumber dana, atau {@code null} bila bukan pembayaran dari tabungan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_tabungan", nullable = true)
	public JenisTabungan getJenisTabungan() {
		jenisTabungan = check(jenisTabungan);
		return jenisTabungan;
	}

	/**
	 * Mengisi jenis tabungan sumber dana.
	 *
	 * @param jenisTabungan jenis tabungan yang akan didebit, atau {@code null} bila bukan
	 *                      pembayaran dari tabungan
	 */
	public void setJenisTabungan(JenisTabungan jenisTabungan) {
		this.jenisTabungan = jenisTabungan;
	}
}
