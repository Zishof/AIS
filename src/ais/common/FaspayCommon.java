package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
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
import ais.database.model.faspay.FaspayRequest;
import ais.database.model.faspay.FaspayRequestDetail;
import ais.database.model.faspay.FaspayRequestDetailBiaya;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyWindow;

public class FaspayCommon {

	public static MyButtonConfig createButton() {
		File fileViaFaspay = new File(Common.REAL_PATH + "/img/faspay-logo.jpg");

		LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_FASPAY,
				LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_FASPAY_STR);

		if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
			fileViaFaspay = lainMahasiswa.ambilFile();
			File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaFaspay.getName());
			boolean ada = fileDiImg.exists();
//			System.out.println("fileViaFaspay = " + fileViaFaspay + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
			if (!ada) {
				FileInputStream fileInputStream;
				try {
					fileInputStream = new FileInputStream(fileViaFaspay);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FaspayCommon.java:90");
				}

			}
		}

		MyButtonConfig bayarViaFaspay = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_faspay", "Bayar via Faspay").getNilai(),
				"/img/" + fileViaFaspay.getName());
		return bayarViaFaspay;
	}

	@SuppressWarnings("unchecked")
	public static List<FaspayRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<FaspayRequestDetailBiaya> faspayRequestDetailBiayas = new ArrayList<FaspayRequestDetailBiaya>();
		Rows rows = (Rows) gridss.getRows();
		if (rows != null && rows.getChildren() != null) {
			List<Row> myRows = rows.getChildren();
//			System.out.println("myRows -> " + myRows.size());
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/FaspayCommon.java:130");
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

				FaspayRequestDetailBiaya faspayRequestDetailBiaya = new FaspayRequestDetailBiaya();
				faspayRequestDetailBiaya.setDetailBiaya(detailBiaya);
				faspayRequestDetailBiaya.setNilai(biaya);
				faspayRequestDetailBiayas.add(faspayRequestDetailBiaya);
			}
		}
		return faspayRequestDetailBiayas;
	}

	public static List<FaspayRequestDetail> populateFaspayRequestDetailDariDetailBiaya(
			List<FaspayRequestDetailBiaya> faspayRequestDetailBiayas) {
		List<FaspayRequestDetail> faspayRequestDetails = new ArrayList<FaspayRequestDetail>();

		int i = 1;
		for (FaspayRequestDetailBiaya faspayRequestDetailBiaya : faspayRequestDetailBiayas) {
			FaspayRequestDetail faspayRequestDetail = new FaspayRequestDetail();
			faspayRequestDetail.setPengaturanPembayaranBulanan(null);
			faspayRequestDetail.setItemBiaya(faspayRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			faspayRequestDetail.setKeterangan(faspayRequestDetailBiaya.getKeterangan());
			faspayRequestDetail.setNilai(faspayRequestDetailBiaya.getNilai());
			faspayRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			faspayRequestDetail.setKe(i);
			faspayRequestDetails.add(faspayRequestDetail);
			i++;
		}

		return faspayRequestDetails;
	}

	public static List<FaspayRequestDetail> populateFaspayRequestDetail(HttpServletRequest request, Mahasiswa mahasiswa,
			String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
//		System.out.println("jenis => " + jenis + ", data => " + data);
		List<FaspayRequestDetail> faspayRequestDetails = new ArrayList<FaspayRequestDetail>();

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

			FaspayRequestDetail faspayRequestDetail = new FaspayRequestDetail();
			faspayRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			faspayRequestDetail.setDetailBiaya(detailBiaya);
			faspayRequestDetail.setItemBiaya(itemBiaya);
			faspayRequestDetail.setKeterangan(keterangan);
			faspayRequestDetail.setNilai(nilai);
			faspayRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			faspayRequestDetail.setKe(i);
			faspayRequestDetail.setDenda(0.0);
			faspayRequestDetail.setNilaiAsli(nilai);
			faspayRequestDetails.add(faspayRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return faspayRequestDetails;
	}

	public static List<FaspayRequestDetail> populateFaspayRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<FaspayRequestDetail> faspayRequestDetails = new ArrayList<FaspayRequestDetail>();

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

				FaspayRequestDetail faspayRequestDetail = new FaspayRequestDetail();

				Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
						: myItemBiaya.getSelectedItem().getValue();
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
						.getPengaturanPembayaranBulanan();
				ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();
				if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
					itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				} else if (jenisBiaya != null && jenisBiaya instanceof ItemBiaya) {
					itemBiaya = (ItemBiaya) jenisBiaya;
				}

				faspayRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				faspayRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				faspayRequestDetail.setItemBiaya(itemBiaya);
				faspayRequestDetail.setKeterangan(keterangan.getValue());
				faspayRequestDetail.setNilai(jumlahCicilan.getValue());
				faspayRequestDetail.setTanggal(tanggal.getValue());
				faspayRequestDetail.setKe(i);

				faspayRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				faspayRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, faspayRequestDetail.getTanggal(),
								jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						faspayRequestDetail.setDenda(denda);
						faspayRequestDetail.setNilaiAsli(nom);
					}
				}

				faspayRequestDetails.add(faspayRequestDetail);
				i++;
			}
		}

		return faspayRequestDetails;
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

				List<FaspayRequestDetailBiaya> faspayRequestDetailBiayas = new ArrayList<FaspayRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					FaspayRequestDetailBiaya faspayRequestDetailBiaya = new FaspayRequestDetailBiaya();
					faspayRequestDetailBiaya.setDetailBiaya(detailBiaya);
					faspayRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					faspayRequestDetailBiayas.add(faspayRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += faspayRequestDetailBiaya.getNilai();
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				FaspayCommon.onSaveFaspay(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						FaspayCommon.populateFaspayRequestDetailDariDetailBiaya(faspayRequestDetailBiayas),
						faspayRequestDetailBiayas, null);

			}
		}

	}

	public static boolean onPilihFaspay(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<FaspayRequestDetail> faspayRequestDetails,
			List<FaspayRequestDetailBiaya> faspayRequestDetailBiayas, String payment_channel,
			String payment_channel_name, Event event) throws Exception {

		String merchant_id = Common.getKonfigurasi("faspay_merchant_id", "31503").getNilai().trim();
		String merchant = Common.getKonfigurasi("faspay_merchant_name", "eCampus").getNilai().trim();
		String UserID = Common.getKonfigurasi("faspay_user_id", "bot31503").getNilai().trim();
		String Password = Common.getKonfigurasi("faspay_password", "W4TYRmO0").getNilai().trim();

		String items = "";

		for (FaspayRequestDetail detail : faspayRequestDetails) {
			if (detail.getIdCicilan() == null) {
				if (detail.getPengaturanPembayaranBulanan() != null) {
					String item = "<item>\n<product>"
							+ detail.getPengaturanPembayaranBulanan().getDetailBiaya().getItemBiaya().getNama()
							+ " bulan " + detail.getPengaturanPembayaranBulanan().getNamaBulan()
							+ "</product>\n<qty>1</qty>\n<amount>" + detail.getNilai().intValue() + "00</amount>\n"
							+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n"
							+ "</item>\n";
					items += item;
				} else {
					String item = "<item>\n<product>" + detail.getItemBiaya().getNama()
							+ "</product>\n<qty>1</qty>\n<amount>" + detail.getNilai().intValue() + "00</amount>\n"
							+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n"
							+ "</item>\n";
					items += item;
				}
			}
		}

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double
					.parseDouble(Common.getKonfigurasi("faspay_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/FaspayCommon.java:406");

		}

		if (biayaAdministrasi > 0.1) {
			String item = "<item>\n<product>Biaya Administrasi</product>\n<qty>1</qty>\n<amount>"
					+ biayaAdministrasi.intValue() + "00</amount>\n"
					+ "<payment_plan>01</payment_plan>\n<merchant_id></merchant_id>\n<tenor>00</tenor>\n" + "</item>\n";
			items += item;
		}

		String bill_no = Common.getGeneratedBarCode();

		String signature = AeSimpleSHA1.SHA1(MD5.crypt(UserID + Password + bill_no));

		System.out.println("signature = " + signature);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) + 12);

		String cust_name = "";
		String cust_no = "";
		String msisdn = "";
		String email = "";
		String address = "";
		String city = "";
		String region = "";
		String poscode = "";
		if (mahasiswa != null) {
			cust_name = mahasiswa.getNama();
			cust_no = mahasiswa.getNim();
			email = mahasiswa.getEmail().split(",")[0];
			address = mahasiswa.getAlamat();
			Session session = HibernateUtil.currentNativeSession();
			BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
			if (biodataMahasiswa != null) {
				city = biodataMahasiswa.getKota() == null ? "" : biodataMahasiswa.getKota().getNama();
				region = biodataMahasiswa.getPropinsi() == null ? "" : biodataMahasiswa.getPropinsi().getNama();
				poscode = biodataMahasiswa.getKodepos();
				msisdn = biodataMahasiswa.getHp();
				if (!Common.isNumber(msisdn.replaceAll("\\+", "").trim())) {
					msisdn = "0810000000";
				}
			}
		} else if (biodataCalonMahasiswa != null) {
			cust_name = biodataCalonMahasiswa.getNama();
			cust_no = biodataCalonMahasiswa.getNoRegistrasi();
			msisdn = biodataCalonMahasiswa.getTeleponRumah();
			if (!Common.isNumber(msisdn.replaceAll("\\+", "").trim())) {
				msisdn = "0810000000";
			}
			email = biodataCalonMahasiswa.getEmail().split(",")[0];
			address = biodataCalonMahasiswa.getAlamat();
			city = biodataCalonMahasiswa.getKotaCalon() == null ? "" : biodataCalonMahasiswa.getKotaCalon().getNama();
			region = biodataCalonMahasiswa.getPropinsiCalon() == null ? ""
					: biodataCalonMahasiswa.getPropinsiCalon().getNama();
			poscode = biodataCalonMahasiswa.getKodePos();
		}

		String postData = "<faspay>\n<request>Post Data Transaksi</request>\n<merchant_id>" + merchant_id
				+ "</merchant_id>\n<merchant>" + merchant + "</merchant>\n<bill_no>" + bill_no + "</bill_no>\n"
				+ "<bill_reff>" + cust_no + "</bill_reff>\n<bill_date>"
				+ dateFormat.format(ais.ui.util.WaktuUtil.getDate()) + "</bill_date>\n<bill_expired>"
				+ dateFormat.format(calendar.getTime()) + "</bill_expired>\n" + "<bill_desc>"
				+ jenisKegiatan.getNamaKegiatan() + "</bill_desc>\n<bill_currency>IDR</bill_currency>\n"
				+ "<bill_gross>" + (biayaAdministrasi.intValue() + amn.intValue())
				+ "00</bill_gross>\n<bill_tax>0</bill_tax>\n" + "<bill_miscfee>0</bill_miscfee>\n<bill_total>"
				+ (biayaAdministrasi.intValue() + amn.intValue()) + "00</bill_total>\n<cust_no>" + cust_no
				+ "</cust_no>\n<cust_name>" + cust_name + "</cust_name>\n<payment_channel>" + payment_channel
				+ "</payment_channel>\n<pay_type>1</pay_type>\n<bank_userid>" + cust_no + "</bank_userid>\n<msisdn>"
				+ msisdn + "</msisdn>\n<email>" + email + "</email>\n<terminal>10</terminal>\n<billing_address>"
				+ address + "</billing_address>\n" + "<billing_address_city>" + city
				+ "</billing_address_city>\n<billing_address_region>" + region
				+ "</billing_address_region>\n<billing_address_state>Indonesia</billing_address_state>\n"
				+ "<billing_address_poscode>" + poscode + "</billing_address_poscode>\n"
				+ "<billing_address_country_code>ID</billing_address_country_code>\n<receiver_name_for_shipping>"
				+ cust_name + "</receiver_name_for_shipping>\n<shipping_address>" + address
				+ "</shipping_address>\n<shipping_address_city>" + city + "</shipping_address_city>\n"
				+ "<shipping_address_region>" + region + "</shipping_address_region>\n"
				+ "<shipping_address_state>Indonesia</shipping_address_state>\n<shipping_address_poscode>" + poscode
				+ "</shipping_address_poscode>\n" + items
				+ "\n<reserve1></reserve1>\n<reserve2></reserve2>\n<signature>" + signature + "</signature>\n</faspay>";

		FaspayRequest faspayRequestTemp = FaspayCommon.sendRequest(postData, mahasiswa, biodataCalonMahasiswa,
				jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
				nilaiBiayaHarusDiBayars, amn, merchant_id, signature, bill_no, payment_channel_name,
				faspayRequestDetails, faspayRequestDetailBiayas, true);
		if (faspayRequestTemp != null && faspayRequestTemp.getId() != null && faspayRequestTemp.getUrl() != null
				&& !faspayRequestTemp.getUrl().trim().isEmpty()) {

			Session session = HibernateUtil.currentNativeSession();
			FaspayRequest faspayRequest = (FaspayRequest) session.createCriteria(FaspayRequest.class)
					.add(Restrictions.idEq(faspayRequestTemp.getId())).uniqueResult();
			HibernateUtil.closeSession();
			if (faspayRequest != null && faspayRequest.getId() != null && faspayRequest.getUrl() != null
					&& !faspayRequest.getUrl().trim().isEmpty()) {

				String code = faspayRequest.getTrxId();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
						+ faspayRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String myUrl = "/common/faspay/no_va.zul?va=" + URLEncoder.encode(faspayRequest.getTrxId(), "UTF-8")
						+ "&nominal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(faspayRequest.getAmount()), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8")
						+ "&biayaTotal="
						+ URLEncoder.encode(
								"Rp. " + Common.numberFormat.get().format(faspayRequest.getAmount() + biayaAdministrasi),
								"UTF-8")
						+ "&qr="
						+ URLEncoder.encode(
								Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(), "UTF-8")
						+ "&terbilang="
						+ URLEncoder.encode(
								IndonesianNumberToWords.convert((long) (faspayRequest.getAmount() + biayaAdministrasi)),
								"UTF-8")
						+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

				Common.displayWindow(myUrl, true, "65%");
			} else {
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
			}

		} else {
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	@SuppressWarnings({ "deprecation" })
	public static boolean onSaveFaspay(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final JadwalPembayaran jadwalPembayaran, final Integer semester, final String tahunAkademik,
			final String keterangan, final Double pengurangan, final Double nilaiBiayaHarusDiBayars,
			final List<FaspayRequestDetail> faspayRequestDetails,
			final List<FaspayRequestDetailBiaya> faspayRequestDetailBiayas, final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String strURL = (Common.getKonfigurasi("faspay_payment_channel_url",
				"http://faspaydev.mediaindonusa.com/pws/100001/182xx00010100000").getNilai());

		String merchant_id = Common.getKonfigurasi("faspay_merchant_id", "31503").getNilai().trim();
		String merchant = Common.getKonfigurasi("faspay_merchant_name", "eCampus").getNilai().trim();
		String UserID = Common.getKonfigurasi("faspay_user_id", "bot31503").getNilai().trim();
		String Password = Common.getKonfigurasi("faspay_password", "W4TYRmO0").getNilai().trim();

		String signature = AeSimpleSHA1.SHA1(MD5.crypt(UserID + Password));

		String postData = "<?xml version=\"1.0\"?>\n<faspay>\n"
				+ "<request>Request List of Payment Gateway</request>\n<merchant_id>" + merchant_id
				+ "</merchant_id>\n<merchant>" + merchant + "</merchant>\n<signature>" + signature
				+ "</signature>\n</faspay>";

		System.out.println("postData = " + postData);

		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "text/xml; charset=ISO-8859-1");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");
			String hasil = post.getResponseBodyAsString();
			System.out.println(hasil);

			JSONObject jSONObject = XML.toJSONObject(hasil);
			JSONObject faspay = jSONObject.getJSONObject("faspay");
			TreeMap<String, String> channel = new TreeMap<String, String>();
			try {
				JSONArray jsonArray = faspay.getJSONArray("payment_channel");
				System.out.println("jsonArray = " + jsonArray);
				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject json = jsonArray.getJSONObject(i);
						System.out.println("json = " + json);
						channel.put(json.get("pg_code").toString(), json.get("pg_name").toString());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			} catch (Exception e) {
				try {
					JSONObject json = faspay.getJSONObject("payment_channel");
					System.out.println("json = " + json);
					channel.put(json.get("pg_code").toString(), json.get("pg_name").toString());
				} catch (Exception ee) {
					Common.tampilErrorJikaAdmin(ee);
				}
			}

			if (channel.size() == 1) {
				String kode = channel.keySet().iterator().next();
				String nama = channel.get(kode);
				onPilihFaspay(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester,
						tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, faspayRequestDetails,
						faspayRequestDetailBiayas, kode, nama, event);
			} else if (!channel.isEmpty()) {
				final MyWindow window = new MyWindow("Pilihlah Bank", "none", false);
				window.setHeight("200px");
				window.setWidth("500px");

				Radiogroup radiogroup = new Radiogroup();
				radiogroup.setParent(window);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(radiogroup);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Rows rows = new Rows();
				rows.setParent(grid);
				for (final String kode : channel.keySet()) {
					final String nama = channel.get(kode);
					Row row = new Row();row.setValign("top");
					row.setParent(rows);
					MyRadioConfig radio = new MyRadioConfig(nama);
					radio.setParent(row);
					radio.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onPilihFaspay(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan,
											jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
											nilaiBiayaHarusDiBayars, faspayRequestDetails, faspayRequestDetailBiayas,
											kode, nama, event);
								}
							});
							window.detach();

						}
					});
				}

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.onModal();

			} else {
				MyMessageboxConfig.show("Kanal pembayaran tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}

		} catch (Exception e) {
			MyMessageboxConfig.show("Kanal pembayaran tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		return true;
	}

	@SuppressWarnings("deprecation")
	public static FaspayRequest sendRequest(String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, String merchant_id, String signature, String bill_no,
			String payment_channel_name, List<FaspayRequestDetail> faspayRequestDetails,
			List<FaspayRequestDetailBiaya> faspayRequestDetailBiayas, Boolean hapusCicilanSebelumnya) throws Exception {
		// Bersihkan detail kegagalan lama agar alert tidak menampilkan penyebab transaksi sebelumnya.
		InfoTeknisPembayaran.bersihkan();
		postData = postData.replaceAll("&", "dan");

		// curl_init and url
		String strURL = (Common
				.getKonfigurasi("faspay_gateway_url", "http://faspaydev.mediaindonusa.com/pws/300002/183xx00010100000")
				.getNilai());
		String redirectURL = (Common
				.getKonfigurasi("faspay_redirect_url", "http://faspaydev.mediaindonusa.com/pws/100003/0830000010100000")
				.getNilai());

		FaspayRequest faspayRequest = new FaspayRequest();
		System.out.println("postData = " + postData);

		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "text/xml; charset=ISO-8859-1");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			System.out.println(hasil);

			JSONObject jSONObject = XML.toJSONObject(hasil);
			JSONObject faspay = jSONObject.getJSONObject("faspay");
			System.out.println("jSONObject = " + faspay);

			String response_code = faspay.optString("response_code", "");
			if (!response_code.trim().isEmpty() && !response_code.trim().equals("00")) {
				// Faspay menolak permintaan — catat kode + pesan server apa adanya agar
				// pengguna/admin tahu penyebab pastinya (bukan sekadar "Transaksi Gagal").
				String response_desc = faspay.optString("response_desc", "");
				InfoTeknisPembayaran.catat("Server Faspay menolak permintaan, kode status=" + response_code
						+ (response_desc.trim().isEmpty() ? "" : ", pesan=" + response_desc.trim())
						+ ". Respons server: " + InfoTeknisPembayaran.potong(hasil, 300) + " (URL: " + strURL + ")");
			}

			String trx_id = faspay.isNull("trx_id") ? "" : ais.common.CommonJSONUtil.ambilLong(faspay,"trx_id") + "";

			String url = redirectURL + "/" + signature + "?trx_id=" + trx_id + "&merchant_id=" + merchant_id
					+ "&bill_no=" + bill_no;

			System.out.println("url = " + url);

			faspayRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			faspayRequest.setNama(faspay.getString("response"));
			faspayRequest.setUrl(url);
			faspayRequest.setTrxId(trx_id);
			faspayRequest.setBillNo(bill_no);
			faspayRequest.setMerchant_id(merchant_id);
			faspayRequest.setSignature(signature);
			faspayRequest.setMerchant(faspay.getString("merchant"));
			faspayRequest.setResponse_code(faspay.getString("response_code"));
			faspayRequest.setResponse_desc(faspay.getString("response_desc"));
			faspayRequest.setMahasiswa(mahasiswa);
			faspayRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			faspayRequest.setJenisKegiatan(jenisKegiatan);
			faspayRequest.setJadwalPembayaran(jadwalPembayaran);
			faspayRequest.setSemester(semester);
			faspayRequest.setTahunAkademik(tahunAkademik);
			faspayRequest.setKeterangan(keterangan);
			faspayRequest.setPengurangan(pengurangan);
			faspayRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
			faspayRequest.setAmount(amount);
			faspayRequest.setResponse(faspay.toString());
			faspayRequest.setRequest(postData);
			faspayRequest.setPayment_channel_name(payment_channel_name);

			try {
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();

				session.save(faspayRequest);
				for (FaspayRequestDetail faspayRequestDetail : faspayRequestDetails) {
					faspayRequestDetail.setFaspayRequest(faspayRequest);
					session.save(faspayRequestDetail);
				}

				for (FaspayRequestDetailBiaya faspayRequestDetailBiaya : faspayRequestDetailBiayas) {
					faspayRequestDetailBiaya.setFaspayRequest(faspayRequest);
					session.save(faspayRequestDetailBiaya);
				}
				session.getTransaction().commit();
			} catch (Exception e) {
				// Faspay sudah menerima transaksi namun penyimpanan lokal gagal — beri tahu
				// penyebabnya supaya admin memeriksa Error Log/DB, bukan menyalahkan gateway.
				InfoTeknisPembayaran.catat("Transaksi diterima gateway namun GAGAL disimpan di aplikasi: "
						+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
				Common.tampilErrorJikaAdmin(e);
				try {
					HibernateUtil.closeSession();
					post.releaseConnection();
				} catch (Exception ee) {
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/common/FaspayCommon.java:sendRequest-simpan");
				}
				return null;
			}

		} catch (java.net.ConnectException ce) {
			// Gateway Faspay tak bisa dihubungi (unreachable/refused). Kembalikan null seperti
			// semula; pemanggil sudah cek null lalu menampilkan alert dengan info teknis ini.
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway Faspay (" + strURL + "): "
					+ InfoTeknisPembayaran.potong(ce.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
			Common.tampilErrorJikaAdmin(ce);
			try {
				HibernateUtil.closeSession();
				post.releaseConnection();
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/common/FaspayCommon.java:sendRequest-connect");
			}
			return null;
		} catch (java.net.SocketTimeoutException te) {
			// Gateway Faspay tidak merespons dalam batas waktu baca (timeout).
			InfoTeknisPembayaran.catat("Gateway Faspay (" + strURL + ") tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(te.getMessage(), 200) + ". Coba beberapa saat lagi.");
			Common.tampilErrorJikaAdmin(te);
			try {
				HibernateUtil.closeSession();
				post.releaseConnection();
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/common/FaspayCommon.java:sendRequest-timeout");
			}
			return null;
		} catch (org.apache.commons.httpclient.ConnectTimeoutException cte) {
			// ConnectTimeoutException = subclass InterruptedIOException, BUKAN ConnectException,
			// jadi perlu catch tersendiri: koneksi tidak tersambung dalam batas waktu.
			InfoTeknisPembayaran.catat("Koneksi ke gateway Faspay (" + strURL + ") tidak tersambung dalam batas waktu: "
					+ InfoTeknisPembayaran.potong(cte.getMessage(), 200) + ". Periksa jaringan/firewall server.");
			Common.tampilErrorJikaAdmin(cte);
			try {
				HibernateUtil.closeSession();
				post.releaseConnection();
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/common/FaspayCommon.java:sendRequest-connecttimeout");
			}
			return null;
		} catch (Exception e) {
			// Kegagalan request/proses lain (mis. respons tak bisa diparse) — tetap kembalikan null.
			InfoTeknisPembayaran.catat("Gagal memproses request/respons Faspay (" + strURL + "): "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
			try {
				HibernateUtil.closeSession();
				post.releaseConnection();
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/common/FaspayCommon.java:777");
			}
			return null;
		} finally {
			post.releaseConnection();
		}

		HibernateUtil.closeSession();

		return faspayRequest;
	}

}
