package ais.database.model.employ;

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
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Model data untuk kenaikan pangkat. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataSop}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code Date tanggal_dirubah}, {@code Pegawai pegawai}, {@code
 * JenisKenaikanPangkat jenisKenaikanPangkat}, {@code String noSuratUsul}; pemetaan persistence: tabel {@code
 * employ.kenaikan_pangkat}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code
 * getTanggal_dirubah()}, {@code getKeterangan()}, {@code getPegawai()}); mutasi data ({@code setOlehId()},
 * {@code onUpdate()}, {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code
 * setKeterangan()}); operasi domain lain ({@code compareTo()}, {@code toString()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor, mutator, dan pembanding hanya membaca atau mengubah state entity di memori.
 * Persistence, transaksi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan session
 * aktif.</p>
 *
 * @see DataSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "kenaikan_pangkat")
public class KenaikanPangkat extends DataSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String keterangan;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getMulai() != null && ((KenaikanPangkat) arg0).getMulai() != null) {
				return getMulai().compareTo(((KenaikanPangkat) arg0).getMulai());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/KenaikanPangkat.java:74");

		}

		return 0;
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

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return keterangan;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	private Pegawai pegawai;
	private JenisKenaikanPangkat jenisKenaikanPangkat;
	private String noSuratUsul;
	private Date tanggalSuratUsul;
	private Golongan golongan;
	private GajiPokok gajiPokok;
	private Insentif insentif;
	private Makan makan;
	private Transport transport;
	private String namaPejabat;
	private String nomorSuratkeputusan;
	private Integer kenaikanBerkalaBulan;
	private Date tanggalSuratkeputusan;
	private Date mulai;
	private Date sampai;
	private Peraturan peraturan;
	private Date tmt;
	private DisposisiSop disposisiSop;
	private String jsonDataPengguna;
	private Boolean kenaikanJabatan = false;
	private String jenis;
	private JabatanFungsional jabatanFungsional;
	private JabatanStruktural jabatanStruktural;
	private Jabatan jabatan;
	private Boolean menjabat = false;
	private Boolean status = false;
	private Boolean nonAktifkanJabatanSebelumnya = false;
	private Boolean terdapatKenaikanGajiBerkala;
	private Boolean gajiLangsungDitentukanDisini;
	private Boolean gajiPokokOtomatisMasaKerja;
	private Boolean nonAktifkanPengguna = false;
	private Boolean aktifkanPengguna = false;
	private Boolean kenaikanPangkatGolongan;
	private Boolean kenaikanPangkatFungsional;

	private Double nilaiGaji = 0.0;
	private Double nilaiInsentif = 0.0;

	public static final String UBAH_JABATAN_DAN_GOLONGAN = "Jabatan dan Golongan";
	public static final String UBAH_JABATAN = "Jabatan";
	public static final String UBAH_GOLONGAN = "Golongan";
	public static final String UBAH_PENGUNDURAN_DIRI = "Pengunduran Diri";

	private String jenisPerubahan;

	// private String status;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/KenaikanPangkat.java:178");

		}

		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@Column(name = "no_surat_usul")
	public String getNoSuratUsul() {
		return noSuratUsul;
	}

	public void setNoSuratUsul(String noSuratUsul) {
		this.noSuratUsul = noSuratUsul;
	}

	@Column(name = "tanggal_surat_usul")
	public Date getTanggalSuratUsul() {
		return tanggalSuratUsul;
	}

	public void setTanggalSuratUsul(Date tanggalSuratUsul) {
		this.tanggalSuratUsul = tanggalSuratUsul;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "golongan", nullable = true)
	public Golongan getGolongan() {
		golongan = check(golongan);
		if (getGajiPokok() != null) {
			golongan = getGajiPokok().getGolongan();
		}

		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			golongan = null;
		}

		return golongan;
	}

	public void setGolongan(Golongan golongan) {
		this.golongan = golongan;
	}

	public String getNamaPejabat() {
		return namaPejabat;
	}

	public void setNamaPejabat(String namaPejabat) {
		this.namaPejabat = namaPejabat;
	}

	public String getNomorSuratkeputusan() {
		return nomorSuratkeputusan;
	}

	public void setNomorSuratkeputusan(String nomorSuratkeputusan) {
		this.nomorSuratkeputusan = nomorSuratkeputusan;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalSuratkeputusan() {
		return tanggalSuratkeputusan;
	}

	public void setTanggalSuratkeputusan(Date tanggalSuratkeputusan) {
		this.tanggalSuratkeputusan = tanggalSuratkeputusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peraturan", nullable = true)
	public Peraturan getPeraturan() {
		peraturan = check(peraturan);
		return peraturan;
	}

	public void setPeraturan(Peraturan peraturan) {
		this.peraturan = peraturan;
	}

	public Boolean getKenaikanJabatan() {
		kenaikanJabatan = getJabatan() != null;

		if (getJabatanFungsional() != null) {
			kenaikanJabatan = true;
		} else if (getJabatanStruktural() != null) {
			kenaikanJabatan = true;
		}

		return kenaikanJabatan;
	}

	public void setKenaikanJabatan(Boolean kenaikanJabatan) {
		this.kenaikanJabatan = kenaikanJabatan;
	}

	public String getJenis() {

		if (getJabatanFungsional() != null) {
			jenis = Pegawai.JENIS_FUNGSIONAL;
		} else if (getJabatanStruktural() != null) {
			jenis = Pegawai.JENIS_STRUKTURAL;
		}

		if (jenis == null) {
			jenis = Pegawai.JENIS_STRUKTURAL;
		}
		return jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_fungsional", nullable = true)
	public JabatanFungsional getJabatanFungsional() {
		jabatanFungsional = check(jabatanFungsional);
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			jabatanFungsional = null;
		}
		return jabatanFungsional;
	}

	public void setJabatanFungsional(JabatanFungsional jabatanFungsional) {
		this.jabatanFungsional = jabatanFungsional;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_struktural", nullable = true)
	public JabatanStruktural getJabatanStruktural() {
		jabatanStruktural = check(jabatanStruktural);

		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			jabatanStruktural = null;
		}
		return jabatanStruktural;
	}

	public void setJabatanStruktural(JabatanStruktural jabatanStruktural) {
		this.jabatanStruktural = jabatanStruktural;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan", nullable = true)
	public Jabatan getJabatan() {
		jabatan = check(jabatan);
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			jabatan = null;
		}
		return jabatan;
	}

	public void setJabatan(Jabatan jabatan) {
		this.jabatan = jabatan;
	}

	public Boolean getMenjabat() {
		if (menjabat == null) {
			menjabat = true;
		}

		if (getSampai() != null
				&& !Common.dateFormat83.get().format(getSampai()).equals(Common.dateFormat83.get().format(WaktuUtil.getDate()))
				&& getSampai().before(WaktuUtil.getDate())) {
			menjabat = false;
		} else if (getMulai() != null
				&& !Common.dateFormat83.get().format(getMulai()).equals(Common.dateFormat83.get().format(WaktuUtil.getDate()))
				&& getMulai().after(WaktuUtil.getDate())) {
			menjabat = false;
		} else if (getMulai() != null) {
			menjabat = true;
		}

		return menjabat;
	}

	public void setMenjabat(Boolean menjabat) {
		this.menjabat = menjabat;
	}

	public Boolean getStatus() {
		if (status == null) {
			status = false;
		}

		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			status = false;
		} else if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = false;
		} else if (disposisiSop != null) {
			status = disposisiSop.getDisposisiSetuju() != null && disposisiSop.getDisposisiSetuju().getId() != null;
		}

		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? WaktuUtil.getDate() : mulai;
	}

	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	@Column(name = "kenaikan_pangkat_golongan")
	public Boolean getKenaikanPangkatGolongan() {
		kenaikanPangkatGolongan = getJabatanStruktural() != null;
		return kenaikanPangkatGolongan;
	}

	public void setKenaikanPangkatGolongan(Boolean kenaikanPangkatGolongan) {
		this.kenaikanPangkatGolongan = kenaikanPangkatGolongan;
	}

	@Column(name = "kenaikan_pangkat_fungsional")
	public Boolean getKenaikanPangkatFungsional() {
		kenaikanPangkatFungsional = getJabatanFungsional() != null;
		return kenaikanPangkatFungsional;
	}

	public void setKenaikanPangkatFungsional(Boolean kenaikanPangkatFungsional) {
		this.kenaikanPangkatFungsional = kenaikanPangkatFungsional;
	}

	@Column(name = "tmt")
	public Date getTmt() {
		return tmt;
	}

	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kenaikan_pangkat", nullable = true)
	public JenisKenaikanPangkat getJenisKenaikanPangkat() {
		jenisKenaikanPangkat = check(jenisKenaikanPangkat);
		return jenisKenaikanPangkat;
	}

	public void setJenisKenaikanPangkat(JenisKenaikanPangkat jenisKenaikanPangkat) {
		this.jenisKenaikanPangkat = jenisKenaikanPangkat;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gaji_pokok", nullable = true)
	public GajiPokok getGajiPokok() {
		gajiPokok = check(gajiPokok);

		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI) || getGajiLangsungDitentukanDisini()) {
			gajiPokok = null;
		}

		return gajiPokok;
	}

	public void setGajiPokok(GajiPokok gajiPokok) {
		this.gajiPokok = gajiPokok;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "insentif", nullable = true)
	public Insentif getInsentif() {
		insentif = check(insentif);
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI) || getGajiLangsungDitentukanDisini()) {
			insentif = null;
		}
		return insentif;
	}

	public void setInsentif(Insentif insentif) {
		this.insentif = insentif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "makan", nullable = true)
	public Makan getMakan() {
		makan = check(makan);
		return makan;
	}

	public void setMakan(Makan makan) {
		this.makan = makan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transport", nullable = true)
	public Transport getTransport() {
		transport = check(transport);
		return transport;
	}

	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	public Boolean getNonAktifkanJabatanSebelumnya() {
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			nonAktifkanJabatanSebelumnya = true;
		}
		return nonAktifkanJabatanSebelumnya == null ? false : nonAktifkanJabatanSebelumnya;
	}

	public void setNonAktifkanJabatanSebelumnya(Boolean nonAktifkanJabatanSebelumnya) {
		this.nonAktifkanJabatanSebelumnya = nonAktifkanJabatanSebelumnya;
	}

	public String getJenisPerubahan() {
		return jenisPerubahan == null || jenisPerubahan.trim().isEmpty()
				|| jenisPerubahan.equalsIgnoreCase("Ubah Jabatan dan Golangan") ? UBAH_JABATAN_DAN_GOLONGAN
						: jenisPerubahan;
	}

	public void setJenisPerubahan(String jenisPerubahan) {
		this.jenisPerubahan = jenisPerubahan;
	}

	@Column(columnDefinition = "text")
	public String getJsonDataPengguna() {
		return jsonDataPengguna == null || jsonDataPengguna.trim().isEmpty() ? new JSONObject().toString()
				: jsonDataPengguna;
	}

	public void setJsonDataPengguna(String jsonDataPengguna) {
		this.jsonDataPengguna = jsonDataPengguna;
	}

	public Boolean getNonAktifkanPengguna() {
		if (getJenisPerubahan().equalsIgnoreCase(UBAH_PENGUNDURAN_DIRI)) {
			nonAktifkanPengguna = true;
		}
		return nonAktifkanPengguna == null ? false : nonAktifkanPengguna;
	}

	public void setNonAktifkanPengguna(Boolean nonAktifkanPengguna) {
		this.nonAktifkanPengguna = nonAktifkanPengguna;
	}

	public Boolean getAktifkanPengguna() {
		return aktifkanPengguna == null ? false : aktifkanPengguna;
	}

	public void setAktifkanPengguna(Boolean aktifkanPengguna) {
		this.aktifkanPengguna = aktifkanPengguna;
	}

	public Boolean getTerdapatKenaikanGajiBerkala() {
		return terdapatKenaikanGajiBerkala == null ? false : terdapatKenaikanGajiBerkala;
	}

	public void setTerdapatKenaikanGajiBerkala(Boolean terdapatKenaikanGajiBerkala) {
		this.terdapatKenaikanGajiBerkala = terdapatKenaikanGajiBerkala;
	}

	public Integer getKenaikanBerkalaBulan() {
		if (!getTerdapatKenaikanGajiBerkala() || getGajiLangsungDitentukanDisini()) {
			kenaikanBerkalaBulan = null;
		}
		return kenaikanBerkalaBulan;
	}

	public void setKenaikanBerkalaBulan(Integer kenaikanBerkalaBulan) {
		this.kenaikanBerkalaBulan = kenaikanBerkalaBulan;
	}

	public Boolean getGajiLangsungDitentukanDisini() {
		return gajiLangsungDitentukanDisini == null ? false : gajiLangsungDitentukanDisini;
	}

	public void setGajiLangsungDitentukanDisini(Boolean gajiLangsungDitentukanDisini) {
		this.gajiLangsungDitentukanDisini = gajiLangsungDitentukanDisini;
	}

	/**
	 * Penanda "Penggajian Otomatis Berdasarkan Masa Kerja". Bila {@code true}, Gaji Pokok dihitung
	 * otomatis dari golongan/penggajian-berdasarkan + masa kerja pegawai (tabel master Gaji Pokok),
	 * bukan dipilih manual. Default {@code false} (mengikuti proses penggajian yang berjalan saat ini).
	 *
	 * <p>{@code @NotAudited}: kolom ditambahkan otomatis ke tabel utama oleh hbm2ddl=update, dan
	 * sengaja tidak diaudit Envers agar tidak menuntut kolom baru pada tabel audit ({@code _AUD}).</p>
	 */
	@org.hibernate.envers.NotAudited
	public Boolean getGajiPokokOtomatisMasaKerja() {
		return gajiPokokOtomatisMasaKerja == null ? false : gajiPokokOtomatisMasaKerja;
	}

	public void setGajiPokokOtomatisMasaKerja(Boolean gajiPokokOtomatisMasaKerja) {
		this.gajiPokokOtomatisMasaKerja = gajiPokokOtomatisMasaKerja;
	}

	public Double getNilaiGaji() {
		return nilaiGaji == null ? 0.0 : nilaiGaji;
	}

	public void setNilaiGaji(Double nilaiGaji) {
		this.nilaiGaji = nilaiGaji;
	}

	public Double getNilaiInsentif() {
		return nilaiInsentif == null ? 0.0 : nilaiInsentif;
	}

	public void setNilaiInsentif(Double nilaiInsentif) {
		this.nilaiInsentif = nilaiInsentif;
	}

}
