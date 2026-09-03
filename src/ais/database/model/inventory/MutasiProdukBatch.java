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
 * Buku besar (ledger) perubahan saldo satu {@link ProdukBatch} -- setiap kali saldo sebuah lot
 * berubah (kulakan/penerimaan, penjualan FEFO, opname, transfer antar-outlet, pembatalan,
 * karantina/lepas QC, dsb.), satu baris ditulis ke sini lewat {@code KantinHelper.catatMutasiBatch}
 * sebagai jejak audit lot yang tidak pernah dihapus/diubah (append-only, sama semangatnya dengan
 * {@link MutasiStokProduksi} pada mesin produksi WO).
 *
 * <p><b>Field audit shadow, bukan bug.</b> {@link #getSaldo()} adalah SNAPSHOT saldo
 * {@link ProdukBatch#getStok()} SETELAH efek {@link #getMasuk()}/{@link #getKeluar()} baris ini
 * diterapkan -- {@code catatMutasiBatch} SELALU dipanggil SETELAH pemanggilnya memperbarui
 * {@code batch.setStok(...)}, lalu membaca {@code batch.getStok()} apa adanya ke kolom ini. Nilai
 * ini karenanya adalah DUPLIKASI terhadap saldo batch pada waktu penulisan, bukan sumber kebenaran
 * -- kelas ini sendiri tidak pernah menghitung ulang saldo dari riwayat masuk/keluarnya sendiri.
 * Ini KEHARUSAN TEKNIS pola ledger snapshot-per-baris di seluruh domain finansial/stok AIS
 * (memudahkan laporan "saldo pada tanggal X" tanpa menjumlah ulang seluruh riwayat), BUKAN cacat
 * desain.</p>
 *
 * <p><b>Penjaga keseimbangan (TIDAK ditegakkan model/database).</b> Tidak ada constraint yang
 * memeriksa {@code saldo == saldo_baris_sebelumnya + masuk - keluar} untuk baris berurutan pada
 * {@code batch} yang sama -- bila pemanggil (mis. penambahan {@code catatMutasiBatch} baru di
 * masa depan) menulis {@code saldo} yang tidak konsisten dengan {@code masuk}/{@code keluar} atau
 * urutan waktu, ledger ini akan menyimpannya tanpa error. Kebenaran kolom {@code saldo} sepenuhnya
 * bergantung pada disiplin seluruh pemanggil {@code catatMutasiBatch} menyetel
 * {@code batch.stok} yang benar SEBELUM memanggilnya -- pola soft-check aplikasi yang sudah
 * tercatat berulang pada ledger-ledger sejenis di paket ini (lihat javadoc
 * {@link MutasiStokProduksi#getQtyMasuk()}); dicatat di sini sebagai referensi audit, bukan
 * temuan baru.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "mutasi_produk_batch")
public class MutasiProdukBatch extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	private Long id;
	private ProdukBatch batch;
	private Date waktu;
	private String jenis;
	private Double masuk;
	private Double keluar;
	private Double saldo;
	private String referensi;
	private String keterangan;
	private String oleh;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Primary key baris mutasi ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	/** Setter {@link #getId()} -- normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
	public void setId(Long id) { this.id = id; }

	/**
	 * Lot/batch produk yang saldo-nya berubah pada baris ini, wajib. Getter memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi proxy lazy yang
	 * mungkin sudah <i>detached</i> dari session asalnya (pola getter relasi standar di seluruh
	 * entity AIS) sebelum mengembalikan field.
	 * @return batch pemilik baris mutasi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "batch", nullable = false)
	public ProdukBatch getBatch() { batch = check(batch); return batch; }
	/** Setter {@link #getBatch()}. */
	public void setBatch(ProdukBatch batch) { this.batch = batch; }

	/**
	 * Waktu baris mutasi ini dicatat. Getter null-safe: {@code null} dibaca sebagai waktu saat
	 * getter dipanggil ({@code WaktuUtil.getDate()}) -- BUKAN dibekukan sekali seperti
	 * {@link #tanggal_dirubah}; tiap panggilan getter pada instance baru yang field-nya belum
	 * diisi bisa mengembalikan nilai berbeda.
	 * @return waktu baris mutasi dicatat, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false)
	public Date getWaktu() { return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu; }
	/** Setter {@link #getWaktu()}. */
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	/**
	 * Jenis/kode peristiwa yang memicu mutasi ini (mis. {@code KULAKAN}, {@code PENJUALAN},
	 * {@code OPNAME_BATCH}, {@code MUTASI_MASUK}/{@code MUTASI_KELUAR} transfer antar outlet,
	 * {@code PEMBATALAN}, {@code QC_KARANTINA}, {@code QC_LEPAS}) -- teks bebas yang ditentukan
	 * oleh pemanggil {@code KantinHelper.catatMutasiBatch}, TIDAK dibatasi enum/konstanta pada
	 * kelas ini.
	 * @return jenis peristiwa, atau {@code null} bila tidak diisi.
	 */
	public String getJenis() { return jenis; }
	/** Setter {@link #getJenis()}. */
	public void setJenis(String jenis) { this.jenis = jenis; }

	/**
	 * Kuantitas yang MASUK ke saldo batch pada peristiwa ini (mis. penerimaan kulakan, batal
	 * penjualan, transfer masuk). Getter null-safe: {@code null} dibaca sebagai {@code 0.0}.
	 * Baris tertentu lazimnya hanya mengisi salah satu dari {@link #getMasuk()}/{@link #getKeluar()}
	 * (arah tunggal per baris), tapi kelas ini TIDAK memvalidasi/menegakkan eksklusivitas itu.
	 * @return kuantitas masuk pada baris ini, tidak pernah {@code null}.
	 */
	public Double getMasuk() { return masuk == null ? 0.0 : masuk; }
	/** Setter {@link #getMasuk()}. */
	public void setMasuk(Double masuk) { this.masuk = masuk; }

	/**
	 * Kuantitas yang KELUAR dari saldo batch pada peristiwa ini (mis. penjualan FEFO, transfer
	 * keluar, opname selisih negatif). Getter null-safe: {@code null} dibaca sebagai {@code 0.0}.
	 * Lihat javadoc {@link #getMasuk()} soal arah tunggal per baris yang tidak ditegakkan model.
	 * @return kuantitas keluar pada baris ini, tidak pernah {@code null}.
	 */
	public Double getKeluar() { return keluar == null ? 0.0 : keluar; }
	/** Setter {@link #getKeluar()}. */
	public void setKeluar(Double keluar) { this.keluar = keluar; }

	/**
	 * Saldo {@link ProdukBatch#getStok()} SETELAH baris mutasi ini diterapkan -- field audit shadow
	 * (snapshot), lihat pembahasan lengkap di javadoc kelas. Getter null-safe: {@code null} dibaca
	 * sebagai {@code 0.0}.
	 * @return saldo batch pada waktu baris ini dicatat, tidak pernah {@code null}.
	 */
	public Double getSaldo() { return saldo == null ? 0.0 : saldo; }
	/** Setter {@link #getSaldo()} -- lihat javadoc kelas soal ketiadaan penjaga konsistensi saldo/masuk/keluar. */
	public void setSaldo(Double saldo) { this.saldo = saldo; }

	/**
	 * Rujukan bebas teks ke dokumen/transaksi pemicu (mis. nomor faktur kulakan, id penjualan,
	 * {@code "BATCH-"+id} opname, nomor referensi transfer/pembatalan) -- tanpa foreign key, murni
	 * jejak tampilan/telusur manual.
	 * @return rujukan dokumen pemicu, atau {@code null} bila tidak diisi.
	 */
	public String getReferensi() { return referensi; }
	/** Setter {@link #getReferensi()}. */
	public void setReferensi(String referensi) { this.referensi = referensi; }

	/**
	 * Catatan bebas tentang baris mutasi ini (mis. alasan opname/pembatalan/karantina).
	 * @return catatan, atau {@code null} bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() { return keterangan; }
	/** Setter {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Nama/identitas petugas atau proses (mis. {@code "SYSTEM"} untuk mutasi otomatis QC) yang
	 * memicu baris mutasi ini -- jejak audit tampilan, bebas teks, tanpa validasi null/kosong pada
	 * setter.
	 * @return identitas pemicu, atau {@code null} bila tidak diisi.
	 */
	public String getOleh() { return oleh; }
	/** Setter {@link #getOleh()} -- tanpa penjaga null/kosong. */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/**
	 * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
	 * field, dan diperbarui otomatis oleh {@link #onUpdate()} setiap {@code UPDATE}. Karena kelas
	 * ini ber-{@code @Audited} (Envers), setiap perubahan pada baris (termasuk field ini) juga
	 * terekam sebagai revisi terpisah pada tabel shadow envers -- baris ledger sendiri normalnya
	 * hanya ditulis sekali (insert-only) dan tidak pernah di-{@code UPDATE} dalam alur bisnis biasa.
	 * @return waktu baris ini terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** Setter {@link #getTanggal_dirubah()} -- normalnya hanya dipanggil {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
