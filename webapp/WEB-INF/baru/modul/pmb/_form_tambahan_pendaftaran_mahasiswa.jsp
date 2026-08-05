<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.*"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.common.Common"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String gelombangId = request.getParameter("gelombangId");
    String paketId = request.getParameter("paketId");
    String biodataId = request.getParameter("id"); // Untuk mode Edit

    // Hentikan eksekusi jika parameter utama tidak lengkap
    if (gelombangId == null || gelombangId.trim().isEmpty() || paketId == null || paketId.trim().isEmpty()) {
        return;
    }

    GelombangPendaftaran gel = (GelombangPendaftaran) GeneralValueObject.ambilData(GelombangPendaftaran.class, gelombangId, true);
    if (gel == null) return;
    Paket pkt = (Paket) GeneralValueObject.ambilData(Paket.class, paketId, true);

    // Load Biodata & Parsing Data Existing (Mode Edit)
    Map<String, String[]> existingData = new HashMap<String, String[]>();
    BiodataCalonMahasiswa biodata = null;
    if (biodataId != null && !biodataId.trim().isEmpty()) {
         biodata = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, biodataId, true);
        if (biodata != null && biodata.getParameterTambahanInds() != null && !biodata.getParameterTambahanInds().trim().isEmpty()) {
            String[] spl = biodata.getParameterTambahanInds().split("\n");
            for (String d : spl) {
                String[] value = d.split("<=>");
                if (value.length > 0) {
                    String jenis = value[0].trim();
                    String val = value.length > 1 ? value[1].trim() : "";
                    String url = value.length > 2 ? value[2].trim() : "";
                    String ket = value.length > 3 ? value[3].trim() : "";
                    existingData.put(jenis, new String[]{val, url, ket});
                }
            }
        }
    }

    Session hibSession = null;
    try {
        hibSession = HibernateUtil.openSession();
        // 1. Dapatkan Kelompok Parameter Tambahan
        List<KelompokParameterTambahanCalonMahasiswa> listKelompok = null;
        if (gel.getKelompokParameterTambahanCalonMahasiswas() != null && !gel.getKelompokParameterTambahanCalonMahasiswas().isEmpty()) {
            listKelompok = new ArrayList<KelompokParameterTambahanCalonMahasiswa>(gel.getKelompokParameterTambahanCalonMahasiswas());
        } else {
            listKelompok = ConstantValues.simpleList(
                hibSession.createCriteria(ParameterTambahanPaket.class)
                    .createAlias("parameterTambahan", "parameterTambahan")
                    .createAlias("kelompokParameterTambahanCalonMahasiswa", "kelompokParameterTambahanCalonMahasiswa")
                    .add(Restrictions.eq("parameterTambahan.aktif", true))
                    .add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
                    .add(Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", pkt)))
                    .add(Restrictions.or(
                        Restrictions.eq("tampilDiSemuaGelombang", true),
                        Restrictions.ilike("gelombangs", ";" + gel.getId() + ";", MatchMode.ANYWHERE)
                    ))
                    .setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa.id")),
                KelompokParameterTambahanCalonMahasiswa.class, false
            );
        }

        // Render HTML
        if (listKelompok != null && !listKelompok.isEmpty()) {
            Collections.sort(listKelompok);
            out.print("<div class=\"mt-4\">");

            // 2. Loop per Kelompok
            for (KelompokParameterTambahanCalonMahasiswa kelompok : listKelompok) {
                if (kelompok == null || kelompok.getTampilDiFormPendaftaran() == null || !kelompok.getTampilDiFormPendaftaran()) {
                    continue;
                }

                // Ambil Daftar Parameter dalam Kelompok Tersebut
                List<ParameterTambahan> listParams = ConstantValues.simpleList(
                    hibSession.createCriteria(ParameterTambahanPaket.class)
                        .createAlias("parameterTambahan", "parameterTambahan")
                        .createAlias("kelompokParameterTambahanCalonMahasiswa", "kelompokParameterTambahanCalonMahasiswa")
                        .add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa", kelompok))
                        .add(Restrictions.eq("parameterTambahan.aktif", true))
                        .add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
                        .add(Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", pkt)))
                        .add(Restrictions.or(
                            Restrictions.eq("tampilDiSemuaGelombang", true),
                            Restrictions.ilike("gelombangs", ";" + gel.getId() + ";", MatchMode.ANYWHERE)
                        ))
                        .setProjection(Projections.groupProperty("parameterTambahan.id")),
                    ParameterTambahan.class, false
                );
                
                if (listParams != null && !listParams.isEmpty()) {
                    Collections.sort(listParams);
                    // Header Kelompok
                    out.print("<h6 class=\"fw-bold mt-4 text-secondary border-bottom pb-1\"><i class=\"fas fa-list me-2\"></i>");
                    out.print(kelompok.getNama());
                    out.print("</h6><div class=\"row g-3 mb-3\">");

                    // Loop Input per Parameter
                    for (ParameterTambahan param : listParams) {
                        if (param == null) continue;
                        String jenis = kelompok.getId() + "->" + param.getId();
                        String[] existVal = existingData.get(jenis);
                        String value = (existVal != null && existVal[0] != null) ? existVal[0] : "";
                        String urlVal = (existVal != null && existVal.length > 1 && existVal[1] != null) ? existVal[1] : "";
                        String ketVal = (existVal != null && existVal.length > 2 && existVal[2] != null) ? existVal[2] : "";
                        boolean isWajib = param.getWajibDiisi() != null && param.getWajibDiisi();
                        String reqStar = isWajib ? " <span class=\"text-danger\">*</span>" : "";
                        
                        // Merangkai Metadata untuk diserap javascript
                        String safeKelompokNama = kelompok.getNama() != null ? kelompok.getNama().replace("'", "&#39;") : "";
                        String safeParamLabel = param.getLabelInputan() != null ? param.getLabelInputan().replace("'", "&#39;") : "";
                        Integer noUrut = param.getNomorUrut() != null ? param.getNomorUrut() : 1;
                        String metaData = "data-kelompok-id=\"" + kelompok.getId() + "\" " +
                                          "data-kelompok-nama=\"" + safeKelompokNama + "\" " +
                                          "data-param-id=\"" + param.getId() + "\" " +
                                          "data-param-label=\"" + safeParamLabel + "\" " +
                                          "data-param-urut=\"" + noUrut + "\" " +
                                          "data-param-jenis=\"" + jenis + "\" " + 
                                          (isWajib ? "data-wajib=\"true\"" : "");
                        String inputId = "param_" + param.getId();

                        // Container Input (+ atribut skip-logic: sembunyikan bila syarat tampil tak terpenuhi)
                        out.print("<div class=\"col-md-6 param-skip-wrapper\"" + ais.common.ParameterTambahanHtmlHelper.syaratTampilAttr(param) + ">");
                        
                        // Cek Pembungkus Border jika ada isian majemuk (Keterangan / Lampiran)
                        boolean hasKet = param.getTampilkanIsianKeterangan() != null && param.getTampilkanIsianKeterangan();
                        boolean hasLamp = param.getHarusMenyertakanLampiran() != null && param.getHarusMenyertakanLampiran();
                        
                        if (hasKet || hasLamp || ais.database.model.ParameterTambahan.PILIHAN_BANYAK.equals(param.getTipeDataInputan())) {
                            out.print("<div class=\"border rounded p-3 bg-white h-100 shadow-sm\">");
                        }
                        
                        // Label
                        out.print("<label class=\"form-label fw-semibold small text-primary\">");
                        out.print(param.getLabelInputan() + reqStar);
                        out.print("</label>");

                        // =========================================================================
                        // PANGGIL HELPER CLASS UNTUK RENDER HTML INPUT DINAMIS
                        // =========================================================================
                        String componentHtml = ais.common.ParameterTambahanHtmlHelper.generateHtmlComponent(
                             param, value, false, inputId, metaData
                        );
                        out.print(componentHtml);

                        // Render Keterangan Info (Default text)
                        if (param.getKeterangan() != null && !param.getKeterangan().trim().isEmpty()) {
                            out.print("<small class=\"text-muted d-block mt-1\"><i>" + param.getKeterangan() + "</i></small>");
                        }

                        // ---------------------------------------------------------
                        // Tambahan: Textarea Keterangan (jika diset Tampilkan Isian Keterangan)
                        // ---------------------------------------------------------
                        if (hasKet) {
                            String ketLabel = param.getLabelInputanKeterangan() != null && !param.getLabelInputanKeterangan().isEmpty() ? param.getLabelInputanKeterangan() : Common.getBahasaConfig("Keterangan");
                            out.print("<div class=\"mt-3 pt-2 border-top border-dashed\">");
                            out.print("<label class=\"form-label fw-semibold small text-secondary\"><i class=\"fas fa-pen me-1\"></i>" + ketLabel + "</label>");
                            out.print("<textarea class=\"form-control form-control-sm shadow-none param-ket\" data-param-jenis=\"" + jenis + "\" rows=\"2\" placeholder=\"" + Common.getBahasaConfig("Tambahkan keterangan di sini...") + "\" onchange=\"document.getElementById('" + inputId + "').setAttribute('data-param-ket', this.value)\">" + ketVal.replace("\"", "&quot;") + "</textarea>");
                            out.print("</div>");
                        }

                        // ---------------------------------------------------------
                        // Tambahan: File Lampiran (jika diset Harus Menyertakan Lampiran)
                        // ---------------------------------------------------------
                        if (hasLamp) {
                            // Tampilkan Dokumen Referensi Peserta (Bila ada)
                            LampiranLain lampirangYangHarisDitampilkanKePeserta = LampiranLain.ambil(param.getId(), ParameterTambahan.class.getName());
                            if(lampirangYangHarisDitampilkanKePeserta != null){
                            	String urlDownloadDokumen = lampirangYangHarisDitampilkanKePeserta.createLinkUri();
                                out.print("<div class=\"mb-3 p-3 border border-warning border-opacity-50 rounded-3 bg-warning bg-opacity-10\">");
                                out.print("<div class=\"fw-bold text-dark small mb-2\"><i class=\"fas fa-circle-info me-1 text-warning\"></i>" + Common.getBahasaConfig("Dokumen Referensi / Format Berkas") + ":</div>");
                                out.print("<a href=\"" + urlDownloadDokumen + "\" target=\"_blank\" class=\"btn btn-sm btn-warning text-dark rounded-pill fw-bold shadow-sm\"><i class=\"fas fa-download me-2\"></i>" + Common.getBahasaConfig("Unduh Referensi") + "</a>");
                                out.print("</div>");
                            }

                            if(biodata != null && biodata.getId() != null){
	                            boolean isLampWajib = param.getLampiranWajibDiisi() != null && param.getLampiranWajibDiisi();
	                            String reqLampiran = isLampWajib ? " <span class=\"text-danger\">*</span>" : "";
	                            
	                            out.print("<div class=\"mt-3 p-3 border border-info border-opacity-25 rounded-3 bg-light\">");
	                            out.print("<label class=\"form-label fw-semibold small text-info mb-2\"><i class=\"fas fa-cloud-arrow-up me-2\"></i>" + Common.getBahasaConfig("Unggah Dokumen") + reqLampiran + "</label>");
	                            
	                            // Eksekusi Pembuatan Komponen Upload Secara Dinamis
	                            String clazzVal = "ais.database.model.file.LampiranLain"; 
						        String jenisVal = "ParameterTambahan"; // Mengikuti jenis umum ParameterTambahan
						        String col = param.getId().toString();
						        String randomID = Common.getGeneratedBarCode(6);
	                            String targetDivId = "kolom_div_" + col + "_" + randomID;
	                            
	                            String urlFile = Common.ROOT + "/pmb?hanya_tampil_jsp=true&p=common&s=upload_component"
	                                    + "&clazz=" + URLEncoder.encode(clazzVal, "UTF-8") 
	                                    + "&jenis=" + URLEncoder.encode(jenisVal, "UTF-8")
	                                    + "&col=" + URLEncoder.encode(col, "UTF-8") 
	                                    + "&rnd=" + URLEncoder.encode(randomID, "UTF-8")
	                                    + "&id=" + (biodataId != null ? biodataId : "");
	
	                            out.print("<div id=\"" + targetDivId + "\" class=\"w-100 min-h-50px py-2 text-center bg-white border border-dashed rounded\">");
	                            out.print("<div class=\"spinner-border spinner-border-sm text-primary\"></div> <span class=\"small text-muted\">" + Common.getBahasaConfig("Memuat komponen unggah...") + "</span>");
	                            out.print("</div>");
	
	                            // Script untuk menarik komponen upload menggunakan fetch
	                            out.print("<script>");
	                            out.print("(async function() {");
	                            out.print("  try {");
	                            out.print("    const response" + randomID + " = await fetch('" + urlFile + "');");
	                            out.print("    const html" + randomID + " = await response" + randomID + ".text();");
	                            out.print("    const targetDiv = document.getElementById('" + targetDivId + "');");
	                            out.print("    if (targetDiv) {");
	                            out.print("      targetDiv.innerHTML = '';");
	                            out.print("      const range = document.createRange();");
	                            out.print("      range.selectNode(targetDiv);");
	                            out.print("      const fragment = range.createContextualFragment(html" + randomID + ");");
	                            out.print("      targetDiv.appendChild(fragment);");
	                            out.print("    }");
	                            out.print("  } catch(e) { console.error('Error fetching upload component:', e); }");
	                            out.print("})();");
	                            out.print("</script>");
	
	                            // Hidden input agar proses submit AJAX parent tetap mengenali komponen ini jika dibutuhkan
	                            out.print("<input type=\"hidden\" class=\"param-lampiran\" data-param-jenis=\"" + jenis + "\">");
	
	                            if(urlVal != null && !urlVal.isEmpty()) {
	                                out.print("<div class=\"mt-3 small\"><a href=\"" + urlVal + "\" target=\"_blank\" class=\"btn btn-sm btn-outline-success rounded-pill fw-bold text-decoration-none shadow-sm\"><i class=\"fas fa-circle-check me-1\"></i>" + Common.getBahasaConfig("Berkas tersimpan (Lihat/Unduh)") + "</a></div>");
	                            }
	                            
	                            out.print("</div>");
                            }
                        }
                        
                        if (hasKet || hasLamp || ais.database.model.ParameterTambahan.PILIHAN_BANYAK.equals(param.getTipeDataInputan())) {
                            out.print("</div>");
                        }
                        
                        out.print("</div>");
                    }
                    out.print("</div>");
                }
            }
            out.print("</div>");
        }
        
    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_form_tambahan_pendaftaran_mahasiswa.jsp:250");
        out.print("<div class=\"alert alert-danger p-3 small rounded-4 shadow-sm\"><i class=\"fas fa-triangle-exclamation me-2\"></i>" + Common.getBahasaConfig("Terjadi kesalahan saat memuat formulir tambahan.") + "</div>");
    } finally {
        if (hibSession != null) {
            try { hibSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_form_tambahan_pendaftaran_mahasiswa.jsp:254");}
            try { hibSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_form_tambahan_pendaftaran_mahasiswa.jsp:255");}
            try { hibSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_form_tambahan_pendaftaran_mahasiswa.jsp:256");}
        }
    }
%>

<script>
// Jadikan fungsi global dengan menempelkannya pada object window
window.syncPilihanBanyakParameterTambahan = function(paramId) {
    let chks = document.querySelectorAll('.chk-param-' + paramId + ':checked');
    let vals = Array.from(chks).map(c => c.value).join(';');
    
    // Sesuaikan target ID (bisa 'param_123' atau '123' tergantung passing helper)
    let hiddenInput = document.getElementById('param_' + paramId) || document.getElementById(paramId);
    if (hiddenInput) {
        hiddenInput.value = vals;
    }
};

// Handler Jika ada Pencarian Objek (Simulasi Bandbox)
window.bukaModalPencarianParameterLain = function(tipeData, inputId) {
    // Di sini Anda dapat menambahkan custom modal popup untuk mencari Dosen/Mahasiswa/Pegawai
    // Untuk saat ini, kita trigger prompt manual untuk simulasi
    let val = prompt('<%= Common.getBahasaConfigJS("Pencarian") %> ' + tipeData + '\n<%= Common.getBahasaConfig("Masukkan ID dan Nama (Format: ID->Nama Lengkap):") %> \nContoh: 15->Budi Santoso');
    if (val) {
        let hiddenInput = document.getElementById(inputId);
        if (hiddenInput) hiddenInput.value = val;
        let txtInput = document.getElementById('txt_' + inputId);
        if (txtInput) {
            let txt = val.includes('->') ? val.split('->')[1] : val;
            txtInput.value = txt;
        }
    }
};

// Auto-sync form Keterangan ke Attribute saat popup AJAX selesai dirender
(function initFormTambahan() {
    setTimeout(function() {
        let paramKets = document.querySelectorAll('.param-ket');
        paramKets.forEach(function(ta) {
            let jenis = ta.getAttribute('data-param-jenis');
            let mainInput = document.querySelector('.param-dinamis[data-param-jenis="' + jenis + '"]');
            if (mainInput && ta.value) {
                mainInput.setAttribute('data-param-ket', ta.value);
            }
        });
    }, 200); // Delay 200ms untuk memastikan elemen DOM telah terpasang sempurna di dalam Modal
})();
</script>
<%-- Mesin skip-logic LIVE: sembunyikan/tampilkan parameter bersyarat saat jawaban acuan berubah --%>
<%= ais.common.ParameterTambahanHtmlHelper.skipLogicScript() %>