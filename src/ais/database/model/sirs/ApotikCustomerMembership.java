package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
 * Kartu keanggotaan pelanggan apotik (program loyalitas + pengingat isi ulang obat rutin),
 * tabel {@code sirs.apotik_customer_membership}. Satu baris = satu anggota; saldo poin
 * ({@link #getPoinSaldo()}) adalah PROYEKSI CEPAT (denormalized) yang harus selalu sinkron
 * dengan jumlah baris {@link ApotikRewardLedger} milik membership ini — ledger adalah sumber
 * kebenaran riwayat, baris ini hanya cache saldo terkini untuk tampilan cepat tanpa agregasi.
 *
 * <p>Ditulis/dibaca lewat {@code ais.action.servlet.api.ApotikMembershipHelper}
 * ({@code daftar}/{@code simpan}/{@code mutasiPoin}/{@code refill}/{@code provisionDemo}) —
 * BUKAN dari layar CRUD generik. Anggota TIDAK harus terkait {@link Pasien} (FK nullable,
 * membership bisa dibuat untuk pelanggan umum yang belum pernah jadi pasien rawat).</p>
 *
 * <p><b>Peringatan konkurensi:</b> {@code ApotikMembershipHelper.mutasiPoin()} membaca
 * {@link #getPoinSaldo()}, menghitung saldo baru di memori, memvalidasi tidak negatif, LALU
 * menulis balik — pola baca-hitung-tulis TANPA row-lock (`session.update` biasa, tanpa
 * {@code LockMode}/{@code FOR UPDATE}) maupun versi optimistik ({@code @Version}) pada entity
 * ini. Dua permintaan penukaran poin (`PENUKARAN`) bersamaan untuk membership yang sama dapat
 * kedua-duanya membaca saldo lama yang sama, kedua-duanya lolos validasi "tidak boleh
 * negatif", dan kedua-duanya commit — poin bisa ditukar melebihi saldo sesungguhnya
 * (double-spend), pola race condition yang SAMA PERSIS dengan bug check-in ganda kamar hotel
 * ({@code task_b718f355}) dan bug double-booking peminjaman aset yang sudah ditambal
 * ({@code PeminjamanMasterAssetHelper.sedangDipinjamAktif()}) — TAPI di subsistem berbeda
 * (loyalitas apotik, bukan finansial riil), jadi dicatat sebagai instance terpisah.</p>
 *
 * @see ApotikRewardLedger
 * @see Pasien
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_customer_membership")
public class ApotikCustomerMembership extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	/** Nilai {@link #getStatus()} bawaan — membership aktif, boleh transaksi/poin. */
	public static final String AKTIF = "AKTIF";
	/** Nilai {@link #getStatus()} — dinonaktifkan sementara (mis. permintaan sendiri), belum tentu karena pelanggaran. */
	public static final String NONAKTIF = "NONAKTIF";
	/** Nilai {@link #getStatus()} — diblokir (mis. penyalahgunaan poin/kecurigaan fraud), paling restriktif. */
	public static final String DIBLOKIR = "DIBLOKIR";

	private Long id;
	private String kode;
	private Pasien pasien;
	private String nama;
	private String telepon;
	private String tier;
	private Long poinSaldo;
	private String status;
	private Boolean consentNotifikasi;
	private String obatRutin;
	private Integer intervalRefillHari;
	private Date tanggalRefillBerikut;
	private Date tanggalDaftar;
	private String keterangan;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Primary key auto-increment; lihat {@code @GeneratedValue(strategy = IDENTITY)}. */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** Lihat {@link #getId()}. Hanya dipakai Hibernate saat memuat baris (kolom {@code insertable = false}). */
	public void setId(Long id) { this.id = id; }

	/** Kode kartu anggota yang unik, mis. {@code "MEM-APT-<epoch-ms>"} (dibangkitkan {@code ApotikMembershipHelper.simpan()} saat baris baru) atau {@code "MEM-APT-UAT-####"} untuk data sampel. */
	@Column(name = "kode", unique = true, nullable = false, length = 60)
	public String getKode() { return kode; }
	/** Lihat {@link #getKode()}. Wajib unik di level DB (`unique = true`) — pemanggil bertanggung jawab menghasilkan nilai yang tak bertabrakan. */
	public void setKode(String kode) { this.kode = kode; }

	/** Pasien terkait bila anggota ini juga tercatat sebagai pasien rawat di modul SIRS; boleh {@code null} untuk pelanggan apotik murni yang belum pernah berobat. */
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "pasien")
	public Pasien getPasien() { pasien = check(pasien); return pasien; }
	/** Lihat {@link #getPasien()}. */
	public void setPasien(Pasien pasien) { this.pasien = pasien; }

	/** Nama tampilan anggota (disalin manual saat pendaftaran, bukan diturunkan otomatis dari {@link #getPasien()} — keduanya bisa berbeda/tak sinkron bila data pasien diubah belakangan). */
	@Column(name = "nama", nullable = false, length = 180)
	public String getNama() { return nama; }
	/** Lihat {@link #getNama()}. */
	public void setNama(String nama) { this.nama = nama; }

	/** Nomor telepon kontak (untuk notifikasi pengingat refill bila {@link #getConsentNotifikasi()} bernilai true). */
	@Column(name = "telepon", length = 60)
	public String getTelepon() { return telepon; }
	/** Lihat {@link #getTelepon()}. */
	public void setTelepon(String telepon) { this.telepon = telepon; }

	/** Tingkatan keanggotaan (mis. {@code REGULER}/{@code GOLD}/{@code PLATINUM}, kode bebas tanpa tabel master terpisah — nilainya cuma teks bebas yang divalidasi ulang manual di {@code ApotikMembershipHelper.simpan()} bukan enum/FK). Default {@code "REGULER"} bila kolom kosong. */
	@Column(name = "tier", length = 30)
	public String getTier() { return tier == null ? "REGULER" : tier; }
	/** Lihat {@link #getTier()}. */
	public void setTier(String tier) { this.tier = tier; }

	/** Saldo poin loyalitas TERKINI — proyeksi cepat, HARUS selalu sama dengan {@code saldoSetelah} pada baris {@link ApotikRewardLedger} TERBARU milik membership ini (dijaga manual oleh {@code ApotikMembershipHelper.mutasiPoin()}, bukan trigger DB/constraint). Default {@code 0} bila kolom kosong. */
	@Column(name = "poin_saldo", nullable = false)
	public Long getPoinSaldo() { return poinSaldo == null ? Long.valueOf(0) : poinSaldo; }
	/** Lihat {@link #getPoinSaldo()}. JANGAN dipanggil langsung tanpa turut menulis baris {@link ApotikRewardLedger} pendamping — keduanya harus berubah bersama dalam satu transaksi, kalau tidak saldo cache ini menyimpang dari riwayat ledger. */
	public void setPoinSaldo(Long poinSaldo) { this.poinSaldo = poinSaldo; }

	/** Status keanggotaan — salah satu {@link #AKTIF}/{@link #NONAKTIF}/{@link #DIBLOKIR} (divalidasi manual di {@code ApotikMembershipHelper.simpan()}, bukan enum Java/constraint DB). Default {@link #AKTIF} bila kolom kosong. */
	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status == null ? AKTIF : status; }
	/** Lihat {@link #getStatus()}. */
	public void setStatus(String status) { this.status = status; }

	/** Persetujuan anggota untuk menerima notifikasi (pengingat refill obat via telepon/SMS/WA — kanal pengiriman aktual ada di luar entity ini). Default {@code false} (opt-in, bukan opt-out) bila kolom kosong. */
	@Column(name = "consent_notifikasi", nullable = false)
	public Boolean getConsentNotifikasi() { return consentNotifikasi == null ? Boolean.FALSE : consentNotifikasi; }
	/** Lihat {@link #getConsentNotifikasi()}. */
	public void setConsentNotifikasi(Boolean consentNotifikasi) { this.consentNotifikasi = consentNotifikasi; }

	/** Nama obat rutin yang dikonsumsi anggota (teks bebas, dipakai {@code ApotikMembershipHelper} sebagai kata kunci pencarian membership juga) — bukan FK ke katalog obat, jadi rawan variasi ejaan/tak konsisten dengan master obat sesungguhnya. */
	@Column(name = "obat_rutin", length = 240)
	public String getObatRutin() { return obatRutin; }
	/** Lihat {@link #getObatRutin()}. */
	public void setObatRutin(String obatRutin) { this.obatRutin = obatRutin; }

	/** Interval hari antar-isi-ulang obat rutin, dipakai menghitung/menampilkan {@link #getTanggalRefillBerikut()} — TIDAK dihitung otomatis oleh entity ini, murni angka yang ditulis manual lewat {@code ApotikMembershipHelper.refill()}. Default {@code 0} bila kolom kosong. */
	@Column(name = "interval_refill_hari")
	public Integer getIntervalRefillHari() { return intervalRefillHari == null ? Integer.valueOf(0) : intervalRefillHari; }
	/** Lihat {@link #getIntervalRefillHari()}. */
	public void setIntervalRefillHari(Integer intervalRefillHari) { this.intervalRefillHari = intervalRefillHari; }

	/** Tanggal target isi ulang obat berikutnya (tanggal murni, tanpa komponen jam — lihat {@code @Temporal(DATE)}); dipakai sumber daftar "perlu dihubungi" pengingat refill. Boleh {@code null} bila belum ditentukan. */
	@Temporal(TemporalType.DATE) @Column(name = "tanggal_refill_berikut")
	public Date getTanggalRefillBerikut() { return tanggalRefillBerikut; }
	/** Lihat {@link #getTanggalRefillBerikut()}. */
	public void setTanggalRefillBerikut(Date tanggalRefillBerikut) { this.tanggalRefillBerikut = tanggalRefillBerikut; }

	/** Tanggal+jam pendaftaran anggota (diisi sekali saat baris dibuat di {@code ApotikMembershipHelper.simpan()}, tidak pernah diubah setelahnya oleh jalur resmi). */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_daftar", nullable = false)
	public Date getTanggalDaftar() { return tanggalDaftar; }
	/** Lihat {@link #getTanggalDaftar()}. */
	public void setTanggalDaftar(Date tanggalDaftar) { this.tanggalDaftar = tanggalDaftar; }

	/** Catatan bebas petugas (mis. alasan status {@link #DIBLOKIR}, atau penanda data sampel/UAT dari {@code provisionDemo()}). */
	@Column(name = "keterangan", length = 500)
	public String getKeterangan() { return keterangan; }
	/** Lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/** Nama tampilan pengguna yang terakhir menulis baris ini — field audit shadow standar AIS (lihat {@code GeneralValueObject}), dideklarasikan ulang di sini karena induk POJO bukan {@code @MappedSuperclass}. */
	@Column(name = "oleh", length = 200)
	public String getOleh() { return oleh; }
	/** Lihat {@link #getOleh()}. */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/** ID login pengguna yang terakhir menulis baris ini — field audit shadow, lihat {@link #getOleh()}. */
	@Column(name = "olehid", length = 200)
	public String getOlehId() { return olehId; }
	/** Lihat {@link #getOlehId()}. */
	public void setOlehId(String olehId) { this.olehId = olehId; }

	/** Stempel waktu perubahan terakhir, ditulis otomatis oleh {@link #onUpdate()} lewat {@code AuditTimestampInterceptor} — field audit shadow standar AIS. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** Lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	/** Callback siklus hidup JPA — menstempel {@link #getTanggal_dirubah()} otomatis sebelum setiap {@code UPDATE}, lewat {@code AuditTimestampInterceptor.ubah(this)}. */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}
}
