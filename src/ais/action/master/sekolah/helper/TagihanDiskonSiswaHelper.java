package ais.action.master.sekolah.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DiskonSiswa;
import ais.database.model.sekolah.DiskonSiswaItemBiaya;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;

/**
 * Helper sinkronisasi diskon tagihan siswa/calon siswa.
 *
 * TagihanUtil dan TagihanUtilCalonSiswa memiliki alur yang sama saat membaca
 * DiskonSiswa. Helper ini sengaja tidak memakai ConstantValues.simpleList(...)
 * agar object yang diproses tetap berada pada session aktif dan tidak berubah
 * menjadi proxy/cached object yang sudah detach.
 */
public final class TagihanDiskonSiswaHelper {

	private TagihanDiskonSiswaHelper() {
	}

	public static int sinkronkanDiskon(List<Tagihan> tagihans) {
		if (tagihans == null || tagihans.isEmpty()) {
			return 0;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			return sinkronkanDiskon(session, tagihans);
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:54");
			}
			return 0;
		} finally {
			closeSession(session);
		}
	}

	public static int sinkronkanDiskon(Session session, List<Tagihan> tagihans) {
		if (session == null || tagihans == null || tagihans.isEmpty()) {
			return 0;
		}

		int jumlahBerubah = 0;
		for (int i = 0; i < tagihans.size(); i++) {
			Tagihan tagihan = tagihans.get(i);
			try {
				if (sinkronkanSatuTagihan(session, tagihan)) {
					jumlahBerubah++;
				}
			} catch (Exception e) {
				rollbackIfActive(session);
				try {
					Common.tampilErrorJikaAdmin(e);
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:78");
				}
			}
		}
		return jumlahBerubah;
	}

	private static boolean sinkronkanSatuTagihan(Session session, Tagihan tagihanSource) throws Exception {
		if (tagihanSource == null) {
			return false;
		}

		Long tagihanId = safeId(tagihanSource);
		Tagihan tagihan = tagihanSource;
		if (tagihanId != null) {
			Tagihan managed = (Tagihan) session.get(Tagihan.class, tagihanId);
			if (managed != null) {
				tagihan = managed;
			}
		}

		NominalBiaya nominalBiaya = tagihan.getNominalBiaya();
		if (nominalBiaya == null || tagihan.getBayarKe() == null || nominalBiaya.getDibayarSebayak() == null
				|| tagihan.getBayarKe().intValue() > nominalBiaya.getDibayarSebayak().intValue()) {
			return false;
		}

		PengaturanBiaya pengaturanBiaya = tagihan.getPengaturanBiaya();
		ItemBiayaSekolah itemBiayaSekolah = tagihan.getItemBiayaSekolah();
		Long itemBiayaId = safeId(itemBiayaSekolah);
		if (pengaturanBiaya == null || itemBiayaId == null || !pengaturanBiaya.checkAdaItemBiaya(itemBiayaSekolah)) {
			return false;
		}

		DiskonHitung hasil = hitungDiskon(session, tagihan, itemBiayaId);
		Double nominalAwal = tagihan.getNominal() == null ? Double.valueOf(0.0) : tagihan.getNominal();
		Double nominalDiskon = Boolean.TRUE.equals(hasil.persen)
				? Double.valueOf(nominalAwal.doubleValue() * (hasil.totalDiskon.doubleValue() / 100.0))
				: hasil.totalDiskon;

		Double currentDiskon = tagihan.getDiskonTidakLangsung() == null ? Double.valueOf(0.0)
				: tagihan.getDiskonTidakLangsung();
		Long currentDiskonId = safeId(tagihan.getDiskonSiswa());
		Long newDiskonId = safeId(hasil.diskonSiswa);

		boolean berubah = !equalsLong(currentDiskonId, newDiskonId)
				|| currentDiskon.intValue() != nominalDiskon.intValue();
		if (!berubah) {
			copyDiskonToSource(tagihanSource, tagihan);
			return false;
		}

		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			tagihan.setDiskon(nominalDiskon);
			tagihan.setDiskonTidakLangsung(nominalDiskon);
			tagihan.setDiskonSiswa(hasil.diskonSiswa);
			Common.refreshUpdate(session, tagihan);
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:142");
				}
			}
			throw e;
		}

		copyDiskonToSource(tagihanSource, tagihan);
		return true;
	}

	@SuppressWarnings("unchecked")
	private static DiskonHitung hitungDiskon(Session session, Tagihan tagihan, Long itemBiayaId) {
		DiskonHitung hasil = new DiskonHitung();
		Long siswaId = safeId(tagihan.getSiswa());
		Long calonSiswaId = safeId(tagihan.getCalonSiswa());
		if (siswaId == null && calonSiswaId == null) {
			return hasil;
		}

		Criteria criteria = session.createCriteria(DiskonSiswaPunyaSiswa.class)
				.addOrder(Order.desc("id")).setMaxResults(1).createAlias("diskonSiswa", "diskonSiswa")
				.createAlias("siswa", "dpsSiswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "dpsCalonSiswa", Criteria.LEFT_JOIN)
				.add(Restrictions.ilike("diskonSiswa.itemBiaya", "," + itemBiayaId + ",",
						org.hibernate.criterion.MatchMode.ANYWHERE))
				.add(Restrictions.eq("diskonSiswa.tahunAjaran", tagihan.getTahunAjaran()))
				.add(Restrictions.eq("setujui", Boolean.TRUE))
				.add(Restrictions.eq("diskonSiswa.aktif", Boolean.TRUE));

		if (siswaId != null && calonSiswaId != null) {
			criteria.add(Restrictions.or(Restrictions.eq("dpsSiswa.id", siswaId),
					Restrictions.eq("dpsCalonSiswa.id", calonSiswaId)));
		} else if (siswaId != null) {
			criteria.add(Restrictions.eq("dpsSiswa.id", siswaId));
		} else {
			criteria.add(Restrictions.eq("dpsCalonSiswa.id", calonSiswaId));
		}

		List<DiskonSiswaPunyaSiswa> daftar = criteria.list();
		if (daftar == null || daftar.isEmpty()) {
			return hasil;
		}

		for (int i = 0; i < daftar.size(); i++) {
			DiskonSiswaPunyaSiswa punyaSiswa = daftar.get(i);
			DiskonSiswa diskonSiswa = loadDiskonSiswa(session, punyaSiswa == null ? null : punyaSiswa.getDiskonSiswa());
			if (diskonSiswa == null || diskonSiswa.getId() == null) {
				continue;
			}

			hasil.persen = diskonSiswa.getMenggunkanPersen();
			Number nilai = (Number) session.createCriteria(DiskonSiswaItemBiaya.class)
					.createAlias("diskonSiswa", "diskonSiswa")
					.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
					.add(Restrictions.eq("diskonSiswa.id", diskonSiswa.getId()))
					.add(Restrictions.eq("itemBiayaSekolah.id", itemBiayaId))
					.setProjection(Projections.sum("defaultBiaya")).uniqueResult();

			double val = nilai == null ? 0.0 : nilai.doubleValue();
			if (val > 0.1) {
				hasil.diskonSiswa = diskonSiswa;
			}
			hasil.totalDiskon = Double.valueOf(hasil.totalDiskon.doubleValue() + val);
		}
		return hasil;
	}

	private static DiskonSiswa loadDiskonSiswa(Session session, DiskonSiswa diskonSiswa) {
		Long id = safeId(diskonSiswa);
		if (id == null) {
			return null;
		}
		Object managed = session.get(DiskonSiswa.class, id);
		return managed instanceof DiskonSiswa ? (DiskonSiswa) managed : diskonSiswa;
	}

	public static boolean diskonTidakMemotongTagihan(Tagihan tagihan) {
		DiskonSiswa diskonSiswa = tagihan == null ? null : tagihan.getDiskonSiswa();
		if (diskonSiswa == null || safeId(diskonSiswa) == null) {
			return false;
		}
		try {
			return !Boolean.TRUE.equals(diskonSiswa.getMemotongTagihan());
		} catch (Exception e) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				DiskonSiswa managed = (DiskonSiswa) session.get(DiskonSiswa.class, safeId(diskonSiswa));
				return managed != null && !Boolean.TRUE.equals(managed.getMemotongTagihan());
			} catch (Exception ignored) {
				return false;
			} finally {
				closeSession(session);
			}
		}
	}

	private static void copyDiskonToSource(Tagihan source, Tagihan managed) {
		if (source == null || managed == null || source == managed) {
			return;
		}
		try {
			source.setDiskon(managed.getDiskon());
			source.setDiskonTidakLangsung(managed.getDiskonTidakLangsung());
			source.setDiskonSiswa(managed.getDiskonSiswa());
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:247");
		}
	}

	private static Long safeId(Object value) {
		if (value == null) {
			return null;
		}
		try {
			if (value instanceof HibernateProxy) {
				LazyInitializer initializer = ((HibernateProxy) value).getHibernateLazyInitializer();
				if (initializer != null && initializer.getIdentifier() instanceof Number) {
					return Long.valueOf(((Number) initializer.getIdentifier()).longValue());
				}
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:262");
		}
		try {
			if (value instanceof GeneralValueObject) {
				return ((GeneralValueObject) value).getId();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:268");
		}
		try {
			if (value instanceof Siswa) {
				return ((Siswa) value).getId();
			}
			if (value instanceof CalonSiswa) {
				return ((CalonSiswa) value).getId();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:277");
		}
		return null;
	}

	private static boolean equalsLong(Long a, Long b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	private static void rollbackIfActive(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:297");
		}
	}

	private static void closeSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:307");
		}
		try {
			session.disconnect();
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:311");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/TagihanDiskonSiswaHelper.java:317");
		}
	}

	/**
	 * Tipe implementasi bersarang {@link DiskonHitung} milik {@link TagihanDiskonSiswaHelper}. Kelas ini memberi
	 * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
	 * TagihanDiskonSiswaHelper}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
	 * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Boolean persen}, {@code Double
	 * totalDiskon}, {@code DiskonSiswa diskonSiswa}. Aturan bisnis bersama tetap berada pada kelas induk atau
	 * service yang dipanggilnya.</p>
	 *
	 * @see TagihanDiskonSiswaHelper
	 */
	private static class DiskonHitung {
		Boolean persen = Boolean.TRUE;
		Double totalDiskon = Double.valueOf(0.0);
		DiskonSiswa diskonSiswa;
	}
}
