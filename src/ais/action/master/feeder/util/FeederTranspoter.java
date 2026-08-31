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

/**
 * Orkestrasi pengiriman data nilai dan aktivitas kuliah mahasiswa (AKM) ke PDDikti Feeder,
 * menangani jalur insert (data baru) maupun update (data sudah ada), termasuk pemulihan otomatis
 * saat update ditolak oleh Feeder. Dipakai oleh proses sinkronisasi Feeder untuk entitas
 * {@link Detailperkuliahan} (nilai per mata kuliah) dan data kuliah mahasiswa (AKM).
 */
public class FeederTranspoter {
	/**
	 * Mengirim data nilai satu {@link Detailperkuliahan} ke Feeder: bila data belum pernah dikirim
	 * (ditandai {@code ada=false}) atau belum memiliki {@code id_kls}/{@code id_reg_pd}, dilakukan
	 * <b>insert</b> baru lewat {@code feederConnector.insertRecordOld}, dan hasil id yang
	 * dikembalikan Feeder disimpan kembali ke {@code detailperkuliahan} dalam transaksi tersendiri.
	 * Bila data sudah ada dan totalnilai lebih besar dari nol, didelegasikan ke {@link #updateNilai}.
	 *
	 * @param feederConnector    klien komunikasi ke Feeder
	 * @param token              token autentikasi sesi Feeder
	 * @param detailperkuliahan  baris nilai yang akan dikirim/diperbarui
	 * @param session            sesi Hibernate aktif untuk menyimpan id hasil insert
	 * @param errorLog           daftar pesan galat, diisi bila operasi Feeder gagal
	 * @param ada                {@code true} bila data ini diperkirakan sudah tercatat di Feeder sebelumnya
	 */
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

	/**
	 * Memperbarui data nilai yang sudah tercatat di Feeder (kunci {@code id_kls}+{@code id_reg_pd}).
	 * Bila update ditolak Feeder, dicoba dipulihkan berurutan: (1) panggil
	 * {@code restoreRecord} untuk mengembalikan record ke status dapat-diubah, lalu ulangi update;
	 * (2) bila restore juga gagal, coba <b>insert ulang</b> record nilai dari awal; hanya bila
	 * langkah insert-ulang ini juga gagal, seluruh pesan galat dari ketiga upaya digabung ke
	 * {@code errorLog} pemanggil.
	 *
	 * @param jsonObject         payload data nilai yang akan dikirim (field {@code id_kls}/{@code id_reg_pd} akan dilepas dan dipindah ke bagian kunci)
	 * @param feederConnector    klien komunikasi ke Feeder
	 * @param token              token autentikasi sesi Feeder
	 * @param detailperkuliahan  baris nilai yang diperbarui, sumber nilai kunci {@code id_kls}/{@code id_reg_pd}
	 * @param session            sesi Hibernate aktif (diteruskan untuk kemungkinan insert ulang)
	 * @param errorLog           daftar pesan galat, diisi hanya bila seluruh upaya pemulihan gagal
	 */
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

	/**
	 * Mengirim data aktivitas kuliah mahasiswa (AKM) baru ke Feeder ({@code kuliah_mahasiswa}).
	 * Bila Feeder tidak mengembalikan {@code id_smt}/{@code id_reg_pd} yang valid (indikasi record
	 * sudah ada sebelumnya), otomatis dialihkan ke {@link #updateAkm}.
	 *
	 * @param jsonObject      payload data AKM yang akan dikirim
	 * @param feederConnector klien komunikasi ke Feeder
	 * @param token           token autentikasi sesi Feeder
	 * @param session         sesi Hibernate aktif (diteruskan bila terjadi fallback ke update)
	 */
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

	/**
	 * Memperbarui data aktivitas kuliah mahasiswa (AKM) yang sudah tercatat di Feeder, dengan
	 * kunci {@code id_smt}+{@code id_reg_pd} diambil dari {@code jsonObject} yang diberikan.
	 *
	 * @param jsonObject      payload data AKM (wajib memuat {@code id_smt} dan {@code id_reg_pd})
	 * @param feederConnector klien komunikasi ke Feeder
	 * @param token           token autentikasi sesi Feeder
	 * @param session         sesi Hibernate aktif (tidak dipakai langsung, diteruskan untuk konsistensi API)
	 */
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
