package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

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

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.action.master.surat.SuratKeluarAction;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * <b>Entity JPA — satu surat keluar: dokumen resmi yang diterbitkan instansi.</b>
 *
 * <p>Ini adalah entity inti modul surat keluar. Satu baris mewakili satu surat: nomornya
 * ({@link #getKode()}), nomor agendanya ({@link #getAgenda()}), perihalnya, tujuannya, isian
 * dinamisnya (lewat {@link KlasifikasiSuratKeluarParemeterValue}), alur persetujuannya, serta
 * seluruh rujukan ke subjek surat (mahasiswa, siswa, dosen, guru, pegawai) dan ke lingkup
 * organisasi (jurusan, fakultas, sekolah, yayasan, satuan kerja). Wujud PDF-nya dihasilkan
 * {@link #cetak(Tbmuser, java.util.Map)}.</p>
 *
 * <h2>Aturan terpenting: klasifikasi menimpa surat</h2>
 * <p>Tujuh getter pada kelas ini <b>tidak</b> sekadar mengembalikan nilai yang tersimpan pada baris
 * surat. Mereka memeriksa {@link #getKlasifikasiSuratKeluar()} dan, bila klasifikasi punya nilai,
 * <b>menimpa</b> field surat dengan nilai klasifikasi lalu menuliskannya balik:
 * {@link #getJurusan()}, {@link #getFakultas()}, {@link #getSatuanKerja()}, {@link #getSekolah()},
 * {@link #getYayasan()}, {@link #getAlurPersetujuanSuratKeluar()}, dan — lewat mekanisme fallback —
 * {@link #getNama()} serta {@link #getPerihal()}.</p>
 *
 * <p>Konsekuensinya berlapis dan perlu benar-benar dipahami sebelum menyunting kelas ini:</p>
 * <ul>
 *   <li>Menyunting satu baris {@link KlasifikasiSuratKeluar} berdampak <b>surut</b> ke seluruh
 *   surat yang sudah terbit dengan jenis tersebut — termasuk surat lama yang sudah ditandatangani,
 *   dicetak, dan diarsipkan. Membuka daftar surat lama sesudah itu sudah cukup untuk memicu getter
 *   menulis nilai baru ke field.</li>
 *   <li>Karena penimpaan bersifat menulis ke field dan bukan sekadar mengembalikan nilai, nilai
 *   lama pada baris surat <b>hilang</b> begitu surat tersimpan lagi. Rekonstruksi hanya mungkin
 *   lewat tabel revisi Envers.</li>
 *   <li>Nilai yang ditampilkan pada layar dan yang tercetak pada PDF karenanya belum tentu sama
 *   dengan yang tersimpan di database sebelum halaman dibuka.</li>
 * </ul>
 *
 * <h2>Penomoran</h2>
 * <p>{@link #getKode()} berisi nomor surat resmi dan {@link #getAgenda()} berisi nomor agenda
 * internal. Keduanya dihasilkan {@code SuratKeluarAction} dari dua mesin {@link NomorSurat} yang
 * <b>terpisah</b> pada klasifikasi ({@link KlasifikasiSuratKeluar#getNomorSurat()} dan
 * {@link KlasifikasiSuratKeluar#getNomorAgenda()}). Alokasi terjadi pada saat <b>simpan</b>, bukan
 * pada saat surat disetujui, sehingga draf yang kemudian ditolak tetap memegang nomornya dan
 * urutan nomor resmi dapat memuat lompatan.</p>
 *
 * <h2>Persetujuan: dua mesin yang berdampingan</h2>
 * <p>Surat keluar dapat melewati dua mekanisme kendali yang berbeda dan saling bebas:</p>
 * <ol>
 *   <li><b>Alur persetujuan surat</b> — {@link AlurPersetujuanSuratKeluar} berjenjang lewat
 *   {@code parent}, dengan satu baris {@link AlurPersetujuanSuratKeluarStatus} per jenjang yang
 *   memuat {@code disetujui}, {@code ditolak}, {@code selesai}, dan {@code waktuPersetujuan}.</li>
 *   <li><b>SOP/disposisi</b> — kelas ini meng-extend {@link ais.database.model.sop.DataSop},
 *   sehingga sebuah surat dapat pula menjadi object yang berjalan di mesin SOP lewat
 *   {@link #getDisposisiSop()}. {@link #getAktif()} membaca keadaan SOP itu.</li>
 * </ol>
 * <p>Yang perlu diketahui: <b>tidak ada satu pun titik pada jalur cetak yang memeriksa apakah surat
 * sudah disetujui.</b> Uraian lengkapnya ada pada
 * {@link #cetak(Tbmuser, java.util.Map)}; ringkasnya, ketiga jalur cetak yang ada — tombol cetak di
 * {@code SuratKeluarAction}, tombol cetak di {@code DasboardSurat}, dan endpoint
 * {@code ais.action.servlet.api.SuratApi} — semuanya memanggil {@code cetak(...)} tanpa memeriksa
 * {@code disetujui}, {@code ditolak}, maupun {@code selesai}.</p>
 *
 * <h2>Basis data dan audit</h2>
 * <p>Skema {@code surat}, tabel {@code surat_keluar}, dengan
 * {@code dynamicInsert}/{@code dynamicUpdate} dan {@link org.hibernate.envers.Audited}. Mengingat
 * dampak surut penimpaan yang diuraikan di atas, jejak Envers pada tabel ini adalah satu-satunya
 * catatan atas keadaan surat pada saat ia benar-benar diterbitkan. Field
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah <b>audit bayangan</b> pendamping
 * Envers — keharusan teknis agar grid ZK dapat membacanya lewat Criteria biasa.</p>
 *
 * <h2>Catatan pembangkitan</h2>
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "surat", name = "surat_keluar")
public class SuratKeluar extends DataSop {

	/**
	 * 
	 * Versi serialisasi. Nilainya identik dengan hampir seluruh entity lain di paket
	 * {@code ais.database.model.surat} karena berasal dari template hbm2java yang sama; jangan
	 * dipakai sebagai penanda tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama surat. Di-generate database ({@code IDENTITY}) dan dipetakan
	 * {@code insertable = false}.
	 *
	 * <p>Nilai ini juga dipakai sebagai bagian nama berkas QR pada {@link #ttdQr()}
	 * ({@code s_k_<id>.png}); baca peringatan di sana mengenai surat yang belum tersimpan.</p>
	 */
	private Long id;
	/**
	 * Nomor urut surat dalam kelompok penomorannya, disimpan terpisah dari {@link #kode}. Dipakai
	 * {@code SuratKeluarAction} sebagai penanda "surat ini sudah pernah dinomori": surat lama yang
	 * {@code index}-nya masih {@code null} akan dinomori ulang pada penyimpanan berikutnya.
	 */
	private Long index;
	/**
	 * Nama pengguna terakhir yang mengubah surat ini (field audit bayangan). Diisi otomatis oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;
	/**
	 * Id/username pengguna terakhir yang mengubah surat ini (field audit bayangan, pasangan dari
	 * {@link #oleh}).
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah surat ini. Getter murni.
	 *
	 * @return id/username pengubah terakhir, atau {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir, dengan <b>penjaga anti-penghapusan</b>: argumen
	 * {@code null} atau berisi spasi saja diabaikan (langsung {@code return}) sehingga jejak audit
	 * lama tidak tertimpa nilai hampa oleh pemanggil tanpa konteks pengguna. Untuk dokumen resmi,
	 * mempertahankan jejak pengubah lebih penting daripada mencatat "diubah oleh tidak diketahui".
	 *
	 * @param olehId id/username pengubah; diabaikan bila kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks surat, yaitu nomor suratnya ({@code kode}) apa adanya.
	 *
	 * <p>Membaca <b>field</b> {@code kode} langsung, bukan lewat {@link #getKode()}, sehingga
	 * hasilnya tidak ter-{@code trim} dan dapat berupa {@code null} — misalnya untuk surat yang
	 * klasifikasinya tidak punya mesin penomoran. Komponen ZK yang menampilkan object ini akan
	 * menampilkan teks kosong atau {@code "null"} pada kasus tersebut, bukan identitas surat yang
	 * berguna.</p>
	 *
	 * @return nomor surat mentah, atau {@code null}.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir, dengan penjaga anti-penghapusan yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengubah; diabaikan bila kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah surat ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} terisi tepat sebelum Hibernate mengeksekusi
	 * {@code UPDATE}. Tidak berjalan pada INSERT.
	 *
	 * <p>Berbeda dari entity lain di paket ini, pada kelas ini method dan deklarasi field
	 * {@code tanggal_dirubah} sudah terpisah pada baris masing-masing, sehingga penyuntingan di
	 * sekitarnya lebih aman.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir, diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat object dibuat. Juga menjadi nilai cadangan bagi
	 * {@link #getWaktu()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir. Umumnya dipanggil interceptor audit.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan {@code TIMESTAMP} (tanggal + jam).
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nomor surat resmi. Dihasilkan {@code SuratKeluarAction.generateCode(...)} dari
	 * {@link KlasifikasiSuratKeluar#getNomorSurat()}. Lihat {@link #getKode()}.
	 */
	private String kode;
	/**
	 * Nomor agenda internal, terpisah dari {@link #kode}. Dihasilkan
	 * {@code SuratKeluarAction.generateCodeAgenda(...)} dari
	 * {@link KlasifikasiSuratKeluar#getNomorAgenda()}.
	 */
	private String agenda;
	/**
	 * Nama/judul surat. Jatuh ke nama klasifikasi bila kosong; lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Ringkasan atau catatan isi surat. Ditampilkan pada daftar dan dikirim ke template laporan
	 * sebagai parameter {@code ringkasan}.
	 */
	private String keterangan;
	/**
	 * Klasifikasi (jenis) surat. Sumber dari hampir seluruh sifat surat ini — lihat catatan
	 * "klasifikasi menimpa surat" pada dokumentasi kelas.
	 */
	private KlasifikasiSuratKeluar klasifikasiSuratKeluar;
	/**
	 * Tanggal surat, diinisialisasi ke tanggal sekarang. Dipetakan {@code DATE} (tanpa jam).
	 */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/**
	 * Tahun surat; diisi otomatis dari tahun berjalan bila kosong. Lihat {@link #getTahun()}.
	 */
	private Integer tahun;
	/**
	 * Bulan surat (1-12); diisi otomatis dari bulan berjalan bila kosong. Lihat
	 * {@link #getBulan()}.
	 */
	private Integer bulan;
	/**
	 * Loker/lemari arsip tempat surat disimpan secara fisik.
	 */
	private LokerSurat lokerSurat;
	/**
	 * Keterangan lampiran surat (teks bebas, mis. "1 berkas"). Dikirim ke template sebagai
	 * parameter {@code berkas}.
	 */
	private String lampiran;
	/**
	 * Perihal surat. Jatuh ke perihal bawaan klasifikasi bila kosong; lihat {@link #getPerihal()}.
	 */
	private String perihal;
	/**
	 * Tujuan surat (teks bebas). Dikirim ke template sebagai parameter {@code asal}.
	 */
	private String kepada;
	/**
	 * Pengguna yang menyusun konsep surat. <b>Dinolkan</b> oleh getter-nya untuk surat milik
	 * mahasiswa/siswa; lihat {@link #getKonseptor()}.
	 */
	private Tbmuser konseptor;
	/**
	 * Penanda tanda tangan (teks bebas). Berbeda dari QR tanda tangan yang dihasilkan
	 * {@link #ttdQr()}.
	 */
	private String ttd;
	/**
	 * Waktu surat; jatuh ke {@link #tanggal_dirubah} bila kosong. Lihat {@link #getWaktu()}.
	 */
	private Date waktu;
	/**
	 * Tahun akademik surat; jatuh ke tahun akademik berjalan bila kosong.
	 */
	private String tahunAkademik;
	/**
	 * Semester surat (ganjil/genap); jatuh ke semester berjalan bila kosong.
	 */
	private String semester;
	/**
	 * Alur persetujuan yang melekat pada surat ini. <b>Ditimpa</b> alur klasifikasi bila
	 * klasifikasi punya alur; lihat {@link #getAlurPersetujuanSuratKeluar()}.
	 */
	private AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar;

	// Refensi
	/**
	 * Mahasiswa subjek surat, bila peruntukan klasifikasi adalah mahasiswa.
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Dosen subjek surat.
	 */
	private Dosen dosen;
	/**
	 * Siswa subjek surat.
	 */
	private Siswa siswa;
	/**
	 * Guru subjek surat.
	 */
	private Guru guru;
	/**
	 * Pegawai subjek surat.
	 */
	private Pegawai pegawai;
	/**
	 * Jurusan terkait. Diturunkan dari mahasiswa atau klasifikasi oleh getter-nya; lihat
	 * {@link #getJurusan()}.
	 */
	private Jurusan jurusan;
	/**
	 * Fakultas terkait. Diturunkan dari mahasiswa atau klasifikasi oleh getter-nya; lihat
	 * {@link #getFakultas()}.
	 */
	private Fakultas fakultas;
	/**
	 * Yayasan terkait; ditimpa nilai klasifikasi bila terisi.
	 */
	private Yayasan yayasan;
	/**
	 * Sekolah terkait; ditimpa nilai klasifikasi bila terisi.
	 */
	private Sekolah sekolah;
	/**
	 * Satuan kerja penerbit; ditimpa nilai klasifikasi bila terisi. Penanda tenant, bukan
	 * penyaring akses.
	 */
	private SatuanKerja satuanKerja;
	/**
	 * Daftar {@code userId} penerima notifikasi surat, berpemisah koma. Lihat
	 * {@link #getUsernamePengguna()}.
	 */
	private String usernamePengguna;
	/**
	 * Apakah surat disiarkan ke penerima pada {@link #usernamePengguna}. Lihat
	 * {@link #getBroadcast()}.
	 */
	private Boolean broadcast;
	/**
	 * Surat masuk tunggal yang dibalas surat ini. Berdampingan dengan {@link #suratMasuks} yang
	 * menampung banyak rujukan sebagai teks.
	 */
	private SuratMasuk suratMasuk;
	/**
	 * Data tambahan berbentuk JSON ({@code columnDefinition = "text"}). Lihat
	 * {@link #getJenisSurats()}.
	 */
	private String jenisSurats;

	/**
	 * Disposisi SOP yang menjalankan surat ini pada mesin SOP; berasal dari pewarisan
	 * {@link ais.database.model.sop.DataSop}. Dibaca {@link #getAktif()}.
	 */
	private DisposisiSop disposisiSop;

	/**
	 * Catatan revisi yang diisi konseptor ketika surat dikembalikan oleh penyetuju.
	 */
	private String catatanRevisi;
	/**
	 * Baris status alur yang menolak surat ini. Dipakai {@code SuratKeluarAction} untuk menyetel
	 * ulang penolakan menjadi "telah direvisi" saat surat disimpan kembali.
	 */
	private AlurPersetujuanSuratKeluarStatus alurDitolak;
	/**
	 * Apakah surat masih aktif; <b>dihitung ulang</b> dari keadaan SOP oleh getter-nya. Lihat
	 * {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Surat keluar sebelumnya yang dikaitkan dengan surat ini. Diwajibkan bila
	 * {@link KlasifikasiSuratKeluar#getKaitkanDenganSuratLain()} aktif.
	 */
	private SuratKeluar suratSebelumnya;
	/**
	 * Daftar id {@link SuratMasuk} terkait, berpemisah koma. Lihat {@link #getSuratMasuks()}.
	 */
	private String suratMasuks;
	/**
	 * Penanda tipe/kelompok surat berupa teks bebas.
	 */
	private String tipe;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Field {@code tanggal} dan {@code tanggal_dirubah} sudah terisi waktu sekarang lewat
	 * inisialisasi field.
	 */
	public SuratKeluar() {
	}

	/**
	 * Mengembalikan kunci utama surat.
	 *
	 * @return id surat, atau {@code null} bila belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Praktis hanya dipakai Hibernate.
	 *
	 * @param id kunci utama surat.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor surat resmi, ter-{@code trim}, dengan nilai kosong dinormalkan menjadi
	 * {@code null} (tidak destruktif — field tidak ditulis balik).
	 *
	 * <h2>Dari mana nomor ini berasal</h2>
	 * <p>Nomor dihasilkan {@code SuratKeluarAction.generateCode(tambah, klasifikasi, tanggal)} yang
	 * memformat indeks berikutnya lewat {@link KlasifikasiSuratKeluar#getNomorSurat()} dan
	 * menyubstitusi penanda {@code KODE_KLASIFIKASI} dengan
	 * {@link KlasifikasiSuratKeluar#getKode()}. Bila klasifikasi tidak punya mesin penomoran,
	 * generator mengembalikan string kosong dan surat terbit <b>tanpa nomor</b> — getter ini lalu
	 * mengembalikan {@code null}, bukan menandai kegagalan.</p>
	 *
	 * <h2>Nomor dialokasikan saat simpan, bukan saat disetujui</h2>
	 * <p>{@code onSave} memanggil generator dengan {@code tambah = true}, yang untuk mesin
	 * ber-{@code gunakanIndexUrut} menaikkan pencacah {@link NomorSurat} secara permanen. Nomor
	 * karenanya melekat pada surat sejak ia masih berupa draf yang belum melewati satu jenjang
	 * persetujuan pun. Draf yang kemudian ditolak tetap memegang nomornya, sehingga register nomor
	 * surat resmi dapat memuat lompatan yang mewakili surat yang tidak pernah terbit. Ini
	 * konsekuensi desain yang perlu diketahui saat mengaudit kesinambungan penomoran, bukan cacat
	 * tersembunyi.</p>
	 *
	 * <h2>Keunikan ditegakkan di lapisan Action, bukan di sini</h2>
	 * <p>Kolom dipetakan {@code nullable = false, length = 50} tanpa {@code unique = true}.
	 * Pencegahan nomor ganda dilakukan {@code SuratKeluarAction} lewat
	 * {@code kodeSudahDipakai(kode, kecualiId)} pada jalur nomor manual dan lewat penjaga
	 * penaikan indeks pada jalur otomatis. Penulisan langsung ke tabel — impor data, perbaikan SQL,
	 * atau jalur kode baru yang tidak memanggil penjaga itu — tidak dicegah apa pun.</p>
	 *
	 * <p>Perhatikan pula bahwa {@code nullable = false} pada anotasi berselisih dengan kemampuan
	 * getter mengembalikan {@code null}: surat tanpa nomor tetap dapat berada di memori dan akan
	 * ditolak database hanya pada saat {@code INSERT}.</p>
	 *
	 * @return nomor surat tanpa spasi tepi, atau {@code null} bila belum/tidak bernomor.
	 */
	@Column(name = "kode", nullable = false, length = 50)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/**
	 * Menetapkan nomor surat resmi. Setter polos: tanpa {@code trim}, tanpa pemeriksaan keunikan,
	 * dan tanpa pemeriksaan format terhadap pola {@link NomorSurat} mana pun.
	 *
	 * @param kode nomor surat.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan ringkasan/catatan isi surat, dengan {@code null} dinormalkan menjadi string
	 * kosong (tidak destruktif — field tidak ditulis balik).
	 *
	 * <p>Dikirim ke template laporan sebagai parameter {@code ringkasan} oleh
	 * {@code SuratKeluarAction.cetakDisposisi(...)}. Ditampilkan pula pada daftar surat, di mana
	 * {@code SuratKeluarAction} membersihkannya lebih dulu dengan {@code MyHtml.bersihkan(...)} —
	 * pembersihan itu tanggung jawab lapisan tampilan, bukan entity, sehingga jalur baru yang
	 * merender nilai ini sebagai HTML harus melakukan pembersihan sendiri.</p>
	 *
	 * @return ringkasan isi; tidak pernah {@code null}.
	 */
	@Column(name = "keterangan", columnDefinition = "text", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : this.keterangan;
	}

	/**
	 * Menetapkan ringkasan/catatan isi surat.
	 *
	 * @param keterangan ringkasan isi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jurusan terkait surat, dengan <b>penurunan berjenjang yang destruktif</b>.
	 *
	 * <h2>Urutan prioritas</h2>
	 * <p>Setelah {@code check(...)} me-resolve proxy lazy, berlaku urutan berikut dan setiap
	 * langkahnya <b>menulis ke field</b> {@code jurusan}:</p>
	 * <ol>
	 *   <li>Bila {@link #getMahasiswa()} tidak {@code null}, jurusan diambil dari jurusan
	 *   mahasiswa tersebut.</li>
	 *   <li>Bila tidak, dan {@link #getKlasifikasiSuratKeluar()} punya jurusan, jurusan diambil
	 *   dari klasifikasi.</li>
	 *   <li>Bila tidak keduanya, nilai yang tersimpan pada surat dipertahankan.</li>
	 * </ol>
	 *
	 * <p>Perhatikan bahwa nilai yang tersimpan pada baris surat hanya dipakai sebagai pilihan
	 * <b>terakhir</b>. Jurusan yang pernah dipilih operator secara sadar akan tertimpa diam-diam
	 * begitu surat dikaitkan ke seorang mahasiswa, atau begitu klasifikasinya diberi jurusan. Dan
	 * karena penimpaan itu ditulis ke field, satu kali pembacaan (mis. saat merender daftar) diikuti
	 * satu kali penyimpanan sudah cukup untuk membuat perubahan itu permanen di database.</p>
	 *
	 * <h2>Implikasi bagi surat historis</h2>
	 * <p>Prioritas "mahasiswa dulu" berarti jurusan pada surat lama mengikuti jurusan mahasiswa
	 * <b>saat ini</b>, bukan saat surat diterbitkan. Mahasiswa yang pindah jurusan menyebabkan surat
	 * lamanya seolah-olah selalu diterbitkan dari jurusan yang baru — termasuk pada PDF yang dicetak
	 * ulang. Bila keakuratan historis dibutuhkan, nilai yang benar hanya dapat diambil dari tabel
	 * revisi Envers.</p>
	 *
	 * @return jurusan terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		if (getMahasiswa() != null) {
			jurusan = getMahasiswa().getJurusan();
		} else if (getKlasifikasiSuratKeluar() != null && getKlasifikasiSuratKeluar().getJurusan() != null) {
			jurusan = getKlasifikasiSuratKeluar().getJurusan();
		}
		return jurusan;
	}

	/**
	 * Menetapkan jurusan terkait surat. Nilai yang ditetapkan akan tertimpa pada pembacaan
	 * berikutnya bila surat punya mahasiswa atau klasifikasinya punya jurusan — lihat
	 * {@link #getJurusan()}.
	 *
	 * @param jurusan jurusan terkait.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas terkait surat, dengan penurunan berjenjang yang destruktif serupa
	 * {@link #getJurusan()}: fakultas mahasiswa lebih diutamakan, lalu fakultas klasifikasi, baru
	 * nilai yang tersimpan pada surat.
	 *
	 * <p><b>Perhatian:</b> cabang pertama memanggil {@code getMahasiswa().getJurusan().getFakultas()}
	 * — rantai tiga tingkat <b>tanpa penjaga {@code null} pada tingkat tengah</b>. Mahasiswa yang
	 * jurusannya belum terisi akan memicu {@code NullPointerException} di sini, bukan sekadar
	 * menghasilkan fakultas kosong. Bandingkan dengan {@link #getJurusan()} yang rantainya hanya dua
	 * tingkat dan karenanya aman. Kode yang memanggil getter ini di dalam pembangun laporan atau
	 * grid sebaiknya membungkusnya, atau memastikan data mahasiswa lengkap lebih dulu.</p>
	 *
	 * @return fakultas terkait, atau {@code null}.
	 * @throws NullPointerException bila surat punya mahasiswa yang jurusannya {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getMahasiswa() != null) {
			fakultas = getMahasiswa().getJurusan().getFakultas();
		} else if (getKlasifikasiSuratKeluar() != null && getKlasifikasiSuratKeluar().getFakultas() != null) {
			fakultas = getKlasifikasiSuratKeluar().getFakultas();
		}
		return fakultas;
	}

	/**
	 * Menetapkan fakultas terkait surat. Akan tertimpa pada pembacaan berikutnya sesuai urutan
	 * prioritas pada {@link #getFakultas()}.
	 *
	 * @param fakultas fakultas terkait.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Menetapkan klasifikasi (jenis) surat. Setter polos tanpa validasi.
	 *
	 * <p>Karena begitu banyak getter pada kelas ini yang mengambil nilai dari klasifikasi,
	 * memanggil setter ini pada surat yang sudah terbit secara efektif <b>menulis ulang sifat surat
	 * tersebut</b>: alur persetujuan, satuan kerja, sekolah, yayasan, jurusan, fakultas, nama, dan
	 * perihalnya semua dapat berubah pada pembacaan berikutnya. Nomor surat yang sudah dialokasikan
	 * tidak ikut berubah, sehingga surat dapat berakhir dengan nomor berpola klasifikasi lama tetapi
	 * bersifat klasifikasi baru.</p>
	 *
	 * @param klasifikasiSuratKeluar klasifikasi surat.
	 */
	public void setKlasifikasiSuratKeluar(KlasifikasiSuratKeluar klasifikasiSuratKeluar) {
		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;
	}

	/**
	 * Mengembalikan klasifikasi (jenis) surat, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}.
	 *
	 * <p>Dapat {@code null}. Beberapa pemanggil di jalur cetak — antara lain
	 * {@link #cetak(Tbmuser, java.util.Map)} yang memanggil
	 * {@code suratKeluar.getKlasifikasiSuratKeluar().getId()} dan
	 * {@code ais.action.servlet.api.SuratApi} yang melakukan hal serupa — <b>tidak</b> menjaga
	 * kemungkinan itu dan akan melempar {@code NullPointerException} untuk surat tanpa klasifikasi.
	 * Karena kolomnya {@code nullable = true}, surat semacam itu sah tersimpan di database.</p>
	 *
	 * @return klasifikasi surat, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_keluar", nullable = true)
	public KlasifikasiSuratKeluar getKlasifikasiSuratKeluar() {
		klasifikasiSuratKeluar = check(klasifikasiSuratKeluar);
		return klasifikasiSuratKeluar;
	}

	/**
	 * Menetapkan tanggal surat.
	 *
	 * @param tanggal tanggal surat.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan tanggal surat, dengan <b>default destruktif</b> ke tanggal sekarang bila
	 * {@code null}: nilai ditulis ke field lalu dikembalikan.
	 *
	 * <p>Dipetakan {@code DATE} sehingga bagian jam tidak tersimpan. Karena field sudah
	 * diinisialisasi waktu sekarang saat object dibuat, cabang default di sini praktis hanya
	 * tercapai untuk baris lama yang kolom tanggalnya {@code NULL} — dan pada kasus itu getter
	 * menetapkan tanggal <b>hari pembacaan</b>, bukan tanggal surat sebenarnya. Bila baris tersebut
	 * kemudian tersimpan, tanggal palsu itu menjadi permanen. Untuk dokumen resmi, ini efek samping
	 * yang layak diwaspadai saat memperbaiki data lama.</p>
	 *
	 * @return tanggal surat; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/**
	 * Menetapkan loker/lemari arsip tempat surat disimpan.
	 *
	 * @param lokerSurat loker arsip.
	 */
	public void setLokerSurat(LokerSurat lokerSurat) {
		this.lokerSurat = lokerSurat;
	}

	/**
	 * Mengembalikan loker/lemari arsip surat, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}. Murni informasi penyimpanan fisik; tidak memengaruhi hak akses maupun
	 * hasil cetak.
	 *
	 * @return loker arsip, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "loker_surat", nullable = true)
	public LokerSurat getLokerSurat() {
		lokerSurat = check(lokerSurat);
		return lokerSurat;
	}

	/**
	 * Mengembalikan keterangan lampiran surat (teks bebas, mis. "1 berkas"). Getter murni; dapat
	 * {@code null}. Dikirim ke template sebagai parameter {@code berkas}.
	 *
	 * @return keterangan lampiran, atau {@code null}.
	 */
	public String getLampiran() {
		return lampiran;
	}

	/**
	 * Menetapkan keterangan lampiran surat.
	 *
	 * @param lampiran keterangan lampiran.
	 */
	public void setLampiran(String lampiran) {
		this.lampiran = lampiran;
	}

	/**
	 * Mengembalikan tujuan surat (teks bebas). Getter murni; dapat {@code null}. Dikirim ke template
	 * sebagai parameter {@code asal}.
	 *
	 * @return tujuan surat, atau {@code null}.
	 */
	public String getKepada() {
		return kepada;
	}

	/**
	 * Menetapkan tujuan surat.
	 *
	 * @param kepada tujuan surat.
	 */
	public void setKepada(String kepada) {
		this.kepada = kepada;
	}

	/**
	 * Mengembalikan pengguna penyusun konsep surat, dengan <b>penolakan destruktif untuk surat
	 * mahasiswa/siswa</b>.
	 *
	 * <p>Setelah {@code check(...)} me-resolve proxy, getter memeriksa: bila surat punya
	 * {@link #getSiswa()} atau {@link #getMahasiswa()}, field {@code konseptor} ditulis
	 * {@code null} dan {@code null} dikembalikan. Alasannya masuk akal — surat yang diminta seorang
	 * mahasiswa tidak "dikonsep" oleh pegawai tertentu — tetapi cara penerapannya perlu
	 * diperhatikan.</p>
	 *
	 * <h2>Konsekuensi</h2>
	 * <ul>
	 *   <li>Penulisan {@code null} ke field bersifat destruktif: setelah satu pembacaan diikuti satu
	 *   penyimpanan, kolom {@code konseptor} pada baris surat benar-benar kosong. Bila seorang
	 *   pegawai memang menyusun surat itu atas nama mahasiswa, jejak siapa yang menyusunnya
	 *   <b>hilang dari data berjalan</b> dan hanya tersisa di tabel revisi Envers serta di field
	 *   audit bayangan {@link #getOleh()}.</li>
	 *   <li>{@link AlurPersetujuanSuratKeluarStatus} membentuk {@code kodeUnik}-nya dengan
	 *   memperhitungkan konseptor. Karena konseptor dipaksa {@code null} untuk surat
	 *   mahasiswa/siswa, kodeUnik yang terbentuk berbentuk {@code "M_"}/{@code "S_"} dan
	 *   <b>tidak memuat tingkat alur</b>, sehingga seluruh jenjang alur pada satu surat menghasilkan
	 *   kodeUnik yang sama. {@code SuratKeluarAction.simpanStatusAlurJikaBelumAda(...)} memasang
	 *   penanganan khusus untuk itu — menyimpan hanya bila kodeUnik belum ada — agar tidak melanggar
	 *   unique constraint. Efek sampingnya: <b>untuk surat mahasiswa/siswa, hanya jenjang pertama
	 *   yang memperoleh baris status</b>; jenjang berikutnya memakai kembali baris yang sama. Ini
	 *   membatasi kemampuan alur berjenjang pada surat mahasiswa/siswa, dan penting diketahui
	 *   sebelum mengubah perilaku konseptor di sini.</li>
	 *   <li>{@link #ttdQr()} menyertakan nama konseptor pada isi QR. Karena getter ini menolak
	 *   konseptor untuk surat mahasiswa/siswa, QR pada surat tersebut tidak pernah memuat nama
	 *   penyusun.</li>
	 * </ul>
	 *
	 * @return pengguna penyusun konsep, atau {@code null} untuk surat mahasiswa/siswa.
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
	 * Menetapkan pengguna penyusun konsep surat. Nilai yang ditetapkan akan dihapus pada pembacaan
	 * berikutnya bila surat punya mahasiswa atau siswa — lihat {@link #getKonseptor()}.
	 *
	 * @param konseptor pengguna penyusun konsep.
	 */
	public void setKonseptor(Tbmuser konseptor) {
		this.konseptor = konseptor;
	}

	/**
	 * Mengembalikan penanda tanda tangan berupa teks bebas. Getter murni; dapat {@code null}.
	 *
	 * <p>Tidak berkaitan dengan {@link #ttdQr()} yang menghasilkan berkas gambar QR, maupun dengan
	 * {@link KlasifikasiSuratKeluarPunyaJenisJabatan} yang seharusnya mengatur jabatan penanda
	 * tangan. Kolom ini sekadar teks yang dapat dipakai template.</p>
	 *
	 * @return penanda tanda tangan, atau {@code null}.
	 */
	public String getTtd() {
		return ttd;
	}

	/**
	 * Menetapkan penanda tanda tangan.
	 *
	 * @param ttd penanda tanda tangan.
	 */
	public void setTtd(String ttd) {
		this.ttd = ttd;
	}

	/**
	 * Menetapkan perihal surat.
	 *
	 * @param perihal perihal surat.
	 */
	public void setPerihal(String perihal) {
		this.perihal = perihal;
	}

	/**
	 * Mengembalikan perihal surat, dengan <b>fallback destruktif</b> ke perihal bawaan klasifikasi.
	 *
	 * <p>Bila {@code perihal} kosong atau hanya berisi spasi dan klasifikasi terisi, getter
	 * menuliskan {@link KlasifikasiSuratKeluar#getPerihalDefault()} ke field lalu mengembalikannya.
	 * Karena perihal bawaan klasifikasi sendiri jatuh ke {@link KlasifikasiSuratKeluar#getNama()}
	 * bila belum diisi, rantai lengkapnya adalah: perihal surat &rarr; perihal bawaan klasifikasi
	 * &rarr; nama klasifikasi.</p>
	 *
	 * <p>Berbeda dari langkah sebelumnya di rantai itu, penulisan di <b>sini</b> permanen: sekali
	 * getter terpanggil dan surat tersimpan, kolom {@code perihal} berisi teks yang disalin dari
	 * klasifikasi. Sesudah itu perubahan pada klasifikasi tidak lagi memengaruhi surat tersebut.
	 * Akibatnya dua surat berjenis sama dari periode berbeda dapat berperihal berbeda tanpa ada yang
	 * menyuntingnya — pembekuan terjadi pada saat pembacaan pertama, bukan pada saat penerbitan.</p>
	 *
	 * @return perihal surat, atau {@code null} bila surat tanpa klasifikasi dan perihalnya kosong.
	 */
	public String getPerihal() {
		if ((perihal == null || perihal.trim().isEmpty()) && getKlasifikasiSuratKeluar() != null) {
			perihal = getKlasifikasiSuratKeluar().getPerihalDefault();
		}
		return perihal;
	}

	/**
	 * Menetapkan nomor urut surat dalam kelompok penomorannya.
	 *
	 * @param index nomor urut.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut surat dalam kelompok penomorannya. Getter murni; dapat {@code null}.
	 *
	 * <p>Nilai {@code null} punya arti operasional: {@code SuratKeluarAction} memperlakukan surat
	 * ber-{@code index} {@code null} sebagai surat yang belum dinomori dan akan
	 * <b>menomorinya ulang</b> pada penyimpanan berikutnya — termasuk menaikkan pencacah
	 * {@link NomorSurat}. Mengosongkan kolom ini pada surat lama karenanya bukan tindakan netral.</p>
	 *
	 * @return nomor urut, atau {@code null} bila surat belum dinomori.
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan mahasiswa subjek surat, setelah proxy lazy di-resolve lewat {@code check(...)}.
	 * Keberadaannya memicu perilaku khusus pada {@link #getJurusan()}, {@link #getFakultas()}, dan
	 * {@link #getKonseptor()}.
	 *
	 * @return mahasiswa subjek, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menetapkan mahasiswa subjek surat.
	 *
	 * @param mahasiswa mahasiswa subjek.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan nama/judul surat, dengan <b>fallback destruktif</b> ke nama klasifikasi bila
	 * kosong: nilai ditulis ke field lalu dikembalikan.
	 *
	 * <p>Sama seperti {@link #getPerihal()}, pembekuan terjadi pada pembacaan pertama. Perubahan
	 * nama klasifikasi sesudah itu tidak lagi memengaruhi surat yang sudah pernah dibaca dan
	 * disimpan.</p>
	 *
	 * @return nama surat, atau {@code null} bila surat tanpa klasifikasi dan namanya kosong.
	 */
	public String getNama() {
		if ((nama == null || nama.trim().isEmpty()) && getKlasifikasiSuratKeluar() != null) {
			nama = getKlasifikasiSuratKeluar().getNama();
		}
		return nama;
	}

	/**
	 * Menetapkan nama/judul surat.
	 *
	 * @param nama nama surat.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan dosen subjek surat, setelah proxy lazy di-resolve lewat {@code check(...)}.
	 *
	 * @return dosen subjek, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan dosen subjek surat.
	 *
	 * @param dosen dosen subjek.
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan pegawai subjek surat, setelah proxy lazy di-resolve lewat {@code check(...)}.
	 *
	 * @return pegawai subjek, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menetapkan pegawai subjek surat.
	 *
	 * @param pegawai pegawai subjek.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan tahun surat, dengan <b>default destruktif</b> ke tahun berjalan bila
	 * {@code null}.
	 *
	 * <p>Perhatikan bahwa nilai default diambil dari <b>waktu pembacaan</b>, bukan dari
	 * {@link #getTanggal()} milik surat. Untuk surat lama yang kolom tahunnya {@code NULL}, getter
	 * ini menetapkan tahun sekarang — bukan tahun surat — dan menuliskannya ke field, sehingga
	 * penyimpanan berikutnya membuat kekeliruan itu permanen. Pemanggil yang benar-benar
	 * membutuhkan tahun surat sebaiknya membacanya dari {@link #getTanggal()}.</p>
	 *
	 * @return tahun surat; tidak pernah {@code null}.
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun surat.
	 *
	 * @param tahun tahun surat.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan bulan surat (1-12), dengan <b>default destruktif</b> ke bulan berjalan bila
	 * {@code null}.
	 *
	 * <p>Sama seperti {@link #getTahun()}, default diambil dari waktu pembacaan dan bukan dari
	 * {@link #getTanggal()}. Perhatikan penambahan {@code + 1} pada nilai
	 * {@code Calendar.MONTH} — {@code Calendar} memakai bulan berbasis nol, sehingga koreksi ini
	 * memang diperlukan dan nilai yang tersimpan berbasis satu.</p>
	 *
	 * @return bulan surat (1-12); tidak pernah {@code null}.
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menetapkan bulan surat (1-12). Tanpa validasi rentang.
	 *
	 * @param bulan bulan surat.
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan alur persetujuan surat, dengan <b>penimpaan destruktif oleh klasifikasi</b>.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Setelah {@code check(...)} me-resolve proxy, getter memeriksa apakah
	 * {@link #getKlasifikasiSuratKeluar()} punya alur; bila ya, alur klasifikasi <b>ditulis ke
	 * field</b> {@code alurPersetujuanSuratKeluar} milik surat dan dikembalikan. Alur yang tersimpan
	 * pada baris surat hanya dipakai bila klasifikasi tidak punya alur.</p>
	 *
	 * <h2>Konsekuensi</h2>
	 * <ul>
	 *   <li><b>Perubahan alur pada klasifikasi berdampak surut ke surat yang sudah berjalan.</b>
	 *   Surat yang sudah melewati dua jenjang persetujuan dengan alur lama akan, setelah alur
	 *   klasifikasi diganti, dianggap mengikuti alur baru — sementara baris
	 *   {@link AlurPersetujuanSuratKeluarStatus} yang sudah terbentuk masih merujuk alur lama.
	 *   Kedua sisi menjadi tidak konsisten, dan tidak ada mekanisme yang memigrasikan atau
	 *   membatalkan status yang sudah ada.</li>
	 *   <li>Penimpaan bersifat menulis, sehingga setelah satu pembacaan dan satu penyimpanan, alur
	 *   asli surat hilang dari data berjalan.</li>
	 *   <li>Asimetrinya penting: penimpaan hanya terjadi bila alur klasifikasi <b>tidak</b>
	 *   {@code null}. Mengaktifkan {@link KlasifikasiSuratKeluar#getTanpaAlur()} — yang membuat alur
	 *   klasifikasi menjadi {@code null} — karenanya <b>tidak</b> menghapus alur pada surat lama;
	 *   surat lama tetap memakai alur yang sudah melekat pada barisnya. Hanya surat baru yang lahir
	 *   tanpa alur.</li>
	 * </ul>
	 *
	 * <h2>Kaitan dengan pembuatan baris status</h2>
	 * <p>{@code SuratKeluarAction.checkAlurPersetujuanSuratKeluarStatus(...)} hanya membuat baris
	 * {@link AlurPersetujuanSuratKeluarStatus} bila getter ini mengembalikan nilai bukan
	 * {@code null}. Surat tanpa alur karenanya tidak punya satu pun catatan persetujuan — dan tetap
	 * memperoleh nomor resmi serta tetap dapat dicetak. Lihat
	 * {@link #cetak(Tbmuser, java.util.Map)}.</p>
	 *
	 * @return alur persetujuan yang berlaku, atau {@code null} bila surat maupun klasifikasinya
	 *         tidak punya alur.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_keluar", nullable = true)
	public AlurPersetujuanSuratKeluar getAlurPersetujuanSuratKeluar() {
		alurPersetujuanSuratKeluar = check(alurPersetujuanSuratKeluar);

		if (getKlasifikasiSuratKeluar() != null
				&& getKlasifikasiSuratKeluar().getAlurPersetujuanSuratKeluar() != null) {
			alurPersetujuanSuratKeluar = getKlasifikasiSuratKeluar().getAlurPersetujuanSuratKeluar();
		}

		return alurPersetujuanSuratKeluar;
	}

	/**
	 * Menetapkan alur persetujuan surat. Nilai yang ditetapkan akan tertimpa pada pembacaan
	 * berikutnya bila klasifikasi surat punya alur — lihat
	 * {@link #getAlurPersetujuanSuratKeluar()}.
	 *
	 * @param alurPersetujuanSuratKeluar alur persetujuan.
	 */
	public void setAlurPersetujuanSuratKeluar(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) {
		this.alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluar;
	}

	/**
	 * Mengembalikan satuan kerja penerbit surat, dengan <b>penimpaan destruktif oleh
	 * klasifikasi</b>: bila klasifikasi punya satuan kerja, nilainya ditulis ke field surat dan
	 * dikembalikan.
	 *
	 * <p>Field ini adalah <b>penanda kepemilikan, bukan penyaring akses</b>. Entity tidak menolak
	 * pembacaan lintas satuan kerja; penyaringan sepenuhnya bergantung pada Criteria yang disusun
	 * lapisan Action ({@code DasboardSurat.createSuratKeluarVisibilityCriterion(...)} — yang
	 * berakhir dengan kriteria "selalu benar" untuk bentuk pengguna yang tidak tertangani cabang
	 * mana pun dan pada blok {@code catch}-nya, yaitu <b>gagal-membuka</b>).</p>
	 *
	 * <p>Karena penimpaan bersifat menulis, memindahkan sebuah klasifikasi ke satuan kerja lain akan
	 * memindahkan pula kepemilikan seluruh surat lama berjenis itu, tanpa jejak selain Envers.</p>
	 *
	 * @return satuan kerja penerbit, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		if (getKlasifikasiSuratKeluar() != null && getKlasifikasiSuratKeluar().getSatuanKerja() != null) {
			satuanKerja = getKlasifikasiSuratKeluar().getSatuanKerja();
		}
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja penerbit surat. Akan tertimpa pada pembacaan berikutnya bila
	 * klasifikasi punya satuan kerja.
	 *
	 * @param satuanKerja satuan kerja penerbit.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan sekolah terkait surat, dengan penimpaan destruktif oleh klasifikasi — pola yang
	 * sama seperti {@link #getSatuanKerja()}.
	 *
	 * @return sekolah terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);

		if (getKlasifikasiSuratKeluar() != null && getKlasifikasiSuratKeluar().getSekolah() != null) {
			sekolah = getKlasifikasiSuratKeluar().getSekolah();
		}

		return sekolah;
	}

	/**
	 * Menetapkan sekolah terkait surat, dengan penjaga: object {@code Sekolah} yang belum tersimpan
	 * (id {@code null}) diperlakukan sebagai {@code null} agar Hibernate tidak mencoba menyimpan
	 * entity transien lewat cascade {@code PERSIST}. Pola yang sama dipakai
	 * {@link #setYayasan(Yayasan)} dan {@link KlasifikasiSuratKeluar#setSekolah(Sekolah)}.
	 *
	 * @param sekolah sekolah terkait; {@code null} atau object tanpa id dianggap tidak ada.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan terkait surat, dengan penimpaan destruktif oleh klasifikasi — pola yang
	 * sama seperti {@link #getSatuanKerja()}.
	 *
	 * @return yayasan terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);

		if (getKlasifikasiSuratKeluar() != null && getKlasifikasiSuratKeluar().getYayasan() != null) {
			yayasan = getKlasifikasiSuratKeluar().getYayasan();
		}

		return yayasan;
	}

	/**
	 * Menetapkan yayasan terkait surat, dengan penjaga entity transien yang sama seperti
	 * {@link #setSekolah(Sekolah)}.
	 *
	 * @param yayasan yayasan terkait; {@code null} atau object tanpa id dianggap tidak ada.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan siswa subjek surat, setelah proxy lazy di-resolve lewat {@code check(...)}.
	 * Keberadaannya memicu penolakan konseptor pada {@link #getKonseptor()}.
	 *
	 * @return siswa subjek, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan siswa subjek surat.
	 *
	 * @param siswa siswa subjek.
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan guru subjek surat, setelah proxy lazy di-resolve lewat {@code check(...)}.
	 *
	 * @return guru subjek, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan guru subjek surat.
	 *
	 * @param guru guru subjek.
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan daftar {@code userId} penerima notifikasi surat dalam bentuk kanonik berpemisah
	 * koma, dengan <b>normalisasi destruktif</b>.
	 *
	 * <h2>Bentuk kanonik</h2>
	 * <p>Nilai disimpan sebagai rangkaian id yang dibungkus dan dipisahkan koma, mis.
	 * {@code ",budi,siti,"}. Bentuk berpembatas di kedua ujung itulah yang memungkinkan pencocokan
	 * token tepat lewat pemeriksaan substring biasa; {@code SuratKeluarAction} memang mencari
	 * {@code "," + userId + ","} untuk menentukan penerima siaran.</p>
	 *
	 * <p>Getter menyusun bentuk itu: nilai kosong menjadi string kosong, selain itu dibungkus koma
	 * di kedua ujung, lalu {@code ",,"} dirapatkan menjadi {@code ","} sebanyak <b>tiga kali
	 * berturut-turut</b>, disusul pemeriksaan harfiah terhadap {@code ","}, {@code ",,"},
	 * {@code ",,,"}, dan {@code ",,,,"} yang masing-masing dijadikan string kosong. Hasilnya ditulis
	 * balik ke field, lalu dikembalikan setelah {@code trim}.</p>
	 *
	 * <h2>Catatan</h2>
	 * <ul>
	 *   <li>Pengulangan tiga kali dan daftar harfiah empat tingkat adalah pendekatan tambal, bukan
	 *   normalisasi menyeluruh: rangkaian koma yang lebih panjang dapat menyisakan {@code ",,"}.
	 *   Sisa itu tidak keliru cocok dengan token mana pun, tetapi membuat data tidak kanonik.</li>
	 *   <li>Pola yang persis sama — termasuk pengulangan tiga kali dan daftar empat tingkatnya —
	 *   dipakai {@link #getSuratMasuks()} pada kelas ini dan
	 *   {@link KlasifikasiSuratKeluar#getKodeGrupPengguna()} dengan pemisah titik koma. Ketiganya
	 *   salinan satu sama lain; memperbaiki salah satu tanpa yang lain akan membuat perilakunya
	 *   berselisih.</li>
	 *   <li>Pemeriksaan {@code null} di baris {@code return} terakhir tidak pernah tercapai karena
	 *   cabang-cabang di atasnya sudah memastikan nilainya bukan {@code null}.</li>
	 *   <li>Sifat destruktifnya berarti sekadar membuka daftar surat sudah mengubah state object;
	 *   penyimpanan berikutnya menuliskan bentuk kanonik ke kolom {@code username_pengguna}.</li>
	 * </ul>
	 *
	 * @return daftar penerima dalam bentuk kanonik berpemisah koma; tidak pernah {@code null}.
	 */
	@Column(name = "username_pengguna", nullable = true, columnDefinition = "text")
	public String getUsernamePengguna() {
		usernamePengguna = (usernamePengguna == null || usernamePengguna.trim().equalsIgnoreCase(",") ? ""
				: "," + usernamePengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (usernamePengguna.equals(",")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,,")) {
			usernamePengguna = "";
		}

		return usernamePengguna == null ? "" : usernamePengguna.trim();
	}

	/**
	 * Menetapkan daftar {@code userId} penerima notifikasi. Setter polos: nilai disimpan apa adanya
	 * dan baru dinormalkan pada pembacaan berikutnya lewat {@link #getUsernamePengguna()}. Tidak
	 * memvalidasi bahwa id yang dituliskan benar-benar ada.
	 *
	 * @param usernamePengguna daftar penerima berpemisah koma.
	 */
	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}

	/**
	 * Menyatakan apakah surat disiarkan ke penerima pada {@link #getUsernamePengguna()}, dengan
	 * normalisasi {@code null} menjadi {@code false} (tidak destruktif).
	 *
	 * @return {@code true} bila surat disiarkan.
	 */
	public Boolean getBroadcast() {
		return broadcast == null ? false : broadcast;
	}

	/**
	 * Menetapkan apakah surat disiarkan ke penerima.
	 *
	 * @param broadcast {@code true} untuk menyiarkan.
	 */
	public void setBroadcast(Boolean broadcast) {
		this.broadcast = broadcast;
	}

	/**
	 * Mengembalikan nomor agenda internal, ter-{@code trim}, dengan nilai kosong dinormalkan
	 * menjadi {@code null} (tidak destruktif).
	 *
	 * <p>Dihasilkan {@code SuratKeluarAction.generateCodeAgenda(...)} dari mesin
	 * {@link KlasifikasiSuratKeluar#getNomorAgenda()} yang <b>terpisah</b> dari mesin nomor surat,
	 * dengan pencacahnya sendiri. Nomor agenda dan nomor surat karenanya lazim berselisih. Bila
	 * klasifikasi tidak punya mesin agenda, generator mengembalikan string kosong dan
	 * {@code SuratKeluarAction} menyembunyikan kolom agenda pada form — kondisi normal untuk
	 * instansi yang tidak memakai penomoran agenda.</p>
	 *
	 * @return nomor agenda tanpa spasi tepi, atau {@code null} bila tidak dipakai.
	 */
	public String getAgenda() {
		return agenda == null || agenda.trim().isEmpty() ? null : agenda.trim();
	}

	/**
	 * Menetapkan nomor agenda internal. Tanpa pemeriksaan keunikan.
	 *
	 * @param agenda nomor agenda.
	 */
	public void setAgenda(String agenda) {
		this.agenda = agenda;
	}

	/**
	 * Mengembalikan surat masuk tunggal yang dibalas surat ini.
	 *
	 * <p>Getter murni: mengembalikan field apa adanya <b>tanpa</b> memanggil {@code check(...)},
	 * berbeda dari mayoritas getter relasi pada kelas ini. Dipadukan dengan {@code @ManyToOne} tanpa
	 * {@code fetch = LAZY} dan {@code @Fetch(FetchMode.SELECT)}, relasi ini pada praktiknya di-fetch
	 * lewat query terpisah.</p>
	 *
	 * <p>Berdampingan dengan {@link #getSuratMasuks()} yang menampung banyak rujukan surat masuk
	 * sebagai daftar id berbentuk teks. Kedua mekanisme itu tidak saling menyinkronkan: mengisi
	 * salah satunya tidak memperbarui yang lain, sehingga sebuah surat dapat menunjuk satu surat
	 * masuk lewat relasi ini sekaligus menunjuk himpunan surat masuk yang berbeda lewat daftar
	 * teksnya.</p>
	 *
	 * @return surat masuk yang dibalas, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_masuk", nullable = true)
	public SuratMasuk getSuratMasuk() {
		return suratMasuk;
	}

	/**
	 * Menetapkan surat masuk tunggal yang dibalas surat ini. Tidak memperbarui
	 * {@link #getSuratMasuks()}.
	 *
	 * @param suratMasuk surat masuk yang dibalas.
	 */
	public void setSuratMasuk(SuratMasuk suratMasuk) {
		this.suratMasuk = suratMasuk;
	}

	/**
	 * Nilai bawaan {@link #getJenisSurats()}: representasi teks dari objek JSON kosong,
	 * yaitu {@code "{}"}.
	 *
	 * <p><b>Peringatan:</b> dideklarasikan {@code public static} tanpa {@code final}, sehingga
	 * merupakan state global yang dapat ditimpa kode mana pun di dalam JVM. Nilainya dihitung sekali
	 * saat kelas dimuat lewat {@code new JSONObject().toString()}. Selama tidak ada yang menugaskan
	 * ulang, ia berperilaku seperti konstanta — tetapi bahasa tidak menjaminnya.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONObject().toString();

	/**
	 * Mengembalikan data tambahan surat dalam bentuk teks JSON, dengan fallback ke
	 * {@link #DEFAULT_FORMULA} (objek JSON kosong) bila kosong. Fallback ini <b>tidak</b> destruktif
	 * — nilai tidak ditulis balik ke field, sehingga kolomnya tetap {@code NULL} di database.
	 *
	 * <p>Kolom bertipe {@code text} dan isinya tidak divalidasi entity: tidak ada jaminan bahwa
	 * teks yang tersimpan benar-benar JSON yang sah. Pemanggil yang mengurainya harus menangani
	 * kegagalan penguraian sendiri.</p>
	 *
	 * <p>Perhatikan bahwa fallback memakai variabel {@code static} yang tidak {@code final}; bila
	 * variabel itu pernah ditimpa, seluruh surat yang kolomnya kosong akan mengembalikan nilai baru
	 * tersebut.</p>
	 *
	 * @return teks JSON data tambahan; tidak pernah {@code null}.
	 */
	@Column(name = "jenis_surats", columnDefinition = "text")
	public String getJenisSurats() {
		return jenisSurats == null || jenisSurats.isEmpty() ? DEFAULT_FORMULA : jenisSurats;
	}

	/**
	 * Menetapkan data tambahan surat dalam bentuk teks JSON. Tanpa validasi format.
	 *
	 * @param jenisSurats teks JSON.
	 */
	public void setJenisSurats(String jenisSurats) {
		this.jenisSurats = jenisSurats;
	}

	/**
	 * Mengembalikan disposisi SOP yang menjalankan surat ini, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}. Implementasi method abstrak dari
	 * {@link ais.database.model.sop.DataSop}.
	 *
	 * <p>Bila terisi, surat ini juga berjalan di mesin SOP — mekanisme kendali yang terpisah dan
	 * berdampingan dengan alur persetujuan surat. {@link #getAktif()} membaca keadaannya.</p>
	 *
	 * @return disposisi SOP, atau {@code null} bila surat tidak berjalan di mesin SOP.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan disposisi SOP untuk surat ini, dengan penjaga berlapis yang <b>hanya mengizinkan
	 * penetapan, tidak pernah penghapusan</b>.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Baris pertama memutus eksekusi bila argumen {@code null} atau ber-id {@code null}: setter
	 * langsung {@code return} tanpa mengubah apa pun. Artinya rujukan disposisi SOP yang sudah
	 * melekat pada surat <b>tidak dapat dilepaskan lewat setter ini</b>; melepaskannya memerlukan
	 * penulisan langsung ke database.</p>
	 *
	 * <p>Baris kedua adalah ekspresi ternary bertingkat yang, karena penjaga di baris pertama sudah
	 * memastikan {@code disposisiSop != null && disposisiSop.getId() != null}, kondisinya
	 * <b>tidak pernah bernilai benar</b>. Cabang {@code this.disposisiSop} karenanya kode mati, dan
	 * seluruh ekspresi selalu bermuara pada penugasan sederhana {@code this.disposisiSop =
	 * disposisiSop}. Bentuk berbelitnya adalah sisa dari upaya penjagaan yang kemudian diduplikasi
	 * di baris pertama; menyederhanakannya menjadi penugasan biasa tidak mengubah perilaku.</p>
	 *
	 * @param disposisiSop disposisi SOP; {@code null} atau tanpa id diabaikan sepenuhnya.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Mengembalikan waktu surat, dengan fallback <b>tidak destruktif</b> ke
	 * {@link #getTanggal_dirubah()} bila {@code null}.
	 *
	 * <p>Perlu diperhatikan apa arti fallback ini: untuk surat yang kolom {@code waktu}-nya kosong,
	 * nilai yang dikembalikan adalah <b>waktu perubahan terakhir</b>, bukan waktu penerbitan surat.
	 * Karena {@code tanggal_dirubah} diperbarui setiap kali surat disunting, waktu yang dilaporkan
	 * dapat bergerak maju seiring waktu tanpa suratnya benar-benar diterbitkan ulang. Nilai ini ikut
	 * masuk ke isi QR pada {@link #ttdQr()}, sehingga QR surat lama dapat memuat waktu yang bukan
	 * waktu penerbitannya.</p>
	 *
	 * <p>Dipetakan {@code TIMESTAMP} (tanggal + jam), berbeda dari {@link #getTanggal()} yang
	 * {@code DATE}.</p>
	 *
	 * @return waktu surat, jatuh ke waktu perubahan terakhir bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? getTanggal_dirubah() : waktu;
	}

	/**
	 * Menetapkan waktu surat.
	 *
	 * @param waktu waktu surat.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan catatan revisi yang diisi konseptor ketika surat dikembalikan penyetuju. Getter
	 * murni; dapat {@code null}.
	 *
	 * <p>{@code SuratKeluarAction} menyalin nilai ini ke {@link #getAlurDitolak()} pada saat surat
	 * disimpan kembali, sekaligus menyetel {@code telahDirevisi = true}, {@code ditolak = false},
	 * dan {@code disetujui = false} pada baris status tersebut — mengembalikan surat ke antrean
	 * persetujuan.</p>
	 *
	 * @return catatan revisi, atau {@code null}.
	 */
	@Column(name = "catatan_revisi", columnDefinition = "text", nullable = true)
	public String getCatatanRevisi() {
		return catatanRevisi;
	}

	/**
	 * Menetapkan catatan revisi surat.
	 *
	 * @param catatanRevisi catatan revisi.
	 */
	public void setCatatanRevisi(String catatanRevisi) {
		this.catatanRevisi = catatanRevisi;
	}

	/**
	 * Mengembalikan baris status alur yang menolak surat ini.
	 *
	 * <p>Getter murni tanpa {@code check(...)}; relasi di-fetch lewat
	 * {@code @Fetch(FetchMode.SELECT)}. Dipakai {@code SuratKeluarAction} sebagai penanda "surat ini
	 * pernah ditolak dan sedang direvisi". Menyimpan hanya <b>satu</b> baris penolakan, sehingga
	 * surat yang ditolak berkali-kali di jenjang berbeda hanya menyisakan penolakan terakhir yang
	 * ditetapkan; riwayat penolakan lengkap harus dibaca dari tabel
	 * {@link AlurPersetujuanSuratKeluarStatus} itu sendiri.</p>
	 *
	 * @return baris status penolakan, atau {@code null} bila surat belum pernah ditolak.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "alur_ditolak", nullable = true)
	public AlurPersetujuanSuratKeluarStatus getAlurDitolak() {
		return alurDitolak;
	}

	/**
	 * Menetapkan baris status alur yang menolak surat ini.
	 *
	 * @param alurDitolak baris status penolakan.
	 */
	public void setAlurDitolak(AlurPersetujuanSuratKeluarStatus alurDitolak) {
		this.alurDitolak = alurDitolak;
	}

	/**
	 * Mengembalikan tahun akademik surat, dengan fallback <b>tidak destruktif</b> ke tahun akademik
	 * berjalan ({@code Common.getCurrentTahunAkademik()}).
	 *
	 * <p>Karena fallback tidak ditulis balik ke field, nilai yang dikembalikan untuk surat lama yang
	 * kolomnya kosong <b>berubah seiring bergantinya tahun akademik</b>. Surat yang sama dapat
	 * melaporkan tahun akademik berbeda pada dua kesempatan pembacaan. Untuk pelaporan historis,
	 * andalkan {@link #getTanggal()} atau pastikan kolom ini terisi.</p>
	 *
	 * @return tahun akademik surat; tidak pernah {@code null}.
	 */
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik surat.
	 *
	 * @param tahunAkademik tahun akademik.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester surat, dengan fallback <b>tidak destruktif</b> ke semester berjalan:
	 * {@code Perkuliahan.GANJIL} bila {@code Common.isNowSemensterGanjil()} bernilai benar, selain
	 * itu {@code Perkuliahan.GENAP}.
	 *
	 * <p>Sama seperti {@link #getTahunAkademik()}, fallback tidak ditulis balik sehingga nilai untuk
	 * surat lama berubah mengikuti kalender akademik yang sedang berjalan.</p>
	 *
	 * @return semester surat; tidak pernah {@code null}.
	 */
	public String getSemester() {
		return semester == null ? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP) : semester;
	}

	/**
	 * Menetapkan semester surat.
	 *
	 * @param semester semester surat.
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Menghasilkan (atau memakai ulang) berkas gambar QR berisi ringkasan identitas surat, lalu
	 * mengembalikan <i>path</i> absolutnya untuk disisipkan ke template laporan sebagai parameter
	 * {@code qr.surat}.
	 *
	 * <h2>Isi QR</h2>
	 * <p>Teks yang disandikan disusun dari potongan-potongan berikut, masing-masing dilewati bila
	 * kosong: {@link #getKode()}, {@link #getPerihal()}, {@link #getWaktu()} (diformat
	 * {@code Common.dateFormat3}), {@link #getNama()}, nama klasifikasi, identitas subjek surat
	 * (mahasiswa dengan NIM, siswa dengan nomor induk nasional, guru, dosen, pegawai), nama
	 * {@link #getKonseptor()}, nama jurusan, fakultas, dan sekolah, ditutup dengan
	 * {@code Common.getRequestHostWithProtocol()} yaitu alamat host aplikasi.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getNama()} disisipkan <b>tanpa</b> penjaga kosong, berbeda dari
	 * potongan lain — sehingga QR dapat memuat baris berisi teks {@code "null"} untuk surat yang
	 * namanya belum terisi.</p>
	 *
	 * <h2>Berkas di-cache selamanya berdasarkan id</h2>
	 * <p>Berkas ditulis ke {@code Common.ambilREAL_PATH_REPORT() + "/s_k_" + getId() + ".png"} dan
	 * <b>hanya dibuat bila belum ada</b> ({@code if (!myfilebarcode.exists())}). Tidak ada mekanisme
	 * kedaluwarsa, penghapusan, maupun pembandingan isi. Konsekuensinya:</p>
	 * <ul>
	 *   <li>Setelah QR sebuah surat pernah dibuat, <b>perubahan apa pun pada surat tidak tercermin
	 *   pada QR-nya</b>. Nomor surat yang diperbaiki, perihal yang disunting, subjek yang diganti,
	 *   atau klasifikasi yang diubah semuanya menghasilkan PDF baru yang tetap memuat QR lama.
	 *   Untuk QR yang berfungsi sebagai penanda keaslian dokumen resmi, ini berarti QR dapat
	 *   <b>berselisih dengan isi surat yang mengapitnya</b> tanpa gejala apa pun.</li>
	 *   <li>Karena isi QR mencantumkan {@code Common.getRequestHostWithProtocol()}, QR yang dibuat
	 *   ketika aplikasi diakses lewat satu host akan terus memuat host itu meski aplikasi kemudian
	 *   dipindah atau diakses lewat domain lain.</li>
	 *   <li>Satu-satunya cara menyegarkan QR adalah menghapus berkasnya dari direktori laporan
	 *   secara manual.</li>
	 * </ul>
	 *
	 * <h2>Surat yang belum tersimpan berbagi satu berkas</h2>
	 * <p>Nama berkas memuat {@link #getId()}. Untuk surat yang belum pernah disimpan, id bernilai
	 * {@code null} dan penggabungan string menghasilkan nama harfiah {@code "s_k_null.png"} —
	 * <b>satu berkas yang sama untuk semua surat belum tersimpan di seluruh instalasi</b>. Yang
	 * pertama membuatnya menentukan isinya, dan karena berkas kemudian dianggap sudah ada, surat
	 * berikutnya memakai QR milik surat lain. Praktik pemanggilan yang ada memang selalu lewat jalur
	 * cetak atas surat yang sudah tersimpan, sehingga kondisi ini tidak lazim tercapai; tetapi
	 * tidak ada penjaga apa pun yang mencegahnya.</p>
	 *
	 * <h2>Penanganan galat</h2>
	 * <p>Pembuatan QR dibungkus {@code try/catch (Exception)} yang mencatat ke
	 * {@code ErrorAuditUtil} lalu melanjutkan. Method tetap mengembalikan <i>path</i> berkas
	 * meskipun berkasnya gagal dibuat dan karenanya tidak ada. Pemanggil — yaitu
	 * {@code SuratKeluarAction.cetakDisposisi(...)} yang memasukkannya ke {@code Map} parameter —
	 * meneruskan <i>path</i> tak berujung itu ke JasperReports, yang lalu gagal atau menghasilkan
	 * bidang gambar kosong tergantung cara template ditulis. Kegagalan pembuatan QR karenanya tidak
	 * pernah menghentikan pencetakan dengan pesan yang jelas.</p>
	 *
	 * @return path absolut berkas QR; berkasnya belum tentu ada bila pembuatannya gagal.
	 */
	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/s_k_" + getId() + ".png");
		if (!myfilebarcode.exists()) {
			try {
				String code = (getKode() == null || getKode().trim().isEmpty() ? "" : getKode() + "\n")
						+ (getPerihal() == null || getPerihal().trim().isEmpty() ? "" : getPerihal() + "\n")
						+ (getWaktu() == null ? "" : Common.dateFormat3.get().format(getWaktu()) + "\n") + getNama()
						+ "\n"
						+ (getKlasifikasiSuratKeluar() == null ? "" : getKlasifikasiSuratKeluar().getNama() + "\n")

						+ (getMahasiswa() == null ? ""
								: getMahasiswa().getNim() + " " + getMahasiswa().getNama() + "\n")
						+ (getSiswa() == null ? ""
								: getSiswa().getNomorIndukNasional() + " " + getSiswa().getNama() + "\n")
						+ (getGuru() == null ? "" : getGuru().getNama() + "\n")
						+ (getDosen() == null ? "" : getDosen().getNama() + "\n")
						+ (getPegawai() == null ? "" : getPegawai().getNama() + "\n")
						+ (getKonseptor() == null ? "" : getKonseptor().getUserNama() + "\n")

						+ (getJurusan() == null ? "" : getJurusan().getNama() + "\n")
						+ (getFakultas() == null ? "" : getFakultas().getNama() + "\n")
						+ (getSekolah() == null ? "" : getSekolah().getNama() + "\n")
						+ Common.getRequestHostWithProtocol();
				BarcodeCommon.generateCRCode(code, myfilebarcode);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/surat/SuratKeluar.java:614");

			}
		}
		return myfilebarcode.getAbsolutePath();
	}

	/**
	 * Menyatakan apakah surat masih aktif, dengan nilai <b>dihitung ulang dari keadaan SOP</b> dan
	 * ditulis balik ke field.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Getter memanggil {@link #getDisposisiSop()} dan menugaskannya ke field {@code disposisiSop}
	 * (penugasan yang tidak mengubah apa-apa karena getter itu sendiri sudah menulis hasilnya).
	 * Kemudian:</p>
	 * <ol>
	 *   <li>Bila disposisi SOP ada dan {@code getAktif()}-nya {@code false}, field {@code aktif}
	 *   surat ditulis {@code false}.</li>
	 *   <li>Bila disposisi SOP ada dan langkah akhirnya berada pada alur yang ditandai
	 *   {@code getPenolakanAdaDiSini()}, field {@code aktif} juga ditulis {@code false}.</li>
	 *   <li>Nilai akhir dikembalikan dengan {@code null} dinormalkan menjadi {@code true}.</li>
	 * </ol>
	 *
	 * <h2>Catatan penting</h2>
	 * <ul>
	 *   <li><b>Sinkronisasi satu arah.</b> Getter hanya dapat menulis {@code false}; tidak ada
	 *   cabang yang mengembalikan nilai ke {@code true} ketika keadaan SOP membaik. Surat yang
	 *   sempat dinonaktifkan karena disposisinya tidak aktif akan tetap {@code aktif = false} di
	 *   database sesudahnya, walau SOP-nya kemudian diaktifkan kembali. Mengembalikannya memerlukan
	 *   {@link #setAktif(Boolean)} secara eksplisit — yang memang tersedia di UI daftar surat bagi
	 *   pengguna non-mahasiswa/siswa.</li>
	 *   <li><b>Default membuka.</b> Nilai {@code null} diartikan {@code true}, sehingga surat yang
	 *   belum pernah dikonfigurasi dianggap aktif.</li>
	 *   <li><b>Rantai panjang tanpa penjaga penuh.</b> Cabang kedua memanggil
	 *   {@code disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()} dengan penjaga
	 *   {@code null} pada setiap tingkat kecuali nilai kembalian {@code getPenolakanAdaDiSini()}
	 *   sendiri; bila method itu mengembalikan {@code Boolean} yang {@code null}, pembongkaran
	 *   otomatis ke {@code boolean} akan melempar {@code NullPointerException}.</li>
	 *   <li>Status "aktif" ini <b>tidak</b> menggantikan status persetujuan. Surat yang tidak aktif
	 *   tetap dapat dicetak lewat jalur cetak mana pun — lihat
	 *   {@link #cetak(Tbmuser, java.util.Map)}.</li>
	 * </ul>
	 *
	 * @return {@code true} bila surat masih aktif.
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif surat secara eksplisit. Satu-satunya cara mengembalikan surat ke
	 * keadaan aktif setelah {@link #getAktif()} menonaktifkannya berdasarkan keadaan SOP.
	 *
	 * @param aktif {@code true} untuk mengaktifkan.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda tipe/kelompok surat berupa teks bebas. Getter murni; dapat {@code null}.
	 * Berpasangan dengan {@link KlasifikasiSuratKeluar#getTipe()}; tidak dibatasi enumerasi mana
	 * pun.
	 *
	 * @return penanda tipe, atau {@code null}.
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Menetapkan penanda tipe/kelompok surat.
	 *
	 * @param tipe penanda tipe.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengembalikan daftar id {@link SuratMasuk} terkait dalam bentuk kanonik berpemisah koma,
	 * dengan <b>normalisasi destruktif</b> yang identik dengan
	 * {@link #getUsernamePengguna()} — termasuk pengulangan perapatan {@code ",,"} sebanyak tiga
	 * kali dan daftar pemeriksaan harfiah sampai empat koma. Seluruh catatan pada getter tersebut
	 * berlaku sama di sini.
	 *
	 * <p>Berdampingan dengan relasi tunggal {@link #getSuratMasuk()} tanpa saling menyinkronkan.
	 * Karena isinya berupa id yang disimpan sebagai teks, tidak ada integritas referensial: id surat
	 * masuk yang sudah dihapus tetap tercantum di sini dan hanya diabaikan saat pemanggil mencoba
	 * memuatnya.</p>
	 *
	 * @return daftar id surat masuk dalam bentuk kanonik berpemisah koma; tidak pernah {@code null}.
	 */
	@Column(name = "surat_masuks", columnDefinition = "text")
	public String getSuratMasuks() {
		suratMasuks = (suratMasuks == null || suratMasuks.trim().equalsIgnoreCase(",") ? ""
				: "," + suratMasuks.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (suratMasuks.equals(",")) {
			suratMasuks = "";
		} else if (suratMasuks.equals(",,")) {
			suratMasuks = "";
		} else if (suratMasuks.equals(",,,")) {
			suratMasuks = "";
		} else if (suratMasuks.equals(",,,,")) {
			suratMasuks = "";
		}

		return suratMasuks == null ? "" : suratMasuks.trim();
	}

	/**
	 * Menetapkan daftar id {@link SuratMasuk} terkait. Setter polos; nilai baru dinormalkan pada
	 * pembacaan berikutnya lewat {@link #getSuratMasuks()}.
	 *
	 * @param suratMasuks daftar id berpemisah koma.
	 */
	public void setSuratMasuks(String suratMasuks) {
		this.suratMasuks = suratMasuks;
	}

	/**
	 * Mengembalikan surat keluar sebelumnya yang dikaitkan dengan surat ini.
	 *
	 * <p>Getter murni tanpa {@code check(...)}; relasi di-fetch lewat
	 * {@code @Fetch(FetchMode.SELECT)}. Relasi ini menunjuk ke kelas yang sama (rujukan mandiri),
	 * dan <b>tidak ada penjaga terhadap siklus</b>: tidak ada yang mencegah surat menunjuk dirinya
	 * sendiri, maupun mencegah dua surat saling menunjuk. Bandingkan dengan
	 * {@link AlurPersetujuanSuratKeluar#getParent()} yang memasang penjaga eksplisit terhadap
	 * rujukan ke diri sendiri. Kode yang menelusuri rantai surat sebelumnya harus membawa
	 * penghitung kedalaman atau himpunan id yang sudah dikunjungi.</p>
	 *
	 * <p>Diwajibkan terisi pada jalur simpan bila
	 * {@link KlasifikasiSuratKeluar#getKaitkanDenganSuratLain()} aktif; sebutannya di UI diambil
	 * dari {@link KlasifikasiSuratKeluar#getIstilahSuratLain()}.</p>
	 *
	 * @return surat sebelumnya, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_sebelumnya", nullable = true)
	public SuratKeluar getSuratSebelumnya() {
		return suratSebelumnya;
	}

	/**
	 * Menetapkan surat keluar sebelumnya yang dikaitkan dengan surat ini. Tanpa penjaga siklus —
	 * lihat {@link #getSuratSebelumnya()}.
	 *
	 * @param suratSebelumnya surat sebelumnya.
	 */
	public void setSuratSebelumnya(SuratKeluar suratSebelumnya) {
		this.suratSebelumnya = suratSebelumnya;
	}

	/**
	 * Mencetak surat ini menjadi PDF dengan parameter laporan yang disusun otomatis.
	 *
	 * <p>Varian ringkas: menyusun {@code Map} parameter lewat
	 * {@code SuratUtil.ubahIsiSuratKeluar(suratKeluar, null)} — yang memuat isian dinamis dari
	 * {@link KlasifikasiSuratKeluarParemeterValue} beserta data subjek surat — lalu mendelegasikan
	 * ke {@link #cetak(Tbmuser, java.util.Map)}. Seluruh catatan pada varian tersebut, termasuk
	 * ketiadaan pemeriksaan status persetujuan, berlaku sama di sini.</p>
	 *
	 * <p>Inilah varian yang dipanggil kedua tombol cetak di UI:
	 * {@code SuratKeluarAction} dan {@code ais.action.master.surat.helper.DasboardSurat}.</p>
	 *
	 * @param tbmuser pengguna yang meminta pencetakan; diteruskan ke pembuatan halaman disposisi.
	 * @throws Exception bila penyusunan parameter atau pencetakan gagal.
	 */
	public void cetak(Tbmuser tbmuser) throws Exception {
		SuratKeluar suratKeluar = this;
		Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar, null);
		cetak(tbmuser, parameters);
	}

	/**
	 * Mencetak surat ini menjadi satu berkas PDF gabungan, lalu menampilkannya kepada pengguna.
	 *
	 * <h2>Alur kerja</h2>
	 * <ol>
	 *   <li><b>Halaman disposisi.</b> Dicari satu baris {@link AlurPersetujuanSuratKeluarStatus}
	 *   milik surat ini yang {@code kodeUnik}-nya tidak {@code null}, diurutkan menurun berdasarkan
	 *   id dan diambil satu ({@code setMaxResults(1)}) — yakni baris status <b>terbaru</b>. Bila
	 *   ada, {@code SuratKeluarAction.cetakDisposisi(...)} menghasilkan halaman disposisi yang
	 *   ditambahkan sebagai sumber pertama gabungan PDF.</li>
	 *   <li><b>Hingga 15 lampiran template.</b> Untuk indeks 1 sampai 15, dicari berkas jrxml di
	 *   {@code LampiranLain} dengan kunci {@code FILE_JRXML_LAYOUT_SURAT} (indeks pertama tanpa
	 *   akhiran, selebihnya berakhiran {@code _2} hingga {@code _15}), menggunakan
	 *   {@code getKlasifikasiSuratKeluar().getId()} sebagai penanda pemilik. Setiap berkas yang
	 *   ditemukan diisi menjadi PDF lewat {@code Report.generateCompileFileReport(...)} dan
	 *   ditambahkan ke gabungan.</li>
	 *   <li><b>Tambalan NIP kepala sekolah.</b> Sebelum mengisi tiap template, isi jrxml dibaca dan
	 *   diperiksa apakah memuat pola {@code nipKepalaSekolah}.{@code split(}. Sebagian template yang
	 *   diunggah administrator menuliskan ekspresi yang mengandaikan nilai parameter memuat tanda
	 *   garis miring, padahal nilainya hanya kode pegawai polos, sehingga pengisian gagal dengan
	 *   {@code ArrayIndexOutOfBoundsException}. Untuk template yang terdeteksi memakai pola
	 *   berisiko itu — dan hanya untuk template itu — nilai parameter diberi awalan {@code "-/"}
	 *   sementara, lalu dikembalikan pada blok {@code finally}. Template lain tidak terpengaruh.</li>
	 *   <li><b>Pendamping HTML.</b> Bila pengisian template pertama menghasilkan berkas pendamping
	 *   berakhiran {@code .html} yang tidak kosong, berkas itu disalin ke sebelah PDF gabungan agar
	 *   pilihan pratinjau HTML/PDF muncul di antarmuka.</li>
	 *   <li><b>Penggabungan dan penampilan.</b> Seluruh sumber digabung dengan
	 *   {@code PDFMergerUtility} ke berkas bernama acak di direktori laporan, lalu
	 *   {@code Report.tampil(...)} menyodorkannya kepada pengguna.</li>
	 * </ol>
	 *
	 * <h2>TIDAK ADA GERBANG PERSETUJUAN PADA JALUR CETAK</h2>
	 * <p>Ini perilaku terpenting yang perlu diketahui dari method ini. Baris status yang diambil
	 * pada langkah 1 <b>tidak disaring berdasarkan status persetujuan</b>: kriterianya hanya
	 * "{@code kodeUnik} tidak null" dan "milik surat ini", tanpa
	 * {@code Restrictions.eq("disetujui", true)}, tanpa memeriksa {@code ditolak}, dan tanpa
	 * memeriksa {@code selesai}. Baris terbaru dipakai apa adanya, termasuk bila seluruh
	 * jenjangnya masih {@code disetujui = false} atau bahkan {@code ditolak = true}.</p>
	 *
	 * <p>Lebih jauh, ketiadaan gerbang itu bukan kelalaian di satu tempat melainkan konsisten di
	 * seluruh jalur:</p>
	 * <ul>
	 *   <li>Tombol cetak pada {@code SuratKeluarAction} hanya muncul dengan syarat template jrxml
	 *   tersedia; ia tidak memeriksa status persetujuan.</li>
	 *   <li>Tombol cetak pada {@code DasboardSurat} sama.</li>
	 *   <li>{@code SuratKeluarAction.checkAlurPersetujuanSuratKeluarStatus(...)}, yang dipanggil
	 *   pada jalur simpan-lalu-cetak, <b>membuat</b> baris-baris status untuk seluruh jenjang alur
	 *   dengan nilai bawaan {@code disetujui = false}, lalu <b>langsung</b> memanggil
	 *   {@code cetakDisposisi(...)}. Surat dengan demikian tercetak lengkap dengan halaman
	 *   disposisinya pada saat yang sama ketika permintaan persetujuannya baru saja dibuat, sebelum
	 *   satu pun pejabat bertindak.</li>
	 *   <li>Endpoint {@code ais.action.servlet.api.SuratApi} juga mencetak tanpa memeriksa status
	 *   persetujuan.</li>
	 *   <li>Klasifikasi ber-{@link KlasifikasiSuratKeluar#getTanpaAlur()} bahkan tidak menghasilkan
	 *   satu pun baris status, namun suratnya tetap memperoleh nomor resmi dan tetap dapat
	 *   dicetak.</li>
	 *   <li>{@link #getAktif()} yang bernilai {@code false} pun tidak menghalangi pencetakan.</li>
	 * </ul>
	 *
	 * <p>Perlu dicatat secara jujur bahwa sebagian dari ini boleh jadi disengaja: mencetak pratinjau
	 * draf adalah kebutuhan nyata, dan halaman disposisi memang dimaksudkan menampilkan <i>keadaan</i>
	 * alur — termasuk keadaan "belum disetujui". Persoalannya adalah tidak ada pembeda apa pun
	 * antara pratinjau dan cetakan final: berkas yang dihasilkan sama persis, memuat nomor surat
	 * resmi yang sudah dialokasikan permanen dan QR dari {@link #ttdQr()}, tanpa tanda air, tanpa
	 * penanda "DRAF", dan tanpa pembatasan siapa yang boleh mencetak yang mana. Dokumen yang keluar
	 * dari sistem sebelum disetujui karenanya tidak dapat dibedakan dari yang sudah disetujui.</p>
	 *
	 * <h2>Penanganan galat: gagal secara diam-diam</h2>
	 * <p>Setiap iterasi lampiran dibungkus dua lapis {@code try/catch}: lapis dalam meneruskan ke
	 * {@code Common.tampilErrorJikaAdmin(e)} — yang hanya menampilkan galat kepada pengguna
	 * ber-peran admin — dan lapis luar mencatat ke {@code ErrorAuditUtil} lalu melanjutkan ke
	 * indeks berikutnya. Penggabungan akhir juga dibungkus {@code tampilErrorJikaAdmin}.
	 * Konsekuensinya: bila sebuah template gagal diisi, pengguna non-admin memperoleh PDF yang
	 * <b>kurang halaman tanpa peringatan apa pun</b>. Surat resmi yang kehilangan lampirannya
	 * tampak seperti surat yang memang hanya sekian halaman.</p>
	 *
	 * <h2>Catatan teknis lain</h2>
	 * <ul>
	 *   <li>{@code getKlasifikasiSuratKeluar().getId()} dipanggil tanpa penjaga {@code null};
	 *   surat tanpa klasifikasi memicu {@code NullPointerException} yang tertangkap
	 *   {@code catch} lapis luar dan diperlakukan sebagai "lampiran tidak ada", sehingga
	 *   pencetakan menghasilkan PDF tanpa isi alih-alih pesan galat yang jelas.</li>
	 *   <li>Aliran keluaran penggabungan ditutup pada blok {@code finally}, tetapi berkas hasil
	 *   pengisian tiap template tidak dibersihkan — direktori laporan tumbuh setiap kali surat
	 *   dicetak.</li>
	 *   <li>Method mengubah {@code Map} {@code parameters} yang diterimanya (menyisipkan
	 *   {@code qr.surat}, {@code disposisi.qr.surat}, dan kawan-kawan lewat
	 *   {@code cetakDisposisi}), jadi {@code Map} tersebut tidak boleh dipakai ulang untuk surat
	 *   lain.</li>
	 * </ul>
	 *
	 * @param tbmuser    pengguna yang meminta pencetakan; diteruskan ke pembuatan halaman disposisi
	 *                   dan ke penyusunan kop surat.
	 * @param parameters peta parameter laporan; <b>dimutasi</b> oleh method ini.
	 * @throws Exception bila pengambilan sesi Hibernate atau pembuatan halaman disposisi gagal;
	 *                   kegagalan per-template dan kegagalan penggabungan tidak dilemparkan.
	 */
	public void cetak(Tbmuser tbmuser, Map<String, Object> parameters) throws Exception {
		SuratKeluar suratKeluar = this;

		PDFMergerUtility ut = new PDFMergerUtility();
		File htmlCompanionSrc = null; // HTML companion template pertama untuk toggle pratinjau
		Session session = HibernateUtil.currentSession();
		AlurPersetujuanSuratKeluarStatus disposisiTerakhir = (AlurPersetujuanSuratKeluarStatus) session
				.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.isNotNull("suratKeluar")).add(Restrictions.eq("suratKeluar", suratKeluar))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (disposisiTerakhir != null) {
			File f = SuratKeluarAction.cetakDisposisi(parameters, disposisiTerakhir, false, tbmuser);
			ut.addSource(f);
		}

		for (int index = 1; index <= 15; index++) {
			try {
				LampiranLain lampiranLain = LampiranLain.ambil(suratKeluar.getKlasifikasiSuratKeluar().getId(),
						LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
				if (lampiranLain != null && lampiranLain.getId() != null) {
					try {

						File jrxmlFile = lampiranLain.ambilFile();
						// KE-1/KE-2 (pola sama dgn SuratKeluarAction.generateReport): sebagian template
						// jrxml (diunggah admin, bukan berkas repo) menulis ekspresi
						// "NIP. "+$P{sekolah.nipKepalaSekolah}.split("/")[1].trim() -- mengasumsikan nilai
						// parameter memuat "/", padahal isinya HANYA mycode Pegawai polos (lihat
						// SuratKeluarAction.tambahParamNipKepalaSekolah) -> ArrayIndexOutOfBoundsException
						// saat fill. Template LAIN mungkin memakai nilai ini apa adanya (tanpa split),
						// jadi TAK diubah scr global -- deteksi pola berisiko HANYA pada isi jrxml yang
						// sedang diproses, sisipkan "-/" di depan HANYA utk lampiran ini.
						Object nipAsli = parameters.get("sekolah.nipKepalaSekolah");
						boolean nipDiubahSementara = false;
						if (jrxmlFile != null && jrxmlFile.exists() && nipAsli instanceof String
								&& !((String) nipAsli).startsWith("-/")) {
							try {
								String isiJrxml = new String(
										java.nio.file.Files.readAllBytes(jrxmlFile.toPath()), "UTF-8");
								if (isiJrxml.indexOf("nipKepalaSekolah}.split(") >= 0) {
									String nilaiAsli = (String) nipAsli;
									parameters.put("sekolah.nipKepalaSekolah",
											nilaiAsli.trim().isEmpty() ? "-/-" : "-/" + nilaiAsli);
									nipDiubahSementara = true;
								}
							} catch (Exception eScan) { ais.common.ErrorAuditUtil.record(eScan, "auto-audit(empty-catch) src/ais/database/model/surat/SuratKeluar.java:729");
								// Gagal baca/cek isi jrxml -- lanjut pakai nilai asli, jangan halangi cetak.
							}
						}

						try {
							File file = Report.generateCompileFileReport(Report.PDF, parameters,
									jrxmlFile.getAbsolutePath(), ais.ui.util.WaktuUtil.getDate(), false);
							ut.addSource(file);
							if (htmlCompanionSrc == null) {
								File k = new File(file.getAbsolutePath() + ".html");
								if (k.exists() && k.length() > 0) htmlCompanionSrc = k;
							}
						} finally {
							if (nipDiubahSementara) {
								parameters.put("sekolah.nipKepalaSekolah", nipAsli);
							}
						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/surat/SuratKeluar.java:753");
			}

		}

		FileOutputStream fosMerge = null;
		try {
			File filePdfBaru = new File(Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
			fosMerge = new FileOutputStream(filePdfBaru);
			ut.setDestinationStream(fosMerge);
			ut.mergeDocuments();
			// Salin HTML companion agar toggle pratinjau HTML/PDF muncul setelah merge
			if (htmlCompanionSrc != null) {
				File htmlDst = new File(filePdfBaru.getAbsolutePath() + ".html");
				java.io.FileInputStream isCopy = null;
				java.io.FileOutputStream osCopy = null;
				try {
					isCopy = new java.io.FileInputStream(htmlCompanionSrc);
					osCopy = new java.io.FileOutputStream(htmlDst);
					byte[] buf = new byte[8192];
					int n;
					while ((n = isCopy.read(buf)) > 0) osCopy.write(buf, 0, n);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/database/model/surat/SuratKeluar.java:773");
				} finally {
					if (isCopy != null) try { isCopy.close(); } catch (Exception ig2) { ais.common.ErrorAuditUtil.record(ig2, "auto-audit(empty-catch) src/ais/database/model/surat/SuratKeluar.java:775");}
					if (osCopy != null) try { osCopy.close(); } catch (Exception ig2) { ais.common.ErrorAuditUtil.record(ig2, "auto-audit(empty-catch) src/ais/database/model/surat/SuratKeluar.java:776");}
				}
			}
			Report.tampil(filePdfBaru);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (fosMerge != null) {
				try {
					fosMerge.close();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/database/model/surat/SuratKeluar.java:cetak-close-merge-stream");
				}
			}
		}
	}
}
