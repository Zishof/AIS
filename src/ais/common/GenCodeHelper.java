package ais.common;

public class GenCodeHelper {

    /**
     * Men-generate fungsi JavaScript static untuk menyimpan data.
     * @param formNamaDiv     ID dari DIV parent yang menampung semua input.
     * @param clazz           Class Model Utama (Master).
     * @param idData          ID data (String). Jika null/kosong berarti mode ADD, jika ada berarti mode EDIT.
     * @param classRelasis    Array Class untuk model relasi (Child).
     * @param kolomRelasis    Array nama kolom di child yang berelasi dengan master (Foreign Key).
     * @param kodeUnik        Kode random untuk membedakan nama fungsi/variabel agar tidak bentrok.
     * @param afterSave       Nama fungsi JS yang dipanggil setelah simpan berhasil.
     * @param kolomWajibDiisi Array berisi nama-nama field (name/id) yang wajib diisi.
     * @param kolomWajibDiisiLabels Array berisi nama-nama label (name/id) yang wajib diisi.
     * @return String berupa kode JavaScript lengkap.
     */
    public static String simpanDataHelper(String formNamaDiv, Class<?> clazz, String idData, 
                                          Class<?>[] classRelasis, String[] kolomRelasis, 
                                          String kodeUnik, String afterSave, String[] kolomWajibDiisi, String[] kolomWajibDiisiLabels) {
        
        StringBuilder sb = new StringBuilder();
        String masterClassName = clazz.getName(); // Nama full class (e.g., com.app.model.User)
        
        // --- 1. Header Fungsi ---
        sb.append("\n// --- GENERATED SAVE FUNCTION ---\n");
        sb.append("async function simpanDataHelper_").append(kodeUnik).append("() {\n");
        sb.append("    try {\n");
        
        // --- 2. Validasi Kolom Wajib ---
        if (kolomWajibDiisi != null && kolomWajibDiisi.length > 0) {
            sb.append("\n        // Pengecekan Field Wajib Diisi\n");
            sb.append("        let wajibDiisi = [");
            for (int i = 0; i < kolomWajibDiisi.length; i++) {
                sb.append("'").append(kolomWajibDiisi[i]).append("'");
                if (i < kolomWajibDiisi.length - 1) sb.append(", ");
            }
            sb.append("];\n");
            
            sb.append("        let wajibDiisiLabel = [");
            for (int i = 0; i < kolomWajibDiisiLabels.length; i++) {
                sb.append("'").append(kolomWajibDiisiLabels[i]).append("'");
                if (i < kolomWajibDiisiLabels.length - 1) sb.append(", ");
            }
            sb.append("];\n");
            
            sb.append("        for (let i = 0; i < wajibDiisi.length; i++) {\n");
            sb.append("            let field = wajibDiisi[i];\n");
            // Cari elemen berdasarkan name atau id, serta akomodasi suffix _kodeUnik jika ada
            sb.append("            let el = document.querySelector(`[name=\"${field}\"]`) || \n");
            sb.append("                     document.querySelector(`[name=\"${field}_").append(kodeUnik).append("\"]`) || \n");
            sb.append("                     document.getElementById(field) || \n");
            sb.append("                     document.getElementById(field + '_").append(kodeUnik).append("');\n");
            
            sb.append("            if (el) {\n");
            sb.append("                let isValid = true;\n");
            sb.append("                if (el.type === 'checkbox' || el.type === 'radio') {\n");
            sb.append("                    if (!el.checked) isValid = false;\n");
            sb.append("                } else {\n");
            sb.append("                    if (!el.value || el.value.trim() === '') isValid = false;\n");
            sb.append("                }\n");
            sb.append("                if (!isValid) {\n");
            sb.append("                    if (typeof tampilkanToast === 'function') {\n");
            sb.append("                        tampilkanToast('' + wajibDiisiLabel[i] + ' wajib diisi!', 'bg-warning');\n");
            sb.append("                    } else {\n");
            sb.append("                        alert('' + wajibDiisiLabel[i] + ' wajib diisi!');\n");
            sb.append("                    }\n");
            sb.append("                    el.focus();\n");
            sb.append("                    return; // Hentikan eksekusi simpan\n");
            sb.append("                }\n");
            sb.append("            }\n");
            sb.append("        }\n");
        }
        
        // --- 3. Persiapan Config untuk Looping (Master + Relasi) ---
        sb.append("\n        // Konfigurasi Model (Index 0 = Master)\n");
        sb.append("        let saveConfigs = [\n");
        
        // Config Master
        sb.append("            { modelClass: '").append(masterClassName).append("', relCol: null },\n");
        
        // Config Child (jika ada)
        if (classRelasis != null && kolomRelasis != null && classRelasis.length == kolomRelasis.length) {
            for (int i = 0; i < classRelasis.length; i++) {
                sb.append("            { modelClass: '").append(classRelasis[i].getName())
                  .append("', relCol: '").append(kolomRelasis[i]).append("' },\n");
            }
        }
        sb.append("        ];\n");
        
        // Variabel untuk menampung Master ID yang baru disimpan
        sb.append("        let generatedMasterId = null;\n");
        
        // Ambil ID Master Existing (jika mode edit)
        if (idData != null && !idData.isEmpty()) {
            sb.append("        let currentMasterId = '").append(idData).append("';\n");
        } else {
            sb.append("        let currentMasterId = null;\n");
        }

        // --- 4. Mulai Loop Penyimpanan ---
        sb.append("\n        // Loop proses simpan (Master dulu, baru Child)\n");
        sb.append("        for (let i = 0; i < saveConfigs.length; i++) {\n");
        sb.append("            let conf = saveConfigs[i];\n");
        sb.append("            let dataObj = {};\n");
        
        // --- 5. Auto Scrape Data dari Elemen HTML ---
        sb.append("\n            // --- Auto Scrape Data dari Div: ").append(formNamaDiv).append(" ---\n");
        sb.append("            let container = document.getElementById('").append(formNamaDiv).append("');\n");
        sb.append("            if (!container) throw 'Container form dengan ID ").append(formNamaDiv).append(" tidak ditemukan';\n");
        
        sb.append("            let elements = container.querySelectorAll('input, textarea, select');\n");
        
        sb.append("            elements.forEach(el => {\n");
        sb.append("                let fieldName = el.getAttribute('name') || el.id;\n");
        sb.append("                if (fieldName) {\n");
        sb.append("                    let cleanName = fieldName.replace('_").append(kodeUnik).append("', '');\n");
        
        // Logic pengambilan value
        sb.append("                    if (el.type === 'checkbox') {\n");
        sb.append("                        dataObj[cleanName] = el.checked;\n");
        sb.append("                    } else if (el.type === 'radio') {\n");
        sb.append("                        if(el.checked) dataObj[cleanName] = el.value;\n");
        sb.append("                    } else {\n");
        sb.append("                        if(el.type === 'number' && el.value !== '') {\n");
        sb.append("                             dataObj[cleanName] = parseFloat(el.value);\n");
        sb.append("                        } else {\n");
        sb.append("                             dataObj[cleanName] = el.value;\n");
        sb.append("                        }\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            });\n");

        // --- 6. Handling ID dan Relasi ---
        sb.append("\n            // Set ID untuk Master (jika Edit Mode)\n");
        sb.append("            if (i === 0 && currentMasterId) {\n");
        sb.append("                dataObj['id'] = parseInt(currentMasterId);\n");
        sb.append("            }\n");

        sb.append("\n            // Set Foreign Key untuk Child (jika iterasi > 0)\n");
        sb.append("            if (i > 0) {\n");
        sb.append("                let refId = generatedMasterId || currentMasterId;\n");
        sb.append("                if (!refId) throw 'Gagal: ID Master tidak ditemukan untuk relasi child';\n");
        sb.append("                \n");
        sb.append("                if (conf.relCol) {\n");
        sb.append("                    dataObj[conf.relCol] = { id: parseInt(refId) };\n");
        sb.append("                }\n");
        sb.append("            }\n");

        // --- 7. Kirim Request via API (Fetch) ---
        sb.append("\n            // --- Kirim Request ---\n");
        sb.append("            let reqObj = {\n");
        sb.append("                \"action\": \"simpanDataRinci\",\n");
        sb.append("                \"id\": (dataObj.id ? dataObj.id : -1),\n");
        sb.append("                \"class\": conf.modelClass,\n");
        sb.append("                \"data\": dataObj\n");
        sb.append("            };\n");
        
        sb.append("            console.log('reqObj -> ', reqObj);\n");

        // Perubahan POST dan Body JSON
        sb.append("            let url = '"+Common.ROOT +"/Data';\n");
        sb.append("            let resp = await fetch(url, {\n");
        sb.append("                method: 'POST',\n");
        sb.append("                headers: {\n");
        sb.append("                    'Content-Type': 'application/json'\n");
        sb.append("                },\n");
        sb.append("                body: JSON.stringify(reqObj)\n");
        sb.append("            });\n");
        
        sb.append("            let json = await resp.json();\n");
        sb.append("            console.log('response -> ', json);\n");
        sb.append("            if (json.status !== '00') {\n");
        sb.append("                throw 'Gagal menyimpan ' + (i===0 ? 'Data Utama' : 'Data Relasi') + ': ' + json.description;\n");
        sb.append("            }\n");

        // --- 8. Tangkap ID Master Baru (jika mode tambah) ---
        sb.append("\n            if (i === 0) {\n");
        sb.append("                if (json.id) generatedMasterId = json.id;\n");
        sb.append("                if (json.data && json.data.id) generatedMasterId = json.data.id;\n");
        sb.append("            }\n");

        sb.append("        } // End Loop\n"); 

        // --- 9. Sukses & Callback ---
        sb.append("\n        // --- Sukses ---\n");
        sb.append("        if (typeof tampilkanToast === 'function') {\n");
        sb.append("            tampilkanToast('Seluruh data berhasil disimpan.', 'bg-success');\n");
        sb.append("        }\n");
        
        if (afterSave != null && !afterSave.isEmpty()) {
            String cmd = afterSave.trim();
            if (cmd.contains("(")) {
                if (cmd.endsWith(";")) {
                    cmd = cmd.substring(0, cmd.length() - 1);
                }
                sb.append("        ").append(cmd).append(";\n");
                
            } else {
                sb.append("        if (typeof ").append(cmd).append(" === 'function') {\n");
                sb.append("            ").append(cmd).append("();\n");
                sb.append("        }\n");
            }
        }
        
        sb.append("        if(generatedMasterId) { updateBulkFiles(generatedMasterId); }\n");
        sb.append("        var myModalElement = document.getElementById('").append(kodeUnik).append("');\n");
        sb.append("        if (myModalElement) {\n");
        sb.append("            var modalInstance = bootstrap.Modal.getInstance(myModalElement);\n");
        sb.append("            if (modalInstance) {\n");
        sb.append("                modalInstance.hide();\n");
        sb.append("            }\n");
        sb.append("        }\n");

        sb.append("    } catch (e) {\n");
        sb.append("        console.error(e);\n");
        sb.append("        if (typeof tampilkanToast === 'function') {\n");
        sb.append("            tampilkanToast('Terjadi Kesalahan: ' + e, 'bg-danger');\n");
        sb.append("        } else {\n");
        sb.append("            alert('Terjadi Kesalahan: ' + e);\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }
}