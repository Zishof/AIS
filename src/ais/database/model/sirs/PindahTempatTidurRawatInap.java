package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity JPA/Hibernate untuk satu transaksi pindah kelas/bed rawat inap pada modul SIRS (Sistem
 * Informasi Rumah Sakit), dipetakan ke tabel {@code sirs.pindah_tempat_tidur_rawat_inap}. Baris
 * ini murni catatan riwayat: dua referensi ke {@link Pendaftaran} — {@link #getPendaftaranDari()}
 * (pendaftaran asal yang ditinggalkan) dan {@link #getPendaftaranKe()} (pendaftaran baru di
 * kelas/bed tujuan) — beserta {@link #getWaktu()} kapan perpindahan terjadi. Class ini sendiri
 * TIDAK memiliki method bisnis (bukan bug — pola generik AIS memisahkan orkestrasi transaksional
 * ke lapisan action/UI, lihat di bawah).
 *
 * <h2>Orkestrasi perpindahan sesungguhnya dilakukan {@code PindahTempatTidurRawatInapAction}</h2>
 * <p>
 * Verifikasi dari {@code ais.action.master.sirs.PindahTempatTidurRawatInapAction#onSave(Event)}
 * (per permintaan investigasi apakah pemindahan bed ikut menutup/memperbarui status bed lama
 * dengan benar, atau punya bug tersendiri serupa {@code task_d82932ef}) menunjukkan bahwa alur
 * simpan MEMANG secara eksplisit:
 * </p>
 * <ol>
 *   <li>Meng-clone {@link Pendaftaran} asal menjadi {@link Pendaftaran} baru (bila
 *   {@code pendaftaranKe} belum ada) dengan {@code tempatTidur}, {@code kelasPerawatan},
 *   {@code ruangPerawatan}, {@code kamarPerawatan} baru sesuai pilihan pengguna, lalu
 *   menyimpannya sebagai {@link #getPendaftaranKe()}.</li>
 *   <li>Menandai {@link Pendaftaran} asal ({@link #getPendaftaranDari()}) sebagai
 *   {@code Pendaftaran.PINDAH} beserta {@code tanggalKeluar}, lalu membuat/memutakhirkan satu
 *   baris {@link DataPasienKeluar} untuk pendaftaran asal itu dengan
 *   {@code statusPulang = ConstantValues.STATUS_PINDAH}.</li>
 *   <li>Memanggil {@code TempatTidur.updateTerisi()} DUA KALI: sekali untuk
 *   {@code pendaftaranDari.getTempatTidur()} (bed lama) dan sekali untuk
 *   {@code pendaftaranKe.getTempatTidur()} (bed baru) — jadi kedua bed memang disegarkan pada
 *   saat transaksi pindah terjadi, tidak dibiarkan basi begitu saja.</li>
 * </ol>
 * <p><b>Namun ini justru memicu {@code task_d82932ef} secara konkret untuk bed lama, bukan
 * menghindarinya.</b> {@link Pendaftaran} asal tetap menyimpan {@code tempatTidur} yang menunjuk
 * ke bed lama SELAMANYA (tidak pernah di-null-kan oleh alur ini maupun alur lain mana pun yang
 * ditemukan), dan baris {@link DataPasienKeluar} berstatus {@code STATUS_PINDAH} yang baru dibuat
 * di atas ikut permanen terhubung ke bed lama itu lewat alias {@code pendaftaran.tempatTidur}.
 * Akibatnya, begitu {@code onSave(Event)} ini pernah dijalankan sekali untuk sebuah bed, PANGGILAN
 * {@link TempatTidur#updateTerisi()} BERIKUTNYA APA PUN — untuk pasien siapa pun yang kelak
 * ditempatkan di bed itu — akan selalu jatuh ke cabang {@code pasienKeluar > 0} dan menghasilkan
 * {@code terisi = false}, walau bed tersebut benar-benar sedang ditempati pasien baru yang aktif.
 * Nilai {@code terisi = false} yang dihasilkan TEPAT SETELAH transaksi pindah ini sendiri memang
 * benar (bed lama memang baru saja dikosongkan) — masalahnya baru muncul pada siklus pemakaian
 * bed itu berikutnya. Lihat javadoc {@link TempatTidur#updateTerisi()} untuk rincian mekanisme
 * bug lengkap; javadoc ini hanya mendokumentasikan bahwa transaksi pindah bed adalah salah satu
 * jalur konkret yang menciptakan kondisi pemicunya, bukan task baru terpisah.</p>
 *
 * <p><b>Penghapusan (undo):</b> {@code onDelete(...)} pada action yang sama membatalkan transaksi
 * pindah dengan mengembalikan {@link Pendaftaran} asal ke status {@code Pendaftaran.TERDAFTAR}
 * (menghapus {@code tanggalKeluar}), men-set {@code TempatTidur.terisi = true} LANGSUNG pada bed
 * asal (bukan lewat {@link TempatTidur#updateTerisi()} — assignment langsung, lihat javadoc
 * method itu untuk pola campuran assign-langsung vs recompute), lalu menghapus baris
 * {@link PindahTempatTidurRawatInap} dan {@link Pendaftaran} tujuan. Tidak ditemukan penanganan
 * eksplisit untuk mengembalikan {@code terisi} bed TUJUAN ke {@code false} pada alur undo ini.</p>
 *
 * <p>Class ini adalah entity hbm2java standar: kedua relasi {@code @ManyToOne} memakai
 * {@code FetchMode.SELECT} eksplisit (bukan default {@code JOIN}) sehingga masing-masing
 * {@link Pendaftaran} dimuat lewat query {@code SELECT} terpisah saat diakses, bukan digabung ke
 * query utama lewat {@code JOIN}. Field audit {@link #oleh}/{@link #olehId}/
 * {@link #tanggal_dirubah} adalah shadow field standar AIS (diisi
 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}) — KEHARUSAN TEKNIS pola audit
 * aplikasi, bukan bug. Modul {@code sirs} tidak memiliki sumbu tenant/satuan-kerja sama sekali
 * (dikonfirmasi berulang kali pada audit sebelumnya, {@code task_90bbdd51}).</p>
 *
 * @see Pendaftaran pendaftaran rawat inap asal maupun tujuan perpindahan
 * @see TempatTidur tempat tidur yang okupansinya dipengaruhi transaksi ini
 * @see DataPasienKeluar baris riwayat kepulangan/pindah yang dibuat bersamaan dengan transaksi ini
 * @see ais.action.master.sirs.PindahTempatTidurRawatInapAction orkestrasi simpan/hapus transaksi pindah bed
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pindah_tempat_tidur_rawat_inap")
public class PindahTempatTidurRawatInap extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.pindah_tempat_tidur_rawat_inap}. Lihat {@link #getId()}. */
	private Long id;
	/** Identifier pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identifier pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identifier pengguna yang terakhir mengubah baris ini. Nilai kosong/blank
	 * sengaja DIABAIKAN (bukan di-set menjadi kosong) agar jejak audit sebelumnya tidak
	 * tertimpa oleh pemanggilan yang tidak membawa identitas pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/**
	 * Representasi string dari transaksi pindah ini, dipakai komponen ZK (combobox/label) yang
	 * memanggil {@code toString()} secara implisit.
	 *
	 * @return {@link #keterangan} apa adanya (tanpa null-check eksplisit — akan mengembalikan
	 *         {@code null} bila {@link #keterangan} belum diisi); CATATAN: berbeda dari
	 *         kebanyakan entity {@code sirs} lain yang mengembalikan {@code nama}, class ini
	 *         tidak memiliki field {@code nama} sama sekali
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menetapkan nama pengguna yang terakhir mengubah baris ini. Nilai kosong/blank sengaja
	 * DIABAIKAN, simetris dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan {@link #tanggal_dirubah}
	 * setiap kali baris ini di-{@code UPDATE}. Pola shadow-audit-field standar AIS.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara manual. Dalam alur normal nilai ini
	 * dimutakhirkan otomatis oleh {@link #onUpdate()}; setter ini dipakai bila pemanggil perlu
	 * memaksa nilai tertentu (mis. saat memuat data hasil migrasi).
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal/jam perubahan terakhir; default konstruksi objek adalah waktu objek
	 *         dibuat di memori, sebelum baris pernah tersimpan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan bebas tentang transaksi pindah ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Pendaftaran rawat inap ASAL yang ditinggalkan. Lihat {@link #getPendaftaranDari()}. */
	private Pendaftaran pendaftaranDari;
	/** Pendaftaran rawat inap baru di kelas/bed TUJUAN. Lihat {@link #getPendaftaranKe()}. */
	private Pendaftaran pendaftaranKe;
	/** Waktu perpindahan terjadi, default saat objek dibuat. Lihat {@link #getWaktu()}. */
	private Date waktu = new Date();

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public PindahTempatTidurRawatInap() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID transaksi pindah, atau {@code null} untuk instance yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris ini. Kolom bertanda {@code insertable = false} pada
	 * pemetaan — nilai sesungguhnya berasal dari {@code IDENTITY} basis data saat
	 * {@code INSERT}, sehingga setter ini biasanya hanya relevan untuk memuat ulang entity yang
	 * sudah memiliki ID.
	 *
	 * @param id ID baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas tentang transaksi pindah ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang transaksi pindah ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan pendaftaran rawat inap ASAL (yang ditinggalkan) dari transaksi pindah ini.
	 * Relasi memakai {@code FetchMode.SELECT} eksplisit sehingga dimuat lewat query terpisah,
	 * bukan {@code JOIN} pada query utama.
	 *
	 * @return pendaftaran asal, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pendaftaran_dari", nullable = true)
	public Pendaftaran getPendaftaranDari() {
		return pendaftaranDari;
	}

	/**
	 * Menetapkan pendaftaran rawat inap ASAL (yang ditinggalkan) dari transaksi pindah ini.
	 *
	 * @param pendaftaranDari pendaftaran asal baru, boleh {@code null}
	 */
	public void setPendaftaranDari(Pendaftaran pendaftaranDari) {
		this.pendaftaranDari = pendaftaranDari;
	}

	/**
	 * Mengembalikan pendaftaran rawat inap baru di kelas/bed TUJUAN dari transaksi pindah ini.
	 * Relasi memakai {@code FetchMode.SELECT} eksplisit sehingga dimuat lewat query terpisah,
	 * bukan {@code JOIN} pada query utama.
	 *
	 * @return pendaftaran tujuan, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pendaftaran_ke", nullable = true)
	public Pendaftaran getPendaftaranKe() {
		return pendaftaranKe;
	}

	/**
	 * Menetapkan pendaftaran rawat inap baru di kelas/bed TUJUAN dari transaksi pindah ini.
	 *
	 * @param pendaftaranKe pendaftaran tujuan baru, boleh {@code null}
	 */
	public void setPendaftaranKe(Pendaftaran pendaftaranKe) {
		this.pendaftaranKe = pendaftaranKe;
	}

	/**
	 * Mengembalikan waktu perpindahan terjadi.
	 *
	 * @return waktu pindah; default konstruksi objek adalah waktu objek dibuat di memori,
	 *         sebelum baris pernah tersimpan (biasanya ditimpa nilai yang dipilih pengguna
	 *         lewat {@code MyDatebox} pada layar sebelum disimpan)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu;
	}

	/**
	 * Menetapkan waktu perpindahan terjadi.
	 *
	 * @param waktu waktu pindah baru
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

}
