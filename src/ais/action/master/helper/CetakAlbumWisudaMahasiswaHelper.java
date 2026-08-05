package ais.action.master.helper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoMahasiswa;

public class CetakAlbumWisudaMahasiswaHelper {
	private Tbmuser users;
	private Mahasiswa mahasiswa;

	public CetakAlbumWisudaMahasiswaHelper() throws Exception {
		users = (Tbmuser) Sessions.getCurrent().getAttribute("usersTemp");
		mahasiswa = users.getMahasiswa();

		final Map<String, Integer> parameters = new HashMap<String, Integer>();
		parameters.put("tahunangkatan", mahasiswa.getTahunangkatan());

		generateImageAlbumWisudaMahasiswa(mahasiswa.getTahunangkatan());
		List<Map<String, Serializable>> maps = getDataAlbumWisudaMahasiswa(mahasiswa.getTahunangkatan());

		Report.generatePDFReport("pdf", parameters, "Album_Wisuda_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings("unchecked")
	public void generateImageAlbumWisudaMahasiswa(Integer tahunangkatan) throws Exception {
		Session session = HibernateUtil.currentSession();

		List<PendaftaranWisuda> listPendaftaranWisuda = session.createCriteria(PendaftaranWisuda.class)
				.createCriteria("mahasiswa").addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				.add(Restrictions.eq("tahunangkatan", tahunangkatan)).list();

		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			Iterator<?> itr = listPendaftaranWisuda.iterator();

			while (itr.hasNext()) {
				PendaftaranWisuda beanPendaftaranWisuda = (PendaftaranWisuda) itr.next();
				FotoMahasiswa fotobm = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
						.addOrder(Order.desc("id"))
						.add(Restrictions.eq("mahasiswa", beanPendaftaranWisuda.getMahasiswa().getId()))
						.setMaxResults(1).uniqueResult();
				// Blob content = fotobm.getFoto();

				try {
					File blobFile = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/tmp/") + "/" + fotobm.getNama());
					CommonMedia.getFileFotoDenganFile(fotobm, blobFile);

					// FileOutputStream outStream = new
					// FileOutputStream(blobFile);
					// InputStream inStream = content.// getBinaryStream();
					//
					// int length = -1;
					// int size = 4096;
					// byte[] buffer = new byte[size];
					//
					// while ((length = inStream.read(buffer)) != -1) {
					// outStream.write(buffer, 0, length);
					// outStream.flush();
					// }
					//
					// inStream.close();
					// outStream.close();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					System.out.println("ERROR(djv_exportBlob) Unable to export: " + fotobm.getNama());
				}

			}
			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Map<String, Serializable>> getDataAlbumWisudaMahasiswa(Integer tahunangkatan) {

		Session session = HibernateUtil.currentSession();
		List<PendaftaranWisuda> listPendaftaranWisuda = session.createCriteria(PendaftaranWisuda.class)
				.createCriteria("mahasiswa").addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
				.add(Restrictions.eq("tahunangkatan", tahunangkatan)).list();

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {

			while (itr.hasNext()) {
				PendaftaranWisuda beanPendaftaranWisuda = (PendaftaranWisuda) itr.next();
				Map map = new java.util.HashMap<String, Serializable>();

				Common.insertProperty(PendaftaranWisuda.class, beanPendaftaranWisuda, map, "");

				map.put("nim", beanPendaftaranWisuda.getMahasiswa().getNim());
				map.put("nama", beanPendaftaranWisuda.getMahasiswa().getNama());
				map.put("alamat", beanPendaftaranWisuda.getMahasiswa().getAlamat());
				map.put("telp", beanPendaftaranWisuda.getMahasiswa().getTelp());
				map.put("program_studi", beanPendaftaranWisuda.getMahasiswa().getJenjang().getNama());
				map.put("fakultas", beanPendaftaranWisuda.getMahasiswa().getJurusan().getFakultas().getNama());

				Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
						.add(Restrictions.eq("mahasiswa", beanPendaftaranWisuda.getMahasiswa())).uniqueResult();
				map.put("judul_skripsi", skripsi.getJudul());

				beanPendaftaranWisuda.getMahasiswa().putPhoto(map);

				List<Detailperkuliahan> listDetailPerkuliahan = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.isNull("ikutiPerkuliahan")).add(Restrictions.ne("nilaiHuruf", ""))
						.add(Restrictions.eq("mahasiswa", beanPendaftaranWisuda.getMahasiswa())).list();

				Double sumNilaixSks = 0.0;
				Integer sks = 0;
				Iterator<?> itr2 = listDetailPerkuliahan.iterator();
				while (itr2.hasNext()) {
					Detailperkuliahan beanDetailperkuliahan = (Detailperkuliahan) itr2.next();
					sumNilaixSks += beanDetailperkuliahan.getTotalIP()
							* beanDetailperkuliahan.getPerkuliahan().getMatakuliah().getSks();
					sks += beanDetailperkuliahan.getPerkuliahan().getMatakuliah().getSks();
				}

				map.put("ipk", sumNilaixSks / sks);
				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return maps;
	}

}
