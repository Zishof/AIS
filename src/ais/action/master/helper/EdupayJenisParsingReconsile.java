package ais.action.master.helper;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.library.util.BigFile;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.LogHostToHost;
import ais.database.model.RekonsiliasiHostToHost;
import ais.database.model.file.LampiranLain;

public class EdupayJenisParsingReconsile implements JenisParsingReconsile {

	private SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

	@Override
	public void parsing(LampiranLain lampiranLain,
			JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost)
			throws Exception {
		// TODO Auto-generated method stub
		Session session = HibernateUtil.currentNativeSession();

		BigFile bigFile = new BigFile(lampiranLain.ambilFile().getAbsolutePath());
		Iterator<String> iterator = bigFile.iterator();
		while (iterator.hasNext()) {
			String data = iterator.next();
			String[] s = data.split(";");
			RekonsiliasiHostToHost rekonsiliasiHostToHost = (RekonsiliasiHostToHost) session
					.createCriteria(RekonsiliasiHostToHost.class)
					.add(Restrictions.eq("keterangan", data)).setMaxResults(1)
					.uniqueResult();
			if (rekonsiliasiHostToHost == null) {
				rekonsiliasiHostToHost = new RekonsiliasiHostToHost();
			}
			rekonsiliasiHostToHost.setKeterangan(data);
			rekonsiliasiHostToHost
					.setJenisRekonsiliasiHostToHost(jenisRekonsiliasiHostToHost);
			rekonsiliasiHostToHost.setLampiranId(lampiranLain.getId());

			try {
				rekonsiliasiHostToHost.setWaktu(format.parse(s[1].trim()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost.setKode(s[6].trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost.setNama(s[10].trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost
						.setNilai(Double.parseDouble(s[9].trim()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost
						.setStatus(Integer.parseInt(s[11].trim()) == 1 ? RekonsiliasiHostToHost.SUKSES
								: RekonsiliasiHostToHost.GAGAL);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			String nim = rekonsiliasiHostToHost.getKode().substring(0,
					rekonsiliasiHostToHost.getKode().length() - 2);

			LogHostToHost logHostToHost = (LogHostToHost) session
					.createCriteria(LogHostToHost.class)
					.add(Restrictions.eq("responseCode", "00"))
					.add(Restrictions.eq("transactionType", ConstantUtil.PAY))
					.add(Restrictions.eq("kode",
							rekonsiliasiHostToHost.getKode()))
					.add(Restrictions
							.sqlRestriction("DATE(this_.tanggal) = DATE('"
									+ Common.databaseDateFormat.get()
											.format(rekonsiliasiHostToHost
													.getWaktu()) + "')"))
					.setMaxResults(1).uniqueResult();

			rekonsiliasiHostToHost.setLogHostToHost(logHostToHost);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, rekonsiliasiHostToHost);
			session.getTransaction().commit();

			if (logHostToHost != null) {
				logHostToHost.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logHostToHost);
				session.getTransaction().commit();

				List<CicilanPembayaran> cicilanPembayarans = Common
						.ambilCicilanPembayarans(session, logHostToHost,
								rekonsiliasiHostToHost.getKode(), nim,
								rekonsiliasiHostToHost.getWaktu());

				if (rekonsiliasiHostToHost.getStatus() != null
						&& rekonsiliasiHostToHost.getStatus().equals(
								RekonsiliasiHostToHost.SUKSES)) {

					if (!cicilanPembayarans.isEmpty()) {
						for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
							cicilanPembayaran
									.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);
							session.getTransaction().begin();
							Common.refreshUpdate(session, cicilanPembayaran);
							session.getTransaction().commit();
						}
					} else {

						List<CicilanPembayaranGagal> cicilanPembayaranGagals = Common
								.ambilCicilanPembayaranGagals(session,
										logHostToHost,
										rekonsiliasiHostToHost.getKode(), nim,
										rekonsiliasiHostToHost.getWaktu());

						for (CicilanPembayaranGagal cicilanPembayaranGagal : cicilanPembayaranGagals) {
							cicilanPembayaranGagal
									.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);

							CicilanPembayaran cicilanPembayaran = Common
									.copyCicilanPembayaranKeSukses(cicilanPembayaranGagal);

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(cicilanPembayaran);
							session.getTransaction().commit();

							session.getTransaction().begin();
							session.createSQLQuery(
									"delete from cicilan_pembayaran_gagal where id="
											+ cicilanPembayaranGagal.getId())
									.executeUpdate();
							session.getTransaction().commit();
						}

					}
				} else {
					for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
						cicilanPembayaran
								.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);

						CicilanPembayaranGagal cicilanPembayaranGagal = Common
								.copyCicilanPembayaranKeGagal(cicilanPembayaran);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session,
								cicilanPembayaranGagal);
						session.getTransaction().commit();

						session.getTransaction().begin();
						session.createSQLQuery(
								"delete from cicilan_pembayaran where id="
										+ cicilanPembayaran.getId())
								.executeUpdate();
						session.getTransaction().commit();

					}
				}
			}
		}

		HibernateUtil.closeSession();
	}
}
