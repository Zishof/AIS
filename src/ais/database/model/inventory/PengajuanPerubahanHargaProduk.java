package ais.database.model.inventory;

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
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * Satu baris pengajuan perubahan harga beli/jual untuk sebuah {@link Produk}, dengan alur
 * persetujuan sederhana: dibuat dalam status "Menunggu" ({@link #getTanggalDisetujui()} {@code null})
 * lalu, saat disetujui, harga pada {@link Produk} terkait langsung ditimpa dengan nilai usulan --
 * lihat {@code ais.action.master.inventory.PengajuanPerubahanHargaProdukAction#onSetujui}.
 *
 * <p><b>Verifikasi gerbang persetujuan -- MENEGASKAN pola self-approval/tanpa cek kepemilikan yang
 * sudah tercatat berulang di audit domain lain codebase ini, bukan temuan baru.</b> Penelusuran
 * {@code PengajuanPerubahanHargaProdukAction.onSetujui} menunjukkan syarat persetujuan HANYA hak UI
 * {@code edit} (privilese {@code UPDATE} pada layar ini, dicek oleh {@code
 * PengajuanRenderer.render}) -- <b>tidak ada pengecekan bahwa user yang menekan tombol "Setujui"
 * berbeda dari user yang membuat pengajuan</b> ({@link #getOleh()}/{@link #getOlehId()} milik
 * pengajuan TIDAK dibandingkan dengan user yang sedang login saat {@code onSetujui} dipanggil).
 * Siapa pun yang memiliki hak edit pada layar Pengajuan Perubahan Harga Produk -- termasuk pembuat
 * pengajuan itu sendiri, bila haknya mencakup {@code UPDATE} -- dapat mengajukan SEKALIGUS menyetujui
 * harga usulannya sendiri tanpa keterlibatan pihak kedua. Satu-satunya pembatasan yang benar-benar
 * ditegakkan adalah CAKUPAN TOKO ({@code scopeToko()} membatasi daftar yang terlihat/dapat diajukan
 * ke produk milik toko sendiri bagi pedagang tanpa hak {@code bolehMelihatTokolain}) -- BUKAN
 * pemisahan peran pengaju vs penyetuju. {@link #getDisetujuiOleh()} hanya mencatat SIAPA yang
 * menekan tombol setujui (untuk jejak audit), bukan gerbang validasi yang mencegah orang yang sama
 * menyetujui pengajuannya sendiri.</p>
 *
 * <p>Saat disetujui, {@code onSetujui} menerapkan {@link #getHargaJual()} SELALU (tidak pernah nol,
 * lihat validasi wajib isi di {@code onSave}) dan {@link #getHargaBeli()} HANYA bila bernilai {@code
 * > 0} -- pengajuan dengan harga beli kosong/nol tidak mengubah harga beli produk, hanya harga jual.
 * Tidak ada validasi bisnis lain (mis. batas persentase kenaikan/penurunan wajar, perbandingan dengan
 * harga sebelumnya di luar tampilan referensi "Harga Saat Ini") yang mencegah harga usulan absurd
 * (nol, negatif via {@code MyDoublebox}, atau jauh di luar kewajaran) diterapkan begitu saja begitu
 * disetujui.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengajuan_perubahan_harga_produk")
public class PengajuanPerubahanHargaProduk extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris pengajuan. Digenerasi database ({@code IDENTITY}, kolom {@code insertable = false}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Userid/nama yang mengajukan (membuat) baris pengajuan ini -- lihat javadoc kelas: field ini TIDAK dibandingkan dengan user penyetuju saat persetujuan diproses. */
	private String oleh;
	/** Id user terkait {@link #oleh}. */
	private String olehId;

	/**
	 * Id user yang mengajukan baris pengajuan ini. Lihat javadoc {@link #setOlehId(String)} untuk
	 * perilaku setter yang mengabaikan nilai kosong.
	 * @return id user pengaju, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id user yang mengajukan baris ini -- BUKAN setter pasif biasa. Nilai {@code null} atau
	 * string kosong/berisi-spasi-saja DIABAIKAN secara diam-diam (method langsung {@code return}
	 * tanpa mengubah field, tanpa melempar exception, tanpa log) -- pola guard yang sama dipakai di
	 * banyak model klaster ini. Efek praktisnya: sekali field ini terisi nilai valid, memanggil setter
	 * ini dengan nilai kosong TIDAK PERNAH bisa mengosongkannya lagi.
	 * @param olehId id user pengaju; nilai kosong/hanya-spasi diabaikan secara diam-diam.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi userid/nama yang mengajukan baris ini. Perilaku guard SAMA seperti {@link
	 * #setOlehId(String)}: nilai {@code null} atau kosong/hanya-spasi diabaikan diam-diam, field tidak
	 * pernah dikosongkan kembali lewat setter ini setelah pernah terisi nilai valid.
	 * @param oleh userid/nama pengaju; nilai kosong/hanya-spasi diabaikan secara diam-diam.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Userid/nama yang mengajukan baris pengajuan ini. Lihat javadoc kelas untuk catatan bahwa field
	 * ini TIDAK dipakai sebagai pembanding saat persetujuan diproses (self-approval dimungkinkan).
	 * @return userid/nama pengaju, atau {@code null} bila belum pernah diisi dengan nilai valid.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris
	 * pengajuan ini (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual).
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang
	 * menstempel ulang {@link #tanggal_dirubah}. Murni hook siklus hidup entity -- tidak melakukan
	 * validasi gerbang persetujuan apa pun; lihat javadoc kelas untuk detail gerbang tersebut (yang
	 * sepenuhnya berada di {@code PengajuanPerubahanHargaProdukAction}, bukan di model ini).
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu terakhir baris pengajuan ini diubah -- field audit shadow diisi otomatis oleh
	 * {@link #onUpdate()} pada tiap {@code UPDATE}, terpisah dari riwayat versi penuh envers
	 * ({@code @Audited}). Inisialisasi default memakai waktu konstruksi objek Java, bukan waktu commit
	 * transaksi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu terakhir baris pengajuan ini diubah, diisi otomatis oleh {@link #onUpdate()} pada
	 * tiap {@code UPDATE}.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas baris pengajuan ini untuk kebutuhan log/debug, format
	 * {@code "<id>-<produk>-<hargaBeli>"}.
	 *
	 * <p><b>Efek samping tersembunyi:</b> method ini MEMANGGIL {@link #getProduk()} (bukan membaca
	 * field {@link #produk} mentah) dan MENIMPA field {@link #produk} dengan hasilnya, yang berarti
	 * bila proxy lazy belum ter-inisialisasi, memanggil {@code toString()} pada objek ini DI LUAR sesi
	 * Hibernate aktif dapat melempar {@code LazyInitializationException}. {@link #hargaBeli} yang
	 * ditampilkan adalah field MENTAH (bukan via {@link #getHargaBeli()}), sehingga bisa tercetak
	 * sebagai {@code null} literal dalam string bila belum diisi -- berbeda dari {@link
	 * #getHargaBeli()} yang menormalisasi {@code null} menjadi {@code 0.0}.
	 * @return string ringkas {@code "<id>-<produk>-<hargaBeli>"} (harga beli MENTAH, bisa {@code null}).
	 */
	public String toString() {
		produk = getProduk();
		return id + "-" + produk + "-" + hargaBeli;
	}

	/** Produk yang harganya diajukan untuk diubah -- lihat javadoc {@link #getProduk()}. */
	private Produk produk;
	/** Usulan harga beli (modal) baru -- lihat javadoc {@link #getHargaBeli()} untuk perilaku "hanya diterapkan bila > 0" saat disetujui. */
	private Double hargaBeli;
	/** Usulan harga jual baru -- lihat javadoc {@link #getHargaJual()}; wajib diisi {@code > 0} sebelum baris bisa disimpan ({@code onSave} menolak bila kosong/{@code <= 0}). */
	private Double hargaJual;
	/** Tanggal pengajuan dibuat -- lihat javadoc {@link #getTanggal()} untuk perilaku default. */
	private Date tanggal;
	/**
	 * Field bertipe {@code Boolean} bernama "alasan" dipetakan ke kolom {@code text} -- ketidaksesuaian
	 * tipe/nama yang mencolok (lihat javadoc {@link #getAlasan()} untuk analisis lebih lanjut).
	 */
	private Boolean alasan;

	/** Tanggal pengajuan disetujui; {@code null} berarti masih berstatus "Menunggu" -- lihat javadoc {@link #getTanggalDisetujui()} dan javadoc kelas untuk gerbang persetujuannya. */
	private Date tanggalDisetujui;
	/** User yang menyetujui pengajuan ini -- lihat javadoc {@link #getDisetujuiOleh()} dan javadoc kelas: field ini murni jejak audit, BUKAN gerbang yang mencegah self-approval. */
	private Tbmuser disetujuiOleh;

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database; kode aplikasi yang membuat pengajuan baru (mis. {@code PengajuanPerubahanHargaProdukAction.onSave}) juga memakainya lalu mengisi field lewat setter. */
	public PengajuanPerubahanHargaProduk() {
	}

	/**
	 * Constructor pembantu yang langsung mengisi {@link #id} -- dipakai untuk membuat referensi
	 * ringan (proxy manual) ke baris pengajuan yang sudah ada tanpa memuat seluruh field-nya dari
	 * database, mis. sebagai argumen operasi yang hanya butuh id (bukan untuk membuat baris BARU;
	 * baris baru harus lewat {@link #PengajuanPerubahanHargaProduk()} lalu mengisi field lain).
	 * @param id id baris pengajuan yang sudah ada.
	 */
	public PengajuanPerubahanHargaProduk(Long id) {
		this.id = id;
	}

	/**
	 * Primary key baris pengajuan ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}. Kolom dideklarasikan {@code insertable = false} --
	 * konsisten dengan penggunaan {@code IDENTITY} standar Hibernate.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert (kecuali lewat {@link #PengajuanPerubahanHargaProduk(Long)} untuk referensi ringan). */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Usulan harga beli (modal) baru untuk {@link #getProduk()}. {@code null} dinormalisasi menjadi
	 * {@code 0.0}.
	 *
	 * <p><b>Diterapkan bersyarat saat disetujui.</b> {@code
	 * PengajuanPerubahanHargaProdukAction.onSetujui} HANYA menimpa {@code Produk.hargaBeli} bila nilai
	 * ini {@code > 0} -- pengajuan yang sengaja mengosongkan harga beli (mis. hanya mengajukan
	 * perubahan harga jual) tidak akan mengubah harga beli produk saat disetujui, berbeda dari {@link
	 * #getHargaJual()} yang SELALU diterapkan.</p>
	 * @return usulan harga beli, tidak pernah {@code null} (default {@code 0.0}).
	 */
	public Double getHargaBeli() {
		return hargaBeli == null ? 0.0 : hargaBeli;
	}

	/** @param hargaBeli usulan harga beli (modal) baru; boleh {@code null}/kosong (dibaca sebagai {@code 0.0} dan TIDAK diterapkan ke produk saat disetujui -- lihat javadoc {@link #getHargaBeli()}). */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Usulan harga jual baru untuk {@link #getProduk()}. {@code null} dinormalisasi menjadi
	 * {@code 0.0}, tetapi secara praktis {@code PengajuanPerubahanHargaProdukAction.onSave} MENOLAK
	 * menyimpan baris bila nilai ini kosong atau {@code <= 0} -- sehingga baris yang berhasil
	 * tersimpan lewat jalur UI standar selalu memiliki harga jual {@code > 0}. Saat pengajuan
	 * disetujui, nilai ini SELALU diterapkan ke {@code Produk.hargaJual} tanpa syarat (berbeda dari
	 * {@link #getHargaBeli()} yang bersyarat {@code > 0}).
	 * @return usulan harga jual, tidak pernah {@code null} (default {@code 0.0}).
	 */
	public Double getHargaJual() {
		return hargaJual == null ? 0.0 : hargaJual;
	}

	/** @param hargaJual usulan harga jual baru; boleh {@code null} pada level model (dibaca sebagai {@code 0.0}), tetapi lapisan UI ({@code onSave}) menolak nilai kosong/{@code <= 0} sebelum baris disimpan. */
	public void setHargaJual(Double hargaJual) {
		this.hargaJual = hargaJual;
	}

	/**
	 * Produk yang harganya diajukan untuk diubah. Relasi {@code LAZY}, opsional secara skema
	 * ({@code nullable = true}) meski secara praktis wajib dipilih di UI ({@code onSave} menolak
	 * simpan bila belum dipilih); getter memanggil {@code check(produk)} milik {@link
	 * GeneralValueObject} yang menormalisasi proxy/nilai kosong sebelum dikembalikan. Cakupan produk
	 * yang bisa dipilih dibatasi toko pengaju lewat {@code scopeToko()} pada layar pengajuan -- lihat
	 * javadoc kelas untuk penjelasan bahwa pembatasan ini hanya berlaku pada TOKO, bukan pemisahan
	 * peran pengaju/penyetuju.
	 * @return produk terkait pengajuan ini (bisa proxy lazy, dinormalisasi via {@code check()}), atau
	 *         {@code null} bila baris "dihapus"nya produk (produk terkait sudah tidak ada).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = true)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/** @param produk produk yang harganya diajukan untuk diubah. */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Tanggal pengajuan ini dibuat/diajukan (bukan tanggal persetujuan -- lihat {@link
	 * #getTanggalDisetujui()} untuk itu). Bila field mentah {@code null}, getter mengembalikan {@code
	 * WaktuUtil.getDate()} -- waktu SAAT GETTER DIPANGGIL, bukan nilai stabil -- sehingga pemanggil
	 * yang butuh tanggal pengajuan yang akurat harus memastikan {@link #setTanggal(Date)} dipanggil
	 * eksplisit sebelum {@code save} (yang memang dilakukan {@code onSave}, dengan fallback ke
	 * {@code new Date()} bila kolom Datebox UI kosong).
	 * @return tanggal pengajuan dibuat, atau waktu saat ini bila belum pernah di-{@code set}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/** @param tanggal tanggal pengajuan ini dibuat/diajukan. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Field bertipe {@code Boolean} bernama "alasan" tetapi dipetakan ke kolom database bertipe
	 * {@code text} -- ketidaksesuaian tipe Java ({@code Boolean}) dengan {@code columnDefinition}
	 * ({@code "text"}) dan dengan nama field itu sendiri (nama menyiratkan teks alasan pengajuan,
	 * tetapi tipenya {@code Boolean}, bukan {@code String}). Verifikasi kode pemakai: {@code
	 * PengajuanPerubahanHargaProdukAction} (satu-satunya Action yang memakai entity ini) TIDAK
	 * membaca maupun menulis {@link #getAlasan()}/{@link #setAlasan(Boolean)} di form maupun renderer
	 * mana pun -- field ini tidak terhubung ke UI apa pun saat ini, kemungkinan sisa rancangan awal
	 * (mis. dimaksudkan sebagai flag "ada alasan tertulis?" sebelum kolom teks alasan yang
	 * sesungguhnya ditambahkan, lalu tidak pernah dituntaskan) atau field yang keliru diberi tipe saat
	 * dibuat. Jangan mengandalkan field ini untuk menyimpan teks alasan pengajuan -- tidak ada tempat
	 * lain di entity ini yang menampung teks tersebut.
	 * @return nilai boolean mentah field {@code alasan}, atau {@code null} bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public Boolean getAlasan() {
		return alasan;
	}

	/** @param alasan nilai boolean mentah untuk field {@code alasan}; lihat javadoc {@link #getAlasan()} untuk catatan bahwa field ini tidak dipakai kode UI saat ini. */
	public void setAlasan(Boolean alasan) {
		this.alasan = alasan;
	}

	/**
	 * User yang menyetujui pengajuan ini -- diisi {@code onSetujui} dengan {@code
	 * Common.getCurrentUser()} pada saat persetujuan diproses. Relasi {@code LAZY}, opsional; getter
	 * memanggil {@code check(disetujuiOleh)} milik {@link GeneralValueObject} yang menormalisasi
	 * proxy/nilai kosong sebelum dikembalikan.
	 *
	 * <p><b>PENTING -- ini adalah jejak audit, BUKAN gerbang otorisasi.</b> Lihat javadoc kelas:
	 * tidak ada kode yang membandingkan field ini (atau {@link #getOleh()}/{@link #getOlehId()}
	 * milik pengajuan) dengan user yang sedang login sebelum mengizinkan tombol "Setujui" ditekan --
	 * field ini hanya MENCATAT siapa yang menekan tombol tersebut setelah faktanya, tidak mencegah
	 * siapa pun yang berhak (termasuk pengaju sendiri) melakukannya.</p>
	 * @return user yang menyetujui pengajuan ini (bisa proxy lazy, dinormalisasi via {@code check()}),
	 *         atau {@code null} bila belum disetujui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/** @param disetujuiOleh user yang menyetujui pengajuan ini. Normalnya TIDAK diisi manual oleh kode aplikasi lain -- diisi otomatis oleh {@code onSetujui} dengan user yang sedang login. */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Tanggal pengajuan ini disetujui. {@code null} adalah PENANDA STATUS UTAMA: {@code
	 * PengajuanRenderer.render} memakai {@code getTanggalDisetujui() != null} untuk menentukan apakah
	 * baris berstatus "Disetujui" (tombol Setujui disembunyikan) atau "Menunggu" (tombol Setujui
	 * ditampilkan bila user punya hak edit). Berbeda dari kebanyakan field tanggal di klaster ini,
	 * getter ini TIDAK menormalisasi {@code null} menjadi waktu saat ini -- mempertahankan {@code
	 * null} apa adanya justru esensial agar logika status "Menunggu vs Disetujui" bekerja benar.
	 * @return tanggal disetujui, atau {@code null} bila masih berstatus "Menunggu".
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_disetujui")
	public Date getTanggalDisetujui() {
		return tanggalDisetujui;
	}

	/** @param tanggalDisetujui tanggal pengajuan disetujui. Normalnya TIDAK diisi manual oleh kode aplikasi lain -- diisi otomatis oleh {@code onSetujui} dengan {@code new Date()} saat persetujuan diproses. */
	public void setTanggalDisetujui(Date tanggalDisetujui) {
		this.tanggalDisetujui = tanggalDisetujui;
	}

}
