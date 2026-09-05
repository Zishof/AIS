package ais.database.model.obe;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;

/**
 * <h3>CapaianLulusan — Capaian Pembelajaran Lulusan (CPL), master tingkat program studi</h3>
 *
 * <p>Entitas master untuk rumusan Capaian Pembelajaran Lulusan (CPL) sebuah program studi
 * ({@link Jurusan}) — pernyataan kompetensi (sikap, pengetahuan, keterampilan umum, atau
 * keterampilan khusus, lihat {@link #getKategori()}) yang harus dikuasai setiap lulusan
 * program studi tersebut sesuai kerangka Outcome-Based Education (OBE) yang dianut modul
 * {@code ais.database.model.obe}. Ini <b>kelas berbeda</b> dari
 * {@link CapaianPembelajaranLulusan} meskipun namanya sangat mirip — lihat catatan
 * disambiguasi di bawah, ini pembeda paling penting untuk dipahami sebelum membaca kode
 * lain yang memakai kedua kelas ini.</p>
 *
 * <h4>Disambiguasi wajib: CapaianLulusan (CPL) vs CapaianPembelajaranLulusan (CPMK)</h4>
 * <p>Walau nama kelas {@link CapaianPembelajaranLulusan} secara harfiah berarti "Capaian
 * Pembelajaran Lulusan" — yang biasanya justru istilah baku untuk CPL, bukan CapaianLulusan —
 * penelusuran seluruh basis kode (bukan hanya nama kelas) menunjukkan kedua kelas ini
 * TIDAK sinonim, TIDAK snapshot-vs-master, dan TIDAK versi lama/baru dari hal yang sama.
 * Keduanya adalah dua konsep OBE yang berbeda pada level kurikulum yang berbeda:</p>
 * <ul>
 *   <li><b>{@code CapaianLulusan} (kelas ini) = CPL</b>: rumusan pada level <i>program studi</i>,
 *       tidak terikat mata kuliah tertentu (field {@link #khususBuatMk} umumnya kosong
 *       kecuali untuk kasus kurikulum lintas-prodi/kelas khusus), memiliki
 *       {@link #getKategori()} sesuai taksonomi SN-Dikti, dan menyimpan tiga daftar CSV
 *       relasi turunan: {@link #getProfil()} (ke {@link ProfilLulusan}/PL — CPL mendukung PL
 *       yang mana), {@link #getBahanKajian()} (ke {@link BahanKajian}/BK — bahan kajian apa
 *       yang mendukung CPL ini), dan {@link #getCapaianPembelajaranLulusan()} (ke
 *       {@link CapaianPembelajaranLulusan} — CPMK mana saja yang menurunkan/mengukur CPL ini).</li>
 *   <li><b>{@link CapaianPembelajaranLulusan} = CPMK</b> (Capaian Pembelajaran Mata Kuliah,
 *       istilah baku Dikti untuk capaian level <i>mata kuliah</i>): SELALU terikat ke satu
 *       {@code Matakuliah} tertentu (field {@code khususBuatMk} adalah inti relasinya, bukan
 *       kasus tepi), memiliki {@code formula}/{@code bobot}/{@code minimal} untuk
 *       <b>perhitungan pencapaian nilai mahasiswa</b> per komponen penilaian — dipakai luas
 *       oleh mesin penilaian OBE ({@code NilaiObeAction}, {@code FormatNilai},
 *       {@code PembombotanNilai}, dst.) dengan nama variabel {@code cpmk}/{@code cpmkCsv}/
 *       {@code kode_cpmk} di seluruh pemanggilnya — bukan sekadar dugaan dari nama kelas,
 *       tapi konvensi penamaan variabel yang konsisten di puluhan titik pemanggilan.
 *       {@code Matakuliah} sendiri punya field CSV {@code capaianPembelajaranLulusan} sendiri
 *       (daftar CPMK yang dipakai mata kuliah tsb.) yang independen dari field CSV
 *       {@link #capaianPembelajaranLulusan} milik CPL ini — keduanya kebetulan bernama sama
 *       tapi menyimpan CSV ID CPMK dari sudut pandang berbeda (CPL "dijabarkan oleh CPMK mana
 *       saja" vs Matakuliah "memakai CPMK mana saja").</li>
 * </ul>
 * <p>Singkatnya: rantai jenjang capaian OBE di modul ini adalah
 * <b>{@link ProfilLulusan} (PL) &rarr; CapaianLulusan (CPL, kelas ini) &rarr;
 * {@link CapaianPembelajaranLulusan} (CPMK, per mata kuliah) &rarr; Sub-CPMK</b> — istilah
 * "Sub-CPMK" sendiri tidak muncul sebagai nama kelas terpisah, tapi muncul eksplisit sebagai
 * konsep di kode pemanggil (mis. {@code FormatNilai.getKodeSubCpmk()}, kunci
 * {@code kode_sub_cpmk}) sebagai kunci di dalam JSON {@code formula} milik satu baris
 * {@link CapaianPembelajaranLulusan} — jadi Sub-CPMK adalah pemecahan lebih halus di
 * <i>dalam</i> satu CPMK, bukan entity/tabel tersendiri. Susunan ini persis mengikuti istilah
 * baku Dikti "CPL &rarr; CPMK &rarr; Sub-CPMK". {@link IndikatorKinerja} (IK) berdiri agak
 * di luar rantai linear ini: IK adalah penjabaran terukur yang menempel langsung ke CPL
 * (relasi many-to-one {@link IndikatorKinerja#getCapaianLulusan()}), dipakai untuk menilai
 * ketercapaian CPL secara independen dari jalur CPMK/nilai mata kuliah.</p>
 *
 * <p>Mengikuti pola entitas OBE lain: extends {@link GeneralValueObject}, ber-audit Envers,
 * tabel {@code public.capaianlulusan}, field audit shadow {@code oleh}/{@code olehId}/
 * {@code tanggal_dirubah} (kebutuhan teknis {@code AuditTimestampInterceptor}, bukan bug),
 * dan pola getter destruktif berpembungkus-koma untuk tiga field CSV-nya (lihat javadoc
 * masing-masing getter untuk detail normalisasi).</p>
 *
 * <p><b>Bank generated by hbm2java</b> — komentar asli generator; kelas sudah banyak diedit
 * manual (kolom {@code kategori}, {@code khusus_buat_mk}, {@code referensi_cpl}, dst. jelas
 * ditambahkan setelah generate awal).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "capaianlulusan")
public class CapaianLulusan extends GeneralValueObject {

	/** 
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return ID pengguna (username) yang terakhir mengubah baris CPL ini. Field audit shadow — lihat {@link #getOleh()}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
	 * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
	 * entitas modul OBE.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang terakhir mengubah baris CPL ini (field audit shadow, diisi via {@link #onUpdate()}). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis Hibernate sebelum UPDATE untuk
	 * mengisi {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} dari sesi pengguna aktif
	 * via {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Timestamp terakhir baris CPL ini diubah; default diisi saat objek dibuat, diperbarui via {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return timestamp terakhir baris CPL ini diubah. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas {@code "<id>-<nama>"}, dipakai untuk tampilan log/debug. */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private Jurusan jurusan;
	private PerguruanTinggi perguruanTinggi;
	private String nama;
	private String keterangan;
	private Boolean aktif;
	/** CSV ID {@link ProfilLulusan} yang didukung CPL ini — lihat {@link #getProfil()}. */
	private String profil;
	/** CSV ID {@link BahanKajian} yang mendukung CPL ini — lihat {@link #getBahanKajian()}. */
	private String bahanKajian;
	/** CSV ID {@link CapaianPembelajaranLulusan} (CPMK) yang menurunkan/mengukur CPL ini — lihat {@link #getCapaianPembelajaranLulusan()}. */
	private String capaianPembelajaranLulusan;
	/** Mata kuliah spesifik bila CPL ini bukan CPL umum prodi, melainkan khusus untuk satu mata kuliah — lihat {@link #getKhususBuatMk()}. */
	private Matakuliah khususBuatMk;
	/** Kategori CPL (teks bebas, biasanya nilai {@link KategoriCpl#getNama()}) — lihat {@link #getKategori()}. */
	private String kategori;
	private String referensi;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public CapaianLulusan() {
	}

	/** @return ID unik baris CPL (primary key, auto-increment via {@code IDENTITY}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return kode singkat CPL (mis. "CPL1", "CPL-S1"), di-trim; string kosong bila belum diisi. */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return rumusan/nama CPL (di-trim); {@code null} bila belum diisi. Wajib diisi ({@code nullable = false}). */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan CPL (opsional); tidak di-trim, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif CPL. Flag satu-arah: {@code null} (baris lama/belum pernah diisi)
	 *         dianggap aktif secara default agar data historis tidak tiba-tiba hilang dari
	 *         daftar CPL aktif.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return program studi pemilik CPL ini. Lazy-loaded via {@link GeneralValueObject#check(Object)};
	 *         bila CPL ini adalah CPL khusus mata kuliah ({@link #getKhususBuatMk()} terisi)
	 *         dan mata kuliah tsb. memiliki jurusan sendiri, jurusan mata kuliah tersebut
	 *         MENIMPA nilai field {@link #jurusan} — jadi untuk CPL khusus-MK, sumber
	 *         kebenaran jurusan sebenarnya adalah {@code khususBuatMk.getJurusan()}, bukan
	 *         field {@link #jurusan} milik baris ini sendiri.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		if (getKhususBuatMk() != null && getKhususBuatMk().getJurusan() != null) {
			jurusan = getKhususBuatMk().getJurusan();
		}
		return jurusan;
	}

	/** @param jurusan lihat {@link #getJurusan()}. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return perguruan tinggi pemilik CPL ini, ditentukan berjenjang: (1) field
	 *         {@link #perguruanTinggi} bila terisi, (2) fallback ke PT milik sesi pengguna
	 *         aktif via {@link ais.action.master.helper.util.PerguruanTinggiUtil#getPerguruanTinggi()}
	 *         (exception ditangkap diam-diam, direkam ke {@link ais.common.ErrorAuditUtil}),
	 *         lalu (3) <b>ditimpa lagi</b> oleh PT hasil penelusuran {@link #getJurusan()} →
	 *         Fakultas → PerguruanTinggi bila rantai relasi tsb. lengkap. Urutan override ini
	 *         berarti PT hasil penelusuran jurusan selalu menang di atas fallback sesi
	 *         maupun nilai field mentah — pastikan konsisten dengan getter serupa di
	 *         {@link BahanKajian#getPerguruanTinggi()} dan
	 *         {@link CapaianPembelajaranLulusan#getPerguruanTinggi()} yang memakai pola sama.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/obe/CapaianLulusan.java:170");
		}
		if (getJurusan() != null && getJurusan().getFakultas() != null
				&& getJurusan().getFakultas().getPerguruanTinggi() != null) {
			perguruanTinggi = getJurusan().getFakultas().getPerguruanTinggi();
		}
		return perguruanTinggi;
	}

	/** @param perguruanTinggi lihat {@link #getPerguruanTinggi()}. */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * <p>Daftar ID {@link ProfilLulusan} (PL) yang didukung/diturunkan oleh CPL ini, berformat
	 * CSV berpembungkus-koma di kedua ujungnya, mis. {@code ",3,7,12,"}. Ini adalah salah satu
	 * dari tiga field relasi CSV milik {@code CapaianLulusan} (bersama {@link #getBahanKajian()}
	 * dan {@link #getCapaianPembelajaranLulusan()}) yang secara kolektif membentuk sisi CPL
	 * dari rantai OBE {@code PL <-> CPL <-> BK} dan {@code CPL <-> CPMK}. Relasi ini adalah
	 * many-to-many yang disimpan sebagai teks (bukan tabel pivot terpisah) — desain yang umum
	 * di modul OBE ini, dipakai konsisten juga oleh {@code Matakuliah.getCapaianPembelajaranLulusan()}
	 * untuk relasi many-to-many mata-kuliah-ke-CPMK.</p>
	 *
	 * <p><b>Kenapa koma di kedua ujung, bukan CSV polos "3,7,12":</b> supaya pencarian
	 * keanggotaan ID cukup dengan {@code contains(",id,")} di pemanggil (lih.
	 * {@code RpsObeAction} baris {@code capaianLulusan.getProfil().contains("," + profilLulusan.getId() + ",")}),
	 * tanpa risiko salah tangkap ID {@code 1} di dalam ID {@code 12} atau {@code 21} seperti
	 * yang akan terjadi bila memakai {@code contains("1")} pada CSV polos. Pola pembungkus-koma
	 * yang identik dipakai di semua field CSV entitas OBE lain di paket ini.</p>
	 *
	 * <p><b>Getter ini destruktif (mutates state saat dibaca)</b>: setiap pemanggilan
	 * menormalkan ulang field {@link #profil} lalu <i>menimpa</i> nilainya sebelum
	 * dikembalikan — bukan sekadar menghitung nilai sementara untuk ditampilkan. Langkah
	 * normalisasinya: (1) bila field kosong/{@code null}/hanya berisi satu koma, mulai dari
	 * string kosong; selain itu (2) bungkus dengan koma di kedua ujung lalu (3) kolaps koma
     * ganda ({@code ",,"} → {@code ","}) sebanyak tiga kali berturut-turut (menangani hingga
	 * ~4 koma berurutan — batas ini cukup untuk kasus nyata tapi secara teori tidak menjamin
	 * kolaps sempurna untuk input yang sangat rusak dengan &gt;8 koma berurutan; risiko ini
	 * diterima karena input selalu berasal dari proses tambah/hapus ID satu-per-satu di UI,
	 * bukan input bebas pengguna). (4) Bila hasil akhirnya persis {@code ","}, {@code ",,"},
	 * {@code ",,,"}, atau {@code ",,,,"} (semua-koma, tak ada ID sama sekali), dikosongkan
	 * total. Hasil akhir selalu di-{@code trim()} dan tidak pernah {@code null} (string kosong
	 * sebagai pengganti). Karena efek samping ini terjadi pada setiap panggilan getter —
	 * termasuk panggilan berulang dalam satu request seperti pada loop checkbox di
	 * {@code RpsObeAction} — normalisasi berulang ini idempotent setelah panggilan pertama,
	 * jadi aman dipanggil berkali-kali, hanya saja butuh diketahui bahwa field internal ikut
	 * berubah, bukan hanya nilai kembalian.</p>
	 *
	 * @return CSV ID PL berpembungkus-koma (mis. {@code ",3,7,12,"}), atau string kosong bila
	 *         tidak ada PL yang terhubung.
	 */
	@Column(name = "profil", nullable = true, columnDefinition = "text")
	public String getProfil() {
		profil = (profil == null || profil.trim().equalsIgnoreCase(",") ? "" : "," + profil.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (profil.equals(",")) {
			profil = "";
		} else if (profil.equals(",,")) {
			profil = "";
		} else if (profil.equals(",,,")) {
			profil = "";
		} else if (profil.equals(",,,,")) {
			profil = "";
		}

		return profil == null ? "" : profil.trim();
	}

	/** @param profil lihat {@link #getProfil()}; boleh CSV mentah tanpa normalisasi, akan dinormalkan saat dibaca kembali via getter. */
	public void setProfil(String profil) {
		this.profil = profil;
	}

	/**
	 * Daftar ID {@link BahanKajian} (BK) yang mendukung CPL ini, berformat CSV
	 * berpembungkus-koma (mis. {@code ",5,9,"}) — mekanisme normalisasi getter destruktif
	 * ini identik dengan {@link #getProfil()}; lihat javadoc di sana untuk penjelasan lengkap
	 * alasan pembungkus-koma dan langkah kolaps koma ganda. Relasi ini adalah sisi CPL dari
	 * kerangka OBE {@code Bahan Kajian mendukung CPL} — bahan kajian sendiri biasanya
	 * dipetakan ke mata kuliah tertentu via {@link BahanKajian#getKhususBuatMk()}.
	 *
	 * @return CSV ID BK berpembungkus-koma, atau string kosong bila tidak ada BK terhubung.
	 */
	@Column(name = "bahan_kajian", nullable = true, columnDefinition = "text")
	public String getBahanKajian() {
		bahanKajian = (bahanKajian == null || bahanKajian.trim().equalsIgnoreCase(",") ? ""
				: "," + bahanKajian.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (bahanKajian.equals(",")) {
			bahanKajian = "";
		} else if (bahanKajian.equals(",,")) {
			bahanKajian = "";
		} else if (bahanKajian.equals(",,,")) {
			bahanKajian = "";
		} else if (bahanKajian.equals(",,,,")) {
			bahanKajian = "";
		}

		return bahanKajian == null ? "" : bahanKajian.trim();
	}

	/** @param bahanKajian lihat {@link #getBahanKajian()}; boleh CSV mentah, akan dinormalkan saat dibaca kembali. */
	public void setBahanKajian(String bahanKajian) {
		this.bahanKajian = bahanKajian;
	}

	/**
	 * Daftar ID {@link CapaianPembelajaranLulusan} (CPMK — lihat catatan disambiguasi di
	 * javadoc kelas) yang menjabarkan/mengukur CPL ini, berformat CSV berpembungkus-koma
	 * (mis. {@code ",14,22,"}) — mekanisme normalisasi getter destruktif identik dengan
	 * {@link #getProfil()}. Field ini dipakai luas oleh mesin pemetaan CPL-vs-CPMK
	 * ({@code CapaianLulusanVsCapaianPembelajaranLulusanAction}, matriks korelasi di
	 * {@code PikobeAction}) dan mesin rekap nilai OBE ({@code RekapHasilTugasPerTugasDanUjianObe},
	 * {@code DasboardObeElearningHelper}) untuk menghitung berapa banyak CPMK yang memetakan
	 * ke tiap CPL. <b>Perhatikan arah relasi</b>: field ini menyimpan "CPMK mana yang mengukur
	 * CPL ini", sedangkan {@code Matakuliah.getCapaianPembelajaranLulusan()} menyimpan "CPMK
	 * mana yang dipakai mata kuliah ini" — keduanya adalah CSV independen milik entity
	 * berbeda yang kebetulan menunjuk ke koleksi {@link CapaianPembelajaranLulusan} yang sama,
	 * tidak otomatis sinkron satu sama lain (mengubah salah satu tidak mengubah yang lain
	 * secara otomatis; sinkronisasi, bila ada, dilakukan eksplisit oleh kode pemanggil seperti
	 * {@code RpsObeExcelHelper}).
	 *
	 * @return CSV ID CPMK berpembungkus-koma, atau string kosong bila tidak ada CPMK terhubung.
	 */
	@Column(name = "capaian_pembelajaran_lulusan", nullable = true, columnDefinition = "text")
	public String getCapaianPembelajaranLulusan() {
		capaianPembelajaranLulusan = (capaianPembelajaranLulusan == null
				|| capaianPembelajaranLulusan.trim().equalsIgnoreCase(",") ? ""
						: "," + capaianPembelajaranLulusan.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (capaianPembelajaranLulusan.equals(",")) {
			capaianPembelajaranLulusan = "";
		} else if (capaianPembelajaranLulusan.equals(",,")) {
			capaianPembelajaranLulusan = "";
		} else if (capaianPembelajaranLulusan.equals(",,,")) {
			capaianPembelajaranLulusan = "";
		} else if (capaianPembelajaranLulusan.equals(",,,,")) {
			capaianPembelajaranLulusan = "";
		}

		return capaianPembelajaranLulusan == null ? "" : capaianPembelajaranLulusan.trim();
	}

	/** @param capaianPembelajaranLulusan lihat {@link #getCapaianPembelajaranLulusan()}; boleh CSV mentah, akan dinormalkan saat dibaca kembali. */
	public void setCapaianPembelajaranLulusan(String capaianPembelajaranLulusan) {
		this.capaianPembelajaranLulusan = capaianPembelajaranLulusan;
	}

	/**
	 * @return mata kuliah spesifik bila CPL ini adalah CPL <i>khusus</i> untuk satu mata
	 *         kuliah (bukan CPL umum program studi) — lazy-loaded, di-null-safe-kan via
	 *         {@link GeneralValueObject#check(Object)}. Umumnya {@code null} untuk CPL biasa;
	 *         bila terisi, field ini juga mempengaruhi hasil {@link #getJurusan()} (jurusan
	 *         mata kuliah menimpa jurusan CPL) dan transitif {@link #getPerguruanTinggi()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "khusus_buat_mk")
	public Matakuliah getKhususBuatMk() {
		khususBuatMk = check(khususBuatMk);
		return khususBuatMk;
	}

	/** @param khususBuatMk lihat {@link #getKhususBuatMk()}. */
	public void setKhususBuatMk(Matakuliah khususBuatMk) {
		this.khususBuatMk = khususBuatMk;
	}

	/**
	 * Kategori CPL sesuai SN-Dikti: Sikap / Pengetahuan / Keterampilan Umum / Keterampilan Khusus
	 *
	 * <p><b>Catatan arsitektur:</b> field ini adalah teks bebas (bukan relasi FK), meski
	 * modul OBE memiliki entitas master khusus {@link KategoriCpl} untuk daftar kategori CPL
	 * per perguruan tinggi. Form pengisian ({@code CapaianLulusanAction}) mengisi field ini
	 * dengan menyalin {@link KategoriCpl#getNama()} dari kategori yang dipilih pengguna di
	 * combobox — bukan menyimpan {@code id} milik {@link KategoriCpl} sebagai foreign key.
	 * Ini pilihan desain yang disengaja (lihat komentar UI "Data lama tetap dapat diedit
	 * walaupun kategorinya sudah dinonaktifkan/dihapus"): CPL lama tetap dapat dibuka/diedit
	 * walau baris {@link KategoriCpl} sumbernya sudah dinonaktifkan atau dihapus, karena tidak
	 * ada FK yang bisa putus. Konsekuensinya: mengganti {@code nama} sebuah {@link KategoriCpl}
	 * yang sudah dipakai tidak akan ikut mengubah label kategori pada CPL yang sudah tersimpan
	 * (tidak ada propagasi rename), dan tidak ada jaminan referensial — nilai
	 * {@link #kategori} bisa saja tidak cocok dengan {@code nama} {@link KategoriCpl} manapun
	 * (mis. karena rename lain waktu, atau CPL dibuat sebelum {@link KategoriCpl} tersebut ada).
	 *
	 * @return kategori CPL (di-trim), atau string kosong bila belum diisi.
	 */
	@Column(name = "kategori", nullable = true, columnDefinition = "text")
	public String getKategori() {
		return kategori == null ? "" : kategori.trim();
	}

	/** @param kategori lihat {@link #getKategori()}; idealnya diisi dengan nilai {@link KategoriCpl#getNama()} yang sedang aktif. */
	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	/**
	 * Referensi sumber CPL (CSV ID ReferensiLulusan) — mis. SN-Dikti, KKNI, Peta Okupasi, SKKNI
	 *
	 * <p>CSV ID {@link ReferensiLulusan} berpembungkus-koma; mekanisme normalisasi getter
	 * destruktif identik dengan {@link #getProfil()} — lihat javadoc di sana untuk penjelasan
	 * lengkap. Perhatikan nama kolom DB-nya {@code referensi_cpl}, berbeda dari nama field
	 * Java {@code referensi} dan berbeda pula dari kolom {@code referensi} milik
	 * {@link ProfilLulusan#getReferensi()} — keduanya independen, masing-masing entity
	 * menyimpan daftar referensi rujukannya sendiri.
	 *
	 * @return CSV ID ReferensiLulusan berpembungkus-koma, atau string kosong bila tidak ada.
	 */
	@Column(name = "referensi_cpl", nullable = true, columnDefinition = "text")
	public String getReferensi() {
		referensi = (referensi == null || referensi.trim().equalsIgnoreCase(",") ? "" : "," + referensi.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");
		if (referensi.equals(",") || referensi.equals(",,") || referensi.equals(",,,") || referensi.equals(",,,,"))
			referensi = "";
		return referensi == null ? "" : referensi.trim();
	}

	/** @param referensi lihat {@link #getReferensi()}; boleh CSV mentah, akan dinormalkan saat dibaca kembali. */
	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}
}
