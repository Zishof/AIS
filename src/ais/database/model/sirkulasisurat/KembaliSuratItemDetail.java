package ais.database.model.sirkulasisurat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;
import ais.database.model.surat.SuratMasuk;

/**
 * Entitas Hibernate (skema {@code surat}, tabel {@code kembali_surat_item_detail}) yang menjadi
 * DETAIL satu dokumen surat masuk ({@link SuratMasuk}) yang dikembalikan dalam satu transaksi
 * {@link KembaliSuratItem} (header) — pasangan dari {@link PeminjamanSuratItemDetail}. Mencatat
 * kondisi pengembalian: kelengkapan dokumen ({@link #getKelengkapan()}), status baik/rusak
 * ({@link #getStatus()}), dan denda keterlambatan/kerusakan ({@link #getDenda()}) beserta status
 * pelunasannya ({@link #getTelahDibayar()}/{@link #getDibayarSejumlah()}).
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "kembali_surat_item_detail")
public class KembaliSuratItemDetail extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Field audit shadow (bukan kolom Hibernate): nama pemroses terakhir, diisi lewat {@link #setOleh(String)}. */
	private String oleh;
	/** Field audit shadow (bukan kolom Hibernate): ID pemroses terakhir, diisi lewat {@link #setOlehId(String)}. */
	private String olehId;

	/** @return ID pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOlehId(String)}). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed): bila
	 * {@code olehId} null atau hanya berisi spasi, method ini langsung {@code return} tanpa
	 * mengubah field, mempertahankan nilai audit sebelumnya.
	 *
	 * @param olehId ID pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** @return representasi ringkas: dokumen {@link #suratMasuk} terkait detail ini. */
	public String toString() {
		return suratMasuk + "";
	}

	/**
	 * Menyetel nama pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed) dengan
	 * pola yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOleh(String)}). */
	public String getOleh() {
		return oleh;
	}

	/** Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini di-update. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang akan diset. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (diperbarui otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Jumlah eksemplar dokumen yang dikembalikan pada baris detail ini; default {@code 0.0} bila belum diisi. */
	private Double dikembali;
	/** Header transaksi pengembalian ({@link KembaliSuratItem}) pemilik detail ini. */
	private KembaliSuratItem kembaliSuratItem;
	/** Detail peminjaman ({@link PeminjamanSuratItemDetail}) yang diselesaikan/dikembalikan lewat detail ini. */
	private PeminjamanSuratItemDetail peminjamanSuratItemDetail;
	/** Keterangan bebas untuk detail pengembalian ini. */
	private String keterangan;
	/** Tanggal dokumen ini dikembalikan; default tanggal saat ini pada instansiasi, dapat diselaraskan dari tanggal persetujuan header (lihat {@link #getTanggal()}). */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Dokumen surat masuk ({@link SuratMasuk}) yang dikembalikan pada baris detail ini. */
	private SuratMasuk suratMasuk;
	/** Status kelengkapan dokumen saat dikembalikan; {@code null} diperlakukan sebagai lengkap (lihat {@link #getKelengkapan()}). */
	private Boolean kelengkapan;
	/** Status kondisi dokumen saat dikembalikan; {@code null} diperlakukan sebagai baik/normal (lihat {@link #getStatus()}). */
	private Boolean status;
	/** Nominal denda (keterlambatan/kerusakan/ketidaklengkapan) untuk detail ini; default {@code 0.0}. */
	private Double denda = 0.0;
	/** Keterangan/alasan denda yang dikenakan. */
	private String ketDenda;
	/** Nominal yang sudah dibayarkan untuk denda ini; default {@code 0.0}. Lihat catatan bug pada {@link #getDibayarSejumlah()}. */
	private Double dibayarSejumlah = 0.0;
	/** Status pelunasan denda; {@code null} diperlakukan sebagai belum lunas (lihat {@link #getTelahDibayar()}). */
	private Boolean telahDibayar;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public KembaliSuratItemDetail() {
	}

	/** @return ID baris (primary key, auto-increment). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan bebas detail pengembalian ini. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan yang akan diset. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param dikembali jumlah eksemplar dokumen yang dikembalikan, akan diset. */
	public void setDikembali(Double dikembali) {
		this.dikembali = dikembali;
	}

	/** @return jumlah eksemplar dokumen yang dikembalikan; default {@code 0.0} bila belum diisi. */
	public Double getDikembali() {
		if (dikembali == null) {
			dikembali = 0.0;
		}
		return dikembali;
	}

	/** @param kembaliSuratItem header transaksi pengembalian pemilik detail ini, akan diset. */
	public void setKembaliSuratItem(KembaliSuratItem kembaliSuratItem) {
		this.kembaliSuratItem = kembaliSuratItem;
	}

	/** @return header transaksi pengembalian ({@link KembaliSuratItem}) pemilik detail ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kembali_surat_item", nullable = false)
	public KembaliSuratItem getKembaliSuratItem() {
		return kembaliSuratItem;
	}

	/** @return detail peminjaman ({@link PeminjamanSuratItemDetail}) yang diselesaikan lewat detail ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_surat_item_detail", nullable = true)
	public PeminjamanSuratItemDetail getPeminjamanSuratItemDetail() {
		return peminjamanSuratItemDetail;
	}

	/** @param peminjamanSuratItemDetail detail peminjaman yang diselesaikan, akan diset. */
	public void setPeminjamanSuratItemDetail(PeminjamanSuratItemDetail peminjamanSuratItemDetail) {
		this.peminjamanSuratItemDetail = peminjamanSuratItemDetail;
	}

	/** @return nominal denda untuk detail ini; default {@code 0.0} bila belum diisi. */
	public Double getDenda() {
		if (denda == null) {
			denda = 0.0;
		}
		return denda;
	}

	/** @param denda nominal denda yang akan diset. */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	/**
	 * @return tanggal pengembalian dokumen ini; bila belum diisi ({@code null}), diselaraskan dari
	 *         {@code kembaliSuratItem.getTanggalPersetujuan()} bila tersedia, atau tanggal saat ini
	 *         sebagai fallback terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		if (tanggal == null) {
			if (kembaliSuratItem != null && kembaliSuratItem.getTanggalPersetujuan() != null) {
				tanggal = kembaliSuratItem.getTanggalPersetujuan();
			}
		}

		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}

		return tanggal;
	}

	/** @param tanggal tanggal pengembalian yang akan diset. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return dokumen surat masuk yang dikembalikan pada baris detail ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_masuk", nullable = false)
	public SuratMasuk getSuratMasuk() {
		return suratMasuk;
	}

	/** @param suratMasuk dokumen surat masuk yang akan diset. */
	public void setSuratMasuk(SuratMasuk suratMasuk) {
		this.suratMasuk = suratMasuk;
	}

	/** @return status pelunasan denda; {@code null} diperlakukan sebagai belum lunas ({@code false}). */
	public Boolean getTelahDibayar() {
		return telahDibayar == null ? false : telahDibayar;
	}

	/** @param telahDibayar status pelunasan denda yang akan diset. */
	public void setTelahDibayar(Boolean telahDibayar) {
		this.telahDibayar = telahDibayar;
	}

	/**
	 * Getter murni: mengembalikan {@code dibayarSejumlah} apa adanya (null -> 0.0), tidak lagi
	 * bergantung pada {@link #getTelahDibayar()} atau {@link #getDenda()}. Lihat perbaikan bug
	 * serupa pada {@code ais.database.model.library.KembaliPengadaanItemDetail#getDibayarSejumlah()}
	 * untuk latar belakang: versi lama getter ini menulis nol ke kolom untuk pembayaran
	 * sebagian karena entity dipetakan dengan property access (nilai getter yang di-flush ke DB).
	 */
	public Double getDibayarSejumlah() {
		return dibayarSejumlah == null ? 0.0 : dibayarSejumlah;
	}

	/** @param dibayarSejumlah nominal yang sudah dibayarkan, akan diset. */
	public void setDibayarSejumlah(Double dibayarSejumlah) {
		this.dibayarSejumlah = dibayarSejumlah;
	}

	/** @return keterangan/alasan denda; string kosong (bukan null) bila belum diisi. */
	@Column(columnDefinition = "text")
	public String getKetDenda() {
		return ketDenda == null ? "" : ketDenda;
	}

	/** @param ketDenda keterangan denda yang akan diset. */
	public void setKetDenda(String ketDenda) {
		this.ketDenda = ketDenda;
	}

	/** @return status kelengkapan dokumen saat dikembalikan; {@code null} diperlakukan sebagai lengkap ({@code true}). */
	public Boolean getKelengkapan() {
		return kelengkapan == null ? true : kelengkapan;
	}

	/** @param kelengkapan status kelengkapan yang akan diset. */
	public void setKelengkapan(Boolean kelengkapan) {
		this.kelengkapan = kelengkapan;
	}

	/** @return status kondisi dokumen saat dikembalikan; {@code null} diperlakukan sebagai baik/normal ({@code true}). */
	public Boolean getStatus() {
		return status == null ? true : status;
	}

	/** @param status status kondisi dokumen yang akan diset. */
	public void setStatus(Boolean status) {
		this.status = status;
	}

}
