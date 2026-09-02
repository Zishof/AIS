package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

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
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.database.model.obe.CapaianPembelajaranLulusan;

/**
 * Satu <b>komponen (butir) penilaian</b> milik sebuah kelas perkuliahan reguler &mdash; satu baris
 * tabel {@code public.formatnilai}. Kalau {@link PembombotanNilai} adalah <i>cetakan</i>
 * ("Absensi 10%, Tugas 20%, UTS 30%, UAS 40%"), maka kelas ini adalah <i>hasil cetakannya</i>:
 * baris nyata per {@link Perkuliahan}, yang kemudian dipakai layar entri nilai, tugas, ujian,
 * rekap, dan ekspor Neo Feeder sebagai kolom-kolom penilaian yang benar-benar ada.
 *
 * <p>Isi tiap baris pada dasarnya cuma empat hal: <b>milik kelas mana</b>
 * ({@link #getPerkuliahan()}), <b>komponen apa</b> ({@link #getStatusPertemuan()}), <b>berapa
 * persen bobotnya</b> ({@link #getPersen()}), dan <b>apa namanya di layar</b>
 * ({@link #getNama()}). Sisanya adalah pelengkap: klasifikasi jenis evaluasi untuk pelaporan,
 * pengunci entri nilai, penanda id Feeder, dan sepasang properti untuk mode OBE.</p>
 *
 * <h3>Dua mode: konvensional dan OBE</h3>
 * <ul>
 * <li><b>Konvensional</b> &mdash; komponen ditentukan oleh {@link StatusPertemuan} baku dengan id
 * yang <b>dipatok keras</b>: {@code 1} Absen, {@code 2} Form/Tugas, {@code 3} UTS, {@code 4} UAS,
 * {@code 21}..{@code 25} Tugas 1..5, {@code 31}..{@code 35} Quiz 1..5 (lihat
 * {@link #ambilNama(StatusPertemuan, Perkuliahan)} dan konstanta {@code ConstantValues.ABSEN},
 * {@code FORM}, {@code UTS}, {@code UAS}, {@code TUGAS_1}.., {@code QUIZ_1}..). Nama tampilnya
 * diambil dari label yang bisa diubah tiap institusi pada {@link PembombotanNilai}.</li>
 * <li><b>OBE</b> &mdash; komponen mewakili CPMK atau Sub-CPMK. Penanda modenya adalah
 * {@link #getCapaianPembelajaranLulusan()} terisi <b>atau</b> {@link #getKodeSubCpmk()} tidak
 * kosong; nama diturunkan dari formula JSON milik CPL/CPMK, dan ambang kelulusan komponen dibaca
 * lewat {@link #ambilMinimal()}.</li>
 * </ul>
 *
 * <h3>Dari mana baris ini datang, dan ke mana perginya</h3>
 * <p>Hampir semua baris <b>tidak</b> dibuat manual, melainkan dicetak oleh
 * {@code PembombotanNilai.setDefaultPembobotan(Perkuliahan, Session, boolean)} &mdash; satu-satunya
 * jembatan antara skema pembobotan dan data penilaian nyata &mdash; yang dipanggil lewat
 * {@code Perkuliahan.ambilFormatNilai(Session, boolean, boolean)}. Perlu diingat: proses cetak
 * ulang <b>menolkan</b> persen komponen lama, bukan menghapus barisnya, supaya nilai mahasiswa
 * yang sudah terlanjur terisi tidak ikut hilang. Jalur pembuatan lain yang lebih jarang:
 * {@code CommonAcademicSyncHelper} (sinkronisasi kelas dari sistem luar) dan layar master
 * {@code ais.action.master.FormatNilaiAction}.</p>
 *
 * <p>Selain disimpan di tabel, tiap baris juga didaftarkan ke <i>flag store</i> berkas milik
 * kelasnya lewat {@code Perkuliahan.populateFormatNilai(FormatNilai, boolean)}, yang memetakan
 * id komponen ke lokasi berkas hasil {@code write()} (lihat
 * {@link ais.database.model.GeneralValueObject}).</p>
 *
 * <p>Yang menunjuk balik ke baris ini lewat kolom {@code format_nilai}: {@link Pertemuan},
 * {@link PertemuanPunyaUjian}, {@link TugasPertemuan}, {@link TugasKelompok}, {@link Skripsi},
 * {@link MahasiswaRequestTugasAkhir}, dan {@link NilaiTemporary}. Konsumen utamanya adalah
 * {@code DetailperkuliahanForPenilaianHelper} (grid entri nilai + tombol kunci/buka kunci),
 * {@code PenilaianHelper}, {@code NilaiObeAction}, {@code RekapHasilTugasPerTugasDanUjianObe},
 * serta {@code FeederExporter}/{@code EksporNilaiFeeder} untuk pelaporan PDDikti.</p>
 *
 * <h3>HASIL VERIFIKASI: bug "slot dosen 1/2 tertukar" TIDAK ADA di kelas ini</h3>
 * <p>Kelas {@link FormatNilaiSkripsi} adalah induk masalah penamaan bobot yang tergeser satu
 * langkah terhadap slot dosen ({@code dosen1} dibobot oleh {@code prosentasiNilaiKetuaSidang},
 * {@code dosen2} oleh {@code prosentasiNilaiPembimbing}), plus {@link VOPembelajaran} yang
 * memetakan ke arah sebaliknya. <b>Cacat itu tidak mungkin muncul di sini</b>, dan buktinya
 * struktural, bukan sekadar "kelihatannya tidak ada":</p>
 * <ol>
 * <li>Seluruh berkas ini <b>nol</b> kemunculan kata {@code dosen}, {@code penguji},
 * {@code pembimbing}, {@code ketua}, maupun {@code prosentasi} &mdash; dalam bentuk apa pun,
 * termasuk komentar. Tidak ada slot dosen untuk ditukar.</li>
 * <li>Bobot di sini disimpan sebagai <b>satu</b> skalar {@link #getPersen()} <b>per baris</b>,
 * bukan sebagai deretan kolom {@code nilai_ketua_sidang}/{@code nilai_pembimbing}/
 * {@code nilai_pengujiN} yang sejajar dengan deretan label peran. Bentuk "N kolom bobot sejajar
 * N kolom label" &mdash; satu-satunya bentuk yang bisa tergeser &mdash; sama sekali tidak ada.</li>
 * <li>Di sisi hulu pun tidak ada jalurnya: {@code PembombotanNilai.setDefaultPembobotan(...)}
 * <b>tidak pernah</b> menerbitkan komponen {@code dosen1}..{@code dosen5} miliknya menjadi
 * {@link FormatNilai}. Bobot per dosen penguji untuk Tugas Akhir/KKN/PKL dibaca langsung dari
 * entity pembobotan oleh modul masing-masing, tidak lewat kelas ini.</li>
 * </ol>
 * <p>Jadi: penilaian per peran dosen adalah urusan {@link FormatNilaiSkripsi} dan kerabatnya;
 * kelas ini murni urusan komponen nilai perkuliahan reguler. Jangan menyalin catatan bug dari
 * berkas skripsi ke sini.</p>
 *
 * <h3>Peringatan: banyak getter di sini BERUBAH-UBAH (berefek samping)</h3>
 * <p>Kelas ini dipetakan {@code dynamicUpdate = true}. Artinya, getter yang menulis balik ke field
 * <b>terpetakan</b> bisa membuat Hibernate meng-{@code flush} perubahan ke database walau pengguna
 * tidak menekan tombol simpan apa pun. Hasil penelusuran menyeluruh atas berkas ini:</p>
 * <ul>
 * <li><b>Menulis field TERPETAKAN (rawan tulis diam-diam ke DB): 4</b> &mdash;
 * {@link #getPersen()} ({@code null} &rarr; {@code 0.0}), {@link #getNama()} (menimpa nama tiap
 * kali dibaca, kecuali nama OBE buatan pengguna), {@link #getNomorUrut()} (menimpa dari JSON
 * urutan milik {@link PembombotanNilai}, <b>hanya untuk kelas non-OBE</b>), dan
 * {@link #getJenisEvaluasi()} (menebak jenis evaluasi dari teks nama, lalu jatuh ke
 * {@code ConstantValues.Tugas} bila tetap kosong).</li>
 * <li><b>Menulis balik referensi relasi ({@code check()}): 5</b> &mdash; semua relasi:
 * {@link #getPerkuliahan()}, {@link #getStatusPertemuan()}, {@link #getKunci()},
 * {@link #getJenisEvaluasi()}, {@link #getCapaianPembelajaranLulusan()}. Ini pola baku repo,
 * lihat {@link ais.database.model.GeneralValueObject#check(Object)}.</li>
 * <li><b>Getter yang menutup sesi Hibernate: 0</b> &mdash; berkas ini tidak menyentuh
 * {@code Session}, {@code HibernateUtil}, maupun {@code Criteria} sama sekali (nol kemunculan).
 * Jalur tak langsungnya hanya lewat {@code check()}, yang membuka dan menutup sesinya sendiri.</li>
 * <li><b>Getter destruktif (membuang data yang sudah ada): 0.</b> Yang paling mendekati adalah
 * {@link #getNama()}, tetapi ia hanya menimpa nama <i>turunan</i>; nama OBE yang diketik pengguna
 * sengaja dipertahankan.</li>
 * </ul>
 * <p>Karena itu, kode yang hanya ingin <i>melihat</i> isi baris (laporan, ekspor, perbandingan)
 * sebaiknya sadar bahwa memanggil getternya bisa mengubah baris tersebut.</p>
 *
 * <h3>Kenapa {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah} dideklarasikan
 * ulang di sini</h3>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Mendeklarasikan ulang keempat properti audit itu di setiap entity
 * bukan duplikasi ceroboh, melainkan <b>keharusan teknis</b> agar kolomnya benar-benar terpetakan.
 * Jangan "dirapikan" dengan menariknya ke induk.</p>
 *
 * <h3>Kuirk lain yang perlu diketahui</h3>
 * <ul>
 * <li>{@link #ambilNama(StatusPertemuan, Perkuliahan)} memanggil
 * {@code perkuliahan.getPembombotanNilai().getXxxLabel()} <b>tanpa</b> memeriksa hasil
 * {@code getPembombotanNilai()}. Praktis aman karena getter itu punya nilai baku berlapis, tetapi
 * pemanggil langsung (bukan lewat {@link #getNama()}) tetap menanggung risiko {@code NPE}.</li>
 * <li>Ambang panjang teks {@value #PANJANG_STRING_STANDAR} dipaksakan oleh
 * {@code potongStringStandar(String)} pada {@code nama} dan {@code kodeSubCpmk} &mdash; potongan
 * dilakukan <b>diam-diam</b>, tanpa peringatan ke pengguna.</li>
 * <li>{@link #compareTo(GeneralValueObject)} membungkus seluruh badannya dalam {@code try/catch}
 * dan mengembalikan {@code 0} bila gagal, sehingga pengurutan yang bermasalah tampak sebagai
 * "semua sama" alih-alih melempar kesalahan.</li>
 * <li>{@code hbm2java} menandai kelas ini dengan komentar bawaan <i>"Absen generated by
 * hbm2java"</i> &mdash; sisa generator Hibernate Tools 2009, bukan keterangan isi kelas.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b>: {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()} beserta setternya dan kait {@code @PreUpdate}
 * {@code onUpdate()}.</li>
 * <li><b>Identitas &amp; relasi</b>: {@link #getId()}, {@link #getPerkuliahan()},
 * {@link #getStatusPertemuan()}, {@link #getKunci()}, {@link #getJenisEvaluasi()},
 * {@link #getCapaianPembelajaranLulusan()}.</li>
 * <li><b>Isi komponen</b>: {@link #getPersen()}, {@link #getNama()}, {@link #getNomorUrut()},
 * {@link #getKodeSubCpmk()}, {@link #getFeeder()}.</li>
 * <li><b>Logika nyata</b>: {@link #compareTo(GeneralValueObject)},
 * {@link #ambilNama(StatusPertemuan, Perkuliahan)}, {@link #getNama()},
 * {@link #getJenisEvaluasi()}, {@link #getNomorUrut()}, {@link #ambilMinimal()}, serta pembantu
 * privat {@code hanyaAngka(String)}, {@code ambilNamaObeDariFormula()},
 * {@code potongStringStandar(String)}.</li>
 * </ul>
 *
 * @see PembombotanNilai#setDefaultPembobotan(Perkuliahan, org.hibernate.Session, boolean)
 * @see Perkuliahan#ambilFormatNilai(org.hibernate.Session, boolean, boolean)
 * @see StatusPertemuan
 * @see FormatNilaiSkripsi
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "formatnilai")
public class FormatNilai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipatok agar instance yang sudah tersimpan
	 * (mis. di flag store berkas hasil {@code write()} atau di sesi ZK) tetap bisa
	 * dibaca ulang meski kelasnya bertambah anggota. Jangan diubah.
	 */
	private static final long serialVersionUID = 4138996528850625293L;
	/** Kunci utama tabel {@code formatnilai}; kolom {@code id}, {@code IDENTITY}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit warisan). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit warisan). */
	private String olehId;

	/**
	 * Mengurutkan komponen penilaian di dalam satu kelas perkuliahan.
	 *
	 * <p>Urutan yang dipakai berlapis, dan lapisan pertamalah yang menentukan:</p>
	 * <ol>
	 * <li>bila <b>kedua</b> baris punya {@link #getNomorUrut()}, dibandingkan berdasarkan nomor urut
	 * komponen &mdash; ini jalur normal, karena {@link #getNomorUrut()} tidak pernah mengembalikan
	 * {@code null} (jatuh ke {@code 1});</li>
	 * <li>selain itu, bila kedua baris punya {@link StatusPertemuan}, dibandingkan berdasarkan nomor
	 * urut master {@link StatusPertemuan};</li>
	 * <li>selain itu, diserahkan ke {@code super.compareTo(...)} milik
	 * {@link ais.database.model.GeneralValueObject}.</li>
	 * </ol>
	 *
	 * <p>Argumen yang bukan {@link FormatNilai} juga langsung diserahkan ke induk.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getNomorUrut()} dan {@link #getStatusPertemuan()},
	 * jadi perbandingan ini <b>bisa menulis</b> ke field terpetakan (lihat catatan getter berefek
	 * samping pada Javadoc kelas). Menyortir sebuah {@code List<FormatNilai>} karenanya bukan operasi
	 * yang benar-benar bebas efek samping.</p>
	 *
	 * <p><b>Jebakan:</b> seluruh badan dibungkus {@code try/catch}. Bila terjadi kesalahan (mis.
	 * proxy lazy gagal dimuat karena sesi sudah tertutup), method mengembalikan {@code 0} &mdash;
	 * "dianggap sama" &mdash; sehingga urutan tampil bisa berantakan tanpa satu pun pesan kesalahan
	 * ke pengguna. Kesalahannya hanya tercatat lewat {@code ErrorAuditUtil}.</p>
	 *
	 * @param arg0 objek pembanding; boleh sembarang {@link ais.database.model.GeneralValueObject}
	 *             dan boleh {@code null} (ditangani oleh induk / blok {@code catch}).
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} juga dikembalikan bila
	 *         terjadi kesalahan.
	 * @see #getNomorUrut()
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (arg0 instanceof FormatNilai) {
				FormatNilai formatNilai = (FormatNilai) arg0;

				if (formatNilai != null && formatNilai.getNomorUrut() != null && getNomorUrut() != null) {
					return getNomorUrut().compareTo(formatNilai.getNomorUrut());
				} else if (formatNilai != null && formatNilai.getStatusPertemuan() != null
						&& getStatusPertemuan() != null) {
					return getStatusPertemuan().getNomorUrut()
							.compareTo(formatNilai.getStatusPertemuan().getNomorUrut());
				} else {
					return super.compareTo(arg0);
				}
			} else {
				return super.compareTo(arg0);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/FormatNilai.java:63");

		}

		return 0;
	}

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau kosong <b>diabaikan diam-diam</b> &mdash; nilai
	 * lama dipertahankan. Jadi jejak audit tidak bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau kosong <b>diabaikan diam-diam</b>, sama seperti
	 * {@link #setOlehId(String)}.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dijalankan Hibernate tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan seluruh pekerjaan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi ulang
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dari pengguna yang sedang aktif. Tidak
	 * pernah dipanggil langsung dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diberi nilai awal waktu server saat objek dibuat, lalu ditimpa oleh
	 * {@code onUpdate()} pada tiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya diisi oleh interceptor audit, bukan oleh kode layar.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan; tidak pernah {@code null} untuk objek yang baru dibuat.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk pencatatan/penelusuran: {@code perkuliahan_persen_statusPertemuan}.
	 *
	 * <p><b>Bukan</b> teks yang layak ditampilkan ke pengguna &mdash; untuk itu pakai
	 * {@link #getNama()}.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getPerkuliahan()} dan {@link #getStatusPertemuan()}
	 * lebih dulu supaya proxy lazy-nya sudah teresolusi saat dirangkai; keduanya menulis balik ke
	 * field. Nilai yang dirangkai kemudian dibaca dari <b>field</b>, bukan dari getter, sehingga hasil
	 * {@code toString()} bergantung pada {@code toString()} milik {@link Perkuliahan} dan
	 * {@link StatusPertemuan}.</p>
	 *
	 * @return teks gabungan kelas, persen, dan jenis komponen.
	 */
	public String toString() {
		getPerkuliahan();
		getStatusPertemuan();
		return perkuliahan + "_" + persen + "_" + statusPertemuan;
	}

	/** Kelas perkuliahan pemilik komponen ini; kolom {@code perkuliahan}. */
	private Perkuliahan perkuliahan;
	/** Bobot komponen dalam persen terhadap nilai akhir; kolom {@code persen}, {@code NOT NULL}. */
	private Double persen = 0.0;
	/** Jenis komponen (Absen/Tugas/UTS/UAS/Quiz/CPMK); kolom {@code status_pertemuan}, {@code NOT NULL}. */
	private StatusPertemuan statusPertemuan;
	/** Pengguna yang sedang mengunci entri nilai komponen ini; kolom {@code kunci}. */
	private Tbmuser kunci;
	/** Nama tampil komponen. Sebagian besar diturunkan otomatis &mdash; lihat {@link #getNama()}. */
	private String nama;
	/** Nomor urut tampil komponen di dalam kelasnya; lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Klasifikasi jenis evaluasi untuk pelaporan/OBE; kolom {@code jenis_evaluasi}. */
	private JenisEvaluasi jenisEvaluasi;
	/** Id komponen evaluasi padanan di Neo Feeder (PDDikti); diisi oleh {@code FeederExporter}. */
	private String feeder;
	/** CPL/CPMK yang diwakili komponen ini pada mode OBE; kolom {@code capaian_pembelajaran_lulusan}. */
	private CapaianPembelajaranLulusan capaianPembelajaranLulusan;
	/** Kunci Sub-CPMK di dalam formula JSON milik CPL; penanda kedua mode OBE. */
	private String kodeSubCpmk;
	/** Batas panjang teks untuk {@code nama} dan {@code kodeSubCpmk}; lihat {@code potongStringStandar(String)}. */
	private static final int PANJANG_STRING_STANDAR = 255;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Objek hasilnya belum terhubung ke kelas
	 * perkuliahan mana pun dan {@link #getPersen()}-nya {@code 0.0}.
	 */
	public FormatNilai() {
	}

	/**
	 * Konstruktor pintas untuk merujuk baris yang sudah ada berdasarkan kuncinya.
	 *
	 * <p>Tidak membaca apa pun dari database &mdash; hanya mengisi field {@code id}. Dipakai untuk
	 * membuat referensi ringan pada query/relasi.</p>
	 *
	 * @param id kunci utama baris {@code formatnilai}.
	 */
	public FormatNilai(Long id) {
		this.id = id;
	}

	/**
	 * Kunci utama baris ini.
	 *
	 * @return id komponen penilaian; {@code null} untuk objek yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Umumnya hanya dipakai Hibernate atau kode sinkronisasi.
	 *
	 * @param id kunci utama baris {@code formatnilai}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menyetel kelas perkuliahan pemilik komponen ini.
	 *
	 * @param perkuliahan kelas pemilik; boleh {@code null} (kolomnya memang {@code nullable}).
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Kelas perkuliahan pemilik komponen ini.
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field &mdash; pola baku
	 * resolusi proxy lazy di repo ini. Bila proxy sudah terlanjur detached, {@code check()} bisa
	 * membuka sesi Hibernate sendiri untuk memuat ulang entity.</p>
	 *
	 * @return kelas perkuliahan pemilik, atau {@code null} bila kolomnya kosong.
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		perkuliahan = check(perkuliahan);
		return this.perkuliahan;
	}

	/**
	 * Bobot komponen ini dalam persen terhadap nilai akhir mahasiswa.
	 *
	 * <p>Nilai {@code 0.0} berarti komponen sedang tidak dipakai. Ingat bahwa
	 * {@code PembombotanNilai.setDefaultPembobotan(...)} <b>menolkan</b> komponen lama alih-alih
	 * menghapusnya, jadi baris berpersen {@code 0.0} adalah keadaan yang lumrah dan bukan tanda data
	 * rusak.</p>
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, getter ini <b>menulis</b> {@code 0.0}
	 * ke field terpetakan. Pada instance yang masih dikelola sesi, pembacaan biasa karenanya bisa
	 * menimbulkan {@code UPDATE} tanpa aksi simpan dari pengguna. Penjagaan ini memang perlu karena
	 * kolomnya {@code NOT NULL}.</p>
	 *
	 * @return bobot dalam persen; tidak pernah {@code null}.
	 */
	@Column(name = "persen", nullable = false)
	public Double getPersen() {
		if (persen == null) {
			persen = 0.0;
		}
		return this.persen;
	}

	/**
	 * Menyetel bobot komponen dalam persen.
	 *
	 * <p>Tidak ada validasi rentang di sini: pemeriksaan agar total seluruh komponen mencapai 100%
	 * dilakukan di layar pengelola dan di {@link PembombotanNilai}, bukan di entity.</p>
	 *
	 * @param persen bobot dalam persen; {@code null} akan dinormalkan menjadi {@code 0.0} saat
	 *               dibaca kembali lewat {@link #getPersen()}.
	 */
	public void setPersen(Double persen) {
		this.persen = persen;
	}

	/**
	 * Menyetel jenis komponen penilaian.
	 *
	 * @param statusPertemuan master jenis komponen; secara skema <b>tidak boleh</b> {@code null},
	 *                        karena kolom {@code status_pertemuan} bersifat {@code NOT NULL}.
	 */
	public void setStatusPertemuan(StatusPertemuan statusPertemuan) {
		this.statusPertemuan = statusPertemuan;
	}

	/**
	 * Jenis komponen penilaian yang diwakili baris ini (Absen, Tugas, UTS, UAS, Quiz, atau turunan
	 * CPMK pada mode OBE).
	 *
	 * <p>Id master inilah yang dipatok keras di
	 * {@link #ambilNama(StatusPertemuan, Perkuliahan)} untuk memilih label mana yang dipakai.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field.</p>
	 *
	 * @return master jenis komponen; secara skema tidak boleh {@code null}, walau data lama hasil bug
	 *         sesi-tertutup pernah ditemukan kosong dan diperbaiki di
	 *         {@code PembombotanNilai.setDefaultPembobotan(...)}.
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pertemuan", nullable = false)
	public StatusPertemuan getStatusPertemuan() {
		statusPertemuan = check(statusPertemuan);
		return statusPertemuan;
	}

	/**
	 * Menerjemahkan sepasang ({@link StatusPertemuan}, {@link Perkuliahan}) menjadi <b>label tampil</b>
	 * komponen penilaian sesuai istilah yang dipilih institusi.
	 *
	 * <p>Nama baku sebuah komponen ada di master {@link StatusPertemuan}, tetapi tiap kampus boleh
	 * menamainya lain ("Kehadiran" alih-alih "Absen", "Tugas Terstruktur" alih-alih "Tugas"). Label
	 * pengganti itu disimpan di skema pembobotan kelas yang bersangkutan, dan method inilah yang
	 * memilihnya berdasarkan <b>id master yang dipatok keras</b>:</p>
	 * <table border="1" summary="Pemetaan id StatusPertemuan ke label PembombotanNilai">
	 * <tr><th>Id</th><th>Komponen</th><th>Sumber label</th></tr>
	 * <tr><td>1</td><td>Absen</td><td>{@code getAbsenLabel()}</td></tr>
	 * <tr><td>2</td><td>Form/Tugas</td><td>{@code getFormLabel()}</td></tr>
	 * <tr><td>3</td><td>UTS</td><td>{@code getUtsLabel()}</td></tr>
	 * <tr><td>4</td><td>UAS</td><td>{@code getUasLabel()}</td></tr>
	 * <tr><td>21..25</td><td>Tugas 1..5</td><td>{@code getTugas1Label()}..{@code getTugas5Label()}</td></tr>
	 * <tr><td>31..35</td><td>Quiz 1..5</td><td>{@code getQuiz1Label()}..{@code getQuiz5Label()}</td></tr>
	 * </table>
	 *
	 * <p>Id di luar daftar itu (termasuk seluruh komponen mode OBE) tidak dikenali, sehingga nama baku
	 * dari {@link StatusPertemuan} dipakai apa adanya.</p>
	 *
	 * <p><b>Jebakan:</b> method memanggil {@code perkuliahan.getPembombotanNilai().getXxxLabel()}
	 * <b>tanpa</b> memeriksa hasil {@code getPembombotanNilai()}. {@link #getNama()} sudah menjaganya,
	 * tetapi pemanggil langsung dari luar (mis. {@code EksporNilaiFeeder}) tidak &mdash; walau
	 * praktis aman karena {@code Perkuliahan.getPembombotanNilai()} punya nilai baku berlapis.</p>
	 *
	 * <p><b>Efek samping tidak langsung:</b> {@code perkuliahan.getPembombotanNilai()} sendiri adalah
	 * getter berefek samping (membaca cache konstanta dan menulis balik ke field
	 * {@link Perkuliahan}).</p>
	 *
	 * @param statusPertemuan jenis komponen; bila {@code null} hasilnya string kosong.
	 * @param perkuliahan     kelas pemilik komponen, sebagai sumber label institusi; bila {@code null}
	 *                        yang dikembalikan adalah nama baku master.
	 * @return label tampil komponen; string kosong bila {@code statusPertemuan} {@code null}, dan bisa
	 *         {@code null} bila label institusi yang terpilih memang belum diisi.
	 * @see PembombotanNilai
	 * @see #getNama()
	 */
	public static String ambilNama(StatusPertemuan statusPertemuan, Perkuliahan perkuliahan) {
		String nama = statusPertemuan == null ? "" : statusPertemuan.getNama();
		if (perkuliahan != null && statusPertemuan != null) {
			if (statusPertemuan.getId().equals(1L)) {
				nama = perkuliahan.getPembombotanNilai().getAbsenLabel();
			} else if (statusPertemuan.getId().equals(2L)) {
				nama = perkuliahan.getPembombotanNilai().getFormLabel();
			} else if (statusPertemuan.getId().equals(3L)) {
				nama = perkuliahan.getPembombotanNilai().getUtsLabel();
			} else if (statusPertemuan.getId().equals(4L)) {
				nama = perkuliahan.getPembombotanNilai().getUasLabel();
			} else if (statusPertemuan.getId().equals(21L)) {
				nama = perkuliahan.getPembombotanNilai().getTugas1Label();
			} else if (statusPertemuan.getId().equals(22L)) {
				nama = perkuliahan.getPembombotanNilai().getTugas2Label();
			} else if (statusPertemuan.getId().equals(23L)) {
				nama = perkuliahan.getPembombotanNilai().getTugas3Label();
			} else if (statusPertemuan.getId().equals(24L)) {
				nama = perkuliahan.getPembombotanNilai().getTugas4Label();
			} else if (statusPertemuan.getId().equals(25L)) {
				nama = perkuliahan.getPembombotanNilai().getTugas5Label();
			}

			else if (statusPertemuan.getId().equals(31L)) {
				nama = perkuliahan.getPembombotanNilai().getQuiz1Label();
			} else if (statusPertemuan.getId().equals(32L)) {
				nama = perkuliahan.getPembombotanNilai().getQuiz2Label();
			} else if (statusPertemuan.getId().equals(33L)) {
				nama = perkuliahan.getPembombotanNilai().getQuiz3Label();
			} else if (statusPertemuan.getId().equals(34L)) {
				nama = perkuliahan.getPembombotanNilai().getQuiz4Label();
			} else if (statusPertemuan.getId().equals(35L)) {
				nama = perkuliahan.getPembombotanNilai().getQuiz5Label();
			}
		}
		return nama;
	}

	/**
	 * Memeriksa apakah sebuah teks <b>hanya</b> terdiri atas angka.
	 *
	 * <p>Dipakai {@link #getNama()} sebagai penyaring kualitas: nama komponen OBE yang isinya cuma
	 * deretan angka (mis. sisa impor yang menaruh id atau indeks di kolom nama) dianggap
	 * <b>tidak layak pakai</b>, sehingga nama akan diturunkan ulang dari formula CPL.</p>
	 *
	 * @param value teks yang diperiksa.
	 * @return {@code true} hanya bila teks tidak {@code null}, tidak kosong setelah di-{@code trim},
	 *         dan setiap karakternya digit.
	 */
	private static boolean hanyaAngka(String value) {
		if (value == null || value.trim().length() == 0) {
			return false;
		}
		String v = value.trim();
		for (int i = 0; i < v.length(); i++) {
			if (!Character.isDigit(v.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Menurunkan nama komponen OBE dari <b>formula JSON</b> milik CPL/CPMK yang tertaut.
	 *
	 * <p>{@code CapaianPembelajaranLulusan.getFormula()} berisi sebuah {@code JSONArray}; tiap
	 * elemennya mewakili satu Sub-CPMK dengan kunci {@code key}, serta atribut tampil {@code kode} dan
	 * {@code nama}. Method ini mencari elemen yang {@code key}-nya cocok dengan
	 * {@link #getKodeSubCpmk()} (perbandingan tanpa membedakan besar-kecil huruf, setelah
	 * {@code trim}), lalu menyusun namanya dengan urutan preferensi:</p>
	 * <ol>
	 * <li>{@code "kode - nama"} bila keduanya terisi;</li>
	 * <li>{@code kode} saja;</li>
	 * <li>{@code nama} saja.</li>
	 * </ol>
	 *
	 * <p>Hasilnya selalu dipotong ke {@value #PANJANG_STRING_STANDAR} karakter.</p>
	 *
	 * <p><b>Tidak berefek samping</b>: membaca field {@code capaianPembelajaranLulusan} dan
	 * {@code kodeSubCpmk} secara langsung (bukan lewat getter), jadi tidak memicu {@code check()}
	 * maupun penulisan balik. Pemanggilnya, {@link #getNama()}, sudah memastikan relasi CPL
	 * teresolusi lebih dulu.</p>
	 *
	 * <p><b>Jebakan:</b> JSON yang rusak ditelan {@code try/catch} dan hanya dicatat ke audit;
	 * kegagalan tampak sebagai "nama tidak ketemu", bukan sebagai kesalahan.</p>
	 *
	 * @return nama Sub-CPMK yang sudah dipotong, atau string kosong bila relasi CPL belum ada,
	 *         {@code kodeSubCpmk} kosong, formula tidak terbaca, atau tidak ada elemen yang cocok.
	 *         Tidak pernah {@code null}.
	 */
	private String ambilNamaObeDariFormula() {
		if (capaianPembelajaranLulusan == null || kodeSubCpmk == null
				|| kodeSubCpmk.trim().length() == 0) {
			return "";
		}
		try {
			JSONArray array = new JSONArray(capaianPembelajaranLulusan.getFormula());
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);
				String key = jsonObject.isNull("key") ? "" : (jsonObject.get("key") + "").trim();
				if (!kodeSubCpmk.trim().equalsIgnoreCase(key)) {
					continue;
				}
				String kode = jsonObject.isNull("kode") ? "" : (jsonObject.get("kode") + "").trim();
				String namaFormula = jsonObject.isNull("nama") ? "" : (jsonObject.get("nama") + "").trim();
				if (kode.length() > 0 && namaFormula.length() > 0) {
					return potongStringStandar(kode + " - " + namaFormula);
				}
				if (kode.length() > 0) {
					return potongStringStandar(kode);
				}
				if (namaFormula.length() > 0) {
					return potongStringStandar(namaFormula);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) FormatNilai:ambilNamaObeDariFormula");
		}
		return "";
	}

	/**
	 * Nama tampil komponen penilaian &mdash; teks yang dilihat pengguna di grid entri nilai, laporan,
	 * dan ekspor.
	 *
	 * <p>Ini <b>bukan</b> getter sepele. Nama komponen sebagian besar bersifat <i>turunan</i>, dan
	 * method ini menghitung ulang serta <b>menyimpannya kembali</b> ke field {@code nama} setiap kali
	 * dipanggil. Urutan keputusannya:</p>
	 * <ol>
	 * <li>Resolusi relasi lebih dulu: {@link #getPerkuliahan()} dan {@link #getStatusPertemuan()}.</li>
	 * <li>Tentukan apakah baris ini komponen <b>OBE</b> &mdash; yaitu bila
	 * {@link #getCapaianPembelajaranLulusan()} terisi <b>atau</b> {@link #getKodeSubCpmk()} tidak
	 * kosong.</li>
	 * <li><b>Nama OBE buatan pengguna dipertahankan:</b> bila mode OBE dan {@code nama} sudah terisi
	 * teks yang bukan sekadar angka ({@code hanyaAngka(String)}), nama itu dikembalikan apa adanya
	 * tanpa ditimpa. Inilah satu-satunya jalan agar nama Sub-CPMK hasil suntingan manual tidak
	 * hilang.</li>
	 * <li>Masih mode OBE tapi nama belum layak &rarr; diturunkan dari formula JSON CPL lewat
	 * {@code ambilNamaObeDariFormula()}.</li>
	 * <li>Jalur konvensional: bila kelas dan skema pembobotannya ada, nama diambil dari
	 * {@link #ambilNama(StatusPertemuan, Perkuliahan)} (label institusi); bila skema tidak ada, dipakai
	 * nama baku {@link StatusPertemuan}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (penting):</b> {@code nama} adalah properti <b>terpetakan</b>, dan kelas ini
	 * memakai {@code dynamicUpdate = true}. Membaca nama pada instance yang masih dikelola sesi
	 * Hibernate karenanya dapat memicu {@code UPDATE} kolom {@code nama} tanpa aksi simpan dari
	 * pengguna &mdash; misalnya ketika administrator mengubah label pada {@link PembombotanNilai},
	 * nama seluruh komponen kelas akan ikut termutakhirkan begitu layar mana pun membacanya. Perilaku
	 * ini disengaja, tetapi mengejutkan bagi kode laporan yang mengira hanya "membaca".</p>
	 *
	 * <p>Selain itu method ini juga menugaskan ulang field {@code perkuliahan} dan
	 * {@code statusPertemuan} dari getternya &mdash; secara teknis mubazir, karena kedua getter itu
	 * sudah menulis balik ke fieldnya sendiri.</p>
	 *
	 * @return nama tampil komponen; bisa {@code null} bila baris belum punya {@link StatusPertemuan}
	 *         maupun nama tersimpan.
	 * @see #ambilNama(StatusPertemuan, Perkuliahan)
	 */
	public String getNama() {
		perkuliahan = getPerkuliahan();
		statusPertemuan = getStatusPertemuan();
		boolean formatObe = getCapaianPembelajaranLulusan() != null
				|| (kodeSubCpmk != null && kodeSubCpmk.trim().length() > 0);
		if (formatObe && nama != null && nama.trim().length() > 0 && !hanyaAngka(nama)) {
			return nama;
		}
		if (formatObe) {
			String namaFormula = ambilNamaObeDariFormula();
			if (namaFormula.length() > 0 && !hanyaAngka(namaFormula)) {
				nama = potongStringStandar(namaFormula);
				return nama;
			}
		}
		if (perkuliahan != null && perkuliahan.getPembombotanNilai() != null && statusPertemuan != null) {
			nama = potongStringStandar(FormatNilai.ambilNama(statusPertemuan, perkuliahan));
		} else if (statusPertemuan != null) {
			nama = potongStringStandar(statusPertemuan.getNama());
		}

		return nama;
	}

	/**
	 * Menyetel nama tampil komponen.
	 *
	 * <p>Teks dipangkas dan dipotong ke {@value #PANJANG_STRING_STANDAR} karakter lebih dulu. Perlu
	 * diingat bahwa untuk komponen <b>non-OBE</b>, nilai yang disetel di sini akan <b>ditimpa</b> lagi
	 * oleh {@link #getNama()} pada pembacaan berikutnya; hanya komponen OBE yang mempertahankan nama
	 * buatan pengguna.</p>
	 *
	 * @param nama nama tampil; boleh {@code null}.
	 * @see #getNama()
	 */
	public void setNama(String nama) {
		this.nama = potongStringStandar(nama);
	}

	/**
	 * Pengguna yang sedang <b>mengunci</b> entri nilai komponen ini.
	 *
	 * <p>Selama kolom ini terisi, grid entri nilai
	 * ({@code ais.action.master.helper.DetailperkuliahanForPenilaianHelper}) menonaktifkan penyuntingan
	 * komponen tersebut dan menampilkan tooltip "Dikunci oleh &lt;user&gt;". Umumnya hanya pengguna
	 * yang mengunci (atau yang berwenang) yang boleh membukanya kembali. Kunci ini terpisah dari
	 * kunci tingkat kelas ({@code Perkuliahan.getDikunci()}) dan kunci tingkat master
	 * ({@code StatusPertemuan.getKunci()}) &mdash; ketiganya diperiksa berbarengan.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila komponen tidak terkunci.
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kunci", nullable = true)
	public Tbmuser getKunci() {
		kunci = check(kunci);
		return kunci;
	}

	/**
	 * Mengunci atau membuka kunci entri nilai komponen ini.
	 *
	 * <p>Diisi {@code Common.getCurrentUser()} saat tombol "kunci" ditekan, dan disetel {@code null}
	 * saat tombol "buka kunci" ditekan.</p>
	 *
	 * @param kunci pengguna pengunci, atau {@code null} untuk membuka kunci.
	 * @see #getKunci()
	 */
	public void setKunci(Tbmuser kunci) {
		this.kunci = kunci;
	}

	/**
	 * Klasifikasi <b>jenis evaluasi</b> komponen ini (kognitif/pengetahuan, aktivitas partisipatif,
	 * tugas, dan seterusnya) &mdash; dipakai untuk pelaporan OBE dan ekspor Neo Feeder.
	 *
	 * <p>Karena kolomnya boleh kosong dan data lama banyak yang belum terisi, getter ini
	 * <b>menebak</b> nilainya lewat rantai jatuh berlapis:</p>
	 * <ol>
	 * <li>resolusi proxy lazy lewat {@code check(...)};</li>
	 * <li>bila masih kosong, tebak dari <b>teks nama komponen</b> (huruf kecil): mengandung
	 * {@code uts}, {@code uas}, {@code tugas}, {@code quis}, {@code "kuis "}, {@code total}, atau
	 * {@code quiz} &rarr; {@code ConstantValues.KognitifPengetahuan};</li>
	 * <li>bila masih kosong, tebak dari nama juga: mengandung {@code absen}, {@code presen},
	 * {@code aktif}, atau {@code lain} &rarr; {@code ConstantValues.AktivitasPartisipatif};</li>
	 * <li>bila masih kosong, ikut jenis evaluasi milik {@link StatusPertemuan};</li>
	 * <li>penyelamat terakhir: {@code ConstantValues.Tugas} &mdash; dipakai juga bila jenis evaluasi
	 * yang ketemu ternyata <b>tidak aktif</b>.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (penting):</b> hasil tebakan <b>ditulis balik</b> ke field terpetakan
	 * {@code jenisEvaluasi}. Dengan {@code dynamicUpdate = true}, sekadar membaca properti ini pada
	 * instance yang dikelola sesi bisa mengisi kolom {@code jenis_evaluasi} di database secara
	 * permanen. Ditambah lagi, method memanggil {@link #getNama()} yang <b>juga</b> berefek samping,
	 * sehingga satu pembacaan bisa memutakhirkan dua kolom sekaligus.</p>
	 *
	 * <p><b>Kuirk:</b> pencocokan dilakukan atas <i>teks nama</i>, jadi label institusi yang tidak
	 * lazim (mis. "Praktikum" atau "Portofolio") tidak akan cocok pada langkah 2 maupun 3 dan akan
	 * jatuh ke langkah 4/5. Ejaan {@code "quis"} (tanpa spasi) dan {@code "kuis "} (dengan spasi di
	 * belakang) sengaja berbeda &mdash; yang kedua menuntut kata "kuis" diikuti sesuatu, sehingga
	 * nama yang persis {@code "Kuis"} justru <b>tidak</b> tertangkap di sana.</p>
	 *
	 * @return jenis evaluasi yang berlaku; praktis tidak pernah {@code null}, sepanjang konstanta
	 *         {@code ConstantValues.Tugas} sudah terinisialisasi.
	 * @see ais.common.ConstantValues
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_evaluasi", nullable = true)
	public JenisEvaluasi getJenisEvaluasi() {
		jenisEvaluasi = check(jenisEvaluasi);

		if (jenisEvaluasi == null && getNama() != null
				&& (nama.toLowerCase().contains("uts") || nama.toLowerCase().contains("uas")
						|| nama.toLowerCase().contains("tugas") || nama.toLowerCase().contains("quis")
						|| nama.toLowerCase().contains("kuis ") || nama.toLowerCase().contains("total")
						|| nama.toLowerCase().contains("quiz"))) {
			jenisEvaluasi = ConstantValues.KognitifPengetahuan;
		}

		if (jenisEvaluasi == null && getNama() != null
				&& (nama.toLowerCase().contains("absen") || nama.toLowerCase().contains("presen")
						|| nama.toLowerCase().contains("aktif") || nama.toLowerCase().contains("lain"))) {
			jenisEvaluasi = ConstantValues.AktivitasPartisipatif;
		}

		if (jenisEvaluasi == null && getStatusPertemuan() != null && getStatusPertemuan().getJenisEvaluasi() != null) {
			jenisEvaluasi = getStatusPertemuan().getJenisEvaluasi();
		}

		if (jenisEvaluasi == null || (jenisEvaluasi != null && !jenisEvaluasi.getAktif())) {
			jenisEvaluasi = ConstantValues.Tugas;
		}

		return jenisEvaluasi;
	}

	/**
	 * Menyetel jenis evaluasi komponen ini.
	 *
	 * <p>Nilai yang disetel bisa saja <b>ditimpa</b> oleh {@link #getJenisEvaluasi()} pada pembacaan
	 * berikutnya, yaitu bila jenis evaluasi yang disetel ternyata tidak aktif.</p>
	 *
	 * @param jenisEvaluasi klasifikasi jenis evaluasi; boleh {@code null}.
	 * @see #getJenisEvaluasi()
	 */
	public void setJenisEvaluasi(JenisEvaluasi jenisEvaluasi) {
		this.jenisEvaluasi = jenisEvaluasi;
	}

	/**
	 * Id komponen evaluasi padanan di <b>Neo Feeder</b> (PDDikti).
	 *
	 * <p>Diisi oleh {@code ais.action.master.feeder.util.FeederExporter} setelah komponen berhasil
	 * didaftarkan ke layanan Feeder, dan dipakai pada pengiriman berikutnya agar komponen yang sama
	 * tidak terkirim dua kali.</p>
	 *
	 * @return id komponen evaluasi Feeder, atau {@code null} bila komponen ini belum pernah
	 *         disinkronkan.
	 */
	public String getFeeder() {
		return feeder;
	}

	/**
	 * Menyetel id komponen evaluasi Neo Feeder.
	 *
	 * @param feeder id komponen evaluasi di Feeder; boleh {@code null} untuk memaksa pendaftaran ulang.
	 * @see #getFeeder()
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Nomor urut tampil komponen di dalam kelasnya (dipakai juga oleh
	 * {@link #compareTo(GeneralValueObject)}).
	 *
	 * <p>Untuk kelas <b>non-OBE</b>, nomor urut bukan sekadar dibaca dari kolom: bila kelas punya
	 * skema pembobotan dan kurikulumnya <b>tidak</b> OBE pada tahun ajaran + semester kelas tersebut
	 * ({@code Kurikulum.apakahObe(tahunAjaran, ganjilGenap)}), nomor urut <b>diambil ulang</b> dari
	 * JSON {@code PembombotanNilai.getNomorUrutFormat()} dengan kunci berupa id
	 * {@link StatusPertemuan}. Dengan begitu, administrator yang menyusun ulang urutan komponen di
	 * layar master langsung berpengaruh ke seluruh kelas yang memakai skema itu, tanpa perlu mencetak
	 * ulang komponen.</p>
	 *
	 * <p>Untuk kelas OBE, jalur itu dilewati dan nilai kolom dipakai apa adanya &mdash; urutannya
	 * memang sudah diberikan berurutan mulai 1 saat komponen dicetak.</p>
	 *
	 * <p><b>Efek samping:</b> hasil pembacaan JSON <b>ditulis balik</b> ke field terpetakan
	 * {@code nomorUrut}; bersama {@code dynamicUpdate = true} ini bisa memicu {@code UPDATE} diam-diam.
	 * Method juga memanggil {@link #getStatusPertemuan()} dan {@link #getPerkuliahan()} (masing-masing
	 * beberapa kali) sehingga ikut memicu resolusi proxy.</p>
	 *
	 * <p><b>Kuirk:</b> pemeriksaan OBE membaca {@code perkuliahan.getTahunAjaran()} dan
	 * {@code getGanjilGenap()} dari <b>field</b> {@code perkuliahan}, bukan dari getter &mdash; aman
	 * di sini hanya karena {@code getPerkuliahan()} sudah dipanggil pada ekspresi yang sama
	 * sebelumnya. Kegagalan penguraian JSON ditelan {@code try/catch} (dicetak ke {@code stderr} dan
	 * dicatat ke audit), sehingga nomor urut cukup tetap memakai nilai lama.</p>
	 *
	 * @return nomor urut komponen; tidak pernah {@code null} &mdash; {@code 1} bila belum pernah
	 *         ditentukan.
	 * @see PembombotanNilai#getNomorUrutFormat()
	 * @see #compareTo(GeneralValueObject)
	 */
	public Integer getNomorUrut() {

		if (getStatusPertemuan() != null && getPerkuliahan() != null && getPerkuliahan().getPembombotanNilai() != null
				&& getPerkuliahan().getKurikulum() != null && !getPerkuliahan().getKurikulum()
						.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {

			try {
				JSONObject jsonData = new JSONObject(getPerkuliahan().getPembombotanNilai().getNomorUrutFormat());
				Long n = jsonData.isNull(getStatusPertemuan().getId().toString()) ? null
						: ais.common.CommonJSONUtil.ambilLong(jsonData, getStatusPertemuan().getId().toString());
				if (n != null) {
					nomorUrut = n.intValue();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/FormatNilai.java:298");
			}

		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil komponen.
	 *
	 * <p>Untuk kelas non-OBE nilai ini akan <b>ditimpa</b> lagi oleh {@link #getNomorUrut()} bila
	 * skema pembobotan kelas menyimpan urutan untuk {@link StatusPertemuan} yang bersangkutan.</p>
	 *
	 * @param nomorUrut nomor urut; boleh {@code null} (dibaca kembali sebagai {@code 1}).
	 * @see #getNomorUrut()
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * CPL/CPMK yang diwakili komponen ini pada <b>mode OBE</b>.
	 *
	 * <p>Terisinya properti ini &mdash; atau terisinya {@link #getKodeSubCpmk()} &mdash; adalah
	 * penanda bahwa baris ini komponen OBE, yang mengubah cara {@link #getNama()} menurunkan nama dan
	 * mengaktifkan perhitungan ambang di {@link #ambilMinimal()}.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(...)} ditulis balik ke field.</p>
	 *
	 * @return CPL/CPMK terkait, atau {@code null} untuk komponen konvensional.
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "capaian_pembelajaran_lulusan", nullable = true)
	public CapaianPembelajaranLulusan getCapaianPembelajaranLulusan() {
		capaianPembelajaranLulusan = check(capaianPembelajaranLulusan);
		return capaianPembelajaranLulusan;
	}

	/**
	 * Menautkan komponen ini ke sebuah CPL/CPMK (mode OBE).
	 *
	 * @param capaianPembelajaranLulusan CPL/CPMK terkait; {@code null} mengembalikan baris ke mode
	 *                                   konvensional (kecuali {@link #getKodeSubCpmk()} masih terisi).
	 */
	public void setCapaianPembelajaranLulusan(CapaianPembelajaranLulusan capaianPembelajaranLulusan) {
		this.capaianPembelajaranLulusan = capaianPembelajaranLulusan;
	}

	/**
	 * Kunci Sub-CPMK di dalam formula JSON milik {@link #getCapaianPembelajaranLulusan()}.
	 *
	 * <p>Nilainya cocok dengan atribut {@code key} salah satu elemen
	 * {@code CapaianPembelajaranLulusan.getFormula()}. Kunci inilah yang dipakai
	 * {@code PembombotanNilai.setDefaultPembobotan(...)} untuk mencocokkan baris lama saat komponen
	 * dicetak ulang &mdash; lebih stabil daripada relasi {@link StatusPertemuan}, yang idnya bisa
	 * berganti antar proses hitung ulang.</p>
	 *
	 * @return kunci Sub-CPMK, atau {@code null} untuk komponen konvensional maupun komponen OBE
	 *         setingkat CPMK penuh.
	 */
	public String getKodeSubCpmk() {
		return kodeSubCpmk;
	}

	/**
	 * Menyetel kunci Sub-CPMK. Teks dipangkas dan dipotong ke
	 * {@value #PANJANG_STRING_STANDAR} karakter.
	 *
	 * @param kodeSubCpmk kunci Sub-CPMK pada formula JSON CPL; boleh {@code null}.
	 * @see #getKodeSubCpmk()
	 */
	public void setKodeSubCpmk(String kodeSubCpmk) {
		this.kodeSubCpmk = potongStringStandar(kodeSubCpmk);
	}

	/**
	 * Memangkas spasi dan memotong teks ke batas {@value #PANJANG_STRING_STANDAR} karakter agar muat
	 * di kolom {@code varchar} tabel.
	 *
	 * <p>Dipakai oleh {@link #setNama(String)}, {@link #setKodeSubCpmk(String)}, {@link #getNama()},
	 * dan {@code ambilNamaObeDariFormula()}. Pemotongan dilakukan <b>diam-diam</b> &mdash; pengguna
	 * tidak diberi tahu bahwa namanya terpotong.</p>
	 *
	 * @param value teks masukan.
	 * @return {@code null} bila masukannya {@code null}; selain itu teks hasil {@code trim} yang
	 *         panjangnya paling banyak {@value #PANJANG_STRING_STANDAR} karakter.
	 */
	private static String potongStringStandar(String value) {
		if (value == null) {
			return null;
		}
		String hasil = value.trim();
		if (hasil.length() <= PANJANG_STRING_STANDAR) {
			return hasil;
		}
		return hasil.substring(0, PANJANG_STRING_STANDAR);
	}

	/**
	 * <b>Ambang ketercapaian (nilai minimal lulus)</b> untuk komponen ini &mdash; dipakai jalur OBE
	 * untuk menetapkan status "L" (lulus) atau "TL" (tidak lulus) per komponen.
	 *
	 * <p>Nilainya dipilih berlapis:</p>
	 * <ol>
	 * <li><b>Nilai dasar</b> diambil dari {@code KurikulumPunyaMatakuliah.getMinimalKetercapaian()}
	 * milik kelas; bila tautan kurikulum-matakuliahnya tidak ada, dipakai {@code 50}.</li>
	 * <li>Bila matakuliah <b>dinilai per CPMK</b>
	 * ({@code KurikulumPunyaMatakuliah.getNilaiMenggunakanCpmk()}) dan CPL yang tertaut punya
	 * {@code getMinimal()} sendiri, ambang CPL itulah yang dipakai.</li>
	 * <li>Selain itu, bila komponen tetap tertaut ke sebuah CPL, ambang dicari di dalam <b>formula
	 * JSON</b> CPL: array ditelusuri sambil terus menimpa variabel {@code minimal} dari atribut
	 * {@code minimal} tiap elemen, dan penelusuran <b>berhenti</b> pada elemen yang {@code kode}-nya
	 * cocok dengan {@link #getKodeSubCpmk()}. Nilai {@code 0} diperlakukan sama dengan "tidak
	 * diisi".</li>
	 * <li>Bila semuanya gagal, kembali ke nilai dasar langkah 1.</li>
	 * </ol>
	 *
	 * <p><b>Kuirk penting pada langkah 3</b> (dicatat apa adanya, bukan untuk diperbaiki di sini):
	 * pencocokan memakai atribut <b>{@code kode}</b>, sedangkan {@code ambilNamaObeDariFormula()}
	 * mencocokkan {@link #getKodeSubCpmk()} dengan atribut <b>{@code key}</b>. Kedua method di berkas
	 * yang sama karenanya mencocokkan kunci yang sama ke <i>atribut JSON yang berbeda</i>. Selain itu
	 * variabel {@code minimal} <b>dinolkan lalu diisi ulang di setiap iterasi</b>, sehingga bila tidak
	 * ada elemen yang cocok, yang tersisa adalah nilai {@code minimal} milik <b>elemen terakhir</b>
	 * array &mdash; bukan nilai dasar. Elemen yang tidak punya {@code key} dilewati.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getPerkuliahan()} (dan menugaskan ulang hasilnya ke
	 * field) serta {@link #getCapaianPembelajaranLulusan()} beberapa kali, jadi ikut memicu resolusi
	 * proxy lazy. Variabel lokal {@code formatNilai = this} hanyalah alias, tidak berpengaruh apa
	 * pun.</p>
	 *
	 * <p><b>Jebakan:</b> {@code perkuliahan} dipakai tanpa pemeriksaan {@code null}, sehingga komponen
	 * yatim (tanpa kelas) akan melempar {@code NullPointerException}. Method juga tidak menjaga
	 * {@code null} pada {@code getNilaiMenggunakanCpmk()}.</p>
	 *
	 * <p>Dipanggil dari {@code ais.action.master.NilaiObeAction} (penentuan status L/TL) dan
	 * {@code ais.action.master.dashboard.admin.RekapHasilTugasPerTugasDanUjianObe}.</p>
	 *
	 * @return ambang ketercapaian komponen; tidak pernah {@code null}.
	 * @throws Exception bila formula JSON milik CPL gagal diuraikan
	 *                   ({@code new JSONArray(...)} di luar blok {@code try} bagian dalam).
	 * @see #getKodeSubCpmk()
	 * @see #getCapaianPembelajaranLulusan()
	 */
	public Double ambilMinimal() throws Exception {
		FormatNilai formatNilai = this;
		perkuliahan = getPerkuliahan();
		Double min = perkuliahan.getKurikulumPunyaMatakuliah() == null ? 50
				: perkuliahan.getKurikulumPunyaMatakuliah().getMinimalKetercapaian();
		Double minimal = min;
		if (perkuliahan.getKurikulumPunyaMatakuliah() != null
				&& perkuliahan.getKurikulumPunyaMatakuliah().getNilaiMenggunakanCpmk()
				&& formatNilai.getCapaianPembelajaranLulusan() != null
				&& formatNilai.getCapaianPembelajaranLulusan().getMinimal() != null) {
			minimal = formatNilai.getCapaianPembelajaranLulusan().getMinimal();
		} else if (formatNilai.getCapaianPembelajaranLulusan() != null) {
			JSONArray array = new JSONArray(formatNilai.getCapaianPembelajaranLulusan().getFormula());
			for (int i = 0; i < array.length(); i++) {
				try {
					JSONObject jsonObject = array.getJSONObject(i);

					if (jsonObject.isNull("key")) {
						continue;
					}

					String kode = "";

					if (!jsonObject.isNull("kode")) {
						kode = jsonObject.get("kode") + "";
					}

					minimal = null;

					if (!jsonObject.isNull("minimal")) {
						try {
							minimal = Double.parseDouble(jsonObject.get("minimal") + "");
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/FormatNilai.java:360");
							// TODO: handle exception
						}
					}
					if (minimal == null || minimal.intValue() == 0) {
						minimal = null;
					}
					if (formatNilai.getKodeSubCpmk() != null && formatNilai.getKodeSubCpmk().equalsIgnoreCase(kode)) {
						break;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/FormatNilai.java:371");
				}
			}
		}
		return minimal == null ? min : minimal;
	}

}
