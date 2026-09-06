package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
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
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Model data untuk satu CATATAN administrasi umum (mis. surat masuk/keluar, kejadian
 * administratif lintas modul) yang diklasifikasi lewat {@link JenisCatatanAdministrasi},
 * opsional dikaitkan dengan {@link SatuanKerja} dan alur SOP lewat {@link DisposisiSop}, dan
 * boleh membawa isian dinamis lewat mekanisme parameter tambahan (lihat catatan keamanan di
 * bawah). Berbeda dari {@link CatatanPegawai} dan {@link CatatanMahasiswa}, kelas ini
 * {@code extends} {@link ais.database.model.sop.DataSop} (bukan langsung {@link
 * GeneralValueObject}), sehingga baris catatan administrasi bisa menjadi OBJEK yang mengalir
 * lewat mesin disposisi SOP. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki
 * {@link ais.database.model.sop.DataSop} (dan transitif {@link GeneralValueObject}). Kelas ini hanya boleh
 * memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga
 * harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code JenisCatatanAdministrasi jenisCatatanAdministrasi},
 * {@code SatuanKerja satuanKerja}, {@code DisposisiSop disposisiSop}, {@code Boolean broadcast}, {@code Boolean
 * aktif}; pemetaan persistence: tabel {@code public.catatan_administrasi}; pembacaan/pencarian ({@code
 * getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getKode()}, {@code
 * getDisposisiSop()}, {@code getAktif()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code
 * setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code populateParameterTambahan(List)}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Catatan getter yang menulis field ({@code getKode()} kebalikannya -- justru MENOLAK isi kosong,
 * {@code getNama()}, {@code getTahun()}, {@code getBulan()}, {@code getAktif()}):</b> beberapa mengisi
 * default dari kalender berjalan atau dari {@link #getJenisCatatanAdministrasi()}, dan {@link #getAktif()}
 * bahkan bisa MENGUNCI dirinya ke {@code false} secara permanen begitu {@link #getDisposisiSop()} menandakan
 * ditolak/tidak aktif -- pola berulang di puluhan entity AIS ({@code
 * ais-getter-mutasi-field-anti-pattern-sistemik}), bukan cacat unik kelas ini.</p>
 * <p><b>Setter {@link #setDisposisiSop(DisposisiSop)} bersifat WRITE-ONCE:</b> begitu {@code disposisiSop}
 * sudah terisi (bukan {@code null} dan punya id), pemanggilan setter berikutnya dengan argumen {@code null}
 * atau entity tanpa id DIABAIKAN diam-diam -- relasi disposisi yang sudah terpasang tidak bisa dilepas lewat
 * setter ini setelah terisi.</p>
 * <p><b>Keamanan mekanisme parameter tambahan -- SUDAH AMAN (verified 6 Sep 2026).</b>
 * {@link #populateParameterTambahan(List)} memanggil {@link ais.database.model.file.LampiranLain
 * #resolveJenisParameterTambahan(Class, Long, String)} dengan {@code CatatanAdministrasi.class} sebagai
 * diskriminator kelas pemilik sebelum menyerahkan {@code jenis} ke penyimpanan lampiran -- yaitu pola
 * PENAMBALAN dari {@code task_484d4bd0} (tabrakan namespace lampiran lintas entity pemakai
 * {@link ais.database.model.ParameterTambahanAstract}), bukan pola lama yang rentan. Ditambal bersamaan
 * dengan {@link CatatanPegawai} dan {@link CatatanMahasiswa} pada revisi yang sama (termasuk dalam 9 entity
 * yang dikonfirmasi terpengaruh {@code task_484d4bd0}). Perlu dicatat memo lain menyebutkan
 * {@code BroadcastHelper} pada jalur catatan administrasi masih memakai kunci mentah pada jalur TERPISAH
 * (BUKAN method ini) -- lihat javadoc {@link ais.database.model.ParameterTambahanAstract#initComponent}.</p>
 * <p><b>Efek samping:</b> selain accessor state, {@code populateParameterTambahan} dan
 * {@code ambilDataParameterTambahan} membaca komponen ZK ({@code Row}/{@code Textbox}) dan bisa memicu
 * pembacaan lampiran lewat {@code LampiranLain}; jangan menganggap model ini selalu murni. Persistence,
 * transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan session
 * aktif.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.sop.DataSop
 * @see CatatanPegawai versi catatan untuk pegawai (pola sama, extends {@link GeneralValueObject} langsung)
 * @see CatatanMahasiswa versi catatan untuk mahasiswa (pola sama, extends {@link GeneralValueObject} langsung)
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "catatan_administrasi")
public class CatatanAdministrasi extends DataSop {

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

	/** Nama tampilan baris catatan; lihat {@link #getNama()} untuk perilaku default. */
	private String nama;
	/** Isi/uraian catatan (teks bebas). */
	private String keterangan;
	/** Waktu kejadian/pencatatan; lihat {@link #getWaktu()} untuk perilaku default. */
	private Date waktu;
	/** Jenis/klasifikasi catatan administrasi. */
	private JenisCatatanAdministrasi jenisCatatanAdministrasi;
	/** Satuan kerja terkait catatan (opsional). */
	private SatuanKerja satuanKerja;
	/** Isian parameter tambahan dinamis, format baris {@code "label<=>nilai<=>url<=>urut<=>id"}. */
	private String parameterTambahan;
	/** Versi kunci-id (bukan label) dari {@link #parameterTambahan}, dipakai pencocokan lampiran. */
	private String parameterTambahanInds;
	/** Baris disposisi SOP tempat catatan ini mengalir; lihat {@link #setDisposisiSop(DisposisiSop)} soal write-once. */
	private DisposisiSop disposisiSop;
	/** Menandai catatan ini disiarkan (broadcast) ke banyak penerima; lihat {@link #getBroadcast()}. */
	private Boolean broadcast;
	/** Tahun pencatatan; lihat {@link #getTahun()} untuk perilaku default. */
	private Integer tahun;
	/** Bulan pencatatan; lihat {@link #getBulan()} untuk perilaku default. */
	private Integer bulan;
	/** Kode ringkas catatan; lihat {@link #getKode()} untuk perilaku default. */
	private String kode;
	/** Nomor urut tampilan tambahan (di luar {@code nomorUrut} milik {@link GeneralValueObject}). */
	private Long index;
	/** Menandai catatan masih aktif/berlaku; lihat {@link #getAktif()} untuk perilaku turunan dari disposisi. */
	private Boolean aktif;

	/** Konstruktor kosong, dipakai Hibernate. */
	public CatatanAdministrasi() {
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

	/** @return kode ringkas, {@code null} bila kosong/belum diisi (bukan string kosong). */
	@Column(name = "kode")
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/** @param kode kode ringkas baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama tampilan. Bila field masih kosong DAN {@link #getJenisCatatanAdministrasi()}
	 *         tidak {@code null}, field ini MENIMPA dirinya sendiri dengan nama jenis catatan
	 *         sebelum dikembalikan.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if ((nama == null || nama.isEmpty()) && getJenisCatatanAdministrasi() != null) {
			nama = getJenisCatatanAdministrasi().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tampilan baru; bisa tertimpa lagi bila kosong dan jenis catatan terisi. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @param index nomor urut tampilan tambahan yang baru. */
	public void setIndex(Long index) {
		this.index = index;
	}

	/** @return nomor urut tampilan tambahan, boleh {@code null}. */
	public Long getIndex() {
		return index;
	}

	/** @return isi/uraian catatan, string kosong (bukan {@code null}) bila belum diisi. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/** @param keterangan isi/uraian catatan yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
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
	@JoinColumn(name = "jenis_catatan_administrasi")
	public JenisCatatanAdministrasi getJenisCatatanAdministrasi() {
		jenisCatatanAdministrasi = check(jenisCatatanAdministrasi);
		return jenisCatatanAdministrasi;
	}

	/** @param jenisCatatanAdministrasi jenis/klasifikasi catatan yang baru. */
	public void setJenisCatatanAdministrasi(JenisCatatanAdministrasi jenisCatatanAdministrasi) {
		this.jenisCatatanAdministrasi = jenisCatatanAdministrasi;
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CatatanAdministrasi.java:201");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CatatanAdministrasi.java:207");

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
	 * #resolveJenisParameterTambahan(Class, Long, String)} dengan {@code CatatanAdministrasi.class}
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
				KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi = (KelompokParameterTambahanCatatanAdministrasi) row
						.getAttribute("kelompokParameterTambahanCatatanAdministrasi");
				if (parameterTambahan != null && kelompokParameterTambahanCatatanAdministrasi != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(CatatanAdministrasi.class, getId(),
							kelompokParameterTambahanCatatanAdministrasi.getId() + "->"
									+ parameterTambahan.getId());

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

					String s = kelompokParameterTambahanCatatanAdministrasi.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCatatanAdministrasi.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCatatanAdministrasi.getId() + "->"
							+ parameterTambahan.getId() + "<=>" + val + "<=>" + url + "<=>"
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

	/** @return satuan kerja terkait, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja")
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/** @param satuanKerja satuan kerja terkait yang baru. */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** @return baris disposisi SOP tempat catatan ini mengalir, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP -- WRITE-ONCE. Argumen {@code null} atau entity tanpa id ({@code
	 * getId() == null}) diabaikan diam-diam sejak awal (guard di kepala method). Selain itu, bila
	 * {@link #disposisiSop} SUDAH terisi sebelumnya, nilai lama itu dipertahankan walau argumen
	 * baru valid -- sehingga sekali disposisi terpasang, tidak ada jalur lewat setter ini untuk
	 * menggantinya ke baris disposisi lain.
	 *
	 * @param disposisiSop disposisi SOP baru; efektif hanya pada pengisian PERTAMA kali.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/** @return {@code true} bila catatan ini disiarkan (broadcast) ke banyak penerima; default {@code false}. */
	public Boolean getBroadcast() {
		return broadcast == null ? false : broadcast;
	}

	/** @param broadcast penanda broadcast yang baru. */
	public void setBroadcast(Boolean broadcast) {
		this.broadcast = broadcast;
	}

	/** @return tahun pencatatan; bila belum pernah diisi, DITENTUKAN SEKALI dari tahun kalender berjalan lalu disimpan ke field. */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/** @param tahun tahun pencatatan baru. */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/** @return bulan pencatatan (1-12); bila belum pernah diisi, DITENTUKAN SEKALI dari bulan kalender berjalan lalu disimpan ke field. */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/** @param bulan bulan pencatatan baru. */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * @return {@code true} (default) bila catatan masih aktif/berlaku. Field {@link #aktif} bisa
	 *         DITIMPA PERMANEN menjadi {@code false} bila {@link #getDisposisiSop()} menandakan
	 *         tidak aktif, ATAU bila alur SOP-nya sampai pada titik penolakan
	 *         ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}) -- sekali kondisi
	 *         itu terpenuhi pada suatu pemanggilan, field tidak pernah kembali {@code true} lagi
	 *         lewat getter ini walau disposisinya berubah kembali aktif.
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda aktif/berlaku yang baru; bisa tertimpa lagi oleh {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
