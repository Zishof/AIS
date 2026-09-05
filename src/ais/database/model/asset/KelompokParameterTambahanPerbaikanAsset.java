package ais.database.model.asset;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * KELOMPOK (pengelompokan tampilan) untuk parameter tambahan dinamis pada form {@link
 * PerbaikanAsset} -- mis. "Data Teknis", "Biaya", "Dokumen Pendukung". Satu kelompok memayungi
 * satu atau lebih {@link ais.database.model.ParameterTambahan} lewat baris penghubung {@link
 * ParameterTambahanPerbaikanAsset}, dan satu {@link JenisPerbaikanAsset} bisa memakai banyak
 * kelompok (relasi many-to-many, lihat {@link JenisPerbaikanAsset#getKelompokParameterTambahanPerbaikanAssets()}).
 *
 * <p>Saat form perbaikan aset dibangun, {@link
 * ais.action.master.helper.ParameterTambahanPerbaikanAssetListener#onEvent} merender satu baris
 * judul per kelompok (memakai {@link #getNama()}) diikuti baris-baris parameter anggotanya,
 * dan menyembunyikan baris judul itu bila tidak ada satu pun parameter anggota yang aktif dan
 * tampil.</p>
 *
 * <h3>Baris default: {@link #checkCreateDefault()}</h3>
 *
 * <p>Kelas ini menyediakan mekanisme "pastikan minimal satu kelompok default tersedia" lewat
 * {@link #checkCreateDefault()} -- dipanggil saat sistem butuh kelompok fallback bernama
 * "Form Tambahan" bila belum ada satu pun baris yang ditandai {@link #getDefaultData()}.</p>
 *
 * <h3>Pengurutan tampilan</h3>
 *
 * <p>Mengimplementasikan {@link Comparable} (lewat {@link GeneralValueObject}) berdasarkan
 * {@link #getNomorUrut()} -- dipakai {@link TreeSet} pada {@link
 * JenisPerbaikanAsset#getKelompokParameterTambahanPerbaikanAssets()} dan pada pengurutan
 * {@code @OrderBy} di query Hibernate untuk kelas yang sama.</p>
 *
 * @see ParameterTambahanPerbaikanAsset baris penghubung ke definisi parameter anggota
 * @see JenisPerbaikanAsset pemakai kelompok ini lewat relasi many-to-many
 * @see PerbaikanAsset form yang menampilkan kelompok ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "kelompok_parameter_tambahan_perbaikan_asset")
public class KelompokParameterTambahanPerbaikanAsset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java; sama dengan entitas sepaket lain karena berasal dari
	 * templat hbm2java yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini. */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong agar jejak audit lama
	 * tidak tertimpa oleh proses batch tanpa konteks pengguna aktif.
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum UPDATE dikirim.
	 * Didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturannya terpusat.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu penyuntingan terakhir, bernilai awal waktu server saat objek dibuat. Bidang
	 * audit ini diulang di tiap entitas AIS sebagai KEHARUSAN TEKNIS: kelas induk tidak
	 * mewariskan pemetaan kolom apa pun untuknya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu penyuntingan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} untuk objek hasil konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berupa id digabung nama, dipakai label komponen ZK dan pesan log.
	 *
	 * @return {@code "<id>-<nama>"}; bagian nama bisa berupa string {@code "null"} literal bila
	 *         field {@code nama} belum terisi (dipakai apa adanya, bukan lewat getter)
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama tampil kelompok, dirender sebagai baris judul pada form perbaikan aset. */
	private String nama;

	/** Keterangan bebas untuk kelompok ini. */
	private String keterangan;

	/** Penanda baris ini adalah kelompok default/fallback; lihat {@link #checkCreateDefault()}. */
	private Boolean defaultData;

	/** Status aktif/tidak aktif kelompok ini. */
	private Boolean aktif;

	/** Nomor urut tampil kelompok, dipakai pengurutan lewat {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;

	/**
	 * Memastikan minimal satu baris kelompok DITANDAI {@link #getDefaultData()} tersedia di basis
	 * data, membuatnya bila belum ada -- dipakai sebagai fallback saat sistem butuh satu kelompok
	 * parameter tambahan untuk ditampilkan tanpa bergantung pada konfigurasi eksplisit pengguna.
	 *
	 * <h3>Perilaku transaksi -- membuka DAN menutup sesi sendiri</h3>
	 *
	 * <p>Method ini mengambil {@link HibernateUtil#currentNativeSession()} (BUKAN sesi
	 * request/thread yang sedang berjalan lewat {@code currentSession()} biasa), mencari satu
	 * baris dengan {@code defaultData = true} lewat {@link Restrictions#eq(String, Object)},
	 * membuatnya bila tidak ditemukan (transaksi {@code begin()}/{@code commit()} SENDIRI, bukan
	 * bagian dari transaksi pemanggil), lalu secara eksplisit memanggil {@link
	 * HibernateUtil#closeSession()} SEBELUM mengembalikan hasil. Ini berarti bila dipanggil dari
	 * tengah-tengah proses yang sudah punya sesi Hibernate sendiri berjalan (mis. sedang
	 * menyimpan {@link PerbaikanAsset} dalam satu transaksi besar), pemanggilan method ini bisa
	 * MENUTUP sesi yang sedang dipakai proses pemanggil jika keduanya kebetulan berbagi sesi
	 * native yang sama -- pemanggil harus SANGAT BERHATI-HATI memanggil method ini hanya di luar
	 * konteks transaksi aktif lain, atau memastikan ulang sesinya sendiri setelahnya.</p>
	 *
	 * @return baris kelompok default yang sudah ada, atau baris baru bernama "Form Tambahan"
	 *         yang baru saja dibuat dan disimpan bila belum ada satu pun sebelumnya
	 */
	public static KelompokParameterTambahanPerbaikanAsset checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset = (KelompokParameterTambahanPerbaikanAsset) session
				.createCriteria(KelompokParameterTambahanPerbaikanAsset.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanPerbaikanAsset == null) {
			kelompokParameterTambahanPerbaikanAsset = new KelompokParameterTambahanPerbaikanAsset();
			kelompokParameterTambahanPerbaikanAsset.setDefaultData(true);
			kelompokParameterTambahanPerbaikanAsset.setNama("Form Tambahan");
			kelompokParameterTambahanPerbaikanAsset.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanPerbaikanAsset);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanPerbaikanAsset;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public KelompokParameterTambahanPerbaikanAsset() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>{@code insertable = false} karena nilainya di-generate database.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya dipanggil Hibernate seusai INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama tampil kelompok.
	 *
	 * @return nama hasil {@code trim()}, atau {@code null} bila belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama tampil kelompok.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk kelompok ini.
	 *
	 * <p>Berbeda dari kebanyakan getter keterangan lain di paket ini, method ini mengembalikan
	 * field APA ADANYA tanpa {@code trim()} maupun substitusi ke string kosong -- bisa
	 * mengembalikan {@code null}.</p>
	 *
	 * @return keterangan tersimpan, atau {@code null} bila belum terisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Penanda baris ini adalah kelompok default/fallback yang dijaga oleh {@link
	 * #checkCreateDefault()}.
	 *
	 * @return {@code true} bila baris ini kelompok default; {@code false} bila bukan atau belum
	 *         pernah diisi
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Mengisi penanda kelompok default.
	 *
	 * @param defaultData nilai baru
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Status aktif/tidak aktif kelompok ini.
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi (default aktif); {@code false} bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Mengisi status aktif.
	 *
	 * @param aktif nilai baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nomor urut tampil kelompok, dipakai {@link #compareTo(GeneralValueObject)} untuk
	 * pengurutan.
	 *
	 * @return nomor urut tersimpan, atau {@code 1} bila belum pernah diisi
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Mengisi nomor urut tampil.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan urutan tampil dua kelompok berdasarkan {@link #getNomorUrut()} -- dipakai
	 * {@link TreeSet} pada {@link JenisPerbaikanAsset#getKelompokParameterTambahanPerbaikanAssets()}
	 * untuk menjaga urutan tampil kelompok tetap konsisten setiap kali di-iterasi.
	 *
	 * <p>Melempar {@link ClassCastException} bila {@code arg0} bukan instance {@link
	 * KelompokParameterTambahanPerbaikanAsset} -- konsisten dengan kontrak {@link
	 * Comparable#compareTo} yang mengasumsikan pembanding bertipe sama.</p>
	 *
	 * @param arg0 kelompok lain yang dibandingkan
	 * @return hasil {@link Integer#compareTo(Integer)} atas nomor urut kedua kelompok
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanPerbaikanAsset) arg0).getNomorUrut());
	}
}
