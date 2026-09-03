package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

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
 * Baris <b>penghubung</b> (tabel silang) antara sebuah Grup Kategori Penilaian dan sebuah Kategori
 * Penilaian pada rantai penilaian rapor modul sekolah, dipetakan ke tabel
 * {@code sekolah.detail_grup_kategori_item_penilaian_siswa}.
 *
 * <p>Isinya benar-benar minimal: dua FK wajib
 * ({@link #getGrupKategoriItemPenilaianSiswa() grupKategoriItemPenilaianSiswa} dan
 * {@link #getKategoriItemPenilaianSiswa() kategoriItemPenilaianSiswa}), satu
 * {@link #getKeterangan() keterangan} yang tidak pernah diisi siapa pun, satu saklar
 * {@link #getAktif() aktif} yang memikul SELURUH semantik "tercentang / tidak tercentang", plus
 * jejak audit warisan ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}). Tidak ada bobot,
 * formula, nomor urut tersimpan, maupun relasi tenant (sekolah/yayasan) di sini — cakupan tenant
 * seluruhnya diwarisi dari sisi grup.
 *
 * <p>Kelas ini <b>tidak punya layar sendiri</b>. Ia hanya lahir, hidup, dan "mati" di dalam dialog
 * <i>Tambah/Ubah Grup Kategori Penilaian</i>
 * ({@code ais.action.master.sekolah.GrupKategoriItemPenilaianSiswaAction}), sebagai baris di balik
 * daftar centang berjudul <i>"Pilih Kategori Penilaian"</i>.
 *
 * <h3>Posisi TERVERIFIKASI dalam rantai penilaian</h3>
 * Rantai berikut dipastikan dari deklarasi kolom FK tiap entity penghubung, bukan dari kemiripan
 * nama kelas. Seluruh penghubung memakai pola "tabel silang + kolom {@code aktif}", bukan
 * {@code @ManyToMany}:
 * <ol>
 * <li>{@link JenisPenilaian} — payung teratas sekaligus <b>layar induk</b> seluruh master
 * penilaian sekolah.</li>
 * <li>{@link DetailJenisPenilaian} — {@code jenisPenilaian} &harr; {@code grupPenilaian}.</li>
 * <li>{@link GrupPenilaian} — pemilik {@code formula} tingkat grup, {@code jenisNilaiHuruf},
 * {@code adaTotal}.</li>
 * <li>{@link DetailGrupPenilaian} — {@code grupPenilaian} &harr;
 * {@code grupKategoriItemPenilaianSiswa}.</li>
 * <li>{@link ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa} (tabel
 * {@code grup_kategori_item_penilaian_siswa}) — pemilik {@code formula} kategori,
 * {@code nilaiBolehDinputOlehGuru}, pembatas {@code khususTingkat}/{@code khususSemester}, dan
 * <b>satu-satunya pemegang relasi tenant</b> {@code sekolah}/{@code yayasan} di sekitar simpul
 * ini.</li>
 * <li><b>{@code DetailGrupKategoriItemPenilaianSiswa}</b> — <b>kelas ini</b>, kolom FK
 * {@code grup_kategori_item_penilaian_siswa} dan {@code kategori_item_penilaian_siswa}.</li>
 * <li>{@link ais.database.model.sekolah.KategoriItemPenilaianSiswa} (tabel
 * {@code kategori_item_penilaian_siswa}) — rumpun butir nilai; {@code getKode()}-nya menjadi
 * kunci urut primer seluruh kolom nilai di layar, rapor, rekap, dan API.</li>
 * <li>{@link JenisItemPenilaianSiswa} — butir nilai konkret (tipe isian,
 * {@code nilaiMin}/{@code nilaiMax}, {@code formula}, {@code wajibDiisi}, {@code nomorUrut}).</li>
 * </ol>
 *
 * <p>Dengan kata lain: kelas ini adalah <b>satu-satunya jembatan</b> yang menentukan butir nilai
 * mana saja yang muncul di bawah sebuah grup. Memutus baris ini sama dengan menghapus seluruh
 * kolom nilai kategori tersebut dari rapor — tanpa menyentuh satu pun baris master.
 *
 * <h3>Cara entity ini dibaca saat runtime</h3>
 * Sembilan pemanggil memakai pola query yang <b>identik kata demi kata</b> (hasil salin-tempel),
 * yaitu memproyeksikan id kategori yang masih aktif untuk satu grup:
 * <pre>{@code
 * createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
 *     .add(Restrictions.eq("grupKategoriItemPenilaianSiswa", grup))
 *     .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
 *     .setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id"))
 * }</pre>
 * Hasilnya dibungkus {@code ConstantValues.simpleList(…, KategoriItemPenilaianSiswa.class, false)}
 * menjadi daftar <i>stub</i> berisi id saja, lalu dipakai sebagai
 * {@code Restrictions.in("kategoriItemPenilaianSiswa", …)} untuk menarik
 * {@link JenisItemPenilaianSiswa}.
 *
 * <p>Kesembilan pemanggil itu sudah diverifikasi: {@code ais.common.GradingHelper} (hitung ulang
 * nilai massal), {@code ais.action.master.sekolah.PenilaianSiswaAction} (ekspor Excel),
 * {@code ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper} dan
 * {@code …DetailPenilaianLesSiswaHelper} (layar isi nilai),
 * {@code ais.action.master.sekolah.helper.PertemuanPunyaUjianSiswaHelper},
 * {@code ais.action.master.helper.TugasMandiriHelper} dan {@code TugasKelompokHelper} (nilai tugas
 * e-learning), {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} dan
 * {@code LaporanRekapTotalNilai} (cetak rapor/rekap), serta REST
 * {@code ais.action.servlet.api.NilaiSiswaApi} dan {@code ais.action.servlet.api.ElearningApiUtil}.
 * Entity ini <b>hidup penuh</b>, bukan sisa skema lama.
 *
 * <p>Baris ini juga di-<i>preload</i> seluruhnya ke cache in-memory global
 * ({@code ais.common.InitData#doInitData()} memanggil {@code initClasses(…, }
 * {@code DetailGrupKategoriItemPenilaianSiswa.class, …)}) dan terdaftar di
 * {@code ais.common.DataUtil#CLASS_JANGAN_DIBERSIHKAN} — artinya barisnya <b>tidak pernah
 * dibersihkan</b> oleh perkakas pembersih data. Gabungkan dengan fakta bahwa layar pengelolanya
 * tidak pernah melakukan {@code delete} (lihat di bawah): tabel ini hanya bertambah, selamanya.
 *
 * <h3>Saklar {@code aktif}: siklus "matikan semua lalu hidupkan yang tercentang"</h3>
 * <b>Ini bagian terpenting dari berkas ini.</b> Baris di tabel ini tidak pernah dihapus; centang
 * dan lepas-centang seluruhnya diterjemahkan menjadi tulisan pada kolom {@code aktif}, oleh
 * {@code GrupKategoriItemPenilaianSiswaAction#onSave(Event)} dengan urutan berikut:
 * <ol>
 * <li>Simpan/perbarui grup induknya.</li>
 * <li><b>Tarik SELURUH baris detail milik grup itu — tanpa penyaring {@code aktif} sama sekali —
 * lalu setel {@code setAktif(false)} pada setiap baris dan {@code flush()}.</b> Pada titik ini,
 * untuk sesaat, grup tersebut tidak punya satu pun kategori aktif.</li>
 * <li>Iterasi peta {@code selectedKategoriItemPenilaianSiswa} (kunci = id kategori, nilai = baris
 * detail) dan setel {@code setAktif(true)} + {@code setGrupKategoriItemPenilaianSiswa(grup)} pada
 * tiap anggotanya, masing-masing dengan {@code flush()}.</li>
 * </ol>
 * Peta pada langkah 3 <b>tidak pernah diisi oleh {@code onSave}</b>. Ia diisi listener
 * {@code ubahJenisPenialain} yang dijalankan {@code Common.createDefaultTimer(...)} sesaat setelah
 * dialog terbuka, dan juga saat combo Yayasan/Sekolah diubah. Dari sinilah tiga masalah nyata
 * lahir:
 *
 * <h4>(a) Bom waktu: menyimpan grup untuk alasan lain melenyapkan seluruh butir nilai</h4>
 * Peta itu adalah {@code HashMap} biasa yang diinisialisasi <b>kosong</b> saat dialog dibangun.
 * Bila peta belum sempat terisi ketika tombol Simpan ditekan — timer belum menyala, listener
 * melempar exception yang tertelan, atau alur pemanggil baru yang memakai {@code onSave} tanpa
 * membangun daftar centang — langkah 2 tetap berjalan penuh sedangkan langkah 3 tidak menghidupkan
 * apa pun. Hasilnya: seluruh pemetaan kategori grup tersebut menjadi {@code aktif=false} sekaligus.
 * Efeknya <b>tidak terlihat di layar master</b> (grid hanya menampilkan daftar kategori aktif, yang
 * kini kosong dan mudah dikira memang belum diisi), tetapi seluruh kolom butir nilai grup itu
 * lenyap dari formulir isi nilai, rapor, rekap, dan API pada saat itu juga. Menyimpan grup semata
 * untuk mengubah <i>formula</i>, nama, keterangan, tingkat, atau semester sudah cukup untuk
 * memicunya — pengguna tidak pernah bermaksud menyentuh daftar kategori.
 *
 * <h4>(b) "Kategori hantu" lintas sekolah yang tidak dapat dilepas</h4>
 * Dua query yang mengisi dialog memakai penyaring yang <b>berbeda</b>:
 * <ul>
 * <li>Daftar checkbox dibangun dari {@link ais.database.model.sekolah.KategoriItemPenilaianSiswa}
 * dengan penyaring tenant {@code isNull("sekolah") OR eq("sekolah", s)} (idem yayasan).</li>
 * <li>Peta pilihan tersimpan dibangun dari tabel <b>ini</b> dengan penyaring
 * {@code eq("grupKategoriItemPenilaianSiswa", grup)} + {@code aktif} + {@code kategori.aktif},
 * <b>tanpa penyaring tenant apa pun</b>.</li>
 * </ul>
 * Baris detail yang menunjuk kategori milik sekolah lain karena itu <b>masuk ke peta</b> tetapi
 * <b>tidak pernah punya checkbox</b>. Tidak ada checkbox berarti tidak ada jalan untuk
 * mengeluarkannya dari peta, sehingga langkah 3 selalu menghidupkannya kembali. Baris hantu itu
 * tetap {@code aktif=true} selamanya, tetap menyumbang butir nilai ke rapor, dan <b>tidak dapat
 * dilepas dari layar mana pun</b>. Baris seperti ini dapat lahir dari unggah Excel, dari perpindahan
 * kepemilikan sekolah pada master kategori, atau dari menyunting grup sambil combo sekolah menunjuk
 * tenant yang berbeda.
 *
 * <h4>(c) Menonaktifkan sebuah kategori mematikan pemetaannya secara permanen</h4>
 * Query pengisi peta ikut menyaring {@code kategoriItemPenilaianSiswa.aktif}. Kategori yang
 * dinonaktifkan karena itu hilang dari peta <i>dan</i> dari daftar checkbox, sehingga penyimpanan
 * grup berikutnya mematikan baris detailnya di langkah 2 tanpa pernah menghidupkannya lagi. Bila
 * kemudian kategori itu diaktifkan ulang lalu dicentang, dialog <b>tidak menemukan baris lama</b>
 * di peta dan membuat objek {@code new DetailGrupKategoriItemPenilaianSiswa()} — sehingga tersimpan
 * <b>baris kedua</b> untuk pasangan (grup, kategori) yang sama: satu mati, satu hidup. Tidak ada
 * kunci unik yang mencegahnya, dan pembaca memakai
 * {@code Projections.groupProperty("kategoriItemPenilaianSiswa.id")} sehingga duplikat itu tidak
 * pernah terlihat — ia hanya menumpuk di tabel dan di tabel audit Envers.
 *
 * <p><b>Sisi baiknya perlu dicatat jujur:</b> semua kerusakan di atas dapat "diperbaiki" pengguna
 * dengan membuka kembali dialog grup, memastikan daftar centang sudah termuat, mencentang ulang,
 * lalu menyimpan — asalkan kategorinya memang tampil sebagai checkbox. Kasus (b) adalah satu-satunya
 * yang tidak punya jalan keluar lewat UI.
 *
 * <h3>Satu pembaca yang MENGABAIKAN {@code aktif}</h3>
 * Dari sepuluh titik baca, sembilan menyaring {@code aktif} (lihat pola query di atas). Pengecualian
 * tunggalnya adalah mesin formula
 * {@code ais.action.master.sekolah.util.GrupPenilaianUtil#hitung(...)}: ia mengambil
 * {@code ConstantValues.ambilBerdasarClass(DetailGrupKategoriItemPenilaianSiswa.class)} — seluruh
 * isi tabel dari cache in-memory — lalu mencocokkan {@code grupKategoriItemPenilaianSiswa.getId()}
 * <b>tanpa pernah memanggil {@code getAktif()}</b>, padahal pada method yang sama {@code getAktif()}
 * dipanggil eksplisit untuk {@code Konstanta}, {@link JenisItemPenilaianSiswa},
 * {@link DetailGrupPenilaian}, dan {@link GrupPenilaian}. Konsekuensinya: <b>kode butir nilai milik
 * kategori yang sudah "dilepas" tetap disubstitusi nilainya ke dalam formula</b>. Rapor berhenti
 * <i>menampilkan</i> butir tersebut, tetapi angka totalnya masih ikut menghitungnya — divergensi
 * senyap antara yang tampil dan yang dijumlahkan. Perhatikan pula bahwa cache itu global lintas
 * tenant, sehingga baris hantu pada butir (b) juga ikut terekspansi di sini.
 *
 * <h3>Cakupan tenant dan hak akses</h3>
 * Entity ini <b>tidak memiliki</b> relasi {@code sekolah}/{@code yayasan}; cakupannya sepenuhnya
 * bergantung pada grup induk. Karena itu tidak ada pola <i>fail-open</i> tenant yang bisa muncul di
 * berkas ini sendiri — masalah tenant yang nyata justru muncul dari <b>ketiadaan</b> penyaring
 * tenant pada query pengisi peta (butir (b) di atas).
 *
 * <p>Seluruh hak akses ditegakkan di layar grup. {@code GrupKategoriItemPenilaianSiswaAction}
 * memang memanggil {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} dengan benar
 * untuk tombol Tambah, checkbox per baris, dan tombol Ubah/Hapus — tetapi layar itu
 * <b>tidak terdaftar sebagai menu mandiri</b>: ia disisipkan sebagai tab di dalam layar
 * <i>Jenis Penilaian</i>. Karena {@code CommonPrivilages.checkPrevilages(...)} selalu mengacu ke
 * {@code Common.getCurrentMenu()}, hak yang sesungguhnya diuji adalah hak pada menu
 * <b>Jenis Penilaian</b>. Ini instance pola <i>pewarisan hak lewat menu induk</i> yang sama dengan
 * yang tercatat pada {@link ais.database.model.sekolah.KategoriItemPenilaianSiswa} dan
 * {@link PaketPsb}; siapa pun yang boleh mengubah Jenis Penilaian otomatis boleh memicu seluruh
 * siklus mati-hidup yang diuraikan di atas.
 *
 * <h3>Hal non-obvious lain sebelum mengubah berkas ini</h3>
 * <ul>
 * <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * TIDAK memetakan satu pun propertinya. Maka {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}, dan {@code keterangan} <b>harus</b> dideklarasikan ulang di sini agar
 * terpetakan. Ini KEHARUSAN TEKNIS, bukan duplikasi yang bisa "dirapikan".</li>
 * <li><b>Akibat sampingannya: field induk selamanya kosong.</b> Field lokal {@code keterangan}
 * membayangi ({@code shadow}) field bernama sama di induk; kode yang membaca field induk secara
 * langsung akan mendapat {@code null}.</li>
 * <li><b>{@link #getNomorUrut()} bukan {@code @Transient}</b> dan setter-nya diwarisi dari
 * {@link GeneralValueObject}, sehingga Hibernate memperlakukannya sebagai properti persisten
 * (kolom {@code nomorUrut} pada tabel utama dan tabel audit Envers). Nilainya adalah salinan yang
 * <b>tidak pernah dibaca kembali</b> — lihat catatan lengkap pada method tersebut.</li>
 * <li><b>Tidak ada {@code equals}/{@code hashCode}.</b> Perbandingan memakai identitas objek;
 * dua instance hasil dua query berbeda untuk baris yang sama tidak akan dianggap sama. Layar
 * pengelola menyiasatinya dengan memakai <i>id kategori</i> sebagai kunci {@code HashMap}, bukan
 * entity ini.</li>
 * <li><b>Urutan alaminya rusak.</b> {@code compareTo} diwarisi dari {@link GeneralValueObject} dan
 * memakai {@code getNomorUrut()} sebagai kunci pertama. Karena {@link #getNomorUrut()} di kelas ini
 * praktis selalu mengembalikan nilai yang sama untuk semua baris, cabang pertama itu selalu menang
 * dan selalu menghasilkan {@code 0} — seluruh baris dianggap setara. Menaruh entity ini di
 * {@code TreeSet}/{@code TreeMap} akan menciutkan koleksi menjadi satu elemen. Saat ini tidak ada
 * pemanggil yang melakukannya (semua memakai {@code List}/{@code HashMap}), jadi ini bahaya laten,
 * bukan bug aktif.</li>
 * <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif</b>, sehingga Hibernate hanya mengirim
 * kolom yang benar-benar berubah. Ini yang membuat siklus mati-hidup di atas relatif murah, tetapi
 * juga berarti tiap iterasi tetap menghasilkan satu revisi Envers ({@code @Audited}) — tabel audit
 * tumbuh dua revisi per kategori setiap kali grup disimpan.</li>
 * </ul>
 *
 * @see ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa
 * @see ais.database.model.sekolah.KategoriItemPenilaianSiswa
 * @see JenisItemPenilaianSiswa
 * @see DetailGrupPenilaian
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "detail_grup_kategori_item_penilaian_siswa", schema = "sekolah")
public class DetailGrupKategoriItemPenilaianSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan perkakas dan <b>tidak boleh diubah</b>: entity
	 * ini ikut terserialisasi ke dalam state desktop ZK dan ke cache, sehingga mengubahnya membuat
	 * data lama gagal dibaca.
	 */
	private static final long serialVersionUID = -9157912161411433979L;

	/**
	 * Kunci primer baris penghubung, kolom {@code id} bertipe IDENTITY. Dideklarasikan ulang di
	 * sini karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah diubah lewat jalur yang
	 *         mengisinya
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>validasi non-trivial</b>: argumen
	 * {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b> (method langsung
	 * {@code return} tanpa menulis apa pun), sehingga jejak audit yang sudah ada tidak dapat
	 * dihapus dengan menyetel nilai kosong. Perilaku ini identik dengan {@link #setOleh(String)}
	 * dan merupakan pola baku seluruh entity turunan {@link GeneralValueObject}.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null} atau kosong/spasi diabaikan diam-diam.
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
	 * <p><b>Hanya UPDATE.</b> Tidak ada {@code @PrePersist}, sehingga baris yang baru dibuat
	 * mengandalkan nilai awal field {@code tanggal_dirubah} (disetel
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat, yaitu waktu <i>instansiasi objek
	 * Java</i>, bukan waktu simpan) dan tidak pernah mendapat {@code oleh}/{@code olehId} dari
	 * jalur ini.</p>
	 *
	 * <p><b>Relevansi khusus untuk entity ini:</b> siklus "matikan semua lalu hidupkan yang
	 * tercentang" pada {@code GrupKategoriItemPenilaianSiswaAction#onSave(Event)} melewati callback
	 * ini <b>dua kali</b> untuk setiap kategori yang tetap tercentang (sekali saat dimatikan,
	 * sekali saat dihidupkan). Jadi {@code tanggal_dirubah} dan {@code oleh} pada baris yang
	 * sebenarnya <i>tidak berubah apa-apa</i> tetap ikut diperbarui setiap kali grup disimpan —
	 * jejak audit di sini tidak dapat dipakai untuk menyimpulkan "kapan kategori ini terakhir kali
	 * benar-benar diubah".</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya. Field
	 * {@code tanggal_dirubah} adalah stempel waktu perubahan terakhir, dipetakan ke kolom bertipe
	 * TIMESTAMP lewat {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi maupun penjaga null.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai kolom TIMESTAMP.
	 *
	 * @return waktu perubahan terakhir; untuk baris yang belum pernah di-UPDATE nilainya adalah
	 *         waktu objek Java ini dibuat, bukan waktu penyimpanan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Sisi "grup" dari pasangan penghubung — kolom FK {@code grup_kategori_item_penilaian_siswa},
	 * wajib. Menentukan cakupan tenant baris ini secara tidak langsung, karena hanya grup yang
	 * punya relasi {@code sekolah}/{@code yayasan}.
	 */
	private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;

	/**
	 * Sisi "kategori" dari pasangan penghubung — kolom FK {@code kategori_item_penilaian_siswa},
	 * wajib. Kategori inilah yang membawa butir-butir {@link JenisItemPenilaianSiswa} ke dalam
	 * grup.
	 */
	private KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa;

	/**
	 * Catatan bebas per baris penghubung. <b>Kolom mati:</b> tidak ada satu pun kode di repo yang
	 * memanggil {@link #setKeterangan(String)} maupun {@link #getKeterangan()} pada entity ini —
	 * dialog grup tidak menyediakan isiannya dan tidak ada layar lain yang menyentuh baris ini.
	 * Dideklarasikan ulang dari {@link GeneralValueObject} agar terpetakan Hibernate.
	 */
	private String keterangan;

	/**
	 * Saklar aktif — <b>satu-satunya penyimpan status "tercentang"</b> pada daftar
	 * <i>"Pilih Kategori Penilaian"</i>. Tidak ada operasi hapus di layar mana pun; melepas centang
	 * berarti menulis {@code false} ke kolom ini. Lihat uraian lengkap siklus mati-hidup pada
	 * Javadoc kelas dan pada {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Juga dipakai langsung oleh
	 * {@code GrupKategoriItemPenilaianSiswaAction} setiap kali sebuah kategori dicentang tetapi
	 * belum punya baris detail yang aktif (termasuk kasus "baris lama sudah mati" yang melahirkan
	 * duplikat — lihat Javadoc kelas). Seluruh field dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah}.
	 */
	public DetailGrupKategoriItemPenilaianSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolom dideklarasikan {@code insertable = false} karena nilainya dibangkitkan basis data
	 * (IDENTITY); jangan menyetelnya sendiri sebelum menyimpan.</p>
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
	 * Menyetel kunci primer. Hanya untuk dipakai Hibernate; kode aplikasi tidak boleh memanggilnya.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas baris ini apa adanya — tanpa {@code trim} dan tanpa penjaga
	 * null. Praktis selalu {@code null}: lihat catatan "kolom mati" pada field {@link #keterangan}.
	 *
	 * @return keterangan, atau {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas baris ini. Tanpa validasi. Tidak ada pemanggil di repo.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris penghubung, dengan <b>bawaan {@code true} bila kolomnya
	 * masih {@code null}</b>.
	 *
	 * <p>Bawaan tersebut bukan kosmetik. Ia sejalan dengan penyaring yang dipakai kesembilan
	 * pembaca SQL — {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif",
	 * true))} — sehingga baris warisan yang kolom {@code aktif}-nya belum pernah ditulis tetap
	 * dihitung sebagai tercentang, baik oleh Java maupun oleh SQL. Konsistensi ini <b>wajib
	 * dijaga</b>: "membersihkan" method ini menjadi pengembali nilai mentah akan mengubah arti
	 * seluruh baris lama.</p>
	 *
	 * <p><b>Peringatan bom waktu.</b> Nilai kolom ini ditulis ulang seluruhnya setiap kali grup
	 * induknya disimpan: {@code GrupKategoriItemPenilaianSiswaAction#onSave(Event)} mula-mula
	 * menyetel {@code false} pada SEMUA baris milik grup itu (tanpa penyaring apa pun), lalu
	 * menyetel {@code true} hanya untuk baris yang ada di peta pilihan. Peta itu dibangun ulang
	 * dari nol setiap kali dialog dibuka, disaring per sekolah/yayasan pada sisi checkbox tetapi
	 * <b>tidak</b> pada sisi peta, dan kosong sampai listener pengisinya berjalan. Akibatnya (rinci
	 * di Javadoc kelas): menyimpan grup untuk alasan apa pun dapat melenyapkan seluruh butir nilai
	 * grup itu dari rapor secara senyap, dan baris yang menunjuk kategori sekolah lain akan
	 * dihidupkan kembali selamanya tanpa cara melepasnya dari layar mana pun.</p>
	 *
	 * <p><b>Satu pembaca mengabaikan nilai ini:</b> mesin formula
	 * {@code ais.action.master.sekolah.util.GrupPenilaianUtil#hitung(...)} membaca seluruh baris
	 * dari cache in-memory dan tidak pernah memanggil method ini, sehingga baris yang sudah mati
	 * masih ikut menyubstitusi kode butir nilai ke dalam formula total.</p>
	 *
	 * @return {@code true} bila baris dianggap tercentang (termasuk saat kolomnya {@code null}),
	 *         {@code false} bila eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris penghubung. Menulis nilai mentah tanpa validasi — termasuk
	 * {@code null}, yang oleh {@link #getAktif()} akan dibaca kembali sebagai {@code true}.
	 *
	 * <p>Hanya ada dua pemanggil di seluruh repo, keduanya di dalam
	 * {@code GrupKategoriItemPenilaianSiswaAction#onSave(Event)}: {@code setAktif(false)} pada
	 * tahap "matikan semua" dan {@code setAktif(true)} pada tahap "hidupkan yang tercentang".
	 * Masing-masing diikuti {@code Common.refreshSaveOrUpdate(session, …)} dan
	 * {@code session.flush()}, sehingga setiap panggilan langsung menghasilkan UPDATE ke basis data
	 * <b>dan</b> satu revisi Envers.</p>
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai "aktif" saat dibaca kembali
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan grup pemilik baris penghubung ini (kolom FK
	 * {@code grup_kategori_item_penilaian_siswa}, {@code nullable = false}).
	 *
	 * <p><b>Efek samping yang disengaja:</b> method ini memanggil
	 * {@code check(grupKategoriItemPenilaianSiswa)} milik {@link GeneralValueObject} dan
	 * <b>menugaskan hasilnya kembali ke field</b>. Itu mekanisme resolusi proxy lazy standar di
	 * seluruh entity AIS: proxy yang belum terinisialisasi atau sudah <i>detached</i> diganti
	 * instance hidup dari cache atau dari session baru. Jadi getter ini bukan pembaca murni —
	 * ia dapat membuka session, melakukan query, dan mengubah isi field. Bila keempat sumber
	 * gagal, {@code check(...)} mengembalikan argumen apa adanya sehingga getter tidak pernah
	 * melempar exception.</p>
	 *
	 * <p>Relasi memakai {@code fetch = LAZY} dengan cascade {@code PERSIST}/{@code MERGE} — grup
	 * yang belum tersimpan akan ikut tersimpan, tetapi <b>tidak ada cascade REMOVE</b>: menghapus
	 * grup tidak akan menghapus baris penghubung ini.</p>
	 *
	 * @return grup kategori penilaian pemilik baris ini; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_kategori_item_penilaian_siswa", nullable = false)
	public GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa() {
		grupKategoriItemPenilaianSiswa = check(grupKategoriItemPenilaianSiswa);
		return grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Menyetel grup pemilik baris penghubung ini. Menulis nilai mentah tanpa validasi maupun
	 * penjaga null.
	 *
	 * <p>Dipanggil {@code GrupKategoriItemPenilaianSiswaAction#onSave(Event)} pada tahap
	 * "hidupkan yang tercentang", tepat sebelum baris disimpan — termasuk untuk baris yang sudah
	 * lama ada, yang karenanya di-<i>reattach</i> ke instance grup hasil {@code session.load(...)}
	 * pada penyimpanan itu.</p>
	 *
	 * @param grupKategoriItemPenilaianSiswa grup pemilik baru
	 */
	public void setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) {
		this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Mengembalikan kategori penilaian yang dipetakan baris ini ke dalam grup (kolom FK
	 * {@code kategori_item_penilaian_siswa}, {@code nullable = false}).
	 *
	 * <p>Sama seperti {@link #getGrupKategoriItemPenilaianSiswa()}, method ini memanggil
	 * {@code check(...)} dan <b>menugaskan hasilnya kembali ke field</b> — resolusi proxy lazy,
	 * dengan efek samping berupa kemungkinan query dan pembukaan session. Bukan pembaca murni.</p>
	 *
	 * <p>Inilah properti yang diproyeksikan seluruh pembaca lewat
	 * {@code Projections.groupProperty("kategoriItemPenilaianSiswa.id")}, dan yang dipakai
	 * renderer daftar grup untuk menampilkan nama kategori bernomor pada kolom ringkasan.</p>
	 *
	 * @return kategori penilaian yang ditunjuk baris ini; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_item_penilaian_siswa", nullable = false)
	public KategoriItemPenilaianSiswa getKategoriItemPenilaianSiswa() {
		kategoriItemPenilaianSiswa = check(kategoriItemPenilaianSiswa);
		return kategoriItemPenilaianSiswa;
	}

	/**
	 * Menyetel kategori penilaian yang dipetakan baris ini. Menulis nilai mentah tanpa validasi.
	 *
	 * <p>Dipanggil {@code GrupKategoriItemPenilaianSiswaAction} saat membangun daftar checkbox:
	 * setiap baris — baik yang diambil dari peta pilihan tersimpan maupun objek
	 * {@code new DetailGrupKategoriItemPenilaianSiswa()} yang baru dibuat — selalu di-<i>set</i>
	 * kategorinya sebelum ditempelkan sebagai atribut checkbox. Perhatikan bahwa pada baris lama
	 * hal ini menulis ulang nilai dengan instance kategori yang sama, sehingga tidak mengubah
	 * data.</p>
	 *
	 * @param kategoriItemPenilaianSiswa kategori penilaian baru
	 */
	public void setKategoriItemPenilaianSiswa(KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa) {
		this.kategoriItemPenilaianSiswa = kategoriItemPenilaianSiswa;
	}

	/**
	 * Nomor urut turunan: meneruskan {@code nomorUrut} milik kategori yang ditunjuk, dengan
	 * bawaan {@code 1} bila kategori itu tidak memilikinya dan {@code 0} bila relasi kategorinya
	 * belum terisi.
	 *
	 * <p><b>Beberapa hal penting yang tidak terlihat dari kodenya:</b></p>
	 * <ul>
	 * <li><b>Nilainya praktis konstan.</b> {@link ais.database.model.sekolah.KategoriItemPenilaianSiswa}
	 * tidak mendeklarasikan ulang {@code nomorUrut}; properti itu hanya ada di
	 * {@link GeneralValueObject}, yang bukan {@code @Entity}/{@code @MappedSuperclass} sehingga
	 * <b>tidak pernah dipetakan maupun dimuat Hibernate</b> untuk kategori. Akibatnya
	 * {@code kategoriItemPenilaianSiswa.getNomorUrut()} selalu {@code null}, dan method ini praktis
	 * selalu mengembalikan {@code 1} (atau {@code 0} untuk baris yatim tanpa kategori). Urutan
	 * tampil butir nilai yang sesungguhnya berasal dari {@code Order.asc("kategoriItemPenilaianSiswa.kode")}
	 * lalu {@code Order.asc("nomorUrut")} milik {@link JenisItemPenilaianSiswa} — bukan dari
	 * sini.</li>
	 * <li><b>Tidak ada pemanggil.</b> Tidak ada satu pun kode di repo yang memanggil method ini
	 * pada entity ini; kembarannya di {@link DetailGrupPenilaian} juga tidak. Keduanya salinan pola
	 * yang sama.</li>
	 * <li><b>Tetapi Hibernate memakainya.</b> Method ini <b>tidak</b> diberi {@code @Transient},
	 * sedangkan setter-nya ({@code setNomorUrut}) tersedia lewat pewarisan dari
	 * {@link GeneralValueObject}. Hibernate karena itu memperlakukan {@code nomorUrut} sebagai
	 * properti persisten kelas ini: kolomnya ikut dibuat pada tabel utama dan pada tabel audit
	 * Envers, ditulis dengan nilai konstan di atas pada setiap simpan, dan saat dimuat kembali
	 * nilainya dituliskan ke field milik {@link GeneralValueObject} yang <b>tidak pernah dibaca
	 * method ini</b>. Efektifnya sebuah kolom tulis-saja. Menambahkan {@code @Transient} akan
	 * mengubah skema, jadi jangan dilakukan tanpa migrasi.</li>
	 * <li><b>Melewati {@code check(...)}.</b> Berbeda dengan kedua getter relasi di atas, method
	 * ini membaca field {@code kategoriItemPenilaianSiswa} <b>secara langsung</b>, tanpa resolusi
	 * proxy lazy. Pada objek yang sudah <i>detached</i> pemanggilan
	 * {@code kategoriItemPenilaianSiswa.getNomorUrut()} dapat memicu inisialisasi proxy dan
	 * gagal.</li>
	 * <li><b>Merusak urutan alami.</b> Karena hasilnya tidak pernah {@code null},
	 * {@code GeneralValueObject#compareTo(GeneralValueObject)} selalu berhenti pada cabang
	 * pertamanya dan selalu mengembalikan {@code 0} untuk sepasang baris mana pun — lihat catatan
	 * {@code TreeSet} pada Javadoc kelas.</li>
	 * </ul>
	 *
	 * @return nomor urut kategori bila ada; {@code 1} bila kategori tidak memilikinya (kasus yang
	 *         berlaku dalam praktik), atau {@code 0} bila relasi kategori belum terisi
	 */
	public Integer getNomorUrut() {
		Integer nomorUrut = 0;
		if (kategoriItemPenilaianSiswa != null) {
			nomorUrut = kategoriItemPenilaianSiswa.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}



}
