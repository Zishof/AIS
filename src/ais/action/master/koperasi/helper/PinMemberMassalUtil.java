package ais.action.master.koperasi.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.servlet.api.KantinHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;

/**
 * Impor/ekspor aman PIN member untuk UI JSP dan ZKoss.
 *
 * <p>Ekspor tidak pernah mengandung PIN, hash, salt, atau material biometrik. Berkas hanya
 * memuat identitas, status PIN, dan kolom {@code pin_baru} kosong. Saat diimpor, PIN baru
 * diteruskan ke mesin PBKDF2 yang sama dengan ApiEBisnis.</p>
 */
public final class PinMemberMassalUtil {
	private PinMemberMassalUtil() {
	}

	public static boolean bolehKelola(Tbmuser pengguna) {
		return pengguna != null && (Common.getApakahAdminLain(pengguna)
				|| (pengguna.getPedagang() != null && Boolean.TRUE.equals(pengguna.getPedagang().getSupervisor())));
	}

	@SuppressWarnings("unchecked")
	public static String buatTemplateTsv() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			List<AnggotaKoperasi> daftar = session.createCriteria(AnggotaKoperasi.class)
					.addOrder(Order.asc("nama")).list();
			StringBuilder isi = new StringBuilder("id\tkode\tnama\tkode_identitas\tpin_sudah_diatur\tpin_baru\r\n");
			for (AnggotaKoperasi anggota : daftar) {
				isi.append(anggota.getId()).append('\t').append(aman(anggota.getKode())).append('\t')
						.append(aman(anggota.getNama())).append('\t').append(aman(anggota.getKodeIdentitas())).append('\t')
						.append(anggota.getPinSudahDiatur() ? "YA" : "TIDAK").append("\t\r\n");
			}
			return isi.toString();
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static JSONObject imporTsv(String isi) throws Exception {
		JSONObject hasil = new JSONObject();
		JSONArray data = new JSONArray();
		String[] baris = (isi == null ? "" : isi.replace("\r", "")).split("\n");
		if (baris.length < 2) {
			hasil.put("status", "91");
			hasil.put("description", "Berkas PIN kosong atau tidak memakai template yang benar.");
			return hasil;
		}
		String[] kepala = baris[0].split("\t", -1);
		int idxId = indeks(kepala, "id");
		int idxKode = indeks(kepala, "kode");
		int idxPin = indeks(kepala, "pin_baru");
		if (idxPin < 0 || (idxId < 0 && idxKode < 0)) {
			hasil.put("status", "91");
			hasil.put("description", "Kolom id/kode dan pin_baru wajib tersedia.");
			return hasil;
		}
		for (int i = 1; i < baris.length; i++) {
			String[] kolom = baris[i].split("\t", -1);
			String pin = nilai(kolom, idxPin).trim();
			if (pin.length() == 0) continue;
			JSONObject item = new JSONObject();
			String id = nilai(kolom, idxId).trim();
			if (id.matches("[0-9]+")) item.put("id", Long.valueOf(id));
			else item.put("kode", nilai(kolom, idxKode).trim());
			item.put("pin", pin);
			data.put(item);
		}
		if (data.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "Isi minimal satu nilai pada kolom pin_baru.");
			return hasil;
		}
		JSONObject permintaan = new JSONObject();
		permintaan.put("data", data);
		KantinHelper.anggotaPinSimpanMassal(permintaan, hasil);
		return hasil;
	}

	private static String aman(String nilai) {
		return nilai == null ? "" : nilai.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
	}

	private static int indeks(String[] kepala, String nama) {
		for (int i = 0; i < kepala.length; i++) if (nama.equalsIgnoreCase(kepala[i].trim())) return i;
		return -1;
	}

	private static String nilai(String[] kolom, int indeks) {
		return indeks >= 0 && indeks < kolom.length ? kolom[indeks] : "";
	}
}
