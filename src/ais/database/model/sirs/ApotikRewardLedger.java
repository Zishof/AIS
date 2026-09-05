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
 * Baris riwayat mutasi poin loyalitas apotik, tabel {@code sirs.apotik_reward_ledger} —
 * APPEND-ONLY (tidak pernah di-{@code UPDATE}/{@code DELETE} oleh jalur resmi manapun, hanya
 * {@code session.save()} baru per mutasi). Ini SUMBER KEBENARAN riwayat poin;
 * {@link ApotikCustomerMembership#getPoinSaldo()} pada entity induk hanyalah proyeksi cepat
 * (denormalized cache) dari {@link #getSaldoSetelah()} baris TERBARU milik satu membership.
 *
 * <p>Ditulis oleh {@code ais.action.servlet.api.ApotikMembershipHelper.mutasiPoin()} (perolehan
 * poin dari transaksi/penukaran hadiah, jenis {@code "PEROLEHAN"}/{@code "PENUKARAN"} — kode
 * bebas, bukan enum/FK ke katalog) dan {@code provisionDemo()} (baris saldo-awal untuk data
 * sampel/UAT, jenis {@code "PEROLEHAN"} dengan {@link #getReferensi()} {@code "SEED-UAT"}).</p>
 *
 * <p><b>Peringatan konkurensi</b> (lihat detail lengkap di javadoc kelas
 * {@link ApotikCustomerMembership}): penulisan baris ini SELALU didahului baca-hitung-tulis
 * `poin_saldo` pada membership induk TANPA row-lock/versi optimistik — dua mutasi bersamaan
 * pada membership yang sama berisiko menghasilkan dua baris ledger dengan
 * {@link #getSaldoSetelah()} yang SAMA-SAMA dihitung dari saldo lama yang sama (bukan
 * berurutan), sehingga rantai saldo ledger bisa tidak konsisten dengan urutan commit
 * sebenarnya. Dicatat sebagai temuan arsitektur, belum ditambal (kategori sama dengan bug
 * check-in ganda kamar hotel {@code task_b718f355}, subsistem berbeda).</p>
 *
 * @see ApotikCustomerMembership
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_reward_ledger")
public class ApotikRewardLedger extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private ApotikCustomerMembership membership;
	private String jenis;
	private Long poin;
	private Long saldoSetelah;
	private String referensi;
	private String keterangan;
	private Date waktu;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Primary key auto-increment; lihat {@code @GeneratedValue(strategy = IDENTITY)}. */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** Lihat {@link #getId()}. Hanya dipakai Hibernate saat memuat baris (kolom {@code insertable = false}). */
	public void setId(Long id) { this.id = id; }

	/** Membership pemilik baris mutasi ini; wajib diisi (`nullable = false`) — satu baris ledger tidak pernah lepas dari satu membership. */
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "membership", nullable = false)
	public ApotikCustomerMembership getMembership() { membership = check(membership); return membership; }
	/** Lihat {@link #getMembership()}. */
	public void setMembership(ApotikCustomerMembership membership) { this.membership = membership; }

	/** Jenis mutasi — kode bebas (bukan enum/FK), nilai yang benar-benar ditulis jalur resmi hanya {@code "PEROLEHAN"} (poin bertambah) dan {@code "PENUKARAN"} (poin berkurang, ditentukan dari tanda {@link #getPoin()}, bukan dari field ini sendiri). */
	@Column(name = "jenis", nullable = false, length = 30)
	public String getJenis() { return jenis; }
	/** Lihat {@link #getJenis()}. */
	public void setJenis(String jenis) { this.jenis = jenis; }

	/** Jumlah poin mutasi — BERTANDA (positif = perolehan, negatif = penukaran/pengurangan), bukan nilai absolut. Wajib diisi. */
	@Column(name = "poin", nullable = false)
	public Long getPoin() { return poin; }
	/** Lihat {@link #getPoin()}. */
	public void setPoin(Long poin) { this.poin = poin; }

	/** Saldo poin membership TEPAT SETELAH mutasi ini diterapkan (snapshot, bukan diturunkan/dihitung ulang saat dibaca) — dipakai merekonstruksi riwayat saldo tanpa perlu menjumlah ulang seluruh baris {@link #getPoin()} sebelumnya. Lihat peringatan konkurensi di javadoc kelas soal keandalan urutan snapshot ini. */
	@Column(name = "saldo_setelah", nullable = false)
	public Long getSaldoSetelah() { return saldoSetelah; }
	/** Lihat {@link #getSaldoSetelah()}. */
	public void setSaldoSetelah(Long saldoSetelah) { this.saldoSetelah = saldoSetelah; }

	/** Rujukan eksternal opsional ke transaksi pemicu mutasi (mis. nomor struk penjualan, atau {@code "SEED-UAT"} untuk baris data sampel) — teks bebas, bukan FK. */
	@Column(name = "referensi", length = 100)
	public String getReferensi() { return referensi; }
	/** Lihat {@link #getReferensi()}. */
	public void setReferensi(String referensi) { this.referensi = referensi; }

	/** Catatan bebas petugas untuk mutasi ini (mis. alasan penyesuaian manual). */
	@Column(name = "keterangan", length = 500)
	public String getKeterangan() { return keterangan; }
	/** Lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/** Tanggal+jam mutasi terjadi (diisi eksplisit oleh pemanggil saat `save()`, bukan otomatis dari stempel DB — berbeda dari {@link #getTanggal_dirubah()} audit shadow di bawah). */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	/** Lihat {@link #getWaktu()}. */
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	/** Nama tampilan pengguna yang mencatat mutasi ini — field audit shadow standar AIS (lihat {@code GeneralValueObject}), dideklarasikan ulang di sini karena induk POJO bukan {@code @MappedSuperclass}. */
	@Column(name = "oleh", length = 200)
	public String getOleh() { return oleh; }
	/** Lihat {@link #getOleh()}. */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/** ID login pengguna yang mencatat mutasi ini — field audit shadow, lihat {@link #getOleh()}. */
	@Column(name = "olehid", length = 200)
	public String getOlehId() { return olehId; }
	/** Lihat {@link #getOlehId()}. */
	public void setOlehId(String olehId) { this.olehId = olehId; }

	/** Stempel waktu perubahan terakhir baris ini, ditulis otomatis oleh {@link #onUpdate()} — field audit shadow standar AIS. Untuk entity append-only seperti ini biasanya hanya terisi sekali (saat insert), karena tidak ada jalur `UPDATE` resmi. */
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
