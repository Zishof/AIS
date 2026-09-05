package ais.database.model.lkp;

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

import ais.common.Common;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.file.LampiranLain;

/**
 * Entity utama modul LKP (Laporan Kinerja Pegawai) yang mencatat satu <b>baris realisasi kerja</b>
 * — laporan aktual satu pegawai atas satu {@link TargetKerjaPegawai} (target bulanan/tahunan untuk
 * satu {@link KegiatanTugasJabatan}), berisi kuantitas dan waktu yang benar-benar dikerjakan pada
 * satu rentang waktu tertentu. Tabel {@code public.realisasi_kerja_pegawai}.
 *
 * <p><b>Relasi header/detail dengan target — bukan 1:1.</b> Satu {@link TargetKerjaPegawai}
 * (header, unik per kombinasi pegawai + kegiatan + bulan + tahun lewat {@link
 * TargetKerjaPegawai#getKodeUnik()}) dapat memiliki <b>banyak</b> baris {@code RealisasiKerjaPegawai}
 * (detail/log kerja) dalam satu periode target — mis. beberapa entri realisasi per hari/minggu.
 * Layar realisasi ({@code ais.action.master.lkp.RealisasiKerjaPegawaiAction}) menjumlahkan
 * ({@code Projections.sum}) kolom {@link #getKuantitas()} dan {@link #getWaktu()} dari seluruh
 * baris realisasi yang {@link #getVerifikasi() terverifikasi} pada satu target, lalu membandingkan
 * totalnya terhadap {@link TargetKerjaPegawai#getKuantitas()}/{@link TargetKerjaPegawai#getWaktu()}
 * untuk menghitung persentase capaian ({@code (jumlah * 100.0) / target}). <b>Kualitas tidak
 * mengikuti pola ini</b>: entity ini sengaja tidak memiliki field "kualitas" tersendiri —
 * capaian kualitas dinilai langsung oleh asesor pada level target ({@link
 * TargetKerjaPegawai#getKualitasRealisasi()}, bukan dijumlah dari baris realisasi manapun), sehingga
 * satu target bisa memiliki banyak baris realisasi kuantitas/waktu namun hanya satu angka kualitas
 * yang dinilai per periode target.</p>
 *
 * <p><b>Parameter tambahan.</b> Baris ini juga dapat membawa isian form tambahan dinamis (lihat
 * {@link KelompokParameterTambahanKegiatan}/{@link ParameterTambahanKegiatan}) yang diserialisasi ke
 * {@link #getParameterTambahan()}/{@link #getParameterTambahanInds()} lewat {@link
 * #populateParameterTambahan(List)}, dengan lampiran (bila disyaratkan parameter) dinamai memakai
 * {@link ais.database.model.file.LampiranLain#resolveJenisParameterTambahan} — mekanisme yang sama
 * dengan yang sudah dipakai luas di modul lain untuk menghindari tabrakan namespace jenis/ref
 * lampiran.</p>
 *
 * @see TargetKerjaPegawai
 * @see KegiatanTugasJabatan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "realisasi_kerja_pegawai")
public class RealisasiKerjaPegawai extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan/mengubah baris ini (field audit shadow,
	 * pasangan {@link #getOleh()}, diisi manual — bukan oleh interceptor otomatis). Dipetakan
	 * sebagai kolom teks ({@code columnDefinition = "text"}).
	 *
	 * @return id pengguna terakhir, dapat {@code null} bila belum pernah diisi.
	 */
	@Column(name = "olehid", columnDefinition = "text")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna yang melakukan perubahan. Nilai {@code null} atau string kosong/blank
	 * diabaikan secara diam-diam (nilai lama dipertahankan).
	 *
	 * @param olehId id pengguna; diabaikan jika {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama/label pengguna yang melakukan perubahan (pasangan {@link #setOlehId(String)}).
	 * Nilai {@code null} atau kosong/blank diabaikan secara diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan jika {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/label pengguna yang terakhir menyimpan/mengubah baris ini. Dipetakan
	 * sebagai kolom teks ({@code columnDefinition = "text"}).
	 *
	 * @return nama pengguna terakhir, dapat {@code null} bila belum pernah diisi.
	 */
	@Column(name = "oleh", columnDefinition = "text")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} melalui {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini diupdate.
	 * Dipanggil otomatis oleh provider persistence, bukan untuk dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan timestamp perubahan terakhir secara eksplisit. Umumnya tidak perlu dipanggil manual
	 * karena {@link #onUpdate()} sudah memperbarui nilai ini otomatis pada setiap update.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini, dipetakan sebagai kolom timestamp.
	 * Diinisialisasi ke waktu saat ini pada konstruksi objek dan diperbarui otomatis oleh {@link
	 * #onUpdate()} setiap kali entity ini diupdate.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk keperluan tampilan/log/debug, berupa gabungan {@code id} dan
	 * {@link #getKeterangan() keterangan} realisasi.
	 *
	 * @return string {@code "<id>-<keterangan>"}.
	 */
	public String toString() {
		return id + "-" + keterangan;
	}

	private TargetKerjaPegawai targetKerjaPegawai;
	private Pegawai pegawai;
	private Double kuantitas;
	private Double waktu;
	private Double biaya;
	private Date tanggalWaktu;
	private Date tanggalWaktuSampai;

	private String keterangan;
	private String catatan;
	private Boolean verifikasi;
	private String parameterTambahanInds;
	private String parameterTambahan;

	/** Konstruktor default (dibutuhkan Hibernate/JPA); field diinisialisasi ke nilai default. */
	public RealisasiKerjaPegawai() {
	}

	/**
	 * Mengembalikan id primary key baris realisasi ini. Dipetakan {@code insertable = false} karena
	 * nilai dibangkitkan basis data (identity).
	 *
	 * @return id realisasi, atau {@code null} untuk instance yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id baris realisasi ini.
	 *
	 * @param id id realisasi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan/deskripsi bebas untuk realisasi ini (kolom teks).
	 *
	 * @return keterangan realisasi, dapat {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan/deskripsi bebas untuk realisasi ini.
	 *
	 * @param keterangan keterangan realisasi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link Pegawai} pemilik realisasi ini. Logikanya <b>mengutamakan pegawai dari
	 * target</b>: bila {@link #getTargetKerjaPegawai()} tersedia dan memiliki pegawai, nilai
	 * tersebutlah yang dikembalikan (dan menimpa field {@code pegawai} lokal), memastikan realisasi
	 * selalu konsisten dengan pegawai pemilik target yang sebenarnya walau field {@code pegawai}
	 * lokal pada baris ini pernah di-set ke nilai lain. Field {@code pegawai} lokal (di-refresh
	 * lewat {@link #check(Object)}) hanya dipakai sebagai fallback ketika baris ini tidak (lagi)
	 * memiliki target — mis. data historis atau realisasi lepas yang tidak ditautkan ke target
	 * manapun. Setiap exception saat proses ini (mis. lazy-load gagal di luar sesi Hibernate)
	 * ditelan dan dicatat lewat {@link ais.common.ErrorAuditUtil#record}, sehingga pemanggil bisa
	 * menerima nilai {@code pegawai} lama/null tanpa menyadari kegagalan pemuatan.
	 *
	 * @return pegawai pemilik target (diutamakan) atau pegawai lokal (fallback), dapat {@code null}
	 *         bila keduanya tidak tersedia.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		try {
			targetKerjaPegawai = getTargetKerjaPegawai();
			if (targetKerjaPegawai != null && targetKerjaPegawai.getPegawai() != null) {
				pegawai = targetKerjaPegawai.getPegawai();
			} else {
				pegawai = check(pegawai);
			}
		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/lkp/RealisasiKerjaPegawai.java:141");
			// TODO: handle exception
		}
		return pegawai;
	}

	/**
	 * Menetapkan pegawai pemilik realisasi secara langsung. Perhatikan bahwa nilai ini hanya akan
	 * dipakai sebagai fallback oleh {@link #getPegawai()} bila {@link #getTargetKerjaPegawai()}
	 * tidak tersedia atau tidak memiliki pegawai — selama target tersedia, nilai pegawai target
	 * yang akan dikembalikan, bukan nilai yang di-set di sini.
	 *
	 * @param pegawai pegawai pemilik realisasi (fallback).
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan {@link TargetKerjaPegawai} (target bulanan/tahunan) yang direalisasikan oleh
	 * baris ini. Satu target dapat memiliki banyak baris realisasi (lihat penjelasan relasi
	 * header/detail pada Javadoc kelas). Relasi lazy, opsional pada level kolom.
	 *
	 * @return target kerja pegawai terkait, dapat {@code null} bila realisasi ini tidak ditautkan
	 *         ke target manapun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "target_kerja_pegawai", nullable = true)
	public TargetKerjaPegawai getTargetKerjaPegawai() {
		targetKerjaPegawai = check(targetKerjaPegawai);
		return targetKerjaPegawai;
	}

	/**
	 * Menetapkan target kerja pegawai yang direalisasikan oleh baris ini.
	 *
	 * @param targetKerjaPegawai target kerja pegawai baru.
	 */
	public void setTargetKerjaPegawai(TargetKerjaPegawai targetKerjaPegawai) {
		this.targetKerjaPegawai = targetKerjaPegawai;
	}

	/**
	 * Mengembalikan kuantitas pekerjaan yang direalisasikan pada baris ini. Nilai-nilai dari
	 * seluruh baris realisasi milik satu target (yang {@link #getVerifikasi() terverifikasi}) akan
	 * dijumlahkan oleh layar realisasi untuk dibandingkan terhadap {@link
	 * TargetKerjaPegawai#getKuantitas()} sebagai persentase capaian.
	 *
	 * @return kuantitas terealisasi; {@code 0.0} bila belum diisi.
	 */
	public Double getKuantitas() {
		return kuantitas == null ? 0.0 : kuantitas;
	}

	/**
	 * Menetapkan kuantitas pekerjaan yang direalisasikan pada baris ini.
	 *
	 * @param kuantitas kuantitas terealisasi baru.
	 */
	public void setKuantitas(Double kuantitas) {
		this.kuantitas = kuantitas;
	}

	/**
	 * Mengembalikan waktu yang dihabiskan untuk realisasi ini (dalam satuan {@link
	 * KegiatanTugasJabatan#getSatuanWaktu()} milik kegiatan pada target terkait). Nilai-nilai dari
	 * seluruh baris realisasi milik satu target yang terverifikasi dijumlahkan untuk dibandingkan
	 * terhadap {@link TargetKerjaPegawai#getWaktu()} sebagai persentase capaian.
	 *
	 * @return waktu terealisasi; {@code 0.0} bila belum diisi.
	 */
	public Double getWaktu() {
		return waktu == null ? 0.0 : waktu;
	}

	/**
	 * Menetapkan waktu yang dihabiskan untuk realisasi ini.
	 *
	 * @param waktu waktu terealisasi baru.
	 */
	public void setWaktu(Double waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan biaya yang timbul dari realisasi ini.
	 *
	 * @return biaya terealisasi; {@code 0.0} bila belum diisi.
	 */
	public Double getBiaya() {
		return biaya == null ? 0.0 : biaya;
	}

	/**
	 * Menetapkan biaya yang timbul dari realisasi ini.
	 *
	 * @param biaya biaya terealisasi baru.
	 */
	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/**
	 * Mengembalikan tanggal/waktu mulai realisasi ini dikerjakan.
	 *
	 * @return tanggal waktu mulai; waktu saat ini ({@link ais.ui.util.WaktuUtil#getDate()}) bila
	 *         belum diisi.
	 */
	public Date getTanggalWaktu() {
		return tanggalWaktu == null ? ais.ui.util.WaktuUtil.getDate() : tanggalWaktu;
	}

	/**
	 * Menetapkan tanggal/waktu mulai realisasi ini dikerjakan.
	 *
	 * @param tanggalWaktu tanggal waktu mulai baru.
	 */
	public void setTanggalWaktu(Date tanggalWaktu) {
		this.tanggalWaktu = tanggalWaktu;
	}

	/**
	 * Mengembalikan status verifikasi baris realisasi ini. Selain field {@code verifikasi} lokal,
	 * status ini juga <b>diwarisi satu-arah</b> dari target: bila {@link #getTargetKerjaPegawai()}
	 * (memakai field {@code targetKerjaPegawai} yang sudah termuat, bukan memanggil ulang getter)
	 * sudah {@link TargetKerjaPegawai#getVerifikasi() terverifikasi}, field lokal {@code
	 * verifikasi} ditimpa menjadi {@code true} — namun sebaliknya tidak berlaku: bila target
	 * <i>belum</i> terverifikasi, status verifikasi lokal baris ini tidak diturunkan/direset
	 * mengikuti target (nilai lokal yang sudah {@code true} tetap {@code true}). Efeknya,
	 * memverifikasi target akan otomatis membuat seluruh baris realisasinya tampak terverifikasi
	 * pada pemanggilan berikutnya, tetapi membatalkan verifikasi target tidak membatalkan status
	 * verifikasi baris realisasi yang sudah terlanjur ditandai {@code true} secara lokal.
	 *
	 * @return {@code true} bila baris ini atau targetnya sudah terverifikasi; {@code false} bila
	 *         belum diisi/belum diverifikasi sama sekali.
	 */
	public Boolean getVerifikasi() {
		if (targetKerjaPegawai != null && targetKerjaPegawai.getVerifikasi()) {
			verifikasi = true;
		}
		return verifikasi == null ? false : verifikasi;
	}

	/**
	 * Menetapkan status verifikasi baris realisasi ini secara lokal. Lihat catatan pada {@link
	 * #getVerifikasi()} mengenai pewarisan satu-arah dari status verifikasi target.
	 *
	 * @param verifikasi status verifikasi baru.
	 */
	public void setVerifikasi(Boolean verifikasi) {
		this.verifikasi = verifikasi;
	}

	/**
	 * Mengembalikan catatan bebas (mis. catatan asesor/atasan) untuk realisasi ini (kolom teks).
	 *
	 * @return catatan realisasi, dapat {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	/**
	 * Menetapkan catatan bebas untuk realisasi ini.
	 *
	 * @param catatan catatan baru.
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Mengembalikan representasi terserialisasi ringkas (indeks/id) dari isian parameter tambahan
	 * pada baris ini, dibangun oleh {@link #populateParameterTambahan(List)}. Berbeda dari {@link
	 * #getParameterTambahan()} yang menyimpan versi lengkap (label, nilai, url lampiran, urutan,
	 * id), representasi ini hanya membawa id kelompok/parameter, nilai, dan url — dipakai untuk
	 * pencarian/pencocokan cepat tanpa perlu mem-parsing string label yang bisa berubah.
	 *
	 * @return string terserialisasi baris-per-baris {@code "kelompokId->parameterId<=>val<=>url"},
	 *         di-trim; string kosong bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		return parameterTambahanInds == null ? "" : parameterTambahanInds.trim();
	}

	/**
	 * Menetapkan representasi terserialisasi ringkas isian parameter tambahan baris ini.
	 *
	 * @param parameterTambahanInds string terserialisasi baru.
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengembalikan representasi terserialisasi lengkap dari isian parameter tambahan pada baris
	 * ini, dibangun oleh {@link #populateParameterTambahan(List)} dan diuraikan kembali oleh {@link
	 * #ambilDataParameterTambahan()}.
	 *
	 * @return string terserialisasi baris-per-baris {@code
	 *         "namaKelompok->labelInputan<=>val<=>url<=>nomorUrut<=>parameterId<=>kelompokId"},
	 *         di-trim; string kosong bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		return parameterTambahan == null ? "" : parameterTambahan.trim();
	}

	/**
	 * Menetapkan representasi terserialisasi lengkap isian parameter tambahan baris ini.
	 *
	 * @param parameterTambahan string terserialisasi baru.
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Membangun dan menyimpan representasi terserialisasi ({@link #parameterTambahan}/{@link
	 * #parameterTambahanInds}) dari isian form parameter tambahan dinamis yang diinput pengguna
	 * pada grid realisasi, satu baris ZK {@link Row} untuk satu kombinasi kelompok+parameter.
	 *
	 * <p>Untuk tiap {@code Row} yang membawa atribut {@code "parameterTambahan"} ({@link
	 * ParameterTambahan}) dan {@code "kelompokParameterTambahanKegiatan"} ({@link
	 * KelompokParameterTambahanKegiatan}) yang tidak {@code null}, method ini:</p>
	 * <ol>
	 * <li>Menghitung kunci namespace "jenis" lewat {@link
	 * ais.database.model.file.LampiranLain#resolveJenisParameterTambahan(Class, Long, String)}
	 * dengan kunci kombinasi {@code "kelompokId->parameterId"} — mencegah tabrakan lampiran antar
	 * parameter berbeda pada realisasi yang sama (pola resolver yang sudah dipakai luas di modul
	 * lain untuk menutup celah tabrakan namespace jenis/ref).</li>
	 * <li>Mengambil nilai isian aktual lewat {@link ParameterTambahan#ambilVal(Row, ParameterTambahan)}.</li>
	 * <li>Bila parameter mensyaratkan lampiran ({@link ParameterTambahan#getHarusMenyertakanLampiran()}),
	 * mencari {@link LampiranLain} yang sudah tersimpan untuk kombinasi id-realisasi + jenis ini
	 * lewat {@link LampiranLain#ambil(Long, String)}, lalu membentuk URI tautannya; kegagalan
	 * membentuk URI (exception) ditelan lewat {@link Common#tampilErrorJikaAdmin(Exception)} — baris
	 * tetap diproses tanpa url lampiran.</li>
	 * <li>Merangkai baris string lengkap (nama kelompok, label input, nilai, url, nomor urut, id
	 * parameter, id kelompok) ke {@link #parameterTambahan}, dan baris ringkas (id kelompok, id
	 * parameter, nilai, url) ke {@link #parameterTambahanInds}, dipisah newline antar baris.</li>
	 * </ol>
	 *
	 * <p>Setiap exception per-baris (mis. atribut row tidak lengkap, cast gagal) ditelan dan
	 * dicatat lewat {@link ais.common.ErrorAuditUtil#record}, sehingga satu baris bermasalah tidak
	 * menggagalkan pemrosesan baris lain dalam batch yang sama. Bila {@code parameterRows}
	 * {@code null} atau kosong, method langsung kembali tanpa mengubah apa pun (isian tambahan
	 * lama pada entity ini dipertahankan apa adanya, tidak dikosongkan).</p>
	 *
	 * @param parameterRows daftar baris ZK grid berisi pasangan kelompok+parameter+nilai yang
	 *                       diinput pengguna; boleh {@code null} atau kosong (no-op).
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
				KelompokParameterTambahanKegiatan kelompokParameterTambahanKegiatan = (KelompokParameterTambahanKegiatan) row
						.getAttribute("kelompokParameterTambahanKegiatan");
				if (parameterTambahan != null && kelompokParameterTambahanKegiatan != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(RealisasiKerjaPegawai.class, getId(),
							kelompokParameterTambahanKegiatan.getId() + "->" + parameterTambahan.getId());

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);

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

					String s = kelompokParameterTambahanKegiatan.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanKegiatan.getId();
					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanKegiatan.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url;
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/lkp/RealisasiKerjaPegawai.java:271");

			}
		}
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Menguraikan kembali (parse) string terserialisasi {@link #getParameterTambahan()} yang
	 * dibangun oleh {@link #populateParameterTambahan(List)} menjadi daftar {@link CommonVO} siap
	 * tampil, satu {@code CommonVO} per baris parameter tambahan.
	 *
	 * <p>String dipecah per baris (newline), lalu tiap baris dipecah lagi memakai delimiter
	 * {@code "<=>"} menjadi field: {@code [0]} label, {@code [1]} nilai, {@code [2]} url lampiran,
	 * {@code [3]} nomor urut, {@code [4]} id. Setiap field diambil secara defensif (dicek panjang
	 * array dan di-trim) sehingga baris dengan jumlah field kurang dari yang diharapkan tidak
	 * melempar {@link ArrayIndexOutOfBoundsException} — field yang hilang diperlakukan sebagai
	 * string kosong atau nilai default ({@code nomorUrut = 1}, {@code id = 1L}). Kegagalan parsing
	 * numerik ({@code nomorUrut}/{@code id}) ditelan lewat {@link ais.common.ErrorAuditUtil#record}
	 * dan jatuh ke nilai default yang sama.</p>
	 *
	 * <p>Hasil dipetakan ke {@link CommonVO}: {@code id} (dari kolom id, sebagai string), {@code
	 * name} (label), {@code name1} (nilai), {@code name2} (url lampiran), {@code nomorUrut}, lalu
	 * diurutkan ({@link Collections#sort(List)}) mengikuti {@link Comparable} milik {@link
	 * CommonVO} sebelum dikembalikan — memastikan urutan tampil konsisten dengan {@code nomorUrut}
	 * parameter, bukan urutan penyimpanan string.</p>
	 *
	 * @return daftar {@link CommonVO} terurut siap tampil, satu per baris parameter tambahan;
	 *         daftar berisi satu elemen kosong bila {@link #getParameterTambahan()} kosong (hasil
	 *         {@code "".split("\n")} adalah array satu elemen string kosong).
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/lkp/RealisasiKerjaPegawai.java:293");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/lkp/RealisasiKerjaPegawai.java:299");

			}
			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Menghitung (lazy, sekali saja per instance) dan mengembalikan tanggal/waktu selesai realisasi
	 * ini, yaitu {@link #getTanggalWaktu() tanggal waktu mulai} ditambah {@link #getWaktu() durasi
	 * waktu} dalam satuan {@link KegiatanTugasJabatan#getSatuanWaktu()} milik kegiatan pada target
	 * terkait.
	 *
	 * <p>Perhitungan hanya dilakukan bila {@link #tanggalWaktu}, {@link #waktu}, dan rantai relasi
	 * {@link #targetKerjaPegawai} → {@link TargetKerjaPegawai#getKegiatanTugasJabatan()} →
	 * {@link KegiatanTugasJabatan#getSatuanWaktu()} semuanya tersedia, <b>dan</b> {@link
	 * #tanggalWaktuSampai} belum pernah dihitung ({@code null}) — hasil dikembalikan dari cache
	 * field {@link #tanggalWaktuSampai} pada pemanggilan berikutnya tanpa dihitung ulang, sehingga
	 * bila {@link #tanggalWaktu}/{@link #waktu} diubah setelah pemanggilan pertama, nilai selesai
	 * yang sudah di-cache <b>tidak</b> ikut diperbarui kecuali {@link
	 * #setTanggalWaktuSampai(Date)} dipanggil ulang dengan {@code null} atau field direset secara
	 * eksternal.</p>
	 *
	 * <p>Satuan waktu dibandingkan case-insensitive terhadap lima label yang dikenali: {@code
	 * "Menit"}, {@code "Jam"}, {@code "Hari"}, {@code "Minggu"}, {@code "Bulan"} — dipetakan
	 * berturut-turut ke {@link Calendar#MINUTE}, {@link Calendar#HOUR_OF_DAY}, {@link
	 * Calendar#DATE}, {@link Calendar#WEEK_OF_MONTH}, {@link Calendar#MONTH}. Label satuan waktu di
	 * luar kelima nilai ini (mis. hasil default {@link KegiatanTugasJabatan#getSatuanWaktu()} yang
	 * tidak dikenali) tidak menambahkan apa pun ke kalender — {@code tanggalWaktuSampai} akan tetap
	 * sama dengan {@code tanggalWaktu} awal, bukan melempar error.</p>
	 *
	 * @return tanggal waktu selesai realisasi, dihitung sekali dan di-cache; sama dengan {@code
	 *         null} bila prasyarat data (tanggal mulai, waktu, atau rantai relasi kegiatan) belum
	 *         lengkap.
	 */
	public Date getTanggalWaktuSampai() {

		if (tanggalWaktu != null && waktu != null && tanggalWaktuSampai == null
				&& targetKerjaPegawai != null && targetKerjaPegawai.getKegiatanTugasJabatan() != null
				&& targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu() != null) {
			Date wkt = tanggalWaktu;
			int w = waktu.intValue();
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(wkt);
			if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Menit")) {
				calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + w);
			} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Jam")) {
				calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + w);
			} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Hari")) {
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + w);
			} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Minggu")) {
				calendar.set(Calendar.WEEK_OF_MONTH, calendar.get(Calendar.WEEK_OF_MONTH) + w);
			} else if (targetKerjaPegawai.getKegiatanTugasJabatan().getSatuanWaktu().equalsIgnoreCase("Bulan")) {
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + w);
			}
			tanggalWaktuSampai = calendar.getTime();
		}

		return tanggalWaktuSampai;
	}

	/**
	 * Menetapkan tanggal/waktu selesai realisasi secara eksplisit, membatalkan hasil cache yang
	 * mungkin sudah dihitung oleh {@link #getTanggalWaktuSampai()} sebelumnya.
	 *
	 * @param tanggalWaktuSampai tanggal waktu selesai baru; {@code null} akan membuat {@link
	 *                           #getTanggalWaktuSampai()} menghitung ulang pada pemanggilan
	 *                           berikutnya bila prasyaratnya terpenuhi.
	 */
	public void setTanggalWaktuSampai(Date tanggalWaktuSampai) {
		this.tanggalWaktuSampai = tanggalWaktuSampai;
	}
}
