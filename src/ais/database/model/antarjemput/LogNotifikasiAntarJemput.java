package ais.database.model.antarjemput;

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

/**
 * Entitas Hibernate untuk log pengiriman notifikasi layanan antar-jemput siswa — dipetakan ke
 * tabel {@code public.log_notifikasi_antar_jemput} (modul {@code antarjemput}). Mencatat satu
 * upaya pengiriman notifikasi (mis. ke perangkat soundbox/WA/aplikasi orang tua) terkait satu
 * kejadian penjemputan/pengantaran ({@link #detailPenjemputanAntarJemput}), termasuk kanal
 * pengiriman, isi pesan, status, jumlah percobaan, dan waktu kirim/diterima — dipakai untuk audit
 * &amp; troubleshooting pengiriman notifikasi (retry, delivery tracking).
 *
 * <h2>Nilai default</h2>
 * <p>
 * {@link #getKanal()} default {@code "SOUNDBOX"} (kanal notifikasi paling umum di layanan
 * antar-jemput AIS — perangkat pengeras suara di titik jemput); {@link #getStatus()} default
 * {@code "ANTRI"}; {@link #getPercobaan()} default {@code 0}; {@link #getNama()} fallback ke
 * {@link #getKanal()} bila belum diisi.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "log_notifikasi_antar_jemput")
public class LogNotifikasiAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439818L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	/** Nama log; fallback ke {@link #getKanal()} bila belum diisi — lihat {@link #getNama()}. */
	private String nama;
	private String keterangan;
	/** Kanal pengiriman notifikasi (mis. SOUNDBOX, WA, dsb); default {@code "SOUNDBOX"} — lihat {@link #getKanal()}. */
	private String kanal;
	/** Identitas/alamat perangkat tujuan notifikasi (mis. id perangkat soundbox atau nomor tujuan). */
	private String perangkatTujuan;
	/** Isi pesan notifikasi yang dikirim. */
	private String pesan;
	/** Status pengiriman (mis. ANTRI/TERKIRIM/GAGAL); default {@code "ANTRI"} — lihat {@link #getStatus()}. */
	private String status;
	/** Jumlah percobaan pengiriman yang sudah dilakukan; default {@code 0}. */
	private Integer percobaan;
	/** Waktu notifikasi dikirim dari sistem. */
	private Date waktuKirim;
	/** Waktu notifikasi tercatat diterima/di-ack oleh perangkat tujuan. */
	private Date waktuDiterima;

	/** Kejadian penjemputan/pengantaran yang menjadi konteks/pemicu notifikasi ini. */
	private DetailPenjemputanAntarJemput detailPenjemputanAntarJemput;

	public LogNotifikasiAntarJemput() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
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

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", length = 255)
	public String getNama() {
		return nama == null ? getKanal() : nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "kanal", length = 40)
	public String getKanal() {
		return kanal == null ? "SOUNDBOX" : kanal;
	}

	public void setKanal(String kanal) {
		this.kanal = kanal;
	}

	@Column(name = "perangkat_tujuan", length = 255)
	public String getPerangkatTujuan() {
		return perangkatTujuan;
	}

	public void setPerangkatTujuan(String perangkatTujuan) {
		this.perangkatTujuan = perangkatTujuan;
	}

	@Column(name = "pesan")
	public String getPesan() {
		return pesan;
	}

	public void setPesan(String pesan) {
		this.pesan = pesan;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null ? "ANTRI" : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getPercobaan() {
		return percobaan == null ? 0 : percobaan;
	}

	public void setPercobaan(Integer percobaan) {
		this.percobaan = percobaan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuKirim() {
		return waktuKirim;
	}

	public void setWaktuKirim(Date waktuKirim) {
		this.waktuKirim = waktuKirim;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuDiterima() {
		return waktuDiterima;
	}

	public void setWaktuDiterima(Date waktuDiterima) {
		this.waktuDiterima = waktuDiterima;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_penjemputan_antar_jemput")
	public DetailPenjemputanAntarJemput getDetailPenjemputanAntarJemput() {
		detailPenjemputanAntarJemput = check(detailPenjemputanAntarJemput);
		return detailPenjemputanAntarJemput;
	}

	public void setDetailPenjemputanAntarJemput(DetailPenjemputanAntarJemput detailPenjemputanAntarJemput) {
		this.detailPenjemputanAntarJemput = detailPenjemputanAntarJemput;
	}
}
