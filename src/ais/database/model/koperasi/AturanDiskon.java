package ais.database.model.koperasi;

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

import ais.database.model.Tbmuser;
import ais.database.model.VoKunci;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.sop.DisposisiSop;

/**
 * Satu aturan/baris promo diskon per-produk (atau global bila
 * {@link #getProduk()} kosong) di koperasi/kantin -- mesin promo UTAMA yang
 * dievaluasi setiap checkout POS oleh {@code KantinHelper.loadAturanDiskonKandidat}
 * dan {@code PosKantinAction.evaluasiDiskon}. Paralel dengan (BUKAN
 * turunan/detail dari) {@link GrupAturanDiskon}: keduanya digabung ke satu
 * daftar kandidat dan dievaluasi oleh SATU mesin hitung yang sama di
 * {@code KantinHelper} -- lihat javadoc {@link GrupAturanDiskon} untuk
 * perbedaan cakupan (satu produk/semua produk di sini, vs banyak produk
 * eksplisit lewat {@link GrupAturanDiskonDetail} di sana).
 *
 * <h3>Rumus dan urutan evaluasi (diverifikasi dari {@code KantinHelper}, method
 * privat yang menghitung diskon per item keranjang)</h3>
 * <ol>
 * <li><b>Kelayakan (eligibility).</b> Baris ini dipertimbangkan untuk satu item
 * keranjang bila: {@link #getAktif()} true, {@link #getProduk()} cocok
 * ATAU {@code null} (berlaku semua produk), {@link #getToko()} cocok ATAU
 * {@code null}, jendela {@link #getTanggalMulai()}/{@link #getTanggalSelesai()}
 * mencakup waktu sekarang (atau {@code null} = tanpa batas), hari sekarang
 * termasuk {@link #getHariAktif()} (atau kosong = semua hari), sasaran member
 * cocok ({@link #getBerlakuSemuaMember()} true, atau
 * {@link #getJenisAnggota()}/{@link #getTipeAnggota()} cocok dengan member yang
 * checkout), dan -- kecuali dipanggil eksplisit lewat picker promo (member
 * memilih manual berdasarkan {@link #getId()}) -- {@link #getAktivasiManual()}
 * TIDAK true (baris {@code aktivasiManual=true} dikecualikan dari auto-apply).</li>
 * <li><b>Pengurutan kandidat.</b> Dari baris-baris yang lolos kelayakan untuk
 * satu item, diurutkan menurun berdasarkan {@link #getPrioritas()} (angka lebih
 * besar dihitung lebih dulu), lalu (bila prioritas sama) berdasarkan estimasi
 * nilai potongan tertinggi, lalu berdasarkan {@link #getId()} sebagai
 * tie-breaker deterministik.</li>
 * <li><b>Penumpukan (stacking).</b> Kandidat pertama SELALU diterapkan.
 * Kandidat berikutnya HANYA ikut ditumpuk bila baris pertama DAN baris itu
 * SENDIRI sama-sama punya {@link #getDapatDigabung()} true (kedua sisi wajib
 * mengizinkan; satu sisi {@code false} sudah cukup menghentikan penumpukan) --
 * dan baris dengan {@link #getGrupEksklusif()} yang kodenya SAMA (tidak
 * kosong) dengan kode yang sudah dipakai pada item itu dilewati (satu kode
 * eksklusif hanya boleh terpakai sekali per item).</li>
 * <li><b>Nilai potongan per baris.</b> Basis nominal ({@code dasar}) ditentukan
 * oleh {@link #getDasarPerhitungan()}: {@code "HARGA_AWAL"} = selalu dari
 * {@code harga x jumlah} item (utuh, sebelum diskon lain); {@code "SETELAH_DISKON"}
 * (default) = dari sisa harga SETELAH diskon-diskon yang sudah ditumpuk lebih
 * dulu pada item yang sama ({@code max(0, itemTotal - diskonTerkumpul)}) --
 * jadi urutan penumpukan MEMENGARUHI hasil akhir, bukan komutatif. Lalu:
 * <ul>
 * <li>bila {@link #getPersentase()} {@code > 0}: {@code nilai = dasar * (persentase / 100)};</li>
 * <li>selain itu, bila {@link #getNominal()} {@code > 0}: {@code nilai = nominal * jumlah},
 * DIPOTONG (clamp) agar tidak melebihi {@code dasar};</li>
 * <li>{@link #getPersentase()} dan {@link #getNominal()} TIDAK PERNAH dijumlahkan --
 * hanya salah satu yang aktif per baris, persentase diprioritaskan bila
 * keduanya terisi {@code > 0}.</li>
 * </ul></li>
 * <li><b>Batas maksimum.</b> Bila {@link #getMaksimalPotongan()} {@code > 0},
 * nilai hasil langkah 4 dipotong ke batas itu. Kasus khusus
 * {@link #getBerlakuPerHariDanPerToko()} true: batas ini bukan per-transaksi
 * melainkan KUOTA HARIAN per toko -- sisa kuota dihitung dari akumulasi
 * pemakaian hari itu (lintas transaksi) DIKURANGI pemakaian yang SUDAH terjadi
 * di keranjang yang sedang diproses (sehingga beberapa item produk yang sama
 * dalam satu keranjang tetap saling berbagi satu kuota harian, bukan
 * masing-masing dapat kuota penuh); begitu sisa kuota habis, nilai potongan
 * baris ini menjadi {@code 0} untuk sisa keranjang/hari itu.</li>
 * <li><b>Penyaluran manfaat: potong harga vs cashback.</b> Bila
 * {@link #getPotonganLangsung()} true (default), nilai masuk akumulator
 * {@code diskon} (memotong harga struk saat itu juga, dibatasi tidak melebihi
 * sisa total item). Bila {@code false}, nilai masuk akumulator {@code cashback}
 * -- TIDAK mengurangi total tagihan saat checkout, melainkan dikreditkan ke
 * saldo diskon/cashback member untuk dicairkan belakangan lewat
 * {@link PencairanDiskon} (lihat javadoc class itu). Total {@code cashback}
 * akhir per item dipotong agar total manfaat (diskon + cashback) tidak pernah
 * melebihi nilai barang -- diskon 100% tidak boleh masih menyisakan cashback
 * tambahan.</li>
 * <li><b>Jejak audit hasil.</b> ID baris {@link #getId()} yang pertama kali
 * memberi kontribusi disimpan sebagai referensi (kolom
 * {@code pembelian.aturan_diskon}); nama-nama promo yang diterapkan digabung
 * dengan {@code " + "} untuk ditampilkan di struk/UI kasir.</li>
 * </ol>
 *
 * <p><b>Kunci baris & disposisi SOP.</b> Berbeda dari {@link GrupAturanDiskon}
 * (yang extends {@code GeneralValueObject} polos), class ini extends
 * {@link VoKunci} sehingga MEWARISI mekanisme kunci-baris
 * ({@link #getDikunci()}) dan tautan disposisi SOP
 * ({@link #getDisposisiSop()}) -- dua fitur workflow persetujuan yang TIDAK
 * dimiliki {@link GrupAturanDiskon} meski keduanya sama-sama "aturan diskon".
 * Field {@link #getOleh()}/{@link #getOlehId()} di sini punya guard
 * "abaikan bila kosong" pada setter-nya (nilai lama dipertahankan bila
 * dipanggil dengan string kosong/null) -- pola yang SAMA dipakai di
 * {@link PencairanDiskon}, tapi TIDAK dipakai pada setter serupa di
 * {@link GrupAturanDiskon}/{@link GrupAturanDiskonDetail} (setter-nya polos,
 * menerima {@code null} apa adanya) -- inkonsistensi kecil antar dua keluarga
 * class "aturan diskon" dalam paket yang sama.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "aturan_diskon")
public class AturanDiskon extends VoKunci {

	private static final long serialVersionUID = 1L;

	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** Nama promo yang tampil di kasir/struk/laporan, mis. {@code "Promo Akhir Tahun Indomie"}. */
	private String namaAturan;
	/** Catatan bebas admin, tidak tampil ke pembeli. */
	private String keterangan;

	// --- 1. TARGET PRODUK & TOKO ---
	/** Produk sasaran. {@code null} = berlaku untuk SEMUA produk (aturan global/berbasis toko saja). */
	private Produk produk;
	/** Toko/kios sasaran. {@code null} = berlaku di semua toko/kios. */
	private Toko toko;

	// --- 2. TARGET MEMBER ---
	/** {@code true} = promo berlaku publik/semua orang; {@code false} = harus dicek {@link #jenisAnggota}/{@link #tipeAnggota}. */
	private Boolean berlakuSemuaMember;
	/** Jenis anggota koperasi sasaran (OPSIONAL, mis. Guru/Siswa); relevan hanya bila {@link #berlakuSemuaMember} false. */
	private JenisAnggotaKoperasi jenisAnggota;
	/** Tipe anggota koperasi sasaran (OPSIONAL, mis. VIP/Reguler); relevan hanya bila {@link #berlakuSemuaMember} false. */
	private TipeAnggotaKoperasi tipeAnggota;

	// --- 3. NILAI DISKON ---
	/** Persentase diskon (mis. {@code 10.0} untuk 10%). Diprioritaskan drpd {@link #nominal} bila {@code > 0} -- lihat rumus di javadoc kelas. */
	private Double persentase;
	/** Batas maksimal potongan rupiah hasil {@link #persentase}/{@link #nominal}; {@code 0}/{@code null} = tanpa batas (atau kuota harian bila {@link #berlakuPerHariDanPerToko} true). */
	private Double maksimalPotongan;
	/** Diskon nominal tetap rupiah per unit (mis. Rp 2.000), dikalikan jumlah dan dipotong agar tidak melebihi basis perhitungan. Dipakai hanya bila {@link #persentase} tidak diisi/{@code <= 0}. */
	private Double nominal;
	// Konflik promo: angka lebih besar dihitung lebih dahulu. Default aman tidak ditumpuk.
	/** Prioritas urutan evaluasi; angka LEBIH BESAR dihitung LEBIH DAHULU saat beberapa aturan cocok untuk item yang sama. Default {@code 100} bila {@code null}. */
	private Integer prioritas;
	/** {@code true} = aturan ini boleh ditumpuk dengan aturan lain yang JUGA mengizinkan penggabungan pada item yang sama; default aman {@code false} (tidak ditumpuk). */
	private Boolean dapatDigabung;
	/** Basis nominal perhitungan persentase: {@code "SETELAH_DISKON"} (default) atau {@code "HARGA_AWAL"} -- lihat rumus lengkap di javadoc kelas. */
	private String dasarPerhitungan;
	/** Kode bebas; aturan lain dengan kode sama (tidak kosong) tidak boleh dipakai bersamaan pada item yang sama. */
	private String grupEksklusif;

	// --- 4. LOGIKA PENERAPAN (POTONG HARGA vs SALDO/CASHBACK) ---
	/** {@code true} (default) = potong harga langsung di struk. {@code false} = masuk ke saldo diskon/cashback member, dicairkan belakangan lewat {@link PencairanDiskon}. */
	private Boolean potonganLangsung;

	/** {@code true} = {@link #maksimalPotongan} menjadi KUOTA HARIAN per toko (bukan per-transaksi). {@code false} (default) = berlaku penuh tiap transaksi. */
	private Boolean berlakuPerHariDanPerToko;

	// --- 5. MASA BERLAKU ---
	/** Tanggal mulai berlaku (inklusif); {@code null} = tanpa batas mulai. */
	private Date tanggalMulai;
	/** Tanggal berakhir berlaku (inklusif); {@code null} = tanpa batas akhir. */
	private Date tanggalSelesai;
	/** CSV hari ISO weekday (1=Senin..7=Minggu); {@code null}/kosong = berlaku semua hari. Lihat javadoc lengkap {@link #getHariAktif()}. */
	private String hariAktif;
	/** Status aktif baris; {@code null} diperlakukan sebagai {@code TRUE} oleh {@link #getAktif()} (fail-open). */
	private Boolean aktif;

	// TRUE = tidak auto-terapkan di checkout, kasir wajib pilih manual lewat picker promo.
	// null/false = perilaku default (auto-apply, aturan pertama yang cocok langsung dipakai).
	/** {@code true} = dikecualikan dari auto-apply; hanya aktif bila member/kasir memilihnya manual lewat picker promo. {@code null}/{@code false} = perilaku default (auto-apply). */
	private Boolean aktivasiManual;

	/** Nama/username pengguna yang terakhir mengubah baris ini (audit trail). Setter mengabaikan nilai kosong/{@code null} -- lihat {@link #setOleh(String)}. */
	private String oleh;
	/** ID pengguna yang terakhir mengubah baris ini (audit trail). Setter mengabaikan nilai kosong/{@code null} -- lihat {@link #setOlehId(String)}. */
	private String olehId;

	/** Pengguna yang mengunci baris ini (workflow kunci-baris {@link VoKunci}); {@code null} = tidak dikunci. */
	private Tbmuser dikunci;
	/** Tautan disposisi SOP (workflow persetujuan) terkait baris ini; {@code null} = tidak/ belum ditautkan ke SOP. */
	private DisposisiSop disposisiSop;

	/** @return ID pengguna yang terakhir mengubah baris ini (audit trail). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId ID pengguna pengubah. Nilai {@code null}/kosong DIABAIKAN
	 *               (nilai lama dipertahankan) -- guard yang sama dipakai
	 *               {@link #setOleh(String)} dan {@link PencairanDiskon#setOlehId(String)},
	 *               agar operasi sistem/otomatis yang tidak membawa identitas
	 *               pengguna tidak menimpa jejak audit pengguna terakhir yang
	 *               valid dengan {@code null}.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * @param oleh nama/username pengubah. Nilai {@code null}/kosong DIABAIKAN
	 *             (nilai lama dipertahankan) -- lihat catatan di {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama/username pengguna yang terakhir mengubah baris ini (audit trail). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyerahkan pembaruan cap waktu audit
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah} setiap
	 * kali baris ini di-UPDATE, agar konsisten dengan intersepsi Hibernate
	 * lain di seluruh basis kode (bukan diatur manual per aksi pemanggil).
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Cap waktu perubahan terakhir; diisi otomatis saat konstruksi dan diperbarui via {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah cap waktu perubahan; biasanya diatur otomatis, bukan manual. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return cap waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Konstruktor kosong (dipakai Hibernate dan pemanggil yang membangun baris baru). */
	public AturanDiskon() {
	}

	/** @return ID baris (primary key, auto-increment). {@code null} sebelum baris disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id ID baris. Umumnya tidak perlu diisi manual -- diisi DB via {@code IDENTITY}. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama promo yang tampil di kasir/struk/laporan. Wajib diisi ({@code NOT NULL} di DB). */
	@Column(name = "nama_aturan", nullable = false, length = 255)
	public String getNamaAturan() {
		return namaAturan;
	}

	/** @param namaAturan nama promo. */
	public void setNamaAturan(String namaAturan) {
		this.namaAturan = namaAturan;
	}

	/** @return catatan bebas admin (tidak tampil ke pembeli). */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan bebas admin. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	// Relasi Produk (OPSIONAL -- null = berlaku semua produk)
	/**
	 * @return produk sasaran aturan ini, atau {@code null} bila berlaku untuk
	 *         semua produk (relasi lazy {@code @ManyToOne}; dilewatkan
	 *         {@link GeneralValueObject#check(Object)} agar proxy Hibernate
	 *         lazy yang belum terinisialisasi diresolusi dgn aman, memakai
	 *         cache identity map bila tersedia).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = true)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	/** @param produk produk sasaran; {@code null} = berlaku semua produk. */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	// Relasi Toko (OPSIONAL)
	/** @return toko/kios sasaran aturan ini, atau {@code null} bila berlaku di semua toko/kios. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/** @param toko toko/kios sasaran; {@code null} = berlaku semua toko/kios. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/** @return {@code true} bila aturan berlaku untuk semua member; {@code null} tersimpan dikembalikan sebagai {@code false} (BERBEDA dari default fail-open field {@code aktif}/{@code potonganLangsung} di class ini -- di sini {@code null} berarti TIDAK publik, harus cek jenis/tipe anggota). */
	@Column(name = "berlaku_semua_member")
	public Boolean getBerlakuSemuaMember() {
		return berlakuSemuaMember == null ? false : berlakuSemuaMember;
	}

	/** @param berlakuSemuaMember flag berlaku semua member. */
	public void setBerlakuSemuaMember(Boolean berlakuSemuaMember) {
		this.berlakuSemuaMember = berlakuSemuaMember;
	}

	// Relasi Jenis Anggota (OPSIONAL)
	/** @return jenis anggota koperasi sasaran tunggal (mis. Guru/Siswa), atau {@code null} bila tidak dibatasi jenis. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_anggota", nullable = true)
	public JenisAnggotaKoperasi getJenisAnggota() {
		jenisAnggota = check(jenisAnggota);
		return jenisAnggota;
	}

	/** @param jenisAnggota jenis anggota koperasi sasaran. */
	public void setJenisAnggota(JenisAnggotaKoperasi jenisAnggota) {
		this.jenisAnggota = jenisAnggota;
	}

	// Relasi Tipe Anggota (OPSIONAL)
	/** @return tipe anggota koperasi sasaran tunggal (mis. VIP/Reguler), atau {@code null} bila tidak dibatasi tipe. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_anggota", nullable = true)
	public TipeAnggotaKoperasi getTipeAnggota() {
		tipeAnggota = check(tipeAnggota);
		return tipeAnggota;
	}

	/** @param tipeAnggota tipe anggota koperasi sasaran. */
	public void setTipeAnggota(TipeAnggotaKoperasi tipeAnggota) {
		this.tipeAnggota = tipeAnggota;
	}

	/**
	 * @return persentase diskon (mis. {@code 10.0} = 10%); {@code null} tersimpan
	 *         dikembalikan sebagai {@code 0.0}. Bila {@code > 0}, mesin hitung
	 *         di {@code KantinHelper} MEMPRIORITASKAN field ini drpd
	 *         {@link #getNominal()} -- lihat rumus lengkap di javadoc kelas.
	 */
	@Column(name = "persentase")
	public Double getPersentase() {
		return persentase == null ? 0.0 : persentase;
	}

	/** @param persentase persentase diskon. */
	public void setPersentase(Double persentase) {
		this.persentase = persentase;
	}

	/**
	 * @return batas atas nominal potongan (Rupiah); {@code 0}/{@code null} =
	 *         tanpa batas per-transaksi. Bila {@link #getBerlakuPerHariDanPerToko()}
	 *         true, nilai ini diperlakukan sbg KUOTA HARIAN per toko, bukan
	 *         batas per-transaksi -- lihat javadoc kelas.
	 */
	@Column(name = "maksimal_potongan")
	public Double getMaksimalPotongan() {
		return maksimalPotongan == null ? 0.0 : maksimalPotongan;
	}

	/** @param maksimalPotongan batas atas nominal potongan. */
	public void setMaksimalPotongan(Double maksimalPotongan) {
		this.maksimalPotongan = maksimalPotongan;
	}

	/**
	 * @return diskon nominal tetap per unit (Rupiah); {@code null} tersimpan
	 *         dikembalikan sebagai {@code 0.0}. Dipakai hanya bila
	 *         {@link #getPersentase()} tidak diisi/{@code <= 0}; hasil kali
	 *         dengan jumlah dipotong (clamp) agar tidak melebihi basis
	 *         perhitungan item.
	 */
	@Column(name = "nominal")
	public Double getNominal() {
		return nominal == null ? 0.0 : nominal;
	}

	/** @param nominal diskon nominal tetap per unit. */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/**
	 * @return prioritas urutan evaluasi; angka LEBIH BESAR dihitung LEBIH
	 *         DAHULU saat beberapa aturan cocok untuk item yang sama. Default
	 *         {@code 100} bila {@code null} (kolom {@code NOT NULL} di DB,
	 *         tapi getter tetap fail-safe bila entity belum lengkap terisi).
	 */
	@Column(name = "prioritas", nullable = false)
	public Integer getPrioritas() {
		return prioritas == null ? 100 : prioritas;
	}

	/** @param prioritas prioritas urutan evaluasi. */
	public void setPrioritas(Integer prioritas) {
		this.prioritas = prioritas;
	}

	/**
	 * @return {@code true} bila aturan ini boleh ditumpuk dengan aturan lain
	 *         yang JUGA mengizinkan penggabungan pada item yang sama; default
	 *         aman {@code false} bila {@code null} (tidak ditumpuk, mencegah
	 *         diskon berganda yang tidak disengaja).
	 */
	@Column(name = "dapat_digabung", nullable = false)
	public Boolean getDapatDigabung() {
		return dapatDigabung == null ? false : dapatDigabung;
	}

	/** @param dapatDigabung flag boleh ditumpuk dengan aturan lain. */
	public void setDapatDigabung(Boolean dapatDigabung) {
		this.dapatDigabung = dapatDigabung;
	}

	/**
	 * @return basis nominal perhitungan persentase: {@code "SETELAH_DISKON"}
	 *         (default, dari sisa harga setelah diskon lain yang sudah
	 *         ditumpuk) atau {@code "HARGA_AWAL"} (selalu dari harga utuh
	 *         sebelum diskon apa pun). Nilai {@code null}/kosong dikembalikan
	 *         sebagai default {@code "SETELAH_DISKON"}. Lihat rumus lengkap
	 *         di javadoc kelas.
	 */
	@Column(name = "dasar_perhitungan", nullable = false, length = 30)
	public String getDasarPerhitungan() {
		return dasarPerhitungan == null || dasarPerhitungan.trim().isEmpty()
				? "SETELAH_DISKON" : dasarPerhitungan;
	}

	/** @param dasarPerhitungan basis perhitungan persentase ({@code "SETELAH_DISKON"} atau {@code "HARGA_AWAL"}). */
	public void setDasarPerhitungan(String dasarPerhitungan) {
		this.dasarPerhitungan = dasarPerhitungan;
	}

	/**
	 * @return kode grup eksklusif; aturan lain dengan kode SAMA (tidak kosong)
	 *         tidak boleh dipakai bersamaan pada item yang sama saat
	 *         penumpukan. {@code null}/kosong = tidak ada pembatasan
	 *         eksklusivitas kode ini.
	 */
	@Column(name = "grup_eksklusif", length = 100)
	public String getGrupEksklusif() {
		return grupEksklusif;
	}

	/** @param grupEksklusif kode grup eksklusif. */
	public void setGrupEksklusif(String grupEksklusif) {
		this.grupEksklusif = grupEksklusif;
	}

	// Menentukan proses eksekusi (Langsung potong nota ATAU masuk tabungan saldo)
	/**
	 * @return {@code true} (default bila {@code null}) = nilai promo memotong
	 *         harga struk langsung (akumulator {@code diskon} di mesin hitung).
	 *         {@code false} = nilai promo masuk sebagai saldo cashback member,
	 *         TIDAK mengurangi total tagihan saat checkout, dan dicairkan
	 *         belakangan lewat {@link PencairanDiskon}.
	 */
	@Column(name = "potongan_langsung", nullable = false)
	public Boolean getPotonganLangsung() {
		return potonganLangsung == null ? true : potonganLangsung; // Default memotong harga awal
	}

	/** @param potonganLangsung flag potong harga langsung vs masuk saldo cashback. */
	public void setPotonganLangsung(Boolean potonganLangsung) {
		this.potonganLangsung = potonganLangsung;
	}

	/** @return tanggal mulai berlaku (inklusif); {@code null} = tanpa batas mulai. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_mulai")
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/** @param tanggalMulai tanggal mulai berlaku. */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/** @return tanggal berakhir berlaku (inklusif); {@code null} = tanpa batas akhir. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/** @param tanggalSelesai tanggal berakhir berlaku. */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Gap-closure "Promo Pilih Hari" -- CSV angka hari ISO-8601 ({@code 1}=Senin .. {@code 7}=Minggu,
	 * mis. {@code "1,2,3,4,5"} utk Senin-Jumat). {@code null}/kosong = berlaku SEMUA hari (tanpa
	 * batasan), konsisten dgn konvensi {@link #getTanggalMulai()}/{@link #getTanggalSelesai()} null =
	 * tanpa batas. Dicek lewat {@link ais.common.HariAktifUtil#aktifPadaHari(String, Date)} dari DUA
	 * mesin pencocokan promo yang wajib tetap sinkron: {@code PosKantinAction.evaluasiDiskon} (ZK/JSP,
	 * checkout langsung) dan {@code KantinHelper.evaluasiDiskonItems} (API, dipakai Electron/Flutter).
	 */
	@Column(name = "hari_aktif", length = 20)
	public String getHariAktif() {
		return hariAktif;
	}

	public void setHariAktif(String hariAktif) {
		this.hariAktif = hariAktif;
	}

	/** @return status aktif baris; {@code null} tersimpan diperlakukan sebagai {@code TRUE} (fail-open). Baris tidak aktif dikecualikan dari kelayakan mesin hitung. */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif baris. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@code true} bila aturan ini DIKECUALIKAN dari auto-apply saat
	 *         checkout dan hanya aktif bila member/kasir memilihnya secara
	 *         manual lewat picker promo (dicocokkan via {@link #getId()}).
	 *         Default {@code false} bila {@code null} (perilaku umum:
	 *         auto-apply, aturan pertama yang lolos kelayakan langsung dipakai).
	 */
	@Column(name = "aktivasi_manual")
	public Boolean getAktivasiManual() {
		return aktivasiManual == null ? false : aktivasiManual;
	}

	/** @param aktivasiManual flag aktivasi manual (bukan auto-apply). */
	public void setAktivasiManual(Boolean aktivasiManual) {
		this.aktivasiManual = aktivasiManual;
	}

	/**
	 * @return pengguna yang mengunci baris ini (mekanisme kunci-baris yang
	 *         diwarisi dari {@link VoKunci}), atau {@code null} bila tidak
	 *         dikunci. Relasi lazy, diresolusi lewat
	 *         {@link GeneralValueObject#check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci", nullable = true)
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/** @param dikunci pengguna pengunci baris ini; {@code null} = lepas kunci. */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * @return tautan disposisi SOP (workflow persetujuan) terkait baris ini,
	 *         atau {@code null} bila tidak/belum ditautkan ke SOP. Relasi
	 *         lazy, diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/** @param disposisiSop tautan disposisi SOP terkait baris ini. */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		this.disposisiSop = disposisiSop;
	}

	// TRUE = Hanya berlaku tiap hari 1x dan per toko. FALSE = berlaku semua
	/**
	 * @return {@code true} bila {@link #getMaksimalPotongan()} diperlakukan
	 *         sebagai KUOTA HARIAN per toko (akumulasi pemakaian lintas
	 *         transaksi hari itu dikurangi dari sisa kuota, bukan batas
	 *         per-transaksi yang direset tiap checkout). Default {@code false}
	 *         bila {@code null} (batas berlaku penuh tiap transaksi).
	 */
	@Column(name = "berlaku_per_hari_dan_per_toko")
	public Boolean getBerlakuPerHariDanPerToko() {
		return berlakuPerHariDanPerToko == null ? false : berlakuPerHariDanPerToko;
	}

	/** @param berlakuPerHariDanPerToko flag kuota harian per toko vs batas per-transaksi. */
	public void setBerlakuPerHariDanPerToko(Boolean berlakuPerHariDanPerToko) {
		this.berlakuPerHariDanPerToko = berlakuPerHariDanPerToko;
	}

}
