package ais.action.master.employ.helper;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.LogicalUtil;
import ais.database.model.Konstanta;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.SkorGolongan;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class GolonganUtil {

	public static String ambilTarget(String formula, Date sekarang) throws Exception {
		String hasil = "";
		try {
			String s = Common.dateFormat1.get().format(sekarang);
			JSONArray jsonArray = new JSONArray(formula);
			TreeMap<String, String> targets = new TreeMap<String, String>(Collections.reverseOrder());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				if (!jsonObject.isNull("tgl") && !jsonObject.isNull("target")) {
					Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
					targets.put(Common.dateFormat8.get().format(tgl), jsonObject.get("target")+"");
				}
			}

			for (String ss : targets.keySet()) {
				Date tanggalEfektif = Common.dateFormat8.get().parse(ss);
				if (tanggalEfektif.before(sekarang) || Common.dateFormat1.get().format(tanggalEfektif).equals(s)) {
					hasil = targets.get(ss);
					break;
				}
			}
			targets = null;
			jsonArray = null;

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:50");
			// TODO: handle exception
		}
		return hasil;
	}

	public static Double ambilPoint(JSONObject jsonObject) throws Exception {
		Double hasil = 0.0;
		try {
			Map<String, Double> data = new HashMap<String, Double>();
			for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
				SkorGolongan skorGolongan = (SkorGolongan) o;
				if (skorGolongan.getAktif()) {
					if (!jsonObject.isNull(skorGolongan.getKode())) {
						data.put(skorGolongan.getKode(), jsonObject.getDouble(skorGolongan.getKode()));
					} else {
						data.put(skorGolongan.getKode(), 0.0);
					}
				}
			}

			Date sekarang = WaktuUtil.getDate();

			if (data != null && !jsonObject.isNull("target")) {
				String target = jsonObject.get("target")+"";
				if (target != null && !target.trim().isEmpty()) {

					target = " " + target + " ";
					target = target.replaceAll("\\(", " ( ");
					target = target.replaceAll("\\)", " ) ");
					target = target.replaceAll("\\+", " + ");
					target = target.replaceAll("\\-", " - ");
					target = target.replaceAll("\\*", " * ");
					target = target.replaceAll("/", " / ");
					target = target.replaceAll("%", " % ");

					for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
						Konstanta konstanta = (Konstanta) o;
						if (konstanta.getAktif() && konstanta.getKode() != null) {
							if (StringUtils.contains(target, " " + konstanta.getKode() + " ")) {
								target = org.apache.commons.lang3.StringUtils.replace(target, " " + konstanta.getKode() + " ",
										" " + konstanta.getKeterangan() + " ");
							}
						}

					}

					for (Object o : ConstantValues.ambilBerdasarClass(Golongan.class).values()) {
						Golongan golongan = (Golongan) o;
						if (golongan.getAktif()) {
							if (StringUtils.contains(target, " " + golongan.getKode() + " ")) {
								Double nilai = GolonganUtil.ambilPoint(golongan.getFormula(), sekarang, 0);
								target = target.replaceAll(" " + golongan.getKode() + " ", " " + nilai + " ");
							}
						}
					}

					try {
						Expression e = new ExpressionBuilder(target).variables(data.keySet())
								.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

						for (String kode : data.keySet()) {
							try {
								e.setVariable(kode, data.get(kode));
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:114");
							}
						}
						try {
							boolean valid = e.validate().isValid();
							System.out.println("formula = " + target + " valid => " + valid+" data -> "+data);
							if (!valid) {
								List<String> d = e.validate().getErrors();
								if (!d.isEmpty()) {
									String ds = "";
									for (String dd : d) {
										ds += ds.isEmpty() ? dd : ".\n" + dd;
									}
									System.out.println("error = " + ds);
								}
							}
						} catch (Exception ee) {
							ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:131");
						}

						hasil = e.evaluate();
					} catch (Exception ee) {
						ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:136");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:141");
		}
		return hasil;

	}

	public static Double ambilPoint(String formula, Date sekarang) throws Exception {
		return ambilPoint(formula, sekarang, 0);
	}

	@SuppressWarnings({})
	public static Double ambilPoint(String formula, Date sekarang, int coba) throws Exception {
		if (coba > 25) {
			return 0.0;
		}
		String s = Common.dateFormat1.get().format(sekarang);
		JSONArray jsonArray = new JSONArray(formula);
		TreeMap<String, Map<String, Double>> formulas = new TreeMap<String, Map<String, Double>>(
				Collections.reverseOrder());
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			if (!jsonObject.isNull("tgl")) {
				Date tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());

				Map<String, Double> data = formulas.get(Common.dateFormat8.get().format(tgl));
				if (data == null) {
					data = new HashMap<String, Double>();
					formulas.put(Common.dateFormat8.get().format(tgl), data);
				}

				for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
					try {
						SkorGolongan skorGolongan = (SkorGolongan) o;
						if (skorGolongan.getAktif()) {
							if (!jsonObject.isNull(skorGolongan.getKode())) {
								data.put(skorGolongan.getKode(), jsonObject.getDouble(skorGolongan.getKode()));
							} else {
								data.put(skorGolongan.getKode(), 0.0);
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:181");
						// TODO: handle exception
					}
				}

			}
		}

		Map<String, Double> data = null;
		for (String ss : formulas.keySet()) {
			Date tanggalEfektif = Common.dateFormat8.get().parse(ss);
			if (tanggalEfektif.before(sekarang) || Common.dateFormat1.get().format(tanggalEfektif).equals(s)) {
				data = formulas.get(ss);
				break;
			}
		}

		Double hasil = 0.0;
		if (data != null) {
			String target = ambilTarget(formula, sekarang);
			hasil = GolonganUtil.hitung(data, target, sekarang, coba);
		}
		formulas = null;
		jsonArray = null;
		return hasil;
	}

	public static Double hitung(Map<String, Double> data, String target, Date sekarang, int coba) throws Exception {
		Double hasil = 0.0;
		if (target != null && !target.trim().isEmpty()) {
			target = " " + target + " ";
			target = target.replaceAll("\\(", " ( ");
			target = target.replaceAll("\\)", " ) ");
			target = target.replaceAll("\\+", " + ");
			target = target.replaceAll("\\-", " - ");
			target = target.replaceAll("\\*", " * ");
			target = target.replaceAll("/", " / ");
			target = target.replaceAll("%", " % ");
			target = " " + target + " ";
			for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
				Konstanta konstanta = (Konstanta) o;
				if (konstanta.getAktif() && konstanta.getKode() != null) {
					if (StringUtils.contains(target, " " + konstanta.getKode() + " ")) {
						target = org.apache.commons.lang3.StringUtils.replace(target, " " + konstanta.getKode() + " ",
								" " + konstanta.getKeterangan() + " ");
					}
				}

			}

			for (Object o : ConstantValues.ambilBerdasarClass(Golongan.class).values()) {
				Golongan golongan = (Golongan) o;
				if (golongan.getAktif()) {
					if (StringUtils.contains(target, " " + golongan.getKode() + " ")) {
						Double nilai = GolonganUtil.ambilPoint(golongan.getFormula(), sekarang, ++coba);
						target = org.apache.commons.lang3.StringUtils.replace(target, " " + golongan.getKode() + " ", " " + nilai + " ");
					}
				}
			}

			try {
				Expression e = new ExpressionBuilder(target).variables(data.keySet())
						.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

				for (String kode : data.keySet()) {
					try {
						e.setVariable(kode, data.get(kode));
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:248");
					}
				}
				try {
					boolean valid = e.validate().isValid();
					System.out.println("formula = " + target + " valid => " + valid);
					if (!valid) {
						List<String> d = e.validate().getErrors();
						if (!d.isEmpty()) {
							String ds = "";
							for (String dd : d) {
								ds += ds.isEmpty() ? dd : ".\n" + dd;
							}
							System.out.println("error = " + ds);
						}
					}
				} catch (Exception ee) {
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/employ/helper/GolonganUtil.java:265");
				}
				hasil = e.evaluate();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/employ/helper/GolonganUtil.java:268");
			}
		}
		return hasil;
	}

	public static String ambilDeskripsi(String formula) throws Exception {
		JSONArray jsonArray = new JSONArray(formula);
		String t = "<ol>";
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			String d = "";
			if (!jsonObject.isNull("tgl")) {
				d = "Tgl:" + jsonObject.get("tgl");
			}

			for (Object o : ConstantValues.ambilBerdasarClass(SkorGolongan.class).values()) {
				SkorGolongan skorGolongan = (SkorGolongan) o;
				if (skorGolongan.getAktif()) {
					if (!jsonObject.isNull(skorGolongan.getKode())) {
						d += ", " + skorGolongan.getNama() + ": " + jsonObject.get(skorGolongan.getKode());
					}
				}
			}

			if (!jsonObject.isNull("target")) {
				d += ", Formula: " + jsonObject.get("target");
			}

			if (!d.isEmpty()) {
				t += "<li>" + d + "</li>";
			}
		}

		t += "</ol>";
		return t;
	}

}
