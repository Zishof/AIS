package ais.action.master.generic.v2;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Menu;
import ais.database.model.Tbmuser;

public class GenericCrudRequestContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient HttpServletRequest request;
    private GenericCrudDefinition definition;
    private Tbmuser user;
    private Menu menu;
    private boolean canRead;
    private boolean canCreate;
    private boolean canUpdate;
    private boolean canDelete;
    private boolean canApprove;
    private boolean canReject;
    private String requestId;

    public static GenericCrudRequestContext from(HttpServletRequest request, GenericCrudDefinition definition) {
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        context.request = request;
        context.definition = definition;
        context.requestId = request == null ? null : first(request.getHeader("X-Request-ID"), request.getParameter("requestId"));
        if (request != null) {
            try { context.user = Common.getCurrentUser(request); } catch (Exception ignored) { context.user = null; }
            Object currentMenu = request.getSession(false) == null ? null : request.getSession(false).getAttribute("current_menu");
            if (currentMenu instanceof Menu) { context.menu = (Menu) currentMenu; }
            if (context.menu != null) {
                context.canRead = check(context.menu, CommonPrivilages.READ, context.user);
                context.canCreate = check(context.menu, CommonPrivilages.CREATE, context.user);
                context.canUpdate = check(context.menu, CommonPrivilages.UPDATE, context.user);
                context.canDelete = check(context.menu, CommonPrivilages.DELETE, context.user);
                context.canApprove = check(context.menu, CommonPrivilages.APPROVE, context.user);
                context.canReject = check(context.menu, CommonPrivilages.REJECT, context.user);
            }
        }
        return context;
    }

    private static String first(String one, String two) { return one != null && one.length() > 0 ? one : two; }
    private static boolean check(Menu menu, Integer code, Tbmuser user) {
        try {
            java.lang.reflect.Method exact = CommonPrivilages.class.getMethod("checkPrevilages",
                    new Class[] { Menu.class, Integer.class, Tbmuser.class });
            Object result = exact.invoke(null, new Object[] { menu, code, user });
            return Boolean.TRUE.equals(result);
        } catch (NoSuchMethodException oldBuild) {
            return CommonPrivilages.checkPrevilages(menu, code);
        } catch (Exception denied) { return false; }
    }
    public HttpServletRequest getRequest() { return request; }
    public GenericCrudDefinition getDefinition() { return definition; }
    public Tbmuser getUser() { return user; }
    public Menu getMenu() { return menu; }
    public boolean isCanRead() { return canRead; }
    public boolean isCanCreate() { return canCreate; }
    public boolean isCanUpdate() { return canUpdate; }
    public boolean isCanDelete() { return canDelete; }
    public boolean isCanApprove() { return canApprove; }
    public boolean isCanReject() { return canReject; }
    public String getRequestId() { return requestId; }
}
