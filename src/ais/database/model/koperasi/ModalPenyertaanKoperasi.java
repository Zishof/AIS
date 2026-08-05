package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur Modal Penyertaan (penguatan modal).

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
 * <h2>ModalPenyertaanKoperasi — Modal Penyertaan (Penguatan Modal Koperasi)</h2>
 *
 * <p>
 * Entity ini mencatat <b>modal penyertaan</b>, yaitu dana yang ditanamkan ke koperasi untuk
 * memperkuat permodalan di luar simpanan pokok dan simpanan wajib. Sesuai UU Perkoperasian dan SOM
 * USPK, modal penyertaan dapat berasal dari <b>anggota</b> maupun <b>pihak lain (non-anggota/investor)</b>,
 * disertai perjanjian yang mengatur jumlah, jangka waktu, imbal hasil, serta kesediaan penyerta ikut
 * menanggung risiko usaha. Berbeda dengan simpanan yang merupakan kewajiban koperasi kepada anggota,
 * modal penyertaan tergolong <b>modal/ekuitas</b> yang memperbesar Modal Sendiri dan kemampuan
 * koperasi menyalurkan pinjaman.
 * </p>
 *
 * <h3>Mengapa dibutuhkan?</h3>
 * <p>
 * Fitur ini melengkapi sub-modul simpan pinjam agar seluruh komponen permodalan koperasi tercatat
 * rapi. Dengan mengetahui total modal penyertaan yang masih aktif, pengurus dapat menghitung Modal
 * Sendiri secara lebih tepat (memengaruhi rasio permodalan, batas maksimum pemberian pinjaman/BMPP,
 * dan penilaian kesehatan), sekaligus memantau kewajiban imbal hasil kepada para penyerta.
 * </p>
 *
 * <h3>Isi pokok</h3>
 * <ul>
 * <li>{@link #getJenisPenyerta()} — {@link #JENIS_ANGGOTA} atau {@link #JENIS_NON_ANGGOTA}.</li>
 * <li>{@link #getNamaPenyerta()} — nama penyerta (anggota atau investor luar).</li>
 * <li>{@link #getNomorPerjanjian()} — nomor akta/perjanjian penyertaan.</li>
 * <li>{@link #getNominal()} — besar modal yang disetor.</li>
 * <li>{@link #getTanggalMasuk()} — tanggal dana masuk.</li>
 * <li>{@link #getJangkaWaktuBulan()} — tenor (0 berarti tanpa batas waktu).</li>
 * <li>{@link #getImbalHasilPersen()} — imbal hasil/bagi hasil per tahun (persen).</li>
 * <li>{@link #getStatus()} — {@link #STATUS_AKTIF}, {@link #STATUS_JATUH_TEMPO}, atau
 * {@link #STATUS_DITARIK}.</li>
 * </ul>
 *
 * <p>
 * Metode {@code @Transient} menyediakan turunan yang berguna tanpa disimpan:
 * {@link #getTanggalJatuhTempo()} (tanggal masuk + jangka waktu) dan
 * {@link #getEstimasiImbalHasilTahunan()} (nominal &times; imbal hasil %).
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS: kunci {@code IDENTITY}, relasi {@link Koperasi} lazy dengan {@code check(...)},
 * hook audit {@code @PreUpdate}, {@code @Audited} (modal penyertaan adalah komitmen penting yang harus
 * dapat ditelusuri), getter numerik/boolean aman-null, dan kompatibel Java 1.7. Terdaftar di
 * {@code hibernate.cfg.xml} sehingga {@code hbm2ddl=update} membuat tabel
 * <code>koperasi.modal_penyertaan</code> otomatis. Entity tidak menyentuh basis data langsung dan
 * tidak mengubah perilaku entity lain.
 * </p>
 *
 * @see PembagianShu
 * @see ais.action.master.koperasi.ModalPenyertaanKoperasiAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "modal_penyertaan")
public class ModalPenyertaanKoperasi extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 6620200014412991001L;

	public static final String JENIS_ANGGOTA = "ANGGOTA";
	public static final String JENIS_NON_ANGGOTA = "NON_ANGGOTA";

	public static final String STATUS_AKTIF = "AKTIF";
	public static final String STATUS_JATUH_TEMPO = "JATUH_TEMPO";
	public static final String STATUS_DITARIK = "DITARIK";

	private Long id;
	private String oleh;
	private String olehId;

	private Koperasi koperasi;
	private String jenisPenyerta = JENIS_ANGGOTA;
	private String namaPenyerta;
	private String nomorPerjanjian;
	private Double nominal = 0.0;
	private Date tanggalMasuk;
	private Integer jangkaWaktuBulan = 0;
	private Double imbalHasilPersen = 0.0;
	private String status = STATUS_AKTIF;
	private Date tanggalKembali;
	private String keterangan;
	private Boolean aktif = true;

	public ModalPenyertaanKoperasi() {
	}

	public ModalPenyertaanKoperasi(Long id) {
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

	@Column(name = "jenis_penyerta", length = 20)
	public String getJenisPenyerta() {
		return jenisPenyerta == null || jenisPenyerta.trim().isEmpty() ? JENIS_ANGGOTA : jenisPenyerta;
	}

	public void setJenisPenyerta(String jenisPenyerta) {
		this.jenisPenyerta = jenisPenyerta;
	}

	@Column(name = "nama_penyerta", length = 255)
	public String getNamaPenyerta() {
		return namaPenyerta;
	}

	public void setNamaPenyerta(String namaPenyerta) {
		this.namaPenyerta = namaPenyerta;
	}

	@Column(name = "nomor_perjanjian", length = 100)
	public String getNomorPerjanjian() {
		return nomorPerjanjian;
	}

	public void setNomorPerjanjian(String nomorPerjanjian) {
		this.nomorPerjanjian = nomorPerjanjian;
	}

	@Column(name = "nominal")
	public Double getNominal() {
		return nominal == null ? 0.0 : nominal;
	}

	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_masuk")
	public Date getTanggalMasuk() {
		return tanggalMasuk;
	}

	public void setTanggalMasuk(Date tanggalMasuk) {
		this.tanggalMasuk = tanggalMasuk;
	}

	@Column(name = "jangka_waktu_bulan")
	public Integer getJangkaWaktuBulan() {
		return jangkaWaktuBulan == null ? 0 : jangkaWaktuBulan;
	}

	public void setJangkaWaktuBulan(Integer jangkaWaktuBulan) {
		this.jangkaWaktuBulan = jangkaWaktuBulan;
	}

	@Column(name = "imbal_hasil_persen")
	public Double getImbalHasilPersen() {
		return imbalHasilPersen == null ? 0.0 : imbalHasilPersen;
	}

	public void setImbalHasilPersen(Double imbalHasilPersen) {
		this.imbalHasilPersen = imbalHasilPersen;
	}

	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_AKTIF : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_kembali")
	public Date getTanggalKembali() {
		return tanggalKembali;
	}

	public void setTanggalKembali(Date tanggalKembali) {
		this.tanggalKembali = tanggalKembali;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** Perkiraan tanggal jatuh tempo = tanggal masuk + jangka waktu (null bila tanpa tenor). Tidak dipersist. */
	@javax.persistence.Transient
	public Date getTanggalJatuhTempo() {
		if (tanggalMasuk == null || getJangkaWaktuBulan() <= 0) {
			return null;
		}
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(tanggalMasuk);
		c.add(java.util.Calendar.MONTH, getJangkaWaktuBulan());
		return c.getTime();
	}

	/** Perkiraan imbal hasil setahun (rupiah) = nominal × imbal hasil %. Tidak dipersist. */
	@javax.persistence.Transient
	public double getEstimasiImbalHasilTahunan() {
		return getNominal() * getImbalHasilPersen() / 100.0;
	}

	@Override
	public String toString() {
		return "Modal Penyertaan " + (namaPenyerta == null ? "" : namaPenyerta) + " - " + getNominal();
	}
}
