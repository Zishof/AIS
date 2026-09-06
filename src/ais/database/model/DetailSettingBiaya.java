package ais.database.model;

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
import org.json.JSONObject;

import ais.common.Common;
import ais.ui.util.MyJSONObject;

/**
 * Entitas Hibernate untuk tabel {@code public.detail_setting_biaya}, merepresentasikan satu baris
 * rincian dari sebuah pengaturan biaya ({@link SettingBiaya}) — yaitu berapa besar biaya
 * {@link #getItemBiaya()} tertentu yang ditagihkan pada termin/tahap pembayaran ke-
 * {@link #getBayarKe()}, lengkap dengan nilai default tanggal tagihan, tanggal deadline, dan
 * keterangan tagihan yang akan dipakai saat sistem meng-generate tagihan mahasiswa secara massal.
 * <p>
 * {@link #getBiayaPerProdi()} menyimpan override nilai default (biaya/tanggal tagihan/deadline/
 * keterangan) per jurusan dalam bentuk string JSON, dengan key berpola {@code b_<idJurusan>},
 * {@code t_<idJurusan>}, {@code d_<idJurusan>}, {@code ket_<idJurusan>}; method
 * {@code ambilDefaultBiaya}, {@code ambilDefaultTanggalTagihan}, {@code ambilDefaultTanggalDeadline},
 * dan {@code ambilDefaultKeteranganTagihan} membaca override tersebut untuk {@link Jurusan}
 * tertentu, sehingga satu baris {@code DetailSettingBiaya} dapat punya nilai berbeda per jurusan
 * tanpa perlu baris terpisah.
 * <p>
 * Relasi {@code @ManyToOne} (lazy): {@link #getSettingBiaya()} (skema/paket biaya induk) dan
 * {@link #getItemBiaya()} (jenis biaya, mis. SPP/her-registrasi/dsb). Perubahan tercatat
 * historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "detail_setting_biaya")
public class DetailSettingBiaya extends GeneralValueObject {
	/** ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database). */
	private static final long serialVersionUID = -7050166125892447098L;
	/** Primary key baris {@code detail_setting_biaya}, kolom {@code id} (identity, auto-generate). */
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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu "terakhir diubah"; diinisialisasi ke waktu sekarang saat instance dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Representasi ringkas untuk log/debug: {@code "<id>-<settingBiaya>-<itemBiaya>-<bayarKe>"}.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getSettingBiaya()} dan {@link #getItemBiaya()},
	 * yang menulis balik field terkait (resolusi proxy lazy via {@code check()}) — sekadar
	 * memanggil {@code toString()} bisa memicu lazy-load kedua relasi tersebut.</p>
	 *
	 * @return string ringkas identitas baris ini
	 */
	public String toString() {
		settingBiaya = getSettingBiaya();
		itemBiaya = getItemBiaya();
		return id + "-" + settingBiaya + "-" + itemBiaya + "-" + bayarKe;
	}

	/** Pengaturan/skema biaya induk tempat rincian ini berada. */
	private SettingBiaya settingBiaya;
	/** Jenis/item biaya yang ditagihkan, mis. SPP, her-registrasi, dsb. */
	private ItemBiaya itemBiaya;
	/** Nilai biaya default; default 0.0 bila kosong. */
	private Double defaultBiaya;
	/** Tanggal terbit tagihan bawaan (default, berlaku untuk seluruh jurusan kecuali di-override); lihat {@link #getBiayaPerProdi()}. */
	private Date defaultTanggalTagihan;
	/** Tanggal deadline pembayaran bawaan (default, berlaku untuk seluruh jurusan kecuali di-override). */
	private Date defaultTanggalDeadline;
	/** Keterangan tagihan bawaan (default, berlaku untuk seluruh jurusan kecuali di-override). */
	private String defaultKeterangan;
	/** Override nilai (biaya/tanggal/keterangan) per jurusan, disimpan sebagai string JSON; lihat {@link #getBiayaPerProdi()}. */
	private String biayaPerProdi;
	/** Nomor urut termin/tahap pembayaran (mis. 1 = pembayaran pertama); default 1. */
	private Integer bayarKe;

	/**
	 * @return primary key baris {@code detail_setting_biaya}; {@code null} sebelum baris
	 *         di-{@code INSERT}.
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
	 * @return jenis/item biaya yang ditagihkan pada rincian ini (proxy lazy diresolusi via
	 *         {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);
		return this.itemBiaya;
	}

	/**
	 * @param itemBiaya item biaya baru untuk rincian ini; {@code null} untuk melepas tautan.
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * @param settingBiaya pengaturan/skema biaya induk baru; {@code null} untuk melepas tautan.
	 */
	public void setSettingBiaya(SettingBiaya settingBiaya) {
		this.settingBiaya = settingBiaya;
	}

	/**
	 * @return pengaturan/skema biaya induk tempat rincian ini berada (proxy lazy diresolusi via
	 *         {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "setting_biaya", nullable = true)
	public SettingBiaya getSettingBiaya() {
		settingBiaya = check(settingBiaya);
		return settingBiaya;
	}

	/**
	 * Nilai biaya bawaan (default) untuk rincian ini — dipakai sebagai nominal ketika {@link
	 * SettingBiaya#getGunakanBiayaDefault()} menyala, atau sebagai fallback saat item biaya
	 * tidak ditemukan di peta JSON {@link SettingBiayaDetail#getBiayas()} (lihat
	 * {@code DetailBiaya.getNilaiBiaya()}). Juga dipakai sebagai fallback oleh {@link
	 * #ambilDefaultBiaya(Jurusan)} ketika jurusan tertentu belum diberi override per-prodi.
	 *
	 * @return nilai biaya bawaan; {@code 0.0} bila belum diisi.
	 */
	public Double getDefaultBiaya() {
		return defaultBiaya == null ? 0.0 : defaultBiaya;
	}

	/**
	 * @param defaultBiaya nilai biaya bawaan baru.
	 */
	public void setDefaultBiaya(Double defaultBiaya) {
		this.defaultBiaya = defaultBiaya;
	}

	/**
	 * @return tanggal terbit tagihan bawaan untuk rincian ini (berlaku untuk seluruh jurusan
	 *         kecuali di-override lewat {@link #getBiayaPerProdi()}); boleh {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDefaultTanggalTagihan() {
		return defaultTanggalTagihan;
	}

	/**
	 * @param defaultTanggalTagihan tanggal terbit tagihan bawaan baru.
	 */
	public void setDefaultTanggalTagihan(Date defaultTanggalTagihan) {
		this.defaultTanggalTagihan = defaultTanggalTagihan;
	}

	/**
	 * @return nomor urut termin/tahap pembayaran rincian ini (mis. 1 = pembayaran pertama,
	 *         2 = pembayaran kedua); default {@code 1} bila belum diisi.
	 */
	public Integer getBayarKe() {
		return bayarKe == null ? 1 : bayarKe;
	}

	/**
	 * @param bayarKe nomor urut termin/tahap pembayaran baru.
	 */
	public void setBayarKe(Integer bayarKe) {
		this.bayarKe = bayarKe;
	}

	/**
	 * Objek JSON kosong statis ({@code "{}"}), dipakai sebagai nilai kembalian bawaan {@link
	 * #getBiayaPerProdi()} ketika kolom {@link #biayaPerProdi} belum diisi. Dibagi (shared) di
	 * antara seluruh instance kelas ini, tetapi tidak pernah dimutasi setelah inisialisasi
	 * sehingga aman dibagi lintas request/tenant.
	 */
	private static JSONObject jsonObject = new JSONObject();

	/**
	 * String JSON berisi override nilai (biaya/tanggal tagihan/tanggal deadline/keterangan) per
	 * jurusan, dengan key berpola {@code b_<idJurusan>}, {@code t_<idJurusan>}, {@code
	 * d_<idJurusan>}, {@code ket_<idJurusan>}. Dibaca oleh {@link #ambilDefaultBiaya(Jurusan)}
	 * dan tiga method {@code ambilDefault*} sejenis untuk mendapatkan override milik satu
	 * jurusan tertentu.
	 *
	 * @return string JSON override per-prodi; {@code "{}"} (JSON kosong statis {@link
	 *         #jsonObject}) bila kolom belum diisi atau hanya berisi spasi — <b>tidak pernah
	 *         {@code null}</b>.
	 */
	@Column(columnDefinition = "text")
	public String getBiayaPerProdi() {
		return biayaPerProdi == null || biayaPerProdi.trim().isEmpty() ? jsonObject.toString() : biayaPerProdi;
	}

	/**
	 * @param biayaPerProdi string JSON override per-prodi baru (format lihat {@link
	 *                      #getBiayaPerProdi()}).
	 */
	public void setBiayaPerProdi(String biayaPerProdi) {
		this.biayaPerProdi = biayaPerProdi;
	}

	/**
	 * Mengambil override nilai biaya khusus satu {@code jurusan} dari {@link
	 * #getBiayaPerProdi()} (key {@code b_<idJurusan>}). Dipanggil oleh {@code
	 * DetailBiaya.getNilaiBiaya()} sebagai cabang PERTAMA (prioritas tertinggi) ketika {@link
	 * SettingBiaya#getTampilkanPerProdi()} menyala dan baris tagihan punya {@code jurusan} yang
	 * jelas — nilai yang dikembalikan di sini menjadi nominal tagihan final untuk item biaya
	 * bersangkutan pada jurusan itu.
	 *
	 * <p><b>FIX &mdash; fallback ke {@link #getDefaultBiaya()}, bukan {@code 0.0} (r85669 mencatat,
	 * diperbaiki sesi berikutnya):</b> sebelumnya method ini mengembalikan {@code 0.0} baik saat
	 * key {@code b_<idJurusan>} memang tidak ada di JSON (jurusan belum diberi tarif khusus)
	 * maupun saat parsing gagal (exception apa pun, termasuk {@code jurusan} itu sendiri {@code
	 * null}) &mdash; dua kondisi yang tidak dapat dibedakan pemanggil, dan pada mode "Tampilkan
	 * Per Prodi" ({@link SettingBiaya#getTampilkanPerProdi()}) method ini adalah satu-satunya
	 * sumber nominal di {@code DetailBiaya.getNilaiBiaya()} sehingga jurusan tanpa override
	 * eksplisit tertagih Rp 0. Sekarang: bila key tidak ada, {@code jurusan} {@code null}, atau
	 * parsing gagal, hasilnya jatuh ke {@link #getDefaultBiaya()} (nilai bawaan template) &mdash;
	 * hanya key yang benar-benar ada di JSON (termasuk yang sengaja diisi {@code 0}) yang
	 * dianggap override eksplisit.</p>
	 *
	 * @param jurusan jurusan acuan; boleh {@code null} &mdash; menghasilkan {@link
	 *                #getDefaultBiaya()} (diperlakukan sama seperti tidak ada override)
	 * @return override nominal biaya untuk {@code jurusan} bila key {@code b_<idJurusan>} ada di
	 *         JSON; {@link #getDefaultBiaya()} bila tidak ada override, {@code jurusan} {@code
	 *         null}, ATAU terjadi kegagalan apa pun saat memproses
	 */
	public Double ambilDefaultBiaya(Jurusan jurusan) {
		Double biaya = getDefaultBiaya();
		try {
			MyJSONObject jsonObject = new MyJSONObject(getBiayaPerProdi());
			if (!jsonObject.isNull("b_" + jurusan.getId())) {
				biaya = jsonObject.getDouble("b_" + jurusan.getId());
			}
			jsonObject = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailSettingBiaya.java:163");
			// TODO: handle exception
		}
		return biaya;
	}

	/**
	 * Mengambil override tanggal terbit tagihan khusus satu {@code jurusan} dari {@link
	 * #getBiayaPerProdi()} (key {@code t_<idJurusan>}), diparse dengan {@link
	 * Common#dateFormat}. Dipanggil oleh {@code DetailBiaya.getDefaultTanggalTagihan()} saat
	 * mode per-prodi menyala.
	 *
	 * <p>Berbeda dari {@link #ambilDefaultBiaya(Jurusan)}: hasil {@code null} di sini (baik
	 * karena key tidak ada, bernilai string kosong, maupun karena exception) secara semantik
	 * lebih wajar diterima sebagai "tidak ada override" oleh pemanggil, karena tanggal
	 * {@code null} adalah nilai yang valid dan lazim ditangani lebih lanjut — bukan nilai
	 * nominal yang langsung dipakai sebagai uang seperti pada kasus biaya.</p>
	 *
	 * @param jurusan jurusan acuan; <b>tidak boleh {@code null}</b> (lihat catatan pada {@link
	 *                #ambilDefaultBiaya(Jurusan)})
	 * @return tanggal terbit tagihan override untuk {@code jurusan}; {@code null} bila tidak ada
	 *         override atau bila parsing/pemrosesan gagal
	 */
	public Date ambilDefaultTanggalTagihan(Jurusan jurusan) {
		Date tgl = null;
		try {
			MyJSONObject jsonObject = new MyJSONObject(getBiayaPerProdi());
			tgl = jsonObject.isNull("t_" + jurusan.getId()) || jsonObject.get("t_" + jurusan.getId()).equals("") ? null
					: Common.dateFormat.get().parse(jsonObject.get("t_" + jurusan.getId()) + "");
			jsonObject = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailSettingBiaya.java:176");
			// TODO: handle exception
		}
		return tgl;
	}


	/**
	 * Mengambil override tanggal deadline pembayaran khusus satu {@code jurusan} dari {@link
	 * #getBiayaPerProdi()} (key {@code d_<idJurusan>}), diparse dengan {@link Common#dateFormat}.
	 * Sama persis strukturnya dengan {@link #ambilDefaultTanggalTagihan(Jurusan)}, hanya beda
	 * key dan makna tanggalnya.
	 *
	 * @param jurusan jurusan acuan; <b>tidak boleh {@code null}</b> (lihat catatan pada {@link
	 *                #ambilDefaultBiaya(Jurusan)})
	 * @return tanggal deadline pembayaran override untuk {@code jurusan}; {@code null} bila
	 *         tidak ada override atau bila parsing/pemrosesan gagal
	 */
	public Date ambilDefaultTanggalDeadline(Jurusan jurusan) {
		Date tgl = null;
		try {
			MyJSONObject jsonObject = new MyJSONObject(getBiayaPerProdi());
			tgl = jsonObject.isNull("d_" + jurusan.getId()) || jsonObject.get("d_" + jurusan.getId()).equals("") ? null
					: Common.dateFormat.get().parse(jsonObject.get("d_" + jurusan.getId()) + "");
			jsonObject = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailSettingBiaya.java:190");
			// TODO: handle exception
		}
		return tgl;
	}

	/**
	 * Mengambil override keterangan tagihan khusus satu {@code jurusan} dari {@link
	 * #getBiayaPerProdi()} (key {@code ket_<idJurusan>}).
	 *
	 * @param jurusan jurusan acuan; <b>tidak boleh {@code null}</b> (lihat catatan pada {@link
	 *                #ambilDefaultBiaya(Jurusan)})
	 * @return keterangan tagihan override untuk {@code jurusan}; string kosong ({@code ""})
	 *         bila tidak ada override atau bila pemrosesan gagal — tidak pernah {@code null}
	 */
	public String ambilDefaultKeteranganTagihan(Jurusan jurusan) {
		String ket = "";
		try {
			MyJSONObject jsonObject = new MyJSONObject(getBiayaPerProdi());
			ket = jsonObject.isNull("ket_" + jurusan.getId()) ? "" : jsonObject.get("ket_" + jurusan.getId()) + "";
			jsonObject = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailSettingBiaya.java:202");
			// TODO: handle exception
		}
		return ket;
	}

	/**
	 * @return keterangan tagihan bawaan (default) untuk rincian ini; string kosong ({@code ""})
	 *         bila belum diisi — tidak pernah {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getDefaultKeterangan() {
		return defaultKeterangan == null ? "" : defaultKeterangan;
	}

	/**
	 * @param defaultKeterangan keterangan tagihan bawaan baru.
	 */
	public void setDefaultKeterangan(String defaultKeterangan) {
		this.defaultKeterangan = defaultKeterangan;
	}

	/**
	 * @return tanggal deadline pembayaran bawaan untuk rincian ini (berlaku untuk seluruh
	 *         jurusan kecuali di-override lewat {@link #getBiayaPerProdi()}); boleh {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getDefaultTanggalDeadline() {
		return defaultTanggalDeadline;
	}

	/**
	 * @param defaultTanggalDeadline tanggal deadline pembayaran bawaan baru.
	 */
	public void setDefaultTanggalDeadline(Date defaultTanggalDeadline) {
		this.defaultTanggalDeadline = defaultTanggalDeadline;
	}
}
