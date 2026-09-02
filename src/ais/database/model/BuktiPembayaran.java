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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;

/**
 * Entity <b>bukti pembayaran</b> (tabel {@code public.bukti_pembayaran}) — satu baris mewakili satu
 * <i>klaim/kwitansi setoran</i> yang dicatat atas nama seorang mahasiswa atau calon mahasiswa,
 * lengkap dengan nilai rupiah, tanggal, cara bayar, dan (opsional) berkas bukti transfer yang
 * diunggah.
 *
 * <h2>Perannya dalam alur pembayaran</h2>
 *
 * <p>Alur uang di AIS memakai tiga entity yang sering tertukar. Ringkasnya:</p>
 *
 * <pre>
 *   ItemBiaya / PengaturanPembayaranBulanan   &rarr; berapa YANG HARUS dibayar (tagihan)
 *   BuktiPembayaran                            &rarr; berapa YANG DIKLAIM sudah dibayar (setoran)
 *   CicilanPembayaran                          &rarr; berapa YANG DIAKUI lunas (posting resmi)
 * </pre>
 *
 * <p>Urutan tipikalnya:</p>
 *
 * <ol>
 *   <li>Mahasiswa/calon mengunggah foto slip transfer lewat portal. Berkasnya masuk ke
 *       {@link ais.database.model.file.LampiranLain} (kolom BLOB), lalu <i>satu baris</i>
 *       {@code BuktiPembayaran} dibuat sebagai penunjuknya —
 *       lihat {@code ais/action/servlet/DoUpload.java} dan
 *       {@code ais/action/master/pmb/CariDataPembayaranAction.java}. Pada tahap ini
 *       {@link #getNilai()} biasanya masih {@code 0.0} dan {@link #getCicilanPembayaran()} masih
 *       {@code null}: baris ini sekadar "ada berkas masuk, tolong diperiksa".</li>
 *   <li>Petugas keuangan membuka layar Bukti Pembayaran
 *       ({@code ais/action/master/BuktiPembayaranAction.java}, halaman
 *       {@code /pages/master/bukti_pembayaran.zul}), melengkapi nilai, item biaya, cara bayar,
 *       semester, dan keterangan.</li>
 *   <li>Setelah diverifikasi, setoran itu di-posting menjadi {@link CicilanPembayaran}. Sejak itu
 *       hubungannya <b>dua arah tetapi tidak simetris</b>: {@code CicilanPembayaran} menyimpan FK
 *       {@code bukti_pembayaran}, dan baris {@code BuktiPembayaran} ini <i>juga</i> menyimpan FK
 *       {@code cicilan_pembayaran} balik. Dua kolom itu harus dijaga konsisten oleh kode
 *       pemanggil — tidak ada {@code mappedBy} yang menegakkannya.</li>
 * </ol>
 *
 * <h2>Relasi dengan {@link CicilanPembayaran} dan {@link CicilanPembayaranGagal}</h2>
 *
 * <p>{@link CicilanPembayaran} memiliki properti {@code buktiPembayaran} ber-FK ke tabel ini, dan
 * <i>menurunkan</i> dua nilai darinya:</p>
 *
 * <ul>
 *   <li>{@link CicilanPembayaran#getIdLampiran()} menyalin balik {@link #getIdLampiran()} bila
 *       bukti bayarnya punya lampiran (getter yang menulis ke field — pola berulang di repo ini);</li>
 *   <li>{@link CicilanPembayaran#getJenisPembayaran()} mewarisi {@link #getJenisPembayaran()} bila
 *       cicilannya sendiri belum punya cara bayar.</li>
 * </ul>
 *
 * <p><b>Konsekuensi penting.</b> {@link CicilanPembayaranGagal} adalah tabel <i>terpisah</i>:
 * status "gagal" tidak disimpan sebagai flag, melainkan barisnya dipindah fisik antara
 * {@code cicilan_pembayaran} dan {@code cicilan_pembayaran_gagal}. Skema tabel gagal tidak
 * memiliki kolom {@code bukti_pembayaran} maupun {@code id_lampiran}, sehingga perjalanan
 * sukses&rarr;gagal&rarr;sukses <b>memutus tautan cicilan ke bukti bayar secara permanen</b>.
 * Yang perlu ditegaskan: <i>baris {@code BuktiPembayaran} itu sendiri tidak ikut hilang</i> — data
 * di tabel ini beserta berkas lampirannya tetap utuh. Yang hilang hanyalah penunjuk dari sisi
 * cicilan, sehingga bukti bayar menjadi "yatim" (tidak lagi muncul pada baris cicilan yang
 * bersangkutan) dan harus dikaitkan ulang secara manual lewat layar Bukti Pembayaran. Detailnya
 * tercatat pada Javadoc kelas {@link CicilanPembayaranGagal}.</p>
 *
 * <h2>Dua mekanisme tautan berkas yang hidup berdampingan</h2>
 *
 * <p>Berkas bukti bayar disimpan di {@link ais.database.model.file.LampiranLain}, dan repo ini
 * memakai <b>dua</b> cara berbeda untuk menautkannya — keduanya aktif, tanpa FK formal:</p>
 *
 * <ul>
 *   <li><b>Penunjuk maju:</b> {@link #getIdLampiran()} menyimpan {@code LampiranLain.id} secara
 *       langsung. Dipakai oleh jalur unggah portal ({@code DoUpload}) dan oleh
 *       {@code BuktiPembayaranAction} ketika berkasnya sudah ada saat penyimpanan.</li>
 *   <li><b>Penunjuk mundur:</b> {@code LampiranLain.ref} = {@code BuktiPembayaran.id} dengan
 *       {@code LampiranLain.jenis} = {@code "ais.database.model.BuktiPembayaran"}. Dipakai tombol
 *       unduh/unggah pada layar cicilan lewat
 *       {@code LampiranLain.createDownloadUploadFileLain(hbox, buktiPembayaran.getId(),
 *       BuktiPembayaran.class.getName(), ...)} di {@code CommonPaymentHelper}.</li>
 * </ul>
 *
 * <p>Perbedaannya bukan sekadar arah penunjuk: kedua jalur memanggil servlet unduh yang sama
 * dengan flag {@code usingId} yang berlawanan, sehingga kriteria pencarian barisnya pun berbeda
 * ({@code idEq(idLampiran)} versus {@code ref = id AND jenis = "…BuktiPembayaran"}). Karena
 * keduanya tidak saling menyinkronkan, satu bukti bayar bisa punya lampiran yang terlihat di satu
 * layar tetapi tidak di layar lain. Ini bukan bug yang diperkenalkan kelas ini — hanya konsekuensi
 * dua konvensi historis yang tidak pernah disatukan.</p>
 *
 * <h2>Sasaran pembayaran: mahasiswa ATAU calon mahasiswa</h2>
 *
 * <p>Dua relasi {@link #getMahasiswa()} dan {@link #getBiodataCalonMahasiswa()} keduanya
 * {@code nullable} dan <b>tidak saling eksklusif secara skema</b>. Konvensinya: pembayaran
 * pendaftaran (calon, belum punya NIM) mengisi {@code biodataCalonMahasiswa}; pembayaran mahasiswa
 * aktif mengisi {@code mahasiswa}. Pada daftar ulang mahasiswa baru, {@code BuktiPembayaranAction}
 * mengisi <i>keduanya</i> — calon yang sudah diterima sekaligus punya baris {@link Mahasiswa}.
 * Kode pembaca karenanya tidak boleh mengasumsikan salah satunya pasti terisi.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ul>
 *   <li><b>Audit (bayangan {@link GeneralValueObject}):</b> {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas setoran:</b> {@link #getNama()} (nama/nomor kwitansi atau nama berkas),
 *       {@link #getKeterangan()}, {@link #getTanggal()}, {@link #getNilai()},
 *       {@link #getSemester()}.</li>
 *   <li><b>Sasaran:</b> {@link #getMahasiswa()}, {@link #getBiodataCalonMahasiswa()}.</li>
 *   <li><b>Klasifikasi biaya:</b> {@link #getJenisKegiatan()}, {@link #getItemBiaya()},
 *       {@link #getPengaturanPembayaranBulanan()}, {@link #getJenisPembayaran()}.</li>
 *   <li><b>Tautan hasil &amp; berkas:</b> {@link #getCicilanPembayaran()},
 *       {@link #getIdLampiran()}.</li>
 *   <li><b>Utilitas:</b> {@link #toString()}.</li>
 * </ul>
 *
 * <p>Tidak ada method bisnis, query statis, maupun {@code equals}/{@code hashCode} di kelas ini —
 * seluruh logikanya berada di getter (lihat bagian berikut) dan di kelas Action pemakainya.</p>
 *
 * <h2>Pola berulang yang DIVERIFIKASI pada kelas ini</h2>
 *
 * <p>Hasil pemeriksaan kode kelas ini sendiri, bukan asumsi dari entity lain:</p>
 *
 * <ul>
 *   <li><b>Getter yang menulis balik ke field:</b> <span>ADA</span>, lima buah —
 *       {@link #getMahasiswa()}, {@link #getBiodataCalonMahasiswa()}, {@link #getJenisKegiatan()},
 *       {@link #getItemBiaya()}, {@link #getJenisPembayaran()} semuanya menugaskan ulang
 *       {@code field = check(field)}. Penugasan ulang ini <i>disengaja</i> (lihat
 *       {@link GeneralValueObject#check(Object)}): tanpanya proxy yang sudah diselesaikan akan
 *       diselesaikan lagi pada setiap pemanggilan. Efeknya terhadap database hanya tak langsung —
 *       proxy diganti instance yang setara, jadi Hibernate tidak melihatnya sebagai perubahan
 *       nilai dan tidak menghasilkan {@code UPDATE}.</li>
 *   <li><b>Getter yang menutup sesi Hibernate:</b> <span>TIDAK ADA</span> di kelas ini. Kelas ini
 *       tidak pernah memanggil {@code HibernateUtil} maupun {@code closeSession()} secara langsung;
 *       satu-satunya sentuhan sesi terjadi di dalam {@code check()} milik kelas induk, yang
 *       mengelola dan menutup sesi darurat miliknya sendiri.</li>
 *   <li><b>Getter destruktif</b> (menge-{@code null}-kan atau merusak nilai tersimpan):
 *       <span>TIDAK ADA</span>. Perlu ditegaskan karena kembarannya
 *       {@link CicilanPembayaranGagal#getTanggal()} <i>destruktif</i>. Di kelas ini
 *       {@link #getTanggal()} dan {@link #getNilai()} hanya melakukan <i>substitusi nilai default
 *       saat baca</i> ({@code null} &rarr; hari ini / {@code 0.0}) <b>tanpa</b> menyentuh field,
 *       sehingga nilai {@code null} di database tetap {@code null}. Bedanya halus tetapi nyata,
 *       dan hanya berlaku selama pemanggil tidak menyimpan hasil bacaan itu kembali lewat
 *       setternya.</li>
 * </ul>
 *
 * <h2>Catatan keamanan bagi pemakai entity ini</h2>
 *
 * <p>Baris tabel ini memuat data pribadi keuangan (identitas pembayar, nominal, keterangan) dan
 * menunjuk ke berkas slip transfer. Penelusuran pemakainya menemukan bahwa <b>pengamanannya
 * bertumpu sepenuhnya pada otorisasi tingkat halaman</b>, bukan tingkat baris. Dicatat di sini
 * sebagai peringatan bagi kode baru yang akan memakai entity ini — bukan sebagai deskripsi
 * perilaku entity itu sendiri, yang memang tidak menegakkan apa pun:</p>
 *
 * <ul>
 *   <li>{@code ais/action/master/BuktiPembayaranAction.java} membaca identitas mahasiswa yang
 *       login hanya untuk menyembunyikan tombol dan memprakisi form; {@code initCriteria}-nya
 *       <b>tidak</b> menambahkan predikat kepemilikan, sehingga daftar yang tampil mencakup bukti
 *       bayar seluruh orang. Parameter URL {@code ?mahasiswa=<id>} yang dikirim
 *       {@code InformasiPembayaranMahasiswaAction} tidak pernah dibaca layar itu dan diabaikan
 *       diam-diam.</li>
 *   <li>Kedua layar daftar ulang memuat bukti berdasarkan id dari parameter URL
 *       ({@code Restrictions.idEq(...)}) tanpa memeriksa siapa pemiliknya.</li>
 *   <li>Berkas lampirannya diunduh lewat servlet {@code ais/action/servlet/AmbilLampiran.java}
 *       yang tidak memeriksa kepemilikan sama sekali. Repo sudah memuat catatan temuan terbuka
 *       {@code ais/action/servlet/SECURITY_FINDING_AmbilLampiran_IDOR.md}, tetapi jalur bukti
 *       pembayaran belum masuk dalam daftar yang diaudit di sana.</li>
 * </ul>
 *
 * <p>Kode baru yang menampilkan {@code BuktiPembayaran} kepada pengguna non-petugas <b>wajib</b>
 * menambahkan filter kepemilikannya sendiri; jangan berasumsi lapisan mana pun sudah
 * melakukannya.</p>
 *
 * <h2>Catatan pemetaan</h2>
 *
 * <p>Pemetaan memakai akses <i>property</i> (anotasi menempel di getter). Karena
 * {@code ais.database.hibernate.MyNamingStrategy} adalah turunan {@code DefaultNamingStrategy}
 * (nama kolom = nama properti apa adanya, tanpa konversi ke {@code snake_case}), properti tanpa
 * {@code @Column} — {@code tanggal}, {@code semester}, {@code nilai}, {@code idLampiran},
 * {@code tanggal_dirubah}, {@code oleh}, {@code olehId} — dipetakan ke kolom bernama <b>persis
 * sama</b> dengan nama propertinya, termasuk {@code idLampiran} dengan huruf besar di tengah.</p>
 *
 * <p>Entity ini {@link Audited} (Hibernate Envers), sehingga setiap {@code INSERT}/{@code UPDATE}
 * menghasilkan baris riwayat. Seperti pada entity lain di repo ini, penghapusan yang dilakukan
 * lewat SQL native akan lolos dari pencatatan Envers.</p>
 *
 * <p>{@code dynamicInsert}/{@code dynamicUpdate} aktif: Hibernate hanya menyertakan kolom yang
 * benar-benar berubah pada pernyataan SQL-nya. Berguna di sini karena kebanyakan kolom relasi
 * bernilai {@code null}.</p>
 *
 * <p>Perhatikan bahwa {@code @Id} dianotasi {@code insertable = false} — nilai id sepenuhnya
 * berasal dari {@code IDENTITY} (sequence) database, dan {@link #setId(Long)} pada object baru
 * tidak akan mengubah id yang dihasilkan.</p>
 *
 * <p>Seluruh relasi kelas ini <b>searah</b>: tidak ada satu pun entity yang memetakan koleksi
 * {@code Set<BuktiPembayaran>}/{@code List<BuktiPembayaran>}, dan satu-satunya properti bertipe
 * {@code BuktiPembayaran} di seluruh repo adalah {@link CicilanPembayaran#getBuktiPembayaran()}.
 * Untuk mengambil bukti bayar milik seorang mahasiswa, jalankan query atas kelas ini — jangan
 * mencarinya sebagai koleksi pada {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}. Seluruh
 * akses ke tabel ini memakai Hibernate Criteria; tidak ada HQL maupun SQL native yang menyebut
 * nama tabel {@code bukti_pembayaran}.</p>
 *
 * <p><b>Peringatan komentar generator.</b> Komentar hbm2java asli di atas kelas ini berbunyi
 * "Bank generated by hbm2java" — sisa salin-tempel dari entity {@code Bank}, tidak ada
 * hubungannya dengan bukti pembayaran. Ditinggalkan apa adanya karena hanya komentar.</p>
 *
 * @see CicilanPembayaran
 * @see CicilanPembayaranGagal
 * @see JenisPembayaran
 * @see ItemBiaya
 * @see PengaturanPembayaranBulanan
 * @see ais.database.model.file.LampiranLain
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "bukti_pembayaran")

public class BuktiPembayaran extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya dibekukan sejak entity ini dibuat; mengubahnya akan
	 * mematahkan sesi ZK dan cache yang sudah terserialisasi di lapangan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci primer, diisi database lewat {@code IDENTITY}. Membayangi field {@code id} milik
	 * {@link GeneralValueObject} — deklarasi ulang ini <b>keharusan teknis</b>, bukan bug: kelas
	 * induk adalah POJO abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) sehingga
	 * Hibernate tidak memetakan propertinya.
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor}. Membayangi field induk.
	 */
	private String oleh;

	/**
	 * Id pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor}. Membayangi field induk.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna audit; boleh {@code null} pada baris yang belum pernah diperbarui
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna audit.
	 *
	 * <p><b>Menolak diam-diam.</b> Argumen {@code null} atau berisi spasi saja diabaikan tanpa
	 * pesan apa pun, sehingga nilai audit yang sudah ada tidak pernah terhapus oleh proses
	 * background yang kebetulan tidak punya konteks pengguna. Konsekuensinya, nilai ini tidak bisa
	 * dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna audit; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna audit.
	 *
	 * <p>Menolak {@code null}/kosong diam-diam dengan alasan yang sama seperti
	 * {@link #setOlehId(String)}.</p>
	 *
	 * @param oleh nama pengguna audit; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna audit; boleh {@code null}
	 * @see GeneralValueObject#getOleh()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Implementasi wajib {@link GeneralValueObject#onUpdate()} — satu-satunya method abstract yang
	 * harus dipenuhi setiap entity AIS. Dipanggil container JPA sebagai callback
	 * {@link javax.persistence.PreUpdate} tepat sebelum {@code UPDATE} dikirim ke database, lalu
	 * meneruskannya ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang
	 * mengisi {@code tanggal_dirubah}, {@code oleh}, dan {@code olehId} dari pengguna yang sedang
	 * login.
	 *
	 * <p>Efek samping: mengubah tiga field audit object ini. Tidak dipanggil pada {@code INSERT}
	 * — untuk baris baru, {@code tanggal_dirubah} berasal dari inisialisasi field di bawah.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object (bukan {@code null})
	 * dan diperbarui otomatis lewat {@link #onUpdate()}. Membayangi field induk.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi — biasanya hanya dipanggil oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang dibuat lewat
	 *         konstruktor Java (diinisialisasi di deklarasi field)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas berformat {@code "<id>-<nama>"}, dipakai ZK untuk mengisi label
	 * {@code Combobox}/{@code Listcell} dan untuk keluaran log.
	 *
	 * <p>Membaca <b>field</b> {@code nama} langsung, bukan lewat {@link #getNama()}, sehingga
	 * spasi di ujung nama tidak dipangkas di sini padahal dipangkas oleh getternya. Untuk baris
	 * yang belum disimpan, {@code id} masih {@code null} sehingga hasilnya berawalan
	 * {@code "null-"}.</p>
	 *
	 * @return {@code id} dan {@code nama} yang digabung tanda hubung; tidak pernah {@code null}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama/nomor kwitansi setoran. Pada bukti yang berasal dari unggahan portal, diisi <b>nama
	 * berkas</b> yang diunggah ({@code DoUpload}); pada entri manual petugas keuangan, diisi teks
	 * bebas dari kotak "Nama". Kolomnya {@code NOT NULL}.
	 */
	private String nama;

	/** Keterangan bebas tentang setoran (nomor referensi bank, catatan verifikasi, dsb). */
	private String keterangan;

	/** Tanggal setoran menurut bukti (presisi hari, bukan waktu). Boleh {@code null} di database. */
	private Date tanggal;

	/**
	 * Mahasiswa aktif pemilik setoran. {@code null} bila setoran ini milik calon mahasiswa yang
	 * belum ber-NIM; lihat {@link #biodataCalonMahasiswa}.
	 */
	private Mahasiswa mahasiswa;

	/**
	 * Calon mahasiswa pemilik setoran (jalur PMB/pendaftaran). Bisa terisi bersamaan dengan
	 * {@link #mahasiswa} pada alur daftar ulang mahasiswa baru.
	 */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	/**
	 * Baris pengaturan pembayaran bulanan (SPP per bulan) yang menjadi sasaran setoran ini, bila
	 * setorannya memang menyasar tagihan bulanan. {@code null} untuk setoran biaya non-bulanan.
	 */
	private PengaturanPembayaranBulanan pengaturanPembayaranBulanan;

	/**
	 * Jenis kegiatan yang membingkai setoran (pendaftaran calon, daftar ulang mahasiswa baru,
	 * pendaftaran mahasiswa lama, dsb). Menentukan tarif/paket biaya mana yang berlaku.
	 */
	private JenisKegiatan jenisKegiatan;

	/** Pos/komponen biaya yang dibayar (SPP, praktikum, wisuda, dsb). */
	private ItemBiaya itemBiaya;

	/**
	 * Cara pembayaran (tunai, transfer bank tertentu, virtual account, dsb) — sekaligus jembatan
	 * ke akunting karena tiap {@link JenisPembayaran} memetakan ke satu {@code Akun}.
	 */
	private JenisPembayaran jenisPembayaran;

	/** Semester akademik sasaran setoran. Diisi dari combobox; default 1 bila tidak dipilih. */
	private Integer semester;

	/** Nilai rupiah setoran. Boleh {@code null} di database — lihat {@link #getNilai()}. */
	private Double nilai;

	/**
	 * Cicilan resmi hasil posting setoran ini, bila sudah diverifikasi. {@code null} selama bukti
	 * masih menunggu verifikasi. Pasangan baliknya adalah
	 * {@link CicilanPembayaran#getBuktiPembayaran()} — dua kolom terpisah tanpa {@code mappedBy}.
	 */
	private CicilanPembayaran cicilanPembayaran;

	/**
	 * Id baris {@link ais.database.model.file.LampiranLain} yang memuat berkas bukti (foto slip,
	 * PDF, dsb). <b>Bukan FK</b> — sekadar angka, tanpa integritas referensial. Lihat pembahasan
	 * "dua mekanisme tautan berkas" pada Javadoc kelas.
	 */
	private Long idLampiran;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate dan kode UI. Seluruh properti dibiarkan
	 * {@code null} kecuali {@link #tanggal_dirubah} yang terisi waktu sekarang dari inisialisasi
	 * field.
	 */
	public BuktiPembayaran() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * @return id; {@code null} untuk object yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer. Praktis hanya dipakai Hibernate saat memuat baris; pada object baru
	 * nilainya diabaikan karena kolom {@code id} dipetakan {@code insertable = false}.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/nomor kwitansi setoran, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Pemangkasan terjadi <b>hanya pada nilai yang dikembalikan</b>; field tetap menyimpan teks
	 * asli, jadi getter ini bukan getter destruktif. Namun bila pemanggil membaca lalu menyimpan
	 * hasilnya kembali lewat {@link #setNama(String)}, versi terpangkaslah yang tersimpan.</p>
	 *
	 * @return nama setoran tanpa spasi tepi; {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/nomor kwitansi setoran. Tanpa validasi — kolomnya {@code NOT NULL} di
	 * database, sehingga menyimpan baris dengan nilai {@code null} akan gagal saat flush.
	 *
	 * @param nama nama setoran atau nama berkas bukti
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang setoran, apa adanya (tanpa pemangkasan).
	 *
	 * @return keterangan; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas tentang setoran.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal setoran menurut bukti.
	 *
	 * <p><b>Substitusi default saat baca, bukan destruktif.</b> Bila field {@code tanggal} masih
	 * {@code null}, method ini mengembalikan <i>tanggal hari ini</i> dari
	 * {@code ais.ui.util.WaktuUtil.getDate()} tanpa menuliskannya ke field. Baris yang tanggalnya
	 * {@code null} di database tetap {@code null} di sana.</p>
	 *
	 * <p>Dua akibat yang perlu disadari: (a) pemanggil tidak bisa membedakan "belum ada tanggal"
	 * dari "dibayar hari ini" lewat getter ini — periksa fieldnya lewat query bila perlu; dan
	 * (b) layar yang membaca nilai ini lalu menyimpannya kembali akan <i>membekukan</i> tanggal
	 * hari itu sebagai tanggal setoran, meski setoran aslinya terjadi jauh sebelumnya.</p>
	 *
	 * <p>Bandingkan dengan {@link CicilanPembayaranGagal#getTanggal()} yang benar-benar destruktif
	 * (menge-{@code null}-kan field); di sini tidak demikian.</p>
	 *
	 * @return tanggal setoran, atau tanggal hari ini bila belum diisi; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyetel tanggal setoran. Presisi yang disimpan hanya sampai hari
	 * ({@link TemporalType#DATE}); komponen jam pada {@link Date} yang diberikan akan dibuang oleh
	 * driver JDBC saat menulis.
	 *
	 * @param tanggal tanggal setoran; boleh {@code null}
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan mahasiswa pemilik setoran, dengan proxy lazy diselesaikan lebih dulu.
	 *
	 * <p><b>Getter yang menulis balik ke field:</b> hasil {@link GeneralValueObject#check(Object)}
	 * ditugaskan kembali ke {@code mahasiswa} supaya proxy yang sudah terselesaikan tidak
	 * diselesaikan ulang pada pemanggilan berikutnya. Penugasan ini menukar proxy dengan instance
	 * yang setara, jadi Hibernate tidak melihatnya sebagai perubahan nilai dan tidak menghasilkan
	 * {@code UPDATE} kolom {@code mahasiswa}.</p>
	 *
	 * <p>Biayanya bisa mahal bila object sudah detached: {@code check()} akan menempuh cache,
	 * session yang masih hidup, lalu — sebagai upaya terakhir — membuka sesi baru untuk membaca
	 * ulang entity. Hindari memanggilnya di dalam loop besar.</p>
	 *
	 * @return mahasiswa pemilik setoran; {@code null} bila setoran ini milik calon mahasiswa atau
	 *         belum dikaitkan ke siapa pun
	 * @see #getBiodataCalonMahasiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pemilik setoran. Karena {@code cascade = PERSIST, MERGE}, menyimpan
	 * bukti pembayaran ikut mem-persist/merge object mahasiswa yang belum terkelola.
	 *
	 * @param mahasiswa mahasiswa pemilik setoran; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan biodata calon mahasiswa pemilik setoran (jalur PMB), dengan proxy lazy
	 * diselesaikan lebih dulu.
	 *
	 * <p><b>Getter yang menulis balik ke field</b> — mekanisme dan implikasinya sama persis dengan
	 * {@link #getMahasiswa()}.</p>
	 *
	 * <p>Jalur unggah bukti bayar pendaftaran ({@code DoUpload},
	 * {@code pmb/CariDataPembayaranAction}) mengisi properti inilah, bukan {@link #getMahasiswa()},
	 * karena calon belum punya baris {@link Mahasiswa}.</p>
	 *
	 * @return biodata calon mahasiswa; {@code null} bila setoran milik mahasiswa aktif
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/**
	 * Menyetel biodata calon mahasiswa pemilik setoran.
	 *
	 * @param biodataCalonMahasiswa biodata calon mahasiswa; boleh {@code null}
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Mengembalikan jenis kegiatan yang membingkai setoran ini.
	 *
	 * <p>Melakukan <b>dua</b> hal sekaligus:</p>
	 *
	 * <ol>
	 *   <li><b>Menulis balik ke field:</b> {@code jenisKegiatan = check(jenisKegiatan)}
	 *       menyelesaikan proxy lazy, sama seperti getter relasi lainnya.</li>
	 *   <li><b>Substitusi default saat baca:</b> bila hasilnya {@code null}, yang dikembalikan
	 *       adalah {@link ConstantValues#PENDAFTARAN_MAHASISWA_LAMA} — nilai default itu
	 *       <b>tidak</b> ditulis ke field, sehingga kolom di database tetap {@code null} dan
	 *       tetap terlihat {@code null} bagi query HQL/SQL. Artinya nilai yang dilihat lapisan UI
	 *       bisa berbeda dari nilai yang dilihat query; jangan menyaring berdasarkan
	 *       {@code jenisKegiatan} dengan berharap default ini ikut terhitung.</li>
	 * </ol>
	 *
	 * <p>Perlu diwaspadai bahwa {@code ConstantValues.PENDAFTARAN_MAHASISWA_LAMA} adalah field
	 * statis yang di-<i>seed</i> saat aplikasi berjalan; bila belum terisi, method ini
	 * mengembalikan {@code null} juga.</p>
	 *
	 * @return jenis kegiatan setoran, atau default "pendaftaran mahasiswa lama" bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		jenisKegiatan = check(jenisKegiatan);
		return jenisKegiatan == null ? ConstantValues.PENDAFTARAN_MAHASISWA_LAMA : jenisKegiatan;
	}

	/**
	 * Menyetel jenis kegiatan setoran.
	 *
	 * @param jenisKegiatan jenis kegiatan; boleh {@code null} (getternya akan memakai default)
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * Mengembalikan semester akademik sasaran setoran, apa adanya.
	 *
	 * <p>Tanpa {@code @Column}, sehingga dipetakan ke kolom bernama {@code semester} oleh
	 * {@code MyNamingStrategy}.</p>
	 *
	 * @return nomor semester; boleh {@code null}
	 */
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Menyetel semester akademik sasaran setoran. Layar Bukti Pembayaran mengirim {@code 1} bila
	 * pengguna tidak memilih apa pun di combobox semester.
	 *
	 * @param semester nomor semester; boleh {@code null}
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan nilai rupiah setoran.
	 *
	 * <p><b>Substitusi default saat baca, bukan destruktif:</b> {@code null} dikembalikan sebagai
	 * {@code 0.0} tanpa menulis apa pun ke field. Dampaknya, bukti yang baru dibuat lewat unggahan
	 * portal (yang memang belum bernilai) tampil sebagai {@code Rp 0} di layar, bukan sebagai sel
	 * kosong — itulah tanda bahwa setoran masih menunggu petugas keuangan mengisi nilainya.</p>
	 *
	 * <p>Karena pemanggil tidak pernah menerima {@code null}, penjumlahan total di layar aman dari
	 * {@code NullPointerException}; sebaliknya, "belum diisi" dan "nol rupiah" menjadi tidak bisa
	 * dibedakan lewat getter ini.</p>
	 *
	 * @return nilai setoran, atau {@code 0.0} bila belum diisi; tidak pernah {@code null}
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nilai rupiah setoran. Tanpa validasi tanda maupun batas atas.
	 *
	 * @param nilai nilai setoran; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan cicilan resmi hasil posting setoran ini.
	 *
	 * <p><b>Berbeda dari getter relasi lain di kelas ini,</b> method ini <i>tidak</i> memanggil
	 * {@code check()} — tidak menulis balik ke field dan tidak menyelesaikan proxy sendiri. Itu
	 * aman karena relasinya tidak ditandai {@code LAZY} (default {@code ManyToOne} adalah
	 * {@code EAGER}) dan dipertegas {@code @Fetch(FetchMode.SELECT)}, sehingga Hibernate memuatnya
	 * lewat {@code SELECT} terpisah saat entity dibaca. Konsekuensinya: pada object yang sudah
	 * <i>detached</i> dan dimuat tanpa cicilan, getter ini mengembalikan apa adanya dan tidak
	 * berusaha memulihkan apa pun.</p>
	 *
	 * <p>Nilai {@code null} berarti setoran belum diverifikasi/di-posting, dan itulah predikat yang
	 * dipakai sebagai <b>antrean verifikasi</b>: {@code BuktiPembayaranAction.ambilBukti(...)} dan
	 * {@code AmbilDataBuktiPembayaranBanyak} menyaring dengan
	 * {@code Restrictions.isNull("cicilanPembayaran")}. Yang menutup siklusnya adalah
	 * {@code CommonPaymentHelper}: setelah cicilan tersimpan, helper itu memanggil
	 * {@link #setCicilanPembayaran(CicilanPembayaran)} lalu {@code Common.refreshUpdate(...)},
	 * sehingga bukti hilang dari antrean dan tombolnya di grid berubah dari "Validasi" menjadi
	 * "Cetak". Pasangan baliknya
	 * {@link CicilanPembayaran#getBuktiPembayaran()} disimpan pada kolom terpisah di tabel
	 * cicilan; keduanya harus dijaga konsisten oleh kode pemanggil. Bila baris cicilan dipindahkan
	 * ke {@link CicilanPembayaranGagal}, sisi cicilan kehilangan penunjuknya sementara kolom di
	 * sini bisa tetap menunjuk ke id cicilan yang sudah tidak ada lagi di tabel
	 * {@code cicilan_pembayaran}.</p>
	 *
	 * @return cicilan pembayaran hasil posting; {@code null} bila belum di-posting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "cicilan_pembayaran", nullable = true)
	public CicilanPembayaran getCicilanPembayaran() {
		return cicilanPembayaran;
	}

	/**
	 * Menyetel cicilan resmi hasil posting setoran ini. Pemanggil bertanggung jawab menyetel sisi
	 * baliknya lewat {@link CicilanPembayaran#setBuktiPembayaran(BuktiPembayaran)} — tidak ada
	 * sinkronisasi otomatis.
	 *
	 * @param cicilanPembayaran cicilan hasil posting; boleh {@code null}
	 */
	public void setCicilanPembayaran(CicilanPembayaran cicilanPembayaran) {
		this.cicilanPembayaran = cicilanPembayaran;
	}

	/**
	 * Mengembalikan pos/komponen biaya yang dibayar, dengan proxy lazy diselesaikan lebih dulu.
	 *
	 * <p><b>Getter yang menulis balik ke field</b> — sama seperti {@link #getMahasiswa()}.</p>
	 *
	 * <p>Pada layar Bukti Pembayaran, item biaya dilekatkan per baris rincian: satu setoran besar
	 * yang mencakup beberapa pos akan menghasilkan <i>beberapa</i> baris {@code BuktiPembayaran},
	 * masing-masing dengan {@code itemBiaya} dan {@code nilai} sendiri.</p>
	 *
	 * @return item biaya sasaran setoran; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);
		return itemBiaya;
	}

	/**
	 * Menyetel pos/komponen biaya yang dibayar.
	 *
	 * @param itemBiaya item biaya; boleh {@code null}
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Mengembalikan id berkas lampiran bukti bayar.
	 *
	 * <p>Nilainya menunjuk ke {@code LampiranLain.id} tetapi <b>bukan FK</b> dan tidak dipetakan
	 * sebagai relasi — sekadar angka. Tidak ada jaminan barisnya masih ada, dan tidak ada
	 * {@code ON DELETE} yang mengurusnya. Getter ini murni membaca field, tanpa efek samping.</p>
	 *
	 * <p>Nilai inilah yang disalin {@link CicilanPembayaran#getIdLampiran()} ke fieldnya sendiri
	 * ketika cicilan punya {@code buktiPembayaran} berlampiran; karena penyalinan itu terjadi di
	 * sisi cicilan, tabel ini tetap menjadi sumber kebenarannya.</p>
	 *
	 * <p><b>Peringatan keamanan.</b> Nilai ini dikirim ke servlet unduh dengan
	 * {@code usingId = true}, yang membuat pencarian barisnya menyusut menjadi pencocokan id murni
	 * pada tabel {@code lampiran_lain} — tanpa penyaringan {@code jenis} maupun pemeriksaan
	 * kepemilikan. Karena id-nya berurutan, jangan pernah memperlakukan angka ini sebagai rahasia
	 * atau sebagai kontrol akses. Lihat bagian "Catatan keamanan" pada Javadoc kelas.</p>
	 *
	 * @return id baris {@link ais.database.model.file.LampiranLain}; {@code null} bila tidak ada
	 *         berkas yang diunggah
	 */
	public Long getIdLampiran() {
		return idLampiran;
	}

	/**
	 * Menyetel id berkas lampiran bukti bayar. Diisi setelah baris {@code LampiranLain} tersimpan
	 * dan id-nya diketahui — lihat {@code ais/action/servlet/DoUpload.java} dan
	 * {@code ais/action/master/BuktiPembayaranAction.java}.
	 *
	 * @param idLampiran id baris lampiran; boleh {@code null}
	 */
	public void setIdLampiran(Long idLampiran) {
		this.idLampiran = idLampiran;
	}

	/**
	 * Mengembalikan cara pembayaran setoran, dengan proxy lazy diselesaikan lebih dulu.
	 *
	 * <p><b>Getter yang menulis balik ke field</b> — sama seperti {@link #getMahasiswa()}.</p>
	 *
	 * <p>Properti ini penting di luar tampilan: {@link CicilanPembayaran#getJenisPembayaran()}
	 * <i>mewarisi</i> nilai dari sini bila cicilannya sendiri belum menentukan cara bayar, dan
	 * setiap {@link JenisPembayaran} memetakan ke satu {@code Akun} sehingga pilihan di sini ikut
	 * menentukan pos akunting setoran.</p>
	 *
	 * <p>Berbeda dari {@link #getJenisKegiatan()}, tidak ada nilai default saat baca di sini —
	 * {@code null} dikembalikan apa adanya. Layar Bukti Pembayaran-lah yang mengisikan
	 * {@link JenisPembayaran} berflag {@code defaultPembayaran} saat form dibuka.</p>
	 *
	 * @return cara pembayaran; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pembayaran", nullable = true)
	public JenisPembayaran getJenisPembayaran() {
		jenisPembayaran = check(jenisPembayaran);
		return jenisPembayaran;
	}

	/**
	 * Menyetel cara pembayaran setoran.
	 *
	 * @param jenisPembayaran cara pembayaran; boleh {@code null}
	 */
	public void setJenisPembayaran(JenisPembayaran jenisPembayaran) {
		this.jenisPembayaran = jenisPembayaran;
	}

	/**
	 * Mengembalikan baris pengaturan pembayaran bulanan yang menjadi sasaran setoran.
	 *
	 * <p>Seperti {@link #getCicilanPembayaran()}, method ini <b>tidak</b> memanggil {@code check()}
	 * — relasinya {@code EAGER} dengan {@code @Fetch(FetchMode.SELECT)}, jadi Hibernate sudah
	 * memuatnya lewat {@code SELECT} terpisah. Tidak ada penulisan balik ke field dan tidak ada
	 * efek samping.</p>
	 *
	 * <p>Terisi hanya untuk setoran yang menyasar tagihan bulanan (SPP per bulan). Untuk setoran
	 * biaya non-bulanan, sasarannya diwakili {@link #getItemBiaya()} saja.</p>
	 *
	 * @return pengaturan pembayaran bulanan sasaran; {@code null} bila setoran bukan tagihan
	 *         bulanan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengaturan_pembayaran_bulanan", nullable = true)
	public PengaturanPembayaranBulanan getPengaturanPembayaranBulanan() {
		return pengaturanPembayaranBulanan;
	}

	/**
	 * Menyetel baris pengaturan pembayaran bulanan sasaran setoran.
	 *
	 * @param pengaturanPembayaranBulanan pengaturan pembayaran bulanan; boleh {@code null}
	 */
	public void setPengaturanPembayaranBulanan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		this.pengaturanPembayaranBulanan = pengaturanPembayaranBulanan;
	}

}
