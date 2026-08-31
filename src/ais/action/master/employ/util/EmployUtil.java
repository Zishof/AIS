package ais.action.master.employ.util;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Utilitas modul employ untuk menjembatani sesi login dari aplikasi luar (portal Liferay, dilihat
 * dari pengecekan {@code "default@liferay.com"}) ke sesi AIS: meresolusi/membuat {@link Tbmuser}
 * berdasarkan parameter {@code email}/{@code userId} yang dikirim portal, mengaitkannya ke
 * {@link Pegawai}, dan menaruh hasilnya di {@code Sessions}/request sebagai {@code "usersTemp"}
 * (pola auto-login yang sama dipakai action lain yang membaca sesi tersebut).
 */
public class EmployUtil {

	/**
	 * Mengautentikasi/menyediakan-otomatis user dari parameter portal luar {@code email}/{@code userId}
	 * pada {@code execution}. Menampilkan pesan HTML "harus login" ke {@code page} dan mengembalikan
	 * {@code null} bila parameter {@code email} tidak dikirim/masih default portal; menampilkan
	 * pesan "belum terhubung ke data pegawai" dan mengembalikan {@code null} bila user ditemukan/
	 * dibuat tapi tidak berelasi {@link Pegawai} apa pun.
	 *
	 * <p>
	 * Bila {@link Tbmuser} belum ada untuk {@link Pegawai} yang cocok emailnya, satu baris baru
	 * dibuat otomatis dengan role {@link Tbmrole#PEGAWAI}, akses root, dan PASSWORD DISET SAMA
	 * DENGAN EMAIL dalam bentuk tidak terenkripsi ({@code is_encripted=false}) — pola auto-provisioning
	 * ini mengasumsikan otentikasi sesungguhnya sudah dilakukan oleh portal luar (Liferay), bukan
	 * oleh AIS sendiri.
	 * </p>
	 *
	 * @param execution konteks eksekusi ZK yang membawa parameter dari portal luar
	 * @param page      halaman ZK tempat pesan error ditampilkan bila gagal
	 * @param namaModul nama modul yang ditampilkan pada pesan "harus login"
	 * @return {@link Pegawai} yang berhasil diautentikasi, atau {@code null} bila gagal (pesan sudah ditampilkan ke {@code page})
	 */
	public Pegawai initLoginFromOutApp(Execution execution, Page page, String namaModul) {
		String email = execution.getParameter("email") == null ? "-11111111" : execution.getParameter("email");
		String userId = execution.getParameter("userId") == null ? "-11111111" : execution.getParameter("userId");

		if (email.equals("default@liferay.com") || execution.getParameter("email") == null) {
			page.getFirstRoot().appendChild(new ais.ui.util.MyHtml(
					"<font style=\"font-family: serif;font-size: x-large;color: blue;\">Anda harus login terlebih dahulu untuk melihat "
							+ namaModul + ".</font>"));
			return null;
		}

		Session session = HibernateUtil.currentSession();
		Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.eq("userId", userId), Restrictions.eq("email", email)))
				.setMaxResults(1).uniqueResult();
		Pegawai pegawai = null;
		if (tbmuser == null || tbmuser.getUserId() == null) {
			pegawai = (Pegawai) session.createCriteria(Pegawai.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.eq("email", email)).setMaxResults(1).uniqueResult();
			if (pegawai != null) {
				tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("pegawai", pegawai))
						.setMaxResults(1).uniqueResult();
				if (tbmuser == null || tbmuser.getUserId() == null) {
					tbmuser = new Tbmuser();
					tbmuser.setPegawai(pegawai);
					tbmuser.setUserId(email);
					tbmuser.setUserNama(pegawai.getNama());
					tbmuser.setEmail(email);
					tbmuser.setIs_encripted(false);
					tbmuser.setRoot(true);
					tbmuser.setSatuanKerja(pegawai.getSatuanKerja());
					tbmuser.setUserRole(new Tbmrole(Tbmrole.PEGAWAI));
					tbmuser.setUserPassword(email);
					tbmuser.setUserShow(1);
					session.save(tbmuser);
				}
			}
		}

		if (pegawai == null) {
			pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
		}

		if (pegawai == null) {
			page.getFirstRoot().appendChild(new ais.ui.util.MyHtml(
					"<font style=\"font-family: serif;font-size: xx-large;color: red;\">Akun anda "
							+ (execution.getParameter("email") == null ? "" : "(" + email + ")")
							+ " belum terhubung ke data pegawai. Harap segera menghubungi administrator untuk menghubungkan data anda ke data pegawai.</font></body></html"));
			return null;
		}
		if (Common.getCurrentUser() == null) {
			Sessions.getCurrent().setAttribute("usersTemp", tbmuser);
			((HttpServletRequest) execution.getNativeRequest()).setAttribute("usersTemp", tbmuser);
		}

		return pegawai;
	}

}
