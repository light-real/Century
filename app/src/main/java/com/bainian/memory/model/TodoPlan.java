package com.bainian.memory.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个 TODO 计划（清单），下面挂着若干任务项。
 *
 * 字段里保留了 inviteCode / memberIds / memberNames 等「多人协作」相关信息——
 * 当前版本这些字段仅用于本机展示（例如显示"共 1 人"、生成一个可分享的邀请码文案），
 * 并不具备真正的跨设备同步能力；后续如果接入云端服务，可以在不改动数据结构的前提下
 * 把这些字段用起来，实现真正的多人实时协作。
 */
public class TodoPlan {
    /** 本机生成的唯一标识（UUID） */
    public String id = "";
    public String title = "";
    /** 创建者的匿名设备 id */
    public String ownerId = "";
    /** 6 位邀请码，预留给未来云同步功能使用 */
    public String inviteCode = "";
    /** 协作者的匿名设备 id（当前版本只会包含自己） */
    public List<String> memberIds = new ArrayList<>();
    /** 各协作者的展示昵称 */
    public List<String> memberNames = new ArrayList<>();
    public long createdAt = 0L;
    public long updatedAt = 0L;

    /** 运行期缓存字段：该计划下任务的完成情况统计，仅用于列表页展示，不持久化 */
    public transient int totalCount = 0;
    public transient int doneCount = 0;
}
