package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
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

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.rab.Pejabat;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Entitas <b>status/riwayat</b> satu tahapan persetujuan untuk satu <b>surat keluar</b> tertentu.
 * Dipetakan ke tabel {@code surat.alur_persetujuan_surat_keluar_status}.
 *
 * <h2>Kedudukan dalam modul persuratan</h2>
 * {@link AlurPersetujuanSuratKeluar} adalah <i>definisi</i> jenjang (template pohon jenis jabatan);
 * kelas ini adalah <i>realisasi</i>-nya: satu baris = satu (surat keluar &times; jenjang alur
 * &times; pejabat) beserta keputusannya. Baris dibuat "malas": jenjang pertama dibuat saat surat
 * keluar disimpan ({@code ais.action.master.surat.SuratKeluarAction}), jenjang berikutnya baru
 * dibuat setelah jenjang sebelumnya ditandai disetujui &mdash; oleh
 * {@code ais.action.master.surat.AlurPersetujuanSuratKeluarStatusAction} (jalur ZK) atau
 * {@code ais.action.servlet.api.SuratApi#disposisi_surat_keluar} (jalur API mobile).
 *
 * <h2>Model status: empat bendera yang tidak saling mengunci</h2>
 * <ul>
 * <li>{@link #getDisetujui()} &mdash; tahapan disetujui;</li>
 * <li>{@link #getDitolak()} &mdash; tahapan ditolak;</li>
 * <li>{@link #getSelesai()} &mdash; tahapan menutup rantai lebih awal (hanya bermakna bila simpul
 * alurnya mengizinkan lewat {@link AlurPersetujuanSuratKeluar#getTerdapatPilihanSelesai()});</li>
 * <li>{@link #getTelahDirevisi()} &mdash; berkas sudah direvisi setelah penolakan sehingga tahapan
 * dibuka kembali;</li>
 * </ul>
 * ditambah {@link #getMasihLanjut()} yang menyatakan apakah masih ada jenjang berikutnya.
 * Semuanya kolom {@code boolean} terpisah dan <b>tidak ada invarian pada level entitas</b> yang
 * mencegah kombinasi mustahil (mis. {@code disetujui} dan {@code ditolak} sama-sama menyala).
 * Saling-eksklusi hanya diusahakan pendengar {@code onCheck} pada kotak centang ZK &mdash; logika
 * presentasi, bukan invarian data &mdash; sehingga tidak berlaku pada jalur API maupun pada kode
 * server lain yang memanggil setter langsung. Konsumen (mis. {@code DasboardSurat},
 * {@code DasboardAlurSurat}) karena itu membaca bendera dengan urutan prioritas ("ditolak dulu, baru
 * disetujui"), bukan dengan asumsi hanya satu bendera menyala.
 *
 * <h2>Tidak ada konsep "seluruh rantai disetujui"</h2>
 * Tidak ada method di kelas ini, di {@link SuratKeluar}, maupun di helper persuratan yang menghitung
 * apakah SELURUH tahapan pada rantai alur sebuah surat sudah disetujui. Yang tersedia hanyalah
 * {@code SuratKeluar.getAlurDitolak()} (penunjuk tunggal ke baris yang menolak, diisi
 * {@code DasboardSurat.tolak}) dan pembacaan per baris pada perender dasbor. "Selesai/final" di
 * modul ini adalah <b>kesimpulan visual</b> dari rangkaian lencana per tahapan, bukan status
 * terhitung yang bisa dijadikan gerbang. Kode yang ingin memastikan sebuah surat benar-benar
 * melewati seluruh jenjang harus menelusuri sendiri pohon {@link AlurPersetujuanSuratKeluar} dan
 * mencocokkannya dengan baris-baris kelas ini.
 *
 * <h2>PERINGATAN KEAMANAN &mdash; gerbang persetujuan hanya mengontrol tampilan</h2>
 * Hasil penelusuran kode pada jalur mutasi data:
 * <ol>
 * <li><b>Jalur dasbor ZK.</b> {@code DasboardSurat} memanggil
 * {@code bolehAksesAlurBerdasarkanLoginV20(pejabat, jenisJabatan)} hanya untuk memutuskan apakah
 * tombol "Tindak Lanjuti"/"Ubah" <i>dirender</i>. Tombol memanggil
 * {@code AlurPersetujuanSuratKeluarStatusAction.onAddExternal(...)}, dan {@code onSave()} pada
 * Action tersebut menuliskan {@link #setDisetujui(Boolean)}, {@link #setDitolak(Boolean)},
 * {@link #setSelesai(Boolean)}, dan {@link #setPejabat(Pejabat)} <b>tanpa memanggil ulang
 * pemeriksaan apa pun</b> &mdash; pejabatnya bahkan diambil dari kombo pemilihan pada form,
 * sehingga siapa pun yang dapat membuka form dapat mencatat persetujuan atas nama pejabat mana pun.
 * Action yang sama juga terdaftar sebagai menu mandiri (lihat {@code MainAction}/{@code MainAction2})
 * sehingga dapat dicapai tanpa melewati dasbor. Ini persis pola "gerbang APPROVE hanya kontrol
 * visibilitas UI" yang sudah dikonfirmasi kritis di modul kepegawaian.</li>
 * <li><b>Jalur API mobile.</b> {@code SuratApi.disposisi_surat_keluar} hanya memvalidasi token
 * ({@code ApiUtil.currentUser}), lalu memuat baris ini semata-mata dari {@code alurId} kiriman
 * klien dan menyetel {@code disetujui}/{@code ditolak} sesuai isi permintaan. Tidak ada pemeriksaan
 * bahwa pemanggil adalah {@link #getPejabat()} baris tersebut, tidak ada pemeriksaan jenis jabatan,
 * dan tidak ada pemeriksaan bahwa jenjang induk sudah disetujui &mdash; sehingga jenjang terakhir
 * dapat disetujui langsung, melompati seluruh jenjang di atasnya.</li>
 * <li><b>Jejak audit dapat menyesatkan.</b> Pada jalur API, {@code setPejabat(pejabatcurrent)} hanya
 * dijalankan bila pemanggil memang terdaftar sebagai {@link Pejabat}; bila tidak, field
 * {@code pejabat} tetap berisi pejabat asli sehingga baris tersimpan seolah-olah pejabat yang
 * berwenanglah yang menyetujui. Satu-satunya jejak pelaku sebenarnya adalah field audit bayangan
 * {@link #getOleh()}/{@link #getOlehId()}.</li>
 * <li><b>Tidak ada pemisahan pengusul vs penyetuju.</b> {@link #getKonseptor()} (pengonsep) dan
 * {@link #getPejabat()} (penyetuju) hidup pada baris yang sama tanpa satu pun pemeriksaan bahwa
 * keduanya berbeda orang. Pada {@code SuratApi} baris jenjang berikutnya justru dibuat dengan
 * {@code setKonseptor(tbmuser)} &mdash; pemanggil yang baru saja menyetujui dicatat sebagai
 * konseptor jenjang selanjutnya.</li>
 * </ol>
 * <b>Dampak hilir yang membedakan sisi surat keluar dari sisi surat masuk.</b>
 * {@code ais.action.master.surat.util.SuratUtil#initGambarTandaTangan(SuratKeluar, java.util.Map)}
 * mencari seluruh baris kelas ini yang ber-{@code disetujui = true} untuk surat bersangkutan, lalu
 * menempelkan <b>berkas gambar tanda tangan</b> pejabat terkait ke parameter laporan
 * ({@code "ttd." + kode jenis jabatan}) yang dipakai templat Jasper saat mencetak surat. Pemeriksaan
 * yang dilakukan hanya "bendera menyala"; keutuhan rantai, kewenangan penyetuju, maupun urutan
 * jenjang tidak diperiksa. Dengan kata lain, menyalakan satu bendera pada baris yang tepat sudah
 * cukup untuk memunculkan tanda tangan pejabat pada surat keluar tercetak.
 *
 * <h2>Catatan arsitektur lain</h2>
 * <ul>
 * <li><b>Getter destruktif</b> pada hampir semua relasi (pola {@code field = check(field)}), plus
 * tiga getter yang lebih agresif: {@link #getKonseptor()} (mengosongkan konseptor bila ada
 * siswa/mahasiswa), {@link #getKodeUnik()} (menghitung ulang kunci unik setiap pembacaan), dan
 * {@link #getSelesai()} (memaksa {@code false} bila simpul alur tidak mengizinkan pilihan
 * selesai).</li>
 * <li><b>Getter yang memfabrikasi nilai</b>: {@link #getWaktuPersetujuan()} dan
 * {@link #getWaktuDitolak()} mengembalikan {@code null} bila bendera terkait mati, dan nilai
 * cadangan bila bendera menyala tetapi kolom kosong.</li>
 * <li><b>Field audit bayangan</b> {@code oleh}/{@code olehId}/{@code tanggal_dirubah} &mdash;
 * keharusan teknis interseptor audit, bukan duplikasi yang perlu dibersihkan.</li>
 * <li><b>Nama field vs nama properti tidak sejalan</b> pada catatan revisi: field
 * {@code catatanDisposisi}, properti {@code catatanRevisi}, kolom {@code catatan_revisi}.</li>
 * </ul>
 *
 * @see AlurPersetujuanSuratKeluar
 * @see AlurPersetujuanSuratMasukStatus
 * @see SuratKeluar
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "surat", name = "alur_persetujuan_surat_keluar_status")
public class AlurPersetujuanSuratKeluarStatus extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya identik dengan entitas alur persuratan lainnya (warisan
	 * salin-tempel generator) sehingga tidak dapat dipakai membedakan tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama (identity, di-generate database). */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi interseptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi interseptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyentuh baris ini.
	 * <p>
	 * Untuk baris persetujuan, field ini sering menjadi satu-satunya jejak <i>pelaku sebenarnya</i>:
	 * {@link #getPejabat()} menyatakan atas nama siapa keputusan tercatat, sedangkan field ini
	 * menyatakan akun mana yang benar-benar melakukan penulisan terakhir. Keduanya dapat berbeda.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir; nilai {@code null}/kosong diabaikan agar jejak audit
	 * yang sudah ada tidak terhapus oleh binding form kosong.
	 *
	 * @param olehId id pengguna
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks ringkas baris status: {@code id-pejabat-keterangan}. Memanggil
	 * {@link #getPejabat()} sehingga ikut menormalisasi (dan berpotensi mengubah) field
	 * {@code pejabat}.
	 *
	 * @return teks tampilan baris status
	 */
	public String toString() {
		return id + "-" + getPejabat() + "-" + keterangan;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; nilai {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pengguna
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyentuh baris ini (field audit bayangan).
	 *
	 * @return nama pengguna, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: mendelegasikan pengisian stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum {@code UPDATE}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan objek. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 * <p>
	 * Berbeda dari padanannya di {@link AlurPersetujuanSuratMasukStatus}, getter ini TIDAK
	 * mematerialkan nilai default bila field masih {@code null} &mdash; sehingga nilainya dapat
	 * {@code null}. Ini penting karena {@link #getWaktuDitolak()} memakainya sebagai nilai cadangan:
	 * pada baris yang stempel perubahannya kosong, waktu penolakan yang dilaporkan juga akan
	 * {@code null} meskipun bendera penolakan menyala.
	 *
	 * @return waktu perubahan terakhir, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Surat keluar yang tahapannya dicatat baris ini. */
	private SuratKeluar suratKeluar;

	/** Simpul definisi alur (jenjang) yang direalisasikan baris ini. */
	private AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar;

	/** Jenis jabatan tahapan; diturunkan dari pejabat bila tersedia. */
	private JenisJabatan jenisJabatan;

	/** Keterangan/catatan disposisi yang ditulis penindaklanjut. */
	private String keterangan;

	/** Bendera "tahapan disetujui". Default {@code false}. */
	private Boolean disetujui = false;

	/** Bendera "rantai ditutup di tahapan ini". Bergantung pada kebijakan simpul alur. */
	private Boolean selesai = false;

	/** Bendera "tahapan ditolak". Default {@code false}. Tidak saling mengunci dengan disetujui. */
	private Boolean ditolak = false;

	/** Bendera "sudah direvisi setelah ditolak". */
	private Boolean telahDirevisi = false;

	/** Bendera "masih ada jenjang berikutnya". {@code null} dibaca sebagai {@code true}. */
	private Boolean masihLanjut;

	/** Pejabat yang tercatat sebagai penindaklanjut/penyetuju tahapan ini. */
	private Pejabat pejabat;

	/** Stempel waktu persetujuan; hanya bermakna bila {@link #disetujui} menyala. */
	private Date waktuPersetujuan;

	/** Peta JSON pejabat tujuan disposisi lanjutan (dipakai form dan BroadcastHelper). */
	private String jenisSurats;

	/** Stempel waktu penolakan; hanya bermakna bila {@link #ditolak} menyala. */
	private Date waktuDitolak;

	/** Kunci unik turunan (bukan masukan pengguna) &mdash; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Pengguna pengonsep baris ini. Tidak pernah diadu dengan {@link #pejabat}. */
	private Tbmuser konseptor;

	/** Mahasiswa pengaju (bila surat berasal dari layanan mahasiswa). */
	private Mahasiswa mahasiswa;

	/** Siswa pengaju (bila surat berasal dari layanan siswa). */
	private Siswa siswa;

	/**
	 * Catatan revisi/disposisi. Perhatikan ketidakselarasan penamaan: field {@code catatanDisposisi},
	 * properti {@code catatanRevisi}, kolom {@code catatan_revisi}.
	 */
	private String catatanDisposisi;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public AlurPersetujuanSuratKeluarStatus() {

	}

	/**
	 * Mengembalikan kunci utama baris status.
	 * <p>
	 * Id inilah yang dikirim klien sebagai {@code alurId} ke
	 * {@code SuratApi.disposisi_surat_keluar}; lihat peringatan keamanan pada dokumentasi kelas
	 * mengenai ketiadaan pemeriksaan kepemilikan atas id tersebut.
	 *
	 * @return id, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan/catatan disposisi, dengan materialisasi default string kosong bila
	 * field masih {@code null} (getter ini menulis balik ke field).
	 *
	 * @return keterangan (tidak pernah {@code null})
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan disposisi. Nilai {@code null}/kosong <b>diabaikan</b>, sehingga catatan
	 * yang sudah ada tidak dapat dihapus lewat setter ini &mdash; termasuk saat penindaklanjut
	 * berikutnya sengaja mengosongkan kolom catatan pada form. Catatan lama akan tetap terlihat
	 * seolah-olah milik keputusan terbaru.
	 *
	 * @param keterangan teks keterangan; {@code null}/kosong tidak berpengaruh
	 */
	public void setKeterangan(String keterangan) {
		if (keterangan == null || keterangan.trim().isEmpty()) {
			return;
		}
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan surat keluar yang tahapannya dicatat baris ini.
	 * <p>
	 * Berbeda dengan relasi lain di kelas ini, getter ini TIDAK memanggil
	 * {@link GeneralValueObject#check(Object)} &mdash; nilai dikembalikan apa adanya. Relasi memakai
	 * {@code FetchMode.SELECT} agar surat dimuat lewat query terpisah, menghindari join besar saat
	 * daftar status dirender.
	 *
	 * @return surat keluar, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_keluar", nullable = true)
	public SuratKeluar getSuratKeluar() {
		return suratKeluar;
	}

	/**
	 * Menyetel surat keluar pemilik baris status.
	 *
	 * @param suratKeluar surat keluar
	 */
	public void setSuratKeluar(SuratKeluar suratKeluar) {
		this.suratKeluar = suratKeluar;
	}

	/**
	 * Mengembalikan simpul definisi alur (jenjang) yang direalisasikan baris ini, setelah
	 * dinormalisasi lewat {@link GeneralValueObject#check(Object)} (getter destruktif).
	 * <p>
	 * Relasi ini dipakai mesin alur untuk mencari jenjang berikutnya (anak-anak simpul ini pada
	 * pohon {@link AlurPersetujuanSuratKeluar}) dan dipakai {@link #getSelesai()} untuk mengetahui
	 * apakah tahapan boleh menutup rantai lebih awal. Bila relasi ini {@code null} &mdash;
	 * dimungkinkan karena kolomnya {@code nullable} &mdash; baris menjadi "melayang": tetap tampil
	 * pada dasbor dan tetap dapat disetujui, tetapi tidak pernah menurunkan jenjang berikutnya
	 * karena pencarian anak tidak punya titik awal.
	 *
	 * @return simpul definisi alur, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_keluar", nullable = true)
	public AlurPersetujuanSuratKeluar getAlurPersetujuanSuratKeluar() {
		alurPersetujuanSuratKeluar = check(alurPersetujuanSuratKeluar);
		return alurPersetujuanSuratKeluar;
	}

	/**
	 * Menyetel simpul definisi alur yang direalisasikan baris ini.
	 *
	 * @param alurPersetujuanSuratKeluar simpul definisi alur
	 */
	public void setAlurPersetujuanSuratKeluar(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) {
		this.alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluar;
	}

	/**
	 * Mengembalikan bendera persetujuan tahapan ini &mdash; satu-satunya penanda bahwa jenjang ini
	 * dianggap lolos, dan bendera dengan konsekuensi paling luas di seluruh modul persuratan.
	 * <p>
	 * Getter melakukan materialisasi default: bila field masih {@code null} (baris lama sebelum
	 * kolom diperkenalkan, atau entitas hasil deserialisasi), field diisi {@code false} lalu
	 * dikembalikan. Karena penulisan terjadi pada field, entitas terkelola yang di-flush setelah
	 * pembacaan ini akan ikut menuliskan {@code false} ke kolom &mdash; sekadar membaca status dapat
	 * mematerialkan nilai di database. Untuk bendera persetujuan, arah materialisasi ini memang
	 * benar (fail-closed: tak diketahui diperlakukan sebagai belum disetujui).
	 * <p>
	 * <b>Semantik dan jangkauan.</b> Bendera bekerja per baris, dan satu baris hanya mewakili satu
	 * pasangan (jenjang &times; pejabat) untuk satu surat. Tidak ada satu pun tempat di codebase
	 * yang mengagregasi bendera ini menjadi "surat sudah disetujui seluruhnya". Konsumennya membaca
	 * per baris:
	 * <ul>
	 * <li>{@code DasboardSurat}/{@code DasboardAlurSurat} merender lencana per tahapan
	 * (Disetujui/Ditolak/Menunggu) dan menentukan tahapan mana yang masih "pending"
	 * ({@code !disetujui && !ditolak});</li>
	 * <li>{@code SuratApi.disposisi_surat_keluar} memakainya sebagai pemicu pembentukan baris jenjang
	 * berikutnya beserta pengiriman notifikasi lewat {@code BroadcastHelper};</li>
	 * <li>{@code SuratUtil.initGambarTandaTangan} menyaring baris dengan {@code disetujui = true}
	 * untuk menempelkan <b>gambar tanda tangan</b> pejabat pada dokumen tercetak.</li>
	 * </ul>
	 * <b>Konsekuensi keamanan.</b> Karena tidak ada agregasi rantai, tidak ada pula pemeriksaan
	 * bahwa jenjang induk sudah disetujui sebelum jenjang anak boleh menyala. Baris jenjang mana pun
	 * yang sudah terwujud di tabel dapat langsung ditandai disetujui, dan seluruh konsumen di atas
	 * akan menghormatinya &mdash; termasuk pembubuhan tanda tangan. Pada jalur API, baris dipilih
	 * semata-mata dari {@code alurId} kiriman klien tanpa pencocokan dengan identitas pemanggil;
	 * pada jalur ZK, pemeriksaan kewenangan ({@code bolehAksesAlurBerdasarkanLoginV20}) hanya
	 * menentukan apakah tombol tindak lanjut dirender, sedangkan {@code onSave()} menulis bendera ini
	 * tanpa memeriksa apa pun dan bahkan mengambil pejabat dari kombo pilihan pada form. Dengan kata
	 * lain bendera ini adalah <i>data biasa</i>, bukan hasil keputusan berwenang yang terverifikasi.
	 * <p>
	 * Pembaca yang membangun fitur baru di atas modul ini sebaiknya tidak memperlakukan
	 * {@code getDisetujui() == true} sebagai bukti otorisasi. Gerbang yang benar harus ditegakkan di
	 * titik mutasi &mdash; memastikan pengguna saat ini benar-benar {@link #getPejabat()} baris
	 * tersebut atau memegang {@link #getJenisJabatan()} yang sesuai, memastikan seluruh jenjang induk
	 * sudah disetujui, dan memastikan penyetuju bukan {@link #getKonseptor()} baris yang sama &mdash;
	 * bukan di titik render.
	 *
	 * @return {@code true} bila tahapan ini bertanda disetujui; tidak pernah {@code null}
	 */
	public Boolean getDisetujui() {
		if (disetujui == null) {
			disetujui = false;
		}
		return disetujui;
	}

	/**
	 * Menyetel bendera persetujuan tahapan.
	 * <p>
	 * Setter menerima nilai apa adanya &mdash; termasuk {@code null}, dan termasuk {@code true}
	 * bersamaan dengan {@link #setDitolak(Boolean)} {@code true} pada objek yang sama. Tidak ada
	 * validasi kewenangan, tidak ada pencatatan siapa yang menyalakan bendera (itu tugas field audit
	 * bayangan {@link #getOleh()}), dan tidak ada pemeriksaan bahwa jenjang induk sudah lolos.
	 * Seluruh penjagaan yang ada hidup di lapisan presentasi (pendengar {@code onCheck} kotak
	 * centang ZK yang saling menonaktifkan) sehingga tidak berlaku bagi jalur API maupun bagi kode
	 * server lain yang memanggil setter ini langsung.
	 *
	 * @param disetujui nilai bendera persetujuan
	 */
	public void setDisetujui(Boolean disetujui) {
		this.disetujui = disetujui;
	}

	/**
	 * Mengembalikan pejabat yang tercatat sebagai penindaklanjut tahapan ini, setelah dinormalisasi
	 * lewat {@link GeneralValueObject#check(Object)} (getter destruktif).
	 * <p>
	 * Field ini menyatakan <i>atas nama siapa</i> keputusan tercatat, bukan akun mana yang
	 * menuliskannya. Pada jalur ZK nilainya diambil dari kombo pemilihan pejabat pada form
	 * ({@code pejabat.getAttribute("pejabat")}), sehingga pengguna yang membuka form dapat memilih
	 * pejabat mana pun; pada jalur API nilainya ditimpa dengan pejabat milik pemanggil hanya bila
	 * pemanggil memang terdaftar sebagai pejabat, dan dibiarkan apa adanya bila tidak.
	 * <p>
	 * Pejabat inilah yang dipakai {@code SuratUtil.initGambarTandaTangan} untuk mencari
	 * {@code FotoGambarTandaTanganPejabat} dan menempelkan gambar tanda tangannya pada surat
	 * tercetak.
	 *
	 * @return pejabat penindaklanjut, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pejabat", nullable = true)
	public Pejabat getPejabat() {
		pejabat = check(pejabat);
		return pejabat;
	}

	/**
	 * Menyetel pejabat penindaklanjut tahapan.
	 * <p>
	 * Mengubah pejabat juga mengubah nilai turunan {@link #getKodeUnik()}, karena kunci unik
	 * dibentuk dari pasangan (pejabat, surat). Perubahan pejabat pada baris yang sudah tersimpan
	 * karena itu dapat menabrak batasan unik baris lain; lapisan Action menanganinya dengan mencari
	 * baris yang sudah memakai kunci target lalu memakai ulang baris tersebut.
	 *
	 * @param pejabat pejabat penindaklanjut
	 */
	public void setPejabat(Pejabat pejabat) {
		this.pejabat = pejabat;
	}

	/**
	 * Mengembalikan waktu persetujuan tahapan, <b>diturunkan dari bendera</b> dan bukan sekadar isi
	 * kolom:
	 * <ul>
	 * <li>bila {@link #getDisetujui()} {@code true} dan kolom masih kosong, dikembalikan waktu
	 * <i>sekarang</i> ({@link WaktuUtil#getDate()}) &mdash; nilai difabrikasi saat pembacaan dan
	 * berubah setiap kali dipanggil sampai ada penyimpanan;</li>
	 * <li>bila {@link #getDisetujui()} {@code false}, dikembalikan {@code null} meskipun kolom
	 * berisi stempel lama.</li>
	 * </ul>
	 * Karena getter inilah yang dibaca Hibernate saat menyusun {@code INSERT}/{@code UPDATE}
	 * (pemetaan berbasis properti), perilaku di atas berdampak langsung ke data tersimpan: mencabut
	 * persetujuan sebuah tahapan akan <b>menghapus</b> stempel waktu persetujuan sebelumnya menjadi
	 * {@code NULL}, sehingga riwayat kapan tahapan itu pernah disetujui hilang dari tabel utama
	 * (hanya tersisa pada tabel audit Envers). Sebaliknya, menyalakan persetujuan tanpa mengisi
	 * stempel akan diam-diam merekam waktu penyimpanan sebagai waktu persetujuan &mdash; termasuk
	 * pada penyetujuan yang dilakukan lewat jalur yang tidak memeriksa kewenangan.
	 * <p>
	 * Berbeda dari padanannya di {@link AlurPersetujuanSuratMasukStatus#getWaktuPersetujuan()},
	 * pemeriksaan di sini memakai {@link #getDisetujui()} (bukan field mentah) sehingga aman
	 * terhadap field {@code null}.
	 *
	 * @return waktu persetujuan, atau {@code null} bila tahapan tidak bertanda disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_persetujuan", nullable = true)
	public Date getWaktuPersetujuan() {
		if (getDisetujui()) {
			return waktuPersetujuan == null ? WaktuUtil.getDate() : waktuPersetujuan;
		} else {
			return null;
		}

	}

	/**
	 * Menyetel stempel waktu persetujuan. Nilai yang disimpan hanya terlihat kembali selama bendera
	 * {@link #getDisetujui()} menyala &mdash; lihat {@link #getWaktuPersetujuan()}.
	 *
	 * @param waktuPersetujuan waktu persetujuan
	 */
	public void setWaktuPersetujuan(Date waktuPersetujuan) {
		this.waktuPersetujuan = waktuPersetujuan;
	}

	/**
	 * Mengembalikan waktu penolakan tahapan, diturunkan dari bendera dengan pola yang sama seperti
	 * {@link #getWaktuPersetujuan()}: {@code null} bila tahapan tidak bertanda ditolak, dan &mdash;
	 * bila bertanda ditolak namun kolom kosong &mdash; nilai cadangan {@link #getTanggal_dirubah()}
	 * (bukan waktu sekarang, berbeda dari sisi persetujuan). Karena {@link #getTanggal_dirubah()} di
	 * kelas ini tidak mematerialkan default, nilai cadangan itu sendiri dapat {@code null}.
	 *
	 * @return waktu penolakan, atau {@code null} bila tahapan tidak bertanda ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_ditolak", nullable = true)
	public Date getWaktuDitolak() {
		if (getDitolak()) {
			return waktuDitolak == null ? getTanggal_dirubah() : waktuDitolak;
		} else {
			return null;
		}

	}

	/**
	 * Menyetel stempel waktu penolakan.
	 *
	 * @param waktuDitolak waktu penolakan
	 */
	public void setWaktuDitolak(Date waktuDitolak) {
		this.waktuDitolak = waktuDitolak;
	}

	/**
	 * Mengembalikan jenis jabatan tahapan ini, dengan aturan turunan: bila {@link #getPejabat()}
	 * tersedia dan punya jenis jabatan, nilai pejabatlah yang menang dan <b>menimpa</b> field
	 * {@code jenisJabatan}; bila tidak, field dinormalisasi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 * <p>
	 * Getter ini bersifat destruktif ganda: ia dapat mengganti jenis jabatan yang sengaja disimpan
	 * pada baris dengan jenis jabatan pejabat terkini. Bila seorang pejabat dipindah ke jabatan lain,
	 * membaca baris status lama akan menampilkan (dan, setelah flush, menyimpan) jabatan barunya
	 * &mdash; riwayat "disetujui oleh siapa dalam kapasitas apa" ikut berubah surut. Hal ini juga
	 * menggeser kunci laporan {@code "ttd." + kode jenis jabatan} pada
	 * {@code SuratUtil.initGambarTandaTangan}, sehingga tanda tangan dapat muncul pada slot templat
	 * yang berbeda dari saat surat pertama kali dicetak.
	 * <p>
	 * Nilai ini ikut membentuk {@link #getKodeUnik()} pada baris yang tidak memiliki pejabat, dan
	 * dipakai {@code bolehAksesAlurBerdasarkanLoginV20} pada dasbor untuk menentukan apakah pengguna
	 * yang sedang masuk berhak melihat tombol tindak lanjut.
	 *
	 * @return jenis jabatan tahapan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_jabatan", nullable = true)
	public JenisJabatan getJenisJabatan() {
		pejabat = getPejabat();
		if (pejabat != null && pejabat.getJenisJabatan() != null) {
			jenisJabatan = pejabat.getJenisJabatan();
		} else {
			jenisJabatan = check(jenisJabatan);
		}
		return jenisJabatan;
	}

	/**
	 * Menyetel jenis jabatan tahapan. Nilai yang disetel dapat ditimpa kembali oleh
	 * {@link #getJenisJabatan()} bila baris memiliki pejabat berjenis jabatan lain.
	 *
	 * @param jenisJabatan jenis jabatan
	 */
	public void setJenisJabatan(JenisJabatan jenisJabatan) {
		this.jenisJabatan = jenisJabatan;
	}

	/**
	 * Mengembalikan bendera "masih ada jenjang berikutnya", dengan default {@code true} bila belum
	 * pernah diisi (tanpa menulis balik ke field).
	 * <p>
	 * Diisi mesin alur saat membuat baris jenjang berikutnya, dan dibaca
	 * {@code AlurPersetujuanSuratKeluarStatusAction} untuk menentukan apakah bagian pemilihan
	 * disposisi lanjutan perlu ditampilkan. Default "masih lanjut" berarti baris yang belum
	 * ditentukan diperlakukan seolah rantai belum berakhir.
	 *
	 * @return {@code true} bila masih ada jenjang berikutnya
	 */
	public Boolean getMasihLanjut() {
		return masihLanjut == null ? true : masihLanjut;
	}

	/**
	 * Menyetel bendera "masih ada jenjang berikutnya".
	 *
	 * @param masihLanjut nilai bendera
	 */
	public void setMasihLanjut(Boolean masihLanjut) {
		this.masihLanjut = masihLanjut;
	}

	/**
	 * Mengembalikan peta JSON pejabat tujuan disposisi lanjutan yang dipilih pada tahapan ini. Bila
	 * kosong, dikembalikan {@code Pertangungjawaban.DEFAULT_FORMULA} &mdash; konstanta JSON kosong
	 * yang dipinjam dari modul akunting agar pemanggil selalu dapat mem-parse hasilnya dengan
	 * {@code new JSONObject(...)} tanpa penjagaan tambahan.
	 * <p>
	 * Isi peta ini menentukan disposisi bebas (ad hoc). Perhatikan bahwa kebijakan
	 * {@link AlurPersetujuanSuratKeluar#getHarusMengikutiAlur()} yang seharusnya melarang disposisi
	 * bebas hanya menyembunyikan tab pemilihannya di antarmuka &mdash; isi peta ini tetap diproses
	 * apa adanya saat penyimpanan dan oleh {@code BroadcastHelper}.
	 *
	 * @return string JSON (tidak pernah {@code null} atau kosong)
	 */
	@Column(name = "jenis_surats", columnDefinition = "text")
	public String getJenisSurats() {
		return jenisSurats == null || jenisSurats.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : jenisSurats;
	}

	/**
	 * Menyetel peta JSON pejabat tujuan disposisi lanjutan. Isi tidak divalidasi sebagai JSON di
	 * sini; kesalahan format baru muncul saat pemanggil mem-parse-nya.
	 *
	 * @param jenisSurats string JSON
	 */
	public void setJenisSurats(String jenisSurats) {
		this.jenisSurats = jenisSurats;
	}

	/**
	 * Mengembalikan bendera penolakan tahapan, dengan default {@code false} bila belum pernah diisi
	 * (tanpa menulis balik ke field).
	 * <p>
	 * Bila menyala, {@code DasboardSurat.tolak(...)} menyimpan baris ini sebagai
	 * {@code SuratKeluar.alurDitolak} &mdash; penunjuk tunggal ke penolakan terakhir pada surat.
	 * Karena penunjuknya tunggal, penolakan sebelumnya pada surat yang sama tidak lagi terlihat dari
	 * sisi {@link SuratKeluar} (riwayatnya tetap ada sebagai baris-baris kelas ini). Pada jalur API,
	 * penolakan juga <b>menghapus</b> baris jenjang berikutnya yang sudah terlanjur dibuat.
	 *
	 * @return {@code true} bila tahapan bertanda ditolak
	 */
	public Boolean getDitolak() {
		return ditolak == null ? false : ditolak;
	}

	/**
	 * Menyetel bendera penolakan tahapan. Sama seperti {@link #setDisetujui(Boolean)}, tidak ada
	 * validasi kewenangan maupun saling-eksklusi dengan bendera persetujuan.
	 *
	 * @param ditolak nilai bendera penolakan
	 */
	public void setDitolak(Boolean ditolak) {
		this.ditolak = ditolak;
	}

	/**
	 * Membentuk kunci unik logis sebuah baris status dari kombinasi aktor dan surat keluar.
	 * <p>
	 * Aturannya berupa rantai prioritas: pasangan pertama yang lengkap menang, sisanya diabaikan.
	 * <ol>
	 * <li>{@code "P_" + pejabat.id + "_" + suratKeluar.id} bila pejabat tersedia;</li>
	 * <li>{@code "J_" + jenisJabatan.id + "_" + suratKeluar.id} bila hanya jenis jabatan tersedia;</li>
	 * <li>{@code "K_" + konseptor.userId + "_" + <barcode acak> + "_" + suratKeluar.id} bila hanya
	 * konseptor tersedia;</li>
	 * <li>{@code "M_" + mahasiswa.id + "_" + suratKeluar.id};</li>
	 * <li>{@code "S_" + siswa.id + "_" + suratKeluar.id}.</li>
	 * </ol>
	 * Seluruh cabang mensyaratkan {@code suratKeluar} tidak {@code null}; bila surat belum ada,
	 * hasilnya {@code null} &mdash; itulah sebabnya query di lapisan Action dan API selalu
	 * menambahkan {@code Restrictions.isNotNull("kodeUnik")} untuk menyaring baris setengah jadi.
	 * <p>
	 * <b>Cabang konseptor bersifat khusus.</b> Hanya cabang itu yang menyisipkan komponen acak
	 * ({@code Common.getGeneratedBarCode()}), sehingga kunci yang dihasilkan <i>tidak stabil</i>:
	 * memanggil method ini dua kali dengan argumen yang sama menghasilkan dua kunci berbeda.
	 * Akibatnya pencarian "apakah baris untuk konseptor ini sudah ada" lewat kunci unik tidak pernah
	 * menemukan baris lama, dan pencegahan duplikasi untuk baris berbasis konseptor praktis tidak
	 * bekerja. Empat cabang lainnya deterministik dan benar-benar berfungsi sebagai kunci.
	 * <p>
	 * <b>Implikasi model.</b> Karena kunci dibentuk dari (aktor, surat) tanpa menyertakan simpul
	 * jenjang, satu pejabat hanya dapat memiliki SATU baris status per surat &mdash; walaupun
	 * definisi alur menempatkan jabatannya pada dua jenjang berbeda. Jenjang kedua akan memakai
	 * ulang baris jenjang pertama alih-alih membuat baris baru, sehingga rantai persetujuan yang
	 * secara sengaja meminta pejabat yang sama menyetujui dua kali (mis. paraf lalu tanda tangan)
	 * tidak dapat direkam sebagai dua keputusan terpisah.
	 *
	 * @param pejabat      pejabat penindaklanjut (prioritas tertinggi)
	 * @param suratKeluar  surat keluar terkait; wajib ada agar kunci terbentuk
	 * @param jenisJabatan jenis jabatan tahapan
	 * @param konseptor    pengguna pengonsep
	 * @param mahasiswa    mahasiswa pengaju
	 * @param siswa        siswa pengaju
	 * @return kunci unik logis, atau {@code null} bila tidak ada kombinasi yang lengkap
	 */
	public static String kodeUnik(Pejabat pejabat, SuratKeluar suratKeluar, JenisJabatan jenisJabatan,
			Tbmuser konseptor, Mahasiswa mahasiswa, Siswa siswa) {
		String kodeUnik = null;
		if (pejabat != null && suratKeluar != null) {
			kodeUnik = "P_" + pejabat.getId() + "_" + suratKeluar.getId();
		} else if (jenisJabatan != null && suratKeluar != null) {
			kodeUnik = "J_" + jenisJabatan.getId() + "_" + suratKeluar.getId();
		} else if (konseptor != null && suratKeluar != null) {
			kodeUnik = "K_" + konseptor.getUserId() + "_" + Common.getGeneratedBarCode() + "_" + suratKeluar.getId();
		} else if (mahasiswa != null && suratKeluar != null) {
			kodeUnik = "M_" + mahasiswa.getId() + "_" + suratKeluar.getId();
		} else if (siswa != null && suratKeluar != null) {
			kodeUnik = "S_" + siswa.getId() + "_" + suratKeluar.getId();
		}
		return kodeUnik;
	}

	/**
	 * Mengembalikan kunci unik baris status &mdash; <b>nilai turunan yang dihitung ulang setiap kali
	 * getter dipanggil</b>, bukan nilai tersimpan yang dibaca apa adanya.
	 * <p>
	 * Implementasinya memanggil
	 * {@link #kodeUnik(Pejabat, SuratKeluar, JenisJabatan, Tbmuser, Mahasiswa, Siswa)} dengan
	 * seluruh relasi baris ini (lewat getter-getternya, sehingga ikut memicu normalisasi destruktif
	 * dan pemuatan proxy), lalu <b>menulis hasilnya balik</b> ke field {@code kodeUnik}. Karena
	 * kolomnya dideklarasikan {@code unique}, perilaku ini punya beberapa konsekuensi:
	 * <ul>
	 * <li><b>Mengubah pejabat berarti mengubah kunci.</b> Menyetel {@link #setPejabat(Pejabat)} ke
	 * pejabat lain otomatis memindahkan baris ini ke kunci {@code "P_<pejabat baru>_<surat>"}. Bila
	 * kunci itu sudah dipakai baris lain, {@code UPDATE} menabrak batasan unik
	 * {@code alur_persetujuan_surat_keluar_status_kodeunik_key}. Lapisan Action menanganinya dengan
	 * mencari lebih dulu baris yang memakai kunci target dan memakai ulang baris tersebut alih-alih
	 * menulis duplikat &mdash; perilaku yang perlu diingat karena artinya sebuah operasi "ubah
	 * pejabat" dapat diam-diam berpindah menyunting baris yang berbeda dari yang dibuka
	 * pengguna.</li>
	 * <li><b>Nilai tersimpan dapat basi.</b> Kolom di database menyimpan kunci saat penyimpanan
	 * terakhir; bila relasi berubah tanpa penyimpanan, nilai yang dikembalikan getter tidak lagi
	 * sama dengan isi kolom. Query yang mencocokkan kolom {@code kodeUnik} membandingkan nilai
	 * tersimpan, bukan nilai terhitung.</li>
	 * <li><b>Baris tanpa surat tidak punya kunci</b> ({@code null}); seluruh query alur menyaringnya
	 * dengan {@code isNotNull("kodeUnik")}.</li>
	 * <li><b>Cabang konseptor tidak stabil</b> karena mengandung komponen acak &mdash; pembacaan
	 * berulang menghasilkan kunci berbeda.</li>
	 * </ul>
	 * Kunci juga tidak menyertakan simpul jenjang ({@link #getAlurPersetujuanSuratKeluar()}),
	 * sehingga keunikannya per (aktor, surat), bukan per (aktor, surat, jenjang).
	 *
	 * @return kunci unik terhitung, atau {@code null} bila baris belum memiliki surat/aktor yang cukup
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = AlurPersetujuanSuratKeluarStatus.kodeUnik(getPejabat(), getSuratKeluar(), getJenisJabatan(),
				getKonseptor(), getMahasiswa(), getSiswa());
		return kodeUnik;
	}

	/**
	 * Menyetel kunci unik. Nilai yang disetel akan <b>ditimpa</b> pada pembacaan berikutnya lewat
	 * {@link #getKodeUnik()}; setter ini praktis hanya berguna bagi Hibernate saat memuat baris.
	 *
	 * @param kodeUnik kunci unik
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan pengguna pengonsep baris ini &mdash; <b>getter destruktif bersyarat</b>.
	 * <p>
	 * Selain normalisasi biasa lewat {@link GeneralValueObject#check(Object)}, getter ini
	 * <b>mengosongkan</b> field {@code konseptor} menjadi {@code null} bila baris memiliki
	 * {@link #getSiswa()} atau {@link #getMahasiswa()}. Aturannya: pengaju berupa siswa/mahasiswa
	 * saling meniadakan dengan pengonsep berupa pengguna internal, sehingga hanya satu identitas
	 * pengaju tersisa pada baris. Karena pengosongan terjadi pada field, entitas terkelola yang
	 * di-flush setelah pembacaan ini akan benar-benar menuliskan {@code konseptor = NULL} ke
	 * database &mdash; sekadar membaca dapat menghapus relasi.
	 * <p>
	 * Penting untuk audit: tidak ada satu pun pemeriksaan di modul ini yang membandingkan konseptor
	 * dengan {@link #getPejabat()}. Orang yang sama dapat tercatat sebagai pengonsep sekaligus
	 * penyetuju tahapan yang sama, dan pada {@code SuratApi} baris jenjang berikutnya justru sengaja
	 * dibuat dengan konseptor = pemanggil yang baru saja menyetujui jenjang sebelumnya. Tidak ada
	 * pemisahan tugas (segregation of duties) yang ditegakkan di manapun pada alur ini.
	 *
	 * @return pengguna pengonsep, atau {@code null} (termasuk bila baru saja dikosongkan karena ada
	 *         siswa/mahasiswa)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "konseptor", nullable = true)
	public Tbmuser getKonseptor() {
		konseptor = check(konseptor);
		if (getSiswa() != null || getMahasiswa() != null) {
			konseptor = null;
		}
		return konseptor;
	}

	/**
	 * Menyetel pengguna pengonsep. Nilai akan dikosongkan kembali oleh {@link #getKonseptor()} bila
	 * baris juga memiliki siswa/mahasiswa.
	 *
	 * @param konseptor pengguna pengonsep
	 */
	public void setKonseptor(Tbmuser konseptor) {
		this.konseptor = konseptor;
	}

	/**
	 * Mengembalikan mahasiswa pengaju (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}). Kehadirannya membuat {@link #getKonseptor()}
	 * mengosongkan diri.
	 *
	 * @return mahasiswa pengaju, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pengaju.
	 *
	 * @param mahasiswa mahasiswa pengaju
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan siswa pengaju (getter destruktif lewat
	 * {@link GeneralValueObject#check(Object)}). Kehadirannya membuat {@link #getKonseptor()}
	 * mengosongkan diri.
	 *
	 * @return siswa pengaju, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menyetel siswa pengaju.
	 *
	 * @param siswa siswa pengaju
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan bendera "sudah direvisi setelah ditolak", dengan default {@code false}.
	 * <p>
	 * Dinyalakan oleh {@code SuratKeluarAction} ketika konseptor memperbaiki surat yang ditolak:
	 * baris penolakan disetel {@code telahDirevisi = true} sekaligus {@code ditolak = false} dan
	 * {@code disetujui = false}, sehingga tahapan kembali berstatus menunggu. Perender dasbor
	 * memakainya untuk menandai bahwa tahapan pernah ditolak namun sudah ditindaklanjuti.
	 * <p>
	 * Perhatikan bahwa pemulihan status itu <b>menghapus jejak penolakan pada baris</b>: bendera
	 * {@code ditolak} dimatikan dan &mdash; karena {@link #getWaktuDitolak()} mengembalikan
	 * {@code null} saat bendera mati &mdash; stempel waktu penolakan ikut hilang dari tabel utama
	 * saat penyimpanan berikutnya. Riwayat penolakan hanya tersisa pada tabel audit Envers dan pada
	 * {@link #getCatatanRevisi()}.
	 *
	 * @return {@code true} bila tahapan sudah direvisi
	 */
	public Boolean getTelahDirevisi() {
		return telahDirevisi == null ? false : telahDirevisi;
	}

	/**
	 * Menyetel bendera "sudah direvisi setelah ditolak".
	 *
	 * @param telahDirevisi nilai bendera
	 */
	public void setTelahDirevisi(Boolean telahDirevisi) {
		this.telahDirevisi = telahDirevisi;
	}

	/**
	 * Mengembalikan catatan revisi/disposisi yang dipertahankan setelah penolakan. Dipetakan ke
	 * kolom {@code catatan_revisi} meskipun field internalnya bernama {@code catatanDisposisi}.
	 * Dibaca antara lain oleh {@code DasboardSurat} dan {@code DasboardAlurSurat} untuk menampilkan
	 * alasan penolakan.
	 *
	 * @return catatan revisi, atau {@code null}
	 */
	@Column(name = "catatan_revisi", columnDefinition = "text", nullable = true)
	public String getCatatanRevisi() {
		return catatanDisposisi;
	}

	/**
	 * Menyetel catatan revisi/disposisi. Berbeda dari {@link #setKeterangan(String)}, setter ini
	 * menerima nilai kosong apa adanya.
	 *
	 * @param catatanRevisi catatan revisi
	 */
	public void setCatatanRevisi(String catatanRevisi) {
		this.catatanDisposisi = catatanRevisi;
	}

	/**
	 * Menghasilkan (bila belum ada) dan mengembalikan lokasi berkas PNG kode QR "tanda tangan
	 * elektronik" untuk baris persetujuan ini, dipakai templat Jasper sebagai gambar verifikasi pada
	 * surat keluar tercetak.
	 * <p>
	 * Berkas disimpan di direktori laporan dengan nama {@code a_s_k_<id baris>.png} &mdash;
	 * <b>pola nama yang sama persis</b> dengan yang dipakai
	 * {@link AlurPersetujuanSuratMasukStatus#ttdQr()}. Karena id kedua tabel status berjalan
	 * independen, dua baris berbeda (satu dari alur surat masuk, satu dari alur surat keluar) yang
	 * kebetulan ber-id sama akan memperebutkan nama berkas yang sama; siapa pun yang membangkitkan
	 * lebih dulu "menang", dan yang belakangan memakai ulang berkas milik modul lain karena method
	 * ini bersifat <i>cache-on-disk</i> (berkas yang sudah ada tidak pernah dibangkitkan ulang).
	 * Akibatnya QR pada sebuah surat dapat memuat data surat yang sama sekali berbeda.
	 * <p>
	 * Isi kode QR dirangkai sebagai teks multi-baris: nama pejabat, nama jenis jabatan, waktu
	 * persetujuan, waktu penolakan, kode dan perihal surat, waktu surat, nama surat, klasifikasi
	 * surat keluar, identitas mahasiswa/siswa/guru/dosen/pegawai penerima, nama konseptor, serta
	 * jurusan/fakultas/sekolah surat, ditutup alamat host aplikasi
	 * ({@code Common.getRequestHostWithProtocol()}) agar pemindai dapat merujuk kembali ke instalasi
	 * penerbit. Setiap komponen dilewati bila sumbernya kosong, sehingga QR tetap terbentuk pada data
	 * yang tidak lengkap.
	 * <p>
	 * Hal-hal yang penting dipahami sebelum mengandalkan berkas ini sebagai bukti:
	 * <ul>
	 * <li><b>Bukan tanda tangan kriptografis.</b> Isinya teks biasa tanpa tanda tangan digital,
	 * tanpa nomor seri, dan tanpa mekanisme verifikasi balik. Siapa pun yang dapat membaca datanya
	 * dapat menyusun QR dengan isi identik. Nilainya adalah kemudahan pemeriksaan silang manual,
	 * bukan jaminan keaslian.</li>
	 * <li><b>Terbit tanpa memeriksa status.</b> Method tidak memeriksa {@link #getDisetujui()} sama
	 * sekali. Memanggilnya pada baris yang belum disetujui tetap menghasilkan berkas QR; yang kosong
	 * hanyalah bagian waktu persetujuan. Penyaringan "hanya cetak QR untuk tahapan yang disetujui"
	 * sepenuhnya menjadi tanggung jawab templat laporan yang memanggilnya. Ini melengkapi celah pada
	 * jalur gambar tanda tangan ({@code SuratUtil.initGambarTandaTangan}) yang memang menyaring
	 * {@code disetujui = true} namun tanpa memverifikasi keutuhan rantai atau kewenangan
	 * penyetuju.</li>
	 * <li><b>Cache tidak pernah kedaluwarsa.</b> Perubahan pejabat, waktu persetujuan, atau isi
	 * surat setelah QR pertama dibuat TIDAK tercermin pada QR. Sebaliknya, pembersihan direktori
	 * laporan membangkitkan QR baru dengan data terkini &mdash; dua cetakan surat yang sama pada
	 * waktu berbeda dapat memuat QR yang isinya berbeda.</li>
	 * <li><b>Dipanggil dari luar Java.</b> Tidak ada pemanggil method ini di dalam kode Java; ia
	 * merupakan titik ekstensi yang dievaluasi sebagai ekspresi pada templat {@code .jrxml} yang
	 * disimpan di database sebagai lampiran ({@code LampiranLain.FILE_JRXML_LAYOUT_SURAT}). Mengubah
	 * tanda tangan method ini akan merusak templat yang tidak terlihat di repositori.</li>
	 * </ul>
	 * Method mengembalikan lokasi berkas meskipun pembangkitan gagal atau {@link #getSuratKeluar()}
	 * kosong &mdash; pada kasus itu berkasnya tidak ada dan templat menampilkan gambar kosong.
	 *
	 * @return lokasi absolut berkas PNG kode QR untuk baris persetujuan ini
	 */
	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/a_s_k_" + getId() + ".png");
		suratKeluar = getSuratKeluar();
		if (!myfilebarcode.exists() && suratKeluar != null) {
			String code = (getPejabat() == null ? "" : getPejabat().getNama() + "\n")
					+ (getJenisJabatan() == null ? "" : getJenisJabatan().getNama() + "\n")
					+ (getWaktuPersetujuan() == null ? ""
							: Common.dateFormat3.get().format(getWaktuPersetujuan()) + "\n")
					+ (getWaktuDitolak() == null ? "" : Common.dateFormat3.get().format(getWaktuDitolak()) + "\n") +

					(suratKeluar.getKode() == null || suratKeluar.getKode().trim().isEmpty() ? ""
							: suratKeluar.getKode() + "\n")
					+ (suratKeluar.getPerihal() == null || suratKeluar.getPerihal().trim().isEmpty() ? ""
							: suratKeluar.getPerihal() + "\n")
					+ (suratKeluar.getWaktu() == null ? ""
							: Common.dateFormat3.get().format(suratKeluar.getWaktu()) + "\n")
					+ suratKeluar.getNama() + "\n"
					+ (suratKeluar.getKlasifikasiSuratKeluar() == null ? ""
							: suratKeluar.getKlasifikasiSuratKeluar().getNama() + "\n")

					+ (suratKeluar.getMahasiswa() == null ? ""
							: suratKeluar.getMahasiswa().getNim() + " " + suratKeluar.getMahasiswa().getNama() + "\n")
					+ (suratKeluar.getSiswa() == null ? ""
							: suratKeluar.getSiswa().getNomorIndukNasional() + " " + suratKeluar.getSiswa().getNama()
									+ "\n")
					+ (suratKeluar.getGuru() == null ? "" : suratKeluar.getGuru().getNama() + "\n")
					+ (suratKeluar.getDosen() == null ? "" : suratKeluar.getDosen().getNama() + "\n")
					+ (suratKeluar.getPegawai() == null ? "" : suratKeluar.getPegawai().getNama() + "\n")
					+ (getKonseptor() == null ? "" : getKonseptor().getUserNama() + "\n")

					+ (suratKeluar.getJurusan() == null ? "" : suratKeluar.getJurusan().getNama() + "\n")
					+ (suratKeluar.getFakultas() == null ? "" : suratKeluar.getFakultas().getNama() + "\n")
					+ (suratKeluar.getSekolah() == null ? "" : suratKeluar.getSekolah().getNama() + "\n")
					+ Common.getRequestHostWithProtocol();
			BarcodeCommon.generateCRCode(code, myfilebarcode);
		}
		return myfilebarcode.getAbsolutePath();
	}

	/**
	 * Mengembalikan bendera "rantai ditutup di tahapan ini" &mdash; satu-satunya mekanisme
	 * penutupan dini yang resmi ada di modul persuratan, dan sekaligus getter dengan efek samping
	 * paling berdampak pada data historis di kelas ini.
	 * <p>
	 * <b>Perilaku.</b> Sebelum mengembalikan nilai, getter memeriksa simpul definisi alur baris ini
	 * lewat {@link #getAlurPersetujuanSuratKeluar()}. Bila simpul tersebut ada namun
	 * {@link AlurPersetujuanSuratKeluar#getTerdapatPilihanSelesai()} bernilai {@code false} &mdash;
	 * yaitu konfigurasi alur tidak (lagi) mengizinkan penutupan dini &mdash; field {@code selesai}
	 * <b>dipaksa menjadi {@code false}</b>. Barulah nilai dikembalikan, dengan {@code null}
	 * diperlakukan sebagai {@code false}. Bila simpul alurnya {@code null}, pemaksaan dilewati dan
	 * nilai apa adanya yang dikembalikan &mdash; jadi baris "melayang" tanpa simpul alur justru
	 * dapat mempertahankan status selesainya meski tidak ada kebijakan yang menaunginya.
	 * <p>
	 * <b>Konsekuensi pada data.</b> Pemaksaan dilakukan pada <i>field</i>, bukan sekadar pada nilai
	 * kembalian. Pada entitas terkelola, pembacaan yang diikuti flush akan benar-benar menuliskan
	 * {@code selesai = false} ke database. Artinya mematikan kebijakan
	 * {@code terdapatPilihanSelesai} pada sebuah definisi alur <b>membatalkan secara surut</b>
	 * seluruh penandaan "selesai" pada baris status historis yang bergantung pada simpul itu, secara
	 * bertahap seiring baris-baris tersebut dibaca aplikasi. Menyalakan kembali kebijakan tidak
	 * memulihkan nilai yang sudah terlanjur tertulis {@code false}: penutupan dini yang dulu sah
	 * hilang tanpa jejak pada tabel utama, dan hanya dapat ditelusuri lewat tabel audit Envers.
	 * Perubahan konfigurasi master karena itu bukan operasi yang netral terhadap riwayat transaksi.
	 * <p>
	 * <b>Hubungan dengan bendera lain.</b> Bendera ini tidak menggantikan {@link #getDisetujui()}
	 * dan tidak diperiksa oleh mesin pembentuk jenjang berikutnya. Pada form ZK, mencentang "Selesai
	 * sampai di sini" ikut memaksa kotak "Persetujuan" tercentang, sehingga tahapan yang ditutup
	 * dini biasanya juga bertanda disetujui &mdash; tetapi itu perilaku pendengar {@code onCheck},
	 * bukan invarian yang ditegakkan entitas. Jalur non-UI dapat menyimpan {@code selesai = true}
	 * dengan {@code disetujui = false} tanpa hambatan. Baris tersebut akan terbaca "sudah dilewati"
	 * oleh perender dasbor namun tidak akan pernah dianggap disetujui oleh
	 * {@code SuratUtil.initGambarTandaTangan}.
	 * <p>
	 * <b>Cakupan pemakaian.</b> Bendera hanya dibaca di dalam kelas ini dan pada perender dasbor
	 * ({@code DasboardSurat}, {@code DasboardAlurSurat}) untuk menentukan lencana status dan apakah
	 * sebuah tahapan sudah dilewati. Tidak ada konsumen di luar itu &mdash; khususnya tidak ada kode
	 * yang memakainya sebagai gerbang untuk mengizinkan surat dikirim, dicetak, atau dianggap sah.
	 * Sama seperti {@link #getDisetujui()}, bendera ini adalah data biasa, bukan hasil keputusan
	 * berwenang yang terverifikasi.
	 *
	 * @return {@code true} bila tahapan ini menutup rantai persetujuan; tidak pernah {@code null}
	 */
	public Boolean getSelesai() {

		if (getAlurPersetujuanSuratKeluar() != null && !getAlurPersetujuanSuratKeluar().getTerdapatPilihanSelesai()) {
			selesai = false;
		}

		return selesai == null ? false : selesai;
	}

	/**
	 * Menyetel bendera "rantai ditutup di tahapan ini". Nilai {@code true} dapat dibatalkan kembali
	 * oleh {@link #getSelesai()} bila simpul alur tidak mengizinkan penutupan dini.
	 *
	 * @param selesai nilai bendera
	 */
	public void setSelesai(Boolean selesai) {
		this.selesai = selesai;
	}
}
