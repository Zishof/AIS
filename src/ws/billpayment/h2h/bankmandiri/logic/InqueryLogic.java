package ws.billpayment.h2h.bankmandiri.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DendaPembayaran;
import ais.database.model.DendaPembayaranNominal;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.TunggakanMahasiswa;
import ws.billpayment.h2h.bankmandiri.BillDetail;
import ws.billpayment.h2h.bankmandiri.InquiryResponse;
import ws.billpayment.h2h.bankmandiri.util.CommonUtil;
import ws.billpayment.h2h.bankmandiri.util.ConstantUtilBankMandiri;
import ws.billpayment.h2h.bankmandiri.util.DisplayUtil;

public class InqueryLogic {

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	public DisplayUtil displayUtil = new DisplayUtil();

	/**
	 * untuk inquery calon mahasiswa
	 * 
	 * @param nim
	 * @param nama
	 * @return
	 */
	public InquiryResponse inqueryCalonMahasiswa(String nim, String nama, LogHostToHost logHostToHost) {

		BiodataCalonMahasiswa biodataCalonMahasiswa = pembayaranUtil.getCalonMahasiswaByNoPendaftaran(nim);
		System.out.println(biodataCalonMahasiswa);
		if (biodataCalonMahasiswa == null) {
			InquiryResponse inquiryResponse = new InquiryResponse();
			inquiryResponse.setBillInfo1(nim);
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNoRegistrasiNotFound(logHostToHost, nim, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.INQUERY);
			return inquiryResponse;
		}

		List<String[]> data = new ArrayList<String[]>();

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA);
		Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(jenisKegiatan);

		Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
				ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
				biodataCalonMahasiswa.getTahunAkademik(),
				biodataCalonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL),
				biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
				biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());

		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
		DendaPembayaran dendaPembayaran = (DendaPembayaran) serializables[1];
		DendaPembayaranNominal dendaPembayaranNominal = (DendaPembayaranNominal) serializables[2];

		if (jadwalPembayaran == null) {
			displayUtil.displayPembayaranTerlambat(logHostToHost, biodataCalonMahasiswa.getNoRegistrasi(),
					pembayaranUtil.getBankHost(), nama, ConstantUtil.INQUERY);
			InquiryResponse inquiryResponse = new InquiryResponse();
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return inquiryResponse;

		}

		Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
				: biodataCalonMahasiswa.getProdi1();
		java.util.Collection<DetailBiaya> detailBiayas = pembayaranUtil
				.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, false);

		java.util.Collection<BillDetail> billDetails;
		if (myjurusan1 != null /* || myjurusan2 != null */) {
			billDetails = CommonUtil.generateBillDetailsCalonMahasiswa(dendaPembayaran, dendaPembayaranNominal,
					detailBiayas);
		} else {
			billDetails = CommonUtil.generateBillDetailsCalonMahasiswaTanpaProdi(dendaPembayaran,
					dendaPembayaranNominal, detailBiayas);
		}

		String pemb = "|";
		Long total = 0L;
		for (BillDetail biaya : billDetails) {
			total += Long.parseLong(biaya.getBillAmount());
			pemb += biaya.getBillCode() + "\\" + biaya.getBillName() + "\\" + biaya.getBillShortName() + "\\"
					+ biaya.getBillAmount() + "|";

		}

		InquiryResponse inquiryResponse = CommonUtil.constructInquiryResponseCalonMahasiswa(biodataCalonMahasiswa,
				billDetails);

		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost, biodataCalonMahasiswa.getNoRegistrasi(), bankHost, nama,
					ConstantUtil.INQUERY);
			inquiryResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if (kegiatan == null || kegiatan.getId() == null) {
			inquiryResponse.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
					: ConstantUtilBankMandiri.SUCCESS_MANDIRI);
			data.add(new String[] { "response_code",
					total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT : ConstantUtilBankMandiri.SUCCESS });
			data.add(new String[] { "response_description",
					total.equals(0L) ? "Inquiry gagal dilakukan" : "Inquiry Sukses Dilakukan" });
			data.add(new String[] { "no_registrasi", biodataCalonMahasiswa.getNoRegistrasi() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
			data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });
			String myJurusan = "";
			String myfakultas = "";
			if (biodataCalonMahasiswa.getProdi1() != null) {
				myJurusan += biodataCalonMahasiswa.getProdi1().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi1().getFakultas().getNama();
			}
			if (biodataCalonMahasiswa.getProdi2() != null) {
				myJurusan += !myJurusan.equals("") ? " dan " + biodataCalonMahasiswa.getProdi2().getNama()
						: biodataCalonMahasiswa.getProdi2().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi2().getFakultas().getNama();
			}
			data.add(new String[] { "fakultas", myfakultas });
			data.add(new String[] { "prodi", myJurusan });
			data.add(new String[] { "angkatan", biodataCalonMahasiswa.getTahun() + "" });

			data.add(new String[] { "semester", Perkuliahan.GANJIL });
			data.add(new String[] { "semester_ke", "0" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran",
					ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

			data.add(new String[] { "jumlah_yang_telah_dibayar",
					kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

			logHostToHost.setResponseCode((ConstantUtilBankMandiri.SUCCESS));
			logHostToHost.setResponseDescription("Inquiry Sukses Dilakukan");
		} else {
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_SUDAH_DIBAYAR);
			inquiryResponse
					.setBillInfo6(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber());
			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID });
			data.add(new String[] { "response_description",
					"Inquiry gagal dilakukan, karena calon mahasiswa sudah melakukan pembayaran" });
			data.add(new String[] { "no_registrasi", biodataCalonMahasiswa.getNoRegistrasi() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
			data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });
			String myJurusan = "";
			String myfakultas = "";
			if (biodataCalonMahasiswa.getProdi1() != null) {
				myJurusan += biodataCalonMahasiswa.getProdi1().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi1().getFakultas().getNama();
			}
			if (biodataCalonMahasiswa.getProdi2() != null) {
				myJurusan += !myJurusan.equals("") ? " dan " + biodataCalonMahasiswa.getProdi2().getNama()
						: biodataCalonMahasiswa.getProdi2().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi2().getFakultas().getNama();
			}
			data.add(new String[] { "fakultas", myfakultas });
			data.add(new String[] { "prodi", myJurusan });
			data.add(new String[] { "angkatan", biodataCalonMahasiswa.getTahun() + "" });

			data.add(new String[] { "semester", Perkuliahan.GANJIL });
			data.add(new String[] { "semester_ke", "0" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran",
					ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

			data.add(new String[] { "jumlah_yang_telah_dibayar",
					kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

			logHostToHost.setResponseCode((ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID));
			logHostToHost.setResponseDescription(
					"Inquiry gagal dilakukan, karena calon mahasiswa sudah melakukan pembayaran");
		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(biodataCalonMahasiswa.getNoRegistrasi());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.INQUERY);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return inquiryResponse;
	}

	/**
	 * Untuk inquery mahasiswa baru
	 * 
	 * @param nim
	 * @param nama
	 * @return
	 */
	public InquiryResponse inqueryMahasiswaBaru(String nim, String nama, LogHostToHost logHostToHost) {

		BiodataCalonMahasiswa biodataCalonMahasiswa = pembayaranUtil.getCalonMahasiswaByNoUjian(nim);
		if (biodataCalonMahasiswa == null) {
			InquiryResponse inquiryResponse = new InquiryResponse();
			inquiryResponse.setBillInfo1(nim);
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNoRegistrasiNotFound(logHostToHost, nim, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.INQUERY);
			return inquiryResponse;
		}

		List<String[]> data = new ArrayList<String[]>();

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(jenisKegiatan);

		Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
				ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
				biodataCalonMahasiswa.getTahunAkademik(),
				biodataCalonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL),
				biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
				biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());

		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
		DendaPembayaran dendaPembayaran = (DendaPembayaran) serializables[1];
		DendaPembayaranNominal dendaPembayaranNominal = (DendaPembayaranNominal) serializables[2];

		if (jadwalPembayaran == null) {
			displayUtil.displayPembayaranTerlambat(logHostToHost, biodataCalonMahasiswa.getNoRegistrasi(),
					pembayaranUtil.getBankHost(), nama, ConstantUtil.INQUERY);
			InquiryResponse inquiryResponse = new InquiryResponse();
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return inquiryResponse;

		}
		java.util.Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaCalonMahasiswa(
				biodataCalonMahasiswa, jenisKegiatan, biodataCalonMahasiswa.getProdiLulus(), true);
		java.util.Collection<BillDetail> billDetails = CommonUtil.generateBillDetails(dendaPembayaran,
				dendaPembayaranNominal, detailBiayas);
		String pemb = "|";
		Long total = 0L;
		for (BillDetail biaya : billDetails) {
			total += Long.parseLong(biaya.getBillAmount());
			pemb += biaya.getBillCode() + "\\" + biaya.getBillName() + "\\" + biaya.getBillShortName() + "\\"
					+ biaya.getBillAmount() + "|";
		}

		InquiryResponse inquiryResponse = CommonUtil.constructInquiryResponseMahasiswaBaru(biodataCalonMahasiswa,
				billDetails);

		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost, biodataCalonMahasiswa.getNoRegistrasi(), bankHost, nama,
					ConstantUtil.INQUERY);
			inquiryResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if (kegiatan == null || kegiatan.getId() == null || kegiatan.getPersentaseLunas() < 100.0) {
			inquiryResponse.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
					: ConstantUtilBankMandiri.SUCCESS_MANDIRI);
			data.add(new String[] { "response_code",
					total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT : ConstantUtilBankMandiri.SUCCESS });
			data.add(new String[] { "response_description",
					total.equals(0L) ? "Inquiry gagal dilakukan" : "Inquiry Sukses Dilakukan" });
			data.add(new String[] { "no_registrasi",
					biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().equals("")
							? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNoUjian() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
			data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });
			data.add(new String[] { "fakultas", biodataCalonMahasiswa.getProdiLulus() == null ? ""
					: biodataCalonMahasiswa.getProdiLulus().getFakultas().getNama() });
			data.add(new String[] { "prodi", biodataCalonMahasiswa.getProdiLulus() == null ? ""
					: biodataCalonMahasiswa.getProdiLulus().getNama() });
			data.add(new String[] { "angkatan", ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "" });
			data.add(new String[] { "semester", Perkuliahan.GANJIL });
			data.add(new String[] { "semester_ke", "0" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran",
					ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

			data.add(new String[] { "jumlah_yang_telah_dibayar",
					kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

			logHostToHost.setResponseCode((ConstantUtilBankMandiri.SUCCESS));
			logHostToHost.setResponseDescription("Inquiry Sukses Dilakukan");
		} else {
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_SUDAH_DIBAYAR);
			inquiryResponse
					.setBillInfo6(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber());
			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID });
			data.add(new String[] { "response_description",
					"Inquiry gagal dilakukan, karena calon mahasiswa sudah melakukan pembayaran" });
			data.add(new String[] { "no_registrasi",
					biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().equals("")
							? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNoUjian() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
			data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });
			data.add(new String[] { "fakultas", biodataCalonMahasiswa.getProdiLulus() == null ? ""
					: biodataCalonMahasiswa.getProdiLulus().getFakultas().getNama() });
			data.add(new String[] { "prodi", biodataCalonMahasiswa.getProdiLulus() == null ? ""
					: biodataCalonMahasiswa.getProdiLulus().getNama() });
			data.add(new String[] { "angkatan", ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "" });
			data.add(new String[] { "semester", Perkuliahan.GANJIL });
			data.add(new String[] { "semester_ke", "0" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran",
					ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

			data.add(new String[] { "jumlah_yang_telah_dibayar",
					kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

			logHostToHost.setResponseCode((ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID));
			logHostToHost
					.setResponseDescription("Inquiry gagal dilakukan, karena mahasiswa sudah melakukan pembayaran");
		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(
				biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().equals("")
						? biodataCalonMahasiswa.getNoRegistrasi()
						: biodataCalonMahasiswa.getNoUjian());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.INQUERY);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return inquiryResponse;
	}

	public InquiryResponse inqueryMahasiswaLama(String nim, String nama, LogHostToHost logHostToHost) {

		Mahasiswa mahasiswa = ConstantValues.ambilByNim(nim);
		if (mahasiswa == null) {
			InquiryResponse inquiryResponse = new InquiryResponse();
			inquiryResponse.setBillInfo1(nim);

			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNimNotFound(logHostToHost, nim, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.INQUERY);
			return inquiryResponse;
		}

		try {
			Session session = HibernateUtil.currentNativeSession();
			int countMahasiswaPindahan = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("alihProdiMahasiswa", mahasiswa)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			HibernateUtil.closeSession();

			if (countMahasiswaPindahan > 0) {
				InquiryResponse inquiryResponse = new InquiryResponse();
				inquiryResponse.setBillInfo1(nim);

				inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_DIBLOKIR);
				displayUtil.displayAlihProdi(logHostToHost, nim, pembayaranUtil.getBankHost(), nama,
						ConstantUtil.INQUERY);
				return inquiryResponse;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ws/billpayment/h2h/bankmandiri/logic/InqueryLogic.java:419");

		}

		mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler" : mahasiswa.getProgram());

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA);
		Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaHanyaBerdasarJenisKegiatan(
				ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, mahasiswa.getJenjang(), null,
				mahasiswa.getJenisSeleksi(), mahasiswa.getProgram(), mahasiswa.getNim());

		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
		DendaPembayaran dendaPembayaran = (DendaPembayaran) serializables[1];
		DendaPembayaranNominal dendaPembayaranNominal = (DendaPembayaranNominal) serializables[2];

		if (jadwalPembayaran == null) {
			displayUtil.displayPembayaranTerlambat(logHostToHost, mahasiswa.getNim(), pembayaranUtil.getBankHost(),
					nama, ConstantUtil.INQUERY);
			InquiryResponse inquiryResponse = new InquiryResponse();
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return inquiryResponse;
		}

		Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil()
				: jadwalPembayaran.getGanjil();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jadwalPembayaran.getTahunAkademik(),
				ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
				mahasiswa.getSemesterMulai());

		JenisKegiatan kegiatanDaftarUlangMahasiswaBaru = pembayaranUtil
				.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		List<TunggakanMahasiswa> tunggakanMahasiswas = pembayaranUtil.getTunggakanMahasiswa(
				new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru, jenisKegiatan }, mahasiswa, semester, null);

		if (tunggakanMahasiswas.size() != 0) {

			displayUtil.displayTunggakanMahasiswa(logHostToHost, mahasiswa.getNim(), pembayaranUtil.getBankHost(), nama,
					ConstantUtil.INQUERY, tunggakanMahasiswas);

			InquiryResponse inquiryResponse = new InquiryResponse();
			inquiryResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
			return inquiryResponse;
		}

		Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);

		@SuppressWarnings("unchecked")
		Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(mahasiswa, jadwalPembayaran, null,
				false);
		java.util.Collection<BillDetail> billDetails = CommonUtil.generateBillDetails(dendaPembayaran,
				dendaPembayaranNominal, detailBiayas);
		String pemb = "|";
		Long total = 0L;
		for (DetailBiaya biaya : detailBiayas) {
			ItemBiaya itemBiaya = biaya.getItemBiaya();
			Double nilai = biaya.hitungTotalKegiatan(kegiatan);
			pemb += itemBiaya.getId() + "\\" + itemBiaya.getNama().trim() + "\\" + itemBiaya.getDeskripsi().trim()
					+ "\\" + (nilai).longValue() + "|";
			total += (nilai).longValue();
		}

		InquiryResponse inquiryResponse = CommonUtil.constructInquiryResponse(mahasiswa, billDetails, ganjil, semester,
				kegiatan);
		boolean tidakBolehMencicil = Common.bolehKonfigurasi("mahasiswa_tidak_boleh_mencicil_pembayaran_via_h2h");

		// Double telahDibayar = kegiatan == null ? 0.0 : kegiatan
		// .getJumlahTelahDibayar();
		// Double sisaCicilan = total - telahDibayar;
		Double sisaCicilan = 100000.0;// hanya temporary

		List<String[]> data = new ArrayList<String[]>();
		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost, mahasiswa.getNim(), bankHost, nama, ConstantUtil.INQUERY);
			inquiryResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if ((kegiatan == null || kegiatan.getId() == null || kegiatan.getAmount() < 0.01 || !tidakBolehMencicil)
				&& sisaCicilan > 0.001) {
			inquiryResponse.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
					: ConstantUtilBankMandiri.SUCCESS_MANDIRI);
			data.add(new String[] { "response_code",
					total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT : ConstantUtilBankMandiri.SUCCESS });
			data.add(new String[] { "response_description", "Inquiry Sukses Dilakukan" });
			data.add(new String[] { "nim", mahasiswa.getNim() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", mahasiswa.getNama() });
			data.add(new String[] { "program", mahasiswa.getProgram() });
			data.add(new String[] { "fakultas", mahasiswa.getJurusan().getFakultas().getNama() });
			data.add(new String[] { "prodi", mahasiswa.getJurusan().getNama() });
			data.add(new String[] { "angkatan", mahasiswa.getTahunangkatan() + "" });
			data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
			data.add(new String[] { "semester_ke", semester + "" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran", ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

			data.add(new String[] { "jumlah_yang_telah_dibayar",
					kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

			logHostToHost.setResponseCode((ConstantUtilBankMandiri.SUCCESS));
			logHostToHost.setResponseDescription("Inquiry Sukses Dilakukan");

		} else {
			inquiryResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_SUDAH_DIBAYAR);
			inquiryResponse
					.setBillInfo6(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber());
			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID });
			data.add(new String[] { "response_description",
					"Inquiry gagal dilakukan, karena mahasiswa sudah melakukan pembayaran" });
			data.add(new String[] { "nim", mahasiswa.getNim() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", mahasiswa.getNama() });
			data.add(new String[] { "program", mahasiswa.getProgram() });
			data.add(new String[] { "fakultas", mahasiswa.getJurusan().getFakultas().getNama() });
			data.add(new String[] { "prodi", mahasiswa.getJurusan().getNama() });
			data.add(new String[] { "angkatan", mahasiswa.getTahunangkatan() + "" });
			data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
			data.add(new String[] { "semester_ke", semester + "" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran", ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

			data.add(new String[] { "jumlah_yang_telah_dibayar",
					kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

			logHostToHost.setResponseCode((ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID));
			logHostToHost
					.setResponseDescription("Inquiry gagal dilakukan, karena mahasiswa sudah melakukan pembayaran");
		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(mahasiswa.getNim());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.INQUERY);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		System.out.println("inquiryResponse = " + inquiryResponse);
		return inquiryResponse;
	}

}
