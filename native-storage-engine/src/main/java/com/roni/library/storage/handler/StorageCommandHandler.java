package com.roni.library.storage.handler;

import com.roni.library.contracts.api.IApiCallback;
import com.roni.library.contracts.api.IApiHandler;
import com.roni.library.contracts.api.NativeApiError;
import com.roni.library.contracts.api.NativeApiRequest;
import com.roni.library.contracts.api.NativeApiResponse;
import com.roni.library.storage.database.Document;
import com.roni.library.storage.database.QueryFilter;
import com.roni.library.storage.database.StorageCollection;
import com.roni.library.storage.preferences.EncryptedPreferences;
import com.roni.library.storage.service.NativeStorageService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageCommandHandler implements IApiHandler {
    private final NativeStorageService storageService;

    public StorageCommandHandler(NativeStorageService storageService) {
        this.storageService = storageService;
    }

    public String getRoutePrefix() {
        return "storage";
    }

    @Override
    public String getPath() {
        return "storage";
    }

    @Override
    public String getAction() {
        return "*";
    }

    @Override
    public NativeApiResponse handleRequest(NativeApiRequest request) {
        if (request == null) {
            return NativeApiResponse.error("req_null", 400, new NativeApiError("NULL_REQUEST", "Request is null"));
        }
        String action = request.getAction();
        if (action == null) {
            action = request.getPath();
        }

        Map<String, Object> params = request.getParameters();
        if (params == null) {
            params = new HashMap<>();
        }

        try {
            Map<String, Object> resultData = new HashMap<>();

            if ("pref.set".equalsIgnoreCase(action) || "storage.set".equalsIgnoreCase(action)) {
                String prefName = (String) params.getOrDefault("prefName", "default");
                String key = (String) params.get("key");
                String value = (String) params.get("value");
                if (key == null) {
                    return NativeApiResponse.error(request.getRequestId(), 400, new NativeApiError("INVALID_ARG", "Key required"));
                }
                EncryptedPreferences pref = storageService.getPreferences(prefName);
                pref.putString(key, value);
                resultData.put("success", true);
                resultData.put("key", key);
                return NativeApiResponse.success(request.getRequestId(), resultData);

            } else if ("pref.get".equalsIgnoreCase(action) || "storage.get".equalsIgnoreCase(action)) {
                String prefName = (String) params.getOrDefault("prefName", "default");
                String key = (String) params.get("key");
                String defValue = (String) params.get("defaultValue");
                EncryptedPreferences pref = storageService.getPreferences(prefName);
                String val = pref.getString(key, defValue);
                resultData.put("key", key);
                resultData.put("value", val);
                return NativeApiResponse.success(request.getRequestId(), resultData);

            } else if ("pref.remove".equalsIgnoreCase(action) || "storage.remove".equalsIgnoreCase(action)) {
                String prefName = (String) params.getOrDefault("prefName", "default");
                String key = (String) params.get("key");
                EncryptedPreferences pref = storageService.getPreferences(prefName);
                boolean removed = pref.remove(key);
                resultData.put("success", removed);
                return NativeApiResponse.success(request.getRequestId(), resultData);

            } else if ("db.insert".equalsIgnoreCase(action)) {
                String colName = (String) params.getOrDefault("collection", "default");
                String docId = (String) params.get("id");
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) params.get("data");
                StorageCollection col = storageService.getDatabase().getCollection(colName);
                Document doc = new Document(docId, data);
                col.insert(doc);
                resultData.put("success", true);
                resultData.put("documentId", doc.getId());
                return NativeApiResponse.success(request.getRequestId(), resultData);

            } else if ("db.findById".equalsIgnoreCase(action)) {
                String colName = (String) params.getOrDefault("collection", "default");
                String docId = (String) params.get("id");
                StorageCollection col = storageService.getDatabase().getCollection(colName);
                Document doc = col.findById(docId);
                resultData.put("found", doc != null);
                if (doc != null) {
                    resultData.put("document", doc.toMap());
                }
                return NativeApiResponse.success(request.getRequestId(), resultData);

            } else if ("db.count".equalsIgnoreCase(action)) {
                String colName = (String) params.getOrDefault("collection", "default");
                StorageCollection col = storageService.getDatabase().getCollection(colName);
                resultData.put("count", col.count(QueryFilter.all()));
                return NativeApiResponse.success(request.getRequestId(), resultData);

            } else {
                return NativeApiResponse.error(request.getRequestId(), 404,
                        new NativeApiError("UNKNOWN_ACTION", "Action not supported: " + action));
            }
        } catch (Exception e) {
            return NativeApiResponse.error(request.getRequestId(), 500,
                    new NativeApiError("EXECUTION_ERROR", e.getMessage()));
        }
    }

    public void handle(NativeApiRequest request, IApiCallback callback) {
        if (callback == null) return;
        NativeApiResponse response = handleRequest(request);
        if (response.isSuccess()) {
            callback.onResponse(response);
        } else {
            callback.onError(response.getError());
        }
    }
}
