package ais.common;

import ais.database.model.ParameterTambahan;
import java.util.Arrays;
import java.util.List;

/**
 * Helper terfokus untuk parameter tambahan html. Tipe ini membungkus satu variasi kecil dari alur
 * yang lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan
 * implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code List TIPE_OBJECT_LOOKUP};
 * pembacaan/pencarian ({@code lolosSyaratTampil()}, {@code syaratTampilAttr()}); operasi domain lain ({@code
 * generateHtmlComponent()}, {@code nilaiCocok()}, {@code petaNilaiDariInds()}, {@code skipLogicScript()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class ParameterTambahanHtmlHelper {

    // Konstanta tipe pencarian objek / Bandbox
    private static final List<String> TIPE_OBJECT_LOOKUP = Arrays.asList(
        ParameterTambahan.PILIHAN_OBJECT, 
        ParameterTambahan.PILIHAN_MAHASISWA, 
        ParameterTambahan.PILIHAN_SISWA, 
        ParameterTambahan.PILIHAN_DOSEN, 
        ParameterTambahan.PILIHAN_GURU, 
        ParameterTambahan.PILIHAN_PEGAWAI, 
        ParameterTambahan.PILIHAN_PENYEDIA,
        ParameterTambahan.PILIHAN_KELAS_SISWA
    );

    /**
     * Method Utama untuk menghasilkan Komponen HTML dari ParameterTambahan
     * @param param ParameterTambahan yang sedang diproses
     * @param val Nilai yang tersimpan sebelumnya di database
     * @param isReadonly Apakah form dalam mode hanya baca
     * @param inputId ID element HTML yang akan di-generate
     * @param extraAttributes Atribut rahasia/metadata untuk JS (data-wajib, data-kelompok, dll)
     * @return String HTML Element (Input, Select, Textarea, dll)
     */
    public static String generateHtmlComponent(ParameterTambahan param, String val, boolean isReadonly, String inputId, String extraAttributes) {
        if (param == null) return "";

        String tipe = param.getTipeDataInputan() != null ? param.getTipeDataInputan() : ParameterTambahan.TEXT;
        
        // Ambil nilai default jika value database masih kosong
        if (val == null || val.trim().isEmpty()) {
            val = param.getNilaiDefault() != null ? param.getNilaiDefault() : "";
        }
        
        // Bersihkan escape String agar tidak bentrok dengan atribut HTML
        String safeVal = val.replace("\"", "&quot;");
        
        // Deteksi status Readonly / Disabled
        boolean lockData = isReadonly || (param.getNilaiTidakBolehDiubah() != null && param.getNilaiTidakBolehDiubah());
        String readonlyAttr = lockData ? " readonly class=\"form-control form-control-sm shadow-none param-dinamis bg-light\" " : " class=\"form-control form-control-sm shadow-none param-dinamis\" ";
        String disabledAttr = lockData ? " disabled " : "";

        // data-param-id disisipkan ke SETIAP value-holder (input/select/textarea/hidden) agar mesin
        // skip-logic (JavaScript) dapat menemukan & membaca nilai parameter ACUAN lintas kelompok saat
        // mengevaluasi syarat tampil/tidak suatu parameter. Tidak mengubah tampilan/perilaku input.
        String dataParamId = param.getId() != null ? " data-param-id=\"" + param.getId() + "\" " : " ";
        extraAttributes = dataParamId + (extraAttributes == null ? "" : extraAttributes);

        StringBuilder html = new StringBuilder();

        // 1. TIPE: TEXT LONG
        if (ParameterTambahan.TEXT.equals(tipe)) {
            int rows = param.getJumlahBaris() != null && param.getJumlahBaris() > 0 ? param.getJumlahBaris() : 2;
            int max = param.getJumlahText() != null && param.getJumlahText() > 0 ? param.getJumlahText() : 255;
            html.append("<textarea id=\"").append(inputId).append("\" ")
                .append(readonlyAttr.replace("input", "textarea")).append(" rows=\"").append(rows).append("\" ")
                .append(" maxlength=\"").append(max).append("\" ").append(extraAttributes).append(">")
                .append(safeVal).append("</textarea>");
        } 
        
        // 2. TIPE: ANGKA (Dengan Min/Max validation)
        else if (ParameterTambahan.ANGKA.equals(tipe)) {
            String min = param.getNilaiMin() != null ? " min=\"" + param.getNilaiMin() + "\"" : "";
            String max = param.getNilaiMax() != null ? " max=\"" + param.getNilaiMax() + "\"" : "";
            html.append("<input type=\"number\" id=\"").append(inputId).append("\" value=\"").append(safeVal).append("\" ")
                .append(min).append(max).append(readonlyAttr).append(extraAttributes).append(">");
        } 
        
        // 3. TIPE: TEXT ANGKA (Hanya bisa ketik nomor)
        else if (ParameterTambahan.TEXT_ANGKA.equals(tipe)) {
            html.append("<input type=\"text\" id=\"").append(inputId).append("\" value=\"").append(safeVal).append("\" ")
                .append(" onkeypress=\"return event.charCode >= 48 && event.charCode <= 57\" ")
                .append(readonlyAttr).append(extraAttributes).append(">");
        } 
        
        // 4. TIPE: TANGGAL
        else if (ParameterTambahan.TANGGAL.equals(tipe)) {
            html.append("<input type=\"date\" id=\"").append(inputId).append("\" value=\"").append(safeVal).append("\" ")
                .append(readonlyAttr).append(extraAttributes).append(">");
        } 
        
        // 5. TIPE: TANGGAL DAN WAKTU
        else if (ParameterTambahan.TANGGAL_DAN_WAKTU.equals(tipe)) {
            String valDt = safeVal.replace(" ", "T"); // Format DateTime-Local HTML5
            html.append("<input type=\"datetime-local\" id=\"").append(inputId).append("\" value=\"").append(valDt).append("\" ")
                .append(readonlyAttr).append(extraAttributes).append(">");
        } 
        
        // 6. TIPE: WAKTU
        else if (ParameterTambahan.WAKTU.equals(tipe)) {
            html.append("<input type=\"time\" id=\"").append(inputId).append("\" value=\"").append(safeVal).append("\" ")
                .append(readonlyAttr).append(extraAttributes).append(">");
        } 
        
        // 7. TIPE: PILIHAN YA/TIDAK
        else if (ParameterTambahan.PILIHAN_YA_TIDAK.equals(tipe)) {
            if (lockData) {
                String display = "true".equalsIgnoreCase(val) ? "Ya" : "false".equalsIgnoreCase(val) ? "Tidak" : safeVal;
                html.append("<input type=\"text\" id=\"").append(inputId).append("\" value=\"").append(display).append("\" ")
                    .append(readonlyAttr).append(extraAttributes).append(">");
            } else {
                html.append("<select id=\"").append(inputId).append("\" class=\"form-select form-select-sm shadow-none param-dinamis\" ").append(extraAttributes).append(">");
                html.append("<option value=\"\">== Pilih ==</option>");
                html.append("<option value=\"true\"").append("true".equalsIgnoreCase(val) ? " selected" : "").append(">Ya</option>");
                html.append("<option value=\"false\"").append("false".equalsIgnoreCase(val) ? " selected" : "").append(">Tidak</option>");
                html.append("</select>");
            }
        } 
        
        // 8. TIPE: PILIHAN CUSTOM (Perbaikan Logika Sesuaikan dengan ParameterTambahanAstract)
        else if (ParameterTambahan.PILIHAN_CUSTOM.equals(tipe)) { 
            String optionsData = param.getNilaiDataInputan();
            
            if (lockData) {
                // Konversi value ke Label jika dikunci
                String displayVal = safeVal;
                if (optionsData != null && !optionsData.isEmpty()) {
                    String[] ss = optionsData.split(";");
                    Arrays.sort(ss);
                    for (String s : ss) {
                        String[] ssss = s.split(":");
                        String label = ssss[0].trim();
                        // 1. Cocokkan dengan seluruh string s (perilaku default ZK)
                        if (s.equals(val)) {
                            displayVal = label;
                            break;
                        }
                        // 2. Cocokkan dengan value kedua (perilaku fallback ZK)
                        if (ssss.length > 1 && ssss[1].trim().equalsIgnoreCase(val)) {
                            displayVal = label;
                            break;
                        }
                    }
                }
                html.append("<input type=\"text\" id=\"").append(inputId).append("\" value=\"").append(displayVal.replace("\"", "&quot;")).append("\" ")
                    .append(readonlyAttr).append(extraAttributes).append(">");
            } else {
                // Render Select Dropdown
                html.append("<select id=\"").append(inputId).append("\" class=\"form-select form-select-sm shadow-none param-dinamis\" ").append(disabledAttr).append(extraAttributes).append(">");
                html.append("<option value=\"\">== Pilih ==</option>");
                
                if (optionsData != null && !optionsData.isEmpty()) {
                    String[] ss = optionsData.split(";");
                    Arrays.sort(ss);
                    
                    // Pass 1: Tentukan nilai yang benar-benar terpilih (Koreksi Fallback ZK)
                    String matchedValue = val;
                    for (String s : ss) {
                        String[] ssss = s.split(":");
                        if (!s.equals(val) && ssss.length > 1 && ssss[1].trim().equalsIgnoreCase(val)) {
                            matchedValue = s;
                            break;
                        }
                    }

                    // Pass 2: Render Options
                    for (String s : ss) {
                        String[] ssss = s.split(":");
                        String label = ssss[0].trim();
                        String value = s; // PERHATIAN: Di ZK, value yang disimpan adalah keseluruhan string 's'
                        
                        String sel = value.equals(matchedValue) ? " selected" : "";
                        html.append("<option value=\"").append(value.replace("\"", "&quot;")).append("\"").append(sel).append(">").append(label).append("</option>");
                    }
                }
                html.append("</select>");
            }
        } 
        
        // 9. TIPE: PILIHAN BANYAK (Checkboxes Multi-select)
        else if (ParameterTambahan.PILIHAN_BANYAK.equals(tipe)) {
            html.append("<div class=\"border p-2 rounded bg-light mb-2 overflow-auto\" style=\"max-height: 150px;\">");
            if (param.getNilaiDataInputan() != null && !param.getNilaiDataInputan().isEmpty()) {
                String[] opts = param.getNilaiDataInputan().split(";");
                for (int i = 0; i < opts.length; i++) {
                    String op = opts[i].trim();
                    // Logika checked ZKoss (disimpan digabung dengan titik koma)
                    boolean checked = (";" + val + ";").contains(";" + op + ";") || val.equals(op);
                    
                    html.append("<div class=\"form-check mb-1\">");
                    html.append("<input class=\"form-check-input chk-param-").append(param.getId()).append("\" type=\"checkbox\" ")
                        .append("value=\"").append(op.replace("\"", "&quot;")).append("\" ")
                        .append("id=\"chk_").append(param.getId()).append("_").append(i).append("\" ")
                        .append(checked ? "checked " : "").append(disabledAttr)
                        .append(" onchange=\"syncPilihanBanyakParameterTambahan('").append(param.getId()).append("')\">");
                    html.append("<label class=\"form-check-label small\" for=\"chk_").append(param.getId()).append("_").append(i).append("\">").append(op).append("</label>");
                    html.append("</div>");
                }
            }
            html.append("</div>");
            html.append("<input type=\"hidden\" id=\"").append(inputId).append("\" class=\"param-dinamis\" value=\"").append(safeVal).append("\" ").append(extraAttributes).append(">");
        } 
        
        // 10. TIPE: PENCARIAN OBJECT (Bandbox di ZK) -> Diubah jadi Input Group + Modal Search di HTML
        else if (TIPE_OBJECT_LOOKUP.contains(tipe)) {
            html.append("<div class=\"input-group input-group-sm\">");
            html.append("<input type=\"hidden\" id=\"").append(inputId).append("\" class=\"param-dinamis\" value=\"").append(safeVal).append("\" ").append(extraAttributes).append(">");
            
            // Format ZK untuk object biasanya disimpan dengan gaya: ID->Nama
            String displayVal = safeVal;
            if(safeVal.contains("->")) {
                 String[] spl = safeVal.split("->");
                 displayVal = spl.length > 1 ? spl[1] : spl[0];
            }
            
            html.append("<input type=\"text\" id=\"txt_").append(inputId).append("\" class=\"form-control shadow-none bg-white\" value=\"").append(displayVal).append("\" readonly placeholder=\"Klik tombol cari...\">");
            
            if (!lockData) {
                html.append("<button class=\"btn btn-outline-primary\" type=\"button\" onclick=\"bukaModalPencarianParameterLain('").append(tipe).append("', '").append(inputId).append("')\"><i class=\"fas fa-search\"></i></button>");
            }
            html.append("</div>");
        } 
        
        // 11. TIPE: MATRIX KOMPLEKS (Readonly Info)
        else if (Arrays.asList(ParameterTambahan.PILIHAN_MATRIX, ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI, ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO).contains(tipe)) {
            html.append("<textarea id=\"").append(inputId).append("\" class=\"form-control form-control-sm shadow-none param-dinamis bg-light border-warning\" rows=\"3\" readonly placeholder=\"(Data Matrix Kompleks)\" ")
                .append(extraAttributes).append(">").append(safeVal).append("</textarea>");
            html.append("<small class=\"text-danger d-block mt-1\" style=\"font-size:10px;\"><i>*Pengeditan matrix disarankan via Aplikasi Desktop Admin.</i></small>");
        } 
        
        // DEFAULT FALLBACK: TEXT
        else {
            html.append("<input type=\"text\" id=\"").append(inputId).append("\" value=\"").append(safeVal).append("\" ")
                .append(readonlyAttr).append(extraAttributes).append(">");
        }
        
        return html.toString();
    }

    /**
     * Pencocokan nilai parameter ACUAN dengan nilai SYARAT (dipakai bersama oleh evaluator ZK maupun
     * mesin JavaScript agar hasilnya konsisten). Bersifat "label-aware": untuk pilihan custom yang di
     * ZK disimpan sebagai gabungan {@code "Label:value"}, membandingkan terhadap keseluruhan string,
     * terhadap bagian LABEL saja, serta terhadap tiap token (untuk pilihan banyak, digabung {@code ";"}).
     * Juga mengenali sinonim ya/tidak (true/false). Case-insensitive.
     *
     * @param stored  nilai jawaban parameter acuan yang sedang berlaku (mis. "Bekerja:1" atau "true" atau "a;b")
     * @param expected nilai syarat yang ditetapkan admin (mis. "Bekerja", "Ya")
     * @return true bila cocok.
     */
    public static boolean nilaiCocok(String stored, String expected) {
        String e = (expected == null ? "" : expected).trim().toLowerCase();
        String s = (stored == null ? "" : stored).trim();
        if (e.isEmpty()) {
            return s.isEmpty();
        }
        String sl = s.toLowerCase();
        if (sl.equals(e)) {
            return true;
        }
        if (sl.equals("true") && (e.equals("ya") || e.equals("y") || e.equals("benar"))) {
            return true;
        }
        if (sl.equals("false") && (e.equals("tidak") || e.equals("tdk") || e.equals("t") || e.equals("no") || e.equals("salah"))) {
            return true;
        }
        String[] toks = sl.split(";");
        for (int i = 0; i < toks.length; i++) {
            String tok = toks[i].trim();
            if (tok.equals(e)) {
                return true;
            }
            int ci = tok.indexOf(':');
            if (ci > 0 && tok.substring(0, ci).trim().equals(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluasi SYARAT TAMPIL (skip-logic) sisi-server untuk perender ZK. Parameter TAMPIL bila tak bersyarat,
     * atau bila nilai parameter acuan cocok (via {@link #nilaiCocok(String, String)}) sesuai logika AND/OR pada
     * JSON {@code syaratTampil}. Nilai acuan diambil dari peta {@code parameterId -> nilai tersimpan}.
     *
     * @param param parameter yang dievaluasi.
     * @param nilaiByParamId peta id-parameter → nilai jawaban yang berlaku.
     * @return true bila parameter harus ditampilkan (JSON kosong/invalid → true, aman).
     */
    public static boolean lolosSyaratTampil(ParameterTambahan param, java.util.Map<Long, String> nilaiByParamId) {
        try {
            String json = param == null ? null : param.getSyaratTampil();
            if (json == null || json.trim().isEmpty()) {
                return true;
            }
            org.json.JSONObject obj = new org.json.JSONObject(json.trim());
            org.json.JSONArray arr = obj.optJSONArray("syarat");
            if (arr == null || arr.length() == 0) {
                return true;
            }
            boolean or = "OR".equalsIgnoreCase(obj.optString("logika", "AND"));
            boolean hasilAnd = true;
            boolean hasilOr = false;
            int dievaluasi = 0;
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject c = arr.optJSONObject(i);
                if (c == null) {
                    continue;
                }
                long pid = c.optLong("parameterId", 0L);
                if (pid == 0L) {
                    continue;
                }
                String expected = c.optString("nilai", "");
                String stored = nilaiByParamId == null ? null : nilaiByParamId.get(Long.valueOf(pid));
                boolean ok = nilaiCocok(stored, expected);
                hasilAnd = hasilAnd && ok;
                hasilOr = hasilOr || ok;
                dievaluasi++;
            }
            if (dievaluasi == 0) {
                return true;
            }
            return or ? hasilOr : hasilAnd;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Membangun peta {@code parameterId -> nilai} dari string jawaban ter-serialisasi (format ZK:
     * baris {@code "kelompokId->parameterId<=>nilai<=>..."} dipisah baris-baru). Dipakai evaluator ZK.
     */
    public static java.util.Map<Long, String> petaNilaiDariInds(String indsSerial) {
        java.util.Map<Long, String> peta = new java.util.HashMap<Long, String>();
        if (indsSerial == null || indsSerial.trim().isEmpty()) {
            return peta;
        }
        String[] baris = indsSerial.split("\n");
        for (int i = 0; i < baris.length; i++) {
            String d = baris[i];
            if (d == null || d.trim().isEmpty()) {
                continue;
            }
            String[] value = d.split("<=>");
            String jenis = value.length > 0 ? value[0].trim() : "";
            String val = value.length > 1 ? value[1].trim() : "";
            int ar = jenis.indexOf("->");
            if (ar >= 0) {
                try {
                    peta.put(Long.valueOf(jenis.substring(ar + 2).trim()), val);
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ParameterTambahanHtmlHelper.java:346");
                    // abaikan token id tak valid
                }
            }
        }
        return peta;
    }

    /**
     * Mengembalikan atribut HTML {@code data-syarat-tampil='<json>'} (sudah di-escape untuk atribut ber-quote
     * tunggal) untuk dipasang pada elemen PEMBUNGKUS parameter di JSP. Mesin skip-logic membaca atribut ini
     * untuk menentukan apakah pembungkus tampil. Mengembalikan string kosong bila parameter tak bersyarat.
     */
    public static String syaratTampilAttr(ParameterTambahan param) {
        if (param == null) {
            return "";
        }
        String j = param.getSyaratTampil();
        if (j == null || j.trim().isEmpty()) {
            return "";
        }
        String esc = j.trim()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        return " data-syarat-tampil='" + esc + "' ";
    }

    /**
     * Mesin skip-logic LIVE (JavaScript, idempoten) untuk form kuesioner JSP. Menyembunyikan/menampilkan
     * pembungkus parameter secara real-time saat responden mengubah jawaban parameter acuan, mengevaluasi
     * atribut {@code data-syarat-tampil} (JSON: {@code {"logika":"AND|OR","syarat":[{"parameterId":..,"nilai":".."}]}}).
     * Membaca nilai acuan lewat {@code [data-param-id]}. Saat pembungkus disembunyikan, atribut {@code required}
     * pada input di dalamnya dilepas (dan dipulihkan saat tampil) agar validasi form tidak memblok field tersembunyi.
     * Cocokkan nilai memakai logika yang setara dengan {@link #nilaiCocok(String, String)}.
     *
     * <p>Aman dipanggil banyak kali (mis. beberapa fragmen JSP): inisialisasi dijaga oleh flag global.</p>
     */
    public static String skipLogicScript() {
        return "\n<script>\n"
            + "(function(){\n"
            + "  if (window.__paramSkipLogicInit) { if(window.__evalParamSkipLogic) window.__evalParamSkipLogic(); return; }\n"
            + "  window.__paramSkipLogicInit = true;\n"
            + "  function cocok(stored, expected){\n"
            + "    var e = (expected==null?'':(''+expected)).trim().toLowerCase();\n"
            + "    var s = (stored==null?'':(''+stored)).trim();\n"
            + "    if (e==='') return s==='';\n"
            + "    var sl = s.toLowerCase();\n"
            + "    if (sl===e) return true;\n"
            + "    if (sl==='true' && (e==='ya'||e==='y'||e==='benar')) return true;\n"
            + "    if (sl==='false' && (e==='tidak'||e==='tdk'||e==='t'||e==='no'||e==='salah')) return true;\n"
            + "    var toks = sl.split(';');\n"
            + "    for (var i=0;i<toks.length;i++){\n"
            + "      var tok = toks[i].trim();\n"
            + "      if (tok===e) return true;\n"
            + "      var ci = tok.indexOf(':');\n"
            + "      if (ci>0 && tok.substring(0,ci).trim()===e) return true;\n"
            + "    }\n"
            + "    return false;\n"
            + "  }\n"
            + "  function bacaNilai(pid){\n"
            + "    var els = document.querySelectorAll(\"[data-param-id='\"+pid+\"']\");\n"
            + "    for (var i=0;i<els.length;i++){ var v=els[i].value; if(v!=null && (''+v).trim()!=='') return ''+v; }\n"
            + "    return '';\n"
            + "  }\n"
            + "  function setTampil(w, show){\n"
            + "    if (show){\n"
            + "      w.style.display='';\n"
            + "      var r=w.querySelectorAll(\"[data-was-required='1']\");\n"
            + "      for (var i=0;i<r.length;i++){ r[i].setAttribute('required','required'); r[i].removeAttribute('data-was-required'); }\n"
            + "      var rw=w.querySelectorAll('[data-was-wajib]');\n"
            + "      for (var k=0;k<rw.length;k++){ rw[k].setAttribute('data-wajib', rw[k].getAttribute('data-was-wajib')); rw[k].removeAttribute('data-was-wajib'); }\n"
            + "    } else {\n"
            + "      w.style.display='none';\n"
            + "      var q=w.querySelectorAll('[required]');\n"
            + "      for (var j=0;j<q.length;j++){ q[j].removeAttribute('required'); q[j].setAttribute('data-was-required','1'); }\n"
            + "      var qw=w.querySelectorAll('[data-wajib]');\n"
            + "      for (var m=0;m<qw.length;m++){ qw[m].setAttribute('data-was-wajib', qw[m].getAttribute('data-wajib')); qw[m].removeAttribute('data-wajib'); }\n"
            + "    }\n"
            + "  }\n"
            + "  function evalSemua(){\n"
            + "    var ws = document.querySelectorAll('[data-syarat-tampil]');\n"
            + "    for (var i=0;i<ws.length;i++){\n"
            + "      var w = ws[i], raw = w.getAttribute('data-syarat-tampil');\n"
            + "      if (!raw) continue;\n"
            + "      var rule; try { rule = JSON.parse(raw); } catch(e){ continue; }\n"
            + "      var arr = rule.syarat || [];\n"
            + "      if (!arr.length){ continue; }\n"
            + "      var or = (''+(rule.logika||'AND')).toUpperCase()==='OR';\n"
            + "      var hAnd=true, hOr=false, n=0;\n"
            + "      for (var j=0;j<arr.length;j++){\n"
            + "        var c = arr[j]; if(!c || !c.parameterId) continue;\n"
            + "        var ok = cocok(bacaNilai(c.parameterId), c.nilai);\n"
            + "        hAnd = hAnd && ok; hOr = hOr || ok; n++;\n"
            + "      }\n"
            + "      var show = (n===0) ? true : (or ? hOr : hAnd);\n"
            + "      setTampil(w, show);\n"
            + "    }\n"
            + "  }\n"
            + "  window.__evalParamSkipLogic = evalSemua;\n"
            + "  var t=null;\n"
            + "  function jadwalkan(){ if(t) clearTimeout(t); t=setTimeout(evalSemua, 30); }\n"
            + "  document.addEventListener('change', jadwalkan, false);\n"
            + "  document.addEventListener('input', jadwalkan, false);\n"
            + "  if (document.readyState==='loading') document.addEventListener('DOMContentLoaded', evalSemua);\n"
            + "  else evalSemua();\n"
            + "})();\n"
            + "</script>\n";
    }
}
