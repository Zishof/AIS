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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.ws.util.PembayaranGatewayHelper;
import ais.common.Common;
import ais.common.ErrorAuditUtil;
import ais.common.OnlineBmtUtil;
import ais.database.hibernate.HibernateUtil;
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
 * sendiri default OFF; tidak ada secret contoh dari ZIP yang disalin ke source.
 * Berdasarkan konfirmasi tertulis tim BMT, {@code DATA} pada setiap respons sukses
 * juga wajib dienkripsi dengan envelope yang sama. Kewajiban ini tidak dibuat
 * sebagai sakelar runtime agar perubahan konfigurasi tidak diam-diam memutus
 * kontrak SIT/UAT atau membocorkan isi respons sebagai JSON terbuka.</p>
 *
 * <h3>Idempotensi</h3>
 * <p>Validasi timestamp saja tidak cukup. Setiap nonce disimpan permanen dan
 * hanya boleh dipakai sekali. PAYMENT juga diserialisasi memakai PostgreSQL
 * advisory transaction lock berdasarkan {@code NO_TRANSAKSI_BMT}; ledger
 * transaksi menyimpan pasangan transaksi-invoice-nominal-channel. Retry transaksi yang
 * sama mengembalikan sukses yang sama, sedangkan penggunaan nomor transaksi
 * untuk invoice/nominal/channel berbeda ditolak. Lock memakai tingkat session supaya
 * tetap dipegang ketika ledger PROCESSING di-commit dan mesin posting membuka
 * transaksi terpisah. Urutan commit ini memungkinkan retry memulihkan SUCCESS
 * bila proses terputus setelah bukti pembayaran terbentuk, tanpa posting ganda.
 * Saldo/tagihan baru diposting melalui mesin kanonik {@link VirtualAccountBank},
 * bukan melalui SQL saldo langsung.</p>
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
		write(response, HttpServletResponse.SC_OK,
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
			/* Kontrak contoh BMT selalu memakai HTTP 200 dan menaruh hasil protokol
			 * pada STATUS/KODE_STATUS. Menjaga pola tersebut mencegah body error
			 * dibuang oleh klien/infrastruktur BMT yang hanya memproses respons 2xx. */
			httpStatus = HttpServletResponse.SC_OK;
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
		JSONObject envelope = parseEnvelope(readBody(request));
		String receivedApiKey = envelope.optString("API_KEY", "").trim();
		List<OnlineBmtUtil.Settings> candidates = OnlineBmtUtil.findCredentialCandidates(receivedApiKey);
		if (candidates.isEmpty()) {
			throw new ApiException(401, "401", "API_KEY tidak valid.");
		}
		String encrypted = envelope.optString("DATA", "").trim();
		if (encrypted.length() == 0) {
			throw new ApiException(400, "400", "Parameter DATA tidak ditemukan.");
		}
		JSONObject data = null;
		VirtualAccountBank invoice = null;
		OnlineBmtUtil.Settings settings = null;
		boolean decrypted = false;
		for (OnlineBmtUtil.Settings candidate : candidates) {
			try {
				JSONObject candidateData = decrypt(encrypted, candidate.getEncryptionKey(), candidate.getHmacKey());
				decrypted = true;
				String invoiceNo = requiredMax(candidateData, "NO_INVOICE", 255);
				VirtualAccountBank candidateInvoice = findInvoice(invoiceNo);
				validateInvoiceChannel(candidateInvoice);
				OnlineBmtUtil.Settings invoiceSettings = OnlineBmtUtil.resolveSettings(candidateInvoice);
				if (candidate.sameSecurity(invoiceSettings)) {
					data = candidateData;
					invoice = candidateInvoice;
					settings = invoiceSettings;
					break;
				}
			} catch (ApiException ignoredCandidate) {
				/* Semua kandidat dicoba tanpa membocorkan key/scope mana yang hampir cocok. */
			}
		}
		if (settings == null) {
			throw new ApiException(401, "401", decrypted
					? "Credential Online BMT tidak berlaku untuk pemilik invoice."
					: "DATA terenkripsi atau signature/HMAC tidak valid.");
		}
		validateFreshness(data, settings);
		String type = required(data, "JENIS_REQUEST").toUpperCase(Locale.ENGLISH);
		reserveNonce(required(data, "NONCE"), type);

		if ("INQUIRY".equals(type)) {
			return inquiry(data, invoice, settings);
		}
		if ("PAYMENT".equals(type)) {
			return payment(data, settings);
		}
		if ("CHECK_STATUS_PAYMENT".equals(type)) {
			return checkStatus(data, settings);
		}
		throw new ApiException(404, "404", "JENIS_REQUEST tidak dikenali.");
	}

	private JSONObject inquiry(JSONObject data, VirtualAccountBank invoice, OnlineBmtUtil.Settings settings) throws Exception {
		String invoiceNo = requiredMax(data, "NO_INVOICE", 255);
		validateNotExpired(invoice);
		if (VirtualAccountBank.isSudahTerbayar(invoice)) {
			throw new ApiException(409, "01", "Tagihan sudah lunas.");
		}
		MerchantIdentity merchantIdentity = requireMerchantIdentity(settings);

		JSONObject value = new JSONObject();
		value.put("NO_INVOICE", invoiceNo);
		value.put("NAMA", invoice.getNamaPemilikRingkas());
		Date invoiceDate = invoice.getTanggal_dirubah() == null ? WaktuUtil.getDate() : invoice.getTanggal_dirubah();
		value.put("TGL", Common.databaseDateFormat.get().format(invoiceDate));
		value.put("DESKRIPSI", invoice.getKeterangan() == null ? "Pembayaran eCampus" : invoice.getKeterangan());
		value.put("NOMINAL", OnlineBmtUtil.payableAmount(invoice));
		value.put("KD_MITRA_BMT", merchantIdentity.kodeMitra);
		value.put("NM_MITRA_BMT", merchantIdentity.namaMitra);
		value.put("KD_MERCHANT", merchantIdentity.kodeMerchant);
		value.put("NM_MERCHANT", merchantIdentity.namaMerchant);
		return success("Request berhasil.", value, settings);
	}

	/**
	 * Membaca identitas kontraktual yang dikembalikan kepada BMT saat inquiry.
	 * Keempat nilai sengaja diwajibkan bersama-sama: respons sukses dengan kode atau
	 * nama kosong akan membuat invoice tidak dapat dipetakan dengan andal di sisi
	 * BMT, walaupun autentikasi dan nominalnya benar. Karena itu konfigurasi setengah
	 * lengkap diperlakukan sebagai layanan belum siap, bukan sebagai inquiry sukses.
	 */
	private static MerchantIdentity requireMerchantIdentity(OnlineBmtUtil.Settings settings) throws ApiException {
		MerchantIdentity identity = new MerchantIdentity(settings.getKodeMitra(), settings.getNamaMitra(),
				settings.getKodeMerchant(), settings.getNamaMerchant());
		if (!identity.isComplete()) {
			throw new ApiException(503, "503",
					"Konfigurasi identitas mitra dan merchant Online BMT belum lengkap: "
							+ settings.describeMissingConfiguration() + ".");
		}
		return identity;
	}

	private JSONObject payment(JSONObject data, OnlineBmtUtil.Settings settings) throws Exception {
		PaymentInput input = PaymentInput.parse(data);
		validateChannel(input.channel);

		Session lockSession = null;
		Transaction ledgerTx = null;
		try {
			lockSession = HibernateUtil.openSession();
			lockTransaction(lockSession, input.transactionNo, true);
			ledgerTx = lockSession.beginTransaction();
			Ledger ledger = findLedger(lockSession, input.transactionNo);
			ledger = bindLegacyChannel(lockSession, ledger, input);
			if (ledger != null && (!input.invoiceNo.equals(ledger.invoiceNo)
					|| !sameAmount(ledger.amount, input.amount)
					|| !input.channel.equals(ledger.channel))) {
				throw new ApiException(409, "01",
						"NO_TRANSAKSI_BMT sudah digunakan untuk invoice, nominal, atau channel lain.");
			}

			VirtualAccountBank invoice = findInvoice(input.invoiceNo);
			validateInvoiceChannel(invoice);
			validateInvoiceCredentials(invoice, settings);
			validateNotExpired(invoice);
			validateAmount(invoice, input.amount);
			boolean invoicePaid = VirtualAccountBank.isSudahTerbayar(invoice);
			if (ledger != null && "SUCCESS".equals(ledger.status)) {
				if (!invoicePaid) {
					updateLedger(lockSession, input.transactionNo, "FAILED", "96",
							"Ledger SUCCESS tidak lagi mempunyai bukti pembayaran pada invoice");
					ledgerTx.commit();
					throw new ApiException(409, "96",
							"Status transaksi memerlukan rekonsiliasi karena bukti pembayaran tidak ditemukan.");
				}
				ledgerTx.commit();
				return transactionResult("00", "Transaksi Berhasil (idempoten).", settings);
			}
			if (invoicePaid) {
				if (ledger != null && "PROCESSING".equals(ledger.status)) {
					updateLedger(lockSession, input.transactionNo, "SUCCESS", "00",
							"Transaksi Berhasil (dipulihkan dari hasil posting invoice)");
					ledgerTx.commit();
					return transactionResult("00", "Transaksi Berhasil (status dipulihkan).", settings);
				}
				throw new ApiException(409, "01", "Transaksi gagal. Tagihan sudah lunas oleh transaksi lain.");
			}

			/* Ledger PROCESSING harus committed sebelum memanggil mesin pembayaran yang
			 * memakai session/transaction sendiri. Bila JVM berhenti setelah posting
			 * berhasil, retry dapat mengenali ledger ini dan memulihkan SUCCESS tanpa
			 * membukukan pembayaran untuk kedua kali. */
			upsertProcessing(lockSession, input);
			ledgerTx.commit();

			try {
				postCanonicalPayment(invoice, input, data.toString());
			} catch (Exception postingError) {
				VirtualAccountBank afterError = findInvoice(input.invoiceNo);
				validateInvoiceCredentials(afterError, settings);
				ledgerTx = lockSession.beginTransaction();
				if (VirtualAccountBank.isSudahTerbayar(afterError)) {
					updateLedger(lockSession, input.transactionNo, "SUCCESS", "00",
							"Transaksi Berhasil (posting selesai sebelum respons internal terputus)");
					ledgerTx.commit();
					return transactionResult("00", "Transaksi Berhasil (status dipulihkan).", settings);
				}
				updateLedger(lockSession, input.transactionNo, "FAILED", "96",
						"Posting pembayaran gagal: " + safeErrorMessage(postingError));
				ledgerTx.commit();
				throw postingError;
			}

			VirtualAccountBank refreshed = findInvoice(input.invoiceNo);
			validateInvoiceCredentials(refreshed, settings);
			ledgerTx = lockSession.beginTransaction();
			if (!VirtualAccountBank.isSudahTerbayar(refreshed)) {
				updateLedger(lockSession, input.transactionNo, "FAILED", "96",
						"Posting pembayaran belum menghasilkan dokumen pembayaran.");
				ledgerTx.commit();
				throw new ApiException(500, "96", "Pembayaran belum dapat dibukukan. Silakan cek status sebelum mengulang.");
			}
			updateLedger(lockSession, input.transactionNo, "SUCCESS", "00", "Transaksi Berhasil");
			ledgerTx.commit();
			return transactionResult("00", "Transaksi Berhasil", settings);
		} catch (Exception e) {
			if (ledgerTx != null && ledgerTx.isActive()) {
				try { ledgerTx.rollback(); } catch (Exception rollback) {
					ErrorAuditUtil.record(rollback, "OnlineBmt.payment.rollback");
				}
			}
			throw e;
		} finally {
			if (lockSession != null) {
				try { lockTransaction(lockSession, input.transactionNo, false); }
				catch (Exception unlock) { ErrorAuditUtil.record(unlock, "OnlineBmt.payment.unlock"); }
			}
			HibernateUtil.closeSessionQuietly(lockSession);
		}
	}

	private JSONObject checkStatus(JSONObject data, OnlineBmtUtil.Settings settings) throws Exception {
		PaymentInput input = PaymentInput.parse(data);
		validateChannel(input.channel);
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.openSession();
			lockTransaction(session, input.transactionNo, true);
			tx = session.beginTransaction();

			/* Invoice sengaja dibaca setelah advisory lock diperoleh. Jika PAYMENT
			 * dengan nomor transaksi yang sama sedang berjalan, CHECK_STATUS menunggu
			 * sampai proses itu selesai lalu membaca bukti pembayaran terbaru. Membaca
			 * sebelum lock menghasilkan object detached yang dapat tetap berstatus belum
			 * lunas walaupun PAYMENT sudah commit selama CHECK_STATUS menunggu. */
			VirtualAccountBank invoice = findInvoice(input.invoiceNo);
			validateInvoiceChannel(invoice);
			validateInvoiceCredentials(invoice, settings);
			validateAmount(invoice, input.amount);
			Ledger ledger = findLedger(session, input.transactionNo);
			ledger = bindLegacyChannel(session, ledger, input);
			boolean pairMatches = ledger != null && input.invoiceNo.equals(ledger.invoiceNo)
					&& sameAmount(ledger.amount, input.amount)
					&& input.channel.equals(ledger.channel);
			boolean invoicePaid = VirtualAccountBank.isSudahTerbayar(invoice);

			/* Bila proses sebelumnya berhenti sesudah bukti pembayaran tersimpan tetapi
			 * sebelum ledger SUCCESS, CHECK_STATUS harus dapat menyelesaikan rekonsiliasi.
			 * Hanya PROCESSING dengan pasangan transaksi-invoice-nominal-channel identik yang
			 * boleh dipulihkan; FAILED tetap memerlukan audit/retry PAYMENT kanonik. */
			if (pairMatches && invoicePaid && "PROCESSING".equals(ledger.status)) {
				updateLedger(session, input.transactionNo, "SUCCESS", "00",
						"Transaksi Berhasil (dipulihkan oleh pemeriksaan status)");
				ledger = new Ledger(ledger.invoiceNo, ledger.amount, ledger.channel, "SUCCESS");
			}
			boolean paid = pairMatches && invoicePaid && "SUCCESS".equals(ledger.status);
			tx.commit();
			return transactionResult(paid ? "00" : "01",
					paid ? "Tagihan Sudah Terbayar" : "Pembayaran belum terkonfirmasi", settings);
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try { tx.rollback(); } catch (Exception rollback) {
					ErrorAuditUtil.record(rollback, "OnlineBmt.checkStatus.rollback");
				}
			}
			throw e;
		} finally {
			if (session != null) {
				try { lockTransaction(session, input.transactionNo, false); }
				catch (Exception unlock) { ErrorAuditUtil.record(unlock, "OnlineBmt.checkStatus.unlock"); }
			}
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private void postCanonicalPayment(VirtualAccountBank invoice, PaymentInput input, String rawData)
			throws Exception {
		Session postingSession = null;
		try {
			postingSession = HibernateUtil.openSession();
			Date paidAt = WaktuUtil.getDate();
			if (invoice.getTopup() != null && invoice.getTopup().doubleValue() > 0.1
					&& (invoice.getCicilan() == null || invoice.getCicilan().length() == 0)) {
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
		/* Generator bank-online lama dapat tetap mengisi bankHost walaupun Online BMT
		 * bersifat inbound dan tidak mempunyai BankHost pemanggil. Pencarian pertama
		 * mempertahankan jalur kanonik; criterion cadangan mencari nomor yang sama
		 * tanpa membatasi host. Kepemilikan kanal tetap diverifikasi setelah lookup. */
		VirtualAccountBank invoice = VirtualAccountBank.ambilVa(invoiceNo, null, null,
				Restrictions.eq("kode", invoiceNo));
		if (invoice == null) {
			throw new ApiException(404, "01", "No Invoice tidak ditemukan.");
		}
		return invoice;
	}

	private static void validateInvoiceChannel(VirtualAccountBank invoice) throws ApiException {
		if (invoice == null || !OnlineBmtUtil.BANK_NAME.equalsIgnoreCase(invoice.getBank())) {
			throw new ApiException(404, "01", "Invoice bukan tagihan Online BMT.");
		}
		boolean enabled;
		if (invoice.getSiswa() != null || invoice.getCalonSiswa() != null) {
			ais.database.model.sekolah.Sekolah sekolah = invoice.getSiswa() != null
					? invoice.getSiswa().getSekolah() : invoice.getCalonSiswa().getSekolah();
			enabled = OnlineBmtUtil.isSekolahEnabled(sekolah, invoice.getKanalPembayaran());
		} else if (invoice.getAnggotaKoperasi() != null) {
			enabled = OnlineBmtUtil.isChannelEnabled(invoice.getKanalPembayaran());
		} else {
			enabled = OnlineBmtUtil.isPerguruanTinggiEnabled(invoice.getPt());
		}
		if (!enabled) {
			throw new ApiException(403, "01", "Kanal Online BMT untuk pemilik invoice sedang dinonaktifkan.");
		}
	}

	/**
	 * Otorisasi kedua setelah DATA berhasil dibuka. API key hanya memilih kandidat
	 * dekripsi; invoice menentukan tenant sebenarnya. Ketiga credential harus sama
	 * dengan konfigurasi efektif invoice agar credential sekolah/kanal A tidak dapat
	 * dipakai untuk inquiry atau pembayaran milik sekolah/kanal B.
	 */
	private static void validateInvoiceCredentials(VirtualAccountBank invoice,
			OnlineBmtUtil.Settings requestSettings) throws ApiException {
		OnlineBmtUtil.Settings current = OnlineBmtUtil.resolveSettings(invoice);
		if (!requestSettings.sameSecurity(current)) {
			throw new ApiException(401, "401", "Credential Online BMT tidak berlaku untuk pemilik invoice.");
		}
	}

	private static void validateAmount(VirtualAccountBank invoice, BigDecimal amount) throws ApiException {
		if (!sameAmount(BigDecimal.valueOf(OnlineBmtUtil.payableAmount(invoice)), amount)) {
			throw new ApiException(422, "01", "Nominal pembayaran tidak sama dengan nominal invoice.");
		}
	}

	private static void validateNotExpired(VirtualAccountBank invoice) throws ApiException {
		if (invoice.getKadaluarsaWaktu() != null && invoice.getKadaluarsaWaktu().before(WaktuUtil.getDate())
				&& !VirtualAccountBank.isSudahTerbayar(invoice)) {
			throw new ApiException(410, "01", "Invoice sudah kedaluwarsa.");
		}
	}

	private static void validateChannel(String channel) throws ApiException {
		if (!CHANNELS.contains(channel)) {
			throw new ApiException(400, "400", "CHANNEL_BMT tidak dikenali.");
		}
	}

	private static void validateFreshness(JSONObject data, OnlineBmtUtil.Settings settings) throws ApiException {
		long timestamp;
		try {
			timestamp = data.getLong("TIMESTAMP");
		} catch (Exception e) {
			throw new ApiException(400, "400", "TIMESTAMP tidak valid.");
		}
		long tolerance = settings.getRequestTimeTolerance();
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

	private static void lockTransaction(Session session, String transactionNo, boolean lock) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = session.connection().prepareStatement(lock
					? "SELECT pg_advisory_lock(hashtext(?))"
					: "SELECT pg_advisory_unlock(hashtext(?))");
			ps.setString(1, "online-bmt:" + transactionNo);
			ps.executeQuery().close();
		} finally { close(ps); }
	}

	private static String safeErrorMessage(Exception error) {
		String message = error == null ? null : error.getMessage();
		if (message == null || message.trim().length() == 0) {
			return error == null ? "kesalahan internal" : error.getClass().getSimpleName();
		}
		message = message.replace('\r', ' ').replace('\n', ' ').trim();
		return message.length() > 500 ? message.substring(0, 500) : message;
	}

	private static Ledger findLedger(Session session, String transactionNo) throws SQLException {
		PreparedStatement ps = null; ResultSet rs = null;
		try {
			ps = session.connection().prepareStatement(
					"SELECT no_invoice, nominal, channel_bmt, status FROM public.online_bmt_request_guard WHERE no_transaksi_bmt=? ORDER BY id DESC LIMIT 1");
			ps.setString(1, transactionNo); rs = ps.executeQuery();
			return rs.next() ? new Ledger(rs.getString(1), rs.getBigDecimal(2), rs.getString(3), rs.getString(4)) : null;
		} finally { close(rs); close(ps); }
	}

	private static void upsertProcessing(Session session, PaymentInput input) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = session.connection().prepareStatement(
					"INSERT INTO public.online_bmt_request_guard(nonce,request_type,no_invoice,no_transaksi_bmt,nominal,channel_bmt,status) "
					+ "VALUES (?,?,?,?,?,?,'PROCESSING') ON CONFLICT (no_transaksi_bmt) DO UPDATE SET "
					+ "status='PROCESSING', updated_at=CURRENT_TIMESTAMP");
			ps.setString(1, input.nonce); ps.setString(2, "PAYMENT"); ps.setString(3, input.invoiceNo);
			ps.setString(4, input.transactionNo); ps.setBigDecimal(5, input.amount);
			ps.setString(6, input.channel); ps.executeUpdate();
		} finally { close(ps); }
	}

	/**
	 * Mengikat channel pada ledger yang dibuat oleh versi aplikasi sebelum kolom
	 * {@code channel_bmt} tersedia. Pengikatan hanya boleh terjadi di bawah advisory
	 * lock dan setelah invoice serta nominal cocok; request telah lolos API key, HMAC,
	 * timestamp, nonce, dan whitelist channel. Setelah sekali terisi, channel tidak
	 * dapat diganti oleh retry berikutnya.
	 */
	private static Ledger bindLegacyChannel(Session session, Ledger ledger, PaymentInput input)
			throws SQLException {
		if (ledger == null || ledger.channel != null || !input.invoiceNo.equals(ledger.invoiceNo)
				|| !sameAmount(ledger.amount, input.amount)) {
			return ledger;
		}
		PreparedStatement ps = null;
		try {
			ps = session.connection().prepareStatement(
					"UPDATE public.online_bmt_request_guard SET channel_bmt=?,updated_at=CURRENT_TIMESTAMP "
					+ "WHERE no_transaksi_bmt=? AND channel_bmt IS NULL");
			ps.setString(1, input.channel);
			ps.setString(2, input.transactionNo);
			ps.executeUpdate();
			return new Ledger(ledger.invoiceNo, ledger.amount, input.channel, ledger.status);
		} finally {
			close(ps);
		}
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
			byte[] outer = decodeBase64(encrypted);
			String decoded = new String(outer, StandardCharsets.UTF_8);
			String[] parts = decoded.split("\\.", -1);
			if (parts.length != 4 || !"v1".equals(parts[0])) throw new Exception("format");
			String payload = parts[0] + "." + parts[1] + "." + parts[2];
			byte[] expected = hmac(payload, hmacKey);
			byte[] received = decodeBase64(parts[3]);
			if (received.length != 32 || !MessageDigest.isEqual(expected, received))
				throw new ApiException(400, "400", "Signature/HMAC tidak valid.");
			byte[] iv = decodeBase64(parts[1]);
			if (iv.length != 16) throw new Exception("iv");
			byte[] ciphertext = decodeBase64(parts[2]);
			if (ciphertext.length == 0 || ciphertext.length % 16 != 0) throw new Exception("ciphertext");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha256(encryptionKey), "AES"),
					new IvParameterSpec(iv));
			return new JSONObject(new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8));
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(400, "400", "DATA terenkripsi tidak valid.");
		}
	}

	private static byte[] decodeBase64(String value) throws Exception {
		if (value == null || value.length() == 0 || !Base64.isBase64(value)) {
			throw new Exception("base64");
		}
		return Base64.decodeBase64(value);
	}

	private static String encrypt(JSONObject data, OnlineBmtUtil.Settings settings) throws Exception {
		byte[] iv = new byte[16];
		new java.security.SecureRandom().nextBytes(iv);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE,
				new SecretKeySpec(sha256(settings.getEncryptionKey()), "AES"),
				new IvParameterSpec(iv));
		String payload = "v1." + Base64.encodeBase64String(iv) + "."
				+ Base64.encodeBase64String(cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8)));
		return Base64.encodeBase64String((payload + "."
				+ Base64.encodeBase64String(hmac(payload, settings.getHmacKey())))
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

	private static JSONObject success(String message, JSONObject data, OnlineBmtUtil.Settings settings) throws Exception {
		JSONObject result = new JSONObject(); result.put("STATUS", true); result.put("KODE_STATUS", "00");
		result.put("KETERANGAN", message);
		/* Konfirmasi BMT tanggal 3 September 2026: parameter DATA pada response
		 * wajib dienkripsi. Jangan kembalikan JSONObject plaintext walaupun konfigurasi
		 * legacy online_bmt_enkripsi_response pernah disetel tidak aktif. */
		result.put("DATA", encrypt(data, settings));
		return result;
	}

	private static JSONObject transactionResult(String code, String message, OnlineBmtUtil.Settings settings) throws Exception {
		JSONObject data = new JSONObject(); data.put("STATUS_TRANSAKSI", code); data.put("DESKRIPSI_STATUS", message);
		return success("00".equals(code) ? "" : message, data, settings);
	}

	private static JSONObject failure(String code, String message) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("STATUS", Boolean.FALSE);
		result.put("KODE_STATUS", code);
		result.put("KETERANGAN", message);
		result.put("DATA", Collections.emptyMap());
		return new JSONObject(result);
	}

	private static String required(JSONObject data, String key) throws ApiException {
		String value = data.optString(key, "").trim();
		if (value.length() == 0) throw new ApiException(400, "400", key + " wajib diisi.");
		return value;
	}

	private static String requiredMax(JSONObject data, String key, int maxLength) throws ApiException {
		String value = required(data, key);
		if (value.length() > maxLength) {
			throw new ApiException(400, "400", key + " terlalu panjang.");
		}
		return value;
	}

	private static JSONObject parseEnvelope(String body) throws ApiException {
		try {
			return new JSONObject(body);
		} catch (Exception e) {
			throw new ApiException(400, "400", "Format JSON request tidak valid.");
		}
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
		response.setStatus(status);
		response.setContentType("application/json; charset=UTF-8");
		response.setHeader("Cache-Control", "no-store");
		PrintWriter writer = response.getWriter(); writer.write(body.toString()); writer.flush();
	}

	private static void close(AutoCloseable value) {
		if (value != null) try { value.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "OnlineBmt.close"); }
	}

	private static final class MerchantIdentity {
		final String kodeMitra, namaMitra, kodeMerchant, namaMerchant;

		MerchantIdentity(String kodeMitra, String namaMitra, String kodeMerchant, String namaMerchant) {
			this.kodeMitra = kodeMitra;
			this.namaMitra = namaMitra;
			this.kodeMerchant = kodeMerchant;
			this.namaMerchant = namaMerchant;
		}

		boolean isComplete() {
			return kodeMitra.length() > 0 && namaMitra.length() > 0
					&& kodeMerchant.length() > 0 && namaMerchant.length() > 0;
		}
	}

	private static final class PaymentInput {
		final String invoiceNo, transactionNo, channel, nonce; final BigDecimal amount;
		private PaymentInput(String invoiceNo, String transactionNo, String channel, String nonce, BigDecimal amount) {
			this.invoiceNo=invoiceNo; this.transactionNo=transactionNo; this.channel=channel; this.nonce=nonce; this.amount=amount;
		}
		static PaymentInput parse(JSONObject data) throws ApiException {
			try {
				BigDecimal amount = new BigDecimal(data.get("NOMINAL").toString());
				if (amount.signum() <= 0) throw new ApiException(400, "400", "NOMINAL harus lebih besar dari nol.");
				return new PaymentInput(requiredMax(data,"NO_INVOICE",255),
						requiredMax(data,"NO_TRANSAKSI_BMT",255),
						requiredMax(data,"CHANNEL_BMT",30).toUpperCase(Locale.ENGLISH), required(data,"NONCE"), amount);
			} catch (ApiException e) { throw e; }
			catch (Exception e) { throw new ApiException(400,"400","NOMINAL tidak valid."); }
		}
	}

	private static final class Ledger {
		final String invoiceNo, channel, status; final BigDecimal amount;
		Ledger(String invoiceNo, BigDecimal amount, String channel, String status) {
			this.invoiceNo=invoiceNo; this.amount=amount; this.channel=channel; this.status=status;
		}
	}

	private static final class ApiException extends Exception {
		private static final long serialVersionUID = 1L; final int httpStatus; final String code;
		ApiException(int httpStatus, String code, String message) { super(message); this.httpStatus=httpStatus; this.code=code; }
	}
}
