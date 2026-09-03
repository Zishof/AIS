package ais.database.model.koperasi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Master meja kantin/toko koperasi -- daftar meja yang dapat dipilih kasir untuk transaksi
 * <i>dine-in</i> (makan di tempat), mis. saat mencatat pesanan agar dapat diantar ke meja yang
 * tepat. Entity referensi murni (kode/nama/keterangan/aktif); tidak menyimpan status okupansi
 * meja saat ini -- status "meja sedang dipakai" (bila ada) dikelola pada transaksi/pesanan yang
 * menunjuk ke sini, bukan pada baris master ini.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "meja_kantin")
public class MejaKantin extends GeneralValueObject {

	/** Versi serialisasi tetap untuk kompatibilitas antar-build. */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (audit shadow) yang terakhir menyimpan/mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId id pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *               dipertahankan) -- pola field audit shadow yang sama dipakai entity lain di
	 *               paket koperasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * @param oleh nama pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *             dipertahankan), sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna (audit shadow) yang terakhir menyimpan/mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan {@link #tanggal_dirubah} (dan field
	 * audit sejenis) ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}. Dipanggil
	 * otomatis oleh provider JPA setiap {@code UPDATE}, tidak untuk dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir (biasanya tidak diset manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu baris terakhir diubah; diperbarui otomatis lewat {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk log/debug: {@code id-nama}. */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;

	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor bawaan (dipakai JPA/Hibernate dan layar master {@code MejaKantinAction}). */
	public MejaKantin() {
	}

	/** @return id baris (identity, dibuat DB). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; biasanya tidak diset manual, dibuat DB saat {@code save}. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode meja (unik). Nilai {@code null}/kosong/hanya-spasi dinormalisasi menjadi
	 *         {@code null}; selain itu ditrim.
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/** @param kode kode meja (unik, wajib diisi). */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama/label meja untuk ditampilkan ke kasir; ditrim, atau {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/label meja. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan bebas terkait meja ini (opsional, mis. lokasi/kapasitas). */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan bebas terkait meja ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return status aktif meja. Fallback ke {@code true} bila kolom {@code null}. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif meja; meja nonaktif tidak lagi dipilihkan di transaksi baru
	 *               tapi tetap tampak di riwayat pesanan lama yang sudah memakainya. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
