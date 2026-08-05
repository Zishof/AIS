package ais.action.master.feeder.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.json.JSONObject;
import org.w3c.dom.Node;

import ais.common.Common;
import ais.database.model.Detailperkuliahan;

public class FeederTranspoter {
	public static void insertNilai(FeederConnector feederConnector, String token, Detailperkuliahan detailperkuliahan,
			Session session, List<String> errorLog, boolean ada) throws Exception {
		JSONObject jsonObject = FeederExporterGenerator.nilai(detailperkuliahan);

		String id_kls = detailperkuliahan.getId_kls();
		String id_reg_pd = detailperkuliahan.getId_reg_pd();
		System.out.println("id_kls = " + id_kls + ", id_reg_pd = " + id_reg_pd + " ada " + ada);
		if (!ada || id_kls == null || id_kls.isEmpty() || id_reg_pd == null || id_reg_pd.isEmpty()) {

			Node node = feederConnector.insertRecordOld(token, "nilai", jsonObject.toString(), errorLog,
					detailperkuliahan);
			id_kls = FeederConverter.value(node, "id_kls");
			id_reg_pd = FeederConverter.value(node, "id_reg_pd");

			detailperkuliahan.setId_kls(id_kls);
			detailperkuliahan.setId_reg_pd(id_reg_pd);
			session.getTransaction().begin();
			Common.refreshUpdate(session, detailperkuliahan);
			session.getTransaction().commit();
		} else if (detailperkuliahan.getTotalNilai() > 0.1) {
			FeederTranspoter.updateNilai(jsonObject, feederConnector, token, detailperkuliahan, session, errorLog);
		}
	}

	public static void updateNilai(JSONObject jsonObject, FeederConnector feederConnector, String token,
			Detailperkuliahan detailperkuliahan, Session session, List<String> errorLog) throws Exception {

		jsonObject.remove("id_kls");
		jsonObject.remove("id_reg_pd");

		Map<String, Object> dataKey = new HashMap<String, Object>();
		dataKey.put("id_kls", detailperkuliahan.getId_kls().trim());
		dataKey.put("id_reg_pd", detailperkuliahan.getId_reg_pd().trim());
		JSONObject jsonObjectKey = new JSONObject(dataKey);
		Map<String, Object> dataUpdate = new HashMap<String, Object>();
		dataUpdate.put("key", jsonObjectKey);
		dataUpdate.put("data", jsonObject);
		JSONObject dataUpdateObject = new JSONObject(dataUpdate);

		List<String> localError = new ArrayList<String>();
		feederConnector.updateRecordOld(token, "nilai", dataUpdateObject.toString(), localError, detailperkuliahan);
		System.out.println("localError update = " + localError);
		if (!localError.isEmpty()) {
			JSONObject jsonObjectRestore = new JSONObject();
			jsonObjectRestore.put("id_kls", detailperkuliahan.getId_kls().trim());
			jsonObjectRestore.put("id_reg_pd", detailperkuliahan.getId_reg_pd().trim());
			List<String> localErrorlagi = new ArrayList<String>();
			feederConnector.restoreRecord(token, "nilai", jsonObjectRestore.toString(), localErrorlagi,
					detailperkuliahan);
			System.out.println("localErrorlagi restore = " + localErrorlagi);
			if (localErrorlagi.isEmpty()) {
				feederConnector.updateRecordOld(token, "nilai", dataUpdateObject.toString(), errorLog, detailperkuliahan);
			} else {
				List<String> localErrorlagilagi = new ArrayList<String>();
				JSONObject jsonObjectLagi = FeederExporterGenerator.nilai(detailperkuliahan);
				feederConnector.insertRecordOld(token, "nilai", jsonObjectLagi.toString(), localErrorlagilagi,
						detailperkuliahan);
				System.out.println("localErrorlagilagi insert = " + localErrorlagi);
				if (!localErrorlagilagi.isEmpty()) {
					errorLog.addAll(localError);
					errorLog.addAll(localErrorlagi);
					errorLog.addAll(localErrorlagilagi);
				}
			}
		}
	}

	public static void insertAkm(JSONObject jsonObject, FeederConnector feederConnector, String token, Session session)
			throws Exception {

		Node node = feederConnector.insertRecordOld(token, "kuliah_mahasiswa", jsonObject.toString());
		String idSmt = FeederConverter.value(node, "id_smt");
		String idRegPd = FeederConverter.value(node, "id_reg_pd");
		if (idSmt != null && !idSmt.isEmpty() && idRegPd != null && !idRegPd.isEmpty()) {
			System.out.println("idSmt = " + idSmt + ", idRegPd = " + idRegPd);
		} else {
			FeederTranspoter.updateAkm(jsonObject, feederConnector, token, session);
		}
	}

	public static void updateAkm(JSONObject jsonObject, FeederConnector feederConnector, String token, Session session)
			throws Exception {
		String idSmt = (String) jsonObject.getString("id_smt");
		String idRegPd = (String) jsonObject.getString("id_reg_pd");

		Map<String, Object> dataKey = new HashMap<String, Object>();
		dataKey.put("id_smt", idSmt.trim());
		dataKey.put("id_reg_pd", idRegPd.trim());
		JSONObject jsonObjectKey = new JSONObject(dataKey);
		Map<String, Object> dataUpdate = new HashMap<String, Object>();
		dataUpdate.put("key", jsonObjectKey);
		dataUpdate.put("data", jsonObject);
		JSONObject dataUpdateObject = new JSONObject(dataUpdate);

		Node node = feederConnector.updateRecordOld(token, "kuliah_mahasiswa", dataUpdateObject.toString());
		System.out.println("node = " + node);
	}
}
