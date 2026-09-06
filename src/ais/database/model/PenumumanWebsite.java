package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
 * Entitas Hibernate untuk tabel {@code public.penumuman_website}, merepresentasikan satu
 * pengumuman/berita yang ditampilkan pada situs web publik institusi (mis. halaman "Berita
 * Kampus"). Satu baris berisi judul ({@link #getJudul()}), ringkasan ({@link #getRingkasan()}),
 * isi lengkap ({@link #getIsi()}), kategori tampilan ({@link #getKategori()}, default
 * {@code "Berita Kampus"}), tanggal publikasi ({@link #getTanggal()}), dan status tayang
 * ({@link #getAktif()}). Setiap pengumuman terikat pada satu {@link #getPerguruanTinggi()}
 * (institusi pemilik konten, relevan pada instalasi multi-institusi/multi-tenant).
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "penumuman_website")
public class PenumumanWebsite extends GeneralValueObject {
	private static final long serialVersionUID = 1L;

	private Long id;
	private String judul;
	private String ringkasan;
	private String isi;
	private String kategori;
	private Date tanggal;
	private Boolean aktif;
	private PerguruanTinggi perguruanTinggi;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @return id unik pengumuman (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * @param id id unik pengumuman.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return judul pengumuman, di-trim saat dibaca; tidak pernah null (kosong bila belum diisi).
	 */
	@Column(name = "judul", nullable = false, columnDefinition = "text")
	public String getJudul() {
		return judul == null ? "" : judul.trim();
	}

	/**
	 * @param judul judul pengumuman yang akan disimpan.
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * @return ringkasan singkat pengumuman (untuk tampilan daftar); tidak pernah null.
	 */
	@Column(name = "ringkasan", columnDefinition = "text")
	public String getRingkasan() {
		return ringkasan == null ? "" : ringkasan;
	}

	/**
	 * @param ringkasan ringkasan pengumuman.
	 */
	public void setRingkasan(String ringkasan) {
		this.ringkasan = ringkasan;
	}

	/**
	 * @return isi lengkap pengumuman (badan berita); tidak pernah null.
	 */
	@Column(name = "isi", columnDefinition = "text")
	public String getIsi() {
		return isi == null ? "" : isi;
	}

	/**
	 * @param isi isi lengkap pengumuman.
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * @return kategori tampilan pengumuman; default {@code "Berita Kampus"} bila belum
	 *         diisi atau hanya berisi spasi.
	 */
	@Column(name = "kategori")
	public String getKategori() {
		return kategori == null || kategori.trim().isEmpty() ? "Berita Kampus" : kategori.trim();
	}

	/**
	 * @param kategori kategori tampilan pengumuman.
	 */
	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	/**
	 * @return tanggal publikasi pengumuman; bila belum diisi, mengembalikan tanggal
	 *         hari ini sebagai fallback (bukan null).
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * @param tanggal tanggal publikasi pengumuman.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return status tayang pengumuman; default {@code true} bila belum pernah diisi
	 *         (flag aktif satu-arah — baris lama tanpa nilai eksplisit dianggap tayang).
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status tayang/nonaktif pengumuman.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return institusi ({@link PerguruanTinggi}) pemilik pengumuman ini — batas
	 *         kepemilikan/tenant pada instalasi multi-institusi; relasi lazy,
	 *         di-"check" (dilewatkan proxy-safe helper) sebelum dikembalikan.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		return perguruanTinggi;
	}

	/**
	 * @param perguruanTinggi institusi pemilik pengumuman ini.
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * @return nama pembuat/pengunggah pengumuman (label tampilan, bukan id).
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyimpan nama pembuat pengumuman. CATATAN: pengisi hanya diterapkan bila
	 * {@code oleh} tidak null/kosong — sekali terisi, nilai ini TIDAK BISA
	 * dikosongkan lagi lewat setter ini (write-guard satu-arah, bukan bug baru;
	 * mengikuti pola arsip yang sama di banyak entitas lain).
	 *
	 * @param oleh nama pembuat pengumuman.
	 */
	public void setOleh(String oleh) {
		if (oleh != null && !oleh.trim().isEmpty()) {
			this.oleh = oleh;
		}
	}

	/**
	 * @return id akun pembuat/pengunggah pengumuman.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat pengumuman. Guard sama seperti {@link #setOleh(String)}:
	 * nilai kosong/null diabaikan, nilai yang sudah terisi tidak bisa dikosongkan lagi.
	 *
	 * @param olehId id pembuat pengumuman.
	 */
	public void setOlehId(String olehId) {
		if (olehId != null && !olehId.trim().isEmpty()) {
			this.olehId = olehId;
		}
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah}
	 * setiap kali baris ini di-update. Field ini adalah kebutuhan teknis, bukan bug.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return judul pengumuman, dipakai sebagai representasi ringkas untuk log/debug.
	 */
	public String toString() {
		return getJudul();
	}
}
