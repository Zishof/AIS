package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

/** Preferensi sesi tervalidasi; persistence DB diaktifkan setelah migration 001. */
@SuppressWarnings("rawtypes")
public class GenericCrudColumnPreferenceService {
    public List validateColumns(GenericCrudDefinition definition, List requested) throws GenericCrudException {
        List result = new ArrayList();
        if (requested == null) return result;
        for (int i = 0; i < requested.size(); i++) {
            String property = String.valueOf(requested.get(i));
            GenericCrudFieldDefinition field = definition.getField(property);
            if (field == null || field.isSensitive()) throw new GenericCrudException(400, "COLUMN_NOT_ALLOWED", "Kolom tidak diizinkan.");
            if (!result.contains(property)) result.add(property);
        }
        return result;
    }
    public void saveSession(GenericCrudRequestContext context, List columns) throws GenericCrudException {
        HttpSession session = context.getRequest().getSession(true);
        session.setAttribute("genericCrud.columns." + context.getDefinition().getEntityKey(), validateColumns(context.getDefinition(), columns));
    }
}
