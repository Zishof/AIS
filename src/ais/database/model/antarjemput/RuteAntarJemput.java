package ais.database.model.antarjemput;

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

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code public.rute_antar_jemput}, merepresentasikan satu rute
 * (trayek) layanan antar-jemput kendaraan sekolah/perguruan tinggi pada modul "antarjemput" —
 * mendefinisikan titik awal dan akhir perjalanan, jam keberangkatan, estimasi durasi tempuh, dan
 * jenis layanannya ({@link #JEMPUT}, {@link #ANTAR}, atau {@link #PULANG_PERGI}). Entitas ini
 * tidak memiliki relasi {@code @ManyToOne}/{@code @OneToMany} eksplisit ke entitas lain; entitas
 * {@link JadwalAntarJemput} (jadwal operasional harian) dan kendaraan terkait mereferensikan rute
 * ini secara terpisah.
 * <p>
 * Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "rute_antar_jemput")
public class RuteAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439812L;

	/** Jenis layanan: rute penjemputan (dari rumah/titik kumpul menuju sekolah/kampus). */
	public static final String JEMPUT = "JEMPUT";
	/** Jenis layanan: rute pengantaran (dari sekolah/kampus menuju rumah/titik kumpul). */
	public static final String ANTAR = "ANTAR";
	/** Jenis layanan: rute pulang-pergi (jemput dan antar dalam satu rute). */
	public static final String PULANG_PERGI = "PULANG_PERGI";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	/** Jenis layanan rute; nilai valid lihat {@link #JEMPUT}, {@link #ANTAR}, {@link #PULANG_PERGI}. Default {@link #JEMPUT}. */
	private String jenisLayanan;
	/** Lokasi/alamat titik awal rute. */
	private String titikAwal;
	/** Lokasi/alamat titik akhir rute. */
	private String titikAkhir;
	/** Jam keberangkatan terjadwal untuk rute ini. */
	private Date jamBerangkat;
	/** Estimasi lama tempuh rute dalam menit. */
	private Integer estimasiMenit;
	private Boolean aktif;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public RuteAntarJemput() {
	}

	/** @return ID unik baris rute (primary key, auto-increment via {@code IDENTITY}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return ID pengguna (username) yang terakhir mengubah baris ini. Field audit shadow — lihat {@link #getOleh()}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
	 * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
	 * entitas modul antarjemput.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini (field audit shadow, diisi via {@link #onUpdate()}). */
	public String getOleh() {
		return oleh;
	}

	/** Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE, memperbarui {@link #tanggal_dirubah} (dan field audit terkait) lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return timestamp terakhir baris ini diubah; diisi otomatis saat objek dibuat dan diperbarui via {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode singkat rute ini, di-trim; {@code null} bila belum diisi. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama rute, di-trim; {@code null} bila belum diisi (tidak ada fallback — berbeda dari {@link JadwalAntarJemput#getNama()} yang memakai nama rute ini sebagai fallback-nya). */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk rute ini. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return jenis layanan rute; default {@link #JEMPUT} bila belum di-set (tidak di-cache ke field). */
	@Column(name = "jenis_layanan", length = 30)
	public String getJenisLayanan() {
		return jenisLayanan == null ? JEMPUT : jenisLayanan;
	}

	/** @param jenisLayanan lihat {@link #getJenisLayanan()}; nilai valid: {@link #JEMPUT}, {@link #ANTAR}, {@link #PULANG_PERGI}. Tidak divalidasi terhadap konstanta ini oleh setter. */
	public void setJenisLayanan(String jenisLayanan) {
		this.jenisLayanan = jenisLayanan;
	}

	/** @return lokasi/alamat titik awal rute. */
	@Column(name = "titik_awal")
	public String getTitikAwal() {
		return titikAwal;
	}

	/** @param titikAwal lihat {@link #getTitikAwal()}. */
	public void setTitikAwal(String titikAwal) {
		this.titikAwal = titikAwal;
	}

	/** @return lokasi/alamat titik akhir rute. */
	@Column(name = "titik_akhir")
	public String getTitikAkhir() {
		return titikAkhir;
	}

	/** @param titikAkhir lihat {@link #getTitikAkhir()}. */
	public void setTitikAkhir(String titikAkhir) {
		this.titikAkhir = titikAkhir;
	}

	/** @return jam keberangkatan terjadwal untuk rute ini (hanya komponen waktu yang dipersist — lihat {@code @Temporal(TIME)}). */
	@Temporal(TemporalType.TIME)
	public Date getJamBerangkat() {
		return jamBerangkat;
	}

	/** @param jamBerangkat lihat {@link #getJamBerangkat()}. */
	public void setJamBerangkat(Date jamBerangkat) {
		this.jamBerangkat = jamBerangkat;
	}

	/** @return estimasi lama tempuh rute dalam menit; default {@code 0} bila belum di-set. */
	public Integer getEstimasiMenit() {
		return estimasiMenit == null ? 0 : estimasiMenit;
	}

	/** @param estimasiMenit lihat {@link #getEstimasiMenit()}. */
	public void setEstimasiMenit(Integer estimasiMenit) {
		this.estimasiMenit = estimasiMenit;
	}

	/** @return {@code true} bila rute aktif; default {@code true} bila belum di-set (tidak di-cache ke field). */
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
