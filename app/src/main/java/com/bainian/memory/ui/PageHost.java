package com.bainian.memory.ui;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 页面宿主接口：各个 Page 类通过它与 MainActivity（导航容器）交互，
 * 而不必直接依赖 Activity 的内部字段，降低耦合。
 */
public interface PageHost {
    /** 跳转到指定 tab（0 回忆 / 1 时间线） */
    void switchTab(int index);

    /** 刷新当前 tab 的内容（增删改记录后调用） */
    void refreshCurrentTab();

    /** 弹出新增/编辑记录弹窗 */
    void showRecordDialog(LocalDate date, com.bainian.memory.model.MemoryRecord editRecord);

    /** 日历页翻页/选中日期后，供百年页跳转时设置 */
    void setCalendarSelection(YearMonth month, LocalDate date);

    LocalDate getSelectedDate();

    void setSelectedDate(LocalDate date);

    YearMonth getVisibleMonth();

    void setVisibleMonth(YearMonth month);

    /** 以次级页面形式打开日历（月历 + 当日记录），带返回箭头 */
    void openCalendar();

    /** 以次级页面形式打开「我的」（原设置页），从首页头像图标进入 */
    void openProfile();

    /** 以次级页面形式打开「百年」人生全景视图，从「我的」页顶部卡片进入 */
    void openCentury();

    /** 切换到「计划」tab（底部导航常驻 tab），供其他页面跳转过去使用 */
    void openTodoList();

    /** 以次级页面形式打开某个计划的任务详情 */
    void openTodoDetail(String planId);

    /**
     * 拉起系统相册选择一张图片作为头像，选择完成（或取消）后回调 onPicked。
     * onPicked 收到的 uri 为空字符串表示用户取消了选择。
     */
    void pickAvatarImage(java.util.function.Consumer<String> onPicked);
}
