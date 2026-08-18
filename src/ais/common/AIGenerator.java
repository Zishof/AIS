package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Window;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.Sekolah;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AIGenerator {

	public static EventListener generateApa(String labelPengumuman, String labelTentang, String tanyaLabel,
			boolean tanyaLangsung, String tanyaAkhiran, String konfigurasiSystem, MyCkEditor myCkEditor,
			EventListener listenerSetelahSelesai, String tanyaMengajar, EventListener listenerProses) {
		return generateApa(labelPengumuman, labelTentang, null, tanyaLabel, tanyaLangsung, tanyaAkhiran,
				konfigurasiSystem, myCkEditor, listenerSetelahSelesai, tanyaMengajar, listenerProses);
	}

	public static EventListener generateApa(final String labelPengumuman, final String labelTentang,
			final Textbox tentangText, final String tanyaLabel, final boolean tanyaLangsung, final String tanyaAkhiran,
			final String konfigurasiSystem, final MyCkEditor myCkEditor, final EventListener listenerSetelahSelesaiOk,
			final String tanyaMengajar, final EventListener listenerProses) {
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Textbox s = new Textbox();

				final EventListener evenProses = new EventListener() {

					// Perkiraan jumlah token yg sudah diterima (utk progres streaming). Direset tiap percobaan.
					private final java.util.concurrent.atomic.AtomicInteger jmlToken = new java.util.concurrent.atomic.AtomicInteger(
							0);

					private void ambilPesan(int coba, Label label, Textbox data, Textbox error) throws Exception {

						if (coba > 5) {

							try {

								error.setValue("Maaf, data " + labelPengumuman
										+ " yang anda maksud belum bisa kami buat, coba lagi");

							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:70");
								// TODO: handle exception
							}
							label.setValue("");
							return;
						}

						PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
						Sekolah sekolah = SekolahUtil.getSekolah();

						// DEFAULT = Ollama (TIDAK_AKTIF). Gemini hanya bila dinyalakan eksplisit di Konfigurasi.
						if (Common.bolehKonfigurasi("ai_menggunakan_gemini", Konfigurasi.TIDAK_AKTIF)) {

							String tanyaApa = Common.getKonfigurasi("ai_chatbot_model_gemini_" + labelPengumuman,
									"{\r\n" + "  \"contents\": [\r\n" + "    {\r\n" + "      \"role\": \"user\",\r\n"
											+ "      \"parts\": [\r\n" + "        {\r\n"
											+ "          \"text\": \"input: Siapa kamu ?\"\r\n" + "        },\r\n"
											+ "        {\r\n"
											+ "          \"text\": \"output: Saya adalah Pengajar atau Dosen atau Guru di UNIVERSITAS_APA_SAJA\"\r\n"
											+ "        },\r\n" + "        {\r\n"
											+ "          \"text\": \"input: Mengajar apa ?\"\r\n" + "        },\r\n"
											+ "        {\r\n"
											+ "          \"text\": \"output: Saya mengajar MENGAJAR_APA_SAJA\"\r\n"
											+ "        },\r\n" + "        {\r\n"
											+ "          \"text\": \"input: BANTUAN_APA_SAJA TANYA_APA_SAJA\"\r\n"
											+ "        },\r\n" + "        {\r\n"
											+ "          \"text\": \"output: \"\r\n" + "        }\r\n" + "      ]\r\n"
											+ "    }\r\n" + "  ],\r\n" + "  \"generationConfig\": {\r\n"
											+ "    \"temperature\": 1,\r\n" + "    \"topK\": 40,\r\n"
											+ "    \"topP\": 0.95,\r\n" + "    \"maxOutputTokens\": 8192,\r\n"
											+ "    \"responseMimeType\": \"text/plain\"\r\n" + "  }\r\n" + "}")
									.getNilai();

							if (tentangText != null && tentangText.getValue() != null
									&& !tentangText.getValue().isEmpty()) {
								if (tanyaLangsung) {
									tanyaApa = tanyaApa.replaceAll("BANTUAN_APA_SAJA",
											StringUtils.replace(tentangText.getValue().trim(), "\"", ""));
									tanyaApa = tanyaApa.replaceAll("TANYA_APA_SAJA", "");
								} else {
									String tanya = s.getValue().trim();
									tanyaApa = tanyaApa.replaceAll("BANTUAN_APA_SAJA",
											StringUtils.replace(tentangText.getValue().trim(), "\"", ""));
									tanyaApa = tanyaApa.replaceAll("TANYA_APA_SAJA",
											StringUtils.replace(tanya, "\"", ""));
								}
							} else {
								if (tanyaLangsung) {
									tanyaApa = tanyaApa.replaceAll("BANTUAN_APA_SAJA",
											StringUtils.replace(tanyaLabel + tanyaAkhiran, "\"", ""));
									tanyaApa = tanyaApa.replaceAll("TANYA_APA_SAJA", "");
								} else {
									String tanya = tanyaLabel + " \"" + s.getValue().trim() + "\"" + tanyaAkhiran;
									tanyaApa = tanyaApa.replaceAll("BANTUAN_APA_SAJA",
											StringUtils.replace(labelTentang, "\"", ""));
									tanyaApa = tanyaApa.replaceAll("TANYA_APA_SAJA",
											StringUtils.replace(tanya, "\"", ""));
								}
							}

							tanyaApa = tanyaApa.replaceAll("MENGAJAR_APA_SAJA", tanyaMengajar);
							tanyaApa = tanyaApa.replaceAll("UNIVERSITAS_APA_SAJA",
									(sekolah != null && sekolah.getId() != null ? sekolah.getNama()
											: perguruanTinggi.getNama()));

							// Model Gemini dapat dikonfigurasi; default model VALID (gemini-2.0-flash-exp sudah 404).
							String versiModelGemini = Common
									.getKonfigurasi("ai_chatbot_versi_model_gemini", "gemini-2.0-flash").getNilai()
									.trim();
							String linkPost = "https://generativelanguage.googleapis.com/v1beta/models/"
									+ versiModelGemini + ":generateContent?key="
									+ Common.getKonfigurasi("ai_chatbot_api_key_gemini",
											"AIzaSyDBSOM4dHks3kQuXyhDzhBkRQz98VjHzPs").getNilai();

							System.out.println("tanyaApa -> " + tanyaApa);

							String[] command = { "curl", "--location", linkPost, "--header",
									"Content-Type: application/json", "--data", tanyaApa };
							String message = "";
							// Fallback dipakai bila API AI gagal / respons tak berisi "candidates" (mis.
							// diblokir safety filter, kuota habis, rate limit, dsb) agar "message" TIDAK
							// PERNAH kosong mengalir ke downstream (mis. Wa.ubahKeBold) yg bisa memicu
							// StringIndexOutOfBoundsException saat charAt(0) atas string kosong.
							String fallbackPesanGagal = "Maaf, data " + labelPengumuman
									+ " yang anda maksud belum bisa kami buat, coba lagi";
							try {

								ProcessBuilder process = new ProcessBuilder(command);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								StringBuilder builder = new StringBuilder();
								String line;
								while ((line = reader.readLine()) != null) {
									builder.append(line);
									builder.append(System.getProperty("line.separator"));
								}
								String hasil = builder.toString();

								System.out.println("hasil -> " + hasil);

								JSONObject jsonObject = new JSONObject(hasil);

								if (jsonObject.has("candidates") && !jsonObject.isNull("candidates")
										&& jsonObject.getJSONArray("candidates").length() > 0) {

									message = (jsonObject.getJSONArray("candidates").getJSONObject(0)
											.getJSONObject("content").getJSONArray("parts").getJSONObject(0)
											.get("text")) + "";

								} else {
									// Respons AI valid secara JSON tapi tanpa "candidates" -> bukan exception,
									// tapi tetap harus diaudit karena berarti AI gagal menjawab (mis. safety
									// block / quota / rate limit). Pakai keterangan "error" dari Gemini bila ada.
									String keteranganError = jsonObject.has("error") ? jsonObject.get("error").toString()
											: hasil;
									ais.common.ErrorAuditUtil.record(
											new Exception("Respons AI Gemini tanpa candidates: " + keteranganError),
											"auto-audit src/ais/common/AIGenerator.java:161 - JSON tanpa candidates");
									message = fallbackPesanGagal;
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AIGenerator.java:166");
								message = fallbackPesanGagal;
							}
							data.setValue(message);

							label.setValue("");

						} else {

							String tanya = tanyaLangsung ? tanyaLabel + tanyaAkhiran
									: tanyaLabel + " \"" + s.getValue().trim() + "\"" + tanyaAkhiran;

							JSONObject postData = new JSONObject();
							// Model UMUM utk Generate (buat konten paragraf). BEDA dari model terjemahan
							// (ecampus-translator) yg khusus terjemah singkat. Server AI sama (ollama_url).
							// Kunci BARU 'ai_model_generate' (default model RINGAN 1.5b) — sengaja beda dari
							// 'ollama_model_generate' lama agar tak terikat baris DB lama yg masih 3b.
							postData.put("model", Common
									.getKonfigurasi("ai_model_generate", "qwen2.5:1.5b-instruct-q4_K_M").getNilai()
									.trim());

							JSONArray messages = new JSONArray();
							JSONObject system = new JSONObject();
							system.put("role", "system");
							system.put("content",
									konfigurasiSystem + " di \""
											+ (sekolah != null && sekolah.getId() != null ? sekolah.getNama()
													: perguruanTinggi.getNama())
											+ "\"");
							messages.put(system);

							JSONObject user = new JSONObject();
							user.put("role", "user");
							user.put("content", tanya);
							messages.put(user);

							postData.put("messages", messages);

							postData.put("stream", true);
							postData.put("keep_alive", "24h");

							// Batas token keluaran (jadi penyebut estimasi persen progres streaming).
							int targetToken = 700;
							try {
								targetToken = Integer
										.parseInt(Common.getKonfigurasi("ollama_num_predict", "700").getNilai().trim());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:209");
							}
							// PENTING (kecepatan): num_predict = 0 / negatif dianggap Ollama "tanpa batas" → generasi
							// berjalan sampai model berhenti sendiri; di CPU (tanpa GPU) ini SANGAT lama. Paksa ke
							// rentang wajar. Berita acara/catatan cukup ratusan token.
							if (targetToken <= 0 || targetToken > 2048) {
								targetToken = 700;
							}
							try {
								JSONObject opt = new JSONObject();
								opt.put("num_predict", targetToken);
								// Percepat di CPU: batasi konteks & (opsional) tetapkan jumlah thread.
								int numCtx = 2048;
								try {
									numCtx = Integer
											.parseInt(Common.getKonfigurasi("ollama_num_ctx", "2048").getNilai().trim());
								} catch (Exception e2) {
								}
								if (numCtx > 0) {
									opt.put("num_ctx", numCtx);
								}
								int numThread = 0;
								try {
									numThread = Integer
											.parseInt(Common.getKonfigurasi("ollama_num_thread", "0").getNilai().trim());
								} catch (Exception e2) {
								}
								if (numThread > 0) {
									opt.put("num_thread", numThread);
								}
								postData.put("options", opt);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:215");
							}
							jmlToken.set(0);

							// Endpoint diturunkan dari ollama_url (sama dgn AiTerjemah) → semua Generate 1 server AI.
							String linkPost = Common.getKonfigurasi("ollama_url", "http://38.47.182.162:11434").getNilai()
									.trim() + "/api/chat";

							String send = postData.toString();
							System.out.println("send -> " + send);
							String[] command = { "curl", "-m", "60000", "--location", linkPost, "--header",
									"Content-Type: application/json", "--data", send };

							try {

								ProcessBuilder process = new ProcessBuilder(command);
								Process p;
								p = process.start();
								BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
								String line;
								String da = "";
								String errorOllama = null;
								while ((line = reader.readLine()) != null) {
									if (line.trim().length() == 0) {
										continue;
									}
									JSONObject jsonObject;
									try {
										jsonObject = new JSONObject(line);
									} catch (Exception parseEx) {
										// Baris bukan JSON utuh (mis. potongan aliran) → lewati, BUKAN error fatal.
										continue;
									}
									// Baris error dari Ollama (mis. model tak ada, OOM) → simpan pesan aslinya.
									if (jsonObject.has("error") && !jsonObject.isNull("error")) {
										errorOllama = String.valueOf(jsonObject.opt("error"));
										continue;
									}
									// Baris tanpa "message" (mis. ringkasan akhir "done") → lewati tanpa dianggap error.
									if (!jsonObject.has("message") || jsonObject.isNull("message")) {
										continue;
									}
									String c = jsonObject.getJSONObject("message").optString("content", "");
									if (c.length() > 0) {
										da += c;
										jmlToken.incrementAndGet();
										label.setValue("Proses data -> " + c);
										data.setValue(da);
									}
								}

								// Bila tak ada keluaran & Ollama mengirim error → catat pesan asli (sekali) agar
								// terdiagnosa; jangan spam per-baris seperti sebelumnya.
								if (da.trim().length() == 0 && errorOllama != null) {
									ais.common.ErrorAuditUtil.record(new Exception("Ollama error: " + errorOllama),
											"auto-audit src/ais/common/AIGenerator.java - Ollama /api/chat error");
									// Error yang biasanya SEMENTARA (antrean penuh / model sedang dimuat / sibuk):
									// tunggu sebentar (backoff naik per percobaan) lalu COBA LAGI, bukan langsung gagal.
									String errLow = errorOllama.toLowerCase();
									boolean sementara = errLow.contains("busy") || errLow.contains("pending")
											|| errLow.contains("try again") || errLow.contains("loading")
											|| errLow.contains("timeout") || errLow.contains("unavailable");
									if (sementara) {
										try {
											Thread.sleep(1500L * Math.max(1, coba));
										} catch (Exception ie) {
										}
										ambilPesan(++coba, label, data, error);
										return;
									}
								}

								data.setValue(da);

								label.setValue("");

							} catch (Exception e) {
								ambilPesan(++coba, label, data, error);
							}

						}
					}

					@Override
					public void onEvent(Event event) throws Exception {

						final Textbox data = new Textbox();
						final Textbox error = new Textbox();
						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								if (!error.getValue().trim().isEmpty()) {
									MyMessageboxConfig.show(error.getValue().trim(), "Informasi", MyMessageboxConfig.OK,
											MyMessageboxConfig.INFORMATION);
								}

								listenerSetelahSelesaiOk.onEvent(new Event("", s, data.getValue()));

								if (myCkEditor != null)
									myCkEditor.setValue(
											ais.action.servlet.Wa.ubahKeBold(data.getValue()).replaceAll("\n", "<br>"));
							}
						});

						// ===== POPUP PROGRESS + STREAMING (semua fungsi Generate) =====
						final org.zkoss.zul.Window popupProgress = new org.zkoss.zul.Window();
						popupProgress.setTitle("🪄 " + labelPengumuman + " — sedang diproses AI");
						popupProgress.setBorder("normal");
						popupProgress.setWidth("580px");
						popupProgress.setHeight("440px");
						popupProgress.setClosable(false);
						popupProgress.setSclass("ais-ai-progress");
						popupProgress.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

						org.zkoss.zul.Vlayout isiPopup = new org.zkoss.zul.Vlayout();
						isiPopup.setStyle("padding:16px;");
						isiPopup.setParent(popupProgress);

						final Label persenLabel = new Label("0%");
						persenLabel.setStyle("font-size:26px;font-weight:800;color:#0d6efd;");
						persenLabel.setParent(isiPopup);

						final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter();
						meter.setValue(0);
						meter.setWidth("100%");
						meter.setParent(isiPopup);

						Label ketProgress = new Label(ais.common.Common.getBahasaConfig("Sedang menghasilkan teks, mohon tunggu…"));
						ketProgress.setStyle("color:#64748b;font-size:12px;");
						ketProgress.setParent(isiPopup);

						org.zkoss.zul.Div streamBox = new org.zkoss.zul.Div();
						streamBox.setStyle("margin-top:12px;height:290px;overflow:auto;border:1px solid #e2e8f0;"
								+ "border-radius:8px;padding:12px;background:#f8fafc;font-size:13px;line-height:1.55;");
						streamBox.setParent(isiPopup);

						final Label streamLabel = new Label("");
						streamLabel.setMultiline(true);
						streamLabel.setPre(true);
						streamLabel.setParent(streamBox);

						try {
							popupProgress.doHighlighted();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:326");
						}

						final Timer timer = new Timer(700);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								listenerProses.onEvent(new Event("", data, data.getValue().trim()));

								boolean selesai = label.getValue().trim().isEmpty();

								// Estimasi persen = jumlah token diterima / target token (num_predict).
								int target = 1500;
								try {
									target = Integer.parseInt(
											Common.getKonfigurasi("ollama_num_predict", "1500").getNilai().trim());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:346");
								}
								if (target < 1) {
									target = 1;
								}
								int pct = selesai ? 100 : (int) (jmlToken.get() * 100L / target);
								if (pct > 99 && !selesai) {
									pct = 99;
								}
								if (pct < 0) {
									pct = 0;
								}
								try {
									meter.setValue(pct);
									persenLabel.setValue(pct + "%");
									String teks = data.getValue();
									if (teks == null) {
										teks = "";
									}
									// tampilkan ekor teks terbaru agar kata-kata baru selalu terlihat
									if (teks.length() > 1400) {
										teks = "…" + teks.substring(teks.length() - 1400);
									}
									streamLabel.setValue(teks);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:370");
								}

								if (myCkEditor != null) {
									myCkEditor
											.setValue(ais.action.servlet.Wa.ubahKeBold(data.getValue()).trim().isEmpty()
													? "Harap tunggu..."
													: data.getValue().replaceAll("\n", "<br>"));
								}
								if (selesai) {
									try {
										meter.setValue(100);
										persenLabel.setValue("100%");
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:383");
									}
									try {
										popupProgress.detach();
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/AIGenerator.java:387");
									}
									timer.stop();
									timer.detach();
								}
							}
						});
						timer.start();

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
									ambilPesan(0, label, data, error);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/AIGenerator.java:403");
								}
								label.setValue("");
							}
						}).start();

					}
				};

				if (tanyaLangsung) {
					evenProses.onEvent(arg0);
				} else {

					final Window myWindow = new Window(labelPengumuman, "none", true);
					myWindow.setHeight("350px");
					myWindow.setWidth("850px");
					myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setWidth("100%");
					grid.setHeight("100%");

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new MyLabelBold(labelTentang));

					row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					row.appendChild(s);
					s.setWidth("95%");
					s.setRows(5);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							myWindow.detach();
						}
					});
					cancel.setParent(toolbar);

					MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Lanjut " + labelPengumuman,
							"/img/svg/gear.svg");
					toolbarbutton.setParent(toolbar);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							myWindow.detach();
							evenProses.onEvent(arg0);
						}
					});

					borderlayout.setParent(myWindow);
					myWindow.onModal();
				}
			}
		};

		return eventListener;
	}

}
