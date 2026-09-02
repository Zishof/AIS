package ais.database.model;

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

import ais.common.ConstantValues;
import ais.database.model.rab.SatuanKerja;

/**
 * Tabel penghubung <b>penugasan pegawai ke satuan kerja</b> (unit anggaran/RAB) — memetakan satu
 * {@link Pegawai} ke satu {@link SatuanKerja} dengan bendera {@link #getAktif() aktif} dan
 * {@link #getKeterangan() keterangan} bebas. Dipetakan ke tabel {@code public.satuan_kerja_pegawai}
 * dan diaudit penuh oleh Envers ({@link Audited}), sehingga setiap perubahan penugasan tersalin ke
 * tabel bayangan {@code new_audit.satuan_kerja_pegawai__audit}.
 *
 * <p><b>Peran sesungguhnya di dalam sistem.</b> Entity ini adalah <i>satu-satunya</i> cara memberi
 * seorang pegawai satuan kerja secara <b>eksplisit</b>, terlepas dari struktur akademiknya. Ia
 * dikonsumsi dari satu titik saja, tetapi titik itu sangat strategis:
 * {@code Pegawai.getSatuanKerja()} memanggil {@link #ambilSatuanKerja(Pegawai)} sebagai
 * <b>langkah PERTAMA</b> dan, bila ada penugasan aktif, langsung mengembalikannya tanpa menjalankan
 * rantai penyimpulan lain (jurusan → fakultas → perguruan tinggi → sekolah → satuan kerja operator
 * yang sedang login). Dengan kata lain, satu baris di tabel ini <b>menang atas seluruh rantai
 * warisan struktur akademik</b>. Dari sana nilainya merambat ke
 * {@code Tbmuser.getSatuanKerja()}/{@code Tbmuser.ambilSatuanKerja()}, lalu ke puluhan layar RAB,
 * akunting, payroll, surat, perpustakaan, dan koperasi yang memakai
 * {@code Common.getCurrentUser().ambilSatuanKerja()} untuk menentukan unit pemilik data yang baru
 * dibuat.
 *
 * <h3>Hubungan dengan pola <i>fail-open</i> {@code SekolahUtil.ambilSatuanKerjas()}</h3>
 *
 * <p>Perlu ditegaskan untuk menghindari kesimpulan yang keliru, karena kedua nama sangat mirip:
 * <b>entity ini BUKAN sumber data bagi {@code SekolahUtil.ambilSatuanKerjas()}</b> (perhatikan
 * akhiran jamak <i>-s</i>). Method utilitas itu — yang hasilnya diperiksa dengan
 * {@code satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1") : ...} pada <b>153 berkas
 * Action</b> di repo ini — sama sekali tidak menyentuh tabel {@code satuan_kerja_pegawai}. Ia
 * mengambil datanya dari dua tempat lain:</p>
 * <ol>
 * <li>daftar <b>kode</b> satuan kerja berupa <i>String</i> dipisah koma pada hak akses pengguna
 * ({@code Tbmuser.hakAkses().getSatuanKerjas()}, kolom milik {@code Tbmrole}), dicocokkan dengan
 * {@code ilike("kode", ..., EXACT)} terhadap baris {@link SatuanKerja} ber-{@code defaultItem=true};
 * atau, bila daftar itu kosong,</li>
 * <li>seluruh {@link SatuanKerja} ber-{@code defaultItem=true} milik
 * {@code Yayasan} aktif hasil resolusi {@code SekolahUtil.getYayasan()}.</li>
 * </ol>
 *
 * <p>Konsekuensinya: <b>himpunan kosong yang memicu {@code "1=1"} tidak pernah disebabkan oleh
 * "pegawai belum ditugaskan ke satuan kerja manapun"</b> — kondisi itu tidak diperiksa di jalur
 * tersebut sama sekali. Penyebab sebenarnya seluruhnya berada di luar entity ini; rinciannya
 * didokumentasikan pada {@link #ambilSatuanKerja(Pegawai)} agar tetap terbaca dari sini.</p>
 *
 * <p>Yang justru relevan: <b>layar master entity ini sendiri</b>
 * ({@code ais.action.master.SatuanKerjaPegawaiAction#initCriteria}) memakai pola fail-open yang
 * sama persis. Saat himpunan satuan kerja kosong, daftar penugasan yang tampil bukan lagi milik
 * unit pengguna melainkan <b>seluruh penugasan pegawai se-instalasi</b>, lengkap dengan nama
 * pegawai — dan dari layar itu penugasan siapa pun dapat disunting atau dihapus.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit yang dideklarasikan ulang</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #getId()} beserta setter-nya, dan {@link #onUpdate()}.</li>
 * <li><b>Relasi inti</b> — {@link #getPegawai()} dan {@link #getSatuanKerja()}, keduanya
 * {@code nullable = false}.</li>
 * <li><b>Atribut</b> — {@link #getKeterangan()} dan {@link #getAktif()}.</li>
 * <li><b>Utilitas statis</b> — {@link #ambilSatuanKerja(Pegawai)}, satu-satunya method berlogika
 * nyata di kelas ini.</li>
 * </ul>
 *
 * <h3>Catatan arsitektural: pengulangan properti warisan</h3>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti apa pun yang dideklarasikan di
 * sana. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>harus</b>
 * dideklarasikan ulang di kelas ini agar benar-benar tersimpan ke kolom; pengulangan tersebut
 * adalah keharusan teknis, bukan duplikasi yang perlu dibersihkan. Hal yang sama berlaku untuk
 * {@link #getKeterangan()}: di sini ia kolom {@code text} sungguhan, sedangkan properti senama pada
 * kelas induk hanya hidup di memori.</p>
 *
 * <h3>Kuirk yang perlu diketahui</h3>
 * <ul>
 * <li>Komentar generator di atas anotasi berbunyi <i>"Bank generated by hbm2java"</i> — sisa
 * salin-tempel dari {@code ais.database.model.Bank}, tidak ada kaitan dengan entity ini.</li>
 * <li>Kolom {@code aktif} praktis <b>tidak pernah terisi</b>: layar masternya tidak pernah
 * memanggil {@link #setAktif(Boolean)} (lihat {@link #getAktif()}).</li>
 * <li>Tidak ada batasan unik pada kolom {@code pegawai}, sehingga satu pegawai dapat memiliki lebih
 * dari satu penugasan aktif — dengan akibat yang dijelaskan pada
 * {@link #ambilSatuanKerja(Pegawai)}.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see Pegawai#getSatuanKerja()
 * @see SatuanKerja
 * @see ais.action.master.SatuanKerjaPegawaiAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "satuan_kerja_pegawai")
public class SatuanKerjaPegawai extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Primary key baris penugasan. Dideklarasikan ulang dari {@link GeneralValueObject} karena
	 * kelas induk tidak dipetakan Hibernate. Lihat {@link #getId()}.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Identitas ({@code userId}) pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * @return identitas ({@code userId}) pengguna terakhir yang menyimpan baris ini, atau
	 *         {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau string kosong/hanya spasi <b>diabaikan diam-diam</b>
	 * — field mempertahankan nilai lamanya dan method langsung kembali tanpa tanda apa pun. Jadi
	 * jejak audit tidak dapat dikosongkan kembali setelah sekali terisi.</p>
	 *
	 * @param olehId identitas pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/hanya spasi <b>diabaikan diam-diam</b>.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang menyimpan baris ini, atau {@code null} bila belum pernah
	 *         terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum setiap {@code UPDATE} baris ini.
	 * Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi ulang
	 * {@link #getOleh()}/{@link #getOlehId()} dari pengguna yang sedang login serta memperbarui
	 * {@link #getTanggal_dirubah()}. Tidak dipanggil pada {@code INSERT} pertama — pada baris baru
	 * nilai stempel waktu berasal dari inisialisasi field di bawah.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat. */ private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya dipanggil oleh
	 * {@code AuditTimestampInterceptor}, bukan oleh kode aplikasi.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini; tidak pernah {@code null} karena field
	 *         sudah diinisialisasi ke waktu server saat object dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris penugasan dalam bentuk {@code id-pegawai-satuanKerja}.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getPegawai()} dan {@link #getSatuanKerja()},
	 * sehingga dapat memicu inisialisasi proxy lazy dan query ke basis data. Jangan dipakai di
	 * jalur yang sensitif terhadap jumlah query (mis. logging di dalam perulangan besar).</p>
	 *
	 * @return teks gabungan id, pegawai, dan satuan kerja
	 */
	public String toString() {
		return id + "-" + getPegawai() + "-" + getSatuanKerja();
	}

	/** Pegawai yang ditugaskan. Wajib diisi ({@code nullable = false}). Lihat {@link #getPegawai()}. */
	private Pegawai pegawai;

	/** Satuan kerja tujuan penugasan. Wajib diisi ({@code nullable = false}). Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Catatan bebas atas penugasan. Kolom {@code text}, boleh kosong. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Bendera penugasan masih berlaku. Boleh {@code null} di basis data dan pada praktiknya memang
	 * hampir selalu {@code null}. Lihat {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Mencari satuan kerja yang ditugaskan secara eksplisit kepada seorang pegawai.
	 *
	 * <p><b>Kapan dipanggil.</b> Ini satu-satunya method berlogika nyata di kelas ini, dan
	 * satu-satunya konsumennya di seluruh repo adalah {@link Pegawai#getSatuanKerja()} — yang
	 * memanggilnya sebagai langkah <b>pertama</b> dan, bila hasilnya bukan {@code null} dan
	 * ber-{@code id}, langsung mengembalikannya sehingga seluruh rantai penyimpulan berbasis
	 * jurusan/fakultas/perguruan tinggi/sekolah <b>dilewati sepenuhnya</b>. Karena
	 * {@code Pegawai.getSatuanKerja()} dipanggil dari {@code Tbmuser.getSatuanKerja()} dan
	 * seterusnya oleh puluhan layar RAB/akunting/payroll/surat, method ini efektif berjalan sangat
	 * sering.</p>
	 *
	 * <p><b>Cara kerja.</b> Membaca <b>cache memori</b> ({@code ConstantValues.ambilBerdasarClass})
	 * berisi seluruh baris {@code SatuanKerjaPegawai} yang dimuat saat startup, lalu mengembalikan
	 * {@link SatuanKerja} milik baris <b>pertama</b> yang pegawainya cocok dan
	 * {@link #getAktif()}-nya bernilai benar.</p>
	 *
	 * <p><b>Hal-hal non-obvious yang perlu diketahui:</b></p>
	 * <ul>
	 * <li><b>Murni cache, tanpa fallback basis data.</b> Bila cache belum/gagal terisi,
	 * {@code ambilBerdasarClass} mengembalikan {@code Collections.EMPTY_MAP} dan method ini
	 * mengembalikan {@code null} <b>tanpa tanda apa pun</b> — seluruh penugasan eksplisit seolah
	 * lenyap dan setiap pegawai jatuh ke rantai penyimpulan warisan. Kelas ini terdaftar pada
	 * {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}, sehingga terlindung dari ambang
	 * {@code preload_maks_baris_kecil} (bawaan 100 baris) yang mematikan preload kelas besar; jadi
	 * risikonya terbatas pada preload yang gagal atau terinterupsi, bukan pada jumlah baris.
	 * Perubahan sesudah startup tetap terlihat karena {@code AuditListener} menyiarkan pembaruan
	 * cache setiap penyimpanan.</li>
	 * <li><b>Hasil tidak deterministik bila penugasan ganda.</b> Iterasi berjalan atas
	 * {@code values()} sebuah {@code Map} tanpa urutan yang dijamin, dan tidak ada batasan unik
	 * pada kolom {@code pegawai}. Bila satu pegawai punya lebih dari satu baris aktif, satuan kerja
	 * yang terpilih dapat <b>berbeda antar-restart</b> — dan karena hasilnya ikut menentukan unit
	 * pemilik data yang baru dibuat, atribusi anggaran bisa berpindah tanpa ada perubahan data.</li>
	 * <li><b>Bukan penyebab pola fail-open.</b> Nilai {@code null} dari sini hanya membuat
	 * {@link Pegawai#getSatuanKerja()} melanjutkan penyimpulan; ia tidak berhubungan dengan
	 * {@code SekolahUtil.ambilSatuanKerjas()} yang memicu {@code Restrictions.sqlRestriction("1=1")}
	 * di 153 Action. Himpunan kosong pada method itu terjadi bila (a) hak akses pengguna tidak
	 * mencantumkan kode satuan kerja <b>dan</b> yayasan aktif tidak teridentifikasi — termasuk pada
	 * setiap pemanggilan dari thread tanpa konteks HTTP, karena {@code SekolahUtil.getYayasan()}
	 * mengembalikan {@code new Yayasan()} ber-{@code id} {@code null} sebagai fallback; (b)
	 * instalasi yang memang tidak pernah mengisi tabel yayasan; (c) hak akses mencantumkan kode
	 * satuan kerja tetapi <b>tidak satu pun kode cocok</b> dengan baris {@link SatuanKerja}
	 * ber-{@code defaultItem=true} — sehingga salah ketik kode justru <b>memperluas</b> akses
	 * alih-alih membatasinya; atau (d) yayasan teridentifikasi tetapi belum punya satuan kerja
	 * default.</li>
	 * <li><b>Potensi {@code NullPointerException}.</b> Penjagaan hanya memeriksa
	 * {@code getPegawai() != null}, lalu langsung memanggil {@code getPegawai().getId().equals(...)}.
	 * Baris cache yang pegawainya belum ber-{@code id} akan melempar NPE ke pemanggil.</li>
	 * <li><b>Biaya per pemanggilan.</b> Perulangan memanggil {@link #getPegawai()} untuk setiap
	 * baris cache, dan getter itu menjalankan {@code check()} yang dapat meresolusi proxy lazy —
	 * biayanya tumbuh sebanding jumlah penugasan se-instalasi.</li>
	 * </ul>
	 *
	 * @param pegawai pegawai yang dicari penugasannya; {@code null} atau belum ber-{@code id}
	 *                menghasilkan {@code null}
	 * @return {@link SatuanKerja} dari penugasan aktif pertama yang ditemukan, atau {@code null}
	 *         bila pegawai tidak punya penugasan aktif <b>atau</b> cache kosong
	 * @see Pegawai#getSatuanKerja()
	 */
	public static SatuanKerja ambilSatuanKerja(Pegawai pegawai) {
		if (pegawai == null || pegawai.getId() == null) {
			return null;
		}
		for (Object o : ConstantValues.ambilBerdasarClass(SatuanKerjaPegawai.class).values()) {
			SatuanKerjaPegawai satuanKerjaPegawai = (SatuanKerjaPegawai) o;
			if (satuanKerjaPegawai != null && satuanKerjaPegawai.getPegawai() != null && satuanKerjaPegawai.getAktif()
					&& satuanKerjaPegawai.getPegawai().getId().equals(pegawai.getId())) {
				return satuanKerjaPegawai.getSatuanKerja();
			}
		}
		return null;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Seluruh field dibiarkan pada nilai
	 * bawaannya, kecuali {@link #getTanggal_dirubah()} yang langsung diisi waktu server.
	 */
	public SatuanKerjaPegawai() {
	}

	/**
	 * @return primary key baris penugasan, atau {@code null} bila belum tersimpan. Dibangkitkan
	 *         basis data ({@link javax.persistence.GenerationType#IDENTITY}) dan berurutan,
	 *         sehingga nilainya dapat ditebak oleh pihak luar
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Hanya untuk keperluan Hibernate atau pemuatan ulang baris yang sudah ada;
	 * kode aplikasi tidak boleh mengubahnya.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas atas penugasan (mis. alasan atau nomor surat tugas). Berbeda dengan properti
	 * senama pada {@link GeneralValueObject} yang tidak dipetakan Hibernate, di sini
	 * {@code keterangan} adalah kolom {@code text} sungguhan sehingga isinya benar-benar tersimpan.
	 *
	 * @return keterangan penugasan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan penugasan.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Bendera "penugasan masih berlaku", dengan bawaan <b>berpihak pada aktif</b>.
	 *
	 * <p><b>Non-obvious.</b> Bila kolom bernilai {@code null} method ini mengembalikan {@code true},
	 * tetapi <b>tidak menuliskan balik</b> nilai itu ke field — kolom tetap {@code null} di basis
	 * data. Ini berbeda dari banyak getter bawaan lain di repo ini yang menormalkan nilai sambil
	 * menyimpannya kembali.</p>
	 *
	 * <p>Pada praktiknya kolom ini <b>tidak pernah terisi</b>: layar master
	 * {@code SatuanKerjaPegawaiAction#onSave} hanya menyimpan {@code pegawai}, {@code satuanKerja},
	 * dan {@code keterangan} — {@link #setAktif(Boolean)} tidak pernah dipanggil dari mana pun di
	 * jalur UI. Akibatnya setiap penugasan yang dibuat lewat aplikasi bersifat aktif permanen dan
	 * satu-satunya cara menghentikannya adalah menghapus barisnya. Konsekuensi lanjutannya terasa
	 * di {@link #ambilSatuanKerja(Pegawai)}, yang menyaring dengan bendera ini dan karena itu
	 * praktis tidak pernah menyaring apa pun.</p>
	 *
	 * <p>Perhatikan pula bahwa checkbox "aktif" pada layar pencarian tetap berfungsi, karena
	 * kriterianya memang menerima {@code aktif IS NULL} sebagai "aktif".</p>
	 *
	 * @return {@code true} bila penugasan berlaku; {@code true} juga bila kolom belum diisi
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi bendera penugasan berlaku. Tidak dipanggil dari jalur UI mana pun — lihat catatan
	 * pada {@link #getAktif()}.
	 *
	 * @param aktif {@code true}/{@code false}, atau {@code null} yang berarti tetap dianggap aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Pegawai yang ditugaskan. Relasi wajib ({@code nullable = false}) dan dimuat malas
	 * ({@link FetchType#LAZY}).
	 *
	 * <p><b>Efek samping ringan:</b> nilai field dilewatkan {@code check()} lalu <b>ditulis balik</b>
	 * ke field. Ini bukan perubahan nilai bisnis, melainkan resolusi proxy lazy menjadi instance
	 * kanonik (lihat {@link GeneralValueObject#check(Object)}); tetap perlu diketahui karena
	 * pemanggilannya dapat memicu query ke basis data, termasuk saat method ini dipanggil di dalam
	 * perulangan {@link #ambilSatuanKerja(Pegawai)}.</p>
	 *
	 * @return pegawai yang ditugaskan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Mengisi pegawai yang ditugaskan.
	 *
	 * @param pegawai pegawai tujuan; wajib terisi sebelum baris disimpan karena kolomnya
	 *                {@code NOT NULL}
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Satuan kerja tujuan penugasan. Relasi wajib ({@code nullable = false}) dan dimuat malas
	 * ({@link FetchType#LAZY}).
	 *
	 * <p><b>Efek samping ringan:</b> sama seperti {@link #getPegawai()}, hasil {@code check()}
	 * ditulis balik ke field untuk meresolusi proxy lazy.</p>
	 *
	 * <p>Nilai inilah yang dikembalikan {@link #ambilSatuanKerja(Pegawai)} dan, lewat
	 * {@link Pegawai#getSatuanKerja()}, menentukan unit pemilik bagi data yang dibuat pegawai
	 * bersangkutan di modul RAB, akunting, payroll, surat, dan perpustakaan.</p>
	 *
	 * @return satuan kerja tujuan penugasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja tujuan penugasan.
	 *
	 * @param satuanKerja satuan kerja tujuan; wajib terisi sebelum baris disimpan karena kolomnya
	 *                    {@code NOT NULL}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
