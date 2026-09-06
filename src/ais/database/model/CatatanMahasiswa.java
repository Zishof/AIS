package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * Model data untuk satu CATATAN mahasiswa (mis. teguran, prestasi, pelanggaran, kejadian
 * akademik) yang dilekatkan pada baris {@link Mahasiswa}, diklasifikasi lewat
 * {@link JenisCatatanMahasiswa}, opsional dikaitkan dengan {@link Dosen} pencatat, dan boleh
 * membawa isian dinamis lewat mekanisme parameter tambahan (lihat catatan keamanan di bawah).
 * Tipe ini membawa state yang dipertukarkan oleh lapisan persistence, service, dan UI; makna
 * bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Mahasiswa mahasiswa}, {@code Dosen dosen}, {@code
 * JenisCatatanMahasiswa jenisCatatanMahasiswa}, {@code String parameterTambahan}, {@code String
 * parameterTambahanInds}; pemetaan persistence: tabel {@code public.catatan_mahasiswa}; pembacaan/pencarian
 * ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getKode()},
 * {@code getMahasiswa()}, {@code ambilDataParameterTambahan()}); mutasi data ({@code setOlehId()}, {@code
 * setId()}, {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code
 * populateParameterTambahan(List)}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Catatan getter yang menulis field ({@code getKode()}, {@code getNama()}, {@code getSemester()}, {@code
 * getTahunAjaran()}):</b> masing-masing menyalin dari {@link #getMahasiswa()} atau kalender berjalan dan
 * MENIMPA field-nya sendiri sebagai efek samping -- pola berulang di puluhan entity AIS
 * ({@code ais-getter-mutasi-field-anti-pattern-sistemik}), bukan cacat unik kelas ini.</p>
 * <p><b>Keamanan mekanisme parameter tambahan -- SUDAH AMAN (verified 6 Sep 2026).</b>
 * {@link #populateParameterTambahan(List)} memanggil {@link ais.database.model.file.LampiranLain
 * #resolveJenisParameterTambahan(Class, Long, String)} dengan {@code CatatanMahasiswa.class} sebagai
 * diskriminator kelas pemilik sebelum menyerahkan {@code jenis} ke penyimpanan lampiran -- yaitu pola
 * PENAMBALAN dari {@code task_484d4bd0} (tabrakan namespace lampiran lintas entity pemakai
 * {@link ais.database.model.ParameterTambahanAstract}), bukan pola lama yang rentan. Ditambal bersamaan
 * dengan {@link CatatanPegawai} dan {@link CatatanAdministrasi} pada revisi yang sama (termasuk dalam 9
 * entity yang dikonfirmasi terpengaruh {@code task_484d4bd0}).</p>
 * <p><b>Efek samping:</b> selain accessor state, {@code populateParameterTambahan} dan
 * {@code ambilDataParameterTambahan} membaca komponen ZK ({@code Row}/{@code Textbox}) dan bisa memicu
 * pembacaan lampiran lewat {@code LampiranLain}; jangan menganggap model ini selalu murni. Persistence,
 * transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan session
 * aktif.</p>
 *
 * @see GeneralValueObject
 * @see CatatanPegawai versi catatan untuk pegawai (pola sama)
 * @see CatatanAdministrasi versi catatan administrasi umum (pola sama, extends {@link ais.database.model.sop.DataSop})
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "catatan_mahasiswa")
public class CatatanMahasiswa extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
	private String olehId;

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: {@code "<id>-<nama>"}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas catatan; lihat {@link #getKode()} untuk perilaku default. */
	private String kode;
	/** Mahasiswa yang menjadi subjek catatan ini. */
	private Mahasiswa mahasiswa;
	/** Dosen pencatat/pelapor (opsional). */
	private Dosen dosen;
	/** Nama tampilan baris catatan; lihat {@link #getNama()} untuk perilaku default. */
	private String nama;
	/** Isi/uraian catatan (teks bebas). */
	private String keterangan;
	/** Waktu kejadian/pencatatan; lihat {@link #getWaktu()} untuk perilaku default. */
	private Date waktu;
	/** Jenis/klasifikasi catatan mahasiswa. */
	private JenisCatatanMahasiswa jenisCatatanMahasiswa;
	/** Isian parameter tambahan dinamis, format baris {@code "label<=>nilai<=>url<=>urut<=>id"}. */
	private String parameterTambahan;
	/** Versi kunci-id (bukan label) dari {@link #parameterTambahan}, dipakai pencocokan lampiran. */
	private String parameterTambahanInds;
	/** Tahun ajaran catatan; lihat {@link #getTahunAjaran()} untuk perilaku default. */
	private String tahunAjaran;
	/** Semester catatan (1=ganjil, 2=genap); lihat {@link #getSemester()} untuk perilaku default. */
	private Integer semester;

	/** Konstruktor kosong, dipakai Hibernate. */
	public CatatanMahasiswa() {
	}

	/** @return primary key entity, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode ringkas. Bila {@link #getMahasiswa()} tidak {@code null}, field ini MENIMPA
	 *         dirinya sendiri dengan {@code mahasiswa.getNim()} sebelum dikembalikan; string kosong
	 *         (bukan {@code null}) bila keduanya kosong.
	 */
	public String getKode() {
		if (getMahasiswa() != null) {
			kode = getMahasiswa().getNim();
		}
		return kode == null ? "" : kode.trim();
	}

	/** @param kode kode ringkas baru; akan tertimpa lagi bila {@link #getMahasiswa()} terisi. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama tampilan. Bila {@link #getMahasiswa()} tidak {@code null}, field ini MENIMPA
	 *         dirinya sendiri dengan {@code mahasiswa.getNama()} sebelum dikembalikan.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getMahasiswa() != null) {
			nama = getMahasiswa().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tampilan baru; akan tertimpa lagi bila {@link #getMahasiswa()} terisi. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return isi/uraian catatan, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan isi/uraian catatan yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return semester catatan (1=ganjil, 2=genap); bila belum pernah diisi, DITENTUKAN SEKALI dari
	 *         {@link Common#isNowSemensterGanjil()} lalu disimpan ke field.
	 */
	@Column(name = "semester", nullable = true)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/** @param semester semester catatan yang baru. */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * @return tahun ajaran; bila belum pernah diisi, DITENTUKAN SEKALI dari
	 *         {@link Common#getCurrentTahunAkademik()} lalu disimpan ke field.
	 */
	@Column(name = "tahun_ajaran", nullable = true, length = 9)
	public String getTahunAjaran() {

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	/** @param tahunAjaran tahun ajaran baru. */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/** @return mahasiswa subjek catatan ini; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param mahasiswa mahasiswa subjek catatan yang baru. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return waktu kejadian/pencatatan; waktu saat ini bila belum pernah diisi (bukan {@code null}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/** @param waktu waktu kejadian/pencatatan yang baru. */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/** @return jenis/klasifikasi catatan, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_catatan_mahasiswa")
	public JenisCatatanMahasiswa getJenisCatatanMahasiswa() {
		jenisCatatanMahasiswa = check(jenisCatatanMahasiswa);
		return jenisCatatanMahasiswa;
	}

	/** @param jenisCatatanMahasiswa jenis/klasifikasi catatan yang baru. */
	public void setJenisCatatanMahasiswa(JenisCatatanMahasiswa jenisCatatanMahasiswa) {
		this.jenisCatatanMahasiswa = jenisCatatanMahasiswa;
	}

	/** @return dosen pencatat/pelapor, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen")
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/** @param dosen dosen pencatat/pelapor yang baru. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}



	/** @return versi kunci-id dari {@link #parameterTambahan}, string kosong (bukan {@code null}) bila belum diisi. */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/** @param parameterTambahanInds versi kunci-id parameter tambahan yang baru. */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #parameterTambahan} (satu baris teks per parameter, dipisah {@code "\n"} dan
	 * {@code "<=>"}) menjadi daftar {@link CommonVO} siap-tampil, terurut menurut
	 * {@code nomorUrut}. Baris yang gagal diparse dicatat lewat {@code ErrorAuditUtil} dan diberi
	 * nilai cadangan, bukan melempar exception ke pemanggil.
	 *
	 * @return daftar parameter tambahan siap tampil; kosong bila {@link #parameterTambahan} kosong.
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CatatanMahasiswa.java:237");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CatatanMahasiswa.java:243");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Membaca kembali isian parameter tambahan dari baris-baris form ZK (hasil
	 * {@link ais.database.model.ParameterTambahanAstract#initComponent}) dan merangkainya menjadi
	 * {@link #parameterTambahan}/{@link #parameterTambahanInds}. Untuk parameter yang mewajibkan
	 * lampiran, {@code jenis} dinormalkan lewat {@link ais.database.model.file.LampiranLain
	 * #resolveJenisParameterTambahan(Class, Long, String)} dengan {@code CatatanMahasiswa.class}
	 * sebagai diskriminator -- SUDAH AMAN terhadap tabrakan namespace {@code task_484d4bd0}
	 * (lihat javadoc class). Kegagalan per-baris ditangkap dan ditampilkan lewat
	 * {@code Common.tampilErrorJikaAdmin} tanpa menghentikan baris lain.
	 *
	 * @param parameterRows baris {@code Row} form ZK; tidak melakukan apa pun bila {@code null}/kosong.
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahasiswa = (KelompokParameterTambahanCatatanMahasiswa) row
						.getAttribute("kelompokParameterTambahanCatatanMahasiswa");
				if (parameterTambahan != null && kelompokParameterTambahanCatatanMahasiswa != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanMahasiswa.class, getId(),
							kelompokParameterTambahanCatatanMahasiswa.getId() + "->" + parameterTambahan.getId());

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);

					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);

					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanCatatanMahasiswa.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCatatanMahasiswa.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCatatanMahasiswa.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/** @return isian parameter tambahan (baris {@code label<=>nilai<=>...}), string kosong bila belum diisi. */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/** @param parameterTambahan isian parameter tambahan yang baru. */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}


}
