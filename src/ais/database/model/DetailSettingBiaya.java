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

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "detail_setting_biaya")
public class DetailSettingBiaya extends GeneralValueObject {
	private static final long serialVersionUID = -7050166125892447098L;
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

	public String toString() {
		settingBiaya = getSettingBiaya();
		itemBiaya = getItemBiaya();
		return id + "-" + settingBiaya + "-" + itemBiaya + "-" + bayarKe;
	}

	private SettingBiaya settingBiaya;
	private ItemBiaya itemBiaya;
	private Double defaultBiaya;
	private Date defaultTanggalTagihan;
	private Date defaultTanggalDeadline;
	private String defaultKeterangan;
	private String biayaPerProdi;
	private Integer bayarKe;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);
		return this.itemBiaya;
	}

	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	public void setSettingBiaya(SettingBiaya settingBiaya) {
		this.settingBiaya = settingBiaya;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "setting_biaya", nullable = true)
	public SettingBiaya getSettingBiaya() {
		settingBiaya = check(settingBiaya);
		return settingBiaya;
	}

	public Double getDefaultBiaya() {
		return defaultBiaya == null ? 0.0 : defaultBiaya;
	}

	public void setDefaultBiaya(Double defaultBiaya) {
		this.defaultBiaya = defaultBiaya;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getDefaultTanggalTagihan() {
		return defaultTanggalTagihan;
	}

	public void setDefaultTanggalTagihan(Date defaultTanggalTagihan) {
		this.defaultTanggalTagihan = defaultTanggalTagihan;
	}

	public Integer getBayarKe() {
		return bayarKe == null ? 1 : bayarKe;
	}

	public void setBayarKe(Integer bayarKe) {
		this.bayarKe = bayarKe;
	}

	private static JSONObject jsonObject = new JSONObject();

	@Column(columnDefinition = "text")
	public String getBiayaPerProdi() {
		return biayaPerProdi == null || biayaPerProdi.trim().isEmpty() ? jsonObject.toString() : biayaPerProdi;
	}

	public void setBiayaPerProdi(String biayaPerProdi) {
		this.biayaPerProdi = biayaPerProdi;
	}

	public Double ambilDefaultBiaya(Jurusan jurusan) {
		Double biaya = 0.0;
		try {
			MyJSONObject jsonObject = new MyJSONObject(getBiayaPerProdi());
			biaya = jsonObject.isNull("b_" + jurusan.getId()) ? 0.0 : jsonObject.getDouble("b_" + jurusan.getId());
			jsonObject = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DetailSettingBiaya.java:163");
			// TODO: handle exception
		}
		return biaya;
	}

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

	@Column(columnDefinition = "text")
	public String getDefaultKeterangan() {
		return defaultKeterangan == null ? "" : defaultKeterangan;
	}

	public void setDefaultKeterangan(String defaultKeterangan) {
		this.defaultKeterangan = defaultKeterangan;
	}

	@Temporal(TemporalType.DATE)
	public Date getDefaultTanggalDeadline() {
		return defaultTanggalDeadline;
	}

	public void setDefaultTanggalDeadline(Date defaultTanggalDeadline) {
		this.defaultTanggalDeadline = defaultTanggalDeadline;
	}
}
