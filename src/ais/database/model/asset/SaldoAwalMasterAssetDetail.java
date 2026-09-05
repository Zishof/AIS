package ais.database.model.asset;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;

/**
 * <h2>SaldoAwalMasterAssetDetail — baris item (barang/jasa) pada dokumen tagihan/penerimaan
 * pengadaan aset {@link SaldoAwalMasterAsset}.</h2>
 *
 * <p>
 * Meski namanya menyiratkan "saldo awal" (opening balance saat migrasi), entity ini sebenarnya
 * berfungsi sebagai <b>baris rincian tagihan vendor</b> di dalam alur pengadaan aset: setiap baris
 * memuat satu jenis {@link MasterAsset} beserta jumlah, harga, diskon, dan pajak (PPN/PPh)-nya.
 * Header {@link #getSaldoAwal() saldoAwal} menyimpan info tanggal, persetujuan, dan status lunas
 * dokumen; baris-baris di kelas ini menjumlah menjadi nilai total dokumen tersebut. Sebagian besar
 * getter di sini bersifat <i>derivatif</i>: mereka lebih dulu mencoba mewarisi nilai dari
 * {@link #getPenerimaanPengadaanMasterAssetDetail() penerimaanPengadaanMasterAssetDetail} (baris
 * BAST/penerimaan barang asal) sebelum jatuh ke field lokal — pola "isi otomatis dari dokumen
 * hulu, tapi tetap bisa ditimpa manual" yang berulang di banyak entity paket ini.
 * </p>
 *
 * <h3>Mode "Termin" berbasis JSON (pembayaran termin pekerjaan)</h3>
 * <p>
 * Bila {@link SaldoAwalMasterAsset#getJsonTermin()} terisi (dokumen berjenis pembayaran termin,
 * bukan penerimaan barang biasa), {@link #getJumlah()} dan {@link #getHarga()} mengambil jalur
 * perhitungan berbeda: {@code jumlah} dipaksa {@code 1.0} (baris termin dihitung sebagai satu unit
 * "pekerjaan", bukan kuantitas barang), dan {@code harga} diambil dari field {@code penagihan} pada
 * JSON tersebut, lalu DIPRORATE (dibagi proporsional) terhadap nilai BAST induk berdasarkan
 * proporsi harga-satuan-dikali-qty tiap baris terhadap total nilai BAST — supaya saat sebuah BAST
 * memiliki banyak baris item, penagihan/PPh tidak tergandakan N kali (sekali per baris) melainkan
 * terbagi wajar sesuai kontribusi nilai masing-masing baris.
 * </p>
 *
 * <h3>Perhitungan pajak &amp; total baris</h3>
 * <p>
 * {@link #hitungPpn()}/{@link #hitungPph()} adalah kalkulasi PPN/PPh MURNI (dipakai untuk tampilan
 * kolom, TIDAK menuliskan hasil ke field manapun), sedangkan {@link #getHargaTotal()} adalah nilai
 * akhir baris (DPP + PPN − PPh, dengan PPh yang tunduk pada konfigurasi global
 * {@code pph_mengurangi_lpj}). Ketiganya mengulang rumus DPP/diskon yang sama secara independen
 * (bukan saling memanggil) demi menghindari efek samping saat dipanggil dari proses dirty-check
 * Hibernate — lihat catatan detail pada javadoc masing-masing method.
 * </p>
 *
 * <h3>Pemetaan basis data &amp; audit</h3>
 * <p>
 * Dipetakan ke tabel <code>asset.saldo_awal_master_asset_detail</code>. Field jejak {@code oleh}/
 * {@code olehId}/{@code tanggal_dirubah} diisi otomatis lewat hook
 * {@link javax.persistence.PreUpdate} {@link #onUpdate()}
 * ({@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}), dan setiap perubahan
 * direkam ke tabel revisi Envers karena kelas ditandai {@link org.hibernate.envers.Audited @Audited}.
 * </p>
 *
 * @author AIS
 * @see SaldoAwalMasterAsset
 * @see MasterAsset
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "saldo_awal_master_asset_detail")
public class SaldoAwalMasterAssetDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.saldo_awal_master_asset_detail}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * @return id pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila belum
	 *         pernah diubah sejak dimuat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Nilai {@code null}/kosong diabaikan agar jejak lama tidak
	 * tertimpa hampa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi ringkas {@code id-masterAsset} untuk log/debug. Memanggil {@link #getMasterAsset()}
	 * (bukan langsung field) sehingga proxy lazy sempat diresolusi lebih dulu.
	 *
	 * @return teks ringkas berisi id baris dan representasi aset terkait.
	 */
	public String toString() {
		masterAsset = getMasterAsset();
		return id + "-" + masterAsset + "";
	}

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila
	 *         belum pernah diubah sejak dimuat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu perubahan terakhir baris ini; diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Aset yang menjadi objek baris ini; bisa diwarisi dari BAST asal, lihat {@link #getMasterAsset()}. */
	private MasterAsset masterAsset;
	/** Kuantitas baris; punya jalur khusus mode termin, lihat {@link #getJumlah()}. */
	private Double jumlah;
	/** Harga satuan baris; punya jalur khusus mode termin (prorata), lihat {@link #getHarga()}. */
	private Double harga;
	/** Dokumen tagihan/penerimaan pengadaan induk (header) tempat baris ini berada. */
	private SaldoAwalMasterAsset saldoAwal;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Penanda tampilan "data per master asset" (bukan per baris pengadaan); default {@code false}. */
	private Boolean dataPerMasterAsset = false;

	/** Aset spesifik (instance/unit) yang tercatat dari baris ini, bila sudah diaset-kan; opsional. */
	private Asset asset;
	/** Baris BAST/penerimaan pengadaan asal; sumber banyak nilai turunan pada kelas ini. */
	private PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail;
	/** Potongan/diskon baris; bisa nominal atau persen, lihat {@link #getDiskonDalamBentukPersen()}. */
	private Double hargaPotongan;
	/** Total baris hasil {@link #getHargaTotal()}; field murni cache, tidak dipakai getter itu sendiri. */
	private Double hargaTotal;
	/** Riwayat posting jurnal akuntansi baris ini, bila sudah diposting; opsional. */
	private PostingHistory postingHistory;
	/** {@code true}=diskon dalam persen (default), {@code false}=diskon nominal tetap. */
	private Boolean diskonDalamBentukPersen;
	/** Persentase PPh baris; bisa diwarisi dari {@link #getJenisPajakBarang()}. */
	private Double persenPph;
	/** Persentase PPN baris; bisa diwarisi dari {@link #getJenisPajakPpn()}. */
	private Double persenPpn;
	/** Jenis pajak barang (menentukan {@link #getPersenPph()} bila diisi). */
	private JenisPajakBarang jenisPajakBarang;
	/** Jenis pajak PPN (menentukan {@link #getPersenPpn()} bila diisi). */
	private JenisPajakPpn jenisPajakPpn;
	/** Workspace terkait; salah satu fallback penentu {@link #getSatuanKerja()}. */
	private Workspace workspace;
	/** Satuan kerja pemilik baris; biasanya diwarisi dari rantai dokumen hulu, lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Kode unik baris, dibuat otomatis via {@link Common#getGeneratedBarCode()} bila kosong. */
	private String kodeUnik;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public SaldoAwalMasterAssetDetail() {
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menghitung nilai PPN (nominal, dalam satuan mata uang) baris ini murni untuk kebutuhan
	 * TAMPILAN (mis. kolom "PPN" pada tabel rincian), TANPA menuliskan hasilnya ke field manapun
	 * pada objek ini — berbeda dari {@link #getHargaTotal()} yang menghitung ulang komponen serupa
	 * lalu menyimpannya sebagai total akhir baris.
	 *
	 * <p><b>Rumus:</b> {@code DPP = jumlah × harga}, lalu {@code potongan = diskonDalamBentukPersen
	 * ? (hargaPotongan / 100) × DPP : hargaPotongan} dikurangkan dari DPP, dan akhirnya
	 * {@code PPN = (persenPpn / 100) × DPP} (setelah dikurangi potongan). Seluruh field sumber
	 * ({@link #jumlah}, {@link #harga}, {@link #hargaPotongan}, {@link #persenPpn},
	 * {@link #persenPph}) dibaca LANGSUNG sebagai field privat (bukan lewat getter-nya yang bisa
	 * memiliki logika fallback ke relasi lain) dan nilai {@code null} diperlakukan sebagai
	 * {@code 0.0} — sengaja demikian karena method ini turut dipanggil dari proses dirty-check
	 * Hibernate (comparing state semasa flush), sehingga memanggil getter relasi (mis.
	 * {@link #getSaldoAwal()}/{@link #getPenerimaanPengadaanMasterAssetDetail()}) di titik itu
	 * berisiko menginisialisasi proxy Hibernate yang sesinya sudah tertutup
	 * (LazyInitializationException).</p>
	 *
	 * <p>Catatan penting: method ini TIDAK ikut menerapkan jalur perhitungan khusus mode "Termin"
	 * (perataan {@code penagihan} dari JSON pada {@link #getHarga()}/{@link #getJumlah()}) karena
	 * ia membaca field {@link #harga}/{@link #jumlah} mentah, bukan lewat getter publiknya. Pada
	 * baris bermode termin, nilai PPN yang dikembalikan di sini karenanya bisa berbeda dari nilai
	 * yang tersirat oleh {@link #getHarga()}/{@link #getJumlah()} — pemanggil yang membutuhkan PPN
	 * konsisten dengan mode termin perlu menghitungnya sendiri dari nilai getter publik, bukan
	 * mengandalkan method ini.</p>
	 *
	 * @return nilai PPN baris ini (selalu non-negatif bila seluruh input non-negatif); tidak
	 *         pernah {@code null}.
	 */
	public Double hitungPpn() {


		/* Gunakan field skalar langsung. Getter ini dibaca juga oleh dirty-check Hibernate;
		 * memanggil getter relasi dari sini dapat menginisialisasi proxy yang sudah detached. */
		double jumlahAman = jumlah == null ? 0.0 : jumlah.doubleValue();
		double hargaAman = harga == null ? 0.0 : harga.doubleValue();
		double potonganAman = hargaPotongan == null ? 0.0 : hargaPotongan.doubleValue();
		double persenPpnAman = persenPpn == null ? 0.0 : persenPpn.doubleValue();
		double persenPphAman = persenPph == null ? 0.0 : persenPph.doubleValue();
		Double dpp = Double.valueOf(jumlahAman * hargaAman);

		Double potongan = getDiskonDalamBentukPersen() ? Double.valueOf((potonganAman / 100.0) * dpp)
				: Double.valueOf(potonganAman);
		dpp = dpp - potongan;

		Double ppn = Double.valueOf((persenPpnAman / 100.0) * dpp);

		return ppn;
	}

	/**
	 * Menghitung nilai PPh (nominal) baris ini murni untuk kebutuhan TAMPILAN, dengan rumus DPP
	 * dan diskon yang identik dengan {@link #hitungPpn()} (lihat javadoc method itu untuk detail
	 * lengkap rumus DPP/potongan serta alasan membaca field skalar langsung, bukan lewat getter).
	 *
	 * <p><b>Perbedaan penting dengan {@link #getHargaTotal()}:</b> method ini SELALU mengembalikan
	 * nilai PPh nominal terlepas dari konfigurasi global {@code pph_mengurangi_lpj} — nilai PPh
	 * tetap perlu DITAMPILKAN di kolom kalkulasi meski konfigurasi itu menonaktifkan pengurangan
	 * PPh terhadap total LPJ. Apakah PPh benar-benar MENGURANGI total akhir baris/dokumen adalah
	 * keputusan TERPISAH yang diambil oleh {@link #getHargaTotal()} (mengecek
	 * {@code pph_mengurangi_lpj}) dan oleh {@code PertangungjawabanAction} di sisi laporan
	 * pertanggungjawaban — bukan oleh method ini. Karena itu, komentar asli pada method ini
	 * menegaskan "PPh JANGAN di-nol-kan di sini" walau konfigurasi tersebut nonaktif.</p>
	 *
	 * @return nilai PPh baris ini (nominal, sebelum mempertimbangkan konfigurasi
	 *         {@code pph_mengurangi_lpj}); tidak pernah {@code null}.
	 */
	public Double hitungPph() {
		// N.PPH = NILAI PPH (nominal) untuk DITAMPILKAN di kolom. Apakah PPH mengurangi
		// total/LPJ dikontrol TERPISAH oleh konfigurasi pph_mengurangi_lpj di
		// getHargaTotal() dan PertangungjawabanAction. Jadi di sini PPH JANGAN di-nol-kan,
		// supaya nilai PPH tetap tampil walau konfigurasi tersebut nonaktif.
		/* Gunakan field skalar langsung. Getter ini juga dibaca oleh dirty-check
		 * Hibernate dan tidak boleh memicu inisialisasi relasi/proxy detached. */
		double jumlahAman = jumlah == null ? 0.0 : jumlah.doubleValue();
		double hargaAman = harga == null ? 0.0 : harga.doubleValue();
		double potonganAman = hargaPotongan == null ? 0.0 : hargaPotongan.doubleValue();
		double persenPpnAman = persenPpn == null ? 0.0 : persenPpn.doubleValue();
		double persenPphAman = persenPph == null ? 0.0 : persenPph.doubleValue();
		Double dpp = Double.valueOf(jumlahAman * hargaAman);

		Double potongan = getDiskonDalamBentukPersen() ? Double.valueOf((potonganAman / 100.0) * dpp)
				: Double.valueOf(potonganAman);
		dpp = dpp - potongan;

		Double pph = Double.valueOf((persenPphAman / 100.0) * dpp);

		return pph;
	}

	/** @return keterangan bebas baris ini; tidak pernah {@code null}, default string kosong. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi aset baris ini secara langsung. Nilai ini hanya berpengaruh sebagai fallback pada
	 * {@link #getMasterAsset()} bila {@link #penerimaanPengadaanMasterAssetDetail} kosong.
	 *
	 * @param masterAsset aset terkait, boleh {@code null}.
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * Mengembalikan aset baris ini. Bila {@link #penerimaanPengadaanMasterAssetDetail} terisi,
	 * aset SELALU diwarisi dari sana (mengabaikan field {@link #masterAsset} lokal — pola "sumber
	 * kebenaran ada di dokumen hulu"); jika tidak, field lokal dikembalikan setelah diresolusi
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link MasterAsset} baris ini, atau {@code null} bila tidak ada di kedua sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "master_asset", nullable = true)
	public MasterAsset getMasterAsset() {

		if (penerimaanPengadaanMasterAssetDetail != null) {
			masterAsset = penerimaanPengadaanMasterAssetDetail.getMasterAsset();
		} else {
			masterAsset = check(masterAsset);
		}

		return masterAsset;
	}

	/**
	 * Mengisi kuantitas baris secara langsung. Nilai ini bisa ditimpa oleh logika fallback pada
	 * {@link #getJumlah()} (mode termin memaksa {@code 1.0}; bila kosong, diwarisi dari BAST asal).
	 *
	 * @param jumlah kuantitas baris, boleh {@code null}.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas baris, dengan jalur khusus untuk dokumen bermode "Termin" (lihat
	 * javadoc kelas). Bila {@link #saldoAwal} terisi dan {@link SaldoAwalMasterAsset#getJsonTermin()}
	 * tidak {@code null} (dokumen ini pembayaran termin pekerjaan), kuantitas SELALU dipaksa
	 * {@code 1.0} — baris termin dihitung sebagai satu unit "pekerjaan/tahap", bukan kuantitas
	 * barang fisik. Di luar mode termin, bila field {@link #jumlah} masih kosong, diwarisi dari
	 * {@link #penerimaanPengadaanMasterAssetDetail}{@code .getDiterima()} (jumlah barang yang
	 * benar-benar diterima pada BAST asal); bila itu pun kosong, jatuh ke default {@code 1.0}.
	 *
	 * <p>Catatan efek samping: seperti getter lain di kelas ini, method ini menulis balik ke field
	 * {@link #jumlah} (bukan murni pembacaan) — nilai yang tadinya {@code null} akan "terkunci"
	 * menjadi hasil fallback begitu getter ini dipanggil sekali.</p>
	 *
	 * @return kuantitas baris; tidak pernah {@code null}.
	 */
	public Double getJumlah() {

		if (saldoAwal != null && saldoAwal.getJsonTermin() != null) {
			jumlah = 1.0;
		} else {

			if (jumlah == null && penerimaanPengadaanMasterAssetDetail != null) {
				jumlah = penerimaanPengadaanMasterAssetDetail.getDiterima();
			}

			if (jumlah == null) {
				jumlah = 1.0;
			}

		}
		return jumlah;
	}

	/**
	 * Mengisi dokumen tagihan/penerimaan pengadaan induk secara langsung. Nilai ini hanya
	 * berpengaruh sebagai fallback pada {@link #getSaldoAwal()} bila field ini masih {@code null}.
	 *
	 * @param saldoAwal dokumen induk, boleh {@code null}.
	 */
	public void setSaldoAwal(SaldoAwalMasterAsset saldoAwal) {
		this.saldoAwal = saldoAwal;
	}

	/**
	 * Mengembalikan dokumen tagihan/penerimaan pengadaan induk. Bila field {@link #saldoAwal}
	 * belum diisi, ditelusuri dari {@link #penerimaanPengadaanMasterAssetDetail} →
	 * {@code getPenerimaanPengadaanMasterAsset()} → {@code getSaldoAwalMasterAsset()} — rantai
	 * navigasi tiga tingkat yang memungkinkan baris ini "menemukan" header induknya walau relasi
	 * langsungnya belum di-set secara eksplisit (mis. saat objek baru dibuat dari BAST).
	 *
	 * @return {@link SaldoAwalMasterAsset} induk, atau {@code null} bila tidak ditemukan di kedua
	 *         sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_master_asset", nullable = true)
	public SaldoAwalMasterAsset getSaldoAwal() {
		if (saldoAwal == null) {
			if (penerimaanPengadaanMasterAssetDetail != null
					&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
					&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getSaldoAwalMasterAsset() != null) {
				saldoAwal = penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
						.getSaldoAwalMasterAsset();
			}
		}
		return saldoAwal;
	}

	/** @return penanda tampilan "data per master asset"; boleh {@code null} (default {@code false} saat objek baru). */
	@Column(name = "data_per_master_asset", nullable = true)
	public Boolean getDataPerMasterAsset() {
		return dataPerMasterAsset;
	}

	/**
	 * Mengisi penanda "data per master asset".
	 *
	 * @param dataPerMasterAsset nilai penanda, boleh {@code null}.
	 */
	public void setDataPerMasterAsset(Boolean dataPerMasterAsset) {
		this.dataPerMasterAsset = dataPerMasterAsset;
	}

	/**
	 * Mengembalikan harga satuan baris ini, dengan tiga lapis fallback berurutan.
	 *
	 * <p><b>Lapis 1 (harga default):</b> bila field {@link #harga} masih kosong, diwarisi dari
	 * {@link #penerimaanPengadaanMasterAssetDetail}{@code .getHargaBeli()} (harga beli pada BAST
	 * asal).</p>
	 *
	 * <p><b>Lapis 2 (mode Termin, menimpa lapis 1):</b> bila {@link #saldoAwal} terisi dan
	 * {@link SaldoAwalMasterAsset#getJsonTermin()} tidak {@code null} (dokumen pembayaran termin
	 * pekerjaan), harga dihitung ULANG dari field {@code penagihan} pada JSON tersebut, LALU
	 * DIPRORATE (dibagi proporsional) terhadap {@code itemNilai / bastNilai} — di mana
	 * {@code bastNilai} adalah total nilai BAST induk, dan {@code itemNilai} adalah
	 * {@code qtyDiterima × hargaBeliSatuan} baris ini (harga satuan diambil dari pesanan
	 * pengadaan, atau bila kosong dari harga beli default {@link MasterAsset}). Tujuan proration
	 * ini: mencegah nilai {@code penagihan} tunggal pada dokumen termin tergandakan penuh N kali
	 * (sekali per baris) ketika BAST memiliki banyak baris item — setiap baris hanya menyerap
	 * porsi penagihan sebanding kontribusi nilainya. Bila {@code bastNilai}/{@code itemNilai}
	 * tidak valid (≤ 0.01) atau baris BAST tidak tersedia, seluruh {@code penagihan} dikembalikan
	 * apa adanya tanpa proration (fallback aman).</p>
	 *
	 * <p><b>Lapis 3 (jaring pengaman terakhir):</b> seluruh proses di atas dibungkus
	 * {@code try/catch} generik (exception dicatat ke {@link ais.common.ErrorAuditUtil} DAN
	 * dicetak ke stderr — satu-satunya method di kelas ini yang melakukan keduanya) sehingga
	 * kegagalan parsing JSON/navigasi relasi tidak menjatuhkan pemanggil; dan hasil akhir yang
	 * masih {@code null} dipaksa menjadi {@code 0.0} sebelum dikembalikan.</p>
	 *
	 * <p>Seperti getter lain di kelas ini, method ini menulis balik ke field {@link #harga} —
	 * bukan murni pembacaan.</p>
	 *
	 * @return harga satuan baris ini; tidak pernah {@code null}.
	 */
	public Double getHarga() {

		if (harga == null && penerimaanPengadaanMasterAssetDetail != null) {
			harga = penerimaanPengadaanMasterAssetDetail.getHargaBeli();
		}

		try {
			if (saldoAwal != null && saldoAwal.getJsonTermin() != null) {
				JSONObject jsonObject = new JSONObject(saldoAwal.getJsonTermin());
				Double penagihan = 0.0;
				if (!jsonObject.isNull("penagihan")) {
					penagihan = jsonObject.getDouble("penagihan");
				}
				// Distribusikan penagihan proporsional per item BAST agar PPh tidak
				// menggelembung N× saat BAST memiliki banyak item
				if (penerimaanPengadaanMasterAssetDetail != null
						&& penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null) {
					Double bastNilai = penerimaanPengadaanMasterAssetDetail
							.getPenerimaanPengadaanMasterAsset().getNilai();
					Double unitPrice = 0.0;
					if (penerimaanPengadaanMasterAssetDetail.getPemesananPengadaanMasterAssetDetail() != null) {
						unitPrice = penerimaanPengadaanMasterAssetDetail
								.getPemesananPengadaanMasterAssetDetail().getHargaBeli();
					}
					if (unitPrice <= 0.0 && penerimaanPengadaanMasterAssetDetail.getMasterAsset() != null) {
						unitPrice = penerimaanPengadaanMasterAssetDetail.getMasterAsset().getHargaBeliDefault();
					}
					Double qty = penerimaanPengadaanMasterAssetDetail.getDiterima();
					Double itemNilai = (qty == null ? 1.0 : qty) * (unitPrice == null ? 0.0 : unitPrice);
					if (bastNilai != null && bastNilai > 0.01 && itemNilai > 0.01) {
						harga = penagihan * (itemNilai / bastNilai);
					} else {
						harga = penagihan;
					}
				} else {
					harga = penagihan;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/asset/SaldoAwalMasterAssetDetail.java:277");
		}

		if (harga == null) {
			harga = 0.0;
		}
		return harga;
	}

	/**
	 * Mengisi harga satuan secara langsung. Nilai ini bisa ditimpa oleh logika fallback pada
	 * {@link #getHarga()} (lapis 1: warisan BAST; lapis 2: perataan mode termin).
	 *
	 * @param harga harga satuan, boleh {@code null}.
	 */
	public void setHarga(Double harga) {
		this.harga = harga;
	}

	/** @return aset spesifik (instance/unit) yang tercatat dari baris ini, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset", nullable = true)
	public Asset getAsset() {
		return asset;
	}

	/**
	 * Mengisi aset spesifik.
	 *
	 * @param asset aset terkait, boleh {@code null}.
	 */
	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	/** @return baris BAST/penerimaan pengadaan asal, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset_detail", nullable = true)
	public PenerimaanPengadaanMasterAssetDetail getPenerimaanPengadaanMasterAssetDetail() {
		return penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Mengisi baris BAST/penerimaan pengadaan asal.
	 *
	 * @param penerimaanPengadaanMasterAssetDetail baris sumber terkait, boleh {@code null}.
	 */
	public void setPenerimaanPengadaanMasterAssetDetail(
			PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail) {
		this.penerimaanPengadaanMasterAssetDetail = penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Mengembalikan potongan/diskon baris ini. Bila field {@link #hargaPotongan} masih kosong,
	 * diwarisi dari {@link #penerimaanPengadaanMasterAssetDetail}{@code .getHargaPotongan()}.
	 *
	 * @return potongan baris ini (nominal atau persen, lihat {@link #getDiskonDalamBentukPersen()});
	 *         tidak pernah {@code null}, default {@code 0.0}.
	 */
	public Double getHargaPotongan() {
		if (hargaPotongan == null && penerimaanPengadaanMasterAssetDetail != null) {
			hargaPotongan = penerimaanPengadaanMasterAssetDetail.getHargaPotongan();
		}
		return hargaPotongan == null ? 0.0 : hargaPotongan;
	}

	/**
	 * Mengisi potongan/diskon baris secara langsung. Nilai ini bisa ditimpa oleh fallback pada
	 * {@link #getHargaPotongan()} bila masih kosong.
	 *
	 * @param hargaPotongan potongan baris, boleh {@code null}.
	 */
	public void setHargaPotongan(Double hargaPotongan) {
		this.hargaPotongan = hargaPotongan;
	}

	/** @return riwayat posting jurnal akuntansi baris ini, boleh {@code null} bila belum diposting. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Mengisi riwayat posting jurnal.
	 *
	 * @param postingHistory riwayat posting, boleh {@code null}.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan persentase PPN baris ini, dengan dua lapis fallback berurutan: pertama
	 * diwarisi dari {@link #penerimaanPengadaanMasterAssetDetail}{@code .getPersenPpn()} bila
	 * field {@link #persenPpn} masih kosong; KEMUDIAN, bila {@link #getJenisPajakPpn()}
	 * menghasilkan nilai, persentase tersebut MENIMPA hasil warisan di atas (jenis pajak PPN
	 * eksplisit selalu menang atas warisan BAST).
	 *
	 * @return persentase PPN (0-100); tidak pernah {@code null}, default {@code 0.0}.
	 */
	public Double getPersenPpn() {

		if (persenPpn == null && penerimaanPengadaanMasterAssetDetail != null) {
			persenPpn = penerimaanPengadaanMasterAssetDetail.getPersenPpn();
		}

		if (getJenisPajakPpn() != null) {
			persenPpn = getJenisPajakPpn().getPersen();
		}

		return persenPpn == null ? 0.0 : persenPpn;
	}

	/**
	 * Mengisi persentase PPN secara langsung. Nilai ini bisa ditimpa oleh fallback pada
	 * {@link #getPersenPpn()}.
	 *
	 * @param persenPpn persentase PPN, boleh {@code null}.
	 */
	public void setPersenPpn(Double persenPpn) {
		this.persenPpn = persenPpn;
	}

	/**
	 * Mengembalikan jenis pajak barang baris ini, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}. Bila kosong, diwarisi dari
	 * {@link #penerimaanPengadaanMasterAssetDetail}{@code .getJenisPajakBarang()}.
	 *
	 * @return {@link JenisPajakBarang} baris ini (menentukan {@link #getPersenPph()} bila diisi),
	 *         atau {@code null} bila tidak ada di kedua sumber.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak_barang", nullable = true)
	public JenisPajakBarang getJenisPajakBarang() {
		jenisPajakBarang = check(jenisPajakBarang);
		if (jenisPajakBarang == null && penerimaanPengadaanMasterAssetDetail != null
				&& penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang() != null) {
			jenisPajakBarang = penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang();
		}
		return jenisPajakBarang;
	}

	/**
	 * Mengisi jenis pajak barang secara langsung. Nilai ini bisa ditimpa oleh fallback pada
	 * {@link #getJenisPajakBarang()}.
	 *
	 * @param jenisPajakBarang jenis pajak barang, boleh {@code null}.
	 */
	public void setJenisPajakBarang(JenisPajakBarang jenisPajakBarang) {
		this.jenisPajakBarang = jenisPajakBarang;
	}

	/**
	 * Mengembalikan jenis pajak PPN baris ini, dengan tiga lapis penentuan (yang terakhir
	 * menang bila kondisinya terpenuhi):
	 * <ol>
	 *   <li>Bila field {@link #jenisPajakPpn} masih kosong dan {@link #persenPpn} kebetulan
	 *       bernilai {@code 11} (persen), diasumsikan {@link JenisPajakPpn#PPN}; jika tidak,
	 *       field yang ada diresolusi lewat {@link GeneralValueObject#check(Object)}.</li>
	 *   <li>Bila dokumen induk ({@link #getSaldoAwal()}) merupakan pembelian langsung
	 *       ({@code PemesananPengadaanMasterAsset.getPembelianLangsung()}), jenis PPN SELALU
	 *       diganti dengan jenis PPN khusus DP dokumen tersebut ({@code getJenisPajakPpnDp()}) —
	 *       menimpa hasil langkah 1 tanpa syarat.</li>
	 *   <li>Jika tidak (bukan pembelian langsung) dan {@link #getPenerimaanPengadaanMasterAssetDetail()}
	 *       memiliki jenis PPN sendiri, jenis tersebut yang dipakai.</li>
	 * </ol>
	 *
	 * @return {@link JenisPajakPpn} hasil penentuan, atau {@code null} bila tidak satu pun
	 *         kondisi di atas menghasilkan nilai.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak_ppn", nullable = true)
	public JenisPajakPpn getJenisPajakPpn() {
		if (jenisPajakPpn == null && persenPpn != null && persenPpn.intValue() == 11) {
			jenisPajakPpn = JenisPajakPpn.PPN;
		} else {
			jenisPajakPpn = check(jenisPajakPpn);
		}

		if (getSaldoAwal() != null && getSaldoAwal().getPenerimaanPengadaanMasterAsset() != null
				&& getSaldoAwal().getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null
				&& getSaldoAwal().getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
						.getPembelianLangsung()) {
			jenisPajakPpn = getSaldoAwal().getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()
					.getJenisPajakPpnDp();
		}

		else if (getPenerimaanPengadaanMasterAssetDetail() != null
				&& getPenerimaanPengadaanMasterAssetDetail().getJenisPajakPpn() != null) {
			jenisPajakPpn = getPenerimaanPengadaanMasterAssetDetail().getJenisPajakPpn();
		}

		return jenisPajakPpn;
	}

	/**
	 * Mengisi jenis pajak PPN, sekaligus menyinkronkan {@link #persenPpn} ke persentase jenis
	 * pajak tersebut (efek samping yang disengaja — memastikan {@link #getPersenPpn()} langsung
	 * konsisten tanpa menunggu getter dipanggil ulang).
	 *
	 * @param jenisPajakPpn jenis pajak PPN; {@code null} mengatur {@link #persenPpn} ke {@code 0.0}.
	 */
	public void setJenisPajakPpn(JenisPajakPpn jenisPajakPpn) {
		setPersenPpn(jenisPajakPpn == null ? 0.0 : jenisPajakPpn.getPersen());
		this.jenisPajakPpn = jenisPajakPpn;
	}

	/**
	 * Mengembalikan persentase PPh baris ini, dengan dua lapis fallback: pertama diambil dari
	 * persentase {@link #getJenisPajakBarang()} bila jenis pajak barang tersedia (menimpa nilai
	 * field {@link #persenPph} tanpa syarat); bila jenis pajak barang kosong DAN field
	 * {@link #persenPph} juga masih kosong, diwarisi dari
	 * {@link #penerimaanPengadaanMasterAssetDetail}{@code .getPersenPph()}.
	 *
	 * @return persentase PPh (0-100); tidak pernah {@code null}, default {@code 0.0}.
	 */
	public Double getPersenPph() {

		if (getJenisPajakBarang() != null) {
			persenPph = jenisPajakBarang.getPersen();
		}

		if (persenPph == null && penerimaanPengadaanMasterAssetDetail != null) {
			persenPph = penerimaanPengadaanMasterAssetDetail.getPersenPph();
		}
		return persenPph == null ? 0.0 : persenPph;
	}

	/**
	 * Mengisi persentase PPh secara langsung. Nilai ini bisa ditimpa oleh fallback pada
	 * {@link #getPersenPph()} (jenis pajak barang selalu menang bila tersedia).
	 *
	 * @param persenPph persentase PPh, boleh {@code null}.
	 */
	public void setPersenPph(Double persenPph) {
		this.persenPph = persenPph;
	}

	/**
	 * Menghitung DAN mengembalikan total akhir baris ini (nilai bersih setelah diskon, PPN, dan
	 * — bergantung konfigurasi — pengurangan PPh). Berbeda dari {@link #hitungPpn()}/
	 * {@link #hitungPph()} yang murni tampilan, method ini adalah nilai yang secara bisnis
	 * dianggap "nilai akhir" baris (dipakai antara lain sebagai komponen total dokumen LPJ).
	 *
	 * <p><b>Rumus:</b> identik dengan {@link #hitungPpn()}/{@link #hitungPph()} untuk komponen
	 * DPP dan potongan ({@code DPP = jumlah × harga}, dikurangi potongan persen/nominal sesuai
	 * {@link #getDiskonDalamBentukPersen()}), lalu {@code PPN = (persenPpn/100) × DPP} dan
	 * {@code PPh = (persenPph/100) × DPP} (BERSYARAT, lihat di bawah), dan akhirnya
	 * {@code total = (DPP + PPN) − PPh}, dibulatkan ke bilangan bulat terdekat
	 * ({@code Math.round}).</p>
	 *
	 * <p><b>Syarat pengurangan PPh — konfigurasi {@code pph_mengurangi_lpj}:</b> berbeda dari
	 * {@link #hitungPph()} yang SELALU mengembalikan nilai PPh nominal, method ini membaca
	 * konfigurasi global {@code pph_mengurangi_lpj} lewat {@link Common#bolehKonfigurasi(String)}
	 * untuk memutuskan apakah PPh benar-benar dikurangkan dari total (bila konfigurasi nonaktif,
	 * {@code pph} dipaksa {@code 0.0} sehingga total = DPP + PPN saja, PPh tetap ada di kolom
	 * tampilan lain tapi tidak memotong nilai LPJ). Pembacaan konfigurasi dibungkus
	 * {@code try/catch (Throwable ...)} (bukan sekadar {@code Exception}) dengan alasan spesifik:
	 * getter ini ikut dibaca Hibernate saat flush/dirty-check, termasuk dari thread latar belakang
	 * saat aplikasi sedang dimatikan — pada kondisi itu cache konfigurasi (MapDB) bisa sudah
	 * tertutup dan melempar {@code Error} "already closed" (bukan {@code Exception} biasa,
	 * sehingga HARUS ditangkap sebagai {@code Throwable} agar tertangkap juga). Bila terjadi,
	 * fallback aman adalah menganggap konfigurasi AKTIF (PPh tetap mengurangi total) — nilai yang
	 * sama dengan kondisi normal, sehingga flush tidak gagal maupun menghasilkan nilai yang salah
	 * secara diam-diam.</p>
	 *
	 * <p>Field {@link #jumlah}/{@link #harga}/{@link #hargaPotongan}/{@link #persenPpn}/
	 * {@link #persenPph} dibaca langsung sebagai field (bukan lewat getter publik) dengan alasan
	 * yang sama seperti {@link #hitungPpn()} — menghindari inisialisasi proxy Hibernate detached
	 * saat dipanggil dari dirty-check. Hasil akhir DITULISKAN ke field {@link #hargaTotal}
	 * (berbeda dari {@link #hitungPpn()}/{@link #hitungPph()} yang tidak menulis field apa pun).</p>
	 *
	 * @return total akhir baris (dibulatkan ke bilangan bulat terdekat); tidak pernah {@code null}.
	 */
	public Double getHargaTotal() {
		/*
		 * Getter ini ikut dibaca Hibernate saat flush/dirty-check (termasuk dari thread latar).
		 * Saat aplikasi sedang berhenti, cache MapDB bisa sudah ditutup sehingga getKonfigurasi
		 * melempar Error "already closed" — itu BUKAN Exception biasa, jadi ditangkap Throwable
		 * dengan nilai default AKTIF agar flush tidak gagal. Saat normal, nilainya sama.
		 */
		boolean pph_mengurangi_lpj = true;
		try {
			pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
		} catch (Throwable t) {
			pph_mengurangi_lpj = true;
		}
		double jumlahAman = jumlah == null ? 0.0 : jumlah.doubleValue();
		double hargaAman = harga == null ? 0.0 : harga.doubleValue();
		double potonganAman = hargaPotongan == null ? 0.0 : hargaPotongan.doubleValue();
		double dpp = jumlahAman * hargaAman;
		double potongan = getDiskonDalamBentukPersen()
				? (potonganAman / 100.0) * dpp : potonganAman;
		dpp -= potongan;

		double ppn = ((persenPpn == null ? 0.0 : persenPpn.doubleValue()) / 100.0) * dpp;
		double pph = !pph_mengurangi_lpj ? 0.0
				: ((persenPph == null ? 0.0 : persenPph.doubleValue()) / 100.0) * dpp;

		hargaTotal = (dpp + ppn) - (pph);

		return (double) Math.round(hargaTotal);
	}

	/**
	 * Mengisi total baris secara langsung. Nilai ini ditimpa setiap kali {@link #getHargaTotal()}
	 * dipanggil, sehingga setter ini efektif hanya sebagai nilai awal/sementara sebelum getter
	 * pertama kali dipanggil.
	 *
	 * @param hargaTotal total baris, boleh {@code null}.
	 */
	public void setHargaTotal(Double hargaTotal) {
		this.hargaTotal = hargaTotal;
	}

	/**
	 * Menentukan bentuk potongan/diskon baris ini. Bernilai {@code true} bila belum pernah diset
	 * (default aman: diskon dianggap persen).
	 *
	 * @return {@code true} bila {@link #getHargaPotongan()} adalah persentase (0-100),
	 *         {@code false} bila nominal tetap.
	 */
	public Boolean getDiskonDalamBentukPersen() {
		return diskonDalamBentukPersen == null ? true : diskonDalamBentukPersen;
	}

	/**
	 * Mengisi bentuk potongan/diskon.
	 *
	 * @param diskonDalamBentukPersen {@code true}=persen, {@code false}=nominal; {@code null}
	 *                                diperlakukan sebagai {@code true} oleh
	 *                                {@link #getDiskonDalamBentukPersen()}.
	 */
	public void setDiskonDalamBentukPersen(Boolean diskonDalamBentukPersen) {
		this.diskonDalamBentukPersen = diskonDalamBentukPersen;
	}

	/**
	 * Mengembalikan satuan kerja pemilik baris ini, dengan penelusuran fallback berlapis. Field
	 * {@link #satuanKerja} diresolusi lebih dulu lewat {@link GeneralValueObject#check(Object)};
	 * kemudian, bila {@link #penerimaanPengadaanMasterAssetDetail} terisi, ditelusuri rantai
	 * navigasi PANJANG (BAST → pesanan → permintaan → dokumen permintaan induk →
	 * {@code getSatuanKerja()}) — bila SELURUH tautan pada rantai itu tidak {@code null}, hasilnya
	 * MENIMPA {@link #satuanKerja} tanpa syarat. Bila {@link #penerimaanPengadaanMasterAssetDetail}
	 * kosong, jalur alternatif dicoba: {@link #getWorkspace()}{@code .getSatuanKerja()}.
	 *
	 * <p>Catatan: rantai navigasi via BAST sengaja ditulis sebagai satu ekspresi ternary bersarang
	 * (bukan serangkaian {@code if} berurutan) untuk memastikan SELURUH tautan diperiksa
	 * non-{@code null} sebelum satu pun getter di ujung rantai dipanggil — mencegah
	 * {@code NullPointerException} di tengah rantai bila salah satu dokumen antara belum
	 * ditautkan.</p>
	 *
	 * @return {@link SatuanKerja} hasil penelusuran, atau {@code null} bila tidak ditemukan di
	 *         satu pun jalur.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		if (penerimaanPengadaanMasterAssetDetail != null) {
			SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = this;
			SatuanKerja satuanKerjadata = (SatuanKerja) (saldoAwalMasterAssetDetail
					.getPenerimaanPengadaanMasterAssetDetail() != null
					&& saldoAwalMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
							.getPemesananPengadaanMasterAssetDetail() != null
					&& saldoAwalMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
							.getPemesananPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAssetDetail() != null

					&& saldoAwalMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
							.getPemesananPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset() != null

					&& saldoAwalMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
							.getPemesananPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAssetDetail()
							.getPermintaanPengadaanMasterAsset().getSatuanKerja() != null
									? saldoAwalMasterAssetDetail.getPenerimaanPengadaanMasterAssetDetail()
											.getPemesananPengadaanMasterAssetDetail()
											.getPermintaanPengadaanMasterAssetDetail()
											.getPermintaanPengadaanMasterAsset().getSatuanKerja()
									: null);
			if (satuanKerjadata != null) {
				satuanKerja = satuanKerjadata;
			}
		} else if (getWorkspace() != null && workspace.getSatuanKerja() != null) {
			satuanKerja = workspace.getSatuanKerja();
		}
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja secara langsung. Nilai ini bisa ditimpa tanpa syarat oleh fallback
	 * rantai BAST pada {@link #getSatuanKerja()} bila rantai tersebut lengkap.
	 *
	 * @param satuanKerja satuan kerja, boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan kode unik baris ini, membuatnya otomatis via
	 * {@link Common#getGeneratedBarCode()} bila masih kosong. Blok kode besar yang dikomentari di
	 * bawah baris aktif adalah SISA skema penomoran deterministik lama (kode unik disusun dari
	 * gabungan id {@link MasterAsset}/BAST/permintaan/{@link SaldoAwalMasterAsset}, bercabang
	 * menurut kombinasi relasi yang tersedia) yang sudah DITINGGALKAN — implementasi aktif saat
	 * ini jauh lebih sederhana dan tidak deterministik (barcode acak), tidak lagi mencoba
	 * merefleksikan struktur relasi baris ke dalam kodenya. Kode lama ini disengaja dibiarkan
	 * sebagai referensi historis, bukan kode mati yang perlu dihapus segera.
	 *
	 * @return kode unik baris; tidak pernah {@code null} atau kosong setelah getter ini dipanggil
	 *         sekali (barcode dibuat otomatis).
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (kodeUnik == null || kodeUnik.trim().isEmpty()) {
			kodeUnik = Common.getGeneratedBarCode();
		}
//		if (getMasterAsset() != null && getPenerimaanPengadaanMasterAssetDetail() != null
//				&& getPenerimaanPengadaanMasterAssetDetail().getPemesananPengadaanMasterAssetDetail() != null
//				&& getPenerimaanPengadaanMasterAssetDetail().getPemesananPengadaanMasterAssetDetail()
//						.getPermintaanPengadaanMasterAssetDetail() != null) {
//			kodeUnik = getMasterAsset().getId() + "_" + getPenerimaanPengadaanMasterAssetDetail().getId() + "_"
//					+ getPenerimaanPengadaanMasterAssetDetail().getPemesananPengadaanMasterAssetDetail()
//							.getPermintaanPengadaanMasterAssetDetail().getId()
//					+ "_" + getSaldoAwal().getId();
//		} else if (getMasterAsset() != null && getPenerimaanPengadaanMasterAssetDetail() != null) {
//			kodeUnik = getMasterAsset().getId() + "_" + getPenerimaanPengadaanMasterAssetDetail().getId() + "_"
//					+ getSaldoAwal().getId();
//		} else if (getSaldoAwal() != null && getSaldoAwal().getJsonTermin() != null) {
//			kodeUnik = null;
//		} else if (getMasterAsset() != null && getSaldoAwal() != null) {
//			kodeUnik = getMasterAsset().getId() + "_" + getSaldoAwal().getId();
//		} else {
//			kodeUnik = null;
//		}
		return kodeUnik;
	}

	/**
	 * Mengisi kode unik secara langsung. Nilai ini hanya berpengaruh sebagai nilai awal — bila
	 * kosong/blank, {@link #getKodeUnik()} akan membuat barcode baru secara otomatis.
	 *
	 * @param kodeUnik kode unik, boleh {@code null}/kosong.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan workspace terkait, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link Workspace} terkait (salah satu fallback penentu {@link #getSatuanKerja()}),
	 *         atau {@code null} bila belum ditetapkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace", nullable = true)
	public Workspace getWorkspace() {
		workspace = check(workspace);
		return workspace;
	}

	/**
	 * Mengisi workspace terkait.
	 *
	 * @param workspace workspace terkait, boleh {@code null}.
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}
}
