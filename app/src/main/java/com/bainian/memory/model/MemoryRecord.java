package com.bainian.memory.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条记忆记录：某一天发生的一件事，包含标题、正文、心情、标签和若干图片。
 */
public class MemoryRecord {
    public String id = "";
    public String date = "";
    public String title = "";
    public String body = "";
    public String mood = "";
    public String tag = "";
    /** 多张图片 URI，用 "|" 拼接存储 */
    public String imageUris = "";

    public List<String> getImageUris() {
        List<String> list = new ArrayList<>();
        if (imageUris == null || imageUris.isEmpty()) return list;
        for (String s : imageUris.split("\\|")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    public void setImageUris(List<String> uris) {
        imageUris = uris == null || uris.isEmpty() ? "" : String.join("|", uris);
    }
}
