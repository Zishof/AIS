package ais.action.servlet.api;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Kumpulan handler API (dipanggil oleh dispatcher servlet API dengan konvensi
 * {@code (HttpServletRequest, JSONObject) -> JSONObject}) untuk modul "Tabungan Siswa" — sistem
 * deposit/tabungan siswa yang dipakai sebagai alat pembayaran non-tunai di kantin/koperasi
 * sekolah (modul inventory: {@link Produk}, {@link Toko}, {@link Pembelian}).
 *
 * <p>
 * Setiap method memvalidasi token akses lewat {@code ApiUtil#currentUser} terlebih dahulu
 * (mengembalikan {@code status "97"} bila token tidak valid), lalu memvalidasi keberadaan entitas
 * yang dirujuk (siswa/mahasiswa) sebelum memproses. Kode status pada respons JSON mengikuti
 * konvensi: {@code "97"} = data/parameter tidak ditemukan atau token tidak sesuai, {@code "90"} =
 * galat internal (pesan diambil dari {@code Common#tampilErrorJikaAdmin}), tidak ada field
 * {@code status} eksplisit berarti sukses.
 * </p>
 */
public class TabunganSiswa {

	/**
	 * Mencatat transaksi pembelian siswa di kantin/koperasi sekolah: untuk setiap produk pada
	 * {@code request}, membuat atau memperbarui data {@link Produk} (berdasarkan kode + toko),
	 * lalu menyimpan/menimpa baris {@link Pembelian} dengan kode transaksi idempoten
	 * {@code kode+"_"+kodeProduk+"_"+kodeToko+"_"+idSiswa} (sehingga pengiriman ulang permintaan
	 * yang sama tidak menghasilkan pembelian dobel). Mengembalikan daftar nama item yang
	 * terbeli beserta profil ringkas siswa dan sisa saldo tabungan terkini.
	 *
	 * @param req     request HTTP asli, sumber header autentikasi
	 * @param request payload JSON berisi {@code siswa} (id), {@code toko} (kode),
	 *                {@code kode} (kode transaksi), dan {@code produks} (array item: kode, nama,
	 *                harga, qty)
	 * @return JSON hasil pencatatan pembelian beserta profil dan saldo tabungan siswa, atau JSON
	 *         berisi {@code status} galat bila token/parameter tidak valid
	 */
	@SuppressWarnings({})
	public static JSONObject pembelian_siswa(HttpServletRequest req, JSONObject request) {

		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				Siswa siswa = (Siswa) (request.isNull("siswa") ? null
						: ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(request.get("siswa").toString())));
				if (siswa != null) {

					if (request.isNull("produks")) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "produks tidak ditemukan");
					} else {
						if (request.isNull("kode")) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "kode tidak ditemukan");
						} else {

							Toko toko = (Toko) (request.isNull("toko") ? null
									: ConstantValues.ambilByKode(Toko.class.getName(), request.get("toko").toString()));

							if (toko == null) {
								jsonObject.put("status", "97");
								jsonObject.put("description", "toko tidak ditemukan");
							} else {

								String kode = request.get("kode") + "";

								JSONArray produks = request.getJSONArray("produks");
								Session session = HibernateUtil.openSession();
								try {
								JSONArray data = new JSONArray();
								for (int i = 0; i < produks.length(); i++) {
									JSONObject produk = produks.getJSONObject(i);

									Produk produkdata = (Produk) ConstantValues
											.simpleObject(
													session.createCriteria(Produk.class)
															.add(Restrictions.eq("kode", produk.get("kode") + ""))
															.add(Restrictions.eq("toko", toko)).setMaxResults(1),
													Produk.class);
									if (produkdata == null) {
										produkdata = new Produk();
										produkdata.setKode(produk.get("kode") + "");
										produkdata.setHargaBeli(Double.parseDouble(produk.get("harga") + ""));
										produkdata.setToko(toko);
									}

									produkdata.setNama(produk.get("nama") + "");
									produkdata.setHargaJual(Double.parseDouble(produk.get("harga") + ""));

									session.getTransaction().begin();
									session.saveOrUpdate(produkdata);
									session.getTransaction().commit();

									String kodeBeli = kode + "_" + produkdata.getKode() + "_" + toko.getKode() + "_"
											+ siswa.getId();

									Pembelian pembelian = (Pembelian) session.createCriteria(Pembelian.class)
											.add(Restrictions.eq("kode", kodeBeli)).setMaxResults(1).uniqueResult();
									if (pembelian == null) {
										pembelian = new Pembelian();
									}
									pembelian.setKode(kodeBeli);
									pembelian.setSiswa(siswa);
									pembelian.setQty(Double.parseDouble(produk.get("qty") + ""));
									pembelian.setHargaSatuan(produkdata.getHargaJual());
									pembelian.setProduk(produkdata);
									pembelian.setToko(toko);
									session.getTransaction().begin();
									session.saveOrUpdate(pembelian);
									session.getTransaction().commit();

									data.put(pembelian.getNama());
								}

								// session.disconnect();
								ApiHelperSupport.closeOpenedSession(session);

								jsonObject.put("data", data);
								jsonObject.put("nis", siswa.getNomorInduk());
								jsonObject.put("nisn", siswa.getNomorIndukNasional());
								jsonObject.put("nama", siswa.getNama());
								jsonObject.put("sekolah", siswa.getSekolah().getNama());
								String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa));
								jsonObject.put("foto", url);
								Double tabungan = siswa.hitungSisaDeposit(WaktuUtil.getDate());
								jsonObject.put("tabungan", tabungan);

								} finally {
									HibernateUtil.closeSessionQuietly(session);
								}
							}
						}
					}
				} else {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Siswa tidak ditemukan");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TabunganSiswa.java:136");
			}
		}
		return jsonObject;
	}

	/**
	 * Mengembalikan profil ringkas siswa (NIS, NISN, nama, sekolah, foto) beserta sisa saldo
	 * tabungan pada tanggal berjalan.
	 *
	 * @param request payload JSON berisi {@code siswa} (id)
	 * @return JSON profil dan saldo tabungan siswa, atau JSON berisi {@code status} galat bila
	 *         token tidak valid atau siswa tidak ditemukan
	 */
	@SuppressWarnings({})
	public static JSONObject tabungan_siswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				Siswa siswa = (Siswa) (request.isNull("siswa") ? null
						: ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(request.get("siswa").toString())));
				if (siswa != null) {
					jsonObject.put("nis", siswa.getNomorInduk());
					jsonObject.put("nisn", siswa.getNomorIndukNasional());
					jsonObject.put("nama", siswa.getNama());
					jsonObject.put("sekolah", siswa.getSekolah().getNama());
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa));
					jsonObject.put("foto", url);
					Double tabungan = siswa.hitungSisaDeposit(WaktuUtil.getDate());
					jsonObject.put("tabungan", tabungan);
				} else {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Siswa tidak ditemukan");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TabunganSiswa.java:174");
			}
		}
		return jsonObject;
	}

	/**
	 * Mengembalikan profil ringkas siswa beserta rincian daftar komponen saldo tabungan (bukan
	 * hanya total) pada tanggal berjalan, lewat {@code Siswa#daftarSisaDeposit}.
	 *
	 * @param request payload JSON berisi {@code siswa} (id)
	 * @return JSON profil siswa beserta rincian daftar tabungan, atau JSON berisi {@code status}
	 *         galat bila token tidak valid atau siswa tidak ditemukan
	 */
	@SuppressWarnings({})
	public static JSONObject daftar_tabungan_siswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Siswa siswa = (Siswa) (request.isNull("siswa") ? null
						: ConstantValues.ambil(Siswa.class.getName(), Long.parseLong(request.get("siswa").toString())));
				if (siswa != null) {
					jsonObject.put("nis", siswa.getNomorInduk());
					jsonObject.put("nisn", siswa.getNomorIndukNasional());
					jsonObject.put("nama", siswa.getNama());
					jsonObject.put("sekolah", siswa.getSekolah().getNama());
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa));
					jsonObject.put("foto", url);
					JSONObject tabungan = siswa.daftarSisaDeposit(WaktuUtil.getDate());
					jsonObject.put("daftar", tabungan);
				} else {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Siswa tidak ditemukan");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TabunganSiswa.java:211");
			}
		}
		return jsonObject;
	}

	/**
	 * Mengembalikan profil ringkas mahasiswa (NIM, prodi, nama, foto) beserta saldo tabungan,
	 * dihitung lewat {@code DepositHelper#hitungDeposit} (jalur perhitungan berbeda dari deposit
	 * siswa sekolah, karena entitas dan skema deposit mahasiswa terpisah dari siswa).
	 *
	 * @param request payload JSON berisi {@code mahasiswa} (id)
	 * @return JSON profil dan saldo tabungan mahasiswa, atau JSON berisi {@code status} galat
	 *         bila token tidak valid atau mahasiswa tidak ditemukan
	 */
	@SuppressWarnings({})
	public static JSONObject tabungan_mahasiswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				Mahasiswa mahasiswa = (Mahasiswa) (request.isNull("mahasiswa") ? null
						: ConstantValues.ambil(Mahasiswa.class.getName(),
								Long.parseLong(request.get("mahasiswa").toString())));
				if (mahasiswa != null) {
					jsonObject.put("nim", mahasiswa.getNim());
					jsonObject.put("prodi", mahasiswa.getJurusan().getNama());
					jsonObject.put("nama", mahasiswa.getNama());
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa));
					jsonObject.put("foto", url);
					Double tabungan = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(mahasiswa);
					jsonObject.put("tabungan", tabungan);
				} else {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Mahasiswa tidak ditemukan");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TabunganSiswa.java:249");
			}
		}
		return jsonObject;
	}
}
