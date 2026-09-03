package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.akunting.PostingHistory;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>baris tagihan</b> pada sebuah {@link Kegiatan} &mdash; tabel {@code public.detail_kegiatan}.
 *
 * <p>Satu {@code Kegiatan} adalah "wadah tagihan" milik seorang mahasiswa/calon mahasiswa untuk satu
 * semester + satu {@link JenisKegiatan} (registrasi, daftar ulang, perkuliahan, wisuda, dsb).
 * {@code DetailKegiatan} adalah <b>satu baris di dalam wadah itu</b>: "SPP semester 3 = Rp 3.500.000,
 * diskon Rp 350.000, jatuh tempo 1 September". Yang dicetak di kartu tagihan, dihitung di dasbor
 * tunggakan, dan dicocokkan dengan pembayaran adalah kumpulan baris ini.</p>
 *
 * <h3>Posisi persis dalam rantai billing</h3>
 *
 * <p>Rantai lengkap (diverifikasi dari kode kelas ini dan kelas tetangganya):</p>
 *
 * <pre>
 * {@link ItemBiaya}                &mdash; katalog JENIS biaya ("SPP", "Praktikum", "Wisuda"); aturan
 *                              penghitungan, boleh/tidaknya nominal diubah kasir
 *      &darr;
 * {@code SettingBiaya}/{@code DetailSettingBiaya}
 *                            &mdash; paket biaya per angkatan/jurusan/gelombang
 *      &darr;
 * {@link DetailBiaya}              &mdash; baris MASTER berisi nominal konkret + {@code bayarKe}
 *                              (cicilan ke berapa). Dipakai bersama oleh BANYAK mahasiswa.
 *      &darr;
 * {@code DetailKegiatan}     &mdash; INSTANSIASI baris master itu untuk SATU Kegiatan milik SATU
 *                              orang: nominal final, diskon, tanggal jatuh tempo, kunci, denda.
 *      &darr;
 * {@link CicilanPembayaran}        &mdash; uang yang benar-benar masuk untuk menutup baris tagihan
 *      &darr;
 * {@link BuktiPembayaran}          &mdash; kwitansi/bukti yang membungkus satu atau banyak cicilan
 * </pre>
 *
 * <p><b>PENTING &mdash; tidak ada foreign key {@code DetailKegiatan} &harr; {@code CicilanPembayaran}.</b>
 * Diperiksa langsung: {@code CicilanPembayaran} tidak punya properti {@code detailKegiatan} sama sekali.
 * Keduanya "bertemu" karena sama-sama menunjuk {@link Kegiatan} yang sama plus salah satu dari
 * {@link DetailBiaya}/{@link ItemBiaya}/{@link PengaturanPembayaranBulanan} yang sama. Pencocokan
 * praktisnya memakai kunci string yang dibangun {@link #kodeUnik(PengaturanPembayaranBulanan, ItemBiaya,
 * Integer, Kegiatan, KegiatanTemporary)} (lihat pemakaiannya di
 * {@code KegiatanPersistenceHelper.hitungTagihanRingkas}). Jangan mencari relasi objek yang tidak ada:
 * "sisa tagihan" dan "status lunas" TIDAK dihitung di kelas ini, melainkan di lapisan helper
 * ({@code KegiatanHelper}, {@code KegiatanPersistenceHelper}, {@code PembayaranHelper}) yang
 * menjumlahkan {@code DetailKegiatan} lalu menguranginya dengan {@code CicilanPembayaran}.</p>
 *
 * <p>Hubungan ke induknya juga tidak berupa koleksi Hibernate: {@link Kegiatan} menyimpan daftar
 * anaknya sebagai <b>string CSV</b> {@code ",<id>:true,<id>:false,"} pada properti
 * {@code detailKegiatans} (lihat {@code Kegiatan.appendDetailKegiatan}). Karena itu
 * {@link #setKegiatan(Kegiatan)} melakukan pendaftaran manual ke string tersebut, dan penghapusan
 * baris dikerjakan dengan menandai {@code :false}, bukan {@code DELETE}.</p>
 *
 * <h3>Pengelompokan method</h3>
 *
 * <ol>
 * <li><b>Jejak audit yang dideklarasikan ulang</b> &mdash; {@link #getId()}, {@link #getOleh()},
 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Lihat catatan
 * "duplikasi yang disengaja" di bawah.</li>
 * <li><b>Nominal</b> &mdash; {@link #getBiaya()} (getter berat, penuh logika), {@link #ambilBiaya()}
 * (pembacaan mentah tanpa efek samping), {@link #getBiayaTemporary()}, {@link #getDendaCustom()},
 * {@link #getMenggunakanDendaCustom()}, {@link #getBatalkanDenda()}.</li>
 * <li><b>Diskon</b> &mdash; {@link #hitungDiskon(Double)}, {@link #getDiskon()},
 * {@link #cariJenisDiskonMahasiswa()}, {@link #adaDiskon()}, {@link #diskonCocok(DiskonMahasiswa)},
 * dan tiga slot {@link #getDiskonMahasiswaData()}/{@link #getDiskonMahasiswaData2()}/
 * {@link #getDiskonMahasiswaData3()}. Ini bagian paling rumit dan paling banyak efek sampingnya.</li>
 * <li><b>Relasi</b> &mdash; {@link #getKegiatan()}, {@link #getKegiatanTemporary()},
 * {@link #getDetailBiaya()}, {@link #getItemBiaya()}, {@link #getPengaturanPembayaranBulanan()},
 * {@link #getPostingHistory()}, {@link #getKunci()}.</li>
 * <li><b>Tanggal jatuh tempo</b> &mdash; {@link #getTanggal()} (getter terberat di kelas ini) dan
 * {@link #getTanggalCustom()}.</li>
 * <li><b>Identitas logis</b> &mdash; dua overload statis {@link #kodeUnik(Long, Long, Integer, Long, Long)},
 * {@link #getKodeUnik()}, {@link #getAktif()}, {@link #getBukanTagihan()}.</li>
 * <li><b>Fasad statis ke persistence helper</b> &mdash; {@link #populatePembayaran(java.util.List, Kegiatan)},
 * {@link #populatePembayaran(DetailBiaya, PengaturanPembayaranBulanan, Kegiatan, Double)},
 * {@link #populateHapusPembayaran(DetailKegiatan, Kegiatan)}. Ketiganya sekarang hanya meneruskan
 * panggilan ke {@link KegiatanPersistenceHelper}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ul>
 * <li><b>Getter di kelas ini bukan getter polos.</b> Sebagian besar getter menghitung ulang nilainya
 * lalu <b>menulis balik ke field</b>. Karena entity ini {@code @Audited} dan
 * {@code dynamicUpdate}, penulisan itu bisa ikut ter-flush ke database bila instance-nya masih
 * <i>attached</i> pada sesi Hibernate aktif &mdash; sekadar membaca sebuah baris tagihan dapat
 * mengubah isinya. Daftar getter penulis: {@link #getBiaya()}, {@link #getBiayaTemporary()},
 * {@link #getDetailBiaya()}, {@link #getItemBiaya()}, {@link #getKeterangan()}, {@link #getTanggal()},
 * {@link #getDiskon()}, {@link #getDendaCustom()}, {@link #getKodeUnik()}, {@link #getKunci()},
 * dan ketiga {@code getDiskonMahasiswaData*()}.</li>
 * <li><b>Getter yang menulis ke OBJECT LAIN.</b> {@link #getBiaya()} memanggil
 * {@code detailBiaya.updateKeterangan(mahasiswa, semester)}, yang men-<i>set</i>
 * {@code nilaiBiayaBaru} pada entity {@link DetailBiaya} &mdash; sebuah baris <b>master yang dipakai
 * bersama banyak mahasiswa</b> &mdash; dan menjalankan query Hibernate di dalamnya. Ini pola yang sama
 * dengan temuan pada {@code CicilanPembayaran.getKegiatan()}; lihat rinciannya di Javadoc
 * {@link #getBiaya()}. Selain itu {@link #setKegiatan(Kegiatan)} menulis ke {@link Kegiatan} induk.</li>
 * <li><b>Urutan pemanggilan berpengaruh.</b> {@link #getDiskon()} menghitung diskon dari <i>field</i>
 * {@code biaya} yang mentah, bukan dari {@link #getBiaya()}. Bila {@link #getBiaya()} belum pernah
 * dipanggil pada instance itu, diskon dihitung di atas nominal basi/nol. Kode pemanggil di repo ini
 * memang selalu membaca biaya lebih dulu, tapi jangan diasumsikan aman di kode baru.</li>
 * <li><b>Duplikasi field {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah
 * KEHARUSAN, bukan bug.</b> {@link GeneralValueObject} bukan {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti
 * apa pun dari sana. Setiap entity harus mendeklarasikan ulang kolom-kolom ini agar tersimpan.
 * Jangan "merapikan" dengan menghapusnya.</li>
 * <li><b>Baris tidak pernah benar-benar dihapus.</b> {@link #getAktif()} (default {@code true})
 * menandai baris hidup; menonaktifkan baris juga <b>mengosongkan</b> {@link #getKodeUnik()} supaya
 * kunci unik-nya bebas dipakai baris pengganti. Lihat {@link #getKodeUnik()}.</li>
 * </ul>
 *
 * <p>Komentar asli file ini &mdash; <i>"Bank generated by hbm2java"</i> &mdash; adalah sisa kepala
 * berkas hasil generator Hibernate Tools (April 2010) yang salah salin dari entity {@code Bank};
 * sengaja dicatat di sini agar tidak menyesatkan pembaca berikutnya.</p>
 *
 * @see Kegiatan
 * @see DetailBiaya
 * @see ItemBiaya
 * @see CicilanPembayaran
 * @see PengaturanPembayaranBulanan
 * @see KegiatanPersistenceHelper
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "detail_kegiatan")
public class DetailKegiatan extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya sengaja dipatok (bukan dihitung ulang kompilator) karena instance
	 * entity ini ikut diserialisasi ke cache MapDB dan ke sesi ZK; mengubahnya membuat data cache
	 * lama gagal dibaca.
	 */
	private static final long serialVersionUID = 2463822577548439808L;
	/** Primary key {@code detail_kegiatan.id}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Identitas (username/NIP) pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris tagihan ini.
	 *
	 * @return isi kolom {@code olehId}, atau {@code null} bila belum pernah diisi
	 * @see ais.database.model.GeneralValueObject
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah.
	 *
	 * <p>Setter ini <b>menolak nilai kosong secara diam-diam</b>: {@code null} atau string yang hanya
	 * berisi spasi diabaikan sehingga nilai lama tetap bertahan. Konsekuensinya jejak audit tidak
	 * bisa "dikosongkan" lewat setter, dan pemanggil yang mengira sudah membersihkannya akan keliru.
	 * Perilaku ini sengaja dipertahankan agar interceptor audit tidak menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId identitas pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai {@code null}
	 * atau kosong diabaikan diam-diam dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris tagihan ini.
	 *
	 * @return isi kolom {@code oleh}, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence tepat sebelum {@code UPDATE}
	 * dieksekusi, dan mendelegasikan pengisian {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * ke {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Jangan panggil manual. Callback ini <b>tidak</b> berjalan pada {@code INSERT} (hanya
	 * {@code @PreUpdate}) maupun pada perubahan lewat HQL/SQL bulk update.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat, lalu
	 * ditimpa {@link #onUpdate()} setiap kali baris diperbarui.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris tagihan ini.
	 *
	 * @return waktu perubahan terakhir (presisi {@code TIMESTAMP})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log dan debug, berbentuk {@code "<id>-<biaya>"}.
	 *
	 * <p>Sengaja membaca <b>field</b> {@code biaya} langsung, bukan {@link #getBiaya()}, supaya
	 * mencetak objek ini (mis. di log atau di debugger) tidak memicu rantai perhitungan nominal
	 * beserta efek sampingnya. Karena itu nilai yang tampil bisa berbeda dari nominal final yang
	 * dilihat pengguna &mdash; ini bukan sumber kebenaran, hanya penanda.</p>
	 *
	 * @return string {@code "<id>-<biaya mentah>"}
	 */
	public String toString() {
		return id + "-" + biaya;
	}

	/** Nominal tagihan baris ini (rupiah). Sering dihitung ulang oleh {@link #getBiaya()}. */
	private Double biaya = 0.0;
	/** Potongan diskon hasil {@link #hitungDiskon(Double)}; ditulis ulang oleh {@link #getDiskon()}. */
	private Double diskon = 0.0;
	/** Bila {@code true}, baris ini tampil tetapi TIDAK ikut dijumlahkan sebagai tagihan; lihat {@link #getBukanTagihan()}. */
	private Boolean bukanTagihan;
	/** Slot diskon per-orang ke-1; lihat {@link #getDiskonMahasiswaData()}. */
	private DiskonMahasiswa diskonMahasiswaData;
	/** Slot diskon per-orang ke-2; lihat {@link #getDiskonMahasiswaData2()}. */
	private DiskonMahasiswa diskonMahasiswaData2;
	/** Slot diskon per-orang ke-3; lihat {@link #getDiskonMahasiswaData3()}. */
	private DiskonMahasiswa diskonMahasiswaData3;
	/** Sumber nominal untuk tagihan bulanan (SPP per bulan); lihat {@link #getPengaturanPembayaranBulanan()}. */
	private PengaturanPembayaranBulanan pengaturanPembayaranBulanan;
	/** Baris master biaya yang di-instansiasi baris ini; lihat {@link #getDetailBiaya()}. */
	private DetailBiaya detailBiaya;
	/** Keterangan bebas; bisa disisipi teks diskon oleh {@link #getKeterangan()}. */
	private String keterangan;
	/** Uraian tambahan untuk cetakan tagihan; lihat {@link #getUraian()}. */
	private String uraian;
	/** Wadah tagihan induk; lihat {@link #getKegiatan()}. */
	private Kegiatan kegiatan;
	/** Kegiatan sementara (draft/simulasi) bila baris ini bagian dari wizard; lihat {@link #getKegiatanTemporary()}. */
	private KegiatanTemporary kegiatanTemporary;
	/** Jejak posting akunting bila baris ini sudah dijurnal; lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;
	/** Tanggal jatuh tempo efektif; sering dihitung ulang oleh {@link #getTanggal()}. */
	private Date tanggal;
	/** Tanggal jatuh tempo yang dipaksa operator; bila terisi mengalahkan semua aturan otomatis. */
	private Date tanggalCustom = null;
	/** Jenis biaya baris ini; lihat {@link #getItemBiaya()}. */
	private ItemBiaya itemBiaya;
	/** Pengguna yang "mengunci" nominal baris ini; lihat {@link #getKunci()}. */
	private Tbmuser kunci;
	/** Nominal manual yang berlaku selama baris terkunci; lihat {@link #getBiayaTemporary()}. */
	private Double biayaTemporary;

	/** Nominal denda manual yang ditambahkan ke biaya; lihat {@link #getDendaCustom()}. */
	private Double dendaCustom;
	/** Saklar pengaktif {@link #dendaCustom}; lihat {@link #getMenggunakanDendaCustom()}. */
	private Boolean menggunakanDendaCustom;

	/** Kunci logis anti-duplikat baris tagihan; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Penanda baris hidup (default {@code true}); lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Bila {@code true}, denda keterlambatan tidak dikenakan pada baris ini; lihat {@link #getBatalkanDenda()}. */
	private Boolean batalkanDenda;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Semua properti diisi lewat setter atau
	 * lewat mesin tagihan di {@code KegiatanHelper}.
	 */
	public DetailKegiatan() {
	}

	/**
	 * Mengembalikan primary key baris tagihan ini.
	 *
	 * <p>Kolomnya {@code insertable = false} karena nilainya dibangkitkan sequence database
	 * ({@code GenerationType.IDENTITY}); jadi {@code id} baru terisi setelah baris benar-benar
	 * di-{@code flush}. Beberapa method di kelas tetangganya (mis.
	 * {@code Kegiatan.appendDetailKegiatan}) diam-diam mengabaikan objek yang {@code id}-nya masih
	 * {@code null}.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Umumnya hanya dipanggil Hibernate; pengisian manual dipakai pada jalur
	 * salin/impor data.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nominal tagihan baris ini secara langsung.
	 *
	 * <p>Perlu diingat bahwa nilai yang di-set di sini <b>bisa ditimpa</b> pada pembacaan berikutnya:
	 * {@link #getBiaya()} akan menghitung ulang nominal bila baris tidak terkunci dan jenis biayanya
	 * ber-{@code nilaiBisaDiubah = false}. Untuk membuat nominal manual bertahan, baris harus dikunci
	 * ({@link #setKunci(Tbmuser)}) dan nilainya diletakkan di {@link #setBiayaTemporary(Double)}.</p>
	 *
	 * @param biaya nominal tagihan dalam rupiah
	 */
	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/**
	 * Mengembalikan nominal tagihan baris ini, <b>menghitungnya ulang lebih dulu</b> dari sumber
	 * biaya yang berlaku. Ini getter terpenting di kelas ini dan sekaligus yang paling banyak efek
	 * sampingnya.
	 *
	 * <h4>Urutan sumber nominal (prioritas dari atas)</h4>
	 * <ol>
	 * <li><b>Baris terkunci</b> &mdash; bila {@link #getKunci()} tidak {@code null} dan
	 * {@link #getBiayaTemporary()} terisi, nominal manual itulah yang dipakai apa adanya. Inilah
	 * satu-satunya cara membuat nominal hasil {@link #setBiaya(Double)} bertahan dari perhitungan
	 * ulang.</li>
	 * <li><b>Tagihan bulanan</b> &mdash; bila ada {@link PengaturanPembayaranBulanan} yang
	 * {@link ItemBiaya}-nya ber-{@code nilaiBisaDiubah = false}, nominal diambil dari
	 * {@code pengaturanPembayaranBulanan.getNominal()}, lalu <b>digantikan</b> hasil
	 * {@code ambilNominalModifikasi(mahasiswa, semester)} bila kegiatan sudah punya mahasiswa dan
	 * semester (mekanisme "nominal per mahasiswa/per semester").</li>
	 * <li><b>Biaya biasa</b> &mdash; bila ada {@link ItemBiaya} + {@link DetailBiaya} dan
	 * {@code nilaiBisaDiubah = false}, nominal diambil dari {@code detailBiaya.getNilaiBiayaBaru()}
	 * bila terisi, jika tidak dari {@code detailBiaya.getNilaiBiaya()}.</li>
	 * <li><b>Tidak satu pun cocok</b> &mdash; field {@code biaya} yang tersimpan dipakai apa adanya
	 * (kasus item yang nominalnya memang boleh diketik kasir).</li>
	 * </ol>
	 *
	 * <p>Setelah nominal dasar ditentukan, {@link #getDendaCustom()} ditambahkan bila
	 * {@link #getMenggunakanDendaCustom()} bernilai {@code true} &mdash; berlaku pada cabang 2 dan 3
	 * saja, <b>tidak</b> pada baris terkunci.</p>
	 *
	 * <h4>Efek samping (penting)</h4>
	 * <ul>
	 * <li>Menulis balik ke field {@code biaya}, {@code kunci}, dan (pada cabang 3) {@code kegiatan}.
	 * Pada instance yang masih <i>attached</i>, penulisan ini bisa ikut ter-{@code flush} sehingga
	 * membaca tagihan berarti mengubahnya di database, lengkap dengan revisi Envers.</li>
	 * <li><b>Menulis ke object LAIN.</b> Pada cabang 3, bila kegiatan punya mahasiswa dan jenis
	 * biayanya bukan {@link ItemBiaya#TIDAK_ADA_PENGHITUNGAN}, dipanggil
	 * {@code detailBiaya.updateKeterangan(mahasiswa, semester)}. Method itu
	 * ({@code PembayaranNominalModifikasiHelper.updateKeterangan}) melakukan query Hibernate dan
	 * men-<i>set</i> {@code nilaiBiayaBaru} pada {@link DetailBiaya} &mdash; entity <b>master yang
	 * dibagi banyak mahasiswa</b>. Jadi membaca biaya SATU baris tagihan dapat mengubah baris master
	 * yang dilihat mahasiswa lain. Ini pola sejenis dengan {@code CicilanPembayaran.getKegiatan()};
	 * dampaknya bergantung pada apakah {@code DetailBiaya} tersebut <i>attached</i> saat itu.</li>
	 * <li>Cabang 2 dan 3 memanggil {@link #getKegiatan()}, {@link #getItemBiaya()},
	 * {@link #getPengaturanPembayaranBulanan()} yang masing-masing dapat memicu lazy-load /
	 * query database.</li>
	 * <li>Seluruh badan method dibungkus {@code try/catch(Exception)} yang hanya mencatat ke
	 * {@code ErrorAuditUtil}: kegagalan perhitungan <b>tidak dilempar</b>, nominal lama yang keluar.
	 * Tagihan yang "diam-diam tidak berubah" biasanya berakar di sini.</li>
	 * </ul>
	 *
	 * <p>Bila hanya butuh membaca nominal tersimpan tanpa satu pun efek samping di atas, pakai
	 * {@link #ambilBiaya()}.</p>
	 *
	 * @return nominal tagihan setelah perhitungan ulang; {@code 0.0} bila field-nya {@code null}
	 * @see #ambilBiaya()
	 * @see #getDiskon()
	 * @see #getBiayaTemporary()
	 */
	@Column(name = "biaya", nullable = false, length = 50)
	public Double getBiaya() {
		kunci = getKunci();
		try {
			Kegiatan kegiatanSnapshot = kegiatan == null ? getKegiatan() : kegiatan;
			Double nominalTerkunci = kegiatanSnapshot == null ? null
					: kegiatanSnapshot.ambilNominalTagihanTerkunci(detailBiaya,
							getPengaturanPembayaranBulanan(), this);
			if (nominalTerkunci != null) {
				biaya = nominalTerkunci;
			} else if (kunci != null && biayaTemporary != null) {
				biaya = biayaTemporary;
			} else if (getPengaturanPembayaranBulanan() != null
					&& getPengaturanPembayaranBulanan().getDetailBiaya() != null
					&& getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya() != null
					&& !getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya().getNilaiBisaDiubah()) {

				Double ni = pengaturanPembayaranBulanan.getNominal();
				if (getKegiatan() != null && getKegiatan().getMahasiswa() != null
						&& getKegiatan().getSemster() != null) {
					ni = pengaturanPembayaranBulanan.ambilNominalModifikasi(getKegiatan().getMahasiswa(),
							getKegiatan().getSemster());
				}

				if (getMenggunakanDendaCustom()) {
					ni = getDendaCustom() + ni;
				}

				biaya = ni;
			} else if (getItemBiaya() != null && detailBiaya != null && !getItemBiaya().getNilaiBisaDiubah()) {

				if (kegiatan == null) {
					kegiatan = getKegiatan();
				}

				Double ni;
				if (kegiatan != null && kegiatan.getMahasiswa() != null
						&& !getItemBiaya().getPenghitungan().equals(ItemBiaya.TIDAK_ADA_PENGHITUNGAN)) {
					detailBiaya.updateKeterangan(kegiatan.getMahasiswa(), kegiatan.getSemster());
					ni = (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
							: detailBiaya.getNilaiBiayaBaru());
				} else {
					ni = detailBiaya.getNilaiBiaya();
				}

				if (getMenggunakanDendaCustom()) {
					ni = getDendaCustom() + ni;
				}

				biaya = ni;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/DetailKegiatan.java:183");
		}

		return biaya == null ? 0.0 : biaya;
	}

	/**
	 * Membaca nominal tagihan <b>apa adanya dari field</b>, tanpa perhitungan ulang dan tanpa satu pun
	 * efek samping {@link #getBiaya()} (tidak menyentuh {@link DetailBiaya}, tidak memicu lazy-load,
	 * tidak menulis balik).
	 *
	 * <p>Karena Hibernate memetakan properti lewat getter, method ini sengaja diberi nama berawalan
	 * {@code ambil} agar <b>tidak</b> dianggap properti persisten. Pakai method ini di laporan,
	 * penjumlahan massal, dan log &mdash; yaitu di mana pun nominal cukup dibaca dan efek samping
	 * justru berbahaya.</p>
	 *
	 * @return nominal tersimpan; {@code 0.0} bila {@code null}
	 * @see #getBiaya()
	 */
	public Double ambilBiaya() {
		return biaya == null ? 0.0 : biaya;
	}

	/**
	 * Menetapkan {@link Kegiatan} induk baris tagihan ini <b>sekaligus mendaftarkan diri</b> ke induk
	 * tersebut.
	 *
	 * <p>Efek samping penting: bila argumennya bukan {@code null}, dipanggil
	 * {@code kegiatan.appendDetailKegiatan(this)} yang menyisipkan {@code "<id>:true"} ke string CSV
	 * {@code Kegiatan.detailKegiatans}. Jadi setter ini <b>mengubah object lain</b>. Perhatikan dua
	 * konsekuensinya:</p>
	 * <ul>
	 * <li>Bila baris ini belum tersimpan ({@link #getId()} masih {@code null}), pendaftaran
	 * <b>diabaikan diam-diam</b> oleh {@code appendDetailKegiatan}. Baris baru karenanya harus
	 * di-{@code flush} lebih dulu, atau didaftarkan ulang lewat
	 * {@link KegiatanPersistenceHelper#updateDetailKegiatan(java.util.List, Kegiatan, boolean)}.</li>
	 * <li>Setter ini tidak pernah <b>melepas</b> pendaftaran dari kegiatan lama saat baris dipindah;
	 * pelepasan dikerjakan terpisah lewat {@link #populateHapusPembayaran(DetailKegiatan, Kegiatan)}.</li>
	 * </ul>
	 *
	 * @param kegiatan wadah tagihan induk; boleh {@code null} untuk melepas relasi
	 */
	public void setKegiatan(Kegiatan kegiatan) {
		this.kegiatan = kegiatan;
		if (kegiatan != null) {
			kegiatan.appendDetailKegiatan(this);
		}
	}

	/**
	 * Mengembalikan {@link Kegiatan} induk baris tagihan ini.
	 *
	 * <p>Berbeda dengan kebanyakan getter di kelas ini, getter ini murni membaca field &mdash; tidak
	 * ada perhitungan, penulisan balik, maupun {@code check()}. Bila relasi belum ter-inisialisasi,
	 * yang dikembalikan bisa berupa proxy lazy Hibernate.</p>
	 *
	 * @return kegiatan induk, atau {@code null} bila baris belum dikaitkan
	 * @see #setKegiatan(Kegiatan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kegiatan", nullable = true)
	public Kegiatan getKegiatan() {
		return kegiatan;
	}

	/**
	 * Menetapkan baris master {@link DetailBiaya} yang di-instansiasi baris tagihan ini.
	 *
	 * <p>Perhatikan bahwa {@link #getDetailBiaya()} dapat menimpa nilai ini bila baris punya
	 * {@link PengaturanPembayaranBulanan}.</p>
	 *
	 * @param detailBiaya baris master biaya; boleh {@code null}
	 */
	public void setDetailBiaya(DetailBiaya detailBiaya) {
		this.detailBiaya = detailBiaya;
	}

	/**
	 * Mengembalikan baris master {@link DetailBiaya} sumber nominal baris tagihan ini.
	 *
	 * <p><b>Efek samping:</b> bila baris punya {@link PengaturanPembayaranBulanan} yang sudah
	 * menunjuk sebuah {@code DetailBiaya}, field {@code detailBiaya} <b>ditimpa</b> dengan nilai dari
	 * pengaturan bulanan tersebut. Aturannya: untuk tagihan bulanan, pengaturan bulanan adalah sumber
	 * kebenaran, dan kolom {@code detail_biaya} di baris ini hanya menyusul. Penulisan ini dapat
	 * ter-{@code flush} bila instance masih <i>attached</i>.</p>
	 *
	 * <p>Kolomnya dideklarasikan {@code nullable = false}, tetapi getter ini tetap bisa mengembalikan
	 * {@code null} pada objek yang belum lengkap diisi (mis. baris hasil wizard yang belum disimpan);
	 * pemanggil tetap wajib menjaga null.</p>
	 *
	 * @return baris master biaya, atau {@code null} bila belum terisi
	 * @see #getItemBiaya()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_biaya", nullable = false)
	public DetailBiaya getDetailBiaya() {
		if (pengaturanPembayaranBulanan != null && pengaturanPembayaranBulanan.getDetailBiaya() != null) {
			detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
		}
		return detailBiaya;
	}

	/**
	 * Mengembalikan keterangan baris tagihan, <b>menyisipkan teks diskon</b> lebih dulu bila baris ini
	 * memakai slot diskon per-orang pertama.
	 *
	 * <p>Bila {@link #getDiskonMahasiswaData()} menghasilkan diskon yang punya
	 * {@code JenisDiskonMahasiswa}, dan keterangan yang tersimpan belum memuat kata "diskon"
	 * (perbandingan huruf kecil), maka ke keterangan ditambahkan potongan teks
	 * {@code ", Diskon : <nama jenis diskon> senilai <nominal terformat>"}. Nominalnya diambil dari
	 * {@link #getDiskon()}, yang berarti getter ini ikut menjalankan seluruh perhitungan diskon.</p>
	 *
	 * <h4>Efek samping dan jebakan</h4>
	 * <ul>
	 * <li>Menulis balik ke field {@code keterangan} dan {@code diskonMahasiswaData}. Karena
	 * {@code keterangan} adalah kolom persisten ({@code text}), teks sisipan itu dapat ikut
	 * ter-{@code flush} ke database beserta revisi Envers &mdash; membaca keterangan berarti
	 * mengubahnya.</li>
	 * <li><b>Jebakan {@code null}:</b> penjaga kondisinya berbunyi "keterangan {@code null} ATAU belum
	 * memuat kata diskon", tetapi penggabungannya memakai {@code +=}. Bila {@code keterangan} masih
	 * {@code null}, hasilnya adalah string yang <b>diawali literal {@code "null"}</b>, yaitu
	 * {@code "null, Diskon : ..."}. Inilah asal-usul teks "null, Diskon..." yang kadang tampak di
	 * kartu tagihan. Dicatat apa adanya, tidak diperbaiki di sini karena data lama sudah terlanjur
	 * tersimpan dalam bentuk itu.</li>
	 * <li><b>Tidak lengkap:</b> hanya slot diskon pertama yang disebut. Diskon dari
	 * {@code KelompokMahasiswa}, dari {@code JenisSeleksi}, dari promo global, maupun dari slot 2/3
	 * tetap mengurangi nominal lewat {@link #hitungDiskon(Double)} tanpa pernah muncul di keterangan.
	 * Nama diskon yang benar-benar dipakai mesin tagihan ada di {@link #cariJenisDiskonMahasiswa()}.</li>
	 * </ul>
	 *
	 * @return keterangan baris tagihan (mungkin sudah ditambah teks diskon), bisa {@code null}
	 * @see #cariJenisDiskonMahasiswa()
	 * @see #getUraian()
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {

		diskonMahasiswaData = getDiskonMahasiswaData();
		if (diskonMahasiswaData != null && diskonMahasiswaData.getJenisDiskonMahasiswa() != null) {
			if (keterangan == null || !keterangan.toLowerCase().contains("diskon")) {
				keterangan += ", Diskon : " + diskonMahasiswaData.getJenisDiskonMahasiswa().getNama() + " senilai "
						+ Common.numberFormat.get().format(getDiskon());
			}
		}

		return keterangan;
	}

	/**
	 * Menetapkan keterangan baris tagihan.
	 *
	 * @param keterangan teks keterangan bebas; boleh {@code null}
	 * @see #getKeterangan()
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link KegiatanTemporary} (kegiatan draft/simulasi) tempat baris ini dibuat,
	 * bila ada.
	 *
	 * <p>Baris tagihan yang lahir dari wizard pembayaran/daftar ulang mula-mula digantung pada
	 * kegiatan sementara; nilai ini ikut membentuk {@link #getKodeUnik()} sehingga tagihan draft
	 * tidak bertabrakan dengan tagihan resmi untuk item yang sama. Getter murni, tanpa efek samping.</p>
	 *
	 * @return kegiatan sementara, atau {@code null} untuk tagihan resmi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kegiatan_temporary", nullable = true)
	public KegiatanTemporary getKegiatanTemporary() {
		return kegiatanTemporary;
	}

	/**
	 * Menetapkan kegiatan sementara pemilik baris ini.
	 *
	 * <p>Mengubah nilai ini mengubah hasil {@link #getKodeUnik()}, jadi jangan diubah pada baris yang
	 * sudah tersimpan kecuali memang sedang memindahkan tagihan draft menjadi resmi.</p>
	 *
	 * @param kegiatanTemporary kegiatan sementara; boleh {@code null}
	 */
	public void setKegiatanTemporary(KegiatanTemporary kegiatanTemporary) {
		this.kegiatanTemporary = kegiatanTemporary;
	}

//	public Boolean getBaru() {
//		return baru == null ? true : baru;
//	}
//
//	public void setBaru(Boolean baru) {
//		this.baru = baru;
//	}

	/**
	 * Mengembalikan {@link PengaturanPembayaranBulanan} sumber nominal baris ini (skema tagihan
	 * bulanan, mis. SPP per bulan), bila baris ini memang tagihan bulanan.
	 *
	 * <p>Bila terisi, pengaturan bulanan menjadi <b>sumber kebenaran tertinggi</b> di kelas ini: ia
	 * mengalahkan {@link #getDetailBiaya()} dan {@link #getItemBiaya()} (keduanya diambil ulang dari
	 * pengaturan ini), menentukan nominal di {@link #getBiaya()}, ikut menentukan tanggal jatuh tempo
	 * di {@link #getTanggal()}, dan menjadi komponen pertama {@link #getKodeUnik()} (prefiks
	 * {@code _B_}). Getter murni, tanpa efek samping.</p>
	 *
	 * @return pengaturan pembayaran bulanan, atau {@code null} untuk tagihan non-bulanan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengaturan_pembayaran_bulanan", nullable = true)
	public PengaturanPembayaranBulanan getPengaturanPembayaranBulanan() {
		return pengaturanPembayaranBulanan;
	}

	/**
	 * Menetapkan pengaturan pembayaran bulanan sumber nominal baris ini.
	 *
	 * <p>Mengisinya mengubah nominal, tanggal jatuh tempo, sekaligus {@link #getKodeUnik()} baris ini
	 * &mdash; jangan disetel ulang pada baris yang sudah punya pembayaran.</p>
	 *
	 * @param pengaturanPembayaranBulanan pengaturan bulanan; boleh {@code null}
	 */
	public void setPengaturanPembayaranBulanan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		this.pengaturanPembayaranBulanan = pengaturanPembayaranBulanan;
	}

	/**
	 * Mengembalikan uraian tambahan baris tagihan (teks bebas yang dicetak di kartu/kwitansi).
	 *
	 * <p>Berbeda dengan {@link #getKeterangan()}, getter ini murni dan <b>menormalkan {@code null}
	 * menjadi string kosong</b> agar aman langsung dirangkai di JSP/ZUL tanpa penjagaan null.
	 * Akibatnya pemanggil tidak bisa membedakan "belum pernah diisi" dari "sengaja dikosongkan".</p>
	 *
	 * @return uraian tambahan, atau string kosong bila belum diisi (tidak pernah {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getUraian() {
		return uraian == null ? "" : uraian;
	}

	/**
	 * Menetapkan uraian tambahan baris tagihan.
	 *
	 * @param uraian teks uraian; boleh {@code null} (akan dibaca sebagai string kosong)
	 */
	public void setUraian(String uraian) {
		this.uraian = uraian;
	}

	/**
	 * Mengembalikan {@link ItemBiaya} (jenis biaya) baris tagihan ini, <b>menurunkannya ulang</b> dari
	 * relasi yang lebih berwenang bila ada.
	 *
	 * <p>Urutan penentuan:</p>
	 * <ol>
	 * <li>Nilai field di-resolve dulu lewat {@code check()} milik {@link GeneralValueObject} (memaksa
	 * proxy lazy menjadi objek nyata / mengambil instance kanonik; dapat memicu query).</li>
	 * <li>Bila ada {@link PengaturanPembayaranBulanan} yang punya {@code DetailBiaya} yang punya
	 * {@code ItemBiaya}, itulah yang dipakai.</li>
	 * <li>Bila tidak, dan field {@code detailBiaya} punya {@code ItemBiaya}, itu yang dipakai.</li>
	 * <li>Bila tidak keduanya, nilai field dipertahankan.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> hasilnya <b>ditulis balik</b> ke field {@code itemBiaya}, sehingga
	 * kolom {@code item_biaya} baris ini dapat berubah hanya karena dibaca. Konsekuensi praktisnya
	 * positif &mdash; kolom itu otomatis "menyembuhkan diri" mengikuti master &mdash; tetapi berarti
	 * pembacaan pun perlu diperlakukan sebagai operasi tulis. Perhatikan juga bahwa langkah 2 dan 3
	 * membaca <b>field</b> {@code detailBiaya} mentah, bukan {@link #getDetailBiaya()}, jadi hasilnya
	 * bisa berbeda tergantung apakah {@link #getDetailBiaya()} sudah pernah dipanggil.</p>
	 *
	 * @return jenis biaya baris ini, atau {@code null} bila belum bisa ditentukan
	 * @see #getDetailBiaya()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);

		if (pengaturanPembayaranBulanan != null && pengaturanPembayaranBulanan.getDetailBiaya() != null
				&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null) {
			itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
		} else if (detailBiaya != null && detailBiaya.getItemBiaya() != null) {
			itemBiaya = detailBiaya.getItemBiaya();
		}

		return this.itemBiaya;
	}

	/**
	 * Menetapkan jenis biaya baris ini.
	 *
	 * <p>Ingat bahwa {@link #getItemBiaya()} akan menimpanya lagi bila {@link #getDetailBiaya()} atau
	 * {@link #getPengaturanPembayaranBulanan()} menunjuk jenis biaya lain.</p>
	 *
	 * @param itemBiaya jenis biaya; boleh {@code null}
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Menetapkan tanggal jatuh tempo tersimpan.
	 *
	 * <p>Nilai ini bersifat <b>cache</b>: {@link #getTanggal()} akan menghitung ulang dan menimpanya
	 * pada pembacaan berikutnya. Untuk memaksa tanggal tertentu, pakai
	 * {@link #setTanggalCustom(Date)} yang mengalahkan seluruh aturan otomatis.</p>
	 *
	 * @param tanggal tanggal jatuh tempo; boleh {@code null}
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan <b>tanggal jatuh tempo efektif</b> baris tagihan ini, menghitungnya ulang dari
	 * kalender akademik. Ini method dengan percabangan terbanyak di kelas ini.
	 *
	 * <h4>Urutan aturan</h4>
	 * <ol>
	 * <li><b>{@link #getTanggalCustom()} terisi</b> &rarr; dipakai apa adanya; seluruh aturan di
	 * bawah dilewati. Inilah "kunci manual" tanggal jatuh tempo.</li>
	 * <li><b>Tagihan bulanan dengan bendera "tanggal tagihan selalu awal bulan"</b> &rarr; tanggal
	 * dibangun sebagai <b>tanggal 1 pukul 07:00</b> pada bulan {@code getRealBulan()}. Tahunnya
	 * ditebak dari string tahun akademik {@code "2025/2026"}: semester genap dengan bulan &lt; 10
	 * memakai potongan kedua, semester ganjil dengan bulan &gt; 5 memakai potongan pertama, sisanya
	 * memakai potongan kedua. Aturan tebak-tahun ini asumsi kalender Indonesia (ganjil = Sep&ndash;Feb)
	 * dan akan salah bila format tahun akademik menyimpang dari {@code "awal/akhir"}.</li>
	 * <li><b>{@code detailBiaya.getDefaultTanggalTagihan()} terisi</b> &rarr; dipakai.</li>
	 * <li><b>{@code itemBiaya.getTanggalTagihanMengikutiRencanaTahunAkademik()}</b> &rarr; tanggal
	 * diambil dari kalender akademik: untuk calon mahasiswa dari tanggal mulai gelombang
	 * pendaftarannya; selain itu dari {@code RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(...)}
	 * berdasarkan fakultas/jurusan/program/tahun akademik dan paritas semester.</li>
	 * </ol>
	 *
	 * <p>Setelah itu ada dua <b>penimpa khusus calon mahasiswa</b> yang dievaluasi belakangan dan
	 * karenanya mengalahkan hasil di atas: bila jenis kegiatannya
	 * {@code ConstantValues.PENDAFTARAN_CALON_MAHASISWA} dipakai
	 * {@code gelombangPendaftaran.getTanggalTagihanRegistrasi()}, dan bila
	 * {@code ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU} dipakai
	 * {@code getTanggalTagihanDaftarUlang()}.</p>
	 *
	 * <h4>Efek samping dan jebakan</h4>
	 * <ul>
	 * <li>Menulis balik ke field {@code tanggal} (kolom persisten) dan ke field {@code kegiatan};
	 * pada instance <i>attached</i> perubahan ini bisa ter-{@code flush}.</li>
	 * <li>Cabang 2&ndash;4 membaca <b>field</b> {@code kegiatan}/{@code detailBiaya}/
	 * {@code pengaturanPembayaranBulanan} mentah, bukan getternya, sehingga hasil bisa berbeda
	 * bergantung getter mana yang sudah dipanggil sebelumnya pada instance yang sama.</li>
	 * <li>Cabang 4 memanggil {@code getItemBiaya().getTanggalTagihanMengikutiRencanaTahunAkademik()}
	 * <b>tanpa penjagaan null</b>. Pada baris yang belum punya jenis biaya, ini melempar
	 * {@code NullPointerException} yang langsung ditelan {@code catch(Exception)} di bawahnya
	 * (blok bertanda {@code auto-audit(empty-catch)}), sehingga seluruh perhitungan tanggal berhenti
	 * diam-diam dan nilai lama/{@code null} yang dikembalikan. Ini penjelasan gejala "tanggal jatuh
	 * tempo tiba-tiba jadi hari ini".</li>
	 * <li>Method ini dapat memicu query database (pencarian {@code RencanaTahunAkademik}) &mdash;
	 * hindari memanggilnya di dalam perulangan besar untuk laporan.</li>
	 * </ul>
	 *
	 * @return tanggal jatuh tempo; bila tidak ada nilai yang bisa ditentukan, dikembalikan
	 *         <b>waktu sekarang</b> ({@code WaktuUtil.getDate()}), bukan {@code null}
	 * @see #getTanggalCustom()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		try {

			if (tanggalCustom != null) {
				tanggal = tanggalCustom;
			} else {
				if (pengaturanPembayaranBulanan != null
						&& pengaturanPembayaranBulanan.getTanggalTagihanSelaluDibuatAwalBulan() && kegiatan != null
						&& kegiatan.getTahunAkademik() != null) {

					int tahun = 0;
					if (kegiatan.getSemster() % 2 == 0 && pengaturanPembayaranBulanan.getRealBulan() < 10) {
						tahun = Integer.parseInt(kegiatan.getTahunAkademik().split("/")[1]);
					} else if (kegiatan.getSemster() % 2 == 1 && pengaturanPembayaranBulanan.getRealBulan() > 5) {
						tahun = Integer.parseInt(kegiatan.getTahunAkademik().split("/")[0]);
					} else {
						tahun = Integer.parseInt(kegiatan.getTahunAkademik().split("/")[1]);
					}

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.YEAR, tahun);
					calendar.set(Calendar.MONTH, pengaturanPembayaranBulanan.getRealBulan() - 1);
					calendar.set(Calendar.DATE, 1);
					calendar.set(Calendar.HOUR_OF_DAY, 7);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					tanggal = calendar.getTime();
				} else if (detailBiaya != null && detailBiaya.getDefaultTanggalTagihan() != null) {
					tanggal = detailBiaya.getDefaultTanggalTagihan();
				} else if (getItemBiaya().getTanggalTagihanMengikutiRencanaTahunAkademik()) {
					kegiatan = getKegiatan();

					if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
							&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null) {
						tanggal = kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getMulai();
					} else {
						Jurusan jurusan = null;
						if (kegiatan != null && kegiatan.getJurusan() != null) {
							jurusan = kegiatan.getJurusan();
						}
						String program = null;
						if (kegiatan != null && kegiatan.getProgram() != null) {
							program = kegiatan.getProgram();
						}

						String mulai = kegiatan == null ? Perkuliahan.GANJIL
								: kegiatan.getMahasiswa() != null ? kegiatan.getMahasiswa().getSemesterMulai()
										: kegiatan.getCalonMahasiswa() != null
												? kegiatan.getCalonMahasiswa().getSemesterMulai()
												: "";

						if (mulai.isEmpty() || mulai.equals(Perkuliahan.GANJIL)) {
							RencanaTahunAkademik s = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(
									jurusan == null ? null : jurusan.getFakultas(), jurusan, null, null, null, null,
									program, null, kegiatan == null ? null : kegiatan.getTahunAkademik(),
									kegiatan == null ? null
											: kegiatan.getSemster() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
							if (s != null) {
								tanggal = s.getTanggalMulai();
							}
						} else {
							RencanaTahunAkademik s = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(
									jurusan == null ? null : jurusan.getFakultas(), jurusan, null, null, null, null,
									program, null, kegiatan == null ? null : kegiatan.getTahunAkademik(),
									kegiatan == null ? null
											: kegiatan.getSemster() % 2 == 1 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
							if (s != null) {
								tanggal = s.getTanggalMulai();
							}
						}
					}
				}

				if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanRegistrasi() != null
						&& kegiatan.getJenisKegiatan() != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
						&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()
								.equals(kegiatan.getJenisKegiatan().getId())) {
					tanggal = kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanRegistrasi();
				}

				else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran() != null
						&& kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanDaftarUlang() != null
						&& kegiatan.getJenisKegiatan() != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
						&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()
								.equals(kegiatan.getJenisKegiatan().getId())) {
					tanggal = kegiatan.getCalonMahasiswa().getGelombangPendaftaran().getTanggalTagihanDaftarUlang();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailKegiatan.java:395");
			// TODO: handle exception
		}

		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Mengembalikan jejak posting akunting untuk baris tagihan ini.
	 *
	 * <p>Terisi bila tagihan ini sudah dijurnal ke buku besar oleh mesin posting. Nilai bukan
	 * {@code null} secara praktis berarti baris ini <b>tidak boleh lagi diubah nominalnya</b> tanpa
	 * membatalkan postingnya lebih dulu &mdash; penegakan aturan itu ada di lapisan action/helper
	 * ({@code PostingDetailKegiatanAction} dan kerabatnya), bukan di entity ini. Getter murni.</p>
	 *
	 * @return riwayat posting akunting, atau {@code null} bila belum pernah diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan jejak posting akunting baris ini. Umumnya hanya dipanggil mesin posting dan mesin
	 * pembatalan posting.
	 *
	 * @param postingHistory riwayat posting; {@code null} untuk menandai belum/batal diposting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan pengguna yang <b>mengunci</b> nominal baris tagihan ini.
	 *
	 * <p>Nilai bukan {@code null} berarti nominal baris ini ditetapkan manual oleh petugas tersebut
	 * dan tersimpan di {@link #getBiayaTemporary()}; {@link #getBiaya()} lalu berhenti menghitung
	 * ulang dan memakai nominal manual itu. Lapisan UI memakai
	 * {@code detailKegiatan.getKunci() != null} sebagai penanda "baris terkunci" pada renderer dan
	 * ekspor Excel.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check()} ditulis balik ke field {@code kunci} (resolusi
	 * proxy lazy; dapat memicu query). Bukan perubahan nilai bisnis, tetapi tetap sebuah penulisan.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak terkunci
	 * @see #getBiayaTemporary()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kunci", nullable = true)
	public Tbmuser getKunci() {
		kunci = check(kunci);
		return kunci;
	}

	/**
	 * Mengunci atau membuka kunci nominal baris tagihan ini.
	 *
	 * <p>Mengisi dengan seorang {@link Tbmuser} membuat nominal manual di
	 * {@link #setBiayaTemporary(Double)} berlaku permanen sampai kunci dilepas; mengisi {@code null}
	 * mengembalikan baris ke perhitungan otomatis {@link #getBiaya()}.</p>
	 *
	 * @param kunci pengguna pengunci, atau {@code null} untuk membuka kunci
	 */
	public void setKunci(Tbmuser kunci) {
		this.kunci = kunci;
	}

	/**
	 * Mengembalikan nominal manual yang berlaku saat baris terkunci.
	 *
	 * <p>Berpasangan dengan {@link #getKunci()} dan {@link #getBiaya()}:</p>
	 * <ul>
	 * <li>Baris <b>terkunci</b> &rarr; nilai simpanan dikembalikan apa adanya, dan
	 * {@link #getBiaya()} memakainya sebagai nominal final.</li>
	 * <li>Baris <b>tidak terkunci</b> &rarr; field ini <b>ditimpa</b> dengan hasil
	 * {@link #getBiaya()}, yakni nominal otomatis terkini. Jadi kotak isian nominal di UI selalu
	 * memperlihatkan angka yang sedang berlaku, dan begitu petugas menekan "kunci", angka itulah yang
	 * membeku.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field {@code kunci} dan {@code biayaTemporary}, serta
	 * ikut menanggung seluruh efek samping {@link #getBiaya()} (termasuk penulisan ke
	 * {@link DetailBiaya}) pada cabang tidak-terkunci.</p>
	 *
	 * @return nominal manual, atau nominal otomatis terkini bila baris tidak terkunci; bisa
	 *         {@code null} pada baris terkunci yang nominal manualnya belum pernah diisi
	 * @see #getKunci()
	 * @see #getBiaya()
	 */
	public Double getBiayaTemporary() {
		kunci = getKunci();
		if (kunci == null) {
			biayaTemporary = getBiaya();
		}
		return biayaTemporary;
	}

	/**
	 * Menetapkan nominal manual baris terkunci.
	 *
	 * <p>Nilai ini hanya berpengaruh selama {@link #getKunci()} tidak {@code null}; pada baris tidak
	 * terkunci ia akan ditimpa lagi oleh {@link #getBiayaTemporary()}.</p>
	 *
	 * @param biayaTemporary nominal manual dalam rupiah; boleh {@code null}
	 */
	public void setBiayaTemporary(Double biayaTemporary) {
		this.biayaTemporary = biayaTemporary;
	}

	/**
	 * Mencari <b>jenis diskon</b> ({@link JenisDiskonMahasiswa}) yang berlaku untuk baris tagihan ini
	 * &mdash; yaitu "diskon apa namanya", bukan "berapa rupiahnya".
	 *
	 * <p>Method ini adalah pasangan pelabelan dari {@link #hitungDiskon(Double)}: keduanya menelusuri
	 * <b>rantai prioritas yang sama</b>, tetapi yang satu mengembalikan objek jenis diskon (untuk
	 * ditampilkan di UI/laporan) dan yang lain mengembalikan nominal potongan. Karena logikanya
	 * disalin ganda, perubahan aturan diskon <b>harus dilakukan di kedua method</b> agar label dan
	 * nominal tidak berbeda.</p>
	 *
	 * <h4>Rantai prioritas</h4>
	 * <ol>
	 * <li><b>Kelompok mahasiswa</b> &mdash; diskon dari {@code mahasiswa.getKelompokMahasiswa()},
	 * asal semester kegiatan berada dalam rentang {@code smtMulai}&ndash;{@code smtSampai} kelompok,
	 * jenis diskonnya {@code cocokUntukKegiatan(kegiatan, detailBiaya)}, dan {@code itemBiaya} baris
	 * ini termasuk dalam daftar item yang didiskon.</li>
	 * <li><b>Jenis seleksi calon mahasiswa</b> &mdash; diskon jalur masuk untuk kegiatan milik calon
	 * mahasiswa, dengan tambahan penyaringan rentang {@code semesterMulai}/{@code semesterSampai}
	 * pada jenis diskonnya ({@code null} berarti tanpa batas).</li>
	 * <li><b>Jenis seleksi mahasiswa</b> &mdash; sama seperti butir 2, untuk kegiatan milik mahasiswa
	 * aktif.</li>
	 * <li><b>Slot diskon per-orang</b> &mdash; berturut-turut {@link #getDiskonMahasiswaData()},
	 * lalu <b>promo global</b> {@code JenisDiskonMahasiswa.cariPromoGlobal(...)} (diskon "berlaku
	 * untuk semua mahasiswa"), lalu {@link #getDiskonMahasiswaData2()} dan
	 * {@link #getDiskonMahasiswaData3()}.</li>
	 * </ol>
	 *
	 * <p>Seluruh cabang 1&ndash;3 dan promo global disyaratkan {@code !adaDiskon()}, artinya diskon
	 * yang <b>ditautkan langsung</b> ke baris ini (slot per-orang) selalu menang atas diskon
	 * kolektif.</p>
	 *
	 * <h4>Efek samping</h4>
	 * <ul>
	 * <li>Menulis balik ke field {@code kegiatan}, {@code detailBiaya}, dan ketiga
	 * {@code diskonMahasiswaData*} &mdash; termasuk kemungkinan <b>menghapus tautan diskon</b>,
	 * lihat peringatan di {@link #getDiskonMahasiswaData()}.</li>
	 * <li>Memanggil {@link #getBiaya()} (untuk menakar promo global), sehingga mewarisi seluruh efek
	 * sampingnya termasuk penulisan ke {@link DetailBiaya}.</li>
	 * <li>Bagian slot per-orang dibungkus {@code try/catch(Exception)} yang hanya mencatat error;
	 * bila terjadi kegagalan, method mengembalikan {@code null} seolah tidak ada diskon.</li>
	 * </ul>
	 *
	 * <p>Dipanggil dari renderer/laporan tagihan (mis. {@code TagihanUIBuilder},
	 * {@code DetailPembayaranMahasiswaRenderer}) untuk mencetak nama diskon di baris tagihan.</p>
	 *
	 * @return jenis diskon yang berlaku, atau {@code null} bila baris ini tidak mendapat diskon apa pun
	 * @see #hitungDiskon(Double)
	 * @see #adaDiskon()
	 */
	public JenisDiskonMahasiswa cariJenisDiskonMahasiswa() {
		kegiatan = getKegiatan();
		detailBiaya = getDetailBiaya();

		if (kegiatan != null && kegiatan.getMahasiswa() != null && detailBiaya != null
				&& detailBiaya.getItemBiaya() != null && kegiatan.getMahasiswa().getKelompokMahasiswa() != null
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() <= kegiatan.getSemster()
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() >= kegiatan.getSemster()

				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa() != null && !(adaDiskon())
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa()
						.cocokUntukKegiatan(kegiatan, detailBiaya)
				&& !kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
						.isEmpty()
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
						.contains(detailBiaya.getItemBiaya().getId())) {

			return kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa();
		} else {

			if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getJenisSeleksi() != null
				&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
				&& !(adaDiskon())
				&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
						.cocokUntukKegiatan(kegiatan, detailBiaya)
				&& !kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
						.isEmpty()
					&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.contains(detailBiaya.getItemBiaya().getId())

					&&

					(

					kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterMulai() == null
							|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() != null
									&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() <= kegiatan.getSemster())

					)

					&&

					(

					kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterSampai() == null
							|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() != null
									&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() >= kegiatan.getSemster())

					)

			) {

				return kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa();

			}

			else if (kegiatan != null && kegiatan.getMahasiswa() != null
					&& kegiatan.getMahasiswa().getJenisSeleksi() != null
					&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null && !(adaDiskon())
					&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
							.cocokUntukKegiatan(kegiatan, detailBiaya)
					&& !kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.isEmpty()
					&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.contains(detailBiaya.getItemBiaya().getId())

					&&

					(

					kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterMulai() == null
							|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() != null
									&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() <= kegiatan.getSemster())

					)

					&&

					(

					kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterSampai() == null
							|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() != null
									&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() >= kegiatan.getSemster())

					)

			) {

				return kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa();

			}

			else {

				diskonMahasiswaData = getDiskonMahasiswaData();
				diskonMahasiswaData2 = getDiskonMahasiswaData2();
				diskonMahasiswaData3 = getDiskonMahasiswaData3();

				try {

					if (diskonCocok(diskonMahasiswaData)) {
						return diskonMahasiswaData.getJenisDiskonMahasiswa();

					}

					// PROMO GLOBAL ("Berlaku Untuk Semua Mahasiswa") — lihat catatan di
					// JenisDiskonMahasiswa.cariPromoGlobal. Dipakai agar keterangan diskon pada
					// baris tagihan menyebut nama promo yang benar-benar dipakai mesin tagihan.
					JenisDiskonMahasiswa promoGlobal = JenisDiskonMahasiswa.cariPromoGlobal(kegiatan, detailBiaya,
							getBiaya());
					if (promoGlobal != null && !adaDiskon()) {
						return promoGlobal;
					}

					if (diskonCocok(diskonMahasiswaData2)) {
						return diskonMahasiswaData2.getJenisDiskonMahasiswa();

					}

					if (diskonCocok(diskonMahasiswaData3)) {
						return diskonMahasiswaData3.getJenisDiskonMahasiswa();

					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/DetailKegiatan.java:555");
				}
			}
		}

		return null;
	}

	/**
	 * Menghitung <b>nominal potongan diskon</b> untuk baris tagihan ini atas dasar nominal yang
	 * diberikan pemanggil.
	 *
	 * <p>Menelusuri rantai prioritas yang sama persis dengan {@link #cariJenisDiskonMahasiswa()}
	 * (kelompok mahasiswa &rarr; jenis seleksi calon mahasiswa &rarr; jenis seleksi mahasiswa &rarr;
	 * slot diskon per-orang / promo global), tetapi mengembalikan rupiah, bukan objek jenis diskon.
	 * Untuk tiap diskon yang cocok, potongan dihitung sebagai
	 * {@code nominal * (persen / 100)} bila {@code getBerupaPersen()} bernilai {@code true}, atau
	 * angka tetap {@code getDiskon()} bila tidak.</p>
	 *
	 * <h4>Perilaku yang perlu diperhatikan</h4>
	 * <ul>
	 * <li><b>Slot per-orang bersifat akumulatif.</b> Pada cabang terakhir, slot 1, 2, dan 3
	 * dijumlahkan ({@code +=}) sehingga seorang mahasiswa bisa menerima tiga diskon sekaligus.
	 * Sebaliknya cabang kelompok mahasiswa memakai penugasan ({@code =}) &mdash; hanya satu diskon.
	 * Asimetri ini disengaja dalam arti "diskon kolektif tidak menumpuk", tetapi patut diketahui
	 * saat menelusuri selisih perhitungan.</li>
	 * <li><b>Tidak ada plafon pada jalur biasa.</b> Hanya potongan <b>promo global</b> yang
	 * dibatasi tidak melebihi nominal ({@code if (potongan > jumlahDiskon) potongan = jumlahDiskon}).
	 * Kombinasi tiga slot per-orang secara teoretis bisa melampaui nominal tagihan dan menghasilkan
	 * tagihan negatif; pembatasan itu, bila ada, dilakukan pemanggil.</li>
	 * <li><b>Promo global mengembalikan hasil lebih awal</b> ({@code return potongan}) sehingga
	 * mematikan penjumlahan slot 1&ndash;3. Promo global hanya diperiksa bila tidak ada satu pun
	 * diskon per-orang yang cocok.</li>
	 * <li>Parameter {@code biaya} <b>menutupi (shadow)</b> field {@code biaya}, dan variabel lokal
	 * {@code diskon} menutupi field {@code diskon}; di dalam badan method, field-field itu tidak
	 * pernah tersentuh langsung. Yang menulis field {@code diskon} adalah {@link #getDiskon()}.</li>
	 * <li><b>Efek samping:</b> menulis balik ke field {@code kegiatan}, {@code detailBiaya}, dan
	 * ketiga {@code diskonMahasiswaData*} (termasuk kemungkinan mengosongkannya &mdash; lihat
	 * {@link #getDiskonMahasiswaData()}); memicu lazy-load; menelan exception ke
	 * {@code ErrorAuditUtil} sehingga kegagalan tampil sebagai diskon yang lebih kecil, bukan
	 * sebagai error.</li>
	 * </ul>
	 *
	 * @param biaya nominal dasar yang akan didiskon (biasanya hasil {@link #getBiaya()} atau field
	 *              {@code biaya} mentah bila dipanggil dari {@link #getDiskon()}); dipakai sebagai
	 *              basis persentase
	 * @return besar potongan dalam rupiah; {@code 0.0} bila tidak ada diskon yang berlaku
	 * @see #getDiskon()
	 * @see #cariJenisDiskonMahasiswa()
	 */
	public Double hitungDiskon(Double biaya) {
		Double jumlahDiskon = biaya;
		kegiatan = getKegiatan();
		detailBiaya = getDetailBiaya();
		Double diskon = 0.0;
		if (kegiatan != null && kegiatan.getMahasiswa() != null && detailBiaya != null
				&& detailBiaya.getItemBiaya() != null && kegiatan.getMahasiswa().getKelompokMahasiswa() != null
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtMulai() <= kegiatan.getSemster()
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getSmtSampai() >= kegiatan.getSemster()

				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa() != null && !(adaDiskon())
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa()
						.cocokUntukKegiatan(kegiatan, detailBiaya)
				&& !kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
						.isEmpty()
				&& kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().ambilItemBiayaIds()
						.contains(detailBiaya.getItemBiaya().getId())) {

			diskon = (kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getBerupaPersen()
					? (jumlahDiskon
							* (kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getDiskon()
									/ 100.0))
					: kegiatan.getMahasiswa().getKelompokMahasiswa().getJenisDiskonMahasiswa().getDiskon());

		} else {

			if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getJenisSeleksi() != null
				&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null
				&& !(adaDiskon())
				&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
						.cocokUntukKegiatan(kegiatan, detailBiaya)
				&& !kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
						.isEmpty()
					&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.contains(detailBiaya.getItemBiaya().getId())

					&&

					(

					kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterMulai() == null
							|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() != null
									&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() <= kegiatan.getSemster())

					)

					&&

					(

					kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterSampai() == null
							|| (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() != null
									&& kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() >= kegiatan.getSemster())

					)

			) {

				diskon += (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getBerupaPersen()
						? (jumlahDiskon
								* (kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getDiskon()
										/ 100.0))
						: kegiatan.getCalonMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getDiskon());

			}

			else if (kegiatan != null && kegiatan.getMahasiswa() != null
					&& kegiatan.getMahasiswa().getJenisSeleksi() != null
					&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa() != null && !(adaDiskon())
					&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
							.cocokUntukKegiatan(kegiatan, detailBiaya)
					&& !kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.isEmpty()
					&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().ambilItemBiayaIds()
							.contains(detailBiaya.getItemBiaya().getId())

					&&

					(

					kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterMulai() == null
							|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterMulai() != null
									&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterMulai() <= kegiatan.getSemster())

					)

					&&

					(

					kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getSemesterSampai() == null
							|| (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
									.getSemesterSampai() != null
									&& kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa()
											.getSemesterSampai() >= kegiatan.getSemster())

					)

			) {

				diskon += (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getBerupaPersen()
						? (jumlahDiskon
								* (kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getDiskon()
										/ 100.0))
						: kegiatan.getMahasiswa().getJenisSeleksi().getJenisDiskonMahasiswa().getDiskon());

			}

			else {

				diskonMahasiswaData = getDiskonMahasiswaData();
				diskonMahasiswaData2 = getDiskonMahasiswaData2();
				diskonMahasiswaData3 = getDiskonMahasiswaData3();

				// PROMO GLOBAL ("Berlaku Untuk Semua Mahasiswa"): hanya bila tidak ada diskon
				// per-orang yang berlaku pada baris ini (tautan/assignment tetap diprioritaskan).
				boolean adaDiskonPerOrang = (diskonMahasiswaData != null && diskonCocok(diskonMahasiswaData))
						|| (diskonMahasiswaData2 != null && diskonCocok(diskonMahasiswaData2))
						|| (diskonMahasiswaData3 != null && diskonCocok(diskonMahasiswaData3));
				if (!adaDiskonPerOrang && !adaDiskon()) {
					JenisDiskonMahasiswa promoGlobal = JenisDiskonMahasiswa.cariPromoGlobal(kegiatan, detailBiaya,
							jumlahDiskon);
					if (promoGlobal != null) {
						double potongan = promoGlobal.getBerupaPersen()
								? (jumlahDiskon * (promoGlobal.getDiskon() / 100.0))
								: promoGlobal.getDiskon();
						if (potongan > jumlahDiskon) {
							potongan = jumlahDiskon;
						}
						return potongan;
					}
				}

				try {

					if (diskonMahasiswaData != null && diskonCocok(diskonMahasiswaData)) {
						diskon += (diskonMahasiswaData.getJenisDiskonMahasiswa().getBerupaPersen()
								? (jumlahDiskon * (diskonMahasiswaData.getJenisDiskonMahasiswa().getDiskon() / 100.0))
								: diskonMahasiswaData.getJenisDiskonMahasiswa().getDiskon());

					}

					if (diskonMahasiswaData2 != null && diskonCocok(diskonMahasiswaData2)) {
						diskon += (diskonMahasiswaData2.getJenisDiskonMahasiswa().getBerupaPersen()
								? (jumlahDiskon * (diskonMahasiswaData2.getJenisDiskonMahasiswa().getDiskon() / 100.0))
								: diskonMahasiswaData2.getJenisDiskonMahasiswa().getDiskon());

					}

					if (diskonMahasiswaData3 != null && diskonCocok(diskonMahasiswaData3)) {
						diskon += (diskonMahasiswaData3.getJenisDiskonMahasiswa().getBerupaPersen()
								? (jumlahDiskon * (diskonMahasiswaData3.getJenisDiskonMahasiswa().getDiskon() / 100.0))
								: diskonMahasiswaData3.getJenisDiskonMahasiswa().getDiskon());

					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/DetailKegiatan.java:702");
				}
			}
		}
		return diskon == null ? 0.0 : diskon;
	}

	/**
	 * Mengembalikan besar potongan diskon baris tagihan ini, <b>menghitungnya ulang</b> lewat
	 * {@link #hitungDiskon(Double)}.
	 *
	 * <p><b>Jebakan urutan pemanggilan.</b> Basis perhitungannya adalah <b>field</b> {@code biaya}
	 * mentah, bukan {@link #getBiaya()}. Pada instance yang baru dimuat dan belum pernah dibaca
	 * biayanya, field itu masih berisi nilai tersimpan (atau {@code 0.0} pada objek baru), sehingga
	 * diskon berupa persen dihitung di atas nominal yang belum diperbarui. Kode pemanggil di repo ini
	 * memang selalu memanggil {@link #getBiaya()} lebih dulu (dan {@link #getBiaya()} menulis balik
	 * ke field itu), tetapi ketergantungan urutan ini tidak dijamin oleh apa pun &mdash; pada kode
	 * baru, panggil {@link #getBiaya()} dulu, atau pakai
	 * {@code hitungDiskon(getBiaya())} secara eksplisit.</p>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field {@code diskon} (kolom persisten) dan mewarisi
	 * seluruh efek samping {@link #hitungDiskon(Double)}.</p>
	 *
	 * @return potongan diskon dalam rupiah; {@code 0.0} bila tidak ada
	 * @see #hitungDiskon(Double)
	 */
	public Double getDiskon() {
		diskon = hitungDiskon(biaya);
		return diskon == null ? 0.0 : diskon;
	}

	/**
	 * Menetapkan besar potongan diskon tersimpan.
	 *
	 * <p>Bersifat cache belaka: {@link #getDiskon()} menimpanya pada pembacaan berikutnya. Untuk
	 * benar-benar memberi diskon, tautkan {@link DiskonMahasiswa} lewat
	 * {@link #setDiskonMahasiswaData(DiskonMahasiswa)} dan kerabatnya.</p>
	 *
	 * @param diskon potongan dalam rupiah; boleh {@code null}
	 */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Memeriksa apakah baris ini punya <b>diskon per-orang</b> yang berlaku pada salah satu dari tiga
	 * slot {@code diskonMahasiswaData}.
	 *
	 * <p>Dipakai sebagai penjaga di {@link #hitungDiskon(Double)} dan
	 * {@link #cariJenisDiskonMahasiswa()}: bila bernilai {@code true}, seluruh diskon kolektif
	 * (kelompok mahasiswa, jenis seleksi, promo global) <b>dilewati</b> supaya tidak menumpuk di atas
	 * diskon yang sudah ditautkan khusus ke mahasiswa ini.</p>
	 *
	 * <p><b>Efek samping:</b> tiga field {@code diskonMahasiswaData*} di-resolve lewat {@code check()}
	 * dan ditulis balik; dapat memicu lazy-load/query. Perhatikan bahwa di sini dipakai {@code check()}
	 * langsung, <b>bukan</b> getter penyaringnya, sehingga penyaringan rentang semester tidak
	 * diterapkan &mdash; sebuah diskon yang sudah lewat semesternya masih bisa membuat method ini
	 * mengembalikan {@code true} kalau {@link #diskonCocok(DiskonMahasiswa)} meloloskannya, padahal
	 * {@link #hitungDiskon(Double)} pada akhirnya tidak memakainya. Akibatnya, pada kasus itu diskon
	 * kolektif ikut terblokir sementara diskon per-orang juga tidak diberikan.</p>
	 *
	 * @return {@code true} bila ada minimal satu slot diskon per-orang yang cocok
	 * @see #diskonCocok(DiskonMahasiswa)
	 */
	public boolean adaDiskon() {
		diskonMahasiswaData = check(diskonMahasiswaData);
		diskonMahasiswaData2 = check(diskonMahasiswaData2);
		diskonMahasiswaData3 = check(diskonMahasiswaData3);
		return diskonCocok(diskonMahasiswaData) || diskonCocok(diskonMahasiswaData2)
				|| diskonCocok(diskonMahasiswaData3);
	}

	/**
	 * Menguji apakah sebuah tautan {@link DiskonMahasiswa} benar-benar berlaku untuk baris tagihan ini.
	 *
	 * <p>Syaratnya berlapis empat: tautannya ada, tautannya {@code aktif}, jenis diskonnya ada dan
	 * {@code aktif}, serta jenis diskon itu {@code cocokUntukKegiatan(getKegiatan(), getDetailBiaya())}
	 * &mdash; yakni cocok dengan jenis kegiatan dan item biaya baris ini.</p>
	 *
	 * <p>Perhatikan bahwa penyaringan <b>rentang semester</b> TIDAK dilakukan di sini, melainkan di
	 * {@link #getDiskonMahasiswaData()} dan kembarannya. Karena itu hasil method ini bergantung pada
	 * apakah pemanggil menyodorkan objek hasil getter (sudah tersaring) atau field mentah.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getKegiatan()} dan {@link #getDetailBiaya()},
	 * sehingga ikut menulis balik field {@code detailBiaya} bila ada pengaturan bulanan.</p>
	 *
	 * @param diskonMahasiswa tautan diskon yang diuji; boleh {@code null}
	 * @return {@code true} bila diskon tersebut berlaku untuk baris ini
	 */
	private boolean diskonCocok(DiskonMahasiswa diskonMahasiswa) {
		return diskonMahasiswa != null && diskonMahasiswa.getAktif()
				&& diskonMahasiswa.getJenisDiskonMahasiswa() != null
				&& diskonMahasiswa.getJenisDiskonMahasiswa().getAktif()
				&& diskonMahasiswa.getJenisDiskonMahasiswa().cocokUntukKegiatan(getKegiatan(), getDetailBiaya());
	}

	/**
	 * Mengembalikan tautan diskon per-orang <b>slot ke-1</b>, setelah menyaringnya terhadap semester
	 * kegiatan.
	 *
	 * <p>Bila diskon punya batas {@code semesterMulai}/{@code semesterSampai} dan semester kegiatan
	 * berada di luar rentang itu, method mengembalikan {@code null} &mdash; diskon dianggap belum
	 * berlaku atau sudah kedaluwarsa untuk baris tagihan ini.</p>
	 *
	 * <p><b>PERINGATAN &mdash; getter ini menghapus data.</b> Penyaringan di atas tidak sekadar
	 * memengaruhi nilai kembalian: ia <b>menulis {@code null} ke field</b> {@code diskonMahasiswaData}.
	 * Field itu dipetakan ke kolom {@code diskon_mahasiswa_data}, sehingga pada instance yang masih
	 * <i>attached</i> pada sesi Hibernate aktif, sekadar <b>membaca</b> baris tagihan di luar rentang
	 * semester dapat membuat tautan diskonnya <b>terhapus permanen</b> dari database (lengkap dengan
	 * revisi Envers). Setelah itu, bila semester kembali masuk rentang, diskon tidak muncul lagi
	 * karena tautannya sudah hilang &mdash; kerusakan bersifat satu arah. Ini pola sejenis dengan
	 * temuan "getter yang mengubah object lain" pada {@code CicilanPembayaran.getKegiatan()}, hanya
	 * saja di sini korbannya adalah kolom baris ini sendiri. Dicatat apa adanya; tidak diperbaiki di
	 * sesi dokumentasi ini.</p>
	 *
	 * <p><b>Efek samping lain:</b> {@code check()} me-resolve proxy lazy dan menulis balik; field
	 * {@code kegiatan} juga ditimpa hasil {@link #getKegiatan()}.</p>
	 *
	 * @return tautan diskon slot 1 yang berlaku, atau {@code null}
	 * @see #getDiskonMahasiswaData2()
	 * @see #diskonCocok(DiskonMahasiswa)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon_mahasiswa_data", nullable = true)
	public DiskonMahasiswa getDiskonMahasiswaData() {
		diskonMahasiswaData = check(diskonMahasiswaData);
		kegiatan = getKegiatan();

		if (diskonMahasiswaData != null && kegiatan != null
				&& ((diskonMahasiswaData.getSemesterMulai() != null
						&& diskonMahasiswaData.getSemesterMulai() > kegiatan.getSemster())
						|| (diskonMahasiswaData.getSemesterSampai() != null
								&& diskonMahasiswaData.getSemesterSampai() < kegiatan.getSemster()))) {
			diskonMahasiswaData = null;
		}

		return diskonMahasiswaData;
	}

	/**
	 * Menautkan diskon per-orang pada slot ke-1.
	 *
	 * @param diskonMahasiswaData tautan diskon; boleh {@code null} untuk melepas
	 * @see #getDiskonMahasiswaData()
	 */
	public void setDiskonMahasiswaData(DiskonMahasiswa diskonMahasiswaData) {
		this.diskonMahasiswaData = diskonMahasiswaData;
	}

	/**
	 * Mengembalikan tautan diskon per-orang <b>slot ke-2</b>, setelah dua tahap penyaringan.
	 *
	 * <p>Selain penyaringan rentang semester yang sama seperti {@link #getDiskonMahasiswaData()},
	 * slot ini juga <b>ditolak bila jenis diskonnya sama persis dengan slot 1</b>. Aturan itu
	 * mencegah satu jenis diskon yang tanpa sengaja ditautkan dua kali dihitung ganda oleh
	 * {@link #hitungDiskon(Double)} yang menjumlahkan ketiga slot.</p>
	 *
	 * <p><b>PERINGATAN yang sama seperti slot 1:</b> penolakan dikerjakan dengan menulis {@code null}
	 * ke field {@code diskonMahasiswaData2} (kolom {@code diskon_mahasiswa_data_2}), sehingga
	 * pembacaan pada instance <i>attached</i> dapat menghapus tautan diskon secara permanen. Method
	 * ini juga memanggil {@link #getDiskonMahasiswaData()} lebih dulu, jadi ia mewarisi risiko
	 * penghapusan pada slot 1 sekaligus.</p>
	 *
	 * <p>Pembandingan jenis diskon dibungkus {@code try/catch} bertanda
	 * {@code auto-audit(empty-catch)} karena {@code getJenisDiskonMahasiswa()} atau {@code getId()}
	 * bisa {@code null}; bila melempar, penolakan tidak terjadi dan slot 2 lolos apa adanya &mdash;
	 * berpotensi diskon ganda.</p>
	 *
	 * @return tautan diskon slot 2 yang berlaku, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon_mahasiswa_data_2", nullable = true)
	public DiskonMahasiswa getDiskonMahasiswaData2() {
		diskonMahasiswaData2 = check(diskonMahasiswaData2);

		diskonMahasiswaData = getDiskonMahasiswaData();

		try {
			if (diskonMahasiswaData2 != null && diskonMahasiswaData != null && diskonMahasiswaData2
					.getJenisDiskonMahasiswa().getId().equals(diskonMahasiswaData.getJenisDiskonMahasiswa().getId())) {
				diskonMahasiswaData2 = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailKegiatan.java:763");
			// TODO: handle exception
		}

		kegiatan = getKegiatan();

		if (diskonMahasiswaData2 != null && kegiatan != null
				&& ((diskonMahasiswaData2.getSemesterMulai() != null
						&& diskonMahasiswaData2.getSemesterMulai() > kegiatan.getSemster())
						|| (diskonMahasiswaData2.getSemesterSampai() != null
								&& diskonMahasiswaData2.getSemesterSampai() < kegiatan.getSemster()))) {
			diskonMahasiswaData2 = null;
		}

		return diskonMahasiswaData2;
	}

	/**
	 * Menautkan diskon per-orang pada slot ke-2.
	 *
	 * <p>Nama parameternya {@code diskonMahasiswaData} (tanpa angka), berbeda dari field tujuannya
	 * {@code diskonMahasiswaData2} &mdash; penamaan warisan, bukan salah sasaran.</p>
	 *
	 * @param diskonMahasiswaData tautan diskon; boleh {@code null} untuk melepas
	 * @see #getDiskonMahasiswaData2()
	 */
	public void setDiskonMahasiswaData2(DiskonMahasiswa diskonMahasiswaData) {
		this.diskonMahasiswaData2 = diskonMahasiswaData;
	}

	/**
	 * Mengembalikan tautan diskon per-orang <b>slot ke-3</b>, setelah tiga tahap penyaringan.
	 *
	 * <p>Slot ini ditolak bila jenis diskonnya sama dengan slot 1 <b>atau</b> sama dengan slot 2
	 * (dua blok {@code try/catch} terpisah), lalu disaring lagi terhadap rentang semester kegiatan
	 * seperti dua slot sebelumnya. Rangkaian ini membuat ketiga slot dijamin memuat tiga jenis diskon
	 * yang berbeda sebelum dijumlahkan {@link #hitungDiskon(Double)}.</p>
	 *
	 * <p><b>PERINGATAN:</b> sama seperti slot 1 dan 2, setiap penolakan ditulis sebagai {@code null}
	 * ke field {@code diskonMahasiswaData3} sehingga pembacaan dapat menghapus tautan diskon secara
	 * permanen pada instance <i>attached</i>. Method ini memanggil {@link #getDiskonMahasiswaData2()}
	 * dan {@link #getDiskonMahasiswaData()} lebih dulu, jadi <b>satu pembacaan slot 3 berpotensi
	 * mengosongkan ketiga kolom diskon sekaligus</b>.</p>
	 *
	 * @return tautan diskon slot 3 yang berlaku, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon_mahasiswa_data_3", nullable = true)
	public DiskonMahasiswa getDiskonMahasiswaData3() {
		diskonMahasiswaData3 = check(diskonMahasiswaData3);
		diskonMahasiswaData2 = getDiskonMahasiswaData2();
		diskonMahasiswaData = getDiskonMahasiswaData();

		try {
			if (diskonMahasiswaData3 != null && diskonMahasiswaData != null && diskonMahasiswaData3
					.getJenisDiskonMahasiswa().getId().equals(diskonMahasiswaData.getJenisDiskonMahasiswa().getId())) {
				diskonMahasiswaData3 = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailKegiatan.java:796");
			// TODO: handle exception
		}

		try {
			if (diskonMahasiswaData3 != null && diskonMahasiswaData2 != null && diskonMahasiswaData3
					.getJenisDiskonMahasiswa().getId().equals(diskonMahasiswaData2.getJenisDiskonMahasiswa().getId())) {
				diskonMahasiswaData3 = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailKegiatan.java:805");
			// TODO: handle exception
		}

		kegiatan = getKegiatan();

		if (diskonMahasiswaData3 != null && kegiatan != null
				&& ((diskonMahasiswaData3.getSemesterMulai() != null
						&& diskonMahasiswaData3.getSemesterMulai() > kegiatan.getSemster())
						|| (diskonMahasiswaData3.getSemesterSampai() != null
								&& diskonMahasiswaData3.getSemesterSampai() < kegiatan.getSemster()))) {
			diskonMahasiswaData3 = null;
		}

		return diskonMahasiswaData3;
	}

	/**
	 * Menautkan diskon per-orang pada slot ke-3.
	 *
	 * @param diskonMahasiswaData tautan diskon; boleh {@code null} untuk melepas
	 * @see #getDiskonMahasiswaData3()
	 */
	public void setDiskonMahasiswaData3(DiskonMahasiswa diskonMahasiswaData) {
		this.diskonMahasiswaData3 = diskonMahasiswaData;
	}

	/**
	 * Menyatakan apakah baris ini <b>bukan</b> tagihan, yaitu ditampilkan di rincian tetapi tidak
	 * ikut menambah kewajiban bayar mahasiswa.
	 *
	 * <p>Dipakai untuk item yang dibebaskan, ditanggung pihak lain, atau sekadar informatif. Lapisan
	 * penghitung tagihan ({@code KegiatanHelper}, {@code KegiatanPersistenceHelper},
	 * {@code CicilanPembayaranAction}) memeriksa bendera ini dan melewati barisnya saat menjumlahkan.
	 * Getter menormalkan {@code null} menjadi {@code false}, jadi baris lama yang kolomnya kosong
	 * tetap dihitung sebagai tagihan.</p>
	 *
	 * @return {@code true} bila baris ini tidak menambah tagihan; tidak pernah {@code null}
	 */
	public Boolean getBukanTagihan() {
		return bukanTagihan == null ? false : bukanTagihan;
	}

	/**
	 * Menetapkan apakah baris ini bukan tagihan.
	 *
	 * @param bukanTagihan {@code true} bila baris tidak menambah kewajiban bayar
	 * @see #getBukanTagihan()
	 */
	public void setBukanTagihan(Boolean bukanTagihan) {
		this.bukanTagihan = bukanTagihan;
	}

	/**
	 * Mengembalikan nominal denda manual yang ditambahkan ke biaya baris ini.
	 *
	 * <p>Nilainya hanya berarti bila {@link #getMenggunakanDendaCustom()} bernilai {@code true};
	 * {@link #getBiaya()} menjumlahkannya ke nominal dasar pada cabang tagihan bulanan dan cabang
	 * biaya biasa (tidak pada baris terkunci).</p>
	 *
	 * <p><b>Efek samping:</b> bila saklar {@code menggunakanDendaCustom} mati, getter ini
	 * <b>menulis {@code 0.0}</b> ke field {@code dendaCustom}. Artinya mematikan saklar lalu membaca
	 * nilainya sekali sudah cukup untuk <b>menghapus angka denda yang sebelumnya diketik petugas</b>
	 * secara permanen pada instance <i>attached</i> &mdash; mengaktifkan saklar kembali tidak
	 * memulihkannya.</p>
	 *
	 * @return nominal denda manual dalam rupiah; {@code 0.0} bila saklarnya mati atau belum diisi
	 * @see #getMenggunakanDendaCustom()
	 * @see #getBatalkanDenda()
	 */
	public Double getDendaCustom() {
		if (!getMenggunakanDendaCustom()) {
			dendaCustom = 0.0;
		}
		return dendaCustom == null ? 0.0 : dendaCustom;
	}

	/**
	 * Menetapkan nominal denda manual.
	 *
	 * <p>Agar nilainya bertahan dan ikut dihitung, saklar
	 * {@link #setMenggunakanDendaCustom(Boolean)} harus dinyalakan &mdash; jika tidak,
	 * {@link #getDendaCustom()} akan menolnya kembali.</p>
	 *
	 * @param dendaCustom nominal denda dalam rupiah; boleh {@code null}
	 */
	public void setDendaCustom(Double dendaCustom) {
		this.dendaCustom = dendaCustom;
	}

	/**
	 * Saklar yang menentukan apakah {@link #getDendaCustom()} berlaku untuk baris ini.
	 *
	 * <p>Menormalkan {@code null} menjadi {@code false}, sehingga baris lama tanpa kolom ini
	 * diperlakukan sebagai "tidak memakai denda manual".</p>
	 *
	 * @return {@code true} bila denda manual dipakai; tidak pernah {@code null}
	 */
	public Boolean getMenggunakanDendaCustom() {
		return menggunakanDendaCustom == null ? false : menggunakanDendaCustom;
	}

	/**
	 * Menyalakan atau mematikan pemakaian denda manual.
	 *
	 * <p>Mematikannya berdampak merusak: pembacaan {@link #getDendaCustom()} berikutnya akan
	 * menolkan angka dendanya. Lihat peringatan di getter tersebut.</p>
	 *
	 * @param menggunakanDendaCustom {@code true} untuk memakai denda manual
	 */
	public void setMenggunakanDendaCustom(Boolean menggunakanDendaCustom) {
		this.menggunakanDendaCustom = menggunakanDendaCustom;
	}

	/**
	 * Membangun <b>kunci logis</b> sebuah baris tagihan dari objek-objek penyusunnya.
	 *
	 * <p>Kunci ini adalah identitas "tagihan yang mana" di luar primary key, dan menjadi alat utama
	 * untuk <b>mencegah tagihan ganda</b> serta untuk <b>mencocokkan tagihan dengan pembayaran</b>
	 * &mdash; ingat bahwa tidak ada foreign key antara {@code DetailKegiatan} dan
	 * {@link CicilanPembayaran}. Bentuknya:</p>
	 *
	 * <ul>
	 * <li>Ada pengaturan bulanan &rarr; {@code "<idPengaturanBulanan>_B_<idKegiatan>"}</li>
	 * <li>Tidak, tetapi ada item biaya &rarr; {@code "<idItemBiaya>_I_<idKegiatan>"}</li>
	 * <li>Tidak keduanya, atau kegiatan {@code null} &rarr; {@code null}</li>
	 * </ul>
	 *
	 * <p>Ditambah dua sufiks opsional: {@code "_T_<idKegiatanTemporary>"} bila tagihannya masih draft,
	 * dan {@code "_<bayarKe>"} bila {@code bayarKe} lebih dari 1 (cicilan ke-2 dan seterusnya).
	 * Perhatikan bahwa {@code bayarKe} bernilai {@code null} atau {@code 1} sengaja <b>tidak</b>
	 * memberi sufiks, agar kunci baris cicilan pertama tetap sama dengan kunci baris tanpa skema
	 * cicilan &mdash; jangan diubah, data lama bergantung pada bentuk ini.</p>
	 *
	 * <p>Pengaturan bulanan mengalahkan item biaya: satu baris yang punya keduanya selalu memakai
	 * bentuk {@code _B_}.</p>
	 *
	 * @param pengaturanPembayaranBulanan pengaturan tagihan bulanan; boleh {@code null}
	 * @param itemBiaya                   jenis biaya; dipakai hanya bila pengaturan bulanan
	 *                                    {@code null}; boleh {@code null}
	 * @param bayarKe                     nomor cicilan; {@code null} atau {@code 1} tidak memberi sufiks
	 * @param kegiatan                    wadah tagihan induk; <b>wajib</b>, bila {@code null} hasilnya
	 *                                    {@code null}
	 * @param kegiatanTemporary           kegiatan draft; boleh {@code null}
	 * @return kunci logis baris tagihan, atau {@code null} bila komponen wajibnya tidak lengkap
	 * @see #kodeUnik(Long, Long, Integer, Long, Long)
	 * @see #getKodeUnik()
	 */
	public static String kodeUnik(PengaturanPembayaranBulanan pengaturanPembayaranBulanan, ItemBiaya itemBiaya,
			Integer bayarKe, Kegiatan kegiatan, KegiatanTemporary kegiatanTemporary) {
		String kodeUnik = null;
		if (pengaturanPembayaranBulanan != null && kegiatan != null) {
			kodeUnik = pengaturanPembayaranBulanan.getId() + "_B_" + kegiatan.getId()
					+ (kegiatanTemporary == null ? "" : "_T_" + kegiatanTemporary.getId())
					+ (bayarKe != null && bayarKe > 1 ? "_" + bayarKe : "");
		} else if (itemBiaya != null && kegiatan != null) {
			kodeUnik = itemBiaya.getId() + "_I_" + kegiatan.getId()
					+ (kegiatanTemporary == null ? "" : "_T_" + kegiatanTemporary.getId())
					+ (bayarKe != null && bayarKe > 1 ? "_" + bayarKe : "");
		} else {
			kodeUnik = null;
		}
		return kodeUnik;
	}

	/**
	 * Varian {@link #kodeUnik(PengaturanPembayaranBulanan, ItemBiaya, Integer, Kegiatan, KegiatanTemporary)}
	 * yang bekerja langsung di atas <b>id</b>, bukan objek entity.
	 *
	 * <p>Aturan pembentukan string-nya identik dan hasilnya dijamin sama untuk id yang sama. Overload
	 * ini dipakai di jalur yang hanya memegang id &mdash; hasil proyeksi Criteria, pemrosesan
	 * berkas unggahan bank, dan gelung besar di laporan &mdash; karena ia <b>tidak menyentuh entity
	 * sama sekali</b>, sehingga tidak memicu lazy-load, tidak menjalankan query, dan tidak
	 * mengaktifkan satu pun getter beefek samping di kelas ini. Pada perulangan panjang, pilih
	 * overload ini.</p>
	 *
	 * @param pengaturanPembayaranBulanan id pengaturan tagihan bulanan; boleh {@code null}
	 * @param itemBiaya                   id jenis biaya; dipakai hanya bila pengaturan bulanan
	 *                                    {@code null}; boleh {@code null}
	 * @param bayarKe                     nomor cicilan; {@code null} atau {@code 1} tidak memberi sufiks
	 * @param kegiatan                    id kegiatan induk; <b>wajib</b>
	 * @param kegiatanTemporary           id kegiatan draft; boleh {@code null}
	 * @return kunci logis baris tagihan, atau {@code null} bila komponen wajibnya tidak lengkap
	 */
	public static String kodeUnik(Long pengaturanPembayaranBulanan, Long itemBiaya, Integer bayarKe, Long kegiatan,
			Long kegiatanTemporary) {
		String kodeUnik = null;
		if (pengaturanPembayaranBulanan != null && kegiatan != null) {
			kodeUnik = pengaturanPembayaranBulanan + "_B_" + kegiatan
					+ (kegiatanTemporary == null ? "" : "_T_" + kegiatanTemporary)
					+ (bayarKe != null && bayarKe > 1 ? "_" + bayarKe : "");
		} else if (itemBiaya != null && kegiatan != null) {
			kodeUnik = itemBiaya + "_I_" + kegiatan + (kegiatanTemporary == null ? "" : "_T_" + kegiatanTemporary)
					+ (bayarKe != null && bayarKe > 1 ? "_" + bayarKe : "");
		} else {
			kodeUnik = null;
		}
		return kodeUnik;
	}

	/**
	 * Mengembalikan kunci logis baris tagihan ini, <b>membangunnya ulang</b> setiap kali dipanggil.
	 *
	 * <p>Kolomnya {@code unique = true}, jadi nilai inilah yang ditegakkan database sebagai penjaga
	 * anti-tagihan-ganda: dua baris hidup dengan kombinasi
	 * (pengaturan bulanan atau item biaya) + kegiatan + kegiatan draft + {@code bayarKe} yang sama
	 * tidak mungkin tersimpan bersamaan.</p>
	 *
	 * <p><b>Kaitan dengan {@link #getAktif()}.</b> Bila baris tidak aktif, kunci sengaja dijadikan
	 * {@code null}. Ini yang membuat pola "hapus = nonaktifkan" bisa bekerja: baris lama yang
	 * dinonaktifkan melepaskan kunci uniknya sehingga baris pengganti dengan kombinasi yang sama
	 * boleh dibuat, sementara riwayatnya tetap tersimpan. Kolom {@code unique} pada PostgreSQL
	 * mengizinkan banyak baris bernilai {@code NULL}, jadi berapa pun banyaknya baris non-aktif tidak
	 * saling bertabrakan.</p>
	 *
	 * <h4>Efek samping dan jebakan</h4>
	 * <ul>
	 * <li>Menulis balik ke field {@code kodeUnik}, {@code pengaturanPembayaranBulanan},
	 * {@code kegiatan}, {@code itemBiaya}, dan {@code detailBiaya} &mdash; karena keempat relasinya
	 * diambil lewat getter masing-masing, getter ini mewarisi efek samping mereka &mdash; terutama
	 * penimpaan {@code detailBiaya} dan {@code itemBiaya} oleh pengaturan bulanan, serta lazy-load
	 * relasi yang belum ter-inisialisasi. ({@link #getBiaya()} tidak ikut dipanggil, jadi
	 * {@link DetailBiaya} tidak ditulisi dari sini.)</li>
	 * <li><b>Risiko {@code NullPointerException}:</b> {@code detailBiaya.getBayarKe()} dipanggil
	 * tanpa penjagaan null dan <b>tanpa</b> {@code try/catch}. Pada baris yang belum punya
	 * {@link DetailBiaya} &mdash; mungkin terjadi pada objek hasil wizard yang belum lengkap
	 * meski kolomnya dideklarasikan {@code nullable = false} &mdash; pembacaan kode unik akan
	 * melempar NPE keluar. Ini satu-satunya getter di kelas ini yang membiarkan exception lolos.</li>
	 * <li>Nomor cicilan diambil dari {@code detailBiaya.getBayarKe()}, bukan dari properti baris ini
	 * sendiri; jadi mengubah baris master {@link DetailBiaya} akan mengubah kunci unik seluruh baris
	 * tagihan yang mengacu padanya.</li>
	 * </ul>
	 *
	 * @return kunci logis baris tagihan, atau {@code null} bila baris tidak aktif
	 * @see #kodeUnik(PengaturanPembayaranBulanan, ItemBiaya, Integer, Kegiatan, KegiatanTemporary)
	 * @see #getAktif()
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (getAktif() == null || !getAktif()) {
			kodeUnik = null;
		} else {
			pengaturanPembayaranBulanan = getPengaturanPembayaranBulanan();
			kegiatan = getKegiatan();
			itemBiaya = getItemBiaya();
			detailBiaya = getDetailBiaya();
			kodeUnik = DetailKegiatan.kodeUnik(pengaturanPembayaranBulanan, itemBiaya, detailBiaya.getBayarKe(),
					kegiatan, kegiatanTemporary);
		}
		return kodeUnik;
	}

	/**
	 * Menetapkan kunci logis secara langsung.
	 *
	 * <p>Praktis tidak berguna untuk mengubah identitas baris: {@link #getKodeUnik()} membangun ulang
	 * nilainya setiap kali dipanggil, jadi apa pun yang di-set di sini akan tertimpa. Setter ini ada
	 * karena dituntut kontrak properti Hibernate.</p>
	 *
	 * @param kodeUnik kunci logis; boleh {@code null}
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Menyatakan apakah baris tagihan ini masih hidup.
	 *
	 * <p>Baris tagihan <b>tidak pernah dihapus fisik</b>; "penghapusan" dilakukan dengan
	 * menonaktifkannya, dan itu sekaligus melepaskan {@link #getKodeUnik()} (lihat penjelasan di
	 * sana). Nilai {@code null} dinormalkan menjadi <b>{@code true}</b> &mdash; perhatikan bahwa
	 * default-nya berlawanan dengan bendera boolean lain di kelas ini &mdash; supaya seluruh baris
	 * lama yang dibuat sebelum kolom ini ada tetap terhitung sebagai tagihan aktif.</p>
	 *
	 * @return {@code true} bila baris masih berlaku; tidak pernah {@code null}
	 * @see #getKodeUnik()
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengaktifkan atau menonaktifkan baris tagihan.
	 *
	 * <p>Menonaktifkan setara dengan menghapus dari sudut pandang pengguna, tetapi datanya tetap ada
	 * untuk audit dan {@link #getKodeUnik()}-nya menjadi {@code null}.</p>
	 *
	 * @param aktif {@code true} untuk baris hidup, {@code false} untuk "dihapus"
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Menyatakan apakah <b>denda keterlambatan dibebaskan</b> untuk baris tagihan ini.
	 *
	 * <p>Berbeda dari {@link #getDendaCustom()} yang menambah denda manual, bendera ini
	 * <b>meniadakan</b> denda otomatis. Pengecekannya dilakukan lapisan penghitung tagihan
	 * (mis. {@code KegiatanAction}, {@code CommonReportHelper}, {@code TagihanUIBuilder},
	 * servlet {@code TagihanMahasiswa}) dengan pola
	 * {@code (detailKegiatan.getBatalkanDenda() || nilaiYangHarusDibayar == 0)} &mdash; jadi denda
	 * juga otomatis gugur bila tagihannya sudah lunas. Entity ini sendiri tidak menghitung denda.</p>
	 *
	 * <p>Menormalkan {@code null} menjadi {@code false} (denda tetap berlaku).</p>
	 *
	 * @return {@code true} bila denda dibebaskan; tidak pernah {@code null}
	 */
	public Boolean getBatalkanDenda() {
		return batalkanDenda == null ? false : batalkanDenda;
	}

	/**
	 * Membebaskan atau memberlakukan kembali denda keterlambatan untuk baris ini.
	 *
	 * @param batalkanDenda {@code true} untuk membebaskan denda
	 * @see #getBatalkanDenda()
	 */
	public void setBatalkanDenda(Boolean batalkanDenda) {
		this.batalkanDenda = batalkanDenda;
	}

	/**
	 * Mengembalikan tanggal jatuh tempo yang <b>dipaksa operator</b>, bila ada.
	 *
	 * <p>Bila terisi, nilai ini mengalahkan seluruh aturan kalender akademik di {@link #getTanggal()}.
	 * Getter murni, tanpa efek samping &mdash; salah satu dari sedikit getter "jujur" di kelas ini,
	 * dan karena itu cara paling aman untuk mengetahui apakah sebuah baris tanggalnya manual.</p>
	 *
	 * @return tanggal jatuh tempo manual, atau {@code null} bila mengikuti aturan otomatis
	 * @see #getTanggal()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalCustom() {
		return tanggalCustom;
	}

	/**
	 * Menetapkan (atau melepas, dengan {@code null}) tanggal jatuh tempo manual.
	 *
	 * @param tanggalCustom tanggal jatuh tempo yang dipaksa; {@code null} untuk kembali ke aturan
	 *                      kalender akademik
	 */
	public void setTanggalCustom(Date tanggalCustom) {
		this.tanggalCustom = tanggalCustom;
	}

	/**
	 * Melepas satu baris tagihan dari daftar anak {@link Kegiatan} induknya (pola "hapus lunak").
	 *
	 * <p>Fasad tipis: seluruh kerjanya didelegasikan ke
	 * {@link KegiatanPersistenceHelper#hapusDetailKegiatan(DetailKegiatan, Kegiatan)}, yang menandai
	 * id baris sebagai {@code "<id>:false"} pada string CSV {@code Kegiatan.detailKegiatans} lalu
	 * menyimpan perubahannya secara asinkron. Baris {@code detail_kegiatan}-nya sendiri
	 * <b>tidak di-{@code DELETE}</b>.</p>
	 *
	 * <p>Nama {@code populate...} adalah warisan dari masa ketika seluruh logika persistensi tagihan
	 * masih tinggal di entity ini; sekarang tidak ada logika tersisa di sini. Kode baru sebaiknya
	 * memanggil {@link KegiatanPersistenceHelper} langsung. Method ini dipertahankan karena masih
	 * dipanggil dari sejumlah action lama.</p>
	 *
	 * @param detailKegiatan baris tagihan yang dilepas; diabaikan bila {@code null} atau belum punya id
	 * @param kegiatan       kegiatan induk; diabaikan bila {@code null} atau belum punya id
	 * @see KegiatanPersistenceHelper#hapusDetailKegiatan(DetailKegiatan, Kegiatan)
	 */
	public static void populateHapusPembayaran(DetailKegiatan detailKegiatan, Kegiatan kegiatan) {
		// Diarahkan ke method baru di KegiatanPersistenceHelper
		KegiatanPersistenceHelper.hapusDetailKegiatan(detailKegiatan, kegiatan);
	}

	/**
	 * Menyegarkan daftar anak {@link Kegiatan} induk agar berisi tepat baris-baris tagihan yang
	 * diberikan.
	 *
	 * <p>Fasad tipis ke
	 * {@link KegiatanPersistenceHelper#updateDetailKegiatan(java.util.List, Kegiatan, boolean)},
	 * yang membangun ulang string CSV {@code Kegiatan.detailKegiatans} dari daftar tersebut lalu
	 * menyimpannya. Argumen {@code refresh} dipatok {@code false}, artinya penyimpanan dilakukan
	 * tanpa memaksa pembacaan ulang dari database &mdash; pemanggil yang butuh perilaku
	 * {@code refresh} harus memanggil helper-nya langsung.</p>
	 *
	 * @param listDetailKegiatan daftar baris tagihan yang menjadi isi kegiatan
	 * @param kegiatan           kegiatan induk; diabaikan bila {@code null} atau belum punya id
	 * @see KegiatanPersistenceHelper#updateDetailKegiatan(java.util.List, Kegiatan, boolean)
	 */
	public static void populatePembayaran(List<DetailKegiatan> listDetailKegiatan, Kegiatan kegiatan) {
		// Diarahkan ke method baru di KegiatanPersistenceHelper
		KegiatanPersistenceHelper.updateDetailKegiatan(listDetailKegiatan, kegiatan, false);
	}

	/**
	 * Mencatat nilai tagihan untuk satu kombinasi biaya pada sebuah {@link Kegiatan}.
	 *
	 * <p>Fasad tipis ke
	 * {@link KegiatanPersistenceHelper#updatePembayaran(DetailBiaya, PengaturanPembayaranBulanan, Kegiatan, Double)},
	 * yang menyusun kunci tagihan dari pasangan {@code detailBiaya}/{@code pengaturanPembayaranBulanan}
	 * lalu memutakhirkan ringkasan nilai pada kegiatan tersebut. Perhatikan bahwa <b>tidak ada</b>
	 * objek {@code DetailKegiatan} yang dibuat di sini &mdash; meski namanya mirip, method ini bekerja
	 * di tingkat ringkasan kegiatan, bukan baris tagihan.</p>
	 *
	 * @param detailBiaya                 baris master biaya; boleh {@code null} bila memakai
	 *                                    pengaturan bulanan
	 * @param pengaturanPembayaranBulanan pengaturan tagihan bulanan; boleh {@code null}
	 * @param kegiatan                    kegiatan induk; diabaikan bila {@code null}
	 * @param n                           nilai tagihan; diabaikan bila {@code null}
	 * @see KegiatanPersistenceHelper#updatePembayaran(DetailBiaya, PengaturanPembayaranBulanan, Kegiatan, Double)
	 */
	public static void populatePembayaran(DetailBiaya detailBiaya, PengaturanPembayaranBulanan pengaturanPembayaranBulanan, Kegiatan kegiatan, Double n) {
		// Disesuaikan dengan nama method baru (updatePembayaran) di KegiatanPersistenceHelper
		KegiatanPersistenceHelper.updatePembayaran(detailBiaya, pengaturanPembayaranBulanan, kegiatan, n);
	}
}
