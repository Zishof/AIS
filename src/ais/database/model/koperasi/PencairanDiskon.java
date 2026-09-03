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

import ais.database.model.GeneralValueObject;
import ais.database.model.inventory.Toko;

/**
 * Riwayat transaksi PENCAIRAN saldo diskon/cashback member koperasi -- baris
 * ini dibuat ketika saldo yang terkumpul (dari {@link AturanDiskon}/
 * {@link GrupAturanDiskon} yang di-set {@code potonganLangsung = false},
 * sehingga nilai promo TIDAK memotong harga struk melainkan dikreditkan ke
 * saldo cashback member) ditarik/dicairkan oleh member, baik tunai, transfer,
 * maupun cara lain via {@link #getCaraPembayaran()}.
 *
 * <h3>Saldo dihitung dinamis, bukan disimpan sebagai kolom</h3>
 * Tidak ada kolom "saldo berjalan" di {@code koperasi.anggota_koperasi} atau di
 * mana pun -- sisa saldo cashback yang BOLEH dicairkan selalu dihitung ulang
 * saat dibutuhkan: {@code SUM(koperasi.pembelian_anggota_koperasi.totalcashback)}
 * milik member DIKURANGI {@code SUM(koperasi.pencairan_diskon.nominal_cair)}
 * milik member yang berstatus {@code BERHASIL} (dan, tergantung jalur baca,
 * juga {@code PENDING} -- lihat catatan validasi di bawah). Konsekuensinya:
 * baris {@link PembelianAnggotaKoperasi} adalah SUMBER kredit saldo, baris
 * class ini adalah SUMBER debit saldo; keduanya harus dibaca bersama untuk
 * tahu saldo riil member kapan pun.
 *
 * <h3>Dua jalur tulis, HANYA SATU yang tervalidasi saldo</h3>
 * <ul>
 * <li>{@code KantinHelper.pencairanDiskonSimpan} (API mobile/Flutter) memanggil
 * helper privat {@code pencairanDiskonSisaSaldo} SEBELUM menyimpan, menolak
 * (status {@code "91"}) bila {@link #getNominalCair()} melebihi sisa saldo
 * riil member -- didokumentasikan di kode sumber sbg "sumber kebenaran akhir
 * aksi finansial", dilewati hanya saat status yang dipilih {@code DITOLAK}
 * (uang tidak sungguh keluar).</li>
 * <li>{@code PencairanDiskonAction} (layar CRUD ZK admin/kasir, yang menurut
 * javadoc kelasnya sendiri dipakai staf untuk "menyetujui/menolak permintaan
 * pencairan") TIDAK memanggil validasi saldo apa pun di {@code onSave} --
 * hanya memastikan member, cara pembayaran, dan nominal {@code > 0} terisi.
 * Artinya staf pemegang akses layar ini dapat membuat/menyetujui baris dengan
 * {@link #getNominalCair()} berapa pun (melebihi saldo riil sekalipun) dan
 * langsung men-set {@link #getStatus()} ke {@code "BERHASIL"} -- begitu
 * BERHASIL, {@code PostingDanaAnggotaUtil} menjurnalkan nominal itu sbg beban
 * riil ke buku besar (lihat {@link #getPostingHistory()}). Ini adalah CELAH
 * validasi yang nyata (dicatat terpisah utk perbaikan, BUKAN diperbaiki lewat
 * javadoc ini), bukan sekadar potensi risiko.</li>
 * </ul>
 *
 * <h3>Status dan efek finansial</h3>
 * {@link #getStatus()} bernilai {@code PENDING} (default, permintaan belum
 * diproses -- tapi tetap MENGURANGI sisa saldo yg dihitung API krn dianggap
 * "sudah dijanjikan"), {@code BERHASIL} (uang benar-benar keluar, memicu
 * posting jurnal), atau {@code DITOLAK} (dibatalkan, tidak memotong saldo
 * sama sekali menurut query saldo di atas krn hanya {@code BERHASIL}/
 * {@code PENDING} yang dihitung sbg pengurang).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pencairan_diskon")
public class PencairanDiskon extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** Nomor referensi/struk pencairan, mis. {@code "WD-20260301-001"}; unik di DB. Diisi otomatis ({@code "WD-" + waktu}) bila dikosongkan oleh pemanggil. */
	private String kodePencairan;

	// --- RELASI UTAMA ---
	/** WAJIB: member yang mencairkan saldo. */
	private AnggotaKoperasi anggotaKoperasi;
	/** OPSIONAL: toko/kios tempat pencairan dilakukan (bila ditarik lewat kasir); {@code null} bila tidak lewat kasir toko tertentu. */
	private Toko toko;
	/** WAJIB: cara pencairan dilakukan (Tunai, Transfer Bank, Potong Belanja, dll). */
	private CaraPembayaranKoperasi caraPembayaran;

	// --- DATA NOMINAL & WAKTU ---
	/** Jumlah saldo (Rupiah) yang ditarik/dicairkan pada baris ini. WASPADA: lihat catatan "Dua jalur tulis" di javadoc kelas -- tidak semua jalur tulis memvalidasi field ini terhadap sisa saldo riil member. */
	private Double nominalCair;
	/** Tanggal dan jam eksekusi pencairan; {@code null} tersimpan dikembalikan sebagai waktu saat ini oleh {@link #getWaktuPencairan()}. */
	private Date waktuPencairan;
	/** Tanggal kedaluwarsa BILA baris ini berupa topup (bukan pencairan biasa); boleh kosong. */
	private Date tanggalExpiredJikaBerupaTopup;

	// --- STATUS & KETERANGAN ---
	/** Status baris: {@code PENDING} (default), {@code BERHASIL}, atau {@code DITOLAK} -- lihat efek finansial tiap status di javadoc kelas. */
	private String status;
	/** Catatan bebas, mis. {@code "Dicairkan tunai oleh kasir A"}, {@code "Ditransfer ke BCA"}. */
	private String keterangan;

	// --- AUDIT TRAIL ---
	/** Nama/username pengguna yang terakhir mengubah baris ini. Setter mengabaikan nilai kosong/{@code null} -- lihat {@link #setOleh(String)}. */
	private String oleh;
	/** ID pengguna yang terakhir mengubah baris ini. Setter mengabaikan nilai kosong/{@code null} -- lihat {@link #setOlehId(String)}. */
	private String olehId;

	/** @return ID pengguna yang terakhir mengubah baris ini (audit trail). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId ID pengguna pengubah. Nilai {@code null}/kosong DIABAIKAN
	 *               (nilai lama dipertahankan) -- pola yang sama dipakai
	 *               {@link #setOleh(String)} dan {@link AturanDiskon#setOlehId(String)},
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
	 * lain di seluruh basis kode.
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
	public PencairanDiskon() {
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

	/**
	 * @return nomor referensi/struk pencairan (kolom {@code UNIQUE NOT NULL}
	 *         di DB, mis. {@code "WD-20260301-001"}). Keunikan ini adalah
	 *         satu-satunya jaring pengaman terhadap duplikasi baris di jalur
	 *         API ({@code KantinHelper.pencairanDiskonSimpan} menangkap
	 *         {@code ConstraintViolationException} dan mengembalikan pesan
	 *         "nomor referensi sudah dipakai") -- BUKAN pengaman terhadap
	 *         pencairan berulang dgn kode BERBEDA utk saldo yang sama (itu
	 *         tugas validasi sisa saldo, lihat javadoc kelas).
	 */
	@Column(name = "kode_pencairan", unique = true, nullable = false, length = 100)
	public String getKodePencairan() {
		return kodePencairan;
	}

	/** @param kodePencairan nomor referensi/struk pencairan; wajib unik. */
	public void setKodePencairan(String kodePencairan) {
		this.kodePencairan = kodePencairan;
	}

	/**
	 * @return member yang mencairkan saldo (relasi lazy {@code @ManyToOne},
	 *         WAJIB diisi -- kolom {@code NOT NULL}). Diresolusi lewat
	 *         {@link GeneralValueObject#check(Object)} agar proxy Hibernate
	 *         lazy yang belum terinisialisasi aman dipakai.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	/** @param anggotaKoperasi member yang mencairkan saldo; wajib diisi. */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/** @return toko/kios tempat pencairan dilakukan lewat kasir, atau {@code null} bila tidak lewat toko tertentu (mis. pencairan admin/transfer langsung). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/** @param toko toko/kios tempat pencairan; {@code null} bila tidak lewat toko tertentu. */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/** @return cara pencairan dilakukan (Tunai, Transfer Bank, Potong Belanja, dll). Relasi lazy, WAJIB diisi. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran", nullable = false)
	public CaraPembayaranKoperasi getCaraPembayaran() {
		caraPembayaran = check(caraPembayaran);
		return caraPembayaran;
	}

	/** @param caraPembayaran cara pencairan; wajib diisi. */
	public void setCaraPembayaran(CaraPembayaranKoperasi caraPembayaran) {
		this.caraPembayaran = caraPembayaran;
	}

	/**
	 * @return nominal (Rupiah) yang dicairkan pada baris ini; {@code null}
	 *         tersimpan dikembalikan sebagai {@code 0.0}. Lihat javadoc kelas
	 *         (bagian "Dua jalur tulis") untuk celah validasi yang perlu
	 *         diwaspadai saat membaca/mempercayai nilai ini dari baris yang
	 *         dibuat lewat layar admin ZK.
	 */
	@Column(name = "nominal_cair", nullable = false)
	public Double getNominalCair() {
		return nominalCair == null ? 0.0 : nominalCair;
	}

	/** @param nominalCair nominal yang dicairkan. */
	public void setNominalCair(Double nominalCair) {
		this.nominalCair = nominalCair;
	}

	/** @return tanggal dan jam eksekusi pencairan; {@code null} tersimpan dikembalikan sebagai waktu SAAT PEMANGGILAN (bukan waktu penyimpanan baris) -- getter ini TIDAK idempotent bila field belum diisi. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_pencairan", nullable = false)
	public Date getWaktuPencairan() {
		return waktuPencairan == null ? ais.ui.util.WaktuUtil.getDate() : waktuPencairan;
	}

	/** @param waktuPencairan tanggal dan jam eksekusi pencairan. */
	public void setWaktuPencairan(Date waktuPencairan) {
		this.waktuPencairan = waktuPencairan;
	}

	/** @return status baris: {@code PENDING} (default bila {@code null}), {@code BERHASIL}, atau {@code DITOLAK} -- lihat efek finansial tiap status di javadoc kelas. */
	@Column(name = "status", length = 50)
	public String getStatus() {
		return status == null ? "PENDING" : status;
	}

	/**
	 * @param status status baru baris ini. TIDAK ada validasi transisi status
	 *               di level entity (mis. {@code BERHASIL} tidak bisa
	 *               otomatis dibalik ke {@code PENDING}/{@code DITOLAK}
	 *               setelah jurnal terposting) -- disiplin alur kerja
	 *               sepenuhnya ada di pemanggil ({@code KantinHelper}/
	 *               {@code PencairanDiskonAction}).
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return catatan bebas terkait pencairan ini. */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan bebas. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return tanggal kedaluwarsa bila baris ini berupa topup (bukan pencairan biasa); {@code null} = tidak berlaku/tidak ada batas. */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_expired_jika_berupa_topup")
	public Date getTanggalExpiredJikaBerupaTopup() {
		return tanggalExpiredJikaBerupaTopup;
	}

	/** @param tanggalExpiredJikaBerupaTopup tanggal kedaluwarsa topup. */
	public void setTanggalExpiredJikaBerupaTopup(Date tanggalExpiredJikaBerupaTopup) {
		this.tanggalExpiredJikaBerupaTopup = tanggalExpiredJikaBerupaTopup;
	}


	/** Riwayat posting jurnal (dok 61 butir B); {@code null} = belum/tidak diposting. Lihat javadoc getter {@link #getPostingHistory()}. */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal (dok 61 butir B): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} menjurnalkan dokumen ini. Sebelumnya perputaran dana
	 * anggota tidak pernah menyentuh buku besar sama sekali.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/** @param postingHistory riwayat posting jurnal terkait baris ini; diisi oleh {@code PostingDanaAnggotaUtil}, bukan alur normal pemanggil lain. */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}