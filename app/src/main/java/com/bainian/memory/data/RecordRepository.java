package com.bainian.memory.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.bainian.memory.model.MemoryRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据层：负责记录的加载、持久化，以及围绕记录的常用查询。
 * 底层用 SharedPreferences 存一份 JSON 数组，足够轻量的单机记录场景。
 */
public class RecordRepository {
    private static final String PREFS_NAME = "bainian_memory";
    private static final String KEY_RECORDS = "records";
    private static final String KEY_BIRTH_YEAR = "birth_year";
    private static final String KEY_AVATAR_URI = "avatar_uri";
    public static final DateTimeFormatter DATE_KEY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SharedPreferences prefs;
    private final List<MemoryRecord> records = new ArrayList<>();

    public RecordRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        records.clear();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_RECORDS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MemoryRecord r = new MemoryRecord();
                r.id = o.optString("id");
                r.date = o.optString("date");
                r.title = o.optString("title");
                r.body = o.optString("body");
                r.mood = o.optString("mood");
                r.tag = o.optString("tag");
                r.imageUris = o.optString("imageUris", o.optString("imageUri", ""));
                records.add(r);
            }
        } catch (JSONException ignored) {}
    }

    private void save() {
        JSONArray arr = new JSONArray();
        for (MemoryRecord r : records) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("date", r.date);
                o.put("title", r.title);
                o.put("body", r.body);
                o.put("mood", r.mood);
                o.put("tag", r.tag);
                o.put("imageUris", r.imageUris);
                arr.put(o);
            } catch (JSONException ignored) {}
        }
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply();
    }

    public List<MemoryRecord> getAll() {
        return records;
    }

    public void add(MemoryRecord record) {
        records.add(record);
        save();
    }

    public void remove(MemoryRecord record) {
        records.remove(record);
        save();
    }

    /** 编辑已有记录后调用，持久化改动 */
    public void update() {
        save();
    }

    public List<MemoryRecord> recordsFor(LocalDate date) {
        String key = date.format(DATE_KEY);
        List<MemoryRecord> res = new ArrayList<>();
        for (MemoryRecord r : records) {
            if (key.equals(r.date)) res.add(r);
        }
        return res;
    }

    /**
     * 「那年今日」——查找历史上与今天同月同日、但不同年份的记录，按年份从近到远排序。
     * 用于首页顶部的怀旧回顾卡片。
     */
    public List<MemoryRecord> onThisDay(LocalDate today) {
        String suffix = today.format(DateTimeFormatter.ofPattern("-MM-dd"));
        String todayKey = today.format(DATE_KEY);
        List<MemoryRecord> res = new ArrayList<>();
        for (MemoryRecord r : records) {
            if (r.date.length() == 10 && r.date.endsWith(suffix) && !r.date.equals(todayKey)) {
                res.add(r);
            }
        }
        res.sort((a, b) -> b.date.compareTo(a.date));
        return res;
    }

    /** 按日期倒序返回最近的 N 条记录，用于首页的记忆瀑布流 */
    public List<MemoryRecord> recent(int limit) {
        List<MemoryRecord> sorted = new ArrayList<>(records);
        sorted.sort((a, b) -> b.date.compareTo(a.date));
        if (sorted.size() > limit) return sorted.subList(0, limit);
        return sorted;
    }

    public int recordCountForYear(int year) {
        int c = 0;
        String prefix = year + "-";
        for (MemoryRecord r : records) {
            if (r.date.startsWith(prefix)) c++;
        }
        return c;
    }

    public int countRecordedDays() {
        Map<String, Boolean> m = new HashMap<>();
        for (MemoryRecord r : records) m.put(r.date, true);
        return m.size();
    }

    public int getBirthYear() {
        return prefs.getInt(KEY_BIRTH_YEAR, LocalDate.now().getYear() - 30);
    }

    public void setBirthYear(int year) {
        prefs.edit().putInt(KEY_BIRTH_YEAR, year).apply();
    }

    /** 自定义头像图片的 URI（字符串形式），未设置时返回 null，界面回退到默认简笔头像 */
    public String getAvatarUri() {
        String v = prefs.getString(KEY_AVATAR_URI, "");
        return v.isEmpty() ? null : v;
    }

    public void setAvatarUri(String uri) {
        prefs.edit().putString(KEY_AVATAR_URI, uri == null ? "" : uri).apply();
    }
}
