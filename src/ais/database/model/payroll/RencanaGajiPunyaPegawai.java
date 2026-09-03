package ais.database.model.payroll;

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
import org.json.JSONObject;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Satu baris <b>RENCANA</b> (anggaran) gaji untuk satu pegawai di dalam satu dokumen rencana gaji
 * tahunan. Tabel: {@code payroll.rencana_gaji_punya_pegawai}.
 *
 * <p>Kelas ini adalah simpul tengah rantai perencanaan penggajian &mdash; induk langsung dari
 * {@link ais.database.model.payroll.RencanaItemGajiPegawai} dan anak dari {@link RencanaGaji}.
 * Rantai lengkapnya, seluruhnya terverifikasi dari kode (bukan dugaan dari nama):</p>
 *
 * <pre>
 * RencanaGaji                   (dokumen rencana per TAHUN; hanya kolom keterangan + tahun)
 *   +-- RencanaGajiPunyaPegawai       &lt;-- KELAS INI (satu baris per pegawai)
 *         +-- RencanaItemGajiPegawai  (rincian per komponen gaji, per bulan)
 * </pre>
 *
 * <p>Pasangan sisi <i>realisasi</i>-nya adalah {@link PembayaranGajiPunyaPegawai} yang menggantung
 * pada {@link PembayaranGaji}. Kedua kelas itu kembar salin-tempel kelas ini &mdash; urutan field,
 * urutan method, bahkan bentuk {@code toString()}-nya sama persis. <b>Tidak ada FK apa pun</b>
 * antara baris rencana dan baris realisasi; keduanya berdiri sendiri dan tidak saling menunjuk.</p>
 *
 * <h2>Isi baris: {@code nilai1}..{@code nilai12} dan JSON {@code komponenGaji}</h2>
 *
 * <p>Kolom {@code nilai1}..{@code nilai12} adalah <b>total gaji rencana bulan Januari..Desember</b>
 * pada tahun {@code getRencanaGaji().getTahun()} &mdash; bukan dua belas komponen gaji, melainkan
 * dua belas bulan. Bukti: {@code RencanaGajiPunyaPegawaiAction} merender keduabelasnya di bawah
 * label {@code Common.BULAN[0..11]}, dan penulisnya mengisinya di dalam perulangan
 * {@code for (bulan = 1; bulan <= 12; bulan++)}.</p>
 *
 * <p>Satu-satunya penulis kolom-kolom itu adalah
 * {@code RencanaItemGajiPegawaiTreeModel.reset(tanggal, formulasBaru, tahun)}, yang dipanggil dari
 * tiga tempat saja: tombol "Ambil Data Pegawai" dan "Hitung Ulang" pada
 * {@code RencanaGajiPunyaPegawaiAction}, serta jalur "salin rencana tahun lain" di
 * {@code RencanaGajiAction.onSave()}. Mekanismenya per bulan:</p>
 *
 * <ol>
 *   <li>Dicari <b>realisasi terakhir yang sudah disetujui</b> untuk pegawai yang sama:
 *       {@link PembayaranGajiPunyaPegawai} dengan {@code pembayaranGaji.tahun = tahun},
 *       {@code pembayaranGaji.bulan &lt;= bulan}, {@code pembayaranGaji.disetujuiOleh IS NOT NULL},
 *       diurutkan {@code bulan} menurun lalu {@code id} menurun, ambil satu.</li>
 *   <li><b>Bila ketemu</b>: nominalnya dipakai apa adanya sebagai nilai bulan tersebut, dan rincian
 *       komponennya disalin ke {@link #getKomponenGaji()} dengan kunci
 *       {@code <kodeItemGaji>_<bulan>}.</li>
 *   <li><b>Bila tidak ketemu</b>: nominal dihitung dari formula komponen gaji pegawai, dan
 *       barulah baris {@link ais.database.model.payroll.RencanaItemGajiPegawai} dibuat untuk bulan
 *       itu.</li>
 * </ol>
 *
 * <p><b>Konsekuensi yang mudah terlewat</b> &mdash; karena syaratnya {@code bulan <= bulan} (bukan
 * {@code bulan = bulan}), setiap bulan yang belum punya realisasi sendiri akan <b>mewarisi nominal
 * bulan terakhir yang sudah disetujui</b>, bukan jatuh ke perhitungan formula. Bila sebuah tahun
 * sudah punya satu saja slip gaji disetujui di bulan Januari, maka kedua belas kolom di baris ini
 * akan berisi angka Januari yang sama, dan <b>nol</b> baris
 * {@link ais.database.model.payroll.RencanaItemGajiPegawai} dibuat untuk tahun itu. Jalur formula
 * hanya menyala untuk pegawai/tahun yang benar-benar belum pernah digaji.</p>
 *
 * <p>Kolom teks {@link #getKomponenGaji()} menyimpan objek JSON datar dengan dua jenis kunci:</p>
 *
 * <ul>
 *   <li>{@code <kodeItemGaji>_<bulan>} &mdash; nominal satu komponen gaji pada satu bulan; inilah
 *       yang dibaca layar (tab per komponen) dan kedua laporan resmi
 *       ({@code LaporanRekapRencanaGaji} dan {@code LaporanRekapRencanaGajiTahunan}).</li>
 *   <li>{@code RENC_TOT_<kodeItemGaji>} &mdash; total setahun untuk komponen itu. Ini
 *       <b>satu-satunya umpan balik dari rencana ke gaji nyata</b>: formula komponen gaji boleh
 *       menyebut token {@code RENC_TOT_<kode>} dan {@code ItemGajiPegawaiTreeModel} akan
 *       menggantinya dengan angka ini.</li>
 * </ul>
 *
 * <p><b>Kuirk JSON:</b> {@code reset()} memuat JSON lama lebih dulu lalu hanya menimpa kunci yang
 * dihasilkan putaran itu &mdash; kunci lama <b>tidak pernah dibuang</b>. Komponen gaji yang sudah
 * tidak berlaku lagi bagi pegawai tersebut akan meninggalkan {@code RENC_TOT_<kode>} basi yang
 * tetap terbaca oleh mesin formula. Perhatikan pula asimetri sumber kunci: JSON milik realisasi
 * memakai <b>id</b> {@code ItemGaji} sebagai kunci, sedangkan JSON milik kelas ini memakai
 * <b>kode</b>-nya; penerjemahannya lewat {@code ConstantValues.ambil(...)} saat penulisan.</p>
 *
 * <h2>Pemetaan Hibernate dan getter yang menulis balik</h2>
 *
 * <p>Anotasi pemetaan dipasang pada <b>getter</b> ({@link #getId()} membawa {@code @Id}), sehingga
 * Hibernate memakai <i>property access</i>: setiap getter dipanggil Hibernate saat menyusun
 * {@code INSERT}/{@code UPDATE} dan saat dirty-check. Digabung dengan {@code dynamicInsert} dan
 * {@code dynamicUpdate}, itu berarti <b>nilai yang disimpan ke basis data adalah nilai yang
 * dikembalikan getter, bukan isi field</b>. Ada tiga tingkatan perilaku di kelas ini:</p>
 *
 * <ol>
 *   <li><b>Resolusi proxy lazy dengan penugasan balik ke field</b> &mdash;
 *       {@link #getRencanaGaji()} dan {@link #getPegawai()} memanggil
 *       {@link ais.database.model.GeneralValueObject#check(Object)} lalu menugaskan hasilnya
 *       kembali ke field. Nilai datanya tidak berubah, tetapi instance proxy bisa berganti menjadi
 *       instance kanonik dari session lain.</li>
 *   <li><b>Materialisasi diam-diam saat menulis</b> &mdash; {@link #getNilai1()} sampai
 *       {@link #getNilai12()} mengembalikan {@code 0.0} bila field masih {@code null}
 *       <b>tanpa</b> menugaskan apa pun ke field. Field-nya memang tidak berubah, tetapi karena
 *       Hibernate membaca lewat getter, kolom {@code nilai1}..{@code nilai12} yang seharusnya
 *       {@code NULL} tetap tersimpan sebagai {@code 0} di basis data. {@code dynamicInsert} pun
 *       tidak pernah bisa menghilangkan kolom-kolom ini dari {@code INSERT}.</li>
 *   <li><b>Penimpaan TANPA SYARAT pada kolom nominal</b> &mdash; {@link #getNilaiTotal()}
 *       menghitung ulang jumlah keduabelas bulan dan <b>menugaskannya ke field</b> setiap kali
 *       dipanggil. Lihat catatan rinci pada method tersebut.</li>
 * </ol>
 *
 * <p>{@link #toString()} juga bukan operasi murni: ia menugaskan hasil {@link #getRencanaGaji()}
 * dan {@link #getPegawai()} ke field masing-masing, sehingga sekadar mencetak object ini dapat
 * memicu inisialisasi lazy.</p>
 *
 * <h2>Cakupan tenant: tidak ada, dan tidak bisa ada</h2>
 *
 * <p><b>Kelas ini tidak punya satu pun kolom tenant</b> &mdash; bukan penyaring yang fail-open,
 * memang tidak ada kolomnya. Hal yang sama berlaku ke atas: {@link RencanaGaji} hanya punya
 * {@code keterangan} dan {@code tahun}, dan layar induknya menegakkan keunikan tahun secara
 * <b>global</b>, jadi satu dokumen rencana per tahun dipakai bersama oleh seluruh tenant. Satu-
 * satunya jejak tenant yang bisa dicapai adalah lewat {@link Pegawai} &rarr;
 * {@code formatItemGaji.getSatuanKerja()}, dan tidak ada satu pun query di jalur rencana gaji yang
 * memakainya sebagai penyaring.</p>
 *
 * <p>Akibatnya di Generic CRUD v2 (audit {@code task_7b6038ac}) &mdash; verifikasi rinci, bukan
 * generalisasi:</p>
 *
 * <ul>
 *   <li><b>Terjangkau untuk BACA dan EKSPOR.</b> Halaman
 *       {@code WEB-INF/new/payroll/uiux/detail/rencana_gaji_punya_pegawai.jsp} mencantumkan kelas
 *       ini sebagai kandidat entity <b>pertama</b>, dan {@code selectMappedClass()} memilih
 *       kandidat pertama yang terpetakan. Nama kelasnya tidak kena satu pun
 *       {@code BLOCKED_CLASS_TOKENS}.</li>
 *   <li><b>Tanpa pembatas tenant.</b> {@code GenericCrudAutoEntityAdapter.scopeBindings()} hanya
 *       memasang pembatas untuk 12 nama properti tetap ({@code yayasan|sekolah|program|fakultas|
 *       jurusan|satuanKerja|mahasiswa|siswa|dosen|guru|orangTua|anggotaKoperasi}). Kelas ini tidak
 *       punya satu pun di antaranya; relasi {@code pegawai} yang dimilikinya juga <b>tidak</b> ada
 *       di daftar itu, dan {@code addScope()} menelan {@code missingProperty} secara diam-diam
 *       sehingga ketiadaan kolom terbaca sebagai "tidak perlu disaring". Menambahkan
 *       {@code pegawai} ke whitelist pun tidak akan menutup celah di sini, karena
 *       {@code Pegawai} sendiri yang harus disaring, bukan baris ini.</li>
 *   <li><b>Mutasi: TIDAK terjangkau &mdash; verifikasi negatif yang menenangkan.</b>
 *       {@code GenericCrudExistingActionInvoker.supports()} mensyaratkan kelas sumber halaman
 *       punya konstruktor tanpa argumen. {@code RencanaGajiPunyaPegawaiAction} hanya punya
 *       {@code RencanaGajiPunyaPegawaiAction(RencanaGaji)}, tidak punya field {@code MyWindow},
 *       dan tidak punya {@code boolean onSave(Event)}. Definisi jatuh ke {@code READ_ONLY};
 *       create/update/delete lewat New UI dimatikan.</li>
 *   <li>Pada halaman induk {@code WEB-INF/new/payroll/uiux/rencana_gaji.jsp} kelas ini berada di
 *       urutan <b>kedua</b> daftar kandidat, jadi yang terpetakan di sana adalah
 *       {@link RencanaGaji}, bukan kelas ini.</li>
 * </ul>
 *
 * <p>Data yang bocor lewat jalur baca/ekspor itu bukan data netral: nilai1..12 adalah gaji bulanan
 * per pegawai, dan {@link #getKomponenGaji()} memuat rinciannya <b>per komponen</b> (gaji pokok,
 * tunjangan, potongan). Kategori sensitivitasnya sama dengan temuan
 * {@link PembayaranItemGajiPegawai}.</p>
 *
 * <h2>Gerbang hak akses: layar detailnya tidak punya satu pun</h2>
 *
 * <p>Terverifikasi ulang dari sisi entity ini (mengonfirmasi dan memperluas {@code task_11fcffa9}):
 * {@code RencanaGajiPunyaPegawaiAction} &mdash; satu-satunya layar yang menulis baris kelas ini
 * &mdash; <b>tidak mengimpor {@code ais.common.CommonPrivilages} sama sekali</b> dan tidak memuat
 * satu pun pemeriksaan hak. Induknya {@code MyDetail} hanya pembungkus {@code Detail} ZK, jadi
 * tidak ada gerbang yang diwarisi. Rinciannya:</p>
 *
 * <ul>
 *   <li>Layar induk {@code RencanaGajiAction} <b>sudah benar</b>: memeriksa
 *       {@code CommonPrivilages.READ} di {@code doAfterCompose()} dan menurunkan flag
 *       {@code edit}/{@code delete} ke tombol Ubah/Hapus barisnya sendiri. Tetapi flag itu
 *       <b>tidak pernah diteruskan</b> ke komponen detail: renderer barisnya memanggil
 *       {@code new RencanaGajiPunyaPegawaiAction(rencanaGaji)} tanpa argumen hak apa pun, untuk
 *       <b>setiap</b> baris. Gerbangnya ada satu tingkat di atas dan berhenti di situ.</li>
 *   <li>Ketiga tombol destruktif di dalam detail itu &mdash; "Ambil Data Pegawai", "Hitung Ulang",
 *       dan tombol sampah "hapus seluruh data" &mdash; karenanya aktif bagi siapa pun yang cukup
 *       berhak <b>BACA</b>, hanya berjarak satu klik pembuka {@code Detail}.</li>
 *   <li>"Hitung Ulang" bersifat merusak, bukan sekadar menyegarkan: untuk setiap baris ia
 *       menghapus seluruh {@link ais.database.model.payroll.RencanaItemGajiPegawai} milik baris itu
 *       dengan SQL mentah lalu menimpa {@code nilai1}..{@code nilai12} dan
 *       {@link #getKomponenGaji()}. Query pendukungnya ({@code initCriteria}) menyaring hanya
 *       berdasarkan dokumen rencana, {@code pegawai.aktif}, dan keberadaan {@code formatItemGaji}
 *       &mdash; <b>nol penyaring tenant</b>, sehingga satu klik mengolah ulang rencana gaji seluruh
 *       pegawai seluruh tenant.</li>
 *   <li>Tombol sampah tingkat toolbar menjalankan SQL mentah
 *       {@code delete from payroll.rencana_gaji_punya_pegawai where rencana_gaji = ?}. Karena SQL
 *       mentah memintas Hibernate, penghapusan itu <b>tidak menghasilkan revisi Envers</b>
 *       (bandingkan tombol sampah per baris yang lewat {@code Common.refreshDelete} dan tetap
 *       teraudit) dan <b>tidak men-cascade</b> ke tabel anak, sehingga baris
 *       {@code payroll.rencana_item_gaji_pegawai} tertinggal yatim.</li>
 *   <li><b>Ketidakcocokan nama kolom.</b> SQL itu menyebut kolom {@code rencana_gaji}, sedangkan
 *       pemetaan relasi induk di kelas ini adalah {@code @JoinColumn(name = "pembayaran_gaji")}
 *       (lihat {@link #getRencanaGaji()}). Seluruh pembaca lain memakai Criteria/HQL dengan nama
 *       properti, jadi hanya SQL mentah inilah yang menyentuh nama kolom fisik secara langsung.
 *       Keduanya hanya bisa sama-sama benar bila tabel fisik memang membawa dua kolom (sisa
 *       penggantian nama; {@code hbm2ddl.auto=update} menambah kolom baru tetapi tidak pernah
 *       membuang yang lama). Bila kolom {@code rencana_gaji} tidak ada, tombol itu selalu gagal
 *       dan pesannya menyesatkan ("data masih berelasi dengan data lainnya"); bila ada tetapi tidak
 *       pernah diisi, tombol itu diam-diam menghapus nol baris. Perlu dipastikan ke basis data
 *       sebelum ada yang "memperbaiki" salah satu sisi.</li>
 *   <li>Kedua tombol berat menjalankan pekerjaannya di {@code new Thread(...)} mentah, di luar
 *       konteks pengguna ZK. Blok "Ambil Data Pegawai" menutup session Hibernate di
 *       {@code finally}; blok "Hitung Ulang" <b>tidak</b> &mdash; setiap klik menyisakan satu
 *       session/koneksi yang tidak ditutup.</li>
 * </ul>
 *
 * <p><b>Efek berantai yang tidak kasat mata:</b> menurut {@code ItemGajiPegawaiTreeModel.reset()},
 * keberadaan baris rencana milik seorang pegawai berfungsi sebagai <b>kunci pelindung</b> bagi
 * penyesuaian komponen gaji per-pegawai ({@code payroll.item_gaji_pegawai}); bila pegawai itu
 * kehilangan seluruh baris rencananya, penyesuaian per-pegawainya boleh tersapu dan dibangun ulang
 * dari format. Jadi tombol hapus di layar tanpa gerbang ini tidak hanya menghapus anggaran, tetapi
 * juga membuka kunci data gaji yang lain.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ul>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, kait
 *       {@link #onUpdate()}.</li>
 *   <li><b>Relasi:</b> {@link #getRencanaGaji()} (dokumen tahunan induk),
 *       {@link #getPegawai()} (pemilik baris).</li>
 *   <li><b>Nominal per bulan:</b> {@link #getNilai1()}..{@link #getNilai12()} beserta setter-nya,
 *       ditambah {@link #getNilaiTotal()} yang turunan.</li>
 *   <li><b>Rincian komponen:</b> {@link #getKomponenGaji()}/{@link #setKomponenGaji(String)}.</li>
 *   <li><b>Lain-lain:</b> {@link #getKeterangan()} (catatan bebas, di jalur nyata selalu diisi
 *       string kosong), {@link #toString()}.</li>
 * </ul>
 *
 * @see ais.database.model.payroll.RencanaItemGajiPegawai
 * @see RencanaGaji
 * @see PembayaranGajiPunyaPegawai
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "rencana_gaji_punya_pegawai")
public class RencanaGajiPunyaPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi warisan generator hbm2java.
	 *
	 * <p><b>Bukan sidik jari kelas ini.</b> Nilai {@code 2463821577548439808L} dipakai bersama
	 * oleh puluhan entity lain hasil generator yang sama (termasuk {@link RencanaGaji} dan
	 * {@link PembayaranGajiPunyaPegawai}); jangan dijadikan penanda identitas kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, {@code IDENTITY} pada kolom {@code id}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (kolom {@code oleh}). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (kolom {@code olehid}). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> argumen {@code null} atau string kosong <b>diabaikan diam-diam</b>
	 * (nilai lama dipertahankan) &mdash; jejak audit sengaja tidak bisa dikosongkan lewat setter
	 * ini. Konsekuensinya kolom ini tidak pernah bisa "dibersihkan" dari kode aplikasi, dan bila
	 * basis data berisi string kosong maka pemuatan entity akan meninggalkan field tetap
	 * {@code null} sehingga penulisan berikutnya menormalkannya menjadi {@code NULL}.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris rencana gaji, dipakai ZK untuk label ringkas dan pesan konfirmasi.
	 *
	 * <p><b>Efek samping (tidak lazim untuk sebuah {@code toString()}):</b> method ini
	 * <b>menugaskan</b> hasil {@link #getRencanaGaji()} dan {@link #getPegawai()} ke field
	 * masing-masing. Karena kedua getter itu melewatkan proxy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}, sekadar mencetak object ini
	 * dapat memicu inisialisasi lazy dan penggantian instance proxy dengan instance kanonik dari
	 * session lain. Aman secara data (nilai tidak berubah), tetapi berarti {@code toString()}
	 * <b>bukan</b> operasi murni dan tidak boleh dipanggil dari konteks yang sesinya sudah
	 * tertutup.</p>
	 *
	 * <p>Bagian pertama hasilnya adalah {@code RencanaGaji.toString()} yang mengembalikan
	 * {@code keterangan} dokumen &mdash; kolom nullable, sehingga teks {@code "null null"} adalah
	 * keluaran yang sah bila kedua relasi kosong.</p>
	 *
	 * @return gabungan {@code "<dokumen rencana> <pegawai>"}; salah satu bagian bisa berbunyi
	 *         {@code "null"} bila relasinya kosong.
	 */
	public String toString() {
		rencanaGaji = getRencanaGaji();
		pegawai = getPegawai();
		return rencanaGaji + " " + pegawai;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * argumen {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit tepat sebelum {@code UPDATE}
	 * dikirim ke basis data.
	 *
	 * <p>Dipanggil oleh provider persistence, <b>tidak pernah</b> dari kode aplikasi. Karena
	 * {@link #getNilaiTotal()} menulis balik ke field setiap kali dibaca, kait ini juga ikut
	 * menyala pada "perubahan" yang sebenarnya hanya lahir dari pembacaan &mdash;
	 * {@code tanggal_dirubah} karenanya bisa bergerak tanpa ada operator yang mengubah apa pun.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Dokumen rencana gaji tahunan pemilik baris ini.
	 *
	 * <p><b>Perhatian:</b> kolom fisiknya bernama {@code pembayaran_gaji}, bukan
	 * {@code rencana_gaji} &mdash; sisa salin-tempel dari {@link PembayaranGajiPunyaPegawai};
	 * lihat {@link #getRencanaGaji()}.</p>
	 */
	private RencanaGaji rencanaGaji;
	/** Pegawai pemilik baris rencana ini (FK {@code pegawai}, nullable). */
	private Pegawai pegawai;
	/**
	 * Catatan bebas per pegawai. Satu-satunya penulis di repo
	 * ({@code RencanaGajiPunyaPegawaiAction} tombol "Ambil Data Pegawai") selalu mengisinya dengan
	 * string kosong, dan tidak ada layar maupun laporan yang menampilkannya kembali.
	 */
	private String keterangan;

	/** Total gaji rencana bulan Januari (kolom {@code nilai1}). */
	private Double nilai1;
	/** Total gaji rencana bulan Februari (kolom {@code nilai2}). */
	private Double nilai2;
	/** Total gaji rencana bulan Maret (kolom {@code nilai3}). */
	private Double nilai3;
	/** Total gaji rencana bulan April (kolom {@code nilai4}). */
	private Double nilai4;
	/** Total gaji rencana bulan Mei (kolom {@code nilai5}). */
	private Double nilai5;
	/** Total gaji rencana bulan Juni (kolom {@code nilai6}). */
	private Double nilai6;
	/** Total gaji rencana bulan Juli (kolom {@code nilai7}). */
	private Double nilai7;
	/** Total gaji rencana bulan Agustus (kolom {@code nilai8}). */
	private Double nilai8;
	/** Total gaji rencana bulan September (kolom {@code nilai9}). */
	private Double nilai9;
	/** Total gaji rencana bulan Oktober (kolom {@code nilai10}). */
	private Double nilai10;
	/** Total gaji rencana bulan November (kolom {@code nilai11}). */
	private Double nilai11;
	/** Total gaji rencana bulan Desember (kolom {@code nilai12}). */
	private Double nilai12;
	/**
	 * Total setahun; <b>kolom turunan</b> yang dihitung ulang dan ditimpa setiap kali
	 * {@link #getNilaiTotal()} dipanggil. Nilai yang disetel lewat
	 * {@link #setNilaiTotal(Double)} tidak pernah bertahan.
	 */
	private Double nilaiTotal;

	/**
	 * Rincian nominal per komponen gaji per bulan dalam bentuk teks JSON; lihat
	 * {@link #getKomponenGaji()}.
	 */
	private String komponenGaji;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Baris baru dibuat hanya di satu tempat, yaitu tombol "Ambil Data Pegawai" pada
	 * {@code RencanaGajiPunyaPegawaiAction}, yang langsung mengisi {@link #setPegawai(Pegawai)},
	 * {@link #setRencanaGaji(RencanaGaji)}, dan {@link #setKeterangan(String)} sebelum menyimpan.
	 * Nominal bulanannya baru terisi pada pemanggilan
	 * {@code RencanaItemGajiPegawaiTreeModel.reset(...)} berikutnya.</p>
	 */
	public RencanaGajiPunyaPegawai() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} ditandai {@code insertable = false}: nilainya sepenuhnya dihasilkan
	 * basis data (strategi {@code IDENTITY}), sehingga {@code null} sebelum baris tersimpan.</p>
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Dipakai Hibernate saat memuat baris. Dari kode aplikasi, penyetelan manual hanya masuk
	 * akal pada pola "klon lalu {@code setId(null)}" yang dipakai jalur salin rencana.</p>
	 *
	 * @param id kunci utama; {@code null} berarti baris belum tersimpan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan dokumen rencana gaji tahunan pemilik baris ini.
	 *
	 * <p><b>Nama kolomnya menyesatkan.</b> FK ini dipetakan ke kolom fisik
	 * {@code pembayaran_gaji} &mdash; sisa salin-tempel dari
	 * {@link PembayaranGajiPunyaPegawai#getPembayaranGaji()} yang tidak ikut diganti namanya saat
	 * kelas ini diturunkan. Seluruh pembaca di repo memakai Criteria/HQL dengan nama <i>properti</i>
	 * {@code rencanaGaji} sehingga tidak terpengaruh; satu-satunya tempat yang menyebut nama kolom
	 * fisik secara langsung adalah SQL mentah tombol "hapus seluruh data" di
	 * {@code RencanaGajiPunyaPegawaiAction}, dan SQL itu menulis {@code rencana_gaji} &mdash;
	 * ketidakcocokan yang dibahas pada Javadoc kelas.</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(Object)} <b>ditugaskan kembali</b> ke field, jadi
	 * getter ini bisa mengganti instance proxy dengan instance kanonik. Relasi bersifat
	 * {@code LAZY} dengan cascade {@code PERSIST}+{@code MERGE}: menyimpan baris ini ikut
	 * mem-persist/merge dokumen induknya, tetapi <b>tidak</b> ikut menghapusnya.</p>
	 *
	 * @return dokumen rencana tahunan, atau {@code null} bila FK kosong.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembayaran_gaji", nullable = true)
	public RencanaGaji getRencanaGaji() {
		rencanaGaji = check(rencanaGaji);
		return rencanaGaji;
	}

	/**
	 * Menyetel dokumen rencana gaji tahunan pemilik baris ini.
	 *
	 * @param rencanaGaji dokumen induk; boleh {@code null}.
	 */
	public void setRencanaGaji(RencanaGaji rencanaGaji) {
		this.rencanaGaji = rencanaGaji;
	}

	/**
	 * Mengembalikan pegawai pemilik baris rencana ini.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getRencanaGaji()}, hasil {@code check(Object)}
	 * ditugaskan kembali ke field. Relasi {@code LAZY} dengan cascade
	 * {@code PERSIST}+{@code MERGE}.</p>
	 *
	 * <p>Relasi inilah satu-satunya jalan mencapai identitas tenant baris ini (lewat
	 * {@code pegawai.getFormatItemGaji().getSatuanKerja()}), dan tidak ada satu pun query di jalur
	 * rencana gaji yang memakainya sebagai penyaring &mdash; lihat bagian cakupan tenant pada
	 * Javadoc kelas.</p>
	 *
	 * @return pegawai pemilik baris, atau {@code null} bila FK kosong.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel pegawai pemilik baris rencana ini.
	 *
	 * @param pegawai pegawai pemilik; boleh {@code null}.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan catatan bebas baris ini.
	 *
	 * @return catatan, pada data nyata praktis selalu string kosong.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas baris ini.
	 *
	 * @param keterangan catatan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Total gaji rencana bulan <b>Januari</b>.
	 *
	 * <p>Mengembalikan {@code 0.0} bila field masih {@code null}, tanpa menugaskan apa pun ke
	 * field. Karena Hibernate memakai property access, nilai yang dikembalikan getter inilah yang
	 * ditulis ke kolom, sehingga {@code NULL} akan tersimpan sebagai {@code 0} pada penulisan
	 * berikutnya.</p>
	 *
	 * @return nominal bulan Januari; tidak pernah {@code null}.
	 */
	public Double getNilai1() {
		return nilai1 == null ? 0.0 : nilai1;
	}

	/**
	 * Menyetel total gaji rencana bulan Januari.
	 *
	 * @param nilai1 nominal bulan Januari; boleh {@code null}.
	 */
	public void setNilai1(Double nilai1) {
		this.nilai1 = nilai1;
	}

	/**
	 * Total gaji rencana bulan <b>Februari</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Februari; tidak pernah {@code null}.
	 */
	public Double getNilai2() {
		return nilai2 == null ? 0.0 : nilai2;
	}

	/**
	 * Menyetel total gaji rencana bulan Februari.
	 *
	 * @param nilai2 nominal bulan Februari; boleh {@code null}.
	 */
	public void setNilai2(Double nilai2) {
		this.nilai2 = nilai2;
	}

	/**
	 * Total gaji rencana bulan <b>Maret</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Maret; tidak pernah {@code null}.
	 */
	public Double getNilai3() {
		return nilai3 == null ? 0.0 : nilai3;
	}

	/**
	 * Menyetel total gaji rencana bulan Maret.
	 *
	 * @param nilai3 nominal bulan Maret; boleh {@code null}.
	 */
	public void setNilai3(Double nilai3) {
		this.nilai3 = nilai3;
	}

	/**
	 * Total gaji rencana bulan <b>April</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan April; tidak pernah {@code null}.
	 */
	public Double getNilai4() {
		return nilai4 == null ? 0.0 : nilai4;
	}

	/**
	 * Menyetel total gaji rencana bulan April.
	 *
	 * @param nilai4 nominal bulan April; boleh {@code null}.
	 */
	public void setNilai4(Double nilai4) {
		this.nilai4 = nilai4;
	}

	/**
	 * Total gaji rencana bulan <b>Mei</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Mei; tidak pernah {@code null}.
	 */
	public Double getNilai5() {
		return nilai5 == null ? 0.0 : nilai5;
	}

	/**
	 * Menyetel total gaji rencana bulan Mei.
	 *
	 * @param nilai5 nominal bulan Mei; boleh {@code null}.
	 */
	public void setNilai5(Double nilai5) {
		this.nilai5 = nilai5;
	}

	/**
	 * Total gaji rencana bulan <b>Juni</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Juni; tidak pernah {@code null}.
	 */
	public Double getNilai6() {
		return nilai6 == null ? 0.0 : nilai6;
	}

	/**
	 * Menyetel total gaji rencana bulan Juni.
	 *
	 * @param nilai6 nominal bulan Juni; boleh {@code null}.
	 */
	public void setNilai6(Double nilai6) {
		this.nilai6 = nilai6;
	}

	/**
	 * Total gaji rencana bulan <b>Juli</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Juli; tidak pernah {@code null}.
	 */
	public Double getNilai7() {
		return nilai7 == null ? 0.0 : nilai7;
	}

	/**
	 * Menyetel total gaji rencana bulan Juli.
	 *
	 * @param nilai7 nominal bulan Juli; boleh {@code null}.
	 */
	public void setNilai7(Double nilai7) {
		this.nilai7 = nilai7;
	}

	/**
	 * Total gaji rencana bulan <b>Agustus</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Agustus; tidak pernah {@code null}.
	 */
	public Double getNilai8() {
		return nilai8 == null ? 0.0 : nilai8;
	}

	/**
	 * Menyetel total gaji rencana bulan Agustus.
	 *
	 * @param nilai8 nominal bulan Agustus; boleh {@code null}.
	 */
	public void setNilai8(Double nilai8) {
		this.nilai8 = nilai8;
	}

	/**
	 * Total gaji rencana bulan <b>September</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan September; tidak pernah {@code null}.
	 */
	public Double getNilai9() {
		return nilai9 == null ? 0.0 : nilai9;
	}

	/**
	 * Menyetel total gaji rencana bulan September.
	 *
	 * @param nilai9 nominal bulan September; boleh {@code null}.
	 */
	public void setNilai9(Double nilai9) {
		this.nilai9 = nilai9;
	}

	/**
	 * Total gaji rencana bulan <b>Oktober</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Oktober; tidak pernah {@code null}.
	 */
	public Double getNilai10() {
		return nilai10 == null ? 0.0 : nilai10;
	}

	/**
	 * Menyetel total gaji rencana bulan Oktober.
	 *
	 * @param nilai10 nominal bulan Oktober; boleh {@code null}.
	 */
	public void setNilai10(Double nilai10) {
		this.nilai10 = nilai10;
	}

	/**
	 * Total gaji rencana bulan <b>November</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan November; tidak pernah {@code null}.
	 */
	public Double getNilai11() {
		return nilai11 == null ? 0.0 : nilai11;
	}

	/**
	 * Menyetel total gaji rencana bulan November.
	 *
	 * @param nilai11 nominal bulan November; boleh {@code null}.
	 */
	public void setNilai11(Double nilai11) {
		this.nilai11 = nilai11;
	}

	/**
	 * Total gaji rencana bulan <b>Desember</b>; perilaku {@code null} sama dengan
	 * {@link #getNilai1()}.
	 *
	 * @return nominal bulan Desember; tidak pernah {@code null}.
	 */
	public Double getNilai12() {
		return nilai12 == null ? 0.0 : nilai12;
	}

	/**
	 * Menyetel total gaji rencana bulan Desember.
	 *
	 * @param nilai12 nominal bulan Desember; boleh {@code null}.
	 */
	public void setNilai12(Double nilai12) {
		this.nilai12 = nilai12;
	}

	/**
	 * Total rencana gaji setahun: jumlah {@link #getNilai1()} sampai {@link #getNilai12()}.
	 *
	 * <p><b>Getter yang menulis balik dan menyentuh kolom nominal.</b> Method ini menghitung
	 * ulang jumlahnya lalu <b>menugaskannya ke field {@code nilaiTotal} tanpa syarat</b> &mdash;
	 * tidak ada penjaga {@code null}, nilai tersimpan selalu ditimpa. Karena kelas ini dipetakan
	 * <i>property access</i> dengan {@code dynamicUpdate = true}, penugasan itu ikut tersimpan
	 * permanen ke basis data: Hibernate memanggil getter ini saat dirty-check/flush, dan bila
	 * kolom tersimpan berbeda dari hasil hitungan maka sebuah {@code UPDATE} benar-benar dikirim
	 * &mdash; lengkap dengan revisi Envers baru dan pergerakan {@code tanggal_dirubah} lewat
	 * {@link #onUpdate()}. <b>Membaca baris ini bisa mengubah baris ini.</b></p>
	 *
	 * <p>Konsekuensi turunan: {@link #setNilaiTotal(Double)} praktis tidak berguna, karena nilai
	 * apa pun yang disetel akan tertimpa pada pembacaan berikutnya. Kolom ini juga tidak pernah
	 * bisa menyimpan total yang berbeda dari jumlah keduabelas bulan, misalnya untuk mencatat
	 * pembulatan atau penyesuaian manual.</p>
	 *
	 * <p><b>Kuirk:</b> penyisiran menyeluruh repo menemukan <b>nol pemanggil aplikasi</b> untuk
	 * {@link #getNilaiTotal()} maupun {@link #setNilaiTotal(Double)} &mdash; layar dan kedua
	 * laporan menjumlahkan sendiri {@code nilai1}..{@code nilai12} di tempat, bukan membaca kolom
	 * ini. Satu-satunya pemanggil yang tersisa adalah Hibernate sendiri, jadi kolom
	 * {@code nilaitotal} hidup semata-mata sebagai efek samping mekanisme persistence.</p>
	 *
	 * @return jumlah nominal keduabelas bulan; tidak pernah {@code null} (nilai bulan yang
	 *         {@code null} dihitung {@code 0.0}).
	 */
	public Double getNilaiTotal() {
		nilaiTotal = (getNilai1() + getNilai2() + getNilai3() + getNilai4() + getNilai5() + getNilai6() + getNilai7()
				+ getNilai8() + getNilai9() + getNilai10() + getNilai11() + getNilai12());
		return nilaiTotal;
	}

	/**
	 * Menyetel total setahun.
	 *
	 * <p><b>Efektif no-op.</b> Nilai yang disetel akan ditimpa hasil hitungan pada pemanggilan
	 * {@link #getNilaiTotal()} berikutnya &mdash; termasuk pemanggilan yang dilakukan Hibernate
	 * sendiri sebelum menulis ke basis data. Method ini ada hanya agar kontrak JavaBean lengkap
	 * dan agar Hibernate bisa mengisi field saat memuat baris.</p>
	 *
	 * @param nilaiTotal total setahun; tidak bertahan.
	 */
	public void setNilaiTotal(Double nilaiTotal) {
		this.nilaiTotal = nilaiTotal;
	}

	/**
	 * Objek JSON kosong yang dipakai sebagai nilai bawaan {@link #getKomponenGaji()}.
	 *
	 * <p>Isinya tidak pernah diubah; keberadaannya hanya untuk menghasilkan literal {@code "{}"}.
	 * Perlu diketahui bahwa field ini <b>statik dan tidak {@code final}</b>, sehingga secara teknis
	 * dapat dimutasi atau diganti dari mana pun dan akan mengubah nilai bawaan bagi <b>seluruh</b>
	 * baris di seluruh aplikasi; {@code org.json.JSONObject} juga tidak <i>thread-safe</i>. Import
	 * {@code org.json.JSONObject} pada kelas ini semata-mata untuk keperluan ini.</p>
	 */
	private static JSONObject D = new JSONObject();

	/**
	 * Rincian nominal per komponen gaji per bulan, dalam bentuk teks JSON datar.
	 *
	 * <p>Bentuk kuncinya (lihat pembahasan lengkap pada Javadoc kelas):</p>
	 * <ul>
	 *   <li>{@code <kodeItemGaji>_<bulan>} &mdash; nominal satu komponen pada satu bulan, dibaca
	 *       layar (tab per komponen gaji) dan kedua laporan rekap rencana gaji;</li>
	 *   <li>{@code RENC_TOT_<kodeItemGaji>} &mdash; total setahun komponen itu, satu-satunya jalur
	 *       umpan balik rencana &rarr; mesin formula gaji nyata.</li>
	 * </ul>
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}.</b> Bila kolom kosong atau hanya berisi
	 * spasi, yang dikembalikan adalah {@code "{}"} dari {@link #D}, sehingga seluruh pemanggil
	 * dapat langsung mem-parsingnya tanpa penjaga {@code null}. Berbeda dari
	 * {@link #getNilaiTotal()}, getter ini <b>tidak</b> menugaskan apa pun ke field &mdash; tetapi
	 * karena Hibernate memakai property access, nilai {@code "{}"} inilah yang tersimpan ke kolom
	 * {@code komponengaji} pada penulisan berikutnya; baris baru karenanya tidak pernah menyimpan
	 * {@code NULL} di kolom ini.</p>
	 *
	 * <p><b>Kasus tepi:</b> {@code reset()} memuat isi lama lebih dulu dan hanya menimpa kunci
	 * yang dihasilkan putaran itu, sehingga kunci komponen yang sudah tidak berlaku lagi &mdash;
	 * termasuk {@code RENC_TOT_<kode>}-nya &mdash; <b>bertahan basi</b> dan tetap ikut terbaca
	 * mesin formula. Kolom ini juga bertipe {@code text} tanpa validasi skema apa pun: isinya
	 * hanya sah sejauh penulisnya konsisten.</p>
	 *
	 * @return teks JSON rincian komponen; tidak pernah {@code null}, minimal {@code "{}"}.
	 */
	@Column(columnDefinition = "text")
	public String getKomponenGaji() {
		return komponenGaji == null || komponenGaji.trim().isEmpty() ? D.toString() : komponenGaji;
	}

	/**
	 * Menyetel teks JSON rincian komponen gaji.
	 *
	 * <p>Satu-satunya penulis nyata adalah
	 * {@code RencanaItemGajiPegawaiTreeModel.reset(...)}, yang selalu menyerahkan hasil
	 * {@code JSONObject.toString()} utuh &mdash; tidak ada jalur yang menyunting satu kunci saja
	 * di tingkat basis data. Tidak ada validasi bentuk JSON di sini; string apa pun akan diterima
	 * dan baru meledak saat pembacaan.</p>
	 *
	 * @param komponenGaji teks JSON; {@code null}/kosong akan dibaca kembali sebagai {@code "{}"}.
	 */
	public void setKomponenGaji(String komponenGaji) {
		this.komponenGaji = komponenGaji;
	}
}
