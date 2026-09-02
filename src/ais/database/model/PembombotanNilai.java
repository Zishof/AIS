package ais.database.model;

// Generated Apr 15, 2010 3:45:21 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.obe.CapaianPembelajaranLulusan;

/**
 * Master <b>format (skema) pembobotan nilai</b> perkuliahan. Satu baris tabel
 * {@code public.pembombotan_nilai} mendefinisikan berapa persen kontribusi tiap komponen
 * penilaian &mdash; absensi, tugas, quiz, UTS, UAS, atau nilai per dosen penguji &mdash;
 * terhadap nilai akhir mahasiswa, berikut label tampilan tiap komponen ("Tugas 1", "Quiz 3",
 * dan seterusnya) yang bisa diubah tiap institusi.
 *
 * <h3>PERINGATAN: nama kelas mengandung TYPO HISTORIS</h3>
 * Ejaan yang benar dalam Bahasa Indonesia adalah <i>pembobotan</i>, bukan
 * <i>"pembombotan"</i>. Typo ini sudah ada sejak kelas dibangkitkan {@code hbm2java}
 * (Apr 2010) dan <b>ikut menjadi nama tabel</b> ({@code pembombotan_nilai}), nama kolom
 * foreign key pada {@link Perkuliahan}, nama properti Hibernate, sampai nama tabel audit
 * Envers. Karena itu <b>nama kelas ini SENGAJA dipertahankan apa adanya</b> &mdash; jangan
 * "memperbaiki"-nya, sebab perubahan nama akan memutus pemetaan Hibernate, query HQL,
 * dan riwayat Envers yang sudah ada di produksi.
 *
 * <p>Perhatikan asimetrinya: layar pengelola master ini,
 * {@code ais.action.master.PembobotanNilaiAction}, justru dieja <b>BENAR</b>
 * ("Pembobotan"). Jadi saat mencari kode terkait fitur ini, cari <b>dua</b> ejaan.</p>
 *
 * <h3>Ini master yang dipakai ulang, bukan milik satu Perkuliahan</h3>
 * Berbeda dari kesan namanya, entity ini <b>bukan</b> anak dari satu kelas perkuliahan.
 * Ia adalah baris master yang dipilih/dipakai ulang oleh banyak
 * {@link Perkuliahan} lewat relasi {@code Perkuliahan.pembombotanNilai}
 * ({@link Perkuliahan#getPembombotanNilai()}). Konsekuensi penting:
 * <ul>
 *   <li>mengubah satu baris {@code PembombotanNilai} berdampak ke <b>semua</b> kelas yang
 *   menunjuk baris itu &mdash; kecuali kelas yang sudah dikunci, karena
 *   {@link Perkuliahan#getPembombotanNilaiBackup()} membekukan salinan skema saat penguncian;</li>
 *   <li>seluruh baris di-cache global lewat
 *   {@code ConstantValues.ambilBerdasarClass(PembombotanNilai.class)}, dan
 *   {@code ConstantValues.DEFAULT_PEMBOBOTAN_NILAI} adalah baris cadangan yang dipakai
 *   {@link Perkuliahan} bila kelas belum memilih format apa pun. Baris default itu
 *   dibuat otomatis oleh {@code InitDataHelper} dengan komposisi Tugas 20% / UTS 30% /
 *   UAS 50% bila belum ada baris ber-{@link #getDefaultPembobotan()} {@code true};</li>
 *   <li>{@link #getDimilikiOleh()} mencatat {@link Dosen} pembuat format &mdash; dosen bisa
 *   membuat format sendiri lewat layar Pembobotan Nilai, tidak hanya memakai format kampus;</li>
 *   <li>{@link #getWajibDitahunAkademikDanSemesterTertentu()} menjadikan sebuah format
 *   <b>dipaksakan</b> ke semua kelas pada tahun akademik + semester tertentu, mengalahkan
 *   pilihan per-kelas (lihat percabangan di {@link Perkuliahan#getPembombotanNilai()}).</li>
 * </ul>
 *
 * <h3>Dua "dunia" komponen nilai</h3>
 * <ol>
 *   <li><b>Konvensional</b> &mdash; komponen tetap: {@link #getAbsen() absen},
 *   {@link #getForm() form}, {@link #getTugas1() tugas1..tugas5},
 *   {@link #getQuiz1() quiz1..quiz5}, {@link #getUts() uts}, {@link #getUas() uas}.
 *   Nilainya berupa persentase ({@code Double}, satuan persen, bukan pecahan 0..1).</li>
 *   <li><b>OBE (Outcome Based Education)</b> &mdash; komponen dibangkitkan dari CPMK/Sub-CPMK
 *   kurikulum, bukan dari kolom-kolom di kelas ini. Lihat
 *   {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)}; kolom konvensional di sini
 *   <b>diabaikan total</b> untuk kelas OBE.</li>
 * </ol>
 *
 * <p>Kolom {@link #getDosen1() dosen1..dosen5} adalah dunia ketiga yang setengah jadi: dipakai
 * untuk pembobotan nilai penguji pada jenis "Tugas Akhir"/"KKN"/"PKL"
 * (lihat {@link #keterhubungan}), tetapi
 * {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)} <b>tidak pernah</b> menerbitkan
 * baris {@link FormatNilai} untuk kolom-kolom itu. Lihat catatan pada
 * {@link #getDosen1()}.</p>
 *
 * <h3>Hubungan dengan FormatNilai</h3>
 * {@code PembombotanNilai} hanyalah <i>cetakan</i>. Yang benar-benar dipakai mesin penilaian
 * adalah baris {@link FormatNilai} per {@link Perkuliahan} (satu baris per komponen, dengan
 * kolom {@code persen} dan relasi ke {@link StatusPertemuan}). Method statis
 * {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)} adalah jembatan yang
 * "mencetak" skema ini menjadi baris-baris {@code FormatNilai}, sedangkan
 * {@link #getNomorUrutFormat()} menyimpan urutan tampil komponen dalam bentuk JSON
 * (kunci = id {@link StatusPertemuan}) yang dibaca balik oleh
 * {@link FormatNilai#getNomorUrut()}.
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Audit</b> (menimpa/menutupi milik {@link GeneralValueObject}):
 *   {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; tampilan</b>: {@link #getId()}, {@link #getNama()}, {@link #toString()},
 *   {@link #getKeterangan()}.</li>
 *   <li><b>Bobot komponen</b> (persentase): {@code getAbsen/getForm/getTugas1..5/getQuiz1..5/getUts/getUas}
 *   dan {@code getDosen1..5}.</li>
 *   <li><b>Label komponen</b>: {@code getAbsenLabel/getFormLabel/getUtsLabel/getUasLabel/
 *   getTugas1..5Label/getQuiz1..5Label} &mdash; semuanya getter berefek samping yang mengisi
 *   nilai default bila masih {@code null}.</li>
 *   <li><b>Cakupan pemakaian</b>: {@link #getAktif()}, {@link #getDefaultPembobotan()},
 *   {@link #getDimilikiOleh()}, {@link #getJenisPembobotan()},
 *   {@link #getWajibDitahunAkademikDanSemesterTertentu()}, {@link #getTahunAkadmeik()},
 *   {@link #getSemester()}.</li>
 *   <li><b>Mesin</b> (statis, menyentuh DB): {@link #tampilkanFormat(Perkuliahan)} dan
 *   {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)}.</li>
 * </ul>
 *
 * <h3>Kuirk yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ol>
 *   <li><b>Tidak ada validasi "total harus 100%" di entity ini.</b> Aturan itu hidup di lapisan
 *   UI ({@code PembobotanNilaiAction} menolak simpan bila jumlah &ne; 100). Data yang masuk
 *   lewat jalur lain (import, skrip, DAO langsung) bisa saja berjumlah bukan 100%.</li>
 *   <li><b>{@link #getNama()} adalah getter yang MENULIS.</b> Ia membangun ulang string nama dari
 *   seluruh komponen dan membuang apa pun yang pernah disetel {@link #setNama(String)}. Karena
 *   pemetaan Hibernate kelas ini memakai <i>property access</i> (anotasi menempel di getter),
 *   string hasil bangunan itu ikut tersimpan ke kolom {@code nama} pada flush berikutnya.</li>
 *   <li><b>Nyaris SEMUA getter di kelas ini mengisi field bila masih {@code null}</b>
 *   (lazy default). Untuk entity terkelola, ini berarti sekadar <i>membaca</i> object bisa
 *   membuatnya "kotor" dan memicu {@code UPDATE} saat flush.</li>
 *   <li><b>{@link #getUts()} dan {@link #getUas()} bisa MENULIS BARIS KONFIGURASI BARU KE DB.</b>
 *   Keduanya membaca {@code Common.getKonfigurasi("default_prentasi_uts"/"default_prentasi_uas", "0")},
 *   dan helper itu menuliskan baris default ke tabel konfigurasi bila kuncinya belum ada.
 *   (Perhatikan nama kunci pun typo: {@code prentasi}, bukan {@code persentasi}.)</li>
 *   <li><b>Mengurutkan koleksi entity ini punya efek samping.</b>
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} memanggil {@code getNama()}, yang di
 *   sini berarti membangun ulang nama <i>dan</i> memicu semua efek samping butir 2&ndash;4 pada
 *   setiap elemen yang dibandingkan.</li>
 *   <li><b>Nama {@code setDefaultPembobotan} dipakai dua kali dengan arti berbeda jauh</b>:
 *   {@link #setDefaultPembobotan(Boolean)} hanyalah setter flag biasa, sedangkan
 *   {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)} adalah method statis besar yang
 *   membuat/mengubah/menyimpan baris {@link FormatNilai} di database.</li>
 *   <li><b>Semua state {@link GeneralValueObject} yang dipakai kelas ini di-<i>shadow</i></b>:
 *   {@code id}, {@code nama}, {@code keterangan}, {@code oleh}, {@code olehId}, dan
 *   {@code tanggal_dirubah} dideklarasikan ulang sebagai field privat di sini, lengkap dengan
 *   accessor-nya. Field milik induk untuk properti tersebut karenanya <b>selalu tetap
 *   {@code null}</b>. Ini pola yang sama di seluruh entity {@code ais.database.model}.</li>
 * </ol>
 *
 * <p>Kelas ini {@code @Audited} (Hibernate Envers) dengan {@code dynamicInsert}/{@code dynamicUpdate}
 * aktif, sehingga setiap perubahan bobot terekam pada tabel revisi.</p>
 *
 * <p>Dibangkitkan awalnya oleh {@code hbm2java} (Hibernate Tools 3.2.4.CR1), lalu diperkaya manual.</p>
 *
 * @see Perkuliahan#getPembombotanNilai()
 * @see FormatNilai
 * @see ais.action.master.PembobotanNilaiAction
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pembombotan_nilai")
public class PembombotanNilai extends GeneralValueObject {


	/** Kondisi bisnis yang wajar ketika rancangan OBE belum siap diterbitkan sebagai FormatNilai. */
	private static final class FormatPenggantiBelumSiapException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		private FormatPenggantiBelumSiapException(String message) {
			super(message);
		}
	}

	/** Mengambil kembali format produksi lama tanpa mengubah bobot atau state Hibernate-nya. */
	private static List<FormatNilai> ambilFormatLamaAktif(List<FormatNilai> formatNilais,
			Map<Long, Double> persenFormatNilaiLama) {
		List<FormatNilai> hasil = new ArrayList<FormatNilai>();
		for (FormatNilai formatNilai : formatNilais) {
			Double persen = formatNilai == null || formatNilai.getId() == null ? null
					: persenFormatNilaiLama.get(formatNilai.getId());
			if (persen != null && persen.doubleValue() > 0.01) {
				hasil.add(formatNilai);
			}
		}
		return hasil;
	}

	/**
	 * Memeriksa sumber CPMK/Sub-CPMK sebelum satu pun FormatNilai ditulis. Nilai {@code null}
	 * berarti sumber siap; selain itu berisi alasan mengapa format lama harus dipertahankan.
	 */
	private static String analisisKesiapanSumberObe(KurikulumPunyaMatakuliah kpm,
			List<CapaianPembelajaranLulusan> capaianPembelajaranLulusans) {
		boolean gunakanCpmk = kpm != null && kpm.getNilaiMenggunakanCpmk();
		int jumlahKomponen = 0;
		int jumlahCpmkTanpaKode = 0;
		double totalBobot = 0.0;

		for (CapaianPembelajaranLulusan cpl : capaianPembelajaranLulusans) {
			if (cpl == null) {
				continue;
			}
			JSONArray formula = null;
			boolean adaSubCpmk = false;
			try {
				formula = new JSONArray(cpl.getFormula() == null ? "[]" : cpl.getFormula());
				for (int i = 0; i < formula.length(); i++) {
					if (!formula.getJSONObject(i).isNull("key")) {
						adaSubCpmk = true;
						break;
					}
				}
			} catch (Exception formulaTidakValid) {
				// Samakan dengan perilaku pembangun lama: formula rusak dianggap belum punya Sub-CPMK,
				// sehingga CPMK induk masih dapat dipakai bila kode dan bobotnya lengkap.
				formula = null;
				adaSubCpmk = false;
			}

			if (gunakanCpmk || !adaSubCpmk) {
				String kode = cpl.getKode() == null ? "" : cpl.getKode().trim();
				if (kode.length() == 0) {
					jumlahCpmkTanpaKode++;
					continue;
				}
				double bobot = cpl.getBobot() == null ? 0.0 : cpl.getBobot().doubleValue();
				if (bobot > 0.01) {
					jumlahKomponen++;
					totalBobot += bobot;
				}
			} else if (formula != null) {
				for (int i = 0; i < formula.length(); i++) {
					JSONObject subCpmk = formula.getJSONObject(i);
					if (subCpmk.isNull("key")) {
						continue;
					}
					double bobot = 0.0;
					if (!subCpmk.isNull("bobot")) {
						try {
							bobot = Double.parseDouble(subCpmk.get("bobot") + "");
						} catch (Exception bobotTidakValid) {
							bobot = 0.0;
						}
					}
					if (bobot > 0.01) {
						jumlahKomponen++;
						totalBobot += bobot;
					}
				}
			}
		}

		if (jumlahKomponen == 0 || totalBobot < 99.0 || totalBobot > 101.0 || jumlahCpmkTanpaKode > 0) {
			return "Rancangan OBE belum siap: komponen berbobot=" + jumlahKomponen + ", total bobot="
					+ totalBobot + "%, CPMK tanpa kode=" + jumlahCpmkTanpaKode
					+ ". Format lama tetap dipakai sampai total bobot mendekati 100% dan setiap CPMK memiliki kode.";
		}
		return null;
	}

	/** Menjelaskan hasil FormatNilai yang gagal validasi setelah seluruh relasi diperiksa. */
	private static String ringkasanFormatTidakSiap(List<FormatNilai> formatNilais) {
		int aktif = 0;
		int tanpaStatus = 0;
		int statusTidakAktif = 0;
		int bobotKosong = 0;
		double totalBobot = 0.0;
		for (FormatNilai formatNilai : formatNilais) {
			if (formatNilai == null || formatNilai.getStatusPertemuan() == null) {
				tanpaStatus++;
				continue;
			}
			if (!formatNilai.getStatusPertemuan().getAktif()) {
				statusTidakAktif++;
				continue;
			}
			Double persen = formatNilai.getPersen();
			if (persen == null || persen.doubleValue() <= 0.01) {
				bobotKosong++;
				continue;
			}
			aktif++;
			totalBobot += persen.doubleValue();
		}
		return "Format pengganti belum siap: komponen layak=" + aktif + ", total bobot aktif=" + totalBobot
				+ "%, tanpa status=" + tanpaStatus + ", status tidak aktif=" + statusTidakAktif
				+ ", bobot kosong/nol=" + bobotKosong + ".";
	}

	/**
	 * Jenis pembobotan untuk kelas kuliah biasa (tatap muka/daring reguler). Nilai inilah yang
	 * dianggap default oleh layar pengelola bila {@link #getJenisPembobotan()} masih {@code null}.
	 * Pada jenis ini komponen yang boleh diisi adalah {@code form}, {@code uts}, dan {@code uas}
	 * (lihat {@link #keterhubungan}).
	 */
	public static final String JENIS_PERKULIAHAN_BELAJAR_MENGAJAR = "Perkuliahan Belajar Mengajar";

	/**
	 * Jenis pembobotan untuk mata kuliah tugas akhir/skripsi: bobot dibagi ke lima slot dosen
	 * penguji/pembimbing ({@code dosen1}..{@code dosen5}), bukan ke UTS/UAS.
	 *
	 * @see #keterhubungan
	 * @see Skripsi
	 */
	public static final String JENIS_PERKULIAHAN_TUGAS_AKHIR = "Tugas Akhir";

	/**
	 * Jenis pembobotan untuk Kuliah Kerja Nyata: hanya dua slot penilai
	 * ({@code dosen1}, {@code dosen2}).
	 *
	 * @see #keterhubungan
	 */
	public static final String JENIS_PERKULIAHAN_KKN = "KKN";

	/**
	 * Jenis pembobotan untuk Praktik Kerja Lapangan: sama seperti KKN, dua slot penilai
	 * ({@code dosen1}, {@code dosen2}) &mdash; biasanya pembimbing lapangan dan pembimbing kampus.
	 *
	 * @see #keterhubungan
	 */
	public static final String JENIS_PERKULIAHAN_PKL = "PKL";


	/**
	 * Peta <b>jenis pembobotan &rarr; daftar nama komponen yang boleh diisi</b>, diisi sekali oleh
	 * blok inisialisasi statis di bawahnya.
	 *
	 * <p>Dipakai oleh {@code PembobotanNilaiAction} untuk meng-<i>enable</i>/<i>disable</i> kotak
	 * isian pada layar: begitu combo "Jenis Pembobotan" berubah, kotak {@code uts}, {@code uas},
	 * {@code form}, dan {@code dosen1}..{@code dosen5} dinyalakan hanya bila namanya ada di daftar
	 * jenis terpilih.</p>
	 *
	 * <p><b>Catatan/kuirk:</b></p>
	 * <ul>
	 *   <li>Map ini {@code public static final} tetapi <b>isinya mutable</b> dan tidak dibungkus
	 *   {@code unmodifiableMap} &mdash; siapa pun bisa mengubah aturan enable/disable secara global
	 *   pada runtime. Jangan menambah/menghapus entri dari kode lain.</li>
	 *   <li>Daftar untuk jenis "Perkuliahan Belajar Mengajar" hanya memuat {@code form}, {@code uts},
	 *   {@code uas} &mdash; komponen {@code absen}, {@code tugas1..5}, dan {@code quiz1..5} TIDAK
	 *   terdaftar, sehingga aturan enable/disable ini tidak menyentuhnya (kotak-kotak itu diatur
	 *   terpisah lewat flag aktif {@link StatusPertemuan} masing-masing).</li>
	 *   <li>Pemanggilnya melakukan {@code get(...)} tanpa memeriksa {@code null}; jenis pembobotan
	 *   di luar keempat konstanta di atas akan berujung {@code NullPointerException}.</li>
	 * </ul>
	 */
	public static final Map<String, List<String>> keterhubungan = new HashMap<String, List<String>>();


	/**
	 * Blok inisialisasi statis yang mengisi {@link #keterhubungan} untuk keempat jenis pembobotan.
	 *
	 * <p>Variabel {@code list} sengaja dipakai ulang: setiap kelompok membuat {@code ArrayList}
	 * <b>baru</b> sebelum diisi, jadi tidak ada dua jenis yang berbagi instance daftar yang sama.</p>
	 */
	static {
		List<String> list = new ArrayList<String>();
		list.add("form");
		list.add("uts");
		list.add("uas");
		keterhubungan.put(JENIS_PERKULIAHAN_BELAJAR_MENGAJAR, list);
		list = new ArrayList<String>();
		list.add("dosen1");
		list.add("dosen2");
		list.add("dosen3");
		list.add("dosen4");
		list.add("dosen5");
		keterhubungan.put(JENIS_PERKULIAHAN_TUGAS_AKHIR, list);
		list = new ArrayList<String>();
		list.add("dosen1");
		list.add("dosen2");
		keterhubungan.put(JENIS_PERKULIAHAN_KKN, list);
		list = new ArrayList<String>();
		list.add("dosen1");
		list.add("dosen2");
		keterhubungan.put(JENIS_PERKULIAHAN_PKL, list);
	}

	/**
	 * 
	 */

	/**
	 * Versi serialisasi. Nilai tetap; ubah hanya bila bentuk serialisasi entity ini memang
	 * sengaja dibuat tidak kompatibel dengan data lama.
	 */
	private static final long serialVersionUID = 4888138074617466176L;
	private Long id;
	private String oleh;
	private String olehId;


	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris pembobotan ini.
	 *
	 * <p><b>Pola shadow:</b> field {@code olehId} dideklarasikan ulang di kelas ini padahal
	 * {@link GeneralValueObject} sudah punya field bernama sama. Accessor ini bekerja pada
	 * salinan milik kelas ini; salinan milik induk tidak pernah terisi. Pola yang sama berlaku
	 * untuk {@code oleh}, {@code tanggal_dirubah}, {@code id}, {@code nama}, dan
	 * {@code keterangan}.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}


	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau kosong/spasi
	 * <b>diabaikan diam-diam</b> sehingga jejak audit yang sudah terisi tidak bisa terhapus oleh
	 * jalur simpan yang kebetulan berjalan tanpa sesi login (batch, penjadwal).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 * @see GeneralValueObject#setOlehId(String)
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}


	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan yang sama seperti
	 * {@link #setOlehId(String)}: {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 * @see GeneralValueObject#setOleh(String)
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}


	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris pembobotan ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see GeneralValueObject#getOleh()
	 */
	public String getOleh() {
		return oleh;
	}


	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan stempel audit lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate menerbitkan
	 * {@code UPDATE} untuk baris ini.
	 *
	 * <p><b>Perhatikan bentuk barisnya:</b> deklarasi method ini dan deklarasi field
	 * {@code tanggal_dirubah} ditulis pada <b>satu baris fisik yang sama</b> (hasil penyisipan
	 * otomatis lintas-entity). Field {@code tanggal_dirubah} yang menyusul di baris itu
	 * meng-<i>shadow</i> field bernama sama milik {@link GeneralValueObject} dan diinisialisasi
	 * ke waktu pembuatan object memakai jam server aplikasi ({@code WaktuUtil.getDate()}),
	 * bukan jam database.</p>
	 *
	 * <p>Karena hanya {@code @PreUpdate} (tanpa {@code @PrePersist}), penyegaran otomatis hanya
	 * terjadi pada perubahan, bukan pada penyimpanan pertama &mdash; untuk baris baru, nilai
	 * awal field itulah yang tersimpan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();


	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 * @see #onUpdate()
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}


	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}
	 * sehingga bagian jam ikut tersimpan.
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} karena field-nya
	 *         diinisialisasi saat object dibuat
	 * @see #onUpdate()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}


	/**
	 * Representasi teks berupa {@code "<id>-<nama>"}, dengan bagian nama diambil dari
	 * {@link #getNama()}.
	 *
	 * <p><b>Hati-hati:</b> karena {@link #getNama()} membangun ulang nama dan menulis balik ke
	 * field (lihat penjelasan di sana), memanggil {@code toString()} &mdash; termasuk secara tidak
	 * sengaja lewat log, debugger, atau konkatenasi string &mdash; ikut memicu seluruh efek samping
	 * getter tersebut, sampai kemungkinan penulisan baris konfigurasi default ke database melalui
	 * {@link #getUts()}/{@link #getUas()}.</p>
	 *
	 * @return {@code id} diikuti tanda hubung dan ringkasan komposisi bobot
	 */
	public String toString() {
		return id + "-" + getNama();
	}

	private String nama;
	private String keterangan;


	/**
	 * Membangun <b>ringkasan komposisi bobot</b> sebagai satu baris teks, mis.
	 * {@code " Absensi - 10.0% Tugas - 20.0% UTS - 30.0% UAS - 40.0%"}.
	 *
	 * <p>Setiap komponen yang bobotnya &ge; 0,01 disisipkan dalam bentuk
	 * {@code "<label> - <bobot>%"} memakai label dari
	 * {@link #getAbsenLabel()}, {@link #getFormLabel()}, {@code getTugasNLabel()},
	 * {@code getQuizNLabel()}, {@link #getUtsLabel()}, dan {@link #getUasLabel()}. Urutannya
	 * tetap: absen, form, tugas1..5, quiz1..5, UTS, UAS. Komponen
	 * {@link #getDosen1() dosen1..dosen5} <b>tidak pernah muncul</b> di ringkasan ini, walaupun
	 * ikut divalidasi ke total 100% oleh layar pengelola.</p>
	 *
	 * <p><b>Ini getter yang MENULIS, dan menimpa {@link #setNama(String)}.</b> Baris pertamanya
	 * mengosongkan field {@code nama} lalu menyusunnya ulang, sehingga nilai apa pun yang pernah
	 * disetel lewat {@link #setNama(String)} hilang pada pembacaan pertama. Karena pemetaan
	 * Hibernate kelas ini memakai <i>property access</i> dan properti {@code nama} tidak ditandai
	 * {@code @Transient}, string hasil bangunan itu menjadi nilai yang tersimpan ke kolom
	 * {@code nama} pada flush berikutnya &mdash; membaca nama bisa membuat entity kotor dan
	 * memicu {@code UPDATE}.</p>
	 *
	 * <p><b>Efek samping berantai:</b> {@link #getUts()} dan {@link #getUas()} yang dipanggil di
	 * sini dapat menuliskan baris konfigurasi default ke database bila kunci
	 * {@code default_prentasi_uts}/{@code default_prentasi_uas} belum ada; getter label yang
	 * dipanggil juga mengisi label default bila masih {@code null}. Dan karena
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} memakai {@code getNama()},
	 * sekadar <i>mengurutkan</i> daftar {@code PembombotanNilai} memicu semua efek itu untuk tiap
	 * elemen.</p>
	 *
	 * <p><b>Kuirk kecil lain:</b></p>
	 * <ul>
	 *   <li>Setiap cabang ditulis dengan badan {@code if} kosong dan logika di {@code else}
	 *   ({@code if (getAbsen() < 0.01) { } else { ... }}) &mdash; bentuk terbalik yang membingungkan
	 *   tapi setara dengan {@code if (bobot >= 0.01)}.</li>
	 *   <li>Pemeriksaan {@code getAbsen() == null ? "0" : ...} di dalam cabang tidak pernah
	 *   bernilai benar: getter bobot tidak pernah mengembalikan {@code null}, dan seandainya bisa,
	 *   perbandingan {@code < 0.01} di atasnya sudah lebih dulu melempar
	 *   {@code NullPointerException} saat unboxing.</li>
	 *   <li>Ambang di sini {@code 0.01}, sedangkan
	 *   {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)} memakai ambang {@code 0.1}
	 *   untuk memutuskan komponen mana yang diterbitkan sebagai {@link FormatNilai} &mdash; bobot
	 *   antara 0,01 dan 0,1 akan <b>tampil di nama tapi tidak menghasilkan komponen nilai</b>.</li>
	 *   <li>Label default {@link #getFormLabel()} adalah {@code "Tugas"}, padahal ada pula
	 *   {@code tugas1..tugas5} berlabel {@code "Tugas 1".."Tugas 5"}; keduanya bisa muncul
	 *   bersamaan dan terlihat mirip di layar.</li>
	 *   <li>Hasilnya selalu diawali satu spasi bila ada minimal satu komponen, dan berupa string
	 *   kosong bila semua bobot nol.</li>
	 * </ul>
	 *
	 * @return ringkasan komposisi bobot; {@code ""} bila tidak ada komponen berbobot
	 * @see #setNama(String)
	 * @see GeneralValueObject#getNama()
	 */
	public String getNama() {
		nama = "";
		if (getAbsen() < 0.01) {

		} else {
			nama += " " + (getAbsenLabel() + " - " + (getAbsen() == null ? "0" : getAbsen().toString() + "%"));
		}

		if (getForm() < 0.01) {

		} else {
			nama += " " + (getFormLabel() + " - " + (getForm() == null ? "0" : getForm().toString() + "%"));
		}
		if (getTugas1() < 0.01) {

		} else {
			nama += " " + (getTugas1Label() + " - " + (getTugas1().toString() + "%"));
		}
		if (getTugas2() < 0.01) {

		} else {
			nama += " " + (getTugas2Label() + " - " + (getTugas2().toString() + "%"));
		}
		if (getTugas3() < 0.01) {

		} else {
			nama += " " + (getTugas3Label() + " - " + (getTugas3().toString() + "%"));
		}
		if (getTugas4() < 0.01) {

		} else {
			nama += " " + (getTugas4Label() + " - " + (getTugas4().toString() + "%"));
		}
		if (getTugas5() < 0.01) {

		} else {
			nama += " " + (getTugas5Label() + " - " + (getTugas5().toString() + "%"));
		}

		if (getQuiz1() < 0.01) {

		} else {
			nama += " " + (getQuiz1Label() + " - " + (getQuiz1().toString() + "%"));
		}
		if (getQuiz2() < 0.01) {

		} else {
			nama += " " + (getQuiz2Label() + " - " + (getQuiz2().toString() + "%"));
		}

		if (getQuiz3() < 0.01) {

		} else {
			nama += " " + (getQuiz3Label() + " - " + (getQuiz3().toString() + "%"));
		}

		if (getQuiz4() < 0.01) {

		} else {
			nama += " " + (getQuiz4Label() + " - " + (getQuiz4().toString() + "%"));
		}
		if (getQuiz5() < 0.01) {

		} else {
			nama += " " + (getQuiz5Label() + " - " + (getQuiz5().toString() + "%"));
		}

		if (getUts() < 0.01) {

		} else {
			nama += " " + (getUtsLabel() + " - " + (getUts() == null ? "0" : getUts().toString() + "%"));
		}

		if (getUas() < 0.01) {

		} else {
			nama += " " + (getUasLabel() + " - " + (getUas() == null ? "0" : getUas().toString() + "%"));
		}

		return nama;
	}


	/**
	 * Menyetel field {@code nama} secara langsung.
	 *
	 * <p><b>Praktis tidak berguna:</b> {@link #getNama()} selalu membangun ulang isinya dari
	 * komponen bobot, sehingga nilai yang disetel di sini hilang pada pembacaan berikutnya.
	 * Setter ini tetap ada karena dibutuhkan Hibernate saat memuat baris dari database dan agar
	 * kontrak {@code JavaBean} lengkap.</p>
	 *
	 * @param nama nama yang akan disetel; akan tertimpa oleh {@link #getNama()}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	private Double absen;
	private Double form;
	private Double uts;
	private Double uas;

	private Double tugas1;
	private Double tugas2;
	private Double tugas3;
	private Double tugas4;
	private Double tugas5;

	private Double quiz1;
	private Double quiz2;
	private Double quiz3;
	private Double quiz4;
	private Double quiz5;

	private Double dosen1;
	private Double dosen2;
	private Double dosen3;
	private Double dosen4;
	private Double dosen5;

	private String absenLabel;
	private String formLabel;
	private String utsLabel;
	private String uasLabel;

	private String tugas1Label;
	private String tugas2Label;
	private String tugas3Label;
	private String tugas4Label;
	private String tugas5Label;

	private String quiz1Label;
	private String quiz2Label;
	private String quiz3Label;
	private String quiz4Label;
	private String quiz5Label;

	private String jenisPembobotan;
	private Boolean defaultPembobotan = false;

	private Dosen dimilikiOleh;

	private Boolean aktif;

	private Boolean wajibDitahunAkademikDanSemesterTertentu;
	private String tahunAkadmeik;
	private String semester;


	/**
	 * String JSON kosong ({@code "{}"}) yang dipakai sebagai nilai jatuh-tempo
	 * {@link #getNomorUrutFormat()} bila kolom penyimpan urutan masih kosong.
	 *
	 * <p><b>Kuirk:</b> dideklarasikan {@code static} tetapi tidak {@code final}, sehingga secara
	 * teknis bisa ditimpa oleh kode lain di paket yang sama dan mengubah default untuk seluruh
	 * aplikasi. Perlakukan sebagai konstanta.</p>
	 */
	private static String JSON = new JSONObject().toString();

	private String nomorUrutFormat;


	/**
	 * Mengembalikan <b>peta urutan tampil komponen nilai</b> dalam bentuk string JSON.
	 *
	 * <p>Bentuknya objek JSON dengan <b>kunci = id {@link StatusPertemuan}</b> (sebagai string)
	 * dan <b>nilai = nomor urut</b>, mis. {@code {"3":1,"7":2}}. Layar pengelola
	 * ({@code PembobotanNilaiAction}) menyusunnya dari kotak isian kolom "Urut", dan
	 * {@link FormatNilai#getNomorUrut()} membacanya balik untuk mengurutkan komponen nilai pada
	 * layar penilaian &mdash; tetapi <b>hanya untuk kelas non-OBE</b>; pada kelas OBE nomor urut
	 * datang dari urutan CPMK/Sub-CPMK.</p>
	 *
	 * <p>Bila kolom masih {@code null} atau kosong, method mengembalikan JSON kosong
	 * {@code "{}"} ({@link #JSON}) supaya pemanggil bisa langsung membungkusnya dengan
	 * {@code new JSONObject(...)} tanpa memeriksa {@code null}. Berbeda dari kebanyakan getter di
	 * kelas ini, method ini <b>tidak</b> menulis balik ke field &mdash; nilai default hanya
	 * dikembalikan, tidak disimpan.</p>
	 *
	 * @return string JSON peta urutan komponen; tidak pernah {@code null}/kosong
	 * @see FormatNilai#getNomorUrut()
	 */
	public String getNomorUrutFormat() {
		return nomorUrutFormat == null || nomorUrutFormat.trim().isEmpty() ? JSON : nomorUrutFormat;
	}


	/**
	 * Menyetel peta urutan komponen nilai dalam bentuk string JSON. Tanpa validasi bentuk JSON
	 * &mdash; string yang cacat baru ketahuan saat dibaca kembali oleh pemanggil
	 * {@link #getNomorUrutFormat()}.
	 *
	 * @param nomorUrutFormat string JSON {@code {"<idStatusPertemuan>":<nomorUrut>, ...}};
	 *        {@code null}/kosong berarti "kembali ke default {@code &#123;&#125;}"
	 */
	public void setNomorUrutFormat(String nomorUrutFormat) {
		this.nomorUrutFormat = nomorUrutFormat;
	}


	/**
	 * Merangkai potongan HTML berisi daftar bernomor komponen nilai beserta persentasenya untuk
	 * satu {@link Perkuliahan} &mdash; dipakai sebagai kolom/tooltip ringkasan format nilai di
	 * berbagai layar dan laporan.
	 *
	 * <p>Bentuk keluaran: {@code <font style='font-size:9px;'><ol><li>Nama - 30%</li>...</ol></font>},
	 * dengan persentase diformat memakai {@code Common.numberFormat}.</p>
	 *
	 * <p><b>PERINGATAN: method bernama "tampilkan" ini BISA MENGUBAH DATA.</b> Selain membaca,
	 * ia menjalankan pemulihan otomatis format OBE:</p>
	 * <ol>
	 *   <li>menentukan kurikulum kelas dari {@link Perkuliahan#getKurikulum()}, dengan
	 *   <i>fallback</i> ke {@code perkuliahan.getKurikulumPunyaMatakuliah().getKurikulum()} untuk
	 *   data lama yang kolom kurikulum langsungnya kosong;</li>
	 *   <li>bila kurikulum itu berstatus OBE pada tahun ajaran + semester kelas, sebuah transaksi
	 *   dibuka dan daftar {@link FormatNilai} kelas diperiksa: kalau tidak ada satu pun yang
	 *   terhubung ke CPMK/Sub-CPMK, berarti kelas masih memakai format konvensional peninggalan
	 *   sebelum fitur OBE;</li>
	 *   <li>dalam kasus itu penanda {@code format_nilai_baru} dilepas
	 *   ({@code perkuliahan.belum(...)}) dan
	 *   {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)} dipanggil dengan
	 *   {@code refresh = true} sehingga komponen nilai kelas <b>ditulis ulang</b> menjadi
	 *   berbasis CPMK/Sub-CPMK, lalu transaksi di-commit.</li>
	 * </ol>
	 *
	 * <p>Untuk kelas non-OBE tidak ada transaksi yang dibuka dan method murni membaca.</p>
	 *
	 * <p><b>Manajemen sesi:</b> method membuka {@link Session} sendiri lewat
	 * {@code HibernateUtil.openSession()} dan menutupnya di blok {@code finally}
	 * ({@code clear} &rarr; {@code disconnect} &rarr; {@code close}, masing-masing dibungkus
	 * penangkap galat terpisah). Ini disengaja agar tetap bekerja walau sesi
	 * <i>open-session-in-view</i> sudah tertutup. Konsekuensinya, entity {@link FormatNilai} yang
	 * dibaca di sini menjadi <i>detached</i> begitu method selesai.</p>
	 *
	 * <p><b>Penanganan galat:</b> semua exception ditelan &mdash; transaksi di-rollback bila masih
	 * terbuka dan galat dicatat ke {@code ErrorAuditUtil}, lalu method tetap mengembalikan HTML
	 * apa adanya (bisa jadi daftar kosong atau HTML terpotong tanpa penutup {@code </ol>}
	 * bila galat terjadi di tengah perakitan). Pemanggil tidak pernah tahu ada kegagalan.</p>
	 *
	 * @param perkuliahan kelas yang formatnya ditampilkan; {@code null} ditangani (dianggap
	 *        non-OBE dan {@code Common.getFormatNilais} yang memutuskan hasilnya)
	 * @return potongan HTML daftar komponen nilai; tidak pernah {@code null}
	 * @see #setDefaultPembobotan(Perkuliahan, Session, boolean)
	 * @see FormatNilai
	 */
	public static String tampilkanFormat(Perkuliahan perkuliahan) {
		String content = "<font style='font-size:9px;'>" + "<ol>";
		Session session = null;
		org.hibernate.Transaction transaction = null;
		try {
			session = HibernateUtil.openSession();
			KurikulumPunyaMatakuliah kpm = perkuliahan == null ? null
					: perkuliahan.getKurikulumPunyaMatakuliah();
			Kurikulum kurikulum = perkuliahan == null ? null : perkuliahan.getKurikulum();
			if (kurikulum == null && kpm != null) {
				kurikulum = kpm.getKurikulum();
			}
			boolean obe = perkuliahan != null && kurikulum != null
					&& kurikulum.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap());
			if (obe) {
				transaction = session.beginTransaction();
			}
			List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
			if (obe) {
				boolean sudahFormatObe = false;
				for (FormatNilai formatNilai : formatNilais) {
					if (formatNilai.getCapaianPembelajaranLulusan() != null
							|| (formatNilai.getKodeSubCpmk() != null
									&& !formatNilai.getKodeSubCpmk().trim().isEmpty())) {
						sudahFormatObe = true;
						break;
					}
				}
				if (!sudahFormatObe) {
					perkuliahan.belum("format_nilai_baru");
					formatNilais = setDefaultPembobotan(perkuliahan, session, true);
				}
				transaction.commit();
				transaction = null;
			}

			for (FormatNilai formatNilai : formatNilais) {
				content += "<li>" + formatNilai.getNama() + " = " + Common.numberFormat.get().format(formatNilai.getPersen())
						+ "%" + "</li>";
			}
			content += "</ol>" + "</font>";
		} catch (Exception e) {
			if (transaction != null) {
				try {
					transaction.rollback();
				} catch (Exception rollbackError) {
					ais.common.ErrorAuditUtil.record(rollbackError,
							"auto-audit(empty-catch) PembombotanNilai:tampilkanFormat:rollback");
				}
			}
			ais.common.ErrorAuditUtil.record(e, "PembombotanNilai tampilkanFormat dan pemulihan format OBE");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception closeError) { ais.common.ErrorAuditUtil.record(closeError, "auto-audit(empty-catch) PembombotanNilai:tampilkanFormat:clear"); }
				try { if (session.isConnected()) session.disconnect(); } catch (Exception closeError) { ais.common.ErrorAuditUtil.record(closeError, "auto-audit(empty-catch) PembombotanNilai:tampilkanFormat:disconnect"); }
				try { if (session.isOpen()) session.close(); } catch (Exception closeError) { ais.common.ErrorAuditUtil.record(closeError, "auto-audit(empty-catch) PembombotanNilai:tampilkanFormat:close"); }
			}
		}
		return content;
	}


	/**
	 * <b>Mesin pencetak komponen nilai:</b> menurunkan skema pembobotan sebuah {@link Perkuliahan}
	 * menjadi baris-baris {@link FormatNilai} nyata di database, lalu mengembalikan daftar baris
	 * yang terpilih.
	 *
	 * <p>Ini method terpenting sekaligus terbesar di kelas ini, dan satu-satunya jembatan antara
	 * "cetakan" {@code PembombotanNilai} dengan data penilaian yang benar-benar dipakai layar KHS,
	 * entri nilai, dan ekspor Feeder.</p>
	 *
	 * <h3>Penjaga di awal</h3>
	 * Seluruh badan method hanya berjalan bila kelas <b>belum</b> ditandai
	 * {@code pembobotan_nilai} ({@code perkuliahan.udah(...)}) <b>atau</b> {@code refresh} bernilai
	 * {@code true}. Bila tidak, method langsung mengembalikan {@code List} kosong (bukan
	 * {@code null}) tanpa menyentuh apa pun.
	 *
	 * <h3>Langkah 1 &mdash; mengambil snapshot format lama</h3>
	 * Semua {@link FormatNilai} milik kelas dibaca urut {@code id}; bobot dan peta lokasi lamanya
	 * disimpan sebagai snapshot. Format lama <b>tidak boleh dinolkan di awal</b>. Rekonstruksi OBE
	 * dapat gagal karena CPMK/Sub-CPMK belum lengkap, dan menolkan lebih dahulu pernah membuat
	 * hitung ulang massal menyimpan 0 (E) untuk seluruh peserta kelas.
	 *
	 * <h3>Langkah 2a &mdash; jalur OBE</h3>
	 * Bila kurikulum kelas berstatus OBE (dengan <i>fallback</i> kurikulum/matakuliah lewat
	 * {@link KurikulumPunyaMatakuliah} untuk data lama), komponen dibangun dari
	 * {@link CapaianPembelajaranLulusan} (CPMK) yang id-nya terdaftar pada kolom
	 * {@code capaianPembelajaranLulusan} matakuliah (daftar id dipisah koma; entri yang bukan
	 * angka diabaikan diam-diam). Untuk tiap CPMK aktif, urut kode lalu nama:
	 * <ul>
	 *   <li>bila flag {@code KurikulumPunyaMatakuliah.nilaiMenggunakanCpmk} menyala <b>atau</b>
	 *   CPMK itu tidak punya Sub-CPMK sama sekali (dideteksi dari formula JSON-nya), dibuat
	 *   <b>satu</b> {@link FormatNilai} langsung dari CPMK dengan bobot CPMK;</li>
	 *   <li>selain itu dibuat satu {@link FormatNilai} <b>per Sub-CPMK</b> dari array JSON
	 *   {@code formula} milik CPMK (entri tanpa {@code key} dilewati).</li>
	 * </ul>
	 * Pencarian baris yang sudah ada diutamakan lewat {@code kodeSubCpmk} (kunci formula JSON)
	 * karena lebih stabil daripada relasi {@link StatusPertemuan} yang id-nya bisa berganti antar
	 * proses hitung ulang. Nomor urut diisi berurutan mulai 1.
	 *
	 * <p>Dua penjaga anti-galat penting di jalur ini (keduanya hasil perbaikan bug nyata):
	 * baris baru <b>tidak</b> dibuat bila {@link StatusPertemuan} hasil pencarian {@code null}
	 * (menghindari pelanggaran {@code NOT NULL} pada kolom {@code status_pertemuan}), dan nama
	 * {@link StatusPertemuan} dijatuhkan berurutan ke {@code kode} &rarr; {@code nama} &rarr;
	 * {@code key} karena entri Sub-CPMK sering tidak mengisi {@code kode}. Baris lama yang
	 * {@code statusPertemuan}-nya terlanjur {@code null} akibat bug sesi-tertutup ikut
	 * diperbaiki di sini.</p>
	 *
	 * <p>Jalur OBE memiliki pencatatan exception lokal, tetapi hasil akhirnya tetap wajib melewati
	 * validasi jumlah bobot. Daftar kosong, komponen tanpa status aktif, atau total bobot di luar
	 * rentang 99-101% dibatalkan dan snapshot lama dipulihkan.</p>
	 *
	 * <h3>Langkah 2b &mdash; jalur konvensional</h3>
	 * Untuk kelas non-OBE, skema diambil dari {@code perkuliahan.getPembombotanNilai()} dan tiap
	 * komponen berbobot {@code > 0,1} diterbitkan sebagai satu {@link FormatNilai} yang terhubung
	 * ke {@link StatusPertemuan} baku dari {@code ConstantValues}: {@code ABSEN}, {@code FORM},
	 * {@code UTS}, {@code UAS}, {@code TUGAS_1..TUGAS_5}, {@code QUIZ_1..QUIZ_5} &mdash; masing-masing
	 * lewat blok kode yang bentuknya identik (cari baris yang ada, buat bila belum ada, isi persen,
	 * simpan, daftarkan ke cache).
	 *
	 * <p><b>Kesenjangan yang perlu dicatat:</b> komponen {@link #getDosen1() dosen1..dosen5}
	 * <b>tidak pernah</b> diterbitkan menjadi {@link FormatNilai} di jalur mana pun, walaupun
	 * ikut divalidasi menuju total 100% oleh layar pengelola. Bobot penilaian per dosen penguji
	 * untuk Tugas Akhir/KKN/PKL karenanya harus dibaca langsung dari entity ini oleh modul
	 * masing-masing, bukan lewat mekanisme {@code FormatNilai}.</p>
	 *
	 * <p>Ambang {@code > 0,1} di sini juga berbeda dari ambang {@code >= 0,01} yang dipakai
	 * {@link #getNama()}, sehingga bobot sangat kecil bisa tampil di nama tetapi tidak
	 * menghasilkan komponen nilai.</p>
	 *
	 * <h3>Commit logis dan pemulihan</h3>
	 * Format lama yang tidak lagi terpilih baru dinolkan <b>setelah</b> format pengganti lengkap
	 * dan total bobotnya tervalidasi. Peta lokasi kemudian dibangun ulang hanya dari format baru.
	 * Bila salah satu tahap gagal, bobot lama dan peta lokasi lama dipulihkan; format baru hasil
	 * percobaan dinonaktifkan. Pola ini harus dipertahankan agar tombol Sinkronkan/Hitung Ulang
	 * bersifat fail-safe dan tidak mengubah nilai sah menjadi nol akibat konfigurasi yang rusak.
	 *
	 * <h3>Transaksi &amp; sesi</h3>
	 * Method <b>tidak</b> membuka atau menutup {@link Session} maupun transaksi sendiri &mdash;
	 * keduanya tanggung jawab pemanggil. Sesi sengaja diterima lewat parameter (bukan
	 * {@code currentSession()}) supaya tetap bekerja setelah sesi <i>open-session-in-view</i>
	 * ditutup. Penyimpanan memakai {@code Common.refreshSaveOrUpdate}, dan tiap baris hasil
	 * didaftarkan ke cache entity lewat {@code masukkanData(FormatNilai.class, ...)} serta ke peta
	 * lokasi kelas lewat {@code perkuliahan.populateFormatNilai(...)}.
	 *
	 * @param perkuliahan kelas yang komponen nilainya dicetak ulang; <b>tidak boleh</b>
	 *        {@code null} (dipakai langsung tanpa penjagaan)
	 * @param session     sesi Hibernate aktif milik pemanggil, dipakai untuk seluruh query dan
	 *                    penyimpanan
	 * @param refresh     {@code true} memaksa pencetakan ulang walau kelas sudah pernah ditandai
	 *                    {@code pembobotan_nilai}
	 * @return daftar {@link FormatNilai} hasil pencetakan (jalur OBE maupun konvensional), atau
	 *         {@code List} kosong bila penjaga di awal menolak menjalankan proses; tidak pernah
	 *         {@code null}
	 * @see #tampilkanFormat(Perkuliahan)
	 * @see Perkuliahan#getPembombotanNilai()
	 * @see FormatNilai
	 */
	@SuppressWarnings({ "unchecked" })
	public static List<FormatNilai> setDefaultPembobotan(Perkuliahan perkuliahan, Session session, boolean refresh) {
		if (!perkuliahan.udah("pembobotan_nilai") || refresh) {

			List<FormatNilai> formatNilaisPilih = new ArrayList<FormatNilai>();
			List<FormatNilai> formatNilais = session.createCriteria(FormatNilai.class).addOrder(Order.asc("id"))
					.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
			String lokasiFormatNilaiLama = perkuliahan.ambilLokasiFormatNilai();
			Map<Long, Double> persenFormatNilaiLama = new HashMap<Long, Double>();
			for (FormatNilai formatNilai : formatNilais) {
				if (formatNilai != null && formatNilai.getId() != null) {
					persenFormatNilaiLama.put(formatNilai.getId(), formatNilai.getPersen());
				}
			}

			try {
			KurikulumPunyaMatakuliah kpmObe = perkuliahan.getKurikulumPunyaMatakuliah();
			Kurikulum kurikulumObe = perkuliahan.getKurikulum();
			Matakuliah matakuliahObe = perkuliahan.getMatakuliah();
			if (kurikulumObe == null && kpmObe != null) {
				kurikulumObe = kpmObe.getKurikulum();
			}
			if (matakuliahObe == null && kpmObe != null) {
				matakuliahObe = kpmObe.getMatakuliah();
			}

			if (kurikulumObe != null && matakuliahObe != null
					&& kurikulumObe.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {

				try {
					Set<Long> longs = new HashSet<Long>();
					String daftarCpmk = matakuliahObe.getCapaianPembelajaranLulusan();
					if (daftarCpmk == null) {
						daftarCpmk = "";
					}
					for (String d : daftarCpmk.split(",")) {
						if (!d.trim().isEmpty()) {
							try {
								longs.add(Long.parseLong(d.trim()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PembombotanNilai.java:323");
								// TODO: handle exception
							}
						}
					}

					// Gunakan session yang diterima parameter bukan currentSession()
					// agar tidak gagal dengan "Session is closed!" bila OSIV sudah ditutup.
					List<CapaianPembelajaranLulusan> capaianPembelajaranLulusans = ConstantValues.simpleList(
							session.createCriteria(CapaianPembelajaranLulusan.class)
									.add(longs.isEmpty() ? Restrictions.sqlRestriction("false")
											: Restrictions.in("id", longs))
									.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							CapaianPembelajaranLulusan.class);
					String masalahSumberObe = analisisKesiapanSumberObe(kpmObe, capaianPembelajaranLulusans);
					if (masalahSumberObe != null) {
						return ambilFormatLamaAktif(formatNilais, persenFormatNilaiLama);
					}
					int index = 1;
					for (final CapaianPembelajaranLulusan capaianPembelajaranLulusan : capaianPembelajaranLulusans) {

						// FLAG "Bobot Penilaian Menggunakan CPMK" ATAU CPMK ini TIDAK punya Sub-CPMK -> buat FormatNilai
						// berbasis CPMK LANGSUNG (permintaan user: bila Sub-CPMK tak dibuat, komponen nilai TETAP tercipta
						// dari CPMK dgn bobot CPMK). Bila flag OFF DAN ada Sub-CPMK -> tetap per Sub-CPMK (perilaku lama).
						// Deteksi ada/tidaknya Sub-CPMK dari formula JSON milik CPMK ini.
						boolean flagCpmk = kpmObe != null && kpmObe.getNilaiMenggunakanCpmk();
						boolean adaSubCpmk = false;
						try {
							JSONArray cekSub = new JSONArray(capaianPembelajaranLulusan.getFormula());
							for (int iSub = 0; iSub < cekSub.length(); iSub++) {
								if (!cekSub.getJSONObject(iSub).isNull("key")) {
									adaSubCpmk = true;
									break;
								}
							}
						} catch (Exception eCekSub) {
							adaSubCpmk = false;
						}
						if (flagCpmk || !adaSubCpmk) {
							StatusPertemuan statusPertemuan = StatusPertemuan
									.ambilByNama(capaianPembelajaranLulusan.getKode());
							FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
									.add(Restrictions.eq("perkuliahan", perkuliahan))
									.add(Restrictions.eq("capaianPembelajaranLulusan", capaianPembelajaranLulusan))
									.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

							if (formatNilai == null) {
								// Guard: statusPertemuan null (kode CPMK kosong) → skip agar tidak INSERT
								// FormatNilai dengan status_pertemuan=null yang melanggar NOT NULL constraint.
								if (statusPertemuan == null) {
									continue;
								}
								formatNilai = new FormatNilai();
								formatNilai.setPerkuliahan(perkuliahan);
								formatNilai.setStatusPertemuan(statusPertemuan);
							}
							formatNilai.setCapaianPembelajaranLulusan(capaianPembelajaranLulusan);
							formatNilai.setKodeSubCpmk(capaianPembelajaranLulusan.getKode());
							String namaCpmk = capaianPembelajaranLulusan.getNama() == null ? ""
									: capaianPembelajaranLulusan.getNama().trim();
							String kodeCpmk = capaianPembelajaranLulusan.getKode() == null ? ""
									: capaianPembelajaranLulusan.getKode().trim();
							formatNilai.setNama(kodeCpmk.length() > 0 && namaCpmk.length() > 0
									? kodeCpmk + " - " + namaCpmk
									: (kodeCpmk.length() > 0 ? kodeCpmk : namaCpmk));
							formatNilai.setPersen(capaianPembelajaranLulusan.getBobot());
							formatNilai.setNomorUrut(index);
							Common.refreshSaveOrUpdate(session, formatNilai);
							perkuliahan.populateFormatNilai(formatNilai, true);
							formatNilaisPilih.add(formatNilai);
							masukkanData(FormatNilai.class, formatNilai);
							index++;
						} else {

							final JSONArray array = new JSONArray(capaianPembelajaranLulusan.getFormula());
							for (int i = 0; i < array.length(); i++) {
								JSONObject jsonObject = array.getJSONObject(i);

								if (jsonObject.isNull("key")) {
									continue;
								}

								String nama = "";

								if (!jsonObject.isNull("nama")) {
									nama = jsonObject.get("nama") + "";
								}

								String kode = "";

								if (!jsonObject.isNull("kode")) {
									kode = jsonObject.get("kode") + "";
								}

								Double bobot = 0.0;

								if (!jsonObject.isNull("bobot")) {
									bobot = Double.parseDouble(jsonObject.get("bobot") + "");
								}

								// Cari FormatNilai lewat kodeSubCpmk (key dari formula JSON) dulu
								// karena lebih stabil daripada relasi statusPertemuan yang ID-nya bisa
								// berbeda antara satu proses hitung ulang dengan proses berikutnya.
								String keyStr = jsonObject.isNull("key") ? "" : (jsonObject.get("key") + "");
								// KE-FIX (PSQLException NOT NULL "status_pertemuan" saat INSERT FormatNilai
								// baru, mis. entri "evaluasi sub CPMK"): entri formula sub-CPMK OBE sering
								// TIDAK mengisi "kode" (hanya "key"/"nama") -- StatusPertemuan.ambilByNama("")
								// pulang null karena guard string kosongnya sendiri, padahal method ini akan
								// SELALU auto-create+simpan StatusPertemuan baru bila diberi nama yang tidak
								// kosong. Jatuhkan ke keyStr lalu nama sebagai nama StatusPertemuan pengganti
								// supaya FormatNilai baru tidak pernah dibuat dengan statusPertemuan null.
								String namaStatusPertemuan = !kode.trim().isEmpty() ? kode
										: (!nama.trim().isEmpty() ? nama : keyStr);
								StatusPertemuan statusPertemuan = StatusPertemuan.ambilByNama(namaStatusPertemuan);
								FormatNilai formatNilai = null;
								if (!keyStr.isEmpty()) {
									formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
											.add(Restrictions.eq("perkuliahan", perkuliahan))
											.add(Restrictions.eq("kodeSubCpmk", keyStr))
											.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								}
								if (formatNilai == null && statusPertemuan != null) {
									formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
											.add(Restrictions.eq("perkuliahan", perkuliahan))
											.add(Restrictions.eq("statusPertemuan", statusPertemuan))
											.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
								}

								if (formatNilai == null) {
									// Guard: statusPertemuan null (kode/key/nama semuanya kosong) → skip
									// agar tidak INSERT FormatNilai dengan status_pertemuan=null (NOT NULL constraint).
									if (statusPertemuan == null) {
										continue;
									}
									formatNilai = new FormatNilai();
									formatNilai.setPerkuliahan(perkuliahan);
									formatNilai.setStatusPertemuan(statusPertemuan);
								} else if (formatNilai.getStatusPertemuan() == null && statusPertemuan != null) {
									// Repair FormatNilai lama yg statusPertemuan-nya null akibat bug session-close.
									// Tanpa ini, ambilMapNomor melewati entry ini → sub-CPMK tidak terhitung.
									formatNilai.setStatusPertemuan(statusPertemuan);
								}
								formatNilai.setCapaianPembelajaranLulusan(capaianPembelajaranLulusan);
								formatNilai.setKodeSubCpmk(jsonObject.get("key") + "");
								String namaTampilan = kode.trim().length() > 0 && nama.trim().length() > 0
										? kode.trim() + " - " + nama.trim()
										: (nama.trim().length() > 0 ? nama.trim() : kode.trim());
								formatNilai.setNama(namaTampilan);
								formatNilai.setPersen(bobot);
								formatNilai.setNomorUrut(index);
								Common.refreshSaveOrUpdate(session, formatNilai);
								perkuliahan.populateFormatNilai(formatNilai, true);
								formatNilaisPilih.add(formatNilai);
								masukkanData(FormatNilai.class, formatNilai);
								index++;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/PembombotanNilai.java:452");
				}

			} else {

				PembombotanNilai pembombotanNilai = perkuliahan.getPembombotanNilai();

				if (pembombotanNilai.getAbsen() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.ABSEN)).addOrder(Order.desc("id"))
							.setMaxResults(1).uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.ABSEN);
					}
					formatNilai.setPersen(pembombotanNilai.getAbsen().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getForm() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.FORM)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.FORM);
					}
					formatNilai.setPersen(pembombotanNilai.getForm().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getUts() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.UTS)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.UTS);
					}
					formatNilai.setPersen(pembombotanNilai.getUts().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getUas() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.UAS)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.UAS);
					}
					formatNilai.setPersen(pembombotanNilai.getUas().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getTugas1() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.TUGAS_1)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.TUGAS_1);
					}
					formatNilai.setPersen(pembombotanNilai.getTugas1().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getTugas2() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.TUGAS_2)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.TUGAS_2);
					}
					formatNilai.setPersen(pembombotanNilai.getTugas2().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getTugas3() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.TUGAS_3)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.TUGAS_3);
					}
					formatNilai.setPersen(pembombotanNilai.getTugas3().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getTugas4() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.TUGAS_4)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.TUGAS_4);
					}
					formatNilai.setPersen(pembombotanNilai.getTugas4().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getTugas5() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.TUGAS_5)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.TUGAS_5);
					}
					formatNilai.setPersen(pembombotanNilai.getTugas5().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getQuiz1() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.QUIZ_1)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.QUIZ_1);
					}
					formatNilai.setPersen(pembombotanNilai.getQuiz1().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getQuiz2() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.QUIZ_2)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.QUIZ_2);
					}
					formatNilai.setPersen(pembombotanNilai.getQuiz2().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getQuiz3() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.QUIZ_3)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.QUIZ_3);
					}
					formatNilai.setPersen(pembombotanNilai.getQuiz3().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getQuiz4() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.QUIZ_4)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.QUIZ_4);
					}
					formatNilai.setPersen(pembombotanNilai.getQuiz4().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}

				if (pembombotanNilai.getQuiz5() > 0.1) {
					FormatNilai formatNilai = (FormatNilai) session.createCriteria(FormatNilai.class)
							.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("statusPertemuan", ConstantValues.QUIZ_5)).setMaxResults(1)
							.uniqueResult();

					if (formatNilai == null) {
						formatNilai = new FormatNilai();
						formatNilai.setPerkuliahan(perkuliahan);
						formatNilai.setStatusPertemuan(ConstantValues.QUIZ_5);
					}
					formatNilai.setPersen(pembombotanNilai.getQuiz5().doubleValue());
					Common.refreshSaveOrUpdate(session, formatNilai);
					perkuliahan.populateFormatNilai(formatNilai, true);
					formatNilaisPilih.add(formatNilai);
					masukkanData(FormatNilai.class, formatNilai);
				}
			}
			if (!Detailperkuliahan.formatNilaiSiapDihitung(formatNilaisPilih)) {
				throw new FormatPenggantiBelumSiapException(ringkasanFormatTidakSiap(formatNilaisPilih));
			}

			// Commit logis dilakukan paling akhir. Format lama baru dinonaktifkan setelah format
			// pengganti lulus validasi, sehingga kegagalan OBE tidak dapat menghapus bobot produksi.
			Set<Long> idFormatTerpilih = new HashSet<Long>();
			for (FormatNilai formatNilai : formatNilaisPilih) {
				if (formatNilai != null && formatNilai.getId() != null) {
					idFormatTerpilih.add(formatNilai.getId());
				}
			}
			for (FormatNilai formatNilai : formatNilais) {
				if (formatNilai != null && formatNilai.getId() != null
						&& !idFormatTerpilih.contains(formatNilai.getId())) {
					formatNilai.setPersen(0.0);
					Common.refreshUpdate(session, formatNilai, false);
					masukkanData(FormatNilai.class, formatNilai);
				}
			}
			perkuliahan.tulisLokasiFormatNilai(new JSONObject().toString());
			for (FormatNilai formatNilai : formatNilaisPilih) {
				perkuliahan.populateFormatNilai(formatNilai, true);
			}
			return formatNilaisPilih;
			} catch (Exception e) {
				// Rollback aplikatif diperlukan karena helper persistence warisan dapat melakukan flush
				// per komponen. Pulihkan bobot dan flag store lama sebelum keluar dari method.
				for (FormatNilai formatNilai : formatNilais) {
					if (formatNilai != null && formatNilai.getId() != null
							&& persenFormatNilaiLama.containsKey(formatNilai.getId())) {
						formatNilai.setPersen(persenFormatNilaiLama.get(formatNilai.getId()));
						try {
							Common.refreshUpdate(session, formatNilai, false);
							masukkanData(FormatNilai.class, formatNilai);
						} catch (Exception restoreError) {
							ais.common.ErrorAuditUtil.record(restoreError,
									"gagal memulihkan FormatNilai id=" + formatNilai.getId());
						}
					}
				}
				for (FormatNilai formatNilai : formatNilaisPilih) {
					if (formatNilai != null && formatNilai.getId() != null
							&& !persenFormatNilaiLama.containsKey(formatNilai.getId())) {
						formatNilai.setPersen(0.0);
						try {
							Common.refreshUpdate(session, formatNilai, false);
							masukkanData(FormatNilai.class, formatNilai);
						} catch (Exception restoreError) {
							ais.common.ErrorAuditUtil.record(restoreError,
									"gagal menonaktifkan FormatNilai baru id=" + formatNilai.getId());
						}
					}
				}
				perkuliahan.tulisLokasiFormatNilai(lokasiFormatNilaiLama);
				if (!(e instanceof FormatPenggantiBelumSiapException)) {
					ais.common.ErrorAuditUtil.record(e,
							"setDefaultPembobotan gagal dan format lama dipulihkan, perkuliahan="
									+ perkuliahan.getId());
				}
				return ambilFormatLamaAktif(formatNilais, persenFormatNilaiLama);
			}
		} else {
			return new ArrayList<FormatNilai>();
		}
	}


	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Semua bobot dibiarkan {@code null} dan
	 * baru dinormalkan menjadi {@code 0.0} (atau nilai konfigurasi, untuk UTS/UAS) saat getter
	 * masing-masing dipanggil pertama kali.
	 */
	public PembombotanNilai() {
	}


	/**
	 * Konstruktor pintasan yang hanya menetapkan primary key &mdash; berguna untuk membuat
	 * referensi ringan ke baris yang sudah ada (mis. sebagai pembanding
	 * {@link GeneralValueObject#equals(Object)} yang berbasis id) tanpa memuat isinya dari
	 * database.
	 *
	 * @param id primary key baris pembobotan
	 */
	public PembombotanNilai(Long id) {
		this.id = id;
	}


	/**
	 * Konstruktor pintasan untuk skema konvensional paling umum: tugas ({@code form}) + UTS + UAS.
	 *
	 * <p>Field diisi langsung tanpa melewati setter dan tanpa validasi total 100%; komponen lain
	 * (absen, tugas1..5, quiz1..5, dosen1..5) dibiarkan {@code null} sehingga terbaca {@code 0.0}.</p>
	 *
	 * @param form persentase komponen tugas (kolom {@code form})
	 * @param uts  persentase Ujian Tengah Semester
	 * @param uas  persentase Ujian Akhir Semester
	 */
	public PembombotanNilai(Double form, Double uts, Double uas) {
		this.form = form;
		this.uts = uts;
		this.uas = uas;
	}


	/**
	 * Mengembalikan primary key baris pembobotan ({@code IDENTITY}, dibangkitkan database).
	 *
	 * <p>Nilai inilah dasar {@link GeneralValueObject#equals(Object)} dan kunci cache
	 * {@code ConstantValues.ambilBerdasarClass(PembombotanNilai.class)}. Bernilai {@code null}
	 * untuk object yang belum pernah disimpan. Field {@code id} di kelas ini meng-<i>shadow</i>
	 * milik {@link GeneralValueObject}.</p>
	 *
	 * @return primary key, atau {@code null} bila belum tersimpan
	 * @see GeneralValueObject#getId()
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}


	/**
	 * Menyetel primary key. Tanpa validasi; normalnya hanya dipanggil Hibernate.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}


	/**
	 * Mengembalikan persentase komponen <b>tugas</b> (kolom database bernama {@code form}).
	 *
	 * <p>Penamaan ini membingungkan: kolomnya {@code form}, tetapi label tampilan defaultnya
	 * {@code "Tugas"} ({@link #getFormLabel()}) dan {@link StatusPertemuan} pasangannya adalah
	 * {@code ConstantValues.FORM}. Komponen ini terpisah dari {@code tugas1..tugas5}, jadi sebuah
	 * skema bisa punya "Tugas" (dari {@code form}) sekaligus "Tugas 1".."Tugas 5" sekaligus.</p>
	 *
	 * <p><b>Getter berefek samping:</b> bila field masih {@code null}, ia diisi {@code 0.0}
	 * (ditulis balik ke field, bukan sekadar dikembalikan) supaya kolom {@code NOT NULL} tidak
	 * pernah gagal saat simpan.</p>
	 *
	 * @return persentase komponen tugas dalam satuan persen (mis. {@code 20.0} berarti 20%);
	 *         tidak pernah {@code null}
	 */
	@Column(name = "form", nullable = false)
	public Double getForm() {
		if (form == null) {
			form = 0.0;
		}
		return this.form;
	}


	/**
	 * Menyetel persentase komponen tugas (kolom {@code form}). Tanpa validasi rentang maupun
	 * total 100% &mdash; aturan itu ditegakkan layar {@code PembobotanNilaiAction}.
	 *
	 * @param form persentase dalam satuan persen; {@code null} akan dinormalkan {@code 0.0} oleh
	 *        {@link #getForm()}
	 */
	public void setForm(Double form) {
		this.form = form;
	}


	/**
	 * Mengembalikan persentase komponen <b>Ujian Tengah Semester</b>.
	 *
	 * <p><b>PERINGATAN &mdash; getter ini bisa MENULIS BARIS BARU KE DATABASE.</b> Bila field masih
	 * {@code null}, nilainya diambil dari konfigurasi aplikasi
	 * {@code Common.getKonfigurasi("default_prentasi_uts", "0")}. Helper konfigurasi itu
	 * <b>menuliskan baris default ke tabel konfigurasi bila kuncinya belum ada</b>, jadi sekadar
	 * membaca bobot UTS pada instalasi baru dapat menerbitkan {@code INSERT}. Nama kuncinya sendiri
	 * mengandung typo: {@code prentasi}, bukan {@code persentasi} &mdash; jangan "diperbaiki",
	 * karena kunci itulah yang tersimpan di database pelanggan.</p>
	 *
	 * <p>Kegagalan parsing/konfigurasi ditelan dan hanya ditampilkan kepada admin lewat
	 * {@code Common.tampilErrorJikaAdmin}; nilai kemudian jatuh ke {@code 0.0}. Hasilnya
	 * ditulis balik ke field, sehingga entity terkelola bisa menjadi kotor hanya karena dibaca.</p>
	 *
	 * @return persentase UTS dalam satuan persen; tidak pernah {@code null}
	 * @see #getUas()
	 */
	@Column(name = "uts", nullable = false)
	public Double getUts() {
		if (uts == null) {
			try {
				uts = Double.parseDouble(Common.getKonfigurasi("default_prentasi_uts", "0").getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		if (uts == null) {
			uts = 0.0;
		}
		return this.uts;
	}


	/**
	 * Menyetel persentase komponen UTS. Tanpa validasi.
	 *
	 * <p>Menyetel nilai non-{@code null} sekaligus mencegah pembacaan konfigurasi default pada
	 * {@link #getUts()}.</p>
	 *
	 * @param uts persentase dalam satuan persen
	 */
	public void setUts(Double uts) {
		this.uts = uts;
	}


	/**
	 * Mengembalikan persentase komponen <b>Ujian Akhir Semester</b>.
	 *
	 * <p>Perilakunya identik dengan {@link #getUts()}, termasuk kemampuannya menuliskan baris
	 * konfigurasi default ke database &mdash; hanya kuncinya yang berbeda:
	 * {@code default_prentasi_uas} (typo yang sama, dipertahankan).</p>
	 *
	 * @return persentase UAS dalam satuan persen; tidak pernah {@code null}
	 * @see #getUts()
	 */
	@Column(name = "uas", nullable = false)
	public Double getUas() {
		if (uas == null) {
			try {
				uas = Double.parseDouble(Common.getKonfigurasi("default_prentasi_uas", "0").getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		if (uas == null) {
			uas = 0.0;
		}
		return this.uas;
	}


	/**
	 * Menyetel persentase komponen UAS. Tanpa validasi.
	 *
	 * @param uas persentase dalam satuan persen
	 * @see #setUts(Double)
	 */
	public void setUas(Double uas) {
		this.uas = uas;
	}


	/**
	 * Menyetel persentase bobot slot dosen 1. Tanpa validasi.
	 *
	 * @param dosen1 persentase dalam satuan persen
	 * @see #getDosen1()
	 */
	public void setDosen1(Double dosen1) {
		this.dosen1 = dosen1;
	}


	/**
	 * Mengembalikan persentase bobot untuk slot dosen penilai pertama ({@code dosen1}) &mdash;
	 * yaitu bobot nilai yang diberikan penilai/penguji pertama.
	 *
	 * <p>Kelompok {@code dosen1..dosen5} hanya relevan untuk jenis pembobotan
	 * {@link #JENIS_PERKULIAHAN_TUGAS_AKHIR} (lima slot),
	 * {@link #JENIS_PERKULIAHAN_KKN} dan {@link #JENIS_PERKULIAHAN_PKL} (dua slot). Pada jenis
	 * "Perkuliahan Belajar Mengajar" kotak isiannya di-<i>disable</i> oleh layar pengelola
	 * berdasarkan {@link #keterhubungan}.</p>
	 *
	 * <p><b>Kesenjangan penting:</b> berbeda dari komponen konvensional lain, slot dosen
	 * <b>tidak pernah</b> diterbitkan sebagai {@link FormatNilai} oleh
	 * {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)} dan <b>tidak pernah</b> muncul
	 * di {@link #getNama()} &mdash; padahal tetap ikut dijumlahkan ke total 100% oleh validasi
	 * layar pengelola. Modul tugas akhir/KKN/PKL membaca nilai ini langsung dari entity, bukan
	 * lewat mekanisme {@code FormatNilai}. Slot ini juga tidak punya field label seperti komponen
	 * lain, sehingga penamaannya di layar bersifat tetap.</p>
	 *
	 * <p><b>Getter berefek samping:</b> mengisi {@code 0.0} ke field bila masih {@code null}.</p>
	 *
	 * @return persentase bobot slot dosen 1 dalam satuan persen; tidak pernah {@code null}
	 */
	public Double getDosen1() {
		if (dosen1 == null) {
			dosen1 = 0.0;
		}
		return dosen1;
	}


	/**
	 * Menyetel persentase bobot slot dosen 2. Tanpa validasi.
	 *
	 * @param dosen2 persentase dalam satuan persen
	 * @see #getDosen1()
	 */
	public void setDosen2(Double dosen2) {
		this.dosen2 = dosen2;
	}


	/**
	 * Mengembalikan persentase bobot slot dosen 2 ({@code dosen2}) &mdash; bobot nilai dari penilai/penguji kedua.
	 * Perilaku dan seluruh catatannya identik dengan {@link #getDosen1()}, termasuk lazy default
	 * {@code 0.0} dan fakta bahwa slot ini tidak diterbitkan sebagai {@link FormatNilai}.
	 *
	 * @return persentase bobot slot dosen 2 dalam satuan persen; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public Double getDosen2() {
		if (dosen2 == null) {
			dosen2 = 0.0;
		}
		return dosen2;
	}


	/**
	 * Menyetel persentase bobot slot dosen 3. Tanpa validasi.
	 *
	 * @param dosen3 persentase dalam satuan persen
	 * @see #getDosen1()
	 */
	public void setDosen3(Double dosen3) {
		this.dosen3 = dosen3;
	}


	/**
	 * Mengembalikan persentase bobot slot dosen 3 ({@code dosen3}) &mdash; bobot nilai dari penilai/penguji ketiga.
	 * Perilaku dan seluruh catatannya identik dengan {@link #getDosen1()}, termasuk lazy default
	 * {@code 0.0} dan fakta bahwa slot ini tidak diterbitkan sebagai {@link FormatNilai}.
	 *
	 * @return persentase bobot slot dosen 3 dalam satuan persen; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public Double getDosen3() {
		if (dosen3 == null) {
			dosen3 = 0.0;
		}
		return dosen3;
	}


	/**
	 * Menyetel persentase bobot slot dosen 4. Tanpa validasi.
	 *
	 * @param dosen4 persentase dalam satuan persen
	 * @see #getDosen1()
	 */
	public void setDosen4(Double dosen4) {
		this.dosen4 = dosen4;
	}


	/**
	 * Mengembalikan persentase bobot slot dosen 4 ({@code dosen4}) &mdash; bobot nilai dari penilai/penguji keempat.
	 * Perilaku dan seluruh catatannya identik dengan {@link #getDosen1()}, termasuk lazy default
	 * {@code 0.0} dan fakta bahwa slot ini tidak diterbitkan sebagai {@link FormatNilai}.
	 *
	 * @return persentase bobot slot dosen 4 dalam satuan persen; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public Double getDosen4() {
		if (dosen4 == null) {
			dosen4 = 0.0;
		}
		return dosen4;
	}


	/**
	 * Menyetel persentase bobot slot dosen 5. Tanpa validasi.
	 *
	 * @param dosen5 persentase dalam satuan persen
	 * @see #getDosen1()
	 */
	public void setDosen5(Double dosen5) {
		this.dosen5 = dosen5;
	}


	/**
	 * Mengembalikan persentase bobot slot dosen 5 ({@code dosen5}) &mdash; bobot nilai dari penilai/penguji kelima.
	 * Perilaku dan seluruh catatannya identik dengan {@link #getDosen1()}, termasuk lazy default
	 * {@code 0.0} dan fakta bahwa slot ini tidak diterbitkan sebagai {@link FormatNilai}.
	 *
	 * @return persentase bobot slot dosen 5 dalam satuan persen; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public Double getDosen5() {
		if (dosen5 == null) {
			dosen5 = 0.0;
		}
		return dosen5;
	}


	/**
	 * Menyetel jenis pembobotan. Tanpa validasi terhadap keempat konstanta yang dikenal
	 * {@link #keterhubungan}; nilai di luar daftar itu akan membuat layar pengelola melempar
	 * {@code NullPointerException} saat menyesuaikan kotak isian.
	 *
	 * @param jenisPembobotan salah satu dari {@link #JENIS_PERKULIAHAN_BELAJAR_MENGAJAR},
	 *        {@link #JENIS_PERKULIAHAN_TUGAS_AKHIR}, {@link #JENIS_PERKULIAHAN_KKN},
	 *        {@link #JENIS_PERKULIAHAN_PKL}
	 */
	public void setJenisPembobotan(String jenisPembobotan) {
		this.jenisPembobotan = jenisPembobotan;
	}


	/**
	 * Mengembalikan jenis pembobotan yang menentukan komponen mana yang boleh diisi.
	 *
	 * <p>Berbeda dari kebanyakan getter di kelas ini, method ini <b>tidak</b> memberi nilai default
	 * &mdash; hasilnya boleh {@code null}. Pemanggil (mis. layar pengelola) yang menganggap
	 * {@code null} berarti {@link #JENIS_PERKULIAHAN_BELAJAR_MENGAJAR}.</p>
	 *
	 * @return jenis pembobotan, atau {@code null} bila belum pernah dipilih
	 * @see #keterhubungan
	 */
	@Column(name = "jenis_pembobotan", nullable = true)
	public String getJenisPembobotan() {
		return jenisPembobotan;
	}


	/**
	 * Menandai/melepas baris ini sebagai <b>format bawaan kampus</b>.
	 *
	 * <p><b>Jangan tertukar</b> dengan method statis
	 * {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)} yang namanya sama persis tetapi
	 * pekerjaannya jauh berbeda (mencetak baris {@link FormatNilai} ke database). Yang ini hanya
	 * setter flag biasa, tanpa efek samping.</p>
	 *
	 * <p>Perlu diketahui: tidak ada penjagaan "hanya boleh satu baris default" di lapisan entity
	 * ini. {@code InitDataHelper} mengambil baris default dengan {@code uniqueResult}-gaya
	 * pencarian, jadi menandai lebih dari satu baris sebagai default berpotensi menghasilkan
	 * perilaku tak tentu saat inisialisasi {@code ConstantValues.DEFAULT_PEMBOBOTAN_NILAI}.</p>
	 *
	 * @param defaultPembobotan {@code true} bila baris ini menjadi format bawaan
	 */
	public void setDefaultPembobotan(Boolean defaultPembobotan) {
		this.defaultPembobotan = defaultPembobotan;
	}


	/**
	 * Mengembalikan penanda "format bawaan kampus".
	 *
	 * <p>Baris yang bertanda inilah yang dijemput {@code InitDataHelper} menjadi
	 * {@code ConstantValues.DEFAULT_PEMBOBOTAN_NILAI}, yaitu skema yang dipakai
	 * {@link Perkuliahan#getPembombotanNilai()} ketika sebuah kelas belum memilih format apa pun.
	 * Bila belum ada satu pun baris default, {@code InitDataHelper} membuatkannya dengan komposisi
	 * Tugas 20% / UTS 30% / UAS 50%.</p>
	 *
	 * <p><b>Getter berefek samping:</b> mengisi {@code false} ke field bila masih {@code null}.
	 * Perhatikan field ini sudah diinisialisasi {@code false} pada deklarasinya, jadi jalur
	 * {@code null} hanya terjadi bila database menyimpan {@code NULL}.</p>
	 *
	 * @return {@code true} bila baris ini format bawaan; tidak pernah {@code null}
	 */
	public Boolean getDefaultPembobotan() {
		if (defaultPembobotan == null) {
			defaultPembobotan = false;
		}
		return defaultPembobotan;
	}


	/**
	 * Mengembalikan persentase komponen <b>absensi/kehadiran</b>, dipasangkan dengan
	 * {@link StatusPertemuan} {@code ConstantValues.ABSEN} saat dicetak menjadi
	 * {@link FormatNilai}.
	 *
	 * <p><b>Getter berefek samping:</b> mengisi {@code 0.0} ke field bila masih {@code null}.</p>
	 *
	 * @return persentase kehadiran dalam satuan persen; tidak pernah {@code null}
	 */
	public Double getAbsen() {
		if (absen == null) {
			absen = 0.0;
		}
		return absen;
	}


	/**
	 * Menyetel persentase komponen absensi/kehadiran. Tanpa validasi.
	 *
	 * @param absen persentase dalam satuan persen
	 */
	public void setAbsen(Double absen) {
		this.absen = absen;
	}


	/**
	 * Mengembalikan persentase komponen <b>Tugas 1</b>, dipasangkan dengan {@link StatusPertemuan}
	 * {@code ConstantValues.TUGAS_1} saat dicetak menjadi {@link FormatNilai}.
	 *
	 * <p>Kelompok {@code tugas1..tugas5} menyediakan lima slot tugas berlabel bebas
	 * ({@link #getTugas1Label()} dan seterusnya), terpisah dari komponen {@code form} yang label
	 * defaultnya kebetulan juga {@code "Tugas"} &mdash; keduanya bisa aktif bersamaan.</p>
	 *
	 * <p>Sebuah slot hanya diterbitkan menjadi {@link FormatNilai} bila bobotnya {@code > 0,1};
	 * lihat {@link #setDefaultPembobotan(Perkuliahan, Session, boolean)}.</p>
	 *
	 * <p><b>Getter berefek samping:</b> mengisi {@code 0.0} ke field bila masih {@code null}.</p>
	 *
	 * @return persentase Tugas 1 dalam satuan persen; tidak pernah {@code null}
	 */
	public Double getTugas1() {
		if (tugas1 == null) {
			tugas1 = 0.0;
		}
		return tugas1;
	}


	/**
	 * Menyetel persentase komponen Tugas 1. Tanpa validasi.
	 *
	 * @param tugas1 persentase dalam satuan persen
	 * @see #getTugas1()
	 */
	public void setTugas1(Double tugas1) {
		this.tugas1 = tugas1;
	}


	/**
	 * Mengembalikan persentase komponen <b>Tugas 2</b> ({@code ConstantValues.TUGAS_2}).
	 * Perilakunya identik dengan {@link #getTugas1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Tugas 2 dalam satuan persen; tidak pernah {@code null}
	 * @see #getTugas1()
	 */
	public Double getTugas2() {
		if (tugas2 == null) {
			tugas2 = 0.0;
		}
		return tugas2;
	}


	/**
	 * Menyetel persentase komponen Tugas 2. Tanpa validasi.
	 *
	 * @param tugas2 persentase dalam satuan persen
	 * @see #getTugas1()
	 */
	public void setTugas2(Double tugas2) {
		this.tugas2 = tugas2;
	}


	/**
	 * Mengembalikan persentase komponen <b>Tugas 3</b> ({@code ConstantValues.TUGAS_3}).
	 * Perilakunya identik dengan {@link #getTugas1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Tugas 3 dalam satuan persen; tidak pernah {@code null}
	 * @see #getTugas1()
	 */
	public Double getTugas3() {
		if (tugas3 == null) {
			tugas3 = 0.0;
		}
		return tugas3;
	}


	/**
	 * Menyetel persentase komponen Tugas 3. Tanpa validasi.
	 *
	 * @param tugas3 persentase dalam satuan persen
	 * @see #getTugas1()
	 */
	public void setTugas3(Double tugas3) {
		this.tugas3 = tugas3;
	}


	/**
	 * Mengembalikan persentase komponen <b>Tugas 4</b> ({@code ConstantValues.TUGAS_4}).
	 * Perilakunya identik dengan {@link #getTugas1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Tugas 4 dalam satuan persen; tidak pernah {@code null}
	 * @see #getTugas1()
	 */
	public Double getTugas4() {
		if (tugas4 == null) {
			tugas4 = 0.0;
		}
		return tugas4;
	}


	/**
	 * Menyetel persentase komponen Tugas 4. Tanpa validasi.
	 *
	 * @param tugas4 persentase dalam satuan persen
	 * @see #getTugas1()
	 */
	public void setTugas4(Double tugas4) {
		this.tugas4 = tugas4;
	}


	/**
	 * Mengembalikan persentase komponen <b>Tugas 5</b> ({@code ConstantValues.TUGAS_5}).
	 * Perilakunya identik dengan {@link #getTugas1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Tugas 5 dalam satuan persen; tidak pernah {@code null}
	 * @see #getTugas1()
	 */
	public Double getTugas5() {
		if (tugas5 == null) {
			tugas5 = 0.0;
		}
		return tugas5;
	}


	/**
	 * Menyetel persentase komponen Tugas 5. Tanpa validasi.
	 *
	 * @param tugas5 persentase dalam satuan persen
	 * @see #getTugas1()
	 */
	public void setTugas5(Double tugas5) {
		this.tugas5 = tugas5;
	}


	/**
	 * Mengembalikan persentase komponen <b>Quiz 1</b>, dipasangkan dengan {@link StatusPertemuan}
	 * {@code ConstantValues.QUIZ_1} saat dicetak menjadi {@link FormatNilai}.
	 *
	 * <p>Kelompok {@code quiz1..quiz5} sepenuhnya sejajar dengan kelompok {@code tugas1..tugas5}:
	 * lima slot berlabel bebas, hanya diterbitkan sebagai komponen nilai bila bobotnya
	 * {@code > 0,1}.</p>
	 *
	 * <p><b>Getter berefek samping:</b> mengisi {@code 0.0} ke field bila masih {@code null}.</p>
	 *
	 * @return persentase Quiz 1 dalam satuan persen; tidak pernah {@code null}
	 * @see #getTugas1()
	 */
	public Double getQuiz1() {
		if (quiz1 == null) {
			quiz1 = 0.0;
		}
		return quiz1;
	}


	/**
	 * Menyetel persentase komponen Quiz 1. Tanpa validasi.
	 *
	 * @param quiz1 persentase dalam satuan persen
	 * @see #getQuiz1()
	 */
	public void setQuiz1(Double quiz1) {
		this.quiz1 = quiz1;
	}


	/**
	 * Mengembalikan persentase komponen <b>Quiz 2</b> ({@code ConstantValues.QUIZ_2}).
	 * Perilakunya identik dengan {@link #getQuiz1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Quiz 2 dalam satuan persen; tidak pernah {@code null}
	 * @see #getQuiz1()
	 */
	public Double getQuiz2() {
		if (quiz2 == null) {
			quiz2 = 0.0;
		}
		return quiz2;
	}


	/**
	 * Menyetel persentase komponen Quiz 2. Tanpa validasi.
	 *
	 * @param quiz2 persentase dalam satuan persen
	 * @see #getQuiz1()
	 */
	public void setQuiz2(Double quiz2) {
		this.quiz2 = quiz2;
	}


	/**
	 * Mengembalikan persentase komponen <b>Quiz 3</b> ({@code ConstantValues.QUIZ_3}).
	 * Perilakunya identik dengan {@link #getQuiz1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Quiz 3 dalam satuan persen; tidak pernah {@code null}
	 * @see #getQuiz1()
	 */
	public Double getQuiz3() {
		if (quiz3 == null) {
			quiz3 = 0.0;
		}
		return quiz3;
	}


	/**
	 * Menyetel persentase komponen Quiz 3. Tanpa validasi.
	 *
	 * @param quiz3 persentase dalam satuan persen
	 * @see #getQuiz1()
	 */
	public void setQuiz3(Double quiz3) {
		this.quiz3 = quiz3;
	}


	/**
	 * Mengembalikan persentase komponen <b>Quiz 4</b> ({@code ConstantValues.QUIZ_4}).
	 * Perilakunya identik dengan {@link #getQuiz1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Quiz 4 dalam satuan persen; tidak pernah {@code null}
	 * @see #getQuiz1()
	 */
	public Double getQuiz4() {
		if (quiz4 == null) {
			quiz4 = 0.0;
		}
		return quiz4;
	}


	/**
	 * Menyetel persentase komponen Quiz 4. Tanpa validasi.
	 *
	 * @param quiz4 persentase dalam satuan persen
	 * @see #getQuiz1()
	 */
	public void setQuiz4(Double quiz4) {
		this.quiz4 = quiz4;
	}


	/**
	 * Mengembalikan persentase komponen <b>Quiz 5</b> ({@code ConstantValues.QUIZ_5}).
	 * Perilakunya identik dengan {@link #getQuiz1()}, termasuk lazy default {@code 0.0}.
	 *
	 * @return persentase Quiz 5 dalam satuan persen; tidak pernah {@code null}
	 * @see #getQuiz1()
	 */
	public Double getQuiz5() {
		if (quiz5 == null) {
			quiz5 = 0.0;
		}
		return quiz5;
	}


	/**
	 * Menyetel persentase komponen Quiz 5. Tanpa validasi.
	 *
	 * @param quiz5 persentase dalam satuan persen
	 * @see #getQuiz1()
	 */
	public void setQuiz5(Double quiz5) {
		this.quiz5 = quiz5;
	}


	/**
	 * Mengembalikan label tampilan komponen absensi, default {@code "Absensi"}.
	 *
	 * <p>Label inilah yang muncul di {@link #getNama()} dan di layar entri nilai, sehingga tiap
	 * institusi bisa menamai komponen sesuai istilah lokalnya ("Kehadiran", "Presensi", dan
	 * sebagainya) tanpa mengubah struktur data.</p>
	 *
	 * <p><b>Getter berefek samping:</b> bila field masih {@code null}, nilai default
	 * <b>ditulis balik ke field</b> (bukan sekadar dikembalikan) sehingga akan ikut tersimpan
	 * ke database pada flush berikutnya. Pola ini berlaku untuk seluruh getter label di kelas
	 * ini.</p>
	 *
	 * @return label komponen absensi; tidak pernah {@code null}
	 */
	public String getAbsenLabel() {
		if (absenLabel == null) {
			absenLabel = "Absensi";
		}
		return absenLabel;
	}


	/**
	 * Menyetel label tampilan komponen absensi. Tanpa validasi; {@code null} akan dipulihkan ke
	 * default oleh {@link #getAbsenLabel()}, tetapi string kosong <b>tidak</b> (string kosong
	 * dianggap label yang sah).
	 *
	 * @param absenLabel label baru
	 */
	public void setAbsenLabel(String absenLabel) {
		this.absenLabel = absenLabel;
	}


	/**
	 * Mengembalikan label tampilan komponen {@code form}, default {@code "Tugas"}.
	 *
	 * <p><b>Sumber kebingungan klasik:</b> kolom database-nya bernama {@code form} tetapi
	 * labelnya "Tugas", dan di saat yang sama ada lima slot terpisah {@code tugas1..tugas5}
	 * berlabel "Tugas 1".."Tugas 5". Sebuah skema yang mengisi keduanya akan menampilkan
	 * "Tugas" dan "Tugas 1" berdampingan.</p>
	 *
	 * <p><b>Getter berefek samping:</b> lihat {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen tugas ({@code form}); tidak pernah {@code null}
	 */
	public String getFormLabel() {
		if (formLabel == null) {
			formLabel = "Tugas";
		}
		return formLabel;
	}


	/**
	 * Menyetel label tampilan komponen {@code form}.
	 *
	 * @param formLabel label baru
	 * @see #getFormLabel()
	 */
	public void setFormLabel(String formLabel) {
		this.formLabel = formLabel;
	}


	/**
	 * Mengembalikan label tampilan komponen UTS, default {@code "UTS"}.
	 *
	 * <p><b>Getter berefek samping:</b> lihat {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen UTS; tidak pernah {@code null}
	 */
	public String getUtsLabel() {
		if (utsLabel == null) {
			utsLabel = "UTS";
		}
		return utsLabel;
	}


	/**
	 * Menyetel label tampilan komponen UTS.
	 *
	 * @param utsLabel label baru
	 * @see #getAbsenLabel()
	 */
	public void setUtsLabel(String utsLabel) {
		this.utsLabel = utsLabel;
	}


	/**
	 * Mengembalikan label tampilan komponen UAS, default {@code "UAS"}.
	 *
	 * <p><b>Getter berefek samping:</b> lihat {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen UAS; tidak pernah {@code null}
	 */
	public String getUasLabel() {
		if (uasLabel == null) {
			uasLabel = "UAS";
		}
		return uasLabel;
	}


	/**
	 * Menyetel label tampilan komponen UAS.
	 *
	 * @param uasLabel label baru
	 * @see #getAbsenLabel()
	 */
	public void setUasLabel(String uasLabel) {
		this.uasLabel = uasLabel;
	}


	/**
	 * Mengembalikan label tampilan komponen Tugas 1, default {@code "Tugas 1"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Tugas 1; tidak pernah {@code null}
	 */
	public String getTugas1Label() {
		if (tugas1Label == null) {
			tugas1Label = "Tugas 1";
		}
		return tugas1Label;
	}


	/**
	 * Menyetel label tampilan komponen Tugas 1.
	 *
	 * @param tugas1Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setTugas1Label(String tugas1Label) {
		this.tugas1Label = tugas1Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Tugas 2, default {@code "Tugas 2"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Tugas 2; tidak pernah {@code null}
	 */
	public String getTugas2Label() {
		if (tugas2Label == null) {
			tugas2Label = "Tugas 2";
		}
		return tugas2Label;
	}


	/**
	 * Menyetel label tampilan komponen Tugas 2.
	 *
	 * @param tugas2Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setTugas2Label(String tugas2Label) {
		this.tugas2Label = tugas2Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Tugas 3, default {@code "Tugas 3"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Tugas 3; tidak pernah {@code null}
	 */
	public String getTugas3Label() {
		if (tugas3Label == null) {
			tugas3Label = "Tugas 3";
		}
		return tugas3Label;
	}


	/**
	 * Menyetel label tampilan komponen Tugas 3.
	 *
	 * @param tugas3Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setTugas3Label(String tugas3Label) {
		this.tugas3Label = tugas3Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Tugas 4, default {@code "Tugas 4"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Tugas 4; tidak pernah {@code null}
	 */
	public String getTugas4Label() {
		if (tugas4Label == null) {
			tugas4Label = "Tugas 4";
		}
		return tugas4Label;
	}


	/**
	 * Menyetel label tampilan komponen Tugas 4.
	 *
	 * @param tugas4Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setTugas4Label(String tugas4Label) {
		this.tugas4Label = tugas4Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Tugas 5, default {@code "Tugas 5"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Tugas 5; tidak pernah {@code null}
	 */
	public String getTugas5Label() {
		if (tugas5Label == null) {
			tugas5Label = "Tugas 5";
		}
		return tugas5Label;
	}


	/**
	 * Menyetel label tampilan komponen Tugas 5.
	 *
	 * @param tugas5Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setTugas5Label(String tugas5Label) {
		this.tugas5Label = tugas5Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Quiz 1, default {@code "Quiz 1"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Quiz 1; tidak pernah {@code null}
	 */
	public String getQuiz1Label() {
		if (quiz1Label == null) {
			quiz1Label = "Quiz 1";
		}
		return quiz1Label;
	}


	/**
	 * Menyetel label tampilan komponen Quiz 1.
	 *
	 * @param quiz1Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setQuiz1Label(String quiz1Label) {
		this.quiz1Label = quiz1Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Quiz 2, default {@code "Quiz 2"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Quiz 2; tidak pernah {@code null}
	 */
	public String getQuiz2Label() {
		if (quiz2Label == null) {
			quiz2Label = "Quiz 2";
		}
		return quiz2Label;
	}


	/**
	 * Menyetel label tampilan komponen Quiz 2.
	 *
	 * @param quiz2Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setQuiz2Label(String quiz2Label) {
		this.quiz2Label = quiz2Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Quiz 3, default {@code "Quiz 3"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Quiz 3; tidak pernah {@code null}
	 */
	public String getQuiz3Label() {
		if (quiz3Label == null) {
			quiz3Label = "Quiz 3";
		}
		return quiz3Label;
	}


	/**
	 * Menyetel label tampilan komponen Quiz 3.
	 *
	 * @param quiz3Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setQuiz3Label(String quiz3Label) {
		this.quiz3Label = quiz3Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Quiz 4, default {@code "Quiz 4"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Quiz 4; tidak pernah {@code null}
	 */
	public String getQuiz4Label() {
		if (quiz4Label == null) {
			quiz4Label = "Quiz 4";
		}
		return quiz4Label;
	}


	/**
	 * Menyetel label tampilan komponen Quiz 4.
	 *
	 * @param quiz4Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setQuiz4Label(String quiz4Label) {
		this.quiz4Label = quiz4Label;
	}


	/**
	 * Mengembalikan label tampilan komponen Quiz 5, default {@code "Quiz 5"}.
	 *
	 * <p><b>Getter berefek samping:</b> nilai default ditulis balik ke field; lihat
	 * {@link #getAbsenLabel()}.</p>
	 *
	 * @return label komponen Quiz 5; tidak pernah {@code null}
	 */
	public String getQuiz5Label() {
		if (quiz5Label == null) {
			quiz5Label = "Quiz 5";
		}
		return quiz5Label;
	}


	/**
	 * Menyetel label tampilan komponen Quiz 5.
	 *
	 * @param quiz5Label label baru
	 * @see #getAbsenLabel()
	 */
	public void setQuiz5Label(String quiz5Label) {
		this.quiz5Label = quiz5Label;
	}


	/**
	 * Mengembalikan {@link Dosen} pemilik/pembuat format pembobotan ini (kolom
	 * {@code dimiliki_oleh}), diisi otomatis oleh layar pengelola dari dosen pengguna yang sedang
	 * login.
	 *
	 * <p>Dipakai untuk memisahkan format buatan dosen tertentu dari format milik kampus.
	 * Boleh {@code null} untuk format kampus/warisan data lama.</p>
	 *
	 * <p>Relasi {@code LAZY}, jadi nilai baliknya bisa berupa proxy Hibernate. Karena itu getter
	 * melewatkannya lewat {@link GeneralValueObject#check(Object)} yang meresolusi proxy tersebut
	 * (memakai cache identitas entity bila sesi asalnya sudah tertutup) dan <b>menulis balik hasil
	 * resolusi ke field</b> &mdash; jadi getter ini pun berefek samping.</p>
	 *
	 * <p>Cascade dibatasi {@code PERSIST} dan {@code MERGE}: menyimpan format tidak pernah
	 * menghapus atau melepas data dosen.</p>
	 *
	 * @return dosen pemilik format, atau {@code null} bila format milik kampus
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dimiliki_oleh", nullable = true)
	public Dosen getDimilikiOleh() {
		dimilikiOleh = check(dimilikiOleh);
		return dimilikiOleh;
	}


	/**
	 * Menyetel dosen pemilik/pembuat format. Tanpa validasi; {@code null} diterima dan berarti
	 * "format milik kampus".
	 *
	 * @param dimilikiOleh dosen pemilik, boleh {@code null}
	 */
	public void setDimilikiOleh(Dosen dimilikiOleh) {
		this.dimilikiOleh = dimilikiOleh;
	}


	/**
	 * Mengembalikan penanda apakah format ini masih boleh dipilih/dipakai.
	 *
	 * <p>Dipakai antara lain oleh {@link Perkuliahan#getPembombotanNilai()} saat mencari format
	 * yang diwajibkan pada tahun akademik + semester tertentu: hanya format {@code aktif} yang
	 * dipertimbangkan. Menonaktifkan format lebih aman daripada menghapusnya, karena kelas lama
	 * masih menunjuk baris ini.</p>
	 *
	 * <p><b>Getter berefek samping:</b> mengisi {@code true} ke field bila masih {@code null},
	 * sehingga data lama yang kolomnya {@code NULL} otomatis dianggap aktif.</p>
	 *
	 * @return {@code true} bila format masih aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}


	/**
	 * Menyetel penanda aktif. Tanpa validasi; {@code null} akan terbaca sebagai {@code true} oleh
	 * {@link #getAktif()}.
	 *
	 * @param aktif {@code false} untuk menonaktifkan format tanpa menghapusnya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}


	/**
	 * Mengembalikan penanda bahwa format ini <b>diwajibkan</b> pada satu tahun akademik dan
	 * semester tertentu.
	 *
	 * <p>Bila menyala, {@link Perkuliahan#getPembombotanNilai()} akan menelusuri seluruh cache
	 * {@code PembombotanNilai} dan memakai format ini untuk <b>semua</b> kelas yang tahun ajaran
	 * dan ganjil/genapnya cocok &mdash; mengalahkan format yang dipilih per kelas. Ini mekanisme
	 * kebijakan kampus, mis. "mulai 2025/2026 ganjil semua kelas wajib memakai komposisi X".
	 * Bendera ini juga yang mengendalikan apakah {@link #getTahunAkadmeik()} dan
	 * {@link #getSemester()} menyimpan nilai atau justru dikosongkan.</p>
	 *
	 * <p>Berbeda dari getter boolean lain di kelas ini, method ini <b>tidak</b> menulis balik ke
	 * field &mdash; {@code null} hanya diterjemahkan menjadi {@code false} pada nilai balik.</p>
	 *
	 * @return {@code true} bila format diwajibkan pada periode tertentu; tidak pernah {@code null}
	 */
	public Boolean getWajibDitahunAkademikDanSemesterTertentu() {
		return wajibDitahunAkademikDanSemesterTertentu == null ? false : wajibDitahunAkademikDanSemesterTertentu;
	}


	/**
	 * Menyetel penanda "wajib pada tahun akademik dan semester tertentu".
	 *
	 * <p>Perhatikan efeknya tidak berhenti di sini: mematikan bendera ini membuat
	 * {@link #getTahunAkadmeik()} dan {@link #getSemester()} <b>mengosongkan</b> nilai yang sudah
	 * tersimpan pada pembacaan berikutnya.</p>
	 *
	 * @param wajibDitahunAkademikDanSemesterTertentu {@code true} untuk mewajibkan format pada
	 *        periode yang ditunjuk {@link #getTahunAkadmeik()} dan {@link #getSemester()}
	 */
	public void setWajibDitahunAkademikDanSemesterTertentu(Boolean wajibDitahunAkademikDanSemesterTertentu) {
		this.wajibDitahunAkademikDanSemesterTertentu = wajibDitahunAkademikDanSemesterTertentu;
	}


	/**
	 * Mengembalikan tahun akademik tempat format ini diwajibkan.
	 *
	 * <p><b>Nama method mengandung typo historis</b>: {@code TahunAkadmeik}, seharusnya
	 * {@code TahunAkademik}. Typo ini merambat ke nama properti Hibernate dan kolomnya, jadi
	 * dipertahankan apa adanya.</p>
	 *
	 * <p><b>PERINGATAN &mdash; getter ini bisa MENGHAPUS DATA.</b> Perilakunya bercabang pada
	 * {@link #getWajibDitahunAkademikDanSemesterTertentu()}:</p>
	 * <ul>
	 *   <li>bendera menyala &rarr; bila field masih kosong, diisi tahun akademik berjalan
	 *   ({@code Common.getCurrentTahunAkademik()}) dan <b>ditulis balik ke field</b>;</li>
	 *   <li>bendera padam &rarr; field <b>disetel {@code null}</b>. Nilai tahun akademik yang
	 *   sudah tersimpan hilang hanya karena dibaca, dan pada entity terkelola perubahan itu ikut
	 *   ter-{@code UPDATE} ke database saat flush.</li>
	 * </ul>
	 *
	 * <p>Konsekuensi praktisnya: mematikan bendera "wajib" lalu menyalakannya lagi <b>tidak</b>
	 * mengembalikan periode lama &mdash; periode harus diisi ulang.</p>
	 *
	 * @return tahun akademik (mis. {@code "2025/2026"}) bila format diwajibkan; {@code null} bila
	 *         tidak
	 * @see #getSemester()
	 */
	public String getTahunAkadmeik() {
		if (getWajibDitahunAkademikDanSemesterTertentu()) {
			tahunAkadmeik = tahunAkadmeik == null || tahunAkadmeik.trim().isEmpty() ? Common.getCurrentTahunAkademik()
					: tahunAkadmeik;
		} else {
			if (tahunAkadmeik != null) {
				tahunAkadmeik = null;
			}
		}
		return tahunAkadmeik;
	}


	/**
	 * Menyetel tahun akademik tempat format diwajibkan. Tanpa validasi format string.
	 *
	 * <p>Nilai ini hanya bertahan selama
	 * {@link #getWajibDitahunAkademikDanSemesterTertentu()} bernilai {@code true}; lihat
	 * peringatan pada {@link #getTahunAkadmeik()}.</p>
	 *
	 * @param tahunAkadmeik tahun akademik, mis. {@code "2025/2026"}
	 */
	public void setTahunAkadmeik(String tahunAkadmeik) {
		this.tahunAkadmeik = tahunAkadmeik;
	}


	/**
	 * Mengembalikan semester (ganjil/genap) tempat format ini diwajibkan.
	 *
	 * <p>Perilakunya cermin persis {@link #getTahunAkadmeik()}, termasuk sifat merusaknya: bila
	 * {@link #getWajibDitahunAkademikDanSemesterTertentu()} padam, field <b>dikosongkan</b> saat
	 * dibaca. Bila menyala dan field masih kosong, diisi semester berjalan &mdash;
	 * {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP} sesuai
	 * {@code Common.isNowSemensterGanjil()} (nama helper itu pun mengandung typo
	 * "semenster", dipertahankan).</p>
	 *
	 * @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP} bila format diwajibkan;
	 *         {@code null} bila tidak
	 * @see #getTahunAkadmeik()
	 */
	public String getSemester() {
		if (getWajibDitahunAkademikDanSemesterTertentu()) {
			semester = semester == null || semester.trim().isEmpty()
					? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
					: semester;
		} else {
			if (semester != null) {
				semester = null;
			}
		}
		return semester;
	}


	/**
	 * Menyetel semester tempat format diwajibkan. Tanpa validasi terhadap
	 * {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}.
	 *
	 * @param semester nama semester; nilainya dibandingkan tanpa peduli besar-kecil huruf oleh
	 *        {@link Perkuliahan#getPembombotanNilai()}
	 * @see #getSemester()
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}


	/**
	 * Mengembalikan keterangan bebas yang diisi pengelola untuk menjelaskan format ini.
	 *
	 * <p><b>Perhatian &mdash; kontraknya berbeda dari induk.</b>
	 * {@link GeneralValueObject#getKeterangan()} menormalkan {@code null} menjadi {@code ""} dan
	 * berjanji tidak pernah mengembalikan {@code null}; method di kelas ini mengembalikan field
	 * apa adanya sehingga <b>bisa {@code null}</b>. Kode yang mengandalkan janji induk (termasuk
	 * cabang terakhir {@link GeneralValueObject#compareTo(GeneralValueObject)}) berperilaku
	 * berbeda untuk entity ini. Field {@code keterangan} di sini juga meng-<i>shadow</i> milik
	 * induk.</p>
	 *
	 * @return keterangan bebas, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}


	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
