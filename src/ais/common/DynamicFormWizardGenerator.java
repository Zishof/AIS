package ais.common;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

public class DynamicFormWizardGenerator {

    /**
     * Entry point sederhana (Updated with stepTitles)
     */
    public static String generateForm(String[][] labels, String[][] formCols, String[] stepTitles, String randomID, String dataTypeName, 
                                      String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama, GeneralValueObject generalValueObject, String afterSave) throws Exception {
        return generateForm(labels, formCols, stepTitles, randomID, dataTypeName, modelClass, paramRequired, paramTidakBolehSama, generalValueObject, null, afterSave);
    }

    /**
     * Generator Form Wizard Modern & Optimal
     */
    @SuppressWarnings({ "rawtypes" })
    public static String generateForm(String[][] labels, String[][] formCols, String[] stepTitles, String randomID, String dataTypeName, 
                                      String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama, 
                                      GeneralValueObject generalValueObject, Map<String,String> hiddenValues, String afterSave) throws Exception {
        
        // 1. Inisialisasi Metadata & Data
        Class clazz = Class.forName(modelClass);
        ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
        
        // Refresh session jika object ada
        if(generalValueObject != null && generalValueObject.getId() != null) {
            try {
                Session session = HibernateUtil.currentNativeSession();
                session.refresh(generalValueObject);
                // session.disconnect();
                if (session.isOpen()) {session.disconnect();session.close();}
                HibernateUtil.closeSession();
            } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DynamicFormWizardGenerator.java:42"); }
        }

        StringBuilder sb = new StringBuilder();
        int totalSteps = formCols.length;
        Long objId = (generalValueObject == null || generalValueObject.getId() == null) ? -1L : generalValueObject.getId();

        // 2. CSS Minimalis
        // Kita hanya mempertahankan CSS penting untuk bentuk lingkaran dan garis connector.
        // Sisanya menggunakan Bootstrap Utility Classes.
        sb.append("<div id='wizardWrapper_").append(randomID).append("'>\n");
        sb.append("<style>\n");
        sb.append(" .wiz-container { position: relative; }\n");
        // Garis connector diatur agar berada di tengah lingkaran (top: 20px asumsi tinggi circle 40px)
        sb.append(" .wiz-connector { position: absolute; top: 20px; left: 0; right: 0; height: 3px; background: #e9ecef; z-index: 0; transform: translateY(-50%); }\n");
        sb.append(" .wiz-circle { width: 40px; height: 40px; border-radius: 50%; background: #fff; border: 2px solid #dee2e6; color: #6c757d; font-weight: bold; display: flex; align-items: center; justify-content: center; z-index: 2; transition: all 0.3s ease; margin: 0 auto; }\n");
        // State colors
        sb.append(" .wiz-item.active .wiz-circle { border-color: #0d6efd; background-color: #0d6efd; color: #fff; box-shadow: 0 0 0 4px rgba(13, 110, 253, 0.2); }\n");
        sb.append(" .wiz-item.done .wiz-circle { border-color: #198754; background-color: #198754; color: #fff; }\n");
        // Content animation
        sb.append(" .wiz-content { display: none; animation: fadeInWiz 0.4s ease-in-out; }\n");
        sb.append(" .wiz-content.active { display: block; }\n");
        sb.append(" @keyframes fadeInWiz { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }\n");
        sb.append("</style>\n");

        // 3. Container Card Modern
        sb.append("<div class='card shadow border-0 rounded-3 overflow-hidden'>\n");
        
        // --- HEADER: Title & Stepper ---
        sb.append("  <div class='card-header bg-white border-bottom pt-4 pb-0'>\n");
        sb.append("    <div class='d-flex justify-content-between align-items-center mb-4 px-2'>\n");
        sb.append("       <h5 class='m-0 fw-bold text-primary'><i class='fas fa-laptop-code me-2'></i>").append(dataTypeName).append("</h5>\n");
        sb.append("       <span class='badge bg-light text-secondary rounded-pill border'>"+(generalValueObject==null||generalValueObject.getId()==null ? "Data Baru" : "Berubah:"+Common.dateFormat51.get().format(generalValueObject.getTanggal_dirubah()) )+"</span>\n");
        sb.append("    </div>\n");

        // --- STEPPER VISUALIZATION ---
        sb.append("    <div class='wiz-container d-flex justify-content-between mb-4 position-relative'>\n");
        sb.append("      <div class='wiz-connector mx-4'></div>\n"); // Garis Connector
        
        for(int i=0; i<totalSteps; i++) {
            // Judul Step: Ambil dari array parameter, fallback ke "Step X" jika null/habis
            String title = (stepTitles != null && stepTitles.length > i) ? stepTitles[i] : "Step " + (i+1);
            
            // Item Wrapper (Circle + Title)
            // Menggunakan 'flex-fill' agar lebar terbagi rata
            sb.append("      <div id='wizItem_").append(i).append("_").append(randomID).append("' ")
              .append("class='wiz-item flex-fill text-center ").append(i==0 ? "active" : "").append("' ")
              .append("style='cursor:pointer; z-index:2;' ")
              .append("onclick='jumpToStep_").append(randomID).append("(").append(i).append(")'>\n");
            
            // Circle Number
            sb.append("        <div class='wiz-circle mb-2'>").append(i+1).append("</div>\n");
            
            // Step Title Text
            sb.append("        <small id='wizTitle_").append(i).append("_").append(randomID).append("' class='d-block fw-bold text-uppercase small ")
              .append(i==0 ? "text-primary" : "text-muted").append("'>").append(title).append("</small>\n");
            
            sb.append("      </div>\n");
        }
        sb.append("    </div>\n"); // End Stepper Container
        sb.append("  </div>\n"); // End Header

        // --- BODY: Form Content ---
        sb.append("  <div class='card-body p-4 bg-light bg-opacity-10'>\n");
        
        sb.append( JSPCurdGenerator.pilihanValSemuaWizard(randomID, formCols, objId, hiddenValues, labels, classMetadata,
				generalValueObject, paramRequired));
         
        sb.append("  </div>\n"); // End Card Body

        // --- FOOTER: Tombol Navigasi (DIPINDAHKAN KE SINI) ---
        sb.append("  <div class='card-footer bg-white border-top py-3'>\n");
        sb.append("    <div class='d-flex justify-content-between'>\n");
        sb.append("      <button type='button' class='btn btn-light text-secondary border' id='btnPrev_").append(randomID).append("' onclick='prevStep_").append(randomID).append("()' style='display:none;'><i class='fas fa-arrow-left me-1'></i> "+Common.getBahasaConfig("Kembali")+"</button>\n");
        sb.append("      <div class='ms-auto'>\n"); // Spacer agar tombol next di kanan
        sb.append("        <button type='button' class='btn btn-primary px-4' id='btnNext_").append(randomID).append("' onclick='nextStep_").append(randomID).append("()'>"+Common.getBahasaConfig("Langkah berikutnya")+" <i class='fas fa-arrow-right ms-1'></i></button>\n");
        sb.append("        <button type='button' class='btn btn-success px-4' id='btnSave_").append(randomID).append("' onclick='saveData_").append(randomID).append("()' style='display:none;'><i class='fas fa-check-circle me-1'></i> "+Common.getBahasaConfig("Selesai dan simpan")+"</button>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n"); // End Card Footer

        sb.append("</div>\n"); // End Card Wrapper
        sb.append("</div>\n"); // End Wizard Wrapper

        // --- JAVASCRIPT LOGIC ---
        sb.append("<script>\n");
        sb.append("(function(){\n"); 
        sb.append("  let currStep = 0;\n");
        sb.append("  const totalSteps = ").append(totalSteps).append(";\n");
        sb.append("  const rID = '").append(randomID).append("';\n");

        sb.append("  const isEmpty"+randomID+" = (v) => (v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));\n");

        // UI Updater (Modified to handle Title colors)
        sb.append("  window.updateUI_").append(randomID).append(" = function() {\n");
        sb.append("    for(let i=0; i<totalSteps; i++) {\n");
        sb.append("      let content = document.getElementById('stepContent_' + i + '_' + rID);\n");
        sb.append("      let item = document.getElementById('wizItem_' + i + '_' + rID);\n");
        sb.append("      let title = document.getElementById('wizTitle_' + i + '_' + rID);\n");
        
        sb.append("      content.classList.remove('active');\n");
        sb.append("      item.classList.remove('active', 'done');\n");
        sb.append("      title.className = 'd-block fw-bold text-uppercase small text-muted';\n"); // Reset to muted

        sb.append("      if(i === currStep) {\n");
        sb.append("        content.classList.add('active');\n");
        sb.append("        item.classList.add('active');\n");
        sb.append("        title.classList.remove('text-muted'); title.classList.add('text-primary');\n"); // Active color
        sb.append("      } else if (i < currStep) {\n");
        sb.append("        item.classList.add('done');\n");
        sb.append("        title.classList.remove('text-muted'); title.classList.add('text-success');\n"); // Done color
        sb.append("      }\n");
        sb.append("    }\n");
        
        // Button Logic
        sb.append("    document.getElementById('btnPrev_' + rID).style.display = (currStep === 0) ? 'none' : 'inline-block';\n");
        sb.append("    if(currStep === totalSteps - 1) {\n");
        sb.append("      document.getElementById('btnNext_' + rID).style.display = 'none';\n");
        sb.append("      document.getElementById('btnSave_' + rID).style.display = 'inline-block';\n");
        sb.append("    } else {\n");
        sb.append("      document.getElementById('btnNext_' + rID).style.display = 'inline-block';\n");
        sb.append("      document.getElementById('btnSave_' + rID).style.display = 'none';\n");
        sb.append("    }\n");
        sb.append("  };\n");

        // Validate Function
        sb.append("  window.validateStep_").append(randomID).append(" = function(stepIdx) {\n");
        for(int i=0; i<formCols.length; i++) {
            sb.append("    if(stepIdx === ").append(i).append(") {\n");
            for(String col : formCols[i]) {
                String pureCol = col.split(";")[0];
                if(paramRequired.contains(pureCol)) {
                    sb.append("      let el = document.getElementById('input_").append(pureCol).append("_").append(randomID).append("');\n");
                    sb.append("      if(!el || isEmpty"+randomID+"(el.value)) {\n");
                    sb.append("         if(typeof tampilkanToast === 'function') tampilkanToast('Kolom ini wajib diisi!', 'bg-danger'); else alert('Wajib diisi');\n");
                    sb.append("         el.focus(); return false;\n");
                    sb.append("      }\n");
                }
            }
            sb.append("    }\n");
        }
        sb.append("    return true;\n");
        sb.append("  };\n");

        // Navigation Functions
        sb.append("  window.nextStep_").append(randomID).append(" = function() {\n");
        sb.append("    if(!validateStep_").append(randomID).append("(currStep)) return;\n");
        sb.append("    if(currStep < totalSteps - 1) { currStep++; updateUI_").append(randomID).append("(); }\n");
        sb.append("  };\n");

        sb.append("  window.prevStep_").append(randomID).append(" = function() {\n");
        sb.append("    if(currStep > 0) { currStep--; updateUI_").append(randomID).append("(); }\n");
        sb.append("  };\n");

        // Clickable Jump Logic
        sb.append("  window.jumpToStep_").append(randomID).append(" = function(targetStep) {\n");
        sb.append("    if(targetStep < currStep) { currStep = targetStep; updateUI_").append(randomID).append("(); return; }\n");
        sb.append("    if(targetStep > currStep) {\n");
        sb.append("       for(let i = currStep; i < targetStep; i++) {\n");
        sb.append("         if(!validateStep_").append(randomID).append("(i)) { currStep = i; updateUI_").append(randomID).append("(); return; }\n");
        sb.append("       }\n");
        sb.append("       currStep = targetStep; updateUI_").append(randomID).append("();\n");
        sb.append("    }\n");
        sb.append("  };\n");

        // Save Data Logic
        sb.append("  window.saveData_").append(randomID).append(" = async function() {\n");
        sb.append("    if(!validateStep_").append(randomID).append("(currStep)) return;\n");
        sb.append("    let d = {};\n");
        sb.append("    d.id = document.getElementById('inputId_' + rID).value;\n");
        
        if(hiddenValues != null) {
            for(String k : hiddenValues.keySet()) sb.append("    d.").append(k).append(" = document.getElementById('input_").append(k).append("_' + rID).value;\n");
        }
        int indexJ = 0;
        for (String[] stepCols : formCols) {
            for (String col : stepCols) {
                 String pureCol = col.split(";")[0];
                 if(pureCol.isEmpty()) continue;
                 String retType = "";
                 try { retType = classMetadata.getPropertyType(pureCol).getReturnedClass().getName(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicFormWizardGenerator.java:222");}
                 
                 if(Boolean.class.getName().equals(retType)) {
                     sb.append("    d.").append(pureCol).append(" = document.getElementById('input_").append(pureCol).append("_' + rID).checked;\n");
                 } else {
                     sb.append("    d.").append(pureCol).append(" = document.getElementById('input_").append(pureCol).append("_' + rID).value;\n");
                 }
            }
            
            int index = 0;
            for (String col : stepCols) {
           	 // Logic label generator (sama seperti kode asli)
               String textWithSpace = col.replaceAll("([a-z])([A-Z]+)", "$1 $2");
               String result = textWithSpace.substring(0, 1).toUpperCase() + textWithSpace.substring(1);
               String label = result;
           	try {
           		label = labels[indexJ][index];
           	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicFormWizardGenerator.java:239");
   				// TODO: handle exception
   			}
           	index++;
   	       	 
   	         	try {
   	         		String[] ss = col.split(";");
   	         		col = ss.length>0 ? ss[0] : "";
   	         		
   	         	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicFormWizardGenerator.java:248");
   	 				// TODO: handle exception
   	 			}
   	         	if(col != null && !col.trim().isEmpty()) {
   			       	if(paramRequired.contains(col)) {
   			       		sb.append("    if(isEmpty"+randomID+"(d."+col+")) { tampilkanToast('"+Common.getBahasaConfig(label) +" "+Common.getBahasaConfig("wajib diisi")+"!', 'bg-danger'); return; }");
   			       	}
   	         	}
           }
            indexJ ++;
        }
        
        
        
        String tidakBolehSama = "";
        for(String s : paramTidakBolehSama) tidakBolehSama += tidakBolehSama.isEmpty() ? s : ";"+s;
        
        sb.append("    let reqObj = { action: 'simpanDataRinci', id: (d.id || -1), tidakBolehSama: '").append(tidakBolehSama).append("', class: '").append(modelClass).append("', data: d };\n");
        sb.append("    try {\n");
        sb.append("      let res = await fetch('").append(Common.ROOT).append("/Data?datasearch=' + encodeURIComponent(JSON.stringify(reqObj)));\n");
        sb.append("      let json = await res.json();\n");
        sb.append("      if(json.status === '00') {\n");
        sb.append("      updateBulkFiles(json.id);\n");
        sb.append("        if(typeof tampilkanToast === 'function') tampilkanToast('Berhasil tersimpan!', 'bg-success');\n"+afterSave+"\n");
        sb.append("        let modalEl = document.getElementById(rID) || document.getElementById('modal_' + rID) || document.querySelector('.modal.show');\n");
        sb.append("        if(modalEl && window.bootstrap) { let mi = bootstrap.Modal.getInstance(modalEl); if(mi) mi.hide(); }\n");
        sb.append("      } else { if(typeof tampilkanToast === 'function') tampilkanToast(json.description, 'bg-danger'); else alert(json.description); }\n");
        sb.append("    } catch(e) { console.error(e); if(typeof tampilkanToast === 'function') tampilkanToast('Error: ' + e, 'bg-danger'); }\n");
        sb.append("  };\n");

        sb.append("})();\n"); 
        sb.append("</script>\n");

        return sb.toString();
    }
}