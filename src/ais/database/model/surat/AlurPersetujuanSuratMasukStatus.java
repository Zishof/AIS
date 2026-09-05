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
 * Entitas <b>status/riwayat</b> satu tahapan persetujuan (disposisi) untuk satu <b>surat masuk</b>
 * tertentu. Dipetakan ke tabel {@code surat.alur_persetujuan_surat_masuk_status}.
 *
 * <h2>Kedudukan dalam modul persuratan</h2>
 * Sementara {@link AlurPersetujuanSuratMasuk} adalah <i>definisi</i> jenjang (template pohon jenis
 * jabatan), kelas ini adalah <i>realisasi</i>-nya: satu baris = satu (surat masuk &times; jenjang
 * alur &times; pejabat) beserta keputusannya. Baris tidak dibuat sekaligus di awal, melainkan
 * "malas": jenjang pertama dibuat saat surat disimpan
 * ({@code ais.action.master.surat.SuratMasukAction}), dan jenjang berikutnya baru dibuat ketika
 * jenjang sebelumnya ditandai disetujui &mdash; oleh
 * {@code ais.action.master.surat.AlurPersetujuanSuratMasukStatusAction} (jalur ZK) atau
 * {@code ais.action.servlet.api.SuratApi#disposisi_surat_masuk} (jalur API mobile).
 *
 * <h2>Model status: tiga bendera yang tidak saling mengunci</h2>
 * Status satu tahapan dibaca dari kombinasi bendera:
 * <ul>
 * <li>{@link #getDisetujui()} &mdash; tahapan disetujui;</li>
 * <li>{@link #getDitolak()} &mdash; tahapan ditolak;</li>
 * <li>{@link #getTelahDirevisi()} &mdash; berkas sudah direvisi setelah penolakan, sehingga tahapan
 * dibuka kembali;</li>
 * <li>{@link #getMasihLanjut()} &mdash; masih ada jenjang berikutnya setelah tahapan ini.</li>
 * </ul>
 * Ketiga bendera keputusan disimpan sebagai kolom {@code boolean} terpisah dan <b>tidak ada
 * invarian pada level entitas</b> yang mencegah kombinasi mustahil seperti
 * {@code disetujui = true} sekaligus {@code ditolak = true}. Saling-eksklusi hanya diusahakan oleh
 * pendengar {@code onCheck} pada kotak centang ZK &mdash; yaitu logika presentasi, bukan invarian
 * data. Konsumen (mis. {@code DasboardSurat}, {@code DasboardAlurSurat}) karena itu membaca bendera
 * dengan urutan prioritas ("ditolak dulu, baru disetujui"), bukan dengan asumsi hanya satu bendera
 * yang menyala.
 *
 * <h2>Tidak ada konsep "seluruh rantai disetujui"</h2>
 * Tidak ada method di kelas ini, di {@link SuratMasuk}, maupun di helper persuratan yang menghitung
 * apakah SELURUH tahapan pada rantai alur sebuah surat sudah disetujui. Yang ada hanyalah:
 * <ul>
 * <li>{@code SuratMasuk.getAlurDitolak()} &mdash; penunjuk tunggal ke baris yang menolak (diisi
 * {@code DasboardSurat.tolak}); dan</li>
 * <li>pembacaan per-baris pada perender dasbor.</li>
 * </ul>
 * Konsekuensinya, "selesai/final" di modul ini adalah <b>kesimpulan visual</b> dari rangkaian
 * lencana per tahapan, bukan status terhitung yang bisa diandalkan sebagai gerbang. Kode yang ingin
 * memastikan sebuah surat benar-benar melewati seluruh jenjang harus menelusuri sendiri pohon
 * {@link AlurPersetujuanSuratMasuk} dan mencocokkannya dengan baris-baris kelas ini.
 *
 * <h2>PERINGATAN KEAMANAN &mdash; gerbang persetujuan hanya mengontrol tampilan</h2>
 * Hasil penelusuran kode pada jalur mutasi data:
 * <ol>
 * <li><b>Jalur dasbor ZK.</b> {@code DasboardSurat} memanggil
 * {@code bolehAksesAlurBerdasarkanLoginV20(pejabat, jenisJabatan)} untuk memutuskan apakah tombol
 * "Tindak Lanjuti"/"Ubah" <i>dirender</i>. Tombol itu memanggil
 * {@code AlurPersetujuanSuratMasukStatusAction.onAddExternal(...)}, dan {@code onSave()} pada Action
 * tersebut menuliskan {@link #setDisetujui(Boolean)}/{@link #setDitolak(Boolean)}/
 * {@link #setPejabat(Pejabat)} <b>tanpa memanggil ulang pemeriksaan apa pun</b>. Action yang sama
 * juga terdaftar sebagai menu mandiri (lihat {@code MainAction}/{@code MainAction2}), sehingga
 * dapat dicapai tanpa melewati dasbor sama sekali. Ini persis pola "gerbang APPROVE hanya kontrol
 * visibilitas UI" yang sudah dikonfirmasi kritis di modul kepegawaian.</li>
 * <li><b>Jalur API mobile.</b> {@code SuratApi.disposisi_surat_masuk} hanya memvalidasi token
 * ({@code ApiUtil.currentUser}) lalu memuat baris ini dari {@code alurId} yang dikirim pemanggil,
 * kemudian menyetel {@code disetujui}/{@code ditolak} sesuai isi permintaan. Tidak ada pemeriksaan
 * bahwa pemanggil adalah {@link #getPejabat()} baris tersebut, tidak ada pemeriksaan jenis jabatan,
 * dan tidak ada pemeriksaan bahwa jenjang induk sudah disetujui &mdash; sehingga jenjang terakhir
 * dapat disetujui langsung, melompati seluruh jenjang di atasnya.</li>
 * <li><b>Jejak audit dapat menyesatkan.</b> Pada jalur API, {@code setPejabat(pejabatcurrent)} hanya
 * dijalankan bila pemanggil memang terdaftar sebagai {@link Pejabat}. Bila tidak, field
 * {@code pejabat} tetap berisi pejabat asli, sehingga baris tersimpan seolah-olah pejabat yang
 * berwenanglah yang menyetujui. Satu-satunya jejak pelaku sebenarnya adalah field audit bayangan
 * {@link #getOleh()}/{@link #getOlehId()}.</li>
 * <li><b>Tidak ada pemisahan pengusul vs penyetuju.</b> {@link #getKonseptor()} (pengonsep) dan
 * {@link #getPejabat()} (penyetuju) hidup pada baris yang sama tanpa satu pun pemeriksaan bahwa
 * keduanya berbeda orang. Pada {@code SuratApi}, baris jenjang berikutnya justru dibuat dengan
 * {@code setKonseptor(tbmuser)} &mdash; pemanggil yang baru saja menyetujui dicatat sebagai
 * konseptor jenjang selanjutnya.</li>
 * </ol>
 * Dampak hilirnya nyata pada dokumen tercetak: {@code SuratUtil.initGambarTandaTangan} menempelkan
 * <b>gambar tanda tangan</b> pejabat ke berkas cetak untuk setiap baris status dengan
 * {@code disetujui = true}, tanpa memeriksa keutuhan rantai. Menyalakan satu bendera pada baris yang
 * tepat sudah cukup untuk memunculkan tanda tangan pejabat pada surat.
 *
 * <h2>Catatan arsitektur lain</h2>
 * <ul>
 * <li><b>Getter destruktif</b> di hampir semua relasi (pola {@code field = check(field)}), plus dua
 * getter yang jauh lebih agresif: {@link #getKonseptor()} (mengosongkan konseptor bila baris
 * menunjuk siswa/mahasiswa) dan {@link #getKodeUnik()} (menghitung ulang kunci unik setiap
 * pembacaan).</li>
 * <li><b>Getter yang memfabrikasi nilai</b>: {@link #getWaktuPersetujuan()} dan
 * {@link #getWaktuDitolak()} mengembalikan {@code null} bila bendera terkait mati, dan
 * mengembalikan "sekarang" bila bendera menyala tetapi kolom masih kosong.</li>
 * <li><b>Field audit bayangan</b> {@code oleh}/{@code olehId}/{@code tanggal_dirubah} &mdash;
 * keharusan teknis interseptor audit, bukan duplikasi yang perlu dibersihkan.</li>
 * <li><b>Nama field vs nama kolom tidak sejalan</b> pada catatan revisi: field internal bernama
 * {@code catatanDisposisi}, propertinya {@code catatanRevisi}, kolomnya {@code catatan_revisi}.</li>
 * </ul>
 *
 * @see AlurPersetujuanSuratMasuk
 * @see AlurPersetujuanSuratKeluarStatus
 * @see SuratMasuk
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "surat", name = "alur_persetujuan_surat_masuk_status")
public class AlurPersetujuanSuratMasukStatus extends GeneralValueObject {

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
	 * Mengembalikan stempel waktu perubahan terakhir, dengan materialisasi default: bila field
	 * masih {@code null} (mis. entitas hasil deserialisasi atau baris lama), field diisi waktu
	 * sekarang lalu dikembalikan. Getter ini karena itu tidak pernah mengembalikan {@code null},
	 * namun juga bukan operasi baca murni.
	 * <p>
	 * Nilai ini dipakai sebagai cadangan oleh {@link #getWaktuDitolak()} ketika sebuah penolakan
	 * belum memiliki stempel waktunya sendiri.
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		if (tanggal_dirubah == null) {
			tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal_dirubah;
	}

	/** Surat masuk yang tahapannya dicatat baris ini. */
	private SuratMasuk suratMasuk;

	/** Simpul definisi alur (jenjang) yang direalisasikan baris ini. */
	private AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk;

	/** Keterangan/catatan disposisi yang ditulis penindaklanjut. */
	private String keterangan;

	/** Bendera "tahapan disetujui". Default {@code false}. */
	private Boolean disetujui = false;

	/** Bendera "tahapan ditolak". Default {@code false}. Tidak saling mengunci dengan disetujui. */
	private Boolean ditolak = false;

	/** Pejabat yang tercatat sebagai penindaklanjut/penyetuju tahapan ini. */
	private Pejabat pejabat;

	/** Stempel waktu persetujuan; hanya bermakna bila {@link #disetujui} menyala. */
	private Date waktuPersetujuan;

	/** Jenis jabatan tahapan; diturunkan dari pejabat bila tersedia. */
	private JenisJabatan jenisJabatan;

	/** Bendera "masih ada jenjang berikutnya". {@code null} dibaca sebagai {@code true}. */
	private Boolean masihLanjut;

	/** Peta JSON pejabat tujuan disposisi lanjutan (dipakai form dan BroadcastHelper). */
	private String jenisSurats;

	/** Kunci unik turunan (bukan masukan pengguna) &mdash; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Siswa pengaju (bila surat berasal dari layanan siswa). */
	private Siswa siswa;

	/** Mahasiswa pengaju (bila surat berasal dari layanan mahasiswa). */
	private Mahasiswa mahasiswa;

	/** Pengguna pengonsep baris ini. Tidak pernah diadu dengan {@link #pejabat}. */
	private Tbmuser konseptor;

	/** Bendera "sudah direvisi setelah ditolak". */
	private Boolean telahDirevisi;

	/**
	 * Catatan revisi/disposisi. Perhatikan ketidakselarasan penamaan: field {@code catatanDisposisi},
	 * properti {@code catatanRevisi}, kolom {@code catatan_revisi}.
	 */
	private String catatanDisposisi;

	/** Stempel waktu penolakan; hanya bermakna bila {@link #ditolak} menyala. */
	private Date waktuDitolak;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public AlurPersetujuanSuratMasukStatus() {
	}

	/**
	 * Mengembalikan kunci utama baris status.
	 * <p>
	 * Id inilah yang dikirim klien sebagai {@code alurId} ke
	 * {@code SuratApi.disposisi_surat_masuk}; lihat peringatan keamanan pada dokumentasi kelas
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
	 * Mengembalikan surat masuk yang tahapannya dicatat baris ini.
	 * <p>
	 * Berbeda dengan relasi lain di kelas ini, getter ini TIDAK memanggil
	 * {@link GeneralValueObject#check(Object)} &mdash; nilai dikembalikan apa adanya. Relasi
	 * memakai {@code FetchMode.SELECT} agar surat dimuat lewat query terpisah, menghindari join
	 * besar saat daftar status dirender.
	 *
	 * @return surat masuk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_masuk", nullable = true)
	public SuratMasuk getSuratMasuk() {
		return suratMasuk;
	}

	/**
	 * Menyetel surat masuk pemilik baris status.
	 *
	 * @param suratMasuk surat masuk
	 */
	public void setSuratMasuk(SuratMasuk suratMasuk) {
		this.suratMasuk = suratMasuk;
	}

	/**
	 * Mengembalikan simpul definisi alur (jenjang) yang direalisasikan baris ini, setelah
	 * dinormalisasi lewat {@link GeneralValueObject#check(Object)} (getter destruktif).
	 * <p>
	 * Relasi inilah yang dipakai mesin alur untuk mencari jenjang berikutnya: anak-anak dari simpul
	 * ini pada pohon {@link AlurPersetujuanSuratMasuk}. Bila relasi ini {@code null} &mdash; yang
	 * dimungkinkan karena kolomnya {@code nullable} &mdash; baris status menjadi "melayang": ia
	 * tetap tampil pada dasbor dan tetap dapat disetujui, tetapi tidak pernah menurunkan jenjang
	 * berikutnya karena pencarian anak tidak punya titik awal.
	 *
	 * @return simpul definisi alur, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_masuk", nullable = true)
	public AlurPersetujuanSuratMasuk getAlurPersetujuanSuratMasuk() {
		alurPersetujuanSuratMasuk = check(alurPersetujuanSuratMasuk);
		return alurPersetujuanSuratMasuk;
	}

	/**
	 * Menyetel simpul definisi alur yang direalisasikan baris ini.
	 *
	 * @param alurPersetujuanSuratMasuk simpul definisi alur
	 */
	public void setAlurPersetujuanSuratMasuk(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk) {
		this.alurPersetujuanSuratMasuk = alurPersetujuanSuratMasuk;
	}

	/**
	 * Mengembalikan bendera persetujuan tahapan ini &mdash; satu-satunya penanda bahwa jenjang ini
	 * dianggap lolos.
	 * <p>
	 * Getter melakukan materialisasi default: bila field masih {@code null} (baris lama sebelum
	 * kolom diperkenalkan, atau entitas hasil deserialisasi), field diisi {@code false} lalu
	 * dikembalikan. Karena penulisan terjadi pada field, entitas terkelola yang di-flush setelah
	 * pembacaan ini akan ikut menuliskan {@code false} ke kolom &mdash; artinya sekadar membaca
	 * status dapat mematerialkan nilai di database. Untuk baris berstatus persetujuan, materialisasi
	 * "aman ke arah tidak disetujui" ini memang arah yang benar (fail-closed).
	 * <p>
	 * <b>Semantik dan jangkauan bendera ini.</b> Bendera bekerja per baris, dan satu baris hanya
	 * mewakili satu pasangan (jenjang &times; pejabat) untuk satu surat. Tidak ada satu pun tempat
	 * di codebase yang mengagregasi bendera ini menjadi "surat sudah disetujui seluruhnya".
	 * Konsumennya membaca per baris:
	 * <ul>
	 * <li>{@code DasboardSurat}/{@code DasboardAlurSurat} merender lencana per tahapan
	 * (Disetujui/Ditolak/Menunggu) dan menentukan tahapan mana yang masih "pending"
	 * ({@code !disetujui && !ditolak});</li>
	 * <li>{@code SuratApi.disposisi_surat_masuk} memakainya sebagai pemicu pembentukan baris jenjang
	 * berikutnya;</li>
	 * <li>pada padanan surat keluar, {@code SuratUtil.initGambarTandaTangan} menyaring baris dengan
	 * {@code disetujui = true} untuk menempelkan gambar tanda tangan pejabat pada dokumen tercetak.</li>
	 * </ul>
	 * <b>Konsekuensi keamanan.</b> Karena tidak ada agregasi rantai, tidak ada pula pemeriksaan
	 * bahwa jenjang induk sudah disetujui sebelum jenjang anak boleh menyala. Baris jenjang mana pun
	 * yang sudah terwujud di tabel dapat langsung ditandai disetujui, dan konsumen di atas akan
	 * menghormatinya. Pada jalur API, baris dipilih semata-mata dari {@code alurId} kiriman klien
	 * tanpa pencocokan dengan identitas pemanggil; pada jalur ZK, pemeriksaan kewenangan
	 * ({@code bolehAksesAlurBerdasarkanLoginV20}) hanya menentukan apakah tombol tindak lanjut
	 * dirender, sedangkan {@code onSave()} menulis bendera ini tanpa memeriksa apa pun. Dengan kata
	 * lain bendera ini adalah <i>data biasa</i>, bukan hasil keputusan berwenang yang terverifikasi.
	 * <p>
	 * Pembaca yang membangun fitur baru di atas modul ini sebaiknya tidak memperlakukan
	 * {@code getDisetujui() == true} sebagai bukti otorisasi. Bila diperlukan gerbang yang benar,
	 * pemeriksaan harus dilakukan di titik mutasi (memastikan pengguna saat ini benar-benar
	 * {@link #getPejabat()} baris tersebut atau memegang {@link #getJenisJabatan()} yang sesuai,
	 * dan memastikan seluruh jenjang induk sudah disetujui), bukan di titik render.
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
	 * Setter ini menerima nilai apa adanya &mdash; termasuk {@code null}, dan termasuk {@code true}
	 * bersamaan dengan {@link #setDitolak(Boolean)} {@code true} pada objek yang sama. Tidak ada
	 * validasi kewenangan, tidak ada pencatatan siapa yang menyalakan bendera (itu tugas field audit
	 * bayangan {@link #getOleh()}), dan tidak ada pemeriksaan bahwa jenjang induk sudah lolos.
	 * Seluruh penjagaan yang ada saat ini hidup di lapisan presentasi (pendengar {@code onCheck}
	 * kotak centang ZK yang saling menonaktifkan) sehingga tidak berlaku bagi jalur API maupun bagi
	 * kode server lain yang memanggil setter ini langsung.
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
	 * ({@code pejabat.getAttribute("pejabat")}) sehingga pengguna yang membuka form dapat memilih
	 * pejabat mana pun; pada jalur API nilainya ditimpa dengan pejabat milik pemanggil hanya bila
	 * pemanggil memang terdaftar sebagai pejabat, dan dibiarkan apa adanya bila tidak.
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
	 * Perhatikan bahwa mengubah pejabat juga mengubah nilai turunan {@link #getKodeUnik()}, karena
	 * kunci unik dibentuk dari pasangan (pejabat, surat). Perubahan pejabat pada baris yang sudah
	 * tersimpan karena itu dapat menabrak batasan unik baris lain; lapisan Action menanganinya
	 * dengan mencari baris yang sudah memakai kunci target lalu memakai ulang baris tersebut.
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
	 * <li>bila {@code disetujui} bernilai {@code true} dan kolom masih kosong, dikembalikan waktu
	 * <i>sekarang</i> ({@link WaktuUtil#getDate()}) &mdash; nilai yang difabrikasi saat pembacaan,
	 * berubah setiap kali dipanggil sampai ada penyimpanan;</li>
	 * <li>bila {@code disetujui} bernilai {@code false}, dikembalikan {@code null} meskipun kolom
	 * berisi stempel lama.</li>
	 * </ul>
	 * Karena getter inilah yang dibaca Hibernate saat menyusun {@code INSERT}/{@code UPDATE}
	 * (pemetaan berbasis properti), perilaku di atas berdampak langsung ke data tersimpan: mencabut
	 * persetujuan sebuah tahapan akan <b>menghapus</b> stempel waktu persetujuan sebelumnya menjadi
	 * {@code NULL}, sehingga riwayat kapan tahapan itu pernah disetujui hilang dari tabel utama
	 * (hanya tersisa pada tabel audit Envers). Sebaliknya, menyalakan persetujuan tanpa mengisi
	 * stempel akan diam-diam merekam waktu penyimpanan sebagai waktu persetujuan.
	 * <p>
	 * Perhatikan pula perbedaan halus dengan padanannya di modul surat keluar: di sini pemeriksaan
	 * memakai field {@code disetujui} secara langsung ({@code if (disetujui)}) alih-alih memanggil
	 * {@link #getDisetujui()}. Bila field masih {@code null} &mdash; kondisi yang mungkin pada baris
	 * lama atau objek yang belum pernah dibaca lewat getter-nya &mdash; pembukaan kotak {@code Boolean}
	 * akan melempar {@code NullPointerException}. Memanggil {@link #getDisetujui()} lebih dulu
	 * mematerialkan {@code false} dan menghilangkan risiko itu, tetapi urutan pemanggilan tersebut
	 * tidak dijamin oleh apa pun.
	 *
	 * @return waktu persetujuan, atau {@code null} bila tahapan tidak bertanda disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_persetujuan", nullable = true)
	public Date getWaktuPersetujuan() {
		if (disetujui) {
			return waktuPersetujuan == null ? WaktuUtil.getDate() : waktuPersetujuan;
		} else {
			return null;
		}

	}

	/**
	 * Menyetel stempel waktu persetujuan. Nilai yang disimpan hanya akan terlihat kembali selama
	 * bendera {@link #getDisetujui()} menyala &mdash; lihat {@link #getWaktuPersetujuan()}.
	 *
	 * @param waktuPersetujuan waktu persetujuan
	 */
	public void setWaktuPersetujuan(Date waktuPersetujuan) {
		this.waktuPersetujuan = waktuPersetujuan;
	}

	/**
	 * Mengembalikan waktu penolakan tahapan, diturunkan dari bendera dengan pola yang sama seperti
	 * {@link #getWaktuPersetujuan()}: {@code null} bila tahapan tidak bertanda ditolak, dan
	 * &mdash; bila bertanda ditolak namun kolom kosong &mdash; nilai cadangan
	 * {@link #getTanggal_dirubah()} (bukan waktu sekarang, berbeda dari sisi persetujuan).
	 * <p>
	 * Berbeda dengan {@link #getWaktuPersetujuan()}, di sini pemeriksaan memakai
	 * {@link #getDitolak()} sehingga aman terhadap field {@code null}.
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
	 * <p>
	 * Catatan: pada jalur API surat masuk ({@code SuratApi.disposisi_surat_masuk}) pemanggilan
	 * setter ini untuk kasus penolakan justru dikomentari, sehingga stempel penolakan hanya terisi
	 * lewat nilai cadangan pada {@link #getWaktuDitolak()}.
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
	 * Getter ini karena itu bersifat destruktif ganda: ia dapat mengganti jenis jabatan yang
	 * sengaja disimpan pada baris dengan jenis jabatan pejabat terkini. Bila seorang pejabat
	 * dipindah ke jabatan lain, membaca baris status lama akan menampilkan (dan, setelah flush,
	 * menyimpan) jabatan barunya &mdash; riwayat "disetujui oleh siapa dalam kapasitas apa" ikut
	 * berubah surut.
	 * <p>
	 * Nilai ini juga ikut membentuk {@link #getKodeUnik()} pada kasus baris yang tidak memiliki
	 * pejabat, dan dipakai {@code bolehAksesAlurBerdasarkanLoginV20} pada dasbor untuk menentukan
	 * apakah pengguna yang sedang masuk berhak melihat tombol tindak lanjut.
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
	 * Bendera ini diisi oleh mesin alur saat membuat baris jenjang berikutnya dan dibaca
	 * {@code AlurPersetujuanSuratMasukStatusAction} untuk menentukan apakah bagian pemilihan
	 * disposisi lanjutan perlu ditampilkan. Default "masih lanjut" berarti baris yang belum
	 * ditentukan akan diperlakukan seolah rantai belum berakhir.
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
	 * Mengembalikan peta JSON pejabat tujuan disposisi lanjutan yang dipilih pada tahapan ini.
	 * Bila kosong, dikembalikan {@code Pertangungjawaban.DEFAULT_FORMULA} &mdash; konstanta JSON
	 * kosong yang dipinjam dari modul akunting agar pemanggil selalu dapat mem-parse hasilnya
	 * dengan {@code new JSONObject(...)} tanpa penjagaan tambahan.
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
	 * Bila menyala, {@code DasboardSurat.tolak(...)} akan menyimpan baris ini sebagai
	 * {@code SuratMasuk.alurDitolak} &mdash; penunjuk tunggal ke penolakan terakhir pada surat.
	 * Karena penunjuknya tunggal, penolakan sebelumnya pada surat yang sama tidak lagi terlihat dari
	 * sisi {@link SuratMasuk} (riwayatnya tetap ada sebagai baris-baris kelas ini).
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
	 * Membentuk kunci unik logis sebuah baris status dari kombinasi aktor dan surat.
	 * <p>
	 * Aturannya berupa rantai prioritas: pasangan pertama yang lengkap menang, sisanya diabaikan.
	 * <ol>
	 * <li>{@code "P_" + pejabat.id + "_" + suratMasuk.id} bila pejabat tersedia;</li>
	 * <li>{@code "J_" + jenisJabatan.id + "_" + suratMasuk.id} bila hanya jenis jabatan tersedia;</li>
	 * <li>{@code "K_" + konseptor.userId + "_" + <barcode acak> + "_" + suratMasuk.id} bila hanya
	 * konseptor tersedia;</li>
	 * <li>{@code "M_" + mahasiswa.id + "_" + suratMasuk.id};</li>
	 * <li>{@code "S_" + siswa.id + "_" + suratMasuk.id}.</li>
	 * </ol>
	 * Seluruh cabang mensyaratkan {@code suratMasuk} tidak {@code null}; bila surat belum ada, hasilnya
	 * {@code null} &mdash; itulah sebabnya query di lapisan Action dan API selalu menambahkan
	 * {@code Restrictions.isNotNull("kodeUnik")} untuk menyaring baris setengah jadi.
	 * <p>
	 * <b>Cabang konseptor bersifat khusus.</b> Hanya cabang itu yang menyisipkan komponen acak
	 * ({@code Common.getGeneratedBarCode()}), sehingga kunci yang dihasilkan <i>tidak stabil</i>:
	 * memanggil method ini dua kali dengan argumen yang sama menghasilkan dua kunci berbeda. Akibatnya
	 * pencarian "apakah baris untuk konseptor ini sudah ada" lewat kunci unik tidak pernah menemukan
	 * baris lama, dan pencegahan duplikasi untuk baris berbasis konseptor praktis tidak bekerja.
	 * Empat cabang lainnya deterministik dan benar-benar berfungsi sebagai kunci.
	 * <p>
	 * <b>Implikasi model.</b> Karena kunci dibentuk dari (aktor, surat) tanpa menyertakan simpul
	 * jenjang, satu pejabat hanya dapat memiliki SATU baris status per surat &mdash; walaupun
	 * definisi alur menempatkan jabatannya pada dua jenjang berbeda. Jenjang kedua akan memakai ulang
	 * baris jenjang pertama alih-alih membuat baris baru, sehingga rantai persetujuan yang secara
	 * sengaja meminta pejabat yang sama menyetujui dua kali tidak dapat direkam.
	 *
	 * @param pejabat      pejabat penindaklanjut (prioritas tertinggi)
	 * @param suratMasuk   surat masuk terkait; wajib ada agar kunci terbentuk
	 * @param jenisJabatan jenis jabatan tahapan
	 * @param konseptor    pengguna pengonsep
	 * @param mahasiswa    mahasiswa pengaju
	 * @param siswa        siswa pengaju
	 * @return kunci unik logis, atau {@code null} bila tidak ada kombinasi yang lengkap
	 */
	public static String kodeUnik(Pejabat pejabat, SuratMasuk suratMasuk, JenisJabatan jenisJabatan, Tbmuser konseptor,
			Mahasiswa mahasiswa, Siswa siswa) {
		String kodeUnik = null;
		if (pejabat != null && suratMasuk != null) {
			kodeUnik = "P_" + pejabat.getId() + "_" + suratMasuk.getId();
		} else if (jenisJabatan != null && suratMasuk != null) {
			kodeUnik = "J_" + jenisJabatan.getId() + "_" + suratMasuk.getId();
		} else if (konseptor != null && suratMasuk != null) {
			kodeUnik = "K_" + konseptor.getUserId() + "_" + Common.getGeneratedBarCode() + "_" + suratMasuk.getId();
		} else if (mahasiswa != null && suratMasuk != null) {
			kodeUnik = "M_" + mahasiswa.getId() + "_" + suratMasuk.getId();
		} else if (siswa != null && suratMasuk != null) {
			kodeUnik = "S_" + siswa.getId() + "_" + suratMasuk.getId();
		}
		return kodeUnik;
	}

	/**
	 * Mengembalikan kunci unik baris status &mdash; <b>nilai turunan yang dihitung ulang setiap kali
	 * getter dipanggil</b>, bukan nilai tersimpan yang dibaca apa adanya.
	 * <p>
	 * Implementasinya memanggil {@link #kodeUnik(Pejabat, SuratMasuk, JenisJabatan, Tbmuser, Mahasiswa, Siswa)}
	 * dengan seluruh relasi baris ini (lewat getter-getternya, sehingga ikut memicu normalisasi
	 * destruktif dan pemuatan proxy), lalu <b>menulis hasilnya balik</b> ke field {@code kodeUnik}.
	 * Karena kolomnya dideklarasikan {@code unique}, perilaku ini punya beberapa konsekuensi yang
	 * perlu dipahami sebelum menyentuh kode di sekitarnya:
	 * <ul>
	 * <li><b>Mengubah pejabat berarti mengubah kunci.</b> Menyetel {@link #setPejabat(Pejabat)} ke
	 * pejabat lain otomatis memindahkan baris ini ke kunci {@code "P_<pejabat baru>_<surat>"}. Bila
	 * kunci itu sudah dipakai baris lain, {@code UPDATE} akan menabrak batasan unik
	 * {@code alur_persetujuan_surat_masuk_status_kodeunik_key}. Lapisan Action menanganinya dengan
	 * mencari lebih dulu baris yang memakai kunci target dan memakai ulang baris tersebut alih-alih
	 * menulis duplikat.</li>
	 * <li><b>Nilai tersimpan dapat basi.</b> Kolom di database menyimpan kunci saat penyimpanan
	 * terakhir; bila relasi berubah tanpa penyimpanan, nilai yang dikembalikan getter tidak lagi
	 * sama dengan isi kolom. Query yang mencocokkan kolom {@code kodeUnik} membandingkan nilai
	 * tersimpan, bukan nilai terhitung.</li>
	 * <li><b>Baris tanpa surat tidak punya kunci.</b> Hasilnya {@code null}; seluruh query alur
	 * menyaringnya dengan {@code isNotNull("kodeUnik")} agar baris setengah jadi tidak ikut
	 * terproses.</li>
	 * <li><b>Cabang konseptor tidak stabil.</b> Bila baris tidak punya pejabat dan tidak punya jenis
	 * jabatan tetapi punya konseptor, kunci mengandung komponen acak sehingga berubah pada setiap
	 * pembacaan &mdash; pembacaan berulang dapat menghasilkan kunci berbeda dan penyimpanan berulang
	 * menghasilkan baris yang seolah-olah selalu baru.</li>
	 * </ul>
	 * Perhatikan juga bahwa kunci tidak menyertakan simpul jenjang
	 * ({@link #getAlurPersetujuanSuratMasuk()}), sehingga keunikannya adalah per (aktor, surat), bukan
	 * per (aktor, surat, jenjang) &mdash; lihat pembahasan pada
	 * {@link #kodeUnik(Pejabat, SuratMasuk, JenisJabatan, Tbmuser, Mahasiswa, Siswa)}.
	 *
	 * @return kunci unik terhitung, atau {@code null} bila baris belum memiliki surat/aktor yang cukup
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(getPejabat(), getSuratMasuk(), getJenisJabatan(),
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
	 * pengaju yang tersisa pada baris. Karena pengosongan terjadi pada field, entitas terkelola yang
	 * di-flush setelah pembacaan ini akan benar-benar menuliskan {@code konseptor = NULL} ke
	 * database &mdash; sekadar membaca dapat menghapus relasi.
	 * <p>
	 * Perlu dicatat untuk audit: tidak ada satu pun pemeriksaan di modul ini yang membandingkan
	 * konseptor dengan {@link #getPejabat()}. Orang yang sama dapat tercatat sebagai pengonsep
	 * sekaligus penyetuju tahapan yang sama, dan pada {@code SuratApi} baris jenjang berikutnya
	 * justru sengaja dibuat dengan konseptor = pemanggil yang baru saja menyetujui jenjang
	 * sebelumnya.
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
	 * Bendera dinyalakan oleh {@code SuratMasukAction} ketika konseptor memperbaiki surat yang
	 * ditolak: baris penolakan disetel {@code telahDirevisi = true} sekaligus
	 * {@code ditolak = false} dan {@code disetujui = false}, sehingga tahapan kembali berstatus
	 * menunggu. Perender dasbor memakainya untuk menandai bahwa tahapan pernah ditolak namun sudah
	 * ditindaklanjuti.
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
	 * Dibaca antara lain oleh {@code DasboardSurat}, {@code DasboardAlurSurat}, dan
	 * {@code DasboardSuratMasuk} untuk menampilkan alasan penolakan.
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
	 * elektronik" untuk baris persetujuan ini, lalu dipakai templat Jasper sebagai gambar verifikasi
	 * pada surat tercetak.
	 * <p>
	 * Berkas disimpan di direktori laporan dengan nama {@code a_s_k_<id baris>.png}. Method bersifat
	 * <i>cache-on-disk</i>: bila berkas dengan nama itu sudah ada, isinya dipakai ulang tanpa
	 * dibangkitkan ulang. Isi kode QR dirangkai sebagai teks multi-baris berisi nama pejabat, nama
	 * jenis jabatan, waktu persetujuan, kode dan perihal surat, waktu surat, nama surat, klasifikasi
	 * surat masuk, nama konseptor, serta jurusan/fakultas/sekolah surat, ditutup alamat host aplikasi
	 * ({@code Common.getRequestHostWithProtocol()}) agar pemindai dapat merujuk kembali ke instalasi
	 * yang menerbitkannya. Setiap komponen dilewati bila sumbernya kosong, sehingga QR tetap terbentuk
	 * pada data yang tidak lengkap.
	 * <p>
	 * Beberapa hal yang penting dipahami sebelum mengandalkan berkas ini sebagai bukti:
	 * <ul>
	 * <li><b>Bukan tanda tangan kriptografis.</b> Isinya teks biasa tanpa tanda tangan digital,
	 * tanpa nomor seri, dan tanpa mekanisme verifikasi balik. Siapa pun yang dapat membaca datanya
	 * dapat menyusun QR dengan isi identik. Nilainya adalah kemudahan pemeriksaan silang secara
	 * manual, bukan jaminan keaslian.</li>
	 * <li><b>Terbit tanpa memeriksa status.</b> Method tidak memeriksa {@link #getDisetujui()} sama
	 * sekali. Memanggilnya pada baris yang belum disetujui tetap menghasilkan berkas QR; yang
	 * kosong hanyalah bagian waktu persetujuan, karena {@link #getWaktuPersetujuan()} mengembalikan
	 * {@code null} saat bendera mati. Penyaringan "hanya cetak QR untuk tahapan yang disetujui"
	 * sepenuhnya menjadi tanggung jawab templat laporan yang memanggilnya.</li>
	 * <li><b>Cache tidak pernah kedaluwarsa.</b> Karena berkas dikunci pada id baris dan tidak pernah
	 * dibangkitkan ulang saat sudah ada, perubahan pejabat, waktu persetujuan, atau isi surat setelah
	 * QR pertama dibuat TIDAK tercermin pada QR. Sebaliknya, pembersihan direktori laporan akan
	 * membangkitkan QR baru dengan data terkini &mdash; sehingga dua cetakan surat yang sama pada
	 * waktu berbeda dapat memuat QR yang isinya berbeda.</li>
	 * <li><b>Dipanggil dari luar Java.</b> Tidak ada pemanggil method ini di dalam kode Java; ia
	 * merupakan titik ekstensi yang dievaluasi sebagai ekspresi pada templat {@code .jrxml} yang
	 * disimpan di database sebagai lampiran ({@code LampiranLain.FILE_JRXML_LAYOUT_SURAT}). Mengubah
	 * tanda tangan method ini akan merusak templat yang tidak terlihat di repositori.</li>
	 * </ul>
	 * Method mengembalikan lokasi berkas meskipun pembangkitan gagal atau {@link #getSuratMasuk()}
	 * kosong &mdash; pada kasus itu berkasnya tidak ada dan templat akan menampilkan gambar kosong.
	 *
	 * @return lokasi absolut berkas PNG kode QR untuk baris persetujuan ini
	 */
	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/a_s_k_" + getId() + ".png");
		suratMasuk = getSuratMasuk();
		if (!myfilebarcode.exists() && suratMasuk != null) {
			String code = (getPejabat() == null ? "" : getPejabat().getNama() + "\n")
					+ (getJenisJabatan() == null ? "" : getJenisJabatan().getNama() + "\n")
					+ (getWaktuPersetujuan() == null ? ""
							: Common.dateFormat3.get().format(getWaktuPersetujuan()) + "\n")
					+

					(suratMasuk.getKode() == null || suratMasuk.getKode().trim().isEmpty() ? ""
							: suratMasuk.getKode() + "\n")
					+ (suratMasuk.getPerihal() == null || suratMasuk.getPerihal().trim().isEmpty() ? ""
							: suratMasuk.getPerihal() + "\n")
					+ (suratMasuk.getWaktu() == null ? ""
							: Common.dateFormat3.get().format(suratMasuk.getWaktu()) + "\n")
					+ suratMasuk.getNama() + "\n"
					+ (suratMasuk.getKlasifikasiSuratMasuk() == null ? ""
							: suratMasuk.getKlasifikasiSuratMasuk().getNama() + "\n")

					+ (getKonseptor() == null ? "" : getKonseptor().getUserNama() + "\n")

					+ (suratMasuk.getJurusan() == null ? "" : suratMasuk.getJurusan().getNama() + "\n")
					+ (suratMasuk.getFakultas() == null ? "" : suratMasuk.getFakultas().getNama() + "\n")
					+ (suratMasuk.getSekolah() == null ? "" : suratMasuk.getSekolah().getNama() + "\n")
					+ Common.getRequestHostWithProtocol();
			BarcodeCommon.generateCRCode(code, myfilebarcode);
		}
		return myfilebarcode.getAbsolutePath();
	}

}
