package ais.action.servlet.api;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.ProduksiDokumen;
import ais.database.model.inventory.ProduksiDokumenBaris;
import ais.database.model.inventory.ProduksiDokumenEvent;
import ais.database.model.inventory.MutasiStokProduksi;
import ais.database.model.inventory.PengajuanPembelianGudang;
import ais.database.model.inventory.ReservasiStokProduksi;
import ais.database.model.inventory.Toko;
import ais.database.model.inventory.ProduksiGenealogiLot;

/** Adapter API produksi. Skema tabel tetap sepenuhnya dikelola Hibernate. */
public final class ProduksiApiHelper {
 private static final Map<String, String> JENIS = new HashMap<String, String>();
 static {
  JENIS.put("bill_of_material", "BOM");
  JENIS.put("work_order", "WO");
  JENIS.put("material_issue", "ISSUE");
  JENIS.put("material_return", "RETURN");
  JENIS.put("production_output", "OUTPUT");
  JENIS.put("production_waste", "WASTE");
  JENIS.put("production_cost", "COST");
  JENIS.put("production_unbuild", "UNBUILD");
  JENIS.put("quality_alert", "QC");
 }
 private ProduksiApiHelper() { }

 private static void tolak(JSONObject hasil, String pesan) throws Exception {
  hasil.put("status", "91"); hasil.put("message", pesan);
 }
 private static String jenis(JSONObject request) { return request == null ? "" : request.optString("jenis", "").trim(); }
 private static BigDecimal desimal(JSONObject j, String nama) {
  Object v = j == null ? null : j.opt(nama);
  if (v == null || JSONObject.NULL.equals(v) || String.valueOf(v).trim().length() == 0) return BigDecimal.ZERO;
  try { return new BigDecimal(String.valueOf(v).replace(",", "").trim()); }
  catch (Exception e) { return BigDecimal.ZERO; }
 }
 private static Long nullableLong(JSONObject j, String nama) {
  if (j == null || !j.has(nama) || j.isNull(nama)) return null;
  long v = j.optLong(nama, 0L); return v <= 0L ? null : Long.valueOf(v);
 }
 private static EbisnisActorContextResolver.ActorContext aktor(Session s, Tbmuser user,
   JSONObject request, JSONObject hasil, String aksi) throws Exception {
  EbisnisActorContextResolver.ActorContext ctx = EbisnisActorContextResolver.resolve(s, user);
  String j = jenis(request);
  if (!JENIS.containsKey(j)) { tolak(hasil, "Jenis dokumen produksi tidak dikenal."); return null; }
  if (!ctx.bolehAksi("produksi_" + j, aksi)) { tolak(hasil, "Akses produksi ini tidak diizinkan."); return null; }
  if (ctx.tokoId == null && !ctx.admin) { tolak(hasil, "Toko aktif belum dipilih."); return null; }
  if (!tabelProduksiTersedia(s)) {
   hasil.put("status", "91");
   hasil.put("errorCode", "PRODUCTION_SCHEMA_NOT_READY");
   hasil.put("message", "Modul Produksi belum siap di server karena tabel produksi belum terbentuk. "
     + "Muat ulang atau menekan tombol yang sama tidak akan menyelesaikannya. "
     + "Silakan hubungi admin dan sertakan kode PRODUCTION_SCHEMA_NOT_READY.");
   hasil.put("userAction", "Tutup halaman Produksi dan hubungi admin. Data lain tidak berubah.");
   hasil.put("adminAction", "Restart aplikasi server agar hbm2ddl.auto=update membuat koperasi.production_document "
     + "beserta tabel produksi terkait (skema koperasi selalu ada -- tidak ada DDL manual). "
     + "Setelah restart, verifikasi tabel dan buka kembali menu Produksi.");
   return null;
  }
  return ctx;
 }

 /**
  * Gerbang seluruh aksi Produksi. Namespace schema adalah prasyarat environment;
  * tabel di dalamnya tetap dibuat/diubah oleh mapping Hibernate saat bootstrap.
  * Pemeriksaan ini sengaja berupa DML/read-only dan tidak menjalankan DDL di
  * tengah request. Dengan demikian database yang belum siap menghasilkan pesan
  * edukatif, bukan stack trace SQLGrammarException berulang.
  */
 private static boolean tabelProduksiTersedia(Session s) {
  Object jumlah = s.createSQLQuery("SELECT COUNT(*) FROM information_schema.tables "
    + "WHERE table_schema='koperasi' AND table_name='production_document'")
    .uniqueResult();
  return jumlah instanceof Number && ((Number) jumlah).longValue() > 0L;
 }
 private static long toko(EbisnisActorContextResolver.ActorContext ctx, JSONObject request) {
  long requested = request == null ? 0L : request.optLong("tokoId", 0L);
  if (ctx.admin && requested > 0L) return requested;
  return ctx.tokoId == null ? 0L : ctx.tokoId.longValue();
 }
 private static String pengguna(EbisnisActorContextResolver.ActorContext ctx) {
  return ctx.userId == null ? "SYSTEM" : String.valueOf(ctx.userId);
 }
 private static JSONObject dokumen(ProduksiDokumen d) throws Exception {
  JSONObject j = new JSONObject();
  j.put("id", d.getId()); j.put("jenis", d.getDocumentType()); j.put("nomor", d.getDocumentNo());
  j.put("statusDokumen", d.getStatus()); j.put("referensi", nilai(d.getReferenceNo()));
  j.put("bomId", d.getBomId() == null ? JSONObject.NULL : d.getBomId());
  j.put("qtyRencana", aman(d.getPlannedQty())); j.put("qtyAktual", aman(d.getActualQty())); j.put("uom", nilai(d.getUom()));
  j.put("biayaBahan", aman(d.getMaterialCost())); j.put("biayaTenagaKerja", aman(d.getLaborCost()));
  j.put("biayaOverhead", aman(d.getOverheadCost())); j.put("totalBiaya", aman(d.getTotalCost()));
  j.put("biayaSatuan", aman(d.getUnitCost())); j.put("tanggalRencana", waktu(d.getPlannedAt()));
  j.put("tanggalAktual", waktu(d.getActualAt())); j.put("catatan", nilai(d.getNotes()));
  j.put("clientMutationId", nilai(d.getClientMutationId())); j.put("dibuatOleh", nilai(d.getCreatedBy()));
  j.put("dibuatPada", waktu(d.getCreatedAt())); j.put("diubahPada", waktu(d.getUpdatedAt()));
  return j;
 }
 private static JSONObject baris(ProduksiDokumenBaris b) throws Exception {
  JSONObject j = new JSONObject();
  j.put("id", b.getId()); j.put("nomorBaris", b.getLineNo()); j.put("tipeBaris", b.getLineType());
  j.put("itemId", b.getItemId() == null ? JSONObject.NULL : b.getItemId()); j.put("kode", nilai(b.getItemCode()));
  j.put("nama", nilai(b.getItemName())); j.put("qty", aman(b.getQty())); j.put("uom", nilai(b.getUom()));
  j.put("lot", nilai(b.getLotNo())); j.put("biayaSatuan", aman(b.getUnitCost()));
  j.put("memengaruhiStok", Boolean.TRUE.equals(b.getStockAffecting())); j.put("catatan", nilai(b.getNotes()));
  return j;
 }
 private static Object nilai(String v) { return v == null ? "" : v; }
 private static BigDecimal aman(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
 private static Object waktu(Date v) { return v == null ? JSONObject.NULL : Long.valueOf(v.getTime()); }

 public static void daftar(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
  Session s = null;
  try {
   s = HibernateUtil.getSessionFactory().openSession();
   EbisnisActorContextResolver.ActorContext ctx = aktor(s, user, request, hasil, "view"); if (ctx == null) return;
   String cari = request.optString("cari", "").trim().toLowerCase(); int limit = request.optInt("limit", 100);
   if (limit < 1) limit = 100; if (limit > 500) limit = 500;
   String hql = "from ProduksiDokumen where tokoId=:toko and documentType=:jenis";
   if (cari.length() > 0) hql += " and (lower(documentNo) like :cari or lower(coalesce(referenceNo,'')) like :cari or lower(coalesce(notes,'')) like :cari)";
   hql += " order by updatedAt desc, id desc";
   Query q = s.createQuery(hql).setLong("toko", toko(ctx, request)).setString("jenis", JENIS.get(jenis(request))).setMaxResults(limit);
   if (cari.length() > 0) q.setString("cari", "%" + cari + "%");
   List list = q.list(); JSONArray data = new JSONArray();
   for (int i = 0; i < list.size(); i++) data.put(dokumen((ProduksiDokumen) list.get(i)));
   hasil.put("status", "00"); hasil.put("data", data); hasil.put("hakAkses", hak(ctx, jenis(request)));
  } finally { HibernateUtil.closeSessionQuietly(s); }
 }

 public static void detail(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
  Session s = null;
  try {
   s = HibernateUtil.getSessionFactory().openSession();
   EbisnisActorContextResolver.ActorContext ctx = aktor(s, user, request, hasil, "view"); if (ctx == null) return;
   ProduksiDokumen d = (ProduksiDokumen) s.get(ProduksiDokumen.class, Long.valueOf(request.optLong("id")));
   if (d == null || d.getTokoId().longValue() != toko(ctx, request) || !JENIS.get(jenis(request)).equals(d.getDocumentType())) { tolak(hasil, "Dokumen produksi tidak ditemukan."); return; }
   JSONObject data = dokumen(d); JSONArray lines = new JSONArray();
   List list = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo").setLong("id", d.getId().longValue()).list();
   for (int i = 0; i < list.size(); i++) lines.put(baris((ProduksiDokumenBaris) list.get(i)));
   data.put("baris", lines); data.put("genealogi", genealogi(s, d.getId())); data.put("riwayatStatus", events(s, d.getId()));
   // Fase D pelengkap (dok. 54 "layar menyusul bila diminta"): rincian WO menyertakan
   // reservasi komponennya -- datanya sudah ditulis production_reservation sejak rilis.
   if ("WO".equals(d.getDocumentType())) {
    JSONArray res = new JSONArray();
    List daftarRes = s.createQuery("from ReservasiStokProduksi where woId=:wo order by id")
      .setLong("wo", d.getId().longValue()).list();
    for (int i = 0; i < daftarRes.size(); i++) {
     ReservasiStokProduksi r = (ReservasiStokProduksi) daftarRes.get(i);
     JSONObject rj = new JSONObject();
     rj.put("produkId", r.getProdukId()); rj.put("keterangan", nilai(r.getKeterangan()));
     rj.put("qty", aman(r.getQty())); rj.put("qtySisa", aman(r.getQtySisa()));
     rj.put("statusReservasi", r.getStatus()); res.put(rj);
    }
    data.put("reservasi", res);
   }
   hasil.put("status", "00"); hasil.put("data", data); hasil.put("hakAkses", hak(ctx, jenis(request)));
  } finally { HibernateUtil.closeSessionQuietly(s); }
 }

 public static void simpan(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
  Session s = null; Transaction tx = null;
  try {
   s = HibernateUtil.getSessionFactory().openSession();
   EbisnisActorContextResolver.ActorContext ctx = aktor(s, user, request, hasil, "create"); if (ctx == null) return;
   tx = s.beginTransaction(); long toko = toko(ctx, request); String mutation = request.optString("clientMutationId", "").trim();
   ProduksiDokumen d = null; long id = request.optLong("id", 0L);
   if (id > 0L) d = (ProduksiDokumen) s.get(ProduksiDokumen.class, Long.valueOf(id));
   if (d == null && mutation.length() > 0) d = (ProduksiDokumen) s.createQuery("from ProduksiDokumen where tokoId=:toko and clientMutationId=:mutation")
     .setLong("toko", toko).setString("mutation", mutation).setMaxResults(1).uniqueResult();
   if (d != null && !"DRAFT".equals(d.getStatus())) { tolak(hasil, "Hanya dokumen DRAFT yang dapat diedit."); rollback(tx); return; }
   boolean baru = d == null;
   if (baru) { d = new ProduksiDokumen(); d.setTokoId(Long.valueOf(toko)); d.setDocumentType(JENIS.get(jenis(request))); d.setStatus("DRAFT"); d.setCreatedBy(pengguna(ctx)); d.setCreatedAt(new Date()); }
   else if (d.getTokoId().longValue() != toko || !JENIS.get(jenis(request)).equals(d.getDocumentType())) { tolak(hasil, "Dokumen produksi tidak sesuai konteks toko."); rollback(tx); return; }
   String nomor = request.optString("nomor", "").trim(); if (nomor.length() == 0) nomor = JENIS.get(jenis(request)) + "-" + System.currentTimeMillis();
   d.setDocumentNo(nomor); d.setReferenceNo(request.optString("referensi", "").trim()); d.setBomId(nullableLong(request, "bomId"));
   d.setPlannedQty(desimal(request, "qtyRencana")); d.setActualQty(desimal(request, "qtyAktual")); d.setUom(request.optString("uom", "").trim());
   d.setMaterialCost(desimal(request, "biayaBahan")); d.setLaborCost(desimal(request, "biayaTenagaKerja")); d.setOverheadCost(desimal(request, "biayaOverhead"));
   hitungBiaya(d); d.setNotes(request.optString("catatan", "").trim()); if (mutation.length() > 0) d.setClientMutationId(mutation);
   d.setUpdatedBy(pengguna(ctx)); d.setUpdatedAt(new Date()); s.saveOrUpdate(d); s.flush();
   s.createQuery("delete from ProduksiGenealogiLot where documentId=:id").setLong("id", d.getId().longValue()).executeUpdate();
   s.createQuery("delete from ProduksiDokumenBaris where documentId=:id").setLong("id", d.getId().longValue()).executeUpdate();
   JSONArray a = request.optJSONArray("baris"); Map<Integer, Long> idBaris = new HashMap<Integer, Long>();
   if (a != null) {
    for (int i = 0; i < a.length(); i++) simpanBaris(s, d.getId(), i + 1, a.getJSONObject(i));
    s.flush();
    List barisTersimpan = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo")
      .setLong("id", d.getId().longValue()).list();
    for (int i = 0; i < barisTersimpan.size(); i++) {
     ProduksiDokumenBaris b = (ProduksiDokumenBaris) barisTersimpan.get(i);
     idBaris.put(b.getLineNo(), b.getId());
    }
   }
   JSONArray g = request.optJSONArray("genealogi");
   if (g != null) for (int i = 0; i < g.length(); i++) simpanGenealogi(s, d.getId(), g.getJSONObject(i), idBaris);
   if (baru) event(s, d.getId(), null, "DRAFT", "Dokumen dibuat", pengguna(ctx));
   tx.commit(); hasil.put("status", "00"); hasil.put("id", d.getId()); hasil.put("data", dokumen(d));
  } catch (Exception e) { rollback(tx); throw e; }
  finally { HibernateUtil.closeSessionQuietly(s); }
 }

 public static void ubahStatus(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
  Session s = null; Transaction tx = null;
  try {
   s = HibernateUtil.getSessionFactory().openSession(); String target = request.optString("statusDokumen", "").trim().toUpperCase();
   EbisnisActorContextResolver.ActorContext ctx = aktor(s, user, request, hasil, aksi(target)); if (ctx == null) return;
   tx = s.beginTransaction(); ProduksiDokumen d = (ProduksiDokumen) s.get(ProduksiDokumen.class, Long.valueOf(request.optLong("id")));
   if (d == null || d.getTokoId().longValue() != toko(ctx, request) || !JENIS.get(jenis(request)).equals(d.getDocumentType())) { tolak(hasil, "Dokumen produksi tidak ditemukan."); rollback(tx); return; }
   if (target.equals(d.getStatus())) { tx.commit(); hasil.put("status", "00"); hasil.put("idempotent", true); return; }
   if (!transisi(d.getDocumentType(), d.getStatus(), target)) { tolak(hasil, "Perubahan status " + d.getStatus() + " ke " + target + " tidak diizinkan."); rollback(tx); return; }
   String awal = d.getStatus(); d.setStatus(target); d.setUpdatedBy(pengguna(ctx)); d.setUpdatedAt(new Date());
   if ("COMPLETED".equals(target) || "POSTED".equals(target)) { d.setActualAt(new Date()); hitungBiaya(d); }
   // Fase 0 dok. 49: dokumen ISSUE/RETURN/OUTPUT/WASTE menggerakkan stok saat POSTED dan
   // dibalikkan saat REVERSED -- transaksional dan TIDAK fail-safe: dokumen yang mengaku POSTED
   // tetapi stoknya tidak bergerak adalah kebohongan data, jadi kegagalan posting membatalkan
   // transisinya. Validasi baris (produk kosong/beda toko/qty<=0) ditolak dengan pesan yang bisa
   // dibaca, bukan exception mentah.
   if (jenisStok(d.getDocumentType())) {
    try {
     if ("POSTED".equals(target)) {
      postingStok(s, d, pengguna(ctx));
      // Fase D: ISSUE ber-referensi WO memakan reservasi komponen WO itu.
      if ("ISSUE".equals(d.getDocumentType())) sesuaikanReservasiIssue(s, d, false);
      // Fase E: hasil produksi ber-produk QC dikarantina + Quality Alert terbit.
      if ("OUTPUT".equals(d.getDocumentType())) buatQcAlertJikaPerlu(s, d, pengguna(ctx), hasil);
     } else if ("REVERSED".equals(target)) {
      balikkanPostingStok(s, d, pengguna(ctx));
      if ("ISSUE".equals(d.getDocumentType())) sesuaikanReservasiIssue(s, d, true);
     }
    } catch (IllegalArgumentException salah) { tolak(hasil, salah.getMessage()); rollback(tx); return; }
   }
   // Fase D dok. 48 P4: siklus reservasi komponen WO -- RELEASED mengunci komponen BOM dan
   // memeriksa kekurangan (-> pengajuan pembelian ber-rujukan WO); CANCELLED/COMPLETED melepas.
   if ("WO".equals(d.getDocumentType())) {
    try {
     if ("RELEASED".equals(target)) reservasiSaatRilis(s, d, pengguna(ctx), hasil);
     else if ("CANCELLED".equals(target)) tutupReservasi(s, d, ReservasiStokProduksi.STATUS_BATAL);
     else if ("COMPLETED".equals(target)) tutupReservasi(s, d, ReservasiStokProduksi.STATUS_SELESAI);
    } catch (IllegalArgumentException salah) { tolak(hasil, salah.getMessage()); rollback(tx); return; }
   }
   s.update(d); event(s, d.getId(), awal, target, request.optString("catatanStatus", ""), pengguna(ctx)); tx.commit();
   hasil.put("status", "00"); hasil.put("data", dokumen(d));
  } catch (Exception e) { rollback(tx); throw e; }
  finally { HibernateUtil.closeSessionQuietly(s); }
 }

 private static void simpanBaris(Session s, Long documentId, int no, JSONObject j) {
  ProduksiDokumenBaris b = new ProduksiDokumenBaris(); b.setDocumentId(documentId); b.setLineNo(Integer.valueOf(no));
  b.setLineType(j.optString("tipeBaris", "MATERIAL").trim().toUpperCase()); b.setItemId(nullableLong(j, "itemId"));
  b.setItemCode(j.optString("kode", "").trim()); b.setItemName(j.optString("nama", "Item produksi").trim());
  b.setQty(desimal(j, "qty")); b.setUom(j.optString("uom", "").trim()); b.setLotNo(j.optString("lot", "").trim());
  b.setUnitCost(desimal(j, "biayaSatuan")); b.setStockAffecting(Boolean.valueOf(j.optBoolean("memengaruhiStok", false)));
  b.setNotes(j.optString("catatan", "").trim()); s.save(b);
 }
 private static void simpanGenealogi(Session s, Long documentId, JSONObject j, Map<Integer, Long> idBaris) {
  Long inputLineId = idBaris.get(Integer.valueOf(j.optInt("inputLineNo", 0)));
  Long outputLineId = idBaris.get(Integer.valueOf(j.optInt("outputLineNo", 0)));
  if (inputLineId == null || outputLineId == null) {
   throw new IllegalArgumentException("Genealogi lot wajib menunjuk inputLineNo dan outputLineNo yang tersedia pada dokumen.");
  }
  ProduksiGenealogiLot g = new ProduksiGenealogiLot(); g.setDocumentId(documentId);
  g.setInputLineId(inputLineId); g.setOutputLineId(outputLineId);
  g.setInputLotNo(j.optString("lotBahan", "").trim()); g.setOutputLotNo(j.optString("lotHasil", "").trim());
  g.setAllocatedQty(desimal(j, "qty")); g.setCreatedAt(new Date()); s.save(g);
 }
 private static JSONArray genealogi(Session s, Long id) throws Exception {
  JSONArray a = new JSONArray(); List list = s.createQuery("from ProduksiGenealogiLot where documentId=:id order by id").setLong("id", id.longValue()).list();
  for (int i = 0; i < list.size(); i++) { ProduksiGenealogiLot g = (ProduksiGenealogiLot) list.get(i); JSONObject j = new JSONObject();
   j.put("inputLineId", g.getInputLineId()); j.put("outputLineId", g.getOutputLineId());
   j.put("inputLineNo", nomorBaris(s, g.getInputLineId())); j.put("outputLineNo", nomorBaris(s, g.getOutputLineId())); j.put("lotBahan", nilai(g.getInputLotNo()));
   j.put("lotHasil", nilai(g.getOutputLotNo())); j.put("qty", aman(g.getAllocatedQty())); a.put(j); }
  return a;
 }
 private static Object nomorBaris(Session s, Long id) {
  if (id == null) return JSONObject.NULL;
  ProduksiDokumenBaris b = (ProduksiDokumenBaris) s.get(ProduksiDokumenBaris.class, id);
  return b == null || b.getLineNo() == null ? JSONObject.NULL : b.getLineNo();
 }
 private static JSONArray events(Session s, Long id) throws Exception {
  JSONArray a = new JSONArray(); List list = s.createQuery("from ProduksiDokumenEvent where documentId=:id order by eventAt, id").setLong("id", id.longValue()).list();
  for (int i = 0; i < list.size(); i++) { ProduksiDokumenEvent e = (ProduksiDokumenEvent) list.get(i); JSONObject j = new JSONObject();
   j.put("dari", nilai(e.getFromStatus())); j.put("ke", nilai(e.getToStatus())); j.put("catatan", nilai(e.getNotes()));
   j.put("aktor", nilai(e.getActorId())); j.put("waktu", waktu(e.getEventAt())); a.put(j); }
  return a;
 }
 private static void event(Session s, Long id, String from, String to, String notes, String actor) {
  ProduksiDokumenEvent e = new ProduksiDokumenEvent(); e.setDocumentId(id); e.setFromStatus(from); e.setToStatus(to);
  e.setNotes(notes); e.setActorId(actor); e.setEventAt(new Date()); s.save(e);
 }
 private static void hitungBiaya(ProduksiDokumen d) {
  BigDecimal total = aman(d.getMaterialCost()).add(aman(d.getLaborCost())).add(aman(d.getOverheadCost())); d.setTotalCost(total);
  BigDecimal qty = aman(d.getActualQty()).compareTo(BigDecimal.ZERO) > 0 ? aman(d.getActualQty()) : aman(d.getPlannedQty());
  d.setUnitCost(qty.compareTo(BigDecimal.ZERO) > 0 ? total.divide(qty, 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
 }
 private static String aksi(String status) {
  if ("ACTIVE".equals(status) || "RELEASED".equals(status) || "POSTED".equals(status)) return "approve";
  if ("COMPLETED".equals(status)) return "complete"; if ("CANCELLED".equals(status)) return "cancel";
  if ("REVERSED".equals(status) || "RETIRED".equals(status)) return "reverse"; return "update";
 }
 private static boolean transisi(String type, String from, String to) {
  if ("BOM".equals(type)) return ("DRAFT".equals(from) && ("ACTIVE".equals(to) || "CANCELLED".equals(to))) || ("ACTIVE".equals(from) && "RETIRED".equals(to));
  if ("WO".equals(type)) return ("DRAFT".equals(from) && ("RELEASED".equals(to) || "CANCELLED".equals(to))) ||
    ("RELEASED".equals(from) && ("IN_PROGRESS".equals(to) || "CANCELLED".equals(to))) ||
    ("IN_PROGRESS".equals(from) && ("COMPLETED".equals(to) || "CANCELLED".equals(to)));
  return ("DRAFT".equals(from) && ("POSTED".equals(to) || "CANCELLED".equals(to))) || ("POSTED".equals(from) && "REVERSED".equals(to));
 }
 private static JSONObject hak(EbisnisActorContextResolver.ActorContext ctx, String jenis) throws Exception {
  JSONObject h = new JSONObject(); h.put("buat", ctx.bolehAksi("produksi_" + jenis, "create")); h.put("ubah", ctx.bolehAksi("produksi_" + jenis, "update"));
  h.put("setujui", ctx.bolehAksi("produksi_" + jenis, "approve")); h.put("batalkan", ctx.bolehAksi("produksi_" + jenis, "cancel")); return h;
 }
 private static void rollback(Transaction tx) { if (tx != null) try { tx.rollback(); } catch (Exception ignored) { } }

 /** Jenis dokumen yang menggerakkan stok. BOM/WO/COST sengaja TIDAK (dok. 49 Adendum). */
 private static boolean jenisStok(String type) {
  return "ISSUE".equals(type) || "RETURN".equals(type) || "OUTPUT".equals(type) || "WASTE".equals(type)
    || "UNBUILD".equals(type);
 }
 /** OUTPUT/RETURN menambah stok; ISSUE/WASTE mengurangi. */
 private static boolean arahMasuk(String type) { return "OUTPUT".equals(type) || "RETURN".equals(type); }
 /** UNBUILD (Fase D) membalik OUTPUT+ISSUE dalam SATU dokumen sehingga arahnya per-BARIS:
  * baris bertipe OUTPUT (barang jadi) KELUAR, baris lain (komponen BOM) MASUK. Jenis dokumen
  * lain tetap arah per-dokumen seperti Fase 0. */
 private static boolean arahMasukBaris(String type, String lineType) {
  if ("UNBUILD".equals(type))
   return !"OUTPUT".equals(lineType == null ? "" : lineType.trim().toUpperCase());
  return arahMasuk(type);
 }

 /**
  * Menulis ledger {@link MutasiStokProduksi} arah FORWARD untuk tiap baris ber-{@code stockAffecting}
  * saat dokumen POSTED, lalu menghitung ulang stok produknya. Idempoten periksa-lalu-lewati per
  * (dokumen, baris, arah) -- pola {@code DistribusiPengirimanApiHelper.postingStok}.
  */
 /**
  * Mesin BERSAMA draf Work Order otomatis (Fase C penjadwal ambang; Fase E MTO dan disposisi
  * REWORK QC) -- SATU mesin, bukan salinan. Idempoten lewat {@code referenceNo}: selama masih
  * ada WO ber-kunci sama berstatus DRAFT/RELEASED/IN_PROGRESS, tidak dibuat dobel (kembali
  * {@code null}). BOM ACTIVE yang baris OUTPUT-nya = produk ikut dirujuk; tanpa BOM, WO tetap
  * terbit dengan catatan jujur supaya staf tahu harus membuat BOM dulu.
  */
 public static Long buatWoDrafOtomatis(Session s, long tokoId, Long produkId, BigDecimal qty,
   String kunci, String catatan) {
  Number sudahAda = (Number) s.createQuery(
    "select count(*) from ProduksiDokumen where documentType='WO' and tokoId=:toko"
      + " and referenceNo=:kunci and status in ('DRAFT','RELEASED','IN_PROGRESS')")
    .setLong("toko", tokoId).setString("kunci", kunci).uniqueResult();
  if (sudahAda != null && sudahAda.longValue() > 0) return null;
  Long bomId = null;
  try {
   List bomIds = s.createQuery("select d.id from ProduksiDokumen d, ProduksiDokumenBaris b"
     + " where b.documentId=d.id and d.documentType='BOM' and d.status='ACTIVE'"
     + " and d.tokoId=:toko and b.lineType='OUTPUT' and b.itemId=:produk"
     + " order by d.updatedAt desc, d.id desc")
     .setLong("toko", tokoId).setLong("produk", produkId.longValue()).setMaxResults(1).list();
   if (!bomIds.isEmpty()) bomId = (Long) bomIds.get(0);
  } catch (Exception eBom) {
   ais.common.ErrorAuditUtil.record(eBom, "auto-audit src/ais/action/servlet/api/ProduksiApiHelper.java:cariBom");
  }
  Object namaSatuan = s.createSQLQuery("SELECT sp.nama FROM koperasi.produk p"
    + " LEFT JOIN koperasi.satuan_produk sp ON sp.id = p.satuan WHERE p.id = :p")
    .setLong("p", produkId.longValue()).uniqueResult();
  ProduksiDokumen wo = new ProduksiDokumen();
  wo.setTokoId(Long.valueOf(tokoId)); wo.setDocumentType("WO");
  wo.setDocumentNo("WO-AUTO-" + System.currentTimeMillis() + "-" + produkId);
  wo.setStatus("DRAFT"); wo.setReferenceNo(kunci); wo.setBomId(bomId);
  wo.setPlannedQty(qty); wo.setUom(namaSatuan == null ? null : String.valueOf(namaSatuan));
  wo.setPlannedAt(new Date()); wo.setCreatedBy("SYSTEM");
  wo.setNotes(catatan + (bomId == null
    ? " BELUM ADA BOM AKTIF utk produk ini -- buat/aktifkan BOM lalu lengkapi WO." : ""));
  s.save(wo);
  return wo.getId();
 }

 /**
  * Fase E dok. 48 P6: OUTPUT POSTED yang memuat produk ber-{@code perlu_qc} menerbitkan SATU
  * dokumen Quality Alert (jenis QC, ringan, menumpang infra dokumen/baris/event) dan
  * MENGKARANTINA batch ber-lot sama ({@code ProdukBatch.STATUS_KARANTINA} +
  * {@code KantinHelper.catatMutasiBatch} yang sudah ada -- koreksi dok. 49 §1.2: karantina
  * tidak dibangun dari nol). Idempoten per dokumen OUTPUT lewat referenceNo. Baris tanpa
  * batch/lot dicatat jujur di notes -- QC tetap terbit.
  */
 private static void buatQcAlertJikaPerlu(Session s, ProduksiDokumen d, String oleh, JSONObject hasil)
   throws Exception {
  Object sudah = s.createQuery("select id from ProduksiDokumen where documentType='QC'"
    + " and tokoId=:toko and referenceNo=:no")
    .setLong("toko", d.getTokoId().longValue()).setString("no", d.getDocumentNo())
    .setMaxResults(1).uniqueResult();
  if (sudah != null) return;
  List lines = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo")
    .setLong("id", d.getId().longValue()).list();
  ProduksiDokumen qc = null; int nomorBaris = 0; StringBuilder catatan = new StringBuilder();
  for (int i = 0; i < lines.size(); i++) {
   ProduksiDokumenBaris b = (ProduksiDokumenBaris) lines.get(i);
   if (!Boolean.TRUE.equals(b.getStockAffecting()) || b.getItemId() == null) continue;
   Object perluQc = s.createSQLQuery("SELECT COALESCE(perlu_qc, false) FROM koperasi.produk WHERE id=:p")
     .setLong("p", b.getItemId().longValue()).uniqueResult();
   if (!Boolean.TRUE.equals(perluQc)) continue;
   if (qc == null) {
    qc = new ProduksiDokumen();
    qc.setTokoId(d.getTokoId()); qc.setDocumentType("QC");
    qc.setDocumentNo("QC-" + d.getDocumentNo());
    qc.setStatus("DRAFT"); qc.setReferenceNo(d.getDocumentNo());
    qc.setCreatedBy("SYSTEM");
    s.save(qc); s.flush();
    event(s, qc.getId(), null, "DRAFT", "Quality Alert otomatis dari OUTPUT " + d.getDocumentNo(), oleh);
   }
   nomorBaris++;
   ProduksiDokumenBaris qb = new ProduksiDokumenBaris();
   qb.setDocumentId(qc.getId()); qb.setLineNo(Integer.valueOf(nomorBaris)); qb.setLineType("QC");
   qb.setItemId(b.getItemId()); qb.setItemCode(b.getItemCode()); qb.setItemName(b.getItemName());
   qb.setQty(aman(b.getQty())); qb.setUom(b.getUom()); qb.setLotNo(b.getLotNo());
   qb.setStockAffecting(Boolean.FALSE); s.save(qb);
   String lot = b.getLotNo() == null ? "" : b.getLotNo().trim();
   boolean terkarantina = false;
   if (lot.length() > 0) {
    ais.database.model.inventory.ProdukBatch batch = (ais.database.model.inventory.ProdukBatch)
      s.createQuery("from ProdukBatch where produk.id=:p and toko.id=:t and nomorBatch=:lot")
      .setLong("p", b.getItemId().longValue()).setLong("t", d.getTokoId().longValue())
      .setString("lot", lot).setMaxResults(1).uniqueResult();
    if (batch != null) {
     batch.setStatus(ais.database.model.inventory.ProdukBatch.STATUS_KARANTINA);
     s.update(batch);
     KantinHelper.catatMutasiBatch(s, batch, "QC_KARANTINA", 0, 0, qc.getDocumentNo(),
       "Karantina otomatis QC hasil produksi " + d.getDocumentNo(), oleh);
     terkarantina = true;
    }
   }
   if (!terkarantina) {
    catatan.append(" Baris ").append(b.getItemName())
      .append(lot.length() == 0 ? " tanpa lot" : " lot " + lot + " tanpa ProdukBatch")
      .append(" -- karantina fisik manual.");
   }
  }
  if (qc != null) {
   qc.setNotes("QC hasil produksi " + d.getDocumentNo() + "." + catatan
     + " Disposisi lewat aksi produksi_qc_disposisi (REWORK/UNBUILD/SCRAP/RELEASE).");
   s.update(qc);
   hasil.put("qcAlertId", qc.getId()); hasil.put("qcAlertNomor", qc.getDocumentNo());
  }
 }

 /**
  * Fase E: disposisi Quality Alert -- SEMUA turunannya memakai mesin fase sebelumnya:
  * REWORK = draf WO ({@link #buatWoDrafOtomatis}); UNBUILD = dokumen UNBUILD DRAFT Fase D
  * (baris OUTPUT + komponen BOM ter-skala, staf meninjau lalu memposting); SCRAP = dokumen
  * WASTE DRAFT Fase 0; RELEASE = lolos tanpa turunan. Batch: REWORK/RELEASE mengangkat
  * karantina (AKTIF); UNBUILD/SCRAP membiarkan KARANTINA -- barangnya memang keluar lewat
  * dokumen turunan. QC lalu POSTED dengan catatan disposisi. Jurnal per disposisi menyusul
  * dari dokumen turunan lewat dasbor Draft Jurnal (butuh pemetaan akun pemilik, dok. 55).
  */
 public static void qcDisposisi(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
  Session s = null; Transaction tx = null;
  try {
   s = HibernateUtil.getSessionFactory().openSession();
   EbisnisActorContextResolver.ActorContext ctx = aktor(s, user, request, hasil, "approve");
   if (ctx == null) return;
   String disposisi = request.optString("disposisi", "").trim().toUpperCase();
   if (!"REWORK".equals(disposisi) && !"UNBUILD".equals(disposisi) && !"SCRAP".equals(disposisi)
     && !"RELEASE".equals(disposisi)) {
    tolak(hasil, "Disposisi tidak dikenal: pilih REWORK, UNBUILD, SCRAP, atau RELEASE."); return;
   }
   tx = s.beginTransaction();
   ProduksiDokumen qc = (ProduksiDokumen) s.get(ProduksiDokumen.class, Long.valueOf(request.optLong("id")));
   if (qc == null || !"QC".equals(qc.getDocumentType()) || qc.getTokoId().longValue() != toko(ctx, request)) {
    tolak(hasil, "Dokumen Quality Alert tidak ditemukan."); rollback(tx); return;
   }
   if (!"DRAFT".equals(qc.getStatus())) {
    tolak(hasil, "Quality Alert sudah didisposisi (status " + qc.getStatus() + ")."); rollback(tx); return;
   }
   JSONArray turunan = terapkanDisposisiQc(s, qc, disposisi, pengguna(ctx));
   tx.commit();
   hasil.put("status", "00"); hasil.put("disposisi", disposisi); hasil.put("turunan", turunan);
  } catch (Exception e) { rollback(tx); throw e; }
  finally { HibernateUtil.closeSessionQuietly(s); }
 }

 /** Inti disposisi QC -- dipisah dari endpoint supaya teruji langsung (pola dok. 44). */
 static JSONArray terapkanDisposisiQc(Session s, ProduksiDokumen qc, String disposisi, String oleh)
   throws Exception {
  List lines = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo")
     .setLong("id", qc.getId().longValue()).list();
   JSONArray turunan = new JSONArray();
   for (int i = 0; i < lines.size(); i++) {
    ProduksiDokumenBaris b = (ProduksiDokumenBaris) lines.get(i);
    if (b.getItemId() == null) continue;
    if ("REWORK".equals(disposisi)) {
     Long woBaru = buatWoDrafOtomatis(s, qc.getTokoId().longValue(), b.getItemId(), aman(b.getQty()),
       "QC:" + qc.getId() + ":" + b.getItemId(),
       "Rework dari " + qc.getDocumentNo() + " (" + b.getItemName() + ").");
     if (woBaru != null) { JSONObject t = new JSONObject(); t.put("jenis", "WO"); t.put("id", woBaru); turunan.put(t); }
    } else if ("UNBUILD".equals(disposisi) || "SCRAP".equals(disposisi)) {
     String tipe = "UNBUILD".equals(disposisi) ? "UNBUILD" : "WASTE";
     ProduksiDokumen anak = new ProduksiDokumen();
     anak.setTokoId(qc.getTokoId()); anak.setDocumentType(tipe);
     anak.setDocumentNo(tipe + "-" + qc.getDocumentNo() + "-" + b.getLineNo());
     anak.setStatus("DRAFT"); anak.setReferenceNo("QC:" + qc.getId());
     anak.setPlannedQty(aman(b.getQty())); anak.setUom(b.getUom()); anak.setCreatedBy("SYSTEM");
     anak.setNotes(disposisi + " dari " + qc.getDocumentNo() + " (" + b.getItemName()
       + "). Tinjau baris lalu POSTED utk menggerakkan stok.");
     s.save(anak); s.flush();
     ProduksiDokumenBaris keluar = new ProduksiDokumenBaris();
     keluar.setDocumentId(anak.getId()); keluar.setLineNo(Integer.valueOf(1));
     keluar.setLineType("UNBUILD".equals(disposisi) ? "OUTPUT" : "MATERIAL");
     keluar.setItemId(b.getItemId()); keluar.setItemCode(b.getItemCode());
     keluar.setItemName(b.getItemName()); keluar.setQty(aman(b.getQty()));
     keluar.setUom(b.getUom()); keluar.setLotNo(b.getLotNo());
     keluar.setStockAffecting(Boolean.TRUE); s.save(keluar);
     if ("UNBUILD".equals(disposisi)) {
      // Komponen BOM ter-skala ikut diprefill supaya staf tinggal meninjau (mesin Fase D).
      isiKomponenUnbuildDariBom(s, anak, b.getItemId(), aman(b.getQty()));
     }
     event(s, anak.getId(), null, "DRAFT", disposisi + " dari " + qc.getDocumentNo(), oleh);
     JSONObject t = new JSONObject(); t.put("jenis", tipe); t.put("id", anak.getId()); turunan.put(t);
    }
    // Batch: REWORK/RELEASE angkat karantina; UNBUILD/SCRAP biarkan (keluar lewat turunan).
    if ("REWORK".equals(disposisi) || "RELEASE".equals(disposisi)) {
     String lot = b.getLotNo() == null ? "" : b.getLotNo().trim();
     if (lot.length() > 0) {
      ais.database.model.inventory.ProdukBatch batch = (ais.database.model.inventory.ProdukBatch)
        s.createQuery("from ProdukBatch where produk.id=:p and toko.id=:t and nomorBatch=:lot")
        .setLong("p", b.getItemId().longValue()).setLong("t", qc.getTokoId().longValue())
        .setString("lot", lot).setMaxResults(1).uniqueResult();
      if (batch != null && ais.database.model.inventory.ProdukBatch.STATUS_KARANTINA.equals(batch.getStatus())) {
       batch.setStatus(ais.database.model.inventory.ProdukBatch.STATUS_AKTIF);
       s.update(batch);
       KantinHelper.catatMutasiBatch(s, batch, "QC_LEPAS", 0, 0, qc.getDocumentNo(),
         "Karantina diangkat: disposisi " + disposisi, oleh);
      }
     }
    }
   }
   String awal = qc.getStatus(); qc.setStatus("POSTED");
   qc.setNotes((qc.getNotes() == null ? "" : qc.getNotes()) + " [Disposisi: " + disposisi + "]");
   qc.setUpdatedBy(oleh); qc.setUpdatedAt(new Date()); s.update(qc);
   event(s, qc.getId(), awal, "POSTED", "Disposisi " + disposisi, oleh);
   return turunan;
 }

 /** Prefill komponen UNBUILD dari BOM ACTIVE produk, ter-skala qty/qtyOutputBom (mesin Fase D). */
 private static void isiKomponenUnbuildDariBom(Session s, ProduksiDokumen anak, Long produkId,
   BigDecimal qty) {
  List bomIds = s.createQuery("select d.id from ProduksiDokumen d, ProduksiDokumenBaris b"
    + " where b.documentId=d.id and d.documentType='BOM' and d.status='ACTIVE'"
    + " and d.tokoId=:toko and b.lineType='OUTPUT' and b.itemId=:produk"
    + " order by d.updatedAt desc, d.id desc")
    .setLong("toko", anak.getTokoId().longValue()).setLong("produk", produkId.longValue())
    .setMaxResults(1).list();
  if (bomIds.isEmpty()) return;
  List barisBom = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo")
    .setLong("id", ((Long) bomIds.get(0)).longValue()).list();
  BigDecimal qtyOutputBom = BigDecimal.ZERO;
  for (int i = 0; i < barisBom.size(); i++) {
   ProduksiDokumenBaris b = (ProduksiDokumenBaris) barisBom.get(i);
   if ("OUTPUT".equals(b.getLineType())) qtyOutputBom = qtyOutputBom.add(aman(b.getQty()));
  }
  BigDecimal rasio = qtyOutputBom.compareTo(BigDecimal.ZERO) > 0
    ? qty.divide(qtyOutputBom, 6, BigDecimal.ROUND_HALF_UP) : qty;
  int no = 1;
  for (int i = 0; i < barisBom.size(); i++) {
   ProduksiDokumenBaris b = (ProduksiDokumenBaris) barisBom.get(i);
   if ("OUTPUT".equals(b.getLineType()) || b.getItemId() == null) continue;
   no++;
   ProduksiDokumenBaris k = new ProduksiDokumenBaris();
   k.setDocumentId(anak.getId()); k.setLineNo(Integer.valueOf(no)); k.setLineType("MATERIAL");
   k.setItemId(b.getItemId()); k.setItemCode(b.getItemCode()); k.setItemName(b.getItemName());
   k.setQty(aman(b.getQty()).multiply(rasio)); k.setUom(b.getUom());
   k.setStockAffecting(Boolean.TRUE); s.save(k);
  }
 }

 /**
  * Fase D: WO RELEASED mengunci komponen BOM sebagai {@link ReservasiStokProduksi} (kebutuhan =
  * qty baris BOM x rasio plannedQty WO terhadap qty baris OUTPUT BOM), lalu memeriksa kekurangan
  * terhadap stok toko dikurangi reservasi AKTIF WO lain. Kekurangan -> PengajuanPembelianGudang
  * ber-rujukan WO bila toko punya gudangPemasok; tanpa gudang, kekurangan tetap dilaporkan di
  * respons ({@code kekurangan}) dan catatan dokumen -- tidak diam-diam hilang. Reservasi murni
  * INFORMASI bagi kasir (keputusan dok. 48 §6 no. 4 terbuka).
  */
 private static void reservasiSaatRilis(Session s, ProduksiDokumen d, String oleh, JSONObject hasil)
   throws Exception {
  if (d.getBomId() == null) return; // WO manual tanpa BOM: tak ada daftar komponen utk dikunci.
  Object sudah = s.createQuery("select id from ReservasiStokProduksi where woId=:wo")
    .setLong("wo", d.getId().longValue()).setMaxResults(1).uniqueResult();
  if (sudah != null) return; // idempoten -- rilis ulang tidak menggandakan kunci.
  List barisBom = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo")
    .setLong("id", d.getBomId().longValue()).list();
  BigDecimal qtyOutputBom = BigDecimal.ZERO;
  for (int i = 0; i < barisBom.size(); i++) {
   ProduksiDokumenBaris b = (ProduksiDokumenBaris) barisBom.get(i);
   if ("OUTPUT".equals(b.getLineType())) qtyOutputBom = qtyOutputBom.add(aman(b.getQty()));
  }
  BigDecimal planned = aman(d.getPlannedQty());
  BigDecimal rasio = qtyOutputBom.compareTo(BigDecimal.ZERO) > 0
    ? planned.divide(qtyOutputBom, 6, BigDecimal.ROUND_HALF_UP) : planned;
  JSONArray kekuranganSemua = new JSONArray(); StringBuilder catatan = new StringBuilder();
  for (int i = 0; i < barisBom.size(); i++) {
   ProduksiDokumenBaris b = (ProduksiDokumenBaris) barisBom.get(i);
   if ("OUTPUT".equals(b.getLineType()) || b.getItemId() == null) continue;
   BigDecimal butuh = aman(b.getQty()).multiply(rasio);
   if (butuh.compareTo(BigDecimal.ZERO) <= 0) continue;
   ReservasiStokProduksi r = new ReservasiStokProduksi();
   r.setWoId(d.getId()); r.setTokoId(d.getTokoId()); r.setProdukId(b.getItemId());
   r.setQty(butuh); r.setQtySisa(butuh); r.setStatus(ReservasiStokProduksi.STATUS_AKTIF);
   r.setKeterangan("WO " + d.getDocumentNo() + " komponen " + b.getItemName());
   r.setOleh(oleh); r.setDibuat(new Date()); r.setDiubah(new Date());
   s.save(r);
   // Kekurangan: stok toko - reservasi AKTIF milik WO LAIN (reservasi WO ini baru dibuat).
   Object stokProduk = s.createSQLQuery("SELECT COALESCE(stok,0) FROM koperasi.produk WHERE id=:p")
     .setLong("p", b.getItemId().longValue()).uniqueResult();
   Object reservedLain = s.createQuery(
     "select sum(qtySisa) from ReservasiStokProduksi where produkId=:p and tokoId=:t"
       + " and status=:st and woId<>:wo")
     .setLong("p", b.getItemId().longValue()).setLong("t", d.getTokoId().longValue())
     .setString("st", ReservasiStokProduksi.STATUS_AKTIF).setLong("wo", d.getId().longValue())
     .uniqueResult();
   BigDecimal tersedia = new BigDecimal(String.valueOf(stokProduk == null ? 0 : stokProduk))
     .subtract(reservedLain == null ? BigDecimal.ZERO : (BigDecimal) reservedLain);
   if (tersedia.compareTo(BigDecimal.ZERO) < 0) tersedia = BigDecimal.ZERO;
   BigDecimal kurang = butuh.subtract(tersedia);
   if (kurang.compareTo(BigDecimal.ZERO) <= 0) continue;
   JSONObject k = new JSONObject(); k.put("produkId", b.getItemId()); k.put("nama", b.getItemName());
   k.put("butuh", butuh); k.put("tersedia", tersedia); k.put("kurang", kurang);
   Toko toko = (Toko) s.get(Toko.class, d.getTokoId());
   if (toko != null && toko.getGudangPemasok() != null) {
    // Idempoten mesin lama: satu pengajuan aktif per (produk, WO).
    Object adaPengajuan = s.createQuery(
      "select id from PengajuanPembelianGudang where woId=:wo and produk.id=:p"
        + " and status in ('BARU','DIPROSES')")
      .setLong("wo", d.getId().longValue()).setLong("p", b.getItemId().longValue())
      .setMaxResults(1).uniqueResult();
    if (adaPengajuan == null) {
     PengajuanPembelianGudang pengajuan = new PengajuanPembelianGudang();
     pengajuan.setProduk((ais.database.model.inventory.Produk) s.get(
       ais.database.model.inventory.Produk.class, b.getItemId()));
     pengajuan.setGudangAsal(toko.getGudangPemasok());
     pengajuan.setGudangTujuan(toko.getGudangPemasok().getGudangInduk());
     pengajuan.setStokSaatDiajukan(Double.valueOf(tersedia.doubleValue()));
     pengajuan.setQtyDiminta(Double.valueOf(kurang.doubleValue()));
     pengajuan.setStatus(PengajuanPembelianGudang.STATUS_BARU);
     pengajuan.setOtomatis(Boolean.TRUE); pengajuan.setWoId(d.getId());
     pengajuan.setWaktuDibuat(ais.ui.util.WaktuUtil.getDate());
     pengajuan.setKeterangan("Kekurangan komponen WO " + d.getDocumentNo() + ": butuh " + butuh
       + " " + b.getItemName() + ", tersedia " + tersedia + " (di luar reservasi WO lain).");
     s.save(pengajuan); k.put("pengajuan", true);
    } else { k.put("pengajuan", true); }
   } else {
    k.put("pengajuan", false);
    catatan.append(" Kekurangan ").append(b.getItemName()).append(" ").append(kurang)
      .append(" TIDAK dibuatkan pengajuan: toko belum punya Gudang Pemasok.");
   }
   kekuranganSemua.put(k);
  }
  if (kekuranganSemua.length() > 0) {
   hasil.put("kekurangan", kekuranganSemua);
   d.setNotes((d.getNotes() == null ? "" : d.getNotes()) + " [Rilis: " + kekuranganSemua.length()
     + " komponen kurang." + catatan + "]");
  }
 }

 /** Fase D: tutup semua reservasi AKTIF milik WO (BATAL saat cancel, SELESAI saat complete). */
 private static void tutupReservasi(Session s, ProduksiDokumen d, String status) {
  s.createQuery("update ReservasiStokProduksi set status=:st, diubah=:kini"
    + " where woId=:wo and status=:aktif")
    .setString("st", status).setTimestamp("kini", new Date())
    .setLong("wo", d.getId().longValue())
    .setString("aktif", ReservasiStokProduksi.STATUS_AKTIF).executeUpdate();
 }

 /**
  * Fase D: ISSUE POSTED ber-referensi WO ({@code referenceNo} = documentNo WO satu toko)
  * mengurangi {@code qtySisa} reservasi komponen WO itu (0 = SELESAI); REVERSED memulihkan
  * (dibatasi qty awal). ISSUE tanpa referensi WO tidak menyentuh reservasi mana pun.
  */
 private static void sesuaikanReservasiIssue(Session s, ProduksiDokumen d, boolean pulihkan) {
  String ref = d.getReferenceNo() == null ? "" : d.getReferenceNo().trim();
  if (ref.length() == 0) return;
  Object woId = s.createQuery("select id from ProduksiDokumen where documentType='WO'"
    + " and tokoId=:t and documentNo=:no")
    .setLong("t", d.getTokoId().longValue()).setString("no", ref).setMaxResults(1).uniqueResult();
  if (woId == null) return;
  List baris = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo")
    .setLong("id", d.getId().longValue()).list();
  for (int i = 0; i < baris.size(); i++) {
   ProduksiDokumenBaris b = (ProduksiDokumenBaris) baris.get(i);
   if (!Boolean.TRUE.equals(b.getStockAffecting()) || b.getItemId() == null) continue;
   ReservasiStokProduksi r = (ReservasiStokProduksi) s.createQuery(
     "from ReservasiStokProduksi where woId=:wo and produkId=:p order by id")
     .setLong("wo", ((Number) woId).longValue()).setLong("p", b.getItemId().longValue())
     .setMaxResults(1).uniqueResult();
   if (r == null) continue;
   BigDecimal qty = aman(b.getQty());
   BigDecimal sisa = pulihkan ? r.getQtySisa().add(qty) : r.getQtySisa().subtract(qty);
   if (sisa.compareTo(BigDecimal.ZERO) < 0) sisa = BigDecimal.ZERO;
   if (sisa.compareTo(r.getQty()) > 0) sisa = r.getQty();
   r.setQtySisa(sisa);
   if (ReservasiStokProduksi.STATUS_AKTIF.equals(r.getStatus())
     || ReservasiStokProduksi.STATUS_SELESAI.equals(r.getStatus())) {
    r.setStatus(sisa.compareTo(BigDecimal.ZERO) > 0
      ? ReservasiStokProduksi.STATUS_AKTIF : ReservasiStokProduksi.STATUS_SELESAI);
   }
   r.setDiubah(new Date()); s.update(r);
  }
 }

 private static void postingStok(Session s, ProduksiDokumen d, String oleh) throws Exception {
  List lines = s.createQuery("from ProduksiDokumenBaris where documentId=:id order by lineNo")
    .setLong("id", d.getId().longValue()).list();
  java.util.Set<Long> tersentuh = new java.util.HashSet<Long>();
  for (int i = 0; i < lines.size(); i++) {
   ProduksiDokumenBaris b = (ProduksiDokumenBaris) lines.get(i);
   if (!Boolean.TRUE.equals(b.getStockAffecting())) continue;
   BigDecimal qty = aman(b.getQty());
   if (qty.compareTo(BigDecimal.ZERO) <= 0)
    throw new IllegalArgumentException("Baris " + b.getLineNo() + " (" + b.getItemName()
      + "): qty wajib lebih dari nol untuk baris yang memengaruhi stok.");
   if (b.getItemId() == null)
    throw new IllegalArgumentException("Baris " + b.getLineNo() + " (" + b.getItemName()
      + "): baris yang memengaruhi stok wajib menunjuk produk katalog (itemId).");
   Object tokoProduk = s.createSQLQuery("SELECT toko FROM koperasi.produk WHERE id = :p")
     .setLong("p", b.getItemId().longValue()).uniqueResult();
   if (tokoProduk == null)
    throw new IllegalArgumentException("Baris " + b.getLineNo() + " (" + b.getItemName()
      + "): produk katalog id " + b.getItemId() + " tidak ditemukan.");
   if (((Number) tokoProduk).longValue() != d.getTokoId().longValue())
    throw new IllegalArgumentException("Baris " + b.getLineNo() + " (" + b.getItemName()
      + "): produk milik toko lain -- dokumen produksi dan produknya wajib satu toko.");
   if (sudahDiposting(s, d.getId(), b.getId(), MutasiStokProduksi.ARAH_FORWARD)) { tersentuh.add(b.getItemId()); continue; }
   MutasiStokProduksi m = new MutasiStokProduksi();
   m.setDokumenId(d.getId()); m.setBarisId(b.getId()); m.setArah(MutasiStokProduksi.ARAH_FORWARD);
   m.setJenis(d.getDocumentType()); m.setToko(d.getTokoId()); m.setProduk(b.getItemId());
   boolean masuk = arahMasukBaris(d.getDocumentType(), b.getLineType());
   m.setQtyMasuk(masuk ? qty : BigDecimal.ZERO);
   m.setQtyKeluar(masuk ? BigDecimal.ZERO : qty);
   m.setKunciIdempoten(kunciPosting(d, b.getId(), MutasiStokProduksi.ARAH_FORWARD));
   m.setKeterangan(d.getDocumentType() + " " + d.getDocumentNo()); m.setOleh(oleh); m.setWaktu(new Date());
   s.save(m); tersentuh.add(b.getItemId());
  }
  s.flush();
  for (java.util.Iterator<Long> it = tersentuh.iterator(); it.hasNext();)
   ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(s, it.next());
 }

 /**
  * Menulis KONTRA-BARIS (arah REVERSE, kolom masuk/keluar ditukar) untuk tiap baris FORWARD milik
  * dokumen saat REVERSED. Ledger tidak pernah dihapus (ADR: koreksi lewat movement lawan) --
  * koreksi atas desain awal dok. 49 yang menghapus baris, lihat Adendum 29-08-2026.
  */
 private static void balikkanPostingStok(Session s, ProduksiDokumen d, String oleh) throws Exception {
  List maju = s.createQuery("from MutasiStokProduksi where dokumenId=:id and arah=:arah")
    .setLong("id", d.getId().longValue()).setString("arah", MutasiStokProduksi.ARAH_FORWARD).list();
  java.util.Set<Long> tersentuh = new java.util.HashSet<Long>();
  for (int i = 0; i < maju.size(); i++) {
   MutasiStokProduksi f = (MutasiStokProduksi) maju.get(i);
   tersentuh.add(f.getProduk());
   if (sudahDiposting(s, d.getId(), f.getBarisId(), MutasiStokProduksi.ARAH_REVERSE)) continue;
   MutasiStokProduksi m = new MutasiStokProduksi();
   m.setDokumenId(f.getDokumenId()); m.setBarisId(f.getBarisId()); m.setArah(MutasiStokProduksi.ARAH_REVERSE);
   m.setJenis(f.getJenis()); m.setToko(f.getToko()); m.setProduk(f.getProduk());
   m.setQtyMasuk(f.getQtyKeluar()); m.setQtyKeluar(f.getQtyMasuk());
   m.setKunciIdempoten(kunciPosting(d, f.getBarisId(), MutasiStokProduksi.ARAH_REVERSE));
   m.setKeterangan("Pembalikan " + f.getJenis() + " " + d.getDocumentNo()); m.setOleh(oleh); m.setWaktu(new Date());
   s.save(m);
  }
  s.flush();
  for (java.util.Iterator<Long> it = tersentuh.iterator(); it.hasNext();)
   ais.action.master.inventory.StokKantinUtil.recomputeStokProdukNative(s, it.next());
 }

 private static boolean sudahDiposting(Session s, Long dokumenId, Long barisId, String arah) {
  Object ada = s.createQuery("select id from MutasiStokProduksi where dokumenId=:d and barisId=:b and arah=:a")
    .setLong("d", dokumenId.longValue()).setLong("b", barisId.longValue()).setString("a", arah)
    .setMaxResults(1).uniqueResult();
  return ada != null;
 }

 /** Kunci idempoten format fondasi Fase 9: PRODUCTION:&lt;dokumen&gt;:&lt;jenis&gt;:&lt;baris&gt;:&lt;arah&gt;. */
 private static String kunciPosting(ProduksiDokumen d, Long barisId, String arah) {
  return "PRODUCTION:" + d.getId() + ":" + d.getDocumentType() + ":" + barisId + ":" + arah;
 }
}
