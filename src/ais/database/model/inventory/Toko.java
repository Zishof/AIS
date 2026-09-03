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

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	@Column(name = "kota", nullable = true, length = 100)
	public String getKota() {
		return kota;
	}

	public void setKota(String kota) {
		this.kota = kota;
	}

	@Column(name = "kode_pos", nullable = true, length = 10)
	public String getKodePos() {
		return kodePos;
	}

	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() {
		return telp;
	}

	public void setTelp(String telp) {
		this.telp = telp;
	}

	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	/** Nama Penanggung Jawab/PIC toko -- kontak utama yang dihubungi admin pusat bila ada kendala operasional toko ini. */
	@Column(name = "pic_nama", nullable = true, length = 255)
	public String getPicNama() {
		return picNama;
	}

	public void setPicNama(String picNama) {
		this.picNama = picNama;
	}

	@Column(name = "pic_hp", nullable = true, length = 50)
	public String getPicHp() {
		return picHp;
	}

	public void setPicHp(String picHp) {
		this.picHp = picHp;
	}

	@Column(name = "npwp", nullable = true, length = 50)
	public String getNpwp() {
		return npwp;
	}

	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/** Teks bebas, mis. {@code "08:00 - 21:00 (Senin-Sabtu)"} -- sengaja satu field teks, bukan struktur jam per-hari, supaya form tetap sederhana sesuai kebutuhan kios kantin. */
	@Column(name = "jam_operasional", nullable = true, length = 100)
	public String getJamOperasional() {
		return jamOperasional;
	}

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

	public void setPesanTerimaKasih(String pesanTerimaKasih) {
		this.pesanTerimaKasih = pesanTerimaKasih;
	}

	@Column(name = "alasan_tahan_json", nullable = true, columnDefinition = "text")
	public String getAlasanTahanJson() {
		return alasanTahanJson;
	}

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

	public void setRoleBolehUbahHarga(String roleBolehUbahHarga) {
		this.roleBolehUbahHarga = roleBolehUbahHarga;
	}

	@Column(name = "toko_demo", nullable = true)
	public Boolean getTokoDemo() {
		return tokoDemo == null ? Boolean.FALSE : tokoDemo;
	}

	public void setTokoDemo(Boolean tokoDemo) {
		this.tokoDemo = tokoDemo;
	}

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

	public void setUnitUsahaJson(String unitUsahaJson) {
		this.unitUsahaJson = unitUsahaJson;
	}

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
		return gudangPemasok;
	}

	public void setGudangPemasok(Gudang gudangPemasok) {
		this.gudangPemasok = gudangPemasok;
	}

	private Pendaftar pendaftar;
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
		return pendaftar;
	}

	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/** Brand/sub-merek opsional yang menaungi toko ini (boleh {@code null} -- toko tanpa brand). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "brand", nullable = true)
	public Brand getBrand() {
		return brand;
	}

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

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_kas", nullable = true)
	public ais.database.model.akunting.Akun getAkunKas() {
		return akunKas;
	}

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

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_piutang", nullable = true)
	public ais.database.model.akunting.Akun getAkunPiutang() {
		return akunPiutang;
	}

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

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_modal_awal", nullable = true)
	public ais.database.model.akunting.Akun getAkunModalAwal() {
		return akunModalAwal;
	}

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

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_laba_ditahan", nullable = true)
	public ais.database.model.akunting.Akun getAkunLabaDitahan() {
		return akunLabaDitahan;
	}

	public void setAkunLabaDitahan(ais.database.model.akunting.Akun akunLabaDitahan) {
		this.akunLabaDitahan = akunLabaDitahan;
	}

}
