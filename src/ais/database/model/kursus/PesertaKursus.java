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

	/**
	 * Mengembalikan kode unik peserta, diturunkan dari nomor induk entitas identitas asal yang
	 * pertama ditemukan, dengan fallback pembangkitan barcode bila entitas asal tidak punya nomor
	 * induk terisi.
	 *
	 * <p><b>Getter destruktif yang menimpa field {@code kode} tiap dipanggil</b> (kecuali pada
	 * cabang terakhir yang mempertahankan nilai lama bila sudah ada) — konsisten dengan pola
	 * getter destruktif lain di kelas ini ({@link #getTbmuser()}, {@link #getJenisPeserta()}).
	 * Urutan pemeriksaan dan sumber kode per jenis identitas asal:</p>
	 * <ol>
	 * <li>{@code mahasiswa} — kode = NIM ({@link Mahasiswa#getNim()}) apa adanya, boleh
	 * {@code null}/kosong bila NIM belum diisi (tidak ada fallback barcode di cabang ini).</li>
	 * <li>{@code siswa} — kode = nomor induk nasional ({@code NISN}) bila terisi, jika tidak jatuh
	 * ke nomor induk sekolah biasa; juga tanpa fallback barcode.</li>
	 * <li>{@code dosen} — kode = NIDN bila terisi; bila NIDN kosong dan {@code kode} lama belum
	 * pernah ada, kode diisi string kosong (bukan barcode); bila {@code kode} lama sudah ada
	 * (baris tersimpan sebelumnya), barcode baru dibangkitkan alih-alih memakai kode lama —
	 * perilaku yang tampak tidak simetris dengan cabang {@code guru}/{@code pegawai} di bawah
	 * yang logikanya identik namun bisa menghasilkan kode berbeda tiap kali dipanggil pada baris
	 * yang sama bila NIDN tetap kosong.</li>
	 * <li>{@code guru} — kode = NUPTK bila terisi, dengan pola fallback yang sama seperti
	 * {@code dosen} di atas (NUPTK).</li>
	 * <li>{@code pegawai} — kode = {@link Pegawai#getCode()} bila terisi, dengan pola fallback
	 * yang sama.</li>
	 * <li>{@code tbmuser} — kode = {@code userId} akun, apa adanya.</li>
	 * <li>Bila keenam identitas asal {@code null} (peserta mandiri/umum) dan {@code kode} yang
	 * tersimpan masih kosong, kode baru dibangkitkan lewat {@link BarcodeCommon#generateCode()};
	 * bila {@code kode} sudah pernah terisi (mis. diisi manual dari form), nilai lama
	 * dipertahankan tanpa perubahan.</li>
	 * </ol>
	 * <p>Kolom ini {@code unique = true} pada level basis data — pembangkitan barcode berulang
	 * pada cabang {@code dosen}/{@code guru}/{@code pegawai} di atas berisiko menghasilkan nilai
	 * yang, walau acak, bisa berbeda tiap pemanggilan pada baris tersimpan yang sama (bukan
	 * idempoten), sehingga sebaiknya tidak dipanggil berulang pada objek yang sama dalam satu
	 * siklus request tanpa segera menyimpan hasilnya.</p>
	 *
	 * @return kode unik peserta sesuai aturan di atas
	 */
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

	/**
	 * Menetapkan kode unik peserta apa adanya; nilai ini dapat ditimpa kembali oleh
	 * {@link #getKode()} pada pemanggilan berikutnya sesuai aturan penurunan kode dari identitas
	 * asal yang dijelaskan di sana.
	 *
	 * @param kode kode unik yang ingin ditetapkan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama peserta, diturunkan dari entitas identitas asal bila belum pernah
	 * dihitung.
	 *
	 * <p><b>Getter dengan efek samping menulis DAN memicu penyimpanan ke basis data</b> — bukan
	 * sekadar getter destruktif yang menimpa field in-memory seperti {@link #getTbmuser()},
	 * melainkan satu tingkat lebih jauh: bila {@code nama} masih {@code null} DAN objek ini sudah
	 * punya {@code id} (baris tersimpan), method ini menghitung nama dari identitas asal yang
	 * pertama ditemukan (urutan sama seperti {@link #getKode()}: mahasiswa, siswa, dosen, guru,
	 * pegawai, lalu {@code tbmuser.getUserId()} sebagai fallback terakhir sebelum string kosong),
	 * memanggil {@link #setNama(String)}, lalu <b>langsung memanggil
	 * {@code Common.refreshUpdate(session, this)}</b> yang mengeksekusi {@code UPDATE} ke basis
	 * data seketika itu juga — bukan menunggu akhir transaksi/flush Hibernate biasa. Artinya
	 * memanggil getter "polos" ini pada baris lama yang {@code nama}-nya kosong dapat menulis ke
	 * database sebagai efek samping baca, di luar siklus commit/transaksi normal pemanggil.</p>
	 * <p>Variabel lokal {@code nama} di dalam method ini <b>membayangi (shadow)</b> field
	 * {@code nama} milik kelas — pola yang sah secara Java namun mudah membingungkan pembaca:
	 * assignment ke variabel lokal {@code nama} di dalam blok {@code if} tidak langsung mengubah
	 * field, field baru berubah lewat pemanggilan eksplisit {@link #setNama(String)}.</p>
	 *
	 * @return nama peserta; hasil hitungan dari identitas asal (dan tersimpan permanen via
	 *         {@code refreshUpdate}) bila sebelumnya kosong dan objek sudah punya id, atau nilai
	 *         field apa adanya (termasuk {@code null}) untuk kasus lain
	 */
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

	/**
	 * Menetapkan nama peserta apa adanya; nilai ini dapat dihitung ulang dan ditimpa oleh
	 * {@link #getNama()} pada pemanggilan berikutnya bila kosong dan objek sudah punya id — lihat
	 * catatan efek samping penyimpanan basis data di javadoc method tersebut.
	 *
	 * @param nama nama peserta yang ingin ditetapkan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan alamat peserta apa adanya, tanpa perhitungan ulang.
	 *
	 * @return alamat peserta, boleh {@code null}
	 */
	@Column(name = "alamat", nullable = true, columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menetapkan alamat peserta.
	 *
	 * @param alamat alamat peserta; boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan kode identitas mentah milik peserta (mis. nomor KTP/kartu pelajar sesuai
	 * {@link #getJenisIdentitasPeserta()} yang dipilih), apa adanya.
	 *
	 * @return kode identitas, boleh {@code null}
	 */
	@Column(name = "kode_identitas", nullable = true)
	public String getKodeIdentitas() {
		return kodeIdentitas;
	}

	/**
	 * Menetapkan kode identitas mentah milik peserta.
	 *
	 * @param kodeIdentitas kode identitas; boleh {@code null}
	 */
	public void setKodeIdentitas(String kodeIdentitas) {
		this.kodeIdentitas = kodeIdentitas;
	}

	/**
	 * Mengembalikan label tipe peserta legacy berbentuk teks bebas, apa adanya — salinan label
	 * {@link #getTipePeserta()} yang dituliskan {@code PesertaKursusAction} pada saat simpan,
	 * TIDAK tersinkron otomatis dengan FK {@code tipePeserta} setelahnya (lihat catatan
	 * redundansi FK+String di javadoc kelas).
	 *
	 * @return label tipe peserta, boleh {@code null}/tidak sinkron dengan {@link #getTipePeserta()}
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Menetapkan label tipe peserta legacy secara manual.
	 *
	 * @param tipe label tipe peserta; boleh {@code null}
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengembalikan label jenis identitas legacy berbentuk teks bebas, apa adanya — salinan label
	 * {@link #getJenisIdentitasPeserta()} yang dituliskan {@code PesertaKursusAction} pada saat
	 * simpan, TIDAK tersinkron otomatis dengan FK {@code jenisIdentitasPeserta} setelahnya (lihat
	 * catatan redundansi FK+String di javadoc kelas).
	 *
	 * @return label jenis identitas, boleh {@code null}/tidak sinkron dengan
	 *         {@link #getJenisIdentitasPeserta()}
	 */
	public String getJenisIdentitas() {
		return jenisIdentitas;
	}

	/**
	 * Menetapkan label jenis identitas legacy secara manual.
	 *
	 * @param jenisIdentitas label jenis identitas; boleh {@code null}
	 */
	public void setJenisIdentitas(String jenisIdentitas) {
		this.jenisIdentitas = jenisIdentitas;
	}

	/**
	 * Mengembalikan nomor telepon rumah/kantor peserta, apa adanya.
	 *
	 * @return nomor telepon, boleh {@code null}
	 */
	public String getTelp() {
		return telp;
	}

	/**
	 * Menetapkan nomor telepon rumah/kantor peserta.
	 *
	 * @param telp nomor telepon; boleh {@code null}
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan nomor telepon genggam peserta, apa adanya.
	 *
	 * @return nomor telepon genggam, boleh {@code null}
	 */
	public String getHp() {
		return hp;
	}

	/**
	 * Menetapkan nomor telepon genggam peserta.
	 *
	 * @param hp nomor telepon genggam; boleh {@code null}
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Mengembalikan alamat surel peserta, apa adanya. Dipetakan ke kolom {@code email_peserta}
	 * (bukan {@code email}) kemungkinan untuk menghindari tabrakan nama dengan kolom lain pada
	 * skema yang sama.
	 *
	 * @return alamat surel, boleh {@code null}
	 */
	@Column(name = "email_peserta")
	public String getEmail() {
		return email;
	}

	/**
	 * Menetapkan alamat surel peserta.
	 *
	 * @param email alamat surel; boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengembalikan jenis kode identitas yang dipakai peserta (Email/NIM/NIS/NIDN/NIK), meresolusi
	 * proxy lazy lewat {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu.
	 * Berbeda dari {@link #getJenisPeserta()}/{@link #getTipePeserta()}, getter ini tidak punya
	 * nilai bawaan bila field masih {@code null}. Lihat {@link JenisIdentitasPeserta} untuk
	 * penjelasan bahwa ini adalah jenis KODE pengenal, bukan jenis dokumen fisik.
	 *
	 * @return jenis kode identitas peserta, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_identitas_peserta", nullable = true)
	public JenisIdentitasPeserta getJenisIdentitasPeserta() {
		jenisIdentitasPeserta = check(jenisIdentitasPeserta);
		return jenisIdentitasPeserta;
	}

	/**
	 * Menetapkan jenis kode identitas yang dipakai peserta.
	 *
	 * @param jenisIdentitasPeserta jenis kode identitas; boleh {@code null}
	 */
	public void setJenisIdentitasPeserta(JenisIdentitasPeserta jenisIdentitasPeserta) {
		this.jenisIdentitasPeserta = jenisIdentitasPeserta;
	}

	/**
	 * Mengembalikan tipe peserta (sumbu kategori identitas), dengan bawaan
	 * {@code KursusUtil.MAHASISWA} bila field masih {@code null}, lalu meresolusi proxy lazy —
	 * getter ini punya efek samping menulis field {@code tipePeserta} dengan nilai bawaan
	 * tersebut, sama seperti {@link #getJenisPeserta()}. <b>Perlu diperhatikan:</b> bawaan
	 * "Mahasiswa" ini berlaku untuk SEMUA peserta yang field {@code tipePeserta}-nya belum diisi,
	 * termasuk peserta yang identitas asalnya bukan mahasiswa (mis. {@code siswa}/{@code
	 * pegawai}/{@code tbmuser}) — bawaan ini murni nilai default kolom, tidak disimpulkan dari
	 * field identitas asal mana yang terisi. Lihat {@link TipePeserta} untuk penjelasan sumbu ini
	 * dan perbedaannya dari {@link #getJenisPeserta()}.
	 *
	 * @return tipe peserta, tidak pernah {@code null} setelah dipanggil sekali
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_peserta", nullable = true)
	public TipePeserta getTipePeserta() {
		if (tipePeserta == null) {
			tipePeserta = KursusUtil.MAHASISWA;
		}
		tipePeserta = check(tipePeserta);
		return tipePeserta;
	}

	/**
	 * Menetapkan tipe peserta.
	 *
	 * @param tipePeserta tipe peserta yang ingin ditetapkan, atau {@code null} untuk memicu
	 *                    bawaan "Mahasiswa" pada {@link #getTipePeserta()} berikutnya
	 */
	public void setTipePeserta(TipePeserta tipePeserta) {
		this.tipePeserta = tipePeserta;
	}

	/**
	 * Mengembalikan penanda apakah peserta masih aktif, dengan bawaan {@code true} bila kolom
	 * masih {@code null}; nilai bawaan itu ditulis balik ke field sehingga pembacaan biasa dapat
	 * memunculkan {@code UPDATE} pada entitas {@code dynamicUpdate} ini (pola sama seperti master
	 * lain di modul ini, mis. {@code JenisJabatanPenelitianDanPengabdian.getAktif()}).
	 *
	 * @return {@code true} bila peserta dianggap aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan penanda aktif peserta.
	 *
	 * @param aktif {@code true} bila peserta masih aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan tanggal pendaftaran peserta, dengan bawaan tanggal saat ini bila field masih
	 * {@code null} — nilai bawaan ini ditulis balik ke field (berbeda dari
	 * {@link PesertaPunyaProdukKursus#getWaktuBeli()}/{@link PesertaInginProdukKursus#getWaktuIngin()}
	 * di entitas sekursus yang TIDAK menulis balik nilai bawaannya).
	 *
	 * @return tanggal pendaftaran, tidak pernah {@code null} setelah dipanggil sekali
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/**
	 * Menetapkan tanggal pendaftaran peserta.
	 *
	 * @param tanggal tanggal pendaftaran yang ingin ditetapkan
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan pengguna yang membuat baris peserta ini, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu.
	 *
	 * @return pengguna pembuat baris, atau {@code null} bila belum tercatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/**
	 * Menetapkan pengguna yang membuat baris peserta ini.
	 *
	 * @param dibuatOleh pengguna pembuat baris
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan jumlah perpanjangan yang sudah/boleh dilakukan peserta ini, apa adanya.
	 *
	 * @return jumlah perpanjangan, boleh {@code null}
	 */
	public Integer getPerpanjang() {
		return perpanjang;
	}

	/**
	 * Menetapkan jumlah perpanjangan peserta ini.
	 *
	 * @param perpanjang jumlah perpanjangan; boleh {@code null}
	 */
	public void setPerpanjang(Integer perpanjang) {
		this.perpanjang = perpanjang;
	}

	/**
	 * Mengembalikan batas maksimal (mis. maksimal perpanjangan/kapasitas) untuk peserta ini, apa
	 * adanya.
	 *
	 * @return batas maksimal, boleh {@code null}
	 */
	public Integer getMaksimal() {
		return maksimal;
	}

	/**
	 * Menetapkan batas maksimal untuk peserta ini.
	 *
	 * @param maksimal batas maksimal; boleh {@code null}
	 */
	public void setMaksimal(Integer maksimal) {
		this.maksimal = maksimal;
	}

	/**
	 * Mengembalikan identitas siswa bila peserta ini berasal dari siswa, meresolusi proxy lazy
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan. Nilai bukan {@code null} di sini juga memengaruhi {@link #getTbmuser()}
	 * (dipaksa {@code null} bila siswa terisi).
	 *
	 * @return siswa asal peserta, atau {@code null} bila peserta bukan siswa atau berasal dari
	 *         identitas lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan identitas siswa asal peserta ini.
	 *
	 * @param siswa siswa asal peserta, atau {@code null} untuk melepas tautan
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan identitas guru bila peserta ini berasal dari guru, meresolusi proxy lazy
	 * lewat {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan. Perlu diketahui: berbeda dari {@code mahasiswa}/{@code siswa}/{@code
	 * dosen}/{@code pegawai}/{@code tbmuser}, tidak ada konstanta {@link TipePeserta} khusus
	 * "Guru" yang disemai {@code KursusUtil} — peserta yang berasal dari guru tetap harus dipilih
	 * salah satu {@link TipePeserta} yang ada secara manual di form.
	 *
	 * @return guru asal peserta, atau {@code null} bila peserta bukan guru atau berasal dari
	 *         identitas lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan identitas guru asal peserta ini.
	 *
	 * @param guru guru asal peserta, atau {@code null} untuk melepas tautan
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan pengguna yang mengunci baris peserta ini untuk pengeditan, meresolusi proxy
	 * lazy lewat {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu. Berbeda
	 * dari kolom lain di kelas ini, kolom join untuk relasi ini tidak beranotasi eksplisit
	 * {@code nullable} (memakai bawaan {@code nullable = true} milik {@code @JoinColumn}).
	 *
	 * @return pengguna yang sedang mengunci baris ini, atau {@code null} bila tidak sedang dikunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan pengguna yang mengunci baris peserta ini.
	 *
	 * @param dikunci pengguna pengunci, atau {@code null} untuk melepas kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan disposisi SOP terkait baris peserta ini (bila pendaftaran peserta berasal dari
	 * alur disposisi surat), meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu.
	 *
	 * @return disposisi SOP terkait, atau {@code null} bila tidak berasal dari alur disposisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan disposisi SOP terkait baris peserta ini dengan penjaga satu arah: argumen
	 * {@code null} atau yang belum tersimpan ({@code getId() == null}) diabaikan, sehingga
	 * tautan disposisi yang sudah ada tidak dapat dilepas kembali lewat setter ini — pola yang
	 * sama seperti setter blok audit ({@link #setOleh(String)}, {@link #setOlehId(String)}).
	 *
	 * <p><b>Catatan kode mati:</b> ekspresi ternary di badan method ini
	 * ({@code this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)
	 * ? this.disposisiSop : disposisiSop}) tidak pernah bisa memilih cabang pertama pada
	 * praktiknya — kondisi {@code disposisiSop == null || disposisiSop.getId() == null} di dalam
	 * ternary sudah dijamin bernilai {@code false} oleh early-return di baris sebelumnya (method
	 * ini sudah berhenti lebih dulu bila kondisi itu benar). Akibatnya baris ini secara efektif
	 * setara dengan {@code this.disposisiSop = disposisiSop;} sederhana; ternary-nya adalah sisa
	 * refactoring yang tidak berbahaya (tidak mengubah perilaku) namun membingungkan pembaca.
	 *
	 * @param disposisiSop disposisi SOP yang ingin ditautkan; {@code null} atau entitas yang
	 *                      belum tersimpan diabaikan diam-diam
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}
}
