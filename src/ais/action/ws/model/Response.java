package ais.action.ws.model;

import java.io.Serializable;

public class Response implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6003625639578807696L;

	private String response_code = "";
	private String response_description = "";
	private String nim = "";
	private String kurs = "";
	private String nama = "";
	private String program = "";
	private String fakultas = "";
	private String prodi = "";
	private String angkatan = "";
	private String semester = "";
	private String semester_ke = "";
	private String amount = "";
	private String total_amount = "";
	private String kode_status_pembayaran = "";
	private String keterangan_status_pembayaran = "";
	private String reference_number = "";
	private String info1 = "";
	private String info2 = "";
	private String info3 = "";
	private String info4 = "";
	private String info6 = "";
	private String info7 = "";
	private String info8 = "";
	private String info9 = "";
	private String info10 = "";

	public String getResponse_code() {
		return response_code;
	}

	public void setResponse_code(String response_code) {
		this.response_code = response_code;
	}

	public String getResponse_description() {
		return response_description;
	}

	public void setResponse_description(String response_description) {
		this.response_description = response_description;
	}

	public String getNim() {
		return nim;
	}

	public void setNim(String nim) {
		this.nim = nim;
	}

	public String getKurs() {
		return kurs;
	}

	public void setKurs(String kurs) {
		this.kurs = kurs;
	}

	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public String getProgram() {
		return program;
	}

	public void setProgram(String program) {
		this.program = program;
	}

	public String getFakultas() {
		return fakultas;
	}

	public void setFakultas(String fakultas) {
		this.fakultas = fakultas;
	}

	public String getProdi() {
		return prodi;
	}

	public void setProdi(String prodi) {
		this.prodi = prodi;
	}

	public String getAngkatan() {
		return angkatan;
	}

	public void setAngkatan(String angkatan) {
		this.angkatan = angkatan;
	}

	public String getSemester() {
		return semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	public String getSemester_ke() {
		return semester_ke;
	}

	public void setSemester_ke(String semester_ke) {
		this.semester_ke = semester_ke;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getTotal_amount() {
		return total_amount == null || total_amount.trim().isEmpty() ? "0" : total_amount;
	}

	public void setTotal_amount(String total_amount) {
		this.total_amount = total_amount;
	}

	public String getKode_status_pembayaran() {
		return kode_status_pembayaran;
	}

	public void setKode_status_pembayaran(String kode_status_pembayaran) {
		this.kode_status_pembayaran = kode_status_pembayaran;
	}

	public String getKeterangan_status_pembayaran() {
		return keterangan_status_pembayaran;
	}

	public void setKeterangan_status_pembayaran(String keterangan_status_pembayaran) {
		this.keterangan_status_pembayaran = keterangan_status_pembayaran;
	}

	public String getReference_number() {
		return reference_number;
	}

	public void setReference_number(String reference_number) {
		this.reference_number = reference_number;
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

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
