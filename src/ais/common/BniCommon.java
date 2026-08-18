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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
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

import com.bni.encrypt.BNIHash;

import ais.action.master.bni.BniRequestAction;
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
import ais.database.model.bni.BniRequest;
import ais.database.model.bni.BniRequestDetail;
import ais.database.model.bni.BniRequestDetailBiaya;
import ais.database.model.file.LampiranLain;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.PesertaPunyaProdukKursus;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class BniCommon {

	public static MyButtonConfig createButton() {
		File fileViaBni = new File(Common.REAL_PATH + "/img/bni-logo.png");
		try {
			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BNI,
					LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BNI_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileViaBni = lainMahasiswa.ambilFile();
				File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaBni.getName());
				boolean ada = fileDiImg.exists();
//				System.out.println("fileViaBni = " + fileViaBni + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
				if (!ada) {
					FileInputStream fileInputStream = new FileInputStream(fileViaBni);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				}
			}
		} catch (Exception e) {

			Common.tampilErrorJikaAdmin(e);
		}

		MyButtonConfig bayarViaBni = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_bni", "Bayar via Bni").getNilai(),
				"/img/" + fileViaBni.getName());
		return bayarViaBni;
	}

	@SuppressWarnings("unchecked")
	public static List<BniRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<BniRequestDetailBiaya> bniRequestDetailBiayas = new ArrayList<BniRequestDetailBiaya>();
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BniCommon.java:127");
				}

				if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					for (MyDoubleboxMin kurang : pengurangan) {
						DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
						if (penguranganItemBiaya != null && penguranganItemBiaya.getId().equals(detailBiaya.getId())) {
							biaya = kurang.getValue() == null ? 0.0 : kurang.getValue();
							break;
						}
					}
				}

				BniRequestDetailBiaya bniRequestDetailBiaya = new BniRequestDetailBiaya();
				bniRequestDetailBiaya.setDetailBiaya(detailBiaya);
				bniRequestDetailBiaya.setNilai(biaya);
				bniRequestDetailBiayas.add(bniRequestDetailBiaya);
			}
		}
		return bniRequestDetailBiayas;
	}

	public static List<BniRequestDetail> populateBniRequestDetailDariDetailBiaya(
			List<BniRequestDetailBiaya> bniRequestDetailBiayas) {
		List<BniRequestDetail> bniRequestDetails = new ArrayList<BniRequestDetail>();

		int i = 1;
		for (BniRequestDetailBiaya bniRequestDetailBiaya : bniRequestDetailBiayas) {
			BniRequestDetail bniRequestDetail = new BniRequestDetail();
			bniRequestDetail.setPengaturanPembayaranBulanan(null);
			bniRequestDetail.setItemBiaya(bniRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			bniRequestDetail.setKeterangan(bniRequestDetailBiaya.getKeterangan());
			bniRequestDetail.setNilai(bniRequestDetailBiaya.getNilai());
			bniRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			bniRequestDetail.setKe(i);
			bniRequestDetails.add(bniRequestDetail);
			i++;
		}

		return bniRequestDetails;
	}

	public static List<BniRequestDetail> populateBniRequestDetail(HttpServletRequest request, Mahasiswa mahasiswa,
			String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
//		System.out.println("jenis => " + jenis + ", data => " + data);
		List<BniRequestDetail> bniRequestDetails = new ArrayList<BniRequestDetail>();

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

			BniRequestDetail bniRequestDetail = new BniRequestDetail();
			bniRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			bniRequestDetail.setDetailBiaya(detailBiaya);
			bniRequestDetail.setItemBiaya(itemBiaya);
			bniRequestDetail.setKeterangan(keterangan);
			bniRequestDetail.setNilai(nilai);
			bniRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			bniRequestDetail.setKe(i);
			bniRequestDetail.setDenda(0.0);
			bniRequestDetail.setNilaiAsli(nilai);
			bniRequestDetails.add(bniRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return bniRequestDetails;
	}

	public static List<BniRequestDetail> populateBniRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<BniRequestDetail> bniRequestDetails = new ArrayList<BniRequestDetail>();

		int i = 1;
		for (Row row : mycicilanrows) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan.getValue() != null
					&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {

				CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
						&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);

				String val = cicilanPembayaran == null ? null : cicilanPembayaran.getValidator();
				if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null")) {
					Tbmuser tbmuser = Common.getCurrentUser();
					val = (tbmuser == null ? "" : tbmuser.toString());

				}

				BniRequestDetail bniRequestDetail = new BniRequestDetail();

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

				bniRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				bniRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				bniRequestDetail.setItemBiaya(itemBiaya);
				bniRequestDetail.setKeterangan(keterangan == null ? null : keterangan.getValue());
				bniRequestDetail.setNilai(jumlahCicilan.getValue());
				bniRequestDetail.setTanggal(tanggal.getValue());
				bniRequestDetail.setKe(i);

				bniRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				bniRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, bniRequestDetail.getTanggal(), jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						bniRequestDetail.setDenda(denda);
						bniRequestDetail.setNilaiAsli(nom);
					}
				}

				bniRequestDetails.add(bniRequestDetail);
				i++;
			}
		}

		return bniRequestDetails;
	}

	public static BniRequest bayarCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa, JenisKegiatan jenisKegiatan,
			boolean tampilInfo) throws Exception {
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
//			System.out.println("jadwalPembayaran => " + jadwalPembayaran);
			if (jadwalPembayaran != null) {
				Double nilaiBiayaHarusDiBayars = 0.0;

				String cicilan = "";
				List<BniRequestDetailBiaya> bniRequestDetailBiayas = new ArrayList<BniRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					BniRequestDetailBiaya bniRequestDetailBiaya = new BniRequestDetailBiaya();
					bniRequestDetailBiaya.setDetailBiaya(detailBiaya);
					bniRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					bniRequestDetailBiayas.add(bniRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += bniRequestDetailBiaya.getNilai();

					ItemBiaya itemBiaya = detailBiaya.getItemBiaya();

					cicilan += cicilan.isEmpty()
							? ("Item-" + itemBiaya.getId().toString() + "-" + bniRequestDetailBiaya.getNilai() + "-"
									+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId())
							: "," + ("Item-" + itemBiaya.getId().toString() + "-" + bniRequestDetailBiaya.getNilai()
									+ "-" + detailBiaya.getBayarKe() + "-" + detailBiaya.getId());
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				return BniCommon.onSaveBni(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						BniCommon.populateBniRequestDetailDariDetailBiaya(bniRequestDetailBiayas),
						bniRequestDetailBiayas, tampilInfo, null, cicilan);

			}

		}
		return null;
	}

	public static BniRequest onPilihBni(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<BniRequestDetail> bniRequestDetails,
			List<BniRequestDetailBiaya> bniRequestDetailBiayas, boolean tampilInfo, Event event, String cicilan)
			throws Exception {

		String merchant_id = Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
		String Password = Common.getKonfigurasi("bni_password", "685dedd9f045787873794ead6276f8bf").getNilai().trim();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();

		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);

		String jam_terakhir_pembayaran = Common.getKonfigurasi("jam_terakhir_pembayaran", "").getNilai();
		if (jam_terakhir_pembayaran != null && !jam_terakhir_pembayaran.trim().isEmpty()) {
			try {
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.set(Calendar.HOUR_OF_DAY,
						calendar1.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(jam_terakhir_pembayaran));
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				calendar = calendar1;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BniCommon.java:403");
			}
		}

		String bill_no = Common.getGeneratedBarCode();

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran = Common.getKonfigurasi("tanggal_terakhir_pembayaran", "").getNilai();
		if (tanggal_terakhir_pembayaran != null && !tanggal_terakhir_pembayaran.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran);
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.setTime(tanggalExpired);
				calendar1.set(Calendar.HOUR_OF_DAY, 23);
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				tanggalExpired = calendar1.getTime();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:420");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		String virtual_account = "";

		boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bni");

		if (Common.bolehKonfigurasi("angka_va_bni_menggunakan_nim")) {
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bni", "8").getNilai()
					+ (tambahkanMerchanId ? merchant_id : "") + (mahasiswa != null ? mahasiswa.getNim()
							: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi() : ""));
		} else {
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_bni", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:441");

			}
			if (Common.bolehKonfigurasi("menggunakan_prefix_bni")) {
				virtual_account = Common.getKonfigurasi("angka_prefix_va_bni", "8").getNilai()
						+ (tambahkanMerchanId ? merchant_id : "") + Common.getGeneratedAngkaDigit(generatedAngkaDigit);
			} else {
				virtual_account = Common.getGeneratedAngkaDigit(generatedAngkaDigit);
			}
		}

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:455");

		}

		String description = "";
		if (!tambahkanMerchanId) {
			description = ",\"description\":\"" + keterangan + "\"";
		}

		int generatedAngkaDigit = 16;
		try {
			generatedAngkaDigit = Integer
					.parseInt(Common.getKonfigurasi("virtual_account_angka_digit_bni", "16").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:468");

		}
		virtual_account = (virtual_account + "00000000000000000").substring(0, generatedAngkaDigit);

		String type = Common.getKonfigurasi("type_transaksi_bni", "createbilling").getNilai();
		String billing_type = Common.getKonfigurasi("billing_type_bni", "c").getNilai();

		int trx_amount = (biayaAdministrasi.intValue() + amn.intValue());

//		trx_amount = -1000;

//		virtual_account="882712899694";

//		bill_no = "2333333333333333333333333333333333333333333333333333333300";

		// BNIHash hash = new BNIHash();
		String data = "{\"customer_email\":\""
				+ (mahasiswa != null ? mahasiswa.getEmail().split(",")[0].trim()
						: (biodataCalonMahasiswa == null ? "test@email.com"
								: biodataCalonMahasiswa.getEmail().split(",")[0].trim()))
				+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired + "\",\"client_id\":\""
				+ merchant_id + "\",\"customer_phone\":\""
				+ (mahasiswa != null ? mahasiswa.getTelp()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getHp() : ""))
				+ "\",\"customer_name\":\""
				+ (mahasiswa != null
						? (mahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") + " " + mahasiswa.getNim())
						: (biodataCalonMahasiswa != null
								? (biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") + " "
										+ biodataCalonMahasiswa.getNoRegistrasi())
								: ""))
				+ "\",\"type\":\"" + type + "\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
				+ trx_amount + "\",\"billing_type\":\"" + billing_type + "\" " + description + " }";

		if (Common.bolehKonfigurasi("aktifkan_konfigurasi_abnormal_duplicate_trx_id", Konfigurasi.TIDAK_AKTIF)) {

			bill_no = Common.getKonfigurasi("konfigurasi_abnormal_duplicate_trx_id", bill_no).getNilai();

			data = "{\"customer_email\":\""
					+ (mahasiswa != null ? mahasiswa.getEmail().split(",")[0].trim()
							: (biodataCalonMahasiswa == null ? "test@email.com"
									: biodataCalonMahasiswa.getEmail().split(",")[0].trim()))
					+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired
					+ "\",\"client_id\":\"" + merchant_id + "\",\"customer_phone\":\""
					+ (mahasiswa != null ? mahasiswa.getTelp()
							: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getHp() : ""))
					+ "\",\"customer_name\":\""
					+ (mahasiswa != null
							? (mahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") + " " + mahasiswa.getNim())
							: (biodataCalonMahasiswa != null
									? (biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") + " "
											+ biodataCalonMahasiswa.getNoRegistrasi())
									: ""))
					+ "\",\"type\":\"" + type + "\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
					+ trx_amount + "\",\"billing_type\":\"" + billing_type + "\" " + description + " }";

		}

		String cid = merchant_id; // from BNI
		String key = Password; // from BNI

		System.out.println("cid => " + cid);
		System.out.println("key => " + key);

		if (cid == null || cid.trim().isEmpty() || key == null || key.trim().isEmpty()) {
			String pesan = "Konfigurasi BNI belum lengkap. Mohon isi bni_merchant_id dan bni_password terlebih dahulu.";
			if (tampilInfo) {
				MyMessageboxConfig.show(pesan, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}
			throw new IllegalArgumentException(pesan);
		}

		String parsedData = BNIHash.hashData(data, cid, key);
		String decodeData = BNIHash.parseData(parsedData, cid, key);

		System.out.println("parsedData = " + parsedData);
		System.out.println("decodeData = " + decodeData);

		String postData = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedData + "\"}";
		final BniRequest bniRequest = BniCommon.sendRequest(postData, mahasiswa, biodataCalonMahasiswa, jenisKegiatan,
				jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, amn,
				merchant_id, data, bill_no, virtual_account, key, bniRequestDetails, bniRequestDetailBiayas, true,
				tanggalExpired, cicilan);
		if (tampilInfo) {
			if (bniRequest != null && bniRequest.getVa() != null && !bniRequest.getVa().trim().isEmpty()) {

				String code = bniRequest.getVa();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + bniRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String nama = mahasiswa != null ? mahasiswa.getNama()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "");
				String myUrl = "/common/bni/no_va.zul?va=" + URLEncoder.encode(bniRequest.getVa(), "UTF-8")
						+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8") + "&nama="
						+ URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
						+ URLEncoder.encode(Common.dateFormat.get().format(bniRequest.getBillExpired()), "UTF-8")
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
				MyMessageboxConfig.show(pesanGagalDenganInfoTeknis(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}

		return bniRequest;
	}

	@SuppressWarnings({})
	public static BniRequest onSaveBni(Double amn, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars,
			List<BniRequestDetail> bniRequestDetails, List<BniRequestDetailBiaya> bniRequestDetailBiayas,
			boolean tampilInfo, Event event, String cicilan) throws Exception {

		if (amn < 0.01) {
			return null;
		}

		return onPilihBni(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester,
				tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, bniRequestDetails,
				bniRequestDetailBiayas, tampilInfo, event, cicilan);

	}

	/**
	 * Detail kegagalan request BNI kini memakai kanal BERSAMA seluruh payment gateway:
	 * {@link InfoTeknisPembayaran} (ThreadLocal baca-sekali). Method di bawah
	 * dipertahankan sebagai delegasi agar pemanggil lama tetap kompatibel.
	 */
	private static void catatInfoTeknis(String info) {
		InfoTeknisPembayaran.catat(info);
	}

	/** @deprecated pakai {@link InfoTeknisPembayaran#ambil()}. */
	public static String ambilInfoTeknisTerakhir() {
		return InfoTeknisPembayaran.ambil();
	}

	/** @deprecated pakai {@link InfoTeknisPembayaran#pesanGagal()}. */
	public static String pesanGagalDenganInfoTeknis() {
		return InfoTeknisPembayaran.pesanGagal();
	}

	private static String potongTeks(String s, int maks) {
		return InfoTeknisPembayaran.potong(s, maks);
	}

	@SuppressWarnings("deprecation")
	public static BniRequest sendRequest(String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, String merchant_id, String signature, String bill_no,
			String virtual_account, String key, List<BniRequestDetail> bniRequestDetails,
			List<BniRequestDetailBiaya> bniRequestDetailBiayas, Boolean hapusCicilanSebelumnya, Date billExpired,
			String cicilan) throws Exception {
		InfoTeknisPembayaran.bersihkan();
		Collections.sort(bniRequestDetails);

		keterangan = "";
		for (BniRequestDetail bniRequestDetail : bniRequestDetails) {
			keterangan += bniRequestDetail.toString();
		}

		Session session = HibernateUtil.currentNativeSession();
		BniRequest bniRequestData = (BniRequest) session.createCriteria(BniRequest.class)
				.add(Restrictions.ne("status", "Payment Sukses"))
				.add(Restrictions.or(Restrictions.eq("mahasiswa", mahasiswa),
						Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)))
				.add(Restrictions.gt("billExpired", ais.ui.util.WaktuUtil.getDate()))
				.add(Restrictions.eq("semester", semester)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(jenisKegiatan == null ? Restrictions.isNull("jenisKegiatan")
						: Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(Restrictions.eq("keterangan", keterangan))

				.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
//		System.out.println("bniRequestData => " + bniRequestData);
		if (bniRequestData != null) {
			HibernateUtil.closeSession();
			return bniRequestData;
		}

		postData = postData.replaceAll("&", "dan");

		// curl_init and url
		String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
		if (!ipClient.trim().isEmpty()) {
			ipClient = ipClient + "/BniForwarder";
		}
		String strURL = !ipClient.trim().isEmpty() ? ipClient
				: (Common.getKonfigurasi("bni_gateway_url", "https://apibeta.bni-ecollection.com/").getNilai());

		BniRequest bniRequest = new BniRequest(cicilan);
//		System.out.println("postData = " + postData);
//		System.out.println("postData = " + postData);

		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "application/json");
			HttpClient httpclient = new HttpClient();

			// Batas waktu koneksi & baca agar request TIDAK menggantung lama (sampai timeout OS ~menit)
			// ketika gateway BNI tidak bisa dihubungi. Bisa diatur via Konfigurasi.
			int bniConnectTimeout = 10000;
			int bniReadTimeout = 20000;
			try {
				bniConnectTimeout = Integer
						.parseInt(Common.getKonfigurasi("bni_connect_timeout_ms", "10000").getNilai().trim());
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/BniCommon.java:668");
			}
			try {
				bniReadTimeout = Integer
						.parseInt(Common.getKonfigurasi("bni_read_timeout_ms", "20000").getNilai().trim());
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/BniCommon.java:673");
			}
			httpclient.getHttpConnectionManager().getParams().setConnectionTimeout(bniConnectTimeout);
			httpclient.getHttpConnectionManager().getParams().setSoTimeout(bniReadTimeout);
			httpclient.getParams().setConnectionManagerTimeout(bniConnectTimeout);
			httpclient.getParams().setSoTimeout(bniReadTimeout);

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			System.out.println(hasil);

			JSONObject bni = new JSONObject(hasil);
			System.out.println("jSONObject = " + bni);

			String status = bni.isNull("status") ? "" : bni.getString("status");

			if (!status.trim().equals("000") && !status.trim().equals("102")) {
				// BNI menolak permintaan — catat status + pesan dari server apa adanya agar
				// pengguna/admin tahu penyebab pastinya (bukan sekadar "Transaksi Gagal").
				String pesanBni = bni.optString("message", "");
				if (pesanBni == null || pesanBni.trim().isEmpty()) pesanBni = bni.optString("description", "");
				String info = "Server BNI menolak permintaan, kode status=" + status
						+ (pesanBni == null || pesanBni.trim().isEmpty() ? "" : ", pesan=" + pesanBni.trim())
						+ ". Respons server: " + potongTeks(hasil, 300);
				catatInfoTeknis(info);
				ais.common.ErrorAuditUtil.record(new IllegalStateException(info),
						"BNI sendRequest ditolak; billNo=" + bill_no + ", va=" + virtual_account
								+ ", url=" + strURL);
				return null;
			}

			// String decodeData = "";
			JSONObject responseData = new JSONObject();
			if (status.trim().equals("000")) {
				String decodeData = null;
				try {
					String data = bni.isNull("data") ? "" : bni.getString("data");
					decodeData = BNIHash.parseData(data, merchant_id, key);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:704");

				}

				try {
					responseData = new JSONObject(decodeData);
					System.out.println("responseData = " + responseData);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:711");

				}
			} else if (status.trim().equals("102")) {
				bniRequest = (BniRequest) session.createCriteria(BniRequest.class)
						.add(Restrictions.eq("va", virtual_account)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();
				return bniRequest;
			}

			try {

				bniRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
				bniRequest.setNama(responseData.isNull("virtual_account") ? virtual_account
						: responseData.getString("virtual_account"));

				bniRequest.setTrxId(responseData.isNull("trx_id") ? virtual_account : responseData.getString("trx_id"));
				bniRequest.setVa(responseData.isNull("virtual_account") ? virtual_account
						: responseData.getString("virtual_account"));
				bniRequest.setBillNo(bill_no);
				bniRequest.setMerchant_id(merchant_id);
				bniRequest.setData(responseData.isNull("virtual_account") ? virtual_account
						: responseData.getString("virtual_account"));
				bniRequest.setMerchant(merchant_id);
				bniRequest.setResponse_code(status);
				bniRequest.setResponse_desc(BniRequestAction.statses.get(status));
				bniRequest.setMahasiswa(mahasiswa);
				bniRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				bniRequest.setJenisKegiatan(jenisKegiatan);
				bniRequest.setJadwalPembayaran(jadwalPembayaran);
				bniRequest.setSemester(semester);
				bniRequest.setTahunAkademik(tahunAkademik);
				bniRequest.setKeterangan(keterangan);
				bniRequest.setPengurangan(pengurangan);
				bniRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
				bniRequest.setAmount(amount);
				bniRequest.setResponse(bni.toString());
				bniRequest.setRequest(postData);
				bniRequest.setBillExpired(billExpired);
				bniRequest.setCicilan(cicilan);

				session.getTransaction().begin();
				session.save(bniRequest);

				for (BniRequestDetail bniRequestDetail : bniRequestDetails) {
					bniRequestDetail.setBniRequest(bniRequest);
					session.save(bniRequestDetail);
				}

				for (BniRequestDetailBiaya bniRequestDetailBiaya : bniRequestDetailBiayas) {
					bniRequestDetailBiaya.setBniRequest(bniRequest);
					session.save(bniRequestDetailBiaya);
				}

				session.getTransaction().commit();
			} catch (Exception e) {
				catatInfoTeknis("VA diterima BNI namun GAGAL disimpan di aplikasi: "
						+ e.getClass().getSimpleName() + " - " + potongTeks(e.getMessage(), 200));
				Common.tampilErrorJikaAdmin(e);
				try {
					HibernateUtil.closeSession();
					post.releaseConnection();
				} catch (Exception ee) {
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/common/BniCommon.java:772");
				}
				return null;

			}

		} catch (java.net.ConnectException ce) {
			// Gateway BNI tak bisa dihubungi (timeout/unreachable). JANGAN lempar exception agar
			// halaman pemanggil (mis. cetak kartu pendaftaran) tetap tampil; pemanggil sudah cek null.
			catatInfoTeknis("Tidak dapat terhubung ke gateway BNI (" + strURL + "): "
					+ potongTeks(ce.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/BniCommon.java:784");
			}
			return null;
		} catch (java.net.SocketTimeoutException te) {
			// Gateway BNI tidak merespons dalam batas waktu baca → sama: kembalikan null, jangan crash halaman.
			catatInfoTeknis("Gateway BNI (" + strURL + ") tidak merespons dalam batas waktu (timeout): "
					+ potongTeks(te.getMessage(), 200) + ". Coba beberapa saat lagi.");
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/BniCommon.java:792");
			}
			return null;
		} catch (org.apache.commons.httpclient.ConnectTimeoutException cte) {
			// commons-httpclient ConnectTimeoutException = subclass java.io.InterruptedIOException,
			// BUKAN java.net.ConnectException → tak tertangkap catch di atas. Gateway BNI tidak
			// menerima koneksi dalam batas waktu; kembalikan null agar halaman pemanggil
			// (mis. cetak kartu pendaftaran) tidak crash. Pemanggil sudah cek null.
			catatInfoTeknis("Koneksi ke gateway BNI (" + strURL + ") tidak tersambung dalam batas waktu: "
					+ potongTeks(cte.getMessage(), 200) + ". Periksa jaringan/firewall server.");
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/BniCommon.java:803");
			}
			return null;
		} catch (Exception e) {
			// Kegagalan request/proses lain → kembalikan null (gagal), JANGAN kembalikan
			// BniRequest yang belum tersimpan agar pemanggil menampilkan hasil yang benar.
			catatInfoTeknis("Gagal memproses request/respons BNI (" + strURL + "): "
					+ e.getClass().getSimpleName() + " - " + potongTeks(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/common/BniCommon.java:812");
			}
			return null;
		} finally {
			post.releaseConnection();
		}

		HibernateUtil.closeSession();

		return bniRequest;
	}

	@SuppressWarnings("deprecation")
	public static BniRequest onSaveBni(Siswa siswa, CalonSiswa calonSiswa, Collection<Tagihan> tag, final Double amn,
			boolean tampilkanFormPembayaran, Double deposit) throws Exception {

		if (amn == null || amn.intValue() == 0) {
			return null;
		}
		String keterangan = "";
		String cicilan = "";
		List<Tagihan> tagihans = null;
		if (tag != null) {
			tagihans = new ArrayList<Tagihan>(tag);
			Collections.sort(tagihans);

			for (Tagihan tagihan : tagihans) {
				keterangan += tagihan.getId() + "-" + tagihan.getItemBiayaSekolah().getNama()
						+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")"
								: "")
						+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
						+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()) + ", ";
				Double nilai = tagihan.getNominal() + tagihan.getDenda();
				cicilan += cicilan.isEmpty() ? ("Bulanan-" + tagihan.getId().toString() + "-" + nilai)
						: "," + ("Bulanan-" + tagihan.getId().toString() + "-" + nilai);
			}
		}

		String[] h = Sekolah.checkCidDanPassword(siswa, calonSiswa);

		String merchant_id = h[0]; // from BNI
		String Password = h[1]; // from BNI

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);

		String bill_no = Common.getGeneratedBarCode();

		String jam_terakhir_pembayaran = Common.getKonfigurasi("jam_terakhir_pembayaran", "").getNilai();
		if (jam_terakhir_pembayaran != null && !jam_terakhir_pembayaran.trim().isEmpty()) {
			try {
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.set(Calendar.HOUR_OF_DAY,
						calendar1.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(jam_terakhir_pembayaran));
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				calendar = calendar1;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BniCommon.java:873");
			}
		}

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran = Common.getKonfigurasi("tanggal_terakhir_pembayaran", "").getNilai();
		if (tanggal_terakhir_pembayaran != null && !tanggal_terakhir_pembayaran.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran);
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.setTime(tanggalExpired);
				calendar1.set(Calendar.HOUR_OF_DAY, 23);
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				tanggalExpired = calendar1.getTime();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:888");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		if (tanggalExpired == null) {
			tanggalExpired = calendar.getTime();
		}

		String virtual_account = "";

		Session session = HibernateUtil.currentNativeSession();

		boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bni");

		if (Common.bolehKonfigurasi("angka_va_bni_menggunakan_nim")) {
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bni", "8").getNilai()
					+ (tambahkanMerchanId ? merchant_id : "")
					+ (siswa != null ? siswa.getNim() : (calonSiswa != null ? calonSiswa.getNoRegistrasi() : ""));
		} else {
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_bni", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:915");

			}
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bni", "8").getNilai()
					+ (tambahkanMerchanId ? merchant_id : "") + Common.getGeneratedAngkaDigit(generatedAngkaDigit);
		}

		int generatedAngkaDigit = 16;
		try {
			generatedAngkaDigit = Integer
					.parseInt(Common.getKonfigurasi("virtual_account_angka_digit_bni", "16").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:926");

		}
		virtual_account = (virtual_account + "00000000000000000").substring(0, generatedAngkaDigit);
		// }

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:935");

		}

		String description = "";
		if (!tambahkanMerchanId) {
			description = ",\"description\":\"" + keterangan + "\"";
		}

		String type = Common.getKonfigurasi("type_transaksi_bni", "createbilling").getNilai();
		String billing_type = Common.getKonfigurasi("billing_type_bni", "c").getNilai();

		// BNIHash hash = new BNIHash();
		String data = "{\"customer_email\":\""
				+ (siswa != null ? siswa.getAlamatEmail().split(",")[0].trim()
						: (calonSiswa == null ? "test@email.com" : calonSiswa.getAlamatEmail().split(",")[0].trim()))
				+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired + "\",\"client_id\":\""
				+ merchant_id + "\",\"customer_phone\":\""
				+ (siswa != null ? siswa.getTeleponSiswa()
						: (calonSiswa != null ? calonSiswa.getTeleponOrangTua() : ""))
				+ "\",\"customer_name\":\""
				+ (siswa != null ? siswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "")
						: (calonSiswa != null ? calonSiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") : ""))
				+ "\",\"type\":\"" + type + "\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
				+ (biayaAdministrasi.intValue() + amn.intValue()) + "\",\"billing_type\":\"" + billing_type + "\"  "
				+ description + " }";

		String cid = merchant_id; // from BNI
		String key = Password; // from BNI

		String parsedData = BNIHash.hashData(data, cid, key);
		String decodeData = BNIHash.parseData(parsedData, cid, key);

		System.out.println("parsedData = " + parsedData);
		System.out.println("decodeData = " + decodeData);

		String postData = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedData + "\"}";

		BniRequest bniRequest = new BniRequest(cicilan);

		try {

//			System.out.println("postData -> " + postData);

			// curl_init and url
			String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
			if (!ipClient.trim().isEmpty()) {
				ipClient = ipClient + "/BniForwarder";
			}
			String strURL = siswa != null && siswa.getSekolah() != null
					&& !siswa.getSekolah().getBniGatewayUrl().isEmpty()
							? siswa.getSekolah().getBniGatewayUrl()
							: !ipClient.trim().isEmpty() ? ipClient
									: (Common.getKonfigurasi("bni_gateway_url", "https://apibeta.bni-ecollection.com/")
											.getNilai());

			System.out.println("url = " + strURL);
			System.out.println("postData = " + postData);

			PostMethod post = new PostMethod(strURL);
			try {
				StringRequestEntity requestEntity = new StringRequestEntity(postData);
				post.setRequestEntity(requestEntity);
				post.setRequestHeader("Content-type", "application/json");
				HttpClient httpclient = new HttpClient();

				int result = httpclient.executeMethod(post);
				System.out.println("Response status code: " + result);
				System.out.println("Response body: ");

				String hasil = post.getResponseBodyAsString();

				System.out.println(hasil);

				JSONObject bni = new JSONObject(hasil);
				System.out.println("jSONObject = " + bni);

				String status = bni.isNull("status") ? "" : bni.getString("status");

				if (status.trim().equals("102")) {
					session = HibernateUtil.currentNativeSession();
					BniRequest bniRequesttemp = (BniRequest) session.createCriteria(BniRequest.class)
							.addOrder(Order.desc("id")).add(Restrictions.eq("va", virtual_account)).setMaxResults(1)
							.uniqueResult();
					HibernateUtil.closeSession();
					if (bniRequesttemp != null) {
						return bniRequesttemp;
					}
					// VA settled di BNI tapi tidak ada catatan di DB.
					// BNI mengizinkan pembuatan VA ulang dengan nomor yang sama menggunakan Trx_ID berbeda.
					// Coba sekali lagi dengan bill_no baru.
					try {
						bill_no = Common.getGeneratedBarCode();
						String dataRetry = "{\"customer_email\":\""
								+ (siswa != null ? siswa.getAlamatEmail().split(",")[0].trim()
										: (calonSiswa == null ? "test@email.com"
												: calonSiswa.getAlamatEmail().split(",")[0].trim()))
								+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired
								+ "\",\"client_id\":\"" + merchant_id + "\",\"customer_phone\":\""
								+ (siswa != null ? siswa.getTeleponSiswa()
										: (calonSiswa != null ? calonSiswa.getTeleponOrangTua() : ""))
								+ "\",\"customer_name\":\""
								+ (siswa != null ? siswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "")
										: (calonSiswa != null ? calonSiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") : ""))
								+ "\",\"type\":\"" + type + "\",\"virtual_account\":\"" + virtual_account
								+ "\",\"trx_amount\":\"" + (biayaAdministrasi.intValue() + amn.intValue())
								+ "\",\"billing_type\":\"" + billing_type + "\"  " + description + " }";
						String parsedRetry = BNIHash.hashData(dataRetry, cid, key);
						String postRetry = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedRetry + "\"}";
						System.out.println("BNI 102 retry bill_no=" + bill_no + " postRetry=" + postRetry);
						PostMethod postMethodRetry = new PostMethod(strURL);
						StringRequestEntity retryEntity = new StringRequestEntity(postRetry);
						postMethodRetry.setRequestEntity(retryEntity);
						postMethodRetry.setRequestHeader("Content-type", "application/json");
						HttpClient retryClient = new HttpClient();
						retryClient.executeMethod(postMethodRetry);
						String hasilRetry = postMethodRetry.getResponseBodyAsString();
						System.out.println("BNI retry response: " + hasilRetry);
						JSONObject bniRetry = new JSONObject(hasilRetry);
						String statusRetry = bniRetry.isNull("status") ? "" : bniRetry.getString("status");
						if (statusRetry.trim().equals("000")) {
							String dataRetryDecoded = bniRetry.isNull("data") ? "" : bniRetry.getString("data");
							String decodeRetry = BNIHash.parseData(dataRetryDecoded, merchant_id, key);
							JSONObject responseRetry = new JSONObject(decodeRetry);
							String vaRetry = responseRetry.isNull("virtual_account") ? virtual_account
									: responseRetry.getString("virtual_account");
							String trxRetry = responseRetry.isNull("trx_id") ? bill_no
									: responseRetry.getString("trx_id");
							bniRequest.setHapusCicilanSebelumnya(true);
							bniRequest.setNama(vaRetry);
							bniRequest.setTrxId(trxRetry);
							bniRequest.setVa(vaRetry);
							bniRequest.setBillNo(bill_no);
							bniRequest.setMerchant_id(merchant_id);
							bniRequest.setData(vaRetry);
							bniRequest.setMerchant(merchant_id);
							bniRequest.setResponse_code(statusRetry);
							bniRequest.setResponse_desc(BniRequestAction.statses.get(statusRetry));
							bniRequest.setSiswa(siswa);
							bniRequest.setCalonSiswa(calonSiswa);
							bniRequest.setNilaiBiayaHarusDiBayars(amn);
							bniRequest.setAmount(amn);
							bniRequest.setResponse(hasilRetry);
							bniRequest.setRequest(postRetry);
							bniRequest.setBillExpired(tanggalExpired);
							bniRequest.setKeterangan(keterangan);
							bniRequest.setDeposit(deposit);
							session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							session.saveOrUpdate(bniRequest);
							session.getTransaction().commit();
							session.createSQLQuery(
									"delete from bni_request_detail where bni_request=" + bniRequest.getId())
									.executeUpdate();
							if (tagihans != null) {
								for (Tagihan tagihan : tagihans) {
									BniRequestDetail bniRequestDetail = new BniRequestDetail();
									bniRequestDetail.setBniRequest(bniRequest);
									bniRequestDetail.setTagihan(tagihan);
									bniRequestDetail.setNilai(tagihan.getNominal() + tagihan.getDenda());
									bniRequestDetail.setKeterangan(tagihan.getItemBiayaSekolah().getNama()
											+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
													? " (ke " + tagihan.getBayarKe() + ")" : "")
											+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
											+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()));
									session.getTransaction().begin();
									session.save(bniRequestDetail);
									session.getTransaction().commit();
									tagihan.setVa(vaRetry);
									tagihan.setExpired(tanggalExpired);
									session.getTransaction().begin();
									session.update(tagihan);
									session.getTransaction().commit();
								}
							}
							HibernateUtil.closeSession();
							return bniRequest;
						}
					} catch (Exception retryEx) {
						System.out.println("BNI retry error: " + retryEx.getMessage());
						Common.tampilErrorJikaAdmin(retryEx);
					}
					return null;
				}

				if (!status.trim().equals("000")) {
					return null;
				}

				data = bni.isNull("data") ? "" : bni.getString("data");

				// String decodeData = "";
				decodeData = BNIHash.parseData(data, merchant_id, key);

				JSONObject responseData = new JSONObject(decodeData);
				System.out.println("responseData = " + responseData);

				bniRequest.setHapusCicilanSebelumnya(true);
				bniRequest.setNama(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));

				bniRequest.setHapusCicilanSebelumnya(true);
				bniRequest.setNama(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));

				bniRequest.setTrxId(responseData.isNull("trx_id") ? "" : responseData.getString("trx_id"));
				bniRequest
						.setVa(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bniRequest.setBillNo(bill_no);
				bniRequest.setMerchant_id(merchant_id);
				bniRequest.setData(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bniRequest.setMerchant(merchant_id);
				bniRequest.setResponse_code(status);
				bniRequest.setResponse_desc(BniRequestAction.statses.get(status));

				bniRequest.setSiswa(siswa);
				bniRequest.setCalonSiswa(calonSiswa);
				bniRequest.setNilaiBiayaHarusDiBayars(amn);
				bniRequest.setAmount(amn);
				bniRequest.setResponse(hasil);
				bniRequest.setRequest(postData);
				bniRequest.setBillExpired(tanggalExpired);

				bniRequest.setKeterangan(keterangan);

				bniRequest.setDeposit(deposit);

				session.getTransaction().begin();
				session.saveOrUpdate(bniRequest);
				session.getTransaction().commit();

				session.createSQLQuery("delete from bni_request_detail where bni_request=" + bniRequest.getId())
						.executeUpdate();
				if (tagihans != null) {
					for (Tagihan tagihan : tagihans) {
						BniRequestDetail bniRequestDetail = new BniRequestDetail();
						bniRequestDetail.setBniRequest(bniRequest);
						bniRequestDetail.setTagihan(tagihan);
						bniRequestDetail.setNilai(tagihan.getNominal() + tagihan.getDenda());
						bniRequestDetail.setKeterangan(tagihan.getItemBiayaSekolah().getNama()
								+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
										? " (ke " + tagihan.getBayarKe() + ")"
										: "")
								+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
								+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()));
						session.getTransaction().begin();
						session.save(bniRequestDetail);
						session.getTransaction().commit();

						tagihan.setVa(virtual_account);
						tagihan.setExpired(tanggalExpired);

						session.getTransaction().begin();
						session.update(tagihan);
						session.getTransaction().commit();
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				System.out.println("Error = " + e.getMessage());
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();
		if (tampilkanFormPembayaran) {
			if (bniRequest != null && bniRequest.getVa() != null && !bniRequest.getVa().trim().isEmpty()) {

				String code = bniRequest.getVa();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + bniRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String nama = siswa != null ? siswa.getNama() : (calonSiswa != null ? calonSiswa.getNama() : "");
				String myUrl = "/common/bni/no_va.zul?va=" + URLEncoder.encode(bniRequest.getVa(), "UTF-8")
						+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8") + "&nama="
						+ URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
						+ URLEncoder.encode(Common.dateFormat.get().format(bniRequest.getBillExpired()), "UTF-8")
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
				MyMessageboxConfig.show(pesanGagalDenganInfoTeknis(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}
		return bniRequest;
	}

	@SuppressWarnings("deprecation")
	public static BniRequest onSaveBni(List<PesertaPunyaProdukKursus> punyaProdukKursus, final Double amn,
			boolean tampilkanFormPembayaran) throws Exception {

		if (amn == null || amn.intValue() == 0 || punyaProdukKursus.isEmpty()) {
			return null;
		}

		PesertaKursus pesertaKursus = punyaProdukKursus.get(0).getPesertaKursus();
		String cicilan = "";
		String keterangan = "";
		for (PesertaPunyaProdukKursus pesertaPunyaProdukKursus : punyaProdukKursus) {
			keterangan += pesertaPunyaProdukKursus.getId() + "-" + pesertaPunyaProdukKursus.getProdukKursus().getNama()
					+ Common.numberFormat.get().format(pesertaPunyaProdukKursus.getProdukKursus().getHargaTotal()) + ", ";

			Double nilai = pesertaPunyaProdukKursus.getProdukKursus().getHargaTotal();
			cicilan += cicilan.isEmpty() ? ("Bulanan-" + pesertaPunyaProdukKursus.getId().toString() + "-" + nilai)
					: "," + ("Bulanan-" + pesertaPunyaProdukKursus.getId().toString() + "-" + nilai);
		}

		String merchant_id = Common.getKonfigurasi("bni_merchant_id", "000").getNilai().trim();
		String Password = Common.getKonfigurasi("bni_password", "685dedd9f045787873794ead6276f8bf").getNilai().trim();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);

		String bill_no = Common.getGeneratedBarCode();

		String jam_terakhir_pembayaran = Common.getKonfigurasi("jam_terakhir_pembayaran", "").getNilai();
		if (jam_terakhir_pembayaran != null && !jam_terakhir_pembayaran.trim().isEmpty()) {
			try {
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.set(Calendar.HOUR_OF_DAY,
						calendar1.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(jam_terakhir_pembayaran));
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				calendar = calendar1;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BniCommon.java:1279");
			}
		}

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran = Common.getKonfigurasi("tanggal_terakhir_pembayaran", "").getNilai();
		if (tanggal_terakhir_pembayaran != null && !tanggal_terakhir_pembayaran.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran);
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.setTime(tanggalExpired);
				calendar1.set(Calendar.HOUR_OF_DAY, 23);
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				tanggalExpired = calendar1.getTime();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:1294");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		String virtual_account = "";

		Session session = HibernateUtil.currentNativeSession();

		boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bni");

		{
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_bni", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:1313");

			}
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bni", "8").getNilai()
					+ (tambahkanMerchanId ? merchant_id : "") + Common.getGeneratedAngkaDigit(generatedAngkaDigit);
		}

		int generatedAngkaDigit = 16;
		try {
			generatedAngkaDigit = Integer
					.parseInt(Common.getKonfigurasi("virtual_account_angka_digit_bni", "16").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:1324");

		}
		virtual_account = (virtual_account + "00000000000000000").substring(0, generatedAngkaDigit);
		// }

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BniCommon.java:1333");

		}

		String description = "";
		if (!tambahkanMerchanId) {
			description = ",\"description\":\"" + keterangan + "\"";
		}

		String type = Common.getKonfigurasi("type_transaksi_bni", "createbilling").getNilai();
		String billing_type = Common.getKonfigurasi("billing_type_bni", "c").getNilai();

		// BNIHash hash = new BNIHash();
		String data = "{\"customer_email\":\"" + (pesertaKursus.getEmail()) + "\",\"trx_id\":\"" + bill_no
				+ "\",\"datetime_expired\":\"" + datetime_expired + "\",\"client_id\":\"" + merchant_id
				+ "\",\"customer_phone\":\"" + (pesertaKursus.getTelp()) + "\",\"customer_name\":\""
				+ (pesertaKursus.getNama().replaceAll("[^\\sa-zA-Z0-9]", "")) + "\",\"type\":\"" + type
				+ "\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
				+ (biayaAdministrasi.intValue() + amn.intValue()) + "\",\"billing_type\":\"" + billing_type + "\"  "
				+ description + " }";

		String cid = merchant_id; // from BNI
		String key = Password; // from BNI

		String parsedData = BNIHash.hashData(data, cid, key);
		String decodeData = BNIHash.parseData(parsedData, cid, key);

		System.out.println("parsedData = " + parsedData);
		System.out.println("decodeData = " + decodeData);

		String postData = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedData + "\"}";

		BniRequest bniRequest = new BniRequest(cicilan);

		try {

//			System.out.println("postData -> " + postData);

			// curl_init and url
			String ipClient = (Common.getKonfigurasi("bni_ip_client", "").getNilai());
			if (!ipClient.trim().isEmpty()) {
				ipClient = ipClient + "/BniForwarder";
			}
			String strURL = !ipClient.trim().isEmpty() ? ipClient
					: (Common.getKonfigurasi("bni_gateway_url", "https://apibeta.bni-ecollection.com/").getNilai());

			System.out.println("url = " + strURL);
			System.out.println("postData = " + postData);

			PostMethod post = new PostMethod(strURL);
			try {
				StringRequestEntity requestEntity = new StringRequestEntity(postData);
				post.setRequestEntity(requestEntity);
				post.setRequestHeader("Content-type", "application/json");
				HttpClient httpclient = new HttpClient();

				int result = httpclient.executeMethod(post);
				System.out.println("Response status code: " + result);
				System.out.println("Response body: ");

				String hasil = post.getResponseBodyAsString();

				System.out.println(hasil);

				JSONObject bni = new JSONObject(hasil);
				System.out.println("jSONObject = " + bni);

				String status = bni.isNull("status") ? "" : bni.getString("status");

				if (status.trim().equals("102")) {
					session = HibernateUtil.currentNativeSession();
					BniRequest bniRequesttemp = (BniRequest) session.createCriteria(BniRequest.class)
							.addOrder(Order.desc("id")).add(Restrictions.eq("va", virtual_account)).setMaxResults(1)
							.uniqueResult();
					HibernateUtil.closeSession();
					return bniRequesttemp;
				}

				if (!status.trim().equals("000")) {
					return null;
				}

				data = bni.isNull("data") ? "" : bni.getString("data");

				// String decodeData = "";
				decodeData = BNIHash.parseData(data, merchant_id, key);

				JSONObject responseData = new JSONObject(decodeData);
				System.out.println("responseData = " + responseData);

				bniRequest.setHapusCicilanSebelumnya(true);
				bniRequest.setNama(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));

				bniRequest.setHapusCicilanSebelumnya(true);
				bniRequest.setNama(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));

				bniRequest.setTrxId(responseData.isNull("trx_id") ? "" : responseData.getString("trx_id"));
				bniRequest
						.setVa(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bniRequest.setBillNo(bill_no);
				bniRequest.setMerchant_id(merchant_id);
				bniRequest.setData(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bniRequest.setMerchant(merchant_id);
				bniRequest.setResponse_code(status);
				bniRequest.setResponse_desc(BniRequestAction.statses.get(status));

				bniRequest.setPesertaKursus(pesertaKursus);
				bniRequest.setNilaiBiayaHarusDiBayars(amn);
				bniRequest.setAmount(amn);
				bniRequest.setResponse(hasil);
				bniRequest.setRequest(postData);
				bniRequest.setBillExpired(tanggalExpired);

				bniRequest.setKeterangan(keterangan);

				session.getTransaction().begin();
				session.saveOrUpdate(bniRequest);
				session.getTransaction().commit();

				session.createSQLQuery("delete from bni_request_detail where bni_request=" + bniRequest.getId())
						.executeUpdate();
				for (PesertaPunyaProdukKursus pesertaPunyaProdukKursus : punyaProdukKursus) {
					BniRequestDetail bniRequestDetail = new BniRequestDetail();
					bniRequestDetail.setBniRequest(bniRequest);
					bniRequestDetail.setPesertaPunyaProdukKursus(pesertaPunyaProdukKursus);
					bniRequestDetail.setNilai(pesertaPunyaProdukKursus.getProdukKursus().getHargaTotal());

					String s = pesertaPunyaProdukKursus.getId() + "-"
							+ pesertaPunyaProdukKursus.getProdukKursus().getNama()
							+ Common.numberFormat.get().format(pesertaPunyaProdukKursus.getProdukKursus().getHargaTotal());

					bniRequestDetail.setKeterangan(s);
					session.getTransaction().begin();
					session.save(bniRequestDetail);
					session.getTransaction().commit();
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				System.out.println("Error = " + e.getMessage());
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();
		if (tampilkanFormPembayaran) {
			if (bniRequest != null && bniRequest.getVa() != null && !bniRequest.getVa().trim().isEmpty()) {

				String code = bniRequest.getVa();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + bniRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String nama = pesertaKursus.getNama();
				String myUrl = "/common/bni/no_va.zul?va=" + URLEncoder.encode(bniRequest.getVa(), "UTF-8")
						+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8") + "&nama="
						+ URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
						+ URLEncoder.encode(Common.dateFormat.get().format(bniRequest.getBillExpired()), "UTF-8")
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
				MyMessageboxConfig.show(pesanGagalDenganInfoTeknis(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}
		return bniRequest;
	}
}
