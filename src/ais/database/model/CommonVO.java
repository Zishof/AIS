package ais.database.model;

import java.io.Serializable;

/**
 * Kelas dasar value object/model AIS untuk common vo. Tipe ini menyatukan identitas, metadata
 * umum, representasi, serta perilaku lintas entity yang benar-benar berlaku bagi turunannya.
 *
 * <p><b>Batas tanggung jawab:</b> interface serialisasi/perbandingan hanya mendukung binding dan collection.
 * Tipe ini tetap merupakan pembawa data; validasi, transaksi, dan aturan domain harus berada pada service agar
 * tidak muncul sumber aturan paralel.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String id}, {@code String name}, {@code
 * String name1}, {@code String name2}, {@code String name3}, {@code String name4}, {@code String name5}, {@code
 * Double mulai}; pembacaan/pencarian ({@code getId()}, {@code getName()}, {@code getName1()}, {@code
 * getName2()}, {@code getName3()}, {@code getName4()}); mutasi data ({@code setId()}, {@code setName()}, {@code
 * setName1()}, {@code setName2()}, {@code setName3()}, {@code setName4()}); operasi domain lain ({@code
 * toString()}, {@code compareTo()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor, mutator, dan pembanding hanya membaca atau mengubah state value object di
 * memori. Tipe ini tidak membuka session, menjalankan transaksi, atau memuat data sendiri.</p>
 */
public class CommonVO implements Serializable, Comparable<CommonVO> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8737675485624100335L;

	/**
	 * Creates a new instance of CommonVO
	 */
	public CommonVO() {
	}

	public CommonVO(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public CommonVO(String id, String name, GeneralValueObject valueObject) {
		this.id = id;
		this.name = name;
		this.valueObject = valueObject;
	}

	public CommonVO(String id, String name, GeneralValueObject valueObject, String name1) {
		this.id = id;
		this.name = name;
		this.valueObject = valueObject;
		this.name1 = name1;
	}

	public CommonVO(String id, String name, String name1) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
	}

	public CommonVO(String id, String name, String name1, String name2) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
		this.name2 = name2;
	}

	public CommonVO(String id, String name, String name1, String name2, String name3) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
		this.name2 = name2;
		this.name3 = name3;
	}

	// public CommonVO(String id, String name, String name1, String name2,
	// String name3, String name4) {
	// this.id = id;
	// this.name = name;
	// this.name1 = name1;
	// this.name2 = name2;
	// this.name3 = name3;
	// this.name4 = name4;
	// }

	public CommonVO(String id, String name, String name1, String name2, String name3, String name4, String name5) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
		this.name2 = name2;
		this.name3 = name3;
		this.name4 = name4;
		this.name5 = name5;
	}

	private String id;
	private String name;
	private String name1;
	private String name2;
	private String name3;
	private String name4;
	private String name5;

	private Double mulai = 0.0;
	private Double sampai = 0.0;
	private Double maksimal = Double.MAX_VALUE;
	private Double nilai = 0.0;
	private Boolean persen;
	private Boolean masing;
	private Boolean dibagi;

	private Integer nomorUrut;

	private GeneralValueObject valueObject;

	public String toString() {
		return name + "==>" + mulai + "-" + sampai + "-" + nilai + "-" + persen + "-" + masing;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName1() {
		return name1;
	}

	public void setName1(String name1) {
		this.name1 = name1;
	}

	public String getName2() {
		return name2;
	}

	public void setName2(String name2) {
		this.name2 = name2;
	}

	public String getName3() {
		return name3 == null ? "" : name3;
	}

	public void setName3(String name3) {
		this.name3 = name3;
	}

	public String getName4() {
		return name4;
	}

	public void setName4(String name4) {
		this.name4 = name4;
	}

	public String getName5() {
		return name5 == null ? "" : name5.trim();
	}

	public void setName5(String name5) {
		this.name5 = name5;
	}

	public Double getMulai() {
		return mulai;
	}

	public void setMulai(Double mulai) {
		this.mulai = mulai;
	}

	public Double getSampai() {
		return sampai;
	}

	public void setSampai(Double sampai) {
		this.sampai = sampai;
	}

	public Double getNilai() {
		return nilai;
	}

	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	public Boolean getPersen() {
		return persen == null ? false : persen;
	}

	public void setPersen(Boolean persen) {
		this.persen = persen;
	}

	public Boolean getMasing() {
		return masing == null ? false : masing;
	}

	public void setMasing(Boolean masing) {
		this.masing = masing;
	}

	public Double getMaksimal() {
		return maksimal;
	}

	public void setMaksimal(Double maksimal) {
		this.maksimal = maksimal;
	}

	public Boolean getDibagi() {
		return dibagi == null ? false : dibagi;
	}

	public void setDibagi(Boolean dibagi) {
		this.dibagi = dibagi;
	}

	public GeneralValueObject getValueObject() {
		return valueObject;
	}

	public void setValueObject(GeneralValueObject valueObject) {
		this.valueObject = valueObject;
	}

	@Override
	public int compareTo(CommonVO o) {
		int compare = 0;
		if (getName5().trim().isEmpty()) {

			compare = getNomorUrut().compareTo(o.getNomorUrut());
			if (compare == 0) {
				try {
					compare = getId().compareTo(o.getId());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CommonVO.java:230");

				}
			}
			if (compare == 0) {
				try {
					compare = getName().compareTo(o.getName());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CommonVO.java:237");

				}
			}
		} else {
			compare = (getName5() + " " + getNomorUrut()).compareTo((o.getName5() + " " + o.getNomorUrut()));
		}
		return compare;
	}

	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}
}
