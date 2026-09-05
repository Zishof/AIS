package ais.database.model.sop;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

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

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.action.master.akunting.KasBesarAction;
import ais.action.master.akunting.KasKecilAction;
import ais.action.master.akunting.PenggantianKasKecilAction;
import ais.action.master.akunting.PertangungjawabanAction;
import ais.action.master.akunting.ProsesTransferAction;
import ais.action.master.akunting.UangMukaAction;
import ais.action.master.asset.PembayaranDpMasterAssetAction;
import ais.action.master.asset.PembayaranPengadaanMasterAssetAction;
import ais.action.master.asset.PembayaranTerminMasterAssetAction;
import ais.action.master.asset.PemesananPengadaanMasterAssetAction;
import ais.action.master.asset.PenerimaanPengadaanMasterAssetAction;
import ais.action.master.asset.PermintaanPengadaanMasterAssetAction;
import ais.action.master.payroll.PembayaranGajiAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.FormSop;

/**
 * Entity master/template SOP (Standard Operating Procedure) — definisi satu prosedur baku yang
 * dapat diajukan/didisposisikan berulang kali oleh banyak pengguna di banyak modul AIS. Baris
 * {@code Sop} sendiri TIDAK menyimpan langkah-langkah alur; langkah (siapa mengerjakan apa,
 * berikutnya ke mana) didefinisikan terpisah oleh {@link AlurSop} yang memegang FK ke baris ini
 * (lihat {@link #initContoh(Session)} untuk contoh bagaimana satu {@code Sop} dirangkai dengan
 * beberapa {@link AlurSop} berurutan). Satu instance perjalanan aktual (pengajuan konkret oleh
 * satu pemohon, di satu titik waktu) direkam oleh {@link DisposisiSop} yang mereferensikan
 * {@code Sop} ini sebagai template-nya.
 *
 * <p><b>Identitas &amp; siklus hidup.</b> {@link #kode} adalah kode singkat SOP (mis. barcode via
 * {@code Common.getGeneratedBarCode}), {@link #nama} nama tampilnya, {@link #versi} versi
 * dokumen SOP (bebas format, mis. "0.1"), {@link #tanggalTerbit} tanggal SOP ini resmi berlaku.
 * {@link #mulai}/{@link #sampai} adalah jendela tanggal opsional pembatasan masa berlaku SOP
 * (di luar jendela ini SOP idealnya tidak lagi ditawarkan untuk pengajuan baru — periksa
 * pemanggil untuk memastikan jendela ini benar-benar ditegakkan, bukan sekadar informasi).
 * {@link #aktif} adalah flag aktif/nonaktif standar (default {@code true} bila belum diisi).</p>
 *
 * <p><b>Kategori &amp; wewenang (derived dari {@link JenisSop}).</b> {@link #jenisSop} adalah FK
 * wajib ke kategori SOP induk. {@link #diperuntukkan}, {@link #jenisPengguna}, dan
 * {@link #usernamePengguna} pada {@code Sop} ini SELALU ditimpa (dibaca ulang) dari
 * {@link JenisSop} pemiliknya setiap kali getter dipanggil dan {@link #getJenisSop()} tidak
 * null — lihat javadoc masing-masing getter untuk detail arah pewarisan nilai ini. Ini berarti
 * field lokal di {@code Sop} untuk ketiga atribut tersebut pada praktiknya hanya dipakai sebagai
 * fallback bila {@link #jenisSop} kebetulan null (padahal kolom ini {@code nullable = false} di
 * skema, sehingga fallback tersebut semestinya tidak pernah teraktifkan dalam data yang valid).</p>
 *
 * <p><b>Cakupan organisasi.</b> {@link #jurusan}, {@link #fakultas}, {@link #yayasan},
 * {@link #sekolah}, dan {@link #satuanKerja} membatasi SOP ini pada unit organisasi tertentu
 * (dipakai untuk penyaringan tampilan per-yayasan/fakultas/sekolah). {@link #getSatuanKerja()}
 * secara khusus TIDAK sekadar mengembalikan field — ia menurunkan (derive) nilai secara
 * berjenjang dari {@link #sekolah} lalu {@link #fakultas} bila field lokal belum diisi; lihat
 * javadoc method tersebut.</p>
 *
 * <p><b>Data uji coba.</b> {@link #untukUjiCoba} menandai baris yang dibuat otomatis oleh
 * {@link #initContoh(Session)} sebagai data demo/latihan, bukan SOP produksi sungguhan.</p>
 *
 * <p><b>Relevansi untuk validasi wewenang persetujuan:</b> {@code Sop} sendiri tidak memiliki
 * predikat "bolehkah user ini menyetujui" — predikat semacam itu ({@code AktorSop.buatCriterion})
 * hidup di {@link AktorSop} dan dipakai lewat {@link AlurSop#getAktorSop()}. Saat menelusuri
 * kebocoran otorisasi pada mesin disposisi SOP, {@code Sop} hanya relevan sebagai penyedia
 * konteks (jenis, cakupan organisasi) — bukan sebagai titik penegakan wewenang itu sendiri.</p>
 *
 * @see AlurSop
 * @see DisposisiSop
 * @see JenisSop
 * @see AktorSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "sop")
public class Sop extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi untuk kontrak {@link java.io.Serializable} yang diwarisi dari
	 * {@link GeneralValueObject}. Nilai ini dipertahankan konstan agar instance yang pernah
	 * diserialisasi (mis. disimpan di sesi HTTP/cache) tetap dapat dibaca kembali selama bentuk
	 * field yang relevan tidak berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** ID baris (primary key), digenerate database ({@code IDENTITY}); lihat {@link #getId()}. */
	private Long id;

	/**
	 * Field audit "shadow" berisi label penyunting terakhir (biasanya nama tampil pengguna).
	 * Bersanding dengan {@link #olehId} yang menyimpan identitas mentahnya; keduanya diisi
	 * mandiri oleh pemanggil (tidak otomatis oleh listener persistence) dan HANYA menerima nilai
	 * non-kosong (lihat {@link #setOleh(String)}) — ini adalah keharusan teknis pola audit
	 * pasangan nama/ID yang berulang di banyak entity AIS, bukan bug.
	 */
	private String oleh;

	/**
	 * Field audit "shadow" berisi identitas mentah (mis. username/ID user) penyunting terakhir,
	 * berpasangan dengan {@link #oleh}. Lihat javadoc {@link #oleh} untuk penjelasan pola ini.
	 */
	private String olehId;

	/**
	 * @return identitas mentah penyunting terakhir sebagaimana tersimpan (lihat {@link #olehId}),
	 *         tanpa transformasi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi {@link #olehId}. Nilai {@code null} atau string kosong/berisi hanya spasi
	 * DIABAIKAN secara diam-diam (method langsung {@code return} tanpa mengubah field) — ini
	 * mencegah baris audit yang sudah terisi tertimpa nilai kosong oleh pemanggil yang lalai,
	 * namun juga berarti tidak ada cara untuk MENGOSONGKAN kembali field ini lewat setter ini.
	 *
	 * @param olehId identitas penyunting; diabaikan bila null/kosong/hanya-spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi {@link #oleh}. Sama seperti {@link #setOlehId(String)}, nilai null/kosong/hanya-spasi
	 * diabaikan secara diam-diam agar label audit yang sudah ada tidak tertimpa kosong.
	 *
	 * @param oleh label penyunting; diabaikan bila null/kosong/hanya-spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return label penyunting terakhir sebagaimana tersimpan (lihat {@link #oleh}), tanpa
	 *         transformasi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence tepat sebelum baris ini
	 * di-{@code UPDATE} ke database. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang bertanggung
	 * jawab memperbarui timestamp audit terkait (termasuk {@link #tanggal_dirubah}) secara
	 * konsisten lintas entity. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Timestamp perubahan terakhir baris ini. Diinisialisasi ke waktu saat instance dibuat
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) dan lazimnya diperbarui ulang oleh
	 * {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi {@link #tanggal_dirubah} secara manual. Dipanggil terutama oleh kode migrasi/impor
	 * yang perlu menyetel timestamp eksplisit; pada alur normal field ini diperbarui otomatis
	 * lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp perubahan terakhir baris ini, dipetakan sebagai kolom {@code TIMESTAMP}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas untuk log/debug: {@code id} digabung {@link #nama} dengan
	 *         pemisah {@code "-"}. Tidak dimaksudkan untuk ditampilkan ke pengguna akhir.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat/barcode SOP; lihat {@link #getKode()} untuk normalisasi saat dibaca. */
	private String kode;

	/** Nama tampil SOP; kolom wajib ({@code nullable = false}) di skema. */
	private String nama;
	/** Versi dokumen SOP (format bebas, mis. {@code "0.1"}); lihat {@link #getVersi()}. */
	private String versi;
	/** FK wajib ke kategori/jenis SOP induk; lihat {@link #getJenisSop()}. */
	private JenisSop jenisSop;
	/** Tanggal SOP ini resmi terbit/berlaku; lihat {@link #getTanggalTerbit()}. */
	private Date tanggalTerbit;
	/** Deskripsi/keterangan bebas teks tentang SOP ini. */
	private String keterangan;

	/** Awal jendela tanggal berlakunya SOP (opsional); lihat {@link #getMulai()}. */
	private Date mulai;
	/** Akhir jendela tanggal berlakunya SOP (opsional); lihat {@link #getSampai()}. */
	private Date sampai;

	/** Flag aktif/nonaktif; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Cakupan organisasi opsional: jurusan; lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Cakupan organisasi opsional: fakultas; lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Cakupan organisasi opsional: yayasan; lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Cakupan organisasi opsional: sekolah; lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/**
	 * Cakupan organisasi opsional: satuan kerja. Field ini bisa DITIMPA oleh
	 * {@link #getSatuanKerja()} dengan nilai yang diturunkan dari {@link #sekolah}/
	 * {@link #fakultas} — lihat javadoc getter tersebut.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Nilai lokal "diperuntukkan"; pada praktiknya SELALU ditimpa oleh
	 * {@link JenisSop#getDiperuntukkan()} setiap kali {@link #getDiperuntukkan()} dipanggil dan
	 * {@link #jenisSop} tidak null — lihat javadoc getter untuk penjelasan lengkap pola
	 * pewarisan nilai ini.
	 */
	private String diperuntukkan;
	/**
	 * Nilai lokal "jenis pengguna" (role, format berpisah-koma); lihat catatan pewarisan pada
	 * {@link #diperuntukkan} dan javadoc {@link #getJenisPengguna()}.
	 */
	private String jenisPengguna;
	/**
	 * Nilai lokal "username pengguna" (username spesifik, format berpisah-koma); lihat catatan
	 * pewarisan pada {@link #diperuntukkan} dan javadoc {@link #getUsernamePengguna()}.
	 */
	private String usernamePengguna;
	/** Flag penanda baris data uji coba/demo yang dibuat oleh {@link #initContoh(Session)}. */
	private Boolean untukUjiCoba;

	/** Konstruktor default kosong, dipakai Hibernate serta kode aplikasi yang membangun SOP baru secara manual sebelum mengisi field satu per satu. */
	public Sop() {
	}

	/**
	 * @return ID baris (primary key), digenerate database dan tidak pernah di-{@code INSERT}
	 *         manual ({@code insertable = false}).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id ID baris; lazimnya hanya diisi ulang oleh Hibernate saat memuat entity dari DB,
	 *           bukan oleh kode aplikasi (kolom bersifat {@code insertable = false}).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return {@link #kode} yang sudah di-{@code trim()}, atau string kosong (bukan
	 *         {@code null}) bila field belum diisi — memudahkan pemanggil UI yang langsung
	 *         menggabung/menampilkan nilai tanpa perlu null-check terpisah.
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * @param kode kode singkat/barcode SOP baru; disimpan apa adanya (trimming terjadi hanya
	 *             saat dibaca lewat {@link #getKode()}).
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return {@link #nama} yang sudah di-{@code trim()}, atau {@code null} bila field memang
	 *         belum diisi (berbeda dengan {@link #getKode()} yang mengembalikan string kosong).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama tampil SOP baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return deskripsi/keterangan SOP apa adanya (tanpa trimming atau default).
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan deskripsi/keterangan SOP baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif SOP ini; {@code true} bila field belum pernah diisi (default
	 *         "aktif" agar SOP lama yang dibuat sebelum kolom ini ada tetap tampak berlaku).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif/nonaktif baru untuk SOP ini.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return versi dokumen SOP apa adanya (format bebas, tidak divalidasi/dinormalisasi).
	 */
	public String getVersi() {
		return versi;
	}

	/**
	 * @param versi versi dokumen SOP baru.
	 */
	public void setVersi(String versi) {
		this.versi = versi;
	}

	/**
	 * @return kategori/jenis SOP induk, dijamin bukan proxy Hibernate yang sudah tidak valid
	 *         berkat {@link #check(Object)} (dipanggil ulang setiap invokasi getter, sesuai pola
	 *         lazy-association standar di {@link GeneralValueObject}). Kolom ini wajib diisi
	 *         di skema ({@code nullable = false}) sehingga nilai {@code null} semestinya hanya
	 *         muncul pada instance yang belum sempat disimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_sop", nullable = false)
	public JenisSop getJenisSop() {
		jenisSop = check(jenisSop);
		return jenisSop;
	}

	/**
	 * @param jenisSop kategori/jenis SOP baru untuk baris ini. Mengganti kategori ini juga akan
	 *                 mengubah hasil {@link #getDiperuntukkan()}/{@link #getJenisPengguna()}/
	 *                 {@link #getUsernamePengguna()} pada panggilan berikutnya karena
	 *                 ketiganya diturunkan langsung dari {@link JenisSop} yang aktif.
	 */
	public void setJenisSop(JenisSop jenisSop) {
		this.jenisSop = jenisSop;
	}

	/**
	 * @return tanggal terbit/berlakunya SOP; bila belum pernah diisi, mengembalikan
	 *         {@code new Date()} (tanggal SAAT DIPANGGIL, bukan tanggal pembuatan baris) —
	 *         perhatikan ini berarti dua panggilan berbeda pada instance yang sama tanpa
	 *         {@link #setTanggalTerbit(Date)} bisa menghasilkan nilai yang sedikit berbeda;
	 *         nilai default ini TIDAK ditulis balik ke field {@link #tanggalTerbit}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTerbit() {
		return tanggalTerbit == null ? new Date() : tanggalTerbit;
	}

	/**
	 * @param tanggalTerbit tanggal terbit/berlaku SOP baru.
	 */
	public void setTanggalTerbit(Date tanggalTerbit) {
		this.tanggalTerbit = tanggalTerbit;
	}

	/**
	 * @param jurusan cakupan jurusan baru untuk SOP ini (boleh {@code null} bila SOP tidak
	 *                dibatasi ke jurusan tertentu).
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return jurusan cakupan SOP ini, dijamin bukan proxy basi berkat {@link #check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param fakultas cakupan fakultas baru untuk SOP ini (boleh {@code null}).
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return fakultas cakupan SOP ini, dijamin bukan proxy basi berkat {@link #check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * @param yayasan cakupan yayasan baru. Berbeda dari setter relasi lain di kelas ini, setter
	 *                ini memaksa {@code null} bila {@code yayasan} yang diberikan transient
	 *                (belum punya {@code id}) — mencegah baris {@code Sop} tersimpan dengan FK
	 *                ke entity yayasan yang belum tersimpan/valid.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * @return yayasan cakupan SOP ini, dijamin bukan proxy basi berkat {@link #check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * @return sekolah cakupan SOP ini, dijamin bukan proxy basi berkat {@link #check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * @param sekolah cakupan sekolah baru. Sama seperti {@link #setYayasan(Yayasan)}, setter ini
	 *                memaksa {@code null} bila {@code sekolah} yang diberikan belum punya
	 *                {@code id} (transient), mencegah FK ke entity yang belum tersimpan.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan satuan kerja cakupan SOP ini, dengan derivasi berjenjang bila field lokal
	 * {@link #satuanKerja} belum diisi secara eksplisit: pertama dicoba satuan kerja milik
	 * {@link #getSekolah()} (bila sekolah diisi dan satuan kerjanya tidak null), lalu — bila
	 * masih belum ditemukan — dicoba satuan kerja milik {@link #getFakultas()}. Hasil derivasi
	 * ini DITULIS BALIK ke field {@link #satuanKerja} sebagai efek samping getter (pola
	 * "getter yang menulis"), sehingga panggilan berikutnya pada instance yang sama akan
	 * langsung mengembalikan nilai yang sudah diturunkan tanpa mengulang derivasi (kecuali
	 * field di-null-kan lagi atau sekolah/fakultas berubah).
	 *
	 * <p>Catatan urutan: derivasi dari sekolah SELALU dicoba lebih dulu dan akan
	 * menimpa/mengalahkan nilai dari fakultas apabila SOP kebetulan memiliki keduanya
	 * (sekolah DAN fakultas) dengan satuan kerja yang berbeda — sekolah menang. Bila baik
	 * sekolah maupun fakultas tidak menghasilkan satuan kerja, nilai {@link #satuanKerja} yang
	 * sudah ada (mis. diisi manual lewat {@link #setSatuanKerja(SatuanKerja)}) tetap
	 * dipertahankan, lalu tetap melalui {@link #check(Object)} untuk memastikan bukan proxy
	 * basi sebelum dikembalikan.</p>
	 *
	 * @return satuan kerja cakupan SOP ini, hasil derivasi dari sekolah/fakultas bila field
	 *         lokal kosong, atau nilai field lokal (setelah {@link #check(Object)}) bila sudah
	 *         diisi dan tidak ada derivasi yang mengalahkannya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {

		if (getSekolah() != null && getSekolah().getSatuanKerja() != null) {
			satuanKerja = getSekolah().getSatuanKerja();
		}

		else if (getFakultas() != null && getFakultas().getSatuanKerja() != null) {
			satuanKerja = getFakultas().getSatuanKerja();
		}

		satuanKerja = check(satuanKerja);

		return satuanKerja;
	}

	/**
	 * @param satuanKerja satuan kerja cakupan baru. Nilai ini bisa DITIMPA pada panggilan
	 *                     {@link #getSatuanKerja()} berikutnya bila {@link #sekolah}/
	 *                     {@link #fakultas} milik SOP ini memiliki satuan kerjanya sendiri —
	 *                     lihat javadoc getter tersebut.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan nilai "diperuntukkan" (target audiens SOP, mis. konstanta
	 * {@code GrupChecklistPenilaianUmum.UNTUK_UMUM}). Bila {@link #getJenisSop()} tidak
	 * {@code null}, field lokal {@link #diperuntukkan} SELALU ditimpa dulu dengan
	 * {@link JenisSop#getDiperuntukkan()} sebelum dikembalikan — artinya nilai yang pernah
	 * diset lewat {@link #setDiperuntukkan(String)} pada {@code Sop} ini efektif hanya
	 * dipakai selama {@link #jenisSop} bernilai {@code null} (skenario yang seharusnya tidak
	 * terjadi pada data valid karena kolom {@code jenis_sop} wajib diisi).
	 *
	 * @return nilai "diperuntukkan" yang diturunkan dari {@link JenisSop} pemilik (kasus umum),
	 *         atau field lokal apa adanya bila {@link #jenisSop} null (fallback yang jarang
	 *         teraktifkan).
	 */
	@Column(name = "diperuntukkan", nullable = true)
	public String getDiperuntukkan() {
		if (getJenisSop() != null) {
			diperuntukkan = getJenisSop().getDiperuntukkan();
		}
		return diperuntukkan;
	}

	/**
	 * @param diperuntukkan nilai lokal "diperuntukkan" baru. Perhatikan bahwa nilai ini pada
	 *                       praktiknya akan ditimpa kembali oleh {@link #getDiperuntukkan()}
	 *                       selama {@link #jenisSop} tidak null — lihat javadoc getter.
	 */
	public void setDiperuntukkan(String diperuntukkan) {
		this.diperuntukkan = diperuntukkan;
	}

	/**
	 * Mengembalikan daftar role/jenis pengguna (format berpisah-koma) yang berwenang atas SOP
	 * ini untuk keperluan penyaringan tampilan. Sama seperti {@link #getDiperuntukkan()}, nilai
	 * ini SELALU ditimpa dari {@link JenisSop#getJenisPengguna()} bila {@link #getJenisSop()}
	 * tidak null, sehingga field lokal {@link #jenisPengguna} pada praktiknya tidak berpengaruh
	 * selama kategori SOP terisi. Hasil akhir selalu di-{@code trim()} dan tidak pernah
	 * {@code null} (string kosong sebagai fallback).
	 *
	 * <p><b>Catatan wewenang:</b> nilai ini murni informasi TAMPILAN/PENYARINGAN yang
	 * diturunkan dari kategori SOP — ia BUKAN sumber kebenaran untuk siapa yang boleh
	 * bertindak sebagai aktor pada satu langkah alur tertentu. Wewenang bertindak per-langkah
	 * ditentukan oleh {@link AktorSop} milik {@link AlurSop} langkah yang bersangkutan (lihat
	 * {@link AktorSop#buatCriterion(ais.database.model.Tbmuser)}), bukan oleh field ini.</p>
	 *
	 * @return jenis pengguna yang diturunkan dari {@link JenisSop} (kasus umum), atau field
	 *         lokal ter-trim bila {@link #jenisSop} null.
	 */
	@Column(name = "jenis_pengguna", nullable = true, columnDefinition = "text")
	public String getJenisPengguna() {
		if (getJenisSop() != null) {
			jenisPengguna = getJenisSop().getJenisPengguna();
		}
		return jenisPengguna == null ? "" : jenisPengguna.trim();
	}

	/**
	 * @param jenisPengguna nilai lokal "jenis pengguna" baru. Efektif ditimpa kembali oleh
	 *                       {@link #getJenisPengguna()} selama {@link #jenisSop} terisi.
	 */
	public void setJenisPengguna(String jenisPengguna) {
		this.jenisPengguna = jenisPengguna;
	}

	/**
	 * Mengembalikan daftar username spesifik (format berpisah-koma) yang berwenang atas SOP ini.
	 * Mengikuti pola derivasi yang sama dengan {@link #getJenisPengguna()}: nilai ditimpa dari
	 * {@link JenisSop#getUsernamePengguna()} bila {@link #getJenisSop()} tidak null. Lihat
	 * catatan wewenang pada javadoc {@link #getJenisPengguna()} — field ini juga bukan sumber
	 * kebenaran validasi wewenang per-langkah alur.
	 *
	 * @return username pengguna yang diturunkan dari {@link JenisSop} (kasus umum), atau field
	 *         lokal ter-trim bila {@link #jenisSop} null.
	 */
	@Column(name = "username_pengguna", nullable = true, columnDefinition = "text")
	public String getUsernamePengguna() {

		if (getJenisSop() != null) {
			usernamePengguna = getJenisSop().getUsernamePengguna();
		}
		return usernamePengguna == null ? "" : usernamePengguna.trim();
	}

	/**
	 * @param usernamePengguna nilai lokal "username pengguna" baru. Efektif ditimpa kembali oleh
	 *                          {@link #getUsernamePengguna()} selama {@link #jenisSop} terisi.
	 */
	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}

	/**
	 * Menyemai (seed) data contoh/demo untuk mesin SOP: tiga {@link AktorSop} generik (staff
	 * unit, kepala unit, ketua yayasan), satu {@link JenisSop} contoh ("Administrasi, logistik,
	 * SDM, dan keuangan"), dan — untuk SETIAP kelas {@link FormSop} yang didaftarkan manual di
	 * {@code allClasses} (mis. {@code UangMukaAction}, {@code PembayaranGajiAction}, berbagai
	 * action pengadaan asset) dikombinasikan dengan SETIAP {@link Yayasan} aktif — satu rangkaian
	 * demo lengkap berupa satu {@code Sop} contoh beserta empat {@link AlurSop} yang membentuk
	 * alur baku: (1) Unit mengajukan &rarr; (2) Kepala Unit menindaklanjuti (bercabang: disetujui
	 * lanjut ke ketua yayasan, atau direvisi kembali ke langkah 1x) &rarr; (3) Ketua Yayasan
	 * menindaklanjuti (disetujui = selesai via {@code setJikaProsesDisetujuiMakaSelesai(true)},
	 * atau direvisi kembali ke langkah 1x) &rarr; (1x) Unit merevisi dan otomatis melanjutkan lagi
	 * ke langkah kepala unit.
	 *
	 * <p><b>Idempotensi.</b> Setiap entity (aktor, jenis SOP, SOP, tiap {@link AlurSop}) HANYA
	 * dibuat bila query {@code Restrictions.eq} pencarian sebelumnya tidak menemukan baris yang
	 * cocok (dicek lewat kombinasi kode/nama/relasi yang relevan) — sehingga method ini AMAN
	 * dipanggil berulang kali (mis. setiap kali aplikasi start atau setiap kali admin membuka
	 * halaman tertentu yang memicunya) tanpa membuat duplikat data demo. Pencarian "sudah ada
	 * SOP demo untuk kombinasi form+yayasan ini?" dilakukan dengan menghitung baris
	 * {@link AlurSop} yang {@code formInputan}-nya cocok dengan nama kelas dan {@code sop.yayasan}
	 * cocok dengan yayasan yang sedang diproses; hanya bila hitungannya nol seluruh rangkaian SOP
	 * contoh untuk kombinasi tersebut dibangun.</p>
	 *
	 * <p><b>Transaksi per-baris, BUKAN satu transaksi besar.</b> Setiap {@code session.save}/
	 * {@code session.update} dibungkus transaksi {@code begin()}/{@code commit()} sendiri-sendiri
	 * secara berurutan (bukan satu transaksi mencakup seluruh method). Konsekuensinya: bila method
	 * ini gagal di tengah jalan (mis. exception saat membuat {@code alurSopKetuaYayasan}), baris
	 * yang sudah ter-commit sebelumnya (aktor, jenis SOP, SOP, {@code alurSopUnit},
	 * {@code alurSopKepalaUnit}) TETAP tersimpan di database walau rangkaian secara keseluruhan
	 * belum lengkap — pemanggilan ulang method ini pada kesempatan berikutnya akan melanjutkan
	 * dari titik yang belum lengkap tersebut berkat pengecekan idempotensi di atas.</p>
	 *
	 * <p><b>Reflection atas {@link FormSop}.</b> Untuk tiap kelas di {@code allClasses}, method
	 * memanggil {@code c.newInstance()} (constructor tanpa argumen) lalu {@code cs.istilah()}
	 * untuk mendapatkan label tampilan form tersebut; kegagalan instansiasi (mis. bila suatu
	 * action tidak lagi memiliki constructor kosong) hanya dicatat lewat
	 * {@code e.printStackTrace()} + {@code ErrorAuditUtil.record} dan TIDAK menghentikan proses
	 * untuk kelas lain — kelas yang gagal diinstansiasi sederhananya tidak mendapat SOP contoh.</p>
	 *
	 * <p><b>Bukan jalur produksi.</b> Method ini murni untuk menyiapkan data latihan/demo (semua
	 * {@code Sop} yang dibuat ditandai {@link #setUntukUjiCoba(Boolean)} {@code true}) dan tidak
	 * boleh dianggap sebagai contoh pola validasi wewenang — perhatikan bahwa seluruh
	 * {@link AktorSop} contoh di sini diberi {@code usernamePengguna} statis ("demo,admin") demi
	 * kemudahan pengujian, BUKAN pola role-based yang dipakai pada AktorSop produksi sungguhan
	 * (bandingkan dengan flag dinamis seperti {@code semuaAtasanLangsungPegawai} di
	 * {@link AktorSop}).</p>
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk seluruh query dan transaksi
	 *                 penyemaian; pemanggil bertanggung jawab atas siklus hidup sesi ini (method
	 *                 ini tidak membuka/menutup sesi, hanya transaksi per-operasi di dalamnya).
	 */
	@SuppressWarnings({ "unchecked" })
	public static void initContoh(Session session) {

		AktorSop unit = (AktorSop) session.createCriteria(AktorSop.class).add(Restrictions.eq("kode", "coba_unit"))
				.setMaxResults(1).uniqueResult();
		if (unit == null) {
			unit = new AktorSop();
			unit.setKode("uji_coba_unit");
			unit.setNama("Staff Kerja");
			unit.setKeterangan("Aktor Staff Unit");
			unit.setUsernamePengguna("demo,admin");
			session.getTransaction().begin();
			session.save(unit);
			session.getTransaction().commit();
		}

		AktorSop kepalaUnit = (AktorSop) session.createCriteria(AktorSop.class)
				.add(Restrictions.eq("kode", "coba_kepala_unit")).setMaxResults(1).uniqueResult();
		if (kepalaUnit == null) {
			kepalaUnit = new AktorSop();
			kepalaUnit.setKode("coba_kepala_unit");
			kepalaUnit.setNama("Kepala Unit");
			kepalaUnit.setKeterangan("Aktor Kepala Unit");
			kepalaUnit.setUsernamePengguna("demo,admin");
			session.getTransaction().begin();
			session.save(kepalaUnit);
			session.getTransaction().commit();
		}

		AktorSop ketuaYayasan = (AktorSop) session.createCriteria(AktorSop.class)
				.add(Restrictions.eq("kode", "coba_ketua_yayasan")).setMaxResults(1).uniqueResult();
		if (ketuaYayasan == null) {
			ketuaYayasan = new AktorSop();
			ketuaYayasan.setKode("coba_ketua_yayasan");
			ketuaYayasan.setNama("Ketua Yayasan");
			ketuaYayasan.setKeterangan("Aktor Ketua Yayasan");
			ketuaYayasan.setUsernamePengguna("demo,admin");
			session.getTransaction().begin();
			session.save(ketuaYayasan);
			session.getTransaction().commit();
		}

		JenisSop jenisSop = (JenisSop) session.createCriteria(JenisSop.class)
				.add(Restrictions.eq("nama", "Administrasi, logistik, SDM, dan keuangan")).setMaxResults(1)
				.uniqueResult();
		if (jenisSop == null) {
			jenisSop = new JenisSop();
			jenisSop.setKode("0001");
			jenisSop.setNama("Administrasi, logistik, SDM, dan keuangan");
			jenisSop.setKeterangan("Administrasi, logistik, SDM, dan keuangan");
			jenisSop.setUsernamePengguna("demo,admin");
			jenisSop.setAktorSop(unit);
			session.getTransaction().begin();
			session.save(jenisSop);
			session.getTransaction().commit();
		}

		TreeMap<String, String> treeMapFormSop = new TreeMap<String, String>();
		List<Class<? extends FormSop>> allClasses = new ArrayList<Class<? extends FormSop>>();
		allClasses.add(PermintaanPengadaanMasterAssetAction.class);
		allClasses.add(PemesananPengadaanMasterAssetAction.class);
		allClasses.add(PenerimaanPengadaanMasterAssetAction.class);
		allClasses.add(PembayaranPengadaanMasterAssetAction.class);
		allClasses.add(PembayaranDpMasterAssetAction.class);
		allClasses.add(PembayaranTerminMasterAssetAction.class);

		allClasses.add(UangMukaAction.class);
		allClasses.add(PertangungjawabanAction.class);
		allClasses.add(KasKecilAction.class);
		allClasses.add(PenggantianKasKecilAction.class);
		allClasses.add(KasBesarAction.class);

		allClasses.add(ProsesTransferAction.class);

		allClasses.add(PembayaranGajiAction.class);

		for (Class<? extends FormSop> c : allClasses) {
			FormSop cs;
			try {
				cs = c.newInstance();
				treeMapFormSop.put(c.getName(), cs.istilah());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sop/Sop.java:388");
			}

		}

		List<Yayasan> yayasans = ConstantValues
				.simpleList(session.createCriteria(Yayasan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Yayasan.class);
		System.out.println("yayasans -> " + yayasans.size());
		for (String c : treeMapFormSop.keySet()) {
			for (Yayasan yayasan : yayasans) {
				int count = ((Number) session.createCriteria(AlurSop.class).add(Restrictions.eq("formInputan", c))
						.createAlias("sop", "sop").add(Restrictions.eq("sop.yayasan", yayasan))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				System.out.println("yayasan -> " + yayasan + " formInputan " + c + " count " + count);
				if (count == 0) {
					String namaForm = treeMapFormSop.get(c);
					String namaSop = "SOP \"" + namaForm + "\" (uji coba)";

					Sop sop = (Sop) session.createCriteria(Sop.class)
							.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("yayasan", yayasan))
							.add(Restrictions.eq("nama", namaSop)).setMaxResults(1).uniqueResult();
					if (sop == null) {
						sop = new Sop();
						sop.setKode(Common.getGeneratedBarCode(3));
						sop.setUntukUjiCoba(true);
						sop.setAktif(true);
						sop.setYayasan(yayasan);
						sop.setNama(namaSop);
						sop.setVersi("0.1");
						sop.setJenisSop(jenisSop);
						sop.setKeterangan("SOP \"" + namaForm + "\", update SOP ini sesuai kebutuhan!!!");
						session.getTransaction().begin();
						session.save(sop);
						session.getTransaction().commit();
					}

					System.out.println("sop -> " + sop);

					AlurSop alurSopUnit = (AlurSop) session.createCriteria(AlurSop.class)
							.add(Restrictions.eq("sop", sop)).add(Restrictions.eq("aktorSop", unit))
							.add(Restrictions.eq("start", true)).setMaxResults(1).uniqueResult();
					if (alurSopUnit == null) {
						alurSopUnit = new AlurSop();
						alurSopUnit.setKode("001");
						alurSopUnit.setAktorSop(unit);
						alurSopUnit.setSop(sop);
						alurSopUnit.setAlurSetelahnyaBerupaPilihan(true);
						alurSopUnit.setAlurSetelahnyaOtomatis(false);
						alurSopUnit.setStart(true);
						alurSopUnit.setFormInputan(c);
						alurSopUnit.setNama("Unit mengajukan \"" + namaForm + "\" kepada kepala unit");
						alurSopUnit.setKeterangan(
								"SOP \"" + namaForm + "\", Unit mengajukan \"" + namaForm + "\" kepada kepala unit");
						session.getTransaction().begin();
						session.save(alurSopUnit);
						session.getTransaction().commit();
					}

					System.out.println("alurSopUnit -> " + alurSopUnit);

					AlurSop alurSopKepalaUnit = (AlurSop) session.createCriteria(AlurSop.class)
							.add(Restrictions.eq("sop", sop)).add(Restrictions.eq("aktorSop", kepalaUnit))
							.setMaxResults(1).uniqueResult();
					if (alurSopKepalaUnit == null) {
						alurSopKepalaUnit = new AlurSop();
						alurSopKepalaUnit.setKode("002");
						alurSopKepalaUnit.setAktorSop(kepalaUnit);
						alurSopKepalaUnit.setSop(sop);
						alurSopKepalaUnit.setAlurSetelahnyaBerupaPilihan(true);
						alurSopKepalaUnit.setAlurSetelahnyaOtomatis(false);
						alurSopKepalaUnit.setSebelumnya(alurSopUnit);
						alurSopKepalaUnit.setStart(false);
						alurSopKepalaUnit.setBekukanFormTampilan(true);
						alurSopKepalaUnit.setFormInputan(c);
						alurSopKepalaUnit.setNama("Kepala Unit menindaklanjuti \"" + namaForm + "\"");
						alurSopKepalaUnit.setKeterangan("SOP \"" + namaForm + "\", Kepala Unit menindaklanjuti \""
								+ namaForm
								+ "\", jika disetujui akan dilanjutkan ke ketua yayasan, jika di revisi kembali ke staff unit");
						session.getTransaction().begin();
						session.save(alurSopKepalaUnit);
						session.getTransaction().commit();
					}

					System.out.println("alurSopKepalaUnit -> " + alurSopKepalaUnit);

					AlurSop alurSopUnitRevisi = (AlurSop) session.createCriteria(AlurSop.class)
							.add(Restrictions.eq("sop", sop)).add(Restrictions.eq("aktorSop", unit))
							.add(Restrictions.eq("start", false)).setMaxResults(1).uniqueResult();
					if (alurSopUnitRevisi == null) {
						alurSopUnitRevisi = new AlurSop();
						alurSopUnitRevisi.setKode("001x");
						alurSopUnitRevisi.setOpsi("Revisi");
						alurSopUnitRevisi.setAktorSop(unit);
						alurSopUnitRevisi.setSop(sop);
						alurSopUnitRevisi.setAlurSetelahnyaBerupaPilihan(true);
						alurSopUnitRevisi.setAlurSetelahnyaOtomatis(false);
						alurSopUnitRevisi.setStart(false);
						alurSopUnitRevisi.setFormInputan(c);
						alurSopUnitRevisi.setSetelahnya(alurSopKepalaUnit);
						alurSopUnitRevisi.setNama("Unit merevisi \"" + namaForm + "\" dan melanjutkan ke kepala unit");
						alurSopUnitRevisi.setKeterangan("SOP \"" + namaForm + "\", Unit melkukan merevisi \"" + namaForm
								+ "\" dan melanjutkan ke kepala unit");
						session.getTransaction().begin();
						session.save(alurSopUnitRevisi);
						session.getTransaction().commit();
					}

					System.out.println("alurSopUnitRevisi -> " + alurSopUnitRevisi);

					alurSopUnit.setSetelahnya(alurSopKepalaUnit);
					session.getTransaction().begin();
					session.update(alurSopUnit);
					session.getTransaction().commit();

					AlurSop alurSopKetuaYayasan = (AlurSop) session.createCriteria(AlurSop.class)
							.add(Restrictions.eq("sop", sop)).add(Restrictions.eq("aktorSop", ketuaYayasan))
							.setMaxResults(1).uniqueResult();
					if (alurSopKetuaYayasan == null) {
						alurSopKetuaYayasan = new AlurSop();
						alurSopKetuaYayasan.setKode("002");
						alurSopKetuaYayasan.setOpsi("Disetujui");
						alurSopKetuaYayasan.setAktorSop(ketuaYayasan);
						alurSopKetuaYayasan.setSop(sop);
						alurSopKetuaYayasan.setBekukanFormTampilan(true);
						alurSopKetuaYayasan.setJikaProsesDisetujuiMakaSelesai(true);
						alurSopKetuaYayasan.setAlurSetelahnyaBerupaPilihan(true);
						alurSopKetuaYayasan.setAlurSetelahnyaOtomatis(false);
						alurSopKetuaYayasan.setSebelumnya(alurSopKepalaUnit);
						alurSopKetuaYayasan.setStart(false);
						alurSopKetuaYayasan.setFormInputan(c);
						alurSopKetuaYayasan.setNama("Ketua yayasan menindaklanjuti \"" + namaForm + "\"");
						alurSopKetuaYayasan.setKeterangan("SOP \"" + namaForm + "\", Ketua yayasan menindaklanjuti \""
								+ namaForm
								+ "\", jika disetujui maka proses selesai, jika di revisi kembali ke staff unit");
						session.getTransaction().begin();
						session.save(alurSopKetuaYayasan);
						session.getTransaction().commit();
					}

					System.out.println("alurSopKetuaYayasan -> " + alurSopKetuaYayasan);

					alurSopKepalaUnit.setSetelahnya(alurSopKetuaYayasan);
					alurSopKepalaUnit.setOpsiSetelahnya("Disetujui");
					alurSopKepalaUnit.setPersetujuanAdaDiSini(false);
					alurSopKepalaUnit.setSetelahnya2(alurSopUnitRevisi);
					alurSopKepalaUnit.setOpsiSetelahnya2("Direvisi");
					alurSopKepalaUnit.setPersetujuanAdaDiSini2(false);

					session.getTransaction().begin();
					session.update(alurSopKepalaUnit);
					session.getTransaction().commit();

					alurSopKetuaYayasan.setSetelahnya(alurSopUnitRevisi);
					alurSopKetuaYayasan.setOpsiSetelahnya("Direvisi");
					alurSopKetuaYayasan.setPersetujuanAdaDiSini(false);

					session.getTransaction().begin();
					session.update(alurSopKetuaYayasan);
					session.getTransaction().commit();
				}
			}
		}
	}

	/**
	 * @return {@code true} bila SOP ini adalah data uji coba/demo (dibuat oleh
	 *         {@link #initContoh(Session)}), {@code false} (default) untuk SOP produksi.
	 */
	public Boolean getUntukUjiCoba() {
		return untukUjiCoba == null ? false : untukUjiCoba;
	}

	/**
	 * @param untukUjiCoba flag penanda data uji coba/demo baru untuk SOP ini.
	 */
	public void setUntukUjiCoba(Boolean untukUjiCoba) {
		this.untukUjiCoba = untukUjiCoba;
	}

	/**
	 * @return awal jendela tanggal berlakunya SOP ini, atau {@code null} bila tidak dibatasi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * @param mulai awal jendela tanggal berlaku baru.
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * @return akhir jendela tanggal berlakunya SOP ini, atau {@code null} bila tidak dibatasi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * @param sampai akhir jendela tanggal berlaku baru.
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}
}
