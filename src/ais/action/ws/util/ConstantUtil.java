/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ais.action.ws.util;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.JenisKegiatan;

/**
 * 
 * @author Fauzi
 */
public class ConstantUtil {

	public static final String SUCCESS = "00";
	public static final String IP_NOT_ALLOWED = "01";
	public static final String NIM_NOT_FOUND = "02";
	public static final String BILLS_HAVE_BEEN_PAID = "03";
	public static final String BILLS_HAS_EXPIRED = "04";
	public static final String PAYMENT_OF_INADEQUATE = "05";
	public static final String BILLS_NOT_FOUND = "06";
	public static final String NOT_VALID_AMOUNT = "07";

	public static final String PEMBAYARAN_PENDAFTARAN_ULANG = "100";
	public static final String PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA = "101";
	public static final String PEMBAYARAN_PENDAFTARAN_WISUDA = "102";
	public static final String PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU = "103";

	public static String PENDAFTARAN_MAHASISWA_LAMA = "Daftar Ulang";
	public static String PENDAFTARAN_CALON_MAHASISWA = "Pendaftaran Calon Mahasiswa";
	public static String PENDAFTARAN_WISUDA = "Pendaftaran Wisuda";
	public static String PENDAFTARAN_ULANG_MAHASISWA_BARU = "Daftar Ulang Mahasiswa Baru";
	public static String PEMBAYARAN_SP = "Pembayaran SP";

	public static void initIstilahPendaftaran(Session session) {
		PENDAFTARAN_MAHASISWA_LAMA = Common
				.getKonfigurasi(session, "ISTILAH_PENDAFTARAN_MAHASISWA_LAMA", "Daftar Ulang").getNilai().trim();
		PENDAFTARAN_CALON_MAHASISWA = Common
				.getKonfigurasi(session, "ISTILAH_PENDAFTARAN_CALON_MAHASISWA", "Pendaftaran Calon Mahasiswa")
				.getNilai().trim();
		PENDAFTARAN_WISUDA = Common.getKonfigurasi(session, "ISTILAH_PENDAFTARAN_WISUDA", "Pendaftaran Wisuda")
				.getNilai().trim();
		PENDAFTARAN_ULANG_MAHASISWA_BARU = Common
				.getKonfigurasi(session, "PENDAFTARAN_ULANG_MAHASISWA_BARU", "Daftar Ulang Mahasiswa Baru").getNilai()
				.trim();
		PEMBAYARAN_SP = Common.getKonfigurasi(session, "ISTILAH_PEMBAYARAN_SP", "Pembayaran SP").getNilai().trim();
		
		
		ConstantValues.
		PENDAFTARAN_MAHASISWA_LAMA = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				// .add(Restrictions.or(Restrictions.isNull("aktif"),
				// Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("namaKegiatan", ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA)).setMaxResults(1)
				.uniqueResult();

		JenisKegiatan aktifJenisPembayaran = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				.add(Restrictions.eq("defaultPembayaran", true)).setMaxResults(1).uniqueResult();
		if (aktifJenisPembayaran != null) {
			ConstantValues.PENDAFTARAN_MAHASISWA_LAMA = aktifJenisPembayaran;
		}

		ConstantValues.	PENDAFTARAN_CALON_MAHASISWA = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				// .add(Restrictions.or(Restrictions.isNull("aktif"),
				// Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("namaKegiatan", ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)).setMaxResults(1)
				.uniqueResult();

		ConstantValues.	PENDAFTARAN_WISUDA = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				// .add(Restrictions.or(Restrictions.isNull("aktif"),
				// Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("namaKegiatan", ConstantUtil.PENDAFTARAN_WISUDA)).setMaxResults(1)
				.uniqueResult();

		ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				// .add(Restrictions.or(Restrictions.isNull("aktif"),
				// Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("namaKegiatan", ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU))
				.setMaxResults(1).uniqueResult();

		ConstantValues.	PEMBAYARAN_SP = (JenisKegiatan) session.createCriteria(JenisKegiatan.class)
				// .add(Restrictions.or(Restrictions.isNull("aktif"),
				// Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("namaKegiatan", ConstantUtil.PEMBAYARAN_SP)).setMaxResults(1)
				.uniqueResult();

		if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA == null) {
			ConstantValues.PENDAFTARAN_CALON_MAHASISWA = new JenisKegiatan();
			ConstantValues.PENDAFTARAN_CALON_MAHASISWA.setNamaKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);
			ConstantValues.PENDAFTARAN_CALON_MAHASISWA.setDefaultKegiatan(true);
			// session.getTransaction().begin();
			session.save(ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
			// session.getTransaction().commit();
		}
		if (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU == null) {
			ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU = new JenisKegiatan();
			ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.setDefaultKegiatan(true);
			ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.setNamaKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
			// session.getTransaction().begin();
			session.save(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
			// session.getTransaction().commit();
		}
		if (ConstantValues.PENDAFTARAN_MAHASISWA_LAMA == null) {
			ConstantValues.PENDAFTARAN_MAHASISWA_LAMA = new JenisKegiatan();
			ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.setDefaultKegiatan(true);
			ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.setNamaKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
			// session.getTransaction().begin();
			session.save(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
			// session.getTransaction().commit();
		}

		if (ConstantValues.PEMBAYARAN_SP == null) {
			ConstantValues.PEMBAYARAN_SP = new JenisKegiatan();
			ConstantValues.PEMBAYARAN_SP.setDefaultKegiatan(true);
			ConstantValues.PEMBAYARAN_SP.setKode("009");
			ConstantValues.PEMBAYARAN_SP.setNamaKegiatan(ConstantUtil.PEMBAYARAN_SP);
			// session.getTransaction().begin();
			session.save(ConstantValues.PEMBAYARAN_SP);
			// session.getTransaction().commit();
		}

		if (ConstantValues.PENDAFTARAN_WISUDA == null) {
			ConstantValues.PENDAFTARAN_WISUDA = new JenisKegiatan();
			ConstantValues.PENDAFTARAN_WISUDA.setDefaultKegiatan(true);
			ConstantValues.PENDAFTARAN_WISUDA.setNamaKegiatan(ConstantUtil.PENDAFTARAN_WISUDA);
			// session.getTransaction().begin();
			session.save(ConstantValues.PENDAFTARAN_WISUDA);
			// session.getTransaction().commit();
		}

		

	}

	public static final String PEMBAYARAN_MAHASISWA_LAINNYA = "Pembayaran Mahasiswa Lainnya";
	public static final String PEMBAYARAN_CALON_MAHASISWA_LAINNYA = "Pembayaran Calon Mahasiswa Lainnya";

	public static final Integer INQUERY = 1;
	public static final Integer PAY = 2;
	public static final Integer REVERSAL = 3;
}
