package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.model.SettingBiaya;
import ais.database.model.SettingBiayaDetail;
import ais.database.model.GeneralValueObject;

/** Memilih template umum/bulanan dengan memperhitungkan daftar mahasiswa terpilih. */
public final class SettingBiayaMahasiswaSelector {

	private SettingBiayaMahasiswaSelector() {
	}

	public static List<SettingBiaya> saringDanPrioritaskan(Session session, List<SettingBiaya> sumber, String nim) {
		List<SettingBiaya> terbatas = new ArrayList<SettingBiaya>();
		List<SettingBiaya> umum = new ArrayList<SettingBiaya>();
		if (sumber == null) {
			return umum;
		}
		for (SettingBiaya setting : sumber) {
			if (setting == null) {
				continue;
			}
			if (setting.getBatasiMahasiswaTertentu()) {
				if (terdaftar(session, setting, nim)) {
					terbatas.add(setting);
				}
			} else {
				umum.add(setting);
			}
		}
		// Untuk mahasiswa terpilih, setting khusus harus menang dari setting cohort umum.
		terbatas.addAll(umum);
		return terbatas;
	}

	/**
	 * Memilih kandidat berdasarkan prioritas terkecil yang masih mempunyai data
	 * cocok. Setiap kelompok prioritas tetap dinilai memakai pembobotan lama. Bila
	 * suatu kelompok prioritas tidak mempunyai kandidat yang memenuhi seluruh
	 * kondisi, pemeriksaan dilanjutkan ke prioritas berikutnya.
	 */
	public static SettingBiaya pilihSatuDenganPrioritas(List<SettingBiaya> sumber, String[] properties,
			Object[] datas) {
		if (sumber == null || sumber.isEmpty()) {
			return null;
		}
		Set<Integer> urutanPrioritas = new TreeSet<Integer>();
		for (SettingBiaya setting : sumber) {
			if (setting != null) {
				urutanPrioritas.add(setting.getPrioritas());
			}
		}
		for (Integer prioritas : urutanPrioritas) {
			List<SettingBiaya> satuPrioritas = new ArrayList<SettingBiaya>();
			for (SettingBiaya setting : sumber) {
				if (setting != null && setting.getPrioritas().equals(prioritas)) {
					satuPrioritas.add(setting);
				}
			}
			SettingBiaya terpilih = (SettingBiaya) GeneralValueObject.ambilSatuData(SettingBiaya.class,
					satuPrioritas, properties, datas);
			if (terpilih != null) {
				return terpilih;
			}
		}
		return null;
	}

	private static boolean terdaftar(Session session, SettingBiaya setting, String nim) {
		if (session == null || setting == null || setting.getId() == null || nim == null
				|| nim.trim().length() == 0) {
			return false;
		}
		Number jumlah = (Number) session.createCriteria(SettingBiayaDetail.class)
				.createAlias("settingBiaya", "settingBiaya")
				.createAlias("mahasiswa", "mahasiswa")
				.add(Restrictions.eq("settingBiaya.id", setting.getId()))
				.add(Restrictions.eq("mahasiswa.nim", nim.trim()))
				.setProjection(Projections.rowCount()).uniqueResult();
		return jumlah != null && jumlah.longValue() > 0L;
	}
}
