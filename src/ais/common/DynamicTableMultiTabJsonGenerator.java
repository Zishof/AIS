package ais.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.servlet.api.DaftarDataService;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Program;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Generator UI dinamis untuk dynamic table multi tab json generator. Kelas ini menerjemahkan
 * metadata/konfigurasi menjadi form, tabel, tab, atau wizard sehingga action tidak membangun ulang
 * struktur komponen yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code load()}); mutasi data ({@code
 * jsonArrayToSet()}, {@code save()}); pelaporan/ekspor ({@code render()}); operasi domain lain ({@code
 * generateWizardTable()}, {@code jsonArrayToStringArray()}, {@code jsonArrayToStringMatrix()}, {@code
 * generateWizardTableLogic()}, {@code paging()}, {@code modal()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> operasi membuat/mengubah komponen UI dan dapat membaca metadata atau data persistence.
 * Jalankan dengan desktop/session aktif dan gunakan generator ini sebagai satu sumber struktur dinamis, bukan
 * menyalin konstruksi widget ke action.</p>
 */
public class DynamicTableMultiTabJsonGenerator {

    /**
     * Entry point utama menggunakan parameter JSONArray (Multi-Model).
     * Index 0 dianggap sebagai Master Table (Utama).
     */
    public static String generateWizardTable(JSONArray wizardConfigs) throws Exception {
        if (wizardConfigs == null || wizardConfigs.length() == 0) {
            return "<div class='alert alert-danger'>Konfigurasi Wizard Kosong</div>";
        }

        // --- 1. Ambil Config Master (Index 0) untuk Tampilan Tabel Utama ---
        JSONObject masterConfig = wizardConfigs.getJSONObject(0);
        
        String randomID = masterConfig.optString("randomID", "rnd" + System.currentTimeMillis());
        String dataTypeName = masterConfig.optString("dataTypeName", "Data");
        
        // --- 2. Generate Logic ---
        return generateWizardTableLogic(wizardConfigs, randomID, dataTypeName);
    }

    /**
     * Helper: Convert JSONArray to String[]
     */
    private static String[] jsonArrayToStringArray(JSONArray jsonArray) {
        if (jsonArray == null) return new String[0];
        String[] result = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            result[i] = jsonArray.optString(i, "");
        }
        return result;
    } 

    /**
     * Helper: Convert Nested JSONArray to String[][]
     */
    private static String[][] jsonArrayToStringMatrix(JSONArray jsonArray) {
        if (jsonArray == null) return new String[0][0];
        String[][] result = new String[jsonArray.length()][];
        
        for (int i = 0; i < jsonArray.length(); i++) {
            Object obj = jsonArray.opt(i);
            
            if (obj instanceof JSONArray) {
                result[i] = jsonArrayToStringArray((JSONArray) obj);
            } else if (obj instanceof String[]) {
                result[i] = (String[]) obj;
            } else if (obj instanceof Object[]) {
                Object[] arr = (Object[]) obj;
                String[] strArr = new String[arr.length];
                for(int k=0; k<arr.length; k++) strArr[k] = String.valueOf(arr[k]);
                result[i] = strArr;
            } else {
                result[i] = new String[0];
            }
        }
        return result;
    }

    /**
     * Helper: Convert JSONArray to Set<String>
     */
    private static Set<String> jsonArrayToSet(JSONArray jsonArray) {
        Set<String> set = new HashSet<String>();
        if (jsonArray == null) return set;
        for (int i = 0; i < jsonArray.length(); i++) {
            set.add(jsonArray.optString(i));
        }
        return set;
    }

    /**
     * Core Logic Generation
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String generateWizardTableLogic(JSONArray wizardConfigs, String randomID, String dataTypeName) throws Exception {
        StringBuilder sb = new StringBuilder();
        
        // Ambil Master Config lagi untuk keperluan tabel depan
        JSONObject masterConfig = wizardConfigs.getJSONObject(0);
        String masterModelClass = masterConfig.getString("modelClass");
        String[] columns = jsonArrayToStringArray(masterConfig.optJSONArray("tableColumns"));
        String[] tableLabels = jsonArrayToStringArray(masterConfig.optJSONArray("tableLabels"));
        String[] searchCols = jsonArrayToStringArray(masterConfig.optJSONArray("searchCols"));
        String sqlWhereMaster = masterConfig.optString("sqlWhere", "");
        
        Class masterClazz = Class.forName(masterModelClass);
        ClassMetadata masterMetadata = HibernateUtil.getClassMetadata(masterClazz);

        // --- 1. CSS & Style Wrapper ---
        sb.append("<div id='appWrapper_").append(randomID).append("'>\n");
        sb.append("<style>\n");
        sb.append(" .cursor-pointer { cursor: pointer; }\n");
        sb.append(" .sort-icon { margin-left: 5px; font-size: 0.8em; }\n");
        sb.append(" .nav-tabs .nav-link.has-error { color: #dc3545; border-color: #dc3545; }\n");
        sb.append(" .master-tab-link { font-weight: bold; font-size: 1.1em; }\n");
        sb.append("</style>\n");

        // --- 2. Header & Toolbar (Master) ---
        sb.append("<div class='container-fluid bg-white shadow-sm rounded p-4 mb-4'>\n");
        sb.append(" <div class='d-flex justify-content-between align-items-center mb-3'>\n");
        sb.append("    <h4 class='mb-0'><span class='fas fa-table text-primary'></span> Data ").append(dataTypeName).append("</h4>\n");
        sb.append("    <div class='btn-group'>\n");
        sb.append("      <button class='btn btn-outline-secondary' onclick='toggleSearch_").append(randomID).append("()' title='Cari Data'><span class='fas fa-search'></span></button>\n");
        sb.append("      <button class='btn btn-outline-secondary' onclick='refreshData_").append(randomID).append("()' title='Refresh'><span class='fas fa-sync'></span></button>\n");
        sb.append("      <button class='btn btn-primary' onclick='openModal_").append(randomID).append("(\"add\")'><span class='far fa-plus-square'></span> Tambah</button>\n");
        sb.append("    </div>\n");
        sb.append(" </div>\n");

        // --- 3. Search Panel (Master Only) ---
        sb.append(" <div id='searchPanel_").append(randomID).append("' class='card card-body bg-light mb-3' style='display:none;'>\n");
        sb.append("    <div class='row g-3'>\n");
        
        for (String col : searchCols) {
            String returnName = null;
            try { returnName = masterMetadata.getPropertyType(col).getReturnedClass().getName(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:135");}
            
            String type = "";
            try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; type=ss.length>1?ss[1]:"";  }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:138");}
            
            String textWithSpace = col.replaceAll("([a-z])([A-Z]+)", "$1 $2");
            String label = textWithSpace.substring(0, 1).toUpperCase() + textWithSpace.substring(1);
            // Cek label custom
            for (int i = 0; i < columns.length; i++) {
                try { if(col.equalsIgnoreCase(columns[i])) { label=tableLabels[i]; break; } }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:144");}
            }

             if(col != null && !col.trim().isEmpty()) {
                 
                 label = Common.getBahasaConfig(label);
                 
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
                    sb.append("             "+s+"\n");
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
        sb.append("    </div>\n");
        sb.append(" </div>\n");

        // --- 4. Table (Master Only) ---
        sb.append(" <div class='table-responsive'>\n");
        sb.append("    <table class='table table-striped table-hover align-middle border'>\n");
        sb.append("      <thead class='table-light'>\n");
        sb.append("        <tr>\n");
        for (int i = 0; i < columns.length; i++) {
            if(i == 0 && DaftarDataService.MAP_ENTITY_TO_FOTO.keySet().contains(masterModelClass)) {
                sb.append("          <th>");
                sb.append("             "+Common.getBahasaConfig("Foto")+"\n");
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

        // ===========================================================================================
        // --- 7. MODAL FORM (MULTI-TABLE / MULTI-WIZARD CONFIG) ---
        // ===========================================================================================
        sb.append("<div class='modal fade' id='formModal_").append(randomID).append("' tabindex='-1' aria-hidden='true' data-bs-backdrop=\"static\">\n");
        sb.append(" <div class='modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable animate__animated animate__rotateInDownLeft'>\n"); 
        sb.append("    <div class='modal-content shadow-lg'>\n");
        sb.append("      <div class='modal-header' style=\"background-color: rgb(180 212 255) !important;\">\n");
        sb.append("        <h5 class='modal-title fw-bold' id='modalTitle_").append(randomID).append("'>Form Data</h5>\n");
        sb.append("        <button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button>\n");
        sb.append("      </div>\n");
        
        sb.append("      <div class='modal-body'>\n");
        sb.append("        <form id='dataForm_").append(randomID).append("'>\n");
        
        // --- LEVEL 1: MASTER TABS (Per Table / Per Config) ---
        sb.append("          <ul class='nav nav-pills mb-3' id='masterTabList_").append(randomID).append("' role='tablist'>\n");
        for (int c = 0; c < wizardConfigs.length(); c++) {
            JSONObject config = wizardConfigs.getJSONObject(c);
            String tabTitle = config.optString("tabTitle", config.getString("dataTypeName"));
            if(tabTitle.isEmpty()) tabTitle = "Data " + (c+1);
            
            String activeClass = (c == 0) ? "active" : "";
            sb.append("            <li class='nav-item' role='presentation'>\n");
            sb.append("              <button class='nav-link master-tab-link ").append(activeClass).append("' id='masterTabBtn_").append(randomID).append("_").append(c).append("' data-bs-toggle='pill' data-bs-target='#masterTabContent_").append(randomID).append("_").append(c).append("' type='button' role='tab'>\n");
            sb.append("                  ").append(tabTitle).append("\n");
            sb.append("              </button>\n");
            sb.append("            </li>\n");
        }
        sb.append("          </ul>\n");
        
        sb.append("          <div class='tab-content' id='masterTabContentWrapper_").append(randomID).append("'>\n");

        // --- LOOP CONFIGS (Untuk generate body tiap Master Tab) ---
        for (int c = 0; c < wizardConfigs.length(); c++) {
            JSONObject config = wizardConfigs.getJSONObject(c);
            String modelClass = config.getString("modelClass");
            Class clazz = Class.forName(modelClass);
            ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
            
            String[] stepTitles = jsonArrayToStringArray(config.optJSONArray("stepTitles"));
            String[][] formCols = jsonArrayToStringMatrix(config.optJSONArray("formCols"));
            String[][] formLabels = jsonArrayToStringMatrix(config.optJSONArray("formLabels"));
            Set<String> paramRequired = jsonArrayToSet(config.optJSONArray("paramRequired"));
            
            // Hidden ID Field per table 
            String idInputId = (c==0) ? "inputId_" + randomID : "input_C" + c + "_id_" + randomID;
            sb.append("          <input type='hidden' id='").append(idInputId).append("'>\n");
            
            // Hidden Values
            if (config.has("hiddenValues")) {
                JSONObject jsonHidden = config.getJSONObject("hiddenValues");
                Iterator<String> keys = jsonHidden.keys();
                while (keys.hasNext()) {
                    String key = keys.next(); 
                    String val = jsonHidden.getString(key);
                    String inputHiddenId = "input_C"+c+"_"+key+"_"+randomID;
                    sb.append("        <input type='hidden' value=\""+val+"\" id='"+inputHiddenId+"'>\n");
                }
            }

            String activeClass = (c == 0) ? "show active" : "";
            sb.append("          <div class='tab-pane fade ").append(activeClass).append("' id='masterTabContent_").append(randomID).append("_").append(c).append("' role='tabpanel'>\n");
            
            // --- LEVEL 2: SUB TABS (Step Titles dalam satu config) ---
            sb.append("            <div class='card border-0'>\n"); 
            sb.append("              <div class='card-header bg-transparent border-bottom-0 p-0'>\n");
            sb.append("                <ul class='nav nav-tabs card-header-tabs' id='subTabList_").append(randomID).append("_C").append(c).append("' role='tablist'>\n");
            for (int i = 0; i < formCols.length; i++) {
                String subTitle = (i < stepTitles.length) ? stepTitles[i] : "Bagian " + (i+1);
                String subActive = (i == 0) ? "active" : "";
                sb.append("                  <li class='nav-item'>\n");
                sb.append("                    <button class='nav-link ").append(subActive).append("' data-bs-toggle='tab' data-bs-target='#subTabContent_").append(randomID).append("_C").append(c).append("_S").append(i).append("' type='button'>").append(subTitle).append("</button>\n");
                sb.append("                  </li>\n");
            }
            sb.append("                </ul>\n");
            sb.append("              </div>\n"); 

            sb.append("              <div class='card-body border border-top-0 rounded-bottom p-3'>\n");
            sb.append("                <div class='tab-content'>\n");
            
            // --- LOOP SUB TABS FIELDS ---
            for (int i = 0; i < formCols.length; i++) {
                String[] currentStepCols = formCols[i];
                String[] currentStepLabels = formLabels[i];
                String subActive = (i == 0) ? "show active" : "";
                
                sb.append("                  <div class='tab-pane fade ").append(subActive).append("' id='subTabContent_").append(randomID).append("_C").append(c).append("_S").append(i).append("' role='tabpanel'>\n");
                sb.append("                    <div class='row g-3'>\n");
                
                // Fields
                for (int j = 0; j < currentStepCols.length; j++) {
                    String col = currentStepCols[j];
                    String colAsli = col;
                    String type = "";
                    String editor = "";
                    try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; type=ss.length>1?ss[1]:""; editor=ss.length>2?ss[2]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:308");}
                    
                    String textWithSpace = col.replaceAll("([a-z])([A-Z]+)", "$1 $2");
                    String label = textWithSpace.substring(0, 1).toUpperCase() + textWithSpace.substring(1);
                    try {
                         label = currentStepLabels[j];
                    }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:314");}

                    if(col != null && !col.trim().isEmpty()) {
                        
                        label = Common.getBahasaConfig(label);
                        String uniqueInputID = "input_C" + c + "_" + col + "_" + randomID;
                        
                        // --- 1. HANDLE UPLOAD FILE (DIPERBAIKI) ---
                        if (col.startsWith("file_") && Common.isValidJsonObject(type)) {
                            sb.append("                       <div id='kolom_div_"+col+"_"+randomID+"' class='col-12'>\n");
                            sb.append("                         <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                            sb.append("                         <div class='input-group'>\n");
                            sb.append("                           <input type='file' class='form-control' id='").append(uniqueInputID).append("' onchange='handleFileSelect_").append(randomID).append("(this, \"").append(col).append("\")' "+(paramRequired.contains(col) ? "required" : "")+">\n");
                            sb.append("                           <button class='btn btn-outline-secondary' type='button' id='btnPreview_").append(uniqueInputID).append("' style='display:none;' onclick='previewFile_").append(randomID).append("(\"").append(col).append("\")'><i class='fas fa-eye'></i></button>\n");
                            sb.append("                         </div>\n");
                            sb.append("                         <input type='hidden' id='val_").append(uniqueInputID).append("' name='").append(col).append("'>\n");
                        } else {
                            String returnName = null;
                            try {
                                returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
                            }catch (Exception e) {
                                continue;
                            }
                            // Layout editor
                            if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan")|| col.equalsIgnoreCase("catatan")|| col.equalsIgnoreCase("alamat")) {
                                sb.append("                       <div id='kolom_div_"+col+"_"+randomID+"' class='col-12'>\n");
                            } else {
                                sb.append("                       <div id='kolom_div_"+col+"_"+randomID+"' class='col-md-6'>\n");
                            }
                            // --- 2. HANDLE FIELDS LAINNYA ---
                            
                            JSONArray colConf;
                            if ((colConf = Common.validJsonArray(type))!=null) {
                                sb.append("        <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                                sb.append("        <select class='form-select form-select-sm select2-search' id='").append(uniqueInputID).append("'>\n");
                                for (int k = 0; k < colConf.length(); k++) {
                                    JSONObject opt = colConf.optJSONObject(k);
                                    if (opt != null) {
                                        sb.append("          <option value='").append(opt.opt("id")==null?"":opt.opt("id")).append("'>").append(opt.opt("nama")==null?"":opt.opt("nama")).append("</option>\n");
                                    }
                                }
                                sb.append("        </select>\n");
                            } else  if(returnName != null && returnName.startsWith("ais.database.model")) {
                                sb.append("                         <label class='form-label fw-semibold' for=\"input_nama_"+uniqueInputID+"\">").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                                String modifiedColName = "C" + c + "_" + col; 
                                
                                String whereTambahan = "";
                                try {
                                    String[] cols = colAsli.split(";", 2);
                                    if(cols.length>1 && Common.isValidJsonObject(cols[1])) {
                                        JSONObject jsonObject = new JSONObject(cols[1]);
                                        if(!jsonObject.isNull("where")) {
                                            whereTambahan = " AND "+ jsonObject.get("where");
                                        }
                                    }
                                }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:369");}
                                
                                String s = JSPCurdGenerator.pilihan(randomID, modifiedColName, label, returnName, paramRequired.contains(col), "id", "nama", whereTambahan);  
                                sb.append("                         "+s+"\n");
                            }
                            else if (type.equalsIgnoreCase("time")){
                                sb.append("                         <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                                sb.append("                         <input type='time' class='form-control' id='").append(uniqueInputID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
                            }
                            else if (type.equalsIgnoreCase("date") || (returnName != null && returnName.equals(Date.class.getName()))) {
                                sb.append("                         <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                                sb.append("                         <input type='date' class='form-control' id='").append(uniqueInputID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
                            }
                            else if (returnName != null && returnName.equals(Boolean.class.getName())) {
                                sb.append("                         <div class='form-check form-switch mt-4'>\n");
                                sb.append("                           <input class='form-check-input' type='checkbox' role='switch' id='").append(uniqueInputID).append("'>\n");
                                sb.append("                           <label class='form-check-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                                sb.append("                         </div>\n");
                            } else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  && editor.equalsIgnoreCase("editor")) {
                                JSPCurdGenerator.textEditor(sb, col, label, uniqueInputID, paramRequired.contains(col), randomID,"");
                            } else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan") || col.equalsIgnoreCase("alamat")) {
                                sb.append("                         <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                                sb.append("                         <textarea class='form-control' id='").append(uniqueInputID).append("' "+(paramRequired.contains(col) ? "required" : "")+" rows='3'></textarea>\n");
                            } else {
                                sb.append("                         <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " <span class='text-danger'>*</span>" : "")).append("</label>\n");
                                sb.append("                         <input type='text' class='form-control' id='").append(uniqueInputID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
                            }
                        }
                        sb.append("                       </div>\n");
                    }
                } 
                sb.append("                    </div>\n");
                sb.append("                  </div>\n"); 
            } 
            
            sb.append("                </div>\n"); 
            sb.append("              </div>\n"); 
            sb.append("            </div>\n"); 
            sb.append("          </div>\n"); 
        } 
        
        sb.append("          </div>\n"); 
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

        // ===========================================================================================
        // --- 8. JAVASCRIPT LOGIC ---
        // ===========================================================================================
        sb.append("<script>\n");
        sb.append(" var page_").append(randomID).append(" = 1;\n");
        sb.append(" var limit_").append(randomID).append(" = 10;\n");
        
        // SETTING DEFAULT SORT DAN ORDER
        String sortDef1 = masterConfig.optString("sort_default_1", "tanggal_dirubah");
        if (sortDef1 == null || sortDef1.isEmpty()) sortDef1 = "tanggal_dirubah";
        String orderDef1 = masterConfig.optString("order_default_1", "desc");
        if (orderDef1 == null || orderDef1.isEmpty()) orderDef1 = "desc";
        
        sb.append(" var sortCol_").append(randomID).append(" = '").append(sortDef1).append("';\n");
        sb.append(" var sortOrder_").append(randomID).append(" = '").append(orderDef1).append("';\n");
        
        sb.append(" var currentMode_").append(randomID).append(" = 'add';\n");
        sb.append(" var modalInstance_").append(randomID).append(";\n");
        
        sb.append(" const isEmpty"+randomID+" = (val) => {\r\n"
                + "  return (val === undefined || val === null || (typeof val === \"string\" && val.trim().length === 0));\r\n"
                + "};\n");

        // --- PREPARE CONFIG ARRAYS FOR JS ---
        sb.append(" var wizardConfigs_").append(randomID).append(" = [\n");
        for(int c=0; c<wizardConfigs.length(); c++) {
            JSONObject conf = wizardConfigs.getJSONObject(c);
            String mClass = conf.getString("modelClass");
            String relKey = conf.optString("relasi_ke_tabel_utama", "");
            
            List<String> flatCols = new ArrayList<String>();
            String[][] fCols = jsonArrayToStringMatrix(conf.optJSONArray("formCols"));
            for(String[] g : fCols) for(String s : g) flatCols.add(s);
            
            sb.append("    {\n");
            sb.append("      index: ").append(c).append(",\n");
            sb.append("      modelClass: '").append(mClass).append("',\n");
            sb.append("      relCol: '").append(relKey).append("',\n"); 
            sb.append("      fields: [");
            for(int k=0; k<flatCols.size(); k++) {
                 String s = flatCols.get(k);
                 try { String[] ss = s.split(";"); s = ss.length>0?ss[0]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:464");}
                 sb.append("'").append(s).append("'").append(k < flatCols.size()-1 ? "," : "");
            }
            sb.append("],\n");
            
            sb.append("      meta: {\n");
            Class clazz = Class.forName(mClass);
            ClassMetadata meta = HibernateUtil.getClassMetadata(clazz);
            for(String s : flatCols) {
                try { String[] ss = s.split(";"); s = ss.length>0?ss[0]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:473");}
                if(s.isEmpty()) continue;
                String rName = "string";
                try { rName = meta.getPropertyType(s).getReturnedClass().getName(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:476");}
                sb.append("        '").append(s).append("': '").append(rName).append("',");
            }
            sb.append("      },\n");

            sb.append("      columnConfigs: {\n");
            for(String s : flatCols) {
                String[] parts = s.split(";", 2);
                if(parts.length > 1) {
                    String colName = parts[0];
                    String rawType = parts[1];
                    if(colName.startsWith("file_") && rawType.trim().startsWith("{")) {
                        sb.append("        '").append(colName).append("': ").append(rawType).append(",\n");
                    } else {
                        sb.append("        '").append(colName).append("': '").append(rawType).append("',\n");
                    }
                }
            }
            sb.append("      }\n");
            sb.append("    },\n");
        }
        sb.append(" ];\n");
        
        paging( randomID,  masterModelClass,  sb);
        // Teruskan masterConfig untuk baca sort_default selanjutnya
        load( randomID,  masterModelClass, masterMetadata, searchCols, columns, sqlWhereMaster,  sb, masterConfig);
        render( randomID,  masterModelClass, masterMetadata, columns,  sb);
        save( randomID,  masterModelClass,  sb);
        modal( randomID,  masterModelClass,  sb);

        sb.append(" function toggleSearch_").append(randomID).append("() { var p = document.getElementById('searchPanel_").append(randomID).append("'); p.style.display = (p.style.display === 'none') ? 'block' : 'none'; }\n");
        sb.append(" function triggerSearch_").append(randomID).append("() { page_").append(randomID).append(" = 1; loadData_").append(randomID).append("(); }\n");
        sb.append(" function refreshData_").append(randomID).append("() { page_").append(randomID).append(" = 1; loadData_").append(randomID).append("(); }\n");
        sb.append(" function changeSort_").append(randomID).append("(col) { \n");
        sb.append("     if(sortCol_").append(randomID).append(" === col) sortOrder_").append(randomID).append(" = (sortOrder_").append(randomID).append(" === 'asc') ? 'desc' : 'asc';\n");
        sb.append("     else { sortCol_").append(randomID).append(" = col; sortOrder_").append(randomID).append(" = 'asc'; }\n");
        sb.append("     loadData_").append(randomID).append("();\n");
        sb.append(" }\n");

        sb.append(" function handleFileSelect_").append(randomID).append("(input, colName) { \n");
        sb.append("     // Implementasi upload file async dan set value ke hidden input val_input_C... \n");
        sb.append(" }\n");

        sb.append(" document.addEventListener('DOMContentLoaded', () => { loadData_").append(randomID).append("(); });\n");
        sb.append("</script>\n");
        
        sb.append("</div>\n"); 
        return sb.toString();
    }
    
    private static void paging(String randomID, String masterModelClass, StringBuilder sb) throws Exception { 
         // --- RENDER PAGINATION ---
        sb.append(" function renderPagination_").append(randomID).append("(totalItems) {\n");
        sb.append("    const paginationContainer = document.getElementById('pagination_").append(randomID).append("');\n");
        sb.append("    paginationContainer.innerHTML = '';\n");
        
        sb.append("    const totalPages = Math.ceil(totalItems / limit_").append(randomID).append(");\n");
        sb.append("    if (totalPages <= 1) return;\n");

        sb.append("    const createPageItem = (text, pageNum, isActive = false, isDisabled = false) => {\n");
        sb.append("       const li = document.createElement('li');\n");
        sb.append("       li.className = `page-item ${isActive ? 'active' : ''} ${isDisabled ? 'disabled' : ''}`;\n");
        sb.append("       const a = document.createElement('a');\n");
        sb.append("       a.className = 'page-link cursor-pointer';\n");
        sb.append("       a.innerHTML = text;\n");
        sb.append("       if (!isDisabled && !isActive) {\n");
        sb.append("           a.onclick = function() { page_").append(randomID).append(" = pageNum; loadData_").append(randomID).append("(); };\n");
        sb.append("       }\n");
        sb.append("       li.appendChild(a);\n");
        sb.append("       return li;\n");
        sb.append("    };\n");

        sb.append("    paginationContainer.appendChild(createPageItem('&laquo;', page_").append(randomID).append(" - 1, false, page_").append(randomID).append(" <= 1));\n");

        sb.append("    const maxVisibleButtons = 5;\n");
        sb.append("    let startPage = Math.max(1, page_").append(randomID).append(" - Math.floor(maxVisibleButtons / 2));\n");
        sb.append("    let endPage = Math.min(totalPages, startPage + maxVisibleButtons - 1);\n");
        sb.append("    if (endPage - startPage + 1 < maxVisibleButtons) {\n");
        sb.append("       startPage = Math.max(1, endPage - maxVisibleButtons + 1);\n");
        sb.append("    }\n");

        sb.append("    if (startPage > 1) {\n");
        sb.append("       paginationContainer.appendChild(createPageItem('1', 1, false));\n");
        sb.append("       if (startPage > 2) {\n");
        sb.append("           const liDots = document.createElement('li'); liDots.className = 'page-item disabled'; liDots.innerHTML = '<span class=\"page-link\">...</span>'; paginationContainer.appendChild(liDots);\n");
        sb.append("       }\n");
        sb.append("    }\n");

        sb.append("    for (let i = startPage; i <= endPage; i++) {\n");
        sb.append("       paginationContainer.appendChild(createPageItem(i, i, i === page_").append(randomID).append("));\n");
        sb.append("    }\n");

        sb.append("    if (endPage < totalPages) {\n");
        sb.append("       if (endPage < totalPages - 1) {\n");
        sb.append("           const liDots = document.createElement('li'); liDots.className = 'page-item disabled'; liDots.innerHTML = '<span class=\"page-link\">...</span>'; paginationContainer.appendChild(liDots);\n");
        sb.append("       }\n");
        sb.append("       paginationContainer.appendChild(createPageItem(totalPages, totalPages, false));\n");
        sb.append("    }\n");

        sb.append("    paginationContainer.appendChild(createPageItem('&raquo;', page_").append(randomID).append(" + 1, false, page_").append(randomID).append(" >= totalPages));\n");
        sb.append(" }\n");
    }
    
    // Perhatikan tambahan parameter masterConfig
    private static void load(String randomID, String masterModelClass, ClassMetadata masterMetadata, String[] searchCols,  String[] columns, String sqlWhereMaster, StringBuilder sb, JSONObject masterConfig) throws Exception { 
        sb.append(" async function loadData_").append(randomID).append("() {\n");
        sb.append("    const start = (page_").append(randomID).append(" - 1);\n");
        sb.append("    let whereClause = '';\n");
        sb.append("    let criteria = [];\n");
        for (String col : searchCols) {
             try { String[] ss = col.split(";"); col = ss.length>0 ? ss[0] : ""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:586");}
             String returnName = null;
             try { returnName = masterMetadata.getPropertyType(col).getReturnedClass().getName(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:588");}
            
             if(col != null && !col.trim().isEmpty()) {
                 String elId = "search_" + col + "_" + randomID;
                 
                 if(returnName != null && returnName.startsWith("ais.database.model")) {
                      sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
                      sb.append("   if(val").append(col).append(") criteria.push(\"").append(col).append(" = \" + val").append(col).append(" + \" \");\n");
                 } else if (returnName != null && returnName.equals(Boolean.class.getName())) {
                      sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
                      sb.append("   if(val").append(col).append(" !== '') criteria.push(\"").append(col).append(" = \" + val").append(col).append(");\n");
                 } else {
                      sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
                      sb.append("   if(val").append(col).append(") criteria.push(\"").append(col).append(" ilike '%\" + val").append(col).append(" + \"%' \");\n");
                 }
             }
        }
        sb.append("    if(criteria.length > 0) whereClause = criteria.join(' AND ');\n");
        
        StringBuilder projection = masterModelClass.equalsIgnoreCase(Tbmuser.class.getName()) ? new StringBuilder("userId") : masterModelClass.equalsIgnoreCase(Tbmrole.class.getName()) ? new StringBuilder("roleId") :  new StringBuilder("id");
        List<String> masterCols = new ArrayList<String>();
        for(String col : columns) {
            if(!masterCols.contains(col)) {
             try { String[] ss = col.split(";"); col = ss.length>0 ? ss[0] : ""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:611");}
            masterCols.add(col);
        }
        }
        for(String col : masterCols) if(!col.equals("id") && !col.equals("userId") && !col.equals("roleId")) {
            projection.append(";").append(col);
        }
        
        sb.append("    var reqObj = {\n");
        sb.append("      \"action\": \"daftar\", \"class\": \"").append(masterModelClass).append("\",\n");
        sb.append("      \"projection\": \"").append(projection.toString()).append("\",\n");
        sb.append("      \"deep\": \"1\", \"max\": limit_").append(randomID).append(", \"halaman\": start,\n");
        sb.append("      \"order1\": sortOrder_").append(randomID).append(", \"sort1\": sortCol_").append(randomID).append(", \"count\": \"true\"\n");
        
        // --- INJECT SORT & ORDER 2 - 5 ---
        for (int i = 2; i <= 5; i++) {
            String sortVal = masterConfig.optString("sort_default_" + i, "");
            String orderVal = masterConfig.optString("order_default_" + i, "");
            if (sortVal != null && !sortVal.trim().isEmpty()) {
                sb.append("      , \"sort").append(i).append("\": \"").append(sortVal).append("\"\n");
                if (orderVal != null && !orderVal.trim().isEmpty()) {
                    sb.append("      , \"order").append(i).append("\": \"").append(orderVal).append("\"\n");
                }
            }
        }
        sb.append("    };\n");
        
        sb.append("    if(whereClause) reqObj[\"where1\"] = whereClause;\n");
        if(!sqlWhereMaster.isEmpty()) sb.append("    reqObj[\"where2\"] = \""+sqlWhereMaster+"\";\n");
        
        // --- POST REQUEST ---
        sb.append("    const servletUrl = '"+Common.ROOT+"/Data';\n");
        sb.append("    try {\n");
        sb.append("      const response = await fetch(servletUrl, {\n");
        sb.append("          method: 'POST',\n");
        sb.append("          headers: {\n");
        sb.append("              'Content-Type': 'application/json'\n");
        sb.append("          },\n");
        sb.append("          body: JSON.stringify(reqObj)\n");
        sb.append("      });\n");
        
        sb.append("      const dataResponse = await response.json();\n");
        sb.append("      renderTable_").append(randomID).append("(dataResponse.data || []);\n");
        sb.append("      renderPagination_").append(randomID).append("(dataResponse.count || 0);\n");
        sb.append("      document.getElementById('pageInfo_").append(randomID).append("').innerText = 'Total: ' + (dataResponse.count || 0);\n");
        sb.append("    } catch (error) { console.error(error); }\n");
        sb.append(" }\n");
    }
    
    private static void render(String randomID, String masterModelClass, ClassMetadata masterMetadata, String[] columns, StringBuilder sb) throws Exception { 
        sb.append(" function renderTable_").append(randomID).append("(data) {\n");
        sb.append("    const tbody = document.getElementById('tableBody_").append(randomID).append("');\n");
        sb.append("    tbody.innerHTML = '';\n");
        sb.append("    if (data.length === 0) { tbody.innerHTML = '<tr><td colspan=\"20\" class=\"text-center\">"+Common.getBahasaConfig("Tidak ada data")+"</td></tr>'; return; }\n");
        sb.append("    data.forEach(item => {\n");
        sb.append("      let tr = document.createElement('tr');\n");
        int index = 0;
        for (String col : columns) {
            if(index == 0 && DaftarDataService.MAP_ENTITY_TO_FOTO.keySet().contains(masterModelClass)) {
                 sb.append("      let td_foto_").append(col).append(" = document.createElement('td');\n");
                 sb.append("      td_foto_").append(col).append(".innerHTML = `").append("<img onclick=\"tampilGambar(this)\" \r\n"
                        + "                                  src=\"`+item.foto_kecil+`\" \r\n"
                        + "                                  class=\"rounded-4 shadow-sm border border-2 border-white\" \r\n"
                        + "                                  alt=\"`+item.nama+`\" \r\n"
                        + "                                  style=\"width: 85px; height: 85px; object-fit: cover; cursor: pointer;\"\r\n"
                        + "                                  title=\"Klik untuk zoom\">").append("`;\n");
                 sb.append("      tr.appendChild(td_foto_").append(col).append(");\n");
            }
            index ++;
             String returnName = null;
             try { returnName = masterMetadata.getPropertyType(col).getReturnedClass().getName(); }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:681");}
             try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableMultiTabJsonGenerator.java:682");}
             
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
        sb.append("      tdAction.innerHTML = `\n");
        sb.append("        <div class=\"dropdown font-sans-serif position-static\">\r\n");
        sb.append("          <button class=\"btn btn-link text-600 btn-sm dropdown-toggle btn-reveal\" type=\"button\" title=\"" + Common.getBahasaConfig("Aksi") + "\" aria-label=\"" + Common.getBahasaConfig("Aksi") + "\" data-bs-toggle=\"dropdown\"><span class=\"fas fa-ellipsis-h fs-10\"></span></button>\r\n");
        sb.append("          <div class=\"dropdown-menu dropdown-menu-end border py-0\">\r\n");
        sb.append("            <div class=\"py-2\">\n");
        sb.append("              <button class='dropdown-item' onclick='openModal_").append(randomID).append("(\"edit\", \""+(masterModelClass.equals(Tbmuser.class.getName()) ? "${item.userId}" : masterModelClass.equals(Tbmrole.class.getName()) ? "${item.roleId}" : masterModelClass.equals(Program.class.getName()) ? "${item.nama}" :  "${item.id}" )+"\")'><span class='fas fa-edit'></span> Ubah</button>\n");
        sb.append("              <button class='dropdown-item text-danger' data-id=\"${item.id}\" onclick=\"prosesDeleteData('"+masterModelClass+"', '${item.id}', function(){ loadData_"+randomID+"(); });\"><span class='fas fa-trash'></span> Hapus</button>\n");
        sb.append("            </div>\r\n");
        sb.append("          </div>\r\n");
        sb.append("        </div>`;\n");
        sb.append("      tr.appendChild(tdAction);\n");
        sb.append("      tbody.appendChild(tr);\n");
        sb.append("    });\n");
        sb.append(" }\n");
    }
    
    private static void save(String randomID, String masterModelClass, StringBuilder sb) throws Exception { 
        sb.append(" async function saveData_").append(randomID).append("() {\n");
        
        sb.append("    let masterId = document.getElementById('inputId_").append(randomID).append("').value;\n");
        sb.append("    if(!masterId) masterId = null;\n");
        
        sb.append("    try {\n");
        sb.append("      for(let i=0; i<wizardConfigs_").append(randomID).append(".length; i++) {\n");
        sb.append("         let conf = wizardConfigs_").append(randomID).append("[i];\n");
        
        sb.append("         let dataObj = {};\n");
        sb.append("         let specificId = (i===0) ? masterId : document.getElementById('input_C'+i+'_id_").append(randomID).append("').value;\n");
        sb.append("         if(specificId) dataObj.id = parseInt(specificId);\n");
        
        sb.append("         if(i > 0) {\n");
        sb.append("             if(!masterId) throw 'Gagal menyimpan Master ID';\n");
        sb.append("             if(conf.relCol) {\n");
        sb.append("                dataObj[conf.relCol] = parseInt(masterId); \n"); 
        sb.append("                dataObj['relasi_ke_tabel_utama'] = { id: parseInt(masterId), relasi: conf.relCol }; \n"); 
        sb.append("             }\n");
        sb.append("         }\n");
        
        sb.append("         conf.fields.forEach(f => {\n");
        sb.append("             let el = document.getElementById('input_C'+i+'_'+f+'_").append(randomID).append("');\n");
        sb.append("             if(el) {\n");
        sb.append("                if(el.type==='checkbox') dataObj[f] = el.checked;\n");
        sb.append("                else if (conf.meta[f] && conf.meta[f].startsWith('ais.database.model')) {\n"); 
        sb.append("                    let idVal = document.getElementById('input_C'+i+'_'+f+'_").append(randomID).append("').value;\n");
        sb.append("                    if(idVal) dataObj[f] = parseInt(idVal);\n");
        sb.append("                }\n");
        sb.append("                else dataObj[f] = el.value;\n");
        sb.append("             } else if (f.startsWith('file_')) { \n");
        sb.append("                let elFileVal = document.getElementById('val_input_C'+i+'_'+f+'_").append(randomID).append("');\n");
        sb.append("                if(elFileVal && elFileVal.value) dataObj[f] = elFileVal.value;\n");
        sb.append("             }\n");
        sb.append("         });\n");
        
        sb.append("         let reqObj = {\n");
        sb.append("           \"action\": \"simpanDataRinci\", \"id\": (dataObj.id ? dataObj.id : -1),\n");
        sb.append("           \"class\": conf.modelClass, \"data\": dataObj\n");
        sb.append("         };\n");
        
        // --- POST REQUEST ---
        sb.append("         let resp = await fetch('"+Common.ROOT+"/Data', {\n");
        sb.append("             method: 'POST',\n");
        sb.append("             headers: { 'Content-Type': 'application/json' },\n");
        sb.append("             body: JSON.stringify(reqObj)\n");
        sb.append("         });\n");
        
        sb.append("         let json = await resp.json();\n");
        sb.append("         if(json.status !== '00') throw 'Error pada Tab '+ (i+1) + ': ' + json.description;\n");
        
        sb.append("         if(i===0) {\n");
        sb.append("             updateBulkFiles(json.id);\n");
        sb.append("             if(!masterId && json.id) masterId = json.id;\n");
        sb.append("         }\n");
        sb.append("      }\n"); 
        
        sb.append("      tampilkanToast('Semua data berhasil disimpan', 'bg-success');\n");
        sb.append("      modalInstance_").append(randomID).append(".hide();\n");
        sb.append("      loadData_").append(randomID).append("();\n");
        
        sb.append("    } catch(e) { tampilkanToast(e, 'bg-danger'); }\n");
        sb.append(" }\n");
    }
    
    private static void modal(String randomID, String masterModelClass, StringBuilder sb) throws Exception { 
        sb.append(" async function openModal_").append(randomID).append("(mode, idMaster) {\n");
        sb.append("    currentMode_").append(randomID).append(" = mode;\n");
        
        sb.append("    let formEl = document.getElementById('dataForm_").append(randomID).append("');\n");
        sb.append("    if(formEl) formEl.reset();\n");
        
        sb.append("    document.getElementById('modalTitle_").append(randomID).append("').innerText = (mode === 'add' ? 'Tambah' : 'Edit') + ' Data';\n");
        
        sb.append("    document.querySelectorAll('#dataForm_").append(randomID).append(" input[type=hidden]').forEach(el => el.value = '');\n");
        sb.append("    document.querySelectorAll('#dataForm_").append(randomID).append(" input, ")
          .append("#dataForm_").append(randomID).append(" select, ")
          .append("#dataForm_").append(randomID).append(" textarea').forEach(function(el) {\n");

        sb.append("        if (['checkbox', 'radio'].includes(el.type)) {\n");
        sb.append("            el.checked = false;\n");
        sb.append("        } else if (el.tagName === 'TEXTAREA') {\n");
        sb.append("            el.value = '';\n");
        sb.append("            el.innerHTML = '';\n"); 
        sb.append("        } else {\n");
        sb.append("            el.value = '';\n");
        sb.append("        }\n");
        sb.append("    });\n");
        
        sb.append("    document.querySelectorAll('[id^=btnPreview_]').forEach(el => el.style.display = 'none');\n");
        sb.append("    document.querySelectorAll('[id^=input_nama_]').forEach(el => el.value = '');\n");

        sb.append("    if(mode === 'edit' && idMaster) {\n");
        sb.append("       document.getElementById('inputId_").append(randomID).append("').value = idMaster;\n");
        
        sb.append("       for(let i=0; i<wizardConfigs_").append(randomID).append(".length; i++) {\n");
        sb.append("          let conf = wizardConfigs_").append(randomID).append("[i];\n");
        sb.append("          let dataItem = null;\n");
        
        sb.append("          let projectionData = conf.fields.join(\";\");\n");
       
        if(masterModelClass.equalsIgnoreCase(Tbmuser.class.getName())) {
             sb.append("          projectionData = 'userId;'+projectionData;\n");
        } else if(masterModelClass.equalsIgnoreCase(Tbmrole.class.getName())) {
             sb.append("          projectionData = 'roleId;'+projectionData;\n");
        } else if(masterModelClass.equalsIgnoreCase(Program.class.getName())) {
             sb.append("          projectionData = 'nama;'+projectionData;\n");
        } else {
             sb.append("          projectionData = 'id;'+projectionData;\n");
        }
        
        // Fetch Master
        sb.append("          if(i===0) { \n"); 
        if(masterModelClass.equalsIgnoreCase(Tbmuser.class.getName()) || masterModelClass.equalsIgnoreCase(Tbmrole.class.getName()) || masterModelClass.equalsIgnoreCase(Program.class.getName())) {
            sb.append("          idMaster = `'`+idMaster+`'`; \n");
        }
        sb.append("              let req = { action: 'daftar', 'projection': projectionData, class: conf.modelClass, 'where1': "+(masterModelClass.equalsIgnoreCase(Tbmuser.class.getName()) ? "'userId='+idMaster" : masterModelClass.equalsIgnoreCase(Tbmrole.class.getName()) ? "'roleId='+idMaster" :  masterModelClass.equalsIgnoreCase(Program.class.getName()) ? "'nama='+idMaster" :  "'id='+idMaster")+", max: 1  };\n");
        // --- POST REQUEST ---
        sb.append("              let resp = await fetch('"+Common.ROOT+"/Data', {\n");
        sb.append("                  method: 'POST',\n");
        sb.append("                  headers: { 'Content-Type': 'application/json' },\n");
        sb.append("                  body: JSON.stringify(req)\n");
        sb.append("              });\n");
        sb.append("              let json = await resp.json();\n");
        sb.append("              if(json.status=='00') dataItem = json.data[0];\n");
        sb.append("          } else {\n");
        
        // Fetch Child
        sb.append("              if(conf.relCol) {\n");
        sb.append("                 let sort1Data = 'id'; \n");
        sb.append("                 if(conf.modelClass === '"+Tbmuser.class.getName()+"') { sort1Data = 'userId';} \n");
        sb.append("                 if(conf.modelClass === '"+Tbmrole.class.getName()+"') { sort1Data = 'roleId';} \n");
        sb.append("                 if(conf.modelClass === '"+Program.class.getName()+"') { sort1Data = 'nama';} \n");
        sb.append("                 let req = { action: 'daftar', 'projection': projectionData, class: conf.modelClass, where1: conf.relCol + ' = ' + idMaster, max: 1, order1:'desc', sort1:sort1Data };\n");
        // --- POST REQUEST ---
        sb.append("                 let resp = await fetch('"+Common.ROOT+"/Data', {\n");
        sb.append("                     method: 'POST',\n");
        sb.append("                     headers: { 'Content-Type': 'application/json' },\n");
        sb.append("                     body: JSON.stringify(req)\n");
        sb.append("                 });\n");
        sb.append("                 let json = await resp.json();\n");
        sb.append("                 if(json.status=='00' && json.data.length > 0) dataItem = json.data[0];\n");
        sb.append("              }\n");
        sb.append("          }\n");
        
        sb.append("          if(dataItem) {\n");
        sb.append("              if(i>0) document.getElementById('input_C'+i+'_id_").append(randomID).append("').value = dataItem.id;\n");
        sb.append("              conf.fields.forEach(f => {\n");
        sb.append("                 console.info('col: ', f); \n");
        sb.append("                 console.info('type: ', conf.columnConfigs[f]); \n");
        sb.append("                 if(f.startsWith(\"file_\")){ \n");
        sb.append("                    let valFile = dataItem[f];\n");
        sb.append("                    if(valFile) {\n");
        sb.append("                        let elVal = document.getElementById('val_input_C'+i+'_'+f+'_").append(randomID).append("');\n");
        sb.append("                        if(elVal) elVal.value = valFile;\n");
        sb.append("                        let btnPreview = document.getElementById('btnPreview_input_C'+i+'_'+f+'_").append(randomID).append("');\n");
        sb.append("                        if(btnPreview) btnPreview.style.display = 'block';\n");
        sb.append("                    }\n");
        sb.append("                    let col = f; \n");
        JSPCurdGenerator.renderUploadFile(sb, randomID);
        sb.append("                 } \n");
        sb.append("                 else if(conf.columnConfigs[f] && conf.columnConfigs[f] === 'editor'){ \n");
        JSPCurdGenerator.renderText(sb, randomID);
        sb.append("                 } else { \n");
        sb.append("                    let el = document.getElementById('input_C'+i+'_'+f+'_").append(randomID).append("');\n");
        sb.append("                    if(el) {\n");
        sb.append("                       if(el.type==='checkbox') el.checked = (dataItem[f] === 'true' || dataItem[f] === true);\n");
        sb.append("                       else if (conf.meta[f] && conf.meta[f].startsWith('ais.database.model')) {\n");
        sb.append("                           let valId = dataItem[f+'_id'] || (dataItem[f] ? dataItem[f].id : '');\n");
        sb.append("                           let valNm = dataItem[f+'_nama'] || (dataItem[f] ? dataItem[f].nama : '');\n");
        sb.append("                           el.value = valId;\n");
        sb.append("                           let elNama = document.getElementById('input_nama_C'+i+'_'+f+'_").append(randomID).append("');\n");
        sb.append("                           if(elNama) elNama.value = valNm;\n");
        sb.append("                       }\n");
        sb.append("                       else if (conf.meta[f] === '"+Date.class.getName()+"' || el.type==='date') {\n");
        sb.append("                           el.value = dataItem[f+'_tgl'] || dataItem[f];\n");
        sb.append("                       }\n");
        sb.append("                       else if (el.tagName === 'TEXTAREA') {\n");
        sb.append("                           el.innerHTML = dataItem[f];\n");
        sb.append("                           el.value = dataItem[f];\n"); 
        sb.append("                       }\n");
        sb.append("                       else el.value = dataItem[f];\n");
        sb.append("                    }\n");
        sb.append("                 }\n");
        sb.append("              });\n");
        sb.append("          }\n");
        sb.append("       }\n"); 
        sb.append("    }\n"); 
        
        sb.append("    var modalEl = document.getElementById('formModal_").append(randomID).append("');\n");
        sb.append("    modalInstance_").append(randomID).append(" = new bootstrap.Modal(modalEl);\n");
        sb.append("    modalInstance_").append(randomID).append(".show();\n");
        sb.append(" }\n");

    }
}