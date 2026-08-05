package ais.action.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;
import ais.database.model.ipaymu.IpaymuResponse;
import id.co.veritrans.mdk.v1.gateway.model.FraudStatus;
import id.co.veritrans.mdk.v1.gateway.model.TransactionStatus;
import id.co.veritrans.mdk.v1.gateway.model.VtResponse;
import id.co.veritrans.mdk.v1.gateway.model.vtdirect.paymentmethod.CreditCard;

/**
 * Servlet implementation class CheckISBN
 */
public class VtResponseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public VtResponseServlet() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void prosesResponse(IpaymuResponse ipaymuResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		IpaymuRequest ipaymuRequest = (IpaymuRequest) session.createCriteria(IpaymuRequest.class)
				.add(Restrictions.ilike("nama", ipaymuResponse.getNama(), MatchMode.EXACT)).setMaxResults(1)
				.uniqueResult();
		System.out.println("ipaymuRequest==>" + ipaymuRequest);
		if (ipaymuRequest != null && ipaymuRequest.getIpaymuResponse() == null) {
			ipaymuRequest.setIpaymuResponse(ipaymuResponse);
			session.getTransaction().begin();
			session.update(ipaymuRequest);
			session.getTransaction().commit();
		}
		if (ipaymuRequest != null && ipaymuResponse.getStatus().trim().equalsIgnoreCase(IpaymuResponse.BERHASIL)) {

			Double nilaiBiayaHarusDiBayars = ipaymuRequest.getNilaiBiayaHarusDiBayars();

			Kegiatan kegiatan = null;

			Mahasiswa mhs = ipaymuRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = ipaymuRequest.getBiodataCalonMahasiswa();

			Integer semester = ipaymuRequest.getSemester();
			JadwalPembayaran jadwalPembayaran = ipaymuRequest.getJadwalPembayaran();
			JenisKegiatan jenisKegiatan = ipaymuRequest.getJenisKegiatan();
			if (mhs != null) {
				kegiatan = mhs.ambilKegiatans(semester, jenisKegiatan);
			} else if (bio != null) {
				kegiatan = bio.ambilKegiatans(semester, jenisKegiatan);
				if (kegiatan == null && semester <= 1) {
					kegiatan = bio.ambilKegiatans(jenisKegiatan);
				}
			}

			System.out.println("mhs==>" + mhs + ",bio==>" + bio + ", semester==>" + semester + ",jadwalPembayaran==>"
					+ jadwalPembayaran + ",jenisKegiatan==>" + jenisKegiatan + ",kegiatan==>" + kegiatan
					+ ",nilaiBiayaHarusDiBayars==>" + nilaiBiayaHarusDiBayars + ",pengurangan==>"
					+ ipaymuRequest.getPengurangan());

			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = new Kegiatan();
				kegiatan.setJenisKegiatan(jenisKegiatan);
				kegiatan.setJadwalPembayaran(jadwalPembayaran);
				kegiatan.setMahasiswa(mhs);
				kegiatan.setCalonMahasiswa(bio);
				kegiatan.setSemster(semester);
				if (mhs != null) {
					kegiatan.setStatusMahasiswa(
							ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mhs, kegiatan.getTahunAkademik(), kegiatan.getSemster())
									.getStatusMahasiswa());
				} else if (bio != null) {
					kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
				}
				kegiatan.setTahunAkademik(ipaymuRequest.getTahunAkademik());
				kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
				kegiatan.setValidated(1);
				kegiatan.setValidator("Vt");
				kegiatan.setPengurangan(ipaymuRequest.getPengurangan());
				kegiatan.setKeterangan(ipaymuRequest.getKeterangan());
				kegiatan.setAmount(nilaiBiayaHarusDiBayars);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			if (ipaymuRequest.getAmount() > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(ipaymuRequest.getAmount());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			List<IpaymuRequestDetail> ipaymuRequestDetails = session.createCriteria(IpaymuRequestDetail.class)
					.add(Restrictions.eq("ipaymuRequest", ipaymuRequest)).addOrder(Order.asc("ke"))
					.add(Restrictions.gt("ke", 0)).list();
			System.out.println("ipaymuRequestDetails==>" + ipaymuRequestDetails);
			if (!ipaymuRequestDetails.isEmpty()) {

				for (IpaymuRequestDetail ipaymuRequestDetail : ipaymuRequestDetails) {
					String ref = "ipaymuRequestDetail-" + ipaymuRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(null);

					}

					cicilanPembayaran.setRef(ref);
					cicilanPembayaran.setValidator("Vt");
					cicilanPembayaran.setKe(ipaymuRequestDetail.getKe());
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setKeterangan(ipaymuRequestDetail.getKeterangan());
					cicilanPembayaran.setItemBiaya(ipaymuRequestDetail.getItemBiaya());
					cicilanPembayaran
							.setPengaturanPembayaranBulanan(ipaymuRequestDetail.getPengaturanPembayaranBulanan());
					cicilanPembayaran.setNilai(ipaymuRequestDetail.getNilai());
					cicilanPembayaran.setTanggal(ipaymuRequestDetail.getTanggal());
					cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);
					session.getTransaction().begin();
					if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
					session.getTransaction().commit();

					System.out.println("cicilanPembayaran==>" + cicilanPembayaran);
				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				Double jumlah = d[0];
				Double denda = d[1];
				kegiatan.setDenda(denda.doubleValue());
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
				kegiatan.setAmount(jumlah.doubleValue());
				kegiatan.setValidator("Vt");

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			pembayaranUtil.updateTunggakan(kegiatan, session);

			if (mhs != null) {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
			} else if (bio != null) {
				CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VtResponseServlet.java:218");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VtResponseServlet.java:219");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VtResponseServlet.java:220");}
			}
		}
	}

	public static void prosesTransaksi(Map<String, String> param) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			IpaymuResponse ipaymuResponse = new IpaymuResponse();
			ipaymuResponse.setNama(param.get("sid"));
			ipaymuResponse.setStatus(param.get("status"));
			ipaymuResponse.setMerchant(param.get("merchant"));
			ipaymuResponse.setTrxId(param.get("trx_id"));
			ipaymuResponse.setProduct(param.get("product"));
			ipaymuResponse.setBuyer(param.get("buyer"));
			ipaymuResponse.setNoRekeningDeposit(param.get("no_rekening_deposit"));
			ipaymuResponse.setComments(param.get("comments"));
			session.getTransaction().begin();
			session.save(ipaymuResponse);
			session.getTransaction().commit();

			prosesResponse(ipaymuResponse);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VtResponseServlet.java:246");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VtResponseServlet.java:247");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/VtResponseServlet.java:248");}
			}
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		try {
			ServletInputStream inputStream = request.getInputStream();
			VtResponse vtResponse = VtResponse.deserializeJson(inputStream);

			// if necessary, we can utilize VtDirect's or VtWeb's Check
			// Transaction Status Feature to make sure the notification is
			// really coming from Veritrans
			String orderId = vtResponse.getOrderId();

			String approvalCode = vtResponse.getApprovalCode();

			String statusMessage = vtResponse.getStatusMessage();
			String permataVaNumber = vtResponse.getPermataVaNumber();
			String signatureKey = vtResponse.getSignatureKey();

			String cardToken = vtResponse.getCardToken();

			String savedCardToken = vtResponse.getSavedCardToken();
			Date savedCardTokenExpiredAt = vtResponse.getSavedCardTokenExpiredAt();
			Boolean secureToken = vtResponse.getSecureToken();
			CreditCard.Bank bank = vtResponse.getBank();
			String billerCode = vtResponse.getBillerCode();
			String billKey = vtResponse.getBillKey();
			String xlTunaiOrderId = vtResponse.getXlTunaiOrderId();
			String biiVaNumber = vtResponse.getBiiVaNumber();
			String redirectUrl = vtResponse.getRedirectUrl();
			String eci = vtResponse.getEci();
			String[] validationMessages = vtResponse.getValidationMessages();
			Integer page = vtResponse.getPage();
			Integer totalPage = vtResponse.getTotalPage();
			Integer totalRecord = vtResponse.getTotalRecord();
			String customField1 = vtResponse.getCustomField1();
			String customField2 = vtResponse.getCustomField2();
			String customField3 = vtResponse.getCustomField3();

			String statusCode = vtResponse.getStatusCode();
			String transactionId = vtResponse.getTransactionId();

			String paymentMethod = vtResponse.getPaymentMethod();
			Date transactionTime = vtResponse.getTransactionTime();
			TransactionStatus transactionStatus = vtResponse.getTransactionStatus();
			BigDecimal grossAmount = vtResponse.getGrossAmount();
			FraudStatus fraudStatus = vtResponse.getFraudStatus();

			String maskedCardNumber = vtResponse.getMaskedCardNumber();

			System.out.println("VtResponseServlet ==> orderId=>" + orderId);
			System.out.println("VtResponseServlet ==> statusCode=>" + statusCode);
			System.out.println("VtResponseServlet ==> transactionId=>" + transactionId);
			System.out.println("VtResponseServlet ==> paymentMethod=>" + paymentMethod);
			System.out.println("VtResponseServlet ==> transactionStatus=>" + transactionStatus);
			System.out.println("VtResponseServlet ==> transactionTime=>" + transactionTime);
			System.out.println("VtResponseServlet ==> grossAmount=>" + grossAmount);
			System.out.println("VtResponseServlet ==> maskedCardNumber=>" + maskedCardNumber);
			System.out.println("VtResponseServlet ==> fraudStatus=>" + fraudStatus);

			System.out.println("VtResponseServlet ==> approvalCode=>" + approvalCode);
			System.out.println("VtResponseServlet ==> statusMessage=>" + statusMessage);
			System.out.println("VtResponseServlet ==> permataVaNumber=>" + permataVaNumber);
			System.out.println("VtResponseServlet ==> signatureKey=>" + signatureKey);
			System.out.println("VtResponseServlet ==> cardToken=>" + cardToken);
			System.out.println("VtResponseServlet ==> savedCardToken=>" + savedCardToken);
			System.out.println("VtResponseServlet ==> savedCardTokenExpiredAt=>" + savedCardTokenExpiredAt);
			System.out.println("VtResponseServlet ==> secureToken=>" + secureToken);
			System.out.println("VtResponseServlet ==> bank=>" + bank);
			System.out.println("VtResponseServlet ==> billerCode=>" + billerCode);
			System.out.println("VtResponseServlet ==> billKey=>" + billKey);
			System.out.println("VtResponseServlet ==> xlTunaiOrderId=>" + xlTunaiOrderId);
			System.out.println("VtResponseServlet ==> biiVaNumber=>" + biiVaNumber);
			System.out.println("VtResponseServlet ==> redirectUrl=>" + redirectUrl);
			System.out.println("VtResponseServlet ==> eci=>" + eci);
			System.out.println("VtResponseServlet ==> validationMessages=>" + validationMessages);
			System.out.println("VtResponseServlet ==> page=>" + page);
			System.out.println("VtResponseServlet ==> totalPage=>" + totalPage);
			System.out.println("VtResponseServlet ==> totalRecord=>" + totalRecord);
			System.out.println("VtResponseServlet ==> customField1=>" + customField1);
			System.out.println("VtResponseServlet ==> customField2=>" + customField2);
			System.out.println("VtResponseServlet ==> customField3=>" + customField3);

			if (vtResponse.getTransactionStatus() == TransactionStatus.SETTLED) {
				// handle settled / successful charge request
			} else {
				// handle denied / unexpected response
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

}
