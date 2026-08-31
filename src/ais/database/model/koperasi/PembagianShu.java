package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur pembagian SHU.

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

/**
 * <h2>PembagianShu — Kepala Pembagian Sisa Hasil Usaha (SHU) per Tahun Buku</h2>
 *
 * <p>
 * Entity ini menyimpan <b>keputusan dan total pembagian SHU</b> untuk satu tahun buku koperasi.
 * Berbeda dengan entity simpan pinjam lain yang merupakan konsolidasi dari engine existing, fitur
 * pembagian SHU memang <b>belum ada</b> di sistem (yang tersedia hanya penanda
 * {@code ProdukKoperasi.hitungShu}), sehingga tabel <code>koperasi.pembagian_shu</code> ini dibuat
 * sebagai kebutuhan baru yang sah — bukan duplikasi.
 * </p>
 *
 * <p>
 * Nilai {@link #getTotalShu()} bersumber dari <b>keputusan Rapat Anggota Tahunan (RAT)</b> dan
 * dimasukkan oleh admin/pengurus — bukan dihitung otomatis oleh sistem — karena SHU adalah hasil
 * akhir laporan keuangan yang disahkan RAT. Persentase alokasi ke tiap pos juga ditetapkan RAT dan
 * disimpan di sini. Rincian bagian tiap anggota berada pada entity anak {@link ShuAnggota}, yang
 * dihitung proporsional oleh sistem: <em>jasa modal</em> sebanding simpanan anggota dan <em>jasa
 * usaha</em> sebanding partisipasi (jasa/bunga) anggota.
 * </p>
 *
 * <h3>Pos alokasi sesuai UU Perkoperasian &amp; SOM USPK</h3>
 * <ul>
 * <li>{@link #getPersenCadangan()} — dana cadangan (penguatan modal).</li>
 * <li>{@link #getPersenJasaModal()} — balas jasa modal (sebanding simpanan).</li>
 * <li>{@link #getPersenJasaUsaha()} — balas jasa usaha (sebanding partisipasi).</li>
 * <li>{@link #getPersenPendidikan()} — dana pendidikan &amp; pelatihan.</li>
 * <li>{@link #getPersenPengurus()} — insentif pengurus/pengawas/pengelola.</li>
 * <li>{@link #getPersenSosial()} — dana sosial.</li>
 * <li>{@link #getPersenLain()} — pos lain-lain.</li>
 * </ul>
 * Total seluruh persentase idealnya 100% dan divalidasi di lapisan Action.
 *
 * <h3>Status</h3>
 * <ul>
 * <li>{@link #STATUS_DRAFT} — kebijakan disusun, belum disahkan RAT.</li>
 * <li>{@link #STATUS_DISAHKAN} — disahkan RAT, siap dibagikan.</li>
 * <li>{@link #STATUS_DIBAGIKAN} — bagian anggota telah dibukukan/dibayarkan.</li>
 * </ul>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS: {@code IDENTITY}, relasi lazy dengan {@code check(...)}, hook audit
 * {@code @PreUpdate}, {@code @Audited} (SHU adalah keputusan penting yang harus dapat ditelusuri),
 * getter numerik null-safe, kompatibel Java 1.7. Terdaftar di {@code hibernate.cfg.xml} sehingga
 * {@code hbm2ddl=update} membuat tabel schema {@code koperasi} otomatis. Kombinasi koperasi+tahun
 * sebaiknya unik (dijaga di Action).
 * </p>
 *
 * @see ShuAnggota
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembagian_shu")
public class PembagianShu extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 7742100014412002001L;

	public static final String STATUS_DRAFT = "DRAFT";
	public static final String STATUS_DISAHKAN = "DISAHKAN";
	public static final String STATUS_DIBAGIKAN = "DIBAGIKAN";

	private Long id;
	private String oleh;
	private String olehId;

	private Koperasi koperasi;
	private Integer tahun = 0;
	private Double totalShu = 0.0;
	private Double persenCadangan = 0.0;
	private Double persenJasaModal = 0.0;
	private Double persenJasaUsaha = 0.0;
	private Double persenPendidikan = 0.0;
	private Double persenPengurus = 0.0;
	private Double persenSosial = 0.0;
	private Double persenLain = 0.0;
	private Date tanggalRat;
	private String status = STATUS_DRAFT;
	private String keterangan;

	public PembagianShu() {
	}

	public PembagianShu(Long id) {
		this.id = id;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		return koperasi;
	}

	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi == null || koperasi.getId() == null ? null : koperasi;
	}

	@Column(name = "tahun")
	public Integer getTahun() {
		return tahun == null ? 0 : tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	@Column(name = "total_shu")
	public Double getTotalShu() {
		return totalShu == null ? 0.0 : totalShu;
	}

	public void setTotalShu(Double totalShu) {
		this.totalShu = totalShu;
	}

	@Column(name = "persen_cadangan")
	public Double getPersenCadangan() {
		return persenCadangan == null ? 0.0 : persenCadangan;
	}

	public void setPersenCadangan(Double persenCadangan) {
		this.persenCadangan = persenCadangan;
	}

	@Column(name = "persen_jasa_modal")
	public Double getPersenJasaModal() {
		return persenJasaModal == null ? 0.0 : persenJasaModal;
	}

	public void setPersenJasaModal(Double persenJasaModal) {
		this.persenJasaModal = persenJasaModal;
	}

	@Column(name = "persen_jasa_usaha")
	public Double getPersenJasaUsaha() {
		return persenJasaUsaha == null ? 0.0 : persenJasaUsaha;
	}

	public void setPersenJasaUsaha(Double persenJasaUsaha) {
		this.persenJasaUsaha = persenJasaUsaha;
	}

	@Column(name = "persen_pendidikan")
	public Double getPersenPendidikan() {
		return persenPendidikan == null ? 0.0 : persenPendidikan;
	}

	public void setPersenPendidikan(Double persenPendidikan) {
		this.persenPendidikan = persenPendidikan;
	}

	@Column(name = "persen_pengurus")
	public Double getPersenPengurus() {
		return persenPengurus == null ? 0.0 : persenPengurus;
	}

	public void setPersenPengurus(Double persenPengurus) {
		this.persenPengurus = persenPengurus;
	}

	@Column(name = "persen_sosial")
	public Double getPersenSosial() {
		return persenSosial == null ? 0.0 : persenSosial;
	}

	public void setPersenSosial(Double persenSosial) {
		this.persenSosial = persenSosial;
	}

	@Column(name = "persen_lain")
	public Double getPersenLain() {
		return persenLain == null ? 0.0 : persenLain;
	}

	public void setPersenLain(Double persenLain) {
		this.persenLain = persenLain;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_rat")
	public Date getTanggalRat() {
		return tanggalRat;
	}

	public void setTanggalRat(Date tanggalRat) {
		this.tanggalRat = tanggalRat;
	}

	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Total seluruh persentase alokasi. Idealnya 100. Tidak dipersist. */
	@javax.persistence.Transient
	public double getTotalPersen() {
		return getPersenCadangan() + getPersenJasaModal() + getPersenJasaUsaha() + getPersenPendidikan()
				+ getPersenPengurus() + getPersenSosial() + getPersenLain();
	}

	/** Nominal SHU untuk jasa modal (rupiah). Tidak dipersist. */
	@javax.persistence.Transient
	public double getNominalJasaModal() {
		return getTotalShu() * getPersenJasaModal() / 100.0;
	}

	/** Nominal SHU untuk jasa usaha (rupiah). Tidak dipersist. */
	@javax.persistence.Transient
	public double getNominalJasaUsaha() {
		return getTotalShu() * getPersenJasaUsaha() / 100.0;
	}

	@Override
	public String toString() {
		return "SHU " + getTahun() + " - " + getTotalShu();
	}

	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal pembagian SHU (dok 61 butir B): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} memecah SHU ke pos-pos pembagiannya.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
