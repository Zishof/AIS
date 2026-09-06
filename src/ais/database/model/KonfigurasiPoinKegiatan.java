package ais.database.model;

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
 * Aturan poin/voucher yang diberikan untuk kehadiran satu {@link FormulirKegiatan} -- fitur "Voucher
 * Pegawai" (bonus poin belanja koperasi dari kehadiran Kajian &amp; Disiplin Kehadiran).
 *
 * <p><b>Relasi 1-ke-1 dengan {@link FormulirKegiatan}.</b> Tiap kategori kegiatan yang diberi poin
 * (mis. "Kajian Tafsir Al-Quran", "Kajian Hadist", "Kajian Al-Hikam", dan satu baris sintetis
 * "Disiplin Kehadiran" yang TIDAK punya sesi {@link Pertemuan} sungguhan) punya SATU baris konfigurasi
 * di sini. Dipisah dari {@code FormulirKegiatan} sendiri (bukan kolom tambahan di situ) supaya entitas
 * kegiatan generik tetap bersih dari konsep "poin/voucher" yang spesifik fitur ini.</p>
 *
 * <p><b>{@link #poinOffline}/{@link #poinOnline}</b> -- nilai poin (Rupiah) untuk kehadiran offline vs
 * online pada kegiatan yang punya sesi {@code Pertemuan} sungguhan (Kajian). Untuk baris sintetis
 * "Disiplin Kehadiran" (tidak ada mode online/offline, murni evaluasi absensi bulanan), HANYA
 * {@link #poinOffline} yang dipakai sebagai nilai poinnya -- {@link #poinOnline} dibiarkan kosong/0,
 * bukan menambah kolom ketiga yang ambigu.</p>
 *
 * <p><b>{@link #durasiBerlakuHari}</b> -- masa berlaku voucher (dalam hari) sejak tanggal poin
 * diterbitkan, sebelum dihanguskan bila belum sempat dibelanjakan. Dipakai untuk mengisi
 * {@link Deposit#getTanggalExpired()} saat voucher diterbitkan (tanggal terbit + durasi ini) --
 * kosong/null berarti voucher TIDAK PERNAH kedaluwarsa.</p>
 *
 * @see Deposit#getKonfigurasiPoinKegiatan()
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "konfigurasi_poin_kegiatan")
public class KonfigurasiPoinKegiatan extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku. Nilai kosong/blank diabaikan (fail-safe agar audit shadow tidak
	 * tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	/** Menandai baris berubah; dipanggil otomatis oleh Hibernate sebelum setiap update. */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah di-set ke
	 * waktu saat ini pada deklarasi field dan di-refresh otomatis oleh {@link #onUpdate()} pada
	 * setiap update; setter ini jarang perlu dipanggil langsung.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code id-formulirKegiatan}, dipakai untuk
	 * debugging/log. Memuat ulang {@link #getFormulirKegiatan()} terlebih dahulu agar relasi
	 * lazy dimuat sebelum digabung ke string.
	 */
	public String toString() {
		formulirKegiatan = getFormulirKegiatan();
		return id + "-" + formulirKegiatan;
	}

	private FormulirKegiatan formulirKegiatan;
	private Double poinOffline;
	private Double poinOnline;
	private Integer durasiBerlakuHari;
	private Boolean aktif;
	private String keterangan;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public KonfigurasiPoinKegiatan() {
	}

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kegiatan (relasi 1-ke-1) yang konfigurasi poin ini berlaku untuknya; wajib diisi
	 * (kolom {@code NOT NULL}); relasi lazy, dimuat via {@link GeneralValueObject#check(Object)}
	 * saat pertama diakses. Lihat Javadoc class untuk penjelasan relasi 1-ke-1 ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "formulir_kegiatan", nullable = false)
	public FormulirKegiatan getFormulirKegiatan() {
		formulirKegiatan = check(formulirKegiatan);
		return formulirKegiatan;
	}

	/** @param formulirKegiatan kegiatan yang konfigurasi poin ini berlaku untuknya. */
	public void setFormulirKegiatan(FormulirKegiatan formulirKegiatan) {
		this.formulirKegiatan = formulirKegiatan;
	}

	/**
	 * @return nilai poin (Rupiah) untuk kehadiran offline pada kegiatan ini; default {@code 0.0}
	 * bila belum diisi. Lihat Javadoc class untuk penjelasan pemakaian field ini pada baris
	 * sintetis "Disiplin Kehadiran".
	 */
	@Column(name = "poin_offline", nullable = true)
	public Double getPoinOffline() {
		return poinOffline == null ? 0.0 : poinOffline;
	}

	/** @param poinOffline nilai poin (Rupiah) untuk kehadiran offline pada kegiatan ini. */
	public void setPoinOffline(Double poinOffline) {
		this.poinOffline = poinOffline;
	}

	/**
	 * @return nilai poin (Rupiah) untuk kehadiran online pada kegiatan ini; default {@code 0.0}
	 * bila belum diisi. Lihat Javadoc class: dibiarkan kosong/0 untuk baris sintetis "Disiplin
	 * Kehadiran" yang tidak punya mode online.
	 */
	@Column(name = "poin_online", nullable = true)
	public Double getPoinOnline() {
		return poinOnline == null ? 0.0 : poinOnline;
	}

	/** @param poinOnline nilai poin (Rupiah) untuk kehadiran online pada kegiatan ini. */
	public void setPoinOnline(Double poinOnline) {
		this.poinOnline = poinOnline;
	}

	/** @return jumlah hari masa berlaku voucher sebelum hangus; {@code null} = tidak pernah kedaluwarsa. */
	@Column(name = "durasi_berlaku_hari", nullable = true)
	public Integer getDurasiBerlakuHari() {
		return durasiBerlakuHari;
	}

	/** @param durasiBerlakuHari jumlah hari masa berlaku voucher sebelum hangus; {@code null} untuk tidak pernah kedaluwarsa. */
	public void setDurasiBerlakuHari(Integer durasiBerlakuHari) {
		this.durasiBerlakuHari = durasiBerlakuHari;
	}

	/** @return status aktif konfigurasi poin ini; default {@code true} bila belum pernah diisi. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif konfigurasi poin ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return keterangan tambahan untuk konfigurasi poin ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan keterangan tambahan untuk konfigurasi poin ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
