package ais.action.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.repository.RepositoryPublicService;
import ais.action.master.repository.RepositoryPublicService.ItemCard;
import ais.action.master.repository.RepositoryPublicService.ItemDetail;
import ais.action.master.repository.RepositoryPublicService.AuthorProfile;
import ais.action.master.repository.RepositoryPublicService.CollectionView;
import ais.action.master.repository.RepositoryPublicService.Query;
import ais.action.master.repository.RepositoryPublicService.SearchResult;
import ais.action.master.repository.RepositoryPublicService.Suggestion;
import ais.action.master.repository.RepositoryWorkflowService;
import ais.common.Common;
import ais.common.security.PublicRegistrationRateLimiter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.Tbmuser;

/** Public entry point and typed read API for Repository AIS. */
public class Repository extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String JSP = "/WEB-INF/baru/modul/repository/landing_page.jsp";
    private static final String CSRF = "repository.public.csrf";
    private final RepositoryPublicService service = new RepositoryPublicService();
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processSafely(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processSafely(request, response);
    }

    private void processSafely(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String requestId = Long.toHexString(System.currentTimeMillis()) + "-"
                + Integer.toHexString(System.identityHashCode(request));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("X-Request-Id", requestId);
        try {
            request.setCharacterEncoding("UTF-8");
            process(request, response, requestId);
        } catch (SecurityException e) {
            if (!response.isCommitted()) renderState(request,response,HttpServletResponse.SC_FORBIDDEN,"Akses tidak diizinkan",e.getMessage(),requestId);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.recordVisibleFailure(e,
                    "Repository public servlet", request, requestId);
            if (!response.isCommitted()) {
                if (isJsonRequest(request)) {
                    writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "INTERNAL_ERROR", "Repository belum dapat melayani permintaan ini.", requestId);
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Repository belum dapat melayani permintaan ini. ID: " + requestId);
                }
            }
        } finally {
            // This servlet runs outside ZK's OpenSessionInView lifecycle. The
            // repository service therefore uses a native ThreadLocal session,
            // which must be returned to the pool after every request.
            HibernateUtil.closeSession();
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response, String requestId) throws Exception {
        String action = clean(request.getParameter("action")).toLowerCase();
        String view = clean(request.getParameter("view")).toLowerCase();
        Long routeId = null;
        String routeAuthor = "";
        String path = clean(request.getPathInfo());
        if (path.length() > 0 && !"/".equals(path)) {
            String[] segments = path.split("/");
            if (path.equals("/search") || path.startsWith("/browse")) view = "search";
            else if (path.equals("/policies") || path.startsWith("/policies/")) view = "policies";
            else if (path.equals("/help")) view = "help";
            else if (path.equals("/rss/recent")) action = "feed";
            else if (path.equals("/sitemap.xml")) { sitemap(request,response); return; }
            else if (path.equals("/oai/request")) { response.sendRedirect(request.getContextPath()+"/oai"+(request.getQueryString()==null?"":"?"+request.getQueryString())); return; }
            else if (segments.length > 2 && "item".equals(segments[1])) { view="item"; routeId=parseLong(segments[2]); }
            else if (segments.length > 2 && "collection".equals(segments[1])) { view="collection"; routeId=parseLong(segments[2]); request.setAttribute("repoRouteCollectionId",routeId); }
            else if (segments.length > 2 && "author".equals(segments[1])) { view="author"; routeAuthor=URLDecoder.decode(segments[2],"UTF-8"); }
        }
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        if (("search".equals(action) || "suggest".equals(action) || "search".equals(view) || "browse".equals(view))
                && !PublicRegistrationRateLimiter.izinkan("repository-search|" + ip, 300, 3600000L)) {
            tooManyRequests(response, requestId); return;
        }
        if ("download".equals(action)
                && !PublicRegistrationRateLimiter.izinkan("repository-download|" + ip, 120, 3600000L)) {
            tooManyRequests(response, requestId); return;
        }
        if (request.getServletPath().endsWith("robots.txt")) { robots(request, response); return; }
        if (request.getServletPath().endsWith("sitemap.xml")) { sitemap(request, response); return; }
        if ("search".equals(action)) {
            writeSearchJson(response, service.search(queryFrom(request)), requestId);
            return;
        }
        if ("suggest".equals(action)) {
            writeSuggestionJson(response, service.suggest(clean(request.getParameter("q")),
                    clean(request.getParameter("field")), 8), requestId);
            return;
        }
        if ("bookmark".equals(action) || "savesearch".equals(action) || "removepreference".equals(action)) {
            Tbmuser actor=Common.getCurrentUser(request);if(actor==null){renderState(request,response,HttpServletResponse.SC_UNAUTHORIZED,"Sesi telah berakhir","Silakan masuk kembali untuk melanjutkan.",requestId);return;}
            if(!"POST".equalsIgnoreCase(request.getMethod())){response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);return;}
            verifyPublicCsrf(request.getSession(false),request.getParameter("csrf"));
            if("bookmark".equals(action)){Long itemId=parseLong(request.getParameter("id"));service.toggleBookmark(actor.getUserId(),itemId);response.sendRedirect(request.getContextPath()+"/repository/item/"+itemId);return;}
            if("savesearch".equals(action)){service.saveSearch(actor.getUserId(),clean(request.getParameter("label")),safeRepositoryUrl(request,request.getParameter("queryValue")),"true".equalsIgnoreCase(request.getParameter("alert")));response.sendRedirect(safeRepositoryUrl(request,request.getParameter("returnTo")));return;}
            service.removePreference(actor.getUserId(),parseLong(request.getParameter("preferenceId")));response.sendRedirect(request.getContextPath()+"/repository");return;
        }
        if ("download".equals(action)) {
            download(request, response);
            return;
        }
        if ("citation".equals(action)) {
            citation(request, response);
            return;
        }
        if ("feed".equals(action)) {
            feed(request, response);
            return;
        }
        if ("oai".equals(action)) {
            oai(request, response);
            return;
        }

        if (view.length() == 0) view = "home";
        request.setAttribute("repoView", view);
        Tbmuser publicUser = Common.getCurrentUser(request);
        if(publicUser==null&&"GET".equalsIgnoreCase(request.getMethod()))response.setHeader("X-Repository-Cacheable","public");
        HttpSession publicSession=request.getSession(true);String publicCsrf=(String)publicSession.getAttribute(CSRF);if(publicCsrf==null){publicCsrf=UUID.randomUUID().toString()+UUID.randomUUID().toString();publicSession.setAttribute(CSRF,publicCsrf);}request.setAttribute("repoCsrf",publicCsrf);
        request.setAttribute("repoPublicUser", publicUser);
        request.setAttribute("repoIsAdmin",Boolean.valueOf(workflow.isRepositoryAdministrator(publicUser)));
        request.setAttribute("repoIsReviewer", Boolean.valueOf(workflow.isRepositoryAdmin(publicUser)&&!workflow.isRepositoryAdministrator(publicUser)));
        request.setAttribute("repoCanDeposit", Boolean.valueOf(workflow.canDeposit(publicUser) && service.hasDepositCollection()));

        if ("item".equals(view)) {
            Long itemId=routeId==null?parseLong(request.getParameter("id")):routeId;
            ItemDetail detail = service.findPublicItem(itemId);
            if (detail == null) {
                detail = service.findTombstone(itemId);
                if (detail == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Publikasi tidak ditemukan."); return; }
            }
            request.setAttribute("repoItem", detail);
            if(publicUser!=null){boolean bookmarked=false;for(RepositoryPublicService.PreferenceView p:service.preferences(publicUser.getUserId(),"BOOKMARK",100))if(detail.id.equals(p.itemId)){bookmarked=true;break;}request.setAttribute("repoBookmarked",Boolean.valueOf(bookmarked));}
            if(!detail.withdrawn){service.recordUsage(detail.id, null, "VIEW", request.getRemoteAddr(), request.getHeader("User-Agent"), actorId(request));detail.viewCount++;}
        } else if ("search".equals(view) || "browse".equals(view)) {
            request.setAttribute("repoSearch", service.search(queryFrom(request)));
            if(publicUser!=null){request.setAttribute("repoSavedSearches",service.preferences(publicUser.getUserId(),"SAVED_SEARCH",20));request.setAttribute("repoSearchAlerts",service.preferences(publicUser.getUserId(),"SEARCH_ALERT",20));}
        } else if ("collection".equals(view)) {
            CollectionView collection=service.findCollection(routeId==null?parseLong(request.getParameter("id")):routeId);
            if(collection==null){response.sendError(HttpServletResponse.SC_NOT_FOUND,"Koleksi tidak ditemukan.");return;}
            request.setAttribute("repoCollection",collection); request.setAttribute("repoSearch",service.search(queryFrom(request)));
        } else if ("author".equals(view)) {
            String name=routeAuthor.length()==0?clean(request.getParameter("name")):routeAuthor;
            AuthorProfile author=service.authorProfile(name);if(author==null){response.sendError(HttpServletResponse.SC_NOT_FOUND,"Profil penulis tidak ditemukan.");return;}
            request.setAttribute("repoAuthor",author);
        } else if ("policies".equals(view) || "help".equals(view)) {
            // Rendered by the allow-listed JSP view.
        } else {
            request.setAttribute("repoView", "home");
            request.setAttribute("repoSummary", service.loadSummary());
            request.setAttribute("repoCollections", service.listCollections(12));
            request.setAttribute("repoLatest", service.latest(6));
            request.setAttribute("repoPopularCollections", service.popularCollections(6));
            request.setAttribute("repoPopularTopics", service.popularSubjects(10));
            if(publicUser!=null){request.setAttribute("repoRecommendations",service.recommendations(publicUser.getUserId(),4));request.setAttribute("repoSearchAlerts",service.preferences(publicUser.getUserId(),"SEARCH_ALERT",10));}
        }
        request.getRequestDispatcher(JSP).forward(request, response);
    }

    private Query queryFrom(HttpServletRequest request) {
        Query q = new Query();
        q.keyword = clean(request.getParameter("q"));
        q.searchField = clean(request.getParameter("field"));
        q.author = clean(request.getParameter("author"));
        q.subject = clean(request.getParameter("subject"));
        q.language = clean(request.getParameter("language"));
        q.identifier = clean(request.getParameter("identifier"));
        q.programStudy = clean(request.getParameter("program"));
        q.searchScope = clean(request.getParameter("scope"));
        q.fullText = clean(request.getParameter("fullText"));
        q.collectionId = request.getAttribute("repoRouteCollectionId") instanceof Long
                ? (Long)request.getAttribute("repoRouteCollectionId") : parseLong(request.getParameter("collection"));
        q.documentType = clean(request.getParameter("type"));
        q.accessPolicy = clean(request.getParameter("access"));
        q.year = parseInteger(request.getParameter("year"));
        q.sort = clean(request.getParameter("sort"));
        Integer page = parseInteger(request.getParameter("page"));
        Integer size = parseInteger(request.getParameter("size"));
        q.page = page == null ? 1 : page.intValue();
        q.pageSize = size == null ? RepositoryPublicService.DEFAULT_PAGE_SIZE : size.intValue();
        return service.normalize(q);
    }

    private void writeSearchJson(HttpServletResponse response, SearchResult result, String requestId) throws Exception {
        JSONObject root = new JSONObject();
        root.put("status", "OK");
        root.put("requestId", requestId);
        root.put("page", result.query.page);
        root.put("pageSize", result.query.pageSize);
        root.put("total", result.total);
        root.put("totalPages", result.totalPages);
        root.put("searchField", result.query.searchField);
        JSONArray items = new JSONArray();
        for (int i = 0; i < result.items.size(); i++) {
            ItemCard item = result.items.get(i);
            JSONObject row = new JSONObject();
            row.put("id", item.id);
            row.put("title", item.title);
            row.put("authors", item.authors);
            row.put("abstract", item.abstractText);
            row.put("year", item.year);
            row.put("documentType", item.documentType);
            row.put("accessPolicy", item.accessPolicy);
            row.put("collection", item.collectionName);
            row.put("oaiIdentifier", item.oaiIdentifier);
            row.put("programStudy", item.programStudy);
            row.put("publicFileCount", item.publicFileCount);
            row.put("pdfAvailable", item.pdfAvailable);
            row.put("superseded",item.superseded);
            row.put("viewCount", item.viewCount);
            row.put("downloadCount", item.downloadCount);
            items.put(row);
        }
        root.put("items", items);
        JSONObject facets = new JSONObject();
        facets.put("type", new JSONObject(result.typeFacets));
        facets.put("access", new JSONObject(result.accessFacets));
        facets.put("year", new JSONObject(result.yearFacets));
        facets.put("author",new JSONObject(result.authorFacets));facets.put("subject",new JSONObject(result.subjectFacets));
        facets.put("language",new JSONObject(result.languageFacets));facets.put("program",new JSONObject(result.programFacets));
        facets.put("source",new JSONObject(result.sourceFacets));facets.put("license",new JSONObject(result.licenseFacets));facets.put("fullText",new JSONObject(result.fullTextFacets));
        root.put("facets", facets);
        writeJson(response, root, HttpServletResponse.SC_OK);
    }

    private void writeSuggestionJson(HttpServletResponse response, List<Suggestion> suggestions, String requestId) throws Exception {
        JSONObject root = new JSONObject(); root.put("status", "OK"); root.put("requestId", requestId);
        JSONArray rows = new JSONArray();
        for (Suggestion suggestion : suggestions) {
            JSONObject row = new JSONObject(); row.put("type", suggestion.type); row.put("label", suggestion.label);
            row.put("detail", suggestion.detail); row.put("value", suggestion.value); rows.put(row);
        }
        root.put("suggestions", rows); writeJson(response, root, HttpServletResponse.SC_OK);
    }

    private void verifyPublicCsrf(HttpSession session,String supplied){String expected=session==null?null:(String)session.getAttribute(CSRF);if(!constantTime(expected,supplied))throw new SecurityException("Token keamanan tidak valid atau sesi telah berakhir.");}
    private boolean constantTime(String a,String b){if(a==null||b==null)return false;int diff=a.length()^b.length(),n=Math.min(a.length(),b.length());for(int i=0;i<n;i++)diff|=a.charAt(i)^b.charAt(i);return diff==0;}
    private String safeRepositoryUrl(HttpServletRequest request,String value){String context=request.getContextPath(),v=clean(value);if(v.startsWith(context+"/repository"))return v;if(v.startsWith("/repository"))return context+v;return context+"/repository";}
    private void renderState(HttpServletRequest request,HttpServletResponse response,int status,String title,String message,String requestId)throws ServletException,IOException{response.setStatus(status);request.setAttribute("repoView","state");request.setAttribute("repoStateCode",Integer.valueOf(status));request.setAttribute("repoStateTitle",title);request.setAttribute("repoStateMessage",message);request.setAttribute("repoRequestId",requestId);request.setAttribute("repoPublicUser",Common.getCurrentUser(request));request.getRequestDispatcher(JSP).forward(request,response);}

    private void citation(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ItemDetail item = service.findPublicItem(parseLong(request.getParameter("id")));
        if (item == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Publikasi tidak ditemukan.");
            return;
        }
        String format = clean(request.getParameter("format")).toLowerCase();
        if (!"ris".equals(format) && !"bibtex".equals(format) && !"endnote".equals(format)
                && !"csl".equals(format) && !"dcxml".equals(format) && !"apa".equals(format)
                && !"ieee".equals(format) && !"harvard".equals(format) && !"vancouver".equals(format)
                && !"chicago".equals(format) && !"text".equals(format)) format = "text";
        String body = service.citation(item, format);
        String extension = "bibtex".equals(format) ? "bib" : ("ris".equals(format) ? "ris"
                : ("endnote".equals(format) ? "enw" : ("csl".equals(format) ? "json" : ("dcxml".equals(format)?"xml":"txt"))));
        response.setContentType("csl".equals(format)
                ? "application/vnd.citationstyles.csl+json;charset=UTF-8" : ("dcxml".equals(format)?"application/xml;charset=UTF-8":"text/plain;charset=UTF-8"));
        response.setHeader("Content-Disposition", "attachment; filename=repository-" + item.id + "." + extension);
        response.getWriter().write(body);
    }

    private void download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RepoBitstream bitstream = service.findDownloadableBitstream(parseLong(request.getParameter("id")));
        if (bitstream == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Berkas tidak ditemukan atau tidak dapat diakses.");
            return;
        }
        File file = service.resolveBitstreamFile(bitstream);
        if (file == null || !file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Berkas fisik belum tersedia.");
            return;
        }
        service.recordUsage(bitstream.getItemId(), bitstream.getId(), "DOWNLOAD", request.getRemoteAddr(),
                request.getHeader("User-Agent"), actorId(request));
        String fileName = safeFileName(bitstream.getNamaFile());
        String mime = clean(bitstream.getMimeType());
        if (mime.length() == 0) mime = getServletContext().getMimeType(fileName);
        if (mime == null || mime.length() == 0) mime = "application/octet-stream";
        response.reset();
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentType(mime);
        response.setHeader("Content-Length", String.valueOf(file.length()));
        boolean inline = "true".equalsIgnoreCase(request.getParameter("inline")) && "application/pdf".equalsIgnoreCase(mime);
        response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + fileName.replace("\"", "")
                + "\"; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));

        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        OutputStream output = response.getOutputStream();
        try {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            output.flush();
        } finally {
            input.close();
        }
    }

    private void oai(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String verb = clean(request.getParameter("verb"));
        String token = clean(request.getParameter("resumptionToken"));
        if (verb.length() == 0 && token.length() > 0) verb = clean(request.getParameter("oaiVerb"));
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String requestUrl = request.getRequestURL().toString();
        String base = requestUrl.endsWith("/oai") ? requestUrl : requestUrl + "?action=oai";
        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.print("<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">");
        out.print("<responseDate>" + xmlDate(new Date()) + "</responseDate><request verb=\"" + xml(verb) + "\">" + xml(base) + "</request>");

        if ("Identify".equals(verb)) {
            out.print("<Identify><repositoryName>Repository AIS</repositoryName><baseURL>" + xml(base)
                    + "</baseURL><protocolVersion>2.0</protocolVersion><adminEmail>repository@localhost</adminEmail>"
                    + "<earliestDatestamp>1970-01-01T00:00:00Z</earliestDatestamp><deletedRecord>transient</deletedRecord>"
                    + "<granularity>YYYY-MM-DDThh:mm:ssZ</granularity></Identify>");
        } else if ("ListMetadataFormats".equals(verb)) {
            out.print("<ListMetadataFormats><metadataFormat><metadataPrefix>oai_dc</metadataPrefix>"
                    + "<schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>"
                    + "<metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace>"
                    + "</metadataFormat></ListMetadataFormats>");
        } else if ("ListSets".equals(verb)) {
            List<RepositoryPublicService.CollectionView> sets = service.listCollections(500);
            out.print("<ListSets>");
            for (int i = 0; i < sets.size(); i++) {
                RepositoryPublicService.CollectionView set = sets.get(i);
                out.print("<set><setSpec>collection:" + set.id + "</setSpec><setName>" + xml(set.nama) + "</setName></set>");
            }
            out.print("</ListSets>");
        } else if ("GetRecord".equals(verb)) {
            if (!validOaiPrefix(request.getParameter("metadataPrefix"))) {
                oaiError(out, "cannotDisseminateFormat", "Metadata prefix harus oai_dc.");
            } else {
                ItemDetail item = service.findOaiItemByIdentifier(request.getParameter("identifier"));
                if (item == null) oaiError(out, "idDoesNotExist", "Identifier tidak ditemukan.");
                else {
                    out.print("<GetRecord>");
                    writeOaiRecord(out, item, true);
                    out.print("</GetRecord>");
                }
            }
        } else if ("ListIdentifiers".equals(verb) || "ListRecords".equals(verb)) {
            if (token.length() == 0 && !validOaiPrefix(request.getParameter("metadataPrefix"))) {
                oaiError(out, "cannotDisseminateFormat", "Metadata prefix harus oai_dc.");
            } else {
                int page = parseOaiPage(token);
                if (page < 1) {
                    oaiError(out, "badResumptionToken", "Resumption token tidak valid.");
                } else {
                    Query q = new Query();
                    q.page = page;
                    q.pageSize = 50;
                    Long setId = token.length() == 0 ? parseSet(request.getParameter("set")) : parseOaiTokenLong(token, "s");
                    Date from = token.length() == 0 ? parseOaiDate(request.getParameter("from"), false) : parseOaiTokenDate(token, "f");
                    Date until = token.length() == 0 ? parseOaiDate(request.getParameter("until"), true) : parseOaiTokenDate(token, "u");
                    boolean invalidSet = token.length() == 0 && clean(request.getParameter("set")).length() > 0 && setId == null;
                    boolean invalidFrom = token.length() == 0 && clean(request.getParameter("from")).length() > 0 && from == null;
                    boolean invalidUntil = token.length() == 0 && clean(request.getParameter("until")).length() > 0 && until == null;
                    if (invalidSet || invalidFrom || invalidUntil || (from != null && until != null && from.after(until))) {
                        oaiError(out, "badArgument", "Parameter set/from/until tidak valid.");
                    } else {
                        q.collectionId = setId;
                        q.modifiedFrom = from;
                        q.modifiedUntil = until;
                        SearchResult result = service.search(q);
                        if (result.items.isEmpty() && page == 1) {
                            oaiError(out, "noRecordsMatch", "Tidak ada record publik yang sesuai.");
                        } else {
                            out.print("<" + verb + ">");
                            for (int i = 0; i < result.items.size(); i++) {
                                ItemDetail item = service.findPublicItem(result.items.get(i).id);
                                writeOaiRecord(out, item, "ListRecords".equals(verb));
                            }
                            String nextToken = page < result.totalPages
                                    ? buildOaiToken(page + 1, setId, from, until) : "";
                            out.print("<resumptionToken completeListSize=\"" + result.total + "\" cursor=\""
                                    + ((page - 1) * q.pageSize) + "\">" + xml(nextToken) + "</resumptionToken>");
                            out.print("</" + verb + ">");
                        }
                    }
                }
            }
        } else {
            oaiError(out, "badVerb", "Verb OAI-PMH tidak dikenal.");
        }
        out.print("</OAI-PMH>");
        out.flush();
    }

    private void writeOaiRecord(PrintWriter out, ItemDetail item, boolean includeMetadata) {
        if (item == null) return;
        if (includeMetadata) out.print("<record>");
        out.print("<header" + (item.withdrawn ? " status=\"deleted\"" : "") + "><identifier>" + xml(item.oaiIdentifier) + "</identifier><datestamp>"
                + xmlDate(item.datestamp == null ? new Date(0L) : item.datestamp) + "</datestamp>"
                + "<setSpec>collection:" + item.collectionId + "</setSpec></header>");
        if (includeMetadata && !item.withdrawn) {
            out.print("<metadata><oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
            out.print("<dc:title>" + xml(item.title) + "</dc:title>");
            String[] authors = item.authors == null ? new String[0] : item.authors.split(";");
            for (int i = 0; i < authors.length; i++) if (authors[i].trim().length() > 0) out.print("<dc:creator>" + xml(authors[i].trim()) + "</dc:creator>");
            if (item.abstractText.length() > 0) out.print("<dc:description>" + xml(item.abstractText) + "</dc:description>");
            if (item.subjects.length() > 0) out.print("<dc:subject>" + xml(item.subjects) + "</dc:subject>");
            if (item.publisher.length() > 0) out.print("<dc:publisher>" + xml(item.publisher) + "</dc:publisher>");
            if (item.year.length() > 0) out.print("<dc:date>" + xml(item.year) + "</dc:date>");
            out.print("<dc:type>" + xml(item.documentType) + "</dc:type><dc:language>" + xml(item.language)
                    + "</dc:language><dc:identifier>" + xml(item.oaiIdentifier) + "</dc:identifier>");
            if (item.dspaceHandle.length() > 0) out.print("<dc:identifier>" + xml(item.dspaceHandle) + "</dc:identifier>");
            out.print("<dc:rights>" + xml(item.accessPolicy) + "</dc:rights></oai_dc:dc></metadata>");
        }
        if (includeMetadata) out.print("</record>");
    }

    private void robots(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("text/plain;charset=UTF-8");
        String origin = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
        response.getWriter().print("User-agent: *\nAllow: " + request.getContextPath()
                + "/repository\nSitemap: " + origin + request.getContextPath() + "/sitemap.xml\n");
    }

    private void sitemap(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/xml;charset=UTF-8");
        String origin = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort())
                + request.getContextPath();
        PrintWriter out = response.getWriter();
        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>"
                + xml(origin + "/repository") + "</loc></url>");
        Query q = new Query(); q.pageSize = RepositoryPublicService.MAX_PAGE_SIZE; q.page = 1;
        SearchResult page;
        do {
            page = service.search(q);
            for (ItemCard item : page.items)
                out.print("<url><loc>" + xml(origin + "/repository/item/" + item.id) + "</loc></url>");
            q.page++;
        } while (q.page <= page.totalPages);
        out.print("</urlset>");
    }

    private void feed(HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean atom = "atom".equalsIgnoreCase(clean(request.getParameter("format")));
        String base = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort())
                + request.getContextPath();
        Query feedQuery=queryFrom(request);feedQuery.page=1;feedQuery.pageSize=20;feedQuery.sort="newest";
        List<ItemCard> items = service.search(feedQuery).items;
        response.setContentType((atom ? "application/atom+xml" : "application/rss+xml") + ";charset=UTF-8");
        PrintWriter out = response.getWriter();
        if (atom) {
            out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><title>Publikasi terbaru Repository AIS</title><id>" + xml(base + "/repository") + "</id><updated>" + xmlDate(new Date()) + "</updated>");
            for (ItemCard item : items) out.print("<entry><title>" + xml(item.title) + "</title><id>" + xml(item.oaiIdentifier) + "</id><link href=\"" + xml(base + "/repository/item/" + item.id) + "\"/><updated>" + xmlDate(item.issuedAt) + "</updated><summary>" + xml(item.abstractText) + "</summary></entry>");
            out.print("</feed>");
        } else {
            out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><title>Publikasi terbaru Repository AIS</title><link>" + xml(base + "/repository") + "</link><description>Karya ilmiah terbaru yang tersedia untuk publik.</description>");
            for (ItemCard item : items) out.print("<item><title>" + xml(item.title) + "</title><guid isPermaLink=\"false\">" + xml(item.oaiIdentifier) + "</guid><link>" + xml(base + "/repository/item/" + item.id) + "</link><description>" + xml(item.abstractText) + "</description><pubDate>" + rfc822(item.issuedAt) + "</pubDate></item>");
            out.print("</channel></rss>");
        }
    }

    private String rfc822(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date == null ? new Date(0L) : date);
    }

    private void tooManyRequests(HttpServletResponse response, String requestId) throws IOException {
        response.setHeader("Retry-After", "3600");
        writeJsonError(response, 429, "RATE_LIMITED",
                "Terlalu banyak permintaan. Silakan coba kembali beberapa saat lagi.", requestId);
    }

    private String actorId(HttpServletRequest request) {
        try { ais.database.model.Tbmuser u = Common.getCurrentUser(request); return u == null ? "" : u.getUserId(); }
        catch (Exception e) { return ""; }
    }

    private boolean validOaiPrefix(String value) {
        return "oai_dc".equals(clean(value));
    }

    private int parseOaiPage(String token) {
        if (token == null || token.length() == 0) return 1;
        if (token.indexOf(';') >= 0 && !token.matches("p[1-9][0-9]*;s[0-9]+;f[0-9]+;u[0-9]+")) return -1;
        String pageToken = token.indexOf(';') < 0 ? token : token.substring(0, token.indexOf(';'));
        if (!pageToken.matches("p[1-9][0-9]*")) return -1;
        try { return Integer.parseInt(pageToken.substring(1)); } catch (Exception e) { return -1; }
    }

    private String buildOaiToken(int page, Long setId, Date from, Date until) {
        return "p" + page + ";s" + (setId == null ? 0L : setId.longValue())
                + ";f" + (from == null ? 0L : from.getTime())
                + ";u" + (until == null ? 0L : until.getTime());
    }

    private Long parseOaiTokenLong(String token, String key) {
        if (token == null || !token.matches("p[1-9][0-9]*;s[0-9]+;f[0-9]+;u[0-9]+")) return null;
        String[] parts = token.split(";");
        for (int i = 1; i < parts.length; i++) if (parts[i].startsWith(key)) {
            try { long value = Long.parseLong(parts[i].substring(1)); return value <= 0L ? null : Long.valueOf(value); }
            catch (Exception ignored) { return null; }
        }
        return null;
    }

    private Date parseOaiTokenDate(String token, String key) {
        Long value = parseOaiTokenLong(token, key);
        return value == null ? null : new Date(value.longValue());
    }

    private Date parseOaiDate(String value, boolean endOfDay) {
        String text = clean(value);
        if (text.length() == 0) return null;
        String pattern = text.length() == 10 ? "yyyy-MM-dd" : "yyyy-MM-dd'T'HH:mm:ss'Z'";
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = format.parse(text);
            return endOfDay && text.length() == 10 ? new Date(parsed.getTime() + 86399999L) : parsed;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseSet(String set) {
        String value = clean(set);
        return value.startsWith("collection:") ? parseLong(value.substring("collection:".length())) : null;
    }

    private void oaiError(PrintWriter out, String code, String message) {
        out.print("<error code=\"" + xml(code) + "\">" + xml(message) + "</error>");
    }

    private String xmlDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date == null ? new Date(0L) : date);
    }

    private String xml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        return "search".equalsIgnoreCase(clean(request.getParameter("action")));
    }

    private void writeJsonError(HttpServletResponse response, int status, String code,
            String message, String requestId) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("status", "ERROR");
            json.put("code", code);
            json.put("message", message);
            json.put("requestId", requestId);
            writeJson(response, json, status);
        } catch (Exception e) {
            response.sendError(status, message);
        }
    }

    private void writeJson(HttpServletResponse response, JSONObject body, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body == null ? "{}" : body.toString());
    }

    private static Long parseLong(String value) {
        try {
            Long parsed = Long.valueOf(clean(value));
            return parsed.longValue() > 0L ? parsed : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(clean(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeFileName(String value) {
        String name = clean(value).replace('\\', '_').replace('/', '_').replace(':', '_');
        return name.length() == 0 ? "repository-file" : name;
    }
}
