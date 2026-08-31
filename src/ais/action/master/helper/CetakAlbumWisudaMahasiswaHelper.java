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

/**
 * Helper yang mencetak "Album Wisuda" (buku kumpulan profil wisudawan) berformat PDF untuk
 * seluruh mahasiswa seangkatan dengan user login saat ini yang telah mendaftar wisuda ({@link
 * PendaftaranWisuda}). Seluruh proses dijalankan langsung di konstruktor: mengambil mahasiswa
 * dari sesi ZK ({@code Sessions} attribute {@code "usersTemp"}), menyiapkan berkas foto tiap
 * wisudawan ke direktori sementara ({@link #generateImageAlbumWisudaMahasiswa}), mengumpulkan
 * data cetak per wisudawan ({@link #getDataAlbumWisudaMahasiswa}), lalu memanggil {@link
 * Report#generatePDFReport} dengan template {@code "Album_Wisuda_Mahasiswa"}.
 *
 * <p>
 * Data yang disiapkan tiap wisudawan meliputi identitas (NIM, nama, alamat, telepon, program
 * studi, fakultas), judul skripsi (diambil tanpa pengecekan {@code null} — bila mahasiswa dalam
 * angkatan tersebut belum/tidak punya record {@link Skripsi}, iterasi baris tersebut gagal
 * dengan {@code NullPointerException} yang ditangkap oleh catch umum sehingga proses berhenti di
 * baris itu dan mahasiswa setelahnya tidak ikut tercetak), foto, serta IPK yang dihitung manual
 * dari seluruh {@link Detailperkuliahan} bernilai (bukan hasil {@code singkronkanKrsMahasiswa}).
 * </p>
 */
public class CetakAlbumWisudaMahasiswaHelper {
	private Tbmuser users;
	private Mahasiswa mahasiswa;

	/**
	 * Mengambil mahasiswa dari sesi ZK saat ini, lalu langsung menjalankan seluruh proses cetak
	 * album wisuda (siapkan foto, kumpulkan data, hasilkan PDF) untuk seluruh wisudawan
	 * seangkatan dengan mahasiswa tersebut.
	 */
	public CetakAlbumWisudaMahasiswaHelper() throws Exception {
		users = (Tbmuser) Sessions.getCurrent().getAttribute("usersTemp");
		mahasiswa = users.getMahasiswa();

		final Map<String, Integer> parameters = new HashMap<String, Integer>();
		parameters.put("tahunangkatan", mahasiswa.getTahunangkatan());

		generateImageAlbumWisudaMahasiswa(mahasiswa.getTahunangkatan());
		List<Map<String, Serializable>> maps = getDataAlbumWisudaMahasiswa(mahasiswa.getTahunangkatan());

		Report.generatePDFReport("pdf", parameters, "Album_Wisuda_Mahasiswa", ais.ui.util.WaktuUtil.getDate(), maps);

	}

	/**
	 * Menyalin foto terbaru ({@link FotoMahasiswa}, diambil dari BLOB) tiap wisudawan angkatan
	 * {@code tahunangkatan} ke berkas fisik di direktori sementara aplikasi ({@code /tmp}), siap
	 * dirujuk oleh template laporan PDF. Kegagalan pada satu wisudawan (mis. tidak punya foto)
	 * dicatat sebagai pesan error ke konsol/admin dan tidak menghentikan wisudawan lainnya.
	 */
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

	/**
	 * Mengumpulkan data cetak (identitas, judul skripsi, foto, IPK) tiap wisudawan angkatan
	 * {@code tahunangkatan} menjadi satu {@link Map} per baris untuk template laporan. IPK
	 * dihitung manual sebagai &Sigma;(nilai&times;sks)/&Sigma;sks dari seluruh {@link
	 * Detailperkuliahan} bernilai milik mahasiswa. Catatan: bila salah satu wisudawan tidak
	 * memiliki record {@link Skripsi}, method ini berhenti pada baris tersebut (exception
	 * ditangkap oleh catch umum di luar loop) sehingga wisudawan berikutnya dalam angkatan yang
	 * sama tidak ikut termuat.
	 *
	 * @return daftar map data cetak, satu per wisudawan yang berhasil diproses sebelum kegagalan (bila ada)
	 */
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
