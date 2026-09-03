package ais.database.model.inventory;

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

/**
 * <h2>SesiKasKasir — Sesi Kas Kasir (Buka / Tutup Kas) untuk Toko/Kantin.</h2>
 *
 * <p>
 * Entity BARU untuk mencatat <b>buka</b> dan <b>tutup kas</b> harian tiap kasir, sehingga akurasi kas
 * dapat dikontrol (laporan "Buka Tutup Kasir" dan "Selisih Kasir" pada katalog laporan Toko/Kantin).
 * Sebelumnya sistem hanya punya rekap penjualan per kasir (dari transaksi POS), namun belum ada
 * konsep <i>sesi kas</i> dengan modal awal, uang fisik saat tutup, dan selisih. Dengan adanya entity
 * ini + pendaftaran di {@code hibernate.cfg.xml}, tabel {@code koperasi.sesi_kas_kasir} otomatis
 * dibuat (hbm2ddl=update).
 * </p>
 *
 * <h3>Alur & perhitungan</h3>
 * <ul>
 *   <li><b>Buka kas:</b> kasir membuka sesi dengan mengisi <i>modal awal</i>; {@code status}=BUKA,
 *       {@code waktuBuka}=sekarang.</li>
 *   <li><b>Tutup kas:</b> saat menutup, sistem menghitung <i>total tunai</i> dan <i>total non‑tunai</i>
 *       dari transaksi POS oleh kasir yang sama dalam rentang {@code waktuBuka}..{@code waktuTutup}
 *       (dicocokkan lewat {@link #getKasirNama()}/{@link #getKasirUserId()} = identitas kasir pada
 *       transaksi -- BUKAN {@code oleh}/{@code olehId}, itu audit generik, lihat javadoc
 *       {@link #getKasirNama()}), lalu kasir mengisi <i>uang fisik</i> yang dihitung. <b>Selisih</b> =
 *       uangFisik − (modalAwal + totalTunai). {@code status}=TUTUP.</li>
 * </ul>
 *
 * <p>
 * Penamaan kolom mengikuti aturan Hibernate proyek ini: field ber-@Column memakai nama eksplisit,
 * sementara field numerik/tanggal tanpa @Column ter-<i>fold</i> menjadi huruf kecil tanpa underscore
 * (mis. {@code modalAwal}→{@code modalawal}, {@code totalTunai}→{@code totaltunai},
 * {@code uangFisik}→{@code uangfisik}, {@code waktuBuka}→{@code waktubuka}). Kompatibel Java 1.7 /
 * Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul kas kasir)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sesi_kas_kasir")
public class SesiKasKasir extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Status sesi masih terbuka (kas belum ditutup). */
	public static final String STATUS_BUKA = "BUKA";
	/** Status sesi sudah ditutup (kas selesai dihitung). */
	public static final String STATUS_TUTUP = "TUTUP";

	/** Primary key baris {@code koperasi.sesi_kas_kasir}. Lihat {@link #getId()}. */
	private Long id;
	/** Toko/kantin tempat sesi kas ini dibuka. Lihat {@link #getToko()}. */
	private Toko toko;
	/**
	 * Nama pengguna pengubah terakhir -- field audit generik <b>shadow</b> milik
	 * {@link GeneralValueObject} (WAJIB dideklarasikan ulang per entity konkret, lihat javadoc
	 * {@link GeneralValueObject#getOleh()}). <b>BUKAN</b> identitas kasir pemilik sesi -- lihat
	 * {@link #getKasirNama()} untuk field yang benar dipakai sebagai identitas bisnis, dan javadoc
	 * di sana untuk kronologi bug yang terjadi akibat tertukarnya dua field ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir -- field shadow dengan peringatan sama seperti {@link #oleh}. */
	private String olehId;
	private String kasirNama;
	private String kasirUserId;
	private String idPerangkat;
	private String namaPerangkat;
	/** Waktu sesi dibuka. Lihat {@link #getWaktuBuka()}. */
	private Date waktuBuka;
	/** Waktu sesi ditutup; {@code null} selagi sesi masih {@link #STATUS_BUKA}. Lihat {@link #getWaktuTutup()}. */
	private Date waktuTutup;
	/** Modal awal yang diisi kasir saat membuka sesi. Lihat {@link #getModalAwal()}. */
	private Double modalAwal;
	/** Total transaksi tunai POS selama sesi, dihitung sistem saat tutup. Lihat {@link #getTotalTunai()}. */
	private Double totalTunai;
	/** Total transaksi non-tunai POS selama sesi, dihitung sistem saat tutup. Lihat {@link #getTotalNonTunai()}. */
	private Double totalNonTunai;
	/** Uang fisik hasil hitung kasir saat menutup sesi. Lihat {@link #getUangFisik()}. */
	private Double uangFisik;
	/** Selisih antara uang fisik dan ekspektasi sistem; DICATAT, tidak memblokir apa pun. Lihat {@link #getSelisih()}. */
	private Double selisih;
	/** Status BUKA/TUTUP sesi. Lihat {@link #getStatus()}. */
	private String status;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	private String kode;
	private String laporanTutupJson;

	/**
	 * Hook {@code @PreUpdate} Hibernate: menyinkronkan {@link #tanggal_dirubah} lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui. Implementasi
	 * kontrak {@link GeneralValueObject#onUpdate()}; isinya tipis karena logika stempel waktu
	 * dipusatkan di interceptor bersama.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir -- field shadow dengan alasan sama seperti {@link #oleh}.
	 * Diinisialisasi ke waktu pembuatan object sehingga baris baru selalu punya nilai walau jalur
	 * simpan lupa mengisinya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public SesiKasKasir() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return primary key, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan {@link Toko}/kantin tempat sesi ini dibuka, dengan proxy lazy diresolusi lewat
	 * {@link #check(Object)}.
	 *
	 * @return toko terkait, atau {@code null} bila belum terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menyetel toko/kantin tempat sesi ini dibuka. Tanpa validasi.
	 *
	 * @param toko toko baru, boleh {@code null}
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Mengembalikan nama pengguna pengubah terakhir (metadata audit generik) -- <b>BUKAN</b>
	 * identitas kasir pemilik sesi, lihat {@link #getKasirNama()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Tanpa validasi penolakan nilai kosong di kelas ini
	 * (berbeda dari {@link GeneralValueObject#setOleh(String)}) -- langsung menimpa field apa
	 * adanya.
	 *
	 * @param oleh nama pengguna pengubah baru, boleh {@code null}/kosong (langsung menimpa)
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna pengubah terakhir (metadata audit generik) -- pasangan
	 * {@link #getOleh()}, sama-sama BUKAN identitas kasir pemilik sesi.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Sama seperti {@link #setOleh(String)}: tanpa
	 * validasi penolakan nilai kosong di kelas ini.
	 *
	 * @param olehId id pengguna pengubah baru, boleh {@code null}/kosong (langsung menimpa)
	 */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Nama kasir yang membuka sesi ini -- SENGAJA field baru terpisah dari {@link #getOleh()} (itu
	 * metadata audit generik "siapa terakhir mengubah baris", diisi otomatis oleh interceptor/listener
	 * Hibernate di {@code ais.database.hibernate}, TIDAK dimaksudkan untuk dibaca sebagai data bisnis).
	 * Sebelum field ini ada, {@code SesiKasUtil.buka()} menyalahgunakan {@code oleh}/{@code olehId}
	 * untuk menyimpan identitas kasir -- interceptor audit generik menimpanya diam-diam sebelum baris
	 * tersimpan (root cause bug "Kas Terbuka tapi checkout ditolak", lihat commit 869f858d), karena
	 * kedua nama itu kebetulan sama dengan nama kolom audit. Pola field terpisah ini mengikuti preseden
	 * {@link ais.database.model.koperasi.PembelianAnggotaKoperasi#getKasirLoginNama()}.
	 */
	@Column(name = "kasir_nama", nullable = true)
	public String getKasirNama() {
		return kasirNama;
	}

	public void setKasirNama(String kasirNama) {
		this.kasirNama = kasirNama;
	}

	/** Pasangan id (userId) dari {@link #getKasirNama()} -- lihat javadoc di sana. */
	@Column(name = "kasir_user_id", nullable = true)
	public String getKasirUserId() {
		return kasirUserId;
	}

	public void setKasirUserId(String kasirUserId) {
		this.kasirUserId = kasirUserId;
	}

	/**
	 * Identitas instalasi/perangkat yang membuka sesi. Nilai ini dibuat sekali oleh aplikasi POS
	 * dan tetap sama setelah aplikasi dibuka ulang. Sesi kas baru wajib terikat ke perangkat agar
	 * akun yang sama pada mesin lain tidak dapat memakai sesi ini secara tidak sengaja.
	 */
	@Column(name = "id_perangkat", nullable = true, length = 128)
	public String getIdPerangkat() {
		return idPerangkat;
	}

	public void setIdPerangkat(String idPerangkat) {
		this.idPerangkat = idPerangkat;
	}

	/** Nama perangkat saat sesi dibuka; snapshot untuk informasi operator dan audit. */
	@Column(name = "nama_perangkat", nullable = true, length = 150)
	public String getNamaPerangkat() {
		return namaPerangkat;
	}

	/**
	 * Menyetel nama perangkat. Tanpa validasi.
	 *
	 * @param namaPerangkat nama perangkat baru, boleh {@code null}
	 */
	public void setNamaPerangkat(String namaPerangkat) {
		this.namaPerangkat = namaPerangkat;
	}

	/**
	 * Mengembalikan waktu sesi ini dibuka, dengan default waktu SEKARANG bila kolom kosong
	 * (dihitung ulang tiap pemanggilan, tidak disimpan balik ke field).
	 *
	 * @return waktu buka, tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuBuka() {
		return waktuBuka == null ? ais.ui.util.WaktuUtil.getDate() : waktuBuka;
	}

	/**
	 * Menyetel waktu sesi dibuka. Tanpa validasi.
	 *
	 * @param waktuBuka waktu buka baru, boleh {@code null} (lihat {@link #getWaktuBuka()} untuk
	 *                  fallback)
	 */
	public void setWaktuBuka(Date waktuBuka) {
		this.waktuBuka = waktuBuka;
	}

	/**
	 * Mengembalikan waktu sesi ini ditutup -- <b>TANPA default</b> (berbeda dari
	 * {@link #getWaktuBuka()}): bernilai {@code null} apa adanya selagi sesi masih berstatus
	 * {@link #STATUS_BUKA}, yang justru penting sebagai penanda "sesi belum ditutup" bagi
	 * pemanggil yang mengecek kolom ini.
	 *
	 * @return waktu tutup, atau {@code null} bila sesi belum ditutup
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuTutup() {
		return waktuTutup;
	}

	/**
	 * Menyetel waktu sesi ditutup. Tanpa validasi.
	 *
	 * @param waktuTutup waktu tutup baru, boleh {@code null}
	 */
	public void setWaktuTutup(Date waktuTutup) {
		this.waktuTutup = waktuTutup;
	}

	/**
	 * Mengembalikan modal awal yang diisi kasir saat membuka sesi -- dasar perhitungan
	 * {@link #getSelisih() selisih} saat tutup (lihat rumus lengkap pada javadoc kelas: selisih =
	 * uangFisik - (modalAwal + totalTunai)).
	 *
	 * @return modal awal, {@code 0.0} bila belum diisi
	 */
	public Double getModalAwal() {
		return modalAwal == null ? 0.0 : modalAwal;
	}

	/**
	 * Menyetel modal awal. Tanpa validasi (termasuk tidak menolak nilai negatif).
	 *
	 * @param modalAwal modal awal baru, boleh {@code null}
	 */
	public void setModalAwal(Double modalAwal) {
		this.modalAwal = modalAwal;
	}

	/**
	 * Mengembalikan total transaksi tunai POS selama sesi ini -- dihitung SISTEM saat kasir
	 * menutup sesi (bukan diisi manual kasir), dicocokkan lewat {@link #getKasirNama()}/
	 * {@link #getKasirUserId()} pada rentang {@link #getWaktuBuka()}..{@link #getWaktuTutup()}
	 * (lihat javadoc kelas dan javadoc {@link #getKasirNama()} untuk kronologi bug pencocokan
	 * identitas kasir yang sempat salah memakai {@link #getOleh()}).
	 *
	 * @return total tunai, {@code 0.0} bila belum dihitung/diisi
	 */
	public Double getTotalTunai() {
		return totalTunai == null ? 0.0 : totalTunai;
	}

	/**
	 * Menyetel total transaksi tunai. Tanpa validasi -- normalnya diisi otomatis oleh proses tutup
	 * kas, bukan dipanggil manual.
	 *
	 * @param totalTunai total tunai baru, boleh {@code null}
	 */
	public void setTotalTunai(Double totalTunai) {
		this.totalTunai = totalTunai;
	}

	/**
	 * Mengembalikan total transaksi non-tunai POS selama sesi ini -- dihitung sistem dengan cara
	 * yang sama seperti {@link #getTotalTunai()}, tetapi TIDAK ikut dalam rumus
	 * {@link #getSelisih()} (hanya kas fisik/tunai yang relevan untuk selisih kas).
	 *
	 * @return total non-tunai, {@code 0.0} bila belum dihitung/diisi
	 */
	public Double getTotalNonTunai() {
		return totalNonTunai == null ? 0.0 : totalNonTunai;
	}

	/**
	 * Menyetel total transaksi non-tunai. Tanpa validasi -- normalnya diisi otomatis oleh proses
	 * tutup kas.
	 *
	 * @param totalNonTunai total non-tunai baru, boleh {@code null}
	 */
	public void setTotalNonTunai(Double totalNonTunai) {
		this.totalNonTunai = totalNonTunai;
	}

	/**
	 * Mengembalikan uang fisik yang dihitung kasir saat menutup sesi -- input MANUAL kasir (beda
	 * dari {@link #getTotalTunai()}/{@link #getTotalNonTunai()} yang dihitung sistem), dibandingkan
	 * terhadap ekspektasi sistem untuk menghasilkan {@link #getSelisih()}.
	 *
	 * @return uang fisik, {@code 0.0} bila belum diisi
	 */
	public Double getUangFisik() {
		return uangFisik == null ? 0.0 : uangFisik;
	}

	/**
	 * Menyetel uang fisik hasil hitung kasir. Tanpa validasi.
	 *
	 * @param uangFisik uang fisik baru, boleh {@code null}
	 */
	public void setUangFisik(Double uangFisik) {
		this.uangFisik = uangFisik;
	}

	/**
	 * Mengembalikan selisih kas: {@code uangFisik - (modalAwal + totalTunai)} (lihat rumus lengkap
	 * pada javadoc kelas). <b>PENTING -- pola arsitektur berulang di domain POS AIS</b>: selisih
	 * ini HANYA DICATAT untuk keperluan laporan/monitoring admin, TIDAK memblokir/menggagalkan
	 * apa pun -- tidak ada validasi yang menolak penutupan sesi karena selisih besar, dan sesi
	 * berikutnya tetap bisa dibuka meski sesi sebelumnya ditutup dengan selisih signifikan. Pola
	 * ini sama persis dengan {@code koperasi.NotaSalesSession} (sesi kasir modul Koperasi/Kantin
	 * versi lain, sudah didokumentasikan terpisah): keduanya sengaja tidak hard-block transaksi
	 * berdasarkan besar selisih kas, mengandalkan laporan "Selisih Kasir" untuk pengawasan manual
	 * oleh admin alih-alih penjagaan otomatis di jalur simpan.
	 *
	 * @return selisih kas (bisa negatif bila uang fisik kurang dari ekspektasi), {@code 0.0} bila
	 *         belum dihitung
	 */
	public Double getSelisih() {
		return selisih == null ? 0.0 : selisih;
	}

	/**
	 * Menyetel selisih kas secara langsung. Tanpa validasi -- normalnya diisi otomatis oleh proses
	 * tutup kas ({@code SesiKasUtil.tutup()}), bukan dipanggil manual.
	 *
	 * @param selisih selisih kas baru, boleh {@code null}
	 */
	public void setSelisih(Double selisih) {
		this.selisih = selisih;
	}

	/**
	 * Mengembalikan status sesi -- {@link #STATUS_BUKA} atau {@link #STATUS_TUTUP}, dengan
	 * <b>default {@link #STATUS_BUKA}</b> bila kolom belum terisi (baris baru dianggap masih
	 * terbuka sampai eksplisit ditutup).
	 *
	 * @return status sesi, tidak pernah {@code null}
	 */
	public String getStatus() {
		return status == null ? STATUS_BUKA : status;
	}

	/**
	 * Menyetel status sesi secara langsung. Tanpa validasi -- pemanggil bertanggung jawab menjaga
	 * konsistensi dengan {@link #getWaktuTutup()} (mis. tidak menyetel {@link #STATUS_TUTUP} tanpa
	 * turut mengisi waktu tutup).
	 *
	 * @param status status baru, sebaiknya {@link #STATUS_BUKA} atau {@link #STATUS_TUTUP}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan keterangan bebas sesi ini.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode idempotensi yang DIBUAT KLIEN (bukan server) -- fondasi fitur "Sesi Kasir offline-first"
	 * (Desktop/Android menyimpan sesi ke database LOKAL dulu, baru disinkronkan ke server belakangan
	 * secara berkala/otomatis, termasuk saat baru online kembali setelah sempat offline). Sinkron bisa
	 * dicoba ULANG (retry) kapan saja -- {@code kode} inilah yang membuat percobaan ulang AMAN: server
	 * ({@code KantinHelper.sesiKasBuka}) mengecek dulu apakah baris dgn {@code kode} ini SUDAH ada
	 * sebelum membuat baris baru, jadi retry jaringan yang gagal di tengah jalan (respons hilang tapi
	 * baris sebenarnya sudah tersimpan) tidak pernah menghasilkan sesi DOBEL. {@code null} utk sesi
	 * lama/sebelum fitur ini ada, atau sesi yang dibuat langsung dari versi web (JSP/ZK) yang belum
	 * (dan tidak perlu) memakai alur offline-first ini.
	 */
	@Column(name = "kode", unique = true, length = 100)
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode idempotensi. Tanpa validasi -- unik ({@code unique = true}) pada level kolom
	 * database, jadi pelanggaran keunikan akan gagal sebagai constraint violation di database,
	 * bukan dicegah di sisi entity.
	 *
	 * @param kode kode idempotensi baru, boleh {@code null} (lihat javadoc {@link #getKode()})
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Snapshot laporan saat kas ditutup. Disimpan sebagai JSON agar cetak ulang selalu memakai
	 * angka yang telah direkonsiliasi pada saat penutupan, bukan menghitung ulang data yang mungkin
	 * sudah berubah karena retur atau koreksi setelah shift berakhir.
	 */
	@Column(name = "laporan_tutup_json", columnDefinition = "text")
	public String getLaporanTutupJson() {
		return laporanTutupJson;
	}

	/**
	 * Menyetel snapshot JSON laporan tutup kas. Tanpa validasi format JSON di sisi entity --
	 * pemanggil ({@code SesiKasUtil.tutup()}) bertanggung jawab membentuk JSON yang valid.
	 *
	 * @param laporanTutupJson JSON laporan baru, boleh {@code null}
	 */
	public void setLaporanTutupJson(String laporanTutupJson) {
		this.laporanTutupJson = laporanTutupJson;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, disinkronkan oleh {@link #onUpdate()} pada
	 * tiap {@code UPDATE}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi -- normalnya diisi otomatis oleh
	 * {@link #onUpdate()}, bukan dipanggil manual.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
