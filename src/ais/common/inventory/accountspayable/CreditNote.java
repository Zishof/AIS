package ais.common.inventory.accountspayable;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Objek nilai (value object) yang bersifat <i>immutable</i> merepresentasikan satu <b>nota
 * kredit</b> (credit note) dalam modul hutang usaha (accounts payable) pada domain inventaris
 * AIS. Sebuah credit note adalah dokumen yang diterbitkan untuk mengurangi nilai suatu invoice
 * (faktur) yang sudah ada — misalnya karena retur barang, koreksi harga, diskon susulan, atau
 * pembatalan sebagian transaksi — tanpa perlu membatalkan invoice aslinya secara utuh. Kelas ini
 * murni sebagai pembawa data (data carrier) yang telah divalidasi pada saat konstruksi, tanpa
 * logika bisnis lebih lanjut (perhitungan saldo, penerapan ke invoice, dsb. menjadi tanggung
 * jawab kelas lain di lapisan service/persistensi).
 *
 * <p>
 * <b>Karakteristik immutability</b> — seluruh field bersifat {@code final} dan kelas dideklarasikan
 * {@code final} (tidak dapat di-subclass), menjadikan setiap instance {@code CreditNote} yang
 * berhasil dibuat sebagai representasi yang tidak dapat diubah lagi. Untuk field bertipe
 * {@link Date} (yang secara alami <i>mutable</i>), kelas ini secara konsisten melakukan
 * <b>defensive copy</b>: baik saat menerima {@code issuedAt} di konstruktor maupun saat
 * mengembalikannya lewat {@link #getIssuedAt()}, selalu dibuat objek {@link Date} baru dari
 * {@code getTime()} milik nilai asal. Pola ini mencegah pemanggil luar memodifikasi tanggal
 * internal objek secara tidak sengaja melalui referensi bersama (shared mutable reference),
 * sekaligus mencegah objek {@code CreditNote} berubah setelah dibuat akibat objek {@link Date}
 * yang diberikan pemanggil dimodifikasi belakangan di luar kelas ini.
 * </p>
 *
 * <p>
 * <b>Validasi pada konstruktor</b> — konstruktor {@link #CreditNote(String, String, BigDecimal,
 * Date, String, String)} menegakkan sejumlah invarian bisnis sebelum objek dapat terbentuk:
 * identitas nota kredit ({@code creditNoteId}), identitas invoice terkait ({@code invoiceId}),
 * dan kunci idempotensi ({@code idempotencyKey}) wajib tidak {@code null}; nilai nominal
 * ({@code amount}) wajib tidak {@code null} dan harus lebih besar dari nol (nota kredit bernilai
 * nol atau negatif dianggap tidak valid secara bisnis); serta tanggal penerbitan
 * ({@code issuedAt}) wajib diisi. Pelanggaran salah satu aturan ini melempar
 * {@link IllegalArgumentException} dengan pesan berbahasa Indonesia yang menjelaskan aturan mana
 * yang dilanggar, sehingga kegagalan validasi dapat segera diketahui oleh pemanggil tanpa perlu
 * menelusuri lebih jauh ke lapisan persistensi.
 * </p>
 *
 * <p>
 * <b>Kunci idempotensi</b> — field {@code idempotencyKey} dirancang untuk mendukung pola
 * idempotent request pada operasi penerbitan nota kredit: pemanggil (mis. lapisan REST/service)
 * dapat menyertakan kunci unik yang sama pada percobaan ulang (retry) akibat kegagalan jaringan
 * atau timeout, sehingga lapisan penyimpanan dapat mendeteksi dan mencegah penerbitan nota kredit
 * duplikat untuk permintaan yang sebenarnya sama. Kelas ini sendiri tidak melakukan pengecekan
 * duplikasi — ia hanya membawa kunci tersebut sebagai bagian dari data yang tidak boleh kosong.
 * </p>
 *
 * <p>
 * Field {@code reason} (alasan penerbitan nota kredit) bersifat opsional dan tidak divalidasi,
 * karena tidak semua alur bisnis mewajibkan alasan tekstual eksplisit.
 * </p>
 */
public final class CreditNote {
	/** Identitas unik nota kredit ini (bukan identitas invoice). */
	private final String creditNoteId;
	/** Identitas invoice (faktur) yang menjadi rujukan/target pengurangan nilai oleh nota kredit ini. */
	private final String invoiceId;
	/** Nilai nominal nota kredit; wajib lebih besar dari nol. */
	private final BigDecimal amount;
	/** Tanggal penerbitan nota kredit; disalin secara defensif saat disimpan maupun dibaca. */
	private final Date issuedAt;
	/** Alasan penerbitan nota kredit (mis. retur, koreksi harga); boleh {@code null}. */
	private final String reason;
	/** Kunci idempotensi untuk mencegah penerbitan duplikat akibat percobaan ulang permintaan. */
	private final String idempotencyKey;

	/**
	 * Membentuk satu nota kredit baru yang tidak dapat diubah (immutable) setelah tervalidasi.
	 *
	 * @param creditNoteId   identitas unik nota kredit; wajib tidak {@code null}
	 * @param invoiceId      identitas invoice yang menjadi rujukan nota kredit ini; wajib tidak
	 *                       {@code null}
	 * @param amount         nilai nominal nota kredit; wajib tidak {@code null} dan harus lebih
	 *                       besar dari {@link BigDecimal#ZERO}
	 * @param issuedAt       tanggal penerbitan; wajib tidak {@code null}, disalin secara defensif
	 *                       sehingga perubahan pada objek yang diberikan pemanggil setelah
	 *                       konstruksi tidak memengaruhi instance ini
	 * @param reason         alasan penerbitan nota kredit; boleh {@code null}
	 * @param idempotencyKey kunci idempotensi untuk mencegah duplikasi penerbitan akibat retry;
	 *                       wajib tidak {@code null}
	 * @throws IllegalArgumentException bila {@code creditNoteId}, {@code invoiceId}, atau
	 *                                  {@code idempotencyKey} bernilai {@code null}; bila
	 *                                  {@code amount} bernilai {@code null} atau tidak positif;
	 *                                  atau bila {@code issuedAt} bernilai {@code null}
	 */
	public CreditNote(String creditNoteId, String invoiceId, BigDecimal amount, Date issuedAt,
			String reason, String idempotencyKey) {
		if (creditNoteId == null || invoiceId == null || idempotencyKey == null) throw new IllegalArgumentException("identitas credit note wajib");
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("nilai credit note harus positif");
		if (issuedAt == null) throw new IllegalArgumentException("tanggal credit note wajib");
		this.creditNoteId = creditNoteId; this.invoiceId = invoiceId; this.amount = amount;
		this.issuedAt = new Date(issuedAt.getTime()); this.reason = reason; this.idempotencyKey = idempotencyKey;
	}
	/** @return identitas unik nota kredit ini. */
	public String getCreditNoteId() { return creditNoteId; }
	/** @return identitas invoice yang menjadi rujukan nota kredit ini. */
	public String getInvoiceId() { return invoiceId; }
	/** @return nilai nominal nota kredit (selalu positif). */
	public BigDecimal getAmount() { return amount; }
	/** @return salinan defensif tanggal penerbitan nota kredit (bukan referensi internal). */
	public Date getIssuedAt() { return new Date(issuedAt.getTime()); }
	/** @return alasan penerbitan nota kredit, atau {@code null} bila tidak diisi. */
	public String getReason() { return reason; }
	/** @return kunci idempotensi yang menyertai penerbitan nota kredit ini. */
	public String getIdempotencyKey() { return idempotencyKey; }
}
