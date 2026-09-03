package ais.database.model.akunting;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Penanda anti-posting-ganda untuk {@link ais.action.servlet.api.JurnalPenyesuaianHelper} &mdash;
 * satu baris = satu (template, periode) yang <b>sudah</b> diposting.
 *
 * <p>Entity ini adalah <b>kunci idempotensi yang sesungguhnya</b> bagi fitur Jurnal Penyesuaian
 * Berkala (amortisasi biaya dibayar di muka, akrual beban, penyisihan piutang tak tertagih), dan
 * ia benar-benar <b>sudah terpasang serta terpakai</b> &mdash; bukan rancangan yang menganggur.
 * Verifikasi jalur pemakaian, seluruhnya di
 * {@code ais.action.servlet.api.JurnalPenyesuaianHelper}: kelas ini terdaftar di
 * {@code hibernate.cfg.xml}, dibaca oleh {@code sudahDiposting(...)} sebagai sumber kebenaran
 * utama, dibaca lagi oleh {@code hapus(...)} sebagai penjaga hapus-template, dan
 * <b>ditulis</b> oleh {@code jalankan(...)} pada transaksi yang sama dengan jurnalnya. Tidak ada
 * pembaca/penulis lain di seluruh repositori &mdash; permukaannya sempit dan seluruhnya berada di
 * satu helper.</p>
 *
 * <h2>Kenapa entity ini ada</h2>
 * <p>Sebelumnya satu-satunya penjaga adalah penanda TEKS {@code [PENYESUAIAN <id> <periode>]}
 * yang ditempel pada {@link PostingHistory#getKeterangan()} lalu dicari lewat {@code LIKE} sebelum
 * memposting (lihat Javadoc {@link TemplateJurnalPenyesuaian} butir 5 untuk riwayat lengkap
 * celahnya). Penjaga teks itu punya beberapa kelemahan struktural yang tidak bisa ditutup tanpa
 * kunci basis data sungguhan: rentan TOCTOU pada permintaan bersamaan, penandanya hilang bila
 * template dihapus lalu dibuat ulang (id baru), dan bisa terpotong diam-diam bila nama template
 * panjang mendorong keterangan melewati batas kolom {@code varchar(255)}.</p>
 * <p>Tabel ini menutup ketiganya sekaligus dengan {@code UNIQUE (template_id, periode)} yang
 * ditegakkan basis data &mdash; bukan query {@code SELECT} lalu {@code INSERT} yang bisa diselip
 * permintaan lain di antaranya. {@code JurnalPenyesuaianHelper.jalankan(...)} menulis baris ini
 * TEPAT SEBELUM membuat {@link PostingHistory} pada transaksi yang sama; pelanggaran unique
 * constraint (dua permintaan bersamaan menembak (template, periode) yang sama) ditangkap sebagai
 * {@code org.hibernate.exception.ConstraintViolationException} dan diperlakukan sebagai "sudah
 * diposting" &mdash; pola yang sama dipakai {@code KantinHelper} untuk idempotensi buka sesi kas.</p>
 *
 * <h2>Urutan tulis di dalam satu transaksi (penting, tidak jelas dari luar)</h2>
 * <p>Urutan langkah di {@code jalankan(...)} bukan kebetulan; ia yang membuat penjaga ini
 * berfungsi sebagai kunci, bukan sekadar catatan:</p>
 * <ol>
 *   <li>{@code beginTransaction()};</li>
 *   <li>baris penanda ini di-{@code save} lalu di-{@code flush} <b>SEGERA</b> &mdash; flush inilah
 *   yang memaksa basis data memutuskan pemenang lomba, sebelum satu baris jurnal pun ditulis;</li>
 *   <li>{@link PostingHistory} dibuat dan di-{@code flush} untuk memperoleh id;</li>
 *   <li>id itu ditulis balik ke {@link #getPostingHistoryId()} (karena itu kolomnya
 *   {@code nullable} &mdash; saat {@code INSERT} pertama nilainya memang belum ada);</li>
 *   <li>{@code CommonAkunting.saveTransaksi(...)} menerbitkan pasangan baris debet/kredit;</li>
 *   <li>{@code commit()}.</li>
 * </ol>
 * <p>Konsekuensi yang menenangkan: bila langkah 5 gagal (mis. {@code saveTransaksi}
 * mengembalikan {@code false} karena periode sudah ditutup), seluruh transaksi di-{@code rollback}
 * &mdash; termasuk baris penanda ini. Jadi kegagalan posting <b>tidak</b> meninggalkan penanda
 * hantu yang memblokir percobaan ulang yang sah. Penanda hanya bertahan bila jurnalnya benar-benar
 * jadi.</p>
 *
 * <h2>Kompatibilitas mundur</h2>
 * <p>Periode yang sudah diposting SEBELUM tabel ini ada tidak punya baris di sini. Karena itu
 * {@code JurnalPenyesuaianHelper.sudahDiposting(...)} tetap memeriksa penanda teks lama sebagai
 * fallback -- tabel ini adalah sumber kebenaran untuk posting BARU, bukan pengganti retroaktif
 * tanpa migrasi data historis (migrasi tersebut belum dijalankan; perlu kredensial basis data
 * produksi untuk mem-back-fill dari {@code posting_history.keterangan} lama).</p>
 * <p>Akibat langsung yang perlu diketahui pembaca: penjaga hapus-template di
 * {@code JurnalPenyesuaianHelper.hapus(...)} <b>hanya</b> menghitung baris tabel ini. Template
 * yang riwayat postingnya semata-mata berupa penanda teks lama terbaca "belum pernah diposting"
 * oleh penjaga itu, sehingga masih bisa dihapus lalu dibuat ulang dengan id baru &mdash; dan
 * periode yang dulu sudah diposting menjadi bisa diposting ulang. Celah residual ini disadari dan
 * didokumentasikan di Javadoc method tersebut; penutupnya adalah backfill data historis, bukan
 * perubahan pada kelas ini.</p>
 *
 * <h2>{@code templateId} sengaja BUKAN relasi JPA</h2>
 * <p>Kolom ini kolom biasa ({@code Long}), bukan {@code @ManyToOne} ke
 * {@link TemplateJurnalPenyesuaian}. Menghapus template TIDAK menghapus jurnal yang sudah
 * terbentuk (baris {@link PostingHistory}/{@link Transaksi} tetap ada -- lihat Javadoc
 * {@link TemplateJurnalPenyesuaian} butir 4), sehingga baris penanda ini pun harus tetap bertahan
 * setelah template-nya dihapus; relasi JPA dengan cascade delete akan menghapus jejak itu dan
 * membuka kembali celah "hapus lalu buat ulang template = id baru = penanda hilang". Sebagai
 * gantinya {@code JurnalPenyesuaianHelper.hapus(...)} menolak menghapus template yang sudah punya
 * baris penanda sama sekali -- lihat Javadoc method itu.</p>
 * <p>Harga yang dibayar: tidak ada integritas referensial yang ditegakkan basis data, jadi
 * {@link #getTemplateId()} bisa saja menunjuk template yang sudah tidak ada. Itu <b>disengaja</b>
 * dan tidak merusak apa pun &mdash; nilainya dipakai murni sebagai bagian kunci idempotensi
 * (dibandingkan dengan {@code Restrictions.eq("templateId", ...)}), tidak pernah untuk
 * mengambil objek template.</p>
 *
 * <h2>Bentuk kelas: sengaja minimal</h2>
 * <p>Berbeda dari {@link TemplateJurnalPenyesuaian} yang {@code extends GeneralValueObject},
 * kelas ini {@code implements Serializable} langsung dan tidak mewarisi apa pun. Ini baris
 * infrastruktur, bukan dokumen yang dilihat/diedit operator: tidak ada layar ZK, tidak ada
 * {@code oleh}/{@code tanggal_dibuat} gaya dokumen, tidak ada bendera aktif, dan tidak ada
 * {@code @Audited} (Envers) &mdash; berbeda lagi dari {@link TemplateJurnalPenyesuaian} yang
 * versinya digandakan ke {@code template_jurnal_penyesuaian_aud}. Riwayat perubahan tidak
 * diperlukan karena baris ini memang tidak pernah diubah setelah ditulis (kecuali satu write-back
 * {@link #setPostingHistoryId(Long)} pada transaksi yang sama) dan tidak pernah dihapus.</p>
 * <p>Kelas ini juga tidak meng-override {@code equals}/{@code hashCode}. Aman dalam pemakaian
 * saat ini karena instansnya tidak pernah dimasukkan ke {@code Set}/{@code Map} maupun
 * dibandingkan antar-sesi; keunikan dijamin basis data, bukan identitas objek Java.</p>
 *
 * <h2>Cakupan organisasi: global, dan itu konsisten</h2>
 * <p>Tabel ini tidak punya kolom satuan kerja/sekolah/yayasan, jadi kunci
 * {@code (template_id, periode)} berlaku GLOBAL lintas tenant. Ini <b>bukan</b> erosi cakupan
 * tenant gaya fail-open yang lazim ditemukan di modul lain: modul Jurnal Penyesuaian Berkala
 * memang global secara rancangan &mdash; {@code JurnalPenyesuaianHelper.semuaTemplate(...)} sama
 * sekali tidak menyaring berdasarkan satuan kerja, dan satuan kerja jurnal yang terbit dipaksa
 * dari konfigurasi global lewat {@code AkunKantinUtil.satkerKantin()}. Karena daftar templatenya
 * global, kunci idempotensi yang global justru yang benar; menambahkan sumbu tenant di sini tanpa
 * ikut menenantkan daftar templatenya malah akan MELONGGARKAN penjaga (satu template yang sama
 * bisa diposting sekali per tenant untuk periode yang sama).</p>
 *
 * <h2>Kuirk yang diketahui</h2>
 * <ul>
 *   <li><b>Sesi Hibernate dipakai ulang setelah {@code ConstraintViolationException}.</b> Di
 *   {@code jalankan(...)} loop template berbagi satu {@code Session}; setelah tabrakan unique
 *   ditangkap dan di-{@code rollback}, sesi yang sama tetap dipakai untuk template berikutnya,
 *   padahal kontrak Hibernate meminta sesi dibuang setelah exception. Efeknya terbatas pada
 *   <b>ketersediaan, bukan integritas</b>: yang mungkin terjadi adalah template berikutnya ikut
 *   gagal (masuk daftar {@code masalah}), bukan jurnal ganda. Jadi arah kegagalannya aman.</li>
 *   <li><b>Tabel dibuat lewat {@code hbm2ddl.auto=update}</b>, tanpa skrip SQL migrasi eksplisit
 *   &mdash; sama seperti tabel saudaranya {@code template_jurnal_penyesuaian}. Unique constraint
 *   ikut terbentuk saat tabel BARU dibuat; bila suatu instalasi terlanjur punya tabel ini tanpa
 *   constraint tersebut, {@code hbm2ddl=update} tidak akan menambahkannya belakangan dan penjaga
 *   turun kembali menjadi sekadar "cek lalu sisip" yang rentan TOCTOU. Layak diverifikasi sekali
 *   per instalasi.</li>
 *   <li><b>{@link #getPeriode()} tidak memvalidasi apa pun.</b> Format kanonik {@code yyyy-MM}
 *   ditegakkan di hulu oleh {@code jalankan(...)} lewat regex ketat
 *   {@code \\d{4}-(0[1-9]|1[0-2])}. Regex itu bagian penting dari penjaga: tanpanya
 *   {@code "2026-1"} dan {@code "2026-01"} mem-parse ke bulan yang sama tetapi menghasilkan DUA
 *   kunci berbeda, sehingga beban yang sama bisa dibukukan dua kali hanya dengan mengubah ejaan
 *   periode lewat REST. Penulis baru mana pun ke tabel ini WAJIB menormalkan periode lebih dulu
 *   dengan cara yang sama.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ul>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Kunci idempotensi (pasangan yang di-{@code UNIQUE}-kan):</b>
 *   {@link #getTemplateId()}/{@link #setTemplateId(Long)} dan
 *   {@link #getPeriode()}/{@link #setPeriode(String)}.</li>
 *   <li><b>Telusur/audit ringan:</b> {@link #getPostingHistoryId()}/{@link #setPostingHistoryId(Long)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)}, dan
 *   {@link #getDibuatPada()}/{@link #setDibuatPada(Date)}.</li>
 * </ul>
 * <p>Seluruh getter/setter di kelas ini adalah aksesor murni: tidak ada substitusi nilai, tidak
 * ada perhitungan ulang, dan &mdash; berbeda dari beberapa entity akunting lain seperti
 * {@code Transaksi.getAkun()} atau {@code ProsesTransitori.getDisetujuiOleh()} &mdash; <b>tidak
 * ada getter yang menulis balik ke state objek</b>. Membaca baris penanda tidak pernah mengubahnya.</p>
 *
 * @see ais.action.servlet.api.JurnalPenyesuaianHelper
 * @see TemplateJurnalPenyesuaian
 * @see PostingHistory
 */
@Entity
@Table(schema = "akunting", name = "penanda_jurnal_penyesuaian",
		uniqueConstraints = @UniqueConstraint(name = "uq_penanda_jurnal_penyesuaian_template_periode",
				columnNames = { "template_id", "periode" }))
public class PenandaJurnalPenyesuaian implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Kunci utama, dibangkitkan basis data. */
	private Long id;
	/** Id {@link TemplateJurnalPenyesuaian}; kolom biasa, BUKAN relasi JPA -- lihat Javadoc kelas. */
	private Long templateId;
	/** Periode berformat {@code yyyy-MM}, sudah divalidasi ketat oleh pemanggil sebelum ditulis. */
	private String periode;
	/** Id {@link PostingHistory} yang terbentuk dari penanda ini; untuk telusur, boleh {@code null}. */
	private Long postingHistoryId;
	/** Id pengguna yang memposting; jejak audit ringan, boleh {@code null}. */
	private String olehId;
	/**
	 * Waktu baris ini ditulis; diisi saat objek dibentuk sehingga baris baru selalu punya stempel
	 * walau pemanggil tidak menyetelnya. Untuk baris yang dimuat dari basis data nilai awal ini
	 * langsung ditimpa Hibernate lewat {@link #setDibuatPada(Date)}.
	 */
	private Date dibuatPada = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib Hibernate. */
	public PenandaJurnalPenyesuaian() {
	}

	/**
	 * Kunci utama baris penanda.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate saat memuat/menyimpan baris; kode aplikasi tidak
	 * perlu memanggilnya karena nilainya dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @param id kunci utama baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Id {@link TemplateJurnalPenyesuaian} yang postingnya ditandai baris ini &mdash; komponen
	 * pertama kunci idempotensi {@code UNIQUE (template_id, periode)}.
	 *
	 * <p>Sengaja {@code Long} biasa, bukan relasi JPA (lihat Javadoc kelas), sehingga nilainya
	 * bisa saja menunjuk template yang sudah dihapus. Itu tidak masalah: nilai ini hanya
	 * dibandingkan sebagai angka oleh {@code JurnalPenyesuaianHelper.sudahDiposting(...)} dan
	 * {@code hapus(...)}, tidak pernah dipakai untuk memuat objek template.</p>
	 *
	 * @return id template, tidak pernah {@code null} untuk baris yang tersimpan.
	 */
	@Column(name = "template_id", nullable = false)
	public Long getTemplateId() {
		return templateId;
	}

	/**
	 * Menyetel id template pemilik penanda.
	 *
	 * @param templateId id {@link TemplateJurnalPenyesuaian}; wajib terisi sebelum disimpan
	 *        (kolomnya {@code NOT NULL}).
	 */
	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
	}

	/**
	 * Periode akuntansi yang sudah diposting, berformat kanonik {@code yyyy-MM} &mdash; komponen
	 * kedua kunci idempotensi.
	 *
	 * <p>Getter ini TIDAK menormalkan apa pun; format kanonik ditegakkan di hulu oleh
	 * {@code JurnalPenyesuaianHelper.jalankan(...)}. Lihat butir "Kuirk yang diketahui" pada
	 * Javadoc kelas mengenai kenapa normalisasi hulu itu wajib bagi setiap penulis baru.</p>
	 *
	 * @return periode {@code yyyy-MM}, tidak pernah {@code null} untuk baris yang tersimpan.
	 */
	@Column(name = "periode", nullable = false, length = 7)
	public String getPeriode() {
		return periode;
	}

	/**
	 * Menyetel periode yang ditandai.
	 *
	 * @param periode teks periode; pemanggil bertanggung jawab memastikan bentuknya kanonik
	 *        {@code yyyy-MM} (panjang kolom hanya 7 karakter, dan ejaan non-kanonik akan
	 *        menghasilkan kunci idempotensi terpisah).
	 */
	public void setPeriode(String periode) {
		this.periode = periode;
	}

	/**
	 * Id {@link PostingHistory} yang terbentuk bersama penanda ini &mdash; jejak telusur dari
	 * penanda ke cap posting, dan lewat cap itu ke baris {@link Transaksi} yang terbit.
	 *
	 * <p>Boleh {@code null}: saat baris penanda pertama kali di-{@code INSERT}, cap postingnya
	 * memang belum dibuat (lihat urutan langkah pada Javadoc kelas). Nilainya diisi beberapa
	 * baris kemudian dalam transaksi yang sama. Baris yang tetap {@code null} setelah commit
	 * seharusnya tidak ada; bila ditemukan, itu petunjuk baris ditulis oleh jalur lain di luar
	 * {@code JurnalPenyesuaianHelper}.</p>
	 *
	 * @return id cap posting, atau {@code null} bila belum/tidak terisi.
	 */
	@Column(name = "posting_history_id", nullable = true)
	public Long getPostingHistoryId() {
		return postingHistoryId;
	}

	/**
	 * Menyetel id cap posting hasil penanda ini.
	 *
	 * <p>Satu-satunya pembaruan yang pernah dialami baris penanda setelah tersimpan, dan
	 * dilakukan di dalam transaksi yang sama dengan {@code INSERT}-nya.</p>
	 *
	 * @param postingHistoryId id {@link PostingHistory} yang baru terbentuk.
	 */
	public void setPostingHistoryId(Long postingHistoryId) {
		this.postingHistoryId = postingHistoryId;
	}

	/**
	 * Id pengguna yang menjalankan posting &mdash; jejak audit ringan "siapa yang membukukan
	 * periode ini".
	 *
	 * <p>Diisi dari {@code Tbmuser.getUserId()} pengguna yang meminta aksi
	 * {@code penyesuaian_posting}. Boleh {@code null} secara skema, tetapi jalur posting menolak
	 * berjalan tanpa pengguna terautentikasi, sehingga baris nyata selalu terisi.</p>
	 *
	 * @return id pengguna pemosting, atau {@code null}.
	 */
	@Column(name = "oleh_id", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pemosting.
	 *
	 * @param olehId id pengguna ({@code Tbmuser.getUserId()}).
	 */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Waktu baris penanda ditulis.
	 *
	 * <p>Perhatikan ini adalah waktu POSTING DIJALANKAN, bukan tanggal jurnalnya. Tanggal jurnal
	 * ditentukan terpisah oleh pemanggil (tanggal yang diketik operator, atau hari terakhir
	 * periode bila dikosongkan) dan tersimpan pada {@link PostingHistory}, bukan di sini.</p>
	 *
	 * @return stempel waktu pembuatan baris, tidak pernah {@code null}.
	 */
	@Column(name = "dibuat_pada", nullable = false)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	/**
	 * Menyetel stempel waktu pembuatan baris.
	 *
	 * @param dibuatPada waktu penulisan baris; menyetel {@code null} akan menggagalkan
	 *        penyimpanan karena kolomnya {@code NOT NULL}.
	 */
	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}
}
