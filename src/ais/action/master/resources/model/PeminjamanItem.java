package ais.action.master.resources.model;

import java.util.ArrayList;
import java.util.List;

import ais.database.model.CommonVO;
import ais.database.model.library.JenisIdentitasAnggota;
import ais.database.model.library.TipeAnggota;


public class PeminjamanItem {
	public Long id;
	public Double jumlah;
	// public Anggota anggota;
	public String kodePeminjaman;
	public String kodeIdentitas;
	public String jenisIdentitas;
	public String kode;
	public String nama;
	public String alamat;
	public String tipe;
	public String keterangan;
	public String telp;
	public String hp;
	public String email;
	public JenisIdentitasAnggota jenisIdentitasAnggota;
	public TipeAnggota tipeAnggota;
	public List<CommonVO> data = new ArrayList<CommonVO>();
	public String info;
	public String error;
	public Integer perpanjang = 0;
	public Integer maksimal = 0;

	// public PeminjamanItem() {
	// }
	//
	// public PeminjamanItem(Long id) {
	// this.id = id;
	// }
	//
	// public Long getId() {
	// return id;
	// }
	//
	// public void setId(Long id) {
	// this.id = id;
	// }
	//
	// public String getKodeIdentitas() {
	// return kodeIdentitas;
	// }
	//
	// public void setKodeIdentitas(String kodeIdentitas) {
	// this.kodeIdentitas = kodeIdentitas;
	// }
	//
	// public String getJenisIdentitas() {
	// return jenisIdentitas;
	// }
	//
	// public void setJenisIdentitas(String jenisIdentitas) {
	// this.jenisIdentitas = jenisIdentitas;
	// }
	//
	// public String getKode() {
	// return kode;
	// }
	//
	// public void setKode(String kode) {
	// this.kode = kode;
	// }
	//
	// public String getNama() {
	// return nama;
	// }
	//
	// public void setNama(String nama) {
	// this.nama = nama;
	// }
	//
	// public String getAlamat() {
	// return alamat;
	// }
	//
	// public void setAlamat(String alamat) {
	// this.alamat = alamat;
	// }
	//
	// public String getTipe() {
	// return tipe;
	// }
	//
	// public void setTipe(String tipe) {
	// this.tipe = tipe;
	// }
	//
	// public String getKeterangan() {
	// return keterangan;
	// }
	//
	// public void setKeterangan(String keterangan) {
	// this.keterangan = keterangan;
	// }
	//
	// public String getTelp() {
	// return telp;
	// }
	//
	// public void setTelp(String telp) {
	// this.telp = telp;
	// }
	//
	// public String getHp() {
	// return hp;
	// }
	//
	// public void setHp(String hp) {
	// this.hp = hp;
	// }
	//
	// public String getEmail() {
	// return email;
	// }
	//
	// public void setEmail(String email) {
	// this.email = email;
	// }
	//
	// public JenisIdentitasAnggota getJenisIdentitasAnggota() {
	// return jenisIdentitasAnggota;
	// }
	//
	// public void setJenisIdentitasAnggota(
	// JenisIdentitasAnggota jenisIdentitasAnggota) {
	// this.jenisIdentitasAnggota = jenisIdentitasAnggota;
	// }
	//
	// public TipeAnggota getTipeAnggota() {
	// return tipeAnggota;
	// }
	//
	// public void setTipeAnggota(TipeAnggota tipeAnggota) {
	// this.tipeAnggota = tipeAnggota;
	// }
	//
	// public List<HashMap<String, Object>> getData() {
	// return data;
	// }
	//
	// public void setData(List<HashMap<String, Object>> data) {
	// this.data = data;
	// }

}
