package ais.action.master.helper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dialog ZKoss "Riwayat Perubahan" untuk satu dokumen, lengkap dengan pemulihan.
 *
 * <p>Seluruh datanya dibaca dan dipulihkan lewat
 * {@code ais.action.servlet.api.RevisiApiHelper} -- kelas yang SAMA dipakai POS
 * Desktop/Android dan halaman JSP. Dengan begitu aturan pemulihannya tidak
 * mungkin berbeda antar kanal, dan tidak ada jalur restore kedua yang perlu
 * dijaga tetap sinkron.</p>
 *
 * <p><b>Batas kewenangan tetap milik helper.</b> {@code RevisiApiHelper.pulihkan}
 * menolak pengguna non-admin, jadi dialog ini tidak perlu -- dan tidak boleh --
 * membuat pemeriksaan hak aksesnya sendiri: dua pemeriksaan yang terpisah pasti
 * menyimpang cepat atau lambat.</p>
 */
public final class RiwayatRevisiZkDialog {

	private RiwayatRevisiZkDialog() {
	}

	/**
	 * @param kodeEntitas kunci pada {@code RevisiApiHelper.ENTITAS}, mis. "ujian"
	 * @param id          id dokumen yang riwayatnya dibuka
	 * @param judul       nama dokumen untuk judul jendela
	 */
	public static void buka(final String kodeEntitas, final Long id, final String judul)
			throws Exception {
		if (kodeEntitas == null || kodeEntitas.trim().length() == 0 || id == null) {
			MyMessageboxConfig.show("Pilih satu baris data dahulu untuk melihat riwayatnya.",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		final Window win = new Window("Riwayat Perubahan — " + (judul == null ? "" : judul),
				"normal", true);
		win.setWidth("760px");
		win.setHeight("560px");
		win.setClosable(true);
		win.setSizable(true);
		win.setPage(org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getFirstPage());

		final Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setParent(win);

		final org.zkoss.zul.Div wadah = new org.zkoss.zul.Div();
		wadah.setWidth("100%");
		wadah.setHeight("430px");
		wadah.setStyle("overflow:auto;border:1px solid #e2e8f0;padding:6px");
		wadah.setParent(isi);

		final EventListener muat = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				wadah.getChildren().clear();
				JSONObject req = new JSONObject();
				req.put("entitas", kodeEntitas);
				req.put("id", String.valueOf(id));
				req.put("batas", 50);
				JSONObject hasil = new JSONObject();
				ais.action.servlet.api.RevisiApiHelper.daftar(
						ais.common.Common.getCurrentUser(), req, hasil);
				if (!"00".equals(hasil.optString("status", ""))) {
					Label gagal = new Label(hasil.optString("description",
							"Riwayat tidak dapat dimuat."));
					gagal.setStyle("color:#b91c1c");
					gagal.setParent(wadah);
					return;
				}
				JSONArray data = hasil.optJSONArray("data");
				if (data == null || data.length() == 0) {
					new Label("Belum ada riwayat perubahan untuk data ini.").setParent(wadah);
					return;
				}
				for (int i = 0; i < data.length(); i++) {
					JSONObject r = data.getJSONObject(i);
					final int rev = r.optInt("rev", -1);
					Hbox baris = new Hbox();
					baris.setWidth("100%");
					baris.setStyle("padding:6px 0;border-bottom:1px solid #f1f5f9");

					Vbox teks = new Vbox();
					teks.setSpacing("0");
					new Label("Revisi " + rev + "  ·  " + r.optString("tipe", "")).setParent(teks);
					Label sub = new Label(r.optString("tanggal", "")
							+ (r.optString("oleh", "").length() > 0
									? "  ·  oleh " + r.optString("oleh") : ""));
					sub.setStyle("font-size:11px;color:#64748b");
					sub.setParent(teks);
					String ringkas = r.optString("nama", "");
					if (ringkas.length() > 0) {
						Label rk = new Label(ringkas);
						rk.setStyle("font-size:11px;color:#334155");
						rk.setParent(teks);
					}
					teks.setParent(baris);

					MyToolbarbuttonConfig pulih =
							new MyToolbarbuttonConfig("Pulihkan ke revisi ini", "/img/undo.gif");
					pulih.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event e) throws Exception {
							/* Pemulihan menimpa data yang sedang berlaku; pastikan disengaja.
							 * Bentuk BERLISTENER dipakai -- itulah pola yang benar-benar berjalan
							 * pada layar ZK di aplikasi ini (lihat tombol Hapus di UjianAction).
							 * Bentuk yang mengembalikan int tidak menahan alur di ZK, sehingga
							 * pemulihan akan berjalan sebelum penggunanya sempat menjawab. */
							MyMessageboxConfig.show(
									"Pulihkan data ini ke keadaan pada revisi " + rev + "?"
											+ "\n\nData yang berlaku sekarang akan ditimpa."
											+ " Perubahan ini sendiri ikut tercatat sebagai revisi baru,"
											+ " sehingga tetap dapat ditelusuri.",
									"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
												return;
											}
											JSONObject rq = new JSONObject();
											rq.put("entitas", kodeEntitas);
											rq.put("id", String.valueOf(id));
											rq.put("rev", rev);
											JSONObject hs = new JSONObject();
											ais.action.servlet.api.RevisiApiHelper.pulihkan(
													ais.common.Common.getCurrentUser(), rq, hs);
											MyMessageboxConfig.show(
													hs.optString("description",
															"00".equals(hs.optString("status", ""))
																	? "Data dipulihkan." : "Pemulihan gagal."),
													"Informasi", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION);
										}
									});
						}
					});
					pulih.setParent(baris);
					ais.ui.util.MenuAksiBaris.pasang(baris);
					baris.setParent(wadah);
				}
			}
		};

		Label catatan = new Label("Pemulihan hanya dapat dilakukan admin sistem, dan tercatat"
				+ " sebagai revisi baru sehingga riwayatnya tetap utuh.");
		catatan.setStyle("font-size:11px;font-style:italic;color:#64748b");
		catatan.setParent(isi);

		Hbox tombol = new Hbox();
		tombol.setStyle("padding-top:8px");
		tombol.setParent(isi);

		MyToolbarbuttonConfig segarkan = new MyToolbarbuttonConfig("Muat Ulang", "/img/refresh.gif");
		segarkan.addEventListener("onClick", muat);
		segarkan.setParent(tombol);

		MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				win.detach();
			}
		});
		tutup.setParent(tombol);

		muat.onEvent(null);
		win.doModal();
	}
}
