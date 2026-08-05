/**
 * BillPaymentServiceSoapImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package CIMB3rdParty.BillPaymentWS;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.report.CommonReportHelper;
import ais.action.ws.logic.PaymentLogic;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.LogHostToHost;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.cimb.CimbRequest;
import ais.database.model.cimb.CimbRequestDetail;
import ais.database.model.cimb.CimbResponse;

public class BillPaymentServiceSoapImpl implements CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoap {

	private PaymentLogic paymentLogic = new PaymentLogic();

	@SuppressWarnings("unchecked")
	public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRs inquiry(
			CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRq parameters) throws java.rmi.RemoteException {

		CIMB3rdParty.BillPaymentWS.InquiryRq rq = parameters.getInquiryRq();

		String transactionID = rq.getTransactionID();
		String channelID = rq.getChannelID();
		String terminalID = rq.getTerminalID();
		String transactionDate = rq.getTransactionDate();
		String companyCode = rq.getCompanyCode();
		String customerKey1 = rq.getCustomerKey1();
		String customerKey2 = rq.getCustomerKey2();
		String customerKey3 = rq.getCustomerKey3();

		Session session = HibernateUtil.currentNativeSession();

		CimbRequest cimbRequest = (CimbRequest) session.createCriteria(CimbRequest.class)
				.add(Restrictions.eq("trxId", customerKey1)).setMaxResults(1).uniqueResult();

		BillDetail[] billDetailList = new BillDetail[] {};

		String billInfo = "";

		if (cimbRequest != null) {

			List<CimbRequestDetail> cimbRequestDetails = session.createCriteria(CimbRequestDetail.class)
					.add(Restrictions.eq("cimbRequest", cimbRequest)).add(Restrictions.gt("ke", 0))
					.addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();

			billDetailList = new BillDetail[cimbRequestDetails.size()];
			int i = 0;
			for (CimbRequestDetail cimbRequestDetail : cimbRequestDetails) {
				String billCurrency = "IDR";
				String billCode = cimbRequestDetail.getItemBiaya().getKode() + "-"
						+ cimbRequestDetail.getItemBiaya().getNama();
				Integer billAmount = cimbRequestDetail.getNilai().intValue();
				String billReference = cimbRequestDetail.getId() + "";
				BillDetail billDetail = new BillDetail(billCurrency, billCode, billAmount, billReference);
				billDetailList[i] = billDetail;

				billInfo += billInfo.isEmpty() ? (billCode + ":" + billAmount) : ";" + (billCode + ":" + billAmount);

				i++;
			}
		}

		String currency = "IDR";
		Integer amount = cimbRequest == null ? 0 : cimbRequest.getAmount().intValue();
		Integer fee = 0;
		Integer paidAmount = cimbRequest == null ? 0 : cimbRequest.getAmount().intValue();
		String customerName = cimbRequest == null ? ""
				: (cimbRequest.getMahasiswa() == null ? cimbRequest.getBiodataCalonMahasiswa().getNama()
						: cimbRequest.getMahasiswa().getNama());
		String additionalData1 = cimbRequest == null ? ""
				: (cimbRequest.getMahasiswa() == null ? cimbRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
						: cimbRequest.getMahasiswa().getNim());
		String additionalData2 = cimbRequest == null ? ""
				: (cimbRequest.getMahasiswa() == null ? cimbRequest.getBiodataCalonMahasiswa().toString()
						: cimbRequest.getMahasiswa().getJurusan().getNama());
		String additionalData3 = "";
		String additionalData4 = "";
		String flagPayment = "100000";
		String responseCode = cimbRequest == null ? "40" : (cimbRequest.getCimbResponse() == null ? "00" : "41");
		String responseDescription = cimbRequest == null ? "Bill Not Found"
				: (cimbRequest.getCimbResponse() == null ? "Transaction Success" : "Bill Already Paid");

		try {
			BankHost bankHost = paymentLogic.pembayaranUtil.getBankHost();

			if (bankHost == null) {
				responseCode = "17";
				responseDescription = "Provider Problem";
			}

			LogHostToHost logHostToHost = new LogHostToHost();
			logHostToHost.setKode(customerKey1);

			logHostToHost.setNama("transactionID:" + transactionID + ", channelID:" + channelID + ", terminalID:"
					+ terminalID + ", transactionDate:" + transactionDate + ", companyCode=" + companyCode
					+ ", customerKey1=" + customerKey1);
			logHostToHost.setKode(customerKey1);
			logHostToHost.setBankHost(bankHost);
			logHostToHost.setIp(((HttpServletRequest) MessageContext.getCurrentContext()
					.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getRemoteAddr());
			logHostToHost.setKeterangan("transactionID:" + transactionID + ", channelID:" + channelID + ", terminalID:"
					+ terminalID + ", transactionDate:" + transactionDate + ", companyCode=" + companyCode
					+ ", customerKey1=" + customerKey1);

			logHostToHost.setNim(cimbRequest == null ? ""
					: (cimbRequest.getMahasiswa() != null ? cimbRequest.getMahasiswa().getNim()
							: (cimbRequest.getBiodataCalonMahasiswa() != null
									? cimbRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
									: "")));
			logHostToHost.setItem(billInfo);
			logHostToHost.setNominal(amount.doubleValue());
			logHostToHost.setTransactionType(ConstantUtil.INQUERY);
			logHostToHost.setResponseCode(responseCode);
			logHostToHost.setResponseDescription(responseDescription);
			logHostToHost.setRequest("transactionID:" + transactionID + ", channelID:" + channelID + ", terminalID:"
					+ terminalID + ", transactionDate:" + transactionDate + ", companyCode=" + companyCode
					+ ", customerKey1=" + customerKey1);

			logHostToHost.setResponse("customerName:" + customerName + ", amount:" + amount + ", additionalData1="
					+ additionalData1 + ", additionalData2=" + additionalData2 + ", responseCode=" + responseCode
					+ ", responseDescription:" + responseDescription + ", billInfo=" + billInfo + ", nim="
					+ logHostToHost.getNim());

			session.getTransaction().begin();
			session.save(logHostToHost);
			session.getTransaction().commit();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();

		CIMB3rdParty.BillPaymentWS.InquiryRs rs = new InquiryRs(transactionID, channelID, terminalID, transactionDate,
				companyCode, customerKey1, customerKey2, customerKey3, billDetailList, currency, amount, fee,
				paidAmount, customerName, additionalData1, additionalData2, additionalData3, additionalData4,
				flagPayment, responseCode, responseDescription);

		CIMB3rdParty.BillPaymentWS.CIMB3RdParty_InquiryRs inquiryRs = new CIMB3RdParty_InquiryRs(rs);

		return inquiryRs;
	}

	@SuppressWarnings("unchecked")
	public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRs payment(
			CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRq parameters) throws java.rmi.RemoteException {

		CIMB3rdParty.BillPaymentWS.PaymentRq rq = parameters.getPaymentRq();

		String transactionID = rq.getTransactionID();
		String channelID = rq.getChannelID();
		String terminalID = rq.getTerminalID();
		String transactionDate = rq.getTransactionDate();
		String companyCode = rq.getCompanyCode();
		String customerKey1 = rq.getCustomerKey1();
		String customerKey2 = rq.getCustomerKey2();
		String customerKey3 = rq.getCustomerKey3();
		String referenceNumberTransaction = rq.getReferenceNumberTransaction();

		Integer amountRequest = rq.getAmount();

		Session session = HibernateUtil.currentNativeSession();
		CimbRequest cimbRequest = (CimbRequest) session.createCriteria(CimbRequest.class)
				.add(Restrictions.eq("trxId", customerKey1)).setMaxResults(1).uniqueResult();

		String currency = "IDR";
		Integer amount = cimbRequest == null ? 0 : cimbRequest.getAmount().intValue();
		Integer fee = 0;
		Integer paidAmount = cimbRequest == null ? 0 : cimbRequest.getAmount().intValue();
		String customerName = cimbRequest == null ? ""
				: (cimbRequest.getMahasiswa() == null ? cimbRequest.getBiodataCalonMahasiswa().getNama()
						: cimbRequest.getMahasiswa().getNama());
		String additionalData1 = cimbRequest == null ? ""
				: (cimbRequest.getMahasiswa() == null ? cimbRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
						: cimbRequest.getMahasiswa().getNim());
		String additionalData2 = cimbRequest == null ? ""
				: (cimbRequest.getMahasiswa() == null ? cimbRequest.getBiodataCalonMahasiswa().toString()
						: cimbRequest.getMahasiswa().getJurusan().getNama());
		String additionalData3 = "";
		String additionalData4 = "";
		String paymentFlag = "100000";
		String responseCode = (amountRequest <= 0 && !amountRequest.equals(amount)) ? "46"
				: (cimbRequest == null ? "40" : (cimbRequest.getCimbResponse() == null ? "00" : "41"));
		String responseDescription = (amountRequest <= 0 && !amountRequest.equals(amount)) ? "Invalid Amount"
				: (cimbRequest == null ? "Bill Not Found"
						: (cimbRequest.getCimbResponse() == null ? "Transaction Success" : "Bill Already Paid"));

		Kegiatan kegiatan = null;

		if (cimbRequest != null && amountRequest > 0 && amountRequest.equals(amount)) {
			CimbResponse cimbResponse = new CimbResponse();
			cimbResponse.setKeterangan(rq.toString());
			cimbResponse.setNama(customerKey1);
			cimbResponse.setStatus("Sukses");

			cimbResponse.setTrxId(customerKey1);
			cimbResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());

			session.getTransaction().begin();
			session.saveOrUpdate(cimbResponse);
			session.getTransaction().commit();

			cimbRequest.setStatus("Sukses");
			cimbRequest.setKodeStatus("00");
			cimbRequest.setCimbResponse(cimbResponse);
			session.getTransaction().begin();
			session.update(cimbRequest);
			session.getTransaction().commit();

			kegiatan = createKegiatan(cimbRequest, session);
			Double nilaiBiayaHarusDiBayars = cimbRequest.getNilaiBiayaHarusDiBayars();

			List<CimbRequestDetail> cimbRequestDetails = session.createCriteria(CimbRequestDetail.class)
					.add(Restrictions.eq("cimbRequest", cimbRequest)).add(Restrictions.gt("ke", 0))
					.addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();
			System.out.println("cimbRequestDetails==>" + cimbRequestDetails);
			if (!cimbRequestDetails.isEmpty()) {

				for (CimbRequestDetail cimbRequestDetail : cimbRequestDetails) {

					String ref = "cimbRequestDetail-" + cimbRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(cimbRequestDetail.getDetailBiaya());
						cicilanPembayaran.setRef(ref);
						cicilanPembayaran.setValidator("CIMB Niaga");
						cicilanPembayaran.setKe(cimbRequestDetail.getKe());
						cicilanPembayaran.setKegiatan(kegiatan);
						cicilanPembayaran.setItemBiaya(cimbRequestDetail.getItemBiaya());
						cicilanPembayaran
								.setPengaturanPembayaranBulanan(cimbRequestDetail.getPengaturanPembayaranBulanan());
						cicilanPembayaran.setNilai(cimbRequestDetail.getNilai());
						cicilanPembayaran.setTanggal(cimbRequestDetail.getTanggal());
						cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);
						cicilanPembayaran.setDenda(cimbRequestDetail.getDenda());
						cicilanPembayaran.setNilaiAsli(cimbRequestDetail.getNilaiAsli());
						session.getTransaction().begin();
						if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
						session.getTransaction().commit();
					}

				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				Double jumlah = d[0];
				Double denda = d[1];
				kegiatan.setDenda(denda.doubleValue());
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
				kegiatan.setAmount(jumlah.doubleValue());
				kegiatan.setValidator("CIMB Niaga");

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();

			}

			if (amount > 0) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(amount.doubleValue());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			pembayaranUtil.updateTunggakan(kegiatan, session);

			Mahasiswa mhs = cimbRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = cimbRequest.getBiodataCalonMahasiswa();
			if (mhs != null) {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
			} else if (bio != null) {
				CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
			}
		}

		try {
			BankHost bankHost = paymentLogic.pembayaranUtil.getBankHost();

			if (bankHost == null) {
				responseCode = "17";
				responseDescription = "Provider Problem";
			}

			LogHostToHost logHostToHost = new LogHostToHost();
			logHostToHost.setBankHost(bankHost);
			logHostToHost.setIp(((HttpServletRequest) MessageContext.getCurrentContext()
					.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST)).getRemoteAddr());
			logHostToHost.setKeterangan("transactionID:" + transactionID + ", channelID:" + channelID + ", terminalID:"
					+ terminalID + ", transactionDate:" + transactionDate + ", companyCode=" + companyCode
					+ ", customerKey1=" + customerKey1);

			logHostToHost.setNim(cimbRequest == null ? ""
					: (cimbRequest.getMahasiswa() != null ? cimbRequest.getMahasiswa().getNim()
							: (cimbRequest.getBiodataCalonMahasiswa() != null
									? cimbRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
									: "")));

			logHostToHost.setNama("transactionID:" + transactionID + ", channelID:" + channelID + ", terminalID:"
					+ terminalID + ", transactionDate:" + transactionDate + ", companyCode=" + companyCode
					+ ", customerKey1=" + customerKey1 + ", referenceNumberTransaction=" + referenceNumberTransaction
					+ ", nim=" + logHostToHost.getNim());
			logHostToHost.setKode(customerKey1);

			logHostToHost.setNominal(amount.doubleValue());
			logHostToHost.setTransactionType(ConstantUtil.PAY);
			logHostToHost.setResponseCode(responseCode);
			logHostToHost.setResponseDescription(responseDescription);
			logHostToHost.setRequest("transactionID:" + transactionID + ", channelID:" + channelID + ", terminalID:"
					+ terminalID + ", transactionDate:" + transactionDate + ", companyCode=" + companyCode
					+ ", customerKey1=" + customerKey1 + ", referenceNumberTransaction=" + referenceNumberTransaction);

			logHostToHost.setResponse("customerName:" + customerName + ", amount:" + amount + ", additionalData1="
					+ additionalData1 + ", additionalData2=" + additionalData2 + ", responseCode=" + responseCode
					+ ", responseDescription:" + responseDescription);
			logHostToHost.setKegiatan(kegiatan);

			session.getTransaction().begin();
			session.save(logHostToHost);
			session.getTransaction().commit();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();

		CIMB3rdParty.BillPaymentWS.PaymentRs rs = new PaymentRs(transactionID, channelID, terminalID, transactionDate,
				companyCode, customerKey1, customerKey2, customerKey3, paymentFlag, customerName, currency, amount, fee,
				paidAmount, referenceNumberTransaction, additionalData1, additionalData2, additionalData3,
				additionalData4, responseCode, responseDescription);

		CIMB3rdParty.BillPaymentWS.CIMB3RdParty_PaymentRs paymentRs = new CIMB3RdParty_PaymentRs(rs);
		return paymentRs;
	}

	public CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRs echoTest(
			CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRq parameters) throws java.rmi.RemoteException {

		CIMB3rdParty.BillPaymentWS.CIMB3RdParty_EchoRs echoRs = new CIMB3RdParty_EchoRs("Sukses");
		return echoRs;
	}

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public static Kegiatan createKegiatan(CimbRequest cimbRequest, Session session) {
		Kegiatan kegiatan = null;
		Double nilaiBiayaHarusDiBayars = cimbRequest.getNilaiBiayaHarusDiBayars();

		Mahasiswa mhs = cimbRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = cimbRequest.getBiodataCalonMahasiswa();

		Integer semester = cimbRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = cimbRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = cimbRequest.getJenisKegiatan();
		if (mhs != null) {
			kegiatan = mhs.ambilKegiatans(semester, jenisKegiatan);
		} else if (bio != null) {
			kegiatan = bio.ambilKegiatans(jenisKegiatan);
		}

		System.out.println("mhs==>" + mhs + ",bio==>" + bio + ", semester==>" + semester + ",jadwalPembayaran==>"
				+ jadwalPembayaran + ",jenisKegiatan==>" + jenisKegiatan + ",kegiatan==>" + kegiatan
				+ ",nilaiBiayaHarusDiBayars==>" + nilaiBiayaHarusDiBayars + ",pengurangan==>"
				+ cimbRequest.getPengurangan() + ", hapusCicilanSebelumnya ==> "
				+ cimbRequest.getHapusCicilanSebelumnya());

		if (kegiatan == null || kegiatan.getId() == null) {
			kegiatan = new Kegiatan();
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setMahasiswa(mhs);
			kegiatan.setCalonMahasiswa(bio);
			kegiatan.setSemster(semester);
			if (mhs != null) {
				kegiatan.setStatusMahasiswa(Common
						.currentStatus(mhs, kegiatan.getTahunAkademik(), kegiatan.getSemster()).getStatusMahasiswa());
			} else if (bio != null) {
				kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
			}
			kegiatan.setTahunAkademik(cimbRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator("CIMB Niaga");
			kegiatan.setPengurangan(cimbRequest.getPengurangan());
			kegiatan.setKeterangan(cimbRequest.getKeterangan());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
		} else {
			session.refresh(kegiatan); 
		}
		return kegiatan;
	}
}
