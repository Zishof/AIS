package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.DiskonSiswaItemBiaya;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;

/**
 * Helper sinkronisasi diskon siswa.
 *
 * Dibuat terpisah agar tombol sinkronisasi di DiskonSiswaAction dan
 * DiskonSiswaPunyaSiswaHelper memakai logika yang sama. Proses ini sengaja
 * memakai openSession() terisolasi agar tidak membawa entity/proxy dari session UI
 * ke session lain.
 */
public class DiskonSiswaSyncHelper {

	private DiskonSiswaSyncHelper() {
	}

	public static int sinkronkanBanyak(List diskonIds, boolean kirimKePembayaran) {
		return sinkronkanBanyak(diskonIds, kirimKePembayaran, null);
	}

	public static int sinkronkanBanyak(List diskonIds, boolean kirimKePembayaran, ais.common.LaporanUpload laporan) {
		if (diskonIds == null || diskonIds.isEmpty()) {
			return 0;
		}

		int total = 0;
		for (int i = 0; i < diskonIds.size(); i++) {
			Long id = toLong(diskonIds.get(i));
			String kunci = "diskonSiswa id=" + id;
			if (id != null) {
				try {
					int jumlah = sinkronkan(id, kirimKePembayaran, false);
					total += jumlah;
					if (laporan != null) {
						laporan.catatBerhasil(i, kunci, "Sinkronisasi berhasil (" + jumlah + " tagihan diperbarui)");
					}
				} catch (Exception e) {
					if (laporan != null) {
						laporan.catatGagalDetail(i, kunci, e);
					}
				}
			} else if (laporan != null) {
				laporan.catatDilewati(i, kunci, "id diskon tidak valid");
			}
		}
		return total;
	}

	public static int sinkronkan(DiskonSiswa diskonSiswa, boolean kirimKePembayaran) {
		return sinkronkan(diskonSiswa == null ? null : diskonSiswa.getId(), kirimKePembayaran);
	}

	public static int sinkronkan(Long diskonSiswaId, boolean kirimKePembayaran) {
		return sinkronkan(diskonSiswaId, kirimKePembayaran, true);
	}

	@SuppressWarnings("unchecked")
	private static int sinkronkan(Long diskonSiswaId, boolean kirimKePembayaran, boolean tangkapErrorDiSini) {
		if (diskonSiswaId == null) {
			return 0;
		}

		Session session = null;
		int jumlahBerubah = 0;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			DiskonSiswa diskonSiswa = (DiskonSiswa) session.get(DiskonSiswa.class, diskonSiswaId);
			if (diskonSiswa == null) {
				return 0;
			}

			List<DiskonSiswaItemBiaya> itemBiayas = loadItemBiaya(session, diskonSiswa);
			jumlahBerubah += updateItemBiayaCache(session, diskonSiswa, itemBiayas);

			List<DiskonSiswaPunyaSiswa> penerimaDiskon = session.createCriteria(DiskonSiswaPunyaSiswa.class)
					.add(Restrictions.eq("setujui", Boolean.TRUE)).add(Restrictions.eq("diskonSiswa", diskonSiswa))
					.list();

			for (int i = 0; i < penerimaDiskon.size(); i++) {
				DiskonSiswaPunyaSiswa punyaSiswa = penerimaDiskon.get(i);
				jumlahBerubah += sinkronkanPenerima(session, diskonSiswa, punyaSiswa, itemBiayas, kirimKePembayaran);
			}
		} catch (Exception e) {
			if (tangkapErrorDiSini) {
				try {
					Common.tampilErrorJikaAdmin(e);
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:84");
				}
			} else {
				closeSession(session);
				session = null;
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		} finally {
			closeSession(session);
		}
		return jumlahBerubah;
	}

	@SuppressWarnings("unchecked")
	private static List<DiskonSiswaItemBiaya> loadItemBiaya(Session session, DiskonSiswa diskonSiswa) {
		if (session == null || diskonSiswa == null) {
			return new ArrayList<DiskonSiswaItemBiaya>();
		}

		return session.createCriteria(DiskonSiswaItemBiaya.class).createAlias("itemBiayaSekolah", "itemBiayaSekolah")
				.addOrder(Order.asc("itemBiayaSekolah.id"))
				.add(Restrictions.or(Restrictions.isNull("itemBiayaSekolah.aktif"),
						Restrictions.eq("itemBiayaSekolah.aktif", Boolean.TRUE)))
				.add(Restrictions.eq("diskonSiswa", diskonSiswa)).list();
	}

	private static int updateItemBiayaCache(Session session, DiskonSiswa diskonSiswa,
			List<DiskonSiswaItemBiaya> itemBiayas) {
		String diskonItem = buildItemBiayaString(itemBiayas);
		String sekarang = diskonSiswa.getItemBiaya();
		if (sekarang == null) {
			sekarang = "";
		}
		if (sekarang.equalsIgnoreCase(diskonItem)) {
			return 0;
		}

		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			diskonSiswa.setItemBiaya(diskonItem);
			Common.refreshUpdate(session, diskonSiswa);
			tx.commit();
			return 1;
		} catch (Exception e) {
			rollback(tx);
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:127");
			}
			return 0;
		}
	}

	private static int sinkronkanPenerima(Session session, DiskonSiswa diskonSiswa,
			DiskonSiswaPunyaSiswa punyaSiswa, List<DiskonSiswaItemBiaya> itemBiayas, boolean kirimKePembayaran) {
		if (session == null || diskonSiswa == null || punyaSiswa == null || itemBiayas == null) {
			return 0;
		}

		Boolean persen = diskonSiswa.getMenggunkanPersen();
		Siswa siswa = punyaSiswa.getSiswa();
		CalonSiswa calonSiswa = punyaSiswa.getCalonSiswa();

		int jumlahBerubah = 0;
		for (int i = 0; i < itemBiayas.size(); i++) {
			DiskonSiswaItemBiaya item = itemBiayas.get(i);
			if (item == null || item.getItemBiayaSekolah() == null) {
				continue;
			}

			Criteria criteria = session.createCriteria(Tagihan.class)
					.add(Restrictions.eq("tahunAjaran", diskonSiswa.getTahunAjaran()))
					.add(Restrictions.eq("itemBiayaSekolah", item.getItemBiayaSekolah()))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
			if (siswa != null) {
				criteria.add(Restrictions.eq("siswa", siswa));
			} else if (calonSiswa != null) {
				criteria.add(Restrictions.eq("calonSiswa", calonSiswa));
			} else {
				criteria.add(Restrictions.sqlRestriction("false"));
			}

			List tagihanList = criteria.list();
			for (int j = 0; j < tagihanList.size(); j++) {
				Tagihan tagihan = (Tagihan) tagihanList.get(j);
				if (!bolehDiproses(tagihan)) {
					continue;
				}

				Double diskon = item.getDefaultBiaya() == null ? Double.valueOf(0.0) : item.getDefaultBiaya();
				Double nominal = tagihan.getNominal() == null ? Double.valueOf(0.0) : tagihan.getNominal();
				Double nominalDiskon = Boolean.TRUE.equals(persen) ? Double.valueOf(nominal.doubleValue() * diskon.doubleValue() / 100.0)
						: diskon;

				if (perluUpdate(tagihan, diskonSiswa, nominalDiskon)) {
					jumlahBerubah += updateTagihan(session, tagihan, diskonSiswa, nominalDiskon);
				}

				if (kirimKePembayaran && TagihanDiskonSiswaHelper.diskonTidakMemotongTagihan(tagihan)) {
					try {
						DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
					} catch (Exception e) {
						try {
							Common.tampilErrorJikaAdmin(e);
						} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:184");
						}
					}
				}
			}
		}
		return jumlahBerubah;
	}

	private static boolean bolehDiproses(Tagihan tagihan) {
		try {
			if (tagihan == null) {
				return false;
			}
			if (!Boolean.TRUE.equals(tagihan.getAktif())) {
				return false;
			}
			if (tagihan.ambilBukanTagihanData()) {
				return false;
			}
			return tagihan.getNominalBiaya() == null || !Boolean.TRUE.equals(tagihan.getNominalBiaya().getBukanTagihan());
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean perluUpdate(Tagihan tagihan, DiskonSiswa diskonSiswa, Double nominalDiskon) {
		Long diskonLamaId = null;
		try {
			diskonLamaId = tagihan.getDiskonSiswa() == null ? null : tagihan.getDiskonSiswa().getId();
		} catch (Exception e) {
			diskonLamaId = null;
		}

		Long diskonBaruId = diskonSiswa == null ? null : diskonSiswa.getId();
		Double lama = tagihan.getDiskonTidakLangsung() == null ? Double.valueOf(0.0) : tagihan.getDiskonTidakLangsung();
		Double baru = nominalDiskon == null ? Double.valueOf(0.0) : nominalDiskon;

		if (diskonBaruId == null && diskonLamaId != null) {
			return true;
		}
		if (diskonBaruId != null && !diskonBaruId.equals(diskonLamaId)) {
			return true;
		}
		return lama.intValue() != baru.intValue();
	}

	private static int updateTagihan(Session session, Tagihan tagihan, DiskonSiswa diskonSiswa, Double nominalDiskon) {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			tagihan.setDiskon(nominalDiskon);
			tagihan.setDiskonTidakLangsung(nominalDiskon);
			tagihan.setDiskonSiswa(diskonSiswa);
			Common.refreshUpdate(session, tagihan);
			tx.commit();
			return 1;
		} catch (Exception e) {
			rollback(tx);
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:245");
			}
		}
		return 0;
	}

	private static String buildItemBiayaString(List<DiskonSiswaItemBiaya> itemBiayas) {
		StringBuilder sb = new StringBuilder();
		if (itemBiayas == null) {
			return "";
		}
		for (int i = 0; i < itemBiayas.size(); i++) {
			DiskonSiswaItemBiaya item = itemBiayas.get(i);
			if (item != null && item.getItemBiayaSekolah() != null && item.getItemBiayaSekolah().getId() != null) {
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append(item.getItemBiayaSekolah().getId());
			}
		}
		return sb.toString();
	}

	private static Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Long) {
			return (Long) value;
		}
		if (value instanceof Number) {
			return Long.valueOf(((Number) value).longValue());
		}
		try {
			return Long.valueOf(value.toString());
		} catch (Exception e) {
			return null;
		}
	}

	private static void rollback(Transaction tx) {
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:290");
		}
	}

	private static void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:300");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:304");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/DiskonSiswaSyncHelper.java:310");
		}
	}
}
