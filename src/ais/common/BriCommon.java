package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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

import ais.action.master.helper.PembayaranUtilHelper;
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
import ais.database.model.bri.BriRequest;
import ais.database.model.bri.BriRequestDetail;
import ais.database.model.bri.BriRequestDetailBiaya;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class BriCommon {

	public static MyButtonConfig createButton() {
		File fileViaBri = new File(Common.REAL_PATH + "/img/bri-logo.png");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BRI,
					LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BRI_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileViaBri = lainMahasiswa.ambilFile();
				File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaBri.getName());
				boolean ada = fileDiImg.exists();
				System.out.println("fileViaBri = " + fileViaBri + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
				if (!ada) {
					FileInputStream fileInputStream = new FileInputStream(fileViaBri);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		MyButtonConfig bayarViaBri = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_bri", "Bayar via Bri").getNilai(),
				"/img/" + fileViaBri.getName());
		return bayarViaBri;
	}

	@SuppressWarnings("unchecked")
	public static List<BriRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<BriRequestDetailBiaya> briRequestDetailBiayas = new ArrayList<BriRequestDetailBiaya>();
		Rows rows = (Rows) gridss.getRows();
		if (rows != null && rows.getChildren() != null) {
			List<Row> myRows = rows.getChildren();
			System.out.println("myRows -> " + myRows.size());
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BriCommon.java:128");
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

				BriRequestDetailBiaya briRequestDetailBiaya = new BriRequestDetailBiaya();
				briRequestDetailBiaya.setDetailBiaya(detailBiaya);
				briRequestDetailBiaya.setNilai(biaya);
				briRequestDetailBiayas.add(briRequestDetailBiaya);
			}
		}
		return briRequestDetailBiayas;
	}

	public static List<BriRequestDetail> populateBriRequestDetailDariDetailBiaya(
			List<BriRequestDetailBiaya> briRequestDetailBiayas) {
		List<BriRequestDetail> briRequestDetails = new ArrayList<BriRequestDetail>();

		int i = 1;
		for (BriRequestDetailBiaya briRequestDetailBiaya : briRequestDetailBiayas) {
			BriRequestDetail briRequestDetail = new BriRequestDetail();
			briRequestDetail.setPengaturanPembayaranBulanan(null);
			briRequestDetail.setItemBiaya(briRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			briRequestDetail.setKeterangan(briRequestDetailBiaya.getKeterangan());
			briRequestDetail.setNilai(briRequestDetailBiaya.getNilai());
			briRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			briRequestDetail.setKe(i);
			briRequestDetails.add(briRequestDetail);
			i++;
		}

		return briRequestDetails;
	}

	public static List<BriRequestDetail> populateBriRequestDetail(HttpServletRequest request, Mahasiswa mahasiswa,
			String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
		System.out.println("jenis => " + jenis + ", data => " + data);
		List<BriRequestDetail> briRequestDetails = new ArrayList<BriRequestDetail>();

		Session session = HibernateUtil.currentNativeSession();
		int i = 1;
		for (String d : data.split(",")) {

			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
			ItemBiaya itemBiaya = null;
			DetailBiaya detailBiaya = null;
			Double nilai = 0.0;

			if (jenis.equalsIgnoreCase(jenis)) {
				pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
						.createCriteria(PengaturanPembayaranBulanan.class)
						.add(Restrictions.idEq(Long.parseLong(d.trim()))).uniqueResult();
				detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
				itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				nilai = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
			} else {
				detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
						.add(Restrictions.idEq(Long.parseLong(d.trim()))).uniqueResult();
				itemBiaya = detailBiaya.getItemBiaya();
				nilai = (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru());
			}

			String keterangan = "";
			if ((keterangan == null || keterangan.trim().isEmpty()) && pengaturanPembayaranBulanan != null) {
				keterangan = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "-"
						+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + ", bulan "
						+ pengaturanPembayaranBulanan.getNamaBulan() + " " + ", nominal Rp. "
						+ Common.numberFormat.get().format(nilai)
						+ (validator.trim().isEmpty() ? "" : ", validator : " + validator);
			} else if ((keterangan == null || keterangan.trim().isEmpty()) && itemBiaya != null && nilai != null) {
				keterangan = itemBiaya.getKode() + "-" + itemBiaya.getNama() + ", nominal Rp. "
						+ Common.numberFormat.get().format(nilai)
						+ (validator.trim().isEmpty() ? "" : ", validator : " + validator);

			}

			BriRequestDetail briRequestDetail = new BriRequestDetail();
			briRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			briRequestDetail.setDetailBiaya(detailBiaya);
			briRequestDetail.setItemBiaya(itemBiaya);
			briRequestDetail.setKeterangan(keterangan);
			briRequestDetail.setNilai(nilai);
			briRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			briRequestDetail.setKe(i);
			briRequestDetail.setDenda(0.0);
			briRequestDetail.setNilaiAsli(nilai);
			briRequestDetails.add(briRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return briRequestDetails;
	}

	public static List<BriRequestDetail> populateBriRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<BriRequestDetail> briRequestDetails = new ArrayList<BriRequestDetail>();

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

				BriRequestDetail briRequestDetail = new BriRequestDetail();

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

				briRequestDetail.setDetailBiaya(detailBiaya); 
				briRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				briRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				briRequestDetail.setItemBiaya(itemBiaya);
				briRequestDetail.setKeterangan(keterangan.getValue());
				briRequestDetail.setNilai(jumlahCicilan.getValue());
				briRequestDetail.setTanggal(tanggal.getValue());
				briRequestDetail.setKe(i);

				briRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				briRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, briRequestDetail.getTanggal(), jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan())
								- nom;
						briRequestDetail.setDenda(denda);
						briRequestDetail.setNilaiAsli(nom);
					}
				}

				briRequestDetails.add(briRequestDetail);
				i++;
			}
		}

		return briRequestDetails;
	}

	public static BriRequest bayarCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa, JenisKegiatan jenisKegiatan,
			boolean tampilInfo) throws Exception {
		Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		if (prodiLulus == null || prodiLulus.getId() == null) {
			Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
					: calonMahasiswa.getProdi1();
			PembayaranUtil.getInstance();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, myjurusan1, false);
			detailBiayas.addAll(detailBiayas1);
		} else {
			PembayaranUtil.getInstance();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, prodiLulus, false);
			detailBiayas.addAll(detailBiayas1);
		}

//		System.out.println("detailBiayas => " + detailBiayas.size());

		if (!detailBiayas.isEmpty()) {

			Serializable[] serializables = PembayaranUtil.getInstance()
					.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(calonMahasiswa.getTanggalDaftar(),
							jenisKegiatan, calonMahasiswa.getJenjang(), calonMahasiswa.getTahunAkademik(),
							calonMahasiswa.getGelombangPendaftaran().getJenisSemester().equalsIgnoreCase(
									Perkuliahan.GANJIL),
							calonMahasiswa.getJenisSeleksi(), calonMahasiswa.getProgram(),
							calonMahasiswa.getNoRegistrasi(), calonMahasiswa.getGelombangPendaftaran());
			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
			System.out.println("jadwalPembayaran => " + jadwalPembayaran);
			if (jadwalPembayaran != null) {
				Double nilaiBiayaHarusDiBayars = 0.0;

				List<BriRequestDetailBiaya> briRequestDetailBiayas = new ArrayList<BriRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					BriRequestDetailBiaya briRequestDetailBiaya = new BriRequestDetailBiaya();
					briRequestDetailBiaya.setDetailBiaya(detailBiaya);
					briRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					briRequestDetailBiayas.add(briRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += briRequestDetailBiaya.getNilai();
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				return BriCommon.onSaveBri(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						BriCommon.populateBriRequestDetailDariDetailBiaya(briRequestDetailBiayas),
						briRequestDetailBiayas, tampilInfo, null);

			}

		}
		return null;
	}

	public static BriRequest onPilihBri(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<BriRequestDetail> briRequestDetails,
			List<BriRequestDetailBiaya> briRequestDetailBiayas, boolean tampilInfo, Event event) throws Exception {

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 2);

		String bill_no = Common.getGeneratedBarCode();

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran_bri = Common.getKonfigurasi("tanggal_terakhir_pembayaran_bri", "")
				.getNilai();
		if (tanggal_terakhir_pembayaran_bri != null && !tanggal_terakhir_pembayaran_bri.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran_bri);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BriCommon.java:391");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		String virtual_account = Common.getGeneratedAngkaDigit(10);

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bri_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BriCommon.java:404");

		}

		String merchant_id = Common.getKonfigurasi("bri_institution_code", "J104408").getNilai();
		String brivaNo = Common.getKonfigurasi("bri_briva_no", "77777").getNilai();

		String nama = mahasiswa != null ? mahasiswa.getNama()
				: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("institutionCode", merchant_id);
		jsonObject.put("brivaNo", brivaNo);
		jsonObject.put("custCode", virtual_account);
		jsonObject.put("nama", nama);
		jsonObject.put("amount", amn);
		jsonObject.put("keterangan", "");
		jsonObject.put("expiredDate", datetime_expired);

		String data = jsonObject.toString();

		final BriRequest briRequest = BriCommon.sendRequest(mahasiswa, biodataCalonMahasiswa, jenisKegiatan,
				jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, amn,
				merchant_id, data, bill_no, virtual_account, brivaNo, briRequestDetails, briRequestDetailBiayas, true);
		if (tampilInfo) {
			if (briRequest != null && briRequest.getVa() != null && !briRequest.getVa().trim().isEmpty()) {

				String code = briRequest.getVa();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
						+ briRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String myUrl = "/common/bri/no_va.zul?va=" + URLEncoder.encode(briRequest.getVa(), "UTF-8")
						+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8")
						+ "&kadalurasa="
						+ URLEncoder.encode(Common.dateFormat.get().format(briRequest.getBill_expired()), "UTF-8") + "&nama="
						+ URLEncoder.encode(nama, "UTF-8") + "&biayaTotal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn + biayaAdministrasi), "UTF-8")
						+ "&qr="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(),
								"UTF-8")
						+ "&terbilang="
						+ URLEncoder.encode(IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)), "UTF-8")
						+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

				Common.displayWindow(myUrl, true, "65%");

			} else {
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}

		return briRequest;
	}

	@SuppressWarnings({})
	public static BriRequest onSaveBri(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final JadwalPembayaran jadwalPembayaran, final Integer semester, final String tahunAkademik,
			final String keterangan, final Double pengurangan, final Double nilaiBiayaHarusDiBayars,
			final List<BriRequestDetail> briRequestDetails, final List<BriRequestDetailBiaya> briRequestDetailBiayas,
			final boolean tampilInfo, final Event event) throws Exception {

		if (amn < 0.01) {
			return null;
		}

		return onPilihBri(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester,
				tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, briRequestDetails,
				briRequestDetailBiayas, tampilInfo, event);

	}

	public static String requestToken() throws Exception {
		String strURL = (Common.getKonfigurasi("bri_gateway_url_token", "https://developer.bri.co.id/v1/api/token")
				.getNilai());
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {

			HttpPost httpPost = new HttpPost(strURL);

			String postData = "{\"grant_type\": \"authorization_code\",  \"client_id\": \""
					+ Common.getKonfigurasi("bri_merchant_id", "6b0c1a35a4c308fc523f8f484246c0fbafda").getNilai()
					+ "\",  \"client_secret\": \""
					+ Common.getKonfigurasi("bri_password", "90eefc01993f81ea87a64e0816776f0429e9").getNilai()
					+ "\",  \"code\": \""
					+ Common.getKonfigurasi("bri_auth_code", "8ab63febc16c5845f9ac1ee75a58d70bfcb99a83").getNilai()
					+ "\"}";

			StringEntity entity = new StringEntity(postData);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			httpPost.setHeader("X-BRI-KEY",
					Common.getKonfigurasi("bri_api_key", "b6642aad94d9861f21671cfcccfa672fc880a89d").getNilai());

			CloseableHttpResponse response = httpclient.execute(httpPost);

			int status = response.getStatusLine().getStatusCode();
			System.out.println("Response status code: " + status);
			String hasil = EntityUtils.toString(response.getEntity());

			JSONObject responseData = new JSONObject(hasil);
			JSONObject data = responseData.getJSONObject("data");
			String access_token = data.getString("access_token");
			System.out.println("access_token => " + access_token);

			Konfigurasi konfigurasi = Common.getKonfigurasi("bri_auth_code_barier", access_token);
			konfigurasi.setNilai(access_token);
			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.update(konfigurasi);
			session.getTransaction().commit();

			HibernateUtil.closeSession();
			MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

			return hasil;
		} finally {
			httpclient.close();
		}
	}

	public static String get(String strURL) throws Exception {

		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {

			HttpGet httpGet = new HttpGet(strURL);

			String barier = Common.getKonfigurasi("bri_auth_code_barier", "43b0fa6ba16c6dcfd37130014e4ddce337b7b178")
					.getNilai();

			httpGet.setHeader("Accept", "application/json");
			httpGet.setHeader("Content-type", "application/json");
			httpGet.setHeader("X-BRI-KEY",
					Common.getKonfigurasi("bri_api_key", "b6642aad94d9861f21671cfcccfa672fc880a89d").getNilai());
			httpGet.setHeader("Authorization", "Bearer " + barier);

			CloseableHttpResponse response = httpclient.execute(httpGet);

			int status = response.getStatusLine().getStatusCode();
			// System.out.println("Response status code: " + status + ", strURL
			// => " + strURL);
			if (status == 401) {
				httpclient.close();
				requestToken();
				return BriCommon.get(strURL);
			}
			String hasil = EntityUtils.toString(response.getEntity());
			return hasil;
		} finally {
			httpclient.close();
		}
	}

	/**
	 * True bila exception (di sepanjang rantai penyebab) merupakan gangguan JARINGAN/DNS ke API BRI
	 * (mis. host developer.bri.co.id tak dapat diresolusi, koneksi ditolak, atau timeout). Ini kendala
	 * INFRASTRUKTUR, bukan bug aplikasi -> pemanggil tidak boleh menganggapnya error aplikasi (KE-4).
	 */
	public static boolean isGangguanJaringan(Throwable e) {
		Throwable c = e;
		while (c != null) {
			if (c instanceof java.net.UnknownHostException
					|| c instanceof java.net.ConnectException
					|| c instanceof java.net.SocketTimeoutException
					|| c instanceof java.net.NoRouteToHostException
					|| c instanceof java.net.PortUnreachableException
					|| c instanceof javax.net.ssl.SSLException) {
				return true;
			}
			String m = c.getMessage();
			if (m != null) {
				String mm = m.toLowerCase();
				if (mm.indexOf("name or service not known") >= 0
						|| mm.indexOf("temporary failure in name resolution") >= 0
						|| mm.indexOf("unknownhost") >= 0
						|| mm.indexOf("connection refused") >= 0
						|| mm.indexOf("connect timed out") >= 0
						|| mm.indexOf("read timed out") >= 0
						|| mm.indexOf("no route to host") >= 0
						|| mm.indexOf("network is unreachable") >= 0) {
					return true;
				}
			}
			c = c.getCause();
		}
		return false;
	}

	public static String post(String postData, String strURL) throws Exception {

		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {

			HttpPost httpPost = new HttpPost(strURL);

			String barier = Common.getKonfigurasi("bri_auth_code_barier", "43b0fa6ba16c6dcfd37130014e4ddce337b7b178")
					.getNilai();

			StringEntity entity = new StringEntity(postData);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			httpPost.setHeader("X-BRI-KEY",
					Common.getKonfigurasi("bri_api_key", "b6642aad94d9861f21671cfcccfa672fc880a89d").getNilai());
			httpPost.setHeader("Authorization", "Bearer " + barier);

			CloseableHttpResponse response = httpclient.execute(httpPost);

			int status = response.getStatusLine().getStatusCode();
			System.out.println("Response status code: " + status);
			if (status == 401) {
				httpclient.close();
				requestToken();
				return BriCommon.post(postData, strURL);
			}
			String hasil = EntityUtils.toString(response.getEntity());
			return hasil;
		} finally {
			httpclient.close();
		}
	}

	/**
	 * Catat "Informasi Teknis" kegagalan request BRIVA sesuai jenis exception — kanal
	 * bersama {@link InfoTeknisPembayaran} agar alert "Transaksi Gagal Dilakukan"
	 * menjelaskan penyebabnya. TIDAK mengubah alur: pemanggil tetap menangani
	 * exception seperti semula, method ini hanya mencatat.
	 */
	private static void catatKegagalanBri(Exception e, String strURL, String hasil, boolean simpanKeDb) {
		String url = strURL == null ? "" : " (" + strURL + ")";
		if (e instanceof java.net.ConnectException) {
			// Gateway BRI tak bisa dihubungi (unreachable/koneksi ditolak).
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway BRI" + url + ": "
					+ InfoTeknisPembayaran.potong(e.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
		} else if (e instanceof java.net.SocketTimeoutException) {
			// Gateway BRI tidak merespons dalam batas waktu baca.
			InfoTeknisPembayaran.catat("Gateway BRI" + url + " tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(e.getMessage(), 200) + ". Coba beberapa saat lagi.");
		} else if (e instanceof org.apache.http.conn.ConnectTimeoutException) {
			// Awas: subclass InterruptedIOException, BUKAN ConnectException.
			InfoTeknisPembayaran.catat("Koneksi ke gateway BRI" + url + " tidak tersambung dalam batas waktu: "
					+ InfoTeknisPembayaran.potong(e.getMessage(), 200) + ". Periksa jaringan/firewall server.");
		} else if (simpanKeDb) {
			// Respons BRI sudah diterima; kegagalan terjadi saat menyimpan hasil ke DB.
			InfoTeknisPembayaran.catat("VA/transaksi diterima gateway BRI namun GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
		} else if (isGangguanJaringan(e)) {
			// Gangguan jaringan/DNS lain (mis. host tak teresolusi) — kendala infrastruktur.
			InfoTeknisPembayaran.catat("Gangguan jaringan/DNS ke gateway BRI" + url + ": "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200)
					+ ". Periksa koneksi internet/whitelist IP server.");
		} else if (hasil != null) {
			// Respons diterima namun ditolak/tak bisa diproses — tampilkan potongan respons server.
			InfoTeknisPembayaran.catat("Server BRI menolak/mengembalikan respons tak terduga"
					+ url + ": " + e.getClass().getSimpleName() + " - "
					+ InfoTeknisPembayaran.potong(e.getMessage(), 200) + ". Respons server: "
					+ InfoTeknisPembayaran.potong(hasil, 300));
		} else {
			InfoTeknisPembayaran.catat("Gagal memproses request/respons BRI" + url + ": "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
		}
	}

	public static BriRequest sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars, Double amount, String merchant_id,
			String postData, String bill_no, String virtual_account, String key,
			List<BriRequestDetail> briRequestDetails, List<BriRequestDetailBiaya> briRequestDetailBiayas,
			Boolean hapusCicilanSebelumnya) throws Exception {
		// Bersihkan "Informasi Teknis" lama agar kegagalan sebelumnya tidak bocor ke alert transaksi ini.
		InfoTeknisPembayaran.bersihkan();

		BriRequest briRequest = new BriRequest();

		// Dideklarasikan di luar try agar bisa dipakai menyusun "Informasi Teknis" di blok catch.
		String strURL = null;
		String hasil = null;
		boolean simpanKeDb = false;
		try {

			System.out.println("postData -> " + postData);

			try {
				strURL = (Common.getKonfigurasi("bri_gateway_url", "https://developer.bri.co.id/v1/api/briva")
						.getNilai());
				hasil = BriCommon.post(postData, strURL);
				System.out.println("Response body: ");
				System.out.println(hasil);

				JSONObject response = new JSONObject(hasil);
				JSONObject responseData = response.getJSONObject("data");

				briRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
				briRequest.setNama(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));

				briRequest.setTrxId(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));
				briRequest.setVa(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));
				briRequest.setBillNo(bill_no);
				briRequest.setMerchant_id(merchant_id);
				briRequest.setData(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));
				briRequest.setMerchant(merchant_id);
				briRequest.setResponse_code(response.getString("responseCode"));
				briRequest.setResponse_desc(response.getString("responseDescription"));
				briRequest.setMahasiswa(mahasiswa);
				briRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				briRequest.setJenisKegiatan(jenisKegiatan);
				briRequest.setJadwalPembayaran(jadwalPembayaran);
				briRequest.setSemester(semester);
				briRequest.setTahunAkademik(tahunAkademik);
				briRequest.setKeterangan(keterangan);
				briRequest.setPengurangan(pengurangan);
				briRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
				briRequest.setAmount(amount);
				briRequest.setResponse(hasil);
				briRequest.setRequest(postData);

				// Respons BRI sudah diterima; kegagalan setelah titik ini berarti gagal simpan ke DB.
				simpanKeDb = true;

				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.save(briRequest);
				session.getTransaction().commit();

				for (BriRequestDetail briRequestDetail : briRequestDetails) {
					briRequestDetail.setBriRequest(briRequest);
					session.getTransaction().begin();
					session.save(briRequestDetail);
					session.getTransaction().commit();
				}

				for (BriRequestDetailBiaya briRequestDetailBiaya : briRequestDetailBiayas) {
					briRequestDetailBiaya.setBriRequest(briRequest);
					session.getTransaction().begin();
					session.save(briRequestDetailBiaya);
					session.getTransaction().commit();
				}
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// Catat penyebab kegagalan untuk alert "Informasi Teknis" (alur tetap: cari request lama).
				catatKegagalanBri(e, strURL, hasil, simpanKeDb);
				// Common.tampilErrorJikaAdmin(e);
				System.out.println("Error = " + e.getMessage());
				Session session = HibernateUtil.currentNativeSession();
				if (mahasiswa != null) {
					briRequest = (BriRequest) session.createCriteria(BriRequest.class)
							.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id"))
							.add(Restrictions.eq("status", "Belum diproses")).setMaxResults(1).uniqueResult();
				} else if (biodataCalonMahasiswa != null) {
					briRequest = (BriRequest) session.createCriteria(BriRequest.class)
							.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
							.addOrder(Order.desc("id")).add(Restrictions.eq("status", "Belum diproses"))
							.setMaxResults(1).uniqueResult();
				}
				HibernateUtil.closeSession();
			}

		} catch (Exception e) {
			// Catat penyebab kegagalan untuk alert "Informasi Teknis" (alur tetap).
			catatKegagalanBri(e, strURL, hasil, simpanKeDb);
			Common.tampilErrorJikaAdmin(e);
		}

		return briRequest;
	}

	public static BriRequest onSaveBri(Siswa siswa, CalonSiswa calonSiswa, Collection<Tagihan> tag, final Double amn,
			boolean tampilkanFormPembayaran, Double deposit) throws Exception {
		// Bersihkan "Informasi Teknis" lama agar kegagalan sebelumnya tidak bocor ke alert transaksi ini.
		InfoTeknisPembayaran.bersihkan();

		if (amn == null || amn.intValue() == 0) {
			return null;
		}

		List<Tagihan> tagihans = new ArrayList<Tagihan>(tag);
		Collections.sort(tagihans);

		String keterangan = "";
		for (Tagihan tagihan : tagihans) {
			keterangan += tagihan.getId() + "-" + tagihan.getItemBiayaSekolah().getNama()
					+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")" : "")
					+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
					+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()) + ", ";
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 2);

		String bill_no = Common.getGeneratedBarCode();

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran_bri = Common.getKonfigurasi("tanggal_terakhir_pembayaran_bri", "")
				.getNilai();
		if (tanggal_terakhir_pembayaran_bri != null && !tanggal_terakhir_pembayaran_bri.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran_bri);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BriCommon.java:754");

			}
		}
		Session session = HibernateUtil.currentNativeSession();
		BriRequest briRequestData = (BriRequest) session.createCriteria(BriRequest.class)
				.add(Restrictions.ne("status", "Payment Sukses"))
				.add(Restrictions.or(Restrictions.eq("siswa", siswa), Restrictions.eq("calonSiswa", calonSiswa)))
				.add(Restrictions.gt("bill_expired", ais.ui.util.WaktuUtil.getDate()))
				.add(Restrictions.eq("keterangan", keterangan)).addOrder(Order.desc("id")).uniqueResult();
		System.out.println("briRequestData => " + briRequestData);
		if (briRequestData != null) {
			HibernateUtil.closeSession();
			return briRequestData;
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		if (tanggalExpired == null) {
			tanggalExpired = calendar.getTime();
		}

		String virtual_account = Common.getGeneratedAngkaDigit(10);

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bri_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BriCommon.java:782");

		}

		String merchant_id = Common.getKonfigurasi("bri_institution_code", "J104408").getNilai();
		String brivaNo = Common.getKonfigurasi("bri_briva_no", "77777").getNilai();

		String nama = siswa != null ? siswa.getNama() : (calonSiswa != null ? calonSiswa.getNama() : "");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("institutionCode", merchant_id);
		jsonObject.put("brivaNo", brivaNo);
		jsonObject.put("custCode", virtual_account);
		jsonObject.put("nama", nama);
		jsonObject.put("amount", amn);
		jsonObject.put("keterangan", "");
		jsonObject.put("expiredDate", datetime_expired);

		String postData = jsonObject.toString();

		final BriRequest briRequest = new BriRequest();

		// Dideklarasikan di luar try agar bisa dipakai menyusun "Informasi Teknis" di blok catch.
		String strURL = null;
		String hasil = null;
		boolean simpanKeDb = false;
		try {

			System.out.println("postData -> " + postData);

			try {
				strURL = (Common.getKonfigurasi("bri_gateway_url", "https://developer.bri.co.id/v1/api/briva")
						.getNilai());
				hasil = BriCommon.post(postData, strURL);
				System.out.println("Response body: ");
				System.out.println(hasil);

				JSONObject response = new JSONObject(hasil);
				JSONObject responseData = response.getJSONObject("data");

				briRequest.setHapusCicilanSebelumnya(true);
				briRequest.setNama(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));

				briRequest.setTrxId(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));
				briRequest.setVa(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));
				briRequest.setBillNo(bill_no);
				briRequest.setMerchant_id(merchant_id);
				briRequest.setData(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));
				briRequest.setMerchant(merchant_id);
				briRequest.setResponse_code(response.getString("responseCode"));
				briRequest.setResponse_desc(response.getString("responseDescription"));
				briRequest.setSiswa(siswa);
				briRequest.setCalonSiswa(calonSiswa);
				briRequest.setNilaiBiayaHarusDiBayars(amn);
				briRequest.setAmount(amn);
				briRequest.setResponse(hasil);
				briRequest.setRequest(postData);
				briRequest.setBill_expired(tanggalExpired);

				briRequest.setDeposit(deposit);
				briRequest.setKeterangan(keterangan);

				// Respons BRI sudah diterima; kegagalan setelah titik ini berarti gagal simpan ke DB.
				simpanKeDb = true;

				session.getTransaction().begin();
				session.save(briRequest);
				session.getTransaction().commit();

				for (Tagihan tagihan : tagihans) {
					BriRequestDetail briRequestDetail = new BriRequestDetail();
					briRequestDetail.setBriRequest(briRequest);
					briRequestDetail.setTagihan(tagihan);
					briRequestDetail.setNilai(tagihan.getNominal()+tagihan.getDenda());
					briRequestDetail.setKeterangan(tagihan.getItemBiayaSekolah().getNama()
							+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")"
									: "")
							+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
							+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()));
					session.getTransaction().begin();
					session.save(briRequestDetail);
					session.getTransaction().commit();
					
					
					tagihan.setVa(virtual_account);
					tagihan.setExpired(tanggalExpired);

					session.getTransaction().begin();
					session.update(tagihan);
					session.getTransaction().commit();
				}

			} catch (Exception e) {
				// Catat penyebab kegagalan untuk alert "Informasi Teknis" (alur tetap seperti semula).
				catatKegagalanBri(e, strURL, hasil, simpanKeDb);
				if (isGangguanJaringan(e)) {
					// KE-4: kegagalan jaringan/DNS ke API BRI (mis. developer.bri.co.id tak teresolusi /
					// server tanpa akses internet). Ini kendala INFRASTRUKTUR, bukan bug aplikasi: jangan
					// tampilkan error mentah ke pengguna & jangan gagalkan alur pemanggil (cetak kartu tetap
					// jalan). VA gagal dibuat -> di-handle di bawah (kembalikan null / pesan 'Transaksi Gagal').
					System.out.println("BRIVA tidak dapat dihubungi (jaringan/DNS): " + e.getMessage());
				} else {
					Common.tampilErrorJikaAdmin(e);
					System.out.println("Error = " + e.getMessage());
				}
			}

		} catch (Exception e) {
			// Catat penyebab kegagalan untuk alert "Informasi Teknis" (alur tetap).
			catatKegagalanBri(e, strURL, hasil, simpanKeDb);
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();
		if (tampilkanFormPembayaran) {
			if (briRequest != null && briRequest.getVa() != null && !briRequest.getVa().trim().isEmpty()) {

				String code = briRequest.getVa();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
						+ briRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String myUrl = "/common/bri/no_va.zul?va=" + URLEncoder.encode(brivaNo + briRequest.getVa(), "UTF-8")
						+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8") + "&nama="
						+ URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
						+ URLEncoder.encode(Common.dateFormat.get().format(briRequest.getBill_expired()), "UTF-8")
						+ "&biayaTotal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn + biayaAdministrasi), "UTF-8")
						+ "&qr="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(),
								"UTF-8")
						+ "&terbilang="
						+ URLEncoder.encode(IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)), "UTF-8")
						+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

				Common.displayWindow(myUrl, true, "65%");

			} else {
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}
		// KE-4: bila VA tidak berhasil dibuat (mis. API BRI tak dapat dihubungi), kembalikan null agar
		// pemanggil (mis. CalonSiswaAction.onCetakKartu) melewati blok info BRIVA dengan bersih dan TIDAK
		// mencetak kode pembayaran palsu seperti "77777null". Kartu/registrasi tetap tercetak tanpa VA.
		if (briRequest == null || briRequest.getVa() == null || briRequest.getVa().trim().isEmpty()) {
			return null;
		}
		return briRequest;
	}

}
