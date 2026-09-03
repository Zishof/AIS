package ais.database.model.payroll;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;

/**
 * Rekap <b>absensi bulanan per pegawai</b> dalam format teks terserialisasi sendiri (bukan satu
 * baris per hari), dipetakan ke tabel <code>payroll.absen_pegawai_detail</code>. Satu baris
 * entity ini merangkum <b>satu bulan penuh</b> untuk satu pegawai — kuncinya
 * {@link #getKodeUnik()} disusun dari {@code pegawai.getId() + "_" + tahun + "_" + bulan} — dan
 * seluruh rincian harian (status hadir/izin/cuti per tanggal) dipadatkan ke dalam satu kolom teks
 * {@link #getAbsensi()}.
 *
 * <h3>1. Format serialisasi kolom {@code absensi}</h3>
 * <p>Kolom {@code absensi} (tipe <code>text</code>) menyimpan banyak "record" harian dipisah
 * titik-koma (<code>;</code>); setiap record adalah 14 nilai dipisah koma dengan urutan tetap
 * (indeks 0-13), sebagaimana disusun {@link #populate}:</p>
 * <ol start="0">
 *   <li><b>ref</b> — kunci record, mis. nomor hari dalam bulan atau kunci lain yang dipakai
 *       pemanggil; dibaca-tulis apa adanya, tidak divalidasi formatnya oleh kelas ini.</li>
 *   <li>ID {@code Statusabsensi} (di-parse {@code Long} oleh {@link #retreiveAbsensiId(String)}).</li>
 *   <li>kode {@code Statusabsensi} ({@link #retreiveAbsensiKode(String)}).</li>
 *   <li>nama {@code Statusabsensi} ({@link #retreiveAbsensiNama(String)}).</li>
 *   <li>literal <code>"0"</code> tetap — ditulis {@link #populate} tapi <b>tidak pernah dibaca
 *       balik</b> oleh method {@code retreiveAbsensi*} mana pun di kelas ini (slot format yang
 *       tersisa/tidak terpakai).</li>
 *   <li>keterangan bebas ({@link #retreiveAbsensiKeterangan(String)}) — satu-satunya field teks
 *       bebas dalam record; {@link #populate} men-sanitasi isinya dengan mengganti karakter
 *       <code>;</code> menjadi <code>"..\n"</code> dan <code>,</code> menjadi <code>_</code>
 *       <b>sebelum</b> disisipkan, supaya tidak merusak pemisah record/field format ini.</li>
 *   <li>jam/waktu mulai ({@link #retreiveAbsensiMulai(String)}).</li>
 *   <li>jam/waktu sampai ({@link #retreiveAbsensiSampai(String)}).</li>
 *   <li>penanda libur nasional ({@link #retreiveAbsensiLiburNasional(String)}).</li>
 *   <li>penanda libur rutin ({@link #retreiveAbsensiLiburRutin(String)}).</li>
 *   <li>penanda cuti/izin ({@link #retreiveAbsensiCutiDanIzin(String)}).</li>
 *   <li>ID data sumber ({@link #retreiveAbsensiIdData(String)}).</li>
 *   <li>jam mulai "harus"/wajib ({@link #retreiveAbsensiMulaiHarus(String)}).</li>
 *   <li>jam sampai "harus"/wajib ({@link #retreiveAbsensiSampaiHarus(String)}) — field terakhir.</li>
 * </ol>
 * <p>Tidak ada skema/kolom terpisah untuk 14 nilai ini di database — semuanya hidup sebagai satu
 * blob teks yang di-<i>parse</i> ulang setiap kali salah satu getter {@code retreiveAbsensi*}
 * dipanggil. Setiap method itu memanggil {@code split(",", n)} dengan batas ({@code limit}) yang
 * berbeda-beda sesuai indeks yang dibutuhkan (lihat javadoc masing-masing method), dan membungkus
 * pem-parsing-an dengan <code>catch (Exception e)</code> kosong (hanya mencatat ke
 * {@code ErrorAuditUtil}) — record yang rusak/pendek diam-diam diperlakukan seperti "tidak
 * ditemukan" (nilai default dikembalikan) alih-alih melempar exception ke pemanggil.</p>
 *
 * <h3>2. Status: entity ini tampak tidak lagi dipakai mesin absensi aktual (temuan arsitektur)</h3>
 * <p><b>Diverifikasi dari kode, bukan asumsi:</b> pencarian nama kelas {@code AbsenPegawaiDetail}
 * ke seluruh pohon sumber hanya menemukan tiga rujukan: (1) pemetaan ORM di
 * <code>hibernate.cfg.xml</code>, (2) satu baris di
 * <code>ais.common.newui.menu.NewUiModuleDashboardService</code> yang sekadar mendaftarkan kelas
 * ini sebagai salah satu "model" pada kartu dashboard generik "Dashboard Presensi" (dipakai untuk
 * menampilkan nama model, bukan memanggil method-nya), dan (3) file kelas ini sendiri. Tidak ada
 * satu pun {@code Action}, helper, servlet API, atau laporan lain di kode yang memanggil
 * {@link #ambil(Pegawai, Integer, Integer)}, {@link #populate}, atau method
 * {@code retreiveAbsensi*} manapun.</p>
 * <p>Ini <b>berbeda</b> dari rantai absensi-ke-gaji yang benar-benar aktif di modul payroll, yaitu
 * {@code StatuskehadiranKaryawanHarian} (satu baris per pegawai per hari, berelasi ke
 * {@link DetailJenisShiftPegawai}/{@link JenisShiftPegawai} lewat
 * {@code JenisShiftPunyaPegawai}) yang dipakai luas oleh <code>ProsesAbsensiPegawai</code>,
 * <code>AbsensiApiAction</code>, dan berbagai laporan payroll — lihat javadoc kelas
 * {@link JenisShiftPegawai} bagian 1. {@code AbsenPegawaiDetail} <b>tidak</b> berelasi struktural
 * (tidak ada FK, tidak ada rujukan silang di kode) ke {@code JenisShiftPegawai},
 * {@link DetailJenisShiftPegawai}, maupun {@code StatuskehadiranKaryawanHarian} — keduanya hanya
 * konsep serumpun ("absensi pegawai") yang kebetulan hidup di package yang sama. Karena tidak
 * terpakai, tidak ada bukti bahwa angka pada entity ini pernah mengalir ke perhitungan
 * lembur/potongan gaji ({@code TransaksiPegawai}/{@code ItemGajiPegawai}) — rantai gaji nyata
 * bersumber dari {@code StatuskehadiranKaryawanHarian}, bukan dari sini.</p>
 *
 * <h3>3. Pola arsitektur berulang di kelas ini</h3>
 * <ul>
 *   <li><b>Field audit shadow</b> — {@link #getOleh()}/{@link #getOlehId()}/
 *       {@link #getTanggal_dirubah()} tidak beranotasi <code>@Column</code>; keharusan teknis
 *       karena {@link GeneralValueObject} bukan <code>@Entity</code> Hibernate murni, bukan bug —
 *       pola yang sama seperti pada {@link JenisShiftPegawai}.</li>
 *   <li><b>Getter destruktif</b> — {@link #getKodeUnik()} menghitung ulang dan menulis balik
 *       field {@code pegawai} dan {@code kodeUnik} setiap kali dipanggil, terlepas dari nilai
 *       yang tersimpan sebelumnya (lihat javadoc method tersebut).</li>
 *   <li><b>Tidak ada filter tenant/satuan-kerja</b> — {@link #ambil} mencari baris murni lewat
 *       {@link #getKodeUnik()} (turunan dari ID pegawai) tanpa validasi tambahan bahwa pegawai
 *       tersebut milik tenant/yayasan pemanggil; keamanan tenant sepenuhnya bergantung pada
 *       lapisan pemanggil (yang, sejauh ditemukan, tidak ada).</li>
 * </ul>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "absen_pegawai_detail", schema = "payroll")
public class AbsenPegawaiDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java untuk kompatibilitas {@link java.io.Serializable}. Nilainya tidak
	 * pernah diubah manual sejak generate awal; tidak berkaitan dengan skema tabel.
	 */
	private static final long serialVersionUID = 7154228487700348608L;

	/** Kunci primer baris ini, auto-increment ({@code IDENTITY}) pada kolom {@code id}. */
	private Long id;

	/**
	 * Nama/username pengguna yang terakhir membuat/mengubah baris ini. Field audit shadow — lihat
	 * catatan pola arsitektur pada javadoc kelas.
	 */
	private String oleh;

	/**
	 * ID pengguna yang terakhir membuat/mengubah baris ini. Field audit shadow — lihat catatan
	 * pola arsitektur pada javadoc kelas.
	 */
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna yang mengubah baris ini. Nilai {@code null} atau kosong/blank
	 * <b>diabaikan</b> (dibiarkan tidak berubah), bukan ditimpa jadi {@code null} — sama seperti
	 * pola pada {@link JenisShiftPegawai#setOlehId(String)}.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama/username pengguna yang mengubah baris ini. Nilai {@code null} atau
	 * kosong/blank <b>diabaikan</b>, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama/username pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum statement
	 * UPDATE dikirim. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang memperbarui
	 * stempel waktu perubahan ({@link #getTanggal_dirubah()}) secara terpusat — pola yang sama
	 * seperti {@link JenisShiftPegawai#onUpdate()}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Stempel waktu perubahan terakhir pada baris ini. Diinisialisasi ke waktu server saat
	 * instance dibuat, lalu diperbarui otomatis oleh {@link #onUpdate()} pada setiap UPDATE.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan secara manual. Umumnya tidak perlu dipanggil langsung
	 * karena {@link #onUpdate()} sudah mengurusnya otomatis pada setiap UPDATE.
	 *
	 * @param tanggal_dirubah stempel waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil stempel waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal/jam perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Pegawai pemilik rekap absensi bulanan ini. Bagian dari {@link #getKodeUnik()}. */
	private Pegawai pegawai;

	/**
	 * Blob teks berisi seluruh record absensi harian bulan ini, terserialisasi dalam format
	 * khusus (14 field per record dipisah koma, record dipisah titik-koma) — lihat javadoc kelas
	 * bagian 1 untuk rincian lengkap tata letaknya.
	 */
	private String absensi;

	/** Tahun (mis. 2026) rekap absensi ini. Bagian dari {@link #getKodeUnik()}. */
	private Integer tahun;

	/**
	 * Bulan rekap absensi ini. Bagian dari {@link #getKodeUnik()}. Perhatikan
	 * {@link #getBulan()}: fallback-nya memakai {@code Calendar.MONTH} yang berbasis-0
	 * (Januari = 0), berbeda dengan {@link #getTahun()} yang memakai {@code Calendar.YEAR} apa
	 * adanya — lihat catatan konsistensi pada javadoc {@link #getBulan()}.
	 */
	private Integer bulan;

	/**
	 * Kunci unik turunan (bukan sumber kebenaran independen) yang disusun dari ID pegawai, tahun,
	 * dan bulan. Selalu dihitung ulang oleh {@link #getKodeUnik()} setiap dipanggil — lihat
	 * javadoc method tersebut.
	 */
	private String kodeUnik;

	/**
	 * Konstruktor default (kosong), dipakai Hibernate saat instansiasi entity dari hasil query
	 * serta oleh {@link #ambil} saat membuat baris rekap bulanan baru.
	 */
	public AbsenPegawaiDetail() {
	}

	/**
	 * Mengambil kunci primer baris ini.
	 *
	 * @return ID baris, atau {@code null} bila belum tersimpan (transient).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris ini. Kolomnya {@code insertable = false} pada mapping Hibernate
	 * (nilai dihasilkan DB lewat IDENTITY), jadi setter ini normalnya hanya relevan saat Hibernate
	 * mengisi field dari hasil query.
	 *
	 * @param id ID baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil blob teks seluruh record absensi bulan ini, di-trim. {@code null} dinormalisasi
	 * jadi string kosong (bukan {@code null}) — jadi pemanggil ({@code retreiveAbsensi*}) selalu
	 * aman memanggil {@code .split(";")} tanpa perlu null-check.
	 *
	 * @return blob absensi (format lihat javadoc kelas bagian 1), tidak pernah {@code null}.
	 */
	@Column(name = "absensi", columnDefinition = "text")
	public String getAbsensi() {
		return absensi == null ? "" : absensi.trim();
	}

	/**
	 * Menyetel blob teks absensi bulan ini. Bila nilainya tidak kosong, method ini juga memanggil
	 * {@link GeneralValueObject#put(String, String)} dengan tag {@code "absensi"} — menulis
	 * salinan nilai ke cache berbasis-berkas per-instance milik {@link GeneralValueObject}
	 * (dipasangkan dengan {@code retreive("absensi")}), <b>terpisah</b> dari penyimpanan field
	 * biasa yang terjadi di baris berikutnya. Nilai kosong/{@code null} <b>tidak</b> memicu
	 * {@code put(...)} — cache lama (bila ada) tidak ikut dibersihkan pada kasus itu.
	 *
	 * @param absensi blob absensi baru; bila kosong/{@code null}, hanya field yang diperbarui.
	 */
	public void setAbsensi(String absensi) {
		if (absensi != null && !absensi.isEmpty()) {
			put(absensi, "absensi");
		}
		this.absensi = absensi;
	}

	/**
	 * Mengambil (atau membuat bila belum ada) baris rekap absensi bulanan untuk satu pegawai pada
	 * satu tahun/bulan, memakai sesi Hibernate baru yang dikelola otomatis. Sekadar overload
	 * praktis dari {@link #ambil(Pegawai, Integer, Integer, Session)} dengan
	 * {@code sessionData = null}.
	 *
	 * @param pegawai pegawai yang dicari rekapnya.
	 * @param tahun tahun rekap.
	 * @param bulan bulan rekap.
	 * @return baris {@code AbsenPegawaiDetail} yang ditemukan atau baru dibuat, atau {@code null}
	 *         bila terjadi exception (lihat catatan penanganan error pada overload 4-parameter).
	 */
	public static AbsenPegawaiDetail ambil(Pegawai pegawai, Integer tahun, Integer bulan) {
		return ambil(pegawai, tahun, bulan, null);
	}

	/**
	 * Mengambil (atau membuat bila belum ada) baris rekap absensi bulanan untuk satu pegawai pada
	 * satu tahun/bulan — implementasi "get-or-create" berbasis {@link #getKodeUnik()}.
	 *
	 * <p>Alurnya: (1) hitung {@code kodeUnik} lewat {@link #ambilKodeUnik(Pegawai, Integer,
	 * Integer)}, (2) cari baris dengan {@code kodeUnik} tersebut lewat Criteria, (3) bila tidak
	 * ditemukan, buat instance baru, isi {@code pegawai}/{@code tahun}/{@code bulan}, lalu simpan
	 * dalam transaksi manual ({@code session.getTransaction().begin()}/{@code commit()}) yang
	 * dibuka dan ditutup di dalam method ini sendiri — <b>bukan</b> mengikuti transaksi milik
	 * pemanggil, sehingga bila pemanggil sedang di tengah transaksinya sendiri, commit di sini
	 * akan menutup/memengaruhi transaksi tersebut secara independen.</p>
	 *
	 * <p><b>Manajemen sesi:</b> bila {@code sessionData} diberikan ({@code non-null}), sesi itu
	 * dipakai apa adanya dan <b>tidak</b> ditutup oleh method ini (tanggung jawab pemanggil). Bila
	 * {@code sessionData} {@code null}, method mengambil sesi native lewat
	 * {@link HibernateUtil#currentNativeSession()}, lalu di akhir memanggil
	 * {@code session.disconnect()}/{@code close()} <b>dan</b> {@link HibernateUtil#closeSession()}
	 * — dua mekanisme penutupan sesi yang berbeda dipanggil berurutan untuk kasus yang sama.</p>
	 *
	 * <p><b>Penanganan error:</b> seluruh badan method dibungkus satu {@code try/catch(Exception)}
	 * yang mencetak stack trace dan mencatat ke {@code ErrorAuditUtil}, lalu method tetap
	 * melanjutkan ke blok penutupan sesi di luar {@code try} dan mengembalikan {@code data} —
	 * yang pada kasus exception tetap bernilai {@code null} (tidak sempat di-assign). Pemanggil
	 * harus selalu menangani hasil {@code null} sebagai kemungkinan sah, bukan hanya "pegawai
	 * belum punya rekap".</p>
	 *
	 * @param pegawai pegawai yang dicari rekapnya; diteruskan apa adanya ke
	 *                {@link #ambilKodeUnik(Pegawai, Integer, Integer)} — bila {@code null}, kode
	 *                unik memakai {@code "0"} sebagai pengganti ID pegawai.
	 * @param tahun tahun rekap.
	 * @param bulan bulan rekap.
	 * @param sessionData sesi Hibernate yang sudah terbuka untuk dipakai ulang, atau {@code null}
	 *                     untuk membuka dan menutup sesi baru secara otomatis.
	 * @return baris yang ditemukan atau baru dibuat, atau {@code null} bila terjadi exception.
	 */
	public static AbsenPegawaiDetail ambil(Pegawai pegawai, Integer tahun, Integer bulan, Session sessionData) {
		AbsenPegawaiDetail data = null;
		try {
			Session session = sessionData == null ? HibernateUtil.currentNativeSession() : sessionData;

			String kodeUnik = AbsenPegawaiDetail.ambilKodeUnik(pegawai, tahun, bulan);

			AbsenPegawaiDetail absenPiketDetail = (AbsenPegawaiDetail) session.createCriteria(AbsenPegawaiDetail.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
			if (absenPiketDetail == null) {
				absenPiketDetail = new AbsenPegawaiDetail();
				absenPiketDetail.setPegawai(pegawai);
				absenPiketDetail.setTahun(tahun);
				absenPiketDetail.setBulan(bulan);
				session.getTransaction().begin();
				session.save(absenPiketDetail);
				session.getTransaction().commit();
			}

			if (sessionData == null) {
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
			}

			data = absenPiketDetail;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/payroll/AbsenPegawaiDetail.java:142");
		}
		if (sessionData == null) {
			HibernateUtil.closeSession();
		}
		return data;
	}

	/**
	 * Mencari record harian ber-{@code ref} tertentu di dalam {@link #getAbsensi()} dan
	 * mengembalikan kode {@code Statusabsensi}-nya (field indeks 2 — lihat javadoc kelas bagian
	 * 1). Memakai {@code split(",")} <b>tanpa batas</b> (bukan {@code split(",", n)} seperti
	 * kebanyakan method {@code retreiveAbsensi*} lain di kelas ini) — aman selama field indeks
	 * 5 (keterangan, satu-satunya field teks bebas) sudah disanitasi dari koma oleh
	 * {@link #populate}, tapi rawan {@code ArrayIndexOutOfBoundsException} bila field-field akhir
	 * record kosong (Java menghilangkan token kosong di ekor hasil {@code split} tanpa batas);
	 * exception semacam itu ditelan diam-diam oleh {@code catch} di bawah dan loop lanjut ke
	 * record berikutnya.
	 *
	 * @param ref kunci record (field indeks 0) yang dicari; bila {@code null}, method langsung
	 *            mengembalikan nilai default tanpa mencari.
	 * @return kode status pada record yang cocok, atau {@code "-"} bila {@code ref} {@code null},
	 *         tidak ditemukan, atau record yang cocok gagal di-parse.
	 */
	public String retreiveAbsensiKode(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:162");

				}
			}
		}

		return "-";
	}

	/**
	 * Sama seperti {@link #retreiveAbsensiKode(String)}, tetapi mengembalikan nama
	 * {@code Statusabsensi} (field indeks 3) dari record ber-{@code ref} yang cocok.
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return nama status pada record yang cocok, atau {@code "-"} bila tidak ditemukan/gagal
	 *         di-parse.
	 */
	public String retreiveAbsensiNama(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:183");

				}
			}
		}

		return "-";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan keterangan bebasnya (field indeks 5).
	 * Berbeda dari {@link #retreiveAbsensiKode(String)}/{@link #retreiveAbsensiNama(String)},
	 * method ini memakai {@code split(",", 9)} — <b>batas eksplisit</b> yang menjaga token ke-9
	 * dan seterusnya (indeks 8+, yaitu liburNasional dst.) tetap utuh dalam satu potongan
	 * terakhir sehingga tidak ikut terpecah, meski hanya field indeks 5 yang diambil di sini.
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return keterangan pada record yang cocok, atau string kosong bila tidak ditemukan/gagal
	 *         di-parse (berbeda dari {@code retreiveAbsensiKode}/{@code Nama} yang defaultnya
	 *         {@code "-"}).
	 */
	public String retreiveAbsensiKeterangan(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:204");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan ID {@code Statusabsensi}-nya (field
	 * indeks 1, di-parse dari {@link String} ke {@link Long}).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return ID status pada record yang cocok, atau {@code -1L} bila {@code ref} {@code null},
	 *         tidak ditemukan, atau field indeks 1 gagal di-parse sebagai angka (termasuk bila
	 *         kosong).
	 */
	public Long retreiveAbsensiId(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						Long id = Long.parseLong(s[1]);

						return id;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:227");

				}
			}
		}

		return -1L;
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan jam/waktu mulai-nya (field indeks 6).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return waktu mulai pada record yang cocok, atau string kosong bila tidak ditemukan/gagal
	 *         di-parse.
	 */
	public String retreiveAbsensiMulai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[6];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:249");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan jam/waktu sampai-nya (field indeks 7).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return waktu sampai pada record yang cocok, atau string kosong bila tidak ditemukan/gagal
	 *         di-parse.
	 */
	public String retreiveAbsensiSampai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[7];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:271");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan penanda libur nasional-nya (field
	 * indeks 8).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return penanda libur nasional pada record yang cocok, atau string kosong bila tidak
	 *         ditemukan/gagal di-parse.
	 */
	public String retreiveAbsensiLiburNasional(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[8];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:293");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan penanda libur rutin-nya (field indeks 9).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return penanda libur rutin pada record yang cocok, atau string kosong bila tidak
	 *         ditemukan/gagal di-parse.
	 */
	public String retreiveAbsensiLiburRutin(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 10);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[9];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:315");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan penanda cuti/izin-nya (field indeks 10).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return penanda cuti/izin pada record yang cocok, atau string kosong bila tidak
	 *         ditemukan/gagal di-parse.
	 */
	public String retreiveAbsensiCutiDanIzin(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 11);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[10];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:337");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan ID data sumbernya (field indeks 11).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return ID data sumber pada record yang cocok, atau string kosong bila tidak
	 *         ditemukan/gagal di-parse.
	 */
	public String retreiveAbsensiIdData(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 12);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[11];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:359");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan jam mulai "harus"/wajib-nya (field
	 * indeks 12).
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return jam mulai wajib pada record yang cocok, atau string kosong bila tidak
	 *         ditemukan/gagal di-parse.
	 */
	public String retreiveAbsensiMulaiHarus(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 13);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[12];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:381");

				}
			}
		}

		return "";
	}

	/**
	 * Mencari record ber-{@code ref} dan mengembalikan jam sampai "harus"/wajib-nya (field
	 * indeks 13, field terakhir dalam format). Menandai batas kanan pemakaian
	 * {@code split(",", 14)} — batas terbesar di antara seluruh method {@code retreiveAbsensi*}.
	 *
	 * @param ref kunci record yang dicari; {@code null} langsung mengembalikan nilai default.
	 * @return jam sampai wajib pada record yang cocok, atau string kosong bila tidak
	 *         ditemukan/gagal di-parse.
	 */
	public String retreiveAbsensiSampaiHarus(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 14);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {

						return s[13];

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/AbsenPegawaiDetail.java:403");

				}
			}
		}

		return "";
	}

	/**
	 * Menulis/memperbarui satu record harian ber-{@code ref} di dalam blob {@link #getAbsensi()},
	 * lalu menyimpan hasilnya ke field {@code absensi} (tanpa memanggil {@link #setAbsensi(String)}
	 * — lihat catatan di bagian akhir javadoc ini). Method inilah satu-satunya penulis format
	 * 14-field yang dijelaskan di javadoc kelas bagian 1.
	 *
	 * <p><b>Alur kerja:</b></p>
	 * <ol>
	 *   <li>Bila {@code statusabsensi.getKode()} bukan {@code "M"} (kode untuk status "Masuk"),
	 *       parameter {@code mulai} dan {@code sampai} dipaksa jadi string kosong — status
	 *       non-masuk (izin/cuti/libur/dsb.) tidak boleh membawa jam kerja.</li>
	 *   <li>{@code keterangan} disanitasi: {@code ;} diganti {@code "..\n"} dan {@code ,} diganti
	 *       {@code _}, supaya tidak merusak pemisah record/field pada penulisan ulang blob
	 *       lengkap di langkah berikutnya.</li>
	 *   <li>Seluruh record lama di-<i>parse</i> ulang satu per satu ({@code split(";")} lalu
	 *       {@code split(",")} per record, <b>tanpa batas</b> — punya kerapuhan yang sama seperti
	 *       {@link #retreiveAbsensiKode(String)}). Record kosong atau yang mengandung literal
	 *       {@code "Belum Ditentukan"} dilewati. Sebuah {@link HashSet} {@code udahAda} menjaga
	 *       agar setiap {@code formatId} (field indeks 0) hanya diproses <b>sekali</b> — bila blob
	 *       lama sudah punya {@code ref} duplikat (mis. dari data historis yang cacat), hanya
	 *       kemunculan pertama yang dipertahankan, sisanya dibuang diam-diam saat blob ditulis
	 *       ulang.</li>
	 *   <li>Untuk record ber-{@code formatId} yang sama dengan {@code ref}, seluruh 14 field
	 *       disusun ulang dari parameter method ini; untuk tiap parameter opsional yang
	 *       {@code null} ({@code keterangan}, {@code mulai}, {@code sampai}, {@code liburNasional},
	 *       {@code liburRutin}, {@code cutiDanIzin}, {@code idData}, {@code mulaiHarus},
	 *       {@code sampaiHarus}), nilai lama pada record itu <b>dipertahankan</b> lewat
	 *       method {@code retreiveAbsensi*} yang bersangkutan — jadi memanggil {@code populate}
	 *       dengan sebagian parameter {@code null} melakukan <i>partial update</i>, bukan
	 *       menghapus field yang tidak disebut. Record lain disalin apa adanya.</li>
	 *   <li>Bila {@code ref} <b>belum ada</b> di blob lama ({@code ada} tetap {@code false}
	 *       setelah loop), satu record baru ditambahkan di akhir blob dengan logika fallback
	 *       parameter {@code null} yang sama seperti langkah sebelumnya (yang pada kasus record
	 *       baru berarti memanggil {@code retreiveAbsensi*} pada blob yang belum punya {@code ref}
	 *       tersebut — sehingga fallback-nya efektif jadi nilai default masing-masing method,
	 *       mis. string kosong atau {@code -1L}).</li>
	 *   <li><b>Batas ukuran:</b> penggabungan ulang record lama ({@code nilais}) hanya dilakukan
	 *       bila jumlah record kurang dari 100.000; di atas itu, blob lama diperlakukan seolah
	 *       kosong untuk keperluan penggabungan (record lama <b>hilang</b> dari hasil akhir),
	 *       meski record baru untuk {@code ref} tetap ditambahkan lewat cabang {@code !ada}.
	 *       Batas ini murni penjaga performa/memori, bukan validasi bisnis.</li>
	 *   <li>Hasil akhir ditulis langsung ke field {@code absensi} — <b>bukan</b> lewat
	 *       {@link #setAbsensi(String)}, jadi mekanisme cache-berkas
	 *       {@link GeneralValueObject#put(String, String)} yang biasanya dipicu setter itu
	 *       <b>tidak</b> ikut terpanggil di sini.</li>
	 * </ol>
	 * <p>Kesalahan pem-parsing-an per-record ditangani individual: {@code catch (Exception e)}
	 * memanggil {@code Common.tampilErrorJikaAdmin(e)} (menampilkan pesan hanya untuk admin) lalu
	 * loop lanjut ke record berikutnya — satu record korup tidak menggagalkan seluruh operasi
	 * {@code populate}, tapi record yang gagal di-parse tersebut juga tidak ikut disalin ke blob
	 * baru (hilang secara diam-diam).
	 *
	 * @param ref kunci record yang ditulis/diperbarui; bila {@code null} (atau
	 *            {@code statusabsensi} {@code null}), method langsung tidak melakukan apa-apa.
	 * @param statusabsensi status kehadiran yang menyediakan ID/kode/nama untuk field indeks 1-3;
	 *                       wajib tidak {@code null} agar method bekerja.
	 * @param keterangan keterangan baru; {@code null} mempertahankan nilai lama record.
	 * @param mulai jam mulai baru; {@code null} mempertahankan nilai lama record; dipaksa kosong
	 *              bila kode status bukan {@code "M"}.
	 * @param sampai jam sampai baru; {@code null} mempertahankan nilai lama record; dipaksa kosong
	 *               bila kode status bukan {@code "M"}.
	 * @param mulaiHarus jam mulai wajib baru; {@code null} mempertahankan nilai lama record.
	 * @param sampaiHarus jam sampai wajib baru; {@code null} mempertahankan nilai lama record.
	 * @param liburNasional penanda libur nasional baru; {@code null} mempertahankan nilai lama.
	 * @param liburRutin penanda libur rutin baru; {@code null} mempertahankan nilai lama.
	 * @param cutiDanIzin penanda cuti/izin baru; {@code null} mempertahankan nilai lama.
	 * @param idData ID data sumber baru; {@code null} mempertahankan nilai lama.
	 */
	public void populate(String ref, Statusabsensi statusabsensi, String keterangan, String mulai, String sampai,
			String mulaiHarus, String sampaiHarus, Long liburNasional, Long liburRutin, Long cutiDanIzin, Long idData) {
		if (ref != null && statusabsensi != null) {

			if (statusabsensi.getKode() == null || !statusabsensi.getKode().equals("M")) {
				mulai = "";
				sampai = "";
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getAbsensi().split(";");
			Boolean ada = false;
			if (nilais.length < 100000) {
				Set<String> udahAda = new HashSet<String>();
				for (String nn : nilais) {
					try {
						if (!nn.isEmpty() && !nn.contains("Belum Ditentukan")) {

							String aformatBaru = "";
							String[] s = nn.split(",");
							if (!s[0].trim().isEmpty()) {
								String formatId = (s[0]);
								if (!udahAda.contains(formatId)) {

									if (ref.equals(formatId)) {

										udahAda.add(formatId);

										System.out.println("nn => " + nn);

										aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode()
												+ "," + statusabsensi.getNama() + ",0,"
												+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan)
												+ "," + (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
												+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
												+ (liburNasional == null ? retreiveAbsensiLiburNasional(ref)
														: liburNasional)
												+ ","
												+ (liburRutin == null ? retreiveAbsensiLiburRutin(ref) : liburRutin)
												+ ","
												+ (cutiDanIzin == null ? retreiveAbsensiCutiDanIzin(ref) : cutiDanIzin)
												+ "," + (idData == null ? retreiveAbsensiIdData(ref) : idData) + ","
												+ (mulaiHarus == null ? retreiveAbsensiMulaiHarus(ref) : mulaiHarus)
												+ ","
												+ (sampaiHarus == null ? retreiveAbsensiSampaiHarus(ref) : sampaiHarus);
										ada = true;
									} else {
										aformatBaru = nn;
									}
									if (!aformatBaru.trim().isEmpty()) {
										formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
									}

								}
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
						+ statusabsensi.getNama() + ",0,"
						+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
						+ (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
						+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
						+ (liburNasional == null ? retreiveAbsensiLiburNasional(ref) : liburNasional) + ","
						+ (liburRutin == null ? retreiveAbsensiLiburRutin(ref) : liburRutin) + ","
						+ (cutiDanIzin == null ? retreiveAbsensiCutiDanIzin(ref) : cutiDanIzin) + ","
						+ (idData == null ? retreiveAbsensiIdData(ref) : idData) + ","
						+ (mulaiHarus == null ? retreiveAbsensiMulaiHarus(ref) : mulaiHarus) + ","
						+ (sampaiHarus == null ? retreiveAbsensiSampaiHarus(ref) : sampaiHarus);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

//			System.out.println("formatBaru => " + formatBaru);

			absensi = formatBaru;

//			put(absensi, "absensi");
		}
	}

	/**
	 * Mengambil pegawai pemilik rekap absensi bulanan ini, sambil menyegarkan proxy Hibernate
	 * lewat {@code check(pegawai)} (pola standar relasi lazy di seluruh model AIS). Kolomnya
	 * {@code nullable = false}, tapi tidak ada validasi di kelas ini yang menegakkan itu sebelum
	 * penyimpanan — kegagalan baru muncul sebagai constraint violation dari database.
	 *
	 * @return pegawai pemilik rekap, seharusnya tidak pernah {@code null} pada baris yang valid.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel pegawai pemilik rekap absensi bulanan ini.
	 *
	 * @param pegawai pegawai baru.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengambil tahun rekap, dengan fallback ke tahun berjalan ({@code Calendar.YEAR} saat ini)
	 * bila field belum pernah diset. <b>Catatan:</b> getter ini menulis balik nilai fallback ke
	 * field {@code tahun} (pola yang sama seperti {@link JenisShiftPegawai#getJumlahShift()}),
	 * jadi baris baru yang belum di-set tahunnya secara eksplisit akan "terkunci" ke tahun saat
	 * getter ini pertama kali dipanggil.
	 *
	 * @return tahun rekap, tidak pernah {@code null}.
	 */
	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		if (tahun == null) {
			tahun = Calendar.getInstance().get(Calendar.YEAR);
		}
		return this.tahun;
	}

	/**
	 * Menyetel tahun rekap.
	 *
	 * @param tahun tahun baru.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Menyusun kunci unik gabungan pegawai/tahun/bulan tanpa memerlukan instance —
	 * {@code (id pegawai atau "0" bila null) + "_" + tahun + "_" + bulan}. Dipakai
	 * {@link #ambil(Pegawai, Integer, Integer, Session)} untuk mencari baris yang sudah ada
	 * sebelum membuat baris baru. Parameter {@code tahun}/{@code bulan} yang {@code null} akan
	 * ikut ter-<i>concat</i> apa adanya sebagai literal string {@code "null"} (tidak ada
	 * validasi/normalisasi di method ini).
	 *
	 * @param pegawai pegawai acuan; {@code null} memakai {@code "0"} sebagai pengganti ID.
	 * @param tahun tahun acuan.
	 * @param bulan bulan acuan.
	 * @return string kunci unik gabungan, tidak pernah {@code null}.
	 */
	public static String ambilKodeUnik(Pegawai pegawai, Integer tahun, Integer bulan) {
		return (pegawai == null ? "0" : pegawai.getId()) + "_" + tahun + "_" + bulan;
	}

	/**
	 * Mengambil kunci unik baris ini. <b>Getter destruktif:</b> setiap kali dipanggil, method ini
	 * (1) menulis balik field {@code pegawai} lewat {@link #getPegawai()} (menyegarkan proxy) dan
	 * (2) <b>menghitung ulang dari nol</b> lalu menimpa field {@code kodeUnik} — logika
	 * perhitungannya nyaris sama dengan {@link #ambilKodeUnik(Pegawai, Integer, Integer)}, tapi
	 * di sini memakai field {@code tahun}/{@code bulan} mentah (bukan {@link #getTahun()}/
	 * {@link #getBulan()}), sehingga <b>tidak</b> mendapat fallback tahun/bulan berjalan milik
	 * kedua getter tersebut; bila field {@code tahun}/{@code bulan} masih {@code null} saat
	 * method ini dipanggil, hasilnya memuat literal {@code "null"} secara harfiah di dalam
	 * string kunci. Nilai yang diset manual lewat {@link #setKodeUnik(String)} karenanya tidak
	 * pernah benar-benar dipakai — selalu ditimpa saat dibaca kembali dalam sesi Hibernate aktif,
	 * pola yang sama seperti getter destruktif berantai pada {@link JenisShiftPegawai}.
	 *
	 * @return kunci unik gabungan pegawai/tahun/bulan yang baru dihitung.
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		pegawai = getPegawai();
		kodeUnik = (pegawai == null ? "0" : pegawai.getId()) + "_" + tahun + "_" + bulan;
		return kodeUnik;
	}

	/**
	 * Menyetel kunci unik baris ini secara manual. Lihat catatan penting pada
	 * {@link #getKodeUnik()}: nilai yang diset di sini akan ditimpa lagi pada pemanggilan
	 * {@code getKodeUnik()} berikutnya dalam sesi Hibernate aktif.
	 *
	 * @param kodeUnik kunci unik baru.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengambil bulan rekap, dengan fallback ke bulan berjalan ({@code Calendar.MONTH} saat ini)
	 * bila field belum pernah diset, ditulis balik ke field seperti {@link #getTahun()}.
	 * <b>Catatan konsistensi:</b> {@code Calendar.MONTH} berbasis-0 (Januari = 0, Desember = 11),
	 * berbeda dari {@link #getTahun()} yang memakai {@code Calendar.YEAR} apa adanya (mis. 2026).
	 * Pemanggil yang menyimpan/membandingkan {@code bulan} harus konsisten memakai konvensi
	 * berbasis-0 ini — tidak ada normalisasi ke bulan berbasis-1 (1-12) di mana pun pada kelas
	 * ini.
	 *
	 * @return bulan rekap (berbasis-0, 0 = Januari), tidak pernah {@code null}.
	 */
	@Column(name = "bulan", nullable = false)
	public Integer getBulan() {
		if (bulan == null) {
			bulan = Calendar.getInstance().get(Calendar.MONTH);
		}
		return bulan;
	}

	/**
	 * Menyetel bulan rekap. Lihat catatan konvensi berbasis-0 pada {@link #getBulan()}.
	 *
	 * @param bulan bulan baru (konvensi pemanggil menentukan basis-0 atau basis-1 — kelas ini
	 *              tidak menormalisasi).
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

}
