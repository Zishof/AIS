package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

/**
 * Tabel silang (<i>join table</i>) yang mengikat sebuah {@link PaketPsb} pada sebuah
 * {@link GelombangPendaftaranPsb}, dipetakan ke tabel
 * {@code sekolah.paket_psb_punya_gelombang_pendaftaran_psb}.
 *
 * <p>Entity ini <b>tidak menyimpan atribut bisnis apa pun</b> — hanya sepasang kunci asing wajib
 * ({@code paket_psb}, {@code gelombang_pendaftaran_psb}) plus jejak audit warisan. Seluruh
 * maknanya terletak pada <i>ada atau tidaknya</i> baris.</p>
 *
 * <h3>Peran yang TERVERIFIKASI dari kode</h3>
 * <ol>
 * <li><b>Sisi tulis — layar Gelombang Pendaftaran PSB.</b> Satu-satunya penulis tabel ini adalah
 * {@code ais.action.master.sekolah.GelombangPendaftaranPsbAction}. Isian pengikatnya berupa
 * checkbox berlabel <i>"Gelombang pendaftaran ini hanya berlaku untuk paket tertentu"</i>; bila
 * dicentang, muncul grid satu kolom berjudul <i>"Pilih Paket"</i> berisi satu checkbox per
 * {@link PaketPsb} yang lolos saring tenant. Setiap paket yang tercentang menjadi satu baris
 * entity ini. Jadi <b>arah kepemilikan relasi bersifat "dari sisi gelombang"</b>: gelombang-lah
 * yang memilih paket, bukan sebaliknya.</li>
 * <li><b>Sisi baca — formulir PPDB publik dan entri calon siswa.</b>
 * {@code ais.action.master.sekolah.CalonSiswaAction} serta seluruh varian formulir PPDB
 * ({@code PPDB1}, {@code PPDB2}, {@code PPDB3}, {@code PPDB_Alumni},
 * {@code PPDB_Simple} … {@code PPDB_Simple8}) menjalankan kriteria yang sama persis:
 * ambil {@code groupProperty("paketPsb.id")} untuk gelombang yang sedang dipilih, lalu pakai
 * daftar id itu sebagai {@code Restrictions.in("id", …)} saat mengisi combobox
 * <i>"Pilih Paket"</i>. Bila daftarnya kosong dipakai {@code Restrictions.sqlRestriction("false")}
 * dan baris combo di-{@code setVisible(false)}.</li>
 * </ol>
 *
 * <h3>Semantik "tidak ada baris" = TANPA pembatasan (bukan "tidak ada paket")</h3>
 * Ini titik paling mudah salah tafsir. Untuk sebuah gelombang:
 * <ul>
 * <li><b>Nol baris</b> &rarr; isian "Paket" <b>disembunyikan seluruhnya</b> dari formulir
 * pendaftaran, dan {@code CalonSiswa#paketPsb} dibiarkan {@code null}. Gelombang itu tidak
 * membatasi paket — bukan berarti pendaftar kehilangan pilihan, melainkan pertanyaannya memang
 * tidak pernah diajukan.</li>
 * <li><b>Satu baris atau lebih</b> &rarr; isian "Paket" muncul, wajib diisi (label
 * <i>"Paket *"</i>), dan pilihannya <b>hanya</b> paket-paket yang terdaftar di sini. Baris entity
 * ini berfungsi sebagai <i>whitelist</i>.</li>
 * </ul>
 * Konsekuensi operasional: menghapus baris terakhir bukan mengunci gelombang, tetapi justru
 * <b>membuka</b>-nya (isian paket hilang, tidak ada lagi penyaringan).
 *
 * <h3>Siklus tulis: HAPUS TOTAL lalu SISIP ULANG</h3>
 * {@code GelombangPendaftaranPsbAction#onSave(...)} tidak melakukan sinkronisasi selisih. Setiap
 * kali gelombang disimpan, urutannya:
 * <ol>
 * <li>{@code session.flush()} atas gelombang itu sendiri;</li>
 * <li>native SQL {@code "delete from sekolah.paket_psb_punya_gelombang_pendaftaran_psb where
 * gelombang_pendaftaran_psb=" + id} &mdash; menghapus SELURUH baris milik gelombang tersebut;</li>
 * <li>iterasi peta pilihan di memori, {@code setGelombangPendaftaranPsb(...)} lalu
 * {@code session.save(...)} + {@code flush()} untuk tiap paket yang tercentang.</li>
 * </ol>
 * Empat akibat yang wajib diketahui:
 * <ul>
 * <li><b>{@code id} tidak stabil.</b> Baris yang "tidak berubah" pun tetap dihapus dan disisip
 * ulang, sehingga mendapat {@code id} baru pada tiap penyimpanan gelombang. Jangan pernah memakai
 * {@code id} entity ini sebagai kunci eksternal, referensi lampiran, atau anchor laporan.</li>
 * <li><b>Jejak Envers bolong.</b> Kelas ini {@code @Audited}, tetapi penghapusan dikerjakan lewat
 * <i>native SQL</i> yang tidak melewati listener Envers. Tabel bayangan revisi karenanya hanya
 * pernah mencatat INSERT, tidak pernah DELETE — riwayat audit menyiratkan pengikatan lama masih
 * hidup padahal barisnya sudah lenyap.</li>
 * <li><b>Kegagalan disembunyikan.</b> Seluruh blok hapus-sisip dibungkus {@code try/catch} yang
 * hanya memanggil {@code Common.tampilErrorJikaAdmin(e)}. Bagi pengguna non-admin, kegagalan
 * pengikatan paket tampak seperti penyimpanan yang berhasil.</li>
 * <li><b>Ketergantungan tersembunyi pada session-per-request.</b> Baris lama dimuat pada request
 * ZK yang berbeda (listener {@code onChange}/timer) dari request klik Simpan, sehingga saat
 * disimpan objeknya sudah <i>detached</i> dan {@code session.save()} benar-benar melakukan INSERT
 * baru. Seandainya pemuatan dan penyimpanan suatu saat terjadi dalam satu session yang sama,
 * {@code session.save()} atas instance yang masih <i>persistent</i> menjadi no-op sementara baris
 * fisiknya sudah dihapus native SQL — seluruh pengikatan paket akan hilang tanpa pesan apa pun.
 * Ini bom waktu yang perlu diingat bila mekanisme session di layar itu pernah diubah.</li>
 * </ul>
 *
 * <h3>Duplikat: dicegah di memori, tidak di basis data</h3>
 * Tidak ada <i>unique constraint</i> pada pasangan ({@code paket_psb},
 * {@code gelombang_pendaftaran_psb}) — baik di anotasi kelas ini maupun di
 * {@code hibernate.cfg.xml}. Pembacaannya pun defensif:
 * {@code GelombangPendaftaranPsbAction} menampung hasil query ke
 * {@code HashMap<Long, PaketPsbPunyaGelombangPendaftaranPsb>} berkunci {@code paketPsb.getId()}
 * dan melewati entri yang kuncinya sudah ada, sedangkan sisi PPDB memakai
 * {@code Projections.groupProperty("paketPsb.id")} (GROUP BY) untuk meredam pengulangan. Kedua
 * penjaga itu adalah petunjuk kuat bahwa baris kembar memang bisa terbentuk di lapangan; efeknya
 * saat ini hanya sampah data, bukan salah tampil.
 *
 * <h3>Cakupan tenant</h3>
 * Entity ini <b>tidak punya</b> relasi {@code sekolah}/{@code yayasan} sendiri; cakupan tenantnya
 * sepenuhnya diturunkan dari gelombang induknya. Daftar paket yang boleh dicentang disaring
 * {@code isNull("yayasan") OR eq("yayasan", y)} dan {@code isNull("sekolah") OR eq("sekolah", s)}
 * terhadap yayasan/sekolah yang sedang terpilih di formulir — pola "global atau milik sendiri"
 * yang benar, bukan fail-open.
 *
 * <h3>Verifikasi pola arsitektur berulang milik repo ini</h3>
 * <ul>
 * <li><b>Broken access control (nol {@code checkPrevilages})</b> &mdash; <b>TIDAK ADA. Ini BUKAN
 * instance ke-5 keluarga PSB.</b> Diverifikasi langsung pada satu-satunya Action pengelolanya:
 * {@code GelombangPendaftaranPsbAction#doBeforeCompose(...)} memanggil
 * {@code Common.doCheckSecurity()}, dan {@code doAfterCompose(...)} menggerbangi
 * {@code add.setVisible(CommonPrivilages.checkPrevilages(CREATE))},
 * {@code edit = checkPrevilages(UPDATE)}, {@code delete = checkPrevilages(DELETE)}; tombol per
 * baris dipasang lewat {@code Common.copyEditDeleteButtons(edit, delete, …)}, checkbox "Aktif" di
 * grid memakai {@code setDisabled(!edit)}, dan tombol unggah massal disembunyikan kecuali
 * CREATE+UPDATE+DELETE dimiliki sekaligus. Rantai empat entity PSB tanpa privilese
 * ({@code RuangPSB}, {@code CalonSiswaPunyaVerifikasiParameter},
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa}/{@code InterviewPunyaCalonSiswa},
 * {@code RuangGelombangPendaftaranPsbPSB}) <b>tidak bertambah</b> lewat berkas ini.</li>
 * <li><b>Pewarisan hak lewat menu induk</b> &mdash; <b>ADA, tetapi bukan atas tabel ini.</b>
 * Layar yang sama juga menyisipkan {@code /pages/master/sekolah/paket_psb.zul} sebagai tab lewat
 * {@code GelombangPendaftaranPsbAction#onPaket(Event)}; halaman itu tidak pernah terdaftar sebagai
 * menu mandiri, sehingga hak CRUD master {@link PaketPsb} sesungguhnya diwarisi dari menu
 * <b>Gelombang Pendaftaran PSB</b>. Baca {@link PaketPsb} untuk uraian lengkapnya. Untuk entity
 * penghubung ini sendiri pewarisan itu justru <i>tepat sasaran</i>: hak menulis pengikatan paket
 * memang seharusnya hak atas gelombang.</li>
 * <li><b>Getter destruktif / write-back</b> &mdash; <b>ADA, versi ringan.</b> Kedua getter relasi
 * menulis balik hasil {@code check(...)} ke fieldnya ({@code paketPsb = check(paketPsb)}). Ini
 * pola de-proxy standar {@link GeneralValueObject}, bukan penurunan ulang nilai seperti
 * {@code PaketPsb#getYayasan()} — tidak ada nilai yang bisa "hilang" karenanya.</li>
 * <li><b>Penciutan {@code TreeSet}</b> &mdash; <b>LATEN, bukan aktif.</b> Kelas ini tidak
 * meng-override {@code compareTo}/{@code equals}/{@code hashCode}, dan tidak meng-override
 * {@code getNomorUrut()}. Pada {@link GeneralValueObject#compareTo(GeneralValueObject)} urutan
 * pemeriksaannya {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr;
 * {@code keterangan}: tiga yang pertama selalu {@code null} untuk entity ini (fieldnya tidak
 * pernah diisi siapa pun), sedangkan {@code getKeterangan()} induk <b>menormalkan {@code null}
 * menjadi {@code ""}</b> sehingga cabang terakhir selalu memenuhi syarat dan menghasilkan
 * {@code "".compareTo("")} = 0. Artinya SEMUA instance saling "sama" bagi {@code TreeSet}/
 * {@code TreeMap} — satu koleksi terurut hanya akan menyimpan SATU baris. Saat ini tidak
 * berbahaya karena seluruh pemanggil memakai {@code List} atau {@code HashMap} berkunci
 * {@code Long}; catatan ini ada agar tidak ada yang "merapikan" kodenya menjadi {@code TreeSet}.
 * Bandingkan dengan penciutan yang benar-benar aktif pada
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa}.</li>
 * <li><b>Fail-open cakupan tenant</b> &mdash; <b>TIDAK ADA</b> pada jalur baca/tulis entity ini
 * (lihat bagian "Cakupan tenant").</li>
 * <li><b>SQL injection</b> &mdash; <b>TIDAK ADA.</b> Native SQL penghapusan memang dirangkai
 * dengan penyambungan string, tetapi yang disambung adalah
 * {@code gelombangPendaftaranPsb.getId()} bertipe {@code Long} — tidak ada masukan pengguna
 * berupa teks yang masuk ke pernyataan itu.</li>
 * <li><b>Kolom {@code aktif} tak pernah ditulis</b> &mdash; <b>TIDAK BERLAKU.</b> Entity ini tidak
 * punya kolom {@code aktif}; "aktif/tidak" sebuah pengikatan sepenuhnya ditentukan oleh ada
 * tidaknya baris.</li>
 * </ul>
 *
 * <h3>Kuirk UI yang perlu diketahui</h3>
 * Setelah sebuah gelombang punya minimal satu pengikatan paket, checkbox
 * <i>"…hanya berlaku untuk paket tertentu"</i> di-{@code setDisabled(true)} bersamaan dengan
 * {@code setChecked(true)}. Pembatasan tidak bisa dimatikan lewat checkbox itu lagi; satu-satunya
 * cara kembali ke kondisi "tanpa pembatasan" adalah membuka centang seluruh paket di grid
 * <i>"Pilih Paket"</i> lalu menyimpan, sehingga siklus hapus-sisip menghapus semua baris dan tidak
 * menyisipkan apa pun.
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Jejak audit warisan</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> &mdash; {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 * constructor {@link #PaketPsbPunyaGelombangPendaftaranPsb()}.</li>
 * <li><b>Kedua sisi relasi</b> &mdash; {@link #getPaketPsb()}/{@link #setPaketPsb(PaketPsb)},
 * {@link #getGelombangPendaftaranPsb()}/{@link
 * #setGelombangPendaftaranPsb(GelombangPendaftaranPsb)}.</li>
 * </ul>
 * Tidak ada method bisnis, query statis, maupun helper perhitungan di kelas ini.
 *
 * <h3>Persistensi</h3>
 * {@code @Entity} dengan {@code dynamicInsert}/{@code dynamicUpdate} dan {@code @Audited}. Karena
 * {@code @Id} dipasang pada <i>getter</i>, Hibernate memakai <b>akses properti</b>: semua getter
 * di kelas ini (termasuk pemanggilan {@code check(...)}) benar-benar dijalankan saat entity
 * ditulis. Kedua {@code @JoinColumn} bersifat {@code nullable = false} — baris penghubung tanpa
 * salah satu sisi tidak akan pernah bisa disimpan. {@code @ManyToOne} keduanya
 * {@code FetchType.LAZY} dengan {@code cascade = PERSIST, MERGE}; perlu dicatat bahwa jalur simpan
 * yang benar-benar dipakai adalah {@code session.save(...)} (aksi {@code SAVE_UPDATE} pada
 * Hibernate 3) yang <b>tidak</b> mengaktifkan gaya cascade {@code persist}/{@code merge}, sehingga
 * menyimpan baris penghubung tidak pernah ikut membuat paket atau gelombang baru — keduanya wajib
 * sudah tersimpan lebih dulu.
 *
 * @see GeneralValueObject
 * @see PaketPsb
 * @see GelombangPendaftaranPsb
 * @see CalonSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "paket_psb_punya_gelombang_pendaftaran_psb")
public class PaketPsbPunyaGelombangPendaftaranPsb extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai ini berasal dari berkas template generator yang sama dengan entity lain di paket
	 * ini (bandingkan {@code PaketPsb} yang memakai {@code 2463821577548439808L}). Tidak
	 * berpengaruh pada pemetaan Hibernate.</p>
	 */
	private static final long serialVersionUID = 1463822577548439808L;
	/**
	 * Kunci utama baris, dipetakan ke kolom {@code id}. Lihat {@link #getId()}.
	 *
	 * <p>Bersifat <b>fana</b>: siklus hapus-sisip pada penyimpanan gelombang memberi nilai baru
	 * setiap kali (lihat Javadoc kelas).</p>
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>validasi non-trivial</b>: argumen
	 * {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b> (method langsung
	 * {@code return} tanpa menyentuh field).
	 *
	 * <p>Akibatnya jejak audit hanya bisa diisi atau ditimpa, tidak pernah bisa dikosongkan
	 * kembali lewat setter ini. Perilakunya sama persis dengan {@link #setOleh(String)} dan
	 * dengan pola yang dipakai seluruh entity turunan {@link GeneralValueObject}.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>validasi non-trivial</b> yang sama
	 * dengan {@link #setOlehId(String)}: argumen {@code null} atau kosong/spasi diabaikan
	 * diam-diam sehingga jejak audit tidak dapat dihapus.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence TEPAT SEBELUM pernyataan
	 * UPDATE dikirim, dan meneruskan objek ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif serta memperbarui
	 * {@code tanggal_dirubah}.
	 *
	 * <p><b>Praktis tidak pernah berjalan untuk entity ini.</b> Baris penghubung tidak pernah
	 * di-UPDATE oleh kode mana pun: layar pengelola selalu menghapus seluruh baris milik sebuah
	 * gelombang lalu menyisipkannya kembali (lihat Javadoc kelas). Konsekuensinya kolom
	 * {@code oleh}/{@code oleh_id} pada tabel ini praktis selalu kosong, karena tidak ada
	 * {@code @PrePersist} yang mengisinya saat INSERT.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} yang diberi nilai awal
	 * {@code ais.ui.util.WaktuUtil.getDate()} — hasil penyisipan otomatis oleh perkakas migrasi.
	 * Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi: nilainya diisi Hibernate saat hidrasi baris, atau
	 * oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan sebagai kolom
	 * {@code TIMESTAMP}.
	 *
	 * <p>Karena baris entity ini selalu disisipkan baru dan tidak pernah di-UPDATE, nilainya
	 * efektif adalah <b>waktu objek Java dibuat</b> — yaitu saat grid "Pilih Paket" dirender,
	 * bukan saat tombol Simpan ditekan. Untuk baris yang objeknya dipakai ulang lintas
	 * penyimpanan, selisihnya bisa mencapai berapa pun lamanya dialog dibuka.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         constructor Java
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: {@code paketPsb + "_" + gelombangPendaftaranPsb}, mis.
	 * {@code "12-Reguler_Gelombang 1"} ({@code PaketPsb#toString()} menghasilkan
	 * {@code id + "-" + nama}, sedangkan {@link GelombangPendaftaranPsb} mewarisi
	 * {@code GeneralValueObject#toString()} yang efektif hanya menampilkan namanya).
	 *
	 * <p><b>Method ini punya EFEK SAMPING dan bisa menyentuh basis data.</b> Dua baris pertamanya
	 * memanggil {@link #getPaketPsb()} dan {@link #getGelombangPendaftaranPsb()} semata-mata untuk
	 * efek sampingnya — kedua getter itu menjalankan {@code check(...)} yang meresolusi proxy
	 * lazy, bila perlu dengan membaca ulang entity dari cache atau dari session baru. Barulah
	 * setelah itu nilai <b>field</b>-nya (bukan getter) dirangkai. Urutan ini disengaja: tanpa
	 * pemanggilan getter di depan, perangkaian field mentah dapat menghasilkan
	 * {@code LazyInitializationException} atau teks proxy.</p>
	 *
	 * <p>Karena itu jangan memanggil {@code toString()} entity ini di dalam loop panas, di dalam
	 * blok logging yang selalu aktif, atau setelah session ditutup.</p>
	 *
	 * @return gabungan representasi paket dan gelombang, dipisah garis bawah
	 */
	public String toString() {
		getPaketPsb();
		getGelombangPendaftaranPsb();
		return paketPsb + "_" + gelombangPendaftaranPsb;
	}

	/** Sisi paket dari pasangan. Wajib terisi. Lihat {@link #getPaketPsb()}. */
	private PaketPsb paketPsb;

	/**
	 * Sisi gelombang dari pasangan; sekaligus pemilik logis baris ini (seluruh operasi tulis
	 * dilakukan per-gelombang). Wajib terisi. Lihat {@link #getGelombangPendaftaranPsb()}.
	 */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;

	/**
	 * Constructor default tanpa argumen.
	 *
	 * <p>WAJIB ada karena Hibernate membutuhkannya untuk membuat instance saat hidrasi baris.
	 * Dipakai juga secara langsung oleh {@code GelombangPendaftaranPsbAction} saat merender grid
	 * "Pilih Paket": setiap paket yang belum terikat mendapat satu objek kosong yang langsung
	 * di-{@code setPaketPsb(...)} dan baru akan disimpan bila checkbox-nya tercentang saat
	 * gelombang disimpan.</p>
	 */
	public PaketPsbPunyaGelombangPendaftaranPsb() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolomnya dipetakan {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code GenerationType.IDENTITY} / kolom serial PostgreSQL).</p>
	 *
	 * <p>Ingat bahwa nilai ini <b>tidak stabil lintas penyimpanan</b> — lihat Javadoc kelas.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
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
	 * <p>Umumnya hanya dipanggil Hibernate. Menyetelnya manual pada objek yang akan di-{@code save}
	 * tidak menghalangi INSERT: dengan strategi {@code IDENTITY}, {@code Session#save(...)} atas
	 * objek <i>detached</i> tetap menyisipkan baris baru dan menimpa id dengan nilai dari basis
	 * data.</p>
	 *
	 * @param id nilai kunci utama yang baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan {@link PaketPsb} yang diikat oleh baris ini, sesudah proxy lazy-nya
	 * diresolusi.
	 *
	 * <p><b>Menulis balik ke field.</b> Hasil {@code check(...)} ditugaskan kembali ke
	 * {@code paketPsb} sebelum dikembalikan — pola de-proxy standar {@link GeneralValueObject}
	 * yang memastikan pembacaan berikutnya (termasuk perangkaian field di {@link #toString()})
	 * memperoleh objek nyata, bukan proxy. Bila keempat tahap resolusi {@code check(...)} gagal,
	 * nilai lama dikembalikan apa adanya, jadi method ini tidak pernah melempar.</p>
	 *
	 * <p>Dibaca terutama oleh {@code GelombangPendaftaranPsbAction} (untuk mencocokkan baris
	 * tersimpan dengan checkbox paket) dan oleh {@link #toString()}. Sisi PPDB tidak memanggil
	 * getter ini melainkan memproyeksikan {@code paketPsb.id} langsung di tingkat SQL.</p>
	 *
	 * @return paket yang diikat; tidak pernah {@code null} untuk baris yang berasal dari basis
	 *         data (kolomnya {@code nullable = false}), tetapi bisa {@code null} pada objek yang
	 *         baru dibuat dan belum di-{@code setPaketPsb(...)}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket_psb", nullable = false)
	public PaketPsb getPaketPsb() {
		paketPsb = check(paketPsb);
		return paketPsb;
	}

	/**
	 * Menyetel paket yang diikat baris ini. Tanpa validasi.
	 *
	 * <p>Dipanggil {@code GelombangPendaftaranPsbAction} untuk SETIAP paket yang dirender di grid
	 * "Pilih Paket" — termasuk paket yang checkbox-nya tidak tercentang — sehingga objek sudah
	 * siap pakai bila pengguna mencentangnya. Objek yang tidak pernah tercentang tidak pernah
	 * ikut disimpan.</p>
	 *
	 * @param paketPsb paket yang diikat; harus sudah tersimpan (cascade {@code PERSIST}/
	 *                 {@code MERGE} tidak aktif pada jalur {@code Session#save(...)} yang dipakai)
	 */
	public void setPaketPsb(PaketPsb paketPsb) {
		this.paketPsb = paketPsb;
	}

	/**
	 * Mengembalikan {@link GelombangPendaftaranPsb} yang diikat oleh baris ini, sesudah proxy
	 * lazy-nya diresolusi.
	 *
	 * <p>Perilakunya identik dengan {@link #getPaketPsb()}, termasuk penulisan balik hasil
	 * {@code check(...)} ke field.</p>
	 *
	 * @return gelombang yang diikat; tidak pernah {@code null} untuk baris yang berasal dari basis
	 *         data (kolomnya {@code nullable = false})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb", nullable = false)
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menyetel gelombang yang diikat baris ini. Tanpa validasi.
	 *
	 * <p>Disetel paling akhir, tepat sebelum {@code session.save(...)} di
	 * {@code GelombangPendaftaranPsbAction#onSave(...)}. Alasannya: objek baris dibuat lebih dulu
	 * saat grid dirender, ketika gelombang baru mungkin belum punya {@code id}. Barulah setelah
	 * gelombang tersimpan dan ter-{@code flush}, seluruh baris terpilih diberi referensi
	 * gelombang lalu disisipkan.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang yang diikat; harus sudah tersimpan
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

}
