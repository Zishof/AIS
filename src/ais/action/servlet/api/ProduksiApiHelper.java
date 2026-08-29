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
  return ctx;
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
     if ("POSTED".equals(target)) postingStok(s, d, pengguna(ctx));
     else if ("REVERSED".equals(target)) balikkanPostingStok(s, d, pengguna(ctx));
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
  return "ISSUE".equals(type) || "RETURN".equals(type) || "OUTPUT".equals(type) || "WASTE".equals(type);
 }
 /** OUTPUT/RETURN menambah stok; ISSUE/WASTE mengurangi. */
 private static boolean arahMasuk(String type) { return "OUTPUT".equals(type) || "RETURN".equals(type); }

 /**
  * Menulis ledger {@link MutasiStokProduksi} arah FORWARD untuk tiap baris ber-{@code stockAffecting}
  * saat dokumen POSTED, lalu menghitung ulang stok produknya. Idempoten periksa-lalu-lewati per
  * (dokumen, baris, arah) -- pola {@code DistribusiPengirimanApiHelper.postingStok}.
  */
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
   m.setQtyMasuk(arahMasuk(d.getDocumentType()) ? qty : BigDecimal.ZERO);
   m.setQtyKeluar(arahMasuk(d.getDocumentType()) ? BigDecimal.ZERO : qty);
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
