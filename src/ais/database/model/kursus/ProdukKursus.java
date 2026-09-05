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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity utama modul kursus/pelatihan non-formal: satu baris merepresentasikan satu
 * <b>"produk" kursus</b> — kelas/paket kursus yang ditawarkan/dijual kepada peserta (mis. "Kelas
 * Bahasa Inggris Batch 5", "Paket IT Fundamental"). Ini adalah entity "penjualan/penawaran",
 * berbeda dari master komponen ({@link KomponenProdukKursus}, {@link KategoriProdukKursus},
 * {@link TingkatKelasProdukKursus}) yang hanya berisi daftar referensi/atribut, dan berbeda pula
 * dari {@link ais.database.model.kursus.KomponenDataProdukKursus} yang merinci isi
 * pembelajaran (video/buku/ujian/pertemuan) di dalam satu produk.
 *
 * <h3>Alur status penawaran</h3>
 * <p>{@link #getStatus()} menormalkan {@code null}/kosong menjadi {@link #DRAFT}, dan empat
 * konstanta {@link #DRAFT}, {@link #PENDING_REVIEW}, {@link #PUBLISHED}, {@link #REJECTED}
 * menyiratkan alur pengajuan-tinjau-terbit yang khas produk yang perlu disetujui sebelum tampil
 * ke calon peserta. Kelas ini sendiri <b>tidak menegakkan transisi status</b> — tidak ada method
 * yang memvalidasi bahwa perpindahan status mengikuti urutan tertentu (mis. tidak ada yang
 * mencegah lompat langsung dari {@code DRAFT} ke {@code PUBLISHED} atau kembali dari
 * {@code PUBLISHED} ke {@code DRAFT}); penegakan alur, jika ada, berada di lapisan
 * action/helper pemanggil {@link #setStatus(String)}.</p>
 *
 * <h3>Instruktur dimodelkan sebagai {@code PesertaKursus}</h3>
 * <p>{@link #getInstruktur()} bertipe {@link PesertaKursus} — kelas yang secara harfiah berarti
 * "peserta kursus" — bukan tipe pegawai/dosen terpisah. Ini berarti pengajar produk kursus
 * memakai kembali model data peserta (kemungkinan satu tabel {@code peserta_kursus} menampung
 * baik peserta didik maupun instruktur, dibedakan lewat atribut lain pada
 * {@code PesertaKursus} itu sendiri, berkas mana yang dikelola agent klaster peserta). Waspadai
 * konsekuensi ini saat menelusuri laporan/izin akses: query yang menyaring "peserta" berdasarkan
 * {@code PesertaKursus} tanpa membedakan peran dapat ikut menjaring baris instruktur, atau
 * sebaliknya.</p>
 *
 * <h3>Perhitungan harga lintas-domain</h3>
 * <p>{@link #getHargaKomponens()} memakai nilai cadangan {@link Pertangungjawaban#DEFAULT_FORMULA}
 * — konstanta milik domain akunting/pertanggungjawaban keuangan — sebagai formula bawaan
 * penghitungan harga produk dari komponen-komponennya. Ini adalah penggunaan lintas-domain yang
 * disengaja: mesin evaluasi formula yang sama yang dipakai untuk menghitung nilai
 * pertanggungjawaban keuangan dipakai kembali di sini untuk menyusun {@link #hargaTotal} dari
 * {@link #hargaKomponens}, alih-alih membangun mesin formula terpisah khusus kursus.</p>
 *
 * <h3>Pola arsitektur yang berulang di sini</h3>
 * <ul>
 * <li><b>Field audit bayangan</b> — {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah
 * keharusan teknis infrastruktur audit (lihat {@link GeneralValueObject}), bukan bug: setter
 * {@code oleh}/{@code olehId} mengabaikan nilai kosong secara sengaja agar jejak audit yang sudah
 * terisi tidak tertimpa oleh jalur simpan yang kebetulan tidak membawa identitas pengguna.</li>
 * <li><b>Getter destruktif pada relasi lazy</b> — {@link #getTingkatKelasProdukKursus()},
 * {@link #getKategoriProdukKursus()}, {@link #getSatuanKerja()}, dan {@link #getInstruktur()}
 * semuanya menulis balik hasil {@code check(...)} ke field sebelum mengembalikannya; ini pola
 * resolusi proxy lazy standar {@link GeneralValueObject#check(Object)}, bukan bug.</li>
 * <li><b>Bendera aktif satu arah secara praktik</b> — {@link #getAktif()} menormalkan {@code null}
 * menjadi {@code true}, dan tidak ada method di kelas ini yang secara otomatis mematikannya;
 * penonaktifan sepenuhnya bergantung pemanggil eksternal memanggil {@link #setAktif(Boolean)}
 * dengan {@code false} secara eksplisit.</li>
 * <li><b>Getter dengan efek waktu-nyata</b> — {@link #getMulai()} mengembalikan {@code new Date()}
 * (saat pemanggilan, bukan nilai tersimpan) setiap kali dipanggil pada baris yang kolom
 * {@code mulai}-nya masih kosong, sehingga dua pemanggilan berurutan pada object yang sama dapat
 * mengembalikan detik yang berbeda; lihat javadoc method untuk implikasinya.</li>
 * </ul>
 *
 * @see KomponenDataProdukKursus rincian komponen pembelajaran (video/buku/ujian/pertemuan) di dalam produk ini
 * @see KomponenProdukKursus master jenis komponen yang dapat menyusun {@link #hargaKomponens}
 * @see KategoriProdukKursus master kategori yang dirujuk {@link #getKategoriProdukKursus()}
 * @see TingkatKelasProdukKursus master tingkat kelas yang dirujuk {@link #getTingkatKelasProdukKursus()}
 * @see KuponKursus kupon diskon yang dapat menunjuk balik ke produk ini lewat {@code produkKursus}
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "produk_kursus")
public class ProdukKursus extends GeneralValueObject {

	/** Status awal/bawaan sebelum produk pernah diajukan untuk ditinjau. Nilai cadangan {@link #getStatus()}. */
	public final static String DRAFT = "Draft";
	/** Status saat produk sudah diajukan pembuatnya dan menunggu tinjauan/persetujuan sebelum terbit. */
	public final static String PENDING_REVIEW = "Pending Review";
	/** Status saat produk sudah disetujui/terbit dan (diasumsikan) tampil ke calon peserta. */
	public final static String PUBLISHED = "Published";
	/** Status saat produk ditolak pada tahap tinjauan. */
	public final static String REJECTED = "Rejected";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code produk_kursus}, dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah produk kursus ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}) agar jejak audit yang sudah terisi tidak terhapus oleh jalur
	 * simpan yang kebetulan tidak membawa identitas pengguna (mis. proses batch/penjadwal).
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
	 * Mengisi nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
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
	 * Mengembalikan nama pengguna yang terakhir mengubah produk kursus ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui lewat Hibernate.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object memakai
	 * {@code WaktuUtil.getDate()} (jam server aplikasi), sehingga object baru selalu punya nilai
	 * walau jalur simpan lupa mengisinya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Tanpa validasi; normalnya diisi otomatis oleh
	 * {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP} sehingga
	 * komponen jam ikut tersimpan.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas produk kursus untuk keperluan log/debug: {@code "id-nama"}.
	 * Perhatikan format ini <b>berbeda</b> dari format {@code "kode - nama"} standar
	 * {@link GeneralValueObject#toString()} yang di-override di sini secara sengaja.
	 *
	 * @return gabungan id dan nama produk kursus
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas produk kursus. */
	private String kode;

	/** Nama produk kursus (kolom wajib, maksimal 255 karakter). */
	private String nama;
	/** Keterangan singkat produk kursus (kolom {@code keterangan_singkat}, bertipe {@code text}). */
	private String keterangan;

	/** Label tahun ajaran/periode penawaran produk kursus ini (mis. "2026/2027"). Format bebas. */
	private String tahunAjaran;
	/** Tingkat/level kelas produk kursus (mis. pemula/menengah/lanjutan); relasi opsional. */
	private TingkatKelasProdukKursus tingkatKelasProdukKursus;
	/** Kategori produk kursus (mis. IT & Software, Bisnis, Bahasa); relasi opsional. */
	private KategoriProdukKursus kategoriProdukKursus;
	/**
	 * Formula teks penghitungan {@link #hargaTotal} dari komponen-komponen produk. Nilai cadangan
	 * {@link #getHargaKomponens()} adalah {@link Pertangungjawaban#DEFAULT_FORMULA} milik domain
	 * akunting — lihat javadoc kelas.
	 */
	private String hargaKomponens;

	/** Harga jual total produk kursus, hasil evaluasi {@link #hargaKomponens} atau diisi manual. */
	private Double hargaTotal;
	/** Status aktif/nonaktif tampil produk; {@code null} dianggap aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Tanggal mulai kelas/batch produk kursus ini. Lihat {@link #getMulai()} untuk perilaku nilai kosong. */
	private Date mulai;
	/** Tanggal berakhir kelas/batch produk kursus ini; boleh {@code null} bila belum ditentukan. */
	private Date sampai;
	/** Satuan kerja pemilik/penyelenggara produk kursus ini; relasi opsional. */
	private SatuanKerja satuanKerja;
	/** Deskripsi lengkap produk kursus (kolom bertipe {@code text}), berbeda dari {@link #keterangan}. */
	private String deskripsi;
	/**
	 * Pengajar/instruktur produk kursus ini. Bertipe {@link PesertaKursus} — lihat peringatan pada
	 * javadoc kelas soal pemakaian kembali model peserta untuk peran instruktur.
	 */
	private PesertaKursus instruktur;
	/** Status alur penawaran produk; lihat konstanta {@link #DRAFT}/{@link #PENDING_REVIEW}/{@link #PUBLISHED}/{@link #REJECTED}. */
	private String status;
	/** Penanda produk kursus gratis (tanpa biaya); {@code null} dianggap tidak gratis oleh {@link #getGratis()}. */
	private Boolean gratis;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public ProdukKursus() {
	}

	/**
	 * Mengembalikan primary key produk kursus.
	 *
	 * @return primary key, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya diisi otomatis oleh Hibernate.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas produk kursus, menormalkan {@code null} menjadi string kosong dan
	 * memangkas spasi tepi. <b>Getter murni-baca</b> (tidak menulis balik ke field, berbeda dari
	 * kelas sejenis {@code KomponenProdukKursus}/{@code KategoriProdukKursus} yang identik bentuknya
	 * tetapi field-nya sama-sama tidak ditulis balik di sini — normalisasi hanya pada nilai
	 * kembalian).
	 *
	 * @return kode produk kursus, tidak pernah {@code null}
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode ringkas produk kursus. Tidak ada penjaga tabrakan kode di lapisan model ini
	 * maupun indeks unik yang terlihat pada kolomnya (berbeda dari {@link KuponKursus#getKode()}
	 * yang kolomnya {@code unique = true}).
	 *
	 * @param kode kode produk kursus
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama produk kursus, dipangkas spasi tepi. Kolom wajib ({@code nullable = false})
	 * pada basis data, tetapi getter ini tetap dapat mengembalikan {@code null} untuk object baru
	 * yang belum pernah diisi.
	 *
	 * @return nama produk kursus (dipangkas), atau {@code null} bila belum pernah diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama produk kursus.
	 *
	 * @param nama nama produk kursus
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan singkat produk kursus (kolom {@code keterangan_singkat}). Getter
	 * murni-baca, tanpa normalisasi.
	 *
	 * @return keterangan singkat, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan_singkat", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan singkat produk kursus.
	 *
	 * @param keterangan keterangan singkat
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif/nonaktif tampil produk kursus, menormalkan {@code null} menjadi
	 * {@code true} (produk baru dianggap aktif secara bawaan). <b>Getter destruktif tersirat</b>:
	 * hasil normalisasi tidak ditulis balik ke field di sini (berbeda dari pola {@code check()} pada
	 * relasi), sehingga pemanggilan berulang pada object yang field-nya masih {@code null} akan
	 * selalu mengevaluasi ulang perbandingan yang sama — bukan bug, hanya bukan pola "tulis balik".
	 *
	 * @return {@code true} bila produk aktif/tampil, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyalakan atau mematikan tampil produk kursus.
	 *
	 * @param aktif {@code true} bila produk aktif/tampil
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan harga jual total produk kursus, menormalkan {@code null} menjadi {@code 0.0}.
	 *
	 * @return harga total, tidak pernah {@code null}
	 */
	public Double getHargaTotal() {
		return hargaTotal == null ? 0.0 : hargaTotal;
	}

	/**
	 * Mengisi harga jual total produk kursus. Tidak ada validasi non-negatif di sini.
	 *
	 * @param hargaTotal harga total baru
	 */
	public void setHargaTotal(Double hargaTotal) {
		this.hargaTotal = hargaTotal;
	}

	/**
	 * Mengembalikan formula teks penghitungan harga dari komponen-komponen produk, menormalkan
	 * {@code null}/kosong menjadi {@link Pertangungjawaban#DEFAULT_FORMULA} — nilai cadangan yang
	 * dipinjam dari domain akunting/pertanggungjawaban keuangan (lihat javadoc kelas). Ini
	 * <b>bukan</b> {@link #hargaTotal} itu sendiri, melainkan ekspresi/formula yang (di lapisan
	 * pemanggil, bukan di kelas ini) dievaluasi untuk menghasilkan {@link #hargaTotal} dari harga
	 * komponen-komponen produk (mis. {@link KomponenDataProdukKursus#getHarga()}).
	 *
	 * @return formula harga komponen, tidak pernah {@code null}/kosong
	 */
	@Column(name = "harga_komponens", columnDefinition = "text")
	public String getHargaKomponens() {
		return hargaKomponens == null || hargaKomponens.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : hargaKomponens;
	}

	/**
	 * Mengisi formula teks penghitungan harga dari komponen-komponen produk.
	 *
	 * @param hargaKomponens formula harga komponen baru
	 */
	public void setHargaKomponens(String hargaKomponens) {
		this.hargaKomponens = hargaKomponens;
	}

	/**
	 * Mengembalikan label tahun ajaran/periode penawaran produk kursus. Getter murni-baca, tanpa
	 * normalisasi maupun nilai cadangan; dapat mengembalikan {@code null}.
	 *
	 * @return tahun ajaran, atau {@code null} bila tidak diisi
	 */
	public String getTahunAjaran() {
		return tahunAjaran;
	}

	/**
	 * Mengisi label tahun ajaran/periode penawaran produk kursus. Tanpa validasi format.
	 *
	 * @param tahunAjaran label tahun ajaran baru
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan tingkat/level kelas produk kursus ini. <b>Getter destruktif</b>: hasil
	 * {@link GeneralValueObject#check(Object)} ditulis balik ke field sebelum dikembalikan —
	 * pola resolusi proxy lazy standar seluruh entity AIS, bukan bug.
	 *
	 * @return tingkat kelas produk kursus, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tingkat_kelas_produk_kursus", nullable = true)
	public TingkatKelasProdukKursus getTingkatKelasProdukKursus() {
		tingkatKelasProdukKursus = check(tingkatKelasProdukKursus);
		return tingkatKelasProdukKursus;
	}

	/**
	 * Menetapkan tingkat/level kelas produk kursus ini.
	 *
	 * @param tingkatKelasProdukKursus tingkat kelas baru, atau {@code null} untuk melepas relasi
	 */
	public void setTingkatKelasProdukKursus(TingkatKelasProdukKursus tingkatKelasProdukKursus) {
		this.tingkatKelasProdukKursus = tingkatKelasProdukKursus;
	}

	/**
	 * Mengembalikan tanggal mulai kelas/batch produk kursus ini, dipetakan sebagai {@code DATE}
	 * (tanpa komponen jam).
	 *
	 * <p><b>Perhatian — getter ini punya efek waktu-nyata, bukan hanya normalisasi:</b> bila kolom
	 * {@code mulai} masih kosong, method mengembalikan {@code new Date()} — waktu SAAT method ini
	 * dipanggil, bukan nilai tersimpan yang tetap. Konsekuensinya dua pemanggilan berurutan pada
	 * object yang sama (mis. render satu baris tabel lalu render ulang beberapa saat kemudian tanpa
	 * reload dari basis data) dapat mengembalikan tanggal yang berbeda bila keduanya jatuh pada hari
	 * kalender yang berbeda, dan nilai ini <b>tidak pernah ditulis balik ke field</b> {@code mulai}
	 * (berbeda dari getter destruktif relasi lazy di kelas ini) — sehingga baris yang tersimpan tetap
	 * kosong di basis data walau tampilannya selalu menunjukkan "hari ini". Bandingkan dengan
	 * {@link #getSampai()} yang murni membaca nilai tersimpan tanpa nilai cadangan apa pun.
	 *
	 * @return tanggal mulai tersimpan, atau tanggal saat ini bila kolom kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? new Date() : mulai;
	}

	/**
	 * Mengisi tanggal mulai kelas/batch produk kursus ini.
	 *
	 * @param mulai tanggal mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal berakhir kelas/batch produk kursus ini, dipetakan sebagai {@code DATE}.
	 * Getter murni-baca tanpa nilai cadangan; dapat mengembalikan {@code null} bila belum ditentukan
	 * — berbeda dari {@link #getMulai()} yang selalu mengembalikan nilai non-{@code null}.
	 *
	 * @return tanggal berakhir, atau {@code null} bila belum ditentukan/tanpa batas
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengisi tanggal berakhir kelas/batch produk kursus ini.
	 *
	 * @param sampai tanggal berakhir baru, atau {@code null} untuk tanpa batas
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan satuan kerja pemilik/penyelenggara produk kursus ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Ini satu-satunya sumbu tenant/kepemilikan pada entity ini; relasi
	 * opsional ({@code nullable = true}) — produk tanpa {@code satuanKerja} tidak tersaring oleh
	 * batasan unit kerja mana pun oleh kelas ini sendiri (penyaringan, bila ada, dilakukan lapisan
	 * pemanggil/query).
	 *
	 * @return satuan kerja penyelenggara, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik/penyelenggara produk kursus ini.
	 *
	 * @param satuanKerja satuan kerja baru, atau {@code null} untuk melepas relasi
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan kategori produk kursus ini. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return kategori produk kursus, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_produk_kursus", nullable = true)
	public KategoriProdukKursus getKategoriProdukKursus() {
		kategoriProdukKursus = check(kategoriProdukKursus);
		return kategoriProdukKursus;
	}

	/**
	 * Menetapkan kategori produk kursus ini.
	 *
	 * @param kategoriProdukKursus kategori baru, atau {@code null} untuk melepas relasi
	 */
	public void setKategoriProdukKursus(KategoriProdukKursus kategoriProdukKursus) {
		this.kategoriProdukKursus = kategoriProdukKursus;
	}

	/**
	 * Mengembalikan deskripsi lengkap produk kursus (kolom bertipe {@code text}). Berbeda dari
	 * {@link #getKeterangan()} yang bermaksud ringkas, field ini menampung uraian panjang (mis.
	 * silabus, target peserta, manfaat kursus) untuk halaman detail produk.
	 *
	 * @return deskripsi lengkap, atau {@code null} bila tidak diisi
	 */
	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi;
	}

	/**
	 * Mengisi deskripsi lengkap produk kursus.
	 *
	 * @param deskripsi deskripsi lengkap baru
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengembalikan pengajar/instruktur produk kursus ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Lihat peringatan pada javadoc kelas: tipe kembalian {@link PesertaKursus}
	 * berarti instruktur dimodelkan lewat kelas yang sama dipakai untuk peserta didik.
	 *
	 * @return instruktur produk kursus, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "instruktur", nullable = true)
	public PesertaKursus getInstruktur() {
		instruktur = check(instruktur);
		return instruktur;
	}

	/**
	 * Menetapkan pengajar/instruktur produk kursus ini.
	 *
	 * @param instruktur instruktur baru, atau {@code null} untuk melepas relasi
	 */
	public void setInstruktur(PesertaKursus instruktur) {
		this.instruktur = instruktur;
	}

	/**
	 * Mengembalikan status alur penawaran produk kursus, menormalkan {@code null}/kosong menjadi
	 * {@link #DRAFT}. Lihat javadoc kelas soal alur status: kelas ini sendiri tidak menegakkan
	 * urutan transisi antar-status.
	 *
	 * @return status produk kursus ({@link #DRAFT}/{@link #PENDING_REVIEW}/{@link #PUBLISHED}/{@link #REJECTED}
	 *         atau nilai bebas lain yang pernah disimpan), tidak pernah {@code null}/kosong
	 */
	@Column(name = "status", nullable = true, length = 50)
	public String getStatus() {
		return status == null || status.isEmpty() ? DRAFT : status;
	}

	/**
	 * Mengisi status alur penawaran produk kursus. Tanpa validasi bahwa nilai yang diisi termasuk
	 * salah satu dari konstanta {@link #DRAFT}/{@link #PENDING_REVIEW}/{@link #PUBLISHED}/{@link #REJECTED}.
	 *
	 * @param status status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan penanda produk kursus gratis (tanpa biaya), menormalkan {@code null} menjadi
	 * {@code false}. Perhatikan tidak ada penegakan otomatis di kelas ini bahwa {@link #hargaTotal}
	 * ikut menjadi nol saat bendera ini {@code true} — kedua field independen dan dapat berbeda
	 * (mis. produk berbendera gratis tetapi {@code hargaTotal} tetap tersimpan bukan nol) kecuali
	 * lapisan pemanggil menyelaraskannya secara eksplisit.
	 *
	 * @return {@code true} bila produk gratis, tidak pernah {@code null}
	 */
	public Boolean getGratis() {
		return gratis == null ? false : gratis;
	}

	/**
	 * Menyalakan atau mematikan penanda gratis produk kursus.
	 *
	 * @param gratis {@code true} bila produk gratis
	 */
	public void setGratis(Boolean gratis) {
		this.gratis = gratis;
	}

}
