package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.wisuda} — <b>event wisuda</b> (angkatan
 * kelulusan ke-{@link #getWisudaKe()}): tanggal pelaksanaan, moto, kuota maksimal peserta,
 * jenis penjadwalan, dan flag konfigurasi terkait hari libur nasional/pengurutan otomatis.
 *
 * <p>Meng-extend {@link VOPembelajaran} dan meng-implement {@link VOPesertaPembelajaran} —
 * abstraksi generik "peristiwa pembelajaran" yang dipakai lintas modul (perkuliahan, wisuda,
 * dsb.), sehingga sejumlah method di sini ({@link #getCourse()}/{@link #setCourse(String)},
 * {@link #ambilVOPembelajaran()}, {@link #getUrutkanotomatis()}/{@link
 * #setUrutkanotomatis(Boolean)}) adalah implementasi kontrak interface tersebut, bukan
 * kebutuhan bisnis wisuda secara langsung. {@link #getMaksimalQuota()} berelasi dengan {@link
 * QuotaWisudaUntukFakultas} (kuota per-fakultas, didokumentasikan terpisah) dan {@link
 * #getHanyaGunakanKuotaPerguruanTinggi()} menentukan apakah kuota per-fakultas itu diabaikan
 * demi kuota tunggal tingkat perguruan tinggi. Pendaftaran peserta wisuda dicatat lewat {@link
 * PendaftaranWisuda} (baris anak, dihitung oleh {@link #ambilJumlahDetailperkuliahanLangsung()}).</p>
 *
 * @see PendaftaranWisuda
 * @see QuotaWisudaUntukFakultas
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "wisuda")
public class Wisuda extends VOPembelajaran implements VOPesertaPembelajaran {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463822572548439808L;
	/** Primary key baris {@code wisuda}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code "<id>-<wisudaKe>-<moto>-<keterangan>"}.
	 *
	 * @return string ringkas identitas event wisuda ini
	 */
	public String toString() {
		return id + "-" + wisudaKe + "-" + moto + "-" + keterangan;
	}

	/** Pengguna yang mengunci event wisuda ini (mencegah perubahan lebih lanjut). */
	private Tbmuser dikunci;

	/**
	 * @return pengguna yang mengunci event wisuda ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} bila belum/tidak dikunci.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * @param dikunci pengguna pengunci baru; {@code null} untuk melepas status terkunci.
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}


	/** Nomor angkatan wisuda ini (wisuda ke-berapa). */
	private Integer wisudaKe;
	/** Kuota maksimal peserta wisuda ini; default 0 bila kosong, lihat {@link #getMaksimalQuota()}. */
	private Integer maksimalQuota;
	/** Flag: hanya memakai kuota tunggal tingkat perguruan tinggi, mengabaikan kuota per-fakultas ({@link QuotaWisudaUntukFakultas}); default {@code false} bila belum diisi. */
	private Boolean hanyaGunakanKuotaPerguruanTinggi;
	/** Tanggal pelaksanaan wisuda. */
	private Date tanggal;
	/** Moto/tema wisuda ini. */
	private String moto;
	/** Flag aktif event wisuda ini; default {@code true} bila belum diisi. */
	private Boolean aktif;
	/** Keterangan bebas event wisuda ini. */
	private String keterangan;
	/** Konfigurasi course terenkode JSON, bagian kontrak {@link VOPembelajaran}; lihat {@link #getCourse()}. */
	private String course;
	/** Jenis penjadwalan wisuda (mis. "Mingguan"); default "Mingguan" bila belum diisi. */
	private String jenis;
	/** Flag: melewati tanggal merah/hari libur nasional saat penjadwalan; default {@code true} bila belum diisi. */
	private Boolean lewatiTanggalMerahNasional;
	/** Flag: urutkan peserta secara otomatis, bagian kontrak {@link VOPesertaPembelajaran}; default {@code true} bila belum diisi. */
	private Boolean urutkanotomatis;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public Wisuda() {
	}

	/**
	 * @return primary key baris {@code wisuda}; {@code null} sebelum baris di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return keterangan bebas event wisuda ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", length = 500)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk event wisuda ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @param wisudaKe nomor angkatan wisuda baru.
	 */
	public void setWisudaKe(Integer wisudaKe) {
		this.wisudaKe = wisudaKe;
	}

	/**
	 * @return nomor angkatan wisuda ini; boleh {@code null}.
	 */
	@Column(name = "wisuda_ke", length = 10)
	public Integer getWisudaKe() {
		return wisudaKe;
	}

	/**
	 * @param maksimalQuota kuota maksimal peserta baru.
	 */
	public void setMaksimalQuota(Integer maksimalQuota) {
		this.maksimalQuota = maksimalQuota;
	}

	/**
	 * @return kuota maksimal peserta wisuda ini (tingkat perguruan tinggi/keseluruhan; lihat
	 *         {@link QuotaWisudaUntukFakultas} untuk kuota per-fakultas); {@code 0} bila belum
	 *         diisi.
	 */
	@Column(name = "maksimal_quota", length = 10)
	public Integer getMaksimalQuota() {
		return maksimalQuota == null ? 0 : maksimalQuota;
	}

	/**
	 * @param tanggal tanggal pelaksanaan baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return tanggal pelaksanaan wisuda ini; boleh {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal", length = 0)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * @param moto moto/tema baru.
	 */
	public void setMoto(String moto) {
		this.moto = moto;
	}

	/**
	 * @return moto/tema wisuda ini, di-{@code trim()}; string kosong ({@code ""}) bila belum
	 *         diisi — tidak pernah {@code null}.
	 */
	@Column(name = "moto", length = 500)
	public String getMoto() {
		return moto == null ? "" : moto.trim();
	}

	/**
	 * Status aktif event wisuda ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code true} pada pembacaan pertama.</p>
	 *
	 * @return status aktif; {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@code true} bila wisuda ini hanya memakai kuota tunggal tingkat perguruan
	 *         tinggi (mengabaikan kuota per-fakultas di {@link QuotaWisudaUntukFakultas});
	 *         default {@code false} bila belum diisi.
	 */
	public Boolean getHanyaGunakanKuotaPerguruanTinggi() {
		return hanyaGunakanKuotaPerguruanTinggi == null ? false : hanyaGunakanKuotaPerguruanTinggi;
	}

	/**
	 * @param hanyaGunakanKuotaPerguruanTinggi nilai flag baru.
	 */
	public void setHanyaGunakanKuotaPerguruanTinggi(Boolean hanyaGunakanKuotaPerguruanTinggi) {
		this.hanyaGunakanKuotaPerguruanTinggi = hanyaGunakanKuotaPerguruanTinggi;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran#getCourse()}: konfigurasi course terenkode
	 * JSON untuk event wisuda ini (makna spesifik ditentukan pemanggil/kontrak interface,
	 * bukan logika bisnis wisuda langsung).
	 *
	 * @return string JSON konfigurasi course; {@code "{}"} (JSON objek kosong) bila field
	 *         mentah kosong/{@code null} — tidak pernah {@code null}/kosong literal.
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * @param course string JSON konfigurasi course baru.
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Menghitung jumlah baris {@link PendaftaranWisuda} yang menunjuk ke event wisuda ini —
	 * implementasi kontrak {@link VOPembelajaran#ambilJumlahDetailperkuliahanLangsung()} untuk
	 * konteks wisuda (nama method mengikuti kontrak generik "perkuliahan", tetapi di sini
	 * dipakai untuk menghitung PESERTA WISUDA, bukan detail perkuliahan).
	 *
	 * <p><b>Efek samping:</b> menjalankan query {@code COUNT(*)} langsung ke database lewat
	 * {@link HibernateUtil#currentSession()}.</p>
	 *
	 * @return jumlah baris {@link PendaftaranWisuda} yang menunjuk ke {@code this}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(PendaftaranWisuda.class).add(Restrictions.eq("wisuda", this))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		return count;
	}

	/**
	 * @return jenis penjadwalan wisuda ini; {@code "Mingguan"} bila belum diisi.
	 */
	public String getJenis() {
		return jenis == null ? "Mingguan" : jenis;
	}

	/**
	 * @param jenis jenis penjadwalan baru.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * @return {@code true} bila penjadwalan wisuda ini melewati tanggal merah/hari libur
	 *         nasional; default {@code true} bila belum diisi.
	 */
	public Boolean getLewatiTanggalMerahNasional() {
		return lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional;
	}

	/**
	 * @param lewatiTanggalMerahNasional nilai flag baru.
	 */
	public void setLewatiTanggalMerahNasional(Boolean lewatiTanggalMerahNasional) {
		this.lewatiTanggalMerahNasional = lewatiTanggalMerahNasional;
	}

	/**
	 * Implementasi kontrak {@link VOPembelajaran#ambilVOPembelajaran()}: mengembalikan diri
	 * sendiri (self-referential), karena {@link Wisuda} SUDAH merupakan implementasi {@link
	 * VOPembelajaran} secara langsung (bukan sekadar memegang referensi ke satu).
	 *
	 * @return {@code this}
	 */
	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		// TODO Auto-generated method stub
		return this;
	}

	/**
	 * Implementasi kontrak {@link VOPesertaPembelajaran#getUrutkanotomatis()}.
	 *
	 * @return {@code true} bila peserta wisuda diurutkan otomatis; default {@code true} bila
	 *         belum diisi.
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * @param urutkanotomatis nilai flag baru.
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

}
