package ais.database.model.faspay;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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



import ais.common.Common;
import ais.database.model.DetailBiaya;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.PengaturanPembayaranBulanan;

/**
 * Entity JPA/Hibernate rincian item tagihan (baris detail) untuk satu {@link FaspayRequest}. Satu
 * {@code FaspayRequest} dapat memiliki banyak baris {@code faspay_request_detail}, masing-masing
 * mewakili satu pos tagihan (mis. satu bulan cicilan atau satu item biaya) yang menjadi bagian
 * dari total transaksi yang diminta ke Faspay. Komponen biaya administrasi/fee terpisah dari pos
 * tagihan ini disimpan pada {@code ais.database.model.faspay.FaspayRequestDetailBiaya}.
 *
 * <p>Lihat javadoc kelas {@link FaspayRequest} untuk penjelasan pola arsitektur umum 4-entity
 * (Request/RequestDetail/RequestDetailBiaya/Response) yang dipakai di semua gateway H2H AIS.</p>
 *
 * @see FaspayRequest
 * @see FaspayRequestDetailBiaya
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "faspay_request_detail")



public class FaspayRequestDetail extends GeneralValueObject {
	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build ({@link java.io.Serializable}).
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	/** Primary key auto-increment (identity) baris detail ini. */
	private Long id;
	/** Nama/label pengguna (audit shadow) yang terakhir membuat/mengubah baris ini. */
	private String oleh;
	/** ID pengguna (audit shadow) yang terakhir membuat/mengubah baris ini, independen dari
	 * relasi entity user. */
	private String olehId;

	/**
	 * Mengambil ID pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan sehingga
	 * nilai audit sebelumnya tetap dipertahankan.
	 *
	 * @param olehId ID pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan sehingga
	 * nilai audit sebelumnya tetap dipertahankan.
	 *
	 * @param oleh nama pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: memperbarui {@link #getTanggal_dirubah()} otomatis lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini di-UPDATE.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp terakhir baris ini diubah. Biasanya diisi otomatis oleh {@link
	 * #onUpdate()}.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk debugging/log, mencantumkan seluruh field utama baris detail
	 * ini.
	 *
	 * @return string ringkas berisi id, faspayRequest, itemBiaya, pengaturanPembayaranBulanan,
	 *         nilai, tanggal, ke, dan keterangan.
	 */
	public String toString() {
		return id + "-" + faspayRequest + "-" + itemBiaya + "-" + pengaturanPembayaranBulanan + "-" + nilai + "-"
				+ tanggal + "-" + ke + "-" + keterangan;
	}

	/** Header transaksi Faspay tempat baris detail ini berada (relasi wajib, {@code nullable =
	 * false}). */
	private FaspayRequest faspayRequest;
	/** Pengaturan pembayaran bulanan yang menjadi acuan nilai/keterangan pos tagihan ini, bila
	 * pos ini berasal dari skema cicilan bulanan. */
	private PengaturanPembayaranBulanan pengaturanPembayaranBulanan;
	/** Item biaya yang menjadi acuan pos tagihan ini, bila bukan berasal dari cicilan bulanan. */
	private ItemBiaya itemBiaya;
	/** Detail biaya (komponen biaya spesifik) acuan nilai pos tagihan ini. */
	private DetailBiaya detailBiaya;
	/** Nominal pos tagihan ini. */
	private Double nilai;
	/** Keterangan pos tagihan; dibangkitkan otomatis oleh {@link #getKeterangan()} bila kosong. */
	private String keterangan;
	/** Tanggal/periode yang direpresentasikan pos tagihan ini (mis. bulan cicilan). */
	private Date tanggal;
	/** Urutan/angsuran ke berapa pos tagihan ini dalam skema cicilan. */
	private Integer ke;
	/** ID baris cicilan pembayaran asal yang menjadi acuan pos tagihan ini. */
	private Long idCicilan;
	/** Nilai denda keterlambatan yang ditambahkan pada pos tagihan ini. */
	private Double denda;
	/** Nilai asli pos tagihan sebelum modifikasi/potongan diterapkan. */
	private Double nilaiAsli;

	/** Referensi ID cicilan pada sistem asal (mis. cross-reference ke skema cicilan gateway
	 * lain atau modul cicilan pembayaran umum). */
	private Long cicilanref;

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public FaspayRequestDetail() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID baris, atau {@code null} bila belum dipersistensi.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key baris ini. Umumnya tidak dipanggil manual karena kolom {@code id}
	 * bersifat {@code insertable = false}.
	 *
	 * @param id nilai ID yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil header transaksi Faspay pemilik baris detail ini.
	 *
	 * @return {@link FaspayRequest} induk baris ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "faspay_request", nullable = false)
	public FaspayRequest getFaspayRequest() {
		return faspayRequest;
	}

	/**
	 * Mengisi header transaksi Faspay pemilik baris detail ini.
	 *
	 * @param faspayRequest header transaksi yang akan ditautkan.
	 */
	public void setFaspayRequest(FaspayRequest faspayRequest) {
		this.faspayRequest = faspayRequest;
	}

	/**
	 * Mengambil pengaturan pembayaran bulanan acuan pos tagihan ini.
	 *
	 * @return {@link PengaturanPembayaranBulanan} terkait, atau {@code null} bila pos tagihan
	 *         bukan berasal dari skema cicilan bulanan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengaturan_pembayaran_bulanan", nullable = true)
	public PengaturanPembayaranBulanan getPengaturanPembayaranBulanan() {
		return pengaturanPembayaranBulanan;
	}

	/**
	 * Mengisi pengaturan pembayaran bulanan acuan pos tagihan ini.
	 *
	 * @param pengaturanPembayaranBulanan pengaturan yang akan ditautkan.
	 */
	public void setPengaturanPembayaranBulanan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		this.pengaturanPembayaranBulanan = pengaturanPembayaranBulanan;
	}

	/**
	 * Mengambil item biaya acuan pos tagihan ini.
	 *
	 * @return {@link ItemBiaya} terkait, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		return itemBiaya;
	}

	/**
	 * Mengisi item biaya acuan pos tagihan ini.
	 *
	 * @param itemBiaya item biaya yang akan ditautkan.
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Mengambil nominal pos tagihan ini.
	 *
	 * @return nominal; {@code 0.0} bila belum pernah diisi (nilai {@code null} di-default-kan
	 *         sekaligus disimpan sebagai efek samping getter).
	 */
	public Double getNilai() {
		if (nilai == null) {
			nilai = 0.0;
		}
		return nilai;
	}

	/**
	 * Mengisi nominal pos tagihan ini.
	 *
	 * @param nilai nominal yang akan diisi.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengambil keterangan pos tagihan ini. Bila kosong, keterangan dibangkitkan otomatis (dan
	 * disimpan sebagai efek samping getter) berdasarkan {@link #pengaturanPembayaranBulanan}
	 * (format: kode-nama item, bulan, nominal, validator = {@link
	 * FaspayRequest#getPayment_channel_name()}) atau, bila tidak ada pengaturan bulanan, dari
	 * {@link #itemBiaya} langsung. Kegagalan lazy-load relasi (mis. proxy Hibernate dari sesi
	 * yang sudah tertutup) ditangkap dan dicatat ke {@code ErrorAuditUtil} agar getter tidak
	 * melempar {@code LazyInitializationException} ke pemanggil.
	 *
	 * @return keterangan pos tagihan; bisa tetap {@code null} bila tidak ada sumber data yang
	 *         memadai untuk dibangkitkan.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		if ((keterangan == null || keterangan.trim().isEmpty())) {
			if (pengaturanPembayaranBulanan != null) {
				try {
					// FIX LazyInitializationException: pengaturanPembayaranBulanan/detailBiaya bisa berupa
					// instance canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke Session
					// lain yang sudah closed -> jangan biarkan getter ini crash, kembalikan fallback aman.
					keterangan = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "-"
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + ", bulan "
							+ pengaturanPembayaranBulanan.getNamaBulan() + " " + ", nominal Rp. "
							+ Common.numberFormat.get().format(pengaturanPembayaranBulanan.ambilNominalModifikasi(
									faspayRequest == null || faspayRequest.getJenisKegiatan() == null ? null
											: faspayRequest.getMahasiswa(),
									faspayRequest == null || faspayRequest.getJenisKegiatan() == null ? null
											: faspayRequest.getSemester()))
							+ ", validator : " + (faspayRequest == null ? "" : faspayRequest.getPayment_channel_name());
				} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/faspay/FaspayRequestDetail.java:getKeterangan-lazy");
				}
			} else if (itemBiaya != null && getNilai() != null) {
				keterangan = itemBiaya.getKode() + "-" + itemBiaya.getNama() + ", nominal Rp. "
						+ Common.numberFormat.get().format(getNilai()) + ", validator : "
						+ (faspayRequest == null ? "" : faspayRequest.getPayment_channel_name());
			}
		}
		return keterangan;
	}

	/**
	 * Mengisi keterangan pos tagihan ini secara eksplisit.
	 *
	 * @param keterangan keterangan yang akan diisi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil tanggal/periode yang direpresentasikan pos tagihan ini.
	 *
	 * @return tanggal, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Mengisi tanggal/periode yang direpresentasikan pos tagihan ini.
	 *
	 * @param tanggal tanggal yang akan diisi.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengambil urutan/angsuran ke berapa pos tagihan ini.
	 *
	 * @return nomor urutan, atau {@code null} bila belum diisi.
	 */
	public Integer getKe() {
		return ke;
	}

	/**
	 * Mengisi urutan/angsuran ke berapa pos tagihan ini.
	 *
	 * @param ke nomor urutan yang akan diisi.
	 */
	public void setKe(Integer ke) {
		this.ke = ke;
	}

	/**
	 * Mengambil ID baris cicilan pembayaran asal acuan pos tagihan ini.
	 *
	 * @return ID cicilan, atau {@code null} bila belum diisi.
	 */
	public Long getIdCicilan() {
		return idCicilan;
	}

	/**
	 * Mengisi ID baris cicilan pembayaran asal acuan pos tagihan ini.
	 *
	 * @param idCicilan ID cicilan yang akan diisi.
	 */
	public void setIdCicilan(Long idCicilan) {
		this.idCicilan = idCicilan;
	}

	/**
	 * Mengambil nilai denda keterlambatan pos tagihan ini.
	 *
	 * @return nilai denda, atau {@code null} bila belum diisi.
	 */
	public Double getDenda() {
		return denda;
	}

	/**
	 * Mengisi nilai denda keterlambatan pos tagihan ini.
	 *
	 * @param denda nilai denda yang akan diisi.
	 */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	/**
	 * Mengambil nilai asli pos tagihan sebelum modifikasi/potongan diterapkan.
	 *
	 * @return nilai asli, atau {@code null} bila belum diisi.
	 */
	public Double getNilaiAsli() {
		return nilaiAsli;
	}

	/**
	 * Mengisi nilai asli pos tagihan sebelum modifikasi/potongan diterapkan.
	 *
	 * @param nilaiAsli nilai asli yang akan diisi.
	 */
	public void setNilaiAsli(Double nilaiAsli) {
		this.nilaiAsli = nilaiAsli;
	}

	/**
	 * Mengambil detail biaya (komponen biaya spesifik) acuan nilai pos tagihan ini.
	 *
	 * @return {@link DetailBiaya} terkait, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_biaya", nullable = true)
	public DetailBiaya getDetailBiaya() {
		return detailBiaya;
	}

	/**
	 * Mengisi detail biaya (komponen biaya spesifik) acuan nilai pos tagihan ini.
	 *
	 * @param detailBiaya detail biaya yang akan ditautkan.
	 */
	public void setDetailBiaya(DetailBiaya detailBiaya) {
		this.detailBiaya = detailBiaya;
	}

	/**
	 * Mengambil referensi ID cicilan pada sistem asal.
	 *
	 * @return ID referensi cicilan, atau {@code null} bila belum diisi.
	 */
	public Long getCicilanref() {
		return cicilanref;
	}

	/**
	 * Mengisi referensi ID cicilan pada sistem asal.
	 *
	 * @param cicilanref ID referensi cicilan yang akan diisi.
	 */
	public void setCicilanref(Long cicilanref) {
		this.cicilanref = cicilanref;
	}

}
