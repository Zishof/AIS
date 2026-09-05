package ais.database.model.asset;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.library.KodeTransaksi;
import ais.database.model.rab.SatuanKerja;

/**
 * <h2>DetailTransaksiAsset — buku besar (ledger) log transaksi generik lintas-modul aset.</h2>
 *
 * <p>
 * Berbeda dari namanya yang terdengar generik, entity ini <b>bukan</b> log yang mencatat semua
 * jenis pergerakan aset (pembelian/pemakaian/peminjaman/retur/penghapusan) dalam satu tabel
 * tunggal. Cakupannya spesifik pada TIGA sumber transaksi yang benar-benar dirujuk lewat foreign
 * key opsional pada kelas ini: {@link #getSaldoAwalMasterAssetDetail() saldoAwalMasterAssetDetail}
 * (baris tagihan/penerimaan pengadaan lewat {@link SaldoAwalMasterAssetDetail}),
 * {@link #getPenerimaanPengadaanMasterAssetDetail() penerimaanPengadaanMasterAssetDetail} (baris
 * penerimaan barang/BAST), {@link #getPemakaianMasterAssetDetail() pemakaianMasterAssetDetail}
 * (baris pemakaian aset), dan {@link #getStokAwal() stokAwal} (baris stok awal migrasi/onboarding,
 * lihat {@link StokAwal}). Jenis pergerakan lain di paket ini — mis. peminjaman, retur, dan
 * penghapusan aset — dikelola oleh entity/tabelnya masing-masing dan TIDAK menuliskan baris ke
 * {@code detail_transaksi_asset} ini. Setiap baris di sini biasanya hanya mengisi <b>salah satu</b>
 * dari keempat referensi tersebut (tidak semuanya sekaligus), menandai dari mana baris ini berasal.
 * </p>
 *
 * <h3>Peran &amp; pemakaian</h3>
 * <p>
 * Karena mengumpulkan beberapa jenis sumber ke satu bentuk baris seragam (aset, lokasi, ruang,
 * qty, qtyBonus, amount, tanggal), kelas ini memudahkan laporan/riwayat yang perlu menampilkan
 * pergerakan lintas beberapa jenis transaksi tanpa harus melakukan UNION manual atas beberapa
 * tabel sumber. {@link #getTanggalDanWaktu()} secara khusus menormalkan tanggal efektif dengan
 * menelusuri balik ke dokumen sumber yang tersedia (BAST/pemakaian) agar konsisten dengan waktu
 * kejadian sebenarnya, bukan sekadar tanggal baris ini disimpan.
 * </p>
 *
 * <h3>Pemetaan basis data &amp; audit</h3>
 * <p>
 * Dipetakan ke tabel <code>asset.detail_transaksi_asset</code>. Field jejak {@code oleh}/
 * {@code olehId}/{@code tanggal_dirubah} diisi otomatis lewat hook
 * {@link javax.persistence.PreUpdate} {@link #onUpdate()}
 * ({@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}), dan setiap perubahan
 * direkam ke tabel revisi Envers karena kelas ditandai {@link org.hibernate.envers.Audited @Audited}.
 * </p>
 *
 * @author AIS
 * @see MasterAsset
 * @see SaldoAwalMasterAssetDetail
 * @see StokAwal
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "detail_transaksi_asset")
public class DetailTransaksiAsset extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.detail_transaksi_asset}. */
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
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
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
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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

	/** Aset yang terlibat pada baris transaksi ini; opsional (boleh {@code null}). */
	private MasterAsset masterAsset;
	/** Kode transaksi/pengelompokan referensi eksternal; opsional. */
	private KodeTransaksi kodeTransaksi;
	/** Pemilik aset pada saat transaksi ini terjadi; opsional. */
	private PemilikAsset pemilikAsset;
	/** Lokasi aset pada saat transaksi ini terjadi; opsional. */
	private Lokasi lokasi;
	/** Ruang aset pada saat transaksi ini terjadi; opsional. */
	private Ruang ruang;
	/** Kuantitas transaksi; default {@code 0.0}. */
	private Double qty = 0.0;
	/** Kuantitas bonus (di luar qty utama, mis. hadiah dari vendor); default {@code 0.0}. */
	private Double qtyBonus = 0.0;
	/** Nilai/nominal transaksi; default {@code 0.0}. */
	private Double amount = 0.0;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Tanggal transaksi (nilai mentah tersimpan); default waktu saat objek dibuat. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();

	/** Tanggal-dan-waktu efektif hasil normalisasi lintas sumber, lihat {@link #getTanggalDanWaktu()}. */
	private Date tanggalDanWaktu = ais.ui.util.WaktuUtil.getDate();

	/* referensi */
	/** Referensi ke baris tagihan/penerimaan pengadaan (salah satu dari empat kemungkinan sumber). */
	private SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail;
	/** Referensi ke baris penerimaan barang/BAST (salah satu dari empat kemungkinan sumber). */
	private PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail;
	/** Referensi ke baris pemakaian aset (salah satu dari empat kemungkinan sumber). */
	private PemakaianMasterAssetDetail pemakaianMasterAssetDetail;
	/** Referensi ke baris stok awal migrasi/onboarding (salah satu dari empat kemungkinan sumber). */
	private StokAwal stokAwal;

	private SatuanKerja satuanKerja;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public DetailTransaksiAsset() {

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
	 * Mengembalikan aset yang terlibat pada baris transaksi ini, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link MasterAsset} terkait, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "master_asset", nullable = true)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		return masterAsset;
	}

	/**
	 * Mengisi aset terkait.
	 *
	 * @param masterAsset aset terkait, boleh {@code null}.
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/** @return keterangan bebas baris ini, boleh {@code null}. */
	@Column(name = "keterangan", columnDefinition = "text", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return pemilik aset pada saat transaksi ini terjadi, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemilik_asset", nullable = true)
	public PemilikAsset getPemilikAsset() {
		return pemilikAsset;
	}

	/**
	 * Mengisi pemilik aset.
	 *
	 * @param pemilikAsset pemilik aset, boleh {@code null}.
	 */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/** @return lokasi aset pada saat transaksi ini terjadi, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		return lokasi;
	}

	/**
	 * Mengisi lokasi aset.
	 *
	 * @param lokasi lokasi terkait, boleh {@code null}.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan ruang aset pada saat transaksi ini terjadi, meresolusi proxy lazy Hibernate
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link Ruang} terkait, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/**
	 * Mengisi ruang aset.
	 *
	 * @param ruang ruang terkait, boleh {@code null}.
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/** @return tanggal transaksi (nilai mentah tersimpan), boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal", nullable = true)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Mengisi tanggal transaksi.
	 *
	 * @param tanggal tanggal transaksi, boleh {@code null}.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return kuantitas transaksi; default {@code 0.0}. */
	public Double getQty() {
		return qty;
	}

	/**
	 * Mengisi kuantitas transaksi.
	 *
	 * @param qty kuantitas transaksi.
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/** @return kuantitas bonus (di luar qty utama); default {@code 0.0}. */
	public Double getQtyBonus() {
		return qtyBonus;
	}

	/**
	 * Mengisi kuantitas bonus.
	 *
	 * @param qtyBonus kuantitas bonus.
	 */
	public void setQtyBonus(Double qtyBonus) {
		this.qtyBonus = qtyBonus;
	}

	/** @return kode transaksi/pengelompokan referensi eksternal, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_transaksi", nullable = true)
	public KodeTransaksi getKodeTransaksi() {
		return kodeTransaksi;
	}

	/**
	 * Mengisi kode transaksi.
	 *
	 * @param kodeTransaksi kode transaksi terkait, boleh {@code null}.
	 */
	public void setKodeTransaksi(KodeTransaksi kodeTransaksi) {
		this.kodeTransaksi = kodeTransaksi;
	}

	/** @return nilai/nominal transaksi; default {@code 0.0}. */
	public Double getAmount() {
		return amount;
	}

	/**
	 * Mengisi nilai/nominal transaksi.
	 *
	 * @param amount nilai transaksi.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * @return referensi ke baris tagihan/penerimaan pengadaan ({@link SaldoAwalMasterAssetDetail}),
	 *         salah satu dari empat kemungkinan sumber baris ini; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_master_asset_detail", nullable = true)
	public SaldoAwalMasterAssetDetail getSaldoAwalMasterAssetDetail() {
		return saldoAwalMasterAssetDetail;
	}

	/**
	 * Mengisi referensi ke baris tagihan/penerimaan pengadaan.
	 *
	 * @param saldoAwalMasterAssetDetail baris sumber terkait, boleh {@code null}.
	 */
	public void setSaldoAwalMasterAssetDetail(SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail) {
		this.saldoAwalMasterAssetDetail = saldoAwalMasterAssetDetail;
	}

	/**
	 * @return referensi ke baris pemakaian aset ({@code PemakaianMasterAssetDetail}), salah satu
	 *         dari empat kemungkinan sumber baris ini; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemakaian_master_asset_detail", nullable = true)
	public PemakaianMasterAssetDetail getPemakaianMasterAssetDetail() {
		return pemakaianMasterAssetDetail;
	}

	/**
	 * Mengisi referensi ke baris pemakaian aset.
	 *
	 * @param pemakaianMasterAssetDetail baris sumber terkait, boleh {@code null}.
	 */
	public void setPemakaianMasterAssetDetail(PemakaianMasterAssetDetail pemakaianMasterAssetDetail) {
		this.pemakaianMasterAssetDetail = pemakaianMasterAssetDetail;
	}

	/**
	 * @return referensi ke baris penerimaan barang/BAST ({@code PenerimaanPengadaanMasterAssetDetail}),
	 *         salah satu dari empat kemungkinan sumber baris ini; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset_detail", nullable = true)
	public PenerimaanPengadaanMasterAssetDetail getPenerimaanPengadaanMasterAssetDetail() {
		return penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Mengisi referensi ke baris penerimaan barang/BAST.
	 *
	 * @param penerimaanPengadaanMasterAssetDetail baris sumber terkait, boleh {@code null}.
	 */
	public void setPenerimaanPengadaanMasterAssetDetail(
			PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail) {
		this.penerimaanPengadaanMasterAssetDetail = penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Menentukan tanggal-dan-waktu efektif baris transaksi ini dengan menelusuri balik ke dokumen
	 * sumber yang tersedia (dalam urutan prioritas): {@link #getSaldoAwalMasterAssetDetail()} →
	 * tanggal pembuatan {@link SaldoAwalMasterAsset} induknya; jika kosong, lalu
	 * {@link #getPemakaianMasterAssetDetail()} → tanggal pembuatan pemakaian induknya; jika masih
	 * kosong, lalu {@link #getPenerimaanPengadaanMasterAssetDetail()} → tanggal pembuatan
	 * penerimaan pengadaan induknya. Penelusuran dibungkus {@code try/catch} generik (dicatat ke
	 * {@link ais.common.ErrorAuditUtil}) karena relasi-relasi ini bisa berupa proxy Hibernate yang
	 * sudah lepas dari sesi (LazyInitializationException) — kegagalan navigasi tidak boleh
	 * menjatuhkan pemanggil, cukup lanjut ke fallback berikutnya.
	 *
	 * <p>Bila ketiga sumber di atas tidak menghasilkan nilai (semua kosong atau gagal dinavigasi,
	 * atau baris ini berasal dari {@link #getStokAwal()} yang TIDAK ikut dicoba di sini),
	 * dikembalikan {@link #getTanggal()} sebagai fallback terakhir. Tujuannya adalah agar laporan
	 * gabungan lintas jenis transaksi menampilkan tanggal kejadian yang paling bermakna secara
	 * bisnis (mis. tanggal BAST ditandatangani), bukan sekadar tanggal baris log ini disimpan ke
	 * database — dua hal yang bisa berbeda cukup jauh bila pencatatan dilakukan belakangan.</p>
	 *
	 * @return tanggal-dan-waktu efektif; tidak pernah {@code null} selama {@link #getTanggal()}
	 *         (fallback terakhir) juga tidak {@code null}.
	 */
	public Date getTanggalDanWaktu() {

		try {
			if (saldoAwalMasterAssetDetail != null) {
				tanggalDanWaktu = saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPembuatan();
			} else if (pemakaianMasterAssetDetail != null) {
				tanggalDanWaktu = pemakaianMasterAssetDetail.getPemakaianMasterAsset().getTanggalPembuatan();
			} else if (penerimaanPengadaanMasterAssetDetail != null) {
				tanggalDanWaktu = penerimaanPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
						.getTanggalPembuatan();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/asset/DetailTransaksiAsset.java:261");
			// TODO: handle exception
		}

		if (tanggalDanWaktu == null) {
			tanggalDanWaktu = getTanggal();
		}
		return tanggalDanWaktu;
	}

	/**
	 * Mengisi tanggal-dan-waktu efektif secara manual. Nilai ini akan ditimpa ulang oleh
	 * {@link #getTanggalDanWaktu()} setiap kali dipanggil bila salah satu dari ketiga referensi
	 * sumber (saldoAwal/pemakaian/penerimaan) berhasil dinavigasi, sehingga setter ini efektif
	 * hanya berpengaruh untuk baris yang sumbernya kosong/gagal dinavigasi.
	 *
	 * @param tanggalDanWaktu tanggal-dan-waktu, boleh {@code null}.
	 */
	public void setTanggalDanWaktu(Date tanggalDanWaktu) {
		this.tanggalDanWaktu = tanggalDanWaktu;
	}

	/**
	 * @return referensi ke baris stok awal migrasi/onboarding ({@link StokAwal}), salah satu dari
	 *         empat kemungkinan sumber baris ini; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "stok_awal", nullable = true)
	public StokAwal getStokAwal() {
		return stokAwal;
	}

	/**
	 * Mengisi referensi ke baris stok awal.
	 *
	 * @param stokAwal baris sumber terkait, boleh {@code null}.
	 */
	public void setStokAwal(StokAwal stokAwal) {
		this.stokAwal = stokAwal;
	}

	/**
	 * Menentukan satuan kerja pemilik baris transaksi ini dengan menelusuri balik ke sumbernya,
	 * dalam urutan prioritas: {@link #getStokAwal()} → {@link #getPemakaianMasterAssetDetail()} →
	 * {@link #getPenerimaanPengadaanMasterAssetDetail()} → {@link #getSaldoAwalMasterAssetDetail()}.
	 * Bila keempatnya tidak menghasilkan nilai, jatuh ke field {@code satuanKerja} milik baris ini
	 * sendiri (diresolusi lewat {@link GeneralValueObject#check(Object)}) — yang biasanya kosong
	 * kecuali diisi manual. Pola ini menjaga agar filter/laporan per-satuan-kerja tetap konsisten
	 * walau baris {@code DetailTransaksiAsset} sendiri tidak menyimpan satuan kerja secara
	 * langsung untuk sebagian besar kasus.
	 *
	 * @return {@link SatuanKerja} hasil penelusuran, atau {@code null} bila tidak ditemukan di
	 *         mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getStokAwal() != null && getStokAwal().getSatuanKerja() != null) {
			satuanKerja = getStokAwal().getSatuanKerja();
		} else if (getPemakaianMasterAssetDetail() != null
				&& getPemakaianMasterAssetDetail().getPemakaianMasterAsset() != null
				&& getPemakaianMasterAssetDetail().getPemakaianMasterAsset().getSatuanKerja() != null) {
			satuanKerja = getPemakaianMasterAssetDetail().getPemakaianMasterAsset().getSatuanKerja();
		} else if (getPenerimaanPengadaanMasterAssetDetail() != null
				&& getPenerimaanPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset() != null
				&& getPenerimaanPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset()
						.getSatuanKerja() != null) {
			satuanKerja = getPenerimaanPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset()
					.getSatuanKerja();
		} else if (getSaldoAwalMasterAssetDetail() != null && getSaldoAwalMasterAssetDetail().getSaldoAwal() != null
				&& getSaldoAwalMasterAssetDetail().getSaldoAwal().getSatuanKerja() != null) {
			satuanKerja = getSaldoAwalMasterAssetDetail().getSaldoAwal().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja baris ini secara langsung. Nilai ini hanya berpengaruh sebagai
	 * fallback terakhir pada {@link #getSatuanKerja()} bila keempat sumber penelusuran lain
	 * tidak menghasilkan nilai.
	 *
	 * @param satuanKerja satuan kerja, boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
