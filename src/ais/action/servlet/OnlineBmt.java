package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.ws.util.PembayaranGatewayHelper;
import ais.common.Common;
import ais.common.ErrorAuditUtil;
import ais.common.OnlineBmtUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.WaktuUtil;

/**
 * Endpoint merchant Online BMT yang kompatibel dengan kontrak pada paket
 * {@code ecampus_api.zip}.
 *
 * <h3>Batas kepercayaan</h3>
 * <p>Endpoint ini hanya menerima POST JSON berisi {@code API_KEY} dan
 * {@code DATA}. DATA wajib memakai envelope {@code v1.iv.ciphertext.hmac} yang
 * di-Base64, AES-256-CBC, dan HMAC-SHA256 seperti contoh BMT. API key, kunci AES,
 * dan kunci HMAC berasal dari konfigurasi server dengan default kosong. Fitur
 * sendiri default OFF; tidak ada secret contoh dari ZIP yang disalin ke source.</p>
 *
 * <h3>Idempotensi</h3>
 * <p>Validasi timestamp saja tidak cukup. Setiap nonce disimpan permanen dan
 * hanya boleh dipakai sekali. PAYMENT juga diserialisasi memakai PostgreSQL
 * advisory transaction lock berdasarkan {@code NO_TRANSAKSI_BMT}; ledger
 * transaksi menyimpan pasangan transaksi-invoice-nominal. Retry transaksi yang
 * sama mengembalikan sukses yang sama, sedangkan penggunaan nomor transaksi
 * untuk invoice/nominal berbeda ditolak. Saldo/tagihan baru diposting melalui
 * mesin kanonik {@link VirtualAccountBank}, bukan melalui SQL saldo langsung.</p>
 *
 * <h3>Aturan nominal</h3>
 * <p>NOMINAL harus sama dengan {@code total + biayaAdmin} invoice, dengan
 * toleransi satu sen untuk perbedaan representasi desimal. Invoice kedaluwarsa,
 * bukan milik kanal Online BMT, atau telah dibayar oleh transaksi lain tidak
 * dapat diposting ulang.</p>
 */
public class OnlineBmt extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Set<String> CHANNELS = new HashSet<String>(Arrays.asList(
			"TELLER", "MOBILE_NASABAH", "MOBILE_PETUGAS", "MOBILE_AGEN", "VIRTUAL_ACCOUNT"));

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		write(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				failure("405", "Method harus POST."));
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject result;
		int httpStatus = HttpServletResponse.SC_OK;
		try {
			result = process(request);
		} catch (ApiException e) {
			httpStatus = e.httpStatus;
			result = failure(e.code, e.getMessage());
		} catch (Exception e) {
			ErrorAuditUtil.record(e, "OnlineBmt.doPost");
			httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			result = failure("500", "Terjadi kesalahan internal saat memproses transaksi.");
		}
		write(response, httpStatus, result);
	}

	private JSONObject process(HttpServletRequest request) throws Exception {
		if (!OnlineBmtUtil.isGlobalEnabled()) {
			throw new ApiException(503, "503", "Kanal Online BMT belum diaktifkan.");
		}
		String apiKey = config(Konfigurasi.ONLINE_BMT_API_KEY);
		String encryptionKey = config(Konfigurasi.ONLINE_BMT_ENCRYPTION_KEY);
		String hmacKey = config(Konfigurasi.ONLINE_BMT_HMAC_KEY);
		if (apiKey.length() == 0 || encryptionKey.length() == 0 || hmacKey.length() == 0) {
			throw new ApiException(503, "503", "Konfigurasi keamanan Online BMT belum lengkap.");
		}

		JSONObject envelope = new JSONObject(readBody(request));
		String receivedApiKey = envelope.optString("API_KEY", "");
		if (!constantTimeEquals(apiKey, receivedApiKey)) {
			throw new ApiException(401, "401", "API_KEY tidak valid.");
		}
		String encrypted = envelope.optString("DATA", "").trim();
		if (encrypted.length() == 0) {
			throw new ApiException(400, "400", "Parameter DATA tidak ditemukan.");
		}
		JSONObject data = decrypt(encrypted, encryptionKey, hmacKey);
		validateFreshness(data);
		String type = required(data, "JENIS_REQUEST").toUpperCase();
		reserveNonce(required(data, "NONCE"), type);

		if ("INQUIRY".equals(type)) {
			return inquiry(data);
		}
		if ("PAYMENT".equals(type)) {
			return payment(data);
		}
		if ("CHECK_STATUS_PAYMENT".equals(type)) {
			return checkStatus(data);
		}
		throw new ApiException(404, "404", "JENIS_REQUEST tidak dikenali.");
	}

	private JSONObject inquiry(JSONObject data) throws Exception {
		String invoiceNo = required(data, "NO_INVOICE");
		VirtualAccountBank invoice = findInvoice(invoiceNo);
		validateInvoiceChannel(invoice);
		if (invoice.getKadaluarsaWaktu() != null && invoice.getKadaluarsaWaktu().before(WaktuUtil.getDate())
				&& !VirtualAccountBank.isSudahTerbayar(invoice)) {
			throw new ApiException(410, "01", "Invoice sudah kedaluwarsa.");
		}

		JSONObject value = new JSONObject();
		value.put("NO_INVOICE", invoiceNo);
		value.put("NAMA", invoice.getNamaPemilikRingkas());
		value.put("TGL", Common.databaseDateFormat.get().format(invoice.getTanggal_dirubah()));
		value.put("DESKRIPSI", invoice.getKeterangan() == null ? "Pembayaran eCampus" : invoice.getKeterangan());
		value.put("NOMINAL", OnlineBmtUtil.payableAmount(invoice));
		value.put("KD_MITRA_BMT", config("online_bmt_kode_mitra"));
		value.put("NM_MITRA_BMT", config("online_bmt_nama_mitra"));
		value.put("KD_MERCHANT", config("online_bmt_kode_merchant"));
		value.put("NM_MERCHANT", config("online_bmt_nama_merchant"));
		value.put("STATUS_TRANSAKSI", VirtualAccountBank.isSudahTerbayar(invoice) ? "00" : "01");
		return success("Request berhasil.", value);
	}

	private JSONObject payment(JSONObject data) throws Exception {
		PaymentInput input = PaymentInput.parse(data);
		validateChannel(input.channel);

		Session lockSession = null;
		Transaction lockTx = null;
		try {
			lockSession = HibernateUtil.openSession();
			lockTx = lockSession.beginTransaction();
			lockTransaction(lockSession, input.transactionNo);
			Ledger ledger = findLedger(lockSession, input.transactionNo);
			if (ledger != null && (!ledger.invoiceNo.equals(input.invoiceNo)
					|| !sameAmount(ledger.amount, input.amount))) {
				throw new ApiException(409, "01",
						"NO_TRANSAKSI_BMT sudah digunakan untuk invoice atau nominal lain.");
			}

			VirtualAccountBank invoice = findInvoice(input.invoiceNo);
			validateInvoiceChannel(invoice);
			validateAmount(invoice, input.amount);
			if (ledger != null && "SUCCESS".equals(ledger.status)) {
				lockTx.commit();
				return transactionResult("00", "Transaksi Berhasil (idempoten).");
			}
			if (VirtualAccountBank.isSudahTerbayar(invoice) && ledger == null) {
				throw new ApiException(409, "01", "Transaksi gagal. Tagihan sudah lunas.");
			}
			upsertProcessing(lockSession, input);

			postCanonicalPayment(invoice, input, data.toString());
			VirtualAccountBank refreshed = findInvoice(input.invoiceNo);
			if (!VirtualAccountBank.isSudahTerbayar(refreshed)) {
				updateLedger(lockSession, input.transactionNo, "FAILED", "96",
						"Posting pembayaran belum menghasilkan dokumen pembayaran.");
				lockTx.commit();
				throw new ApiException(500, "96", "Pembayaran belum dapat dibukukan. Silakan cek status sebelum mengulang.");
			}
			updateLedger(lockSession, input.transactionNo, "SUCCESS", "00", "Transaksi Berhasil");
			lockTx.commit();
			return transactionResult("00", "Transaksi Berhasil");
		} catch (Exception e) {
			if (lockTx != null && lockTx.isActive()) {
				try { lockTx.rollback(); } catch (Exception rollback) {
					ErrorAuditUtil.record(rollback, "OnlineBmt.payment.rollback");
				}
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(lockSession);
		}
	}

	private JSONObject checkStatus(JSONObject data) throws Exception {
		PaymentInput input = PaymentInput.parse(data);
		validateChannel(input.channel);
		VirtualAccountBank invoice = findInvoice(input.invoiceNo);
		validateInvoiceChannel(invoice);
		validateAmount(invoice, input.amount);
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Ledger ledger = findLedger(session, input.transactionNo);
			boolean paid = ledger != null && ledger.invoiceNo.equals(input.invoiceNo)
					&& sameAmount(ledger.amount, input.amount) && "SUCCESS".equals(ledger.status)
					&& VirtualAccountBank.isSudahTerbayar(invoice);
			return transactionResult(paid ? "00" : "01",
					paid ? "Tagihan Sudah Terbayar" : "Pembayaran belum terkonfirmasi");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private void postCanonicalPayment(VirtualAccountBank invoice, PaymentInput input, String rawData)
			throws Exception {
		Session postingSession = null;
		try {
			postingSession = HibernateUtil.openSession();
			Date paidAt = WaktuUtil.getDate();
			if (invoice.getTopup().doubleValue() > 0.1 && invoice.getCicilan().length() == 0) {
				VirtualAccountBank.bayarTopup(invoice, postingSession, paidAt, OnlineBmtUtil.BANK_NAME,
						false, rawData);
			} else if (invoice.getSiswa() != null || invoice.getCalonSiswa() != null) {
				VirtualAccountBank.bayarSiswa(invoice, postingSession, paidAt, OnlineBmtUtil.BANK_NAME,
						false, rawData, false);
			} else {
				PembayaranGatewayHelper.prosesRincianVA(postingSession, invoice, false,
						OnlineBmtUtil.BANK_NAME, null, paidAt, rawData, invoice.getMahasiswa(),
						invoice.getBiodataCalonMahasiswa(), invoice.getSemester(), invoice.getJenisKegiatan(),
						new JSONArray());
			}
		} finally {
			HibernateUtil.closeSessionQuietly(postingSession);
		}
	}

	private static VirtualAccountBank findInvoice(String invoiceNo) throws ApiException {
		VirtualAccountBank invoice = VirtualAccountBank.ambilVa(invoiceNo, null);
		if (invoice == null) {
			throw new ApiException(404, "01", "No Invoice tidak ditemukan.");
		}
		return invoice;
	}

	private static void validateInvoiceChannel(VirtualAccountBank invoice) throws ApiException {
		if (invoice == null || !OnlineBmtUtil.BANK_NAME.equalsIgnoreCase(invoice.getBank())) {
			throw new ApiException(404, "01", "Invoice bukan tagihan Online BMT.");
		}
		if (!OnlineBmtUtil.isPerguruanTinggiEnabled(invoice.getPt())) {
			throw new ApiException(403, "01", "Kanal Online BMT untuk pemilik invoice sedang dinonaktifkan.");
		}
	}

	private static void validateAmount(VirtualAccountBank invoice, BigDecimal amount) throws ApiException {
		if (!sameAmount(BigDecimal.valueOf(OnlineBmtUtil.payableAmount(invoice)), amount)) {
			throw new ApiException(422, "01", "Nominal pembayaran tidak sama dengan nominal invoice.");
		}
	}

	private static void validateChannel(String channel) throws ApiException {
		if (!CHANNELS.contains(channel)) {
			throw new ApiException(400, "400", "CHANNEL_BMT tidak dikenali.");
		}
	}

	private static void validateFreshness(JSONObject data) throws ApiException {
		long timestamp;
		try {
			timestamp = data.getLong("TIMESTAMP");
		} catch (Exception e) {
			throw new ApiException(400, "400", "TIMESTAMP tidak valid.");
		}
		long tolerance = 300L;
		try {
			tolerance = Long.parseLong(config("online_bmt_request_time_tolerance"));
		} catch (Exception ignore) {
			tolerance = 300L;
		}
		if (tolerance < 30L || tolerance > 3600L) tolerance = 300L;
		if (Math.abs((System.currentTimeMillis() / 1000L) - timestamp) > tolerance) {
			throw new ApiException(408, "408", "Request sudah kedaluwarsa.");
		}
	}

	private static void reserveNonce(String nonce, String type) throws Exception {
		if (nonce.length() > 200) throw new ApiException(400, "400", "NONCE terlalu panjang.");
		Session session = null;
		Transaction tx = null;
		PreparedStatement ps = null;
		try {
			session = HibernateUtil.openSession();
			tx = session.beginTransaction();
			ps = session.connection().prepareStatement(
					"INSERT INTO public.online_bmt_nonce(nonce, request_type) VALUES (?, ?)");
			ps.setString(1, nonce);
			ps.setString(2, type);
			ps.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			if (isUniqueViolation(e)) {
				throw new ApiException(409, "409", "NONCE sudah pernah digunakan.");
			}
			throw e;
		} finally {
			close(ps); HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static void lockTransaction(Session session, String transactionNo) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = session.connection().prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))");
			ps.setString(1, "online-bmt:" + transactionNo);
			ps.executeQuery().close();
		} finally { close(ps); }
	}

	private static Ledger findLedger(Session session, String transactionNo) throws SQLException {
		PreparedStatement ps = null; ResultSet rs = null;
		try {
			ps = session.connection().prepareStatement(
					"SELECT no_invoice, nominal, status FROM public.online_bmt_request_guard WHERE no_transaksi_bmt=? ORDER BY id DESC LIMIT 1");
			ps.setString(1, transactionNo); rs = ps.executeQuery();
			return rs.next() ? new Ledger(rs.getString(1), rs.getBigDecimal(2), rs.getString(3)) : null;
		} finally { close(rs); close(ps); }
	}

	private static void upsertProcessing(Session session, PaymentInput input) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = session.connection().prepareStatement(
					"INSERT INTO public.online_bmt_request_guard(nonce,request_type,no_invoice,no_transaksi_bmt,nominal,status) "
					+ "VALUES (?,?,?,?,?,'PROCESSING') ON CONFLICT (no_transaksi_bmt) DO UPDATE SET "
					+ "status='PROCESSING', updated_at=CURRENT_TIMESTAMP");
			ps.setString(1, input.nonce); ps.setString(2, "PAYMENT"); ps.setString(3, input.invoiceNo);
			ps.setString(4, input.transactionNo); ps.setBigDecimal(5, input.amount); ps.executeUpdate();
		} finally { close(ps); }
	}

	private static void updateLedger(Session session, String transactionNo, String status, String code,
			String message) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = session.connection().prepareStatement(
					"UPDATE public.online_bmt_request_guard SET status=?,response_code=?,response_message=?,updated_at=CURRENT_TIMESTAMP WHERE no_transaksi_bmt=?");
			ps.setString(1, status); ps.setString(2, code); ps.setString(3, message);
			ps.setString(4, transactionNo); ps.executeUpdate();
		} finally { close(ps); }
	}

	private static JSONObject decrypt(String encrypted, String encryptionKey, String hmacKey) throws ApiException {
		try {
			byte[] outer = Base64.decodeBase64(encrypted);
			String decoded = new String(outer, StandardCharsets.UTF_8);
			String[] parts = decoded.split("\\.", -1);
			if (parts.length != 4 || !"v1".equals(parts[0])) throw new Exception("format");
			String payload = parts[0] + "." + parts[1] + "." + parts[2];
			byte[] expected = hmac(payload, hmacKey);
			byte[] received = Base64.decodeBase64(parts[3]);
			if (!MessageDigest.isEqual(expected, received))
				throw new ApiException(400, "400", "Signature/HMAC tidak valid.");
			byte[] iv = Base64.decodeBase64(parts[1]);
			if (iv.length != 16) throw new Exception("iv");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha256(encryptionKey), "AES"),
					new IvParameterSpec(iv));
			return new JSONObject(new String(cipher.doFinal(Base64.decodeBase64(parts[2])), StandardCharsets.UTF_8));
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(400, "400", "DATA terenkripsi tidak valid.");
		}
	}

	private static String encrypt(JSONObject data) throws Exception {
		byte[] iv = new byte[16];
		new java.security.SecureRandom().nextBytes(iv);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE,
				new SecretKeySpec(sha256(config(Konfigurasi.ONLINE_BMT_ENCRYPTION_KEY)), "AES"),
				new IvParameterSpec(iv));
		String payload = "v1." + Base64.encodeBase64String(iv) + "."
				+ Base64.encodeBase64String(cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8)));
		return Base64.encodeBase64String((payload + "."
				+ Base64.encodeBase64String(hmac(payload, config(Konfigurasi.ONLINE_BMT_HMAC_KEY))))
				.getBytes(StandardCharsets.UTF_8));
	}

	private static byte[] hmac(String value, String key) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(sha256(key), "HmacSHA256"));
		return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
	}

	private static byte[] sha256(String value) throws Exception {
		return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
	}

	private static boolean constantTimeEquals(String expected, String actual) throws Exception {
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
				actual.getBytes(StandardCharsets.UTF_8));
	}

	private static JSONObject success(String message, JSONObject data) throws Exception {
		JSONObject result = new JSONObject(); result.put("STATUS", true); result.put("KODE_STATUS", "00");
		result.put("KETERANGAN", message);
		if (Common.bolehKonfigurasi("online_bmt_enkripsi_response", Konfigurasi.AKTIF)) result.put("DATA", encrypt(data));
		else result.put("DATA", data);
		return result;
	}

	private static JSONObject transactionResult(String code, String message) throws Exception {
		JSONObject data = new JSONObject(); data.put("STATUS_TRANSAKSI", code); data.put("DESKRIPSI_STATUS", message);
		return success("00".equals(code) ? "" : message, data);
	}

	private static JSONObject failure(String code, String message) {
		JSONObject result = new JSONObject(); result.put("STATUS", false); result.put("KODE_STATUS", code);
		result.put("KETERANGAN", message); result.put("DATA", new JSONObject()); return result;
	}

	private static String required(JSONObject data, String key) throws ApiException {
		String value = data.optString(key, "").trim();
		if (value.length() == 0) throw new ApiException(400, "400", key + " wajib diisi.");
		return value;
	}

	private static String config(String key) {
		return Common.getKonfigurasi(key, "").getNilai().trim();
	}

	private static boolean sameAmount(BigDecimal a, BigDecimal b) {
		return a != null && b != null && a.subtract(b).abs().compareTo(new BigDecimal("0.01")) <= 0;
	}

	private static boolean isUniqueViolation(Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof SQLException && "23505".equals(((SQLException) t).getSQLState())) return true;
		}
		return false;
	}

	private static String readBody(HttpServletRequest request) throws IOException, ApiException {
		StringBuilder body = new StringBuilder(); BufferedReader reader = request.getReader(); String line;
		while ((line = reader.readLine()) != null) {
			if (body.length() + line.length() > 1024 * 1024) throw new ApiException(413, "413", "Request terlalu besar.");
			body.append(line);
		}
		if (body.toString().trim().length() == 0) throw new ApiException(400, "400", "Request body kosong.");
		return body.toString();
	}

	private static void write(HttpServletResponse response, int status, JSONObject body) throws IOException {
		response.setStatus(status); response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setHeader("Cache-Control", "no-store");
		PrintWriter writer = response.getWriter(); writer.write(body.toString()); writer.flush();
	}

	private static void close(AutoCloseable value) {
		if (value != null) try { value.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "OnlineBmt.close"); }
	}

	private static final class PaymentInput {
		final String invoiceNo, transactionNo, channel, nonce; final BigDecimal amount;
		private PaymentInput(String invoiceNo, String transactionNo, String channel, String nonce, BigDecimal amount) {
			this.invoiceNo=invoiceNo; this.transactionNo=transactionNo; this.channel=channel; this.nonce=nonce; this.amount=amount;
		}
		static PaymentInput parse(JSONObject data) throws ApiException {
			try {
				return new PaymentInput(required(data,"NO_INVOICE"), required(data,"NO_TRANSAKSI_BMT"),
						required(data,"CHANNEL_BMT").toUpperCase(), required(data,"NONCE"),
						new BigDecimal(data.get("NOMINAL").toString()));
			} catch (ApiException e) { throw e; }
			catch (Exception e) { throw new ApiException(400,"400","NOMINAL tidak valid."); }
		}
	}

	private static final class Ledger {
		final String invoiceNo, status; final BigDecimal amount;
		Ledger(String invoiceNo, BigDecimal amount, String status) { this.invoiceNo=invoiceNo; this.amount=amount; this.status=status; }
	}

	private static final class ApiException extends Exception {
		private static final long serialVersionUID = 1L; final int httpStatus; final String code;
		ApiException(int httpStatus, String code, String message) { super(message); this.httpStatus=httpStatus; this.code=code; }
	}
}
