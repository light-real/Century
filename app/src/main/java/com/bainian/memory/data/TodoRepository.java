package com.bainian.memory.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.bainian.memory.model.TodoItem;
import com.bainian.memory.model.TodoPlan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 「TODO 计划」的数据层：负责计划与任务的增删改查，全部保存在本机
 * SharedPreferences 里的一份 JSON 数据（与 {@link RecordRepository} 的持久化方式一致）。
 *
 * 说明：数据模型里保留了"协作者 / 邀请码"等字段，是为未来接入云端多人实时同步预留的
 * 扩展位——当前版本纯离线运行，不依赖任何网络或第三方 SDK，换/卸载设备后数据不会保留，
 * 但不会因为网络问题导致功能不可用。
 */
public class TodoRepository {
    private static final String PREFS_NAME = "bainian_todo";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_PLANS = "plans";
    private static final String KEY_ITEMS = "items";

    private final SharedPreferences prefs;
    private final List<TodoPlan> plans = new ArrayList<>();
    private final List<TodoItem> items = new ArrayList<>();

    public TodoRepository(Context ctx) {
        this.prefs = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    // ═══════════════════════════════════════════
    //  本机身份：匿名 deviceId + 昵称（为未来协作预留）
    // ═══════════════════════════════════════════

    public String getDeviceId() {
        String id = prefs.getString(KEY_DEVICE_ID, "");
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    public String getNickname() {
        String name = prefs.getString(KEY_NICKNAME, "");
        return name.isEmpty() ? "我" : name;
    }

    public void setNickname(String name) {
        prefs.edit().putString(KEY_NICKNAME, name == null ? "" : name.trim()).apply();
    }

    // ═══════════════════════════════════════════
    //  加载 / 持久化
    // ═══════════════════════════════════════════

    private void load() {
        plans.clear();
        items.clear();
        try {
            JSONArray planArr = new JSONArray(prefs.getString(KEY_PLANS, "[]"));
            for (int i = 0; i < planArr.length(); i++) {
                plans.add(planFromJson(planArr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
        try {
            JSONArray itemArr = new JSONArray(prefs.getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < itemArr.length(); i++) {
                items.add(itemFromJson(itemArr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {}
    }

    private void savePlans() {
        JSONArray arr = new JSONArray();
        for (TodoPlan p : plans) arr.put(planToJson(p));
        prefs.edit().putString(KEY_PLANS, arr.toString()).apply();
    }

    private void saveItems() {
        JSONArray arr = new JSONArray();
        for (TodoItem it : items) arr.put(itemToJson(it));
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    // ═══════════════════════════════════════════
    //  计划：创建 / 列表 / 删除
    // ═══════════════════════════════════════════

    /** 新建一个 TODO 计划，返回创建好的计划对象 */
    public TodoPlan createPlan(String title) {
        TodoPlan p = new TodoPlan();
        p.id = UUID.randomUUID().toString();
        p.title = title == null ? "" : title.trim();
        p.ownerId = getDeviceId();
        p.inviteCode = generateInviteCode();
        p.memberIds = new ArrayList<>();
        p.memberIds.add(p.ownerId);
        p.memberNames = new ArrayList<>();
        p.memberNames.add(getNickname());
        long now = System.currentTimeMillis();
        p.createdAt = now;
        p.updatedAt = now;
        plans.add(0, p);
        savePlans();
        return p;
    }

    /** 我的全部计划，按最近更新时间倒序，附带任务完成度统计 */
    public List<TodoPlan> getAllPlans() {
        List<TodoPlan> result = new ArrayList<>(plans);
        for (TodoPlan p : result) {
            int total = 0, done = 0;
            for (TodoItem it : items) {
                if (it.planId.equals(p.id)) {
                    total++;
                    if (it.done) done++;
                }
            }
            p.totalCount = total;
            p.doneCount = done;
        }
        Collections.sort(result, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return result;
    }

    public TodoPlan getPlan(String planId) {
        for (TodoPlan p : plans) {
            if (p.id.equals(planId)) return p;
        }
        return null;
    }

    public void renamePlan(String planId, String newTitle) {
        TodoPlan p = getPlan(planId);
        if (p == null) return;
        p.title = newTitle == null ? "" : newTitle.trim();
        p.updatedAt = System.currentTimeMillis();
        savePlans();
    }

    /** 删除计划及其下所有任务 */
    public void deletePlan(String planId) {
        for (int i = plans.size() - 1; i >= 0; i--) {
            if (plans.get(i).id.equals(planId)) plans.remove(i);
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).planId.equals(planId)) items.remove(i);
        }
        savePlans();
        saveItems();
    }

    // ═══════════════════════════════════════════
    //  任务项：增删改查
    // ═══════════════════════════════════════════

    public List<TodoItem> getItems(String planId) {
        List<TodoItem> result = new ArrayList<>();
        for (TodoItem it : items) {
            if (it.planId.equals(planId)) result.add(it);
        }
        Collections.sort(result, Comparator.comparingLong(a -> a.createdAt));
        return result;
    }

    public TodoItem addItem(String planId, String text) {
        TodoItem it = new TodoItem();
        it.id = UUID.randomUUID().toString();
        it.planId = planId;
        it.text = text == null ? "" : text.trim();
        it.done = false;
        it.createdBy = getDeviceId();
        it.createdByName = getNickname();
        long now = System.currentTimeMillis();
        it.createdAt = now;
        it.updatedAt = now;
        items.add(it);
        saveItems();
        touchPlan(planId);
        return it;
    }

    public void toggleItemDone(TodoItem item) {
        TodoItem it = findItem(item.id);
        if (it == null) return;
        it.done = !it.done;
        if (it.done) {
            it.doneBy = getDeviceId();
            it.doneByName = getNickname();
        } else {
            it.doneBy = "";
            it.doneByName = "";
        }
        it.updatedAt = System.currentTimeMillis();
        saveItems();
        touchPlan(it.planId);
    }

    public void updateItemText(String itemId, String newText) {
        TodoItem it = findItem(itemId);
        if (it == null) return;
        it.text = newText == null ? "" : newText.trim();
        it.updatedAt = System.currentTimeMillis();
        saveItems();
        touchPlan(it.planId);
    }

    public void deleteItem(String itemId) {
        TodoItem it = findItem(itemId);
        if (it == null) return;
        items.remove(it);
        saveItems();
        touchPlan(it.planId);
    }

    private TodoItem findItem(String itemId) {
        for (TodoItem it : items) {
            if (it.id.equals(itemId)) return it;
        }
        return null;
    }

    private void touchPlan(String planId) {
        TodoPlan p = getPlan(planId);
        if (p == null) return;
        p.updatedAt = System.currentTimeMillis();
        savePlans();
    }

    // ═══════════════════════════════════════════
    //  JSON 转换 & 小工具
    // ═══════════════════════════════════════════

    private JSONObject planToJson(TodoPlan p) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", p.id);
            o.put("title", p.title);
            o.put("ownerId", p.ownerId);
            o.put("inviteCode", p.inviteCode);
            o.put("memberIds", new JSONArray(p.memberIds));
            o.put("memberNames", new JSONArray(p.memberNames));
            o.put("createdAt", p.createdAt);
            o.put("updatedAt", p.updatedAt);
        } catch (JSONException ignored) {}
        return o;
    }

    private TodoPlan planFromJson(JSONObject o) {
        TodoPlan p = new TodoPlan();
        p.id = o.optString("id");
        p.title = o.optString("title");
        p.ownerId = o.optString("ownerId");
        p.inviteCode = o.optString("inviteCode");
        p.memberIds = jsonToStringList(o.optJSONArray("memberIds"));
        p.memberNames = jsonToStringList(o.optJSONArray("memberNames"));
        p.createdAt = o.optLong("createdAt");
        p.updatedAt = o.optLong("updatedAt");
        return p;
    }

    private JSONObject itemToJson(TodoItem it) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", it.id);
            o.put("planId", it.planId);
            o.put("text", it.text);
            o.put("done", it.done);
            o.put("createdBy", it.createdBy);
            o.put("createdByName", it.createdByName);
            o.put("doneBy", it.doneBy);
            o.put("doneByName", it.doneByName);
            o.put("createdAt", it.createdAt);
            o.put("updatedAt", it.updatedAt);
        } catch (JSONException ignored) {}
        return o;
    }

    private TodoItem itemFromJson(JSONObject o) {
        TodoItem it = new TodoItem();
        it.id = o.optString("id");
        it.planId = o.optString("planId");
        it.text = o.optString("text");
        it.done = o.optBoolean("done");
        it.createdBy = o.optString("createdBy");
        it.createdByName = o.optString("createdByName");
        it.doneBy = o.optString("doneBy");
        it.doneByName = o.optString("doneByName");
        it.createdAt = o.optLong("createdAt");
        it.updatedAt = o.optLong("updatedAt");
        return it;
    }

    private List<String> jsonToStringList(JSONArray arr) {
        List<String> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.optString(i));
        }
        return list;
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去掉易混淆字符 0/O/1/I
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
}
