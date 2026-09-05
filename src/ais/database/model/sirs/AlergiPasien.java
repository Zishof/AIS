package ais.database.model.sirs;

// Blueprint Integrasi SIRS — Fase 2 (Fondasi data / keselamatan klinis). Entitas BARU, aditif.

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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Riwayat alergi / intoleransi pasien — analog "AllergyIntolerance" pada FHIR. Menutup gap
 * keselamatan klinis (C1 pada laporan analisis Fase 1): sebelumnya {@link Pasien} tidak memiliki
 * field alergi apa pun, sehingga peresepan tidak dapat memeriksa kontra-indikasi.
 *
 * <p>Setiap baris mencatat satu substansi alergen: kategori (obat/makanan/lingkungan), substansi
 * (teks bebas, opsional ditautkan ke {@link ItemMedis} bila alergi obat yang ada di katalog),
 * reaksi, tingkat keparahan, status klinis, tanggal pencatatan, dan pencatat.</p>
 *
 * <p><b>Keselamatan:</b> status klinis memakai {@code statusKlinis} (AKTIF/INAKTIF/RESOLVED) —
 * penonaktifan alergi dilakukan dengan MENGUBAH status, BUKAN menghapus baris. Envers menyimpan
 * seluruh riwayat perubahan (tanpa @NotAudited). Ini konsisten dengan prinsip blueprint: jangan
 * menghapus fisik / menghilangkan data alergi secara diam-diam.</p>
 *
 * <p><b>Skema:</b> tabel {@code sirs.alergi_pasien}; tabel audit dibuat otomatis oleh
 * {@code hbm2ddl.auto=update} + Envers di {@code new_audit.alergi_pasien__audit}. Kompatibel
 * Java 1.6/1.7 dan Hibernate 3.6.</p>
 *
 * <h2>Status pemakaian: dibaca oleh pemeriksa keselamatan, tetapi belum ada layar pengisian</h2>
 *
 * <p>Penelusuran seluruh basis kode menemukan tiga berkas yang menyentuh entity ini, dan
 * pembagian perannya penting untuk dipahami:</p>
 *
 * <ul>
 *   <li><b>Pembaca — {@code ApotikApiHelper#profilKeselamatanResep(...)}.</b> Ini konsumen
 *       sungguhan: ia mengambil seluruh alergi berstatus {@link #STATUS_AKTIF} milik pasien
 *       pemilik resep, mencocokkan {@link #getItemMedis()} dengan item pada resep, dan
 *       menerbitkan peringatan bertingkat ({@code BAHAYA} bila cocok eksak,
 *       {@code PERINGATAN} bila pasien punya alergi aktif yang substansinya berupa teks bebas).</li>
 *   <li><b>Pembaca — {@code ApotikPasienHelper#detail(...)}.</b> Menyertakan hingga 50 alergi
 *       terbaru pasien pada respons endpoint {@code apotik_pasien_detail}.</li>
 *   <li><b>Satu-satunya penulis — {@code ApotikDemoProvisionHelper}.</b> Dan ini bukan alur
 *       produksi: ia menanam data contoh untuk keperluan demo/UAT, lengkap dengan penanda
 *       {@code keterangan} berbunyi "DATA SAMPLE/UAT — wajib diverifikasi sebelum penggunaan
 *       nyata" dan {@code olehId} {@code "seed_demo"}.</li>
 * </ul>
 *
 * <p><b>Tidak ada layar ZK, form, atau endpoint produksi yang memungkinkan tenaga kesehatan
 * mencatat alergi pasien.</b> Pada deployment nyata, tabel ini karena itu kosong, dan
 * {@code profilKeselamatanResep} akan selalu menemukan nol alergi.</p>
 *
 * <p>Yang perlu diapresiasi: <b>pemeriksa keselamatan sudah dirancang fail-safe menghadapi
 * keadaan itu.</b> Ia tidak pernah menyimpulkan "aman". Nilai awalnya
 * {@code kesimpulan = "PERLU_TELAAH_APOTEKER"}, {@code evaluasiOtomatisLengkap = false},
 * {@code basisPengetahuanInteraksiTersedia = false}, dan ia selalu menambahkan peringatan
 * {@code TELAAH_INTERAKSI_MANUAL}. Kesimpulan hanya berubah menjadi {@code "ALERGI_TERDETEKSI"}
 * bila ada kecocokan eksak — tidak pernah menjadi "tidak ada alergi". Rancangan itu benar:
 * ketiadaan data alergi tidak boleh dibaca sebagai ketiadaan alergi. Kekosongan tabel karena itu
 * adalah <b>gap fitur</b> (alergi tidak dapat dicatat), bukan lubang keselamatan yang
 * menghasilkan klaim palsu.</p>
 *
 * <h2>Kendali akses data medis: sama saja dengan entity biasa</h2>
 *
 * <p>Pertanyaan yang wajar diajukan untuk data sesensitif riwayat alergi adalah apakah ia
 * mendapat perlakuan akses yang lebih ketat daripada entity lain. <b>Jawabannya: tidak.</b>
 * Entity ini tidak punya properti tenant, tidak punya penanda kerahasiaan, dan tidak punya
 * pemeriksaan hak khusus di tingkat model.</p>
 *
 * <ul>
 *   <li><b>Generic CRUD v2 memperlakukannya seperti master biasa.</b> Kelas ini memenuhi syarat
 *       {@code GenericCrudAutoDefinitionFactory.findMappedClass()}, dan
 *       {@code BLOCKED_CLASS_TOKENS} — yang memuat {@code "password"}, {@code "token"},
 *       {@code "lampiran"}, {@code "audit"}, {@code "bank"} — <b>tidak memuat satu pun token
 *       medis</b>: tidak ada {@code "alergi"}, {@code "pasien"}, {@code "medis"},
 *       {@code "diagnosa"}, maupun {@code "rekam"}. Akibatnya {@code isBlockedClass()} bernilai
 *       {@code false} dan entity berstatus {@code FULL_CRUD}.</li>
 *   <li><b>Pembatas barisnya kosong.</b> Sama seperti {@link Pasien} dan
 *       {@link KepesertaanPasien}: {@code scopeBindings()} hanya mengenal {@code yayasan},
 *       {@code sekolah}, {@code program}, {@code fakultas}, {@code jurusan}, {@code satuanKerja},
 *       dan kelas ini tidak punya satu pun dari nama itu. {@code applyScope()} tidak memasang
 *       {@code Restrictions} apa pun.</li>
 *   <li><b>Endpoint API tidak menyaring per pengguna.</b> {@code ApotikApiDispatcher}
 *       meneruskan {@code apotik_pasien_detail} ke {@code ApotikPasienHelper.detail(payload,
 *       hasil)} <b>tanpa memberikan {@code Tbmuser}</b> — helper karena itu secara struktural
 *       tidak dapat menyaring berdasarkan siapa yang bertanya. Pengamanannya bergantung
 *       sepenuhnya pada gerbang menu {@code PosApi.bolehAksesActionKantin} yang berjalan lebih
 *       dulu (didokumentasikan fail-closed, kunci {@code apotik_*} default {@code false}).
 *       Gerbang per-menu itu <b>bukan</b> pembatas per-baris: siapa pun yang lolos gerbangnya
 *       dapat menarik profil alergi <i>pasien mana pun</i> dengan mengiterasi id pasien.</li>
 * </ul>
 *
 * <p>Perlu ditegaskan bahwa ini <b>bukan regresi</b> dan bukan pengabaian yang khusus terjadi di
 * sini: modul SIRS memang tidak memiliki sumbu tenant per-baris sama sekali (lihat javadoc
 * {@link Pasien}), dan seluruh kendali aksesnya bersifat per-menu/per-role. Yang membedakan
 * kelas ini adalah <b>tingkat kerugian bila datanya bocor</b> — riwayat alergi termasuk kategori
 * data kesehatan yang paling mudah menyimpulkan kondisi seseorang. Bila kelak entity medis
 * hendak diberi perlindungan lebih, langkah paling murah dan paling berdampak adalah
 * menambahkan token medis ke {@code BLOCKED_CLASS_TOKENS} sehingga ia tidak pernah keluar lewat
 * permukaan CRUD generik.</p>
 *
 * @see Pasien
 * @see ItemMedis
 * @see KepesertaanPasien
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "alergi_pasien")
public class AlergiPasien extends GeneralValueObject {

	// Kategori alergen

	/**
	 * Kategori alergen "OBAT" — satu-satunya kategori yang dapat diperiksa mesin.
	 *
	 * <p>Hanya baris berkategori ini yang lazimnya menautkan {@link #getItemMedis()}, dan hanya
	 * lewat tautan itulah {@code ApotikApiHelper#profilKeselamatanResep(...)} dapat menyatakan
	 * kecocokan eksak dengan item pada resep. Alergi obat yang substansinya hanya diketik
	 * sebagai teks tanpa menautkan {@link ItemMedis} <b>tidak akan pernah</b> memicu peringatan
	 * {@code BAHAYA} — ia hanya menghasilkan peringatan
	 * {@code ALERGI_AKTIF_PERLU_VERIFIKASI} yang meminta apoteker memeriksa manual. Itu
	 * konsekuensi rancangan yang disengaja: menebak kecocokan dari teks bebas berisiko
	 * menghasilkan positif palsu yang melatih pengguna mengabaikan peringatan.</p>
	 *
	 * <p>Keempat konstanta kategori adalah {@code String} lepas, bukan enum dan bukan master
	 * basis data. {@link #setKategori(String)} tidak memvalidasi nilainya.</p>
	 */
	public static final String KATEGORI_OBAT = "OBAT";

	/**
	 * Kategori alergen "MAKANAN": relevan untuk perencanaan diet pasien rawat inap dan gizi.
	 * Tidak diperiksa mesin — lihat {@link #KATEGORI_OBAT}.
	 */
	public static final String KATEGORI_MAKANAN = "MAKANAN";

	/**
	 * Kategori alergen "LINGKUNGAN": debu, serbuk, lateks, dan sejenisnya. Tidak diperiksa
	 * mesin — lihat {@link #KATEGORI_OBAT}.
	 */
	public static final String KATEGORI_LINGKUNGAN = "LINGKUNGAN";

	/**
	 * Kategori alergen "LAINNYA": penampung untuk alergen yang tidak masuk tiga kategori di
	 * atas. Lihat {@link #KATEGORI_OBAT}.
	 */
	public static final String KATEGORI_LAINNYA = "LAINNYA";

	// Tingkat keparahan

	/**
	 * Tingkat keparahan "RINGAN".
	 *
	 * <p>Ketiga konstanta keparahan dipakai sebagai <b>informasi bagi manusia</b>, bukan sebagai
	 * pemicu logika: {@code profilKeselamatanResep} menyertakan nilainya pada keluaran JSON
	 * tetapi tidak pernah menaikkan atau menurunkan tingkat peringatan berdasarkannya. Alergi
	 * berkeparahan {@link #KEPARAHAN_BERAT} yang tidak menautkan {@link ItemMedis} menghasilkan
	 * peringatan yang sama persis dengan alergi ringan. Nilainya tidak divalidasi setter.</p>
	 */
	public static final String KEPARAHAN_RINGAN = "RINGAN";

	/** Tingkat keparahan "SEDANG". Lihat {@link #KEPARAHAN_RINGAN}. */
	public static final String KEPARAHAN_SEDANG = "SEDANG";

	/**
	 * Tingkat keparahan "BERAT" — mencakup reaksi anafilaksis yang mengancam jiwa.
	 * Lihat {@link #KEPARAHAN_RINGAN} soal mengapa nilai ini tidak menaikkan tingkat peringatan
	 * otomatis.
	 */
	public static final String KEPARAHAN_BERAT = "BERAT";

	// Status klinis

	/**
	 * Status klinis "AKTIF" — alergi masih berlaku dan harus diperhitungkan saat meresepkan.
	 *
	 * <p>Ini nilai default field {@code statusKlinis} <i>dan</i> nilai yang dikembalikan
	 * {@link #getStatusKlinis()} ketika field kosong atau blank. Ia juga satu-satunya nilai yang
	 * dicari {@code profilKeselamatanResep}, yang menyaring dengan
	 * {@code Restrictions.eq("statusKlinis", STATUS_AKTIF)}.</p>
	 *
	 * <p><b>Arah default-nya benar dan penting.</b> Baris yang statusnya belum terisi diperlakukan
	 * sebagai alergi aktif, bukan sebaliknya. Untuk data keselamatan, "tidak tahu" harus dibaca
	 * sebagai "mungkin berbahaya". Bandingkan dengan {@code Restrictions.eq} pada query: karena
	 * penyaringan terjadi di basis data, baris yang kolomnya benar-benar {@code NULL}
	 * <b>tidak</b> ikut terambil — normalisasi di getter tidak menolong di sana. Bila kelak
	 * ditemukan baris warisan berstatus {@code NULL}, query itu perlu diperluas menjadi
	 * {@code Restrictions.or(eq(...), isNull(...))}.</p>
	 */
	public static final String STATUS_AKTIF = "AKTIF";

	/**
	 * Status klinis "INAKTIF" — alergi tidak lagi dianggap berlaku, tetapi barisnya tetap
	 * disimpan.
	 *
	 * <p>Inilah cara yang benar untuk "menghapus" alergi pada sistem ini, sesuai prinsip yang
	 * dinyatakan javadoc kelas: ubah status, jangan hapus baris. Envers merekam transisi
	 * statusnya sehingga selalu dapat ditelusuri siapa yang menonaktifkan dan kapan.</p>
	 */
	public static final String STATUS_INAKTIF = "INAKTIF";

	/**
	 * Status klinis "RESOLVED" — alergi dinyatakan sudah teratasi/tidak lagi terjadi secara
	 * klinis.
	 *
	 * <p>Berbeda dari {@link #STATUS_INAKTIF} yang lebih bersifat administratif ("tidak lagi
	 * dipakai"), {@code RESOLVED} adalah pernyataan klinis. Perbedaan itu <b>tidak ditegakkan
	 * kode mana pun</b> — bagi {@code profilKeselamatanResep} keduanya sama-sama bukan
	 * {@link #STATUS_AKTIF} sehingga sama-sama diabaikan. Pembedaannya semata untuk pembaca
	 * manusia dan untuk pemetaan ke {@code AllergyIntolerance.clinicalStatus} FHIR di masa
	 * depan.</p>
	 */
	public static final String STATUS_RESOLVED = "RESOLVED";

	/**
	 * Versi serialisasi Java; memakai pola bernomor entity Fase 2 yang ditulis tangan
	 * ({@code ...022L}), bukan nilai seragam hasil {@code hbm2java}.
	 * Bandingkan {@link KepesertaanPasien} ({@code ...021L}).
	 */
	private static final long serialVersionUID = 4820100719000000022L;

	/** Kunci utama {@code sirs.alergi_pasien.id}; lihat {@link #getId()}. */
	private Long id;

	/** Field audit bayangan: id pengguna pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/** Field audit bayangan: identitas pengguna pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;

	/** Stempel waktu perubahan terakhir; lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = new Date();

	/** Pasien pemilik riwayat alergi ini; lihat {@link #getPasien()}. */
	private Pasien pasien;

	/**
	 * Tautan opsional ke katalog obat — <b>satu-satunya jalur pemeriksaan mesin</b>.
	 * Lihat {@link #getItemMedis()}.
	 */
	private ItemMedis itemMedis; // opsional: bila alergi obat yang ada di katalog

	/** Kategori alergen ({@link #KATEGORI_OBAT} dan kerabatnya); lihat {@link #getKategori()}. */
	private String kategori;

	/** Nama substansi alergen sebagai teks bebas; lihat {@link #getSubstansi()}. */
	private String substansi;

	/** Deskripsi reaksi yang timbul; lihat {@link #getReaksi()}. */
	private String reaksi;

	/**
	 * Tingkat keparahan ({@link #KEPARAHAN_RINGAN} dan kerabatnya); lihat
	 * {@link #getKeparahan()}.
	 */
	private String keparahan;

	/**
	 * Status klinis alergi; default {@link #STATUS_AKTIF}. Lihat {@link #getStatusKlinis()}.
	 */
	private String statusKlinis = STATUS_AKTIF;

	/**
	 * Waktu pencatatan alergi; default waktu instansiasi objek. Lihat
	 * {@link #getTanggalCatat()}.
	 */
	private Date tanggalCatat = new Date();

	/** Nama tenaga kesehatan pencatat, disimpan sebagai teks; lihat {@link #getPencatat()}. */
	private String pencatat;

	/** Catatan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Satu-satunya pemanggil langsung di basis kode adalah
	 * {@code ApotikDemoProvisionHelper} yang menanam data contoh untuk demo/UAT — lihat bagian
	 * status pemakaian pada javadoc kelas. Kehadiran konstruktor ini juga membuat entity dapat
	 * dibuat lewat Generic CRUD v2.</p>
	 */
	public AlergiPasien() {
	}

	/**
	 * Representasi teks alergi: nama substansi apa adanya, atau string kosong bila belum diisi.
	 *
	 * <p>Membaca field {@code substansi} langsung, bukan lewat {@link #getSubstansi()} — pola
	 * yang benar untuk {@code toString()} karena method ini kerap dipanggil dari logger dan
	 * penangan pengecualian, tempat memicu query basis data sangat tidak diinginkan. Di sini
	 * bedanya tidak berdampak karena {@code substansi} bertipe {@code String}, bukan relasi
	 * lazy.</p>
	 *
	 * <p><b>Catatan privasi:</b> nilai kembalian adalah data kesehatan. Setiap kali objek ini
	 * masuk ke pesan log atau pesan galat, substansi alergen pasien ikut terbawa. Tidak seperti
	 * {@link Pasien#toString()} yang menyertakan nama, di sini tidak ada pengenal pasien —
	 * sehingga kebocorannya relatif lebih terbatas, tetapi tetap perlu diperhatikan pada log
	 * yang juga memuat konteks pasien di baris sekitarnya.</p>
	 *
	 * @return nama substansi alergen, atau string kosong
	 */
	public String toString() {
		return substansi == null ? "" : substansi;
	}

	/**
	 * Mengembalikan kunci utama baris. {@code IDENTITY}, karena itu {@code insertable = false}.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama; untuk kerangka persistensi.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan id pengguna pengubah terakhir.
	 *
	 * <p>Bagian dari trio audit bayangan yang wajib diulang di setiap entity karena
	 * {@link GeneralValueObject} bukan {@code @Entity}/{@code @MappedSuperclass} — keharusan
	 * teknis, bukan duplikasi ceroboh.</p>
	 *
	 * <p>Pada data alergi, field ini punya makna klinis tambahan: ia melengkapi
	 * {@link #getPencatat()} dengan identitas sistem yang dapat ditelusuri. Perhatikan bahwa
	 * data contoh dari {@code ApotikDemoProvisionHelper} sengaja diberi
	 * {@code olehId = "seed_demo"} — penanda yang berguna untuk memisahkan data UAT dari data
	 * nyata bila keduanya pernah bercampur di satu basis data.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, menolak nilai {@code null}/blank secara diam-diam
	 * agar jejak audit lama tidak terhapus tanpa sengaja.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan identitas pengguna pengubah terakhir.
	 *
	 * @return identitas pengguna, atau {@code null}
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, menolak nilai kosong secara diam-diam.
	 *
	 * @param oleh identitas pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui stempel waktu audit tepat sebelum
	 * {@code UPDATE} dikirim, dengan mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Pada entity ini, callback tersebut adalah pelengkap penting bagi jejak Envers:
	 * penonaktifan alergi dilakukan sebagai {@code UPDATE} status (bukan {@code DELETE}),
	 * sehingga setiap transisi {@link #STATUS_AKTIF} → {@link #STATUS_INAKTIF} otomatis
	 * mendapat stempel waktu. Berjalan hanya pada {@code UPDATE}; jangan dipanggil manual.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris; dipetakan {@code TIMESTAMP}.
	 *
	 * <p>Jangan dikelirukan dengan {@link #getTanggalCatat()}: yang ini menandai kapan
	 * <i>barisnya</i> terakhir disunting, sedangkan yang itu menandai kapan alergi
	 * <i>dicatat secara klinis</i>. Keduanya berbeda begitu alergi dinonaktifkan.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir; umumnya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan kode aplikasi.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan pasien pemilik riwayat alergi ini, meresolusi proxy lazy lebih dulu.
	 *
	 * <p>Relasi inti entity, dan satu-satunya penyaring yang dipakai konsumennya: baik
	 * {@code ApotikApiHelper#profilKeselamatanResep(...)} maupun
	 * {@code ApotikPasienHelper#detail(...)} mengambil alergi dengan
	 * {@code Restrictions.eq("pasien", pasien)}.</p>
	 *
	 * <p><b>Kolom {@code nullable = true} adalah kelemahan integritas yang berdampak
	 * keselamatan.</b> Baris alergi tanpa pasien tidak bermakna apa pun secara klinis, tetapi
	 * skema mengizinkannya dan entity tidak memvalidasinya. Baris yatim semacam itu tidak akan
	 * pernah terambil oleh query mana pun — ia menumpuk tak terlihat, dan bila ia sebenarnya
	 * <i>dimaksudkan</i> untuk seorang pasien (mis. karena kegagalan penugasan pada alur impor),
	 * maka pasien itu tampak tidak punya alergi sama sekali. Kegagalan senyap semacam ini paling
	 * berbahaya justru pada data keselamatan. Nullable di sini mengikuti pola seragam model SIRS
	 * agar {@code hbm2ddl.auto=update} tidak gagal pada tabel warisan, bukan karena baris tanpa
	 * pasien memang diinginkan.</p>
	 *
	 * <p>Cascade {@code {PERSIST, MERGE}} tanpa {@code REMOVE}: menghapus baris alergi tidak
	 * pernah menyentuh baris pasien — dan sebaliknya, menghapus pasien juga tidak
	 * menghapus alerginya (tidak ada {@code @OneToMany} dengan cascade dari sisi
	 * {@link Pasien}).</p>
	 *
	 * @return pasien pemilik, atau {@code null} pada baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = true)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	/**
	 * Menyetel pasien pemilik riwayat alergi.
	 *
	 * @param pasien pasien pemilik
	 */
	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	/**
	 * Mengembalikan tautan opsional ke katalog obat, meresolusi proxy lazy lebih dulu —
	 * <b>satu-satunya jalur yang memungkinkan pemeriksaan kontra-indikasi otomatis</b>.
	 *
	 * <p>Javadoc {@code ApotikApiHelper#profilKeselamatanResep(...)} menyatakannya eksplisit:
	 * "Pemeriksaan otomatis sengaja hanya menyatakan kecocokan yang eksak melalui FK
	 * {@code AlergiPasien.getItemMedis()}; alergi teks bebas tidak ditebak dan tidak pernah
	 * menghasilkan klaim 'aman'". Mekanismenya sederhana dan dapat diaudit: kumpulkan id
	 * {@link ItemMedis} dari seluruh {@code ResepDetail}, lalu untuk tiap alergi aktif periksa
	 * apakah {@code getItemMedis().getId()} termasuk di dalamnya. Bila ya, terbit peringatan
	 * bertingkat {@code BAHAYA} dengan kode {@code ALERGI_OBAT_COCOK_EKSAK}.</p>
	 *
	 * <p><b>Batas yang harus dipahami setiap pemanggil.</b> Pencocokan ini bekerja pada tingkat
	 * <i>item katalog</i>, bukan tingkat <i>zat aktif</i>. Pasien yang alergi terhadap satu
	 * merek amoksisilin tidak akan memicu peringatan ketika diresepkan merek amoksisilin lain,
	 * karena keduanya baris {@link ItemMedis} yang berbeda. Untuk menutup celah itu diperlukan
	 * pemetaan ke {@link GenerikItem} atau ke kode zat aktif standar — sesuatu yang belum ada.
	 * Selama itu belum ada, peringatan {@code ALERGI_AKTIF_PERLU_VERIFIKASI} yang selalu terbit
	 * ketika pasien punya alergi aktif tanpa kecocokan eksak adalah pengaman utamanya, dan ia
	 * bergantung sepenuhnya pada apoteker yang membacanya.</p>
	 *
	 * <p>Nullable, dan memang seharusnya: alergi makanan, lingkungan, atau obat yang tidak ada
	 * di katalog tetap wajib dapat dicatat. Kekosongan field ini bukan cacat data.</p>
	 *
	 * @return item katalog yang menjadi alergen, atau {@code null} bila alergen berupa teks
	 *         bebas atau bukan obat
	 * @see #KATEGORI_OBAT
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_medis", nullable = true)
	public ItemMedis getItemMedis() {
		itemMedis = check(itemMedis);
		return itemMedis;
	}

	/**
	 * Menyetel tautan ke katalog obat.
	 *
	 * <p>Mengisi field ini menaikkan alergi dari "perlu telaah manual" menjadi "dapat diperiksa
	 * mesin" — lihat {@link #getItemMedis()}. Setiap layar pengisian alergi yang kelak dibangun
	 * sebaiknya mendorong pengisiannya untuk alergi berkategori {@link #KATEGORI_OBAT}.</p>
	 *
	 * @param itemMedis item katalog alergen, boleh {@code null}
	 */
	public void setItemMedis(ItemMedis itemMedis) {
		this.itemMedis = itemMedis;
	}

	/**
	 * Mengembalikan kategori alergen.
	 *
	 * <p>Nilai yang diharapkan salah satu {@link #KATEGORI_OBAT}, {@link #KATEGORI_MAKANAN},
	 * {@link #KATEGORI_LINGKUNGAN}, atau {@link #KATEGORI_LAINNYA}, tetapi tidak ada penjaga
	 * apa pun. Field ini bersifat informatif: {@code profilKeselamatanResep} menyertakannya pada
	 * keluaran JSON tetapi tidak memakainya sebagai syarat — pencocokan bergantung pada
	 * {@link #getItemMedis()}, bukan pada kategori. Alergi berkategori {@code MAKANAN} yang
	 * kebetulan menautkan {@link ItemMedis} tetap akan memicu peringatan bahaya, dan itu
	 * perilaku yang benar (lebih baik terlalu waspada daripada melewatkan).</p>
	 *
	 * @return kategori alergen, atau {@code null}
	 */
	@Column(name = "kategori", nullable = true, length = 30)
	public String getKategori() {
		return kategori;
	}

	/**
	 * Menyetel kategori alergen. Tanpa validasi terhadap konstanta {@code KATEGORI_*}.
	 *
	 * @param kategori kategori alergen
	 */
	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	/**
	 * Mengembalikan nama substansi alergen sebagai teks bebas.
	 *
	 * <p>Inti klinis baris ini — apa yang membuat pasien alergi. Dibatasi 100 karakter dan
	 * nullable pada tingkat skema, walaupun baris alergi tanpa substansi praktis tidak bermakna:
	 * ia akan muncul pada daftar alergi aktif pasien sebagai entri kosong, memicu peringatan
	 * {@code ALERGI_AKTIF_PERLU_VERIFIKASI} tanpa memberi tahu apoteker apa yang harus
	 * diverifikasi. Tidak ada validasi wajib-isi di entity.</p>
	 *
	 * <p>Teks bebas, tanpa kamus terkendali. Konsekuensinya, dua catatan alergi terhadap zat
	 * yang sama dapat tertulis berbeda ("Amoxicillin", "amoksisilin", "amox") dan tidak akan
	 * pernah dikenali sebagai hal yang sama oleh mesin apa pun. Ini alasan mendasar mengapa
	 * pemeriksaan otomatis bersandar pada {@link #getItemMedis()} dan bukan pada field ini.</p>
	 *
	 * <p>Nilai ini ikut ditampilkan pada pesan peringatan bagi apoteker
	 * ({@code "Alergi aktif cocok dengan item resep: " + substansi}) dan pada keluaran endpoint
	 * {@code apotik_pasien_detail}. Ia juga nilai kembalian {@link #toString()}.</p>
	 *
	 * @return nama substansi alergen, atau {@code null}
	 */
	@Column(name = "substansi", nullable = true, length = 100)
	public String getSubstansi() {
		return substansi;
	}

	/**
	 * Menyetel nama substansi alergen. Tanpa validasi wajib-isi maupun kamus terkendali.
	 *
	 * @param substansi nama substansi alergen
	 */
	public void setSubstansi(String substansi) {
		this.substansi = substansi;
	}

	/**
	 * Mengembalikan deskripsi reaksi yang timbul akibat alergen (mis. "Ruam dan sesak",
	 * "Gatal ringan").
	 *
	 * <p>Teks bebas tanpa batas panjang eksplisit. Bersifat informatif bagi tenaga kesehatan
	 * yang membaca profil pasien; tidak dipakai logika mana pun. Nilainya diteruskan apa adanya
	 * ke keluaran JSON endpoint alergi dan profil keselamatan resep.</p>
	 *
	 * @return deskripsi reaksi, atau {@code null}
	 */
	@Column(name = "reaksi", nullable = true)
	public String getReaksi() {
		return reaksi;
	}

	/**
	 * Menyetel deskripsi reaksi yang timbul.
	 *
	 * @param reaksi deskripsi reaksi
	 */
	public void setReaksi(String reaksi) {
		this.reaksi = reaksi;
	}

	/**
	 * Mengembalikan tingkat keparahan alergi.
	 *
	 * <p>Nilai yang diharapkan salah satu {@link #KEPARAHAN_RINGAN}, {@link #KEPARAHAN_SEDANG},
	 * atau {@link #KEPARAHAN_BERAT}. Seperti dicatat pada {@link #KEPARAHAN_RINGAN}, nilai ini
	 * <b>tidak memengaruhi tingkat peringatan otomatis</b>: alergi berat yang tidak menautkan
	 * {@link ItemMedis} menghasilkan peringatan yang sama dengan alergi ringan. Bila kelak
	 * peringatan hendak dibedakan berdasarkan keparahan, field ini perlu lebih dulu diberi
	 * kamus terkendali — sebab tanpa validasi, "Berat"/"berat"/"BERAT" adalah tiga nilai
	 * berbeda.</p>
	 *
	 * @return tingkat keparahan, atau {@code null}
	 */
	@Column(name = "keparahan", nullable = true, length = 20)
	public String getKeparahan() {
		return keparahan;
	}

	/**
	 * Menyetel tingkat keparahan alergi. Tanpa validasi terhadap konstanta
	 * {@code KEPARAHAN_*}.
	 *
	 * @param keparahan tingkat keparahan
	 */
	public void setKeparahan(String keparahan) {
		this.keparahan = keparahan;
	}

	/**
	 * Mengembalikan status klinis alergi, <b>menormalkan nilai kosong menjadi
	 * {@link #STATUS_AKTIF}</b>.
	 *
	 * <p>Normalisasinya lebih lengkap daripada pengecekan {@code null} biasa: field yang
	 * {@code null} <i>maupun</i> yang hanya berisi spasi sama-sama dibaca sebagai
	 * {@link #STATUS_AKTIF}. Arah default itu adalah keputusan keselamatan yang benar — data
	 * alergi yang statusnya tidak jelas harus diperlakukan sebagai masih berlaku, bukan
	 * sebaliknya. Bandingkan dengan {@link KepesertaanPasien#getStatusAktif()} yang menerapkan
	 * prinsip serupa (berpihak pada pasien), dan dengan {@link Pasien#getAktif()} yang justru
	 * mengembalikan {@code null} apa adanya.</p>
	 *
	 * <p>Getter ini <b>tidak</b> menulis kembali ke field — hanya nilai kembaliannya yang
	 * dinormalkan. Tidak ada efek samping persistensi, berbeda dari getter destruktif pada
	 * {@link Pasien}.</p>
	 *
	 * <p><b>Batas normalisasi yang penting.</b> Konsumen menyaring alergi aktif <i>di basis
	 * data</i> dengan {@code Restrictions.eq("statusKlinis", STATUS_AKTIF)}. Penyaringan itu
	 * terjadi sebelum objek Java mana pun terbentuk, sehingga normalisasi di getter ini
	 * <b>tidak berlaku di sana</b>: baris yang kolom {@code status_klinis}-nya benar-benar
	 * {@code NULL} akan lolos dari daftar alergi aktif — kebalikan persis dari niat getter ini.
	 * Hari ini keadaan itu tidak muncul karena field diinisialisasi {@code = STATUS_AKTIF} pada
	 * deklarasinya sehingga setiap baris baru selalu terisi. Namun baris yang masuk lewat jalur
	 * lain — impor SQL langsung, atau Generic CRUD v2 yang mengosongkan field — bisa saja
	 * bernilai {@code NULL} dan akan tersembunyi secara senyap. Bila itu terjadi, query
	 * konsumen perlu diperluas menjadi
	 * {@code Restrictions.or(Restrictions.eq(...), Restrictions.isNull(...))}.</p>
	 *
	 * @return status klinis; tidak pernah {@code null} maupun blank
	 */
	@Column(name = "status_klinis", nullable = true, length = 20)
	public String getStatusKlinis() {
		return (statusKlinis == null || statusKlinis.trim().isEmpty()) ? STATUS_AKTIF : statusKlinis;
	}

	/**
	 * Menyetel status klinis alergi.
	 *
	 * <p>Inilah cara yang benar untuk "menghapus" alergi: setel ke {@link #STATUS_INAKTIF} atau
	 * {@link #STATUS_RESOLVED}, jangan hapus barisnya. Envers merekam transisinya lengkap dengan
	 * pelaku dan waktu. Tidak ada validasi terhadap konstanta {@code STATUS_*}, dan tidak ada
	 * penjaga yang mencegah transisi mundur dari {@code RESOLVED} kembali ke {@code AKTIF}
	 * (yang memang kadang sah secara klinis).</p>
	 *
	 * @param statusKlinis status klinis baru
	 */
	public void setStatusKlinis(String statusKlinis) {
		this.statusKlinis = statusKlinis;
	}

	/**
	 * Mengembalikan waktu alergi ini dicatat secara klinis; default waktu instansiasi objek.
	 *
	 * <p>Dipetakan {@code TIMESTAMP}. Dipakai konsumen sebagai kunci pengurutan:
	 * {@code ApotikPasienHelper#detail(...)} mengambil 50 alergi terbaru dengan
	 * {@code addOrder(Order.desc("tanggalCatat"))}, dan {@code profilKeselamatanResep}
	 * mengurutkan dengan cara yang sama. Batas 50 baris itu perlu dicatat: pasien dengan
	 * riwayat alergi sangat panjang akan mendapat daftar terpotong pada endpoint detail —
	 * pemeriksa keselamatan resep sendiri tidak memasang {@code setMaxResults}, sehingga ia
	 * tetap memeriksa seluruh alergi aktif.</p>
	 *
	 * <p>Jangan dikelirukan dengan {@link #getTanggal_dirubah()} yang menandai penyuntingan
	 * baris. Setelah alergi dinonaktifkan, {@code tanggalCatat} tetap menunjuk pencatatan awal
	 * sementara {@code tanggal_dirubah} bergerak — perbedaan itu yang memungkinkan menghitung
	 * berapa lama sebuah alergi berlaku.</p>
	 *
	 * @return waktu pencatatan klinis
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_catat", nullable = true)
	public Date getTanggalCatat() {
		return tanggalCatat;
	}

	/**
	 * Menyetel waktu pencatatan klinis alergi.
	 *
	 * @param tanggalCatat waktu pencatatan
	 */
	public void setTanggalCatat(Date tanggalCatat) {
		this.tanggalCatat = tanggalCatat;
	}

	/**
	 * Mengembalikan nama tenaga kesehatan yang mencatat alergi ini.
	 *
	 * <p>Disimpan sebagai <b>teks bebas 60 karakter</b>, bukan relasi ke {@link Dokter} maupun
	 * ke {@code Tbmuser}. Pilihan itu punya sisi baik dan buruk. Sisi baiknya: pencatat bisa
	 * saja perawat, apoteker, atau petugas yang tidak punya baris {@link Dokter}, dan teks bebas
	 * menampung semuanya tanpa memaksa membuat master baru. Sisi buruknya: nilainya tidak dapat
	 * ditelusuri ke identitas sistem, tidak dapat diaudit silang, dan dapat diketik apa saja —
	 * termasuk nama orang lain. Data contoh dari {@code ApotikDemoProvisionHelper}, misalnya,
	 * mengisinya {@code "Apoteker Demo"}.</p>
	 *
	 * <p>Untuk keperluan telusur yang sungguhan, andalkan {@link #getOlehId()} dan jejak Envers
	 * — keduanya diisi kerangka, bukan diketik pengguna. Field ini sebaiknya diperlakukan
	 * sebagai catatan bagi pembaca manusia, bukan sebagai bukti.</p>
	 *
	 * @return nama pencatat, atau {@code null}
	 */
	@Column(name = "pencatat", nullable = true, length = 60)
	public String getPencatat() {
		return pencatat;
	}

	/**
	 * Menyetel nama tenaga kesehatan pencatat. Teks bebas tanpa verifikasi identitas — lihat
	 * {@link #getPencatat()}.
	 *
	 * @param pencatat nama pencatat
	 */
	public void setPencatat(String pencatat) {
		this.pencatat = pencatat;
	}

	/**
	 * Mengembalikan catatan bebas tentang alergi ini.
	 *
	 * <p>Dipakai {@code ApotikDemoProvisionHelper} untuk menandai data contoh dengan kalimat
	 * "DATA SAMPLE/UAT — wajib diverifikasi sebelum penggunaan nyata". Penanda itu adalah
	 * satu-satunya cara membedakan alergi hasil penanaman demo dari alergi nyata bila keduanya
	 * pernah bercampur — bersama {@code olehId = "seed_demo"}. Perhatikan bahwa penanda ini
	 * hanya konvensi, bukan mekanisme: tidak ada kode yang menyaring berdasarkan isinya, dan
	 * {@code profilKeselamatanResep} akan memperlakukan alergi demo persis seperti alergi
	 * nyata.</p>
	 *
	 * @return catatan bebas, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang alergi.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Derived, bukan kolom DB — WAJIB @Transient agar Hibernate tidak mencari setter "aktif".
	 *
	 * <p>Peringatan pada kalimat di atas perlu dibaca serius, karena ia menjelaskan sebuah
	 * perangkap Hibernate yang mudah menyebabkan kegagalan startup. Pemetaan entity ini berbasis
	 * <b>akses properti</b> (anotasi {@code @Id} dipasang pada getter, bukan pada field),
	 * sehingga Hibernate memindai <i>setiap</i> method yang berbentuk getter JavaBean dan
	 * menganggapnya properti persisten kecuali diberi {@code @Transient}. Method ini bernama
	 * {@code isAktif()} tanpa argumen dan mengembalikan {@code boolean} — bentuk getter yang
	 * sah untuk properti bernama {@code aktif}. Tanpa {@code @Transient}, Hibernate akan
	 * mencari {@code setAktif(boolean)} yang tidak ada dan gagal memetakan kelas saat
	 * {@code SessionFactory} dibangun — yang berarti <b>seluruh aplikasi gagal start</b>, bukan
	 * sekadar fitur ini yang rusak.</p>
	 *
	 * <p>Bandingkan dengan {@link KepesertaanPasien#berlakuPada(Date)} yang juga logika turunan
	 * tetapi <b>tidak</b> memerlukan {@code @Transient}: karena ia menerima argumen, ia bukan
	 * getter JavaBean dan Hibernate mengabaikannya. Perbedaan halus ini layak diingat sebelum
	 * menambahkan method turunan baru ke entity mana pun di basis kode ini.</p>
	 *
	 * <p>Efek sampingnya yang menguntungkan: karena {@code aktif} tidak menjadi properti
	 * terpetakan, {@code GenericCrudAutoDefinitionFactory.hasBooleanProperty(metadata, "aktif")}
	 * tidak menemukannya, sehingga entity ini dinyatakan <i>tidak</i> soft-deletable oleh CRUD
	 * generik. Data alergi karena itu tidak dapat "dihapus" lewat permukaan generik — sejalan
	 * dengan prinsip kelas ini bahwa penonaktifan harus lewat perubahan
	 * {@link #getStatusKlinis()}.</p>
	 *
	 * <p>Secara fungsional method hanyalah pintasan
	 * {@code STATUS_AKTIF.equals(getStatusKlinis())}. Ia memanggil getter (bukan field) sehingga
	 * ikut menikmati normalisasi nilai kosong menjadi {@link #STATUS_AKTIF} — alergi yang
	 * statusnya belum terisi dilaporkan aktif, arah yang benar untuk data keselamatan.
	 * Perbandingannya {@code case-sensitive} dan konstanta ditaruh di sisi kiri sehingga aman
	 * terhadap {@code null}.</p>
	 *
	 * <p>Perhatikan bahwa konsumen di {@code ApotikApiHelper} <b>tidak</b> memakai method ini —
	 * mereka menyaring di basis data dengan {@code Restrictions.eq("statusKlinis",
	 * STATUS_AKTIF)}. Method ini karena itu berguna untuk pemeriksaan pada objek yang sudah di
	 * tangan, bukan untuk membangun query.</p>
	 *
	 * @return {@code true} bila status klinis alergi ini {@link #STATUS_AKTIF} (termasuk bila
	 *         statusnya kosong dan dinormalkan menjadi aktif)
	 */
	@Transient
	public boolean isAktif() {
		return STATUS_AKTIF.equals(getStatusKlinis());
	}
}
