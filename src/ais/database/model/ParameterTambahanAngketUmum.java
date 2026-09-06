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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Konfigurasi parameter tambahan untuk angket umum, angket dosen, dan angket guru.
 *
 * Catatan kompatibilitas:
 * - Relasi lama ke GrupChecklistPenilaianUmum tetap dipertahankan.
 * - Relasi baru ke GrupChecklistPenilaianDosen dan GrupChecklistPenilaianGuru bersifat optional.
 * - Satu baris cukup mengacu ke salah satu grup target saja.
 *
 * <h2>Desain "target grup" (BUKAN pola tabrakan jenis/ref)</h2>
 * <p>
 * Berbeda dari pola tabrakan namespace jenis+ref yang tercatat di banyak entitas
 * lampiran/parameter-tambahan lain (id generik + kolom {@code jenis} diskriminator
 * yang bisa salah ditafsir modul), entitas ini memakai TIGA kolom FK terpisah
 * ({@link #getGrupChecklistPenilaianUmum()}, {@link #getGrupChecklistPenilaianDosen()},
 * {@link #getGrupChecklistPenilaianGuru()}) dan invarian "hanya satu yang terisi"
 * ditegakkan di level setter (mengosongkan dua field lain begitu satu di-set).
 * Tidak ditemukan tabrakan namespace generik id+jenis di entitas ini.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "parameter_tambahan_angket_umum")
public class ParameterTambahanAngketUmum extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Fakultas fakultas;
	private Jurusan jurusan;
	private String program;
	private Jenjang jenjang;
	private ParameterTambahan parameterTambahan;
	private Boolean tampilDiSemuaTahunAngkatan;
	private String tahunAngkatans;
	private GrupChecklistPenilaianUmum grupChecklistPenilaianUmum;
	private GrupChecklistPenilaianDosen grupChecklistPenilaianDosen;
	private GrupChecklistPenilaianGuru grupChecklistPenilaianGuru;
	private Integer nomorUrut;
	private Yayasan yayasan;
	private Sekolah sekolah;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public ParameterTambahanAngketUmum() {
	}

	/**
	 * @return id unik baris konfigurasi (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * @param id id unik baris konfigurasi.
	 */
	public void setId(Long id) {
		this.id = id;
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
		this.olehId = olehId;
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
		this.oleh = oleh;
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
	 * @return jurusan target parameter tambahan ini berlaku (opsional — null berarti
	 *         berlaku lintas jurusan); relasi lazy, di-"check" sebelum dikembalikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param jurusan jurusan target; diabaikan (disimpan sebagai null) bila objek
	 *                yang diberikan belum punya id (transient/detached).
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan == null || jurusan.getId() == null ? null : jurusan;
	}

	/**
	 * @return definisi {@link ParameterTambahan} generik yang dikonfigurasi baris ini
	 *         untuk tampil pada angket; relasi wajib (nullable=false pada kolom DB).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * @param parameterTambahan definisi parameter tambahan terkait; diabaikan
	 *                          (disimpan null) bila belum punya id.
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan == null || parameterTambahan.getId() == null ? null : parameterTambahan;
	}

	/**
	 * @return apakah parameter ini ditampilkan untuk semua tahun angkatan; default
	 *         {@code true} bila belum pernah diisi (flag satu-arah).
	 */
	@Column(name = "tampil_di_semua_tahun_angkatan")
	public Boolean getTampilDiSemuaTahunAngkatan() {
		return tampilDiSemuaTahunAngkatan == null ? Boolean.TRUE : tampilDiSemuaTahunAngkatan;
	}

	/**
	 * @param tampilDiSemuaTahunAngkatan status tampil-di-semua-tahun-angkatan.
	 */
	public void setTampilDiSemuaTahunAngkatan(Boolean tampilDiSemuaTahunAngkatan) {
		this.tampilDiSemuaTahunAngkatan = tampilDiSemuaTahunAngkatan;
	}

	/**
	 * @return daftar tahun angkatan (mentah, biasanya dipisah koma/delimiter tertentu
	 *         oleh pemanggil) yang menjadi target bila {@link #getTampilDiSemuaTahunAngkatan()}
	 *         bernilai false; tidak pernah null.
	 */
	@Column(columnDefinition = "text")
	public String getTahunAngkatans() {
		return tahunAngkatans == null ? "" : tahunAngkatans;
	}

	/**
	 * @param tahunAngkatans daftar tahun angkatan target.
	 */
	public void setTahunAngkatans(String tahunAngkatans) {
		this.tahunAngkatans = tahunAngkatans;
	}

	/**
	 * @return fakultas target parameter tambahan ini berlaku (opsional); relasi lazy,
	 *         di-"check" sebelum dikembalikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * @param fakultas fakultas target; diabaikan (disimpan null) bila belum punya id.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas == null || fakultas.getId() == null ? null : fakultas;
	}

	/**
	 * @return yayasan pemilik konfigurasi ini. Bila {@link #getParameterTambahan()}
	 *         sendiri sudah punya yayasan, nilai TERSEBUT yang dipakai (menimpa field
	 *         {@link #yayasan} lokal) — yayasan pada parameter induk dianggap sumber
	 *         kebenaran; relasi lazy, di-"check" sebelum dikembalikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		ParameterTambahan pt = getParameterTambahan();
		if (pt != null && pt.getYayasan() != null) {
			yayasan = pt.getYayasan();
		}
		return yayasan;
	}

	/**
	 * @param yayasan yayasan pemilik; diabaikan (disimpan null) bila belum punya id.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * @return sekolah pemilik konfigurasi ini. Sama seperti {@link #getYayasan()},
	 *         bila {@link #getParameterTambahan()} punya sekolah sendiri, nilai
	 *         tersebut yang dipakai (menimpa field lokal); relasi lazy.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		ParameterTambahan pt = getParameterTambahan();
		if (pt != null && pt.getSekolah() != null) {
			sekolah = pt.getSekolah();
		}
		return sekolah;
	}

	/**
	 * @param sekolah sekolah pemilik; diabaikan (disimpan null) bila belum punya id.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * @return nama program studi (teks bebas) target parameter tambahan ini, bila ada.
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * @param program nama program studi target.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * @return jenjang pendidikan target parameter tambahan ini berlaku (opsional);
	 *         relasi lazy, di-"check" sebelum dikembalikan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * @param jenjang jenjang target; diabaikan (disimpan null) bila belum punya id.
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang == null || jenjang.getId() == null ? null : jenjang;
	}

	/**
	 * @return grup checklist penilaian UMUM target parameter ini (relasi lama,
	 *         tetap dipertahankan demi kompatibilitas mundur). Salah satu dari
	 *         tiga relasi grup ({@code Umum}/{@code Dosen}/{@code Guru}) yang saling
	 *         eksklusif — lihat catatan desain pada Javadoc kelas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_umum", nullable = true)
	public GrupChecklistPenilaianUmum getGrupChecklistPenilaianUmum() {
		grupChecklistPenilaianUmum = check(grupChecklistPenilaianUmum);
		return grupChecklistPenilaianUmum;
	}

	/**
	 * Menetapkan grup target sebagai "Umum". Menegakkan invarian mutual-eksklusif:
	 * bila grup ini diisi (id valid), field {@code grupChecklistPenilaianDosen} dan
	 * {@code grupChecklistPenilaianGuru} otomatis dikosongkan.
	 *
	 * @param grupChecklistPenilaianUmum grup checklist penilaian umum target.
	 */
	public void setGrupChecklistPenilaianUmum(GrupChecklistPenilaianUmum grupChecklistPenilaianUmum) {
		this.grupChecklistPenilaianUmum = grupChecklistPenilaianUmum == null || grupChecklistPenilaianUmum.getId() == null ? null
				: grupChecklistPenilaianUmum;
		if (this.grupChecklistPenilaianUmum != null) {
			this.grupChecklistPenilaianDosen = null;
			this.grupChecklistPenilaianGuru = null;
		}
	}

	/**
	 * @return grup checklist penilaian DOSEN target parameter ini (relasi baru,
	 *         opsional). Lihat catatan mutual-eksklusif pada
	 *         {@link #getGrupChecklistPenilaianUmum()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_dosen", nullable = true)
	public GrupChecklistPenilaianDosen getGrupChecklistPenilaianDosen() {
		grupChecklistPenilaianDosen = check(grupChecklistPenilaianDosen);
		return grupChecklistPenilaianDosen;
	}

	/**
	 * Menetapkan grup target sebagai "Dosen". Menegakkan invarian mutual-eksklusif:
	 * bila grup ini diisi (id valid), field {@code grupChecklistPenilaianUmum} dan
	 * {@code grupChecklistPenilaianGuru} otomatis dikosongkan.
	 *
	 * @param grupChecklistPenilaianDosen grup checklist penilaian dosen target.
	 */
	public void setGrupChecklistPenilaianDosen(GrupChecklistPenilaianDosen grupChecklistPenilaianDosen) {
		this.grupChecklistPenilaianDosen = grupChecklistPenilaianDosen == null || grupChecklistPenilaianDosen.getId() == null ? null
				: grupChecklistPenilaianDosen;
		if (this.grupChecklistPenilaianDosen != null) {
			this.grupChecklistPenilaianUmum = null;
			this.grupChecklistPenilaianGuru = null;
		}
	}

	/**
	 * @return grup checklist penilaian GURU target parameter ini (relasi baru,
	 *         opsional, khusus modul sekolah). Lihat catatan mutual-eksklusif pada
	 *         {@link #getGrupChecklistPenilaianUmum()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_guru", nullable = true)
	public GrupChecklistPenilaianGuru getGrupChecklistPenilaianGuru() {
		grupChecklistPenilaianGuru = check(grupChecklistPenilaianGuru);
		return grupChecklistPenilaianGuru;
	}

	/**
	 * Menetapkan grup target sebagai "Guru". Menegakkan invarian mutual-eksklusif:
	 * bila grup ini diisi (id valid), field {@code grupChecklistPenilaianUmum} dan
	 * {@code grupChecklistPenilaianDosen} otomatis dikosongkan.
	 *
	 * @param grupChecklistPenilaianGuru grup checklist penilaian guru target.
	 */
	public void setGrupChecklistPenilaianGuru(GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this.grupChecklistPenilaianGuru = grupChecklistPenilaianGuru == null || grupChecklistPenilaianGuru.getId() == null ? null
				: grupChecklistPenilaianGuru;
		if (this.grupChecklistPenilaianGuru != null) {
			this.grupChecklistPenilaianUmum = null;
			this.grupChecklistPenilaianDosen = null;
		}
	}

	/**
	 * @return nomor urut tampilan parameter ini. Getter DESTRUKTIF: setiap kali
	 *         dipanggil, nilai selalu ditimpa dari {@link #getParameterTambahan()}
	 *         (bila ada) sebelum dikembalikan — nomor urut lokal hanyalah cache
	 *         turunan dari parameter induk, bukan sumber kebenaran independen.
	 *         Default {@code 1} bila parameter induk tidak punya nomor urut.
	 */
	public Integer getNomorUrut() {
		ParameterTambahan pt = getParameterTambahan();
		if (pt != null) {
			nomorUrut = pt.getNomorUrut();
		}
		return nomorUrut == null ? Integer.valueOf(1) : nomorUrut;
	}

	/**
	 * @param nomorUrut nomor urut tampilan (akan ditimpa ulang oleh getter bila
	 *                  parameter induk punya nomor urut sendiri — lihat {@link #getNomorUrut()}).
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return label ringkas grup target yang sedang aktif, mis. {@code "Umum - <isi>"},
	 *         {@code "Dosen - <isi>"}, atau {@code "Guru - <isi>"} — tergantung mana
	 *         dari ketiga relasi grup yang terisi (mutual-eksklusif). String kosong
	 *         bila tidak ada satu pun grup terisi atau bila terjadi exception saat
	 *         memuat relasi lazy (dicatat via {@code ErrorAuditUtil}). Bukan kolom
	 *         DB — dihitung setiap dipanggil ({@code @Transient}).
	 */
	@Transient
	public String getTargetGrupLabel() {
		try {
			if (getGrupChecklistPenilaianUmum() != null) {
				return "Umum - " + getGrupChecklistPenilaianUmum().getIsi();
			}
			if (getGrupChecklistPenilaianDosen() != null) {
				return "Dosen - " + getGrupChecklistPenilaianDosen().getIsi();
			}
			if (getGrupChecklistPenilaianGuru() != null) {
				return "Guru - " + getGrupChecklistPenilaianGuru().getIsi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ParameterTambahanAngketUmum.java:283");
		}
		return "";
	}

	/**
	 * @return representasi ringkas "{id}-{label grup target} - {label input parameter}",
	 *         dipakai untuk keperluan log/debug/tampilan pilihan.
	 */
	public String toString() {
		ParameterTambahan pt = getParameterTambahan();
		return (getId() == null ? "" : getId() + "-") + getTargetGrupLabel() + " - "
				+ (pt == null ? "" : pt.getLabelInputan());
	}
}
