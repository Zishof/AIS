package ais.database.model.akunting;

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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity <b>SETORAN / KEWAJIBAN PAJAK</b> pada mesin akuntansi: satu baris tabel
 * {@code akunting.pajak} mewakili <b>satu kewajiban pajak (PPh atau PPN) yang timbul dari satu
 * dokumen sumber</b> dan yang harus disetorkan ke kas negara. Nama kelasnya menyesatkan singkat —
 * ini <b>BUKAN</b> master tarif pajak. Master tarifnya adalah
 * {@code ais.database.model.asset.JenisPajakBarang} (PPh, punya {@code getPersen()} dan
 * {@code getAkun()}/{@code getAkunDanaTitipan()}) dan
 * {@code ais.database.model.asset.JenisPajakPpn} (PPN); class ini hanya <i>menunjuk</i> keduanya.
 * Ada pula {@code ais.database.model.sirs.PajakMedis} (modul rumah sakit) yang sama sekali tidak
 * berhubungan dengan class ini walau layar SIRS-nya juga bernama "Pajak".
 *
 * <h2>Jawaban atas pertanyaan "kaki pajak" mesin posting — HASIL VERIFIKASI</h2>
 * <p>Dugaan bahwa {@code Pertangungjawaban}/{@code PertangungjawabanKasBesar} adalah dokumen
 * "berkaki ganda" (jurnal utama + PAJAK + pengembalian) <b>benar sebagai niat desain, tetapi tidak
 * seluruhnya terwujud</b>. Rinciannya, terverifikasi dari kode:</p>
 * <ul>
 *   <li>Kedua entity LPJ itu memang punya <b>tiga</b> cap posting: {@code postingHistory} (kaki
 *   utama), {@code postingHistoryPajak} (kaki pajak), dan {@code postingHistoryPengembalian}
 *   (kaki pengembalian sisa uang muka).</li>
 *   <li>Kaki <b>pengembalian</b> sudah terimplementasi:
 *   {@code PostingPertangungjawabanPengembalianAction} benar-benar memanggil
 *   {@code setPostingHistoryPengembalian(...)}.</li>
 *   <li>Kaki <b>pajak TIDAK pernah dipakai</b>: di seluruh repo tidak ada satu pun pemanggil
 *   {@code setPostingHistoryPajak(...)} selain setter di entity-nya sendiri. Kolom
 *   {@code posting_history_pajak} praktis mati — hanya <i>dibaca</i> sebagai penjaga hapus di
 *   {@code PertangungjawabanApiHelper}/{@code PertangungjawabanKasBesarApiHelper} ("sudah dijurnal
 *   sehingga tidak boleh dihapus"). Komentar di
 *   {@code PostingPertangungjawabanKasBesarAction.batalkanPostingSemua} menyebut hal yang sama.</li>
 *   <li>Sebagai gantinya, <b>kaki pajak diwujudkan sebagai DOKUMEN TERSENDIRI, yaitu baris entity
 *   INI</b>. Setiap item biaya LPJ yang memiliki Jenis PPh menghasilkan satu baris {@code Pajak}
 *   yang menyimpan cap posting-nya sendiri ({@link #getPostingHistory()}), diposting dari layar
 *   terpisah {@code PostingPertangungjawabanPajakAction}, dan jurnalnya ditandai dengan
 *   diskriminator {@code ref = "pajak"} pada {@code akunting.grup_transaksi} supaya pembatalan
 *   posting kaki utama (yang menyaring {@code ref is null}) tidak ikut menghapusnya.</li>
 * </ul>
 * <p>Jadi: <b>ya, class ini adalah "kaki pajak" mesin posting</b> — tetapi berupa dokumen saudara,
 * bukan cap kedua pada dokumen LPJ. Dan sekaligus <b>bukan hanya</b> itu: entity yang sama dipakai
 * untuk PPh pengadaan aset, PPh termin kontrak, dan setoran manual PPh/PPN dari POS.</p>
 *
 * <h2>Lima jalur pembuatan baris (sumber data)</h2>
 * <p>Baris {@code Pajak} tidak pernah dibuat lewat layar CRUD sendiri; semuanya diterbitkan mesin:</p>
 * <ol>
 *   <li><b>LPJ uang muka</b> — {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject,
 *   SaldoAwalMasterAssetDetail)} dari {@code PertangungjawabanAction}, satu baris per item
 *   {@code formula} (JSON) yang punya field {@code pajak}. Relasi terisi: {@link #getPertangungjawaban()},
 *   {@link #getKeyData()}, {@link #getPajakData()}.</li>
 *   <li><b>LPJ kas besar</b> — jalur kembar dari {@code PertangungjawabanKasBesarAction}; relasi
 *   {@link #getPertangungjawabanKasBesar()}.</li>
 *   <li><b>Tagihan pengadaan aset (per item)</b> — {@code SaldoAwalMasterAssetDetailAction} dan
 *   {@code SaldoAwalPunyaMasterAssetHelper}; relasi {@link #getSaldoAwalMasterAssetDetail()}.
 *   Nominalnya <b>tidak disimpan</b>, melainkan dihitung ulang setiap dibaca (lihat di bawah).</li>
 *   <li><b>Tagihan pengadaan aset (mode BREAKDOWN)</b> — {@code BreakdownTagihanVendorHelper.sinkronPajakBreakdown}
 *   membuat SATU baris yang tertaut langsung ke tagihannya lewat {@link #getSaldoAwal()}, dengan
 *   {@link #getNilai()} = angka <i>Bukti Potong</i> yang diketik manual (bukan tarif x DPP). Saat mode ini
 *   menyala, baris per item jalur (3) dipadamkan lewat {@code aktif = false} dan disaring dari kedua
 *   layar pajak lewat {@code LEFT JOIN ... breakdownAktif}.</li>
 *   <li><b>PPh pembayaran termin kontrak</b> — {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)};
 *   baris ini <b>tidak punya relasi apa pun</b>, ia hanya ditandai {@link #getKeyData()} = id detail
 *   termin (lihat catatan idempotensi di method itu).</li>
 * </ol>
 * <p>Jalur keenam ada di luar mesin ini: {@code PengadaanPosApiHelper.pajakSetor} (layar POS
 * "Bayar Pajak") membuat baris {@code Pajak} lepas — tanpa relasi, dengan {@link #getNtpn()} dan
 * {@link #getTanggalStor()} terisi — lalu <i>dokumen sumbernya</i> yang menunjuk balik ke sini
 * ({@code PembayaranTerminMasterAssetDetail.getPajak()} dan
 * {@code PenerimaanPengadaanMasterAssetDetail.getPajak()}). Arah relasi terbalik itu tidak terlihat
 * dari file ini dan mudah terlewat saat menelusuri pemakaian.</p>
 *
 * <h2>Hilir: baris pembayaran di Daftar Pengajuan Transfer</h2>
 * <p>Setiap baris {@code Pajak} didorong ke kolam antrean pembayaran lewat
 * {@link DaftarPengajuanTransfer#simpanPajak(Pajak)}, menghasilkan satu baris DPT bertipe
 * {@code pajak} yang berpasangan dengan baris pembayaran vendor: vendor menerima <i>netto</i>,
 * pajak yang dipotong disetorkan lewat baris tersendiri, sehingga berlaku
 * <code>netto(vendor) + Σ(baris pajak) = bruto</code>. Rekening tujuan setoran diambil dari
 * {@code getJenisPajakBarang().getAkun()} (bank/atas nama/no rekening), jadi <b>master tarif PPh
 * sekaligus memegang rekening kas negara</b> yang dituju.</p>
 *
 * <h2>HAL PALING NON-OBVIOUS: sebagian besar getter di sini MENULIS ke database</h2>
 * <p>Sama seperti {@link DaftarPengajuanTransfer}, class ini memakai <b>property access</b>
 * (anotasi Hibernate menempel di getter) dan hampir semua getter "turunan" <b>menulis balik hasil
 * resolusinya ke field</b>. Karena field-nya properti terpetakan, penulisan itu terbaca oleh
 * <i>dirty checking</i> dan <b>tersimpan permanen</b> pada flush berikutnya — termasuk saat entity
 * hanya <i>dirender</i> di layar. Getter yang berperilaku begitu: {@link #getKode()},
 * {@link #getNama()}, {@link #getKeterangan()}, {@link #getJenisPajakBarang()},
 * {@link #getSatuanKerja()}, {@link #getNilai()}, {@link #getTanggal()}, {@link #getNpwp()},
 * {@link #getNamaWp()}, {@link #getJumlah()}, dan {@link #getDpp()}.</p>
 * <p>Untuk data finansial ini berarti <b>nominal pajak dan DPP bukan angka tersimpan, melainkan
 * hasil hitung ulang</b>: mengubah tarif pada {@code JenisPajakBarang} atau mengubah jumlah/harga
 * item tagihan akan <b>mengubah baris pajak lama secara retroaktif</b> pada pembacaan berikutnya,
 * termasuk baris yang sudah pernah dijurnal. Jurnal yang sudah terbentuk (tabel
 * {@code akunting.transaksi}) tidak ikut berubah, sehingga nilai di layar pajak dapat berbeda dari
 * nilai di buku besar tanpa jejak apa pun. Class ini juga {@code @Audited} (Envers), sehingga setiap
 * penulisan balik itu menerbitkan revisi baru di {@code akunting.pajak_aud} — sekadar membuka layar
 * pajak menggelembungkan tabel revisi.</p>
 *
 * <h2>Kuirk kolom {@code aktif}: satu kolom, dua makna yang bertabrakan</h2>
 * <p>Di layar <i>Data Pajak</i> ({@code pertangungjawaban_pajak.zul}) kolom ini berlabel
 * <b>"Bayar"</b> dan berpasangan dengan {@link #getTanggalTransaksi()} berlabel "Tanggal Bayar" —
 * artinya "setoran sudah dibayar". Tetapi {@code BreakdownTagihanVendorHelper} memakai kolom yang
 * sama sebagai <b>saklar hidup/mati baris</b> ({@code setAktif(!breakdown)}), dan
 * {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)} menyalakannya saat pembuatan semata-mata
 * "agar lolos filter aktif". Akibat praktisnya: menyalakan mode breakdown pada sebuah tagihan
 * <b>mencentang/mencentang-balik kotak "Bayar"</b> sejumlah baris pajak tanpa ada peristiwa
 * pembayaran, dan baris PPh termin lahir dalam keadaan sudah tercentang "Bayar". Kolom ini juga
 * menjadi gerbang posting: kedua layar pajak menyaring
 * {@code or(aktif is null, aktif = true)}.</p>
 *
 * <h2>Cakupan data (tenant/unit kerja)</h2>
 * <p>Entity ini <b>tidak punya kolom {@code sekolah} maupun {@code yayasan} sama sekali</b>.
 * Satu-satunya pembatas cakupan adalah {@link #getSatuanKerja()} (unit kerja RAB), yang
 * <b>nullable</b> dan diturunkan dari dokumen sumber. Baik {@code PertangungjawabanPajakAction}
 * maupun {@code PostingPertangungjawabanPajakAction} menyaring dengan pola
 * {@code or(satuanKerja is null, ...)} — sehingga baris tanpa satuan kerja selalu terlihat oleh
 * semua orang — dan jatuh ke {@code sqlRestriction("1=1")} bila pengguna tidak punya satuan kerja
 * sama sekali. Lihat catatan lebih rinci di {@link #getSatuanKerja()}.</p>
 *
 * <h2>Pengelompokan anggota class</h2>
 * <ol>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()} beserta {@code onUpdate()}; blok yang sama persis dipakai
 *   di {@link DaftarPengajuanTransfer}.</li>
 *   <li><b>Pabrik statis</b> — {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject,
 *   SaldoAwalMasterAssetDetail)} dan {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)},
 *   ditambah dua penelusur kode induk {@code cariKodeTagihanAtauBast} dan
 *   {@code cariKodePermintaanInduk}.</li>
 *   <li><b>Properti turunan</b> — getter cerdas yang menulis balik (daftar di atas).</li>
 *   <li><b>Relasi dokumen sumber</b> — {@link #getPertangungjawaban()},
 *   {@link #getPertangungjawabanKasBesar()}, {@link #getSaldoAwalMasterAssetDetail()},
 *   {@link #getSaldoAwal()}, plus penanda {@link #getKeyData()}/{@link #getPajakData()}.</li>
 *   <li><b>Data setoran</b> — {@link #getNtpn()}, {@link #getNpwp()}, {@link #getNamaWp()},
 *   {@link #getTanggalStor()}, {@link #getAktif()}, {@link #getTanggalTransaksi()}.</li>
 *   <li><b>Hilir</b> — {@link #getPostingHistory()} (jurnal) dan
 *   {@link #getDaftarPengajuanTransfer()} (antrean pembayaran).</li>
 * </ol>
 *
 * <p><b>Catatan pewarisan:</b> class ini {@code extends}
 * {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — melainkan POJO abstrak biasa. Hibernate karena itu <b>tidak</b>
 * memetakan properti milik induk, sehingga field seperti {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di sini; pengulangan itu bukan bug.
 * Yang dipakai dari induk adalah utilitas runtime-nya, terutama {@code check(...)} untuk resolusi
 * proxy lazy.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see DaftarPengajuanTransfer#simpanPajak(Pajak)
 * @see ais.database.model.asset.JenisPajakBarang
 * @see ais.database.model.asset.JenisPajakPpn
 * @see PostingHistory
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "pajak")
public class Pajak extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Wajib ada karena {@link ais.database.model.GeneralValueObject}
	 * mengimplementasikan {@code Serializable} dan instance entity ikut tersimpan di sesi HTTP ZK.
	 * Jangan diubah pada kelas yang sudah dipakai produksi agar tidak muncul
	 * {@code InvalidClassException} saat sesi lama dideserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (kolom {@code id}, IDENTITY). Dideklarasikan ulang di sini — lihat catatan pewarisan di Javadoc class. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini (jejak audit, diisi mesin/`AuditTimestampInterceptor`). */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini (jejak audit). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini. Getter polos.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disentuh jalur beraudit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir. <b>Setter defensif</b>: nilai {@code null} atau
	 * kosong <b>diabaikan</b> (nilai lama dipertahankan), sehingga jejak audit tidak pernah terhapus
	 * oleh pemanggil yang kebetulan tidak punya konteks pengguna — mis. mesin latar seperti
	 * {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)}
	 * atau {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)}.
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir. Sama defensifnya dengan {@link #setOlehId(String)}:
	 * {@code null}/kosong diabaikan agar nama lama tidak hilang.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini. Getter polos.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil kerangka kerja tepat sebelum {@code UPDATE} dijalankan,
	 * lalu mendelegasikan pengisian jejak audit ({@code oleh}, {@code olehId},
	 * {@link #getTanggal_dirubah()}) ke {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p><b>Konsekuensi yang perlu diketahui:</b> karena hampir semua getter di class ini menulis
	 * balik ke field (lihat Javadoc class), <i>membaca</i> entity di dalam sesi hidup sudah cukup
	 * untuk memicu {@code UPDATE}, dan kait ini akan mencatat pengguna yang kebetulan sedang membuka
	 * layar sebagai "pengubah" walau ia tidak menyunting apa pun. Cap yang sama juga masuk ke tabel
	 * revisi Envers {@code akunting.pajak_aud}.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Cap waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir. Setter polos; biasanya diisi
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh kode pemanggil.
	 *
	 * @param tanggal_dirubah cap waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir (kolom {@code tanggal_dirubah}, TIMESTAMP).
	 * Getter polos.
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat
	 *         di JVM ini, tetapi bisa {@code null} untuk baris lama hasil migrasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk <code>&lt;id&gt;-&lt;nama&gt;</code>.
	 *
	 * <p><b>Perhatian:</b> method ini membaca <b>field</b> {@code nama} langsung, <i>bukan</i>
	 * {@link #getNama()}. Untuk baris yang namanya diturunkan dari dokumen sumber (LPJ / detail
	 * tagihan) field itu masih {@code null} sampai getter-nya sempat dipanggil, sehingga
	 * {@code toString()} bisa menghasilkan {@code "123-null"}. Nilai ini dipakai antara lain sebagai
	 * kunci log pada tombol "Singkronkan" di {@code PertangungjawabanPajakAction}.</p>
	 *
	 * @return teks <code>id-nama</code>
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode dokumen. TURUNAN untuk baris berelasi (lihat {@link #getKode()}); diisi eksplisit untuk
	 * baris termin dan baris setoran POS.
	 */
	private String kode;
	/** Master tarif PPh yang dipakai baris ini; sekaligus pemegang akun jurnal + rekening setoran. */
	private JenisPajakBarang jenisPajakBarang;
	/** Master jenis PPN; terisi HANYA untuk setoran PPN dari layar POS (lihat {@link #getJenisPajakPpn()}). */
	private ais.database.model.asset.JenisPajakPpn jenisPajakPpn;
	/** Judul dokumen. TURUNAN untuk baris berelasi (lihat {@link #getNama()}); kolom NOT NULL. */
	private String nama;
	/** Keterangan bebas. TURUNAN untuk baris berelasi (lihat {@link #getKeterangan()}). */
	private String keterangan;
	/** Unit kerja RAB — SATU-SATUNYA pembatas cakupan data pada entity ini, dan nullable. */
	private SatuanKerja satuanKerja;
	/** Nominal pajak (rupiah). Sebagian jalur menghitungnya ulang tiap dibaca — lihat {@link #getNilai()}. */
	private Double nilai;
	/** Dasar Pengenaan Pajak (rupiah). Sebagian jalur menghitungnya ulang tiap dibaca — lihat {@link #getDpp()}. */
	private Double dpp;
	/** Kolom bermakna ganda: ditimpa menjadi PERSEN tarif oleh {@link #getJumlah()} bila jenis PPh terisi. */
	private Double jumlah;
	/** Tanggal dokumen/transaksi pajak; TURUNAN untuk baris berelasi (lihat {@link #getTanggal()}). */
	private Date tanggal;
	/** Penanda "sudah dibayar" DAN saklar hidup/mati baris — lihat kuirk di Javadoc class. */
	private Boolean aktif;

	/** Nomor Transaksi Penerimaan Negara (bukti setor); diisi jalur LPJ dan jalur setoran POS. */
	private String ntpn = "";
	/** NPWP wajib pajak; TURUNAN dari penyedia untuk baris tagihan pengadaan (lihat {@link #getNpwp()}). */
	private String npwp = "";
	/** Nama wajib pajak; TURUNAN dari penyedia untuk baris tagihan pengadaan (lihat {@link #getNamaWp()}). */
	private String namaWp = "";
	/** Tanggal setor ke kas negara (tanggal murni, tanpa jam). */
	private Date tanggalStor;

	/** Cap posting jurnal milik baris ini — inilah "kaki pajak" yang sesungguhnya (lihat Javadoc class). */
	private PostingHistory postingHistory;
	/** Dokumen sumber: LPJ uang muka. */
	private Pertangungjawaban pertangungjawaban;
	/** Dokumen sumber: LPJ kas besar. */
	private PertangungjawabanKasBesar pertangungjawabanKasBesar;
	/** Penanda item di dalam dokumen sumber: kunci item JSON LPJ, ATAU id detail termin. */
	private Long keyData;
	/** Salinan mentah item JSON LPJ; jadi sumber hitung {@link #getNilai()}/{@link #getDpp()} jalur LPJ. */
	private String pajakData;
	/** Dokumen sumber: satu item tagihan pengadaan aset (mode per-item). */
	private SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail;
	/** PPh mode BREAKDOWN: Pajak tertaut LANGSUNG ke tagihan vendor (tanpa detail PO);
	 *  nilai = Bukti Potong. getNilai() mengembalikan nilai tersimpan saat detail null. */
	private ais.database.model.asset.SaldoAwalMasterAsset saldoAwal;
	/** Tanggal pembayaran setoran (kolom {@code tanggal_transaksi}); di layar berlabel "Tanggal Bayar". */
	private Date tanggalTransaksi;

	/** Baris antrean pembayaran yang menampung setoran ini; dibuat {@link DaftarPengajuanTransfer#simpanPajak(Pajak)}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;

	/**
	 * Pabrik idempoten untuk <b>tiga</b> jalur pembuatan baris pajak sekaligus: LPJ kas besar, LPJ
	 * uang muka, dan item tagihan pengadaan aset. Method ini <b>membuka session Hibernate sendiri,
	 * mengelola transaksinya sendiri, dan langsung menerbitkan baris antrean pembayaran</b> lewat
	 * {@link DaftarPengajuanTransfer#simpanPajak(Pajak)} — jadi memanggilnya berarti menulis ke
	 * database, bukan sekadar membangun object.
	 *
	 * <p><b>Cara memakainya:</b> pemanggil mengisi <i>tepat satu</i> jalur dan membiarkan sisanya
	 * {@code null}:</p>
	 * <ul>
	 *   <li>{@code buat(lpj, null, itemJson, null)} — dari {@code PertangungjawabanAction} saat LPJ
	 *   uang muka disetujui, dipanggil sekali per elemen {@code formula} (JSONArray) milik LPJ.</li>
	 *   <li>{@code buat(null, lpjKasBesar, itemJson, null)} — jalur kembar dari
	 *   {@code PertangungjawabanKasBesarAction}.</li>
	 *   <li>{@code buat(null, null, null, detailTagihan)} — dari
	 *   {@code SaldoAwalMasterAssetDetailAction} dan {@code SaldoAwalPunyaMasterAssetHelper}.</li>
	 * </ul>
	 *
	 * <p><b>Gerbang mode BREAKDOWN (paling awal):</b> bila detail tagihan yang diberikan berada di
	 * bawah tagihan vendor yang memakai breakdown, method langsung {@code return} tanpa berbuat
	 * apa-apa. Alasannya: pada mode breakdown PPh yang sah adalah SATU baris "Bukti Potong" yang
	 * tertaut langsung ke tagihan ({@link #getSaldoAwal()}), sehingga menerbitkan baris per item
	 * akan membuat PPh dibayar dobel. Pemeriksaan itu dibungkus {@code catch (Throwable)} yang
	 * hanya dicatat ke {@code ErrorAuditUtil} — bila pembacaan gagal, alurnya <b>lanjut</b>
	 * (fail-open ke perilaku lama).</p>
	 *
	 * <p><b>Jalur LPJ (kas besar dan uang muka — dua blok yang identik salin-tempel).</b> Kunci
	 * idempotensinya adalah pasangan (dokumen LPJ, {@code keyData}), dengan {@code keyData} diambil
	 * dari field {@code key} pada JSON item. Baris lama dicari lebih dulu dan dipakai ulang bila
	 * ada. Yang <b>disimpan</b> hanyalah penanda dan data setoran ({@code keyData}, relasi LPJ,
	 * salinan JSON, jenis PPh, NPWP, NTPN, nama WP, tanggal setor) — <b>nominal dan DPP sengaja
	 * tidak diset</b>, karena {@link #getNilai()} dan {@link #getDpp()} menghitungnya ulang dari
	 * salinan JSON setiap kali dibaca.</p>
	 *
	 * <p><b>Dua {@code return} yang menghentikan SELURUH method</b> ada di dalam blok LPJ: bila JSON
	 * tidak punya {@code key}, atau bila {@code JenisPajakBarang} yang ditunjuk tidak ditemukan
	 * (termasuk saat item memang tanpa pajak). Karena {@code return} itu keluar dari method — bukan
	 * dari blok — blok jalur berikutnya ikut terlewat. Dalam praktik hal ini tidak berbahaya karena
	 * seluruh pemanggil hanya pernah mengisi satu jalur, tetapi menjadi jebakan bila kelak ada
	 * pemanggil yang mengisi dua argumen sekaligus.</p>
	 *
	 * <p><b>Jalur tagihan pengadaan aset.</b> Detail dimuat ULANG lewat session baru (memastikan
	 * object ter-attach), lalu:
	 * <ul>
	 *   <li>bila belum ada baris pajak <i>dan</i> detail punya Jenis PPh → baris baru dibuat, hanya
	 *   dengan relasi ke detail; semua atribut lain (kode, nama, DPP, nilai, satuan kerja, NPWP,
	 *   nama WP, tanggal) <b>diturunkan</b> oleh getter dari detail tersebut;</li>
	 *   <li>bila sudah ada baris pajak <i>tetapi</i> Jenis PPh detail dihapus → baris pajak
	 *   <b>DIHAPUS permanen</b> ({@code Common.refreshDelete}) supaya baris PPh di Daftar Pengajuan
	 *   Transfer ikut hilang.</li>
	 * </ul>
	 * Perhatikan bahwa penyimpanan pertama memicu {@link #getNama()}, yang untuk jalur ini membaca
	 * {@code detail.getMasterAsset().getNama()} <b>tanpa penjaga null</b>; bila master aset belum
	 * terisi, NPE-nya ditelan {@code catch (Exception)} di bawah dan baris pajak gagal terbit
	 * secara senyap (hanya tercatat di {@code ErrorAuditUtil}).</p>
	 *
	 * <p><b>Efek samping lain:</b> untuk ketiga jalur, {@link DaftarPengajuanTransfer#simpanPajak(Pajak)}
	 * dipanggil <i>setelah</i> session lokal ditutup dan memakai {@code currentNativeSession} milik
	 * pemanggil. Method itu pula yang secara tidak langsung mem-<i>persist</i> nominal pajak, karena
	 * saat baris DPT di-flush Hibernate membaca {@code getNominal()}-nya, yang meneruskan ke
	 * {@link #getNilai()} di sini.</p>
	 *
	 * <p><b>Dari mana dipanggil:</b> {@code PertangungjawabanAction} (2 titik),
	 * {@code PertangungjawabanKasBesarAction} (2 titik),
	 * {@code SaldoAwalMasterAssetDetailAction} (2 titik), {@code SaldoAwalPunyaMasterAssetHelper}
	 * (2 titik) — beberapa di antaranya dari jalur RENDER, sehingga sekadar membuka layar tagihan
	 * dapat menerbitkan baris pajak baru.</p>
	 *
	 * @param pertangungjawaban            LPJ uang muka sumber; {@code null} bila bukan jalur ini.
	 *                                     Diabaikan bila id-nya masih {@code null}
	 * @param pertangungjawabanKasBesar    LPJ kas besar sumber; {@code null} bila bukan jalur ini.
	 *                                     Diabaikan bila id-nya masih {@code null}
	 * @param pajakData                    item biaya LPJ dalam bentuk JSON; wajib memuat {@code key}
	 *                                     dan {@code pajak} (id {@code JenisPajakBarang}), opsional
	 *                                     {@code ntpn}, {@code npwp}, {@code namaWp},
	 *                                     {@code tanggalStor} (format {@code Common.dateFormat1})
	 * @param saldoAwalMasterAssetDetail   satu item tagihan pengadaan aset; {@code null} bila bukan
	 *                                     jalur ini
	 */
	public static void buat(Pertangungjawaban pertangungjawaban, PertangungjawabanKasBesar pertangungjawabanKasBesar,
			JSONObject pajakData, SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail) {

		// MODE BREAKDOWN: PPh tagihan diwakili SATU baris Pajak tertaut tagihan (nilai = Bukti
		// Potong). Maka JANGAN buat/refresh baris pajak PER DETAIL agar PPh tidak dibayar dobel.
		try {
			if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getSaldoAwal() != null
					&& Boolean.TRUE.equals(saldoAwalMasterAssetDetail.getSaldoAwal().getBreakdownAktif())) {
				return;
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:136");
		}

		if (pertangungjawabanKasBesar != null && pertangungjawabanKasBesar.getId() != null && pajakData != null) {
			try {
				Long key;
				if (pajakData.isNull("key")) {
					return;
				} else {
					key = ais.common.CommonJSONUtil.ambilLong(pajakData, "key");
				}

				JenisPajakBarang barang;
				if (!pajakData.isNull("pajak")) {
					barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(pajakData.get("pajak") + ""));
				} else {
					barang = null;
				}

				if (barang == null) {
					return;
				}

				String ntpn = "";

				if (!pajakData.isNull("ntpn")) {
					ntpn = pajakData.get("ntpn") + "";
				}

				String npwp = "";

				if (!pajakData.isNull("npwp")) {
					npwp = pajakData.get("npwp") + "";
				}

				String namaWp = "";

				if (!pajakData.isNull("namaWp")) {
					namaWp = pajakData.get("namaWp") + "";
				}

				String tanggalStor = "";

				if (!pajakData.isNull("tanggalStor")) {
					tanggalStor = pajakData.get("tanggalStor") + "";
				}
				Date tglStor = null;
				try {
					tglStor = tanggalStor.isEmpty() ? null : Common.dateFormat1.get().parse(tanggalStor);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:186");
					// TODO: handle exception
				}

				Session session = HibernateUtil.openSession();
				try {
				Pajak pajak = (Pajak) session.createCriteria(Pajak.class)
						.add(Restrictions.eq("pertangungjawabanKasBesar", pertangungjawabanKasBesar))
						.add(Restrictions.eq("keyData", key)).setMaxResults(1).uniqueResult();
				if (pajak == null) {
					pajak = new Pajak();
				}
				pajak.setKeyData(key);
				pajak.setPertangungjawabanKasBesar(pertangungjawabanKasBesar);
				pajak.setPajakData(pajakData.toString());
				pajak.setJenisPajakBarang(barang);
				pajak.setNpwp(npwp);
				pajak.setNtpn(ntpn);
				pajak.setNamaWp(namaWp);
				pajak.setTanggalStor(tglStor);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, pajak);
				session.getTransaction().commit();

				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}

				DaftarPengajuanTransfer.simpanPajak(pajak);
			} finally {
				// Tutup TUNTAS session yang DIBUKA (clear+disconnect+close), termasuk jalur exception.
				HibernateUtil.closeSessionQuietly(session);
			}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/Pajak.java:224");
			}

		}

		if (pertangungjawaban != null && pertangungjawaban.getId() != null && pajakData != null) {
			try {
				Long key;
				if (pajakData.isNull("key")) {
					return;
				} else {
					key = ais.common.CommonJSONUtil.ambilLong(pajakData, "key");
				}

				JenisPajakBarang barang;
				if (!pajakData.isNull("pajak")) {
					barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(pajakData.get("pajak") + ""));
				} else {
					barang = null;
				}

				if (barang == null) {
					return;
				}

				String ntpn = "";

				if (!pajakData.isNull("ntpn")) {
					ntpn = pajakData.get("ntpn") + "";
				}

				String npwp = "";

				if (!pajakData.isNull("npwp")) {
					npwp = pajakData.get("npwp") + "";
				}

				String namaWp = "";

				if (!pajakData.isNull("namaWp")) {
					namaWp = pajakData.get("namaWp") + "";
				}

				String tanggalStor = "";

				if (!pajakData.isNull("tanggalStor")) {
					tanggalStor = pajakData.get("tanggalStor") + "";
				}
				Date tglStor = null;
				try {
					tglStor = tanggalStor.isEmpty() ? null : Common.dateFormat1.get().parse(tanggalStor);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:276");
					// TODO: handle exception
				}

				Session session = HibernateUtil.openSession();
				try {
				Pajak pajak = (Pajak) session.createCriteria(Pajak.class)
						.add(Restrictions.eq("pertangungjawaban", pertangungjawaban))
						.add(Restrictions.eq("keyData", key)).setMaxResults(1).uniqueResult();
				if (pajak == null) {
					pajak = new Pajak();
				}
				pajak.setKeyData(key);
				pajak.setPertangungjawaban(pertangungjawaban);
				pajak.setPajakData(pajakData.toString());
				pajak.setJenisPajakBarang(barang);

				pajak.setNpwp(npwp);
				pajak.setNtpn(ntpn);
				pajak.setNamaWp(namaWp);
				pajak.setTanggalStor(tglStor);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, pajak);
				session.getTransaction().commit();
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}

				DaftarPengajuanTransfer.simpanPajak(pajak);
			} finally {
				// Tutup TUNTAS session yang DIBUKA (clear+disconnect+close), termasuk jalur exception.
				HibernateUtil.closeSessionQuietly(session);
			}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/Pajak.java:314");
			}
		}

		if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getId() != null) {
			Session session = null;
			org.hibernate.Transaction tx = null;
			try {
				session = HibernateUtil.openSession();
				SaldoAwalMasterAssetDetail detailData = (SaldoAwalMasterAssetDetail) session
						.get(SaldoAwalMasterAssetDetail.class, saldoAwalMasterAssetDetail.getId());
				if (detailData == null) {
					return;
				}

				Pajak pajak = (Pajak) session.createCriteria(Pajak.class)
						.add(Restrictions.eq("saldoAwalMasterAssetDetail", detailData)).setMaxResults(1)
						.uniqueResult();

				tx = session.beginTransaction();
				if (pajak == null && detailData.getJenisPajakBarang() != null) {
					pajak = new Pajak();
					pajak.setSaldoAwalMasterAssetDetail(detailData);
					Common.refreshSaveOrUpdate(session, pajak);
				} else if (pajak != null && detailData.getJenisPajakBarang() == null) {
					Common.refreshDelete(session, pajak);
					pajak = null;
				}
				tx.commit();

				if (pajak != null) {
					DaftarPengajuanTransfer.simpanPajak(pajak);
				}
			} catch (Exception e) {
				try {
					if (tx != null && tx.isActive()) {
						tx.rollback();
					}
				} catch (Exception rollbackException) { ais.common.ErrorAuditUtil.record(rollbackException, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:352");
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/Pajak.java:354");
			} finally {
				if (session != null) {
					try {
						session.close();
					} catch (Exception closeException) { ais.common.ErrorAuditUtil.record(closeException, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:359");
					}
				}
				HibernateUtil.closeSession();
			}
		}

	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Tidak menyetel apa pun; seluruh
	 * pengisian dilakukan oleh pabrik statis di class ini atau oleh pemanggil di lapisan API/POS.
	 */
	public Pajak() {
	}

	/**
	 * Mengembalikan kunci utama baris ini (kolom {@code id}). Getter polos.
	 *
	 * <p>Nilai di-<i>generate</i> database ({@code IDENTITY}) dan kolomnya {@code insertable = false},
	 * jadi menyetelnya sendiri sebelum {@code save} tidak berpengaruh pada INSERT.</p>
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
	 * Menyetel kunci utama. Setter polos; dipakai Hibernate saat memuat baris.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode dokumen pajak — <b>getter turunan yang menulis balik</b>.
	 *
	 * <p>Kode diambil dari dokumen sumber dengan urutan prioritas: LPJ uang muka → LPJ kas besar →
	 * tagihan induk dari detail tagihan → tagihan langsung (mode Bukti Potong). Untuk baris PPh
	 * termin dan baris setoran POS keempat relasi itu kosong, sehingga nilai yang tersimpan
	 * dikembalikan apa adanya (untuk termin, kode induk hasil penelusuran di
	 * {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)}).</p>
	 *
	 * <p><b>Efek samping:</b> hasil resolusi ditulis ke field {@code kode}, dan karena {@code kode}
	 * adalah properti terpetakan, nilainya ikut ter-<i>flush</i> ke database. Mengubah kode dokumen
	 * sumber karena itu akan "merambat" ke baris pajak lama begitu baris itu dibaca lagi.</p>
	 *
	 * <p>Nilai dikembalikan dalam keadaan sudah di-{@code trim}, dan tidak pernah {@code null} —
	 * kode kosong menjadi string kosong. Dipakai antara lain sebagai judul baris di layar Data Pajak,
	 * pada label "kode pengajuan" di Daftar Pengajuan Transfer, dan sebagai bagian keterangan jurnal
	 * di {@code PostingPertangungjawabanPajakAction}.</p>
	 *
	 * @return kode dokumen (sudah di-trim), atau string kosong bila tidak ada
	 */
	public String getKode() {
		if (pertangungjawaban != null) {
			kode = pertangungjawaban.getKode();
		} else if (pertangungjawabanKasBesar != null) {
			kode = pertangungjawabanKasBesar.getKode();
		} else if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getSaldoAwal() != null) {
			kode = saldoAwalMasterAssetDetail.getSaldoAwal().getKode();
		} else if (saldoAwal != null) {
			kode = saldoAwal.getKode();
		}
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode dokumen secara manual.
	 *
	 * <p><b>Akan ditimpa</b> oleh {@link #getKode()} pada pembacaan berikutnya bila baris ini punya
	 * relasi ke dokumen sumber. Setter ini karena itu hanya efektif untuk baris tanpa relasi: PPh
	 * termin (diisi {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)} dengan kode induk
	 * pengadaan) dan setoran POS (diisi {@code PengadaanPosApiHelper.pajakSetor} dengan kode
	 * bernomor {@code PJK}).</p>
	 *
	 * @param kode kode dokumen; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul dokumen pajak — <b>getter turunan yang menulis balik</b>.
	 *
	 * <p>Nama diambil dari dokumen sumber dengan urutan: LPJ uang muka → LPJ kas besar → nama master
	 * aset milik detail tagihan. Baris PPh termin memakai nama tetap {@code "Tagihan Termin"} dan
	 * baris setoran POS memakai {@code "Setoran PPH/PPN pengadaan"}, keduanya diset eksplisit karena
	 * tidak punya relasi.</p>
	 *
	 * <p><b>Dua jebakan pada method ini:</b></p>
	 * <ol>
	 *   <li>Cabang detail tagihan memanggil {@code saldoAwalMasterAssetDetail.getMasterAsset().getNama()}
	 *   <b>tanpa penjaga null</b> — satu-satunya cabang di class ini yang tidak memeriksa relasi
	 *   antaranya. Bila master aset kosong, hasilnya NPE, yang pada jalur
	 *   {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)}
	 *   ditelan {@code catch} sehingga baris pajak gagal terbit tanpa pesan.</li>
	 *   <li>Kolomnya {@code nullable = false}. Baris tanpa relasi <i>dan</i> tanpa nama eksplisit
	 *   karena itu akan ditolak database saat INSERT.</li>
	 * </ol>
	 *
	 * @return judul dokumen (sudah di-trim), atau {@code null} bila field masih kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (pertangungjawaban != null) {
			nama = pertangungjawaban.getNama();
		} else if (pertangungjawabanKasBesar != null) {
			nama = pertangungjawabanKasBesar.getNama();
		} else if (saldoAwalMasterAssetDetail != null) {
			nama = saldoAwalMasterAssetDetail.getMasterAsset().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul dokumen. <b>Akan ditimpa</b> oleh {@link #getNama()} pada pembacaan berikutnya
	 * bila baris ini punya relasi ke dokumen sumber; efektif hanya untuk baris termin dan setoran POS.
	 *
	 * @param nama judul dokumen; boleh {@code null}, tetapi kolomnya {@code NOT NULL}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas — <b>getter turunan yang menulis balik</b>, dengan urutan
	 * prioritas LPJ uang muka → LPJ kas besar → detail tagihan pengadaan.
	 *
	 * <p>Berbeda dari {@link #getNama()} dan {@link #getKode()}, nilai dikembalikan apa adanya tanpa
	 * normalisasi, sehingga bisa {@code null}. Ditampilkan di kolom "Keterangan" layar Data Pajak
	 * (dilewatkan {@code Common.simpleString} di sana) dan ikut ke pesan pada layar POS.</p>
	 *
	 * @return keterangan dokumen, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (pertangungjawaban != null) {
			keterangan = pertangungjawaban.getKeterangan();
		} else if (pertangungjawabanKasBesar != null) {
			keterangan = pertangungjawabanKasBesar.getKeterangan();
		} else if (saldoAwalMasterAssetDetail != null) {
			keterangan = saldoAwalMasterAssetDetail.getKeterangan();
		}
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan. <b>Akan ditimpa</b> oleh {@link #getKeterangan()} pada pembacaan
	 * berikutnya bila baris ini punya relasi ke dokumen sumber; efektif hanya untuk baris termin
	 * dan setoran POS (di POS diisi dari field {@code keterangan} permintaan API).
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan bendera {@code aktif} dengan {@code null} dinormalkan menjadi {@code false}.
	 *
	 * <p><b>Kolom ini menanggung DUA makna yang bertabrakan</b> (lihat juga Javadoc class):</p>
	 * <ul>
	 *   <li><b>"Sudah dibayar"</b> — di layar Data Pajak
	 *   ({@code pertangungjawaban_pajak.zul}) kolom ini dirender sebagai <i>checkbox</i> berlabel
	 *   <b>"Bayar"</b>, berpasangan dengan {@link #getTanggalTransaksi()} berlabel "Tanggal Bayar";
	 *   mencentangnya langsung menyimpan baris dan mengisi tanggal hari ini.</li>
	 *   <li><b>Saklar hidup/mati baris</b> — {@code BreakdownTagihanVendorHelper.sinkronPajakBreakdown}
	 *   memanggil {@code setAktif(!breakdown)} untuk memadamkan baris PPh per item saat tagihan
	 *   vendor beralih ke mode Bukti Potong, dan menyalakannya lagi saat kembali ke mode PO. Efek
	 *   yang mudah menjebak: menekan saklar breakdown <b>mengubah status "Bayar"</b> sejumlah baris
	 *   pajak tanpa ada peristiwa pembayaran.</li>
	 * </ul>
	 *
	 * <p>Nilai ini juga menjadi <b>gerbang posting jurnal</b>: kedua layar pajak menyaring
	 * {@code or(aktif is null, aktif = true)}, sehingga baris dengan {@code aktif = false} tidak
	 * pernah muncul untuk diposting. {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)}
	 * menyalakannya saat pembuatan — baris PPh termin karena itu lahir dalam keadaan sudah
	 * tercentang "Bayar" — sedangkan
	 * {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)}
	 * tidak pernah menyentuhnya (tetap {@code NULL}, yang oleh filter SQL diperlakukan sebagai
	 * aktif).</p>
	 *
	 * <p><b>Kuirk filter di layar Data Pajak:</b> kotak centang "Tampilkan hanya yang aktif" dalam
	 * keadaan tercentang (bawaan) justru memasang {@code sqlRestriction("true")} — yaitu menampilkan
	 * <i>semua</i> baris, bukan hanya yang aktif; melepas centangnya menampilkan {@code aktif = false}
	 * saja, sehingga baris ber-{@code NULL} (semua baris jalur LPJ dan tagihan) ikut hilang. Kotak
	 * "Tidakaktif" di sebelahnya tidak pernah mengubah hasil apa pun.</p>
	 *
	 * @return {@code true} bila baris ditandai dibayar/aktif; {@code false} bila {@code NULL} atau
	 *         memang {@code false}
	 */
	public Boolean getAktif() {

		return aktif == null ? false : aktif;
	}

	/**
	 * Menyetel bendera {@code aktif}/"Bayar". Setter polos — perhatikan makna gandanya pada
	 * {@link #getAktif()} sebelum memakainya.
	 *
	 * <p>Perlu diketahui: menyimpan {@code null} tidak sama dengan menyimpan {@code false}. Filter
	 * SQL di kedua layar pajak memperlakukan {@code NULL} sebagai <i>aktif</i>, sementara
	 * {@link #getAktif()} di Java mengembalikan {@code false} untuk {@code NULL}. Jadi baris
	 * ber-{@code NULL} tampil sebagai "belum dibayar" di layar namun tetap ikut terjaring posting
	 * jurnal.</p>
	 *
	 * @param aktif bendera baru; {@code null} berarti "belum pernah ditentukan"
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan master tarif PPh yang berlaku untuk baris ini — <b>getter turunan yang menulis
	 * balik</b>, dan sekaligus properti paling berpengaruh di class ini.
	 *
	 * <p>Alurnya: proxy lazy diresolusi lebih dulu lewat {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject}; lalu, bila baris tertaut ke satu item tagihan
	 * pengadaan, jenis PPh <b>selalu diambil ulang dari item itu</b> ({@code detail.getJenisPajakBarang()})
	 * dan menimpa nilai tersimpan. Untuk jalur lain (LPJ, termin, breakdown, setoran POS) nilai
	 * tersimpan dipakai apa adanya.</p>
	 *
	 * <p><b>Mengapa penting:</b> object ini memasok tiga hal sekaligus di hilir —
	 * (1) <i>tarif</i> {@code getPersen()} yang dipakai {@link #getJumlah()} dan, lewat
	 * {@code SaldoAwalMasterAssetDetail.getPersenPph()}, ikut menentukan {@link #getNilai()};
	 * (2) <i>akun jurnal debet</i> {@code getAkun()} pada
	 * {@code PostingPertangungjawabanPajakAction} — dan salah satu kandidat akun kredit lewat
	 * {@code getAkunDanaTitipan()}; (3) <i>rekening tujuan setoran</i> (bank, atas nama, nomor
	 * rekening) yang dibaca {@link DaftarPengajuanTransfer} untuk baris pembayaran pajak. Mengubah
	 * master tarif karena itu berdampak retroaktif pada nominal, jurnal, dan rekening tujuan baris
	 * pajak lama.</p>
	 *
	 * <p><b>Konsekuensi bila {@code null}:</b> mesin posting jurnal mengambil akun debet dengan
	 * {@code pajak.getJenisPajakBarang().getAkun()} <b>tanpa penjaga null</b>, sehingga baris tanpa
	 * jenis PPh — misalnya setoran <b>PPN</b> yang dibuat layar POS, yang hanya mengisi
	 * {@link #getJenisPajakPpn()} — melempar NPE yang ditelan {@code catch} per baris dan
	 * <b>dilewati diam-diam</b> dari posting. Di layar Data Pajak, baris ber-{@code null} yang
	 * <i>juga</i> punya relasi detail tagihan malah <b>dihapus permanen dari jalur render</b>
	 * ({@code PajakRenderer} memanggil {@code Common.refreshDelete} lalu menyembunyikan barisnya).</p>
	 *
	 * @return master tarif PPh, atau {@code null} untuk baris PPN/tanpa pajak
	 */
	// nullable=true: baris Pajak mode BREAKDOWN (Bukti Potong) tak terikat satu jenis PPh PO.
	// Semua jalur kritis (getNominal/simpanPajak/getKode) sudah guard jenisPajakBarang null.
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak_barang", nullable = true)
	public JenisPajakBarang getJenisPajakBarang() {
		jenisPajakBarang = check(jenisPajakBarang);
		if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getJenisPajakBarang() != null) {
			jenisPajakBarang = saldoAwalMasterAssetDetail.getJenisPajakBarang();
		}
		return jenisPajakBarang;
	}

	/**
	 * Menyetel master tarif PPh. Setter polos, tetapi <b>akan ditimpa</b> oleh
	 * {@link #getJenisPajakBarang()} pada pembacaan berikutnya bila baris ini tertaut ke item
	 * tagihan pengadaan — pada jalur itu, jenis PPh yang otoritatif adalah milik item tagihannya.
	 *
	 * <p>Dipakai eksplisit oleh keempat pabrik: jalur LPJ (dari field {@code pajak} pada JSON item),
	 * {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)} (dari formula PO),
	 * {@code BreakdownTagihanVendorHelper} (dari {@code SaldoAwalMasterAsset.getBreakdownJenisPph()}),
	 * dan {@code PengadaanPosApiHelper.pajakSetor} (khusus setoran PPH).</p>
	 *
	 * @param jenisPajakBarang master tarif PPh; boleh {@code null}
	 */
	public void setJenisPajakBarang(JenisPajakBarang jenisPajakBarang) {
		this.jenisPajakBarang = jenisPajakBarang;
	}

	/**
	 * Mengembalikan unit kerja (RAB) pemilik baris ini — <b>getter turunan yang menulis balik</b>.
	 *
	 * <p>Resolusinya: LPJ uang muka → LPJ kas besar; lalu, secara <b>terpisah</b>, tagihan induk dari
	 * detail tagihan pengadaan bila ada, dan kalau tidak ada barulah nilai tersimpan diresolusi
	 * proxy-nya lewat {@code check(...)}. Struktur dua tahap itu berarti unit kerja dari tagihan
	 * pengadaan <b>menang</b> atas apa pun yang sudah diturunkan dari LPJ di tahap pertama. Baris PPh
	 * termin memperoleh unit kerjanya sekali saja saat dibuat (dari PO induk); baris setoran POS
	 * tidak pernah memilikinya.</p>
	 *
	 * <p><b>Ini satu-satunya pembatas cakupan data pada entity ini.</b> {@code Pajak} tidak punya
	 * kolom {@code sekolah} maupun {@code yayasan}, jadi tidak ada pemisahan tenant di level baris.
	 * Penyaring di kedua layar pajak berbentuk
	 * {@code or(satuanKerja is null, ...)}, sehingga <b>baris tanpa unit kerja selalu terlihat oleh
	 * semua pengguna</b>; dan bila {@code SekolahUtil.ambilSatuanKerjas()} mengembalikan himpunan
	 * kosong, penyaringnya jatuh ke {@code sqlRestriction("1=1")} — seluruh baris pajak di instalasi
	 * ikut tampil. Pada layar posting, cabang yang dipakai ketika halaman dibuka dari dasbor Draft
	 * Jurnal (parameter URL {@code sudah_posting}) bahkan <b>tidak memasang penyaring unit kerja
	 * sama sekali</b>. Dasbor pajak ({@code DasboardPajak}) juga membaca seluruh baris tanpa
	 * penyaring unit kerja dan menyimpan hasilnya di cache berkunci tetap {@code "ADMIN"} — satu
	 * salinan yang dipakai bersama semua pengguna.</p>
	 *
	 * @return unit kerja pemilik, atau {@code null} bila dokumen sumber tidak menentukannya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (pertangungjawaban != null && pertangungjawaban.getSatuanKerja() != null) {
			satuanKerja = pertangungjawaban.getSatuanKerja();
		} else if (pertangungjawabanKasBesar != null && pertangungjawabanKasBesar.getSatuanKerja() != null) {
			satuanKerja = pertangungjawabanKasBesar.getSatuanKerja();
		}
		if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getSaldoAwal() != null
				&& saldoAwalMasterAssetDetail.getSaldoAwal().getSatuanKerja() != null) {
			satuanKerja = saldoAwalMasterAssetDetail.getSaldoAwal().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja. <b>Akan ditimpa</b> oleh {@link #getSatuanKerja()} pada pembacaan
	 * berikutnya bila baris ini punya dokumen sumber yang menentukan unit kerja.
	 *
	 * @param satuanKerja unit kerja; boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan <b>nominal pajak dalam rupiah</b> — method paling berpengaruh secara finansial
	 * di class ini, dan sekaligus <b>getter yang menghitung ulang lalu menulis balik</b>.
	 *
	 * <p>Nilainya ditentukan menurut jalur asal baris:</p>
	 * <ol>
	 *   <li><b>Item tagihan pengadaan</b> — dihitung ulang setiap kali dibaca sebagai
	 *   <code>persenPph / 100 x {@link #getDpp()}</code>, dengan {@code persenPph} diambil dari
	 *   {@code SaldoAwalMasterAssetDetail.getPersenPph()} yang sendirinya menurunkan angkanya dari
	 *   {@code JenisPajakBarang.getPersen()} yang berlaku <i>sekarang</i>.</li>
	 *   <li><b>LPJ uang muka / LPJ kas besar</b> — dibaca ulang dari field {@code pajak_nilai} pada
	 *   salinan JSON {@link #getPajakData()}. Bila JSON rusak atau tidak memuat field itu,
	 *   exception-nya dicetak ke {@code stderr} + {@code ErrorAuditUtil} dan nilai <i>lama</i>
	 *   dipertahankan (fail-quiet).</li>
	 *   <li><b>PPh termin, Bukti Potong (breakdown), setoran POS</b> — tidak ada cabang yang cocok,
	 *   sehingga nilai tersimpan dikembalikan apa adanya. Untuk Bukti Potong angka itu sengaja
	 *   <i>bukan</i> hasil tarif x DPP, melainkan nominal yang diketik petugas.</li>
	 * </ol>
	 *
	 * <p><b>Pembulatan yang asimetris — perhatikan bila mencocokkan angka.</b> Yang dikembalikan
	 * adalah {@code Math.round(...)} (bilangan bulat), tetapi yang <i>ditulis balik ke kolom</i>
	 * {@code nilai} adalah angka <b>sebelum</b> dibulatkan. Jadi kolom database dan angka yang
	 * dipakai layar/jurnal bisa berbeda hingga setengah rupiah, dan penyaring SQL yang bekerja pada
	 * kolom ({@code nilai &gt; 0.01}, {@code nilai &lt;&gt; 0.0}) menilai angka yang berbeda dari yang
	 * dilihat pengguna. {@code null} diperlakukan sebagai {@code 0}, sehingga method ini tidak pernah
	 * mengembalikan {@code null}.</p>
	 *
	 * <p><b>Dampak retroaktif (integritas kalkulasi pajak).</b> Karena jalur (1) dan (2) menghitung
	 * ulang dari sumber yang bisa berubah, mengubah tarif pada master {@code JenisPajakBarang}, atau
	 * mengubah jumlah/harga item tagihan, akan mengubah nominal pajak <b>baris lama</b> pada
	 * pembacaan berikutnya — termasuk baris yang jurnalnya sudah terbentuk. Jurnal di
	 * {@code akunting.transaksi} tidak ikut berubah (nominalnya sudah ter-<i>snapshot</i>), sehingga
	 * layar pajak dan buku besar bisa berselisih tanpa jejak; dan bila posting dibatalkan lalu
	 * diulang, jurnal barunya memakai tarif baru untuk transaksi lama.</p>
	 *
	 * <p><b>Selisih DPP yang perlu diketahui.</b> Untuk jalur (1), {@link #getDpp()} di class ini
	 * memakai <code>jumlah x harga</code> <b>tanpa</b> mengurangi potongan harga, sedangkan
	 * {@code SaldoAwalMasterAssetDetail.hitungPph()} dan {@code getHargaTotal()} memakai
	 * <code>(jumlah x harga) - potongan</code>. Untuk item tagihan yang berdiskon, nominal PPh di
	 * baris pajak ini karena itu <b>lebih besar</b> daripada PPh yang dipotong dari pembayaran
	 * vendor, sehingga invarian <code>netto(vendor) + Σ(baris pajak) = bruto</code> yang dinyatakan
	 * {@link DaftarPengajuanTransfer} tidak lagi terpenuhi. Selisihnya ikut terbawa ke jurnal karena
	 * {@code PostingPertangungjawabanPajakAction} mendebet dan mengkredit angka dari method ini.</p>
	 *
	 * <p><b>Siapa yang membacanya:</b> renderer layar Data Pajak dan layar posting, keterangan +
	 * nominal jurnal di {@code PostingPertangungjawabanPajakAction}, {@code DasboardPajak},
	 * {@code DaftarPengajuanTransfer.getNominal()} dan
	 * {@code DaftarPengajuanTransfer.hitungTotalPphSaldoAwal(...)}, serta cetak Bukti Setor Pajak di
	 * {@code PengadaanPosApiHelper}.</p>
	 *
	 * @return nominal pajak dalam rupiah, sudah dibulatkan ke bilangan bulat; {@code 0} bila kosong
	 */
	public Double getNilai() {
		if (saldoAwalMasterAssetDetail != null) {
			Double dpp = getDpp();
			Double pph = ((saldoAwalMasterAssetDetail.getPersenPph() / 100.0) * dpp);
			nilai = pph;
		} else if (pertangungjawaban != null && pajakData != null) {
			try {
				nilai = new JSONObject(pajakData).getDouble("pajak_nilai");
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/Pajak.java:486");
			}
		} else if (pertangungjawabanKasBesar != null && pajakData != null) {
			try {
				nilai = new JSONObject(pajakData).getDouble("pajak_nilai");
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/Pajak.java:492");
			}
		}

		return (double) Math.round(nilai == null ? 0.0 : nilai);
	}

	/**
	 * Menyetel nominal pajak.
	 *
	 * <p><b>Efektif hanya untuk baris tanpa relasi</b>: PPh termin
	 * ({@link #buatDariTermin(PembayaranTerminMasterAssetDetail)}), Bukti Potong breakdown
	 * ({@code BreakdownTagihanVendorHelper}), dan setoran POS ({@code PengadaanPosApiHelper.pajakSetor}).
	 * Untuk baris LPJ dan baris item tagihan, {@link #getNilai()} akan menghitung ulang dan menimpa
	 * nilai yang diset di sini pada pembacaan berikutnya.</p>
	 *
	 * <p>Tidak ada validasi: nilai negatif maupun {@code null} diterima apa adanya.</p>
	 *
	 * @param nilai nominal pajak dalam rupiah; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan tanggal dokumen pajak — <b>getter turunan yang menulis balik</b>.
	 *
	 * <p>Prioritasnya: tanggal pembuatan tagihan induk (untuk baris item tagihan) → tanggal
	 * pembuatan LPJ uang muka → tanggal pembuatan LPJ kas besar. Untuk baris tanpa relasi (PPh
	 * termin, Bukti Potong, setoran POS) nilai tersimpan dipakai.</p>
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}</b>: bila kosong, dikembalikan
	 * <i>waktu sekarang</i>. Perlu diperhatikan bahwa nilai pengganti itu <b>ikut ditulis
	 * balik</b> hanya jika cabang di atas menyalakannya — pada kasus kosong murni, {@code new Date()}
	 * dikembalikan tanpa disimpan, sehingga dua pembacaan berturut-turut bisa menghasilkan tanggal
	 * yang berbeda.</p>
	 *
	 * <p>Kolom inilah yang dipakai penyaring rentang tanggal di layar Data Pajak
	 * ({@code date(this_.tanggal) between ...}) dan yang dipakai {@link DaftarPengajuanTransfer}
	 * sebagai {@code waktu} baris pembayaran pajak — berbeda dari layar posting, yang menyaring
	 * memakai {@link #getTanggalTransaksi()}.</p>
	 *
	 * @return tanggal dokumen; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getSaldoAwal() != null) {
			tanggal = saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPembuatan();
		} else if (pertangungjawaban != null) {
			tanggal = pertangungjawaban.getTanggalPembuatan();
		} else if (pertangungjawabanKasBesar != null) {
			tanggal = pertangungjawabanKasBesar.getTanggalPembuatan();
		}
		return tanggal == null ? new Date() : tanggal;
	}

	/**
	 * Menyetel tanggal dokumen. <b>Akan ditimpa</b> oleh {@link #getTanggal()} pada pembacaan
	 * berikutnya bila baris ini punya dokumen sumber.
	 *
	 * <p>Dipakai eksplisit oleh {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)} (tanggal
	 * termin dibayar), {@code BreakdownTagihanVendorHelper} (waktu server saat baris Bukti Potong
	 * dibuat), dan {@code PengadaanPosApiHelper.pajakSetor} (tanggal setor).</p>
	 *
	 * @param tanggal tanggal dokumen; boleh {@code null} ({@link #getTanggal()} akan menggantinya
	 *                dengan waktu sekarang saat dibaca)
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan cap posting jurnal baris ini. Getter relasi polos (tanpa turunan).
	 *
	 * <p>Inilah kolom yang membuat entity ini berperan sebagai <b>kaki pajak</b> mesin posting:
	 * selama {@code null}, baris dianggap belum dijurnal dan muncul di layar "Draft Jurnal → Pajak";
	 * setelah diposting, {@code PostingPertangungjawabanPajakAction} mengisinya dengan
	 * {@link PostingHistory} hasil posting massal. Pembatalan posting mengosongkannya lagi dan
	 * menghapus baris {@code akunting.grup_transaksi} yang ber-{@code ref = 'pajak'} dan belum
	 * di-<i>closing</i>.</p>
	 *
	 * <p>Perlu dibedakan dari {@code Pertangungjawaban.getPostingHistoryPajak()} — kolom bernama
	 * mirip pada dokumen LPJ yang <b>tidak pernah diisi</b> oleh kode mana pun (lihat Javadoc
	 * class).</p>
	 *
	 * @return cap posting jurnal, atau {@code null} bila belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menautkan/melepas cap posting jurnal. Setter polos.
	 *
	 * <p>Hanya dipanggil oleh {@code PostingPertangungjawabanPajakAction}: diisi saat posting massal
	 * berhasil, dan dikosongkan ({@code null}) saat posting dibatalkan.</p>
	 *
	 * @param postingHistory cap posting; {@code null} berarti "batalkan posting"
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan tanggal pembayaran setoran (kolom {@code tanggal_transaksi}); di layar Data Pajak
	 * berlabel <b>"Tanggal Bayar"</b>. Getter polos.
	 *
	 * <p>Diisi otomatis dengan tanggal hari ini ketika petugas mencentang kotak "Bayar"
	 * ({@link #getAktif()}) di renderer, dan dikosongkan lagi ketika centangnya dilepas. Jalur
	 * termin dan LPJ tidak mengisinya, sedangkan {@code PengadaanPosApiHelper.pajakSetor} mengisinya
	 * sama dengan tanggal setor.</p>
	 *
	 * <p><b>Penting untuk posting:</b> layar posting menyaring rentang tanggal memakai kolom
	 * <i>ini</i> ({@code date(this_.tanggal_transaksi) between ...}), bukan {@link #getTanggal()}.
	 * Baris yang belum pernah dicentang "Bayar" karena itu ber-{@code NULL} dan <b>tidak akan
	 * terjaring</b> oleh posting bertanggal — kewajiban pajaknya tidak pernah masuk buku besar
	 * sampai seseorang mencentangnya.</p>
	 *
	 * @return tanggal bayar, atau {@code null} bila belum ditandai dibayar
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		return tanggalTransaksi;
	}

	/**
	 * Menyetel tanggal pembayaran setoran. Setter polos, tanpa validasi (tanggal masa depan atau
	 * tanggal di periode yang sudah di-<i>closing</i> diterima apa adanya).
	 *
	 * @param tanggalTransaksi tanggal bayar; {@code null} berarti belum dibayar
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan LPJ uang muka yang menjadi dokumen sumber baris ini. Getter relasi polos.
	 *
	 * <p>Terisi hanya untuk jalur {@code buat(lpj, null, itemJson, null)}. Selain menandai asal
	 * baris, relasi ini dipakai luas oleh getter turunan lain ({@link #getKode()},
	 * {@link #getNama()}, {@link #getKeterangan()}, {@link #getSatuanKerja()},
	 * {@link #getTanggal()}, {@link #getNilai()}, {@link #getDpp()}) dan oleh mesin posting untuk
	 * menentukan salah satu kandidat akun kredit lewat
	 * {@code getUangMuka().getJenisUangMuka().getAkun()}. Renderer layar Data Pajak juga
	 * menampilkan alur SOP dan status transfer milik LPJ ini.</p>
	 *
	 * @return LPJ uang muka sumber, atau {@code null} untuk jalur lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban", nullable = true)
	public Pertangungjawaban getPertangungjawaban() {
		return pertangungjawaban;
	}

	/**
	 * Menautkan baris ini ke sebuah LPJ uang muka. Setter polos; dipanggil oleh
	 * {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)}.
	 *
	 * <p>Perlu diingat bahwa mengisi relasi ini mengaktifkan sederet getter turunan, sehingga
	 * nilai-nilai yang sudah diset manual (kode, nama, keterangan, nominal, DPP, tanggal) akan
	 * ditimpa dari LPJ pada pembacaan berikutnya.</p>
	 *
	 * @param pertangungjawaban LPJ uang muka sumber; boleh {@code null}
	 */
	public void setPertangungjawaban(Pertangungjawaban pertangungjawaban) {
		this.pertangungjawaban = pertangungjawaban;
	}

	/**
	 * Mengembalikan satu item tagihan pengadaan aset yang menjadi dokumen sumber baris ini (mode
	 * PPh <b>per item</b>). Getter relasi polos.
	 *
	 * <p>Relasi ini yang paling banyak mengendalikan perilaku turunan: ia menentukan jenis PPh
	 * ({@link #getJenisPajakBarang()}), DPP ({@link #getDpp()}), nominal ({@link #getNilai()}),
	 * NPWP/nama wajib pajak dari penyedia, unit kerja, dan tanggal. Ia juga menjadi kunci dua
	 * perilaku "pembersihan" yang destruktif: {@code PajakRenderer} menghapus permanen baris yang
	 * detailnya sudah tidak punya jenis PPh, dan
	 * {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)}
	 * melakukan hal yang sama saat detailnya diubah menjadi tanpa pajak.</p>
	 *
	 * <p>Saat tagihan induknya beralih ke mode <i>breakdown</i>, baris-baris ber-relasi ini
	 * dipadamkan ({@code aktif = false}) dan disaring keluar dari kedua layar pajak lewat
	 * {@code LEFT JOIN} ke {@code breakdownAktif}, digantikan satu baris {@link #getSaldoAwal()}.</p>
	 *
	 * @return item tagihan pengadaan sumber, atau {@code null} untuk jalur lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_master_asset_detail", nullable = true)
	public SaldoAwalMasterAssetDetail getSaldoAwalMasterAssetDetail() {
		return saldoAwalMasterAssetDetail;
	}

	/**
	 * Menautkan baris ini ke satu item tagihan pengadaan aset. Setter polos.
	 *
	 * <p>Mengisi relasi ini membuat hampir seluruh atribut baris menjadi turunan — termasuk nominal
	 * pajak, yang sejak saat itu dihitung ulang dari tarif dan harga item setiap kali dibaca.</p>
	 *
	 * @param saldoAwalMasterAssetDetail item tagihan sumber; boleh {@code null}
	 */
	public void setSaldoAwalMasterAssetDetail(SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail) {
		this.saldoAwalMasterAssetDetail = saldoAwalMasterAssetDetail;
	}

	/**
	 * Mengembalikan tagihan pengadaan aset yang ditunjuk <b>langsung</b> (tanpa lewat item) — penanda
	 * baris <b>PPh mode BREAKDOWN / "Bukti Potong"</b>. Getter relasi polos.
	 *
	 * <p>Baris semacam ini dibuat {@code BreakdownTagihanVendorHelper.sinkronPajakBreakdown} ketika
	 * pengguna menyalakan mode breakdown pada sebuah tagihan vendor. Yang membedakannya dari jalur
	 * per item: <b>nominalnya tidak dihitung dari tarif</b>, melainkan disalin dari angka Bukti
	 * Potong yang diketik petugas ({@code SaldoAwalMasterAsset.getBreakdownBuktiPotong()}), sementara
	 * jenis PPh tetap diisi semata-mata untuk menentukan akun jurnal dan rekening setoran.</p>
	 *
	 * <p>Selama mode breakdown menyala, baris per item pada tagihan yang sama dipadamkan; saat
	 * dimatikan, baris Bukti Potong ini yang dipadamkan dan baris per item dihidupkan kembali.</p>
	 *
	 * @return tagihan pengadaan pemilik Bukti Potong, atau {@code null} untuk jalur lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal", nullable = true)
	public ais.database.model.asset.SaldoAwalMasterAsset getSaldoAwal() {
		return saldoAwal;
	}

	/**
	 * Menautkan baris ini langsung ke sebuah tagihan pengadaan (menjadikannya baris Bukti Potong).
	 * Setter polos; hanya dipanggil {@code BreakdownTagihanVendorHelper}.
	 *
	 * @param saldoAwal tagihan pengadaan; boleh {@code null}
	 */
	public void setSaldoAwal(ais.database.model.asset.SaldoAwalMasterAsset saldoAwal) {
		this.saldoAwal = saldoAwal;
	}

	/**
	 * Mengembalikan salinan mentah item biaya LPJ dalam bentuk teks JSON (kolom bertipe
	 * {@code text}). Getter polos.
	 *
	 * <p>Ini <b>bukan sekadar arsip</b>: untuk baris jalur LPJ, isi JSON inilah yang dibaca ulang
	 * oleh {@link #getNilai()} (field {@code pajak_nilai}) dan {@link #getDpp()} (field
	 * {@code jumlah}) setiap kali dibaca. Salinannya dibuat sekali saat
	 * {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)}
	 * dijalankan (yaitu saat LPJ disetujui) dan tidak pernah disegarkan sesudahnya — bila
	 * {@code formula} LPJ kemudian disunting, baris pajak tetap memakai angka lama.</p>
	 *
	 * @return teks JSON item biaya, atau {@code null} untuk baris non-LPJ
	 */
	@Column(columnDefinition = "text")
	public String getPajakData() {
		return pajakData;
	}

	/**
	 * Menyimpan salinan mentah item biaya LPJ. Setter polos, tanpa validasi bentuk JSON — teks yang
	 * tidak valid baru ketahuan saat {@link #getNilai()}/{@link #getDpp()} gagal mem-parsing-nya,
	 * dan kegagalan itu hanya dicatat, tidak dilaporkan ke pengguna.
	 *
	 * @param pajakData teks JSON item biaya; boleh {@code null}
	 */
	public void setPajakData(String pajakData) {
		this.pajakData = pajakData;
	}

	/**
	 * Mengembalikan penanda item di dalam dokumen sumber. Getter polos, tetapi <b>maknanya berbeda
	 * menurut jalur</b>:
	 * <ul>
	 *   <li><b>Jalur LPJ</b> — nilai field {@code key} pada item {@code formula}; bersama relasi LPJ
	 *   membentuk kunci idempotensi baris pajak.</li>
	 *   <li><b>Jalur PPh termin</b> — <b>id {@code PembayaranTerminMasterAssetDetail}</b>. Kolom
	 *   khusus sengaja tidak ditambahkan agar tabel ber-{@code @Audited} ini tidak perlu di-{@code ALTER};
	 *   yang membedakannya dari jalur LPJ adalah syarat bahwa keempat relasi dokumen sumber
	 *   {@code NULL}. Pola pengenalan yang sama diulang di
	 *   {@code DaftarPengajuanTransferAction}, {@code DaftarPengajuanTransferSearchHelper}, dan
	 *   {@code ProsesTransferAction} untuk menemukan detail termin dari sebuah baris pajak.</li>
	 * </ul>
	 *
	 * @return penanda item, atau {@code null} untuk baris Bukti Potong / setoran POS
	 */
	public Long getKeyData() {
		return keyData;
	}

	/**
	 * Menyetel penanda item di dalam dokumen sumber. Setter polos — perhatikan makna gandanya pada
	 * {@link #getKeyData()}: mengisinya pada baris yang juga punya relasi dokumen sumber akan
	 * membuat baris itu tidak lagi dikenali sebagai baris termin, dan sebaliknya.
	 *
	 * @param keyData kunci item JSON LPJ, atau id detail termin
	 */
	public void setKeyData(Long keyData) {
		this.keyData = keyData;
	}

	/**
	 * Mengembalikan LPJ kas besar yang menjadi dokumen sumber baris ini. Getter relasi polos.
	 *
	 * <p>Sepenuhnya sejajar dengan {@link #getPertangungjawaban()} — kedua jalur diproses oleh dua
	 * blok kode yang identik salin-tempel di
	 * {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)},
	 * dan seluruh getter turunan memeriksa keduanya berurutan. Bedanya hanya pada mesin posting: LPJ
	 * kas besar tidak menyumbang kandidat akun kredit "jenis uang muka".</p>
	 *
	 * @return LPJ kas besar sumber, atau {@code null} untuk jalur lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban_kas_besar", nullable = true)
	public PertangungjawabanKasBesar getPertangungjawabanKasBesar() {
		return pertangungjawabanKasBesar;
	}

	/**
	 * Menautkan baris ini ke sebuah LPJ kas besar. Setter polos; dipanggil oleh
	 * {@link #buat(Pertangungjawaban, PertangungjawabanKasBesar, JSONObject, SaldoAwalMasterAssetDetail)}.
	 *
	 * @param pertangungjawabanKasBesar LPJ kas besar sumber; boleh {@code null}
	 */
	public void setPertangungjawabanKasBesar(PertangungjawabanKasBesar pertangungjawabanKasBesar) {
		this.pertangungjawabanKasBesar = pertangungjawabanKasBesar;
	}

	/**
	 * Mengembalikan NTPN (Nomor Transaksi Penerimaan Negara), yaitu nomor bukti setor yang
	 * diterbitkan sistem penerimaan negara. Getter polos.
	 *
	 * <p>Diinisialisasi string kosong (bukan {@code null}). Diisi dari field {@code ntpn} pada JSON
	 * item LPJ, atau — pada layar POS "Bayar Pajak" — dari masukan petugas, di mana pengisiannya
	 * <b>wajib</b>. Jalur PPh tagihan pengadaan dan PPh termin tidak pernah mengisinya, sehingga
	 * baris pajak dari kedua jalur itu tidak menyimpan bukti setor apa pun sekalipun sudah
	 * dicentang "Bayar" dan sudah dijurnal.</p>
	 *
	 * @return NTPN, atau string kosong bila belum ada
	 */
	public String getNtpn() {

		return ntpn;
	}

	/**
	 * Menyetel NTPN. Setter polos, tanpa validasi format maupun keunikan — NTPN yang sama dapat
	 * dipakai ulang di banyak baris pajak.
	 *
	 * @param ntpn nomor bukti setor; boleh {@code null}
	 */
	public void setNtpn(String ntpn) {
		this.ntpn = ntpn;
	}

	/**
	 * Mengembalikan NPWP wajib pajak — <b>getter turunan yang menulis balik</b>.
	 *
	 * <p>Untuk baris item tagihan pengadaan, NPWP diambil dari penyedia/vendor tagihan induk
	 * ({@code detail.getSaldoAwal().getPenyedia().getNpwp()}) <b>bila</b> penyedia itu punya NPWP
	 * yang tidak kosong; bila kosong, nilai tersimpan dipertahankan. Untuk jalur lain nilai
	 * tersimpan dipakai apa adanya (jalur LPJ mengisinya dari JSON, jalur termin dari penyedia PO,
	 * jalur POS dari masukan petugas).</p>
	 *
	 * <p>Karena resolusinya menulis balik, memperbaiki NPWP pada master penyedia akan <b>merambat ke
	 * baris pajak lama</b> begitu baris itu dibaca — termasuk baris yang bukti setornya sudah
	 * dicetak.</p>
	 *
	 * @return NPWP wajib pajak, atau string kosong bila belum ada
	 */
	public String getNpwp() {
		if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getSaldoAwal() != null
				&& saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia() != null
				&& saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia().getNpwp() != null
				&& !saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia().getNpwp().isEmpty()) {
			npwp = saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia().getNpwp();
		}
		return npwp;
	}

	/**
	 * Menyetel NPWP wajib pajak. Setter polos, tanpa validasi format. <b>Akan ditimpa</b> oleh
	 * {@link #getNpwp()} pada pembacaan berikutnya bila baris ini tertaut ke item tagihan yang
	 * penyedianya punya NPWP.
	 *
	 * @param npwp NPWP; boleh {@code null}
	 */
	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/**
	 * Mengembalikan nama wajib pajak — <b>getter turunan yang menulis balik</b>, dengan aturan yang
	 * sama persis seperti {@link #getNpwp()}: untuk baris item tagihan pengadaan, nama diambil dari
	 * {@code penyedia.getAtasNama()} bila tidak kosong, selain itu nilai tersimpan dipakai.
	 *
	 * <p>Perhatikan bahwa yang dipakai adalah <i>atas nama</i> rekening penyedia, bukan nama badan
	 * usahanya, sehingga kolom "Nama Wp" di layar Data Pajak dan pada Bukti Setor Pajak sebenarnya
	 * menampilkan nama pemilik rekening vendor.</p>
	 *
	 * @return nama wajib pajak, atau string kosong bila belum ada
	 */
	public String getNamaWp() {
		if (saldoAwalMasterAssetDetail != null && saldoAwalMasterAssetDetail.getSaldoAwal() != null
				&& saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia() != null
				&& saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia().getAtasNama() != null
				&& !saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia().getAtasNama().isEmpty()) {
			namaWp = saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia().getAtasNama();
		}
		return namaWp;
	}

	/**
	 * Menyetel nama wajib pajak. Setter polos. <b>Akan ditimpa</b> oleh {@link #getNamaWp()} pada
	 * pembacaan berikutnya bila baris ini tertaut ke item tagihan yang penyedianya punya "atas nama".
	 *
	 * @param namaWp nama wajib pajak; boleh {@code null}
	 */
	public void setNamaWp(String namaWp) {
		this.namaWp = namaWp;
	}

	/**
	 * Mengembalikan tanggal setor ke kas negara. Getter polos.
	 *
	 * <p>Dipetakan sebagai {@code TemporalType.DATE} — tanggal murni tanpa jam, satu-satunya properti
	 * waktu di class ini yang bukan {@code TIMESTAMP}. Diisi dari field {@code tanggalStor} pada JSON
	 * item LPJ (di-parse dengan {@code Common.dateFormat1}; kegagalan parsing menghasilkan
	 * {@code null} secara senyap) atau dari masukan wajib pada layar POS "Bayar Pajak".</p>
	 *
	 * <p>Perlu dibedakan dari {@link #getTanggalTransaksi()} ("Tanggal Bayar", tanggal internal saat
	 * petugas mencentang lunas) dan {@link #getTanggal()} (tanggal dokumen sumber). Ketiganya
	 * ditampilkan berdampingan di layar Data Pajak sebagai "Tgl.Stor", "Tanggal Bayar", dan
	 * "Tgl.Trx"; hanya "Tanggal Bayar" yang dipakai menyaring posting jurnal.</p>
	 *
	 * @return tanggal setor, atau {@code null} bila belum ada
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalStor() {
		return tanggalStor;
	}

	/**
	 * Menyetel tanggal setor ke kas negara. Setter polos, tanpa validasi (tidak ada pemeriksaan
	 * bahwa tanggal setor tidak mendahului tanggal dokumen).
	 *
	 * @param tanggalStor tanggal setor; boleh {@code null}
	 */
	public void setTanggalStor(Date tanggalStor) {
		this.tanggalStor = tanggalStor;
	}

	/**
	 * Mengembalikan baris antrean pembayaran (Daftar Pengajuan Transfer) yang mewakili setoran ini.
	 * Getter relasi polos.
	 *
	 * <p>Relasi ini sekaligus berfungsi sebagai <b>penanda idempotensi</b>:
	 * {@link DaftarPengajuanTransfer#simpanPajak(Pajak)} langsung {@code return} bila nilainya sudah
	 * terisi. Karena itu {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)} sengaja
	 * mengosongkannya di memori sebelum memanggil {@code simpanPajak} ketika ingin memaksa baris DPT
	 * lama disegarkan dengan logika terbaru.</p>
	 *
	 * <p><b>Efek samping dari jalur render:</b> {@code PajakRenderer} pada layar Data Pajak
	 * memeriksa relasi ini untuk setiap baris yang ditampilkan dan, bila masih kosong, menjadwalkan
	 * {@code simpanPajak} lewat timer ZK. Artinya <i>membuka halaman</i> saja sudah menerbitkan
	 * baris pembayaran baru ke kolam antrean transfer. Tombol "Singkronkan" pada layar yang sama
	 * melakukan hal serupa secara massal untuk baris yang belum sempat dirender (dibatasi jumlah per
	 * klik), dan bergerbang hak akses {@code UPDATE}.</p>
	 *
	 * @return baris antrean pembayaran, atau {@code null} bila belum diantrekan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menautkan/melepas baris antrean pembayaran. Setter polos.
	 *
	 * <p>Menyetel {@code null} <b>tidak</b> menghapus baris DPT yang sudah ada di database — ia hanya
	 * memutus penunjuk dari sisi ini, dan itulah yang dimanfaatkan
	 * {@link #buatDariTermin(PembayaranTerminMasterAssetDetail)} untuk memaksa sinkronisasi ulang.
	 * {@code simpanPajak} akan menemukan kembali baris DPT lama lewat kriteria {@code pajak = ...}
	 * sehingga tidak terbentuk duplikat.</p>
	 *
	 * @param daftarPengajuanTransfer baris antrean pembayaran; boleh {@code null}
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Buat / refresh baris Pajak (PPh) untuk satu detail Pembayaran Termin, lalu terbitkan baris
	 * pembayaran PPh di DPC lewat {@link DaftarPengajuanTransfer#simpanPajak(Pajak)}.
	 *
	 * <p>Nilai PPh = tarif JenisPajakBarang % x DPP(penagihan) diambil dari detail termin. Semua
	 * nilai (nilai/dpp/nama/kode/jenisPajakBarang/satuanKerja/tanggal/npwp/namaWp) di-SET langsung —
	 * getter Pajak mengembalikan nilai tersimpan karena relasi SaldoAwal/Pertanggungjawaban null.
	 * Bila detail tidak memiliki JenisPajakBarang (atau PPh 0), baris Pajak yang ada DIHAPUS supaya
	 * baris PPh di DPC ikut hilang. Idempoten (dicari by pembayaranTerminMasterAssetDetail).</p>
	 *
	 * <p>Memakai session dedikasi {@code HibernateUtil.openSession()} dan {@code simpanPajak} yang
	 * memakai currentNativeSession; aman dipanggil dari loop onSave termin karena loop mengambil
	 * ulang session-nya di iterasi berikutnya (pastikanSessionTerbuka).</p>
	 *
	 * <p><b>Bentuk baris yang dihasilkan berbeda dari jalur lain.</b> Baris PPh termin
	 * <b>tidak memiliki satu pun relasi dokumen sumber</b> — bukan LPJ, bukan detail tagihan, bukan
	 * tagihan. Identitasnya hanya {@link #getKeyData()} = id detail termin, dan itulah sebabnya
	 * pencarian idempotensinya harus menyertakan empat syarat {@code IS NULL} sekaligus. Pola
	 * pengenalan yang sama diulang di tiga tempat lain
	 * ({@code DaftarPengajuanTransferAction}, {@code DaftarPengajuanTransferSearchHelper},
	 * {@code ProsesTransferAction}) untuk menemukan kembali detail termin dari sebuah baris pajak;
	 * bila kelak ada jalur baru yang mengisi {@code keyData} tanpa relasi, keempat tempat itu perlu
	 * ikut disesuaikan.</p>
	 *
	 * <p><b>Konsekuensi tidak ada relasi:</b> seluruh getter turunan berhenti pada nilai tersimpan,
	 * sehingga nominal, DPP, kode, nama, unit kerja, NPWP, dan nama wajib pajak baris ini merupakan
	 * <i>snapshot</i> saat termin dibayar — satu-satunya jalur di class ini yang <b>tidak</b>
	 * terdampak perubahan tarif retroaktif. Konsekuensi lain: {@link #getSatuanKerja()} hanya terisi
	 * bila PO induknya punya satuan kerja, dan bila tidak, baris ini menjadi baris "tanpa unit kerja"
	 * yang terlihat oleh semua pengguna di kedua layar pajak.</p>
	 *
	 * <p><b>Penetapan {@code aktif = true}</b> dilakukan supaya baris lolos penyaring "aktif" di
	 * daftar Pertanggungjawaban Pajak. Efek sampingnya, di layar itu kolom yang sama berlabel
	 * "Bayar", sehingga baris PPh termin tampil sebagai <i>sudah dibayar</i> sejak detik pembuatannya
	 * — padahal setorannya baru akan diproses lewat baris Daftar Pengajuan Transfer yang dibuat di
	 * langkah terakhir.</p>
	 *
	 * <p><b>Penelusuran kode induk</b> memakai empat lapis cadangan berurutan: Kode Terima
	 * Tagihan/Kode BAST ({@code cariKodeTagihanAtauBast}) → Kode PO → Kode PR
	 * ({@code cariKodePermintaanInduk}) → kode pengajuan termin → kode invoice PO. Rantai itu
	 * memastikan kolom kode tidak pernah kosong, tetapi juga berarti kode yang tampil pada satu baris
	 * pajak bisa berasal dari tingkat dokumen yang berbeda-beda antar baris.</p>
	 *
	 * <p><b>Dari mana dipanggil:</b> {@code PembayaranTerminMasterAssetAction} (jalur simpan dan
	 * jalur timer per-id), tombol "Singkronkan Pajak Termin" pada
	 * {@code PertangungjawabanPajakAction} (untuk termin lama yang dibuat sebelum fitur ini ada),
	 * dan {@code PengadaanPosApiHelper} pada versi POS.</p>
	 *
	 * @param detailParam detail pembayaran termin sumber; diabaikan bila {@code null} atau belum
	 *                    punya id. Object ini hanya dipakai untuk mengambil id — datanya dimuat ulang
	 *                    di dalam session milik method ini
	 * @return {@code true} bila pada akhir proses ada baris Pajak yang aktif untuk detail tersebut;
	 *         {@code false} bila detail tidak valid, tidak punya PPh (baris lama ikut dihapus), atau
	 *         terjadi kegagalan (transaksi di-<i>rollback</i>, pesannya hanya tampil bagi admin)
	 */
	public static boolean buatDariTermin(PembayaranTerminMasterAssetDetail detailParam) {
		if (detailParam == null || detailParam.getId() == null) {
			return false;
		}
		boolean hasil = false;
		Session session = null;
		org.hibernate.Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			PembayaranTerminMasterAssetDetail detail = (PembayaranTerminMasterAssetDetail) session
					.get(PembayaranTerminMasterAssetDetail.class, detailParam.getId());
			if (detail == null) {
				return false;
			}

			JenisPajakBarang jpb = detail.getJenisPajakBarangTermin();
			Double dppNull = detail.getDppTermin();
			double dpp = dppNull == null ? 0.0 : dppNull;
			Double pphNull = detail.getNilaiPphTermin();
			double pph = pphNull == null ? 0.0 : pphNull;

			// Idempoten TANPA kolom baru (hindari ALTER pada tabel @Audited): tandai Pajak termin
			// via keyData = id detail termin, dibedakan dari jalur lain (pertanggungjawaban/saldoAwal)
			// dengan guard relasi lain NULL. Aman karena id detail unik dan Pajak termin tidak
			// memiliki relasi saldoAwal/pertanggungjawaban.
			Pajak pajak = (Pajak) session.createCriteria(Pajak.class)
					.add(Restrictions.eq("keyData", detail.getId()))
					.add(Restrictions.isNull("saldoAwalMasterAssetDetail")).add(Restrictions.isNull("saldoAwal"))
					.add(Restrictions.isNull("pertangungjawaban")).add(Restrictions.isNull("pertangungjawabanKasBesar"))
					.setMaxResults(1).uniqueResult();

			tx = session.beginTransaction();
			if (jpb != null && pph > 0.0) {
				if (pajak == null) {
					pajak = new Pajak();
				}
				pajak.setKeyData(detail.getId());
				// aktif = true agar baris PPh termin diperlakukan sama seperti pajak jalur lain dan lolos
				// filter "aktif" di daftar Pertanggungjawaban Pajak (getAktif() default false bila null).
				pajak.setAktif(true);
				pajak.setJenisPajakBarang(jpb);
				pajak.setNilai(pph);
				pajak.setDpp(dpp);
				// kode = KODE INDUK. Prioritas sesuai permintaan:
				//   (1) Kode Terima Tagihan (SaldoAwalMasterAsset) — ditelusuri PO → BAST
				//       (PenerimaanPengadaanMasterAsset) → getSaldoAwalMasterAsset().getKodeTagihan();
				//   (2) bila tak ada → Kode BAST (PenerimaanPengadaanMasterAsset.getKode());
				//   (3) bila tak ada → Kode PO (Pemesanan Pengadaan .getKode());
				//   (4) bila tak ada → Kode PR (Permintaan Pengadaan induk).
				// Langkah (1)+(2) di-resolve bersama di cariKodeTagihanAtauBast (butuh lookup BAST yang
				// sama). Fallback terakhir: kode pengajuan termin (…/TRM/…) lalu kode invoice PO agar
				// kolom tidak pernah kosong.
				ais.database.model.asset.PemesananPengadaanMasterAsset poInduk = detail
						.getPemesananPengadaanMasterAsset();
				String kodeInduk = cariKodeTagihanAtauBast(session, poInduk);
				if (kodeInduk == null || kodeInduk.trim().isEmpty()) {
					kodeInduk = poInduk == null ? null : poInduk.getKode();
				}
				if (kodeInduk == null || kodeInduk.trim().isEmpty()) {
					kodeInduk = cariKodePermintaanInduk(session, poInduk);
				}
				if (kodeInduk == null || kodeInduk.trim().isEmpty()) {
					kodeInduk = detail.getPembayaranTerminMasterAsset() == null ? null
							: detail.getPembayaranTerminMasterAsset().getKode();
				}
				if ((kodeInduk == null || kodeInduk.trim().isEmpty()) && poInduk != null) {
					kodeInduk = poInduk.getKodeInvoice();
				}
				pajak.setKode(kodeInduk);
				// Nama = deskripsi BERSIH "Tagihan Termin" (TANPA kode). Kode pengajuan termin
				// ditampilkan sebagai baris TERSENDIRI di DPC lewat getPajak().getKode() (lihat
				// DaftarPengajuanTransferAction renderer: label "kode pengajuan") dan sebagai tombol
				// di daftar Pertanggungjawaban Pajak. Menaruh kode di nama akan membuatnya tampil
				// GANDA (di judul hijau DPC dan di baris kode), tidak sesuai contoh yang diminta.
				pajak.setNama("Tagihan Termin");
				if (detail.getPemesananPengadaanMasterAsset() != null) {
					pajak.setSatuanKerja(detail.getPemesananPengadaanMasterAsset().getSatuanKerja());
				}
				pajak.setTanggal(detail.getTanggalDibayar());

				PenyediaAsset penyedia = detail.getPembayaranTerminMasterAsset() == null ? null
						: detail.getPembayaranTerminMasterAsset().getPenyedia();
				if (penyedia == null && detail.getPemesananPengadaanMasterAsset() != null) {
					penyedia = detail.getPemesananPengadaanMasterAsset().getPenyedia();
				}
				if (penyedia != null) {
					pajak.setNpwp(penyedia.getNpwp());
					pajak.setNamaWp(penyedia.getAtasNama());
				}
				Common.refreshSaveOrUpdate(session, pajak);
			} else if (pajak != null) {
				Common.refreshDelete(session, pajak);
				pajak = null;
			}
			tx.commit();

			if (pajak != null) {
				// Paksa simpanPajak menyinkronkan ulang baris DPT walau sudah ada: set ref DPT ke null
				// (in-memory) supaya guard "getDaftarPengajuanTransfer() != null -> return" tidak
				// men-skip. Perlu agar kolom computed DPT (mis. waktu) ter-persist ULANG dengan logika
				// terbaru pada baris lama; simpanPajak akan menemukan DPT lama via kriteria pajak dan
				// menyimpannya kembali (bukan membuat duplikat).
				pajak.setDaftarPengajuanTransfer(null);
				DaftarPengajuanTransfer.simpanPajak(pajak);
			}
			hasil = pajak != null;
		} catch (Exception e) {
			try {
				if (tx != null && tx.isActive()) {
					tx.rollback();
				}
			} catch (Exception rollbackException) { ais.common.ErrorAuditUtil.record(rollbackException, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:783");
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					if (session.isOpen()) {
						session.close();
					}
				} catch (Exception closeException) { ais.common.ErrorAuditUtil.record(closeException, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:792");
				}
			}
			HibernateUtil.closeSession();
		}
		return hasil;
	}

	/**
	 * Telusuri kode INDUK level penerimaan/tagihan dari sebuah PO, dengan prioritas:
	 * (1) Kode Terima Tagihan = {@code SaldoAwalMasterAsset.getKodeTagihan()} (fallback {@code getKode()})
	 * yang tertaut ke BAST; (2) bila tak ada → Kode BAST = {@code PenerimaanPengadaanMasterAsset.getKode()}.
	 *
	 * <p>BAST (Penerimaan) dicari dari PO via relasi {@code pemesananPengadaanMasterAsset}; Terima
	 * Tagihan diambil dari {@code bast.getSaldoAwalMasterAsset()} (rantai sama dengan Trace Status
	 * Pengadaan). Kembalikan {@code null} bila BAST/tagihan tak ada agar pemanggil lanjut ke PO/PR.</p>
	 *
	 * @param session sesi Hibernate aktif
	 * @param po      PO induk termin; boleh {@code null}
	 * @return kode Terima Tagihan, atau kode BAST, atau {@code null}
	 */
	private static String cariKodeTagihanAtauBast(Session session,
			ais.database.model.asset.PemesananPengadaanMasterAsset po) {
		if (po == null || session == null) {
			return null;
		}
		try {
			ais.database.model.asset.PenerimaanPengadaanMasterAsset bast = (ais.database.model.asset.PenerimaanPengadaanMasterAsset) session
					.createCriteria(ais.database.model.asset.PenerimaanPengadaanMasterAsset.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset", po)).setMaxResults(1).uniqueResult();
			if (bast == null) {
				return null;
			}
			// (1) Kode Terima Tagihan dari SaldoAwalMasterAsset yang tertaut BAST.
			ais.database.model.asset.SaldoAwalMasterAsset tagihan = bast.getSaldoAwalMasterAsset();
			if (tagihan != null) {
				String k = tagihan.getKodeTagihan();
				if (k == null || k.trim().isEmpty()) {
					k = tagihan.getKode();
				}
				if (k != null && !k.trim().isEmpty()) {
					return k.trim();
				}
			}
			// (2) Kode BAST.
			String kb = bast.getKode();
			if (kb != null && !kb.trim().isEmpty()) {
				return kb.trim();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:855");
			// Diamkan: pemanggil punya fallback (PO / PR / termin).
		}
		return null;
	}

	/**
	 * Telusuri kode INDUK pengadaan (PR / Permintaan Pengadaan) dari sebuah PO. Kode induk dipakai
	 * sebagai kode baris Pajak PPh termin (permintaan: samakan kode pajak termin dengan kode PR induk).
	 *
	 * <p>PO menyimpan referensi PR sebagai CSV id {@code PermintaanPengadaanMasterAssetDetail} pada
	 * {@code getPermintaanPengadaanMasterAssets()} (lihat PemesananPengadaanMasterAssetAction.onSave).
	 * Ambil id pertama yang valid, muat PR-detail, lalu kembalikan
	 * {@code getPermintaanPengadaanMasterAsset().getKode()}. Bila PO tak punya referensi PR (mis. dibuat
	 * "tanpa permintaan") kembalikan {@code null} agar pemanggil memakai fallback.</p>
	 *
	 * @param session sesi Hibernate yang sedang aktif (dipakai untuk memuat PR-detail)
	 * @param po      PO induk termin; boleh {@code null}
	 * @return kode PR induk, atau {@code null} bila tak ada
	 */
	private static String cariKodePermintaanInduk(Session session,
			ais.database.model.asset.PemesananPengadaanMasterAsset po) {
		if (po == null || session == null) {
			return null;
		}
		try {
			String csv = po.getPermintaanPengadaanMasterAssets();
			if (csv == null || csv.trim().isEmpty()) {
				return null;
			}
			for (String tok : csv.split(",")) {
				String t = tok == null ? "" : tok.trim();
				if (t.isEmpty()) {
					continue;
				}
				Long prDetailId;
				try {
					prDetailId = Long.valueOf(t);
				} catch (Exception e) {
					continue;
				}
				ais.database.model.asset.PermintaanPengadaanMasterAssetDetail prDet = (ais.database.model.asset.PermintaanPengadaanMasterAssetDetail) session
						.get(ais.database.model.asset.PermintaanPengadaanMasterAssetDetail.class, prDetailId);
				if (prDet != null && prDet.getPermintaanPengadaanMasterAsset() != null) {
					String k = prDet.getPermintaanPengadaanMasterAsset().getKode();
					if (k != null && !k.trim().isEmpty()) {
						return k.trim();
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/Pajak.java:891");
			// Diamkan: pemanggil sudah punya fallback (kode termin / kode invoice).
		}
		return null;
	}

	/**
	 * Mengembalikan isi kolom {@code jumlah} — <b>getter turunan yang menulis balik</b>, dan
	 * <b>kolom paling ambigu di class ini</b>.
	 *
	 * <p>Bila baris punya jenis PPh, method ini <b>menimpa</b> field {@code jumlah} dengan
	 * <i>persentase tarif</i> {@code JenisPajakBarang.getPersen()} (mis. {@code 2.0} untuk 2%) dan
	 * mengembalikannya. Nama method dan nama kolom menyiratkan sebuah <i>nominal</i>, padahal yang
	 * tersimpan pada mayoritas baris justru sebuah <i>persen</i>.</p>
	 *
	 * <p><b>Akibat yang perlu diketahui:</b> {@code PengadaanPosApiHelper.pajakSetor} mengisi kolom
	 * ini dengan total nominal setoran ({@code setJumlah(totalNilai)}). Untuk setoran <b>PPH</b> —
	 * yang jenis PPh-nya terisi — nilai nominal itu <b>lenyap ditimpa tarif</b> pada pembacaan
	 * pertama, dan karena penulisan balik ikut ter-<i>flush</i>, hilangnya permanen di database.
	 * Untuk setoran <b>PPN</b> (jenis PPh {@code null}) nominalnya bertahan. Jadi satu kolom yang
	 * sama berisi persen pada sebagian baris dan rupiah pada sebagian lain; jangan menjumlahkannya.</p>
	 *
	 * <p>Sejauh penelusuran, tidak ada layar atau laporan yang membaca kolom ini untuk entity
	 * {@code Pajak} — pemakaian {@code getJumlah()} yang ditemukan di modul lain seluruhnya milik
	 * {@code ais.database.model.sirs.PajakMedis} dan detail transaksi SIRS, kelas yang berbeda.
	 * Kolom ini karena itu praktis tidur, tetapi tetap tertulis ke database (dan ke tabel revisi
	 * Envers) setiap kali baris pajak dibaca.</p>
	 *
	 * @return persentase tarif PPh bila jenis PPh terisi; selain itu nilai tersimpan (bisa
	 *         {@code null})
	 */
	public Double getJumlah() {
		if (getJenisPajakBarang() != null) {
			jumlah = getJenisPajakBarang().getPersen();
		}
		return jumlah;
	}

	/**
	 * Menyetel isi kolom {@code jumlah}. Setter polos — tetapi lihat peringatan di
	 * {@link #getJumlah()}: untuk baris yang punya jenis PPh, apa pun yang diset di sini akan
	 * ditimpa oleh persentase tarif pada pembacaan berikutnya.
	 *
	 * @param jumlah nilai kolom {@code jumlah}; boleh {@code null}
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan <b>Dasar Pengenaan Pajak (DPP)</b> dalam rupiah — <b>getter turunan yang
	 * menulis balik</b>, dan pasangan wajib dari {@link #getNilai()}.
	 *
	 * <p>Sumbernya menurut jalur baris:</p>
	 * <ol>
	 *   <li><b>Item tagihan pengadaan</b> — dihitung ulang sebagai
	 *   <code>detail.getJumlah() x detail.getHarga()</code>, keduanya juga getter turunan pada
	 *   {@code SaldoAwalMasterAssetDetail} yang bisa mengambil angka dari BAST bila field lokalnya
	 *   kosong.</li>
	 *   <li><b>LPJ uang muka / LPJ kas besar</b> — dibaca dari field {@code jumlah} pada salinan JSON
	 *   {@link #getPajakData()}; kegagalan parsing hanya dicatat dan nilai lama dipertahankan.</li>
	 *   <li><b>PPh termin, Bukti Potong, setoran POS</b> — tidak ada cabang yang cocok, nilai
	 *   tersimpan dikembalikan (bisa {@code null}, dan method ini <b>tidak</b> menormalkannya menjadi
	 *   0 — berbeda dari {@link #getNilai()}).</li>
	 * </ol>
	 *
	 * <p><b>Selisih dengan DPP yang dipakai dokumen sumber — perlu diperhatikan.</b> Rumus di sini
	 * <b>tidak mengurangkan potongan harga</b>, sedangkan {@code SaldoAwalMasterAssetDetail.hitungPph()},
	 * {@code hitungPpn()}, dan {@code getHargaTotal()} semuanya memakai
	 * <code>(jumlah x harga) - potongan</code> (potongan bisa berbentuk persen atau nominal).
	 * Untuk item tagihan yang berdiskon, DPP di baris pajak ini karena itu lebih besar daripada DPP
	 * yang dipakai menghitung pembayaran vendor, dan — karena {@link #getNilai()} mengalikannya
	 * dengan tarif — nominal PPh yang diantrekan serta dijurnal ikut lebih besar. Untuk item tanpa
	 * potongan kedua rumus identik, sehingga selisih ini hanya muncul pada tagihan berdiskon.</p>
	 *
	 * <p>Cabang pertama memuat pemeriksaan {@code saldoAwalMasterAssetDetail == null ? 0.0 : ...}
	 * yang tidak pernah bernilai benar (kondisi {@code != null} sudah dipastikan tepat di atasnya) —
	 * sisa penyuntingan lama, tidak berpengaruh pada hasil.</p>
	 *
	 * @return DPP dalam rupiah, atau {@code null} bila belum pernah diisi maupun diturunkan
	 */
	public Double getDpp() {
		SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = getSaldoAwalMasterAssetDetail();

		if (saldoAwalMasterAssetDetail != null) {
			dpp = saldoAwalMasterAssetDetail == null ? 0.0
					: (saldoAwalMasterAssetDetail.getJumlah() * saldoAwalMasterAssetDetail.getHarga());
		} else if (pertangungjawaban != null && pajakData != null) {
			try {
				dpp = new JSONObject(pajakData).getDouble("jumlah");
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/Pajak.java:918");
			}
		} else if (pertangungjawabanKasBesar != null && pajakData != null) {
			try {
				dpp = new JSONObject(pajakData).getDouble("jumlah");
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/Pajak.java:924");
			}
		}

		return dpp;
	}

	/**
	 * Menyetel DPP. Setter polos, tanpa validasi.
	 *
	 * <p><b>Efektif hanya untuk baris tanpa relasi</b> (PPh termin, Bukti Potong, setoran POS);
	 * untuk baris LPJ dan baris item tagihan, {@link #getDpp()} akan menghitung ulang dan menimpanya
	 * pada pembacaan berikutnya.</p>
	 *
	 * @param dpp Dasar Pengenaan Pajak dalam rupiah; boleh {@code null}
	 */
	public void setDpp(Double dpp) {
		this.dpp = dpp;
	}
	/**
	 * Jenis PPN bila setoran ini mewakili PPN, bukan PPh. Satu rekaman setoran hanya
	 * mewakili SATU jenis pajak: {@code jenisPajakBarang} terisi untuk PPh, kolom ini
	 * terisi untuk PPN. Kolom NULLABLE sehingga rekaman lama tetap sah.
	 * Ditambahkan 2026-08-20 bersama tahap "Bayar Pajak" pada Pengadaan POS.
	 *
	 * <p><b>Cakupan pemakaian sangat sempit — hasil verifikasi.</b> Satu-satunya penulis kolom ini
	 * adalah {@code PengadaanPosApiHelper.pajakSetor} (layar POS "Bayar Pajak"), dan satu-satunya
	 * pembacanya adalah helper POS yang sama, untuk memberi label baris sebagai "PPN" atau "PPH" dan
	 * mencetak Bukti Setor. <b>Kedua layar ZK tidak mengenalnya sama sekali</b>:</p>
	 * <ul>
	 *   <li>{@code PertangungjawabanPajakAction} menampilkan kolom "Jenis Pajak" dari
	 *   {@link #getJenisPajakBarang()} saja, sehingga baris PPN tampil dengan jenis kosong;</li>
	 *   <li>{@code PostingPertangungjawabanPajakAction} mengambil akun debet dengan
	 *   {@code pajak.getJenisPajakBarang().getAkun()} tanpa penjaga {@code null} dan tanpa cabang
	 *   untuk PPN — baris PPN karena itu melempar NPE yang ditangkap per baris
	 *   ("lanjut ke entitas berikutnya", hanya dicatat ke {@code ErrorAuditUtil}), sehingga
	 *   <b>setoran PPN tidak pernah masuk jurnal</b> lewat layar itu dan juga tidak menghasilkan
	 *   pesan kesalahan yang terlihat pengguna.</li>
	 * </ul>
	 * <p>Catatan ketidakcocokan dokumentasi yang perlu diverifikasi di DDL: komentar di
	 * {@code BreakdownTagihanVendorHelper} menyatakan kolom {@code jenis_pajak_barang} pada tabel
	 * {@code akunting.pajak} bersifat {@code NOT NULL}, sedangkan pemetaan di class ini sudah
	 * {@code nullable = true} dan jalur setoran PPN memang menyimpan baris tanpa jenis PPh. Bila
	 * batasan {@code NOT NULL} masih ada di database, pencatatan setoran PPN akan gagal di tingkat
	 * DB, bukan hanya gagal dijurnal.</p>
	 *
	 * @return master jenis PPN, atau {@code null} untuk baris PPh maupun baris tanpa pajak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak_ppn", nullable = true)
	public ais.database.model.asset.JenisPajakPpn getJenisPajakPpn() {
		jenisPajakPpn = check(jenisPajakPpn);
		return jenisPajakPpn;
	}

	/**
	 * Menyetel master jenis PPN. Setter polos; hanya dipanggil oleh
	 * {@code PengadaanPosApiHelper.pajakSetor} saat jenis setoran yang dipilih adalah PPN.
	 *
	 * <p>Satu baris {@code Pajak} hanya mewakili SATU jenis pajak: isi
	 * {@link #getJenisPajakBarang()} untuk PPh <b>atau</b> properti ini untuk PPN, tidak keduanya.
	 * Tidak ada validasi di entity yang menegakkan aturan itu.</p>
	 *
	 * @param jenisPajakPpn master jenis PPN; boleh {@code null}
	 */
	public void setJenisPajakPpn(ais.database.model.asset.JenisPajakPpn jenisPajakPpn) {
		this.jenisPajakPpn = jenisPajakPpn;
	}
}
