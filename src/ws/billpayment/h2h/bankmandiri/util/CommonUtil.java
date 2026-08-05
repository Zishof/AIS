/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ws.billpayment.h2h.bankmandiri.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DendaPembayaran;
import ais.database.model.DendaPembayaranNominal;
import ais.database.model.DetailBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ws.billpayment.h2h.bankmandiri.BillDetail;
import ws.billpayment.h2h.bankmandiri.InquiryResponse;
import ws.billpayment.h2h.bankmandiri.PaymentResponse;

/**
 * 
 * @author Fauzi
 */
public class CommonUtil extends ais.action.ws.util.CommonUtil {

//	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public static Collection<BillDetail> generateBillDetails(
			DendaPembayaran dendaPembayaran,
			DendaPembayaranNominal dendaPembayaranNominal,
			Collection<DetailBiaya> detailBiaya) {
		List<BillDetail> billDetails = new ArrayList<BillDetail>();
		
		for (DetailBiaya biaya : detailBiaya) {
			BillDetail billDetail = new BillDetail();
			billDetail.setBillAmount(biaya.getNilaiBiaya().intValue() + "");
			billDetail.setBillCode(biaya.getItemBiaya().getKode() == null ? ""
					: biaya.getItemBiaya().getKode() + "");
			billDetail.setBillName(biaya.getItemBiaya().getDeskripsi() + "");
			billDetail.setBillShortName(biaya.getItemBiaya().getNama());
			billDetails.add(billDetail);
		}
		Collections.sort(billDetails);
		return billDetails;
	}

	public static Collection<BillDetail> generateBillDetailsCalonMahasiswa(
			DendaPembayaran dendaPembayaran,
			DendaPembayaranNominal dendaPembayaranNominal,
			Collection<DetailBiaya> detailBiaya) {
		List<BillDetail> billDetails = new ArrayList<BillDetail>();
		
		for (DetailBiaya biaya : detailBiaya) {
			BillDetail billDetail = new BillDetail();
			billDetail.setBillAmount(biaya.getNilaiBiaya().intValue() + "");
			billDetail.setBillCode(biaya.getItemBiaya().getKode() == null ? ""
					: biaya.getItemBiaya().getKode() + "-"
							+ biaya.getJurusan().getKode());
			billDetail.setBillName(biaya.getItemBiaya().getDeskripsi() + "-"
					+ biaya.getJurusan().getNama());
			billDetail.setBillShortName(biaya.getItemBiaya().getNama() + "-"
					+ biaya.getJurusan().getNama());
			billDetails.add(billDetail);
		}
		Collections.sort(billDetails);
		return billDetails;
	}

	public static Collection<BillDetail> generateBillDetailsCalonMahasiswaTanpaProdi(
			DendaPembayaran dendaPembayaran,
			DendaPembayaranNominal dendaPembayaranNominal,
			Collection<DetailBiaya> detailBiaya) {
		List<BillDetail> billDetails = new ArrayList<BillDetail>();
		
		for (DetailBiaya biaya : detailBiaya) {
			BillDetail billDetail = new BillDetail();
			billDetail.setBillAmount(biaya.getNilaiBiaya().intValue() + "");
			billDetail.setBillCode(biaya.getItemBiaya().getKode() == null ? ""
					: biaya.getItemBiaya().getKode());
			billDetail.setBillName(biaya.getItemBiaya().getDeskripsi());
			billDetail.setBillShortName(biaya.getItemBiaya().getNama());
			billDetails.add(billDetail);
		}
		Collections.sort(billDetails);
		return billDetails;
	}

	public static InquiryResponse constructInquiryResponse(Mahasiswa mahasiswa,
			java.util.Collection<BillDetail> billDetails, Boolean ganjil,
			Integer semester, Kegiatan kegiatan) {
		InquiryResponse inquiryResponse = new InquiryResponse();
		inquiryResponse
				.setBillDetails(billDetails.toArray(new BillDetail[] {}));

		inquiryResponse.setBillInfo1(mahasiswa.getNim());
		inquiryResponse.setBillInfo2(mahasiswa.getNama());
		inquiryResponse
				.setBillInfo3(ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA);
		inquiryResponse.setBillInfo4(mahasiswa.getJurusan() == null ? ""
				: mahasiswa.getJurusan().getNama() + "/"
						+ mahasiswa.getJurusan().getFakultas().getNama());
		inquiryResponse.setBillInfo5((ganjil ? Perkuliahan.GANJIL
				: Perkuliahan.GENAP) + " - " + mahasiswa.getTahunangkatan());
		inquiryResponse.setBillInfo6("-");
		inquiryResponse.setBillInfo7(mahasiswa.getProgram());

		inquiryResponse
				.setBillInfo8(ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG);
		inquiryResponse.setBillInfo9(semester + "");
		inquiryResponse.setBillInfo10(kegiatan == null ? "0.0" : kegiatan
				.getJumlahTelahDibayar().toString());

		return inquiryResponse;
	}

	public static InquiryResponse constructInquiryResponseCalonMahasiswa(
			BiodataCalonMahasiswa biodataCalonMahasiswa,
			java.util.Collection<BillDetail> billDetails) {
		InquiryResponse inquiryResponse = new InquiryResponse();
		inquiryResponse
				.setBillDetails(billDetails.toArray(new BillDetail[] {}));

		inquiryResponse.setBillInfo1(biodataCalonMahasiswa.getNoRegistrasi());
		inquiryResponse.setBillInfo2(biodataCalonMahasiswa.getNama());
		inquiryResponse
				.setBillInfo3(ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA
						+ "-"
						+ (biodataCalonMahasiswa.getJenisSeleksi() == null ? ""
								: biodataCalonMahasiswa.getJenisSeleksi()
										.getNama()));

		String jurusan1 = biodataCalonMahasiswa.getProdi1() == null ? ""
				: biodataCalonMahasiswa.getProdi1().getNama();
		String jurusan2 = biodataCalonMahasiswa.getProdi2() == null ? ""
				: biodataCalonMahasiswa.getProdi2().getNama();

		inquiryResponse.setBillInfo4(jurusan1
				+ (!jurusan2.equals("") ? (!jurusan1.equals("") ? " dan " : "")
						+ jurusan2 : ""));
		inquiryResponse.setBillInfo5(Perkuliahan.GANJIL + " - "
				+ biodataCalonMahasiswa.getTahun() + "");
		inquiryResponse.setBillInfo6("-");
		inquiryResponse
				.setBillInfo7(biodataCalonMahasiswa.getProgram() == null ? ""
						: "");

		inquiryResponse
				.setBillInfo8(ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA);
		return inquiryResponse;
	}

	public static InquiryResponse constructInquiryResponseMahasiswaBaru(
			BiodataCalonMahasiswa biodataCalonMahasiswa,
			java.util.Collection<BillDetail> billDetails) {
		InquiryResponse inquiryResponse = new InquiryResponse();
		inquiryResponse
				.setBillDetails(billDetails.toArray(new BillDetail[] {}));

		inquiryResponse
				.setBillInfo1(biodataCalonMahasiswa.getNim() == null ? "" : "");
		inquiryResponse.setBillInfo2(biodataCalonMahasiswa.getNama());
		inquiryResponse
				.setBillInfo3(ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU);

		inquiryResponse
				.setBillInfo4(biodataCalonMahasiswa.getProdiLulus() == null ? ""
						: biodataCalonMahasiswa.getProdiLulus().getNama()
								+ "/"
								+ biodataCalonMahasiswa.getProdiLulus()
										.getFakultas().getNama());
		inquiryResponse.setBillInfo5(Perkuliahan.GANJIL + " - "
				+ biodataCalonMahasiswa.getTahun() + "");
		inquiryResponse.setBillInfo6("-");
		inquiryResponse
				.setBillInfo7(biodataCalonMahasiswa.getProgram() == null ? ""
						: "");

		inquiryResponse
				.setBillInfo8(ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU);
		return inquiryResponse;
	}

	// ================================== PAYMENT
	// ================================================

	public static PaymentResponse constructPaymentResponseMahasiswaLama(
			Mahasiswa mahasiswa, Boolean ganjil, Integer semester,
			Kegiatan kegiatan) {
		PaymentResponse paymentResponse = new PaymentResponse();

		paymentResponse.setBillInfo1(mahasiswa.getNim());
		paymentResponse.setBillInfo2(mahasiswa.getNama());
		paymentResponse
				.setBillInfo3(ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA);
		paymentResponse.setBillInfo4(mahasiswa.getJurusan() == null ? ""
				: mahasiswa.getJurusan().getNama() + "/"
						+ mahasiswa.getJurusan().getFakultas().getNama());
		paymentResponse.setBillInfo5((ganjil ? Perkuliahan.GANJIL
				: Perkuliahan.GENAP)
				+ " - "
				+ mahasiswa.getTahunangkatan()
				+ "");
		paymentResponse.setBillInfo6("-");
		paymentResponse.setBillInfo7(mahasiswa.getProgram());

		paymentResponse
				.setBillInfo8(ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG);
		paymentResponse.setBillInfo9(semester + "");
		paymentResponse.setBillInfo10(kegiatan == null ? "0.0" : kegiatan
				.getJumlahTelahDibayar().toString());
		return paymentResponse;
	}

	public static PaymentResponse constructPaymentResponseCalonMahasiswa(
			BiodataCalonMahasiswa biodatacalonmahasiswa, Boolean ganjil) {
		PaymentResponse paymentResponse = new PaymentResponse();

		paymentResponse.setBillInfo1(biodatacalonmahasiswa.getNoRegistrasi());
		paymentResponse.setBillInfo2(biodatacalonmahasiswa.getNama());
		paymentResponse
				.setBillInfo3(ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA
						+ "-"
						+ (biodatacalonmahasiswa.getJenisSeleksi() == null ? ""
								: biodatacalonmahasiswa.getJenisSeleksi()
										.getNama()));

		String myJurusan = "";
		if (biodatacalonmahasiswa.getProdi1() != null) {
			myJurusan += biodatacalonmahasiswa.getProdi1().getNama();
		}
		if (biodatacalonmahasiswa.getProdi2() != null) {
			myJurusan += !myJurusan.equals("") ? " dan "
					+ biodatacalonmahasiswa.getProdi2().getNama()
					: biodatacalonmahasiswa.getProdi2().getNama();
		}

		paymentResponse.setBillInfo4(myJurusan);
		paymentResponse.setBillInfo5((ganjil ? Perkuliahan.GANJIL
				: Perkuliahan.GENAP)
				+ " - "
				+ biodatacalonmahasiswa.getTahun()
				+ "");
		paymentResponse.setBillInfo6("-");
		paymentResponse.setBillInfo7(biodatacalonmahasiswa.getProgram());

		paymentResponse
				.setBillInfo8(ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA);
		paymentResponse.setBillInfo9(1 + "");
		return paymentResponse;
	}
}
