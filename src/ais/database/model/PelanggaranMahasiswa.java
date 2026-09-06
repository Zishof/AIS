package ais.database.model;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.sekolah.Hukuman;
import ais.database.model.sekolah.Pelanggaran;
import ais.database.model.sekolah.PelanggaranDanHukuman;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>catatan pelanggaran mahasiswa</b> (tabel {@code public.pelanggaran_mahasiswa}) — padanan
 * {@link ais.database.model.sekolah.PelanggaranSiswa} tapi untuk mahasiswa perguruan tinggi. Satu
 * baris mencatat SATU insiden pelanggaran seorang {@link Mahasiswa}, jenis pelanggaran+hukuman
 * ({@link #getPelanggaranDanHukuman()}), serta rincian pelanggaran ({@link #getPelanggarans()}) dan
 * hukuman ({@link #getHukumans()}) yang berlaku lewat dua tabel pivot many-to-many. Bisa ditampilkan
 * sebagai notifikasi ke mahasiswa saat login ({@link #getTampilkanInfoIniSaatMahasiswaLogin()}),
 * dibatasi sampai tanggal tertentu ({@link #getBatasWaktuDitampilkan()}).
 *
 * <h3>Catatan keamanan — verifikasi pola fail-open OrangTua (BEDA dari task_5e93a600)</h3>
 * <p>task_5e93a600 mencatat fail-open pada {@code OrangTua.ambilAnakSiswa()} untuk modul SISWA
 * (sekolah). Untuk entity MAHASISWA ini, ditemukan pola BERBEDA namun serupa dampaknya: pada
 * {@code ais.action.master.pelanggaran.DasbordPelanggaran#muatPelanggaranMahasiswa(DashData, Mahasiswa)},
 * parameter {@code mhs} HANYA diisi bila user yang login adalah mahasiswa itu sendiri (memfilter
 * {@code Restrictions.eq("mahasiswa", mhs)}). Untuk role personal LAIN yang punya
 * {@code bolehLihatSemua} (orang tua, guru, dosen, pegawai — {@code lingkup MAHASISWA/SEMUA}), method
 * ini dipanggil dengan {@code mhs == null} dan TIDAK menerima parameter {@code ortu} sama sekali
 * (berbeda dari {@code muatPelanggaranSiswa} yang eksplisit menyaring anak orang tua via
 * {@code ambilAnakSiswa()} dan fail-closed bila kosong) — sehingga criteria query TIDAK PERNAH
 * diberi restriksi apa pun, dan SELURUH baris {@code PelanggaranMahasiswa} di seluruh perguruan tinggi
 * (dibatasi {@code MAX_ROWS}) ditampilkan ke dasbor akun personal non-mahasiswa mana pun. Ini
 * ditandai terpisah sebagai temuan baru (bukan perluasan task_5e93a600) karena akar penyebabnya beda:
 * bukan FK yang salah, melainkan filter kepemilikan yang memang tidak pernah ditulis untuk cabang
 * ini.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pelanggaran_mahasiswa", schema = "public")
public class PelanggaranMahasiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private Mahasiswa mahasiswa;
	private PelanggaranDanHukuman pelanggaranDanHukuman;
	private Date waktu;
	private String keterangan;
	private String nama;
	private String ta;
	private Boolean aktif;

	private Boolean tampilkanInfoIniSaatMahasiswaLogin;
	private Date batasWaktuDitampilkan;

	private Set<Hukuman> hukumans = new HashSet<Hukuman>();

	/**
	 * @return himpunan hukuman yang dijatuhkan untuk pelanggaran ini, terurut nama, lewat tabel pivot
	 *         {@code pelanggaran_mahasiswa_has_hukuman}.
	 */
	@ManyToMany(targetEntity = Hukuman.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_mahasiswa_has_hukuman", schema = "public", joinColumns = @JoinColumn(name = "pelanggaran_mahasiswa"), inverseJoinColumns = @JoinColumn(name = "hukuman"))
	public Set<Hukuman> getHukumans() {
		return hukumans;
	}

	/**
	 * @param hukumans himpunan hukuman yang dijatuhkan.
	 */
	public void setHukumans(Set<Hukuman> hukumans) {
		this.hukumans = hukumans;
	}

	private Set<Pelanggaran> pelanggarans = new HashSet<Pelanggaran>();

	/**
	 * @return himpunan jenis pelanggaran yang tercatat pada insiden ini, terurut nama, lewat tabel
	 *         pivot {@code pelanggaran_mahasiswa_has_pelanggaran}.
	 */
	@ManyToMany(targetEntity = Pelanggaran.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_mahasiswa_has_pelanggaran", schema = "public", joinColumns = @JoinColumn(name = "pelanggaran_mahasiswa"), inverseJoinColumns = @JoinColumn(name = "pelanggaran"))
	public Set<Pelanggaran> getPelanggarans() {
		return pelanggarans;
	}

	/**
	 * @param pelanggarans himpunan jenis pelanggaran yang tercatat.
	 */
	public void setPelanggarans(Set<Pelanggaran> pelanggarans) {
		this.pelanggarans = pelanggarans;
	}

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public PelanggaranMahasiswa() {
	}

	/**
	 * Konstruktor ringkas untuk kebutuhan referensi cepat (mis. lookup/pengujian) tanpa mengisi
	 * seluruh relasi.
	 *
	 * @param id   id baris.
	 * @param nama nama/ringkasan baris.
	 */
	public PelanggaranMahasiswa(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return mahasiswa yang melakukan pelanggaran ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa_id", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return this.mahasiswa;
	}

	/**
	 * @param mahasiswa mahasiswa yang melakukan pelanggaran ini.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return keterangan/kronologi pelanggaran ini.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan/kronologi pelanggaran ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return nama ringkas baris ini — getter-mutasi: SELALU dihitung ulang dari
	 *         "{mahasiswa}_{pelanggaranDanHukuman}_{waktu}" setiap dipanggil, menimpa nilai
	 *         {@link #nama} yang mungkin di-set manual sebelumnya.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		nama = getMahasiswa() + "_" + getPelanggaranDanHukuman() + "_" + getWaktu();
		return this.nama;
	}

	/**
	 * @param nama nama ringkas baris ini (lihat catatan getter — nilai ini akan ditimpa saat dibaca
	 *             kembali).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return {@code true} bila baris ini aktif/berlaku; default {@code true} ({@code null}
	 *         dianggap aktif).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif/nonaktif baris ini.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return jenis pelanggaran+hukuman utama yang dipakai untuk insiden ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pelanggaran_dan_hukuman", nullable = false)
	public PelanggaranDanHukuman getPelanggaranDanHukuman() {
		pelanggaranDanHukuman = check(pelanggaranDanHukuman);
		return pelanggaranDanHukuman;
	}

	/**
	 * @param pelanggaranDanHukuman jenis pelanggaran+hukuman utama.
	 */
	public void setPelanggaranDanHukuman(PelanggaranDanHukuman pelanggaranDanHukuman) {
		this.pelanggaranDanHukuman = pelanggaranDanHukuman;
	}

	/**
	 * @return waktu terjadinya pelanggaran; default waktu saat ini bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * @param waktu waktu terjadinya pelanggaran.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * @return tahun akademik pelanggaran ini tercatat; default tahun akademik berjalan
	 *         ({@link Common#getCurrentTahunAkademik()}) bila belum diisi.
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/**
	 * @param ta tahun akademik pelanggaran ini tercatat.
	 */
	public void setTa(String ta) {
		this.ta = ta;
	}

	/**
	 * @return batas tanggal notifikasi pelanggaran ini masih ditampilkan ke mahasiswa saat login,
	 *         atau {@code null} bila tidak dibatasi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getBatasWaktuDitampilkan() {
		return batasWaktuDitampilkan;
	}

	/**
	 * @param batasWaktuDitampilkan batas tanggal notifikasi masih ditampilkan.
	 */
	public void setBatasWaktuDitampilkan(Date batasWaktuDitampilkan) {
		this.batasWaktuDitampilkan = batasWaktuDitampilkan;
	}

	/**
	 * @return {@code true} bila pelanggaran ini ditampilkan sebagai notifikasi saat mahasiswa terkait
	 *         login; default {@code true} ({@code null} dianggap tampil).
	 */
	public Boolean getTampilkanInfoIniSaatMahasiswaLogin() {
		return tampilkanInfoIniSaatMahasiswaLogin == null ? true : tampilkanInfoIniSaatMahasiswaLogin;
	}

	/**
	 * @param tampilkanInfoIniSaatMahasiswaLogin status tampil sebagai notifikasi login.
	 */
	public void setTampilkanInfoIniSaatMahasiswaLogin(Boolean tampilkanInfoIniSaatMahasiswaLogin) {
		this.tampilkanInfoIniSaatMahasiswaLogin = tampilkanInfoIniSaatMahasiswaLogin;
	}

}
