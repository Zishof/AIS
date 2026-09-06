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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate untuk tabel {@code public.checklist_penilaian_dosen_oleh_mahasiswa},
 * merepresentasikan satu jawaban penilaian yang diberikan seorang {@link #getMahasiswa()}
 * terhadap seorang {@link #getDosen()} atas satu butir {@link #getChecklistPenilaianDosen()}
 * (item checklist penilaian dosen), pada {@link #getTahunAkademik()}/{@link #getSemester()} dan
 * (opsional) untuk mata kuliah {@link #getPerkuliahan()} tertentu. Nilai skor tersimpan di
 * {@link #getNilai()} menggunakan skala konstanta kelas ini: {@link #SANGAT_BAIK} (1) sampai
 * {@link #BURUK} (5). {@link #getChecklistBaruPenilaianDosenOlehMahasiswa()} menghubungkan baris
 * ini ke versi/skema checklist penilaian yang lebih baru bila berlaku.
 * <p>
 * Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "checklist_penilaian_dosen_oleh_mahasiswa")
public class ChecklistPenilaianDosenOlehMahasiswa extends GeneralValueObject {

	/** Skor "sangat baik" (nilai terbaik) pada skala penilaian {@link #getNilai()}. */
	public static final Integer SANGAT_BAIK = 1;
	/** Skor "baik". */
	public static final Integer BAIK = 2;
	/** Skor "cukup". */
	public static final Integer CUKUP = 3;
	/** Skor "kurang baik". */
	public static final Integer KURANG_BAIK = 4;
	/** Skor "buruk" (nilai terendah) pada skala penilaian {@link #getNilai()}. */
	public static final Integer BURUK = 5;

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Mahasiswa yang memberikan penilaian. */
	private Mahasiswa mahasiswa;
	private String tahunAkademik;
	private Integer semester;
	/** Skor penilaian; lihat konstanta {@link #SANGAT_BAIK}..{@link #BURUK}. */
	private Integer nilai;
	/** Dosen yang dinilai. */
	private Dosen dosen;
	/** Mata kuliah/kelas kuliah konteks penilaian ini, bila ada. */
	private Perkuliahan perkuliahan;
	/** Butir checklist penilaian yang dijawab oleh baris ini. */
	private ChecklistPenilaianDosen checklistPenilaianDosen;
	private String keterangan;
	/** Referensi ke skema checklist penilaian dosen versi baru, bila baris ini memakainya. */
	private ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public ChecklistPenilaianDosenOlehMahasiswa() {
	}

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa
	 * nilai lama) — write-guard satu-arah, konsisten dengan pola arsip lain.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId.trim();
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa
	 * nilai lama) — write-guard satu-arah, konsisten dengan pola arsip lain.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh.trim();
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
	 * @return {@link #getKeterangan()}, atau string kosong bila belum diisi —
	 *         dipakai untuk keperluan log/debug.
	 */
	@Override
	public String toString() {
		return keterangan == null ? "" : keterangan;
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
	 * @return keterangan/catatan tambahan pada penilaian ini.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan/catatan tambahan pada penilaian ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return mahasiswa yang memberikan penilaian ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * @param mahasiswa mahasiswa yang memberikan penilaian ini.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return dosen yang dinilai.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return dosen;
	}

	/**
	 * @param dosen dosen yang dinilai.
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * @return butir checklist penilaian yang dijawab oleh baris ini (relasi lazy,
	 *         di-refresh/divalidasi via {@code check(...)} saat dibaca).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_penilaian_dosen", nullable = false)
	public ChecklistPenilaianDosen getChecklistPenilaianDosen() {
		checklistPenilaianDosen = check(checklistPenilaianDosen);
		return checklistPenilaianDosen;
	}

	/**
	 * @param checklistPenilaianDosen butir checklist penilaian yang dijawab oleh baris ini.
	 */
	public void setChecklistPenilaianDosen(ChecklistPenilaianDosen checklistPenilaianDosen) {
		this.checklistPenilaianDosen = checklistPenilaianDosen;
	}

	/**
	 * @return tahun akademik saat penilaian ini diberikan.
	 */
	@Column(name = "tahun_akademik", nullable = false)
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * @param tahunAkademik tahun akademik saat penilaian ini diberikan.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return semester saat penilaian ini diberikan.
	 */
	@Column(name = "semester", nullable = false)
	public Integer getSemester() {
		return semester;
	}

	/**
	 * @param semester semester saat penilaian ini diberikan.
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * @return skor penilaian; lihat konstanta {@link #SANGAT_BAIK}..{@link #BURUK}.
	 */
	@Column(name = "nilai", nullable = false)
	public Integer getNilai() {
		return nilai;
	}

	/**
	 * @param nilai skor penilaian; lihat konstanta {@link #SANGAT_BAIK}..{@link #BURUK}.
	 */
	public void setNilai(Integer nilai) {
		this.nilai = nilai;
	}

	/**
	 * @return mata kuliah/kelas kuliah konteks penilaian ini, bila ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		return perkuliahan;
	}

	/**
	 * @param perkuliahan mata kuliah/kelas kuliah konteks penilaian ini.
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * @return referensi ke skema checklist penilaian dosen versi baru, bila baris
	 *         ini memakainya (null bila memakai skema lama/{@link #getChecklistPenilaianDosen()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "checklist_baru_penilaian_dosen_oleh_mahasiswa", nullable = true)
	public ChecklistBaruPenilaianDosenOlehMahasiswa getChecklistBaruPenilaianDosenOlehMahasiswa() {
		return checklistBaruPenilaianDosenOlehMahasiswa;
	}

	/**
	 * @param checklistBaruPenilaianDosenOlehMahasiswa referensi ke skema checklist
	 *                                                  penilaian dosen versi baru.
	 */
	public void setChecklistBaruPenilaianDosenOlehMahasiswa(
			ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa) {
		this.checklistBaruPenilaianDosenOlehMahasiswa = checklistBaruPenilaianDosenOlehMahasiswa;
	}
}
