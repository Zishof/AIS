package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.ui.util.MyMessageboxConfig;

public class PustakaUtil {

	@SuppressWarnings("unchecked")
	public static boolean checkPeminjamaBuku(Mahasiswa mahasiswa) throws Exception {
		if (Common.bolehKonfigurasi("apakah_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat_sebanyak_beberapa_hari", Konfigurasi.TIDAK_AKTIF)) {

			int jumlahHari = 100;
			try {
				jumlahHari = Integer.parseInt(Common.getKonfigurasi(
						"jumlah_hari_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat",
						"100").getNilai().trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			Session session = HibernateUtil.currentNativeSession();
			String content = "";

			try {
				List<PeminjamanPengadaanItemDetail> objects = session
						.createCriteria(PeminjamanPengadaanItemDetail.class)
						.add(Restrictions.isNull("kembaliPengadaanItemDetail"))
						.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
						.createAlias("peminjamanPengadaanItem.anggota", "anggota")
						.add(Restrictions.eq("anggota.mahasiswa", mahasiswa)).list();

				for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : objects) {
					if (jumlahHari <= peminjamanPengadaanItemDetail.getJumlahHariTerlambat()) {
						content += "Item atau buku \"" + peminjamanPengadaanItemDetail.getItem().getNama()
								+ "\" terlambat "
								+ Common.numberFormat.get().format(peminjamanPengadaanItemDetail.getJumlahHariTerlambat())
								+ " hari di perpustakaan "
								+ peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getPerpustakaan().getNama()
								+ ".\n";
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PustakaUtil.java:54");
			}
			HibernateUtil.closeSession();

			if (!content.trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Maaf, Anda tidak bisa masuk ke sistem karena alasan belum mengembalikan buku sbb :\n\n"
								+ content,
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method
								// stub

								Clients.showBusy("Loading ...");

								Common.goLogoff();
							}
						});

				return false;
			}
		}
		return true;
	}

}
