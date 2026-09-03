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
 * Entity master <b>tingkat teratas</b> klasifikasi kegiatan kesiswaan, tabel
 * {@code sekolah.jenis_kelompok_kegiatan_kesiswaan}. Isinya sengaja sangat sedikit: satu baris
 * mewakili satu <i>golongan besar</i> yang menaungi rumpun-rumpun kegiatan, dan data bawaan yang
 * dimaksudkan penulisnya hanya <b>dua</b> baris &mdash; {@code "Kelompok Utama"} dan
 * {@code "Kelompok Penunjang"} (lihat {@code ais.common.InitDataHelper}, dan catatan bug penyemaian
 * di bawah).
 *
 * <h2>PERINGATAN: ini BUKAN katalog "Akademik/Olahraga/Seni/Keagamaan"</h2>
 *
 * <p>Dugaan yang wajar &mdash; bahwa "jenis kelompok kegiatan kesiswaan" berisi nama-nama bidang
 * ekstrakurikuler seperti Akademik, Olahraga, Seni, atau Keagamaan &mdash; <b>keliru</b> dan sudah
 * diverifikasi dari kode. Nama-nama bidang semacam itu berada satu tingkat di bawah, pada
 * {@link KelompokKegiatanKesiswaan} (mis. baris seed
 * {@code "Keagamaan dan moral pancasila"}); dan nama kegiatan konkret seperti {@code "PHBI"} berada
 * dua tingkat di bawah, pada {@link DetailKelompokKegiatanKesiswaan}. Class ini adalah lapisan
 * paling abstrak di atas keduanya, yang isinya hanyalah pembeda "utama" versus "penunjang".</p>
 *
 * <h2>Posisi dalam hierarki master kegiatan kesiswaan</h2>
 *
 * <p>Modul kegiatan kesiswaan memakai klasifikasi <b>tiga tingkat</b>, dan class ini adalah
 * tingkat 1:</p>
 *
 * <ol>
 *   <li><b>class ini</b> &mdash; golongan besar ({@code "Kelompok Utama"} /
 *       {@code "Kelompok Penunjang"});</li>
 *   <li>{@link KelompokKegiatanKesiswaan} &mdash; rumpun/aspek kegiatan. Entity itulah yang
 *       menyimpan foreign key ke baris ini, lewat
 *       {@code KelompokKegiatanKesiswaan.getJenisKelompokKegiatanKesiswaan()} yang bersifat
 *       <b>wajib</b> ({@code nullable = false});</li>
 *   <li>{@link DetailKelompokKegiatanKesiswaan} &mdash; rincian kegiatan konkret.</li>
 * </ol>
 *
 * <p><b>Relasinya satu arah, dari anak ke induk.</b> Class ini <b>tidak</b> punya koleksi balik ke
 * {@link KelompokKegiatanKesiswaan}; daftar anak selalu diambil lewat query di sisi pemanggil.
 * Konsekuensi praktisnya: menghapus baris di sini <b>tidak</b> meng-cascade apa pun, melainkan
 * ditolak database karena constraint foreign key &mdash; itulah yang ditangkap tombol Hapus di
 * layarnya dan dilaporkan sebagai pesan "berelasi dengan data lainnya".</p>
 *
 * <h2>PERINGATAN KOSAKATA: label layar tidak sejajar dengan nama class</h2>
 *
 * <p>Istilah "Jenis", "Kelompok", dan "Aspek" dipakai secara berbeda di kode dan di layar. Yang
 * terverifikasi:</p>
 *
 * <ul>
 *   <li>entity <b>ini</b> di layar induknya disebut <b>"Kelompok Aspek"</b> (judul tab pada
 *       {@code kelompok_kegiatan_kesiswaan.zul}), <b>"Kelompok Aspek Kegiatan"</b> (judul kolom
 *       grid), dan <b>"Kelompok Kegiatan Kesiswaan"</b> (label combobox pada form tambah/ubah
 *       aspek). Di layar milik entity ini sendiri, ia memakai namanya yang panjang
 *       ({@code "Tambah/Ubah Jenis Kelompok Kegiatan Kesiswaan"}) sementara filter pencariannya
 *       cukup berlabel <b>"Nama Kelompok"</b>;</li>
 *   <li>sebutan <b>"Aspek Kegiatan Kesiswaan"</b> / <b>"Aspek Kegiatan"</b> justru menunjuk
 *       <b>anak</b> baris ini, yaitu {@link KelompokKegiatanKesiswaan};</li>
 *   <li>sebutan <b>"Aspek Rinci"</b> menunjuk cucu, {@link DetailKelompokKegiatanKesiswaan}.</li>
 * </ul>
 *
 * <p>Jadi kata "Kelompok" pada layar naik satu tingkat dibanding kata "Kelompok" pada nama class
 * {@link KelompokKegiatanKesiswaan}. Jangan menyimpulkan tingkat hierarki dari label UI.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ul>
 *   <li><b>Identitas</b> &mdash; {@link #getId()}/{@link #setId(Long)},
 *       {@link #getNama()}/{@link #setNama(String)},
 *       {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()};</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()};</li>
 *   <li><b>Konstruksi</b> &mdash; {@link #JenisKelompokKegiatanKesiswaan()}.</li>
 * </ul>
 *
 * <p>Perhatikan apa yang <b>tidak ada</b> di sini, berbeda dari kebanyakan katalog master lain di
 * paket ini: tidak ada kolom {@code aktif}, {@code nomorUrut}, {@code kode}/{@code feeder}, dan
 * tidak ada kolom cakupan {@code sekolah} maupun {@code yayasan}. Karena tidak ada kolom
 * {@code aktif}, pola bug "kolom {@code aktif} tak pernah ditulis" yang berulang di entity
 * bersaudara <b>tidak berlaku sama sekali</b> untuk berkas ini. Karena tidak ada kolom cakupan,
 * daftarnya global untuk seluruh instalasi: pada pemasangan multi-yayasan, semua sekolah memakai
 * dan menyunting daftar yang sama. Itu konsekuensi desain master bersama, bukan kasus
 * <i>fail-open</i> (tidak ada cakupan yang bisa gagal-terbuka).</p>
 *
 * <h2>Hal non-obvious</h2>
 *
 * <ol>
 *   <li><b>Baris {@code "Kelompok Penunjang"} tidak pernah tersemai &mdash; bug salin-tempel di
 *       {@code InitDataHelper}.</b> Blok penyemaian mencari baris {@code "Kelompok Utama"},
 *       membuatnya bila belum ada, lalu men-{@code flush}. Query berikutnya, yang seharusnya
 *       mencari {@code "Kelompok Penunjang"}, memakai literal yang <b>salah</b>:
 *       {@code Restrictions.eq("nama", "Kelompok Utama")} sekali lagi. Query itu karenanya selalu
 *       menemukan baris yang baru saja disimpan, blok pembuatannya tidak pernah dijalankan, dan
 *       variabel {@code penunjang} memegang objek {@code "Kelompok Utama"} yang sama persis.
 *       Akibatnya seluruh aspek bawaan &mdash; termasuk yang secara semantik <i>dimaksudkan</i>
 *       sebagai penunjang &mdash; digantungkan ke {@code "Kelompok Utama"}, dan pada instalasi
 *       baru tabel ini praktis hanya berisi <b>satu</b> baris. Admin harus mengetik sendiri
 *       {@code "Kelompok Penunjang"} lewat layarnya bila memerlukannya. Padanan PT
 *       ({@code ais.database.model.KelompokKegiatanKemahasiswaan}) tidak punya bug ini.</li>
 *   <li><b>Tabel ini di schema {@code sekolah}, anaknya di schema {@code public}.</b> Berbeda dari
 *       {@link KelompokKegiatanKesiswaan} dan {@link DetailKelompokKegiatanKesiswaan} yang
 *       dipetakan ke {@code schema = "public"} (mengikuti versi PT), class ini memakai
 *       {@code schema = "sekolah"} seperti kerabat lainnya
 *       ({@link SkalaKegiatanKesiswaan}, {@link JabatanKegiatanKesiswaan},
 *       {@link KegiatanKesiswaan}, {@link NilaiKegiatanKesiswaan}). Satu hierarki tiga tingkat
 *       karenanya terbelah di dua schema dan foreign key-nya menyeberang schema. Dicatat apa
 *       adanya, bukan anjuran perubahan.</li>
 *   <li><b>Foreign key dari anak bernama menyesatkan.</b> Kolom yang menunjuk baris ini di tabel
 *       {@code public.kelompok_kegiatan_kesiswaan} bernama {@code skala_kegiatan_kesiswaan},
 *       bukan sesuatu seperti {@code jenis_kelompok_kegiatan_kesiswaan}. Padahal ada entity lain
 *       yang benar-benar bernama {@link SkalaKegiatanKesiswaan} dan entity itu <b>tidak berelasi
 *       sama sekali</b> dengan class ini. Bug salin-tempel generator yang sama juga ada di versi
 *       PT dan versi dosen. Membaca skema database tanpa membaca anotasi
 *       {@code KelompokKegiatanKesiswaan.getJenisKelompokKegiatanKesiswaan()} akan menghasilkan
 *       kesimpulan yang salah.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak base class.</b>
 *       {@link GeneralValueObject#getKeterangan()} menormalkan {@code null} menjadi {@code ""};
 *       override di sini mengembalikan nilai mentah sehingga {@code null} bisa lolos ke pemanggil.
 *       Detail dan dampaknya dijelaskan di {@link #getKeterangan()}.</li>
 *   <li><b>{@link #toString()} memakai format {@code id + "-" + nama}</b> &mdash; berbeda dari
 *       {@link GeneralValueObject#toString()} maupun dari {@link KelompokKegiatanKesiswaan#toString()}
 *       (yang hanya memakai nama). Lihat {@link #toString()} untuk konsekuensi format ini pada
 *       objek yang belum tersimpan.</li>
 *   <li><b>Properti induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} adalah POJO
 *       abstrak biasa &mdash; <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 *       sehingga Hibernate tidak memetakan properti apa pun miliknya. Deklarasi ulang
 *       {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} di sini adalah
 *       <b>keharusan teknis</b>, bukan duplikasi yang perlu "dibersihkan".</li>
 *   <li><b>Komentar generator dipertahankan.</b> Baris {@code "Bank generated by hbm2java"} pada
 *       Javadoc asli adalah sisa keluaran {@code hbm2java} yang salah tempel (menyebut entity
 *       {@code Bank}); Javadoc ini menggantikannya, tetapi komentar baris ke-3
 *       ({@code // Generated ... by Hibernate Tools}) sengaja dibiarkan sebagai penanda asal-usul
 *       berkas.</li>
 * </ol>
 *
 * <h2>Layar, jalur masuk, dan hak akses</h2>
 *
 * <p>Layar masternya {@code /pages/master/sekolah/jenis_kelompok_kegiatan_kesiswaan.zul} dengan
 * action {@code ais.action.master.sekolah.JenisKelompokKegiatanKesiswaanAction}: grid dua kolom
 * (Nama, Keterangan) plus kolom tombol, filter tunggal "Nama Kelompok", dan form popup dua isian.
 * Ada pula jalur UI baru berbasis JSP ({@code /WEB-INF/new/sekolah/uiux/jenis_kelompok_kegiatan_kesiswaan.jsp}
 * beserta {@code _service.jsp}-nya) yang merupakan scaffold hasil generator
 * ({@code generate_new_jsp_scaffold.py}) dan hanya mendelegasikan ke
 * {@code /WEB-INF/new/_shared/…/dispatcher.jsp} tanpa membawa logika sendiri.</p>
 *
 * <p><b>Catatan hak akses (dicatat apa adanya, bukan anjuran perubahan di berkas ini):</b></p>
 *
 * <ul>
 *   <li><b>Pewarisan hak lewat menu induk, rantai TIGA TINGKAT.</b> Penelusuran seluruh repo tidak
 *       menemukan satu pun entri menu maupun tautan langsung ke
 *       {@code jenis_kelompok_kegiatan_kesiswaan.zul}. Satu-satunya jalur masuk yang ada adalah
 *       rantai sisipan: menu <i>"Kegiatan Kesiswaan"</i>
 *       ({@code kegiatan_kesiswaan.zul}) &rarr; tab <i>"Aspek Kegiatan"</i> yang menyisipkan
 *       {@code kelompok_kegiatan_kesiswaan.zul} &rarr; tab <i>"Kelompok Aspek"</i> yang menyisipkan
 *       layar ini. Karena {@code Common.getCurrentMenu()} membaca atribut session
 *       {@code "currentMenu"} yang diisi saat pengguna meng-<i>klik</i> entri menu, seluruh
 *       pemeriksaan {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} di action ini
 *       sebenarnya dievaluasi terhadap hak menu <b>terluar</b>. Artinya: hak ubah pada menu
 *       "Kegiatan Kesiswaan" dengan sendirinya memberi hak CRUD atas katalog tingkat teratas ini,
 *       tanpa pernah bisa dipisahkan lewat pengaturan role;</li>
 *   <li><b>tetapi action ini sendiri adalah contoh POSITIF pemasangan gerbang</b>, dan justru lebih
 *       ketat daripada action induknya: tombol Tambah dipasangi
 *       {@code checkPrevilages(CREATE)}, tombol Ubah/Hapus per baris dipasangi
 *       {@code UPDATE}/{@code DELETE}, dan tombol unggah Excel massal dipasangi syarat gabungan
 *       ({@code add.isVisible() && edit && delete}). Yang terakhir ini patut dicatat: pemasangan
 *       {@code setVisible()} eksplisit <i>sebelum</i> {@code Common.appendKeToolbar(...)}
 *       menghindarkan berkas ini dari pola cacat yang ditemukan di modul lain, karena
 *       {@code appendKeToolbar} sendiri tidak menyalin visibilitas dari tombol jangkarnya.
 *       Bandingkan dengan {@code KelompokKegiatanKesiswaanAction} (layar induk) yang seluruh
 *       gerbang CREATE/UPDATE/DELETE-nya justru <b>dikomentari mati</b>;</li>
 *   <li><b>tombol "Download" (ekspor Excel) tidak digerbangi apa pun</b> &mdash;
 *       {@code Common.cetakData(...)} dipasang ke toolbar tanpa {@code setVisible()}, jadi siapa
 *       pun yang bisa membuka layar bisa mengunduh seluruh isi tabel. Untuk katalog dua baris tanpa
 *       data pribadi, dampaknya kecil; dicatat sebagai observasi;</li>
 *   <li><b>{@code Common.doCheckSecurity()} pada {@code doBeforeCompose} praktis no-op untuk layar
 *       ini.</b> Method itu berujung di {@code CommonPrivilages.doCheckPrevilagesRead()}, yang
 *       hanya menegakkan hak READ untuk URL yang tercantum di whitelist {@code MUST_CHECKED} &mdash;
 *       12 URL yang <b>seluruhnya modul perguruan tinggi</b> ({@code mahasiswa.zul},
 *       {@code fakultas.zul}, {@code dosen.zul}, dan seterusnya). Tidak ada satu pun URL modul
 *       sekolah di daftar itu, dan lagi pula layar ini selalu dimuat sebagai sisipan sehingga
 *       {@code getRequestPath()} mengembalikan halaman terluar. Gerbang READ yang efektif untuk
 *       layar ini semata-mata adalah hak membuka menu induknya;</li>
 *   <li><b>tidak ada perakitan SQL mentah dari input pengguna</b> di seluruh jalur layar ini &mdash;
 *       lihat catatan verifikasi di bawah.</li>
 * </ul>
 *
 * <h2>Hasil verifikasi pola {@code OrganisasiSiswaAction} (SQL injection &amp; salah-salin schema)</h2>
 *
 * <p><b>NEGATIF, terverifikasi dari kode.</b> {@code JenisKelompokKegiatanKesiswaanAction} memang
 * memanggil {@code Restrictions.sqlRestriction(...)} di dua tempat, tetapi <b>keduanya memakai
 * literal konstan</b> ({@code "true"} pada {@code initCriteria()} dan {@code "1=1"} pada
 * {@code checkNamaJenisKelompokKegiatanKesiswaan()}) yang dipilih lewat operator terner, bukan
 * string hasil perangkaian. Nilai ketikan pengguna hanya masuk lewat
 * {@code Restrictions.ilike("nama", ...)} dan {@code Restrictions.eq("nama", ...)} yang
 * terparameter penuh. Bandingkan dengan {@code OrganisasiSiswaAction.initCriteria()} yang
 * merangkai {@code searchnamamhs}/{@code searchnim} apa adanya ke dalam sub-query
 * {@code sqlRestriction} berisi {@code ilike '%…%'} &mdash; pola itu <b>tidak ada</b> di sini.
 * Bug salah-salin schema pada SQL mentah (mis. menyebut kolom dengan awalan schema, atau menembak
 * {@code public.*} padahal {@code sekolah.*}) juga <b>tidak ada</b>, karena tidak ada SQL mentah
 * sama sekali. Pemeriksaan tambahan yang juga negatif: {@code RevisiHelper.createNewRevisi(...)}
 * dipanggil dengan kelas audit yang <b>benar</b> ({@code JenisKelompokKegiatanKesiswaan.class}),
 * dan tidak ada getter di berkas ini yang menulis balik ke field-nya sendiri
 * (pola "getter destruktif").</p>
 *
 * @see KelompokKegiatanKesiswaan
 * @see DetailKelompokKegiatanKesiswaan
 * @see KegiatanKesiswaan
 * @see SkalaKegiatanKesiswaan
 * @see JabatanKegiatanKesiswaan
 * @see ais.database.model.KelompokKegiatanKemahasiswaan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_kelompok_kegiatan_kesiswaan")



public class JenisKelompokKegiatanKesiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sama persis dengan milik {@link KelompokKegiatanKesiswaan},
	 * {@link SkalaKegiatanKesiswaan}, {@link JabatanKegiatanKesiswaan}, dan padanan PT-nya
	 * {@link ais.database.model.KelompokKegiatanKemahasiswaan} &mdash; hasil salin-tempel generator;
	 * tidak ada makna khusus di balik angka ini.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Identitas (NIS/NIP/username) pengubah terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini (NIS/NIP/username,
	 * tergantung jenis akun), sebagaimana diisi
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * @return identitas pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, dengan <b>penjagaan anti-timpa</b>: nilai
	 * {@code null} atau string kosong/hanya spasi diabaikan diam-diam sehingga jejak audit yang
	 * sudah ada tidak terhapus oleh proses batch atau salinan bean yang tidak membawa konteks
	 * pengguna.
	 *
	 * <p>Konsekuensinya, nilai kolom ini <b>tidak dapat dikosongkan kembali</b> lewat setter; sekali
	 * terisi, hanya bisa diganti dengan identitas lain yang tidak kosong.</p>
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
	 * ({@code WaktuUtil.getDate()}) yang dieksekusi saat konstruktor berjalan. Karena baris ini
	 * juga ber-{@code @Audited}, setiap {@code UPDATE} sekaligus menghasilkan satu revisi Envers
	 * yang bisa ditelusuri lewat tautan riwayat di grid layarnya.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * sengaja berbagi satu baris fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan
	 * entity paket ini. Jangan dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu
	 * konflik di banyak sesi paralel.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima.
	 *
	 * <p>Umumnya tidak dipanggil manual &mdash; {@link #onUpdate()} yang mengisinya otomatis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini. Dipetakan sebagai {@code TIMESTAMP},
	 * dan karena field-nya diinisialisasi {@code WaktuUtil.getDate()} saat konstruktor berjalan,
	 * nilainya tidak pernah {@code null} untuk objek yang dibuat lewat konstruktor.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dengan format {@code id + "-" + nama}, mis.
	 * {@code "1-Kelompok Utama"}.
	 *
	 * <p>Formatnya <b>berbeda</b> dari {@link GeneralValueObject#toString()} (yang memakai
	 * {@code kode + " - " + nama}) dan berbeda pula dari
	 * {@link KelompokKegiatanKesiswaan#toString()} (yang hanya memakai nama).</p>
	 *
	 * <p><b>Perhatian &mdash; dua kuirk:</b></p>
	 * <ol>
	 *   <li>{@code id} ikut dirangkai apa adanya, sehingga objek yang <i>belum tersimpan</i>
	 *       menghasilkan teks berawalan {@code "null-"};</li>
	 *   <li>field {@code nama} dibaca <b>langsung</b>, bukan lewat {@link #getNama()}, sehingga
	 *       hasilnya <b>tidak di-{@code trim}</b> dan bisa {@code null}.</li>
	 * </ol>
	 *
	 * <p>Dampaknya kecil karena nyaris tidak ada jalur UI yang memakainya: combobox pemilih induk
	 * di {@code KelompokKegiatanKesiswaanAction} diisi lewat
	 * {@code Common.insertCombo(combo, "nama", "keterangan", ...)} yang menyusun label secara
	 * reflektif dari properti bernama, bukan dari {@code toString()}; dan kolom grid layar induk
	 * memakai {@code getNama()}. Pemakaian yang tersisa bersifat diagnostik &mdash; antara lain
	 * satu {@code System.out.println} pada blok penyemaian {@code InitDataHelper}.</p>
	 *
	 * @return teks {@code "<id>-<nama>"}; dapat berisi {@code "null"} pada salah satu atau kedua
	 *         bagiannya
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama golongan kegiatan, wajib. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas, opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Semua field dibiarkan {@code null}
	 * kecuali {@link #tanggal_dirubah} yang langsung terisi waktu saat ini lewat inisialisasi
	 * field.
	 *
	 * <p>Dipanggil langsung oleh {@code JenisKelompokKegiatanKesiswaanAction.onAdd()} untuk
	 * menyiapkan form "Tambah Jenis Kelompok Kegiatan Kesiswaan", dan oleh
	 * {@code ais.common.InitDataHelper} saat menyemai data bawaan.</p>
	 */
	public JenisKelompokKegiatanKesiswaan() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dihasilkan database dengan strategi {@link javax.persistence.GenerationType#IDENTITY}
	 * (sequence PostgreSQL di balik kolom {@code serial}), karena itu kolomnya dipetakan
	 * {@code insertable = false}. Bernilai {@code null} selama objek belum pernah disimpan &mdash;
	 * kondisi inilah yang dipakai layar masternya untuk membedakan mode "Tambah" dari "Ubah"
	 * (judul jendela) dan untuk memutuskan apakah pemeriksaan duplikat nama perlu mengecualikan
	 * baris yang sedang disunting (lihat {@code checkNamaJenisKelompokKegiatanKesiswaan()}).</p>
	 *
	 * <p>Nilai id juga menjadi bagian pertama {@link #toString()}, dan menjadi
	 * <i>reference id</i> yang dipakai {@code RevisiHelper} untuk menarik riwayat Envers baris
	 * ini.</p>
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
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris; kode aplikasi sebaiknya
	 * tidak menyetel id secara manual.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama golongan kegiatan kesiswaan (mis. {@code "Kelompok Utama"}),
	 * <b>sudah di-{@code trim}</b>.
	 *
	 * <p>Ini satu-satunya isian yang benar-benar bermakna pada entity ini; nilainya muncul sebagai
	 * label item combobox "Kelompok Kegiatan Kesiswaan" pada form tambah/ubah aspek dan sebagai isi
	 * kolom grid "Kelompok Aspek Kegiatan" di layar {@link KelompokKegiatanKesiswaan}.</p>
	 *
	 * <p>Kolom {@code nama} bersifat {@code nullable = false} di database, dan layar masternya juga
	 * memvalidasi di sisi aplikasi: nama kosong ditolak dengan pesan
	 * {@code "Nama Jenis Kelompok Kegiatan Kesiswaan harus diisi"}, dan nama yang sudah ada ditolak
	 * sebagai duplikat. Perhatikan bahwa pemeriksaan duplikat itu
	 * ({@code Restrictions.eq("nama", ...)}) bersifat <b>case-sensitive</b>, sehingga
	 * {@code "Kelompok Utama"} dan {@code "kelompok utama"} tetap lolos sebagai dua baris berbeda.
	 * Tidak ada {@code unique constraint} di tingkat database yang menegakkannya, jadi jalur impor
	 * Excel massal ({@code Common.uploadData}) pun bisa memasukkan duplikat.</p>
	 *
	 * <p><b>Getter ini tidak menulis balik hasil {@code trim} ke field</b> &mdash; pembersihan hanya
	 * berlaku pada nilai kembalian dan tidak memicu {@code UPDATE} tersembunyi (berbeda dari pola
	 * "getter destruktif" yang ditemukan di beberapa entity lain). Sebagai efek sampingnya,
	 * {@link #toString()} yang membaca field langsung bisa mengembalikan teks yang masih berspasi
	 * tepi.</p>
	 *
	 * @return nama golongan tanpa spasi tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama golongan kegiatan kesiswaan. Tanpa validasi dan tanpa {@code trim} &mdash;
	 * pemeriksaan wajib-isi serta anti-duplikat dilakukan di layar masternya sebelum setter ini
	 * dipanggil.
	 *
	 * <p>Pemanggil: {@code JenisKelompokKegiatanKesiswaanAction.onSave()} (meneruskan isi textbox
	 * apa adanya, tanpa {@code trim} &mdash; padahal validasinya memeriksa versi ter-{@code trim},
	 * sehingga spasi tepi bisa ikut tersimpan ke database) dan blok penyemaian
	 * {@code ais.common.InitDataHelper}.</p>
	 *
	 * @param nama nama golongan yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini &mdash; isian textarea 3 baris pada form tambah/ubah,
	 * kolom "Keterangan" di grid layarnya, dan teks deskripsi pada item combobox pemilih induk di
	 * layar {@link KelompokKegiatanKesiswaan}.
	 *
	 * <p><b>Override ini membalik kontrak base class.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjanjikan hasil <i>tidak pernah</i> {@code null}
	 * ({@code null} dinormalkan menjadi {@code ""}); versi di sini mengembalikan nilai field mentah,
	 * sehingga {@code null} bisa lolos ke pemanggil. Pola yang sama muncul di sejumlah entity
	 * turunan {@code hbm2java} lain di paket ini &mdash; termasuk anaknya
	 * {@link KelompokKegiatanKesiswaan#getKeterangan()} &mdash; jadi ini variasi arsitektural yang
	 * dikenal, bukan anomali terisolasi. Tetapi kode pemanggil tetap tidak boleh mengandalkan
	 * jaminan non-null milik base class saat bekerja dengan tipe ini.</p>
	 *
	 * <p>Dampak praktisnya kecil namun nyata: cabang pembanding {@code keterangan} pada
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} tidak lagi selalu memenuhi syarat
	 * non-null, sehingga dua baris yang seharusnya diurutkan lewat keterangan bisa jatuh ke hasil
	 * {@code 0} (dianggap setara). Pada praktiknya cabang itu jarang tercapai karena
	 * {@code compareTo} sudah lebih dulu memakai {@code nama}.</p>
	 *
	 * @return keterangan baris ini, bisa {@code null}
	 * @see GeneralValueObject#getKeterangan()
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi; nilai {@code null} diterima dan &mdash; berbeda
	 * dari base class &mdash; akan terbaca kembali sebagai {@code null} lewat
	 * {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
