package ais.action.master.helper.util;

import java.net.InetAddress;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.RadiusHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.radius.Radcheck;

public class RadiusProcessor extends TimerTask {

	private String localIp = "";

	public RadiusProcessor() {
		InetAddress thisIp;
		try {
			thisIp = InetAddress.getLocalHost();
			localIp = thisIp.getHostName();
			System.out.println("IP:" + localIp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e); 
		}

	}

	@Override
	public void run() {
		doProcess();
	}

	@SuppressWarnings("unchecked")
	private void doProcess() {

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi(
				"radius_syncrhonizer", Konfigurasi.AKTIF, "116.66.206.181",
				"171.27.27.10", "172.27.18.181");

		boolean ketemuIp = (auto_proses_tunggakan.getInfo1() != null && auto_proses_tunggakan
				.getInfo1().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo2() != null && auto_proses_tunggakan
						.getInfo2().trim().equals(localIp.trim()))
				|| (auto_proses_tunggakan.getInfo3() != null && auto_proses_tunggakan
						.getInfo3().trim().equals(localIp.trim()));

		System.out.println("IP Ketemu untuk RadiusProcessor ==> " + ketemuIp);

		if (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF)
				&& ketemuIp) {

			System.out.println("=========== RADIUS PROCESS ==========");

			Session session = HibernateUtil.currentNativeSession();
			Session radiusSession = RadiusHibernateUtil.getInstance()
					.currentSession();

			ProjectionList projectionList = Projections.projectionList();
			projectionList.add(Projections.property("userId"));
			projectionList.add(Projections.property("userPassword"));

			List<Object[]> tbmusers = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(projectionList).list();

			System.out.println("=========== PROSES " + tbmusers.size()
					+ " data pengguna ==========");

			for (Object[] tbmuser : tbmusers) {

				String userId = tbmuser[0] == null ? "" : tbmuser[0].toString();
				String userPassword = tbmuser[1] == null ? "" : tbmuser[1]
						.toString();

				if (userId == null || userId.trim().equals("")) {
					continue;
				}
				String pass = "";
				try {
					pass = Common.desEncrypter.get().decrypt(userPassword);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

				Integer count = ((Number) radiusSession
						.createCriteria(Radcheck.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("value", pass))
						.add(Restrictions.eq("kodeUniq", tbmuser.getClass()
								.getName() + "_users_" + userId))
						.setMaxResults(1).uniqueResult()).intValue();

				if (count.equals(0)) {
					Radcheck radcheck = (Radcheck) radiusSession
							.createCriteria(Radcheck.class)
							.add(Restrictions.eq("kodeUniq", tbmuser.getClass()
									.getName() + "_users_" + userId))
							.setMaxResults(1).uniqueResult();
					System.out.println("radcheck baru / ganti password ==> "
							+ radcheck);
					if (radcheck == null) {
						radcheck = new Radcheck();
					}

					if (radcheck.getValue() == null
							|| radcheck.getValue().trim().equals("")
							|| !radcheck.getValue().equals(pass)) {
						radcheck.setAttribute("Password");
						radcheck.setKodeUniq(tbmuser.getClass().getName()
								+ "_users_" + userId);
						radcheck.setOp("==");
						radcheck.setUsername(userId.trim());
						radcheck.setValue(pass);
						radiusSession.getTransaction().begin();
						radiusSession.saveOrUpdate(radcheck);
						radiusSession.getTransaction().commit();
					}
					radcheck = null;
				}
			}

			tbmusers = null;

			projectionList = Projections.projectionList();
			projectionList.add(Projections.property("id"));
			projectionList.add(Projections.property("nim"));
			projectionList.add(Projections.property("pass"));
			projectionList.add(Projections.property("userOrtu"));
			projectionList.add(Projections.property("passOrtu"));

			List<Object[]> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(projectionList).list();

			System.out.println("=========== PROSES " + mahasiswas.size()
					+ " data mahasiswa ==========");

			for (Object[] mahasiswa : mahasiswas) {

				Object id = mahasiswa[0];
				Object nim = mahasiswa[1];
				Object mypass = mahasiswa[2];
				Object userOrtu = mahasiswa[3];
				Object passOrtu = mahasiswa[4];

				if (nim == null || nim.toString().trim().equals("")) {
					continue;
				}

				String pass = "";
				try {
					pass = Common.desEncrypter.get().decrypt(mypass.toString());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}
				Integer count = ((Number) radiusSession
						.createCriteria(Radcheck.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("value", pass))
						.add(Restrictions.eq("kodeUniq", mahasiswa.getClass()
								.getName() + "_" + id)).setMaxResults(1)
						.uniqueResult()).intValue();

				if (count.equals(0)) {

					Radcheck radcheck = (Radcheck) radiusSession
							.createCriteria(Radcheck.class)
							.add(Restrictions.eq("kodeUniq", mahasiswa
									.getClass().getName() + "_" + id))
							.setMaxResults(1).uniqueResult();
					System.out.println("radcheck baru / ganti password ==> "
							+ radcheck);
					if (radcheck == null) {
						radcheck = new Radcheck();
					}

					if (radcheck.getValue() == null
							|| radcheck.getValue().trim().equals("")
							|| !radcheck.getValue().equals(pass)) {
						radcheck.setAttribute("Password");
						radcheck.setKodeUniq(mahasiswa.getClass().getName()
								+ "_" + id);
						radcheck.setOp("==");
						radcheck.setUsername(nim.toString().trim());
						radcheck.setValue(pass + "");
						radiusSession.getTransaction().begin();
						radiusSession.saveOrUpdate(radcheck);
						radiusSession.getTransaction().commit();
					}
					radcheck = null;
				}

				if (userOrtu != null && !userOrtu.toString().trim().equals("")) {
					pass = "";
					try {
						pass = Common.desEncrypter.get().decrypt(passOrtu.toString());
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}

					count = ((Number) radiusSession
							.createCriteria(Radcheck.class)
							.setProjection(Projections.rowCount())
							.add(Restrictions.eq("value", pass))
							.add(Restrictions.eq("kodeUniq", mahasiswa
									.getClass().getName() + "_ortu_" + id))
							.setMaxResults(1).uniqueResult()).intValue();

					if (count.equals(0)) {
						Radcheck radcheck = (Radcheck) radiusSession
								.createCriteria(Radcheck.class)
								.add(Restrictions.eq("kodeUniq", mahasiswa
										.getClass().getName() + "_ortu_" + id))
								.setMaxResults(1).uniqueResult();
						System.out
								.println("radcheck baru / ganti password ==> "
										+ radcheck);
						if (radcheck == null) {
							radcheck = new Radcheck();
						}
						if (radcheck.getValue() == null
								|| radcheck.getValue().trim().equals("")
								|| !radcheck.getValue().equals(pass)) {
							radcheck.setAttribute("Password");
							radcheck.setKodeUniq(mahasiswa.getClass().getName()
									+ "_ortu_" + id);
							radcheck.setOp("==");
							radcheck.setUsername(userOrtu.toString().trim());
							radcheck.setValue(pass);
							radiusSession.getTransaction().begin();
							radiusSession.saveOrUpdate(radcheck);
							radiusSession.getTransaction().commit();
						}
						radcheck = null;
					}
				}
			}

			mahasiswas = null;

			
			HibernateUtil.closeSession();
			RadiusHibernateUtil.getInstance().closeSession();

		}
	}
}
