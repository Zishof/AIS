<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.database.model.Mahasiswa"%>
<%@page import="ais.database.model.BiodataCalonMahasiswa"%>
<%@page import="ais.database.model.JenisKegiatan"%>
<%@page import="ais.database.model.Jurusan"%>
<%@page import="ais.database.model.Fakultas"%>
<%@page import="ais.database.model.Kegiatan"%>
<%@page import="ais.database.model.Perkuliahan"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.action.master.helper.KegiatanHelper"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Disjunction"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.concurrent.Callable"%>
<%@page import="java.util.concurrent.ExecutorService"%>
<%@page import="java.util.concurrent.Executors"%>
<%@page import="java.util.concurrent.Future"%>
<%@page import="java.util.concurrent.ConcurrentHashMap"%>
<%@page import="java.util.concurrent.atomic.AtomicInteger"%>
<%@page import="java.text.DecimalFormat"%>
<%@page import="org.json.JSONObject"%>

<%!
    // Kelas Pembungkus ID Data untuk disalurkan ke Thread
    public static class WorkItem {
        public String type; // "MHS" atau "CALON"
        public Long id;
        public Long jkId; // Menyimpan ID Jenis Kegiatan secara spesifik
        public WorkItem(String type, Long id, Long jkId) { this.type = type; this.id = id; this.jkId = jkId; }
    }

    // Kelas Pembungkus Hasil dari masing-masing Thread
    public static class WorkerResult {
        public StringBuilder html = new StringBuilder();
        public Double tagihan = 0.0;
        public Double dibayar = 0.0;
        public Double piutang = 0.0;
    }

    // Kelas State Global untuk Status Background Job
    public static class TagihanState {
        public int totalData = 0;
        public AtomicInteger currentProses = new AtomicInteger(0);
        public String statusText = "Inisialisasi...";
        public StringBuilder htmlResult = new StringBuilder();
        public boolean isDone = false;
        public long startTime = System.currentTimeMillis();
        
        // Melacak progres untuk masing-masing Thread Paralel
        // [0] = Selesai, [1] = Total Beban Thread Tersebut
        public Map<Integer, int[]> workerProgress = new ConcurrentHashMap<Integer, int[]>();
    }

    // Penyimpanan sesi pekerjaan di Memory
    public static Map<String, TagihanState> jobStates = new ConcurrentHashMap<String, TagihanState>();
%>
<%
    try { out.clear(); out.clearBuffer(); } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:66");}
    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
    if (Common.getCurrentUser(request) == null) { out.print("{\"status\":\"error\", \"message\":\"Sesi Berakhir\"}"); return; }

    String action = request.getParameter("action");

    // =====================================================================
    // ROUTING STATUS POLLING (MEMANTAU PROGRES THREAD)
    // =====================================================================
    if ("PROGRESS".equals(action)) {
        String jobId = request.getParameter("jobId");
        TagihanState state = jobStates.get(jobId);
        JSONObject res = new JSONObject();
        if (state != null) {
            res.put("status", "00");
            res.put("current", state.currentProses.get());
            res.put("total", state.totalData);
            res.put("statusText", state.statusText);
            res.put("isDone", state.isDone);
            
            // Merakit HTML Progress Bar untuk masing-masing Thread
            StringBuilder wHtml = new StringBuilder("<div class='row text-start mt-4'>");
            for (int w = 0; w < state.workerProgress.size(); w++) {
                int[] wProg = state.workerProgress.get(w);
                if (wProg != null) {
                    int wCur = wProg[0];
                    int wTot = wProg[1];
                    double wPct = wTot > 0 ? (wCur * 100.0 / wTot) : 100.0;
                    String wPctStr = new DecimalFormat("#.##").format(wPct);
                    
                    wHtml.append("<div class='col-md-6 col-lg-4 mb-3'>");
                    wHtml.append("<div class='d-flex justify-content-between small fw-bold mb-1 text-secondary'>");
                    wHtml.append("<span><i class='fas fa-microchip me-1'></i> Worker ").append(w + 1).append("</span>");
                    wHtml.append("<span>").append(wCur).append(" / ").append(wTot).append(" (").append(wPctStr).append("%)</span>");
                    wHtml.append("</div>");
                    wHtml.append("<div class='progress shadow-sm' style='height: 10px; border-radius: 5px;'>");
                    wHtml.append("<div class='progress-bar bg-").append(wCur == wTot ? "success" : "primary").append(" progress-bar-striped progress-bar-animated' style='width: ").append(wPctStr).append("%'></div>");
                    wHtml.append("</div></div>");
                }
            }
            wHtml.append("</div>");
            res.put("workersHtml", wHtml.toString());

        } else {
            res.put("status", "error"); res.put("isDone", true);
        }
        out.print(res.toString()); out.flush(); return;
    }

    if ("RESULT".equals(action)) {
        String jobId = request.getParameter("jobId");
        TagihanState state = jobStates.remove(jobId); 
        JSONObject res = new JSONObject();
        if (state != null) {
            res.put("status", "00");
            res.put("html", state.htmlResult.toString());
        } else {
            res.put("status", "error");
        }
        out.print(res.toString()); out.flush(); return;
    }

    // =====================================================================
    // ROUTING START: MEMULAI THREAD PEKERJA LATAR BELAKANG
    // =====================================================================
    if ("START".equals(action)) {
        final String jobId = java.util.UUID.randomUUID().toString();
        final TagihanState state = new TagihanState();
        jobStates.put(jobId, state);

        final String ta = request.getParameter("tahunAkademik");
        final String taSampai = request.getParameter("tahunAkademikSampai");
        final String smtStr = request.getParameter("semester");
        final String fakIdStr = request.getParameter("fakultasId"); // Filter Fakultas
        final String jurIdStr = request.getParameter("jurusanId");
        final String jkIdStr = request.getParameter("jkId");
        final String angMulaiStr = request.getParameter("angkatanMulai");
        final String angSampaiStr = request.getParameter("angkatanSampai");
        final String numWorkersStr = request.getParameter("jumlahWorker");
        final String q = request.getParameter("q"); // Filter Pencarian (NIM/Nama)
        final boolean ulang = "true".equals(request.getParameter("hitungUlang"));

        // Penyesuaian Jumlah Pekerja dengan Default 10, Minimal 1, dan Maksimal 50
        int parsedWorkers = 10;
        try {
            if (numWorkersStr != null && !numWorkersStr.trim().isEmpty()) {
                parsedWorkers = Integer.parseInt(numWorkersStr);
                if (parsedWorkers < 1) parsedWorkers = 1;
                if (parsedWorkers > 50) parsedWorkers = 50; 
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:156");}
        final int maxWorkersConfig = parsedWorkers;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Session masterSess = null;
                try {
                    masterSess = HibernateUtil.openSession();
                    state.statusText = "Mengekstrak ID Mahasiswa dari pangkalan data...";

                    int angMulai = angMulaiStr != null && !angMulaiStr.isEmpty() ? Integer.parseInt(angMulaiStr) : 0;
                    int angSampai = angSampaiStr != null && !angSampaiStr.isEmpty() ? Integer.parseInt(angSampaiStr) : 0;
                    
                    // =======================================================================
                    // PENGAMBILAN JENIS KEGIATAN
                    // =======================================================================
                    List<JenisKegiatan> listJk = new ArrayList<JenisKegiatan>();
                    if (jkIdStr != null && !jkIdStr.trim().isEmpty()) {
                        JenisKegiatan singleJk = (JenisKegiatan) masterSess.get(JenisKegiatan.class, Long.parseLong(jkIdStr));
                        if (singleJk != null) listJk.add(singleJk);
                    } else {
                        listJk = ais.common.ConstantValues.simpleList(masterSess.createCriteria(JenisKegiatan.class).add(Restrictions.eq("aktif", true)).addOrder(Order.asc("namaKegiatan")), JenisKegiatan.class);
                    }

                    Jurusan jur = jurIdStr != null && !jurIdStr.trim().isEmpty() ? (Jurusan) masterSess.get(Jurusan.class, Long.parseLong(jurIdStr)) : null;
                    Fakultas fak = null;
                    if (jur != null) {
                        fak = jur.getFakultas();
                    } else if (fakIdStr != null && !fakIdStr.trim().isEmpty()) {
                        fak = (Fakultas) masterSess.get(Fakultas.class, Long.parseLong(fakIdStr));
                    }

                    final boolean ganjil = (smtStr == null || smtStr.isEmpty() || smtStr.equals("Ganjil"));
                    final boolean genap = (smtStr == null || smtStr.isEmpty() || smtStr.equals("Genap"));

                    // =======================================================================
                    // PENARIKAN DATA ENTITAS SEBELUM LOOPING JENIS KEGIATAN
                    // =======================================================================
                    boolean needMhs = false;
                    boolean needCalon = false;

                    for (JenisKegiatan j : listJk) {
                        boolean isMaba = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
                                || (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && j.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()));
                        if (isMaba) needCalon = true;
                        else needMhs = true;
                    }

                    List<BiodataCalonMahasiswa> calonList = new ArrayList<BiodataCalonMahasiswa>();
                    List<Mahasiswa> mhsList = new ArrayList<Mahasiswa>();

                    if (needCalon) {
                        Criteria crit = masterSess.createCriteria(BiodataCalonMahasiswa.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.between("tahunAkademik", ta, taSampai));
                        
                        if (q != null && !q.trim().isEmpty()) {
                            Disjunction orQ = Restrictions.disjunction();
                            orQ.add(Restrictions.ilike("noRegistrasi", q, MatchMode.ANYWHERE));
                            orQ.add(Restrictions.ilike("nama", q, MatchMode.ANYWHERE));
                            crit.add(orQ);
                        }

                        if (jur != null) {
                            crit.add(Restrictions.or(Restrictions.eq("prodiLulus", jur), Restrictions.eq("prodi1", jur)));
                        } else if (fak != null) {
                            crit.createAlias("prodiLulus", "pl", Criteria.LEFT_JOIN);
                            crit.createAlias("prodi1", "p1", Criteria.LEFT_JOIN);
                            crit.add(Restrictions.or(Restrictions.eq("pl.fakultas", fak), Restrictions.eq("p1.fakultas", fak)));
                        }
                        
                        calonList = ais.common.ConstantValues.simpleList(crit, BiodataCalonMahasiswa.class);
                    }

                    if (needMhs) {
                        Criteria c = masterSess.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                        c.add(Restrictions.between("tahunangkatan", angMulai, angSampai));
                        
                        if (q != null && !q.trim().isEmpty()) {
                            Disjunction orQ = Restrictions.disjunction();
                            orQ.add(Restrictions.ilike("nim", q, MatchMode.ANYWHERE));
                            orQ.add(Restrictions.ilike("nama", q, MatchMode.ANYWHERE));
                            c.add(orQ);
                        }
                        
                        if (jur != null) {
                            c.add(Restrictions.eq("jurusan", jur));
                        } else if (fak != null) {
                            c.createAlias("jurusan", "j", Criteria.LEFT_JOIN);
                            c.add(Restrictions.eq("j.fakultas", fak));
                        }
                        
                        mhsList = ais.common.ConstantValues.simpleList(c, Mahasiswa.class);
                    }

                    List<WorkItem> allItems = new ArrayList<WorkItem>();

                    // =======================================================================
                    // DISTRIBUSI DATA BERSDASARKAN MASING-MASING JENIS KEGIATAN
                    // =======================================================================
                    for (JenisKegiatan j : listJk) {
                        boolean isMaba = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && j.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
                                || (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null && j.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()));

                        if (isMaba) {
                            for(BiodataCalonMahasiswa b : calonList) allItems.add(new WorkItem("CALON", b.getId(), j.getId()));
                        } else {
                            for(Mahasiswa m : mhsList) allItems.add(new WorkItem("MHS", m.getId(), j.getId()));
                        }
                    }

                    // Bersihkan memory list entitas mentah
                    calonList.clear();
                    mhsList.clear();

                    int totalData = allItems.size();
                    state.totalData = totalData;
                    if (totalData == 0) { state.statusText = "Data kosong."; state.isDone = true; return; }

                    final int thn = Integer.parseInt(ta.split("/")[0]);
                    final int thnSampai = Integer.parseInt(taSampai.split("/")[0]);

                    // Merakit HTML Header Dinamis
                    state.htmlResult.append("<table class='table table-hover table-bordered align-middle mb-0' style='width: 100%; font-size: 0.85rem; white-space: nowrap;'>");
                    state.htmlResult.append("<thead class='table-dark text-center'><tr>");
                    state.htmlResult.append("<th>NIM / NO REG</th><th>NAMA</th><th>JENIS PEMBAYARAN</th><th>FAKULTAS</th><th>JURUSAN</th><th>STATUS</th><th>ANGKATAN</th>");
                    
                    int maxCols = 7;
                    for (int tahun = thn; tahun <= thnSampai; tahun++) {
                        if (ganjil) { state.htmlResult.append("<th>Tag. " + tahun + "1</th><th>Byr " + tahun + "1</th><th>Sisa " + tahun + "1</th>"); maxCols+=3; }
                        if (genap) { state.htmlResult.append("<th>Tag. " + tahun + "2</th><th>Byr " + tahun + "2</th><th>Sisa " + tahun + "2</th>"); maxCols+=3; }
                    }
                    state.htmlResult.append("<th>TOT TAGIHAN</th><th>TOT DIBAYAR</th><th>SISA PIUTANG</th><th>% LUNAS</th><th>RINCIAN CATATAN</th>");
                    state.htmlResult.append("</tr></thead><tbody class='bg-white'>");
                    
                    final int finalMaxCols = maxCols;

                    // =========================================================
                    // INJEKSI THREAD PEKERJA (WORKER POOL) DINAMIS
                    // =========================================================
                    int numWorkers = Math.min(maxWorkersConfig, totalData);
                    int chunkSize = (int) Math.ceil((double) totalData / numWorkers);
                    
                    ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
                    List<Callable<WorkerResult>> tasks = new ArrayList<Callable<WorkerResult>>();

                    state.statusText = "Mengeksekusi " + numWorkers + " Utas Pekerja (Thread) secara paralel...";

                    for (int w = 0; w < numWorkers; w++) {
                        int startIdx = w * chunkSize;
                        int endIdx = Math.min(startIdx + chunkSize, totalData);
                        if (startIdx >= endIdx) break; // Safeguard

                        final List<WorkItem> chunk = allItems.subList(startIdx, endIdx);
                        final int workerId = w;
                        
                        state.workerProgress.put(workerId, new int[]{0, chunk.size()});

                        tasks.add(new Callable<WorkerResult>() {
                            @Override
                            public WorkerResult call() {
                                WorkerResult res = new WorkerResult();
                                Session wSess = null;
                                DecimalFormat df = new DecimalFormat("#,###");
                                
                                try {
                                    wSess = HibernateUtil.openSession();

                                    for (int i = 0; i < chunk.size(); i++) {
                                        WorkItem item = chunk.get(i);
                                        // PENTING: Penarikan JenisKegiatan menggunakan ID dari masing-masing WorkItem
                                        JenisKegiatan wJk = (JenisKegiatan) wSess.get(JenisKegiatan.class, item.jkId);
                                        Double dibayarTotal = 0.0; Double terhutangTotal = 0.0;
                                        StringBuilder dNotes = new StringBuilder();

                                        res.html.append("<tr>");

                                        if (item.type.equals("MHS")) {
                                            Mahasiswa mhs = (Mahasiswa) wSess.get(Mahasiswa.class, item.id);
                                            res.html.append("<td class='fw-bold'>").append(mhs.getNim() != null ? mhs.getNim() : "-").append("</td>");
                                            res.html.append("<td>").append(mhs.getNama() != null ? mhs.getNama() : "-").append("</td>");
                                            res.html.append("<td>").append(wJk.getNamaKegiatan() != null ? wJk.getNamaKegiatan() : "-").append("</td>");
                                            res.html.append("<td>").append((mhs.getJurusan() != null && mhs.getJurusan().getFakultas() != null) ? mhs.getJurusan().getFakultas().getNama() : "-").append("</td>");
                                            res.html.append("<td>").append(mhs.getJurusan() != null ? mhs.getJurusan().getNama() : "-").append("</td>");
                                            res.html.append("<td>").append(mhs.getStatusAwalMahasiswa() != null ? mhs.getStatusAwalMahasiswa().getNama() : "-").append("</td>");
                                            res.html.append("<td class='text-center'>").append(mhs.getTahunangkatan() != null ? mhs.getTahunangkatan() : "-").append("</td>");

                                            for (int tahun = thn; tahun <= thnSampai; tahun++) {
                                                if (mhs.getTahunangkatan() != null && mhs.getTahunangkatan() <= tahun) {
                                                    String Ta = tahun + "/" + (tahun + 1);
                                                    if (ganjil) {
                                                        Integer smtInt = ais.common.Common.getSemester(mhs.getTahunangkatan(), Ta, Perkuliahan.GANJIL, mhs.getPindahKeKampusIniMasukSemester(), mhs.getSemesterMulai());
                                                        if (smtInt >= wJk.getMinSmt() && smtInt <= wJk.getMaxSmt()) {
                                                            Kegiatan k = KegiatanHelper.checkKegiatanMahasiswa(wJk, mhs, smtInt, Ta, ulang, false, null, wSess);
                                                            if (k != null) {
                                                                Double amt = k.getAmount() != null ? k.getAmount() : 0.0; Double terh = k.getAmountTerhutang() != null ? k.getAmountTerhutang() : 0.0;
                                                                res.html.append("<td class='text-end' data-t='n' data-v='").append(amt+terh).append("'>").append(df.format(amt + terh)).append("</td><td class='text-end text-success' data-t='n' data-v='").append(amt).append("'>").append(df.format(amt)).append("</td><td class='text-end text-danger' data-t='n' data-v='").append(terh).append("'>").append(df.format(terh)).append("</td>");
                                                                dibayarTotal += amt; terhutangTotal += terh;
                                                                if (k.getTagihans() != null && !k.getTagihans().isEmpty()) dNotes.append(k.getTagihans()).append("<br>");
                                                            } else { res.html.append("<td>-</td><td>-</td><td>-</td>"); }
                                                        } else { res.html.append("<td>-</td><td>-</td><td>-</td>"); }
                                                    }
                                                    if (genap) {
                                                        Integer smtInt = ais.common.Common.getSemester(mhs.getTahunangkatan(), Ta, Perkuliahan.GENAP, mhs.getPindahKeKampusIniMasukSemester(), mhs.getSemesterMulai());
                                                        if (smtInt >= wJk.getMinSmt() && smtInt <= wJk.getMaxSmt()) {
                                                            Kegiatan k = KegiatanHelper.checkKegiatanMahasiswa(wJk, mhs, smtInt, Ta, ulang, false, null, wSess);
                                                            if (k != null) {
                                                                Double amt = k.getAmount() != null ? k.getAmount() : 0.0; Double terh = k.getAmountTerhutang() != null ? k.getAmountTerhutang() : 0.0;
                                                                res.html.append("<td class='text-end' data-t='n' data-v='").append(amt+terh).append("'>").append(df.format(amt + terh)).append("</td><td class='text-end text-success' data-t='n' data-v='").append(amt).append("'>").append(df.format(amt)).append("</td><td class='text-end text-danger' data-t='n' data-v='").append(terh).append("'>").append(df.format(terh)).append("</td>");
                                                                dibayarTotal += amt; terhutangTotal += terh;
                                                                if (k.getTagihans() != null && !k.getTagihans().isEmpty()) dNotes.append(k.getTagihans()).append("<br>");
                                                            } else { res.html.append("<td>-</td><td>-</td><td>-</td>"); }
                                                        } else { res.html.append("<td>-</td><td>-</td><td>-</td>"); }
                                                    }
                                                } else {
                                                    if (ganjil) res.html.append("<td>-</td><td>-</td><td>-</td>");
                                                    if (genap) res.html.append("<td>-</td><td>-</td><td>-</td>");
                                                }
                                            }

                                        } else {
                                            BiodataCalonMahasiswa mhsCalon = (BiodataCalonMahasiswa) wSess.get(BiodataCalonMahasiswa.class, item.id);
                                            Jurusan jrs = mhsCalon.getProdiLulus() != null ? mhsCalon.getProdiLulus() : mhsCalon.getProdi1();
                                            res.html.append("<td class='fw-bold'>").append(mhsCalon.getNoRegistrasi() != null ? mhsCalon.getNoRegistrasi() : "-").append("</td>");
                                            res.html.append("<td>").append(mhsCalon.getNama() != null ? mhsCalon.getNama() : "-").append("</td>");
                                            res.html.append("<td>").append(wJk.getNamaKegiatan() != null ? wJk.getNamaKegiatan() : "-").append("</td>");
                                            res.html.append("<td>").append((jrs != null && jrs.getFakultas() != null) ? jrs.getFakultas().getNama() : "-").append("</td>");
                                            res.html.append("<td>").append(jrs != null ? jrs.getNama() : "-").append("</td>");
                                            res.html.append("<td>").append(mhsCalon.getStatusAwalMahasiswa() != null ? mhsCalon.getStatusAwalMahasiswa().getNama() : "-").append("</td>");
                                            res.html.append("<td class='text-center'>").append(mhsCalon.getTahun() != null ? mhsCalon.getTahun() : "-").append("</td>");

                                            for (int tahun = thn; tahun <= thnSampai; tahun++) {
                                                if (mhsCalon.getTahun() != null && mhsCalon.getTahun().equals(tahun)) {
                                                    String Ta = tahun + "/" + (tahun + 1);
                                                    if (ganjil) {
                                                        int smtInt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && wJk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) ? 0 : 1;
                                                        Kegiatan k = KegiatanHelper.checkKegiatanCalonMahasiswa(wJk, mhsCalon, smtInt, Ta, ulang, false, null, wSess);
                                                        if (k != null) {
                                                            Double amt = k.getAmount() != null ? k.getAmount() : 0.0; Double terh = k.getAmountTerhutang() != null ? k.getAmountTerhutang() : 0.0;
                                                            res.html.append("<td class='text-end' data-t='n' data-v='").append(amt+terh).append("'>").append(df.format(amt + terh)).append("</td><td class='text-end text-success' data-t='n' data-v='").append(amt).append("'>").append(df.format(amt)).append("</td><td class='text-end text-danger' data-t='n' data-v='").append(terh).append("'>").append(df.format(terh)).append("</td>");
                                                            dibayarTotal += amt; terhutangTotal += terh;
                                                            if (k.getTagihans() != null && !k.getTagihans().isEmpty()) dNotes.append(k.getTagihans()).append("<br>");
                                                        } else { res.html.append("<td>-</td><td>-</td><td>-</td>"); }
                                                    }
                                                    if (genap) {
                                                        int smtInt = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null && wJk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) ? 0 : 2;
                                                        Kegiatan k = KegiatanHelper.checkKegiatanCalonMahasiswa(wJk, mhsCalon, smtInt, Ta, ulang, false, null, wSess);
                                                        if (k != null) {
                                                            Double amt = k.getAmount() != null ? k.getAmount() : 0.0; Double terh = k.getAmountTerhutang() != null ? k.getAmountTerhutang() : 0.0;
                                                            res.html.append("<td class='text-end' data-t='n' data-v='").append(amt+terh).append("'>").append(df.format(amt + terh)).append("</td><td class='text-end text-success' data-t='n' data-v='").append(amt).append("'>").append(df.format(amt)).append("</td><td class='text-end text-danger' data-t='n' data-v='").append(terh).append("'>").append(df.format(terh)).append("</td>");
                                                            dibayarTotal += amt; terhutangTotal += terh;
                                                            if (k.getTagihans() != null && !k.getTagihans().isEmpty()) dNotes.append(k.getTagihans()).append("<br>");
                                                        } else { res.html.append("<td>-</td><td>-</td><td>-</td>"); }
                                                    }
                                                } else {
                                                    if (ganjil) res.html.append("<td>-</td><td>-</td><td>-</td>");
                                                    if (genap) res.html.append("<td>-</td><td>-</td><td>-</td>");
                                                }
                                            }
                                        }

                                        double pLunas = (dibayarTotal + terhutangTotal) > 0 ? (dibayarTotal * 100.0) / (dibayarTotal + terhutangTotal) : 0.0;
                                        
                                        res.tagihan += (dibayarTotal + terhutangTotal); 
                                        res.dibayar += dibayarTotal; 
                                        res.piutang += terhutangTotal;

                                        res.html.append("<td class='text-end fw-bold' data-t='n' data-v='").append(dibayarTotal+terhutangTotal).append("'>").append(df.format(dibayarTotal + terhutangTotal)).append("</td>");
                                        res.html.append("<td class='text-end fw-bold text-success' data-t='n' data-v='").append(dibayarTotal).append("'>").append(df.format(dibayarTotal)).append("</td>");
                                        res.html.append("<td class='text-end fw-bold text-danger' data-t='n' data-v='").append(terhutangTotal).append("'>").append(df.format(terhutangTotal)).append("</td>");
                                        res.html.append("<td class='text-center fw-bold' data-t='n' data-v='").append(pLunas).append("'>").append(new DecimalFormat("#.##").format(pLunas)).append("%</td>");
                                        res.html.append("<td><small class='text-muted'>").append(dNotes.toString()).append("</small></td>");
                                        res.html.append("</tr>");

                                        state.workerProgress.get(workerId)[0] = i + 1;
                                        state.currentProses.incrementAndGet();

                                        if (i > 0 && i % 25 == 0) { wSess.flush(); wSess.clear(); }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:437");
                                } finally {
                                    if (wSess != null && wSess.isOpen()) { try { wSess.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:439");} try { wSess.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:439");} }
                                    try { HibernateUtil.closeSessionQuietly(wSess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:440");}
                                }
                                return res;
                            }
                        });
                    }

                    List<Future<WorkerResult>> futures = executor.invokeAll(tasks);
                    executor.shutdown();

                    Double grandTagihan = 0.0, grandDibayar = 0.0, grandPiutang = 0.0;
                    
                    for (Future<WorkerResult> future : futures) {
                        WorkerResult wRes = future.get();
                        state.htmlResult.append(wRes.html);
                        grandTagihan += wRes.tagihan;
                        grandDibayar += wRes.dibayar;
                        grandPiutang += wRes.piutang;
                    }

                    state.htmlResult.append("</tbody>");
                    state.htmlResult.append("<tfoot class='table-secondary'><tr><td colspan='").append(finalMaxCols).append("' class='text-end fw-bold text-uppercase'>Grand Total Seluruh Data Hitung Ulang:</td>");
                    DecimalFormat df = new DecimalFormat("#,###");
                    state.htmlResult.append("<td class='text-end fw-bold fs-6' data-t='n' data-v='").append(grandTagihan).append("'>").append(df.format(grandTagihan)).append("</td>");
                    state.htmlResult.append("<td class='text-end fw-bold fs-6 text-success' data-t='n' data-v='").append(grandDibayar).append("'>").append(df.format(grandDibayar)).append("</td>");
                    state.htmlResult.append("<td class='text-end fw-bold fs-6 text-danger' data-t='n' data-v='").append(grandPiutang).append("'>").append(df.format(grandPiutang)).append("</td>");
                    double finalPersen = grandTagihan > 0 ? (grandDibayar * 100.0) / grandTagihan : 0.0;
                    state.htmlResult.append("<td class='text-center fw-bold' data-t='n' data-v='").append(finalPersen).append("'>").append(new DecimalFormat("#.##").format(finalPersen)).append("%</td><td></td></tr></tfoot></table>");

                    state.statusText = "Selesai: Berhasil Mengkalkulasi Ulang " + totalData + " Identitas dalam " + numWorkers + " Proses Paralel.";
                    state.isDone = true;

                } catch (Exception e) {
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:473");
                    state.statusText = "Gagal memproses data.";
                    state.isDone = true;
                } finally {
                    if (masterSess != null && masterSess.isOpen()) { try { masterSess.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:477");} try { masterSess.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:477");} }
                    try { HibernateUtil.closeSessionQuietly(wSess); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pembayaran/_service_proses_tagihan.jsp:478");}
                }
            }
        }).start();

        out.print("{\"status\":\"started\", \"jobId\":\"" + jobId + "\"}"); out.flush(); return;
    }
%>