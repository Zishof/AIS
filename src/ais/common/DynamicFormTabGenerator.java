package ais.common;

import java.util.Map;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

public class DynamicFormTabGenerator {

	/**
	 * Entry point sederhana
	 */
	public static String generateForm(String[][] labels, String[][] formCols, String[] stepTitles, String randomID,
			String dataTypeName, String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama,
			GeneralValueObject generalValueObject, String afterSave) throws Exception {
		return generateForm(labels, formCols, stepTitles, randomID, dataTypeName, modelClass, paramRequired,
				paramTidakBolehSama, generalValueObject, null, afterSave);
	}

	/**
	 * Generator Form Berbasis Tabs Bootstrap (Modified)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static String generateForm(String[][] labels, String[][] formCols, String[] stepTitles, String randomID,
			String dataTypeName, String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama,
			GeneralValueObject generalValueObject, Map<String, String> hiddenValues, String afterSave)
			throws Exception {

		// 1. Inisialisasi Metadata & Data
		Class clazz = Class.forName(modelClass);
		ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);

		// Refresh session jika object ada
		if (generalValueObject != null && generalValueObject.getId() != null) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				session.refresh(generalValueObject);
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DynamicFormTabGenerator.java:43");
			}
		}

		StringBuilder sb = new StringBuilder();
		int totalTabs = formCols.length;
		Long objId = (generalValueObject == null || generalValueObject.getId() == null) ? -1L
				: generalValueObject.getId();

		sb.append("<div id='tabWrapper_").append(randomID).append("'>\n");

		// 2. Container Card
		sb.append("<div class='card shadow-sm border-0'>\n");

		// --- HEADER ---
		sb.append("  <div class='card-header bg-white border-bottom pt-3'>\n");
		sb.append("    <div class='d-flex justify-content-between align-items-center mb-3'>\n");
		sb.append("       <h5 class='m-0 fw-bold text-primary'><i class='fas fa-folder-open me-2'></i>")
				.append(dataTypeName).append("</h5>\n");
		sb.append(
				"       <span class='badge bg-light text-secondary rounded-pill border'>"
						+ (generalValueObject == null || generalValueObject.getId() == null ? "Data Baru"
								: "Terakhir update "
										+ Common.dateFormat31.get().format(generalValueObject.getTanggal_dirubah()))
						+ "</span>\n");
		sb.append("    </div>\n");

		// --- NAVIGATION TABS (Bootstrap Pure) ---
		sb.append("    <ul class='nav nav-tabs card-header-tabs' id='myTab_").append(randomID)
				.append("' role='tablist'>\n");

		for (int i = 0; i < totalTabs; i++) {
			String title = (stepTitles != null && stepTitles.length > i) ? stepTitles[i] : "Tab " + (i + 1);
			boolean isActive = (i == 0);

			sb.append("      <li class='nav-item' role='presentation'>\n");
			sb.append("        <button class='nav-link ").append(isActive ? "active" : "").append("' ")
					.append("id='tabBtn_").append(i).append("_").append(randomID).append("' ")
					.append("type='button' role='tab' ").append("onclick='switchTab_").append(randomID).append("(")
					.append(i).append(")'>\n");

			sb.append("          <span class='badge rounded-pill bg-secondary me-1' id='badge_").append(i).append("_")
					.append(randomID).append("'>").append(i + 1).append("</span> ");
			sb.append(title);
			sb.append("        </button>\n");
			sb.append("      </li>\n");
		}
		sb.append("    </ul>\n");
		sb.append("  </div>\n"); 

		// --- BODY: Tab Content ---
		sb.append("  <div class='card-body'>\n");

		sb.append(JSPCurdGenerator.pilihanValSemuaTab(randomID, formCols, objId, hiddenValues, labels, classMetadata,
				generalValueObject, paramRequired));

		sb.append("  </div>\n");

		// --- FOOTER: Action Buttons ---
		sb.append("  <div class='card-footer bg-white border-top py-3'>\n");
		sb.append("    <div class='d-flex justify-content-end gap-2'>\n");
		sb.append("      <button type='button' class='btn btn-light border' id='btnPrev_").append(randomID)
				.append("' onclick='prevTab_").append(randomID)
				.append("()' style='display:none;'>&laquo; Sebelumnya</button>\n");
		sb.append("      <button type='button' class='btn btn-primary' id='btnNext_").append(randomID)
				.append("' onclick='nextTab_").append(randomID).append("()'>Selanjutnya &raquo;</button>\n");
		sb.append("      <button type='button' class='btn btn-success' id='btnSave_").append(randomID)
				.append("' onclick='saveData_").append(randomID)
				.append("()' style='display:none;'><i class='fas fa-save me-1'></i> Simpan Data</button>\n");
		sb.append("    </div>\n");
		sb.append("  </div>\n"); 

		sb.append("</div>\n"); 
		sb.append("</div>\n"); 

		// --- JAVASCRIPT LOGIC ---
		sb.append("<script>\n");
		sb.append("(function(){\n");
		sb.append("  let currTab = 0;\n");
		sb.append("  const totalTabs = ").append(totalTabs).append(";\n");
		sb.append("  const rID = '").append(randomID).append("';\n");

		sb.append("  const isEmpty" + randomID
				+ " = (v) => (v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));\n");

		sb.append("  window.updateUI_").append(randomID).append(" = function() {\n");
		sb.append("    for(let i=0; i<totalTabs; i++) {\n");
		sb.append("      let btn = document.getElementById('tabBtn_' + i + '_' + rID);\n");
		sb.append(
				"      if(i === currTab) { btn.classList.add('active'); btn.classList.add('text-primary'); } else { btn.classList.remove('active'); btn.classList.remove('text-primary'); }\n");

		sb.append("      let pane = document.getElementById('tabPane_' + i + '_' + rID);\n");
		sb.append("      if(i === currTab) { \n");
		sb.append("         pane.classList.add('show'); pane.classList.add('active'); \n");
		sb.append("      } else { \n");
		sb.append("         pane.classList.remove('show'); pane.classList.remove('active'); \n");
		sb.append("      }\n");

		sb.append("      let badge = document.getElementById('badge_' + i + '_' + rID);\n");
		sb.append(
				"      if(i < currTab) { badge.classList.replace('bg-secondary', 'bg-success'); } else if(i === currTab) { badge.classList.replace('bg-success', 'bg-primary'); badge.classList.replace('bg-secondary', 'bg-primary'); } else { badge.className = 'badge rounded-pill bg-secondary me-1'; }\n");
		sb.append("    }\n");

		sb.append(
				"    document.getElementById('btnPrev_' + rID).style.display = (currTab === 0) ? 'none' : 'inline-block';\n");
		sb.append("    if(currTab === totalTabs - 1) {\n");
		sb.append("      document.getElementById('btnNext_' + rID).style.display = 'none';\n");
		sb.append("      document.getElementById('btnSave_' + rID).style.display = 'inline-block';\n");
		sb.append("    } else {\n");
		sb.append("      document.getElementById('btnNext_' + rID).style.display = 'inline-block';\n");
		sb.append("      document.getElementById('btnSave_' + rID).style.display = 'none';\n");
		sb.append("    }\n");
		sb.append("  };\n");

		// --- VALIDATION FUNCTION ---
		sb.append("  window.validateTab_").append(randomID).append(" = function(tabIdx) {\n");
		for (int i = 0; i < formCols.length; i++) {
			sb.append("    if(tabIdx === ").append(i).append(") {\n");
			for (String col : formCols[i]) {
				String pureCol = col.split(";")[0];
				if (paramRequired.contains(pureCol)) {
					// PERBAIKAN PENTING: Pengamanan pengecekan DOM Element
					sb.append("      let el = document.getElementById('input_").append(pureCol).append("_' + rID);\n");
					sb.append("      if(el && isEmpty" + randomID + "(el.value)) {\n");
					sb.append(
							"         if(typeof tampilkanToast === 'function') tampilkanToast('Mohon lengkapi data di Tab ini sebelum melanjutkan!', 'bg-danger'); else alert('Mohon lengkapi data');\n");
					sb.append("         el.focus(); return false;\n");
					sb.append("      }\n");
				}
			}
			sb.append("    }\n");
		}
		sb.append("    return true;\n");
		sb.append("  };\n");

		// --- NAVIGATION LOGIC ---
		sb.append("  window.switchTab_").append(randomID).append(" = function(targetIdx) {\n");
		sb.append("    if(targetIdx === currTab) return;\n");
		sb.append("    if(!validateTab_").append(randomID).append("(currTab)) return;\n");
		sb.append("    currTab = targetIdx;\n");
		sb.append("    updateUI_").append(randomID).append("();\n");
		sb.append("  };\n");

		sb.append("  window.nextTab_").append(randomID).append(" = function() {\n");
		sb.append("    if(currTab < totalTabs - 1) switchTab_").append(randomID).append("(currTab + 1);\n");
		sb.append("  };\n");

		sb.append("  window.prevTab_").append(randomID).append(" = function() {\n");
		sb.append("    if(currTab > 0) switchTab_").append(randomID).append("(currTab - 1);\n");
		sb.append("  };\n");

		// --- SAVE DATA LOGIC ---
		sb.append("  window.saveData_").append(randomID).append(" = async function() {\n");

		sb.append("    for(let i=0; i<totalTabs; i++) {\n");
		sb.append("       if(!validateTab_").append(randomID).append("(i)) {\n");
		sb.append("          currTab = i;\n");
		sb.append("          updateUI_").append(randomID).append("();\n");
		sb.append("          return;\n"); 
		sb.append("       }\n");
		sb.append("    }\n");

		sb.append("    let d = {};\n");
		sb.append("    d.id = document.getElementById('inputId_' + rID).value;\n");

		if (hiddenValues != null) {
			for (String k : hiddenValues.keySet())
				sb.append("    let hEl_").append(k).append(" = document.getElementById('input_").append(k).append("_' + rID);\n")
				  .append("    if(hEl_").append(k).append(") d.").append(k).append(" = hEl_").append(k).append(".value;\n");
		}

		// PERBAIKAN PENTING: Pengecekan Eksistensi DOM Element sebelum memanggil '.value' / '.checked'
		for (String[] stepCols : formCols) {
			for (String col : stepCols) {
				String pureCol = col.split(";")[0];
				if (pureCol.isEmpty())
					continue;
				String retType = "";
				try {
					retType = classMetadata.getPropertyType(pureCol).getReturnedClass().getName();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicFormTabGenerator.java:223");
				}

				sb.append("    let valEl_").append(pureCol).append(" = document.getElementById('input_").append(pureCol).append("_' + rID);\n");
				sb.append("    if(valEl_").append(pureCol).append(") {\n");
				if (Boolean.class.getName().equals(retType)) {
					sb.append("        d.").append(pureCol).append(" = valEl_").append(pureCol).append(".checked;\n");
				} else {
					sb.append("        d.").append(pureCol).append(" = valEl_").append(pureCol).append(".value;\n");
				}
				sb.append("    }\n");
			}
		}

		int indexJ = 0;
		for (String[] stepCols : formCols) {
			int index = 0;
			for (String col : stepCols) {
				String label = col;
				try {
					label = labels[indexJ][index];
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicFormTabGenerator.java:244");
				}
				if (label == null) label = col;

				String pureCol = col.split(";")[0];
				if (pureCol != null && !pureCol.trim().isEmpty()) {
					if (paramRequired.contains(pureCol)) {
						// PERBAIKAN PENTING: Pengamanan tambahan required check
						sb.append("    let reqEl_").append(pureCol).append(" = document.getElementById('input_").append(pureCol).append("_' + rID);\n");
						sb.append("    if(reqEl_").append(pureCol).append(" && isEmpty" + randomID + "(reqEl_").append(pureCol).append(".value)) { tampilkanToast('")
								.append(Common.getBahasaConfig(label)).append(" wajib diisi!', 'bg-danger'); return; }\n");
					}
				}
				index++;
			}
			indexJ++;
		}

		String tidakBolehSama = "";
		for (String s : paramTidakBolehSama)
			tidakBolehSama += tidakBolehSama.isEmpty() ? s : ";" + s;

		sb.append("    let reqObj = { action: 'simpanDataRinci', id: (d.id || -1), tidakBolehSama: '")
				.append(tidakBolehSama).append("', class: '").append(modelClass).append("', data: d };\n");
		sb.append("    try {\n");
		sb.append("      let res = await fetch('").append(Common.ROOT)
				.append("/Data?datasearch=' + encodeURIComponent(JSON.stringify(reqObj)));\n");
		sb.append("      let json = await res.json();\n");
		sb.append("      if(json.status === '00') {\n");
		
		sb.append("      updateBulkFiles(json.id);\n");
		
		sb.append(
				"        if(typeof tampilkanToast === 'function') tampilkanToast('Berhasil tersimpan!', 'bg-success');\n"
						+ afterSave + "\n");
		sb.append(
				"        let modalEl = document.getElementById(rID) || document.getElementById('modal_' + rID) || document.querySelector('.modal.show');\n");
		sb.append(
				"        if(modalEl && window.bootstrap) { let mi = bootstrap.Modal.getInstance(modalEl); if(mi) mi.hide(); }\n");
		sb.append(
				"      } else { if(typeof tampilkanToast === 'function') tampilkanToast(json.description, 'bg-danger'); else alert(json.description); }\n");
		sb.append(
				"    } catch(e) { console.error(e); if(typeof tampilkanToast === 'function') tampilkanToast('Error: ' + e, 'bg-danger'); }\n");
		sb.append("  };\n");

		sb.append("})();\n");
		sb.append("</script>\n");

		return sb.toString();
	}
}