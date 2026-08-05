/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.action.ws;

import ais.action.ws.logic.InqueryLogic;
import ais.action.ws.logic.PaymentLogic;
import ais.action.ws.logic.ReversalLogic;
import ais.action.ws.model.Response;
import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.ConstantUtil;
import ais.common.ConstantValues;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;

/**
 * 
 * @author fauzi
 */
public class PembayaranWithJenisBayarAction {

	private ReversalLogic reversalLogic = new ReversalLogic();
	private InqueryLogic inqueryLogic = new InqueryLogic();
	private PaymentLogic paymentLogic = new PaymentLogic();

	/**
	 * Sample method
	 */
	public String hello(String name) {
		return "Hello " + name;
	}

	public Response reversal(String nim, String nama,
			LogHostToHost logHostToHost, String nominalTagihan, String kode) {

		System.out.println(nama);
		BankHost bankHost = reversalLogic.pembayaranUtil.getBankHost();
		if (bankHost == null) {
			return CommonUtil.convertToResponse(reversalLogic.displayUtil
					.displayIpNotAllowed(logHostToHost, nim, bankHost, nama,
							ConstantUtil.REVERSAL));
		}
		Mahasiswa mahasiswa =  ConstantValues.ambilByNim(nim);
		if (mahasiswa == null) {
			// reversal untuk calon mahasiswa
			BiodataCalonMahasiswa biodataCalonMahasiswa = reversalLogic.pembayaranUtil
					.getCalonMahasiswaByNoUjian(nim);
			if (biodataCalonMahasiswa == null) {
				biodataCalonMahasiswa = reversalLogic.pembayaranUtil
						.getCalonMahasiswaByNoPendaftaran(nim);
				if (biodataCalonMahasiswa == null) {
					return CommonUtil
							.convertToResponse(reversalLogic.displayUtil
									.displayNimNotFound(logHostToHost, nim,
											bankHost, nama,
											ConstantUtil.REVERSAL));
				} else {

					nama = ("reversal calon mahasiswa dengan no pendaftaran = " + nim);
					return CommonUtil.convertToResponse(reversalLogic
							.reversalCalonMahasiswa(biodataCalonMahasiswa,
									bankHost, nama, logHostToHost,
									Double.parseDouble(nominalTagihan)));

				}
			} else {
				nama = ("reversal calon mahasiswa dengan no ujian = " + nim);
				return CommonUtil.convertToResponse(reversalLogic
						.reversalMahasiswaBaru(biodataCalonMahasiswa, bankHost,
								nama, logHostToHost,
								Double.parseDouble(nominalTagihan)));
			}
		} else {
			mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler"
					: mahasiswa.getProgram());
			return CommonUtil.convertToResponse(reversalLogic
					.reversalMahasiswaLama(mahasiswa, bankHost, nama,
							logHostToHost, Double.parseDouble(nominalTagihan),
							kode, null, nim));
		}
	}

	public Response inquery(String nim, String nama,
			LogHostToHost logHostToHost, String kode) {
		System.out.println(nama);
		BankHost bankHost = inqueryLogic.pembayaranUtil.getBankHost();
		if (bankHost == null) {
			return CommonUtil.convertToResponse(inqueryLogic.displayUtil
					.displayIpNotAllowed(logHostToHost, nim, bankHost, nama,
							ConstantUtil.INQUERY));
		}
		Mahasiswa mahasiswa =  ConstantValues.ambilByNim(nim);
		if (mahasiswa == null) {
			// inquery untuk calon mahasiswa
			BiodataCalonMahasiswa biodataCalonMahasiswa = inqueryLogic.pembayaranUtil
					.getCalonMahasiswaByNoUjian(nim);
			if (biodataCalonMahasiswa == null) {
				biodataCalonMahasiswa = inqueryLogic.pembayaranUtil
						.getCalonMahasiswaByNoPendaftaran(nim);
				if (biodataCalonMahasiswa == null) {
					return CommonUtil
							.convertToResponse(inqueryLogic.displayUtil
									.displayNimNotFound(logHostToHost, nim,
											bankHost, nama,
											ConstantUtil.INQUERY));
				} else {
					nama = ("inquiry calon mahasiswa dengan no pendaftaran = " + nim);
					return CommonUtil.convertToResponse(inqueryLogic
							.inqueryCalonMahasiswa(biodataCalonMahasiswa,
									bankHost, nama, logHostToHost));

				}
			} else {
				nama = ("inquiry calon mahasiswa dengan no ujian = " + nim);
				return CommonUtil.convertToResponse(inqueryLogic
						.inqueryMahasiswaBaru(biodataCalonMahasiswa, bankHost,
								nama, logHostToHost));
			}
		} else {
			mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler"
					: mahasiswa.getProgram());
			return CommonUtil.convertToResponse(inqueryLogic
					.inqueryMahasiswaLama(mahasiswa, bankHost, nama,
							logHostToHost, kode, null));
		}
	}

	public Response pay(String nim, String reffNumber, String tanggalBayar,
			String jamBayar, String userID, String namaCabang,
			String nominalTagihan, String nama, LogHostToHost logHostToHost,
			String kode) {
		System.out.println(nama);

		BankHost bankHost = paymentLogic.pembayaranUtil.getBankHost();
		if (bankHost == null) {
			return CommonUtil.convertToResponse(paymentLogic.displayUtil
					.displayIpNotAllowed(logHostToHost, nim, bankHost, nama,
							ConstantUtil.PAY));
		}

		Mahasiswa mahasiswa =  ConstantValues.ambilByNim(nim);
		if (mahasiswa == null) {
			// payment untuk calon mahasiswa
			BiodataCalonMahasiswa biodataCalonMahasiswa = inqueryLogic.pembayaranUtil
					.getCalonMahasiswaByNoUjian(nim);
			if (biodataCalonMahasiswa == null) {

				biodataCalonMahasiswa = inqueryLogic.pembayaranUtil
						.getCalonMahasiswaByNoPendaftaran(nim);
				if (biodataCalonMahasiswa == null) {
					return CommonUtil
							.convertToResponse(inqueryLogic.displayUtil
									.displayNimNotFound(logHostToHost, nim,
											bankHost, nama, ConstantUtil.PAY));
				} else {

					nama = ("pay calon mahasiswa dengan no pendaftaran = " + nim);
					return CommonUtil.convertToResponse(paymentLogic
							.pembayaranCalonMahasiswa(biodataCalonMahasiswa,
									bankHost, nama,
									Double.parseDouble(nominalTagihan),
									logHostToHost));

				}
			} else {
				nama = ("pay calon mahasiswa dengan no ujian = " + nim);
				return CommonUtil.convertToResponse(paymentLogic
						.pembayaranMahasiswaBaru(biodataCalonMahasiswa,
								bankHost, nama,
								Double.parseDouble(nominalTagihan),
								logHostToHost));
			}
		} else {
			mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler"
					: mahasiswa.getProgram());
			return CommonUtil.convertToResponse(paymentLogic
					.pembayaranMahasiswaLama(mahasiswa, bankHost, nama,
							Double.parseDouble(nominalTagihan), logHostToHost,
							kode, null, nim));
		}

	}
}
