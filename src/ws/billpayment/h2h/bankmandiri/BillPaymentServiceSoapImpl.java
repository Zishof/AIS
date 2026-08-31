/**
 * BillPaymentServiceSoapImpl.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ws.billpayment.h2h.bankmandiri;

import javax.servlet.http.HttpServletRequest;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ws.billpayment.h2h.bankmandiri.logic.InqueryLogic;
import ws.billpayment.h2h.bankmandiri.logic.PaymentLogic;
import ws.billpayment.h2h.bankmandiri.logic.ReversalLogic;
import ws.billpayment.h2h.bankmandiri.util.ConstantUtilBankMandiri;

/**
 * Implementasi endpoint SOAP bill-payment. Kelas ini menghubungkan request protokol bank dengan
 * validasi, inquiry, pembayaran, reversal, dan pencatatan yang disediakan komponen domain AIS.
 *
 * <p><b>Batas tanggung jawab:</b> tipe ini mendeklarasikan kontrak {@link BillPaymentServiceSoap}. Implementasi
 * konkret bertanggung jawab atas transaksi, resource, error handling, dan efek samping; pemanggil sebaiknya
 * bergantung pada kontrak ini agar tidak menggandakan integrasi.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code PaymentLogic pembayaranLogic}, {@code
 * ReversalLogic reversalLogic}, {@code InqueryLogic inqueryLogic}; operasi domain lain ({@code reverse()},
 * {@code payment()}, {@code inquiry()}, {@code echoTest()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> bergantung pada perannya, operasi dapat mengubah konfigurasi endpoint, membuat stub,
 * melakukan I/O jaringan, atau meneruskan request ke logika transaksi. Error transport tetap diteruskan sebagai
 * kontrak JAX-RPC/Axis; jangan menggandakan mapping WSDL di kelas lain.</p>
 */
public class BillPaymentServiceSoapImpl implements BillPaymentServiceSoap {

	private PaymentLogic pembayaranLogic = new PaymentLogic();
	private ReversalLogic reversalLogic = new ReversalLogic();
	private InqueryLogic inqueryLogic = new InqueryLogic();

	public ReversalResponse reverse(ReversalRequest request)
			throws java.rmi.RemoteException {
		String nim = request.getBillKey1();
		String nama = request.toString();

		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(request.getBillKey1());
		logHostToHost.setInfo1(request.getChannelID());
		logHostToHost.setInfo2(request.getCompanyCode());
		logHostToHost.setInfo3(request.getCurrency());
		logHostToHost.setInfo4(request.getLanguage());
		logHostToHost.setInfo5(request.getOrigTransmissionDateTime());
		logHostToHost.setInfo6(request.getOrigTrxDateTime());
		logHostToHost.setInfo7(request.getPaymentAmount());
		logHostToHost.setInfo8(request.getReference1());
		logHostToHost.setInfo9(request.getReference2());
		logHostToHost.setInfo10(request.getReference3());
		logHostToHost.setInfo11(request.getTerminalID());
		logHostToHost.setInfo12(request.getTransactionID());
		logHostToHost.setInfo13(request.getTransmissionDateTime());
		logHostToHost.setInfo14(request.getTrxDateTime());

		try {

			ReversalResponse reversalResponse = null;
			Mahasiswa mahasiswa = ConstantValues.ambilByNim(nim);
			if (mahasiswa == null) {

				BiodataCalonMahasiswa biodataCalonMahasiswa = reversalLogic.pembayaranUtil
						.getCalonMahasiswaByNoUjian(nim);
				if (biodataCalonMahasiswa == null) {
					biodataCalonMahasiswa = reversalLogic.pembayaranUtil
							.getCalonMahasiswaByNoPendaftaran(nim);
					if (biodataCalonMahasiswa == null) {
						reversalResponse = new ReversalResponse();
						reversalResponse
								.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
					} else {
						reversalResponse = reversalLogic
								.reversalCalonMahasiswaBaru(nim, nama,
										logHostToHost);

					}
				} else {
					reversalResponse = reversalLogic.reversalMahasiswaBaru(nim,
							nama, logHostToHost);
				}

			} else {
				reversalResponse = reversalLogic.reversalMahasiswaLama(nim,
						nama, logHostToHost);
			}

			// ReversalResponse reversalResponse = reversalLogic
			// .reversalMahasiswaLama(nim, nama, logHostToHost);
			// if (reversalResponse
			// .getStatus()
			// .getErrorCode()
			// .equalsIgnoreCase(
			// ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN
			// .getErrorCode())) {
			// reversalResponse = reversalLogic.reversalCalonMahasiswaBaru(
			// nim, nama, logHostToHost);
			// }
			return reversalResponse;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		return null;
	}

	public PaymentResponse payment(PaymentRequest request)
			throws java.rmi.RemoteException {
		String nim = request.getBillKey1();
		String paymentAmount = request.getPaymentAmount();
		String nama = request.toString();

		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(request.getBillKey1());
		logHostToHost.setInfo1(request.getChannelID());
		logHostToHost.setInfo2(request.getCompanyCode());
		logHostToHost.setInfo3(request.getCurrency());
		logHostToHost.setInfo4(request.getLanguage());
		logHostToHost.setInfo7(request.getPaymentAmount());
		logHostToHost.setInfo8(request.getReference1());
		logHostToHost.setInfo9(request.getReference2());
		logHostToHost.setInfo10(request.getReference3());
		logHostToHost.setInfo11(request.getTerminalID());
		logHostToHost.setInfo12(request.getTransactionID());
		logHostToHost.setInfo13(request.getTransmissionDateTime());
		logHostToHost.setInfo14(request.getTrxDateTime());

		try {
			PaymentResponse paymentResponse = null;
			Mahasiswa mahasiswa =  ConstantValues.ambilByNim(nim);
			if (mahasiswa == null) {

				BiodataCalonMahasiswa biodataCalonMahasiswa = pembayaranLogic.pembayaranUtil
						.getCalonMahasiswaByNoUjian(nim);
				if (biodataCalonMahasiswa == null) {
					biodataCalonMahasiswa = pembayaranLogic.pembayaranUtil
							.getCalonMahasiswaByNoPendaftaran(nim);
					if (biodataCalonMahasiswa == null) {
						paymentResponse = new PaymentResponse();
						paymentResponse.setBillInfo1(nim);
						paymentResponse
								.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
					} else {

						paymentResponse = pembayaranLogic
								.pembayaranCalonMahasiswaBaru(nim, nama,
										logHostToHost, paymentAmount);
					}
				} else {
					paymentResponse = pembayaranLogic.pembayaranMahasiswaBaru(
							nim, nama, logHostToHost, paymentAmount);
				}

			} else {
				paymentResponse = pembayaranLogic.pembayaranMahasiswaLama(nim,
						nama, logHostToHost, paymentAmount);
			}
			return paymentResponse;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		return null;
	}

	public InquiryResponse inquiry(InquiryRequest request)
			throws java.rmi.RemoteException {
		String nim = request.getBillKey1();
		String nama = request.toString();

		LogHostToHost logHostToHost = new LogHostToHost();
		logHostToHost.setInfo0(request.getBillKey1());
		logHostToHost.setInfo1(request.getChannelID());
		logHostToHost.setInfo2(request.getCompanyCode());
		logHostToHost.setInfo4(request.getLanguage());
		logHostToHost.setInfo8(request.getReference1());
		logHostToHost.setInfo9(request.getReference2());
		logHostToHost.setInfo10(request.getReference3());
		logHostToHost.setInfo11(request.getTerminalID());
		logHostToHost.setInfo13(request.getTransmissionDateTime());
		logHostToHost.setInfo14(request.getTrxDateTime());

		try {
			InquiryResponse inquiryResponse = null;
			Mahasiswa mahasiswa =  ConstantValues.ambilByNim(nim);
			if (mahasiswa == null) {

				BiodataCalonMahasiswa biodataCalonMahasiswa = inqueryLogic.pembayaranUtil
						.getCalonMahasiswaByNoUjian(nim);
				if (biodataCalonMahasiswa == null) {
					biodataCalonMahasiswa = inqueryLogic.pembayaranUtil
							.getCalonMahasiswaByNoPendaftaran(nim);
					if (biodataCalonMahasiswa == null) {
						inquiryResponse = new InquiryResponse();
						inquiryResponse.setBillInfo1(nim);
						inquiryResponse
								.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
					} else {
						inquiryResponse = inqueryLogic.inqueryCalonMahasiswa(
								nim, nama, logHostToHost);

					}
				} else {
					inquiryResponse = inqueryLogic.inqueryMahasiswaBaru(nim,
							nama, logHostToHost);
				}

			} else {
				inquiryResponse = inqueryLogic.inqueryMahasiswaLama(nim, nama,
						logHostToHost);
			}

			return inquiryResponse;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		return null;
	}

	public java.lang.String echoTest(java.lang.String tx)
			throws java.rmi.RemoteException {
		 String ipAdd = ((HttpServletRequest) MessageContext.getCurrentContext()
				.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST))
				.getRemoteAddr();
		return "No IP Anda = " + ipAdd + " ---> kembalian echo -----> " + tx;
	}

}
