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
import org.json.JSONArray;

import ais.database.model.GeneralValueObject;

/**
 * <b>Simpul tengah rantai konfigurasi penilaian siswa modul Sekolah.</b> Satu baris mewakili satu
 * &quot;Grup Penilaian&quot; &mdash; misalnya <i>Pengetahuan</i>, <i>Keterampilan</i>, atau
 * <i>Sikap</i> &mdash; yaitu satu tab/blok nilai pada layar penilaian dan satu blok kolom pada
 * rapor. Grup inilah yang memiliki <b>formula</b> perhitungan nilai akhir, skala huruf yang
 * dipakai, dan pembatasan tingkat/semester tempat grup itu berlaku. Dipetakan ke tabel
 * {@code sekolah.grup_penilaian}.
 *
 * <h2>Kedudukan pada rantai penilaian</h2>
 * <p>Rantai lengkap (delapan entity), dari puncak ke daun; kelas ini berada di tengah:</p>
 * <pre>
 *   {@link JenisPenilaian}                          (puncak: dipilih per kurikulum/mata pelajaran)
 *        &darr;  {@link DetailJenisPenilaian}        (kolom: jenis_penilaian_id, grup_penilaian_id)
 *   <b>GrupPenilaian &mdash; KELAS INI</b>                 (pemilik formula, jenisNilaiHuruf, khususTingkat/Semester,
 *                                             nilaiBolehDinputOlehGuru, tampilDirekap, adaTotal)
 *        &darr;  {@link DetailGrupPenilaian}         (kolom: grup_penilaian_id, grup_kategori_item_penilaian_siswa)
 *   {@link GrupKategoriItemPenilaianSiswa}
 *        &darr;  {@link DetailGrupKategoriItemPenilaianSiswa}
 *   {@link KategoriItemPenilaianSiswa}
 *        &darr;
 *   {@link JenisItemPenilaianSiswa}                 (daun: kode yang dipakai di formula, mis. uh1, uts, uas)
 * </pre>
 * <p>Dengan kata lain: sebuah {@code JenisPenilaian} memakai beberapa {@code GrupPenilaian}, dan
 * setiap {@code GrupPenilaian} memakai beberapa {@link GrupKategoriItemPenilaianSiswa} yang pada
 * akhirnya menurunkan daftar kode {@link JenisItemPenilaianSiswa} yang boleh muncul di dalam
 * formula grup ini.</p>
 *
 * <h2>Tidak ada koleksi &mdash; seluruh navigasi lewat Criteria</h2>
 * <p>Kelas ini <b>tidak memiliki satu pun</b> {@code @OneToMany}. Kedua sisi rantai memegang FK di
 * seberang: {@code DetailJenisPenilaian.grup_penilaian_id} di atas dan
 * {@code DetailGrupPenilaian.grup_penilaian_id} di bawah. Akibatnya seluruh penelusuran rantai
 * dilakukan lewat {@code Criteria} eksplisit di sisi pemanggil, dan <b>tidak ada cascade
 * penghapusan</b>: menonaktifkan atau menghapus baris di sini tidak menyentuh baris penghubung
 * mana pun. Konsekuensi praktisnya, memuat &quot;isi&quot; sebuah grup penilaian selalu berupa
 * query terpisah &mdash; lihat daftar pembaca di bawah.</p>
 *
 * <h2>Formula: riwayat ber-tanggal dalam satu kolom teks</h2>
 * <p>Kolom {@code formula} bukan satu ekspresi, melainkan {@link JSONArray} berisi
 * <b>beberapa versi ber-tanggal</b>. Setiap elemen memuat {@code tgl} (tanggal efektif),
 * {@code target} (ekspresi nilai), {@code target_min} dan {@code target_max} (batas validasi
 * nilai). Mesin evaluasinya adalah {@code ais.action.master.sekolah.util.GrupPenilaianUtil}, yang
 * memilih versi bertanggal efektif terbaru yang tidak melewati tanggal evaluasi &mdash; sehingga
 * mengubah formula hari ini tidak mengubah perhitungan nilai historis. Ekspresi dievaluasi memakai
 * pustaka {@code exp4j}, dengan substitusi placeholder {@code kkm}/{@code KKM}, kode
 * {@code ais.database.model.Konstanta}, dan kode {@link JenisItemPenilaianSiswa}.</p>
 * <p><b>Bentuk penyimpanan ini sengaja bebas-skema</b>: kolomnya {@code columnDefinition = "text"}
 * dan tidak pernah divalidasi saat disimpan. Formula yang salah ketik tidak ditolak
 * {@code onSave()}; ia baru gagal saat evaluasi, dan kegagalan itu dikembalikan sebagai
 * {@code 0.0} secara diam-diam (exception ditelan di {@code GrupPenilaianUtil.hitung}). Nilai
 * siswa yang tiba-tiba nol massal karena satu formula rusak karena itu tidak akan memunculkan
 * pesan kesalahan apa pun.</p>
 *
 * <h2>Properti induk yang TIDAK dipetakan &mdash; dan akibatnya</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}:
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti apa pun miliknya. Itulah
 * sebabnya {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>dideklarasikan
 * ulang</b> di kelas ini &mdash; itu keharusan teknis, bukan duplikasi keliru. Sebaliknya, properti
 * induk yang <b>tidak</b> dideklarasikan ulang di sini menjadi state in-memory murni yang hilang
 * setiap kali object dimuat ulang dari database:</p>
 * <ul>
 *   <li><b>{@code keterangan}</b> &mdash; ini instance pola yang sama dengan
 *       {@code StatusAwalSiswa} (batch 56) dan {@link JenisPenilaian} (batch 60).
 *       {@code GrupPenilaianAction} menyediakan {@code Textbox} &quot;Keterangan&quot; setinggi 3
 *       baris, memanggil {@code setKeterangan(...)} pada {@code onSave(...)}, merendernya sebagai
 *       kolom &quot;Keterangan&quot; di grid, dan menyertakannya pada daftar kolom ekspor/impor
 *       Excel &mdash; tetapi tidak ada kolom {@code keterangan} pada pemetaan kelas ini, sehingga
 *       nilainya <b>tidak pernah tersimpan</b>. Kolom &quot;Keterangan&quot; pada grid selalu
 *       tampil kosong (bukan {@code null}: {@link GeneralValueObject#getKeterangan()} menormalkan
 *       {@code null} menjadi {@code ""}). Bandingkan dengan {@link GrupKategoriItemPenilaianSiswa}
 *       satu tingkat di bawah, yang memetakan {@code keterangan} dengan benar.</li>
 *   <li><b>{@code kode}</b> &mdash; juga tidak dipetakan, dan layar master tidak punya isian Kode.
 *       Ini mematikan satu fitur utuh: {@code GrupPenilaianUtil.ambilPoint(String, Date, Map, int)}
 *       menyapu seluruh cache {@code GrupPenilaian} untuk mengganti <i>kode grup lain</i> yang
 *       muncul di dalam sebuah formula dengan nilai grup tersebut (formula antar-grup, dibatasi 25
 *       level rekursi). Karena {@code getKode()} selalu {@code null}, pencocokannya menjadi
 *       pencarian teks {@code " null "} dan praktis tidak pernah cocok. Fitur itu mati berlapis:
 *       satu-satunya pemanggil overload tersebut adalah dirinya sendiri, jadi ia bahkan tidak
 *       pernah tercapai dari kode aplikasi.</li>
 *   <li><b>{@code nomorUrut}</b> dan <b>{@code nim}</b> &mdash; tidak dipetakan dan tidak pernah
 *       diisi; lihat catatan {@link #compareTo(GeneralValueObject)} soal cabang mati.</li>
 * </ul>
 *
 * <h2>Pembaca utama di aplikasi</h2>
 * <ul>
 *   <li>Layar penilaian siswa: {@code DetailPenilaianSiswaHelper} dan
 *       {@code DetailPenilaianLesSiswaHelper} membangun satu {@code Tab} per grup penilaian
 *       (ditambah tab &quot;Total&quot; bila {@link #getAdaTotal()}), lalu menyaring per
 *       {@link #getKhususTingkat()}/{@link #getKhususSemester()} terhadap tingkat/semester kelas
 *       siswa, dan mengunci input bila {@link #getNilaiBolehDinputOlehGuru()} bernilai
 *       {@code false}.</li>
 *   <li>Rapor dan rekap: {@code LaporanRaporSiswa} dan {@code LaporanRekapTotalNilai}; yang
 *       terakhir menyaring {@code grupPenilaian.tampilDirekap} lewat {@code createAlias(...)} di
 *       dua tempat.</li>
 *   <li>Ujian/pertemuan dan tugas: {@code PertemuanPunyaUjianSiswaHelper},
 *       {@code TugasMandiriHelper}, {@code TugasKelompokHelper}, {@code PenilaianSiswaAction}.</li>
 *   <li>Antarmuka luar: {@code NilaiSiswaApi} dan {@code ElearningApiUtil}.</li>
 *   <li>Cache: {@code InitData} mendaftarkan kelas ini pada {@code initClasses(...)}, sehingga
 *       seluruh barisnya di-<i>preload</i> ke cache {@code ConstantValues} saat startup dan dibaca
 *       kembali lewat {@code ConstantValues.ambilBerdasarClass(GrupPenilaian.class)}.</li>
 * </ul>
 *
 * <h2>Bom waktu &quot;aktif&quot; pada baris pemetaan &mdash; dilihat dari sisi entity ini</h2>
 * <p>Bug yang ditemukan batch 54-55 berada pada {@code GrupPenilaianAction.onSave(...)}, yaitu
 * Action yang mengelola <b>entity ini</b>. Urutannya:</p>
 * <ol>
 *   <li>Simpan/ubah baris {@code GrupPenilaian} ini lalu {@code session.flush()}.</li>
 *   <li>Muat <b>SEMUA</b> {@link DetailGrupPenilaian} milik grup ini &mdash; tanpa filter apa pun
 *       &mdash; dan setel {@code aktif = false} satu per satu.</li>
 *   <li>Hidupkan kembali ({@code aktif = true}) hanya baris yang ada di dalam peta pilihan
 *       {@code selectedGrupKategoriItemPenilaianSiswa}.</li>
 * </ol>
 * <p><b>Yang wajib dipahami dari sisi entity ini:</b> peta pilihan tersebut <b>bukan</b> koleksi
 * milik object {@code GrupPenilaian}. Kelas ini tidak punya koleksi sama sekali (lihat bagian
 * &quot;Tidak ada koleksi&quot; di atas), sehingga satu-satunya sumber kebenaran daftar centang
 * adalah peta sementara di layar, yang diisi oleh listener {@code ubahJenisPenialain} lewat
 * {@code Common.createDefaultTimer(...)} &mdash; timer ZK <b>asinkron 50 ms sekali jalan</b>. Bila
 * pengguna menekan Simpan sebelum timer itu selesai, langkah (2) tetap berjalan penuh sementara
 * langkah (3) tidak menghidupkan apa pun: <b>seluruh pemetaan grup kategori milik grup penilaian
 * ini lenyap dalam sekali klik</b>, tanpa pesan kesalahan. Overlay &quot;sibuk&quot; tidak
 * melindungi seluruh jendela: layar ini memasang <i>beberapa</i> timer sekaligus (pengisi combo
 * Jenis Nilai Huruf, satu per baris formula, dan pengisi daftar centang), dan
 * {@code CommonTimerHelper} membersihkan overlay itu begitu <b>timer pertama mana pun</b> selesai
 * &mdash; sehingga jendela sudah dapat diklik sebelum daftar centang terisi.</p>
 * <p>Dua asimetri filter memperlebar kerusakannya, keduanya khas grup penilaian:</p>
 * <ul>
 *   <li>Query pengisi peta menyaring {@code DetailGrupPenilaian.aktif} <i>dan</i>
 *       {@code grupKategoriItemPenilaianSiswa.aktif}, sedangkan langkah &quot;matikan semua&quot;
 *       tidak menyaring apa pun. Baris pemetaan yang grup kategorinya kebetulan sedang nonaktif
 *       karena itu ikut dimatikan tetapi tidak pernah dihidupkan kembali &mdash; pemetaannya hilang
 *       diam-diam begitu grup kategori tersebut diaktifkan lagi.</li>
 *   <li>Daftar centang hanya menampilkan {@link GrupKategoriItemPenilaianSiswa} yang aktif,
 *       sehingga pemetaan ke grup kategori nonaktif tidak dapat dipulihkan lewat layar ini.</li>
 * </ul>
 * <p>Perbaikan yang benar tidak boleh dilakukan hanya pada {@code GrupPenilaianAction}: bentuk
 * yang sama terdapat pada {@code JenisPenilaianAction} untuk {@link DetailJenisPenilaian} satu
 * tingkat di atas (lihat Javadoc kelas tersebut).</p>
 *
 * <h2>Verifikasi pola berulang lain</h2>
 * <ul>
 *   <li><b>Bug penciutan {@code TreeSet} (batch 50/55/59) &mdash; NEGATIF di sini.</b>
 *       {@link #getNomorUrut()} <b>tidak</b> di-override pada kelas ini (bandingkan
 *       {@code ParameterVerifikasiCalonSiswa} yang mengembalikan default 1), dan tidak ada satu pun
 *       {@code TreeSet}/{@code TreeMap}/{@code SortedSet} di repo yang menampung
 *       {@code GrupPenilaian}. Yang ada hanyalah enam pemanggilan {@code Collections.sort(...)}
 *       atas {@code List}; karena {@code Collections.sort} adalah pengurutan stabil, hasil
 *       {@code compareTo == 0} hanya mempertahankan urutan asal, tidak menghapus elemen. Risikonya
 *       tetap dicatat pada {@link #compareTo(GeneralValueObject)} bila kelak koleksi berbasis
 *       pohon dipakai.</li>
 *   <li><b>Getter destruktif/write-back.</b> Ada satu, tetapi ringan:
 *       {@link #getYayasan()} <b>menimpa</b> field {@code yayasan} dengan yayasan milik
 *       {@link #getSekolah()} setiap kali dipanggil. Karena Hibernate memakai <i>property
 *       access</i>, nilai hasil timpa itulah yang ikut tertulis saat flush. Efeknya menegakkan
 *       konsistensi (yayasan selalu mengikuti sekolah), bukan menghapus data pengguna seperti
 *       {@code ItemBiayaSekolah.getKelamin()} atau {@code NominalBiaya.getNominal()} pada batch 60
 *       &mdash; tetapi tetap berarti kolom {@code yayasan_id} seluruh grup penilaian sebuah sekolah
 *       akan tertulis ulang diam-diam bila yayasan sekolah itu dipindahkan.</li>
 *   <li><b>Fail-open cakupan tenant.</b> Ada, bentuk yang sudah dikenal.
 *       {@code GrupPenilaianAction.initCriteria(...)} tidak menurunkan sekolah/yayasan dari sesi
 *       login; ia hanya membaca dua combo pencarian, dan saat combo itu kosong ia menambahkan
 *       {@code Restrictions.sqlRestriction("1=1")}. Bagi akun yang konteks sekolah maupun
 *       yayasannya tidak dapat ditentukan {@code InitComboUtil}, daftar berisi grup penilaian
 *       <b>seluruh instalasi</b>, dan keempat checkbox baris ({@code Aktif}, {@code Boleh Diisi
 *       Guru}, {@code Tampil di Rekap}, {@code Ada Total}) langsung menyimpan perubahan lintas
 *       sekolah. Memperkuat temuan lama; bukan kekhususan file ini.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Ada. {@code grup_penilaian.zul} tidak memiliki entri
 *       menu sendiri; satu-satunya pintu adalah tab yang disisipkan {@code JenisPenilaianAction}
 *       lewat {@code MyInclude("/pages/master/sekolah/grup_penilaian.zul")}. Karena
 *       {@code CommonPrivilages.checkPrevilages(...)} dinilai terhadap URL menu yang sedang aktif,
 *       hak CRUD pada layar Jenis Penilaian otomatis menjadi hak CRUD di layar ini &mdash; dan pada
 *       tab-tab bertetangga, termasuk {@code /pages/master/konstanta.zul} (konstanta GLOBAL
 *       instalasi).</li>
 *   <li><b>Sisi positif gerbang hak akses.</b> Berbeda dari beberapa layar batch 55-60, layar
 *       master ini bergerbang dengan benar: tombol Tambah memakai
 *       {@code checkPrevilages(CREATE)}, {@code edit}/{@code delete} diambil dari
 *       {@code UPDATE}/{@code DELETE}, seluruh checkbox baris memakai {@code setDisabled(!edit)},
 *       tombol Ubah/Hapus lewat {@code Common.copyEditDeleteButtons(edit, delete, ...)}, dan tombol
 *       Upload Excel bahkan menuntut ketiga hak sekaligus. {@code RevisiHelper.createNewRevisi}
 *       juga dipanggil dengan kelas audit yang benar (bandingkan bug salah kelas pada
 *       {@code JadwalPelajaranPunyaItemHelper}, batch 58).</li>
 * </ul>
 *
 * <h2>Kuirk lain yang perlu diketahui</h2>
 * <ul>
 *   <li>Daftar kolom ekspor/impor Excel pada {@code GrupPenilaianAction} berisi properti
 *       <b>{@code "jenis"}</b> yang tidak ada pada kelas ini maupun induknya, sekaligus
 *       <b>tidak memuat {@code "nama"}</b> &mdash; sehingga berkas unduhan kehilangan nama grup
 *       penilaian, kolom utama layar itu sendiri.</li>
 *   <li>Judul jendela tambah/ubah tertulis &quot;Tambah/Ubah <b>Jenis</b> Penilaian&quot;, salah
 *       tempel dari layar {@link JenisPenilaian}; label isiannya sendiri berbunyi &quot;Nama
 *       Penilaian&quot;. Yang benar adalah &quot;Grup Penilaian&quot;, sebagaimana judul kolom
 *       grid dan pesan validasinya.</li>
 *   <li>{@link #DEFAULT_FORMULA} adalah {@code public static} <b>tanpa {@code final}</b>: nilainya
 *       dapat diganti dari mana saja dan berlaku untuk seluruh JVM. Bentuk yang sama terdapat pada
 *       {@code KasBesar}/{@code KasKecil}/{@code Pertangungjawaban} di modul akunting.</li>
 *   <li>Kolom {@code aktif}, {@code adaTotal}, {@code tampilDirekap},
 *       {@code nilaiBolehDinputOlehGuru}, {@code khususSemester}, dan {@code khususTingkat}
 *       dipetakan <b>tanpa {@code @Column}</b>, sehingga nama kolomnya mengikuti strategi penamaan
 *       default Hibernate (nama properti apa adanya; pada PostgreSQL identifier tanpa kutip dilipat
 *       menjadi huruf kecil). Jangan mengganti nama properti-properti ini tanpa migrasi kolom.</li>
 *   <li>{@code @Audited} (Envers) aktif untuk seluruh kolom kelas ini, tetapi perubahan pada
 *       pemetaan grup kategori tercatat pada tabel audit {@link DetailGrupPenilaian}, bukan di
 *       sini &mdash; jejak audit satu aksi simpan karena itu tersebar di dua tabel.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; representasi:</b> {@link #GrupPenilaian()}, {@link #getId()},
 *       {@link #setId(Long)}, {@link #getNama()}, {@link #setNama(String)}, {@link #toString()},
 *       {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Jejak audit (deklarasi ulang wajib):</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Cakupan tenant:</b> {@link #getSekolah()}, {@link #setSekolah(Sekolah)},
 *       {@link #getYayasan()}, {@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Konfigurasi perhitungan:</b> {@link #DEFAULT_FORMULA}, {@link #getFormula()},
 *       {@link #setFormula(String)}, {@link #getJenisNilaiHuruf()},
 *       {@link #setJenisNilaiHuruf(JenisNilaiHuruf)}, {@link #getAdaTotal()},
 *       {@link #setAdaTotal(Boolean)}.</li>
 *   <li><b>Cakupan berlakunya grup:</b> {@link #getKhususTingkat()},
 *       {@link #setKhususTingkat(Integer)}, {@link #getKhususSemester()},
 *       {@link #setKhususSemester(Integer)}.</li>
 *   <li><b>Kendali tampilan/pengisian:</b> {@link #getAktif()}, {@link #setAktif(Boolean)},
 *       {@link #getNilaiBolehDinputOlehGuru()}, {@link #setNilaiBolehDinputOlehGuru(Boolean)},
 *       {@link #getTampilDirekap()}, {@link #setTampilDirekap(Boolean)}.</li>
 * </ul>
 *
 * <p><b>Catatan komentar generator:</b> baris &quot;{@code GrupPenilaian generated by hbm2java}&quot;
 * yang semula menjadi seluruh isi Javadoc kelas ini adalah jejak pembangkitan {@code hbm2java};
 * kelasnya sudah lama disunting tangan (formula, cakupan tingkat/semester, flag tampilan) sehingga
 * keterangan itu tidak lagi menggambarkan isinya dan digantikan dokumentasi ini.</p>
 *
 * @see JenisPenilaian
 * @see DetailJenisPenilaian
 * @see DetailGrupPenilaian
 * @see GrupKategoriItemPenilaianSiswa
 * @see JenisItemPenilaianSiswa
 * @see JenisNilaiHuruf
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "grup_penilaian", schema = "sekolah")
public class GrupPenilaian extends GeneralValueObject {


	/**
	 * Urutan alami grup penilaian, dengan <b>prioritas kunci yang sengaja diubah</b> dari
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}.
	 *
	 * <p>Induk memakai urutan {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr;
	 * {@code keterangan}. Kelas ini membalik dua pasangan itu menjadi:</p>
	 * <ol>
	 *   <li>{@code nama} &mdash; satu-satunya kunci yang benar-benar dipetakan ke kolom;</li>
	 *   <li>{@code keterangan};</li>
	 *   <li>{@code nomorUrut};</li>
	 *   <li>{@code nim}.</li>
	 * </ol>
	 * <p>Sebuah kunci hanya dipakai bila <b>kedua</b> object memilikinya (non-{@code null}).</p>
	 *
	 * <p><b>Cabang 3 dan 4 adalah kode mati.</b> {@link GeneralValueObject#getKeterangan()}
	 * menormalkan {@code null} menjadi {@code ""}, sehingga cabang {@code keterangan} <b>selalu</b>
	 * memenuhi syarat begitu salah satu {@code nama} bernilai {@code null}. Alur eksekusi karena itu
	 * tidak pernah mencapai perbandingan {@code nomorUrut} maupun {@code nim} &mdash; yang memang
	 * juga tidak dipetakan pada kelas ini dan selalu {@code null}. Praktisnya method ini adalah
	 * &quot;urut menurut nama, jatuh ke keterangan (yang selalu kosong, jadi selalu setara)&quot;.</p>
	 *
	 * <p><b>Efek nyata:</b> enam titik di aplikasi memanggil {@code Collections.sort(...)} atas
	 * {@code List<GrupPenilaian>} &mdash; {@code DetailPenilaianSiswaHelper},
	 * {@code DetailPenilaianLesSiswaHelper}, {@code PenilaianSiswaAction},
	 * {@code LaporanRekapTotalNilai} (dua tempat), dan {@code NilaiSiswaApi}. Method inilah yang
	 * menentukan urutan tab pada layar penilaian, urutan blok kolom pada rekap nilai, dan urutan
	 * elemen pada respons API. Karena kuncinya nama dan perbandingannya peka besar-kecil huruf,
	 * urutannya alfabetis (huruf kapital mendahului huruf kecil) dan <b>tidak dapat diatur admin</b>:
	 * layar master tidak menyediakan isian nomor urut, dan cabang {@code nomorUrut} pun tidak
	 * terjangkau. Satu-satunya cara mengurutkan ulang tab adalah mengganti nama grupnya.</p>
	 *
	 * <p><b>Kehati-hatian:</b> dua grup penilaian bernama sama menghasilkan {@code 0}. Pada
	 * {@code Collections.sort} (pengurutan stabil) itu tidak berbahaya &mdash; urutan asalnya
	 * dipertahankan dan tidak ada elemen yang hilang. Namun {@code compareTo} di sini tidak
	 * konsisten dengan {@link GeneralValueObject#equals(Object)} (yang berbasis {@code id}), jadi
	 * menaruh {@code GrupPenilaian} di dalam {@code TreeSet}/{@code TreeMap} akan menciutkan
	 * grup-grup bernama sama menjadi satu &mdash; persis bug yang sudah terkonfirmasi pada entity
	 * lain. Saat ini tidak ada koleksi berbasis pohon seperti itu di repo; jangan menambahkannya
	 * tanpa comparator eksplisit.</p>
	 *
	 * <p>Seluruh badan dibungkus {@code try/catch}; kegagalan tak terduga dicatat ke audit error
	 * dan method mengembalikan {@code 0}.</p>
	 *
	 * @param arg0 grup penilaian (atau entity lain) pembanding
	 * @return negatif/nol/positif sesuai kontrak {@code Comparable}; {@code 0} bila tidak ada kunci
	 *         pembanding yang tersedia di kedua belah pihak
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/GrupPenilaian.java:51");

		}

		return 0;
	}

	/**
	 * Versi serialisasi kelas ini.
	 *
	 * <p>Object {@code GrupPenilaian} ikut diserialkan ke cache in-memory/MapDB
	 * ({@code InitData} mem-<i>preload</i> seluruh barisnya saat startup) dan ke session ZK, jadi
	 * mengubah nilai ini membuat data ter-cache lama tidak terbaca ({@code InvalidClassException})
	 * setelah deploy. Jangan diubah.</p>
	 */
	private static final long serialVersionUID = -8817799955174105108L;
	/**
	 * Primary key ({@code id}). Dideklarasikan ulang karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat {@link #getId()}.
	 */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini (kolom {@code oleh}); lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini (kolom {@code olehId}); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir, dengan <b>validasi non-trivial</b>: nilai
	 * {@code null} atau kosong/spasi diabaikan diam-diam (method langsung {@code return}).
	 *
	 * <p>Akibatnya jejak audit yang sudah terisi tidak dapat dihapus lewat setter ini &mdash;
	 * disengaja, agar jalur simpan yang berjalan tanpa sesi login (proses batch, penjadwal, impor
	 * Excel) tidak menghapus informasi pengguna yang sudah ada.</p>
	 *
	 * @param olehId id pengguna penyimpan; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam sehingga jejak
	 * audit yang sudah terisi tidak pernah terhapus lewat setter.
	 *
	 * @param oleh nama pengguna penyimpan; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum {@code UPDATE}: meneruskan object ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(...)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} diperbarui dari sesi pengguna aktif.
	 *
	 * <p>Perhatikan bahwa kait ini <b>tidak</b> ikut terpanggil untuk perubahan pemetaan grup
	 * kategori: baris yang berubah pada langkah &quot;matikan semua lalu hidupkan yang dicentang&quot;
	 * milik {@code GrupPenilaianAction.onSave(...)} adalah {@link DetailGrupPenilaian}, bukan baris
	 * ini. Jadi {@code tanggal_dirubah} di sini bisa saja tidak bergerak walau isi grupnya berubah
	 * total.</p>
	 *
	 * <p>Baris ini sengaja ditulis rapat dengan deklarasi field {@code tanggal_dirubah} (stempel
	 * waktu perubahan terakhir, diinisialisasi ke jam server aplikasi lewat
	 * {@code WaktuUtil.getDate()} sehingga baris baru selalu punya nilai) karena mengikuti bentuk
	 * seragam seluruh entity AIS; jangan dirapikan tanpa menyapu semua entity.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir; dipetakan sebagai {@code TIMESTAMP} sehingga
	 * bagian jam ikut tersimpan.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} kecuali sengaja disetel demikian
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik grup penilaian ini (FK {@code sekolah_id}); lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Penanda baris masih dipakai; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Yayasan pemilik, selalu diturunkan ulang dari {@code sekolah}; lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Nama grup penilaian (mis. &quot;Pengetahuan&quot;); lihat {@link #getNama()}. */
	private String nama;
	/** Riwayat formula ber-tanggal dalam bentuk teks JSON; lihat {@link #getFormula()}. */
	private String formula;
	/** Skala nilai huruf yang dipakai grup ini; lihat {@link #getJenisNilaiHuruf()}. */
	private JenisNilaiHuruf jenisNilaiHuruf;
	/** Apakah guru boleh mengisi nilai grup ini sendiri; lihat {@link #getNilaiBolehDinputOlehGuru()}. */
	private Boolean nilaiBolehDinputOlehGuru;
	/** Apakah grup ini ikut ditampilkan pada rekap nilai; lihat {@link #getTampilDirekap()}. */
	private Boolean tampilDirekap;
	/** Pembatasan semester berlakunya grup ({@code null} = semua); lihat {@link #getKhususSemester()}. */
	private Integer khususSemester;
	/** Pembatasan tingkat/kelas berlakunya grup ({@code null} = semua); lihat {@link #getKhususTingkat()}. */
	private Integer khususTingkat;
	/** Apakah grup ini memunculkan kolom/tab &quot;Total&quot;; lihat {@link #getAdaTotal()}. */
	private Boolean adaTotal;

	/**
	 * Mengembalikan nama grup penilaian (kolom {@code nama}).
	 *
	 * <p>Ini satu-satunya identitas grup yang dipetakan ke kolom &mdash; {@code kode} tidak
	 * dipetakan &mdash; sehingga nama inilah yang muncul sebagai judul tab pada layar penilaian,
	 * judul blok kolom pada rapor/rekap, dan sekaligus <b>kunci pengurutan tunggal</b> lewat
	 * {@link #compareTo(GeneralValueObject)}. Wajib diisi: {@code GrupPenilaianAction.onSave(...)}
	 * menolak simpan dengan pesan &quot;Nama Grup Penilaian harus diisi&quot; bila kosong.</p>
	 *
	 * @return nama grup penilaian, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama")
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama grup penilaian. Tanpa validasi di sisi model; kewajiban isi ditegakkan di
	 * {@code GrupPenilaianAction.onSave(...)}.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Representasi teks grup penilaian: <b>hanya {@code nama}</b>.
	 *
	 * <p>Meng-override {@link GeneralValueObject#toString()} yang berformat {@code "kode - nama"}.
	 * Override ini bukan sekadar selera: karena {@code kode} tidak dipetakan pada kelas ini,
	 * bentuk induk akan selalu menghasilkan {@code "null - <nama>"}. Nilai kembalian dipakai
	 * langsung sebagai label {@code Combobox}/{@code Listcell} di banyak layar, jadi mengubah
	 * format ini berdampak luas.</p>
	 *
	 * <p><b>Catatan:</b> tidak ada penjagaan {@code null} &mdash; grup tanpa nama akan tercetak
	 * sebagai teks {@code "null"}.</p>
	 *
	 * @return nama grup penilaian apa adanya
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate memerlukannya untuk membuat
	 * instance saat hidrasi entity, dan dipakai {@code GrupPenilaianAction.onAdd(...)} untuk
	 * menyiapkan form tambah data kosong.
	 */
	public GrupPenilaian() {
	}

	/**
	 * Mengembalikan primary key baris ini (kolom {@code id}, {@code IDENTITY}).
	 *
	 * <p>{@code insertable = false} karena nilainya dibangkitkan database. Id ini menjadi dasar
	 * {@link GeneralValueObject#equals(Object)}, kunci cache {@code ConstantValues}, dan kunci
	 * seluruh {@code Criteria} yang menelusuri rantai penilaian dari/ke grup ini.</p>
	 *
	 * @return primary key, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik grup penilaian ini (FK {@code sekolah_id}).
	 *
	 * <p>Memanggil {@link GeneralValueObject#check(Object)} lebih dulu &mdash; pola getter relasi
	 * standar seluruh entity AIS &mdash; lalu menugaskan hasilnya kembali ke field, karena
	 * {@code check()} dapat mengembalikan instance lain (kanonik dari identity map, dari cache,
	 * atau hasil reload lewat session baru). Tanpa itu, grup penilaian yang dibaca dari cache
	 * <i>preload</i> {@code InitData} akan melempar {@code LazyInitializationException} saat
	 * relasi ini disentuh di luar session yang memuatnya.</p>
	 *
	 * <p>Inilah satu-satunya sumber cakupan tenant yang benar untuk grup ini; {@link #getYayasan()}
	 * hanya menurunkannya. Perhatikan bahwa kolom ini <b>boleh {@code null}</b> pada level
	 * pemetaan &mdash; kewajiban isinya hanya ditegakkan di layar
	 * ({@code GrupPenilaianAction.onSave(...)} menolak simpan tanpa sekolah), sehingga baris yang
	 * masuk lewat SQL mentah atau migrasi bisa saja tanpa sekolah dan lolos seluruh filter tenant.</p>
	 *
	 * @return sekolah pemilik yang sudah teresolusi, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan <b>penjagaan non-trivial</b>: object yang {@code null}
	 * <i>atau</i> yang {@code getId()}-nya masih {@code null} (entity belum tersimpan) disimpan
	 * sebagai {@code null}.
	 *
	 * <p>Tujuannya mencegah Hibernate mencoba mem-<i>persist</i> sekolah baru lewat
	 * {@code CascadeType.PERSIST} pada relasi ini. Efek sampingnya: menyetel sekolah yang belum
	 * tersimpan <b>gagal diam-diam</b> &mdash; pemanggil tidak mendapat kesalahan apa pun, dan
	 * baris grup penilaian tersimpan tanpa sekolah (lihat catatan fail-open tenant pada Javadoc
	 * kelas).</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau entity tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik grup penilaian ini (FK {@code yayasan_id}), <b>dengan
	 * menurunkannya ulang dari {@link #getSekolah()} setiap kali dipanggil</b>.
	 *
	 * <p>Ini getter ber-efek-samping: bila sekolah terisi, field {@code yayasan} <b>ditimpa</b>
	 * dengan {@code sekolah.getYayasan()} &mdash; nilai yayasan yang pernah disetel eksplisit lewat
	 * {@link #setYayasan(Yayasan)} akan hilang begitu getter ini dipanggil. Karena Hibernate
	 * memetakan kelas ini dengan <i>property access</i> ({@code @Id} berada pada getter), nilai
	 * hasil timpa itulah yang ikut tertulis ke kolom saat flush berikutnya.</p>
	 *
	 * <p><b>Kapan ini terasa:</b> memindahkan sebuah sekolah ke yayasan lain akan menulis ulang
	 * kolom {@code yayasan_id} seluruh grup penilaian sekolah tersebut secara diam-diam, satu per
	 * satu, saat barisnya kebetulan tersentuh. Perilaku ini menegakkan konsistensi (yayasan selalu
	 * mengikuti sekolah) dan bukan penghapusan data pengguna &mdash; berbeda dari getter destruktif
	 * seperti {@code ItemBiayaSekolah.getKelamin()} &mdash; tetapi tetap harus diperhitungkan
	 * ketika membaca jejak audit Envers, karena perubahan itu tidak berasal dari aksi pengguna
	 * mana pun.</p>
	 *
	 * <p>Setelah penurunan, hasilnya tetap dilewatkan {@link GeneralValueObject#check(Object)}
	 * untuk meresolusi proxy lazy.</p>
	 *
	 * @return yayasan pemilik yang sudah teresolusi, atau {@code null} bila sekolah maupun yayasan
	 *         tidak diisi
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
	 * Menyetel yayasan pemilik, dengan penjagaan yang sama seperti
	 * {@link #setSekolah(Sekolah)}: {@code null} atau entity yang belum punya id disimpan sebagai
	 * {@code null}.
	 *
	 * <p><b>Nilai yang disetel di sini tidak awet.</b> {@link #getYayasan()} akan menimpanya dengan
	 * yayasan milik sekolah pada pemanggilan berikutnya bila sekolah terisi. Setter ini karena itu
	 * hanya benar-benar berpengaruh untuk baris yang tidak punya sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau entity tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan skala nilai huruf yang dipakai grup ini (FK {@code jenis_nilai_huruf}),
	 * setelah meresolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Menentukan bagaimana angka hasil evaluasi {@link #getFormula()} diterjemahkan menjadi
	 * huruf (A/B/C/&hellip;) pada layar penilaian dan rapor. Boleh {@code null}: pada layar master
	 * pilihan pertama combo-nya berbunyi &quot;=Tanpa Jenis Nilai Huruf=&quot;, artinya grup ini
	 * hanya menampilkan angka. Combo tersebut disaring per sekolah yang sedang dipilih dan hanya
	 * memuat {@link JenisNilaiHuruf} yang aktif; isinya dimuat lewat listener {@code onChange}
	 * combo Sekolah <i>dan</i> satu timer ZK, sehingga saat membuka data lama pilihannya baru
	 * terpasang beberapa puluh milidetik setelah jendela tampil.</p>
	 *
	 * @return skala nilai huruf yang sudah teresolusi, atau {@code null} bila grup ini tanpa nilai
	 *         huruf
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_nilai_huruf")
	public JenisNilaiHuruf getJenisNilaiHuruf() {
		jenisNilaiHuruf = check(jenisNilaiHuruf);
		return jenisNilaiHuruf;
	}

	/**
	 * Menyetel skala nilai huruf grup ini. Tanpa penjagaan id (berbeda dari
	 * {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}), sehingga entity yang belum
	 * tersimpan akan ikut di-<i>persist</i> lewat {@code CascadeType.PERSIST}.
	 *
	 * <p>Dipanggil {@code GrupPenilaianAction.onSave(...)} dengan nilai combo; {@code null}
	 * berarti grup ini tidak memakai nilai huruf.</p>
	 *
	 * @param jenisNilaiHuruf skala nilai huruf baru, boleh {@code null}
	 */
	public void setJenisNilaiHuruf(JenisNilaiHuruf jenisNilaiHuruf) {
		this.jenisNilaiHuruf = jenisNilaiHuruf;
	}

	/**
	 * Mengembalikan status aktif grup penilaian, dengan <b>normalisasi {@code null} menjadi
	 * {@code true}</b>.
	 *
	 * <p>Grup yang nonaktif tidak lagi muncul pada daftar master (filter bawaan &quot;Tampilkan
	 * hanya yang aktif&quot;) dan disaring pada seluruh pembaca rantai penilaian, yang semuanya
	 * memakai bentuk toleran-NULL {@code isNull("aktif") OR eq("aktif", true)} &mdash; konsisten
	 * dengan normalisasi di sini.</p>
	 *
	 * <p><b>Catatan pengamatan (tanpa kesimpulan):</b> nilai kolom ini tidak pernah disetel di
	 * {@code GrupPenilaianAction.onSave(...)}; satu-satunya penulis adalah checkbox
	 * &quot;Aktif&quot; per baris di grid. Karena Hibernate memakai <i>property access</i>, nilai
	 * yang tertulis saat {@code INSERT} adalah hasil getter ini &mdash; jadi {@code true} untuk
	 * baris yang dibuat lewat layar. Baris yang masuk lewat SQL mentah/migrasi dapat tetap
	 * {@code NULL} di database; itu tidak menimbulkan masalah di sini justru karena semua filter
	 * pembacanya toleran-NULL. Verifikasi empiris nilai kolom sesungguhnya belum dilakukan.</p>
	 *
	 * @return {@code true} bila grup aktif atau kolomnya belum pernah diisi; {@code false} bila
	 *         sengaja dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif grup penilaian. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox &quot;Aktif&quot; pada grid master, yang
	 * langsung menyimpan perubahan lewat {@code Common.refreshSaveOrUpdate(...)} tanpa dialog
	 * konfirmasi. Checkbox tersebut dinonaktifkan bila pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param aktif status aktif baru; {@code null} akan terbaca sebagai {@code true} lewat
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nilai bawaan kolom {@code formula}: teks JSON array kosong ({@code "[]"}), yaitu riwayat
	 * formula tanpa satu pun versi.
	 *
	 * <p><b>Peringatan:</b> {@code public static} <b>tanpa {@code final}</b> &mdash; nilainya dapat
	 * ditimpa dari mana saja dan berlaku untuk seluruh JVM. Bentuk yang sama terdapat pada
	 * {@code KasBesar}, {@code KasKecil}, dan {@code Pertangungjawaban} di modul akunting;
	 * perlakukan sebagai konstanta meski compiler tidak menegakkannya.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan riwayat formula grup ini sebagai teks JSON, dengan <b>normalisasi</b>: nilai
	 * {@code null} maupun string kosong dikembalikan sebagai {@link #DEFAULT_FORMULA}
	 * ({@code "[]"}) sehingga pemanggil selalu bisa langsung membungkusnya dengan
	 * {@code new JSONArray(...)} tanpa memeriksa {@code null}.
	 *
	 * <p><b>Bentuk isinya</b> adalah array versi ber-tanggal, bukan satu ekspresi tunggal. Setiap
	 * elemen memuat {@code tgl} (tanggal efektif), {@code target} (ekspresi nilai),
	 * {@code target_min}/{@code target_max} (batas validasi), ditambah nilai contoh per kode
	 * {@link JenisItemPenilaianSiswa} yang dipakai untuk pratinjau angka di layar master.
	 * {@code GrupPenilaianUtil.ambilTarget(...)}/{@code ambilTargetMin(...)}/{@code ambilTargetMax(...)}
	 * memilih versi bertanggal efektif terbaru yang tidak melewati tanggal evaluasi &mdash; itulah
	 * mekanisme yang menjaga nilai historis tidak berubah ketika formula diperbarui.</p>
	 *
	 * <p><b>Tidak divalidasi.</b> Kolomnya {@code text} dan {@code onSave(...)} menyimpan apa pun
	 * yang ada di layar. Ekspresi yang tidak valid baru gagal saat evaluasi, dan kegagalan itu
	 * dikembalikan sebagai {@code 0.0} secara diam-diam.</p>
	 *
	 * <p><b>Konsekuensi normalisasi:</b> karena getter ini tidak pernah mengembalikan
	 * {@code null}, dan Hibernate memakai <i>property access</i>, baris yang kolom formulanya
	 * {@code NULL} di database akan tertulis ulang menjadi {@code "[]"} begitu tersentuh flush.</p>
	 *
	 * @return teks JSON riwayat formula; {@link #DEFAULT_FORMULA} bila belum diisi &mdash; tidak
	 *         pernah {@code null}
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Menyetel riwayat formula grup ini sebagai teks JSON. Tanpa validasi bentuk maupun isi.
	 *
	 * <p>Satu-satunya pemanggil di aplikasi adalah {@code GrupPenilaianAction.onSave(...)}, yang
	 * meneruskan {@code array.toString()} &mdash; {@link JSONArray} yang disunting langsung
	 * (<i>in place</i>) oleh baris-baris formula di layar. Perhatikan bahwa menghapus satu baris
	 * formula di layar tidak mengecilkan array itu: elemen yang dihapus <b>diganti dengan
	 * {@code JSONObject} kosong</b>, sehingga teks yang tersimpan di sini terus tumbuh berisi
	 * elemen-elemen kosong seiring penyuntingan. Elemen kosong itu diabaikan pembacanya (semuanya
	 * menyaring {@code tgl} tidak-null), jadi tidak mengubah hasil hitung.</p>
	 *
	 * @param formula teks JSON riwayat formula; {@code null}/kosong akan terbaca sebagai
	 *                {@link #DEFAULT_FORMULA}
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan pembatasan tingkat/kelas tempat grup ini berlaku, dengan <b>normalisasi</b>:
	 * nilai {@code <= 0} dikembalikan sebagai {@code null}.
	 *
	 * <p>{@code null} berarti &quot;Semua Tingkat&quot;. Nilai yang valid 1&ndash;12, sesuai isi
	 * combo pada layar master. Normalisasi ini penting karena seluruh pembaca memakai pola
	 * {@code getKhususTingkat() != null && !getKhususTingkat().equals(tingkatKelas)} untuk
	 * <i>melewati</i> grup yang tidak berlaku; tanpa normalisasi, nilai {@code 0} warisan data lama
	 * akan menyembunyikan grup dari semua tingkat sekaligus.</p>
	 *
	 * <p>Dipakai di {@code DetailPenilaianSiswaHelper}, {@code DetailPenilaianLesSiswaHelper},
	 * {@code PertemuanPunyaUjianSiswaHelper}, {@code PenilaianSiswaAction},
	 * {@code TugasMandiriHelper}, {@code TugasKelompokHelper}, dan {@code ElearningApiUtil}.
	 * Perhatikan bahwa penyaringan ini dilakukan <b>di Java setelah data dimuat</b>, bukan di
	 * query.</p>
	 *
	 * @return tingkat/kelas khusus (1&ndash;12), atau {@code null} bila grup berlaku untuk semua
	 *         tingkat
	 */
	public Integer getKhususTingkat() {
		return khususTingkat != null && khususTingkat <= 0 ? null : khususTingkat;
	}

	/**
	 * Menyetel pembatasan tingkat/kelas grup ini. Tanpa validasi rentang.
	 *
	 * <p>Dipanggil {@code GrupPenilaianAction.onSave(...)} dengan nilai combo &quot;Khusus buat
	 * tingkat&quot;; pilihan pertamanya (&quot;Semua Tingkat&quot;) bernilai {@code null}.</p>
	 *
	 * @param khususTingkat tingkat/kelas khusus; {@code null} atau nilai {@code <= 0} berarti semua
	 *                      tingkat
	 */
	public void setKhususTingkat(Integer khususTingkat) {
		this.khususTingkat = khususTingkat;
	}

	/**
	 * Mengembalikan pembatasan semester tempat grup ini berlaku, dengan normalisasi yang sama
	 * seperti {@link #getKhususTingkat()}: nilai {@code <= 0} dikembalikan sebagai {@code null}.
	 *
	 * <p>{@code null} berarti &quot;Semua Semester&quot;; nilai yang valid 1 atau 2. Berbeda dari
	 * pembatasan tingkat yang dipakai untuk <i>melewati</i> grup, nilai ini dipakai pembacanya
	 * untuk <b>mempersempit rentang semester yang dirender</b>: {@code DetailPenilaianSiswaHelper}
	 * dan {@code DetailPenilaianLesSiswaHelper} mengganti larik semester menjadi
	 * {@code new int[] { getKhususSemester() }} sehingga hanya semester itu yang muncul.</p>
	 *
	 * @return semester khusus (1 atau 2), atau {@code null} bila grup berlaku untuk semua semester
	 */
	public Integer getKhususSemester() {
		return khususSemester != null && khususSemester <= 0 ? null : khususSemester;
	}

	/**
	 * Menyetel pembatasan semester grup ini. Tanpa validasi rentang.
	 *
	 * <p>Dipanggil {@code GrupPenilaianAction.onSave(...)} dengan nilai combo &quot;Khusus buat
	 * Semester&quot;; pilihan pertamanya (&quot;Semua Semester&quot;) bernilai {@code null}.</p>
	 *
	 * @param khususSemester semester khusus; {@code null} atau nilai {@code <= 0} berarti semua
	 *                       semester
	 */
	public void setKhususSemester(Integer khususSemester) {
		this.khususSemester = khususSemester;
	}

	/**
	 * Mengembalikan apakah guru boleh mengisi sendiri nilai pada grup ini, dengan
	 * <b>normalisasi {@code null} menjadi {@code true}</b> &mdash; artinya bawaannya
	 * <b>boleh</b>.
	 *
	 * <p>Ini gerbang bisnis, bukan hak akses sistem: {@code DetailPenilaianSiswaHelper} dan
	 * {@code DetailPenilaianLesSiswaHelper} memakainya untuk mengunci kotak isian nilai bagi akun
	 * guru pada grup yang nilainya harus datang dari sumber lain (mis. hasil hitung formula atau
	 * input tata usaha). Karena bawaannya {@code true}, grup baru selalu terbuka untuk guru sampai
	 * admin sengaja mematikannya.</p>
	 *
	 * <p>{@link GrupKategoriItemPenilaianSiswa} satu tingkat di bawah punya flag bernama sama;
	 * keduanya diperiksa terpisah pada layar penilaian.</p>
	 *
	 * @return {@code true} bila guru boleh mengisi nilai grup ini atau kolomnya belum pernah diisi
	 */
	public Boolean getNilaiBolehDinputOlehGuru() {
		return nilaiBolehDinputOlehGuru == null ? true : nilaiBolehDinputOlehGuru;
	}

	/**
	 * Menyetel izin pengisian nilai oleh guru. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox &quot;Boleh Diinput Guru&quot; pada grid
	 * master, yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate(...)}; checkbox itu
	 * dinonaktifkan bila pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param nilaiBolehDinputOlehGuru izin baru; {@code null} akan terbaca sebagai {@code true}
	 */
	public void setNilaiBolehDinputOlehGuru(Boolean nilaiBolehDinputOlehGuru) {
		this.nilaiBolehDinputOlehGuru = nilaiBolehDinputOlehGuru;
	}

	/**
	 * Mengembalikan apakah grup ini memunculkan kolom/tab agregat &quot;Total&quot;, dengan
	 * <b>normalisasi {@code null} menjadi {@code true}</b>.
	 *
	 * <p>Pada layar penilaian, {@code DetailPenilaianSiswaHelper} dan
	 * {@code DetailPenilaianLesSiswaHelper} menambahkan satu tab tambahan tanpa grup kategori
	 * (nilai gabungan seluruh kategori grup, dihitung memakai {@link #getFormula()}) hanya bila
	 * flag ini bernilai {@code true}. Karena bawaannya {@code true}, grup baru selalu punya tab
	 * Total sampai admin mematikannya.</p>
	 *
	 * @return {@code true} bila tab/kolom Total ditampilkan atau kolomnya belum pernah diisi
	 */
	public Boolean getAdaTotal() {
		return adaTotal == null ? true : adaTotal;
	}

	/**
	 * Menyetel apakah grup ini memunculkan kolom/tab &quot;Total&quot;. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox &quot;Ada Total&quot; pada grid master,
	 * yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param adaTotal nilai baru; {@code null} akan terbaca sebagai {@code true}
	 */
	public void setAdaTotal(Boolean adaTotal) {
		this.adaTotal = adaTotal;
	}

	/**
	 * Mengembalikan apakah grup ini ikut ditampilkan pada rekap nilai, dengan <b>normalisasi
	 * {@code null} menjadi {@code true}</b>.
	 *
	 * <p>Satu-satunya penegak flag ini adalah {@code LaporanRekapTotalNilai}, yang menyaringnya di
	 * <b>query</b> lewat {@code createAlias("grupPenilaian", ...)} dengan bentuk toleran-NULL
	 * {@code isNull("grupPenilaian.tampilDirekap") OR eq("grupPenilaian.tampilDirekap", true)} di
	 * dua tempat &mdash; konsisten dengan normalisasi di sini. Perhatikan bahwa laporan lain
	 * ({@code LaporanRaporSiswa}) maupun API nilai <b>tidak</b> memeriksanya, jadi mematikan flag
	 * ini menyembunyikan grup dari rekap total saja, bukan dari rapor atau dari
	 * {@code NilaiSiswaApi}.</p>
	 *
	 * @return {@code true} bila grup ikut tampil di rekap atau kolomnya belum pernah diisi
	 */
	public Boolean getTampilDirekap() {
		return tampilDirekap == null ? true : tampilDirekap;
	}

	/**
	 * Menyetel apakah grup ini ikut ditampilkan pada rekap nilai. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox &quot;Tampil di rekap&quot; pada grid
	 * master, yang langsung menyimpan lewat {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param tampilDirekap nilai baru; {@code null} akan terbaca sebagai {@code true}
	 */
	public void setTampilDirekap(Boolean tampilDirekap) {
		this.tampilDirekap = tampilDirekap;
	}

}
