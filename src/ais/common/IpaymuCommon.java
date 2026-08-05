package ais.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;
import ais.database.model.ipaymu.IpaymuRequestDetailBiaya;
import ais.database.model.ipaymu.IpaymuResponse;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class IpaymuCommon {

	@SuppressWarnings("unchecked")
	public static List<IpaymuRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas = new ArrayList<IpaymuRequestDetailBiaya>();
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
					if (component instanceof Doublebox
							&& detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
						Doublebox jumlah = (Doublebox) component;
						biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
					} else if (component instanceof Label) {
						Label myLabel = (Label) component;
						// System.out.println("myLabel = " +
						// myLabel.getValue());
						biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/IpaymuCommon.java:78");
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

				IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya = new IpaymuRequestDetailBiaya();
				ipaymuRequestDetailBiaya.setDetailBiaya(detailBiaya);
				ipaymuRequestDetailBiaya.setNilai(biaya);
				ipaymuRequestDetailBiayas.add(ipaymuRequestDetailBiaya);
			}
		}
		return ipaymuRequestDetailBiayas;
	}

	public static List<IpaymuRequestDetail> populateIpaymuRequestDetailDariDetailBiaya(
			List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas) {
		List<IpaymuRequestDetail> ipaymuRequestDetails = new ArrayList<IpaymuRequestDetail>();

		int i = 1;
		for (IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya : ipaymuRequestDetailBiayas) {
			IpaymuRequestDetail ipaymuRequestDetail = new IpaymuRequestDetail();
			ipaymuRequestDetail.setPengaturanPembayaranBulanan(null);
			ipaymuRequestDetail.setItemBiaya(ipaymuRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			ipaymuRequestDetail.setKeterangan(ipaymuRequestDetailBiaya.getKeterangan());
			ipaymuRequestDetail.setNilai(ipaymuRequestDetailBiaya.getNilai());
			ipaymuRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			ipaymuRequestDetail.setKe(i);
			ipaymuRequestDetails.add(ipaymuRequestDetail);
			i++;
		}

		return ipaymuRequestDetails;
	}

	public static List<IpaymuRequestDetail> populateIpaymuRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<IpaymuRequestDetail> ipaymuRequestDetails = new ArrayList<IpaymuRequestDetail>();

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

				IpaymuRequestDetail ipaymuRequestDetail = new IpaymuRequestDetail();

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

				ipaymuRequestDetail.setDetailBiaya(detailBiaya); 
				ipaymuRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				ipaymuRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				ipaymuRequestDetail.setItemBiaya(itemBiaya);
				ipaymuRequestDetail.setKeterangan(keterangan.getValue());
				ipaymuRequestDetail.setNilai(jumlahCicilan.getValue());
				ipaymuRequestDetail.setTanggal(tanggal.getValue());
				ipaymuRequestDetail.setKe(i);

				ipaymuRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				ipaymuRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, ipaymuRequestDetail.getTanggal(),
								jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						ipaymuRequestDetail.setDenda(denda);
						ipaymuRequestDetail.setNilaiAsli(nom);
					}
				}

				ipaymuRequestDetails.add(ipaymuRequestDetail);
				i++;
			}
		}

		return ipaymuRequestDetails;
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

				List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas = new ArrayList<IpaymuRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya = new IpaymuRequestDetailBiaya();
					ipaymuRequestDetailBiaya.setDetailBiaya(detailBiaya);
					ipaymuRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					ipaymuRequestDetailBiayas.add(ipaymuRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += ipaymuRequestDetailBiaya.getNilai();
				}

				IpaymuCommon.onSaveIpaymu(nilaiBiayaHarusDiBayars, null, calonMahasiswa, jenisKegiatan,
						jadwalPembayaran, 1, calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru",
						0.0, nilaiBiayaHarusDiBayars,
						IpaymuCommon.populateIpaymuRequestDetailDariDetailBiaya(ipaymuRequestDetailBiayas),
						ipaymuRequestDetailBiayas, null);

			}
		}

	}

	@SuppressWarnings({})
	public static boolean onSaveIpaymu(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<IpaymuRequestDetail> ipaymuRequestDetails,
			List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas, Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String add_info1 = "";
		String add_info2 = "";

		for (IpaymuRequestDetail detail : ipaymuRequestDetails) {
			if (detail.getIdCicilan() == null) {
				add_info2 += add_info2.isEmpty() ? detail.getKeterangan() : "; " + detail.getKeterangan();
			}
		}

		if (mahasiswa != null) {
			add_info2 = mahasiswa.getNama() + "-" + mahasiswa.getNim() + ", " + add_info2;
		} else if (biodataCalonMahasiswa != null) {
			add_info2 = biodataCalonMahasiswa.getNama()
					+ (biodataCalonMahasiswa.getNoRegistrasi() == null
							|| biodataCalonMahasiswa.getNoRegistrasi().trim().isEmpty() ? ""
									: "-" + biodataCalonMahasiswa.getNoRegistrasi())
					+ (biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().isEmpty()
							? ""
							: "-" + biodataCalonMahasiswa.getNoUjian())
					+ ", " + add_info2;
		}

		String amount = amn.intValue() + "";
		if (mahasiswa != null) {
			add_info1 = mahasiswa.getNama() + "-" + mahasiswa.getNim();
		} else if (biodataCalonMahasiswa != null) {
			add_info1 = biodataCalonMahasiswa.getNama() + "-" + biodataCalonMahasiswa.getNoRegistrasi();
		}

		String key = Common.getKonfigurasi("ipaymu_key", "HZ2j4j8y112OHd2UVWH60QXfT04Pf1").getNilai();
		Boolean langsung = Common.bolehKonfigurasi("ipaymu_langsung_menggunakan_virtual_account");

		if (langsung) {
			String urlStr = Common.getKonfigurasi("ipaymu_gateway_url_va", "https://my.ipaymu.com/api/GetVa.php")
					.getNilai();

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("key", key);
			params.put("amount", amount);
			params.put("notes", add_info2);

			String postData = URLBuilder.httpBuildQuery(params, "UTF-8");
			final IpaymuRequest ipaymuRequest = IpaymuCommon.sendRequest(urlStr, postData, mahasiswa,
					biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan,
					pengurangan, nilaiBiayaHarusDiBayars, amn, add_info2, ipaymuRequestDetails,
					ipaymuRequestDetailBiayas);
			if (ipaymuRequest != null && ipaymuRequest.getTrxId() != null
					&& !ipaymuRequest.getTrxId().trim().isEmpty()) {

				Common.displayWindow(ipaymuRequest.getUrl().trim(), true, "95%");

			} else {
				// Tampilkan alert kegagalan BESERTA "Informasi Teknis" yang dicatat sendRequest
				// (pola bersama seluruh payment gateway, lihat InfoTeknisPembayaran).
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}

		} else {
			String urlStr = Common.getKonfigurasi("ipaymu_gateway_url", "https://my.ipaymu.com/payment.htm").getNilai();
			String return_url = Common.getRequestHostWithProtocol()
					+ Common.getKonfigurasi("ipaymu_path_url_response", "/FinPayResponse").getNilai();

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("key", key);
			params.put("action", "payment");
			params.put("product", add_info1);
			params.put("price", amount);
			params.put("quantity", "1");
			params.put("comments", add_info2);
			params.put("ureturn", Common.getRequestHostWithProtocol() + "/common/ipaymu/return.zul?kembalian=1");
			params.put("unotify", return_url);
			params.put("ucancel", Common.getRequestHostWithProtocol() + "/common/ipaymu/batal.zul");
			params.put("format", "json");

			String postData = URLBuilder.httpBuildQuery(params, "UTF-8");
			final IpaymuRequest ipaymuRequest = IpaymuCommon.sendRequest(urlStr, postData, mahasiswa,
					biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan,
					pengurangan, nilaiBiayaHarusDiBayars, amn, add_info2, ipaymuRequestDetails,
					ipaymuRequestDetailBiayas);
			if (ipaymuRequest != null && ipaymuRequest.getUrl() != null && !ipaymuRequest.getUrl().trim().isEmpty()) {

				Common.displayWindow(ipaymuRequest.getUrl().trim(), true, "95%");

			} else {
				// Alert kegagalan + "Informasi Teknis" dari sendRequest (lihat InfoTeknisPembayaran).
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}

		return true;
	}

	public static IpaymuRequest sendRequest(String urlStr, String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, String comments,
			List<IpaymuRequestDetail> ipaymuRequestDetails, List<IpaymuRequestDetailBiaya> ipaymuRequestDetailBiayas)
			throws Exception {
		// Pola "Informasi Teknis" kegagalan payment gateway (lihat InfoTeknisPembayaran):
		// bersihkan detail lama agar kegagalan transaksi sebelumnya tidak bocor ke alert ini.
		InfoTeknisPembayaran.bersihkan();

		String hasil = "";
		JSONObject param;
		String va;
		String trxId;
		try {
			// curl_init and url
			URL url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// CURLOPT_POST
			con.setRequestMethod("POST");

			// CURLOPT_FOLLOWLOCATION
			con.setInstanceFollowRedirects(true);

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

			if (code < 200 || code >= 300) {
				// Server iPaymu MENOLAK permintaan (kode status bukan sukses) → catat detailnya
				// supaya pengguna/admin tahu penyebab pastinya (bukan sekadar "Transaksi Gagal").
				String badan = "";
				try {
					java.io.InputStream err = con.getErrorStream();
					if (err != null) {
						StringBuilder errBuf = new StringBuilder();
						int ce;
						while ((ce = err.read()) != -1) {
							errBuf.append((char) ce);
						}
						err.close();
						badan = errBuf.toString();
					}
				} catch (Exception ignore) {
					ais.common.ErrorAuditUtil.record(ignore, "iPaymu sendRequest gagal baca error stream; url=" + urlStr);
				}
				String info = "Server iPaymu menolak permintaan, kode status=" + code
						+ (badan.trim().isEmpty() ? "" : ", respons server: " + InfoTeknisPembayaran.potong(badan, 300))
						+ ". URL gateway: " + urlStr;
				InfoTeknisPembayaran.catat(info);
				ais.common.ErrorAuditUtil.record(new IllegalStateException(info), "iPaymu sendRequest ditolak; url=" + urlStr);
				return null;
			}

			// read the response
			DataInputStream input = new DataInputStream(con.getInputStream());
			int c;
			StringBuilder resultBuf = new StringBuilder();
			while ((c = input.read()) != -1) {
				resultBuf.append((char) c);
			}
			input.close();
			hasil = resultBuf.toString();

			param = new JSONObject(hasil);

			va = param.isNull("va") ? "" : param.getString("va");
			trxId = param.isNull("id") ? "" : param.getString("id");
			System.out.println("==> response param => " + param);
			System.out.println("==> response va => " + va);
			System.out.println("==> response trxId => " + trxId);

			// Respons HTTP sukses namun TANPA tanda transaksi (id/va/url kosong semua) →
			// gateway menolak secara logis (mis. key salah). Catat agar alert pemanggil
			// (yang mengecek trxId/url kosong) menampilkan penyebabnya.
			if (trxId.trim().isEmpty() && va.trim().isEmpty() && param.isNull("url")) {
				InfoTeknisPembayaran.catat("Gateway iPaymu merespons kode status=" + code
						+ " namun tanpa id/va/url transaksi. Respons server: " + InfoTeknisPembayaran.potong(hasil, 300)
						+ ". URL gateway: " + urlStr);
			}
		} catch (java.net.ConnectException ce) {
			// Gateway iPaymu tak bisa dihubungi (unreachable/refused) → kembalikan null agar
			// pemanggil menampilkan alert kegagalan beserta informasi teknisnya, bukan crash.
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway iPaymu (" + urlStr + "): "
					+ InfoTeknisPembayaran.potong(ce.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
			ais.common.ErrorAuditUtil.record(ce, "iPaymu sendRequest ConnectException; url=" + urlStr);
			return null;
		} catch (java.net.SocketTimeoutException te) {
			// Gateway iPaymu tidak merespons dalam batas waktu → sama: null, jangan crash halaman.
			InfoTeknisPembayaran.catat("Gateway iPaymu (" + urlStr + ") tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(te.getMessage(), 200) + ". Coba beberapa saat lagi.");
			ais.common.ErrorAuditUtil.record(te, "iPaymu sendRequest timeout; url=" + urlStr);
			return null;
		} catch (Exception e) {
			// Kegagalan request/parse lain (respons bukan JSON, koneksi putus, dsb) → null.
			InfoTeknisPembayaran.catat("Gagal memproses request/respons iPaymu (" + urlStr + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			ais.common.ErrorAuditUtil.record(e, "iPaymu sendRequest gagal; url=" + urlStr);
			return null;
		}

		IpaymuRequest ipaymuRequest = new IpaymuRequest();
		ipaymuRequest.setNama(param.isNull("sessionID") ? va : param.getString("sessionID"));
		ipaymuRequest.setUrl(param.isNull("url")
				? "/common/ipaymu/no_va.zul?va=" + URLEncoder.encode(va, "UTF-8") + "&nominal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amount), "UTF-8")
				: param.getString("url"));
		ipaymuRequest.setMahasiswa(mahasiswa);
		ipaymuRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
		ipaymuRequest.setJenisKegiatan(jenisKegiatan);
		ipaymuRequest.setJadwalPembayaran(jadwalPembayaran);
		ipaymuRequest.setSemester(semester);
		ipaymuRequest.setTahunAkademik(tahunAkademik);
		ipaymuRequest.setKeterangan(keterangan);
		ipaymuRequest.setPengurangan(pengurangan);
		ipaymuRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
		ipaymuRequest.setAmount(amount);
		ipaymuRequest.setComments(comments);
		ipaymuRequest.setTrxId(trxId);

		Session session = HibernateUtil.currentNativeSession();
		try {
			session.getTransaction().begin();
			session.save(ipaymuRequest);
			session.getTransaction().commit();

			for (IpaymuRequestDetail ipaymuRequestDetail : ipaymuRequestDetails) {
				ipaymuRequestDetail.setIpaymuRequest(ipaymuRequest);
				session.getTransaction().begin();
				session.save(ipaymuRequestDetail);
				session.getTransaction().commit();
			}

			for (IpaymuRequestDetailBiaya ipaymuRequestDetailBiaya : ipaymuRequestDetailBiayas) {
				ipaymuRequestDetailBiaya.setIpaymuRequest(ipaymuRequest);
				session.getTransaction().begin();
				session.save(ipaymuRequestDetailBiaya);
				session.getTransaction().commit();
			}

			if (va != null && !va.trim().isEmpty()) {
				IpaymuResponse ipaymuResponse = new IpaymuResponse();
				ipaymuResponse.setNama(va);
				ipaymuResponse.setStatus(IpaymuResponse.PENDING);
				ipaymuResponse.setMerchant("e-Campus");
				ipaymuResponse.setTrxId(trxId);
				ipaymuResponse.setProduct(comments);
				ipaymuResponse.setBuyer("Mahasiswa");
				ipaymuResponse.setNoRekeningDeposit(va);
				ipaymuResponse.setComments(comments);
				session.getTransaction().begin();
				session.save(ipaymuResponse);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			// Transaksi sudah diterima gateway iPaymu namun GAGAL disimpan di aplikasi →
			// catat detailnya lalu kembalikan null agar alert pemanggil informatif.
			InfoTeknisPembayaran.catat("Transaksi diterima gateway iPaymu namun GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200)
					+ (trxId == null || trxId.trim().isEmpty() ? "" : ". trxId=" + trxId));
			ais.common.ErrorAuditUtil.record(e, "iPaymu sendRequest gagal simpan; trxId=" + trxId + ", url=" + urlStr);
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) {
				ais.common.ErrorAuditUtil.record(ignore, "iPaymu sendRequest rollback gagal; url=" + urlStr);
			}
			return null;
		}

		return ipaymuRequest;
	}

}
