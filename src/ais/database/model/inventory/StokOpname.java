package ais.database.model.inventory;

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

/**
 * Model data untuk stok opname. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code Produk produk}, {@code
 * Toko toko}, {@code Double stokSistem}, {@code Double stokFisik}, {@code Double selisih}, {@code Date
 * waktuOpname}, {@code String keterangan}; pemetaan persistence: tabel {@code koperasi.stok_opname};
 * pembacaan/pencarian ({@code getId()}, {@code getProduk()}, {@code getToko()}, {@code getStokSistem()}, {@code
 * getStokFisik()}, {@code getSelisih()}); mutasi data ({@code onUpdate()}, {@code setId()}, {@code setProduk()},
 * {@code setToko()}, {@code setStokSistem()}, {@code setStokFisik()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "stok_opname")
public class StokOpname extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Primary key baris opname. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;

	/** Produk (baris per-toko, bukan produk generik) yang dihitung fisiknya pada baris opname ini. Wajib diisi (kolom {@code produk} {@code nullable = false}). */
	private Produk produk;

	/** Toko/outlet tempat opname dilakukan. Wajib diisi (kolom {@code toko} {@code nullable = false}); menentukan cakupan laporan Berita Acara per toko. */
	private Toko toko;

	// Stok di aplikasi saat opname dilakukan
	private Double stokSistem;
	// Stok nyata yang dihitung karyawan
	private Double stokFisik;
	// Fisik dikurang Sistem (Minus = Hilang/Rusak, Plus = Ketemu barang lebih)
	private Double selisih;

	/** Waktu baris opname ini dicatat (bukan waktu dokumen sesi induk {@link SesiStokOpname}). {@code null} pada objek baru diisi waktu-baca oleh {@link #getWaktuOpname()}. */
	private Date waktuOpname;
	// Alasan: "Barang Basi", "Hilang dicuri", dll
	private String keterangan;

	/** Userid/nama petugas yang mencatat baris opname ini (bebas teks, tidak ber-FK ke tabel user). Dipakai sebagai jejak audit ringan terpisah dari envers ({@code @Audited}). */
	private String oleh;

	/**
	 * Hook lifecycle Hibernate yang dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris ini
	 * (dipicu oleh anotasi {@link javax.persistence.PreUpdate} di atasnya, BUKAN dipanggil manual oleh
	 * kode aplikasi). Mendelegasikan seluruh pekerjaan ke {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, yang bertugas menstempel ulang
	 * field audit (mis. {@link #tanggal_dirubah}) dengan waktu server saat ini. Method ini sengaja
	 * kosong dari logika bisnis lain -- ia murni titik kait (hook) siklus hidup entity, bukan tempat
	 * validasi selisih opname atau efek samping domain. Visibilitas {@code protected} sesuai kontrak
	 * JPA/Hibernate untuk callback lifecycle: harus bisa diakses oleh proxy/subclass yang dibuat
	 * Hibernate saat membungkus entity, tapi tidak perlu (dan tidak boleh) dipanggil langsung dari kode
	 * lapisan service/action. Karena anotasi ini hanya bereaksi pada {@code UPDATE}, insert pertama kali
	 * (saat baris opname baru disimpan) tidak memicu callback ini -- {@link #tanggal_dirubah} pada baris
	 * baru berasal murni dari nilai inisialisasi field-nya, bukan dari method ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Stempel waktu terakhir baris ini diubah -- field audit shadow yang diisi otomatis oleh {@link
	 * #onUpdate()} pada tiap {@code UPDATE}, terpisah dari mekanisme envers ({@code @Audited}) yang
	 * menyimpan riwayat versi penuh di tabel {@code _AUD}. Inisialisasi default memakai waktu saat objek
	 * Java dibuat (bukan waktu commit transaksi), sehingga pada baris yang baru di-{@code INSERT} nilainya
	 * adalah waktu konstruksi objek in-memory, bukan waktu baris benar-benar tersimpan ke database.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database. Kode aplikasi yang membuat baris opname baru juga memakai constructor ini lalu mengisi field lewat setter. */
	public StokOpname() {
	}

	/**
	 * Primary key baris opname ini. Digenerasi database via strategi {@code IDENTITY} saat baris pertama
	 * kali disimpan; {@code null} pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris. Normalnya TIDAK diisi manual oleh kode aplikasi -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Baris {@link Produk} (skema toko-per-baris, bukan produk generik lintas toko) yang stok fisiknya
	 * dihitung pada baris opname ini. Relasi {@code LAZY} -- mengakses field pada objek proxy yang belum
	 * di-inisialisasi di luar sesi Hibernate aktif akan melempar {@code LazyInitializationException}.
	 * Wajib diisi ({@code nullable = false}); cascade PERSIST/MERGE dari sisi ini memungkinkan produk baru
	 * ikut tersimpan bila belum ada, meski pola normalnya produk sudah ada sebelum opname dilakukan.
	 * @return baris produk yang diopname (bisa proxy lazy).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		return produk;
	}

	/** @param produk baris produk yang diopname. */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Toko/outlet tempat opname fisik ini dilakukan. Menentukan cakupan agregasi laporan Berita Acara
	 * Stock Opname (§2.6) bersama {@link SesiStokOpname#getToko()} -- baris opname tidak langsung
	 * ber-relasi ke sesi, keduanya disatukan lewat rentang tanggal &amp; toko yang sama, bukan foreign key.
	 * Relasi {@code LAZY}, wajib diisi.
	 * @return toko tempat baris opname ini dicatat (bisa proxy lazy).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		return toko;
	}

	/** @param toko toko tempat baris opname ini dicatat. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Stok menurut sistem (aplikasi) pada saat opname dilakukan -- snapshot beku, BUKAN nilai live yang
	 * dihitung ulang tiap dibaca. Diisi eksplisit oleh pemanggil (lihat {@code StokOpnameScanUtil} /
	 * {@code StokOpnameKantinAction}) dari hasil kalkulasi {@code StokKantinUtil.formulaStokSql} pada
	 * saat opname dibuat, sehingga nilainya tetap mencerminkan kondisi sistem SAAT opname, bahkan jika
	 * stok sistem berubah lagi setelahnya. {@code null} dinormalisasi menjadi {@code 0.0} agar aman
	 * dipakai langsung pada aritmatika {@link #getSelisih()} tanpa NPE.
	 * @return stok sistem saat opname, tidak pernah {@code null} (default {@code 0.0}).
	 */
	public Double getStokSistem() {
		return stokSistem == null ? 0.0 : stokSistem;
	}

	/** @param stokSistem stok sistem saat opname dilakukan; boleh {@code null} (dibaca sebagai {@code 0.0}). */
	public void setStokSistem(Double stokSistem) {
		this.stokSistem = stokSistem;
	}

	/**
	 * Stok fisik hasil hitung nyata oleh karyawan/petugas opname di lapangan -- angka input manusia,
	 * rawan salah hitung atau, dalam skenario kecurangan, sengaja dimanipulasi untuk menutupi barang
	 * hilang. Tidak ada validasi range/batas atas pada level model ini; validasi (bila ada) berada di
	 * lapisan UI/service pemanggil. {@code null} dinormalisasi menjadi {@code 0.0}.
	 * @return stok hasil hitung fisik, tidak pernah {@code null} (default {@code 0.0}).
	 */
	public Double getStokFisik() {
		return stokFisik == null ? 0.0 : stokFisik;
	}

	/** @param stokFisik stok hasil hitung fisik; boleh {@code null} (dibaca sebagai {@code 0.0}). */
	public void setStokFisik(Double stokFisik) {
		this.stokFisik = stokFisik;
	}

	/**
	 * Selisih antara stok fisik dan stok sistem ({@code stokFisik - stokSistem}) -- jantung domain
	 * "kecurangan/selisih fisik" modul ini. Nilai negatif berarti barang HILANG/RUSAK/dicuri (fisik lebih
	 * sedikit dari catatan sistem); nilai positif berarti KETEMU barang lebih (bisa indikasi kesalahan
	 * pencatatan sebelumnya, atau justru indikasi barang masuk tanpa tercatat -- kebocoran di arah
	 * sebaliknya).
	 *
	 * <p><b>PENTING -- getter ini DESTRUKTIF/self-recomputing, bukan accessor pasif.</b> Setiap kali
	 * dipanggil, method ini MENGHITUNG ULANG {@code selisih = getStokFisik() - getStokSistem()} dan
	 * MENIMPA field in-memory {@link #selisih} dengan hasilnya sebelum mengembalikannya -- nilai apa pun
	 * yang sebelumnya di-{@code set} lewat {@link #setSelisih(Double)} akan hilang begitu getter ini
	 * dipanggil sesudahnya, karena getter selalu menang atas nilai yang di-set manual. Ini artinya
	 * pemanggil TIDAK BISA mengandalkan urutan "set lalu get" untuk mempertahankan nilai custom pada
	 * field ini -- get selalu mengembalikan hasil kalkulasi live dari {@code stokFisik}/{@code
	 * stokSistem} saat ITU dipanggil, bukan snapshot yang dibekukan seperti {@link #getStokSistem()}.</p>
	 *
	 * <p><b>Implikasi pada persistence Hibernate.</b> Karena Hibernate (dengan {@code dynamicUpdate =
	 * true}) menulis kolom {@code selisih} ke database berdasarkan NILAI YANG DIKEMBALIKAN getter ini
	 * pada saat flush/commit -- bukan berdasarkan field mentah -- maka kolom {@code selisih} di database
	 * SELALU konsisten dengan {@code stokFisik - stokSistem} pada saat baris terakhir di-flush, TIDAK
	 * PERNAH bisa menyimpang darinya selama getter dipanggil melalui jalur normal Hibernate. Ini adalah
	 * pengaman implisit yang bagus terhadap manipulasi langsung nilai {@code selisih} tanpa mengubah
	 * fisik/sistem -- tapi juga berarti {@link #setSelisih(Double)} pada praktiknya TIDAK PERNAH benar-
	 * benar "menang" pada baris yang sudah pernah dibaca ulang lewat getter ini sebelum disimpan.
	 * Komentar eksplisit di {@code StokOpnameKantinAction} ("getSelisih() (computed getter) yang TIDAK
	 * boleh diandalkan mengisi kolom ini otomatis -- WAJIB ditulis eksplisit") mengonfirmasi bahwa
	 * developer lain sudah pernah tersandung pola ini: memanggil {@code setSelisih(...)} secara eksplisit
	 * sebelum {@code session.save()}/{@code flush()} tetap WAJIB dilakukan di kode pemanggil bila
	 * urutan operasi membuat ada kemungkinan getter dipanggil (mis. oleh UI binding) SEBELUM set manual
	 * terjadi -- jangan berasumsi getter otomatis "menyelamatkan" nilai kolom.</p>
	 *
	 * <p><b>Temuan integritas terkait penjaga keseimbangan opname (audit domain stok/opname).</b> Baris
	 * {@code StokOpname} individual TIDAK memiliki mekanisme blocking apa pun berdasarkan besaran
	 * {@code selisih} -- getter/setter ini murni kalkulasi & penyimpanan angka, tanpa validasi ambang
	 * batas (mis. menolak selisih di atas toleransi tertentu tanpa approval tambahan). Penelusuran alur
	 * penutupan sesi opname ({@code KantinHelper.soSesiSelesai} yang mengubah {@link
	 * SesiStokOpname#getStatus()} menjadi {@link SesiStokOpname#STATUS_SELESAI}) mengonfirmasi sesi BISA
	 * ditutup tanpa memeriksa satu pun baris {@code StokOpname} terkait -- tidak ada agregasi selisih,
	 * tidak ada ambang toleransi, tidak ada gerbang approval berjenjang untuk selisih besar sebelum status
	 * berubah menjadi final. Pola ini KONSISTEN dengan pola "soft-check, bukan hard-block" yang sudah
	 * terdokumentasi di {@code koperasi.SalesInventoryTripHelper} (modul lain, batch javadoc sebelumnya)
	 * -- selisih DICATAT (baik di baris ini maupun sebagai perubahan stok produk via {@code
	 * recomputeStokProdukNative}) tetapi TIDAK PERNAH memblokir alur bisnis apa pun secara otomatis;
	 * mitigasi kecurangan sepenuhnya bergantung pada review manual atas laporan/Berita Acara setelah
	 * fakta, bukan pencegahan preventif di level data model atau service. Karena ini memperkuat pola yang
	 * sudah tercatat (bukan temuan baru), tidak diajukan task eskalasi terpisah dari dokumentasi ini.</p>
	 *
	 * @return selisih fisik dikurang sistem, dihitung ulang setiap panggilan (bukan nilai tersimpan pasif).
	 */
	public Double getSelisih() {
		selisih = getStokFisik() - getStokSistem();
		return selisih;
	}

	/**
	 * Menimpa field {@link #selisih} secara manual. Perhatikan risiko yang dijelaskan di {@link
	 * #getSelisih()}: nilai yang diset di sini akan TERTIMPA lagi begitu {@link #getSelisih()} dipanggil
	 * (misalnya oleh Hibernate saat flush, atau oleh binding UI), karena getter tersebut selalu menghitung
	 * ulang dari {@code stokFisik}/{@code stokSistem} alih-alih mengembalikan field mentah ini apa adanya.
	 * @param selisih nilai selisih yang ingin diset; efektif hanya sampai {@link #getSelisih()} berikutnya dipanggil.
	 */
	public void setSelisih(Double selisih) {
		this.selisih = selisih;
	}

	/**
	 * Waktu baris opname ini dicatat. {@code null} (baris baru yang belum diisi eksplisit) dibaca sebagai
	 * waktu-baca saat ini ({@link ais.ui.util.WaktuUtil#getDate()}) -- BUKAN waktu baris dibuat, karena
	 * nilai ini dihitung ulang setiap getter dipanggil selama field mentah masih {@code null}; begitu
	 * di-{@code set} eksplisit atau dibaca dari database (nilai tidak null), hasilnya stabil.
	 * @return waktu opname; default waktu-baca saat ini bila belum pernah diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuOpname() {
		return waktuOpname == null ? ais.ui.util.WaktuUtil.getDate() : waktuOpname;
	}

	/** @param waktuOpname waktu baris opname ini dicatat. */
	public void setWaktuOpname(Date waktuOpname) {
		this.waktuOpname = waktuOpname;
	}

	/**
	 * Alasan/catatan bebas teks untuk selisih pada baris ini (mis. "Barang Basi", "Hilang dicuri",
	 * "Kesalahan input awal"). Kolom {@code text} tanpa batas panjang keras, tanpa validasi format atau
	 * daftar nilai baku (bukan enum) -- sepenuhnya isian bebas operator, sehingga tidak bisa dipakai
	 * sebagai dasar klasifikasi/agregasi otomatis (mis. total kerugian per kategori "hilang" vs "rusak")
	 * tanpa parsing teks tambahan di lapisan laporan.
	 * @return keterangan/alasan selisih, atau {@code null} bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan alasan/catatan selisih. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Userid/nama petugas yang mencatat baris opname ini. Bebas teks, tidak ber-FK ke tabel user --
	 * dipakai sebagai jejak audit ringan yang tetap terbaca meski akun user yang bersangkutan kemudian
	 * dihapus/diubah, berbeda dengan riwayat penuh yang disimpan mekanisme envers ({@code @Audited}).
	 * @return identitas petugas pencatat, atau {@code null} bila tidak diisi oleh pemanggil.
	 */
	public String getOleh() {
		return oleh;
	}

	/** @param oleh identitas petugas pencatat baris opname ini. */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Stempel waktu terakhir baris ini diubah, diisi otomatis oleh {@link #onUpdate()} pada tiap
	 * {@code UPDATE}. Lihat javadoc field {@link #tanggal_dirubah} untuk detail perbedaannya dari
	 * mekanisme envers.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Penanda jurnal. Jurnal selisih opname: Persediaan lawan akun selisih persediaan. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Jejak posting jurnal untuk baris selisih opname ini (Persediaan lawan akun selisih persediaan).
	 * {@code null} berarti baris ini BELUM diposting ke buku besar -- baris opname dapat eksis lama
	 * dalam keadaan belum diposting tanpa mempengaruhi status {@link SesiStokOpname}, karena kedua
	 * konsep (penutupan sesi vs posting jurnal) tidak saling mengunci satu sama lain di level model.
	 * Relasi {@code LAZY}; mengakses field {@code PostingHistory} di luar sesi aktif dapat melempar
	 * {@code LazyInitializationException}.
	 * @return jejak posting jurnal, atau {@code null} bila belum diposting.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * @param postingHistory jejak posting jurnal baris ini. Diisi oleh proses posting akunting, bukan
	 *                        oleh alur pencatatan opname itu sendiri -- pemisahan tanggung jawab yang
	 *                        sama dipakai di seluruh model finansial modul ini (lihat {@link
	 *                        MutasiStokToko#getPostingHistory()}).
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
