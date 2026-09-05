package ais.database.model.surat;

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

import ais.action.master.surat.util.SuratUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * <b>Entity JPA — klasifikasi (jenis) surat keluar: cetak biru dari setiap surat resmi yang
 * diterbitkan.</b>
 *
 * <p>Satu baris kelas ini mewakili satu jenis surat, misalnya "Surat Tugas", "Surat Keterangan
 * Aktif Kuliah", atau "Surat Keputusan". Setiap {@link SuratKeluar} menunjuk satu klasifikasi, dan
 * dari klasifikasi itulah surat mewarisi hampir seluruh sifatnya: mesin penomorannya
 * ({@link #getNomorSurat()}, {@link #getNomorAgenda()}), alur persetujuan yang harus dilalui
 * ({@link #getAlurPersetujuanSuratKeluar()}), untuk siapa surat dibuat
 * ({@link #getKlasifikasiSuratKeluarUntuk()}), perihal bawaan
 * ({@link #getPerihalDefault()}), lingkup organisasi ({@link #getSatuanKerja()},
 * {@link #getFakultas()}, {@link #getJurusan()}, {@link #getSekolah()}, {@link #getYayasan()}),
 * serta berbagai prasyarat pembuatan surat.</p>
 *
 * <h2>Klasifikasi mendominasi surat, bukan sebaliknya</h2>
 * <p>Perlu dipahami sejak awal: sejumlah getter pada {@link SuratKeluar} <b>menimpa</b> nilai yang
 * tersimpan pada surat dengan nilai dari klasifikasinya. {@link SuratKeluar#getSatuanKerja()},
 * {@link SuratKeluar#getSekolah()}, {@link SuratKeluar#getYayasan()},
 * {@link SuratKeluar#getJurusan()}, {@link SuratKeluar#getFakultas()}, dan terutama
 * {@link SuratKeluar#getAlurPersetujuanSuratKeluar()} semuanya memeriksa klasifikasi lebih dulu dan
 * memakai nilai klasifikasi bila terisi. Akibatnya, <b>mengubah satu baris klasifikasi berdampak
 * surut ke seluruh surat yang sudah pernah diterbitkan dengan jenis tersebut</b> — termasuk surat
 * lama yang sudah ditandatangani dan diarsipkan. Perlakukan setiap suntingan pada tabel ini sebagai
 * perubahan yang menyentuh data historis, bukan sekadar konfigurasi ke depan.</p>
 *
 * <h2>Anak-anak konfigurasi</h2>
 * <ul>
 *   <li>{@link KlasifikasiSuratKeluarParemeter} — definisi isian dinamis (label, tipe, urutan,
 *   kunci parameter jrxml); jawabannya per-surat di
 *   {@link KlasifikasiSuratKeluarParemeterValue}.</li>
 *   <li>{@link KlasifikasiSuratKeluarPunyaJenisJabatan} — jabatan penanda tangan. <b>Entity
 *   tidur</b>: helper editornya tidak terpasang di UI dan jalur cetak tidak membacanya.</li>
 *   <li>{@link KlasifikasiSuratKeluarPunyaTembusan} — jabatan penerima tembusan. Juga tidur, dengan
 *   alasan yang sama.</li>
 *   <li>{@link KlasifikasiSuratKeluarUntuk} — peruntukan/subjek surat; barisnya di-seed otomatis
 *   oleh {@code SuratUtil} dan berperan sebagai enumerasi.</li>
 *   <li>{@code ais.database.model.file.LampiranLain} — berkas template jrxml, dicari memakai
 *   {@link #getId()} klasifikasi ini dan kunci
 *   {@code LampiranLain.FILE_JRXML_LAYOUT_SURAT} (hingga 15 berkas berurut). Inilah yang
 *   sesungguhnya menentukan wujud surat yang tercetak.</li>
 * </ul>
 *
 * <h2>Gerbang hak lihat (fail-open)</h2>
 * <p>{@link #getKodeGrupPengguna()} menampung daftar {@code roleId} bertanda pemisah titik koma
 * yang menentukan role mana saja yang boleh melihat surat berjenis ini pada dasbor.
 * {@code DasboardSurat.createSuratKeluarVisibilityCriterion(...)} menyusun kriteria "pengguna
 * adalah konseptor/dosen/guru terkait ATAU {@code kodeGrupPengguna} memuat token
 * {@code ;roleId;}". Perlu diketahui bahwa method penyusun kriteria itu berakhir dengan kriteria
 * "selalu benar" untuk bentuk pengguna yang tidak tertangani cabang mana pun <b>dan</b> pada blok
 * {@code catch}-nya — sehingga kegagalan tak terduga saat menyusun penyaring berujung
 * <b>membuka</b>, bukan menutup. Pola yang sama sudah tercatat pada
 * {@link KlasifikasiSuratMasuk}.</p>
 *
 * <h2>Basis data dan audit</h2>
 * <p>Skema {@code surat}, tabel {@code klasifikasi_surat_keluar}, dengan
 * {@code dynamicInsert}/{@code dynamicUpdate} dan {@link org.hibernate.envers.Audited}. Mengingat
 * dampak surut yang disebut di atas, jejak revisi Envers pada tabel ini adalah satu-satunya cara
 * merekonstruksi konfigurasi yang berlaku ketika sebuah surat lama diterbitkan. Field
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah <b>audit bayangan</b> pendamping
 * Envers — keharusan teknis agar grid ZK dapat membacanya lewat Criteria biasa.</p>
 *
 * <h2>Catatan pembangkitan</h2>
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "klasifikasi_surat_keluar")
public class KlasifikasiSuratKeluar extends GeneralValueObject {

	/**
	 * 
	 * Versi serialisasi. Nilainya identik dengan hampir seluruh entity lain di paket
	 * {@code ais.database.model.surat} karena berasal dari template hbm2java yang sama; jangan
	 * dipakai sebagai penanda tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama klasifikasi. Di-generate database ({@code IDENTITY}).
	 *
	 * <p>Id ini juga menjadi kunci pencarian berkas template jrxml di
	 * {@code ais.database.model.file.LampiranLain}
	 * ({@code LampiranLain.ambil(klasifikasi.getId(), FILE_JRXML_LAYOUT_SURAT)}), sehingga menghapus
	 * lalu membuat ulang sebuah klasifikasi akan memutuskan kaitannya dengan seluruh template yang
	 * sudah diunggah untuknya.</p>
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah klasifikasi ini (field audit bayangan). Diisi otomatis
	 * oleh {@link ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;
	/**
	 * Id/username pengguna terakhir yang mengubah klasifikasi ini (field audit bayangan, pasangan
	 * dari {@link #oleh}).
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah klasifikasi ini. Getter murni.
	 *
	 * @return id/username pengubah terakhir, atau {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir, dengan <b>penjaga anti-penghapusan</b>: argumen
	 * {@code null} atau berisi spasi saja diabaikan (langsung {@code return}) sehingga jejak audit
	 * lama tidak tertimpa nilai hampa. Mengingat perubahan pada tabel ini berdampak surut ke surat
	 * lama, jejak audit di sini bernilai tinggi.
	 *
	 * @param olehId id/username pengubah; diabaikan bila kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks klasifikasi, yaitu nilai {@code nama} apa adanya.
	 *
	 * <p>Membaca <b>field</b> {@code nama} langsung, bukan lewat {@link #getNama()}, sehingga
	 * hasilnya tidak ter-{@code trim} dan dapat berupa {@code null}. Dipakai ZK sebagai label
	 * combobox dan sebagai teks pada berbagai grid surat.</p>
	 *
	 * @return nama klasifikasi mentah, atau {@code null}.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir, dengan penjaga anti-penghapusan yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengubah; diabaikan bila kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah klasifikasi ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} terisi tepat sebelum {@code UPDATE} dieksekusi.
	 * Tidak berjalan pada INSERT.
	 *
	 * <p><b>Perhatian:</b> deklarasi field {@code tanggal_dirubah} berada pada BARIS FISIK YANG
	 * SAMA dengan method ini, sehingga Javadoc ini sekaligus mendokumentasikan field tersebut:
	 * stempel waktu perubahan terakhir, diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat object dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Kode klasifikasi. Bukan sekadar label: nilainya disubstitusikan ke dalam pola nomor surat
	 * menggantikan penanda {@code KODE_KLASIFIKASI}. Lihat {@link #getKode()}.
	 */
	private String kode;

	/**
	 * Nama klasifikasi, mis. "Surat Tugas". Menjadi nilai bawaan {@link SuratKeluar#getNama()} dan
	 * {@link #getPerihalDefault()}.
	 */
	private String nama;
	/**
	 * Mesin penomoran surat resmi. Lihat {@link #getNomorSurat()}.
	 */
	private NomorSurat nomorSurat;
	/**
	 * Mesin penomoran agenda (nomor internal, terpisah dari nomor surat). Lihat
	 * {@link #getNomorAgenda()}.
	 */
	private NomorSurat nomorAgenda;
	/**
	 * Perihal bawaan surat; bila kosong, {@link #getPerihalDefault()} memakai {@link #getNama()}.
	 */
	private String perihalDefault;
	// private String prefix;
	// private String postfix;
	/**
	 * Sifat surat (mis. "Biasa", "Rahasia"). Dinormalkan menjadi "Biasa" bila kosong; lihat
	 * {@link #getSifat()}.
	 */
	private String sifat;
	/**
	 * Keterangan bebas untuk operator.
	 */
	private String keterangan;
	/**
	 * Template isi surat berupa teks bebas ({@code columnDefinition = "text"}). Berbeda dari
	 * template jrxml yang tersimpan sebagai berkas di {@code LampiranLain}; lihat
	 * {@link #getTemplate()}.
	 */
	private String template;
	/**
	 * Peruntukan/subjek surat. Dinormalkan menjadi {@code SuratUtil.UMUM} bila kosong; lihat
	 * {@link #getKlasifikasiSuratKeluarUntuk()}.
	 */
	private KlasifikasiSuratKeluarUntuk klasifikasiSuratKeluarUntuk;
	/**
	 * Alur persetujuan berjenjang yang harus dilalui surat berjenis ini. <b>Dinolkan</b> oleh
	 * getter-nya bila {@link #getTanpaAlur()} aktif; lihat {@link #getAlurPersetujuanSuratKeluar()}.
	 */
	private AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar;

	/**
	 * Fakultas pemilik klasifikasi; menimpa {@link SuratKeluar#getFakultas()} bila terisi.
	 */
	private Fakultas fakultas;
	/**
	 * Jurusan pemilik klasifikasi; menimpa {@link SuratKeluar#getJurusan()} bila terisi (kecuali
	 * bila surat punya mahasiswa, yang diprioritaskan).
	 */
	private Jurusan jurusan;
	/**
	 * Satuan kerja pemilik klasifikasi; menimpa {@link SuratKeluar#getSatuanKerja()} bila terisi.
	 * Penanda tenant, bukan penyaring akses.
	 */
	private SatuanKerja satuanKerja;
	/**
	 * Sekolah pemilik klasifikasi; menimpa {@link SuratKeluar#getSekolah()} bila terisi.
	 */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik klasifikasi; menimpa {@link SuratKeluar#getYayasan()} bila terisi.
	 */
	private Yayasan yayasan;
	/**
	 * Apakah klasifikasi masih dapat dipilih; default {@code true}. Lihat {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Daftar {@code roleId} berpemisah titik koma yang boleh melihat surat berjenis ini pada
	 * dasbor. Lihat {@link #getKodeGrupPengguna()}.
	 */
	private String kodeGrupPengguna;
	/**
	 * Prasyarat: mahasiswa harus lunas semester lalu. Lihat
	 * {@link #getHarusBayarLunasSmtLalu()}.
	 */
	private Boolean harusBayarLunasSmtLalu;
	/**
	 * Prasyarat: mahasiswa harus lunas semester berjalan. Lihat
	 * {@link #getHarusBayarLunasSmtSaatIni()}.
	 */
	private Boolean harusBayarLunasSmtSaatIni;
	/**
	 * Kode item biaya yang dikaitkan bila surat berjenis ini berbayar. Lihat
	 * {@link #getKodeItemBiaya()}.
	 */
	private String kodeItemBiaya;
	/**
	 * Apakah biaya surat cukup dibayar sekali. Lihat {@link #getSekaliBayar()}.
	 */
	private Boolean sekaliBayar;
	/**
	 * Apakah surat berjenis ini terbit <b>tanpa</b> alur persetujuan sama sekali. Lihat
	 * {@link #getTanpaAlur()} — flag dengan dampak paling besar pada kelas ini.
	 */
	private Boolean tanpaAlur;
	/**
	 * Apakah surat berjenis ini terbit tanpa template. Lihat {@link #getTanpaTemplate()}.
	 */
	private Boolean tanpaTemplate;
	/**
	 * Apakah surat berjenis ini wajib dikaitkan dengan surat lain. Lihat
	 * {@link #getKaitkanDenganSuratLain()}.
	 */
	private Boolean kaitkanDenganSuratLain;
	/**
	 * Sebutan yang dipakai di UI untuk surat terkait; default "Surat Sebelumnya". Lihat
	 * {@link #getIstilahSuratLain()}.
	 */
	private String istilahSuratLain;
	/**
	 * Apakah pemilih semester ditampilkan pada form surat. Bila {@code null}, nilainya
	 * <b>disimpulkan dari nama peruntukan</b>; lihat {@link #getTampilkanSemester()}.
	 */
	private Boolean tampilkanSemester;

	/**
	 * Prasyarat: mahasiswa harus berstatus aktif kuliah. Lihat {@link #getAktifKuliah()}.
	 */
	private Boolean aktifKuliah;

	/**
	 * Penanda tipe/kelompok klasifikasi berupa teks bebas. Lihat {@link #getTipe()}.
	 */
	private String tipe;
	/**
	 * Batas awal jendela waktu pencetakan. Lihat {@link #getBisaDicetakMulai()} — perhatikan
	 * peringatan tentang arah perbandingan pada penegakannya.
	 */
	private Date bisaDicetakMulai;
	/**
	 * Batas akhir jendela waktu pencetakan. Lihat {@link #getBisaDicetakSampai()}.
	 */
	private Date bisaDicetakSampai;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Seluruh nilai default diterapkan oleh getter masing-masing, bukan di sini.
	 */
	public KlasifikasiSuratKeluar() {
	}

	/**
	 * Mengembalikan kunci utama klasifikasi.
	 *
	 * @return id klasifikasi, atau {@code null} bila belum pernah disimpan.
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
	 * @param id kunci utama klasifikasi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode klasifikasi, dengan normalisasi {@code null} menjadi string kosong (tidak
	 * destruktif — field tidak ditulis balik).
	 *
	 * <p><b>Kode ini masuk ke nomor surat resmi.</b>
	 * {@code SuratKeluarAction.generateCode(...)} dan {@code generateCodeAgenda(...)} menyubstitusi
	 * penanda {@code KODE_KLASIFIKASI} pada pola {@link NomorSurat} dengan nilai ini. Karena
	 * normalisasi mengubah {@code null} menjadi string kosong, klasifikasi yang kodenya belum diisi
	 * menghasilkan nomor surat dengan bagian kode yang <b>hilang tanpa penanda</b> — mis.
	 * {@code "001//UNIV/2026"} alih-alih {@code "001/ST/UNIV/2026"} — bukan kegagalan yang terlihat.
	 * Tidak ada validasi yang mewajibkan kode terisi sebelum klasifikasi dipakai menerbitkan
	 * surat.</p>
	 *
	 * <p>Substitusi dilakukan dengan {@code StringUtils.replaceIgnoreCase} pada
	 * {@code generateCode} tetapi dengan {@code StringUtils.replace} (peka huruf besar-kecil) pada
	 * {@code generateCodeAgenda} — ketidakseragaman yang berarti pola bernomor agenda harus menulis
	 * penanda persis dalam huruf besar.</p>
	 *
	 * @return kode klasifikasi; tidak pernah {@code null}.
	 */
	public String getKode() {
		return kode == null ? "" : kode;
	}

	/**
	 * Menetapkan kode klasifikasi. Tanpa validasi keunikan maupun format; dua klasifikasi boleh
	 * berbagi kode yang sama dan akan menghasilkan awalan nomor surat yang sama.
	 *
	 * @param kode kode klasifikasi.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama klasifikasi, sudah ter-{@code trim} dan tanpa menulis balik ke field.
	 *
	 * <p>Kolom dipetakan {@code nullable = false} dengan panjang 255. Nilai ini menjadi bawaan bagi
	 * {@link SuratKeluar#getNama()} dan, lewat {@link #getPerihalDefault()}, juga bawaan bagi
	 * {@link SuratKeluar#getPerihal()} ketika keduanya belum diisi pada surat.</p>
	 *
	 * @return nama klasifikasi tanpa spasi tepi, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama klasifikasi. Setter polos tanpa {@code trim} maupun pemeriksaan duplikasi.
	 *
	 * @param nama nama klasifikasi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas klasifikasi. Getter murni; dapat {@code null}.
	 *
	 * @return keterangan, atau {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas klasifikasi.
	 *
	 * @param keterangan keterangan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
	//
	// public void setPrefix(String prefix) {
	// this.prefix = prefix;
	// }
	//
	// public String getPrefix() {
	// if (prefix == null) {
	// prefix = "";
	// }
	// return prefix;
	// }

	/**
	 * Menetapkan template isi surat berupa teks.
	 *
	 * @param template teks template.
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * Mengembalikan template isi surat berupa teks bebas ({@code columnDefinition = "text"}).
	 * Getter murni; dapat {@code null}.
	 *
	 * <p><b>Bukan template cetak.</b> Wujud PDF surat ditentukan berkas jrxml yang diunggah
	 * administrator dan disimpan di {@code ais.database.model.file.LampiranLain} dengan kunci
	 * {@code FILE_JRXML_LAYOUT_SURAT}; lihat
	 * {@link SuratKeluar#cetak(ais.database.model.Tbmuser, java.util.Map)}. Kolom {@code template}
	 * di sini adalah teks pendamping yang dipakai lapisan UI, bukan berkas laporan.</p>
	 *
	 * @return teks template, atau {@code null}.
	 */
	@Column(name = "template", columnDefinition = "text", nullable = true)
	public String getTemplate() {
		return template;
	}
	//
	// public String getPostfix() {
	// if (postfix == null) {
	// postfix = "";
	// }
	// return postfix;
	// }
	//
	// public void setPostfix(String postfix) {
	// this.postfix = postfix;
	// }

	/**
	 * Mengembalikan peruntukan/subjek surat, dengan <b>default destruktif</b> ke
	 * {@code SuratUtil.UMUM}.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Bila field {@code klasifikasiSuratKeluarUntuk} bernilai {@code null}, getter menuliskan
	 * {@code SuratUtil.UMUM} ke field lalu mengembalikannya. Bila tidak {@code null}, proxy lazy
	 * di-resolve lewat {@code check(...)}. Perhatikan bahwa kedua cabang saling eksklusif: pada
	 * cabang default, {@code check(...)} <b>tidak</b> dipanggil.</p>
	 *
	 * <p>Karena nilai default dituliskan ke field, memanggil getter ini pada klasifikasi yang
	 * peruntukannya belum diatur lalu menyimpan object akan mengubah kolom
	 * {@code klasifikasi_surat_keluar_untuk} dari {@code NULL} menjadi id baris "Umum". Ini
	 * biasanya yang diinginkan, tetapi tetap merupakan perubahan data yang terjadi tanpa aksi
	 * pengguna.</p>
	 *
	 * <h2>Ketergantungan pada blok static SuratUtil</h2>
	 * <p>{@code SuratUtil.UMUM} diisi oleh blok {@code static} pada
	 * {@code ais.action.master.surat.util.SuratUtil} yang mencari baris
	 * {@link KlasifikasiSuratKeluarUntuk} bernama persis {@code "Umum"} dan
	 * <b>membuatnya bila belum ada</b>. Blok tersebut membungkus seluruh isinya dengan
	 * {@code try/catch} yang hanya mencatat galat; bila inisialisasi gagal — misalnya database belum
	 * siap saat kelas dimuat — {@code SuratUtil.UMUM} tetap {@code null}. Dalam kondisi itu getter
	 * ini menuliskan {@code null} ke field dan mengembalikan {@code null}, sehingga pemanggil yang
	 * mengandalkan jaminan "tidak pernah null" — termasuk
	 * {@link #getTampilkanSemester()} pada kelas ini sendiri, yang memanggil
	 * {@code getKlasifikasiSuratKeluarUntuk().getNama()} — dapat menemui
	 * {@code NullPointerException}. {@link #getTampilkanSemester()} sudah memasang penjaga
	 * {@code != null}; kode baru sebaiknya melakukan hal yang sama alih-alih mempercayai
	 * default.</p>
	 *
	 * <h2>Konsekuensi bagi form surat</h2>
	 * <p>{@code SuratKeluarAction} membandingkan {@code getId()} hasil getter ini terhadap
	 * {@code SuratUtil.MAHASISWA}, {@code SuratUtil.SISWA}, {@code SuratUtil.DOSEN},
	 * {@code SuratUtil.GURU}, dan {@code SuratUtil.PEGAWAI} untuk menentukan baris pencarian mana
	 * yang tampil di form. Peruntukan "Umum" tidak cocok dengan satu pun di antaranya, sehingga
	 * klasifikasi tanpa peruntukan eksplisit menghasilkan form tanpa pencarian subjek — perilaku
	 * yang benar untuk surat umum, tetapi mudah disalahartikan sebagai "form rusak" bila peruntukan
	 * sebenarnya lupa diisi.</p>
	 *
	 * @return peruntukan surat; umumnya tidak {@code null}, tetapi dapat {@code null} bila
	 *         inisialisasi {@code SuratUtil} gagal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_keluar_untuk", nullable = true)
	public KlasifikasiSuratKeluarUntuk getKlasifikasiSuratKeluarUntuk() {
		if (klasifikasiSuratKeluarUntuk == null) {
			klasifikasiSuratKeluarUntuk = SuratUtil.UMUM;
		} else {
			klasifikasiSuratKeluarUntuk = check(klasifikasiSuratKeluarUntuk);
		}
		return klasifikasiSuratKeluarUntuk;
	}

	/**
	 * Menetapkan peruntukan/subjek surat. Nilai {@code null} akan diganti {@code SuratUtil.UMUM}
	 * pada pembacaan berikutnya lewat {@link #getKlasifikasiSuratKeluarUntuk()}.
	 *
	 * @param klasifikasiSuratKeluarUntuk peruntukan surat.
	 */
	public void setKlasifikasiSuratKeluarUntuk(KlasifikasiSuratKeluarUntuk klasifikasiSuratKeluarUntuk) {
		this.klasifikasiSuratKeluarUntuk = klasifikasiSuratKeluarUntuk;
	}

	/**
	 * Mengembalikan alur persetujuan berjenjang untuk surat berjenis ini, atau {@code null} bila
	 * klasifikasi ini dikonfigurasi <b>tanpa alur</b>.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Getter me-resolve proxy lazy lewat {@code check(...)}, kemudian — bila
	 * {@link #getTanpaAlur()} bernilai {@code true} — <b>menuliskan {@code null} ke field</b>
	 * {@code alurPersetujuanSuratKeluar} dan mengembalikannya. Jadi flag {@code tanpaAlur} tidak
	 * sekadar mengabaikan alur pada saat baca; ia menghapus rujukannya dari object.</p>
	 *
	 * <h2>Getter destruktif dengan risiko kehilangan konfigurasi</h2>
	 * <p>Karena {@code null} ditulis ke field dan bukan hanya dikembalikan, urutan kejadian berikut
	 * menyebabkan kehilangan data yang permanen di tabel:</p>
	 * <ol>
	 *   <li>Administrator mengaktifkan {@code tanpaAlur} pada sebuah klasifikasi yang sudah punya
	 *   alur, lalu menyimpan.</li>
	 *   <li>Getter ini terpanggil (mis. saat merender ulang daftar) dan menulis {@code null} ke
	 *   field; penyimpanan berikutnya menuliskan {@code NULL} ke kolom
	 *   {@code alur_persetujuan_surat_keluar}.</li>
	 *   <li>Administrator berubah pikiran dan menonaktifkan {@code tanpaAlur}.</li>
	 *   <li>Alur yang semula terpasang <b>tidak kembali</b> — rujukannya sudah terhapus dan harus
	 *   dipilih ulang secara manual. Bila tidak disadari, klasifikasi tersebut kini "beralur" secara
	 *   konfigurasi tetapi kosong alurnya.</li>
	 * </ol>
	 * <p>Nilai lama masih dapat ditelusuri lewat tabel revisi Envers, tetapi tidak dari data
	 * berjalan.</p>
	 *
	 * <h2>Dampak ke surat: alur klasifikasi menimpa alur surat</h2>
	 * <p>{@link SuratKeluar#getAlurPersetujuanSuratKeluar()} memeriksa klasifikasinya dan, bila
	 * klasifikasi punya alur, <b>menimpa</b> alur yang tersimpan pada surat dengan alur klasifikasi.
	 * Karena itu perubahan alur pada tabel ini berdampak surut ke surat-surat yang sudah terbit.
	 * Perhatikan asimetrinya: penimpaan hanya terjadi bila alur klasifikasi tidak {@code null}, jadi
	 * mengaktifkan {@code tanpaAlur} <b>tidak</b> menghapus alur yang sudah melekat pada surat
	 * lama — surat lama tetap memakai alur yang tersimpan pada barisnya sendiri.</p>
	 *
	 * <h2>Kaitan dengan gerbang persetujuan</h2>
	 * <p>Klasifikasi ber-{@code tanpaAlur} berarti {@link SuratKeluar} yang lahir darinya tidak
	 * memiliki {@link AlurPersetujuanSuratKeluar}. Pada
	 * {@code SuratKeluarAction.checkAlurPersetujuanSuratKeluarStatus(...)}, pembuatan baris
	 * {@link AlurPersetujuanSuratKeluarStatus} hanya dilakukan bila
	 * {@code suratKeluar.getAlurPersetujuanSuratKeluar() != null}; tanpa alur, tidak ada satu pun
	 * baris status yang dibuat. Surat tetap memperoleh nomor resmi dari {@link #getNomorSurat()} dan
	 * tetap dapat dicetak. Ini memang perilaku yang dimaksudkan untuk surat yang tidak butuh
	 * persetujuan, tetapi berarti flag ini adalah <b>sakelar yang mematikan seluruh jenjang
	 * persetujuan untuk sebuah jenis surat</b> — termasuk jenis surat yang secara kebijakan
	 * seharusnya ditandatangani pejabat berwenang. Tidak ada pembatasan role khusus untuk mengubah
	 * flag ini di luar hak akses layar CRUD klasifikasi.</p>
	 *
	 * @return alur persetujuan, atau {@code null} bila {@link #getTanpaAlur()} aktif atau memang
	 *         belum diatur.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_keluar", nullable = true)
	public AlurPersetujuanSuratKeluar getAlurPersetujuanSuratKeluar() {
		alurPersetujuanSuratKeluar = check(alurPersetujuanSuratKeluar);
		if (getTanpaAlur()) {
			alurPersetujuanSuratKeluar = null;
		}

		return alurPersetujuanSuratKeluar;
	}

	/**
	 * Menetapkan alur persetujuan berjenjang untuk surat berjenis ini. Nilai yang ditetapkan akan
	 * <b>dihapus</b> pada pembacaan berikutnya bila {@link #getTanpaAlur()} aktif — lihat
	 * {@link #getAlurPersetujuanSuratKeluar()}.
	 *
	 * @param alurPersetujuanSuratKeluar alur persetujuan.
	 */
	public void setAlurPersetujuanSuratKeluar(AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar) {
		this.alurPersetujuanSuratKeluar = alurPersetujuanSuratKeluar;
	}

	/**
	 * Mengembalikan fakultas pemilik klasifikasi, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}.
	 *
	 * <p>Bila terisi, nilai ini <b>menimpa</b> {@link SuratKeluar#getFakultas()} pada setiap surat
	 * berjenis ini, kecuali surat tersebut terkait mahasiswa (fakultas mahasiswa diprioritaskan).
	 * Penanda lingkup organisasi, bukan penyaring akses.</p>
	 *
	 * @return fakultas pemilik, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan fakultas pemilik klasifikasi.
	 *
	 * @param fakultas fakultas pemilik; boleh {@code null}.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan jurusan pemilik klasifikasi, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}. Bila terisi, menimpa {@link SuratKeluar#getJurusan()} kecuali surat
	 * terkait mahasiswa.
	 *
	 * @return jurusan pemilik, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan jurusan pemilik klasifikasi.
	 *
	 * @param jurusan jurusan pemilik; boleh {@code null}.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan mesin penomoran <b>nomor surat resmi</b> untuk jenis surat ini, setelah proxy
	 * lazy di-resolve lewat {@code check(...)}.
	 *
	 * <h2>Peran dalam penomoran</h2>
	 * <p>Inilah kaitan antara klasifikasi dan mesin {@link NomorSurat}.
	 * {@code SuratKeluarAction.generateCode(tambah, klasifikasi, tanggal)} membaca nilai ini untuk:
	 * menentukan indeks berikutnya (dari {@code NomorSurat.getNomorIndex()} bila
	 * {@code getGunakanIndexUrut()} aktif, atau dari perhitungan berbasis pencacah surat bila
	 * tidak), memformat nomor lewat {@code NomorSurat.format(index, tanggal)}, lalu menyubstitusi
	 * penanda {@code KODE_KLASIFIKASI} dengan {@link #getKode()}. Hasilnya menjadi
	 * {@link SuratKeluar#getKode()}, yaitu nomor surat yang tercetak.</p>
	 *
	 * <p>Bila getter ini mengembalikan {@code null}, {@code generateCode(...)} langsung
	 * mengembalikan string kosong — surat terbit <b>tanpa nomor</b>, bukan gagal terbit. Karena itu
	 * klasifikasi yang lupa diberi mesin penomoran menghasilkan surat bernomor kosong secara
	 * senyap.</p>
	 *
	 * <p><b>Nomor dialokasikan pada saat simpan, bukan pada saat disetujui.</b> {@code onSave}
	 * memanggil {@code generateCode(true, ...)} yang, untuk mesin ber-{@code gunakanIndexUrut},
	 * menaikkan pencacah {@link NomorSurat} secara permanen. Surat yang kemudian ditolak dalam alur
	 * persetujuan tetap memegang nomor tersebut, sehingga urutan nomor surat resmi dapat memuat
	 * lompatan yang mewakili draf yang tidak pernah terbit. Ini konsekuensi desain, bukan cacat
	 * yang tersembunyi, tetapi perlu diketahui saat mengaudit kesinambungan penomoran.</p>
	 *
	 * @return mesin penomoran nomor surat, atau {@code null} bila belum diatur.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Menetapkan mesin penomoran nomor surat resmi.
	 *
	 * @param nomorSurat mesin penomoran; boleh {@code null}, yang berarti surat terbit tanpa nomor.
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

	/**
	 * Menyatakan apakah klasifikasi masih dapat dipakai, dengan <b>default destruktif</b>
	 * {@code true}: bila field {@code aktif} bernilai {@code null}, getter menuliskan {@code true}
	 * ke field lalu mengembalikannya.
	 *
	 * <p>Arah default-nya perlu diperhatikan: klasifikasi yang belum pernah dikonfigurasi otomatis
	 * dianggap <b>aktif</b>, bukan nonaktif. Karena nilainya juga ditulis balik, kolom {@code aktif}
	 * yang semula {@code NULL} akan berubah menjadi {@code true} pada penyimpanan berikutnya.</p>
	 *
	 * @return {@code true} bila klasifikasi aktif.
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan status aktif klasifikasi.
	 *
	 * @param aktif {@code true} untuk mengaktifkan.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan daftar {@code roleId} yang boleh melihat surat berjenis ini, dalam bentuk
	 * string bertanda pemisah titik koma yang <b>dinormalkan secara destruktif</b>.
	 *
	 * <h2>Bentuk kanonik dan cara kerjanya</h2>
	 * <p>Nilai disimpan sebagai rangkaian {@code roleId} yang <i>dibungkus dan dipisahkan</i> titik
	 * koma, mis. {@code ";ADMIN;TU;"}. Bentuk berpembatas di kedua ujung inilah yang membuat
	 * pencocokan token tepat mungkin dilakukan lewat {@code LIKE} biasa:
	 * {@code DasboardSurat.createSuratKeluarVisibilityCriterion(...)} mencari substring
	 * {@code ";" + roleId + ";"} dengan {@code Restrictions.ilike(..., MatchMode.ANYWHERE)},
	 * sehingga role {@code "TU"} tidak keliru cocok dengan {@code "TUGAS"}.</p>
	 *
	 * <p>Getter menyusun bentuk kanonik itu: bila nilai kosong hasilnya string kosong, selain itu
	 * nilai dibungkus titik koma di kedua ujung, lalu {@code ";;"} dirapatkan menjadi {@code ";"}
	 * sebanyak <b>tiga kali berturut-turut</b>, disusul serangkaian pemeriksaan harfiah terhadap
	 * {@code ";"}, {@code ";;"}, {@code ";;;"}, dan {@code ";;;;"} yang masing-masing dijadikan
	 * string kosong. Hasil akhirnya ditulis balik ke field.</p>
	 *
	 * <h2>Pembersihan yang tidak lengkap secara struktural</h2>
	 * <p>Pengulangan tiga kali dan daftar pemeriksaan harfiah sampai empat titik koma adalah
	 * pendekatan tambal, bukan normalisasi menyeluruh. Rangkaian titik koma yang lebih panjang dari
	 * yang diantisipasi tidak sepenuhnya dirapatkan — mis. masukan berisi enam titik koma beruntun
	 * dapat menyisakan {@code ";;"} setelah tiga putaran. Sisa {@code ";;"} tidak membahayakan
	 * pencocokan token (tetap tidak cocok dengan {@code ";roleId;"} mana pun) tetapi membuat data
	 * tidak kanonik dan menyulitkan perbandingan antarbaris. Pola pembersihan yang persis sama —
	 * termasuk pengulangan tiga kali dan daftar harfiah empat tingkatnya — muncul pula pada
	 * {@link SuratKeluar#getUsernamePengguna()} dan {@link SuratKeluar#getSuratMasuks()} dengan
	 * pemisah koma; ketiganya salinan satu sama lain.</p>
	 *
	 * <h2>Sifat destruktif dan implikasi keamanan</h2>
	 * <p>Karena hasil normalisasi ditulis balik ke field, sekadar membaca getter ini mengubah state
	 * object; bila object kemudian tersimpan, kolom {@code kode_grup_pengguna} ikut berubah. Untuk
	 * kolom yang berfungsi sebagai daftar kendali akses, perubahan yang terjadi tanpa aksi pengguna
	 * layak diketahui — meskipun dalam hal ini perubahannya hanya berupa perapian pemisah, bukan
	 * penambahan atau penghapusan role.</p>
	 *
	 * <p>Perlu ditegaskan bahwa kolom ini hanya salah satu cabang dari kriteria kelihatan-tidaknya
	 * surat, dan bahwa kriteria penyusunnya <b>gagal-membuka</b>: bentuk pengguna yang tidak
	 * tertangani cabang mana pun, maupun exception saat menyusun kriteria, sama-sama berujung pada
	 * kriteria "selalu benar". Nilai kosong pada kolom ini sendiri bersifat menutup untuk cabang
	 * berbasis role (string kosong tidak cocok dengan {@code ";roleId;"} mana pun), jadi risiko
	 * keterbukaan berasal dari struktur method penyusun kriteria, bukan dari kolom ini. Pola
	 * gagal-membuka tersebut sudah tercatat pada {@link KlasifikasiSuratMasuk}.</p>
	 *
	 * @return daftar role dalam bentuk kanonik berpemisah titik koma; tidak pernah {@code null}.
	 */
	@Column(name = "kode_grup_pengguna", columnDefinition = "text", nullable = true)
	public String getKodeGrupPengguna() {
		if (kodeGrupPengguna == null) {
			kodeGrupPengguna = "";
		}

		kodeGrupPengguna = (kodeGrupPengguna == null || kodeGrupPengguna.trim().equalsIgnoreCase(";") ? ""
				: ";" + kodeGrupPengguna.trim() + ";").replaceAll(";;", ";").replaceAll(";;", ";")
				.replaceAll(";;", ";");

		if (kodeGrupPengguna.equals(";")) {
			kodeGrupPengguna = "";
		} else if (kodeGrupPengguna.equals(";;")) {
			kodeGrupPengguna = "";
		} else if (kodeGrupPengguna.equals(";;;")) {
			kodeGrupPengguna = "";
		} else if (kodeGrupPengguna.equals(";;;;")) {
			kodeGrupPengguna = "";
		}

		return kodeGrupPengguna;
	}

	/**
	 * Menetapkan daftar {@code roleId} yang boleh melihat surat berjenis ini. Setter polos: nilai
	 * disimpan apa adanya dan baru dinormalkan pada pembacaan berikutnya lewat
	 * {@link #getKodeGrupPengguna()}. Tidak memvalidasi bahwa {@code roleId} yang dituliskan benar
	 * ada di tabel {@code Tbmrole}; role yang tidak dikenal hanya diabaikan saat ditampilkan.
	 *
	 * @param kodeGrupPengguna daftar role berpemisah titik koma.
	 */
	public void setKodeGrupPengguna(String kodeGrupPengguna) {
		this.kodeGrupPengguna = kodeGrupPengguna;
	}

	/**
	 * Mengembalikan sifat surat, dengan <b>default destruktif</b> {@code "Biasa"}: bila
	 * {@code sifat} kosong atau hanya berisi spasi, getter menuliskan {@code "Biasa"} ke field lalu
	 * mengembalikannya.
	 *
	 * <p>Nilainya berupa teks bebas, bukan enumerasi — tidak ada daftar nilai sah yang ditegakkan
	 * entity, dan tidak ada logika di paket ini yang mengubah perilaku berdasarkan sifat surat.
	 * Praktisnya kolom ini bersifat informatif: ia dicetak pada surat lewat template dan
	 * ditampilkan di daftar, tetapi tidak membatasi siapa pun. Khususnya, sifat
	 * {@code "Rahasia"} <b>tidak</b> mempersempit hak lihat — pembatasan hak lihat sepenuhnya
	 * ditentukan {@link #getKodeGrupPengguna()}.</p>
	 *
	 * @return sifat surat; tidak pernah kosong, minimal {@code "Biasa"}.
	 */
	public String getSifat() {
		if (sifat == null || sifat.trim().isEmpty()) {
			sifat = "Biasa";
		}
		return sifat;
	}

	/**
	 * Menetapkan sifat surat. Nilai kosong akan diganti {@code "Biasa"} pada pembacaan berikutnya.
	 *
	 * @param sifat sifat surat.
	 */
	public void setSifat(String sifat) {
		this.sifat = sifat;
	}

	/**
	 * Mengembalikan kode item biaya yang dikaitkan dengan surat berjenis ini, dengan <b>normalisasi
	 * destruktif</b> {@code null} menjadi string kosong.
	 *
	 * <p>Dipakai bersama {@link #getSekaliBayar()} dan sepasang flag pelunasan untuk mengaitkan
	 * penerbitan surat dengan tagihan mahasiswa. Nilainya berupa kode teks, bukan relasi entity,
	 * sehingga tidak ada jaminan referensial: kode yang tidak lagi ada di modul keuangan tetap
	 * tersimpan di sini tanpa keluhan.</p>
	 *
	 * @return kode item biaya; tidak pernah {@code null}.
	 */
	public String getKodeItemBiaya() {
		if (kodeItemBiaya == null) {
			kodeItemBiaya = "";
		}
		return kodeItemBiaya;
	}

	/**
	 * Menetapkan kode item biaya untuk surat berjenis ini. Tanpa validasi terhadap modul keuangan.
	 *
	 * @param kodeItemBiaya kode item biaya.
	 */
	public void setKodeItemBiaya(String kodeItemBiaya) {
		this.kodeItemBiaya = kodeItemBiaya;
	}

	/**
	 * Menyatakan apakah mahasiswa harus lunas pada semester berjalan sebelum surat berjenis ini
	 * boleh dibuat, dengan normalisasi {@code null} menjadi {@code false} (tidak destruktif).
	 *
	 * <p>Beririsan tetapi tidak identik dengan
	 * {@link KlasifikasiSuratKeluarUntuk#getMahasiswaHarusTelahMembayar()} yang berada pada entity
	 * peruntukan; keduanya dapat aktif bersamaan dan diperiksa di tempat yang berbeda.</p>
	 *
	 * @return {@code true} bila pelunasan semester berjalan disyaratkan.
	 */
	public Boolean getHarusBayarLunasSmtSaatIni() {
		return harusBayarLunasSmtSaatIni == null ? false : harusBayarLunasSmtSaatIni;
	}

	/**
	 * Menetapkan syarat pelunasan semester berjalan.
	 *
	 * @param harusBayarLunasSmtSaatIni {@code true} untuk mensyaratkan.
	 */
	public void setHarusBayarLunasSmtSaatIni(Boolean harusBayarLunasSmtSaatIni) {
		this.harusBayarLunasSmtSaatIni = harusBayarLunasSmtSaatIni;
	}

	/**
	 * Menyatakan apakah mahasiswa harus lunas pada semester sebelumnya, dengan normalisasi
	 * {@code null} menjadi {@code false} (tidak destruktif).
	 *
	 * @return {@code true} bila pelunasan semester lalu disyaratkan.
	 */
	public Boolean getHarusBayarLunasSmtLalu() {
		return harusBayarLunasSmtLalu == null ? false : harusBayarLunasSmtLalu;
	}

	/**
	 * Menetapkan syarat pelunasan semester sebelumnya.
	 *
	 * @param harusBayarLunasSmtLalu {@code true} untuk mensyaratkan.
	 */
	public void setHarusBayarLunasSmtLalu(Boolean harusBayarLunasSmtLalu) {
		this.harusBayarLunasSmtLalu = harusBayarLunasSmtLalu;
	}

	/**
	 * Menyatakan apakah biaya surat berjenis ini cukup dibayar sekali (bukan setiap penerbitan),
	 * dengan normalisasi {@code null} menjadi {@code false} (tidak destruktif).
	 *
	 * @return {@code true} bila cukup dibayar sekali.
	 */
	public Boolean getSekaliBayar() {
		return sekaliBayar == null ? false : sekaliBayar;
	}

	/**
	 * Menetapkan apakah biaya surat cukup dibayar sekali.
	 *
	 * @param sekaliBayar {@code true} bila cukup sekali.
	 */
	public void setSekaliBayar(Boolean sekaliBayar) {
		this.sekaliBayar = sekaliBayar;
	}

	/**
	 * Mengembalikan satuan kerja pemilik klasifikasi, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}.
	 *
	 * <p>Berbeda dengan {@link KlasifikasiSuratKeluarPunyaTembusan#getSatuanKerja()} dan
	 * {@link KlasifikasiSuratKeluarPunyaJenisJabatan#getSatuanKerja()}, getter ini <b>tidak</b>
	 * melakukan auto-isi dari sesi pengguna; nilainya harus ditetapkan eksplisit.</p>
	 *
	 * <p>Bila terisi, nilai ini <b>menimpa</b> {@link SuratKeluar#getSatuanKerja()} pada setiap
	 * surat berjenis ini. Berfungsi sebagai penanda tenant/unit, bukan penyaring akses: entity tidak
	 * menolak pembacaan lintas satuan kerja, dan penyaringan sepenuhnya bergantung pada Criteria
	 * yang disusun lapisan Action.</p>
	 *
	 * @return satuan kerja pemilik, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik klasifikasi.
	 *
	 * @param satuanKerja satuan kerja pemilik; boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan sekolah pemilik klasifikasi, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}. Bila terisi, menimpa {@link SuratKeluar#getSekolah()}.
	 *
	 * @return sekolah pemilik, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik klasifikasi, dengan penjaga: object {@code Sekolah} yang belum
	 * tersimpan (id {@code null}) diperlakukan sebagai {@code null} agar Hibernate tidak mencoba
	 * menyimpan entity transien lewat cascade {@code PERSIST}. Pola penjaga yang sama dipakai
	 * {@link #setYayasan(Yayasan)} dan {@link SuratKeluar#setSekolah(Sekolah)}.
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object tanpa id dianggap tidak ada.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik klasifikasi, setelah proxy lazy di-resolve lewat
	 * {@code check(...)}. Bila terisi, menimpa {@link SuratKeluar#getYayasan()}.
	 *
	 * @return yayasan pemilik, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan pemilik klasifikasi, dengan penjaga entity transien yang sama seperti
	 * {@link #setSekolah(Sekolah)}.
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object tanpa id dianggap tidak ada.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan mesin penomoran <b>nomor agenda</b> untuk jenis surat ini, setelah proxy lazy
	 * di-resolve lewat {@code check(...)}.
	 *
	 * <p>Nomor agenda adalah penomoran internal yang berjalan terpisah dari nomor surat resmi:
	 * {@code SuratKeluarAction.generateCodeAgenda(...)} memakai {@link NomorSurat} yang berbeda,
	 * menaikkan pencacahnya sendiri, dan hasilnya disimpan ke {@link SuratKeluar#getAgenda()}
	 * alih-alih {@link SuratKeluar#getKode()}. Karena kedua mesin terpisah, nomor surat dan nomor
	 * agenda dapat berselisih dan memang lazim berselisih.</p>
	 *
	 * <p>Bila {@code null}, {@code generateCodeAgenda(...)} mengembalikan string kosong dan surat
	 * terbit tanpa nomor agenda — kondisi normal untuk instansi yang tidak memakai agenda.
	 * {@code SuratKeluarAction} menyembunyikan kolom agenda pada form ketika nilainya kosong.</p>
	 *
	 * @return mesin penomoran nomor agenda, atau {@code null} bila tidak dipakai.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_agenda", nullable = true)
	public NomorSurat getNomorAgenda() {
		nomorAgenda = check(nomorAgenda);
		return nomorAgenda;
	}

	/**
	 * Menetapkan mesin penomoran nomor agenda.
	 *
	 * @param nomorAgenda mesin penomoran agenda; boleh {@code null}.
	 */
	public void setNomorAgenda(NomorSurat nomorAgenda) {
		this.nomorAgenda = nomorAgenda;
	}

	/**
	 * Mengembalikan perihal bawaan surat, dengan <b>fallback ke {@link #getNama()}</b> bila
	 * {@code perihalDefault} kosong. Fallback ini tidak destruktif — nilai tidak ditulis balik ke
	 * field, sehingga kolom {@code perihal_default} tetap {@code NULL} di database.
	 *
	 * <p>Nilai ini dipakai {@link SuratKeluar#getPerihal()} sebagai perihal bawaan surat yang
	 * perihalnya belum diisi — dan di sana fallback-nya <b>destruktif</b>: nilai ditulis ke field
	 * {@code perihal} milik surat. Jadi rantai lengkapnya adalah: nama klasifikasi &rarr; perihal
	 * bawaan klasifikasi &rarr; perihal surat, dengan penulisan permanen hanya terjadi pada langkah
	 * terakhir.</p>
	 *
	 * <p>Konsekuensi yang perlu diketahui: mengganti nama klasifikasi mengubah perihal bawaan bagi
	 * surat <b>baru</b>, tetapi tidak mengubah surat lama yang perihalnya sudah terlanjur tertulis.
	 * Dua surat berjenis sama dari periode berbeda karenanya dapat berperihal berbeda tanpa ada
	 * yang menyuntingnya.</p>
	 *
	 * @return perihal bawaan; jatuh ke nama klasifikasi bila belum diisi.
	 */
	@Column(name = "perihal_default", columnDefinition = "text", nullable = true)
	public String getPerihalDefault() {
		return perihalDefault == null || perihalDefault.trim().isEmpty() ? getNama() : perihalDefault;
	}

	/**
	 * Menetapkan perihal bawaan surat. Nilai kosong berarti perihal bawaan mengikuti
	 * {@link #getNama()}.
	 *
	 * @param perihalDefault perihal bawaan.
	 */
	public void setPerihalDefault(String perihalDefault) {
		this.perihalDefault = perihalDefault;
	}

	/**
	 * Menyatakan apakah surat berjenis ini terbit <b>tanpa alur persetujuan</b>, dengan normalisasi
	 * {@code null} menjadi {@code false} (tidak destruktif).
	 *
	 * <p>Flag ini adalah sakelar yang dibaca {@link #getAlurPersetujuanSuratKeluar()} untuk
	 * menghapus rujukan alur dari object. Baca Javadoc getter tersebut untuk konsekuensi lengkapnya,
	 * termasuk kehilangan konfigurasi alur yang bersifat permanen di data berjalan dan akibatnya
	 * terhadap pembuatan baris {@link AlurPersetujuanSuratKeluarStatus}.</p>
	 *
	 * @return {@code true} bila jenis surat ini tidak memakai alur persetujuan.
	 */
	public Boolean getTanpaAlur() {
		return tanpaAlur == null ? false : tanpaAlur;
	}

	/**
	 * Menetapkan apakah surat berjenis ini terbit tanpa alur persetujuan. Mengaktifkannya akan
	 * menyebabkan {@link #getAlurPersetujuanSuratKeluar()} menghapus rujukan alur yang tersimpan.
	 *
	 * @param tanpaAlur {@code true} untuk menerbitkan tanpa alur persetujuan.
	 */
	public void setTanpaAlur(Boolean tanpaAlur) {
		this.tanpaAlur = tanpaAlur;
	}

	/**
	 * Menyatakan apakah surat berjenis ini terbit tanpa template, dengan normalisasi {@code null}
	 * menjadi {@code false} (tidak destruktif).
	 *
	 * <p>Berbeda dari {@link #getTanpaAlur()}, flag ini <b>tidak</b> menghapus apa pun dari object;
	 * ia murni penanda yang dibaca lapisan UI.</p>
	 *
	 * @return {@code true} bila jenis surat ini tidak memakai template.
	 */
	public Boolean getTanpaTemplate() {
		return tanpaTemplate == null ? false : tanpaTemplate;
	}

	/**
	 * Menetapkan apakah surat berjenis ini terbit tanpa template.
	 *
	 * @param tanpaTemplate {@code true} untuk menerbitkan tanpa template.
	 */
	public void setTanpaTemplate(Boolean tanpaTemplate) {
		this.tanpaTemplate = tanpaTemplate;
	}

	/**
	 * Menyatakan apakah mahasiswa harus berstatus aktif kuliah agar surat berjenis ini boleh
	 * dibuat, dengan normalisasi {@code null} menjadi {@code false} (tidak destruktif).
	 *
	 * @return {@code true} bila status aktif kuliah disyaratkan.
	 */
	public Boolean getAktifKuliah() {
		return aktifKuliah == null ? false : aktifKuliah;
	}

	/**
	 * Menetapkan syarat status aktif kuliah.
	 *
	 * @param aktifKuliah {@code true} untuk mensyaratkan.
	 */
	public void setAktifKuliah(Boolean aktifKuliah) {
		this.aktifKuliah = aktifKuliah;
	}

	/**
	 * Menyatakan apakah pemilih semester ditampilkan pada form surat; bila belum pernah diatur,
	 * nilainya <b>disimpulkan dari nama peruntukan</b>.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Bila field {@code tampilkanSemester} tidak {@code null}, nilainya dipakai apa adanya. Bila
	 * {@code null}, hasilnya {@code true} apabila {@link #getKlasifikasiSuratKeluarUntuk()} terisi
	 * dan namanya — dibandingkan <i>case-insensitive</i> — sama dengan salah satu dari
	 * {@code "mahasiswa"}, {@code "siswa"}, {@code "dosen"}, atau {@code "guru"}. Peruntukan
	 * "Pegawai" dan "Umum" karenanya tidak memunculkan pemilih semester secara bawaan, yang masuk
	 * akal karena keduanya tidak terikat kalender akademik.</p>
	 *
	 * <p>Penyimpulan ini <b>tidak destruktif</b>: hasilnya tidak ditulis balik ke field, sehingga
	 * kolom tetap {@code NULL} dan penyimpulan diulang pada setiap pemanggilan. Nilai yang
	 * disimpulkan karenanya selalu mengikuti peruntukan terkini — bila peruntukan klasifikasi
	 * diubah, tampil-tidaknya pemilih semester ikut berubah tanpa perlu menyunting flag ini.</p>
	 *
	 * <h2>Ketergantungan pada teks nama, bukan pada id</h2>
	 * <p>Perbandingan dilakukan terhadap <b>nama</b> peruntukan, bukan terhadap
	 * {@code SuratUtil.MAHASISWA.getId()} dan kawan-kawannya seperti yang dilakukan
	 * {@code SuratKeluarAction}. Dua mekanisme pencocokan yang berbeda atas konsep yang sama ini
	 * membuat keduanya bisa berselisih: baris peruntukan yang namanya diubah menjadi
	 * {@code "Mahasiswa Aktif"} tidak lagi cocok di sini (pemilih semester hilang) sementara
	 * pencocokan berbasis id di {@code SuratKeluarAction} masih cocok selama baris tersebut belum
	 * ter-seed ganda. Sebaliknya nama {@code "MAHASISWA"} berhuruf besar tetap cocok di sini tetapi
	 * memicu penambahan baris duplikat oleh {@code SuratUtil}. Baca
	 * {@link KlasifikasiSuratKeluarUntuk#getNama()} untuk rangkaian akibatnya.</p>
	 *
	 * <p>Perhatikan pula bahwa {@code getNama()} pada peruntukan dapat mengembalikan {@code null}
	 * bila nama belum diisi; rantai {@code getKlasifikasiSuratKeluarUntuk().getNama().equalsIgnoreCase(...)}
	 * akan melempar {@code NullPointerException} pada kasus itu. Penjaga yang terpasang hanya
	 * memeriksa peruntukannya sendiri tidak {@code null}, bukan namanya.</p>
	 *
	 * @return {@code true} bila pemilih semester ditampilkan.
	 */
	public Boolean getTampilkanSemester() {
		return tampilkanSemester == null
				? (getKlasifikasiSuratKeluarUntuk() != null
						&& (getKlasifikasiSuratKeluarUntuk().getNama().equalsIgnoreCase("mahasiswa")
								|| getKlasifikasiSuratKeluarUntuk().getNama().equalsIgnoreCase("siswa")
								|| getKlasifikasiSuratKeluarUntuk().getNama().equalsIgnoreCase("dosen")
								|| getKlasifikasiSuratKeluarUntuk().getNama().equalsIgnoreCase("guru")))
				: tampilkanSemester;
	}

	/**
	 * Menetapkan apakah pemilih semester ditampilkan. Menetapkan nilai eksplisit — termasuk
	 * {@code false} — mematikan penyimpulan otomatis berbasis nama peruntukan.
	 *
	 * @param tampilkanSemester {@code true} untuk menampilkan; {@code null} untuk kembali ke
	 *                          penyimpulan otomatis.
	 */
	public void setTampilkanSemester(Boolean tampilkanSemester) {
		this.tampilkanSemester = tampilkanSemester;
	}

	/**
	 * Mengembalikan penanda tipe/kelompok klasifikasi berupa teks bebas. Getter murni tanpa
	 * normalisasi; dapat {@code null}.
	 *
	 * <p>Berpasangan dengan {@link SuratKeluar#getTipe()} yang bentuknya sama. Nilainya tidak
	 * dibatasi enumerasi mana pun di level entity.</p>
	 *
	 * @return penanda tipe, atau {@code null}.
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Menetapkan penanda tipe/kelompok klasifikasi.
	 *
	 * @param tipe penanda tipe.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Menyatakan apakah surat berjenis ini wajib dikaitkan dengan surat lain, dengan normalisasi
	 * {@code null} menjadi {@code false} (tidak destruktif).
	 *
	 * <p>Bila aktif, {@code SuratKeluarAction.onSave(...)} menolak penyimpanan selama
	 * {@link SuratKeluar#getSuratSebelumnya()} belum dipilih, dengan pesan yang memakai sebutan dari
	 * {@link #getIstilahSuratLain()}. Ini salah satu prasyarat yang benar-benar ditegakkan pada
	 * jalur simpan.</p>
	 *
	 * @return {@code true} bila kaitan ke surat lain diwajibkan.
	 */
	public Boolean getKaitkanDenganSuratLain() {
		return kaitkanDenganSuratLain == null ? false : kaitkanDenganSuratLain;
	}

	/**
	 * Menetapkan apakah kaitan ke surat lain diwajibkan.
	 *
	 * @param kaitkanDenganSuratLain {@code true} untuk mewajibkan.
	 */
	public void setKaitkanDenganSuratLain(Boolean kaitkanDenganSuratLain) {
		this.kaitkanDenganSuratLain = kaitkanDenganSuratLain;
	}

	/**
	 * Mengembalikan sebutan yang dipakai UI untuk surat terkait, dengan fallback tidak destruktif
	 * ke {@code "Surat Sebelumnya"}.
	 *
	 * <p>Murni kosmetik: dipakai sebagai label kolom dan sebagai kata ganti dalam pesan peringatan
	 * ketika {@link #getKaitkanDenganSuratLain()} aktif. Instansi yang menyebut kaitannya
	 * "Surat Induk" atau "Surat Dasar" dapat menyesuaikannya di sini tanpa efek samping.</p>
	 *
	 * @return sebutan surat terkait; tidak pernah {@code null}.
	 */
	public String getIstilahSuratLain() {
		return istilahSuratLain == null ? "Surat Sebelumnya" : istilahSuratLain;
	}

	/**
	 * Menetapkan sebutan surat terkait.
	 *
	 * @param istilahSuratLain sebutan; kosong berarti memakai "Surat Sebelumnya".
	 */
	public void setIstilahSuratLain(String istilahSuratLain) {
		this.istilahSuratLain = istilahSuratLain;
	}

	/**
	 * Mengembalikan batas <b>awal</b> jendela waktu pencetakan surat berjenis ini. Getter murni;
	 * {@code null} berarti tidak ada batas awal.
	 *
	 * <h2>PERINGATAN: arah perbandingan pada penegakannya terbalik</h2>
	 * <p>Satu-satunya tempat nilai ini ditegakkan adalah {@code SuratKeluarAction.onSave(...)}.
	 * Kode di sana memformat kedua tanggal dengan pola {@code "yyMMddHHmmss"}, mengubahnya menjadi
	 * {@code double}, lalu membandingkannya sebagai angka. Kondisi yang dipakai adalah:</p>
	 * <pre>if (bisaDicetakMulai != null &amp;&amp; angka(bisaDicetakMulai) &lt; angka(sekarang)) { tolak; }</pre>
	 * <p>Artinya penyimpanan <b>ditolak justru ketika waktu sekarang sudah melewati batas awal</b> —
	 * yaitu tepat ketika seharusnya diizinkan. Pesan yang muncul berbunyi "baru dapat dicetak
	 * setelah tanggal X", padahal tanggal X sudah lewat. Kondisi yang benar semestinya
	 * {@code angka(bisaDicetakMulai) > angka(sekarang)}, yaitu menolak selama batas awal masih di
	 * masa depan. Perbandingan pada {@link #getBisaDicetakSampai()} terbalik dengan cara yang
	 * bercermin.</p>
	 *
	 * <p>Efek gabungannya: jendela waktu yang dikonfigurasi bekerja <b>berkebalikan</b> — surat
	 * tidak dapat dibuat selama berada di dalam jendela, dan dapat dibuat di luar jendela.
	 * Klasifikasi yang kedua batasnya diisi praktis tidak dapat dipakai sama sekali selama jendela
	 * masih berjalan.</p>
	 *
	 * <h2>Catatan lain</h2>
	 * <ul>
	 *   <li>Pemeriksaannya berada di jalur <b>simpan</b> ({@code onSave}), bukan di jalur cetak,
	 *   meskipun seluruh pesannya berbicara tentang pencetakan. Surat yang sudah tersimpan tetap
	 *   dapat dicetak ulang kapan saja lewat tombol cetak maupun lewat
	 *   {@code ais.action.servlet.api.SuratApi}, tanpa pemeriksaan jendela apa pun.</li>
	 *   <li>Pola {@code "yyMMddHHmmss"} memakai tahun dua digit, sehingga perbandingan numeriknya
	 *   akan berurutan keliru untuk tanggal melewati tahun 2099.</li>
	 *   <li>Kolom dipetakan {@code TIMESTAMP} sehingga bagian jam ikut diperhitungkan, bukan
	 *   perbandingan per hari.</li>
	 * </ul>
	 *
	 * @return batas awal jendela pencetakan, atau {@code null} bila tidak dibatasi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getBisaDicetakMulai() {
		return bisaDicetakMulai;
	}

	/**
	 * Menetapkan batas awal jendela waktu pencetakan. Perhatikan bahwa penegakannya terbalik —
	 * lihat {@link #getBisaDicetakMulai()}.
	 *
	 * @param bisaDicetakMulai batas awal; {@code null} berarti tidak dibatasi.
	 */
	public void setBisaDicetakMulai(Date bisaDicetakMulai) {
		this.bisaDicetakMulai = bisaDicetakMulai;
	}

	/**
	 * Mengembalikan batas <b>akhir</b> jendela waktu pencetakan surat berjenis ini. Getter murni;
	 * {@code null} berarti tidak ada batas akhir.
	 *
	 * <p><b>Arah perbandingan pada penegakannya juga terbalik.</b>
	 * {@code SuratKeluarAction.onSave(...)} memakai kondisi</p>
	 * <pre>if (bisaDicetakSampai != null &amp;&amp; angka(bisaDicetakSampai) &gt; angka(sekarang)) { tolak; }</pre>
	 * <p>yang menolak penyimpanan <b>selama batas akhir masih di masa depan</b> — yaitu ketika surat
	 * seharusnya masih boleh dibuat — dan mengizinkannya setelah batas akhir terlampaui. Kondisi
	 * yang benar semestinya {@code angka(bisaDicetakSampai) < angka(sekarang)}. Lihat
	 * {@link #getBisaDicetakMulai()} untuk uraian lengkap dan catatan tambahan (pemeriksaan berada
	 * di jalur simpan, bukan cetak; pola tahun dua digit).</p>
	 *
	 * @return batas akhir jendela pencetakan, atau {@code null} bila tidak dibatasi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getBisaDicetakSampai() {
		return bisaDicetakSampai;
	}

	/**
	 * Menetapkan batas akhir jendela waktu pencetakan. Perhatikan bahwa penegakannya terbalik —
	 * lihat {@link #getBisaDicetakSampai()}.
	 *
	 * @param bisaDicetakSampai batas akhir; {@code null} berarti tidak dibatasi.
	 */
	public void setBisaDicetakSampai(Date bisaDicetakSampai) {
		this.bisaDicetakSampai = bisaDicetakSampai;
	}

}
