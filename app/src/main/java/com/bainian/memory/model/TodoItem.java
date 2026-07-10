package com.bainian.memory.model;

/**
 * TODO 计划下的一条任务项。
 */
public class TodoItem {
    /** 本机生成的唯一标识（UUID） */
    public String id = "";
    /** 所属计划的 id */
    public String planId = "";
    public String text = "";
    public boolean done = false;
    /** 创建者的匿名设备 id（用于展示"谁添加的"，为未来多人协作预留） */
    public String createdBy = "";
    public String createdByName = "";
    /** 完成者的匿名设备 id（用于展示"谁完成的"） */
    public String doneBy = "";
    public String doneByName = "";
    public long createdAt = 0L;
    public long updatedAt = 0L;
}
