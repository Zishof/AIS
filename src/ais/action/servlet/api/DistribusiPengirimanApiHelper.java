package ais.action.servlet.api;

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

	private static void pastikanTabel(Connection conn) throws Exception {
		Statement st = null;
		try {
			st = conn.createStatement();
			st.execute("CREATE SCHEMA IF NOT EXISTS inventory_distribution");
			st.execute("CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_document ("
					+ "id bigserial PRIMARY KEY, toko_id bigint NOT NULL, document_type varchar(50) NOT NULL,"
					+ "document_no varchar(80) NOT NULL, status varchar(30) NOT NULL DEFAULT 'DRAFT',"
					+ "reference_no varchar(120), origin_name varchar(180), destination_name varchar(180),"
					+ "carrier_name varchar(180), tracking_no varchar(120), planned_at timestamp, actual_at timestamp,"
					+ "notes text, client_mutation_id varchar(100), created_by varchar(100), created_at timestamp NOT NULL DEFAULT now(),"
					+ "updated_by varchar(100), updated_at timestamp NOT NULL DEFAULT now(), version bigint NOT NULL DEFAULT 0,"
					+ "CONSTRAINT uq_distribution_document_no UNIQUE (toko_id, document_type, document_no))");
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_distribution_document_mutation ON inventory_distribution.distribution_document(toko_id, client_mutation_id) WHERE client_mutation_id IS NOT NULL AND client_mutation_id <> ''");
			st.execute("CREATE INDEX IF NOT EXISTS ix_distribution_document_list ON inventory_distribution.distribution_document(toko_id, document_type, updated_at DESC)");
			st.execute("CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_document_line ("
					+ "id bigserial PRIMARY KEY, document_id bigint NOT NULL REFERENCES inventory_distribution.distribution_document(id) ON DELETE CASCADE,"
					+ "line_no integer NOT NULL, item_id bigint, item_code varchar(100), item_name varchar(255) NOT NULL,"
					+ "qty numeric(24,6) NOT NULL DEFAULT 0, uom varchar(50), notes text,"
					+ "CONSTRAINT uq_distribution_document_line UNIQUE(document_id,line_no))");
			st.execute("CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_document_event ("
					+ "id bigserial PRIMARY KEY, document_id bigint NOT NULL REFERENCES inventory_distribution.distribution_document(id) ON DELETE CASCADE,"
					+ "from_status varchar(30), to_status varchar(30) NOT NULL, notes text, actor_id varchar(100), event_at timestamp NOT NULL DEFAULT now())");
			st.execute("ALTER TABLE inventory_distribution.distribution_document ADD COLUMN IF NOT EXISTS origin_toko_id bigint");
			st.execute("ALTER TABLE inventory_distribution.distribution_document ADD COLUMN IF NOT EXISTS destination_toko_id bigint");
			st.execute("ALTER TABLE inventory_distribution.distribution_document_line ADD COLUMN IF NOT EXISTS source_product_id bigint");
			st.execute("ALTER TABLE inventory_distribution.distribution_document_line ADD COLUMN IF NOT EXISTS destination_product_id bigint");
			st.execute("CREATE TABLE IF NOT EXISTS inventory_distribution.distribution_stock_posting (id bigserial PRIMARY KEY, document_id bigint NOT NULL REFERENCES inventory_distribution.distribution_document(id),line_id bigint NOT NULL REFERENCES inventory_distribution.distribution_document_line(id), direction varchar(10) NOT NULL,legacy_mutation_id bigint NOT NULL, source_toko_id bigint NOT NULL, destination_toko_id bigint NOT NULL,source_product_id bigint NOT NULL, destination_product_id bigint NOT NULL, qty numeric(24,6) NOT NULL,created_by varchar(100), created_at timestamp NOT NULL DEFAULT now(),CONSTRAINT uq_distribution_stock_posting UNIQUE(document_id,line_id,direction))");
			st.execute("CREATE INDEX IF NOT EXISTS ix_distribution_stock_posting_document ON inventory_distribution.distribution_stock_posting(document_id,direction)");
		} finally {
			tutup(st);
		}
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
		long originTokoId = rs.getLong("origin_toko_id"); j.put("asalTokoId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(originTokoId));
		long destinationTokoId = rs.getLong("destination_toko_id"); j.put("tujuanTokoId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(destinationTokoId));
		j.put("pengangkut", nilai(rs.getString("carrier_name")));
		j.put("nomorPelacakan", nilai(rs.getString("tracking_no")));
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
			pastikanTabel(conn);
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
			Connection conn = session.connection(); pastikanTabel(conn);
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
				long sourceProductId = rs.getLong(7); l.put("sourceProductId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(sourceProductId)); long destinationProductId = rs.getLong(8); l.put("destinationProductId", rs.wasNull() ? JSONObject.NULL : Long.valueOf(destinationProductId));
				lines.put(l);
			}
			data.put("baris", lines);
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
			Connection conn = session.connection(); pastikanTabel(conn); conn.setAutoCommit(false);
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
			if (id <= 0L) {
				String nomor = request.optString("nomor", "").trim();
				if (nomor.length() == 0) nomor = JENIS.get(jenis(request)) + "-" + new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new java.util.Date());
				ps = conn.prepareStatement("INSERT INTO inventory_distribution.distribution_document(toko_id,document_type,document_no,status,reference_no,origin_name,destination_name,carrier_name,tracking_no,planned_at,actual_at,notes,client_mutation_id,created_by,updated_by) VALUES(?,?,?,'DRAFT',?,?,?,?,?,?,?,?,?,?,?) RETURNING id");
				int n=1; ps.setLong(n++, tokoId(ctx, request)); ps.setString(n++, jenis(request)); ps.setString(n++, nomor);
				isiTeks(ps,n++,request,"referensi"); isiTeks(ps,n++,request,"asal"); ps.setString(n++,tujuan);
				isiTeks(ps,n++,request,"pengangkut"); isiTeks(ps,n++,request,"nomorPelacakan"); isiWaktu(ps,n++,request,"rencana"); isiWaktu(ps,n++,request,"aktual"); isiTeks(ps,n++,request,"catatan");
				ps.setString(n++, mutation.length()==0?null:mutation); ps.setString(n++, ctx.userId); ps.setString(n++, ctx.userId);
				rs=ps.executeQuery(); rs.next(); id=rs.getLong(1); tutup(rs); tutup(ps); rs=null; ps=null;
			} else {
				ps=conn.prepareStatement("UPDATE inventory_distribution.distribution_document SET reference_no=?,origin_name=?,destination_name=?,carrier_name=?,tracking_no=?,planned_at=?,actual_at=?,notes=?,updated_by=?,updated_at=now(),version=version+1 WHERE id=? AND toko_id=? AND document_type=? AND status='DRAFT'");
				int n=1; isiTeks(ps,n++,request,"referensi"); isiTeks(ps,n++,request,"asal"); ps.setString(n++,tujuan); isiTeks(ps,n++,request,"pengangkut"); isiTeks(ps,n++,request,"nomorPelacakan"); isiWaktu(ps,n++,request,"rencana"); isiWaktu(ps,n++,request,"aktual"); isiTeks(ps,n++,request,"catatan"); ps.setString(n++,ctx.userId); ps.setLong(n++,id); ps.setLong(n++,tokoId(ctx,request)); ps.setString(n++,jenis(request));
				if(ps.executeUpdate()!=1){tolak(hasil,"Hanya dokumen DRAFT yang dapat diedit.");conn.rollback();return;} tutup(ps);ps=null;
			}
			ps=conn.prepareStatement("DELETE FROM inventory_distribution.distribution_document_line WHERE document_id=?");ps.setLong(1,id);ps.executeUpdate();tutup(ps);ps=null;
			JSONArray lines=request.optJSONArray("baris");
			if(lines!=null){
				ps=conn.prepareStatement("INSERT INTO inventory_distribution.distribution_document_line(document_id,line_no,item_id,item_code,item_name,qty,uom,notes) VALUES(?,?,?,?,?,?,?,?)");
				for(int i=0;i<lines.length();i++){JSONObject l=lines.optJSONObject(i);if(l==null)continue;String nama=l.optString("nama","").trim();if(nama.length()==0)continue;ps.setLong(1,id);ps.setInt(2,i+1);long item=l.optLong("itemId",0);if(item>0)ps.setLong(3,item);else ps.setNull(3,java.sql.Types.BIGINT);ps.setString(4,l.optString("kode",""));ps.setString(5,nama);ps.setDouble(6,l.optDouble("qty",0));ps.setString(7,l.optString("uom",""));ps.setString(8,l.optString("catatan",""));ps.addBatch();}ps.executeBatch();
			}
			conn.commit(); hasil.put("status","success"); hasil.put("id",id); hasil.put("message","Dokumen pengiriman tersimpan.");
		} catch(Exception e){try{session.connection().rollback();}catch(Exception ignored){ais.common.ErrorAuditUtil.record(ignored,"auto-audit DistribusiPengirimanApiHelper.rollback");}throw e;}
		finally{tutup(rs);tutup(ps);HibernateUtil.closeSessionQuietly(session);}
	}

	private static void isiTeks(PreparedStatement ps,int i,JSONObject r,String k)throws Exception{String v=r.optString(k,"").trim();ps.setString(i,v.length()==0?null:v);}
	private static void isiWaktu(PreparedStatement ps,int i,JSONObject r,String k)throws Exception{String v=r.optString(k,"").trim();if(v.length()==0){ps.setNull(i,java.sql.Types.TIMESTAMP);return;}try{ps.setTimestamp(i,Timestamp.valueOf(v.length()==16?v+":00":v));}catch(Exception e){ps.setNull(i,java.sql.Types.TIMESTAMP);}}

	public static void ubahStatus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String tujuanStatus=request.optString("statusDokumen","").trim().toUpperCase();
		String aksi=aksiUntukStatus(tujuanStatus);
		Session session=HibernateUtil.getSessionFactory().openSession();PreparedStatement ps=null;ResultSet rs=null;
		try{
			EbisnisActorContextResolver.ActorContext ctx=aktor(session,tbmuser,request,hasil,aksi);if(ctx==null)return;
			Connection conn=session.connection();pastikanTabel(conn);conn.setAutoCommit(false);
			ps=conn.prepareStatement("SELECT status FROM inventory_distribution.distribution_document WHERE id=? AND toko_id=? AND document_type=? FOR UPDATE");ps.setLong(1,request.optLong("id",0));ps.setLong(2,tokoId(ctx,request));ps.setString(3,jenis(request));rs=ps.executeQuery();if(!rs.next()){tolak(hasil,"Dokumen tidak ditemukan.");conn.rollback();return;}String asal=rs.getString(1);tutup(rs);tutup(ps);rs=null;ps=null;
			if(!transisiBoleh(asal,tujuanStatus)){tolak(hasil,"Perubahan status "+asal+" ke "+tujuanStatus+" tidak diizinkan.");conn.rollback();return;}
			ps=conn.prepareStatement("UPDATE inventory_distribution.distribution_document SET status=?,updated_by=?,updated_at=now(),version=version+1 WHERE id=?");ps.setString(1,tujuanStatus);ps.setString(2,ctx.userId);ps.setLong(3,request.optLong("id",0));ps.executeUpdate();tutup(ps);ps=null;
			ps=conn.prepareStatement("INSERT INTO inventory_distribution.distribution_document_event(document_id,from_status,to_status,notes,actor_id) VALUES(?,?,?,?,?)");ps.setLong(1,request.optLong("id",0));ps.setString(2,asal);ps.setString(3,tujuanStatus);ps.setString(4,request.optString("catatanStatus",""));ps.setString(5,ctx.userId);ps.executeUpdate();conn.commit();hasil.put("status","success");hasil.put("message","Status dokumen diperbarui menjadi "+tujuanStatus+".");
		}catch(Exception e){try{session.connection().rollback();}catch(Exception ignored){ais.common.ErrorAuditUtil.record(ignored,"auto-audit DistribusiPengirimanApiHelper.ubahStatus.rollback");}throw e;}finally{tutup(rs);tutup(ps);HibernateUtil.closeSessionQuietly(session);}
	}

	private static boolean transisiBoleh(String a,String b){if(a==null||b==null||a.equals(b))return false;if("DRAFT".equals(a))return "SUBMITTED".equals(b)||"CANCELLED".equals(b);if("SUBMITTED".equals(a))return "APPROVED".equals(b)||"REJECTED".equals(b)||"CANCELLED".equals(b);if("APPROVED".equals(a))return "IN_PROGRESS".equals(b)||"COMPLETED".equals(b)||"CANCELLED".equals(b)||"REVERSED".equals(b);if("IN_PROGRESS".equals(a))return "COMPLETED".equals(b)||"CANCELLED".equals(b);if("REJECTED".equals(a))return "DRAFT".equals(b);if("COMPLETED".equals(a))return "REVERSED".equals(b);return false;}
}
