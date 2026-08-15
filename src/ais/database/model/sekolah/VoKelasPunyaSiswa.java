package ais.database.model.sekolah;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

public abstract class VoKelasPunyaSiswa extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9043530115369882721L;

	public abstract Siswa getSiswa();

	public abstract List<Long> ambilMk();

	public abstract String getDetailNilai();

	public abstract void setDetailNilai(String detailNilai);

	public abstract String getDetailNilaiTotal();

	public abstract void setDetailNilaiTotal(String detailNilaiTotal);

	public abstract String getKeterangan1();

	public abstract String getKeterangan2();

	public abstract void setKeterangan1(String keterangan1);

	public abstract void setKeterangan2(String keterangan2);

	public abstract KelasSiswa ambilKelasSiswa();
	
	public abstract Boolean getAktif();

	public String retreiveDetailNilai(JenisItemPenilaianSiswa jenisItemPenilaianSiswa,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Matapelajaran matapelajaran, Integer smt,
			Boolean hanyaValid) {

		if (ambilKelasSiswa() != null && !ambilKelasSiswa().getPublikasiNilaiHarusTelahDiverifikasi()) {
			hanyaValid = null;
		}

		if (jenisItemPenilaianSiswa != null && jenisItemPenilaianSiswa.getId() != null) {
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Boolean valid = Boolean.parseBoolean(s[5]);
					Long grupId = Long.parseLong(s[7]);
					if ((hanyaValid == null || hanyaValid.equals(valid))
							&& jenisItemPenilaianSiswa.getId().equals(formatId)
							&& matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
							&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:70");

				}
			}
		}

		return "";
	}

	public Boolean retreiveDetailVerify(JenisItemPenilaianSiswa jenisItemPenilaianSiswa,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Matapelajaran matapelajaran, Integer smt) {

		if (jenisItemPenilaianSiswa != null && jenisItemPenilaianSiswa.getId() != null) {
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);
					if (jenisItemPenilaianSiswa.getId().equals(formatId) && matapelajaran.getId().equals(matpelId)
							&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return Boolean.parseBoolean(s[5]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:95");

				}
			}
		}

		return false;
	}

	public Double retreiveTotalNilai(List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas, String target,
			Matapelajaran matapelajaran, GrupPenilaian grupPenilaian,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Integer smt, Boolean hanyaValid)
			throws Exception {

		if (ambilKelasSiswa() != null && !ambilKelasSiswa().getPublikasiNilaiHarusTelahDiverifikasi()) {
			hanyaValid = null;
		}

		Double total = 0.0;
		if (matapelajaran != null && matapelajaran.getId() != null) {
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			Map<String, String> data = new HashMap<String, String>();

			for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {
				if (jenisItemPenilaianSiswa != null) {
					data.put(jenisItemPenilaianSiswa.getKode(), "0.0");
					data.put(jenisItemPenilaianSiswa.getKode() + "_s", "0.0");
				}
			}

			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}

					Long matpelId = Long.parseLong(s[1]);

					Integer smtId = Integer.parseInt(s[6]);
					Boolean valid = Boolean.parseBoolean(s[5]);
					Long grupId = Long.parseLong(s[7]);

					if ((hanyaValid == null || hanyaValid.equals(valid)) && matapelajaran.getId().equals(matpelId)
							&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						String n = nilaiUntukFormula(s[2]);
						JenisItemPenilaianSiswa jenisItemPenilaianSiswa = (JenisItemPenilaianSiswa) ConstantValues
								.ambil(JenisItemPenilaianSiswa.class.getName(), Long.parseLong(s[0]));
						data.put(jenisItemPenilaianSiswa.getKode(), n);

						boolean sesuai = retreiveDetailVerify(jenisItemPenilaianSiswa, grupKategoriItemPenilaianSiswa,
								matapelajaran, smt);
						data.put(jenisItemPenilaianSiswa.getKode() + "_s", sesuai ? "1.0" : "0.0");
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:146");
//					e.printStackTrace();
				}
			}

			Date sekarang = WaktuUtil.getDate();

//			System.out.println("data -> " + data + " " + target);
			total = GrupPenilaianUtil.hitung(data, matapelajaran, target, grupPenilaian, null, sekarang, null);
		}

		return total;
	}

	public void populateDetailNilai(JenisItemPenilaianSiswa jenisItemPenilaianSiswa, Matapelajaran matapelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, String jumlah, Boolean verify, Integer smt) {
		if (jumlah != null && jumlah.trim().isEmpty()) {
			verify = false;
		}
		jumlah = jumlah == null ? "" : jumlah;
		jumlah = org.apache.commons.lang3.StringUtils.replace(jumlah, "|", " ");
		jumlah = org.apache.commons.lang3.StringUtils.replace(jumlah, ";", ",");
		if (jenisItemPenilaianSiswa != null) {
			String formatBaru = "";
			String[] nilais = getDetailNilai() == null ? new String[] {} : getDetailNilai().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						Long matpelId = Long.parseLong(s[1]);
						Integer smtId = Integer.parseInt(s[6]);
						Long grupId = Long.parseLong(s[7]);
						if (jenisItemPenilaianSiswa.getId().equals(formatId) && matapelajaran.getId().equals(matpelId)
								&& smtId.equals(smt) && grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
							aformatBaru = jenisItemPenilaianSiswa.getId() + "|" + matapelajaran.getId() + "|" + jumlah
									+ "|0|0|" + verify + "|" + smt + "|" + grupKategoriItemPenilaianSiswa.getId();
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:192");
				}
			}

			if (!ada) {
				String aformatBaru = jenisItemPenilaianSiswa.getId() + "|" + matapelajaran.getId() + "|" + jumlah
						+ "|0|0|" + verify + "|" + smt + "|" + grupKategoriItemPenilaianSiswa.getId();
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			setDetailNilai(formatBaru);
		}
	}

	public String retreiveDetailNilaiTotal(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa,
			Matapelajaran matapelajaran, Integer smt) {

		if (grupKategoriItemPenilaianSiswa != null && grupKategoriItemPenilaianSiswa.getId() != null) {
			String[] nilais = getDetailNilaiTotal() == null ? new String[] {} : getDetailNilaiTotal().split(";");
			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					Long matpelId = Long.parseLong(s[1]);
					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);
					if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
							&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:221");

				}
			}
		}

		return "";
	}

	public Double retreiveTotalNilaiTotal(String target, Matapelajaran matapelajaran, GrupPenilaian grupPenilaian,
			Integer smt, List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas) throws Exception {
		Double total = 0.0;
		if (matapelajaran != null && matapelajaran.getId() != null) {
			String[] nilais = getDetailNilaiTotal() == null ? new String[] {} : getDetailNilaiTotal().split(";");
			Map<String, String> data = new HashMap<String, String>();

			for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {
				if (grupKategoriItemPenilaianSiswa != null) {
					data.put(grupKategoriItemPenilaianSiswa.getKode(), "0.0");
				}
			}

			for (String nn : nilais) {
				try {
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}

					Long matpelId = Long.parseLong(s[1]);

					Integer smtId = Integer.parseInt(s[6]);
					Long grupId = Long.parseLong(s[7]);

					if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)) {
						String n = nilaiUntukFormula(s[2]);
						GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) ConstantValues
								.ambil(GrupKategoriItemPenilaianSiswa.class.getName(), grupId);
						data.put(grupKategoriItemPenilaianSiswa.getKode(), n);

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:259");
//					e.printStackTrace();
				}
			}

			Date sekarang = WaktuUtil.getDate();

//			System.out.println("retreiveTotalNilaiTotal data -> " + data + " " + target);
			total = GrupPenilaianUtil.hitung(data, matapelajaran, target, grupPenilaian, null, sekarang, null);
		}

		return total;
	}

	public void populateDetailNilaiTotal(Matapelajaran matapelajaran,
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa, Double jumlah, Boolean verify, Integer smt) {
		if (jumlah != null) {
			verify = false;
		}
		if (grupKategoriItemPenilaianSiswa != null) {
			String formatBaru = "";
			String[] nilais = getDetailNilaiTotal() == null ? new String[] {} : getDetailNilaiTotal().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = splitNilai(nn);
					if (s == null) {
						continue;
					}
					if (!s[0].trim().isEmpty()) {
						Long matpelId = Long.parseLong(s[1]);
						Integer smtId = Integer.parseInt(s[6]);
						Long grupId = Long.parseLong(s[7]);
						if (matapelajaran.getId().equals(matpelId) && smtId.equals(smt)
								&& grupId.equals(grupKategoriItemPenilaianSiswa.getId())) {
							aformatBaru = "0|" + matapelajaran.getId() + "|" + jumlah + "|0|0|" + verify + "|" + smt
									+ "|" + grupKategoriItemPenilaianSiswa.getId();
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:302");
				}
			}

			if (!ada) {
				String aformatBaru = "0|" + matapelajaran.getId() + "|" + jumlah + "|0|0|" + verify + "|" + smt + "|"
						+ grupKategoriItemPenilaianSiswa.getId();
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			setDetailNilaiTotal(formatBaru);
		}
	}

	private String[] splitNilai(String nilai) {
		String[] s = StringUtils.splitPreserveAllTokens(nilai, "|");
		if (s == null || s.length < 8) {
			return null;
		}
		for (int i = 0; i < 8; i++) {
			if (s[i] == null) {
				s[i] = "";
			}
		}
		return s;
	}

	private String nilaiUntukFormula(String nilai) {
		if (nilai == null || nilai.trim().isEmpty()) {
			return "0.0";
		}
		try {
			Double.parseDouble(nilai.trim());
			return nilai.trim();
		} catch (Exception e) {
			String[] pasangan = StringUtils.split(nilai, ":");
			if (pasangan != null && pasangan.length > 1) {
				try {
					Double.parseDouble(pasangan[1].trim());
					return pasangan[1].trim();
				} catch (Exception ex) {
					ais.common.ErrorAuditUtil.record(ex,
							"auto-audit(nilai pilihan bukan angka) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:nilaiUntukFormula");
				}
			}
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(nilai non angka dianggap 0 untuk formula) src/ais/database/model/sekolah/VoKelasPunyaSiswa.java:nilaiUntukFormula");
			return "0.0";
		}
	}

}
