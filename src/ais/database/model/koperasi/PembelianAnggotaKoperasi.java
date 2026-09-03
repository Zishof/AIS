package ais.database.model.koperasi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.List;

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
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.Lokasi;
import ais.database.model.inventory.DraftPembelian;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.SesiKasKasir;
import ais.database.model.inventory.Toko;

/**
 * <h2>PembelianAnggotaKoperasi — Header Struk Belanja / Transaksi Kasir Koperasi</h2>
 *
 * <p>
 * Entity ini adalah <b>kepala (header) satu struk belanja</b> di toko/kantin koperasi: satu baris
 * di tabel {@code koperasi.pembelian_anggota_koperasi} sama dengan satu kali "bayar" di kasir.
 * Rincian barangnya <b>tidak</b> disimpan di sini melainkan sebagai kumpulan baris
 * {@link ais.database.model.inventory.Pembelian} yang menunjuk balik ke header ini lewat properti
 * {@code pembelianAnggotaKoperasi}. Jadi hubungannya satu-header-banyak-baris, dan header inilah
 * yang memegang seluruh angka uang tingkat struk (total, diskon, pajak, tunai/non-tunai, kembalian)
 * beserta konteks operasionalnya (toko, lokasi, meja kantin, sesi kas, mesin POS, kasir).
 * </p>
 *
 * <h3>Bedanya dengan {@code TransaksiKoperasi}</h3>
 * <p>
 * Keduanya sama-sama berada di skema {@code koperasi} dan sama-sama menyangkut uang seorang
 * {@link AnggotaKoperasi}, tetapi <b>domainnya berbeda</b> dan tidak saling menggantikan:
 * </p>
 * <ul>
 * <li><b>{@code PembelianAnggotaKoperasi} (kelas ini)</b> — sisi <i>ritel</i>: belanja barang di
 * kasir/POS. Turunannya adalah baris {@code inventory.Pembelian} yang menunjuk
 * {@link ais.database.model.inventory.Produk}, jadi ia bergerak bersama stok barang, toko, dan sesi
 * kas kasir. Alur pembuatannya dimulai dari {@code KantinHelper.bayar()} /
 * {@code PosApi} / {@code TopupHelper}, bukan dari layar simpan-pinjam.</li>
 * <li><b>{@code TransaksiKoperasi}</b> — sisi <i>simpan-pinjam/keuangan anggota</i>: dokumen yang
 * rinciannya berupa angsuran, pokok, dan margin, bukan produk.</li>
 * </ul>
 * <p>
 * Yang menghubungkan keduanya secara tidak langsung adalah <b>saldo deposit anggota</b>:
 * {@code DepositHelper.hitungDeposit(AnggotaKoperasi)} menjumlahkan topup dari sisi keuangan lalu
 * <b>mengurangi</b>nya dengan belanja dari header kelas ini — lihat
 * {@link #getNominalBayar1()} untuk aturan penjumlahan lima slot metode bayarnya.
 * </p>
 *
 * <h3>Dua jalur pembuatan: checkout langsung dan finalisasi draft</h3>
 * <p>
 * Sebuah header bisa lahir lewat dua jalur yang <b>sangat</b> berbeda perilakunya, dan pembedanya
 * adalah apakah {@link #getDraftPembelianAnggotaKoperasi()} terisi:
 * </p>
 * <ol>
 * <li><b>Checkout langsung</b> (draft {@code null}) — kasir memindai barang lalu langsung membayar.
 * Rincian barang datang sebagai {@code JSONArray} dari klien POS dan diubah menjadi baris
 * {@code Pembelian} oleh {@link #simpanRinci(Session, JSONArray, String, Date, Toko,
 * KodePembayaranOnline, DraftPembelianAnggotaKoperasi)}.</li>
 * <li><b>Finalisasi draft</b> (draft terisi) — pesanan sudah dicatat lebih dulu sebagai
 * {@link DraftPembelianAnggotaKoperasi} (mis. pesanan kantin/meja yang dibayar belakangan), lalu
 * baris {@link DraftPembelian}-nya "dipromosikan" jadi baris {@code Pembelian} definitif.</li>
 * </ol>
 * <p>
 * <b>Konsekuensi penting jalur kedua:</b> begitu draft terpasang, sekumpulan getter di kelas ini
 * berhenti membaca kolomnya sendiri dan <b>menyalin ulang</b> nilai dari draft setiap kali dipanggil
 * — lihat {@link #getAnggotaKoperasi()}, {@link #getLokasi()}, {@link #getToko()},
 * {@link #getTbmuser()}, {@link #getMejaKantin()}, {@link #getTanggalPembayaran()}, dan
 * {@link #getKodePembayaranOnline()}. Draft, bukan header, yang menjadi sumber kebenaran untuk
 * konteks transaksi. Ini <b>pola getter destruktif</b> yang berulang di domain finansial AIS: nilai
 * hasil {@code setXxx(...)} bisa hilang diam-diam pada pembacaan berikutnya, dan karena getter yang
 * sama juga dipakai Hibernate saat menulis, nilai turunan itu ikut tersimpan ke kolom header.
 * </p>
 *
 * <h3>Angka uang di header ini</h3>
 * <ul>
 * <li>{@link #getBiaya()} dan {@link #getTotalBiaya()} — keduanya diisi dengan angka total yang sama
 * oleh {@code KantinHelper}; {@code totalBiaya} yang dipakai seluruh perhitungan turunan.</li>
 * <li>{@link #getBayarTunai()} + {@link #getBayarNonTunai()} — pemecahan uang yang diterima; jumlah
 * keduanya dilaporkan lewat {@link #getBayar()} dan menjadi dasar {@link #getLunas()}.</li>
 * <li>{@link #getCaraPembayaranKoperasi()} sampai {@link #getCaraPembayaranKoperasi5()} plus
 * {@link #getNominalBayar2()}…{@link #getNominalBayar5()} — <b>split pembayaran</b> hingga lima
 * metode dalam satu struk; nominal slot 1 sengaja tidak punya kolom sendiri.</li>
 * <li>{@link #getTotalDiskon()}, {@link #getTotalCashback()}, {@link #getPpn()},
 * {@link #getHargaPpn()}, {@link #getPajak()}, {@link #getRetur()}, {@link #getKembalian()} —
 * angka pelengkap struk.</li>
 * </ul>
 *
 * <h3>Jejak audit dan kemampuan telusur</h3>
 * <p>
 * Kelas ini {@code @Audited} (Hibernate Envers), sehingga tiap perubahan barisnya tersimpan sebagai
 * revisi dan bisa dibuka lewat {@code RevisiApiHelper}. Di luar itu ia membawa beberapa
 * <b>snapshot teks</b> yang sengaja tidak mengandalkan relasi, supaya laporan lama tetap benar
 * meski master datanya berubah kemudian: {@link #getKasirLoginNama()},
 * {@link #getNamaMesin()}, {@link #getIdPerangkat()}, dan
 * {@link #getDetailPembelianCadangan()}. Ditambah {@link #getSumberTransaksi()} dan
 * {@link #getWaktuSinkron()} untuk membedakan transaksi yang sempat mengendap offline di perangkat
 * kasir.
 * </p>
 *
 * <h3>Catatan tenancy</h3>
 * <p>
 * Berbeda dari {@link CalonAnggotaKoperasi} atau {@link AnggotaKoperasi}, header ini <b>tidak</b>
 * memiliki kolom {@code koperasi} sendiri. Pemisahan antar penyewa/unit bergantung sepenuhnya pada
 * {@link #getToko()} / {@link #getLokasi()} — yang keduanya {@code nullable}. Setiap query laporan
 * atau API yang mengambil data kelas ini karena itu <b>wajib</b> memasang penyaring toko/lokasinya
 * sendiri; tidak ada penyaring bawaan pada tingkat entity yang menahannya.
 * </p>
 *
 * @see DraftPembelianAnggotaKoperasi dokumen draft yang difinalisasi menjadi header ini
 * @see ais.database.model.inventory.Pembelian baris rincian barang milik header ini
 * @see AnggotaKoperasi pemilik/pembeli (boleh {@code null} untuk pembeli umum non-anggota)
 * @see ais.database.model.inventory.SesiKasKasir sesi kas yang menampung uangnya
 * @see PembatalanTransaksiKantin dokumen pembatalan atas header ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembelian_anggota_koperasi")
public class PembelianAnggotaKoperasi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan beberapa entity koperasi
	 * lain hasil pembangkitan Hibernate Tools; jangan diubah karena entity ini ikut diserialisasi ke
	 * dalam sesi ZK dan cache.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (identity/auto-increment). Lihat {@link #getId()}. */
	private Long id;
	/** Nomor urut tampilan sementara, tidak dipetakan ke kolom. Lihat {@link #getIndex()}. */
	private Long index;
	/** Id pengguna yang terakhir mengubah baris ini (field bayangan audit). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang tercatat terakhir kali mengubah baris ini.
	 *
	 * <p>
	 * Ini <b>field bayangan audit</b> yang diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan data bisnis. Untuk transaksi
	 * yang masuk lewat {@code PosApi}, nilainya kerap tidak mewakili kasir sebenarnya karena aplikasi
	 * POS tidak memakai sesi browser — gunakan {@link #getKasirLoginNama()} atau
	 * {@link #getTbmuser()} bila yang dicari adalah identitas kasir.
	 * </p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p>
	 * Setter ini <b>menolak nilai kosong</b>: {@code null} maupun string yang hanya berisi spasi
	 * diabaikan diam-diam sehingga nilai lama dipertahankan. Perilaku ini disengaja — jejak audit
	 * yang sudah ada tidak boleh terhapus oleh proses yang kebetulan tidak mengetahui pelakunya
	 * (mis. penulisan lewat kanal API). Nilai tak-kosong tetap boleh menimpa nilai tak-kosong
	 * sebelumnya, karena yang dicatat memang pengubah <i>terakhir</i>, bukan pembuat.
	 * </p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini (field bayangan audit). Lihat {@link #getOleh()}. */
	private String oleh;

	/**
	 * Representasi teks header ini, yaitu {@link #getKode()} apa adanya (nomor struk).
	 *
	 * <p>
	 * Dipakai komponen ZK (mis. combobox/listbox) sebagai label baris. Sengaja tidak menambahkan
	 * nominal atau nama pembeli supaya tetap ringkas dan tidak memicu pemuatan relasi malas.
	 * Mengembalikan {@code null} bila kode belum terisi.
	 * </p>
	 *
	 * @return nomor struk transaksi ini
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>
	 * Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan agar jejak audit
	 * yang sudah ada tidak terhapus. Untuk permintaan yang datang lewat {@code PosApi}, nilai yang
	 * tersimpan biasanya bukan nama kasir melainkan nilai cadangan {@code "external_update"} —
	 * lihat penjelasan lengkapnya di {@link #getKasirLoginNama()}.
	 * </p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang tercatat terakhir kali mengubah baris ini (field bayangan audit).
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see #getKasirLoginNama() identitas kasir sebenarnya untuk transaksi POS
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang berjalan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>
	 * Mendelegasikan pengisian {@link #getOleh()}, {@link #getOlehId()}, dan
	 * {@link #getTanggal_dirubah()} ke {@code AuditTimestampInterceptor.ubah(Object)} sehingga tiga
	 * field bayangan audit itu selalu konsisten tanpa perlu diisi manual di tiap pemanggil. Hanya
	 * dipicu pada pembaruan, bukan penyisipan baris baru.
	 * </p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Cap waktu perubahan terakhir (field bayangan audit). Lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir.
	 *
	 * <p>
	 * Umumnya tidak dipanggil langsung; {@link #onUpdate()} yang mengisinya lewat interceptor audit.
	 * </p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu baris ini terakhir diubah (field bayangan audit, bukan waktu transaksi).
	 *
	 * <p>
	 * Jangan dipakai sebagai tanggal transaksi — untuk itu gunakan {@link #getTanggalPembayaran()},
	 * dan untuk waktu penerimaan di server pada transaksi offline gunakan {@link #getWaktuSinkron()}.
	 * Nilai awalnya waktu objek dibuat di memori, lalu ditimpa interceptor pada tiap pembaruan.
	 * </p>
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nilai {@link #sumberTransaksi} bila transaksi masuk lewat jalur online normal (kasir terhubung saat bayar). */
	public static final String SUMBER_ONLINE = "ONLINE";
	/** Nilai {@link #sumberTransaksi} bila transaksi awalnya disimpan offline di perangkat kasir (IndexedDB) lalu masuk ke server lewat antrian sinkronisasi ({@code pos_offline_service.jsp}). */
	public static final String SUMBER_OFFLINE_SYNC = "OFFLINE_SYNC";

	/** Nomor struk, unik per transaksi. Lihat {@link #getKode()}. */
	private String kode;
	/** Catatan bebas kasir/supervisor atas struk ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Asal-usul transaksi: online atau hasil sinkronisasi offline. Lihat {@link #getSumberTransaksi()}. */
	private String sumberTransaksi = SUMBER_ONLINE;
	/** Waktu server menerima transaksi offline. Lihat {@link #getWaktuSinkron()}. */
	private Date waktuSinkron;
	/** Anggota koperasi pembeli; {@code null} untuk pembeli umum. Lihat {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Meja kantin tempat pesanan disajikan. Lihat {@link #getMejaKantin()}. */
	private MejaKantin mejaKantin;
	/** Lokasi/outlet tempat transaksi terjadi; menjadi asal {@link #getToko()}. Lihat {@link #getLokasi()}. */
	private Lokasi lokasi;
	/** Waktu pembeli membayar (waktu bisnis transaksi). Lihat {@link #getTanggalPembayaran()}. */
	private Date tanggalPembayaran = new Date();
	/** Metode pembayaran slot 1; nominalnya implisit. Lihat {@link #getCaraPembayaranKoperasi()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi;
	// Split pembayaran (mis. Rp50rb Transfer + Rp50rb Tunai untuk satu transaksi Rp100rb): metode
	// PERTAMA yang dipilih kasir selalu masuk ke caraPembayaranKoperasi (slot 1) di atas, dengan
	// nominalnya DIHITUNG IMPLISIT = totalBiaya - (nominalBayar2+3+4+5) -- BUKAN kolom tersendiri --
	// supaya transaksi lama/satu-metode (nominalBayar2..5 semua 0/null) tetap 100% identik perilakunya
	// tanpa migrasi data apa pun. Slot 2-5 di bawah ini OPSIONAL, dipakai hanya kalau kasir memilih
	// lebih dari satu metode pembayaran (maks 5 total). Lihat DepositHelper.hitungDeposit(AnggotaKoperasi)
	// utk cara kelima slot ini dijumlahkan saat menentukan potongan saldo deposit anggota.
	/** Metode pembayaran slot 2 (opsional). Lihat {@link #getCaraPembayaranKoperasi2()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi2;
	/** Metode pembayaran slot 3 (opsional). Lihat {@link #getCaraPembayaranKoperasi3()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi3;
	/** Metode pembayaran slot 4 (opsional). Lihat {@link #getCaraPembayaranKoperasi4()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi4;
	/** Metode pembayaran slot 5 (opsional). Lihat {@link #getCaraPembayaranKoperasi5()}. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi5;
	/** Nominal yang dibayar lewat metode slot 2. Lihat {@link #getNominalBayar2()}. */
	private Double nominalBayar2 = 0.0;
	/** Nominal yang dibayar lewat metode slot 3. Lihat {@link #getNominalBayar3()}. */
	private Double nominalBayar3 = 0.0;
	/** Nominal yang dibayar lewat metode slot 4. Lihat {@link #getNominalBayar4()}. */
	private Double nominalBayar4 = 0.0;
	/** Nominal yang dibayar lewat metode slot 5. Lihat {@link #getNominalBayar5()}. */
	private Double nominalBayar5 = 0.0;
	/** Kode pembayaran online/QR yang melunasi struk ini. Lihat {@link #getKodePembayaranOnline()}. */
	private KodePembayaranOnline kodePembayaranOnline;
	/** Nilai belanja; diisi sama dengan {@code totalBiaya}. Lihat {@link #getBiaya()}. */
	private Double biaya = 0.0;
	/** Nominal retur atas struk ini. Lihat {@link #getRetur()}. */
	private Double retur = 0.0;
	/** Diskon tingkat struk, angka mentah. Lihat {@link #getDiskon()}. */
	private Double diskon = 0.0;
	/** Penanda apakah {@link #getDiskon()} berupa persen. Lihat {@link #getDiskonDalamPersen()}. */
	private Boolean diskonDalamPersen = false;
	/** Total rupiah diskon yang benar-benar diberikan. Lihat {@link #getTotalDiskon()}. */
	private Double totalDiskon = 0.0;
	/** Total cashback yang dijanjikan struk ini. Lihat {@link #getTotalCashback()}. */
	private Double totalCashback = 0.0;
	/** Persentase PPN yang dipakai. Lihat {@link #getPpn()}. */
	private Double ppn = 0.0;
	/** Dasar pengenaan PPN. Lihat {@link #getHargaPpn()}. */
	private Double hargaPpn = 0.0;
	/** Toko/unit usaha pemilik transaksi; penentu batas tenancy. Lihat {@link #getToko()}. */
	private Toko toko;
	/** Total yang harus dibayar pembeli. Lihat {@link #getTotalBiaya()}. */
	private Double totalBiaya = 0.0;

	/** Bagian pembayaran berupa uang tunai. Lihat {@link #getBayarTunai()}. */
	private Double bayarTunai = 0.0;
	/** Bagian pembayaran non-tunai. Lihat {@link #getBayarNonTunai()}. */
	private Double bayarNonTunai = 0.0;
	/** Uang kembalian yang diserahkan ke pembeli. Lihat {@link #getKembalian()}. */
	private Double kembalian = 0.0;

	/** Total uang diterima; nilai turunan. Lihat {@link #getBayar()}. */
	private Double bayar = 0.0;

	/** Akun kasir yang membuat transaksi. Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;

	/** Status lunas; nilai turunan. Lihat {@link #getLunas()}. */
	private Boolean lunas;

	/** Jejak penjurnalan struk ini ke buku besar. Lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	/** Draft asal bila header ini hasil finalisasi. Lihat {@link #getDraftPembelianAnggotaKoperasi()}. */
	private DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi;

	/** Snapshot nama kasir saat checkout. Lihat {@link #getKasirLoginNama()}. */
	private String kasirLoginNama;

	/** Snapshot nama mesin POS fisik asal transaksi. Lihat {@link #getNamaMesin()}. */
	private String namaMesin;

	/**
	 * Salinan keranjang asli saat checkout dalam format JSON. Nilai ini sengaja
	 * disimpan di header transaksi sebagai bukti pembanding independen terhadap
	 * baris {@code koperasi.pembelian}. Dengan demikian transaksi lama yang
	 * rincianya tidak lengkap masih dapat diaudit tanpa menebak dari struk.
	 */
	private String detailPembelianCadangan;

	/** Sesi kas kasir yang menampung uang transaksi ini. Lihat {@link #getSesiKasKasir()}. */
	private SesiKasKasir sesiKasKasir;
	/** Identitas perangkat POS asal transaksi. Lihat {@link #getIdPerangkat()}. */
	private String idPerangkat;

	/**
	 * Membentuk seluruh <b>baris rincian barang</b> ({@link Pembelian}) milik header ini, lalu
	 * mengembalikan salinannya dalam bentuk {@link JSONArray} untuk dikirim balik ke klien POS
	 * (struk, tampilan konfirmasi, dan antrian cetak).
	 *
	 * <h3>Dua cabang yang sengaja berbeda perilaku</h3>
	 * <p>
	 * Method ini bercabang di baris pertama berdasarkan apakah parameter
	 * {@code draftPembelianAnggotaKoperasi} terisi dan sudah punya id:
	 * </p>
	 * <ol>
	 * <li><b>Cabang finalisasi draft</b> — seluruh {@link DraftPembelian} milik draft tersebut
	 * diambil dan "dipromosikan" menjadi baris {@code Pembelian} definitif. Nilai barangnya
	 * (kode, nama, qty, diskon, aturan diskon, cashback, harga satuan, produk) disalin apa adanya
	 * dari draft, sementara {@code toko}, {@code waktu}, dan {@code kodePembayaranOnline} diambil
	 * dari parameter supaya konteks pembayaran yang berlaku adalah konteks saat <i>dibayar</i>,
	 * bukan saat dipesan. Setiap draft yang berhasil dipromosikan lalu ditandai lunas lewat
	 * {@code draftPembelian.setLunas(pembelian)} sehingga tidak dapat difinalisasi dua kali.</li>
	 * <li><b>Cabang checkout langsung</b> — rincian datang sebagai {@code JSONArray transaksi} dari
	 * klien. Tiap elemen dibaca dengan penjagaan {@code isNull} dan nilai cadangan yang seragam
	 * (kode/nama menjadi {@code "_"}, harga/diskon/cashback menjadi 0, jumlah menjadi 1), lalu
	 * produknya dicari atau dibuat lewat
	 * {@link #resolveOrCreateProduk(Session, String, String, String, Double, Toko)}. Kode baris
	 * dibentuk sebagai {@code kodeUnik + "-" + kodeBarang} agar tetap dapat ditelusuri balik ke
	 * struk induknya. Bila klien mengirim {@code satuan_jual_id}, satuan jual beserta
	 * {@code qty_input} dan {@code faktor_ke_dasar} ikut disnapshot ke baris — nilai-nilai itu sudah
	 * divalidasi server di {@code KantinHelper.terapkanSatuanJual} sebelum sampai ke sini, jadi di
	 * titik ini hanya disalin.</li>
	 * </ol>
	 *
	 * <h3>Baris "Produk Ekstra" dan pemetaan induk dua-lintasan</h3>
	 * <p>
	 * Satu baris belanja dapat memiliki baris <i>ekstra</i> (mis. topping, tambahan porsi) yang
	 * disimpan sebagai baris {@code Pembelian} tersendiri dengan {@code indukId} menunjuk ke id
	 * baris induknya. Pada cabang checkout langsung hal ini sederhana: baris induk disimpan lebih
	 * dulu sehingga sudah punya id, lalu tiap ekstra dibuat dengan {@code setIndukId(pembelian.getId())}
	 * dan qty-nya dikalikan qty induk ({@code jumlahBarang * jumlahEkstra}) supaya menambah dua porsi
	 * makanan ikut menggandakan toppingnya.
	 * </p>
	 * <p>
	 * Pada cabang finalisasi draft persoalannya lebih halus dan itulah sebabnya loopnya
	 * <b>dua lintasan, bukan satu</b>. {@code DraftPembelian.getIndukId()} menunjuk ke id
	 * <i>DraftPembelian</i> lain; begitu draft itu difinalisasi ia melahirkan baris {@code Pembelian}
	 * dengan id yang <b>baru dan berbeda</b>. Menyalin {@code indukId} mentah-mentah akan membuat
	 * baris ekstra menunjuk ke id yang salah domainnya. Karena itu lintasan pertama hanya memproses
	 * baris dasar ({@code indukId == null}) sambil mencatat peta <i>id draft lama &rarr; id pembelian
	 * baru</i>, dan lintasan kedua memproses baris ekstra dengan menerjemahkan {@code indukId}
	 * melalui peta itu. Bila terjemahannya gagal (induk tidak ada di peta), baris ekstra
	 * <b>dilewati dan dicatat</b> ke {@code ErrorAuditUtil} — sengaja <i>tidak</i> mundur ke
	 * {@code indukId = null}, karena baris ekstra yatim akan terbaca sebagai baris belanja mandiri
	 * dan salah diatribusikan pada laporan maupun perhitungan piutang.
	 * </p>
	 *
	 * <h3>Batasan atomisitas — perilaku yang disengaja</h3>
	 * <p>
	 * Kedua cabang <b>tidak</b> berjalan dalam satu transaksi basis data. Tiap baris disimpan dengan
	 * pasangan {@code begin()}/{@code commit()}-nya sendiri, dan filosofinya berbeda per cabang:
	 * </p>
	 * <ul>
	 * <li><b>Checkout langsung</b> menganut "tanpa atomisitas lintas-baris": kegagalan satu item
	 * ditangkap, di-rollback secara eksplisit, dicatat ke audit, lalu loop lanjut ke item berikutnya.
	 * Rollback eksplisit itu bukan hiasan — tanpa itu transaksi yang gagal tertinggal dalam keadaan
	 * aktif dan meracuni {@code begin()}/{@code commit()} <b>seluruh</b> item sesudahnya. Baris
	 * ekstra punya {@code try}/{@code catch} sendiri yang terpisah dari induknya, supaya satu topping
	 * yang gagal tidak membatalkan baris induk yang sudah berhasil di-commit.</li>
	 * <li><b>Finalisasi draft</b> justru dibiarkan bersifat semua-atau-tidak-sama-sekali: tidak ada
	 * {@code try}/{@code catch} per baris di sana, sehingga kegagalan di tengah loop merambat keluar
	 * dan ditangkap pemanggil ({@code bayar()}). Ini keputusan sadar yang dipertahankan apa adanya,
	 * bukan kelalaian mengikuti pola cabang tetangganya.</li>
	 * </ul>
	 * <p>
	 * Konsekuensi praktis yang perlu diketahui pemanggil: pada cabang checkout langsung, jumlah
	 * elemen {@link JSONArray} yang dikembalikan <b>boleh jadi lebih sedikit</b> daripada jumlah item
	 * yang dikirim klien bila ada baris yang gagal, dan total di header tidak otomatis menyesuaikan
	 * diri — angka header diisi terpisah oleh {@code KantinHelper}. Selisih antara header dan jumlah
	 * baris karena itu memang mungkin terjadi, dan justru itulah alasan keberadaan
	 * {@link #getDetailPembelianCadangan()} sebagai bukti pembanding independen.
	 * </p>
	 *
	 * <h3>Bentuk JSON yang dikembalikan</h3>
	 * <p>
	 * Tiap baris yang berhasil disimpan diserialisasi dengan
	 * {@code Common.insertProperty(Pembelian.class, ..., kedalaman 1, ...)} sambil
	 * <b>mengecualikan</b> relasi berat dan relasi melingkar: {@code siswa}, {@code calonSiswa},
	 * {@code mahasiswa}, {@code biodataCalonMahasiswa}, {@code pembelianAnggotaKoperasi} (induknya
	 * sendiri — tanpa pengecualian ini serialisasi akan berputar), {@code tbmuser},
	 * {@code kodePembayaranOnline}, {@code toko}, dan {@code draftPembelian}. Daftar pengecualian
	 * yang sama dipakai untuk baris ekstra agar bentuk keluarannya seragam.
	 * </p>
	 *
	 * @param session sesi Hibernate aktif; method ini mengelola transaksinya sendiri per baris
	 * @param transaksi array rincian barang dari klien POS; hanya dipakai pada cabang checkout
	 *            langsung, boleh diabaikan (namun tidak boleh {@code null}) pada cabang draft
	 * @param kodeUnik nomor struk yang menjadi awalan kode tiap baris rincian
	 * @param currentWaktu waktu yang dicap ke seluruh baris rincian, supaya satu struk memiliki satu
	 *            waktu yang seragam alih-alih waktu per baris yang berbeda beberapa milidetik
	 * @param toko toko pemilik baris rincian dan lingkup pencarian produk
	 * @param kodePembayaranOnline kode pembayaran online yang melunasi struk, boleh {@code null}
	 * @param draftPembelianAnggotaKoperasi draft yang sedang difinalisasi; bila {@code null} atau
	 *            belum punya id, method memilih cabang checkout langsung
	 * @return array JSON berisi baris rincian yang berhasil tersimpan, termasuk baris ekstra
	 * @see #resolveOrCreateProduk(Session, String, String, String, Double, Toko)
	 * @see DraftPembelianAnggotaKoperasi#simpanRinci
	 */
	@SuppressWarnings("unchecked")
	public JSONArray simpanRinci(Session session, JSONArray transaksi, String kodeUnik, Date currentWaktu, Toko toko,
			KodePembayaranOnline kodePembayaranOnline, DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi) {

		JSONArray arrayTransaksi = new JSONArray();
		if (draftPembelianAnggotaKoperasi != null && draftPembelianAnggotaKoperasi.getId() != null) {

			List<DraftPembelian> draftPembelians = session.createCriteria(DraftPembelian.class)
					.add(Restrictions.eq("draftPembelianAnggotaKoperasi", draftPembelianAnggotaKoperasi)).list();

			// Gap-closure "Produk Ekstra" -- REMAP 2-PASS, BUKAN 1 loop spt sebelumnya: baris ekstra
			// (DraftPembelian.indukId != null) menunjuk ke id DraftPembelian LAIN, yang begitu
			// difinalisasi jadi Pembelian BARU dgn id BARU -- indukId-nya harus di-remap ke id BARU
			// itu, bukan disalin mentah. Pass 1 memproses SEMUA baris dasar (indukId == null) dulu &
			// mencatat peta id-lama->id-baru; pass 2 baru memproses baris ekstra memakai peta itu.
			// SENGAJA TIDAK ditambah try/catch per-baris di sini (beda dari cabang normal-checkout di
			// bawah) -- cabang finalisasi draft ini SUDAH all-or-nothing sejak awal (kegagalan di
			// tengah loop merambat ke luar, ditangkap pemanggil bayar()), keputusan SADAR dipertahankan
			// apa adanya, bukan ikut pola isolasi cabang lain tanpa sengaja.
			java.util.Map<Long, Long> petaDraftKePembelian = new java.util.HashMap<Long, Long>();

			// Pass 1: baris DASAR (indukId == null).
			for (DraftPembelian draftPembelian : draftPembelians) {
				if (draftPembelian.getIndukId() != null) {
					continue;
				}

				AturanDiskon aturanDiskon = draftPembelian.getAturanDiskon();
				Produk produk = draftPembelian.getProduk();

				Pembelian pembelian = new Pembelian();
				pembelian.setPembelianAnggotaKoperasi(this);
				pembelian.setAnggotaKoperasi(this.getAnggotaKoperasi());
				pembelian.setKode(draftPembelian.getKode());
				pembelian.setNama(draftPembelian.getNama());
				pembelian.setKodePembayaranOnline(kodePembayaranOnline);
				pembelian.setQty(draftPembelian.getQty());
				pembelian.setDiskon(draftPembelian.getDiskon());
				pembelian.setAturanDiskon(aturanDiskon);
				pembelian.setCashback(draftPembelian.getCashback());
				pembelian.setHargaSatuan(draftPembelian.getHargaSatuan());
				pembelian.setProduk(produk);
				pembelian.setToko(toko);
				pembelian.setWaktu(currentWaktu);
				session.getTransaction().begin();
				session.save(pembelian);
				session.getTransaction().commit();

				petaDraftKePembelian.put(draftPembelian.getId(), pembelian.getId());

				JSONObject data = new JSONObject();
				Common.insertProperty(Pembelian.class, pembelian, data, "", 1, "siswa", "calonSiswa", "mahasiswa",
						"biodataCalonMahasiswa", "pembelianAnggotaKoperasi", "tbmuser", "kodePembayaranOnline", "toko",
						"draftPembelian");
				arrayTransaksi.put(data);

				draftPembelian.setLunas(pembelian);
				session.getTransaction().begin();
				session.update(draftPembelian);
				session.getTransaction().commit();
			}

			// Pass 2: baris EKSTRA (indukId != null) -- indukId di-remap via peta pass 1.
			for (DraftPembelian draftPembelian : draftPembelians) {
				if (draftPembelian.getIndukId() == null) {
					continue;
				}
				Long indukPembelianId = petaDraftKePembelian.get(draftPembelian.getIndukId());
				if (indukPembelianId == null) {
					// Induk gagal/tak ketemu ter-map -- JANGAN fallback indukId=null (itu akan membuat
					// baris ekstra ini keanggep baris mandiri, salah atribusi piutang/laporan). Skip +
					// audit log, konsisten dgn semangat "no cross-line atomicity" fitur ini.
					ais.common.ErrorAuditUtil.record(
							new RuntimeException("draft finalize: baris ekstra draftPembelian id=" + draftPembelian.getId()
									+ " indukId=" + draftPembelian.getIndukId() + " tidak ketemu di peta induk"),
							"auto-audit src/ais/database/model/koperasi/PembelianAnggotaKoperasi.java:simpanRinci:draftFinalizeOrphanEkstra");
					continue;
				}

				AturanDiskon aturanDiskon = draftPembelian.getAturanDiskon();
				Produk produk = draftPembelian.getProduk();

				Pembelian pembelian = new Pembelian();
				pembelian.setPembelianAnggotaKoperasi(this);
				pembelian.setAnggotaKoperasi(this.getAnggotaKoperasi());
				pembelian.setKode(draftPembelian.getKode());
				pembelian.setNama(draftPembelian.getNama());
				pembelian.setKodePembayaranOnline(kodePembayaranOnline);
				pembelian.setQty(draftPembelian.getQty());
				pembelian.setDiskon(draftPembelian.getDiskon());
				pembelian.setAturanDiskon(aturanDiskon);
				pembelian.setCashback(draftPembelian.getCashback());
				pembelian.setHargaSatuan(draftPembelian.getHargaSatuan());
				pembelian.setProduk(produk);
				pembelian.setToko(toko);
				pembelian.setWaktu(currentWaktu);
				pembelian.setIndukId(indukPembelianId);
				session.getTransaction().begin();
				session.save(pembelian);
				session.getTransaction().commit();

				JSONObject data = new JSONObject();
				Common.insertProperty(Pembelian.class, pembelian, data, "", 1, "siswa", "calonSiswa", "mahasiswa",
						"biodataCalonMahasiswa", "pembelianAnggotaKoperasi", "tbmuser", "kodePembayaranOnline", "toko",
						"draftPembelian");
				arrayTransaksi.put(data);

				draftPembelian.setLunas(pembelian);
				session.getTransaction().begin();
				session.update(draftPembelian);
				session.getTransaction().commit();
			}

		} else {

			for (int i = 0; i < transaksi.length(); i++) {
				try {
					JSONObject objectTransaksi = transaksi.getJSONObject(i);
					String idBarang = objectTransaksi.isNull("id") ? null : objectTransaksi.get("id") + "";
					String kodeBarang = objectTransaksi.isNull("kode") ? "_" : objectTransaksi.get("kode") + "";
					String namaBarang = objectTransaksi.isNull("nama") ? "_" : objectTransaksi.get("nama") + "";
					Double hargaBarang = objectTransaksi.isNull("harga") ? 0.0
							: Double.parseDouble((objectTransaksi.get("harga") + "").trim());
					Double jumlahBarang = objectTransaksi.isNull("jumlah") ? 1.0
							: Double.parseDouble((objectTransaksi.get("jumlah") + "").trim());
					Double diskonBarang = objectTransaksi.isNull("diskon") ? 0.0
							: Double.parseDouble((objectTransaksi.get("diskon") + "").trim());
					Double cashbackBarang = objectTransaksi.isNull("cashback") ? 0.0
							: Double.parseDouble((objectTransaksi.get("cashback") + "").trim());
					Produk produk = resolveOrCreateProduk(session, idBarang, kodeBarang, namaBarang, hargaBarang, toko);

					AturanDiskon aturanDiskon = (objectTransaksi.isNull("aturanDiskon") ? null
							: (AturanDiskon) GeneralValueObject.ambilData(AturanDiskon.class,
									(objectTransaksi.get("aturanDiskon") + "").trim()));

					Pembelian pembelian = new Pembelian();
					pembelian.setPembelianAnggotaKoperasi(this);
					pembelian.setAnggotaKoperasi(this.getAnggotaKoperasi());
					pembelian.setKode(kodeUnik + "-" + kodeBarang);
					pembelian.setNama(namaBarang);
					pembelian.setKodePembayaranOnline(kodePembayaranOnline);
					pembelian.setQty(jumlahBarang);
					// Fase B: snapshot satuan jual (sudah divalidasi/diturunkan server di
					// KantinHelper.terapkanSatuanJual sebelum sampai ke sini).
					if (!objectTransaksi.isNull("satuan_jual_id")) {
						pembelian.setSatuanJual(Long.valueOf(objectTransaksi.optLong("satuan_jual_id")));
						pembelian.setQtyInput(Double.valueOf(objectTransaksi.optDouble("qty_input", 0)));
						pembelian.setFaktorKeDasar(Double.valueOf(objectTransaksi.optDouble("faktor_ke_dasar", 1)));
					}
					pembelian.setDiskon(diskonBarang);
					pembelian.setAturanDiskon(aturanDiskon);
					pembelian.setCashback(cashbackBarang);
					pembelian.setHargaSatuan(hargaBarang);
					pembelian.setProduk(produk);
					pembelian.setToko(toko);
					pembelian.setWaktu(currentWaktu);
					session.getTransaction().begin();
					session.save(pembelian);
					session.getTransaction().commit();

					JSONObject data = new JSONObject();
					Common.insertProperty(Pembelian.class, pembelian, data, "", 1, "siswa", "calonSiswa", "mahasiswa",
							"biodataCalonMahasiswa", "pembelianAnggotaKoperasi", "tbmuser", "kodePembayaranOnline",
							"toko", "draftPembelian");
					arrayTransaksi.put(data);

					// Gap-closure "Produk Ekstra" -- SETELAH baris induk tersimpan (punya .getId()), bikin
					// SATU baris Pembelian per ekstra terpilih, indukId menunjuk balik ke baris induk ini.
					// Try/catch TERPISAH dari try/catch per-item induk di atas (baris ~275) -- kegagalan 1
					// ekstra TIDAK BOLEH rollback induk yang sudah commit, konsisten dgn filosofi "no
					// cross-line atomicity" yang sudah dianut loop ini sejak awal (1 item transaksi gagal
					// tidak menggagalkan item lain).
					if (!objectTransaksi.isNull("ekstra")) {
						JSONArray ekstraArr = objectTransaksi.optJSONArray("ekstra");
						for (int k = 0; ekstraArr != null && k < ekstraArr.length(); k++) {
							try {
								JSONObject e = ekstraArr.getJSONObject(k);
								String idEkstra = e.isNull("id") ? null : e.get("id") + "";
								String kodeEkstra = e.isNull("kode") ? "_" : e.get("kode") + "";
								String namaEkstra = e.isNull("nama") ? "_" : e.get("nama") + "";
								Double hargaEkstra = e.isNull("harga") ? 0.0
										: Double.parseDouble((e.get("harga") + "").trim());
								Double jumlahEkstra = e.isNull("jumlah") ? 1.0
										: Double.parseDouble((e.get("jumlah") + "").trim());

								Produk produkEkstra = resolveOrCreateProduk(session, idEkstra, kodeEkstra, namaEkstra,
										hargaEkstra, toko);

								Pembelian pembelianEkstra = new Pembelian();
								pembelianEkstra.setPembelianAnggotaKoperasi(this);
								pembelianEkstra.setAnggotaKoperasi(this.getAnggotaKoperasi());
								pembelianEkstra.setKode(kodeUnik + "-" + kodeBarang + "-" + kodeEkstra);
								pembelianEkstra.setNama(namaEkstra);
								pembelianEkstra.setKodePembayaranOnline(kodePembayaranOnline);
								pembelianEkstra.setQty(jumlahBarang * jumlahEkstra);
								pembelianEkstra.setDiskon(0.0);
								pembelianEkstra.setCashback(0.0);
								pembelianEkstra.setHargaSatuan(hargaEkstra);
								pembelianEkstra.setProduk(produkEkstra);
								pembelianEkstra.setToko(toko);
								pembelianEkstra.setWaktu(currentWaktu);
								pembelianEkstra.setIndukId(pembelian.getId());
								session.getTransaction().begin();
								session.save(pembelianEkstra);
								session.getTransaction().commit();

								JSONObject dataEkstra = new JSONObject();
								Common.insertProperty(Pembelian.class, pembelianEkstra, dataEkstra, "", 1, "siswa",
										"calonSiswa", "mahasiswa", "biodataCalonMahasiswa", "pembelianAnggotaKoperasi",
										"tbmuser", "kodePembayaranOnline", "toko", "draftPembelian");
								arrayTransaksi.put(dataEkstra);
							} catch (Exception eEkstra) {
								ais.common.ErrorAuditUtil.record(eEkstra,
										"auto-audit src/ais/database/model/koperasi/PembelianAnggotaKoperasi.java:simpanRinci:ekstra");
								try {
									if (session.getTransaction() != null && session.getTransaction().isActive()) {
										session.getTransaction().rollback();
									}
								} catch (Exception eRollback) {
									ais.common.ErrorAuditUtil.record(eRollback,
											"auto-audit(rollback) src/ais/database/model/koperasi/PembelianAnggotaKoperasi.java:simpanRinci:ekstra");
								}
							}
						}
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/koperasi/PembelianAnggotaKoperasi.java:249");
					// Rollback eksplisit -- lihat JavaDoc di DraftPembelianAnggotaKoperasi.simpanRinci
					// (pola yg sama): tanpa ini, satu item gagal membuat transaction tertinggal
					// aktif-tapi-gagal, meracuni begin()/commit() SEMUA item sesudahnya di loop ini.
					try {
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception eRollback) {
						ais.common.ErrorAuditUtil.record(eRollback, "auto-audit(rollback) src/ais/database/model/koperasi/PembelianAnggotaKoperasi.java:simpanRinci");
					}
				}
			}
		}

		return arrayTransaksi;
	}

	/**
	 * Cari {@link Produk} berdasarkan id (kalau dikirim), atau kode+nama persis di toko yang sama;
	 * kalau tak ketemu, buat baris baru dgn harga jual/beli = {@code hargaBarang}. Diekstrak dari
	 * logic yang SEBELUMNYA inline-duplicated persis di sini dan di
	 * {@link DraftPembelianAnggotaKoperasi#simpanRinci} (gap-closure "Produk Ekstra" -- tanpa
	 * ekstraksi ini logic yg sama akan ter-copy-paste 4x begitu baris Ekstra ikut butuh resolve
	 * produk yg sama persis). {@code public static} supaya bisa dipanggil lintas-kelas dari
	 * {@code DraftPembelianAnggotaKoperasi} tanpa duplikasi.
	 */
	public static Produk resolveOrCreateProduk(Session session, String idBarang, String kodeBarang,
			String namaBarang, Double hargaBarang, Toko toko) {
		Produk produk = (Produk) (idBarang != null && !idBarang.trim().isEmpty()
				? GeneralValueObject.ambilData(Produk.class, idBarang)
				: ConstantValues.simpleObject(
						session.createCriteria(Produk.class).add(Restrictions.eq("toko", toko))
								.add(Restrictions.ilike("nama", namaBarang, MatchMode.EXACT))
								.add(Restrictions.ilike("kode", kodeBarang, MatchMode.EXACT)),
						Produk.class));
		if (produk == null) {
			produk = new Produk();
			produk.setNama(namaBarang);
			produk.setKode(kodeBarang);
			produk.setHargaBeli(hargaBarang);
			produk.setHargaJual(hargaBarang);
			produk.setToko(toko);
			session.getTransaction().begin();
			session.save(produk);
			session.getTransaction().commit();
		}
		return produk;
	}

	/**
	 * Konstruktor kosong yang diwajibkan JPA/Hibernate untuk membuat instance saat memuat baris dari
	 * basis data.
	 *
	 * <p>
	 * Semua field angka sudah bernilai awal {@code 0.0} lewat inisialisasi field, dan
	 * {@link #getSumberTransaksi()} sudah bernilai {@link #SUMBER_ONLINE}, sehingga objek baru bisa
	 * langsung dipakai tanpa risiko {@code NullPointerException} pada perhitungan.
	 * </p>
	 */
	public PembelianAnggotaKoperasi() {
	}

	/**
	 * Kunci utama baris ini, dibangkitkan basis data (strategi {@code IDENTITY}).
	 *
	 * <p>
	 * Kolomnya {@code insertable = false} karena nilainya ditentukan sepenuhnya oleh sekuens basis
	 * data. Bernilai {@code null} selama objek belum pernah disimpan — pemeriksaan
	 * {@code getId() != null} dipakai di banyak tempat sebagai penanda "sudah tersimpan".
	 * </p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Dipakai Hibernate saat memuat baris; jangan diubah manual pada baris yang
	 * sudah tersimpan karena akan memutus kaitannya dengan baris {@link Pembelian} anaknya.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nomor struk transaksi ini.
	 *
	 * <p>
	 * Wajib terisi ({@code nullable = false}) dan menjadi awalan kode seluruh baris rincian yang
	 * dibuat {@link #simpanRinci(Session, JSONArray, String, Date, Toko, KodePembayaranOnline,
	 * DraftPembelianAnggotaKoperasi)} — pola {@code kode + "-" + kodeBarang} — sehingga baris
	 * rincian tetap dapat ditelusuri balik ke strukna bahkan bila relasinya hilang. Nilainya
	 * dibangkitkan pemanggil ({@code KantinHelper}/{@code PosApi}), bukan oleh entity ini.
	 * </p>
	 *
	 * @return nomor struk
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Mengisi nomor struk.
	 *
	 * @param kode nomor struk yang dibangkitkan pemanggil
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Catatan bebas atas struk ini.
	 *
	 * <p>
	 * Bertipe {@code text} sehingga panjangnya tidak dibatasi. Dipakai antara lain untuk catatan
	 * supervisor pada transaksi yang perlu penjelasan (mis. koreksi harga manual, catatan pembatalan)
	 * dan untuk keterangan yang diketik kasir di layar POS.
	 * </p>
	 *
	 * @return catatan bebas, atau {@code null} bila tidak ada
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas atas struk ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Asal-usul transaksi ini: {@link #SUMBER_ONLINE} (default, dibuat lewat jalur checkout online
	 * normal {@code KantinHelper.bayar()}) atau {@link #SUMBER_OFFLINE_SYNC} (awalnya tersimpan di
	 * antrian offline browser kasir/IndexedDB, baru masuk ke server belakangan lewat sinkronisasi).
	 * Dipakai laporan "Riwayat Sinkronisasi" untuk membedakan mana transaksi yang sempat tertunda di
	 * perangkat kasir sebelum sampai ke server, terlepas dari kapan transaksi itu SEBENARNYA terjadi
	 * (lihat {@link #getTanggalPembayaran()}) dibanding kapan baru tersimpan di server
	 * (lihat {@link #getWaktuSinkron()}).
	 */
	@Column(name = "sumber_transaksi", length = 20)
	public String getSumberTransaksi() {
		return sumberTransaksi == null ? SUMBER_ONLINE : sumberTransaksi;
	}

	/**
	 * Menandai asal-usul transaksi ini.
	 *
	 * <p>
	 * Diisi {@link #SUMBER_OFFLINE_SYNC} oleh {@code pos_offline_service.jsp} saat memasukkan
	 * transaksi dari antrian offline; jalur checkout online normal membiarkannya pada nilai awal
	 * {@link #SUMBER_ONLINE}. Nilai {@code null} tetap dibaca sebagai {@link #SUMBER_ONLINE} oleh
	 * {@link #getSumberTransaksi()}, sehingga transaksi lama sebelum fitur ini ada tidak perlu
	 * dimigrasi.
	 * </p>
	 *
	 * @param sumberTransaksi {@link #SUMBER_ONLINE} atau {@link #SUMBER_OFFLINE_SYNC}
	 */
	public void setSumberTransaksi(String sumberTransaksi) {
		this.sumberTransaksi = sumberTransaksi;
	}

	/**
	 * Waktu SERVER benar-benar menerima &amp; menyimpan transaksi ini lewat antrian sinkronisasi
	 * offline ({@code pos_offline_service.jsp}, {@code aksi=sync}). Bernilai {@code null} untuk
	 * transaksi {@link #SUMBER_ONLINE} (langsung tersimpan saat itu juga, tidak ada jeda berarti untuk
	 * dicatat terpisah). Selisih antara nilai ini dan {@link #getTanggalPembayaran()} (waktu transaksi
	 * yang dicatat di perangkat kasir saat pembeli membayar) adalah "keterlambatan sinkron" — makin
	 * lama kasir offline sebelum koneksi pulih, makin besar selisihnya.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_sinkron", nullable = true)
	public Date getWaktuSinkron() {
		return waktuSinkron;
	}

	/**
	 * Mencatat waktu server menerima transaksi ini lewat antrian sinkronisasi offline.
	 *
	 * <p>
	 * Hanya diisi pada jalur {@link #SUMBER_OFFLINE_SYNC}; dibiarkan {@code null} untuk transaksi
	 * online supaya laporan dapat membedakan keduanya tanpa kolom penanda tambahan.
	 * </p>
	 *
	 * @param waktuSinkron waktu penerimaan di server; {@code null} untuk transaksi online
	 */
	public void setWaktuSinkron(Date waktuSinkron) {
		this.waktuSinkron = waktuSinkron;
	}

	/**
	 * Mengisi anggota koperasi pembeli.
	 *
	 * <p>
	 * <b>Perhatian:</b> nilai yang diisi di sini akan <b>diabaikan saat pembacaan</b> bila header ini
	 * memiliki {@link #getDraftPembelianAnggotaKoperasi()} — lihat {@link #getAnggotaKoperasi()}.
	 * Untuk header hasil finalisasi draft, kepemilikan transaksi harus diubah lewat draftnya.
	 * </p>
	 *
	 * @param anggotaKoperasi anggota pembeli; {@code null} untuk pembeli umum non-anggota
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Anggota koperasi yang menjadi pembeli pada struk ini.
	 *
	 * <p>
	 * Boleh {@code null}: kasir koperasi juga melayani pembeli umum yang bukan anggota, dan struk
	 * semacam itu tetap sah sebagai penjualan — hanya saja tidak memotong saldo deposit siapa pun.
	 * Bila terisi, anggota inilah yang saldo depositnya berkurang sebesar bagian pembayaran yang
	 * memakai metode pemotong deposit; perhitungannya ada di
	 * {@code DepositHelper.hitungDeposit(AnggotaKoperasi)} dan dilakukan dari header ini, bukan dari
	 * baris rinciannya.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif.</b> Bila {@link #getDraftPembelianAnggotaKoperasi()} terisi, field
	 * {@code anggotaKoperasi} milik header <b>ditimpa</b> oleh nilai dari draft pada setiap
	 * pemanggilan; kolom header sendiri tidak lagi menjadi sumber kebenaran. Karena Hibernate juga
	 * memakai getter ini saat menulis, nilai turunan tersebut ikut tersimpan ke kolom
	 * {@code anggota_koperasi}. Bila draft tidak ada, nilai dilewatkan
	 * {@link GeneralValueObject#check(Object)} untuk menyelesaikan proksi malas dan menyamakan
	 * instance dengan objek kanonik seid.
	 * </p>
	 *
	 * @return anggota pembeli, atau {@code null} untuk pembeli umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		if (draftPembelianAnggotaKoperasi != null) {
			anggotaKoperasi = draftPembelianAnggotaKoperasi.getAnggotaKoperasi();
		} else {
			anggotaKoperasi = check(anggotaKoperasi);
		}
		return anggotaKoperasi;
	}

	/**
	 * Mengisi nomor urut tampilan sementara.
	 *
	 * @param index nomor urut baris pada daftar yang sedang ditampilkan
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Nomor urut tampilan sementara.
	 *
	 * <p>
	 * Tidak ada anotasi pemetaan pada properti ini sehingga Hibernate memperlakukannya sebagai
	 * properti biasa yang <b>ikut dipetakan implisit</b> hanya bila kolomnya ada; pada praktiknya
	 * nilainya diisi lapisan tampilan (grid ZK/laporan) untuk memberi nomor urut baris dan tidak
	 * memiliki arti bisnis. Jangan dipakai sebagai pengenal transaksi — gunakan {@link #getId()} atau
	 * {@link #getKode()}.
	 * </p>
	 *
	 * @return nomor urut tampilan, atau {@code null} bila tidak diisi
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Lokasi/outlet tempat transaksi ini terjadi.
	 *
	 * <p>
	 * Lokasi adalah satuan yang lebih halus daripada {@link #getToko()}: satu toko dapat memiliki
	 * beberapa lokasi (mis. kantin utama, kios lapangan). Perhatikan arah turunannya —
	 * {@link #getToko()} justru mengambil tokonya <i>dari</i> lokasi ini bila lokasi terisi, jadi
	 * lokasi yang keliru akan ikut memindahkan transaksi ke toko yang keliru pula.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif.</b> Bila draft terpasang, nilai lokasi disalin ulang dari draft pada tiap
	 * pemanggilan sehingga {@link #setLokasi(Lokasi)} tidak berpengaruh; selebihnya nilai dilewatkan
	 * {@link GeneralValueObject#check(Object)}.
	 * </p>
	 *
	 * @return lokasi transaksi, atau {@code null} bila tidak dicatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		if (draftPembelianAnggotaKoperasi != null) {
			lokasi = draftPembelianAnggotaKoperasi.getLokasi();
		} else {
			lokasi = check(lokasi);
		}
		return lokasi;
	}

	/**
	 * Mengisi lokasi/outlet transaksi.
	 *
	 * <p>
	 * Diabaikan pada pembacaan bila header ini punya draft — lihat {@link #getLokasi()}.
	 * </p>
	 *
	 * @param lokasi lokasi transaksi; boleh {@code null}
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Nilai belanja struk ini.
	 *
	 * <p>
	 * Pada praktiknya {@code KantinHelper} mengisi properti ini dengan angka yang <b>sama persis</b>
	 * dengan {@link #getTotalBiaya()} ({@code setBiaya(total)} dipanggil berdampingan dengan
	 * {@code setTotalBiaya(total)}). Perhitungan turunan — kelunasan, pemotongan deposit, nominal
	 * slot 1 — semuanya bersandar pada {@code totalBiaya}, bukan pada properti ini. Anggap
	 * {@code biaya} sebagai kolom warisan yang dipertahankan untuk laporan lama; gunakan
	 * {@link #getTotalBiaya()} untuk logika baru.
	 * </p>
	 *
	 * <p>
	 * Tidak seperti {@link #getTotalBiaya()}, getter ini <b>tidak</b> menjaga terhadap {@code null},
	 * sehingga pemanggil yang membaca baris lama perlu memeriksanya sendiri.
	 * </p>
	 *
	 * @return nilai belanja, mungkin {@code null} pada baris lama
	 */
	public Double getBiaya() {
		return biaya;
	}

	/**
	 * Mengisi nilai belanja struk.
	 *
	 * @param biaya nilai belanja; biasanya disamakan dengan {@link #setTotalBiaya(Double)}
	 */
	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/**
	 * Angka diskon tingkat struk apa adanya.
	 *
	 * <p>
	 * Satuannya <b>bergantung pada</b> {@link #getDiskonDalamPersen()}: bila penanda itu bernilai
	 * benar, angka ini adalah persentase; bila tidak, angka ini rupiah. Hasil akhirnya — berapa
	 * rupiah yang benar-benar dipotong — dicatat terpisah di {@link #getTotalDiskon()}, sehingga
	 * laporan tidak perlu menghitung ulang dan tidak berisiko salah menafsirkan satuan.
	 * </p>
	 *
	 * @return angka diskon tingkat struk
	 */
	public Double getDiskon() {
		return diskon;
	}

	/**
	 * Mengisi angka diskon tingkat struk.
	 *
	 * @param diskon angka diskon; satuannya ditentukan {@link #setDiskonDalamPersen(Boolean)}
	 */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Penanda satuan {@link #getDiskon()}: {@code true} bila persen, {@code false}/{@code null} bila
	 * rupiah.
	 *
	 * @return penanda satuan diskon
	 */
	@Column(name = "diskon_dalam_persen")
	public Boolean getDiskonDalamPersen() {
		return diskonDalamPersen;
	}

	/**
	 * Menetapkan satuan {@link #getDiskon()}.
	 *
	 * @param diskonDalamPersen {@code true} bila angka diskon berupa persen
	 */
	public void setDiskonDalamPersen(Boolean diskonDalamPersen) {
		this.diskonDalamPersen = diskonDalamPersen;
	}

	/**
	 * Total rupiah diskon yang benar-benar diberikan pada struk ini.
	 *
	 * <p>
	 * Ini angka yang sudah "matang" — hasil menerapkan {@link #getDiskon()} beserta satuannya, dan
	 * pada jalur POS juga sudah memperhitungkan diskon per baris/aturan diskon. Laporan penjualan
	 * memakai properti ini, bukan {@link #getDiskon()}.
	 * </p>
	 *
	 * @return total rupiah diskon
	 */
	@Column(name = "total_diskon")
	public Double getTotalDiskon() {
		return totalDiskon;
	}

	/**
	 * Mengisi total rupiah diskon struk.
	 *
	 * @param totalDiskon total rupiah diskon
	 */
	public void setTotalDiskon(Double totalDiskon) {
		this.totalDiskon = totalDiskon;
	}

	/**
	 * Persentase PPN yang berlaku pada struk ini.
	 *
	 * <p>
	 * Bersanding dengan {@link #getHargaPpn()} (dasar pengenaan) dan {@link #getPajak()} (nominal
	 * pajak yang sudah termasuk di {@link #getTotalBiaya()}). Bernilai 0 bila fitur pajak nonaktif
	 * pada toko yang bersangkutan.
	 * </p>
	 *
	 * @return persentase PPN
	 */
	public Double getPpn() {
		return ppn;
	}

	/**
	 * Mengisi persentase PPN struk.
	 *
	 * @param ppn persentase PPN
	 */
	public void setPpn(Double ppn) {
		this.ppn = ppn;
	}

	/**
	 * Bagian pembayaran yang diterima dalam bentuk <b>uang tunai</b>.
	 *
	 * <p>
	 * Getter ini menjaga terhadap {@code null} dengan menormalkannya menjadi {@code 0.0} — dan
	 * normalisasi itu <b>ditulis balik ke field</b>, bukan sekadar dikembalikan, sehingga baris lama
	 * yang kolomnya kosong ikut ternormalkan begitu dibaca. Penjagaan ini penting karena nilainya
	 * dijumlahkan tanpa pemeriksaan lagi di {@link #getBayar()} dan {@link #getLunas()}.
	 * </p>
	 *
	 * <p>
	 * Bersama {@link #getBayarNonTunai()}, properti ini juga menjadi dasar rekonsiliasi
	 * {@link #getSesiKasKasir()}: hanya bagian tunai yang secara fisik harus ada di laci kas saat
	 * sesi ditutup.
	 * </p>
	 *
	 * @return nominal tunai yang diterima, tidak pernah {@code null}
	 */
	@Column(name = "bayar_tunai")
	public Double getBayarTunai() {
		if (bayarTunai == null) {
			bayarTunai = 0.0;
		}
		return bayarTunai;
	}

	/**
	 * Mengisi bagian pembayaran tunai.
	 *
	 * @param bayarTunai nominal tunai; {@code null} akan dinormalkan menjadi {@code 0.0} saat dibaca
	 */
	public void setBayarTunai(Double bayarTunai) {
		this.bayarTunai = bayarTunai;
	}

	/**
	 * Bagian pembayaran yang diterima secara <b>non-tunai</b> (transfer, kartu, QRIS, potong saldo).
	 *
	 * <p>
	 * Sama seperti {@link #getBayarTunai()}, {@code null} dinormalkan menjadi {@code 0.0} dan ditulis
	 * balik ke field.
	 * </p>
	 *
	 * <p>
	 * Perlu dibedakan dari lima slot {@link #getCaraPembayaranKoperasi()}: pasangan
	 * tunai/non-tunai ini adalah pembagian <i>kasar</i> untuk rekonsiliasi laci kas, sedangkan lima
	 * slot metode bayar adalah rincian <i>per instrumen</i> untuk pemotongan deposit dan pelaporan.
	 * Keduanya diisi terpisah oleh {@code KantinHelper} dan tidak saling menurunkan.
	 * </p>
	 *
	 * @return nominal non-tunai yang diterima, tidak pernah {@code null}
	 */
	@Column(name = "bayar_non_tunai")
	public Double getBayarNonTunai() {
		if (bayarNonTunai == null) {
			bayarNonTunai = 0.0;
		}
		return bayarNonTunai;
	}

	/**
	 * Mengisi bagian pembayaran non-tunai.
	 *
	 * @param bayarNonTunai nominal non-tunai; {@code null} dinormalkan menjadi {@code 0.0} saat dibaca
	 */
	public void setBayarNonTunai(Double bayarNonTunai) {
		this.bayarNonTunai = bayarNonTunai;
	}

	/**
	 * Mengisi waktu pembeli membayar.
	 *
	 * <p>
	 * Diabaikan pada pembacaan bila header ini punya draft — lihat {@link #getTanggalPembayaran()}.
	 * </p>
	 *
	 * @param tanggalPembayaran waktu transaksi menurut perangkat kasir
	 */
	public void setTanggalPembayaran(Date tanggalPembayaran) {
		this.tanggalPembayaran = tanggalPembayaran;
	}

	/**
	 * Waktu pembeli membayar — <b>waktu bisnis</b> transaksi ini.
	 *
	 * <p>
	 * Inilah tanggal yang dipakai seluruh laporan penjualan dan yang menjadi batas perhitungan saldo
	 * deposit di {@code DepositHelper} ({@code date(h.tanggal_pembayaran) <= date(batas)}). Untuk
	 * transaksi offline, nilainya adalah waktu menurut <b>perangkat kasir</b> saat pembeli membayar,
	 * bukan saat server menerimanya; selisih terhadap {@link #getWaktuSinkron()} adalah lama
	 * keterlambatan sinkron. Jangan menggantinya dengan {@link #getTanggal_dirubah()}, yang hanya
	 * mencatat kapan barisnya terakhir disunting.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif.</b> Bila draft terpasang, nilainya disalin ulang dari draft setiap kali
	 * dipanggil. Perhatikan bahwa cabang ini <b>tidak</b> memiliki pasangan {@code else}: ketika
	 * draft kosong, nilai dikembalikan apa adanya tanpa melewati
	 * {@link GeneralValueObject#check(Object)} — wajar karena {@link Date} bukan entity.
	 * </p>
	 *
	 * @return waktu pembayaran
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembayaran")
	public Date getTanggalPembayaran() {
		if (draftPembelianAnggotaKoperasi != null) {
			tanggalPembayaran = draftPembelianAnggotaKoperasi.getTanggalPembayaran();
		}
		return tanggalPembayaran;
	}

	/**
	 * Mengisi akun kasir pembuat transaksi.
	 *
	 * <p>
	 * Diabaikan pada pembacaan bila header ini punya draft — lihat {@link #getTbmuser()}.
	 * </p>
	 *
	 * @param tbmuser akun kasir; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Akun pengguna kasir yang membuat transaksi ini.
	 *
	 * <p>
	 * Relasi ini menunjuk ke baris {@code Tbmuser} yang <b>hidup</b>: bila akun kasir kemudian
	 * diganti namanya, dinonaktifkan, atau dipakai orang lain, laporan yang membaca relasi ini ikut
	 * berubah. Justru karena itu {@link #getKasirLoginNama()} ada sebagai snapshot teks yang membeku
	 * pada saat checkout. Untuk laporan historis gunakan snapshot itu; gunakan relasi ini bila yang
	 * dibutuhkan adalah akunnya (mis. untuk memeriksa hak akses atau menghubungi kasirnya).
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif.</b> Bila draft terpasang, kasir disalin ulang dari draft — artinya untuk
	 * pesanan yang dibayar belakangan, yang tercatat adalah pembuat <i>draft</i>, bukan kasir yang
	 * menerima uangnya. Bila itu perlu dibedakan, {@link #getKasirLoginNama()} yang dapat
	 * diandalkan karena diisi langsung di titik pembayaran.
	 * </p>
	 *
	 * @return akun kasir, atau {@code null} bila tidak tercatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (draftPembelianAnggotaKoperasi != null) {
			tbmuser = draftPembelianAnggotaKoperasi.getTbmuser();
		} else {
			tbmuser = check(tbmuser);
		}
		return tbmuser;
	}

	/**
	 * Nama kasir yang BENAR-BENAR login di mesin POS saat transaksi dibuat -- SENGAJA field baru
	 * terpisah dari {@link #getOleh()}/{@link #getOlehId()} (itu metadata audit generik "siapa
	 * terakhir mengubah baris", diisi otomatis {@code AuditTimestampInterceptor} lewat
	 * {@code Common.getCurrentUser(HttpServletRequest)} -- yang SELALU {@code null} utk permintaan
	 * lewat {@code PosApi}, krn app POS diautentikasi lewat mekanismenya sendiri, bukan sesi
	 * browser, sehingga {@code oleh} selalu jatuh ke nilai fallback {@code "external_update"}).
	 * Disalin (snapshot teks, bukan sekadar mengandalkan relasi {@link #getTbmuser()}) tepat saat
	 * checkout di {@code KantinHelper.bayar/draft_bayar}/{@code TopupHelper}, supaya nama kasir yang
	 * tampil di laporan tetap benar apa pun yang terjadi pada akun {@code Tbmuser}-nya di kemudian
	 * hari.
	 */
	@Column(name = "kasir_login_nama", nullable = true)
	public String getKasirLoginNama() {
		return kasirLoginNama;
	}

	/**
	 * Mengisi snapshot nama kasir yang login di mesin POS.
	 *
	 * <p>
	 * Diisi tepat pada saat checkout oleh {@code KantinHelper.bayar}/{@code draft_bayar} dan
	 * {@code TopupHelper}. Karena ini snapshot teks, nilainya sengaja tidak diperbarui lagi setelah
	 * transaksi tersimpan.
	 * </p>
	 *
	 * @param kasirLoginNama nama kasir saat transaksi dibuat
	 */
	public void setKasirLoginNama(String kasirLoginNama) {
		this.kasirLoginNama = kasirLoginNama;
	}

	/**
	 * Nama mesin POS fisik asal transaksi ini (Desktop/Android) -- diisi admin sekali per instalasi
	 * di layar Konfigurasi, dikirim klien di tiap checkout. Gap-closure "toko dgn banyak mesin POS":
	 * kasir yang SAMA bisa login di lebih dari satu mesin, field ini yang membedakan mesin mana
	 * transaksinya, TIDAK bisa disimpulkan dari kasir/{@code tbmuser} saja. {@code null} utk
	 * transaksi lama (sebelum fitur ini ada) atau jalur JSP (belum kirim identitas mesin).
	 */
	@Column(name = "nama_mesin", nullable = true, length = 100)
	public String getNamaMesin() {
		return namaMesin;
	}

	/**
	 * Mengisi nama mesin POS fisik asal transaksi.
	 *
	 * @param namaMesin nama mesin yang dikirim klien POS; {@code null} untuk jalur yang belum
	 *            mengirim identitas mesin
	 */
	public void setNamaMesin(String namaMesin) {
		this.namaMesin = namaMesin;
	}

	/**
	 * Salinan keranjang belanja asli saat checkout dalam bentuk teks JSON.
	 *
	 * <p>
	 * Ini <b>bukti pembanding independen</b> terhadap baris {@code koperasi.pembelian}. Nilainya
	 * disimpan di header, di luar tabel rincian, justru supaya tetap utuh ketika baris rinciannya
	 * bermasalah — misalnya karena satu item gagal tersimpan pada loop
	 * {@link #simpanRinci(Session, JSONArray, String, Date, Toko, KodePembayaranOnline,
	 * DraftPembelianAnggotaKoperasi)} yang memang tidak atomik lintas-baris. Dengan begitu selisih
	 * antara total header dan jumlah baris rincian dapat diaudit dari data, bukan ditebak dari struk
	 * kertas.
	 * </p>
	 *
	 * <p>
	 * Bertipe {@code text} dan boleh {@code null} untuk transaksi lama sebelum fitur ini ada.
	 * Isinya <b>tidak</b> ikut diperbarui bila transaksi disunting kemudian — memang begitu
	 * maksudnya, karena nilainya adalah rekaman keadaan pada saat checkout.
	 * </p>
	 *
	 * @return teks JSON keranjang asli, atau {@code null} bila tidak tersedia
	 */
	@Column(name = "detail_pembelian_cadangan", nullable = true, columnDefinition = "text")
	public String getDetailPembelianCadangan() {
		return detailPembelianCadangan;
	}

	/**
	 * Mengisi salinan keranjang asli dalam bentuk teks JSON.
	 *
	 * @param detailPembelianCadangan teks JSON keranjang saat checkout
	 */
	public void setDetailPembelianCadangan(String detailPembelianCadangan) {
		this.detailPembelianCadangan = detailPembelianCadangan;
	}

	/** Sesi kas aktif yang menerima transaksi ini; menjadi sumber utama rekonsiliasi kas. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sesi_kas_kasir", nullable = true)
	public SesiKasKasir getSesiKasKasir() {
		return sesiKasKasir;
	}

	/**
	 * Menautkan transaksi ini ke sesi kas kasir yang sedang terbuka.
	 *
	 * <p>
	 * Diisi {@code KantinHelper} saat checkout dengan sesi kas aktif milik kasir/mesin yang
	 * bersangkutan. Perhatikan bahwa relasi ini {@code nullable}: transaksi yang dibuat di luar sesi
	 * kas (mis. jalur API tertentu atau data lama) tetap tersimpan, tetapi tidak akan muncul pada
	 * rekonsiliasi kas mana pun.
	 * </p>
	 *
	 * @param sesiKasKasir sesi kas penerima transaksi; boleh {@code null}
	 */
	public void setSesiKasKasir(SesiKasKasir sesiKasKasir) {
		this.sesiKasKasir = sesiKasKasir;
	}

	/** Snapshot perangkat asal transaksi untuk audit dan rekonsiliasi offline. */
	@Column(name = "id_perangkat", nullable = true, length = 128)
	public String getIdPerangkat() {
		return idPerangkat;
	}

	/**
	 * Mengisi identitas perangkat POS asal transaksi.
	 *
	 * <p>
	 * Berbeda dari {@link #setNamaMesin(String)} yang berupa label buatan admin, nilai ini adalah
	 * pengenal teknis yang dibangkitkan klien dan bertahan di penyimpanan lokal perangkat. Itulah
	 * yang membuatnya berguna untuk rekonsiliasi antrian offline: transaksi yang dikirim ulang dari
	 * perangkat yang sama dapat dikenali meski labelnya berubah.
	 * </p>
	 *
	 * @param idPerangkat pengenal perangkat POS; boleh {@code null}
	 */
	public void setIdPerangkat(String idPerangkat) {
		this.idPerangkat = idPerangkat;
	}

	/**
	 * Mengisi nominal retur atas struk ini.
	 *
	 * @param retur nominal retur
	 */
	public void setRetur(Double retur) {
		this.retur = retur;
	}

	/**
	 * Nominal retur yang tercatat pada struk ini.
	 *
	 * <p>
	 * Pada jalur POS kantin, pengembalian barang umumnya dicatat sebagai dokumen tersendiri
	 * ({@code inventory.ReturPenjualan}) atau pembatalan
	 * ({@link PembatalanTransaksiKantin}), bukan lewat kolom ini — kolom ini praktis hanya diisi
	 * jalur non-POS dan disetel {@code 0.0} oleh penyedia data demo. Karena itu <b>jangan</b>
	 * mengandalkan properti ini sebagai satu-satunya sumber angka retur; periksa dokumen retur dan
	 * pembatalan yang menunjuk ke header ini.
	 * </p>
	 *
	 * <p>
	 * Getter ini tidak menjaga terhadap {@code null}.
	 * </p>
	 *
	 * @return nominal retur, mungkin {@code null} pada baris lama
	 */
	public Double getRetur() {
		return retur;
	}

	/**
	 * Mengisi total yang harus dibayar pembeli.
	 *
	 * @param totalBiaya total struk; {@code null} dinormalkan menjadi {@code 0.0} saat dibaca
	 */
	public void setTotalBiaya(Double totalBiaya) {
		this.totalBiaya = totalBiaya;
	}

	/**
	 * Total yang harus dibayar pembeli untuk struk ini — angka rujukan utama header.
	 *
	 * <p>
	 * Nilainya sudah bersih: diskon sudah dipotong dan pajak sudah termasuk di dalamnya (lihat
	 * {@link #getPajak()}). Tiga perhitungan penting bersandar padanya:
	 * </p>
	 * <ul>
	 * <li>{@link #getLunas()} — struk dianggap lunas bila total ini tidak melebihi jumlah uang yang
	 * diterima;</li>
	 * <li>{@link #getNominalBayar1()} — nominal metode bayar slot 1 adalah sisa total ini setelah
	 * dikurangi slot 2 sampai 5;</li>
	 * <li>pemotongan saldo deposit anggota di {@code DepositHelper}, yang menghitung ekspresi yang
	 * sama langsung di SQL terhadap kolom {@code total_biaya}.</li>
	 * </ul>
	 *
	 * <p>
	 * Getter ini menormalkan {@code null} menjadi {@code 0.0} <b>dan menulisnya balik ke field</b>,
	 * karena ketiga perhitungan di atas menjumlahkannya tanpa pemeriksaan lagi.
	 * </p>
	 *
	 * @return total struk, tidak pernah {@code null}
	 */
	@Column(name = "total_biaya")
	public Double getTotalBiaya() {
		if (totalBiaya == null) {
			totalBiaya = 0.0;
		}
		return totalBiaya;
	}

	/** Nominal pajak yang sudah termasuk di {@link #getTotalBiaya()}. Lihat {@link #getPajak()}. */
	private Double pajak = 0.0;

	/**
	 * Mengisi nominal pajak struk.
	 *
	 * <p>
	 * {@code KantinHelper} membaca angka ini dari muatan JSON klien dan menjepitnya di
	 * {@code Math.max(0.0, ...)} sebelum disetel, sehingga nilai negatif tidak dapat masuk lewat
	 * jalur POS.
	 * </p>
	 *
	 * @param pajak nominal pajak; {@code null} dibaca sebagai {@code 0.0}
	 */
	public void setPajak(Double pajak) {
		this.pajak = pajak;
	}

	/** Nominal pajak (PPN) yang sudah termasuk di dalam {@link #getTotalBiaya()}. 0 bila pajak nonaktif. */
	@Column(name = "pajak")
	public Double getPajak() {
		return pajak == null ? 0.0 : pajak;
	}

	/**
	 * Mengisi nominal kembalian.
	 *
	 * @param kembalian uang kembalian yang diserahkan ke pembeli
	 */
	public void setKembalian(Double kembalian) {
		this.kembalian = kembalian;
	}

	/**
	 * Uang kembalian yang diserahkan kepada pembeli.
	 *
	 * <p>
	 * Dicatat sebagai kolom tersendiri, bukan dihitung dari {@code bayarTunai - totalBiaya}, karena
	 * kasir dapat menerima uang berlebih tanpa mengembalikan seluruh selisihnya (mis. sisanya
	 * disimpan sebagai deposit anggota). Getter ini <b>tidak</b> menjaga terhadap {@code null},
	 * sehingga tampilan yang membacanya perlu memeriksanya sendiri — dan memang begitu yang dilakukan
	 * {@code PembelianAnggotaKoperasiAction}, yang menampilkan {@code "0.0"} bila kosong.
	 * </p>
	 *
	 * @return nominal kembalian, mungkin {@code null}
	 */
	public Double getKembalian() {
		return kembalian;
	}

	/**
	 * Mengisi dasar pengenaan PPN.
	 *
	 * @param hargaPpn nilai yang dikenai PPN
	 */
	public void setHargaPpn(Double hargaPpn) {
		this.hargaPpn = hargaPpn;
	}

	/**
	 * Dasar pengenaan PPN pada struk ini, yakni nilai yang dikalikan {@link #getPpn()} untuk
	 * menghasilkan {@link #getPajak()}.
	 *
	 * @return dasar pengenaan PPN
	 */
	@Column(name = "harga_ppn")
	public Double getHargaPpn() {
		return hargaPpn;
	}

	/**
	 * Jejak penjurnalan struk ini ke buku besar.
	 *
	 * <p>
	 * Terisi begitu mesin posting penjualan kantin ({@code PostingPenjualanKantinAction}) memproses
	 * transaksi ini, dan kekosongannya adalah <b>gerbang anti-jurnal-ganda</b>: mesin posting hanya
	 * mengambil header yang {@code postingHistory}-nya masih {@code null}, dan pembatalan posting
	 * mengembalikannya ke {@code null}. Jangan mengubah properti ini dari luar mesin posting — nilai
	 * yang dihapus manual akan membuat transaksi yang sama dijurnal dua kali.
	 * </p>
	 *
	 * <p>
	 * Relasinya sengaja diambil dengan {@link FetchMode#SELECT} agar tidak ikut memperbesar
	 * {@code JOIN} pada query daftar transaksi yang jumlah barisnya besar.
	 * </p>
	 *
	 * @return riwayat posting, atau {@code null} bila belum pernah dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menautkan atau melepas jejak penjurnalan struk ini.
	 *
	 * @param postingHistory riwayat posting; {@code null} berarti belum/tidak lagi terjurnal
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Status kelunasan struk — <b>nilai turunan, selalu dihitung ulang</b>.
	 *
	 * <p>
	 * Bernilai benar bila {@link #getTotalBiaya()} tidak melebihi jumlah
	 * {@link #getBayarTunai()} + {@link #getBayarNonTunai()}. Perhatikan bahwa pembandingnya
	 * {@code <=}, bukan {@code ==}, sehingga struk yang uangnya diterima berlebih (kembaliannya belum
	 * dikurangkan) tetap terhitung lunas.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif.</b> Field {@code lunas} ditimpa hasil hitungan pada setiap pemanggilan,
	 * sehingga {@link #setLunas(Boolean)} tidak pernah berpengaruh pada nilai yang dibaca kembali —
	 * setter itu ada semata agar bentuk JavaBean-nya lengkap. Karena tidak ada anotasi kolom pada
	 * getter ini, statusnya memang tidak disimpan sebagai kolom melainkan dihitung dari angka uang di
	 * baris yang sama; konsekuensinya status kelunasan <b>tidak dapat dipakai sebagai kriteria
	 * penyaring di query basis data</b> dan harus disaring di Java setelah baris dimuat.
	 * </p>
	 *
	 * <p>
	 * Jangan bingung dengan {@code DraftPembelianAnggotaKoperasi.getLunas()} yang berbeda sama
	 * sekali: milik draft itu mengembalikan <i>header {@code PembelianAnggotaKoperasi}</i> yang
	 * melunasi draft tersebut (jadi {@code null} berarti belum dibayar), bukan sebuah boolean.
	 * </p>
	 *
	 * @return {@code true} bila uang yang diterima menutupi total struk
	 */
	public Boolean getLunas() {
		lunas = getTotalBiaya() <= (getBayarTunai() + getBayarNonTunai());
		return lunas;
	}

	/**
	 * Setter pelengkap bentuk JavaBean untuk status kelunasan.
	 *
	 * <p>
	 * <b>Tidak berpengaruh:</b> nilai apa pun yang diisi di sini akan ditimpa hasil perhitungan pada
	 * pemanggilan {@link #getLunas()} berikutnya. Untuk mengubah status kelunasan, ubahlah angka
	 * uangnya ({@link #setBayarTunai(Double)} / {@link #setBayarNonTunai(Double)} /
	 * {@link #setTotalBiaya(Double)}).
	 * </p>
	 *
	 * @param lunas diabaikan secara efektif
	 */
	public void setLunas(Boolean lunas) {
		this.lunas = lunas;
	}

	/**
	 * Total uang yang diterima untuk struk ini — <b>nilai turunan, selalu dihitung ulang</b> sebagai
	 * {@link #getBayarNonTunai()} + {@link #getBayarTunai()}.
	 *
	 * <p>
	 * <b>Getter destruktif</b> dengan pola yang sama seperti {@link #getLunas()}: field {@code bayar}
	 * ditimpa hasil penjumlahan tiap kali dipanggil, sehingga {@link #setBayar(Double)} tidak
	 * berpengaruh. Karena kedua penjumlahnya sudah dinormalkan dari {@code null}, hasilnya tidak
	 * pernah {@code null}.
	 * </p>
	 *
	 * <p>
	 * Perhatikan angka ini <b>belum</b> dikurangi {@link #getKembalian()} — ia adalah uang yang
	 * diterima, bukan uang yang tertahan di kas.
	 * </p>
	 *
	 * @return jumlah uang yang diterima
	 */
	public Double getBayar() {
		bayar = getBayarNonTunai() + getBayarTunai();
		return bayar;
	}

	/**
	 * Setter pelengkap bentuk JavaBean untuk total uang diterima.
	 *
	 * <p>
	 * <b>Tidak berpengaruh:</b> nilainya ditimpa perhitungan pada {@link #getBayar()} berikutnya.
	 * </p>
	 *
	 * @param bayar diabaikan secara efektif
	 */
	public void setBayar(Double bayar) {
		this.bayar = bayar;
	}

	/**
	 * Metode pembayaran <b>slot 1</b> — metode PERTAMA yang dipilih kasir untuk struk ini.
	 *
	 * <p>
	 * Slot 1 istimewa dibanding slot 2 sampai 5: <b>nominalnya tidak punya kolom sendiri</b>,
	 * melainkan dihitung implisit sebagai sisa {@link #getTotalBiaya()} setelah dikurangi slot 2-5
	 * (lihat {@link #getNominalBayar1()}). Rancangan ini dipilih supaya transaksi lama satu-metode —
	 * yang seluruh {@code nominalBayar2..5}-nya {@code 0}/{@code null} — berperilaku persis seperti
	 * sebelum fitur split pembayaran ada, tanpa migrasi data sama sekali.
	 * </p>
	 *
	 * <p>
	 * Metode inilah yang menentukan apakah dan berapa banyak saldo deposit anggota terpotong:
	 * {@code DepositHelper} memotong bagian sebuah slot bila metodenya <i>tidak</i> diverifikasi
	 * manual ({@code manual = false}) <b>atau</b> ditandai eksplisit memotong deposit
	 * ({@code memotong_deposit = true}).
	 * </p>
	 *
	 * <p>
	 * Nilai dilewatkan {@link GeneralValueObject#check(Object)} untuk menyelesaikan proksi malas.
	 * Berbeda dari relasi konteks seperti {@link #getToko()}, slot metode bayar <b>tidak</b> disalin
	 * ulang dari draft — metode pembayaran memang baru ditentukan pada saat membayar.
	 * </p>
	 *
	 * @return metode pembayaran slot 1, atau {@code null} bila belum ditentukan
	 * @see #getNominalBayar1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi() {
		caraPembayaranKoperasi = check(caraPembayaranKoperasi);
		return caraPembayaranKoperasi;
	}

	/**
	 * Mengisi metode pembayaran slot 1.
	 *
	 * @param caraPembayaranKoperasi metode pembayaran pertama yang dipilih kasir
	 */
	public void setCaraPembayaranKoperasi(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
	}

	/**
	 * Metode pembayaran <b>slot 2</b> (opsional), berpasangan dengan {@link #getNominalBayar2()}.
	 *
	 * <p>
	 * Hanya terisi bila kasir memecah pembayaran satu struk ke lebih dari satu instrumen (mis.
	 * separuh transfer, separuh tunai). Tetap {@code null} pada transaksi satu-metode.
	 * </p>
	 *
	 * @return metode pembayaran slot 2, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi_2", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi2() {
		caraPembayaranKoperasi2 = check(caraPembayaranKoperasi2);
		return caraPembayaranKoperasi2;
	}

	/**
	 * Mengisi metode pembayaran slot 2.
	 *
	 * @param caraPembayaranKoperasi2 metode pembayaran kedua; {@code null} bila tidak dipakai
	 */
	public void setCaraPembayaranKoperasi2(CaraPembayaranKoperasi caraPembayaranKoperasi2) {
		this.caraPembayaranKoperasi2 = caraPembayaranKoperasi2;
	}

	/**
	 * Metode pembayaran <b>slot 3</b> (opsional), berpasangan dengan {@link #getNominalBayar3()}.
	 *
	 * @return metode pembayaran slot 3, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi_3", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi3() {
		caraPembayaranKoperasi3 = check(caraPembayaranKoperasi3);
		return caraPembayaranKoperasi3;
	}

	/**
	 * Mengisi metode pembayaran slot 3.
	 *
	 * @param caraPembayaranKoperasi3 metode pembayaran ketiga; {@code null} bila tidak dipakai
	 */
	public void setCaraPembayaranKoperasi3(CaraPembayaranKoperasi caraPembayaranKoperasi3) {
		this.caraPembayaranKoperasi3 = caraPembayaranKoperasi3;
	}

	/**
	 * Metode pembayaran <b>slot 4</b> (opsional), berpasangan dengan {@link #getNominalBayar4()}.
	 *
	 * @return metode pembayaran slot 4, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi_4", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi4() {
		caraPembayaranKoperasi4 = check(caraPembayaranKoperasi4);
		return caraPembayaranKoperasi4;
	}

	/**
	 * Mengisi metode pembayaran slot 4.
	 *
	 * @param caraPembayaranKoperasi4 metode pembayaran keempat; {@code null} bila tidak dipakai
	 */
	public void setCaraPembayaranKoperasi4(CaraPembayaranKoperasi caraPembayaranKoperasi4) {
		this.caraPembayaranKoperasi4 = caraPembayaranKoperasi4;
	}

	/**
	 * Metode pembayaran <b>slot 5</b> (opsional), berpasangan dengan {@link #getNominalBayar5()}.
	 * Slot terakhir yang tersedia — satu struk paling banyak dibayar dengan lima instrumen.
	 *
	 * @return metode pembayaran slot 5, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi_5", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi5() {
		caraPembayaranKoperasi5 = check(caraPembayaranKoperasi5);
		return caraPembayaranKoperasi5;
	}

	/**
	 * Mengisi metode pembayaran slot 5.
	 *
	 * @param caraPembayaranKoperasi5 metode pembayaran kelima; {@code null} bila tidak dipakai
	 */
	public void setCaraPembayaranKoperasi5(CaraPembayaranKoperasi caraPembayaranKoperasi5) {
		this.caraPembayaranKoperasi5 = caraPembayaranKoperasi5;
	}

	/**
	 * Nominal yang dibayar lewat metode {@link #getCaraPembayaranKoperasi2()}.
	 *
	 * <p>
	 * {@code null} dinormalkan menjadi {@code 0.0} pada pengembalian — tetapi, berbeda dari
	 * {@link #getTotalBiaya()}, normalisasi ini <b>tidak</b> ditulis balik ke field, sehingga kolom
	 * basis datanya tetap {@code null} untuk transaksi lama. Itu memang yang dikehendaki: SQL
	 * {@code DepositHelper} membungkus tiap slot dengan {@code COALESCE(...,0)} sendiri, jadi
	 * kolom kosong tidak perlu dimigrasi.
	 * </p>
	 *
	 * @return nominal slot 2, tidak pernah {@code null}
	 */
	@Column(name = "nominal_bayar_2")
	public Double getNominalBayar2() {
		return nominalBayar2 == null ? 0.0 : nominalBayar2;
	}

	/**
	 * Mengisi nominal slot 2.
	 *
	 * <p>
	 * Perlu diingat nilai ini <b>mengurangi</b> nominal slot 1 yang dihitung implisit; tidak ada
	 * validasi pada tingkat entity yang mencegah jumlah slot 2-5 melampaui
	 * {@link #getTotalBiaya()} — bila itu terjadi {@link #getNominalBayar1()} dijepit ke {@code 0.0}
	 * dan kelebihannya hilang dari perhitungan.
	 * </p>
	 *
	 * @param nominalBayar2 nominal yang dibayar lewat slot 2
	 */
	public void setNominalBayar2(Double nominalBayar2) {
		this.nominalBayar2 = nominalBayar2;
	}

	/**
	 * Nominal yang dibayar lewat metode {@link #getCaraPembayaranKoperasi3()}; {@code null}
	 * dikembalikan sebagai {@code 0.0}.
	 *
	 * @return nominal slot 3, tidak pernah {@code null}
	 */
	@Column(name = "nominal_bayar_3")
	public Double getNominalBayar3() {
		return nominalBayar3 == null ? 0.0 : nominalBayar3;
	}

	/**
	 * Mengisi nominal slot 3.
	 *
	 * @param nominalBayar3 nominal yang dibayar lewat slot 3
	 */
	public void setNominalBayar3(Double nominalBayar3) {
		this.nominalBayar3 = nominalBayar3;
	}

	/**
	 * Nominal yang dibayar lewat metode {@link #getCaraPembayaranKoperasi4()}; {@code null}
	 * dikembalikan sebagai {@code 0.0}.
	 *
	 * @return nominal slot 4, tidak pernah {@code null}
	 */
	@Column(name = "nominal_bayar_4")
	public Double getNominalBayar4() {
		return nominalBayar4 == null ? 0.0 : nominalBayar4;
	}

	/**
	 * Mengisi nominal slot 4.
	 *
	 * @param nominalBayar4 nominal yang dibayar lewat slot 4
	 */
	public void setNominalBayar4(Double nominalBayar4) {
		this.nominalBayar4 = nominalBayar4;
	}

	/**
	 * Nominal yang dibayar lewat metode {@link #getCaraPembayaranKoperasi5()}; {@code null}
	 * dikembalikan sebagai {@code 0.0}.
	 *
	 * @return nominal slot 5, tidak pernah {@code null}
	 */
	@Column(name = "nominal_bayar_5")
	public Double getNominalBayar5() {
		return nominalBayar5 == null ? 0.0 : nominalBayar5;
	}

	/**
	 * Mengisi nominal slot 5.
	 *
	 * @param nominalBayar5 nominal yang dibayar lewat slot 5
	 */
	public void setNominalBayar5(Double nominalBayar5) {
		this.nominalBayar5 = nominalBayar5;
	}

	/**
	 * Nominal slot 1 (metode {@link #getCaraPembayaranKoperasi()}) SELALU dihitung implisit dari sisa
	 * {@link #getTotalBiaya()} setelah dikurangi slot 2-5 -- bukan kolom tersendiri -- supaya invarian
	 * "jumlah semua slot = totalBiaya" otomatis terjaga tanpa perlu divalidasi terpisah saat simpan.
	 */
	@Transient
	public Double getNominalBayar1() {
		double sisa = getTotalBiaya() - getNominalBayar2() - getNominalBayar3() - getNominalBayar4()
				- getNominalBayar5();
		return sisa < 0.0 ? 0.0 : sisa;
	}

	/**
	 * Setter pelengkap bentuk JavaBean untuk nominal slot 1.
	 *
	 * <p>
	 * <b>Sengaja tidak melakukan apa pun.</b> Nominal slot 1 adalah nilai turunan
	 * ({@link #getNominalBayar1()}), sehingga menyimpannya akan menciptakan kemungkinan
	 * ketidaksesuaian antara jumlah kelima slot dan {@link #getTotalBiaya()}. Method ini ada semata
	 * agar properti {@code nominalBayar1} dapat diikat oleh kerangka kerja yang mengharuskan
	 * pasangan getter/setter (pengikatan formulir ZK, serialisasi JSON dua arah).
	 * </p>
	 *
	 * @param nominalBayar1 diabaikan sepenuhnya
	 */
	public void setNominalBayar1(Double nominalBayar1) {
		// Slot 1 dihitung implisit dari totalBiaya dikurangi slot 2-5.
	}

	/**
	 * Kode pembayaran online (mis. QR/tautan bayar) yang melunasi struk ini.
	 *
	 * <p>
	 * Bila terisi, nilainya juga disalin ke setiap baris {@link Pembelian} yang dibuat
	 * {@link #simpanRinci(Session, JSONArray, String, Date, Toko, KodePembayaranOnline,
	 * DraftPembelianAnggotaKoperasi)}, sehingga rekonsiliasi dengan penyedia pembayaran dapat
	 * dilakukan dari sisi baris maupun sisi header.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif satu arah.</b> Bila draft terpasang, nilai disalin ulang dari draft; bila
	 * tidak, nilai dikembalikan apa adanya <b>tanpa</b> melewati
	 * {@link GeneralValueObject#check(Object)} — berbeda dari relasi lain di kelas ini yang punya
	 * cabang {@code else} berisi {@code check(...)}. Akibatnya, pada header tanpa draft nilai yang
	 * dikembalikan bisa berupa proksi Hibernate yang belum terinisialisasi. Relasi ini diambil dengan
	 * {@link FetchMode#SELECT} sehingga tidak memperbesar {@code JOIN} query daftar.
	 * </p>
	 *
	 * @return kode pembayaran online, atau {@code null} bila tidak lewat kanal online
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_pembayaran_online", nullable = true)
	public KodePembayaranOnline getKodePembayaranOnline() {
		if (draftPembelianAnggotaKoperasi != null) {
			kodePembayaranOnline = draftPembelianAnggotaKoperasi.getKodePembayaranOnline();
		}
		return kodePembayaranOnline;
	}

	/**
	 * Mengisi kode pembayaran online yang melunasi struk ini.
	 *
	 * <p>
	 * Diabaikan pada pembacaan bila header ini punya draft — lihat
	 * {@link #getKodePembayaranOnline()}.
	 * </p>
	 *
	 * @param kodePembayaranOnline kode pembayaran online; boleh {@code null}
	 */
	public void setKodePembayaranOnline(KodePembayaranOnline kodePembayaranOnline) {
		this.kodePembayaranOnline = kodePembayaranOnline;
	}

	/**
	 * Toko/unit usaha pemilik transaksi ini — <b>penentu batas tenancy</b> struk.
	 *
	 * <p>
	 * Toko menentukan lingkup pencarian produk saat rincian disimpan (lihat
	 * {@link #resolveOrCreateProduk(Session, String, String, String, Double, Toko)}, yang mencari
	 * produk dengan penyaring {@code toko}), dan menjadi satu-satunya pemisah antar unit usaha pada
	 * entity ini karena header ini tidak punya kolom {@code koperasi} sendiri.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif tiga cabang</b>, diperiksa berurutan:
	 * </p>
	 * <ol>
	 * <li>bila draft terpasang, toko diambil dari draft;</li>
	 * <li>bila tidak, tetapi {@link #getLokasi()} terisi dan lokasi itu punya toko, toko
	 * <b>diturunkan dari lokasi</b> — inilah sebabnya mengubah lokasi ikut memindahkan transaksi ke
	 * toko lain;</li>
	 * <li>barulah kolom {@code toko} milik header sendiri dipakai, dilewatkan
	 * {@link GeneralValueObject#check(Object)}.</li>
	 * </ol>
	 * <p>
	 * Karena urutan ini, {@link #setToko(Toko)} hanya berpengaruh pada header yang tidak punya draft
	 * <i>dan</i> tidak punya lokasi bertoko. Relasinya {@code nullable}: struk tanpa toko tetap dapat
	 * tersimpan, dan struk semacam itu tidak akan tersaring oleh penyaring toko mana pun — hal yang
	 * perlu diperhitungkan setiap query laporan.
	 * </p>
	 *
	 * @return toko pemilik transaksi, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		if (draftPembelianAnggotaKoperasi != null) {
			toko = draftPembelianAnggotaKoperasi.getToko();
		} else if (getLokasi() != null && getLokasi().getToko() != null) {
			toko = getLokasi().getToko();
		} else {
			toko = check(toko);
		}
		return toko;
	}

	/**
	 * Mengisi toko pemilik transaksi.
	 *
	 * <p>
	 * Hanya berpengaruh bila header ini tidak punya draft dan tidak punya lokasi bertoko — lihat
	 * urutan cabang di {@link #getToko()}.
	 * </p>
	 *
	 * @param toko toko pemilik transaksi; boleh {@code null}
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Total cashback yang dijanjikan struk ini kepada pembeli.
	 *
	 * <p>
	 * Cashback di sini baru berupa <b>hak</b> yang tercatat, belum uang yang berpindah. Pencairannya
	 * dicatat dokumen tersendiri ({@link PencairanDiskon}) dan itulah yang dipotong dari saldo pada
	 * perhitungan {@code DepositHelper}, bukan angka pada header ini. Jangan menjumlahkan keduanya
	 * sekaligus — hasilnya akan menghitung ganda.
	 * </p>
	 *
	 * <p>
	 * {@code null} dikembalikan sebagai {@code 0.0} tanpa ditulis balik ke field.
	 * </p>
	 *
	 * @return total cashback, tidak pernah {@code null}
	 */
	public Double getTotalCashback() {
		return totalCashback == null ? 0.0 : totalCashback;
	}

	/**
	 * Mengisi total cashback struk.
	 *
	 * @param totalCashback total cashback yang dijanjikan
	 */
	public void setTotalCashback(Double totalCashback) {
		this.totalCashback = totalCashback;
	}

	/**
	 * Meja kantin tempat pesanan struk ini disajikan.
	 *
	 * <p>
	 * Hanya relevan pada mode layanan meja (pesan di meja, bayar belakangan); struk ritel biasa
	 * membiarkannya {@code null}. Dipakai layar dapur/pramusaji untuk mengelompokkan pesanan.
	 * </p>
	 *
	 * <p>
	 * <b>Getter destruktif:</b> disalin ulang dari draft bila draft terpasang — memang yang
	 * dikehendaki, karena mejanya ditentukan saat pesanan dibuat, bukan saat dibayar. Selebihnya
	 * dilewatkan {@link GeneralValueObject#check(Object)}.
	 * </p>
	 *
	 * @return meja kantin, atau {@code null} bila bukan layanan meja
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "meja_kantin", nullable = true)
	public MejaKantin getMejaKantin() {
		if (draftPembelianAnggotaKoperasi != null) {
			mejaKantin = draftPembelianAnggotaKoperasi.getMejaKantin();
		} else {
			mejaKantin = check(mejaKantin);
		}
		return mejaKantin;
	}

	/**
	 * Mengisi meja kantin tempat pesanan disajikan.
	 *
	 * <p>
	 * Diabaikan pada pembacaan bila header ini punya draft — lihat {@link #getMejaKantin()}.
	 * </p>
	 *
	 * @param mejaKantin meja kantin; boleh {@code null}
	 */
	public void setMejaKantin(MejaKantin mejaKantin) {
		this.mejaKantin = mejaKantin;
	}

	/**
	 * Draft pesanan yang difinalisasi menjadi header ini — <b>properti paling menentukan perilaku
	 * kelas ini</b>.
	 *
	 * <p>
	 * Bila terisi, header ini lahir dari jalur "pesan dulu, bayar belakangan": pesanan sudah dicatat
	 * sebagai {@link DraftPembelianAnggotaKoperasi} beserta baris {@link DraftPembelian}-nya, lalu
	 * dipromosikan menjadi baris {@link Pembelian} definitif oleh
	 * {@link #simpanRinci(Session, JSONArray, String, Date, Toko, KodePembayaranOnline,
	 * DraftPembelianAnggotaKoperasi)}. Bila {@code null}, header ini berasal dari checkout langsung
	 * di kasir.
	 * </p>
	 *
	 * <p>
	 * Keberadaannya mengubah hasil tujuh getter lain, yang berhenti membaca kolomnya sendiri dan
	 * menyalin ulang nilai dari draft pada setiap pemanggilan: {@link #getAnggotaKoperasi()},
	 * {@link #getLokasi()}, {@link #getToko()}, {@link #getTbmuser()}, {@link #getMejaKantin()},
	 * {@link #getTanggalPembayaran()}, dan {@link #getKodePembayaranOnline()}. Karena Hibernate
	 * memakai getter yang sama saat menulis, nilai turunan itu ikut tersimpan — jadi menyunting
	 * draftnya akan mengubah pula isi kolom header pada penyimpanan berikutnya.
	 * </p>
	 *
	 * <p>
	 * Kaitan ini dua arah: dari sisi draft, {@code DraftPembelianAnggotaKoperasi.getLunas()}
	 * mengembalikan header {@code PembelianAnggotaKoperasi} yang melunasinya, dan kekosongannya
	 * dipakai sebagai gerbang agar satu draft tidak dibayar dua kali.
	 * </p>
	 *
	 * @return draft asal, atau {@code null} bila transaksi dari checkout langsung
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "draft_pembelian_anggota_koperasi", nullable = true)
	public DraftPembelianAnggotaKoperasi getDraftPembelianAnggotaKoperasi() {
		return draftPembelianAnggotaKoperasi;
	}

	/**
	 * Menautkan header ini ke draft pesanan asalnya.
	 *
	 * <p>
	 * <b>Berdampak luas:</b> begitu draft dipasang, tujuh getter konteks berhenti memakai kolom
	 * header dan beralih ke draft (lihat {@link #getDraftPembelianAnggotaKoperasi()}). Jangan
	 * memasang draft pada header yang konteksnya sudah diisi manual, kecuali memang bermaksud
	 * menimpanya.
	 * </p>
	 *
	 * @param draftPembelianAnggotaKoperasi draft asal; {@code null} untuk checkout langsung
	 */
	public void setDraftPembelianAnggotaKoperasi(DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi) {
		this.draftPembelianAnggotaKoperasi = draftPembelianAnggotaKoperasi;
	}
}
