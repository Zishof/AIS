package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

/**
 * Satu baris <b>"item biaya apa yang dipotong, dan berapa besar potongannya"</b> di dalam
 * sebuah aturan diskon siswa — entity penghubung (join entity dengan atribut) antara
 * {@link DiskonSiswa} dan {@link ItemBiayaSekolah}.
 *
 * <p>Tabel: {@code sekolah.diskon_siswa_item_biaya}. Satu baris menjawab pertanyaan
 * <i>"untuk aturan diskon ini, item biaya X ikut didiskon, dan nilai diskonnya berapa?"</i>
 * Satu-satunya atribut nilainya adalah {@link #getDefaultBiaya()}, yang — sesuai saklar
 * {@code DiskonSiswa.getMenggunkanPersen()} — dibaca sebagai <b>persen</b> atau sebagai
 * <b>nominal rupiah</b>.</p>
 *
 * <h2>Koreksi penting atas dugaan awal: sisi kanan relasi adalah ItemBiayaSekolah</h2>
 *
 * <p>Entity ini <b>TIDAK</b> menunjuk {@code PengaturanBiayaItemBiaya} (baris tarif di dalam
 * sebuah {@code PengaturanBiaya}), melainkan langsung menunjuk {@link ItemBiayaSekolah} —
 * katalog item biaya milik satu sekolah. Artinya sebuah aturan diskon berlaku pada
 * <i>jenis</i> biaya (mis. "SPP", "Uang Gedung"), bukan pada satu baris tarif tertentu.
 * Keterkaitan dengan {@code PengaturanBiayaItemBiaya} bersifat <b>tidak langsung</b>: kedua
 * entity sama-sama bermuara pada {@link ItemBiayaSekolah} yang sama, lalu bertemu kembali di
 * baris {@link Tagihan} yang dihasilkan mesin billing.</p>
 *
 * <h2>Status: HIDUP — menyentuh langsung nominal tagihan siswa (terverifikasi)</h2>
 *
 * <p>Entity ini bukan peninggalan yatim. Rantai lengkap yang terverifikasi dari kode:</p>
 *
 * <pre>
 * DiskonSiswa (aturan diskon: nama, jenis, tahun ajaran, masa berlaku, persen/nominal)
 *   |
 *   +-- DiskonSiswaItemBiaya (KELAS INI) --&gt; ItemBiayaSekolah   "apa yang didiskon + berapa"
 *   |
 *   +-- DiskonSiswaPunyaSiswa --&gt; Siswa / CalonSiswa            "siapa yang menerima"
 *                    |
 *                    v
 *   DiskonSiswaSyncHelper.sinkronkan(...)
 *                    |
 *                    v
 *   Tagihan.diskon / Tagihan.diskonTidakLangsung / Tagihan.diskonSiswa
 *                    |
 *                    v
 *   DaftarPengajuanTransfer.simpanDiskonPembayaran(...)  (bila diskon TIDAK memotong tagihan)
 * </pre>
 *
 * <p>Pembaca/penulis nyata yang terverifikasi:</p>
 * <ul>
 *   <li><b>{@code DiskonSiswaAction}</b> (layar master "Konfigurasi Diskon",
 *       {@code /pages/master/sekolah/diskon_siswa.zul}) — satu-satunya penulis. Dialog
 *       tambah/ubah merender satu baris per {@link ItemBiayaSekolah} aktif milik sekolah
 *       terpilih: sebuah {@code Checkbox} (item ikut didiskon?) berpasangan dengan sebuah
 *       {@code MyDoublebox} (nilai diskon &rarr; {@link #setDefaultBiaya(Double)}). Lihat
 *       catatan "Siklus tulis hapus-total lalu sisip-ulang" di bawah.</li>
 *   <li><b>{@code DiskonSiswaSyncHelper}</b> — mesin sinkronisasi. {@code loadItemBiaya()}
 *       mengambil seluruh baris milik satu {@link DiskonSiswa} (hanya yang item biayanya
 *       masih aktif), lalu {@code sinkronkanPenerima()} memakai {@link #getDefaultBiaya()}
 *       untuk menghitung dan <b>menulis</b> potongan ke baris {@link Tagihan} setiap penerima
 *       diskon yang sudah disetujui.</li>
 *   <li><b>{@code TagihanDiskonSiswaHelper}</b> — menghitung total diskon yang berlaku atas
 *       satu item biaya untuk satu siswa memakai agregat
 *       {@code Projections.sum("defaultBiaya")} pada seluruh baris kelas ini yang cocok.</li>
 *   <li><b>{@code DiskonSiswaAction.DiskonSiswaRenderer}</b> — menampilkan daftar item yang
 *       didiskon beserta nilainya pada panel rincian grid (dengan sufiks {@code %} bila
 *       aturan induk memakai persen).</li>
 *   <li><b>{@code DiskonSiswaAction.refreshDashboard()}</b> — kartu metrik "Item biaya
 *       terkait" pada dasbor layar (lihat catatan kebocoran lintas tenant di bawah).</li>
 *   <li><b>{@code hibernate.cfg.xml}</b> — kelas terdaftar sebagai mapping resmi, jadi tabel
 *       ini benar-benar dibuat/dikelola Hibernate pada instalasi baru.</li>
 * </ul>
 *
 * <p>Catatan: {@code TagihanUtil}, {@code TagihanUtilCalonSiswa}, dan
 * {@code DiskonSiswaPunyaSiswaHelper} meng-<i>import</i> kelas ini tetapi tidak pernah
 * memakainya (import mati, sisa refaktor) — jangan tertipu menyangka mesin pembangkit tagihan
 * membaca tabel ini secara langsung; jalur satu-satunya adalah lewat
 * {@code DiskonSiswaSyncHelper}/{@code TagihanDiskonSiswaHelper}.</p>
 *
 * <h2>Semantik {@code defaultBiaya}: persen ATAU rupiah — ditentukan entity induk</h2>
 *
 * <p>Nama properti {@code defaultBiaya} adalah warisan salin-tempel dari
 * {@link PengaturanBiayaItemBiaya#getDefaultBiaya()} dan <b>menyesatkan</b>: di sini angka
 * tersebut bukan "biaya", melainkan <b>besaran potongan</b>. Penafsirannya ditentukan
 * {@code DiskonSiswa.getMenggunkanPersen()} (default {@code true}):</p>
 * <ul>
 *   <li><b>persen = true</b> &rarr; potongan = {@code nominalTagihan * defaultBiaya / 100};</li>
 *   <li><b>persen = false</b> &rarr; potongan = {@code defaultBiaya} apa adanya (rupiah).</li>
 * </ul>
 * <p>Saklar itu berada di entity induk, sehingga <b>mengubah checkbox "Menggunakan
 * penghitungan persen" pada satu aturan diskon akan mengubah arti SELURUH angka di baris-baris
 * ini sekaligus</b>: nilai 10 yang tadinya berarti "diskon 10%" mendadak berarti "diskon
 * Rp10". Tidak ada validasi maupun konversi yang mencegahnya.</p>
 *
 * <h2>Efek pada tagihan (verifikasi dari {@code DiskonSiswaSyncHelper})</h2>
 * <ul>
 *   <li>Baris {@link Tagihan} yang disasar dipilih dengan kunci
 *       (tahun ajaran aturan diskon + {@link ItemBiayaSekolah} baris ini + siswa/calon siswa
 *       penerima + tagihan aktif). Tagihan yang bukan tagihan sesungguhnya
 *       ({@code ambilBukanTagihanData()} / {@code NominalBiaya.getBukanTagihan()}) dilewati.</li>
 *   <li>Yang ditulis: {@code Tagihan.setDiskon(...)}, {@code Tagihan.setDiskonTidakLangsung(...)},
 *       dan {@code Tagihan.setDiskonSiswa(aturanDiskon)}.</li>
 *   <li><b>Diskon TIDAK bersifat kumulatif.</b> Ketiga setter di atas menimpa nilai lama, dan
 *       {@code Tagihan} hanya menyimpan satu FK {@code diskonSiswa}. Bila dua aturan diskon
 *       menyasar item biaya + siswa yang sama, yang tersimpan adalah aturan yang
 *       <i>terakhir</i> disinkronkan — bukan jumlah keduanya.</li>
 *   <li><b>Mencentang item tanpa mengisi nilai tetap berdampak.</b> {@link #getDefaultBiaya()}
 *       mengembalikan {@code 0.0} untuk nilai kosong, sehingga baris seperti itu tetap membuat
 *       {@code Tagihan} distempel {@code diskonSiswa = aturan ini} dengan potongan {@code 0} —
 *       dan dengan demikian menendang keluar diskon lain yang tadinya menempel di tagihan itu.</li>
 * </ul>
 *
 * <h2>Kuirk &amp; bom waktu yang ditemukan (bukan bagian file ini, tetapi mempengaruhinya)</h2>
 * <ul>
 *   <li><b>Siklus tulis hapus-total lalu sisip-ulang.</b> {@code DiskonSiswaAction.onSave()}
 *       menjalankan native SQL {@code delete from sekolah.diskon_siswa_item_biaya where
 *       diskon_siswa=&lt;id&gt;} lalu menyisipkan ulang hanya baris yang tercentang. Karena
 *       DELETE-nya native SQL, <b>Envers ({@link Audited}) tidak mencatat penghapusan itu</b>:
 *       jejak audit perubahan diskon (data finansial) berlubang pada setiap penyimpanan.
 *       Pola identik pernah dicatat pada {@code PaketPsbPunyaGelombangPendaftaranPsb}.</li>
 *   <li><b>Ketergantungan pada cache checkbox lintas halaman.</b> Karena DELETE-nya menyeluruh,
 *       daftar item yang disisipkan ulang harus mencakup seluruh halaman daftar item biaya,
 *       bukan hanya halaman yang sedang tampil. {@code DiskonSiswaAction} sudah menanganinya
 *       lewat {@code checkboxItemBiayaSekolahPerId} — jangan menyederhanakan mekanisme itu,
 *       karena kesalahan di sana menghapus permanen diskon di halaman lain.</li>
 *   <li><b>Dua sumber kebenaran.</b> Kolom teks {@code DiskonSiswa.itemBiaya} menyimpan daftar
 *       id {@link ItemBiayaSekolah} yang sama dalam bentuk CSV. {@code DiskonSiswaAction.onSave()}
 *       dan {@code DiskonSiswaSyncHelper.updateItemBiayaCache()} sama-sama menyusunnya ulang
 *       dari tabel ini, tetapi bila baris tabel ini dimodifikasi lewat jalur lain, CSV itu
 *       menjadi basi sampai sinkronisasi berikutnya dijalankan.</li>
 *   <li><b>Tidak ada unique constraint</b> pada pasangan (diskon_siswa, item_biaya_sekolah_id).
 *       {@code TagihanDiskonSiswaHelper} memakai {@code sum(defaultBiaya)}, sehingga baris
 *       duplikat akan <i>menjumlahkan</i> potongan secara diam-diam.</li>
 *   <li><b>Deteksi perubahan dibulatkan ke bilangan bulat.</b>
 *       {@code DiskonSiswaSyncHelper.perluUpdate()} membandingkan
 *       {@code lama.intValue() != baru.intValue()}: perubahan nilai diskon yang bagian
 *       bulatnya sama (mis. 10,4% &rarr; 10,9% pada tagihan kecil) tidak pernah tersinkron.</li>
 *   <li><b>Penjaga null yang tak pernah aktif.</b> {@code DiskonSiswaSyncHelper} masih menulis
 *       {@code item.getDefaultBiaya() == null ? 0.0 : ...} padahal {@link #getDefaultBiaya()}
 *       sudah tidak pernah mengembalikan {@code null} — kode mati yang tidak berbahaya.</li>
 *   <li><b>{@code InitIndex.initFkCascadeDiskonSiswaItemBiaya()} kini stub kosong</b>: FK
 *       {@code ON DELETE CASCADE} tidak lagi dipasang otomatis, sehingga menghapus satu
 *       {@link DiskonSiswa} dapat meninggalkan baris yatim di tabel ini pada instalasi lama.</li>
 * </ul>
 *
 * <h2>Cakupan tenant (yayasan/sekolah)</h2>
 *
 * <p>Entity ini <b>tidak</b> punya properti {@code sekolah}/{@code yayasan} sendiri. Cakupan
 * tenantnya bersifat turunan ganda: lewat {@code DiskonSiswa.getSekolah()} di satu sisi dan
 * {@code ItemBiayaSekolah.getSekolah()} di sisi lain. Tidak ada apa pun di tingkat entity
 * maupun basis data yang memaksa kedua sekolah itu sama — konsistensi sepenuhnya bergantung
 * pada layar penulis, yang membangun daftar pilihan item biaya dari combo "Sekolah" yang
 * sedang terpilih. Setiap penulis baru (impor massal, API, skrip perbaikan data) wajib
 * menegakkan sendiri kesamaan sekolah tersebut.</p>
 *
 * <h2>Pola arsitektur berulang — hasil verifikasi</h2>
 * <ul>
 *   <li><b>Getter write-back {@code check(...)}</b>: ADA pada {@link #getDiskonSiswa()} dan
 *       {@link #getItemBiayaSekolah()}, dan merupakan bentuk yang <i>normal/aman</i> —
 *       resolusi proxy lazy baku yang dijelaskan pada
 *       {@link ais.database.model.GeneralValueObject}, bukan mutasi data bisnis.</li>
 *   <li><b>Getter destruktif</b>: <b>TIDAK ADA</b>. {@link #getDefaultBiaya()} hanya melakukan
 *       <i>null-coalescing</i> ({@code null} &rarr; {@code 0.0}) tanpa menulis balik ke field,
 *       sehingga membaca entity ini tidak pernah menghasilkan {@code UPDATE} tak terduga.</li>
 *   <li><b>Penciutan {@code TreeSet}</b> (pola batch 55): <b>TIDAK BERLAKU</b> di sini. Kelas
 *       ini tidak meng-override {@code getNomorUrut()} (jadi tetap {@code null} sesuai
 *       kontrak induk), dan — hasil penelusuran seluruh repo — <b>tidak ada satu pun
 *       {@code Set}/{@code TreeSet}/koleksi terpetakan</b> yang menampung entity ini. Semua
 *       pembacanya memakai {@code List} dari {@code Criteria}, sehingga baris ber-nilai sama
 *       tidak pernah saling menelan.</li>
 *   <li><b>{@code toString()}</b>: tidak di-override; memakai versi induk yang menyusun
 *       {@code id + "-" + nama}. Karena properti {@code nama} milik induk tidak dipetakan pada
 *       entity ini, hasilnya selalu berakhir {@code "-null"} — hanya berdampak pada log/debug.</li>
 * </ul>
 *
 * <h2>Catatan warisan {@link GeneralValueObject}</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun propertinya. Karena itu
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b>
 * dideklarasikan ulang di kelas ini; itu keharusan teknis, bukan duplikasi yang keliru.
 * Sebaliknya, properti induk yang <i>tidak</i> dideklarasikan ulang (mis. {@code nama},
 * {@code keterangan}, {@code nomorUrut}, {@code aktif}) sama sekali tidak tersimpan ke basis
 * data dan hilang setiap kali instance dimuat ulang.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; serialisasi:</b> {@link #serialVersionUID}, {@link #getId()},
 *       {@link #setId(Long)}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}.</li>
 *   <li><b>Relasi:</b> {@link #getDiskonSiswa()}, {@link #setDiskonSiswa(DiskonSiswa)},
 *       {@link #getItemBiayaSekolah()},
 *       {@link #setItemBiayaSekolah(ItemBiayaSekolah)}.</li>
 *   <li><b>Nilai bisnis:</b> {@link #getDefaultBiaya()}, {@link #setDefaultBiaya(Double)}.</li>
 *   <li><b>Konstruktor:</b> {@link #DiskonSiswaItemBiaya()}.</li>
 * </ul>
 *
 * @see DiskonSiswa
 * @see DiskonSiswaPunyaSiswa
 * @see ItemBiayaSekolah
 * @see PengaturanBiayaItemBiaya
 * @see Tagihan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "diskon_siswa_item_biaya", schema = "sekolah")
public class DiskonSiswaItemBiaya extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Wajib ada karena {@link GeneralValueObject} mengimplementasikan
	 * {@code Serializable} dan instance entity dapat ikut tersimpan pada state komponen ZK.
	 *
	 * <p>Nilai ini dibangkitkan Hibernate Tools dan <b>tidak boleh</b> diubah tanpa alasan:
	 * mengubahnya membuat state sesi yang sudah terlanjur diserialisasi tidak dapat dibaca
	 * kembali.</p>
	 */
	private static final long serialVersionUID = 7096788954859657529L;

	/** Kunci utama baris; lihat {@link #getId()}. */
	private Long id;
	/** Nama tampilan pengguna pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris diskon item ini.
	 *
	 * @return id pengguna (biasanya {@code Tbmuser.userid}), atau {@code null} bila baris belum
	 *         pernah melewati {@code AuditTimestampInterceptor}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> (method langsung {@code return} tanpa menulis apa pun),
	 * sehingga jejak audit yang sudah terisi tidak dapat dihapus lewat setter ini. Ini pola
	 * seragam di seluruh entity repo.</p>
	 *
	 * @param olehId id pengguna baru; {@code null}/kosong tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau string kosong/spasi diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna baru; {@code null}/kosong tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna yang terakhir mengubah baris diskon item ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA yang dijalankan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari pengguna sesi aktif dan
	 * memperbarui {@link #setTanggal_dirubah(Date)}.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga properti audit pada instance ini. Dipanggil oleh
	 * penyedia JPA/Hibernate, bukan oleh kode aplikasi.</p>
	 *
	 * <p><b>Catatan penting untuk entity ini:</b> alur simpan pada layar diskon hampir tidak
	 * pernah melakukan {@code UPDATE} atas baris yang sudah ada — ia menghapus seluruh baris
	 * milik satu aturan diskon lewat native SQL lalu menyisipkannya kembali. Akibatnya kait ini
	 * jarang terpicu, dan nilai {@code oleh}/{@code olehId} praktis selalu berasal dari
	 * penyisipan terakhir, bukan dari riwayat pengubahan.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat instance dibuat
	 * (lewat {@code WaktuUtil.getDate()}, yang menghormati zona waktu konfigurasi aplikasi)
	 * sehingga baris baru tidak pernah memiliki kolom waktu kosong.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()};
	 * berbeda dari {@link #setOleh(String)}, setter ini <b>menerima</b> {@code null}.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris diskon item ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang baru
	 *         dibuat di memori, tetapi dapat {@code null} untuk baris lama di basis data
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Item biaya sekolah yang didiskon oleh baris ini; lihat {@link #getItemBiayaSekolah()}. */
	private ItemBiayaSekolah itemBiayaSekolah;
	/** Aturan diskon induk yang memiliki baris ini; lihat {@link #getDiskonSiswa()}. */
	private DiskonSiswa diskonSiswa;

	/**
	 * Besaran potongan untuk item biaya ini — <b>persen atau rupiah</b> tergantung
	 * {@code DiskonSiswa.getMenggunkanPersen()}; lihat {@link #getDefaultBiaya()}.
	 */
	private Double defaultBiaya;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung oleh {@code DiskonSiswaAction} ketika pengguna mencentang sebuah
	 * item biaya yang belum pernah didiskon. Instance hasil konstruktor ini belum punya
	 * {@link #getDiskonSiswa()} maupun {@link #getItemBiayaSekolah()}; keduanya wajib diisi
	 * sebelum {@code session.save(...)} karena kedua kolom FK-nya {@code nullable = false}.</p>
	 */
	public DiskonSiswaItemBiaya() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} dan nilainya dibangkitkan basis data
	 * ({@code IDENTITY}/sequence), sehingga id baru baru terisi setelah {@code flush}.
	 * {@code DiskonSiswaAction} memanfaatkan sifat itu: {@code id != null} dipakai sebagai
	 * penanda "item ini sudah tercentang di basis data" saat membangun ulang formulir.</p>
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Tanpa validasi.
	 *
	 * <p>Normalnya hanya dipanggil Hibernate. Mengisinya secara manual pada objek lepas berisiko
	 * membuat {@code save}/{@code update} menimpa baris diskon milik aturan lain.</p>
	 *
	 * @param id id baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan aturan diskon induk yang memiliki baris ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code diskon_siswa} yang
	 * {@code nullable = false} — setiap baris wajib bernaung di bawah tepat satu
	 * {@link DiskonSiswa}. Induk inilah pemilik saklar
	 * {@code getMenggunkanPersen()} yang menentukan arti {@link #getDefaultBiaya()}, serta
	 * pemilik masa berlaku, tahun ajaran, dan cakupan sekolah/yayasan diskon.</p>
	 *
	 * <p><b>Efek samping (aman):</b> memanggil {@code check(...)} lalu menugaskan hasilnya
	 * kembali ke field. Ini resolusi proxy lazy baku {@link GeneralValueObject}, bukan mutasi
	 * data bisnis; tidak menghasilkan {@code UPDATE}.</p>
	 *
	 * @return aturan diskon induk (proxy sudah teresolusi bila memungkinkan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon_siswa", nullable = false)
	public DiskonSiswa getDiskonSiswa() {
		diskonSiswa = check(diskonSiswa);
		return this.diskonSiswa;
	}

	/**
	 * Menyetel aturan diskon induk. Tanpa validasi maupun penolakan {@code null}.
	 *
	 * <p>Dipanggil {@code DiskonSiswaAction.onSave()} tepat sebelum {@code session.save(...)}
	 * pada tahap sisip-ulang. Karena kolomnya {@code nullable = false}, meninggalkan properti
	 * ini kosong akan menyebabkan kegagalan {@code INSERT} di tingkat basis data, bukan
	 * kesalahan validasi yang ramah.</p>
	 *
	 * @param diskonSiswa aturan diskon induk
	 */
	public void setDiskonSiswa(DiskonSiswa diskonSiswa) {
		this.diskonSiswa = diskonSiswa;
	}

	/**
	 * Mengembalikan item biaya sekolah yang didiskon oleh baris ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code item_biaya_sekolah_id} yang
	 * {@code nullable = false}. Inilah kunci yang dipakai {@code DiskonSiswaSyncHelper} untuk
	 * mencocokkan baris {@link Tagihan} milik penerima diskon, dan yang dipakai
	 * {@code TagihanDiskonSiswaHelper} untuk menjumlahkan potongan per item.</p>
	 *
	 * <p>Perhatikan bahwa seluruh pembaca menyaring item biaya yang masih aktif
	 * ({@code aktif IS NULL OR aktif = true}). Menonaktifkan sebuah {@link ItemBiayaSekolah}
	 * karena itu <b>mematikan diskonnya secara senyap</b> tanpa menghapus baris ini — dan
	 * karena {@code DiskonSiswaAction.onSave()} hanya merender item aktif, baris untuk item
	 * yang dinonaktifkan akan ikut terhapus pada penyimpanan berikutnya.</p>
	 *
	 * <p><b>Efek samping (aman):</b> resolusi proxy lazy lewat {@code check(...)}, sama seperti
	 * {@link #getDiskonSiswa()}.</p>
	 *
	 * @return item biaya sekolah yang didiskon (proxy sudah teresolusi bila memungkinkan)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_sekolah_id", nullable = false)
	public ItemBiayaSekolah getItemBiayaSekolah() {
		itemBiayaSekolah = check(itemBiayaSekolah);
		return itemBiayaSekolah;
	}

	/**
	 * Menyetel item biaya sekolah yang didiskon. Tanpa validasi maupun penolakan {@code null}.
	 *
	 * <p>Dipanggil {@code DiskonSiswaAction} saat membuat baris baru untuk item yang baru
	 * dicentang pengguna. <b>Tidak ada pemeriksaan</b> bahwa {@code itemBiayaSekolah.getSekolah()}
	 * sama dengan {@code getDiskonSiswa().getSekolah()}; konsistensi tenant sepenuhnya
	 * bergantung pada layar pemanggil (lihat bagian "Cakupan tenant" pada Javadoc kelas).</p>
	 *
	 * @param itemBiayaSekolah item biaya sekolah yang didiskon
	 */
	public void setItemBiayaSekolah(ItemBiayaSekolah itemBiayaSekolah) {
		this.itemBiayaSekolah = itemBiayaSekolah;
	}

	/**
	 * Mengembalikan besaran potongan untuk item biaya ini.
	 *
	 * <p><b>Nama properti menyesatkan</b> (warisan salin-tempel dari
	 * {@link PengaturanBiayaItemBiaya}): ini bukan "biaya default", melainkan
	 * <b>nilai diskon</b>. Penafsirannya:</p>
	 * <ul>
	 *   <li>{@code DiskonSiswa.getMenggunkanPersen() == true} (default) &rarr; angka ini adalah
	 *       <b>persen</b>; potongan dihitung {@code nominalTagihan * nilai / 100}.</li>
	 *   <li>{@code false} &rarr; angka ini adalah <b>nominal rupiah</b> yang dipotong apa
	 *       adanya.</li>
	 * </ul>
	 *
	 * <p>Getter melakukan <i>null-coalescing</i>: field kosong dilaporkan sebagai {@code 0.0}
	 * dan <b>tidak</b> ditulis balik ke field (bukan getter destruktif; membaca entity ini tidak
	 * pernah memicu {@code UPDATE}). Konsekuensinya, method ini <b>tidak pernah</b>
	 * mengembalikan {@code null}, sehingga penjaga {@code null} di
	 * {@code DiskonSiswaSyncHelper} merupakan kode mati.</p>
	 *
	 * <p><b>Konsekuensi finansial nilai 0:</b> baris yang dicentang tetapi nilainya dibiarkan
	 * kosong tetap membuat {@code DiskonSiswaSyncHelper} menstempel
	 * {@code Tagihan.setDiskonSiswa(...)} dengan potongan {@code 0} — sehingga menggeser diskon
	 * lain yang sebelumnya menempel pada tagihan yang sama. Sebaliknya,
	 * {@code TagihanDiskonSiswaHelper} baru menganggap sebuah diskon "berlaku" bila jumlah
	 * nilainya melebihi ambang {@code 0.1}.</p>
	 *
	 * <p>Properti ini sengaja tidak diberi {@code @Column}, sehingga nama kolom fisiknya
	 * diturunkan otomatis dari nama properti oleh strategi penamaan Hibernate bawaan, dan
	 * kolomnya nullable.</p>
	 *
	 * @return nilai diskon (persen atau rupiah sesuai aturan induk); {@code 0.0} bila belum
	 *         pernah diisi — tidak pernah {@code null}
	 */
	public Double getDefaultBiaya() {
		return defaultBiaya == null ? 0.0 : defaultBiaya;
	}

	/**
	 * Menyetel besaran potongan untuk item biaya ini. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onChange} kotak angka "Diskon" pada dialog
	 * tambah/ubah {@code DiskonSiswaAction} — dan <b>hanya</b> ketika checkbox item yang
	 * bersangkutan sedang tercentang. Nilai yang tersimpan langsung mempengaruhi nominal
	 * {@link Tagihan} setiap penerima diskon pada sinkronisasi berikutnya.</p>
	 *
	 * <p><b>Tidak ada validasi rentang.</b> Nilai negatif akan menaikkan tagihan alih-alih
	 * menurunkannya, dan nilai persen di atas 100 akan menghasilkan potongan melebihi nominal
	 * tagihan; keduanya diterima apa adanya oleh mesin sinkronisasi.</p>
	 *
	 * @param defaultBiaya nilai diskon baru (persen atau rupiah sesuai aturan induk); boleh
	 *                     {@code null}, yang akan dibaca kembali sebagai {@code 0.0}
	 */
	public void setDefaultBiaya(Double defaultBiaya) {
		this.defaultBiaya = defaultBiaya;
	}

}
