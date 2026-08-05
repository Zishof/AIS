package ais.ui.util;

import java.io.Serializable;
import java.net.URLEncoder;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;

public class SetelahUpload implements EventListener {

	private MyToolbarbuttonConfig downloadButton;
	private boolean janganPreviewDiLayarUtama;
	@SuppressWarnings("unused")
	private Serializable ref;
	private String jenis;
	@SuppressWarnings("unused")
	private boolean usingId;
	private Row vbPreview;
	private MyToolbarbuttonConfig hapusButton;
	@SuppressWarnings("rawtypes")
	private Class clazz;

	@SuppressWarnings("rawtypes")
	public SetelahUpload(MyToolbarbuttonConfig downloadButton, boolean janganPreviewDiLayarUtama, Serializable ref,
			String jenis, boolean usingId, Component vbPreview, MyToolbarbuttonConfig hapusButton, Class clazz) {
		this.downloadButton = downloadButton;
		this.janganPreviewDiLayarUtama = janganPreviewDiLayarUtama;
		this.ref = ref;
		this.jenis = jenis;
		this.usingId = usingId;

		this.vbPreview = Common.tampilanScroll2(vbPreview);
		this.vbPreview.getGrid().setWidth("98%");
		this.vbPreview.getGrid().setVisible(false);

		this.hapusButton = hapusButton;
		this.clazz = clazz;
	}

	@Override
	public void onEvent(final Event arg0) throws Exception {
		if (janganPreviewDiLayarUtama) {
			return;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event a) throws Exception {

				if (vbPreview == null || vbPreview.getPage() == null) {
					// komponen sudah tidak terpasang ke halaman (mis. window sudah ditutup
					// sebelum timer sempat berjalan) - tidak perlu lanjut memproses preview
					return;
				}

				vbPreview.getGrid().setVisible(true);
				Common.clear(vbPreview);
				boolean mobile = Common.isMobile();

				if (arg0 != null && arg0.getName() != null && arg0.getName().equalsIgnoreCase("")) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							System.out.println("Fokus ke downloadButton");
							downloadButton.focus();
							Clients.scrollIntoView(downloadButton);
						}
					});
				}

				FileFotoLain fileFotoLain = (FileFotoLain) arg0.getData();

				String nama = (fileFotoLain != null && fileFotoLain.getNama() != null
						&& (fileFotoLain.getNama().trim().equalsIgnoreCase("link")
								|| fileFotoLain.getNama().trim().equalsIgnoreCase("Berupa link file")))
										? fileFotoLain.getLink()
										: fileFotoLain.getNama();
				if (nama == null) {
					nama = jenis;
				}
				String nn = nama.length() > 30 ? nama.substring(0, 30) + "..." : nama;
				downloadButton.setLabel(nn);

				String icon = FileFoto.icon(nama);

				downloadButton.setImage(icon);

				String userid = null;
				try {
					String olehId = (fileFotoLain == null || fileFotoLain.getOlehId() == null) ? ""
							: fileFotoLain.getOlehId();
					String[] spl = olehId.split(";");
					userid = spl[0];
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/SetelahUpload.java:102");
					// TODO: handle exception
				}

				downloadButton.setTooltiptext("Download / Preview file \"" + nn + "\""
						+ (userid == null ? ""
								: " file ini diupload oleh \"" + userid + "\" pada "
										+ (fileFotoLain.getTanggal_dirubah() == null ? ""
												: Common.dateFormat5.get().format(fileFotoLain.getTanggal_dirubah()))));

				Tbmuser tbmuser = Common.getCurrentUser();
				String olehId = Common.generateOlehId(tbmuser);

				hapusButton.setVisible(Common.getApakahAdmin() || (tbmuser != null && fileFotoLain.getOlehId() != null
						&& olehId.equalsIgnoreCase(fileFotoLain.getOlehId())));

				String linkUtama = fileFotoLain.getLink() == null || fileFotoLain.getLink().isEmpty()
						? LampiranLain.ambilLinkLampiranLain(fileFotoLain, true, false, clazz, true)
						: fileFotoLain.getLink();

				if (nama.equalsIgnoreCase("Berupa link file") && !fileFotoLain.getLink().trim().isEmpty()) {
					linkUtama = fileFotoLain.getLink();
				}

				if (fileFotoLain.getGdrive() != null) {
					if (!janganPreviewDiLayarUtama) {
						Common.clear(vbPreview, "lampiran_tambahan");
						String contentVideo = fileFotoLain.getGdrive();
						Html html = new ais.ui.util.MyHtml("<iframe src=\"https://drive.google.com/file/d/"
								+ contentVideo + "/preview\" " + Common.getStyleContent() + "></iframe>");
						html.setAttribute("lampiran_tambahan", true);
						html.setParent(vbPreview);

						if (vbPreview instanceof HtmlBasedComponent) {
							((HtmlBasedComponent) vbPreview).setStyle("min-height: 470px;");
						}
						return;
					} else {
						linkUtama = "https://drive.google.com/open?id=" + fileFotoLain.getGdrive();
					}

				}

				// System.out.println("linkUtama -> " + linkUtama + " link > " +
				// fileFotoLain.getLink());

				if (nama != null) {
					nama = nama.trim();
					downloadButton.setVisible(true);
					if (!janganPreviewDiLayarUtama) {

						if (fileFotoLain.merupakanGambar() && !linkUtama.toLowerCase().trim().contains("dropbox")) {
							Image image = new Image(linkUtama);
							// Tanda tangan (jenis mengandung "TTD"/"tanda tangan") ditampilkan PROPORSIONAL &
							// kecil: tinggi 350px membuat gambar tanda tangan yang beraspek-lebar melebar sangat
							// besar (lihat tab Data Pegawai). Batasi via max-width/max-height sehingga browser
							// menskala proporsional (aspek rasio terjaga) tanpa mengubah preview gambar lain (foto).
							boolean tandaTangan = jenis != null && (jenis.toLowerCase().contains("ttd")
									|| jenis.toLowerCase().contains("tanda tangan"));
							if (tandaTangan) {
								image.setStyle("max-width:280px;max-height:90px;");
							} else if (!mobile) {
								if (vbPreview instanceof HtmlBasedComponent) {
									((HtmlBasedComponent) vbPreview).setStyle("min-height: 300px;");
								}
								image.setHeight("350px");
							} else {
								image.setWidth("100%");
							}
							image.setAttribute("lampiran_tambahan", true);
							vbPreview.appendChild(image);
						} else if (nama.toLowerCase().endsWith(".pdf")
								&& !linkUtama.toLowerCase().trim().contains("dropbox")) {
							if (vbPreview instanceof HtmlBasedComponent) {
								((HtmlBasedComponent) vbPreview).setStyle("min-height: 410px;");
							}
							Iframe iframe = new Iframe();
							iframe.setHeight("400px");
							iframe.setWidth(mobile ? "100%" : "90%");
							iframe.setStyle("border:none");
							iframe.setAttribute("lampiran_tambahan", true);
							vbPreview.appendChild(iframe);
							iframe.setSrc("https://docs.google.com/gview?embedded=true&url="
									+ URLEncoder.encode(linkUtama, "UTF-8"));

						} else if (FileFoto.merupakanDokumen(nama)
								&& (linkUtama.trim().contains("AmbilLampiran")
										|| linkUtama.trim().contains("/al?d="))) {
							// Dokumen (docx/doc/xls/ppt dll) yang link-nya via servlet lampiran. Selain bentuk
							// lama "AmbilLampiran?..." kini JUGA mengenali bentuk pendek "/al?d=..." (servlet
							// 'al' = AmbilLampiran). Tanpa ini, dokumen ber-link /al?d= JATUH ke cabang fallback
							// generik di bawah dan tampil sebagai "Tautan yang dikirim: <url panjang>" + iframe
							// rusak — bukan pratinjau dokumen. Dengan match ini, dokumen dipratinjau via
							// Google Docs viewer seperti halnya PDF.
							if (vbPreview instanceof HtmlBasedComponent) {
								((HtmlBasedComponent) vbPreview).setStyle("min-height: 410px;");
							}
							Iframe iframe = new Iframe();
							iframe.setHeight("400px");
							iframe.setWidth(mobile ? "100%" : "90%");
							iframe.setStyle("border:none");
							iframe.setAttribute("lampiran_tambahan", true);
							vbPreview.appendChild(iframe);
							iframe.setSrc("https://docs.google.com/gview?embedded=true&url="
									+ URLEncoder.encode(linkUtama, "UTF-8"));
						} else if (!linkUtama.trim().isEmpty() || linkUtama.toLowerCase().contains("yout")) {
							if (linkUtama.toLowerCase().trim().contains("dropbox") || fileFotoLain.bisaPreview()
									|| (fileFotoLain.getNama() != null
											&& fileFotoLain.getNama().equalsIgnoreCase("Berupa link file"))) {
								Common.displayUrlContent(linkUtama, vbPreview);
							}
						}
					}
				}
			}
		});

	}

}
