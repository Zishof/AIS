package ais.action.servlet.api;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * API dokumen operasional distribusi/pengiriman untuk POS Desktop dan Android.
 *
 * <p>Dokumen operasional tidak semuanya memengaruhi stok. Posting hanya dilakukan saat
 * penerimaan transfer outlet atau reverse logistics berstatus COMPLETED, lalu dibalik sekali
 * saat berstatus REVERSED. Kunci unik per dokumen/baris/arah mencegah posting ganda. Seluruh
 * koneksi dibuka melalui Hibernate dan ditutup pada finally.</p>
 */
public final class DistribusiPengirimanApiHelper {

	private static final Map<String, String> JENIS = new HashMap<String, String>();
	static {
		JENIS.put("delivery_order", "DO");
		JENIS.put("freight_order", "FO");
		JENIS.put("shipment_tracking", "SHP");
		JENIS.put("proof_of_delivery", "POD");
		JENIS.put("penerimaan_transfer_outlet", "RCV");
		JENIS.put("klaim_distribusi", "CLM");
		JENIS.put("reverse_logistics", "REV");
	}

	private DistribusiPengirimanApiHelper() {
	}

	private static void tutup(ResultSet c) {
		if (c == null) return;
		try { c.close(); } catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit DistribusiPengirimanApiHelper.tutupResultSet");
		}
	}

	private static void tutup(Statement c) {
		if (c == null) return;
		try { c.close(); } catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit DistribusiPengirimanApiHelper.tutupStatement");
		}
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("message", pesan);
	}

	private static String jenis(JSONObject request) {
		return request == null ? "" : request.optString("jenis", "").trim();
	}

	private static String aksiUntukStatus(String status) {
		if ("SUBMITTED".equals(status)) return "submit";
		if ("APPROVED".equals(status)) return "approve";
		if ("REJECTED".equals(status)) return "reject";
		if ("CANCELLED".equals(status)) return "cancel";
		if ("REVERSED".equals(status)) return "reverse";
		return "update";
	}

	private static EbisnisActorContextResolver.ActorContext aktor(Session session,
			Tbmuser tbmuser, JSONObject request, JSONObject hasil, String aksi) throws Exception {
		EbisnisActorContextResolver.ActorContext ctx = EbisnisActorContextResolver.resolve(session, tbmuser);
		String j = jenis(request);
		if (!JENIS.containsKey(j)) {
			tolak(hasil, "Jenis dokumen pengiriman tidak dikenal.");
			return null;
		}
		if (!ctx.bolehAksi(j, aksi)) {
			tolak(hasil, "Akses " + aksi + " untuk menu pengiriman ini tidak diizinkan.");
			return null;
		}
		if (ctx.tokoId == null && !ctx.admin) {
			tolak(hasil, "Toko aktif belum dipilih.");
			return null;
		}
		return ctx;
	}

	private static long tokoId(EbisnisActorContextResolver.ActorContext ctx, JSONObject request) {
		long dariRequest = request == null ? 0L : request.optLong("tokoId", 0L);
		if (ctx.admin && dariRequest > 0L) return dariRequest;
		return ctx.tokoId == null ? 0L : ctx.tokoId.longValue();
	}

	private static JSONObject baris(ResultSet rs) throws Exception {
		JSONObject j = new JSONObject();
		j.put("id", rs.getLong("id"));
		j.put("jenis", rs.getString("document_type"));
		j.put("nomor", rs.getString("document_no"));
		j.put("statusDokumen", rs.getString("status"));
		j.put("referensi", nilai(rs.getString("reference_no")));
		j.put("asal", nilai(rs.getString("origin_name")));
		j.put("tujuan", nilai(rs.getString("destination_name")));
		long originTokoId = rs.getLong("origin_toko_id");
		j.put("asalTokoId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(originTokoId));
		long destinationTokoId = rs.getLong("destination_toko_id");
		j.put("tujuanTokoId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(destinationTokoId));
		j.put("pengangkut", nilai(rs.getString("carrier_name")));
		j.put("nomorPelacakan", nilai(rs.getString("tracking_no")));
		j.put("penerima", nilai(rs.getString("receiver_name")));
		j.put("buktiUrl", nilai(rs.getString("proof_url")));
		j.put("nomorTagihanAngkut", nilai(rs.getString("freight_invoice_no")));
		BigDecimal nilaiTagihan = rs.getBigDecimal("freight_amount");
		j.put("nilaiTagihanAngkut", nilaiTagihan == null ? BigDecimal.ZERO : nilaiTagihan);
		j.put("tanggalTagihanAngkut", waktu(rs.getTimestamp("freight_invoice_date")));
		j.put("rencana", waktu(rs.getTimestamp("planned_at")));
		j.put("aktual", waktu(rs.getTimestamp("actual_at")));
		j.put("catatan", nilai(rs.getString("notes")));
		j.put("dibuatOleh", nilai(rs.getString("created_by")));
		j.put("diperbarui", waktu(rs.getTimestamp("updated_at")));
		j.put("jumlahBaris", rs.getInt("line_count"));
		return j;
	}

	private static String nilai(String s) { return s == null ? "" : s; }
	private static String waktu(Timestamp t) {
		return t == null ? "" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(t);
	}

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			EbisnisActorContextResolver.ActorContext ctx = aktor(session, tbmuser, request, hasil, "view");
			if (ctx == null) return;
			Connection conn = session.connection();
			String cari = request.optString("cari", "").trim();
			int limit = request.optInt("limit", 100);
			if (limit < 1 || limit > 500) limit = 100;
			ps = conn.prepareStatement("SELECT d.*, (SELECT count(*) FROM inventory_distribution.distribution_document_line l WHERE l.document_id=d.id) line_count FROM inventory_distribution.distribution_document d WHERE d.toko_id=? AND d.document_type=? AND (?='' OR d.document_no ILIKE ? OR COALESCE(d.reference_no,'') ILIKE ? OR COALESCE(d.destination_name,'') ILIKE ?) ORDER BY d.updated_at DESC LIMIT ?");
			ps.setLong(1, tokoId(ctx, request));
			ps.setString(2, jenis(request));
			ps.setString(3, cari);
			String like = "%" + cari + "%";
			ps.setString(4, like); ps.setString(5, like); ps.setString(6, like); ps.setInt(7, limit);
			rs = ps.executeQuery();
			JSONArray data = new JSONArray();
			while (rs.next()) data.put(baris(rs));
			hasil.put("status", "success");
			hasil.put("data", data);
			hasil.put("hakAkses", hak(ctx, jenis(request)));
		} finally {
			tutup(rs); tutup(ps); HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static JSONObject hak(EbisnisActorContextResolver.ActorContext ctx, String j) throws Exception {
		JSONObject h = new JSONObject();
		h.put("create", ctx.bolehAksi(j, "create"));
		h.put("update", ctx.bolehAksi(j, "update") || ctx.bolehAksi(j, "edit_draft"));
		h.put("submit", ctx.bolehAksi(j, "submit"));
		h.put("approve", ctx.bolehAksi(j, "approve"));
		h.put("reject", ctx.bolehAksi(j, "reject"));
		h.put("cancel", ctx.bolehAksi(j, "cancel"));
		h.put("reverse", ctx.bolehAksi(j, "reverse"));
		return h;
	}

	public static void detail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		PreparedStatement ps = null; ResultSet rs = null;
		try {
			EbisnisActorContextResolver.ActorContext ctx = aktor(session, tbmuser, request, hasil, "view");
			if (ctx == null) return;
			Connection conn = session.connection();
			ps = conn.prepareStatement("SELECT d.*, (SELECT count(*) FROM inventory_distribution.distribution_document_line l WHERE l.document_id=d.id) line_count FROM inventory_distribution.distribution_document d WHERE d.id=? AND d.toko_id=? AND d.document_type=?");
			ps.setLong(1, request.optLong("id", 0)); ps.setLong(2, tokoId(ctx, request)); ps.setString(3, jenis(request));
			rs = ps.executeQuery();
			if (!rs.next()) { tolak(hasil, "Dokumen pengiriman tidak ditemukan."); return; }
			JSONObject data = baris(rs); tutup(rs); tutup(ps); rs = null; ps = null;
			ps = conn.prepareStatement("SELECT item_id,item_code,item_name,qty,uom,notes,source_product_id,destination_product_id FROM inventory_distribution.distribution_document_line WHERE document_id=? ORDER BY line_no");
			ps.setLong(1, request.optLong("id", 0)); rs = ps.executeQuery();
			JSONArray lines = new JSONArray();
			while (rs.next()) {
				JSONObject l = new JSONObject();
				long itemId = rs.getLong(1); l.put("itemId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(itemId));
				l.put("kode", nilai(rs.getString(2))); l.put("nama", nilai(rs.getString(3)));
				l.put("qty", rs.getDouble(4)); l.put("uom", nilai(rs.getString(5))); l.put("catatan", nilai(rs.getString(6)));
				long sourceProductId = rs.getLong(7); l.put("sourceProductId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(sourceProductId));
				long destinationProductId = rs.getLong(8); l.put("destinationProductId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(destinationProductId));
				lines.put(l);
			}
			data.put("baris", lines);
			tutup(rs); tutup(ps); rs = null; ps = null;
			ps = conn.prepareStatement("SELECT from_status,to_status,notes,actor_id,event_at FROM inventory_distribution.distribution_document_event WHERE document_id=? ORDER BY event_at,id");
			ps.setLong(1, request.optLong("id", 0)); rs = ps.executeQuery();
			JSONArray events = new JSONArray();
			while (rs.next()) {
				JSONObject e = new JSONObject();
				e.put("dariStatus", nilai(rs.getString(1))); e.put("keStatus", nilai(rs.getString(2)));
				e.put("catatan", nilai(rs.getString(3))); e.put("pelaku", nilai(rs.getString(4)));
				e.put("waktu", waktu(rs.getTimestamp(5))); events.put(e);
			}
			data.put("riwayatStatus", events);
			tutup(rs); tutup(ps); rs = null; ps = null;
			ps = conn.prepareStatement("SELECT direction,legacy_mutation_id,source_toko_id,destination_toko_id,source_product_id,destination_product_id,qty,created_by,created_at FROM inventory_distribution.distribution_stock_posting WHERE document_id=? ORDER BY created_at,id");
			ps.setLong(1, request.optLong("id", 0)); rs = ps.executeQuery();
			JSONArray postings = new JSONArray();
			while (rs.next()) {
				JSONObject p = new JSONObject();
				p.put("arah", nilai(rs.getString(1))); p.put("mutasiId", rs.getLong(2));
				p.put("tokoAsalId", rs.getLong(3)); p.put("tokoTujuanId", rs.getLong(4));
				p.put("produkAsalId", rs.getLong(5)); p.put("produkTujuanId", rs.getLong(6));
				p.put("qty", rs.getDouble(7)); p.put("dibuatOleh", nilai(rs.getString(8)));
				p.put("waktu", waktu(rs.getTimestamp(9))); postings.put(p);
			}
			data.put("postingStok", postings);
			hasil.put("status", "success"); hasil.put("data", data); hasil.put("hakAkses", hak(ctx, jenis(request)));
		} finally { tutup(rs); tutup(ps); HibernateUtil.closeSessionQuietly(session); }
	}

	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		PreparedStatement ps = null; ResultSet rs = null;
		try {
			long id = request.optLong("id", 0L);
			String aksi = id > 0L ? "edit_draft" : "create";
			EbisnisActorContextResolver.ActorContext ctx = aktor(session, tbmuser, request, hasil, aksi);
			if (ctx == null) return;
			String tujuan = request.optString("tujuan", "").trim();
			if (tujuan.length() == 0) { tolak(hasil, "Tujuan dokumen wajib diisi."); return; }
			Connection conn = session.connection(); conn.setAutoCommit(false);
			String mutation = request.optString("clientMutationId", "").trim();
			if (id <= 0L && mutation.length() > 0) {
				ps = conn.prepareStatement("SELECT id FROM inventory_distribution.distribution_document WHERE toko_id=? AND client_mutation_id=?");
				ps.setLong(1, tokoId(ctx, request)); ps.setString(2, mutation); rs = ps.executeQuery();
				if (rs.next()) {
					id = rs.getLong(1);
					tutup(rs); tutup(ps); rs = null; ps = null;
					conn.commit();
					hasil.put("status", "success"); hasil.put("id", id);
					hasil.put("message", "Dokumen pengiriman sudah tersimpan.");
					return;
				}
				tutup(rs); tutup(ps); rs = null; ps = null;
			}
			boolean dokumenBaru = id <= 0L;
			if (dokumenBaru) {
				String nomor = request.optString("nomor", "").trim();
				if (nomor.length() == 0) nomor = JENIS.get(jenis(request)) + "-" + new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new java.util.Date());
				ps = conn.prepareStatement("INSERT INTO inventory_distribution.distribution_document(toko_id,document_type,document_no,status,reference_no,origin_name,destination_name,origin_toko_id,destination_toko_id,carrier_name,tracking_no,planned_at,actual_at,notes,client_mutation_id,receiver_name,proof_url,freight_invoice_no,freight_amount,freight_invoice_date,created_by,created_at,updated_by,updated_at,version) VALUES(?,?,?,'DRAFT',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),?,now(),0) RETURNING id");
				int n=1; ps.setLong(n++, tokoId(ctx, request)); ps.setString(n++, jenis(request)); ps.setString(n++, nomor);
				isiTeks(ps,n++,request,"referensi"); isiTeks(ps,n++,request,"asal"); ps.setString(n++,tujuan); isiLong(ps,n++,request,"asalTokoId"); isiLong(ps,n++,request,"tujuanTokoId");
				isiTeks(ps,n++,request,"pengangkut"); isiTeks(ps,n++,request,"nomorPelacakan"); isiWaktu(ps,n++,request,"rencana"); isiWaktu(ps,n++,request,"aktual"); isiTeks(ps,n++,request,"catatan");
				ps.setString(n++, mutation.length()==0?null:mutation); isiTeks(ps,n++,request,"penerima"); isiTeks(ps,n++,request,"buktiUrl"); isiTeks(ps,n++,request,"nomorTagihanAngkut"); isiDesimal(ps,n++,request,"nilaiTagihanAngkut"); isiWaktu(ps,n++,request,"tanggalTagihanAngkut"); ps.setString(n++, ctx.userId); ps.setString(n++, ctx.userId);
				rs=ps.executeQuery(); rs.next(); id=rs.getLong(1); tutup(rs); tutup(ps); rs=null; ps=null;
			} else {
				ps=conn.prepareStatement("UPDATE inventory_distribution.distribution_document SET reference_no=?,origin_name=?,destination_name=?,origin_toko_id=?,destination_toko_id=?,carrier_name=?,tracking_no=?,planned_at=?,actual_at=?,notes=?,receiver_name=?,proof_url=?,freight_invoice_no=?,freight_amount=?,freight_invoice_date=?,updated_by=?,updated_at=now(),version=version+1 WHERE id=? AND toko_id=? AND document_type=? AND status='DRAFT'");
				int n=1; isiTeks(ps,n++,request,"referensi"); isiTeks(ps,n++,request,"asal"); ps.setString(n++,tujuan); isiLong(ps,n++,request,"asalTokoId"); isiLong(ps,n++,request,"tujuanTokoId"); isiTeks(ps,n++,request,"pengangkut"); isiTeks(ps,n++,request,"nomorPelacakan"); isiWaktu(ps,n++,request,"rencana"); isiWaktu(ps,n++,request,"aktual"); isiTeks(ps,n++,request,"catatan"); isiTeks(ps,n++,request,"penerima"); isiTeks(ps,n++,request,"buktiUrl"); isiTeks(ps,n++,request,"nomorTagihanAngkut"); isiDesimal(ps,n++,request,"nilaiTagihanAngkut"); isiWaktu(ps,n++,request,"tanggalTagihanAngkut"); ps.setString(n++,ctx.userId); ps.setLong(n++,id); ps.setLong(n++,tokoId(ctx,request)); ps.setString(n++,jenis(request));
				if(ps.executeUpdate()!=1){tolak(hasil,"Hanya dokumen DRAFT yang dapat diedit.");conn.rollback();return;} tutup(ps);ps=null;
			}
			ps=conn.prepareStatement("DELETE FROM inventory_distribution.distribution_document_line WHERE document_id=?");ps.setLong(1,id);ps.executeUpdate();tutup(ps);ps=null;
			JSONArray lines=request.optJSONArray("baris");
			if(lines!=null){
				ps=conn.prepareStatement("INSERT INTO inventory_distribution.distribution_document_line(document_id,line_no,item_id,item_code,item_name,qty,uom,notes,source_product_id,destination_product_id) VALUES(?,?,?,?,?,?,?,?,?,?)");
				for(int i=0;i<lines.length();i++){JSONObject l=lines.optJSONObject(i);if(l==null)continue;String nama=l.optString("nama","").trim();if(nama.length()==0)continue;ps.setLong(1,id);ps.setInt(2,i+1);long item=l.optLong("itemId",0);if(item>0)ps.setLong(3,item);else ps.setNull(3,java.sql.Types.BIGINT);ps.setString(4,l.optString("kode",""));ps.setString(5,nama);ps.setDouble(6,l.optDouble("qty",0));ps.setString(7,l.optString("uom",""));ps.setString(8,l.optString("catatan",""));isiLong(ps,9,l,"sourceProductId");isiLong(ps,10,l,"destinationProductId");ps.addBatch();}ps.executeBatch();
			}
			if (dokumenBaru) {
				tutup(ps); ps = null;
				ps=conn.prepareStatement("INSERT INTO inventory_distribution.distribution_document_event(document_id,from_status,to_status,notes,actor_id,event_at) VALUES(?,NULL,'DRAFT',?,?,now())");
				ps.setLong(1,id); ps.setString(2,"Dokumen dibuat"); ps.setString(3,ctx.userId); ps.executeUpdate();
			}
			conn.commit(); hasil.put("status","success"); hasil.put("id",id); hasil.put("message","Dokumen pengiriman tersimpan.");
		} catch(Exception e){try{session.connection().rollback();}catch(Exception ignored){ais.common.ErrorAuditUtil.record(ignored,"auto-audit DistribusiPengirimanApiHelper.rollback");}throw e;}
		finally{tutup(rs);tutup(ps);HibernateUtil.closeSessionQuietly(session);}
	}

	private static void isiTeks(PreparedStatement ps,int i,JSONObject r,String k)throws Exception{String v=r.optString(k,"").trim();ps.setString(i,v.length()==0?null:v);}
	private static void isiLong(PreparedStatement ps,int i,JSONObject r,String k)throws Exception{long v=r.optLong(k,0L);if(v>0L)ps.setLong(i,v);else ps.setNull(i,java.sql.Types.BIGINT);}
	private static void isiDesimal(PreparedStatement ps,int i,JSONObject r,String k)throws Exception{String v=r.optString(k,"").trim().replace(",","");if(v.length()==0){ps.setNull(i,java.sql.Types.NUMERIC);return;}ps.setBigDecimal(i,new BigDecimal(v));}
	private static void isiWaktu(PreparedStatement ps,int i,JSONObject r,String k)throws Exception{String v=r.optString(k,"").trim();if(v.length()==0){ps.setNull(i,java.sql.Types.TIMESTAMP);return;}try{ps.setTimestamp(i,Timestamp.valueOf(v.length()==16?v+":00":v));}catch(Exception e){ps.setNull(i,java.sql.Types.TIMESTAMP);}}

	public static void ubahStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String tujuanStatus=request.optString("statusDokumen","").trim().toUpperCase();
		String aksi=aksiUntukStatus(tujuanStatus);
		Session session=HibernateUtil.getSessionFactory().openSession();PreparedStatement ps=null;ResultSet rs=null;
		try{
			EbisnisActorContextResolver.ActorContext ctx=aktor(session,tbmuser,request,hasil,aksi);if(ctx==null)return;
			Connection conn=session.connection();conn.setAutoCommit(false);
			ps=conn.prepareStatement("SELECT status,origin_toko_id,destination_toko_id,document_no,document_type,carrier_name,tracking_no,receiver_name,proof_url,freight_invoice_no,freight_amount FROM inventory_distribution.distribution_document WHERE id=? AND toko_id=? AND document_type=? FOR UPDATE");ps.setLong(1,request.optLong("id",0));ps.setLong(2,tokoId(ctx,request));ps.setString(3,jenis(request));rs=ps.executeQuery();if(!rs.next()){tolak(hasil,"Dokumen tidak ditemukan.");conn.rollback();return;}String asal=rs.getString(1);long originTokoId=rs.getLong(2);if(rs.wasNull())originTokoId=0L;long destinationTokoId=rs.getLong(3);if(rs.wasNull())destinationTokoId=0L;String nomor=rs.getString(4);String jenisDokumen=rs.getString(5);String pengangkut=rs.getString(6);String pelacakan=rs.getString(7);String penerima=rs.getString(8);String bukti=rs.getString(9);String nomorTagihan=rs.getString(10);BigDecimal nilaiTagihan=rs.getBigDecimal(11);tutup(rs);tutup(ps);rs=null;ps=null;
			if(!transisiBoleh(asal,tujuanStatus)){tolak(hasil,"Perubahan status "+asal+" ke "+tujuanStatus+" tidak diizinkan.");conn.rollback();return;}
			if(!validasiKelengkapanStatus(jenisDokumen,tujuanStatus,pengangkut,pelacakan,penerima,bukti,nomorTagihan,nilaiTagihan,hasil)){conn.rollback();return;}
			if("COMPLETED".equals(tujuanStatus)&&memengaruhiStok(jenis(request))){
				if(!validasiTokoStok(ctx,jenis(request),originTokoId,destinationTokoId,hasil)){conn.rollback();return;}
				postingStok(conn,request.optLong("id",0),nomor,originTokoId,destinationTokoId,"FORWARD",ctx.userId);
			}else if("REVERSED".equals(tujuanStatus)&&memengaruhiStok(jenis(request))){
				postingStok(conn,request.optLong("id",0),nomor,destinationTokoId,originTokoId,"REVERSE",ctx.userId);
			}
			ps=conn.prepareStatement("UPDATE inventory_distribution.distribution_document SET status=?,updated_by=?,updated_at=now(),version=version+1 WHERE id=?");ps.setString(1,tujuanStatus);ps.setString(2,ctx.userId);ps.setLong(3,request.optLong("id",0));ps.executeUpdate();tutup(ps);ps=null;
			ps=conn.prepareStatement("INSERT INTO inventory_distribution.distribution_document_event(document_id,from_status,to_status,notes,actor_id,event_at) VALUES(?,?,?,?,?,now())");ps.setLong(1,request.optLong("id",0));ps.setString(2,asal);ps.setString(3,tujuanStatus);ps.setString(4,request.optString("catatanStatus",""));ps.setString(5,ctx.userId);ps.executeUpdate();conn.commit();hasil.put("status","success");hasil.put("message","Status dokumen diperbarui menjadi "+tujuanStatus+".");
		}catch(Exception e){try{session.connection().rollback();}catch(Exception ignored){ais.common.ErrorAuditUtil.record(ignored,"auto-audit DistribusiPengirimanApiHelper.ubahStatus.rollback");}throw e;}finally{tutup(rs);tutup(ps);HibernateUtil.closeSessionQuietly(session);}
	}

	private static boolean memengaruhiStok(String jenis){return "penerimaan_transfer_outlet".equals(jenis)||"reverse_logistics".equals(jenis);}

	private static boolean validasiKelengkapanStatus(String jenis,String status,String pengangkut,String pelacakan,String penerima,String bukti,String nomorTagihan,BigDecimal nilaiTagihan,JSONObject hasil)throws Exception{
		if(("IN_PROGRESS".equals(status)||"COMPLETED".equals(status))&&"shipment_tracking".equals(jenis)&&(kosong(pengangkut)||kosong(pelacakan))){tolak(hasil,"Pengangkut dan nomor pelacakan wajib diisi sebelum shipment dijalankan.");return false;}
		if("COMPLETED".equals(status)&&"proof_of_delivery".equals(jenis)&&(kosong(penerima)||kosong(bukti))){tolak(hasil,"Nama penerima dan URL bukti penerimaan wajib diisi sebelum POD diselesaikan.");return false;}
		if("COMPLETED".equals(status)&&"freight_order".equals(jenis)&&(kosong(nomorTagihan)||nilaiTagihan==null||nilaiTagihan.compareTo(BigDecimal.ZERO)<=0)){tolak(hasil,"Nomor dan nilai tagihan angkut wajib diisi sebelum freight order diselesaikan.");return false;}
		return true;
	}

	private static boolean kosong(String nilai){return nilai==null||nilai.trim().length()==0;}

	private static boolean validasiTokoStok(EbisnisActorContextResolver.ActorContext ctx,String jenis,long asal,long tujuan,JSONObject hasil)throws Exception{
		if(asal<=0L||tujuan<=0L||asal==tujuan){tolak(hasil,"Toko asal dan tujuan stok wajib diisi serta harus berbeda.");return false;}
		if(!ctx.admin&&"penerimaan_transfer_outlet".equals(jenis)&&ctx.tokoId!=null&&tujuan!=ctx.tokoId.longValue()){tolak(hasil,"Penerimaan hanya boleh diposting ke toko aktif.");return false;}
		if(!ctx.admin&&"reverse_logistics".equals(jenis)&&ctx.tokoId!=null&&asal!=ctx.tokoId.longValue()){tolak(hasil,"Reverse logistics hanya boleh berasal dari toko aktif.");return false;}
		return true;
	}

	private static long produkUntukToko(Connection conn,long eksplisit,String kode,long tokoId)throws Exception{
		PreparedStatement ps=null;ResultSet rs=null;
		try{
			if(eksplisit>0L){ps=conn.prepareStatement("SELECT id FROM koperasi.produk WHERE id=? AND toko=?");ps.setLong(1,eksplisit);ps.setLong(2,tokoId);rs=ps.executeQuery();if(rs.next())return rs.getLong(1);tutup(rs);tutup(ps);rs=null;ps=null;}
			if(kode==null||kode.trim().length()==0)return 0L;
			ps=conn.prepareStatement("SELECT id FROM koperasi.produk WHERE toko=? AND lower(trim(coalesce(kode,'')))=lower(trim(?)) ORDER BY id LIMIT 1");ps.setLong(1,tokoId);ps.setString(2,kode);rs=ps.executeQuery();return rs.next()?rs.getLong(1):0L;
		}finally{tutup(rs);tutup(ps);}
	}

	private static void postingStok(Connection conn,long documentId,String nomor,long tokoAsal,long tokoTujuan,String arah,String userId)throws Exception{
		PreparedStatement lines=null;PreparedStatement cek=null;PreparedStatement mutasi=null;PreparedStatement jejak=null;ResultSet rs=null;ResultSet cekRs=null;ResultSet mutasiRs=null;
		try{
			lines=conn.prepareStatement("SELECT id,item_id,item_code,item_name,qty,source_product_id,destination_product_id FROM inventory_distribution.distribution_document_line WHERE document_id=? ORDER BY line_no FOR UPDATE");lines.setLong(1,documentId);rs=lines.executeQuery();boolean ada=false;
			while(rs.next()){
				ada=true;long lineId=rs.getLong(1);double qty=rs.getDouble(5);if(qty<=0D)throw new IllegalArgumentException("Qty dokumen "+nomor+" harus lebih besar dari nol.");
				cek=conn.prepareStatement("SELECT legacy_mutation_id FROM inventory_distribution.distribution_stock_posting WHERE document_id=? AND line_id=? AND direction=?");cek.setLong(1,documentId);cek.setLong(2,lineId);cek.setString(3,arah);cekRs=cek.executeQuery();if(cekRs.next()){tutup(cekRs);tutup(cek);cekRs=null;cek=null;continue;}tutup(cekRs);tutup(cek);cekRs=null;cek=null;
				long itemId=rs.getLong(2);if(rs.wasNull())itemId=0L;long sourceId=rs.getLong(6);if(rs.wasNull())sourceId=0L;long destinationId=rs.getLong(7);if(rs.wasNull())destinationId=0L;String kode=rs.getString(3);
				if("REVERSE".equals(arah)){long swap=sourceId;sourceId=destinationId;destinationId=swap;}
				if(sourceId<=0L)sourceId=produkUntukToko(conn,itemId,kode,tokoAsal);if(destinationId<=0L)destinationId=produkUntukToko(conn,0L,kode,tokoTujuan);
				if(sourceId<=0L||destinationId<=0L)throw new IllegalArgumentException("Produk '"+rs.getString(4)+"' belum dipetakan pada toko asal dan tujuan.");
				mutasi=conn.prepareStatement("INSERT INTO koperasi.mutasi_stok_toko(produk_asal,produk_tujuan,toko_asal,toko_tujuan,qty,waktu,keterangan,oleh,tanggal_dirubah) VALUES(?,?,?,?,?,now(),?,?,now()) RETURNING id");mutasi.setLong(1,sourceId);mutasi.setLong(2,destinationId);mutasi.setLong(3,tokoAsal);mutasi.setLong(4,tokoTujuan);mutasi.setDouble(5,qty);mutasi.setString(6,("REVERSE".equals(arah)?"Pembalikan ":"Posting ")+"pengiriman "+nomor);mutasi.setString(7,userId);mutasiRs=mutasi.executeQuery();mutasiRs.next();long mutationId=mutasiRs.getLong(1);tutup(mutasiRs);tutup(mutasi);mutasiRs=null;mutasi=null;
				jejak=conn.prepareStatement("INSERT INTO inventory_distribution.distribution_stock_posting(document_id,line_id,direction,legacy_mutation_id,source_toko_id,destination_toko_id,source_product_id,destination_product_id,qty,created_by,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,now())");jejak.setLong(1,documentId);jejak.setLong(2,lineId);jejak.setString(3,arah);jejak.setLong(4,mutationId);jejak.setLong(5,tokoAsal);jejak.setLong(6,tokoTujuan);jejak.setLong(7,sourceId);jejak.setLong(8,destinationId);jejak.setDouble(9,qty);jejak.setString(10,userId);jejak.executeUpdate();tutup(jejak);jejak=null;
			}
			if(!ada)throw new IllegalArgumentException("Dokumen "+nomor+" belum memiliki rincian barang.");
		}finally{tutup(mutasiRs);tutup(cekRs);tutup(rs);tutup(jejak);tutup(mutasi);tutup(cek);tutup(lines);}
	}

	private static boolean transisiBoleh(String a,String b){if(a==null||b==null||a.equals(b))return false;if("DRAFT".equals(a))return "SUBMITTED".equals(b)||"CANCELLED".equals(b);if("SUBMITTED".equals(a))return "APPROVED".equals(b)||"REJECTED".equals(b)||"CANCELLED".equals(b);if("APPROVED".equals(a))return "IN_PROGRESS".equals(b)||"COMPLETED".equals(b)||"CANCELLED".equals(b);if("IN_PROGRESS".equals(a))return "COMPLETED".equals(b)||"CANCELLED".equals(b);if("REJECTED".equals(a))return "DRAFT".equals(b);if("COMPLETED".equals(a))return "REVERSED".equals(b);return false;}
}
