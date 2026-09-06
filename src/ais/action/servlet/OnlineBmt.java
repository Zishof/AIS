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
 *
 * <h3>Perbandingan dengan gerbang pembayaran AIS lain</h3>
 * <p>Perlu dicatat karena berbeda tajam dari kebanyakan servlet sekelasnya di paket ini:
 * kelas ini <b>benar-benar memeriksa</b> bahan autentikasi pada pesan masuk, dan
 * memeriksanya <b>sebelum</b> percabangan jenis transaksi. Urutan pada {@link #process}
 * adalah sakelar fitur, API key, dekripsi AES beserta pemeriksaan HMAC, pengikatan
 * kredensial ke pemilik invoice, kesegaran waktu, lalu pemesanan nonce &mdash; baru
 * sesudah itu {@code INQUIRY}, {@code PAYMENT}, atau {@code CHECK_STATUS_PAYMENT}
 * dijalankan. Karena itu ketiga cabang transaksi terlindungi oleh gerbang yang sama, dan
 * pola "tanda tangan hanya diperiksa saat penerbitan token" yang muncul di gerbang lain
 * tidak berlaku di sini. Kelas ini layak dipakai sebagai contoh acuan.</p>
 *
 * @see ais.common.OnlineBmtUtil
 * @see ais.database.model.VirtualAccountBank
 */
public class OnlineBmt extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Daftar putih nilai {@code CHANNEL_BMT} yang diterima.
	 *
	 * <p>Kanal ikut menjadi bagian identitas transaksi pada ledger, sehingga nomor transaksi
	 * yang sama tidak dapat dipakai ulang dengan kanal berbeda. Nilai dibandingkan setelah
	 * dihuruf-besarkan oleh {@link PaymentInput#parse(JSONObject)}.</p>
	 */
	private static final Set<String> CHANNELS = new HashSet<String>(Arrays.asList(
			"TELLER", "MOBILE_NASABAH", "MOBILE_PETUGAS", "MOBILE_AGEN", "VIRTUAL_ACCOUNT"));

	/**
	 * Menolak permintaan GET.
	 *
	 * <p>Kontrak Online BMT hanya mengenal POST. Penolakan tetap dikirim sebagai HTTP 200
	 * dengan {@code KODE_STATUS} bernilai {@code "405"}, mengikuti pola balasan kelas ini
	 * yang selalu menaruh hasil protokol di dalam badan JSON.</p>
	 *
	 * @param request  permintaan masuk; isinya tidak dibaca
	 * @param response balasan yang akan diisi JSON penolakan
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		write(response, HttpServletResponse.SC_OK,
				failure("405", "Method harus POST."));
	}

	/**
	 * Titik masuk tunggal kanal Online BMT: menjalankan {@link #process} dan membungkus
	 * hasilnya menjadi balasan JSON.
	 *
	 * <p>{@link ApiException} dijawab HTTP 200 dengan kode protokol pada
	 * {@code KODE_STATUS} &mdash; kontrak contoh BMT selalu memakai HTTP 200 dan menaruh hasil
	 * protokol pada {@code STATUS}/{@code KODE_STATUS}, sehingga menjaga pola tersebut mencegah
	 * badan galat dibuang oleh klien atau infrastruktur BMT yang hanya memproses respons 2xx.
	 * Perhatikan bahwa medan {@code httpStatus} pada {@link ApiException} karena itu tidak
	 * dipakai di sini; ia hanya menyimpan niat semula. Kegagalan tak terduga dicatat ke audit
	 * lalu dijawab HTTP 500 berkode {@code "500"} dengan pesan umum, tanpa membocorkan rincian
	 * internal.</p>
	 *
	 * @param request  permintaan masuk berisi amplop {@code API_KEY} dan {@code DATA}
	 * @param response balasan yang akan diisi JSON hasil
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 */
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

	/**
	 * Memeriksa seluruh bahan autentikasi, lalu mengarahkan ke salah satu dari tiga jenis
	 * transaksi.
	 *
	 * <h4>Urutan gerbang &mdash; seluruhnya dijalankan sebelum percabangan</h4>
	 * <ol>
	 *   <li>sakelar fitur {@code OnlineBmtUtil.isGlobalEnabled()}; bila mati dijawab 503;</li>
	 *   <li>amplop diurai, {@code API_KEY} dicocokkan menjadi daftar kandidat kredensial;
	 *       daftar kosong dijawab 401;</li>
	 *   <li>tiap kandidat dicoba untuk mendekripsi {@code DATA} lewat {@link #decrypt}, yang
	 *       sekaligus memeriksa HMAC. Kandidat yang berhasil masih harus lolos syarat berikut:
	 *       {@code NO_INVOICE} di dalamnya menunjuk invoice yang ada, invoice itu memang milik
	 *       kanal Online BMT, dan kredensial kandidat sama dengan kredensial efektif pemilik
	 *       invoice. Barulah kandidat itu dipakai;</li>
	 *   <li>bila tidak ada kandidat yang lolos, dijawab 401 dengan pesan yang membedakan
	 *       "gagal didekripsi" dari "berhasil didekripsi tetapi bukan pemilik invoice";</li>
	 *   <li>{@link #validateFreshness} menolak permintaan yang stempel waktunya di luar
	 *       toleransi;</li>
	 *   <li>{@link #reserveNonce} memesan {@code NONCE} secara permanen sehingga pesan yang
	 *       sama tidak dapat diputar ulang.</li>
	 * </ol>
	 *
	 * <p>Percabangan kandidat sengaja menelan {@link ApiException} tiap kandidat tanpa
	 * membocorkan kunci atau lingkup mana yang hampir cocok.</p>
	 *
	 * <p>Setelah semua gerbang di atas, {@code JENIS_REQUEST} menentukan tujuan:
	 * {@code INQUIRY} ke {@link #inquiry}, {@code PAYMENT} ke {@link #payment}, dan
	 * {@code CHECK_STATUS_PAYMENT} ke {@link #checkStatus}. Nilai lain dijawab 404.</p>
	 *
	 * @param request permintaan masuk berisi amplop {@code API_KEY} dan {@code DATA}
	 * @return badan balasan JSON yang sudah lengkap
	 * @throws ApiException bila salah satu gerbang menolak permintaan
	 * @throws Exception    bila terjadi kegagalan tak terduga
	 */
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

	/**
	 * Menjawab permintaan {@code INQUIRY}: memberitahukan rincian tagihan di balik satu nomor
	 * invoice.
	 *
	 * <p>Invoice sudah ditemukan dan diverifikasi kepemilikan kanalnya oleh {@link #process};
	 * di sini tinggal diperiksa bahwa ia belum kedaluwarsa dan belum lunas. Tagihan yang sudah
	 * lunas dijawab {@link ApiException} berkode protokol {@code "01"}.</p>
	 *
	 * <p>Balasan memuat {@code NO_INVOICE}, {@code NAMA} penagih, {@code TGL}, {@code DESKRIPSI}
	 * (bawaan {@code "Pembayaran eCampus"} bila keterangan kosong), {@code NOMINAL}, serta
	 * identitas mitra dan merchant dari {@link #requireMerchantIdentity}. Seluruhnya dienkripsi
	 * ulang oleh {@link #success}.</p>
	 *
	 * @param invoice  invoice yang sudah ditemukan dan diverifikasi kanalnya
	 * @param data     isi permintaan yang sudah didekripsi
	 * @param settings kredensial efektif pemilik invoice
	 * @return badan balasan JSON dengan {@code DATA} terenkripsi
	 * @throws ApiException bila invoice kedaluwarsa, sudah lunas, atau identitas mitra belum
	 *                      lengkap
	 * @throws Exception    bila enkripsi balasan gagal
	 */
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
	 *
	 * <p>Keempat nilai sengaja diwajibkan bersama-sama: respons sukses dengan kode atau nama
	 * kosong akan membuat invoice tidak dapat dipetakan dengan andal di sisi BMT, walaupun
	 * autentikasi dan nominalnya benar. Karena itu konfigurasi setengah lengkap diperlakukan
	 * sebagai layanan belum siap (503), bukan sebagai inquiry sukses.</p>
	 *
	 * @param settings kredensial efektif pemilik invoice
	 * @return identitas mitra dan merchant yang sudah dipastikan lengkap
	 * @throws ApiException bila salah satu dari keempat nilai masih kosong
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

	/**
	 * Menjalankan permintaan {@code PAYMENT}: membukukan pembayaran satu invoice, sekali saja.
	 *
	 * <h4>Penguncian dan ledger</h4>
	 * <p>Seluruh proses berjalan di bawah <i>advisory lock</i> PostgreSQL bertingkat session
	 * yang dikunci pada {@code NO_TRANSAKSI_BMT} (lihat {@link #lockTransaction}), sehingga dua
	 * permintaan bernomor transaksi sama tidak pernah berjalan bersamaan. Ledger
	 * {@code online_bmt_request_guard} menyimpan pasangan transaksi-invoice-nominal-channel;
	 * nomor transaksi yang dipakai ulang untuk pasangan berbeda ditolak dengan kode protokol
	 * {@code "01"}.</p>
	 *
	 * <h4>Jalur pemulihan</h4>
	 * <ul>
	 *   <li>Ledger {@code SUCCESS} dan invoice memang lunas &rarr; dijawab sukses idempoten
	 *       tanpa membukukan ulang.</li>
	 *   <li>Ledger {@code SUCCESS} tetapi bukti pembayaran pada invoice hilang &rarr; ledger
	 *       diturunkan menjadi {@code FAILED} berkode {@code "96"} dan permintaan ditolak
	 *       karena memerlukan rekonsiliasi.</li>
	 *   <li>Invoice sudah lunas dan ledger masih {@code PROCESSING} &rarr; ledger dipulihkan
	 *       menjadi {@code SUCCESS}; inilah yang menyelamatkan proses yang terputus setelah
	 *       posting berhasil.</li>
	 *   <li>Invoice sudah lunas tanpa ledger yang cocok &rarr; ditolak karena dibayar transaksi
	 *       lain.</li>
	 * </ul>
	 *
	 * <p>Ledger {@code PROCESSING} sengaja di-<i>commit</i> <b>sebelum</b> mesin pembayaran
	 * dipanggil, karena mesin itu memakai session dan transaksi sendiri. Bila JVM berhenti
	 * setelah posting berhasil, percobaan ulang dapat mengenali ledger ini dan memulihkan
	 * {@code SUCCESS} tanpa membukukan pembayaran untuk kedua kali. Kegagalan posting pun
	 * diperiksa ulang terhadap keadaan invoice yang sebenarnya sebelum dinyatakan gagal.</p>
	 *
	 * <p>Kunci selalu dilepas di blok {@code finally}.</p>
	 *
	 * @param data     isi permintaan yang sudah didekripsi dan lolos seluruh gerbang
	 * @param settings kredensial efektif pemilik invoice
	 * @return badan balasan JSON berisi status transaksi terenkripsi
	 * @throws ApiException bila pasangan transaksi bentrok, nominal tidak cocok, invoice
	 *                      kedaluwarsa, atau pembayaran tidak dapat dibukukan
	 * @throws Exception    bila posting atau enkripsi balasan gagal
	 */
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

	/**
	 * Menjawab permintaan {@code CHECK_STATUS_PAYMENT}: melaporkan apakah satu transaksi sudah
	 * terbayar.
	 *
	 * <p>Sama seperti {@link #payment}, pemeriksaan berjalan di bawah <i>advisory lock</i> atas
	 * {@code NO_TRANSAKSI_BMT}. Invoice sengaja dibaca <b>setelah</b> kunci diperoleh: jika
	 * sebuah {@code PAYMENT} bernomor transaksi sama sedang berjalan, pemeriksaan status
	 * menunggu sampai proses itu selesai lalu membaca bukti pembayaran terbaru. Membaca sebelum
	 * mengunci menghasilkan objek <i>detached</i> yang dapat tetap tampak belum lunas walaupun
	 * {@code PAYMENT} sudah di-<i>commit</i> selama pemeriksaan menunggu.</p>
	 *
	 * <p>Transaksi dinyatakan terbayar hanya bila pasangan transaksi-invoice-nominal-channel
	 * pada ledger cocok, invoice memang lunas, dan status ledger {@code SUCCESS}. Ledger
	 * {@code PROCESSING} yang pasangannya cocok dan invoice-nya sudah lunas dipulihkan menjadi
	 * {@code SUCCESS} di sini &mdash; menyelesaikan rekonsiliasi bila proses sebelumnya berhenti
	 * sesudah bukti pembayaran tersimpan tetapi sebelum ledger dimutakhirkan. Ledger
	 * {@code FAILED} sengaja tidak ikut dipulihkan; keadaan itu tetap memerlukan audit atau
	 * pengulangan {@code PAYMENT} kanonik.</p>
	 *
	 * <p>Balasan berkode {@code "00"} bila terbayar dan {@code "01"} bila belum. Kunci selalu
	 * dilepas di blok {@code finally}.</p>
	 *
	 * @param data     isi permintaan yang sudah didekripsi dan lolos seluruh gerbang
	 * @param settings kredensial efektif pemilik invoice
	 * @return badan balasan JSON berisi status transaksi terenkripsi
	 * @throws ApiException bila invoice tidak ditemukan, bukan milik kanal ini, kredensial tidak
	 *                      berlaku, atau nominal tidak cocok
	 * @throws Exception    bila enkripsi balasan gagal
	 */
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

	/**
	 * Membukukan pembayaran lewat mesin posting kanonik AIS, bukan lewat SQL saldo langsung.
	 *
	 * <p>Mesin yang dipilih bergantung pada bentuk invoice:</p>
	 * <ul>
	 *   <li>invoice ber-{@code topup} lebih dari 0,1 dan tanpa daftar cicilan &rarr;
	 *       {@code VirtualAccountBank.bayarTopup};</li>
	 *   <li>invoice milik siswa atau calon siswa &rarr; {@code VirtualAccountBank.bayarSiswa};</li>
	 *   <li>selebihnya (mahasiswa, calon mahasiswa, koperasi) &rarr;
	 *       {@code PembayaranGatewayHelper.prosesRincianVA}.</li>
	 * </ul>
	 *
	 * <p>Session posting dibuka sendiri dan selalu ditutup di blok {@code finally}. Session itu
	 * sengaja terpisah dari session pemegang kunci pada {@link #payment}, dan itulah alasan
	 * kunci di sana dipasang pada tingkat session, bukan tingkat transaksi.</p>
	 *
	 * @param invoice invoice yang akan dilunasi
	 * @param input   rincian permintaan pembayaran
	 * @param rawData isi permintaan yang sudah didekripsi, disimpan sebagai jejak pada dokumen
	 *                pembayaran
	 * @throws Exception bila mesin posting gagal
	 */
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

	/**
	 * Mencari invoice berdasarkan nomornya.
	 *
	 * <p>Generator bank-online lama dapat tetap mengisi {@code bankHost} walaupun Online BMT
	 * bersifat <i>inbound</i> dan tidak mempunyai {@code BankHost} pemanggil. Karena itu
	 * pencarian pertama mempertahankan jalur kanonik, sementara kriteria cadangan mencari nomor
	 * yang sama tanpa membatasi host. Kepemilikan kanal tetap diverifikasi setelah pencarian
	 * lewat {@link #validateInvoiceChannel} dan {@link #validateInvoiceCredentials}.</p>
	 *
	 * @param invoiceNo nomor invoice dari permintaan
	 * @return invoice yang ditemukan; tidak pernah {@code null}
	 * @throws ApiException bila tidak ada invoice bernomor tersebut
	 */
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

	/**
	 * Memastikan invoice memang milik kanal Online BMT dan kanal itu sedang aktif bagi
	 * pemiliknya.
	 *
	 * <p>Pemeriksaan berjalan dua tingkat. Pertama, kolom {@code bank} pada invoice harus sama
	 * dengan {@code OnlineBmtUtil.BANK_NAME}. Kedua, sakelar aktif dibaca sesuai jenis pemilik:
	 * sekolah untuk invoice siswa atau calon siswa, sakelar kanal untuk anggota koperasi, dan
	 * sakelar perguruan tinggi untuk selebihnya.</p>
	 *
	 * <p>Bersifat <i>fail-closed</i>: invoice {@code null} pun ditolak.</p>
	 *
	 * @param invoice invoice yang diperiksa
	 * @throws ApiException bila invoice bukan milik kanal ini (404) atau kanal sedang
	 *                      dinonaktifkan bagi pemiliknya (403)
	 */
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
	 * Otorisasi kedua setelah {@code DATA} berhasil dibuka.
	 *
	 * <p>API key hanya memilih kandidat dekripsi; <b>invoice</b>-lah yang menentukan tenant
	 * sebenarnya. Ketiga kredensial harus sama dengan konfigurasi efektif invoice agar
	 * kredensial sekolah atau kanal A tidak dapat dipakai untuk inquiry maupun pembayaran milik
	 * sekolah atau kanal B.</p>
	 *
	 * <p>Pemeriksaan ini sengaja diulang setelah setiap pembacaan ulang invoice di
	 * {@link #payment}, sehingga pergantian konfigurasi di tengah proses tidak dapat dipakai
	 * menyelundupkan pembayaran lintas tenant.</p>
	 *
	 * @param invoice         invoice yang menentukan tenant sebenarnya
	 * @param requestSettings kredensial yang dipakai membuka permintaan
	 * @throws ApiException bila kredensial permintaan bukan milik pemilik invoice
	 */
	private static void validateInvoiceCredentials(VirtualAccountBank invoice,
			OnlineBmtUtil.Settings requestSettings) throws ApiException {
		OnlineBmtUtil.Settings current = OnlineBmtUtil.resolveSettings(invoice);
		if (!requestSettings.sameSecurity(current)) {
			throw new ApiException(401, "401", "Credential Online BMT tidak berlaku untuk pemilik invoice.");
		}
	}

	/**
	 * Memastikan nominal permintaan sama dengan nominal yang harus dibayar pada invoice.
	 *
	 * <p>Nilai pembanding adalah {@code OnlineBmtUtil.payableAmount(invoice)}, yaitu total
	 * ditambah biaya administrasi. Perbandingan memakai {@link #sameAmount} sehingga selisih
	 * sampai satu sen dimaafkan &mdash; menampung perbedaan representasi desimal antar sistem.</p>
	 *
	 * @param invoice invoice yang menjadi acuan
	 * @param amount  nominal yang dilaporkan pemanggil
	 * @throws ApiException bila selisihnya melampaui satu sen
	 */
	private static void validateAmount(VirtualAccountBank invoice, BigDecimal amount) throws ApiException {
		if (!sameAmount(BigDecimal.valueOf(OnlineBmtUtil.payableAmount(invoice)), amount)) {
			throw new ApiException(422, "01", "Nominal pembayaran tidak sama dengan nominal invoice.");
		}
	}

	/**
	 * Menolak invoice yang sudah melewati waktu kedaluwarsa.
	 *
	 * <p>Invoice yang <b>sudah lunas</b> sengaja dikecualikan: setelah dibayar, lewatnya masa
	 * berlaku tidak lagi relevan dan pemeriksaan status atas transaksi lama harus tetap bisa
	 * dijawab. Invoice tanpa {@code kadaluarsaWaktu} dianggap tidak pernah kedaluwarsa.</p>
	 *
	 * @param invoice invoice yang diperiksa
	 * @throws ApiException bila invoice kedaluwarsa dan belum lunas
	 */
	private static void validateNotExpired(VirtualAccountBank invoice) throws ApiException {
		if (invoice.getKadaluarsaWaktu() != null && invoice.getKadaluarsaWaktu().before(WaktuUtil.getDate())
				&& !VirtualAccountBank.isSudahTerbayar(invoice)) {
			throw new ApiException(410, "01", "Invoice sudah kedaluwarsa.");
		}
	}

	/**
	 * Memastikan {@code CHANNEL_BMT} termasuk dalam daftar putih {@link #CHANNELS}.
	 *
	 * @param channel nama kanal yang sudah dihuruf-besarkan pemanggil
	 * @throws ApiException bila kanal tidak dikenali
	 */
	private static void validateChannel(String channel) throws ApiException {
		if (!CHANNELS.contains(channel)) {
			throw new ApiException(400, "400", "CHANNEL_BMT tidak dikenali.");
		}
	}

	/**
	 * Menolak permintaan yang stempel waktunya terlalu jauh dari waktu server.
	 *
	 * <p>{@code TIMESTAMP} dibaca sebagai detik sejak epoch, dan selisih mutlaknya terhadap
	 * waktu server dibandingkan dengan toleransi milik konfigurasi. Karena selisih diambil
	 * mutlak, stempel waktu yang terlalu maju pun ditolak.</p>
	 *
	 * <p>Pemeriksaan ini <b>tidak cukup sendirian</b> untuk mencegah pemutaran ulang pesan;
	 * penjagaan sebenarnya ada pada {@link #reserveNonce}.</p>
	 *
	 * @param data     isi permintaan yang sudah didekripsi
	 * @param settings kredensial efektif yang memuat besaran toleransi
	 * @throws ApiException bila stempel waktu tidak ada, tidak sah, atau di luar toleransi
	 */
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

	/**
	 * Memesan sebuah nonce secara permanen sehingga pesan yang sama tidak dapat diputar ulang.
	 *
	 * <p>Pemesanan dilakukan dengan menyisipkan baris ke tabel {@code online_bmt_nonce}.
	 * Keunikan ditegakkan oleh basis data, bukan oleh pemeriksaan baca-lalu-tulis, sehingga
	 * tidak ada celah balapan antar permintaan bersamaan: pelanggaran batasan unik
	 * (SQLSTATE {@code 23505}) diterjemahkan menjadi penolakan berkode {@code "409"}.</p>
	 *
	 * <p>Nonce lebih dari 200 karakter ditolak lebih dahulu. Baris nonce sengaja tidak pernah
	 * dihapus.</p>
	 *
	 * @param nonce nilai {@code NONCE} dari permintaan
	 * @param type  jenis permintaan, disimpan sebagai keterangan
	 * @throws ApiException bila nonce terlalu panjang atau sudah pernah dipakai
	 * @throws Exception    bila penyimpanan gagal karena sebab lain
	 */
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

	/**
	 * Memasang atau melepas <i>advisory lock</i> PostgreSQL atas satu nomor transaksi.
	 *
	 * <p>Kunci dipasang pada tingkat <b>session</b> ({@code pg_advisory_lock}), bukan tingkat
	 * transaksi, agar tetap dipegang ketika ledger {@code PROCESSING} di-<i>commit</i> dan mesin
	 * posting membuka transaksi tersendiri. Nama kunci dibentuk dari awalan
	 * {@code "online-bmt:"} ditambah nomor transaksi, lalu diringkas {@code hashtext}.</p>
	 *
	 * <p>Karena kunci bertingkat session, pemanggil <b>wajib</b> melepasnya di blok
	 * {@code finally} &mdash; dan {@link #payment} serta {@link #checkStatus} memang melakukannya.</p>
	 *
	 * @param session       session yang koneksinya dipakai memasang kunci
	 * @param transactionNo nomor transaksi yang menjadi nama kunci
	 * @param lock          {@code true} memasang kunci, {@code false} melepasnya
	 * @throws SQLException bila perintah kunci gagal dijalankan
	 */
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

	/**
	 * Merapikan pesan galat agar aman disimpan pada kolom keterangan ledger.
	 *
	 * <p>Pesan kosong diganti nama kelas pengecualiannya, pemisah baris diganti spasi supaya
	 * satu baris ledger tidak terpecah, dan panjangnya dipotong pada 500 karakter.</p>
	 *
	 * @param error pengecualian asal; boleh {@code null}
	 * @return pesan satu baris sepanjang paling banyak 500 karakter
	 */
	private static String safeErrorMessage(Exception error) {
		String message = error == null ? null : error.getMessage();
		if (message == null || message.trim().length() == 0) {
			return error == null ? "kesalahan internal" : error.getClass().getSimpleName();
		}
		message = message.replace('\r', ' ').replace('\n', ' ').trim();
		return message.length() > 500 ? message.substring(0, 500) : message;
	}

	/**
	 * Membaca baris ledger terakhir untuk satu nomor transaksi.
	 *
	 * <p>Bila ada lebih dari satu baris, yang diambil adalah yang id-nya terbesar. Pemanggil
	 * wajib sudah memegang <i>advisory lock</i> atas nomor transaksi tersebut agar hasil bacaan
	 * tidak berubah di tengah proses.</p>
	 *
	 * @param session       session yang koneksinya dipakai
	 * @param transactionNo nomor transaksi yang dicari
	 * @return isi ledger, atau {@code null} bila belum ada
	 * @throws SQLException bila kueri gagal
	 */
	private static Ledger findLedger(Session session, String transactionNo) throws SQLException {
		PreparedStatement ps = null; ResultSet rs = null;
		try {
			ps = session.connection().prepareStatement(
					"SELECT no_invoice, nominal, channel_bmt, status FROM public.online_bmt_request_guard WHERE no_transaksi_bmt=? ORDER BY id DESC LIMIT 1");
			ps.setString(1, transactionNo); rs = ps.executeQuery();
			return rs.next() ? new Ledger(rs.getString(1), rs.getBigDecimal(2), rs.getString(3), rs.getString(4)) : null;
		} finally { close(rs); close(ps); }
	}

	/**
	 * Menandai satu nomor transaksi sebagai sedang diproses, membuat barisnya bila belum ada.
	 *
	 * <p>Memakai {@code INSERT ... ON CONFLICT (no_transaksi_bmt) DO UPDATE} sehingga percobaan
	 * ulang atas nomor yang sama menulis ke baris yang sama, bukan menambah baris baru.</p>
	 *
	 * <p>Pemanggil <b>wajib</b> me-<i>commit</i> transaksi ini sebelum memanggil mesin posting;
	 * lihat penjelasan pada {@link #payment}.</p>
	 *
	 * @param session session yang koneksinya dipakai
	 * @param input   rincian permintaan yang disimpan ke ledger
	 * @throws SQLException bila penyimpanan gagal
	 */
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
	 * Mengikat kanal pada ledger yang dibuat versi aplikasi sebelum kolom {@code channel_bmt}
	 * tersedia.
	 *
	 * <p>Pengikatan hanya boleh terjadi di bawah <i>advisory lock</i> dan setelah invoice serta
	 * nominal cocok; pada titik itu permintaan sudah lolos API key, HMAC, stempel waktu, nonce,
	 * dan daftar putih kanal. Perintah SQL-nya masih menyertakan syarat
	 * {@code channel_bmt IS NULL}, sehingga setelah sekali terisi kanal tidak dapat diganti oleh
	 * percobaan ulang berikutnya.</p>
	 *
	 * <p>Ledger yang sudah berkanal, atau yang invoice/nominalnya tidak cocok, dikembalikan apa
	 * adanya tanpa disentuh.</p>
	 *
	 * @param session session yang koneksinya dipakai
	 * @param ledger  ledger yang ditemukan; boleh {@code null}
	 * @param input   rincian permintaan yang menjadi sumber nama kanal
	 * @return ledger dengan kanal terisi, atau ledger asal bila tidak ada yang diubah
	 * @throws SQLException bila pemutakhiran gagal
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

	/**
	 * Memutakhirkan status, kode, dan pesan hasil pada baris ledger satu nomor transaksi.
	 *
	 * <p>Status yang dipakai kelas ini adalah {@code PROCESSING}, {@code SUCCESS}, dan
	 * {@code FAILED}. Kolom {@code updated_at} selalu ikut disegarkan.</p>
	 *
	 * @param session       session yang koneksinya dipakai
	 * @param transactionNo nomor transaksi yang dimutakhirkan
	 * @param status        status baru
	 * @param code          kode hasil protokol yang disimpan
	 * @param message       keterangan hasil; sebaiknya sudah dirapikan {@link #safeErrorMessage}
	 * @throws SQLException bila pemutakhiran gagal
	 */
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

	/**
	 * Membuka amplop {@code DATA} dan sekaligus memeriksa keasliannya.
	 *
	 * <p>Isi {@code DATA} adalah Base64 dari teks {@code v1.<iv>.<ciphertext>.<hmac>}, yang
	 * masing-masing bagiannya sendiri ber-Base64. Urutan pemeriksaan:</p>
	 * <ol>
	 *   <li>jumlah bagian harus tepat empat dan penanda versi harus {@code "v1"};</li>
	 *   <li>HMAC-SHA256 dihitung atas tiga bagian pertama lalu dibandingkan dengan bagian
	 *       keempat memakai {@link MessageDigest#isEqual} &mdash; perbandingan berwaktu tetap,
	 *       sehingga tidak membocorkan informasi lewat lama pembandingan;</li>
	 *   <li>panjang IV harus tepat 16 bita dan panjang ciphertext harus kelipatan 16;</li>
	 *   <li>baru setelah itu AES-256-CBC dijalankan, dengan kunci berupa SHA-256 atas kunci
	 *       enkripsi.</li>
	 * </ol>
	 *
	 * <p>Urutan tersebut penting: keaslian diperiksa <b>sebelum</b> dekripsi, sehingga ciphertext
	 * yang dirusak tidak pernah sampai ke penguraian isian.</p>
	 *
	 * <p>Seluruh kegagalan selain HMAC dijawab pesan seragam {@code "DATA terenkripsi tidak
	 * valid."} agar bentuk kesalahan tidak menjadi petunjuk bagi penyerang.</p>
	 *
	 * @param encrypted     isi {@code DATA} dari amplop permintaan
	 * @param encryptionKey kunci enkripsi kandidat
	 * @param hmacKey       kunci HMAC kandidat
	 * @return isi permintaan yang sudah terbuka
	 * @throws ApiException bila HMAC tidak cocok atau amplop tidak sah
	 */
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

	/**
	 * Menerjemahkan teks Base64 menjadi bita, dengan penolakan tegas atas masukan yang tidak
	 * sah.
	 *
	 * <p>Diperlukan karena {@code Base64.decodeBase64} pada pustaka commons-codec mengabaikan
	 * karakter yang bukan Base64 dan mengembalikan larik kosong, bukan melempar kesalahan.
	 * Pemeriksaan eksplisit di sini mencegah masukan rusak lolos diam-diam.</p>
	 *
	 * @param value teks Base64
	 * @return bita hasil terjemahan
	 * @throws Exception bila teks kosong atau bukan Base64 yang sah
	 */
	private static byte[] decodeBase64(String value) throws Exception {
		if (value == null || value.length() == 0 || !Base64.isBase64(value)) {
			throw new Exception("base64");
		}
		return Base64.decodeBase64(value);
	}

	/**
	 * Membungkus isi balasan ke dalam amplop yang bentuknya sama dengan amplop permintaan.
	 *
	 * <p>IV sepanjang 16 bita dibangkitkan {@link java.security.SecureRandom} untuk setiap
	 * balasan, sehingga dua balasan berisi sama tetap menghasilkan ciphertext berbeda. Hasil
	 * akhirnya adalah Base64 dari {@code v1.<iv>.<ciphertext>.<hmac>}.</p>
	 *
	 * @param data     isi balasan yang akan dienkripsi
	 * @param settings kredensial efektif pemilik invoice
	 * @return amplop terenkripsi siap ditaruh pada medan {@code DATA}
	 * @throws Exception bila enkripsi gagal
	 */
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

	/**
	 * Menghitung HMAC-SHA256 atas sebuah teks.
	 *
	 * <p>Kunci mentah tidak dipakai langsung, melainkan diringkas lebih dahulu dengan SHA-256
	 * sehingga kunci sepanjang apa pun menghasilkan bahan kunci 32 bita yang seragam.</p>
	 *
	 * @param value teks yang ditandatangani
	 * @param key   kunci HMAC mentah dari konfigurasi
	 * @return 32 bita hasil HMAC
	 * @throws Exception bila algoritma tidak tersedia atau kunci tidak sah
	 */
	private static byte[] hmac(String value, String key) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(sha256(key), "HmacSHA256"));
		return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Meringkas sebuah teks menjadi 32 bita dengan SHA-256.
	 *
	 * <p>Dipakai menurunkan bahan kunci AES maupun HMAC dari kunci mentah di konfigurasi.</p>
	 *
	 * @param value teks yang diringkas
	 * @return 32 bita hasil ringkasan
	 * @throws Exception bila algoritma tidak tersedia
	 */
	private static byte[] sha256(String value) throws Exception {
		return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Membentuk balasan berhasil dengan {@code DATA} yang selalu terenkripsi.
	 *
	 * <p>Konfirmasi BMT tanggal 3 September 2026: parameter {@code DATA} pada respons wajib
	 * dienkripsi. Karena itu {@link #encrypt} dipanggil tanpa syarat &mdash; jangan mengembalikan
	 * {@link JSONObject} apa adanya walaupun konfigurasi lama
	 * {@code online_bmt_enkripsi_response} pernah disetel tidak aktif.</p>
	 *
	 * @param message  keterangan yang ditaruh pada medan {@code KETERANGAN}
	 * @param data     isi balasan yang akan dienkripsi
	 * @param settings kredensial efektif pemilik invoice
	 * @return badan balasan berstatus berhasil berkode {@code "00"}
	 * @throws Exception bila enkripsi gagal
	 */
	private static JSONObject success(String message, JSONObject data, OnlineBmtUtil.Settings settings) throws Exception {
		JSONObject result = new JSONObject(); result.put("STATUS", true); result.put("KODE_STATUS", "00");
		result.put("KETERANGAN", message);
		/* Konfirmasi BMT tanggal 3 September 2026: parameter DATA pada response
		 * wajib dienkripsi. Jangan kembalikan JSONObject plaintext walaupun konfigurasi
		 * legacy online_bmt_enkripsi_response pernah disetel tidak aktif. */
		result.put("DATA", encrypt(data, settings));
		return result;
	}

	/**
	 * Membentuk balasan berisi status satu transaksi.
	 *
	 * <p>Medan {@code STATUS_TRANSAKSI} dan {@code DESKRIPSI_STATUS} dibungkus {@link #success},
	 * sehingga amplopnya tetap terenkripsi. Perhatikan bahwa {@code KETERANGAN} di lapisan luar
	 * sengaja dikosongkan ketika kode bernilai {@code "00"}; pesan hanya diulang di luar bila
	 * hasilnya bukan berhasil.</p>
	 *
	 * @param code     kode status transaksi, {@code "00"} berhasil
	 * @param message  keterangan status
	 * @param settings kredensial efektif pemilik invoice
	 * @return badan balasan lengkap
	 * @throws Exception bila enkripsi gagal
	 */
	private static JSONObject transactionResult(String code, String message, OnlineBmtUtil.Settings settings) throws Exception {
		JSONObject data = new JSONObject(); data.put("STATUS_TRANSAKSI", code); data.put("DESKRIPSI_STATUS", message);
		return success("00".equals(code) ? "" : message, data, settings);
	}

	/**
	 * Membentuk balasan gagal.
	 *
	 * <p>Berbeda dengan {@link #success}, medan {@code DATA} di sini berupa objek kosong dan
	 * <b>tidak</b> dienkripsi &mdash; memang tidak ada isi rahasia yang perlu dilindungi, dan
	 * balasan gagal harus tetap dapat dibentuk pada keadaan ketika kredensial pemilik invoice
	 * belum atau tidak dapat ditentukan.</p>
	 *
	 * <p>{@link LinkedHashMap} dipakai agar urutan medan pada JSON tetap tersusun seperti
	 * kontrak contoh BMT.</p>
	 *
	 * @param code    kode status protokol
	 * @param message keterangan kegagalan
	 * @return badan balasan berstatus gagal
	 */
	private static JSONObject failure(String code, String message) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("STATUS", Boolean.FALSE);
		result.put("KODE_STATUS", code);
		result.put("KETERANGAN", message);
		result.put("DATA", Collections.emptyMap());
		return new JSONObject(result);
	}

	/**
	 * Membaca satu isian wajib sebagai teks yang sudah dipangkas spasinya.
	 *
	 * @param data isi permintaan yang sudah didekripsi
	 * @param key  nama isian
	 * @return nilai isian, dipastikan tidak kosong
	 * @throws ApiException bila isian tidak ada atau kosong
	 */
	private static String required(JSONObject data, String key) throws ApiException {
		String value = data.optString(key, "").trim();
		if (value.length() == 0) throw new ApiException(400, "400", key + " wajib diisi.");
		return value;
	}

	/**
	 * Membaca satu isian wajib sekaligus membatasi panjangnya.
	 *
	 * <p>Batas panjang mencegah nilai berlebihan masuk ke kolom basis data maupun ke nama
	 * <i>advisory lock</i>.</p>
	 *
	 * @param data      isi permintaan yang sudah didekripsi
	 * @param key       nama isian
	 * @param maxLength panjang maksimum yang diterima
	 * @return nilai isian yang sudah dipastikan tidak kosong dan tidak terlalu panjang
	 * @throws ApiException bila isian kosong atau melebihi batas panjang
	 */
	private static String requiredMax(JSONObject data, String key, int maxLength) throws ApiException {
		String value = required(data, key);
		if (value.length() > maxLength) {
			throw new ApiException(400, "400", key + " terlalu panjang.");
		}
		return value;
	}

	/**
	 * Mengurai badan permintaan menjadi amplop JSON.
	 *
	 * <p>Kegagalan penguraian diterjemahkan menjadi {@link ApiException} berkode {@code "400"},
	 * bukan dibiarkan menjadi kegagalan tak terduga yang akan dijawab HTTP 500.</p>
	 *
	 * @param body badan permintaan mentah
	 * @return amplop JSON
	 * @throws ApiException bila badan permintaan bukan JSON yang sah
	 */
	private static JSONObject parseEnvelope(String body) throws ApiException {
		try {
			return new JSONObject(body);
		} catch (Exception e) {
			throw new ApiException(400, "400", "Format JSON request tidak valid.");
		}
	}

	/**
	 * Membandingkan dua nominal dengan toleransi satu sen.
	 *
	 * <p>Toleransi diperlukan karena sistem yang berbeda merepresentasikan pecahan rupiah
	 * dengan cara berbeda. Perbandingan memakai {@link BigDecimal}, bukan bilangan pecahan
	 * biner, sehingga tidak ada galat pembulatan yang menumpuk.</p>
	 *
	 * <p>Nilai {@code null} pada salah satu sisi selalu dinilai tidak sama.</p>
	 *
	 * @param a nominal pertama
	 * @param b nominal kedua
	 * @return {@code true} bila selisih mutlaknya tidak melampaui satu sen
	 */
	private static boolean sameAmount(BigDecimal a, BigDecimal b) {
		return a != null && b != null && a.subtract(b).abs().compareTo(new BigDecimal("0.01")) <= 0;
	}

	/**
	 * Menelusuri rantai penyebab sebuah pengecualian untuk mencari pelanggaran batasan unik.
	 *
	 * <p>Penelusuran diperlukan karena Hibernate membungkus {@link SQLException} asli beberapa
	 * lapis. Penanda yang dicari adalah SQLSTATE {@code 23505} milik PostgreSQL.</p>
	 *
	 * @param e pengecualian yang diperiksa
	 * @return {@code true} bila salah satu penyebabnya adalah pelanggaran batasan unik
	 */
	private static boolean isUniqueViolation(Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof SQLException && "23505".equals(((SQLException) t).getSQLState())) return true;
		}
		return false;
	}

	/**
	 * Membaca badan permintaan menjadi satu string, dengan pembatasan ukuran.
	 *
	 * <p>Pembacaan dihentikan begitu panjangnya melampaui 1 MiB sehingga permintaan raksasa
	 * tidak menghabiskan memori. Badan yang kosong ditolak.</p>
	 *
	 * <p>Perhatikan bahwa pemisah baris dibuang, sehingga payload multi-baris digabung rapat &mdash;
	 * tidak menjadi masalah karena isinya JSON.</p>
	 *
	 * @param request permintaan yang dibaca
	 * @return badan permintaan sebagai satu string
	 * @throws IOException  bila pembacaan gagal
	 * @throws ApiException bila badan permintaan kosong (400) atau terlalu besar (413)
	 */
	private static String readBody(HttpServletRequest request) throws IOException, ApiException {
		StringBuilder body = new StringBuilder(); BufferedReader reader = request.getReader(); String line;
		while ((line = reader.readLine()) != null) {
			if (body.length() + line.length() > 1024 * 1024) throw new ApiException(413, "413", "Request terlalu besar.");
			body.append(line);
		}
		if (body.toString().trim().length() == 0) throw new ApiException(400, "400", "Request body kosong.");
		return body.toString();
	}

	/**
	 * Menuliskan badan balasan JSON beserta header yang menyertainya.
	 *
	 * <p>Header {@code Cache-Control: no-store} dipasang agar balasan yang memuat status
	 * transaksi tidak tersimpan di perantara mana pun.</p>
	 *
	 * @param response balasan yang ditulis
	 * @param status   kode status HTTP
	 * @param body     badan balasan
	 * @throws IOException bila penulisan gagal
	 */
	private static void write(HttpServletResponse response, int status, JSONObject body) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json; charset=UTF-8");
		response.setHeader("Cache-Control", "no-store");
		PrintWriter writer = response.getWriter(); writer.write(body.toString()); writer.flush();
	}

	/**
	 * Menutup sumber daya JDBC tanpa memunculkan kegagalan baru.
	 *
	 * <p>Kegagalan penutupan dicatat ke audit, bukan dilempar, supaya tidak menutupi kegagalan
	 * sebenarnya yang sedang merambat dari blok {@code try}.</p>
	 *
	 * @param value sumber daya yang ditutup; {@code null} diabaikan
	 */
	private static void close(AutoCloseable value) {
		if (value != null) try { value.close(); } catch (Exception e) { ErrorAuditUtil.record(e, "OnlineBmt.close"); }
	}

	/**
	 * Identitas mitra dan merchant yang dikembalikan kepada BMT pada balasan inquiry.
	 *
	 * <p>Nilainya berasal dari konfigurasi efektif pemilik invoice, dan keempatnya diperlakukan
	 * sebagai satu kesatuan &mdash; lihat {@link OnlineBmt#requireMerchantIdentity}.</p>
	 */
	private static final class MerchantIdentity {
		/**
		 * Kode mitra BMT, nama mitra BMT, kode merchant, dan nama merchant. Seluruhnya
		 * {@code final} sehingga objek ini tidak dapat berubah setelah dibentuk.
		 */
		final String kodeMitra, namaMitra, kodeMerchant, namaMerchant;

		/**
		 * Membentuk identitas dari keempat nilai konfigurasi.
		 *
		 * @param kodeMitra    kode mitra BMT
		 * @param namaMitra    nama mitra BMT
		 * @param kodeMerchant kode merchant
		 * @param namaMerchant nama merchant
		 */
		MerchantIdentity(String kodeMitra, String namaMitra, String kodeMerchant, String namaMerchant) {
			this.kodeMitra = kodeMitra;
			this.namaMitra = namaMitra;
			this.kodeMerchant = kodeMerchant;
			this.namaMerchant = namaMerchant;
		}

		/**
		 * Menguji apakah keempat nilai sudah terisi.
		 *
		 * @return {@code true} bila tidak ada satu pun yang kosong
		 */
		boolean isComplete() {
			return kodeMitra.length() > 0 && namaMitra.length() > 0
					&& kodeMerchant.length() > 0 && namaMerchant.length() > 0;
		}
	}

	/**
	 * Rincian permintaan {@code PAYMENT} atau {@code CHECK_STATUS_PAYMENT} yang sudah diurai
	 * dan divalidasi bentuknya.
	 *
	 * <p>Empat nilai pertama membentuk identitas transaksi yang disimpan pada ledger, sehingga
	 * nomor transaksi yang sama tidak dapat dipakai ulang untuk invoice, nominal, atau kanal
	 * yang berbeda.</p>
	 */
	private static final class PaymentInput {
		/**
		 * Nomor invoice, nomor transaksi BMT, kanal (sudah dihuruf-besarkan), nonce, dan nominal.
		 * Seluruhnya {@code final} sehingga objek ini tidak dapat berubah setelah dibentuk.
		 */
		final String invoiceNo, transactionNo, channel, nonce; final BigDecimal amount;
		/**
		 * Konstruktor privat; objek hanya dibentuk lewat {@link #parse(JSONObject)} sehingga
		 * seluruh nilainya dipastikan sudah divalidasi.
		 *
		 * @param invoiceNo     nomor invoice
		 * @param transactionNo nomor transaksi BMT
		 * @param channel       kanal yang sudah dihuruf-besarkan
		 * @param nonce         nilai nonce
		 * @param amount        nominal pembayaran
		 */
		private PaymentInput(String invoiceNo, String transactionNo, String channel, String nonce, BigDecimal amount) {
			this.invoiceNo=invoiceNo; this.transactionNo=transactionNo; this.channel=channel; this.nonce=nonce; this.amount=amount;
		}
		/**
		 * Mengurai isi permintaan menjadi {@link PaymentInput} sambil memvalidasi bentuknya.
		 *
		 * <p>Nominal dibaca sebagai {@link BigDecimal} dan harus lebih besar dari nol. Nomor invoice
		 * dan nomor transaksi dibatasi 255 karakter, kanal 30 karakter lalu dihuruf-besarkan dengan
		 * {@link Locale#ENGLISH} &mdash; penting agar hasilnya tidak bergantung pada
		 * <i>locale</i> server.</p>
		 *
		 * <p>Kegagalan penguraian nominal diterjemahkan menjadi pesan {@code "NOMINAL tidak valid."},
		 * sementara {@link ApiException} dari pemeriksaan isian diteruskan apa adanya agar pesannya
		 * tetap menunjuk isian yang bermasalah.</p>
		 *
		 * @param data isi permintaan yang sudah didekripsi
		 * @return rincian permintaan yang sudah tervalidasi
		 * @throws ApiException bila ada isian wajib yang kosong, terlalu panjang, atau nominalnya
		 *                      tidak sah
		 */
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

	/**
	 * Cuplikan satu baris tabel {@code online_bmt_request_guard}, yaitu ledger idempotensi
	 * transaksi.
	 *
	 * <p>Bersifat objek nilai yang tidak dapat berubah; pemutakhiran ke basis data dilakukan
	 * lewat {@link OnlineBmt#updateLedger} atau {@link OnlineBmt#bindLegacyChannel}, yang
	 * membentuk objek baru bila perlu.</p>
	 */
	private static final class Ledger {
		/**
		 * Nomor invoice, kanal ({@code null} pada baris warisan), status
		 * ({@code PROCESSING}/{@code SUCCESS}/{@code FAILED}), dan nominal transaksi.
		 */
		final String invoiceNo, channel, status; final BigDecimal amount;
		/**
		 * Membentuk cuplikan ledger dari hasil baca basis data.
		 *
		 * @param invoiceNo nomor invoice yang tercatat
		 * @param amount    nominal yang tercatat
		 * @param channel   kanal yang tercatat; {@code null} pada baris warisan
		 * @param status    status yang tercatat
		 */
		Ledger(String invoiceNo, BigDecimal amount, String channel, String status) {
			this.invoiceNo=invoiceNo; this.amount=amount; this.channel=channel; this.status=status;
		}
	}

	/**
	 * Kegagalan yang sudah punya kode protokol, sehingga dapat dijawab kepada BMT tanpa
	 * membocorkan rincian internal.
	 *
	 * <p>Dipakai di seluruh gerbang pemeriksaan kelas ini. {@link OnlineBmt#doPost}
	 * menerjemahkannya menjadi balasan gagal ber-HTTP 200 sesuai kontrak BMT.</p>
	 */
	private static final class ApiException extends Exception {
		/**
		 * Versi serialisasi, kode status HTTP yang dimaksudkan, dan kode status protokol.
		 *
		 * <p><b>Perhatikan:</b> {@code httpStatus} saat ini tidak dipakai &mdash;
		 * {@link OnlineBmt#doPost} selalu menjawab HTTP 200 untuk kegagalan jenis ini, mengikuti
		 * kontrak BMT. Nilainya tetap disimpan sebagai catatan niat semula di tiap tempat pelemparan.</p>
		 */
		private static final long serialVersionUID = 1L; final int httpStatus; final String code;
		/**
		 * Membentuk kegagalan berkode.
		 *
		 * @param httpStatus kode status HTTP yang dimaksudkan; lihat catatan pada medannya
		 * @param code       kode status protokol yang dikirim pada {@code KODE_STATUS}
		 * @param message    keterangan yang dikirim pada {@code KETERANGAN}; harus aman dibaca pihak
		 *                   luar
		 */
		ApiException(int httpStatus, String code, String message) { super(message); this.httpStatus=httpStatus; this.code=code; }
	}
}
