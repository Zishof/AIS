package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Pencacah Nomor Induk Siswa (NIS) yang DIPERSISTENSIKAN per <b>(sekolah, tahun, epoch)</b> &mdash;
 * tabel {@code sekolah.nis_counter}. Entity ini adalah perbaikan mode &quot;hitung otomatis&quot;
 * pada {@link FormatNis} ({@code gunakanIndexUrut = false}): sebelumnya nomor urut dihitung ulang
 * dari {@code rowCount} pendaftar {@link CalonSiswa} (lihat javadoc lama
 * {@code ais.common.CommonPSB#getindex}), yang terbukti menghasilkan NIS kembar deterministik dan
 * tidak terisolasi per sekolah. Baris tabel ini menggantikan penghitungan ulang itu dengan satu
 * angka yang dinaikkan atomik satu per satu, dikunci lewat
 * {@code ais.database.hibernate.KunciEntityHelper#jalankanDenganKunci(Class, java.io.Serializable,
 * KunciEntityHelper.PekerjaanTransaksi)} (kunci baris {@code FOR NO KEY UPDATE NOWAIT} + retry,
 * aman lintas node aplikasi) sehingga dua pembangkitan NIS bersamaan tidak dapat menerima nomor
 * yang sama.
 *
 * <h3>Kunci baris: (sekolah, tahun, epoch)</h3>
 * <ul>
 * <li>{@code sekolah} &mdash; wajib, satu baris pencacah per sekolah. Menutup kebocoran volume
 * pendaftaran antar tenant yang ada pada mekanisme lama.</li>
 * <li>{@code tahun} &mdash; tahun masuk calon siswa bila {@code FormatNis.getResetUrutanTiapTahun()}
 * aktif, atau sentinel {@code 0} ketika nomor urut TIDAK direset tiap tahun (satu pencacah tunggal
 * sepanjang umur sekolah).</li>
 * <li>{@code epoch} &mdash; sentinel {@code "AWAL"} secara bawaan, atau tanggal
 * {@code FormatNis.getResetTiap()} (format {@code yyyyMMdd}) begitu tanggal reset kustom itu
 * terlampaui. Karena {@code resetTiap} adalah tanggal tunggal (bukan pola berulang), sekali
 * terlampaui pencacah pindah permanen ke baris/epoch baru yang dimulai dari nol &mdash; meniru
 * niat &quot;reset sekali pada tanggal tersebut&quot; dari mekanisme lama tanpa mengorbankan
 * ketertelusuran pencacah sebelumnya.</li>
 * </ul>
 *
 * <p><b>Bukan {@code @Audited}, dan itu disengaja.</b> Baris ini murni pencacah teknis, bukan
 * data bisnis yang perlu riwayat perubahan; menambah anotasi Envers berarti juga wajib menyiapkan
 * tabel {@code new_audit.nis_counter__audit} secara manual (lihat catatan {@code hbm2ddl.auto}
 * pada {@code hibernate.cfg.xml}) tanpa manfaat nyata untuk sebuah angka pencacah.</p>
 *
 * @see FormatNis
 * @see ais.common.CommonPSB
 * @see ais.database.hibernate.KunciEntityHelper
 */
@Entity
@Table(schema = "sekolah", name = "nis_counter",
		uniqueConstraints = @UniqueConstraint(name = "uq_nis_counter_sekolah_tahun_epoch",
				columnNames = { "sekolah_id", "tahun", "epoch" }))
public class NisCounter {

	private Long id;
	private Sekolah sekolah;
	private Integer tahun;
	private String epoch;
	private Long nilai;

	/**
	 * Sentinel {@code tahun} ketika {@code FormatNis.getResetUrutanTiapTahun()} tidak aktif
	 * (pencacah tunggal, tidak direset tiap tahun).
	 */
	public static final int TAHUN_TANPA_RESET = 0;

	/**
	 * Sentinel {@code epoch} sebelum tanggal {@code FormatNis.getResetTiap()} (bila ada) terlampaui,
	 * atau ketika {@code FormatNis} tidak memakai tanggal reset kustom sama sekali.
	 */
	public static final String EPOCH_AWAL = "AWAL";

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Sekolah pemilik pencacah ini; wajib diisi (satu baris pencacah tidak pernah lintas sekolah). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		return sekolah;
	}

	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah;
	}

	/** Tahun masuk yang menjadi lingkup pencacah ini, atau {@link #TAHUN_TANPA_RESET}. */
	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		return tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/** Penanda epoch reset kustom; lihat penjelasan pada javadoc kelas. */
	@Column(name = "epoch", nullable = false, length = 32)
	public String getEpoch() {
		return epoch;
	}

	public void setEpoch(String epoch) {
		this.epoch = epoch;
	}

	/**
	 * Nilai pencacah saat ini &mdash; nomor urut NIS TERAKHIR yang sudah diterbitkan pada lingkup
	 * (sekolah, tahun, epoch) ini. Nomor urut BERIKUTNYA adalah nilai ini ditambah satu; kenaikan
	 * dilakukan atomik oleh pemanggil di dalam blok terkunci
	 * {@code KunciEntityHelper.jalankanDenganKunci(NisCounter.class, id, ...)}.
	 */
	@Column(name = "nilai", nullable = false)
	public Long getNilai() {
		return nilai == null ? 0L : nilai;
	}

	public void setNilai(Long nilai) {
		this.nilai = nilai;
	}
}
