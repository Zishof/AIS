package ais.common;

import java.util.Map;
import java.util.Set;

import org.hibernate.metadata.ClassMetadata;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

public class DynamicFormGenerator {
	
	
	/**
     * Menghasilkan HTML dan JavaScript khusus untuk Form Input (Tambah/Edit) dan logic Penyimpanan.
     * Tidak menampilkan tabel data.
     *
     * @param columns       Array nama field (untuk label mapping)
     * @param labels        Array label header (untuk label mapping)
     * @param formCols      Array nama field yang muncul di form
     * @param randomID      String unik untuk suffix ID elemen
     * @param dataTypeName  Nama entitas untuk display
     * @param modelClass    Nama full class Java Model
     * @param paramRequired Set field yang wajib diisi
     * @return String berisi kode HTML dan Javascript form
     * @throws Exception 
     */
    public static String generateForm(String[] labels, 
                                      String[] formCols, String randomID, String dataTypeName, 
                                      String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama, GeneralValueObject generalValueObject) throws Exception {
    	Map<String,String> hiddenValues = null;
        return generateForm( labels, 
                formCols,  randomID,  dataTypeName, 
                 modelClass,  paramRequired,  paramTidakBolehSama,  generalValueObject,  hiddenValues);
    }

    /**
     * Menghasilkan HTML dan JavaScript khusus untuk Form Input (Tambah/Edit) dan logic Penyimpanan.
     * Tidak menampilkan tabel data.
     *
     * @param columns       Array nama field (untuk label mapping)
     * @param labels        Array label header (untuk label mapping)
     * @param formCols      Array nama field yang muncul di form
     * @param randomID      String unik untuk suffix ID elemen
     * @param dataTypeName  Nama entitas untuk display
     * @param modelClass    Nama full class Java Model
     * @param paramRequired Set field yang wajib diisi
     * @return String berisi kode HTML dan Javascript form
     * @throws Exception 
     */
    @SuppressWarnings("rawtypes")
    public static String generateForm(String[] labels, 
                                      String[] formCols, String randomID, String dataTypeName, 
                                      String modelClass, Set<String> paramRequired, Set<String> paramTidakBolehSama, GeneralValueObject generalValueObject, Map<String,String> hiddenValues) throws Exception {
        
        Class clazz = Class.forName(modelClass);
        ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
        StringBuilder sb = new StringBuilder();

        // --- 1. CSS & Style Wrapper ---
        sb.append("<div id='formWrapper_").append(randomID).append("'>\n");
        sb.append("<style>\n");
        sb.append(" .form-container { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0.125rem 0.25rem rgba(0,0,0,0.075); }\n");
        sb.append("</style>\n");

        // --- 2. Form Container ---
        sb.append("<div class='container-fluid mb-4'>\n");
        sb.append("  <div class='form-container'>\n");
        
        // Header Form
        sb.append("    <div class='d-flex justify-content-between align-items-center mb-4 border-bottom pb-2'>\n");
        sb.append("      <h4 class='mb-0 text-primary'><span class='fas fa-edit'></span> Form ").append(dataTypeName).append("</h4>\n");
        sb.append("    </div>\n");

        // Form Body
        sb.append(JSPCurdGenerator.pilihanValSemua( randomID,  formCols, paramRequired,  generalValueObject, hiddenValues,  labels,   classMetadata));
        
        
        
        
        sb.append("  </div>\n"); // End form-container
        sb.append("</div>\n"); // End container-fluid

        // --- 4. Javascript Logic ---
        sb.append("<script>\n");
        
        sb.append(" const isEmpty"+randomID+" = (val) => {\r\n"
        		+ "  return (\r\n"
        		+ "    val === undefined || \r\n"
        		+ "    val === null || \r\n"
        		+ "    (typeof val === \"string\" && val.trim().length === 0) || \r\n"
        		+ "    (Array.isArray(val) && val.length === 0) || \r\n"
        		+ "    (typeof val === \"object\" && Object.keys(val).length === 0)\r\n"
        		+ "  );\r\n"
        		+ "};");
        

        // --- SAVE DATA FUNCTION ---
        sb.append(" async function saveData_").append(randomID).append("() {\n");   
        
        
    
        
        sb.append("   let data = {};\n");
        sb.append("   let idVal = document.getElementById('inputId_").append(randomID).append("').value;\n");
        sb.append("   if(idVal) data.id = idVal;\n");
        
        if(hiddenValues != null) {
        	for(String key : hiddenValues.keySet()) {
        		 sb.append("   data.").append(key).append(" = document.getElementById('input_"+key+"_").append(randomID).append("').value;\n");
        	}
        }
        
        // Loop ambil value dari input
        for (String col : formCols) {
        	try {
         		String[] ss = col.split(";");
         		col = ss.length>0 ? ss[0] : "";
         	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DynamicFormGenerator.java:119");
 				// TODO: handle exception
 			}
            String returnName = null;
            try {
				returnName = classMetadata.getPropertyType(col).getReturnedClass().getName();
			}catch (Exception e) {
				continue;
//				e.printStackTrace();
			}
            
         	
         	if(col != null && !col.trim().isEmpty()) {
	            String elId = "input_" + col + "_" + randomID;
	            
	            if (returnName != null && returnName.equals(Boolean.class.getName())) {
	                sb.append("   data.").append(col).append(" = document.getElementById('").append(elId).append("').checked;\n");
	            } else if (returnName != null && returnName.startsWith("ais.database.model")) {
	            	sb.append("   data.").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
	            } else {
	                sb.append("   data.").append(col).append(" = document.getElementById('").append(elId).append("').value;\n");
	            }
         	}
        }
        
        
        // Validation
        sb.append(JSPCurdGenerator.validation( randomID,  formCols,  labels, paramRequired));
        
        String tidakBolehSama = "";
        for(String s : paramTidakBolehSama) {
        	tidakBolehSama += tidakBolehSama.isEmpty() ? s : ";"+s;
        }

    

        sb.append("   var reqObj = {\n");
        sb.append("     \"action\": \"simpanDataRinci\",\n");
        sb.append("     \"id\": data.id==null ? -1 : data.id,\n");
        sb.append("     \"tidakBolehSama\": \""+tidakBolehSama+"\",\n");
        sb.append("     \"class\": \"").append(modelClass).append("\",\n");
        sb.append("     \"data\": data\n"); // object langsung, nanti stringify
        sb.append("   };\n");
        
        
        
        sb.append("   // Note: 'data' property in prompt sample was JSON Object string inside JSON string. Adjusting based on logic:\n");
        // Memperbaiki format JSON sesuai permintaan (API mengharapkan structure JSON string didalam JSON)
        // Namun fetch biasanya mengirim JSON standard. Mengikuti pola prompt:
        sb.append("   // Construct string req manually to match prompt pattern exactly if needed, or use JSON.stringify safely.\n");
        sb.append("   // Implementation using standard JSON approach for safety:\n");
        sb.append("   var req = JSON.stringify(reqObj);\n");
        
        sb.append("   var dataReq = encodeURIComponent(req);\n");
        sb.append("   const servletUrl = '"+Common.ROOT+"/Data?datasearch=' + dataReq;\n");

        sb.append("   try {\n");
        sb.append("     const response = await fetch(servletUrl);\n");
        sb.append("     if (!response.ok) throw new Error(response.status);\n");
        
        sb.append("     const dataResponse = await response.json();\n");
        
        sb.append("   if(dataResponse.status=='00'){ \n");
        sb.append("     	tampilkanToast('Data berhasil disimpan', 'bg-success');"
        		+ " 		updateBulkFiles(dataResponse.id);"
        		+ "                    var myModalElement = document.getElementById('"+randomID+"');\r\n"
        		+ "\r\n"
        		+ "                    // 2. Dapatkan instance modal yang sedang aktif\r\n"
        		+ "                    // Gunakan 'getInstance' jika modal sudah terbuka.\r\n"
        		+ "                    var modalInstance = bootstrap.Modal.getInstance(myModalElement); \r\n"
        		+ "\r\n"
        		+ "                    // ATAU gunakan 'getOrCreateInstance' jika Anda ragu apakah instance-nya sudah ada atau belum\r\n"
        		+ "                    // var modalInstance = bootstrap.Modal.getOrCreateInstance(myModalElement);\r\n"
        		+ "\r\n"
        		+ "                    // 3. Panggil fungsi hide()\r\n"
        		+ "                    if (modalInstance) {\r\n"
        		+ "                        modalInstance.hide();\r\n"
        		+ "                    }"
        		+ "\n");
        
        
        
        
        sb.append("     }\n");
        sb.append("   if(dataResponse.status!='00'){ \n");
        sb.append("     tampilkanToast(dataResponse.description, 'bg-danger');\n");
        sb.append("     }\n");
        sb.append("   } catch (e) { tampilkanToast('Gagal simpan: ' + e, 'bg-danger'); }\n");
        sb.append(" }\n");


        sb.append("</script>\n");
        sb.append("</div>\n"); // End wrapper

        return sb.toString();
    }
}