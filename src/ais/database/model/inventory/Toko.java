package ais.database.model.inventory;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

import ais.database.model.Brand;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pendaftar;
import ais.database.model.sirs.Gudang;

/**
 * <h3>Toko/outlet -- entity TENANT paling sentral modul retail, kantin, dan koperasi.</h3>
 *
 * <p>Satu baris {@code koperasi.toko} mewakili satu gerai fisik: kios kantin sekolah, outlet
 * koperasi, bengkel, atau toko mandiri yang mendaftar lewat portal ebisnis.id. Hampir seluruh data
 * transaksional modul retail menggantung padanya -- {@link Produk} (katalog), {@link ProdukBatch}
 * (lot fisik per outlet), {@link Pedagang}, {@link SetoranTenant}, {@link SesiKasKasir},
 * {@link MutasiStokToko}, {@link Pembelian}, {@link StokOpname}, dan seterusnya. Selain menjadi
 * batas partisi data, kelas ini juga menampung PROFIL gerai (alamat, kontak, jam operasional),
 * KEBIJAKAN operasional (oversell, hak ubah harga, otomatisasi lewat tengah malam), dan
 * PEMETAAN AKUN akuntansi per outlet.</p>
 *
 * <p><b>Dimensi tenant -- {@code Toko} adalah AKAR, bukan simpul tengah.</b> Ini pertanyaan
 * terpenting tentang kelas ini, dan jawabannya berbeda dari dugaan yang wajar. {@code Toko} TIDAK
 * memiliki field {@code satuanKerja}, {@code yayasan}, {@code sekolah}, {@code program},
 * {@code fakultas}, {@code jurusan}, MAUPUN {@code koperasi} -- tidak satu pun. Penelusuran
 * menyeluruh atas paket {@code ais.database.model.inventory} juga tidak menemukan satu entity pun
 * di dalamnya yang menyimpan pengait organisasi semacam itu. Artinya rantai tenant seluruh modul
 * retail berhenti persis di sini: data transaksi menunjuk ke {@code Toko}, dan {@code Toko} tidak
 * menunjuk ke mana-mana.</p>
 *
 * <p>Pola ini BERBEDA TAJAM dari {@code ais.database.model.koperasi.Koperasi}, yang tenant-nya
 * berantai dua tingkat (entity anak &rarr; {@code Koperasi} &rarr; {@code SatuanKerja} dkk.). Di
 * sana filter berdasarkan {@code koperasi} saja tidak otomatis menjamin isolasi lintas satuan
 * kerja karena masih ada tingkat di atasnya yang bisa terlewat. Di sini persoalannya lain sama
 * sekali: TIDAK ADA tingkat di atas {@code Toko} untuk difilter, sehingga filter
 * {@code eq("toko", tokoAktif)} yang benar sudah cukup dan memang final. Sisi buruknya, tidak ada
 * pula pembatas otomatis dari struktur organisasi -- satu pemasangan AIS yang melayani banyak
 * satuan kerja menempatkan SEMUA tokonya dalam satu ruang datar tanpa sekat bawaan. Siapa boleh
 * melihat toko mana sepenuhnya ditentukan {@link #getBolehMelihatTokolain()} plus disiplin tiap
 * pemanggil, bukan oleh hierarki data.</p>
 *
 * <p><b>Generic CRUD v2.</b> Per audit revisi berjalan, {@code Toko.class} TIDAK terdaftar sebagai
 * {@code entityClass} pada adapter mana pun di bawah
 * {@code ais.action.master.generic.v2.adapter}; CRUD toko dilayani Action ZK klasik
 * ({@code TokoAction}) dan jalur API/helper. Perlu dicatat untuk rencana pendaftaran di masa
 * depan, dan konsekuensinya di sini LEBIH TAJAM daripada pada {@link Produk}. Whitelist
 * {@code scopeBindings()} pada {@code GenericCrudAutoEntityAdapter} hanya mengenal properti
 * {@code yayasan}, {@code sekolah}, {@code program}, {@code fakultas}, {@code jurusan},
 * {@code satuanKerja}, {@code mahasiswa}, {@code siswa}, {@code dosen}, {@code guru},
 * {@code orangTua}, dan {@code anggotaKoperasi}. {@code Toko} tidak memiliki SATU PUN di antaranya
 * -- sebagaimana dijelaskan di atas. Karena {@code addScope(...)} diam-diam melewati properti yang
 * tidak dimiliki entity target, peta lingkup yang dihasilkan akan KOSONG SEPENUHNYA, sehingga
 * {@code applyScope} tidak menambahkan satu pun {@code Restriction} dan
 * {@code validateObjectScope} tidak memeriksa apa pun. Mendaftarkan {@code Toko} ke Generic CRUD
 * v2 apa adanya berarti setiap role non-admin yang punya hak READ menunya dapat membaca -- dan
 * lewat {@code applyUpdateValues} juga menyunting -- seluruh baris toko lintas instansi. Ini
 * penguatan pola whitelist berbasis-refleksi yang sudah tercatat, bukan celah baru, dan disebutkan
 * di sini justru karena {@code Toko} adalah kandidat pendaftaran yang tampak wajar.</p>
 *
 * <p><b>Empat kanal, satu sumber kebenaran.</b> Baris toko disunting dari layar ZK admin, JSP,
 * Kasir Desktop, dan Android. Perlu diperhatikan {@code TokoAction} (form ZK admin) secara
 * historis hanya menyunting kode/nama/keterangan; mayoritas field profil dan kebijakan di bawah
 * ditambahkan lewat jalur {@code KantinHelper.tokoProfilAmbil/Simpan} dan sengaja belum muncul di
 * form ZK. Karena itu jangan menyimpulkan sebuah field tidak terpakai hanya karena tidak terlihat
 * di layar admin.</p>
 *
 * <p><b>Envers.</b> Kelas ber-{@code @Audited}. {@code hbm2ddl.auto=update} menambahkan kolom baru
 * ke tabel utama {@code koperasi.toko} secara otomatis tetapi TERBUKTI TIDAK menyinkronkannya ke
 * tabel audit {@code new_audit.toko__audit}. Setiap kolom baru karena itu WAJIB diikuti ALTER
 * manual lewat berkas migrasi SQL terpisah SEBELUM deploy -- bila dilewatkan, UPDATE apa pun pada
 * baris toko gagal saat menulis baris auditnya. Lihat {@link #getAlamat()} dan
 * {@link #getUnitUsahaJson()} yang masing-masing menyebut berkas migrasinya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "toko")
public class Toko extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}. Nilainya
	 * identik dengan {@code serialVersionUID} pada {@link Produk} dan beberapa entity lain hasil
	 * generate hbm2java batch yang sama -- artefak copy-paste template generator, BUKAN indikasi
	 * kekerabatan atau kompatibilitas biner antar-kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer; lihat {@link #getId()}. */
	private Long id;

	/** Jejak audit nama pembuat/pengubah; lihat {@link #getOleh()}. */
	private String oleh;

	/** Jejak audit id pengguna pembuat/pengubah; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * {@code Tbmuser.userId} pengguna yang terakhir membuat/mengubah baris toko ini -- jejak audit
	 * ringan yang berdiri SENDIRI di samping histori Envers ({@code new_audit.toko__audit}).
	 * Keduanya sengaja hidup berdampingan: Envers menyimpan revisi lengkap tetapi hanya terbaca
	 * lewat query API-nya, sedangkan kolom ini ikut terbawa pada SELECT biasa sehingga grid dan
	 * laporan dapat menampilkan "diubah oleh" tanpa join ke skema audit. Duplikasi ini KEHARUSAN
	 * TEKNIS, bukan redundansi yang perlu dibersihkan.
	 *
	 * @return userId pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()} dengan PENJAGA masukan kosong: argumen {@code null} atau yang
	 * hanya berisi spasi DIABAIKAN DIAM-DIAM -- nilai lama dipertahankan, tanpa exception dan tanpa
	 * log. Perilaku ini disengaja agar jalur simpan yang kebetulan tidak membawa identitas pengguna
	 * (sinkronisasi dari Kasir Desktop/Android, job terjadwal, pendaftaran mandiri ebisnis.id)
	 * tidak MENGHAPUS jejak audit yang sudah benar dengan menimpanya jadi kosong. Konsekuensi yang
	 * harus disadari: field ini TIDAK BISA dikosongkan lagi lewat setter ini setelah sekali terisi.
	 * Pola penjaga yang sama dipakai {@link #setOleh(String)} dan sama persis dengan
	 * {@link Produk#setOlehId(String)}.
	 *
	 * @param olehId userId pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Setter {@link #getOleh()} dengan penjaga masukan kosong yang sama persis dengan
	 * {@link #setOlehId(String)} -- {@code null}/spasi diabaikan diam-diam sehingga jejak audit lama
	 * tidak terhapus. Lihat javadoc setter tersebut untuk alasan dan konsekuensinya.
	 *
	 * @param oleh nama/identitas pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama/identitas tampilan pengguna yang terakhir membuat/mengubah baris ini -- pendamping
	 * {@link #getOlehId()} yang menyimpan id teknisnya. Disimpan sebagai teks beku (BUKAN relasi ke
	 * {@code Tbmuser}) supaya nama yang tercatat tetap seperti saat perubahan terjadi walau akun
	 * penggunanya kemudian diganti nama atau dihapus.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum setiap {@code UPDATE}
	 * baris toko ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah} yang menyetel
	 * {@link #getTanggal_dirubah()} ke waktu saat itu. Karena berjalan di tingkat penyedia
	 * persistensi, cap waktu ikut diperbarui APA PUN jalur yang mengubah baris -- form ZK admin,
	 * {@code KantinHelper.tokoProfilSimpan}, API Kasir Desktop/Android, atau pendaftaran mandiri --
	 * tanpa pemanggil perlu mengingatnya. Hook ini HANYA bereaksi pada UPDATE; nilai awal saat
	 * INSERT pertama berasal dari inisialisasi field {@link #tanggal_dirubah}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
	 * field ini (bukan pada konstruktor), sehingga toko yang baru dibuat sudah punya cap waktu
	 * masuk akal sebelum {@code INSERT} pertama; sesudahnya diperbarui otomatis oleh
	 * {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setter {@link #getTanggal_dirubah()}. Normalnya TIDAK dipanggil kode aplikasi -- nilainya
	 * dikelola otomatis oleh {@link #onUpdate()}, dan penyetelan manual akan tertimpa hook itu pada
	 * UPDATE berikutnya.
	 *
	 * @param tanggal_dirubah cap waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris toko ini (presisi {@code TIMESTAMP}). Dipakai untuk
	 * sinkronisasi klien Kasir Desktop/Android dalam menentukan baris mana yang perlu ditarik
	 * ulang. Nama field/properti sengaja dipertahankan bergaya {@code snake_case}
	 * ({@code tanggal_dirubah}, bukan {@code tanggalDirubah}) karena nama kolomnya diturunkan
	 * implisit dari nama properti -- mengubahnya menjadi camelCase akan mengubah nama kolom dan
	 * memutus pemetaan pada basis data yang sudah berjalan.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris toko dalam format {@code "<id>-<nama>"}, dengan tiap bagian yang
	 * {@code null} diganti string kosong sehingga hasilnya tidak pernah memuat literal
	 * {@code "null"} dan method ini tidak pernah melempar {@code NullPointerException}. Dipakai
	 * combobox pemilih toko, log, dan pesan kesalahan.
	 *
	 * <p>Kedua bagian dibaca LANGSUNG dari field, bukan lewat getter-nya, sehingga hasilnya bisa
	 * berbeda tipis dari {@link #getNama()} yang melakukan {@code trim()}. Pembacaan langsung
	 * disengaja: {@code toString()} kerap dipanggil pada objek proxy/detached saat logging atau
	 * debugging, dan melewati getter berisiko memicu resolusi lazy di tempat yang tidak
	 * seharusnya.</p>
	 *
	 * @return ringkasan identitas toko
	 */
	public String toString() {
		return (id == null ? "" : id) + "-" + (nama == null ? "" : nama);
	}

	/** Kode singkat toko; lihat {@link #getKode()}. */
	private String kode;

	/** Nama toko; lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Penanda aktif (soft delete); lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Gerbang visibilitas LINTAS TOKO -- kunci isolasi tenant; lihat {@link #getBolehMelihatTokolain()}. */
	private Boolean bolehMelihatTokolain;

	/** Alamat gerai; lihat {@link #getAlamat()}. */
	private String alamat;

	/** Kota gerai; lihat {@link #getKota()}. */
	private String kota;

	/** Kode pos gerai; lihat {@link #getKodePos()}. */
	private String kodePos;

	/** Telepon gerai; lihat {@link #getTelp()}. */
	private String telp;

	/** Surel gerai; lihat {@link #getEmail()}. */
	private String email;

	/** Nama penanggung jawab gerai; lihat {@link #getPicNama()}. */
	private String picNama;

	/** Nomor HP penanggung jawab; lihat {@link #getPicHp()}. */
	private String picHp;

	/** NPWP gerai; lihat {@link #getNpwp()}. */
	private String npwp;

	/** Jam operasional sebagai teks bebas; lihat {@link #getJamOperasional()}. */
	private String jamOperasional;

	/** Ucapan penutup struk dan layar pelanggan; lihat {@link #getPesanTerimaKasih()}. */
	private String pesanTerimaKasih;

	/** Daftar alasan penahanan transaksi dalam JSON; lihat {@link #getAlasanTahanJson()}. */
	private String alasanTahanJson;

	/** Gerbang oversell tingkat toko; lihat {@link #getBolehTransaksiStokHabis()}. */
	private Boolean bolehTransaksiStokHabis;

	/** Gerbang kebijakan ubah harga; lihat {@link #getSemuaBolehUbahHarga()}. */
	private Boolean semuaBolehUbahHarga;

	/** Daftar putih userId pengubah harga (CSV); lihat {@link #getUserBolehUbahHarga()}. */
	private String userBolehUbahHarga;

	/** Daftar putih roleId pengubah harga (CSV); lihat {@link #getRoleBolehUbahHarga()}. */
	private String roleBolehUbahHarga;

	/** Otomatisasi pelunasan lewat tengah malam (tri-state); lihat {@link #getOtomatisBayarSetelahJam24()}. */
	private Boolean otomatisBayarSetelahJam24;

	/** Otomatisasi penandaan terlayani lewat tengah malam (tri-state); lihat {@link #getOtomatisLayaniSetelahJam24()}. */
	private Boolean otomatisLayaniSetelahJam24;

	/** Penanda toko khusus demo/UAT. Default false agar data sample mustahil muncul di toko produksi. */
	private Boolean tokoDemo;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk menginstansiasi entity saat
	 * memuat baris dari basis data, dan dipakai jalur aplikasi untuk membuat toko baru sebelum
	 * field-nya diisi. Tidak menyetel nilai apa pun kecuali inisialisasi {@link #tanggal_dirubah}
	 * yang berjalan otomatis pada deklarasi field-nya. Seluruh getter berpenjaga pada kelas ini
	 * ({@link #getAktif()}, {@link #getBolehMelihatTokolain()}, {@link #getPesanTerimaKasih()},
	 * dst.) memastikan objek hasil konstruktor ini sudah aman dibaca meski semua kolomnya masih
	 * {@code null}.
	 */
	public Toko() {
	}

	/**
	 * Konstruktor pintasan yang hanya menyetel {@link #getId()} -- dipakai untuk membentuk
	 * REFERENSI ringan ke toko yang sudah ada tanpa memuat barisnya dari basis data (mis. sebagai
	 * nilai relasi pada entity lain atau parameter kriteria query). Objek hasil konstruktor ini
	 * BUKAN entity terkelola dan seluruh field lainnya {@code null}.
	 *
	 * <p>Perlu kehati-hatian khusus pada kelas ini: karena
	 * {@link #getBolehMelihatTokolain()} dan {@link #getAktif()} adalah getter berpenjaga yang
	 * mengembalikan nilai default alih-alih {@code null}, objek referensi ringan akan tampak
	 * memiliki kebijakan yang sah ({@code aktif = true}, {@code bolehMelihatTokolain = false})
	 * padahal kolomnya belum pernah dibaca dari basis data. JANGAN mengambil keputusan lingkup atau
	 * kebijakan dari objek yang dibangun lewat konstruktor ini -- muat barisnya lebih dulu. Jangan
	 * pula menyimpannya lewat {@code saveOrUpdate} karena dapat menimpa baris nyata dengan
	 * kolom-kolom kosong.</p>
	 *
	 * @param id kunci primer toko yang dirujuk
	 */
	public Toko(Long id) {
		this.id = id;
	}

	/**
	 * Kunci primer toko (strategi {@code IDENTITY} -- nilainya dibangkitkan basis data, bukan
	 * aplikasi). {@code null} selama objek belum pernah disimpan; setelah {@code INSERT} pertama
	 * Hibernate mengisinya kembali ke objek yang sama.
	 *
	 * <p>Nilai inilah yang tersimpan pada kolom {@code toko} di hampir seluruh entity modul retail,
	 * sehingga secara praktis menjadi PENGENAL TENANT yang dibandingkan pada setiap filter lingkup.
	 * {@code insertable = false} disengaja: kolom {@code id} tidak ikut disertakan pada
	 * {@code INSERT} sehingga basis data selalu yang menentukan nilainya, dan id yang disetel manual
	 * lewat {@link #setId(Long)} pada objek BARU akan diabaikan saat penyimpanan pertama.</p>
	 *
	 * @return kunci primer, atau {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter {@link #getId()} -- normalnya hanya dipanggil Hibernate saat memuat baris, atau oleh
	 * kode yang sengaja membangun referensi ringan seperti {@link #Toko(Long)}. Lihat catatan
	 * {@code insertable = false} pada javadoc getter.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama toko sebagaimana ditampilkan pada combobox pemilih toko, judul struk, dan laporan.
	 * Kolomnya {@code nullable = false} dengan panjang maksimum 255 karakter.
	 *
	 * <p>Berbeda dari {@link Produk#getNama()} yang destruktif, getter ini adalah pembaca MURNI: ia
	 * hanya melakukan {@code trim()} pada nilai kembalian tanpa menulis apa pun kembali ke field,
	 * sehingga membacanya tidak menandai entity kotor dan tidak menerbitkan revisi Envers. Nilai
	 * {@code null} dikembalikan apa adanya sebagai {@code null} (tidak dinormalkan menjadi string
	 * kosong), jadi pemanggil tetap harus memeriksanya meski kolomnya {@code NOT NULL} -- objek
	 * yang belum disimpan atau hasil {@link #Toko(Long)} bisa saja bernamakan {@code null}.</p>
	 *
	 * <p>Perhatikan nama TIDAK dijamin unik oleh basis data. Karena combobox pemilih toko
	 * menampilkan nama, dua toko bernama sama akan tampak identik bagi pengguna; pembedaan yang
	 * dapat diandalkan hanya lewat {@link #getId()}.</p>
	 *
	 * @return nama toko yang sudah di-{@code trim()}, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Setter {@link #getNama()} -- menyimpan nilai apa adanya tanpa {@code trim()} dan tanpa
	 * pemeriksaan duplikat. Normalisasi spasi baru terjadi saat dibaca lewat getter.
	 *
	 * @param nama nama toko
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tentang toko -- salah satu dari tiga field (bersama {@link #getKode()} dan
	 * {@link #getNama()}) yang secara historis disunting form ZK admin {@code TokoAction}. Pembaca
	 * murni tanpa normalisasi apa pun; nilai dikembalikan persis seperti tersimpan, termasuk
	 * {@code null}.
	 *
	 * @return keterangan toko, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Setter {@link #getKeterangan()} -- menyimpan nilai apa adanya, termasuk {@code null} untuk
	 * mengosongkan keterangan.
	 *
	 * @param keterangan keterangan bebas, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode singkat toko menurut penomoran internal instansi (mis. {@code "KTN-01"}) -- dipakai pada
	 * laporan ringkas, penomoran dokumen, dan pencarian cepat. Tidak ada anotasi {@code @Column}
	 * eksplisit, sehingga nama dan panjang kolomnya diturunkan implisit oleh Hibernate dari nama
	 * properti.
	 *
	 * <p>Sebagaimana {@link #getNama()}, kode TIDAK dijamin unik oleh basis data dan tidak ada
	 * mekanisme di entity ini yang mencegah dua toko berkode sama. Berbeda dari {@link Produk} yang
	 * memiliki kunci unik ternormalisasi untuk menangkal duplikat katalog, {@code Toko} tidak
	 * memiliki penangkal serupa -- jumlah barisnya memang kecil dan dikelola manual oleh admin.
	 * Getter ini pembaca murni: nilai dikembalikan apa adanya tanpa {@code trim()}, sehingga
	 * pembandingan kode sebaiknya menormalkan spasi sendiri.</p>
	 *
	 * @return kode toko, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Setter {@link #getKode()} -- menyimpan nilai apa adanya tanpa normalisasi maupun pemeriksaan
	 * bentrok dengan toko lain.
	 *
	 * @param kode kode singkat toko
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Penanda toko aktif -- mekanisme SOFT DELETE: toko yang "ditutup" dari layar admin hanya
	 * disetel {@code false} sehingga seluruh transaksi historis yang menunjuk kepadanya tetap utuh
	 * dan laporan lama tidak kehilangan nama gerainya.
	 *
	 * <p>Getter null-safe dengan default {@code true} (FAIL-OPEN): baris lama yang kolomnya
	 * {@code NULL} diperlakukan sebagai AKTIF, menjaga kompatibilitas mundur agar daftar toko tidak
	 * mendadak kosong. Konsekuensinya untuk penulisan query sama dengan {@link Produk#getAktif()}:
	 * filter aktif TIDAK boleh ditulis sebagai {@code eq("aktif", true)} polos karena di Postgres
	 * pembandingan dengan {@code NULL} tidak pernah menghasilkan benar, sehingga baris lama hilang
	 * diam-diam dari hasil. Pola yang benar adalah {@code OR IS NULL}.</p>
	 *
	 * <p>Perlu diperhatikan menonaktifkan toko TIDAK menonaktifkan {@link Produk} miliknya, tidak
	 * menutup {@link SesiKasKasir} yang sedang berjalan, dan tidak menghalangi transaksi lewat
	 * jalur API yang tidak memeriksa flag ini -- penonaktifan hanya menyembunyikannya dari layar
	 * yang memang memfilter.</p>
	 *
	 * @return {@code true} bila toko aktif; {@code true} pula untuk baris yang kolomnya {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Setter {@link #getAktif()}. Menyetel {@code false} adalah cara resmi "menutup" toko.
	 * Menyetel {@code null} secara efektif sama dengan {@code true} karena default fail-open pada
	 * getter.
	 *
	 * @param aktif status aktif toko
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Gerbang visibilitas LINTAS TOKO -- {@code true} berarti pengguna yang sedang bekerja pada
	 * toko ini boleh melihat (dan pada beberapa layar memilih) data milik toko LAIN.
	 *
	 * <p><b>Ini pengendali isolasi tenant paling penting di seluruh modul retail.</b> Sebagaimana
	 * dijelaskan pada javadoc kelas, {@code Toko} adalah akar tenant tanpa induk organisasi, jadi
	 * tidak ada hierarki data yang membatasi siapa melihat apa. Yang tersisa adalah flag ini plus
	 * disiplin tiap pemanggil. Default-nya {@code false} (FAIL-CLOSED) untuk baris yang kolomnya
	 * {@code NULL} -- pilihan yang benar: toko yang belum pernah dikonfigurasi terkurung pada
	 * datanya sendiri, bukan sebaliknya. Perhatikan arah default ini KEBALIKAN dari
	 * {@link #getAktif()} yang fail-open; keduanya sengaja berbeda karena taruhannya berbeda.</p>
	 *
	 * <p><b>Penegakannya tersebar dan tidak seragam.</b> Tidak ada satu titik pusat yang
	 * menerapkan flag ini; setiap layar dan layanan memeriksanya sendiri, dengan kualitas yang
	 * berbeda-beda. Sebagian jalur menegakkannya di SERVER dengan menambahkan pembatas pada
	 * kriteria query atau memvalidasi objek yang diminta -- pola ini yang benar. Sebagian layar ZK
	 * lama justru hanya memanggil {@code setDisabled(!bolehMelihatTokolain)} pada combobox pemilih
	 * toko, yakni pembatasan di tingkat TAMPILAN belaka, sehingga nilai toko yang dikirim ulang
	 * tidak dijamin berada dalam lingkup pengguna. Ini konsisten dengan pola filter tenant lemah
	 * yang sudah tercatat di domain lain dan bukan temuan baru; dicantumkan di sini agar
	 * pengembang berikutnya tidak keliru menyimpulkan bahwa membaca flag ini di satu tempat sudah
	 * cukup mengamankan seluruh jalur.</p>
	 *
	 * <p>Perlu dicatat pula flag ini bersifat SATU ARAH dan tidak simetris: ia memberi toko ini
	 * kemampuan MELIHAT toko lain, bukan memberi izin toko lain melihat toko ini. Sebuah toko
	 * tidak dapat menyembunyikan diri dari toko lain yang flag-nya menyala.</p>
	 *
	 * @return {@code true} bila toko ini boleh melihat data toko lain; {@code false} untuk kolom
	 *         {@code null}
	 */
	public Boolean getBolehMelihatTokolain() {
		return bolehMelihatTokolain == null ? false : bolehMelihatTokolain;
	}

	/**
	 * Setter {@link #getBolehMelihatTokolain()} -- menyalakan atau mematikan gerbang visibilitas
	 * lintas toko TANPA memeriksa hak akses pemanggil. Karena flag ini adalah pengendali isolasi
	 * tenant utama modul retail (lihat javadoc getter), setiap jalur yang memanggilnya wajib
	 * memastikan sendiri bahwa pengguna berwenang mengubah kebijakan tersebut; entity ini tidak
	 * menegakkan apa pun. Menyetel {@code null} secara efektif sama dengan {@code false}.
	 *
	 * @param bolehMelihatTokolain kebijakan visibilitas lintas toko
	 */
	public void setBolehMelihatTokolain(Boolean bolehMelihatTokolain) {
		this.bolehMelihatTokolain = bolehMelihatTokolain;
	}

	/**
	 * Profil toko lengkap (fitur "Konfigurasi" Kasir Desktop) -- sebelumnya {@code Toko} cuma
	 * punya kode/nama/keterangan (lihat form admin {@code TokoAction.java}, yang TETAP hanya
	 * menyunting 3 field itu -- field baru di bawah ini sengaja belum ditambahkan ke form ZK admin,
	 * hanya dipakai jalur baru {@code KantinHelper.tokoProfilAmbil/Simpan}). Kolom-kolom ini BARU
	 * di tabel utama {@code koperasi.toko} -- TIDAK perlu migrasi manual, {@code hbm2ddl.auto=update}
	 * (lihat hibernate.cfg.xml) otomatis menambahkannya saat server berikutnya start.
	 * <p><b>KECUALI tabel audit</b>: {@code hbm2ddl.auto=update} TERBUKTI TIDAK menyinkron kolom baru
	 * ke tabel audit Envers ({@code new_audit.toko__audit}, entitas ini {@code @Audited}) -- lihat
	 * catatan eksplisit di hibernate.cfg.xml. Itu SATU-SATUNYA bagian yang masih perlu ALTER manual,
	 * lihat berkas migrasi SQL terpisah (WAJIB dijalankan sebelum deploy, sebelum baris apa pun di
	 * tabel ini diubah -- kalau tidak, INSERT ke tabel audit gagal saat entitas diubah).
	 */
	@Column(name = "alamat", nullable = true)
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Setter {@link #getAlamat()}. Bagian dari kelompok field profil yang disunting lewat
	 * {@code KantinHelper.tokoProfilSimpan}, bukan lewat form ZK admin.
	 *
	 * @param alamat alamat gerai, boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Kota tempat gerai berada -- bagian profil toko yang dicetak pada kop struk dan dokumen.
	 * Teks bebas maksimum 100 karakter, sengaja BUKAN relasi ke master wilayah: profil kios kantin
	 * tidak memerlukan data wilayah terstruktur, dan menjadikannya relasi akan memaksa setiap
	 * pemasangan mengisi master wilayah lebih dulu. Pembaca murni tanpa normalisasi.
	 *
	 * @return kota gerai, atau {@code null} bila belum diisi
	 */
	@Column(name = "kota", nullable = true, length = 100)
	public String getKota() {
		return kota;
	}

	/**
	 * Setter {@link #getKota()} -- menyimpan nilai apa adanya tanpa validasi terhadap master
	 * wilayah mana pun.
	 *
	 * @param kota kota gerai, boleh {@code null}
	 */
	public void setKota(String kota) {
		this.kota = kota;
	}

	/**
	 * Kode pos gerai -- bagian profil yang dicetak pada kop struk dan dokumen. Disimpan sebagai
	 * TEKS (maksimum 10 karakter), bukan angka, karena kode pos bukan bilangan yang dihitung dan
	 * dapat mengandung nol di depan yang akan hilang bila disimpan numerik. Tidak ada validasi
	 * format.
	 *
	 * @return kode pos gerai, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode_pos", nullable = true, length = 10)
	public String getKodePos() {
		return kodePos;
	}

	/**
	 * Setter {@link #getKodePos()} -- menyimpan nilai apa adanya tanpa validasi format.
	 *
	 * @param kodePos kode pos gerai, boleh {@code null}
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Nomor telepon gerai (bukan nomor pribadi penanggung jawab -- itu {@link #getPicHp()}) --
	 * dicetak pada kop struk agar pembeli dapat menghubungi gerai. Disimpan sebagai teks maksimum
	 * 50 karakter sehingga dapat memuat awalan negara, tanda pemisah, atau beberapa nomor
	 * sekaligus; tidak ada normalisasi maupun validasi format.
	 *
	 * @return telepon gerai, atau {@code null} bila belum diisi
	 */
	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() {
		return telp;
	}

	/**
	 * Setter {@link #getTelp()} -- menyimpan nilai apa adanya tanpa normalisasi format nomor.
	 *
	 * @param telp telepon gerai, boleh {@code null}
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Alamat surel gerai -- bagian profil untuk korespondensi dan kop dokumen. Maksimum 255
	 * karakter, TANPA validasi format alamat surel dan tanpa jaminan keunikan; entity ini tidak
	 * pernah mengirim surel ke alamat ini sendiri, jadi nilainya murni informatif kecuali ada jalur
	 * lain yang memakainya.
	 *
	 * @return surel gerai, atau {@code null} bila belum diisi
	 */
	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() {
		return email;
	}

	/**
	 * Setter {@link #getEmail()} -- menyimpan nilai apa adanya tanpa validasi format.
	 *
	 * @param email surel gerai, boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/** Nama Penanggung Jawab/PIC toko -- kontak utama yang dihubungi admin pusat bila ada kendala operasional toko ini. */
	@Column(name = "pic_nama", nullable = true, length = 255)
	public String getPicNama() {
		return picNama;
	}

	/**
	 * Setter {@link #getPicNama()} -- menyimpan NAMA sebagai teks beku, sengaja bukan relasi ke
	 * {@code Tbmuser}/{@code Pegawai}. Penanggung jawab gerai sering kali bukan pengguna sistem
	 * (mis. pemilik kios yang tidak punya akun), dan membekukan namanya membuat kontak yang tercatat
	 * tetap seperti saat diisi walau akun terkait berubah. Konsekuensinya nama di sini TIDAK ikut
	 * berubah bila orangnya berganti nama di master pegawai, dan tidak ada validasi bahwa orang
	 * tersebut memang ada.
	 *
	 * @param picNama nama penanggung jawab gerai, boleh {@code null}
	 */
	public void setPicNama(String picNama) {
		this.picNama = picNama;
	}

	/**
	 * Nomor HP penanggung jawab/PIC gerai -- kontak pribadi yang dihubungi admin pusat bila ada
	 * kendala operasional, berpasangan dengan {@link #getPicNama()}. Berbeda peran dari
	 * {@link #getTelp()} yang merupakan nomor gerai untuk pembeli; nomor di sini bersifat internal
	 * dan tidak dicetak pada struk. Teks maksimum 50 karakter tanpa normalisasi format.
	 *
	 * @return nomor HP penanggung jawab, atau {@code null} bila belum diisi
	 */
	@Column(name = "pic_hp", nullable = true, length = 50)
	public String getPicHp() {
		return picHp;
	}

	/**
	 * Setter {@link #getPicHp()} -- menyimpan nilai apa adanya tanpa normalisasi format nomor.
	 *
	 * @param picHp nomor HP penanggung jawab, boleh {@code null}
	 */
	public void setPicHp(String picHp) {
		this.picHp = picHp;
	}

	/**
	 * NPWP gerai -- nomor pokok wajib pajak yang dicetak pada dokumen bernilai pajak (faktur,
	 * dokumen pengadaan) bila gerai berbadan usaha sendiri. Disimpan sebagai TEKS maksimum 50
	 * karakter, bukan angka, karena NPWP lazim ditulis berformat dengan titik dan strip serta dapat
	 * berawalan nol. Nullable dan tanpa validasi checksum maupun format -- banyak kios kantin tidak
	 * memiliki NPWP terpisah dari induknya.
	 *
	 * @return NPWP gerai, atau {@code null} bila tidak ada/belum diisi
	 */
	@Column(name = "npwp", nullable = true, length = 50)
	public String getNpwp() {
		return npwp;
	}

	/**
	 * Setter {@link #getNpwp()} -- menyimpan nilai apa adanya tanpa validasi format maupun
	 * checksum.
	 *
	 * @param npwp NPWP gerai, boleh {@code null}
	 */
	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/** Teks bebas, mis. {@code "08:00 - 21:00 (Senin-Sabtu)"} -- sengaja satu field teks, bukan struktur jam per-hari, supaya form tetap sederhana sesuai kebutuhan kios kantin. */
	@Column(name = "jam_operasional", nullable = true, length = 100)
	public String getJamOperasional() {
		return jamOperasional;
	}

	/**
	 * Setter {@link #getJamOperasional()} -- menerima teks bebas. Karena bentuknya tidak
	 * terstruktur, nilai ini TIDAK dapat dan TIDAK PERNAH dipakai untuk menegakkan apa pun: tidak
	 * ada mekanisme yang menolak transaksi di luar jam yang tertulis di sini. Nilainya murni
	 * informatif untuk ditampilkan kepada pembeli.
	 *
	 * @param jamOperasional teks jam operasional, boleh {@code null}
	 */
	public void setJamOperasional(String jamOperasional) {
		this.jamOperasional = jamOperasional;
	}

	/** Teks resmi default kalau toko belum pernah menyunting -- dipakai getter di bawah SATU-SATUNYA tempat literal ini muncul, supaya struk/layar customer/form Konfigurasi semua konsisten tanpa duplikasi string. */
	public static final String PESAN_TERIMA_KASIH_DEFAULT = "Terima Kasih Telah Berbelanja, Semoga Belanja Berkah Berpahala";

	/**
	 * Ucapan penutup yang dicetak di struk pembayaran DAN ditampilkan (versi lebih besar) di Layar
	 * Pelanggan setelah transaksi selesai -- SATU field dipakai utk kedua tempat itu ({@code struk.js}
	 * dan {@code customer.html}), sesuai permintaan "kata-kata ucapan terima kasih di struk dan layar
	 * kedua (customer)" (bukan dua field terpisah). Tiap toko boleh menyunting kata-katanya sendiri
	 * lewat layar "Konfigurasi" (gerbang sama dgn field profil toko lain, lihat
	 * {@code KantinHelper.tokoProfilSimpan}); kalau belum pernah disunting (kolom kosong/NULL),
	 * getter ini otomatis mengembalikan {@link #PESAN_TERIMA_KASIH_DEFAULT} yang formal.
	 */
	@Column(name = "pesan_terima_kasih", nullable = true, length = 500)
	public String getPesanTerimaKasih() {
		return (pesanTerimaKasih == null || pesanTerimaKasih.trim().isEmpty()) ? PESAN_TERIMA_KASIH_DEFAULT : pesanTerimaKasih;
	}

	/**
	 * Setter {@link #getPesanTerimaKasih()}. Perhatikan asimetri yang mudah membingungkan:
	 * menyetel {@code null} atau string kosong TIDAK menghasilkan struk tanpa ucapan penutup,
	 * melainkan mengembalikan toko ke teks baku {@link #PESAN_TERIMA_KASIH_DEFAULT} karena
	 * getter-nya menormalkan keduanya menjadi default. Dengan kata lain ucapan penutup tidak dapat
	 * dimatikan lewat setter ini -- toko yang ingin struk tanpa ucapan harus menyimpan teks berisi
	 * spasi atau karakter tak terlihat, dan itu memang tidak disediakan sebagai fitur.
	 *
	 * @param pesanTerimaKasih ucapan penutup; {@code null}/kosong berarti kembali ke teks baku
	 */
	public void setPesanTerimaKasih(String pesanTerimaKasih) {
		this.pesanTerimaKasih = pesanTerimaKasih;
	}

	/**
	 * Daftar alasan penahanan transaksi yang tersedia di Kasir toko ini, disimpan sebagai JSON pada
	 * satu kolom {@code text}. "Menahan" adalah menyisihkan transaksi yang sedang berjalan agar
	 * kasir dapat melayani pembeli berikutnya lebih dulu; daftar ini memberi kasir pilihan alasan
	 * baku (mis. pembeli mengambil barang lain, menunggu konfirmasi harga) alih-alih mengetik bebas,
	 * sehingga alasannya dapat direkap.
	 *
	 * <p>Disimpan sebagai JSON dalam satu kolom, bukan sebagai tabel anak, karena daftarnya pendek,
	 * hanya dibaca sebagai satu kesatuan saat Kasir dimuat, dan tidak pernah menjadi sasaran query
	 * atau agregasi -- pola yang sama dipakai {@link #getUnitUsahaJson()} pada kelas ini serta
	 * {@link Produk#getEkstraPilihan()} dan {@link Produk#getKemasan()}. Konsekuensinya isi kolom
	 * ini TIDAK memiliki integritas referensial maupun validasi bentuk dari basis data: JSON rusak
	 * baru ketahuan saat diurai di lapisan klien. Pembaca murni; {@code null}/kosong berarti toko
	 * belum menyiapkan alasan baku dan kasir memakai perilaku bawaannya.</p>
	 *
	 * @return JSON daftar alasan penahanan, atau {@code null} bila belum diatur
	 */
	@Column(name = "alasan_tahan_json", nullable = true, columnDefinition = "text")
	public String getAlasanTahanJson() {
		return alasanTahanJson;
	}

	/**
	 * Setter {@link #getAlasanTahanJson()} -- menerima string JSON MENTAH tanpa memvalidasi
	 * bentuknya; JSON rusak tersimpan apa adanya dan baru menimbulkan masalah saat diurai klien.
	 *
	 * @param alasanTahanJson JSON daftar alasan penahanan, boleh {@code null}
	 */
	public void setAlasanTahanJson(String alasanTahanJson) {
		this.alasanTahanJson = alasanTahanJson;
	}

	/**
	 * Kebijakan stok per toko. Default {@code false} mengikuti izin pada tiap produk; bila
	 * {@code true}, seluruh produk toko ini boleh dijual saat stok nol atau minus.
	 */
	@Column(name = "boleh_transaksi_stok_habis", nullable = true)
	public Boolean getBolehTransaksiStokHabis() {
		return bolehTransaksiStokHabis == null ? Boolean.FALSE : bolehTransaksiStokHabis;
	}

	/**
	 * Setter {@link #getBolehTransaksiStokHabis()} -- gerbang OVERSELL tingkat toko.
	 *
	 * <p>Kebijakan efektif sebuah produk adalah gabungan flag ini dengan override per-produk
	 * {@link Produk#getIzinkanJualMinusStok()}, dan override produk MENANG: {@code false} di sana
	 * memblokir penjualan minus walau gerbang toko menyala, {@code true} mengizinkannya walau
	 * gerbang toko mati, dan {@code null} berarti mengikuti nilai di sini. Menyalakan gerbang ini
	 * karena itu tidak menjamin seluruh produk toko boleh dijual minus. Menyetel {@code null} pada
	 * kelas ini secara efektif sama dengan {@code false} karena getter-nya fail-closed.</p>
	 *
	 * <p>Perlu disadari penjualan minus menghasilkan {@link Produk#getStok()} bernilai NEGATIF yang
	 * tetap tersimpan apa adanya; tidak ada proses yang mengoreksinya sendiri, dan angka negatif itu
	 * ikut terbawa ke perbandingan ambang reorder serta laporan nilai persediaan.</p>
	 *
	 * @param bolehTransaksiStokHabis kebijakan oversell tingkat toko
	 */
	public void setBolehTransaksiStokHabis(Boolean bolehTransaksiStokHabis) {
		this.bolehTransaksiStokHabis = bolehTransaksiStokHabis;
	}

	/**
	 * Kebijakan UBAH HARGA per toko (permintaan 2026-08-20). Default {@code true} =
	 * perilaku lama: semua pengguna boleh mengubah harga. Bila diset {@code false},
	 * hanya akun yang terdaftar pada {@link #getUserBolehUbahHarga()} yang boleh
	 * mengubah harga jual/harga beli -- berlaku di master Produk, Kulakan/Bulk Entry
	 * Faktur, dan Grup Produk. Penegakannya di SERVER supaya keempat kanal
	 * (Desktop, Android, JSP, ZK) tunduk pada aturan yang sama.
	 */
	@Column(name = "semua_boleh_ubah_harga", nullable = true)
	public Boolean getSemuaBolehUbahHarga() {
		return semuaBolehUbahHarga == null ? Boolean.TRUE : semuaBolehUbahHarga;
	}

	/**
	 * Setter {@link #getSemuaBolehUbahHarga()} -- saklar utama kebijakan ubah harga toko.
	 *
	 * <p>Perhatikan default getter-nya {@code true} (FAIL-OPEN, demi kompatibilitas mundur agar
	 * pemasangan lama tidak mendadak terkunci), sehingga menyetel {@code null} berarti SEMUA
	 * pengguna boleh mengubah harga -- bukan berarti "belum diatur lalu dikunci". Karena itu
	 * mengosongkan field ini adalah cara MELONGGARKAN kebijakan, bukan mengetatkannya. Pengetatan
	 * hanya terjadi bila nilainya {@code false} secara eksplisit, dan barulah kemudian
	 * {@link #getUserBolehUbahHarga()} serta {@link #getRoleBolehUbahHarga()} dievaluasi secara
	 * OR.</p>
	 *
	 * @param semuaBolehUbahHarga {@code false} untuk membatasi ke daftar putih; {@code null}/
	 *        {@code true} berarti semua boleh
	 */
	public void setSemuaBolehUbahHarga(Boolean semuaBolehUbahHarga) {
		this.semuaBolehUbahHarga = semuaBolehUbahHarga;
	}

	/**
	 * Daftar {@code Tbmuser.userId} yang boleh mengubah harga ketika
	 * {@link #getSemuaBolehUbahHarga()} bernilai {@code false}. Disimpan sebagai CSV
	 * berpembatas koma dgn koma pembungkus (mis. {@code ",admin,kasir1,"}) supaya
	 * pencarian keanggotaan cukup memakai LIKE tanpa memecah string.
	 */
	@Column(name = "user_boleh_ubah_harga", columnDefinition = "text", nullable = true)
	public String getUserBolehUbahHarga() {
		return userBolehUbahHarga;
	}

	/**
	 * Setter {@link #getUserBolehUbahHarga()} -- menyimpan CSV daftar putih APA ADANYA, tanpa
	 * menambahkan koma pembungkus, tanpa membuang spasi, dan tanpa memvalidasi bahwa userId di
	 * dalamnya benar-benar ada.
	 *
	 * <p><b>Koma pembungkus adalah tanggung jawab pemanggil dan menentukan kebenaran hasil.</b>
	 * Format yang diharapkan adalah {@code ",admin,kasir1,"} justru agar pemeriksaan keanggotaan
	 * cukup memakai pencarian substring {@code ",<userId>,"} tanpa memecah string. Bila pemanggil
	 * menyimpan {@code "admin,kasir1"} tanpa koma di ujung, anggota pertama dan terakhir tidak akan
	 * cocok. Sebaliknya, karena pencocokan berbasis substring, userId yang merupakan bagian dari
	 * userId lain berpotensi cocok keliru bila koma pembungkus tidak konsisten. Entity ini tidak
	 * menegakkan format tersebut sama sekali.</p>
	 *
	 * @param userBolehUbahHarga CSV userId berkoma pembungkus, boleh {@code null}
	 */
	public void setUserBolehUbahHarga(String userBolehUbahHarga) {
		this.userBolehUbahHarga = userBolehUbahHarga;
	}

	/**
	 * Pesanan yang belum lunas dan sudah LEWAT hari (melewati jam 24) ditandai
	 * terbayar secara otomatis.
	 *
	 * <p>TIGA keadaan, bukan dua:</p>
	 * <ul>
	 *   <li>{@code null} -- ikut pengaturan global
	 *       ({@code otomatis_verifikasi_bayar_setelah_jam_24});</li>
	 *   <li>{@code TRUE} -- menyala untuk toko ini walau global mati;</li>
	 *   <li>{@code FALSE} -- mati untuk toko ini walau global menyala.</li>
	 * </ul>
	 *
	 * <p>Tri-state disengaja: dgn boolean biasa, "belum pernah diatur" tidak
	 * dapat dibedakan dari "sengaja dimatikan", sehingga toko yang belum
	 * disentuh akan ikut menyala begitu global dinyalakan -- justru kebalikan
	 * dari maksud pengaturan per toko.</p>
	 */
	@Column(name = "otomatis_bayar_setelah_jam_24")
	public Boolean getOtomatisBayarSetelahJam24() {
		return otomatisBayarSetelahJam24;
	}

	/**
	 * Setter {@link #getOtomatisBayarSetelahJam24()}.
	 *
	 * <p><b>Jangan pernah menormalkan {@code null} menjadi {@code false} pada jalur simpan.</b>
	 * Field ini TRI-STATE dan {@code null} adalah nilai bermakna ("ikut pengaturan global"), bukan
	 * "belum diisi". Menormalkannya akan mengunci toko pada keadaan mati secara permanen terhadap
	 * pengaturan global -- persis kebalikan dari maksud tri-state, sebagaimana dijelaskan pada
	 * javadoc getter. Form yang menyunting field ini karena itu memerlukan kendali tiga pilihan
	 * (ikut global / nyala / mati), bukan kotak centang dua keadaan.</p>
	 *
	 * <p>Perlu disadari otomatisasi ini menandai pesanan sebagai TERBAYAR tanpa ada uang yang
	 * benar-benar diterima -- ia menutup piutang berdasarkan berlalunya waktu, bukan berdasarkan
	 * penerimaan kas. Menyalakannya adalah keputusan kebijakan akuntansi, bukan sekadar kenyamanan
	 * operasional.</p>
	 *
	 * @param otomatisBayarSetelahJam24 {@code null} ikut global, {@code TRUE} nyala, {@code FALSE} mati
	 */
	public void setOtomatisBayarSetelahJam24(Boolean otomatisBayarSetelahJam24) {
		this.otomatisBayarSetelahJam24 = otomatisBayarSetelahJam24;
	}

	/**
	 * Transaksi yang belum dilayani dan sudah lewat hari ditandai terlayani
	 * secara otomatis. Tri-state, sama seperti
	 * {@link #getOtomatisBayarSetelahJam24()}.
	 */
	@Column(name = "otomatis_layani_setelah_jam_24")
	public Boolean getOtomatisLayaniSetelahJam24() {
		return otomatisLayaniSetelahJam24;
	}

	/**
	 * Setter {@link #getOtomatisLayaniSetelahJam24()} -- TRI-STATE, sama seperti
	 * {@link #setOtomatisBayarSetelahJam24(Boolean)}: {@code null} berarti "ikut pengaturan global"
	 * dan tidak boleh dinormalkan menjadi {@code false}. Lihat javadoc setter tersebut untuk alasan
	 * dan konsekuensinya. Berbeda dari otomatisasi pembayaran, penandaan terlayani tidak berdampak
	 * akuntansi -- ia hanya menutup antrean pelayanan yang tertinggal terbuka.
	 *
	 * @param otomatisLayaniSetelahJam24 {@code null} ikut global, {@code TRUE} nyala, {@code FALSE} mati
	 */
	public void setOtomatisLayaniSetelahJam24(Boolean otomatisLayaniSetelahJam24) {
		this.otomatisLayaniSetelahJam24 = otomatisLayaniSetelahJam24;
	}

	/**
	 * Daftar {@code Tbmrole.roleId} (hak akses / grup pengguna) yang boleh mengubah harga
	 * ketika {@link #getSemuaBolehUbahHarga()} bernilai {@code false}. Bersifat OR terhadap
	 * {@link #getUserBolehUbahHarga()}: pengguna boleh mengubah harga bila userId-nya
	 * terdaftar ATAU role-nya terdaftar. Format CSV berpembatas koma dgn koma pembungkus,
	 * sama seperti daftar pengguna.
	 */
	@Column(name = "role_boleh_ubah_harga", columnDefinition = "text", nullable = true)
	public String getRoleBolehUbahHarga() {
		return roleBolehUbahHarga;
	}

	/**
	 * Setter {@link #getRoleBolehUbahHarga()} -- menyimpan CSV daftar putih role APA ADANYA, dengan
	 * kewajiban koma pembungkus dan risiko pencocokan substring yang sama persis seperti
	 * {@link #setUserBolehUbahHarga(String)}; lihat javadoc setter tersebut.
	 *
	 * <p>Perlu diingat hubungannya dengan daftar pengguna adalah OR, bukan AND: memberi izin lewat
	 * role secara otomatis memberi izin kepada SETIAP pengguna yang memegang role itu, tanpa perlu
	 * namanya tercantum di daftar pengguna. Daftar ini karena itu jauh lebih luas dampaknya per
	 * entri dibanding daftar userId, dan tidak ada mekanisme pengecualian per pengguna terhadap
	 * role yang sudah diberi izin.</p>
	 *
	 * @param roleBolehUbahHarga CSV roleId berkoma pembungkus, boleh {@code null}
	 */
	public void setRoleBolehUbahHarga(String roleBolehUbahHarga) {
		this.roleBolehUbahHarga = roleBolehUbahHarga;
	}

	/**
	 * Penanda toko khusus demo/UAT -- dipakai untuk memisahkan gerai percobaan berisi data contoh
	 * dari gerai produksi, terutama oleh generator data sample dan layar yang sengaja menyembunyikan
	 * toko demo.
	 *
	 * <p>Getter null-safe dengan default {@code Boolean.FALSE} (FAIL-CLOSED) -- arah default yang
	 * benar untuk penanda semacam ini: baris lama dan toko yang belum pernah dikonfigurasi dianggap
	 * PRODUKSI, sehingga data contoh mustahil menyelinap ke gerai sungguhan hanya karena kolomnya
	 * belum terisi. Kesalahan yang mungkin terjadi hanyalah toko demo yang lupa ditandai lalu
	 * diperlakukan sebagai produksi, dan itu jauh lebih aman daripada kebalikannya.</p>
	 *
	 * <p>Penanda ini murni deklaratif: ia TIDAK menghalangi transaksi nyata, tidak mengisolasi data,
	 * dan tidak mencegah toko demo muncul pada laporan yang tidak sengaja memfilternya. Hanya
	 * pemanggil yang memeriksanya yang terpengaruh.</p>
	 *
	 * @return {@code true} bila toko ditandai demo/UAT; {@code false} untuk kolom {@code null}
	 */
	@Column(name = "toko_demo", nullable = true)
	public Boolean getTokoDemo() {
		return tokoDemo == null ? Boolean.FALSE : tokoDemo;
	}

	/**
	 * Setter {@link #getTokoDemo()}. Menyetel {@code null} secara efektif sama dengan
	 * {@code false} (toko dianggap produksi) karena default fail-closed pada getter.
	 *
	 * @param tokoDemo {@code true} menandai toko sebagai demo/UAT
	 */
	public void setTokoDemo(Boolean tokoDemo) {
		this.tokoDemo = tokoDemo;
	}

	/** Unit usaha toko dalam JSON; lihat {@link #getUnitUsahaJson()}. */
	private String unitUsahaJson;

	/**
	 * Unit usaha toko ini -- JSON array kode dari {@code ais.common.UnitUsahaKatalog}
	 * (mis. {@code ["BENGKEL_MOTOR","SPAREPART_MOTOR","CUCI_MOTOR"]}); satu toko boleh
	 * memiliki LEBIH DARI SATU unit usaha. {@code null}/kosong = belum dipilih -- generator
	 * data contoh produk akan menanyakan unit usaha lewat popup checkbox pada kasus itu.
	 * Diedit dari keempat kanal CRUD Toko (ZK/JSP/Desktop/Android).
	 * <p><b>Kolom BARU pada entitas ber-{@code @Audited}</b>: WAJIB jalankan
	 * {@code webapp/sql/migrasi_toko_unit_usaha_audit.sql} SEBELUM deploy (gotcha Envers
	 * yang sama dengan kolom profil di atas, lihat javadoc kelas).</p>
	 */
	@Column(name = "unit_usaha_json", nullable = true, columnDefinition = "text")
	public String getUnitUsahaJson() {
		return unitUsahaJson;
	}

	/**
	 * Setter {@link #getUnitUsahaJson()} -- menerima JSON MENTAH tanpa memvalidasi bentuknya dan
	 * tanpa memastikan kode di dalamnya dikenal {@code ais.common.UnitUsahaKatalog}. Kode tak
	 * dikenal tersimpan apa adanya dan akan diabaikan diam-diam oleh pembacanya. Karena disimpan
	 * sebagai teks, tidak ada integritas referensial: menghapus atau mengganti kode pada katalog
	 * TIDAK memperbarui baris toko mana pun yang terlanjur menyimpannya.
	 *
	 * @param unitUsahaJson JSON array kode unit usaha, boleh {@code null}/kosong
	 */
	public void setUnitUsahaJson(String unitUsahaJson) {
		this.unitUsahaJson = unitUsahaJson;
	}

	/** Gudang cabang pemasok toko ini (rujukan, tidak memblokir); lihat {@link #getGudangPemasok()}. */
	private Gudang gudangPemasok;

	/**
	 * Gudang cabang yang WAJIB memasok toko ini -- SEBELUMNYA tidak ada relasi sama sekali antara
	 * {@code Toko} dan {@code Gudang} (setiap gudang cabang bisa mengirim ke toko mana pun tanpa
	 * penugasan resmi, lihat gap analisis 2026-07-26). Dipakai oleh {@code StokThresholdScheduler}
	 * untuk menentukan gudang mana yang "bertanggung jawab" memasok toko ini (referensi, TIDAK
	 * memblokir {@code PengirimanGudangUtil.kirim} dari gudang lain -- itu tetap bebas seperti
	 * sebelumnya, field ini murni penanda/rujukan default) -- {@code null} berarti belum ditentukan.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang_pemasok", nullable = true)
	public Gudang getGudangPemasok() {
		gudangPemasok = check(gudangPemasok);
		return gudangPemasok;
	}

	/**
	 * Setter {@link #getGudangPemasok()} -- menetapkan gudang cabang penanggung jawab pasokan
	 * sebagai RUJUKAN saja. Menyetelnya TIDAK memblokir pengiriman dari gudang lain, yang tetap
	 * bebas seperti sebelum field ini ada; satu-satunya pembaca yang bertindak atasnya adalah
	 * {@code StokThresholdScheduler} saat menentukan ke gudang mana pengajuan otomatis ditujukan.
	 * Menyetel {@code null} berarti belum ditentukan.
	 *
	 * <p>Perhatikan relasi ini menyeberangi paket ke {@code ais.database.model.sirs.Gudang} dan
	 * getter-nya tidak memanggil {@link ais.database.model.GeneralValueObject#check(Object)},
	 * sehingga rawan proxy lazy pada objek yang sudah <i>detached</i> -- sama seperti seluruh relasi
	 * lain pada kelas ini.</p>
	 *
	 * @param gudangPemasok gudang cabang pemasok, boleh {@code null}
	 */
	public void setGudangPemasok(Gudang gudangPemasok) {
		this.gudangPemasok = gudangPemasok;
	}

	/** Pemilik toko pada portal ebisnis.id; lihat {@link #getPendaftar()}. */
	private Pendaftar pendaftar;

	/** Brand/sub-merek opsional; lihat {@link #getBrand()}. */
	private Brand brand;

	/**
	 * Pemilik toko ini pada portal ebisnis.id -- {@code null} utk semua baris {@code Toko} LAMA
	 * (Kantin/Koperasi/dsb, tidak berasal dari pendaftaran mandiri ebisnis.id). Dibiarkan nullable
	 * SENGAJA supaya penambahan kolom ini tidak mengubah perilaku toko-toko yang sudah ada sama
	 * sekali -- lihat {@code PendaftarDashboardHelper.tokoTambah}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar", nullable = true)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * Setter {@link #getPendaftar()} -- menautkan toko ke pemiliknya pada portal ebisnis.id.
	 *
	 * <p>Field ini adalah SATU-SATUNYA pengait kepemilikan pada kelas ini, dan perlu dipahami
	 * batasnya: ia menyatakan siapa yang mendaftarkan toko, BUKAN sumbu tenant organisasi. Toko
	 * warisan (kantin/koperasi) seluruhnya bernilai {@code null} di sini, sehingga field ini tidak
	 * dapat dipakai sebagai pembatas lingkup umum -- filter berdasarkan pendaftar akan menyisihkan
	 * semua toko lama sekaligus. Lihat javadoc kelas mengenai ketiadaan sumbu tenant organisasi
	 * pada {@code Toko}. Nullability-nya disengaja agar penambahan kolom ini tidak mengubah
	 * perilaku toko-toko yang sudah ada sama sekali.</p>
	 *
	 * @param pendaftar pemilik pada portal ebisnis.id, boleh {@code null} untuk toko warisan
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/** Brand/sub-merek opsional yang menaungi toko ini (boleh {@code null} -- toko tanpa brand). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "brand", nullable = true)
	public Brand getBrand() {
		brand = check(brand);
		return brand;
	}

	/**
	 * Setter {@link #getBrand()} -- menautkan toko ke brand/sub-merek yang menaunginya. Murni
	 * pengelompokan untuk pelaporan dan tampilan; TIDAK berperan sebagai batas tenant dan tidak
	 * membatasi apa pun. Beberapa toko boleh berbagi satu brand, dan toko tanpa brand ({@code null})
	 * sepenuhnya sah.
	 *
	 * @param brand brand penaung, boleh {@code null}
	 */
	public void setBrand(Brand brand) {
		this.brand = brand;
	}


	/**
	 * Akun Kas/Bank outlet.
	 * <p>Dipakai sebagai lawan jurnal pembayaran & penerimaan tunai toko bila metode pembayarannya belum punya akun sendiri. Ditempelkan pada master ini (bukan konfigurasi global) supaya
	 * tiap outlet/jenis bisa berbeda; konfigurasi global tetap dipakai sebagai cadangan terakhir
	 * agar pemasangan lama tidak berubah perilakunya. Kolomnya dibuat otomatis oleh Hibernate.</p>
	 */
	private ais.database.model.akunting.Akun akunKas;

	/**
	 * Akun Kas/Bank outlet -- lawan jurnal pembayaran dan penerimaan tunai toko ini.
	 *
	 * <p>Bersama {@link #getAkunPiutang()}, {@link #getAkunModalAwal()}, dan
	 * {@link #getAkunLabaDitahan()}, field ini membentuk PEMETAAN AKUN PER OUTLET yang membuat satu
	 * pemasangan AIS dapat membukukan beberapa gerai ke akun berbeda tanpa memisahkan bagan
	 * akunnya. Keempatnya mengikuti resolusi BERJENJANG yang sama: nilai pada master toko ini
	 * dipakai lebih dulu, dan bila {@code null} pemostingan jatuh ke konfigurasi global. Karena
	 * itulah seluruhnya {@code nullable} -- pemasangan lama yang belum pernah memetakan akun
	 * per-outlet terus berjalan persis seperti sebelumnya tanpa migrasi data.</p>
	 *
	 * <p>Konsekuensi yang perlu disadari: karena {@code null} bermakna "pakai konfigurasi global"
	 * dan bukan kesalahan, salah memetakan akun di sini TIDAK menimbulkan kegagalan yang terlihat --
	 * jurnal tetap terbentuk, hanya membebani akun yang keliru, dan selisihnya baru tampak saat
	 * rekonsiliasi. Relasi menyeberang paket ke {@code ais.database.model.akunting.Akun} dan
	 * getter-nya tidak memanggil {@link ais.database.model.GeneralValueObject#check(Object)},
	 * sehingga rawan proxy lazy pada objek detached. Kolomnya dibuat otomatis oleh Hibernate pada
	 * tabel utama -- ingat kewajiban ALTER manual tabel audit Envers yang dijelaskan pada javadoc
	 * kelas.</p>
	 *
	 * @return akun kas/bank outlet, atau {@code null} untuk memakai konfigurasi global
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_kas", nullable = true)
	public ais.database.model.akunting.Akun getAkunKas() {
		akunKas = check(akunKas);
		return akunKas;
	}

	/**
	 * Setter {@link #getAkunKas()} -- TIDAK memvalidasi bahwa akun yang disetel bertipe kas/bank,
	 * maupun bahwa ia berada pada bagan akun yang relevan. Akun bertipe apa pun akan diterima dan
	 * dibebani saat pemostingan. Menyetel {@code null} mengembalikan outlet ke konfigurasi global.
	 * Perubahan berlaku MAJU saja: jurnal yang sudah terbentuk tidak dipetakan ulang.
	 *
	 * @param akunKas akun kas/bank outlet, boleh {@code null}
	 */
	public void setAkunKas(ais.database.model.akunting.Akun akunKas) {
		this.akunKas = akunKas;
	}

	/**
	 * Akun Piutang Usaha outlet.
	 * <p>Dikredit saat penerimaan piutang pelanggan dijurnal. Ditempelkan pada master ini (bukan konfigurasi global) supaya
	 * tiap outlet/jenis bisa berbeda; konfigurasi global tetap dipakai sebagai cadangan terakhir
	 * agar pemasangan lama tidak berubah perilakunya. Kolomnya dibuat otomatis oleh Hibernate.</p>
	 */
	private ais.database.model.akunting.Akun akunPiutang;

	/**
	 * Akun Piutang Usaha outlet -- dikredit saat penerimaan piutang pelanggan dijurnal. Mengikuti
	 * pola pemetaan akun per-outlet dengan cadangan konfigurasi global; lihat {@link #getAkunKas()}
	 * untuk penjelasan lengkap mekanisme, konsekuensi, dan catatan proxy lazy yang berlaku sama
	 * bagi keempat akun.
	 *
	 * <p>Akun inilah yang terpengaruh bila {@link #getOtomatisBayarSetelahJam24()} menyala:
	 * otomatisasi tersebut menutup piutang berdasarkan berlalunya waktu, bukan penerimaan kas, dan
	 * kreditnya masuk ke akun ini.</p>
	 *
	 * @return akun piutang usaha outlet, atau {@code null} untuk memakai konfigurasi global
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_piutang", nullable = true)
	public ais.database.model.akunting.Akun getAkunPiutang() {
		akunPiutang = check(akunPiutang);
		return akunPiutang;
	}

	/**
	 * Setter {@link #getAkunPiutang()} -- tanpa validasi tipe akun, berlaku maju saja; lihat
	 * {@link #setAkunKas(ais.database.model.akunting.Akun)}.
	 *
	 * @param akunPiutang akun piutang usaha outlet, boleh {@code null}
	 */
	public void setAkunPiutang(ais.database.model.akunting.Akun akunPiutang) {
		this.akunPiutang = akunPiutang;
	}

	/**
	 * Akun Modal/Ekuitas Awal outlet.
	 * <p>Menampung selisih debet-kredit pada jurnal pembukaan (saldo awal). Ditempelkan pada master ini (bukan konfigurasi global) supaya
	 * tiap outlet/jenis bisa berbeda; konfigurasi global tetap dipakai sebagai cadangan terakhir
	 * agar pemasangan lama tidak berubah perilakunya. Kolomnya dibuat otomatis oleh Hibernate.</p>
	 */
	private ais.database.model.akunting.Akun akunModalAwal;

	/**
	 * Akun Modal/Ekuitas Awal outlet -- menampung SELISIH debet-kredit pada jurnal pembukaan (saldo
	 * awal). Mengikuti pola pemetaan akun per-outlet dengan cadangan konfigurasi global; lihat
	 * {@link #getAkunKas()} untuk penjelasan lengkapnya.
	 *
	 * <p>Perannya sebagai penampung selisih membuat akun ini perlu perhatian khusus: ia menyerap
	 * apa pun yang tidak seimbang pada jurnal pembukaan agar jurnal tetap balance. Akibatnya
	 * kesalahan pada saldo awal TIDAK menggagalkan pembukaan, melainkan mengendap sebagai angka
	 * pada akun ini -- saldo yang tidak wajar di sini adalah petunjuk pertama bahwa saldo awal
	 * outlet perlu ditinjau ulang.</p>
	 *
	 * @return akun modal/ekuitas awal outlet, atau {@code null} untuk memakai konfigurasi global
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_modal_awal", nullable = true)
	public ais.database.model.akunting.Akun getAkunModalAwal() {
		akunModalAwal = check(akunModalAwal);
		return akunModalAwal;
	}

	/**
	 * Setter {@link #getAkunModalAwal()} -- tanpa validasi tipe akun, berlaku maju saja; lihat
	 * {@link #setAkunKas(ais.database.model.akunting.Akun)}. Mengubahnya setelah jurnal pembukaan
	 * terbentuk tidak memindahkan selisih yang sudah terlanjur mengendap di akun sebelumnya.
	 *
	 * @param akunModalAwal akun modal/ekuitas awal outlet, boleh {@code null}
	 */
	public void setAkunModalAwal(ais.database.model.akunting.Akun akunModalAwal) {
		this.akunModalAwal = akunModalAwal;
	}

	/**
	 * Akun Laba Ditahan outlet.
	 * <p>Tujuan pemindahan laba/rugi bersih saat tutup buku. Ditempelkan pada master ini (bukan konfigurasi global) supaya
	 * tiap outlet/jenis bisa berbeda; konfigurasi global tetap dipakai sebagai cadangan terakhir
	 * agar pemasangan lama tidak berubah perilakunya. Kolomnya dibuat otomatis oleh Hibernate.</p>
	 */
	private ais.database.model.akunting.Akun akunLabaDitahan;

	/**
	 * Akun Laba Ditahan outlet -- tujuan pemindahan laba/rugi bersih saat TUTUP BUKU. Mengikuti pola
	 * pemetaan akun per-outlet dengan cadangan konfigurasi global; lihat {@link #getAkunKas()} untuk
	 * penjelasan lengkapnya.
	 *
	 * <p>Berbeda dari ketiga akun lain yang dipakai pada transaksi harian, akun ini hanya tersentuh
	 * pada peristiwa tutup buku yang jarang dan sulit dibatalkan. Kesalahan pemetaan di sini karena
	 * itu berumur panjang: ia tidak akan terdeteksi oleh pemakaian sehari-hari, dan baru muncul
	 * sebagai laba ditahan yang salah tempat pada laporan posisi keuangan setelah periode ditutup.
	 * Pastikan pemetaan sudah benar SEBELUM tutup buku pertama outlet ini.</p>
	 *
	 * @return akun laba ditahan outlet, atau {@code null} untuk memakai konfigurasi global
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_laba_ditahan", nullable = true)
	public ais.database.model.akunting.Akun getAkunLabaDitahan() {
		akunLabaDitahan = check(akunLabaDitahan);
		return akunLabaDitahan;
	}

	/**
	 * Setter {@link #getAkunLabaDitahan()} -- tanpa validasi tipe akun, berlaku maju saja; lihat
	 * {@link #setAkunKas(ais.database.model.akunting.Akun)}. Mengubahnya setelah tutup buku tidak
	 * memindahkan laba ditahan yang sudah terlanjur dibukukan ke akun sebelumnya.
	 *
	 * @param akunLabaDitahan akun laba ditahan outlet, boleh {@code null}
	 */
	public void setAkunLabaDitahan(ais.database.model.akunting.Akun akunLabaDitahan) {
		this.akunLabaDitahan = akunLabaDitahan;
	}

}
