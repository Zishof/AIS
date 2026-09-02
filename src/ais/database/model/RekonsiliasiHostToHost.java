package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

/**
 * Entity <b>hasil rekonsiliasi pembayaran host-to-host</b> — tabel
 * {@code public.rekonsiliasi_host_to_host}, {@code @Audited} (Hibernate Envers),
 * {@code dynamicInsert}/{@code dynamicUpdate}. Turunan langsung
 * {@link ais.database.model.GeneralValueObject}.
 *
 * <h3>Peran dalam alur pembayaran</h3>
 * <p>Satu baris entity ini mewakili <b>satu baris rekaman pada berkas rekonsiliasi</b> yang
 * dikirim bank/payment gateway (host-to-host). Berkas tersebut diunggah lewat layar
 * {@code /pages/master/rekonsiliasi_host_to_host.zul}
 * ({@code ais.action.master.RekonsiliasiHostToHostAction}), disimpan sebagai
 * {@link ais.database.model.file.LampiranLain} berjenis
 * {@code LampiranLain.REKONSILIASI_HOST_TO_HOST}, lalu diurai baris demi baris oleh implementasi
 * {@code ais.action.master.helper.JenisParsingReconsile} yang dipilih pada
 * {@link #getJenisRekonsiliasiHostToHost()} — baku
 * {@code ais.action.master.helper.DefaultJenisParsingReconsile}, varian lain
 * {@code EdupayJenisParsingReconsile}. Pengurai yang sama juga dijalankan <b>terjadwal tanpa
 * campur tangan pengguna</b> oleh
 * {@code ais.action.master.helper.util.ReconsilePembayaranHostToHostSyncrhonizerProcessor}
 * (Spring {@code ScheduledTimerTask}, tiap 6 jam), yang menyapu berkas pada direktori konfigurasi
 * {@code direktori_folder_tempat_file_auto_reconsile} bila
 * {@code aktifkan_auto_reconsile_biaya_host_to_host} aktif.</p>
 *
 * <p>Isi baris berkas dipetakan ke properti entity ini dengan memecah teks memakai pemisah
 * titik-koma ({@code ";"}), lalu mengambil kolom pada posisi tetap:</p>
 * <table border="1" summary="Pemetaan kolom berkas rekonsiliasi ke properti entity">
 *   <tr><th>Kolom berkas</th><th>Properti</th></tr>
 *   <tr><td>seluruh baris mentah</td><td>{@link #getKeterangan()} — sekaligus kunci pencocokan</td></tr>
 *   <tr><td>{@code s[1]}</td><td>{@link #getWaktu()}</td></tr>
 *   <tr><td>{@code s[6]}</td><td>{@link #getKode()} (kode bayar/virtual account)</td></tr>
 *   <tr><td>{@code s[9]}</td><td>{@link #getNilai()}</td></tr>
 *   <tr><td>{@code s[10]}</td><td>{@link #getNama()} (nama pembayar)</td></tr>
 *   <tr><td>{@code s[11]}</td><td>{@link #getStatus()} — {@code 1} &rarr; {@link #SUKSES}, selain itu {@link #GAGAL}</td></tr>
 * </table>
 *
 * <h3>Hubungan dengan {@link CicilanPembayaran} dan {@link CicilanPembayaranGagal}</h3>
 * <p>Baris entity ini adalah <b>pemicu sekaligus jejak</b> perpindahan cicilan antar dua tabel
 * yang saling eksklusif. Setelah baris rekonsiliasi disimpan, pengurai mencari
 * {@link LogHostToHost} yang cocok (kode bayar sama, {@code responseCode = "00"},
 * {@code transactionType = PAY}, tanggal sama), lalu:</p>
 * <ul>
 *   <li>bila {@link #getStatus()} sama dengan {@link #SUKSES}: seluruh {@link CicilanPembayaran}
 *       yang cocok ditandai merujuk baris ini; bila tidak ada satu pun yang cocok, baris
 *       {@link CicilanPembayaranGagal} yang cocok <b>dipindahkan ke tabel sukses</b>
 *       ({@code Common.copyCicilanPembayaranKeSukses(...)} lalu
 *       {@code delete from cicilan_pembayaran_gagal ...});</li>
 *   <li>bila status <b>bukan</b> {@link #SUKSES} (termasuk saat status {@code null}): seluruh
 *       {@link CicilanPembayaran} yang cocok <b>dipindahkan ke tabel gagal</b>
 *       ({@code Common.copyCicilanPembayaranKeGagal(...)} lalu
 *       {@code delete from cicilan_pembayaran ...}).</li>
 * </ul>
 * <p>Kedua entity cicilan menyimpan referensi balik ke baris ini
 * ({@code CicilanPembayaran.getRekonsiliasiHostToHost()} dan
 * {@link CicilanPembayaranGagal#getRekonsiliasiHostToHost()}), sehingga baris rekonsiliasi
 * berfungsi sebagai <i>alasan</i> mengapa sebuah cicilan berada di tabel sukses atau gagal —
 * satu-satunya keterangan sebab yang tersedia, karena {@link CicilanPembayaranGagal} sendiri
 * tidak punya properti "alasan gagal".</p>
 *
 * <h3>Hal yang mengejutkan / perlu diketahui</h3>
 * <ul>
 *   <li><b>{@code keterangan} bukan catatan bebas, melainkan kunci alami.</b> Pengurai mencari
 *       baris lama dengan {@code Restrictions.eq("keterangan", <baris mentah>)} sebelum membuat
 *       baris baru, sehingga isi kolom ini <b>harus</b> baris berkas apa adanya. Tidak ada
 *       {@code unique constraint} yang menegakkan keunikan tersebut; pencarian hanya memakai
 *       {@code setMaxResults(1)}, jadi duplikat (mis. berkas yang sama diunggah dua kali oleh dua
 *       operator bersamaan) tidak tertangkap. Konsekuensi lain: mengubah {@code keterangan} lewat
 *       layar apa pun akan membuat baris tersebut tidak lagi dikenali saat berkas yang sama
 *       diproses ulang, sehingga rekaman yang sama dibuat lagi sebagai baris baru.</li>
 *   <li><b>Kegagalan parsing bersifat "fail-dangerous".</b> Setiap kolom diurai dalam
 *       {@code try/catch} sendiri yang hanya memanggil {@code Common.tampilErrorJikaAdmin(e)}.
 *       Bila kolom status ({@code s[11]}) tidak dapat diurai, {@link #getStatus()} tetap
 *       {@code null} dan alur di atas jatuh ke cabang "bukan sukses" — artinya
 *       <b>cicilan yang sah bisa dipindahkan ke tabel gagal hanya karena satu kolom berkas
 *       rusak</b>. Sebaliknya, bila kolom kode ({@code s[6]}) kosong, pengurai melempar
 *       {@code NullPointerException} saat memotong NIM dari {@link #getKode()} dan berhenti di
 *       tengah berkas; baris-baris sebelumnya sudah ter-<i>commit</i> karena tiap penyimpanan
 *       memakai transaksi kecil sendiri — tidak ada transaksi tunggal yang membungkus satu
 *       berkas.</li>
 *   <li><b>{@link #getLampiranId()} praktis hanya ditulis, tidak dibaca — dan ketiadaan
 *       pembacanya membuat tautan unduh berkas rusak.</b> Pengurai mengisinya dengan
 *       {@code lampiranLain.getId()}, tetapi satu-satunya pembaca di seluruh basis kode adalah
 *       daftar kolom ekspor {@code Common.cetakData(...)} pada layar rekonsiliasi. Tautan unduh
 *       berkas pada layar tersebut justru menelusuri lampiran lewat
 *       {@code LampiranLain.ambil(getId(), LampiranLain.REKONSILIASI_HOST_TO_HOST)}, yang
 *       mencocokkan kolom {@code ref} milik lampiran — padahal saat diunggah {@code ref} diisi
 *       {@code Common.randLong()}, bukan id baris rekonsiliasi. Pencarian itu karenanya selalu
 *       mengembalikan {@code null}: tautan tidak mengunduh apa pun, dan pemeriksaan "sudah ada
 *       lampiran?" pada penyimpanan ulang selalu melaporkan lampiran belum diisi. Properti inilah
 *       yang seharusnya dipakai untuk menutup celah tersebut. Nilainya juga sekadar {@code Long}
 *       lepas: tidak ada {@code @ManyToOne}/foreign key, sehingga lampiran yang dihapus
 *       meninggalkan id menggantung.</li>
 *   <li><b>{@link #getLogHostToHost()} adalah {@code @ManyToOne} tunggal</b>, bukan koleksi —
 *       satu baris rekonsiliasi hanya menyimpan <i>satu</i> log gateway hasil pencocokan
 *       ({@code setMaxResults(1)}). Bila ada beberapa log yang cocok pada tanggal yang sama, sisanya
 *       tidak tercatat. {@link LogHostToHost} sendiri punya {@code @ManyToOne} balik ke kelas ini,
 *       jadi keduanya sepasang {@code @ManyToOne} yang saling menunjuk, bukan relasi
 *       {@code @OneToMany} dua arah yang dikelola Hibernate.</li>
 *   <li><b>{@link #getStatus()} adalah teks bebas.</b> Hanya nilai yang persis sama dengan
 *       {@link #SUKSES} (perbandingan {@code equals}, peka huruf besar/kecil) yang dianggap sukses;
 *       nilai lain apa pun — termasuk {@code "sukses"} huruf kecil — diperlakukan sebagai gagal.
 *       Tidak ada {@code enum}, {@code check constraint}, maupun validasi setter.</li>
 *   <li><b>Kolom tanpa {@code @Column}.</b> {@code kode}, {@code lampiranId}, {@code nilai}, dan
 *       {@code status} tidak diberi anotasi {@code @Column}, sehingga jatuh ke
 *       {@code ais.database.hibernate.MyNamingStrategy} (turunan
 *       {@code org.hibernate.cfg.DefaultNamingStrategy}) yang memakai nama properti apa adanya —
 *       termasuk nama kolom ber-<i>camelCase</i> {@code lampiranId}.</li>
 *   <li><b>Envers tidak melihat perpindahan cicilan.</b> Kelas ini {@code @Audited}, tetapi
 *       penghapusan baris cicilan yang dipicunya dilakukan dengan SQL native
 *       ({@code session.createSQLQuery("delete from ...")}), sehingga riwayat revisi Envers untuk
 *       {@link CicilanPembayaran}/{@link CicilanPembayaranGagal} tidak pernah memuat peristiwa
 *       hapus tersebut.</li>
 *   <li>{@code serialVersionUID} kelas ini <b>sama persis</b> dengan milik
 *       {@link JenisRekonsiliasiHostToHost}, {@link LogHostToHost}, dan sejumlah entity lain
 *       ({@code 2463821577543439808L}) — sisa salin-tempel, bukan sesuatu yang bermakna.</li>
 *   <li><b>Layar penulisnya tidak benar-benar dijaga.</b>
 *       {@code RekonsiliasiHostToHostAction.doBeforeCompose} memang memanggil
 *       {@code Common.doCheckSecurity()}, tetapi rantai itu bermuara pada
 *       {@code CommonPrivilages.doCheckPrevilagesRead()} yang hanya menegakkan pemeriksaan untuk
 *       daftar tetap 12 halaman ({@code CommonPrivilages.MUST_CHECKED}) — dan
 *       {@code /pages/master/rekonsiliasi_host_to_host.zul} tidak termasuk di dalamnya. Tidak ada
 *       pemeriksaan {@code CommonPrivilages.CREATE}/{@code UPDATE} pada {@code onAdd}/{@code onSave}
 *       maupun di dalam pengurai. Bandingkan dengan
 *       {@code ais.action.master.LogHostToHostYangBelumReconsileAction} yang memeriksa
 *       {@code usersTemp} + {@code CommonPrivilages.READ} secara nyata, namun tombol
 *       destruktifnya ("Diyatakan Gagal") hanya disembunyikan lewat {@code setVisible(...)} tanpa
 *       pemeriksaan ulang peran di dalam {@code onClick}. Kedua layar menulis ke tabel cicilan yang
 *       sama dengan tingkat penjagaan berbeda.</li>
 *   <li>Komentar bawaan generator lama menyebut kelas ini "Bank generated by hbm2java"; kelas ini
 *       bukan entity bank.</li>
 * </ul>
 *
 * <h3>Getter yang menulis balik / menutup sesi / destruktif</h3>
 * <p>Diverifikasi dari kode kelas ini sendiri: <b>satu-satunya</b> getter yang menulis balik ke
 * field adalah {@link #getNilai()} (mengganti {@code null} menjadi {@code 0.0}); karena entity
 * dipetakan {@code dynamicUpdate}, nilai itu bisa ikut ter-{@code UPDATE} ke database pada
 * <i>flush</i> berikutnya. {@link #getNama()} memang memangkas spasi, tetapi mengembalikan salinan
 * dan <b>tidak</b> menulis balik ke field. Tidak ada getter yang menutup session Hibernate, tidak
 * ada getter destruktif, dan tidak ada flag aktif satu-arah. Yang perlu dicatat di sisi setter:
 * {@link #setOleh(String)} dan {@link #setOlehId(String)} <b>mengabaikan diam-diam</b> nilai
 * {@code null}/kosong, sehingga jejak audit yang sudah terisi tidak dapat dikosongkan lewat setter.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 *   <li><b>Konstanta status</b> — {@link #SUKSES}, {@link #GAGAL}.</li>
 *   <li><b>Jejak audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)} dan kait
 *       {@link #onUpdate()}. Field-field ini <b>sengaja dideklarasikan ulang</b> di sini:
 *       {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa (bukan
 *       {@code @Entity}/{@code @MappedSuperclass}), sehingga Hibernate tidak memetakan properti
 *       induknya — pengulangan ini keharusan teknis, bukan duplikasi yang keliru.</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)} dan {@link #toString()}.</li>
 *   <li><b>Isi rekaman berkas rekonsiliasi</b> — {@link #getKode()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #getWaktu()}, {@link #getNilai()},
 *       {@link #getStatus()}.</li>
 *   <li><b>Relasi &amp; asal berkas</b> — {@link #getJenisRekonsiliasiHostToHost()} (strategi
 *       pengurai), {@link #getLogHostToHost()} (log gateway hasil pencocokan),
 *       {@link #getLampiranId()} (id berkas sumber, tanpa foreign key).</li>
 * </ol>
 *
 * @see CicilanPembayaranGagal
 * @see CicilanPembayaran
 * @see LogHostToHost
 * @see JenisRekonsiliasiHostToHost
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "rekonsiliasi_host_to_host")
public class RekonsiliasiHostToHost extends GeneralValueObject {

	/**
	 * Nilai {@link #getStatus()} untuk rekaman yang dinyatakan <b>berhasil</b> oleh bank/payment
	 * gateway. Diisi pengurai berkas bila kolom status bernilai {@code 1}.
	 *
	 * <p>Perbandingan di seluruh basis kode dilakukan dengan {@code equals}, sehingga nilai ini
	 * peka huruf besar/kecil dan setiap nilai lain (termasuk {@code null}) diperlakukan sebagai
	 * gagal.</p>
	 */
	public static final String SUKSES = "Sukses";
	/**
	 * Nilai {@link #getStatus()} untuk rekaman yang dinyatakan <b>gagal</b> oleh bank/payment
	 * gateway. Diisi pengurai berkas bila kolom status bukan {@code 1}.
	 *
	 * <p>Konstanta ini dipakai untuk <i>menulis</i> status dan sebagai pilihan filter pencarian;
	 * logika percabangan pemindahan cicilan tidak pernah membandingkan dengan konstanta ini,
	 * melainkan hanya memeriksa "sama dengan {@link #SUKSES} atau bukan".</p>
	 */
	public static final String GAGAL = "Gagal";

	/**
	 * Penanda versi serialisasi Java. Nilainya identik dengan sejumlah entity lain di paket ini
	 * (mis. {@link JenisRekonsiliasiHostToHost}, {@link LogHostToHost}) — warisan salin-tempel
	 * dari generator, tanpa makna khusus.
	 */
	private static final long serialVersionUID = 2463821577543439808L;
	/** Kunci utama baris ({@code id}, {@code IDENTITY}); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, bisa {@code null} bila baris belum pernah melewati kait audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong/hanya spasi diabaikan diam-diam
	 * (method langsung {@code return}), sehingga id yang sudah terisi tidak dapat dihapus lewat
	 * setter ini.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Perhatian:</b> berperilaku sama seperti {@link #setOlehId(String)} — {@code null}
	 * atau string kosong/hanya spasi diabaikan, sehingga nilai yang sudah ada tidak dapat dihapus
	 * lewat setter ini.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, bisa {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil kontainer persistence tepat sebelum baris ini
	 * di-{@code UPDATE}, dan mendelegasikan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Catatan format: deklarasi field {@code tanggal_dirubah} berada pada baris fisik yang
	 * sama dengan method ini (gaya penyisipan otomatis di repo ini), sehingga tidak dapat diberi
	 * Javadoc terpisah. Field tersebut menyimpan waktu perubahan terakhir dan diinisialisasi ke
	 * waktu sekarang lewat {@code ais.ui.util.WaktuUtil#getDate()} saat objek dibuat; lihat
	 * {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Biasanya tidak dipanggil kode aplikasi secara langsung — pengisiannya dilakukan kait
	 * {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Tanpa {@code @Column}, sehingga nama kolomnya
	 * mengikuti nama properti apa adanya ({@code tanggal_dirubah}) sesuai
	 * {@code ais.database.hibernate.MyNamingStrategy}.</p>
	 *
	 * @return waktu perubahan terakhir; diinisialisasi ke waktu pembuatan objek, jadi umumnya
	 *         tidak {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini, yaitu <b>nama pembayar</b> ({@link #getNama()}) apa adanya —
	 * bukan {@code nama} yang sudah dipangkas, melainkan field mentahnya.
	 *
	 * <p>Dua hal yang perlu diperhatikan:</p>
	 * <ul>
	 *   <li><b>Bisa mengembalikan {@code null}</b> bila kolom nama pada berkas rekonsiliasi gagal
	 *       diurai. Pemanggil ZK yang menampilkan objek ini (mis. {@code Comboitem}/{@code Label})
	 *       harus siap menerima {@code null}.</li>
	 *   <li><b>Membocorkan nama pihak pembayar</b> ke mana pun objek ini di-<i>render</i> sebagai
	 *       teks, termasuk keluaran {@code List.toString()} pada grid layar rekonsiliasi.</li>
	 * </ul>
	 *
	 * @return nama pembayar; bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode bayar/virtual account dari berkas rekonsiliasi; lihat {@link #getKode()}. */
	private String kode;
	/** Nama pembayar dari berkas rekonsiliasi; lihat {@link #getNama()}. */
	private String nama;
	/** Baris mentah berkas rekonsiliasi, sekaligus kunci pencocokan; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Strategi pengurai yang dipakai; lihat {@link #getJenisRekonsiliasiHostToHost()}. */
	private JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost;
	/** Id {@code LampiranLain} berkas sumber (tanpa foreign key); lihat {@link #getLampiranId()}. */
	private Long lampiranId;
	/** Waktu transaksi menurut berkas rekonsiliasi; lihat {@link #getWaktu()}. */
	private Date waktu;
	/** Nominal transaksi menurut berkas rekonsiliasi; lihat {@link #getNilai()}. */
	private Double nilai;
	/** Vonis bank: {@link #SUKSES} atau {@link #GAGAL}; lihat {@link #getStatus()}. */
	private String status;
	/** Log gateway hasil pencocokan; lihat {@link #getLogHostToHost()}. */
	private LogHostToHost logHostToHost;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Semua properti dibiarkan
	 * {@code null} kecuali {@code tanggal_dirubah} yang diisi waktu sekarang pada inisialisasi
	 * field.
	 */
	public RekonsiliasiHostToHost() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} bertipe {@code IDENTITY} dan ditandai {@code insertable = false} —
	 * nilainya dibangkitkan database saat {@code INSERT}, bukan dikirim aplikasi. Layar
	 * rekonsiliasi memakai nilai ini sebagai kunci penelusuran berkas sumber
	 * ({@code LampiranLain.ambil(getId(), LampiranLain.REKONSILIASI_HOST_TO_HOST)}), tetapi
	 * penelusuran itu tidak pernah berhasil karena lampiran disimpan dengan {@code ref} acak —
	 * lihat catatan pada {@link #getLampiranId()}.</p>
	 *
	 * @return id baris; {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan <b>kode bayar</b> (nomor virtual account/kode billing) rekaman ini, diambil
	 * dari kolom ke-7 ({@code s[6]}) baris berkas rekonsiliasi.
	 *
	 * <p>Kode ini dipakai pengurai untuk dua hal: (1) mencocokkan {@link LogHostToHost} dengan
	 * kode yang sama — perlu dicatat bahwa {@code LogHostToHost.getKode()} di seberang sana
	 * <b>bukan kolom tersimpan</b>, melainkan hasil pemotongan teks pesan log
	 * ({@code nama.split("NIM =")...}), sehingga pencocokan ini bergantung pada bentuk kalimat log;
	 * dan (2) <b>menurunkan NIM mahasiswa</b> dengan memotong dua karakter
	 * terakhir kode ({@code kode.substring(0, kode.length() - 2)}) — asumsi format yang tidak
	 * divalidasi di mana pun. Bila properti ini {@code null} (kolom berkas rusak/kurang),
	 * pemotongan tersebut melempar {@code NullPointerException} dan pemrosesan berkas berhenti di
	 * tengah jalan.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya {@code kode} apa adanya sesuai
	 * {@code MyNamingStrategy}.</p>
	 *
	 * @return kode bayar/virtual account; bisa {@code null}
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode bayar/virtual account rekaman ini.
	 *
	 * <p>Dipanggil pengurai berkas ({@code JenisParsingReconsile}) dan layar rekonsiliasi. Tidak
	 * ada validasi format maupun panjang minimum, padahal pembacanya mengasumsikan panjang
	 * sekurang-kurangnya dua karakter (lihat {@link #getKode()}).</p>
	 *
	 * @param kode kode bayar/virtual account
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan <b>nama pembayar</b> menurut berkas rekonsiliasi (kolom ke-11,
	 * {@code s[10]}), sudah dipangkas spasi awal/akhir.
	 *
	 * <p>Pemangkasan dilakukan pada salinan yang dikembalikan; field {@code nama} sendiri
	 * <b>tidak</b> ditulis balik, sehingga getter ini bukan getter yang menulis balik ke
	 * database. Perlu diingat {@link #toString()} justru mengembalikan field mentahnya, bukan
	 * hasil pemangkasan ini.</p>
	 *
	 * @return nama pembayar tanpa spasi tepi; {@code null} bila field belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama pembayar rekaman ini.
	 *
	 * <p>Kolom dipetakan {@code nullable = false} sepanjang 255 karakter, tetapi setter ini tidak
	 * memvalidasi apa pun — nilai {@code null} baru akan ditolak database saat {@code INSERT}.
	 * Karena pengurai mengisi properti ini di dalam {@code try/catch} yang menelan kesalahan,
	 * baris berkas dengan kolom nama hilang akan gagal disimpan pada tahap yang jauh lebih
	 * lambat.</p>
	 *
	 * @param nama nama pembayar
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan <b>baris mentah berkas rekonsiliasi</b> yang menghasilkan baris entity ini —
	 * bukan catatan bebas yang diketik pengguna.
	 *
	 * <p>Nilai ini berperan sebagai <b>kunci alami</b>: sebelum membuat baris baru, pengurai
	 * mencari baris lama dengan {@code Restrictions.eq("keterangan", <baris mentah>)} sehingga
	 * pemrosesan ulang berkas yang sama memperbarui baris lama alih-alih menggandakannya. Tidak
	 * ada indeks unik yang menegakkan hal itu di database. Nilai ini juga ditampilkan apa adanya
	 * sebagai teks tautan unduh pada grid layar rekonsiliasi — artinya <b>seluruh isi rekaman
	 * bank</b> (termasuk kolom yang tidak dipetakan ke properti mana pun) terlihat oleh siapa pun
	 * yang bisa membuka layar tersebut, dan ikut terbawa pada filter pencarian
	 * {@code ilike} berdasarkan keterangan.</p>
	 *
	 * <p>Dipetakan sebagai kolom {@code text} agar muat menampung baris berkas sepanjang apa pun.</p>
	 *
	 * @return baris mentah berkas rekonsiliasi; bisa {@code null} untuk baris yang dibuat manual
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel baris mentah berkas rekonsiliasi.
	 *
	 * <p><b>Efek samping penting:</b> karena nilai ini adalah kunci pencocokan pengurai (lihat
	 * {@link #getKeterangan()}), mengubahnya membuat baris ini tidak lagi dikenali saat berkas
	 * yang sama diproses ulang — rekaman tersebut akan dibuat lagi sebagai baris baru dan alur
	 * pemindahan cicilan dijalankan sekali lagi.</p>
	 *
	 * @param keterangan baris mentah berkas rekonsiliasi
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan <b>jenis/strategi rekonsiliasi</b> yang dipakai untuk mengurai berkas sumber
	 * baris ini.
	 *
	 * <p>{@link JenisRekonsiliasiHostToHost#getNamaKelas()} menyimpan <b>nama kelas lengkap</b>
	 * implementasi {@code ais.action.master.helper.JenisParsingReconsile}, yang dimuat secara
	 * reflektif ({@code Class.forName(...).newInstance()}) saat berkas diunggah atau saat
	 * penjadwal rekonsiliasi berjalan. Nilai bakunya
	 * {@code ais.action.master.helper.DefaultJenisParsingReconsile}, ditentukan konfigurasi
	 * {@code default_class_yang_digunakan_untuk_memproses_reconsile_pembayaran_host_to_host}.</p>
	 *
	 * <p>Relasi {@code @ManyToOne} dengan {@code cascade = PERSIST, MERGE} dan
	 * {@code FetchMode.SELECT}; kolom join {@code jenis_rekonsiliasi_host_to_host},
	 * {@code nullable = true}.</p>
	 *
	 * @return jenis rekonsiliasi; bisa {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_rekonsiliasi_host_to_host", nullable = true)
	public JenisRekonsiliasiHostToHost getJenisRekonsiliasiHostToHost() {
		return jenisRekonsiliasiHostToHost;
	}

	/**
	 * Menyetel jenis/strategi rekonsiliasi baris ini.
	 *
	 * <p>Diisi pengurai dengan jenis yang dipilih operator pada layar unggah, sehingga baris hasil
	 * menyimpan jejak "diurai dengan strategi apa". Karena {@code cascade = PERSIST, MERGE},
	 * menyimpan baris rekonsiliasi juga akan menyimpan objek jenis yang belum tersimpan.</p>
	 *
	 * @param jenisRekonsiliasiHostToHost jenis rekonsiliasi; boleh {@code null}
	 */
	public void setJenisRekonsiliasiHostToHost(JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost) {
		this.jenisRekonsiliasiHostToHost = jenisRekonsiliasiHostToHost;
	}

	/**
	 * Mengembalikan id {@link ais.database.model.file.LampiranLain} berkas rekonsiliasi yang
	 * menjadi sumber baris ini.
	 *
	 * <p><b>Referensi lepas, bukan relasi.</b> Tidak ada {@code @ManyToOne}/foreign key, sehingga
	 * lampiran yang dihapus meninggalkan id menggantung tanpa terdeteksi. Properti ini juga
	 * praktis <b>hanya ditulis</b>: satu-satunya pembacanya di seluruh basis kode adalah daftar
	 * kolom ekspor {@code Common.cetakData(...)} pada layar rekonsiliasi. Tautan unduh berkas pada
	 * layar yang sama justru menelusuri lampiran lewat
	 * {@code LampiranLain.ambil(getId(), LampiranLain.REKONSILIASI_HOST_TO_HOST)}, tanpa menyentuh
	 * properti ini.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya ber-<i>camelCase</i> {@code lampiranId}
	 * sesuai {@code MyNamingStrategy}.</p>
	 *
	 * @return id lampiran berkas sumber; bisa {@code null}
	 */
	public Long getLampiranId() {
		return lampiranId;
	}

	/**
	 * Menyetel id lampiran berkas rekonsiliasi sumber.
	 *
	 * <p>Dipanggil pengurai dengan {@code lampiranLain.getId()} tepat setelah baris rekonsiliasi
	 * dibuat/ditemukan. Bila berkas yang sama diunggah ulang sebagai lampiran baru, nilai lama
	 * <b>ditimpa</b> sehingga jejak unggahan pertama hilang.</p>
	 *
	 * @param lampiranId id lampiran berkas sumber
	 */
	public void setLampiranId(Long lampiranId) {
		this.lampiranId = lampiranId;
	}

	/**
	 * Mengembalikan <b>waktu transaksi</b> menurut berkas rekonsiliasi (kolom ke-2, {@code s[1]},
	 * berformat {@code yyyyMMddHHmmss}).
	 *
	 * <p>Nilai ini bukan sekadar informasi tampilan: pengurai memakainya untuk membatasi
	 * pencarian {@link LogHostToHost} yang cocok pada <b>tanggal yang sama</b>
	 * ({@code DATE(this_.tanggal) = DATE(waktu)}) dan meneruskannya ke pencarian cicilan yang akan
	 * dipindahkan. Layar rekonsiliasi juga mengurutkan grid menurun berdasarkan properti ini serta
	 * menyediakan filter rentang tanggal mulai/sampai.</p>
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Bila parsing kolom tanggal gagal, nilainya tetap
	 * {@code null} dan pembentukan restriksi tanggal pada pencarian log akan melempar kesalahan.</p>
	 *
	 * @return waktu transaksi; bisa {@code null} bila kolom berkas gagal diurai
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu;
	}

	/**
	 * Menyetel waktu transaksi rekaman ini.
	 *
	 * @param waktu waktu transaksi menurut berkas rekonsiliasi
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan <b>nominal transaksi</b> menurut berkas rekonsiliasi (kolom ke-10,
	 * {@code s[9]}).
	 *
	 * <p><b>Getter yang menulis balik:</b> bila field masih {@code null}, method ini menetapkannya
	 * menjadi {@code 0.0} lebih dulu. Karena entity dipetakan {@code dynamicUpdate}, perubahan itu
	 * dapat ikut ter-{@code UPDATE} ke database pada <i>flush</i> berikutnya — sekadar membaca
	 * nominal pada baris yang nominalnya belum terisi bisa mengubah isi tabel. Akibat lain yang
	 * lebih halus: nominal yang gagal diurai tidak dapat lagi dibedakan dari transaksi bernilai
	 * nol.</p>
	 *
	 * <p>Nominal ini bersifat informatif saja — alur pemindahan cicilan sepenuhnya ditentukan
	 * {@link #getStatus()} dan pencocokan kode/tanggal, <b>tanpa</b> membandingkan nominal
	 * rekonsiliasi dengan nominal cicilan.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya {@code nilai} apa adanya sesuai
	 * {@code MyNamingStrategy}.</p>
	 *
	 * @return nominal transaksi; tidak pernah {@code null} ({@code 0.0} bila belum terisi)
	 */
	public Double getNilai() {
		if (nilai == null) {
			nilai = 0.0;
		}
		return nilai;
	}

	/**
	 * Menyetel nominal transaksi rekaman ini.
	 *
	 * @param nilai nominal transaksi; {@code null} akan diubah menjadi {@code 0.0} pada pembacaan
	 *              berikutnya oleh {@link #getNilai()}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan <b>vonis bank</b> atas rekaman ini: {@link #SUKSES}, {@link #GAGAL}, atau
	 * {@code null} bila kolom status berkas gagal diurai.
	 *
	 * <p>Ini adalah properti paling menentukan di kelas ini. Pengurai
	 * ({@code DefaultJenisParsingReconsile}/{@code EdupayJenisParsingReconsile}) dan
	 * {@code RekonsiliasiHostToHostAction} sama-sama memakai pola
	 * {@code status != null && status.equals(SUKSES)}:</p>
	 * <ul>
	 *   <li><b>sama dengan {@link #SUKSES}</b> &rarr; cicilan yang cocok dianggap sah; bila tidak
	 *       ada cicilan sukses yang cocok, baris {@link CicilanPembayaranGagal} yang cocok
	 *       dipindahkan kembali ke {@link CicilanPembayaran};</li>
	 *   <li><b>selain itu (termasuk {@code null})</b> &rarr; cicilan sukses yang cocok
	 *       <b>dipindahkan ke {@link CicilanPembayaranGagal}</b> dan baris aslinya dihapus dengan
	 *       SQL native.</li>
	 * </ul>
	 * <p>Karena cabang "gagal" adalah cabang <i>default</i>, kegagalan mengurai satu kolom berkas
	 * berakibat merugikan mahasiswa: pembayaran yang sudah tercatat sukses dapat berpindah ke
	 * tabel gagal. Perbandingan juga peka huruf besar/kecil dan tidak dijaga {@code enum} maupun
	 * {@code check constraint}.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya {@code status} apa adanya sesuai
	 * {@code MyNamingStrategy}.</p>
	 *
	 * @return {@link #SUKSES}, {@link #GAGAL}, teks lain, atau {@code null}
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Menyetel vonis bank atas rekaman ini.
	 *
	 * <p>Diisi pengurai dari kolom ke-12 berkas ({@code s[11]}): bernilai {@code 1} menjadi
	 * {@link #SUKSES}, selain itu {@link #GAGAL}. Nilai apa pun boleh masuk — setter ini tidak
	 * memvalidasi bahwa argumen merupakan salah satu dari dua konstanta kelas ini, sehingga teks
	 * yang salah ketik akan diperlakukan sebagai gagal oleh seluruh pembaca.</p>
	 *
	 * @param status status rekaman; sebaiknya {@link #SUKSES} atau {@link #GAGAL}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan <b>log transaksi gateway</b> yang berhasil dicocokkan dengan rekaman ini.
	 *
	 * <p>Pencocokan dilakukan pengurai dengan kriteria: {@code responseCode = "00"},
	 * {@code transactionType = ConstantUtil.PAY}, {@code kode} sama dengan {@link #getKode()},
	 * dan tanggal log sama dengan tanggal {@link #getWaktu()} — diambil <b>satu</b> hasil saja
	 * ({@code setMaxResults(1)}).</p>
	 *
	 * <p>Nilai {@code null} berarti tidak ada log gateway yang cocok; dalam kondisi itu pengurai
	 * <b>tidak melakukan pemindahan cicilan sama sekali</b> — baris rekonsiliasi tetap tersimpan,
	 * tetapi status sukses/gagalnya tidak berpengaruh apa pun pada tabel cicilan. Baris seperti ini
	 * yang muncul pada layar "Log Host To Host Yang Belum Reconsile".</p>
	 *
	 * <p>Relasi {@code @ManyToOne} (bukan koleksi) dengan {@code cascade = PERSIST, MERGE} dan
	 * {@code FetchMode.SELECT}; kolom join {@code log_host_to_host}, {@code nullable = true}.
	 * {@link LogHostToHost} menyimpan {@code @ManyToOne} balik ke kelas ini, jadi keduanya sepasang
	 * penunjuk yang harus dijaga konsisten secara manual oleh pemanggil — Hibernate tidak
	 * menyinkronkannya.</p>
	 *
	 * @return log gateway yang cocok; {@code null} bila belum/tidak ada yang cocok
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "log_host_to_host", nullable = true)
	public LogHostToHost getLogHostToHost() {
		return logHostToHost;
	}

	/**
	 * Menyetel log transaksi gateway hasil pencocokan.
	 *
	 * <p>Dipanggil pengurai berkas setelah pencarian log selesai (termasuk saat hasilnya
	 * {@code null}). Pemanggil bertanggung jawab menyetel pula penunjuk baliknya lewat
	 * {@code LogHostToHost#setRekonsiliasiHostToHost(RekonsiliasiHostToHost)}; kedua sisi tidak
	 * saling menyinkronkan.</p>
	 *
	 * @param logHostToHost log gateway; boleh {@code null}
	 */
	public void setLogHostToHost(LogHostToHost logHostToHost) {
		this.logHostToHost = logHostToHost;
	}

}
