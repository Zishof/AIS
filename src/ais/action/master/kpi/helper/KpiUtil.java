package ais.action.master.kpi.helper;

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
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.Kpi;
import ais.database.model.kpi.SkorKpi;
import ais.ui.util.WaktuUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class KpiUtil {

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
					targets.put(Common.dateFormat8.get().format(tgl), jsonObject.get("target") + "");
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

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:51");
			// TODO: handle exception
		}
		return hasil;
	}

	public static Double ambilPoint(JSONObject jsonObject, boolean refresh) throws Exception {
		return ambilPoint(jsonObject, null, refresh);
	}

	@SuppressWarnings("rawtypes")
	public static Double ambilPoint(JSONObject jsonObject, Map dataItemKpi, boolean refresh) throws Exception {
		Double hasil = 0.0;
		try {
			Map<String, Double> data = new HashMap<String, Double>();
			for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
				SkorKpi skorKpi = (SkorKpi) o;
				if (skorKpi.getAktif()) {
					if (!jsonObject.isNull(skorKpi.getKode())) {
						data.put(skorKpi.getKode(), jsonObject.getDouble(skorKpi.getKode()));
					} else {
						data.put(skorKpi.getKode(), 0.0);
					}
				}
			}

			Date sekarang = WaktuUtil.getDate();

			if (data != null && !jsonObject.isNull("target")) {
				String target = jsonObject.get("target") + "";
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

					for (Object o : ConstantValues.ambilBerdasarClass(Kpi.class).values()) {
						Kpi kpi = (Kpi) o;
						if (kpi.getAktif()) {
							if (StringUtils.contains(target, " " + kpi.getKode() + " ")) {
								Double nilai = KpiUtil.ambilPoint(kpi.getFormula(), sekarang, dataItemKpi, refresh, 0);
								target = target.replaceAll(" " + kpi.getKode() + " ", " " + nilai + " ");
							}
						}
					}

					if (dataItemKpi != null) {
						for (Object o : dataItemKpi.values()) {
							ItemKpi itemKpi = (ItemKpi) o;
							if (itemKpi.getAktif()) {
								if (StringUtils.contains(target, " " + itemKpi.getKode() + " ")) {
									Double nilai = refresh
											? KpiUtil.ambilPoint(itemKpi.getFormula(), sekarang, dataItemKpi, refresh,
													0)
											: itemKpi.getTarget();
									target = target.replaceAll(" " + itemKpi.getKode() + " ", " " + nilai + " ");
								}
							}
						}
					}

					if (Common.isNumber(target)) {
						hasil = Double.parseDouble(target.trim());
					} else {

						try {
							Expression e = new ExpressionBuilder(target).variables(data.keySet())
									.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

							for (String kode : data.keySet()) {
								try {
									e.setVariable(kode, data.get(kode));
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:138");
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
								ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:155");
							}
							hasil = e.evaluate();
						} catch (Exception ee) {
							ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:159");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:165");
		}
		return hasil;

	}

	public static Double ambilPoint(String formula, Date sekarang, boolean refresh) throws Exception {
		return ambilPoint(formula, sekarang, null, refresh, 0);
	}

	@SuppressWarnings({ "rawtypes" })
	public static Double ambilPoint(String formula, Date sekarang, Map dataItemKpi, boolean refresh, int coba)
			throws Exception {
		if (coba > 500) {
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

				for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
					SkorKpi skorKpi = (SkorKpi) o;
					if (skorKpi.getAktif()) {
						try {
							if (!jsonObject.isNull(skorKpi.getKode())) {
								data.put(skorKpi.getKode(), jsonObject.getDouble(skorKpi.getKode()));
							} else {
								data.put(skorKpi.getKode(), 0.0);
							}
						} catch (Exception e) {
							data.put(skorKpi.getKode(), 0.0);
						}
					}
				}

			}
		}

		Map<String, Double> data = null;
		for (String ss : formulas.keySet()) {
			try {
				Date tanggalEfektif = Common.dateFormat8.get().parse(ss);
				if (tanggalEfektif.before(sekarang) || Common.dateFormat1.get().format(tanggalEfektif).equals(s)) {
					data = formulas.get(ss);
					break;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:222");
				// TODO: handle exception
			}
		}

		Double hasil = 0.0;
		if (data != null) {
			String target = ambilTarget(formula, sekarang);
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

				for (Object o : ConstantValues.ambilBerdasarClass(Kpi.class).values()) {
					Kpi kpi = (Kpi) o;
					if (kpi.getAktif()) {
						if (StringUtils.contains(target, " " + kpi.getKode() + " ")) {
							Double nilai = KpiUtil.ambilPoint(kpi.getFormula(), sekarang, dataItemKpi, refresh, ++coba);
							target = target.replaceAll(" " + kpi.getKode() + " ", " " + nilai + " ");
						}
					}
				}

				if (dataItemKpi != null) {
					for (Object o : dataItemKpi.values()) {
						ItemKpi itemKpi = (ItemKpi) o;
						if (itemKpi.getAktif()) {
							if (StringUtils.contains(target, " " + itemKpi.getKode() + " ")) {
								Double nilai = refresh
										? KpiUtil.ambilPoint(itemKpi.getFormula(), sekarang, dataItemKpi, refresh,
												++coba)
										: itemKpi.getTarget();
								target = target.replaceAll(" " + itemKpi.getKode() + " ", " " + nilai + " ");
							}
						}
					}
				}

				if (Common.isNumber(target)) {
					hasil = Double.parseDouble(target.trim());
				} else {

					try {
						Expression e = new ExpressionBuilder(target).variables(data.keySet())
								.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();

						for (String kode : data.keySet()) {
							try {
								e.setVariable(kode, data.get(kode));
							} catch (Exception ee) {
								ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:288");
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
							ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:305");
						}
						hasil = e.evaluate();
					} catch (Exception ee) {
						ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/master/kpi/helper/KpiUtil.java:309");
					}
				}
			}
		}
		formulas = null;
		jsonArray = null;
		return hasil;
	}

	public static String ambilDeskripsi(String formula) throws Exception {

		String t = "<ol style='font-size:x-small;text-align: left;'>";

		try {
			JSONArray jsonArray = new JSONArray(formula);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String d = "";
				if (!jsonObject.isNull("tgl")) {
					d = "Tgl:" + jsonObject.get("tgl");
				}

				for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
					SkorKpi skorKpi = (SkorKpi) o;
					if (skorKpi.getAktif()) {
						if (!jsonObject.isNull(skorKpi.getKode())) {
							d += ", " + skorKpi.getNama() + ": " + jsonObject.get(skorKpi.getKode());
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/helper/KpiUtil.java:349");
			// TODO: handle exception
		}

		t += "</ol>";
		return t;
	}

}
