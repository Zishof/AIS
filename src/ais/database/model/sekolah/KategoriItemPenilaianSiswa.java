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
 * Master <b>Kategori Penilaian</b> pada modul sekolah — sebuah <i>rumpun</i> yang menaungi
 * sekumpulan butir nilai ({@link JenisItemPenilaianSiswa}), dipetakan ke tabel
 * {@code sekolah.kategori_item_penilaian_siswa}.
 *
 * <p>Isinya sangat ramping: hanya {@code kode}, {@code nama}, {@code keterangan}, saklar
 * {@code aktif}, dan sepasang relasi cakupan {@link Sekolah}/{@link Yayasan}. Tidak ada bobot,
 * formula, batas nilai, maupun koleksi anak apa pun di sini — semuanya milik lapis lain (lihat
 * pembagian peran di bawah). Judul dialog pengelolanya adalah <i>"Tambah Kategori Penilaian"</i> /
 * <i>"Ubah Kategori Penilaian"</i>, dengan isian berlabel <i>"Kode Kategori Penilaian"</i>,
 * <i>"Nama Kategori Penilaian"</i>, <i>"Yayasan *"</i>, <i>"Sekolah *"</i> dan
 * <i>"Keterangan"</i> ({@code ais.action.master.sekolah.KategoriItemPenilaianSiswaAction#init}).
 *
 * <h3>Posisi TERVERIFIKASI dalam rantai penilaian rapor sekolah</h3>
 * Rantai berikut dipastikan dari deklarasi kolom FK pada masing-masing entity penghubung (bukan
 * dari kemiripan nama kelas). Semua penghubung memakai pola "tabel silang + kolom {@code aktif}",
 * bukan {@code @ManyToMany}:
 * <ol>
 * <li>{@link JenisPenilaian} (tabel {@code jenis_penilaian}) — payung paling atas, sekaligus
 * <b>layar induk</b> seluruh master penilaian sekolah.</li>
 * <li>{@link DetailJenisPenilaian} (tabel {@code detail_jenis_penilaian_grup}) —
 * {@code jenisPenilaian} &harr; {@code grupPenilaian}.</li>
 * <li>{@link GrupPenilaian} (tabel {@code grup_penilaian}) — pemilik {@code formula},
 * {@code jenisNilaiHuruf}, {@code adaTotal}, {@code khususTingkat}/{@code khususSemester}.</li>
 * <li>{@link DetailGrupPenilaian} (tabel {@code detail_grup_penilaian_data}) —
 * {@code grupPenilaian} &harr; {@code grupKategoriItemPenilaianSiswa}.</li>
 * <li>{@link GrupKategoriItemPenilaianSiswa} (tabel {@code grup_kategori_item_penilaian_siswa}) —
 * pemilik {@code formula} kategori, {@code nilaiBolehDinputOlehGuru}, dan pembatas
 * {@code khususTingkat}/{@code khususSemester}. Inilah lapis yang benar-benar dipegang mesin
 * hitung nilai.</li>
 * <li>{@link DetailGrupKategoriItemPenilaianSiswa} (tabel
 * {@code detail_grup_kategori_item_penilaian_siswa}, kolom FK
 * {@code kategori_item_penilaian_siswa}) — {@code grupKategoriItemPenilaianSiswa} &harr;
 * <b>entity ini</b>.</li>
 * <li><b>{@code KategoriItemPenilaianSiswa}</b> — kelas ini.</li>
 * <li>{@link JenisItemPenilaianSiswa} (tabel {@code jenis_item_penilaian_siswa}) — butir nilai
 * konkret; menunjuk balik ke entity ini lewat kolom {@code kategori_item_penilaian_siswa}.
 * Di sinilah tipe isian, {@code nilaiMin}/{@code nilaiMax}, {@code formula}, {@code wajibDiisi},
 * {@code nomorUrut} dan seterusnya berada.</li>
 * </ol>
 *
 * <p><b>Keluarga ini TERPISAH dari {@link JenisNilaiSiswa}.</b> {@code JenisNilaiSiswa} adalah
 * profil template cetak JasperReports (lihat dokumentasi kelas tersebut), bukan bagian dari rantai
 * di atas; kemiripan nama sepenuhnya kebetulan.
 *
 * <h3>Cara entity ini benar-benar dibaca saat runtime</h3>
 * Seluruh pembaca memakai pola dua langkah yang identik — <b>tidak satu pun</b> yang menanyakan
 * tabel ini secara langsung untuk perhitungan:
 * <ol>
 * <li>Ambil id kategori yang tercentang pada sebuah grup:
 * {@code createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
 * .add(eq("grupKategoriItemPenilaianSiswa", …))
 * .add(or(isNull("aktif"), eq("aktif", true)))
 * .setProjection(groupProperty("kategoriItemPenilaianSiswa.id"))}, lalu dibungkus
 * {@code ConstantValues.simpleList(…, KategoriItemPenilaianSiswa.class, false)} menjadi daftar
 * <i>stub</i> yang hanya berisi {@code id}.</li>
 * <li>Pakai daftar stub itu sebagai {@code Restrictions.in("kategoriItemPenilaianSiswa", …)} untuk
 * menarik {@link JenisItemPenilaianSiswa}, <b>diurutkan
 * {@code Order.asc("kategoriItemPenilaianSiswa.kode")} lalu {@code Order.asc("nomorUrut")}</b>.</li>
 * </ol>
 *
 * <p>Konsekuensi yang mudah terlewat: <b>{@link #getKode()} adalah kunci urut PRIMER seluruh
 * kolom nilai</b> pada formulir input maupun rapor. Mengubah kode sebuah kategori akan
 * memindahkan seluruh kolom butir nilainya ke posisi lain di layar, di rapor, di rekap, dan di
 * respons API — tanpa peringatan apa pun.
 *
 * <p>Delapan pemanggil pola tersebut sudah diverifikasi: {@code ais.common.GradingHelper} (mesin
 * hitung ulang nilai massal), {@code ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper}
 * dan {@code …DetailPenilaianLesSiswaHelper} (layar isi nilai),
 * {@code ais.action.master.helper.TugasMandiriHelper} dan {@code TugasKelompokHelper} (pemetaan
 * nilai tugas e-learning), {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} dan
 * {@code LaporanRekapTotalNilai} (cetak rapor/rekap), serta REST
 * {@code ais.action.servlet.api.NilaiSiswaApi} dan {@code ElearningApiUtil}. Entity ini
 * <b>hidup penuh</b>, bukan sisa skema lama.
 *
 * <p>Nilai siswanya sendiri <b>tidak</b> disimpan di rantai master ini, melainkan terdenormalisasi
 * ke kolom teks pada {@code KelasSiswaPunyaSiswa} / {@link KelasLesSiswaPunyaSiswa} melalui
 * {@code populateDetailNilai(jenisItem, matapelajaran, grupKategori, nilai, sesuai, semester)}.
 * Perhatikan kuncinya: {@code jenisItem} + {@code grupKategori} — <b>kategori tidak ikut menjadi
 * kunci penyimpanan nilai</b>, perannya murni pengelompokan dan pengurutan.
 *
 * <h3>Layar pengelola dan konsekuensi hak akses</h3>
 * Layar CRUD-nya {@code /pages/master/sekolah/kategori_item_penilaian_siswa.zul}
 * ({@code ais.action.master.sekolah.KategoriItemPenilaianSiswaAction}). Layar itu <b>tidak
 * terdaftar sebagai menu mandiri</b>: satu-satunya penyisipannya di seluruh repo adalah tab
 * <i>Kategori Penilaian</i> di dalam layar Jenis Penilaian
 * ({@code JenisPenilaianAction#onKategoriPenilaian(Event)} menyisipkannya lewat {@code MyInclude}).
 * Karena {@code CommonPrivilages.checkPrevilages(...)} selalu mengacu ke
 * {@code Common.getCurrentMenu()}, seluruh hak CREATE/UPDATE/DELETE yang ditegakkan di layar ini
 * sesungguhnya adalah hak pada menu <b>Jenis Penilaian</b> — memberi seseorang hak ubah Jenis
 * Penilaian otomatis memberinya hak ubah dan hapus seluruh master kategori (juga master Grup
 * Kategori, Grup Penilaian, Jenis Item, dan Nilai Huruf yang menempel sebagai tab bersaudara).
 * Ini mekanisme <i>pewarisan hak lewat menu induk</i> yang sama dengan yang tercatat pada
 * {@link PaketPsb}.
 *
 * <p>Sisi positifnya perlu dicatat jujur: berbeda dengan banyak layar master sekolah lain,
 * {@code KategoriItemPenilaianSiswaAction} <b>memang memanggil</b>
 * {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} dan memakai hasilnya untuk
 * menyembunyikan tombol Tambah, menonaktifkan checkbox <i>Aktif</i> per baris, serta menyembunyikan
 * tombol Ubah/Hapus. Tidak ada {@code edit}/{@code delete} yang di-<i>hardcode</i> di sini.
 *
 * <h3>Cakupan tenant (sekolah/yayasan)</h3>
 * Kedua relasi tenant boleh {@code null}, dan itu <b>bermakna</b>: kategori dengan
 * {@code sekolah == null} sengaja ikut terbawa ke setiap sekolah. Penyaring pemakainya —
 * {@code GrupKategoriItemPenilaianSiswaAction} saat menyusun daftar centang <i>"Pilih Kategori
 * Penilaian"</i> — memakai {@code isNull("sekolah") OR eq("sekolah", s)} (idem yayasan).
 *
 * <p>Layar masternya sendiri <b>tidak</b> menyaring tenant sama sekali:
 * {@code KategoriItemPenilaianSiswaAction#initCriteria(boolean)} hanya menambahkan pembatas
 * sekolah/yayasan bila combo pencarian kebetulan terisi, dan memakai
 * {@code Restrictions.sqlRestriction("1=1")} bila kosong — sedangkan combo itu diisi
 * {@code Common.initYayasanDanSekolahDanSemua(...)} yang menyediakan pilihan "=Semua=". Jadi hak
 * READ pada menu Jenis Penilaian sudah cukup untuk melihat — dan mengekspor lewat tombol cetak
 * dengan kolom {@code id, kode, nama, keterangan, yayasan, sekolah, aktif} — katalog kategori
 * SELURUH sekolah dan yayasan pada satu instalasi, lengkap dengan tombol Ubah/Hapus per baris bila
 * hak UPDATE/DELETE dimiliki. Keparahannya <b>rendah</b> (metadata katalog, bukan data pribadi
 * siswa) dan mekanismenya sama dengan kelas temuan "nol filter tenant" yang sudah tercatat pada
 * audit repo ini, namun permukaan <b>tulis</b> lintas tenant tetap nyata.
 *
 * <h3>Perilaku saklar {@code aktif} — jebakan yang wajib dipahami</h3>
 * <ul>
 * <li><b>Mesin nilai TIDAK PERNAH membaca {@code aktif} milik kategori.</b> Delapan pemanggil
 * runtime di atas hanya menyaring {@code aktif} pada {@link DetailGrupKategoriItemPenilaianSiswa}
 * dan pada {@link JenisItemPenilaianSiswa}. Menonaktifkan sebuah kategori karena itu <b>tidak
 * berpengaruh apa pun</b> pada formulir isi nilai, rapor, rekap, maupun API — butir-butirnya tetap
 * ikut dihitung.</li>
 * <li><b>Efeknya tertunda dan mengejutkan.</b> Kategori non-aktif dibuang dari daftar centang
 * <i>maupun</i> dari peta pilihan tersimpan di {@code GrupKategoriItemPenilaianSiswaAction} (query
 * pemuat peta menyaring {@code kategoriItemPenilaianSiswa.aktif}). Sementara itu
 * {@code onSave(...)}-nya mula-mula menyetel <b>seluruh</b> baris detail grup menjadi
 * {@code aktif=false}, lalu menghidupkan kembali hanya yang ada di peta. Akibatnya: menonaktifkan
 * kategori hari ini tidak mengubah rapor sama sekali, tetapi begitu ada orang menyimpan grup itu
 * lain waktu — bahkan hanya untuk mengubah formulanya — pemetaan kategori tersebut ikut mati
 * permanen dan seluruh butir nilainya lenyap senyap dari rapor.</li>
 * <li><b>"Kategori hantu" lintas sekolah.</b> Daftar centang disaring per sekolah/yayasan, tetapi
 * peta pilihan tersimpan tidak. Baris detail yang menunjuk kategori milik sekolah lain karena itu
 * tetap dihidupkan kembali saat menyimpan meskipun tidak pernah tampil sebagai checkbox — aktif,
 * ikut menyumbang nilai, dan tidak dapat dilepas dari layar mana pun.</li>
 * <li><b>Combobox memakai penyaring yang lebih ketat.</b>
 * {@code JenisItemPenilaianSiswaAction} mengisi combo <i>"=Semua Kategori="</i> dan
 * <i>"=Tanpa Kategori="</i> dengan {@code Restrictions.eq("aktif", true)} — <b>tanpa</b> cabang
 * {@code isNull} yang dipakai di tempat lain. Baris dengan kolom {@code aktif} bernilai NULL akan
 * hilang dari kedua combo itu. Saat ini hal tersebut tidak terjadi karena {@link #getAktif()}
 * mengubah {@code null} menjadi {@code true} sebelum Hibernate membaca propertinya, sehingga
 * kolomnya selalu tersimpan terisi. Ini <b>ketergantungan tersembunyi</b>: "membersihkan"
 * {@code getAktif()} menjadi pengembali nilai mentah akan membuat kategori raib dari combo
 * tersebut.</li>
 * </ul>
 *
 * <h3>Hal non-obvious lain sebelum mengubah berkas ini</h3>
 * <ul>
 * <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * TIDAK memetakan properti apa pun miliknya. Maka {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}, dan juga {@code kode}/{@code nama}/{@code keterangan} <b>harus</b>
 * dideklarasikan ulang di sini agar terpetakan. Ini KEHARUSAN TEKNIS, bukan duplikasi yang bisa
 * "dirapikan".</li>
 * <li><b>Akibat sampingannya: field induk selamanya kosong.</b> Field lokal
 * {@code kode}/{@code nama}/{@code keterangan} membayangi ({@code shadow}) field bernama sama di
 * induk. Kode apa pun yang membaca field induk secara langsung (bukan lewat getter) akan mendapat
 * {@code null}. Saat ini aman karena seluruh method induk — termasuk
 * {@code GeneralValueObject#compareTo(GeneralValueObject)} — mengaksesnya lewat getter yang
 * di-override kelas ini.</li>
 * <li><b>Tiga kontrak null yang berbeda dalam satu kelas.</b> {@link #getKode()} tidak pernah
 * mengembalikan {@code null} (mengembalikan {@code ""}), {@link #getNama()} boleh {@code null},
 * dan {@link #getKeterangan()} mengembalikan nilai mentah tanpa {@code trim} maupun penjaga null.
 * Ketiganya harus diperlakukan berbeda oleh pemanggil.</li>
 * <li><b>{@link #toString()} melewati getter.</b> Ia membaca field {@code nama} langsung, jadi
 * hasilnya tidak ter-{@code trim} dan menjadi {@code "…-null"} bila nama belum terisi.</li>
 * <li><b>Deklarasi {@code nama}/{@code kode} berada SETELAH {@link #toString()}.</b> Sah secara
 * Java, tetapi mudah membuat pembaca mengira {@code toString()} memakai field induk. Tidak.</li>
 * <li><b>Baris {@code aktif} tak pernah ditulis oleh dialog Tambah/Ubah</b> —
 * {@code onSave(...)} hanya menulis kode, nama, keterangan, sekolah, yayasan. Kolom {@code aktif}
 * hanya berubah lewat checkbox pada grid daftar (atau lewat unggah Excel). Yang menyelamatkan
 * keadaan adalah nilai bawaan {@code true} dari {@link #getAktif()}, lihat catatan saklar
 * {@code aktif} di atas.</li>
 * <li><b>Kode mati di luar berkas ini.</b> Ada LIMA tempat yang membuat
 * {@code new KategoriItemPenilaianSiswa()} lalu {@code setId(-1L)} dan <b>tidak pernah
 * memakainya lagi</b>: {@code ais.action.master.GelombangPendaftaranAction} (1),
 * {@code ais.action.master.MatakuliahAction} (3), dan
 * {@code ais.action.master.sekolah.GelombangPendaftaranPsbAction} (1). Semuanya residu salin-tempel
 * dari sebuah template pembangun daftar checkbox; jangan dijadikan petunjuk bahwa entity ini
 * dipakai di modul PMB/matakuliah — tidak.</li>
 * <li><b>Terlindung dari pembersihan cache.</b> Nama kelas ini terdaftar di
 * {@code ais.common.DataUtil.CLASS_JANGAN_DIBERSIHKAN}, sehingga instance-nya tidak dibuang oleh
 * pembersihan berkala; instance yang beredar sering sudah <i>detached</i> — inilah alasan
 * {@link #getSekolah()} dan {@link #getYayasan()} memanggil
 * {@code GeneralValueObject#check(Object)}. Kelasnya juga disiapkan saat boot lewat
 * {@code ais.common.InitData} (pembuatan/penyelarasan skema), bukan penyemaian baris — tidak ada
 * kategori bawaan sama sekali, instalasi baru mulai dengan tabel kosong.</li>
 * </ul>
 *
 * <h3>Verifikasi pola arsitektur berulang milik repo ini</h3>
 * <ul>
 * <li><b>Getter destruktif/write-back</b> — <b>ADA, dua tingkat.</b> {@link #getSekolah()} menulis
 * balik hasil {@code check(...)} (ringan, sekadar de-proxy); {@link #getYayasan()} lebih jauh lagi:
 * ia menimpa field {@code sekolah} <i>dan</i> menurunkan ulang {@code yayasan} dari
 * {@code sekolah.getYayasan()} pada setiap pembacaan.</li>
 * <li><b>{@code getKeterangan()} membalik kontrak induk</b> — <b>ADA.</b> Induk menjamin tidak
 * pernah {@code null}; override di sini mengembalikan nilai mentah. Lihat
 * {@link #getKeterangan()}.</li>
 * <li><b>{@code compareTo()} dipangkas</b> — <b>TIDAK ADA.</b> Kelas ini tidak meng-override
 * {@code compareTo}/{@code equals}/{@code hashCode}; semuanya diwarisi apa adanya. Yang tersisa
 * hanyalah efek tidak langsung dari poin sebelumnya (cabang {@code keterangan} pada
 * {@code compareTo} induk kehilangan jaminan non-null-nya).</li>
 * <li><b>Penciutan {@code TreeSet}</b> — <b>TIDAK ADA.</b> Entity ini tidak punya koleksi apa pun,
 * dan seluruh pemanggil menampungnya dalam {@code List} hasil
 * {@code ConstantValues.simpleList(...)} dengan pengurutan dikerjakan SQL
 * ({@code addOrder(Order.asc("kode"))}), bukan {@code TreeSet}/{@code TreeMap}.</li>
 * <li><b>Fail-open cakupan tenant</b> — <b>TIDAK PERSIS.</b> Di sini bukan penyaring yang gagal
 * terbuka, melainkan memang <b>tidak ada penyaring tenant sama sekali</b> pada
 * {@code initCriteria(...)} layar master (lihat bagian Cakupan tenant). Varian yang sama pernah
 * dicatat pada {@code RuangPSB}/{@code KelasSiswaPSB}.</li>
 * </ul>
 *
 * @see JenisItemPenilaianSiswa
 * @see DetailGrupKategoriItemPenilaianSiswa
 * @see GrupKategoriItemPenilaianSiswa
 * @see GrupPenilaian
 * @see JenisPenilaian
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kategori_item_penilaian_siswa")
public class KategoriItemPenilaianSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>tidak unik</b> di repo ini — konstanta yang sama
	 * ({@code 2463821577548439808L}) juga dipakai {@link GrupKategoriItemPenilaianSiswa}, sisa
	 * penyalinan berkas antar entity. Tidak berdampak selama kedua kelas tidak pernah saling
	 * di-deserialisasi silang, tetapi jangan dijadikan penanda identitas kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci primer, kolom {@code id}. Dideklarasikan ulang (tidak diwarisi) karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate. Lihat {@link #getId()}.
	 */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis oleh interceptor audit. */
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
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat, yaitu waktu <i>instansiasi
	 * objek Java</i>, bukan waktu simpan) dan tidak pernah mendapat {@code oleh}/{@code olehId}
	 * dari jalur ini.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.
	 * Field {@code tanggal_dirubah} adalah stempel waktu perubahan terakhir, dipetakan ke kolom
	 * bertipe TIMESTAMP lewat {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya <b>tidak</b> dipanggil kode aplikasi —
	 * pengisiannya diserahkan ke {@link #onUpdate()}; pakai setter ini hanya untuk migrasi atau
	 * pemulihan data historis.
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diizinkan (kolomnya nullable)
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; untuk baris yang belum pernah di-UPDATE isinya adalah
	 *         waktu objek Java-nya dibuat, bukan waktu INSERT
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berformat {@code "<id>-<nama>"}.
	 *
	 * <p><b>Membaca field secara langsung, bukan lewat getter.</b> Karena itu hasilnya tidak
	 * ter-{@code trim} dan akan berbunyi {@code "null-null"} untuk objek yang baru dibuat, atau
	 * {@code "12-null"} bila nama belum diisi. Dipakai antara lain oleh keluaran {@code println}
	 * diagnostik pada {@code DetailPenilaianSiswaHelper} saat mencetak daftar id kategori.</p>
	 *
	 * @return gabungan id dan nama yang dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode kategori. <b>Kunci urut PRIMER seluruh butir nilai</b> — lihat dokumentasi kelas.
	 * Tidak ada batasan unik pada kolom ini dan tidak ada validasi wajib isi pada layar master.
	 */
	private String kode;

	/**
	 * Nama kategori sebagaimana tampil di layar dan laporan. Satu-satunya isian yang divalidasi
	 * wajib oleh {@code KategoriItemPenilaianSiswaAction#onSave(Event)}.
	 */
	private String nama;

	/** Keterangan bebas; dipakai sebagai teks deskripsi pada combobox pemilih kategori. */
	private String keterangan;

	/**
	 * Saklar aktif. Perhatikan: <b>tidak pernah dibaca mesin hitung nilai</b> — lihat bagian
	 * "Perilaku saklar {@code aktif}" pada dokumentasi kelas sebelum mengandalkannya.
	 */
	private Boolean aktif;

	/** Cakupan sekolah; {@code null} berarti berlaku untuk semua sekolah. */
	private Sekolah sekolah;

	/**
	 * Cakupan yayasan; {@code null} berarti berlaku untuk semua yayasan. Nilainya diturunkan ulang
	 * dari {@code sekolah} setiap kali dibaca — lihat {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh field dibiarkan kosong
	 * kecuali {@code tanggal_dirubah} yang langsung diisi waktu saat ini oleh inisialisasi
	 * field-nya.
	 */
	public KategoriItemPenilaianSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Kolomnya {@code insertable = false} dengan strategi {@link javax.persistence.GenerationType#IDENTITY}
	 * — nilai dibangkitkan basis data ({@code serial}/{@code identity}), sehingga id bersifat
	 * <b>berurutan dan mudah ditebak</b>. Anotasi {@code @Id} yang menempel pada getter inilah yang
	 * menetapkan seluruh entity ini memakai <i>property access</i>: Hibernate membaca dan menulis
	 * state lewat getter/setter, bukan lewat field. Itulah sebabnya efek samping pada
	 * {@link #getAktif()}, {@link #getSekolah()} dan {@link #getYayasan()} ikut terbawa ke nilai
	 * yang benar-benar tersimpan.</p>
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
	 * Menyetel kunci primer. Dipakai Hibernate saat memuat baris, dan oleh kode yang membangun
	 * <i>stub</i> berisi id saja (pola {@code ConstantValues.simpleList(..., false)} yang dipakai
	 * seluruh pembaca entity ini).
	 *
	 * @param id kunci primer; {@code null} untuk objek yang belum tersimpan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode kategori dalam bentuk sudah di-{@code trim}.
	 *
	 * <p><b>Tidak pernah {@code null}</b>: nilai {@code null} dipetakan menjadi string kosong.
	 * Kontrak ini berbeda dari {@link #getNama()} (boleh {@code null}) maupun
	 * {@link #getKeterangan()} (mentah) di kelas yang sama — perbedaan yang mudah menjebak.</p>
	 *
	 * <p>Kolom yang mendasarinya adalah kunci urut primer pada seluruh query butir nilai
	 * ({@code Order.asc("kategoriItemPenilaianSiswa.kode")}), namun pengurutan itu dikerjakan SQL
	 * pada nilai mentah — normalisasi di sini hanya berlaku untuk pemakaian di sisi Java.</p>
	 *
	 * @return kode kategori tanpa spasi tepi, atau {@code ""} bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode kategori apa adanya (tanpa {@code trim}, tanpa validasi, tanpa pengecekan
	 * duplikat). Dipanggil dari dialog Tambah/Ubah dan dari jalur unggah Excel.
	 *
	 * @param kode kode kategori; {@code null} diizinkan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama kategori dalam bentuk sudah di-{@code trim}.
	 *
	 * <p>Berbeda dari {@link #getKode()}, method ini <b>mempertahankan {@code null}</b>: bila field
	 * kosong yang dikembalikan {@code null}, bukan {@code ""}. Nilai ini juga dipakai
	 * {@code GeneralValueObject#compareTo(GeneralValueObject)} sebagai kunci urut ketiga, dan oleh
	 * {@code RevisiHelper.createNewRevisi(...)} sebagai label baris pada grid master.</p>
	 *
	 * <p>Kolomnya dideklarasikan {@code nullable = false} sepanjang 255 karakter, jadi pada
	 * praktiknya baris yang tersimpan lewat ORM selalu bernama.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori apa adanya (tanpa {@code trim}). Validasi "harus diisi" ada di layar
	 * ({@code onSave}), bukan di sini.
	 *
	 * @param nama nama kategori; {@code null} diizinkan oleh method ini meski kolomnya
	 *             {@code NOT NULL}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas <b>apa adanya</b>.
	 *
	 * <p><b>Membalik kontrak induk.</b> {@code GeneralValueObject#getKeterangan()} menjamin tidak
	 * pernah mengembalikan {@code null} (mengembalikan {@code ""}); override di sini mengembalikan
	 * nilai mentah tanpa {@code trim} dan tanpa penjaga null. Konsekuensi konkretnya: cabang
	 * terakhir {@code GeneralValueObject#compareTo(GeneralValueObject)} — yang di induk selalu
	 * dapat dipakai — di kelas ini bisa gugur, sehingga dua kategori tanpa nama dan tanpa
	 * keterangan akan dianggap setara ({@code compareTo} mengembalikan {@code 0}). Efek itu tidak
	 * berbahaya di sini karena semua pemanggil memakai {@code List} dengan pengurutan SQL, bukan
	 * {@code TreeSet}.</p>
	 *
	 * <p>Pemanggil yang merangkai keterangan ke dalam teks (mis. deskripsi item combobox lewat
	 * {@code Common.insertComboDanSemua(..., "keterangan", ...)}) harus siap menerima
	 * {@code null}.</p>
	 *
	 * @return keterangan mentah, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas apa adanya.
	 *
	 * @param keterangan keterangan; {@code null} diizinkan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif, dengan <b>nilai bawaan {@code true}</b> bila field belum terisi.
	 *
	 * <p>Karena entity ini memakai <i>property access</i> (lihat {@link #getId()}), Hibernate
	 * membaca nilai properti lewat method ini juga. Artinya baris yang disimpan tanpa pernah
	 * menyentuh saklar aktif tetap menuliskan {@code true} ke kolomnya — bukan NULL — dan baris
	 * lama ber-NULL akan "sembuh sendiri" menjadi {@code true} pada UPDATE berikutnya. Perilaku
	 * itulah yang membuat penyaring ketat {@code Restrictions.eq("aktif", true)} pada combobox
	 * kategori di {@code JenisItemPenilaianSiswaAction} bekerja; mengubah method ini menjadi
	 * pengembali nilai mentah akan menyembunyikan kategori dari combobox tersebut.</p>
	 *
	 * <p><b>Jangkauan saklar ini jauh lebih sempit dari yang terlihat</b> — mesin hitung nilai,
	 * rapor, rekap, dan REST API tidak pernah membacanya. Lihat bagian "Perilaku saklar
	 * {@code aktif}" pada dokumentasi kelas.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} hanya bila
	 *         dinonaktifkan secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif. Satu-satunya pemanggil di layar master adalah listener {@code onCheck}
	 * pada checkbox <i>"Aktif"</i> di grid daftar (yang langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)}, jadi perubahan tersimpan seketika tanpa tombol
	 * Simpan); dialog Tambah/Ubah tidak pernah memanggilnya.
	 *
	 * @param aktif status baru; {@code null} akan dibaca sebagai {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sekolah pemilik kategori ini, sekaligus <b>menulis balik</b> hasilnya ke field
	 * ({@code sekolah = check(sekolah)}).
	 *
	 * <p>{@code GeneralValueObject#check(Object)} menyelesaikan proxy lazy yang mungkin sudah
	 * <i>detached</i> — perlu di sini karena kelas ini terdaftar pada
	 * {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN} sehingga instance-nya berumur panjang. Method
	 * tersebut tidak pernah melempar exception dan tidak pernah mengembalikan {@code null} untuk
	 * argumen non-null; kegagalan resolusi bersifat senyap.</p>
	 *
	 * <p>Efek sampingnya ringan (hanya mengganti referensi objek dengan versi yang teresolusi),
	 * tetapi tetap membuat getter ini <b>tidak murni</b> — memanggilnya mengubah state objek.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila kategori berlaku untuk semua sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan normalisasi: objek yang {@code null} <b>atau yang belum
	 * punya id</b> disimpan sebagai {@code null}.
	 *
	 * <p>Aturan kedua itu penting — ia mencegah tersimpannya {@code Sekolah} hasil {@code new}
	 * yang belum persist (mis. item combobox pembungkus pilihan "=Semua="), yang jika lolos akan
	 * ikut ter-{@code cascade} PERSIST dan membuat baris sekolah palsu.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id diartikan "semua sekolah"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik kategori ini — <b>getter destruktif</b>.
	 *
	 * <p>Berbeda dari {@link #getSekolah()} yang sekadar menulis balik hasil de-proxy, method ini
	 * benar-benar <b>menurunkan ulang</b> nilainya: bila {@code getSekolah()} tidak {@code null},
	 * field {@code yayasan} <b>ditimpa</b> dengan {@code sekolah.getYayasan()}. Ia juga menulis
	 * ulang field {@code sekolah} sebagai efek samping dari pemanggilan {@code getSekolah()}.</p>
	 *
	 * <p>Konsekuensi nyata: nilai apa pun yang disetel lewat {@link #setYayasan(Yayasan)} bersifat
	 * sementara selama {@code sekolah} terisi — termasuk pilihan yayasan pada dialog Tambah/Ubah.
	 * Karena entity memakai <i>property access</i>, nilai yang benar-benar tersimpan ke kolom
	 * {@code yayasan_id} juga hasil penurunan ini, bukan pilihan pengguna. Kolom itu efektifnya
	 * adalah <b>denormalisasi</b> dari {@code sekolah.yayasan}, dan hanya bisa berbeda bila
	 * {@code sekolah} kosong.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila kategori berlaku untuk semua yayasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik, dengan normalisasi yang sama seperti
	 * {@link #setSekolah(Sekolah)}: objek {@code null} atau yang belum punya id disimpan sebagai
	 * {@code null}.
	 *
	 * <p>Ingat bahwa nilai yang disetel di sini akan <b>ditimpa</b> {@link #getYayasan()} selama
	 * {@code sekolah} terisi.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id diartikan "semua yayasan"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

}
