package ais.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.servlet.api.DaftarDataService;
import ais.database.hibernate.HibernateUtil;

/**
 * Generator UI dinamis untuk dynamic table tab json generator. Kelas ini menerjemahkan
 * metadata/konfigurasi menjadi form, tabel, tab, atau wizard sehingga action tidak membangun ulang
 * struktur komponen yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah mutasi data ({@code jsonArrayToSet()}); operasi domain lain
 * ({@code generateWizardTable()}, {@code jsonArrayToStringArray()}, {@code jsonArrayToStringMatrix()}, {@code
 * generateWizardTableLogic()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> operasi membuat/mengubah komponen UI dan dapat membaca metadata atau data persistence.
 * Jalankan dengan desktop/session aktif dan gunakan generator ini sebagai satu sumber struktur dinamis, bukan
 * menyalin konstruksi widget ke action.</p>
 */
public class DynamicTableTabJsonGenerator {

    /**
     * Entry point utama menggunakan parameter JSON tunggal.
     */
    @SuppressWarnings("unchecked")
	public static String generateWizardTable(JSONObject params) throws Exception {
        // --- 1. Parsing JSON to Local Variables ---
        
        // Scalar Values
        String randomID = params.optString("randomID", "rnd" + System.currentTimeMillis());
        String dataTypeName = params.optString("dataTypeName", "Data");
        String modelClass = params.getString("modelClass");
        String sqlWhere = params.optString("sqlWhere", "");
        
        // Handling Hidden Values (Map<String, String>)
        Map<String, String> hiddenValues = new HashMap<String, String>();
        if (params.has("hiddenValues")) {
            JSONObject jsonHidden = params.getJSONObject("hiddenValues");
            Iterator<String> keys = jsonHidden.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                hiddenValues.put(key, jsonHidden.getString(key));
            }
        }

        // Arrays 1D
        String[] columns = jsonArrayToStringArray(params.optJSONArray("tableColumns"));
        String[] tableLabels = jsonArrayToStringArray(params.optJSONArray("tableLabels"));
        String[] searchCols = jsonArrayToStringArray(params.optJSONArray("searchCols"));
        String[] stepTitles = jsonArrayToStringArray(params.optJSONArray("stepTitles"));
        
        // Arrays 2D
        String[][] formCols = jsonArrayToStringMatrix(params.optJSONArray("formCols"));
        String[][] formLabels = jsonArrayToStringMatrix(params.optJSONArray("formLabels"));

        // Sets
        Set<String> paramRequired = jsonArrayToSet(params.optJSONArray("paramRequired"));
        Set<String> paramTidakBolehSama = jsonArrayToSet(params.optJSONArray("paramTidakBolehSama"));

        // --- 2. Execute Logic ---
        return generateWizardTableLogic(columns, tableLabels, searchCols, formCols, formLabels, stepTitles,
                randomID, dataTypeName, modelClass, paramRequired, paramTidakBolehSama, sqlWhere, hiddenValues);
    }

    // Helper: Convert JSONArray to String[]
    private static String[] jsonArrayToStringArray(JSONArray jsonArray) {
        if (jsonArray == null) return new String[0];
        String[] result = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            result[i] = jsonArray.optString(i, "");
        }
        return result;
    }

    // Helper: Convert Nested JSONArray to String[][]
    private static String[][] jsonArrayToStringMatrix(JSONArray jsonArray) {
        if (jsonArray == null) return new String[0][0];
        String[][] result = new String[jsonArray.length()][];
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray innerArray = jsonArray.optJSONArray(i);
            if (innerArray != null) {
                result[i] = jsonArrayToStringArray(innerArray);
            } else {
                result[i] = new String[0];
            }
        }
        return result;
    }

    // Helper: Convert JSONArray to Set<String>
    private static Set<String> jsonArrayToSet(JSONArray jsonArray) {
        Set<String> set = new HashSet<String>();
        if (jsonArray == null) return set;
        for (int i = 0; i < jsonArray.length(); i++) {
            set.add(jsonArray.optString(i));
        }
        return set;
    }

    /**
     * Core Logic (Private) - Ini berisi logika asli HTML Generation yang Anda berikan.
     * Dipisahkan agar method utama bersih hanya melakukan parsing JSON.
     */
    @SuppressWarnings("rawtypes")
    private static String generateWizardTableLogic(String[] columns, String[] tableLabels, String[] searchCols, 
                                             String[][] formCols, String[][] formLabels, String[] stepTitles,
                                             String randomID, String dataTypeName, 
                                             String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama, 
                                             String sqlWhere, Map<String,String> hiddenValues) throws Exception {
        
        Class clazz = Class.forName(modelClass);
        ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
        StringBuilder sb = new StringBuilder();
        
        // Flatten formCols for specific logic usages (like delete/save gathering)
        List<String> flatFormCols = new ArrayList<String>();
        for(String[] group : formCols) {
            for(String col : group) flatFormCols.add(col);
        }

        // --- 1. CSS & Style Wrapper ---
        sb.append("<div id='appWrapper_").append(randomID).append("'>\n");
        sb.append("<style>\n");
        sb.append(" .cursor-pointer { cursor: pointer; }\n");
        sb.append(" .sort-icon { margin-left: 5px; font-size: 0.8em; }\n");
        sb.append(" .nav-tabs .nav-link.has-error { color: #dc3545; border-color: #dc3545; }\n");
        sb.append("</style>\n");

        // --- 2. Header & Toolbar ---
        sb.append("<div class='container-fluid bg-white shadow-sm rounded p-4 mb-4'>\n");
        sb.append(" <div class='d-flex justify-content-between align-items-center mb-3'>\n");
        sb.append("    <h4 class='mb-0'><span class='fas fa-table text-primary'></span> Data ").append(dataTypeName).append("</h4>\n");
        sb.append("    <div class='btn-group'>\n");
        sb.append("      <button class='btn btn-outline-secondary' onclick='toggleSearch_").append(randomID).append("()' title='Cari Data'><span class='fas fa-search'></span></button>\n");
        sb.append("      <button class='btn btn-outline-secondary' onclick='refreshData_").append(randomID).append("()' title='Refresh'><span class='fas fa-sync'></span></button>\n");
        sb.append("      <button class='btn btn-primary' onclick='openModal_").append(randomID).append("(\"add\")'><span class='far fa-plus-square'></span> Tambah</button>\n");
        sb.append("    </div>\n");
        sb.append(" </div>\n");

        // --- 3. Search Panel ---
        sb.append(" <div id='searchPanel_").append(randomID).append("' class='card card-body bg-light mb-3' style='display:none;'>\n");
        sb.append("    <div class='row g-3'>\n");
        
        for (String col : searchCols) {
        	String returnName = null;
        	String type = "";
            try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; type=ss.length>1?ss[1]:"";  }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:146");}
			 
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            String textWithSpace = col.replaceAll("([a-z])([A-Z]+)", "$1 $2");
            String result = textWithSpace.substring(0, 1).toUpperCase() + textWithSpace.substring(1);
            String label = result;
            
             for (int i = 0; i < columns.length; i++) {
                 try {
                     if(col.equalsIgnoreCase(columns[i])) {
                         label=tableLabels[i];
                         break;
                     }
                 }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:164");}
             }
            
           
            
             

             if(col != null && !col.trim().isEmpty()) {
                sb.append("      <div class='col-md-4'>\n");
                sb.append("        <label class='form-label small text-muted'>Cari ").append(label).append("</label>\n");
                
                JSONArray colConf;
				if ((colConf = Common.validJsonArray(type))!=null) {
                    // --- RENDER COMBOBOX DARI JSON ARRAY (SEARCH PANEL) ---
                    sb.append("        <select class='form-select form-select-sm select2-search' id='search_").append(col).append("_").append(randomID).append("' onchange='triggerSearch_").append(randomID).append("()'>\n");
                    for (int k = 0; k < colConf.length(); k++) {
                        JSONObject opt = colConf.optJSONObject(k);
                        if (opt != null) {
                            sb.append("          <option value='").append(opt.opt("id")==null?"":opt.opt("id")).append("'>").append(opt.opt("nama")==null?"":opt.opt("nama")).append("</option>\n");
                        }
                    }
                    sb.append("        </select>\n");
                } else if(returnName != null && returnName.startsWith("ais.database.model")) {
                    String s = JSPCurdGenerator.pilihan(randomID, col, label, returnName, false, "id", "nama", "","search","triggerSearch_"+randomID+"();"); 
                    sb.append("            "+s+"\n");
                } else if (returnName != null && returnName.equals(Boolean.class.getName())) {
                    sb.append("        <select class='form-select form-select-sm' id='search_").append(col).append("_").append(randomID).append("' onchange='triggerSearch_").append(randomID).append("()'>\n");
                    sb.append("          <option value=''>Semua</option>\n");
                    sb.append("          <option value='true'>Aktif</option>\n");
                    sb.append("          <option value='false'>Tidak Aktif</option>\n");
                    sb.append("        </select>\n");
                } else {
                    sb.append("        <input type='text' class='form-control form-control-sm' id='search_").append(col).append("_").append(randomID).append("' onchange='triggerSearch_").append(randomID).append("()' placeholder='...'>\n");
                }
                sb.append("      </div>\n");
             }
        }
        
        if(hiddenValues != null) {
            for(String key : hiddenValues.keySet()) {
                 String val = hiddenValues.get(key);
                 sb.append("       <input type='hidden' value=\""+val+"\" id='search_"+key+"_").append(randomID).append("'>\n");
            }
        }
        sb.append("    </div>\n");
        sb.append(" </div>\n");

        // --- 4. Table ---
        sb.append(" <div class='table-responsive'>\n");
        sb.append("    <table class='table table-striped table-hover align-middle border'>\n");
        sb.append("      <thead class='table-light'>\n");
        sb.append("        <tr>\n");
        for (int i = 0; i < columns.length; i++) {
        	if(i == 0 && DaftarDataService.MAP_ENTITY_TO_FOTO.keySet().contains(modelClass)) {
        		sb.append("          <th>");
	            sb.append(" 			"+Common.getBahasaConfig("Foto")+"\n");
	            sb.append("          </th>\n");
        	}  
            sb.append("          <th class='cursor-pointer' onclick='changeSort_").append(randomID).append("(\"").append(columns[i]).append("\")'>\n");
            sb.append("            ").append(tableLabels[i]);
            sb.append(" <span id='sortIcon_").append(columns[i]).append("_").append(randomID).append("' class='sort-icon'></span>\n");
            sb.append("          </th>\n");
        }
        sb.append("          <th class='text-center' style='width: 50px;'></th>\n");
        sb.append("        </tr>\n");
        sb.append("      </thead>\n");
        sb.append("      <tbody id='tableBody_").append(randomID).append("'></tbody>\n");
        sb.append("    </table>\n");
        sb.append(" </div>\n");

        // --- 5. Pagination ---
        sb.append(" <div class='d-flex justify-content-between align-items-center mt-3'>\n");
        sb.append("    <span class='text-muted small' id='pageInfo_").append(randomID).append("'>"+Common.getBahasaConfig("Menampilkan")+"...</span>\n");
        sb.append("    <nav><ul class='pagination pagination-sm mb-0' id='pagination_").append(randomID).append("'></ul></nav>\n");
        sb.append(" </div>\n");
        sb.append("</div>\n"); 



        // --- 7. Modal Form (TABBED BASED) ---
        sb.append("<div class='modal fade' id='formModal_").append(randomID).append("' tabindex='-1' aria-hidden='true' data-bs-backdrop=\"static\">\n");
        sb.append(" <div class='modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable animate__animated animate__rotateInDownLeft'>\n");
        sb.append("    <div class='modal-content shadow-lg'>\n");
        sb.append("      <div class='modal-header' style=\"background-color: rgb(180 212 255) !important;\">\n");
        sb.append("        <h5 class='modal-title fw-bold' id='modalTitle_").append(randomID).append("'>Form Data</h5>\n");
        sb.append("        <button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button>\n");
        sb.append("      </div>\n");
        
        sb.append("      <div class='modal-body'>\n");
        sb.append("        <form id='dataForm_").append(randomID).append("'>\n");
        sb.append("          <input type='hidden' id='inputId_").append(randomID).append("'>\n");
        
        if(hiddenValues != null) {
            for(String key : hiddenValues.keySet()) {
                 String val = hiddenValues.get(key);
                 sb.append("       <input type='hidden' value=\""+val+"\" id='input_"+key+"_").append(randomID).append("'>\n");
            }
        }

        // --- TAB NAVIGATION (Bootstrap Native) ---
        sb.append("          <ul class='nav nav-tabs mb-3' id='tabList_").append(randomID).append("' role='tablist'>\n");
        for (int i = 0; i < formCols.length; i++) {
            String title = (i < stepTitles.length) ? stepTitles[i] : "Tab " + (i+1);
            String activeClass = (i == 0) ? "active" : "";
            sb.append("            <li class='nav-item' role='presentation'>\n");
            sb.append("              <button class='nav-link ").append(activeClass).append("' id='tabBtn_").append(randomID).append("_").append(i).append("' data-bs-toggle='tab' data-bs-target='#tabContent_").append(randomID).append("_").append(i).append("' type='button' role='tab' aria-selected='").append(i==0).append("'>");
            sb.append(title);
            sb.append("</button>\n");
            sb.append("            </li>\n");
        }
        sb.append("          </ul>\n");

        // --- TAB CONTENT ---
        sb.append("          <div class='tab-content' id='tabContentWrapper_").append(randomID).append("'>\n");
        
        // Loop Tabs
        for (int i = 0; i < formCols.length; i++) {
            String[] currentStepCols = formCols[i];
            String[] currentStepLabels = formLabels[i];
            String activeClass = (i == 0) ? "show active" : "";
            
            sb.append("            <div class='tab-pane fade ").append(activeClass).append("' id='tabContent_").append(randomID).append("_").append(i).append("' role='tabpanel' tabindex='0'>\n");
            sb.append("              <div class='row g-3'>\n");
            
            // Loop Fields in this Tab
            for (int j = 0; j < currentStepCols.length; j++) {
                String col = currentStepCols[j];
                String label = currentStepLabels[j];
                
                String colAsli = col;
                
                String type = "";
                String editor = "";
                 try {
                     String[] ss = col.split(";");
                     col = ss.length>0 ? ss[0] : "";
                     type = ss.length>1 ? ss[1] : "";
                     editor= ss.length>2 ? ss[2] : "";
                 }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:302");}
                 
                 if(col != null && !col.trim().isEmpty()) {
                     
                	// Layout
                     if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan")|| col.equalsIgnoreCase("catatan")|| col.equalsIgnoreCase("alamat")) {
                         sb.append("                      <div id='kolom_div_"+col+"_"+randomID+"' class='col-12'>\n");
                     } else {
                         sb.append("                      <div id='kolom_div_"+col+"_"+randomID+"' class='col-md-6'>\n");
                     }
                 	
                 	if (col.startsWith("file_") && Common.isValidJsonObject(type)) {
                 		
                 		
                 		
                 		// --- File generate nanti
                         
                         
                         
             		} else {
                     
             			String returnName = null;
    					
   					 
    		            try {
    						returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
    					}catch (Exception e) {
    						continue;
//    						e.printStackTrace();
    					}
	
	                     // Field Generation
    		            JSONArray colConf;
    					if ((colConf = Common.validJsonArray(type))!=null) {
    						sb.append("        <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                            sb.append("        <select class='form-select form-select-sm select2-search' id='input_").append(col).append("_").append(randomID).append("'>\n");
                            for (int k = 0; k < colConf.length(); k++) {
                                JSONObject opt = colConf.optJSONObject(k);
                                if (opt != null) {
                                    sb.append("          <option value='").append(opt.opt("id")==null?"":opt.opt("id")).append("'>").append(opt.opt("nama")==null?"":opt.opt("nama")).append("</option>\n");
                                }
                            }
                            sb.append("        </select>\n");
                        } else if(returnName != null && returnName.startsWith("ais.database.model")) {
	                    	 
	                    	 String whereTambahan = "";
	     					try {
	     						String[] cols = colAsli.split(";", 2);
	     						if(cols.length>1 && Common.isValidJsonObject(cols[1])) {
	     							JSONObject jsonObject = new JSONObject(cols[1]);
	     							if(!jsonObject.isNull("where")) {
	     								whereTambahan = " AND "+ jsonObject.get("where");
	     							}
	     						}
	     					}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:356");
	     						// TODO: handle exception
	     					}
	                 	 
	                    	 
	                         sb.append("                     <label class='form-label fw-semibold' for=\"input_nama_"+col+"_"+randomID+"\">").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
	                         String s = JSPCurdGenerator.pilihan(randomID, col, label, returnName, paramRequired.contains(col), "id", "nama", whereTambahan);  
	                         sb.append("                     "+s+"\n");
	                     }
	                     else if (type.equalsIgnoreCase("time")){
	                         sb.append("                     <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
	                         sb.append("                     <input type='time' class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
	                     }
	                     else if (type.equalsIgnoreCase("date") || (returnName != null && returnName.equals(Date.class.getName()))) {
	                        sb.append("                     <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
	                        sb.append("                     <input type='date' class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
	                    }
	                    else if (returnName != null && returnName.equals(Boolean.class.getName())) {
	                         sb.append("                     <div class='form-check form-switch mt-4'>\n");
	                         sb.append("                       <input class='form-check-input' type='checkbox' role='switch' id='input_").append(col).append("_").append(randomID).append("'>\n");
	                         sb.append("                       <label class='form-check-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
	                         sb.append("                     </div>\n");
	                     } else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  && editor.equalsIgnoreCase("editor")) {
	                    	 String idData = "input_"+col+"_"+randomID;
	                    	 JSPCurdGenerator.textEditor(sb, col, label,idData, paramRequired.contains(col), randomID,"");
	                     } else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan") || col.equalsIgnoreCase("alamat")) {
	                         sb.append("                     <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
	                         sb.append("                     <textarea class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" rows='3'></textarea>\n");
	                     } else {
	                         sb.append("                     <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
	                         sb.append("                     <input type='text' class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
	                     }
             		}
                     sb.append("                  </div>\n");
                 }
            } // End Loop Fields
            sb.append("              </div>\n");
            sb.append("            </div>\n"); // End Tab Pane
        } // End Loop Tabs
        sb.append("          </div>\n"); // End Tab Content

        sb.append("        </form>\n");
        sb.append("      </div>\n");
        
        // --- FOOTER BUTTONS ---
        sb.append("      <div class='modal-footer'>\n");
        sb.append("        <button type='button' class='btn btn-secondary' data-bs-dismiss='modal'>"+Common.getBahasaConfig("Batal")+"</button>\n");
        sb.append("        <button type='button' class='btn btn-success' id='btnSave_").append(randomID).append("' onclick='saveData_").append(randomID).append("()'>"+Common.getBahasaConfig("Simpan")+" <span class='fas fa-save'></span></button>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append(" </div>\n");
        sb.append("</div>\n");

        // --- 8. JAVASCRIPT LOGIC ---
        sb.append("<script>\n");
        sb.append(" var page_").append(randomID).append(" = 1;\n");
        sb.append(" var limit_").append(randomID).append(" = 10;\n");
        sb.append(" var sortCol_").append(randomID).append(" = 'id';\n");
        sb.append(" var sortOrder_").append(randomID).append(" = 'asc';\n");
        sb.append(" var currentMode_").append(randomID).append(" = 'add';\n");
        sb.append(" var modalInstance_").append(randomID).append(";\n");
        
        // --- TAB VALIDATION LOGIC ---
        // Validate a specific container (tab pane)
        sb.append(" function validateContainer_").append(randomID).append("(container) {\n");
        sb.append("    let inputs = container.querySelectorAll('[required]');\n");
        sb.append("    let isValid = true;\n");
        sb.append("    inputs.forEach(input => {\n");
        sb.append("       if(!input.value || input.value.trim() === '') {\n");
        sb.append("          input.classList.add('is-invalid');\n");
        sb.append("          isValid = false;\n");
        sb.append("          input.addEventListener('input', function() { this.classList.remove('is-invalid'); }, {once:true});\n");
        sb.append("       }\n");
        sb.append("    });\n");
        sb.append("    return isValid;\n");
        sb.append(" }\n");

        // Setup listener for Tab Switching
        sb.append(" document.addEventListener('DOMContentLoaded', () => {\n");
        sb.append("    var tabEls = document.querySelectorAll('#tabList_").append(randomID).append(" button[data-bs-toggle=\"tab\"]');\n");
        sb.append("    tabEls.forEach(function(tabEl) {\n");
        sb.append("      tabEl.addEventListener('hide.bs.tab', function (event) {\n");
        sb.append("         // Validasi Tab yang sedang aktif (sebelum pindah)\n");
        sb.append("         const activeTarget = event.target.getAttribute('data-bs-target');\n"); // Tab yang mau ditinggalkan
        sb.append("         const activePane = document.querySelector(activeTarget);\n");
        sb.append("         if(activePane && !validateContainer_").append(randomID).append("(activePane)) {\n");
        sb.append("             event.preventDefault(); // Batalkan perpindahan\n");
        sb.append("             event.stopPropagation();\n");
        sb.append("             tampilkanToast('"+Common.getBahasaConfig("Harap lengkapi field bertanda bintang pada tab ini sebelum pindah")+"', 'bg-warning');\n");
        sb.append("         }\n");
        sb.append("      });\n");
        sb.append("    });\n");
        sb.append(" });\n");

        sb.append(" const isEmpty"+randomID+" = (val) => {\r\n"
                + "  return (val === undefined || val === null || (typeof val === \"string\" && val.trim().length === 0) || (Array.isArray(val) && val.length === 0));\r\n"
                + "};\n");

        // --- CRUD LOGIC ---
        
        sb.append(" async function loadData_").append(randomID).append("() {\n");
        sb.append("    const start = (page_").append(randomID).append(" - 1);\n");
        sb.append("    let whereClause = '';\n");
        sb.append("    let criteria = [];\n");
        for (String col : searchCols) {
        	String returnName = null;
			
			 
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            String elId = "search_" + col + "_" + randomID;
           
            try { String[] ss = col.split(";"); col = ss.length>0 ? ss[0] : ""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:472");}
            
            if(col != null && !col.trim().isEmpty()) {
                if(returnName != null && returnName.startsWith("ais.database.model")) {
                     sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
                     sb.append("   if(val").append(col).append(") criteria.push(\"").append(col).append(" = \" + val").append(col).append(" + \" \");\n");
                }
                else if (returnName != null && returnName.equals(Boolean.class.getName())) {
                     sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
                     sb.append("   if(val").append(col).append(" !== '') criteria.push(\"").append(col).append(" = \" + val").append(col).append(");\n");
                } else {
                     sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
                     sb.append("   if(val").append(col).append(") criteria.push(\"").append(col).append(" ilike '%\" + val").append(col).append(" + \"%' \");\n");
                }
            }
        }
        sb.append("    if(criteria.length > 0) whereClause = criteria.join(' AND ');\n");
        
        StringBuilder projection = new StringBuilder("id");
        List<String> stringsCols = new ArrayList<String>();
        for(String col : columns) if(!stringsCols.contains(col)) stringsCols.add(col);
        for(String col : flatFormCols) if(!stringsCols.contains(col)) stringsCols.add(col);
        for(String col : stringsCols) if(!col.equals("id")) projection.append(";").append(col);
        
        sb.append("    var reqObj = {\n");
        sb.append("      \"action\": \"daftar\", \"class\": \"").append(modelClass).append("\",\n");
        sb.append("      \"projection\": \"").append(projection.toString()).append("\",\n");
        sb.append("      \"deep\": \"1\", \"max\": limit_").append(randomID).append(", \"halaman\": start,\n");
        sb.append("      \"order1\": sortOrder_").append(randomID).append(", \"sort1\": sortCol_").append(randomID).append(", \"count\": \"true\"\n");
        sb.append("    };\n");
        sb.append("    if(whereClause) reqObj[\"where1\"] = whereClause;\n");
        if(!sqlWhere.isEmpty()) sb.append("    reqObj[\"where2\"] = \""+sqlWhere+"\";\n");
        
        sb.append("    var req = JSON.stringify(reqObj);\n");
        sb.append("    var dataReq = encodeURIComponent(req);\n");
        sb.append("    const servletUrl = '"+Common.ROOT+"/Data?datasearch=' + dataReq;\n");
        sb.append("    try {\n");
        sb.append("      const response = await fetch(servletUrl);\n");
        sb.append("      const dataResponse = await response.json();\n");
        sb.append("      renderTable_").append(randomID).append("(dataResponse.data || []);\n");
        sb.append("      renderPagination_").append(randomID).append("(dataResponse.count || 0);\n");
        sb.append("      document.getElementById('pageInfo_").append(randomID).append("').innerText = 'Total: ' + (dataResponse.count || 0);\n");
        sb.append("    } catch (error) { console.error(error); }\n");
        sb.append(" }\n");

        // --- RENDER TABLE ---
        sb.append(" function renderTable_").append(randomID).append("(data) {\n");
        sb.append("    const tbody = document.getElementById('tableBody_").append(randomID).append("');\n");
        sb.append("    tbody.innerHTML = '';\n");
        sb.append("    if (data.length === 0) { tbody.innerHTML = '<tr><td colspan=\"20\" class=\"text-center\">"+Common.getBahasaConfig("Tidak ada data")+"</td></tr>'; return; }\n");
        sb.append("    data.forEach(item => {\n");
        sb.append("      let tr = document.createElement('tr');\n");
        int index = 0;
        for (String col : columns) {
        	
        	   if(index == 0 && DaftarDataService.MAP_ENTITY_TO_FOTO.keySet().contains(modelClass)) {
              	 sb.append("      let td_foto_").append(col).append(" = document.createElement('td');\n");
              	 sb.append("      td_foto_").append(col).append(".innerHTML = `").append("<img onclick=\"tampilGambar(this)\" \r\n"
              	 		+ "		                             src=\"`+item.foto_kecil+`\" \r\n"
              	 		+ "		                             class=\"rounded-4 shadow-sm border border-2 border-white\" \r\n"
              	 		+ "		                             alt=\"`+item.nama+`\" \r\n"
              	 		+ "		                             style=\"width: 85px; height: 85px; object-fit: cover; cursor: pointer;\"\r\n"
              	 		+ "		                             title=\"Klik untuk zoom\">").append("`;\n");
              	sb.append("      tr.appendChild(td_foto_").append(col).append(");\n");
               }
        	   index++;
        	String returnName = null;
			
			 
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
             try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:547");}
             sb.append("      let td_").append(col).append(" = document.createElement('td');\n");
             if (returnName != null && returnName.equals(Boolean.class.getName())) {
                 sb.append("      let badgeClass"+col+" = item.").append(col).append("=='true' ? 'bg-success' : 'bg-secondary';\n");
                 sb.append("      let badgeText"+col+" = item.").append(col).append("=='true' ? 'Aktif' : 'Tidak Aktif';\n");
                 sb.append("      td_").append(col).append(".innerHTML = `<span class='badge ${badgeClass"+col+"}'>${badgeText"+col+"}</span>`;\n");
                 sb.append("      td_").append(col).append(".className = 'text-center';\n");
             } else {
                 sb.append("      td_").append(col).append(".innerText = item.").append(col).append(" || '-';\n");
             }
             sb.append("      tr.appendChild(td_").append(col).append(");\n");
        }
        sb.append("      let tdAction = document.createElement('td'); tdAction.className = 'text-center';\n");
        sb.append("      let itemStr = encodeURIComponent(JSON.stringify(item));\n");
        sb.append("      tdAction.innerHTML = `\n");
        sb.append("        <div class=\"dropdown font-sans-serif position-static\">\r\n");
        sb.append("          <button class=\"btn btn-link text-600 btn-sm dropdown-toggle btn-reveal\" type=\"button\" title=\"" + Common.getBahasaConfig("Aksi") + "\" aria-label=\"" + Common.getBahasaConfig("Aksi") + "\" data-bs-toggle=\"dropdown\"><span class=\"fas fa-ellipsis-h fs-10\"></span></button>\r\n");
        sb.append("          <div class=\"dropdown-menu dropdown-menu-end border py-0\">\r\n");
        sb.append("            <div class=\"py-2\">\n");
        sb.append("              <button class='dropdown-item' onclick='openModal_").append(randomID).append("(\"edit\", \"${itemStr}\")'><span class='fas fa-edit'></span> Ubah</button>\n");
        sb.append("              <button class='dropdown-item text-danger' data-id=\"${item.id}\" onclick=\"prosesDeleteData('"+modelClass+"', '${item.id}', function(){ loadData_"+randomID+"(); });\"><span class='fas fa-trash'></span> Hapus</button>\n");
        sb.append("            </div>\r\n");
        sb.append("          </div>\r\n");
        sb.append("        </div>`;\n");
        sb.append("      tr.appendChild(tdAction);\n");
        sb.append("      tbody.appendChild(tr);\n");
        sb.append("    });\n");
        sb.append(" }\n");

        // --- SAVE DATA (With Tab Check) ---
        sb.append(" async function saveData_").append(randomID).append("() {\n");
        
        // 1. Loop through ALL tabs to validate
        sb.append("    let allValid = true;\n");
        sb.append("    let firstInvalidTabTrigger = null;\n");
        
        for(int i=0; i<formCols.length; i++) {
             sb.append("    if(allValid) { \n");
             sb.append("       let pane").append(i).append(" = document.getElementById('tabContent_").append(randomID).append("_").append(i).append("');\n");
             sb.append("       if(!validateContainer_").append(randomID).append("(pane").append(i).append(")) {\n");
             sb.append("          allValid = false;\n");
             sb.append("          firstInvalidTabTrigger = document.getElementById('tabBtn_").append(randomID).append("_").append(i).append("');\n");
             sb.append("       }\n");
             sb.append("    }\n");
        }
        
        sb.append("    if(!allValid) {\n");
        sb.append("       tampilkanToast('"+Common.getBahasaConfig("Ada data yang belum lengkap. Harap periksa semua tab.")+"', 'bg-danger');\n");
        sb.append("       if(firstInvalidTabTrigger) { \n");
        sb.append("          // Switch to invalid tab manually using Bootstrap API\n");
        sb.append("          const tabInstance = bootstrap.Tab.getOrCreateInstance(firstInvalidTabTrigger);\n");
        sb.append("          tabInstance.show();\n");
        sb.append("       }\n");
        sb.append("       return;\n");
        sb.append("    }\n");

        // 2. Collect Data
        sb.append("    let dataObj = {};\n");
        sb.append("    let idVal = document.getElementById('inputId_").append(randomID).append("').value;\n");
        sb.append("    if(currentMode_").append(randomID).append(" === 'edit' && idVal) dataObj.id = parseInt(idVal);\n");
        
        if(hiddenValues != null) {
            for(String key : hiddenValues.keySet()) {
                 sb.append("    dataObj.").append(key).append(" = document.getElementById('input_"+key+"_").append(randomID).append("').value;\n");
            }
        }
        
        for (String col : flatFormCols) {
        	String returnName = null;
			
			 
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:624");}
            if(col!=null && !col.trim().isEmpty()){
                if (returnName != null && returnName.equals(Boolean.class.getName())) {
                    sb.append("    dataObj.").append(col).append(" = document.getElementById('input_").append(col).append("_").append(randomID).append("').checked;\n");
                } else {
                    sb.append("    dataObj.").append(col).append(" = document.getElementById('input_").append(col).append("_").append(randomID).append("').value;\n");
                }
            }
        }
        
        String tidakBolehSama = "";
        for(String s : paramTidakBolehSama) tidakBolehSama += tidakBolehSama.isEmpty() ? s : ";"+s;
        
        sb.append("    var reqObj = {\n");
        sb.append("      \"action\": \"simpanDataRinci\", \"id\": dataObj.id==null ? -1 : dataObj.id,\n");
        sb.append("      \"tidakBolehSama\": \""+tidakBolehSama+"\", \"class\": \"").append(modelClass).append("\", \"data\": dataObj\n");
        sb.append("    };\n");
        sb.append("    var req = JSON.stringify(reqObj);\n");
        sb.append("    var dataReq = encodeURIComponent(req);\n");
        sb.append("    const servletUrl = '"+Common.ROOT+"/Data?datasearch=' + dataReq;\n");
        sb.append("    try {\n");
        sb.append("      const response = await fetch(servletUrl);\n");
        sb.append("      const dataResponse = await response.json();\n");
        sb.append("      if(dataResponse.status=='00'){ \n");
        sb.append("        updateBulkFiles(dataResponse.id);\n");
        sb.append("        tampilkanToast('"+Common.getBahasaConfig("Berhasil disimpan")+"', 'bg-success');\n");
        sb.append("        modalInstance_").append(randomID).append(".hide();\n");
        sb.append("        loadData_").append(randomID).append("();\n");
        sb.append("      } else { tampilkanToast(dataResponse.description, 'bg-danger'); }\n");
        sb.append("    } catch (e) { tampilkanToast('Gagal: ' + e, 'bg-danger'); }\n");
        sb.append(" }\n");
        
        // --- DELETE DATA ---
        sb.append(" async function deleteData_").append(randomID).append("() {\n");
        sb.append("    var id = document.getElementById('inputHapusId_"+randomID+"').value;");
        sb.append("    var reqObj = { \"action\": \"hapusDataRinci\", \"class\": \"").append(modelClass).append("\", \"id\": id };\n");
        sb.append("    var req = JSON.stringify(reqObj);\n");
        sb.append("    var dataReq = encodeURIComponent(req);\n");
        sb.append("    const servletUrl = '"+Common.ROOT+"/Data?datasearch=' + dataReq;\n");
        sb.append("    try {\n");
        sb.append("      const response = await fetch(servletUrl);\n");
        sb.append("      const dataResponse = await response.json();\n");
        sb.append("      if(dataResponse.status=='00'){ \n");
        sb.append("        tampilkanToast('"+Common.getBahasaConfig("Berhasil dihapus")+"', 'bg-success');\n");
        sb.append("        const instanceModal = bootstrap.Modal.getInstance(modalKonfirmasi"+randomID+"); instanceModal.hide();\n");
        sb.append("        loadData_").append(randomID).append("();\n");
        sb.append("      } else { tampilkanToast(dataResponse.description, 'bg-danger'); }\n");
        sb.append("    } catch (e) { tampilkanToast('Gagal hapus: ' + e, 'bg-danger'); }\n");
        sb.append(" }\n");
        
        // --- HELPER FUNCTIONS ---
        sb.append(" function toggleSearch_").append(randomID).append("() { var p = document.getElementById('searchPanel_").append(randomID).append("'); p.style.display = (p.style.display === 'none') ? 'block' : 'none'; }\n");
        sb.append(" function triggerSearch_").append(randomID).append("() { page_").append(randomID).append(" = 1; loadData_").append(randomID).append("(); }\n");
        sb.append(" function refreshData_").append(randomID).append("() { page_").append(randomID).append(" = 1; loadData_").append(randomID).append("(); }\n");
        sb.append(" function changeSort_").append(randomID).append("(col) { \n");
        sb.append("     if(sortCol_").append(randomID).append(" === col) sortOrder_").append(randomID).append(" = (sortOrder_").append(randomID).append(" === 'asc') ? 'desc' : 'asc';\n");
        sb.append("     else { sortCol_").append(randomID).append(" = col; sortOrder_").append(randomID).append(" = 'asc'; }\n");
        sb.append("     loadData_").append(randomID).append("();\n");
        sb.append(" }\n");

        // --- OPEN MODAL (Logic Reset & Fill for Tabs) ---
        sb.append(" function openModal_").append(randomID).append("(mode, itemEncoded) {\n");
        sb.append("    currentMode_").append(randomID).append(" = mode;\n");
        sb.append("    document.getElementById('dataForm_").append(randomID).append("').reset();\n");
        sb.append("    // Reset Tab to first\n");
        sb.append("    const firstTabBtn = document.querySelector('#tabList_").append(randomID).append(" button:first-child');\n");
        sb.append("    if(firstTabBtn) bootstrap.Tab.getOrCreateInstance(firstTabBtn).show();\n");
        sb.append("    // Reset Validation visual\n");
        sb.append("    document.querySelectorAll('.is-invalid').forEach(e => e.classList.remove('is-invalid'));\n");
        
        sb.append("    document.getElementById('modalTitle_").append(randomID).append("').innerText = (mode === 'add' ? 'Tambah' : 'Edit') + ' Data "+dataTypeName+"';\n");
        
        sb.append("    if(mode === 'edit' && itemEncoded) {\n");
        sb.append("      let item = JSON.parse(decodeURIComponent(itemEncoded));\n");
        sb.append("      document.getElementById('inputId_").append(randomID).append("').value = item.id;\n");
        
        // Fill Data Loop
        for(String col : flatFormCols) {
        	String returnName = null;
			
			 
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            String type = "";
            try {
                String[] ss = col.split(";");
                col = ss.length>0 ? ss[0] : "";
                type = ss.length>1 ? ss[1] : "";
            }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableTabJsonGenerator.java:716");}
            
            if(col!=null && !col.trim().isEmpty()){
            	if (col.equalsIgnoreCase("file") && Common.isValidJsonObject(type)) {
            		JSPCurdGenerator. renderUploadFile( sb,  type, col,  randomID);
        		} else  if(returnName.startsWith("ais.database.model")) {
                    sb.append("      document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_id)  ? item."+col+"_id : -1);\n");
                    sb.append("      document.getElementById('input_nama_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_nama) ? item."+col+"_nama : '');\n");
                }
                else if (type.equalsIgnoreCase("time")){
                    sb.append("      document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_time) ? item."+col+"_time : '');\n");
                }
                
                else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan") || col.equalsIgnoreCase("alamat")) {
	 				sb.append("     document.getElementById('input_").append(col).append("_").append(randomID).append("').innerHTML = (!isEmpty"+randomID+"(item.").append(col).append(") ? item."+col+" : '');\n");
	 			}
                
                else if (type.equalsIgnoreCase("date") || returnName.equals(Date.class.getName())) {
                    sb.append("      document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_tgl) ? item."+col+"_tgl : '');\n");
                }
                else if (returnName.equals(Boolean.class.getName())) {
                     sb.append("      document.getElementById('input_").append(col).append("_").append(randomID).append("').checked = item.").append(col).append("=='true';\n");
                } else {
                     sb.append("      document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append(") ? item."+col+" : '');\n");
                }
            }
        }
        sb.append("    }\n");
        
        sb.append("    var modalEl = document.getElementById('formModal_").append(randomID).append("');\n");
        sb.append("    modalInstance_").append(randomID).append(" = new bootstrap.Modal(modalEl);\n");
        sb.append("    const dialog = modalEl.querySelector('.modal-dialog');\n");
        sb.append("    dialog.classList.remove('animate__rotateInDownLeft');\n");
        sb.append("    void dialog.offsetWidth;\n");
        sb.append("    dialog.classList.add('animate__rotateInDownLeft');\n");
        sb.append("    modalInstance_").append(randomID).append(".show();\n");
        sb.append(" }\n");

        sb.append(" function renderPagination_").append(randomID).append("(totalItems) {\n");
        sb.append("    const totalPages = Math.ceil(totalItems / limit_").append(randomID).append(");\n");
        sb.append("    const container = document.getElementById('pagination_").append(randomID).append("'); container.innerHTML = '';\n");
        sb.append("    if(totalPages <= 1) return;\n");
        sb.append("    const addBtn = (label, pageNum, active, disabled) => {\n");
        sb.append("      let li = document.createElement('li'); li.className = 'page-item ' + (active?'active ':'') + (disabled?'disabled':'');\n");
        sb.append("      li.innerHTML = `<a class='page-link cursor-pointer' onclick='if(!${disabled}) { page_").append(randomID).append(" = ${pageNum}; loadData_").append(randomID).append("(); }'>${label}</a>`;\n");
        sb.append("      container.appendChild(li);\n");
        sb.append("    };\n");
        sb.append("    addBtn('Prev', page_").append(randomID).append("-1, false, page_").append(randomID).append("===1);\n");
        sb.append("    for(let i=1; i<=totalPages; i++) if(i===1||i===totalPages||(i>=page_").append(randomID).append("-1 && i<=page_").append(randomID).append("+1)) addBtn(i, i, page_").append(randomID).append("===i, false);\n");
        sb.append("    addBtn('Next', page_").append(randomID).append("+1, false, page_").append(randomID).append("===totalPages);\n");
        sb.append(" }\n");

        sb.append(" document.addEventListener('DOMContentLoaded', () => { loadData_").append(randomID).append("(); });\n");
        sb.append("</script>\n");
        
        sb.append("</div>\n"); 
        return sb.toString();
    }
}