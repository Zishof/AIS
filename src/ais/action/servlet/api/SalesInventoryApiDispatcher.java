package ais.action.servlet.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.database.model.Tbmuser;

/**
 * <h3>Dispatcher aksi {@code si_*} -- varian "eBisnis Inventory &amp; Sales".</h3>
 *
 * <p>Dipanggil dari {@code ApiEBisnis.prosesAksiTambahan} (hook yang disisipkan TEPAT sebelum
 * fallback "Aksi tidak dikenal" di {@code PosApi.proses}) -- jadi HANYA hidup di endpoint
 * {@code /Api_eBisnis}; endpoint {@code /PosApi} lama tidak pernah melihat aksi ini.</p>
 *
 * <p>Kontrak return: {@code true} = aksi sudah ditangani (termasuk ditangani-dengan-error);
 * {@code false} = bukan aksi milik dispatcher ini, biarkan jatuh ke fallback "Aksi tidak
 * dikenal" existing. Aksi ber-prefix {@code si_} yang TIDAK dikenali TETAP ditangani (balasan
 * error spesifik) supaya pesan errornya jelas milik modul ini -- fail-closed, bukan diam.</p>
 */
public final class SalesInventoryApiDispatcher {

	private SalesInventoryApiDispatcher() {
	}

	public static boolean dispatch(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (action == null || !action.startsWith("si_")) {
			return false;
		}

		// Gerbang aktor fail-closed utk SELURUH permukaan si_: aktor POS biasa ditolak di sini
		// (lapisan kedua setelah gate menu bolehAksesActionKantin -- dua-duanya harus lolos).
		EbisnisActorContextResolver.ActorContext ctx = EbisnisActorContextResolver.resolve(tbmuser);
		if (EbisnisActorContextResolver.ACTOR_POS.equals(ctx.actorType) && !adaMenuInventorySales(ctx)) {
			hasil.put("status", "error");
			hasil.put("message", "Akun Anda tidak terdaftar pada modul Inventory & Sales. Hubungi admin untuk mengaktifkan.");
			return true;
		}

		if ("si_actor_context".equals(action)) {
			SalesInventoryHelper.aktorContext(tbmuser, hasil);
			normalisasi(hasil);
			return true;
		}

		// -- P2: Master Supplier / Customer / Sales (layar 01-07) --
		if ("si_supplier_list".equals(action)) {
			SalesInventoryMasterHelper.supplierList(ctx, payload, hasil);
		} else if ("si_supplier_detail".equals(action)) {
			SalesInventoryMasterHelper.supplierDetail(ctx, payload, hasil);
		} else if ("si_supplier_create".equals(action) || "si_supplier_update".equals(action)) {
			SalesInventoryMasterHelper.supplierSimpan(ctx, tbmuser, payload, hasil);
		} else if ("si_supplier_deactivate".equals(action)) {
			SalesInventoryMasterHelper.supplierDeactivate(ctx, tbmuser, payload, hasil);
		} else if ("si_customer_list".equals(action)) {
			SalesInventoryMasterHelper.customerList(ctx, payload, hasil);
		} else if ("si_customer_detail".equals(action)) {
			SalesInventoryMasterHelper.customerDetail(ctx, payload, hasil);
		} else if ("si_customer_create".equals(action) || "si_customer_update".equals(action)) {
			SalesInventoryMasterHelper.customerSimpan(ctx, tbmuser, payload, hasil);
		} else if ("si_customer_deactivate".equals(action)) {
			SalesInventoryMasterHelper.customerDeactivate(ctx, tbmuser, payload, hasil);
		} else if ("si_sales_list".equals(action) || "si_sales_detail".equals(action)) {
			SalesInventoryMasterHelper.salesList(ctx, payload, hasil);
		} else if ("si_sales_create".equals(action) || "si_sales_update".equals(action)) {
			SalesInventoryMasterHelper.salesSimpan(ctx, tbmuser, payload, hasil);
		} else if ("si_sales_deactivate".equals(action)) {
			SalesInventoryMasterHelper.salesDeactivate(ctx, tbmuser, payload, hasil);
		} else if ("si_inventory_balance".equals(action)) {
			SalesInventoryStokHelper.inventoryBalance(ctx, payload, hasil);
		} else if ("si_inventory_ledger".equals(action)) {
			SalesInventoryStokHelper.inventoryLedger(ctx, payload, hasil);
		} else if ("si_supplier_price_list".equals(action)) {
			SalesInventoryHargaHelper.supplierPriceList(ctx, payload, hasil);
		} else if ("si_supplier_price_save".equals(action)) {
			SalesInventoryHargaHelper.supplierPriceSave(ctx, tbmuser, payload, hasil);
		} else if ("si_customer_price_list".equals(action)) {
			SalesInventoryHargaHelper.customerPriceList(ctx, payload, hasil);
		} else if ("si_customer_price_save".equals(action)) {
			SalesInventoryHargaHelper.customerPriceSave(ctx, tbmuser, payload, hasil);
		} else if ("si_price_analysis".equals(action)) {
			SalesInventoryHargaHelper.priceAnalysis(ctx, payload, hasil);
		} else if ("si_purchase_terms_save".equals(action)) {
			SalesInventoryPayableHelper.purchaseTermsSave(ctx, tbmuser, payload, hasil);
		} else if ("si_payable_list".equals(action) || "si_payable_from_purchase".equals(action)) {
			SalesInventoryPayableHelper.payableList(ctx, payload, hasil);
		} else if ("si_payable_payment_create".equals(action)) {
			SalesInventoryPayableHelper.payablePaymentCreate(ctx, tbmuser, payload, hasil);
		} else if ("si_payable_payment_history".equals(action)) {
			SalesInventoryPayableHelper.payablePaymentHistory(ctx, payload, hasil);
		} else if ("si_payable_payment_receipt".equals(action)) {
			SalesInventoryPayableHelper.payablePaymentReceipt(ctx, payload, hasil);
		} else if ("si_payable_aging".equals(action)) {
			SalesInventoryPayableHelper.payableAging(ctx, payload, hasil);
		} else if ("si_purchase_report".equals(action)) {
			SalesInventoryPayableHelper.purchaseReport(ctx, payload, hasil);
		} else if ("si_import_legacy".equals(action)) {
			SalesInventoryDbfImportHelper.importLegacy(ctx, tbmuser, payload, hasil);
		} else if ("si_sales_order_create".equals(action)) {
			SalesInventoryReceivableHelper.salesOrderSimpan(ctx, tbmuser, payload, hasil, false);
		} else if ("si_sales_order_update".equals(action)) {
			SalesInventoryReceivableHelper.salesOrderSimpan(ctx, tbmuser, payload, hasil, true);
		} else if ("si_sales_order_list".equals(action)) {
			SalesInventoryReceivableHelper.salesOrderList(ctx, payload, hasil);
		} else if ("si_sales_order_detail".equals(action)) {
			SalesInventoryReceivableHelper.salesOrderDetail(ctx, payload, hasil);
		} else if ("si_sales_order_status".equals(action)) {
			SalesInventoryReceivableHelper.salesOrderStatus(ctx, tbmuser, payload, hasil);
		} else if ("si_sales_order_invoice".equals(action)) {
			SalesInventoryReceivableHelper.salesOrderInvoice(ctx, tbmuser, payload, hasil);
		} else if ("si_receivable_list".equals(action) || "si_receivable_from_sale".equals(action)) {
			SalesInventoryReceivableHelper.receivableList(ctx, payload, hasil);
		} else if ("si_collection_create".equals(action)) {
			SalesInventoryReceivableHelper.collectionCreate(ctx, tbmuser, payload, hasil);
		} else if ("si_collection_history".equals(action)) {
			SalesInventoryReceivableHelper.collectionHistory(ctx, payload, hasil);
		} else if ("si_collection_receipt".equals(action)) {
			SalesInventoryReceivableHelper.collectionReceipt(ctx, payload, hasil);
		} else if ("si_receivable_aging_customer".equals(action)) {
			SalesInventoryReceivableHelper.receivableAgingCustomer(ctx, payload, hasil);
		} else if ("si_receivable_aging_sales".equals(action)) {
			SalesInventoryReceivableHelper.receivableAgingSales(ctx, payload, hasil);
		} else if ("si_spj_create".equals(action)) {
			SalesInventoryTripHelper.spjSimpan(ctx, tbmuser, payload, hasil, false);
		} else if ("si_spj_update".equals(action)) {
			SalesInventoryTripHelper.spjSimpan(ctx, tbmuser, payload, hasil, true);
		} else if ("si_spj_list".equals(action)) {
			SalesInventoryTripHelper.spjList(ctx, payload, hasil);
		} else if ("si_spj_detail".equals(action)) {
			SalesInventoryTripHelper.spjDetail(ctx, payload, hasil);
		} else if ("si_spj_status".equals(action)) {
			SalesInventoryTripHelper.spjStatus(ctx, tbmuser, payload, hasil);
		} else if ("si_spj_nota_assign".equals(action)) {
			SalesInventoryTripHelper.spjNotaAssign(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_start".equals(action)) {
			SalesInventoryTripHelper.tripStart(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_list".equals(action)) {
			SalesInventoryTripHelper.tripList(ctx, payload, hasil);
		} else if ("si_trip_detail".equals(action) || "si_trip_report".equals(action)) {
			SalesInventoryTripHelper.tripDetail(ctx, payload, hasil);
		} else if ("si_trip_nota_result".equals(action)) {
			SalesInventoryTripHelper.tripNotaResult(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_barang_update".equals(action)) {
			SalesInventoryTripHelper.tripBarangUpdate(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_cash_sale".equals(action)) {
			SalesInventoryTripHelper.tripCashSale(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_deposit".equals(action)) {
			SalesInventoryTripHelper.tripDeposit(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_return".equals(action)) {
			SalesInventoryTripHelper.tripReturn(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_reconcile".equals(action)) {
			SalesInventoryTripHelper.tripReconcile(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_close".equals(action)) {
			SalesInventoryTripHelper.tripClose(ctx, tbmuser, payload, hasil);
		} else if ("si_trip_purchase_link".equals(action)) {
			SalesInventoryTripHelper.tripPurchaseLink(ctx, tbmuser, payload, hasil);
		} else if ("si_expense_category_list".equals(action)) {
			SalesInventoryTripHelper.expenseCategoryList(ctx, payload, hasil);
		} else if ("si_expense_category_save".equals(action)) {
			SalesInventoryTripHelper.expenseCategorySave(ctx, tbmuser, payload, hasil);
		} else if ("si_expense_create".equals(action)) {
			SalesInventoryTripHelper.expenseCreate(ctx, tbmuser, payload, hasil);
		} else if ("si_coa_list".equals(action)) {
			SalesInventoryFinanceHelper.coaList(ctx, payload, hasil);
		} else if ("si_coa_save".equals(action)) {
			SalesInventoryFinanceHelper.coaSave(ctx, tbmuser, payload, hasil);
		} else if ("si_cash_journal_list".equals(action)) {
			SalesInventoryFinanceHelper.cashJournalList(ctx, payload, hasil);
		} else if ("si_profit_loss_params".equals(action)) {
			SalesInventoryFinanceHelper.profitLossParams(ctx, payload, hasil);
		} else if ("si_gross_profit_report".equals(action)) {
			SalesInventoryFinanceHelper.grossProfitReport(ctx, payload, hasil);
		} else if ("si_profit_loss_report".equals(action) || "si_profit_loss_print".equals(action)) {
			SalesInventoryFinanceHelper.profitLossReport(ctx, payload, hasil);
		} else {
			// Aksi si_ lain menyusul per fase (P5 SPJ/Nota Sales, P6 finance).
			hasil.put("status", "error");
			hasil.put("message", "Aksi Inventory & Sales belum tersedia di server ini: " + action);
			return true;
		}
		normalisasi(hasil);
		return true;
	}

	/** Minimal satu kunci menu varian aktif utk role aktor -- dipakai meloloskan aktor POS yang
	 *  oleh admin memang sengaja diberi kunci Inventory &amp; Sales lewat editor Grup Pengguna. */
	private static boolean adaMenuInventorySales(EbisnisActorContextResolver.ActorContext ctx) {
		String[] kunci = { "master_supplier", "master_customer", "master_sales", "persediaan", "harga",
				"hutang", "penjualan_sales", "piutang", "surat_perintah_sales", "nota_sales",
				"biaya_sales", "pembelian_sales", "rekonsiliasi_sales", "kas_jurnal", "laba_rugi",
				"laporan_inventory_sales" };
		for (int i = 0; i < kunci.length; i++) {
			if (ctx.bolehMenu(kunci[i])) {
				return true;
			}
		}
		return false;
	}

	/** Seragamkan konvensi "00"/"91" helper ke status:"success"/"error" (paritas normalisasi PosApi). */
	private static void normalisasi(JSONObject hasil) throws Exception {
		String status = hasil.optString("status", "");
		if ("00".equals(status)) {
			hasil.put("status", "success");
		} else if (!"success".equals(status) && !"error".equals(status)) {
			hasil.put("statusAsli", status);
			hasil.put("status", "error");
			if (!hasil.has("message")) {
				hasil.put("message", hasil.optString("description", "Permintaan tidak dapat diproses."));
			}
		}
	}
}
