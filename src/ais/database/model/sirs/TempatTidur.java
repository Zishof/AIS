package ais.database.model.sirs;

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

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;

/**
 * Entity JPA/Hibernate untuk satu tempat tidur (bed) rawat inap pada modul SIRS (Sistem Informasi
 * Rumah Sakit), dipetakan ke tabel {@code sirs.tempat_tidur}. Setiap tempat tidur berada di dalam
 * satu {@link Kamar} (yang pada gilirannya berada di dalam satu {@link Ruang}), memiliki
 * {@link KelasPerawatan} sendiri (independen dari kelas kamar induknya — lihat catatan pada
 * {@link #getKelasPerawatan()}), serta status administratif via {@link StatusTempatTidur}
 * (mis. tersedia/dibersihkan/rusak — daftar nilai konkretnya berada di data master, bukan enum
 * Java).
 *
 * <h2>WASPADA — {@code terisi} adalah flag persisten yang SALAH DIHITUNG oleh {@link #updateTerisi()}</h2>
 * <p>
 * Okupansi (apakah bed sedang ditempati) TIDAK dihitung on-the-fly setiap kali dibaca. Field
 * {@link #terisi} adalah nilai <b>materialized</b> yang harus secara eksplisit disegarkan lewat
 * {@link #updateTerisi()} (atau di-set langsung tanpa lewat method itu — lihat pemanggil di bawah).
 * Verifikasi ulang dari sisi entity ini (per permintaan investigasi lanjutan atas
 * {@code task_d82932ef} yang pertama kali ditemukan pada klaster Pasien batch 100) MENGONFIRMASI
 * bug tersebut secara independen: {@link #updateTerisi()} menghitung SELURUH riwayat
 * {@link DataPasienKeluar} yang pernah terhubung ke bed ini lewat {@code pendaftaran.tempatTidur},
 * bukan hanya status pasien yang AKTIF saat ini menempati bed tersebut. Kombinasi ini dengan fakta
 * bahwa {@link Pendaftaran#setTempatTidur(TempatTidur)} TIDAK PERNAH di-null-kan saat pasien
 * pulang/pindah membuat sebuah bed yang pernah sekali saja mengalami kepulangan pasien akan
 * SELAMANYA dihitung "tidak terisi" oleh {@link #updateTerisi()}, walau bed itu kemudian ditempati
 * ulang oleh pasien baru yang MASIH DIRAWAT. Rincian mekanisme lengkap ada pada javadoc
 * {@link #updateTerisi()} dan {@link #getTerisi()} di bawah. Rujuk {@code task_d82932ef} untuk
 * detail temuan awal dan status perbaikannya; javadoc ini hanya mendokumentasikan perilaku
 * berjalan, TIDAK memperbaiki kodenya.
 * </p>
 *
 * <p><b>Pemanggil {@link #updateTerisi()}:</b> {@code ais.action.master.sirs.DataPasienKeluarAction}
 * (saat pasien dinyatakan pulang) dan {@code ais.action.master.sirs.PindahTempatTidurRawatInapAction#onSave(Event)}
 * (dipanggil dua kali per transaksi pindah bed: sekali untuk bed asal, sekali untuk bed tujuan).
 * Selain lewat method ini, beberapa action lain men-set {@link #terisi} secara LANGSUNG tanpa
 * melalui {@link #updateTerisi()} sama sekali — mis. {@code PendaftaranRawatInapAction} men-set
 * {@code true} saat pendaftaran baru disimpan dan {@code false} saat pendaftaran dibatalkan/dihapus,
 * serta {@code PindahTempatTidurRawatInapAction#onDelete(...)} men-set {@code true} kembali saat
 * transaksi pindah dibatalkan. Artinya field ini adalah campuran dua gaya pemutakhiran (recompute
 * vs assign langsung) yang TIDAK saling menyadari kondisi historis lawannya.</p>
 *
 * <p>Class ini adalah entity hbm2java standar (getter relasi memakai {@code check(...)} warisan
 * {@link GeneralValueObject} untuk resolusi proxy Hibernate lazy yang aman lintas session — lihat
 * javadoc {@link GeneralValueObject#check(Object)} untuk mekanismenya). Field audit
 * {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah shadow field standar AIS (diisi
 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}) — ini KEHARUSAN TEKNIS pola audit
 * aplikasi, bukan bug. Modul {@code sirs} tidak memiliki sumbu tenant/satuan-kerja sama sekali
 * (dikonfirmasi berulang kali pada audit sebelumnya, {@code task_90bbdd51}); entity ini juga tidak
 * mengecualikan diri dari pola tersebut.</p>
 *
 * @see Kamar kamar yang menaungi tempat tidur ini
 * @see StatusTempatTidur status administratif tempat tidur (bukan status okupansi)
 * @see KelasPerawatan kelas perawatan tempat tidur ini, independen dari kelas kamar induknya
 * @see Pendaftaran pendaftaran rawat inap yang menunjuk balik ke tempat tidur ini lewat {@code tempatTidur}
 * @see DataPasienKeluar riwayat kepulangan pasien yang (secara keliru) dipakai sebagai basis {@link #updateTerisi()}
 * @see ais.action.master.sirs.PindahTempatTidurRawatInapAction alur pindah bed yang aktif memicu pola bug ini pada bed asal
 * @see ais.action.master.sirs.DataPasienKeluarAction pemanggil {@link #updateTerisi()} saat pasien pulang
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "tempat_tidur")
public class TempatTidur extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.tempat_tidur}. Lihat {@link #getId()}. */
	private Long id;
	/** Identifier pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identifier pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identifier pengguna yang terakhir mengubah baris ini. Nilai kosong/blank
	 * sengaja DIABAIKAN (bukan di-set menjadi kosong) agar jejak audit sebelumnya tidak
	 * tertimpa oleh pemanggilan yang tidak membawa identitas pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/**
	 * Representasi string dari tempat tidur ini, dipakai komponen ZK (combobox/label) yang
	 * memanggil {@code toString()} secara implisit.
	 *
	 * @return {@link #nama} tempat tidur apa adanya (tanpa null-check eksplisit — akan
	 *         mengembalikan {@code null} bila {@link #nama} belum diisi)
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna yang terakhir mengubah baris ini. Nilai kosong/blank sengaja
	 * DIABAIKAN, simetris dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan {@link #tanggal_dirubah}
	 * setiap kali baris ini di-{@code UPDATE}. Pola shadow-audit-field standar AIS; lihat juga
	 * {@link #oleh} dan {@link #olehId} yang diisi terpisah oleh pemanggil (bukan oleh callback
	 * ini).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara manual. Dalam alur normal nilai ini
	 * dimutakhirkan otomatis oleh {@link #onUpdate()}; setter ini dipakai bila pemanggil perlu
	 * memaksa nilai tertentu (mis. saat memuat data hasil migrasi).
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal/jam perubahan terakhir; default konstruksi objek adalah waktu objek
	 *         dibuat di memori, sebelum baris pernah tersimpan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama/label tempat tidur, mis. "Bed 1", "Bed A". Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas tentang tempat tidur ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Ruang perawatan yang menaungi tempat tidur ini (lewat {@link Kamar}). Lihat {@link #getRuang()}. */
	private Ruang ruang;
	/** Kelas perawatan tempat tidur ini, independen dari kelas kamar induknya. Lihat {@link #getKelasPerawatan()}. */
	private KelasPerawatan kelasPerawatan;
	/** Kamar yang menaungi tempat tidur ini. Lihat {@link #getKamar()}. */
	private Kamar kamar;
	/** Status administratif tempat tidur (bukan status okupansi). Lihat {@link #getStatusTempatTidur()}. */
	private StatusTempatTidur statusTempatTidur;
	/**
	 * Flag okupansi materialized: {@code true} bila bed dianggap sedang ditempati. LIHAT
	 * PERINGATAN pada javadoc kelas dan {@link #updateTerisi()} — flag ini TIDAK selalu
	 * merefleksikan kondisi aktual karena cara perhitungannya keliru untuk bed yang pernah
	 * mengalami kepulangan pasien di masa lalu.
	 */
	private Boolean terisi;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public TempatTidur() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID tempat tidur, atau {@code null} untuk instance yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris ini. Kolom bertanda {@code insertable = false} pada
	 * pemetaan — nilai sesungguhnya berasal dari {@code IDENTITY} basis data saat
	 * {@code INSERT}, sehingga setter ini biasanya hanya relevan untuk memuat ulang entity yang
	 * sudah memiliki ID (mis. hasil {@code session.load(...)}).
	 *
	 * @param id ID baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/label tempat tidur ini.
	 *
	 * @return nama tempat tidur, mis. "Bed 1"
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama/label tempat tidur ini. Kolom wajib diisi di lapisan basis data
	 * ({@code nullable = false}), tetapi setter ini sendiri tidak melakukan validasi apa pun.
	 *
	 * @param nama nama baru, maksimal 50 karakter di kolom basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang tempat tidur ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang tempat tidur ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan ruang perawatan tempat tidur ini secara langsung. Dalam praktiknya ruang lebih
	 * sering diturunkan dari {@link #kamar} (lewat {@link Kamar#getRuang()}) oleh kode
	 * pemanggil (lihat {@code PindahTempatTidurRawatInapAction}), sehingga field ini bisa saja
	 * tidak konsisten dengan ruang milik {@link #kamar} bila keduanya di-set terpisah.
	 *
	 * @param ruang ruang baru, boleh {@code null}
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengembalikan ruang perawatan tempat tidur ini, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject} agar aman dipanggil meski entity ini
	 * sudah lepas dari session Hibernate yang memuatnya.
	 *
	 * @return ruang perawatan, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return ruang;
	}

	/**
	 * Menetapkan kelas perawatan tempat tidur ini. Kelas ini independen dari kelas perawatan
	 * milik {@link Kamar} induknya — keduanya adalah kolom terpisah dan tidak ada trigger/kode
	 * yang memaksa konsistensi antara {@code TempatTidur.kelasPerawatan} dan
	 * {@code Kamar.kelasPerawatan}. UI (mis. {@code PindahTempatTidurRawatInapAction}) memang
	 * menyinkronkan combobox kelas mengikuti kamar yang dipilih, tetapi itu perilaku layar, bukan
	 * jaminan level data.
	 *
	 * @param kelasPerawatan kelas perawatan baru, boleh {@code null}
	 */
	public void setKelasPerawatan(KelasPerawatan kelasPerawatan) {
		this.kelasPerawatan = kelasPerawatan;
	}

	/**
	 * Mengembalikan kelas perawatan tempat tidur ini, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject}.
	 *
	 * @return kelas perawatan, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_perawatan", nullable = true)
	public KelasPerawatan getKelasPerawatan() {
		kelasPerawatan = check(kelasPerawatan);
		return kelasPerawatan;
	}

	/**
	 * Menetapkan kamar yang menaungi tempat tidur ini.
	 *
	 * @param kamar kamar baru, boleh {@code null}
	 */
	public void setKamar(Kamar kamar) {
		this.kamar = kamar;
	}

	/**
	 * Mengembalikan kamar yang menaungi tempat tidur ini, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject}.
	 *
	 * @return kamar, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = true)
	public Kamar getKamar() {
		kamar = check(kamar);
		return kamar;
	}

	/**
	 * Menghitung ulang dan menyimpan flag okupansi {@link #terisi} untuk tempat tidur ini
	 * berdasarkan riwayat basis data. <b>Method ini mengandung bug integritas okupansi yang
	 * signifikan secara klinis, dikonfirmasi ulang dari sisi entity ini sendiri dan didokumentasikan
	 * di sini sebagai perluasan {@code task_d82932ef}</b> (temuan awal dari klaster Pasien,
	 * batch 100) — javadoc ini SENGAJA tidak memperbaiki kode, hanya mendokumentasikan perilaku
	 * berjalan secara akurat karena perbaikannya sudah dijadwalkan lewat task tersebut.
	 *
	 * <p><b>Mekanisme (bila {@link #getId()} tidak {@code null}, jika tidak method ini no-op):</b></p>
	 * <ol>
	 *   <li>{@code pasienKeluar} = {@code COUNT(*)} baris {@link DataPasienKeluar} yang di-alias
	 *   ke {@code pendaftaran} lalu difilter {@code Restrictions.eq("pendaftaran.tempatTidur", this)}.
	 *   Query ini TIDAK memfilter berdasarkan tanggal kepulangan, status pulang, maupun apakah
	 *   {@link Pendaftaran} yang bersangkutan masih menjadi pendaftaran AKTIF pasien tersebut — ia
	 *   menghitung SELURUH baris {@link DataPasienKeluar} yang pernah ada sepanjang riwayat bed
	 *   ini, tanpa batas waktu.</li>
	 *   <li>{@code pendaftaran} = {@code COUNT(*)} baris {@link Pendaftaran} dengan
	 *   {@code tempatTidur == this}, juga tanpa filter status pendaftaran (aktif/pulang/pindah)
	 *   maupun tanggal — menghitung SELURUH pendaftaran yang PERNAH menunjuk ke bed ini.</li>
	 *   <li>Jika {@code pasienKeluar > 0} → {@link #setTerisi(Boolean) setTerisi(false)}. Cabang
	 *   ini DIPRIORITASKAN di atas hasil hitung {@code pendaftaran}: begitu bed pernah punya SATU
	 *   saja baris kepulangan yang terhubung, bed dianggap kosong SELAMANYA oleh cabang ini,
	 *   berapa pun banyaknya pendaftaran aktif yang mungkin menunjuk ke bed yang sama sekarang.</li>
	 *   <li>Jika tidak, dan {@code pendaftaran > 0} → {@code setTerisi(true)}. Baris komentar
	 *   {@code //setStatusTempatTidur(ConstantValues.TIDAK_TERSEDIA);} tepat di bawahnya adalah
	 *   kode mati (dinonaktifkan) yang menunjukkan niat awal untuk juga memutakhirkan
	 *   {@link StatusTempatTidur} secara otomatis — niat itu tidak pernah direalisasikan.</li>
	 *   <li>Jika keduanya nol → {@code setTerisi(false)} (bed memang belum pernah dipakai).</li>
	 * </ol>
	 *
	 * <p><b>Mengapa ini adalah bug, bukan sekadar desain longgar:</b> akar masalahnya adalah
	 * {@link Pendaftaran#setTempatTidur(TempatTidur)} tidak pernah di-set {@code null} ketika
	 * pasien pulang atau pindah bed (dikonfirmasi lewat pencarian pemanggil setter tersebut di
	 * seluruh kode: tidak ada satu pun titik yang mengosongkannya). Akibatnya baris
	 * {@link Pendaftaran} historis pasien yang SUDAH PULANG tetap permanen menunjuk ke bed lamanya,
	 * dan {@link DataPasienKeluar} yang tercatat untuk kepulangan itu tetap permanen "terhubung" ke
	 * bed yang sama lewat alias {@code pendaftaran.tempatTidur}. Begitu kondisi ini terjadi sekali
	 * untuk sebuah bed, cabang {@code pasienKeluar > 0} pada method ini akan SELALU bernilai benar
	 * untuk bed tersebut, tidak peduli berapa kali bed itu kemudian ditempati ulang oleh pasien
	 * baru yang benar-benar aktif dirawat di sana sekarang.</p>
	 *
	 * <p><b>Titik pemicu konkret:</b> {@code PindahTempatTidurRawatInapAction#onSave(Event)}
	 * secara aktif menciptakan kondisi ini pada SETIAP transaksi pindah bed: ia membuat/memutakhirkan
	 * baris {@link DataPasienKeluar} berstatus {@code ConstantValues.STATUS_PINDAH} untuk
	 * pendaftaran asal (yang {@code tempatTidur}-nya masih menunjuk ke bed lama), lalu memanggil
	 * {@code tempatTidur.updateTerisi()} untuk bed asal maupun bed tujuan. Pada saat itu juga hasil
	 * untuk bed asal memang benar ({@code false}, karena memang baru saja dikosongkan) — tetapi bed
	 * itu kini "tercemar" secara permanen: pemanggilan {@link #updateTerisi()} berikutnya kapan pun
	 * di masa depan, untuk pasien SIAPA PUN yang ditempatkan di bed itu, akan tetap menghasilkan
	 * {@code false} karena cabang {@code pasienKeluar > 0} sudah dan akan selalu terpenuhi.</p>
	 *
	 * <p><b>Dampak operasional:</b> widget pemilihan bed (mis. {@code AmbilDataTempatTidurBanbox},
	 * dipakai {@code PindahTempatTidurRawatInapAction} dan {@code PendaftaranRawatInapAction} untuk
	 * menyaring bed yang ditawarkan ke petugas) mengandalkan {@link #getTerisi()} untuk menentukan
	 * bed mana yang "kosong"/tersedia. Bed yang SEDANG DITEMPATI pasien aktif akan tampil sebagai
	 * kosong pada widget tersebut apabila bed itu pernah — kapan pun sebelumnya — mengalami satu
	 * kepulangan pasien. Risikonya bersifat klinis-operasional: petugas dapat menempatkan pasien
	 * baru ke bed yang sebenarnya masih terisi (risiko tabrakan penempatan pasien), dan laporan
	 * okupansi/ketersediaan bed rumah sakit menjadi tidak dapat dipercaya untuk bed yang sudah
	 * "berumur" (pernah dipakai minimal satu kali).</p>
	 *
	 * <p><b>Catatan tambahan:</b> query kedua ({@code pendaftaran}) sendiri juga tidak memfilter
	 * status pendaftaran aktif, sehingga secara terpisah juga tidak bisa dipakai sebagai indikator
	 * "sedang ditempati SEKARANG" yang andal — namun karena cabang pertama nyaris selalu
	 * mendahuluinya untuk bed yang sudah pernah dipakai, cacat pada query kedua ini jarang sempat
	 * teramati dalam praktik.</p>
	 */
	public void updateTerisi() {

		if (getId() != null) {
			Session session = HibernateUtil.currentSession();
			Number pasienKeluar = (Number) session.createCriteria(DataPasienKeluar.class)
					.createAlias("pendaftaran", "pendaftaran").setProjection(Projections.rowCount())
					.add(Restrictions.eq("pendaftaran.tempatTidur", this)).uniqueResult();

			Number pendaftaran = (Number) session.createCriteria(Pendaftaran.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("tempatTidur", this)).uniqueResult();

			if (pasienKeluar.intValue() > 0) {
				setTerisi(false);
			} else if (pendaftaran.intValue() > 0) {
				setTerisi(true);
//				setStatusTempatTidur(ConstantValues.TIDAK_TERSEDIA);
			} else {
				setTerisi(false);
			}

		}
	}

	/**
	 * Menetapkan flag okupansi {@link #terisi} secara langsung, TANPA melalui perhitungan
	 * {@link #updateTerisi()}. Beberapa pemanggil (mis. {@code PendaftaranRawatInapAction} saat
	 * menyimpan/membatalkan pendaftaran, {@code PindahTempatTidurRawatInapAction#onDelete(...)}
	 * saat membatalkan transaksi pindah) memakai jalur ini untuk assignment langsung, sehingga
	 * nilai {@link #terisi} adalah gabungan dua sumber kebenaran yang tidak saling menyadari
	 * riwayat satu sama lain.
	 *
	 * @param terisi nilai baru: {@code true} bila dianggap terisi, {@code false} bila kosong,
	 *               boleh {@code null}
	 */
	public void setTerisi(Boolean terisi) {
		this.terisi = terisi;
	}

	/**
	 * Mengembalikan flag okupansi {@link #terisi} apa adanya — method ini HANYA membaca field
	 * in-memory/kolom basis data, TIDAK menghitung ulang okupansi. Nilai yang dikembalikan hanya
	 * seakurat pemanggilan {@link #updateTerisi()} (atau {@link #setTerisi(Boolean)} langsung)
	 * terakhir yang pernah terjadi untuk baris ini.
	 *
	 * <p><b>WASPADA:</b> lihat javadoc {@link #updateTerisi()} untuk rincian lengkap bug
	 * perhitungan okupansi ({@code task_d82932ef}). Ringkasnya: untuk tempat tidur yang PERNAH
	 * mengalami satu kepulangan pasien di masa lalu (kapan pun), getter ini akan cenderung
	 * mengembalikan {@code false} ("kosong") meskipun bed tersebut SEDANG DITEMPATI pasien lain
	 * yang aktif dirawat di sana sekarang — karena {@link Pendaftaran#tempatTidur} pasien yang
	 * sudah pulang tidak pernah dikosongkan, sehingga riwayat kepulangan lama tetap "menempel" ke
	 * bed ini secara permanen dan terus memicu cabang {@code false} pada {@link #updateTerisi()}
	 * setiap kali dipanggil ulang. Kode pemanggil (widget pemilihan bed, laporan okupansi) yang
	 * memakai getter ini untuk menampilkan status "tersedia" harus memperlakukan hasilnya sebagai
	 * TIDAK SEPENUHNYA DAPAT DIPERCAYA untuk bed yang sudah pernah dipakai sebelumnya.</p>
	 *
	 * @return {@code true} bila terakhir dihitung/di-set sebagai terisi, {@code false} bila
	 *         kosong, boleh {@code null} bila belum pernah dihitung sama sekali
	 */
	public Boolean getTerisi() {
		return terisi;
	}

	/**
	 * Menetapkan status administratif tempat tidur (mis. tersedia/dibersihkan/rusak — nilai
	 * konkret berasal dari data master {@link StatusTempatTidur}, bukan enum Java). Field ini
	 * terpisah sepenuhnya dari flag okupansi {@link #terisi}: keduanya tidak saling
	 * memutakhirkan otomatis satu sama lain (baris kode di {@link #updateTerisi()} yang akan
	 * melakukan itu sengaja dinonaktifkan/dikomentari).
	 *
	 * @param statusTempatTidur status baru, boleh {@code null}
	 */
	public void setStatusTempatTidur(StatusTempatTidur statusTempatTidur) {
		this.statusTempatTidur = statusTempatTidur;
	}

	/**
	 * Mengembalikan status administratif tempat tidur ini, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject}.
	 *
	 * @return status tempat tidur, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_tempat_tidur", nullable = true)
	public StatusTempatTidur getStatusTempatTidur() {
		statusTempatTidur = check(statusTempatTidur);
		return statusTempatTidur;
	}

}
