package ais.database.model.temp;

// Generated Dec 12, 2009 7:42:38 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.Perkuliahan;



/**
 * Entitas Hibernate (skema {@code public}, tabel {@code detailperkuliahan_temp}) yang menjadi
 * BUFFER/staging detail perkuliahan mahasiswa per matakuliah (nilai, IP, status persetujuan
 * konversi nilai) — nama dan struktur menunjukkan kelas ini adalah versi sementara/percobaan dari
 * entitas {@code Detailperkuliahan} (non-temp) yang menjadi tabel resmi pada modul akademik.
 *
 * <p>
 * <b>Status dorman — TERVERIFIKASI</b>: kelas ini terdaftar di pemetaan Hibernate
 * ({@code hibernate.cfg.xml}) tetapi TIDAK direferensikan oleh Action/Helper/API manapun di
 * seluruh codebase AIS (pencarian menyeluruh terhadap nama kelas fully-qualified hanya menemukan
 * file ini sendiri dan baris registrasi mapping) — konsisten dengan sifatnya sebagai staging data
 * lama yang sudah tidak dipakai proses bisnis aktif. Relasi ke {@link NilaiTemp} (lihat
 * {@code detailperkuliahan} pada kelas tersebut) turut dorman untuk alasan yang sama.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "detailperkuliahan_temp")



public class DetailperkuliahanTemp extends GeneralValueObject {

	/** Nilai {@link #persetujuan}: sudah disetujui (mis. konversi nilai matakuliah sudah disahkan). */
	public static final Integer DISETUJUI = 1;
	/** Nilai {@link #persetujuan}: belum disetujui (nilai default). */
	public static final Integer BELUM_DISETUJUI = 0;

	/**
	 *
	 */
	private static final long serialVersionUID = 8612385827123829867L;
	private Long id;
	/** Field audit shadow (bukan kolom Hibernate): nama pemroses terakhir, diisi lewat {@link #setOleh(String)}. */
	private String oleh;/** Field audit shadow (bukan kolom Hibernate): ID pemroses terakhir, diisi lewat {@link #setOlehId(String)}. */private String olehId;/** @return ID pemroses terakhir yang mengubah baris ini (field audit shadow). */public String getOlehId() {return olehId;}/**
	 * Menyetel ID pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed): bila
	 * {@code olehId} null atau hanya berisi spasi, method ini langsung {@code return} tanpa
	 * mengubah field, mempertahankan nilai audit sebelumnya.
	 *
	 * @param olehId ID pemroses yang akan diset; diabaikan bila null/kosong.
	 */public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed) dengan
	 * pola yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOleh(String)}). */
	public String getOleh() {
		return oleh;
	}

	/** Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini di-update. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang akan diset. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (diperbarui otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas: {@link #perkuliahan} diikuti {@link #mahasiswa}. */
	public String toString() {
		return perkuliahan + "_" + mahasiswa;
	}

	/** Mahasiswa pemilik detail perkuliahan ini. */
	private Mahasiswa mahasiswa;
	/** Matakuliah/kelas perkuliahan yang diambil. */
	private Perkuliahan perkuliahan;
	/** Perkuliahan alternatif yang diikuti (mis. kelas pengganti), bila berbeda dari {@link #perkuliahan}. */
	private Perkuliahan ikutiPerkuliahan;
	/** Semester saat matakuliah ini diambil. */
	private Integer semester;
	/** Total nilai numerik matakuliah ini; default {@code 0.0}. */
	private Double totalNilai = 0.0;
	/** Nilai huruf (mis. "A", "B+"); default string kosong. */
	private String nilaiHuruf = "";
	/** Total IP (indeks prestasi) yang disumbang matakuliah ini; default {@code 0.0}. */
	private Double totalIP = 0.0;
	/** Status persetujuan (lihat {@link #DISETUJUI}/{@link #BELUM_DISETUJUI}); default belum disetujui, otomatis menjadi disetujui bila {@link #matakuliahKonversi} terisi (lihat {@link #getPersetujuan()}). */
	private Integer persetujuan = BELUM_DISETUJUI;
	/** Matakuliah hasil konversi (bila nilai ini adalah hasil konversi dari matakuliah lain). */
	private Matakuliah matakuliahKonversi;
	/** Matakuliah asli sebelum dikonversi ke {@link #matakuliahKonversi}. */
	private Matakuliah matakuliahAsliSebelumKonversi;

	/** Paket perkuliahan (bundel matakuliah) terkait, bila ada. */
	private PaketPerkuliahan paketPerkuliahan;
	/** Tahun akademik dalam format "AAAA/AAAA+1", diresolusi otomatis dari data mahasiswa dan semester (lihat {@link #getTahunAkademik()}). */
	private String tahunAkademik;

	/** Rincian nilai (kolom {@code text}, bebas format) untuk matakuliah ini; default string kosong. */
	private String detailNilai = "";
	/** Rincian nilai tambahan (kolom {@code text}, bebas format); default string kosong. */
	private String detailNilaiTambahan = "";

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public DetailperkuliahanTemp() {
	}

	/** @return ID baris (primary key, auto-increment). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return mahasiswa pemilik detail perkuliahan ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		return this.mahasiswa;
	}

	/** @param mahasiswa mahasiswa yang akan diset. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return matakuliah/kelas perkuliahan yang diambil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		return this.perkuliahan;
	}

	/** @param perkuliahan perkuliahan yang akan diset. */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/** @param semester semester yang akan diset. */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/** @return semester saat matakuliah ini diambil. */
	@Column(name = "semester", length = 5)
	public Integer getSemester() {
		return semester;
	}

	/** @param totalNilai total nilai numerik yang akan diset. */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/** @return total nilai numerik matakuliah ini; default {@code 0.0} bila belum diisi. */
	@Column(name = "total_nilai", nullable = true, precision = 15)
	public Double getTotalNilai() {
		if (totalNilai == null) {
			totalNilai = 0.0;
		}
		return totalNilai;
	}

	/** @param nilaiHuruf nilai huruf yang akan diset. */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/** @return nilai huruf (mis. "A", "B+"). */
	@Column(name = "nilai_huruf", nullable = true, length = 2)
	public String getNilaiHuruf() {
		return nilaiHuruf;
	}

	/** @param totalIP total IP yang akan diset. */
	public void setTotalIP(Double totalIP) {
		this.totalIP = totalIP;
	}

	/** @return total IP (indeks prestasi) yang disumbang matakuliah ini. */
	@Column(name = "nilai_ip", nullable = true, precision = 15)
	public Double getTotalIP() {
		return totalIP;
	}

	/** @param persetujuan status persetujuan yang akan diset (lihat {@link #DISETUJUI}/{@link #BELUM_DISETUJUI}). */
	public void setPersetujuan(Integer persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * @return status persetujuan; otomatis menjadi {@link #DISETUJUI} bila
	 *         {@link #getMatakuliahKonversi()} terisi (konversi matakuliah dianggap otomatis
	 *         disetujui), terlepas dari nilai kolom yang tersimpan.
	 */
	@Column(name = "persetujuan", nullable = false, length = 1)
	public Integer getPersetujuan() {
		if (getMatakuliahKonversi() != null) {
			persetujuan = DISETUJUI;
		}
		return persetujuan;
	}

	/** @param matakuliahKonversi matakuliah hasil konversi yang akan diset. */
	public void setMatakuliahKonversi(Matakuliah matakuliahKonversi) {
		this.matakuliahKonversi = matakuliahKonversi;
	}

	/** @return matakuliah hasil konversi, bila nilai ini adalah hasil konversi dari matakuliah lain. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matakuliah_konversi", nullable = true)
	public Matakuliah getMatakuliahKonversi() {
		return matakuliahKonversi;
	}

	/**
	 * Membandingkan berdasarkan {@link #totalNilai} secara MENURUN (nilai lebih tinggi
	 * diurutkan lebih dulu) — dipakai untuk pengurutan daftar nilai tertinggi ke terendah.
	 *
	 * @return hasil perbandingan {@code totalNilai} terbalik, atau {@code 0} bila salah satu nilai
	 *         null atau {@code arg0} bukan instance {@link DetailperkuliahanTemp}.
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof DetailperkuliahanTemp) {
			DetailperkuliahanTemp o = (DetailperkuliahanTemp) arg0;
			if (totalNilai == null || o.totalNilai == null) {
				return 0;
			} else {
				return o.totalNilai.compareTo(totalNilai);
			}
		}
		return 0;
	}

	/** @param matakuliahAsliSebelumKonversi matakuliah asli sebelum konversi yang akan diset. */
	public void setMatakuliahAsliSebelumKonversi(Matakuliah matakuliahAsliSebelumKonversi) {
		this.matakuliahAsliSebelumKonversi = matakuliahAsliSebelumKonversi;
	}

	/** @return matakuliah asli sebelum dikonversi ke {@link #matakuliahKonversi}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matakuliah_asli_sebelum_konversi", nullable = true)
	public Matakuliah getMatakuliahAsliSebelumKonversi() {
		return matakuliahAsliSebelumKonversi;
	}

	/** @return perkuliahan alternatif yang diikuti (mis. kelas pengganti), bila berbeda dari {@link #perkuliahan}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "ikuti_perkuliahan", nullable = true)
	public Perkuliahan getIkutiPerkuliahan() {
		return ikutiPerkuliahan;
	}

	/** @param ikutiPerkuliahan perkuliahan alternatif yang akan diset. */
	public void setIkutiPerkuliahan(Perkuliahan ikutiPerkuliahan) {
		this.ikutiPerkuliahan = ikutiPerkuliahan;
	}

	/** @return paket perkuliahan (bundel matakuliah) terkait, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "paket_perkuliahan", nullable = true)
	public PaketPerkuliahan getPaketPerkuliahan() {
		return paketPerkuliahan;
	}

	/** @param paketPerkuliahan paket perkuliahan yang akan diset. */
	public void setPaketPerkuliahan(PaketPerkuliahan paketPerkuliahan) {
		this.paketPerkuliahan = paketPerkuliahan;
	}

	/**
	 * Meresolusi tahun akademik dari data mahasiswa dan semester bila keduanya tersedia dan
	 * semester bukan 0 (mengembalikan cache/hasil tersimpan bila syarat tidak terpenuhi).
	 *
	 * @return tahun akademik dalam format "AAAA/AAAA+1", atau nilai tersimpan sebelumnya bila
	 *         data mahasiswa/semester belum lengkap.
	 */
	public String getTahunAkademik() {
		if (mahasiswa != null && mahasiswa.getTahunangkatan() != null && semester != null && !semester.equals(0)) {
			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs,
					mahasiswa.getSemesterMulai());
			tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		}
		return tahunAkademik;
	}

	/** @param tahunAkademik tahun akademik yang akan diset. */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/** @return rincian nilai (bebas format); string kosong bila belum diisi. */
	@Column(name = "detail_nilai_baru_lagi", columnDefinition = "text")
	public String getDetailNilai() {
		return detailNilai;
	}

	/** @param detailNilai rincian nilai yang akan diset. */
	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	/** @return rincian nilai tambahan (bebas format); string kosong bila belum diisi. */
	@Column(name = "detail_nilai_tambahan_baru_lagi", columnDefinition = "text")
	public String getDetailNilaiTambahan() {
		return detailNilaiTambahan;
	}

	/** @param detailNilaiTambahan rincian nilai tambahan yang akan diset. */
	public void setDetailNilaiTambahan(String detailNilaiTambahan) {
		this.detailNilaiTambahan = detailNilaiTambahan;
	}

}
