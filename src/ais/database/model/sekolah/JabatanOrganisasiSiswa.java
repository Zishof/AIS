package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>jabatan yang diemban siswa di dalam sebuah organisasi siswa</b>, dipetakan ke
 * tabel {@code sekolah.jabatan_organisasi_siswa}. Satu baris mewakili satu nama jabatan
 * kepengurusan &mdash; misalnya "Ketua", "Wakil Ketua", "Sekretaris", "Bendahara", atau "Anggota"
 * &mdash; yang kemudian dipasangkan ke keanggotaan seorang siswa pada satu organisasi lewat
 * {@link OrganisasiSiswaPunyaSiswa}.
 *
 * <h2>Isi tabel: teks bebas, tanpa data bawaan</h2>
 *
 * <p><b>Contoh nama jabatan di atas TIDAK berasal dari kode.</b> Penelusuran seluruh repo tidak
 * menemukan satu pun blok penyemaian (<i>seed</i>) untuk tabel ini: tidak ada di
 * {@code ais.common.InitDataHelper}, tidak ada penyalinan otomatis dari padanan PT, dan tidak ada
 * skrip SQL pemasangan yang mengisinya. Berbeda dari banyak katalog master lain di paket ini, entity
 * ini juga <b>tidak</b> punya kolom kode referensi (tidak ada {@code kode} maupun {@code feeder}),
 * sehingga isinya sepenuhnya teks bebas yang diketik operator lewat layarnya. Konsekuensi praktis:
 * pada instalasi baru tabel ini <b>kosong</b>, dan combobox "Jabatan" di layar keanggotaan organisasi
 * tampil tanpa pilihan sampai ada yang mengisinya lebih dahulu.</p>
 *
 * <h2>Relasi: satu arah, foreign key ada di pihak lain</h2>
 *
 * <p>Class ini tidak memegang koleksi maupun foreign key apa pun. Satu-satunya pemakainya sebagai
 * relasi adalah {@link OrganisasiSiswaPunyaSiswa}, lewat properti
 * {@code OrganisasiSiswaPunyaSiswa.getJabatanOrganisasiSiswa()} yang dipetakan ke kolom
 * {@code jabatan_organisasi_siswa} dan bersifat <b>opsional</b> ({@code nullable = true}) &mdash;
 * jadi seorang siswa boleh terdaftar sebagai anggota organisasi tanpa jabatan tertentu.</p>
 *
 * <p>Karena arah relasinya dari anak ke induk dan tidak ada <i>cascade</i>, menghapus satu baris di
 * sini <b>tidak</b> menghapus keanggotaan mana pun: database yang menolak lewat constraint foreign
 * key, lalu tombol Hapus di layarnya menangkap kegagalan itu dan menampilkannya sebagai pesan
 * "berelasi dengan data lainnya".</p>
 *
 * <h2>Kembaran struktural di modul lain</h2>
 *
 * <p>Berkas ini adalah anggota ketiga dari satu triplet dengan bentuk identik &mdash; field,
 * anotasi, dan tata letak baris yang sama persis, hanya berbeda schema/tabel:
 * {@code ais.database.model.JabatanOrganisasiDosen} ({@code public.jabatan_organisasi_dosen}),
 * {@code ais.database.model.JabatanOrganisasiIntraKampus}
 * ({@code public.jabatan_organisasi_intra_kampus}), dan class ini untuk sisi sekolah. Ketiganya
 * <b>tidak</b> saling berelasi maupun saling menyemai; tiap modul mengelola daftarnya sendiri.
 * Salinan sekolah ini adalah yang paling tertinggal secara kode: layarnya masih memakai
 * {@code MyMessageboxConfig} polos, sementara dua kembarannya sudah dimodernkan ke
 * {@code PesanFormalHelper}.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ul>
 *   <li><b>Identitas &amp; isi</b> &mdash; {@link #getId()}/{@link #setId(Long)},
 *       {@link #getNama()}/{@link #setNama(String)},
 *       {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()};</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()};</li>
 *   <li><b>Konstruksi</b> &mdash; {@link #JabatanOrganisasiSiswa()}.</li>
 * </ul>
 *
 * <p>Perhatikan apa yang <b>tidak ada</b>: tidak ada kolom {@code aktif}, tidak ada
 * {@code nomorUrut}, tidak ada {@code kode}/{@code feeder}, dan tidak ada kolom cakupan
 * {@code sekolah} maupun {@code yayasan}. Dua konsekuensi yang sudah diverifikasi:</p>
 *
 * <ul>
 *   <li>pola bug "kolom {@code aktif} tak pernah ditulis" yang berulang di entity bersaudara
 *       <b>tidak berlaku sama sekali</b> di berkas ini, karena kolomnya memang tidak ada;</li>
 *   <li>pola "penciutan {@link java.util.TreeSet}" akibat {@code getNomorUrut()} yang di-override
 *       menjadi tidak pernah {@code null} juga <b>tidak berlaku</b>: class ini tidak meng-override
 *       {@code getNomorUrut()}, sehingga nilainya selalu {@code null} dan
 *       {@link GeneralValueObject#compareTo(GeneralValueObject)} jatuh ke cabang
 *       {@code nama} (yang di sini {@code nullable = false} sekaligus {@code unique = true}).
 *       Pengurutan dan deduplikasi karenanya bekerja normal.</li>
 * </ul>
 *
 * <p>Ketiadaan kolom cakupan berarti daftar jabatan bersifat <b>global untuk seluruh instalasi</b>:
 * pada pemasangan multi-yayasan, semua sekolah memakai dan menyunting daftar yang sama. Itu
 * konsekuensi desain master bersama, bukan kasus <i>fail-open</i> (tidak ada cakupan yang bisa
 * gagal-terbuka).</p>
 *
 * <h2>Hal non-obvious</h2>
 *
 * <ol>
 *   <li><b>{@link #getNama()} memangkas spasi saat dibaca, {@link #setNama(String)} tidak saat
 *       ditulis.</b> Nilai yang tersimpan di database adalah apa adanya dari kotak isian
 *       ({@code JabatanOrganisasiSiswaAction.onSave()} memanggil {@code setNama(nama.getValue())}
 *       tanpa {@code trim()}), sementara pemeriksaan duplikat di
 *       {@code checkNamaJabatanOrganisasiSiswa()} membandingkan nilai yang <b>sudah</b> di-{@code
 *       trim}. Akibatnya {@code "Ketua"} dan {@code " Ketua "} lolos sebagai dua baris berbeda
 *       (constraint {@code unique} database pun melihatnya berbeda), tetapi keduanya tampil
 *       identik di layar karena getter memangkas spasi saat membaca. Dicatat apa adanya.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak base class.</b>
 *       {@link GeneralValueObject#getKeterangan()} menormalkan {@code null} menjadi {@code ""};
 *       override di sini mengembalikan nilai mentah sehingga {@code null} bisa lolos ke pemanggil.
 *       Lihat {@link #getKeterangan()}.</li>
 *   <li><b>{@link #toString()} memakai format {@code id + "-" + nama}</b>, berbeda dari
 *       {@link GeneralValueObject#toString()}. Lihat {@link #toString()} untuk konsekuensinya pada
 *       objek yang belum tersimpan.</li>
 *   <li><b>Properti induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} adalah POJO
 *       abstrak biasa &mdash; <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 *       sehingga Hibernate tidak memetakan properti apa pun miliknya. Deklarasi ulang
 *       {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} di berkas ini adalah
 *       <b>keharusan teknis</b>, bukan duplikasi yang perlu "dibersihkan".</li>
 *   <li><b>Komentar generator diganti.</b> Javadoc lama berbunyi {@code "Bank generated by
 *       hbm2java"} &mdash; sisa keluaran {@code hbm2java} yang salah tempel (menyebut entity
 *       {@code Bank}) dan tersebar di ratusan berkas model repo ini. Nilai
 *       {@link #serialVersionUID} pun sama persis di ratusan entity lain, jadi <b>bukan</b> penanda
 *       klon. Komentar baris ke-3 ({@code // Generated ... by Hibernate Tools}) sengaja dibiarkan
 *       sebagai penanda asal-usul berkas.</li>
 * </ol>
 *
 * <h2>Layar, jalur masuk, dan hak akses</h2>
 *
 * <p>Layar masternya {@code /pages/master/sekolah/jabatan_organisasi_siswa.zul} dengan action
 * {@code ais.action.master.sekolah.JabatanOrganisasiSiswaAction}: grid dua kolom (Nama Jabatan,
 * Keterangan) plus kolom tombol, filter tunggal berlabel <i>"Nama Jabatan"</i>, dan form popup dua
 * isian berlabel <i>"Nama Jabatan Organisasi"</i> serta <i>"Keterangan"</i>. Judul jendela form
 * dibangun di {@code init()} sebagai <i>"Tambah/Ubah Jabatan Organisasi Siswa"</i> &mdash; judul
 * statis {@code "Tambah OrganisasiSiswa"} pada berkas ZUL adalah sisa salin-tempel yang selalu
 * ditimpa saat runtime. Ada pula jalur UI baru berbasis JSP
 * ({@code /WEB-INF/new/sekolah/uiux/jabatan_organisasi_siswa.jsp} beserta {@code _service.jsp}-nya)
 * yang merupakan scaffold hasil generator ({@code generate_new_jsp_scaffold.py}) dan hanya
 * mendelegasikan ke dispatcher bersama tanpa membawa logika sendiri.</p>
 *
 * <p><b>Catatan hak akses (dicatat apa adanya, bukan anjuran perubahan di berkas ini):</b></p>
 *
 * <ul>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Penelusuran repo tidak menemukan tautan langsung ke
 *       {@code jabatan_organisasi_siswa.zul} selain satu: {@code OrganisasiSiswaAction
 *       .onJabatanOrganisasiSiswa()} menyisipkannya lewat {@code MyInclude} ke dalam tab
 *       <i>"Jabatan"</i> pada layar <i>Organisasi Siswa</i>. Karena
 *       {@code CommonPrivilages.checkPrevilages()} menilai hak terhadap
 *       {@code Common.getCurrentMenu()} &mdash; atribut <b>session</b> {@code currentMenu} yang
 *       terisi saat pengguna mengklik menu, bukan terhadap path halaman yang sedang di-<i>include</i>
 *       &mdash; maka hak Tambah/Ubah/Hapus pada tab ini sebenarnya diwarisi dari entri menu
 *       <i>Organisasi Siswa</i>. Siapa pun yang boleh menyunting organisasi siswa dengan sendirinya
 *       boleh menyunting katalog jabatan. Tab tersebut juga tidak punya gerbang tampil sendiri.</li>
 *   <li><b>Tombol "Download" tanpa gerbang.</b> Di {@code doAfterCompose()}, tombol ekspor Excel
 *       hasil {@code Common.cetakData(...)} dipasang lewat {@code Common.appendKeToolbar(...)}
 *       tanpa {@code setVisible(...)}; {@code appendKeToolbar} hanya memindahkan induk komponen dan
 *       tidak menyalin visibilitas dari tombol jangkarnya, sehingga tombol itu tampil bagi siapa
 *       pun yang bisa membuka layarnya. Isi yang terekspor adalah katalog nama jabatan
 *       ({@code id}, {@code nama}, {@code keterangan}) &mdash; tidak memuat data pribadi siswa.</li>
 *   <li><b>Contoh POSITIF di layar yang sama:</b> tombol unggah massal justru bergerbang eksplisit
 *       dan ketat ({@code upload.setVisible(add != null &amp;&amp; add.isVisible() &amp;&amp; edit
 *       &amp;&amp; delete)}), dan {@code doBeforeCompose()} memanggil
 *       {@code Common.doCheckSecurity()}. Tombol Ubah/Hapus per baris pun benar-benar terikat ke
 *       {@code edit}/{@code delete}, dan tautan riwayat revisi memakai kelas audit yang <b>benar</b>
 *       ({@code RevisiHelper.createNewRevisi(JabatanOrganisasiSiswa.class, ...)}).</li>
 * </ul>
 *
 * <h2>Verifikasi kerentanan {@code OrganisasiSiswaAction} (hasil: TIDAK TERJANGKAU dari sini)</h2>
 *
 * <p>{@code OrganisasiSiswaAction} memang memakai class ini secara langsung &mdash; meng-import-nya,
 * membaca kolom ke-6 berkas Excel unggahan lewat
 * {@code Common.getSheetContentAsObject(sheet, 5, i, JabatanOrganisasiSiswa.class)} untuk diisikan
 * ke {@code OrganisasiSiswaPunyaSiswa.setJabatanOrganisasiSiswa(...)}, dan menuliskan
 * {@code getNama()}-nya kembali saat mengekspor daftar anggota. Namun dua cacat yang dikenal pada
 * action tersebut <b>tidak</b> dapat dicapai lewat entity ini:</p>
 *
 * <ul>
 *   <li><b>SQL injection</b> pada {@code OrganisasiSiswaAction.initCriteria()} berasal dari dua
 *       kotak isian pencarian <i>siswa</i> ({@code searchnamamhs} dan {@code searchnim}) yang
 *       disambung mentah ke {@code Restrictions.sqlRestriction(...)}. Tidak ada satu pun nilai milik
 *       {@code JabatanOrganisasiSiswa} yang masuk ke string SQL itu, dan layar jabatan tidak
 *       menyediakan filter apa pun selain nama;</li>
 *   <li><b>bug schema salah-salin</b> pada string SQL yang sama ({@code inner join siswa b} tanpa
 *       kualifikasi schema, dan rujukan {@code sekolah.organisasi_siswa} ke tabel yang sudah
 *       ber-alias) juga sepenuhnya berada di dalam string tersebut;</li>
 *   <li>pencarian di layar jabatan sendiri <b>aman</b>: {@code JabatanOrganisasiSiswaAction
 *       .initCriteria()} memakai {@code Restrictions.ilike("nama", ..., MatchMode.ANYWHERE)} yang
 *       ter-parameterisasi, dan {@code sqlRestriction} hanya dipakai untuk literal konstan
 *       {@code "true"}/{@code "1=1"} tanpa penyambungan input.</li>
 * </ul>
 *
 * <p>Verifikasi negatif ini adalah yang <b>ketiga berturut-turut</b> di lingkungan
 * {@code OrganisasiSiswa}, dan kali ini pada entity yang benar-benar dipakai langsung oleh action
 * bermasalah tersebut &mdash; menguatkan bahwa kerentanannya terlokalisasi pada dua kotak pencarian
 * itu saja, bukan pola yang menular ke keluarga entity-nya.</p>
 *
 * <h2>Jalur tulis yang menyentuh baris ini dari layar lain</h2>
 *
 * <p>Nilai jabatan dipilih lewat combobox {@code readonly} yang diisi
 * {@code Common.insertCombo(combobox, "nama", JabatanOrganisasiSiswa.class)} di dua tempat, dan
 * keduanya menyimpan <b>langsung pada event {@code onChange}</b> lewat {@code Common.refreshUpdate},
 * tanpa tombol Simpan:</p>
 *
 * <ul>
 *   <li>{@code OrganisasiSiswaPunyaSiswaHelper} &mdash; panel pengurus dari sisi organisasi (jalur
 *       operator sekolah);</li>
 *   <li>{@code SiswaPunyaOrganisasiSiswaHelper} &mdash; panel dari sisi biodata siswa. Di sini siswa
 *       dapat memilih sendiri jabatannya, tetapi gerbangnya <b>benar</b> dan patut dicatat sebagai
 *       contoh positif: {@code bolehEdit} mensyaratkan {@code tbmuser.getSiswa() != null}
 *       <i>dan</i> id siswa pemilik baris sama dengan id siswa yang login, <i>dan</i> baris belum
 *       disetujui ({@code !getPersetujuan()}). Begitu operator mencentang "Setujui", seluruh isian
 *       termasuk combobox jabatan langsung ter-{@code disable}. Kontras dengan bug gerbang
 *       {@code getMahasiswa()} yang ditemukan pada layar sekolah lain.</li>
 * </ul>
 *
 * <p>Pada jalur unggah Excel milik {@code OrganisasiSiswaAction}, sel jabatan yang tidak cocok
 * dengan baris mana pun akan menghasilkan {@code null} secara diam-diam (exception di dalam
 * {@code getSheetContentAsObject} ditelan dan hanya dicatat ke audit error), sehingga impor massal
 * bisa menghasilkan keanggotaan tanpa jabatan tanpa peringatan apa pun ke operator.</p>
 *
 * @see GeneralValueObject
 * @see OrganisasiSiswaPunyaSiswa
 * @see OrganisasiSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jabatan_organisasi_siswa")



public class JabatanOrganisasiSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini sama persis di ratusan entity paket model repo ini
	 * (sisa salin-tempel kerangka {@code hbm2java}), jadi kesamaannya <b>bukan</b> penanda bahwa
	 * dua class merupakan klon satu sama lain. Jangan diubah: nilai lama diperlukan agar objek yang
	 * pernah diserialisasi (mis. ke session ZK) tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris, dipetakan ke kolom {@code id}. Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate; lihat {@link #getId()}.
	 */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris; diisi otomatis lewat {@link #onUpdate()}. */
	private String oleh;
	/** Identitas (id) pengguna yang terakhir mengubah baris; pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, <b>dengan penjagaan anti-timpa</b>: nilai
	 * {@code null} atau kosong/hanya spasi diabaikan diam-diam sehingga nilai lama tetap bertahan.
	 *
	 * <p>Penjagaan ini melindungi jejak audit dari jalur penyalinan properti reflektif yang
	 * mengirim nilai kosong. Konsekuensinya, kolom ini <b>tidak dapat dikosongkan kembali</b> lewat
	 * setter; sekali terisi, hanya bisa diganti dengan identitas lain yang tidak kosong.</p>
	 *
	 * @param olehId identitas pengubah; diabaikan bila {@code null} atau kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null} atau kosong/hanya spasi diabaikan diam-diam.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null} atau kosong/hanya spasi
	 * @see #setOlehId(String)
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> baris ini di-{@code
	 * UPDATE}, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Tidak ada pasangan {@code @PrePersist}:
	 * pada baris baru, stempel waktu berasal dari inisialisasi field {@link #tanggal_dirubah}
	 * ({@code WaktuUtil.getDate()}) yang dieksekusi saat konstruktor berjalan. Karena class ini
	 * juga ber-{@code @Audited}, setiap {@code UPDATE} sekaligus menghasilkan satu revisi Envers
	 * yang bisa ditelusuri lewat tautan riwayat pada kolom pertama grid layarnya.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * sengaja berbagi satu baris fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan
	 * entity paket ini. Jangan dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu
	 * konflik di banyak sesi paralel.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi dan tanpa penjagaan anti-timpa,
	 * berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}.
	 *
	 * <p>Normalnya tidak dipanggil dari kode aplikasi: nilai diisi otomatis saat konstruksi objek
	 * dan diperbarui {@link #onUpdate()} sebelum tiap {@code UPDATE}. Memanggilnya manual akan
	 * ditimpa kembali oleh kait tersebut pada penyimpanan berikutnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai
	 * {@code TemporalType.TIMESTAMP} (tanggal sekaligus jam).
	 *
	 * <p>Tidak pernah {@code null} untuk objek yang dibuat lewat konstruktor Java, karena field-nya
	 * diinisialisasi {@code WaktuUtil.getDate()} saat konstruksi. Baris yang masuk lewat SQL mentah
	 * atau migrasi tetap bisa mengembalikan {@code null}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dengan format {@code id + "-" + nama}, mis. {@code "3-Sekretaris"}.
	 *
	 * <p>Meng-override {@link GeneralValueObject#toString()}. Perhatikan dua hal: (1) pada objek
	 * yang <b>belum</b> tersimpan {@link #id} masih {@code null} sehingga hasilnya berawalan
	 * {@code "null-"}; (2) method ini membaca field {@link #nama} secara langsung, bukan lewat
	 * {@link #getNama()}, sehingga spasi di ujung nama <b>tidak</b> dipangkas di sini &mdash;
	 * berbeda dari nilai yang dilihat layar.</p>
	 *
	 * @return {@code id} disambung tanda hubung dan {@code nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama jabatan; kolom {@code nama}, wajib diisi dan unik di seluruh tabel. Inilah teks yang
	 * tampil di combobox "Jabatan" pada layar keanggotaan organisasi siswa.
	 */
	private String nama;
	/** Keterangan bebas; kolom {@code keterangan}, opsional. Tidak dipakai sebagai filter mana pun. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate/JPA dan dipakai
	 * {@code JabatanOrganisasiSiswaAction.onAdd()} saat membuka form "Tambah Jabatan Organisasi
	 * Siswa".
	 *
	 * <p>Efek samping yang perlu diketahui: inisialisasi field {@link #tanggal_dirubah} berjalan di
	 * sini, sehingga objek baru langsung membawa stempel waktu sekarang bahkan sebelum disimpan.</p>
	 */
	public JabatanOrganisasiSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan ke kolom {@code id} dengan strategi {@code IDENTITY} (nilai dibangkitkan
	 * database) dan {@code insertable = false} &mdash; kolom ini sengaja tidak disertakan pada
	 * pernyataan {@code INSERT}. Bernilai {@code null} selama objek belum tersimpan; itulah yang
	 * dipakai {@code JabatanOrganisasiSiswaAction} untuk membedakan alur Tambah dari alur Ubah
	 * (judul jendela, serta apakah baris perlu di-{@code load} ulang sebelum disimpan).</p>
	 *
	 * <p>Juga menjadi dasar {@link GeneralValueObject#equals(Object)}; dua objek yang belum
	 * tersimpan karenanya tidak bisa dibedakan lewat {@code equals}.</p>
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
	 * Menyetel kunci utama baris. Tanpa validasi.
	 *
	 * <p>Normalnya hanya dipanggil Hibernate setelah {@code INSERT}. Menyetelnya manual pada objek
	 * lepas akan mengubah identitas {@code equals}/{@code hashCode} objek tersebut.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jabatan, <b>sudah dipangkas spasi depan/belakang</b>.
	 *
	 * <p>Dipetakan ke kolom {@code nama} yang {@code nullable = false}, {@code unique = true}, dan
	 * berpanjang 255. Karena Hibernate memakai <i>property access</i> (anotasi berada pada getter),
	 * nilai yang dipangkas inilah yang juga ditulis ke database saat {@code INSERT}/{@code UPDATE}
	 * &mdash; sementara pemeriksaan duplikat di layar membandingkan nilai kotak isian yang
	 * di-{@code trim}. Baris yang sudah terlanjur memuat spasi (mis. masuk lewat SQL mentah) tetap
	 * akan terbaca terpangkas oleh aplikasi.</p>
	 *
	 * <p>Nilai ini juga menjadi kunci pengurutan yang dipakai
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} untuk class ini, karena
	 * {@code nomorUrut} dan {@code nim} tidak pernah terisi di sini.</p>
	 *
	 * @return nama jabatan tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jabatan. Tanpa validasi dan <b>tanpa</b> pemangkasan spasi &mdash; pemangkasan
	 * baru terjadi di {@link #getNama()}.
	 *
	 * <p>Kewajiban isi dan keunikan tidak ditegakkan di sini melainkan oleh pemanggil
	 * ({@code JabatanOrganisasiSiswaAction.onSave()} menolak nilai kosong dan menjalankan
	 * {@code checkNamaJabatanOrganisasiSiswa()}) serta oleh constraint database.</p>
	 *
	 * @param nama nama jabatan baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini, <b>apa adanya</b>.
	 *
	 * <p>Meng-override {@link GeneralValueObject#getKeterangan()} yang menormalkan {@code null}
	 * menjadi {@code ""}. Karena override ini mengembalikan nilai mentah, {@code null} dapat lolos
	 * ke pemanggil &mdash; termasuk ke {@code new Label(...)} pada renderer grid layarnya, yang
	 * menanganinya sebagai teks kosong sehingga tidak berakibat apa-apa di sana. Pemanggil baru
	 * sebaiknya tetap berjaga terhadap {@code null}.</p>
	 *
	 * <p>Kolom {@code keterangan} bersifat opsional ({@code nullable = true}) dan tanpa batas
	 * panjang eksplisit pada anotasi.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi; {@code null} diterima apa adanya dan akan terbaca
	 * kembali sebagai {@code null} lewat {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
