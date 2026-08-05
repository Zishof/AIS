package ais.action.master.helper.util;

import java.net.InetAddress;
import java.util.TimerTask;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;

public class FotoMahasiswaSyncrhonizerProcessor extends TimerTask {

	private String localIp = "";

	public FotoMahasiswaSyncrhonizerProcessor() {
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

	private void doProcess() {

		Konfigurasi foto_mahasiswa_syncrhonizer = Common.getKonfigurasi(
				"foto_mahasiswa_syncrhonizer", Konfigurasi.TIDAK_AKTIF);

		boolean ketemuIp = (foto_mahasiswa_syncrhonizer.getInfo1() != null && foto_mahasiswa_syncrhonizer
				.getInfo1().trim().equals(localIp.trim()))
				|| (foto_mahasiswa_syncrhonizer.getInfo2() != null && foto_mahasiswa_syncrhonizer
						.getInfo2().trim().equals(localIp.trim()))
				|| (foto_mahasiswa_syncrhonizer.getInfo3() != null && foto_mahasiswa_syncrhonizer
						.getInfo3().trim().equals(localIp.trim()));

		System.out
				.println("IP Ketemu untuk FotoMahasiswaSyncrhonizerProcessor ==> "
						+ ketemuIp);

		if (foto_mahasiswa_syncrhonizer.getNilai().equals(Konfigurasi.AKTIF)
				&& ketemuIp) {

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();

			Konfigurasi foto_mahasiswa_syncrhonizer_clean = Common
					.getKonfigurasi("foto_mahasiswa_syncrhonizer_clean",
							Konfigurasi.TIDAK_AKTIF);

			if (foto_mahasiswa_syncrhonizer_clean.getNilai().equals(
					Konfigurasi.AKTIF)) {
				String sql = "delete from foto_mahasiswa;";
				System.out.println(sql);
				session.createSQLQuery(sql).executeUpdate();
			}

			String sql = "INSERT INTO foto_mahasiswa(nama, keterangan, mahasiswa, foto) "
					+ "select 'foto_mahasiswa' as foto_mahasiswa, 'image/png', mahasiswa, max(foto) foto from "
					+ "( "
					+ "	select a.mahasiswa,max(b.foto) as foto "
					+ "	from biodata_mahasiswa a "
					+ "	left join foto_biodata_mahasiswa b on (a.id = b.biodata_mahasiswa) "
					+ "	where b.foto is not null "
					+ "	group by a.mahasiswa, foto_utama "
					+ "	order by (case when foto_utama is null then false else foto_utama end) desc  "
					+ ") a "
					+ "where mahasiswa not in (select mahasiswa from foto_mahasiswa) "
					+ "group by mahasiswa;";
			System.out.println(sql);
			session.createSQLQuery(sql).executeUpdate();

			sql = "INSERT INTO foto_mahasiswa(nama, keterangan, mahasiswa, foto) "
					+ "select 'foto_mahasiswa' as foto_mahasiswa, 'image/png', b.id, a.foto "
					+ "from biodata_calon_mahasiswa a  "
					+ "inner join mahasiswa b on (trim(a.nim) = trim(b.nim)) "
					+ "where a.foto is not null "
					+ "and b.id not in (select mahasiswa from foto_mahasiswa);";
			System.out.println(sql);
			session.createSQLQuery(sql).executeUpdate();

			sql = "delete from foto_mahasiswa where foto is null;";
			System.out.println(sql);
			session.createSQLQuery(sql).executeUpdate();

			session.getTransaction().commit();
			
			HibernateUtil.closeSession();

		}
	}

}
