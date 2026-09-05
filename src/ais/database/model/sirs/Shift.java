package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.Lokasi;

/**
 * Entitas katalog master shift kerja/jaga pada schema {@code sirs}
 * (tabel {@code shift}) — mendefinisikan rentang jam ({@link #getMulai()}
 * &ndash; {@link #getSampai()}) yang dipakai sebagai relasi
 * {@code ManyToOne} dari penjadwalan dokter/petugas (lihat pemakaian di
 * {@code JadwalDokterAction}, {@code CalendarJadwalDokterComposer},
 * {@code CalendarJadwalLokasiComposer}, {@code CalendarJadwalPolyComposer},
 * {@code CalendarJadwalUmumComposer}, serta berbagai Action transaksi
 * SIRS yang mencatat shift petugas saat transaksi terjadi, mis.
 * {@code TransaksiAction}, {@code PendaftaranRawatInapAction}).
 *
 * <p>
 * <b>Shift LINTAS TENGAH MALAM ditangani eksplisit.</b> Bila
 * {@link #getSampai()} lebih awal dari {@link #getMulai()} (mis. shift
 * malam 22:00&ndash;06:00), {@link #getJumlah()} menambahkan satu hari
 * ke waktu akhir sebelum menghitung selisih jam, dan
 * {@link #getSampaiD()} menambahkan 24.0 ke representasi desimalnya —
 * lihat javadoc masing-masing method untuk detail.
 * </p>
 *
 * <p>
 * PERHATIAN ARSITEKTUR: kelas ini memuat beberapa getter dengan EFEK
 * SAMPING TULIS-BALIK ke field instance — {@link #getMulai()},
 * {@link #getSampai()} (lazy-init ke {@code new Date()} bila
 * {@code null}, BUKAN {@code null} tetap), {@link #getJumlah()},
 * {@link #getMulaiD()}, {@link #getSampaiD()}, dan
 * {@link #getKeteranganLabel()} semuanya menghitung ulang dan menimpa
 * field aslinya setiap kali dipanggil, mengikuti pola getter destruktif
 * yang berulang di paket model AIS lain. Lihat javadoc masing-masing
 * getter untuk rincian efek sampingnya.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "shift")
public class Shift extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Membangun representasi ringkas shift ini untuk keperluan
	 * tampilan/log, berformat {@code "jenisShift - nama, mulai s.d
	 * sampai, jumlah jam"}.
	 *
	 * <p>
	 * GETTER DESTRUKTIF: baris pertama method ini memanggil
	 * {@link #getJenisShift()} dan MENIMPA field {@link #jenisShift}
	 * dengan hasil resolusi proxy-nya (efek samping dari
	 * {@code check(...)} di dalam {@link #getJenisShift()}), sebelum
	 * dipakai menyusun string. {@link #getJumlah()} yang dipanggil di
	 * dalamnya juga punya efek samping tulis-balik sendiri — lihat
	 * javadoc method tersebut.
	 * </p>
	 *
	 * @return string ringkasan shift, mis.
	 *         {@code "Malam - Shift 1, 22:00 s.d 06:00, 8 jam"}.
	 */
	public String toString() {
		jenisShift = getJenisShift();
		return jenisShift + " - " + nama + ", " + (mulai == null ? "" : Common.timeFormat.get().format(mulai)) + " s.d "
				+ (sampai == null ? "" : Common.timeFormat.get().format(sampai)) + ", "
				+ Common.numberFormat.get().format(getJumlah()) + " jam";
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private Date mulai;
	private Date sampai;
	private Lokasi lokasi;
	private String nama;
	private String keterangan;
	private String keteranganLabel;
	private JenisBiayaLain jenisShift;
	private Double jumlah;

	private Double mulaiD;
	private Double sampaiD;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public Shift() {
	}

	/**
	 * Primary key baris shift, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik shift ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID shift.
	 *
	 * @param id ID shift.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama shift (mis. "Pagi", "Siang", "Malam").
	 *
	 * @return nama shift.
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama shift.
	 *
	 * @param nama nama shift.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas shift ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas shift ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil jam mulai shift ini (hanya komponen waktu, lewat
	 * {@code @Temporal(TIME)}).
	 *
	 * <p>
	 * GETTER DENGAN LAZY-INIT TULIS-BALIK: bila {@link #mulai} bernilai
	 * {@code null}, method ini MENIMPA field dengan {@code new Date()}
	 * (waktu saat ini) alih-alih membiarkannya {@code null} — berbeda
	 * dari kebanyakan field opsional di katalog {@code sirs} lain, shift
	 * tanpa jam mulai eksplisit akan otomatis "dianggap mulai sekarang"
	 * begitu getter ini dipanggil sekali.
	 * </p>
	 *
	 * @return jam mulai shift; tidak pernah {@code null} setelah getter
	 *         ini dipanggil (lazy-init ke waktu saat ini bila kosong).
	 */
	@Temporal(TemporalType.TIME)
	public Date getMulai() {
		if (mulai == null) {
			mulai = new Date();
		}
		return mulai;
	}

	/**
	 * Menetapkan jam mulai shift ini.
	 *
	 * @param mulai jam mulai shift.
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengambil jam akhir shift ini (hanya komponen waktu). Sama seperti
	 * {@link #getMulai()}, {@code null} otomatis ditulis-balik jadi
	 * {@code new Date()} (waktu saat ini) lewat lazy-init.
	 *
	 * @return jam akhir shift; tidak pernah {@code null} setelah getter
	 *         ini dipanggil.
	 */
	@Temporal(TemporalType.TIME)
	public Date getSampai() {
		if (sampai == null) {
			sampai = new Date();
		}
		return sampai;
	}

	/**
	 * Menetapkan jam akhir shift ini.
	 *
	 * @param sampai jam akhir shift.
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengambil relasi lokasi tempat shift ini berlaku (opsional).
	 *
	 * @return lokasi terkait, atau {@code null} jika tidak dibatasi
	 *         lokasi tertentu.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menetapkan relasi lokasi tempat shift ini berlaku.
	 *
	 * @param lokasi lokasi terkait.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil relasi jenis shift ini, dipetakan lewat kolom
	 * {@code jenis_shift} ke entitas {@link JenisBiayaLain} — PERHATIKAN
	 * bahwa relasi ini menunjuk ke katalog "jenis biaya lain", bukan
	 * katalog "jenis shift" tersendiri; penamaan field/getter
	 * ({@code jenisShift}) tidak mencerminkan tipe target relasinya.
	 *
	 * @return jenis shift (sebagai {@link JenisBiayaLain}), atau
	 *         {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_shift", nullable = true)
	public JenisBiayaLain getJenisShift() {
		jenisShift = check(jenisShift);
		return jenisShift;
	}

	/**
	 * Menetapkan relasi jenis shift ini.
	 *
	 * @param jenisShift jenis shift (sebagai {@link JenisBiayaLain}).
	 */
	public void setJenisShift(JenisBiayaLain jenisShift) {
		this.jenisShift = jenisShift;
	}

	/**
	 * Menghitung durasi shift ini dalam jam, dari selisih
	 * {@link #getMulai()} ke {@link #getSampai()}.
	 *
	 * <p>
	 * GETTER DESTRUKTIF: hasil perhitungan SELALU MENIMPA field
	 * {@link #jumlah}, mengabaikan nilai apa pun yang pernah diset lewat
	 * {@link #setJumlah(Double)}. Perhitungan menyamakan tanggal
	 * mulai/akhir ke tanggal hari ini (hanya komponen jam yang relevan),
	 * lalu MENANGANI SHIFT LINTAS TENGAH MALAM: bila jam akhir lebih
	 * awal dari jam mulai (shift malam, mis. 22:00&ndash;06:00), satu
	 * hari ditambahkan ke waktu akhir sebelum menghitung selisih detik,
	 * memakai {@code org.joda.time.Seconds}. Bila salah satu dari
	 * {@link #getMulai()}/{@link #getSampai()} {@code null} (praktis
	 * tidak pernah terjadi karena keduanya sudah lazy-init sendiri),
	 * hasilnya {@code 0.0}.
	 * </p>
	 *
	 * @return durasi shift dalam jam (bisa pecahan, mis. {@code 7.5}).
	 */
	public Double getJumlah() {
		if (getMulai() != null && getSampai() != null) {
			Calendar now = Calendar.getInstance();

			Calendar start = Calendar.getInstance();
			start.setTime(getMulai());
			start.set(Calendar.YEAR, now.get(Calendar.YEAR));
			start.set(Calendar.MONTH, now.get(Calendar.MONTH));
			start.set(Calendar.DATE, now.get(Calendar.DATE));

			Calendar end = Calendar.getInstance();
			end.setTime(getSampai());
			end.set(Calendar.YEAR, now.get(Calendar.YEAR));
			end.set(Calendar.MONTH, now.get(Calendar.MONTH));
			end.set(Calendar.DATE, now.get(Calendar.DATE));

			DateTime startDateTime = new DateTime(start.getTime());
			DateTime endDateTime;
			if (end.before(start)) {
				endDateTime = new DateTime(end.getTime()).plusDays(1);
			} else {
				endDateTime = new DateTime(end.getTime());
			}

			Seconds hours = Seconds.secondsBetween(startDateTime, endDateTime);
			jumlah = hours.getSeconds() / 3600.0;
		} else {
			jumlah = 0.0;
		}
		return jumlah;
	}

	/**
	 * Menetapkan durasi shift ini secara eksplisit. Nilai ini akan
	 * DITIMPA lagi oleh {@link #getJumlah()} pada panggilan berikutnya
	 * — lihat javadoc getter.
	 *
	 * @param jumlah durasi shift dalam jam.
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengambil representasi desimal jam mulai shift ini (mis. jam 07:30
	 * menjadi {@code 7.5}), diformat lewat {@code Common.timeFormat2}.
	 *
	 * <p>
	 * GETTER DESTRUKTIF: hasil parsing SELALU MENIMPA field
	 * {@link #mulaiD}, mengabaikan nilai apa pun yang pernah diset lewat
	 * {@link #setMulaiD(Double)}. Nilai sumbernya diambil dari
	 * {@link #getMulai()}, yang sendiri sudah lazy-init ke waktu saat
	 * ini bila kosong.
	 * </p>
	 *
	 * @return representasi desimal jam mulai shift.
	 */
	public Double getMulaiD() {
		mulaiD = Double.parseDouble(Common.timeFormat2.get().format(getMulai()));
		return mulaiD;
	}

	/**
	 * Menetapkan representasi desimal jam mulai shift ini secara
	 * eksplisit. Nilai ini akan DITIMPA lagi oleh {@link #getMulaiD()}
	 * pada panggilan berikutnya — lihat javadoc getter.
	 *
	 * @param mulaiD representasi desimal jam mulai.
	 */
	public void setMulaiD(Double mulaiD) {
		this.mulaiD = mulaiD;
	}

	/**
	 * Mengambil representasi desimal jam akhir shift ini, diformat lewat
	 * {@code Common.timeFormat2}.
	 *
	 * <p>
	 * GETTER DESTRUKTIF DENGAN PENANGANAN LINTAS TENGAH MALAM: hasil
	 * SELALU MENIMPA field {@link #sampaiD}. Bila {@link #getMulai()}
	 * berada SETELAH {@link #getSampai()} (shift malam, mis.
	 * 22:00&ndash;06:00), nilai desimal jam akhir ditambah {@code 24.0}
	 * (mis. 06:00 menjadi {@code 30.0}) agar selisih terhadap jam mulai
	 * tetap positif saat dipakai perhitungan lain yang membandingkan
	 * kedua nilai desimal ini secara langsung — konsisten dengan
	 * penanganan lintas tengah malam di {@link #getJumlah()}.
	 * </p>
	 *
	 * @return representasi desimal jam akhir shift; ditambah 24.0 bila
	 *         shift melewati tengah malam.
	 */
	public Double getSampaiD() {
		if (getMulai().after(getSampai())) {
			sampaiD = Double.parseDouble(Common.timeFormat2.get().format(getSampai())) + 24.0;
		} else {
			sampaiD = Double.parseDouble(Common.timeFormat2.get().format(getSampai()));
		}
		return sampaiD;
	}

	/**
	 * Menetapkan representasi desimal jam akhir shift ini secara
	 * eksplisit. Nilai ini akan DITIMPA lagi oleh {@link #getSampaiD()}
	 * pada panggilan berikutnya — lihat javadoc getter.
	 *
	 * @param sampaiD representasi desimal jam akhir.
	 */
	public void setSampaiD(Double sampaiD) {
		this.sampaiD = sampaiD;
	}

	/**
	 * Mengambil label keterangan ringkas shift ini.
	 *
	 * <p>
	 * GETTER DESTRUKTIF: method ini MEMANGGIL {@link #toString()} (yang
	 * sendiri punya efek samping tulis-balik pada {@link #jenisShift})
	 * dan MENIMPA field {@link #keteranganLabel} dengan hasilnya setiap
	 * kali dipanggil, mengabaikan nilai apa pun yang pernah diset lewat
	 * {@link #setKeteranganLabel(String)}.
	 * </p>
	 *
	 * @return label keterangan shift, sama dengan {@link #toString()}.
	 */
	public String getKeteranganLabel() {
		keteranganLabel = toString();
		return keteranganLabel;
	}

	/**
	 * Menetapkan label keterangan shift ini secara eksplisit. Nilai ini
	 * akan DITIMPA lagi oleh {@link #getKeteranganLabel()} pada
	 * panggilan berikutnya — lihat javadoc getter.
	 *
	 * @param keteranganLabel teks label keterangan.
	 */
	public void setKeteranganLabel(String keteranganLabel) {
		this.keteranganLabel = keteranganLabel;
	}

}
