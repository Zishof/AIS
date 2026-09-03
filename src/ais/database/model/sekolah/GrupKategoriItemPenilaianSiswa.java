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
import org.json.JSONArray;

import ais.database.model.GeneralValueObject;

/**
 * <b>Grup Kategori Penilaian</b> &mdash; simpul TENGAH rantai penilaian rapor modul sekolah,
 * dipetakan ke tabel {@code sekolah.grup_kategori_item_penilaian_siswa}.
 *
 * <p>Dalam bahasa layar, satu baris kelas ini adalah <i>satu tab kolom nilai</i> pada formulir isi
 * nilai/rapor (misalnya "Pengetahuan", "Keterampilan", "Sikap", "UTS", "UAS"): ia mengumpulkan
 * beberapa {@link KategoriItemPenilaianSiswa} menjadi satu blok kolom, memiliki
 * {@link #getFormula() formula} sendiri untuk menghitung nilai total blok itu, dan boleh dibatasi
 * hanya berlaku untuk {@link #getKhususTingkat() tingkat} dan/atau
 * {@link #getKhususSemester() semester} tertentu.
 *
 * <p>Kelas ini juga <b>satu-satunya pemegang relasi tenant</b> ({@link #getSekolah() sekolah} dan
 * {@link #getYayasan() yayasan}) di sekitar simpul ini: baik {@link DetailGrupPenilaian} di atasnya
 * maupun {@link DetailGrupKategoriItemPenilaianSiswa} di bawahnya tidak punya kolom tenant sama
 * sekali dan mewarisi cakupannya dari sini.
 *
 * <h3>Posisi TERVERIFIKASI dalam rantai penilaian</h3>
 * Rantai berikut diverifikasi dari deklarasi kolom FK tiap entity penghubung (bukan dari kemiripan
 * nama kelas). Seluruh penghubung memakai pola "tabel silang + kolom {@code aktif}", bukan
 * {@code @ManyToMany}:
 * <ol>
 * <li>{@link JenisPenilaian} &mdash; payung teratas sekaligus <b>layar induk</b> seluruh master
 * penilaian sekolah.</li>
 * <li>{@link DetailJenisPenilaian} &mdash; {@code jenisPenilaian} &harr; {@code grupPenilaian}.</li>
 * <li>{@link GrupPenilaian} &mdash; pemilik {@code formula} tingkat grup, {@code jenisNilaiHuruf},
 * {@code adaTotal}.</li>
 * <li>{@link DetailGrupPenilaian} &mdash; kolom FK {@code grup_penilaian_id} dan
 * {@code grup_kategori_item_penilaian_siswa}; inilah baris yang menunjuk <b>kelas ini</b>.</li>
 * <li><b>{@code GrupKategoriItemPenilaianSiswa}</b> &mdash; <b>kelas ini</b>.</li>
 * <li>{@link DetailGrupKategoriItemPenilaianSiswa} &mdash; kolom FK
 * {@code grup_kategori_item_penilaian_siswa} (menunjuk kelas ini) dan
 * {@code kategori_item_penilaian_siswa}.</li>
 * <li>{@link KategoriItemPenilaianSiswa} &mdash; rumpun butir nilai; {@code getKode()}-nya menjadi
 * kunci urut primer kolom nilai di layar, rapor, rekap, dan API.</li>
 * <li>{@link JenisItemPenilaianSiswa} &mdash; butir nilai konkret (tipe isian,
 * {@code nilaiMin}/{@code nilaiMax}, {@code formula}, {@code wajibDiisi}, {@code nomorUrut}).</li>
 * </ol>
 *
 * <p>Rantai ini <b>terpisah total</b> dari {@link JenisNilaiSiswa} (skala nilai huruf); jangan
 * tertukar.
 *
 * <h3>Bagaimana kelas ini dibaca saat runtime</h3>
 * Pola baku seluruh pembaca (salin-tempel, terverifikasi identik di 10+ berkas) adalah dua langkah
 * proyeksi berturut-turut:
 * <pre>{@code
 * // 1) grup penilaian -> daftar grup kategori (kelas INI)
 * createCriteria(DetailGrupPenilaian.class)
 *     .add(Restrictions.eq("grupPenilaian", grupPenilaian))
 *     .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
 *     .setProjection(Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"));
 * // ... dibungkus ConstantValues.simpleList(..., GrupKategoriItemPenilaianSiswa.class, false)
 * Collections.sort(grupKategoriItemPenilaianSiswas);   // <-- memakai compareTo KELAS INI
 *
 * // 2) grup kategori -> daftar kategori -> butir nilai
 * createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
 *     .add(Restrictions.eq("grupKategoriItemPenilaianSiswa", grupKategori))
 *     .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
 *     .setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id"));
 * }</pre>
 * Pemanggil yang sudah diverifikasi: {@code ais.action.master.sekolah.helper.DetailPenilaianSiswaHelper}
 * dan {@code …DetailPenilaianLesSiswaHelper} (layar isi nilai),
 * {@code ais.action.master.sekolah.helper.PertemuanPunyaUjianSiswaHelper},
 * {@code ais.action.master.sekolah.helper.TampilStudiSiswaHelper} (portal siswa/ortu),
 * {@code ais.action.master.sekolah.PenilaianSiswaAction} (ekspor Excel),
 * {@code ais.action.master.helper.TugasMandiriHelper} dan {@code TugasKelompokHelper} (nilai tugas
 * e-learning), {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} dan
 * {@code LaporanRekapTotalNilai} (cetak rapor/rekap), {@code ais.common.GradingHelper} (hitung
 * ulang massal), serta REST {@code ais.action.servlet.api.NilaiSiswaApi} dan
 * {@code ais.action.servlet.api.ElearningApiUtil}. Entity ini <b>hidup penuh</b>, bukan sisa skema
 * lama.
 *
 * <p>Barisnya juga di-<i>preload</i> seluruhnya ke cache in-memory global
 * ({@code ais.common.InitData#doInitData()} memanggil {@code initClasses(…,}
 * {@code GrupKategoriItemPenilaianSiswa.class, …)}) dan terdaftar di
 * {@code ais.common.DataUtil#CLASS_JANGAN_DIBERSIHKAN} &mdash; tidak pernah dibersihkan perkakas
 * pembersih data. Karena {@code ConstantValues.simpleList(..., false)} menghidrasi object dari cache
 * itu, seluruh getter di bawah dipanggil atas object yang <b>dibagi lintas tenant di dalam satu
 * JVM</b>.
 *
 * <h3>Layar pengelola dan hak akses</h3>
 * Layar tunggal pengelolanya adalah {@code ais.action.master.sekolah.GrupKategoriItemPenilaianSiswaAction}
 * ({@code /pages/master/sekolah/grup_kategori_item_penilaian_siswa.zul}). Action itu memanggil
 * {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} dengan benar untuk tombol Tambah,
 * dua checkbox per baris, serta tombol Ubah/Hapus &mdash; contoh POSITIF gerbang per kontrol.
 *
 * <p><b>Namun layar ini tidak terdaftar sebagai menu mandiri.</b> Satu-satunya pintu masuknya
 * adalah {@code JenisPenilaianAction#onGrupKategori(Event)} yang menyisipkannya sebagai tab
 * ({@code MyInclude("/pages/master/sekolah/grup_kategori_item_penilaian_siswa.zul")}) di dalam layar
 * <i>Jenis Penilaian</i>. Karena {@code CommonPrivilages.checkPrevilages(...)} selalu mengacu ke
 * {@code Common.getCurrentMenu()}, hak yang sesungguhnya diuji adalah hak pada menu <b>Jenis
 * Penilaian</b>. Ini instance pola <i>pewarisan hak lewat menu induk</i> yang sama dengan yang
 * tercatat pada {@link KategoriItemPenilaianSiswa}, {@link DetailGrupKategoriItemPenilaianSiswa},
 * dan {@link PaketPsb}: siapa pun yang boleh mengubah Jenis Penilaian otomatis boleh memicu seluruh
 * siklus mati-hidup yang diuraikan di bawah.
 *
 * <p>Satu kontrol pada layar ini <b>tidak</b> bergerbang: tombol unduh Excel hasil
 * {@code Common.cetakData(this, contents)} dipasang lewat {@code Common.appendKeToolbar(...)} yang
 * (lihat catatan {@code AsramaSiswa}) tidak menyalin {@code isVisible()} dari tombol jangkarnya,
 * sehingga hak BACA saja sudah cukup untuk mengunduhnya. Isinya hanya metadata master
 * ({@code id, kode, nama, sekolah, formula, keterangan, khususTingkat, khususSemester,
 * nilaiBolehDinputOlehGuru, aktif}) &mdash; tidak ada PII siswa &mdash; sehingga dampaknya rendah.
 * Tombol unggah Excel di sebelahnya justru bergerbang benar
 * ({@code upload.setVisible(add.isVisible() && edit && delete)}).
 *
 * <h3>Bom waktu "grup kategori hantu": kelas INI adalah objek hantunya</h3>
 * Batch 51/54 mendokumentasikan siklus <i>"matikan semua lalu hidupkan yang tercentang"</i> pada
 * {@code GrupKategoriItemPenilaianSiswaAction#onSave(Event)} yang menyasar
 * {@link DetailGrupKategoriItemPenilaianSiswa}. Verifikasi dari sisi berkas ini menemukan bahwa
 * <b>mekanisme yang persis sama juga berjalan satu tingkat di atas</b>, di
 * {@code GrupPenilaianAction#onSave(Event)} yang menyasar {@link DetailGrupPenilaian} &mdash; dan di
 * sana <b>kelas inilah yang menjadi "hantu"</b>. Dua query pengisi dialog <i>Grup Penilaian</i>
 * memakai penyaring yang berbeda:
 * <ul>
 * <li><b>Daftar checkbox</b> ("Pilih Grup Kategori") dibangun dari <b>kelas ini</b> dengan penyaring
 * tenant {@code isNull("sekolah") OR eq("sekolah", s)} (idem yayasan) plus
 * {@code aktif}.</li>
 * <li><b>Peta pilihan tersimpan</b> dibangun dari {@link DetailGrupPenilaian} dengan
 * {@code eq("grupPenilaian", g)} + {@code aktif} + {@code grupKategoriItemPenilaianSiswa.aktif},
 * <b>tanpa penyaring tenant apa pun</b>.</li>
 * </ul>
 * Baris {@link DetailGrupPenilaian} yang menunjuk grup kategori milik sekolah lain karena itu masuk
 * ke peta tetapi tidak pernah punya checkbox; tidak ada checkbox berarti tidak ada cara
 * mengeluarkannya dari peta, sehingga langkah "hidupkan yang tercentang" selalu mengaktifkannya
 * kembali. Baris hantu itu tetap {@code aktif=true} selamanya, tetap menyumbang <b>seluruh blok
 * kolom nilainya</b> ke rapor, dan <b>tidak dapat dilepas dari layar mana pun</b>.
 *
 * <p>Kondisi pemicunya nyata karena {@link #getSekolah()} pada kelas ini <b>boleh {@code null}</b>
 * (lihat catatan fail-open di bawah) dan karena kepemilikan sekolah sebuah grup kategori dapat
 * berpindah lewat unggah Excel atau lewat penyuntingan biasa.
 *
 * <h3>Saklar {@code aktif}: asimetri yang membuatnya berbahaya</h3>
 * {@link #getAktif()} pada kelas ini <b>hanya dibaca di dua tempat</b>, keduanya di layar master:
 * checkbox "Aktif" pada grid {@code GrupKategoriItemPenilaianSiswaAction}, dan penyaring
 * {@code grupKategoriItemPenilaianSiswa.aktif} pada query pengisi peta di
 * {@code GrupPenilaianAction}. <b>Nol</b> pembaca runtime (formulir isi nilai, rapor, rekap, API,
 * mesin formula) memeriksanya &mdash; mereka menyaring {@code DetailGrupPenilaian.aktif} saja.
 * Akibatnya:
 * <ol>
 * <li><b>Melepas centang "Aktif" tidak menghasilkan apa-apa yang terlihat.</b> Blok kolom nilainya
 * tetap muncul di rapor seperti biasa; pengguna wajar menyimpulkan saklarnya "tidak berfungsi".</li>
 * <li><b>Tetapi ia sudah memasang bom.</b> Grup kategori non-aktif hilang dari peta <i>dan</i> dari
 * daftar checkbox dialog Grup Penilaian. Penyimpanan Grup Penilaian berikutnya &mdash; walau hanya
 * untuk mengubah nama atau formula &mdash; mematikan baris {@link DetailGrupPenilaian}-nya di
 * langkah "matikan semua" dan tidak pernah menghidupkannya lagi. Pada saat itu juga seluruh blok
 * kolom nilai lenyap dari rapor.</li>
 * <li><b>Mengaktifkan ulang lalu mencentang kembali menimbulkan duplikat.</b> Dialog tidak
 * menemukan baris lama di peta sehingga membuat {@code new DetailGrupPenilaian()} &mdash; pasangan
 * (grupPenilaian, grupKategori) yang sama kini punya dua baris, satu mati satu hidup. Tidak ada
 * kunci unik yang mencegahnya dan pembaca memakai {@code Projections.groupProperty(...)} sehingga
 * duplikat tak pernah terlihat; ia hanya menumpuk di tabel dan di tabel audit Envers.</li>
 * </ol>
 * Bentuk (2) dan (3) identik dengan temuan batch 54 satu tingkat di bawah &mdash; ini pola
 * arsitektur keluarga tabel penghubung penilaian, bukan kekhususan satu berkas.
 *
 * <h3>Verifikasi pola berulang lain</h3>
 * <ul>
 * <li><b>Penciutan {@code TreeSet}/{@code compareTo} &mdash; TIDAK ADA, tapi ada catatan.</b> Kelas
 * ini <b>tidak</b> meng-override {@code getNomorUrut()} (kontras {@link DetailGrupPenilaian} dan
 * {@link ParameterVerifikasiCalonSiswa} yang meng-override-nya sehingga tak pernah {@code null}),
 * dan tidak ada satu pun {@code TreeSet}/{@code TreeMap} berkunci tipe ini di seluruh repo.
 * Pengurutan nyata dilakukan dengan {@code Collections.sort(List)} yang stabil, sehingga elemen
 * tidak pernah hilang. Yang perlu diketahui: {@link #compareTo(GeneralValueObject)} di sini
 * <b>membalik prioritas kunci</b> kelas induk &mdash; lihat Javadoc method tersebut.</li>
 * <li><b>Getter write-back/destruktif &mdash; ADA, tiga buah.</b> {@link #getKode()} menulis balik
 * hasil normalisasi ke field (dan mengisi kode dari nama bila kosong), {@link #getYayasan()}
 * <b>menimpa</b> yayasan dengan yayasan milik sekolah, dan {@link #getFormula()},
 * {@link #getAktif()}, {@link #getKhususTingkat()}, {@link #getKhususSemester()},
 * {@link #getNilaiBolehDinputOlehGuru()} mengembalikan nilai <i>coalesced</i> yang &mdash; karena
 * Hibernate memakai akses properti &mdash; ikut tertulis ke kolom pada flush berikutnya. Rincian
 * dan dampaknya ada di Javadoc masing-masing method. Tidak ada yang menghapus data pengguna secara
 * permanen seperti {@code ItemBiayaSekolah.getKelamin()} atau {@code NominalBiaya.getNominal()};
 * yang terjadi di sini adalah normalisasi, bukan penghapusan.</li>
 * <li><b>{@code getKeterangan()} tidak dipetakan (pola batch 56) &mdash; TIDAK BERLAKU.</b>
 * {@link #getKeterangan()} di sini dianotasi {@code @Column(name = "keterangan")} dan benar-benar
 * dipetakan, diisi dari layar, serta ditampilkan di grid dan ekspor Excel.</li>
 * <li><b>Fail-open cakupan tenant &mdash; ADA (bentuk lunak).</b> Kolom {@code sekolah_id} dan
 * {@code yayasan_id} nullable, dan setiap query pemakai memakai bentuk
 * {@code s == null ? Restrictions.sqlRestriction("1=1") : (isNull("sekolah") OR eq("sekolah", s))}.
 * Pengguna tanpa sekolah efektif karena itu melihat SELURUH grup kategori instalasi, dan baris
 * ber-{@code sekolah} {@code null} sengaja tampil untuk semua sekolah (perilaku "template global"
 * yang memang diinginkan). {@code onSave} layar master mewajibkan Yayasan dan Sekolah terisi,
 * sehingga baris tanpa tenant praktis hanya lahir dari unggah Excel atau SQL mentah.</li>
 * <li><b>Pewarisan hak lewat menu induk &mdash; ADA</b>, lihat bagian hak akses di atas.</li>
 * </ul>
 *
 * <h3>Temuan keamanan di jalur REST (di luar berkas ini, tapi menyentuh entity ini langsung)</h3>
 * {@code ais.action.servlet.api.NilaiSiswaApi#input_nilai_siswa(HttpServletRequest, JSONObject)}
 * menerima {@code kelas_siswa_punya_siswa_id}, {@code jenis_item_penilaian_id},
 * {@code matapelajaran_id}, dan {@code grup_kategori_item_id} mentah dari badan permintaan, lalu
 * memuat keempatnya dengan {@code session.get(...)} <b>tanpa satu pun pemeriksaan kepemilikan</b>:
 * tidak diperiksa apakah guru pemegang token mengajar kelas/mata pelajaran tersebut, dan tidak
 * diperiksa apakah grup kategori yang dirujuk milik sekolah/yayasan guru itu. Satu-satunya gerbang
 * adalah "token valid dan pemiliknya seorang guru". Konsekuensinya, akun guru mana pun di instalasi
 * dapat <b>menulis dan mengubah nilai siswa mana pun di sekolah mana pun</b>, termasuk memicu
 * perhitungan ulang total dan nilai huruf. Ini bentuk IDOR yang sama dengan yang sudah dilacak
 * {@code task_493423ef}/{@code task_5e93a600}; tidak dibuat task baru.
 *
 * <h3>Hal non-obvious lain sebelum mengubah berkas ini</h3>
 * <ul>
 * <li><b>Komentar generator salah salin.</b> Baris <i>"Bank generated by hbm2java"</i> pada Javadoc
 * lama berasal dari {@code ais.database.model.Bank} dan tidak ada hubungannya dengan entity ini
 * &mdash; pola salin-tempel yang sudah dilacak sejak batch 51/54. Komentar itu diganti Javadoc ini.</li>
 * <li><b>Penamaan kolom apa adanya.</b> Proyek memakai
 * {@code ais.database.hibernate.MyNamingStrategy} (turunan {@code DefaultNamingStrategy}), sehingga
 * properti tanpa {@code @Column} dipetakan ke kolom <b>bernama persis seperti propertinya</b>:
 * {@code kode}, {@code aktif}, {@code khususTingkat}, {@code khususSemester},
 * {@code nilaiBolehDinputOlehGuru}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}
 * (camelCase, bukan snake_case).</li>
 * <li><b>Akses properti.</b> {@code @Id} berada pada getter, sehingga Hibernate membaca dan menulis
 * seluruh state lewat <b>getter</b>. Setiap normalisasi di dalam getter karena itu bukan sekadar
 * kenyamanan pemanggil &mdash; ia ikut menentukan isi kolom.</li>
 * <li><b>{@code @Audited} (Envers)</b> aktif, jadi setiap flush menghasilkan revisi. Siklus
 * mati-hidup yang dijelaskan di atas ikut menggemukkan tabel audit.</li>
 * <li><b>{@code dynamicInsert}/{@code dynamicUpdate}</b> aktif: hanya kolom yang berubah yang ikut
 * dalam pernyataan SQL.</li>
 * <li><b>Induk bukan entity.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash; bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga propertinya TIDAK dipetakan
 * Hibernate. Deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 * di kelas ini karena itu <b>keharusan teknis, bukan duplikasi yang perlu "dibersihkan"</b>.
 * Konsekuensi lain: {@code nomorUrut} dan {@code nim} yang diwarisi tidak pernah dipetakan dan
 * selalu {@code null} pada object hasil muat dari basis data &mdash; fakta yang dipakai
 * {@link DetailGrupPenilaian#getNomorUrut()}.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see DetailGrupPenilaian
 * @see DetailGrupKategoriItemPenilaianSiswa
 * @see KategoriItemPenilaianSiswa
 * @see JenisItemPenilaianSiswa
 * @see GrupPenilaian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "grup_kategori_item_penilaian_siswa")
public class GrupKategoriItemPenilaianSiswa extends GeneralValueObject {

	/**
	 * Membandingkan dua entity untuk keperluan pengurutan, dengan <b>prioritas kunci yang dibalik</b>
	 * dari {@link GeneralValueObject#compareTo(GeneralValueObject)}.
	 *
	 * <p>Urutan kunci di sini adalah {@code nama} &rarr; {@code keterangan} &rarr; {@code nomorUrut}
	 * &rarr; {@code nim}, sedangkan kelas induk memakai {@code nomorUrut} &rarr; {@code nim} &rarr;
	 * {@code nama} &rarr; {@code keterangan}. Sebuah kunci hanya dipakai bila <b>kedua</b> object
	 * memilikinya (non-null); bila tidak ada yang memenuhi syarat &mdash; atau terjadi exception,
	 * yang ditelan dan dicatat ke audit &mdash; method mengembalikan {@code 0}.
	 *
	 * <p><b>Dalam praktiknya method ini selalu berhenti di cabang pertama.</b> Kolom {@code nama}
	 * dideklarasikan {@code nullable = false} dan divalidasi wajib oleh layar master, sehingga
	 * perbandingan efektifnya adalah {@code getNama().compareTo(...)} &mdash; perbandingan
	 * {@code String} yang <b>peka besar-kecil huruf</b> (seluruh nama berhuruf kapital mendahului
	 * yang berhuruf kecil). Cabang {@code keterangan}, {@code nomorUrut}, dan {@code nim} praktis
	 * kode mati: {@code nomorUrut} dan {@code nim} diwarisi dari {@link GeneralValueObject} yang
	 * bukan kelas terpetakan sehingga selalu {@code null} pada object hasil muat dari basis data.
	 *
	 * <p><b>Mengapa ini penting.</b> Method ini bukan sekadar pelengkap: ia adalah <b>kunci urut
	 * nyata seluruh blok kolom nilai</b>. Sekitar sepuluh pemanggil menjalankan
	 * {@code Collections.sort(grupKategoriItemPenilaianSiswas)} atas hasil proyeksi
	 * {@link DetailGrupPenilaian} &mdash; antara lain {@code DetailPenilaianSiswaHelper},
	 * {@code DetailPenilaianLesSiswaHelper}, {@code PertemuanPunyaUjianSiswaHelper},
	 * {@code TugasMandiriHelper}, {@code PenilaianSiswaAction}, {@code LaporanRekapTotalNilai},
	 * {@code ElearningApiUtil}, dan {@code NilaiSiswaApi}. Urutan tab/kolom nilai di layar, rapor,
	 * dan API karena itu ditentukan oleh <b>nama</b>.
	 *
	 * <p><b>Kuirk yang mudah mengejutkan:</b> kedua layar master justru mengurutkan daftarnya
	 * dengan {@code addOrder(Order.asc("kode"))} di sisi basis data. Jadi urutan grup kategori yang
	 * dilihat administrator saat mencentang (berdasarkan <i>kode</i>) bisa berbeda dari urutan
	 * kolom yang akhirnya muncul di rapor (berdasarkan <i>nama</i>). Mengubah salah satu sisi tanpa
	 * yang lain akan memperlebar perbedaan itu.
	 *
	 * <p>Nilai {@code 0} di sini tidak berarti {@code equals}; {@code compareTo} tidak konsisten
	 * dengan {@link #equals(Object)}, jadi jangan pernah memakai tipe ini sebagai kunci
	 * {@code TreeSet}/{@code TreeMap} &mdash; dua grup kategori bernama sama (misalnya milik dua
	 * sekolah berbeda) akan saling menciutkan. Saat ini tidak ada koleksi terurut seperti itu di
	 * repo; seluruh pengurutan memakai {@code Collections.sort(List)} yang stabil dan aman.
	 *
	 * @param arg0 entity pembanding; boleh entity jenis lain karena tanda tangan diwarisi dari
	 *             {@link GeneralValueObject}
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila tidak ada kunci
	 *         pembanding yang tersedia atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/GrupKategoriItemPenilaianSiswa.java:51");

		}

		return 0;
	}

	/**
	 * Penanda versi serialisasi Java. Object entity ini ikut berpindah lewat sesi/desktop ZK dan
	 * cache in-memory, jadi nilainya <b>jangan diubah</b> tanpa alasan; perubahan akan membuat
	 * state ZK lama tidak dapat dideserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer {@code bigserial}; lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir (jejak audit warisan); lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna pengubah terakhir (jejak audit warisan); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat callback
	 * {@link #onUpdate()}; tidak pernah disetel dari layar.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila baris belum pernah ter-UPDATE
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Nilai {@code null} atau kosong/spasi diabaikan diam-diam</b> (method langsung
	 * {@code return} tanpa menulis apa pun), sehingga jejak audit yang sudah ada tidak dapat
	 * dihapus dengan menyetel nilai kosong. Ini pola baku seluruh entity turunan
	 * {@link GeneralValueObject}, bukan kekhususan berkas ini.
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
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila baris belum pernah ter-UPDATE
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence TEPAT SEBELUM pernyataan
	 * UPDATE dikirim, dan meneruskan object ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif serta memperbarui
	 * {@code tanggal_dirubah}.
	 *
	 * <p><b>Hanya UPDATE.</b> Tidak ada {@code @PrePersist}, sehingga baris yang baru dibuat
	 * mengandalkan nilai awal field {@code tanggal_dirubah} (disetel
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat object Java dibuat &mdash; waktu
	 * <i>instansiasi</i>, bukan waktu simpan) dan tidak pernah mendapat {@code oleh}/{@code olehId}
	 * dari jalur ini.
	 *
	 * <p><b>Relevansi khusus untuk entity ini:</b> karena beberapa getter di kelas ini
	 * menormalisasi nilai dan menulisnya balik ({@link #getKode()}, {@link #getYayasan()},
	 * {@link #getFormula()}, dan getter {@code null}&rarr;default lainnya), baris yang <i>hanya
	 * dibaca</i> oleh sebuah laporan pun bisa terdeteksi kotor dan ter-UPDATE. Jejak audit di sini
	 * karena itu tidak dapat dipakai untuk menyimpulkan "kapan grup kategori ini terakhir kali
	 * benar-benar diubah pengguna".
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus &mdash; method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} &mdash; hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya. Field
	 * {@code tanggal_dirubah} adalah stempel waktu perubahan terakhir, dipetakan ke kolom bertipe
	 * TIMESTAMP lewat {@link #getTanggal_dirubah()}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Normalnya hanya dipanggil {@code AuditTimestampInterceptor}; menyetelnya manual akan
	 * ditimpa pada UPDATE berikutnya lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * TIMESTAMP).
	 *
	 * <p>Tidak pernah {@code null} untuk object yang dibuat lewat konstruktor Java, karena field-nya
	 * diinisialisasi {@code WaktuUtil.getDate()} pada saat instansiasi.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log dan debugging, berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca <b>field</b> {@code nama} secara langsung, bukan lewat {@link #getNama()},
	 * sehingga nilainya <b>tidak di-trim</b> &mdash; berbeda satu spasi dari yang dilihat sisa
	 * aplikasi. Untuk object baru yang belum tersimpan hasilnya berbentuk {@code "null-..."}.
	 *
	 * @return gabungan id dan nama, dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas grup kategori; lihat {@link #getKode()} untuk normalisasi yang dilakukannya. */
	private String kode;

	/** Nama grup kategori (wajib); lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Saklar aktif; lihat {@link #getAktif()} dan bagian "Saklar {@code aktif}" pada Javadoc kelas. */
	private Boolean aktif;
	/** Sekolah pemilik (nullable = "berlaku untuk semua"); lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik, praktis turunan dari sekolah; lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Formula total blok, disimpan sebagai teks JSON; lihat {@link #getFormula()}. */
	private String formula;

	/** Pembatas tingkat/kelas; lihat {@link #getKhususTingkat()}. */
	private Integer khususTingkat;
	/** Pembatas semester; lihat {@link #getKhususSemester()}. */
	private Integer khususSemester;
	/** Izin input nilai oleh guru; lihat {@link #getNilaiBolehDinputOlehGuru()}. */
	private Boolean nilaiBolehDinputOlehGuru;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Dipakai juga oleh {@code GrupKategoriItemPenilaianSiswaAction#onAdd(Event)} untuk membuat
	 * baris kosong sebelum dialog Tambah dibuka. Seluruh field skalar dibiarkan {@code null} kecuali
	 * {@code tanggal_dirubah} yang langsung terisi waktu instansiasi (lihat {@link #onUpdate()}).
	 */
	public GrupKategoriItemPenilaianSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini (kolom {@code id}, {@code bigserial}).
	 *
	 * <p>Dianotasi {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code GenerationType.IDENTITY}). Bernilai {@code null} untuk object yang belum tersimpan
	 * &mdash; kondisi yang dipakai layar master untuk membedakan judul dialog "Tambah" dan "Ubah".
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
	 * Menyetel kunci primer. Tanpa validasi.
	 *
	 * <p>Hanya dipanggil Hibernate; jangan disetel manual pada object yang sudah dikelola sesi.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas grup kategori &mdash; <b>getter dengan efek samping</b>, bukan
	 * pembaca murni.
	 *
	 * <p>Dua hal terjadi di sini:
	 * <ol>
	 * <li><b>Isi otomatis dari nama.</b> Bila field {@code kode} masih {@code null}, method
	 * <b>menulis</b> {@code kode = getNama()} ke field. Jadi grup kategori yang dibuat tanpa mengisi
	 * kode akan diam-diam memakai namanya sendiri sebagai kode.</li>
	 * <li><b>Normalisasi tampilan.</b> Nilai yang dikembalikan selalu dibersihkan: seluruh tanda
	 * baca kecuali {@code _} dan {@code -} dibuang
	 * ({@code replaceAll("[\\p{Punct}&&[^_-]]+", "")}), hasilnya di-{@code trim()}, lalu setiap
	 * spasi diganti {@code _}. Nilai {@code null} dikembalikan sebagai {@code ""}, tidak pernah
	 * {@code null}.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi persistensi.</b> Kelas ini memakai akses properti, sehingga <b>nilai
	 * hasil normalisasi inilah yang dibaca Hibernate saat dirty-checking dan yang tertulis ke kolom
	 * {@code kode}</b> pada flush berikutnya &mdash; termasuk pada baris yang hanya dibaca oleh
	 * laporan. Kode {@code "UTS (Ganjil)"} yang pernah masuk lewat unggah Excel akan berubah
	 * permanen menjadi {@code "UTS_Ganjil"} begitu barisnya tersentuh. Normalisasinya
	 * <i>idempoten</i> (menjalankannya berulang menghasilkan nilai sama), jadi tidak ada erosi
	 * bertingkat, tetapi bentuk aslinya tidak dapat dipulihkan.
	 *
	 * <p>Kode ini dipakai sebagai label checkbox di dialog <i>Grup Penilaian</i>
	 * ({@code kode + " - " + nama}), sebagai kolom pertama grid master, sebagai kunci
	 * {@code addOrder(Order.asc("kode"))} pada kedua layar master, dan sebagai target pencarian
	 * {@code ilike} pada filter. Karena pengurutan basis data memakai <b>kolom</b> sedangkan
	 * pengurutan runtime memakai {@link #compareTo(GeneralValueObject)} yang berbasis
	 * <b>nama</b>, keduanya bisa menghasilkan urutan berbeda.
	 *
	 * @return kode yang sudah dinormalisasi; {@code ""} bila kode maupun nama kosong &mdash; tidak
	 *         pernah {@code null}
	 */
	public String getKode() {
		if (kode == null) {
			kode = getNama();
		}
		return kode == null ? "" : org.apache.commons.lang3.StringUtils.replace(kode.replaceAll("[\\p{Punct}&&[^_-]]+", "").trim(), " ", "_");
	}

	/**
	 * Menyetel kode ringkas grup kategori. Tanpa validasi &mdash; normalisasi baru terjadi saat
	 * dibaca lewat {@link #getKode()}.
	 *
	 * <p>Dipanggil {@code GrupKategoriItemPenilaianSiswaAction#onSave(Event)} dengan isi textbox
	 * "Kode Grup Kategori Penilaian". Textbox itu diinisialisasi dari {@link #getKode()} yang sudah
	 * ternormalisasi, sehingga perjalanan bolak-balik lewat layar bersifat stabil.
	 *
	 * @param kode kode baru; boleh {@code null} (akan diisi ulang dari nama saat dibaca)
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama grup kategori (kolom {@code nama}, {@code NOT NULL}, maksimal 255
	 * karakter), sudah di-{@code trim()}.
	 *
	 * <p>Inilah label yang dilihat pengguna sebagai judul tab/blok kolom nilai, dan sekaligus
	 * <b>kunci urut nyata</b> seluruh blok kolom nilai lewat
	 * {@link #compareTo(GeneralValueObject)}.
	 *
	 * <p>Perhatikan bahwa yang di-trim hanya nilai <b>yang dikembalikan</b>, bukan field-nya; karena
	 * Hibernate memakai akses properti, versi ter-trim itulah yang akhirnya tertulis ke kolom.
	 * {@link #toString()} membaca field mentah sehingga bisa menampilkan spasi yang tidak terlihat
	 * di tempat lain.
	 *
	 * @return nama grup kategori tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama grup kategori. Tanpa validasi di kelas ini.
	 *
	 * <p>Kewajiban isi ditegakkan di layar: {@code onSave(Event)} menolak simpan dengan pesan
	 * "Nama Grup Kategori Penilaian harus diisi" bila textbox-nya kosong. Jalur unggah Excel
	 * <b>tidak</b> melewati validasi itu.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas grup kategori (kolom {@code keterangan}, nullable, teks
	 * panjang).
	 *
	 * <p><b>Menimpa</b> {@link GeneralValueObject#getKeterangan()} yang menormalisasi {@code null}
	 * menjadi {@code ""}; versi ini mengembalikan nilai apa adanya, jadi pemanggil <b>harus</b>
	 * memeriksa {@code null} sendiri. Perbedaan itu ikut mengubah perilaku cabang {@code keterangan}
	 * pada {@link #compareTo(GeneralValueObject)}, yang di kelas ini bisa dilewati bila keterangan
	 * kosong (di kelas induk cabang serupa selalu memenuhi syarat).
	 *
	 * <p>Berbeda dengan beberapa entity master lain (pola batch 56), properti ini <b>benar-benar
	 * dipetakan</b>: diisi dari textarea "Keterangan" pada dialog, ditampilkan sebagai kolom grid,
	 * dan ikut dalam ekspor Excel.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan saklar aktif grup kategori, dengan <b>{@code null} dianggap {@code true}</b>.
	 *
	 * <p>Coalescing itu punya dua akibat. Pertama, baris lama yang kolom {@code aktif}-nya masih
	 * {@code NULL} tetap terbaca aktif oleh kode Java. Kedua &mdash; karena Hibernate memakai akses
	 * properti &mdash; nilai {@code true} hasil coalescing <b>ikut tertulis</b> ke kolom pada
	 * INSERT/UPDATE berikutnya, sehingga {@code NULL} perlahan tergantikan {@code true} untuk baris
	 * yang tersentuh. Risiko pola "kolom {@code aktif} tak pernah ditulis" karena itu hanya nyata
	 * untuk baris yang masuk lewat SQL mentah/migrasi dan tidak pernah lagi disentuh Java.
	 *
	 * <p><b>Cakupan pembacanya sangat sempit &mdash; dan di situlah bahayanya.</b> Hanya dua tempat
	 * yang membacanya, keduanya di layar master: checkbox "Aktif" pada grid
	 * {@code GrupKategoriItemPenilaianSiswaAction}, dan penyaring
	 * {@code grupKategoriItemPenilaianSiswa.aktif} pada query pengisi peta pilihan di
	 * {@code GrupPenilaianAction}. <b>Nol</b> pembaca runtime (formulir isi nilai, rapor, rekap,
	 * API, mesin formula) memeriksanya. Baca bagian "Saklar {@code aktif}" pada Javadoc kelas
	 * sebelum menyentuh apa pun: melepas centang tidak menyembunyikan apa-apa dari rapor, tetapi
	 * memastikan blok kolom nilai ini akan lenyap permanen pada penyimpanan Grup Penilaian
	 * berikutnya.
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} hanya bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel saklar aktif. Tanpa validasi.
	 *
	 * <p>Satu-satunya pemanggil adalah listener {@code onCheck} checkbox "Aktif" pada grid layar
	 * master, yang langsung menyusulinya dengan {@code Common.refreshSaveOrUpdate(...)} &mdash;
	 * jadi <b>satu klik langsung tersimpan</b>, tanpa dialog konfirmasi dan tanpa tombol Simpan.
	 * Checkbox itu dinonaktifkan bila pengguna tidak punya hak UPDATE.
	 *
	 * @param aktif status aktif baru; {@code null} akan terbaca sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sekolah pemilik grup kategori (kolom {@code sekolah_id}, nullable), setelah
	 * proxy lazy diselesaikan lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>{@code null} berarti "berlaku untuk semua sekolah"</b>, bukan "data rusak". Seluruh
	 * query pemakai memakai bentuk {@code isNull("sekolah") OR eq("sekolah", s)} sehingga baris
	 * tanpa sekolah sengaja tampil di setiap tenant &mdash; perilaku "template global" yang memang
	 * diinginkan.
	 *
	 * <p><b>Sisi fail-open-nya:</b> bila pengguna yang sedang bekerja tidak punya sekolah efektif,
	 * query yang sama jatuh ke {@code Restrictions.sqlRestriction("1=1")} dan menampilkan
	 * <b>seluruh</b> grup kategori instalasi. {@code onSave} layar master mewajibkan Sekolah dan
	 * Yayasan terisi, sehingga baris tanpa tenant praktis hanya lahir dari unggah Excel atau SQL
	 * mentah.
	 *
	 * <p>Nilai ini juga yang menentukan daftar {@link KategoriItemPenilaianSiswa} mana yang muncul
	 * sebagai checkbox di dialog grup kategori &mdash; dan karenanya menjadi salah satu pemicu
	 * "kategori hantu" yang diuraikan pada {@link DetailGrupKategoriItemPenilaianSiswa}.
	 *
	 * @return sekolah pemilik, atau {@code null} bila grup kategori berlaku lintas sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan <b>normalisasi</b>: object yang {@code null} <i>atau</i>
	 * belum punya id disimpan sebagai {@code null}.
	 *
	 * <p>Penjagaan itu mencegah Hibernate mencoba meng-cascade PERSIST atas {@code Sekolah}
	 * sementara (misalnya item combo "Semua Sekolah") dan menciptakan baris sekolah baru yang tidak
	 * dikehendaki.
	 *
	 * @param sekolah sekolah pemilik baru; {@code null} atau object tanpa id menghasilkan
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik (kolom {@code yayasan_id}, nullable) &mdash; <b>getter dengan
	 * efek samping yang menimpa nilai tersimpan</b>.
	 *
	 * <p>Alurnya: bila {@link #getSekolah()} tidak {@code null}, field {@code yayasan}
	 * <b>ditimpa</b> dengan {@code sekolah.getYayasan()}; barulah hasilnya di-{@code check(...)}
	 * dan dikembalikan. Artinya:
	 * <ul>
	 * <li>{@link #setYayasan(Yayasan)} secara efektif <b>tidak berpengaruh</b> selama sekolah
	 * terisi &mdash; yayasan selalu mengikuti yayasan milik sekolah. Combo "Yayasan" pada dialog
	 * hanya berperan sebagai penyaring daftar sekolah dan penyaring daftar kategori, bukan sebagai
	 * nilai yang benar-benar tersimpan.</li>
	 * <li>Karena Hibernate memakai akses properti, penimpaan itu <b>ikut tertulis ke kolom</b> pada
	 * flush berikutnya. Memindahkan sebuah {@code Sekolah} ke yayasan lain karena itu diam-diam
	 * memindahkan pula seluruh grup kategorinya, tanpa jejak tindakan pengguna.</li>
	 * <li>Bila sekolah {@code null}, nilai yang pernah disetel manual dipertahankan &mdash; inilah
	 * satu-satunya cara membuat grup kategori "milik satu yayasan, semua sekolah".</li>
	 * </ul>
	 *
	 * @return yayasan pemilik &mdash; yayasan milik sekolah bila sekolah terisi, atau nilai yang
	 *         disetel manual bila tidak; boleh {@code null}
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
	 * {@link #setSekolah(Sekolah)}: object {@code null} atau tanpa id disimpan sebagai {@code null}.
	 *
	 * <p><b>Perhatikan:</b> nilai yang disetel di sini akan ditimpa oleh {@link #getYayasan()}
	 * setiap kali sekolah terisi. Menyetel yayasan tanpa menyetel sekolah adalah satu-satunya cara
	 * agar nilai ini bertahan.
	 *
	 * @param yayasan yayasan pemilik baru; {@code null} atau object tanpa id menghasilkan
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Nilai bawaan {@link #getFormula()} bila formula belum pernah diisi: representasi teks dari
	 * {@code JSONArray} kosong, yaitu {@code "[]"}.
	 *
	 * <p><b>Kuirk:</b> konstanta ini {@code public static} <b>tanpa {@code final}</b>, sehingga
	 * kode mana pun dalam JVM dapat menggantinya dan mengubah nilai bawaan seluruh grup kategori
	 * sekaligus. Tidak ada yang melakukannya saat ini, tetapi bentuk deklarasinya patut diketahui
	 * sebelum ada yang tergoda memanfaatkannya.
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan formula perhitungan nilai total blok kolom ini, disimpan sebagai teks JSON pada
	 * kolom {@code formula} bertipe {@code text}.
	 *
	 * <p><b>Bentuk isinya</b> adalah array object berkunci tanggal berlaku, misalnya
	 * {@code [{"tgl":"01-07-2025","target":"(UH1+UH2)/2","target_min":"60"}, ...]}. Mesin formula
	 * {@code ais.action.master.sekolah.util.GrupPenilaianUtil#ambilTarget(String, Date)} dan
	 * {@code #ambilTargetMin(String, Date)} memilih entri dengan tanggal berlaku terbaru yang tidak
	 * melewati tanggal acuan &mdash; jadi satu grup kategori dapat menyimpan beberapa versi rumus
	 * sekaligus dan berganti otomatis sesuai kalender. Ekspresinya kemudian dievaluasi dengan
	 * mensubstitusi {@code getKode()} tiap {@link JenisItemPenilaianSiswa} di bawahnya.
	 *
	 * <p><b>Normalisasi:</b> {@code null} maupun string kosong dikembalikan sebagai
	 * {@link #DEFAULT_FORMULA} ({@code "[]"}) agar pemanggil selalu aman memanggil
	 * {@code new JSONArray(formula)}. Seperti getter lain di kelas ini, nilai coalesced itu ikut
	 * tertulis ke kolom pada flush berikutnya, sehingga kolom {@code NULL} lama perlahan berubah
	 * menjadi {@code "[]"}.
	 *
	 * <p>Isi kolom ini <b>tidak divalidasi</b> di mana pun: {@code onSave(Event)} menulis
	 * {@code array.toString()} apa adanya, dan kesalahan sintaks baru terasa saat rapor dihitung
	 * &mdash; di sana exception-nya ditelan sehingga totalnya diam-diam menjadi kosong.
	 *
	 * @return teks JSON formula; {@code "[]"} bila belum diisi &mdash; tidak pernah {@code null}
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Menyetel teks JSON formula. Tanpa validasi sintaks.
	 *
	 * <p>Dipanggil {@code onSave(Event)} dengan {@code array.toString()} dari editor formula yang
	 * dibangun {@code GrupPenilaianAction#reloadFormula(...)}.
	 *
	 * @param formula teks JSON formula baru; {@code null}/kosong akan terbaca sebagai {@code "[]"}
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan pembatas tingkat (kelas 1&ndash;12) grup kategori ini, dengan
	 * <b>normalisasi</b>: nilai {@code <= 0} dikembalikan sebagai {@code null}.
	 *
	 * <p>Semantiknya: {@code null} berarti "berlaku untuk semua tingkat"; angka berarti blok kolom
	 * nilai ini <b>hanya</b> dirender untuk kelas dengan tingkat tersebut. Penyaringnya dipakai
	 * pembaca runtime dengan pola {@code kelasSiswa.getTingkat() > 0 && getKhususTingkat() != null
	 * && !getKhususTingkat().equals(kelasSiswa.getTingkat())} &rarr; {@code continue} &mdash;
	 * terverifikasi di {@code DetailPenilaianSiswaHelper}, {@code DetailPenilaianLesSiswaHelper},
	 * {@code TugasMandiriHelper}, dan {@code NilaiSiswaApi}. {@link GrupPenilaian} punya pembatas
	 * sejenis yang diperiksa terpisah di tingkat di atasnya.
	 *
	 * <p>Nilai {@code 0} tidak akan pernah tersimpan lewat layar (combo "Khusus buat tingkat" hanya
	 * menawarkan "Semua Tingkat" &rarr; {@code null} dan angka 1&ndash;12), tetapi normalisasi ini
	 * melindungi baris hasil unggah Excel. Seperti getter lain, nilai {@code null} hasil
	 * normalisasi ikut tertulis ke kolom pada flush berikutnya.
	 *
	 * @return tingkat yang dibatasi, atau {@code null} bila berlaku untuk semua tingkat
	 */
	public Integer getKhususTingkat() {
		return khususTingkat != null && khususTingkat <= 0 ? null : khususTingkat;
	}

	/**
	 * Menyetel pembatas tingkat. Tanpa validasi rentang &mdash; nilai {@code <= 0} baru
	 * dinetralkan saat dibaca lewat {@link #getKhususTingkat()}.
	 *
	 * @param khususTingkat tingkat yang dibatasi; {@code null} atau {@code <= 0} berarti semua
	 *                      tingkat
	 */
	public void setKhususTingkat(Integer khususTingkat) {
		this.khususTingkat = khususTingkat;
	}

	/**
	 * Mengembalikan pembatas semester (1 atau 2) grup kategori ini, dengan normalisasi yang sama
	 * seperti {@link #getKhususTingkat()}: nilai {@code <= 0} menjadi {@code null}.
	 *
	 * <p>Semantiknya berbeda dari pembatas tingkat: alih-alih melewati blok kolom, nilai ini
	 * <b>mempersempit daftar semester</b> yang dirender ({@code smts = new int[]{ getKhususSemester() }})
	 * pada {@code DetailPenilaianSiswaHelper} dan {@code DetailPenilaianLesSiswaHelper}. Nilai dari
	 * {@link GrupPenilaian} di tingkat atas diperiksa <b>setelahnya</b> pada beberapa cabang,
	 * sehingga pembatas grup dapat menimpa pembatas grup kategori &mdash; perhatikan urutan itu bila
	 * dua tingkat sama-sama diisi.
	 *
	 * @return semester yang dibatasi, atau {@code null} bila berlaku untuk semua semester
	 */
	public Integer getKhususSemester() {
		return khususSemester != null && khususSemester <= 0 ? null : khususSemester;
	}

	/**
	 * Menyetel pembatas semester. Tanpa validasi rentang.
	 *
	 * @param khususSemester semester yang dibatasi (1 atau 2); {@code null} atau {@code <= 0}
	 *                       berarti semua semester
	 */
	public void setKhususSemester(Integer khususSemester) {
		this.khususSemester = khususSemester;
	}

	/**
	 * Mengembalikan izin pengisian nilai oleh guru untuk blok kolom ini, dengan <b>{@code null}
	 * dianggap {@code true}</b> (bawaan permisif).
	 *
	 * <p>Bila {@code false}, kolom nilai blok ini dirender hanya-baca bagi pengguna berperan guru;
	 * pengisiannya menjadi wewenang administrator/wali kelas saja. Pemeriksaannya dilakukan
	 * berpasangan dengan flag sejenis milik {@link GrupPenilaian} &mdash; keduanya harus mengizinkan
	 * agar isian terbuka.
	 *
	 * <p><b>Catatan penting:</b> ini kontrol <i>tampilan</i>, bukan gerbang otorisasi. Jalur REST
	 * {@code NilaiSiswaApi#input_nilai_siswa(...)} menulis nilai <b>tanpa memeriksa flag ini sama
	 * sekali</b> (dan tanpa memeriksa kepemilikan kelas/mata pelajaran &mdash; lihat Javadoc kelas),
	 * sehingga menonaktifkannya tidak menghalangi penulisan lewat API.
	 *
	 * <p>Seperti {@link #getAktif()}, nilai {@code true} hasil coalescing ikut tertulis ke kolom
	 * pada flush berikutnya.
	 *
	 * @return {@code true} bila guru boleh mengisi nilai atau flag belum pernah disetel;
	 *         {@code false} hanya bila eksplisit dilarang
	 */
	public Boolean getNilaiBolehDinputOlehGuru() {
		return nilaiBolehDinputOlehGuru == null ? true : nilaiBolehDinputOlehGuru;
	}

	/**
	 * Menyetel izin pengisian nilai oleh guru. Tanpa validasi.
	 *
	 * <p>Satu-satunya pemanggil adalah listener {@code onCheck} checkbox "Boleh Diinput Guru" pada
	 * grid layar master, yang langsung menyimpannya lewat
	 * {@code Common.refreshSaveOrUpdate(...)} &mdash; satu klik langsung tersimpan. Checkbox itu
	 * dinonaktifkan bila pengguna tidak punya hak UPDATE.
	 *
	 * @param nilaiBolehDinputOlehGuru izin baru; {@code null} akan terbaca sebagai {@code true}
	 */
	public void setNilaiBolehDinputOlehGuru(Boolean nilaiBolehDinputOlehGuru) {
		this.nilaiBolehDinputOlehGuru = nilaiBolehDinputOlehGuru;
	}
}
