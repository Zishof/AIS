package ais.database.model;

// Generated Dec 22, 2009 12:14:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.kkn.KelompokKkn;
import ais.database.model.library.Item;
import ais.database.model.pkl.KelompokPkl;

/**
 * Baris <b>penyerahan/keterkaitan pustaka {@link Item}</b> (tabel {@code public.data_punya_item}) —
 * menandai bahwa satu {@link Item} (koleksi perpustakaan) diserahkan/terkait dengan SATU dari beberapa
 * kemungkinan konteks tugas akhir/kegiatan mahasiswa: {@link Skripsi},
 * {@link MahasiswaRequestTugasAkhir}, {@link JadwalUjianPMB}, {@link KelompokKkn}, atau
 * {@link KelompokPkl}. Hanya {@link #getItem()} yang wajib diisi; field konteks lainnya bersifat
 * opsional dan saling eksklusif secara konvensi (hanya salah satu yang relevan per baris, tidak
 * ditegakkan lewat constraint DB).
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "data_punya_item")

public class DataPunyaItem extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1950126270979098967L;
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

	/**
	 * @return representasi ringkas "{skripsi}_{item}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return skripsi + "_" + item;
	}

	private Skripsi skripsi;
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private JadwalUjianPMB jadwalUjianPMB;
	private KelompokKkn kelompokKkn;
	private KelompokPkl kelompokPkl;
	private Item item;
	private String keterangan;

	/**
	 * @return keterangan/catatan bebas tentang baris ini.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * @param keterangan keterangan/catatan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public DataPunyaItem() {
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
	 * @return konteks skripsi terkait, atau {@code null} bila baris ini menyangkut konteks lain.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skripsi", nullable = true)
	public Skripsi getSkripsi() {
		skripsi = check(skripsi);
		return skripsi;
	}

	/**
	 * @param skripsi konteks skripsi terkait.
	 */
	public void setSkripsi(Skripsi skripsi) {
		this.skripsi = skripsi;
	}

	/**
	 * @return item pustaka yang diserahkan/terkait (wajib diisi).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = false)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * @param item item pustaka yang diserahkan/terkait.
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * @return konteks pengajuan tugas akhir terkait, atau {@code null} bila baris ini menyangkut
	 *         konteks lain.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa_request_tugas_akhir", nullable = true)
	public MahasiswaRequestTugasAkhir getMahasiswaRequestTugasAkhir() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		return mahasiswaRequestTugasAkhir;
	}

	/**
	 * @param mahasiswaRequestTugasAkhir konteks pengajuan tugas akhir terkait.
	 */
	public void setMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) {
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
	}

	/**
	 * @return konteks jadwal ujian PMB terkait, atau {@code null} bila baris ini menyangkut konteks
	 *         lain.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_ujian_pmb", nullable = true)
	public JadwalUjianPMB getJadwalUjianPMB() {
		jadwalUjianPMB = check(jadwalUjianPMB);
		return jadwalUjianPMB;
	}

	/**
	 * @param jadwalUjianPMB konteks jadwal ujian PMB terkait.
	 */
	public void setJadwalUjianPMB(JadwalUjianPMB jadwalUjianPMB) {
		this.jadwalUjianPMB = jadwalUjianPMB;
	}

	/**
	 * @return konteks kelompok KKN terkait, atau {@code null} bila baris ini menyangkut konteks lain.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kkn", nullable = true)
	public KelompokKkn getKelompokKkn() {
		kelompokKkn = check(kelompokKkn);
		return kelompokKkn;
	}

	/**
	 * @param kelompokKkn konteks kelompok KKN terkait.
	 */
	public void setKelompokKkn(KelompokKkn kelompokKkn) {
		this.kelompokKkn = kelompokKkn;
	}

	/**
	 * @return konteks kelompok PKL terkait, atau {@code null} bila baris ini menyangkut konteks lain.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_pkl", nullable = true)
	public KelompokPkl getKelompokPkl() {
		kelompokPkl = check(kelompokPkl);
		return kelompokPkl;
	}

	/**
	 * @param kelompokPkl konteks kelompok PKL terkait.
	 */
	public void setKelompokPkl(KelompokPkl kelompokPkl) {
		this.kelompokPkl = kelompokPkl;
	}

}
