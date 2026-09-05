package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.library.Perpustakaan;

/**
 * Entitas <b>Rencana dan Realisasi Output Kegiatan</b> pada modul RAB/perencanaan anggaran,
 * dipetakan ke tabel {@code rab.rencana_dan_realisasi_output_kegiatan}. Satu baris mencatat rencana
 * (target) dan capaian (realisasi) satu {@link OutputKegiatan} bagi satu {@link SatuanKerja},
 * dirinci per bulan sepanjang dua belas bulan.
 *
 * <h2>Tiga larik dua belas bulan yang berdiri sendiri</h2>
 * <p>Entitas menyimpan <b>tiga</b> deret paralel yang masing-masing terdiri dari dua belas kolom
 * terpisah — bukan koleksi, bukan tabel anak:</p>
 * <ul>
 *   <li>{@code targetBulan1}..{@code targetBulan12} — rencana capaian per bulan;</li>
 *   <li>{@code realisasiBulan1}..{@code realisasiBulan12} — capaian nyata per bulan;</li>
 *   <li>{@code prosentaseRealisasiBulan1}..{@code prosentaseRealisasiBulan12} — persentase capaian
 *   per bulan.</li>
 * </ul>
 * <p>Ditambah tiga kolom ringkasan tahunan ({@code target}, {@code realisasi}, {@code prosentase})
 * yang dihitung ulang setiap kali dibaca. Bentuk "dua belas kolom mendatar" ini adalah keputusan
 * desain lama yang membuat entitas terikat pada kalender dua belas bulan: tidak ada kolom tahun,
 * dan tidak ada cara menyimpan lebih dari satu tahun anggaran untuk pasangan
 * (satuan kerja, output) yang sama tanpa membuat baris kedua yang tak terbedakan dari baris
 * pertama.</p>
 *
 * <h2>PERINGATAN #1 — tidak ada penjaga keseimbangan rencana versus realisasi</h2>
 * <p>Pertanyaan yang wajar diajukan atas entitas bernama "rencana dan realisasi" adalah: apakah
 * realisasi dibatasi agar tidak melampaui rencana, atau apakah ada mekanisme persetujuan bila
 * dilampaui? Verifikasi atas kode menjawab <b>tidak, sama sekali tidak ada</b>. Ini murni pencatatan
 * bebas:</p>
 * <ul>
 *   <li>Tidak ada validasi di tingkat entitas. Seluruh setter menerima nilai apa pun, termasuk
 *   negatif dan termasuk realisasi yang jauh melebihi target.</li>
 *   <li>Tidak ada validasi di tingkat {@code ais.action.master.rab.RencanaDanRealisasiOutputKegiatanAction}.
 *   Ketiga puluh enam kotak isian dipasangi satu {@code EventListener} bersama pada peristiwa
 *   {@code onChange} yang langsung menyalin nilai kotak ke objek lalu memanggil
 *   {@code session.update(...)} — tanpa satu pun pemeriksaan sebelum penyimpanan.</li>
 *   <li>Tidak ada kolom status, tanda tangan, atau persetujuan pada entitas ini. Tidak ada
 *   {@code aktif}, {@code disetujui}, maupun rujukan ke pejabat penyetuju.</li>
 *   <li>Tidak ada penguncian periode. Bulan yang sudah lewat tetap dapat disunting kapan saja, dan
 *   bulan yang belum datang sudah dapat diisi realisasinya.</li>
 * </ul>
 * <p>Jejak perubahan sepenuhnya bergantung pada {@link Audited} (Hibernate Envers) dan pasangan
 * field {@code oleh}/{@code olehId}. Jadi bila kelak dibutuhkan penjaga keseimbangan atau alur
 * persetujuan, seluruhnya harus dibangun baru — jangan berasumsi ada penjaga tersembunyi di lapisan
 * lain.</p>
 *
 * <h2>PERINGATAN #2 — {@code prosentase} bukan turunan dari realisasi dibagi target</h2>
 * <p>Ini kesalahpahaman yang paling mudah terjadi. Kedua belas kolom
 * {@code prosentaseRealisasiBulanN} <b>tidak dihitung</b> dari
 * {@code realisasiBulanN / targetBulanN}. Ketiganya adalah kotak isian yang berdiri sendiri di
 * layar: operator mengetik target, realisasi, dan persentase secara terpisah, dan tidak ada kode
 * mana pun yang menurunkan yang satu dari yang lain. Akibatnya persentase yang tersimpan dapat
 * bertentangan dengan angka target dan realisasi pada baris yang sama tanpa terdeteksi.
 * {@link #getProsentase()} pun hanya merata-ratakan kedua belas persentase yang diketik itu, bukan
 * menghitung {@code getRealisasi() / getTarget()}. Uraian lengkapnya beserta akibat pembagi tetap
 * dua belas ada pada dokumentasi {@link #getProsentase()}.</p>
 *
 * <h2>PERINGATAN #3 — tiga getter ringkasan menulis ke basis data</h2>
 * <p>{@link #getTarget()}, {@link #getRealisasi()}, dan {@link #getProsentase()} bukan getter murni:
 * ketiganya menghitung ulang nilainya dari kolom bulanan lalu <b>menuliskan hasilnya kembali ke
 * field</b>. Karena entitas memakai <i>property access</i> (anotasi {@link Id} melekat pada
 * {@link #getId()}), Hibernate memanggil getter-getter ini saat <i>dirty checking</i>, sehingga
 * hasil perhitungan ulang benar-benar tertulis ke kolom {@code target}, {@code realisasi}, dan
 * {@code prosentase}. Ketiga kolom itu karenanya berperan sebagai <b>cache ternormalisasi-balik</b>
 * yang selalu disegarkan, bukan sebagai nilai yang dapat disimpan sendiri. Konsekuensi praktisnya:
 * {@link #setTarget(Double)}, {@link #setRealisasi(Double)}, dan {@link #setProsentase(Double)}
 * praktis tidak berpengaruh, dan kolom-kolom tersebut aman dipakai untuk pelaporan SQL langsung
 * <i>hanya</i> bila seluruh baris pernah dimuat aplikasi setelah perubahan terakhir pada kolom
 * bulanannya.</p>
 *
 * <h2>Hubungan dengan entitas lain</h2>
 * <p>Entitas ini memegang tiga relasi, seluruhnya {@code nullable}:</p>
 * <ul>
 *   <li>{@link #getSatuanKerja()} — satuan kerja pelaksana; penanda tenant, diisi otomatis untuk
 *   objek baru;</li>
 *   <li>{@link #getOutputKegiatan()} — keluaran yang direncanakan/direalisasikan. Katalog yang sama
 *   dirujuk {@link Tor#getOutputKegiatan()}, sehingga dokumen kerangka acuan dan pencatatan capaian
 *   bertemu pada output yang sama meskipun keduanya <b>tidak</b> saling merujuk. Tidak ada penjaga
 *   yang mencocokkan {@link Tor#getVolume()} dengan jumlah target bulanan di sini;</li>
 *   <li>{@link #getSatuan()} — satuan pengukuran. Tidak ada penjaga bahwa satuan ini sama dengan
 *   satuan yang dipakai TOR untuk output yang sama.</li>
 * </ul>
 * <p>Perhatikan tidak adanya relasi ke {@link Workspace}, {@link Proyek}, maupun
 * {@link RenstraProgram}: capaian output tidak tertaut ke pohon anggaran maupun ke dokumen rencana
 * strategis pada tingkat basis data.</p>
 *
 * <h2>Jebakan baris tak terlihat</h2>
 * <p>Kolom {@code output_kegiatan} dinyatakan {@code nullable = true}, tetapi
 * {@code RencanaDanRealisasiOutputKegiatanAction.initCriteria(...)} membangun kriterianya dengan
 * {@code createAlias("outputKegiatan", "outputKegiatan")} — sebuah <i>inner join</i>. Baris yang
 * tersimpan tanpa output karenanya <b>tidak akan pernah muncul</b> di daftar mana pun, sekaligus
 * tidak dapat disunting maupun dihapus lewat layar. Baris semacam itu tetap ada di basis data dan
 * tetap terhitung bila ada pelaporan SQL langsung. Bila menemui selisih antara angka laporan dan
 * angka layar, inilah tempat pertama yang perlu diperiksa.</p>
 *
 * <h2>Pembatasan tenant</h2>
 * <p>Kriteria pencarian memakai bentuk cakupan pohon: himpunan satuan kerja pengguna diambil lewat
 * {@code SekolahUtil.ambilSatuanKerjas()}, dan bila satker induk dipilih, seluruh anaknya
 * ditambahkan lewat {@code SatuanKerjaTreeModel.getChildsSet(...)}. Kriterianya kemudian
 * {@code Restrictions.in("satuanKerja", satuanKerjas)}, di-{@code OR} dengan
 * {@code isNull("satuanKerja")} ketika tidak ada induk dipilih. Namun bila himpunan cakupan
 * <b>kosong</b>, seluruh kriteria diganti {@code Restrictions.sqlRestriction("1=1")} — pengguna
 * melihat capaian seluruh satuan kerja. Perilaku <i>fail-open</i> ini pola berulang yang sudah
 * tercatat pada inisiatif dokumentasi ini, dan di sini menyangkut data kinerja antar satker.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah — hal yang cukup berarti mengingat ada tiga puluh sembilan kolom
 * numerik. Anotasi {@link Audited} membuat Hibernate Envers merekam setiap revisi ke tabel bayangan
 * pada skema {@code rab}; perlu diketahui bahwa penyegaran otomatis ketiga kolom ringkasan dapat
 * memicu revisi baru meski pengguna tidak menyunting apa pun. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}. Strategi fetch relasinya tidak seragam:
 * {@code satuanKerja} dan {@code outputKegiatan} memakai bawaan {@link ManyToOne} (eager) dengan
 * {@link FetchMode#SELECT}, sedangkan {@code satuan} dinyatakan {@link FetchType#LAZY} sehingga
 * getter-nya memanggil {@link GeneralValueObject#check(Object)}.</p>
 *
 * <h2>Catatan gaya kode</h2>
 * <p>Berkas asli menempelkan deklarasi field {@code tanggal_dirubah} di belakang method
 * {@link #onUpdate()}. Pada berkas ini deklarasi tersebut dipisah baris agar dapat diberi
 * dokumentasi, tanpa mengubah semantik apa pun.</p>
 *
 * @see OutputKegiatan
 * @see Tor
 * @see SatuanKerja
 * @see ais.database.dao.rab.RencanaDanRealisasiOutputKegiatanDao
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "rencana_dan_realisasi_output_kegiatan")

public class RencanaDanRealisasiOutputKegiatan extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada
	 * {@code rab.rencana_dan_realisasi_output_kegiatan.id}. Bernilai {@code null} selama objek belum
	 * pernah disimpan — kondisi ini juga dipakai {@link #getSatuanKerja()} sebagai penanda "objek
	 * masih baru".
	 */
	private Long id;

	/**
	 * Field audit bayangan: nama pengguna terakhir yang mengubah baris ini. Diisi lewat
	 * {@link #setOleh(String)} oleh lapisan interceptor/penyimpanan, bukan oleh pengguna. Pada
	 * entitas ini jejak audit berperan lebih penting dari biasanya, karena tidak ada mekanisme
	 * persetujuan yang membatasi siapa boleh mengubah angka capaian.
	 */
	private String oleh;

	/**
	 * Field audit bayangan: identitas (id pengguna) terakhir yang mengubah baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh lapisan interceptor/penyimpanan.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Setter ini <b>menolak diam-diam</b> nilai
	 * {@code null} maupun string kosong/spasi sehingga jejak audit yang sudah terisi tidak terhapus
	 * oleh proses penyalinan objek atau pengikatan form yang mengirim nilai kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum operasi {@code UPDATE}, mendelegasikan
	 * pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak boleh dipanggil
	 * langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Field audit bayangan: stempel waktu perubahan terakhir. Diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()} (bukan {@code new Date()}, agar mengikuti zona
	 * waktu/penyesuaian waktu aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya relevan pada skenario impor/migrasi data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai kolom {@code TIMESTAMP}.
	 *
	 * @return waktu penyimpanan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Satuan kerja pelaksana; penanda tenant, diisi otomatis untuk objek baru oleh {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/**
	 * Keluaran kegiatan yang direncanakan dan direalisasikan. Meskipun kolomnya {@code nullable},
	 * baris tanpa nilai ini tidak akan pernah tampil di layar — lihat "Jebakan baris tak terlihat"
	 * pada dokumentasi kelas.
	 */
	private OutputKegiatan outputKegiatan;

	/** Satuan pengukuran bagi seluruh angka target dan realisasi pada baris ini. */
	private Satuan satuan;

	/**
	 * Kolom ringkasan: jumlah kedua belas target bulanan. Dihitung ulang dan ditulis kembali setiap
	 * kali {@link #getTarget()} dipanggil — berperan sebagai cache, bukan nilai mandiri.
	 */
	private Double target = 0.0;
	/** Target capaian bulan ke-1 (Januari). */
	private Double targetBulan1 = 0.0;
	/** Target capaian bulan ke-2 (Februari). */
	private Double targetBulan2 = 0.0;
	/** Target capaian bulan ke-3 (Maret). */
	private Double targetBulan3 = 0.0;
	/** Target capaian bulan ke-4 (April). */
	private Double targetBulan4 = 0.0;
	/** Target capaian bulan ke-5 (Mei). */
	private Double targetBulan5 = 0.0;
	/** Target capaian bulan ke-6 (Juni). */
	private Double targetBulan6 = 0.0;
	/** Target capaian bulan ke-7 (Juli). */
	private Double targetBulan7 = 0.0;
	/** Target capaian bulan ke-8 (Agustus). */
	private Double targetBulan8 = 0.0;
	/** Target capaian bulan ke-9 (September). */
	private Double targetBulan9 = 0.0;
	/** Target capaian bulan ke-10 (Oktober). */
	private Double targetBulan10 = 0.0;
	/** Target capaian bulan ke-11 (November). */
	private Double targetBulan11 = 0.0;
	/** Target capaian bulan ke-12 (Desember). */
	private Double targetBulan12 = 0.0;

	/**
	 * Kolom ringkasan: jumlah kedua belas realisasi bulanan. Dihitung ulang dan ditulis kembali
	 * setiap kali {@link #getRealisasi()} dipanggil — berperan sebagai cache, bukan nilai mandiri.
	 */
	private Double realisasi = 0.0;
	/** Realisasi capaian bulan ke-1 (Januari). */
	private Double realisasiBulan1 = 0.0;
	/** Realisasi capaian bulan ke-2 (Februari). */
	private Double realisasiBulan2 = 0.0;
	/** Realisasi capaian bulan ke-3 (Maret). */
	private Double realisasiBulan3 = 0.0;
	/** Realisasi capaian bulan ke-4 (April). */
	private Double realisasiBulan4 = 0.0;
	/** Realisasi capaian bulan ke-5 (Mei). */
	private Double realisasiBulan5 = 0.0;
	/** Realisasi capaian bulan ke-6 (Juni). */
	private Double realisasiBulan6 = 0.0;
	/** Realisasi capaian bulan ke-7 (Juli). */
	private Double realisasiBulan7 = 0.0;
	/** Realisasi capaian bulan ke-8 (Agustus). */
	private Double realisasiBulan8 = 0.0;
	/** Realisasi capaian bulan ke-9 (September). */
	private Double realisasiBulan9 = 0.0;
	/** Realisasi capaian bulan ke-10 (Oktober). */
	private Double realisasiBulan10 = 0.0;
	/** Realisasi capaian bulan ke-11 (November). */
	private Double realisasiBulan11 = 0.0;
	/** Realisasi capaian bulan ke-12 (Desember). */
	private Double realisasiBulan12 = 0.0;

	/**
	 * Kolom ringkasan: rata-rata kedua belas persentase bulanan, selalu dibagi dua belas. Dihitung
	 * ulang dan ditulis kembali setiap kali {@link #getProsentase()} dipanggil.
	 */
	private Double prosentase = 0.0;
	/** Persentase capaian bulan ke-1, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan1 = 0.0;
	/** Persentase capaian bulan ke-2, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan2 = 0.0;
	/** Persentase capaian bulan ke-3, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan3 = 0.0;
	/** Persentase capaian bulan ke-4, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan4 = 0.0;
	/** Persentase capaian bulan ke-5, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan5 = 0.0;
	/** Persentase capaian bulan ke-6, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan6 = 0.0;
	/** Persentase capaian bulan ke-7, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan7 = 0.0;
	/** Persentase capaian bulan ke-8, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan8 = 0.0;
	/** Persentase capaian bulan ke-9, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan9 = 0.0;
	/** Persentase capaian bulan ke-10, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan10 = 0.0;
	/** Persentase capaian bulan ke-11, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan11 = 0.0;
	/** Persentase capaian bulan ke-12, diketik operator — bukan turunan target/realisasi. */
	private Double prosentaseRealisasiBulan12 = 0.0;

	/** Uraian kendala yang dihadapi dalam mencapai target; teks bebas tanpa validasi. */
	private String kendala;
	/** Uraian solusi atas kendala; teks bebas tanpa validasi. */
	private String solusi;
	/** Keterangan tambahan; teks bebas tanpa validasi. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai {@code RencanaDanRealisasiOutputKegiatanAction} saat menekan tombol tambah data.
	 * Objek hasil konstruktor ini punya {@code id} bernilai {@code null}, yang mengaktifkan
	 * pengisian otomatis satuan kerja pada {@link #getSatuanKerja()}, dan seluruh field numeriknya
	 * sudah berisi {@code 0.0}.
	 */
	public RencanaDanRealisasiOutputKegiatan() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Normalnya diisi Hibernate setelah {@code INSERT}. Perlu diingat bahwa
	 * mengubah nilai ini dari {@code null} menjadi bukan-{@code null} juga mematikan pengisian
	 * otomatis satuan kerja pada {@link #getSatuanKerja()}.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan satuan kerja pelaksana. Method ini <b>bukan getter murni</b>: untuk objek yang
	 * belum tersimpan, ia mengisi sendiri relasi tenant dari konteks pengguna yang sedang login.
	 *
	 * <h3>Kapan pengisian otomatis berjalan</h3>
	 * <p>Blok pengisian hanya dijalankan bila <b>kedua</b> syarat terpenuhi: field
	 * {@link #satuanKerja} masih {@code null} <b>dan</b> {@link #id} juga {@code null}. Syarat kedua
	 * itulah penjaganya — ia memastikan baris yang sudah ada di basis data dengan kolom
	 * {@code satuan_kerja} bernilai NULL tidak diam-diam dipindahkan menjadi milik satuan kerja
	 * pengguna yang kebetulan membacanya. Penjaga ini penting justru karena entitas memakai
	 * <i>property access</i>, sehingga Hibernate memanggil getter ini saat <i>dirty checking</i> dan
	 * akan menuliskan nilai barunya pada {@code flush} berikutnya. Bandingkan dengan
	 * {@link RenstraProgram#getSatuanKerja()} di paket yang sama, yang justru tidak memasang penjaga
	 * ini.</p>
	 *
	 * <h3>Urutan sumber nilai</h3>
	 * <ol>
	 *   <li>{@code Common.getCurrentUser().ambilSatuanKerja()} — satuan kerja yang melekat pada
	 *   pengguna aktif; jalur utama.</li>
	 *   <li>Bila jalur pertama menghasilkan {@code null}, dicoba
	 *   {@code Common.getCurrentPerpustakaan().getSatuanKerja()} — satuan kerja milik konteks
	 *   perpustakaan aktif, jalur cadangan bagi sesi yang berjalan dalam konteks modul
	 *   perpustakaan.</li>
	 * </ol>
	 *
	 * <h3>Penanganan galat bersifat fail-open</h3>
	 * <p>Seluruh blok dibungkus {@code try/catch(Exception)} yang tidak melemparkan ulang, melainkan
	 * hanya mencatat kejadian lewat {@code ais.common.ErrorAuditUtil.record(...)}. Ini disengaja:
	 * getter entitas dipanggil dari banyak konteks tanpa sesi pengguna (proses batch, penjadwal,
	 * uji, rekonstruksi revisi oleh Envers) di mana {@code Common.getCurrentUser()} wajar gagal.
	 * Konsekuensinya, pada konteks tanpa pengguna baris tersimpan dengan {@code satuan_kerja}
	 * bernilai NULL. Pada entitas ini akibatnya khas: baris ber-NULL tetap <b>terlihat</b> di layar
	 * ketika pengguna tidak memilih satker induk (kriteria menggabungkan {@code isNull("satuanKerja")}
	 * dengan {@code OR}), tetapi menjadi <b>tak terlihat</b> begitu satker induk dipilih. Angka
	 * capaian yang muncul dan hilang tergantung isian filter hampir selalu berasal dari baris
	 * semacam ini.</p>
	 *
	 * <h3>Tidak ada resolusi proxy</h3>
	 * <p>Berbeda dari {@link #getSatuan()} pada entitas yang sama, method ini tidak memanggil
	 * {@link GeneralValueObject#check(Object)}. Itu konsisten dengan pemetaannya: relasi ini tidak
	 * dinyatakan {@code LAZY} sehingga mengikuti bawaan {@link ManyToOne} (eager) dan nilainya sudah
	 * berupa objek nyata. Jangan menambahkan {@code check(...)} tanpa sekaligus mengubah strategi
	 * fetch-nya.</p>
	 *
	 * @return satuan kerja pelaksana, atau {@code null} bila tidak dapat ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (this.satuanKerja == null && this.id == null) {
			try {
				SatuanKerja satuanKerja = Common.getCurrentUser().ambilSatuanKerja();
				Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
				if (satuanKerja == null && currentPerpustakaan != null) {
					satuanKerja = currentPerpustakaan.getSatuanKerja();
				}
				this.satuanKerja = satuanKerja;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/RencanaDanRealisasiOutputKegiatan.java:154");
			}
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pelaksana secara eksplisit. Pengisian manual ini mengalahkan pengisian
	 * otomatis pada {@link #getSatuanKerja()}, karena getter hanya bertindak ketika field masih
	 * {@code null}. Dipanggil {@code RencanaDanRealisasiOutputKegiatanAction} saat menyimpan, setelah
	 * memastikan pengguna memilih satuan kerja pada form. Tidak ada validasi bahwa satuan kerja yang
	 * diberikan berada dalam cakupan wewenang pengguna.
	 *
	 * @param satuanKerja satuan kerja pelaksana; boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan keluaran kegiatan yang dicatat pada baris ini. Getter murni tanpa efek samping:
	 * relasi dipetakan eager ({@link ManyToOne} tanpa {@code fetch = LAZY}) dengan
	 * {@link FetchMode#SELECT}, sehingga nilainya sudah berupa objek nyata dan tidak perlu dipaksa
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Nilainya dapat {@code null} secara skema, tetapi baris ber-{@code null} tidak akan pernah
	 * tampil di layar karena kriteria pencarian memakai <i>inner join</i> lewat
	 * {@code createAlias("outputKegiatan", "outputKegiatan")}. Row renderer juga menampilkan
	 * {@code getOutputKegiatan().getKode()} sebagai satu-satunya penanda identitas baris — sehingga
	 * dua output berkode sama (yang tidak dicegah, karena {@link OutputKegiatan} tidak menjamin
	 * keunikan kode) menghasilkan baris yang tampak tidak terbedakan.</p>
	 *
	 * @return keluaran kegiatan terkait, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "output_kegiatan", nullable = true)
	public OutputKegiatan getOutputKegiatan() {
		return outputKegiatan;
	}

	/**
	 * Menyetel keluaran kegiatan yang dicatat pada baris ini. Tidak ada penjaga yang mencegah dua
	 * baris memiliki pasangan (satuan kerja, output) yang sama — duplikasi semacam itu akan tampil
	 * sebagai dua baris identik di layar dan menggandakan angka pada pelaporan agregat.
	 *
	 * @param outputKegiatan entri katalog output kegiatan; boleh {@code null}, tetapi menyetelnya
	 *                       {@code null} membuat baris hilang dari seluruh layar.
	 */
	public void setOutputKegiatan(OutputKegiatan outputKegiatan) {
		this.outputKegiatan = outputKegiatan;
	}

	/**
	 * Mengembalikan satuan pengukuran bagi seluruh angka pada baris ini. Method ini <b>bukan getter
	 * murni</b>: ia memanggil {@link GeneralValueObject#check(Object)} yang memaksa proxy Hibernate
	 * yang masih malas menjadi objek nyata, lalu menuliskan hasilnya kembali ke field. Langkah itu
	 * diperlukan karena relasi dinyatakan {@link FetchType#LAZY} — berbeda dari
	 * {@link #getSatuanKerja()} dan {@link #getOutputKegiatan()} pada entitas yang sama yang
	 * dipetakan eager.
	 *
	 * @return satuan pengukuran, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan", nullable = true)
	public Satuan getSatuan() {
		satuan = check(satuan);
		return satuan;
	}

	/**
	 * Menyetel satuan pengukuran bagi seluruh angka pada baris ini. Perlu diperhatikan bahwa satuan
	 * berlaku untuk <b>seluruh</b> kolom target dan realisasi sekaligus; tidak ada cara mencatat
	 * bulan yang memakai satuan berbeda. Tidak ada pula penjaga bahwa satuan ini sama dengan
	 * {@link Tor#getSatuan()} untuk output yang sama.
	 *
	 * @param satuan master satuan; boleh {@code null}.
	 */
	public void setSatuan(Satuan satuan) {
		this.satuan = satuan;
	}

	/**
	 * Mengembalikan target capaian bulan ke-1, menormalkan {@code null} menjadi {@code 0.0}. Method
	 * ini <b>bukan getter murni</b>: hasil normalisasi dituliskan kembali ke field.
	 *
	 * <h3>Pola bersama tiga puluh enam getter bulanan</h3>
	 * <p>Bentuk yang sama dipakai oleh seluruh getter {@code getTargetBulanN},
	 * {@code getRealisasiBulanN}, dan {@code getProsentaseRealisasiBulanN} untuk N = 1..12. Uraian
	 * di sini berlaku bagi ketiga puluh enamnya; dokumentasi masing-masing merujuk balik ke sini
	 * agar tidak berulang.</p>
	 *
	 * <h3>Mengapa efek samping ini berarti</h3>
	 * <p>Entitas memakai <i>property access</i> (anotasi {@link Id} melekat pada {@link #getId()}),
	 * sehingga Hibernate membaca nilai properti dengan memanggil getter ini, termasuk saat
	 * melakukan <i>dirty checking</i> menjelang {@code flush}. Normalisasi {@code null} menjadi
	 * {@code 0.0} karenanya <b>benar-benar tertulis</b> ke basis data dan mencatat satu revisi baru
	 * di tabel Envers. Karena row renderer layar membaca ketiga puluh enam getter ini untuk mengisi
	 * kotak-kotak isian setiap baris, sekadar membuka daftar rencana-realisasi sudah cukup untuk
	 * menormalkan seluruh kolom NULL pada seluruh baris yang tampil di halaman itu.</p>
	 *
	 * <h3>Implikasi bagi pelaporan</h3>
	 * <p>Setelah pembacaan pertama, "bulan ini belum direncanakan/belum dilaporkan" (NULL) tidak
	 * lagi dapat dibedakan dari "bulan ini memang nol" (0). Bagi baris yang diisi bertahap sepanjang
	 * tahun — pola pemakaian yang wajar — perbedaan ini bermakna: laporan yang menghitung "berapa
	 * bulan sudah dilaporkan" akan menghitung berlebih. Bila kelengkapan laporan bulanan perlu
	 * dilacak, sediakan penanda terpisah alih-alih mengandalkan NULL. Untuk memeriksa keadaan asli
	 * data, lakukan kueri SQL langsung sebelum baris pernah dimuat aplikasi, atau telusuri revisi
	 * paling awal pada tabel Envers.</p>
	 *
	 * <h3>Catatan pembulatan</h3>
	 * <p>Nilai bertipe {@link Double}, bukan {@code BigDecimal}. Penjumlahan kedua belas bulan pada
	 * {@link #getTarget()} dan {@link #getRealisasi()} karenanya dapat memunculkan galat pembulatan
	 * biner. Untuk angka capaian yang bersifat cacah hal ini jarang terasa, tetapi jangan
	 * memperlakukan hasilnya sebagai angka yang mengikat tanpa pembulatan eksplisit.</p>
	 *
	 * @return target bulan ke-1; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan1() {
		if (targetBulan1 == null) {
			targetBulan1 = 0.0;
		}
		return targetBulan1;
	}

	/**
	 * Menyetel target capaian bulan ke-1. Menerima nilai apa pun, termasuk negatif; tidak ada
	 * validasi maupun pembatasan terhadap realisasi bulan yang sama.
	 *
	 * @param targetBulan1 target bulan ke-1.
	 */
	public void setTargetBulan1(Double targetBulan1) {
		this.targetBulan1 = targetBulan1;
	}

	/**
	 * Mengembalikan target capaian bulan ke-2, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-2; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan2() {

		if (targetBulan2 == null) {
			targetBulan2 = 0.0;
		}
		return targetBulan2;
	}

	/**
	 * Menyetel target capaian bulan ke-2. Tanpa validasi.
	 *
	 * @param targetBulan2 target bulan ke-2.
	 */
	public void setTargetBulan2(Double targetBulan2) {
		this.targetBulan2 = targetBulan2;
	}

	/**
	 * Mengembalikan target capaian bulan ke-3, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-3; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan3() {
		if (targetBulan3 == null) {
			targetBulan3 = 0.0;
		}
		return targetBulan3;
	}

	/**
	 * Menyetel target capaian bulan ke-3. Tanpa validasi.
	 *
	 * @param targetBulan3 target bulan ke-3.
	 */
	public void setTargetBulan3(Double targetBulan3) {
		this.targetBulan3 = targetBulan3;
	}

	/**
	 * Mengembalikan target capaian bulan ke-4, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-4; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan4() {
		if (targetBulan4 == null) {
			targetBulan4 = 0.0;
		}
		return targetBulan4;
	}

	/**
	 * Menyetel target capaian bulan ke-4. Tanpa validasi.
	 *
	 * @param targetBulan4 target bulan ke-4.
	 */
	public void setTargetBulan4(Double targetBulan4) {
		this.targetBulan4 = targetBulan4;
	}

	/**
	 * Mengembalikan target capaian bulan ke-5, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-5; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan5() {
		if (targetBulan5 == null) {
			targetBulan5 = 0.0;
		}
		return targetBulan5;
	}

	/**
	 * Menyetel target capaian bulan ke-5. Tanpa validasi.
	 *
	 * @param targetBulan5 target bulan ke-5.
	 */
	public void setTargetBulan5(Double targetBulan5) {
		this.targetBulan5 = targetBulan5;
	}

	/**
	 * Mengembalikan target capaian bulan ke-6, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-6; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan6() {
		if (targetBulan6 == null) {
			targetBulan6 = 0.0;
		}
		return targetBulan6;
	}

	/**
	 * Menyetel target capaian bulan ke-6. Tanpa validasi.
	 *
	 * @param targetBulan6 target bulan ke-6.
	 */
	public void setTargetBulan6(Double targetBulan6) {
		this.targetBulan6 = targetBulan6;
	}

	/**
	 * Mengembalikan target capaian bulan ke-7, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-7; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan7() {
		if (targetBulan7 == null) {
			targetBulan7 = 0.0;
		}
		return targetBulan7;
	}

	/**
	 * Menyetel target capaian bulan ke-7. Tanpa validasi.
	 *
	 * @param targetBulan7 target bulan ke-7.
	 */
	public void setTargetBulan7(Double targetBulan7) {
		this.targetBulan7 = targetBulan7;
	}

	/**
	 * Mengembalikan target capaian bulan ke-8, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-8; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan8() {
		if (targetBulan8 == null) {
			targetBulan8 = 0.0;
		}
		return targetBulan8;
	}

	/**
	 * Menyetel target capaian bulan ke-8. Tanpa validasi.
	 *
	 * @param targetBulan8 target bulan ke-8.
	 */
	public void setTargetBulan8(Double targetBulan8) {
		this.targetBulan8 = targetBulan8;
	}

	/**
	 * Mengembalikan target capaian bulan ke-9, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-9; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan9() {
		if (targetBulan9 == null) {
			targetBulan9 = 0.0;
		}
		return targetBulan9;
	}

	/**
	 * Menyetel target capaian bulan ke-9. Tanpa validasi.
	 *
	 * @param targetBulan9 target bulan ke-9.
	 */
	public void setTargetBulan9(Double targetBulan9) {
		this.targetBulan9 = targetBulan9;
	}

	/**
	 * Mengembalikan target capaian bulan ke-10, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-10; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan10() {
		if (targetBulan10 == null) {
			targetBulan10 = 0.0;
		}
		return targetBulan10;
	}

	/**
	 * Menyetel target capaian bulan ke-10. Tanpa validasi.
	 *
	 * @param targetBulan10 target bulan ke-10.
	 */
	public void setTargetBulan10(Double targetBulan10) {
		this.targetBulan10 = targetBulan10;
	}

	/**
	 * Mengembalikan target capaian bulan ke-11, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-11; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan11() {
		if (targetBulan11 == null) {
			targetBulan11 = 0.0;
		}
		return targetBulan11;
	}

	/**
	 * Menyetel target capaian bulan ke-11. Tanpa validasi.
	 *
	 * @param targetBulan11 target bulan ke-11.
	 */
	public void setTargetBulan11(Double targetBulan11) {
		this.targetBulan11 = targetBulan11;
	}

	/**
	 * Mengembalikan target capaian bulan ke-12, menormalkan {@code null} menjadi {@code 0.0} sambil
	 * menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap.
	 *
	 * @return target bulan ke-12; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTargetBulan12() {
		if (targetBulan12 == null) {
			targetBulan12 = 0.0;
		}
		return targetBulan12;
	}

	/**
	 * Menyetel target capaian bulan ke-12. Tanpa validasi.
	 *
	 * @param targetBulan12 target bulan ke-12.
	 */
	public void setTargetBulan12(Double targetBulan12) {
		this.targetBulan12 = targetBulan12;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-1, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap
	 * efek samping dan implikasinya.
	 *
	 * <p>Perlu ditegaskan bahwa nilai ini <b>tidak dibatasi</b> oleh {@link #getTargetBulan1()}:
	 * realisasi boleh melampaui target sebesar apa pun tanpa peringatan maupun persetujuan. Lihat
	 * "PERINGATAN #1" pada dokumentasi kelas.</p>
	 *
	 * @return realisasi bulan ke-1; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan1() {
		if (realisasiBulan1 == null) {
			realisasiBulan1 = 0.0;
		}
		return realisasiBulan1;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-1. Menerima nilai apa pun, termasuk nilai yang melampaui
	 * target bulan yang sama dan nilai negatif; tidak ada validasi.
	 *
	 * @param realisasiBulan1 realisasi bulan ke-1.
	 */
	public void setRealisasiBulan1(Double realisasiBulan1) {
		this.realisasiBulan1 = realisasiBulan1;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-2, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-2; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan2() {
		if (realisasiBulan2 == null) {
			realisasiBulan2 = 0.0;
		}
		return realisasiBulan2;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-2. Tanpa validasi.
	 *
	 * @param realisasiBulan2 realisasi bulan ke-2.
	 */
	public void setRealisasiBulan2(Double realisasiBulan2) {
		this.realisasiBulan2 = realisasiBulan2;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-3, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-3; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan3() {
		if (realisasiBulan3 == null) {
			realisasiBulan3 = 0.0;
		}
		return realisasiBulan3;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-3. Tanpa validasi.
	 *
	 * @param realisasiBulan3 realisasi bulan ke-3.
	 */
	public void setRealisasiBulan3(Double realisasiBulan3) {
		this.realisasiBulan3 = realisasiBulan3;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-4, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-4; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan4() {
		if (realisasiBulan4 == null) {
			realisasiBulan4 = 0.0;
		}
		return realisasiBulan4;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-4. Tanpa validasi.
	 *
	 * @param realisasiBulan4 realisasi bulan ke-4.
	 */
	public void setRealisasiBulan4(Double realisasiBulan4) {
		this.realisasiBulan4 = realisasiBulan4;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-5, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-5; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan5() {
		if (realisasiBulan5 == null) {
			realisasiBulan5 = 0.0;
		}
		return realisasiBulan5;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-5. Tanpa validasi.
	 *
	 * @param realisasiBulan5 realisasi bulan ke-5.
	 */
	public void setRealisasiBulan5(Double realisasiBulan5) {
		this.realisasiBulan5 = realisasiBulan5;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-6, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-6; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan6() {
		if (realisasiBulan6 == null) {
			realisasiBulan6 = 0.0;
		}
		return realisasiBulan6;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-6. Tanpa validasi.
	 *
	 * @param realisasiBulan6 realisasi bulan ke-6.
	 */
	public void setRealisasiBulan6(Double realisasiBulan6) {
		this.realisasiBulan6 = realisasiBulan6;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-7, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-7; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan7() {
		if (realisasiBulan7 == null) {
			realisasiBulan7 = 0.0;
		}
		return realisasiBulan7;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-7. Tanpa validasi.
	 *
	 * @param realisasiBulan7 realisasi bulan ke-7.
	 */
	public void setRealisasiBulan7(Double realisasiBulan7) {
		this.realisasiBulan7 = realisasiBulan7;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-8, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-8; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan8() {
		if (realisasiBulan8 == null) {
			realisasiBulan8 = 0.0;
		}
		return realisasiBulan8;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-8. Tanpa validasi.
	 *
	 * @param realisasiBulan8 realisasi bulan ke-8.
	 */
	public void setRealisasiBulan8(Double realisasiBulan8) {
		this.realisasiBulan8 = realisasiBulan8;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-9, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-9; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan9() {
		if (realisasiBulan9 == null) {
			realisasiBulan9 = 0.0;
		}
		return realisasiBulan9;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-9. Tanpa validasi.
	 *
	 * @param realisasiBulan9 realisasi bulan ke-9.
	 */
	public void setRealisasiBulan9(Double realisasiBulan9) {
		this.realisasiBulan9 = realisasiBulan9;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-10, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-10; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan10() {
		if (realisasiBulan10 == null) {
			realisasiBulan10 = 0.0;
		}
		return realisasiBulan10;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-10. Tanpa validasi.
	 *
	 * @param realisasiBulan10 realisasi bulan ke-10.
	 */
	public void setRealisasiBulan10(Double realisasiBulan10) {
		this.realisasiBulan10 = realisasiBulan10;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-11, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-11; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan11() {
		if (realisasiBulan11 == null) {
			realisasiBulan11 = 0.0;
		}
		return realisasiBulan11;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-11. Tanpa validasi.
	 *
	 * @param realisasiBulan11 realisasi bulan ke-11.
	 */
	public void setRealisasiBulan11(Double realisasiBulan11) {
		this.realisasiBulan11 = realisasiBulan11;
	}

	/**
	 * Mengembalikan realisasi capaian bulan ke-12, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} dan
	 * {@link #getRealisasiBulan1()} untuk uraian lengkap.
	 *
	 * @return realisasi bulan ke-12; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getRealisasiBulan12() {
		if (realisasiBulan12 == null) {
			realisasiBulan12 = 0.0;
		}
		return realisasiBulan12;
	}

	/**
	 * Menyetel realisasi capaian bulan ke-12. Tanpa validasi.
	 *
	 * @param realisasiBulan12 realisasi bulan ke-12.
	 */
	public void setRealisasiBulan12(Double realisasiBulan12) {
		this.realisasiBulan12 = realisasiBulan12;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-1, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getTargetBulan1()} untuk uraian lengkap
	 * efek samping dan implikasinya.
	 *
	 * <p><b>Ini nilai yang diketik operator, bukan hasil perhitungan.</b> Tidak ada kode mana pun
	 * yang menurunkannya dari {@code getRealisasiBulan1() / getTargetBulan1()}. Ketiganya adalah
	 * kotak isian terpisah pada layar, dan persentase yang tersimpan dapat sepenuhnya bertentangan
	 * dengan angka target dan realisasi pada bulan yang sama tanpa terdeteksi. Satuannya pun tidak
	 * ditegakkan: tidak ada yang memastikan nilai berada pada rentang 0–100 (atau 0–1), sehingga
	 * penafsirannya bergantung pada kebiasaan operator.</p>
	 *
	 * @return persentase capaian bulan ke-1; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan1() {
		if (prosentaseRealisasiBulan1 == null) {
			prosentaseRealisasiBulan1 = 0.0;
		}
		return prosentaseRealisasiBulan1;
	}

	/**
	 * Menyetel persentase capaian bulan ke-1. Tanpa validasi rentang maupun pencocokan terhadap
	 * target dan realisasi bulan yang sama.
	 *
	 * @param prosentaseRealisasiBulan1 persentase capaian bulan ke-1.
	 */
	public void setProsentaseRealisasiBulan1(Double prosentaseRealisasiBulan1) {
		this.prosentaseRealisasiBulan1 = prosentaseRealisasiBulan1;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-2, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-2; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan2() {
		if (prosentaseRealisasiBulan2 == null) {
			prosentaseRealisasiBulan2 = 0.0;
		}
		return prosentaseRealisasiBulan2;
	}

	/**
	 * Menyetel persentase capaian bulan ke-2. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan2 persentase capaian bulan ke-2.
	 */
	public void setProsentaseRealisasiBulan2(Double prosentaseRealisasiBulan2) {
		this.prosentaseRealisasiBulan2 = prosentaseRealisasiBulan2;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-3, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-3; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan3() {
		if (prosentaseRealisasiBulan3 == null) {
			prosentaseRealisasiBulan3 = 0.0;
		}
		return prosentaseRealisasiBulan3;
	}

	/**
	 * Menyetel persentase capaian bulan ke-3. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan3 persentase capaian bulan ke-3.
	 */
	public void setProsentaseRealisasiBulan3(Double prosentaseRealisasiBulan3) {
		this.prosentaseRealisasiBulan3 = prosentaseRealisasiBulan3;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-4, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-4; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan4() {
		if (prosentaseRealisasiBulan4 == null) {
			prosentaseRealisasiBulan4 = 0.0;
		}
		return prosentaseRealisasiBulan4;
	}

	/**
	 * Menyetel persentase capaian bulan ke-4. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan4 persentase capaian bulan ke-4.
	 */
	public void setProsentaseRealisasiBulan4(Double prosentaseRealisasiBulan4) {
		this.prosentaseRealisasiBulan4 = prosentaseRealisasiBulan4;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-5, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-5; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan5() {
		if (prosentaseRealisasiBulan5 == null) {
			prosentaseRealisasiBulan5 = 0.0;
		}
		return prosentaseRealisasiBulan5;
	}

	/**
	 * Menyetel persentase capaian bulan ke-5. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan5 persentase capaian bulan ke-5.
	 */
	public void setProsentaseRealisasiBulan5(Double prosentaseRealisasiBulan5) {
		this.prosentaseRealisasiBulan5 = prosentaseRealisasiBulan5;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-6, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-6; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan6() {
		if (prosentaseRealisasiBulan6 == null) {
			prosentaseRealisasiBulan6 = 0.0;
		}
		return prosentaseRealisasiBulan6;
	}

	/**
	 * Menyetel persentase capaian bulan ke-6. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan6 persentase capaian bulan ke-6.
	 */
	public void setProsentaseRealisasiBulan6(Double prosentaseRealisasiBulan6) {
		this.prosentaseRealisasiBulan6 = prosentaseRealisasiBulan6;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-7, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-7; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan7() {
		if (prosentaseRealisasiBulan7 == null) {
			prosentaseRealisasiBulan7 = 0.0;
		}
		return prosentaseRealisasiBulan7;
	}

	/**
	 * Menyetel persentase capaian bulan ke-7. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan7 persentase capaian bulan ke-7.
	 */
	public void setProsentaseRealisasiBulan7(Double prosentaseRealisasiBulan7) {
		this.prosentaseRealisasiBulan7 = prosentaseRealisasiBulan7;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-8, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-8; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan8() {
		if (prosentaseRealisasiBulan8 == null) {
			prosentaseRealisasiBulan8 = 0.0;
		}
		return prosentaseRealisasiBulan8;
	}

	/**
	 * Menyetel persentase capaian bulan ke-8. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan8 persentase capaian bulan ke-8.
	 */
	public void setProsentaseRealisasiBulan8(Double prosentaseRealisasiBulan8) {
		this.prosentaseRealisasiBulan8 = prosentaseRealisasiBulan8;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-9, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-9; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan9() {
		if (prosentaseRealisasiBulan9 == null) {
			prosentaseRealisasiBulan9 = 0.0;
		}
		return prosentaseRealisasiBulan9;
	}

	/**
	 * Menyetel persentase capaian bulan ke-9. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan9 persentase capaian bulan ke-9.
	 */
	public void setProsentaseRealisasiBulan9(Double prosentaseRealisasiBulan9) {
		this.prosentaseRealisasiBulan9 = prosentaseRealisasiBulan9;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-10, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-10; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan10() {
		if (prosentaseRealisasiBulan10 == null) {
			prosentaseRealisasiBulan10 = 0.0;
		}
		return prosentaseRealisasiBulan10;
	}

	/**
	 * Menyetel persentase capaian bulan ke-10. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan10 persentase capaian bulan ke-10.
	 */
	public void setProsentaseRealisasiBulan10(Double prosentaseRealisasiBulan10) {
		this.prosentaseRealisasiBulan10 = prosentaseRealisasiBulan10;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-11, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-11; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan11() {
		if (prosentaseRealisasiBulan11 == null) {
			prosentaseRealisasiBulan11 = 0.0;
		}
		return prosentaseRealisasiBulan11;
	}

	/**
	 * Menyetel persentase capaian bulan ke-11. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan11 persentase capaian bulan ke-11.
	 */
	public void setProsentaseRealisasiBulan11(Double prosentaseRealisasiBulan11) {
		this.prosentaseRealisasiBulan11 = prosentaseRealisasiBulan11;
	}

	/**
	 * Mengembalikan persentase capaian bulan ke-12, menormalkan {@code null} menjadi {@code 0.0}
	 * sambil menuliskannya kembali ke field. Lihat {@link #getProsentaseRealisasiBulan1()} untuk
	 * uraian lengkap.
	 *
	 * @return persentase capaian bulan ke-12; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getProsentaseRealisasiBulan12() {
		if (prosentaseRealisasiBulan12 == null) {
			prosentaseRealisasiBulan12 = 0.0;
		}
		return prosentaseRealisasiBulan12;
	}

	/**
	 * Menyetel persentase capaian bulan ke-12. Tanpa validasi.
	 *
	 * @param prosentaseRealisasiBulan12 persentase capaian bulan ke-12.
	 */
	public void setProsentaseRealisasiBulan12(Double prosentaseRealisasiBulan12) {
		this.prosentaseRealisasiBulan12 = prosentaseRealisasiBulan12;
	}

	/**
	 * Mengembalikan uraian kendala yang dihadapi dalam mencapai target, apa adanya tanpa pemangkasan
	 * spasi maupun normalisasi {@code null}. Disunting langsung di grid oleh layar; nilainya
	 * tersimpan seketika pada setiap {@code onChange}.
	 *
	 * @return teks kendala, atau {@code null} bila belum diisi.
	 */
	public String getKendala() {
		return kendala;
	}

	/**
	 * Menyetel uraian kendala. Teks bebas tanpa validasi; tidak diwajibkan meski realisasi jauh di
	 * bawah target.
	 *
	 * @param kendala teks kendala.
	 */
	public void setKendala(String kendala) {
		this.kendala = kendala;
	}

	/**
	 * Mengembalikan uraian solusi atas kendala, apa adanya tanpa pemangkasan spasi maupun
	 * normalisasi {@code null}.
	 *
	 * @return teks solusi, atau {@code null} bila belum diisi.
	 */
	public String getSolusi() {
		return solusi;
	}

	/**
	 * Menyetel uraian solusi atas kendala. Teks bebas tanpa validasi; tidak ada penjaga yang
	 * mewajibkan solusi diisi ketika {@link #getKendala()} terisi.
	 *
	 * @param solusi teks solusi.
	 */
	public void setSolusi(String solusi) {
		this.solusi = solusi;
	}

	/**
	 * Mengembalikan keterangan tambahan, apa adanya tanpa pemangkasan spasi maupun normalisasi
	 * {@code null}.
	 *
	 * @return teks keterangan, atau {@code null} bila belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan tambahan. Teks bebas tanpa validasi.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan total target setahun, yaitu jumlah kedua belas target bulanan. Method ini
	 * <b>bukan getter murni</b> dan perilakunya perlu dipahami sebelum kolom {@code target} dipakai
	 * dalam pelaporan.
	 *
	 * <h3>Apa yang sebenarnya terjadi</h3>
	 * <p>Setiap pemanggilan menghitung ulang jumlah dari {@link #getTargetBulan1()} sampai
	 * {@link #getTargetBulan12()}, lalu <b>menuliskan hasilnya ke field {@link #target}</b> sebelum
	 * mengembalikannya. Nilai yang sebelumnya tersimpan pada field selalu ditimpa; tidak ada jalur
	 * yang mempertahankannya. Karena kedua belas getter bulanan itu sendiri menormalkan {@code null}
	 * menjadi {@code 0.0} sambil menulis balik, satu pemanggilan {@code getTarget()} berpotensi
	 * mengubah tiga belas field sekaligus.</p>
	 *
	 * <h3>Mengapa ini tertulis ke basis data</h3>
	 * <p>Entitas memakai <i>property access</i> — anotasi {@link Id} melekat pada {@link #getId()},
	 * bukan pada field — sehingga Hibernate membaca nilai properti {@code target} dengan memanggil
	 * getter ini, termasuk saat melakukan <i>dirty checking</i> menjelang {@code flush}. Hasil
	 * perhitungan ulang karenanya benar-benar tersimpan ke kolom {@code target}, dan karena entitas
	 * ber-{@link Audited}, perubahan itu juga mencatat revisi baru di tabel Envers meski pengguna
	 * tidak menyunting apa pun. Kolom {@code target} dengan demikian adalah <b>cache
	 * ternormalisasi-balik</b>: ia menyimpan hasil turunan demi kemudahan pelaporan SQL, dan
	 * disegarkan sebagai efek samping pembacaan.</p>
	 *
	 * <h3>Akibat bagi {@link #setTarget(Double)}</h3>
	 * <p>Setter pasangannya praktis tidak berguna: nilai apa pun yang disetel akan tertimpa pada
	 * pembacaan berikutnya, yang hampir pasti terjadi sebelum {@code flush}. Jangan memakainya untuk
	 * menyimpan total yang berbeda dari jumlah kolom bulanan; satu-satunya cara mengubah total
	 * adalah mengubah kolom bulanannya.</p>
	 *
	 * <h3>Akibat bagi pelaporan SQL langsung</h3>
	 * <p>Kolom {@code target} aman dibaca langsung lewat SQL <b>hanya</b> bila baris bersangkutan
	 * pernah dimuat aplikasi setelah perubahan terakhir pada kolom bulanannya. Bila data pernah
	 * disunting lewat jalur non-aplikasi (impor massal, perbaikan SQL manual), kolom ringkasan bisa
	 * basi dan berbeda dari jumlah dua belas kolom bulanan. Laporan yang menuntut ketepatan
	 * sebaiknya menghitung sendiri dengan {@code targetBulan1 + ... + targetBulan12} alih-alih
	 * memercayai kolom {@code target}.</p>
	 *
	 * <h3>Catatan pembulatan</h3>
	 * <p>Penjumlahan dilakukan pada {@link Double}, bukan {@code BigDecimal}, sehingga hasilnya
	 * tunduk pada galat pembulatan biner. Untuk pembandingan kesamaan, gunakan toleransi alih-alih
	 * {@code equals}.</p>
	 *
	 * @return jumlah kedua belas target bulanan; tidak pernah {@code null}.
	 */
	public Double getTarget() {
		target = getTargetBulan1() + getTargetBulan2() + getTargetBulan3() + getTargetBulan4() + getTargetBulan5()
				+ getTargetBulan6() + getTargetBulan7() + getTargetBulan8() + getTargetBulan9() + getTargetBulan10()
				+ getTargetBulan11() + getTargetBulan12();
		return target;
	}

	/**
	 * Menyetel total target setahun. <b>Praktis tidak berpengaruh</b>: {@link #getTarget()}
	 * menghitung ulang dan menimpa nilai ini setiap kali dipanggil, dan pemanggilan itu terjadi
	 * antara lain saat Hibernate melakukan <i>dirty checking</i>. Setter ini ada semata karena
	 * templat generator hbm2java mensyaratkan pasangan getter/setter untuk setiap properti yang
	 * dipetakan. Untuk mengubah total, ubahlah kolom bulanannya.
	 *
	 * @param target nilai yang akan tertimpa pada pembacaan berikutnya.
	 */
	public void setTarget(Double target) {
		this.target = target;
	}

	/**
	 * Mengembalikan total realisasi setahun, yaitu jumlah kedua belas realisasi bulanan. Seperti
	 * {@link #getTarget()}, method ini <b>bukan getter murni</b>: ia menghitung ulang jumlah dari
	 * {@link #getRealisasiBulan1()} sampai {@link #getRealisasiBulan12()} lalu menuliskan hasilnya
	 * ke field {@link #realisasi}, yang kemudian ikut tertulis ke basis data lewat mekanisme
	 * <i>property access</i> Hibernate. Seluruh uraian pada {@link #getTarget()} — perihal cache
	 * ternormalisasi-balik, ketidakbergunaan setter pasangannya, risiko kolom basi bagi pelaporan
	 * SQL langsung, dan catatan pembulatan {@link Double} — berlaku sama persis di sini.
	 *
	 * <p>Yang perlu ditambahkan khusus untuk nilai ini: <b>tidak ada penjaga</b> yang membandingkan
	 * hasilnya dengan {@link #getTarget()}. Total realisasi boleh melampaui total target sebesar apa
	 * pun, boleh bernilai negatif, dan tidak memerlukan persetujuan siapa pun. Bila sebuah laporan
	 * perlu menandai capaian yang melampaui rencana, perbandingan itu harus dilakukan di sisi
	 * laporan — entitas ini tidak menyediakannya.</p>
	 *
	 * @return jumlah kedua belas realisasi bulanan; tidak pernah {@code null}.
	 */
	public Double getRealisasi() {
		realisasi = getRealisasiBulan1() + getRealisasiBulan2() + getRealisasiBulan3() + getRealisasiBulan4()
				+ getRealisasiBulan5() + getRealisasiBulan6() + getRealisasiBulan7() + getRealisasiBulan8()
				+ getRealisasiBulan9() + getRealisasiBulan10() + getRealisasiBulan11() + getRealisasiBulan12();
		return realisasi;
	}

	/**
	 * Menyetel total realisasi setahun. <b>Praktis tidak berpengaruh</b>, dengan alasan yang sama
	 * seperti {@link #setTarget(Double)}: {@link #getRealisasi()} menghitung ulang dan menimpa nilai
	 * ini setiap kali dipanggil. Untuk mengubah total, ubahlah kolom bulanannya.
	 *
	 * @param realisasi nilai yang akan tertimpa pada pembacaan berikutnya.
	 */
	public void setRealisasi(Double realisasi) {
		this.realisasi = realisasi;
	}

	/**
	 * Mengembalikan persentase capaian setahun. Method ini <b>bukan getter murni</b>, dan cara
	 * menghitungnya mengandung dua hal yang sering disalahpahami — keduanya perlu dipahami sebelum
	 * angka ini dipakai sebagai indikator kinerja.
	 *
	 * <h3>Salah paham pertama — ini bukan realisasi dibagi target</h3>
	 * <p>Yang dihitung adalah <b>rata-rata dari kedua belas kolom persentase bulanan</b>, yaitu
	 * {@code (prosentaseRealisasiBulan1 + ... + prosentaseRealisasiBulan12) / 12.0}. Kedua belas
	 * kolom itu sendiri <b>bukan</b> hasil perhitungan melainkan angka yang diketik operator pada
	 * kotak isian tersendiri di layar (lihat {@link #getProsentaseRealisasiBulan1()}). Jadi tidak
	 * ada satu pun jalur kode yang menghubungkan nilai yang dikembalikan method ini dengan
	 * {@link #getTarget()} maupun {@link #getRealisasi()}. Sebuah baris dapat menunjukkan total
	 * realisasi jauh di bawah total target sambil melaporkan persentase 100 — dan sistem tidak akan
	 * mempermasalahkannya. Bila yang dibutuhkan adalah persentase yang benar-benar turunan,
	 * hitunglah sendiri sebagai {@code getRealisasi() / getTarget()} dengan penjagaan pembagian nol,
	 * dan jangan memakai nilai dari method ini.</p>
	 *
	 * <h3>Salah paham kedua — pembaginya selalu dua belas</h3>
	 * <p>Pembagi dipatok tetap {@code 12.0}, bukan banyaknya bulan yang benar-benar terisi. Pada
	 * tahun berjalan yang baru terlapor sebagian, bulan-bulan yang belum diisi bernilai {@code 0.0}
	 * (baik karena memang bawaan maupun karena dinormalkan oleh getter bulanannya) dan tetap ikut
	 * membagi. Akibatnya, unit kerja yang melapor sempurna 100% selama enam bulan pertama akan
	 * menampilkan persentase tahunan 50, bukan 100. Angka ini karenanya hanya bermakna sebagai
	 * "persentase terhadap satu tahun penuh", bukan sebagai "persentase capaian sejauh ini". Setiap
	 * dasbor atau laporan yang memakainya sebagai ukuran kinerja berjalan akan menilai terlalu
	 * rendah, dan perbedaannya melebar makin awal tahun berjalan.</p>
	 *
	 * <h3>Efek samping penulisan</h3>
	 * <p>Sama seperti {@link #getTarget()} dan {@link #getRealisasi()}, hasil perhitungan dituliskan
	 * kembali ke field {@link #prosentase} dan — lewat mekanisme <i>property access</i> Hibernate —
	 * ikut tertulis ke kolom {@code prosentase} pada {@code flush} berikutnya, sekaligus mencatat
	 * revisi Envers. Pemanggilan tunggal method ini berpotensi mengubah tiga belas field: kolom
	 * ringkasan ditambah kedua belas kolom bulanan yang dinormalkan dari {@code null} menjadi
	 * {@code 0.0}. {@link #setProsentase(Double)} karenanya praktis tidak berpengaruh.</p>
	 *
	 * <h3>Catatan pembulatan</h3>
	 * <p>Pembagian dilakukan pada {@link Double}; hasilnya tidak dibulatkan ke sejumlah angka di
	 * belakang koma. Lapisan tampilan memformatnya lewat {@code Common.numberFormat}, tetapi nilai
	 * yang tersimpan tetap penuh presisi biner.</p>
	 *
	 * @return rata-rata kedua belas persentase bulanan dengan pembagi tetap dua belas; tidak pernah
	 *         {@code null}.
	 */
	public Double getProsentase() {
		prosentase = (getProsentaseRealisasiBulan1() + getProsentaseRealisasiBulan2() + getProsentaseRealisasiBulan3()
				+ getProsentaseRealisasiBulan4() + getProsentaseRealisasiBulan5() + getProsentaseRealisasiBulan6()
				+ getProsentaseRealisasiBulan7() + getProsentaseRealisasiBulan8() + getProsentaseRealisasiBulan9()
				+ getProsentaseRealisasiBulan10() + getProsentaseRealisasiBulan11() + getProsentaseRealisasiBulan12())
				/ 12.0;
		return prosentase;
	}

	/**
	 * Menyetel persentase capaian setahun. <b>Praktis tidak berpengaruh</b>, dengan alasan yang sama
	 * seperti {@link #setTarget(Double)} dan {@link #setRealisasi(Double)}: {@link #getProsentase()}
	 * menghitung ulang dan menimpa nilai ini setiap kali dipanggil. Untuk mengubah persentase
	 * tahunan, ubahlah kedua belas kolom persentase bulanannya.
	 *
	 * @param prosentase nilai yang akan tertimpa pada pembacaan berikutnya.
	 */
	public void setProsentase(Double prosentase) {
		this.prosentase = prosentase;
	}

}
