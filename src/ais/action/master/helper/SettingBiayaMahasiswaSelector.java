package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
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
		// Begitu ada setting yang memang membatasi mahasiswa dan NIM ini terdaftar,
		// jangan masukkan setting cohort umum lagi. Mencampur keduanya membuat angka
		// prioritas setting umum dapat mengambil alih tagihan khusus mahasiswa.
		return terbatas.isEmpty() ? umum : terbatas;
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
			// GeneralValueObject mempertahankan kandidat pertama bila skor spesifikasinya
			// sama. Urutkan eksplisit agar hasil stabil: setting TA terbaru, lalu ID terbaru.
			Collections.sort(satuPrioritas, new Comparator<SettingBiaya>() {
				@Override
				public int compare(SettingBiaya kiri, SettingBiaya kanan) {
					int bandingTa = bandingTurun(kiri == null ? null : kiri.getTa(),
							kanan == null ? null : kanan.getTa());
					return bandingTa != 0 ? bandingTa
							: bandingTurun(kiri == null ? null : kiri.getId(),
									kanan == null ? null : kanan.getId());
				}
			});
			SettingBiaya terpilih = (SettingBiaya) GeneralValueObject.ambilSatuData(SettingBiaya.class,
					satuPrioritas, properties, datas);
			if (terpilih != null) {
				return terpilih;
			}
		}
		return null;
	}

	private static int bandingTurun(Comparable kiri, Comparable kanan) {
		if (kiri == kanan) {
			return 0;
		}
		if (kiri == null) {
			return 1;
		}
		if (kanan == null) {
			return -1;
		}
		return kanan.compareTo(kiri);
	}

	private static boolean terdaftar(Session session, SettingBiaya setting, String nim) {
		if (session == null || setting == null || setting.getId() == null || nim == null
				|| nim.trim().length() == 0) {
			return false;
		}
		Number jumlah = (Number) session.createCriteria(SettingBiayaDetail.class)
				.createAlias("settingBiaya", "settingBiaya")
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)
				.add(Restrictions.eq("settingBiaya.id", setting.getId()))
				.add(Restrictions.or(Restrictions.eq("mahasiswa.nim", nim.trim()),
						Restrictions.eq("calonMahasiswa.nim", nim.trim())))
				.setProjection(Projections.rowCount()).uniqueResult();
		return jumlah != null && jumlah.longValue() > 0L;
	}
}
