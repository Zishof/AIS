package ais.database.model;

// Generated Apr 5, 2010 1:13:29 AM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.ruang_pmb} — <b>ruang ujian PMB</b> (Penerimaan
 * Mahasiswa Baru): kode dan nama ruang, gedung, kapasitas, paket ujian, status penuh, serta
 * tahun/tahun akademik yang KEDUANYA diturunkan dari {@link #getUjianPMB()} terkait.
 *
 * <p>Meng-extend {@link DataSop} dan menautkan {@link DisposisiSop} — status aktif ruang ini
 * ({@link #getAktif()}) tunduk pada alur SOP (lihat {@code SopUtil}/{@code AlurSop} yang sudah
 * didokumentasikan lengkap pada batch sebelumnya sebagai gerbang sentral mesin SOP): ruang bisa
 * dianggap tidak aktif bukan hanya lewat {@link #setAktif(Boolean)} langsung, melainkan juga
 * bila disposisi SOP terkait tidak aktif atau ditolak lewat alur yang menandai penolakan
 * berhenti di sini ({@code AlurSop.getPenolakanAdaDiSini()}).</p>
 *
 * @see DisposisiSop
 * @see UjianPMB
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ruang_pmb")
public class RuangPMB extends DataSop {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = -7550466125892447098L;
	/** Primary key baris {@code ruang_pmb}, kolom {@code id} (identity, auto-generate). */
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
	 * Representasi ringkas untuk log/debug: nama ruang.
	 *
	 * @return {@link #nama} apa adanya (tidak melalui {@link #getNama()}, sehingga tidak
	 *         di-{@code trim()})
	 */
	public String toString() {
		return nama;
	}

	/** Kode ruangan (identitas singkat). */
	private String kodeRuangan;
	/** Nama ruang ujian PMB. */
	private String nama;
	/** Gedung tempat ruang ini berada. */
	private Gedung gedung;
	/** Kapasitas ruang (jumlah peserta maksimal); default 30 bila kosong, lihat {@link #getKapasitasRuangan()}. */
	private Integer kapasitasRuangan;
	/** Paket ujian yang memakai ruang ini. */
	private Paket paket;
	/** Penanda jumlah/status penuh ruang ini; default 0 bila kosong. */
	private Integer penuh;
	/** Tahun ujian; diturunkan dari {@link #ujianPMB} bila terisi, lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Tahun akademik ujian; diturunkan dari {@link #ujianPMB} bila terisi, lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;

	/** Ujian PMB yang memakai ruang ini; sumber penurunan {@link #tahun}/{@link #tahunAkademik}. */
	private UjianPMB ujianPMB;
	/** Disposisi SOP yang menggerbangi status aktif ruang ini; lihat {@link #getAktif()}. */
	private DisposisiSop disposisiSop;
	/** Flag aktif lokal ruang ini; bisa ditimpa {@code false} oleh gerbang SOP, lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public RuangPMB() {
	}

	/**
	 * @return primary key baris {@code ruang_pmb}; {@code null} sebelum baris di-{@code INSERT}.
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
	 * @return kode ruangan; boleh {@code null} (meski kolomnya {@code nullable = false} di skema).
	 */
	@Column(name = "kode_ruangan", nullable = false, length = 50)
	public String getKodeRuangan() {
		return this.kodeRuangan;
	}

	/**
	 * @param kodeRuangan kode ruangan baru.
	 */
	public void setKodeRuangan(String kodeRuangan) {
		this.kodeRuangan = kodeRuangan;
	}

	/**
	 * @return nama ruang, di-{@code trim()}; {@code null} bila field mentah {@code null} (meski
	 *         kolomnya {@code nullable = false} di skema).
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama ruang baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return gedung tempat ruang ini berada (proxy lazy diresolusi via {@code check()}); boleh
	 *         {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gedung", nullable = true)
	public Gedung getGedung() {
		gedung = check(gedung);
		return this.gedung;
	}

	/**
	 * @param gedung gedung baru; {@code null} untuk melepas tautan.
	 */
	public void setGedung(Gedung gedung) {
		this.gedung = gedung;
	}

	/**
	 * @param kapasitasRuangan kapasitas baru.
	 */
	public void setKapasitasRuangan(Integer kapasitasRuangan) {
		this.kapasitasRuangan = kapasitasRuangan;
	}

	/**
	 * Kapasitas ruang (jumlah peserta maksimal).
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 30} pada pembacaan pertama.</p>
	 *
	 * @return kapasitas ruang; {@code 30} bila belum diisi.
	 */
	@Column(name = "kapasitas_ruangan", length = 10, nullable = false)
	public Integer getKapasitasRuangan() {
		if (kapasitasRuangan == null) {
			kapasitasRuangan = 30;
		}
		return kapasitasRuangan;
	}

	/**
	 * @param paket paket ujian baru; {@code null} untuk melepas tautan.
	 */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	/**
	 * @return paket ujian yang memakai ruang ini (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket")
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	/**
	 * @param penuh penanda penuh baru.
	 */
	public void setPenuh(Integer penuh) {
		this.penuh = penuh;
	}

	/**
	 * Penanda jumlah/status penuh ruang ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 0} pada pembacaan pertama.</p>
	 *
	 * @return nilai penuh; {@code 0} bila belum diisi.
	 */
	@Column(name = "penuh")
	public Integer getPenuh() {
		if (penuh == null) {
			penuh = 0;
		}
		return penuh;
	}

	/**
	 * Tahun ujian ruang ini.
	 *
	 * <p><b>Getter yang menulis balik (diturunkan dari relasi):</b> bila {@link #getUjianPMB()}
	 * tidak {@code null}, field {@link #tahun} DITIMPA dengan {@code ujianPMB.getTahun()} setiap
	 * kali getter ini dipanggil — nilai yang pernah diset manual lewat {@link
	 * #setTahun(Integer)} tertimpa selama relasi {@link #ujianPMB} terisi.</p>
	 *
	 * @return tahun efektif (dari {@link #ujianPMB} bila tersedia, atau field lokal bila
	 *         tidak); boleh {@code null}.
	 */
	public Integer getTahun() {
		ujianPMB = getUjianPMB();
		if (ujianPMB != null) {
			tahun = ujianPMB.getTahun();
		}
		return tahun;
	}

	/**
	 * @param tahun tahun baru untuk field lokal (bisa tetap ditimpa oleh tahun {@link
	 *              #ujianPMB} saat dibaca via {@link #getTahun()}).
	 */
	@Column(name = "tahun")
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Tahun akademik ujian ruang ini.
	 *
	 * <p><b>Getter yang menulis balik (diturunkan dari relasi):</b> pola sama seperti {@link
	 * #getTahun()} — bila {@link #getUjianPMB()} tidak {@code null}, field {@link
	 * #tahunAkademik} DITIMPA dengan {@code ujianPMB.getTahunAkademik()}.</p>
	 *
	 * @return tahun akademik efektif (dari {@link #ujianPMB} bila tersedia, atau field lokal
	 *         bila tidak); boleh {@code null}.
	 */
	public String getTahunAkademik() {
		ujianPMB = getUjianPMB();
		if (ujianPMB != null) {
			tahunAkademik = ujianPMB.getTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * @param tahunAkademik tahun akademik baru untuk field lokal (bisa tetap ditimpa oleh tahun
	 *                      akademik {@link #ujianPMB} saat dibaca via {@link
	 *                      #getTahunAkademik()}).
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return ujian PMB yang memakai ruang ini (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ujian_pmb", nullable = true)
	public UjianPMB getUjianPMB() {
		ujianPMB = check(ujianPMB);
		return ujianPMB;
	}

	/**
	 * @param ujianPMB ujian PMB baru; {@code null} untuk melepas tautan.
	 */
	public void setUjianPMB(UjianPMB ujianPMB) {
		this.ujianPMB = ujianPMB;
	}

	/**
	 * @return disposisi SOP yang menggerbangi status aktif ruang ini (proxy lazy diresolusi via
	 *         {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menautkan disposisi SOP baru.
	 *
	 * <p><b>Guard tampak berlapis, tetapi lapisan kedua tidak pernah efektif:</b> baris pertama
	 * sudah langsung {@code return} (tidak melakukan apa pun) bila {@code disposisiSop} {@code
	 * null} atau belum ber-ID; akibatnya, pada titik ternary di baris kedua, kondisi {@code
	 * (disposisiSop == null || disposisiSop.getId() == null)} SUDAH PASTI {@code false} (kasus
	 * itu sudah ditangani guard di atasnya) — sehingga ternary tersebut efektif selalu memilih
	 * cabang {@code disposisiSop}, sama seperti penugasan langsung {@code this.disposisiSop =
	 * disposisiSop;} tanpa ternary sama sekali. Tampak sebagai sisa refactor/duplikasi guard,
	 * bukan bug fungsional (perilaku akhirnya tetap benar: hanya menerima disposisi yang valid
	 * dan sudah ber-ID). Dicatat apa adanya; tidak dibersihkan di sesi dokumentasi ini.</p>
	 *
	 * @param disposisiSop disposisi SOP baru; nilai {@code null} atau belum ber-ID diabaikan
	 *                     (tautan lama dipertahankan).
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Status aktif ruang ini.
	 *
	 * <p><b>Getter yang menulis balik, digerbangi SOP:</b> selain field lokal {@link #aktif},
	 * getter ini memeriksa {@link #getDisposisiSop()} — bila disposisi itu ada dan TIDAK aktif
	 * ({@code !disposisiSop.getAktif()}), field {@link #aktif} DITIMPA menjadi {@code false};
	 * bila disposisi itu punya {@code disposisiEnd} dengan {@link
	 * ais.database.model.sop.AlurSop#getPenolakanAdaDiSini()} bernilai {@code true} (alur SOP
	 * menandai bahwa penolakan berhenti/berlaku di titik ini), field {@link #aktif} juga
	 * DITIMPA menjadi {@code false}. Kedua kondisi ini bersifat SATU ARAH — tidak ada jalur
	 * yang mengembalikan {@link #aktif} menjadi {@code true} lagi begitu salah satunya pernah
	 * memaksanya {@code false} pada instance yang sama (field yang sudah {@code false} tidak
	 * ditimpa balik oleh kondisi yang tidak lagi terpenuhi pada pemanggilan berikutnya, karena
     * tidak ada cabang {@code else} yang mengembalikannya ke {@code true}).</p>
	 *
	 * @return {@code true} bila ruang ini aktif dan tidak digerbangi status SOP negatif; default
	 *         {@code true} bila field lokal belum diisi dan tidak ada gerbang SOP yang aktif.
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif baru untuk field lokal (bisa tetap ditimpa menjadi {@code
	 *              false} oleh gerbang SOP saat dibaca via {@link #getAktif()} — lihat javadoc
	 *              getter; tidak bisa dipaksa kembali {@code true} lewat setter ini bila gerbang
	 *              SOP masih menahannya).
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
