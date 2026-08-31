package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.model.GeneralValueObject;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.obe.BahanKajian;
import ais.database.model.obe.CapaianLulusan;
import ais.database.model.obe.CapaianPembelajaranLulusan;
import ais.database.model.obe.ReferensiLulusan;

/**
 * Helper terpusat untuk fitur "Generate via AI" pada modul OBE/RPS sisi native JSP.
 * Menyediakan pembangun konteks OBE (mirror bangunKonteksObe versi ZK), parser JSON
 * tahan-banting, parser format COCOK/USUL_BARU, dan pembuat entitas OBE. Dipakai oleh
 * service JSP di webapp/WEB-INF/baru/modul/elearning/obe/_generate_*_ai.jsp.
 * Panggilan AI memakai {@link ais.action.servlet.AiGenerateServlet#generateText(String,int)}.
 */
public class ObeAiJspHelper {

	// ---------------------------------------------------------------------
	// Konteks OBE komprehensif
	// ---------------------------------------------------------------------
	public static String bangunKonteks(KurikulumPunyaMatakuliah kpm, Matakuliah mk) {
		StringBuilder k = new StringBuilder();
		k.append("=== KONTEKS RPS/OBE YANG SUDAH TERSIMPAN ===\n");
		if (mk != null) {
			k.append("Nama Matakuliah: ").append(bersih(mk.getNama()));
			if (mk.getKode() != null) k.append(" (").append(mk.getKode()).append(")");
			k.append("\n");
			if (mk.getKelompokMatakuliah() != null) k.append("Rumpun/Kelompok MK: ").append(bersih(mk.getKelompokMatakuliah().getNama())).append("\n");
			if (mk.getSks() != null) k.append("SKS: ").append(mk.getSks()).append("\n");
			if (mk.getJurusan() != null) {
				k.append("Program Studi: ").append(bersih(mk.getJurusan().getNama()));
				try { if (mk.getJurusan().getKaprodi() != null) k.append(" (Kaprodi: ").append(bersih(mk.getJurusan().getKaprodi().getNama())).append(")"); } catch (Exception e) {}
				k.append("\n");
			}
			if (mk.getKeterangan() != null && !mk.getKeterangan().trim().isEmpty())
				k.append("Default Capaian/Kompetensi: ").append(potong(bersih(mk.getKeterangan()), 500)).append("\n");
		}
		if (kpm != null) {
			if (kpm.getDeskripsiPembelajaran() != null && !kpm.getDeskripsiPembelajaran().trim().isEmpty())
				k.append("Deskripsi Singkat MK: ").append(potong(bersih(kpm.getDeskripsiPembelajaran()), 700)).append("\n");
			if (kpm.getMinimalKetercapaian() != null) k.append("Minimal Ketercapaian: ").append(kpm.getMinimalKetercapaian()).append("\n");
			if (Boolean.TRUE.equals(kpm.getNilaiMenggunakanCpmk())) k.append("Penilaian menggunakan CPMK (tanpa Sub-CPMK).\n");
			if (kpm.getMitraPengembang() != null && !kpm.getMitraPengembang().trim().isEmpty())
				k.append("Mitra Pengembang: ").append(bersih(kpm.getMitraPengembang())).append("\n");
		}
		if (mk != null) {
			// CPL
			String cpl = daftarKodeNama(CapaianLulusan.class, mk.getCapaianLulusan());
			if (cpl.length() > 0) k.append("CPL: ").append(cpl).append("\n");
			// CPMK + Sub
			String cpmkCsv = mk.getCapaianPembelajaranLulusan();
			if (cpmkCsv != null && !cpmkCsv.trim().isEmpty()) {
				StringBuilder cpmk = new StringBuilder();
				for (String id : cpmkCsv.split(",")) {
					if (id == null || id.trim().isEmpty()) continue;
					CapaianPembelajaranLulusan cp = (CapaianPembelajaranLulusan) GeneralValueObject.ambilData(CapaianPembelajaranLulusan.class, id.trim(), true);
					if (cp == null) continue;
					if (cpmk.length() > 0) cpmk.append("; ");
					cpmk.append(bersih(cp.getKode())).append("=").append(potong(bersih(cp.getNama()), 140));
					try {
						if (cp.getFormula() != null && !cp.getFormula().trim().isEmpty()) {
							JSONArray fa = new JSONArray(cp.getFormula());
							if (fa.length() > 0) {
								cpmk.append(" [Sub: ");
								for (int i = 0; i < fa.length(); i++) {
									JSONObject so = fa.optJSONObject(i);
									if (so == null) continue;
									if (i > 0) cpmk.append(", ");
									cpmk.append(bersih(so.optString("kode", ""))).append(":").append(potong(bersih(so.optString("nama", "")), 80));
								}
								cpmk.append("]");
							}
						}
					} catch (Exception e) {}
				}
				if (cpmk.length() > 0) k.append("CPMK: ").append(cpmk).append("\n");
			}
			String bk = daftarKodeNama(BahanKajian.class, mk.getBahanKajian());
			if (bk.length() > 0) k.append("Bahan Kajian: ").append(potong(bk, 500)).append("\n");
		}
		if (kpm != null) {
			String pu = daftarKodeNama(ReferensiLulusan.class, kpm.getPustaka());
			if (pu.length() > 0) k.append("Pustaka Utama: ").append(potong(pu, 500)).append("\n");
			String pp = daftarKodeNama(ReferensiLulusan.class, kpm.getPustakaPendukung());
			if (pp.length() > 0) k.append("Pustaka Pendukung: ").append(potong(pp, 400)).append("\n");
			if (kpm.getCatatan() != null && !kpm.getCatatan().trim().isEmpty())
				k.append("Catatan: ").append(potong(bersih(kpm.getCatatan()), 400)).append("\n");
		}
		k.append("=== AKHIR KONTEKS ===\n");
		return k.toString();
	}

	private static String daftarKodeNama(Class<?> clazz, String csv) {
		if (csv == null || csv.trim().isEmpty()) return "";
		StringBuilder b = new StringBuilder();
		for (String id : csv.split(",")) {
			if (id == null || id.trim().isEmpty()) continue;
			try {
				GeneralValueObject o = (GeneralValueObject) GeneralValueObject.ambilData(clazz, id.trim(), true);
				if (o == null) continue;
				String kode = null, nama = null;
				try { kode = (String) clazz.getMethod("getKode").invoke(o); } catch (Exception e) {}
				try { nama = (String) clazz.getMethod("getNama").invoke(o); } catch (Exception e) {}
				if (b.length() > 0) b.append("; ");
				if (kode != null && !kode.trim().isEmpty()) b.append(bersih(kode)).append("=");
				b.append(potong(bersih(nama), 120));
			} catch (Exception e) {}
		}
		return b.toString();
	}

	// ---------------------------------------------------------------------
	// Parser JSON tahan-banting
	// ---------------------------------------------------------------------
	public static JSONArray ekstrakArray(String resp) {
		if (resp == null) return new JSONArray();
		String s = buangFence(resp.trim());
		int a = s.indexOf('[');
		int b = s.lastIndexOf(']');
		if (a >= 0 && b > a) {
			try { return new JSONArray(s.substring(a, b + 1)); } catch (Exception e) {}
		}
		// Cadangan: pindai objek per objek dari brace-depth, bungkus jadi array
		JSONArray arr = new JSONArray();
		for (JSONObject o : pindaiObjek(s)) arr.put(o);
		return arr;
	}

	public static JSONObject ekstrakObjek(String resp) {
		if (resp == null) return new JSONObject();
		String s = buangFence(resp.trim());
		int a = s.indexOf('{');
		int b = s.lastIndexOf('}');
		if (a >= 0 && b > a) {
			try { return new JSONObject(s.substring(a, b + 1)); } catch (Exception e) {}
		}
		List<JSONObject> list = pindaiObjek(s);
		return list.isEmpty() ? new JSONObject() : list.get(0);
	}

	private static String buangFence(String s) {
		int fence = s.indexOf("```");
		if (fence >= 0) {
			int nl = s.indexOf('\n', fence);
			int fa = s.lastIndexOf("```");
			if (nl >= 0 && fa > nl) return s.substring(nl + 1, fa).trim();
			if (nl >= 0) return s.substring(nl + 1).trim();
		}
		return s;
	}

	private static List<JSONObject> pindaiObjek(String s) {
		List<JSONObject> hasil = new ArrayList<JSONObject>();
		int depth = 0, mulai = -1;
		boolean dalamString = false, escape = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (dalamString) {
				if (escape) escape = false;
				else if (c == '\\') escape = true;
				else if (c == '"') dalamString = false;
				continue;
			}
			if (c == '"') dalamString = true;
			else if (c == '{') { if (depth == 0) mulai = i; depth++; }
			else if (c == '}') {
				if (depth > 0) {
					depth--;
					if (depth == 0 && mulai >= 0) {
						try { hasil.add(new JSONObject(s.substring(mulai, i + 1))); } catch (Exception e) {}
						mulai = -1;
					}
				}
			}
		}
		return hasil;
	}

	// ---------------------------------------------------------------------
	// Parser format teks COCOK / ALASAN_COCOK / USUL_BARU
	// ---------------------------------------------------------------------
	/**
	 * Tipe implementasi bersarang {@link Seleksi} milik {@link ObeAiJspHelper}. Kelas ini memberi nama pada state
	 * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link ObeAiJspHelper}.
	 * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List cocok}, {@code List baru},
	 * {@code String alasan}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 *
	 * @see ObeAiJspHelper
	 */
	public static class Seleksi {
		public List<String> cocok = new ArrayList<String>();
		public List<String[]> baru = new ArrayList<String[]>(); // [kode, deskripsi]
		public String alasan = "";
	}

	public static Seleksi parseSeleksi(String resp) {
		Seleksi h = new Seleksi();
		if (resp == null) return h;
		boolean inUsul = false;
		for (String raw : resp.split("\n")) {
			String line = raw.trim();
			if (line.isEmpty()) continue;
			String up = line.toUpperCase();
			if (up.startsWith("COCOK:")) {
				inUsul = false;
				String v = line.substring(6).replace("[", "").replace("]", "").trim();
				for (String kk : v.split("[,;]")) { if (kk.trim().length() > 0) h.cocok.add(kk.trim().toUpperCase()); }
			} else if (up.startsWith("ALASAN_COCOK:")) {
				inUsul = false;
				h.alasan = line.substring("ALASAN_COCOK:".length()).replace("[", "").replace("]", "").trim();
			} else if (up.startsWith("USUL_BARU")) {
				inUsul = true;
			} else if (inUsul && line.startsWith("-")) {
				String body = line.substring(1).trim();
				if (body.toUpperCase().startsWith("TIDAK ADA")) continue;
				int c = body.indexOf(':');
				if (c > 0) {
					String kode = body.substring(0, c).replace("[", "").replace("]", "").trim();
					String desk = body.substring(c + 1).trim();
					if (kode.length() > 0) h.baru.add(new String[] { kode, desk.isEmpty() ? kode : desk });
				} else if (body.length() > 0) {
					h.baru.add(new String[] { body, body });
				}
			}
		}
		return h;
	}

	// ---------------------------------------------------------------------
	// CSV helper (dedupe, aman-null)
	// ---------------------------------------------------------------------
	public static String appendId(String csv, Long id) {
		if (id == null) return csv == null ? "" : csv;
		String sid = String.valueOf(id);
		if (csv == null || csv.trim().isEmpty()) return sid;
		for (String p : csv.split(",")) { if (p.trim().equals(sid)) return csv; }
		return csv + "," + sid;
	}

	// ---------------------------------------------------------------------
	// Pembuat entitas OBE (dipanggil dalam transaksi terbuka)
	// ---------------------------------------------------------------------
	public static Long buatCapaianPembelajaran(Session s, Matakuliah mk, PerguruanTinggi pt, String kode, String nama) {
		CapaianPembelajaranLulusan e = new CapaianPembelajaranLulusan();
		e.setKode(kode);
		e.setNama(nama);
		if (mk != null) e.setJurusan(mk.getJurusan());
		e.setPerguruanTinggi(pt);
		e.setKhususBuatMk(mk);
		e.setAktif(Boolean.TRUE);
		s.save(e);
		s.flush();
		return e.getId();
	}

	public static Long buatCapaianLulusan(Session s, Matakuliah mk, PerguruanTinggi pt, String kode, String nama) {
		CapaianLulusan e = new CapaianLulusan();
		e.setKode(kode);
		e.setNama(nama);
		if (mk != null) e.setJurusan(mk.getJurusan());
		e.setPerguruanTinggi(pt);
		e.setKhususBuatMk(mk);
		e.setAktif(Boolean.TRUE);
		s.save(e);
		s.flush();
		return e.getId();
	}

	public static Long buatBahanKajian(Session s, Matakuliah mk, PerguruanTinggi pt, String kode, String nama) {
		BahanKajian e = new BahanKajian();
		if (kode != null && !kode.trim().isEmpty()) e.setKode(kode);
		e.setNama(nama);
		e.setPerguruanTinggi(pt);
		if (mk != null) e.setJurusan(mk.getJurusan());
		e.setKhususBuatMk(mk);
		e.setAktif(Boolean.TRUE);
		s.save(e);
		s.flush();
		return e.getId();
	}

	public static Long buatReferensi(Session s, PerguruanTinggi pt, String nama) {
		ReferensiLulusan e = new ReferensiLulusan();
		e.setNama(nama);
		e.setPerguruanTinggi(pt);
		e.setAktif(Boolean.TRUE);
		s.save(e);
		s.flush();
		return e.getId();
	}

	// ---------------------------------------------------------------------
	// util string
	// ---------------------------------------------------------------------
	public static String bersih(String s) {
		if (s == null) return "";
		return s.replaceAll("<[^>]*>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
	}

	public static String potong(String s, int maks) {
		if (s == null) return "";
		s = s.trim();
		return s.length() > maks ? s.substring(0, maks) + "..." : s;
	}
}
