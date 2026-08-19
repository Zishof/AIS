package ais.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.servlet.api.DaftarDataService;
import ais.database.hibernate.HibernateUtil;

public class DynamicTableWizardGenerator {

    /**
     * Menghasilkan HTML dan JavaScript untuk CRUD dengan fitur WIZARD FORM.
     * * @param columns       Array nama field untuk TABEL (1D)
     * @param tableLabels   Array label untuk HEADER TABEL (1D)
     * @param searchCols    Array nama field untuk PENCARIAN
     * @param formCols      Array 2D field untuk FORM WIZARD ({{step1_f1, step1_f2}, {step2_f1}})
     * @param formLabels    Array 2D label untuk FORM WIZARD (sesuai struktur formCols)
     * @param stepTitles    Array Judul untuk setiap Step Wizard (misal: {"Info Dasar", "Detail"})
     */
    public static String generateWizardTable(String[] columns, String[] tableLabels, String[] searchCols, 
                                             String[][] formCols, String[][] formLabels, String[] stepTitles,
                                             String randomID, String dataTypeName, 
                                             String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama) throws Exception {
         String sqlWhere = "";
         Map<String,String> hiddenValues = null;
        return generateWizardTable(columns, tableLabels, searchCols, 
                formCols, formLabels, stepTitles, randomID, dataTypeName, 
                 modelClass, paramRequired, paramTidakBolehSama, sqlWhere, hiddenValues);
    }

    @SuppressWarnings("rawtypes")
    public static String generateWizardTable(String[] columns, String[] tableLabels, String[] searchCols, 
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
        // CSS Wizard
        sb.append(" .wizard-header { display: flex; justify-content: space-between; margin-bottom: 20px; position: relative; }\n");
        sb.append(" .wizard-step-item { text-align: center; position: relative; z-index: 2; cursor: pointer; width: 100%; }\n");
        sb.append(" .wizard-circle { width: 35px; height: 35px; background: #e9ecef; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 5px; font-weight: bold; color: #6c757d; transition: all 0.3s; border: 2px solid #dee2e6; }\n");
        sb.append(" .wizard-step-item.active .wizard-circle { background: #0d6efd; color: #fff; border-color: #0d6efd; box-shadow: 0 0 0 3px rgba(13,110,253,0.25); }\n");
        sb.append(" .wizard-step-item.completed .wizard-circle { background: #198754; color: #fff; border-color: #198754; }\n");
        sb.append(" .wizard-line { position: absolute; top: 17px; left: 0; width: 100%; height: 2px; background: #dee2e6; z-index: 1; }\n");
        sb.append(" .step-content { display: none; animation: fadeIn 0.4s; }\n");
        sb.append(" .step-content.active { display: block; }\n");
        sb.append(" @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }\n");
        sb.append("</style>\n");

        // --- 2. Header & Toolbar (Sama seperti sebelumnya) ---
        sb.append("<div class='container-fluid bg-white shadow-sm rounded p-4 mb-4'>\n");
        sb.append(" <div class='d-flex justify-content-between align-items-center mb-3'>\n");
        sb.append("    <h4 class='mb-0'><span class='fas fa-table text-primary'></span> Data ").append(dataTypeName).append("</h4>\n");
        sb.append("    <div class='btn-group'>\n");
        sb.append("      <button class='btn btn-outline-secondary' onclick='toggleSearch_").append(randomID).append("()' title='Cari Data'><span class='fas fa-search'></span></button>\n");
        sb.append("      <button class='btn btn-outline-secondary' onclick='refreshData_").append(randomID).append("()' title='Refresh'><span class='fas fa-sync'></span></button>\n");
        sb.append("      <button class='btn btn-primary' onclick='openModal_").append(randomID).append("(\"add\")'><span class='far fa-plus-square'></span> Tambah</button>\n");
        sb.append("    </div>\n");
        sb.append(" </div>\n");

        // --- 3. Search Panel (Sama) ---
        sb.append(" <div id='searchPanel_").append(randomID).append("' class='card card-body bg-light mb-3' style='display:none;'>\n");
        sb.append("    <div class='row g-3'>\n");
        
        for (String col : searchCols) {
        	String returnName = null;
			
        	String type = "";
            try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; type=ss.length>1?ss[1]:"";  }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:91");}
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            String textWithSpace = col.replaceAll("([a-z])([A-Z]+)", "$1 $2");
            String result = textWithSpace.substring(0, 1).toUpperCase() + textWithSpace.substring(1);
            String label = result;
            // Cari label dari tableLabels jika ada match
             for (int i = 0; i < columns.length; i++) {
                 try {
                     if(col.equalsIgnoreCase(columns[i])) {
                         label=tableLabels[i];
                         break;
                     }
                 }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:108");}
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
                } else  if(returnName != null && returnName.startsWith("ais.database.model")) {
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

        // --- 4. Table (Sama) ---
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

        // --- 5. Pagination (Sama) ---
        sb.append(" <div class='d-flex justify-content-between align-items-center mt-3'>\n");
        sb.append("    <span class='text-muted small' id='pageInfo_").append(randomID).append("'>"+Common.getBahasaConfig("Menampilkan")+"...</span>\n");
        sb.append("    <nav><ul class='pagination pagination-sm mb-0' id='pagination_").append(randomID).append("'></ul></nav>\n");
        sb.append(" </div>\n");
        sb.append("</div>\n"); 

       

        // --- 7. Modal Form (WIZARD MODIFIED) ---
        sb.append("<div class='modal fade' id='formModal_").append(randomID).append("' tabindex='-1' aria-hidden='true' data-bs-backdrop=\"static\">\n");
        sb.append(" <div class='modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable animate__animated animate__rotateInDownLeft'>\n");
        sb.append("    <div class='modal-content shadow-lg'>\n");
        sb.append("      <div class='modal-header' style=\"background-color: rgb(180 212 255) !important;\">\n");
        sb.append("        <h5 class='modal-title fw-bold' id='modalTitle_").append(randomID).append("'>Form Wizard</h5>\n");
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

        // --- WIZARD HEADER (Circles) ---
        sb.append("          <div class='wizard-header'>\n");
        sb.append("            <div class='wizard-line'></div>\n");
        for (int i = 0; i < formCols.length; i++) {
            String title = (i < stepTitles.length) ? stepTitles[i] : "Step " + (i+1);
            sb.append("            <div class='wizard-step-item' id='wizardIndicator_").append(randomID).append("_").append(i).append("' onclick='jumpToStep_").append(randomID).append("(").append(i).append(")'>\n");
            sb.append("              <div class='wizard-circle'>").append(i + 1).append("</div>\n");
            sb.append("              <small class='fw-bold text-muted'>").append(title).append("</small>\n");
            sb.append("            </div>\n");
        }
        sb.append("          </div>\n");

        // --- WIZARD BODY (Steps) ---
        sb.append("          <div id='wizardContent_").append(randomID).append("'>\n");
        
        // Loop Steps
        for (int i = 0; i < formCols.length; i++) {
            String[] currentStepCols = formCols[i];
            String[] currentStepLabels = formLabels[i];
            
            sb.append("            <div class='step-content' id='stepContent_").append(randomID).append("_").append(i).append("'>\n");
            sb.append("              <div class='row g-3'>\n");
            
            // Loop Fields in this Step
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
                 }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:244");}
                 
                 if(col != null && !col.trim().isEmpty()) {
                     
                	
                 	
                 	if (col.startsWith("file_") && Common.isValidJsonObject(type)) {
                 		
                 		 sb.append("                      <div id='kolom_div_"+col+"_"+randomID+"' class='col-12'>\n");
                 		
                 		// --- File generate nanti
                         
                         
                         
             		} else {
             		// Layout
             			String returnName = null;
    					
   					 
    		            try {
    						returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
    					}catch (Exception e) {
    						continue;
//    						e.printStackTrace();
    					}
                        if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan")|| col.equalsIgnoreCase("catatan")|| col.equalsIgnoreCase("alamat")) {
                            sb.append("                      <div id='kolom_div_"+col+"_"+randomID+"' class='col-12'>\n");
                        } else {
                            sb.append("                      <div id='kolom_div_"+col+"_"+randomID+"' class='col-md-6'>\n");
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
	     					}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:300");
	     						// TODO: handle exception
	     					}
	                    	 
	                         sb.append("                    <label class='form-label fw-semibold' for=\"input_nama_"+col+"_"+randomID+"\">").append(label).append((paramRequired.contains(col) ? " *" : "")).append("</label>\n");
	                         String s = JSPCurdGenerator.pilihan(randomID, col, label, returnName, paramRequired.contains(col), "id", "nama", whereTambahan);  
	                         sb.append("                    "+s+"\n");
	                     }
	                     else if (type.equalsIgnoreCase("time")){
	                         sb.append("                    <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " *" : "")).append("</label>\n");
	                         sb.append("                    <input type='time' class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
	                     }
	                     else if (type.equalsIgnoreCase("date") || (returnName != null && returnName.equals(Date.class.getName()))) {
	                        sb.append("                    <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " *" : "")).append("</label>\n");
	                        sb.append("                    <input type='date' class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
	                    }
	                    else if (returnName != null && returnName.equals(Boolean.class.getName())) {
	                         sb.append("                    <div class='form-check form-switch mt-4'>\n");
	                         sb.append("                      <input class='form-check-input' type='checkbox' role='switch' id='input_").append(col).append("_").append(randomID).append("'>\n");
	                         sb.append("                      <label class='form-check-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " *" : "")).append("</label>\n");
	                         sb.append("                    </div>\n");
	                     } else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  && editor.equalsIgnoreCase("editor")) {
	                    	 String idData = "input_"+col+"_"+randomID;
	                    	 JSPCurdGenerator.textEditor(sb, col, label, idData, paramRequired.contains(col), randomID,"");
	                     } else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan") || col.equalsIgnoreCase("alamat")) {
	                         sb.append("                    <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " *" : "")).append("</label>\n");
	                         sb.append("                    <textarea class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" rows='3'></textarea>\n");
	                     } else {
	                         sb.append("                    <label class='form-label fw-semibold'>").append(label).append((paramRequired.contains(col) ? " *" : "")).append("</label>\n");
	                         sb.append("                    <input type='text' class='form-control' id='input_").append(col).append("_").append(randomID).append("' "+(paramRequired.contains(col) ? "required" : "")+" >\n");
	                     }
             		}
                     sb.append("                 </div>\n");
                 }
            } // End Loop Fields
            sb.append("              </div>\n");
            sb.append("            </div>\n"); // End Step Div
        } // End Loop Steps
        sb.append("          </div>\n");

        sb.append("        </form>\n");
        sb.append("      </div>\n");
        
        // --- FOOTER BUTTONS ---
        sb.append("      <div class='modal-footer justify-content-between'>\n");
        sb.append("        <button type='button' class='btn btn-secondary' data-bs-dismiss='modal'>"+Common.getBahasaConfig("Batal")+"</button>\n");
        sb.append("        <div>\n");
        sb.append("          <button type='button' class='btn btn-outline-primary me-2' id='btnPrev_").append(randomID).append("' onclick='prevStep_").append(randomID).append("()'><span class='fas fa-arrow-left'></span> Sebelumnya</button>\n");
        sb.append("          <button type='button' class='btn btn-primary' id='btnNext_").append(randomID).append("' onclick='nextStep_").append(randomID).append("()'>Lanjut <span class='fas fa-arrow-right'></span></button>\n");
        sb.append("          <button type='button' class='btn btn-success' id='btnSave_").append(randomID).append("' onclick='saveData_").append(randomID).append("()' style='display:none;'>"+Common.getBahasaConfig("Simpan")+" <span class='fas fa-save'></span></button>\n");
        sb.append("        </div>\n");
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
        
        // Wizard State
        sb.append(" var currentStep_").append(randomID).append(" = 0;\n");
        sb.append(" var totalSteps_").append(randomID).append(" = ").append(formCols.length).append(";\n");

        // --- WIZARD FUNCTIONS ---
        sb.append(" function showStep_").append(randomID).append("(n) {\n");
        sb.append("   // Hide all steps\n");
        sb.append("   for(let i=0; i<totalSteps_").append(randomID).append("; i++) {\n");
        sb.append("     document.getElementById('stepContent_").append(randomID).append("_' + i).classList.remove('active');\n");
        sb.append("     let ind = document.getElementById('wizardIndicator_").append(randomID).append("_' + i);\n");
        sb.append("     ind.classList.remove('active');\n");
        sb.append("     if(i < n) ind.classList.add('completed'); else ind.classList.remove('completed');\n");
        sb.append("   }\n");
        sb.append("   // Show current\n");
        sb.append("   document.getElementById('stepContent_").append(randomID).append("_' + n).classList.add('active');\n");
        sb.append("   document.getElementById('wizardIndicator_").append(randomID).append("_' + n).classList.add('active');\n");
        
        sb.append("   // Handle Buttons\n");
        sb.append("   document.getElementById('btnPrev_").append(randomID).append("').style.display = (n === 0) ? 'none' : 'inline-block';\n");
        sb.append("   if(n === totalSteps_").append(randomID).append(" - 1) {\n");
        sb.append("      document.getElementById('btnNext_").append(randomID).append("').style.display = 'none';\n");
        sb.append("      document.getElementById('btnSave_").append(randomID).append("').style.display = 'inline-block';\n");
        sb.append("   } else {\n");
        sb.append("      document.getElementById('btnNext_").append(randomID).append("').style.display = 'inline-block';\n");
        sb.append("      document.getElementById('btnSave_").append(randomID).append("').style.display = 'none';\n");
        sb.append("   }\n");
        sb.append("   currentStep_").append(randomID).append(" = n;\n");
        sb.append(" }\n");

        sb.append(" function nextStep_").append(randomID).append("() {\n");
        sb.append("   if(!validateStep_").append(randomID).append("(currentStep_").append(randomID).append(")) return;\n");
        sb.append("   if(currentStep_").append(randomID).append(" < totalSteps_").append(randomID).append(" - 1) {\n");
        sb.append("     showStep_").append(randomID).append("(currentStep_").append(randomID).append(" + 1);\n");
        sb.append("   }\n");
        sb.append(" }\n");

        sb.append(" function prevStep_").append(randomID).append("() {\n");
        sb.append("   if(currentStep_").append(randomID).append(" > 0) {\n");
        sb.append("     showStep_").append(randomID).append("(currentStep_").append(randomID).append(" - 1);\n");
        sb.append("   }\n");
        sb.append(" }\n");
        
        sb.append(" function jumpToStep_").append(randomID).append("(n) {\n");
        // Optional: Block jumping forward if previous steps invalid
        // sb.append("   if(n > currentStep_").append(randomID).append(" && !validateStep_").append(randomID).append("(currentStep_").append(randomID).append(")) return;\n");
        sb.append("   showStep_").append(randomID).append("(n);\n");
        sb.append(" }\n");
        
        // Validation per step
        sb.append(" function validateStep_").append(randomID).append("(stepIdx) {\n");
        sb.append("   let container = document.getElementById('stepContent_").append(randomID).append("_' + stepIdx);\n");
        sb.append("   let inputs = container.querySelectorAll('[required]');\n");
        sb.append("   let isValid = true;\n");
        sb.append("   inputs.forEach(input => {\n");
        sb.append("      if(!input.value || input.value.trim() === '') {\n");
        sb.append("         input.classList.add('is-invalid');\n");
        sb.append("         isValid = false;\n");
        sb.append("         // Optional: Attach listener to remove invalid class on input\n");
        sb.append("         input.addEventListener('input', function() { this.classList.remove('is-invalid'); }, {once:true});\n");
        sb.append("      }\n");
        sb.append("   });\n");
        sb.append("   if(!isValid) tampilkanToast('"+Common.getBahasaConfig("Harap lengkapi field bertanda bintang")+" (*)', 'bg-danger');\n");
        sb.append("   return isValid;\n");
        sb.append(" }\n");

        sb.append(" const isEmpty"+randomID+" = (val) => {\r\n"
                + "  return (val === undefined || val === null || (typeof val === \"string\" && val.trim().length === 0) || (Array.isArray(val) && val.length === 0));\r\n"
                + "};\n");

        // --- EXISTING CRUD LOGIC (Load, Save, Delete) ---
        // (Copied and adapted from original, keeping logic intact but flatting inputs for Save)
        
        sb.append(" async function loadData_").append(randomID).append("() {\n");
        sb.append("   const start = (page_").append(randomID).append(" - 1);\n");
        // ... (Logic search sama seperti original)
        // Construct WHERE clause dynamically from search inputs
        sb.append("   let whereClause = '';\n");
        sb.append("   let criteria = [];\n");
        for (String col : searchCols) {
        	String returnName = null;
			
        	try {
         		String[] ss = col.split(";");
         		col = ss.length>0 ? ss[0] : "";
         	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:449");
 				// TODO: handle exception
 			}
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            String elId = "search_" + col + "_" + randomID;
            
            
			
	         	
	         	if(col != null && !col.trim().isEmpty()) {
			
					if(returnName.startsWith("ais.database.model")) {
						 sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
			             sb.append("   if(val").append(col).append(") criteria.push(\"").append(col).append(" = \" + val").append(col).append(" + \" \");\n");
					}
					
					else if(col.equalsIgnoreCase("namaNegara")) { 
						
						 sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
			             sb.append("   if(val").append(col).append(" !== '') criteria.push(\"").append("nama_kegiatan").append(" ilike '%\" + val").append(col).append(" + \"%' \");\n");
			
					}    else if(col.equalsIgnoreCase("namaNegara")) { 
						
						sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
			            sb.append("   if(val").append(col).append(" !== '') criteria.push(\"").append("nama_kegiatan").append(" ilike '%\" + val").append(col).append(" + \"%' \");\n");
						
						
					}   else if(col.equalsIgnoreCase("namaSiswa")) { 
						
						sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
			            sb.append("   if(val").append(col).append(" !== '') criteria.push(\"").append("nama_siswa").append(" ilike '%\" + val").append(col).append(" + \"%' \");\n");
						
					
					} 
					
		            
					else if (returnName
							.equals(Boolean.class.getName())) {
		                sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
		                sb.append("   if(val").append(col).append(" !== '') criteria.push(\"").append(col).append(" = \" + val").append(col).append(");\n");
		            } else {
		                sb.append("   var val").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
		                sb.append("   if(val").append(col).append(") criteria.push(\"").append(col).append(" ilike '%\" + val").append(col).append(" + \"%' \");\n");
		            }
	         	}
        }
        sb.append("   if(criteria.length > 0) whereClause = criteria.join(' AND ');\n");
        
        // Projection
        StringBuilder projection = new StringBuilder("id");
        List<String> stringsCols = new ArrayList<String>();
        for(String col : columns) if(!stringsCols.contains(col)) stringsCols.add(col);
        for(String col : flatFormCols) if(!stringsCols.contains(col)) stringsCols.add(col);
        for(String col : stringsCols) if(!col.equals("id")) projection.append(";").append(col);
        
        sb.append("   var reqObj = {\n");
        sb.append("     \"action\": \"daftar\", \"class\": \"").append(modelClass).append("\",\n");
        sb.append("     \"projection\": \"").append(projection.toString()).append("\",\n");
        sb.append("     \"deep\": \"1\", \"max\": limit_").append(randomID).append(", \"halaman\": start,\n");
        sb.append("     \"order1\": sortOrder_").append(randomID).append(", \"sort1\": sortCol_").append(randomID).append(", \"count\": \"true\"\n");
        sb.append("   };\n");
        sb.append("   if(whereClause) reqObj[\"where1\"] = whereClause;\n");
        if(!sqlWhere.isEmpty()) sb.append("   reqObj[\"where2\"] = \""+sqlWhere+"\";\n");
        
        sb.append("   var req = JSON.stringify(reqObj);\n");
        sb.append("   var dataReq = encodeURIComponent(req);\n");
        sb.append("   const servletUrl = '"+Common.ROOT+"/Data?datasearch=' + dataReq;\n");
        sb.append("   try {\n");
        sb.append("     const response = await fetch(servletUrl);\n");
        sb.append("     const dataResponse = await response.json();\n");
        sb.append("     renderTable_").append(randomID).append("(dataResponse.data || []);\n");
        sb.append("     renderPagination_").append(randomID).append("(dataResponse.count || 0);\n");
        sb.append("     document.getElementById('pageInfo_").append(randomID).append("').innerText = 'Total: ' + (dataResponse.count || 0);\n");
        sb.append("   } catch (error) { console.error(error); }\n");
        sb.append(" }\n");

        // --- RENDER TABLE (Sama) ---
        sb.append(" function renderTable_").append(randomID).append("(data) {\n");
        sb.append("   const tbody = document.getElementById('tableBody_").append(randomID).append("');\n");
        sb.append("   tbody.innerHTML = '';\n");
        sb.append("   if (data.length === 0) { tbody.innerHTML = '<tr><td colspan=\"20\" class=\"text-center\">"+Common.getBahasaConfig("Tidak ada data")+"</td></tr>'; return; }\n");
        sb.append("   data.forEach(item => {\n");
        sb.append("     let tr = document.createElement('tr');\n");
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
         		String[] ss = col.split(";");
         		col = ss.length>0 ? ss[0] : "";
         	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:556");
 				// TODO: handle exception
 			}
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
             try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:565");}
             sb.append("     let td_").append(col).append(" = document.createElement('td');\n");
             if (returnName != null && returnName.equals(Boolean.class.getName())) {
                 sb.append("     let badgeClass"+col+" = item.").append(col).append("=='true' ? 'bg-success' : 'bg-secondary';\n");
                 sb.append("     let badgeText"+col+" = item.").append(col).append("=='true' ? 'Aktif' : 'Tidak Aktif';\n");
                 sb.append("     td_").append(col).append(".innerHTML = `<span class='badge ${badgeClass"+col+"}'>${badgeText"+col+"}</span>`;\n");
                 sb.append("     td_").append(col).append(".className = 'text-center';\n");
             } else {
                 sb.append("     td_").append(col).append(".innerText = item.").append(col).append(" || '-';\n");
             }
             sb.append("     tr.appendChild(td_").append(col).append(");\n");
        }
        sb.append("     let tdAction = document.createElement('td'); tdAction.className = 'text-center';\n");
        sb.append("     let itemStr = encodeURIComponent(JSON.stringify(item));\n");
        sb.append("     tdAction.innerHTML = `\n");
        sb.append("       <div class=\"dropdown font-sans-serif position-static\">\r\n");
        sb.append("         <button class=\"btn btn-link text-600 btn-sm dropdown-toggle btn-reveal\" type=\"button\" title=\"" + Common.getBahasaConfig("Aksi") + "\" aria-label=\"" + Common.getBahasaConfig("Aksi") + "\" data-bs-toggle=\"dropdown\"><span class=\"fas fa-ellipsis-h fs-10\"></span></button>\r\n");
        sb.append("         <div class=\"dropdown-menu dropdown-menu-end border py-0\">\r\n");
        sb.append("           <div class=\"py-2\">\n");
        sb.append("             <button class='dropdown-item' onclick='openModal_").append(randomID).append("(\"edit\", \"${itemStr}\")'><span class='fas fa-edit'></span> Ubah</button>\n");
        sb.append("             <button class='dropdown-item text-danger' data-id=\"${item.id}\" onclick=\"prosesDeleteData('"+modelClass+"', '${item.id}', function(){ loadData_"+randomID+"(); });\"><span class='fas fa-trash'></span> Hapus</button>\n");
        sb.append("           </div>\r\n");
        sb.append("         </div>\r\n");
        sb.append("       </div>`;\n");
        sb.append("     tr.appendChild(tdAction);\n");
        sb.append("     tbody.appendChild(tr);\n");
        sb.append("   });\n");
        sb.append(" }\n");

        // --- SAVE DATA (Flatten Logic) ---
        sb.append(" async function saveData_").append(randomID).append("() {\n");
        // Validate Last Step first
        sb.append("   if(!validateStep_").append(randomID).append("(currentStep_").append(randomID).append(")) return;\n");
        
        sb.append("   let dataObj = {};\n");
        sb.append("   let idVal = document.getElementById('inputId_").append(randomID).append("').value;\n");
        sb.append("   if(currentMode_").append(randomID).append(" === 'edit' && idVal) dataObj.id = parseInt(idVal);\n");
        
        if(hiddenValues != null) {
            for(String key : hiddenValues.keySet()) {
                 sb.append("   dataObj.").append(key).append(" = document.getElementById('input_"+key+"_").append(randomID).append("').value;\n");
            }
        }
        
        for (String col : flatFormCols) {
        	String returnName = null;
        	 try { String[] ss = col.split(";"); col = ss.length>0?ss[0]:""; }catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:611");}
			 
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
           
            if(col!=null && !col.trim().isEmpty()){
                if (returnName != null && returnName.equals(Boolean.class.getName())) {
                    sb.append("   dataObj.").append(col).append(" = document.getElementById('input_").append(col).append("_").append(randomID).append("').checked;\n");
                } else {
                    sb.append("   dataObj.").append(col).append(" = document.getElementById('input_").append(col).append("_").append(randomID).append("').value;\n");
                }
            }
        }
        
        String tidakBolehSama = "";
        for(String s : paramTidakBolehSama) tidakBolehSama += tidakBolehSama.isEmpty() ? s : ";"+s;
        
        sb.append("   var reqObj = {\n");
        sb.append("     \"action\": \"simpanDataRinci\", \"id\": dataObj.id==null ? -1 : dataObj.id,\n");
        sb.append("     \"tidakBolehSama\": \""+tidakBolehSama+"\", \"class\": \"").append(modelClass).append("\", \"data\": dataObj\n");
        sb.append("   };\n");
        sb.append("   var req = JSON.stringify(reqObj);\n");
        sb.append("   var dataReq = encodeURIComponent(req);\n");
        sb.append("   const servletUrl = '"+Common.ROOT+"/Data?datasearch=' + dataReq;\n");
        sb.append("   try {\n");
        sb.append("     const response = await fetch(servletUrl);\n");
        sb.append("     const dataResponse = await response.json();\n");
        sb.append("     if(dataResponse.status=='00'){ \n");
        sb.append("       updateBulkFiles(dataResponse.id);\n");
        sb.append("       tampilkanToast('"+Common.getBahasaConfig("Berhasil disimpan")+"', 'bg-success');\n");
        sb.append("       modalInstance_").append(randomID).append(".hide();\n");
        sb.append("       loadData_").append(randomID).append("();\n");
        sb.append("     } else { tampilkanToast(dataResponse.description, 'bg-danger'); }\n");
        sb.append("   } catch (e) { tampilkanToast('Gagal: ' + e, 'bg-danger'); }\n");
        sb.append(" }\n");
        
        
        // --- HELPER FUNCTIONS ---
        sb.append(" function toggleSearch_").append(randomID).append("() { var p = document.getElementById('searchPanel_").append(randomID).append("'); p.style.display = (p.style.display === 'none') ? 'block' : 'none'; }\n");
        sb.append(" function triggerSearch_").append(randomID).append("() { page_").append(randomID).append(" = 1; loadData_").append(randomID).append("(); }\n");
        sb.append(" function refreshData_").append(randomID).append("() { page_").append(randomID).append(" = 1; loadData_").append(randomID).append("(); }\n");
        sb.append(" function changeSort_").append(randomID).append("(col) { \n");
        sb.append("    if(sortCol_").append(randomID).append(" === col) sortOrder_").append(randomID).append(" = (sortOrder_").append(randomID).append(" === 'asc') ? 'desc' : 'asc';\n");
        sb.append("    else { sortCol_").append(randomID).append(" = col; sortOrder_").append(randomID).append(" = 'asc'; }\n");
        sb.append("    loadData_").append(randomID).append("();\n");
        sb.append(" }\n");

        // --- OPEN MODAL (Logic Reset & Fill for Wizard) ---
        sb.append(" function openModal_").append(randomID).append("(mode, itemEncoded) {\n");
        sb.append("   currentMode_").append(randomID).append(" = mode;\n");
        sb.append("   document.getElementById('dataForm_").append(randomID).append("').reset();\n");
        // Reset Wizard to Step 0
        sb.append("   showStep_").append(randomID).append("(0);\n");
        
        sb.append("   document.getElementById('modalTitle_").append(randomID).append("').innerText = (mode === 'add' ? 'Tambah' : 'Edit') + ' Data "+dataTypeName+"';\n");
        
        sb.append("   if(mode === 'edit' && itemEncoded) {\n");
        sb.append("     let item = JSON.parse(decodeURIComponent(itemEncoded));\n");
        sb.append("     document.getElementById('inputId_").append(randomID).append("').value = item.id;\n");
        
        // Fill Data Loop
        for(String col : flatFormCols) {
        	String returnName = null;
        	String type = "";
          	try {
          		String[] ss = col.split(";");
          		col = ss.length>0 ? ss[0] : "";
          		type = ss.length>1 ? ss[1] : "";
          	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicTableWizardGenerator.java:683");
  				// TODO: handle exception
  			}
			 
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            
          	
            if(col!=null && !col.trim().isEmpty()){
            	
            	if (col.equalsIgnoreCase("file") && Common.isValidJsonObject(type)) {
            		JSPCurdGenerator. renderUploadFile( sb,  type, col,  randomID);
        		} else if(returnName.startsWith("ais.database.model")) {
	 				sb.append("     document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_id)  ? item."+col+"_id : -1);\n");
	 				sb.append("     document.getElementById('input_nama_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_nama) ? item."+col+"_nama : '');\n");
	 			}
	 			
	 			else if (type.equalsIgnoreCase("time")){
	 				sb.append("     document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_time) ? item."+col+"_time : '');\n");
	 			} 
	 			
	 			
	 			else if ((type.equalsIgnoreCase("text") || type.equalsIgnoreCase("editor"))  || col.equalsIgnoreCase("keterangan") || col.equalsIgnoreCase("alamat")) {
	 				sb.append("     document.getElementById('input_").append(col).append("_").append(randomID).append("').innerHTML = (!isEmpty"+randomID+"(item.").append(col).append(") ? item."+col+" : '');\n");
	 			}

	 			
	 			else if (type.equalsIgnoreCase("date") || returnName
						.equals(Date.class.getName())) {
	 				sb.append("     document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append("_tgl) ? item."+col+"_tgl : '');\n");
	 			}
	         	
	 			else if (returnName
						.equals(Boolean.class.getName())) {
	                 sb.append("     document.getElementById('input_").append(col).append("_").append(randomID).append("').checked = item.").append(col).append("=='true';\n");
	            } else {
	                 sb.append("     document.getElementById('input_").append(col).append("_").append(randomID).append("').value = (!isEmpty"+randomID+"(item.").append(col).append(") ? item."+col+" : '');\n");
	            }
            }
        }
        sb.append("   }\n");
        
        sb.append("   var modalEl = document.getElementById('formModal_").append(randomID).append("');\n");
        sb.append("   modalInstance_").append(randomID).append(" = new bootstrap.Modal(modalEl);\n");
        sb.append("   // Animation Reset\n");
        sb.append("   const dialog = modalEl.querySelector('.modal-dialog');\n");
        sb.append("   dialog.classList.remove('animate__rotateInDownLeft');\n");
        sb.append("   void dialog.offsetWidth;\n");
        sb.append("   dialog.classList.add('animate__rotateInDownLeft');\n");
        sb.append("   modalInstance_").append(randomID).append(".show();\n");
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