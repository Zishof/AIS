package ais.action.master.helper.obe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Matakuliah;
import ais.database.model.Pertemuan;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ProfilLulusan;
import ais.ui.util.MyWindow;

/**
 * Pemilih data OBE untuk agenda perkuliahan.
 *
 * <p>Helper ini sengaja tidak membuka halaman ZUL master. Pemilih menerima
 * mata kuliah dari pertemuan aktif sehingga daftar PL, CPL, dan CPMK selalu
 * dikunci pada program studi mata kuliah tersebut.</p>
 */
public final class AmbilDataBanyakObeAgendaHelper {

	public static final String JENIS_PROFIL_LULUSAN = "PL";
	public static final String JENIS_CAPAIAN_LULUSAN = "CPL";
	public static final String JENIS_CPMK = "CPMK";

	private AmbilDataBanyakObeAgendaHelper() {
	}

	public static void buka(Component induk, Pertemuan pertemuan, String jenis,
			EventListener selesai) throws Exception {
		Matakuliah matakuliah = ambilMatakuliah(pertemuan);
		if (matakuliah == null || matakuliah.getJurusan() == null) {
			throw new IllegalStateException(
					"Program studi mata kuliah belum ditentukan. Lengkapi data mata kuliah terlebih dahulu.");
		}

		if (JENIS_PROFIL_LULUSAN.equals(jenis)) {
			bukaProfilLulusan(induk, matakuliah, selesai);
		} else if (JENIS_CAPAIAN_LULUSAN.equals(jenis)) {
			bukaCapaianLulusan(induk, matakuliah, selesai);
		} else if (JENIS_CPMK.equals(jenis)) {
			bukaCpmk(induk, matakuliah, selesai);
		} else {
			throw new IllegalArgumentException("Jenis data OBE tidak dikenali: " + jenis);
		}
	}

	private static Matakuliah ambilMatakuliah(Pertemuan pertemuan) {
		if (pertemuan == null || pertemuan.getPerkuliahan() == null) {
			return null;
		}
		return pertemuan.getPerkuliahan().getMatakuliah();
	}

	private static void bukaProfilLulusan(Component induk, final Matakuliah matakuliah,
			final EventListener selesai) throws Exception {
		List<ProfilLulusan> tersimpan = ambilProfilLulusan(matakuliah.getProfilLulusan());
		final AmbilDataProfilLulusanBanyak popup = new AmbilDataProfilLulusanBanyak(tersimpan,
				matakuliah.getJurusan());
		popup.setEventListener(new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				List<ProfilLulusan> dipilih = (List<ProfilLulusan>) event.getData();
				Set<Long> ids = parseIds(matakuliah.getProfilLulusan());
				if (dipilih != null) {
					for (ProfilLulusan data : dipilih) {
						if (data != null && data.getId() != null) {
							ids.add(data.getId());
						}
					}
				}
				matakuliah.setProfilLulusan(gabungIds(ids));
				simpanDanMuatUlang(induk, matakuliah, selesai);
			}
		});
		tampilkanPopup(popup, "Pilih Profil Lulusan");
	}

	private static void bukaCapaianLulusan(Component induk, final Matakuliah matakuliah,
			final EventListener selesai) throws Exception {
		List<CapaianLulusan> tersimpan = ambilCapaianLulusan(matakuliah.getCapaianLulusan());
		final AmbilDataCapaianLulusanBanyak popup = new AmbilDataCapaianLulusanBanyak(tersimpan,
				matakuliah.getJurusan(), matakuliah);
		popup.setEventListener(new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				List<CapaianLulusan> dipilih = (List<CapaianLulusan>) event.getData();
				Set<Long> ids = parseIds(matakuliah.getCapaianLulusan());
				if (dipilih != null) {
					for (CapaianLulusan data : dipilih) {
						if (data != null && data.getId() != null) {
							ids.add(data.getId());
						}
					}
				}
				matakuliah.setCapaianLulusan(gabungIds(ids));
				simpanDanMuatUlang(induk, matakuliah, selesai);
			}
		});
		tampilkanPopup(popup, "Pilih Capaian Lulusan (CPL)");
	}

	private static void bukaCpmk(Component induk, final Matakuliah matakuliah,
			final EventListener selesai) throws Exception {
		List<CapaianPembelajaranLulusan> tersimpan = ambilCpmk(
				matakuliah.getCapaianPembelajaranLulusan());
		final AmbilDataCapaianPembelajaranLulusanBanyak popup = new AmbilDataCapaianPembelajaranLulusanBanyak(
				tersimpan, matakuliah.getJurusan(), matakuliah);
		popup.setEventListener(new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				List<CapaianPembelajaranLulusan> dipilih = (List<CapaianPembelajaranLulusan>) event.getData();
				Set<Long> ids = parseIds(matakuliah.getCapaianPembelajaranLulusan());
				if (dipilih != null) {
					for (CapaianPembelajaranLulusan data : dipilih) {
						if (data != null && data.getId() != null) {
							ids.add(data.getId());
						}
					}
				}
				matakuliah.setCapaianPembelajaranLulusan(gabungIds(ids));
				simpanDanMuatUlang(induk, matakuliah, selesai);
			}
		});
		tampilkanPopup(popup, "Pilih CPMK");
	}

	private static List<ProfilLulusan> ambilProfilLulusan(String nilai) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<Long> ids = parseIds(nilai);
			if (ids.isEmpty()) {
				return new ArrayList<ProfilLulusan>();
			}
			return ConstantValues.simpleList(session.createCriteria(ProfilLulusan.class)
					.add(Restrictions.in("id", ids)).addOrder(Order.asc("kode")), ProfilLulusan.class);
		} finally {
			tutupSession(session);
		}
	}

	private static List<CapaianLulusan> ambilCapaianLulusan(String nilai) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<Long> ids = parseIds(nilai);
			if (ids.isEmpty()) {
				return new ArrayList<CapaianLulusan>();
			}
			return ConstantValues.simpleList(session.createCriteria(CapaianLulusan.class)
					.add(Restrictions.in("id", ids)).addOrder(Order.asc("kode")), CapaianLulusan.class);
		} finally {
			tutupSession(session);
		}
	}

	private static List<CapaianPembelajaranLulusan> ambilCpmk(String nilai) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Set<Long> ids = parseIds(nilai);
			if (ids.isEmpty()) {
				return new ArrayList<CapaianPembelajaranLulusan>();
			}
			return ConstantValues.simpleList(session.createCriteria(CapaianPembelajaranLulusan.class)
					.add(Restrictions.in("id", ids)).addOrder(Order.asc("kode")),
					CapaianPembelajaranLulusan.class);
		} finally {
			tutupSession(session);
		}
	}

	private static void tampilkanPopup(MyWindow popup, String judul) throws Exception {
		popup.setTitle(judul);
		popup.setWidth("850px");
		popup.setHeight("95%");
		popup.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		popup.onModal();
	}

	private static void simpanDanMuatUlang(Component induk, Matakuliah matakuliah,
			EventListener selesai) throws Exception {
		Common.refreshUpdate(matakuliah);
		if (selesai != null) {
			selesai.onEvent(new Event("onObeDataChanged", induk, matakuliah));
		}
	}

	private static Set<Long> parseIds(String nilai) {
		Set<Long> hasil = new LinkedHashSet<Long>();
		if (nilai == null || nilai.trim().length() == 0) {
			return hasil;
		}
		String[] bagian = nilai.split(",");
		for (String item : bagian) {
			try {
				String bersih = item == null ? "" : item.trim();
				if (bersih.length() > 0) {
					hasil.add(Long.valueOf(bersih));
				}
			} catch (NumberFormatException e) {
				ais.common.ErrorAuditUtil.record(e,
						"AmbilDataBanyakObeAgendaHelper.parseIds nilai=" + item);
			}
		}
		return hasil;
	}

	private static String gabungIds(Set<Long> ids) {
		StringBuilder hasil = new StringBuilder();
		for (Long id : ids) {
			if (id == null) {
				continue;
			}
			if (hasil.length() > 0) {
				hasil.append(',');
			}
			hasil.append(id.longValue());
		}
		return hasil.toString();
	}

	private static void tutupSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "AmbilDataBanyakObeAgendaHelper.clear");
		}
		try {
			session.disconnect();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "AmbilDataBanyakObeAgendaHelper.disconnect");
		}
		try {
			session.close();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "AmbilDataBanyakObeAgendaHelper.close");
		}
	}
}
