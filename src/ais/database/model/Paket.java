package ais.database.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

/**
 * Model data untuk paket. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code String kode}, {@code String nama}, {@code String
 * keterangan}, {@code Integer jumlahProdiYgBolehDiambil}; pemetaan persistence: tabel {@code public.paket};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()},
 * {@code getNama()}, {@code getKeterangan()}); mutasi data ({@code setOlehId()}, {@code setOleh()}, {@code
 * onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()}, {@code setNama()}); operasi domain lain ({@code
 * toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "paket")
public class Paket extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -227313087242633498L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private Integer jumlahProdiYgBolehDiambil = 1;
	private Boolean bisaDipilihSemuaGelombang;
	private Boolean bisaMemilihPilihanYangSama;
	private Boolean biayaPendaftaranSemuaGelombangSama;
	private Boolean wajibUploadFoto;
	private String kelasVerifikasiRapor;

	private Boolean aktif;
	private PerguruanTinggi perguruanTinggi;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "nama")
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public String toString() {
		return id + "-" + nama;
	}

	@Column(name = "keterangan")
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getKeterangan() {
		return keterangan;
	}

	public String getKode() {
		if (kode == null) {
			kode = "--";
		}
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public Integer getJumlahProdiYgBolehDiambil() {
		if (jumlahProdiYgBolehDiambil == null) {
			jumlahProdiYgBolehDiambil = 1;
		}
		return jumlahProdiYgBolehDiambil;
	}

	public void setJumlahProdiYgBolehDiambil(Integer jumlahProdiYgBolehDiambil) {
		this.jumlahProdiYgBolehDiambil = jumlahProdiYgBolehDiambil;
	}

	public Boolean getBisaDipilihSemuaGelombang() {
		if (bisaDipilihSemuaGelombang == null) {
			bisaDipilihSemuaGelombang = true;
		}
		return bisaDipilihSemuaGelombang;
	}

	public void setBisaDipilihSemuaGelombang(Boolean bisaDipilihSemuaGelombang) {
		this.bisaDipilihSemuaGelombang = bisaDipilihSemuaGelombang;
	}

	public Boolean getBisaMemilihPilihanYangSama() {
		if (bisaMemilihPilihanYangSama == null) {
			bisaMemilihPilihanYangSama = true;
		}
		return bisaMemilihPilihanYangSama;
	}

	public void setBisaMemilihPilihanYangSama(Boolean bisaMemilihPilihanYangSama) {
		this.bisaMemilihPilihanYangSama = bisaMemilihPilihanYangSama;
	}

	public String getKelasVerifikasiRapor() {
		return kelasVerifikasiRapor == null
				|| (!kelasVerifikasiRapor.trim().isEmpty() && !StringUtils.contains(kelasVerifikasiRapor, ":"))
						? "10:1;10:2;11:1;11:2;12:1;12:2"
						: kelasVerifikasiRapor.trim();
	}

	public void setKelasVerifikasiRapor(String kelasVerifikasiRapor) {
		this.kelasVerifikasiRapor = kelasVerifikasiRapor;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public Boolean getBiayaPendaftaranSemuaGelombangSama() {
		return biayaPendaftaranSemuaGelombangSama == null ? false : biayaPendaftaranSemuaGelombangSama;
	}

	public void setBiayaPendaftaranSemuaGelombangSama(Boolean biayaPendaftaranSemuaGelombangSama) {
		this.biayaPendaftaranSemuaGelombangSama = biayaPendaftaranSemuaGelombangSama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Paket.java:199");
		}
		return perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi;
	}

	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	public Boolean getWajibUploadFoto() {
		return wajibUploadFoto == null ? true : wajibUploadFoto;
	}

	public void setWajibUploadFoto(Boolean wajibUploadFoto) {
		this.wajibUploadFoto = wajibUploadFoto;
	}

}
