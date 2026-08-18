<%@page session="false"%>
<%--
  Endpoint JSON untuk "Stok Opname By Scan HP / PDT" (versi JSP).
  Memakai backend bersama ais.action.master.inventory.StokOpnameScanUtil (sama dengan versi ZK).

  Aksi:
    - cariBarcode : cari produk dari barcode + stok sistem terkini.
    - simpan      : simpan daftar hasil scan (batch) lalu sesuaikan stok.
    - ringkasan   : angka ringkas opname hari ini (untuk panel dasbor).

  Sesi: memakai currentSession() (dikelola kerangka kerja) -> TIDAK ditutup manual.
  page session="false" mencegah bentrok variabel implicit "session" dengan Session Hibernate.
--%>
<%@page import="org.json.*"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.inventory.Produk"%>
<%@page import="ais.database.model.inventory.Toko"%>
<%@page import="ais.database.model.inventory.StokOpname"%>
<%@page import="ais.action.master.inventory.StokOpnameScanUtil"%>
<%@page import="ais.action.master.inventory.StokOpnameScanUtil.RingkasanOpname"%>
<%!
    /** Pedagang dikunci ke tokonya; admin boleh memakai parameter toko. */
    private Long resolveToko(HttpServletRequest request) {
        // Pedagang/toko dikunci ke tokonya sendiri; hanya admin-kantin boleh memakai parameter toko.
        Tbmuser u = Common.getCurrentUser(request);
        if (u != null && u.getPedagang() != null && u.getPedagang().getToko() != null) {
            return u.getPedagang().getToko().getId();
        }
        String p = request.getParameter("toko");
        if (p != null && p.trim().length() > 0) {
            try { return Long.valueOf(p.trim()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/stok/opname_scan_service.jsp:34"); /* abaikan */ }
        }
        return null;
    }
%>
<%
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
JSONObject result = new JSONObject();
try {
    Tbmuser tbmuser = Common.getCurrentUser(request);
    if (tbmuser == null || tbmuser.getUserId() == null) {
        result.put("status", "01");
        result.put("message", "Sesi berakhir, silakan masuk kembali.");
        out.print(result.toString());
        return;
    }
    String aksi = request.getParameter("aksi");
    // Sesi native REQUEST-SCOPED (ThreadLocal): DITUTUP TERPUSAT oleh FilterJSP di akhir request (clear+disconnect+close).
    // JANGAN closeSession()/session.close() manual di JSP -> clear() dapat membuang tulisan yang belum ter-flush (simpan gagal). Lihat COOKBOOK di HibernateUtil.
    Session session = HibernateUtil.currentNativeSession();
    Long tokoId = resolveToko(request);

    if ("cariBarcode".equals(aksi)) {
        if (tokoId == null) {
            result.put("status", "02"); result.put("message", "Toko/kios belum dipilih.");
            out.print(result.toString()); return;
        }
        String barcode = request.getParameter("barcode");
        Produk p = StokOpnameScanUtil.cariProdukByBarcode(session, tokoId, barcode);
        if (p == null) {
            result.put("status", "00"); result.put("found", false);
            result.put("message", "Barcode \"" + (barcode == null ? "" : barcode) + "\" tidak ditemukan pada toko ini.");
        } else {
            result.put("status", "00"); result.put("found", true);
            result.put("produkId", p.getId());
            result.put("kode", p.getKode() == null ? "" : p.getKode());
            result.put("nama", p.getNama() == null ? "" : p.getNama());
            result.put("stokSistem", StokOpnameScanUtil.hitungStokSistem(session, p.getId()));
            result.put("hargaJual", p.getHargaJual() == null ? 0 : p.getHargaJual());
        }

    } else if ("simpan".equals(aksi)) {
        if (tokoId == null) {
            result.put("status", "02"); result.put("message", "Toko/kios belum dipilih.");
            out.print(result.toString()); return;
        }
        String itemsStr = request.getParameter("items");
        if (itemsStr == null || itemsStr.trim().length() == 0) {
            result.put("status", "03"); result.put("message", "Tidak ada item untuk disimpan.");
            out.print(result.toString()); return;
        }
        String keterangan = request.getParameter("keterangan");
        JSONArray items = new JSONArray(itemsStr);
        JSONArray hasil = new JSONArray();
        int sukses = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject it = items.getJSONObject(i);
            Long produkId = it.has("produkId") ? Long.valueOf(it.get("produkId").toString()) : null;
            double fisik = it.has("fisik") ? Double.parseDouble(it.get("fisik").toString()) : 0d;
            if (produkId == null) { continue; }
            try {
                StokOpname so = StokOpnameScanUtil.simpanOpname(session, tokoId, produkId, fisik, keterangan,
                        tbmuser.getUserId());
                JSONObject h = new JSONObject();
                h.put("produkId", produkId);
                h.put("stokSistem", so.getStokSistem());
                h.put("stokFisik", so.getStokFisik());
                h.put("selisih", so.getSelisih());
                hasil.put(h);
                sukses++;
            } catch (Exception e) {
                JSONObject h = new JSONObject();
                h.put("produkId", produkId); h.put("error", e.getMessage());
                hasil.put(h);
            }
        }
        result.put("status", "00"); result.put("sukses", sukses);
        result.put("total", items.length()); result.put("hasil", hasil);

    } else if ("ringkasan".equals(aksi)) {
        RingkasanOpname r = StokOpnameScanUtil.ringkasanHariIni(session, tokoId);
        result.put("status", "00");
        result.put("jumlahCatatan", r.jumlahCatatan);
        result.put("jumlahProduk", r.jumlahProduk);
        result.put("totalLebih", r.totalLebih);
        result.put("totalKurang", r.totalKurang);
        result.put("selisihBersih", r.selisihBersih);

    } else {
        result.put("status", "98"); result.put("message", "Aksi tidak dikenal.");
    }
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/kantin/stok/opname_scan_service.jsp:127");
    try { result.put("status", "99"); result.put("message", "Error: " + e.getMessage()); } catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/kantin/stok/opname_scan_service.jsp:128"); }
}
out.print(result.toString());
%>
