package ais.database.model.kursus;

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
import org.hibernate.envers.Audited;

import ais.action.master.kursus.helper.KursusUtil;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sop.DisposisiSop;

/**
 * Entitas utama <b>data peserta kursus/pelatihan non-formal</b> (mis. peminjam materi, pengambil
 * kuis/tugas, calon penerima sertifikat pada modul kursus/LMS AIS). Satu baris mewakili satu
 * peserta, yang identitasnya bisa berasal dari salah satu entitas civitas yang sudah ada
 * ({@link Mahasiswa}, {@link Siswa}, {@link Dosen}, {@link Guru}, {@link Pegawai}) atau dari akun
 * umum {@link Tbmuser}, ATAU murni data mandiri (nama/kode/alamat yang diketik sendiri tanpa
 * ditautkan ke entitas mana pun) untuk peserta dari luar (masyarakat umum).
 *
 * <p><b>Kenapa {@code extends VOMahasiswa}, bukan {@link ais.database.model.GeneralValueObject}
 * langsung seperti entitas kursus lain?</b> {@link VOMahasiswa} adalah superclass yang juga
 * dipakai {@link Mahasiswa} dan {@link ais.database.model.BiodataCalonMahasiswa}, membawa mesin
 * penagihan/kegiatan mahasiswa yang besar: {@code ambilKegiatans()}, {@code ambilCicilan()},
 * {@code ambilKodeTagihan()}, {@code ambilPengeluaranMahasiswa()}, dan sejenisnya. <b>Penelusuran
 * menyeluruh tidak menemukan satu pun pemanggil</b> yang mengeksekusi method-method itu pada
 * instance {@code PesertaKursus} di seluruh basis kode — hanya {@link Mahasiswa} dan
 * {@link ais.database.model.BiodataCalonMahasiswa} yang benar-benar memakainya. Ini penting
 * karena hampir seluruh method tersebut bercabang dengan pola
 * {@code if (this instanceof Mahasiswa) ... else} (cabang lain diasumsikan {@code BiodataCalonMahasiswa})
 * (lihat mis. {@code VOMahasiswa.reInitKegiatan}, {@code .ambilPengeluaranMahasiswa}) — bila
 * method itu SAMPAI terpanggil pada instance {@code PesertaKursus} (yang bukan {@code Mahasiswa}
 * maupun {@code BiodataCalonMahasiswa}), cabang {@code else} akan mengeksekusi query Hibernate
 * dengan {@code Restrictions.eq("calonMahasiswa", this)} padahal {@code this} bukan
 * {@code BiodataCalonMahasiswa} — berpotensi salah hasil atau exception saat runtime. Selama tidak
 * ada pemanggil yang mengeksekusinya pada {@code PesertaKursus}, ini bukan bug aktif, namun
 * permukaan warisan yang besar dan berisiko ini sepenuhnya tidak terpakai — kemungkinan pilihan
 * {@code extends VOMahasiswa} pada awalnya hanya untuk mewarisi kerangka value-object umum, tanpa
 * bermaksud memakai mesin penagihan mahasiswa.</p>
 *
 * <p><b>Field polimorfik "siapa peserta ini" tidak saling eksklusif secara teknis.</b> Kelas ini
 * punya enam field relasi identitas ({@code mahasiswa}, {@code siswa}, {@code dosen},
 * {@code guru}, {@code pegawai}, {@code tbmuser}) yang secara desain mewakili "peserta ini adalah
 * salah satu dari keenamnya", tercermin di {@link #toString()}, {@link #getKode()}, dan
 * {@link #getNama()} yang memeriksa keenamnya berurutan dan berhenti pada yang pertama tidak
 * {@code null}. Namun tidak ada constraint basis data maupun validasi entity yang mencegah lebih
 * dari satu field terisi sekaligus — bila itu terjadi, hasil {@link #toString()}/{@link #getKode()}/
 * {@link #getNama()} ditentukan oleh urutan pemeriksaan (mahasiswa &gt; siswa &gt; dosen &gt; guru
 * &gt; pegawai &gt; tbmuser), bukan oleh field mana yang "benar". Layar {@code PesertaKursusAction}
 * secara eksplisit menge-null-kan {@code mahasiswa}/{@code dosen}/{@code pegawai} sebelum
 * menyimpan {@code siswa}/{@code guru} dari form, tetapi tidak semua jalur simpan konsisten
 * melakukan ini.</p>
 *
 * <p><b>Dua pasang FK+String legacy yang bisa tidak sinkron.</b> Selain FK
 * {@link #getJenisIdentitasPeserta()}/{@link #getTipePeserta()}, kelas ini juga menyimpan label
 * pilihan sebagai teks bebas di kolom {@code jenisIdentitas}/{@code tipe} (diisi dari label
 * combobox yang sama saat simpan di {@code PesertaKursusAction}) — kolom legacy yang tidak
 * tersinkron otomatis bila baris {@link JenisIdentitasPeserta}/{@link TipePeserta} berubah nama
 * setelahnya. Lihat javadoc {@link JenisPeserta} dan {@link TipePeserta} untuk penjelasan bahwa
 * keduanya adalah <b>dua sumbu independen</b> (status/keteraturan vs kategori identitas), bukan
 * satu hierarki.</p>
 *
 * @see VOMahasiswa
 * @see JenisPeserta
 * @see TipePeserta
 * @see JenisIdentitasPeserta
 * @see ProdukPeserta
 * @see PesertaPunyaProdukKursus
 * @see PesertaInginProdukKursus
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "peserta_kursus")
public class PesertaKursus extends VOMahasiswa {

	/**
	 * Penanda versi serialisasi Java, bernilai sama dengan entitas lain sepaket karena kerangka
	 * kelasnya disalin dari sumber yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama sekuensial dari basis data. Lihat {@link #getId()}. */
	private Long id;
	/**
	 * Blok audit bayangan yang dipadatkan ke satu baris — pola penyisipan otomatis yang dipakai
	 * di seluruh basis kode AIS agar dapat ditempelkan ke entitas lama tanpa mengubah struktur
	 * berkas. Isinya: field {@code oleh} (nama tampil pengubah terakhir, lihat
	 * {@link #getOleh()}), field {@code olehId} beserta getter-nya (identitas pengubah terakhir),
	 * dan setter {@code setOlehId} yang berpenjaga satu arah — argumen {@code null} atau berisi
	 * spasi saja diabaikan sehingga jejak audit yang sudah terisi tidak dapat dikosongkan kembali
	 * lewat setter.
	 *
	 * <p>Pengulangan blok ini di hampir setiap entitas adalah keharusan teknis, bukan cacat:
	 * {@link ais.database.model.GeneralValueObject} merupakan POJO abstrak biasa dan bukan
	 * {@code @MappedSuperclass}, sehingga properti yang dideklarasikan di sana tidak ikut dipetakan
	 * Hibernate ke kolom tabel turunannya. Di kelas ini blok audit ditempelkan lewat rantai
	 * warisan {@code PesertaKursus -> VOMahasiswa -> VoKunci -> ... -> GeneralValueObject}.
	 */
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna pengubah terakhir baris peserta ini.
	 *
	 * @return identitas (id) pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas pengguna pengubah terakhir dengan penjaga satu arah: argumen
	 * {@code null} atau berisi spasi saja diabaikan.
	 *
	 * @param olehId identitas pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama tampil pengguna pengubah terakhir dengan penjaga satu arah yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama tampil pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang menyimpan baris peserta ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA sebelum setiap {@code UPDATE}; mendelegasikan pencatatan stempel waktu
	 * dan identitas pengubah ke {@code AuditTimestampInterceptor.ubah(this)}. Deklarasi field
	 * {@code tanggal_dirubah} sengaja ditempelkan pada baris yang sama, mengikuti pola blok audit
	 * yang sama seperti pada baris {@code oleh}/{@code olehId} di atas.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir; biasanya sudah diurus {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; untuk objek baru berisi waktu objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan representasi teks peserta ini, gabungan {@link #getKode() kode} dan nama
	 * identitas asalnya.
	 *
	 * <p>Method ini <b>mengakses field {@code mahasiswa}/{@code siswa}/{@code dosen}/{@code
	 * guru}/{@code pegawai}/{@code tbmuser} secara langsung</b> (bukan lewat getter-nya masing-
	 * masing yang memanggil {@code check(...)}), sehingga proxy lazy yang belum diinisialisasi
	 * pada objek detached berpotensi meledak {@code LazyInitializationException} di sini walau
	 * getter relasi yang sesuai (mis. {@link #getMahasiswa()}) sendiri aman dipanggil. Urutan
	 * pemeriksaan menentukan hasil bila lebih dari satu field identitas terisi sekaligus (lihat
	 * catatan di javadoc kelas): {@code mahasiswa} diperiksa lebih dulu, lalu {@code siswa},
	 * {@code dosen}, {@code guru}, {@code pegawai}, {@code tbmuser}, dan baru bila keenamnya
	 * {@code null} dipakai field {@code nama} milik peserta itu sendiri (peserta mandiri/umum
	 * tanpa tautan ke entitas civitas mana pun).
	 *
	 * @return string "{@code kode} - {@code nama}" berdasarkan identitas asal peserta yang
	 *         pertama ditemukan
	 */
	public String toString() {
		String nama = "";
		if (mahasiswa != null) {
			nama = mahasiswa.getNim() + " - " + mahasiswa.getNama();
		} else if (siswa != null) {
			nama = siswa.getNim() + " - " + siswa.getNamaSiswa();
		} else if (dosen != null) {
			nama = dosen.getCode() + " - " + dosen.getNama();
		} else if (guru != null) {
			nama = guru.getKode() + " - " + guru.getNama();
		} else if (pegawai != null) {
			nama = pegawai.getCode() + " - " + pegawai.getNama();
		} else if (tbmuser != null) {
			nama = tbmuser.getUserId() + " - " + tbmuser.getUserNama();
		} else {
			nama = this.nama;
		}
		return kode + " - " + nama;
	}

	/** Kode identitas mentah milik peserta (mis. nomor KTP/kartu pelajar); lihat {@link #getKodeIdentitas()}. Berbeda dari {@link #kode}, field ini murni data, tidak dibangkitkan otomatis. */
	private String kodeIdentitas;
	/** Label jenis identitas legacy berbentuk teks bebas, salinan label {@link #jenisIdentitasPeserta} saat simpan; lihat {@link #getJenisIdentitas()} dan catatan redundansi di javadoc kelas. */
	private String jenisIdentitas;
	/** Kode unik peserta; diturunkan otomatis dari entitas identitas asal atau dibangkitkan barcode. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama peserta; dipakai sebagai fallback bila keenam field identitas asal semuanya {@code null}, dan dihitung ulang oleh {@link #getNama()} bila field ini masih kosong. */
	private String nama;
	/** Alamat peserta, teks bebas panjang ({@code columnDefinition = "text"}). Lihat {@link #getAlamat()}. */
	private String alamat;

	/** Identitas peserta bila berasal dari mahasiswa. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Identitas peserta bila berasal dari siswa. Lihat {@link #getSiswa()}. */
	private Siswa siswa;
	/** Identitas peserta bila berasal dari dosen. Lihat {@link #getDosen()}. */
	private Dosen dosen;
	/** Identitas peserta bila berasal dari guru. Lihat {@link #getGuru()}. */
	private Guru guru;
	/** Identitas peserta bila berasal dari pegawai. Lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Identitas peserta bila berasal dari akun umum {@link Tbmuser}; lihat {@link #getTbmuser()} untuk penjaga yang menge-null-kan field ini bila {@code mahasiswa}/{@code siswa} terisi. */
	private Tbmuser tbmuser;
	/** Jenis peserta (sumbu status/keteraturan). Lihat {@link #getJenisPeserta()} dan {@link JenisPeserta}. */
	private JenisPeserta jenisPeserta;
	/** Label tipe peserta legacy berbentuk teks bebas, salinan label {@link #tipePeserta} saat simpan; lihat {@link #getTipe()} dan catatan redundansi di javadoc kelas. */
	private String tipe;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nomor telepon rumah/kantor peserta. Lihat {@link #getTelp()}. */
	private String telp;
	/** Nomor telepon genggam peserta. Lihat {@link #getHp()}. */
	private String hp;
	/** Alamat surel peserta, dipetakan ke kolom {@code email_peserta}. Lihat {@link #getEmail()}. */
	private String email;
	/** Tipe peserta (sumbu kategori identitas). Lihat {@link #getTipePeserta()} dan {@link TipePeserta}. */
	private TipePeserta tipePeserta;
	/** Penanda peserta masih aktif; bawaan {@code true}. Lihat {@link #getAktif()}. */
	private Boolean aktif = true;

	/** Tanggal pendaftaran peserta; diisi awal saat objek dibuat. Lihat {@link #getTanggal()}. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Pengguna yang membuat baris peserta ini. Lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;

	/** Jumlah perpanjangan yang sudah/boleh dilakukan peserta ini. Lihat {@link #getPerpanjang()}. */
	private Integer perpanjang;
	/** Batas maksimal (mis. maksimal perpanjangan/kapasitas) untuk peserta ini. Lihat {@link #getMaksimal()}. */
	private Integer maksimal;
	/** Jenis kode identitas yang dipakai peserta (Email/NIM/NIS/NIDN/NIK). Lihat {@link #getJenisIdentitasPeserta()} dan {@link JenisIdentitasPeserta}. */
	private JenisIdentitasPeserta jenisIdentitasPeserta;
	/** Pengguna yang mengunci baris peserta ini untuk pengeditan. Lihat {@link #getDikunci()}. */
	private Tbmuser dikunci;
	/** Disposisi SOP terkait baris peserta ini, bila proses pendaftaran melalui alur disposisi surat. Lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate. */
	public PesertaKursus() {
	}

	/**
	 * Mengembalikan kunci utama peserta. Kolomnya {@code insertable = false} karena nilainya
	 * dibangkitkan basis data.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama; dipakai Hibernate dan proses impor.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas peserta ini, apa adanya.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas peserta ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan identitas mahasiswa bila peserta ini berasal dari mahasiswa terdaftar,
	 * meresolusi proxy lazy lewat {@link ais.database.model.GeneralValueObject#check(Object)}
	 * bila perlu sebelum dikembalikan.
	 *
	 * @return mahasiswa asal peserta, atau {@code null} bila peserta bukan mahasiswa atau berasal
	 *         dari identitas lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menetapkan identitas mahasiswa asal peserta ini.
	 *
	 * @param mahasiswa mahasiswa asal peserta, atau {@code null} untuk melepas tautan
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan identitas dosen bila peserta ini berasal dari dosen, meresolusi proxy lazy
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan.
	 *
	 * @return dosen asal peserta, atau {@code null} bila peserta bukan dosen atau berasal dari
	 *         identitas lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan identitas dosen asal peserta ini.
	 *
	 * @param dosen dosen asal peserta, atau {@code null} untuk melepas tautan
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan identitas pegawai bila peserta ini berasal dari pegawai, meresolusi proxy
	 * lazy lewat {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan.
	 *
	 * @return pegawai asal peserta, atau {@code null} bila peserta bukan pegawai atau berasal dari
	 *         identitas lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan identitas pegawai asal peserta ini.
	 *
	 * @param pegawai pegawai asal peserta, atau {@code null} untuk melepas tautan
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan akun {@link Tbmuser} umum bila peserta ini bukan berasal dari mahasiswa
	 * maupun siswa terdaftar.
	 *
	 * <p><b>Getter destruktif dengan efek samping menulis field {@code null}.</b> Setelah
	 * meresolusi proxy lazy lewat {@code check(...)}, method ini memeriksa
	 * {@link #getMahasiswa()} dan {@link #getSiswa()} — bila salah satu terisi, field
	 * {@code tbmuser} <b>ditimpa menjadi {@code null}</b> sebelum dikembalikan, walau baris di
	 * basis data masih menyimpan FK {@code tbmuser} yang lama. Karena entitas ini
	 * {@code dynamicUpdate = true}, pemanggilan getter ini pada entity yang sedang di-dirty-check
	 * Hibernate (mis. dalam siklus transaksi yang membaca getter sebelum flush) berpotensi memicu
	 * {@code UPDATE} yang mengosongkan kolom {@code tbmuser} di baris tersimpan — bukan sekadar
	 * mengubah nilai in-memory. Ini konsisten dengan pola "getter destruktif" yang sudah tercatat
	 * berulang di berbagai entity AIS lain: method yang namanya menyiratkan accessor pasif,
	 * padahal punya efek samping tulis-state yang bisa mengubah data tersimpan.</p>
	 *
	 * @return akun {@link Tbmuser} asal peserta, atau {@code null} bila peserta berasal dari
	 *         mahasiswa/siswa (dipaksa {@code null} oleh getter ini) atau memang belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		if (getMahasiswa() != null || getSiswa() != null) {
			tbmuser = null;
		}
		return tbmuser;
	}

	/**
	 * Menetapkan akun {@link Tbmuser} asal peserta ini; nilai yang ditetapkan di sini dapat
	 * ditimpa kembali menjadi {@code null} oleh {@link #getTbmuser()} bila {@code mahasiswa}
	 * atau {@code siswa} terisi — lihat catatan getter destruktif di javadoc method tersebut.
	 *
	 * @param tbmuser akun {@link Tbmuser} asal peserta, atau {@code null} untuk melepas tautan
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan jenis peserta (sumbu status/keteraturan kepesertaan), dengan bawaan
	 * {@code KursusUtil.ANGGOTA_REGULER} ("Peserta Reguler") bila field masih {@code null},
	 * lalu meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} — getter ini punya efek
	 * samping menulis field {@code jenisPeserta} dengan nilai bawaan tersebut (konsisten dengan
	 * {@code dynamicUpdate} entitas ini). Lihat {@link JenisPeserta} untuk penjelasan sumbu ini
	 * dan perbedaannya dari {@link #getTipePeserta()}.
	 *
	 * @return jenis peserta, tidak pernah {@code null} setelah dipanggil sekali
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_peserta", nullable = true)
	public JenisPeserta getJenisPeserta() {
		if (jenisPeserta == null) {
			jenisPeserta = KursusUtil.ANGGOTA_REGULER;
		}
		jenisPeserta = check(jenisPeserta);
		return jenisPeserta;
	}

	/**
	 * Menetapkan jenis peserta.
	 *
	 * @param jenisPeserta jenis peserta yang ingin ditetapkan, atau {@code null} untuk memicu
	 *                     bawaan "Peserta Reguler" pada {@link #getJenisPeserta()} berikutnya
	 */
	public void setJenisPeserta(JenisPeserta jenisPeserta) {
		this.jenisPeserta = jenisPeserta;
	}

	@Column(unique = true)
	public String getKode() {
		if (mahasiswa != null) {
			kode = mahasiswa.getNim();
		} else if (siswa != null) {
			kode = siswa.getNomorIndukNasional() == null || siswa.getNomorIndukNasional().trim().isEmpty()
					? siswa.getNomorInduk()
					: siswa.getNomorIndukNasional();
		} else if (dosen != null) {
			kode = dosen.getNidn() == null || dosen.getNidn().trim().isEmpty()
					? (kode == null ? "" : BarcodeCommon.generateCode())
					: dosen.getNidn();
		} else if (guru != null) {
			kode = guru.getNuptk() == null || guru.getNuptk().trim().isEmpty()
					? (kode == null ? "" : BarcodeCommon.generateCode())
					: guru.getNuptk();
		} else if (pegawai != null) {
			kode = pegawai.getCode() == null || pegawai.getCode().trim().isEmpty()
					? (kode == null ? "" : BarcodeCommon.generateCode())
					: pegawai.getCode();
		} else if (tbmuser != null) {
			kode = tbmuser.getUserId();
		} else if (kode == null || kode.isEmpty()) {
			kode = BarcodeCommon.generateCode();
		}

		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public String getNama() {
		if (nama == null && id != null) {
			String nama;
			if (mahasiswa != null) {
				nama = mahasiswa.getNama();
			} else if (siswa != null) {
				nama = siswa.getNama();
			} else if (dosen != null) {
				nama = dosen.getNama();
			} else if (guru != null) {
				nama = guru.getNama();
			} else if (pegawai != null) {
				nama = pegawai.getNama();
			} else if (tbmuser != null) {
				nama = tbmuser.getUserId();
			} else {
				nama = "";
			}
			setNama(nama);
			Session session = HibernateUtil.currentSession();
			Common.refreshUpdate(session, this);
		}
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "alamat", nullable = true, columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	@Column(name = "kode_identitas", nullable = true)
	public String getKodeIdentitas() {
		return kodeIdentitas;
	}

	public void setKodeIdentitas(String kodeIdentitas) {
		this.kodeIdentitas = kodeIdentitas;
	}

	public String getTipe() {
		return tipe;
	}

	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	public String getJenisIdentitas() {
		return jenisIdentitas;
	}

	public void setJenisIdentitas(String jenisIdentitas) {
		this.jenisIdentitas = jenisIdentitas;
	}

	public String getTelp() {
		return telp;
	}

	public void setTelp(String telp) {
		this.telp = telp;
	}

	public String getHp() {
		return hp;
	}

	public void setHp(String hp) {
		this.hp = hp;
	}

	@Column(name = "email_peserta")
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_identitas_peserta", nullable = true)
	public JenisIdentitasPeserta getJenisIdentitasPeserta() {
		jenisIdentitasPeserta = check(jenisIdentitasPeserta);
		return jenisIdentitasPeserta;
	}

	public void setJenisIdentitasPeserta(JenisIdentitasPeserta jenisIdentitasPeserta) {
		this.jenisIdentitasPeserta = jenisIdentitasPeserta;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_peserta", nullable = true)
	public TipePeserta getTipePeserta() {
		if (tipePeserta == null) {
			tipePeserta = KursusUtil.MAHASISWA;
		}
		tipePeserta = check(tipePeserta);
		return tipePeserta;
	}

	public void setTipePeserta(TipePeserta tipePeserta) {
		this.tipePeserta = tipePeserta;
	}

	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	public Integer getPerpanjang() {
		return perpanjang;
	}

	public void setPerpanjang(Integer perpanjang) {
		this.perpanjang = perpanjang;
	}

	public Integer getMaksimal() {
		return maksimal;
	}

	public void setMaksimal(Integer maksimal) {
		this.maksimal = maksimal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	public void setGuru(Guru guru) {
		this.guru = guru;
	}
	
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}
}
