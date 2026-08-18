package ais.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.report.Report;
import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.finpay.FinpayRequestDetail;
import ais.database.model.finpay.FinpayRequestDetailBiaya;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class FinpayCommon {

	public static SHA256InJava sha256InJava = new SHA256InJava();

	@SuppressWarnings("unchecked")
	public static List<FinpayRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas = new ArrayList<FinpayRequestDetailBiaya>();
		Rows rows = (Rows) gridss.getRows();
		if (rows != null && rows.getChildren() != null) {
			List<Row> myRows = rows.getChildren();
			for (Row row : myRows) {
				if (!row.isVisible()) {
					continue;
				}
				DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");

				Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru();
				try {
					Component component = (Component) row.getAttribute("tag");
					if (component instanceof Doublebox && detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
						Doublebox jumlah = (Doublebox) component;
						biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
					} else if (component instanceof Label) {
						Label myLabel = (Label) component;
						// System.out.println("myLabel = " +
						// myLabel.getValue());
						biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FinpayCommon.java:80");
				}

				if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					for (MyDoubleboxMin kurang : pengurangan) {
						DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
						if (penguranganItemBiaya != null
								&& penguranganItemBiaya.getId().equals(detailBiaya.getId())) {
							biaya = kurang.getValue() == null ? 0.0 : kurang.getValue();
							break;
						}
					}
				}

				FinpayRequestDetailBiaya finpayRequestDetailBiaya = new FinpayRequestDetailBiaya();
				finpayRequestDetailBiaya.setDetailBiaya(detailBiaya);
				finpayRequestDetailBiaya.setNilai(biaya);
				finpayRequestDetailBiayas.add(finpayRequestDetailBiaya);
			}
		}
		return finpayRequestDetailBiayas;
	}

	public static List<FinpayRequestDetail> populateFinpayRequestDetailDariDetailBiaya(
			List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas) {
		List<FinpayRequestDetail> finpayRequestDetails = new ArrayList<FinpayRequestDetail>();

		int i = 1;
		for (FinpayRequestDetailBiaya finpayRequestDetailBiaya : finpayRequestDetailBiayas) {
			FinpayRequestDetail finpayRequestDetail = new FinpayRequestDetail();
			finpayRequestDetail.setPengaturanPembayaranBulanan(null);
			finpayRequestDetail.setItemBiaya(finpayRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			finpayRequestDetail.setKeterangan(finpayRequestDetailBiaya.getKeterangan());
			finpayRequestDetail.setNilai(finpayRequestDetailBiaya.getNilai());
			finpayRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			finpayRequestDetail.setKe(i);
			finpayRequestDetails.add(finpayRequestDetail);
			i++;
		}

		return finpayRequestDetails;
	}

	public static List<FinpayRequestDetail> populateFinpayRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<FinpayRequestDetail> finpayRequestDetails = new ArrayList<FinpayRequestDetail>();

		int i = 1;
		for (Row row : mycicilanrows) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan.getValue() != null
					&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {

				CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null && row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);

				String val = cicilanPembayaran == null ? null : cicilanPembayaran.getValidator();
				if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null")) {
					Tbmuser tbmuser = Common.getCurrentUser();
					val = (tbmuser == null ? "" : tbmuser.toString());

				}

				FinpayRequestDetail finpayRequestDetail = new FinpayRequestDetail();

				Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
						: myItemBiaya.getSelectedItem().getValue();
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
						.getPengaturanPembayaranBulanan();
				ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();
				DetailBiaya detailBiaya = null;
				if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
					itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				} else if (jenisBiaya != null && jenisBiaya instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) jenisBiaya;
					itemBiaya = detailBiaya.getItemBiaya();
				}

				finpayRequestDetail.setDetailBiaya(detailBiaya);
				finpayRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				finpayRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				finpayRequestDetail.setItemBiaya(itemBiaya);
				finpayRequestDetail.setKeterangan(keterangan.getValue());
				finpayRequestDetail.setNilai(jumlahCicilan.getValue());
				finpayRequestDetail.setTanggal(tanggal.getValue());
				finpayRequestDetail.setKe(i);

				finpayRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				finpayRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, finpayRequestDetail.getTanggal(),
								jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						finpayRequestDetail.setDenda(denda);
						finpayRequestDetail.setNilaiAsli(nom);
					}
				}

				finpayRequestDetails.add(finpayRequestDetail);
				i++;
			}
		}

		return finpayRequestDetails;
	}

	public static void bayarCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa, JenisKegiatan jenisKegiatan)
			throws Exception {
		Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		if (prodiLulus == null || prodiLulus.getId() == null) {
			Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
					: calonMahasiswa.getProdi1();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, myjurusan1, false);
			detailBiayas.addAll(detailBiayas1);
		} else {
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, prodiLulus, false);
			detailBiayas.addAll(detailBiayas1);
		}

		if (!detailBiayas.isEmpty()) {

			Serializable[] serializables = PembayaranUtil.getInstance()
					.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(calonMahasiswa.getTanggalDaftar(),
							jenisKegiatan, calonMahasiswa.getJenjang(), calonMahasiswa.getTahunAkademik(),
							calonMahasiswa.getGelombangPendaftaran().getJenisSemester().equalsIgnoreCase(
									Perkuliahan.GANJIL),
							calonMahasiswa.getJenisSeleksi(), calonMahasiswa.getProgram(),
							calonMahasiswa.getNoRegistrasi(), calonMahasiswa.getGelombangPendaftaran());
			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

			if (jadwalPembayaran != null) {
				Double nilaiBiayaHarusDiBayars = 0.0;

				List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas = new ArrayList<FinpayRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					FinpayRequestDetailBiaya finpayRequestDetailBiaya = new FinpayRequestDetailBiaya();
					finpayRequestDetailBiaya.setDetailBiaya(detailBiaya);
					finpayRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					finpayRequestDetailBiayas.add(finpayRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += finpayRequestDetailBiaya.getNilai();
				}

				FinpayCommon.onSaveFinpay(nilaiBiayaHarusDiBayars, null, calonMahasiswa, jenisKegiatan,
						jadwalPembayaran, 1, calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru",
						0.0, nilaiBiayaHarusDiBayars,
						FinpayCommon.populateFinpayRequestDetailDariDetailBiaya(finpayRequestDetailBiayas),
						finpayRequestDetailBiayas, null);

			}
		}

	}

	@SuppressWarnings({})
	public static boolean onSaveFinpay(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<FinpayRequestDetail> finpayRequestDetails,
			List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas, Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String add_info1 = "";
		String add_info2 = "";
		String add_info3 = "";

		String amount = amn.intValue() + "";
		if (mahasiswa != null) {
			add_info1 = mahasiswa.getNama() + "-" + mahasiswa.getNim();
			add_info2 = mahasiswa.getJurusan().getNama();
			add_info3 = mahasiswa.getJurusan().getFakultas().getNama();
		} else if (biodataCalonMahasiswa != null) {
			add_info1 = biodataCalonMahasiswa.getNama() + "-" + biodataCalonMahasiswa.getNoRegistrasi();
			add_info2 = biodataCalonMahasiswa.getNoUjian() == null ? "" : biodataCalonMahasiswa.getNoUjian();
			add_info3 = "";
		}
		String add_info4 = "";
		String add_info5 = "";

		String cust_email = "";
		String cust_id = "";
		String cust_msisdn = "";
		String cust_name = "";

		if (mahasiswa != null) {
			try {
				cust_email = mahasiswa.getEmail().trim().isEmpty() ? mahasiswa.getNim() + "@info.com"
						: mahasiswa.getEmail().split(",")[0];
			} catch (Exception e) {
				cust_email = mahasiswa.getEmail().trim().isEmpty() ? mahasiswa.getNim() + "@info.com"
						: mahasiswa.getEmail().trim();
			}
			cust_id = mahasiswa.getNim();
			cust_name = mahasiswa.getNama();
			cust_msisdn = mahasiswa.getTelp() == null || mahasiswa.getTelp().trim().isEmpty() ? "081300000"
					: mahasiswa.getTelp().trim();
		} else if (biodataCalonMahasiswa != null) {
			try {
				cust_email = biodataCalonMahasiswa.getEmail().trim().isEmpty()
						? biodataCalonMahasiswa.getNim() + "@info.com"
						: biodataCalonMahasiswa.getEmail().split(",")[0];
			} catch (Exception e) {
				cust_email = biodataCalonMahasiswa.getEmail().trim().isEmpty()
						? biodataCalonMahasiswa.getNim() + "@info.com"
						: biodataCalonMahasiswa.getEmail().trim();
			}
			cust_id = biodataCalonMahasiswa.getNoRegistrasi();
			cust_name = biodataCalonMahasiswa.getNama();
			cust_msisdn = biodataCalonMahasiswa.getHp() == null || biodataCalonMahasiswa.getHp().trim().isEmpty()
					? "081300000"
					: biodataCalonMahasiswa.getHp().trim();
		}

		String invoice = Common.getGeneratedBarCode();
		TreeMap<String, String> data = FinpayCommon.generateFinpayPostdata(invoice, amount, add_info1, add_info2,
				add_info3, add_info4, add_info5, cust_email, cust_id, cust_msisdn, cust_name, null);

		final FinpayRequest finpayRequest = FinpayCommon.sendRequest(data, mahasiswa, biodataCalonMahasiswa,
				jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
				nilaiBiayaHarusDiBayars, amn, finpayRequestDetails, finpayRequestDetailBiayas);
		if (finpayRequest != null && finpayRequest.getPaymentCode() != null
				&& !finpayRequest.getPaymentCode().trim().isEmpty()) {

			final String informasiPembayaran = Common
					.getKonfigurasi("finpay_payment_info", "http://portalfinpay.com/index.php/bank").getNilai();

			MyMessageboxConfig.show("Kode pembayaran Anda adalah " + finpayRequest.getPaymentCode()
					+ " dengan tagihan sebesar " + Common.numberFormat.get().format(amn)
					+ "\n\nAnda dapat membayar tagihan ini dengan memasukkan kode \"" + finpayRequest.getPaymentCode()
					+ "\" di semua channel Finpay.\nUntuk informasi lebih lanjut bisa dilihat di "
					+ informasiPembayaran, "Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "unchecked", "rawtypes" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									String info = "Kode Pembayaran\t\t: " + finpayRequest.getPaymentCode() + "\n";
									info += "Kode invoice\t\t\t: " + finpayRequest.getInvoice() + "\n";
									info += "Tagihan \t\t\t: " + Common.numberFormat.get().format(amn) + "\n";
									info += "Info Pembayaran \t: " + informasiPembayaran + "\n\n";
									if (finpayRequest.getMahasiswa() != null) {
										info += "NIM \t\t\t\t: " + finpayRequest.getMahasiswa().getNim() + "\n";
										info += "Nama \t\t\t\t: " + finpayRequest.getMahasiswa().getNama() + "\n";
									} else if (finpayRequest.getBiodataCalonMahasiswa() != null) {
										info += "No. Reg \t\t\t: "
												+ finpayRequest.getBiodataCalonMahasiswa().getNoRegistrasi() + "\n";
										if (finpayRequest.getBiodataCalonMahasiswa().getNoUjian() != null) {
											info += "No. Ujian \t\t\t: "
													+ finpayRequest.getBiodataCalonMahasiswa().getNoUjian() + "\n";
										}
										info += "Nama \t\t\t\t: " + finpayRequest.getBiodataCalonMahasiswa().getNama()
												+ "\n";
									}

									Map parameters = ais.common.HashMapGenerator.getRand();
									parameters.put("tanggal", finpayRequest.getTanggal_dirubah());
									parameters.put("finpayRequest", finpayRequest.getId());
									parameters.put("info", info);
									Report.generatePDFReport(Report.PDF, parameters, "Bukti_Finpay_Mahasiswa",
											ais.ui.util.WaktuUtil.getDate());

									try {
										File file = Report.generateFileReport(Report.PDF, parameters,
												"Bukti_Finpay_Mahasiswa", ais.ui.util.WaktuUtil.getDate(),
												Common.locale);
										CommonEmail.infoBayarViaFinpay(finpayRequest, file);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/FinpayCommon.java:374");

									}

								}
							}, "Menyiapkan pembayaran via finpay..");

						}
					});
		} else {
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	public static TreeMap<String, String> generateFinpayPostdata(String invoice, String amount, String add_info1,
			String add_info2, String add_info3, String add_info4, String add_info5, String cust_email, String cust_id,
			String cust_msisdn, String cust_name, String payment_code) {

		add_info1 = Common.maxPanjang(add_info1, 29);
		add_info2 = Common.maxPanjang(add_info2, 29);
		add_info3 = Common.maxPanjang(add_info3, 29);
		add_info4 = Common.maxPanjang(add_info4, 29);
		add_info5 = Common.maxPanjang(add_info5, 29);

		String merchant_id = Common.getKonfigurasi("finpay_merchant_id", "AK444").getNilai();
		String mer_password = Common.getKonfigurasi("finpay_password_merchant", "ak2016").getNilai();

		String timeout = Common.getKonfigurasi("finpay_timeout", "30").getNilai();

		String sof_id = Common.getKonfigurasi("finpay_sof_id", "finpay021").getNilai();
		// String sof_type = Common.getKonfigurasi("finpay_sof_type",
		// "pay").getNilai();

		String trans_date = Common.datetimeFormat1s.get().format(ais.ui.util.WaktuUtil.getDate());

		TreeMap<String, String> data = new TreeMap<String, String>();
		data.put("amount", amount);
		data.put("invoice", invoice);
		data.put("merchant_id", merchant_id);
		data.put("sof_id", sof_id);

		if (payment_code == null) {
			data.put("sof_type", "pay");
		} else {
			data.put("sof_type", "check");
			data.put("payment_code", payment_code);
		}
		data.put("timeout", timeout);
		data.put("trans_date", trans_date);

		data.put("add_info1", add_info1);
		data.put("add_info2", add_info2);
		data.put("add_info3", add_info3);
		data.put("add_info4", add_info4);
		data.put("add_info5", add_info5);

		cust_email = Common.maxPanjang(cust_email, 49);
		data.put("cust_email", cust_email);

		cust_id = Common.maxPanjang(cust_id, 49);
		data.put("cust_id", cust_id);

		cust_msisdn = Common.maxPanjang(cust_msisdn, 32);
		data.put("cust_msisdn", cust_msisdn);

		cust_name = Common.maxPanjang(cust_name, 32);
		data.put("cust_name", cust_name);

		String failed_url = Common.getRequestHostWithProtocol() + "/common/finpay/batal.zul";
		String success_url = Common.getRequestHostWithProtocol() + "/common/finpay/return.zul";
		String return_url = Common.getRequestHostWithProtocol()
				+ Common.getKonfigurasi("finpay_path_url_response", "/FinPayResponse").getNilai();

		data.put("failed_url", failed_url);
		data.put("success_url", success_url);
		data.put("return_url", return_url);

		String output = "";
		for (String s : data.values()) {
			if (!s.trim().isEmpty()) {
				output += output.trim().isEmpty() ? s : "%" + s;
			}
		}

		System.out.println("output = " + output.toUpperCase());

		output = output.toUpperCase() + "%" + mer_password;

		System.out.println("output+password = " + output.toUpperCase());

		String mer_signature = sha256InJava.getSHA256Hash(output);

		String sendData = "mer_signature=" + mer_signature;

		for (String s : data.keySet()) {
			sendData += ("&" + s + "=" + data.get(s));
		}

		System.out.println("sendData = " + sendData);
		data.put("sendData", sendData);
		return data;
	}

	public static FinpayRequest sendRequest(TreeMap<String, String> data, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, List<FinpayRequestDetail> finpayRequestDetails,
			List<FinpayRequestDetailBiaya> finpayRequestDetailBiayas) throws IOException, Exception {
		// Bersihkan detail kegagalan lama agar alert tidak menampilkan penyebab transaksi sebelumnya.
		InfoTeknisPembayaran.bersihkan();
		// curl_init and url
		String strURL = Common
				.getKonfigurasi("new_finpay_gateway_url", "https://sandbox.finpay.co.id/servicescode/api/apiFinpay.php")
				.getNilai();
		URL url = new URL(strURL);

		String res;
		try {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);

			String postData = data.get("sendData");

			con.setRequestProperty("Content-length", String.valueOf(postData.length()));

			con.setDoOutput(true);
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			output.writeBytes(postData);
			output.close();

			// "Post data send ... waiting for reply");
			int code = con.getResponseCode(); // 200 = HTTP_OK
			System.out.println("Response    (Code):" + code);
			System.out.println("Response (Message):" + con.getResponseMessage());

			// read the response
			DataInputStream input = new DataInputStream(con.getInputStream());
			int c;
			StringBuilder resultBuf = new StringBuilder();
			while ((c = input.read()) != -1) {
				resultBuf.append((char) c);
			}
			input.close();

			res = resultBuf.toString();
		} catch (java.net.ConnectException ce) {
			// Gateway Finpay tak bisa dihubungi (unreachable/refused). Alur tetap seperti semula:
			// exception dilempar ulang; catat penyebabnya untuk alert pemanggil.
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway Finpay (" + strURL + "): "
					+ InfoTeknisPembayaran.potong(ce.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
			throw ce;
		} catch (java.net.SocketTimeoutException te) {
			// Gateway Finpay tidak merespons dalam batas waktu baca (timeout).
			InfoTeknisPembayaran.catat("Gateway Finpay (" + strURL + ") tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(te.getMessage(), 200) + ". Coba beberapa saat lagi.");
			throw te;
		} catch (Exception e) {
			// Kegagalan request/baca respons lain — catat lalu lempar ulang (alur tetap sama).
			InfoTeknisPembayaran.catat("Gagal memproses request/respons Finpay (" + strURL + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			throw e;
		}

		System.out.println("==> res param => " + res);

		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(res);
		} catch (Exception e) {
			// Respons Finpay tidak bisa diparse sebagai JSON — catat potongan respons mentahnya.
			InfoTeknisPembayaran.catat("Gagal memproses request/respons Finpay (" + strURL + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200)
					+ ". Respons server: " + InfoTeknisPembayaran.potong(res, 300));
			throw e;
		}

		System.out.println("==> response jsonObject => " + jsonObject);

		String payment_code_respons = jsonObject.optString("payment_code", "");
		if (payment_code_respons == null || payment_code_respons.trim().isEmpty()) {
			// Finpay menolak/menggagalkan permintaan (payment_code kosong) — catat kode + pesan
			// server apa adanya agar alert pemanggil tidak generik. Alur tetap seperti semula.
			String status_code = jsonObject.optString("status_code", "");
			String status_desc = jsonObject.optString("status_desc", "");
			InfoTeknisPembayaran.catat("Server Finpay menolak permintaan, kode status=" + status_code
					+ (status_desc.trim().isEmpty() ? "" : ", pesan=" + status_desc.trim())
					+ ". Respons server: " + InfoTeknisPembayaran.potong(res, 300) + " (URL: " + strURL + ")");
		}

		FinpayRequest finpayRequest = new FinpayRequest();
		finpayRequest.setNama(data.get("mer_signature"));
		finpayRequest.setTipe(data.get("sof_id"));
		finpayRequest.setMerchant(data.get("merchant_id"));
		finpayRequest.setInvoice(data.get("invoice"));
		finpayRequest.setPaymentCode(jsonObject.getString("payment_code"));
		finpayRequest.setResultCode(jsonObject.getString("status_code"));
		finpayRequest.setStatus(jsonObject.getString("status_desc"));
		finpayRequest.setMahasiswa(mahasiswa);
		finpayRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
		finpayRequest.setJenisKegiatan(jenisKegiatan);
		finpayRequest.setJadwalPembayaran(jadwalPembayaran);
		finpayRequest.setSemester(semester);
		finpayRequest.setTahunAkademik(tahunAkademik);
		finpayRequest.setKeterangan(keterangan);
		finpayRequest.setPengurangan(pengurangan);
		finpayRequest.setAmount(amount);
		finpayRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);

		try {
			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(finpayRequest);
			session.getTransaction().commit();

			for (FinpayRequestDetail finpayRequestDetail : finpayRequestDetails) {
				finpayRequestDetail.setFinpayRequest(finpayRequest);
				session.getTransaction().begin();
				session.save(finpayRequestDetail);
				session.getTransaction().commit();
			}

			for (FinpayRequestDetailBiaya finpayRequestDetailBiaya : finpayRequestDetailBiayas) {
				finpayRequestDetailBiaya.setFinpayRequest(finpayRequest);
				session.getTransaction().begin();
				session.save(finpayRequestDetailBiaya);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			// Finpay sudah menerima transaksi namun penyimpanan lokal gagal — beri tahu
			// penyebabnya supaya admin memeriksa Error Log/DB, bukan menyalahkan gateway.
			InfoTeknisPembayaran.catat("Transaksi diterima gateway namun GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			throw e;
		}

		HibernateUtil.closeSession();

		return finpayRequest;
	}

}
