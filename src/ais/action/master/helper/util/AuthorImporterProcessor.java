package ais.action.master.helper.util;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.library.util.BigFile;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.library.Pengarang;

public class AuthorImporterProcessor extends TimerTask {

	private String localIp = "";

	public AuthorImporterProcessor() {
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
		try {
			doProcess();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unused")
	private void doProcess() throws Exception {

		if (true) {
			return;
		}

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi("author_importer_processor", Konfigurasi.AKTIF,
				"/opt/ol_dump_authors_2012-03-31.txt", "", "");

		File myfile = new File(auto_proses_tunggakan.getInfo1() + "");
		System.out
				.println("================================ AuthorImporterProcessor ================================== "
						+ auto_proses_tunggakan.getNilai() + ", lokasi = " + myfile.getAbsolutePath() + ", exist = "
						+ myfile.exists());

		if (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF) && myfile.exists()) {

			BigFile file = new BigFile(myfile.getAbsolutePath());

			List<Pengarang> myPengarangs = new ArrayList<Pengarang>();
			for (String line : file) {

				try {
					String[] temp = line.split("	");
					String j = temp[temp.length - 1];
					JSONObject object = new JSONObject(j);

					String key = (object.get("key")+"").replaceAll("/authors/", "").trim();

					String revision = "";
					try {
						revision = (object.getString("revision"));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/AuthorImporterProcessor.java:79");
					}

					// System.out
					// .println("================================ check for key
					// "
					// + key
					// + " ================================== ");

					Session session = HibernateUtil.currentNativeSession();

					Integer count = ((Number) session.createCriteria(Pengarang.class).add(Restrictions.eq("kode", key))
							.add(Restrictions.eq("revisi", revision)).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

					if (count.equals(0)) {
						// Thread.sleep(100);
						Pengarang pengarang = (Pengarang) (session.createCriteria(Pengarang.class)
								.add(Restrictions.eq("kode", key)).setMaxResults(1).uniqueResult());

						if (pengarang == null) {
							pengarang = new Pengarang();
						}
						pengarang.setAktif(true);

						String personal_name = "";
						String name = "";

						try {
							personal_name = (object.getString("personal_name"));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/AuthorImporterProcessor.java:109");
						}
						try {
							name = (object.getString("name"));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/AuthorImporterProcessor.java:113");
						}
						pengarang.setKode(key);
						pengarang.setNama(name == null || name.trim().equals("") ? personal_name : name.trim());
						pengarang.setKeterangan(personal_name);

						if (pengarang.getRevisi() == null || !pengarang.getRevisi().equals(revision)) {
							pengarang.setRevisi(revision);
							myPengarangs.add(pengarang);
							System.out.println("Save or update pengarang => " + pengarang + " - ID => "
									+ pengarang.getId() + ", key => " + pengarang.getKode() + ", myPengarangs => "
									+ myPengarangs.size());
						}

						if (myPengarangs.size() >= 1000) {
							session.getTransaction().begin();
							for (Pengarang myPengarang : myPengarangs) {
								session.saveOrUpdate(myPengarang);
							}
							session.getTransaction().commit();
							myPengarangs = null;
							myPengarangs = new ArrayList<Pengarang>();
						}
					}

					HibernateUtil.closeSession();

				} catch (Exception e) {
					HibernateUtil.rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			for (Pengarang myPengarang : myPengarangs) {
				session.saveOrUpdate(myPengarang);
			}
			session.getTransaction().commit();

			HibernateUtil.closeSession();

		}
	}

	public static void main(String[] argv) {
		String key = "/authors/OL1667738A".replaceAll("/authors/", "").trim();
		System.out.println(key);
	}

}
