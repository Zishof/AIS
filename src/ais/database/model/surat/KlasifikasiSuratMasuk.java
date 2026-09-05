package ais.database.model.surat;

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

import ais.common.ConstantValues;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * KATALOG klasifikasi (kategori) surat masuk: satu baris menetapkan sekaligus penomoran agenda,
 * alur disposisi, sifat surat, cakupan unit organisasi, hak lihat berbasis role, aturan peminjaman
 * arsip, dan daftar field tambahan dinamis untuk setiap {@link SuratMasuk} yang memakainya.
 *
 * <h3>Peran: satu tabel master yang mengendalikan hampir seluruh perilaku surat masuk</h3>
 *
 * <p>{@link SuratMasuk} tidak hanya MERUJUK klasifikasi, tetapi MENURUNKAN sebagian besar
 * atributnya dari sini. Getter-getter {@code SuratMasuk#getJurusan()}, {@code getFakultas()},
 * {@code getSekolah()}, {@code getYayasan()}, {@code getSatuanKerja()}, {@code getSifat()},
 * {@code getSifatSurat()}, {@code getPerihal()}, dan {@code getAlurPersetujuanSuratMasuk()}
 * semuanya menimpa nilai milik surat dengan nilai klasifikasi bila klasifikasi mengisinya.
 * Akibatnya klasifikasi bertindak sebagai sumber kebenaran, dan mengubah satu baris di sini
 * mengubah pembacaan SELURUH surat masuk yang memakainya secara surut -- termasuk surat lama
 * yang sudah selesai diproses.</p>
 *
 * <h3>Penomoran agenda</h3>
 *
 * <p>Kode agenda surat masuk dibangkitkan {@code SuratMasukAction.generateCode(...)} dengan dua
 * jalur yang dipilih oleh {@link #getNomorSurat()}:</p>
 * <ul>
 *   <li>bila {@link #getNomorSurat()} terisi, format diserahkan sepenuhnya kepada mesin
 *       {@link NomorSurat} ({@code format(index, tanggal)}), dengan urutan yang diambil dari
 *       {@code getNomorIndex()} bila {@code gunakanIndexUrut} aktif -- dan dinaikkan lewat
 *       {@code NomorSurat.tambahIndexNomorSurat(...)} -- atau dari pencacahan baris surat masuk
 *       sejenis bila tidak;</li>
 *   <li>bila kosong, format cadangan dirakit dari {@link #getPrefix()}, nomor urut empat digit,
 *       bulan Romawi, tahun, dan {@link #getPostfix()}.</li>
 * </ul>
 * <p>Pada kedua jalur, penanda teks {@code KODE_KLASIFIKASI} dalam hasil akhir digantikan
 * {@link #getKode()}.</p>
 *
 * <h3>PERINGATAN: dua getter destruktif yang MENGHAPUS konfigurasi</h3>
 *
 * <p>Kelas ini dipetakan dengan {@code dynamicUpdate = true} dan akses properti lewat getter,
 * sehingga apa pun yang ditulis sebuah getter ke field ikut ter-flush ke basis data. Dua getter
 * di kelas ini menulis, dan yang ditulis adalah nilai KOSONG/NULL:</p>
 * <ul>
 *   <li>{@link #getPostfix()} mengosongkan field {@code prefix} -- bukan {@code postfix};
 *       lihat javadoc metode itu;</li>
 *   <li>{@link #getAlurPersetujuanSuratMasuk()} me-{@code null}-kan relasi alur bila
 *       {@link #getTanpaAlur()} bernilai benar.</li>
 * </ul>
 * <p>Keduanya bukan sekadar kejanggalan gaya: nilai yang dihapus adalah konfigurasi yang
 * diketikkan admin dan tidak dapat dipulihkan selain dari tabel revisi Envers.</p>
 *
 * <h3>Hak lihat surat berbasis role</h3>
 *
 * <p>{@link #getKodeGrupPengguna()} adalah gerbang kelihatan-tidaknya surat pada dasbor.
 * {@code DasboardSurat.createSuratMasukVisibilityCriterion(...)} menyusun kriteria "pengguna
 * adalah konseptor surat ATAU {@code kodeGrupPengguna} klasifikasi memuat token
 * {@code ;roleId;}" bagi pengguna yang role-nya tidak memiliki {@code melihatSemuaSurat}.
 * Karena token dicari dengan {@code ilike ANYWHERE}, kolom kosong TIDAK cocok dengan role mana
 * pun -- untuk cabang ini perilakunya menutup, bukan membuka. Perlu diketahui bahwa metode
 * penyusun kriteria itu berakhir dengan kriteria "selalu benar" untuk bentuk pengguna yang tidak
 * tertangani cabang mana pun DAN pada blok {@code catch}-nya; jadi kegagalan tak terduga saat
 * menyusun penyaring berujung membuka, bukan menutup.</p>
 *
 * <h3>Field yang tidak punya pembaca (dormant)</h3>
 *
 * <p>{@link #getMasaBerlakuSurat()} dan {@link #getStatusDipertahankan()} -- keduanya bertema
 * retensi/penyusutan arsip -- ditelusuri ke seluruh basis kode dan HANYA muncul di
 * {@code KlasifikasiSuratMasukAction}: sekali sebagai {@code Combobox} pada formulir, sekali
 * sebagai {@code Label} pada daftar. Tidak ada satu pun mesin retensi, penjadwal, atau laporan
 * yang membacanya. Keduanya karena itu berstatus metadata tidur: benar secara data, tanpa akibat
 * apa pun terhadap perilaku sistem.</p>
 *
 * <h3>Field tambahan dinamis</h3>
 *
 * <p>Rantainya dua tingkat dan khas modul ini: klasifikasi ini &rarr;
 * {@link KlasifikasiSuratMasukParemeter} (definisi field) &rarr;
 * {@link KlasifikasiSuratMasukParemeterValue} (nilai per surat). BUKAN mekanisme generik
 * {@code ais.database.model.ParameterTambahan} milik modul aset/penggajian/koperasi.</p>
 *
 * @see SuratMasuk dokumen yang menurunkan atributnya dari klasifikasi ini
 * @see NomorSurat mesin format penomoran agenda
 * @see AlurPersetujuanSuratMasuk akar alur disposisi bawaan
 * @see KlasifikasiSuratMasukParemeter definisi field tambahan dinamis
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "klasifikasi_surat_masuk")
public class KlasifikasiSuratMasuk extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java; nilainya sama dengan hampir seluruh entitas hasil templat
	 * hbm2java di basis kode ini karena disalin dari templat generator yang sama, bukan karena
	 * kelas-kelas tersebut kompatibel secara biner satu sama lain.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, kolom {@code id}; dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama tampil pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String oleh;
	/** Id pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String olehId;

	/**
	 * Id pengguna penyunting terakhir.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * <p>Pengabaian nilai kosong adalah KEHARUSAN TEKNIS. Baris klasifikasi ikut tersimpan
	 * ulang oleh jalur-jalur yang berjalan tanpa konteks pengguna aktif -- antara lain
	 * penulisan balik nilai turunan oleh getter destruktif yang dijelaskan pada javadoc kelas.
	 * Bila setter ini menerima nilai kosong, jejak "diubah oleh siapa" pada baris master akan
	 * terhapus oleh proses yang bukan perbuatan manusia.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks klasifikasi, yaitu nama katalognya apa adanya.
	 *
	 * <p>Nilai diambil langsung dari field, bukan lewat {@link #getNama()}, sehingga bisa
	 * {@code null} dan tidak di-{@code trim}. Metode ini yang muncul pada kotak pilihan
	 * klasifikasi di formulir surat masuk dan pada label ringkas di berbagai dasbor.</p>
	 *
	 * @return nama klasifikasi, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna penyunting terakhir.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA: menyerahkan pembaruan stempel waktu dan identitas pengubah
	 * ke {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate menulis perubahan
	 * baris ini ke basis data.
	 *
	 * <p>Kait hanya berjalan pada UPDATE, bukan INSERT; nilai awal {@code tanggal_dirubah}
	 * diberikan lewat inisialisasi field yang ditulis pada baris yang sama dengan metode ini.
	 * Karena kelas beranotasi {@code @Audited}, Envers menyimpan riwayat versi terpisah; trio
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah jejak audit BAYANGAN pada
	 * baris hidup agar tampilan tidak perlu menyentuh tabel revisi hanya untuk menampilkan
	 * "diubah oleh siapa, kapan" -- keharusan teknis, bukan duplikasi yang keliru.</p>
	 *
	 * <p>Untuk kelas ini kait tersebut punya efek samping yang perlu disadari: karena getter
	 * destruktif pada kelas ini dapat mengubah field tanpa perbuatan pengguna, sebuah baris
	 * bisa ter-UPDATE -- dan karenanya tercatat sebagai "diubah" di Envers -- hanya karena
	 * pernah dibaca.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi karena
	 * {@link #onUpdate()} sudah mengurusnya; disediakan untuk impor dan perbaikan data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; untuk objek baru berisi waktu pembuatan objek
	 *         karena field diinisialisasi pada deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode ringkas klasifikasi; menggantikan penanda {@code KODE_KLASIFIKASI} pada nomor agenda. */
	private String kode;
	/** Nama klasifikasi yang tampil di seluruh kotak pilihan dan daftar surat masuk. */
	private String nama;
	/**
	 * Awalan nomor agenda pada jalur penomoran cadangan (tanpa {@link NomorSurat}).
	 * DAPAT TERHAPUS sebagai efek samping {@link #getPostfix()}; lihat javadoc metode itu.
	 */
	private String prefix;
	/** Akhiran nomor agenda pada jalur penomoran cadangan (tanpa {@link NomorSurat}). */
	private String postfix;
	/** Mesin format penomoran agenda; bila terisi, ia menggantikan jalur prefix/postfix. */
	private NomorSurat nomorSurat;
	/** Penjelasan bebas mengenai kegunaan klasifikasi ini. */
	private String keterangan;
	/** Akar alur disposisi bawaan yang diwariskan ke setiap surat masuk berklasifikasi ini. */
	private AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk;
	/** Cakupan fakultas; bila terisi, MENIMPA fakultas pada surat masuk yang memakainya. */
	private Fakultas fakultas;
	/** Cakupan jurusan; bila terisi, MENIMPA jurusan pada surat masuk yang memakainya. */
	private Jurusan jurusan;
	/** Penanda klasifikasi masih boleh dipilih pada formulir surat masuk baru. */
	private Boolean aktif;
	/** Daftar role berpemisah titik koma yang boleh melihat surat berklasifikasi ini. */
	private String kodeGrupPengguna;
	/** Sifat surat dalam bentuk teks bebas; peninggalan sebelum adanya entitas {@link SifatSurat}. */
	private String sifat;
	/** Cakupan sekolah; bila terisi, MENIMPA sekolah pada surat masuk yang memakainya. */
	private Sekolah sekolah;
	/** Cakupan satuan kerja; bila terisi, MENIMPA satuan kerja pada surat masuk yang memakainya. */
	private SatuanKerja satuanKerja;
	/** Cakupan yayasan; bila terisi, MENIMPA yayasan pada surat masuk yang memakainya. */
	private Yayasan yayasan;
	/** Perihal bawaan yang mengisi formulir surat masuk selama operator belum mengetik sendiri. */
	private String perihalDefault;
	/** Penanda klasifikasi ini sengaja tidak memakai alur disposisi. */
	private Boolean tanpaAlur;
	/** Metadata masa berlaku arsip; TIDAK dibaca mesin mana pun (lihat javadoc kelas). */
	private MasaBerlakuSurat masaBerlakuSurat;
	/** Metadata status retensi arsip; TIDAK dibaca mesin mana pun (lihat javadoc kelas). */
	private StatusDipertahankan statusDipertahankan;
	/** Sifat surat sebagai entitas; menggantikan peran field teks {@link #sifat}. */
	private SifatSurat sifatSurat;

	/** Penanda ragam katalog, berasal dari parameter permintaan {@code tipe} (bawaan {@code "surat"}). */
	private String tipe;

	/** Penanda arsip berklasifikasi ini boleh dipinjam lewat modul sirkulasi surat. */
	private Boolean bolehDipinjam;
	/** Batas hari peminjaman arsip; bawaan tujuh hari. */
	private Integer maksimalHariPinjam;
	/** Batas jumlah perpanjangan peminjaman; bawaan dua kali. Ejaan field memang bertypo. */
	private Integer maksimalJumlahPerpanjaangan;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate. Baris baru belum sah disimpan sebelum
	 * {@link #setNama} diisi karena kolom {@code nama} beranotasi {@code nullable = false};
	 * sisa atributnya punya nilai bawaan yang diberikan oleh getter masing-masing.
	 */
	public KlasifikasiSuratMasuk() {
	}

	/**
	 * Kunci utama baris.
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
	 * Mengisi kunci utama. Hanya dipakai Hibernate dan proses impor.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama klasifikasi, sudah di-{@code trim}.
	 *
	 * <p>Nama ini juga menjadi cadangan {@link #getPerihalDefault()} bila perihal bawaan belum
	 * diisi, sehingga mengubah nama klasifikasi ikut mengubah perihal yang terisi otomatis pada
	 * formulir surat masuk baru.</p>
	 *
	 * @return nama klasifikasi tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama klasifikasi.
	 *
	 * @param nama nama klasifikasi
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Penjelasan bebas mengenai kegunaan klasifikasi ini.
	 *
	 * @return penjelasan bebas, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi penjelasan bebas klasifikasi.
	 *
	 * @param keterangan penjelasan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Perihal bawaan untuk surat masuk berklasifikasi ini, dengan cadangan {@link #getNama()}.
	 *
	 * <p>Dipakai {@code SuratMasuk#getPerihal()}: selama surat belum punya perihal sendiri,
	 * pembacaan perihal surat MENGISI dirinya dari nilai ini. Karena {@code SuratMasuk} juga
	 * dipetakan {@code dynamicUpdate}, pengisian itu bersifat menetap begitu surat tersimpan --
	 * jadi perihal bawaan "menempel" pada surat pada pembacaan pertama, dan perubahan perihal
	 * bawaan di kemudian hari tidak lagi memengaruhi surat tersebut.</p>
	 *
	 * <p>Berbeda dengan getter lain di kelas ini, metode ini TIDAK destruktif: cadangan
	 * {@code getNama()} dikembalikan tanpa ditulis ke field {@code perihalDefault}, sehingga
	 * kolomnya tetap kosong dan tautan "ikut nama" tetap hidup bila nama diubah.</p>
	 *
	 * @return perihal bawaan; nama klasifikasi bila perihal bawaan belum diisi
	 */
	@Column(name = "perihal_default", columnDefinition = "text", nullable = true)
	public String getPerihalDefault() {
		return perihalDefault == null || perihalDefault.trim().isEmpty() ? getNama() : perihalDefault;
	}

	/**
	 * Mengisi perihal bawaan. Mengisinya dengan teks kosong sama dengan mengembalikan perilaku
	 * "ikut nama klasifikasi".
	 *
	 * @param perihalDefault perihal bawaan; boleh {@code null} atau kosong
	 */
	public void setPerihalDefault(String perihalDefault) {
		this.perihalDefault = perihalDefault;
	}

	/**
	 * Mengisi awalan nomor agenda pada jalur penomoran cadangan.
	 *
	 * <p>PERHATIAN: nilai yang diisi di sini tidak dijamin bertahan. {@link #getPrefix()}
	 * mengosongkannya bila {@link #getNomorSurat()} terisi, dan {@link #getPostfix()}
	 * mengosongkannya tanpa syarat yang berarti. Lihat javadoc kedua metode tersebut.</p>
	 *
	 * @param prefix awalan nomor agenda
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Awalan nomor agenda pada jalur penomoran cadangan, dinormalkan ke teks kosong.
	 *
	 * <h3>Getter destruktif: awalan dihapus saat mesin penomoran dipasang</h3>
	 *
	 * <p>Bila {@code nomorSurat} terisi, metode ini MENULIS teks kosong ke field
	 * {@code prefix} lalu mengembalikannya. Niatnya jelas dan masuk akal: begitu format nomor
	 * diserahkan kepada mesin {@link NomorSurat}, awalan lama tidak boleh ikut menempel pada
	 * hasil. Yang perlu disadari adalah cara mencapainya -- karena {@code prefix} adalah
	 * properti persisten dan kelas dipetakan {@code dynamicUpdate = true}, pengosongan itu
	 * ter-flush ke basis data. Awalan yang pernah diketik admin karena itu HILANG PERMANEN
	 * pada pembacaan pertama setelah mesin penomoran dipasang, dan tidak kembali bila mesin
	 * penomoran dilepas lagi; pemulihannya hanya lewat tabel revisi Envers.</p>
	 *
	 * <p>Perhatikan pula pemeriksaan pada {@code SuratMasukAction.generateCode(...)} berbentuk
	 * {@code getPrefix().equals("")}, yang aman justru karena normalisasi di sini menjamin
	 * hasilnya tidak pernah {@code null}.</p>
	 *
	 * @return awalan nomor agenda; teks kosong bila belum diisi atau bila mesin penomoran
	 *         sedang dipakai, tidak pernah {@code null}
	 */
	public String getPrefix() {
		if (prefix == null) {
			prefix = "";
		}
		if (nomorSurat != null) {
			prefix = "";
		}
		return prefix;
	}

	/**
	 * Akhiran nomor agenda pada jalur penomoran cadangan, dinormalkan ke teks kosong.
	 *
	 * <h3>PERINGATAN: getter ini mengosongkan {@code prefix}, bukan {@code postfix}</h3>
	 *
	 * <p>Setelah menormalkan {@code postfix} yang {@code null} menjadi teks kosong, metode ini
	 * menjalankan {@code if (postfix != null) { prefix = ""; }}. Dua hal membuat baris itu
	 * bermasalah. Pertama, syaratnya tidak pernah salah: normalisasi tepat di atasnya menjamin
	 * {@code postfix} selalu bukan {@code null} ketika syarat diperiksa, sehingga blok itu
	 * SELALU dijalankan. Kedua, field yang dikosongkan adalah {@code prefix} -- field yang
	 * berbeda dari yang sedang dibaca metode ini. Bentuknya khas kekeliruan salin-tempel dari
	 * {@link #getPrefix()} yang memang sengaja mengosongkan {@code prefix}.</p>
	 *
	 * <p>Akibatnya, membaca akhiran nomor agenda menghapus awalan nomor agenda. Karena kelas
	 * dipetakan dengan {@code dynamicUpdate = true} dan akses properti lewat getter, penghapusan
	 * itu ikut ter-flush ke basis data pada transaksi berjalan. Pemicunya bukan penyuntingan
	 * oleh admin, melainkan sekadar PEMBACAAN: daftar klasifikasi di
	 * {@code KlasifikasiSuratMasukAction} merangkai {@code Label} dari {@code getPrefix()} lalu
	 * {@code getPostfix()} secara berurutan pada entitas yang berada dalam sesi Hibernate, dan
	 * formulir suntingnya melakukan hal serupa saat memuat kotak isian. Nilai yang tampil pada
	 * layar masih benar karena {@code getPrefix()} dievaluasi lebih dulu; yang tersimpan
	 * kemudian ke basis data sudah kosong.</p>
	 *
	 * <p>Cakupan kerusakannya terbatas pada klasifikasi yang BELUM memakai {@link NomorSurat},
	 * sebab hanya jalur penomoran cadangan yang membaca {@code prefix}. Untuk klasifikasi
	 * seperti itu, nomor agenda surat berikutnya kehilangan awalannya tanpa pemberitahuan.
	 * Kembarannya untuk surat keluar, {@code KlasifikasiSuratKeluar}, TIDAK memiliki cacat ini
	 * karena kedua metode prefix/postfix di sana seluruhnya dikomentari.</p>
	 *
	 * @return akhiran nomor agenda; teks kosong bila belum diisi, tidak pernah {@code null}
	 */
	public String getPostfix() {
		if (postfix == null) {
			postfix = "";
		}
		if (postfix != null) {
			prefix = "";
		}
		return postfix;
	}

	/**
	 * Mengisi akhiran nomor agenda pada jalur penomoran cadangan.
	 *
	 * @param postfix akhiran nomor agenda
	 */
	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}

	/**
	 * Akar alur disposisi bawaan bagi surat masuk berklasifikasi ini.
	 *
	 * <h3>Getter destruktif: relasi di-null-kan bila klasifikasi ditandai tanpa alur</h3>
	 *
	 * <p>Setelah membongkar proxy lazy lewat {@code check(...)}, metode ini me-{@code null}-kan
	 * field bila {@link #getTanpaAlur()} bernilai benar. Sama seperti pengosongan pada
	 * {@link #getPrefix()}, penulisan itu mengenai properti persisten pada kelas yang dipetakan
	 * {@code dynamicUpdate = true}, sehingga kunci asing {@code alur_persetujuan_surat_masuk}
	 * benar-benar dikosongkan di basis data. Mencentang "tanpa alur" karena itu bersifat SATU
	 * ARAH: menghilangkan centangnya kemudian tidak mengembalikan alur yang dulu dipilih, dan
	 * admin harus memilih ulang dari awal.</p>
	 *
	 * <h3>Bagaimana alur ini sampai ke surat</h3>
	 *
	 * <p>{@code SuratMasuk#getAlurPersetujuanSuratMasuk()} menimpa alur miliknya sendiri dengan
	 * nilai dari sini setiap kali klasifikasi mengisinya, dan formulir surat masuk
	 * ({@code SuratMasukAction}) me-{@code setDisabled(true)} kotak pilihan alur begitu
	 * klasifikasi membawa alur -- jadi operator tidak dapat menyimpang dari alur yang
	 * ditetapkan klasifikasi. Rantai simpul alurnya kemudian dibentangkan
	 * {@code checkAlurPersetujuanSuratMasukStatus(...)}, yang menelusuri {@code getParent()}
	 * dari simpul yang dipilih sampai akar dan membuat satu
	 * {@link AlurPersetujuanSuratMasukStatus} untuk setiap tingkat.</p>
	 *
	 * @return akar alur disposisi bawaan, atau {@code null} bila klasifikasi ditandai tanpa alur
	 *         maupun bila memang belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_masuk", nullable = true)
	public AlurPersetujuanSuratMasuk getAlurPersetujuanSuratMasuk() {
		alurPersetujuanSuratMasuk = check(alurPersetujuanSuratMasuk);
		if (getTanpaAlur()) {
			alurPersetujuanSuratMasuk = null;
		}
		return alurPersetujuanSuratMasuk;
	}

	/**
	 * Menetapkan akar alur disposisi bawaan klasifikasi ini.
	 *
	 * <p>Nilai yang diisi di sini akan dihapus oleh {@link #getAlurPersetujuanSuratMasuk()}
	 * pada pembacaan berikutnya bila {@link #getTanpaAlur()} bernilai benar.</p>
	 *
	 * @param alurPersetujuanSuratMasuk akar alur disposisi; boleh {@code null}
	 */
	public void setAlurPersetujuanSuratMasuk(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk) {
		this.alurPersetujuanSuratMasuk = alurPersetujuanSuratMasuk;
	}

	/**
	 * Cakupan fakultas klasifikasi ini.
	 *
	 * <p>Relasi LAZY, karena itu proxy dibongkar lebih dulu lewat {@code check(...)} milik
	 * {@code GeneralValueObject}. Bila terisi, nilai ini MENIMPA fakultas pada setiap
	 * {@link SuratMasuk} yang memakai klasifikasi ini -- lihat {@code SuratMasuk#getFakultas()},
	 * yang menulis nilai klasifikasi ke field miliknya sendiri.</p>
	 *
	 * @return fakultas cakupan, atau {@code null} bila klasifikasi berlaku lintas fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan cakupan fakultas klasifikasi ini.
	 *
	 * @param fakultas fakultas cakupan; {@code null} berarti lintas fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Cakupan jurusan klasifikasi ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Bila terisi, nilai ini
	 * MENIMPA jurusan pada setiap {@link SuratMasuk} yang memakai klasifikasi ini.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} bila klasifikasi berlaku lintas jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan cakupan jurusan klasifikasi ini.
	 *
	 * @param jurusan jurusan cakupan; {@code null} berarti lintas jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Kode ringkas klasifikasi, dinormalkan ke teks kosong.
	 *
	 * <p>Kode ini bukan sekadar label: {@code SuratMasukAction.generateCode(...)} menggantikan
	 * penanda teks {@code KODE_KLASIFIKASI} di dalam nomor agenda yang sudah jadi dengan nilai
	 * ini, pada KEDUA jalur penomoran. Penggantiannya memakai
	 * {@code StringUtils.replaceIgnoreCase} di dalam {@code try/catch} yang, bila gagal,
	 * membiarkan penanda mentah {@code KODE_KLASIFIKASI} tetap tercetak pada nomor agenda.</p>
	 *
	 * <p>Getter ini destruktif ringan: bila field masih {@code null} ia menuliskan teks kosong
	 * ke field. Normalisasi itu diperlukan karena pemanggil memakai hasilnya langsung sebagai
	 * pengganti tanpa pemeriksaan {@code null}.</p>
	 *
	 * @return kode klasifikasi; teks kosong bila belum diisi, tidak pernah {@code null}
	 */
	public String getKode() {
		if (kode == null) {
			kode = "";
		}
		return kode;
	}

	/**
	 * Mengisi kode ringkas klasifikasi.
	 *
	 * @param kode kode klasifikasi
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Penanda klasifikasi masih boleh dipakai, dengan bawaan aktif.
	 *
	 * <p>Getter ini destruktif ringan: {@code null} dinaikkan menjadi {@code true} dan ditulis
	 * ke field, sehingga baris lama yang kolomnya kosong menjadi aktif secara eksplisit begitu
	 * dibaca. Perilaku itu selaras dengan penyaring di sisi pembaca, yang berbentuk
	 * "{@code aktif} bernilai null ATAU {@code aktif} bernilai benar" -- misalnya pada
	 * {@code AmbilDataKlasifikasiSuratMasukBanbox} yang mengisi kotak pilihan klasifikasi.
	 * Dengan kata lain flag ini SATU ARAH: kosong dianggap aktif, dan hanya {@code false}
	 * eksplisit yang menyembunyikan klasifikasi dari pemilihan.</p>
	 *
	 * <p>Menonaktifkan klasifikasi hanya menghentikan PEMILIHANNYA pada surat baru. Surat lama
	 * yang sudah memakainya tetap menurunkan seluruh atribut dari klasifikasi nonaktif ini,
	 * karena tidak ada satu pun getter penurun atribut di {@link SuratMasuk} yang memeriksa
	 * flag ini.</p>
	 *
	 * @return {@code true} bila klasifikasi masih boleh dipilih; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Mengisi penanda aktif klasifikasi.
	 *
	 * @param aktif {@code false} untuk menyembunyikan klasifikasi dari pemilihan surat baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Daftar role yang boleh melihat surat berklasifikasi ini, dinormalkan menjadi rangkaian
	 * token berpembatas titik koma.
	 *
	 * <h3>Bentuk normal dan alasannya</h3>
	 *
	 * <p>Isi kolom dibungkus titik koma di kedua ujung lalu deretan titik koma ganda diciutkan
	 * (tiga kali {@code replaceAll(";;", ";")}), dan beberapa bentuk sisa yang hanya berisi
	 * titik koma dipulangkan menjadi teks kosong. Normalisasi ini KEHARUSAN TEKNIS, bukan
	 * hiasan: pencocokan di sisi pembaca dilakukan dengan
	 * {@code Restrictions.ilike(..., ";" + roleId + ";", MatchMode.ANYWHERE)}, sehingga tanpa
	 * titik koma pembungkus, role {@code "AD"} akan ikut cocok dengan entri {@code "ADM"}.
	 * Penciutan bertingkat tiga kali itu sendiri hanya menangani sampai tujuh titik koma
	 * berurutan; masukan yang lebih kotor dari itu lolos dengan token kosong di tengah, yang
	 * tidak berbahaya karena tidak ada role ber-id kosong.</p>
	 *
	 * <p>Getter ini destruktif: bentuk normal ditulis kembali ke field dan karena itu tersimpan
	 * ke basis data. Untuk kolom ini sifat tersebut justru diperlukan agar data lama ikut
	 * ternormalkan.</p>
	 *
	 * <h3>Perannya sebagai gerbang kelihatan-tidaknya surat</h3>
	 *
	 * <p>{@code DasboardSurat.createSuratMasukVisibilityCriterion(...)} memakai kolom ini untuk
	 * pengguna yang role-nya TIDAK memiliki {@code melihatSemuaSurat} dan bukan mahasiswa
	 * maupun siswa: yang bersangkutan hanya melihat surat yang ia konsep sendiri, ATAU surat
	 * yang klasifikasinya memuat token role-nya di sini. Kolom kosong tidak cocok dengan role
	 * mana pun, jadi untuk cabang ini kosong berarti MENUTUP.</p>
	 *
	 * <p>Yang perlu diketahui saat mengandalkan kolom ini sebagai kendali akses: metode
	 * penyusun kriteria tersebut mengembalikan kriteria "selalu benar" untuk bentuk pengguna
	 * yang tidak tertangani cabang mana pun, dan juga pada blok {@code catch}-nya. Kolom ini
	 * karena itu bukan gerbang yang berdiri sendiri -- ia hanya berlaku bagi pengguna yang
	 * sampai ke cabang berbasis role.</p>
	 *
	 * @return daftar role dalam bentuk {@code ";role1;role2;"}, atau teks kosong bila tidak
	 *         dibatasi; tidak pernah {@code null}
	 */
	@Column(name = "kode_grup_pengguna", columnDefinition = "text", nullable = true)
	public String getKodeGrupPengguna() {
		if (kodeGrupPengguna == null) {
			kodeGrupPengguna = "";
		}

		kodeGrupPengguna = (kodeGrupPengguna == null || kodeGrupPengguna.trim().equalsIgnoreCase(";") ? ""
				: ";" + kodeGrupPengguna.trim() + ";").replaceAll(";;", ";").replaceAll(";;", ";")
				.replaceAll(";;", ";");

		if (kodeGrupPengguna.equals(";")) {
			kodeGrupPengguna = "";
		} else if (kodeGrupPengguna.equals(";;")) {
			kodeGrupPengguna = "";
		} else if (kodeGrupPengguna.equals(";;;")) {
			kodeGrupPengguna = "";
		} else if (kodeGrupPengguna.equals(";;;;")) {
			kodeGrupPengguna = "";
		}

		return kodeGrupPengguna;
	}

	/**
	 * Mengisi daftar role yang boleh melihat surat berklasifikasi ini.
	 *
	 * <p>Nilai mentah apa pun boleh dikirim; {@link #getKodeGrupPengguna()} yang akan
	 * menormalkannya menjadi bentuk bertitik koma pada pembacaan berikutnya.</p>
	 *
	 * @param kodeGrupPengguna daftar role, dipisah titik koma
	 */
	public void setKodeGrupPengguna(String kodeGrupPengguna) {
		this.kodeGrupPengguna = kodeGrupPengguna;
	}

	/**
	 * Sifat surat dalam bentuk teks, dengan bawaan {@code "Biasa"}.
	 *
	 * <p>Field ini peninggalan dari masa sebelum sifat surat menjadi entitas tersendiri. Kini
	 * ia berperan sebagai jembatan: {@link #getSifatSurat()} memakainya untuk MENCARI entitas
	 * {@link SifatSurat} yang namanya sama bila relasi entitasnya belum terisi. Di hilir,
	 * {@code SuratMasuk#getSifat()} menimpa sifat miliknya dengan NAMA entitas sifat milik
	 * klasifikasi, bukan dengan teks ini -- sehingga teks di sini hanya berpengaruh selama
	 * pemetaan ke entitas berhasil.</p>
	 *
	 * <p>Getter destruktif ringan: {@code null} atau teks kosong ditulis menjadi {@code "Biasa"}.</p>
	 *
	 * @return sifat surat dalam bentuk teks; {@code "Biasa"} bila belum diisi
	 */
	public String getSifat() {
		if (sifat == null || sifat.trim().isEmpty()) {
			sifat = "Biasa";
		}
		return sifat;
	}

	/**
	 * Mengisi sifat surat dalam bentuk teks.
	 *
	 * @param sifat sifat surat; kosong akan dipulihkan menjadi {@code "Biasa"} saat dibaca
	 */
	public void setSifat(String sifat) {
		this.sifat = sifat;
	}

	/**
	 * Mesin format penomoran agenda untuk klasifikasi ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Keberadaan nilai di
	 * sini adalah SAKELAR antara dua jalur penomoran di
	 * {@code SuratMasukAction.generateCode(...)}: bila terisi, seluruh format diserahkan ke
	 * {@link NomorSurat#format(Long, java.util.Date)} beserta aturan reset urutan per tahun,
	 * pengurutan per kelompok, dan pemakaian indeks tersimpan; bila kosong, dipakai format
	 * cadangan {@code prefix/NNNN/BulanRomawi/Tahun/postfix}.</p>
	 *
	 * <p>Memasang mesin penomoran punya efek samping merusak yang perlu diketahui: pembacaan
	 * {@link #getPrefix()} berikutnya akan MENGHAPUS awalan yang dulu diketik admin. Lihat
	 * javadoc metode tersebut.</p>
	 *
	 * @return mesin format penomoran, atau {@code null} bila memakai format cadangan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Menetapkan mesin format penomoran agenda klasifikasi ini.
	 *
	 * @param nomorSurat mesin format penomoran; {@code null} berarti memakai format cadangan
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

	/**
	 * Cakupan satuan kerja klasifikasi ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Bila terisi, nilai ini
	 * MENIMPA satuan kerja pada setiap {@link SuratMasuk} yang memakai klasifikasi ini --
	 * {@code SuratMasuk#getSatuanKerja()} bahkan menomorduakan nilai milik surat sendiri dan
	 * hanya memakainya bila klasifikasi tidak mengisi. Konsekuensinya, cakupan satuan kerja
	 * sebuah surat masuk tidak dapat menyimpang dari klasifikasinya, dan memindahkan sebuah
	 * klasifikasi ke satuan kerja lain memindahkan pula seluruh surat lamanya secara surut.</p>
	 *
	 * @return satuan kerja cakupan, atau {@code null} bila klasifikasi berlaku lintas satuan kerja
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan cakupan satuan kerja klasifikasi ini.
	 *
	 * @param satuanKerja satuan kerja cakupan; {@code null} berarti lintas satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Cakupan sekolah klasifikasi ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Bila terisi, nilai ini
	 * MENIMPA sekolah pada setiap {@link SuratMasuk} yang memakai klasifikasi ini.</p>
	 *
	 * @return sekolah cakupan, atau {@code null} bila klasifikasi berlaku lintas sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan cakupan sekolah, MENOLAK objek yang belum tersimpan.
	 *
	 * <p>Objek {@link Sekolah} yang id-nya masih {@code null} diperlakukan sama dengan
	 * {@code null}. Penjagaan itu mencegah cascade {@code PERSIST} menyimpan diam-diam baris
	 * sekolah baru yang sebetulnya hanya wadah kosong dari kotak pilihan.</p>
	 *
	 * @param sekolah sekolah cakupan; objek tanpa id diperlakukan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Cakupan yayasan klasifikasi ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Bila terisi, nilai ini
	 * MENIMPA yayasan pada setiap {@link SuratMasuk} yang memakai klasifikasi ini.</p>
	 *
	 * @return yayasan cakupan, atau {@code null} bila klasifikasi berlaku lintas yayasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan cakupan yayasan, MENOLAK objek yang belum tersimpan dengan alasan yang sama
	 * seperti {@link #setSekolah(Sekolah)}.
	 *
	 * @param yayasan yayasan cakupan; objek tanpa id diperlakukan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Penanda klasifikasi ini sengaja tidak memakai alur disposisi, dengan bawaan {@code false}.
	 *
	 * <p>Berbeda dari getter lain di kelas ini, metode ini TIDAK menulis bawaannya ke field:
	 * {@code null} diterjemahkan menjadi {@code false} hanya pada nilai kembalian, sehingga
	 * kolomnya tetap kosong. Nilai kembaliannya dipakai {@link #getAlurPersetujuanSuratMasuk()}
	 * untuk memutuskan apakah relasi alur perlu dihapus -- dan penghapusan itulah yang bersifat
	 * menetap, bukan flag ini sendiri.</p>
	 *
	 * @return {@code true} bila klasifikasi ditandai tanpa alur disposisi; tidak pernah
	 *         {@code null}
	 */
	public Boolean getTanpaAlur() {
		return tanpaAlur == null ? false : tanpaAlur;
	}

	/**
	 * Menandai klasifikasi ini memakai atau tidak memakai alur disposisi.
	 *
	 * <p>PERHATIAN: mengisinya dengan {@code true} bersifat SATU ARAH terhadap konfigurasi alur.
	 * Pembacaan {@link #getAlurPersetujuanSuratMasuk()} berikutnya akan menghapus relasi alur
	 * secara menetap, dan mengembalikan flag ini ke {@code false} tidak memulihkan pilihan
	 * alur yang lama.</p>
	 *
	 * @param tanpaAlur {@code true} bila klasifikasi tidak memakai alur disposisi
	 */
	public void setTanpaAlur(Boolean tanpaAlur) {
		this.tanpaAlur = tanpaAlur;
	}

	/**
	 * Masa berlaku arsip bawaan untuk surat berklasifikasi ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. FIELD INI TIDUR:
	 * penelusuran seluruh basis kode atas pemanggil {@code getMasaBerlakuSurat()} hanya
	 * menemukan dua tempat, keduanya di {@code KlasifikasiSuratMasukAction} -- sebuah
	 * {@code Combobox} pada formulir pengaturan dan sebuah {@code Label} pada daftar. Tidak ada
	 * mesin retensi, penjadwal penyusutan arsip, maupun laporan yang membacanya, dan
	 * {@link SuratMasuk} sendiri tidak memiliki relasi ke {@link MasaBerlakuSurat}. Nilai di
	 * sini karena itu murni catatan kebijakan bagi manusia, tanpa akibat apa pun terhadap
	 * perilaku sistem.</p>
	 *
	 * @return masa berlaku arsip bawaan, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masa_berlaku_surat", nullable = true)
	public MasaBerlakuSurat getMasaBerlakuSurat() {
		masaBerlakuSurat = check(masaBerlakuSurat);
		return masaBerlakuSurat;
	}

	/**
	 * Menetapkan masa berlaku arsip bawaan klasifikasi ini.
	 *
	 * @param masaBerlakuSurat masa berlaku arsip; boleh {@code null}
	 */
	public void setMasaBerlakuSurat(MasaBerlakuSurat masaBerlakuSurat) {
		this.masaBerlakuSurat = masaBerlakuSurat;
	}

	/**
	 * Status retensi arsip bawaan untuk surat berklasifikasi ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Sama seperti
	 * {@link #getMasaBerlakuSurat()}, FIELD INI TIDUR: satu-satunya pembaca adalah
	 * {@code KlasifikasiSuratMasukAction} untuk mengisi {@code Combobox} pada formulir dan
	 * menampilkan {@code Label} pada daftar. Tidak ada mekanisme penyusutan atau pemusnahan
	 * arsip yang mengonsumsinya.</p>
	 *
	 * @return status retensi arsip bawaan, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_dipertahankan", nullable = true)
	public StatusDipertahankan getStatusDipertahankan() {
		statusDipertahankan = check(statusDipertahankan);
		return statusDipertahankan;
	}

	/**
	 * Menetapkan status retensi arsip bawaan klasifikasi ini.
	 *
	 * @param statusDipertahankan status retensi arsip; boleh {@code null}
	 */
	public void setStatusDipertahankan(StatusDipertahankan statusDipertahankan) {
		this.statusDipertahankan = statusDipertahankan;
	}

	/**
	 * Sifat surat sebagai entitas, dengan pemulihan otomatis dari field teks {@link #getSifat()}.
	 *
	 * <h3>Jembatan dari data lama ke entitas</h3>
	 *
	 * <p>Setelah membongkar proxy lazy, metode ini menelusuri seluruh {@link SifatSurat} yang
	 * ter-cache di {@code ConstantValues} dan mengambil yang NAMANYA sama persis (mengabaikan
	 * besar kecil huruf dan spasi tepi) dengan teks {@link #getSifat()}. Ini jalur migrasi
	 * ringan bagi baris lama yang hanya punya sifat dalam bentuk teks, sebelum sifat surat
	 * menjadi tabel tersendiri.</p>
	 *
	 * <p>Getter ini destruktif: hasil pencarian ditulis ke field, sehingga kunci asing
	 * {@code sifat_surat} terisi permanen begitu baris pernah dibaca -- praktis sebuah
	 * backfill yang berjalan sendiri saat data diakses. Yang perlu diketahui: pencarian
	 * MENCOCOKKAN NAMA, bukan id. Bila ada dua entitas sifat bernama sama, yang terpilih adalah
	 * yang pertama ditemui pada iterasi cache, dan urutan iterasi itu tidak dijamin. Bila tidak
	 * ada yang cocok sama sekali, field tetap {@code null} dan pencarian diulang pada setiap
	 * pembacaan berikutnya.</p>
	 *
	 * <p>Di hilir, {@code SuratMasuk#getSifatSurat()} dan {@code SuratMasuk#getSifat()} sama-sama
	 * menimpa nilai milik surat dengan hasil dari sini, sehingga sifat sebuah surat masuk
	 * sepenuhnya ditentukan klasifikasinya selama klasifikasi mengisi.</p>
	 *
	 * @return entitas sifat surat, atau {@code null} bila tidak ada yang namanya cocok
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sifat_surat", nullable = true)
	public SifatSurat getSifatSurat() {
		sifatSurat = check(sifatSurat);
		if (sifatSurat == null && getSifat() != null && !getSifat().trim().isEmpty()) {
			for (Object o : ConstantValues.ambilBerdasarClass(SifatSurat.class).values()) {
				SifatSurat sf = (SifatSurat) o;
				if (sf != null && sf.getNama() != null && sf.getNama().trim().equalsIgnoreCase(getSifat().trim())) {
					sifatSurat = sf;
					break;
				}
			}
		}
		return sifatSurat;
	}

	/**
	 * Menetapkan entitas sifat surat klasifikasi ini.
	 *
	 * @param sifatSurat entitas sifat surat; boleh {@code null}
	 */
	public void setSifatSurat(SifatSurat sifatSurat) {
		this.sifatSurat = sifatSurat;
	}

	/**
	 * Penanda ragam katalog, dipakai memisahkan beberapa daftar klasifikasi dalam satu tabel.
	 *
	 * <p>Nilainya berasal dari parameter permintaan {@code tipe} pada halaman pengaturan
	 * ({@code KlasifikasiSuratMasukAction}), dengan bawaan {@code "surat"} bila parameter tidak
	 * dikirim. Halaman itu mengisi penanda ini ke baris yang belum punya nilai saat daftar
	 * dirender, dan menyetelnya pada setiap penyimpanan. Karena berasal dari parameter URL,
	 * nilainya adalah pengelompokan tampilan, BUKAN kendali akses -- ia tidak dipakai satu pun
	 * penyaring hak lihat, sehingga mengubah parameter itu hanya memindahkan baris antar daftar,
	 * tidak membuka data yang seharusnya tertutup.</p>
	 *
	 * <p>Berbeda dari kebanyakan getter di kelas ini, metode ini tidak menormalkan apa pun dan
	 * dapat mengembalikan {@code null} untuk baris lama.</p>
	 *
	 * @return penanda ragam katalog, atau {@code null} untuk baris yang belum pernah ditandai
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Mengisi penanda ragam katalog.
	 *
	 * @param tipe penanda ragam katalog
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Penanda arsip berklasifikasi ini boleh dipinjam, dengan bawaan BOLEH.
	 *
	 * <p>Perhatikan arah bawaannya: {@code null} diterjemahkan menjadi {@code true}, sehingga
	 * klasifikasi lama yang dibuat sebelum modul sirkulasi surat ada otomatis terbuka untuk
	 * peminjaman tanpa keputusan sadar dari admin. Nilai kembalian saja yang diubah -- field
	 * tidak ditulis -- jadi kolomnya tetap kosong di basis data.</p>
	 *
	 * @return {@code true} bila arsip boleh dipinjam; tidak pernah {@code null}
	 */
	public Boolean getBolehDipinjam() {
		return bolehDipinjam == null ? true : bolehDipinjam;
	}

	/**
	 * Menetapkan boleh tidaknya arsip berklasifikasi ini dipinjam.
	 *
	 * @param bolehDipinjam {@code false} untuk menutup peminjaman
	 */
	public void setBolehDipinjam(Boolean bolehDipinjam) {
		this.bolehDipinjam = bolehDipinjam;
	}

	/**
	 * Batas lama peminjaman arsip dalam hari, dengan bawaan tujuh.
	 *
	 * <p>Dibaca {@code PeminjamanSuratItemAction} lewat rantai
	 * {@code suratMasuk.getKlasifikasiSuratMasuk().getMaksimalHariPinjam()} untuk menentukan
	 * batas hari pengembalian. Pemanggil di sana masih memasang pemeriksaan {@code null}
	 * terhadap hasilnya meskipun getter ini tidak pernah mengembalikan {@code null} -- sisa
	 * penjagaan dari sebelum bawaan ditambahkan, tidak berbahaya.</p>
	 *
	 * @return batas hari peminjaman; tujuh bila belum ditetapkan, tidak pernah {@code null}
	 */
	public Integer getMaksimalHariPinjam() {
		return maksimalHariPinjam == null ? 7 : maksimalHariPinjam;
	}

	/**
	 * Menetapkan batas lama peminjaman arsip dalam hari.
	 *
	 * @param maksimalHariPinjam batas hari peminjaman
	 */
	public void setMaksimalHariPinjam(Integer maksimalHariPinjam) {
		this.maksimalHariPinjam = maksimalHariPinjam;
	}

	/**
	 * Batas jumlah perpanjangan peminjaman arsip, dengan bawaan dua kali.
	 *
	 * <p>Dibaca {@code PeminjamanSuratItemAction} dan {@code LibraryUtil} untuk membatasi
	 * berapa kali sebuah peminjaman arsip surat masuk dapat diperpanjang. Ejaan
	 * "Perpanjaangan" pada nama metode memang salah sejak awal dan HARUS dipertahankan: ia
	 * menentukan nama kolom yang dipakai Hibernate sekaligus nama properti yang dipakai
	 * pemanggil dan mekanisme CRUD generik.</p>
	 *
	 * @return batas jumlah perpanjangan; dua bila belum ditetapkan, tidak pernah {@code null}
	 */
	public Integer getMaksimalJumlahPerpanjaangan() {
		return maksimalJumlahPerpanjaangan == null ? 2 : maksimalJumlahPerpanjaangan;
	}

	/**
	 * Menetapkan batas jumlah perpanjangan peminjaman arsip.
	 *
	 * @param maksimalJumlahPerpanjaangan batas jumlah perpanjangan
	 */
	public void setMaksimalJumlahPerpanjaangan(Integer maksimalJumlahPerpanjaangan) {
		this.maksimalJumlahPerpanjaangan = maksimalJumlahPerpanjaangan;
	}

}
