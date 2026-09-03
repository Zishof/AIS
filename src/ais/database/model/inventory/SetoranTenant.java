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
 * <h2>SetoranTenant — Setoran &amp; Bagi Hasil Tenant/Stan (Toko) untuk Toko/Kantin.</h2>
 *
 * <p>
 * Entity BARU untuk mencatat kewajiban dan setoran tiap <b>tenant/stan</b> (direpresentasikan oleh
 * {@link Toko}) secara periodik, sehingga tersedia laporan katalog §3.8: <i>Bagi Hasil Tenant</i>,
 * <i>Setoran Tenant</i>, dan <i>Tunggakan Tenant</i>. Sebelumnya {@code Toko} hanya menyimpan
 * nama/keterangan sehingga aspek komersial (komisi, sewa, setoran) belum bisa dilaporkan. Dengan
 * entity ini + pendaftaran di {@code hibernate.cfg.xml}, tabel {@code koperasi.setoran_tenant}
 * otomatis dibuat (hbm2ddl=update).
 * </p>
 *
 * <h3>Perhitungan</h3>
 * <ul>
 *   <li><b>Bagi hasil (komisi)</b> = {@code omzet} × {@code persenBagiHasil}% (disimpan pada
 *       {@code nilaiBagiHasil}; boleh diisi manual bila skema bukan persentase).</li>
 *   <li><b>Total kewajiban</b> = {@code nilaiBagiHasil} + {@code sewa} + {@code biayaLayanan}.</li>
 *   <li><b>Sisa/tunggakan</b> = total kewajiban − {@code setoran}. Bila &le; 0 dianggap LUNAS.</li>
 * </ul>
 *
 * <p>
 * Penamaan kolom mengikuti aturan proyek: field ber-@Column memakai nama eksplisit, field
 * numerik/tanggal tanpa @Column ter-<i>fold</i> menjadi huruf kecil tanpa underscore
 * (mis. {@code persenBagiHasil}→{@code persenbagihasil}, {@code nilaiBagiHasil}→{@code nilaibagihasil},
 * {@code biayaLayanan}→{@code biayalayanan}). Kompatibel Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul tenant)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "setoran_tenant")
public class SetoranTenant extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Kewajiban tenant sudah terpenuhi. */
	public static final String STATUS_LUNAS = "LUNAS";
	/** Masih ada kekurangan setoran (tunggakan). */
	public static final String STATUS_KURANG = "KURANG";

	/** Primary key baris {@code koperasi.setoran_tenant}. Lihat {@link #getId()}. */
	private Long id;
	/** Toko/tenant/lapak pemilik kewajiban setoran periode ini. Lihat {@link #getToko()}. */
	private Toko toko;
	/** Tanggal pencatatan baris ini. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Kode periode (mis. "2026-09") yang diwakili baris ini. Lihat {@link #getPeriode()}. */
	private String periode;
	/** Omzet tenant pada periode ini, dasar perhitungan bagi hasil. Lihat {@link #getOmzet()}. */
	private Double omzet;
	/** Persentase bagi hasil/komisi yang berlaku. Lihat {@link #getPersenBagiHasil()}. */
	private Double persenBagiHasil;
	/** Nilai bagi hasil hasil perhitungan (atau isian manual). Lihat {@link #getNilaiBagiHasil()}. */
	private Double nilaiBagiHasil;
	/** Komponen sewa lapak pada total kewajiban. Lihat {@link #getSewa()}. */
	private Double sewa;
	/** Komponen biaya layanan pada total kewajiban. Lihat {@link #getBiayaLayanan()}. */
	private Double biayaLayanan;
	/** Jumlah yang sudah disetor tenant untuk periode ini. Lihat {@link #getSetoran()}. */
	private Double setoran;
	/** Status LUNAS/KURANG hasil perbandingan setoran terhadap kewajiban. Lihat {@link #getStatus()}. */
	private String status;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/**
	 * Nama pengguna pengubah terakhir -- field <b>shadow</b> yang meng-override {@code oleh} milik
	 * {@link GeneralValueObject} (WAJIB dideklarasikan ulang per entity konkret karena
	 * {@code GeneralValueObject} sendiri abstrak/tidak dipetakan sebagai tabel -- lihat javadoc
	 * {@link GeneralValueObject#getOleh()}).
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir -- field shadow dengan alasan sama seperti {@link #oleh}. */
	private String olehId;

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
	 * Stempel waktu perubahan terakhir -- field shadow yang sama alasannya dengan {@link #oleh}.
	 * Diinisialisasi ke waktu pembuatan object sehingga baris baru selalu punya nilai walau jalur
	 * simpan lupa mengisinya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public SetoranTenant() {
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
	 * Mengembalikan {@link Toko} (tenant/lapak) pemilik kewajiban setoran ini, dengan proxy lazy
	 * diresolusi lewat {@link #check(Object)}. Berbeda dari {@link Pedagang#getToko()}, getter ini
	 * <b>tidak</b> memiliki fallback ke {@code Common.getCurrentToko()} -- bisa mengembalikan
	 * {@code null} apa adanya bila kolom belum terisi.
	 *
	 * @return toko/tenant terkait, atau {@code null} bila belum terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menyetel toko/tenant pemilik baris ini. Tanpa validasi.
	 *
	 * @param toko toko baru, boleh {@code null}
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Mengembalikan tanggal pencatatan baris ini, dengan default waktu SEKARANG bila kolom kosong
	 * (dihitung ulang setiap pemanggilan, bukan disimpan balik ke field -- berbeda dari pola
	 * "getter destruktif" yang meng-assign hasil default ke field-nya sendiri).
	 *
	 * @return tanggal pencatatan, tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyetel tanggal pencatatan. Tanpa validasi.
	 *
	 * @param tanggal tanggal baru, boleh {@code null} (lihat {@link #getTanggal()} untuk fallback)
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan kode periode (mis. {@code "2026-09"}) yang diwakili baris ini. Tanpa default
	 * atau normalisasi format -- format string periode sepenuhnya ditentukan pemanggil
	 * ({@code TenantSetoranUtil}).
	 *
	 * @return kode periode, boleh {@code null}
	 */
	public String getPeriode() {
		return periode;
	}

	/**
	 * Menyetel kode periode. Tanpa validasi.
	 *
	 * @param periode kode periode baru, boleh {@code null}
	 */
	public void setPeriode(String periode) {
		this.periode = periode;
	}

	/**
	 * Mengembalikan omzet tenant pada periode ini -- dasar perhitungan
	 * {@link #getNilaiBagiHasil() bagi hasil} (lihat rumus lengkap pada javadoc kelas).
	 *
	 * @return omzet, {@code 0.0} bila belum diisi (bukan {@code null})
	 */
	public Double getOmzet() {
		return omzet == null ? 0.0 : omzet;
	}

	/**
	 * Menyetel omzet periode ini. Tanpa validasi (termasuk tidak menolak nilai negatif).
	 *
	 * @param omzet omzet baru, boleh {@code null}
	 */
	public void setOmzet(Double omzet) {
		this.omzet = omzet;
	}

	/**
	 * Mengembalikan persentase bagi hasil/komisi yang berlaku untuk periode ini.
	 *
	 * @return persentase bagi hasil, {@code 0.0} bila belum diisi
	 */
	public Double getPersenBagiHasil() {
		return persenBagiHasil == null ? 0.0 : persenBagiHasil;
	}

	/**
	 * Menyetel persentase bagi hasil. Tanpa validasi rentang (0-100 tidak dipaksakan di sini).
	 *
	 * @param persenBagiHasil persentase baru, boleh {@code null}
	 */
	public void setPersenBagiHasil(Double persenBagiHasil) {
		this.persenBagiHasil = persenBagiHasil;
	}

	/**
	 * Mengembalikan nilai bagi hasil (komisi) hasil perhitungan {@code omzet × persenBagiHasil}
	 * (dihitung oleh {@code TenantSetoranUtil}, BUKAN oleh getter ini -- entity ini murni
	 * penyimpan hasil) atau isian manual bila skema tenant bukan persentase. Lihat rumus lengkap
	 * pada javadoc kelas ({@code "Total kewajiban" = nilaiBagiHasil + sewa + biayaLayanan}).
	 *
	 * @return nilai bagi hasil, {@code 0.0} bila belum diisi
	 */
	public Double getNilaiBagiHasil() {
		return nilaiBagiHasil == null ? 0.0 : nilaiBagiHasil;
	}

	/**
	 * Menyetel nilai bagi hasil. Tanpa validasi -- boleh diisi manual, tidak dipaksa selalu hasil
	 * kali {@link #getOmzet()} dan {@link #getPersenBagiHasil()}.
	 *
	 * @param nilaiBagiHasil nilai baru, boleh {@code null}
	 */
	public void setNilaiBagiHasil(Double nilaiBagiHasil) {
		this.nilaiBagiHasil = nilaiBagiHasil;
	}

	/**
	 * Mengembalikan komponen sewa lapak pada total kewajiban periode ini.
	 *
	 * @return nilai sewa, {@code 0.0} bila belum diisi
	 */
	public Double getSewa() {
		return sewa == null ? 0.0 : sewa;
	}

	/**
	 * Menyetel komponen sewa. Tanpa validasi.
	 *
	 * @param sewa nilai sewa baru, boleh {@code null}
	 */
	public void setSewa(Double sewa) {
		this.sewa = sewa;
	}

	/**
	 * Mengembalikan komponen biaya layanan pada total kewajiban periode ini.
	 *
	 * @return nilai biaya layanan, {@code 0.0} bila belum diisi
	 */
	public Double getBiayaLayanan() {
		return biayaLayanan == null ? 0.0 : biayaLayanan;
	}

	/**
	 * Menyetel komponen biaya layanan. Tanpa validasi.
	 *
	 * @param biayaLayanan nilai baru, boleh {@code null}
	 */
	public void setBiayaLayanan(Double biayaLayanan) {
		this.biayaLayanan = biayaLayanan;
	}

	/**
	 * Mengembalikan jumlah yang sudah disetor tenant untuk periode ini -- dikurangkan terhadap
	 * total kewajiban untuk menentukan {@link #getStatus() status LUNAS/KURANG} (lihat rumus
	 * lengkap pada javadoc kelas).
	 *
	 * @return jumlah setoran, {@code 0.0} bila belum diisi
	 */
	public Double getSetoran() {
		return setoran == null ? 0.0 : setoran;
	}

	/**
	 * Menyetel jumlah setoran. Tanpa validasi -- tidak otomatis menyinkronkan
	 * {@link #getStatus()}; pemanggil ({@code TenantSetoranUtil}) bertanggung jawab menghitung
	 * ulang status setelah mengubah nilai ini.
	 *
	 * @param setoran jumlah setoran baru, boleh {@code null}
	 */
	public void setSetoran(Double setoran) {
		this.setoran = setoran;
	}

	/**
	 * Mengembalikan status kewajiban periode ini -- {@link #STATUS_LUNAS} atau
	 * {@link #STATUS_KURANG}, dengan <b>default {@link #STATUS_KURANG}</b> bila kolom belum
	 * terisi (bukan {@code null} maupun LUNAS -- baris baru yang belum sempat dihitung ulang
	 * dianggap masih ada tunggakan sampai dibuktikan lunas, sisi aman untuk laporan tunggakan).
	 *
	 * @return status kewajiban, tidak pernah {@code null}
	 */
	public String getStatus() {
		return status == null ? STATUS_KURANG : status;
	}

	/**
	 * Menyetel status kewajiban secara langsung. Tanpa validasi terhadap nilai
	 * {@link #getSetoran()}/kewajiban aktual -- pemanggil bertanggung jawab menjaga konsistensi
	 * (nilai ini murni disimpan apa adanya, tidak dihitung ulang oleh setter).
	 *
	 * @param status status baru, sebaiknya {@link #STATUS_LUNAS} atau {@link #STATUS_KURANG}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
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
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. <b>Berbeda dari {@link Pedagang#setOleh(String)}
	 * dan {@link GeneralValueObject#setOleh(String)}</b>: setter ini TIDAK menolak nilai
	 * {@code null}/kosong -- langsung menimpa field apa adanya. Perhatikan bila menyalin pola dari
	 * entity lain di paket ini yang justru mengabaikan nilai kosong demi menjaga jejak audit.
	 *
	 * @param oleh nama pengguna pengubah baru, boleh {@code null}/kosong (langsung menimpa)
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Sama seperti {@link #setOleh(String)}: TIDAK ada
	 * validasi/penolakan nilai kosong di kelas ini (berbeda dari {@link GeneralValueObject}).
	 *
	 * @param olehId id pengguna pengubah baru, boleh {@code null}/kosong (langsung menimpa)
	 */
	public void setOlehId(String olehId) {
		this.olehId = olehId;
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
