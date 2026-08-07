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
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.ui.util.WaktuUtil;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tugas_pertemuan")
public class TugasPertemuan extends Tugas {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8996611659323620994L;

	private Long id;
	private String oleh;
	private String olehId;

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

	private String isitugas = "";
	private String judultugas = "";

	private Date mulai;
	private Date selesai;

	private FormatNilai formatNilai;
	private String formatNilais;
	private Double prosentase;

	private SyaratUjian syaratMengumpulkanTugas;

	private Long pertemuan;
	private Pertemuan pertemuanData;

	private String mhsYgTidakIkut;

	private String mhsBolehUploadUlang;

	private String nilaiManualJson;

	private String subCpmkPerPeserta;

	private String syaratAkses;

	private JenisItemPenilaianSiswa jenisItemPenilaianSiswa;

	private GrupPenilaian grupPenilaian;

	private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;

	private String keteranganNilai;

	private String keteranganNilaiLama;

	private Boolean aktif;

	public String toString() {
		return id + "-" + judultugas;
	}

	public TugasPertemuan() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "syarat_mengumpulkan_tugas", nullable = true)
	public SyaratUjian getSyaratMengumpulkanTugas() {
		syaratMengumpulkanTugas = check(syaratMengumpulkanTugas);
		return syaratMengumpulkanTugas;
	}

	public void setSyaratMengumpulkanTugas(SyaratUjian syaratMengumpulkanTugas) {
		this.syaratMengumpulkanTugas = syaratMengumpulkanTugas;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_nilai", nullable = true)
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	public Double getProsentase() {
		return prosentase == null ? 100.0 : prosentase;
	}

	public void setProsentase(Double prosentase) {
		this.prosentase = prosentase;
	}

	public String getJudultugas() {
		return judultugas == null ? "" : judultugas.trim();
	}

	public void setJudultugas(String judultugas) {
		this.judultugas = judultugas;
	}

	public void setIsitugas(String isitugas) {
		this.isitugas = isitugas;
	}

	@Column(name = "isitugas", columnDefinition = "text")
	public String getIsitugas() {
		return isitugas;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		return mulai == null ? WaktuUtil.getDate() : mulai;
	}

	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getSelesai() {
		return selesai;
	}

	public void setSelesai(Date selesai) {
		this.selesai = selesai;
	}

//	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
//	@Fetch(FetchMode.SELECT)
//	@JoinColumn(name = "pertemuan", nullable = false)
//	public Pertemuan getPertemuan() {
//		return pertemuan;
//	}
//
//	public void setPertemuan(Pertemuan pertemuan) {
//		this.pertemuan = pertemuan;
//	}

	public Pertemuan ambilPertemuan() {
		return getPertemuan() == null ? null
				: (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, getPertemuan().toString());
	}

	@Column(name = "pertemuan", nullable = false)
	public Long getPertemuan() {
		return pertemuan;
	}

	public void setPertemuan(Long pertemuan) {
		this.pertemuan = pertemuan;
	}

	@Column(columnDefinition = "text")
	public String getMhsYgTidakIkut() {
		mhsYgTidakIkut = (mhsYgTidakIkut == null || mhsYgTidakIkut.trim().equalsIgnoreCase(",") ? ""
				: "," + mhsYgTidakIkut.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (mhsYgTidakIkut.equals(",")) {
			mhsYgTidakIkut = "";
		} else if (mhsYgTidakIkut.equals(",,")) {
			mhsYgTidakIkut = "";
		} else if (mhsYgTidakIkut.equals(",,,")) {
			mhsYgTidakIkut = "";
		}
		return mhsYgTidakIkut == null ? "" : mhsYgTidakIkut.trim();
	}

	public void setMhsYgTidakIkut(String mhsYgTidakIkut) {
		this.mhsYgTidakIkut = mhsYgTidakIkut;
	}

	@Column(columnDefinition = "text")
	public String getMhsBolehUploadUlang() {
		mhsBolehUploadUlang = (mhsBolehUploadUlang == null || mhsBolehUploadUlang.trim().equalsIgnoreCase(",") ? ""
				: "," + mhsBolehUploadUlang.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (mhsBolehUploadUlang.equals(",")) {
			mhsBolehUploadUlang = "";
		} else if (mhsBolehUploadUlang.equals(",,")) {
			mhsBolehUploadUlang = "";
		} else if (mhsBolehUploadUlang.equals(",,,")) {
			mhsBolehUploadUlang = "";
		}
		return mhsBolehUploadUlang == null ? "" : mhsBolehUploadUlang.trim();
	}

	public void setMhsBolehUploadUlang(String mhsBolehUploadUlang) {
		this.mhsBolehUploadUlang = mhsBolehUploadUlang;
	}

	@Column(columnDefinition = "text", name = "nilai_manual_json")
	public String getNilaiManualJson() {
		return nilaiManualJson == null || nilaiManualJson.trim().isEmpty() ? new org.json.JSONObject().toString()
				: nilaiManualJson;
	}

	public void setNilaiManualJson(String nilaiManualJson) {
		this.nilaiManualJson = nilaiManualJson;
	}

	@Column(columnDefinition = "text", name = "sub_cpmk_per_peserta")
	public String getSubCpmkPerPeserta() {
		return subCpmkPerPeserta == null || subCpmkPerPeserta.trim().isEmpty()
				? new org.json.JSONObject().toString()
				: subCpmkPerPeserta;
	}

	public void setSubCpmkPerPeserta(String subCpmkPerPeserta) {
		this.subCpmkPerPeserta = subCpmkPerPeserta;
	}

	/** Ambil set FormatNilai-ID Sub-CPMK yang terpilih untuk mahasiswa tertentu.
	 *  Mengembalikan null jika semua Sub-CPMK terpilih (default). */
	@javax.persistence.Transient
	public java.util.Set<Long> ambilSubCpmkPeserta(Long mahasiswaId) {
		if (mahasiswaId == null) {
			return null;
		}
		try {
			org.json.JSONObject j = new org.json.JSONObject(getSubCpmkPerPeserta());
			String key = mahasiswaId.toString();
			if (j.isNull(key)) {
				return null;
			}
			org.json.JSONArray arr = j.optJSONArray(key);
			if (arr == null) {
				return null;
			}
			java.util.Set<Long> hasil = new java.util.LinkedHashSet<Long>();
			for (int i = 0; i < arr.length(); i++) {
				try {
					hasil.add(Long.parseLong(arr.getString(i)));
				} catch (Exception ex) { /* skip entri tidak valid */ }
			}
			return hasil;
		} catch (Exception e) {
			return null;
		}
	}

	@Column(columnDefinition = "text")
	public String getSyaratAkses() {
		return syaratAkses == null || syaratAkses.trim().isEmpty() ? new JSONObject().toString() : syaratAkses;
	}

	public void setSyaratAkses(String syaratAkses) {
		this.syaratAkses = syaratAkses;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_item_penilaian_siswa", nullable = true)
	public JenisItemPenilaianSiswa getJenisItemPenilaianSiswa() {
		jenisItemPenilaianSiswa = check(jenisItemPenilaianSiswa);
		return jenisItemPenilaianSiswa;
	}

	public void setJenisItemPenilaianSiswa(JenisItemPenilaianSiswa jenisItemPenilaianSiswa) {
		this.jenisItemPenilaianSiswa = jenisItemPenilaianSiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_kategori_item_penilaian_siswa", nullable = true)
	public GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa() {
		grupKategoriItemPenilaianSiswa = check(grupKategoriItemPenilaianSiswa);
		return grupKategoriItemPenilaianSiswa;
	}

	public void setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) {
		this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_penilaian", nullable = true)
	public GrupPenilaian getGrupPenilaian() {
		grupPenilaian = check(grupPenilaian);
		return grupPenilaian;
	}

	public void setGrupPenilaian(GrupPenilaian grupPenilaian) {
		this.grupPenilaian = grupPenilaian;
	}

	public static String DEFAULT_FORMULA = new JSONObject().toString();

	@Column(columnDefinition = "text", name = "keterangannilai")
	public String getKeteranganNilaiLama() {
		return keteranganNilaiLama == null || keteranganNilaiLama.trim().isEmpty() ? DEFAULT_FORMULA
				: keteranganNilaiLama;
	}

	public void setKeteranganNilaiLama(String keteranganNilaiLama) {
		this.keteranganNilaiLama = keteranganNilaiLama;
	}

	@Override
	@Column(columnDefinition = "text", name = "keterangan_nilai_baru")
	public String getKeteranganNilai() {
		return keteranganNilai == null || keteranganNilai.trim().isEmpty() ? getKeteranganNilaiLama() : keteranganNilai;
	}

	@Override
	public void setKeteranganNilai(String keteranganNilai) {
		this.keteranganNilai = keteranganNilai;
	}

	@Column(columnDefinition = "text")
	public String getFormatNilais() {
		return formatNilais == null || formatNilais.trim().isEmpty() ? Tugas.JSON : formatNilais;
	}

	public void setFormatNilais(String formatNilais) {
		this.formatNilais = formatNilais;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "pertemuan", nullable = true, insertable = false, updatable = false)
	public Pertemuan getPertemuanData() {
		return pertemuanData;
	}

	public void setPertemuanData(Pertemuan pertemuanData) {
		this.pertemuanData = pertemuanData;
	}

	public Boolean getAktif() {
		aktif = !getJudultugas().isEmpty();
		return aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
