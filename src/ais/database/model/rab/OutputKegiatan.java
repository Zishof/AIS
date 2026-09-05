package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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
 * Entitas master <b>Output Kegiatan</b> (keluaran kegiatan) pada modul RAB/perencanaan anggaran,
 * dipetakan ke tabel {@code rab.output_kegiatan}. Dalam terminologi perencanaan anggaran
 * pemerintah/institusi (RKA-KL, RENSTRA), <i>output</i> adalah barang atau jasa terukur yang
 * dihasilkan oleh sebuah kegiatan — misalnya "Dokumen Kurikulum Tersusun", "Laboratorium
 * Terakreditasi", atau "Mahasiswa Penerima Beasiswa". Entitas ini hanyalah <b>katalog</b> output:
 * kode, nama, dan keterangan. Angka target maupun capaiannya tidak disimpan di sini melainkan pada
 * entitas turunan.
 *
 * <h2>Posisi dalam klaster rencana &amp; realisasi output kegiatan</h2>
 * <p>Verifikasi atas basis kode menunjukkan {@code OutputKegiatan} dirujuk oleh tepat dua entitas,
 * keduanya di paket yang sama:</p>
 * <ul>
 *   <li>{@link RencanaDanRealisasiOutputKegiatan#getOutputKegiatan()} — baris rencana (target) dan
 *   realisasi bulanan atas output ini, per satuan kerja. Inilah tempat angka capaian sebenarnya
 *   berada. Satu {@code OutputKegiatan} dapat memiliki banyak baris rencana-realisasi, satu per
 *   satuan kerja pelaksana.</li>
 *   <li>{@link Tor#getOutputKegiatan()} — dokumen Kerangka Acuan Kerja/Term of Reference yang
 *   menguraikan latar belakang, dasar hukum, dan strategi pencapaian output ini beserta volume dan
 *   satuannya.</li>
 * </ul>
 * <p>Selain itu, {@code ais.action.master.rab.OutputKegiatanAction} menyediakan layar CRUD untuk
 * mengelola katalog ini, dan {@code RencanaDanRealisasiOutputKegiatanAction} memakainya sebagai
 * sumber isi combobox pemilihan output. Perhatikan bahwa <b>tidak ada</b> relasi langsung dari
 * {@code OutputKegiatan} ke {@link Workspace} (yang berperan sebagai pohon program/kegiatan) maupun
 * ke {@link Proyek}: pengikatan output ke kegiatan induknya terjadi secara tidak langsung, yaitu
 * lewat {@link Tor} yang memegang referensi ke {@code Workspace} pada peran {@code program} dan
 * {@code kegiatan}. Dengan kata lain katalog output ini <b>datar</b>, bukan hierarkis.</p>
 *
 * <h2>Master global — tidak ada pembatasan tenant</h2>
 * <p>Entitas ini <b>tidak memiliki relasi {@code SatuanKerja}</b>, berbeda dari tetangga sepaket
 * {@link Indikator}, {@link Sasaran}, {@link Proyek}, dan {@link Tor} yang semuanya membawa kolom
 * {@code satuan_kerja}. Sejalan dengan itu, pencarian pada {@code OutputKegiatanAction} hanya
 * menyaring berdasarkan {@code nama} dan sama sekali tidak menyaring per satuan kerja, sehingga
 * seluruh pengguna yang punya akses ke layar tersebut melihat dan dapat mengubah katalog yang sama.
 * Pada satu sisi ini memang wajar untuk katalog nomenklatur bersama (agar nama output seragam
 * lintas satker); pada sisi lain berarti satu satker dapat mengubah atau menghapus entri yang
 * sedang dipakai satker lain, dan penghapusan semacam itu hanya tertahan oleh kegagalan
 * <i>foreign key</i> di tingkat basis data. Pembatasan tenant justru diterapkan satu tingkat di
 * bawah, pada {@link RencanaDanRealisasiOutputKegiatan} yang menyimpan {@code satuanKerja}
 * per baris rencana-realisasi. Ini instansi dari pola berulang "filter tenant lemah/hilang" yang
 * sudah tercatat pada inisiatif dokumentasi ini.</p>
 *
 * <h2>Tidak ada penjaga keunikan kode maupun nama</h2>
 * <p>Baik {@link #getKode()} maupun {@link #getNama()} <b>tidak</b> dibatasi {@code unique} pada
 * anotasi, dan {@code OutputKegiatanAction} tidak memvalidasi duplikasi saat menyimpan — blok
 * pemeriksaan duplikat nama yang pernah ditulis masih berbentuk kode terkomentari di berkas Action
 * tersebut. Akibatnya dua entri dengan kode identik dapat hidup berdampingan. Hal ini berdampak
 * nyata pada tampilan: {@code RencanaDanRealisasiOutputKegiatanAction} menampilkan kolom identitas
 * baris hanya dengan {@code getOutputKegiatan().getKode()}, sehingga dua output berkode sama akan
 * tampak tidak terbedakan di layar rencana-realisasi. Pemakai yang mengandalkan kode sebagai kunci
 * pelaporan perlu menegakkan keunikan secara operasional atau lewat indeks basis data.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah, dan diberi {@link Audited} sehingga Hibernate Envers merekam
 * setiap revisi ke tabel bayangan pada skema {@code rab}. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}. Kolom {@code nama} dan {@code keterangan}
 * dipetakan sebagai {@code text} (panjang tak terbatas), sementara {@code kode} tidak diberi
 * anotasi {@code @Column} sehingga memakai pemetaan bawaan Hibernate.</p>
 *
 * <h2>Catatan gaya kode</h2>
 * <p>Berkas asli memampatkan deklarasi field {@code oleh}/{@code olehId} beserta accessor-nya ke
 * dalam satu baris, dan menempelkan deklarasi field {@code tanggal_dirubah} di belakang method
 * {@link #onUpdate()}. Pada berkas ini deklarasi tersebut dipisah baris agar setiap anggota dapat
 * diberi dokumentasi, tanpa mengubah semantik apa pun.</p>
 *
 * @see RencanaDanRealisasiOutputKegiatan
 * @see Tor
 * @see ais.database.dao.rab.OutputKegiatanDao
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "output_kegiatan")



public class OutputKegiatan extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada
	 * {@code rab.output_kegiatan.id}. Bernilai {@code null} selama objek belum pernah disimpan.
	 */
	private Long id;

	/**
	 * Field audit bayangan: nama pengguna terakhir yang mengubah baris ini. Diisi lewat
	 * {@link #setOleh(String)} oleh lapisan interceptor/penyimpanan, bukan oleh pengguna.
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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	 * Kait daur hidup JPA yang dijalankan tepat sebelum operasi {@code UPDATE}. Method ini
	 * mendelegasikan pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, sehingga
	 * {@link #tanggal_dirubah} selalu mencerminkan saat penyimpanan terakhir tanpa perlu diisi oleh
	 * pemanggil. Tidak boleh dipanggil langsung dari kode aplikasi.
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

	/**
	 * Kode/nomenklatur output kegiatan, misalnya kode keluaran pada dokumen RKA-KL. Diisi bebas oleh
	 * operator lewat {@code OutputKegiatanAction}; tidak dibangkitkan otomatis dan tidak dijamin
	 * unik (lihat catatan pada dokumentasi kelas).
	 */
	private String kode;

	/**
	 * Nama/uraian output kegiatan. Dipetakan sebagai kolom {@code text} sehingga uraian panjang
	 * dapat ditampung utuh.
	 */
	private String nama;

	/**
	 * Keterangan tambahan atas output. Dipetakan sebagai kolom {@code text}.
	 */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai {@code OutputKegiatanAction} saat menekan tombol tambah data.
	 */
	public OutputKegiatan() {
	}

	/**
	 * Konstruktor pintas yang langsung mengisi {@link #nama}, disediakan oleh generator hbm2java.
	 *
	 * @param nama nama/uraian output kegiatan.
	 */
	public OutputKegiatan(String nama) {
		this.nama = nama;
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
	 * Menyetel kunci utama. Normalnya diisi Hibernate setelah {@code INSERT}; pengisian manual hanya
	 * relevan pada skenario impor/migrasi.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama output dengan spasi di ujung sudah dipangkas. Pemangkasan dilakukan pada
	 * getter (bukan setter), sehingga nilai di memori bisa saja masih mengandung spasi sementara
	 * nilai yang ditulis Hibernate ke basis data sudah terpangkas — perilaku ini konsisten dengan
	 * seluruh entitas hasil hbm2java di repo.
	 *
	 * @return nama output yang sudah dipangkas, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama output apa adanya, tanpa pemangkasan maupun pemeriksaan duplikasi. Karena kolom
	 * bertipe {@code text} dan tidak diberi batasan {@code unique}, nilai apa pun (termasuk nama
	 * yang sudah dipakai entri lain) akan diterima.
	 *
	 * @param nama nama/uraian output kegiatan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan tambahan atas output. Berbeda dari {@link #getNama()}, nilai
	 * dikembalikan apa adanya tanpa pemangkasan spasi.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan tambahan atas output.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode/nomenklatur output. Nilai dikembalikan apa adanya, tanpa pemangkasan spasi
	 * dan tanpa nilai bawaan bila kosong — pemanggil yang menampilkannya langsung (seperti
	 * {@code RencanaDanRealisasiOutputKegiatanAction}) perlu bersiap menerima {@code null}.
	 *
	 * @return kode output, atau {@code null} bila belum diisi.
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode/nomenklatur output. Tidak ada validasi format maupun pemeriksaan keunikan;
	 * seluruh penegakan keunikan menjadi tanggung jawab operator atau indeks basis data.
	 *
	 * @param kode kode output kegiatan.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
