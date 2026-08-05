package ais.action.master.helper.virtualaccount;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyDoublebox;
import ais.ui.util.WaktuUtil;

public class DownloadTagihanMahasiswaBankBankaltimtara {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static VirtualAccountBank downloadData(Mahasiswa mahasiswa, Integer smt, JadwalPembayaran myjadwalPembayaran,
			Collection detailBiayas, Grid gridCicilan, Double biayaAdmin, boolean pakaiva) throws Exception {

		try {

			String detailbiaya = "";
			for (Object o : detailBiayas) {
				if (o instanceof DetailBiaya) {
					DetailBiaya biaya = (DetailBiaya) o;
					detailbiaya += (detailbiaya.isEmpty() ? biaya.getId() : "," + biaya.getId());
				}
			}

			String ket = "";
			String pemb = "";
			String cicilan = "";
			Double total = 0.0;
			JadwalPembayaran jdw = myjadwalPembayaran != null && myjadwalPembayaran.getKhususUntukNim() != null
					&& myjadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
							? myjadwalPembayaran
							: null;
			List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
			for (Row row : mycicilanrows) {
				MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

				if (jumlahCicilan.getValue() != null && jumlahCicilan.getValue().intValue() != 0) {
					CicilanPembayaran cicilanPembayaranSebelumnya = (CicilanPembayaran) row
							.getAttribute("cicilanPembayaran");
					if (cicilanPembayaranSebelumnya.getId() == null) {
						try {
							PengaturanPembayaranBulanan biaya = cicilanPembayaranSebelumnya
									.getPengaturanPembayaranBulanan();
							if (biaya != null) {

								Double nilai = jumlahCicilan.getValue();

								cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Bulanan-" + biaya.getId().toString() + "-" + nilai));

								Double hasilDenda = biaya.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), jdw,
										myjadwalPembayaran == null ? null : myjadwalPembayaran.getJenisKegiatan());

								String desc = biaya.getKeterangan();
								desc = (desc.isEmpty() ? (biaya.getDetailBiaya().getItemBiaya().getNama()) : desc)
										+ ", Rp. " + Common.numberFormat.get().format(nilai)
										+ (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

								ket += ket.isEmpty() ? biaya.getDetailBiaya().getItemBiaya().getNama()
										: "," + biaya.getDetailBiaya().getItemBiaya().getNama();

								pemb += biaya.getDetailBiaya().getItemBiaya().getKode().trim() + "," + desc + ";";
								total += nilai;
							} else {

								Double nilai = jumlahCicilan.getValue();

								Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
								ItemBiaya itemBiaya;
								DetailBiaya detailBiaya = (DetailBiaya) (myItemBiaya.getSelectedItem() == null ? null
										: myItemBiaya.getSelectedItem().getValue());
								if (cicilanPembayaranSebelumnya != null
										&& cicilanPembayaranSebelumnya.getItemBiaya() != null
										&& cicilanPembayaranSebelumnya.getItemBiaya().getId() != null) {
									itemBiaya = cicilanPembayaranSebelumnya.getItemBiaya();

								} else {
									itemBiaya = detailBiaya.getItemBiaya();
								}
								cicilan = MahasiswaVirtualAccountHelper.tambahTokenCicilan(cicilan, ("Item-" + itemBiaya.getId().toString() + "-" + nilai + "-"
												+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId()));

								String desc = itemBiaya.getNama() + ", Rp. " + Common.numberFormat.get().format(nilai);

								ket += ket.isEmpty() ? itemBiaya.getNama() : "," + itemBiaya.getNama();

								pemb += itemBiaya.getKode().trim() + "," + desc + ";";
								total += nilai;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBankaltimtara.java:113");
						}
					}
				}
			}

			Session session = MahasiswaVirtualAccountHelper.openSession();
			try {

			boolean tagihan_expired_akhir_hari = Common
					.getKonfigurasi("tagihan_expired_akhir_hari", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF);
			Date expired_date = myjadwalPembayaran.getEndDate();
			if (tagihan_expired_akhir_hari) {
				try {
					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.set(Calendar.HOUR_OF_DAY, 23);
					calendar.set(Calendar.MINUTE, 59);
					calendar.set(Calendar.SECOND, 59);
					expired_date = calendar.getTime();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBankaltimtara.java:134");
				}
			} else {
				String tagihan_expired_jam = Common.getKonfigurasi("tagihan_expired_jam", "").getNilai();
				if (!tagihan_expired_jam.isEmpty()) {
					if (!tagihan_expired_jam.isEmpty() && !tagihan_expired_jam.equalsIgnoreCase("0")) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.HOUR_OF_DAY,
									calendar.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(tagihan_expired_jam));
							expired_date = calendar.getTime();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBankaltimtara.java:146");
						}
					}
				} else {
					String tagihan_expired_day = Common.getKonfigurasi("tagihan_expired_day", "0").getNilai();

					if (!tagihan_expired_day.isEmpty() && !tagihan_expired_day.equalsIgnoreCase("0")) {
						try {
							Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
							calendar.set(Calendar.DATE,
									calendar.get(Calendar.DATE) + Integer.parseInt(tagihan_expired_day));
							expired_date = calendar.getTime();
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBankaltimtara.java:159");
						}
					}
				}
			}
			VirtualAccountBank virtualAccountBankBankaltimtara = (VirtualAccountBank) session
					.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("terjadiKendala", false))
					.add(Restrictions.ge("kadaluarsaWaktu", WaktuUtil.getDate()))
					.add(Restrictions.eq("keterangan", pemb)).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("semester", smt)).add(Restrictions.eq("pakaiva", pakaiva))
					.add(Restrictions.eq("jenisKegiatan", myjadwalPembayaran.getJenisKegiatan()))
					.add(Restrictions.isNull("kegiatan")).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			if (virtualAccountBankBankaltimtara == null) {
				JSONObject response = null;
				String postData = "";
				String va = "";
				int panjang = 15;
				try {
					panjang = Integer
							.parseInt(Common.getKonfigurasi("bankaltimtara_panjang_va", panjang + "").getNilai());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBankaltimtara.java:179");
				}

				va = Common.getKonfigurasi("bankaltimtara_prefix_va", "0099").getNilai()
						+ Common.getGeneratedAngkaDigit(panjang);
				int mytotal = total.intValue() + biayaAdmin.intValue();

				if (pakaiva) {

					JSONObject jsonObject = new JSONObject();

					System.out.println("Request body: ");

					String strURL = (Common.getKonfigurasi("bankaltimtara_gateway_url_autentication",
							"https://api-dev.bankaltimtara.co.id:8300/api/user/auth").getNilai());

					String user = Common.getKonfigurasi("bankaltimtara_username", "ubtva1").getNilai();
					String pwd = Common.getKonfigurasi("bankaltimtara_password", "12345678").getNilai();

					JSONObject login = new JSONObject();
					login.put("username", user);
					login.put("password", pwd);

					System.out.println(login + "");

					String[] command = { "curl", "--silent", "--show-error", "--location", strURL, "--header",
							"Content-type: application/json", "--data-raw", login.toString() };

					ProcessBuilder process = new ProcessBuilder(command);
					process.redirectErrorStream(true);
					Process p;
					p = process.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					StringBuilder builder = new StringBuilder();
					String line = null;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						builder.append(System.getProperty("line.separator"));
					}
					String hasil = builder.toString();
					System.out.println(hasil);

					JSONObject jSONObject = BankaltimtaraResponseUtil.parseJson(hasil, "autentikasi VA tagihan mahasiswa");

					String token = BankaltimtaraResponseUtil.ambilStringWajib(jSONObject, "token", "autentikasi VA tagihan mahasiswa");

					strURL = (Common.getKonfigurasi("bankaltimtara_gateway_url_create_va",
							"https://api-dev.bankaltimtara.co.id:8300/api/va/create").getNilai());

					
//				String inst_id = Common.getKonfigurasi("bankaltimtara_inst_id_va", "0099").getNilai();
//				jsonObject.put("inst_id", inst_id);
					jsonObject.put("number", va);
					jsonObject.put("name", Common.maxPanjang(mahasiswa.getNama() + " " + mahasiswa.getNim(), 30));
					jsonObject.put("amount", mytotal + "");
					jsonObject.put("description", Common.maxPanjang(ket, 30));

					postData = jsonObject.toString();

					System.out.println(postData + "");

					command = new String[] { "curl", "--silent", "--show-error", "--location", strURL, "--header",
							"Content-type: application/json", "--header", "Authorization: Bearer " + token,
							"--data-raw", postData };

					process = new ProcessBuilder(command);
					process.redirectErrorStream(true);

					p = process.start();
					reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					builder = new StringBuilder();
					line = null;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						builder.append(System.getProperty("line.separator"));
					}
					hasil = builder.toString();
					System.out.println(hasil);

					response = BankaltimtaraResponseUtil.parseJson(hasil, "pembuatan VA tagihan mahasiswa");
					BankaltimtaraResponseUtil.pastikanSukses(response, "pembuatan VA tagihan mahasiswa");

					if (response.getString("code").equals("00")) {
						virtualAccountBankBankaltimtara = new VirtualAccountBank(
								PerguruanTinggiUtil.getPerguruanTinggi().getId());
						virtualAccountBankBankaltimtara.setPakaiva(pakaiva);

						virtualAccountBankBankaltimtara.setKanalPembayaran(
								myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
										: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

						virtualAccountBankBankaltimtara.setKadaluarsa(expired_date);
						virtualAccountBankBankaltimtara.setOtomatis(false);
						virtualAccountBankBankaltimtara.setKode(va);
						virtualAccountBankBankaltimtara.setRequest(postData);
						virtualAccountBankBankaltimtara.setResponse(response == null ? "" : response.toString());
						virtualAccountBankBankaltimtara
								.setLink(response.isNull("barcode") ? "" : response.get("barcode") + "");
						virtualAccountBankBankaltimtara.setCicilan(cicilan);
						virtualAccountBankBankaltimtara.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
						virtualAccountBankBankaltimtara.setKeterangan(pemb);
						virtualAccountBankBankaltimtara.setTotal(total);
						virtualAccountBankBankaltimtara.setBulanan("");
						virtualAccountBankBankaltimtara.setDetailbiaya(detailbiaya);

						virtualAccountBankBankaltimtara.setMahasiswa(mahasiswa);
						virtualAccountBankBankaltimtara.setJadwalPembayaran(myjadwalPembayaran);
						virtualAccountBankBankaltimtara.setSemester(smt);
						virtualAccountBankBankaltimtara.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
						virtualAccountBankBankaltimtara.setBank("Bank Bankaltimtara");

						MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
						session.saveOrUpdate(virtualAccountBankBankaltimtara);
						MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
					}
				} else {
					virtualAccountBankBankaltimtara = new VirtualAccountBank(
							PerguruanTinggiUtil.getPerguruanTinggi().getId());
					virtualAccountBankBankaltimtara.setPakaiva(pakaiva);

					virtualAccountBankBankaltimtara.setKanalPembayaran(
							myjadwalPembayaran == null || myjadwalPembayaran.getJenisKegiatan() == null ? null
									: myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran());

					virtualAccountBankBankaltimtara.setKadaluarsa(expired_date);
					virtualAccountBankBankaltimtara.setOtomatis(false);
					virtualAccountBankBankaltimtara.setKode(va);
					virtualAccountBankBankaltimtara.setRequest(postData);
					virtualAccountBankBankaltimtara.setResponse(response == null ? "" : response.toString());
					virtualAccountBankBankaltimtara.setCicilan(cicilan);
					virtualAccountBankBankaltimtara.setJenisKegiatan(myjadwalPembayaran.getJenisKegiatan());
					virtualAccountBankBankaltimtara.setKeterangan(pemb);
					virtualAccountBankBankaltimtara.setTotal(total);
					virtualAccountBankBankaltimtara.setBulanan("");
					virtualAccountBankBankaltimtara.setDetailbiaya(detailbiaya);

					try {
						DownloadTagihanMahasiswaBankBankaltimtara.qris(mytotal, va, virtualAccountBankBankaltimtara);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBankaltimtara.java:314");
					}

					virtualAccountBankBankaltimtara.setMahasiswa(mahasiswa);
					virtualAccountBankBankaltimtara.setJadwalPembayaran(myjadwalPembayaran);
					virtualAccountBankBankaltimtara.setSemester(smt);
					virtualAccountBankBankaltimtara.setTahunAkademik(myjadwalPembayaran.getTahunAkademik());
					virtualAccountBankBankaltimtara.setBank("Bank Bankaltimtara");

					MahasiswaVirtualAccountHelper.beginTransactionIfNeeded(session);
					session.saveOrUpdate(virtualAccountBankBankaltimtara);
					MahasiswaVirtualAccountHelper.commitTransactionIfActive(session);
				}
			}

			return virtualAccountBankBankaltimtara;
		} finally {
			// Tutup session yang DIBUKA di FINALLY -> tak bocor pada jalur exception.
			MahasiswaVirtualAccountHelper.closeSessionQuietly(session);
			MahasiswaVirtualAccountHelper.closeHibernateContextQuietly();
		}
		} catch (Exception e) {
			// Sebelumnya HANYA tampilErrorJikaAdmin (popup, cuma kelihatan kalau yg login admin) --
			// utk user non-admin, kegagalan "tarik ulang virtual" jadi TANPA JEJAK sama sekali.
			// Tambah audit log spy penyebab asli (mis. IP outbound belum whitelist di bank, kredensial
			// salah, curl timeout) bisa ditelusuri dari log, bukan cuma popup generik "hubungi Admin".
			ais.common.ErrorAuditUtil.record(e,
					"DownloadTagihanMahasiswaBankBankaltimtara.downloadData: gagal membuat/menarik ulang VA/QRIS Bankaltimtara");
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	public static void qris(int mytotal, String va, VirtualAccountBank virtualAccountBankBankaltimtara)
			throws Exception {
		JSONObject jsonObject = new JSONObject();

		System.out.println("Request body: ");

		String strURL = (Common.getKonfigurasi("bankaltimtara_gateway_qris_url_autentication",
				"https://api-dev.bankaltimtara.co.id:8084/api/user/auth").getNilai());

		String user = Common.getKonfigurasi("bankaltimtara_qris_username", "qrisdev").getNilai();
		String pwd = Common.getKonfigurasi("bankaltimtara_qris_password", "PB@|1Kp@paN19112021").getNilai();

		JSONObject login = new JSONObject();
		login.put("username", user);
		login.put("password", pwd);

		System.out.println(login + "");

		String[] command = { "curl", "--silent", "--show-error", "--location", strURL, "--header",
				"Content-type: application/json", "--data-raw", login.toString() };

		ProcessBuilder process = new ProcessBuilder(command);
		process.redirectErrorStream(true);
		Process p;
		p = process.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		String hasil = builder.toString();
		System.out.println(hasil);

		try {

			JSONObject jSONObject = BankaltimtaraResponseUtil.parseJson(hasil, "autentikasi QRIS Bankaltimtara");

			String token = BankaltimtaraResponseUtil.ambilStringWajib(jSONObject, "token", "autentikasi QRIS Bankaltimtara");

			strURL = (Common.getKonfigurasi("bankaltimtara_gateway_qris_url_create_va",
					"https://api-dev.bankaltimtara.co.id:8084/api/qrismpm/generate").getNilai());

			int panjang = 15;
			try {
				panjang = Integer.parseInt(Common.getKonfigurasi("bankaltimtara_panjang_va", panjang + "").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/virtualaccount/DownloadTagihanMahasiswaBankBankaltimtara.java:387");
			}

			String inst_id = Common.getKonfigurasi("bankaltimtara_inst_id_qris", "211028001").getNilai();
			jsonObject.put("institution", inst_id);
			jsonObject.put("kd_tagihan", va);
			jsonObject.put("amount", mytotal + "");
			jsonObject.put("method", "12");

			String postData = jsonObject.toString();

			System.out.println(postData + "");

			command = new String[] { "curl", "--silent", "--show-error", "--location", strURL, "--header",
					"Content-type: application/json", "--header", "Authorization: Bearer " + token,
					"--data-raw", postData };

			process = new ProcessBuilder(command);
			process.redirectErrorStream(true);

			p = process.start();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			builder = new StringBuilder();
			line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			hasil = builder.toString();
			System.out.println(hasil);

			JSONObject response = BankaltimtaraResponseUtil.parseJson(hasil, "generate QRIS Bankaltimtara");

			virtualAccountBankBankaltimtara.setBarcode(response.get("barcode") + "");

			virtualAccountBankBankaltimtara
					.setKadaluarsaBarcode(Common.databaseDateFormat1.get().parse(response.get("expired_time") + ""));

		} catch (Exception e) {
			virtualAccountBankBankaltimtara.setHtmlTemporaryData(hasil);
		}

	}

}
