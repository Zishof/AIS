package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — otomasi ARO simpanan berjangka (deposito).

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * <h2>DepositoRolloverKoperasi — Status ARO (Automatic Roll Over) Simpanan Berjangka</h2>
 *
 * <p>
 * Entity ini menyimpan status <b>perpanjangan otomatis (Automatic Roll Over/ARO)</b> untuk tiap
 * simpanan berjangka (deposito) koperasi. Deposito adalah simpanan bertenor: bila jatuh tempo, ada
 * dua kemungkinan — <b>diperpanjang otomatis</b> untuk satu tenor berikutnya, atau <b>dicairkan</b>.
 * Karena tanggal jatuh tempo perlu bergerak maju setiap kali diperpanjang, informasinya disimpan di
 * sini (bukan sekadar dihitung) agar penjadwal (scheduler) dapat memprosesnya secara otomatis dan
 * pengurus dapat memantau/menyetel perilakunya.
 * </p>
 *
 * <h3>Hubungan ke deposito</h3>
 * <p>
 * Satu baris mewakili satu deposito, ditautkan lewat {@link #getTransaksiKoperasiId()} (id
 * {@link TransaksiKoperasi} simpanan berjangka). Sengaja memakai id (bukan relasi objek) agar ringan
 * dan tidak menambah keterikatan audit lintas-entity. Detail anggota/nominal dibaca saat diperlukan
 * dari transaksi terkait.
 * </p>
 *
 * <h3>Perilaku otomatis</h3>
 * <ul>
 * <li>{@link #getAroOtomatis()} = true dan sudah lewat {@link #getTanggalJatuhTempo()} → jatuh tempo
 * diperpanjang satu tenor ({@link #getJangkaWaktuBulan()}), {@link #getJumlahPerpanjangan()}
 * bertambah, dan {@link #getTanggalRolloverTerakhir()} dicatat.</li>
 * <li>{@link #getAroOtomatis()} = false dan sudah lewat jatuh tempo → status menjadi
 * {@link #STATUS_JATUH_TEMPO} (menunggu pencairan manual oleh pengurus).</li>
 * </ul>
 * Perpanjangan hanya menggeser tanggal jatuh tempo (menggulung pokok); perhitungan/pembayaran bunga
 * mengikuti mekanisme bunga simpanan yang sudah ada, sehingga proses ARO bersifat aman dan tidak
 * mengubah nilai akad deposito.
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS: kunci {@code IDENTITY}, hook audit {@code @PreUpdate}, {@code @Audited},
 * getter aman-null, dan kompatibel Java 1.7. Terdaftar di {@code hibernate.cfg.xml} sehingga
 * {@code hbm2ddl=update} membuat tabel <code>koperasi.deposito_rollover</code> otomatis. Entity tidak
 * menyentuh basis data langsung dan tidak mengubah entity lain (khususnya {@link TransaksiKoperasi}
 * tidak diubah sama sekali).
 * </p>
 *
 * @see ais.action.master.koperasi.helper.DepositoAroHelper
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "deposito_rollover")
public class DepositoRolloverKoperasi extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 6620300014413771001L;

	/**
	 * Deposito masih dalam masa tenor berjalan (belum jatuh tempo, atau sudah jatuh tempo tapi ARO
	 * baru saja menggulung tanggal jatuh tempo maju). Status bawaan baris baru. Hanya baris
	 * berstatus ini yang diperiksa {@code DepositoAroHelper.proses} untuk kemungkinan rollover.
	 */
	public static final String STATUS_BERJALAN = "BERJALAN";

	/**
	 * Sudah lewat tanggal jatuh tempo dan {@link #getAroOtomatis()} bernilai {@code false}, sehingga
	 * ARO tidak menggulungnya lagi. Menunggu tindakan manual pengurus (pencairan). Status ini
	 * bersifat "diam" -- {@code DepositoAroHelper} tidak lagi memeriksa baris di status ini pada
	 * siklus berikutnya karena kueri pemrosesan hanya mengambil baris {@link #STATUS_BERJALAN}.
	 */
	public static final String STATUS_JATUH_TEMPO = "JATUH_TEMPO";

	/**
	 * Dimaksudkan sebagai status akhir siklus hidup baris ini setelah deposito dicairkan pengurus.
	 * <b>Belum ada kode di repositori ini yang men-set nilai konstanta ini</b> -- baik
	 * {@code DepositoAroHelper} maupun {@link ais.action.master.koperasi.DepositoAroKoperasiAction}
	 * hanya membaca/membandingkannya (untuk gaya tampilan), tidak pernah menulisnya. Pencairan
	 * deposito yang sesungguhnya (di layar lain, di luar paket ini) tampaknya tidak memperbarui
	 * baris {@code deposito_rollover} terkait, sehingga baris yang sudah lewat jatuh tempo dan
	 * dicairkan akan tetap tampil selamanya sebagai {@link #STATUS_JATUH_TEMPO} ("menunggu
	 * pencairan") di layar pemantauan ARO -- staleness pada data pemantauan, bukan pada saldo/bunga
	 * deposito itu sendiri (yang dikelola lewat mekanisme simpanan yang terpisah).
	 */
	public static final String STATUS_DICAIRKAN = "DICAIRKAN";

	private Long id;
	private String oleh;
	private String olehId;

	private Long transaksiKoperasiId;
	private Boolean aroOtomatis = true;
	private Date tanggalJatuhTempo;
	private Integer jangkaWaktuBulan = 0;
	private Integer jumlahPerpanjangan = 0;
	private Date tanggalRolloverTerakhir;
	private String status = STATUS_BERJALAN;
	private String keterangan;
	private Boolean aktif = true;

	/** Konstruktor bawaan (dipakai JPA/Hibernate dan saat mendaftarkan baris rollover baru). */
	public DepositoRolloverKoperasi() {
	}

	/** @param id id baris yang sudah diketahui (mis. untuk memuat ulang referensi ringan). */
	public DepositoRolloverKoperasi(Long id) {
		this.id = id;
	}

	/** @return id baris (identity, dibuat DB). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; biasanya tidak diset manual, dibuat DB saat {@code save}. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return id pengguna (audit shadow) yang terakhir menyimpan/mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId id pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *               dipertahankan) -- pola field audit shadow yang sama dipakai entity lain di
	 *               paket koperasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * @param oleh nama pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *             dipertahankan), sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna (audit shadow) yang terakhir menyimpan/mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan {@link #tanggal_dirubah} (dan field
	 * audit sejenis) ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}. Terpanggil
	 * setiap {@code UPDATE} -- termasuk saat {@code DepositoAroHelper} menggulung tanggal jatuh
	 * tempo atau mengubah status. Dipanggil otomatis oleh provider JPA, bukan untuk dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir (biasanya tidak diset manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu baris terakhir diubah; diperbarui otomatis lewat {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return id {@link TransaksiKoperasi} simpanan berjangka yang diwakili baris ini. Sengaja id
	 *         mentah (bukan {@code @ManyToOne}) -- lihat catatan desain kelas.
	 */
	@Column(name = "transaksi_koperasi_id")
	public Long getTransaksiKoperasiId() {
		return transaksiKoperasiId;
	}

	/** @param transaksiKoperasiId id transaksi simpanan berjangka yang diwakili baris ini. */
	public void setTransaksiKoperasiId(Long transaksiKoperasiId) {
		this.transaksiKoperasiId = transaksiKoperasiId;
	}

	/**
	 * @return apakah deposito ini diperpanjang otomatis (ARO) saat jatuh tempo. Fallback ke
	 *         {@code true} bila kolom {@code null} -- berbeda dari kebiasaan "opt-in default OFF"
	 *         fitur baru AIS karena field Java {@link #aroOtomatis} sendiri diinisialisasi
	 *         {@code true} (lihat deklarasi field), dan baris baru yang didaftarkan
	 *         {@code DepositoAroHelper} juga eksplisit di-set {@code true}: perilaku bawaan produk
	 *         deposito berjangka memang "diperpanjang otomatis" kecuali dimatikan pengurus.
	 */
	@Column(name = "aro_otomatis")
	public Boolean getAroOtomatis() {
		return aroOtomatis == null ? true : aroOtomatis;
	}

	/** @param aroOtomatis nyala/matikan perpanjangan otomatis untuk deposito ini. */
	public void setAroOtomatis(Boolean aroOtomatis) {
		this.aroOtomatis = aroOtomatis;
	}

	/**
	 * @return tanggal jatuh tempo berjalan. Digulung maju oleh {@code DepositoAroHelper} setiap
	 *         kali ARO memperpanjang deposito yang sudah lewat jatuh tempo; nilai ini SELALU
	 *         tanggal jatuh tempo yang aktif saat ini, bukan tanggal jatuh tempo awal akad
	 *         (tanggal awal tidak disimpan terpisah di entity ini -- lihat
	 *         {@link TransaksiKoperasi} untuk tanggal setor asli bila diperlukan).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_jatuh_tempo")
	public Date getTanggalJatuhTempo() {
		return tanggalJatuhTempo;
	}

	/** @param tanggalJatuhTempo tanggal jatuh tempo berjalan yang baru. */
	public void setTanggalJatuhTempo(Date tanggalJatuhTempo) {
		this.tanggalJatuhTempo = tanggalJatuhTempo;
	}

	/**
	 * @return tenor (jangka waktu) satu periode perpanjangan, dalam bulan. Diambil dari
	 *         {@code ProdukKoperasi.getJangkaWaktuBulan()} saat baris didaftarkan dan dipakai
	 *         berulang tiap kali ARO menggulung tanggal jatuh tempo. Fallback ke {@code 0} bila
	 *         kolom {@code null} -- nilai {@code 0} membuat {@code DepositoAroHelper} melewati
	 *         baris ini (tenor tidak valid untuk digulung).
	 */
	@Column(name = "jangka_waktu_bulan")
	public Integer getJangkaWaktuBulan() {
		return jangkaWaktuBulan == null ? 0 : jangkaWaktuBulan;
	}

	/** @param jangkaWaktuBulan tenor satu periode perpanjangan, dalam bulan. */
	public void setJangkaWaktuBulan(Integer jangkaWaktuBulan) {
		this.jangkaWaktuBulan = jangkaWaktuBulan;
	}

	/**
	 * @return berapa kali deposito ini sudah digulung ARO sepanjang riwayatnya. Bertambah
	 *         kumulatif setiap siklus {@code DepositoAroHelper} yang menggulung tanggal jatuh
	 *         tempo (bisa bertambah lebih dari satu dalam satu siklus bila deposito terlewat
	 *         beberapa tenor sekaligus, mis. setelah penjadwal sempat berhenti lama). Fallback ke
	 *         {@code 0} bila kolom {@code null}.
	 */
	@Column(name = "jumlah_perpanjangan")
	public Integer getJumlahPerpanjangan() {
		return jumlahPerpanjangan == null ? 0 : jumlahPerpanjangan;
	}

	/** @param jumlahPerpanjangan jumlah kumulatif perpanjangan ARO. */
	public void setJumlahPerpanjangan(Integer jumlahPerpanjangan) {
		this.jumlahPerpanjangan = jumlahPerpanjangan;
	}

	/** @return waktu perpanjangan ARO paling akhir dilakukan; {@code null} bila belum pernah digulung. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_rollover_terakhir")
	public Date getTanggalRolloverTerakhir() {
		return tanggalRolloverTerakhir;
	}

	/** @param tanggalRolloverTerakhir waktu perpanjangan ARO paling akhir. */
	public void setTanggalRolloverTerakhir(Date tanggalRolloverTerakhir) {
		this.tanggalRolloverTerakhir = tanggalRolloverTerakhir;
	}

	/**
	 * @return status siklus hidup ARO: {@link #STATUS_BERJALAN}, {@link #STATUS_JATUH_TEMPO}, atau
	 *         {@link #STATUS_DICAIRKAN} (lihat catatan pada konstanta itu). Fallback ke
	 *         {@link #STATUS_BERJALAN} bila kolom kosong/{@code null}.
	 */
	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_BERJALAN : status;
	}

	/** @param status status baru; gunakan salah satu konstanta {@code STATUS_*} kelas ini. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return catatan bebas pengurus terkait baris rollover ini (opsional). */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan bebas pengurus. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return apakah baris ini masih dipantau otomasi ARO. Fallback ke {@code true} bila kolom
	 *         {@code null}. Dipakai kueri {@code DepositoAroHelper.proses} sebagai filter tambahan
	 *         ({@code r.aktif is null or r.aktif = true}) di luar filter status -- flag nonaktif
	 *         satu-arah untuk menghentikan pemantauan baris tertentu tanpa menghapusnya (mis. data
	 *         uji/duplikat), berbeda dari field {@link #status} yang menggambarkan siklus hidup ARO
	 *         itu sendiri.
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif nyala/matikan pemantauan ARO untuk baris ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return representasi ringkas untuk log/debug: id transaksi koperasi dan tanggal jatuh tempo berjalan. */
	@Override
	public String toString() {
		return "DepositoRollover[tx=" + transaksiKoperasiId + ", jatuhTempo=" + tanggalJatuhTempo + "]";
	}
}
