package ais.action.master.helper.util;

import java.net.InetAddress;
import java.util.TimerTask;

import org.hibernate.Session;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;

/**
 * Tugas terjadwal ({@link TimerTask}) yang menyinkronkan tabel {@code foto_mahasiswa} (foto profil
 * tunggal per mahasiswa, dipakai untuk tampilan ringkas di seluruh aplikasi) dari dua sumber data
 * mentah: {@code biodata_mahasiswa}/{@code foto_biodata_mahasiswa} (mengambil foto utama, atau bila
 * tidak ada foto mana pun yang tersedia) dan {@code biodata_calon_mahasiswa} (foto dari data
 * pendaftaran PMB yang dicocokkan lewat NIM). Hanya menyisipkan baris untuk mahasiswa yang belum
 * punya baris {@code foto_mahasiswa} sama sekali (idempoten terhadap eksekusi berulang) dan
 * membersihkan baris yang akhirnya tetap tanpa foto.
 *
 * <p>
 * Proses ini sengaja dibatasi hanya berjalan pada satu/beberapa server tertentu dalam klaster untuk
 * menghindari duplikasi kerja: aktif hanya bila konfigurasi {@code foto_mahasiswa_syncrhonizer}
 * bernilai {@link Konfigurasi#AKTIF} DAN hostname mesin saat ini ({@link #localIp}, sebenarnya
 * hostname bukan alamat IP walau namanya menyiratkan demikian) cocok dengan salah satu dari tiga
 * slot host yang diizinkan ({@code info1}/{@code info2}/{@code info3} pada baris konfigurasi
 * tersebut). Konfigurasi tambahan {@code foto_mahasiswa_syncrhonizer_clean} (bila AKTIF)
 * mengosongkan seluruh tabel {@code foto_mahasiswa} sebelum sinkronisasi ulang dari awal —
 * digunakan untuk memaksa build ulang penuh, bukan mode normal sehari-hari.
 * </p>
 */
public class FotoMahasiswaSyncrhonizerProcessor extends TimerTask {

	private String localIp = "";

	/** Merekam hostname server saat ini ke {@link #localIp}, dipakai {@link #run()} untuk menentukan apakah proses ini boleh berjalan pada server ini. */
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

	/** Titik masuk {@link TimerTask}: memanggil {@link #doProcess()} yang melakukan seluruh pengecekan gerbang (konfigurasi aktif + host cocok) dan sinkronisasi foto. */
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
