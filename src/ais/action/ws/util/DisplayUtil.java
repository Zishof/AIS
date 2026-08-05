/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.action.ws.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.LogHostToHost;
import ais.database.model.TunggakanMahasiswa;
import ws.billpayment.h2h.bankmandiri.util.CommonUtil;

/**
 * 
 * @author Fauzi
 */
public class DisplayUtil {

	public List<String[]> displayNoRegistrasiNotFound(
			LogHostToHost logHostToHost, String nim, BankHost bankHost,
			String nama, Integer transactionType) {
		return displayNimNotFound(logHostToHost, nim, bankHost, nama,
				transactionType);
	}

	public List<String[]> displayIpNotAllowed(LogHostToHost logHostToHost,
			String nim, BankHost bankHost, String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code", ConstantUtil.IP_NOT_ALLOWED });
		data.add(new String[] { "response_description",
				"IP tidak diperbolehkan untuk mengakses" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.IP_NOT_ALLOWED));
		logHostToHost
				.setResponseDescription("IP tidak diperbolehkan untuk mengakses");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayNimNotFound(LogHostToHost logHostToHost,
			String nim, BankHost bankHost, String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code", ConstantUtil.NIM_NOT_FOUND });
		data.add(new String[] {
				"response_description",
				"Mahasiswa atau calon mahasiswa dengan kode " + nim
						+ " tidak ditemukan" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.NIM_NOT_FOUND));
		logHostToHost
				.setResponseDescription("Mahasiswa atau calon mahasiswa dengan kode "
						+ nim + " tidak ditemukan");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayPembayaranTerlambat(
			LogHostToHost logHostToHost, String nim, BankHost bankHost,
			String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code", ConstantUtil.BILLS_HAS_EXPIRED });
		data.add(new String[] { "response_description",
				"Tidak ada jadwal pembayaran/pembayaran terlambat" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.BILLS_HAS_EXPIRED));
		logHostToHost
				.setResponseDescription("Tidak ada jadwal pembayaran/pembayaran terlambat");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayAlihProdi(LogHostToHost logHostToHost,
			String nim, BankHost bankHost, String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code", ConstantUtil.BILLS_HAS_EXPIRED });
		data.add(new String[] { "response_description",
				"Mahasiswa ini telah alih prodi" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.BILLS_HAS_EXPIRED));
		logHostToHost.setResponseDescription("Mahasiswa ini telah alih prodi");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayKodePembayaranTidakDitemukan(
			LogHostToHost logHostToHost, String nim, BankHost bankHost,
			String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code", ConstantUtil.BILLS_NOT_FOUND });
		data.add(new String[] { "response_description",
				"Kode pembayaran tidak ditemukan" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.BILLS_NOT_FOUND));
		logHostToHost.setResponseDescription("Kode pembayaran tidak ditemukan");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayReversalTidakDiaktifkan(
			LogHostToHost logHostToHost, String nim, BankHost bankHost,
			String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code",
				ConstantUtil.PAYMENT_OF_INADEQUATE });
		data.add(new String[] { "response_description",
				"Reversal tidak di-izinkan" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.PAYMENT_OF_INADEQUATE));
		logHostToHost.setResponseDescription("Reversal tidak di-izinkan");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayTunggakanMahasiswa(
			LogHostToHost logHostToHost, String nim, BankHost bankHost,
			String nama, Integer transactionType,
			List<TunggakanMahasiswa> tunggakanMahasiswas) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code", ConstantUtil.NOT_VALID_AMOUNT });
		data.add(new String[] {
				"response_description",
				"Mahasiswa ini masih memiliki tunggakan sbb: "
						+ tunggakanMahasiswas });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.NOT_VALID_AMOUNT));
		logHostToHost
				.setResponseDescription("Mahasiswa ini masih memiliki tunggakan sbb: "
						+ tunggakanMahasiswas);
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayKesalahanSistem(LogHostToHost logHostToHost,
			String nim, BankHost bankHost, String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code", ConstantUtil.NOT_VALID_AMOUNT });
		data.add(new String[] { "response_description",
				"Terjadi kesalahan sistem" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.NOT_VALID_AMOUNT));
		logHostToHost.setResponseDescription("Terjadi kesalahan sistem");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}

	public List<String[]> displayNominalTagihanTidakMencukupi(
			LogHostToHost logHostToHost, String nim, BankHost bankHost,
			String nama, Integer transactionType) {
		
		List<String[]> data = new ArrayList<String[]>();
		data.add(new String[] { "response_code",
				ConstantUtil.PAYMENT_OF_INADEQUATE });
		data.add(new String[] { "response_description",
				"Nominal Pembayaran Kurang dari Nominal Tagihan" });
		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(nim);
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		logHostToHost.setResponseCode((ConstantUtil.PAYMENT_OF_INADEQUATE));
		logHostToHost
				.setResponseDescription("Nominal Pembayaran Kurang dari Nominal Tagihan");
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(transactionType);
		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		return data;
	}
}
