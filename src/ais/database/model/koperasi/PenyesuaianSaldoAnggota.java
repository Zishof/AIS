package ais.database.model.koperasi;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

import ais.database.model.Deposit;
import ais.database.model.GeneralValueObject;

/**
 * <h3>Penyesuaian Saldo Anggota &mdash; "stok opname" untuk saldo voucher/deposit member.</h3>
 *
 * <p>Perannya persis seperti {@code StokOpname} pada barang, hanya objeknya saldo: mencatat
 * <b>saldo menurut sistem</b>, <b>saldo yang seharusnya</b> menurut hitungan petugas, selisih di
 * antara keduanya, dan alasannya. Tanpa catatan seperti ini, koreksi saldo hanya berupa entri
 * deposit tambahan yang tidak menerangkan apa pun ketika ditanyakan berbulan-bulan kemudian.</p>
 *
 * <p><b>Cara koreksinya diterapkan.</b> Saldo member TIDAK disimpan sebagai satu kolom, melainkan
 * dihitung ({@code DepositHelper.hitungDeposit} = jumlah {@link Deposit} dikurangi pemakaian).
 * Karena itu penyesuaian tidak "menimpa" saldo, melainkan membuat satu baris {@link Deposit}
 * senilai selisihnya &mdash; positif bila saldo kurang, negatif bila saldo lebih. Dengan begitu
 * riwayat mutasi tetap utuh dan saldo hasil hitungan langsung cocok dengan hasil opname.</p>
 *
 * <p>Baris {@link Deposit} yang terbentuk disimpan di {@link #getDeposit()} sebagai jejak balik,
 * sehingga dari catatan opname selalu dapat ditelusuri entri mana yang mengoreksinya.</p>
 *
 * <p><b>Gerbang otorisasi (dicek di {@code PenyesuaianSaldoHelper}, BUKAN di entity ini).</b>
 * Karena baris ini bisa menggeser saldo finansial anggota tanpa melalui transaksi
 * belanja/topup normal, entity ini sendiri TIDAK punya penjagaan apa pun -- siapa saja yang bisa
 * memanggil setter/{@code save} langsung bisa membuat opname sembarangan. Penjagaan sepenuhnya
 * ada di lapisan servlet {@code PenyesuaianSaldoHelper}, yaitu: (1) hanya peran ber-{@code
 * Tbmrole.bolehEntryTopup} yang boleh menyimpan (gerbang yang sama dgn topup/edit deposit,
 * dicek server-side); (2) {@link #getKeterangan()} wajib diisi; (3) saldo sistem DIBACA ULANG
 * di server tepat sebelum baris ini dibentuk (bukan memakai angka kiriman klien), sehingga
 * {@link #getSelisih()} selalu dihitung dari keadaan terkini; (4) baris {@link Deposit} dan
 * baris opname ini dibuat dalam SATU transaksi Hibernate (rollback bersama bila salah satu
 * gagal). Yang TIDAK ada: batas nominal per penyesuaian, alur persetujuan dua-tingkat
 * (maker-checker), dan notifikasi ke anggota terkait -- satu pengguna ber-hak topup dapat
 * langsung mengeksekusi koreksi sebesar apa pun sendirian. Ini pola yang SAMA dgn topup/edit
 * deposit manual (bukan celah baru khusus kelas ini), tetapi patut diingat bila kelak ada
 * kebutuhan audit tambahan (mis. threshold nominal yang wajib disetujui atasan).</p>
 */
@Entity
@Audited
@Table(schema = "koperasi", name = "penyesuaian_saldo_anggota")
public class PenyesuaianSaldoAnggota extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}, entity ini tidak dikirim lewat Java serialization jarak jauh. */
	private static final long serialVersionUID = 1L;

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Anggota koperasi (member) yang saldonya diopname/dikoreksi lewat baris ini. Lihat
	 * {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Saldo hasil hitungan sistem PADA SAAT opname disimpan (dibekukan sebagai bukti). Lihat
	 * {@link #getSaldoSistem()}. */
	private Double saldoSistem;
	/** Saldo yang seharusnya menurut petugas (padanan "stok fisik" pada opname barang). Lihat
	 * {@link #getSaldoFisik()}. */
	private Double saldoFisik;
	/** Saldo fisik dikurangi saldo sistem; positif = saldo ditambah, negatif = saldo dikurangi.
	 * Lihat {@link #getSelisih()}. */
	private Double selisih;
	/** Waktu opname/koreksi ini dicatat. Nilai default (sebelum di-{@code set}) adalah waktu
	 * instansiasi objek Java, BUKAN waktu insert DB. Lihat {@link #getWaktu()}. */
	private Date waktu = ais.ui.util.WaktuUtil.getDate();
	/** Alasan penyesuaian; wajib diisi supaya koreksi saldo selalu dapat dipertanggungjawabkan.
	 * Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Baris Deposit koreksi yang dibentuk penyesuaian ini (jejak balik ke mutasi saldonya).
	 * Lihat {@link #getDeposit()}. */
	private Deposit deposit;
	/** Nama petugas yang mencatat baris ini (jejak audit tampilan, bukan FK). Lihat
	 * {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang mencatat baris ini (jejak audit, pasangan {@link #oleh}). Lihat
	 * {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris opname ini TERAKHIR diubah, dengan
	 * menuliskan waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum setiap {@code UPDATE}, tidak pernah dipanggil manual dari kode
	 * aplikasi -- pada praktiknya baris opname jarang di-{@code UPDATE} setelah dibuat, karena
	 * {@code PenyesuaianSaldoHelper} hanya mengenal jalur {@code simpan} (insert) dan
	 * {@code list} (baca), tidak ada aksi edit/hapus.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}. Nilai
	 * awal (sebelum ada update apa pun) adalah waktu instansiasi objek Java, bukan waktu insert
	 * DB sesungguhnya. Lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi
	 * -- baris opname ini normalnya HANYA dibentuk oleh {@code PenyesuaianSaldoHelper.simpan},
	 * yang mensyaratkan hak akses {@code Tbmrole.bolehEntryTopup}, membaca ulang saldo sistem
	 * terkini (bukan memakai angka kiriman klien), mewajibkan {@link #keterangan} terisi, dan
	 * SELALU membentuk baris {@link Deposit} pendamping dalam transaksi yang sama (lihat Javadoc
	 * kelas dan berkas {@code PenyesuaianSaldoHelper}). Membuat instance ini secara manual di
	 * luar jalur tsb melewati SEMUA penjagaan itu. */
	public PenyesuaianSaldoAnggota() {
	}

	/**
	 * PK identity baris opname ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate. Catatan: {@code @Column(insertable = false)} di sini -- sama seperti pola pada
	 * {@link PembayaranHutang#getId()}, tidak berpengaruh praktis krn kolom identity memang
	 * selalu dibuat DB, bukan dikirim Hibernate saat insert.
	 *
	 * @return id baris opname, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini secara eksplisit.
	 *
	 * @param id id baris opname.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Anggota koperasi (member) yang saldonya diopname/dikoreksi lewat baris ini. Relasi
	 * {@code LAZY}: mengakses field pada objek yang dikembalikan di luar sesi Hibernate yang
	 * masih terbuka akan melempar {@code LazyInitializationException}. Catatan: kolomnya
	 * {@code nullable = true} scr definisi JPA, tetapi {@code PenyesuaianSaldoHelper.simpan}
	 * SELALU mensyaratkan {@code id_member} valid sebelum baris ini dibentuk -- pada praktiknya
	 * field ini tidak pernah {@code null} utk baris yang dibuat lewat jalur resmi.
	 *
	 * @return anggota koperasi yang diopname.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	/**
	 * Menetapkan anggota koperasi yang diopname. Tidak memvalidasi apa pun di level entity --
	 * validasi "member wajib dipilih dan ditemukan" ada di {@code PenyesuaianSaldoHelper},
	 * bukan di setter ini.
	 *
	 * @param anggotaKoperasi anggota koperasi terkait.
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/** Saldo hasil hitungan sistem PADA SAAT opname disimpan (dibekukan sebagai bukti).
	 *
	 * @return saldo sistem yang dibekukan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "saldo_sistem")
	public Double getSaldoSistem() {
		return saldoSistem;
	}

	/**
	 * Menetapkan saldo sistem yang dibekukan. Nilai ini SEHARUSNYA hasil hitungan
	 * {@code DepositHelper.hitungDeposit} yang dibaca ULANG oleh helper tepat sebelum baris ini
	 * disimpan (bukan angka kiriman klien) -- setter ini sendiri menerima nilai apa pun tanpa
	 * verifikasi ulang terhadap saldo aktual.
	 *
	 * @param saldoSistem saldo sistem baru.
	 */
	public void setSaldoSistem(Double saldoSistem) {
		this.saldoSistem = saldoSistem;
	}

	/** Saldo yang seharusnya menurut petugas (padanan "stok fisik" pada opname barang).
	 *
	 * @return saldo fisik hasil opname, atau {@code null} bila belum diisi.
	 */
	@Column(name = "saldo_fisik")
	public Double getSaldoFisik() {
		return saldoFisik;
	}

	/**
	 * Menetapkan saldo fisik hasil opname. Tidak divalidasi di sini (boleh negatif bila
	 * dipanggil langsung) -- {@code PenyesuaianSaldoHelper.simpan} menolak nilai negatif dan
	 * nilai bukan angka sebelum entity ini dibangun.
	 *
	 * @param saldoFisik saldo fisik baru.
	 */
	public void setSaldoFisik(Double saldoFisik) {
		this.saldoFisik = saldoFisik;
	}

	/** Saldo fisik dikurangi saldo sistem; positif = saldo ditambah, negatif = saldo dikurangi.
	 *
	 * @return selisih saldo, atau {@code null} bila belum diisi.
	 */
	@Column(name = "selisih")
	public Double getSelisih() {
		return selisih;
	}

	/**
	 * Menetapkan selisih saldo. Nilai ini yang menentukan arah &amp; nominal baris {@link
	 * Deposit} pendamping ({@code selisih} langsung dipakai sbg {@code Deposit.nominal}, bukan
	 * nilai mutlaknya -- lihat {@code PenyesuaianSaldoHelper.simpan}). Tidak divalidasi terhadap
	 * {@link #getSaldoFisik()} - {@link #getSaldoSistem()} di setter ini; helper yang menjamin
	 * konsistensi ketiganya sebelum {@code save}.
	 *
	 * @param selisih selisih saldo baru.
	 */
	public void setSelisih(Double selisih) {
		this.selisih = selisih;
	}

	/**
	 * Waktu opname/koreksi ini dicatat. Berbeda dari beberapa entity lain di paket ini,
	 * getter ini TIDAK null-safe -- mengembalikan {@code null} apa adanya bila field {@link
	 * #waktu} pernah di-{@code set} eksplisit ke {@code null} (walau nilai awal field selalu
	 * waktu instansiasi objek Java, lihat inisialisasi {@link #waktu}).
	 *
	 * @return waktu opname, bisa {@code null} bila di-{@code set} eksplisit ke {@code null}.
	 */
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu;
	}

	/**
	 * Menetapkan waktu opname/koreksi.
	 *
	 * @param waktu waktu opname baru.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/** Alasan penyesuaian; wajib diisi supaya koreksi saldo selalu dapat dipertanggungjawabkan.
	 *
	 * @return alasan penyesuaian, atau {@code null} bila baris dibuat di luar jalur helper
	 *         resmi (jalur resmi selalu mengisinya -- lihat {@code PenyesuaianSaldoHelper.simpan}).
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan alasan penyesuaian. Tidak ada guard "wajib diisi" di level entity -- kewajiban
	 * itu ditegakkan {@code PenyesuaianSaldoHelper.simpan} (menolak keterangan kosong sebelum
	 * entity ini dibangun), bukan di setter ini.
	 *
	 * @param keterangan alasan penyesuaian baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Baris Deposit koreksi yang dibentuk penyesuaian ini (jejak balik ke mutasi saldonya).
	 *
	 * @return baris {@link Deposit} pendamping, atau {@code null} bila baris opname dibuat di
	 *         luar jalur helper resmi.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "deposit", nullable = true)
	public Deposit getDeposit() {
		deposit = check(deposit);
		return deposit;
	}

	/**
	 * Menetapkan baris Deposit pendamping. Dipanggil {@code PenyesuaianSaldoHelper.simpan}
	 * setelah baris {@link Deposit} disimpan dalam transaksi yang sama; kode aplikasi normal
	 * tidak perlu memanggilnya langsung.
	 *
	 * @param deposit baris Deposit koreksi terkait.
	 */
	public void setDeposit(Deposit deposit) {
		this.deposit = deposit;
	}

	/**
	 * Nama petugas yang mencatat baris opname ini (jejak audit tampilan, bukan FK). Berbeda dari
	 * pola guard null/blank pada entity lain di paket ini ({@link PembayaranHutang#setOleh
	 * (String)} dkk.), setter {@link #setOleh(String)} DI SINI TIDAK memiliki guard -- nilai
	 * {@code null}/kosong akan menimpa nilai lama apa adanya.
	 *
	 * @return nama pencatat, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pencatat. TIDAK ADA guard null/blank di sini (beda dgn entity lain di
	 * paket ini) -- memanggil dgn {@code null}/kosong akan mengosongkan field yang sudah terisi.
	 *
	 * @param oleh nama pencatat baru.
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * ID/username petugas yang mencatat baris opname ini, pasangan {@link #getOleh()}.
	 *
	 * @return id/username pencatat, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "olehid")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id/username pencatat. TIDAK ADA guard null/blank -- sama catatan dgn
	 * {@link #setOleh(String)}.
	 *
	 * @param olehId id/username pencatat baru.
	 */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Timestamp perubahan terakhir baris opname ini, diisi otomatis oleh {@link #onUpdate()}
	 * setiap kali Hibernate melakukan {@code UPDATE}. Nilai awal (sebelum ada update apa pun)
	 * adalah waktu instansiasi objek Java, BUKAN waktu insert DB sesungguhnya.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}; memanggil setter ini eksplisit dari kode aplikasi
	 * akan ditimpa lagi oleh callback tsb pada {@code UPDATE} berikutnya.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** Riwayat posting jurnal (dok 61 butir B tahap 2): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} menjurnalkan dokumen ini. Jurnal memorial tanpa kas, arah
	 * mengikuti tanda {@link #selisih} (positif = kewajiban ke anggota bertambah), nilainya
	 * memakai nilai mutlak {@link #selisih} supaya baris debet/kredit tidak pernah negatif;
	 * selisih nol tidak pernah terpilih utk diposting. Lihat {@link #getPostingHistory()}. */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal (dok 61 butir B tahap 2): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} menjurnalkan dokumen ini.
	 *
	 * @return riwayat posting, atau {@code null} bila baris opname ini belum diposting (mis.
	 *         konfigurasi akun {@code akun_selisih_saldo_anggota_id} belum diisi, sehingga mesin
	 *         posting sengaja tidak menjurnalkan apa pun).
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/**
	 * Menetapkan riwayat posting jurnal. Dipanggil {@code PostingDanaAnggotaUtil} saat
	 * menjurnalkan baris ini; kode aplikasi normal tidak perlu memanggilnya langsung.
	 *
	 * @param postingHistory riwayat posting baru.
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
