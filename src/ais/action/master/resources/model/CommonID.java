package ais.action.master.resources.model;


/**
 * DTO generik untuk endpoint web service JAX-RS (paket {@code ais.action.master.resources}):
 * membawa {@code id} beserta hingga 23 kolom informasi tambahan bertipe teks generik
 * ({@code info1}..{@code info23}) yang maknanya berbeda-beda tergantung konteks pemanggilnya —
 * dipakai sebagai bentuk respons ringkas untuk data referensi/lookup yang jumlah dan urutan
 * kolomnya bervariasi antar entitas, tanpa perlu membuat kelas DTO baru untuk setiap kasus.
 * Seluruhnya berupa field data polos (getter/setter trivial).
 */
public class CommonID {
	private Long id;
	private String info1;
	private String info2;
	private String info3;
	private String info4;
	private String info5;
	private String info6;
	private String info7;
	private String info8;
	private String info9;
	private String info10;
	private String info11;
	private String info12;
	private String info13;
	private String info14;
	private String info15;
	private String info16;
	private String info17;
	private String info18;
	private String info19;
	private String info20;
	private String info21;
	private String info22;
	private String info23;

	public CommonID() {
	}

	public CommonID(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getInfo1() {
		return info1;
	}

	public void setInfo1(String info1) {
		this.info1 = info1;
	}

	public String getInfo2() {
		return info2;
	}

	public void setInfo2(String info2) {
		this.info2 = info2;
	}

	public String getInfo3() {
		return info3;
	}

	public void setInfo3(String info3) {
		this.info3 = info3;
	}

	public String getInfo4() {
		return info4;
	}

	public void setInfo4(String info4) {
		this.info4 = info4;
	}

	public String getInfo5() {
		return info5;
	}

	public void setInfo5(String info5) {
		this.info5 = info5;
	}

	public String getInfo6() {
		return info6;
	}

	public void setInfo6(String info6) {
		this.info6 = info6;
	}

	public String getInfo7() {
		return info7;
	}

	public void setInfo7(String info7) {
		this.info7 = info7;
	}

	public String getInfo8() {
		return info8;
	}

	public void setInfo8(String info8) {
		this.info8 = info8;
	}

	public String getInfo9() {
		return info9;
	}

	public void setInfo9(String info9) {
		this.info9 = info9;
	}

	public String getInfo10() {
		return info10;
	}

	public void setInfo10(String info10) {
		this.info10 = info10;
	}

	public String getInfo11() {
		return info11;
	}

	public void setInfo11(String info11) {
		this.info11 = info11;
	}

	public String getInfo12() {
		return info12;
	}

	public void setInfo12(String info12) {
		this.info12 = info12;
	}

	public String getInfo13() {
		return info13;
	}

	public void setInfo13(String info13) {
		this.info13 = info13;
	}

	public String getInfo14() {
		return info14;
	}

	public void setInfo14(String info14) {
		this.info14 = info14;
	}

	public String getInfo15() {
		return info15;
	}

	public void setInfo15(String info15) {
		this.info15 = info15;
	}

	public String getInfo16() {
		return info16;
	}

	public void setInfo16(String info16) {
		this.info16 = info16;
	}

	public String getInfo17() {
		return info17;
	}

	public void setInfo17(String info17) {
		this.info17 = info17;
	}

	public String getInfo18() {
		return info18;
	}

	public void setInfo18(String info18) {
		this.info18 = info18;
	}

	public String getInfo19() {
		return info19;
	}

	public void setInfo19(String info19) {
		this.info19 = info19;
	}

	public String getInfo20() {
		return info20;
	}

	public void setInfo20(String info20) {
		this.info20 = info20;
	}

	public String getInfo21() {
		return info21;
	}

	public void setInfo21(String info21) {
		this.info21 = info21;
	}

	public String getInfo22() {
		return info22;
	}

	public void setInfo22(String info22) {
		this.info22 = info22;
	}

	public String getInfo23() {
		return info23;
	}

	public void setInfo23(String info23) {
		this.info23 = info23;
	}
}
