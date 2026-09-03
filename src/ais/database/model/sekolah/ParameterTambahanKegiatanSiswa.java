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
import ais.database.model.ParameterTambahan;

/**
 * Entity <b>penghubung</b> pada rantai <i>field kustom</i> ("parameter tambahan") modul
 * <b>SEKOLAH</b> untuk layar <b>Kegiatan Siswa</b> (ekstrakurikuler/organisasi/prestasi). Satu baris
 * tabel {@code sekolah.parameter_tambahan_kegiatan_siswa} menyatakan satu fakta tunggal:
 * <i>"definisi field X dipakai pada kelompok kegiatan Y"</i>.
 *
 * <h3>Rantai konfigurasi (3 lapis &mdash; lebih pendek dari saudara-saudaranya)</h3>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi field generik</b>: label
 *   ({@code labelInputan}), tipe inputan ({@code tipeDataInputan}), daftar nilai pilihan
 *   ({@code nilaiDataInputan}), wajib/tidak, wajib lampiran/tidak, syarat tampil (skip-logic),
 *   hierarki {@code parent}, dan nomor urut. Tabel ini dipakai <b>bersama oleh SELURUH modul</b>
 *   AIS (perguruan tinggi maupun sekolah), jadi satu definisi bisa dipetakan ke banyak layar
 *   sekaligus.</li>
 *   <li>{@link KelompokKegiatanSiswa} &mdash; kategori/heading yang menjadi judul seksi pada
 *   formulir.</li>
 *   <li><b>Kelas ini</b> &mdash; <b>penghubung</b> antara (1) dan (2). Tanpa baris di sini, sebuah
 *   definisi {@link ParameterTambahan} tidak akan pernah muncul di formulir Kegiatan Siswa.</li>
 * </ol>
 *
 * <p><b>Perbedaan struktural penting dari anggota keluarga lain.</b> Pada varian
 * {@code ParameterTambahanCatatanSiswa}, {@code ParameterTambahanCatatanGuru},
 * {@code ParameterTambahanCatatanKelasSiswa}, dan {@code ParameterTambahanGelombangPendaftaranPsb},
 * lapis kedua adalah entity kategori khusus bernama {@code KelompokParameterTambahan*} yang
 * <b>hanya</b> berfungsi sebagai heading formulir, dan di atasnya masih ada lapis keempat
 * ({@code Jenis*}) berupa {@code @ManyToMany} pencentang kategori. Di sini <b>tidak ada</b> entity
 * {@code KelompokParameterTambahanKegiatanSiswa} sama sekali: perannya diambil alih oleh
 * {@link KelompokKegiatanSiswa}, yaitu master <i>domain</i> "Kelompok Kegiatan Siswa" yang sudah ada
 * untuk keperluan lain (punya {@code poin}, {@code bisaDipilihSiswa}, {@code aktif},
 * {@code defaultData}, cakupan {@code sekolah}/{@code yayasan}). Konsekuensinya rantai di sini hanya
 * <b>3 lapis</b>, dan pemilihan kategori bukan lewat pencentangan terpisah melainkan lewat kolom
 * {@code kelompokKegiatanSiswa} pada baris data {@link KegiatanSiswa} itu sendiri &mdash; siswa
 * memilih satu kelompok kegiatan, dan formulir merender <b>hanya</b> parameter milik kelompok itu.
 *
 * <h3>Ke mana nilai isian pengguna disimpan</h3>
 * <p>Entity ini <b>tidak menyimpan nilai isian sama sekali</b> &mdash; ia murni konfigurasi. Pemilik
 * data sesungguhnya adalah {@link KegiatanSiswa} (tabel {@code sekolah.kegiatan_siswa}), yang
 * menampung seluruh jawaban dalam <b>dua kolom {@code text}</b>, ditulis berbarengan oleh
 * {@link KegiatanSiswa#populateParameterTambahanKegiatanSiswa(java.util.List)}:</p>
 * <ul>
 *   <li><b>{@code nilai}</b> &mdash; versi <i>berlabel</i> (untuk tampilan/rekap), satu baris per
 *   field dipisah {@code "\n"}, tiap baris <b>8 ruas</b> dipisah {@code "<=>"}:
 *   <pre>
 * namaKelompok-&gt;labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut &lt;=&gt; idParameter &lt;=&gt; idKelompok &lt;=&gt; indexKe &lt;=&gt; keterangan
 *   </pre>
 *   Perhatikan: <b>8 ruas</b>, bukan 7 seperti sub-keluarga "Catatan*" dan bukan 6 seperti varian
 *   {@code CatatanGuru}. Varian ini mempertahankan ruas {@code indexKe} (indeks pengulangan baris
 *   form) yang di sub-keluarga "Catatan*" sudah dibuang, sehingga formatnya justru <b>sama panjang
 *   dengan varian biodata Alumni/Mahasiswa versi perguruan tinggi</b>. Pembacanya adalah
 *   {@link KegiatanSiswa#ambilDataParameterTambahan()}, yang hanya memakai ruas 0&ndash;4 (label,
 *   nilai, URL, nomor urut, id parameter) dan mengabaikan ruas 5&ndash;7.</li>
 *   <li><b>{@code nilai_inds}</b> &mdash; versi <i>ber-ID</i> (untuk mengisi ulang formulir), satu
 *   baris per field dipisah {@code "\n"}, tiap baris <b>4 ruas</b>:
 *   <pre>
 * idKelompok-&gt;idParameter &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
 *   </pre>
 *   Dibaca oleh {@link ais.action.master.sekolah.helper.ParameterTambahanKegiatanSiswaListener}
 *   (perakit formulir dan validator wajib-isi) serta oleh
 *   {@code ais.common.ParameterTambahanHtmlHelper.petaNilaiDariInds(...)} yang membangun peta
 *   {@code idParameter -> nilai} untuk evaluasi <i>syarat tampil</i> (skip-logic).</li>
 * </ul>
 * <p>Kunci gabungan <b>{@code idKelompok + "->" + idParameter}</b> dipakai konsisten di tiga tempat:
 * sebagai ruas pertama {@code nilai_inds}, sebagai penanda baris saat formulir dibangun ulang, dan
 * sebagai argumen {@code jenis} pada {@code LampiranLain.ambil(kegiatanSiswa.getId(), jenis)}
 * &mdash; jadi berkas unggahan tiap field disimpan di {@link ais.database.model.file.LampiranLain}
 * dengan {@code idPemilik} = id {@link KegiatanSiswa} dan {@code jenis} = kunci gabungan tersebut.
 * Mengubah id kelompok atau id parameter setelah data terisi akan memutus ketiga kaitan itu
 * sekaligus.</p>
 *
 * <h3>Bagaimana baris entity ini dibaca saat formulir dibangun</h3>
 * <p>Berbeda dari varian "Catatan*" yang memakai {@code Projections.groupProperty("parameterTambahan.id")}
 * sehingga hasil query-nya berupa {@link ParameterTambahan} (bukan objek kelas ini), perakit
 * formulir Kegiatan Siswa memuat <b>objek kelas ini apa adanya</b>:</p>
 * <pre>
 * session.createCriteria(ParameterTambahanKegiatanSiswa.class)
 *     .add(Restrictions.eq("kelompokKegiatanSiswa", kelompokKegiatanSiswa))
 *     .createAlias("parameterTambahan", "parameterTambahan")
 *     .add(parent == null ? Restrictions.isNull("parameterTambahan.parent")
 *                         : Restrictions.eq("parameterTambahan.parent", parent))
 *     .createAlias("kelompokKegiatanSiswa", "kelompokKegiatanSiswa")
 *     .add(Restrictions.eq("parameterTambahan.aktif", true))
 *     .add(Restrictions.eq("kelompokKegiatanSiswa.aktif", true))
 * </pre>
 * <p>lalu {@code Collections.sort(...)}. Akibatnya atribut milik entity ini <b>benar-benar terbaca
 * runtime</b> &mdash; berbeda dari beberapa saudaranya di mana {@code nomorUrut} entity penghubung
 * adalah kode mati:</p>
 * <ul>
 *   <li>{@link #getNomorUrut()} dipakai sebagai kunci urut pertama oleh
 *   {@code GeneralValueObject.compareTo(...)} yang dipanggil {@code Collections.sort} di
 *   {@code displayRinci(...)} maupun di dasbor rekap;</li>
 *   <li>{@link #getWajibDiisi()} dipakai dua kali: menempelkan penanda <code>" (*)"</code> pada
 *   label field, dan menentukan apakah field wajib divalidasi sebelum {@link KegiatanSiswa}
 *   boleh disimpan ({@code ParameterTambahanKegiatanSiswaListener.validate(...)}).</li>
 * </ul>
 * <p>Karena wadah hasilnya {@code List} + {@code Collections.sort} (bukan {@code TreeSet}), bug
 * "penciutan senyap" akibat nomor urut kembar yang dikenal di keluarga
 * {@code KelompokParameterTambahan*} <b>tidak berlaku</b> di sini: baris bernomor urut sama tetap
 * tampil semuanya, hanya urutan relatifnya yang tidak dijamin.</p>
 *
 * <h3>Jalur pembaca yang diketahui</h3>
 * <ul>
 *   <li>{@link ais.action.master.sekolah.helper.ParameterTambahanKegiatanSiswaListener} &mdash;
 *   perakit formulir dinamis (termasuk sub-grid rekursif untuk parameter bertingkat),
 *   validator wajib-isi + wajib-lampiran, dan pemeriksa {@code check()} yang menentukan apakah blok
 *   parameter tambahan perlu dirender sama sekali.</li>
 *   <li>{@code ais.action.master.sekolah.KegiatanSiswaAction} &mdash; layar transaksi Kegiatan
 *   Siswa (memasang listener di atas dan memanggil validator sebelum simpan).</li>
 *   <li>{@code ais.action.master.dashboard.sekolah.DashboardRekapKegiatanSiswaData} &mdash; dasbor
 *   rekap: mendaftar seluruh kelompok aktif lalu seluruh parameter di bawahnya sebagai daftar
 *   <i>kolom</i> yang bisa dicentang untuk spreadsheet rekap.</li>
 *   <li>{@code ais.action.master.sekolah.ParameterTambahanKegiatanSiswaAction} &mdash; layar master
 *   pemetaan itu sendiri (CRUD baris tabel ini, plus ekspor/impor Excel).</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi rantai:</b> {@link #getParameterTambahan()} (definisi field),
 *   {@link #getKelompokKegiatanSiswa()} (kelompok kegiatan/heading).</li>
 *   <li><b>Perilaku formulir:</b> {@link #getWajibDiisi()} (override lokal atas sifat wajib milik
 *   definisi), {@link #getNomorUrut()} (kunci pengurutan, didenormalisasi dari definisi).</li>
 *   <li><b>Konstruktor:</b> {@link #ParameterTambahanKegiatanSiswa()}.</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} BUKAN {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia
 *   POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan satu pun property induknya</b>. Karena
 *   itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, dan
 *   {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar kolom-kolom itu
 *   benar-benar tersimpan. Field induk yang tidak dideklarasikan ulang &mdash; {@code nama},
 *   {@code nim}, {@code keterangan} &mdash; tetap ada di memori tetapi tidak pernah terisi dari
 *   DB.</li>
 *   <li><b>Kelas ini TIDAK memiliki field {@code keterangan} sendiri.</b> {@code getKeterangan()},
 *   {@code getNama()}, {@code getNim()}, {@code equals(...)}, {@code compareTo(...)}, dan
 *   {@code toString()} sepenuhnya diwarisi dari {@link GeneralValueObject}. (Pola
 *   "{@code getKeterangan()} membalik kontrak base class" karena itu <b>TIDAK ADA</b> di file
 *   ini.)</li>
 *   <li>Karena {@code nama}/{@code nim}/{@code keterangan} warisan selalu bernilai bawaan,
 *   {@code compareTo(...)} praktis <b>hanya</b> memakai cabang pertamanya ({@code nomorUrut}) &mdash;
 *   yang di kelas ini selalu non-null berkat guard {@code == null ? 1} pada
 *   {@link #getNomorUrut()}. Cabang {@code nim}/{@code nama}/{@code keterangan} adalah kode mati
 *   yang aman.</li>
 *   <li>Anotasi {@code @Id} berada pada <b>getter</b>, sehingga Hibernate memakai <i>property
 *   access</i> untuk seluruh property. Digabung dengan {@code dynamicUpdate = true}, getter yang
 *   menulis balik ke field ({@link #getNomorUrut()}, {@link #getWajibDiisi()}, dan {@code check(...)}
 *   pada kedua getter relasi) dapat mengotori state dan memicu {@code UPDATE} beserta revisi Envers
 *   baru pada baris yang <b>sekadar dibaca</b>.</li>
 *   <li>{@code nomorUrut} dan {@code wajibDiisi} tidak punya {@code @Column} eksplisit sehingga nama
 *   kolomnya mengikuti strategi penamaan bawaan Hibernate (nama property apa adanya, dilipat ke
 *   huruf kecil oleh PostgreSQL: {@code nomorurut}, {@code wajibdiisi}).</li>
 *   <li><b>Nama tabel dan kolom FK di kelas ini BERSIH</b> dari bug salin-tempel template modul SOP
 *   yang mencemari beberapa saudaranya: tabelnya {@code sekolah.parameter_tambahan_kegiatan_siswa}
 *   dan kolom FK kategorinya {@code kelompok_kegiatan_siswa} &mdash; keduanya konsisten dengan nama
 *   kelas dan domainnya, bukan sisa {@code ..._alur_sop}. Bandingkan
 *   {@code sekolah.ParameterTambahanCatatanSiswa} (tabel <i>dan</i> kolom FK keduanya salah) serta
 *   {@code sekolah.ParameterTambahanCatatanGuru}/{@code ...CatatanKelasSiswa} (kolom FK salah).</li>
 *   <li>{@code serialVersionUID} kelas ini ({@code 2463821577548439808L}) identik dengan
 *   {@link KelompokKegiatanSiswa} dan dengan hampir seluruh entity turunan template hbm2java yang
 *   sama &mdash; nilai itu <b>tidak</b> membedakan kelas, jangan dipakai sebagai penanda tipe.</li>
 *   <li><b>Kuirk parser "keterangan" (bug nyata, jalur AKTIF).</b> Saat formulir dibangun ulang,
 *   {@code ParameterTambahanKegiatanSiswaListener.displayRinci(...)} mengambil keterangan per-field
 *   dengan {@code value[value.length - 1]} atas baris {@code nilai_inds} yang dipecah
 *   {@code split("<=>")}. Karena {@code String.split} Java membuang ruas kosong di ekor, baris yang
 *   keterangannya kosong hanya menyisakan 3 ruas &mdash; sehingga yang terbaca sebagai "keterangan"
 *   justru <b>URL lampiran</b>, dan bila URL juga kosong, <b>nilai isian itu sendiri</b>. Ini
 *   kembaran pola yang sudah tercatat di varian {@code CatatanPegawai}/{@code CatatanGuru}, tetapi
 *   di sini <b>berdampak nyata</b> karena parser-nya benar-benar dipanggil (di varian tersebut
 *   parser-nya kode mati). Dicatat apa adanya; perbaikan di luar cakupan dokumentasi.</li>
 * </ul>
 *
 * @see ParameterTambahan
 * @see KelompokKegiatanSiswa
 * @see KegiatanSiswa
 * @see ais.action.master.sekolah.helper.ParameterTambahanKegiatanSiswaListener
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "parameter_tambahan_kegiatan_siswa")



public class ParameterTambahanKegiatanSiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama; {@code IDENTITY}, diisi database saat {@code INSERT}. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini; lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini; lihat {@link #setOlehId(String)}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Perhatikan:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi &mdash; nilai lama dipertahankan. Jadi jejak audit tidak bisa dikosongkan lewat
	 * setter ini, dan kegagalan penyetelan tidak menimbulkan error apa pun.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Perhatikan:</b> sama seperti {@link #setOlehId(String)}, setter ini <b>menolak
	 * diam-diam</b> nilai {@code null} maupun string kosong/spasi.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pemutakhiran stempel audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate
	 * menjalankan {@code UPDATE} atas baris ini.
	 *
	 * <p><b>Efek samping:</b> mengubah state entity yang sedang di-flush. Tidak dipanggil pada
	 * {@code INSERT} (tidak ada {@code @PrePersist} di kelas ini); nilai awal
	 * {@code tanggal_dirubah} berasal dari inisialisasi field di baris deklarasi.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis berimpit pada baris yang sama
	 * dengan method ini oleh generator; jangan dipisah tanpa alasan agar diff terhadap file
	 * saudaranya tetap mudah dibaca.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan; tidak pernah {@code null} untuk instance baru karena field-nya
	 *         diinisialisasi {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Definisi field kustom yang dipetakan; lapis pertama rantai. Lihat {@link #getParameterTambahan()}. */
	private ParameterTambahan parameterTambahan;
	/** Kelompok kegiatan tempat field ini muncul; lapis kedua rantai. Lihat {@link #getKelompokKegiatanSiswa()}. */
	private KelompokKegiatanSiswa kelompokKegiatanSiswa;
	/** Penanda field wajib diisi; lihat {@link #getWajibDiisi()} untuk aturan turunannya. */
	private Boolean wajibDiisi;

	/** Nomor urut tampil; salinan denormalisasi dari definisi. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil field ini pada formulir Kegiatan Siswa.
	 *
	 * <p><b>Bukan getter murni.</b> Method ini selalu <i>menimpa</i> field {@code nomorUrut} dengan
	 * nilai {@code getParameterTambahan().getNomorUrut()} setiap kali dipanggil, selama relasi
	 * definisi bisa diresolusi. Artinya kolom {@code nomorurut} milik tabel ini <b>tidak pernah
	 * otoritatif</b>: nilai apa pun yang tersimpan di sana akan tertimpa saat baris dibaca, dan
	 * sumber kebenaran pengurutan sesungguhnya adalah {@link ParameterTambahan#getNomorUrut()}.
	 * Nilai yang disetel lewat {@link #setNomorUrut(Integer)} hanya bertahan selama relasi definisi
	 * masih {@code null}.</p>
	 *
	 * <p><b>Efek samping.</b> (a) Memanggil {@link #getParameterTambahan()}, yang dapat memicu
	 * lazy-init/reload lewat {@code check(...)}. (b) Karena Hibernate memakai <i>property access</i>
	 * dan kelas ini {@code dynamicUpdate = true}, penimpaan field di sini dapat menandai baris
	 * sebagai kotor sehingga sekadar <b>membaca</b> baris memicu {@code UPDATE} plus revisi Envers
	 * baru. Efeknya bersifat <i>self-healing</i> (nilai yang ditulis selalu sama dengan sumbernya),
	 * jadi tidak merusak data, tetapi tetap menghasilkan riwayat audit palsu.</p>
	 *
	 * <p><b>Dipanggil dari mana.</b> Terutama secara <i>implisit</i> oleh
	 * {@code GeneralValueObject.compareTo(...)} &mdash; yakni setiap {@code Collections.sort(...)}
	 * atas daftar entity ini di
	 * {@code ParameterTambahanKegiatanSiswaListener.displayRinci(...)} (urutan field pada formulir)
	 * dan di {@code DashboardRekapKegiatanSiswaData} (urutan kolom rekap). Juga oleh Hibernate saat
	 * menulis baris.</p>
	 *
	 * @return nomor urut tampil; <b>tidak pernah {@code null}</b> &mdash; bernilai {@code 1} bila
	 *         baik field maupun definisi induk tidak memberi nilai
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil.
	 *
	 * <p>Perhatikan bahwa nilai ini akan tertimpa oleh {@link #getNomorUrut()} pada pembacaan
	 * berikutnya bila relasi {@link #getParameterTambahan()} bisa diresolusi.</p>
	 *
	 * @param nomorUrut nomor urut tampil; boleh {@code null}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh field dibiarkan pada nilai
	 * bawaannya kecuali {@code tanggal_dirubah}, yang diisi waktu sekarang saat objek dibuat.
	 */
	public ParameterTambahanKegiatanSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dihasilkan database
	 * ({@code IDENTITY}); menyetel id secara manual sebelum {@code INSERT} tidak akan berpengaruh.
	 * Id inilah yang dipakai {@code GeneralValueObject.equals(...)} untuk menentukan identitas.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum tersimpan
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
	 * Mengembalikan <b>definisi field kustom</b> yang dipetakan oleh baris ini (lapis pertama
	 * rantai): label, tipe inputan, daftar pilihan, hierarki {@code parent}, syarat tampil, wajib
	 * lampiran, dan nomor urut semuanya berasal dari sini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}) dan {@code LAZY}. Getter memanggil
	 * {@code GeneralValueObject.check(...)} yang meresolusi proxy lazy lewat rantai
	 * {@code EntityIdentityMap} &rarr; cache &rarr; session aktif &rarr; reload session baru;
	 * kegagalan bersifat <b>senyap</b> (objek dikembalikan apa adanya, tidak pernah melempar
	 * exception). Hasil resolusi ditulis balik ke field &mdash; dengan {@code dynamicUpdate = true}
	 * dan property access, ini dapat menandai baris kotor saat sekadar dibaca.</p>
	 *
	 * <p>{@code CascadeType.PERSIST}/{@code MERGE} berarti menyimpan baris pemetaan ini juga
	 * menyimpan/menggabungkan definisi field yang menempel padanya; tidak ada cascade
	 * {@code REMOVE}, jadi menghapus pemetaan tidak menghapus definisinya.</p>
	 *
	 * @return definisi field kustom; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menyetel definisi field kustom yang dipetakan baris ini.
	 *
	 * @param parameterTambahan definisi field kustom
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan <b>kelompok kegiatan</b> tempat field ini muncul (lapis kedua rantai) &mdash;
	 * sekaligus judul seksi pada formulir.
	 *
	 * <p>Kolom FK-nya bernama {@code kelompok_kegiatan_siswa}, konsisten dengan nama kelas dan
	 * domainnya; kelas ini <b>tidak</b> mewarisi nama kolom keliru {@code ..._alur_sop} dari template
	 * modul SOP seperti beberapa saudaranya.</p>
	 *
	 * <p>Relasi wajib ({@code nullable = false}) dan {@code LAZY}; sama seperti
	 * {@link #getParameterTambahan()}, getter ini memanggil {@code check(...)} dan menulis balik
	 * hasil resolusi ke field (berpotensi memicu {@code UPDATE}/revisi Envers pada pembacaan).</p>
	 *
	 * <p>Query perakit formulir menyaring baris ini dengan {@code kelompokKegiatanSiswa.aktif = true}
	 * berpasangan dengan {@code parameterTambahan.aktif = true}: menonaktifkan salah satu ujung
	 * rantai cukup untuk menyembunyikan field dari formulir tanpa menghapus baris pemetaan.</p>
	 *
	 * @return kelompok kegiatan pemilik seksi; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_siswa", nullable = false)
	public KelompokKegiatanSiswa getKelompokKegiatanSiswa() {
		kelompokKegiatanSiswa = check(kelompokKegiatanSiswa);
		return kelompokKegiatanSiswa;
	}

	/**
	 * Menyetel kelompok kegiatan tempat field ini muncul.
	 *
	 * @param kelompokKegiatanSiswa kelompok kegiatan pemilik seksi
	 */
	public void setKelompokKegiatanSiswa(KelompokKegiatanSiswa kelompokKegiatanSiswa) {
		this.kelompokKegiatanSiswa = kelompokKegiatanSiswa;
	}

	/**
	 * Mengembalikan apakah field ini <b>wajib diisi</b> pada formulir Kegiatan Siswa.
	 *
	 * <p>Ini adalah <i>override per-pemetaan</i>: nilai lokal (bila ada) menang atas sifat wajib
	 * milik definisi. Aturan resolusinya berlapis:</p>
	 * <ol>
	 *   <li>bila field lokal masih {@code null} dan relasi definisi bisa diresolusi, nilai diambil
	 *   dari {@link ParameterTambahan#getWajibDiisi()};</li>
	 *   <li>bila setelah itu masih {@code null} (definisi tidak ada, atau definisi sendiri belum
	 *   diisi), nilai dipaksa menjadi <b>{@code true}</b>.</li>
	 * </ol>
	 * <p>Bawaan {@code true} ini berarti sistem <i>fail-closed</i> untuk pengisian data: field yang
	 * konfigurasinya belum lengkap justru menjadi wajib, bukan opsional.</p>
	 *
	 * <p><b>Efek samping.</b> Berbeda dari {@link #getNomorUrut()} yang menimpa tanpa syarat, method
	 * ini hanya menulis saat field masih {@code null} &mdash; tetapi karena baris yang kolom
	 * {@code wajibdiisi}-nya masih {@code NULL} di database akan dikotori menjadi {@code true} pada
	 * pembacaan pertama, kombinasi property access + {@code dynamicUpdate = true} dapat menghasilkan
	 * {@code UPDATE} dan revisi Envers baru untuk baris yang sekadar dirender. Setelah sekali
	 * tersimpan, efek ini berhenti.</p>
	 *
	 * <p><b>Dipanggil dari mana.</b>
	 * {@code ParameterTambahanKegiatanSiswaListener.displayRinci(...)} untuk menempelkan penanda
	 * <code>" (*)"</code> pada label field;
	 * {@code ParameterTambahanKegiatanSiswaListener.validate(...)} sebagai syarat pertama validasi
	 * wajib-isi sebelum {@link KegiatanSiswa} boleh disimpan (digabung dengan syarat tipe inputan
	 * bukan {@code ParameterTambahan.TIDAK_ADA}, nilai masih kosong, dan parameter lolos
	 * <i>syarat tampil</i>); dan renderer baris pada layar master
	 * {@code ParameterTambahanKegiatanSiswaAction} sebagai state awal centang "Isian Wajib".</p>
	 *
	 * @return {@code true} bila field wajib diisi; <b>tidak pernah {@code null}</b>
	 */
	public Boolean getWajibDiisi() {
		if (wajibDiisi == null && getParameterTambahan() != null) {
			wajibDiisi = getParameterTambahan().getWajibDiisi();
		}
		if (wajibDiisi == null) {
			wajibDiisi = true;
		}
		return wajibDiisi;
	}

	/**
	 * Menyetel apakah field ini wajib diisi pada formulir Kegiatan Siswa.
	 *
	 * <p>Menyetel {@code null} secara efektif berarti "ikuti definisi induk", karena
	 * {@link #getWajibDiisi()} akan mengisinya ulang dari {@link ParameterTambahan} pada pembacaan
	 * berikutnya.</p>
	 *
	 * @param wajibDiisi {@code true}/{@code false} untuk override lokal, atau {@code null} untuk
	 *                   mengikuti definisi induk
	 */
	public void setWajibDiisi(Boolean wajibDiisi) {
		this.wajibDiisi = wajibDiisi;
	}

}
